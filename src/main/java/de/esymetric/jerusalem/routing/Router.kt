package de.esymetric.jerusalem.routing

import de.esymetric.jerusalem.ownDataRepresentation.Node
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.*
import de.esymetric.jerusalem.ownDataRepresentation.geoData.Position
import de.esymetric.jerusalem.utils.Utils
import java.util.*

class Router(
    dataDirectoryPath: String, private val routingAlgorithm: RoutingAlgorithm,
    private val heuristics: RoutingHeuristics, private val maxExecutionTimeS: Int
) {
    private var nrf: NearestNodeFinder = NearestNodeFinder()
    private var nif: NodeIndexFile
    var nlf: PartitionedNodeListFile
    private var wlf: PartitionedTransitionListFile
    private var wcf: PartitionedWayCostFile

    init {
        nif = PartitionedQuadtreeNodeIndexFile(
            dataDirectoryPath, true
        )
        nlf = PartitionedNodeListFile(dataDirectoryPath, true)
        wlf = PartitionedTransitionListFile(dataDirectoryPath, true)
        wcf = PartitionedWayCostFile(dataDirectoryPath, true)
    }

    fun close() {
        nlf.close()
        wlf.close()
        wcf.close()
    }

    fun findRoute(
        vehicle: String, lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): List<Node>? {
        val startTime = Date()
        val start = Position()
        start.latitude = lat1
        start.longitude = lng1
        val target = Position()
        target.latitude = lat1
        target.longitude = lng1
        val type = RoutingType.valueOf(vehicle)
        val nodeA = nrf.findNearestNode(lat1, lng1, nif, nlf, wlf, wcf, type)
        if (nodeA == null && debugMode) println("ERROR: Start node not found!")
        val nodeB = nrf.findNearestNode(lat2, lng2, nif, nlf, wlf, wcf, type)
        if (nodeB == null && debugMode) println("ERROR: Target node not found!")
        if (nodeA == null || nodeB == null) return null
        if (debugMode) {
            println(
                "START NODE: id=" + nodeA.uID + " lat="
                        + nodeA.lat + " lng=" + nodeA.lng
            )
            println(
                "TARGET NODE: id=" + nodeB.uID + " lat="
                        + nodeB.lat + " lng=" + nodeB.lng
            )
        }

        val masterNodesB = nodeB.findConnectedMasterNodes(nlf, wlf)
        val route = routingAlgorithm.getRoute(
            nodeA, nodeB, type,
            nlf, wlf, wcf, heuristics, masterNodesB, maxExecutionTimeS,
            true // use optimized routing?
        )

        if (debugMode) {
            println(
                "required time "
                        + Utils.formatTimeStopWatch(
                    Date().time
                            - startTime.time
                )
            )
        }
        return route
    }

    companion object {
        @JvmStatic
        var debugMode = false
    }
}