#!/usr/bin/env bash

docker restart some-rabbit
docker restart some-mongo
./gradlew bootRun
