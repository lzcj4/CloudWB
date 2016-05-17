package com.ifingers.yunwb.utility;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

/**
 * get local conference records, path etc
 * Created by Macoo on 2/18/2016.
 */
public class LocalConferenceRecords {

    public static String getRootPath(Context context){
        return context.getApplicationInfo().dataDir + "/history/";
    }

    public static ArrayList<Integer> retrieveConferenceYears(Context context) throws ParseException {
        File root = new File(getRootPath(context));
        String[] dirs = root.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });

        ArrayList<Integer> rtn = new ArrayList<>();
        if (dirs != null)
            for (String dir : dirs) {
                String array[] = dir.split("_");
                SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");
                Date date = sd.parse(array[0]);
                Calendar calender = Calendar.getInstance();
                calender.setTime(date);
                if (!rtn.contains(calender.get(Calendar.YEAR))){
                    rtn.add(calender.get(Calendar.YEAR));
                }
            }

        Collections.sort(rtn, Collections.reverseOrder());
        return rtn;
    }

    public static ArrayList<String> retrieveRecords(String year, Context context){
        String rootStr = getRootPath(context);
        File root = new File(rootStr);
        String[] dirs = root.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });

        ArrayList<String> names = new ArrayList<>();
        for (String dir: dirs) {
            if (dir.startsWith(year)){
                File rootDate = new File(rootStr + dir);
                String conferenceFolders[] = rootDate.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File current, String name) {
                        return new File(current, name).isDirectory();
                    }
                });
                for(String folder : conferenceFolders){
                    String name = dir + "_" + folder;
                    names.add(name);
                }
            }
        }

        return names;
    }
}
