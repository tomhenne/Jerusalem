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
    private var openList: SortedSet<Node> = TreeSet()
    private var openListMap = HashMap<Long, Node>()
    private var closedList = HashSet<Long>()

    lateinit var nlf: PartitionedNodeListFile
    lateinit var wlf: PartitionedTransitionListFile
    lateinit var wcf: PartitionedWayCostFile
    lateinit var type: RoutingType
    lateinit var target: Node
    lateinit var heuristics: RoutingHeuristics

    override fun getRoute(
        start: Node, target: Node, type: RoutingType,
        nlf: PartitionedNodeListFile, wlf: PartitionedTransitionListFile, wcf: PartitionedWayCostFile,
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
                if (Router.debugMode) println(
                    "final number of open nodes was "
                            + openList.size + " - closed list size was "
                            + closedList.size + " - final cost is "
                            + Utils.formatTimeStopWatch((node.totalCost.toInt() * 1000).toLong())
                )
                return getFullPath(node)
            }
            if (SAVE_STATE_AS_KML) saveStateAsKml(getFullPath(node), ++count) // for debugging
            expand(node, targetNodeMasterNodes)
            closedList.add(node.uID)
            if (closedList.size > MAX_NODES_IN_CLOSED_LIST) break
            if (closedList.size and 0xfff == 0
                && Date().time > maxTime
            ) break
            if (Router.debugMode && closedList.size and 0xffff == 0) println(
                "closed list now contains "
                        + closedList.size + " entries"
            )
        }
        if (Router.debugMode) println(
            "no route - open list is empty - final number of open nodes was "
                    + openList.size
                    + " - closed list size was "
                    + closedList.size
        )
        bestNode ?: return null
        return getFullPath(bestNode)
    }

    private fun getFullPath(sourceNode: Node): List<Node> {
        var node: Node = sourceNode
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

    fun expand(currentNode: Node, targetNodeMasterNodes: List<Node?>) {
        // falls es sich um eine mit dem Ziel verbunde Kreuzungsnode handelt,
        // den Original-Pfad verfolgen und nicht den optimierten Pfad, welcher
        // die targetNode �berspringen w�rde
        for (t in currentNode.listTransitions(nlf, wlf, wcf)) {
            var transition = t
            if (targetNodeMasterNodes.contains(t.targetNode) && targetNodeMasterNodes.contains(currentNode) ) {
                transition = wlf.getTransition(currentNode, t.id, nlf, wcf) ?: t
            }

            var successor = transition.targetNode
            if (closedList.contains(successor!!.uID)) continue

            // clone successor object - this is required because successor
            // contains search path specific information and can be in open list
            // multiple times
            successor = successor.clone() as Node
            var cost = currentNode.realCostSoFar
            val transitionCost = transition.getCost(type)
            if (transitionCost == RoutingHeuristics.BLOCKED_WAY_COST) continue
            cost += transitionCost
            successor.realCostSoFar = cost
            cost += successor.getRemainingCost(target, type, heuristics)
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
        kml.save("current_$count.kml")
    }

    companion object {
        const val MAX_NODES_IN_CLOSED_LIST = 1000000
        const val SAVE_STATE_AS_KML = false
    }
}