package at.mkaran.thesis.operator;

import at.mkaran.thesis.osm.Node;
import at.mkaran.thesis.osm.Way;
import ch.hsr.geohash.BoundingBox;
import ch.hsr.geohash.GeoHash;
import com.datatorrent.api.Context;
import com.datatorrent.api.DefaultInputPort;
import com.datatorrent.api.DefaultOutputPort;
import com.datatorrent.common.util.BaseOperator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Query Ways that lie within the bounding box of the incoming tuple (geohashes)
 */
public class OSMQueryOperator extends BaseOperator {

    private static final Logger LOG = LoggerFactory.getLogger(OSMQueryOperator.class);


    private static final String OVERPASS_BASE_URL = "/api/interpreter?data=";
    private static final String QUERY_OUT_FORMAT = "[out:json]";
    private static final String QUERY_NODE_FILTER = "foreach(\n" +
            "  out;\n" +
            "  node(w);\n" +
            "  out;\n" +
            ");";

    private String overpassUrl;
    private boolean simulateOSMQueries;
    private String overpassQueryWayFilter;

    public void setOverpassUrl(String overpassUrl) {
        this.overpassUrl = overpassUrl;
    }

    public void setSimulateOSMQueries(boolean simulateOSMQueries) {
        this.simulateOSMQueries = simulateOSMQueries;
    }

    public void setOverpassQueryWayFilter(String overpassQueryWayFilter) {
        this.overpassQueryWayFilter = overpassQueryWayFilter;
    }

    @Override
    public void setup(Context.OperatorContext context) {
        super.setup(context);
    }

    /**
     * Input port on which aggregated requests are received
     */
    public final transient DefaultInputPort<String> inputPort = new DefaultInputPort<String>() {
        @Override
        public void process(String geoHash) {
            GeoHash geoHashObject = GeoHash.fromGeohashString(geoHash);
            BoundingBox bb = geoHashObject.getBoundingBox();

            LOG.info(" sending OverpassAPI request for BB of: " + geoHash);
            try {
                JSONObject overpassResponse = getWaysInBoundingBox(bb);
                if (overpassResponse != null) {
                    List<Way> ways = getWaysFromResponse(overpassResponse);
                    LOG.info(" received " + ways.size() + " ways from OverpassAPI");

                    for (Way way : ways) {
                        way.setBoundingGeohash(geoHash);
                        outputPort.emit(way);
                    }
                    LOG.info(" emitted " + ways.size() + " ways");

                } else {
                    LOG.warn(" OverpassAPI response is NULL");
                }
            } catch (Exception e) {
                LOG.warn(" OverpassAPI request failed. Reason: " + e.toString());
            }

        }

    };

    /**
     * Output port which emits ways within the given geoHash
     */
    public final transient DefaultOutputPort<Way> outputPort = new DefaultOutputPort<>();

    /**
     * Returns an Overpass Query that fetches all Nodes and Ways within a bounding box around the given Location in lat/lon
     *
     * @param bb BoundingBox
     * @return
     */
    private JSONObject getWaysInBoundingBox(BoundingBox bb) throws Exception {
        String query = getQuery(bb);
        if (this.simulateOSMQueries) {
            return getWaysFromSampleResponses();
        }

        query = this.overpassUrl + OVERPASS_BASE_URL + URLEncoder.encode(query, "UTF-8");
        System.out.println(query);
        JSONParser jsonParser = new JSONParser();
        return  (JSONObject) jsonParser.parse(readUrl(query));
    }

    private String getQuery(BoundingBox bb) {
        String bbOverpassString = "[bbox:" + bb.getMinLat() + "," + bb.getMinLon() + "," + bb.getMaxLat() + ", " + bb.getMaxLon() + "];";
        return QUERY_OUT_FORMAT + bbOverpassString + overpassQueryWayFilter + QUERY_NODE_FILTER;
    }

    public String getQueryTest(double lat, double lon, int precision) throws UnsupportedEncodingException {
        String geohash = GeoHash.withCharacterPrecision(lat, lon, precision).toBase32();
        GeoHash geoHashObject = GeoHash.fromGeohashString(geohash);
        BoundingBox bb = geoHashObject.getBoundingBox();
        String query = this.overpassUrl + OVERPASS_BASE_URL + URLEncoder.encode(getQuery(bb), "UTF-8");
        return query;
    }

    /**
     * Download File from URL (HTTP)
     *
     * @param urlString
     * @return
     * @throws Exception
     */
    private String readUrl(String urlString) throws Exception {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            return buffer.toString();
        } finally {
            if (reader != null)
                reader.close();
        }
    }

    private List<Way> getWaysFromResponse(JSONObject response) {
        ArrayList<Way> ways = new ArrayList<>();
        HashMap<Long, Node> nodes = new HashMap<>();

        JSONArray elements = (JSONArray) response.get("elements");
        for (int i = 0; i < elements.size(); i++) {
            JSONObject element = (JSONObject) elements.get(i);
            long id = Long.parseLong(element.get("id").toString());
            String type = element.get("type").toString();
            if (type.equals("way")) {
                Way way = new Way(id);
                way.parseEntity(element);
                ways.add(way);
            } else if (type.equals("node")) {
                Node node = new Node(id);
                node.parseEntity(element);
                nodes.put(node.getId(), node);
            }
        }

        for (Way way : ways) {
            ArrayList<Node> wayNodes = new ArrayList<>();
            JSONArray nodeIds = way.getNodeIds();
            for (int i = 0; i < nodeIds.size(); i++) {
                Long nodeId = Long.parseLong(nodeIds.get(i).toString());
                wayNodes.add(nodes.get(nodeId));
            }
            way.setNodes(wayNodes);
        }

        return ways;
    }

    private JSONObject getWaysFromSampleResponses() {
        String[] filenames = {"u26s59.json", "u27ju5.json", "u27n40.json"};
        int randomNum = ThreadLocalRandom.current().nextInt(0, 2 + 1);
        String file = filenames[randomNum];
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader("src/main/resources/overpass_examples/" + file));
            return (JSONObject) obj;

        } catch (Exception e) {
            return null;
        }
    }

}
