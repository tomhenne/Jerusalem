package de.esymetric.jerusalem.ownDataRepresentation.fileSystem;

import java.util.List;

public interface NodeIndexFile {
	public class NodeIndexNodeDescriptor {
		public int id;
		public int latInt;
		public int lngInt;
	}
	
	public int setID(double lat, double lng, int id);
	public int getID(double lat, double lng);
	public List<NodeIndexNodeDescriptor> getIDPlusSourroundingIDs(double lat, double lng, int radius);
	public int getCapacity();
	public void close();
	public float getWriteCacheHitRatio();
	public float getReadCacheHitRatio();
	public int getCacheSize();
	public int getMaxCacheSize();
}
