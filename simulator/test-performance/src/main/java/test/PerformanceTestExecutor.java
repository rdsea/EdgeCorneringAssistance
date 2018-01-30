package test;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import util.Log;
import webserver.RestResponse;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

/**
 * Created by matthias on 21.11.17.
 */
public class PerformanceTestExecutor {

    private static Test test;

    public static Counter DRIVERS_STARTED = Counter.build()
                .name("drivers_started")
                .help("Total number of drivers that started a trip.")
                .register();
    public static Gauge DRIVERS_ACTIVE = Gauge.build()
                .name("drivers_active")
                .help("Total number of drivers that are currently driving.")
                .register();
    public static Counter DRIVERS_FINISHED = Counter.build()
                .name("drivers_finished")
                .help("Total number of drivers finished their trips. (Or moved out of the covered Overpass Region)")
                .register();
    public static Counter REC_REQUESTS_SENT = Counter.build()
                .name("rec_requests_sent")
                .help("Total number of recommendation requests that have been sent to recommendation services.")
                .register();
    public static Counter REC_POLL_REQUESTS_SENT = Counter.build()
                .name("rec_poll_requests_sent")
                .help("Total number of poll requests that have been sent to recommendation services.")
                .register();
    public static Counter REC_RESPONSES_RECEIVED = Counter.build()
                .name("rec_responses_received")
                .help("Total number of responses received.")
                .register();
    public static Counter REC_REQUESTS_TIMEOUT = Counter.build()
                .name("rec_requests_timeout")
                .help("Total number requests that timed out. Per default the client polls the recommendation services at a maximum of three times. After that the request is called timed out.")
                .register();
    public static Histogram REC_RESPONSE_TIMES = Histogram.build()
                .name("rec_response_times")
                .help("Time passed between sending a recommendation request (no poll request) to recommendation services and receiving any response.")
                .register();
    public static Histogram REC_RESULT_TIMES = Histogram.build()
                .name("rec_result_times")
                .help("Time passed between sending a recommendation request (no poll request) to recommendation services and receiving curves as result.")
                .register();
    public static Counter CONSUL_REQUESTS_SENT = Counter.build()
                .name("consul_requests_sent")
                .help("Total number of requests sent to Consul Server to retrieve a list of available recommendation services.")
                .register();
    public static Counter CONSUL_REQUESTS_RETRIED = Counter.build()
                .name("consul_requests_retried")
                .help("Total number of retrying to send failed requests to Consul Server.")
                .register();
    public static Counter CONSUL_RESPONSES_SUCCESS = Counter.build()
                .name("consul_responses_success")
                .help("Total number of successfully received responses from consul.")
                .register();
    public static Counter CONSUL_RESPONSES_FAILED = Counter.build()
                .name("consul_responses_failed")
                .help("Total number of failed requests to the Consul Server.")
                .register();
    public static Counter CONSUL_RESPONSES_TIMEOUT = Counter.build()
                .name("consul_responses_timeout")
                .help("Total number of timeouts to Consul Server. Per default clients retry sending failed requests at a maximum of three times. After that a request is called timed out.")
                .register();
    public static Gauge START_TIME = Gauge.build()
                .name("simulation_start_time")
                .help("Unix timestamp when Test was started")
                .register();
    public static Gauge END_TIME = Gauge.build()
                .name("simulation_stop_time")
                .help("Unix timestamp when the test was stopped.")
                .register();
    public static Gauge  SHUTDOWN_TIME = Gauge.build()
                .name("simulation_shutdown_time")
                .help("Unix timestamp when the test was eventually shutdown.")
                .register();
    public static Counter REQUESTS_INITIATED = Counter.build()
                .name("requests_created")
                .help("Number of requests were initiated (but not yet successfully cued and sent).")
                .register();
    public static Counter REQUEST_INITS_FAILED = Counter.build()
            .name("request_inits_failed")
            .help("Number of requests that could not be created due to problems on the client side (Threading for example).")
            .register();
    public static Counter REQUEST_GRPC_ERROR = Counter.build()
            .name("request_grpc_error")
            .help("Number of grpc errors occured when trying to send a request (this means sending failed)")
            .register();
    public static Counter METERS_TRAVELLED = Counter.build()
            .name("meters_travelled")
            .help("Meters travelled in total by all drivers")
            .register();
    public static Counter BUSY_RESPONSES = Counter.build()
            .name("busy_response")
            .help("Total number of busy responses from recommendation service.")
            .register();

    public static RestResponse start(TestConfig config) {
        Log.ENABLE_LOGGING = config.isLoggingEnabled();
        test = new PerformanceTest(config);
        Thread thread = new Thread(test);
        thread.start();
        return new RestResponse("Performance-Test started with configs: " + config.toString());
    }

    public static void stop() {
        if (test != null) {
            Log.info("PerformanceTestExecutor stopped");
            test.shutdown();
            test = null;
        }

    }

    public static void resetPrometheus() {
        CollectorRegistry.defaultRegistry.clear();
    }
}
