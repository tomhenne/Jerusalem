package de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.longOsm2OwnMaps

interface LongOsmNodeID2OwnIDMap {
    val numberOfUsedArrays: Int
    fun put(osmNodeID: Long, ownNodeID: Int): Boolean
    operator fun get(osmNodeID: Long): Int
    fun close()
    fun delete()
}