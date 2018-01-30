#!/usr/bin/env bash

cp ../recommendation/build/generated/source/proto/main/grpc/at/mkaran/thesis/recommendation/RecommendationGrpc.java src/main/java/distributed/recommendation/
cp ../recommendation/build/generated/source/proto/main/java/at/mkaran/thesis/recommendation/RecommendationProto.java src/main/java/distributed/recommendation/
#cp ../recommendation/src/main/java/at/mkaran/thesis/recommendation/RecommendationClient.java src/main/java/distributed/recommendation/

sed -i -e 's/package at.mkaran.thesis.recommendation;/package distributed.recommendation;/g' src/main/java/distributed/recommendation/RecommendationGrpc.java
sed -i -e 's/return at.mkaran.thesis.recommendation.RecommendationProto.getDescriptor();/return RecommendationProto.getDescriptor();/g' src/main/java/distributed/recommendation/RecommendationGrpc.java
sed -i -e 's/package at.mkaran.thesis.recommendation;/package distributed.recommendation;/g' src/main/java/distributed/recommendation/RecommendationProto.java

rm src/main/java/distributed/recommendation/RecommendationGrpc.java-e
rm src/main/java/distributed/recommendation/RecommendationProto.java-e