package latmos.myweatherstation;


import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class WriteSDCard {

    private String[] fileDataTypes = {"Temperature", "Humidity", "Pressure","Battery", "Date", "Time"};
    private Context context;

    public WriteSDCard(Context context) {
        this.context = context;
    }

    public boolean writeToSDFile(ArrayList<Weather> weatherMeasurements){
        if(!isExternalStorageWritable()) return false;
        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File(root.getAbsolutePath() + "/StationViewer/ExportData");
        dir.mkdirs();
        File file = new File(dir, "exportData.txt");
        try{
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);

            for(String dataType : fileDataTypes){
                pw.println(dataType);
                for(Weather weather : weatherMeasurements){
                    switch (dataType){
                        case "Temperature":
                            pw.print(weather.getTemperature().toString());
                            pw.print(",");
                            break;
                        case "Humidity":
                            pw.print(weather.getHumidity().toString());
                            pw.print(",");
                            break;
                        case "Pressure":
                            pw.print(weather.getPressure().toString());
                            pw.print(",");
                            break;
                        case "Battery":
                            pw.print(weather.getBatteryVoltage().toString());
                            pw.print(",");
                            break;
                        case "Date":
                            pw.print(weather.getDateInCustomString());
                            pw.print(",");
                            break;
                        case "Time":
                            pw.print(weather.getTimeInCustomString());
                            pw.print(",");
                            break;
                    }
                }
            }
            pw.flush();
            pw.close();
            f.close();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        return true;
    }

    // Checks if external storage is available for read and write
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
            return true;
        return false;
    }
}
