package test;

import car.Driver;
import car.DriverListener;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import util.Log;
import webserver.RabbitMQPublisher;

/**
 * Simulates a driver with certain speed driving the test-track

 */
public class DataQualityTest implements Runnable, DriverListener {
    private final TestConfig config;

    // Prometheus Metrics for this test
    public static Counter DRIVERS_STARTED;
    public static Gauge DRIVERS_ACTIVE;
    public static Counter DRIVERS_FINISHED;
    public static Counter REC_REQUESTS_SENT;
    public static Counter REC_POLL_REQUESTS_SENT;
    public static Counter REC_RESPONSES_RECEIVED;
    public static Counter REC_REQUESTS_TIMEOUT;
    public static Histogram REC_RESPONSE_TIMES;
    public static Histogram REC_RESULT_TIMES;
    public static Gauge START_TIME;
    public static Gauge END_TIME;
    public static Counter GPS_LOCATIONS_EMITTED;
    public static Counter CURVES_RECEIVED;
    public static Histogram GPS_ERROR_METERS;

    private final static String PROM_TEST_PREFIX = "dq";

    private Driver driver;

    public DataQualityTest(TestConfig config) {
        this.config = config;
    }

    void initPromMetrics() {
        DRIVERS_STARTED = Counter.build()
                .name(PROM_TEST_PREFIX+ "drivers_started")
                .help("Total number of drivers that started a trip.")
                .register();
        DRIVERS_ACTIVE = Gauge.build()
                .name(PROM_TEST_PREFIX+ "drivers_active")
                .help("Total number of drivers that are currently driving.")
                .register();
        DRIVERS_FINISHED = Counter.build()
                .name(PROM_TEST_PREFIX+ "drivers_finished")
                .help("Total number of drivers finished their trips. (Or moved out of the covered Overpass Region)")
                .register();
        REC_REQUESTS_SENT = Counter.build()
                .name(PROM_TEST_PREFIX+ "rec_requests_sent")
                .help("Total number of recommendation requests that have been sent to recommendation services.")
                .register();
        REC_POLL_REQUESTS_SENT = Counter.build()
                .name(PROM_TEST_PREFIX+ "rec_poll_requests_sent")
                .help("Total number of poll requests that have been sent to recommendation services.")
                .register();
        REC_RESPONSES_RECEIVED = Counter.build()
                .name(PROM_TEST_PREFIX+ "rec_responses_received")
                .help("Total number of responses received.")
                .register();
        REC_REQUESTS_TIMEOUT = Counter.build()
                .name(PROM_TEST_PREFIX+ "rec_requests_timeout")
                .help("Total number requests that timed out. Per default the client polls the recommendation services at a maximum of three times. After that the request is called timed out.")
                .register();
        REC_RESPONSE_TIMES = Histogram.build()
                .name(PROM_TEST_PREFIX+ "rec_response_times")
                .help("Time passed between sending a recommendation request (no poll request) to recommendation services and receiving any response.")
                .register();
        REC_RESULT_TIMES = Histogram.build()
                .name(PROM_TEST_PREFIX+ "rec_result_times")
                .help("Time passed between sending a recommendation request (no poll request) to recommendation services and receiving curves as result.")
                .register();
        START_TIME = Gauge.build()
                .name(PROM_TEST_PREFIX+ "simulation_start_time")
                .help("Unix timestamp when Test was started")
                .register();
        END_TIME = Gauge.build()
                .name(PROM_TEST_PREFIX+ "simulation_stop_time")
                .help("Unix timestamp when the test was stopped.")
                .register();
        GPS_LOCATIONS_EMITTED = Counter.build()
                .name(PROM_TEST_PREFIX+ "gps_locations_emitted")
                .help("Total number of gps locations emitted.")
                .register();
        CURVES_RECEIVED = Counter.build()
                .name(PROM_TEST_PREFIX+ "curves_received")
                .help("Total number of received curves from recommendation.")
                .register();
        GPS_ERROR_METERS = Histogram.build()
                .name(PROM_TEST_PREFIX+ "gps_error_meters")
                .help("Error in meters that was added to a GPS coordinate")
                .register();
    }

    @Override
    public void run() {
        initPromMetrics();
        START_TIME.setToCurrentTime();

        driver = new Driver(config, this);
        Thread thread = new Thread(driver);
        thread.start();
    }

    public void shutdown() {
        driver.shutdown();
        Log.info("Test completely shutdown");
        END_TIME.setToCurrentTime();
        RabbitMQPublisher.shutdown();
    }

    @Override
    public void driverFinishedTrip(String id) {

    }


}
