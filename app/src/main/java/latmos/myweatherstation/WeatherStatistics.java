package latmos.myweatherstation;


import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class WeatherStatistics {
    private ArrayList<Weather> weatherMeasurements;

    public WeatherStatistics(ArrayList<Weather> weatherMeasurements) {
        this.weatherMeasurements = weatherMeasurements;
    }

    public double findAvgTemperature(){
        DecimalFormat formatter = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));
        int[] temperatures = new int[this.weatherMeasurements.size()];

        for(int i = 0; i < this.weatherMeasurements.size(); i++)
            temperatures[i] = (this.weatherMeasurements.get(i).getTemperature());
        return Double.valueOf(formatter.format(findAverage(temperatures)));
    }

    public double findAvgHumidity(){
        DecimalFormat formatter = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));
        int[] humidity = new int[this.weatherMeasurements.size()];

        for(int i = 0; i < this.weatherMeasurements.size(); i++)
            humidity[i] = (this.weatherMeasurements.get(i).getHumidity());
        return Double.valueOf(formatter.format(findAverage(humidity)));
    }

    public double findAvgPressure(){
        DecimalFormat formatter = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));
        int[] pressure = new int[this.weatherMeasurements.size()];

        for(int i = 0; i < this.weatherMeasurements.size(); i++)
            pressure[i] = (this.weatherMeasurements.get(i).getPressure());
        return Double.valueOf(formatter.format(findAverage(pressure)));
    }

    public int findMaxTemperature(){
        int[] temperatures = new int[this.weatherMeasurements.size()];

        for(int i = 0; i < this.weatherMeasurements.size(); i++){
          temperatures[i] = (this.weatherMeasurements.get(i).getTemperature());
        }
        return findMax(temperatures);
    }

    public int findMaxHumidity(){
        int[] humidity = new int[this.weatherMeasurements.size()];

        for(int i = 0; i < this.weatherMeasurements.size(); i++){
            humidity[i] = (this.weatherMeasurements.get(i).getHumidity());
        }
        return findMax(humidity);
    }

    public int findMaxPressure(){
        int[] pressure = new int[this.weatherMeasurements.size()];

        for(int i = 0; i < this.weatherMeasurements.size(); i++){
            pressure[i] = (this.weatherMeasurements.get(i).getPressure());
        }
        return findMax(pressure);
    }

    public int findMinTemperature(){
        int[] temperatures = new int[this.weatherMeasurements.size()];

        for(int i = 0; i < this.weatherMeasurements.size(); i++){
            temperatures[i] = (this.weatherMeasurements.get(i).getTemperature());
        }
        return findMin(temperatures);
    }

    public int findMinHumidity(){
        int[] humidity = new int[this.weatherMeasurements.size()];

        for(int i = 0; i < this.weatherMeasurements.size(); i++){
            humidity[i] = (this.weatherMeasurements.get(i).getHumidity());
        }
        return findMin(humidity);
    }

    public int findMinPressure(){
        int[] pressure = new int[this.weatherMeasurements.size()];

        for(int i = 0; i < this.weatherMeasurements.size(); i++){
            pressure[i] = (this.weatherMeasurements.get(i).getPressure());
        }
        return findMin(pressure);
    }

    private int findMax(int... vals) {
        int max = Integer.MIN_VALUE;

        for (int d : vals) {
            if (d > max) max = d;
        }

        return max;
    }

    private int findMin(int... vals) {
        int min = Integer.MAX_VALUE;

        for (int d : vals) {
            if (d < min) min = d;
        }

        return min;
    }

    private double findAverage(int... vals){
        double sum = 0;

        for(int val : vals) sum += val;

        return (double) sum / vals.length;
    }

    public ArrayList<Weather> getWeatherMeasurements() {
        return weatherMeasurements;
    }
}
