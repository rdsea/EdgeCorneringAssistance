package distributed.consul;

import at.mkaran.thesis.commons.http.HttpHelper;
import model.Location;
import model.Node;
import model.Service;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import test.PerformanceTestExecutor;
import util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Client to handle queries to service-registry.
 */
public class Consul {
    private final static String CONSUL_SERVER_ADDRESS = "http://consul-server:8500/";
    private final static String RECOMMENDATION_SERVICE_QUERY = "v1/catalog/service/recommendation";
    private final JSONParser jsonParser;
    private final int retryMax;

    private int retryRequests;

    private List<Service> availableServices = new ArrayList<>();
    private ConsulListener listener;
    private Timer periodalServiceUpdateTimer;

    public Consul(ConsulListener listener, int refreshServiceListIntervalSeconds, int retryMax) {
        this.listener = listener;
        this.jsonParser = new JSONParser();
        this.retryMax = retryMax;
        this.retryRequests = retryMax;
        initPeriodicalServiceUpdate(refreshServiceListIntervalSeconds * 1000);
    }

    public int getNumberOfAvailableRecServices() {
        return availableServices.size();
    }

    /**
     * Schedules a task to update the service list for the specified update interval time.
     */
    private void initPeriodicalServiceUpdate(long refreshServiceListInterval) {
        final TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                sendConsulQuery(false);
            }
        };
        this.periodalServiceUpdateTimer = new Timer();
        this.periodalServiceUpdateTimer.schedule(timerTask, 0,refreshServiceListInterval);
    }

    private void sendConsulQuery(boolean retry) {
        PerformanceTestExecutor.CONSUL_REQUESTS_SENT.inc();
        if (retry) {
            PerformanceTestExecutor.CONSUL_REQUESTS_RETRIED.inc();
        }
        try {
            String json = HttpHelper.okHttp(CONSUL_SERVER_ADDRESS + RECOMMENDATION_SERVICE_QUERY);
            JSONArray jsonArray = (JSONArray) this.jsonParser.parse(json);

            if (jsonArray != null) {
                consulQuerySuccess(jsonArray);
            } else {
                consulQueryFailed();
            }
        } catch (Exception e) {
            consulQueryFailed();
        }
    }

    private void consulQuerySuccess(JSONArray result) {
        PerformanceTestExecutor.CONSUL_RESPONSES_SUCCESS.inc();
        this.availableServices = parseJSONToServiceList(result);
        this.retryRequests = this.retryMax;
        this.listener.onServiceListUpdateSuccess(this.availableServices.size());
    }

    private void consulQueryFailed() {
        PerformanceTestExecutor.CONSUL_RESPONSES_FAILED.inc();
        this.retryRequests--;
        if (this.retryRequests > 0) {
            // retry
            sendConsulQuery(true);
        } else {
            PerformanceTestExecutor.CONSUL_RESPONSES_TIMEOUT.inc();
            this.listener.onServiceListUpdateFailed();
        }
    }


    /**
     * Determines the closest Service (Location of the node that hosts the service) from a clients location.
     * In case a service was marked busy, it is not considered.
     * @param lat Client's latitude
     * @param lon Client's longitude
     * @return
     */
    public Service findClosestService(double lat, double lon) {
        Location clientLocation = new Location(lat, lon);
        Double minDistance = null;
        Service closestService = null;

        for (Service service : availableServices) {
            if (service.isBusy()) {
                continue;
            }
            double currentDistance = calcDistance(clientLocation, service.getNode().getLocation());
            if (minDistance == null) {
                minDistance = currentDistance;
                closestService = service;
            } else {
                if (currentDistance < minDistance) {
                    minDistance = currentDistance;
                    closestService = service;
                }
            }
        }

        return closestService;
    }


    private List<Service> parseJSONToServiceList(JSONArray jsonArray) {
        List<Service> services = new ArrayList<>();
        int nodeCount = jsonArray.size();
        for (int i=0; i < nodeCount; i++) {
            JSONObject element = (JSONObject) jsonArray.get(i);
            Node node = new Node();
            node.setId((String) element.get("ID"));
            node.setName((String) element.get("Node"));
            node.setAddress((String) element.get("Address"));
            JSONObject nodeMeta = (JSONObject) element.get("NodeMeta");
            String lat = (String) nodeMeta.get("lat");
            String lon = (String) nodeMeta.get("lon");
            Location location = new Location(Double.parseDouble(lat), Double.parseDouble(lon));
            node.setLocation(location);

            Service service = new Service();
            service.setId((String) element.get("ServiceID"));
            service.setName((String) element.get("ServiceName"));
            service.setAddress((String) element.get("ServiceAddress"));
            Long port = (Long) element.get("ServicePort");
            service.setPort(port.intValue());
            service.setNode(node);

            services.add(service);
        }
        return services;
    }


    /**
     * Calculate the distance between two points using the "haversine" formula.
     * This code was taken from http://www.movable-type.co.uk/scripts/latlong.html.
     *
     * @param start The starting point
     * @param end The end point
     * @return The distance between the points in meters
     */
    private int calcDistance(Location start, Location end) {
        double lat1 = start.getLatitude();
        double lat2 = end.getLatitude();
        double lon1 = start.getLongitude();
        double lon2 = end.getLongitude();
        int r = 6371000; // meters
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (int) (r * c);
    }

    public void shutdown() {
        if (this.periodalServiceUpdateTimer != null) {
            this.periodalServiceUpdateTimer.cancel();
        }
    }
}


