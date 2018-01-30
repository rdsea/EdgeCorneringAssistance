#!/usr/bin/env bash

# This scripts creates a GPS Track along the path of a given list of coordinates
# Specify coordinates as semicolon seperated list in the form: lon,lat;lon,lat;...
# example: ./createGPSTrack.sh "14.343321,48.154559;14.347054,48.152348;14.350477,48.149406;14.351410,48.146549;14.356818,48.150286"

baseurl="http://router.project-osrm.org/route/v1/driving/"
params="?alternatives=false&annotations=nodes"
wget $baseurl$1$params -O osrm-response.json
Rscript gps_extraction.R