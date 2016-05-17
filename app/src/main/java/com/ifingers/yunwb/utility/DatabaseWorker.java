package com.ifingers.yunwb.utility;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Macoo on 2/4/2016.
 */

public class DatabaseWorker extends SQLiteOpenHelper {

    private SQLiteDatabase mDatabase;
    private Context context;

    public DatabaseWorker(Context context) {
        super(context, String.format("/data/data/%s/yunwb.db", context.getApplicationContext().getPackageName()), null, 3);
        this.context = context;
    }

    public void createDataBase() throws IOException {

        boolean dbExist = checkDataBase();
        if (dbExist) {
//do nothing – database already exist
        } else {

//By calling this method and empty database will be created into the default system path
//of your application so we are gonna be able to overwrite that database with our database.
            this.getReadableDatabase();

            try {

                copyDataBase();

            } catch (IOException e) {

                throw new Error("Error copying database");

            }
        }
    }

    private boolean checkDataBase() {

        SQLiteDatabase checkDB = null;

        try {
            String myPath = String.format("/data/data/%s/yunwb.db", context.getApplicationContext().getPackageName());
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);

        } catch (SQLiteException e) {

//database does’t exist yet.

        }

        if (checkDB != null) {

            checkDB.close();

        }

        return checkDB != null ? true : false;
    }

    private void copyDataBase() throws IOException {

//Open your local db as the input stream
        InputStream myInput = context.getAssets().open("yunwb.db");

// Path to the just created empty db
        String outFileName = String.format("/data/data/%s/yunwb.db", context.getApplicationContext().getPackageName());

//Open the empty db as the output stream
        OutputStream myOutput = new FileOutputStream(outFileName);

//transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;

        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }

//Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        mDatabase = db;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }
}

