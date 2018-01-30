package at.mkaran.thesis.recommendation.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configurable parameters for the gRPC recommendation service.
 * Parameters are specified using .env file in the docker directory.
 */
public class ServerConfig {


    /**
     * A unique string that is appended to the recommendation service name.
     * This unique ID is used for service discovery.
     */
    private String nodeId;
    /**
     * Enable/Disable load balancing for recommendation services.
     * If enabled: On every request to the gRPC server, prometheus is queried to find out if the current node is busy.
     * Otherwise: Every request is handled by the current node, regardless of current load.
     */
    private boolean loadBalanceRec;
    /**
     * Enable/Disable load balancing for detection services.
     * If enabled: On every request to the detection, prometheus is queried first to find out which detection service
     * has the least usage.
     * Otherwise: Every request is handled by a static node, regardless of current load.
     */
    private boolean loadBalanceDet;
    /**
     * Specify a default address to call the detection.
     * This is only necessary if load balancing is deactivated.
     */
    private String defaultDetectionServerAddress;
    /**
     * Enable/Disable simulating queries to OWM.
     * For testing purposes only.
     */
    private boolean simulateWeatherAPICalls;
    /**
     * Enable/Disable sending requests to the detection service.
     * For testing purposes only.
     */
    private boolean sendDetectionRequests;
    /**
     * Curves can be queried by following modes.
     * For each mode, a <code>value</code> also needs to be configured.
     * - <code>"bb"</code>: a bounding box of configurable size. <code>value</code>: size of the bounding box (expressed in geohash precision)
     * - <code>"radius"</code>: a configurable radius. <code>value</code>: Radius in meters
     * - <code>"geohash"</code>: a fixed geohash that is appended when curves are detected. <code>value</code>: fixed geohash precision (must be the same as used in detection)
     */
    private FindCurvesMode findCurvesMode;
    /**
     * Precision of the geohash that is used to describe the area of received weather conditions from OWM.
     * The smaller the precision, the larger the area for the retrieved weather of a specific location.
     * Note: Using small precision values reduces queries to OWM, but the weather can be inaccurate for specific locations.
     * Using large precision values increases the accuracy of the weather for a specific location, but also increases
     * the amount of queries sent to OWM.
     */
    private int weatherGeohashPrecision;

    /**
     * Distributed MongoDB Credentials
     */
    private String mongoUri;
    private String mongoUser;
    private String mongoPw;
    /**
     * Specify whether to use Atlas DaaS provider
     */
    private boolean useAtlasDB;
    /**
     * If enabled, the distributed MongoDB instance does not require Authentification
     */
    private boolean enableAuth;
    /**
     * Key to use OpenWeatherMaps
     */
    private String owmApiKey;

    /**
     * Interval in Seconds to check CPU usage (prometheus)
     */
    private int cpuCheckInterval;


    public ServerConfig() {
    }

    public ServerConfig(String nodeId) {
        this.nodeId = nodeId;
    }

    public ServerConfig(String nodeId, boolean loadBalanceRec, boolean loadBalanceDet, String defaultDetectionServerAddress, boolean simulateWeatherAPICalls, boolean sendDetectionRequests, FindCurvesMode findCurvesMode, int weatherGeohashPrecision, String mongoUri, String mongoUser, String mongoPw, String owmKey, boolean useAtlasDB, int cpuCheckInterval, boolean enableAuth) {
        this.nodeId = nodeId;
        this.loadBalanceRec = loadBalanceRec;
        this.loadBalanceDet = loadBalanceDet;
        this.defaultDetectionServerAddress = defaultDetectionServerAddress;
        this.simulateWeatherAPICalls = simulateWeatherAPICalls;
        this.sendDetectionRequests = sendDetectionRequests;
        this.findCurvesMode = findCurvesMode;
        this.weatherGeohashPrecision = weatherGeohashPrecision;
        this.mongoUri = mongoUri;
        this.mongoUser = mongoUser;
        this.mongoPw = mongoPw;
        this.owmApiKey = owmKey;
        this.useAtlasDB = useAtlasDB;
        this.cpuCheckInterval = cpuCheckInterval;
        this.enableAuth = enableAuth;
    }

    public ServerConfig readFromFile() throws IOException {
        Properties prop = new Properties();
        InputStream input = null;


        input = new FileInputStream("config.properties");

        // load a properties file
        prop.load(input);

        String nodeId = prop.getProperty("NODE_ID");
        boolean loadBalanceRec = Boolean.parseBoolean(prop.getProperty("LOAD_BALANCE_REC"));
        boolean loadBalanceDet = Boolean.parseBoolean(prop.getProperty("LOAD_BALANCE_DET"));
        String defaultDetectionServerAddress = prop.getProperty("DEFAULT_DETECTION_SERVER_ADDRESS");
        boolean simulateWeatherAPICalls = Boolean.parseBoolean(prop.getProperty("SIMULATE_WEATHER_API_CALLS"));
        boolean sendDetectionRequests = Boolean.parseBoolean(prop.getProperty("SEND_DETECTION_REQUESTS"));
        FindCurvesMode findCurvesMode = new FindCurvesMode(prop.getProperty("FIND_CURVES_BY_MODE"), Integer.parseInt(prop.getProperty("FIND_CURVES_BY_VALUE")));
        int weatherGeohashPrecsion = Integer.parseInt(prop.getProperty("WEATHER_GEOHASH_PRECISION"));
        String distributedMongoUri = prop.getProperty("DISTRIBUTED_MONGO_URI");
        String distributedMongoUser = prop.getProperty("DISTRIBUTED_MONGO_USER");
        String distributedMongoPw = prop.getProperty("DISTRIBUTED_MONGO_PW");
        String owmApiKey = prop.getProperty("OWM_API_KEY");
        boolean useAtlasDB = Boolean.parseBoolean(prop.getProperty("USE_ATLAS_DB"));
        boolean enableAuth = Boolean.parseBoolean(prop.getProperty("ENABLE_AUTH"));
        int cpuCheckIntervalSeconds = Integer.parseInt(prop.getProperty("CPU_CHECK_INTERVAL_SECONDS"));

        return new ServerConfig(
                nodeId,
                loadBalanceRec,
                loadBalanceDet,
                defaultDetectionServerAddress,
                simulateWeatherAPICalls,
                sendDetectionRequests,
                findCurvesMode,
                weatherGeohashPrecsion,
                distributedMongoUri,
                distributedMongoUser,
                distributedMongoPw,
                owmApiKey,
                useAtlasDB,
                cpuCheckIntervalSeconds,
                enableAuth
        );

    }

    public String getNodeId() {
        return nodeId;
    }

    public boolean isRecLoadBalancingEnabled() {
        return loadBalanceRec;
    }
    public boolean isDetLoadBalancingEnabled() {
        return loadBalanceDet;
    }


    public String getDefaultDetectionServerAddress() {
        return defaultDetectionServerAddress;
    }

    public boolean isSimulateWeatherAPICallsEnabled() {
        return simulateWeatherAPICalls;
    }

    public boolean isSendDetectionRequestsEnabled() {
        return sendDetectionRequests;
    }

    public FindCurvesMode getFindCurvesMode() {
        return findCurvesMode;
    }

    public int getWeatherGeohashPrecision() {
        return weatherGeohashPrecision;
    }

    public String getMongoUri() {
        return mongoUri;
    }

    public String getMongoUser() {
        return mongoUser;
    }

    public String getMongoPw() {
        return mongoPw;
    }

    public String getOwmApiKey() {
        return owmApiKey;
    }

    public boolean useAtlasDB() {
        return useAtlasDB;
    }

    public int getCpuCheckInterval() {
        return cpuCheckInterval;
    }

    public boolean isAuthEnabled() {
        return enableAuth;
    }

    @Override
    public String toString() {
        return "ServerConfig{" +
                "nodeId='" + nodeId + '\'' +
                ", loadBalanceRec=" + loadBalanceRec +
                ", loadBalanceDet=" + loadBalanceDet +
                ", defaultDetectionServerAddress='" + defaultDetectionServerAddress + '\'' +
                ", simulateWeatherAPICalls=" + simulateWeatherAPICalls +
                ", sendDetectionRequests=" + sendDetectionRequests +
                ", findCurvesMode=" + findCurvesMode +
                ", weatherGeohashPrecision=" + weatherGeohashPrecision +
                ", mongoUri='" + mongoUri + '\'' +
                ", mongoUser='" + mongoUser + '\'' +
                ", mongoPw='" + "<hidden>" + '\'' +
                ", owmAPIKey='" + "<hidden>" + '\'' +
                ", useAtlasDB='" + useAtlasDB + '\'' +
                ", enableAuth='" + enableAuth + '\'' +
                ", cpuCheckInterval='" + cpuCheckInterval + '\'' +
                '}';
    }
}
