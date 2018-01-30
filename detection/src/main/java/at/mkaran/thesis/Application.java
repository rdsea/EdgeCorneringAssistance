/**
 * Put your copyright and license info here.
 */
package at.mkaran.thesis;

import at.mkaran.thesis.operator.*;
import org.apache.hadoop.conf.Configuration;

import com.datatorrent.api.annotation.ApplicationAnnotation;
import com.datatorrent.api.StreamingApplication;
import com.datatorrent.api.DAG;

@ApplicationAnnotation(name="CurveDetection")
public class Application implements StreamingApplication
{


  @Override
  public void populateDAG(DAG dag, Configuration conf)
  {


    // Setup DAG
    MyRabbitMQInputOperator consumer = dag.addOperator("rabbitInputOperator", new MyRabbitMQInputOperator());
    consumer.initRabbitServer(
            conf.get("rabbitMQServerAddress", "rabbit"),
            conf.getInt("rabbitMQServerPort", 5672)
            );

    RequestAggregator requestAggregator = dag.addOperator("requestAggregator", new RequestAggregator());
    dag.addStream("rabbit-to-aggregator", consumer.outputPort, requestAggregator.inputPort);

    OSMQueryOperator OSMQueryOperator = dag.addOperator("osmOperator", new OSMQueryOperator());
    dag.addStream("aggregator-to-query", requestAggregator.outputPort, OSMQueryOperator.inputPort);

    DetectCurvesOperator curvesOperator = dag.addOperator("curvesOperator", new DetectCurvesOperator());
    dag.addStream("query-to-curves", OSMQueryOperator.outputPort, curvesOperator.inputPort);

    MongoDBOutputOperator mongoOperator = dag.addOperator("mongoOperator", new MongoDBOutputOperator());

    //ConsoleOutputOperator cons = dag.addOperator("console", new ConsoleOutputOperator());
    dag.addStream("curves-to-mongo", curvesOperator.outputPort, mongoOperator.inputPort);

  }
}
