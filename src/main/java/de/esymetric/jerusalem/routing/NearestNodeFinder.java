package de.esymetric.jerusalem.routing;

import java.util.List;

import de.esymetric.jerusalem.ownDataRepresentation.Node;
import de.esymetric.jerusalem.ownDataRepresentation.Transition;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.NodeIndexFile;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedNodeListFile;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedTransitionListFile;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedWayCostFile;
import de.esymetric.jerusalem.ownDataRepresentation.geoData.GPSMath;

public class NearestNodeFinder {

	final static int MAX_SEARCH_RADIUS = 5;  // was 3 !!!
	
	public Node findNearestNode(double lat, double lng, NodeIndexFile nif,
			PartitionedNodeListFile nlf, PartitionedTransitionListFile wlf, PartitionedWayCostFile wcf, RoutingType type) {
		for( int radius = 0; radius < MAX_SEARCH_RADIUS; radius++) {
			Node n = findNearestNode(lat, lng, nif, nlf, wlf, wcf, type, radius);
			if( n != null ) 
				return n;
		}
		return null;
	}

	public Node findNearestNode(double lat, double lng, NodeIndexFile nif,
			PartitionedNodeListFile nlf, PartitionedTransitionListFile wlf, PartitionedWayCostFile wcf, RoutingType type, int radius) {

		List<NodeIndexFile.NodeIndexNodeDescriptor> list = nif.getIDPlusSourroundingIDs(lat, lng, radius);
		Node nearestNode = null;
		double nearestNodeDistance = Double.MAX_VALUE;
		for (NodeIndexFile.NodeIndexNodeDescriptor nind : list) {
			 {
				Node n = nlf.getNode(new LatLonDir(nind.latInt, nind.lngInt), nind.id);  // this is new and hopefully finds all nodes
				if (n.transitionID != -1) { // must have transitions :-)

					// must have transitions for this routingType!!

					List<Transition> transitions = n.listTransitions(false, nlf, wlf, wcf);
					boolean hasSuitableTransitions = false;
					for (Transition t : transitions) {
						if (t.getCost(type) != RoutingHeuristics.BLOCKED_WAY_COST) {
							hasSuitableTransitions = true;
							break;
						}
					}

					if (hasSuitableTransitions) {
						double d = GPSMath.CalculateDistance(lat, lng, n.lat,
								n.lng);
						if (d < nearestNodeDistance) {
							nearestNodeDistance = d;
							nearestNode = n;
						}
					}
				}
				
				//BUG!!!! cannot be used use other list
				/*
				if (n.nextNodeID != -1)
					nind.id = n.nextNodeID;
				else
					break;
					*/
			}
		}
		return nearestNode;
	}
}
