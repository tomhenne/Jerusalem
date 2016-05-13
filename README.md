#JERUSALEM

##Java Enabled Routing Using Speedy Algorithms for Largely Extended Maps 

JERUSALEM is the name of the new routing engine used in GPS-Sport.net RoutePlanner.

Facts:

* Supports 6 routing modes: by foot, bike, mountain bike, racing bike, car (fastest), car (shortest)
* Supports routing on the entire planet (whereever OpenStreetMap  has data) (and is fast enough to handle the entire planet even on a slow computer)
* Uses up-to-date planet file from <http://www.OpenStreetMap.org>
* 100% Java

**TRY IT OUT IN ACTION** on Run.GPS Routeplanner: <http://rp.gps-sport.net>

(you need to create a free account on www.gps-sport.net first)

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

