# Prototype
In this configuration we demonstrate a prototype of the system as described in Section 6.3.3 of the thesis.

## Prerequisites

### Setup an OverpassAPI instance
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

8.) 
To start the Overpass API Dispatcher run the following:
```
rm -rf overpass/db/osm3s_v0.7.54_osm_base # only needed if dispatcher fails
rm -rf /dev/shm/osm3s_v0.7.54_osm_base # only needed if dispatcher fails
nohup overpass/exec/bin/dispatcher --osm-base --db-dir=overpass/db &
```

Note: the commands above assume that overpass was installed in `overpass/` and the db-dir is located at `overpass/db`.

9.)
Test the Installation by running the following query:
```
wget --output-document=test.xml http://<IP-OF-VM>/api/interpreter?data=%3Cprint%20mode=%22body%22/%3E
```

The output should like something like this:
```
<?xml version="1.0" encoding="UTF-8"?>
<osm version="0.6" generator="Overpass API 0.7.54.12 054bb0bb">
<note>The data included in this document is from www.openstreetmap.org. The data is made available under ODbL.</note>
<meta osm_base=""/></osm>
```
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
Setup virtual machines in GCP, AWS or wherever.
You can create as many instances as you wish.
The setup was tested with running Ubuntu 16.04 on each VM. (other distributions should work fine though too).
Following VM's with hostnames are assumed to work with the provideddocker-compose files:

* consul
* rec-1
* rec-2
* det-3
* external (this single VM is used to create car-fleets, monitor services, etc.)

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

5.) Make sure you have built the docker-images: `thesis/rec-server` and  `thesis/performance-app`
```
./recommendation/buildImage.sh
./simulator/test-performance/buildImage.sh
```

6.) Clone the repository containing all docker configs to the VM of the swarm-manager
```
git clone git@gitlab.com:thesis.17/prototype.git
```

#### Start a consul-server

```
cd docker/prototype/consul
docker stack deploy -c docker-compose.yml prototype
```

Note: In production, consul recommends:
```
"We recommend 3 or 5 total servers per datacenter. 
A single server deployment is highly discouraged as data loss is inevitable in a failure scenario. Please refer to https://www.consul.io/docs/internals/consensus.html#deployment-table for more detail."
```

#### Start the recommendation services

1.) 
```
cd docker/prototype/rec
```

2.) Configure all properties of the rec-service in the file `rec/config-replace.properties`. For instance like this:
```
NODE_ID=!X!
LOAD_BALANCE_REC=true
LOAD_BALANCE_DET=false
DEFAULT_DETECTION_SERVER_ADDRESS=rabbit-3
SIMULATE_WEATHER_API_CALLS=true
SEND_DETECTION_REQUESTS=true
FIND_CURVES_BY_MODE=geohash
FIND_CURVES_BY_VALUE=6
WEATHER_GEOHASH_PRECISION=4
USE_ATLAS_DB=false
DISTRIBUTED_MONGO_URI=<IP-OF-mongodb-VM>
DISTRIBUTED_MONGO_USER=rec
DISTRIBUTED_MONGO_PW=<PW>
OWM_API_KEY=<key>
CPU_CHECK_INTERVAL_SECONDS=10
ENABLE_AUTH=true
```

In this case, load balancing is enabled for the availabe recommendation services (rec-1, rec-2, rec-3).
Since the detection service needs many resources, we only use 1 single detection node (det-1).
Therefore load balancing for detection services is disabled per default.

3.) Configure and start the recommendation services:

First specify a unique ID and the location of the node where the service executes on by using the `./scaleRecServer.sh` script:
```
./scaleRecServer.sh <uniqueID> <lat> <lon>
```
This generates appropriate `docker-compose.yml` files.

Next start the service using:
```
docker stack deploy -c docker-compose.yml prototype # in case you're using the images from a private repo add "--with-registry-auth"
```

4.) Start each rec-service like this:

```
./scaleRecServer.sh 1 50.489824 3.7378804
docker stack deploy -c docker-compose.yml prototype --with-registry-auth
./scaleRecServer.sh 2 51.5285582 -0.241681
docker stack deploy -c docker-compose.yml prototype --with-registry-auth
```
Note: The locations of the rec-services in this example refer to the europe-west-1 and europe-west-2 zones in GCP.

#### Start a detection service

1.) Build the detection application (on your machine locally)
```
cd detection
mvn clean package -DskipTests
```

2.) On the swarm manager VM:
```
cd docker/prototype/det
```

3.) Similar as described for the rec-services, configure and start the det service(s):
```
./scaleDetServer.sh 3 50.489824 3.7378804
docker stack deploy -c docker-compose.yml prototype
```

4.) Login to the dtGateway at: `<IP-OF-DET-VM>:9091`

5.) Follow all the configuration steps (simply click next) and upload a valid Apex licence

6.) Upload the Apex Detection Application which you previously built locally. It should be located in `detection/target/apexapp-1.0-SNAPSHOT.apa`

7.) Configure the Apex Application to your likings (OverpassServer, Partitions, Memory, Mongo Credentials, etc.).
File: `detection/src/main/resources/META-INF/properties.xml`

The following have been changed for the sample setup:
```
dt.application.CurveDetection.operator.*.attr.MEMORY_MB	                    512
dt.application.CurveDetection.operator.*.port.*.attr.BUFFER_MEMORY_MB	    128
dt.application.CurveDetection.operator.mongoOperator.prop.mongoPW	        <PW>
dt.application.CurveDetection.operator.mongoOperator.prop.mongoURI	        <URI>
dt.application.CurveDetection.operator.mongoOperator.prop.mongoUser	        det
dt.application.CurveDetection.operator.mongoOperator.prop.useAtlasDb	    false
dt.application.CurveDetection.operator.osmOperator.attr.PARTITIONER	        com.datatorrent.common.partitioner.StatelessPartitioner:5
dt.application.CurveDetection.operator.osmOperator.prop.overpassUrl	        http://<IP-OF-OVERPASS-INSTANCE>
rabbitMQServerAddress                                                       rabbit-3
```

8.) Launch the Apex App 

#### Start a car-fleet

1.) 
```
cd docker/prototype/car
```

2.)
```
docker stack deploy -c docker-compose.yml prototype --with-registry-auth
```

3.)
In a browser open:
```
http://<IP-OF-EXTERNAL-VM>:8080/#/
```

4.) Specify the settings to your likings. 
To enable load balancing, make sure "Load Balancing" is checked.

5.) Hit Start

#### Monitor the Application

1.) 
```
cd docker/prototype/monitor
```

2.) Configure all targets to scrape metrics from in the file `prometheus/prometheus.yml`:
```
...
- targets: ['fleet:8080','node-exporter-1:9100','node-exporter-2:9100']
...
```
Note: The IDs of the specified node-exporters must match the IDs that were configured for the rec-services

3.) Start prometheus and grafana:
```
docker stack deploy -c docker-compose.yml prototype
```

4.) In a browser open:
```
http://<IP-OF-EXTERNAL-VM>:3000/#/
```

5.) Configure Grafana:

Login
```
user: admin
pw: admin
```

Add prometheus as datasource (type: Prometheus) with HTTP Settings:
```
url: http://prometheus-monitor:9090
access: proxy
```

Import Dashboard from JSON using the file: `dashboards/prototype-dashboard.json`

On the right pane, make sure to specify a valid time (the current time).
You should now have a live monitoring dashboard.





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

* (Re-)Starting dispatcher in pre-configured machine:
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

See my post in [google-apex-group](https://groups.google.com/forum/?utm_medium=email&utm_source=footer#!msg/dt-users/FL6vUT-SpRg/yzi9EJd4BAAJ)



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