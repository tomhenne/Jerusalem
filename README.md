#JERUSALEM

##Java Enabled Routing Using Speedy Algorithms for Largely Extended Maps 

JERUSALEM is the name of the new routing engine used in GPS-Sport.net RoutePlanner.

Facts:

* supports 6 routing modes: by foot, bike, mountain bike, racing bike, car (fastest), car (shortest)
* supports routing on the entire planet (whereever OpenStreetMap  has data)
* uses up-to-date planet file from [http://www.OpenStreetMap.org]
* will be open source when the source code is ready for publication (maybe in 2-4 months)
* currently used for GPS-Sport.net RoutePlanner and will also be used in Run.GPS in the future
* 100% Java

Live demo on Run.GPS Routeplanner: <http://rp.gps-sport.net>

##Minimum Requirements

* quad core
* 4GB memory
* 64 bit operating system

##Usage

###Route

Compute a route from A to B. If you enter an output file name, both a GPX and a KML file will be written containing the route. Resulting list of coordinates is written on stdout.

````
java -jar Jerusalem.jar route 
foot|bike|racingBike|mountainBike|car|carShortest 
<latitude1> <longitude1> <latitude2> <longitude2> 
[<output-file-base-name>]
````

###Rebuild

Rebuild the planet or a subset. Input file is a bzipped osm file (e.g. "planet.osm.bz2"). If you enter "-", OSM data is read from stdin.

```
java -jar Jerusalem.jar rebuild <source-filepah>|- 
<highest-node-id-in-Open-Street-Map>
```

###Clean

Remove all temporary files (resulting from the rebuild).

```
java -jar Jerusalem.jar clean
```

###Test

Does a lot of test runs (but only in the region of Bavaria (Germany), so you need to have that area) and outputs the results as GPX and KML.

```
java -jar Jerusalem.jar routingTest
```

