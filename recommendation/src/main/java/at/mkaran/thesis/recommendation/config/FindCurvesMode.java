package at.mkaran.thesis.recommendation.config;

/**
 * Configuration to specify how to retrieve curves from the (local and distributed) cache.
 */

public class FindCurvesMode {
    private FindModeEnum mode;
    private int value;

    public FindCurvesMode(String var, int value) {
        switch (var) {
            case "bb":
                mode = FindModeEnum.BY_BOUNDING_BOX;
                break;
            case "radius":
                mode = FindModeEnum.BY_RADIUS;
                break;
            case "geohash":
                mode = FindModeEnum.BY_GEOHASH;
                break;
            default:
                throw new IllegalArgumentException("Invalid FindCurvesMode: " + var);
        }
        this.value = value;
    }

    public FindModeEnum getMode() {
        return mode;
    }

    /**
     * Returns the value for a @link{at.mkaran.thesis.{@link FindModeEnum}}.
     * For Mode: "bb" the value represents the geohash precision of the surrounding boundinx. (The smaller the value, the larger the bounding box).
     * For Mode: "radius" the value represents the radius in meters to search for within a circle.
     * For Mode: "geohash" the value is ignored
     * @return
     */
    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "FindCurvesMode{" +
                "mode=" + mode.mode() +
                ", value=" + value +
                '}';
    }
}