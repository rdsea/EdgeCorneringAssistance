package at.mkaran.thesis.curvedetection;

import at.mkaran.thesis.osm.Node;
import at.mkaran.thesis.osm.Way;
import com.peertopark.java.geocalc.EarthCalc;
import com.peertopark.java.geocalc.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by matthias on 15.08.17.
 */
public class Detection {

    private final Way way;
    private ArrayList<Curve> curves;
    private DetectionParams params;

    public Detection(Way way, DetectionParams params) {
        this.way = way;
        this.params = params;
    }

    /**
     * Detect all Curves of this Way
     */
    public ArrayList<Curve> detectCurves() {
        ArrayList<Node> nodes = way.getNodes();
        curves = new ArrayList<>();

        // 1. Stage: Detect curves using a moving triangle (curves can grow at each iteration)
        for (int index=0; index < (nodes.size() - 2); index++) {
            Triangle triangle = new Triangle(nodes.get(index), nodes.get(index+1), nodes.get(index+2), params.getAngleThreshold());

            Curve currentCurve = new Curve(triangle.getS1().getStart(), triangle.getS2().getEnd(), triangle.getAllPoints(), triangle.getTwistType());
            Curve lastCurve = getPreviousCurve();
            if (lastCurve == null) {
                // first curve or straight
                curves.add(currentCurve);
            } else {
                if (lastCurve.getType() == triangle.getTwistType()) {
                    // extend curve
                    lastCurve.addPoint(triangle.getS2().getEnd());
                    lastCurve.setEnd(triangle.getS2().getEnd());
                } else {
                    // new curve
                    curves.add(currentCurve);
                }
            }
        }

        // 2. Stage: Remove Straights
        ListIterator<Curve> iter = curves.listIterator();
        while(iter.hasNext()){
            Curve currentCurve = iter.next();
            if (currentCurve.getType().equals(TwistType.STRAIGHT)) {
                iter.remove();
            }
        }


        // 3. Stage: Merge connecting curves
        iter = curves.listIterator();
        while(iter.hasNext()){
            Curve currentCurve = iter.next();
            if (iter.hasNext()) {
                Curve nextCurve = curves.get(iter.nextIndex());
                if (nextCurve.getType().equals(currentCurve.getType())) {
                    if (EarthCalc.getDistance(currentCurve.getEnd(), nextCurve.getStart()) < params.getMergeThreshold()) {
                        nextCurve.setStart(currentCurve.getStart());
                        nextCurve.prependPoints(currentCurve.getPoints());
                        iter.remove();
                    }
                }
            }
        }

        // 4. Stage: fix gaps (end/start points often are far away from other curve points)
        for (Curve curve: curves) {
            List<Point> points = curve.getPoints();
            if (points.size() > 3 ) {
                if (EarthCalc.getDistance(curve.getEnd(), points.get(points.size()-2)) > params.getGapThreshold()) {
                    curve.setEnd(points.get(points.size()-2));
                    curve.removePoint(points.size()-1);
                }
                if (EarthCalc.getDistance(curve.getStart(), points.get(1)) > params.getGapThreshold()) {
                    curve.setStart(points.get(1));
                    curve.removePoint(0);
                }
            }
        }


        // 5. Stage: Calculate properties of curves
        iter = curves.listIterator();
        while(iter.hasNext()){
            Curve curve = iter.next();
            curve.removeDuplicatePoints();
            if (curve.getPoints().size() < 3) {
                iter.remove();
                continue;
            }
            curve.calculateCenterPoint();
            curve.calculateLength();
            curve.calculateCircumCircleRadius();
            curve.calculateStartBearing();
            curve.calculateEndBearing();

            if (curve.getRadius() == null || curve.getRadius() > params.getRadiusThreshold()) {
                iter.remove();
            }
        }

        return curves;


    }

    /**
     * Get the previous Curve
     * @return
     */
    private Curve getPreviousCurve() {
        if (curves.size() > 0) {
            return (curves.get(curves.size()-1));
        } else {
            return(null);
        }
    }
}
