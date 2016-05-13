package de.esymetric.jerusalem.ownDataRepresentation.fileSystem.unused;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.NodeIndexFile;
import de.esymetric.jerusalem.utils.Utils;

public class QuadtreeNodeIndexFile implements NodeIndexFile {

	final static int NUMBER_OF_USED_DIGITS = 6; // if you change this, also
	// change USED_DIGITS_MULT and
	// MAX_SEARCH_RADIUS in
	// NearestNodeFinder
	final static int NUMBER_OF_USED_DIGITS_MULT_2 = NUMBER_OF_USED_DIGITS * 2;
	final static int NUMBER_OF_USED_DIGITS_BEHIND_COMMA = NUMBER_OF_USED_DIGITS - 3;
	final static int USED_DIGITS_MULT = 1000; // 4 digits
	final static double LAT_OFFS = 90.0;
	final static double LNG_OFFS = 180.0;
	final static long SENTENCE_LENGTH = 40L;
	final static int MAX_LAT_INT = 180 * USED_DIGITS_MULT;
	final static int MAX_LNG_INT = 360 * USED_DIGITS_MULT;

	final static int CACHE_KEY_LENGTH = 9;
	final static int MAX_CACHE_SIZE = 32000; // number of entries

	String filePath;
	RandomAccessFile raf; // = new BufferedRandomAccessFileStreamImpl();
	// file too big for bufferedrandomaccessfile
	boolean readOnly = false;
	int numberOfSentences = 0;
	byte[] emptySentence = new byte[40];
	int writeCount;
	int writeCacheHits;
	int readCount;
	int readCacheHits;

	public float getWriteCacheHitRatio() {
		if (writeCount == 0)
			return 0;
		float f = 100.0f * writeCacheHits / writeCount;
		writeCacheHits = 0;
		writeCount = 0;
		return f;
	}

	public float getReadCacheHitRatio() {
		if (readCount == 0)
			return 0;
		float f = 100.0f * readCacheHits / readCount;
		readCacheHits = 0;
		readCount = 0;
		return f;
	}

	public int getCacheSize() {
		return cache.size();
	}

	Map<Integer, Integer> cache = new TreeMap<Integer, Integer>();

	public QuadtreeNodeIndexFile(String dataDirectoryPath, boolean readOnly,
			boolean startNewFile) {
		filePath = dataDirectoryPath + File.separatorChar + "nodeIndex.data";
		this.readOnly = readOnly;

		if (startNewFile) {
			new File(filePath).delete();
			numberOfSentences = 0;
		}

		for (int i = 0; i < 40; i++)
			emptySentence[i] = -1;

		try {
			raf = new RandomAccessFile(filePath, readOnly ? "r" : "rw");
			if (startNewFile)
				insertNewSentence();

			long fileLength = new File(filePath).length();
			numberOfSentences = (int) (fileLength / SENTENCE_LENGTH);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void close() {
		try {
			if (raf != null)
				raf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public int getID(double lat, double lng) {
		int latInt = (int) ((lat + LAT_OFFS) * USED_DIGITS_MULT);
		int lngInt = (int) ((lng + LNG_OFFS) * USED_DIGITS_MULT);

		try {
			return getID(latInt, lngInt);
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

	int getID(int latInt, int lngInt) throws IOException {
		if (numberOfSentences == 0)
			return -1;

		if (latInt < 0 || latInt > MAX_LAT_INT)
			return -1;
		if (lngInt < 0 || lngInt > MAX_LNG_INT)
			return -1;

		readCount++;

		int id = 0;
		int[] keyChain = getKeyChain(latInt, lngInt);
		int shortCacheKey = makeShortCacheKey(keyChain);
		Integer idFromCache = cache.get(shortCacheKey);
		int i = 0;
		if (idFromCache != null) {
			id = idFromCache.intValue();
			i = CACHE_KEY_LENGTH;
			readCacheHits++;
		}

		for (; i < NUMBER_OF_USED_DIGITS_MULT_2; i++) {
			if (i == CACHE_KEY_LENGTH && idFromCache == null) {
				if (cache.size() >= MAX_CACHE_SIZE) {
					cache.clear();
					Runtime.getRuntime().gc();
					System.out.println("emptied node index cache, mem: "
							+ Utils.memInfoStr());
				}
				cache.put(shortCacheKey, id);
			}
			raf.seek((long) id * SENTENCE_LENGTH + (long) keyChain[i] * 4L);
			id = raf.readInt();
			if (id == -1)
				break;
		}

		return id; // the last id is the id referring to a node
	}

	int[] getKeyChain(int latInt, int lngInt) {
		int[] keyChain = new int[NUMBER_OF_USED_DIGITS_MULT_2];
		for (int i = NUMBER_OF_USED_DIGITS_MULT_2 - 2; i >= 0; i -= 2) {
			keyChain[i] = latInt % 10;
			keyChain[i + 1] = lngInt % 10;
			latInt /= 10;
			lngInt /= 10;
		}
		return keyChain;
	}

	int makeShortCacheKey(int[] keyChain) {
		int key = 0;
		for (int i = 0; i < CACHE_KEY_LENGTH; i++) {
			key += keyChain[i];
			key *= 10;
		}
		return key;
	}

	@Override
	public List<NodeIndexNodeDescriptor> getIDPlusSourroundingIDs(double lat,
			double lng, int radius) {
		List<NodeIndexNodeDescriptor> list = new ArrayList<NodeIndexNodeDescriptor>();

		int latInt = (int) ((lat + LAT_OFFS) * USED_DIGITS_MULT);
		int lngInt = (int) ((lng + LNG_OFFS) * USED_DIGITS_MULT);

		try {
			int rSquare = radius * radius;
			for (int x = latInt - radius; x <= latInt + radius; x++)
				for (int y = lngInt - radius; y <= lngInt + radius; y++) {
					int deltaX = x - latInt;
					int deltaY = y - lngInt;
					if (deltaX * deltaX + deltaY * deltaY <= rSquare) {
						NodeIndexNodeDescriptor nind = new NodeIndexNodeDescriptor();
						nind.id = getID(x, y);
						nind.latInt = x / USED_DIGITS_MULT - (int) LAT_OFFS; // not
																				// elegant
						nind.lngInt = y / USED_DIGITS_MULT - (int) LNG_OFFS;
						if (nind.id >= 0)
							list.add(nind);
					}
				}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return list;
	}

	@Override
	public int setID(double lat, double lng, int id) {
		int latInt = (int) ((lat + LAT_OFFS) * USED_DIGITS_MULT);
		int lngInt = (int) ((lng + LNG_OFFS) * USED_DIGITS_MULT);

		try {
			return setID(latInt, lngInt, id);

		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

	int setID(int latInt, int lngInt, int nodeID) throws IOException {

		if (latInt < 0 || latInt > MAX_LAT_INT)
			return -1;
		if (lngInt < 0 || lngInt > MAX_LNG_INT)
			return -1;

		writeCount++;

		int id = 0;
		int[] keyChain = getKeyChain(latInt, lngInt);
		int shortCacheKey = makeShortCacheKey(keyChain);
		Integer idFromCache = cache.get(shortCacheKey);
		int i = 0;
		if (idFromCache != null) {
			id = idFromCache.intValue();
			i = CACHE_KEY_LENGTH;
			writeCacheHits++;
		}

		// read as long as leafs exist

		for (; i < NUMBER_OF_USED_DIGITS_MULT_2 - 1; i++) {
			long pos = (long) id * SENTENCE_LENGTH + (long) keyChain[i] * 4L;
			raf.seek(pos);
			int foundID = raf.readInt();
			if (foundID == -1)
				break;
			id = foundID;
		}

		// add new leafs

		for (; i < NUMBER_OF_USED_DIGITS_MULT_2 - 1; i++) {
			int newID = insertNewSentence();
			if (newID == -1) {
				System.out
						.println("ERROR: cannot create new sentence in QuadtreeNodeIndexFile");
				return -1;
			}
			long pos = (long) id * SENTENCE_LENGTH + (long) keyChain[i] * 4L;
			raf.seek(pos);
			raf.writeInt(newID);
			if (i == CACHE_KEY_LENGTH && idFromCache == null) {
				if (cache.size() >= MAX_CACHE_SIZE) {
					cache.clear();
					Runtime.getRuntime().gc();
					System.out.println("emptied node index cache, mem: "
							+ Utils.memInfoStr());
				}
				cache.put(shortCacheKey, id);
			}
			id = newID;
		}

		// insert nodeID on the last leaf

		long pos = (long) id * SENTENCE_LENGTH + (long) keyChain[i] * 4L;
		raf.seek(pos);
		int oldID = raf.readInt();
		raf.seek(pos);
		raf.writeInt(nodeID);
		return oldID;
	}

	public int insertNewSentence() {
		long fileLength = (long) numberOfSentences * SENTENCE_LENGTH;

		try {
			raf.seek(fileLength);
			raf.write(emptySentence);
			int id = numberOfSentences;
			numberOfSentences++;
			return id;
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

	@Override
	public int getCapacity() {
		return 10 * numberOfSentences;
	}

	@Override
	public int getMaxCacheSize() {
		return MAX_CACHE_SIZE;
	}

}
