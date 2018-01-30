package distributed.consul;

/**
 * Created by matthias on 09.11.17.
 */
public interface ConsulListener {
    void onServiceListUpdateSuccess(int numberOfAvailableServices);
    void onServiceListUpdateFailed();
}
