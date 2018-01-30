package com.k.matthias.corneringassistanceapplication.detection;

/**
 * Implementation of the "Upcoming Curve Detection" algorithm
 */

import android.content.Context;

import com.k.matthias.corneringassistanceapplication.db.LocalCurveCache;
import com.k.matthias.corneringassistanceapplication.model.CachedCurve;
import com.k.matthias.corneringassistanceapplication.model.Location;
import com.k.matthias.corneringassistanceapplication.util.BearingUtil;

import java.util.ArrayList;

public class UpcomingCurveDetection {

    private final CurveDetectedListener mListener;
    private final Context mContext;
    private Location lastLocation;
    private Location currentLocation;
    private double mCurrentDriverBearing = 0.0;
    private CachedCurve mApproachingCurve = null;
    private CachedCurve mLastApprochingCurve = null; // improvement (sometimes right after curve has passed and bounding box was just left, the same curve could be triggered although it was passed already)
    private static final int WARN_BEFORE_CURVE_METERS = 300;
    private double mLastDistanceToCurveStart = WARN_BEFORE_CURVE_METERS;
    private ArrayList<CachedCurve> curveBlackList = new ArrayList<>(); // Blacklist of already driven curves
    private static final int BLACKLIST_SIZE = 10; // size of blacklist

    public UpcomingCurveDetection(CurveDetectedListener listener, Context context) {
        this.mListener = listener;
        this.mContext = context;
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

        // Driver is approaching the curve (e.g. is less than CURVE_APPROACHING_DISTANCE away from the curve's center)
        double distanceToCurveStart = mApproachingCurve.distanceToStart(currentLocation);

        if ((distanceToCurveStart <= mLastDistanceToCurveStart)) {
            // driver is still before curve's starting point
            mListener.onEnteringCurve(mApproachingCurve, (int) distanceToCurveStart);
            mLastDistanceToCurveStart = distanceToCurveStart;

        } else {
            mListener.onPassedCurve();
            // add to list of recently driven curves ("blackList")
            if (curveBlackList.size() > BLACKLIST_SIZE) {
                curveBlackList.clear();
            }
            curveBlackList.add(mApproachingCurve);
            // driver passed curve's starting point: curve is removed
            LocalCurveCache.getInstance(mContext).removeCurve(mApproachingCurve);
            mLastApprochingCurve = mApproachingCurve;
            mApproachingCurve = null;
            mLastDistanceToCurveStart = WARN_BEFORE_CURVE_METERS;
        }

    }

    private void checkForUpcomingCurves() {
        CachedCurve nearest = LocalCurveCache.getInstance(mContext).findNearestCurveResult(currentLocation.getLatitude(), currentLocation.getLongitude());
        if (nearest != null && !curveBlackList.contains(nearest)) {
            if (!nearest.equals(mLastApprochingCurve)) { // discard curves that have just been left
                double distanceToNearest = nearest.distanceToCenter(currentLocation);
                if (distanceToNearest < WARN_BEFORE_CURVE_METERS) {
                    nearest.relocateStartEndPoints(currentLocation);
                    // Check if candidate fits current driving direction (it might be another curve not in the direction of the driver)
                    if (nearest.hasMatchingStartBearing(mCurrentDriverBearing)) {
                        mApproachingCurve = nearest;
                        mListener.onApproachingCurveDetected(nearest, (int) distanceToNearest);
                    }
                }
            }
        }
    }
}

