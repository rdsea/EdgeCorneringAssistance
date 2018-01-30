import at.mkaran.thesis.common.CurveDTO;
import at.mkaran.thesis.common.CurveRecommendationDTO;
import at.mkaran.thesis.common.CurveRecommendationListDTO;
import at.mkaran.thesis.common.PointDTO;
import car.LocalMongoCurveCache;
import model.CachedCurve;
import model.Location;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the local MongoDB database.
 */
public class TestLocalCache {

    private LocalMongoCurveCache curveCache;

    private final Location searchLocation = new Location(15.536140, 48.150192);

    private final Location nearestLoc = new Location(15.525705, 48.151769);
    private final Location secondNearestLoc = new Location(15.517139, 48.151746);
    private final Location furthestLoc = new Location(15.496060, 48.146672);

    private final static double ALLOWED_LOCATION_ERROR = 0.000001;
    @Before
    public void setup() {
        curveCache = new LocalMongoCurveCache();
        curveCache.removeAll();
    }

    @After
    public void tearDown() {
        //curveCache.removeAll();
        curveCache.closeConnection();
    }


    @Test
    public void testInsert() throws Exception {
        insertTestCurves();
        Assert.assertEquals("size should be 3", 3, curveCache.findAll().size());
    }

    @Test
    public void testNearestQueryOnEmptyCollection() {
        CachedCurve nearest = curveCache.findNearestCurveResult(45.5, 23.3, 5000.0, 0.0);
        Assert.assertNull(nearest);
    }

    @Test
    public void testNearestQuery() throws Exception {
        insertTestCurves();
        CachedCurve foundNearest = curveCache.findNearestCurveResult(searchLocation.getLatitude(), searchLocation.getLongitude(), 5000.0, 0.0);
        Assert.assertNotNull(foundNearest);
        Assert.assertEquals(nearestLoc.getLatitude(), foundNearest.getCenterPoint().getLatitude(), ALLOWED_LOCATION_ERROR);
        Assert.assertEquals(nearestLoc.getLongitude(), foundNearest.getCenterPoint().getLongitude(), ALLOWED_LOCATION_ERROR);
        Assert.assertNotNull(foundNearest.getCenterPoint());
        Assert.assertNotNull(foundNearest.getEndPoint().getLatitude());
        Assert.assertNotNull(foundNearest.getStartPoint().getLatitude());
    }

    @Test
    public void removeOldCurves() throws Exception {
        insertTestCurves();
        Assert.assertEquals("size should be 3", 3, curveCache.findAll().size());
        curveCache.removeAll();
        Assert.assertEquals("size should be 0", 0, curveCache.findAll().size());

    }

    private void insertTestCurves() throws Exception{
        CurveDTO.Builder curveBuilder = CurveDTO.newBuilder();
        CurveDTO nearest = curveBuilder
                .setCenter(PointDTO.newBuilder().setLat(nearestLoc.getLatitude()).setLon(nearestLoc.getLongitude()).build())
                .setStart(PointDTO.newBuilder().setLat(nearestLoc.getLatitude()).setLon(nearestLoc.getLongitude()).build())
                .setEnd(PointDTO.newBuilder().setLat(nearestLoc.getLatitude()).setLon(nearestLoc.getLongitude()).build())
                .setStartBearing(270.0)
                .setEndBearing(270.0)
                .setLength(40.0)
                .setRadius(200)
                .setTurntype("LEFT")
                .build();

        CurveDTO secondNearest = curveBuilder
                .setCenter(PointDTO.newBuilder().setLat(secondNearestLoc.getLatitude()).setLon(secondNearestLoc.getLongitude()).build())
                .setStart(PointDTO.newBuilder().setLat(secondNearestLoc.getLatitude()).setLon(secondNearestLoc.getLongitude()).build())
                .setEnd(PointDTO.newBuilder().setLat(secondNearestLoc.getLatitude()).setLon(secondNearestLoc.getLongitude()).build())
                .setStartBearing(270.0)
                .setEndBearing(270.0)
                .setLength(40.0)
                .setRadius(200)
                .setTurntype("LEFT")
                .build();

        CurveDTO furthest = curveBuilder
                .setCenter(PointDTO.newBuilder().setLat(furthestLoc.getLatitude()).setLon(furthestLoc.getLongitude()).build())
                .setStart(PointDTO.newBuilder().setLat(furthestLoc.getLatitude()).setLon(furthestLoc.getLongitude()).build())
                .setEnd(PointDTO.newBuilder().setLat(furthestLoc.getLatitude()).setLon(furthestLoc.getLongitude()).build())
                .setStartBearing(270.0)
                .setEndBearing(270.0)
                .setLength(40.0)
                .setRadius(200)
                .setTurntype("LEFT")
                .build();

        CurveRecommendationDTO.Builder recBuilder = CurveRecommendationDTO.newBuilder();
        CurveRecommendationDTO recNearest = recBuilder.setCurve(nearest).build();
        CurveRecommendationDTO recSecondNearest = recBuilder.setCurve(secondNearest).build();
        CurveRecommendationDTO recFurthest = recBuilder.setCurve(furthest).build();

        CurveRecommendationListDTO.Builder listBuilder = CurveRecommendationListDTO.newBuilder();
        CurveRecommendationListDTO list = listBuilder
                .addCurveRecommondations(recNearest)
                .addCurveRecommondations(recSecondNearest)
                .addCurveRecommondations(recFurthest)
                .build();
        curveCache.insertMany(list);
    }
}
