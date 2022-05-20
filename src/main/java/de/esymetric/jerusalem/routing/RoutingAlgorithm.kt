package de.esymetric.jerusalem.routing

import de.esymetric.jerusalem.ownDataRepresentation.Node
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedNodeListFile
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedTransitionListFile
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedWayCostFile

interface RoutingAlgorithm {
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
    fun getRoute(
        start: Node, target: Node,
        type: RoutingType,
        nlf: PartitionedNodeListFile,
        wlf: PartitionedTransitionListFile,
        wcf: PartitionedWayCostFile,
        heuristics: RoutingHeuristics,
        targetNodeMasterNodes: List<Node>,  // 2 crossroads nodes connected to target node
        maxExecutionTimeSec: Int,
        useOptimizedPath: Boolean
    ): List<Node>?
}