package at.mkaran.thesis.recommendation.model;

/**
 * Created by matthias on 08.09.17.
 */
public enum RoadCondition {
    DRY (0.85),
    WET (0.5),
    SNOW (0.25),
    GLAZE (0.1),
    UNDEFINIED (0.85);

    private final double friction;

    RoadCondition(double friction) {
        this.friction = friction;
    }

    public double friction() {
        return friction;
    }
}
