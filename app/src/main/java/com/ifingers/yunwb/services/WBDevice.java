package com.ifingers.yunwb.services;

import android.app.Activity;

import com.ifingers.yunwb.bluetooth.BLCommService;
import com.ifingers.yunwb.bluetooth.IrmtInterface;
import com.ifingers.yunwb.bluetooth.TouchScreen;
import com.ifingers.yunwb.dao.TouchPoint;
import com.ifingers.yunwb.utility.SingleLogger;
import com.ifingers.yunwb.utility.WhiteboardTaskContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 *
 * Created by SFY on 2016/2/21.
 */
public class WBDevice implements IWBDevice {
    private static WBDevice ourInstance = new WBDevice();

    public static WBDevice getInstance() {
        return ourInstance;
    }

    private BLCommService sdkService = null;
    private WBDeviceStatusHandler statusHandler = null;
    private WBDeviceDataHandler dataHandler = null;
    private boolean snapshotButtonPressed = false;
    private HashSet<Integer> movingIdSet = new HashSet<>();
    private WhiteboardTaskContext config = null;
    private int ruberMaxSize;
    private int ruberMinSize;
    private int penMaxSize;
    private int penMinSize;

    private WBDevice() {
    }

    @Override
    public void init(Activity activity, WBDeviceStatusHandler handler) {
        this.statusHandler = handler;

        //one time setting
        if (sdkService == null) {
            config = WhiteboardTaskContext.getInstance();
            ruberMaxSize = config.getRuberMaxSize();
            ruberMinSize = config.getRuberMinSize();
            penMaxSize = config.getPenMaxSize();
            penMinSize = config.getPenMinSize();

            sdkService = new BLCommService(activity, new IrmtInterface() {
                @Override
                public void onGestureGet(int i) {
                }

                @Override
                public void onTouchUp(List<TouchScreen.TouchPoint> list) {
                    if (dataHandler != null) {
                        Map<Integer, List<TouchPoint>> map = triage(list, false);
                        movingIdSet.removeAll(map.keySet());
                        dataHandler.onTouchUp(map);
                    }
                }

                @Override
                public void onTouchDown(List<TouchScreen.TouchPoint> list) {
                    if (dataHandler != null) {
                        Map<Integer, List<TouchPoint>> map = triage(list, true);
                        Map<Integer, List<TouchPoint>> moveMap = new HashMap<>();
                        Map<Integer, List<TouchPoint>> downMap = new HashMap<>();
                        for (Map.Entry<Integer, List<TouchPoint>> entry : map.entrySet()) {
                            int id = entry.getKey();
                            List<TouchPoint> path = entry.getValue();
                            if (movingIdSet.contains(id)) {
                                moveMap.put(id, path);
                            } else {
                                downMap.put(id, path);
                                movingIdSet.add(id);
                            }
                        }

                        if (moveMap.size() > 0) {
                            dataHandler.onTouchMove(moveMap);
                        }
                        if (downMap.size() > 0) {
                            dataHandler.onTouchDown(downMap);
                        }
                    }
                }

                @Override
                public void onTouchMove(List<TouchScreen.TouchPoint> list) {
                    //this event never comes, because SDK don't want to implement...
                    //so we need mock at onTouchDown
                }

                @Override
                public void onSnapshot(int i) {
                    if (i == 0x11) {
                        if (snapshotButtonPressed) {
                            if (WBDevice.this.dataHandler != null)
                                WBDevice.this.dataHandler.onSnapshotClicked();
                        }
                        snapshotButtonPressed = true;
                    } else if (i == 0x10) {
                        snapshotButtonPressed = false;
                    }
                }

                @Override
                public void onIdGet(long l) {
                }

                @Override
                public void onError(int i) {
                    if (i == BLCommService.BL_ERROR_CONN_FAILED) {
                        WBDevice.this.statusHandler.onDisconnected();
                    } else if (i == BLCommService.BL_ERROR_CONN_LOST) {
                        if (WBDevice.this.dataHandler != null)
                            WBDevice.this.dataHandler.onLostConnection();
                    } else if (i == BLCommService.BL_ERROR_DEV_NOT_FOUND) {
                        WBDevice.this.statusHandler.onDeviceNotFound();
                    }
                }

                @Override
                public void onBLconnected() {
                    WBDevice.this.statusHandler.onConnected();
                }
            });
        }

        sdkService.enable();
    }

    @Override
    public void disconnect() {
        sdkService.disconnect();
    }

    @Override
    public void connect(String deviceName) {
        sdkService.connect(deviceName);
    }

    @Override
    public void setDataHandler(WBDeviceDataHandler handler) {
        this.dataHandler = handler;
    }

    private TouchPoint createPointFrom(TouchScreen.TouchPoint p) {
        TouchPoint tp = new TouchPoint();
        if ((p.pointArea >= ruberMinSize && p.pointArea <= ruberMaxSize))
            tp.setPointColor((byte)3);
        else
            tp.setPointColor((byte)1);
        tp.setPointId(p.pointId);
        if(config.isWhConverse()) {
            tp.setPointHeight((short) p.pointWidth);
            tp.setPointWidth((short) p.pointHeight);
        } else {
            tp.setPointHeight((short) p.pointHeight);
            tp.setPointWidth((short) p.pointWidth);
        }

        tp.setPointX((short) p.pointX);
        tp.setPointY((short) p.pointY);

        return tp;
    }

    private boolean isInRange(int area) {
        if (area >= penMinSize && area <= penMaxSize)
            return true;
        else if (area >= ruberMinSize && area <= ruberMaxSize)
            return true;
        else
            return false;
    }

    private Map<Integer, List<TouchPoint>> triage(List<TouchScreen.TouchPoint> list, boolean isDown) {
        Map<Integer, List<TouchPoint>> ret = new HashMap<>();
        for (TouchScreen.TouchPoint p : list) {
            if (isDown && !isInRange(p.pointArea)) {
                //no need check area of up point. it cause the path can't be ended if up point area is too big
                continue;
            }
            TouchPoint point = createPointFrom(p);
            int id = point.getPointId();
            List<TouchPoint> plist = ret.get(id);
            if (plist == null) {
                plist = new ArrayList<>();
                ret.put(id, plist);
            }
            plist.add(point);
        }

        return ret;
    }
}
