import at.mkaran.thesis.common.*;
import car.LocalMongoCurveCache;
import distributed.recommendation.RecommendationClient;
import distributed.recommendation.RecommendationListener;
import model.CachedCurve;
import model.Location;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests sending a sample request to a remote recommendation service
 */
public class TestRecServer {

    private RecommendationClient recommendationClient;

    private final String address = "<ADDRESS-OF-A-REC-SERVICE>";
    private final int port = 50051;

    @Before
    public void setup() {
        this.recommendationClient = new RecommendationClient(address, port);
    }

    @After
    public void tearDown() throws Exception {
        //curveCache.removeAll();
        recommendationClient.shutdown();
    }


    @Test
    public void testSendRequest() throws Exception {
        RequestDTO request = RequestDTO.newBuilder()
                .setLocation(PointDTO.newBuilder().setLat(48.151769).setLon(15.525705).build())
                .build();
        recommendationClient.setListener(new RecommendationListener() {
            @Override
            public void onResult(ResponseDTO result) {
                System.out.println("Response Code: " + result.getResponseCode());
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
        recommendationClient.callRequestRecommendation(request);
    }
}
