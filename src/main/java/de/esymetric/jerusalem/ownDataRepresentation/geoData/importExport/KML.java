package de.esymetric.jerusalem.ownDataRepresentation.geoData.importExport;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Vector;

import nanoxml.XMLElement;
import de.esymetric.jerusalem.ownDataRepresentation.geoData.Position;

public class KML {
	public static final String SUFFIX = ".kml";

	final String NS = "http://earth.google.com/kml/2.1";

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

	public XMLElement CreateXmlDocument() {
		if (name == null || name.length() == 0)
			name = "unnamed";

		/*
		 * XmlDocument doc = new XmlDocument(); XmlNode docNode =
		 * doc.CreateXmlDeclaration("1.0", "UTF-8", null);
		 * doc.AppendChild(docNode);
		 */

		// kml
		XMLElement kmlNode = new XMLElement();
		kmlNode.setName("kml");
		kmlNode.setAttribute("xmlns", NS);
		kmlNode.setAttribute("creator", "Run.GPS");

		// Document

		XMLElement documentNode = new XMLElement();
		documentNode.setName("Document");
		kmlNode.addChild(documentNode);

		// name

		XMLElement docNameNode = new XMLElement();
		docNameNode.setName("name");
		documentNode.addChild(docNameNode);

		// style

		XMLElement styleNode = new XMLElement();
		styleNode.setName("Style");
		styleNode.setAttribute("id", "trackStyle");
		documentNode.addChild(styleNode);

		// LineStyle

		XMLElement lineStyleNode = new XMLElement();
		lineStyleNode.setName("LineStyle");
		lineStyleNode.setAttribute("color", "ff0000ff");
		lineStyleNode.setAttribute("width", "6");
		styleNode.addChild(lineStyleNode);
		
		XMLElement lineStylecolor = new XMLElement();
		lineStylecolor.setName("color");
		lineStylecolor.setContent("ff0000ff");
		lineStyleNode.addChild(lineStylecolor);		

		XMLElement lineStyleWidth= new XMLElement();
		lineStyleWidth.setName("width");
		lineStyleWidth.setContent("6");
		lineStyleNode.addChild(lineStyleWidth);		

		// track

		XMLElement placemarkNode = new XMLElement();
		placemarkNode.setName("Placemark");
		documentNode.addChild(placemarkNode);

		XMLElement nameNode = new XMLElement();
		nameNode.setName("name");
		placemarkNode.addChild(nameNode);

		XMLElement styleUrlNode = new XMLElement();
		styleUrlNode.setName("styleUrl");
		styleUrlNode.setContent("#trackStyle");
		placemarkNode.addChild(styleUrlNode);

		XMLElement lineStringNode = new XMLElement();
		lineStringNode.setName("LineString");
		placemarkNode.addChild(lineStringNode);

		XMLElement tessellateNode = new XMLElement();
		tessellateNode.setName("tessellate");
		tessellateNode.setContent("1");
		lineStringNode.addChild(tessellateNode);

		StringBuilder coordinatesString = new StringBuilder();
		int count = 0;
		for (Position pos : trackPositions) {
			coordinatesString.append(Double2StringEN(pos.longitude) + ","
					+ Double2StringEN(pos.latitude) + ","
					+ Double2StringEN(pos.altitude) + " ");
			count++;
			if (count % 10 == 0)
				coordinatesString.append('\n');
		}

		XMLElement coordinatesNode = new XMLElement();
		coordinatesNode.setName("coordinates");
		coordinatesNode.setContent(coordinatesString.toString());
		lineStringNode.addChild(coordinatesNode);

		return kmlNode;
	}

	public void LoadTrack(XMLElement doc, boolean loadFully) {
		// interpret KML content

		/*
		 * String nameSpace = NS; nameSpace = doc.ChildNodes[1].NamespaceURI;
		 * 
		 * XmlNamespaceManager xnm = new XmlNamespaceManager(doc.NameTable);
		 * xnm.addNamespace("k", nameSpace);
		 */

		XMLElement nameNode = doc.getChildByPath("Document/Placemark/name");
		if (nameNode != null)
			name = nameNode.getContent();
		XMLElement descNode = doc
				.getChildByPath("Document/Placemark/description");
		if (descNode != null)
			description = descNode.getContent();

		if (loadFully) {
			XMLElement coordNode = doc
					.getChildByPath("Document/Placemark/LineString/coordinates");
			if (coordNode == null)
				coordNode = doc
						.getChildByPath("Document/Placemark/MultiGeometry/LineString/coordinates");

			if (coordNode != null) {

				String coordinates = coordNode.getContent();
				trackPositions.clear();
				if (coordinates != null) {
					coordinates = coordinates.replace('\n', ' ');
					String[] coordinatesSplit = split(coordinates, ' ');
					for (int i = 0; i < coordinatesSplit.length; i++) {
						String[] positionSplit = split(coordinatesSplit[i], ',');
						if (positionSplit.length == 3) {
							Position p = new Position();
							p.latitude = String2DoubleEN(positionSplit[1]);
							p.longitude = String2DoubleEN(positionSplit[0]);
							p.altitude = String2DoubleEN(positionSplit[2]);
							trackPositions.add(p);
						}
					}
				}
			}
		}
	}

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

	final static DecimalFormat double2StringENDecFormat = new DecimalFormat(
			"0.000000000", new DecimalFormatSymbols(Locale.ENGLISH));

	public static String Double2StringEN(double d) {
		return double2StringENDecFormat.format(d);
	}

	public static String[] split(String s, char delimiter) {
		Vector<String> v = new Vector<String>();
		for (;;) {
			int p = s.indexOf(delimiter);
			if (p < 0) {
				v.add(s);
				return v.toArray(new String[0]);
			} else {
				v.add(s.substring(0, p));
				s = s.substring(p + 1);
			}
		}
	}

	public void Save(String filePath) {
		FileWriter fw;
		try {
			fw = new FileWriter(filePath);
			CreateXmlDocument().write(fw);
			/*
			String t = CreateXmlDocument().toString();
			t = t.replace("<Placemark>", "	<Style id=\"trackStyle\">		<LineStyle color=\"d38b3a1b\" width=\"4\">			<color>ff0000ff</color>			<width>5</width>		</LineStyle>" +
	"</Style>	<StyleMap id=\"trackStyle0\">		<Pair>			<key>normal</key>			<styleUrl>#trackStyle</styleUrl>		</Pair>		<Pair>"+
		"	<key>highlight</key>			<styleUrl>#trackStyle2</styleUrl>		</Pair>	</StyleMap>	<Style id=\"trackStyle1\">	</Style>	<Style id=\"trackStyle2\">" +
	"	<LineStyle color=\"d38b3a1b\" width=\"4\">			<color>ff0000ff</color>			<width>5</width>		</LineStyle>	</Style>	<Placemark>		<styleUrl>#trackStyle0</styleUrl>");
			fw.write(t);*/
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
