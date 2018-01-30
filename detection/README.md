# Curve Detection
This project is an Apache Apex Application that is able to detect curves around given locations (lat/lon pairs).

## Software Components

* Java
* Maven
* Hadoop/Yarn
* Apache Apex
* RabbitMQ

## Build the Apex Application (.apa)
```mvn clean package -DskipTests```

## Test the Application locally
To run the application locally

1.) Setup a RabbitMQ Server running on localhost
```
docker pull rabbitmq
docker run -d --hostname my-rabbit --name some-rabbit -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```
Or use an existing docker container
```
docker restart some-rabbit
```

Check if RabbitMQ is running by browsing to:
```
http://localhost:15672/#/
```


2. In the file `test/resources/test_properties.xml` make sure that the property `testMode` is set to `true`


3. Start a local MongoDB instance 
```
See: https://docs.mongodb.com/getting-started/shell/
```

4. Provide simulation data as input to RabbitMQ Server
```
See: https://gitlab.com/thesis.17/thesis_simulator
```

5. Run the application test file `ApplicationTest`. This executes the application for some seconds (Default: 50secs) locally.