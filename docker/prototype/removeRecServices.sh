#!/usr/bin/env bash

# Removes all the docker services of rec-1 and rec-2
docker service rm prototype_grpc-server-1
docker service rm prototype_grpc-server-2
docker service rm prototype_redis-1
docker service rm prototype_redis-2
docker service rm prototype_prometheus-1
docker service rm prototype_prometheus-2
docker service rm prototype_node-exporter-1
docker service rm prototype_node-exporter-2
docker service rm prototype_consul-client-1
docker service rm prototype_consul-client-2