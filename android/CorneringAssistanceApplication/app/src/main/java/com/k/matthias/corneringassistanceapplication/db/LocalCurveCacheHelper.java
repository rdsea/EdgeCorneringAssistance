package com.k.matthias.corneringassistanceapplication.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Helper class to create SQLite Database Tables
 */

public class LocalCurveCacheHelper extends SQLiteOpenHelper {
    
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "CurveCache.db";

    public LocalCurveCacheHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + LocalCurveCacheContract.CachedCurveEntry.TABLE_NAME + " (" +
                    LocalCurveCacheContract.CachedCurveEntry._ID + " INTEGER PRIMARY KEY," +
                    LocalCurveCacheContract.CachedCurveEntry.COLUMN_CENTER_LAT + " REAL," +
                    LocalCurveCacheContract.CachedCurveEntry.COLUMN_CENTER_LON + " REAL," +
                    LocalCurveCacheContract.CachedCurveEntry.COLUMN_START_LAT + " REAL," +
                    LocalCurveCacheContract.CachedCurveEntry.COLUMN_START_LON + " REAL," +
                    LocalCurveCacheContract.CachedCurveEntry.COLUMN_END_LAT + " REAL," +
                    LocalCurveCacheContract.CachedCurveEntry.COLUMN_END_LON + " REAL," +
                    LocalCurveCacheContract.CachedCurveEntry.COLUMN_RADIUS + " REAL," +
                    LocalCurveCacheContract.CachedCurveEntry.COLUMN_RECOMMENDED_SPEED + " INTEGER," +
                    LocalCurveCacheContract.CachedCurveEntry.COLUMN_START_BEARING + " REAL," +
                    LocalCurveCacheContract.CachedCurveEntry.COLUMN_END_BEARING + " REAL)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + LocalCurveCacheContract.CachedCurveEntry.TABLE_NAME;
}
