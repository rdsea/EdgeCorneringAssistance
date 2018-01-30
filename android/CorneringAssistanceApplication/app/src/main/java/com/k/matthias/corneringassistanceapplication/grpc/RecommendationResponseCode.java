package com.k.matthias.corneringassistanceapplication.grpc;

/**
 * Created by matthias on 26.01.18.
 */

public final class RecommendationResponseCode {
    public static final int CURVES_DETECTED = 0;
    public static final int DETECTION_SERVICE_BUSY = 1;
    public static final int RECOMMENDATION_SERVICE_BUSY = 2;
    public static final int ERROR = 3;
}
