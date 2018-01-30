package at.mkaran.thesis.curvedetection;

import at.mkaran.thesis.common.CurveDTO;
import at.mkaran.thesis.common.PointDTO;
import at.mkaran.thesis.util.GeoUtil;
import com.peertopark.java.geocalc.EarthCalc;
import com.peertopark.java.geocalc.Point;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by matthias on 19.03.17.
 */
public class Curve {
    private Point start;
    private Point end;
    private List<Point> points;
    private TwistType type;
    private Double radius;
    private double length;
    private Point centerPoint;
    private Double startBearing;
    private Double endBearing;

    public Curve() {
    }

    public Curve(Point start, Point end, List<Point> points, TwistType type) {
        this.start = start;
        this.end = end;
        this.points = points;
        this.type = type;
    }

    public void removePoint(int index) {
        this.points.remove(index);
    }

    public Double getStartBearing() {
        return startBearing;
    }

    public void setStartBearing(Double startBearing) {
        this.startBearing = startBearing;
    }

    public Double getEndBearing() {
        return endBearing;
    }

    public void setEndBearing(Double endBearing) {
        this.endBearing = endBearing;
    }

    public Point getStart() {
        return start;
    }

    public void setStart(Point start) {
        this.start = start;
    }

    public Point getEnd() {
        return end;
    }

    public void setEnd(Point end) {
        this.end = end;
    }

    public List<Point> getPoints() {
        return points;
    }

    public void setPoints(List<Point> points) {
        this.points = points;
    }

    public TwistType getType() {
        return type;
    }

    public void setType(TwistType type) {
        this.type = type;
    }

    public void addPoint(Point point) {
        this.points.add(point);
    }

    public void addPoints(List<Point> points) {
        this.points.addAll(points);
    }

    public void prependPoints(List<Point> points) {
        this.points.addAll(0, points);
    }

    public Double getRadius() {
        return this.radius;
    }

    private void setRadius(Double radius) {
        this.radius = radius;
    }

    public double getLength() {
        return this.length;
    }

    private void setLength(double length) {
        this.length = length;
    }

    public Point getCenterPoint() {
        return this.centerPoint;
    }

    private void setCenterPoint(Point centerPoint) {
        this.centerPoint = centerPoint;
    }

    public void calculateRadius() {
        double startBearing = EarthCalc.getBearing(start, points.get(1));
        double endBearing = EarthCalc.getBearing(points.get(points.size()-1), end);
        double s = EarthCalc.getDistance(start, end);
        double alpha = GeoUtil.calculateAngle(startBearing, endBearing) * GeoUtil.RADIANS_FACTOR;
        double radius =  s / (2*Math.sin(alpha/2));
        setRadius(round(radius));
    }

    public void calculateCircumCircleRadius(){
        double a = EarthCalc.getDistance(centerPoint, end);
        double b = EarthCalc.getDistance(start, end);
        double c = EarthCalc.getDistance(start, centerPoint);

        if (a > 0 && b > 0 && c > 0) {
            double abc = a*b*c;
            double divider = Math.sqrt(Math.abs((a+b+c)*(b+c-a)*(c+a-b)*(a+b-c)));
            if (divider == 0) {
                setRadius(null);
            } else {
                setRadius(round(abc/divider));
            }
        } else {
            setRadius(null);
        }


    }

    public void calculateLength() {
        double length = 0.0;
        for (int i=0; i < points.size()-1; i++) {
            length += EarthCalc.getDistance(points.get(0), points.get(i+1));
        }
        setLength(round(length));
    }

    public void calculateCenterPoint() {
        int pointCount = points.size();
        if ((pointCount%2)==0) {
            // even
            int previousIndex = (int) Math.floor(points.size()/2);
            int nextIndex = (int) Math.round(points.size()/2);
            Point previousPoint = points.get(previousIndex);
            Point nextPoint = points.get(nextIndex);
            double distance = EarthCalc.getDistance(previousPoint, nextPoint);
            double interpolatePointBearing = EarthCalc.getBearing(previousPoint, nextPoint);
            double[] latlng = GeoUtil.movePoint(previousPoint.getLatitude(), previousPoint.getLongitude(), distance/2, interpolatePointBearing);
            setCenterPoint(Point.build(latlng[0], latlng[1]));
        } else {
            // uneven
            int centralPointIndex = Math.round(points.size()/2);
            Point centralPoint = points.get(centralPointIndex);
            setCenterPoint(centralPoint);
        }

    }

    public void calculateStartBearing() {
        Segment segment = new Segment(points.get(0), points.get(1));
        double startBearing = segment.getBearing();
        setStartBearing(round(startBearing));
    }

    public void calculateEndBearing() {
        Segment segment = new Segment(points.get(points.size() - 2), end);
        double endBearing = segment.getBearing();
        setEndBearing(round(endBearing));
    }

    public void removeDuplicatePoints() {
        Set<Point> pointSet = new LinkedHashSet<Point>(getPoints());
        ArrayList<Point> noDuplicates = new ArrayList<>();
        noDuplicates.addAll(pointSet);
        setPoints(noDuplicates);
    }

    @Override
    public String toString() {
        String string = "radius: " + this.radius;
        string += "length: " + this.length;
        string += "type: " + this.type;
        return string;
    }

    /**
     * Converts the curve object to a DTO
     * @return
     */
    public CurveDTO toDTO() {
        return CurveDTO.newBuilder()
                .setStart(PointDTO.newBuilder().setLat(this.start.getLatitude()).setLon(this.start.getLongitude()))
                .setEnd(PointDTO.newBuilder().setLat(this.end.getLatitude()).setLon(this.end.getLongitude()))
                .setCenter(PointDTO.newBuilder().setLat(this.centerPoint.getLatitude()).setLon(this.centerPoint.getLongitude()))
                .setStartBearing(this.startBearing)
                .setEndBearing(this.endBearing)
                .setLength(this.length)
                .setRadius(this.radius)
                .setTurntype(this.type.name())
                .build();
    }

    public SimpleCurve toSimpleCurve() {
        SimpleCurve curve = new SimpleCurve();
        curve.setCenterPoint(new SimplePoint(this.centerPoint.getLatitude(), this.centerPoint.getLongitude()));
        curve.setEndPoint(new SimplePoint(this.end.getLatitude(), this.end.getLongitude()));
        curve.setStartPoint(new SimplePoint(this.start.getLatitude(), this.start.getLongitude()));
        curve.setStartBearing(this.startBearing);
        curve.setEndBearing(this.endBearing);
        curve.setRadius(this.radius);
        curve.setLength(this.length);
        curve.setType(this.type.name());
        return curve;
    }

    private double round(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
