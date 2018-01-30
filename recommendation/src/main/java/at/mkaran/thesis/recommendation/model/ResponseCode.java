package at.mkaran.thesis.recommendation.model;

/**
 * Response Code of recommendation service that is transmitted to clients
 */
public enum ResponseCode {
    CURVES_AVAILABLE (0),
    DETECTION_IN_PROGRESS (1),
    RECOMMENDATION_BUSY (2),
    RECOMMENDATION_ERROR (3);

    private final int code;

    ResponseCode(int code) {
        this.code = code;
    }

    public int code() {
        return this.code;
    }
}