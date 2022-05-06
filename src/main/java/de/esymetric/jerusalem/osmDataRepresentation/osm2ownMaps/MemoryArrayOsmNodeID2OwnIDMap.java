package de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps;

import java.io.File;
import java.util.Date;

import de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.longOsm2OwnMaps.LongOsmNodeID2OwnIDMapFileCache;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir;

public class MemoryArrayOsmNodeID2OwnIDMap {

	final static String CELL_MAP_FILENAME = "cellMap.data";
	
	OsmNodeID2CellIDMapMemory cellMap;
	LongOsmNodeID2OwnIDMapFileCache osm2ownMap;
	String dataDirectoryPath;
	boolean readOnly;

	public MemoryArrayOsmNodeID2OwnIDMap(String dataDirectoryPath,
			int maxOsm2OwnMapCacheSize, boolean readOnly) {
		cellMap = new OsmNodeID2CellIDMapMemory();
		this.dataDirectoryPath = dataDirectoryPath;
		this.readOnly = readOnly;
		
		osm2ownMap = new LongOsmNodeID2OwnIDMapFileCache(dataDirectoryPath, maxOsm2OwnMapCacheSize);
	}

	public float getAvgGetAccessNumberOfReads() {
		return 0f;
	}
	
	public int getAndClearNumberOfFileChanges() {
		return 0;
	}

	public void put(double lat, double lng, long osmID, int ownID) {
		LatLonDir lld = new LatLonDir(lat, lng);

		cellMap.put(osmID, lld);
		osm2ownMap.put(osmID, ownID);
	}

	public void loadExistingOsm2OwnIDIntoMemory(Date startTime) {
		
		String filePath = dataDirectoryPath + File.separatorChar + CELL_MAP_FILENAME;
		cellMap.load(filePath);
	}

	public void persistCellMap(Date startTime) {
		
		String filePath = dataDirectoryPath + File.separatorChar + CELL_MAP_FILENAME;
		cellMap.save(filePath);
	}

	public LatLonDir getLatLonDir(int osmNodeID) {
		return new LatLonDir(cellMap.getShort(osmNodeID));
	}

	public short getShortCellID(long osmNodeID) {
		return cellMap.getShort(osmNodeID);
	}

	public int get(long osmNodeID) {
		return osm2ownMap.get(osmNodeID);
	}

	public void close() {
		osm2ownMap.close();
		
	}

	public void setReadOnly() {
		osm2ownMap.close();
		
		readOnly = true;
	}

	public int getNumberOfUsedArrays() {
		return cellMap.getNumberOfUsedArrays();
	}

	public int getMaxNumberOfArrays() {
		return cellMap.getMaxNumberOfArrays();
	}

	public float getEstimatedMemorySizeMB() {
		return cellMap.getEstimatedMemorySizeMB() + osm2ownMap.getEstimatedMemoryUsedMB();
	}
}
