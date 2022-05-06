package de.esymetric.jerusalem.routing;

import java.util.List;

import de.esymetric.jerusalem.ownDataRepresentation.Node;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedNodeListFile;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedTransitionListFile;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedWayCostFile;

public interface RoutingAlgorithm {
	/**
	 * Do the actual routing.
	 * 
	 * @param start Start node
	 * @param target Target node
	 * @param type Type (vehicle) for routing
	 * @param nlf Provides access to the nodes database
	 * @param wlf Provides access to the transition database
	 * @param wcf Provides access to the way cost database
	 * @param heuristics Heuristics to be used for routing
	 * @param maxExecutionTimeSec Maximum allowed execution time 
	 * @return List of nodes the of the route
	 */
	public List<Node> getRoute(Node start, Node target, RoutingType type,
			PartitionedNodeListFile nlf, PartitionedTransitionListFile wlf, PartitionedWayCostFile wcf,
			RoutingHeuristics heuristics, 
			List<Node> targetNodeMasterNodes, // 2 crossroads nodes connected to target node
			int maxExecutionTimeSec, boolean useOptimizedPath);
}
