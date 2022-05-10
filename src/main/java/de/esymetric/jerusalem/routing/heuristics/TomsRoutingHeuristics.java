package de.esymetric.jerusalem.routing.heuristics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.esymetric.jerusalem.routing.RoutingHeuristics;
import de.esymetric.jerusalem.routing.RoutingType;

public class TomsRoutingHeuristics implements RoutingHeuristics {

	String[] highwayTypesNOTForPedestrians = { "motorway", "motorway_link",
			"trunk", "trunk_link", "raceway",
			"bus_guideway" };  // primary should be allowed - otherwise
								// pedestrian routing does not work for London
	Set<String> highwayTypesNOTForPedestriansMap = new HashSet<String>();

	String[] highwayTypesForCycling = { "primary", "primary_link", "secondary",
			"secondary_link", "tertiary", "unclassified", "road",
			"residential", "living_street", "service", "track", "path",
			"cycleway", "birdleway", "byway", "roundabout" };
	Set<String> highwayTypesForCyclingMap = new HashSet<String>();

	String[] highwayTypesForRacingBike = { "primary", "primary_link",
			"secondary", "secondary_link", "tertiary", "unclassified", "road",
			"residential", "living_street", "service", "cycleway", "roundabout" };
	Set<String> highwayTypesForRacingBikeMap = new HashSet<String>();

	String[] highwayTypesForMountainBike = { "secondary", "secondary_link",
			"tertiary", "unclassified", "road", "residential", "living_street",
			"service", "track", "path", "cycleway", "footway", "birdleway",
			"byway", "roundabout" };
	Set<String> highwayTypesForMountainBikeMap = new HashSet<String>();

	String[] highwayTypesForCar = { "motorway", "motorway_link", "trunk",
			"trunk_link", "primary", "primary_link", "secondary",
			"secondary_link", "tertiary", "unclassified", "road",
			"residential", "living_street", "service", "roundabout" };
	int[] highwaySpeedsForCarKMH = { 130, 60, 105, 50, 100, 50, 80, 40, 70, 60,
			60, 50, 30, 20, 30 };
	Set<String> highwayTypesForCarMap = new HashSet<String>();
	Map<String, Double> highwaySpeedsForCarMapMPerS = new HashMap<String, Double>();

	final Map<String, String> standardTagsForEstimationFoot = new HashMap<String, String>();
	final Map<String, String> standardTagsForEstimationBike = new HashMap<String, String>();
	final Map<String, String> standardTagsForEstimationCar = new HashMap<String, String>();

	public TomsRoutingHeuristics() {
		for (String t : highwayTypesNOTForPedestrians)
			highwayTypesNOTForPedestriansMap.add(t);
		for (String t : highwayTypesForCycling)
			highwayTypesForCyclingMap.add(t);
		for (String t : highwayTypesForRacingBike)
			highwayTypesForRacingBikeMap.add(t);
		for (String t : highwayTypesForMountainBike)
			highwayTypesForMountainBikeMap.add(t);
		for (String t : highwayTypesForCar)
			highwayTypesForCarMap.add(t);
		int count = 0;
		for (String t : highwayTypesForCar) {
			highwaySpeedsForCarMapMPerS.put(t,
					(double) highwaySpeedsForCarKMH[count] / 3.6);
			count++;
		}

		standardTagsForEstimationFoot.put("highway", "footway");
		standardTagsForEstimationBike.put("highway", "cycleway");
		standardTagsForEstimationCar.put("highway", "motorway");
		// this has the least costs
		// never over-estimate the time (=cost), i.e. always put the fastest highway type here
	}

	@Override
	public double calculateCost(RoutingType type,
			Map<String, String> tags, boolean isOriginalDirection) {

		String highwayType = tags.get("highway");
		if (highwayType == null)
			return BLOCKED_WAY_COST;

		switch (type) {
		default:
		case foot:
			// highway types

			if (highwayTypesNOTForPedestriansMap.contains(highwayType))
				return BLOCKED_WAY_COST;

			return 370.3704; // 1000 m / 2.7 m/s
		case bike:
			// one-way

			if (!isOriginalDirection && hasTag(tags, "oneway", "yes"))
				return BLOCKED_WAY_COST;

			// check if road is not allowed for bikes, see
			// http://wiki.openstreetmap.org/wiki/Bicycle and
			// http://www.gps-sport.net/forums/thread/2546-0/Routenplaner-Punkte-einf%FCgen-f%FChrt-oft-zu-verwirrenden-Ergebnissen
			// 24.05.2013

			if( tags.containsKey("bicycle") ) {
				String bicycle = tags.get("bicycle");
				if( "no".equals(bicycle) ) return BLOCKED_WAY_COST;
			}

			// highway types

			if (!highwayTypesForCyclingMap.contains(highwayType)
					&& (!"footway".equals(highwayType) || !hasTag(tags,
							"cycleway", "yes")))
				return BLOCKED_WAY_COST;

			return 200.0; // 1000 m / 5 m/s

		case racingBike:
			// one-way

			if (!isOriginalDirection && hasTag(tags, "oneway", "yes"))
				return BLOCKED_WAY_COST;

			// check surface

			if (tags.containsKey("surface")) {
				String surface = tags.get("surface");
				if ("unpaved".equals(surface) || "cobblestone".equals(surface)
						|| "gravel".equals(surface)
						|| "pebblestone".equals(surface)
						|| "grass".equals(surface) || "earth".equals(surface)
						|| "ground".equals(surface) || "dirt".equals(surface)
						|| "mud".equals(surface) || "sand".equals(surface))
					return BLOCKED_WAY_COST;
			}

			// check if road is not allowed for bikes, see
			// http://wiki.openstreetmap.org/wiki/Bicycle and
			// http://www.gps-sport.net/forums/thread/2546-0/Routenplaner-Punkte-einf%FCgen-f%FChrt-oft-zu-verwirrenden-Ergebnissen
			// 24.05.2013

			if( tags.containsKey("bicycle") ) {
				String bicycle = tags.get("bicycle");
				if( "no".equals(bicycle) ) return BLOCKED_WAY_COST;
			}

			// highway types

			if (!highwayTypesForRacingBikeMap.contains(highwayType)
					&& (!"footway".equals(highwayType) || !hasTag(tags,
							"cycleway", "yes")))
				return BLOCKED_WAY_COST;

			return 142.8571; // 1000 m / 7 m/s

		case mountainBike:
			// one-way

			if (!isOriginalDirection && hasTag(tags, "oneway", "yes"))
				return BLOCKED_WAY_COST;

			// highway types

			if (!highwayTypesForCyclingMap.contains(highwayType))
				return BLOCKED_WAY_COST;

			return 200.0; // 1000 m / 5 m/s

		case car:

			// one-way

			if (!isOriginalDirection && hasTag(tags, "oneway", "yes"))
				return BLOCKED_WAY_COST;

			// highway types

			if (!highwayTypesForCarMap.contains(highwayType))
				return BLOCKED_WAY_COST;

			// calculate cost

			double timeSCar = ((1000.0 / (double)highwaySpeedsForCarMapMPerS
					.get(highwayType))); // v = 14 m/s, * 1000

			if (tags.containsKey("maxspeed")) {
				try {
					int maxSpeedKMH = Integer.parseInt(tags.get("maxspeed"));
					if (maxSpeedKMH > 10 && maxSpeedKMH < 130) {
						//double maxSpeedMPS = (double) maxSpeedKMH / 3.6;
						timeSCar = 3600.0 / maxSpeedKMH; // v
																				// =
																				// 14
																				// m/s,
																				// *
																				// 100
					}
				} catch (NumberFormatException e) {
					//e.printStackTrace();
				}
			}

			return timeSCar;

		case carShortest:
			// one-way

			if (!isOriginalDirection && hasTag(tags, "oneway", "yes"))
				return BLOCKED_WAY_COST;

			// highway types

			if (!highwayTypesForCarMap.contains(highwayType))
				return BLOCKED_WAY_COST;

			//return timeSCarShortest;
			return 71.4286; // 1000 m / 14 m/s
		}
	}

	boolean hasTag(Map<String, String> tags, String k, String v) {
		if (!tags.containsKey(k))
			return false;
		return v.equals(tags.get(k));
	}

	public double estimateRemainingCost(RoutingType type) {
		Map<String, String> standardTags = null;
		switch (type) {
		case foot:
		default:
			standardTags = standardTagsForEstimationFoot;
			break;
		case bike:
		case racingBike:
		case mountainBike:
			standardTags = standardTagsForEstimationBike;
			break;
		case car:
		case carShortest:
			standardTags = standardTagsForEstimationCar;
			break;
		}

		return calculateCost(type, standardTags, true);
	}

}
