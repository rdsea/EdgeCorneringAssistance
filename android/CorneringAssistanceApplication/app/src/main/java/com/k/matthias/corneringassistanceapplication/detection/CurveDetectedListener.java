package com.k.matthias.corneringassistanceapplication.detection;

import com.k.matthias.corneringassistanceapplication.model.CachedCurve;

public interface CurveDetectedListener {
    void onApproachingCurveDetected(CachedCurve curve, int distanceLeft);
    void onEnteringCurve(CachedCurve curve, int distanceLeft);
    void onPassedCurve();
}
