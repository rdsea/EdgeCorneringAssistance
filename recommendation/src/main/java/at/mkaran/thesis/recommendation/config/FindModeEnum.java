package at.mkaran.thesis.recommendation.config;

/**
 * Enum that specifies concrete modes to search for curves.
 */
public enum FindModeEnum {
    /**
     * Use a dynamic Bounding Box (size can be specified by geoHash character precision).
     * Note: This mode allows to search for areas of dynamic size, independent of the detection (or any other component)
     */
    BY_BOUNDING_BOX("bb"),
    /**
     * Use a radius (in meters).
     * Note: This mode allows to search for areas of dynamic size, independent of the detection (or any other component)
     */
    BY_RADIUS("radius"),
    /**
     * Use the fixed geohash that is stored to each curve (appendGeohashes must be enabled in detection service).
     * Note: This mode does not allow to specify a dynamic area to search for. The search area is fixed by the
     * geohash that was stored to a curve by the detection.
     */
    BY_GEOHASH("geohash");

    private final String mode;

    FindModeEnum(String mode) {
        this.mode = mode;
    }

    public String mode() {
        return this.mode;
    }
}
