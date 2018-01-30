package test;

/**
 * Created by matthias on 21.11.17.
 */
public class TestConfig {
    private int numberOfDrivers;
    private int driverGeoHashPrecision;
    private int refreshConsulPeriodSeconds;
    private int sendRecPollDelaySeconds;
    private int maxRecPolls;
    private int maxConsulRetries;
    private String recServerAddress = null;
    private boolean enableLogging = false; // default set to false currently
    private int grpcTimeout;
    private boolean enableLoadBalancing = false;

    public TestConfig() {
    }

    public static TestConfig getDefault() {
        TestConfig defaultConfig = new TestConfig();
        defaultConfig.setDriverGeoHashPrecision(6);
        defaultConfig.setMaxConsulRetries(3);
        defaultConfig.setMaxRecPolls(3);
        defaultConfig.setRefreshConsulPeriodSeconds(60 * 5); // 5minutes
        defaultConfig.setSendRecPollDelaySeconds(3);
        defaultConfig.setNumberOfDrivers(10);
        defaultConfig.setRecServerAddress(null);
        defaultConfig.setEnableLogging(false);
        defaultConfig.setGrpcTimeout(5);
        defaultConfig.setEnableLoadBalancing(false);
        return defaultConfig;
    }

    public int getNumberOfDrivers() {
        return numberOfDrivers;
    }

    public void setNumberOfDrivers(int numberOfDrivers) {
        this.numberOfDrivers = numberOfDrivers;
    }

    public int getDriverGeoHashPrecision() {
        return driverGeoHashPrecision;
    }

    public void setDriverGeoHashPrecision(int driverGeoHashPrecision) {
        this.driverGeoHashPrecision = driverGeoHashPrecision;
    }

    public int getRefreshConsulPeriodSeconds() {
        return refreshConsulPeriodSeconds;
    }

    public void setRefreshConsulPeriodSeconds(int refreshConsulPeriodSeconds) {
        this.refreshConsulPeriodSeconds = refreshConsulPeriodSeconds;
    }

    public int getSendRecPollDelaySeconds() {
        return sendRecPollDelaySeconds;
    }

    public void setSendRecPollDelaySeconds(int sendRecPollDelaySeconds) {
        this.sendRecPollDelaySeconds = sendRecPollDelaySeconds;
    }

    public int getMaxRecPolls() {
        return maxRecPolls;
    }

    public void setMaxRecPolls(int maxRecPolls) {
        this.maxRecPolls = maxRecPolls;
    }

    public int getMaxConsulRetries() {
        return maxConsulRetries;
    }

    public void setMaxConsulRetries(int maxConsulRetries) {
        this.maxConsulRetries = maxConsulRetries;
    }

    public String getRecServerAddress() {
        return recServerAddress;
    }

    public void setRecServerAddress(String recServerAddress) {
        if (recServerAddress.length() > 0) {
            this.recServerAddress = recServerAddress;
        } else {
            this.recServerAddress = null;
        }
    }

    public boolean isLoggingEnabled() {
        return enableLogging;
    }

    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
    }

    @Override
    public String toString() {
        return "TestConfig{" +
                "numberOfDrivers=" + numberOfDrivers +
                ", driverGeoHashPrecision=" + driverGeoHashPrecision +
                ", refreshConsulPeriodSeconds=" + refreshConsulPeriodSeconds +
                ", sendRecPollDelaySeconds=" + sendRecPollDelaySeconds +
                ", maxRecPolls=" + maxRecPolls +
                ", maxConsulRetries=" + maxConsulRetries +
                ", recServerAddress='" + recServerAddress + '\'' +
                ", enableLB='" + enableLoadBalancing + '\'' +
                ", enableLogging=" + enableLogging +
                '}';
    }

    public void setGrpcTimeout(int grpcTimeout) {
        this.grpcTimeout = grpcTimeout;
    }

    public int getGrpcTimeout() {
        return grpcTimeout;
    }

    public void setEnableLoadBalancing(boolean enableLoadBalancing) {
        this.enableLoadBalancing = enableLoadBalancing;
    }

    public boolean isLoadBalancingEnabled() {
        return enableLoadBalancing;
    }
}
