package at.mkaran.thesis.commons.loadbalancer.model;

/**
 * POJO to store information about a service returned by Prometheus API
 */
public class ServiceInfo {
    private String nodeExServiceId;
    private String serviceName;
    private String serviceAddress;
    private int servicePort;
    private String nodeAddress;
    private ServiceMetrics metrics;
    private String nodeCapabilites;

    public ServiceInfo() {
    }

    public String getNodeExServiceId() {
        return nodeExServiceId;
    }

    public void setNodeExServiceId(String nodeExServiceId) {
        this.nodeExServiceId = nodeExServiceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceAddress() {
        return serviceAddress;
    }

    public void setServiceAddress(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    public int getServicePort() {
        return servicePort;
    }

    public void setServicePort(int servicePort) {
        this.servicePort = servicePort;
    }

    public String getNodeAddress() {
        return nodeAddress;
    }

    public void setNodeAddress(String nodeAddress) {
        this.nodeAddress = nodeAddress;
    }

    public ServiceMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(ServiceMetrics metrics) {
        this.metrics = metrics;
    }

    public String getNodeCapabilites() {
        return nodeCapabilites;
    }

    public void setNodeCapabilites(String nodeCapabilites) {
        this.nodeCapabilites = nodeCapabilites;
    }

    /**
     * Extracts the nodeID using the service names.
     * By configuration the Node Exporter (and all other) services always are named as following "node-exporter-<NODE-ID>".
     * The suffix <NODE-ID> is an arbitrary string.
     * @return The nodeId of this node
     */
    public String extractNodeId() {
        String searchString = "node-exporter-";
        if (!this.nodeExServiceId.contains(searchString)) {
            return null;
        } else {
            return this.nodeExServiceId.substring(this.nodeExServiceId.indexOf(searchString) + searchString.length(), this.nodeExServiceId.length());
        }
    }
}
