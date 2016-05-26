package com.ifingers.yunwb.bluetooth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TouchScreen {
    public static final int POINT_STATUS_DOWN = 7;

    public IrTouch mIrTouch = new IrTouch();

    public List<TouchPoint> mTouchUpList = new ArrayList<TouchPoint>();
    public List<TouchPoint> mTouchDownList = new ArrayList<TouchPoint>();
    public List<TouchPoint> mTouchMoveList = new ArrayList<TouchPoint>();

    public int mGuesture;
    public long TouchScreenID;
    public int mNumOfPoints;
    public int mSnapShot;
    public final int dataSize = 10;
    private int maxIdFromHardware = 255;
    private int idRound = 0;//count id by ourselves.
    private int lastId = 0;

    private HashMap<Integer, TouchPoint> downMap = new HashMap<>();
    private HashMap<Integer, TouchPoint> upMap = new HashMap<>();

    public TouchScreen() {
    }

    void parsePoints(int pointCount, int[] dataBuffer) {
        TouchPoint parsedPoint;
        downMap.clear();
        upMap.clear();

        for (int kk = 0; kk < pointCount; kk++) {
            parsedPoint = new TouchPoint();
            parsedPoint.pointStatus = (byte) dataBuffer[kk * dataSize];
            int id = dataBuffer[kk * dataSize + 1];
            if (lastId - id > 20) {
                //当当前ID和之前的ID相差20时，可以表示硬件那边ID达到最大后，重新从1开始计数了
                //因为正常情况，差值应该为1.用差值就可以避免判断硬件那边最大ID是31还是255了
                idRound++;
            }
            lastId = id;
            parsedPoint.pointId = id + idRound * maxIdFromHardware;

            parsedPoint.pointX = dataBuffer[kk * dataSize + 2] + dataBuffer[kk * dataSize + 3] * 256;
            parsedPoint.pointY = dataBuffer[kk * dataSize + 4] + dataBuffer[kk * dataSize + 5] * 256;
            parsedPoint.pointWidth = dataBuffer[kk * dataSize + 6] + dataBuffer[kk * dataSize + 7] * 256;
            parsedPoint.pointHeight = dataBuffer[kk * dataSize + 8] + dataBuffer[kk * dataSize + 9] * 256;
            parsedPoint.pointArea = parsedPoint.pointWidth * parsedPoint.pointHeight;

            //only keeps last point for one id at one time read
            //because IRMT TEST does this way.
            //and hardware points are not accurate. Many too closed point cause the bad result
            if (parsedPoint.pointStatus == POINT_STATUS_DOWN) {
                downMap.put(parsedPoint.pointId, parsedPoint);
            } else {
                upMap.put(parsedPoint.pointId, parsedPoint);
            }
        }

        if (downMap.size() > 0) {
            for (Map.Entry<Integer, TouchPoint> entry : downMap.entrySet()) {
                mTouchDownList.add(entry.getValue());
            }
        }

        if (upMap.size() > 0) {
            for (Map.Entry<Integer, TouchPoint> entry : upMap.entrySet()) {
                mTouchUpList.add(entry.getValue());
            }
        }
    }

    void setIrTouchFeature(int[] dataBuffer) {
        mIrTouch.mScreenXLED = dataBuffer[0] + dataBuffer[1] * 256;
        mIrTouch.mScreenYLED = dataBuffer[2] + dataBuffer[3] * 256;
        mIrTouch.mScreenLedInsert = dataBuffer[4];
        mIrTouch.mScreenLedDistance = dataBuffer[5] + dataBuffer[6] * 256;
        mIrTouch.mScreenMaxPoint = dataBuffer[7];
        mIrTouch.mScreenFrameRate = dataBuffer[8];
    }

    void setNumOfPoints(int NewNumOfPoints) {
        mNumOfPoints = NewNumOfPoints;
    }

    void setmGuesture(int newGuesture) {
        mGuesture = newGuesture;
    }

    void setID(long NewID) {
        TouchScreenID = NewID;
    }

    void setSnapShot(int Shot) {
        mSnapShot = Shot;
    }

    public class TouchPoint {
        public int pointId;
        public byte pointStatus;
        public int pointX;
        public int pointY;
        public int pointWidth;
        public int pointHeight;
        public int pointArea;
        public byte pointColor;
    }

    public class IrTouch {
        public int mScreenXLED;
        public int mScreenYLED;
        public int mScreenLedInsert;
        public int mScreenLedDistance;
        public int mScreenMaxPoint;
        public int mScreenFrameRate;
    }
}
