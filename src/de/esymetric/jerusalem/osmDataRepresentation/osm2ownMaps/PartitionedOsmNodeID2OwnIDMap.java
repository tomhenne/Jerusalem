package de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.longOsm2OwnMaps.LongOsmNodeID2OwnIDMapFileCache;
import de.esymetric.jerusalem.osmDataRepresentation.unused.LongOsmNodeID2OwnIDMapFileBased;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir;
import de.esymetric.jerusalem.utils.FileBasedHashMap;
import de.esymetric.jerusalem.utils.FileBasedHashMapForLongKeys;
import de.esymetric.jerusalem.utils.Utils;

public class PartitionedOsmNodeID2OwnIDMap {

	OsmNodeID2CellIDMapMemory cellMap;
	FileBasedHashMapForLongKeys currentHashMap = new FileBasedHashMapForLongKeys();
	//LongOsmNodeID2OwnIDMapFileCache osm2ownMap;
	LatLonDir currentHashMapLatLonDir;
	String dataDirectoryPath;
	boolean readOnly;

	// FileBasedHashMapCache cache = new FileBasedHashMapCache();

	public PartitionedOsmNodeID2OwnIDMap(String dataDirectoryPath,
			boolean readOnly) {
		cellMap = new OsmNodeID2CellIDMapMemory();
		this.dataDirectoryPath = dataDirectoryPath;
		this.readOnly = readOnly;
		
		//osm2ownMap = new LongOsmNodeID2OwnIDMapFileCache(dataDirectoryPath);
	}

	public float getAvgGetAccessNumberOfReads() {
		return currentHashMap.getAvgGetAccessNumberOfReads();
		//return 0f;
	}
	
	public int getAndClearNumberOfFileChanges() {
		return currentHashMap.getAndClearNumberOfFileChanges();
		//return 0;
	}

	public void deleteLatLonTempFiles() {
		for (File f : new File(dataDirectoryPath).listFiles())
			if (f.isDirectory() && f.getName().startsWith("lat_"))
				for (File g : f.listFiles())
					if (g.isDirectory() && g.getName().startsWith("lng_"))
						for (File h : g.listFiles())
							if (h.isFile() && h.getName().equals("osmMap.data"))
								h.delete();

	}

	public void put(double lat, double lng, long osmID, int ownID) {
		LatLonDir lld = new LatLonDir(lat, lng);
		if (currentHashMapLatLonDir == null
				|| !lld.equals(currentHashMapLatLonDir)) {
			currentHashMapLatLonDir = lld;

			currentHashMap.open(lld.makeDir(dataDirectoryPath, true)
					+ File.separatorChar + "osmMap.data", readOnly);
		}

		cellMap.put(osmID, lld);
		currentHashMap.put(osmID, ownID);
		//osm2ownMap.put(osmID, ownID);
	}

	public void loadExistingOsm2OwnIDIntoMemory(Date startTime) {
		int count = 1;
		File[] files = new File(dataDirectoryPath).listFiles();
		Arrays.sort(files);
		for (File f : files)
			if (f != null && f.isDirectory() && f.getName().startsWith("lat_")) {
				System.out.println(Utils.FormatTimeStopWatch(new Date()
						.getTime()
						- startTime.getTime())
						+ "  >>> "
						+ f.getName()
						+ " # "
						+ count++
						+ ", cellmap uses "
						+ cellMap.getNumberOfUsedArrays()
						+ "/" + cellMap.getMaxNumberOfArrays() + " arrays");

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
									&& h.getName().equals("osmMap.data")) {

								double lat = Integer.parseInt(f.getName()
										.substring(4))
										- LatLonDir.LAT_OFFS;
								double lng = Integer.parseInt(g.getName()
										.substring(4))
										- LatLonDir.LNG_OFFS;
								LatLonDir lld = new LatLonDir(lat, lng);
								short cellID = lld.getShortKey();

								try {
									int l = (int) (h.length() / 12L);

									FileInputStream fis = new FileInputStream(h);
									DataInputStream dis = new DataInputStream(
											new BufferedInputStream(fis, 100000));

									for (int i = 0; i < l; i++) {
										int osmNodeID = dis.readInt();
										// dis.skipBytes(8);
										dis.readLong();
										if (osmNodeID > 0)
											cellMap.put(osmNodeID, cellID);
									}

									dis.close();
									fis.close();
								} catch (FileNotFoundException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
					}
			}

	}

	public LatLonDir getLatLonDir(int osmNodeID) {
		return new LatLonDir(cellMap.getShort(osmNodeID));
	}

	public short getShortCellID(long osmNodeID) {
		return cellMap.getShort(osmNodeID);
	}

	public int get(long osmNodeID) {
		LatLonDir lld = new LatLonDir(cellMap.getShort(osmNodeID));
		if (currentHashMapLatLonDir == null
				|| !lld.equals(currentHashMapLatLonDir)) {
			String nextFilePath = lld.makeDir(dataDirectoryPath, false)
					+ File.separatorChar + "osmMap.data";
			if (new File(nextFilePath).exists()) {
				currentHashMapLatLonDir = lld;
				currentHashMap.open(nextFilePath, readOnly);
			} else
				return -1;
		}

		return currentHashMap.get(osmNodeID);
		//return osm2ownMap.get(osmNodeID);
	}

	public void close() {
		currentHashMap.close();
		//osm2ownMap.close();
		// System.out.println("PartitionedOsmNodeID2OwnIDMap: deleting temporary files");
		// deleteLatLonTempFiles();
	}

	public void setReadOnly() {
		currentHashMap.close();
		//osm2ownMap.close();
		
		// currentHashMap = new
		currentHashMapLatLonDir = null;
		readOnly = true;
	}

	public int getNumberOfUsedArrays() {
		return cellMap.getNumberOfUsedArrays();
	}

	public int getMaxNumberOfArrays() {
		return cellMap.getMaxNumberOfArrays();
	}

	public float getEstimatedMemorySizeMB() {
		return cellMap.getEstimatedMemorySizeMB();
	}
}
