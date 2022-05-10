package de.esymetric.jerusalem.ownDataRepresentation.geoData

class Coord2 {
    var x = 0.0
    var y = 0.0

    constructor() {}
    constructor(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    fun distance(c: Coord2): Double {
        val dx = c.x - x
        val dy = c.y - y
        return Math.sqrt(dx * dx + dy * dy)
    }
}