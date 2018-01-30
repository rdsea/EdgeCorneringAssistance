package at.mkaran.thesis.osm;

import org.json.simple.JSONObject;

import java.util.Locale;

/**
 * Created by matthias on 30.05.17.
 */
public class Node extends Entity {
    private double latitude;
    private double longitude;

    public Node() {
        super();
    }

    public Node(long id) {
        super(id);
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

    @Override
    public void parseEntity(JSONObject json) {
        setLongitude(Double.parseDouble(json.get("lon").toString()));
        setLatitude(Double.parseDouble(json.get("lat").toString()));
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "[%f,%f]", getLatitude(), getLongitude());
    }
}
