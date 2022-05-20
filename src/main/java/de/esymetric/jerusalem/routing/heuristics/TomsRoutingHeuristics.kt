package de.esymetric.jerusalem.routing.heuristics

import de.esymetric.jerusalem.routing.RoutingHeuristics
import de.esymetric.jerusalem.routing.RoutingType

class TomsRoutingHeuristics : RoutingHeuristics {
    var highwayTypesNOTForPedestrians = arrayOf(
        "motorway", "motorway_link",
        "trunk", "trunk_link", "raceway",
        "bus_guideway"
    ) // primary should be allowed - otherwise

    // pedestrian routing does not work for London
    var highwayTypesNOTForPedestriansMap: MutableSet<String> = HashSet()
    var highwayTypesForCycling = arrayOf(
        "primary", "primary_link", "secondary",
        "secondary_link", "tertiary", "unclassified", "road",
        "residential", "living_street", "service", "track", "path",
        "cycleway", "birdleway", "byway", "roundabout"
    )
    var highwayTypesForCyclingMap: MutableSet<String> = HashSet()
    var highwayTypesForRacingBike = arrayOf(
        "primary", "primary_link",
        "secondary", "secondary_link", "tertiary", "unclassified", "road",
        "residential", "living_street", "service", "cycleway", "roundabout"
    )
    var highwayTypesForRacingBikeMap: MutableSet<String> = HashSet()
    var highwayTypesForMountainBike = arrayOf(
        "secondary", "secondary_link",
        "tertiary", "unclassified", "road", "residential", "living_street",
        "service", "track", "path", "cycleway", "footway", "birdleway",
        "byway", "roundabout"
    )
    var highwayTypesForMountainBikeMap: MutableSet<String> = HashSet()
    var highwayTypesForCar = arrayOf(
        "motorway", "motorway_link", "trunk",
        "trunk_link", "primary", "primary_link", "secondary",
        "secondary_link", "tertiary", "unclassified", "road",
        "residential", "living_street", "service", "roundabout"
    )
    var highwaySpeedsForCarKMH = intArrayOf(
        130, 60, 105, 50, 100, 50, 80, 40, 70, 60,
        60, 50, 30, 20, 30
    )
    var highwayTypesForCarMap: MutableSet<String> = HashSet()
    var highwaySpeedsForCarMapMPerS: MutableMap<String, Double> = HashMap()
    val standardTagsForEstimationFoot: MutableMap<String, String> = HashMap()
    val standardTagsForEstimationBike: MutableMap<String, String> = HashMap()
    val standardTagsForEstimationCar: MutableMap<String, String> = HashMap()

    init {
        for (t in highwayTypesNOTForPedestrians) highwayTypesNOTForPedestriansMap.add(t)
        for (t in highwayTypesForCycling) highwayTypesForCyclingMap.add(t)
        for (t in highwayTypesForRacingBike) highwayTypesForRacingBikeMap.add(t)
        for (t in highwayTypesForMountainBike) highwayTypesForMountainBikeMap.add(t)
        for (t in highwayTypesForCar) highwayTypesForCarMap.add(t)
        var count = 0
        for (t in highwayTypesForCar) {
            highwaySpeedsForCarMapMPerS[t] = highwaySpeedsForCarKMH[count].toDouble() / 3.6
            count++
        }
        standardTagsForEstimationFoot["highway"] = "footway"
        standardTagsForEstimationBike["highway"] = "cycleway"
        standardTagsForEstimationCar["highway"] = "motorway"
        // this has the least costs
        // never over-estimate the time (=cost), i.e. always put the fastest highway type here
    }

    override fun calculateCost(
        type: RoutingType?,
        tags: Map<String, String>?, isOriginalDirection: Boolean
    ): Double {
        val highwayType = tags!!["highway"] ?: return RoutingHeuristics.Companion.BLOCKED_WAY_COST
        return when (type) {
            RoutingType.foot -> {
                // highway types
                if (highwayTypesNOTForPedestriansMap.contains(highwayType)) RoutingHeuristics.Companion.BLOCKED_WAY_COST else 370.3704
                // 1000 m / 2.7 m/s
            }
            RoutingType.bike -> {
                // one-way
                if (!isOriginalDirection && hasTag(
                        tags,
                        "oneway",
                        "yes"
                    )
                ) return RoutingHeuristics.Companion.BLOCKED_WAY_COST

                // check if road is not allowed for bikes, see
                // http://wiki.openstreetmap.org/wiki/Bicycle and
                // http://www.gps-sport.net/forums/thread/2546-0/Routenplaner-Punkte-einf%FCgen-f%FChrt-oft-zu-verwirrenden-Ergebnissen
                // 24.05.2013
                if (tags.containsKey("bicycle")) {
                    val bicycle = tags["bicycle"]
                    if ("no" == bicycle) return RoutingHeuristics.Companion.BLOCKED_WAY_COST
                }

                // highway types
                if (!highwayTypesForCyclingMap.contains(highwayType)
                    && ("footway" != highwayType || !hasTag(
                        tags,
                        "cycleway", "yes"
                    ))
                ) RoutingHeuristics.Companion.BLOCKED_WAY_COST else 200.0
                // 1000 m / 5 m/s
            }
            RoutingType.racingBike -> {
                // one-way
                if (!isOriginalDirection && hasTag(
                        tags,
                        "oneway",
                        "yes"
                    )
                ) return RoutingHeuristics.Companion.BLOCKED_WAY_COST

                // check surface
                if (tags.containsKey("surface")) {
                    val surface = tags["surface"]
                    if ("unpaved" == surface || "cobblestone" == surface || "gravel" == surface || "pebblestone" == surface || "grass" == surface || "earth" == surface || "ground" == surface || "dirt" == surface || "mud" == surface || "sand" == surface) return RoutingHeuristics.Companion.BLOCKED_WAY_COST
                }

                // check if road is not allowed for bikes, see
                // http://wiki.openstreetmap.org/wiki/Bicycle and
                // http://www.gps-sport.net/forums/thread/2546-0/Routenplaner-Punkte-einf%FCgen-f%FChrt-oft-zu-verwirrenden-Ergebnissen
                // 24.05.2013
                if (tags.containsKey("bicycle")) {
                    val bicycle = tags["bicycle"]
                    if ("no" == bicycle) return RoutingHeuristics.Companion.BLOCKED_WAY_COST
                }

                // highway types
                if (!highwayTypesForRacingBikeMap.contains(highwayType)
                    && ("footway" != highwayType || !hasTag(
                        tags,
                        "cycleway", "yes"
                    ))
                ) RoutingHeuristics.Companion.BLOCKED_WAY_COST else 142.8571
                // 1000 m / 7 m/s
            }
            RoutingType.mountainBike -> {
                // one-way
                if (!isOriginalDirection && hasTag(
                        tags,
                        "oneway",
                        "yes"
                    )
                ) return RoutingHeuristics.Companion.BLOCKED_WAY_COST

                // highway types
                if (!highwayTypesForCyclingMap.contains(highwayType)) RoutingHeuristics.Companion.BLOCKED_WAY_COST else 200.0
                // 1000 m / 5 m/s
            }
            RoutingType.car -> {

                // one-way
                if (!isOriginalDirection && hasTag(
                        tags,
                        "oneway",
                        "yes"
                    )
                ) return RoutingHeuristics.Companion.BLOCKED_WAY_COST

                // highway types
                if (!highwayTypesForCarMap.contains(highwayType)) return RoutingHeuristics.Companion.BLOCKED_WAY_COST

                // calculate cost
                var timeSCar = 1000.0 / highwaySpeedsForCarMapMPerS[highwayType] as Double // v = 14 m/s, * 1000
                if (tags.containsKey("maxspeed")) {
                    try {
                        val maxSpeedKMH = tags["maxspeed"]!!.toInt()
                        if (maxSpeedKMH > 10 && maxSpeedKMH < 130) {
                            //double maxSpeedMPS = (double) maxSpeedKMH / 3.6;
                            timeSCar = 3600.0 / maxSpeedKMH // v
                            // =
                            // 14
                            // m/s,
                            // *
                            // 100
                        }
                    } catch (e: NumberFormatException) {
                        //e.printStackTrace();
                    }
                }
                timeSCar
            }
            RoutingType.carShortest -> {
                // one-way
                if (!isOriginalDirection && hasTag(
                        tags,
                        "oneway",
                        "yes"
                    )
                ) return RoutingHeuristics.Companion.BLOCKED_WAY_COST

                // highway types
                if (!highwayTypesForCarMap.contains(highwayType)) RoutingHeuristics.Companion.BLOCKED_WAY_COST else 71.4286

                //return timeSCarShortest;
                // 1000 m / 14 m/s
            }
            else -> {
                if (highwayTypesNOTForPedestriansMap.contains(highwayType)) RoutingHeuristics.Companion.BLOCKED_WAY_COST else 370.3704
            }
        }
    }

    fun hasTag(tags: Map<String, String>?, k: String, v: String): Boolean {
        return if (!tags!!.containsKey(k)) false else v == tags[k]
    }

    override fun estimateRemainingCost(type: RoutingType?): Double {
        var standardTags: Map<String, String>? = null
        standardTags = when (type) {
            RoutingType.foot -> standardTagsForEstimationFoot
            RoutingType.bike, RoutingType.racingBike, RoutingType.mountainBike -> standardTagsForEstimationBike
            RoutingType.car, RoutingType.carShortest -> standardTagsForEstimationCar
            else -> standardTagsForEstimationFoot
        }
        return calculateCost(type, standardTags, true)
    }
}