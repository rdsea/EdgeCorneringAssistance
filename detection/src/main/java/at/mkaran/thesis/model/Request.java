package at.mkaran.thesis.model;

/**
 * Created by matthias on 10.08.17.
 */
public class Request {
    private Double lat;
    private Double lon;
    private boolean valid = false;

    public Request() {
    }

    public Request(Double lat, Double lon) {
        this.lat = lat;
        this.lon = lon;
        if (lat != null && lon != null) {
            this.valid = true;
        }
    }

    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public String toString() {
        String string = "lat: " + lat;
        string += ",lon: " + lon;
        return string;
    }
}
