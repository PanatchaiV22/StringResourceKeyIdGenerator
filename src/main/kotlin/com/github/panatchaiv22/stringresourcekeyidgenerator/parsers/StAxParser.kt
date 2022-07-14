package com.github.panatchaiv22.stringresourcekeyidgenerator.parsers

import com.github.panatchaiv22.stringresourcekeyidgenerator.utils.NotificationUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.xmlpull.mxp1.MXParser
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException

class StAxParser {

    fun parse(vFile: VirtualFile, project: Project): List<String> {
        val idList = mutableListOf<String>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(vFile.inputStream, null)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> if (parser.name.equals(STRING_TAG, ignoreCase = true)) {
                        idList.add((parser as MXParser).getAttributeValue(0))
                    }
                }
                eventType = parser.next()
            }
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
            NotificationUtils.notify(e.stackTraceToString(), project)
        } catch (e: IOException) {
            e.printStackTrace()
            NotificationUtils.notify(e.stackTraceToString(), project)
        }
        return idList
    }

    companion object {
        private const val STRING_TAG = "string"
    }
}
