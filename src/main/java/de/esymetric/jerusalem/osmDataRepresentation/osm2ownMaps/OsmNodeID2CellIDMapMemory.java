package de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps;

import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir;

import java.io.*;

public class OsmNodeID2CellIDMapMemory {

	final static int INITIAL_NUMBER_OF_ARRAYS = 128000;
	final static int NUMBER_OF_ARRAYS_INCREMENT = 16000;
	final static long HIGHEST_OSM_NODE_ID = 2200000000L; // just an estimate

	short[][] mapArrays = new short[INITIAL_NUMBER_OF_ARRAYS][];

	
	int arraySize;
	int numberOfUsedArrays;

	public int getNumberOfUsedArrays() {
		return numberOfUsedArrays;
	}

	public int getMaxNumberOfArrays() {
		return mapArrays.length;
	}

	public OsmNodeID2CellIDMapMemory() {
		arraySize = (int) (HIGHEST_OSM_NODE_ID / (long) INITIAL_NUMBER_OF_ARRAYS) + 1;
	}

	public boolean put(long osmNodeID, LatLonDir lld) {
		return put(osmNodeID, lld.getShortKey());
	}

	void increaseArray(int requiredSize) {
		while (mapArrays.length < requiredSize) {
			short[][] newMapArrays = new short[mapArrays.length
					+ NUMBER_OF_ARRAYS_INCREMENT][];
			System.arraycopy(mapArrays, 0, newMapArrays, 0, mapArrays.length);
			mapArrays = newMapArrays;
			System.out.print("o2o+");
		}
	}

	boolean put(long osmNodeID, short ownNodeID) {
		int arrayNumber = (int) (osmNodeID / (long) arraySize);
		increaseArray(arrayNumber + 1);
		if (mapArrays[arrayNumber] == null) {
			mapArrays[arrayNumber] = new short[arraySize];
			numberOfUsedArrays++;
		}
		int arrayIndex = (int) (osmNodeID - (long) arrayNumber
				* (long) arraySize);
		mapArrays[arrayNumber][arrayIndex] = (short) (ownNodeID + 1);
		return true;
	}

	public LatLonDir get(long osmNodeID) {
		return new LatLonDir(getShort(osmNodeID));
	}

	short getShort(long osmNodeID) {
		int arrayNumber = (int) (osmNodeID / (long) arraySize);
		if (mapArrays.length <= arrayNumber)
			return -1;
		if (mapArrays[arrayNumber] == null)
			return -1;
		int arrayIndex = (int) (osmNodeID - (long) arrayNumber
				* (long) arraySize);
		return (short) (mapArrays[arrayNumber][arrayIndex] - 1);
	}

	public float getEstimatedMemorySizeMB() {
		return (float) arraySize * 2.0f * (float) numberOfUsedArrays / 1024.0f
				/ 1024.0f;
	}

	public boolean save(String filePath) {
		try {
			DataOutputStream dos = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(filePath, false),
							32000));

			dos.writeInt(mapArrays.length);

			for (short[] array : mapArrays) {
				if( array == null ) {
					dos.writeInt(0);
					continue;
				}
				
				dos.writeInt(array.length);
				for (short s : array)
					dos.writeShort(s);
			}

			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;

	}

	public boolean load(String filePath) {
		try {
			DataInputStream dis = new DataInputStream(
					new BufferedInputStream(new FileInputStream(filePath),
							32000));

			int nrArrays = dis.readInt();
			mapArrays = new short[nrArrays][];

			for( int i = 0; i < nrArrays; i++) {
				int l = dis.readInt();
				if( l == 0 ) continue;
				
				short[] array = mapArrays[i] = new short[l];
				
				for( int j = 0; j < l; j++) {
					array[j] = dis.readShort();
				}
				
			}

			dis.close();

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;

	}

}
