package com.ifingers.yunwb.utility;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.view.WindowManager;

/**
 * Created by Nick_PC on 2016/5/16.
 */
public class WakeLock {

    private final Context mContext;
    PowerManager.WakeLock mScreenLock;
    WifiManager.WifiLock mWifiLock;

    public WakeLock(Context context) {
        mContext = context;
        initLocks();
    }

    private void initLocks() {
        PowerManager powerMgr = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        powerMgr.newWakeLock(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, "scrren_lock");

        WifiManager wifiMgr = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mWifiLock = wifiMgr.createWifiLock("wifi_lock");
    }

    public void lockScreen() {
        if (null != mScreenLock && !mScreenLock.isHeld()) {
            mScreenLock.acquire();
        }
    }

    public void lockWifi() {
        if (null != mWifiLock && !mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }
    }

    public void lockAll() {
        lockScreen();
        lockWifi();
    }

    public void unlockScreen() {
        if (null != mScreenLock && mScreenLock.isHeld()) {
            mScreenLock.release();
        }
    }

    public void unlockWifi() {
        if (null != mWifiLock && mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }

    public void unlockAll() {
        unlockScreen();
        unlockWifi();
    }
}
