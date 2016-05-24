package com.ifingers.yunwb.services;

import android.app.Activity;

import com.ifingers.yunwb.dao.Path;
import com.ifingers.yunwb.dao.TouchPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by SFY on 2/26/2016.
 */
public class WBMockDevice implements IWBDevice {
    private static WBMockDevice ourInstance = new WBMockDevice();
    private WBDeviceStatusHandler statusHandler = null;
    private WBDeviceDataHandler dataHandler = null;
    private ArrayList<Path> paths = new ArrayList<>();
    private Timer timer = null;

    private short x;
    private short y;
    private int currentStep;
    private int totalStep = 0;
    private byte id;
    private short x2;
    private short y2;
    private byte id2;

    public static WBMockDevice getInstance() {
        return ourInstance;
    }

    private WBMockDevice() {
    }
    @Override
    public void init(Activity activity, WBDeviceStatusHandler handler) {
        this.statusHandler = handler;
        x = 0;
        y = 0;
        currentStep = 0;
        id = 1;
        x2 = 0;
        y2 = 32767;
        id2 = 2;
    }

    @Override
    public void disconnect() {
    }

    @Override
    public void setDataHandler(WBDeviceDataHandler handler) {
        this.dataHandler = handler;
    }
    @Override
    public void connect(String deviceName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //mockNothing();
                    mock2Path();
                } catch (Exception e) {
                }
            }
        }).start();
    }

    private Map<Integer, List<TouchPoint>> triage(List<TouchPoint> list) {
        Map<Integer, List<TouchPoint>> ret = new HashMap<>();
        for (TouchPoint p : list) {
            int id = p.getPointId();
            List<TouchPoint> plist = ret.get(id);
            if (plist == null) {
                plist = new ArrayList<>();
                ret.put(id, plist);
            }
            plist.add(p);
        }

        return ret;
    }

    private void mockDisconnect() throws Exception {
        Thread.sleep(1000);
        statusHandler.onDisconnected();
    }

    private void mockLostConnection() throws Exception {
        Thread.sleep(1000);
        statusHandler.onConnected();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                    dataHandler.onLostConnection();
                } catch (Exception e) {

                }
            }
        }).start();
    }

    private void mockSnapshot() throws Exception {
        Thread.sleep(1000);
        statusHandler.onConnected();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                    dataHandler.onSnapshotClicked();
                } catch (Exception e) {

                }
            }
        }).start();
    }

    private void mockNotFound() throws Exception {
        Thread.sleep(1000);
        statusHandler.onDeviceNotFound();
    }

    private void mockNothing() {
        statusHandler.onConnected();
    }

    //draw horizontal line one by one from up to down
    private void mock1Path() throws Exception {
        Thread.sleep(1000);
        statusHandler.onConnected();

        if (timer != null) {
            timer.cancel();;
            timer.purge();
        }

        y = 1000;
        totalStep = 200;
        final int span = 150;

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (WBMockDevice.this) {
                    if (currentStep % totalStep == 0) {
                        ArrayList<TouchPoint> all = new ArrayList<TouchPoint>();
                        //touch down
                        TouchPoint p = new TouchPoint();
                        p.setPointY(y);
                        p.setPointX(x);
                        p.setPointId(id);
                        p.setPointColor((byte) 1);

                        all.add(p);

                        if (dataHandler != null)
                            dataHandler.onTouchDown(triage(all));

                        x += span;
                    } else if (currentStep % totalStep == totalStep - 1) {
                        //touch up
                        ArrayList<TouchPoint> all = new ArrayList<TouchPoint>();

                        TouchPoint p = new TouchPoint();
                        p.setPointY(y);
                        p.setPointX(x);
                        p.setPointId(id);
                        p.setPointColor((byte) 1);

                        all.add(p);

                        if (dataHandler != null)
                            dataHandler.onTouchUp(triage(all));
                        id ++;
                        y += 1000;
                        if (y > 30767) {
                            y = (short)(new Random().nextInt() % 1000);
                        }
                        x = 0;
                    } else {
                        //move
                        ArrayList<TouchPoint> all = new ArrayList<TouchPoint>();

                        TouchPoint p = new TouchPoint();
                        p.setPointY(y);
                        p.setPointX(x);
                        p.setPointId(id);
                        p.setPointColor((byte) 1);

                        all.add(p);

                        if (dataHandler != null)
                            dataHandler.onTouchMove(triage(all));

                        x += span;
                    }
                }

                currentStep++;
            }
        }, 2000, 20);
    }

    private void mock2Path() throws Exception {
        Thread.sleep(1000);
        statusHandler.onConnected();
        totalStep = 200;

        if (timer != null) {
            timer.cancel();;
            timer.purge();
        }

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                x += 60;
                if (x > 31767) {
                    x = 1000;
                    y += 1000;
                    if (y > 31767) {
                        y = 1000;
                    }
                }

                x2 += 60;
                if (x2 > 31767) {
                    x2 = 1000;
                    y2 -= 1000;
                    if (y2 < 1000) {
                        y2 = 31767;
                    }
                }

                synchronized (WBMockDevice.this) {
                    if (currentStep % totalStep == 0) {
                        ArrayList<TouchPoint> all = new ArrayList<TouchPoint>();
                        //touch down
                        TouchPoint p = new TouchPoint();
                        p.setPointY(y);
                        p.setPointX(x);
                        p.setPointId(id);
                        p.setPointColor((byte) 1);

                        all.add(p);

                        p = new TouchPoint();
                        p.setPointY(y2);
                        p.setPointX(x2);
                        p.setPointId(id2);
                        p.setPointColor((byte) 2);

                        all.add(p);

                        if (dataHandler != null)
                            dataHandler.onTouchDown(triage(all));
                    } else if (currentStep % totalStep == totalStep - 1) {
                        //touch up
                        ArrayList<TouchPoint> all = new ArrayList<TouchPoint>();

                        TouchPoint p = new TouchPoint();
                        p.setPointY(y);
                        p.setPointX(x);
                        p.setPointId(id);
                        p.setPointColor((byte) 1);

                        all.add(p);
                        id += 2;

                        p = new TouchPoint();
                        p.setPointY(y2);
                        p.setPointX(x2);
                        p.setPointId(id2);
                        p.setPointColor((byte) 2);

                        all.add(p);
                        id2 += 2;
                        if (dataHandler != null)
                            dataHandler.onTouchUp(triage(all));
                    } else {
                        //move
                        ArrayList<TouchPoint> all = new ArrayList<TouchPoint>();

                        TouchPoint p = new TouchPoint();
                        p.setPointY(y);
                        p.setPointX(x);
                        p.setPointId(id);
                        p.setPointColor((byte) 1);

                        all.add(p);

                        p = new TouchPoint();
                        p.setPointY(y2);
                        p.setPointX(x2);
                        p.setPointId(id2);
                        p.setPointColor((byte) 2);

                        all.add(p);

                        if (dataHandler != null)
                            dataHandler.onTouchMove(triage(all));
                    }
                }

                currentStep++;
            }
        }, 500, 20);
    }
}
