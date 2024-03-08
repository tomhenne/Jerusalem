# JERUSALEM

## Java Enabled Routing Using Speedy Algorithms for Largely Extended Maps 

JERUSALEM is the name of the routing engine used in GPS-Sport.net RoutePlanner.

Facts:

* Provides fast routing covering the entire planet on low-cost hardware
* Supports 6 routing modes: by foot, bike, mountain bike, racing bike, car (fastest), car (shortest)
* 100% Kotlin

**TRY IT OUT IN ACTION** on Run.GPS Routeplanner: <http://rp.gps-sport.net>

(you need to create a free account on www.gps-sport.net first)

## Minimum requirements 
### ...for running the routing service 

* 8GB RAM
* 350 GB free HD space (for the entire planet)

### ... for building the graph (for the entire planet)

* 32GB RAM (16 GB is possible, but not very fast)
* 1 TB free HD space (for the planet.osm file, temp data and the planet build)

## Getting started

1. Clone the project and use the .jar file from /etc or build the project.
2. Download planet osm or a portion of it from https://planet.openstreetmap.org/
3. Install bzip2 if you don't have it yet.
4. Put Jerusalem.jar and the bzipped osm file in a directory and build the graph (takes about 24h for the entire planet):
```
bzip2 -dc planet-latest.osm.bz2 | java  -server -Xmx28000m -jar Jerusalem.jar rebuild - temp
```
4. Now you have all graph data stored in /jerusalemData (about 270 GB for the entire planet). All temporary data can be deleted.
5. Test the graph:
```
java  -Xmx900m -jar Jerusalem.jar routingTest
```
6. You're all settled and can use the graph for routing:
```
java  -Xmx900m -jar Jerusalem.jar route foot 48.11 11.48 48.12 11.49
```

## Usage

### Route

Compute a route from A to B. If you enter an output file name, both a GPX and a KML file will be written containing the route. Resulting list of coordinates is written on stdout.

````
java -Xmx900m -jar Jerusalem.jar route 
foot|bike|racingBike|mountainBike|car|carShortest 
<latitude1> <longitude1> <latitude2> <longitude2> 
[<output-file-base-name>]
````

### Rebuild

Rebuild the planet or a subset. Input file is a bzipped osm file (e.g. "planet.osm.bz2"). If you enter "-", OSM data is read from stdin.

```
java -Xmx22000m -jar Jerusalem.jar rebuild <source-filepah>|- 
```

28 GB of heap space are required to build the planet as of 03/2024. Since the planet file is getting bigger and bigger, more will be required with later planet versions. On a Macbook Pro M3 Pro the planet rebuild currently takes about 2 days. Current planet osm statistics can be found here: https://wiki.openstreetmap.org/wiki/Stats

### Clean

Remove all temporary files (resulting from the rebuild).

```
java -jar Jerusalem.jar clean
```

### Test

Does a lot of test runs (but only in the region of Bavaria (Germany), so you need to have that area) and outputs the results as GPX and KML.

```
java -jar Jerusalem.jar routingTest
```

## Files

Each file is stored in a latitude/longitude sector in the file system.
E.g. 

```
/lat_138/lng_191/nodes.dat
```

contains the list of nodes for latitude 48 (138 - 90) and longitude 11 (191 - 180).

| File                  | Description                                             |
|-----------------------|---------------------------------------------------------|
| transitions.data      | Relations between nodes including distance.             |
| wayCost.data          | Cost for traversal stored for each routing mode.        |
| nodes.data            | List of nodes in this sector.                           |
| quadtreeIndex.data    | Fast search index for finding nodes by geo coordinates. |
| quadtreeNodeList.data | List of nodes contained in the quadtree leaves.         |

### transitions.data

| Offset | Data type | Field               | Description                             |
|--------|-----------|---------------------|-----------------------------------------|
| 0      | INT       | targetNodeID        | ID of target node                       |
| 4      | INT       | nextTransitionID    | ID of the next transition (linked list) |
| 8      | INT       | wayCostID           | ID of the way                           |
| 12     | SHORT     | wayCostLatLonDirKey | ID of the lat/lon directory             |
| 14     | FLOAT     | distance            | distance in meters                      |

### wayCost.data

| Offset | Data type | Field            | Description                          |
|--------|-----------|------------------|--------------------------------------|
| 0      | USHORT    | costFoot         | by foot cost per meter               |
| 2      | USHORT    | costBike         | by bike cost per meter               |
| 4      | USHORT    | costRacingBike   | by racing bike cost per meter        |
| 6      | USHORT    | costMountainBike | by mountain bike cost per meter      |
| 8      | USHORT    | costCar          | by car cost per meter (fastest way)  |
| 10     | USHORT    | costCarShortest  | by car cost per meter (shortest way) |

### nodes.data

| Offset | Data type | Field        | Description                                                                             |
|--------|-----------|--------------|-----------------------------------------------------------------------------------------|
| 0      | FLOAT     | lat          | latitude                                                                                |
| 4      | FLOAT     | lng          | longitude                                                                               |
| 8      | INT       | transitionId | id of the first of the transitions of this node (transitions are stored as linked list) |

### quadTreeIndex.data

This is a tree structure with ten leaves for each node.
Lat/lng coordinates are first transformed into integers, e.g.
(lat/lng) = (48.11, 11.48) >> ((lat + 90) * 1000, (lng + 180) * 1000) = (138110, lngInt=191480)

The digits are rotatingly taken from latitude and longitude, so the tree path in the example would be
1 -> 1 -> 3 -> 9 -> 8 -> 1 -> 1 -> 4 -> 1 -> 8 -> 0 -> 0

| Offset | Data type | Field    | Description                       |
|--------|-----------|----------|-----------------------------------|
| 0      | INT       | nextID_0 | points to the next node for key 0 |
| 4      | INT       | nextID_1 | points to the next node for key 1 |
| 8      | INT       | nextID_2 | points to the next node for key 2 |
| 12     | INT       | nextID_3 | points to the next node for key 3 |
| 16     | INT       | nextID_4 | points to the next node for key 4 |
| 20     | INT       | nextID_5 | points to the next node for key 5 |
| 24     | INT       | nextID_6 | points to the next node for key 6 |
| 28     | INT       | nextID_7 | points to the next node for key 7 |
| 32     | INT       | nextID_8 | points to the next node for key 8 |
| 26     | INT       | nextID_9 | points to the next node for key 9 |

### quadTreeNodeList.data

| Offset | Data type | Field  | Description                                 |
|--------|-----------|--------|---------------------------------------------|
| 0      | INT       | nodeID | ID of the node in nodes.data                |
| 4      | INT       | nextID | ID of next entry in this file (linked list) |




