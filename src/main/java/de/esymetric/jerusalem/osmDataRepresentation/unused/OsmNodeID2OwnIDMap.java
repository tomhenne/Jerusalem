package de.esymetric.jerusalem.osmDataRepresentation.unused;

public interface OsmNodeID2OwnIDMap {
	public int getNumberOfUsedArrays();
	
	public boolean put(int osmNodeID, int ownNodeID);
	
	public int get(int osmNodeID);
	
	public void close();

	public void delete();

}
