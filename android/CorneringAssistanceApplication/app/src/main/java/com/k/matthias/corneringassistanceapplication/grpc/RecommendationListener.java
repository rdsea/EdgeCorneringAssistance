package com.k.matthias.corneringassistanceapplication.grpc;

/**
 * Listener to listen for new results from the recommendation service
 */
public interface RecommendationListener {
    void onRecommendationResult();
}
