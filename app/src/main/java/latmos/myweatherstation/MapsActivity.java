package latmos.myweatherstation;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;


/**
 * This shows how to create a simple activity with a map and a marker on the map.
 */
@RuntimePermissions
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private String defaultWeatherStationID = "XYZ";
    private String defaultWeatherStationName = "Thermi Station";
    private LatLng defaultCoordinates = new LatLng(40.5469, 23.0195);

    TextView tvMapInfoLatitude;
    TextView tvMapInfoLongitude;
    TextView tvMapInfoDate;
    TextView tvMapInfoTime;
    TextView tvMapInfoWeatherStation;
    TextView tvMapInfoWeather;
    TextView tvMapInfoTemperature;
    TextView tvMapInfoCaseTemperature;
    TextView tvMapInfoHumidity;
    TextView tvMapInfoPressure;
    TextView tvMapInfoBattery;
    View mapView;

    public static String selectedMarkerWeatherStationID = "";
    public static WeatherStationDBHandler weatherStationDBHandler;  // Handles SQL database for weather stations. ServerCo...Service uses this
    private static ArrayList<Marker> markers;

    private Boolean settings_battery_monitoring;
    private static Boolean MAPS_ON_START_ZOOM_CENTER = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.title_activity_maps);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapView = mapFragment.getView();
        mapFragment.getMapAsync(this);

        // Show my location button (can't make it work for API >= 6.0)
        if (mapView != null &&
                mapView.findViewById(Integer.parseInt("1")) != null) {
            // Get the button view
            View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
            // and next place it, on bottom right (as Google Maps app)
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                    locationButton.getLayoutParams();
            // position on right bottom
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, 30, 30);
        }

        weatherStationDBHandler = new WeatherStationDBHandler(this, null, null, 0);    // Initialize our database
        markers = new ArrayList<Marker>();

        // Store a default weather station when the app is first time executed
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if(!prefs.getBoolean("firstTimeAppExecution", false)) {
            weatherStationDBHandler.insertWeatherStation(MapsActivity.this, new WeatherStation(defaultWeatherStationID,
                    defaultWeatherStationName, defaultCoordinates));  // Insert the default station into database
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("firstTimeAppExecution", true);
            editor.commit();
        }

        // use this to start and trigger a service
        Intent i = new Intent(MapsActivity.this, ServerCommunicationService.class);
        MapsActivity.this.startService(i);
    }

    @Override
    public void onMapReady(GoogleMap map) {

        // Check if I should perform automatic battery monitoring
        SharedPreferences settingPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        settings_battery_monitoring = settingPrefs.getBoolean("perform_auto_check_for_battery_voltage", true);
        AlarmManager manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        Intent batteryMonitoringIntent = new Intent(MapsActivity.this, BatteryVoltageBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(MapsActivity.this,0,batteryMonitoringIntent,0);
        if(settings_battery_monitoring) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, 60);
            manager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_HOUR * 2, pendingIntent);
        }else manager.cancel(pendingIntent);

        markers.clear();
        for (String id : weatherStationDBHandler.getAllWeatherStationIDs()) {
            markers.add(map.addMarker(new MarkerOptions().position(weatherStationDBHandler.getWeatherStationCoordinates(id))
                    .title(weatherStationDBHandler.getWeatherStationName(id))));
        }

        // Map animation
        CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(39.0042, 24.1059));
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(6f);
        if (MAPS_ON_START_ZOOM_CENTER) {
            center = CameraUpdateFactory.newLatLng(new LatLng(39.0042, 24.1059));
            MAPS_ON_START_ZOOM_CENTER = false;
        // If there are markers zoom to the last chosen
        }else if(markers.size() != 0) {
            zoom = CameraUpdateFactory.zoomTo(10f);
            if (!MainActivity.current_weather_station_ID.equals("")) {
                for (Marker marker : markers) {
                    if (weatherStationDBHandler.getWeatherStationID(marker.getTitle().toString()).equals(MainActivity.current_weather_station_ID))
                        center = CameraUpdateFactory.newLatLng(marker.getPosition());
                }
            }else  // Zoom to the only one weather station shown
                center = CameraUpdateFactory.newLatLng(markers.get(0).getPosition());
        }
        map.moveCamera(center);
        map.animateCamera(zoom);

        // requires permission
        final MapsActivity temp = this;
        MapsActivityPermissionsDispatcher.showMyLocationWithCheck(temp, map);

        // Go to the Main Activity if long click on a marker
        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if(markers.size() != 0) {
                    for (Marker marker : markers) {
                        //Toast.makeText(MapsActivity.this, "lon: " + Math.abs(marker.getPosition().longitude - latLng.longitude), Toast.LENGTH_LONG).show();
                        //Toast.makeText(MapsActivity.this, "lat: " + Math.abs(marker.getPosition().latitude - latLng.latitude), Toast.LENGTH_LONG).show();
                        if (Math.abs(marker.getPosition().latitude - latLng.latitude) < 0.1 && Math.abs(marker.getPosition().longitude - latLng.longitude) < 0.1) {
                            MainActivity.current_weather_station_ID = weatherStationDBHandler.getWeatherStationID(marker.getTitle().toString());
                            Intent MainActivityIntent = new Intent(MapsActivity.this, MainActivity.class);
                            startActivity(MainActivityIntent);
                            break;
                        }
                    }
                }else{
                    // Go to main activity but without any station checked
                    MainActivity.current_weather_station_ID = "";
                    Intent MainActivityIntent = new Intent(MapsActivity.this, MainActivity.class);
                    startActivity(MainActivityIntent);
                }
            }
        });

        // Custom window to display weather station data
        map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Getting view from the layout file info_window_layout
                View windowView = getLayoutInflater().inflate(R.layout.map_info_window, null);

                selectedMarkerWeatherStationID = weatherStationDBHandler.getWeatherStationID(marker.getTitle().toString());
                Weather weather = ServerCommunicationService.getWeatherObjService(selectedMarkerWeatherStationID);
                WeatherStation weatherStation = ServerCommunicationService.getWeatherStationObjService(selectedMarkerWeatherStationID);

                tvMapInfoLatitude = (TextView) windowView.findViewById(R.id.tvMapInfoLatitude);
                tvMapInfoLongitude = (TextView) windowView.findViewById(R.id.tvMapInfoLongitude);
                tvMapInfoDate = (TextView) windowView.findViewById(R.id.tvMapInfoDate);
                tvMapInfoTime = (TextView) windowView.findViewById(R.id.tvMapInfoTime);
                tvMapInfoWeatherStation = (TextView) windowView.findViewById(R.id.tvMapInfoWeatherStation);
                tvMapInfoWeather = (TextView) windowView.findViewById(R.id.tvMapInfoWeather);
                tvMapInfoTemperature = (TextView) windowView.findViewById(R.id.tvMapInfoTemperature);
                tvMapInfoCaseTemperature = (TextView) windowView.findViewById(R.id.tvMapInfoCaseTemperature);
                tvMapInfoHumidity = (TextView) windowView.findViewById(R.id.tvMapInfoHumidity);
                tvMapInfoPressure = (TextView) windowView.findViewById(R.id.tvMapInfoPressure);
                //TextView tvMapInfoWind = (TextView) windowView.findViewById(R.id.tvMapInfoWind);
                tvMapInfoBattery = (TextView) windowView.findViewById(R.id.tvMapInfoBattery);

                tvMapInfoWeatherStation.setText(marker.getTitle().toString());
                DecimalFormat df = new DecimalFormat("#.0000");
                LatLng latLng = marker.getPosition();
                tvMapInfoLatitude.setText("Latitude: " + df.format(latLng.latitude));
                tvMapInfoLongitude.setText("Longitude: " + df.format(latLng.longitude));
                tvMapInfoDate.setText("Date: " + weather.getDateInCustomString() + '/' + Calendar.getInstance().get(Calendar.YEAR));
                tvMapInfoTime.setText("Time: " + weather.getTimeInCustomString());

                if(weather != null && weatherStation != null) {

                    tvMapInfoWeather.setText(weather.getWeather());
                    tvMapInfoTemperature.setText(weather.getTemperature() + "" + '\u2103');
                    tvMapInfoCaseTemperature.setText(weatherStation.getCase_temperature() + "" + '\u2103');
                    tvMapInfoHumidity.setText(weather.getHumidity() + "" + '%');
                    tvMapInfoPressure.setText(weather.getPressure() + " " + "mbar");
                    //tvMapInfoWind.setText(weatherObj.getWind().toString() + "m/s");
                    tvMapInfoBattery.setText(weatherStation.getBatteryVoltage().toString() + " Volt");

                    // Change the weather image
                    ImageView weatherImg = (ImageView) windowView.findViewById(R.id.imgMapInfoWeather);
                    switch (weather.getWeather()) {
                        case "Sunny":
                            weatherImg.setImageResource(R.drawable.sun_img);
                            break;
                        case "Raining":
                            weatherImg.setImageResource(R.drawable.rain_img);
                            break;
                        case "Light rain":
                            weatherImg.setImageResource(R.drawable.rain_img);
                            break;
                        case "Clouds":
                            weatherImg.setImageResource(R.drawable.cloudy_img);
                            break;
                        case "Sunrise":
                            weatherImg.setImageResource(R.drawable.sunrise_img);
                            break;
                        case "Sunset":
                            weatherImg.setImageResource(R.drawable.sunrise_img);  // Same as sunrise image
                            break;
                        case "Night":
                            weatherImg.setImageResource(R.drawable.night_img);   // Same as sunrise image
                            break;
                    }
                }
                return windowView;
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
            Intent settingsIntent = new Intent(MapsActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
        }else if(id == R.id.action_help){
            final AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
            final View helpView = getLayoutInflater().inflate(R.layout.help, null);
            builder.setView(helpView)
                    .setPositiveButton("OK", null)
                    .show();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MapsActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @NeedsPermission({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET})
    void showMyLocation(GoogleMap map) {
        // Show my location
        try{
            map.setMyLocationEnabled(true);
            map.getUiSettings().setMyLocationButtonEnabled(true);
            map.getUiSettings().setCompassEnabled(true);
        }catch(SecurityException e){
            e.printStackTrace();
        }
    }

    @OnShowRationale({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET})
    void showMyLocationRational(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setMessage("To see your current location in proximity with your weather station, enable Location Access")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        request.proceed();
                    }
                })
                .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        request.cancel();
                    }
                })
                .show();
    }

    @OnNeverAskAgain({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET})
    void showMyLocationNever() {
        // do nothing
    }
}
