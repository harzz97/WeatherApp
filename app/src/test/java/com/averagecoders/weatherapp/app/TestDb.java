package com.averagecoders.weatherapp.app;

/**
 * Created by basuh on 9/20/2016.
 */
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.util.Log;

import com.averagecoders.weatherapp.app.WeatherContract.LocationEntry;
import com.averagecoders.weatherapp.app.WeatherContract.WeatherEntry;

import java.util.Map;
import java.util.Set;

public class TestDb extends AndroidTestCase {

    private static final String LOG_TAG = TestDb.class.getSimpleName();

    public void testCreateDb() throws Throwable {
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
        SQLiteDatabase db = new WeatherDbHelper(mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());
        db.close();
    }

    public void testInsertReadDb() throws Throwable {
        WeatherDbHelper dbHelper = new WeatherDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Location
        long rowId = testInsertReadDb(db, getLocationContentValues(), LocationEntry.TABLE_NAME);

        // Weather
        testInsertReadDb(db, getWeatherContentValues(rowId), WeatherEntry.TABLE_NAME);

        dbHelper.close();
    }

    private long testInsertReadDb(SQLiteDatabase db, ContentValues values, String table)
            throws Throwable {
        long rowId = db.insert(table, null, values);
        assertTrue(rowId != -1);
        Log.d(LOG_TAG, "New row id: " + rowId);
        Cursor cursor = db.query( table, null, null, null, null, null, null );
        validateCursor(cursor, values);
        return rowId;
    }

    public static final String DATETEXT = "20141205";
    public static final String LOCATION_SETTING = "99705";

    public static ContentValues getWeatherContentValues(long rowId) {
        ContentValues values = new ContentValues();
        values.put(WeatherEntry.COLUMN_LOC_KEY, rowId);
        values.put(WeatherEntry.COLUMN_DATE, DATETEXT);
        values.put(WeatherEntry.COLUMN_DEGREES, 1.1);
        values.put(WeatherEntry.COLUMN_HUMIDITY, 1.2);
        values.put(WeatherEntry.COLUMN_PRESSURE, 1.3);
        values.put(WeatherEntry.COLUMN_MAX_TEMP, 75);
        values.put(WeatherEntry.COLUMN_MIN_TEMP, 65);
        values.put(WeatherEntry.COLUMN_SHORT_DESC, "Asteroids");
        values.put(WeatherEntry.COLUMN_WIND_SPEED, 5.5);
        values.put(WeatherEntry.COLUMN_WEATHER_ID, 321);
        return values;
    }

    public static ContentValues getLocationContentValues() {
        return getLocationContentValues(LOCATION_SETTING);
    }

    public static ContentValues getLocationContentValues(String location_setting) {
        ContentValues values = new ContentValues();
        values.put(LocationEntry.COLUMN_LOCATION_SETTING, location_setting);
        values.put(LocationEntry.COLUMN_CITY_NAME, "North Pole");
        values.put(LocationEntry.COLUMN_COORD_LAT, 64.7488);
        values.put(LocationEntry.COLUMN_COORD_LONG, -147.353);
        return values;
    }

    public static void validateCursor(Cursor valueCursor, ContentValues expectedValues) {

        assertTrue(valueCursor.moveToFirst());

        Set<Map.Entry<String, Object>> valueSet = expectedValues.valueSet();
        for (Map.Entry<String, Object> entry : valueSet) {
            String columnName = entry.getKey();
            int idx = valueCursor.getColumnIndex(columnName);
            assertFalse(idx == -1);
            String expectedValue = entry.getValue().toString();
            assertEquals(expectedValue, valueCursor.getString(idx));
        }
        valueCursor.close();
    }
}