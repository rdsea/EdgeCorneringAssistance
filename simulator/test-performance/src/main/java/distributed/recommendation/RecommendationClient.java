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
    private String host;
    private int port;
    private RecommendationListener listener = null;
    private int timeout;

    /** Construct client connecting to HelloWorld server at {@code host:port}. */
    public RecommendationClient(String host, int port, int grpcTimeout) {
        this(ManagedChannelBuilder.forAddress(host, port)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext(true));
        this.timeout = grpcTimeout;
        this.host = host;
        this.port = port;
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
        channel.shutdown().shutdownNow();
    }

    public void callRequestRecommendation(RequestDTO request) {
        try {
            ResponseDTO result = blockingStub.withDeadlineAfter(timeout, TimeUnit.SECONDS).requestRecommendation(request);
            if (listener != null) {
                this.listener.onResult(result);
            }
        } catch (Exception e) {
            this.listener.onGrpcError(e);
        }
    }

    public void callPollDatabase(RequestDTO requestDTO) {
        try {
            ResponseDTO result = blockingStub.withDeadlineAfter(timeout, TimeUnit.SECONDS).pollDatabase(requestDTO);
            if (listener != null) {
                this.listener.onResult(result);
            }
        } catch (Exception e) {
            this.listener.onGrpcError(e);
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
