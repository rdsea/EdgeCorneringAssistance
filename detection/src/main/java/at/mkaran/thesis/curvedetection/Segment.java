package at.mkaran.thesis.curvedetection;

import com.peertopark.java.geocalc.EarthCalc;
import com.peertopark.java.geocalc.Point;

/**
 * Created by matthias on 16.03.17.
 */
public class Segment {
    private Point start;
    private Point end;
    private double bearing;

    public Segment(Point start, Point end) {
        this.start = start;
        this.end = end;
        this.bearing = EarthCalc.getBearing(start, end);
    }

    public Point getStart() {
        return start;
    }

    public Point getEnd() {
        return end;
    }

    public double getBearing() {
        return bearing;
    }
}
