package de.esymetric.jerusalem.ownDataRepresentation.geoData

object GPSMath {
    const val EARTH_RADIUS = 6371007.0

    /****************************************************************
     * Distances
     */

    @JvmStatic
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        var lat1 = lat1
        var lon1 = lon1
        var lat2 = lat2
        var lon2 = lon2
        lat1 = deg2Rad(lat1)
        lon1 = deg2Rad(lon1)
        lat2 = deg2Rad(lat2)
        lon2 = deg2Rad(lon2)
        val dLat = lat2 - lat1
        val dLong = lon2 - lon1
        if (dLat == 0.0 && dLong == 0.0) return 0.0
        val slat = Math.sin(dLat / 2)
        val slong = Math.sin(dLong / 2)
        val a = slat * slat + Math.cos(lat1) * Math.cos(lat2) * slong * slong
        val c = 2.0 * Math.asin(Math.min(1.0, Math.sqrt(a)))
        return EARTH_RADIUS * c
    }

    fun deg2Rad(deg: Double): Double {
        return deg / 180.0 * Math.PI
    }


}