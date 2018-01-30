# Recommendation Service
This service calculates a recommended "safe" speed for curves.
The input for the service are lat/lon-pairs.
Based on current weather conditions, the recommended speed for each curve is calculated.
Curves itself are calculated by the detection-service

## Software Components
* Java
* Gradle
* gRPC
* Redis
* MongoDB

Additionally the service uses the "OWM-JAPIs" library for fetching weather data from OWM.
Unfortunately the currently latest working version (2.5.0.5) is not yet on MavenCentral.
Therefore you need to download this library manually from https://bitbucket.org/akapribot/owm-japis/downloads/ .
After downloading, extract the .zip directory and copy "libs/owm-japis-2.5.0.5.jar" to "recommendation/libs".

## Build and Run
```
./gradlew clean build -x test
./gradlew installDist
./gradlew runServer
```

## Build docker image
```
./buildImage.sh
```
