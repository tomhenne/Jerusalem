package de.esymetric.jerusalem.ownDataRepresentation

import de.esymetric.jerusalem.routing.RoutingHeuristics
import de.esymetric.jerusalem.routing.RoutingType

class Transition {
    var id = 0
    @JvmField
	var nextTransitionID = 0
    @JvmField
	var targetNode: Node? = null
    @JvmField
	var origTargetNode // for TransitionOptimizer, original targetNode before optimization
            : Node? = null
    var costFoot = 0.0
    var costBike = 0.0
    var costRacingBike = 0.0
    var costMountainBike = 0.0
    var costCar = 0.0
    var costCarShortest = 0.0
    var distanceM = 0.0
    fun getCost(type: RoutingType?): Double {
        var transitionCost = 0.0
        when (type) {
            RoutingType.foot -> transitionCost = costFoot
            RoutingType.bike -> transitionCost = costBike
            RoutingType.racingBike -> transitionCost = costRacingBike
            RoutingType.mountainBike -> transitionCost = costMountainBike
            RoutingType.car -> transitionCost = costCar
            RoutingType.carShortest -> transitionCost = costCarShortest
        }
        return if (transitionCost == RoutingHeuristics.BLOCKED_WAY_COST) transitionCost else transitionCost * distanceM / 1000.0
    }

    val isAllBlocked: Boolean
        get() = costFoot == RoutingHeuristics.BLOCKED_WAY_COST && costBike == RoutingHeuristics.BLOCKED_WAY_COST && costRacingBike == RoutingHeuristics.BLOCKED_WAY_COST && costMountainBike == RoutingHeuristics.BLOCKED_WAY_COST && costCar == RoutingHeuristics.BLOCKED_WAY_COST && costCarShortest == RoutingHeuristics.BLOCKED_WAY_COST
}