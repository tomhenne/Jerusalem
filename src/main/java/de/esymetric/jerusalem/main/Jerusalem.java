package de.esymetric.jerusalem.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.apache.tools.bzip2.CBZip2InputStream;

import de.esymetric.jerusalem.extraTools.CrossroadsFinder;
import de.esymetric.jerusalem.osmDataRepresentation.OSMDataReader;
import de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.PartitionedOsmNodeID2OwnIDMap;
import de.esymetric.jerusalem.ownDataRepresentation.Node;
import de.esymetric.jerusalem.ownDataRepresentation.geoData.Position;
import de.esymetric.jerusalem.ownDataRepresentation.geoData.importExport.GPX;
import de.esymetric.jerusalem.ownDataRepresentation.geoData.importExport.KML;
import de.esymetric.jerusalem.rebuilding.Rebuilder;
import de.esymetric.jerusalem.rebuilding.optimizer.TransitionsOptimizer;
import de.esymetric.jerusalem.routing.Router;
import de.esymetric.jerusalem.routing.RoutingType;
import de.esymetric.jerusalem.routing.algorithms.TomsAStarStarRouting;
import de.esymetric.jerusalem.routing.heuristics.TomsRoutingHeuristics;
import de.esymetric.jerusalem.utils.Utils;

public class Jerusalem {

	public static void main(String[] args) {

		System.out
				.println("JERUSALEM 0.85 Java Enabled Routing Using Speedy Algorithms for Largely Extended Maps (jerusalem.gps-sport.net) based on OSM (OpenStreetMap.org)");

		if (args.length == 0) {
			printUsage();
			return;
		}

		String command = args[0];
		int maxExecutionTimeS = 120;
		String dataDirectoryPath = System.getProperty("user.dir")
				+ File.separatorChar + "jerusalemData";
		String tempDirectoryPath = dataDirectoryPath;

		new File(dataDirectoryPath).mkdir();

		// rebuild all

		if ("optimize".equals(command)) {

			TransitionsOptimizer to = new TransitionsOptimizer(
					dataDirectoryPath);
			to.optimize(new Date());
			to.close();

			return;
		}

		if ("clean".equals(command)) {
			// remove temp files from osm 2 own id map

			if (args.length < 2) {
				printUsage();
				return;
			}

			tempDirectoryPath = args[1] + File.separatorChar
					+ "jerusalemTempData";
			new File(tempDirectoryPath).mkdirs();

			PartitionedOsmNodeID2OwnIDMap poniom = new PartitionedOsmNodeID2OwnIDMap(
					tempDirectoryPath, true);
			poniom.deleteLatLonTempFiles();
			poniom.close();

			return;
		}

		if ("rebuildIndex".equals(command)) {
			if (args.length < 2) {
				printUsage();
				return;
			}

			tempDirectoryPath = args[1] + File.separatorChar
					+ "jerusalemTempData";

			Rebuilder rebuilder = new Rebuilder(dataDirectoryPath,
					tempDirectoryPath, new TomsRoutingHeuristics(), true,
					false, true);
			rebuilder.makeQuadtreeIndex();
			// do NOT close Rebuilder rebuilder.close();

			return;
		}

		if ("rebuildTransitions".equals(command)) {
			if (args.length < 2) {
				printUsage();
				return;
			}

			tempDirectoryPath = args[1] + File.separatorChar
					+ "jerusalemTempData";

			Rebuilder rebuilder = new Rebuilder(dataDirectoryPath,
					tempDirectoryPath, new TomsRoutingHeuristics(), true,
					false, true);
			rebuilder.buildTransitions();
			// do NOT close Rebuilder rebuilder.close();

			return;
		}

		if ("rebuild".equals(command) || "rebuildWays".equals(command)) {
			boolean rebuildOnlyWays = "rebuildWays".equals(command);

			if (args.length < 3) {
				printUsage();
				return;
			}

			String filePath = args[1];

			if (!"-".equals(filePath)) {
				System.out.println("Rebuilding " + filePath);
			} else {
				System.out.println("Rebuilding from stdin");
				filePath = null;
			}

			tempDirectoryPath = args[2] + File.separatorChar
					+ "jerusalemTempData";
			new File(tempDirectoryPath).mkdirs();

			Date startTime = new Date();
			System.out.println("start date " + startTime);

			try {
				Rebuilder rebuilder = new Rebuilder(dataDirectoryPath,
						tempDirectoryPath, new TomsRoutingHeuristics(),
						rebuildOnlyWays, rebuildOnlyWays, false);
				if (filePath != null) {
					InputStream fis = new FileInputStream(filePath);
					CBZip2InputStream bzis = new CBZip2InputStream(fis);
					OSMDataReader osmdr = new OSMDataReader(bzis, rebuilder,
							rebuildOnlyWays);
					osmdr.read(startTime);
					bzis.close();
					fis.close();
				} else {
					OSMDataReader osmdr = new OSMDataReader(System.in,
							rebuilder, rebuildOnlyWays);
					osmdr.read(startTime);
				}
				rebuilder.finishProcessingAndClose();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("finish date " + new Date());
			System.out.println("required time "
					+ Utils.FormatTimeStopWatch(new Date().getTime()
							- startTime.getTime()));

			return;
		}

		// routing

		if ("route".equals(command) || "routeVerbose".equals(command) || "routeWithTransitions".equals(command)) {

			if (args.length < 6) {
				printUsage();
				return;
			}

			Router router = new Router(dataDirectoryPath,
					new TomsAStarStarRouting(), new TomsRoutingHeuristics(),
					maxExecutionTimeS);
			Router.setDebugMode("routeVerbose".equals(command));
			boolean outputTransitions = "routeWithTransitions".equals(command);
			String routingType = args[1];
			double lat1 = Double.parseDouble(args[2]);
			double lng1 = Double.parseDouble(args[3]);
			double lat2 = Double.parseDouble(args[4]);
			double lng2 = Double.parseDouble(args[5]);
			List<Node> route = router.findRoute(routingType, lat1, lng1, lat2,
					lng2);
			router.close();
			if (route != null) {
				for (Node n : route) {
					StringBuilder sb = new StringBuilder();
					sb.append(n.lat).append(',').append(n.lng);
					if( outputTransitions ) {
						sb.append(',').append(n.getNumberOfTransitionsIfTransitionsAreLoaded());
					}
					System.out.println(sb.toString());
				}
				if (args.length > 6) {
					String filename = args[6];
					GPX gpx = new GPX();
					KML kml = new KML();
					Vector<Position> trackPts = new Vector<Position>();
					for (Node n : route) {
						Position p = new Position();
						p.latitude = n.lat;
						p.longitude = n.lng;
						trackPts.add(p);
					}
					gpx.setTrackPositions(trackPts);
					gpx.Save(dataDirectoryPath + File.separatorChar + filename
							+ "-" + routingType + ".gpx");
					kml.setTrackPositions(trackPts);
					kml.Save(dataDirectoryPath + File.separatorChar + filename
							+ "-" + routingType + ".kml");
				}
			} 
			return;
		}

		// find crossroads

		if ("findCrossroads".equals(command) ) {

			if (args.length < 2) {
				printUsage();
				return;
			}

			String routingType = args[1];
			
			InputStreamReader isr = new InputStreamReader(System.in);
			LineNumberReader lnr = new LineNumberReader(isr);
			List<Position> positions = new ArrayList<Position>();
			for(;;) {
				String line = null;
				try {
					line = lnr.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if( line == null || "eof".equals(line)) break;
				String[] lp = line.split(",");
				if( lp.length < 3 ) continue;
				Position p = new Position();
				p.latitude = Double.parseDouble(lp[0]);
				p.longitude = Double.parseDouble(lp[1]);
				p.altitude = Double.parseDouble(lp[2]);
				positions.add(p);
			}
			
			CrossroadsFinder cf = new CrossroadsFinder(dataDirectoryPath);
			cf.loadNumberOfCrossroads(positions, RoutingType.valueOf(RoutingType.class, routingType));
			
				for (Position p : positions) {
					StringBuilder sb = new StringBuilder();
					sb.append(p.latitude).append(',').append(p.longitude).append(',').append(p.altitude).append(',').append(p.nrOfTransitions);
					System.out.println(sb.toString());
				}
			return;
		}

		// find crossroads test

		if ("findCrossroadsTest".equals(command) ) {

			if (args.length < 2) {
				printUsage();
				return;
			}

			String routingType = args[1];
			
			List<Position> positions = new ArrayList<Position>();
			Position pt = new Position();
			pt.latitude = 48.116915489240476;
			pt.longitude = 11.48764371871948;
			pt.altitude = 600;
			positions.add(pt);
			pt = new Position();
			pt.latitude = 48.15;
			pt.longitude = 11.55;
			pt.altitude = 660;
			positions.add(pt);
			
			
			CrossroadsFinder cf = new CrossroadsFinder(dataDirectoryPath);
			cf.loadNumberOfCrossroads(positions, RoutingType.valueOf(RoutingType.class, routingType));
			
				for (Position p : positions) {
					StringBuilder sb = new StringBuilder();
					sb.append(p.latitude).append(',').append(p.longitude).append(',').append(p.altitude).append(',').append(p.nrOfTransitions);
					System.out.println(sb.toString());
				}
			return;
		}

		// test routing

		if ("routingTest".equals(command)) {

			Date startTime = new Date();
			System.out.println("routing test start date " + startTime);

			Router router = new Router(dataDirectoryPath,
					new TomsAStarStarRouting(), new TomsRoutingHeuristics(),
					maxExecutionTimeS);
			Router.setDebugMode(true);

			testRoute(router, 48.116915489240476, 11.48764371871948, 48.219297,
					11.372824, "hadern-a8", dataDirectoryPath);

			testRoute(router, 48.116915489240476, 11.48764371871948,
					48.29973956844243, 10.97055673599243, "hadern-kissing",
					dataDirectoryPath);

			testRoute(router, 48.125166, 11.451445, 48.12402, 11.515946, "a96",
					dataDirectoryPath);

			testRoute(router, 48.125166, 11.451445, 48.103516, 11.501441,
					"a96_Fuerstenrieder", dataDirectoryPath);

			testRoute(router, 48.09677, 11.323729, 48.393707, 11.841116,
					"autobahn", dataDirectoryPath);

			testRoute(router, 48.107891, 11.461865, 48.099986, 11.511051,
					"durch-waldfriedhof", dataDirectoryPath);

			testRoute(router, 48.107608, 11.461648, 48.108656, 11.477371,
					"grosshadern-fussweg", dataDirectoryPath);

			testRoute(router, 48.275653, 11.786957, 48.106514, 11.449685,
					"muenchen-quer", dataDirectoryPath);

			testRoute(router, 48.073606, 11.38175, 48.065548, 11.327763,
					"gauting-unterbrunn", dataDirectoryPath);

			testRoute(router, 48.073606, 11.38175, 48.152888, 11.346259,
					"gauting-puchheim", dataDirectoryPath);

			testRoute(router, 48.073606, 11.38175, 48.365138, 11.583881,
					"gauting-moosanger", dataDirectoryPath);

			testRoute(router, 47.986073, 11.326733, 48.230162, 11.717434,
					"starnberg-ismaning", dataDirectoryPath);

			router.close();

			System.out.println("finish date " + new Date());
			System.out.println("required time "
					+ Utils.FormatTimeStopWatch(new Date().getTime()
							- startTime.getTime()));
			return;
		}
	}

	private static void printUsage() {
		System.out
				.println("java -jar Jerusalem.jar route foot|bike|racingBike|mountainBike|car|carShortest <latitude1> <longitude1> <latitude2> <longitude2> [<output-file-base-name>]");
		System.out
				.println("java -jar Jerusalem.jar rebuild <source-filepath>|- <temp-filepath>");
		System.out
				.println("java -jar Jerusalem.jar rebuildIndex <temp-filepath>");
		System.out
				.println("java -jar Jerusalem.jar rebuildTransitions <temp-filepath>");
		System.out.println("java -jar Jerusalem.jar optimize");
		System.out.println("java -jar Jerusalem.jar clean <temp-filepath>");
		System.out.println("java -jar Jerusalem.jar routingTest");
		System.out.println("java -jar Jerusalem.jar findCrossroads foot|bike|racingBike|mountainBike|car|carShortest");
		System.out.println("java -jar Jerusalem.jar findCrossroadsTest foot|bike|racingBike|mountainBike|car|carShortest");
	}

	static void testRoute(Router router, double lat1, double lng1, double lat2,
			double lng2, String name, String dataDirectoryPath) {
		for (RoutingType rt : RoutingType.values())
			testRoute(router, rt.name(), lat1, lng1, lat2, lng2, name,
					dataDirectoryPath);
	}

	static void testRoute(Router router, String routingType, double lat1,
			double lng1, double lat2, double lng2, String name,
			String dataDirectoryPath) {
		System.out.println("---------------------------------------------");
		System.out
				.println("Computing Route " + name + " (" + routingType + ")");
		System.out.println("---------------------------------------------");

		List<Node> route = router
				.findRoute(routingType, lat1, lng1, lat2, lng2);
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

	}

}
