package test;

/**
 * Created by matthias on 21.11.17.
 */
public class TestConfig {
    private int driverGeoHashPrecision;
    private int sendRecPollDelaySeconds;
    private int maxRecPolls;
    private String recServerAddress = null;
    private boolean enableLogging = false; // always set to false currently
    private int constantDriverSpeed;
    private boolean callRecommendationServer = true;
    private boolean addGPSInaccuracies = false;
    private boolean addGPSOutagesEnabled = false;

    public TestConfig() {
    }

    public boolean callRecommendationServer() {
        return callRecommendationServer;
    }

    public void setCallRecommendationServer(boolean callRecommendationServer) {
        this.callRecommendationServer = callRecommendationServer;
    }

    public int getDriverGeoHashPrecision() {
        return driverGeoHashPrecision;
    }

    public void setDriverGeoHashPrecision(int driverGeoHashPrecision) {
        this.driverGeoHashPrecision = driverGeoHashPrecision;
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

    public int getConstantDriverSpeed() {
        return constantDriverSpeed;
    }

    public void setConstantDriverSpeed(int constantDriverSpeed) {
        this.constantDriverSpeed = constantDriverSpeed;
    }

    public boolean isAddGPSInaccuraciesEnabled() {
        return addGPSInaccuracies;
    }

    public void enableGPSInaccuracies(boolean addGPSInaccuracies) {
        this.addGPSInaccuracies = addGPSInaccuracies;
    }

    public boolean isAddGPSOutagesEnabled() {
        return addGPSOutagesEnabled;
    }

    public void enableGPSOutages(boolean addGPSOutagesEnabled) {
        this.addGPSOutagesEnabled = addGPSOutagesEnabled;
    }


    @Override
    public String toString() {
        return "TestConfig{" +
                "driverGeoHashPrecision=" + driverGeoHashPrecision +
                ", sendRecPollDelaySeconds=" + sendRecPollDelaySeconds +
                ", maxRecPolls=" + maxRecPolls +
                ", recServerAddress='" + recServerAddress + '\'' +
                ", enableLogging=" + enableLogging +
                ", constantDriverSpeed=" + constantDriverSpeed +
                ", callRecommendationServer=" + callRecommendationServer +
                ", addGPSInaccuracies=" + addGPSInaccuracies +
                ", addGPSOutagesEnabled=" + addGPSOutagesEnabled +
                '}';
    }
}
