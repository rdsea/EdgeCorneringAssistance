package car;

import at.mkaran.thesis.common.PointDTO;
import at.mkaran.thesis.common.RequestDTO;
import at.mkaran.thesis.common.ResponseDTO;
import ch.hsr.geohash.BoundingBox;
import ch.hsr.geohash.GeoHash;
import distributed.consul.Consul;
import distributed.consul.ConsulListener;
import distributed.recommendation.RecommendationClient;
import distributed.recommendation.RecommendationListener;
import model.Service;
import test.PerformanceTestExecutor;
import test.TestConfig;
import util.Log;

import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A driver
 */
public class Driver implements Runnable, GPSListener, ConsulListener, RecommendationListener {

    private final DriverListener driverListener;
    private Consul consul;
    private String id;
    private BoundingBox latestBB = null;
    private Service closestRecService = null;
    private RecommendationClient recStub = null;
    private double currentLat;
    private double currentLon;
    private int numberOfPollsInCurrentBB = 0;
    private Long lastRequestSentAny = null;
    private Long lastRequestSentRecommendation;
    private ExecutorService grpcRequestThreadExecutor;


    private TestConfig testConfig;
    private GPS gps;
    private double lastLat;
    private double lastLon;


    public Driver(String id, TestConfig testConfig, DriverListener driverListener) {
        this.id = id;
        this.testConfig = testConfig;
        this.driverListener = driverListener;
        this.grpcRequestThreadExecutor = Executors.newSingleThreadExecutor();
        Log.info("car.Driver " + id + ": initialized");
    }

    public String getId() {
        return id;
    }

    @Override
    public void run() {
        if (this.testConfig.isLoadBalancingEnabled()) {
            this.consul = new Consul(this, testConfig.getRefreshConsulPeriodSeconds(), testConfig.getMaxConsulRetries());
        } else {
            this.recStub = new RecommendationClient(testConfig.getRecServerAddress(), 50051, testConfig.getGrpcTimeout());
            this.recStub.setListener(this);
            gps = new GPS(this.id);
            Log.info("car.Driver " + id + ": car.GPS initialized");
            PerformanceTestExecutor.DRIVERS_STARTED.inc();
            PerformanceTestExecutor.DRIVERS_ACTIVE.inc();
            gps.startGPSSimulation(this);
        }
    }

    /**
     * Determines whether or not a driver left a Bounding Box
     * @param lat
     * @param lon
     * @return
     */
    private boolean tripStartedOrMovedOutOfBoundingBox(double lat, double lon) {
        BoundingBox currentBB = GeoHash.withCharacterPrecision(lat, lon, testConfig.getDriverGeoHashPrecision()).getBoundingBox();
        if (currentBB.equals(latestBB)) {
            return false;
        } else {
            this.latestBB = currentBB;
            return true;
        }
    }

    @Override
    public void onLocationUpdate(double lat, double lon) {
        this.currentLat = lat;
        this.currentLon = lon;
        //Log.info(this.id + ": location update. " + "["+lat+","+lon+"]");
        if (tripStartedOrMovedOutOfBoundingBox(lat, lon)) {
            Log.info(this.id + ": moved out of Bounding Box");
            Log.info(this.id + ": location update. " + "["+lat+","+lon+"]");
            callRecommendation(lat, lon, true, false);
        }
    }


    private void callRecommendation(double lat, double lon, boolean updateNode, boolean sendPollRequest) {
        lastLat = lat;
        lastLon = lon;
        if (updateNode && testConfig.isLoadBalancingEnabled()) {
            connectToClosestRecService(lat, lon); // since the driver is moving, a new (fog) node could be close
        }
        if (this.recStub != null) {
            RequestDTO request = RequestDTO.newBuilder()
                    .setLocation(PointDTO.newBuilder().setLat(lat).setLon(lon).build())
                    .build();
            Log.info(this.id + " Sending request to recommendation server: " + recStub.getHost());
            this.lastRequestSentAny = System.currentTimeMillis();
            if (sendPollRequest) {
                Log.info("Send new poll request. Current poll requests: " + this.numberOfPollsInCurrentBB);
                numberOfPollsInCurrentBB++;
                asyncCallRequest(true, request);
                PerformanceTestExecutor.REC_POLL_REQUESTS_SENT.inc();
            } else {
                this.numberOfPollsInCurrentBB = 0;
                this.lastRequestSentRecommendation = System.currentTimeMillis();
                asyncCallRequest(false, request);;
                PerformanceTestExecutor.REC_REQUESTS_SENT.inc();
            }
        }
    }

    private void asyncCallRequest(boolean poll, RequestDTO request) {
        PerformanceTestExecutor.REQUESTS_INITIATED.inc();
        Thread thread;
        if (poll) {
            thread = new Thread(() -> this.recStub.callPollDatabase(request));
        } else {
            thread = new Thread(() -> this.recStub.callRequestRecommendation(request));
        }
        try {
            grpcRequestThreadExecutor.execute(thread);
        } catch (Exception e) {
            PerformanceTestExecutor.REQUEST_INITS_FAILED.inc();
        }
    }

    @Override
    public void onResult(ResponseDTO result) {
        PerformanceTestExecutor.REC_RESPONSES_RECEIVED.inc();
        if (this.lastRequestSentAny != null) {
            PerformanceTestExecutor.REC_RESPONSE_TIMES.observe(System.currentTimeMillis() - this.lastRequestSentAny);
            this.lastRequestSentAny = null;
        }

        if (result != null) {
            Log.info(this.id + ": Recommendation Result received with response code: " + result.getResponseCode());

            int responseCode = result.getResponseCode();
            switch (responseCode) {
                case 0:
                    // Curves available
                    if (this.lastRequestSentRecommendation != null) {
                        PerformanceTestExecutor.REC_RESULT_TIMES.observe(System.currentTimeMillis() - this.lastRequestSentRecommendation);
                        this.lastRequestSentRecommendation = null;
                    }
                    break;
                case 1:
                    // Detection request was sent and the detection is busy with detecting curves
                    pollServer();
                    break;
                case 2:
                    // Service is busy
                    PerformanceTestExecutor.BUSY_RESPONSES.inc();
                    this.closestRecService.setBusy();
                    callRecommendation(lastLat, lastLon, true, false);
                    break;
                case 3:
                    // Error Response
                    break;
                default:
                    break;
            }
        } else {
            Log.info(this.id + ": Recommendation Result is null");
        }
    }

    /**
     * Sends a request to the recommendation service with a Delay (default: 1sec).
     *
     */
    private void pollServer() {

        if (numberOfPollsInCurrentBB < testConfig.getMaxRecPolls()) {
            new Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            callRecommendation(currentLat, currentLon, false, true);
                        }
                    },
                    testConfig.getSendRecPollDelaySeconds() * 1000
            );
        } else {
            Log.info(id + "Maximum polls reached in current bounding box.");
            PerformanceTestExecutor.REC_REQUESTS_TIMEOUT.inc();
            this.lastRequestSentRecommendation = null;
        }
    }

    @Override
    public void onServiceListUpdateSuccess(int numberOfAvailableServices) {
        Log.info(this.id + " Service List Update. " + numberOfAvailableServices + " available services");
        if (this.closestRecService == null && numberOfAvailableServices > 0) {
            // kickoff
            gps = new GPS(this.id);
            Log.info("car.Driver " + id + ": car.GPS initialized");
            PerformanceTestExecutor.DRIVERS_ACTIVE.inc();
            gps.startGPSSimulation(this);
        } else {
            Log.error("No Available Rec Services");
        }
    }

    /**
     * Determines the closest recommendation service to the given location.
     * In case a new recommendation service was found closer to the current one,
     * the current one will be shutdown and replaced the new one.
     * @param lat
     * @param lon
     */
    private void connectToClosestRecService(double lat, double lon) {
        Service currentClosest = consul.findClosestService(lat, lon);

        if (currentClosest != null) {
            if (!currentClosest.equals(this.closestRecService)) {
                    Log.info(this.id + ": found new closest service: " + currentClosest.toString());

                    // shutdown old connection
                    shutDownOldConnection();

                    // store meta infos about current service
                    this.closestRecService = currentClosest;
                    // establish connection to new node
                    String address = currentClosest.getAddress();
                    this.recStub = new RecommendationClient(address, currentClosest.getPort(), testConfig.getGrpcTimeout());
                    this.recStub.setListener(this);

            }

        } else {
            shutDownOldConnection();
            Log.info(this.id + ": currently no recommendation service is available");
        }

    }

    private void shutDownOldConnection() {
        if (this.recStub != null) {
            try {
                this.recStub.shutdown();
            } catch (InterruptedException e) {
                Log.error(this.id + "Could not shutdown current recommendation client");
            }
        }
    }


    @Override
    public void onTripFinished() {
        driverListener.driverFinishedTrip(this.id);
        PerformanceTestExecutor.DRIVERS_ACTIVE.dec();
        PerformanceTestExecutor.DRIVERS_FINISHED.inc();
        Log.info(this.id + ": finished. ");
        shutdown();
    }

    @Override
    public void onCarOutsideOverpassCoverage() {
        Log.info(this.id + ": is not within OverpassCoverage. ");
    }



    @Override
    public void onServiceListUpdateFailed() {
        Log.error(this.id + " Service List Update failed ");
    }

    @Override
    public void onGrpcError(Exception e) {
        PerformanceTestExecutor.REQUEST_GRPC_ERROR.inc();
    }

    public void shutdown() {
        if (this.recStub != null) {
            try {
                this.recStub.shutdown();
            } catch (InterruptedException e) {
                Log.info(this.id + ": shutting down client failed");
            }
        }
        if (this.consul != null) {
            this.consul.shutdown();
        }
        if (this.gps != null) {
            this.gps.shutdown();
        }
        this.grpcRequestThreadExecutor.shutdownNow();

    }


}
