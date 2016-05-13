package de.esymetric.jerusalem.osmDataRepresentation.unused;


public class OsmNodeID2OwnIDMapCombined implements OsmNodeID2OwnIDMap {

	OsmNodeID2OwnIDMapFile fileMap;
	OsmNodeID2OwnIDMapMemory memMap;
	
	public OsmNodeID2OwnIDMapCombined(String dataDirectoryPath, int
			minNodeID, int maxNodeID, long maxSizeInMemoryBytes) {
		fileMap = new OsmNodeID2OwnIDMapFile(dataDirectoryPath, 
				"osmNodeID2OwnIDMap_partial", minNodeID, maxNodeID);
		memMap = new OsmNodeID2OwnIDMapMemory(minNodeID, maxNodeID, maxSizeInMemoryBytes);
	}
	
	@Override
	public void close() {
		fileMap.close();
		memMap.close();
	}

	@Override
	public void delete() {
		fileMap.delete();
		memMap.delete();
	}

	@Override
	public int get(int osmNodeID) {
		int id = memMap.get(osmNodeID);
		if( id == -1 ) return fileMap.get(osmNodeID);
		else return id;
	}

	@Override
	public boolean put(int osmNodeID, int ownNodeID) {
		if( memMap.put(osmNodeID, ownNodeID) ) return true;
		else
			return fileMap.put(osmNodeID, ownNodeID);
	}

	@Override
	public int getNumberOfUsedArrays() {
		return memMap.getNumberOfUsedArrays();
	}

}
