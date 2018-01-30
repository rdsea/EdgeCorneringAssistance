package at.mkaran.thesis.util;

import at.mkaran.thesis.curvedetection.TwistType;
import at.mkaran.thesis.osm.Node;
import com.peertopark.java.geocalc.Coordinate;
import com.peertopark.java.geocalc.DegreeCoordinate;
import com.peertopark.java.geocalc.Point;

import static java.lang.Math.*;


/**
 * Created by matthias on 19.03.17.
 */
public class GeoUtil {

    public static final double RADIANS_FACTOR = 0.017453293;
    static final double BEARING_WEST = 270.0;
    static final double BEARING_NORTH = 0.0;
    static final double BEARING_EAST = 90.0;
    static final double BEARING_SOUTH = 180.0;


    public static double calculateAngle(double startBearing, double endBearing) {
        double d = Math.abs(endBearing - startBearing);
        if (d > 180) {
            d = Math.abs(d - 360);
        }
        return(d);
    }

    /**
     * Moves a point by distance and bearing
     * @param latitude          Latitude of starting point
     * @param longitude         Longitude of starting point
     * @param distanceInMetres  Distance that you want to move the point by
     * @param bearing           an angle, direction towards which you want to move the point. 0 is towards the North, 90 - East, 180 - South, 270 - West. And all between, i.e. 45 is North East.
     * @return  double Array containing latitude, longitude of destination point
     */
    public static double[] movePoint(double latitude, double longitude, double distanceInMetres, double bearing) {
        double brngRad = toRadians(bearing);
        double latRad = toRadians(latitude);
        double lonRad = toRadians(longitude);
        int earthRadiusInMetres = 6371000;
        double distFrac = distanceInMetres / earthRadiusInMetres;

        double latitudeResult = asin(sin(latRad) * cos(distFrac) + cos(latRad) * sin(distFrac) * cos(brngRad));
        double a = atan2(sin(brngRad) * sin(distFrac) * cos(latRad), cos(distFrac) - sin(latRad) * sin(latitudeResult));
        double longitudeResult = (lonRad + a + 3 * PI) % (2 * PI) - PI;

        double[] arr = {toDegrees(latitudeResult), toDegrees(longitudeResult)};
        return arr;
    }

    public static double bearingDiff(double b1, double b2) {
        double d = Math.abs(b2 - b1);
        if (d > 180) {
            d = Math.abs(d - 360);
        }
        return(d);
    }

    public static double getNormalBearing(double bearing, TwistType type) {
        double normalBearing = 0.0;
        if (type.equals(TwistType.LEFT)) {
            normalBearing = bearing - 90.0;
        } else {
            normalBearing = bearing + 90.0;
        }
        return(normalBearing % 360);
    }

    public static Point toPoint (Node node) {
        Coordinate lat = new DegreeCoordinate(node.getLatitude());
        Coordinate lng = new DegreeCoordinate(node.getLongitude());
        return new Point(lat, lng);
    }
}
