# Demo
This demo runs the services in a simple all-in-one deployment using docker.
The following features are NOT covered in this demo:

* Using a service-registry (Consul)
* Using a centralized DaaS for the distributed cache
* Load-balance client and service requests
* Monitoring of services (Prometheus)
* Authentication in the distributed cache
* Performance Testing

## Requirements
* A powerful machine. Since this setup includes running a Hadoop/YARN cluster and many other services, we recommend a machine
with resources similar to a Google [n1-standard-4](https://cloud.google.com/compute/docs/machine-types). The demo might
work on lower resources too (this was not tested though).

* An Apache Apex license. You can request a [free license](https://www.datatorrent.com/license-upgrade/)

* A valid OpenWeatherMaps API KEY. You can [sign-up](https://home.openweathermap.org/users/sign_up) for a free key. 

## Run the Demo

### Start the services
1.) 

```
cd recommendation
```
Add your OWM-API Key to the file `demo-config/config.properties`:
```
...
OWM_API_KEY=<YOUR-OWM-API-KEY>
...
```
Build the docker image:
```
./buildImage.sh
```

2.) 
Build the detection application (Apache Apex)
```
cd detection
mvn clean package -DskipTests
```

3.)
Create a docker network so that our services can communicate to each other
```
docker network create swarmnet
```

4.)
Start the services
```
cd docker/demo
docker-compose up
```

### Configure the Apache Apex Application

1.) Login to the dtGateway at: `<IP-OF-YOUR-MACHINE>:9091` (`localhost` if you run it locally)

2.) In the WEB-UI Follow all the configuration steps (simply click next) and upload your license

3.) Navigate to the `Develop` tab and then to  `Application Packages`. Upload the previously built detection application. (It should be located in `detection/target/apexapp-1.0-SNAPSHOT.apa`)

4.) Navigate to the `Develop` tab and then to  `Application Configurations`. Upload the config-file `docker/demo/apex-config.apc` file.

5) Hit `Launch`

### Run the Client
You have 2 options to run or simulate a client:

1.) Run a simulation that visualizes a driver and all results on a map

```
docker run -d --hostname my-rabbit --name some-rabbit -p 5672:5672 -p 15672:15672 rabbitmq:3-management
docker run --name some-mongo -d mongo
cd simulator/test-data-quality/
./gradlew bootRun
```

2.) Or install our Android prototype application to a device
```
cd android/CorneringAssistanceApplication
./gradlew installDebug
```
To configure the Android Prototype correctly please have a look at [Android App README](https://github.com/rdsea/EdgeCorneringAssistance/blob/master/android/CorneringAssistanceApplication/README.md).
The README also describes how you can use GPX files to simulate a driver using the Emulator.
