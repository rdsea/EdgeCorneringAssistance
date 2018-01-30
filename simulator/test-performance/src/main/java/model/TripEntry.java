package model;

/**
 * Created by matthias on 09.11.17.
 */
public class TripEntry {
    private long timestamp;
    private double lat;
    private double lon;

    public TripEntry(long timestamp, double lat, double lon) {
        this.timestamp = timestamp;
        this.lat = lat;
        this.lon = lon;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }
}
