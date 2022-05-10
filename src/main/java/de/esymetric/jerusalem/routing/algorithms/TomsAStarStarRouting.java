package de.esymetric.jerusalem.routing.algorithms;

import de.esymetric.jerusalem.ownDataRepresentation.Node;
import de.esymetric.jerusalem.ownDataRepresentation.Transition;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedNodeListFile;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedTransitionListFile;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedWayCostFile;
import de.esymetric.jerusalem.ownDataRepresentation.geoData.GPSMath;
import de.esymetric.jerusalem.ownDataRepresentation.geoData.Position;
import de.esymetric.jerusalem.ownDataRepresentation.geoData.importExport.KML;
import de.esymetric.jerusalem.routing.Router;
import de.esymetric.jerusalem.routing.RoutingAlgorithm;
import de.esymetric.jerusalem.routing.RoutingHeuristics;
import de.esymetric.jerusalem.routing.RoutingType;
import de.esymetric.jerusalem.utils.Utils;

import java.util.*;

public class TomsAStarStarRouting implements RoutingAlgorithm {

    final static int MAX_NODES_IN_CLOSED_LIST = 1000000;
    final static boolean SAVE_STATE_AS_KML = false;

    SortedSet<Node> openList = new TreeSet<Node>();
    HashMap<Long, Node> openListMap = new HashMap<Long, Node>();
    HashSet<Long> closedList = new HashSet<Long>();

    PartitionedNodeListFile nlf;
    PartitionedTransitionListFile wlf;
    PartitionedWayCostFile wcf;
    RoutingType type;
    Node target;
    RoutingHeuristics heuristics;

    @Override
    public List<Node> getRoute(Node start, Node target, RoutingType type,
                               PartitionedNodeListFile nlf, PartitionedTransitionListFile wlf, PartitionedWayCostFile wcf,
                               RoutingHeuristics heuristics, List<Node> targetNodeMasterNodes, int maxExecutionTimeSec,
                               boolean useOptimizedPath) {


        this.nlf = nlf;
        this.wlf = wlf;
        this.wcf = wcf;
        this.type = type;
        this.target = target;
        this.heuristics = heuristics;

        long maxTime = new Date().getTime() + (long) maxExecutionTimeSec
                * 1000L;

        // clear

        openList.clear();
        openListMap.clear();
        closedList.clear();

        start.totalCost = GPSMath.CalculateDistance(start.lat, start.lng,
                target.lat, target.lng)
                * heuristics.estimateRemainingCost(type) / 1000.0;
        start.realCostSoFar = 0;
        openList.add(start);
        openListMap.put(start.getUID(), start);
        Node node;
        Node bestNode = null;
        int count = 0;

        while (openList.size() > 0) {
            node = openList.first();
            openList.remove(node);
            openListMap.remove(node.getUID());

            if (bestNode == null || bestNode.remainingCost() > node.remainingCost())
                bestNode = node;


            if (node.equals(target)) {
                if (Router.getDebugMode())
                    System.out.println("final number of open nodes was "
                            + openList.size() + " - closed list size was "
                            + closedList.size() + " - final cost is "
                            + Utils.formatTimeStopWatch((int) node.totalCost * 1000));
                return getFullPath(node);
            }
            if(SAVE_STATE_AS_KML)
                saveStateAsKml(getFullPath(node), ++count);  // for debugging
            expand(node, targetNodeMasterNodes, useOptimizedPath);
            closedList.add(node.getUID());
            if (closedList.size() > MAX_NODES_IN_CLOSED_LIST)
                break;
            if ((closedList.size() & 0xfff) == 0
                    && new Date().getTime() > maxTime)
                break;
            if (Router.getDebugMode() && (closedList.size() & 0xffff) == 0)
                System.out.println("closed list now contains "
                        + closedList.size() + " entries");

        }

        if (Router.getDebugMode())
            System.out
                    .println("no route - open list is empty - final number of open nodes was "
                            + openList.size()
                            + " - closed list size was "
                            + closedList.size());
        return getFullPath(bestNode);
    }

    List<Node> getFullPath(Node node) {
        if (node == null)
            return null;

        List<Node> foundPath = new LinkedList<Node>();
        for (; ; ) {
            foundPath.add(node);
            if (node.predecessor == null)
                break;
            else {
                node = node.predecessor;
            }
        }
        Collections.reverse(foundPath);
        return foundPath;
    }

    void expand(Node currentNode, List<Node> targetNodeMasterNodes, boolean useOptimizedPath) {

        boolean isTargetMasterNode = targetNodeMasterNodes.contains(currentNode);  // check!
        // falls es sich um eine mit dem Ziel verbunde Kreuzungsnode handelt,
        // den Original-Pfad verfolgen und nicht den optimierten Pfad, welcher
        // die targetNode �berspringen w�rde

        for (Transition t : currentNode.listTransitions(!useOptimizedPath || isTargetMasterNode, nlf, wlf, wcf)) {
            Node successor = t.targetNode;

            if (closedList.contains(successor.getUID()))
                continue;

            // clone successor object - this is required because successor
            // contains search path specific information and can be in open list
            // multiple times

            successor = (Node) successor.clone();

            double cost = currentNode.realCostSoFar;

            double transitionCost = t.getCost(type);
            if (transitionCost == (double) RoutingHeuristics.BLOCKED_WAY_COST)
                continue;
            cost += transitionCost;

            successor.realCostSoFar = cost;

            cost += successor.getRemainingCost(target, type, heuristics);

            successor.totalCost = cost;

            if (openListMap.containsKey(successor.getUID())
                    && cost > openListMap.get(successor.getUID()).totalCost)
                continue;

            successor.predecessor = currentNode;

            openList.remove(successor);
            openList.add(successor);
            openListMap.put(successor.getUID(), successor);
        }
    }

    /**
     * Enable this method to document the process of route creation.
     */
    void saveStateAsKml(List<Node> route, int count) {
        KML kml = new KML();
        Vector<Position> trackPts = new Vector<Position>();
        for (Node n : route) {
            Position p = new Position();
            p.latitude = n.lat;
            p.longitude = n.lng;
            trackPts.add(p);
        }
        kml.setTrackPositions(trackPts);
        kml.Save("current_" + count + ".kml");
    }

}
