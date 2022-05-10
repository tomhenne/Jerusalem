package de.esymetric.jerusalem.ownDataRepresentation.geoData.importExport

import de.esymetric.jerusalem.ownDataRepresentation.geoData.Position
import nanoxml.XMLElement
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.LineNumberReader
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class GPX {
    val NS = "http://www.topografix.com/GPX/1/1"
    val XSINS = "http://www.w3.org/2001/XMLSchema-instance"
    val XSISCHEMA = "http://www.topografix.com/GPX/1/1/gpx.xsd"
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

    fun Save(filePath: String?): String? {

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
                    trkptNode.setAttribute("lat", Double2StringEN(pos.latitude))
                    trkptNode.setAttribute("lon", Double2StringEN(pos.longitude))
                    val eleNode = XMLElement()
                    eleNode.name = "ele"
                    eleNode.content = Double2StringEN(pos.altitude)
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
                        node.content = Double2StringEN(speed)
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
                    wptNode.setAttribute("lat", Double2StringEN(pm.lookAt.latitude))
                    wptNode.setAttribute("lon", Double2StringEN(pm.lookAt.longitude))
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

    /*
	 * public String SaveOld(String filePath) { try { XMLElement doc = new
	 * XMLElement(); if (doc == null) return "Cannot create XML";
	 * XmlUtils.save(doc, filePath); return null; } catch (Exception e) {
	 * e.printStackTrace(); return e.getMessage(); } }
	 *
	 * public XMLElement CreateXmlDocument() { if (name == null || name.length()
	 * == 0) name = "unnamed";
	 *
	 * // gpx
	 *
	 * XMLElement gpxNode = new XMLElement(); gpxNode.setName("gpx");
	 * gpxNode.setAttribute("version", "1.1"); gpxNode.setAttribute("creator",
	 * "Run.GPS"); gpxNode.setAttribute("xmlns:xsi", XSINS);
	 * gpxNode.setAttribute("xsi:schemaLocation", XSISCHEMA); //
	 * doc.addChild(gpxNode);
	 *
	 * // metadata
	 *
	 * XMLElement metadataNode = new XMLElement();
	 * metadataNode.setName("metadata"); gpxNode.addChild(metadataNode);
	 *
	 * XMLElement nameNode = new XMLElement(); nameNode.setName("name");
	 * nameNode.setContent(name); metadataNode.addChild(nameNode);
	 *
	 * XMLElement descNode = new XMLElement(); descNode.setName("desc");
	 * descNode.setContent(description); metadataNode.addChild(descNode);
	 *
	 * XMLElement linkNode = new XMLElement(); linkNode.setName("link");
	 * linkNode.setAttribute("href", "http://www.rungps.net");
	 * metadataNode.addChild(linkNode);
	 *
	 * XMLElement textNode = new XMLElement(); textNode.setName("text");
	 * textNode.setContent("Run.GPS by eSymetric GmbH");
	 * linkNode.addChild(textNode);
	 *
	 * // track
	 *
	 * if (trackPositions != null && trackPositions.size() > 0) { XMLElement
	 * trkNode = new XMLElement(); trkNode.setName("trk");
	 * gpxNode.addChild(trkNode);
	 *
	 * nameNode = new XMLElement(); nameNode.setName("name");
	 * nameNode.setContent(name); trkNode.addChild(nameNode);
	 *
	 * XMLElement trksegNode = new XMLElement(); trksegNode.setName("trkseg");
	 * trkNode.addChild(trksegNode);
	 *
	 * int count = 0; for (Position pos : trackPositions) { XMLElement trkptNode
	 * = new XMLElement(); trkptNode.setName("trkpt");
	 * trkptNode.setAttribute("lat", FormatUtils
	 * .Double2StringEN(pos.latitude)); trkptNode.setAttribute("lon",
	 * FormatUtils .Double2StringEN(pos.longitude));
	 * trksegNode.addChild(trkptNode);
	 *
	 * XMLElement eleNode = new XMLElement(); eleNode.setName("ele");
	 * eleNode.setContent(Double2StringEN(pos.altitude));
	 * trkptNode.addChild(eleNode);
	 *
	 * if (trackPositionTimes != null) { long timestamp =
	 * trackPositionTimes.elementAt(count); if (timestamp > MIN_TICKS &&
	 * timestamp < MAX_TICKS) { XMLElement node = new XMLElement();
	 * node.setName("time"); Calendar c = new GregorianCalendar();
	 * c.setTimeInMillis(timestamp); String dtStr = df.format(c.getTime());
	 * node.setContent(dtStr); trkptNode.addChild(node); } }
	 *
	 * if (trackPositionSpeeds != null) { double speed =
	 * trackPositionSpeeds.elementAt(count); XMLElement node = new XMLElement();
	 * node.setName("speed");
	 * node.setContent(Double2StringEN(speed));
	 * trkptNode.addChild(node); } count++; } }
	 *
	 * // waypoints / placemarks
	 *
	 * if (placemarks != null && placemarks.size() > 0) { for (Placemark pm :
	 * placemarks) { XMLElement wptNode = new XMLElement();
	 * wptNode.setName("wpt"); gpxNode.addChild(wptNode);
	 *
	 * wptNode.setAttribute("lat", FormatUtils
	 * .Double2StringEN(pm.lookAt.latitude)); wptNode.setAttribute("lon",
	 * FormatUtils .Double2StringEN(pm.lookAt.longitude));
	 *
	 * nameNode = new XMLElement(); nameNode.setName("name");
	 * nameNode.setContent(pm.name); wptNode.addChild(nameNode); } }
	 *
	 * return gpxNode; }
	 */
    fun Load(filePath: String?): Boolean {
        // open xml file
        var fr: FileReader? = null
        try {
            fr = FileReader(filePath)
            val lnr = LineNumberReader(fr)
            val ixr = IncrementalXMLParser(lnr)
            val metadataNode = ixr.readElement("metadata", 5000)
            if (metadataNode != null) {
                val nameNode = metadataNode
                    .getChildByPath("metadata/name")
                if (nameNode != null) name = nameNode.content
                val descNode = metadataNode
                    .getChildByPath("metadata/desc")
                if (descNode != null) description = descNode.content
            }

            // track
            trackPositions!!.clear()
            lowerRightCorner.latitude = 90.0
            lowerRightCorner.longitude = -180.0
            upperLeftCorner.latitude = -90.0
            upperLeftCorner.longitude = 180.0
            var startTimestamp = 0L
            while (true) {
                val n = ixr.readElement("trkpt", 5000) ?: break
                if ("trkpt" != n.name) {
                    continue
                }
                val p = Position()
                p.latitude = String2DoubleEN(n.getAttribute("lat") as String)
                p.longitude = String2DoubleEN(n.getAttribute("lon") as String)
                val eleS = n.getChild("ele")
                if (eleS != null) p.altitude = String2DoubleEN(eleS.content)
                val timestampS = n.getChild("time")
                if (timestampS != null) {
                    try {
                        val timestamp = df.parse(timestampS.content)
                        if (startTimestamp == 0L) startTimestamp = timestamp.time
                        p.elapsedTimeSec = ((timestamp.time - startTimestamp) / SEC2TICKS).toInt()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (p.latitude < lowerRightCorner.latitude) lowerRightCorner.latitude = p.latitude
                if (p.longitude < upperLeftCorner.longitude) upperLeftCorner.longitude = p.longitude
                if (p.latitude > upperLeftCorner.latitude) upperLeftCorner.latitude = p.latitude
                if (p.longitude > lowerRightCorner.longitude) lowerRightCorner.longitude = p.longitude
                trackPositions!!.add(p)
            }
            lnr.close()
            fr.close()
        } catch (e: IOException) {
            return false
        }
        return true
    }

    companion object {
        const val SUFFIX = ".gpx"
        val MAX_TICKS = GregorianCalendar(2100, 1, 1)
            .timeInMillis
        val MIN_TICKS = GregorianCalendar(1900, 1, 1)
            .timeInMillis
        val df = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        )

        /*
	 * public boolean Load(String filePath) { // open xml file
	 *
	 * XMLElement doc = new XMLElement(); FileReader fr = null; try { fr = new
	 * FileReader(filePath); doc.parseFromReader(fr); fr.close(); } catch
	 * (Exception e) { return false; }
	 *
	 * // interpret
	 *
	 * Load(doc, true);
	 *
	 * return true; }
	 *
	 * public void Load(XMLElement doc, boolean loadFully) { // interpret GPX
	 * content
	 *
	 * // String nameSpace = NS; // nameSpace = doc.ChildNodes[1].NamespaceURI;
	 *
	 * // XmlNamespaceManager xnm = new XmlNamespaceManager(doc.NameTable); //
	 * xnm.addNamespace("t", nameSpace);
	 *
	 * XMLElement nameNode = doc.getChildByPath("metadata/name"); if (nameNode
	 * != null) name = nameNode.getContent(); XMLElement descNode =
	 * doc.getChildByPath("metadata/desc"); if (descNode != null) description =
	 * descNode.getContent();
	 *
	 * if (loadFully) { // track
	 *
	 * Vector<XMLElement> trkpts = doc.getChildByPath("trk/trkseg")
	 * .getChildren(); trackPositions.clear(); lowerRightCorner.latitude = 90;
	 * lowerRightCorner.longitude = -180; upperLeftCorner.latitude = -90;
	 * upperLeftCorner.longitude = 180; long startTimestamp = 0L; for (int i =
	 * 0; i < trkpts.size(); i++) { XMLElement n = (XMLElement)
	 * trkpts.elementAt(i); if (!"trkpt".equals(n.getName())) continue;
	 *
	 * Position p = new Position(); p.latitude = String2DoubleEN((String)
	 * n.getAttribute("lat")); p.longitude = String2DoubleEN((String)
	 * n.getAttribute("lon"));
	 *
	 * XMLElement eleS = n.getChild("ele"); if (eleS != null) p.altitude =
	 * String2DoubleEN(eleS.getContent());
	 *
	 * XMLElement timestampS = n.getChild("time"); if (timestampS != null) { try
	 * { Date timestamp = df.parse(timestampS.getContent()); if (startTimestamp
	 * == 0L) startTimestamp = timestamp.getTime(); p.elapsedTimeSec = (int)
	 * ((timestamp.getTime() - startTimestamp) / SEC2TICKS); } catch (Exception
	 * e) { e.printStackTrace(); } } if (p.latitude < lowerRightCorner.latitude)
	 * lowerRightCorner.latitude = p.latitude; if (p.longitude <
	 * upperLeftCorner.longitude) upperLeftCorner.longitude = p.longitude; if
	 * (p.latitude > upperLeftCorner.latitude) upperLeftCorner.latitude =
	 * p.latitude; if (p.longitude > lowerRightCorner.longitude)
	 * lowerRightCorner.longitude = p.longitude; trackPositions.add(p); }
	 *
	 * // waypoints
	 *
	 * Vector<XMLElement> wpts = doc.getChildren(); placemarks.clear(); for (int
	 * i = 0; i < wpts.size(); i++) { XMLElement n = (XMLElement)
	 * trkpts.elementAt(i); if (!"wpt".equals(n.getName())) continue;
	 *
	 * Placemark p = new Placemark(); p.lookAt.latitude =
	 * String2DoubleEN((String) n .getAttribute("lat")); p.lookAt.longitude =
	 * String2DoubleEN((String) n .getAttribute("lon")); p.name =
	 * n.getChild("name").getContent();
	 *
	 * placemarks.add(p); } } }
	 */
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

        val double2StringENDecFormat = DecimalFormat("0.000000000", DecimalFormatSymbols(Locale.ENGLISH))
        fun Double2StringEN(d: Double): String {
            return double2StringENDecFormat.format(d)
        }
    }
}