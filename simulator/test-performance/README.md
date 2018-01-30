# Performance Test
This sub-project simulates many drivers following real recorded tracks.
Using this application it is possible to start an abritrary number of cars. Each instance of this application is called a `fleet`.
Many fleets consisting of many drivers can be started using docker.
The goal is to find out the maximum number of drivers that can be handled by the system when load balancing is disabled.

## Test Tracks
The.zip file "trip.zip" contains all tracks that have been used for the thesis.

## Build docker image
```
unzip trips.zip
./buildIamge.sh
```

## Launch docker image

1.) Launch one or more car-fleets (you can do this on any virtual machine of course):
```
docker run -d -p 8080:8080 --name fleet-1 thesis:sim-performance
```

2.) In a browser open: `<IP-OF-FLEET>:8080/`

3.) Enter an arbitrary number of cars and make sure the IP-address of the rec-server is set to: `grpc-server-1`.
The other settings can optionally be changed.