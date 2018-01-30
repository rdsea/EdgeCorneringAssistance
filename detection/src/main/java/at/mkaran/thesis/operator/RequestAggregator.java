package at.mkaran.thesis.operator;

import at.mkaran.thesis.Application;
import at.mkaran.thesis.model.Request;
import ch.hsr.geohash.GeoHash;
import com.datatorrent.api.DefaultInputPort;
import com.datatorrent.api.DefaultOutputPort;
import com.datatorrent.common.util.BaseOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Operator that receives tuples from LoadBalancer and aggregates (i.e. groups) requests by
 * 1) time (default = 10 application windows, where 1 window = 500ms, i.e. 5 seconds)
 * 2) location (locations that have the same geohash of given precision)
 */
public class RequestAggregator extends BaseOperator {

    private static final Logger LOG = LoggerFactory.getLogger(RequestAggregator.class);


    private int receivedTuplesInWindow = 0;
    private int geoHashPrecision;
    protected List<String> aggregatedRequests = new ArrayList<>();


    /**
     * Input port on which requests are received
     */
    public final transient DefaultInputPort<Request> inputPort = new DefaultInputPort<Request>() {
        /**
         * call belongs to the InputPort and gets triggered when any tuple arrives at the Input port of the operator.
         * This call is specific only to Generic and Output adapters, since Input Adapters do not have an input port.
         * This is made for all the tuples at the input port until the end window marker tuple is received on the input port.
         */
        @Override
        public void process(Request request) {
            receivedTuplesInWindow++;
            // Calculate geohash of configured precision
            String geohash = GeoHash.withCharacterPrecision(request.getLat(), request.getLon(), geoHashPrecision).toBase32();

            // Do aggregation
            if (!aggregatedRequests.contains(geohash)) {
                aggregatedRequests.add(geohash);
            }

        }
    };

    /**
     * Output port which emits geohashes (i.e. grouped locations from possibly multiple requests)
     */
    public final transient DefaultOutputPort<String> outputPort = new DefaultOutputPort<>();

    /**
     * call marks the end of the window and allows for any processing to be done after the window ends.
     */
    @Override
    public void endWindow() {
        //System.out.println("RequestAggregator: endWindow " + System.currentTimeMillis() );

        if (aggregatedRequests.isEmpty()) {
            return;
        }


        LOG.info(" aggregated " + receivedTuplesInWindow + " requests to " + aggregatedRequests.size() + " geohashes");
        for (String geohash : aggregatedRequests) {
            LOG.info(" emitting geohashed location " + geohash);
            outputPort.emit(geohash);
        }

        aggregatedRequests.clear();
        receivedTuplesInWindow=0;
        super.endWindow();
    }

    /**
     * call is used for gracefully shutting down the operator and releasing any resources held by the operator.
     */
    @Override
    public void teardown() {
        super.teardown();
    }

    public void setGeoHashPrecision(int geoHashPrecision) {
        this.geoHashPrecision = geoHashPrecision;
    }
}
