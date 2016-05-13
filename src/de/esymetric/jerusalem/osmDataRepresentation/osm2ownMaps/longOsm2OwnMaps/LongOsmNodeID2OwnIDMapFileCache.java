package de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.longOsm2OwnMaps;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class LongOsmNodeID2OwnIDMapFileCache {
	
	int maxSize = 1;  // server: 16  pc: 2
	
	Queue<Integer> queue = new LinkedList<Integer>();
	Map<Integer, LongOsmNodeID2OwnIDMapFile> cache = new HashMap<Integer, LongOsmNodeID2OwnIDMapFile>();
	
	int numberOfAccesses;
	int numberOfFileChanges;
	
	String dataDirectoryPath;
	
	public LongOsmNodeID2OwnIDMapFileCache(String dataDirectoryPath, int maxCacheSize) {
		this.dataDirectoryPath = dataDirectoryPath;
		this.maxSize = maxCacheSize;

		String dirPath = dataDirectoryPath + File.separatorChar + LongOsmNodeID2OwnIDMapFile.DIR;
		//deleteRecursively(dirPath);
		new File(dirPath).mkdir();
	}
	
	public boolean put(long osmNodeID, int ownNodeID) {
		LongOsmNodeID2OwnIDMapFile f1 = getMap(osmNodeID, false);
		return f1.put(osmNodeID, ownNodeID);
		
	}
	

	public int get(long osmNodeID) {
		LongOsmNodeID2OwnIDMapFile f1 = getMap(osmNodeID, true);
		return f1.get(osmNodeID);
	}

	public int getEstimatedMemoryUsedMB() {
		return (int)((long)cache.size() * (long)LongOsmNodeID2OwnIDMapFile.FILE_SIZE / 1024L / 1024L);
	}
	
	
	private LongOsmNodeID2OwnIDMapFile getMap(long osmNodeID, boolean readOnly) {
		numberOfAccesses++;
		
		int fileNumber = LongOsmNodeID2OwnIDMapFile.getFileNumber(osmNodeID);
		
		// get it from cache?
		
		if( cache.containsKey(fileNumber) ) {
			queue.remove(fileNumber);
			queue.add(fileNumber); // insert at end of queue
			return cache.get(fileNumber);
		}
		
		// not in queue
		
		//TODO if( readOnly && !new File(filePath).exists() ) return null;
		
		// remove first element?
		
		LongOsmNodeID2OwnIDMapFile raf = null;
		if( queue.size() >= maxSize ) {
			int fn = queue.poll();
			raf = cache.remove(fn);
			/*
			try {
				raf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			*/
		}
		
			//raf = new RandomAccessFile(filePath, readOnly ? "r" : "rw");
			if( raf == null )
			raf = new LongOsmNodeID2OwnIDMapFile(dataDirectoryPath);
			raf.openFile(fileNumber);
			numberOfFileChanges++;
		
		cache.put(fileNumber, raf);
		queue.add(fileNumber);
		
		return raf;
	}
	
	public int getAndClearNumberOfFileChanges() {
		int n = numberOfFileChanges;
		numberOfFileChanges = 0;
		return n;
	}
	
	public void close() {
		for( LongOsmNodeID2OwnIDMapFile raf : cache.values())
			try {
				raf.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		queue.clear();
		cache.clear();
	}
}
