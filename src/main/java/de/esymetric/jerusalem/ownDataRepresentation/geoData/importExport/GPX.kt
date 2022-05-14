package de.esymetric.jerusalem.ownDataRepresentation.geoData.importExport

import de.esymetric.jerusalem.ownDataRepresentation.geoData.Position
import nanoxml.XMLElement
import java.io.FileWriter
import java.io.IOException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class GPX {
    val SEC2TICKS = 1000L * 1000L * 10L
    var name: String? = null
    var description: String? = null
    var trackPositions: Vector<Position>? = Vector()
    var trackPositionTimes: Vector<Long>? = null
    var trackPositionSpeeds: Vector<Double>? = null
    var placemarks: Vector<Placemark>? = Vector()
    var upperLeftCorner = Position()
    var lowerRightCorner = Position()

    fun makeTag(tagName: String?, content: String?): String {
        val e = XMLElement()
        e.name = tagName
        e.content = content
        return e.toString()
    }

    fun save(filePath: String?): String? {

        // erzeugt man zuerst das komplette XML, so kommt es zum
        // Speicher�berlauf
        var fw: FileWriter? = null
        return try {
            fw = FileWriter(filePath, false)
            fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            fw
                .write(
                    "<gpx version=\"1.1\" creator=\"Run.GPS Trainer UV\" "
                            + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                            + "schemaLocation=\"http://www.topografix.com/GPX/1/1/gpx.xsd\" "
                            + "xmlns=\"http://www.topografix.com/GPX/1/1\">"
                )

            // metadata
            val metadataNode = XMLElement()
            metadataNode.name = "metadata"
            var nameNode = XMLElement()
            nameNode.name = "name"
            nameNode.content = name
            metadataNode.addChild(nameNode)
            val descNode = XMLElement()
            descNode.name = "desc"
            descNode.content = description
            metadataNode.addChild(descNode)
            val linkNode = XMLElement()
            linkNode.name = "link"
            linkNode.setAttribute("href", "http://www.rungps.net")
            metadataNode.addChild(linkNode)
            val textNode = XMLElement()
            textNode.name = "text"
            textNode.content = "Run.GPS by eSymetric GmbH"
            linkNode.addChild(textNode)
            fw.write(metadataNode.toString())

            // track
            if (trackPositions != null && trackPositions!!.size > 0) {
                fw.write("<trk>")
                fw.write(makeTag("name", name))
                fw.write("<trkseg>")
                var count = 0
                for (pos in trackPositions!!) {
                    val trkptNode = XMLElement()
                    trkptNode.name = "trkpt"
                    trkptNode.setAttribute("lat", double2StringEN(pos.latitude))
                    trkptNode.setAttribute("lon", double2StringEN(pos.longitude))
                    val eleNode = XMLElement()
                    eleNode.name = "ele"
                    eleNode.content = double2StringEN(pos.altitude)
                    trkptNode.addChild(eleNode)
                    if (trackPositionTimes != null) {
                        val timestamp = trackPositionTimes!!.elementAt(count)
                        if (timestamp > MIN_TICKS && timestamp < MAX_TICKS) {
                            val node = XMLElement()
                            node.name = "time"
                            val c: Calendar = GregorianCalendar()
                            c.timeInMillis = timestamp
                            val dtStr = df.format(c.time)
                            node.content = dtStr
                            trkptNode.addChild(node)
                        }
                    }
                    if (trackPositionSpeeds != null) {
                        val speed = trackPositionSpeeds!!.elementAt(count)
                        val node = XMLElement()
                        node.name = "speed"
                        node.content = double2StringEN(speed)
                        trkptNode.addChild(node)
                    }
                    fw.write(trkptNode.toString())
                    count++
                }
                fw.write("</trkseg>")
                fw.write("</trk>")
            }

            // waypoints / placemarks
            if (placemarks != null && placemarks!!.size > 0) {
                for (pm in placemarks!!) {
                    val wptNode = XMLElement()
                    wptNode.name = "wpt"
                    wptNode.setAttribute("lat", double2StringEN(pm.lookAt.latitude))
                    wptNode.setAttribute("lon", double2StringEN(pm.lookAt.longitude))
                    nameNode = XMLElement()
                    nameNode.name = "name"
                    nameNode.content = pm.name
                    wptNode.addChild(nameNode)
                    fw.write(wptNode.toString())
                }
            }
            fw.write("</gpx>")
            null
        } catch (e: Exception) {
            e.printStackTrace()
            e.message
        } finally {
            try {
                fw?.close()
            } catch (e: IOException) {
            }
        }
    }

    companion object {
        val MAX_TICKS = GregorianCalendar(2100, 1, 1)
            .timeInMillis
        val MIN_TICKS = GregorianCalendar(1900, 1, 1)
            .timeInMillis
        val df = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        )


        val double2StringENDecFormat = DecimalFormat("0.000000000", DecimalFormatSymbols(Locale.ENGLISH))
        fun double2StringEN(d: Double): String {
            return double2StringENDecFormat.format(d)
        }
    }
}