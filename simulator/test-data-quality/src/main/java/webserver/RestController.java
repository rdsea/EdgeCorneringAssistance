package webserver;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import test.DataQualityTestExectuor;
import test.TestConfig;

import java.io.IOException;
import java.io.Writer;


@Controller
public class RestController {

    @RequestMapping(path = "/startSimulation")
    public @ResponseBody RestResponse startSimulation(
            @RequestParam("driverSpeed") int driverSpeed,
            @RequestParam("driverGeoHashPrecision") int driverGeoHashPrecision,
            @RequestParam("sendRecPollDelaySeconds") int sendRecPollDelaySeconds,
            @RequestParam("maxRecPolls") int maxRecPolls,
            @RequestParam("staticRecAddress") String staticRecAddress,
            @RequestParam("enableLogging") boolean enableLogging,
            @RequestParam("callRecommendationServer") boolean callRecommendationServer,
            @RequestParam("addGPSInaccuracies") boolean addGPSInaccuracies,
            @RequestParam("addGPSOutages") boolean addGPSOutages
    ){
        TestConfig config = new TestConfig();
        config.setDriverGeoHashPrecision(driverGeoHashPrecision);
        config.setSendRecPollDelaySeconds(sendRecPollDelaySeconds);
        config.setMaxRecPolls(maxRecPolls);
        config.setRecServerAddress(staticRecAddress);
        config.setEnableLogging(enableLogging);
        config.setConstantDriverSpeed(driverSpeed);
        config.setCallRecommendationServer(callRecommendationServer);
        config.enableGPSInaccuracies(addGPSInaccuracies);
        config.enableGPSOutages(addGPSOutages);
        return DataQualityTestExectuor.start(config);
    }

    @RequestMapping(path = "/stopSimulation")
    public @ResponseBody RestResponse stopSimulation(){
        DataQualityTestExectuor.stop();
        return new RestResponse("Test stopped");
    }

    @RequestMapping(path = "/metrics")
    public void metrics(Writer responseWriter) throws IOException {
        TextFormat.write004(responseWriter, CollectorRegistry.defaultRegistry.metricFamilySamples());
        responseWriter.close();
    }

    @RequestMapping(path = "/resetprom")
    public @ResponseBody RestResponse resetprom(){
        DataQualityTestExectuor.resetPrometheus();
        return new RestResponse("Reset prometheus metrics");
    }


}
