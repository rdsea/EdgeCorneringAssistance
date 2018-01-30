package com.k.matthias.corneringassistanceapplication.db;

import android.provider.BaseColumns;

/**
 * Contract class for SQLite fields of the curve cache
 */

public final class LocalCurveCacheContract {
    private LocalCurveCacheContract() {

    }

    /* Inner class that defines the table contents */
    public static class CachedCurveEntry implements BaseColumns {
        public static final String TABLE_NAME = "curve";
        public static final String COLUMN_CENTER_LAT = "center_lat";
        public static final String COLUMN_CENTER_LON = "center_lon";
        public static final String COLUMN_START_LAT = "start_lat";
        public static final String COLUMN_START_LON = "start_lon";
        public static final String COLUMN_END_LAT = "end_lat";
        public static final String COLUMN_END_LON = "end_lon";
        public static final String COLUMN_RADIUS = "radius";
        public static final String COLUMN_START_BEARING = "start_bearing";
        public static final String COLUMN_END_BEARING = "end_bearing";
        public static final String COLUMN_RECOMMENDED_SPEED = "rec_speed";
    }
}
