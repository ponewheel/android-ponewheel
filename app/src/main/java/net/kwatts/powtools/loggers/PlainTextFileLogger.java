package net.kwatts.powtools.loggers;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.ArrayMap;

import com.github.mikephil.charting.data.Entry;
import com.google.android.gms.maps.model.LatLng;

import net.kwatts.powtools.DeviceInterface;
import net.kwatts.powtools.MapActivity;
import net.kwatts.powtools.OWDevice;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * Created by kwatts on 4/21/16.
 */

public class PlainTextFileLogger  {
    public static final String TAG = MapActivity.class.getSimpleName();

    private File file;
    protected final String name = "TXT";
    private static final String ONEWHEEL_LOGGING_PATH = "powlogs";


    public PlainTextFileLogger(File file) {
        this.file = file;
    }


    public void write(DeviceInterface dev) throws Exception {
        boolean wasFileNew = file.createNewFile();

        FileOutputStream writer = new FileOutputStream(file, true);
        BufferedOutputStream output = new BufferedOutputStream(writer);

        if (wasFileNew) {
            output.write(dev.getCSVHeader().getBytes());
        }

        output.write(dev.toCSV().getBytes());
        output.flush();
        output.close();
       // Files.addToMediaDatabase(file, "text/csv");
    }

    public void annotate(String description, OWDevice dev) throws Exception {
        // TODO Auto-generated method stub

    }

    public String getName() {
        return name;
    }

    public static boolean createDirIfNotExists(String path) {
        boolean ret = true;

        File file = new File(path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                ret = false;
            }
        }
        return ret;
    }

    @NonNull
    public static String getLoggingPath() {
        String logPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + ONEWHEEL_LOGGING_PATH;
        PlainTextFileLogger.createDirIfNotExists(logPath);
        return logPath;
    }

    public static void getEntriesFromFile(String fileName, ArrayList<Entry> timeSpeedMap, ArrayMap<Long, LatLng> latLngs) {
        String logFileLocation = PlainTextFileLogger.getLoggingPath() + "/" + fileName;
        System.out.println("logFile = " + logFileLocation);

        BufferedReader bufferedReader = null;
        FileReader fileReader = null;

        try {

            fileReader = new FileReader(logFileLocation);
            bufferedReader = new BufferedReader(fileReader);

            String currentLine;

            bufferedReader.readLine(); // ignore

            Long referenceTime = null;
            while ((currentLine = bufferedReader.readLine()) != null) {
//                System.out.println(currentLine);
                StringTokenizer stringTokenizer = new StringTokenizer(currentLine, ",");

                Date date;
                //System.out.println("now = " + OWDevice.SIMPLE_DATE_FORMAT.format(new Date()));
                String dateString = stringTokenizer.nextToken();
                try {
                    date = OWDevice.SIMPLE_DATE_FORMAT.parse(dateString);
                } catch (ParseException e) {
                    //System.out.println("now (old)= " + OWDevice.OLD_SIMPLE_DATE_FORMAT.format(new Date()));
                    date = OWDevice.OLD_SIMPLE_DATE_FORMAT.parse(dateString);

                }


                String speed = stringTokenizer.nextToken();
                long time = date.getTime();
                if (referenceTime == null) {
                    referenceTime = time;
                }
                time = time - referenceTime;
                timeSpeedMap.add(new Entry(time, Float.valueOf(speed)));

                if (currentLine.contains("LOC=(")) {
                    int startLocation = currentLine.indexOf("LOC=(") + "LOC=(".length();
                    int commaLocation = currentLine.indexOf(',', startLocation);
                    int endParenLocation = currentLine.indexOf(')', commaLocation);
                    String longLocation = currentLine.substring(commaLocation + 1, endParenLocation);
                    String latLocation = currentLine.substring(startLocation, commaLocation);
                    LatLng latLng = new LatLng(Double.parseDouble(longLocation), Double.parseDouble(latLocation));
                    latLngs.put(time, latLng);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            try {

                if (bufferedReader != null)
                    bufferedReader.close();

                if (fileReader != null)
                    fileReader.close();

            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }
    }
}
