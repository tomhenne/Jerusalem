package de.esymetric.jerusalem.osmDataRepresentation

import de.esymetric.jerusalem.utils.Utils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

class OSMDataReader(
    private var inputStream: InputStream,
    private var listener: OSMDataReaderListener, var jumpOverNodes: Boolean
) {
    private var entityCount: Long = 0

    interface OSMDataReaderListener {
        fun foundNode(node: OSMNode)
        fun foundWay(way: OSMWay)
    }

    var buffer = ""
    fun read(startTime: Date): Boolean {
        val lnr = BufferedReader(
            InputStreamReader(
                inputStream
            ), 1_000_000
        )
        try {
            readToTag(lnr, "<osm")
            readToTag(lnr, ">")
            if (jumpOverNodes) {
                while (true) {
                    buffer = lnr.readLine()
                    if (!buffer.contains("<way")) {
                        continue
                    }
                    break
                }
            }
            while (true) {
                if (readToTag(lnr, "<") == null) break
                val xmlNodeName = readToTag(lnr, " ")
                val sb = StringBuilder()
                sb.append(xmlNodeName)
                sb.append(' ')
                val tail = readToTag(lnr, ">")
                sb.append(tail)
                if (tail?.last() != '/')  {
                    sb.append('>')
                    sb.append(readToTag(lnr, "</$xmlNodeName>"))
                    sb.append("</")
                    sb.append(xmlNodeName)
                }
                val content = sb.toString()
                entityCount++
                if (entityCount and 0xFFFFF == 0L) {
                    println(
                        Utils.formatTimeStopWatch(
                            Date().time - startTime.time
                        ) + " " + entityCount + " entities read"
                    )
                    if (entityCount and 0x700000 == 0L) {
                        print("free memory: "
                                    + Runtime.getRuntime().freeMemory() / 1024L / 1024L
                                    + " MB / "
                                    + Runtime.getRuntime().totalMemory() / 1024L / 1024L
                                    + " MB "
                        )
                        System.gc()
                        println(
                            " >>> "
                                    + Runtime.getRuntime().freeMemory() / 1024L / 1024L
                                    + " MB / "
                                    + Runtime.getRuntime().totalMemory() / 1024L / 1024L
                                    + " MB "
                        )
                    }
                }
                if ("node" == xmlNodeName) {
                    makeOSMNode(content)
                    continue
                }
                if ("way" == xmlNodeName) {
                    makeOSMWay(content)
                    continue
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return true
    }

    private fun makeOSMWay(content: String) {
        try {
            val way = OSMWay()
            val xmlNodes = splitXmlNodes(content)
            val attributes = getAttributes(xmlNodes.first())
            way.id = attributes["id"]?.toInt() ?: throw OsmReaderXMLParseException()

            val nodes = mutableListOf<Long>()
            val tags = mutableMapOf<String, String>()

            for (childNode in xmlNodes) {
                if (childNode.startsWith("nd")) {
                    val childAttrib = getAttributes(childNode)
                    childAttrib["ref"]?.toLong()?.let {
                        nodes.add(it)
                    }
                } else
                    if (childNode.startsWith("tag")) {
                        val childAttrib = getAttributes(childNode)
                        tags[childAttrib["k"]!!] =
                            childAttrib["v"]!!
                    }
            }

            way.nodes = nodes.toTypedArray()
            way.tags = tags
            listener.foundWay(way)
        } catch (e: OsmReaderXMLParseException) {
            println("XML Parse Error on: $content")
            e.printStackTrace()
        }
    }

    private fun makeOSMNode(content: String) {
        try {
            val node = OSMNode()
            val xmlNodes = splitXmlNodes(content)
            val attributes = getAttributes(xmlNodes.first())
            node.id = attributes["id"]?.toLong() ?: throw OsmReaderXMLParseException()
            node.lat = attributes["lat"]?.toDouble() ?: throw OsmReaderXMLParseException()
            node.lng = attributes["lon"]?.toDouble() ?: throw OsmReaderXMLParseException()
            listener.foundNode(node)
        } catch (e: OsmReaderXMLParseException) {
            println("XML Parse Error on: $content")
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    fun readToTag(lnr: BufferedReader, tag: String): String? {
        if (buffer.isNotEmpty()) {
            val p = buffer.indexOf(tag)
            if (p >= 0) {
                val head = buffer.substring(0, p)
                val tail = buffer.substring(p + tag.length)
                buffer = tail + '\n'
                return head
            }
        }
        val buf = StringBuffer()
        buf.append(buffer)
        while (true) {
            val line = lnr.readLine() ?: return null
            val p = line.indexOf(tag)
            if (p < 0) {
                buf.append(line)
                buf.append('\n')
            } else {
                val head = line.substring(0, p)
                val tail = line.substring(p + tag.length)
                buf.append(head)
                buffer = tail + '\n'
                return buf.toString()
            }
        }
    }

    private fun splitXmlNodes(content: String): List<String> =
        content.split('<')

    private fun getAttributes(content: String): Map<String, String> {
        var tail = content
        val attrib = mutableMapOf<String, String>()

        while(true) {
            val pBlank = tail.indexOf(' ')
            if (pBlank < 0 ) break;

            val pEquals = tail.indexOf('=')
            if (pEquals < 0 ) break;

            val name = tail.substring(pBlank + 1, pEquals)
            tail = tail.substring(pEquals + 2) // jump over quote

            val pQuote = tail.indexOf('"')
            if (pQuote < 0 ) break;

            val value = tail.substring(0, pQuote)
            tail = tail.substring(pQuote + 1)

            attrib[name] = value
        }

        return attrib
    }
}

class OsmReaderXMLParseException : RuntimeException()
