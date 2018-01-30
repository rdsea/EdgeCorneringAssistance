package at.mkaran.thesis.commons.rabbit;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.json.simple.JSONObject;

/**
 * Created by matthias on 12.09.17.
 */
public class RabbitMQHelper {

    private final static String EXCHANGE = "locationExchange";


    @SuppressWarnings("unchecked")
    public static void requestCurveDetection(String host, double lat, double lon) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.FANOUT);

            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, EXCHANGE, "");

            JSONObject json = new JSONObject();
            json.put("longitude", lon);
            json.put("latitude", lat);
            String message = json.toString();
            channel.basicPublish(EXCHANGE, "", null, message.getBytes("UTF-8"));
        } catch (Exception e) {
            System.out.println("Could not request Curve Detection due to: " + e.toString());
        }

    }
}
