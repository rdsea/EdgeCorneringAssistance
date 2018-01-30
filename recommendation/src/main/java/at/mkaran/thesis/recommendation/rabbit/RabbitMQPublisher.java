package at.mkaran.thesis.recommendation.rabbit;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by matthias on 13.11.17.
 */
public class RabbitMQPublisher {
    private final static String EXCHANGE = "locationExchange";

    private ConcurrentHashMap<String,Channel> rabbitChannelPool;

    public RabbitMQPublisher() {
        this.rabbitChannelPool = new ConcurrentHashMap<>();
    }

    private Channel addRabbitChannel(String hostname) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(hostname);
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.FANOUT);
            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, EXCHANGE, "");
            rabbitChannelPool.put(hostname, channel);
            return channel;
        } catch (Exception e) {
            System.out.println("Could not add channel to pool: " + e.toString());
            return null;
        }
    }

    public void requestCurveDetection(String host, double lat, double lon) {
            try {
                Channel channel = null;
                if (rabbitChannelPool.containsKey(host)) {
                    channel = rabbitChannelPool.get(host);
                    sendTupleToRabbitServer(channel, lat, lon);
                } else {
                    channel = addRabbitChannel(host);
                    if (channel != null) {
                        sendTupleToRabbitServer(channel, lat, lon);
                    }
                }
            } catch (Exception e) {
                System.out.println("Could not send tuple to detection due to: " + e.toString());
            }

    }

    private void sendTupleToRabbitServer(Channel channel, double lat, double lon) {
        try {
            JSONObject json = new JSONObject();
            json.put("longitude", lon);
            json.put("latitude", lat);
            String message = json.toString();
            channel.basicPublish(EXCHANGE, "", null, message.getBytes("UTF-8"));
        } catch (Exception e) {
            System.out.println("Could not send tuple to detection due to: " + e.toString());
        }

    }

    public void shutdown() {

    }
}
