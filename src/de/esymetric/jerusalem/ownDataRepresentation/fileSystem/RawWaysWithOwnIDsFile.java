package de.esymetric.jerusalem.ownDataRepresentation.fileSystem;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import de.esymetric.jerusalem.osmDataRepresentation.OSMWay;
import de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.MemoryArrayOsmNodeID2OwnIDMap;
import de.esymetric.jerusalem.ownDataRepresentation.Node;
import de.esymetric.jerusalem.rebuilding.Rebuilder;
import de.esymetric.jerusalem.routing.RoutingHeuristics;
import de.esymetric.jerusalem.utils.MemoryEfficientLongToIntMap;
import de.esymetric.jerusalem.utils.Utils;

public class RawWaysWithOwnIDsFile {
	// Aufbau 1 Datensatz: int wayCostIDForward, int wayCostIDBackward, int
	// ownNodeID1, short latLonDirKey1,  ..., int onwNodeIDN, short latLonDirKeyN, -1

	final static String FILENAME = "rawWays.data";

	String filePath, dataDirectoryPath;
	boolean readOnly = false;

	LatLonDir currentLatLonDir = new LatLonDir(-1000, -1000);

	public RawWaysWithOwnIDsFile(String dataDirectoryPath, boolean readOnly) {
		this.dataDirectoryPath = dataDirectoryPath;
		this.readOnly = readOnly;
	}

	public boolean writeWay(OSMWay way,
			MemoryArrayOsmNodeID2OwnIDMap osmID2ownIDMap,
			MemoryEfficientLongToIntMap findNodesNodesCache) {
		LatLonDir lld = new LatLonDir(way.getLatLonDirID(osmID2ownIDMap));
		openOutputStream(lld);

		try {
			daos.writeInt(way.wayCostIDForward);
			daos.writeInt(way.wayCostIDBackward);

			for (long osmNodeID : way.nodes) {
				int nodeID = findNodesNodesCache.get(osmNodeID);
				if (nodeID == -1) {
					System.out.println("RawWaysFile: Error - node ID is -1");
					continue;
				}

				daos.writeInt(nodeID);
				daos.writeShort(osmID2ownIDMap.getShortCellID(osmNodeID));
			}

			daos.writeInt(-1);

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	List<Node> readWay(OSMWay way, LatLonDir lld, PartitionedNodeListFile nlf) {
		List<Node> nodes = new LinkedList<Node>();

		openInputStream(lld);
		if (dain == null)
			return null;

		try {
			if (dain.available() <= 0)
				return null;

			way.wayCostIDForward = dain.readInt();
			way.wayCostIDBackward = dain.readInt();

			for (;;) {
				Node node = new Node();
				node.id = dain.readInt();
				if (node.id == -1)
					break;

				short latLonDirKey = dain.readShort();
				
				node.loadByID(new LatLonDir(latLonDirKey), nlf);
				nodes.add(node);
			}

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return nodes;

	}

	DataOutputStream daos = null;

	public void openOutputStream(LatLonDir newLatLonDir) {
		if (daos != null && newLatLonDir.equals(currentLatLonDir))
			return;

		if (daos != null)
			try {
				daos.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		currentLatLonDir = newLatLonDir;
		filePath = currentLatLonDir.makeDir(dataDirectoryPath, !readOnly)
				+ File.separatorChar + FILENAME;

		try {
			daos = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(filePath, true)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	DataInputStream dain = null;

	public boolean openInputStream(LatLonDir newLatLonDir) {
		if (dain != null && newLatLonDir.equals(currentLatLonDir))
			return true;

		if (dain != null)
			try {
				dain.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		currentLatLonDir = newLatLonDir;
		filePath = currentLatLonDir.makeDir(dataDirectoryPath, !readOnly)
				+ File.separatorChar + FILENAME;

		if (!new File(filePath).exists())
			return false;

		try {
			dain = new DataInputStream(new BufferedInputStream(
					new FileInputStream(filePath)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public void close() {
		if (dain != null)
			try {
				dain.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		if (daos != null)
			try {
				daos.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		dain = null;
		daos = null;
		currentLatLonDir = new LatLonDir(-1000, -1000);
	}

	public void buildTransitions(Date startTime, PartitionedNodeListFile nlf, PartitionedTransitionListFile wlf, RoutingHeuristics routingHeuristics,
			MemoryArrayOsmNodeID2OwnIDMap osmID2ownIDMap) {
		nlf.setMaxFileCacheSize(8);
		// ein Weg kann Nodes aus mehreren benachbarten Sektoren enthalten
		// daher mŸssen diese auch gebuffert werden
		
		File[] files = new File(dataDirectoryPath).listFiles();
		Arrays.sort(files);
		for (File f : files)
			if (f.isDirectory() && f.getName().startsWith("lat_")) {
				for (File g : f.listFiles())
					if (g != null && g.isDirectory()
							&& g.getName().startsWith("lng_")) {
						File[] list = g.listFiles();
						if (list == null) {
							System.out.println("Cannot list files in "
									+ g.getPath());
							continue;
						}
						for (File h : list)
							if (h != null && h.isFile()
									&& h.getName().equals(FILENAME)) {

								int dirLatInt = Integer.parseInt(f.getName()
										.replace("lat_", ""))
										- (int) LatLonDir.LAT_OFFS;
								int dirLngInt = Integer.parseInt(g.getName()
										.replace("lng_", ""))
										- (int) LatLonDir.LNG_OFFS;

								System.out.println("\n"
										+ Utils.FormatTimeStopWatch(new Date()
												.getTime()
												- startTime.getTime())
										+ " building transitions for lat="
										+ dirLatInt + " lng=" + dirLngInt);

								LatLonDir lld = new LatLonDir(dirLatInt,
										dirLngInt);

								for (;;) {
									OSMWay way = new OSMWay();
									List<Node> nodes = readWay(way, lld, nlf);
									if (nodes == null)
										break;

									if (nodes.size() != 0) {
										insertTransitions(way, nodes, nlf, wlf, routingHeuristics, lld);
									}
								}

								close();
							}
					}
				Rebuilder.cleanMem(startTime);
			}
		System.out.println(Utils.FormatTimeStopWatch(new Date().getTime()
				- startTime.getTime())
				+ " finished building transitions");

	}

	public void deleteFiles(Date startTime) {
		System.out.println(Utils.FormatTimeStopWatch(new Date().getTime()
				- startTime.getTime())
				+ " deleting rawWays.data files");
		
		File[] files = new File(dataDirectoryPath).listFiles();
		Arrays.sort(files);
		for (File f : files)
			if (f.isDirectory() && f.getName().startsWith("lat_")) {
				for (File g : f.listFiles())
					if (g != null && g.isDirectory()
							&& g.getName().startsWith("lng_")) {
						File[] list = g.listFiles();
						if (list == null) {
							System.out.println("Cannot list files in "
									+ g.getPath());
							continue;
						}
						for (File h : list)
							if (h != null && h.isFile()
									&& h.getName().equals(FILENAME)) {

								h.delete();
							}
					}
			}

	}

	
	private void insertTransitions(OSMWay way, List<Node> wayNodes, PartitionedNodeListFile nlf, PartitionedTransitionListFile wlf, RoutingHeuristics routingHeuristics,
			LatLonDir lld) {

		for (int i = 0; i < wayNodes.size() - 1; i++) {
			Node nodeA = wayNodes.get(i);
			Node nodeB = wayNodes.get(i + 1);

			short latLonDirKey = lld.getShortKey();
			if (way.wayCostIDForward != -1) {
				
						nodeA.addTransition(nodeB, nlf, wlf, way.wayCostIDForward,
								latLonDirKey,
								routingHeuristics, false);

			}
			if (way.wayCostIDBackward != -1) {
				
				nodeB.addTransition(nodeA, nlf, wlf, way.wayCostIDBackward,
						latLonDirKey,
						routingHeuristics, true);
			}

		}
		wayNodes.clear();
	}

	
	
}
