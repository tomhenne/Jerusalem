package de.esymetric.jerusalem.osmDataRepresentation.osm2ownMaps.longOsm2OwnMaps;

public interface LongOsmNodeID2OwnIDMap {
    int getNumberOfUsedArrays();

    boolean put(long osmNodeID, int ownNodeID);

    int get(long osmNodeID);

    void close();

    void delete();

}
