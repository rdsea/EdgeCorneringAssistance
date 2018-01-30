# Android Prototype
This android application implements a very basic prototype for a native client of the Cornering
Assistance Application.


## Recommendation Server
The application connects to a single gRPC recommendation server using a static IP.
Set the variable `STATIC_REC_SERVER_ADDRESS` ub the class `GrpcTask.java` to the address of the server.
In production consider to use a service registry instead.

## Google Maps
The application uses Google Maps API.
You need a valid Maps Key (See: https://developers.google.com/maps/documentation/android-api/signup)
and paste in the `AndroidManifest.xml` as described.

## Build and install the application to a device
```
./gradlew installDebug
```

## Simulate on Emulator using GPX files
It is possible to test the application using the emulator.
In the `tracks` directory is a GPX file that represents the test track from the thesis.
To replay the track use the GPX tool of the Emulator:
[Android Emulator Docs](https://developer.android.com/studio/run/emulator.html#runningapp)
