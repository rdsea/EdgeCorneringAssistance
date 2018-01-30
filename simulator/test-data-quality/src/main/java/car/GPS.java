package car;

import model.Location;
import model.TripEntry;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import test.DataQualityTest;
import util.Log;
import webserver.RabbitMQPublisher;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static java.lang.Math.*;
import static java.lang.Math.PI;
import static java.lang.Math.toDegrees;

/**
 * Simulates the car.GPS of a car
 */
public class GPS {

    private static final String CSV_TRACK_FILE_PATH = "tracks/thesis-test-track.csv";
    private final GPSListener listener;
    private final GPSConfig gpsConfig;
    private ArrayList<TripEntry> testTrackEntries = new ArrayList<>();
    private boolean shutdown = false;
    private double constantDriverSpeedMs; // desired constant speed in ms/s the test driver shall have
    private int lastIndexWhenPaused;
    private boolean pause = false;
    private int currentTupleIndex = 0;
    private int[] outageTupleIndexArray;
    private Long lastOutageStartedTime = null;

    public GPS(int constantDriverSpeedKmh, GPSListener listener, GPSConfig gpsConfig) {
        this.constantDriverSpeedMs = constantDriverSpeedKmh / 3.6;
        this.listener = listener;
        this.gpsConfig = gpsConfig;
        readTestTrack();
        if (gpsConfig.isAddOutagesEnabled()) {
            this.outageTupleIndexArray = prepareOutageTuplesIndices();
            Log.info("Outage indices: " + Arrays.toString(this.outageTupleIndexArray));
        }
    }

    public int getTrackSize() {
        return this.testTrackEntries.size();
    }

    /**
     * Start reading car.GPS Locations as they were recorded.
     * In case a car moves outside the covered area of Overpass the trip is finished.
     * @param atIndex Start the trip simulation from the specified index (should be 0 to start)
     */
    public void startGPSSimulation(int atIndex) {
        if (testTrackEntries != null) {
            for (int i=atIndex; i < testTrackEntries.size(); i++) {
                currentTupleIndex++;
                Log.info("Current tuple index: " + currentTupleIndex);
                if (shutdown) {
                    listener.onTripFinished();
                    break;
                } else if (pause) {
                    this.lastIndexWhenPaused = i;
                    listener.onTripPaused();
                    break;
                } else {
                    try {
                        Thread.sleep(getTimeDelayInMs(testTrackEntries.get(i).getDist()));
                    } catch (InterruptedException e) {
                        Log.error("GPS emiting failed due to: " + e.toString());
                    }
                    if (gpsConfig.isAddOutagesEnabled()) {
                        // simulate outages (i.e. skip entries for certain time period)
                        if (skipCurrentTupleDueToOutage()) {
                            Log.info("Current Tuple skipped due to outage ");
                            continue;
                        }

                        if (startNewOutage(testTrackEntries.get(i))) {
                            this.lastOutageStartedTime = System.currentTimeMillis();
                            continue;
                        }

                    }
                    emitEntry(testTrackEntries.get(i));
                }


            }
        }
    }

    /**
     * Returns an array of tuple indices at which an outage shall happen.
     * For instance: [4,19,109,...] means outages shall happen at Tuple 4, then again at Tuple 19 and so on...
     * @return
     */
    private int[] prepareOutageTuplesIndices() {
        int[] indexArray = new int[gpsConfig.getNumOutages()];
        int totalNumTuples = getTrackSize();
        int outageArea = (int) Math.floor(totalNumTuples / gpsConfig.getNumOutages());
        for (int i=0; i< gpsConfig.getNumOutages(); i++) {
            int minTupleIndexForArea = i * outageArea + 1;
            int maxTupleIndexForArea = (i+1) * outageArea;
            int randomIndexForArea = ThreadLocalRandom.current().nextInt(minTupleIndexForArea, maxTupleIndexForArea + 1);
            indexArray[i] = randomIndexForArea;
        }
        return indexArray;
    }

    private boolean startNewOutage(TripEntry tripEntry) {
        boolean indexArrayContains = IntStream.of(outageTupleIndexArray).anyMatch(x -> x == currentTupleIndex);
        Log.info("IndexArr: " + Arrays.toString(outageTupleIndexArray) + " contains " + currentTupleIndex + " " + indexArrayContains);
        if (indexArrayContains) {
            Log.info("New Outage started for " + gpsConfig.getOutageDurationSeconds()+  " seconds");
            RabbitMQPublisher.sendOutagePosition(tripEntry.getLat(), tripEntry.getLon());
            return true;
        } else {
            return false;
        }
    }

    private boolean skipCurrentTupleDueToOutage() {
        if (lastOutageStartedTime == null) {
            return false;
        }
        int currentOutageDuration =  Math.round((System.currentTimeMillis() - lastOutageStartedTime) / 1000);
        Log.info("Current Outage duration is: " + currentOutageDuration);
        return currentOutageDuration < gpsConfig.getOutageDurationSeconds();
    }

    private void emitEntry(TripEntry entry) {
        if (gpsConfig.isAddInaccuraciesEnabled()) {
            Location errorGPS = addGPSError(entry.getLat(), entry.getLon(), gpsConfig.getMinErrorMeters(), gpsConfig.getMaxErrorMeters());
            listener.onLocationUpdate(errorGPS.getLatitude(), errorGPS.getLongitude());
        } else {
            listener.onLocationUpdate(entry.getLat(), entry.getLon());
        }
    }

    public void pauseGPSSimulation() {
        this.pause = true;
    }

    public void resumeGPSSimulation() {
        this.pause = false;
        listener.onTripResumed();
        startGPSSimulation(lastIndexWhenPaused);

    }

    private long getTimeDelayInMs(double distanceInMeters) {
        double timeDelaySeconds = distanceInMeters / constantDriverSpeedMs;
        return (long) (timeDelaySeconds * 1000);
    }

    /**
     * Reads in the test-track containing lat,lon and dist (in meters) indicating the distance to the previous entry
     */
    private void readTestTrack() {
        File csvFile = new File(CSV_TRACK_FILE_PATH);
        try {
            CSVParser csvParser = CSVParser.parse(csvFile, Charset.defaultCharset(), CSVFormat.DEFAULT.withHeader("lat","lon","dist"));
            for (CSVRecord csvRecord : csvParser) {
                if (csvRecord.getRecordNumber() == 1)
                    continue; // skip Header
                double lat = Double.parseDouble(csvRecord.get("lat"));
                double lon = Double.parseDouble(csvRecord.get("lon"));
                double dist = Double.parseDouble(csvRecord.get("dist"));
                TripEntry entry = new TripEntry(lat, lon, dist);
                testTrackEntries.add(entry);
            }
        } catch (IOException e) {
            Log.error("Reading test-track from file failed" + e.toString());
        }
    }

    /**
     * Moves a point by distance and bearing
     * @param latitude          Latitude of starting point
     * @param longitude         Longitude of starting point
     * @param distanceInMetres  Distance that you want to move the point by
     * @param bearing           an angle, direction towards which you want to move the point. 0 is towards the North, 90 - East, 180 - South, 270 - West. And all between, i.e. 45 is North East.
     * @return  Location Object containing longitude, latitude of destination point
     */
    private Location movePoint(double latitude, double longitude, double distanceInMetres, double bearing) {
        double brngRad = toRadians(bearing);
        double latRad = toRadians(latitude);
        double lonRad = toRadians(longitude);
        int earthRadiusInMetres = 6371000;
        double distFrac = distanceInMetres / earthRadiusInMetres;

        double latitudeResult = asin(sin(latRad) * cos(distFrac) + cos(latRad) * sin(distFrac) * cos(brngRad));
        double a = atan2(sin(brngRad) * sin(distFrac) * cos(latRad), cos(distFrac) - sin(latRad) * sin(latitudeResult));
        double longitudeResult = (lonRad + a + 3 * PI) % (2 * PI) - PI;

        return new Location(toDegrees(longitudeResult), toDegrees(latitudeResult));
    }

    /**
     * Adds a random error to a GPS coordinate that lies within the given bounds [minErrorMeters, maxErrorMeters]
     * @param latitude
     * @param longitude
     * @param minErrorMeters possible minimum error
     * @param maxErrorMeters possible maximum error
     * @return
     */
    private Location addGPSError(double latitude, double longitude, int minErrorMeters, int maxErrorMeters) {
        int randomError = ThreadLocalRandom.current().nextInt(minErrorMeters, maxErrorMeters + 1);
        double randomDirection = ThreadLocalRandom.current().nextInt(0, 360);
        DataQualityTest.GPS_ERROR_METERS.observe(randomError);
        return movePoint(latitude, longitude, randomError, randomDirection);
    }

    public void shutdown() {
        this.shutdown = true;
    }




}
