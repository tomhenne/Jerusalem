package de.esymetric.jerusalem.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;


public class BufferedRandomAccessFileCache {

	final static int MAX_SIZE = 3;

	private int maxSize = MAX_SIZE;
	public void setMaxCacheSize(int s) {maxSize = s; }

	Queue<String> queue = new LinkedList<String>();
	Map<String, BufferedRandomAccessFile> cache = new HashMap<String, BufferedRandomAccessFile>();

	int numberOfAccesses;
	int numberOfFileChanges;

	public BufferedRandomAccessFile getRandomAccessFile(String filePath, boolean readOnly) {
		numberOfAccesses++;

		// get it from cache?

		if( cache.containsKey(filePath) ) {
			queue.remove(filePath);
			queue.add(filePath); // insert at end of queue
			return cache.get(filePath);
		}

		// not in queue

		if( readOnly && !new File(filePath).exists() ) return null;

		// remove first element?

		BufferedRandomAccessFile raf = null;
		if( queue.size() >= maxSize ) {
			String fp = queue.poll();
			raf = cache.remove(fp);
			// do not close here - file is closed in "open" method

			// now reuse the BufferedRandomAccessFile
		}
		else raf = new BufferedRandomAccessFile();

		try {
			if( !readOnly ) new File(filePath).getParentFile().mkdirs();
			raf.open(filePath, readOnly ? "r" : "rw");
			numberOfFileChanges++;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}

		cache.put(filePath, raf);
		queue.add(filePath);

		return raf;
	}

	public int getAndClearNumberOfFileChanges() {
		int n = numberOfFileChanges;
		numberOfFileChanges = 0;
		return n;
	}

	public void close() {
		for( BufferedRandomAccessFile raf : cache.values())
			try {
				raf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		queue.clear();
		cache.clear();
	}
}
