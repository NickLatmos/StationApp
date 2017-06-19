package latmos.myweatherstation;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;

public class RemoteControlNotificationReceiver extends BroadcastReceiver {

    // Preferences file to store small size data. These preferences are different from the settings preferences.
    private final String mPrefs = "WeatherStationPreferences";
    private SharedPreferences sharedpreferences;

    private String ID;
    private NotificationCompat.Builder builder;
    Context context;

    public RemoteControlNotificationReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        builder = new NotificationCompat.Builder(context);
        ID = intent.getExtras().getString("ID");
        this.context = context;

        sharedpreferences = context.getSharedPreferences(mPrefs, Context.MODE_PRIVATE);

        new SendPostRequest().execute();
    }


    private class SendPostRequest extends AsyncTask<Void, Void, Integer> {

        protected Integer doInBackground(Void... arg0) {
            try {
                URL url = new URL(RemoteControlActivity.VALVE_URI);

                JSONObject postDataParams = new JSONObject();
                postDataParams.put("valve_status", "NC");
                postDataParams.put("ID",ID);

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
                return conn.getResponseCode();
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }

        // Display notification
        @Override
        protected void onPostExecute(Integer result) {

            if(result == 200) {
                builder.setDefaults(Notification.DEFAULT_ALL)
                        .setWhen(System.currentTimeMillis())
                        .setSmallIcon(R.drawable.ic_rc_off)
                        .setContentTitle("Switch Deactivated")
                        .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                        .setContentInfo("Status")
                        .setContentText("Relay is off");
                SharedPreferences.Editor mEditor = sharedpreferences.edit();
                mEditor.putBoolean("RCSwitchStatus" + ID, false).commit();
                mEditor.apply();
            }
            else{
                builder.setDefaults(Notification.DEFAULT_ALL)
                        .setWhen(System.currentTimeMillis())
                        .setSmallIcon(R.drawable.ic_rc_failed)
                        .setContentTitle("Switch Deactivated")
                        .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                        .setContentInfo("Status")
                        .setContentText("Could not turn off relay. Try manually");
                SharedPreferences.Editor mEditor = sharedpreferences.edit();
                mEditor.putBoolean("RCSwitchStatus" + ID, true).commit();
                mEditor.apply();
            }

            NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(1,builder.build());
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