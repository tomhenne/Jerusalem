package de.esymetric.jerusalem.routing;

import java.util.Map;


public interface RoutingHeuristics {
	public final static double BLOCKED_WAY_COST = 999999.0; // sec
	
	/**
	 * Calculates the routing cost for a given routing type (vehicle) and way type.
	 * 
	 * @param type Routing type
	 * @param tags Original tags from OpenStreetMap, e.g. defining highway type
	 * @param isOriginalDirection True if the transition is in original direction, false if it's backward (relevant for one-way streets)
	 * @return Expected duration for a distance of 1000 meters in seconds
	 */
	public double calculateCost(RoutingType type, Map<String, String> tags, boolean isOriginalDirection);

	/**
	 * Estimates the transition costs for a given routing type.
	 * 
	 * @param type Routing type
	 * @return Minimum possible cost (never over-estimate the cost) in seconds per 1000 meters
	 */
	public double estimateRemainingCost(RoutingType type);
}
