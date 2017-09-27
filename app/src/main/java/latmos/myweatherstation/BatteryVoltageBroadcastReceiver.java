package latmos.myweatherstation;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

// Receives the battery voltage once every 2 hours and if is lower than 10.9V shows a notification
public class BatteryVoltageBroadcastReceiver extends BroadcastReceiver {

    private String URI_BATTERY_VOLTAGE = "http://alatmos.dyndns.org:5000/weather_station/";
    //private String URI_BATTERY_VOLTAGE = "http://192.168.1.60:5000/weather_station/";

    private static Float BATTERY_THRESHOLD_MESSAGE = 10.9f;
    private NotificationCompat.Builder builder;
    private Context context;
    private WeatherStationDBHandler weatherStationDBHandler;   // Important! We must create a new DBHandler to be used in a broadcast receiver.

    public BatteryVoltageBroadcastReceiver() {}

    @Override
    public void onReceive(Context context, Intent intent) {
        builder = new NotificationCompat.Builder(context);
        this.context = context;
        weatherStationDBHandler = new WeatherStationDBHandler(context,null,null,0);    // Initialize our database
        //Toast.makeText(context, "Inside Battery Broadcast Receiver", Toast.LENGTH_LONG).show();
        new GetBatteryVoltage().execute();
    }

    // Get latest battery voltage measurements for all weather stations.
    // If at least one of them has low battery voltage then show a notification.
    private class GetBatteryVoltage extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            try {
                for(String id : weatherStationDBHandler.getAllWeatherStationIDs()) {
                    String urlString = URI_BATTERY_VOLTAGE + id + '/' + "battery_voltage" + '/';
                    URL url = new URL(urlString);
                    String battery_voltage = fetchContent(url);
                    if(ShowBatteryNotification(battery_voltage))
                        return battery_voltage;
                }
                return "";
            }catch (Exception e){
                e.printStackTrace();
                return "";
            }
        }

        // If battery voltage is too low then show a notification.
        @Override
        protected void onPostExecute(String battery_voltage) {
            if(!battery_voltage.equals("")) {
                builder.setDefaults(Notification.DEFAULT_ALL)
                        .setWhen(System.currentTimeMillis())
                        .setSmallIcon(R.mipmap.battery_low_img)
                        .setContentTitle("Attention Required")
                        .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                        .setContentInfo("Your station's battery is running too low")
                        .setContentText("Battery voltage: " + battery_voltage + 'V');
                NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(2,builder.build());
            }
            //Toast.makeText(context, "bat" + battery_voltage, Toast.LENGTH_LONG).show();
        }

        private Boolean ShowBatteryNotification(String battery_voltage){
            if(!battery_voltage.equals("")){
                Float battery = Float.parseFloat(battery_voltage);
                // Check if battery is ok or not
                if(battery <= BATTERY_THRESHOLD_MESSAGE) return true;
                else return false;
            }else return false;
        }

        // Returns the battery voltage (String format) of a specific weather station
        private String fetchContent(URL url) {
            StringBuffer data = new StringBuffer(512);
            try {
                //opens a connection, now its time to read the incoming data
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                //Unnecessary to use the code below because connection.getInputSream() calls it by itself
                //Connects to the server and gets the header
                connection.setRequestMethod("GET");
                connection.connect();
                int response = connection.getResponseCode();
                if(response != 200) return "";

                //Now we will get the main body (of data)
                //Use a BufferedReader to provide efficient reading of characters, lines etc...
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                String tmp = "";
                while ((tmp = reader.readLine()) != null) data.append(tmp).append("\n");
                reader.close();                       //DON'T forget to close the reader

                JSONObject jsonObject = new JSONObject(data.toString());
                String battery = jsonObject.getString("battery");
                return battery;
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }
    }

}
