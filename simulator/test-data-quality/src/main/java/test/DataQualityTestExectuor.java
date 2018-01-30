package test;

import io.prometheus.client.CollectorRegistry;
import util.Log;
import webserver.RestResponse;

/**
 * Created by matthias on 21.11.17.
 */
public class DataQualityTestExectuor {

    private static DataQualityTest test;

    public static RestResponse start(TestConfig config) {
        resetPrometheus();
        Log.ENABLE_LOGGING = config.isLoggingEnabled();
        test = new DataQualityTest(config);
        Thread thread = new Thread(test);
        thread.start();
        return new RestResponse("Test started with configs: " + config.toString());
    }

    public static void stop() {
        if (test != null) {
            Log.info("TestExecutor stopped");
            test.shutdown();
            test = null;
        }

    }

    public static void resetPrometheus() {
        CollectorRegistry.defaultRegistry.clear();
    }
}
