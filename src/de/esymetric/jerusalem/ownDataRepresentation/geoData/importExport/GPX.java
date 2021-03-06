package de.esymetric.jerusalem.ownDataRepresentation.geoData.importExport;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Vector;

import nanoxml.XMLElement;
import de.esymetric.jerusalem.ownDataRepresentation.geoData.Position;

public class GPX {
	public final static String SUFFIX = ".gpx";
	public final static long MAX_TICKS = (new GregorianCalendar(2100, 1, 1))
			.getTimeInMillis();
	public final static long MIN_TICKS = (new GregorianCalendar(1900, 1, 1))
			.getTimeInMillis();

	final String NS = "http://www.topografix.com/GPX/1/1";
	final String XSINS = "http://www.w3.org/2001/XMLSchema-instance";
	final String XSISCHEMA = "http://www.topografix.com/GPX/1/1/gpx.xsd";
	public final long SEC2TICKS = 1000L * 1000L * 10L;

	String name;

	public String getName() {
		return name;
	}

	public void setName(String value) {
		name = value;
	}

	String description;

	public String getDescription() {
		return description;
	}

	public void setDescription(String value) {
		description = value;
	}

	Vector<Position> trackPositions = new Vector<Position>();

	public Vector<Position> getTrackPositions() {
		return trackPositions;
	}

	public void setTrackPositions(Vector<Position> value) {
		trackPositions = value;
	}

	Vector<Long> trackPositionTimes;

	public Vector<Long> getTrackPositionTimes() {
		return trackPositionTimes;
	}

	public void setTrackPositionTimes(Vector<Long> value) {
		trackPositionTimes = value;
	}

	Vector<Double> trackPositionSpeeds;

	public Vector<Double> getTrackPositionSpeeds() {
		return trackPositionSpeeds;
	}

	public void setTrackPositionSpeeds(Vector<Double> value) {
		trackPositionSpeeds = value;
	}

	Vector<Placemark> placemarks = new Vector<Placemark>();

	public Vector<Placemark> getPlacemarks() {
		return placemarks;
	}

	public void setPlacemarks(Vector<Placemark> value) {
		placemarks = value;
	}

	Position upperLeftCorner = new Position();

	public Position getUpperLeftCorner() {
		return upperLeftCorner;
	}

	Position lowerRightCorner = new Position();

	public Position getLowerRightCorner() {
		return lowerRightCorner;
	}

	final static SimpleDateFormat df = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

	String makeTag(String tagName, String content) {
		XMLElement e = new XMLElement();
		e.setName(tagName);
		e.setContent(content);
		return e.toString();
	}

	public String Save(String filePath) {

		// erzeugt man zuerst das komplette XML, so kommt es zum
		// Speicherüberlauf

		FileWriter fw = null;
		try {
			fw = new FileWriter(filePath, false);

			fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			fw
					.write("<gpx version=\"1.1\" creator=\"Run.GPS Trainer UV\" "
							+ " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
							+ "schemaLocation=\"http://www.topografix.com/GPX/1/1/gpx.xsd\" "
							+ "xmlns=\"http://www.topografix.com/GPX/1/1\">");

			// metadata

			XMLElement metadataNode = new XMLElement();
			metadataNode.setName("metadata");

			XMLElement nameNode = new XMLElement();
			nameNode.setName("name");
			nameNode.setContent(name);
			metadataNode.addChild(nameNode);

			XMLElement descNode = new XMLElement();
			descNode.setName("desc");
			descNode.setContent(description);
			metadataNode.addChild(descNode);

			XMLElement linkNode = new XMLElement();
			linkNode.setName("link");
			linkNode.setAttribute("href", "http://www.rungps.net");
			metadataNode.addChild(linkNode);

			XMLElement textNode = new XMLElement();
			textNode.setName("text");
			textNode.setContent("Run.GPS by eSymetric GmbH");
			linkNode.addChild(textNode);

			fw.write(metadataNode.toString());

			// track

			if (trackPositions != null && trackPositions.size() > 0) {
				fw.write("<trk>");
				fw.write(makeTag("name", name));
				fw.write("<trkseg>");

				int count = 0;
				for (Position pos : trackPositions) {
					XMLElement trkptNode = new XMLElement();
					trkptNode.setName("trkpt");
					trkptNode.setAttribute("lat", Double2StringEN(pos.latitude));
					trkptNode.setAttribute("lon", Double2StringEN(pos.longitude));

					XMLElement eleNode = new XMLElement();
					eleNode.setName("ele");
					eleNode.setContent(Double2StringEN(pos.altitude));
					trkptNode.addChild(eleNode);

					if (trackPositionTimes != null) {
						long timestamp = trackPositionTimes.elementAt(count);
						if (timestamp > MIN_TICKS && timestamp < MAX_TICKS) {
							XMLElement node = new XMLElement();
							node.setName("time");
							Calendar c = new GregorianCalendar();
							c.setTimeInMillis(timestamp);
							String dtStr = df.format(c.getTime());
							node.setContent(dtStr);
							trkptNode.addChild(node);
						}
					}

					if (trackPositionSpeeds != null) {
						double speed = trackPositionSpeeds.elementAt(count);
						XMLElement node = new XMLElement();
						node.setName("speed");
						node.setContent(Double2StringEN(speed));
						trkptNode.addChild(node);
					}
					fw.write(trkptNode.toString());
					count++;
				}
				fw.write("</trkseg>");
				fw.write("</trk>");
			}

			// waypoints / placemarks

			if (placemarks != null && placemarks.size() > 0) {
				for (Placemark pm : placemarks) {
					XMLElement wptNode = new XMLElement();
					wptNode.setName("wpt");

					wptNode.setAttribute("lat", Double2StringEN(pm.lookAt.latitude));
					wptNode.setAttribute("lon", Double2StringEN(pm.lookAt.longitude));

					nameNode = new XMLElement();
					nameNode.setName("name");
					nameNode.setContent(pm.name);
					wptNode.addChild(nameNode);

					fw.write(wptNode.toString());
				}
			}

			fw.write("</gpx>");

			return null;

		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		} finally {
			try {
				if (fw != null)
					fw.close();
			} catch (IOException e) {
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

	public boolean Load(String filePath) {
		// open xml file

		FileReader fr = null;
		try {
			fr = new FileReader(filePath);
			LineNumberReader lnr = new LineNumberReader(fr);

			IncrementalXMLParser ixr = new IncrementalXMLParser(lnr);

			XMLElement metadataNode = ixr.readElement("metadata", 5000);
			if (metadataNode != null) {

				XMLElement nameNode = metadataNode
						.getChildByPath("metadata/name");
				if (nameNode != null)
					name = nameNode.getContent();
				XMLElement descNode = metadataNode
						.getChildByPath("metadata/desc");
				if (descNode != null)
					description = descNode.getContent();
			}

			// track

			trackPositions.clear();
			lowerRightCorner.latitude = 90;
			lowerRightCorner.longitude = -180;
			upperLeftCorner.latitude = -90;
			upperLeftCorner.longitude = 180;
			long startTimestamp = 0L;
			for (;;) {
				XMLElement n = ixr.readElement("trkpt", 5000);
				if (n == null)
					break;
				if (!"trkpt".equals(n.getName()))
					continue;

				Position p = new Position();
				p.latitude = String2DoubleEN((String) n.getAttribute("lat"));
				p.longitude = String2DoubleEN((String) n.getAttribute("lon"));

				XMLElement eleS = n.getChild("ele");
				if (eleS != null)
					p.altitude = String2DoubleEN(eleS.getContent());

				XMLElement timestampS = n.getChild("time");
				if (timestampS != null) {
					try {
						Date timestamp = df.parse(timestampS.getContent());
						if (startTimestamp == 0L)
							startTimestamp = timestamp.getTime();
						p.elapsedTimeSec = (int) ((timestamp.getTime() - startTimestamp) / SEC2TICKS);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (p.latitude < lowerRightCorner.latitude)
					lowerRightCorner.latitude = p.latitude;
				if (p.longitude < upperLeftCorner.longitude)
					upperLeftCorner.longitude = p.longitude;
				if (p.latitude > upperLeftCorner.latitude)
					upperLeftCorner.latitude = p.latitude;
				if (p.longitude > lowerRightCorner.longitude)
					lowerRightCorner.longitude = p.longitude;
				trackPositions.add(p);

				// waypoints

				/*
				 * not supported at the moment! Vector<XMLElement> wpts =
				 * doc.getChildren(); placemarks.clear(); for (int i = 0; i <
				 * wpts.size(); i++) { XMLElement n = (XMLElement)
				 * trkpts.elementAt(i); if (!"wpt".equals(n.getName()))
				 * continue;
				 * 
				 * Placemark p = new Placemark(); p.lookAt.latitude =
				 * String2DoubleEN((String) n .getAttribute("lat"));
				 * p.lookAt.longitude = String2DoubleEN((String) n
				 * .getAttribute("lon")); p.name =
				 * n.getChild("name").getContent();
				 * 
				 * placemarks.add(p); }
				 */
			}

			lnr.close();
			fr.close();
		} catch (IOException e) {
			return false;
		}

		return true;
	}

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

	public static double String2DoubleEN(String s) {
		try {
			return Double.parseDouble(s);
		} catch (Exception e) {
			return 0.0;
		}
	}

	final static DecimalFormat double2StringENDecFormat = new DecimalFormat("0.000000000", new DecimalFormatSymbols(Locale.ENGLISH));
	
	public static String Double2StringEN(double d) {
		return double2StringENDecFormat.format(d);
	}

}
