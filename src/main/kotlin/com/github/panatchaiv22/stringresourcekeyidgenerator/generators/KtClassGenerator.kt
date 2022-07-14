package com.github.panatchaiv22.stringresourcekeyidgenerator.generators

import android.content.Context
import android.databinding.tool.ext.stripNonJava
import co.mona.android.core.base.ResNameConverter
import com.github.panatchaiv22.stringresourcekeyidgenerator.constants.STRINGS_XML_FILE
import com.github.panatchaiv22.stringresourcekeyidgenerator.parsers.StAxParser
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.squareup.kotlinpoet.*
import org.jetbrains.kotlin.idea.util.projectStructure.getModule
import org.jetbrains.kotlin.idea.util.projectStructure.module
import java.io.File
import java.nio.charset.StandardCharsets


class KtClassGenerator {

    private val parser = StAxParser()

//    /* ktlint-disable */
//    package co.mona.android.applicationHelper
//
//    import co.mona.android.core.base.ResNameConverter
//    import android.content.Context
//
//    class MonacoResNameConverter(
//        private val context: Context
//    ) : ResNameConverter {
//
//        override fun convertResToName(resId: Int): String {
//            return context.resources.getResourceEntryName(resId)
//        }
//    }
//    /* ktlint-enable */

    private val classDeclaration = """
    /* ktlint-disable */
    package co.mona.android.applicationHelper

    import co.mona.android.core.base.ResNameConverter
    import android.content.Context

    class MonacoResNameConverter(
        private val context: Context
    ) : ResNameConverter {

    """.trimIndent()

    private val functionEnd = """
            }
        }
    """.trimIndent()

    private val classEnd = """
    }
    /* ktlint-enable */
    
    """.trimIndent()

    fun generate1(vFile: VirtualFile, document: Document) {
        val fileTemplate = """
    /* ktlint-disable */
    package co.mona.android.applicationHelper

    import co.mona.android.core.base.ResNameConverter
    import android.content.Context

    class MonacoResNameConverter(
        private val context: Context
    ) : ResNameConverter {

        override fun convertResToName(resId: Int): String {
            when (resId) {
                0 -> 0
                1 -> 1
                else -> 2
            }
        }
    }
    /* ktlint-enable */
    
""".trimIndent()

        val project = ProjectLocator.getInstance().getProjectsForFile(vFile).first()
        val idsList = parser.parse(vFile, project)
        println("Project Name: ${project.name}")
        println("Project Path: ${project.basePath}")
        // "Project Path: ${project.basePath}/app/src/main/java/co/mona/android/applicationHelper/MonacoResNameConverter.kt"
        // ProjectRootManager.getInstance(project)
        val files: Array<PsiFileSystemItem> =
            FilenameIndex.getFilesByName(project, "strings.xml", GlobalSearchScope.projectScope(project), false)
        files.forEach {
            println("file: $it")
        }

        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
        psiFile?.virtualFile?.let { dv ->
            val module = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(dv)
            val names = module?.name?.split('.') ?: listOf()
            println("Module Name: ${names.getOrNull(names.size - 2)}")
        }

//        val vFiles = ProjectRootManager.getInstance(project).contentRootsFromAllModules
//        val sourceRootsList: String = Arrays.stream(vFiles).map { obj: VirtualFile -> obj.url }
//            .collect(Collectors.joining("\n"))
//        println("----------->$sourceRootsList")

        val file = File(
            "${
                project.basePath
            }/app/src/main/java/co/mona/android/applicationHelper/MonacoResNameConverter.kt"
        )

//        Project Name: My Application
//        Module Name: My_Application.app.main
//                File Path: /Users/panatchai/IntelliJIDEAProjects/MyApplication/app/src/main/java/co/mona/android/applicationHelper/MonacoResNameConverter.kt

        try {
            val r = Runnable {
                try {
                    if (!file.exists()) {
                        file.parentFile.mkdirs()
                        file.createNewFile()
                    }

                    val cFile: VirtualFile? =
                        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)

                    cFile?.setBinaryContent(fileTemplate.toByteArray())
                } catch (e: Exception) {
                    println(e)
                }
            }
            WriteCommandAction.runWriteCommandAction(project, r)
        } catch (e: Exception) {
            println(e)
        }

//        val cFile: VirtualFile? = LocalFileSystem.getInstance().findFileByPath("co/mona/android/applicationHelper/MonacoResNameConverter.kt")
//        val ktFile = File("co/mona/android/applicationHelper", "MonacoResNameConverter.kt")
//        if (!ktFile.exists()) ktFile.createNewFile()
//        ktFile.writeText(fileTemplate)
    }

    private fun generateStringIdCases(vFile: VirtualFile, project: Project): StringBuilder {
        val idList = parser.parse(vFile, project)
        val builder = StringBuilder()
        idList.forEach { id ->
            builder.appendLine("""R.string.$id -> "$id"""")
        }
        return builder
    }

    private fun generateWhenCodeBlock(body: StringBuilder): CodeBlock {
        val cb = CodeBlock.builder()
            .beginControlFlow("return when (resId)")

//        idList.forEach { id ->
//            cb.addStatement("R.string.$id -> %S", id)
//        }

        return cb
            .add(body.toString())
            .addStatement("else -> %S", "")
            .endControlFlow()
            .build()
    }

    private fun generateModuleFunctionBody(
        name: String,
        modifier: KModifier? = null,
        parameter: ParameterSpec? = null,
        bodyBlock: CodeBlock? = null
    ): FunSpec {
        val builder = FunSpec.builder(name.stripNonJava()).returns(String::class)

        parameter?.let { builder.addParameter(it) }
        modifier?.let { builder.addModifiers(it) }
        bodyBlock?.let { builder.addCode(it) }

        return builder.build()
    }

    private fun generateMainFuncBody(
        name: String = "convertResToName",
        moduleList: List<String>,
        parameter: ParameterSpec
    ): FunSpec {
        val builder = FunSpec.builder(name).returns(String::class)
        val cb = CodeBlock.builder()
            .beginControlFlow("return when")

        moduleList.forEach { module ->
            cb.addStatement("${module.stripNonJava()}($FUN_PARAM).also { tmp = it }.isNotEmpty() -> tmp")
        }

        with(builder) {
            addModifiers(KModifier.OVERRIDE)
            addParameter(parameter)
            addStatement("""var tmp = """"")
            addCode(
                cb
                    .addStatement("else -> $DEFAULT_ELSE")
                    .endControlFlow()
                    .build()
            )
        }

        return builder.build()
    }

    private fun generateStringIdCacheForModule(moduleName: String, project: Project): Boolean {
        val vFile =
            LocalFileSystem.getInstance()
                .findFileByPath("${project.basePath}/$moduleName/src/main/res/values/strings.xml")
        if (vFile != null) {
            val moduleFunctionBody = generateStringIdCases(vFile, project)
            return saveModuleCache(moduleName, moduleFunctionBody)
        } else {
            // TODO: Handle error!
        }
        return false
    }

    private fun getModuleFunctionBody(module: String, project: Project): StringBuilder {
        val tmpDir = File(FileUtilRt.getTempDirectory(), TEMP_DIR)
        val moduleFile = File(tmpDir, module)
        val builder = StringBuilder()
        if (!moduleFile.exists()) {
            if (generateStringIdCacheForModule(module, project)) {
                return getModuleFunctionBody(module, project)
            } else {
                // TODO: show error!
                println("Project Base: ${project.basePath}")
                println("Module Path: ${project.basePath}/$module/src/main/res/values/strings.xml")
            }
        } else {
            builder.append(moduleFile.readText(StandardCharsets.UTF_8))
        }
        return builder
    }

    private fun generateClass(moduleList: List<String>, funcList: List<FunSpec>): TypeSpec {
        // the main function body
        val mainFuncBody = generateMainFuncBody(
            moduleList = moduleList,
            parameter = ParameterSpec.builder(FUN_PARAM, Int::class).build(),
        )

        // the class constructor
        val primaryConstructor = FunSpec.constructorBuilder()
            .addParameter(ParameterSpec.builder(CLASS_PARAM, Context::class).build())
            .build()

        // the class builder
        val classBuilder = TypeSpec.classBuilder(CLASS_NAME)
            .addSuperinterface(ResNameConverter::class)
            .primaryConstructor(primaryConstructor)
            .addProperty(
                PropertySpec.builder(CLASS_PARAM, Context::class)
                    .initializer(CLASS_PARAM)
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .addMember(""""RedundantVisibilityModifier"""")
                    .build()
            )
            .addFunction(mainFuncBody)

        funcList.forEach { func ->
            classBuilder.addFunction(func)
        }
        // create each module as function (1 function per 1 module)
//        moduleList.forEach { module ->
//            val moduleFunc = generateModuleFunctionBody(
//                name = module,
//                modifier = KModifier.PRIVATE,
//                parameter = ParameterSpec.builder(FUN_PARAM, Int::class).build(),
//                bodyBlock = generateWhenCodeBlock(getModuleFunctionBody(module))
//            )
//            classBuilder.addFunction(moduleFunc)
//        }

        return classBuilder.build()
    }

    private fun saveModuleCache(module: String, content: StringBuilder): Boolean {
        val tmpDir = File(FileUtilRt.getTempDirectory(), TEMP_DIR)
        val moduleFile = File(tmpDir, module)
        if (!moduleFile.exists()) {
            if (!moduleFile.createNewFile()) return false
        }
        moduleFile.writeText(content.toString(), StandardCharsets.UTF_8)
        return true
    }

    private fun getModuleList(project: Project): List<String> {
        val files: Array<PsiFileSystemItem> =
            FilenameIndex.getFilesByName(project, STRINGS_XML_FILE, GlobalSearchScope.projectScope(project), false)
        val moduleList = mutableListOf<String>()
        files.forEach { file ->
            file.module?.name?.let { moduleName ->
                moduleList.add(moduleName.split(".")[1])
            }
            // println("Module: ${it.module?.name} --- File: ${it.virtualFile.name}")
        }
        return moduleList
    }

    fun generate() {
//        val moduleList = listOf("app", "module1")
//        val file = FileSpec.builder("co.mona.android.applicationHelper", "MonacoResNameConverter.kt")
//            .addImport("co.mona.android", "R")
//            .addImport("co.mona.android.core.base", "ResNameConverter")
//            .addType(generateClass(moduleList))
//            .build()
//
//        val sb = StringBuilder("/* ktlint-disable */")
//        sb.appendLine()
//        file.writeTo(sb)
//        sb.appendLine("/* ktlint-enable */")
//        println(sb)
    }

    fun generate(vFile: VirtualFile, document: Document) {
        // create a module function
        val project = ProjectLocator.getInstance().getProjectsForFile(vFile).first()
        val module = vFile.getModule(project)

        try {
            val r = Runnable {
                module?.let {
                    val moduleName = it.name.split(".")[1]
                    val moduleFunctionBody = generateStringIdCases(vFile, project)
                    if (!saveModuleCache(moduleName, moduleFunctionBody)) {
                        // TODO: handle error
                        return@Runnable
                    }

//            val moduleFunc: FunSpec = generateModuleFunctionBody(
//                name = moduleName,
//                modifier = KModifier.PRIVATE,
//                parameter = ParameterSpec.builder(FUN_PARAM, Int::class).build(),
//                bodyBlock = generateWhenCodeBlock(getModuleStringList(moduleName))
//            )
//            println("Module Function: $moduleFunc")
                    // TODO: compose the class
                    val moduleList = getModuleList(project)
                    val functionList = mutableListOf<FunSpec>()
                    moduleList.forEach { moduleName ->
                        val moduleFunSpec = generateModuleFunctionBody(
                            name = moduleName,
                            modifier = KModifier.PRIVATE,
                            parameter = ParameterSpec.builder(FUN_PARAM, Int::class).build(),
                            bodyBlock = generateWhenCodeBlock(getModuleFunctionBody(moduleName, project))
                        )
                        functionList.add(moduleFunSpec)
                    }

                    val file = FileSpec.builder("co.mona.android.applicationHelper", "MonacoResNameConverter.kt")
                        .addImport("co.mona.android", "R")
                        .addImport("co.mona.android.core.base", "ResNameConverter")
                        .addType(generateClass(moduleList, functionList))
                        .build()

                    val sb = StringBuilder("/* ktlint-disable */")
                    sb.appendLine()
                    file.writeTo(sb)
                    sb.appendLine("/* ktlint-enable */")
                    println(sb)
                }
            }
            WriteCommandAction.runWriteCommandAction(project, r)
        } catch (e: Exception) {
            println(e)
        }
    }

    companion object {
        private const val TEMP_DIR = "string_resource_generator"
        private const val FUN_PARAM = "resId"
        private const val CLASS_NAME = "MonacoResNameConverter"
        private const val CLASS_PARAM = "context"
        private const val DEFAULT_ELSE = "context.resources.getResourceEntryName($FUN_PARAM)"
    }
}
