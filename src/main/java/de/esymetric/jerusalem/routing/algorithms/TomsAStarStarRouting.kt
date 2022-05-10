package de.esymetric.jerusalem.routing.algorithms

import de.esymetric.jerusalem.ownDataRepresentation.Node
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedNodeListFile
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedTransitionListFile
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedWayCostFile
import de.esymetric.jerusalem.ownDataRepresentation.geoData.GPSMath.calculateDistance
import de.esymetric.jerusalem.ownDataRepresentation.geoData.Position
import de.esymetric.jerusalem.ownDataRepresentation.geoData.importExport.KML
import de.esymetric.jerusalem.routing.Router
import de.esymetric.jerusalem.routing.RoutingAlgorithm
import de.esymetric.jerusalem.routing.RoutingHeuristics
import de.esymetric.jerusalem.routing.RoutingType
import de.esymetric.jerusalem.utils.Utils
import java.util.*

class TomsAStarStarRouting : RoutingAlgorithm {
    var openList: SortedSet<Node> = TreeSet()
    var openListMap = HashMap<Long, Node>()
    var closedList = HashSet<Long>()
    var nlf: PartitionedNodeListFile? = null
    var wlf: PartitionedTransitionListFile? = null
    var wcf: PartitionedWayCostFile? = null
    var type: RoutingType? = null
    var target: Node? = null
    var heuristics: RoutingHeuristics? = null
    override fun getRoute(
        start: Node, target: Node, type: RoutingType?,
        nlf: PartitionedNodeListFile?, wlf: PartitionedTransitionListFile?, wcf: PartitionedWayCostFile?,
        heuristics: RoutingHeuristics, targetNodeMasterNodes: List<Node>, maxExecutionTimeSec: Int,
        useOptimizedPath: Boolean
    ): List<Node>? {
        this.nlf = nlf
        this.wlf = wlf
        this.wcf = wcf
        this.type = type
        this.target = target
        this.heuristics = heuristics
        val maxTime = Date().time + maxExecutionTimeSec.toLong() * 1000L

        // clear
        openList.clear()
        openListMap.clear()
        closedList.clear()
        start.totalCost = calculateDistance(
            start.lat, start.lng,
            target.lat, target.lng
        ) * heuristics.estimateRemainingCost(type) / 1000.0
        start.realCostSoFar = 0.0
        openList.add(start)
        openListMap[start.uID] = start
        var node: Node
        var bestNode: Node? = null
        var count = 0
        while (openList.size > 0) {
            node = openList.first()
            openList.remove(node)
            openListMap.remove(node.uID)
            if (bestNode == null || bestNode.remainingCost() > node.remainingCost()) bestNode = node
            if (node == target) {
                if (Router.Companion.debugMode) println(
                    "final number of open nodes was "
                            + openList.size + " - closed list size was "
                            + closedList.size + " - final cost is "
                            + Utils.formatTimeStopWatch((node.totalCost.toInt() * 1000).toLong())
                )
                return getFullPath(node)
            }
            if (SAVE_STATE_AS_KML) saveStateAsKml(getFullPath(node), ++count) // for debugging
            expand(node, targetNodeMasterNodes, useOptimizedPath)
            closedList.add(node.uID)
            if (closedList.size > MAX_NODES_IN_CLOSED_LIST) break
            if (closedList.size and 0xfff == 0
                && Date().time > maxTime
            ) break
            if (Router.Companion.debugMode && closedList.size and 0xffff == 0) println(
                "closed list now contains "
                        + closedList.size + " entries"
            )
        }
        if (Router.Companion.debugMode) println(
            "no route - open list is empty - final number of open nodes was "
                    + openList.size
                    + " - closed list size was "
                    + closedList.size
        )
        bestNode ?: return null
        return getFullPath(bestNode)
    }

    fun getFullPath(sourceNode: Node): List<Node>? {
        var node: Node = sourceNode ?: return null
        val foundPath: MutableList<Node> = LinkedList()
        while (true) {
            foundPath.add(node)
            node = if (node.predecessor == null) break else {
                node.predecessor!!
            }
        }
        foundPath.reverse()
        return foundPath
    }

    fun expand(currentNode: Node, targetNodeMasterNodes: List<Node?>, useOptimizedPath: Boolean) {
        val isTargetMasterNode = targetNodeMasterNodes.contains(currentNode) // check!
        // falls es sich um eine mit dem Ziel verbunde Kreuzungsnode handelt,
        // den Original-Pfad verfolgen und nicht den optimierten Pfad, welcher
        // die targetNode �berspringen w�rde
        for (t in currentNode.listTransitions(!useOptimizedPath || isTargetMasterNode, nlf, wlf!!, wcf)) {
            var successor = t.targetNode
            if (closedList.contains(successor!!.uID)) continue

            // clone successor object - this is required because successor
            // contains search path specific information and can be in open list
            // multiple times
            successor = successor.clone() as Node
            var cost = currentNode.realCostSoFar
            val transitionCost = t.getCost(type)
            if (transitionCost == RoutingHeuristics.Companion.BLOCKED_WAY_COST) continue
            cost += transitionCost
            successor.realCostSoFar = cost
            cost += successor.getRemainingCost(target!!, type, heuristics!!)
            successor.totalCost = cost
            if (openListMap.containsKey(successor.uID)
                && cost > openListMap[successor.uID]!!.totalCost
            ) continue
            successor.predecessor = currentNode
            openList.remove(successor)
            openList.add(successor)
            openListMap[successor.uID] = successor
        }
    }

    /**
     * Enable this method to document the process of route creation.
     */
    fun saveStateAsKml(route: List<Node?>?, count: Int) {
        val kml = KML()
        val trackPts = Vector<Position>()
        for (n in route!!) {
            val p = Position()
            p.latitude = n!!.lat
            p.longitude = n.lng
            trackPts.add(p)
        }
        kml.trackPositions = trackPts
        kml.Save("current_$count.kml")
    }

    companion object {
        const val MAX_NODES_IN_CLOSED_LIST = 1000000
        const val SAVE_STATE_AS_KML = false
    }
}