package latmos.myweatherstation;

import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class WeatherStation {

    private String weather_station_name;        // User defined
    private String ID;
    private LatLng coordinates;
    private Integer case_temperature;
    private Float battery_voltage;

    public WeatherStation(String ID, String name, LatLng coordinates){
        this.weather_station_name = name;
        this.ID = ID;
        this.coordinates = coordinates;
    }

    public WeatherStation(String ID, String name, LatLng coordinates, Integer case_temperature, Float battery_voltage){
        this.weather_station_name = name;
        this.ID = ID;
        this.coordinates = coordinates;
        this.case_temperature = case_temperature;
        this.battery_voltage = battery_voltage;
    }

    public String getName() {
        return weather_station_name;
    }

    public void setName(String weather_station_name) {
        this.weather_station_name = weather_station_name;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public void setCoordinates(LatLng coordinates) {
        this.coordinates = coordinates;
    }

    public String getID() {
        return ID;
    }

    public LatLng getCoordinates() {
        return coordinates;
    }

    public Float getLatitude(){
        return Float.parseFloat(Double.toString(coordinates.latitude));
    }

    public Float getLongitude(){
        return Float.parseFloat(Double.toString(coordinates.longitude));
    }

    public Integer getCase_temperature() {

        return case_temperature;
    }

    public void setCaseTemperature(Integer case_temperature) {
        this.case_temperature = case_temperature;
    }

    public Float getBatteryVoltage() {
        DecimalFormat formatter = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));
        return Float.valueOf(formatter.format((battery_voltage)));
    }

    public void setBatteryVoltage(Float battery_voltage) {
        this.battery_voltage = battery_voltage;
    }
}
