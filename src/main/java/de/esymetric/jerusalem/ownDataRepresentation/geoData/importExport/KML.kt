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
    var description: String? = null
    var trackPositions = Vector<Position>()
    fun CreateXmlDocument(): XMLElement {
        if (name == null || name!!.length == 0) name = "unnamed"

        /*
		 * XmlDocument doc = new XmlDocument(); XmlNode docNode =
		 * doc.CreateXmlDeclaration("1.0", "UTF-8", null);
		 * doc.AppendChild(docNode);
		 */

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
                Double2StringEN(pos.longitude) + ","
                        + Double2StringEN(pos.latitude) + ","
                        + Double2StringEN(pos.altitude) + " "
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

    fun LoadTrack(doc: XMLElement, loadFully: Boolean) {
        // interpret KML content

        /*
		 * String nameSpace = NS; nameSpace = doc.ChildNodes[1].NamespaceURI;
		 *
		 * XmlNamespaceManager xnm = new XmlNamespaceManager(doc.NameTable);
		 * xnm.addNamespace("k", nameSpace);
		 */
        val nameNode = doc.getChildByPath("Document/Placemark/name")
        if (nameNode != null) name = nameNode.content
        val descNode = doc
            .getChildByPath("Document/Placemark/description")
        if (descNode != null) description = descNode.content
        if (loadFully) {
            var coordNode = doc
                .getChildByPath("Document/Placemark/LineString/coordinates")
            if (coordNode == null) coordNode = doc
                .getChildByPath("Document/Placemark/MultiGeometry/LineString/coordinates")
            if (coordNode != null) {
                var coordinates = coordNode.content
                trackPositions.clear()
                if (coordinates != null) {
                    coordinates = coordinates.replace('\n', ' ')
                    val coordinatesSplit = split(coordinates, ' ')
                    for (i in coordinatesSplit.indices) {
                        val positionSplit = split(coordinatesSplit[i], ',')
                        if (positionSplit.size == 3) {
                            val p = Position()
                            p.latitude = String2DoubleEN(positionSplit[1])
                            p.longitude = String2DoubleEN(positionSplit[0])
                            p.altitude = String2DoubleEN(positionSplit[2])
                            trackPositions.add(p)
                        }
                    }
                }
            }
        }
    }

    fun Save(filePath: String?) {
        val fw: FileWriter
        try {
            fw = FileWriter(filePath)
            CreateXmlDocument().write(fw)
            /*
			String t = CreateXmlDocument().toString();
			t = t.replace("<Placemark>", "	<Style id=\"trackStyle\">		<LineStyle color=\"d38b3a1b\" width=\"4\">			<color>ff0000ff</color>			<width>5</width>		</LineStyle>" +
	"</Style>	<StyleMap id=\"trackStyle0\">		<Pair>			<key>normal</key>			<styleUrl>#trackStyle</styleUrl>		</Pair>		<Pair>"+
		"	<key>highlight</key>			<styleUrl>#trackStyle2</styleUrl>		</Pair>	</StyleMap>	<Style id=\"trackStyle1\">	</Style>	<Style id=\"trackStyle2\">" +
	"	<LineStyle color=\"d38b3a1b\" width=\"4\">			<color>ff0000ff</color>			<width>5</width>		</LineStyle>	</Style>	<Placemark>		<styleUrl>#trackStyle0</styleUrl>");
			fw.write(t);*/fw.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        const val SUFFIX = ".kml"

        /*
	 * public static String Double2StringEN(double d) { return
	 * String.format(Locale.ENGLISH, "%.9f", d); }
	 */
        fun String2DoubleEN(s: String): Double {
            return try {
                s.toDouble()
            } catch (e: Exception) {
                0.0
            }
        }

        val double2StringENDecFormat = DecimalFormat(
            "0.000000000", DecimalFormatSymbols(Locale.ENGLISH)
        )

        fun Double2StringEN(d: Double): String {
            return double2StringENDecFormat.format(d)
        }

        fun split(s: String, delimiter: Char): Array<String> {
            var s = s
            val v = Vector<String>()
            while (true) {
                val p = s.indexOf(delimiter)
                s = if (p < 0) {
                    v.add(s)
                    return v.toTypedArray()
                } else {
                    v.add(s.substring(0, p))
                    s.substring(p + 1)
                }
            }
        }
    }
}