package com.ifingers.yunwb;

import android.app.Application;

import com.tencent.bugly.Bugly;

/**
 * Created by Nick_PC on 2016/5/24.
 */
public class AppApplication extends Application {
    @Override
    public void onCreate() {
        Bugly.init(getApplicationContext(), "900031466", false);
        super.onCreate();
    }
}
