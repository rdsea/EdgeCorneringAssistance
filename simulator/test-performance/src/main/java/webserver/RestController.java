package webserver;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import test.PerformanceTestExecutor;
import test.TestConfig;

import java.io.IOException;
import java.io.Writer;


@Controller
public class RestController {

    @RequestMapping(path = "/startWithConfigs")
    public @ResponseBody RestResponse startWithConfigs(
            @RequestParam("numDrivers") int numDrivers,
            @RequestParam("driverGeoHashPrecision") int driverGeoHashPrecision,
            @RequestParam("refreshConsulPeriodSeconds") int refreshConsulPeriodSeconds,
            @RequestParam("sendRecPollDelaySeconds") int sendRecPollDelaySeconds,
            @RequestParam("maxRecPolls") int maxRecPolls,
            @RequestParam("maxConsulRetries") int maxConsulRetries,
            @RequestParam("staticRecAddress") String staticRecAddress,
            @RequestParam("enableLogging") boolean enableLogging,
            @RequestParam("grpcTimeout") int grpcTimeout,
            @RequestParam("loadbalance") boolean loadbalance
    ){
        TestConfig config = new TestConfig();
        config.setNumberOfDrivers(numDrivers);
        config.setDriverGeoHashPrecision(driverGeoHashPrecision);
        config.setRefreshConsulPeriodSeconds(refreshConsulPeriodSeconds);
        config.setSendRecPollDelaySeconds(sendRecPollDelaySeconds);
        config.setMaxRecPolls(maxRecPolls);
        config.setMaxConsulRetries(maxConsulRetries);
        config.setRecServerAddress(staticRecAddress);
        config.setEnableLoadBalancing(loadbalance);
        config.setEnableLogging(enableLogging);
        config.setGrpcTimeout(grpcTimeout);
        return PerformanceTestExecutor.start(config);
    }

    @RequestMapping(path = "/start")
    public @ResponseBody RestResponse start(){
        PerformanceTestExecutor.start(TestConfig.getDefault());
        return new RestResponse("Test started with default configs: " + TestConfig.getDefault().toString());
    }

    @RequestMapping(path = "/stop")
    public @ResponseBody RestResponse stop(){
        PerformanceTestExecutor.stop();
        return new RestResponse("Test stopped");
    }

    @RequestMapping(path = "/metrics")
    public void metrics(Writer responseWriter) throws IOException {
        TextFormat.write004(responseWriter, CollectorRegistry.defaultRegistry.metricFamilySamples());
        responseWriter.close();
    }

    @RequestMapping(path = "/resetprom")
    public @ResponseBody RestResponse resetprom(){
        PerformanceTestExecutor.resetPrometheus();
        return new RestResponse("Reset prometheus metrics");
    }


}
