package latmos.myweatherstation;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

// Activated when viewing Google Maps
// Provides the latest measurements when user clicks on a weather station marker.
public class ServerCommunicationService extends Service {

    //private final String LAST_MEASUREMENT_URI = "http://alatmos.dyndns.org:5000/weather_station/last_measurement/";  // +ID
    private final String LAST_MEASUREMENT_URI = "http://192.168.1.60:5000/weather_station/last_measurement/";  // +ID
    private Timer timer = new Timer();

    public static ArrayList<Weather> weatherObjService;
    public static ArrayList<WeatherStation> weatherStationObjService;

    // Returns the last weather measurements for the selected weather station marker
    public static Weather getWeatherObjService(String id) {
        for(int i = 0; i < weatherObjService.size(); i++){
            if(weatherObjService.get(i).getWeather_station_id().equals(id))
                return weatherObjService.get(i);
        }
        return null;
    }

    // Returns the last weatherStation object for the selected weather station marker
    public static WeatherStation getWeatherStationObjService(String id) {
        for(int i = 0; i < weatherStationObjService.size(); i++){
            if(weatherStationObjService.get(i).getID().equals(id))
                return weatherStationObjService.get(i);
        }
        return null;
    }

    public ServerCommunicationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //
    @Override
    public void onCreate() {
        super.onCreate();
        weatherObjService = new ArrayList<>();
        weatherStationObjService = new ArrayList<>();

        // Fetch new data every 1 minute
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // If having 2 or more measurements per weather station delete the old ones.
                if (weatherObjService.size() >= 2 * MapsActivity.weatherStationDBHandler.getAllWeatherStationIDs().length) {
                    for (int i = 0; i < weatherObjService.size() / 2; i++) {
                        weatherStationObjService.remove(i);
                        weatherObjService.remove(i);
                    }
                }
                // Get the new measurements if any
                for (String id : MapsActivity.weatherStationDBHandler.getAllWeatherStationIDs()) {
                    String urlString = LAST_MEASUREMENT_URI + id + '/';
                    try {
                        URL url = new URL(urlString);
                        String data = fetchContent(url);
                        insertDataToWeather(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0, 1*5*1000);      // 1 Minutes
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    // Downloads measurements from Raspberry Server
    public String fetchContent(URL url) {
        StringBuffer data = new StringBuffer(512);
        try {
            //opens a connection, now its time to read the incoming data
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            //Unnecessary to use the code below because connection.getInputSream() calls it by itself
            //Connects to the server and gets the header
            connection.setRequestMethod("GET");
            connection.connect();
            int response = connection.getResponseCode();
            if (response != 200)
                return "Error occurred";

            //Now we will get the main body (of data)
            //Use a BufferedReader to provide efficient reading of characters, lines etc...
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String tmp = "";
            while ((tmp = reader.readLine()) != null) data.append(tmp).append("\n");
            reader.close();                       //DON'T forget to close the reader

            return data.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Insert the downloaded data into ArrayList of Weather class
    public void insertDataToWeather(String data) {
        if (data != "Error occurred") {
            try {
                // Data returned are inside a JSON Array
                JSONObject jsonObject = new JSONObject(data);

                String id = jsonObject.getString("ID");
                String date = jsonObject.getString("date");
                String time = jsonObject.getString("time");
                Date receivedDate = MainActivity.receivedDateFormat.parse(date);
                Date receivedTime = MainActivity.receivedTimeFormat.parse(time);

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
                Date measurementDateTime = finalCal.getTime();

                /*Date receivedTime = MainActivity.receivedTimeFormat.parse(time);
                Calendar cal = Calendar.getInstance(); // creates calendar
                cal.setTime(receivedTime);             // sets calendar time/date
                cal.add(Calendar.HOUR_OF_DAY, 3);      // adds 3 hours
                receivedTime = cal.getTime();          // returns new date object, one hour in the future
                String desiredTime = MainActivity.desiredTimeFormat.format(receivedTime);*/
                String weather = jsonObject.getString("weather");
                Integer temperature = jsonObject.getInt("temperature");
                Integer humidity = jsonObject.getInt("humidity");
                Integer pressure = jsonObject.getInt("pressure");
                Integer case_temperature = jsonObject.getInt("case_temperature");
                Float battery = Float.parseFloat(jsonObject.getString("battery"));
                //Integer wind = jsonObjects.get(i).getInt("wind");  // Not available yet
                weatherObjService.add(new Weather(id, measurementDateTime, weather, temperature, humidity, pressure));
                weatherStationObjService.add(new WeatherStation(id, MapsActivity.weatherStationDBHandler.getWeatherStationName(id),
                        MapsActivity.weatherStationDBHandler.getWeatherStationCoordinates(id), case_temperature, battery));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
