package de.esymetric.jerusalem.rebuilding.optimizer;

import java.util.ArrayList;
import java.util.List;

import de.esymetric.jerusalem.ownDataRepresentation.Node;
import de.esymetric.jerusalem.ownDataRepresentation.Transition;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedNodeListFile;
import de.esymetric.jerusalem.ownDataRepresentation.fileSystem.PartitionedTransitionListFile;

public class TransitionsExpander {

	public List<Node> expandNodes(List<Node> nodes, PartitionedNodeListFile nlf, PartitionedTransitionListFile wlf) {
		
		List<Node> nnodes = new ArrayList<Node>();
		
		Node na = null;
		
		for(Node nb : nodes) {
			if( na != null ) {
				expandNode(na, nb, nnodes, nlf, wlf);
			}
			
			na = nb;
			nnodes.add(na);
		}
		
		return nnodes;
	}

	private void expandNode(Node na, Node nb, List<Node> nnodes, PartitionedNodeListFile nlf, PartitionedTransitionListFile wlf) {
		na.clearTransitionsCache(); // clear transitions that do not have origTargetNodes loaded
		
		List<Transition> ts = na.listTransitions(true, nlf, wlf);
		
		for(Transition t : ts) {
			if( t.targetNode.equals(nb) ) {
				
				followTransitions(na, t, nnodes, nb, nlf, wlf);
				
				return;
			}
		}
		
		System.out.println("trans not found");
	}

	private void followTransitions(Node sourceNode, Transition t, List<Node> nnodes, Node finalNode, PartitionedNodeListFile nlf, PartitionedTransitionListFile wlf) {
		Node targetNode = t.origTargetNode;
		if( targetNode == null ) targetNode = t.targetNode;
		
		if( targetNode.equals(finalNode) ) return;
		
		nnodes.add(targetNode);
		
		List<Transition> ts = targetNode.listTransitionsWithoutSameWayBack(sourceNode, true, nlf, wlf );
		if( ts.size() != 1 ) return;
		
		followTransitions(targetNode, ts.get(0), nnodes, finalNode, nlf, wlf);
	}
	
}
