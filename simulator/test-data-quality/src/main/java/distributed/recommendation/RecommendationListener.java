package distributed.recommendation;

import at.mkaran.thesis.common.CurveRecommendationListDTO;
import at.mkaran.thesis.common.ResponseDTO;

/**
 * Listener to listen for new results from the recommendation service
 */
public interface RecommendationListener {
    void onResult(ResponseDTO result);
    void onError(Exception e);
}
