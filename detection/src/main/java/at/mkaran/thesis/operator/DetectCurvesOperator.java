package at.mkaran.thesis.operator;

import at.mkaran.thesis.curvedetection.Curve;
import at.mkaran.thesis.curvedetection.Detection;
import at.mkaran.thesis.curvedetection.DetectionParams;
import at.mkaran.thesis.curvedetection.SimpleCurve;
import at.mkaran.thesis.osm.Way;
import com.datatorrent.api.Context;
import com.datatorrent.api.DefaultInputPort;
import com.datatorrent.api.DefaultOutputPort;
import com.datatorrent.common.util.BaseOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Detect curves on the incoming tuple (OSM Way object)
 */
public class DetectCurvesOperator extends BaseOperator {

    private static final Logger LOG = LoggerFactory.getLogger(DetectCurvesOperator.class);

    private double angleThreshold;
    private double radiusThreshold;
    private double mergeThreshold;
    private double gapThreshold;
    private DetectionParams detectionParams;


    @Override
    public void setup(Context.OperatorContext context) {
        super.setup(context);
        this.detectionParams = new DetectionParams(angleThreshold, radiusThreshold, mergeThreshold, gapThreshold);
    }

    @Override
    public void beginWindow(long windowId) {
        super.beginWindow(windowId);
    }

    /**
     * Input port on which ways from OSM are received
     */
    public final transient DefaultInputPort<Way> inputPort = new DefaultInputPort<Way>() {
        @Override
        public void process(Way way) {
            Detection detection = new Detection(way, detectionParams);
            LOG.info("detecting curves on way: " + way.getName());
            ArrayList<Curve> curves = detection.detectCurves();

            for (Curve curve : curves) {
                SimpleCurve simpleCurve = curve.toSimpleCurve();
                simpleCurve.setBoundingGeohash(way.getBoundingGeohash());
                outputPort.emit(simpleCurve);
            }
            LOG.info("emitted " + curves.size() + " detected curves");
        }

    };

    /**
     * Output port which emits ways within the given geoHash
     */
    public final transient DefaultOutputPort<SimpleCurve> outputPort = new DefaultOutputPort<SimpleCurve>();

    @Override
    public void endWindow() {
        super.endWindow();
    }

    @Override
    public void teardown() {
        super.teardown();
    }

    public void setAngleThreshold(double angleThreshold) {
        this.angleThreshold = angleThreshold;
    }

    public void setRadiusThreshold(double radiusThreshold) {
        this.radiusThreshold = radiusThreshold;
    }

    public void setMergeThreshold(double mergeThreshold) {
        this.mergeThreshold = mergeThreshold;
    }

    public void setGapThreshold(double gapThreshold) {
        this.gapThreshold = gapThreshold;
    }
}
