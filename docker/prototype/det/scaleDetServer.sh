#!/usr/bin/env bash
sed 's/X/'"$1"'/g' docker-compose-replace.yml > docker-compose.yml
sed -i -e 's/!LAT!/'"$2"'/g' docker-compose.yml
sed -i -e 's/!LON!/'"$3"'/g' docker-compose.yml