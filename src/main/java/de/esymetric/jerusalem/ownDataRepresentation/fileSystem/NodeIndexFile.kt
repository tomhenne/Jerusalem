package de.esymetric.jerusalem.ownDataRepresentation.fileSystem

interface NodeIndexFile {
    class NodeIndexNodeDescriptor {
        var id = 0
        var latInt = 0
        var lngInt = 0
    }

    fun setID(lat: Double, lng: Double, id: Int): Int
    fun getID(lat: Double, lng: Double): Int
    fun getIDPlusSourroundingIDs(lat: Double, lng: Double, radius: Int): List<NodeIndexNodeDescriptor>
    val capacity: Int
    fun close()
    val writeCacheHitRatio: Float
    val readCacheHitRatio: Float
    val maxCacheSize: Int
}