package car;

/**
 * Created by matthias on 21.11.17.
 */
public class GPSConfig {

    private boolean addInaccuracies;
    private Integer minErrorMeters;
    private Integer maxErrorMeters;

    private boolean addOutages;
    private Integer numOutages;
    private Integer outageDurationSeconds;


    public GPSConfig(boolean addInaccuracies, Integer minErrorMeters, Integer maxErrorMeters, boolean addOutages, Integer numOutages, Integer outageDurationSeconds) {
        this.addInaccuracies = addInaccuracies;
        this.minErrorMeters = minErrorMeters;
        this.maxErrorMeters = maxErrorMeters;
        this.addOutages = addOutages;
        this.numOutages = numOutages;
        this.outageDurationSeconds = outageDurationSeconds;
    }

    public boolean isAddInaccuraciesEnabled() {
        return this.addInaccuracies;
    }

    public boolean isAddOutagesEnabled() {
        return addOutages;
    }

    public Integer getNumOutages() {
        return numOutages;
    }

    public Integer getOutageDurationSeconds() {
        return outageDurationSeconds;
    }

    public int getMinErrorMeters() {
        return minErrorMeters;
    }

    public int getMaxErrorMeters() {
        return maxErrorMeters;
    }
}
