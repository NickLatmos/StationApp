package latmos.myweatherstation;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;

import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Nick on 5/6/2017.
 */

public class WeatherStationDBHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "WEATHER_STATION_DB.db";
    private static final String TABLE_WEATHER_STATIONS = "WEATHER_STATIONS";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_NAME = "_name";
    private static final String COLUMN_LAT = "_latitude";
    private static final String COLUMN_LON = "_longitude";

    public WeatherStationDBHandler(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " + TABLE_WEATHER_STATIONS + " ("
                + COLUMN_ID +   " VARCHAR2(10) PRIMARY KEY, "
                + COLUMN_NAME + " VARCHAR2(30) NOT NULL,"
                + COLUMN_LAT +  " NUMERIC(6,4), "
                + COLUMN_LON +  " NUMERIC(6,4) " + ')';
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        String query  = "DROP TABLE IF EXISTS " + TABLE_WEATHER_STATIONS;
        db.execSQL(query);
        onCreate(db);
    }

    // Insert data to weather station table
    public void insertWeatherStation(Context context, WeatherStation weatherStation){
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, weatherStation.getID());
        values.put(COLUMN_NAME, weatherStation.getName());
        values.put(COLUMN_LAT, weatherStation.getLatitude());
        values.put(COLUMN_LON, weatherStation.getLongitude());
        db.insert(TABLE_WEATHER_STATIONS, null, values);
        db.close();
    }

    public String getRandomWeatherStationID(){
        String id = "";
        SQLiteDatabase db = getWritableDatabase();
        String query = "SELECT " + COLUMN_ID + " FROM " + TABLE_WEATHER_STATIONS;
        Cursor c = db.rawQuery(query, null);

        if(c.moveToFirst())
            id = c.getString(0);
        c.close();
        db.close();
        return id;
    }

    // Provide the weather station ID to get the name. (name is user defined)
    public String getWeatherStationName(String id){
        String name = "No available weather station";
        SQLiteDatabase db = getWritableDatabase();
        String query = "SELECT " + COLUMN_NAME + " FROM " + TABLE_WEATHER_STATIONS + " WHERE " + COLUMN_ID +"=\""+ id +'"';
        Cursor c = db.rawQuery(query, null);

        if(c.moveToFirst())
            name = c.getString(0);
        c.close();
        db.close();
        return name;
    }

    // Provide the weather station ID to get the name. (name is user defined)
    public String getWeatherStationID(String name){
        String id;
        SQLiteDatabase db = getWritableDatabase();
        String query = "SELECT " + COLUMN_ID + " FROM " + TABLE_WEATHER_STATIONS + " WHERE " + COLUMN_NAME +"=\""+ name +"\";";
        Cursor c = db.rawQuery(query, null);

        if(c.moveToFirst())
            name = c.getString(0);
        c.close();
        db.close();
        return name;
    }

    // Provide the weather station ID to get the name. (name is user defined)
    public LatLng getWeatherStationCoordinates(String id){
        Float latitude = new Float(0);
        Float longitude = new Float(0);
        SQLiteDatabase db = getWritableDatabase();
        String query = "SELECT " + COLUMN_LAT + " FROM " + TABLE_WEATHER_STATIONS + " WHERE " + COLUMN_ID +"=\""+ id +"\";";
        Cursor c = db.rawQuery(query, null);

        if(c.moveToFirst())
            latitude = c.getFloat(0);

        query = "SELECT " + COLUMN_LON + " FROM " + TABLE_WEATHER_STATIONS + " WHERE " + COLUMN_ID +"=\""+ id +"\";";
        c = db.rawQuery(query, null);

        if(c.moveToFirst())
            longitude = c.getFloat(0);

        LatLng coordinates = new LatLng(latitude,longitude);
        c.close();
        db.close();
        return coordinates;
    }

    // Returns all the place names
    public String[] getAllWeatherStationIDs(){
        ArrayList<String> namesList = new ArrayList<String>();
        String[] ids;
        SQLiteDatabase db = getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_WEATHER_STATIONS;
        Cursor c = db.rawQuery(query, null);

        int counter = 0;
        c.moveToFirst();
        while (!c.isAfterLast()) {
            namesList.add(c.getString(c.getColumnIndex(COLUMN_ID)));
            c.moveToNext();
            counter++;
        }
        c.close();
        db.close();

        ids = new String[counter];
        for(int i = 0; i < counter; i++)
            ids[i] = namesList.get(i);

        return ids;
    }

    // Returns all the place names
    public String[] getAllWeatherStationNames(){
        ArrayList<String> namesList = new ArrayList<String>();
        String[] names;
        SQLiteDatabase db = getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_WEATHER_STATIONS;
        Cursor c = db.rawQuery(query, null);

        int counter = 0;
        c.moveToFirst();
        while (!c.isAfterLast()) {
            namesList.add(c.getString(c.getColumnIndex(COLUMN_NAME)));
            c.moveToNext();
            counter++;
        }
        c.close();
        db.close();

        names = new String[counter];
        for(int i = 0; i < counter; i++)
            names[i] = namesList.get(i);

        return names;
    }

    // Returns all the latitudes
    public Float[] getAllLatitudes(){
        ArrayList<Float> latitudesArrayList = new ArrayList<Float>();
        Float[] latitudes;
        SQLiteDatabase db = getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_WEATHER_STATIONS;
        Cursor c = db.rawQuery(query, null);

        int counter = 0;
        c.moveToFirst();
        while (!c.isAfterLast()) {
            latitudesArrayList.add(c.getFloat(c.getColumnIndex(COLUMN_LAT)));
            c.moveToNext();
            counter++;
        }
        c.close();
        db.close();

        latitudes = new Float[counter];
        for(int i = 0; i < counter; i++)
            latitudes[i] = latitudesArrayList.get(i);

        return latitudes;
    }

    // Returns all the longitudes
    public Float[] getAllLongitudes(){
        ArrayList<Float> longitudesArrayList = new ArrayList<Float>();
        Float[] longitudes;
        SQLiteDatabase db = getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_WEATHER_STATIONS;
        Cursor c = db.rawQuery(query, null);

        int counter = 0;
        c.moveToFirst();
        while (!c.isAfterLast()) {
            longitudesArrayList.add(c.getFloat(c.getColumnIndex(COLUMN_LON)));
            c.moveToNext();
            counter++;
        }
        c.close();
        db.close();

        longitudes = new Float[counter];
        for(int i = 0; i < counter; i++)
            longitudes[i] = longitudesArrayList.get(i);

        return longitudes;
    }

    // Deletes a weather station
    public void deleteWeatherStation(String id){
        SQLiteDatabase db = getWritableDatabase();
        String query = "DELETE FROM " + TABLE_WEATHER_STATIONS + " WHERE " + COLUMN_ID + "=\"" + id +"\";";
        db.execSQL(query);
    }

    // Deletes a weather station
    public void deleteAll(){
        SQLiteDatabase db = getWritableDatabase();
        String query = "DELETE FROM " + TABLE_WEATHER_STATIONS +  ";";
        db.execSQL(query);
    }
}
