package com.github.panatchaiv22.stringresourcekeyidgenerator.parsers

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
                val tagname = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> if (tagname.equals("string", ignoreCase = true)) {
                        idList.add((parser as MXParser).getAttributeValue(0))
                    }
                }
                eventType = parser.next()
            }

        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return idList
//        --------------------------------------------------------------------
//        val psiFile = vFile.getPsiFileSafely(project)
//        val viewProvider = psiFile?.viewProvider
//        val xmlFile = viewProvider?.getPsi(XMLLanguage.INSTANCE) as XmlFile?
//        val xDoc: XmlDocument? = xmlFile?.document
//        xDoc?.rootTag?.attributes?.forEach {
//            println("Attr: $it")
//        }
//        return listOf()
//        --------------------------------------------------------------------
//        val idsList = mutableListOf<String>()
//        val xPath = XPathFactory.newInstance().newXPath()
//        val builderFactory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
//        val builder: DocumentBuilder = builderFactory.newDocumentBuilder()
//
//        return try {
//            vFile.inputStream.use { stream ->
//                val stringXml = builder.parse(stream)
//                val nodeList = xPath.evaluate("/resources/string/@name", stringXml, XPathConstants.NODESET) as NodeList
//                for (i in 0 until nodeList.length) {
//                    val node = nodeList.item(i)
//                    idsList.add(node.nodeValue)
//                }
//            }
//            idsList
//        } catch (e: Exception) {
//            // project.logger.error("extraStringResAsWhenCase, no string XML found in module $xmlPath")
//            printlnError("extraStringResAsWhenCase, no string XML found in module ${vFile.name}")
//            idsList
//        }
//        --------------------------------------------------------------------
//        val xmlInputFactory: XMLInputFactory = XMLInputFactory.newInstance()
//        val idsList = mutableListOf<String>()
//
//        vFile.inputStream.use { stream ->
//            val reader: XMLEventReader = xmlInputFactory.createXMLEventReader(stream)
//            while (reader.hasNext()) {
//
//                val nextEvent: XMLEvent = reader.nextEvent()
//                if (nextEvent.isStartElement) {
//                    val startElement: StartElement = nextEvent.asStartElement()
//                    if (startElement.name.localPart.equals("string")) {
//                        val name = startElement.getAttributeByName(QName("name")).value
//                        idsList.add(name)
//                    }
//                }
//            }
//            reader.close()
//        }
//        return idsList
    }
}
