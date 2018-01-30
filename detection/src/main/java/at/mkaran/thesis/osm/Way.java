package at.mkaran.thesis.osm;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;

/**
 * Created by matthias on 30.05.17.
 */
public class Way extends Entity{
    private String name;
    private String highway;
    private String maxspeed;
    private JSONArray nodeIds;
    private ArrayList<Node> nodes;
    private String boundingGeohash = null;

    public Way() {
        super();
    }

    public Way(long id) {
        super(id);
    }

    public String getName() {
        return name;
    }

    private void setName(String name) {
        this.name = name;
    }

    public String getHighway() {
        return highway;
    }

    private void setHighway(String highway) {
        this.highway = highway;
    }

    public String getMaxspeed() {
        return maxspeed;
    }

    private void setMaxspeed(String maxspeed) {
        this.maxspeed = maxspeed;
    }

    private void setNodeIds(JSONArray nodeIds) {
        this.nodeIds = nodeIds;
    }

    public JSONArray getNodeIds() {
        return nodeIds;
    }

    public ArrayList<Node> getNodes() {
        return nodes;
    }

    public void setNodes(ArrayList<Node> nodes) {
        this.nodes = nodes;
    }

    public void setBoundingGeohash(String boundingGeohash) {
        this.boundingGeohash = boundingGeohash;
    }

    public String getBoundingGeohash() {
        return boundingGeohash;
    }

    @Override
    public void parseEntity(JSONObject json) {
        setNodeIds((JSONArray) json.get("nodes"));
        JSONObject tags = (JSONObject) json.get("tags");
        if (tags != null) {
            setName((String) tags.get("name"));
            setHighway((String) tags.get("highway"));
            setMaxspeed((String) tags.get("maxspeed"));
        }
    }

    @Override
    public String toString() {
        String string =  getName() + "\n" + getHighway() + "\n";
        for (Node node: nodes) {
            string += node.toString() + "\n";
        }
        return string;
    }
}
