package latmos.myweatherstation;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

public class RemoteControlActivity extends AppCompatActivity {

    //public static String VALVE_URI = "http://alatmos.dyndns.org:5000/weather_station/valve/";
    public static String VALVE_URI = "http://192.168.1.60:5000/weather_station/valve/";

    private TextView tvTotalMillilitres;
    private TextView tvFlowRate;
    private TextView tvRCLastTimeOpened;
    private Switch swRC;
    private Spinner weatherStationDropdown;
    private String weatherStationDropdownID;
    private Boolean switchStatus;
    private Integer settings_rc_deactivation_time;
    private AlarmManager manager;
    private Intent remoteControlOverIntent;
    private PendingIntent pendingIntent;

    private static WeatherStationDBHandler weatherStationDBHandler;

    public static DateFormat LastTimeOpenedFormat = new SimpleDateFormat("MM/dd/yyyy  kk:mm");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_control);

        // Add parent Activity to Manifest
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tvTotalMillilitres = (TextView) findViewById(R.id.tvTotalMillilitres);
        tvFlowRate = (TextView) findViewById(R.id.tvFlowRate);
        tvRCLastTimeOpened = (TextView) findViewById(R.id.tvRCLastTimeOpened);
        swRC = (Switch) findViewById(R.id.swRC);
        weatherStationDropdown = (Spinner) findViewById(R.id.spWeatherStationNameRemoteControl);

        weatherStationDBHandler = new WeatherStationDBHandler(this,null,null,0);

        // Set dropdown adapter for weather stations
        ArrayAdapter<String> weatherStationsAdapter = new ArrayAdapter<>(this, R.layout.spinner_item,
                weatherStationDBHandler.getAllWeatherStationNames());  //modified - changed weatherStationNames
        weatherStationDropdown.setAdapter(weatherStationsAdapter);

        // Set the currently selected weather station in the dropdown
        int selection = 0;
        for(String name : weatherStationDBHandler.getAllWeatherStationNames()){
            if (weatherStationDBHandler.getWeatherStationName(MainActivity.current_weather_station_ID).
                    equals(name)) {
                weatherStationDropdown.setSelection(selection);
                break;
            }
            selection++;
        }
        weatherStationDropdownID = MainActivity.current_weather_station_ID;
        // Set deactivation time if enabled
        manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        remoteControlOverIntent = new Intent(RemoteControlActivity.this, RemoteControlNotificationReceiver.class);

        // Get switch status
        switchStatus = MainActivity.sharedpreferences.getBoolean("RCSwitchStatus" + weatherStationDropdownID, false);
        swRC.setChecked(switchStatus);
        tvRCLastTimeOpened.setText(MainActivity.sharedpreferences.getString("RCLastTimeOpened","Unknown"));

        weatherStationDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                weatherStationDropdownID = weatherStationDBHandler.
                        getWeatherStationID(weatherStationDropdown.getSelectedItem().toString());
                switchStatus = MainActivity.sharedpreferences.getBoolean("RCSwitchStatus" + weatherStationDropdownID, false);
                swRC.setChecked(switchStatus);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // Open/Close Remote control and set the last time opened (now)
        swRC.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    new SendPostRequest().execute("NO");    // Set relay on
                    // Save the last time it was pressed
                    Calendar now = Calendar.getInstance();
                    String nowStr = LastTimeOpenedFormat.format(now.getTime());
                    SharedPreferences.Editor mEditor = MainActivity.sharedpreferences.edit();
                    mEditor.putString("RCLastTimeOpened",nowStr).commit();
                    mEditor.apply();

                    // Disable relay after a certain time and notify the user.
                    SharedPreferences settingPrefs = PreferenceManager.getDefaultSharedPreferences(RemoteControlActivity.this);
                    Boolean settings_rc_deactivatiom_time_enabled = settingPrefs.getBoolean("perform_automatic_rc_deactivation", false);
                    if(settings_rc_deactivatiom_time_enabled) {
                        remoteControlOverIntent.putExtra("ID", weatherStationDropdownID);
                        pendingIntent = PendingIntent.getBroadcast(RemoteControlActivity.this,0,remoteControlOverIntent,0);
                        settings_rc_deactivation_time = Integer.parseInt(settingPrefs.getString("rc_deactivation_time", "60"));
                        Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.MINUTE, settings_rc_deactivation_time);
                        manager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
                    }
                }else {
                    new SendPostRequest().execute("NC");        // Close valve
                    if(pendingIntent != null)
                        manager.cancel(pendingIntent);
                }
                switchStatus = isChecked;
                SharedPreferences.Editor mEditor = MainActivity.sharedpreferences.edit();
                mEditor.putBoolean("RCSwitchStatus" + weatherStationDropdownID, switchStatus).commit();
                mEditor.apply();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(RemoteControlActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
        }else if(id == R.id.action_help){
            final AlertDialog.Builder builder = new AlertDialog.Builder(RemoteControlActivity.this);
            final View helpView = getLayoutInflater().inflate(R.layout.help, null);
            builder.setView(helpView)
                    .setPositiveButton("OK", null)
                    .show();
        }

        return super.onOptionsItemSelected(item);
    }

    private class SendPostRequest extends AsyncTask<String, Void, String> {

        protected void onPreExecute(){

        }

        protected String doInBackground(String... arg0) {
            try {
                URL url = new URL(VALVE_URI);

                JSONObject postDataParams = new JSONObject();
                postDataParams.put("valve_status", arg0[0]);
                postDataParams.put("ID",weatherStationDropdownID);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(6000);    // milliseconds
                conn.setConnectTimeout(4000); // milliseconds
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(getPostDataString(postDataParams));

                writer.flush();
                writer.close();
                os.close();

                int responseCode=conn.getResponseCode();

                /*if (responseCode == HttpsURLConnection.HTTP_OK) {

                    BufferedReader in=new BufferedReader(
                            new InputStreamReader(
                                    conn.getInputStream()));
                    StringBuffer sb = new StringBuffer("");
                    String line="";

                    while((line = in.readLine()) != null) {

                        sb.append(line);
                        break;
                    }
                    in.close();
                    return sb.toString();
                }*/
                if(responseCode == HttpURLConnection.HTTP_OK) return new String("Command was sent.");
                else return new String("Something went wrong.");
            } catch (Exception e) {
                return new String("Exception: " + e.getMessage());
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getApplicationContext(), result,
                    Toast.LENGTH_LONG).show();
        }
    }

    public String getPostDataString(JSONObject params) throws Exception {

        StringBuilder result = new StringBuilder();
        boolean first = true;

        Iterator<String> itr = params.keys();

        while(itr.hasNext()){

            String key= itr.next();
            Object value = params.get(key);

            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(key, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(value.toString(), "UTF-8"));

        }
        return result.toString();
    }
}