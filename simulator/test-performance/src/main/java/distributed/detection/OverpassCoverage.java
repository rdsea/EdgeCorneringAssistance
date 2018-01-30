package distributed.detection;

/**
 * Represents the exact area of coverage of the installed OverpassAPI instance.
 * For the thesis, the area spans "Graz Umgebung"
 */


public final class OverpassCoverage {
    private static final double COVERED_BB_SOUTH = 46.642;
    private static final double COVERED_BB_WEST = 14.58;
    private static final double COVERED_BB_NORTH = 47.484;
    private static final double COVERED_BB_EAST = 16.204;

    /**
     * Checks weather or not a Point lies within the Bounding Box
     * @param lat
     * @param lon
     * @return
     */
    public static boolean isCarWithinOverpassCoverage(double lat, double lon) {
        return (lat >= COVERED_BB_SOUTH && lat <= COVERED_BB_NORTH && lon <= COVERED_BB_EAST && lon >= COVERED_BB_WEST);
    }
}
