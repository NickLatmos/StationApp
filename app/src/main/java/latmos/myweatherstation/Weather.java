package latmos.myweatherstation;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Date;
import java.util.Locale;

public class Weather {
    private String weather_station_id;
    private Date datetime;
    private String weather;
    private Integer temperature;
    private Integer humidity;
    private Integer pressure;
    private Integer wind;
    private Float battery_voltage;

    public Weather(String id, Date datetime, String weather, Integer temperature, Integer humidity, Integer pressure/*, Integer wind*/){
      this.weather_station_id = id;
      this.datetime = datetime;
      this.weather = weather;
      this.temperature = temperature;
      this.humidity = humidity;
      this.pressure = pressure;
      //this.wind = wind;   // Not available yet
    }

    public Weather(String id, Date datetime, String weather, Integer temperature, Integer humidity, Integer pressure, Float battery_voltage /*, Integer wind*/){
        this.weather_station_id = id;
        this.datetime = datetime;
        this.weather = weather;
        this.temperature = temperature;
        this.humidity = humidity;
        this.pressure = pressure;
        this.battery_voltage = battery_voltage;
        //this.wind = wind;   // Not available yet
    }

    public Float getBatteryVoltage() {
        DecimalFormat formatter = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));
        return Float.valueOf(formatter.format((battery_voltage)));
    }

    public String getWeather_station_id() {
        return weather_station_id;
    }

    public String getWeather() {
        return weather;
    }

    public Integer getTemperature() {
        return temperature;
    }

    public Integer getHumidity() {
        return humidity;
    }

    // Convert the pressure to mb
    public Integer getPressure() {
        return pressure / 100;  //mbar
    }

    /*public Integer getWind() {
        return wind;
    }*/

    public String getDateInCustomString(){
        String desiredDate = MainActivity.desiredDateFormat.format(this.datetime);
        return desiredDate;
    }

    public String getTimeInCustomString(){
        String desiredTime = MainActivity.desiredTimeFormat.format(this.datetime);
        return desiredTime;
    }

    public Date getDateTime() {
        return datetime;
    }
}
