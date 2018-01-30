package car;

import distributed.detection.OverpassCoverage;
import model.TripEntry;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import test.PerformanceTestExecutor;
import util.Haversine;
import util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * Simulates the car.GPS of a car
 */
public class GPS {

    private ArrayList<TripEntry> tripEntries = new ArrayList<>();
    private String driverId;
    private boolean shutdown = false;

    public GPS(String driverId) {
        this.driverId = driverId;
        readTripFromFile(this.driverId + ".csv");
    }

    /**
     * Start reading car.GPS Locations as they were recorded.
     * In case a car moves outside the covered area of Overpass the trip is finished.
     * @param listener A listener to receive the Location Updates
     */
    public void startGPSSimulation(GPSListener listener) {
        if (tripEntries != null) {
            TripEntry previousEntry = null;
            for (TripEntry tripEntry : tripEntries) {
                if (!shutdown) {
                    long timeDelay = getTimeDelay(previousEntry, tripEntry);
                    try {
                        Thread.sleep(timeDelay);
                    } catch (InterruptedException e) {
                        Log.error("car.GPS simulator failed: " + e.toString());
                    }
                    if (previousEntry != null) {
                        double metersTravelled = Haversine.distance(previousEntry.getLat(), previousEntry.getLon(), tripEntry.getLat(), tripEntry.getLon());
                        PerformanceTestExecutor.METERS_TRAVELLED.inc(metersTravelled);
                    }
                    previousEntry = tripEntry;
                    listener.onLocationUpdate(tripEntry.getLat(), tripEntry.getLon());

                } else {
                    break;
                }

            }
            listener.onTripFinished();
        }
    }

    /**
     * Reads a track containing timestamp, lat/lon
     * @param filename The full filename
     */
    private void readTripFromFile(String filename) {
        File csvFile = new File("trips/" + filename);
        try {
            CSVParser csvParser = CSVParser.parse(csvFile, Charset.defaultCharset(), CSVFormat.DEFAULT.withHeader("ts", "lat", "lon"));
            for (CSVRecord csvRecord : csvParser) {
                if (csvRecord.getRecordNumber() == 1)
                    continue; // skip Header
                long ts = Long.parseLong(csvRecord.get("ts"));
                double lat = Double.parseDouble(csvRecord.get("lat"));
                double lon = Double.parseDouble(csvRecord.get("lon"));
                TripEntry trackEntry = new TripEntry(ts, lat, lon);
                tripEntries.add(trackEntry);
            }
        } catch (IOException e) {
            Log.error("Reading trip from file failed" + e.toString());
        }
    }


    /**
     * Calculate time delay between two subsequent locations of the same track/driver
     * @param prev
     * @param curr
     * @return
     */
    private long getTimeDelay(TripEntry prev, TripEntry curr) {
        if (prev == null || curr == null) {
            return 0;
        }
        return curr.getTimestamp() - prev.getTimestamp();
    }

    public void shutdown() {
        this.shutdown = true;
    }




}
