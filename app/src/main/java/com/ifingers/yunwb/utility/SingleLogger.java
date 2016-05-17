package com.ifingers.yunwb.utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by 19003597 on 5/5/2016.
 */
public class SingleLogger {
    private static SingleLogger ourInstance = new SingleLogger();

    public static SingleLogger getInstance() {
        return ourInstance;
    }

    private FileOutputStream outputStream = null;

    private SingleLogger() {
        long timestamp = System.currentTimeMillis();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.CHINA);
        String time = formatter.format(new Date());
        String filename = "log-" + time + "-" + timestamp + ".log";
        try
        {
            String path = "/sdcard/yunwb/";
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(path, filename);
            if (!file.exists())
                file.createNewFile();
            outputStream = new FileOutputStream(file);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void log(String s) {
        if (outputStream != null) {
            try {
                s += "\r\n";
                outputStream.write(s.getBytes());
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
