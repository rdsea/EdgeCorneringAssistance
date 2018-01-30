#!/usr/bin/env bash
./gradlew clean build -x test
./gradlew installDist
docker build -t rec-server .
docker tag rec-server thesis/rec-server