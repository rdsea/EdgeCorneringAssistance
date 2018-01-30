package at.mkaran.thesis.recommendation;

import at.mkaran.thesis.common.CurveDTO;
import at.mkaran.thesis.common.CurveListDTO;
import at.mkaran.thesis.recommendation.testdata.TestCurveGenerator;
import org.junit.Test;

/**
 * Created by matthias on 08.09.17.
 */
public class TestCurveGeneratorTest {
    @Test
    public void generateRandomCurves() throws Exception {
        CurveListDTO list = TestCurveGenerator.generateRandomCurves(48.49591193, 14.13294959);
        for (CurveDTO curve : list.getCurvesList()) {
            System.out.println(curve.toString());
        }
    }

}