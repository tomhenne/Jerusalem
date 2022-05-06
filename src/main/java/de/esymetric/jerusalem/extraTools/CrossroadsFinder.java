package de.esymetric.jerusalem.extraTools;

import java.util.List;

import de.esymetric.jerusalem.ownDataRepresentation.Node;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.NodeIndexFile;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedNodeListFile;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedQuadtreeNodeIndexFile;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedTransitionListFile;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedWayCostFile;
import de.esymetric.jerusalem.ownDataRepresentation.geoData.GPSMath;
import de.esymetric.jerusalem.ownDataRepresentation.geoData.Position;
import de.esymetric.jerusalem.routing.NearestNodeFinder;
import de.esymetric.jerusalem.routing.RoutingType;

public class CrossroadsFinder {

	final double MAX_DISTANCE_TO_NODE = 10.0;
	
	protected static boolean debugMode = false;

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

	public CrossroadsFinder(String dataDirectoryPath) {
		nrf = new NearestNodeFinder();
		nif = new PartitionedQuadtreeNodeIndexFile(dataDirectoryPath, true,
				false);
		nlf = new PartitionedNodeListFile(dataDirectoryPath, true);
		wlf = new PartitionedTransitionListFile(dataDirectoryPath, true);
		wcf = new PartitionedWayCostFile(dataDirectoryPath, true);
	}

	public void close() {
		nlf.close();
		wlf.close();
		wcf.close();
	}

	public void loadNumberOfCrossroads(List<Position> positions, RoutingType routingType) {
		
		for(Position p : positions) {
			Node n = nrf.findNearestNode(p.latitude, p.longitude, nif, nlf, wlf, wcf, routingType);
			if( n == null ) continue;
			double d = GPSMath.CalculateDistance(p.latitude, p.longitude, n.lat,
					n.lng);
			if( d <= MAX_DISTANCE_TO_NODE )
				p.nrOfTransitions = (byte)n.listTransitions(false, nlf, wlf).size();
		}
	}
}
