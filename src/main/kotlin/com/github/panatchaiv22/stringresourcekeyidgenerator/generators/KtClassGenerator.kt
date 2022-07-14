package com.github.panatchaiv22.stringresourcekeyidgenerator.generators

import android.content.Context
import android.databinding.tool.ext.stripNonJava
import co.mona.android.core.base.ResNameConverter
import com.github.panatchaiv22.stringresourcekeyidgenerator.constants.STRINGS_XML_FILE
import com.github.panatchaiv22.stringresourcekeyidgenerator.parsers.StAxParser
import com.github.panatchaiv22.stringresourcekeyidgenerator.utils.NotificationUtils
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.jetbrains.rd.util.printlnError
import com.squareup.kotlinpoet.*
import org.jetbrains.kotlin.idea.util.projectStructure.getModule
import org.jetbrains.kotlin.idea.util.projectStructure.module
import java.io.File
import java.nio.charset.StandardCharsets

class KtClassGenerator {

    private val parser = StAxParser()
    private val logger = PluginManager.getInstance().thisLogger()

    private fun notify(msg: String, project: Project) {
        NotificationUtils.notify(msg, project)
    }

    private fun generateStringIdCases(vFile: VirtualFile, project: Project): StringBuilder {
        val idList = parser.parse(vFile, project)
        val builder = StringBuilder()
        idList.forEach { id ->
            builder.appendLine("""R.string.$id -> "$id"""")
        }
        return builder
    }

    private fun generateModuleWhenCodeBlock(body: StringBuilder): CodeBlock {
        val cb = CodeBlock.builder().beginControlFlow("return when (resId)")

        return cb.add(body.toString()).addStatement("else -> %S", "").endControlFlow().build()
    }

    private fun generateModuleFunctionBody(
        name: String, modifier: KModifier? = null, parameter: ParameterSpec? = null, bodyBlock: CodeBlock? = null
    ): FunSpec {
        val builder = FunSpec.builder(name.stripNonJava()).returns(String::class)

        parameter?.let { builder.addParameter(it) }
        modifier?.let { builder.addModifiers(it) }
        bodyBlock?.let { builder.addCode(it) }

        return builder.build()
    }

    private fun generateMainFuncBody(
        name: String = "convertResToName", stringXmlFileList: List<PsiFileSystemItem>, parameter: ParameterSpec
    ): FunSpec {
        val builder = FunSpec.builder(name).returns(String::class)
        val cb = CodeBlock.builder().beginControlFlow("return when")

        stringXmlFileList.forEach { sFile ->
            val moduleName = getModuleName(sFile)
            cb.addStatement("${moduleName.stripNonJava()}($FUN_PARAM).also { tmp = it }.isNotEmpty() -> tmp")
        }

        with(builder) {
            addModifiers(KModifier.OVERRIDE)
            addParameter(parameter)
            addStatement("""var tmp = """"")
            addCode(
                cb.addStatement("else -> $DEFAULT_ELSE").endControlFlow().build()
            )
        }

        return builder.build()
    }

    private fun generateStringIdCacheForModule(psiStringXmlFile: PsiFileSystemItem, project: Project): Boolean {
        val vFile = psiStringXmlFile.virtualFile
        val moduleName = getModuleName(psiStringXmlFile)
        val moduleFunctionBody = generateStringIdCases(vFile, project)
        return saveModuleCache(moduleName, moduleFunctionBody, project)
    }

    private fun getModuleFunctionBody(psiStringXmlFile: PsiFileSystemItem, project: Project): StringBuilder {
        val moduleName = getModuleName(psiStringXmlFile)
        val tmpFile = File(FileUtilRt.getTempDirectory(), "${project.name.stripNonJava()}_$moduleName")
        val builder = StringBuilder()
        if (!tmpFile.exists()) {
            if (generateStringIdCacheForModule(psiStringXmlFile, project)) {
                return getModuleFunctionBody(psiStringXmlFile, project)
            }
        } else {
            logger.info("Reading ${tmpFile.absolutePath}")
            builder.append(tmpFile.readText(StandardCharsets.UTF_8))
        }
        return builder
    }

    private fun generateClass(stringXmlFileList: List<PsiFileSystemItem>, funcList: List<FunSpec>): TypeSpec {
        // the main function body
        val mainFuncBody = generateMainFuncBody(
            stringXmlFileList = stringXmlFileList,
            parameter = ParameterSpec.builder(FUN_PARAM, Int::class).build(),
        )

        // the class constructor
        val primaryConstructor =
            FunSpec.constructorBuilder().addParameter(ParameterSpec.builder(CLASS_PARAM, Context::class).build())
                .build()

        // the class builder
        val classBuilder = TypeSpec.classBuilder(CLASS_NAME).addSuperinterface(ResNameConverter::class)
            .primaryConstructor(primaryConstructor).addProperty(
                PropertySpec.builder(CLASS_PARAM, Context::class).initializer(CLASS_PARAM)
                    .addModifiers(KModifier.PRIVATE).build()
            ).addAnnotation(
                AnnotationSpec.builder(Suppress::class).addMember(""""RedundantVisibilityModifier"""").build()
            ).addFunction(mainFuncBody)

        funcList.forEach { func ->
            classBuilder.addFunction(func)
        }

        return classBuilder.build()
    }

    private fun saveModuleCache(moduleName: String, content: StringBuilder, project: Project): Boolean {
        val tmpFile = File(FileUtilRt.getTempDirectory(), "${project.name.stripNonJava()}_$moduleName")
        if (!tmpFile.exists()) {
            if (!FileUtilRt.createIfNotExists(tmpFile)) {
                val msg = "Cannot create ${tmpFile.absolutePath}"
                logger.error(msg)
                printlnError(msg)
                notify(msg, project)
                return false
            }
        }
        tmpFile.writeText(content.toString(), StandardCharsets.UTF_8)
        return true
    }

    private fun getModuleName(psiFile: PsiFileSystemItem): String {
//        println("------------XML: ${psiFile.virtualFile.path}")
//        println("-----XML-Module: ${psiFile.module?.name ?: "-"}")
        val path = psiFile.module?.name?.split(".")
        val builder = StringBuilder()
        path?.let {
            for (i in 1 until it.size - 1) {
                val name = if (i > 1) {
                    it[i].capitalize()
                } else {
                    it[i]
                }
                builder.append(name)
            }
        }
        return builder.toString().stripNonJava()
    }

    private fun getModuleList(project: Project): List<PsiFileSystemItem> {
        val files: Array<PsiFileSystemItem> =
            FilenameIndex.getFilesByName(project, STRINGS_XML_FILE, GlobalSearchScope.projectScope(project), false)
        return files.distinct().toList()
    }

    fun generate(vFile: VirtualFile, document: Document) {
        // create a module function
        val project = ProjectLocator.getInstance().getProjectsForFile(vFile).first()

        try {
            val r = Runnable {
                // generate new cache of the given strings.xml file
                vFile.getModule(project)?.let {
                    val moduleName = it.name.split(".")[1]
                    val moduleFunctionBody = generateStringIdCases(vFile, project)
                    if (!saveModuleCache(moduleName, moduleFunctionBody, project)) {
                        return@Runnable
                    }
                }

                // load caches for other strings.xml
                val stringXmlFileList = getModuleList(project)
                val functionList = mutableListOf<FunSpec>()
                stringXmlFileList.forEach { psiStringXmlFile ->
                    val moduleFunSpec = generateModuleFunctionBody(
                        name = getModuleName(psiStringXmlFile),
                        modifier = KModifier.PRIVATE,
                        parameter = ParameterSpec.builder(FUN_PARAM, Int::class).build(),
                        bodyBlock = generateModuleWhenCodeBlock(getModuleFunctionBody(psiStringXmlFile, project))
                    )
                    functionList.add(moduleFunSpec)
                }

                val file = FileSpec.builder("co.mona.android.applicationHelper", "$CLASS_NAME.kt")
                    .addImport("co.mona.android", "R").addImport("co.mona.android.core.base", "ResNameConverter")
                    .addType(generateClass(stringXmlFileList, functionList)).build()

                val convertorFile = File(
                    "${
                        project.basePath
                    }/app/src/main/java/co/mona/android/applicationHelper/$CLASS_NAME.kt"
                )
                if (!convertorFile.exists()) {
                    convertorFile.parentFile.mkdirs()
                    convertorFile.createNewFile()
                }
                val sb = StringBuilder("/* ktlint-disable */")
                sb.appendLine()
                file.writeTo(sb)
                sb.appendLine("/* ktlint-enable */")
                convertorFile.writeText(
                    sb.replaceFirst("""Context,""".toRegex(RegexOption.MULTILINE), "Context"),
                    StandardCharsets.UTF_8
                )

                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(convertorFile)
            }
            WriteCommandAction.runWriteCommandAction(project, r)
        } catch (e: Exception) {
            printlnError(e.stackTraceToString())
            notify(e.stackTraceToString(), project)
        }
    }

    companion object {
        private const val FUN_PARAM = "resId"
        private const val CLASS_NAME = "MonacoResNameConverter"
        private const val CLASS_PARAM = "context"
        private const val DEFAULT_ELSE = "context.resources.getResourceEntryName($FUN_PARAM)"
    }
}
