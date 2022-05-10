package de.esymetric.jerusalem.ownDataRepresentation.geoData

class Coord2H {
    var x = 0.0
    var y = 0.0
    var heading = 0.0

    constructor() {}
    constructor(x: Double, y: Double, heading: Double) {
        this.x = x
        this.y = y
        this.heading = heading
    }
}