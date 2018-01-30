package car;


import model.CachedCurve;
import model.Location;
import util.BearingUtil;
import util.Log;
import webserver.RabbitMQPublisher;

import java.util.ArrayList;

public class NearestCurveDetection {

    private final CurveDetectedListener mListener;


    private LocalMongoCurveCache localMongoCurveCache;
    private Location lastLocation;
    private Location currentLocation;
    private double mCurrentDriverBearing = 0.0;
    private CachedCurve mApproachingCurve = null;
    private CachedCurve mLastApprochingCurve = null; // improvement (sometimes right after curve has passed and bounding box was just left, the same curve could be triggered although it was passeD)
    private int mWarnBeforeCurveMeters = 300; // A default value, will be overwritten by Lambda calls
    private double mLastDistanceToCurveStart = mWarnBeforeCurveMeters;
    private ArrayList<CachedCurve> curveBlackList = new ArrayList<>(); // Blacklist of already driven curves
    private static final int BLACKLIST_SIZE = 10; // size of blacklist

    public NearestCurveDetection(CurveDetectedListener listener, LocalMongoCurveCache localMongoCurveCache) {
        this.localMongoCurveCache = localMongoCurveCache;
        this.mListener = listener;
    }

    public void driverLocationUpdate(double lat, double lon) {
        this.currentLocation = new Location(lon, lat);
        if (lastLocation != null) {
            mCurrentDriverBearing = BearingUtil.getBearing(lastLocation, currentLocation);
            if (mApproachingCurve != null) {
                // A curve is approaching
                handleApproachingCurve();
            } else {
                checkForUpcomingCurves();
            }
        }
        lastLocation = currentLocation;
    }

    private void handleApproachingCurve() {

        // Driver is approaching the curve (e.g. he is less than CURVE_APPROACHING_DISTANCE away from the curve's center)
        double distanceToCurveStart = mApproachingCurve.distanceToStart(currentLocation);

        if ((distanceToCurveStart <= mLastDistanceToCurveStart)) {
            // driver is still before curve's starting point
            mListener.onEnteringCurve(distanceToCurveStart);
            mLastDistanceToCurveStart = distanceToCurveStart;

        } else {
            mListener.onPassedCurve();
            // add to list of recently driven curves ("blackList")
            if (curveBlackList.size() > BLACKLIST_SIZE) {
                curveBlackList.clear();
            }
            curveBlackList.add(mApproachingCurve);
            // driver passed curve's starting point: curve is removed
            localMongoCurveCache.removeCurve(mApproachingCurve);
            mLastApprochingCurve = mApproachingCurve;
            mApproachingCurve = null;
            mLastDistanceToCurveStart = mWarnBeforeCurveMeters;
        }

    }

    private void checkForUpcomingCurves() {
        CachedCurve nearest = localMongoCurveCache.findNearestCurveResult(currentLocation.getLatitude(), currentLocation.getLongitude(), 500.0, null);
        if (nearest != null && !curveBlackList.contains(nearest)) {
            if (!nearest.equals(mLastApprochingCurve)) { // discard curves that have just been left
                double distanceToNearest = nearest.distanceToCenter(currentLocation);
                if (distanceToNearest < mWarnBeforeCurveMeters) {
                    nearest.relocateStartEndPoints(currentLocation);
                    // Check if candidate fits current driving direction (it might be another curve not in the direction of the driver)
                    if (nearest.hasMatchingStartBearing(mCurrentDriverBearing)) {
                        RabbitMQPublisher.sendCurve(nearest);
                        mApproachingCurve = nearest;
                        mListener.onApproachingCurveDetected(nearest);
                    }
                }
            }
        }
    }
}
