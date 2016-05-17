package com.ifingers.yunwb.dao;

import android.graphics.Color;

/**
 * class for wb point
 * Created by Macoo on 2/3/2016.
 */
public class TouchPoint {
    private int pointId;
    private short pointX;
    private short pointY;
    private short pointWidth;
    private short pointHeight;
    private byte pointColor;

    public int getPointId() {
        return pointId;
    }

    public void setPointId(int pointId) {
        this.pointId = pointId;
    }

    public short getPointX() {
        return pointX;
    }

    public void setPointX(short pointX) {
        this.pointX = pointX;
    }

    public short getPointY() {
        return pointY;
    }

    public void setPointY(short pointY) {
        this.pointY = pointY;
    }

    public short getPointWidth() {
        return pointWidth;
    }

    public void setPointWidth(short pointWidth) {
        this.pointWidth = pointWidth;
    }

    public short getPointHeight() {
        return pointHeight;
    }

    public void setPointHeight(short pointHeight) {
        this.pointHeight = pointHeight;
    }

    public byte getPointColor() {
        return pointColor;
    }

    public int getSystemColor(){
        if (pointColor == 1)
            return Color.BLACK;
        else if (pointColor == 2)
            return Color.RED;
        else if (pointColor == 3)
            return Color.WHITE;
        else
            return Color.BLACK;
    }

    public void setPointColor(byte pointColor) {
        this.pointColor = pointColor;
    }
}
