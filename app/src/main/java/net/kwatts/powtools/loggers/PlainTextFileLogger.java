package net.kwatts.powtools.loggers;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;

import net.kwatts.powtools.database.Database;
import net.kwatts.powtools.database.entities.Attribute;
import net.kwatts.powtools.database.entities.Moment;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import timber.log.Timber;

public class PlainTextFileLogger  {

    private static final String ONEWHEEL_LOGGING_PATH = "powlogs";

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
    public static String getLoggingPath(Context applicationContext) {
        String logPath = applicationContext.getCacheDir().getAbsolutePath() + "/" + ONEWHEEL_LOGGING_PATH;
        createDirIfNotExists(logPath);
        return logPath;
    }

    public static File createLogFile(Context context, long rideId, String rideDate, Database database) {
        File file = new File( PlainTextFileLogger.getLoggingPath(context) + "/owlogs_" + rideDate + ".csv");
        if (file.exists()) {
            boolean deleted = file.delete();
            Timber.d("deleted?" + deleted);
        }
        try {
            FileOutputStream writer = new FileOutputStream(file, true);
            BufferedOutputStream output = new BufferedOutputStream(writer);

            StringBuilder stringBuilder = new StringBuilder();
            List<String> headers = database.attributeDao().getDistinctKeysFromRide(rideId);
            HashMap<String, String> keyValueOrderKeeper = new LinkedHashMap<>();
            headers.add(0, "time");
            headers.add("gps_lat");
            headers.add("gps_long");
            for (String header : headers) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append(',');
                }
                stringBuilder.append(header);
                keyValueOrderKeeper.put(header, "");
            }
            stringBuilder.append('\n');
            output.write(stringBuilder.toString().getBytes());

            List<Moment> moments = database.momentDao().getFromRide(rideId);
//                Long referenceTime = null;
            for (Moment moment : moments) {
                stringBuilder.setLength(0); // reset
                keyValueOrderKeeper.values().clear();

                long time = moment.getDate().getTime();

                // do we need relative time?
//                    if (referenceTime == null) {
//                        referenceTime = time;
//                    }
//                    time = time - referenceTime;
                stringBuilder.append(Long.toString(time));

                List<Attribute> attributes = database.attributeDao().getFromMoment(moment.id);
                for (Attribute attribute : attributes) {
                    keyValueOrderKeeper.put(attribute.getKey(), attribute.getValue());
                }
                keyValueOrderKeeper.put("gps_lat", moment.getGpsLat());
                keyValueOrderKeeper.put("gps_long", moment.getGpsLong());

                for (String value : keyValueOrderKeeper.values()) {
                    if (stringBuilder.length() > 0) {
                        stringBuilder.append(',');
                    }
                    stringBuilder.append(value);
                }
                stringBuilder.append('\n');
                output.write(stringBuilder.toString().getBytes());
            }

            output.flush();
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return file;
    }
}
