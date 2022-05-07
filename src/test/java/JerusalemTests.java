import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import junit.framework.TestCase;

import org.apache.tools.bzip2.CBZip2InputStream;

import de.esymetric.jerusalem.osmDataRepresentation.OSMDataReader;
import de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.OsmNodeID2CellIDMapMemory;
import de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.PartitionedOsmNodeID2OwnIDMap;
import de.esymetric.jerusalem.osmDataRepresentation.unused.LongOsmNodeID2OwnIDMapFileBased;
import de.esymetric.jerusalem.ownDataRepresentation.Node;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedNodeListFile;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedQuadtreeNodeIndexFile;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.NodeIndexFile.NodeIndexNodeDescriptor;
import de.esymetric.jerusalem.ownDataRepresentation.geoData.Position;
import de.esymetric.jerusalem.ownDataRepresentation.geoData.importExport.KML;
import de.esymetric.jerusalem.rebuilding.Rebuilder;
import de.esymetric.jerusalem.rebuilding.optimizer.TransitionsOptimizer;
import de.esymetric.jerusalem.routing.Router;
import de.esymetric.jerusalem.routing.RoutingType;
import de.esymetric.jerusalem.routing.algorithms.TomsAStarStarRouting;
import de.esymetric.jerusalem.routing.heuristics.TomsRoutingHeuristics;
import de.esymetric.jerusalem.utils.BufferedRandomAccessFile;
import de.esymetric.jerusalem.utils.BufferedRandomAccessFileCache;
import de.esymetric.jerusalem.utils.FileBasedHashMap;
import de.esymetric.jerusalem.utils.FileBasedHashMapForLongKeys;
import de.esymetric.jerusalem.utils.MemoryEfficientLongToIntMap;
import de.esymetric.jerusalem.utils.RandomAccessFileCache;

public class JerusalemTests extends TestCase {

	public JerusalemTests() {
		super("Jerusalem Test");
	}

	public void testLanLonDir() throws Exception {
		for (double lat = -89; lat < 89; lat += 0.5)
			for (double lng = -89; lng < 89; lng += 0.3) {
				boolean ok = new LatLonDir(lat, lng).equals(new LatLonDir(
						new LatLonDir(lat, lng).getShortKey()));
				assertTrue(ok);
				// System.out.println("" + lat + " " + lng + " " + ok);
			}

		checkOffsetBits(48, 11, 48, 11);

		checkOffsetBits(48, 11, 49, 11);
		checkOffsetBits(48, 11, 49, 12);
		checkOffsetBits(48, 11, 48, 12);
		checkOffsetBits(48, 11, 48, 10);

		checkOffsetBits(48, 11, 47, 10);
		checkOffsetBits(48, 11, 47, 12);
		checkOffsetBits(48, 11, 47, 11);
		checkOffsetBits(48, 11, 49, 10);

	}

	void checkOffsetBits(int lat1, int lng1, int lat2, int lng2) {
		LatLonDir lld1 = new LatLonDir(lat1, lng1);
		int offsetBits = lld1.getOffsetBits(lat2, lng2);
		LatLonDir lld3 = new LatLonDir(lat1, lng1, offsetBits);
		assertTrue(new LatLonDir(lat2, lng2).equals(lld3));
	}

	public void testOsmNodeID2CellIDMap() throws Exception {

		String filePath = "testData/osmNodeID2CellIDMap.data";
		
		OsmNodeID2CellIDMapMemory map = new OsmNodeID2CellIDMapMemory();
		LatLonDir lld = new LatLonDir(48.12345, 11.92737);
		map.put(1234, lld);
		assertTrue(lld.equals(map.get(1234)));
		
		map.save(filePath);
		
		map = new OsmNodeID2CellIDMapMemory();
		map.load(filePath);
		
		assertTrue(lld.equals(map.get(1234)));
	}

	public void testFileBasedHashMap() throws Exception {
		new File("testData/fileBasedHashMapTest.data").delete();
		FileBasedHashMap fbhm = new FileBasedHashMap();
		fbhm.open("testData/fileBasedHashMapTest.data", false);
		for (int id = 1000000; id > 0; id -= 111) {
			int value = (int) (Math.random() * 788823548);
			fbhm.put(id, value);
			assertTrue(fbhm.get(id) == value);
		}
		for (int id = 1000001; id < 1286746; id += 263) {
			int value = (int) (Math.random() * 788823548);
			fbhm.put(id, value);
			if (fbhm.get(id) != value) {
				fbhm.put(id, value);
				assertTrue(fbhm.get(id) == value);
			}
			assertTrue(fbhm.get(id) == value);
		}
		for (int id = 2000001; id < 3000000; id += 555) {
			int key = id;
			int value = (int) (Math.random() * 788823548);
			fbhm.put(key, value);
			assertTrue(fbhm.get(key) == value);
		}
		fbhm.close();
		new File("testData/fileBasedHashMapTest.data").delete();
	}

	public void testFileBasedHashMapForLongKeys() throws Exception {
		new File("testData/fileBasedHashMapTestForLongKeys.data").delete();
		FileBasedHashMapForLongKeys fbhm = new FileBasedHashMapForLongKeys();
		fbhm.open("testData/fileBasedHashMapTestForLongKeys.data", false);
		for (int id = 1000000; id > 0; id -= 111) {
			int value = (int) (Math.random() * 788823548);
			fbhm.put(id, value);
			assertTrue(fbhm.get(id) == value);
		}
		for (int id = 1000001; id < 1286746; id += 263) {
			int value = (int) (Math.random() * 788823548);
			fbhm.put(id, value);
			if (fbhm.get(id) != value) {
				fbhm.put(id, value);
				assertTrue(fbhm.get(id) == value);
			}
			assertTrue(fbhm.get(id) == value);
		}
		for (long id = 3000000000L; id < 3300000000L; id += 23555L) {
			long key = id;
			int value = (int) (Math.random() * 788823548);
			fbhm.put(key, value);
			assertTrue(fbhm.get(key) == value);
		}
		fbhm.close();
		new File("testData/fileBasedHashMapTestForLongKeys.data").delete();
	}

	public void testPartitionedOsmNodeID2OwnIDMap() {
		LatLonDir.deleteAllLatLonDataFiles("testData");
		PartitionedOsmNodeID2OwnIDMap map = new PartitionedOsmNodeID2OwnIDMap(
				"testData", false);
		for (int i = 0; i < 25; i++) {
			double lat = Math.random() * 180 - 90;
			double lng = Math.random() * 360 - 180;
			int key = (int) (Math.random() * 788823);
			int value = (int) (Math.random() * 788823548);

			map.put(lat, lng, key, value);
			assertTrue(map.get(key) == value);
		}
		LatLonDir.deleteAllLatLonDataFiles("testData");
	}

	public void testLongOsmNodeID2OwnIDMap() {
		LatLonDir.deleteAllLatLonDataFiles("testData");
		LongOsmNodeID2OwnIDMapFileBased map = new LongOsmNodeID2OwnIDMapFileBased(
				"testData");
		for (int i = 0; i < 6; i++) {
			long key = (long) (Math.random() * 2100000000L);
			int value = (int) (Math.random() * 788823548);

			map.put(key, value);
			assertTrue(map.get(key) == value);
		}
		//LatLonDir.deleteAllLatLonDataFiles("testData");
	}

	public void testMemoryEfficientLongToIntMap() {
		MemoryEfficientLongToIntMap map = new MemoryEfficientLongToIntMap();
		
		map.put( 23477289977L, 1234567);
		
		for (int i = 0; i < 250; i++) {
			long key = (long) (Math.random() * 2100000000L);
			int value = (int) (Math.random() * 788823548);

			map.put(key, value);
			assertTrue(map.get(key) == value);
		}
		
		assertTrue(map.get(23477289977L) == 1234567);
		
		
		// test the keys() method
		
		map.clear();
		
		map.put( 23477289977L, 1234567);

		assertEquals(23477289977L, map.keys()[0]);
		
		// test the keysIterator() method
		
		map.put( 123456789123L, 12345);
		
		Set<Long> s = new HashSet<Long>();
		
		Iterator<Long> it = map.keysIterator();
		while( it.hasNext() )
			s.add(it.next());
		
		assertEquals(2, s.size());
		
		assertTrue(s.contains(23477289977L));
		assertTrue(s.contains(123456789123L));
		
		for (int i = 0; i < 250; i++) {
			long key = (long) (i * 21000000L);
			int value = (int) (Math.random() * 788823548);

			map.put(key, value);
		}
		
		s.clear();
		
		it = map.keysIterator();
		while( it.hasNext() )
			s.add(it.next());
		
		assertEquals(252, s.size());
		
	}

	public void testBufferedRandomAccessFileCache() throws Exception {
		BufferedRandomAccessFileCache rafCache = new BufferedRandomAccessFileCache();

		for (int i = 0; i <= 30; i++) {
			BufferedRandomAccessFile raf = rafCache.getRandomAccessFile(
					"testData/rafCache" + i + ".data", false);
			for (int j = 0; j < 200000; j++) {
				int p = (int)(Math.random() * 20000);
				raf.seek(p);
				raf.writeInt(j);
			}
			raf.seek(999);
			raf.writeInt(333);
		}

		for (int i = 10; i > 0; i--) {
			BufferedRandomAccessFile raf = rafCache.getRandomAccessFile(
					"testData/rafCache" + i + ".data", false);
			raf.seek(999);
			assertEquals(333, raf.readInt());
		}

		rafCache.close();
	}

	public void testRandomAccessFileCache() throws Exception {
		RandomAccessFileCache rafCache = new RandomAccessFileCache();

		for (int i = 0; i <= 30; i++) {
			RandomAccessFile raf = rafCache.getRandomAccessFile(
					"testData/rafCache" + i + ".data", false);
			for (int j = 0; j < 200000; j++) {
				int p = (int)(Math.random() * 20000);
				raf.seek(p);
				raf.writeInt(j);
			}
			raf.seek(999);
			raf.writeInt(333);
		}

		for (int i = 10; i > 0; i--) {
			RandomAccessFile raf = rafCache.getRandomAccessFile(
					"testData/rafCache" + i + ".data", false);
			raf.seek(999);
			assertEquals(333, raf.readInt());
		}

		rafCache.close();
	}

	
	public void testRebuildAndRouting() throws Exception {
		String dataDirectoryPath = "testData";
		new File(dataDirectoryPath).mkdirs();
		//String tempDirectoryPath = "/Users/tomhenne/tempData";
		String tempDirectoryPath = "tempData";
		new File(tempDirectoryPath).mkdirs();

		Rebuilder rebuilder = new Rebuilder(dataDirectoryPath, tempDirectoryPath, 
				new TomsRoutingHeuristics(), false, false, false);
		FileInputStream fis = new FileInputStream("osmData/munich.osm.bz2");
		CBZip2InputStream bzis = new CBZip2InputStream(fis);
		OSMDataReader reader = new OSMDataReader(bzis, rebuilder, false);
		reader.read(new Date());
		bzis.close();
		fis.close();
		rebuilder.finishProcessingAndClose();
		System.out.println("BRAF-INFO: " + 
		BufferedRandomAccessFile.getShortInfoAndResetCounters());
		
		
		// create quadtree index
		
		rebuilder = new Rebuilder(dataDirectoryPath, tempDirectoryPath, 
				new TomsRoutingHeuristics(), true, false, true);
		rebuilder.makeQuadtreeIndex();
		rebuilder.finishProcessingAndClose();
		
		// test

		Router router = new Router(dataDirectoryPath,
				new TomsAStarStarRouting(), new TomsRoutingHeuristics(), 120);
		Router.setDebugMode(true);
		makeRoute(router, 48.107891, 11.461865, 48.099986, 11.511051,
				"durch-waldfriedhof", dataDirectoryPath);

		makeRoute(router, 48.107608, 11.461648, 48.108656, 11.477371,
				"grosshadern-fussweg", dataDirectoryPath);
		
		makeRoute(router, 48.116892, 11.487076,    48.117909, 11.472429, 
				"eichenstrasse", dataDirectoryPath);
		

		/* nicht in hadern2 enthalten: 
		makeRoute(router, 48.125166, 11.451445, 48.103516, 11.501441,
				"a96_Fuerstenrieder", dataDirectoryPath);

		makeRoute(router, 48.125166, 11.451445, 48.12402, 11.515946, "a96",
				dataDirectoryPath);
		 */
	}

	
	public void testPartionedNodeListFile() throws Exception {
		String dataDirectoryPath = "testData";
		PartitionedNodeListFile nlf = new PartitionedNodeListFile(dataDirectoryPath, false);
		
		int nodeID = nlf.insertNewNodeStreamAppend(48.11, 11.48);
		
		int nodeID2 = nlf.insertNewNodeStreamAppend(48.111, 11.481);
		
		int nodeID3 = nlf.insertNewNodeStreamAppend(-48.111, -11.481);
		

		nlf.close();
		
		// ---
		
		nlf = new PartitionedNodeListFile(dataDirectoryPath, false);
		
		nlf.setTransitionID(48.111, 11.481, nodeID2, 2222);
		nlf.setTransitionID(48.11, 11.48, nodeID, 1111);
		nlf.setTransitionID(-48.111, -11.481, nodeID3, 3333);
		
		nlf.close();
		
		// ---
		
		nlf = new PartitionedNodeListFile(dataDirectoryPath, true);
		Node n1 = nlf.getNode(new LatLonDir(48.11, 11.48), nodeID);
		assertEquals(1111, n1.transitionID);

		Node n2 = nlf.getNode(new LatLonDir(48.111, 11.481), nodeID2);
		assertEquals(2222, n2.transitionID);
		
		Node n3 = nlf.getNode(new LatLonDir(-48.111, -11.481), nodeID3);
		assertEquals(3333, n3.transitionID);

		nlf.close();
	}
	
	public void testRouting() throws Exception {
		String dataDirectoryPath = "testData";

		Router router = new Router(dataDirectoryPath,
				new TomsAStarStarRouting(), new TomsRoutingHeuristics(), 120);
		Router.setDebugMode(true);
		makeRoute(router, 48.107891, 11.461865, 48.099986, 11.511051,
				"durch-waldfriedhof", dataDirectoryPath);

		makeRoute(router, 48.107608, 11.461648, 48.108656, 11.477371,
				"grosshadern-fussweg", dataDirectoryPath);

		makeRoute(router, 48.116892, 11.487076,    48.117909, 11.472429, 
				"eichenstrasse", dataDirectoryPath);

	}

	
	public void testRebuildOnlyWays() throws Exception {
		String dataDirectoryPath = "testData";
		String tempDirectoryPath = "tempData";

		// rebuild full

		Rebuilder rebuilder = new Rebuilder(dataDirectoryPath,tempDirectoryPath, 
				new TomsRoutingHeuristics(), false, false, false);
		FileInputStream fis = new FileInputStream("osmData/hadern.osm.bz2");
		CBZip2InputStream bzis = new CBZip2InputStream(fis);
		OSMDataReader reader = new OSMDataReader(bzis, rebuilder, false);
		reader.read(new Date());
		bzis.close();
		fis.close();
		rebuilder.finishProcessingAndClose();

		// create quadtree index
		
		rebuilder = new Rebuilder(dataDirectoryPath, tempDirectoryPath, 
				new TomsRoutingHeuristics(), true, false, true);
		rebuilder.makeQuadtreeIndex();
		rebuilder.finishProcessingAndClose();
		
		// rebuild only ways

		rebuilder = new Rebuilder(dataDirectoryPath, tempDirectoryPath,
				new TomsRoutingHeuristics(), true, true, false);
		fis = new FileInputStream("osmData/hadern.osm.bz2");
		bzis = new CBZip2InputStream(fis);
		reader = new OSMDataReader(bzis, rebuilder, true);
		reader.read(new Date());
		bzis.close();
		fis.close();
		rebuilder.finishProcessingAndClose();

		
		// test
		
		Router router = new Router(dataDirectoryPath,
				new TomsAStarStarRouting(), new TomsRoutingHeuristics(), 120);
		Router.setDebugMode(true);
		makeRoute(router, 48.107891, 11.461865, 48.099986, 11.511051,
				"durch-waldfriedhof", dataDirectoryPath);

		makeRoute(router, 48.107608, 11.461648, 48.108656, 11.477371,
				"grosshadern-fussweg", dataDirectoryPath);

		/*
		makeRoute(router, 48.125166, 11.451445, 48.103516, 11.501441,
				"a96_Fuerstenrieder", dataDirectoryPath);

		makeRoute(router, 48.125166, 11.451445, 48.12402, 11.515946, "a96",
				dataDirectoryPath);*/

	}

	
	public void testTransitionsOptimizer() throws Exception {
		String dataDirectoryPath = "testData";
		new File(dataDirectoryPath).mkdirs();
		
		TransitionsOptimizer to = new TransitionsOptimizer(dataDirectoryPath);
		to.optimize(new Date());
		to.close();
		

		
		// test
		
		Router router = new Router(dataDirectoryPath,
				new TomsAStarStarRouting(), new TomsRoutingHeuristics(), 120);
		Router.setDebugMode(true);
		makeRoute(router, 48.107891, 11.461865, 48.099986, 11.511051,
				"durch-waldfriedhof", dataDirectoryPath);

		makeRoute(router, 48.107608, 11.461648, 48.108656, 11.477371,
				"grosshadern-fussweg", dataDirectoryPath);
		
		makeRoute(router, 48.116892, 11.487076,    48.117909, 11.472429, 
				"eichenstrasse-opt", dataDirectoryPath);

	}

	
	
	void makeRoute(Router router, double lat1, double lng1, double lat2,
			double lng2, String name, String dataDirectoryPath) {
		for (RoutingType rt : RoutingType.values())
			makeRoute(router, rt.name(), lat1, lng1, lat2, lng2, name,
					dataDirectoryPath);
	}

	void makeRoute(Router router, String routingType, double lat1, double lng1,
			double lat2, double lng2, String name, String dataDirectoryPath) {
		System.out.println("---------------------------------------------");
		System.out
				.println("Computing Route " + name + " (" + routingType + ")");
		System.out.println("---------------------------------------------");

		List<Node> route = router
				.findRoute(routingType, lat1, lng1, lat2, lng2);
		assertNotNull(route);
		if (route == null) {
			System.out.println("ERROR: no route found for " + name + " ("
					+ routingType + ")");
			return;
		}

		System.out.println();

		KML kml = new KML();
		Vector<Position> trackPts = new Vector<Position>();
		for (Node n : route) {
			Position p = new Position();
			p.latitude = n.lat;
			p.longitude = n.lng;
			trackPts.add(p);
		}
		kml.setTrackPositions(trackPts);
		kml.Save(dataDirectoryPath + File.separatorChar + name + "-"
				+ routingType + ".kml");

		assertTrue(trackPts.size() > 10);
	}

	static String D02(int d) {
		return d < 10 ? "0" + String.valueOf(d) : String.valueOf(d);
	}

	public static String FormatTimeStopWatch(long ticks) {
		int sec = (int) (ticks / 1000L);

		int days = sec / 86400;
		sec -= days * 86400;
		int hours = sec / 3600;
		sec -= hours * 3600;
		int min = sec / 60;
		sec -= min * 60;

		return "" + (days > 0 ? (days + ".") : "") + hours + ":"
				+ D02(Math.abs(min)) + ":" + D02(Math.abs(sec));
	}

	public void testPartionedQuadtreeNodeIndex() throws Exception {
		PartitionedQuadtreeNodeIndexFile pf = new PartitionedQuadtreeNodeIndexFile("testData", false, true);
		
		pf.setID(48.11,  11.48, 77);
		assertEquals(77, pf.getID(48.11,  11.48));
		
		
		pf.setID(48.11,  11.48, 55);
		pf.setID(33.11,  22.48, 44);
		
		assertEquals(55, pf.getID(48.11,  11.48));
		assertEquals(44, pf.getID(33.11,  22.48));
		
		
		/* just testing ... this is wrong
		pf.setID(48.11001,  11.48, 55);
		pf.setID(48.11002,  11.48, 56);
		
		List<NodeIndexNodeDescriptor> l = pf.getIDPlusSourroundingIDs(48.11001, 11.48, 0);
		System.out.println(l);
		*/
		
		for( double lat = 50.0; lat < 50.4; lat += 0.00001)
			for( double lng = 11.0; lng < 12; lng += 0.009)
				pf.setID(lat,  lng, (int)lat * (int)(lng * 100.0));
		
		for( double lat = 50.0; lat < 50.4; lat += 0.00001)
			for( double lng = 11.0; lng < 12; lng += 0.009) {
				//System.out.println(" " + lat + " " + lng);
				assertEquals((int)lat * (int)(lng * 100.0), pf.getID(lat,  lng ));
			}
		
		for( double lat = 50.0; lat < 60; lat += 0.3)
			for( double lng = -120.0; lng < 60; lng += 0.8)
				pf.setID(lat,  lng, (int)(lat * lng));
		
		for( double lat = 50.0; lat < 60; lat += 0.3)
			for( double lng = -120.0; lng < 60; lng += 0.8)
				assertEquals((int)(lat * lng), pf.getID(lat,  lng ));
		
		for( double lat = 50.0; lat < 60; lat += 0.3)
			for( double lng = -120.0; lng < 60; lng += 0.8)
				pf.setID(lat,  lng, (int)(lat * lng));
		
		for( double lat = 50.0; lat < 60; lat += 0.3)
			for( double lng = -120.0; lng < 60; lng += 0.8)
				assertEquals((int)(lat * lng), pf.getID(lat,  lng ));
	}
	
	public void testBufferedRandomAccessFile() throws Exception {
		new File("testData/braf.data").delete();
		
		BufferedRandomAccessFile braf = new BufferedRandomAccessFile();
		braf.open("testData/braf.data", "rw");
		for (int i = 0; i < 10000; i++)
			braf.writeInt(-1);
		braf.close();

		braf.open("testData/braf.data", "rw");
		braf.seek(20000);
		for (int i = 0; i < 10000; i++)
			braf.writeInt(-1);
		braf.close();

		assertEquals(60000, new File("testData/braf.data").length());

		braf.open("testData/braf.data", "rw");
		for (int i = 11; i < 2000; i += 10) {
			braf.seek(i);
			braf.writeInt(i);
		}
		for (int i = 11; i < 2000; i += 10) {
			braf.seek(i);
			assertTrue(braf.readInt() == i);
		}
		for (int i = 55; i < 2009; i += 9) {
			braf.seek(i);
			braf.writeInt(i);
		}
		braf.close();

		braf.open("testData/braf.data", "rw");
		for (int i = 55; i < 2009; i += 9) {
			braf.seek(i);
			assertTrue(braf.readInt() == i);
		}
		braf.close();
	}

}
