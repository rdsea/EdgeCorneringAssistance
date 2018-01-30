package car;

import at.mkaran.thesis.common.PointDTO;
import at.mkaran.thesis.common.RequestDTO;
import at.mkaran.thesis.common.ResponseDTO;
import ch.hsr.geohash.BoundingBox;
import ch.hsr.geohash.GeoHash;
import distributed.recommendation.RecommendationClient;
import distributed.recommendation.RecommendationListener;
import model.CachedCurve;
import test.DataQualityTest;
import test.TestConfig;
import util.Log;
import webserver.RabbitMQPublisher;

import java.util.Timer;

/**
 * A driver
 */
public class Driver implements Runnable, GPSListener, RecommendationListener, CurveDetectedListener {

    private final DriverListener driverListener;
    private final NearestCurveDetection nearestCurveDetection;
    private final LocalMongoCurveCache localMongoCurveCache;
    private String id = "test-track-driver";
    private BoundingBox latestBB = null;
    private RecommendationClient recStub = null;
    private double currentLat;
    private double currentLon;
    private int numberOfPollsInCurrentBB = 0;
    private Long lastRequestSentAny = null;
    private Long lastRequestSentRecommendation;

    private TestConfig config;
    private GPS gps;
    private String staticRecServer = null;


    public Driver(TestConfig config, DriverListener driverListener) {
        this.config = config;
        this.driverListener = driverListener;
        this.staticRecServer = this.config.getRecServerAddress();
        this.localMongoCurveCache = new LocalMongoCurveCache();
        this.nearestCurveDetection = new NearestCurveDetection(this, localMongoCurveCache);
    }

    public String getId() {
        return id;
    }

    @Override
    public void run() {
        if (config.callRecommendationServer()) {
            this.recStub = new RecommendationClient(staticRecServer, 50051);
            this.recStub.setListener(this);
        }

        GPSConfig gpsConfig = new GPSConfig(
                config.isAddGPSInaccuraciesEnabled(), 0, 30,
                config.isAddGPSOutagesEnabled(), 5, 10);
        gps = new GPS(config.getConstantDriverSpeed(), this, gpsConfig);
        Log.info("Test-Driver initialized");
        DataQualityTest.DRIVERS_STARTED.inc();
        gps.startGPSSimulation(0);
    }


    @Override
    public void onLocationUpdate(double lat, double lon) {
        RabbitMQPublisher.sendLocationUpdate(lat, lon);
        nearestCurveDetection.driverLocationUpdate(lat, lon);
        this.currentLat = lat;
        this.currentLon = lon;

        DataQualityTest.GPS_LOCATIONS_EMITTED.inc();
        if (tripStartedOrMovedOutOfBoundingBox(lat, lon)) {
            RabbitMQPublisher.sendBoundingBox(lat ,lon,config.getDriverGeoHashPrecision());
            Log.info(this.id + ": moved out of Bounding Box");
            Log.info(this.id + ": location update. " + "["+lat+","+lon+"]");
            RabbitMQPublisher.sendStatus("driver moved out of BB");
            if (config.callRecommendationServer()) {
                callRecommendation(lat, lon,  false);
            }

        }
    }


    private void callRecommendation(double lat, double lon, boolean sendPollRequest) {
        if (this.recStub != null) {
            RequestDTO request = RequestDTO.newBuilder()
                    .setLocation(PointDTO.newBuilder().setLat(lat).setLon(lon).build())
                    .build();
            Log.info(this.id + " Sending request to recommendation server");
            this.lastRequestSentAny = System.currentTimeMillis();
            if (sendPollRequest) {
                Log.info("Send new poll request. Current poll requests: " + this.numberOfPollsInCurrentBB);
                numberOfPollsInCurrentBB++;
                DataQualityTest.REC_POLL_REQUESTS_SENT.inc();
                Thread thread = new Thread(() -> this.recStub.callPollDatabase(request));
                thread.start();
            } else {
                this.numberOfPollsInCurrentBB = 0;
                this.lastRequestSentRecommendation = System.currentTimeMillis();
                DataQualityTest.REC_REQUESTS_SENT.inc();
                Thread thread = new Thread(() -> this.recStub.callRequestRecommendation(request));
                thread.start();
            }
        }
    }

    @Override
    public void onResult(ResponseDTO result) {
        DataQualityTest.REC_RESPONSES_RECEIVED.inc();
        if (this.lastRequestSentAny != null) {
            DataQualityTest.REC_RESPONSE_TIMES.observe(System.currentTimeMillis() - this.lastRequestSentAny);
            this.lastRequestSentAny = null;
        }

        if (result != null) {
            Log.info(this.id + ": Recommendation Result received with response code: " + result.getResponseCode());

            int responseCode = result.getResponseCode();
            switch (responseCode) {
                case 0:
                    // Curves available
                    if (this.lastRequestSentRecommendation != null) {
                        DataQualityTest.REC_RESULT_TIMES.observe(System.currentTimeMillis() - this.lastRequestSentRecommendation);
                        this.lastRequestSentRecommendation = null;
                    }
                    localMongoCurveCache.removeAll(); // clears the cache from old curves
                    localMongoCurveCache.insertMany(result.getCurveList());
                    RabbitMQPublisher.sendStatus("curves received and stored to local cache");
                    RabbitMQPublisher.sendCurves(result.getCurveList().getCurveRecommondationsList());
                    DataQualityTest.CURVES_RECEIVED.inc(result.getCurveList().getCurveRecommondationsCount());
                    break;
                case 1:
                    // Detection request was sent and the detection is busy with detecting curves
                    pollServer();
                    break;
                case 2:
                    // Service is busy
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

        if (numberOfPollsInCurrentBB < config.getMaxRecPolls()) {
            new Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            RabbitMQPublisher.sendStatus("Polling...");
                            callRecommendation(currentLat, currentLon, true);

                        }
                    },
                    config.getSendRecPollDelaySeconds() * 1000
            );
        } else {
            RabbitMQPublisher.sendStatus("Timeout");
            Log.info(id + "Maximum polls reached in current bounding box.");
            DataQualityTest.REC_REQUESTS_TIMEOUT.inc();
            this.lastRequestSentRecommendation = null;
        }
    }


    /**
     * Determines whether or not a driver left a Bounding Box
     * @param lat
     * @param lon
     * @return
     */
    private boolean tripStartedOrMovedOutOfBoundingBox(double lat, double lon) {
        BoundingBox currentBB = GeoHash.withCharacterPrecision(lat, lon, config.getDriverGeoHashPrecision()).getBoundingBox();
        if (currentBB.equals(latestBB)) {
            return false;
        } else {
            this.latestBB = currentBB;
            return true;
        }
    }


    @Override
    public void onTripFinished() {
        driverListener.driverFinishedTrip(this.id);
        DataQualityTest.DRIVERS_ACTIVE.dec();
        DataQualityTest.DRIVERS_FINISHED.inc();
        Log.info(this.id + ": finished. ");
        shutdown();
    }

    @Override
    public void onTripPaused() {

    }

    @Override
    public void onTripResumed() {

    }

    @Override
    public void onCarOutsideOverpassCoverage() {
        Log.info(this.id + ": is not within OverpassCoverage. ");
    }



    @Override
    public void onError(Exception e) {}

    public void shutdown() {
        if (this.recStub != null) {
            try {
                this.recStub.shutdown();
            } catch (InterruptedException e) {
                Log.info(this.id + ": shutting down client failed");
            }
        }
        if (this.gps != null) {
            this.gps.shutdown();
        }

    }


    @Override
    public void onApproachingCurveDetected(CachedCurve curve) {
        RabbitMQPublisher.sendCurve(curve);
    }

    @Override
    public void onEnteringCurve(double distanceLeft) {

    }

    @Override
    public void onPassedCurve() {

    }
}
