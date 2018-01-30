package at.mkaran.thesis.commons.mongodb;

import org.junit.Test;

/**
 * Tests connection to AtlasDB
 */
public class TestAtlasDB {


    @Test
    public void testAtlasImpl() {
        CurveStorage.getInstance().initConnection("recommendation", "<PW>", "<URI>", true);
        CurveStorage.getInstance().getDAO().findAll();
    }

}
