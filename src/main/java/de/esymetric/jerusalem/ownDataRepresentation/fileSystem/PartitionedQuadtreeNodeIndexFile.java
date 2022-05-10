package de.esymetric.jerusalem.ownDataRepresentation.fileSystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import de.esymetric.jerusalem.ownDataRepresentation.Node;
import de.esymetric.jerusalem.rebuilding.Rebuilder;
import de.esymetric.jerusalem.utils.BufferedRandomAccessFile;
import de.esymetric.jerusalem.utils.BufferedRandomAccessFileCache;
import de.esymetric.jerusalem.utils.Utils;

public class PartitionedQuadtreeNodeIndexFile implements NodeIndexFile {

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
	final static long NODELIST_SENTENCE_LENGTH = 8L; // int nodeID, int next
	final static int MAX_LAT_INT = 180 * USED_DIGITS_MULT;
	final static int MAX_LNG_INT = 360 * USED_DIGITS_MULT;

	final static String INDEX_FILENAME = "quadtreeIndex.data";
	final static String LIST_FILENAME = "quadtreeNodeList.data";

	final static int CACHE_KEY_LENGTH = 9;
	final static int MAX_CACHE_SIZE = 32000; // number of entries

	String dataDirectoryPath;
	BufferedRandomAccessFileCache rafIndexCache = new BufferedRandomAccessFileCache();
	BufferedRandomAccessFileCache rafListCache = new BufferedRandomAccessFileCache();

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

	String getIndexFilePath(double lat, double lng) {
		LatLonDir lld = new LatLonDir(lat, lng);
		return lld.getDir(dataDirectoryPath) + File.separatorChar
				+ INDEX_FILENAME;
	}

	String getListFilePath(double lat, double lng) {
		LatLonDir lld = new LatLonDir(lat, lng);
		return lld.getDir(dataDirectoryPath) + File.separatorChar
				+ LIST_FILENAME;
	}

	public int getCacheSize() {
		return 0; /* cache.size(); */
	}

	public PartitionedQuadtreeNodeIndexFile(String dataDirectoryPath,
			boolean readOnly, boolean startNewFile) {
		this.dataDirectoryPath = dataDirectoryPath;
		this.readOnly = readOnly;

		if( readOnly ) {
			rafIndexCache.setMaxCacheSize(8);
			rafListCache.setMaxCacheSize(8);
		}

		for (int i = 0; i < 40; i++)
			emptySentence[i] = -1;
	}

	String lastIndexFilePath;
	BufferedRandomAccessFile lastIndexRaf;

	BufferedRandomAccessFile getIndexRaf(double lat, double lng) {
		String filePath = getIndexFilePath(lat, lng);

		if (filePath.equals(lastIndexFilePath))
			return lastIndexRaf;

		try {
			BufferedRandomAccessFile raf = rafIndexCache.getRandomAccessFile(
					filePath, readOnly);

			if (raf != null) {

				lastIndexFilePath = filePath;
				lastIndexRaf = raf;

				numberOfSentences = raf.getSize() / (int) SENTENCE_LENGTH;
				if (numberOfSentences == 0)
					insertNewSentence(raf);
			}
			return raf;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	String lastListFilePath;
	BufferedRandomAccessFile lastListRaf;

	BufferedRandomAccessFile getListRaf(double lat, double lng) {
		String filePath = getListFilePath(lat, lng);

		if (filePath.equals(lastListFilePath))
			return lastListRaf;

		try {
			BufferedRandomAccessFile raf = rafListCache.getRandomAccessFile(
					filePath, readOnly);

			if (raf != null) {

				lastListFilePath = filePath;
				lastListRaf = raf;

			}
			return raf;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	@Override
	public void close() {
		rafIndexCache.close();
		rafListCache.close();
	}

	@Override
	public int getID(double lat, double lng) {
		BufferedRandomAccessFile raf = getIndexRaf(lat, lng); // rafCache.getRandomAccessFile(getFilePath(lat,
																// lng),
																// readOnly);
		if (raf == null)
			return -1;

		int latInt = (int) ((lat + LAT_OFFS) * USED_DIGITS_MULT);
		int lngInt = (int) ((lng + LNG_OFFS) * USED_DIGITS_MULT);

		try {
			return getID(latInt, lngInt, raf);
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

	public int getID(int latInt, int lngInt) {
		int lat = latInt / USED_DIGITS_MULT - (int) LAT_OFFS;
		int lng = lngInt / USED_DIGITS_MULT - (int) LNG_OFFS;

		BufferedRandomAccessFile raf = getIndexRaf(lat, lng); // rafCache.getRandomAccessFile(getFilePath(lat,
																// lng),
																// readOnly);
		if (raf == null)
			return -1;

		try {
			return getID(latInt, lngInt, raf);
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}

	}

	int getID(int latInt, int lngInt, BufferedRandomAccessFile raf)
			throws IOException {
		if (numberOfSentences == 0)
			return -1;

		if (latInt < 0 || latInt > MAX_LAT_INT)
			return -1;
		if (lngInt < 0 || lngInt > MAX_LNG_INT)
			return -1;

		readCount++;

		int id = 0;
		int[] keyChain = getKeyChain(latInt, lngInt);
		int i = 0;

		for (; i < NUMBER_OF_USED_DIGITS_MULT_2; i++) {
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
						int idInListFile = getID(x, y);
						if (idInListFile == -1)
							continue;
						List<Integer> nodes = getAllNodeIDsFromQuadtreeList(
								lat, lng, idInListFile);
						for (int nodeID : nodes) {
							NodeIndexNodeDescriptor nind = new NodeIndexNodeDescriptor();
							nind.id = nodeID;
							nind.latInt = x / USED_DIGITS_MULT - (int) LAT_OFFS; // not
																					// elegant
							nind.lngInt = y / USED_DIGITS_MULT - (int) LNG_OFFS;
							list.add(nind);
						}
					}
				}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	List<Integer> getAllNodeIDsFromQuadtreeList(double lat, double lng, int id) {
		List<Integer> l = new LinkedList<Integer>();
		BufferedRandomAccessFile rafList = getListRaf(lat, lng);
		if (rafList == null)
			return l;

		try {
			for (;;) {
				rafList.seek(id * NODELIST_SENTENCE_LENGTH);
				int nodeID;
				nodeID = rafList.readInt();
				int nextID = rafList.readInt();

				l.add(nodeID);
				if (nextID == -1)
					break;
				id = nextID;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return l;
	}

	@Override
	public int setID(double lat, double lng, int id) {
		BufferedRandomAccessFile raf = getIndexRaf(lat, lng); // rafCache.getRandomAccessFile(getFilePath(lat,
																// lng),
																// readOnly);

		int latInt = (int) ((lat + LAT_OFFS) * USED_DIGITS_MULT);
		int lngInt = (int) ((lng + LNG_OFFS) * USED_DIGITS_MULT);

		try {
			return setID(latInt, lngInt, id, raf);

		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

	int setID(int latInt, int lngInt, int nodeID, BufferedRandomAccessFile raf)
			throws IOException {

		if (latInt < 0 || latInt > MAX_LAT_INT)
			return -1;
		if (lngInt < 0 || lngInt > MAX_LNG_INT)
			return -1;

		writeCount++;

		int id = 0;
		int[] keyChain = getKeyChain(latInt, lngInt);
		int i = 0;

		// read as long as leafs exist

		for (; i < NUMBER_OF_USED_DIGITS_MULT_2 - 1; i++) {
			long pos = (long) id * SENTENCE_LENGTH + (long) keyChain[i] * 4L;
			if (!raf.seek(pos))
				break;
			int foundID = raf.readInt();
			if (foundID == -1)
				break;
			id = foundID;
		}

		// add new leafs

		for (; i < NUMBER_OF_USED_DIGITS_MULT_2 - 1; i++) {
			int newID = insertNewSentence(raf);
			if (newID == -1) {
				System.out
						.println("ERROR: cannot create new sentence in QuadtreeNodeIndexFile");
				return -1;
			}
			long pos = (long) id * SENTENCE_LENGTH + (long) keyChain[i] * 4L;
			raf.seek(pos);
			raf.writeInt(newID);
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

	public int insertNewSentence(BufferedRandomAccessFile raf) {
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

	public void makeQuadtreeIndex(Date startTime, PartitionedNodeListFile nlf) {
		File[] files = new File(dataDirectoryPath).listFiles();
		Arrays.sort(files);
		for (File f : files)
			if (f.isDirectory() && f.getName().startsWith("lat_")) {
				for (File g : f.listFiles())
					if (g != null && g.isDirectory()
							&& g.getName().startsWith("lng_")) {
						File[] list = g.listFiles();
						if (list == null) {
							System.out.println("Cannot list files in "
									+ g.getPath());
							continue;
						}
						for (File h : list)
							if (h != null && h.isFile()
									&& h.getName().equals(PartitionedNodeListFile.FILE_NAME)) {

								int dirLatInt = Integer.parseInt(f.getName()
										.replace("lat_", ""))
										- (int) LatLonDir.LAT_OFFS;
								int dirLngInt = Integer.parseInt(g.getName()
										.replace("lng_", ""))
										- (int) LatLonDir.LNG_OFFS;

								String quadtreeIndexFilePath = g.getPath()
										+ File.separatorChar
										+ INDEX_FILENAME;
								String quadtreeListFilePath = g.getPath()
										+ File.separatorChar
										+ LIST_FILENAME;

								new File(quadtreeIndexFilePath).delete();
								new File(quadtreeListFilePath).delete();

								BufferedRandomAccessFile rafIndex = new BufferedRandomAccessFile();
								BufferedRandomAccessFile rafList = new BufferedRandomAccessFile();

								try {
									rafIndex.open(quadtreeIndexFilePath, "rw");
									rafList.open(quadtreeListFilePath, "rw");
								} catch (FileNotFoundException e1) {
									e1.printStackTrace();
									continue;
								}

								numberOfSentences = 0;
								insertNewSentence(rafIndex);

								List<Node> nodes = nlf.getAllNodesInFile(h
										.getPath());
								System.out.println("\n"
										+ Utils.formatTimeStopWatch(new Date()
												.getTime()
												- startTime.getTime())
										+ " building quadtree lat=" + dirLatInt
										+ " lng=" + dirLngInt + " with "
										+ nodes.size() + " nodes");
								int idInListFile = 0;
								for (Node n : nodes) {
									try {
										int latInt = (int) ((n.lat + LAT_OFFS) * USED_DIGITS_MULT);
										int lngInt = (int) ((n.lng + LNG_OFFS) * USED_DIGITS_MULT);

										int foundID = setID(latInt, lngInt,
												idInListFile, rafIndex);

										// TEST REMOVE
										/*
										 * int testID = getID(latInt, lngInt,
										 * rafIndex); if( testID != idInListFile
										 * ) System.out.println("error");
										 */

										idInListFile++;

										rafList.writeInt((int) n.id);
										rafList.writeInt(foundID);

									} catch (IOException e) {
										e.printStackTrace();
									}
								}

								try {
									rafIndex.close();
									rafList.close();
								} catch (IOException e) {
									e.printStackTrace();
								}

							}
					}
				Rebuilder.cleanMem(startTime);
			}
		System.out.println(Utils.formatTimeStopWatch(new Date().getTime()
				- startTime.getTime())
				+ " finished building quadtree");

	}

}
