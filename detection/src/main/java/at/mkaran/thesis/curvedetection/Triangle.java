package at.mkaran.thesis.curvedetection;

import at.mkaran.thesis.osm.Node;
import at.mkaran.thesis.util.GeoUtil;
import com.peertopark.java.geocalc.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matthias on 16.03.17.
 */
public class Triangle {

    private final double angleThreshold;
    private Segment s1;
    private Segment s2;
    private double angleDiff;
    private TwistType twistType;


    public Triangle(Node n1, Node n2, Node n3, double angleThreshold) {
        this.s1 = new Segment(GeoUtil.toPoint(n1), GeoUtil.toPoint(n2));
        this.s2 = new Segment(GeoUtil.toPoint(n2), GeoUtil.toPoint(n3));
        this.angleDiff = calculateAngle(s1, s2);
        this.twistType = calculateTwistType(s1, s2);
        this.angleThreshold = angleThreshold;
    }

    public Segment getS1() {
        return s1;
    }

    public Segment getS2() {
        return s2;
    }

    public double getAngleDiff() {
        return angleDiff;
    }

    public TwistType getTwistType() {
        return twistType;
    }

    private double calculateAngle(Segment s1, Segment s2) {
        double d = Math.abs(s2.getBearing() - s1.getBearing());
        if (d > 180) {
            d = Math.abs(d - 360);
        }
        return(d);
    }

    private TwistType calculateTwistType(Segment s1, Segment s2) {
        if (angleDiff >= angleThreshold) {
            double d = s2.getBearing() - s1.getBearing();
            if (Math.abs(d) > 180) {
                d = d * -1;
            }
            if (d >= 0) {
                return TwistType.RIGHT;
            } else {
                return TwistType.LEFT;
            }
        } else {
            return TwistType.STRAIGHT;
        }

    }

    public List<Point> getAllPoints() {
        List<Point> allPoints = new ArrayList<>();
        allPoints.add(s1.getStart());
        allPoints.add(s1.getEnd());
        allPoints.add(s2.getStart());
        allPoints.add(s2.getEnd());
        return allPoints;
    }
}
