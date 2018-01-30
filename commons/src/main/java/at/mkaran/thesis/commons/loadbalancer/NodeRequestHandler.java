package at.mkaran.thesis.commons.loadbalancer;

import at.mkaran.thesis.commons.http.HttpHelper;
import at.mkaran.thesis.commons.loadbalancer.model.ServiceInfo;
import at.mkaran.thesis.commons.loadbalancer.model.ServiceMetrics;
import at.mkaran.thesis.commons.loadbalancer.model.ServiceType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles incoming requests on Nodes.
 * In case a Node can't handle a request itself, it is possible to choose another Node that is capable of handling the request
 */
public class NodeRequestHandler {
    private final JSONParser jsonParser;
    private String nodeId;
    private static final String NODE_EXPORTER_SERVICE_ID_PREFIX = "node-exporter-";
    private static final String NODE_EXPORTER_SERVICE_NAME = "node-exporter";
    private static final String PROMETHEUS_SERVICE_ADDRESS = "http://prometheus-%s:9090/";
    private static final String PROMETHEUS_QUERY_API_URL = "api/v1/query?query=";
    private static final String PROMETHEUS_QUERY_CURRENT_NODE = "100-(avg by (service_id,service_name,service_address, service_port, node_address) (rate(node_cpu{mode=\"idle\",service_id=\"%s\"}[1m])) * 100)";
    private static final String PROMETHEUS_QUERY_ALL_NODES = "100-(avg by (node_capabilities,service_id,service_name,service_address, service_port, node_address) (rate(node_cpu{mode=\"idle\",service_name=\"%s\"}[1m])) * 100)";
    private static final double CPU_USAGE_MAX_THRESHOLD = 70.0; // A node's maximum cpu usage (percentage) to accept new requests

    /**
     * Initializes the Handler for the given serviceType
     * @param nodeId The nodeId for the node where this service executes on
     */
    public NodeRequestHandler(String nodeId) {
        this.nodeId = nodeId;
        this.jsonParser = new JSONParser();
    }

    /**
     * Decide whether a Node can handle an incoming request on a service by evaluating metrics provided by Prometheus.
     * (Prometheus itself scrapes Node-Exporter that is running on the node).
     * @return true if request successful and node can handle the request, false otherwise
     */
    public boolean canNodeHandleRequest() {
        System.out.println("-------LOAD BALANCER-------");
        System.out.println("Checking current node: " + this.nodeId);
        try {
            String query = URLEncoder.encode(String.format(PROMETHEUS_QUERY_CURRENT_NODE, NODE_EXPORTER_SERVICE_ID_PREFIX + this.nodeId), "UTF-8");
            String url = String.format(PROMETHEUS_SERVICE_ADDRESS, this.nodeId) + PROMETHEUS_QUERY_API_URL + query;
            System.out.println("Sending HTTP Request to: " + url);

            JSONObject jsonResponse = callPrometheus(url);
            if (jsonResponse != null) {
                System.out.println("Request to Prometheus successful, result: " + jsonResponse.toJSONString());

                List<ServiceInfo> serviceInfos = parseJSON(jsonResponse);
                if (serviceInfos.size() > 0) {
                    ServiceMetrics metrics = serviceInfos.get(0).getMetrics();
                    System.out.println(serviceInfos.get(0).getNodeExServiceId() + "\'s CPU usage is: " + metrics.getCpuUsage());
                    if (metrics.getCpuUsage() <= CPU_USAGE_MAX_THRESHOLD) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }

        } catch (UnsupportedEncodingException e) {
            System.out.println("Request to Prometheus failed");
            return false;
        }
        return false;
    }

    /**
     * From all available nodes (except the node we are currently on) that run the specified service type, the one with the lowest cpu usage is returned.
     * @return The least busy node. NULL If the request failed or no node was found.
     * @param serviceType The service type to query nodes for
     * @return
     */
    public ServiceInfo findLeastBusyNode(ServiceType serviceType) {
        System.out.println("-------LOAD BALANCER-------");

        String query = null;
        try {
            query = URLEncoder.encode(String.format(PROMETHEUS_QUERY_ALL_NODES, NODE_EXPORTER_SERVICE_NAME), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }

        JSONObject jsonResponse = callPrometheus(String.format(PROMETHEUS_SERVICE_ADDRESS, this.nodeId) + PROMETHEUS_QUERY_API_URL + query);

        double currentMinCpuUsageValue = Double.MAX_VALUE;
        ServiceInfo currentLeastBusy = null;

        if (jsonResponse != null) {
            System.out.println("Request to Prometheus successful: " + jsonResponse.toJSONString());

            String currentNodeExporterId = NODE_EXPORTER_SERVICE_ID_PREFIX + this.nodeId;
            List<ServiceInfo> serviceInfos = parseJSON(jsonResponse);
            for (ServiceInfo serviceInfo : serviceInfos) {
                if (serviceInfo.getNodeExServiceId().equals(currentNodeExporterId)) {
                    // skip the node we are currently on
                    continue;
                }
                if (!serviceInfo.getNodeCapabilites().toLowerCase().contains(serviceType.name().toLowerCase())) {
                    // skip nodes that are not capable of running the specified serviceType
                    continue;
                }
                if (serviceInfo.getMetrics().getCpuUsage() < currentMinCpuUsageValue) {
                    currentMinCpuUsageValue = serviceInfo.getMetrics().getCpuUsage();
                    currentLeastBusy = serviceInfo;
                }
            }
        }
        if (currentLeastBusy != null) {
            System.out.println("Least busy node is: " + currentLeastBusy.extractNodeId());
        } else {
            System.out.println("Least busy node is: null");
        }

        return currentLeastBusy;
    }

    private JSONObject callPrometheus(String url) {
        try {
            String json = HttpHelper.okHttp(url);
            return (JSONObject) jsonParser.parse(json);
        } catch (Exception e) {
            return null;
        }
    }


    private List<ServiceInfo> parseJSON(JSONObject jsonObject) {
        List<ServiceInfo> serviceInfos = new ArrayList<>();
        JSONObject data = (JSONObject) jsonObject.get("data");
        JSONArray results = (JSONArray) data.get("result");
        int nodeCount = results.size();
        for (int i=0; i < nodeCount; i++) {
            JSONObject resultElement = (JSONObject) results.get(i);
            JSONObject metric = (JSONObject) resultElement.get("metric");
            ServiceInfo serviceInfo = new ServiceInfo();
            serviceInfo.setNodeAddress((String) metric.get("node_address"));
            serviceInfo.setNodeExServiceId((String) metric.get("service_id"));
            serviceInfo.setServiceAddress((String) metric.get("service_address"));
            serviceInfo.setServiceName((String) metric.get("service_name"));
            serviceInfo.setServicePort(Integer.valueOf((String) metric.get("service_port")));
            serviceInfo.setNodeCapabilites((String) metric.get("node_capabilities"));
            ServiceMetrics serviceMetrics = new ServiceMetrics();
            JSONArray value = (JSONArray) resultElement.get("value");
            serviceMetrics.setCpuUsage(Double.valueOf((String) value.get(1)));
            serviceInfo.setMetrics(serviceMetrics);
            serviceInfos.add(serviceInfo);
        }
        return serviceInfos;
    }

}
