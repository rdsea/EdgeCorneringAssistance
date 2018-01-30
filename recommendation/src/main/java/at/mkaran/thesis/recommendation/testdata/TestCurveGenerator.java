package at.mkaran.thesis.recommendation.testdata;

import at.mkaran.thesis.common.CurveDTO;
import at.mkaran.thesis.common.CurveListDTO;
import at.mkaran.thesis.common.PointDTO;

import java.util.concurrent.ThreadLocalRandom;

/**
 * This class simulates the Curve Cache Service, i.e it generates some random curves.
 */
public class TestCurveGenerator {

    public static CurveListDTO generateRandomCurves(double originLat, double originLon) {
        int count  = ThreadLocalRandom.current().nextInt(1, 15);

        CurveListDTO.Builder curveListBuilder = CurveListDTO.newBuilder();
        for (int i=0; i < count; i++) {
            CurveDTO curve = CurveDTO.newBuilder()
                    .setStart(createRandomPoint(originLat, originLon))
                    .setEnd(createRandomPoint(originLat, originLon))
                    .setCenter(createRandomPoint(originLat, originLon))
                    .setStartBearing(randomBearing())
                    .setEndBearing(randomBearing())
                    .setTurntype(randomTurnType())
                    .setLength(randomLength())
                    .setRadius(randomRadius())
                    .build();
            curveListBuilder.addCurves(curve);
        }
        return curveListBuilder.build();
    }


    private static PointDTO createRandomPoint(double lat, double lon) {
        double randomLat = ThreadLocalRandom.current().nextDouble(lat - 0.005, lat + 0.005);
        double randomLon = ThreadLocalRandom.current().nextDouble(lon - 0.005, lon + 0.005);
        return PointDTO.newBuilder().setLat(randomLat).setLon(randomLon).build();
    }

    private static double randomBearing() {
        return ThreadLocalRandom.current().nextDouble(0.0, 360.0);
    }

    private static String randomTurnType() {
        boolean bool = ThreadLocalRandom.current().nextBoolean();
        if (bool) {
            return "right";
        } else {
            return "left";
        }
    }

    private static int randomLength() {
        return ThreadLocalRandom.current().nextInt(10, 500);
    }

    private static int randomRadius() {
        return ThreadLocalRandom.current().nextInt(10, 300);
    }
}
