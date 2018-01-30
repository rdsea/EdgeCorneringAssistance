package com.k.matthias.corneringassistanceapplication.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.k.matthias.corneringassistanceapplication.grpc.CurveRecommendationDTO;
import com.k.matthias.corneringassistanceapplication.grpc.CurveRecommendationListDTO;
import com.k.matthias.corneringassistanceapplication.model.CachedCurve;
import com.k.matthias.corneringassistanceapplication.model.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Singleton class that connects to SQLite database to cache detected curves locally
 */

public class LocalCurveCache {

    private static LocalCurveCache sInstance;
    private LocalCurveCacheHelper dbHelper;

    public static synchronized LocalCurveCache getInstance(Context context) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new LocalCurveCache(context.getApplicationContext());
        }
        return sInstance;
    }

    private LocalCurveCache(Context context) {
        dbHelper = new LocalCurveCacheHelper(context);
    }

    public void insertCurves(CurveRecommendationListDTO curveRecommendationListDTO) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        for (CurveRecommendationDTO curveRecommendationDTO : curveRecommendationListDTO.getCurveRecommondationsList()) {
            values.put(LocalCurveCacheContract.CachedCurveEntry.COLUMN_START_LAT, curveRecommendationDTO.getCurve().getStart().getLat());
            values.put(LocalCurveCacheContract.CachedCurveEntry.COLUMN_START_LON, curveRecommendationDTO.getCurve().getStart().getLon());
            values.put(LocalCurveCacheContract.CachedCurveEntry.COLUMN_CENTER_LAT, curveRecommendationDTO.getCurve().getCenter().getLat());
            values.put(LocalCurveCacheContract.CachedCurveEntry.COLUMN_CENTER_LON, curveRecommendationDTO.getCurve().getCenter().getLon());
            values.put(LocalCurveCacheContract.CachedCurveEntry.COLUMN_END_LAT, curveRecommendationDTO.getCurve().getEnd().getLat());
            values.put(LocalCurveCacheContract.CachedCurveEntry.COLUMN_END_LON, curveRecommendationDTO.getCurve().getEnd().getLon());
            values.put(LocalCurveCacheContract.CachedCurveEntry.COLUMN_RADIUS, curveRecommendationDTO.getCurve().getRadius());
            values.put(LocalCurveCacheContract.CachedCurveEntry.COLUMN_RECOMMENDED_SPEED, curveRecommendationDTO.getRecommendedSpeed());
            values.put(LocalCurveCacheContract.CachedCurveEntry.COLUMN_START_BEARING, curveRecommendationDTO.getCurve().getStartBearing());
            values.put(LocalCurveCacheContract.CachedCurveEntry.COLUMN_END_BEARING, curveRecommendationDTO.getCurve().getEndBearing());
            db.insert(LocalCurveCacheContract.CachedCurveEntry.TABLE_NAME, null, values);
        }
        db.close();
    }

    public void removeCurve(CachedCurve cachedCurve) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(LocalCurveCacheContract.CachedCurveEntry.TABLE_NAME, "_id = ? ", new String[] { Long.toString(cachedCurve.getId()) });
    }

    public void removeAllCurves() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("DELETE FROM "+ LocalCurveCacheContract.CachedCurveEntry.TABLE_NAME);
    }

    /**
     * Finds the nearest curve considering the distance from give lat,lon coordinates to
     * a curve's center point.
     * @see <a href="https://stackoverflow.com/a/7261601/2350644">https://stackoverflow.com/a/7261601/2350644</a>
     * @param lat Latitude of search location
     * @param lon Longitude of search location
     * @return nearest curve or null
     */
    public CachedCurve findNearestCurveResult(double lat, double lon) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                LocalCurveCacheContract.CachedCurveEntry._ID,
                LocalCurveCacheContract.CachedCurveEntry.COLUMN_START_LAT,
                LocalCurveCacheContract.CachedCurveEntry.COLUMN_START_LON,
                LocalCurveCacheContract.CachedCurveEntry.COLUMN_END_LAT,
                LocalCurveCacheContract.CachedCurveEntry.COLUMN_END_LON,
                LocalCurveCacheContract.CachedCurveEntry.COLUMN_CENTER_LAT,
                LocalCurveCacheContract.CachedCurveEntry.COLUMN_CENTER_LON,
                LocalCurveCacheContract.CachedCurveEntry.COLUMN_RADIUS,
                LocalCurveCacheContract.CachedCurveEntry.COLUMN_RECOMMENDED_SPEED,
                LocalCurveCacheContract.CachedCurveEntry.COLUMN_START_BEARING,
                LocalCurveCacheContract.CachedCurveEntry.COLUMN_END_BEARING,
        };

        String sortOrder =
                "((%f - " + LocalCurveCacheContract.CachedCurveEntry.COLUMN_CENTER_LAT + ") * (%f - " + LocalCurveCacheContract.CachedCurveEntry.COLUMN_CENTER_LAT + ") + " +
                        " (%f - " + LocalCurveCacheContract.CachedCurveEntry.COLUMN_CENTER_LON + ") * (%f - " + LocalCurveCacheContract.CachedCurveEntry.COLUMN_CENTER_LON + ") * %f)";
        double fudge = Math.pow(Math.cos(Math.toRadians(lat)), 2);
        sortOrder = String.format(Locale.ROOT, sortOrder, lat, lat, lon, lon, fudge);

        Cursor cursor = db.query(
                LocalCurveCacheContract.CachedCurveEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                null,                                     // The columns for the WHERE clause
                null,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        CachedCurve curve = null;

        if (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndex(LocalCurveCacheContract.CachedCurveEntry._ID));
            double startLat = cursor.getDouble(cursor.getColumnIndex(LocalCurveCacheContract.CachedCurveEntry.COLUMN_START_LAT));
            double startLon = cursor.getDouble(cursor.getColumnIndex(LocalCurveCacheContract.CachedCurveEntry.COLUMN_START_LON));
            double endLat = cursor.getDouble(cursor.getColumnIndex(LocalCurveCacheContract.CachedCurveEntry.COLUMN_END_LAT));
            double endLon = cursor.getDouble(cursor.getColumnIndex(LocalCurveCacheContract.CachedCurveEntry.COLUMN_END_LON));
            double centerLat = cursor.getDouble(cursor.getColumnIndex(LocalCurveCacheContract.CachedCurveEntry.COLUMN_CENTER_LAT));
            double centerLon = cursor.getDouble(cursor.getColumnIndex(LocalCurveCacheContract.CachedCurveEntry.COLUMN_CENTER_LON));
            double radius = cursor.getDouble(cursor.getColumnIndex(LocalCurveCacheContract.CachedCurveEntry.COLUMN_RADIUS));
            int speed = cursor.getInt(cursor.getColumnIndex(LocalCurveCacheContract.CachedCurveEntry.COLUMN_RECOMMENDED_SPEED));
            double startBearing = cursor.getDouble(cursor.getColumnIndex(LocalCurveCacheContract.CachedCurveEntry.COLUMN_START_BEARING));
            double endBearing = cursor.getDouble(cursor.getColumnIndex(LocalCurveCacheContract.CachedCurveEntry.COLUMN_END_BEARING));

            curve = new CachedCurve(id,
                    new Location(centerLon, centerLat),
                    new Location(endLon, endLat),
                    new Location(startLon, startLat),
                    radius, speed, startBearing, endBearing);
        }

        cursor.close();
        return curve;
    }

    public List<CachedCurve> findAllCurves() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        ArrayList<CachedCurve> curves = new ArrayList<>();

        String[] projection = {
                LocalCurveCacheContract.CachedCurveEntry._ID,
                LocalCurveCacheContract.CachedCurveEntry.COLUMN_START_LAT,
                LocalCurveCacheContract.CachedCurveEntry.COLUMN_START_LON,
                LocalCurveCacheContract.CachedCurveEntry.COLUMN_END_LAT,
                LocalCurveCacheContract.CachedCurveEntry.COLUMN_END_LON,
                LocalCurveCacheContract.CachedCurveEntry.COLUMN_CENTER_LAT,
                LocalCurveCacheContract.CachedCurveEntry.COLUMN_CENTER_LON,
                LocalCurveCacheContract.CachedCurveEntry.COLUMN_RADIUS,
                LocalCurveCacheContract.CachedCurveEntry.COLUMN_RECOMMENDED_SPEED,
                LocalCurveCacheContract.CachedCurveEntry.COLUMN_START_BEARING,
                LocalCurveCacheContract.CachedCurveEntry.COLUMN_END_BEARING,
        };

        Cursor cursor = db.query(
                LocalCurveCacheContract.CachedCurveEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                null,                                     // The columns for the WHERE clause
                null,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );

        if (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndex(LocalCurveCacheContract.CachedCurveEntry._ID));
            double startLat = cursor.getDouble(cursor.getColumnIndex(LocalCurveCacheContract.CachedCurveEntry.COLUMN_START_LAT));
            double startLon = cursor.getDouble(cursor.getColumnIndex(LocalCurveCacheContract.CachedCurveEntry.COLUMN_START_LON));
            double endLat = cursor.getDouble(cursor.getColumnIndex(LocalCurveCacheContract.CachedCurveEntry.COLUMN_END_LAT));
            double endLon = cursor.getDouble(cursor.getColumnIndex(LocalCurveCacheContract.CachedCurveEntry.COLUMN_END_LON));
            double centerLat = cursor.getDouble(cursor.getColumnIndex(LocalCurveCacheContract.CachedCurveEntry.COLUMN_CENTER_LAT));
            double centerLon = cursor.getDouble(cursor.getColumnIndex(LocalCurveCacheContract.CachedCurveEntry.COLUMN_CENTER_LON));
            double radius = cursor.getDouble(cursor.getColumnIndex(LocalCurveCacheContract.CachedCurveEntry.COLUMN_RADIUS));
            int speed = cursor.getInt(cursor.getColumnIndex(LocalCurveCacheContract.CachedCurveEntry.COLUMN_RECOMMENDED_SPEED));
            double startBearing = cursor.getDouble(cursor.getColumnIndex(LocalCurveCacheContract.CachedCurveEntry.COLUMN_START_BEARING));
            double endBearing = cursor.getDouble(cursor.getColumnIndex(LocalCurveCacheContract.CachedCurveEntry.COLUMN_END_BEARING));

            CachedCurve curve = new CachedCurve(id,
                    new Location(centerLon, centerLat),
                    new Location(endLon, endLat),
                    new Location(startLon, startLat),
                    radius, speed, startBearing, endBearing);

            curves.add(curve);

        }

        cursor.close();
        return curves;
    }
}


