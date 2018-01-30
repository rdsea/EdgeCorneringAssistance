package at.mkaran.thesis;

import at.mkaran.thesis.operator.OSMQueryOperator;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

/**
 * Created by matthias on 26.11.17.
 */
public class OSMTester {

    @Test
    public void printOSMQuery() throws UnsupportedEncodingException {
        OSMQueryOperator op = new OSMQueryOperator();
        op.setOverpassUrl("http://<SOME-URL>");
        System.out.println(op.getQueryTest(47.215132,15.6274832,6));
    }
}
