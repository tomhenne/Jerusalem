package de.esymetric.jerusalem.routing;

import java.util.Date;
import java.util.List;

import de.esymetric.jerusalem.ownDataRepresentation.Node;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.NodeIndexFile;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedNodeListFile;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedQuadtreeNodeIndexFile;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedTransitionListFile;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedWayCostFile;
import de.esymetric.jerusalem.ownDataRepresentation.geoData.Position;
import de.esymetric.jerusalem.rebuilding.optimizer.TransitionsExpander;
import de.esymetric.jerusalem.utils.Utils;

public class Router {

	protected static boolean debugMode = false;
	protected int maxExecutionTimeS = 60;

	public static void setDebugMode(boolean debugMode_) {
		debugMode = debugMode_;
	}

	public static boolean getDebugMode() {
		return debugMode;
	}

	NearestNodeFinder nrf;
	NodeIndexFile nif;
	PartitionedNodeListFile nlf;
	PartitionedTransitionListFile wlf;
	PartitionedWayCostFile wcf;
	RoutingHeuristics heuristics;
	RoutingAlgorithm routingAlgorithm;

	public Router(String dataDirectoryPath, RoutingAlgorithm routingAlgorithm,
			RoutingHeuristics heuristics, int maxExecutionTimeS) {
		nrf = new NearestNodeFinder();
		nif = new PartitionedQuadtreeNodeIndexFile(dataDirectoryPath, true,
				false);
		nlf = new PartitionedNodeListFile(dataDirectoryPath, true);
		wlf = new PartitionedTransitionListFile(dataDirectoryPath, true);
		wcf = new PartitionedWayCostFile(dataDirectoryPath, true);
		this.heuristics = heuristics;
		this.routingAlgorithm = routingAlgorithm;
		this.maxExecutionTimeS = maxExecutionTimeS;
	}

	public void close() {
		nlf.close();
		wlf.close();
		wcf.close();
	}

	public List<Node> findRoute(String vehicle, double lat1, double lng1,
			double lat2, double lng2) {

		Date startTime = new Date();
		Position start = new Position();
		start.latitude = lat1;
		start.longitude = lng1;
		Position target = new Position();
		target.latitude = lat1;
		target.longitude = lng1;
		RoutingType type = RoutingType.valueOf(RoutingType.class, vehicle);

		Node nodeA = nrf.findNearestNode(lat1, lng1, nif, nlf, wlf, wcf, type);
		if (nodeA == null && debugMode)
			System.out.println("ERROR: Start node not found!");
		Node nodeB = nrf.findNearestNode(lat2, lng2, nif, nlf, wlf, wcf, type);
		if (nodeB == null && debugMode)
			System.out.println("ERROR: Target node not found!");

		if (nodeA == null || nodeB == null)
			return null;

		if (debugMode) {
			System.out.println("START NODE: id=" + nodeA.getUID() + " lat="
					+ nodeA.lat + " lng=" + nodeA.lng);
			System.out.println("TARGET NODE: id=" + nodeB.getUID() + " lat="
					+ nodeB.lat + " lng=" + nodeB.lng);
		}

		// DEBUG

		// List<Node> masterNodesA = nodeA.findConnectedMasterNodes(nlf, wlf);
		// nodeA = masterNodesA.get(0);
		List<Node> masterNodesB = nodeB.findConnectedMasterNodes(nlf, wlf);
		//nodeB = masterNodesB.get(0);

		List<Node> route = routingAlgorithm.getRoute(nodeA, nodeB, type, 
				nlf, wlf, wcf, heuristics, masterNodesB, maxExecutionTimeS,
				true  // use optimized routing?
				);


		if (route != null) {

			// expand transitions for optimized version

			TransitionsExpander te = new TransitionsExpander();
			int numberOfNodesBefore = route.size();
			route = te.expandNodes(route, nlf, wlf);
			if( debugMode )
				System.out.println("node expansion (optimized path): " + numberOfNodesBefore + " >>> " + route.size());

			// add start and end point

			Node originalStartNode = new Node();
			originalStartNode.lat = lat1;
			originalStartNode.lng = lng1;
			route.add(0, originalStartNode);
			Node originalTargetNode = new Node();
			originalTargetNode.lat = lat2;
			originalTargetNode.lng = lng2;
			route.add(originalTargetNode);
		}

		if (debugMode) {
			System.out.println("required time "
					+ Utils.FormatTimeStopWatch(new Date().getTime()
							- startTime.getTime()));
			// System.out.println("file access info: " +
			// BufferedRandomAccessFile.getShortInfoAndResetCounters());
		}

		return route;
	}

}
