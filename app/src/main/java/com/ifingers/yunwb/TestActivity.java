package com.ifingers.yunwb;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.ifingers.yunwb.bluetooth.BLCommService;
import com.ifingers.yunwb.bluetooth.IrmtInterface;
import com.ifingers.yunwb.bluetooth.TouchScreen;
import com.ifingers.yunwb.services.IWBDevice;
import com.ifingers.yunwb.utility.WhiteboardTaskContext;

import java.util.List;

public class TestActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private EditText edit = null;
    private EditText name = null;
    private Button button = null;
    private SurfaceView surface = null;
    private WhiteboardTaskContext globalConfig = WhiteboardTaskContext.getInstance();

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    edit.setText("connected" + "\r\n" + edit.getText());
                    break;
                case 1:
                    edit.setText("disconnected" + "\r\n" + edit.getText());
                    break;
                case 2:
                    edit.setText("Device not found" + "\r\n" + edit.getText());
                    break;
                case 3:
                    edit.setText("lost communication" + "\r\n" + edit.getText());
                    break;
                case 4:
                    StringBuffer sb = new StringBuffer();
                    List<TouchScreen.TouchPoint> points = (List<TouchScreen.TouchPoint>) msg.obj;
                    for (TouchScreen.TouchPoint tp : points) {
                        int x = (int)(tp.pointX);
                        int y = (int)(tp.pointY);
                        int w = (int)(tp.pointWidth);
                        int h = (int)(tp.pointHeight);
                        sb.append(String.format("x = %d, y = %d, w = %d, h = %d\r\n", x, y, w, h));//converse wh
                    }
                    edit.setText(sb.toString() + "\r\n" + edit.getText());
                    break;
            }
        }
    };

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        IWBDevice device = globalConfig.getWbDevice();
        device.init(this, new IWBDevice.WBDeviceStatusHandler() {
            @Override
            public void onConnected() {

            }

            @Override
            public void onDisconnected() {

            }

            @Override
            public void onDeviceNotFound() {

            }
        });
        device.connect("test");

        Canvas canvas = holder.lockCanvas();
        globalConfig.setWhiteBoardWidth(canvas.getWidth());
        globalConfig.setWhiteBoardHeight(canvas.getHeight());
        holder.unlockCanvasAndPost(canvas);

        //HostWbTask hostWbTask = new HostWbTask(null, null, holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        final BLCommService sdkService = new BLCommService(this, new IrmtInterface() {
            @Override
            public void onGestureGet(int i) {
            }

            @Override
            public void onTouchUp(List<TouchScreen.TouchPoint> list) {
                Message msg = new Message();
                msg.what = 4;
                msg.obj = list;
                mHandler.sendMessage(msg);
            }

            @Override
            public void onTouchDown(List<TouchScreen.TouchPoint> list) {
                Message msg = new Message();
                msg.what = 4;
                msg.obj = list;
                mHandler.sendMessage(msg);
            }

            @Override
            public void onTouchMove(List<TouchScreen.TouchPoint> list) {
                //this event never comes, because SDK don't want to implement...
                //so we need mock at onTouchDown
            }

            @Override
            public void onSnapshot(int i) {

            }

            @Override
            public void onIdGet(long l) {
            }

            @Override
            public void onError(int i) {
                if (i == 1) {
                    Message msg = new Message();
                    msg.what = 1;
                    mHandler.sendMessage(msg);
                } else if (i == 2) {
                    Message msg = new Message();
                    msg.what = 3;
                    mHandler.sendMessage(msg);
                } else if (i == 3) {
                    Message msg = new Message();
                    msg.what = 2;
                    mHandler.sendMessage(msg);
                }
            }

            @Override
            public void onBLconnected() {
                Message msg = new Message();
                msg.what = 0;
                mHandler.sendMessage(msg);
            }
        });

        edit = (EditText) findViewById(R.id.result_text);
        button = (Button) findViewById(R.id.test_button);
        name = (EditText) findViewById(R.id.name_text);

        sdkService.enable();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sdkService.connect(name.getText().toString() );
            }
        });
//
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    while (true) {
//                        Map<Integer, List<TouchPoint>> p = WBDevice.getInstance().getPointCache();
//                        String str = "";
//                        if (p.size() > 0) {
//                            str = "path count = " + p.size() + "\r\n";
//                            for (Map.Entry<Integer, List<TouchPoint>> entry : p.entrySet()) {
//                                Integer pid = entry.getKey();
//                                str += "pid = " + pid + " point count = " + entry.getValue().size() + "\r\n";
//                            }
//
//                            Message msg = new Message();
//                            msg.what = 4;
//                            msg.obj = str;
//                            mHandler.sendMessage(msg);
//                        }
//                        Thread.sleep(2000);
//                    }
//                } catch (Exception e) {
//
//                }
//            }
//        }).start();
    }

    class LoopThread extends Thread{

        SurfaceHolder surfaceHolder;
        Context context;
        boolean isRunning;
        float radius = 10f;
        Paint paint;

        public LoopThread(SurfaceHolder surfaceHolder,Context context){

            this.surfaceHolder = surfaceHolder;
            this.context = context;
            isRunning = false;

            paint = new Paint();
            paint.setColor(Color.YELLOW);
            paint.setStyle(Paint.Style.STROKE);
        }

        @Override
        public void run() {

            Canvas c = null;

            while(isRunning){

                try{
                    synchronized (surfaceHolder) {

                        c = surfaceHolder.lockCanvas(null);
                        doDraw(c);
                        //通过它来控制帧数执行一次绘制后休息50ms
                        Thread.sleep(50);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    surfaceHolder.unlockCanvasAndPost(c);
                }

            }

        }

        public void doDraw(Canvas c){

            //这个很重要，清屏操作，清楚掉上次绘制的残留图像

            c.translate(200, 200);
            c.drawCircle(0,0, radius++, paint);

            if(radius > 100){
                radius = 10f;
            }

        }

    }
}
