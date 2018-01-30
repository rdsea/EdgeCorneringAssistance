# Commons - Module
This module serves as library for common implementations across the services of the thesis.
* Common gRPC service contracts
* Helper class to connect to a MongoDB instance (local or remote)
* LoadBalancer to handle service requests on nodes 
* Helper class to send requests to RabbitMQ

## Build

Build the library
```
./gradlew clean fatJar
```

## Install Client Code for Maven Projects

To install the library to the local maven repository maven clients:
```
mvn install:install-file -Dfile=build/libs/commons-all-<VERSION>.jar -DgroupId=at.mkaran.thesis -DartifactId=commons -Dversion=<VERSION> -Dpackaging=jar
```

Then include the dependency in pom.xml:
```
    <dependency>
      <groupId>at.mkaran.thesis</groupId>
      <artifactId>commons</artifactId>
      <version><VERSION></version>
    </dependency>
```

## Install Client Code for Gradle Projects
```
    compile files('libs/commons-<VERSION>-SNAPSHOT.jar')
```

