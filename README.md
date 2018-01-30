# Cornering Assistance Application
This project contains the source-code of the prototype that was implemented for the master's thesis "Using Mobile Edge Computing Technologies for Real-Time Cornering Assistance" at the TU Wien.

## Introduction
To contribute to safer driving in any type of car, in the thesis "Using Mobile Edge Computing Technologies for Real-Time Cornering Assistance" we introduce a novel system that assists drivers in real-time while cornering. 
The system is designed in a way that it can be deployed to both the cloud and/or the emerging edge-computing infrastructure.

Goals:
* warn drivers ahead of curves in real-time
* recommend safe speeds to enter a curve
* use novel edge- and cloud-computing architectures and algorithms

See the [poster](https://github.com/rdsea/EdgeCorneringAssistance/blob/master/thesis-poster.pdf) for more information about the thesis.

## Demo Videos

The following videos demonstrate the prototype application:

[Simulation-Demo](https://youtu.be/VNNTYC3l3pU)

[Real-World-Demo](https://youtu.be/SBRD8GrFyZoO)

## Run the Demo
See [Demo - README](https://github.com/rdsea/EdgeCorneringAssistance/blob/master/docker/demo/README.md) to run the demo yourself.

## Prototype Structure
The software components of the thesis' prototype are split up into the following directories:

*android*: A native android prototype that shows upcoming curves and recommends a safe speed live on the road.

*commons*: A Java library that contains common code used in the services (recommendation and detection).

*detection*: Source code for the detection application written in Apache Apex

*docker*: Docker configurations for deploying the prototype, run experiments and a simple demo (described below).

*recommendation*: Source code for the recommendation service.

*simulator*: Java applications that simulate client/car functionalities.






