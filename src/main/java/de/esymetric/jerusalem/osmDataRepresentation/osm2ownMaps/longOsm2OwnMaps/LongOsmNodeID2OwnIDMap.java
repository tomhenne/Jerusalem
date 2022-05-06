package de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.longOsm2OwnMaps;

public interface LongOsmNodeID2OwnIDMap {
	public int getNumberOfUsedArrays();
	
	public boolean put(long osmNodeID, int ownNodeID);
	
	public int get(long osmNodeID);
	
	public void close();

	public void delete();

}
