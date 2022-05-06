package de.esymetric.jerusalem.osmDataRepresentation.unused;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class OsmWayID2StartNodeAndEndNodeMap {

	int minWayID = 0;
	int maxWayID = 0;
	
	RandomAccessFile raf;
	
	public OsmWayID2StartNodeAndEndNodeMap(String dataDirectoryPath, int minWayID, int maxWayID) {
		this.minWayID = minWayID;
		this.maxWayID = maxWayID;
		String filePath = dataDirectoryPath +
		File.separatorChar + 
		"osmWayID2StartNodeAndEndNodeMap.data";
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
		System.out.println("Creating new WAY OSM-ID to start and end node map file");
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
	
	public void close() {
		try {
			if ( raf != null ) raf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void delete() {
		// TODO insert code
	}
	
	public long getFileSize() {
		return ((long)maxWayID - (long)minWayID + 1L) * 8L;  // int = 4 bytes
	}
	
	public void put(int osmWayID, int firstNodeID, int lastNodeID) {
		try {
			raf.seek((long)(osmWayID - minWayID) * 8L);
			raf.writeInt(firstNodeID);
			raf.writeInt(lastNodeID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int[] get(int osmWayID) {
		int[] r = new int[2];
		try {
			raf.seek((long)(osmWayID - minWayID) * 8L);
			r[0] = raf.readInt();
			r[1] = raf.readInt();
			return r;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
