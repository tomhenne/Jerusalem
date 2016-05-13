package de.esymetric.jerusalem.ownDataRepresentation;

import java.util.ArrayList;
import java.util.List;

import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.LatLonDir;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedNodeListFile;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedTransitionListFile;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedWayCostFile;
import de.esymetric.jerusalem.ownDataRepresentation.geoData.GPSMath;
import de.esymetric.jerusalem.routing.RoutingHeuristics;
import de.esymetric.jerusalem.routing.RoutingType;

public class Node implements Comparable<Node> {
	public long id; // used for ownID, but in Rebuilder also for osmID
	public double lat;
	public double lng;
	// public int nextNodeID;
	public int transitionID;

	// routing
	public double totalCost; // real cost + estimated cost
	public double realCostSoFar; // real cost from the start node to this node
	public Node predecessor;

	final static double LAT_OFFS = 90.0;
	final static double LNG_OFFS = 180.0;

	private List<Transition> transitions;

	private long uid;
	private short latLonDirKey;

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Node) {
			Node n = (Node) obj;
				return n.id == id && // ID is not unique any more ...
						n.getUID() == getUID(); 
		} else
			return false;
	};

	/*
	@Override
	public int hashCode() { return (int)id << 16 + getLatLonDirKey(); };
	*/
	
	public void setLatLonDirKey(short llk) {
		latLonDirKey = llk;
	}

	public short getLatLonDirKey() {
		if (latLonDirKey == 0) {

			LatLonDir lld = new LatLonDir(lat, lng);
			latLonDirKey = lld.getShortKey();
		}
		return latLonDirKey;
	}

	public long getUID() {
		if (uid != 0)
			return uid;

		uid = (long) id << 32;
		int fileID = ((int) (lat + LAT_OFFS) << 16) | (int) (lng + LNG_OFFS);
		uid |= fileID;
		return uid;
	}

	@Override
	public Object clone() {
		Node n = new Node();
		n.id = id;
		n.uid = uid;
		n.lat = lat;
		n.lng = lng;
		// n.nextNodeID = nextNodeID;
		n.transitionID = transitionID;
		n.latLonDirKey = latLonDirKey;
		return n;
	}

	// isOriginalDirection: on a one-way street cars and bikes cannot go in the
	// opposite direction
	public void addTransition(Node targetNode, PartitionedNodeListFile nlf,
			PartitionedTransitionListFile wlf, int wayCostID,
			short wayCostLatLonDirKey, RoutingHeuristics heuristics,
			boolean isOriginalDirection) {

		double distanceM = GPSMath.CalculateDistance(lat, lng, targetNode.lat,
				targetNode.lng);
		transitionID = wlf.insertTransition(this, targetNode, distanceM,
				transitionID, wayCostID, wayCostLatLonDirKey);
		nlf.setTransitionID(lat, lng, (int) id, transitionID); // first
																// transition
																// for this node
	}

	public int getNumberOfTransitionsIfTransitionsAreLoaded() {
		if( transitions == null ) return -1;
		else return transitions.size();
	}
	
	public List<Transition> listTransitions(boolean loadOriginalTargetNodes, PartitionedNodeListFile nlf,
			PartitionedTransitionListFile wlf) {
		return listTransitions(loadOriginalTargetNodes, nlf, wlf, null);
	}

	public List<Transition> listTransitions(boolean loadOriginalTargetNodes, PartitionedNodeListFile nlf, 
			PartitionedTransitionListFile wlf, PartitionedWayCostFile wcf) {
		if (transitions != null)
			return transitions; // return cached version

		transitions = new ArrayList<Transition>();
		if (transitionID == -1)
			return transitions;
		int tID = transitionID;
		for (;;) {
			Transition t = wlf.getTransition(this, tID, loadOriginalTargetNodes, nlf, wcf);
			if( t == null )
				break; // bug in data
			transitions.add(t);
			if (t.nextTransitionID == -1)
				break;
			if( t.nextTransitionID == tID )
				break; // bug in data
			tID = t.nextTransitionID;
		}
		return transitions;
	}

	public List<Transition> listTransitionsWithoutSameWayBack(Node predecessor, boolean loadOriginalTargetNodes, 
			PartitionedNodeListFile nlf, PartitionedTransitionListFile wlf) {
		return listTransitionsWithoutSameWayBack(predecessor, loadOriginalTargetNodes, nlf, wlf, null);
	}

	public List<Transition> listTransitionsWithoutSameWayBack(Node predecessor,
			boolean loadOriginalTargetNodes, 
			PartitionedNodeListFile nlf, PartitionedTransitionListFile wlf,
			PartitionedWayCostFile wcf) {
		if (transitions == null)
			listTransitions(loadOriginalTargetNodes, nlf, wlf, wcf);
		List<Transition> l = new ArrayList<Transition>();
		for (Transition t : transitions)
			if (!t.targetNode.equals(predecessor)  && 
					(t.origTargetNode == null || !t.origTargetNode.equals(predecessor))
					)
				l.add(t);
		return l;
	}

	public String toString() {
		return "" + id + " " + lat + " " + lng + " f=" + totalCost;
	}
	
	public void clearTransitionsCache() { transitions = null; }

	@Override
	public int compareTo(Node o) {
		if (equals(o))
			return 0;
		if (totalCost < o.totalCost)
			return -1;
		else if (totalCost > o.totalCost)
			return 1;
		return getUID() < o.getUID() ? -1 : 1;
	}

	public double getRemainingCost(Node target, RoutingType type,
			RoutingHeuristics heuristics) {
		double d = GPSMath.CalculateDistance(lat, lng, target.lat, target.lng);
		return d * heuristics.estimateRemainingCost(type) / 1000.0;
	}

	public double remainingCost() {
		return totalCost - realCostSoFar;
	}

	public void loadByID(LatLonDir lld, PartitionedNodeListFile nlf) {
		nlf.getNode(this, lld, (int) id);
	}
	
	public List<Node> findConnectedMasterNodes(PartitionedNodeListFile nlf, PartitionedTransitionListFile wlf) {
		List<Node> nodes = new ArrayList<Node>();
		
		List<Transition> ts = listTransitions(true, nlf, wlf);
		if( ts.size() > 2 ) {  // is master node
			nodes.add(this);
			return nodes;
		}
		
		// debug
		
		//if( ts.size() == 1 )
		//	System.out.print("special");
		
		
		for(Transition t : ts)
			nodes.add(searchMasterNode(t.targetNode, this, nlf, wlf));
		
		
		// verify / debug
		
		//for(Node nx : nodes)
		//	if( nx.transitions.size() <= 2 )
		//		System.out.print("not a master node");
		
		return nodes;
	}
	
	Node searchMasterNode(Node n, Node comingFromNode, PartitionedNodeListFile nlf, PartitionedTransitionListFile wlf) {
		List<Transition> ts = n.listTransitionsWithoutSameWayBack(comingFromNode, true, nlf, wlf);
		if( ts.size() == 1 )
			return searchMasterNode( ts.get(0).targetNode, n, nlf, wlf);
		else return n;
	}
}
