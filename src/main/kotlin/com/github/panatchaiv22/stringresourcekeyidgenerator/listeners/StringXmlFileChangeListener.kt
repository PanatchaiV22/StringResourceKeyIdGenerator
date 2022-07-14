package com.github.panatchaiv22.stringresourcekeyidgenerator.listeners

import com.github.panatchaiv22.stringresourcekeyidgenerator.constants.STRINGS_XML_FILE
import com.github.panatchaiv22.stringresourcekeyidgenerator.generators.KtClassGenerator
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import kotlin.system.measureTimeMillis

class StringXmlFileChangeListener : FileDocumentManagerListener {

    private val ktGenerator = KtClassGenerator()
    private val logger = PluginManager.getInstance().thisLogger()

    override fun beforeDocumentSaving(document: Document) {
        super.beforeDocumentSaving(document)
        val vFile = FileDocumentManager.getInstance().getFile(document)
        if (vFile != null && vFile.name == STRINGS_XML_FILE) {

            ApplicationManager.getApplication().invokeLaterOnWriteThread {
                val elapsed: Long = measureTimeMillis {
                    ktGenerator.generate(vFile, document)
                }
                val msg = "String Resource Generator: $elapsed milliseconds"
                logger.info(msg)
                println(msg)
            }
        }
    }
}
