package de.esymetric.jerusalem.ownDataRepresentation;

import de.esymetric.jerusalem.routing.RoutingHeuristics;
import de.esymetric.jerusalem.routing.RoutingType;

public class Transition {
	public int id;
	public int nextTransitionID;
	public Node targetNode;
	public Node origTargetNode;  // for TransitionOptimizer, original targetNode before optimization
	public double costFoot;
	public double costBike;
	public double costRacingBike;
	public double costMountainBike;
	public double costCar;
	public double costCarShortest;
	public double distanceM;
	
	public double  getCost(RoutingType type) {
		double transitionCost = 0.0; 
		switch(type) {
		case foot:
			transitionCost = costFoot;
			break;
		case bike:
			transitionCost = costBike;
			break;
		case racingBike:
			transitionCost = costRacingBike;
			break;
		case mountainBike:
			transitionCost = costMountainBike;
			break;
		case car:
			transitionCost = costCar;
			break;
		case carShortest:
			transitionCost = costCarShortest;
			break;
		}
		if( transitionCost == RoutingHeuristics.BLOCKED_WAY_COST ) return transitionCost;
		
		return transitionCost * distanceM / 1000.0;
	}
	
	public boolean isAllBlocked() {
		return
		costFoot == RoutingHeuristics.BLOCKED_WAY_COST &&
		costBike == RoutingHeuristics.BLOCKED_WAY_COST &&
		costRacingBike == RoutingHeuristics.BLOCKED_WAY_COST &&
		costMountainBike == RoutingHeuristics.BLOCKED_WAY_COST &&
		costCar == RoutingHeuristics.BLOCKED_WAY_COST &&
		costCarShortest == RoutingHeuristics.BLOCKED_WAY_COST;
	}
}
