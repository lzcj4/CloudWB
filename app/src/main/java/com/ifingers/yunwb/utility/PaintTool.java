package com.ifingers.yunwb.utility;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import com.ifingers.yunwb.dao.TouchPoint;

import java.util.List;

/**
 * Created by SFY on 2016/3/3.
 */
public class PaintTool {
    private static PaintTool ourInstance = new PaintTool();
    public static PaintTool getInstance() {
        return ourInstance;
    }

    private WhiteboardTaskContext config = WhiteboardTaskContext.getInstance();
    private Paint paint;
    private float scaleFactorX = 0;
    private float scaleFactorY = 0;
    private Paint bitmapPaint;
    private float penWidthFactor = 4.5f / 830; //in 1920 x 1080
    private float penWidth = 3.f;

    private PaintTool() {
        paint = new Paint();
        paint.setStrokeWidth(penWidth);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);

        bitmapPaint = new Paint();
        bitmapPaint.setStrokeWidth(penWidth);
        bitmapPaint.setAntiAlias(true);
        bitmapPaint.setStyle(Paint.Style.STROKE);
        bitmapPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void init(int boardWidth, int boardHeight) {
        config.setWhiteBoardWidth(boardWidth);
        config.setWhiteBoardHeight(boardHeight);
        scaleFactorX = config.getScaleFactorX();
        scaleFactorY = config.getScaleFactorY();
        penWidth = penWidthFactor * boardWidth;
        paint.setStrokeWidth(penWidth);
    }

    public Paint getBitmapPaint() {
        return bitmapPaint;
    }

    private float scaleX(float x) {
        return x * scaleFactorX;
    }

    private float scaleY(float y) {
        return y * scaleFactorY;
    }

    public void erase(Canvas canvas, List<TouchPoint> points) {
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        for (TouchPoint point : points) {
            float x = scaleX(point.getPointX());
            float y = scaleY(point.getPointY());
            float w = scaleX(point.getPointWidth());
            float h = scaleY(point.getPointHeight());
            canvas.drawCircle(x, y, (w + h) / 4, paint);
        }
    }

    public void pen(Canvas canvas, TouchPoint startPoint, List<TouchPoint> points) {
        android.graphics.Path path = new android.graphics.Path();
        float x = scaleX(startPoint.getPointX());
        float y = scaleY(startPoint.getPointY());
        path.moveTo(x, y);
        paint.setColor(startPoint.getSystemColor());
        paint.setStyle(Paint.Style.STROKE);

        for (TouchPoint point : points) {
            x = scaleX(point.getPointX());
            y = scaleY(point.getPointY());
            float w = scaleX(point.getPointWidth());
            float h = scaleY(point.getPointHeight());

            path.lineTo(x, y);
        }

        canvas.drawPath(path, paint);
    }
}
