package com.ifingers.yunwb.services;

import android.app.Activity;

import com.ifingers.yunwb.dao.TouchPoint;

import java.util.List;
import java.util.Map;

/**
 * Created by SFY on 2/28/2016.
 */
public interface IWBDevice {
    void init(Activity activity, WBDeviceStatusHandler handler);
    void connect(String deviceName);
    void setDataHandler(WBDeviceDataHandler handler);
    void disconnect();

    interface WBDeviceStatusHandler {
        void onConnected();
        void onDisconnected();//尝试连接时出错
        void onDeviceNotFound();
    }

    interface WBDeviceDataHandler {
        void onLostConnection();//通信过程中出错
        void onSnapshotClicked();
        void onTouchDown(Map<Integer, List<TouchPoint>> points);
        void onTouchUp(Map<Integer, List<TouchPoint>> points);
        void onTouchMove(Map<Integer, List<TouchPoint>> points);
    }
}
