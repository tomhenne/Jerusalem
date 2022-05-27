package de.esymetric.jerusalem.ownDataRepresentation.geoData.importExport

import de.esymetric.jerusalem.ownDataRepresentation.geoData.Position
import org.w3c.dom.Document
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult


class KML {
    val NS = "http://earth.google.com/kml/2.1"
    var name: String? = null
    var trackPositions = Vector<Position>()
    val documentBuilderFactory = DocumentBuilderFactory.newInstance()
    val documentBuilder = documentBuilderFactory.newDocumentBuilder()

    fun createXmlDocument(): Document {
        if (name == null || name!!.length == 0) name = "unnamed"

        // kml
        val doc = documentBuilder.newDocument()
        val kmlNode = doc.createElement("kml")
        doc.appendChild(kmlNode)
        kmlNode.setAttribute("xmlns", NS)
        kmlNode.setAttribute("creator", "Run.GPS")

        // Document
        val documentNode = doc.createElement("Document")
        kmlNode.appendChild(documentNode)

        // name
        val docNameNode = doc.createElement("name")
        documentNode.appendChild(docNameNode)

        // style
        val styleNode = doc.createElement("Style")
        styleNode.setAttribute("id", "trackStyle")
        documentNode.appendChild(styleNode)

        // LineStyle
        val lineStyleNode = doc.createElement("LineStyle")
        lineStyleNode.setAttribute("color", "ff0000ff")
        lineStyleNode.setAttribute("width", "6")
        styleNode.appendChild(lineStyleNode)
        val lineStylecolor = doc.createElement("color")
        lineStylecolor.textContent = "ff0000ff"
        lineStyleNode.appendChild(lineStylecolor)
        val lineStyleWidth = doc.createElement("width")
        lineStyleWidth.textContent = "6"
        lineStyleNode.appendChild(lineStyleWidth)

        // track
        val placemarkNode = doc.createElement("Placemark")
        documentNode.appendChild(placemarkNode)
        val nameNode = doc.createElement("name")
        placemarkNode.appendChild(nameNode)
        val styleUrlNode = doc.createElement("styleUrl")
        styleUrlNode.textContent = "#trackStyle"
        placemarkNode.appendChild(styleUrlNode)
        val lineStringNode = doc.createElement("LineString")
        placemarkNode.appendChild(lineStringNode)
        val tessellateNode = doc.createElement("tessellate")
        tessellateNode.textContent = "1"
        lineStringNode.appendChild(tessellateNode)
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
        val coordinatesNode = doc.createElement("coordinates")
        coordinatesNode.textContent = coordinatesString.toString()
        lineStringNode.appendChild(coordinatesNode)
        return doc
    }


    fun save(filePath: String?) {
        val transformerFactory = TransformerFactory.newInstance()
        val transformer: Transformer = transformerFactory.newTransformer()
        val domSource = DOMSource(createXmlDocument())
        val streamResult = StreamResult(File(filePath))
        transformer.transform(domSource, streamResult)
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