package webserver;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public final class CamelRoute extends RouteBuilder {

    @Override
    public final void configure() throws Exception {
        from("rabbitmq:localhost/simulationExchange?autoDelete=false&durable=false&exchangeType=fanout")
                //.to("log:org.apache.camel.example?level=INFO")
                .to("websocket://dashboard?sendToAll=true");
    }
}
