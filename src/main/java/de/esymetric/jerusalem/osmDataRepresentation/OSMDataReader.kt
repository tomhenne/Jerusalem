package de.esymetric.jerusalem.osmDataRepresentation

import de.esymetric.jerusalem.utils.Utils
import nanoxml.XMLElement
import nanoxml.XMLParseException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

class OSMDataReader(
    var inputStream: InputStream,
    var listener: OSMDataReaderListener, var jumpOverNodes: Boolean
) {
    var entityCount: Long = 0

    interface OSMDataReaderListener {
        fun foundNode(node: OSMNode?)
        fun foundWay(way: OSMWay?)
    }

    var buffer = ""
    fun read(startTime: Date): Boolean {
        val lnr = BufferedReader(
            InputStreamReader(
                inputStream
            ), 100000
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
                var content = "<" + xmlNodeName + " " + readToTag(lnr, ">")
                content += if (!content.endsWith("/"))
                    ">" + readToTag(lnr, "</$xmlNodeName>") + "</" + xmlNodeName + ">"
                else
                    ">"
                entityCount++
                if (entityCount and 0xFFFFF == 0L) {
                    println(
                        Utils.formatTimeStopWatch(
                            Date()
                                .time
                                    - startTime.time
                        )
                                + " " + entityCount + " entities read"
                    )
                    if (entityCount and 0x700000 == 0L) {
                        print(
                            "free memory: "
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
            var xmlNode: XMLElement? = XMLElement()
            xmlNode!!.parseString(content)
            val way = OSMWay()
            way.id = xmlNode.getAttribute("id").toString().toInt()
            way.nodes = ArrayList()
            way.tags = HashMap()
            val enumeration = xmlNode.childrenIterator
            while (enumeration.hasNext()) {
                val child = enumeration.next()
                if ("nd" == child.name) way.nodes!!.add(
                    child.getAttribute("ref")
                        .toString().toLong()
                )
                if ("tag" == child.name)
                    (way.tags as HashMap<String?, String?>)[child.getAttribute("k").toString()] =
                        child.getAttribute("v").toString()
            }
            xmlNode = null
            way.nodes!!.trimToSize()
            listener.foundWay(way)
        } catch (e: XMLParseException) {
            println("XML Parse Error on: $content")
            e.printStackTrace()
        }
    }

    private fun makeOSMNode(content: String) {
        try {
            val xmlNode = XMLElement()
            xmlNode.parseString(content)
            val node = OSMNode()
            node.id = xmlNode.getAttribute("id").toString().toLong()
            node.lat = xmlNode.getAttribute("lat")
                .toString().toDouble()
            node.lng = xmlNode.getAttribute("lon")
                .toString().toDouble()
            listener.foundNode(node)
        } catch (e: XMLParseException) {
            println("XML Parse Error on: $content")
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    fun readToTag(lnr: BufferedReader, tag: String): String? {
        if (buffer.length > 0) {
            val p = buffer.indexOf(tag)
            if (p >= 0) {
                val head = buffer.substring(0, p)
                val tail = buffer.substring(p + tag.length)
                buffer = """
                    $tail
                    
                    """.trimIndent()
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
                buffer = """
                    $tail
                    
                    """.trimIndent()
                return buf.toString()
            }
        }
    }
}