package de.esymetric.jerusalem.osmDataRepresentation.unused;



public class OsmNodeID2OwnIDMapMemory implements OsmNodeID2OwnIDMap {
	
	final static int NUMBER_OF_ARRAYS = 128000;
	
	int[][] mapArrays = new int[NUMBER_OF_ARRAYS][];
	
	int minNodeID = 0;
	int maxNodeID = 0;
	int arraySize;
	int numberOfUsedArrays;
	int maxNumberOfArrays;
	
	public int getNumberOfUsedArrays() { return numberOfUsedArrays; }
	
	public OsmNodeID2OwnIDMapMemory(int minNodeID, int maxNodeID, long maxSizeBytes) {
		this.minNodeID = minNodeID;
		this.maxNodeID = maxNodeID;
		
		int maxNodeCount = maxNodeID - minNodeID;
		
		arraySize = (maxNodeCount / NUMBER_OF_ARRAYS) + 1; 
		
		maxNumberOfArrays = (int)(maxSizeBytes / arraySize / 4L);
		
		System.out.println("OsmNodeID2OwnIDMapMemory: maximum number of memory arrays is " + maxNumberOfArrays);
	}
	
	public boolean put(int osmNodeID, int ownNodeID) {
		osmNodeID -= minNodeID;
		int arrayNumber = osmNodeID / arraySize;
		if( mapArrays[arrayNumber] == null )  {
			if( numberOfUsedArrays >= maxNumberOfArrays ) return false;
			mapArrays[arrayNumber] = new int[arraySize];
			numberOfUsedArrays++;
		}
		int arrayIndex = osmNodeID - arrayNumber * arraySize;
		mapArrays[arrayNumber][arrayIndex] = ownNodeID + 1;
		return true;
	}
	
	public int get(int osmNodeID) {
		osmNodeID -= minNodeID;
		int arrayNumber = osmNodeID / arraySize;
		if( mapArrays[arrayNumber] == null ) return -1;
		int arrayIndex = osmNodeID - arrayNumber * arraySize;
		return mapArrays[arrayNumber][arrayIndex] - 1;
	}
	
	public void close() {
		System.out.println("" + numberOfUsedArrays + "/" + NUMBER_OF_ARRAYS + " memory arrays have been used");
	}
	
	public void delete() {
	}
}
