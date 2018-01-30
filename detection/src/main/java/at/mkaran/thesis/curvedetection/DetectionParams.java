package at.mkaran.thesis.curvedetection;

/**
 * Created by matthias on 16.06.17.
 */
public class DetectionParams {
    /**
     * Minimum angle between two segments of a triangle to be considered a curve (otherwise its a straight)
     */
    private double angleThreshold = 2.0;
    /**
     * Sometimes the curve curvedetection detects two distinct curves of same type (e.g. both right or left) that are very close to each other.
     * If the distance between two curves of same type exceeds this threshold, two curves are merged together
     */
    private double radiusThreshold = 1000;
    /**
     * Sometimes start/end points of a curve are far away from the rest of the curve.
     * If the distance between start/end point and the second/next_to_last point of the curve exceeds this threshold, the start/end point is moved
     */
    private double mergeThreshold = 50;
    /**
     * Sometimes start/end points of a curve are far away from the rest of the curve.
     * If the distance between start/end point and the second/next_to_last point of the curve exceeds this threshold, the start/end point is moved
     */
    private double gapThreshold = 30;

    public DetectionParams() {
    }

    public DetectionParams(double angleThreshold, double radiusThreshold, double mergeThreshold, double gapThreshold) {
        this.angleThreshold = angleThreshold;
        this.radiusThreshold = radiusThreshold;
        this.mergeThreshold = mergeThreshold;
        this.gapThreshold = gapThreshold;
    }

    public static DetectionParams getDefaults() {
        return new DetectionParams(2.0, 1000, 50, 30);
    }

    public double getAngleThreshold() {
        return angleThreshold;
    }

    public double getRadiusThreshold() {
        return radiusThreshold;
    }

    public double getMergeThreshold() {
        return mergeThreshold;
    }

    public double getGapThreshold() {
        return gapThreshold;
    }
}
