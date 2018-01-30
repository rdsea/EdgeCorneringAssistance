# Data Quality Test
This sub-project simulates one single driver that drives along a predefined test-track (see sample-track directory).
Along this test-track there are a set of predefined curves where we know the measured value for the curve radius.
The simulation places the driver at the start position of the test-track and emits GPS locations at a configurable speed.
The client-behavior is completely simulated, i.e the recommendation service is contacted for new curves,
results are stored in a local cache and on each GPS update the local curve detection searches for the nearest curve.
On the WebUI (localhost:8080), the simulation is visualized:
- The current location of the driver
- The set of measured curves (red circles)
- The actual result of the detection (green curves)

## Build the application
```./gradlew clean build```

## Install the application
To run the application locally

1.) Setup a RabbitMQ Server running on localhost
```
docker pull rabbitmq
docker run -d --hostname my-rabbit --name some-rabbit -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

Check if RabbitMQ is running by browsing to:
```
http://localhost:15672/#/
```

2.) Setup a MongoDB database on localhost
```
docker run --name some-mongo -d mongo
```

3.) Manually download full-screen Leaflet package via bower (its not available in the repo)
```
bower install Leaflet/Leaflet.fullscreen
```

4. Start the WebServer
```
./gradlew bootRun
```

5. Start the Simulation
```
Visit: localhost:8080 in a browser
```

## Run the application
In a terminal run:
```
./runLocally.sh
```

## Test Track
The `track` folder contains a test track as .csv file.
Per default `thesis-test-track.csv` is used for the simulation.
To use your own arbitrary track, you can specify waypoints.
Between these waypoints, a script calculates a route and creates GPS points along the waypoints.

1) Copy lon,lat coordinates of at least 2 waypoints (you can use [OpenStreetMaps](https://www.openstreetmap.org) for instance)

2) Execute the ./createGPS Track using the coordinates of your waypoints. For example:
```
./createGPSTrack.sh "48.269880,16.194496;48.2627089,16.1870748;48.259453,16.220693;48.249388,16.240871"
```
Note: The script makes use of [Project OSRM](http://project-osrm.org/docs/v5.10.0/api/#route-service) to calculate a route and
      [OverpassAPI](https://wiki.openstreetmap.org/wiki/Overpass_API) to receive GPS coordinates. This script overrides the existing `thesis-test-track.csv`. 
To avoid this, change the name of the output .csv file at the very last line of the R-Script `gps_extraction.R`. 
Then change the variable `CSV_TRACK_FILE_PATH` in `car.GPS.java` accordingly and restart the application.





