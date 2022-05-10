#!/bin/bash

#check /etc/security/limits.conf for maximum file handles
bzip2 -dc oberbayern-latest.osm.bz2 | java  -server -Xmx10500m -jar jerusalem2-1.0-SNAPSHOT.jar rebuildIndex temp


