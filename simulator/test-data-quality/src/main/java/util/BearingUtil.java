package util;

import model.Location;

import static java.lang.Math.*;

public final class BearingUtil {

    /**
     * Get angle between two bearings
     * @param startBearing
     * @param endBearing
     * @return
     */
    public static double calculateAngle(double startBearing, double endBearing) {
        double d = Math.abs(endBearing - startBearing);
        if (d > 180) {
            d = Math.abs(d - 360);
        }
        return(d);
    }

    /**
     * Returns the (azimuth) bearing, in decimal degrees, from standPoint to forePoint
     *
     * @param standPoint Origin point
     * @param forePoint  Destination point
     * @return (azimuth) bearing, in decimal degrees
     */
    public static double getBearing(Location standPoint, Location forePoint) {
        /**
         * Formula: θ = atan2( 	sin(Δlong).cos(lat2), cos(lat1).sin(lat2) − sin(lat1).cos(lat2).cos(Δlong) )
         */

        double y = sin(toRadians(forePoint.getLongitude() - standPoint.getLongitude())) * cos(toRadians(forePoint.getLatitude()));
        double x = cos(toRadians(standPoint.getLatitude())) * sin(toRadians(forePoint.getLatitude()))
                - sin(toRadians(standPoint.getLatitude())) * cos(toRadians(forePoint.getLatitude())) * cos(toRadians(forePoint.getLongitude() - standPoint.getLongitude()));

        double bearing = (atan2(y, x) + 2 * PI) % (2 * PI);

        return toDegrees(bearing);
    }


}
