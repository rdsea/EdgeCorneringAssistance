package car;

import model.CachedCurve;

public interface CurveDetectedListener {
    void onApproachingCurveDetected(CachedCurve curve);
    void onEnteringCurve(double distanceLeft);
    void onPassedCurve();
}
