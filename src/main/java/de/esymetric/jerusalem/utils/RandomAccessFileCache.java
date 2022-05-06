package de.esymetric.jerusalem.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class RandomAccessFileCache {

	final static int MAX_SIZE = 200; // was 256

	Queue<String> queue = new LinkedList<String>();
	Map<String, RandomAccessFile> cache = new HashMap<String, RandomAccessFile>();

	int numberOfAccesses;
	int numberOfFileChanges;

	public RandomAccessFile getRandomAccessFile(String filePath,
			boolean readOnly) {
		numberOfAccesses++;

		// get it from cache?

		if (cache.containsKey(filePath)) {
			queue.remove(filePath);
			queue.add(filePath); // insert at end of queue
			return cache.get(filePath);
		}

		// not in queue

		if (readOnly && !new File(filePath).exists())
			return null;

		// remove first element?

		if (queue.size() >= MAX_SIZE) {
			String fp = queue.poll();
			RandomAccessFile raf = cache.remove(fp);
			try {
				raf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(filePath, readOnly ? "r" : "rw");
			numberOfFileChanges++;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out
					.println("FAILURE to open RandomAccessFile, file cache size: "
							+ cache.size());
		}

		if (raf == null) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}

			new File(filePath).delete();

			try {
				raf = new RandomAccessFile(filePath, readOnly ? "r" : "rw");
				numberOfFileChanges++;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.out
						.println("FAILURE(2) to open RandomAccessFile, file cache size: "
								+ cache.size());

				return null;
			}
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
		for (RandomAccessFile raf : cache.values())
			try {
				raf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		queue.clear();
		cache.clear();
	}
}
