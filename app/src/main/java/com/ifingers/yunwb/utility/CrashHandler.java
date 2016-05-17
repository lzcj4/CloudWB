package com.ifingers.yunwb.utility;
import android.content.Context;
import android.os.Looper;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Created by SFY on 2016/3/15.
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static CrashHandler sInstance = null;
    private static Object lock = new Object();
    private Thread.UncaughtExceptionHandler mDefaultExceptionHandler;
    private Context context = null;
    private CrashHandler()
    {
    }
    public static CrashHandler getInstance()
    {
        if (sInstance == null)
        {
            synchronized (lock)
            {
                if (sInstance == null)
                    sInstance = new CrashHandler();
            }
        }
        return sInstance;
    }
    public void register(Context context)
    {
        this.context = context;
        mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }
    @Override
    public void uncaughtException(Thread thread, Throwable ex)
    {
        //生成日志
        final String filename = cacheLog(ex);
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                Looper.prepare();
                if(filename != null)
                    Toast.makeText(context, "程序崩溃了:( \n崩溃日志已保存到"+filename+"",Toast.LENGTH_LONG).show();
                Looper.loop();
            }
        }).start();
        try
        {
            Thread.sleep(2000);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        mDefaultExceptionHandler.uncaughtException(thread,ex);
    }
    private String cacheLog(Throwable ex)
    {
        if(ex == null)
            return null;
        StringBuilder buffer = new StringBuilder();
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null)
        {
            cause.printStackTrace();
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        buffer.append(result);
        long timestamp = System.currentTimeMillis();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.CHINA);
        String time = formatter.format(new Date());
        String filename = "crash-" + time + "-" + timestamp + ".log";
        try
        {
            String path = "/sdcard/crash/";
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(path, filename);
            if (!file.exists())
                file.createNewFile();
            FileOutputStream out = new FileOutputStream(file);
            out.write(buffer.toString().getBytes());
            out.close();
            return file.getAbsolutePath();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }
}