package latmos.myweatherstation;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Random;
import java.util.TimeZone;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import com.borax12.materialdaterangepicker.date.DatePickerDialog;
import com.borax12.materialdaterangepicker.time.RadialPickerLayout;
import com.borax12.materialdaterangepicker.time.TimePickerDialog;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

import org.json.JSONArray;
import org.json.JSONObject;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class PlotActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener,TimePickerDialog.OnTimeSetListener {

    private String URI = "http://alatmos.dyndns.org:5000/weather_station/";
    //private String URI = "http://192.168.1.60:5000/weather_station/";
    private Integer HOUR_DIFFERENCE_WITH_SERVER = 3;

    private enum DATA_TYPE {
        TEMPERATURE, HUMIDITY, PRESSURE, WIND, BATTERY
    }

    private EditText dateEtxt;
    private EditText timeEtxt;
    private Spinner dropdown;
    private Spinner weatherStationDropdown;
    private Button btDownload;
    String[] spinnerItems = new String[]{"Temperature", "Humidity", "Pressure", "Battery"};

    //Graph variables
    LineGraphSeries<DataPoint> series;
    GraphView graph;
    double x = 0;
    double y = 0;
    Random randomGenerator;
    private int seriesColor = Color.RED;
    private DATA_TYPE data_type = DATA_TYPE.TEMPERATURE;  // default value

    private String weatherStationDropdownID;  // Use this variable for weather station ID.
    private ArrayList<Weather> arrayOfWeatherMeasurements;
    private String fromYear, fromMonth, fromDay, toYear, toMonth, toDay;
    private String fromHour, fromMinute, toHour, toMinute;
    private String fromYearUser="", fromMonthUser, fromDayUser, toYearUser, toMonthUser, toDayUser;
    private String fromHourUser="", fromMinuteUser, toHourUser, toMinuteUser;

    private DateFormat dateTimeFormatAtXAxis = new SimpleDateFormat("MM/dd HH:mm");

    private static WeatherStationDBHandler weatherStationDBHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot);

        // Add parent Activity to Manifest
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dateEtxt = (EditText) findViewById(R.id.DateEditTextID);
        timeEtxt = (EditText) findViewById(R.id.TimeEditTextID);
        dropdown = (Spinner) findViewById(R.id.spinnerDataType);
        weatherStationDropdown = (Spinner) findViewById(R.id.spinnerWeatherStationName);
        graph = (GraphView) findViewById(R.id.graph);
        btDownload = (Button) findViewById(R.id.btDownload);

        weatherStationDBHandler = new WeatherStationDBHandler(this,null,null,0);
        // Create data type dropdown menu
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, spinnerItems);
        dropdown.setAdapter(adapter);

        // Set dropdown adapter for weather stations
        ArrayAdapter<String> weatherStationsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,
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
        arrayOfWeatherMeasurements = new ArrayList<>();

        //createGraph();

        // Show a Datepicker when the dateButton is clicked
        dateEtxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar now = Calendar.getInstance();
                DatePickerDialog DatePickerDialog = com.borax12.materialdaterangepicker.date.DatePickerDialog.newInstance(
                        PlotActivity.this,
                        now.get(Calendar.YEAR),
                        now.get(Calendar.MONTH),
                        now.get(Calendar.DAY_OF_MONTH)
                );
                DatePickerDialog.show(getFragmentManager(), "Datepickerdialog");
            }
        });

        timeEtxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar now = Calendar.getInstance();
                TimePickerDialog tpd = com.borax12.materialdaterangepicker.time.TimePickerDialog.newInstance(
                        PlotActivity.this,
                        now.get(Calendar.HOUR_OF_DAY),
                        now.get(Calendar.MINUTE),
                        false
                );
                tpd.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {

                    }
                });
                tpd.show(getFragmentManager(), "Timepickerdialog");
            }
        });

        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // legend
                switch (dropdown.getSelectedItem().toString()){
                    case "Temperature":
                        seriesColor = Color.RED;
                        data_type = DATA_TYPE.TEMPERATURE;
                        break;
                    case "Humidity":
                        seriesColor = Color.BLUE;
                        data_type = DATA_TYPE.HUMIDITY;
                        break;
                    case "Pressure":
                        seriesColor = Color.MAGENTA;
                        data_type = DATA_TYPE.PRESSURE;
                        break;
                    case "Battery":
                        seriesColor = Color.CYAN;
                        data_type = DATA_TYPE.BATTERY;
                        break;
                    case "Wind":
                        seriesColor = Color.GREEN;
                        data_type = DATA_TYPE.WIND;
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }
        });

        weatherStationDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                weatherStationDropdownID = weatherStationDBHandler.
                        getWeatherStationID(weatherStationDropdown.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        btDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!fromYearUser.equals("") && !fromHourUser.equals(""))
                {
                    arrayOfWeatherMeasurements.clear();
                    new FetchContentFromServer().execute(URI);
                }else
                    Toast.makeText(PlotActivity.this, "Choose Date and Time Range", Toast.LENGTH_LONG).show();
            }
        });

    }

    // The function is called when user has selected the desired date
    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth,int yearEnd, int monthOfYearEnd, int dayOfMonthEnd) {
        String date = "From "+ dayOfMonth+"/"+(++monthOfYear)+"/"+year+" to "+dayOfMonthEnd+"/"+(++monthOfYearEnd)+"/"+yearEnd;
        fromYearUser = String.valueOf(year);
        fromMonthUser = monthOfYear < 10 ? "0"+monthOfYear : ""+monthOfYear;
        fromDayUser = dayOfMonth < 10 ? "0"+dayOfMonth : ""+dayOfMonth;
        toYearUser = String.valueOf(yearEnd);
        toMonthUser = monthOfYearEnd < 10 ? "0"+monthOfYearEnd : "" + monthOfYearEnd;
        toDayUser = dayOfMonthEnd < 10 ? "0"+dayOfMonthEnd : "" + dayOfMonthEnd;
        dateEtxt.setText(date);
    }

    // The function is called when user has selected the desired time
    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute, int hourOfDayEnd, int minuteEnd) {
        // Text to show for the user
        fromHourUser = hourOfDay < 10 ? "0"+hourOfDay : ""+hourOfDay;
        fromMinuteUser = minute < 10 ? "0"+minute : ""+minute;
        toHourUser = hourOfDayEnd < 10 ? "0"+hourOfDayEnd : ""+hourOfDayEnd;
        toMinuteUser = minuteEnd < 10 ? "0"+minuteEnd : ""+minuteEnd;
        String time = "From " + fromHourUser+ ":" + fromMinuteUser + " to " + toHourUser + ":" + toMinuteUser;
        timeEtxt.setText(time);
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
            Intent settingsIntent = new Intent(PlotActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
        }else if(id == R.id.action_statistics){
            // Show statistics in a dialog
            WeatherStatistics statistics = new WeatherStatistics(arrayOfWeatherMeasurements);
            final View customView = PlotActivity.this.getLayoutInflater().inflate(R.layout.statistics_dialog, null);
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

            AlertDialog.Builder statisticsDialog = new AlertDialog.Builder(PlotActivity.this);
            statisticsDialog.setView(customView)
                    .setPositiveButton("OK", null)
                    .show();
        }else if(id == R.id.action_help){
            final AlertDialog.Builder builder = new AlertDialog.Builder(PlotActivity.this);
            final View helpView = getLayoutInflater().inflate(R.layout.help, null);
            builder.setView(helpView)
                    .setPositiveButton("OK", null)
                    .show();
        }else if(id == R.id.action_data){
            // Export data to a file
            // Alternative way of updating UI from background (no Asynctask used)
            if(arrayOfWeatherMeasurements.size() != 0){
                final PlotActivity temp = this;
                PlotActivityPermissionsDispatcher.exportPlotDataToSDCardWithCheck(temp);
            }else Toast.makeText(PlotActivity.this, "No data found", Toast.LENGTH_LONG).show();
        }

        return super.onOptionsItemSelected(item);
    }

    // Export Data to SD card
    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void exportPlotDataToSDCard() {
        final android.os.Handler handler = new android.os.Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message message) {
                boolean result = message.getData().getBoolean("filestatus");
                if(result) Toast.makeText(PlotActivity.this, "Successfully written to /StationViewer/ExportData", Toast.LENGTH_LONG).show();
                else Toast.makeText(PlotActivity.this, "Make sure the SD card is mounted", Toast.LENGTH_LONG).show();
            }
        };

        Thread fileWriterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                WriteSDCard writeSDCard = new WriteSDCard(PlotActivity.this);
                boolean status = writeSDCard.writeToSDFile(arrayOfWeatherMeasurements);
                Message message = handler.obtainMessage();
                Bundle b = new Bundle();
                b.putBoolean("filestatus",status);
                message.setData(b);
                handler.sendMessage(message);
            }
        });
        fileWriterThread.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PlotActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void exportPlotDataToSDCardRationale(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setMessage("To export your plot data you have to give permission to write in your SD card.")
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

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void exportPlotDataToSDCardNever() {
        Toast.makeText(this,"You have denied permission", Toast.LENGTH_LONG).show();
    }

    // Plot data
    void plotData()
    {
        final android.os.Handler handler = new android.os.Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message message) {
                boolean create_graph = message.getData().getBoolean("graph");  // not used

                // Create the graph when the data have been downloaded
                if(data_type != DATA_TYPE.BATTERY)
                    createGraph(0,arrayOfWeatherMeasurements.size(), getMin(arrayOfWeatherMeasurements,data_type),
                            getMax(arrayOfWeatherMeasurements, data_type));
                else
                    createGraphFloat(0,arrayOfWeatherMeasurements.size(), getMinFloat(arrayOfWeatherMeasurements,data_type),
                            getMaxFloat(arrayOfWeatherMeasurements, data_type));
            }
        };

        Thread plotThread = new Thread(new Runnable() {
            @Override
            public void run() {
                getDataPoints(data_type);
                Message message = handler.obtainMessage();
                Bundle b = new Bundle();
                b.putBoolean("graph",true);
                message.setData(b);
                handler.sendMessage(message);
            }
        });
        plotThread.start();
    }

    /*
    * This function gets the datapoints in oder to create the graph.
    * In the future it will call a web service to receive them
    */
    public void getDataPoints(final DATA_TYPE data_type){

        series = new LineGraphSeries<DataPoint>();

        //New
        /*series.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                if(arrayOfWeatherMeasurements.size() != 0) {
                    // Prepare View
                    Toast.makeText(PlotActivity.this, "Datapoint x: " + ((int) dataPoint.getX()), Toast.LENGTH_LONG).show();
                    AlertDialog.Builder dataDialog = new AlertDialog.Builder(PlotActivity.this);
                    final View dataView = PlotActivity.this.getLayoutInflater().inflate(R.layout.datapoint, null);
                    TextView tvDataPointWeatherStation = (TextView) dataView.findViewById(R.id.tvDataPointWeatherStation);
                    TextView tvDataPointWeather = (TextView) dataView.findViewById(R.id.tvDataPointWeather);
                    TextView tvDataPointDate = (TextView) dataView.findViewById(R.id.tvDataPointDate);
                    TextView tvDataPointTime = (TextView) dataView.findViewById(R.id.tvDataPointTime);
                    TextView tvDataPointTemperature = (TextView) dataView.findViewById(R.id.tvDataPointTemperature);
                    TextView tvDataPointHumidity = (TextView) dataView.findViewById(R.id.tvDataPointHumidity);
                    TextView tvDataPointPressure = (TextView) dataView.findViewById(R.id.tvDataPointPressure);
                    //TextView tvDataPointWind = (TextView) dataView.findViewById(R.id.tvDataPointWind);

                    String text = "Weather Station: " +
                            weatherStationDBHandler.getWeatherStationName(weatherStationDropdownID);
                    tvDataPointWeatherStation.setText(text);
                    text = "Weather: " + arrayOfWeatherMeasurements.get(((int) dataPoint.getX())).getWeather();
                    tvDataPointWeather.setText(text);
                    text = "Date: " + arrayOfWeatherMeasurements.get(((int) dataPoint.getX())).getDateInCustomString();
                    tvDataPointDate.setText(text);
                    text = "Time: " + arrayOfWeatherMeasurements.get(((int) dataPoint.getX())).getTimeInCustomString();
                    tvDataPointTime.setText(text);
                    text = "Temperature: " + arrayOfWeatherMeasurements.get(((int) dataPoint.getX())).getTemperature() + '\u2103';
                    tvDataPointTemperature.setText(text);
                    text = "Humidity: " + arrayOfWeatherMeasurements.get(((int) dataPoint.getX())).getHumidity() + '%';
                    tvDataPointHumidity.setText(text);
                    text = "Pressure: " + arrayOfWeatherMeasurements.get(((int) dataPoint.getX())).getPressure() + " mbar";
                    tvDataPointPressure.setText(text);
                     /*text = "Wind: " + arrayOfWeatherMeasurements.get(Integer.parseInt(String.valueOf(dataPoint.getX()))).getWind();
                    tvDataPointWind.setText(text);*/

                   /* dataDialog.setView(dataView)
                            .setPositiveButton("OK", null)
                            .show();
                }
            }
        });*/

        for (int i = 0 ; i < arrayOfWeatherMeasurements.size(); i++){
            switch (data_type){
                case TEMPERATURE:
                    series.appendData(new DataPoint(arrayOfWeatherMeasurements.get(i).getDateTime().getTime(),
                            arrayOfWeatherMeasurements.get(i).getTemperature()), true, arrayOfWeatherMeasurements.size());
                    break;

                case HUMIDITY:
                    series.appendData(new DataPoint(arrayOfWeatherMeasurements.get(i).getDateTime().getTime(),
                            arrayOfWeatherMeasurements.get(i).getHumidity()), true, arrayOfWeatherMeasurements.size());
                    break;

                case PRESSURE:
                    series.appendData(new DataPoint(arrayOfWeatherMeasurements.get(i).getDateTime().getTime(),
                            arrayOfWeatherMeasurements.get(i).getPressure()), true, arrayOfWeatherMeasurements.size());
                    break;

                case BATTERY:
                    series.appendData(new DataPoint(arrayOfWeatherMeasurements.get(i).getDateTime().getTime(),
                            arrayOfWeatherMeasurements.get(i).getBatteryVoltage()), true, arrayOfWeatherMeasurements.size());
                    break;

                /*case WIND:
                    series.appendData(new DataPoint(Double.parseDouble(arrayOfWeatherMeasurements.get(i).getDate()),
                            arrayOfWeatherMeasurements.get(i).getWind()), true, arrayOfWeatherMeasurements.size());
                    break;*/
            }
        }
        series.setDrawDataPoints(true);
        series.setDataPointsRadius(6);
        series.setColor(seriesColor);
        //series.setAnimated(true);
        series.setDrawBackground(false);
        series.setTitle(dropdown.getSelectedItem().toString());
    }

    /*
    * Create a graph according to the graph type
    */
    public void createGraph(int minX, int maxX, int minY, int maxY)
    {
        graph.removeAllSeries();
        graph.addSeries(series);

        // set custom time label formatter
        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if(isValueX){
                    Date d = new Date((long) (value));
                    return (dateTimeFormatAtXAxis.format(d));
                }else
                    return super.formatLabel(value, isValueX);
            }
        });
        graph.getGridLabelRenderer().setNumHorizontalLabels(2); // only 4 because of the space

        // set manual x bounds to have nice steps
        graph.getViewport().setMinX(arrayOfWeatherMeasurements.get(0).getDateTime().getTime());
        graph.getViewport().setMaxX(arrayOfWeatherMeasurements.get(arrayOfWeatherMeasurements.size() - 1).getDateTime().getTime());
        graph.getViewport().setXAxisBoundsManual(true);

        // enable scaling and scrolling
        graph.getViewport().setScalableY(true);
        graph.getViewport().setScalable(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setScrollableY(true);
        graph.getViewport().setMaxY(maxY);
        graph.getViewport().setMinY(minY);

        graph.getLegendRenderer().setVisible(false);
    }

    /*
* Create a graph according to the graph type
*/
    public void createGraphFloat(int minX, int maxX, Float minY, Float maxY)
    {
        graph.removeAllSeries();
        graph.addSeries(series);

        // set custom time label formatter
        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if(isValueX){
                    Date d = new Date((long) (value));
                    return (dateTimeFormatAtXAxis.format(d));
                }else
                    return super.formatLabel(value, isValueX);
            }
        });
        graph.getGridLabelRenderer().setNumHorizontalLabels(2); // only 4 because of the space

        // set manual x bounds to have nice steps
        graph.getViewport().setMinX(arrayOfWeatherMeasurements.get(0).getDateTime().getTime());
        graph.getViewport().setMaxX(arrayOfWeatherMeasurements.get(arrayOfWeatherMeasurements.size() - 1).getDateTime().getTime());
        graph.getViewport().setXAxisBoundsManual(true);

        // enable scaling and scrolling
        graph.getViewport().setScalableY(true);
        graph.getViewport().setScalable(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setScrollableY(true);
        graph.getViewport().setMaxY(maxY);
        graph.getViewport().setMinY(minY);

        graph.getLegendRenderer().setVisible(false);
    }

    // Fetches weather measurements and displays them in UI.
    private class FetchContentFromServer extends AsyncTask<String, Void, String > {
        ProgressDialog dialog = new ProgressDialog(PlotActivity.this, R.style.AppCompatAlertDialogStyle);

        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(PlotActivity.this, "",
                    "Fetching data from server.\n Please wait", true);
            // Set localtime
            Calendar localTime = Calendar.getInstance();
            localTime.set(Calendar.YEAR, Integer.valueOf(fromYearUser));
            localTime.set(Calendar.MONTH, Integer.valueOf(fromMonthUser));
            localTime.set(Calendar.DAY_OF_MONTH, Integer.valueOf(fromDayUser));
            localTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(fromHourUser));

            // Convert localtime to server time
            TimeZone tz = TimeZone.getTimeZone("GMT");
            Calendar serverTime = new GregorianCalendar(tz);
            serverTime.setTimeInMillis(localTime.getTimeInMillis());
            fromYear = String.valueOf(serverTime.get(Calendar.YEAR));
            fromMonth = String.valueOf(serverTime.get(Calendar.MONTH) < 10 ? "0"+serverTime.get(Calendar.MONTH) : serverTime.get(Calendar.MONTH));
            fromDay = String.valueOf(serverTime.get(Calendar.DAY_OF_MONTH) < 10 ? "0"+serverTime.get(Calendar.DAY_OF_MONTH) : serverTime.get(Calendar.DAY_OF_MONTH));
            fromHour = String.valueOf(serverTime.get(Calendar.HOUR_OF_DAY) < 10 ? "0"+serverTime.get(Calendar.HOUR_OF_DAY) : serverTime.get(Calendar.HOUR_OF_DAY));

            // Do the same for the end range values
            localTime.set(Calendar.YEAR, Integer.valueOf(toYearUser));
            localTime.set(Calendar.MONTH, Integer.valueOf(toMonthUser));
            localTime.set(Calendar.DAY_OF_MONTH, Integer.valueOf(toDayUser));
            localTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(toHourUser));

            serverTime.setTimeInMillis(localTime.getTimeInMillis());
            toYear = String.valueOf(serverTime.get(Calendar.YEAR));
            toMonth = String.valueOf(serverTime.get(Calendar.MONTH) < 10 ? "0"+serverTime.get(Calendar.MONTH) : serverTime.get(Calendar.MONTH));
            toDay = String.valueOf(serverTime.get(Calendar.DAY_OF_MONTH) < 10 ? "0"+serverTime.get(Calendar.DAY_OF_MONTH) : serverTime.get(Calendar.DAY_OF_MONTH));
            toHour = String.valueOf(serverTime.get(Calendar.HOUR_OF_DAY) < 10 ? "0"+serverTime.get(Calendar.HOUR_OF_DAY) : serverTime.get(Calendar.HOUR_OF_DAY));

            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            // Make the REST GET request to the following URI
            String urlString = strings[0] + weatherStationDropdownID + '/'
                    +"fromdate/"+fromYear+'/'+fromMonth+'/'+fromDay+'/'
                    +"todate/"+toYear+'/'+toMonth+'/'+toDay+'/'
                    +"fromtime/"+fromHour+'/'+fromMinuteUser+'/'+"00/"   // Minutes don't change due to timezone difference
                    +"totime/"+toHour+'/'+toMinuteUser+'/'+"00/";
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
        @Override
        protected void onPostExecute(String data) {
            dialog.dismiss();
            if (arrayOfWeatherMeasurements.size() == 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(PlotActivity.this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
                builder.setMessage("Data not Found. Try different date or time")
                        .setPositiveButton("OK", null)
                        .show();
            }else {
                Toast.makeText(PlotActivity.this, "Size = " + arrayOfWeatherMeasurements.size(), Toast.LENGTH_SHORT).show();
                //New
                /*if(data_type != DATA_TYPE.BATTERY)
                    createGraph(0,arrayOfWeatherMeasurements.size(), getMin(arrayOfWeatherMeasurements,data_type),
                            getMax(arrayOfWeatherMeasurements, data_type));
                else
                    createGraphFloat(0,arrayOfWeatherMeasurements.size(), getMinFloat(arrayOfWeatherMeasurements,data_type),
                            getMaxFloat(arrayOfWeatherMeasurements, data_type));
                getDataPoints(data_type);*/
                plotData();
            }
        }
    }

    // Returns the min value of the specified data type
    private static int getMin(ArrayList<Weather> array, DATA_TYPE data_type){
        int min;

        switch (data_type)
        {
            case TEMPERATURE:
                min = array.get(0).getTemperature();
                for(int i = 1; i < array.size(); i++){
                    if(array.get(i).getTemperature() < min)
                        min = array.get(i).getTemperature();
                }
                break;

            case HUMIDITY:
                min = array.get(0).getHumidity();
                for(int i = 1; i < array.size(); i++){
                    if(array.get(i).getHumidity() < min)
                        min = array.get(i).getHumidity();
                }
                break;

            case PRESSURE:
                min = array.get(0).getPressure();
                for(int i = 1; i < array.size(); i++){
                    if(array.get(i).getPressure() < min)
                        min = array.get(i).getPressure();
                }
                break;

            default:
                min = 0;
                break;
        }
        return min;
    }

    // Returns the min value of the specified data type
    private static Float getMinFloat(ArrayList<Weather> array, DATA_TYPE data_type){
        Float min;

        switch (data_type)
        {
            case BATTERY:
                min = array.get(0).getBatteryVoltage();
                for(int i = 1; i < array.size(); i++){
                    if(array.get(i).getBatteryVoltage() < min)
                        min = array.get(i).getBatteryVoltage();
                }
                break;

            default:
                min = 0.0f;
                break;
        }
        return min;
    }

    // Returns the max value of the specified data type
    private static int getMax(ArrayList<Weather> array, DATA_TYPE data_type){
        int max;

        switch (data_type)
        {
            case TEMPERATURE:
                max = array.get(0).getTemperature();
                for(int i = 1; i < array.size(); i++){
                    if(array.get(i).getTemperature() > max)
                        max = array.get(i).getTemperature();
                }
                break;

            case HUMIDITY:
                max = array.get(0).getHumidity();
                for(int i = 1; i < array.size(); i++){
                    if(array.get(i).getHumidity() > max)
                        max = array.get(i).getHumidity();
                }
                break;

            case PRESSURE:
                max = array.get(0).getPressure();
                for(int i = 1; i < array.size(); i++){
                    if(array.get(i).getPressure() > max)
                        max = array.get(i).getPressure();
                }
                break;

            default:
                max = 0;
                break;
        }
        return max;
    }

    // Returns the max value of the specified data type
    private static Float getMaxFloat(ArrayList<Weather> array, DATA_TYPE data_type){
        Float max;

        switch (data_type)
        {
            case BATTERY:
                max = array.get(0).getBatteryVoltage();
                for(int i = 1; i < array.size(); i++){
                    if(array.get(i).getBatteryVoltage() > max)
                        max = array.get(i).getBatteryVoltage();
                }
                break;

            default:
                max = 0.0f;
                break;
        }
        return max;
    }

    // Downloads measurements from Raspberry Server
    public String fetchContent(URL url){
        StringBuffer data = new StringBuffer(1024 * 1024);
        try {
            //opens a connection, now its time to read the incoming data
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

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
    public void insertDataToWeather(String data){
        if (data != "Error occurred") {
            try {
                // Data returned are inside a JSON Array
                JSONArray jsonArray = new JSONArray(data);
                ArrayList<JSONObject> jsonObjects = new ArrayList<>(jsonArray.length());
                for (int i = 0; i < jsonArray.length(); i++)
                    jsonObjects.add(jsonArray.getJSONObject(i));

                // Extract data from each object
                for (int i = 0; i < jsonArray.length(); i++) {
                    String id = jsonObjects.get(i).getString("ID");
                    String date = jsonObjects.get(i).getString("date");
                    String time = jsonObjects.get(i).getString("time");
                    Date receivedDate = MainActivity.receivedDateFormat.parse(date);
                    Date receivedTime = MainActivity.receivedTimeFormat.parse(time);

                    Calendar calDate = Calendar.getInstance();
                    calDate.setTime(receivedDate);

                    Calendar calTime = Calendar.getInstance();
                    calTime.setTime(receivedTime);

                    // Server difference is 3 hours
                    if(calTime.get(Calendar.HOUR_OF_DAY) >= 21)
                        calDate.add(Calendar.DAY_OF_MONTH, 1);
                    calTime.add(Calendar.HOUR_OF_DAY, 3);       // Add the difference in hours

                    Calendar finalCal = Calendar.getInstance();
                    finalCal.set(calDate.get(Calendar.YEAR), calDate.get(Calendar.MONTH), calDate.get(Calendar.DAY_OF_MONTH),
                            calTime.get(Calendar.HOUR_OF_DAY), calTime.get(Calendar.MINUTE), calTime.get(Calendar.SECOND));
                    Date measurementDateTime = finalCal.getTime();

                    String weather = jsonObjects.get(i).getString("weather");
                    Float battery = Float.parseFloat(jsonObjects.get(i).getString("battery"));
                    Integer temperature = jsonObjects.get(i).getInt("temperature");
                    Integer humidity = jsonObjects.get(i).getInt("humidity");
                    Integer pressure = jsonObjects.get(i).getInt("pressure");
                    Integer case_temperature = jsonObjects.get(i).getInt("case_temperature");
                    //Integer wind = jsonObjects.get(i).getInt("wind");  // Not available yet
                    arrayOfWeatherMeasurements.add(new Weather(id,measurementDateTime, weather, temperature, humidity, pressure, battery));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
