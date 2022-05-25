package de.esymetric.jerusalem.ownDataRepresentation.geoData.importExport

import de.esymetric.jerusalem.ownDataRepresentation.geoData.Position
import nanoxml.XMLElement
import java.io.FileWriter
import java.io.IOException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class KML {
    val NS = "http://earth.google.com/kml/2.1"
    var name: String? = null
    var trackPositions = Vector<Position>()

    fun createXmlDocument(): XMLElement {
        if (name == null || name!!.length == 0) name = "unnamed"

        // kml
        val kmlNode = XMLElement()
        kmlNode.name = "kml"
        kmlNode.setAttribute("xmlns", NS)
        kmlNode.setAttribute("creator", "Run.GPS")

        // Document
        val documentNode = XMLElement()
        documentNode.name = "Document"
        kmlNode.addChild(documentNode)

        // name
        val docNameNode = XMLElement()
        docNameNode.name = "name"
        documentNode.addChild(docNameNode)

        // style
        val styleNode = XMLElement()
        styleNode.name = "Style"
        styleNode.setAttribute("id", "trackStyle")
        documentNode.addChild(styleNode)

        // LineStyle
        val lineStyleNode = XMLElement()
        lineStyleNode.name = "LineStyle"
        lineStyleNode.setAttribute("color", "ff0000ff")
        lineStyleNode.setAttribute("width", "6")
        styleNode.addChild(lineStyleNode)
        val lineStylecolor = XMLElement()
        lineStylecolor.name = "color"
        lineStylecolor.content = "ff0000ff"
        lineStyleNode.addChild(lineStylecolor)
        val lineStyleWidth = XMLElement()
        lineStyleWidth.name = "width"
        lineStyleWidth.content = "6"
        lineStyleNode.addChild(lineStyleWidth)

        // track
        val placemarkNode = XMLElement()
        placemarkNode.name = "Placemark"
        documentNode.addChild(placemarkNode)
        val nameNode = XMLElement()
        nameNode.name = "name"
        placemarkNode.addChild(nameNode)
        val styleUrlNode = XMLElement()
        styleUrlNode.name = "styleUrl"
        styleUrlNode.content = "#trackStyle"
        placemarkNode.addChild(styleUrlNode)
        val lineStringNode = XMLElement()
        lineStringNode.name = "LineString"
        placemarkNode.addChild(lineStringNode)
        val tessellateNode = XMLElement()
        tessellateNode.name = "tessellate"
        tessellateNode.content = "1"
        lineStringNode.addChild(tessellateNode)
        val coordinatesString = StringBuilder()
        var count = 0
        for (pos in trackPositions) {
            coordinatesString.append(
                double2StringEN(pos.longitude) + ","
                        + double2StringEN(pos.latitude) + ","
                        + double2StringEN(pos.altitude) + " "
            )
            count++
            if (count % 10 == 0) coordinatesString.append('\n')
        }
        val coordinatesNode = XMLElement()
        coordinatesNode.name = "coordinates"
        coordinatesNode.content = coordinatesString.toString()
        lineStringNode.addChild(coordinatesNode)
        return kmlNode
    }


    fun save(filePath: String?) {
        val fw: FileWriter
        try {
            fw = FileWriter(filePath)
            createXmlDocument().write(fw)
            fw.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {

        val double2StringENDecFormat = DecimalFormat(
            "0.000000000", DecimalFormatSymbols(Locale.ENGLISH)
        )

        fun double2StringEN(d: Double): String {
            return double2StringENDecFormat.format(d)
        }

    }
}