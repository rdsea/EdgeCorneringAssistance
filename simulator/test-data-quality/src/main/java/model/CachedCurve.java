package model;

import org.bson.types.ObjectId;
import util.BearingUtil;
import util.Haversine;

/**
 * Created by matthias on 29.11.17.
 */
public class CachedCurve {
    private ObjectId id;
    private Location centerPoint;
    private Location endPoint;
    private Location startPoint;
    private double radius;
    private int recommendedSpeed;
    private double startBearing;
    private double endBearing;

    public CachedCurve(ObjectId id, Location centerPoint, Location endPoint, Location startPoint, double radius, int recommendedSpeed, double startBearing, double endBearing) {
        this.id = id;
        this.centerPoint = centerPoint;
        this.endPoint = endPoint;
        this.startPoint = startPoint;
        this.radius = radius;
        this.recommendedSpeed = recommendedSpeed;
        this.startBearing = startBearing;
        this.endBearing = endBearing;
    }


    public double distanceToStart(Location location) {
        return Haversine.distance(location.getLatitude(), location.getLongitude(), getStartPoint().getLatitude(), getStartPoint().getLongitude());
    }

    public double distanceToEnd(Location location) {
        return Haversine.distance(location.getLatitude(), location.getLongitude(), getEndPoint().getLatitude(), getEndPoint().getLongitude());
    }

    public double distanceToCenter(Location location) {
        return Haversine.distance(location.getLatitude(), location.getLongitude(), getCenterPoint().getLatitude(), getCenterPoint().getLongitude());
    }

    /**
     * Checks whether a given bearing fits the curve's start bearing
     * @param bearing
     * @return
     */
    public boolean hasMatchingStartBearing(double bearing) {
        double bearingDiff = BearingUtil.calculateAngle(bearing, startBearing);
        if (bearingDiff < 45) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Relocates start and end point of the curve according to given Location.
     * In case the end point lies closer than the start point, the two are switched.
     * @param location
     */
    public void relocateStartEndPoints(Location location) {
        double distToStart = distanceToStart(location);
        double distToEnd = distanceToEnd(location);
        if (distToEnd < distToStart) {
            Location copy_start = startPoint;
            startPoint = endPoint;
            endPoint = copy_start;
            // bearings also need to be switched by 180°
            double copy_startBearing = startBearing;
            startBearing = endBearing;
            endBearing = copy_startBearing;
            startBearing = (startBearing + 180)%360;
            endBearing = (endBearing + 180)%360;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CachedCurve that = (CachedCurve) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public Location getCenterPoint() {
        return centerPoint;
    }

    public void setCenterPoint(Location centerPoint) {
        this.centerPoint = centerPoint;
    }

    public Location getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(Location endPoint) {
        this.endPoint = endPoint;
    }

    public Location getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(Location startPoint) {
        this.startPoint = startPoint;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public int getRecommendedSpeed() {
        return recommendedSpeed;
    }

    public void setRecommendedSpeed(int recommendedSpeed) {
        this.recommendedSpeed = recommendedSpeed;
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

    @Override
    public String toString() {
        return "CachedCurve{" +
                "id=" + id +
                ", centerPoint=" + centerPoint +
                ", endPoint=" + endPoint +
                ", startPoint=" + startPoint +
                ", radius=" + radius +
                ", recommendedSpeed=" + recommendedSpeed +
                ", startBearing=" + startBearing +
                ", endBearing=" + endBearing +
                '}';
    }
}
