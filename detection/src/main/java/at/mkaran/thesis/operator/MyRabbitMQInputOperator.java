package at.mkaran.thesis.operator;

import at.mkaran.thesis.model.Request;
import com.datatorrent.contrib.rabbitmq.AbstractSinglePortRabbitMQInputOperator;
import com.rabbitmq.client.BuiltinExchangeType;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Starting point for the Apex Pipeline
 * Reads Requests from a RabbitMQ Queue
 */
public class MyRabbitMQInputOperator extends AbstractSinglePortRabbitMQInputOperator<Request> {

    private static final Logger LOG = LoggerFactory.getLogger(MyRabbitMQInputOperator.class);


    public MyRabbitMQInputOperator() {
        super();
    }

    public void initRabbitServer(String rabbitServerHostName, int rabbitServerPort) {
        setHost(rabbitServerHostName);
        setPort(rabbitServerPort);
        setExchange("locationExchange");
        setExchangeType(BuiltinExchangeType.FANOUT.getType());
    }


    @Override
    public Request getTuple(byte[] message) {
        String msg = new String(message);

        JSONParser parser = new JSONParser();
        try {
            JSONObject jsonObject = (JSONObject) parser.parse(msg);

            double lat = (Double) jsonObject.get("latitude");
            double lon = (Double) jsonObject.get("longitude");

            LOG.info("Received Request from Rabbit: " + new Request(lat, lon).toString());
            return new Request(lat, lon);

        } catch (ParseException e) {
            e.printStackTrace();
            return new Request( null, null);
        }
    }
}
