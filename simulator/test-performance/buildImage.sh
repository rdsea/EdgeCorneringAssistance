#!/usr/bin/env bash

./gradlew clean build
docker build -t sim-performance .
docker tag sim-performance thesis:sim-performance