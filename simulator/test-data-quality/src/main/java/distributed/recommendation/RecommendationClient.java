package distributed.recommendation;

import at.mkaran.thesis.common.RequestDTO;
import at.mkaran.thesis.common.ResponseDTO;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Client that calls gRPC server methods
 */
public class RecommendationClient {
    private static final Logger logger = Logger.getLogger(RecommendationClient.class.getName());

    private final ManagedChannel channel;
    private final RecommendationGrpc.RecommendationBlockingStub blockingStub;
    private RecommendationListener listener = null;

    /** Construct client connecting to HelloWorld server at {@code host:port}. */
    public RecommendationClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext(true));
    }

    public void setListener(RecommendationListener listener) {
        this.listener = listener;
    }

    /** Construct client for accessing the recommendation server using the existing channel. */
    RecommendationClient(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        blockingStub = RecommendationGrpc.newBlockingStub(channel);

    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void callRequestRecommendation(RequestDTO request) {
        try {
            ResponseDTO result = blockingStub.requestRecommendation(request);
            if (listener != null) {
                this.listener.onResult(result);
            }
        } catch (Exception e) {
            this.listener.onError(e);
        }
    }

    public void callPollDatabase(RequestDTO requestDTO) {
        try {
            ResponseDTO result = blockingStub.pollDatabase(requestDTO);
            if (listener != null) {
                this.listener.onResult(result);
            }
        } catch (Exception e) {
            this.listener.onError(e);
        }
    }

}
