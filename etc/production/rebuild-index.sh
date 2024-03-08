#!/bin/bash

#check /etc/security/limits.conf for maximum file handles
java  -server -Xmx16500m -jar jerusalem2-1.0-SNAPSHOT-jar-with-dependencies.jar rebuildIndex temp



