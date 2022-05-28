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

* quad core
* 8GB RAM
* 64 bit operating system
* 300 GB free HD space (for the entire planet)

### ... for building the graph (for the entire planet)

* quad core
* 32GB RAM (16 GB is possible, but not very fast)
* 64 bit operating system
* 500 GB free HD space (for the entire planet)

## Getting started

1. Clone the project and use the .jar file from /etc or build the project.
2. Download planet osm or a portion of it from https://planet.openstreetmap.org/
3. Install bzip2 if you don't have it yet.
4. Put Jerusalem.jar and the bzipped osm file in a directory and build the graph:
```
bzip2 -dc planet-latest.osm.bz2 | java  -server -Xmx22000m -jar Jerusalem.jar rebuild - temp
```
4. Now you have all graph data stored in /jerusalemData (about 270 GB for the entire planet)
5. Test the graph:
```
java  -Xmx500m -jar Jerusalem.jar routingTest
```
6. You're all settled and can use the graph for routing:
```
java  -Xmx500m -jar Jerusalem.jar route foot 48.11 11.48 48.12 11.49
```

## Usage

### Route

Compute a route from A to B. If you enter an output file name, both a GPX and a KML file will be written containing the route. Resulting list of coordinates is written on stdout.

````
java -Xmx500m -jar Jerusalem.jar route 
foot|bike|racingBike|mountainBike|car|carShortest 
<latitude1> <longitude1> <latitude2> <longitude2> 
[<output-file-base-name>]
````

### Rebuild

Rebuild the planet or a subset. Input file is a bzipped osm file (e.g. "planet.osm.bz2"). If you enter "-", OSM data is read from stdin.

```
java -Xmx22000m -jar Jerusalem.jar rebuild <source-filepah>|- 
```

22 GB of heap space are required to build the planet as of 05/2022. Since the planet file is getting bigger and bigger, more will be required with later planet versions. Current planet osm statistics can be found here: https://wiki.openstreetmap.org/wiki/Stats

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



