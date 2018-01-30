# Experiments
In this configuration we present the exact setup of the experiments that were executed for the thesis (Section 7).

## Prerequisites

### Setup an OverpassAPI instance
Note: an own OverpassAPI instance is only needed for the performance test. The data-quality test can be executed on the main server.

1.) Use the [BBBike - ExtractTool](https://extract.bbbike.org/) to create and download Map of "Graz&Umgebung" using the following coordinates:
```
SouthWest lon:14.58, lat:46.642
NorthEast lon:16.204, lat:47.484
```

2.) Download the Extract in the  `PBF` format by providing your email address and following the link provided by BBBike

3.) Using [osmconvert](http://wiki.openstreetmap.org/wiki/DE:Osmconvert) convert the .pbf file to .osm using:
```
./osmconvert graz-and-umgebung.pbf >graz-and-umgebung.osm
```

4.) Zip the file using bz2:
```
bzip2 graz-and-umgebung.osm
```

5.) Create a VM in the Cloud (at any cloud provider). For the experiment the following specs on GCP were used:
`n1-standard-8 (8 vCPUs, 30 GB disk space)`

6.) Upload `graz-and-umgebung.osm.bz2` to the VM

7.) Install OverpassAPI by following the instructions at [OpenStreetMaps - OverpassAPI Setup](http://wiki.openstreetmap.org/wiki/Overpass_API/Installation#Ubuntu_or_Debian_6.0_.28squeeze.29_or_Debian_7.0_.28wheezy.29). For the planet file, use: `graz-and-umgebung.osm.bz2` 


### Setup a distributed Mongo database that can be accessed by all services
It is possible to use a DaaS (for instance: [AtlasDB](https://www.mongodb.com/cloud/atlas)) or an own instance.
For the experiments, an own instance was setup as following:

1.) Create a VM in the Cloud (at any cloud provider) named "mongodb"

2.) Configure the Firewall settings (depends on your provider) to allow to connect to database from both your local machine and the instances that will be configured in the following steps

3.) Install docker and docker-compose:
```
curl -fsSL get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
```
Log out and log back in
```
sudo curl -L https://github.com/docker/compose/releases/download/1.18.0/docker-compose-`uname -s`-`uname -m` -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

4.) Specify a password for the admin database in `docker-compose.yml`

5.) Start the mongo instance
```
docker-compose up -d
```

6.) Connect to the instance with a client of your choice (on MacOSX i can recommend: Robo3T)

7.) Create a new database called: `curveStorage`

8.) Add a new collection called: `curves`

9.) Create two users `det` and `rec`:
```
use curveStorage
db.createUser( { user: "rec",
                 pwd: "<PW>",
                 roles: [ { role: "readWrite", db: "curveStorage" }] },
               { w: "majority" , wtimeout: 5000 } 
              )
```
```
use curveStorage
db.createUser( { user: "det",
                 pwd: "<PW>",
                 roles: [ { role: "readWrite", db: "curveStorage" }] },
               { w: "majority" , wtimeout: 5000 } 
              )
```

10.) add indexes
```
db.getCollection('curves').createIndex({'centerPoint':"2dsphere" });
db.curves.createIndex({geohash:"text"}) ;
```
### Setup virtual machines for the services
Setup virtual machines in GCP, AWS, or wherever.
The setup was tested with running Ubuntu 16.04 on each VM. (other distributions should work fine though too).
Following VM's with hostnames are assumed to work with the docker-compose files:

* rec
* det

1.) Install docker on all the VM's as described before

2.) At a VM of your choice init the swarm manager and note the resulting `join-token` command:
```
docker swarm init
```

3.) Create an overlay swarm network called `swarmnet` with:
```
docker network create -d overlay swarmnet
```

4.) On all other VM's, join the swarm node using the noted command

5.) Make sure you have built the docker-images for the rec-server and the performance-test-app:
```
./recommendation/buildImage.sh
./simulator/test-performance/buildImage.sh
```

6.) Clone the repository containing all docker configs to the VM of the swarm-manager
```
git clone git@gitlab.com:thesis.17/prototype.git
```

7.) On the swarm-manager VM: Configure all properties of the rec-service in the file `rec/config.properties`:
```
NODE_ID= # obsolete
LOAD_BALANCE_REC=false
LOAD_BALANCE_DET=false
DEFAULT_DETECTION_SERVER_ADDRESS=rabbit
SIMULATE_WEATHER_API_CALLS=false
SEND_DETECTION_REQUESTS=true
FIND_CURVES_BY_MODE=geohash
FIND_CURVES_BY_VALUE=6
WEATHER_GEOHASH_PRECISION=4
USE_ATLAS_DB=false
DISTRIBUTED_MONGO_URI=db
DISTRIBUTED_MONGO_USER=rec
DISTRIBUTED_MONGO_PW=<PW>
OWM_API_KEY=<key>
CPU_CHECK_INTERVAL_SECONDS=10 # obsolete
```

9.) On the swarm-manager VM:  start all services using:
```
docker stack deploy -c det/docker-compose.yml experiment
docker stack deploy -c rec/docker-compose.yml experiment # in case you're using the images from a private repo add "--with-registry-auth"
```

10.) Make sure all services started using:
```
docker service ls
```
or 
```
docker stack ps experiment
```

### Start and Configure the Apex Application
1.) Login to the dtGateway at: `det:9090` (det = IP of the VM)

2.) Follow all the configuration steps (simply click next) and upload a valid Apex licence

3.) Upload the Apex Detection Application which can be found in detection/target/apexapp-1.0-SNAPSHOT.apa
(If you don't have that file yet, you need to build the application as described in the README of the detection source code)

4.) Configure the Apex Application to your likings (OverpassServer, Partitions, Memory, Mongo Credentials, etc.).
File: `detection/src/main/resources/META-INF/properties.xml`

The following have been changed for the experiment setup:
```
dt.application.CurveDetection.operator.*.attr.MEMORY_MB	                    512
dt.application.CurveDetection.operator.*.port.*.attr.BUFFER_MEMORY_MB	      128
dt.application.CurveDetection.operator.mongoOperator.prop.mongoPW	          <PW>
dt.application.CurveDetection.operator.mongoOperator.prop.mongoURI	        <URI>
dt.application.CurveDetection.operator.mongoOperator.prop.mongoUser	        det
dt.application.CurveDetection.operator.mongoOperator.prop.useAtlasDb	      false
dt.application.CurveDetection.operator.osmOperator.attr.PARTITIONER	        com.datatorrent.common.partitioner.StatelessPartitioner:5
dt.application.CurveDetection.operator.osmOperator.prop.overpassUrl	        http://<IP-OF-OVERPASS-INSTANCE>
rabbitMQServerAddress                                                       rabbit
```

5.) Launch the Apex App


### Setup a Monitor to see results
To monitor the results live, prometheus and grafana need to be installed.

In the directory `monitoring/` docker can be used to run both.

To scrape all `fleets` of cars (See next section), add targets to `prometheus/prometheus.yml`. If a fleet runs on your localhost for instance add:
```
    - targets: ['localhost:8080','<IP-OF-FLEET-VM>:8080',...]
```

Open `localhost:3000` in a browser, add prometheus (running on localhost:9090) as data-source and import the dashboards from the `dashboards` directory.


### Launch experiments
To launch an experiment, make sure you've followed all previous steps such that all services can communicate to each other.

#### Performance

1.) Launch one or more car-fleets (you can do this on any virtual machine of course):
```
docker run -d -p 8080:8080 --name fleet-1 thesis:sim-performance 
```

2.) In a browser open: `<IP-OF-FLEET>:8080/`

3.) Enter an arbitrary number of cars and make sure the IP-address of the rec-server is set to: `grpc-server-1`.
The other settings can optionally be changed.

4.) Go to your monitor, open the previously imported `Performance Dashboard` and see the results live.

#### Data-Quality

1.) Launch a rabbitMQ server on your localhost:
```
docker pull rabbitmq
docker run -d --hostname my-rabbit --name some-rabbit -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

2.) Launch a mongodb instance on your localhost (this is needed for performing the local detection):
```
docker run -p 27017:27017 --name some-mongo -d mongo
```

3.) Launch one car on your localhost:

In case Java is available:
```
./gradlew bootRun
```

Otherwise
```
docker run -d -p 8080:8080 --name car hiasel/thesis:sim-data-quality
```
(Note: make sure mongo and rabbit are accessible from within the docker container)

4.) In a browser open: `localhost:8080/`

5.) Change settings to your likings. If you enabled `Call server` make sure the IP of the recommendation server is correct.

6.) Hit `Start`

7.) You can see the map with live results of the detection.

8.) Optionally: Go to your monitor, open the previously imported `Data Quality Dashboard` and see the more statistics live.

## Troubleshooting

### OverpassAPI
* Setting up Apache API: make sure full paths are specified
* Make sure you have r-w rights on all the path dirs
* Make sure the correct .conf file is active in /etc/apache2/sites-available
    - i always configured „default“
    - but there was another file active called something like „000-default.conf“
    - overwriting the configs of my file to the „000-default.conf“ file fixed it
* If starting the dispatcher gives an error (i.e „…already in use“)
    - Remove the „osm-XXX“ file in DB_DIR
    - Remove the „osm-XXX“ file in /dev/hsm (or something like this, see troubleshooting page)

* (Re-)Starting the dispatcher in pre-configured machine:
    - $EXEC_DIR/bin/dispatcher --osm-base --db-dir=$DB_DIR
    - in case it fails remove these files with
		- rm -rf overpass/db/osm3s_v0.7.54_osm_base
		- rm -rf /dev/shm/osm3s_v0.7.54_osm_base
		- call dispatcher again

* Start without opening a ssh-terminal connection to the machine:
nohup overpass/exec/bin/dispatcher --osm-base --db-dir=overpass/db &

### Apex
Depending on the chosen resources for the Apex Application (running on hadoop-yarn cluster), the memory of the yarn resource manager needs to be adjusted. 

Open a terminal inside the container where hadoop-yarn executes:
```
docker exec -it <CONTAINER-ID> sh
sudo vi /etc/hadoop/conf/yarn-site.xml
```
Add following properties:
```
<property> 
  <name>yarn.nodemanager.resource.memory-mb</name> 
  <value>12000</value> 
</property>
<property> 
  <name>yarn.nodemanager.vmem-pmem-ratio</name>
  <value>10</value> 
</property>
<property> 
  <name>yarn.scheduler.minimum-allocation-mb</name>
  <value>128</value> 
</property>
<property> 
  <name>yarn.scheduler.maximum-allocation-mb</name>
  <value>12000</value> 
</property>
```
Restart hadoop-yarn
```
sudo /etc/init.d/hadoop-yarn-resourcemanager restart
```

See my post in [google-apex-group](https://groups.google.com/forum/?utm_medium=email&utm_source=footer#!msg/dt-users/FL6vUT-SpRg/yzi9EJd4BAAJ).



### gRPC-Server
Sometimes it happens that the grpc-server fails to start because it fails to connect to local mongo or redis (both in the interal network).
If this happens, simply stop the grpc-server with:

```
docker service rm <id-of-grpc-server>
```
and restart it with:
```
docker stack deploy -c rec/docker-compose.yml experiment # in case you're using the images from a private repo add "--withAuthCredentials"
```