package latmos.myweatherstation;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IntegerRes;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final String GET_VALUES_URI = "http://alatmos.dyndns.org:5000/weather_station/";
    private static String ID_VALIDATION_URL = "http://alatmos.dyndns.org:5000/weather_station/validate/";
    //private final String GET_VALUES_URI = "http://192.168.1.60:5000/weather_station/";
    //private static String ID_VALIDATION_URL = "http://192.168.1.60:5000/weather_station/validate/";

    private static WeatherStationDBHandler weatherStationDBHandler;
    private ArrayList<Weather> arrayOfWeatherMeasurements;
    private WeatherAdapter adapter;

    public static DateFormat receivedDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    public static DateFormat desiredDateFormat = new SimpleDateFormat("dd/MM");
    public static DateFormat receivedTimeFormat = new SimpleDateFormat("HH:mm:ss.SSSSSS");
    public static DateFormat desiredTimeFormat = new SimpleDateFormat("HH:mm");

    private NavigationView navigationView;
    private  SwipeRefreshLayout refreshLayout;

    public static final String mPrefs = "WeatherStationPreferences";   // Preferences file to store small size data
    public static SharedPreferences sharedpreferences;
    private static int weather_station_dialog_position = -1;
    public static String current_weather_station_ID = "";
    public static String settings_weather_measurements_interval;
    public static String settings_weather_measurements_max_measurements;

    private boolean FLAG_ON_START = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefresher);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                arrayOfWeatherMeasurements.clear();
                adapter.clear();
                new FetchContentFromServer().execute(GET_VALUES_URI);
            }
        });

        weatherStationDBHandler = new WeatherStationDBHandler(this, null, null, 0);
        arrayOfWeatherMeasurements = new ArrayList<>();

        // Create the adapter to convert the array to views
        adapter = new WeatherAdapter(this, arrayOfWeatherMeasurements);
        // Attach the adapter to a ListView
        ListView weatherList = (ListView) findViewById(R.id.weatherList);
        weatherList.setAdapter(adapter);

        // Set the previous user defined values for the currently selected weather station
        sharedpreferences = getSharedPreferences(mPrefs, Context.MODE_PRIVATE);

        // Set the user defined settings
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SharedPreferences settingPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        settings_weather_measurements_interval = settingPrefs.getString("weather_measurements_interval", "5");
        settings_weather_measurements_max_measurements = settingPrefs.getString("weather_measurements_max_length", "20");

        // Fetch data from server when Activity is created.
        new FetchContentFromServer().execute(GET_VALUES_URI);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Go to settings
            Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
        }else if(id == R.id.action_statistics){
            // Show statistics in a dialog
            WeatherStatistics statistics = new WeatherStatistics(arrayOfWeatherMeasurements);
            final View customView = MainActivity.this.getLayoutInflater().inflate(R.layout.statistics_dialog, null);
            TextView tvMaxTemperature = (TextView) customView.findViewById(R.id.tvMaxTemperature);
            TextView tvMaxPressure = (TextView) customView.findViewById(R.id.tvMaxPressure);
            TextView tvMaxHumidity = (TextView) customView.findViewById(R.id.tvMaxHumidity);
            TextView tvMinTemperature = (TextView) customView.findViewById(R.id.tvMinTemperature);
            TextView tvMinHumidity = (TextView) customView.findViewById(R.id.tvMinHumidity);
            TextView tvMinPressure = (TextView) customView.findViewById(R.id.tvMinPressure);
            TextView tvAvgTemperature = (TextView) customView.findViewById(R.id.tvAvgTemperature);
            TextView tvAvgHumidity = (TextView) customView.findViewById(R.id.tvAvgHumidity);
            TextView tvAvgPressure = (TextView) customView.findViewById(R.id.tvAvgPressure);

            tvMaxTemperature.setText("Max: " + statistics.findMaxTemperature() + "" + '\u2103');
            tvMinTemperature.setText("Min: " + statistics.findMinTemperature() + "" + '\u2103');
            tvAvgTemperature.setText("Avg: " + statistics.findAvgTemperature() + "" + '\u2103');
            tvMaxHumidity.setText(statistics.findMaxHumidity() + "" + '%');
            tvMinHumidity.setText(statistics.findMinHumidity() + "" + '%');
            tvAvgHumidity.setText(statistics.findAvgHumidity() + "" + '%');
            tvMaxPressure.setText(statistics.findMaxPressure() + "mbar");
            tvMinPressure.setText(statistics.findMinPressure() + "mbar");
            tvAvgPressure.setText(statistics.findAvgPressure() + "mbar");

            AlertDialog.Builder statisticsDialog = new AlertDialog.Builder(MainActivity.this);
            statisticsDialog.setView(customView)
                    .setPositiveButton("OK", null)
                    .show();
        }else if(id == R.id.action_help){
            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            final View helpView = getLayoutInflater().inflate(R.layout.help, null);
            builder.setView(helpView)
                    .setPositiveButton("OK", null)
                    .show();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_weather_station) {
            chooseWeatherStation();
        } else if (id == R.id.nav_new_weather_station) {
            createNewWeatherStation();
        } else if (id == R.id.nav_delete_weather_station) {
            deleteWeatherStation();
        } else if (id == R.id.nav_plot_diagram) {
            Intent plotActivityIntent = new Intent(MainActivity.this, PlotActivity.class);
            startActivity(plotActivityIntent);
        } else if (id == R.id.nav_remote_control) {
            Intent remoteControlIntent = new Intent(MainActivity.this, RemoteControlActivity.class);
            startActivity(remoteControlIntent);
        } else if (id == R.id.nav_google_maps) {
            Intent googleMapsActivityIntent = new Intent(MainActivity.this, MapsActivity.class);
            startActivity(googleMapsActivityIntent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // User chooses his/her weather station to display data from.
    private void chooseWeatherStation(){
        final String names[] = weatherStationDBHandler.getAllWeatherStationNames();
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Choose your weather station")
                .setSingleChoiceItems(names, weather_station_dialog_position,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int position) {
                                weather_station_dialog_position = position;
                                current_weather_station_ID = weatherStationDBHandler.getWeatherStationID(names[position]);  // Change weather station
                                /*SharedPreferences.Editor mEditor = sharedpreferences.edit();
                                mEditor.putString("weather_station_ID", current_weather_station_ID).commit();
                                mEditor.putInt("weather_station_position", weather_station_dialog_position).commit();
                                mEditor.apply();*/
                            }
                        }
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        arrayOfWeatherMeasurements.clear();
                        adapter.clear();
                        FLAG_ON_START = true;
                        new FetchContentFromServer().execute(GET_VALUES_URI);
                        // Change the item's name
                        Menu navigationDrawerMenu = navigationView.getMenu();
                        MenuItem nav_weather_station = navigationDrawerMenu.findItem(R.id.nav_weather_station);
                        nav_weather_station.setTitle(weatherStationDBHandler.getWeatherStationName(current_weather_station_ID));
                    }
                })  //must put a new click listener
                .show();
    }

    // User creates a new weather station from navigation Drawer
    private void createNewWeatherStation(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        final View customView = MainActivity.this.getLayoutInflater().inflate(R.layout.insert_weather_station, null);

        builder.setView(customView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        EditText etWeatherStationID = (EditText) customView.findViewById(R.id.etWeatherStationID);
                        EditText etWeatherStationName = (EditText) customView.findViewById(R.id.etWeatherStationName);
                        EditText etPlaceLatitude = (EditText) customView.findViewById(R.id.etPlaceLatitude);
                        EditText etPlaceLongitude = (EditText) customView.findViewById(R.id.etPlaceLongitude);

                        if(!(etWeatherStationID.getText().toString().matches("") || etWeatherStationName.getText().toString().matches("")
                                || etPlaceLatitude.getText().toString().matches("") || etPlaceLongitude.getText().toString().matches("")))
                        {
                            String weatherStationID = etWeatherStationID.getText().toString();
                            String weatherStationName = etWeatherStationName.getText().toString();
                            Float latitude = Float.parseFloat(etPlaceLatitude.getText().toString());
                            Float longitude = Float.parseFloat(etPlaceLongitude.getText().toString());
                            new IDValidation().execute(weatherStationID);
                            weatherStationDBHandler.insertWeatherStation(MainActivity.this, new WeatherStation(weatherStationID,
                                    weatherStationName, new LatLng(latitude,longitude)));  // Insert into database
                        }else
                            Toast.makeText(MainActivity.this, "Fault! Insert all the the fields", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteWeatherStation(){
        final String names[] = weatherStationDBHandler.getAllWeatherStationNames();
        // arraylist to keep the selected items
        final ArrayList<Integer> selectedItems = new ArrayList<>();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Select the station to be deleted")
                .setMultiChoiceItems(names, null, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int indexSelected, boolean isChecked) {
                        if (isChecked) {
                            // If the user checked the item, add it to the selected items
                            selectedItems.add(indexSelected);
                        } else if (selectedItems.contains(indexSelected)) {
                            // Else, if the item is already in the array, remove it
                            selectedItems.remove(Integer.valueOf(indexSelected));
                        }
                    }
                }).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // Delete the names from database and Array List
                        for(int i : selectedItems){
                            if(current_weather_station_ID.equals(weatherStationDBHandler.getWeatherStationID(names[i]))){
                                current_weather_station_ID = "";
                                weather_station_dialog_position = -1;
                                /*SharedPreferences.Editor mEditor = sharedpreferences.edit();
                                mEditor.putString("weather_station_ID", current_weather_station_ID).commit();
                                mEditor.putInt("weather_station_position", weather_station_dialog_position).commit();
                                mEditor.apply();*/
                            }
                            // Delete selected weather station
                            weatherStationDBHandler.deleteWeatherStation(weatherStationDBHandler.getWeatherStationID(names[i]));
                        }
                    }
                }).setNegativeButton("Cancel",null)
                .create();
        dialog.show();
    }

    // Fetches weather measurements and displays them in UI.
    // @args String -> URI
    private class FetchContentFromServer extends AsyncTask<String, Void, String >{
        ProgressDialog dialog = new ProgressDialog(MainActivity.this, R.style.AppCompatAlertDialogStyle);

        @Override
        protected void onPreExecute() {
            // Show dialog only on start up.
            if (FLAG_ON_START){
                 dialog = ProgressDialog.show(MainActivity.this, "",
                    "Fetching data from server.\n Please wait", true);
            }
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            String urlString = strings[0] + current_weather_station_ID + '/'
                    + "last_measurements/" + settings_weather_measurements_max_measurements + '/'
                    + "interval/" + settings_weather_measurements_interval + '/';
            try{
                URL url = new URL(urlString);
                String data = fetchContent(url);
                insertDataToWeather(data);
                return data;
            }catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }

        // Toast can only be used after the doInBackground() has finished
        // Inflate the vire with the fetched data
        @Override
        protected void onPostExecute(String data) {
            refreshLayout.setRefreshing(false);
            adapter.notifyDataSetChanged();
            //Toast.makeText(MainActivity.this, "Size: " + arrayOfWeatherMeasurements.size(), Toast.LENGTH_LONG).show();
            if(FLAG_ON_START) {
                dialog.dismiss();
                FLAG_ON_START = false;
            }
        }

        // Downloads measurements from Raspberry Server
        private String fetchContent(URL url){
            StringBuffer data = new StringBuffer(1024 * 1024);
            try {
                //opens a connection, now its time to read the incoming data
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setReadTimeout(3000);
                connection.setConnectTimeout(3000);
                //Unnecessary to use the code below because connection.getInputSream() calls it by itself
                //Connects to the server and gets the header
                connection.setRequestMethod("GET");
                connection.connect();
                int response = connection.getResponseCode();
                if(response != 200)
                    return "Error occurred";

                //Now we will get the main body (of data)
                //Use a BufferedReader to provide efficient reading of characters, lines etc...
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                String tmp = "";
                while((tmp=reader.readLine()) != null) data.append(tmp).append("\n");
                reader.close();                       //DON'T forget to close the reader

                return data.toString();
            }catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }

        // Insert the downloaded data into ArrayList of Weather class
        private void insertDataToWeather(String data){
            if (data != "Error occurred") {
                try {
                    // Data returned are inside a JSON Array ordered by date/time
                    JSONArray jsonArray = new JSONArray(data);
                    ArrayList<JSONObject> jsonObjects = new ArrayList<>(jsonArray.length());
                    for (int i = 0; i < jsonArray.length(); i++)
                        jsonObjects.add(jsonArray.getJSONObject(i));

                    Calendar previousCal = Calendar.getInstance();  // previousCal will get the Calendar's value of the last measurement

                    // Extract data from each object
                    for (int i = 0; i < jsonArray.length(); i++) {
                        String id = jsonObjects.get(i).getString("ID");
                        String date = jsonObjects.get(i).getString("date");
                        String time = jsonObjects.get(i).getString("time");
                        Date receivedDate = receivedDateFormat.parse(date);
                        Date receivedTime = receivedTimeFormat.parse(time);

                        Calendar calDate = Calendar.getInstance();
                        calDate.setTime(receivedDate);

                        Calendar calTime = Calendar.getInstance();
                        calTime.setTime(receivedTime);

                        if(calTime.get(Calendar.HOUR_OF_DAY) >= 21)
                            calDate.add(Calendar.DAY_OF_MONTH, 1);
                        calTime.add(Calendar.HOUR_OF_DAY, 3);       // Add the difference in hours

                        Calendar finalCal = Calendar.getInstance();
                        finalCal.set(calDate.get(Calendar.YEAR), calDate.get(Calendar.MONTH), calDate.get(Calendar.DAY_OF_MONTH),
                                     calTime.get(Calendar.HOUR_OF_DAY), calTime.get(Calendar.MINUTE), calTime.get(Calendar.SECOND));

                        // Ordered by most recent date/time
                        // If difference is less than the interval then go to the next entry
                        if(i != 0) {
                            if (previousCal.getTimeInMillis() - finalCal.getTimeInMillis() < Integer.parseInt(settings_weather_measurements_interval) * 60 * 1000)
                                continue;
                        }

                        // Display the user defined number
                        if(arrayOfWeatherMeasurements.size() >= Integer.parseInt(settings_weather_measurements_max_measurements))
                            continue;

                        previousCal = finalCal;
                        Date measurementDateTime = finalCal.getTime();

                        /*String desiredDate = desiredDateFormat.format(receivedDate);
                        Calendar cal = Calendar.getInstance(); // creates calendar
                        cal.setTime(receivedTime);             // sets calendar time/date
                        cal.add(Calendar.HOUR_OF_DAY, 3);      // adds 3 hours
                        receivedTime = cal.getTime();          // returns new date object
                        String desiredTime = desiredTimeFormat.format(receivedTime);*/
                        String weather = jsonObjects.get(i).getString("weather");
                        Integer temperature = jsonObjects.get(i).getInt("temperature");
                        Integer humidity = jsonObjects.get(i).getInt("humidity");
                        Integer pressure = jsonObjects.get(i).getInt("pressure");
                        Integer case_temperature = jsonObjects.get(i).getInt("case_temperature");
                        //Integer wind = jsonObjects.get(i).getInt("wind");  // Not available yet
                        arrayOfWeatherMeasurements.add(new Weather(id, measurementDateTime, weather, temperature, humidity, pressure));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // This class is used to decide whether the ID the user inserted is valuid or not.
    private class IDValidation extends AsyncTask<String, Void, String[] > {
        ProgressDialog dialog = new ProgressDialog(MainActivity.this, R.style.AppCompatAlertDialogStyle);

        @Override
        protected void onPreExecute() {
            // Show dialog only on start up.
            dialog = ProgressDialog.show(MainActivity.this, "",
                    "Validating ID...", true);
            super.onPreExecute();
        }

        @Override
        protected String[] doInBackground(String... strings) {
            String urlString = ID_VALIDATION_URL + strings[0] + '/';
            try {
                URL url = new URL(urlString);
                int code = validateWeatherStationID(url);
                String response = String.valueOf(code);
                String args[] = {strings[0], response};   // return {id, response_code}
                return args;
            }catch (Exception e){
                e.printStackTrace();
                String test[] = {"",""};
                return test;
            }
        }

        @Override
        protected void onPostExecute(String[] args) {
            dialog.dismiss();
            String id = args[0];
            Integer response_code = Integer.parseInt(args[1]);

            // If HTTP code is != 200 then ID is not valid.
            switch (response_code){
                case 200:
                    current_weather_station_ID = id;
                    weather_station_dialog_position++;
                    arrayOfWeatherMeasurements.clear();
                    adapter.clear();
                    new FetchContentFromServer().execute(GET_VALUES_URI);
                    // Set the new weather station as the selected one
                    final AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                    builder1.setTitle("ID has been successfully validated.")
                            .setNegativeButton("OK", null)
                            .show();
                    break;

                default:
                    weatherStationDBHandler.deleteWeatherStation(id);           // Delete the weather station user just inserted
                    final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("ID is not valid")
                            .setNegativeButton("Cancel", null)
                            .show();
                    break;
            }
        }

        // Asks server for ID validation.
        private int validateWeatherStationID(URL url){
            try {
                //opens a connection, now its time to read the incoming data
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.connect();
                int response = connection.getResponseCode();
                return response;
            }catch (Exception e){
                e.printStackTrace();
                return 0;
            }
        }
    }
}
