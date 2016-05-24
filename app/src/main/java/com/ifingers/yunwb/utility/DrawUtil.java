package com.ifingers.yunwb.utility;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * Created by Nick_PC on 2016/5/24.
 */
public class DrawUtil {
    final int ROTATE_STEP = 90;
    int rotateDegree = 360;

    public static DrawUtil getInstance() {
        return InstanceHolder.instance;
    }

    private static class InstanceHolder {
        private static DrawUtil instance = new DrawUtil();
    }

    public void rotateLeft() {
        rotateDegree -= ROTATE_STEP;
    }

    public void rotateRight() {
        rotateDegree += ROTATE_STEP;
    }

    private class Tuple<T1, T2> {
        private T1 item1;
        private T2 item2;

        public T1 getItem1() {
            return item1;
        }

        public T2 getItem2() {
            return item2;
        }

        Tuple(T1 value1, T2 value2) {
            item1 = value1;
            item2 = value2;
        }
    }

    private Tuple<Float, Float> getXY(float x, float y, int w, int h) {
        int rotateDirection = rotateDegree / ROTATE_STEP % 4;
        float resultX = x, resultY = y;
        switch (rotateDirection) {
            case -1:
            case 3:
                resultX = y;
                resultY = h - x;
                break;
            case -2:
            case 2:
//                resultX = w - h + x;
//                resultY = h - y;

                resultX = w - x;
                resultY = y;
                break;
            case -3:
            case 1:
                resultX = w - y;
                resultY = x;
                break;
            case 0:
                resultX = x;
                resultY = y;
                break;
        }

        return new Tuple<>(resultX, resultY);
    }

    public void drawCircle(Canvas canvas, float x, float y, float r, int w, int h, Paint paint) {
        Tuple<Float, Float> loc = getXY(x, y, w, h);
        canvas.drawCircle(loc.getItem1(), loc.getItem2(), r, paint);
    }

    public void moveTo(Path path, float x, float y, int w, int h) {
        Tuple<Float, Float> loc = getXY(x, y, w, h);
        path.moveTo(loc.getItem1(), loc.getItem2());
    }

    public void lineTo(Path path, float x, float y, int w, int h) {
        Tuple<Float, Float> loc = getXY(x, y, w, h);
        path.lineTo(loc.getItem1(), loc.getItem2());
    }
}
