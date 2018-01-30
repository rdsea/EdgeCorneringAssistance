package webserver;

import at.mkaran.thesis.common.CurveRecommendationDTO;
import ch.hsr.geohash.BoundingBox;
import ch.hsr.geohash.GeoHash;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import model.CachedCurve;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import util.Log;

import java.util.List;

public class RabbitMQPublisher {
    private final static String EXCHANGE = "simulationExchange";
    private static Connection connection;
    private static Channel channel = null;

    public RabbitMQPublisher() {
    }

    private static void init() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.FANOUT);
            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, EXCHANGE, "");
        } catch (Exception e) {
            System.out.println("Init RabbitMQ failed");
        }
    }

    public static void sendLocationUpdate(double lat, double lon) {
        if (channel == null) {
            init();
        }
        try {
            JSONObject json = new JSONObject();
            json.put("event", "location");
            json.put("lon", lon);
            json.put("lat", lat);
            String message = json.toString();
            channel.basicPublish(EXCHANGE, "", null, message.getBytes("UTF-8"));
            Log.info("(x) sent message: " + json.toJSONString() + " to rabbitMQ");
        } catch (Exception e) {
            Log.error("Could not send tuple to detection due to: " + e.toString());
        }

    }

    public static void sendBoundingBox(double lat, double lon, int geoHashPrecision) {
        if (channel == null) {
            init();
        }
        BoundingBox bb = GeoHash.withCharacterPrecision(lat, lon, geoHashPrecision).getBoundingBox();
        try {
            JSONObject json = new JSONObject();
            json.put("event", "bb");
            json.put("minLat", bb.getMinLat());
            json.put("maxLat", bb.getMaxLat());
            json.put("minLon", bb.getMinLon());
            json.put("maxLon", bb.getMaxLon());
            String message = json.toString();
            channel.basicPublish(EXCHANGE, "", null, message.getBytes("UTF-8"));
            Log.info("(x) sent message: " + json.toJSONString() + " to rabbitMQ");
        } catch (Exception e) {
            Log.error("Could not send tuple to detection due to: " + e.toString());
        }

    }

    public static void sendCurve(CachedCurve curve) {
        if (channel == null) {
            init();
        }
        try {
            JSONObject json = new JSONObject();
            json.put("event", "curve");
            json.put("lat1", curve.getStartPoint().getLatitude());
            json.put("lat2", curve.getCenterPoint().getLatitude());
            json.put("lat3", curve.getEndPoint().getLatitude());
            json.put("lon1", curve.getStartPoint().getLongitude());
            json.put("lon2", curve.getCenterPoint().getLongitude());
            json.put("lon3", curve.getEndPoint().getLongitude());
            json.put("radius", curve.getRadius());
            json.put("speed", curve.getRecommendedSpeed());
            String message = json.toString();
            channel.basicPublish(EXCHANGE, "", null, message.getBytes("UTF-8"));
            Log.info("(x) sent message: " + json.toJSONString() + " to rabbitMQ");
        } catch (Exception e) {
            Log.error("Could not send tuple to detection due to: " + e.toString());
        }
    }

    public static void sendCurves(List<CurveRecommendationDTO> curves) {
        if (channel == null) {
            init();
        }
        try {
            JSONObject json = new JSONObject();
            json.put("event", "curves");
            JSONArray jsonArray = new JSONArray();
            for (CurveRecommendationDTO curve : curves) {
                JSONObject curveJson = new JSONObject();
                curveJson.put("lat1", curve.getCurve().getStart().getLat());
                curveJson.put("lat2", curve.getCurve().getCenter().getLat());
                curveJson.put("lat3", curve.getCurve().getEnd().getLat());
                curveJson.put("lon1", curve.getCurve().getStart().getLon());
                curveJson.put("lon2", curve.getCurve().getCenter().getLon());
                curveJson.put("lon3", curve.getCurve().getEnd().getLon());
                curveJson.put("radius", curve.getCurve().getRadius());
                curveJson.put("speed", curve.getRecommendedSpeed());
                jsonArray.add(curveJson);
            }
            json.put("curves", jsonArray);
            String message = json.toString();
            channel.basicPublish(EXCHANGE, "", null, message.getBytes("UTF-8"));
            Log.info("(x) sent message: " + json.toJSONString() + " to rabbitMQ");
        } catch (Exception e) {
            Log.error("Could not send tuple to detection due to: " + e.toString());
        }
    }

    public static void shutdown() {
        try {
            channel.close();
            connection.close();
            channel = null;
        } catch (Exception e) {
            Log.error(e.toString());
        }

    }

    public static void sendStatus(String status) {
        if (channel == null) {
            init();
        }
        try {
            JSONObject json = new JSONObject();
            json.put("event", "status");
            json.put("status", status);
            String message = json.toString();
            channel.basicPublish(EXCHANGE, "", null, message.getBytes("UTF-8"));
            Log.info("(x) sent message: " + json.toJSONString() + " to rabbitMQ");
        } catch (Exception e) {
            Log.error("Could not send tuple to detection due to: " + e.toString());
        }
    }

    public static void sendOutagePosition(double lat, double lon) {
        if (channel == null) {
            init();
        }
        try {
            JSONObject json = new JSONObject();
            json.put("event", "outage");
            json.put("lon", lon);
            json.put("lat", lat);
            String message = json.toString();
            channel.basicPublish(EXCHANGE, "", null, message.getBytes("UTF-8"));
            Log.info("(x) sent message: " + json.toJSONString() + " to rabbitMQ");
        } catch (Exception e) {
            Log.error("Could not send tuple to detection due to: " + e.toString());
        }
    }
}
