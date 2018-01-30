package at.mkaran.thesis.commons.mongodb;

import at.mkaran.thesis.common.*;
import ch.hsr.geohash.GeoHash;
import model.Location;
import org.bson.Document;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matthias on 30.11.17.
 */
public class TestCurveStorage {

    private CurveStorage curveCache;

    private final Location searchLocation = new Location(15.536140, 48.150192);

    private final Location nearestLoc = new Location(15.525705, 48.151769);
    private final Location secondNearestLoc = new Location(15.517139, 48.151746);
    private final Location furthestLoc = new Location(15.496060, 48.146672);

    private final static double ALLOWED_LOCATION_ERROR = 0.000001;
    private final static int GEOHASH_PRECISION = 6;
    @Before
    public void setup() {
        curveCache = CurveStorage.getInstance();
        curveCache.initConnectionNoAuth("localhost");
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
        Assert.assertEquals("size should be 3", 3, curveCache.getDAO().findAll().size());
    }

    @Test
    public void testFindCurvesByRadius() throws Exception {
        insertTestCurves();
        CurveListDTO foundCurves = curveCache.getDAO()
                .findCurves(nearestLoc.getLatitude(), nearestLoc.getLongitude(), 1);
        Assert.assertNotNull(foundCurves);
        Assert.assertEquals(1, foundCurves.getCurvesList().size());
        Assert.assertEquals(nearestLoc.getLatitude(), foundCurves.getCurvesList().get(0).getCenter().getLat(), ALLOWED_LOCATION_ERROR);
        Assert.assertEquals(nearestLoc.getLongitude(), foundCurves.getCurvesList().get(0).getCenter().getLon(), ALLOWED_LOCATION_ERROR);

        foundCurves = curveCache.getDAO()
                .findCurves(searchLocation.getLatitude(), searchLocation.getLongitude(), 5000);
        Assert.assertNotNull(foundCurves);
        Assert.assertEquals(3, foundCurves.getCurvesList().size());
    }

    @Test
    public void testFindCurvesByGeoHash() throws Exception {
        insertTestCurves();
        String ghNearest = GeoHash.withCharacterPrecision(nearestLoc.getLatitude(), nearestLoc.getLongitude(), GEOHASH_PRECISION).toBase32();
        CurveListDTO foundCurves = curveCache.getDAO().findCurves(ghNearest);
        Assert.assertNotNull(foundCurves);
        Assert.assertEquals(1, foundCurves.getCurvesList().size());
        Assert.assertEquals(nearestLoc.getLatitude(), foundCurves.getCurvesList().get(0).getCenter().getLat(), ALLOWED_LOCATION_ERROR);
        Assert.assertEquals(nearestLoc.getLongitude(), foundCurves.getCurvesList().get(0).getCenter().getLon(), ALLOWED_LOCATION_ERROR);
    }

    @Test
    public void testFindCurvesByBoundingBox() throws Exception {
        insertTestCurves();
        // Small Bounding Box
        GeoHash ghNearest = GeoHash.withCharacterPrecision(nearestLoc.getLatitude(), nearestLoc.getLongitude(), GEOHASH_PRECISION);
        CurveListDTO foundCurves = curveCache.getDAO().findCurves(ghNearest.getBoundingBox());
        Assert.assertNotNull(foundCurves);
        Assert.assertEquals(1, foundCurves.getCurvesList().size());

        // Larger Bounding Box
        ghNearest = GeoHash.withCharacterPrecision(nearestLoc.getLatitude(), nearestLoc.getLongitude(), 5);
        foundCurves = curveCache.getDAO().findCurves(ghNearest.getBoundingBox());
        Assert.assertNotNull(foundCurves);
        Assert.assertEquals(2, foundCurves.getCurvesList().size());

        // Very Large Bounding Box
        ghNearest = GeoHash.withCharacterPrecision(nearestLoc.getLatitude(), nearestLoc.getLongitude(), 4);
        foundCurves = curveCache.getDAO().findCurves(ghNearest.getBoundingBox());
        Assert.assertNotNull(foundCurves);
        Assert.assertEquals(3, foundCurves.getCurvesList().size());
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

        String ghNearest = GeoHash.withCharacterPrecision(nearestLoc.getLatitude(), nearestLoc.getLongitude(), GEOHASH_PRECISION).toBase32();
        String ghSecondNearest = GeoHash.withCharacterPrecision(secondNearestLoc.getLatitude(), secondNearestLoc.getLongitude(), GEOHASH_PRECISION).toBase32();
        String ghFurthest = GeoHash.withCharacterPrecision(furthestLoc.getLatitude(), furthestLoc.getLongitude(), GEOHASH_PRECISION).toBase32();


        List<Document> docs = new ArrayList<Document>();
        docs.add(curveCache.getDAO().curveToDocument(nearest).append("geohash", ghNearest));
        docs.add(curveCache.getDAO().curveToDocument(secondNearest).append("geohash", ghSecondNearest));
        docs.add(curveCache.getDAO().curveToDocument(furthest).append("geohash", ghFurthest));

        curveCache.getDAO().insertMany(docs);
    }
}
