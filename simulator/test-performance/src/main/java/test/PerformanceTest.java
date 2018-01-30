package test;

import car.Driver;
import car.DriverListener;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Tests the complete application flow:
 *
 * car -> consul
 * car <- consul (rec-service list)
 * car -> rec (call closest)
 * ...
 *
 * option 1) new location:
 * car -> rec -> mongo
 * rec -> det
 * car <-- rec (detection busy)
 * car --> rec (poll)
 * car <-- rec (poll result)
 * ...
 * det -> mongo (detection done)
 * car --> rec (poll)
 * car <-- rec (poll result)
 *
 * option 2) cached location:
 * car -> rec -> mongo
 * car <-- rec (result)
 *

 */
public class PerformanceTest extends Test implements DriverListener {
    private List<Driver> driverList = new ArrayList<>();
    private final Semaphore mySemaphore;
    private boolean testRunning = false;
    private TripManager tripManager;

    private ExecutorService driverThreadExecutor;

    public PerformanceTest(TestConfig config) {
        super(config);
        mySemaphore = new Semaphore(config.getNumberOfDrivers());
    }

    @Override
    public void run() {
        testRunning = true;
        tripManager = new TripManager();
        PerformanceTestExecutor.START_TIME.setToCurrentTime();

        // create a Thread Pool of fixed size: numberOfDrivers (Each driver needs his own thread)
        driverThreadExecutor = Executors.newFixedThreadPool(config.getNumberOfDrivers());

        while (testRunning) {
            try {
                Log.info("Aquiring Semaphore permit...");
                mySemaphore.acquire();
                Log.info("Aquired Semaphore permit");

                String tripId = tripManager.getNewUnusedTripId();
                if (tripId != null && PerformanceTestExecutor.DRIVERS_ACTIVE.get() < config.getNumberOfDrivers()) {
                    Driver driver = new Driver(tripId, config, this);
                    this.driverList.add(driver);
                    driverThreadExecutor.execute(new Thread(driver));

                } else {
                    Log.info("No more trips avaialable or numDrivers exceeded");
                    mySemaphore.release();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }



    @Override
    public void shutdown() {
        testRunning = false;
        PerformanceTestExecutor.END_TIME.setToCurrentTime();
        for (Driver driver : driverList) {
            Log.info("Shutting down driver: " + driver.getId());
            driver.shutdown();
        }
        driverThreadExecutor.shutdown();
        Log.info("Test completely shutdown");
        PerformanceTestExecutor.SHUTDOWN_TIME.setToCurrentTime();
    }

    @Override
    public void driverFinishedTrip(String id) {
        tripManager.releaseTripId(id);
        if (testRunning) {
            mySemaphore.release();
        }
    }


}
