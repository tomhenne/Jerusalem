package de.esymetric.jerusalem.osmDataRepresentation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import nanoxml.XMLElement;
import nanoxml.XMLParseException;
import de.esymetric.jerusalem.utils.Utils;

public class OSMDataReader {

	long entityCount;

	public interface OSMDataReaderListener {
		 void foundNode(OSMNode node);

		 void foundWay(OSMWay way);
	}

	InputStream inputStream;
	OSMDataReaderListener listener;
	String buffer = "";
	boolean jumpOverNodes;

	public OSMDataReader(InputStream inputStream,
			OSMDataReaderListener listener, boolean jumpOverNodes) {
		this.listener = listener;
		this.inputStream = inputStream;
		this.jumpOverNodes = jumpOverNodes;
	}

	public boolean read(Date startTime) {
		BufferedReader lnr = new BufferedReader(new InputStreamReader(
				inputStream), 100000);

		try {
			readToTag(lnr, "<osm");
			readToTag(lnr, ">");

			if (jumpOverNodes) {
				for (;;) {
					buffer = lnr.readLine();
					if (!buffer.contains("<way"))
						continue;
					break;
				}
			}

			for (;;) {
				if (readToTag(lnr, "<") == null)
					break;
				String xmlNodeName = readToTag(lnr, " ");
				String content = "<" + xmlNodeName + " " + readToTag(lnr, ">");
				if (!content.endsWith("/"))
					content += ">" + readToTag(lnr, "</" + xmlNodeName + ">")
							+ "</" + xmlNodeName + ">";
				else
					content += ">";

				entityCount++;
				if ((entityCount & 0xFFFFF) == 0) {
					System.out.println(Utils.formatTimeStopWatch(new Date()
							.getTime()
							- startTime.getTime())
							+ " " + entityCount + " entities read");
					if ((entityCount & 0x700000) == 0) {
						System.out
								.print("free memory: "
										+ (Runtime.getRuntime().freeMemory() / 1024L / 1024L)
										+ " MB / "
										+ (Runtime.getRuntime().totalMemory() / 1024L / 1024L)
										+ " MB ");
						System.gc();
						System.out
								.println(" >>> "
										+ (Runtime.getRuntime().freeMemory() / 1024L / 1024L)
										+ " MB / "
										+ (Runtime.getRuntime().totalMemory() / 1024L / 1024L)
										+ " MB ");
					}
				}

				if ("node".equals(xmlNodeName)) {
					makeOSMNode(content);
					continue;
				}

				if ("way".equals(xmlNodeName)) {
					makeOSMWay(content);
					continue;
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return true;
	}

	private void makeOSMWay(String content) {
		try {
			XMLElement xmlNode = new XMLElement();
			xmlNode.parseString(content);

			OSMWay way = new OSMWay();
			way.id = Integer.parseInt(xmlNode.getAttribute("id").toString());

			way.nodes = new ArrayList<Long>();
			way.tags = new HashMap<String, String>();

			Iterator<XMLElement> enumeration = xmlNode.getChildrenIterator();
			while (enumeration.hasNext()) {
				XMLElement child = enumeration.next();
				if ("nd".equals(child.getName()))
					way.nodes.add(Long.parseLong(child.getAttribute("ref")
							.toString()));

				if ("tag".equals(child.getName()))
					way.tags.put(child.getAttribute("k").toString(), child
							.getAttribute("v").toString());

			}
			xmlNode = null;

			way.nodes.trimToSize();
			listener.foundWay(way);
		} catch (XMLParseException e) {
			System.out.println("XML Parse Error on: " + content);
			e.printStackTrace();
		}
	}

	private void makeOSMNode(String content) {
		try {
			XMLElement xmlNode = new XMLElement();
			xmlNode.parseString(content);

			OSMNode node = new OSMNode();
			node.id = Long.parseLong(xmlNode.getAttribute("id").toString());
			node.lat = Double.parseDouble(xmlNode.getAttribute("lat") 
					.toString());
			node.lng = Double.parseDouble(xmlNode.getAttribute("lon")
					.toString());

			listener.foundNode(node);

		} catch (XMLParseException e) {
			System.out.println("XML Parse Error on: " + content);
			e.printStackTrace();
		}
	}

	String readToTag(BufferedReader lnr, String tag) throws IOException {
		if (buffer.length() > 0) {
			int p = buffer.indexOf(tag);
			if (p >= 0) {
				String head = buffer.substring(0, p);
				String tail = buffer.substring(p + tag.length());
				buffer = tail + '\n';
				return head;
			}
		}

		StringBuffer buf = new StringBuffer();
		buf.append(buffer);
		for (;;) {
			String line = lnr.readLine();
			if (line == null)
				return null;
			int p = line.indexOf(tag);
			if (p < 0) {
				buf.append(line);
				buf.append('\n');
			} else {
				String head = line.substring(0, p);
				String tail = line.substring(p + tag.length());
				buf.append(head);
				buffer = tail + '\n';
				return buf.toString();
			}
		}
	}

}
