package at.mkaran.thesis.curvedetection;

import org.bson.Document;

import java.util.Arrays;

/**
 * Created by matthias on 16.11.17.
 */
public class SimpleCurve {
    private double radius;
    private double length;
    private String type;
    private SimplePoint centerPoint;
    private SimplePoint startPoint;
    private SimplePoint endPoint;
    private double startBearing;
    private double endBearing;
    private String boundingGeohash = null;

    public SimpleCurve() {
    }

    public SimpleCurve(double radius, double length, String type, SimplePoint centerPoint, SimplePoint startPoint, SimplePoint endPoint, double startBearing, double endBearing) {
        this.radius = radius;
        this.length = length;
        this.type = type;
        this.centerPoint = centerPoint;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.startBearing = startBearing;
        this.endBearing = endBearing;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public SimplePoint getCenterPoint() {
        return centerPoint;
    }

    public void setCenterPoint(SimplePoint centerPoint) {
        this.centerPoint = centerPoint;
    }

    public SimplePoint getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(SimplePoint startPoint) {
        this.startPoint = startPoint;
    }

    public SimplePoint getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(SimplePoint endPoint) {
        this.endPoint = endPoint;
    }

    public double getStartBearing() {
        return startBearing;
    }

    public void setStartBearing(double startBearing) {
        this.startBearing = startBearing;
    }

    public double getEndBearing() {
        return endBearing;
    }

    public void setEndBearing(double endBearing) {
        this.endBearing = endBearing;
    }

    public void setBoundingGeohash(String boundingGeohash) {
        this.boundingGeohash = boundingGeohash;
    }

    public Document toDoc() {
        Document doc = new Document(
                "radius", this.getRadius())
                .append("length", this.getLength())
                .append("type", this.getType())
                .append("centerPoint", new Document("type", "Point").append("coordinates", Arrays.asList(this.getCenterPoint().getLongitude(), this.getCenterPoint().getLatitude())))
                .append("startPoint", new Document("type", "Point").append("bearing", this.getStartBearing()).append("coordinates", Arrays.asList(this.getStartPoint().getLongitude(), this.getStartPoint().getLatitude())))
                .append("endPoint", new Document("type", "Point").append("bearing", this.getEndBearing()).append("coordinates", Arrays.asList(this.getEndPoint().getLongitude(), this.getEndPoint().getLatitude())));
        return doc;
    }

    public Document appendGeoHash(Document document) {
        return document.append("geohash", this.boundingGeohash);
    }
}
