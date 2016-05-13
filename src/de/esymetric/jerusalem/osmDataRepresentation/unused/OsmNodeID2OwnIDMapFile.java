package de.esymetric.jerusalem.osmDataRepresentation.unused;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;


public class OsmNodeID2OwnIDMapFile implements OsmNodeID2OwnIDMap {

	final int MAX_CACHE_SIZE = 3000000;  
	
	int minNodeID = 0;
	int maxNodeID = 0;
	
	RandomAccessFile raf;
	
	public OsmNodeID2OwnIDMapFile(String dataDirectoryPath, String name, int minNodeID, int maxNodeID) {
		this.minNodeID = minNodeID;
		this.maxNodeID = maxNodeID;
		String filePath = dataDirectoryPath +
		File.separatorChar + 
		name + ".data";
		File f = new File(filePath);

		if( !f.exists() || f.length() < getFileSize() )
			createEmptyFile(f);
		
		try {
			raf = new RandomAccessFile(f, "rw");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	void createEmptyFile(File f) {
		System.out.println("Creating new NODE OSM-ID to own ID map file");
		byte[] buffer = new byte[16384];
		for( int i = 0; i < buffer.length; i++) buffer[i] = -1;
		
		int count = (int)(getFileSize() / (long)buffer.length) + 1;
		
		try {
			FileOutputStream fos = new FileOutputStream(f);
			for(int i = 0; i < count; i++ )
				fos.write(buffer);
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void copyToMemoryMap(OsmNodeID2OwnIDMap memoryMap) {
		byte[] buf = new byte[4096];
		try {
			int copyCount = 0;
			for (int i = 0; i <= (int) (raf.length() / 4096L); i++) {
				int l = raf.read(buf);
				if( l <= 0 ) break;
				DataInputStream dais = new DataInputStream(
						new ByteArrayInputStream(buf, 0, l));
				
				int indexOffset = i * 1024;
				for (int j = 0; j < l / 4; j++) {

					int ownID = dais.readInt();

					if (ownID != -1) {
						memoryMap.put(indexOffset + j, ownID);
						copyCount++;
					}
				}
				dais.close();
				if (i % 10000 == 0)
					System.out.println("copied " + copyCount + " of "
							+ ((i + 1) * 1024) + " entries to osm2ownID memory map, "
							+ memoryMap.getNumberOfUsedArrays()
							+ " used arrays");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void copyToMemoryMapOld(OsmNodeID2OwnIDMapMemory memoryMap) {
		try {
			int copyCount = 0;
			for(int i = 0; i < (int)(raf.length() / 4L); i++ ) {
				int ownID = raf.readInt();
				
				if( ownID != -1 ) {
					memoryMap.put(i, ownID);
					copyCount++;
				}
				if( i % 1000000 == 0 ) 
					System.out.println("copied " + copyCount + " of " + i + " entries to osm2ownID memory map, " + 
							memoryMap.getNumberOfUsedArrays() + " used arrays");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		memoryMap.close(); // just get info!
	}
	
	public void close() {
		try {
			if ( raf != null ) raf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void delete() {
		// don't delete - reuse
	}
	
	public long getFileSize() {
		return ((long)maxNodeID - (long)minNodeID + 1L) * 4L;  // int = 4 bytes
	}
	
	public boolean put(int osmNodeID, int ownNodeID) {
		try {
			raf.seek((long)(osmNodeID - minNodeID) * 4L);
			raf.writeInt(ownNodeID);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public int get(int osmNodeID) {
		try {
			raf.seek((long)(osmNodeID - minNodeID) * 4L);
			return  raf.readInt();
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

	@Override
	public int getNumberOfUsedArrays() {
		return 0;
	}
}
