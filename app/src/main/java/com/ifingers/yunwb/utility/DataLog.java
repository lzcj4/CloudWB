package com.ifingers.yunwb.utility;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Created by Nick_PC on 2016/5/5.
 */
public class DataLog {

    private static final String TAG = "DataLog";
    private static final String DATA_FOLDER = "BTData";
    private static final String DATA_IN_FILE = "In.txt";
    private static final String DATA_IN_LINE_FILE = "In_Line.txt";
    private static final String DATA_OUT_FILE = "Out.txt";

    private OutputStreamWriter mInWriter;
    private OutputStreamWriter mInLineWriter;
    private OutputStreamWriter mOutWriter;

    public static DataLog getInstance() {
        return InstanceHolder.instance;
    }

    private DataLog() {
        createFiles();
    }

    private static class InstanceHolder {
        private static DataLog instance = new DataLog();
    }

    private void createFiles() {
        try {
            File dataFolder = getDataFolder();
            if (null != dataFolder) {
                mInWriter = createDataFile(dataFolder, DATA_IN_FILE);
                mInLineWriter = createDataFile(dataFolder, DATA_IN_LINE_FILE);
                mOutWriter = createDataFile(dataFolder, DATA_OUT_FILE);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static File getDataFolder() throws IllegalAccessException {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            throw new IllegalAccessException("External storage is not mounted");
        }
        File result = null;
        File rootFolder = Environment.getExternalStorageDirectory();
        if (null != rootFolder && rootFolder.exists()) {
            File dataFolder = new File(rootFolder, String.format("/%s/", DATA_FOLDER));
            if (!dataFolder.exists()) {
                if (dataFolder.mkdirs()) {
                    result = dataFolder;
                } else {
                    Log.e(TAG, "create btdata folder failed");
                }
            } else {
                result = dataFolder;
            }
        }
        return result;
    }


    public void restart() {
        this.close();
        this.createFiles();
    }

    private static OutputStreamWriter createDataFile(File parentFolder, String fileName) {
        OutputStreamWriter result = null;
        File file = new File(parentFolder, String.format("/%s", fileName));
        if (file.exists()) {
            file.delete();
            Log.i(DataLog.TAG, String.format("--- file :%s/%s deleted ---", parentFolder.getAbsolutePath(), fileName));
        }
        try {
            if (file.createNewFile()) {
                result = new OutputStreamWriter(new FileOutputStream(file));
                Log.i(DataLog.TAG, String.format("+++ new file :%s/%s created +++", parentFolder.getAbsolutePath(), fileName));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void write(OutputStreamWriter writer, String data) {
        if (writer != null && !TextUtils.isEmpty(data)) {
            try {
                writer.append(data);
                // writer.flush();
                if (count++ % 50 == 0) {
                    writer.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void write(OutputStreamWriter writer, byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte item : data) {
            sb.append(String.format("%02X ", item));
        }
        //sb.append("\r\n");
        write(writer, sb.toString());
    }

    public void writeInData(String data) {
        write(mInWriter, data);
    }

    public void writeInData(byte[] data) {
        write(mInWriter, data);
    }

    public void writeInLineData(String data) {
        if (!TextUtils.isEmpty(data)) {
            write(mInLineWriter, data + "\r\n");
        }
    }

    public void writeOutData(String data) {
        write(mOutWriter, data);
    }

    public void writeOutData(byte[] data) {
        write(mOutWriter, data);
    }

    static long count = 0;

    private static void closeStreamWriter(OutputStreamWriter writer) {
        if (null != writer) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        closeStreamWriter(mInWriter);
        closeStreamWriter(mInLineWriter);
        closeStreamWriter(mOutWriter);
        mInWriter = null;
        mInLineWriter = null;
        mOutWriter = null;
    }
}


