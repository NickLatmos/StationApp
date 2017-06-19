package latmos.myweatherstation;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class WeatherAdapter extends ArrayAdapter<Weather>
{
    public WeatherAdapter(Context context, ArrayList<Weather> weather){
        super(context, R.layout.item_weather, weather);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        Weather weatherItem = getItem(position);
        View customView = inflater.inflate(R.layout.item_weather, parent, false);

        TextView tvDate = (TextView) customView.findViewById(R.id.tvDate);
        TextView tvTime = (TextView) customView.findViewById(R.id.tvTime);
        TextView tvWeather = (TextView) customView.findViewById(R.id.tvWeather);
        TextView tvTemperature = (TextView) customView.findViewById(R.id.tvTemperature);
        TextView tvHumidity = (TextView) customView.findViewById(R.id.tvHumidity);
        TextView tvPressure = (TextView) customView.findViewById(R.id.tvPressure);
        //TextView tvWind = (TextView) customView.findViewById(R.id.tvWind);

        tvDate.setText(weatherItem.getDateInCustomString());
        tvTime.setText(weatherItem.getTimeInCustomString());
        tvWeather.setText(weatherItem.getWeather());
        tvTemperature.setText(weatherItem.getTemperature().toString() + '\u2103');
        tvHumidity.setText(weatherItem.getHumidity().toString() + '%');
        tvPressure.setText(weatherItem.getPressure().toString() + "\n mb");
        //tvWind.setText(weatherItem.getWind().toString() + "m/s");

        // Change the weather image
        ImageView weatherImg = (ImageView) customView.findViewById(R.id.weatherImg);
        String weather = weatherItem.getWeather();
        switch (weather){
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
                weatherImg.setImageResource(R.drawable.night_img);  // Same as sunrise image
                break;
        }
        return customView;
    }
}
