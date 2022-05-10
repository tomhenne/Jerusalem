package de.esymetric.jerusalem.ownDataRepresentation.geoData

class Position {
    @JvmField
    var latitude = 0.0
    @JvmField
    var longitude = 0.0
    var altitude = 0.0
    var elapsedTimeSec = 0
    var nrOfTransitions: Byte = 0
    fun Equals(other: Any?): Boolean {
        return if (other !is Position) false else other.latitude == latitude && other.longitude == longitude
    }

    fun GetHashCode(): Int {
        return super.hashCode()
    }

    fun IsNull(): Boolean {
        return latitude == 0.0 && longitude == 0.0
    }

    fun distanceTo(p: Position): Double {
        return GPSMath.calculateDistance(p.latitude, p.longitude, latitude, longitude)
    }
}