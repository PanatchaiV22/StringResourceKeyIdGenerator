package com.github.panatchaiv22.stringresourcekeyidgenerator.listeners

import com.github.panatchaiv22.stringresourcekeyidgenerator.constants.STRINGS_XML_FILE
import com.github.panatchaiv22.stringresourcekeyidgenerator.generators.KtClassGenerator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener

class MyApplicationListener : FileDocumentManagerListener {

    private val ktGenerator = KtClassGenerator()

    override fun beforeDocumentSaving(document: Document) {
        super.beforeDocumentSaving(document)
        val vFile = FileDocumentManager.getInstance().getFile(document)
        if (vFile != null && vFile.name == STRINGS_XML_FILE) {

//            val project = ProjectLocator.getInstance().getProjectsForFile(vFile).first()
//            val files: Array<PsiFileSystemItem> =
//                FilenameIndex.getFilesByName(project, STRINGS_XML_FILE, GlobalSearchScope.projectScope(project), false)
//            files.forEach {
//                println("Module: ${it.module?.name} --- File: ${it.virtualFile.name}")
//            }

            ApplicationManager.getApplication().invokeLaterOnWriteThread {
//                val elapsed: Long = measureTimeMillis {
                ktGenerator.generate(vFile, document)
//                }
//                println("Elapsed Time: $elapsed milliseconds")
            }
        }

//        println(StringBuilder().apply {
//            append("A VirtualFile is about to be saved\n")
//            append("\tvFile: $vFile\n")
//            append("\tdocument: $document\n")
//            append("\tname: ${vFile?.name}\n")
//        })
    }
}
