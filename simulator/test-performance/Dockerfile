FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD build/libs/simulator-test-performance-1.0-SNAPSHOT.jar app.jar
ADD trips/ trips/
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]