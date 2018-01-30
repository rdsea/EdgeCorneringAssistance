package at.mkaran.thesis.recommendation;

import at.mkaran.thesis.common.*;
import at.mkaran.thesis.commons.loadbalancer.NodeRequestHandler;
import at.mkaran.thesis.commons.loadbalancer.model.ServiceInfo;
import at.mkaran.thesis.commons.loadbalancer.model.ServiceType;
import at.mkaran.thesis.commons.mongodb.CurveStorage;
import at.mkaran.thesis.recommendation.config.FindModeEnum;
import at.mkaran.thesis.recommendation.config.ServerConfig;
import at.mkaran.thesis.recommendation.localCache.mongo.LocalMongoCurveCache;
import at.mkaran.thesis.recommendation.model.ResponseCode;
import at.mkaran.thesis.recommendation.model.RoadCondition;
import at.mkaran.thesis.recommendation.model.WeatherCondition;
import at.mkaran.thesis.recommendation.rabbit.RabbitMQPublisher;
import ch.hsr.geohash.BoundingBox;
import ch.hsr.geohash.GeoHash;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import net.aksingh.owmjapis.CurrentWeather;
import net.aksingh.owmjapis.OpenWeatherMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * gRPC Server that calculates the recommended speed for curves.
 */
public class RecommendationServer {
    private static final Logger logger = Logger.getLogger(RecommendationServer.class.getName());
    private static final int port = 50051;
    private Server server;

    private static final String FIELD_WEATHER_NAME = "%s:weather_name";
    private static final String FIELD_WEATHER_DESC = "%s:weather_desc";
    private static final String FIELD_WEATHER_TEMP = "%s:weather_temp";
    private static final String FIELD_WEATHER_RAIN = "%s:weather_rain";
    private static final String FIELD_WEATHER_TIME = "%s:weather_time";
    private static final long WEATHER_INVALIDATION_INTERVAL = 1000 * 60 * 10; // 10 minutes

    private static RedisCommands<String, String> commands = null;
    private static StatefulRedisConnection<String, String> connection = null;
    private static RedisClient client = null;

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        final RecommendationServer server = new RecommendationServer();
        server.start(new ServerConfig().readFromFile());
        server.blockUntilShutdown();
    }

    private void start(ServerConfig serverConfig) throws IOException {
        logger.info("Starting rec-service with: " + serverConfig.toString());
        // "docker.for.mac.localhost" --> resolves to "localhost" of the container's host (only on MacOSX)
        if (!serverConfig.isAuthEnabled()) {
            CurveStorage.getInstance().initConnectionNoAuth(serverConfig.getMongoUri());
        } else {
            CurveStorage.getInstance().initConnection(serverConfig.getMongoUser(), serverConfig.getMongoPw(), serverConfig.getMongoUri(), serverConfig.useAtlasDB());
        }
        // "localhost" --> resolves to "localhost" of the container
        LocalMongoCurveCache.getInstance().initConnection(serverConfig.getNodeId());

        logger.info("Init Redis");
        if (serverConfig.getNodeId() != null && !serverConfig.getNodeId().isEmpty()) {
            client = RedisClient.create("redis://redis-"+serverConfig.getNodeId());
        } else {
            client = RedisClient.create("redis://redis");
        }
        connection = client.connect();
        commands = connection.sync();

    /* The port on which the server should run */
        server = ServerBuilder.forPort(port)
                .addService(new RecommendationService(serverConfig))
                .build()
                .start();
        logger.info("RecommendationServer with ID: " + serverConfig.getNodeId() + " started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Shutdown redis connections
                commands.close();
                connection.close();
                client.shutdown();
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                RecommendationServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    private static class RecommendationService extends RecommendationGrpc.RecommendationImplBase {

        private static final double REDUCE_FACTOR = 0.41;
        private static final double GRAVITATIONAL_SPEED = 9.81;
        private final RabbitMQPublisher rabbitMQPublisher;
        private ServerConfig config;

        private String[] dryWeatherNames = {"clear sky", "few clouds", "scattered clouds", "broken clouds"};
        private String[] rainWeatherNames = {"shower rain", "rain"};
        private String[] thunderstormWeatherDescription = {"thunderstorm with rain", "thunderstorm with light rain", "thunderstorm with heavy rain"};
        private String[] snowWeatherNames = {"snow"};

        private boolean busy = false;
        private Timer periodicalCpuCheck;

        private NodeRequestHandler nodeRequestHandler;


        public RecommendationService(ServerConfig config) {
            this.config = config;
            this.rabbitMQPublisher = new RabbitMQPublisher();
            if (config.isRecLoadBalancingEnabled() || config.isDetLoadBalancingEnabled()) {
                this.nodeRequestHandler = new NodeRequestHandler(config.getNodeId());
            }
            if (config.isRecLoadBalancingEnabled()) {
                initPeriodicalCPUCheck();
            }

        }

        /**
         * Schedules a task to update the service list for the specified update interval time.
         */
        private void initPeriodicalCPUCheck() {
            final TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    busy = !nodeRequestHandler.canNodeHandleRequest();
                }
            };
            this.periodicalCpuCheck = new Timer();
            this.periodicalCpuCheck.schedule(timerTask, config.getCpuCheckInterval() * 1000,config.getCpuCheckInterval() * 1000);
        }

        @Override
        public void requestRecommendation(RequestDTO request, StreamObserver<ResponseDTO> responseObserver) {
            String reqId = UUID.randomUUID().toString();
            logger.info("New Request: " + reqId + " requestRecommendation on Thread: " + Thread.currentThread().getName());
            if (!config.isRecLoadBalancingEnabled()) {
                doRecommendation(request, responseObserver, false, reqId);
            } else {
                if (!busy) {
                    doRecommendation(request, responseObserver, false, reqId);
                } else {
                    doSendBusyResult(responseObserver);
                }
            }


        }

        @Override
        public void pollDatabase(RequestDTO request, StreamObserver<ResponseDTO> responseObserver) {
            String pollId = UUID.randomUUID().toString();
            logger.info("New Request: " + pollId + " pollDatabase on Thread: " + Thread.currentThread().getName());
            if (!config.isRecLoadBalancingEnabled()) {
                doRecommendation(request, responseObserver, true, pollId);
            } else {
                if (nodeRequestHandler.canNodeHandleRequest()) {
                    doRecommendation(request, responseObserver, true, pollId);
                } else {
                    doSendBusyResult(responseObserver);
                }
            }
        }

        /**
         * Returns the standard message in case the server is busy
         * @param responseObserver
         */
        private void doSendBusyResult(StreamObserver<ResponseDTO> responseObserver) {
            ResponseDTO.Builder responseBuilder = ResponseDTO.newBuilder();
            ResponseCode responseCode = ResponseCode.RECOMMENDATION_BUSY;
            CurveRecommendationListDTO curveResponse = CurveRecommendationListDTO.getDefaultInstance();

            responseBuilder.setResponseCode(responseCode.code());
            responseBuilder.setCurveList(curveResponse);
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        }


        /**
         * Performs the recommendation task on this node.
         * If curves were found in the cache, the recommended speed is calulated for those curves and the response is sent to client.
         * Otherwise the curve-detection is called and the client receives a boolean flag indicating that detection is in progress.
         * @param request The request of the client
         */
        private void doRecommendation(RequestDTO request, StreamObserver<ResponseDTO> responseObserver, boolean isPollRequest, String reqId) {
            logger.info("Handling request: " + reqId);
            ResponseDTO.Builder responseBuilder = ResponseDTO.newBuilder();
            ResponseCode responseCode = ResponseCode.RECOMMENDATION_ERROR;
            CurveRecommendationListDTO curveResponse = CurveRecommendationListDTO.getDefaultInstance();


            CurveListDTO cachedCurves = getCachedCurvesWithinArea(request);
            if (cachedCurves != null) {
                CurveRecommendationListDTO.Builder curveRecommendationBuilder = CurveRecommendationListDTO.newBuilder();

                logger.info("Fetch weather data for curves...");
                WeatherCondition currentWeather = getCurrentWeather(request.getLocation().getLat(), request.getLocation().getLon());
                logger.info("Weather data received. Calculating Recommended Speed for curves...");
                RoadCondition roadCondition = calculateRoadCondition(currentWeather);
                for (CurveDTO curve : cachedCurves.getCurvesList()) {
                    int recommendedSpeed = calculateRecommendedSpeed(curve.getRadius(), roadCondition);
                    CurveRecommendationDTO recommendation = CurveRecommendationDTO.newBuilder()
                            .setCurve(curve)
                            .setRecommendedSpeed(recommendedSpeed)
                            .build();
                    curveRecommendationBuilder.addCurveRecommondations(recommendation);
                }
                logger.info("Calculations done. Returning response to client...");
                responseCode = ResponseCode.CURVES_AVAILABLE;
                curveResponse = curveRecommendationBuilder.build();

            } else {
                if (isPollRequest) {
                    logger.info("Poll Request: Still no curves found in the cache.");
                } else {
                    logger.info("No curves found in the cache. Sending new detection request...");
                    requestCurveDetection(request);
                }
                responseCode = ResponseCode.DETECTION_IN_PROGRESS;
            }


            responseBuilder.setResponseCode(responseCode.code());
            responseBuilder.setCurveList(curveResponse);
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            logger.info("Completed request: " + reqId);
        }

        /**
         * Checks the local cache and (if none found) the distributed cache for curves in the given BoundingBox.
         * The local cache is updated with fetched curves from the distributed cache.
         * @param request The request of a driver
         * @return
         */
        private CurveListDTO getCachedCurvesWithinArea(RequestDTO request) {

            CurveListDTO cachedCurvesFromLocal;
            CurveListDTO cachedCurvesFromDistributed = null;

            double lat = request.getLocation().getLat();
            double lon = request.getLocation().getLon();

            FindModeEnum findMode = config.getFindCurvesMode().getMode();
            int findValue = config.getFindCurvesMode().getValue();


            if (findMode == FindModeEnum.BY_BOUNDING_BOX) {
                logger.info("Find curves in caches by Bounding Box...");
                BoundingBox bb = GeoHash.withCharacterPrecision(lat, lon, findValue).getBoundingBox();
                cachedCurvesFromLocal = LocalMongoCurveCache.getInstance().getDAO().findCurves(bb);
                if (cachedCurvesFromLocal == null) {
                    cachedCurvesFromDistributed = CurveStorage.getInstance().getDAO().findCurves(bb);
                }
            } else if (findMode == FindModeEnum.BY_RADIUS) {
                logger.info("Find curves in caches by radius...");
                cachedCurvesFromLocal = LocalMongoCurveCache.getInstance().getDAO().findCurves(lat, lon, findValue);
                if (cachedCurvesFromLocal == null) {
                    cachedCurvesFromDistributed = CurveStorage.getInstance().getDAO().findCurves(lat, lon, findValue);
                }
            } else {
                logger.info("Find curves in caches by fixed geohash...");
                String fixedGeohash = GeoHash.withCharacterPrecision(lat, lon, findValue).toBase32();
                cachedCurvesFromLocal = LocalMongoCurveCache.getInstance().getDAO().findCurves(fixedGeohash);
                if (cachedCurvesFromLocal == null) {
                    cachedCurvesFromDistributed = CurveStorage.getInstance().getDAO().findCurves(fixedGeohash);
                    if (cachedCurvesFromDistributed != null) {
                        logger.info("Found curves in distributed cache");
                        asyncStoreCurvesFromDistributedToLocalCache(cachedCurvesFromDistributed, fixedGeohash); // Geohash needs to be appended only in this mode
                        return cachedCurvesFromDistributed;
                    }
                }
            }

            if (cachedCurvesFromLocal != null) {
                logger.info("Found curves in local cache");
                return cachedCurvesFromLocal;
            }

            if (cachedCurvesFromDistributed != null) {
                logger.info("Found curves in distributed cache");
                asyncStoreCurvesFromDistributedToLocalCache(cachedCurvesFromDistributed, null);
                return cachedCurvesFromDistributed;
            }

            logger.info("No curves found in (both local and distributed) cache found");
            return null;
        }


        private WeatherCondition getCurrentWeather(double lat, double lon) {
            String geohash  = GeoHash.withCharacterPrecision(lat, lon, config.getWeatherGeohashPrecision()).toBase32();
            WeatherCondition cached = getWeather(geohash);
            if (cached != null) {
                logger.info("Return weather from cache");
                return cached;
            } else {
                if (config.isSimulateWeatherAPICallsEnabled()) {
                    WeatherCondition sim = simulateWeatherAPI();
                    store(geohash,sim);
                    return sim;
                } else {
                    logger.info("Weather cache is empty. Sending request to OWM...");
                    OpenWeatherMap owm = new OpenWeatherMap(config.getOwmApiKey());
                    CurrentWeather owmWeather = owm.currentWeatherByCoordinates((float) lat, (float) lon);
                    long timestamp = System.currentTimeMillis();

                    String weatherName = owmWeather.getWeatherInstance(0).getWeatherName();
                    String weatherDesc = owmWeather.getWeatherInstance(0).getWeatherDescription();
                    logger.info("Received Weather data from OWM: " + weatherName);

                    float rain3h = 0.0f;
                    if (owmWeather.hasRainInstance()) {
                        rain3h = owmWeather.getRainInstance().getRain3h();
                    }
                    float temperature = 0.0f;
                    if (owmWeather.hasMainInstance()) {
                        temperature = owmWeather.getMainInstance().getTemperature();
                    }
                    WeatherCondition current = new WeatherCondition(weatherName, weatherDesc, rain3h, temperature, timestamp);
                    store(geohash, current);
                    return current;
                }
            }
        }

        private WeatherCondition simulateWeatherAPI() {
            logger.info("Weather cache is empty. Simulate query to OWM...");
            return new WeatherCondition("few clouds", "bla", 1.0f, 33.0f, System.currentTimeMillis());
        }

        private RoadCondition calculateRoadCondition(WeatherCondition weather) {
            RoadCondition condition = null;

            // 1. Classify condition by weather names and description
            if (weather.getWeatherName() != null & weather.getWeatherDesc() != null) {
                if (Arrays.asList(dryWeatherNames).contains(weather.getWeatherName())) {
                    condition = RoadCondition.DRY;
                } else if (Arrays.asList(rainWeatherNames).contains(weather.getWeatherName())) {
                    condition = RoadCondition.WET;
                } else if (Arrays.asList(snowWeatherNames).contains(weather.getWeatherName())) {
                    condition = RoadCondition.SNOW;
                } else if (Arrays.asList(thunderstormWeatherDescription).contains(weather.getWeatherDesc())) {
                    condition = RoadCondition.WET;
                } else {
                    // fallback
                    condition = RoadCondition.UNDEFINIED;
                }
            } else {
                // fallback
                return RoadCondition.UNDEFINIED;
            }

            // 2. If it rained within last 3h, the road is still likely to be wet
            if (weather.getRain3h() > 1.0f && (condition == RoadCondition.DRY || condition == RoadCondition.UNDEFINIED)) {
                condition = RoadCondition.WET;
            }

            // 3. Classify glaze
            if (weather.getTemperature() <= 3.0) {
                condition = RoadCondition.GLAZE;
            }

            return condition;
        }

        private int calculateRecommendedSpeed(double curveRadius, RoadCondition roadCondition) {
            int theoreticalMaxSpeed = (int) (Math.sqrt(roadCondition.friction() * GRAVITATIONAL_SPEED * curveRadius) * 3.6);
            return (int) (theoreticalMaxSpeed * REDUCE_FACTOR);
        }

        /**
         * Detect curves for a client's location.
         * Sends lat and lon from the clients request to the RabbitMQ Server of a chosen detection-service.
         * A request is only sent if another request with the same bounding box hasnt already been sent.
         * (This avoids sending unnecessary requests)
         * The detection-service is chosen by the load balancer.
         * Note:    this call is non-blocking and does not return a result to the client.
         *          On the following requests, the recommendation service will have curves in the cache that the client can poll.
         *          (since in the meantime the curve-detection service detects and stores them to the DB)
         *
         * @param request The user request containing location
         */
        private void requestCurveDetection(RequestDTO request) {
            if (config.isSendDetectionRequestsEnabled()) {
                if (!config.isDetLoadBalancingEnabled()) {
                    // send to default detection node
                    asyncSendTupleToDetection(config.getDefaultDetectionServerAddress(), request.getLocation().getLat(), request.getLocation().getLon());
                } else {
                    ServiceInfo leastBusyService = nodeRequestHandler.findLeastBusyNode(ServiceType.DETECTION);
                    if (leastBusyService != null) {
                        String rabbitServerHostname = leastBusyService.getNodeAddress();
                        asyncSendTupleToDetection(rabbitServerHostname, request.getLocation().getLat(), request.getLocation().getLon());
                        logger.info("Curve detection request sent to RabbitMQ Server: " + rabbitServerHostname + " running on: " + leastBusyService.getNodeAddress());

                    } else {
                        logger.warning("Curve detection was requested, but no running detection-service was found.");
                    }
                }
            }
        }

        private void asyncSendTupleToDetection(String serverAddress, double lat, double lon) {
            Thread thread = new Thread(() -> rabbitMQPublisher.requestCurveDetection(serverAddress, lat, lon));
            thread.start();
            logger.info("Curve detection request sent to RabbitMQ Server: " + serverAddress);
        }

        private void asyncStoreCurvesFromDistributedToLocalCache(CurveListDTO distributedCurves, String geohash) {
            Thread thread;
            if (geohash != null) {
                thread = new Thread(() -> LocalMongoCurveCache.getInstance().getDAO().insertMany(distributedCurves, geohash));
            } else {
                thread = new Thread(() -> LocalMongoCurveCache.getInstance().getDAO().insertMany(distributedCurves));
            }
            thread.start();
            logger.info("Copy curves from distributed to local cache");


        }

        private void store(String geohash, WeatherCondition weatherCondition) {
            if (commands != null) {
                logger.info("Store weather");
                commands.set(String.format(FIELD_WEATHER_NAME, geohash), String.valueOf(weatherCondition.getWeatherName()));
                commands.set(String.format(FIELD_WEATHER_DESC, geohash), String.valueOf(weatherCondition.getWeatherDesc()));
                commands.set(String.format(FIELD_WEATHER_TEMP, geohash), String.valueOf(weatherCondition.getTemperature()));
                commands.set(String.format(FIELD_WEATHER_RAIN, geohash), String.valueOf(weatherCondition.getRain3h()));
                commands.set(String.format(FIELD_WEATHER_TIME, geohash), String.valueOf(weatherCondition.getTimestamp()));
            }
        }

        private WeatherCondition getWeather(String geohash) {
            if (commands != null) {
                logger.info("Get Weather");
                String time = commands.get(String.format(FIELD_WEATHER_TIME, geohash));

                if (time != null) {
                    long storedTime = Long.valueOf(time);
                    if (System.currentTimeMillis() - storedTime <= WEATHER_INVALIDATION_INTERVAL) {
                        String temp = commands.get(String.format(FIELD_WEATHER_TEMP, geohash));
                        String name = commands.get(String.format(FIELD_WEATHER_NAME, geohash));
                        String desc = commands.get(String.format(FIELD_WEATHER_DESC, geohash));
                        String rain = commands.get(String.format(FIELD_WEATHER_RAIN, geohash));
                        return new WeatherCondition(name, desc, Float.valueOf(rain), Float.valueOf(temp), storedTime);
                    } else {
                        commands.del(String.format(FIELD_WEATHER_TIME, geohash));
                        commands.del(String.format(FIELD_WEATHER_TEMP, geohash));
                        commands.del(String.format(FIELD_WEATHER_NAME, geohash));
                        commands.del(String.format(FIELD_WEATHER_DESC, geohash));
                        commands.del(String.format(FIELD_WEATHER_RAIN, geohash));
                    }
                }
            }
            return null;
        }
    }


}
