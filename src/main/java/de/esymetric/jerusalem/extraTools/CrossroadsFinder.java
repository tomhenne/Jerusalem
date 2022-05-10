package de.esymetric.jerusalem.extraTools;

import de.esymetric.jerusalem.ownDataRepresentation.Node;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.*;
import de.esymetric.jerusalem.ownDataRepresentation.geoData.GPSMath;
import de.esymetric.jerusalem.ownDataRepresentation.geoData.Position;
import de.esymetric.jerusalem.routing.NearestNodeFinder;
import de.esymetric.jerusalem.routing.RoutingType;

import java.util.List;

public class CrossroadsFinder {

	final double MAX_DISTANCE_TO_NODE = 10.0;
	
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
			double d = GPSMath.calculateDistance(p.latitude, p.longitude, n.lat,
					n.lng);
			if( d <= MAX_DISTANCE_TO_NODE )
				p.nrOfTransitions = (byte)n.listTransitions(false, nlf, wlf).size();
		}
	}
}
