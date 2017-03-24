package net.kwatts.powtools.loggers;
import net.kwatts.powtools.DeviceInterface;
import net.kwatts.powtools.OWDevice;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by kwatts on 4/21/16.
 */

    public class PlainTextFileLogger  {

        private File file;
        protected final String name = "TXT";

        public PlainTextFileLogger(File file) {
            this.file = file;
        }


        public void write(DeviceInterface dev) throws Exception {
            if (!file.exists()) {
                file.createNewFile();

                FileOutputStream writer = new FileOutputStream(file, true);
                BufferedOutputStream output = new BufferedOutputStream(writer);
                output.write(dev.getCSVHeader().getBytes());
                output.flush();
                output.close();

            }

            FileOutputStream writer = new FileOutputStream(file, true);
            BufferedOutputStream output = new BufferedOutputStream(writer);

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
    }
