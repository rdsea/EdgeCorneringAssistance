package car;

/**
 * Listener to listen for car.GPS Updates
 */
public interface GPSListener {
    void onLocationUpdate(double lat, double lon);

    void onTripFinished();

    void onTripPaused();

    void onTripResumed();

    void onCarOutsideOverpassCoverage();
}
