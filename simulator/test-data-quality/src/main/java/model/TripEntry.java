package model;

/**
 * Created by matthias on 09.11.17.
 */
public class TripEntry {
    private double lat;
    private double lon;
    private double dist;

    public TripEntry(double lat, double lon, double dist) {
        this.lat = lat;
        this.lon = lon;
        this.dist = dist;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public double getDist() {
        return dist;
    }
}
