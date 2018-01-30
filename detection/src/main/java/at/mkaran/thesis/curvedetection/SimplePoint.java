package at.mkaran.thesis.curvedetection;

/**
 * Created by matthias on 16.11.17.
 */
public class SimplePoint {
    private double latitude;
    private double longitude;

    public SimplePoint() {
    }

    public SimplePoint(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
