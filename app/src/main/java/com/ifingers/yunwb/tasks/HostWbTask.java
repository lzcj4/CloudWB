package com.ifingers.yunwb.tasks;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

import com.ifingers.yunwb.R;
import com.ifingers.yunwb.WhiteBoardActivity;
import com.ifingers.yunwb.dao.ConferenceDao;
import com.ifingers.yunwb.dao.Frame;
import com.ifingers.yunwb.dao.Path;
import com.ifingers.yunwb.dao.TouchPoint;
import com.ifingers.yunwb.services.IWBDevice;
import com.ifingers.yunwb.utility.LocalConferenceRecords;
import com.ifingers.yunwb.utility.PaintTool;
import com.ifingers.yunwb.utility.ServerAPI;
import com.ifingers.yunwb.utility.ServerError;
import com.ifingers.yunwb.utility.SingleLogger;
import com.ifingers.yunwb.utility.WbMessager;
import com.ifingers.yunwb.utility.WhiteboardTaskContext;

import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 *
 * Created by Macoo on 2/24/2016.
 */
public class HostWbTask implements Runnable, ICommonTask<Bitmap>, IWBDevice.WBDeviceDataHandler, IWBDevice.WBDeviceStatusHandler {

    private String TAG = "HostWbTask";
    private WhiteboardTaskContext globalConfig = WhiteboardTaskContext.getInstance();
    private boolean isAlive;
    private SurfaceHolder holder;
    private String meetingUrl;
    private String conferenceId;
    private TaskManagers taskManager;
    private int roundSinceLastSnapshot = 1;
    private int snapshotTrigger;
    private int heartbeatInterval = globalConfig.getPostHeartbeatInterval();
    private ServerAPI serverAPI = ServerAPI.getInstance();
    private IWBDevice device = globalConfig.getWbDevice();
    private PathCache pathCache = new PathCache();
    private Bitmap bitmap = null;
    private Canvas canvas = null;
    private PaintTool paintTool = PaintTool.getInstance();
    private WhiteBoardActivity activity;
    private String date;
    private String name;
    private ConferenceDao conferenceDao = new ConferenceDao();
    private String deviceName;

    public HostWbTask(String url, String conferenceId, SurfaceHolder holder, WhiteBoardActivity activity, String date, String name, String deviceName) {
        this.meetingUrl = url;
        this.conferenceId = conferenceId;
        this.holder = holder;
        this.activity = activity;
        this.date = date;
        this.name = name;
        this.deviceName = deviceName;
        conferenceDao.setConferenceId(conferenceId);//init id at least

        taskManager = TaskManagers.getInstance();
        snapshotTrigger = globalConfig.getPostSnapshotInterval() / globalConfig.getPostFrameInterval();
        if (snapshotTrigger == 0)
            snapshotTrigger = 1;

        Canvas tmp = holder.lockCanvas();
        int w = tmp.getWidth();
        int h = tmp.getHeight();
        paintTool.init(w, h);

        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        tmp.drawColor(Color.WHITE);
        holder.unlockCanvasAndPost(tmp);

        canvas = new Canvas(bitmap);
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));

        canvas.drawColor(Color.WHITE);

        device.setDataHandler(this);
    }

    @Override
    public void start() {
        isAlive = true;
    }

    @Override
    public void stop() {
        //don't block the gui thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                device.disconnect();

                try {
                    int count = 0;
                    final int max = 5;
                    do {
                        ServerAPI.BaseData data = serverAPI.endConference(conferenceDao);
                        int code = data.getCode();
                        if (code == ServerError.OK || code == ServerError.MEETING_IS_NOT_ALIVE) {
                            break;
                        } else {
                            Thread.sleep(400);
                        }
                    } while (++count < max);
                    if (count == max) {
                        Log.e(TAG, "try endConference failed " + max + " times");
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Resources res = activity.getResources();
                                Toast.makeText(activity, String.format(res.getString(R.string.network_slow_text), "终止会议失败"), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                }
            }
        }).start();

        isAlive = false;
    }

    @Override
    public Bitmap getData() {
        return bitmap;
    }

    @Override
    public void forcePush() {
        renderBitmap();
    }

    @Override
    public void run() {
        isAlive = true;
        long lastTick = 0;
        int maxFailureCount = globalConfig.getMaxContinuousFailureCount();
        int failureCount = 0;
        List<Path> accumulatedPath = new ArrayList<>();
        String userId = globalConfig.getUserId();

        while (isAlive) {
            try {
                sleepInternal();
                HashMap<Integer, Path> paths = pathCache.fetchAndPurge();

                long nowTick = new Date().getTime();
                if (paths.size() == 0) {
                    if (nowTick - lastTick > heartbeatInterval) {
                        ServerAPI.BaseData data = serverAPI.postHeartbeat(meetingUrl, conferenceId);
                        int code = data.getCode();
                        if (data.getCode() == ServerError.OK) {
                            lastTick = nowTick;
                            failureCount = 0;
                        } else if (code == ServerError.MEETING_IS_NOT_ALIVE) {
                            handleConferenceFinished(TaskMsg.ConferenceFinished);
                            break;
                        } else {
                            failureCount++;
                        }
                    }
                }
                else {
                    for (Path path : paths.values()) {
                        accumulatedPath.add(path);
                    }

                    Frame frame = new Frame();
                    for (Path path : accumulatedPath) {
                        frame.addPath(path);
                    }

                    ServerAPI.BaseData data = serverAPI.postFrame(meetingUrl, conferenceId, frame);
                    int code = data.getCode();
                    if (code == ServerError.OK) {
                        failureCount = 0;
                        accumulatedPath.clear();
                    } else if (code == ServerError.MEETING_IS_NOT_ALIVE) {
                        handleConferenceFinished(TaskMsg.ConferenceFinished);
                        break;
                    } else {
                        failureCount++;
                    }
                }

                ServerAPI.MeetingData meetingData = serverAPI.shoutIAmOnline(meetingUrl, conferenceId, userId);
                int code = meetingData.getCode();
                if (code == ServerError.OK) {
                    conferenceDao.fill(meetingData.getMeeting().getConference());
                    globalConfig.setMeetingInfo(meetingData.getMeeting());
                    saveResourcesToLocal(conferenceDao.getImages());
                } else if (code == ServerError.MEETING_IS_NOT_ALIVE) {
                    handleConferenceFinished(TaskMsg.ConferenceFinished);
                    break;
                } else {
                    failureCount++;
                }

                if (roundSinceLastSnapshot == 0){
                    Log.i(TAG, "post snapshot");
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
                    ServerAPI.BaseData data = serverAPI.postSnapshot(meetingUrl, conferenceId, bos.toByteArray());
                    if (data.getCode() == ServerError.OK) {
                        failureCount = 0;
                    } else if (code == ServerError.MEETING_IS_NOT_ALIVE) {
                        handleConferenceFinished(TaskMsg.ConferenceFinished);
                        break;
                    } else {
                        failureCount++;
                        roundSinceLastSnapshot = -1;//trigger at next time;
                    }
                }

                roundSinceLastSnapshot = (roundSinceLastSnapshot + 1) % snapshotTrigger;

                if (failureCount > maxFailureCount) {
                    do {
                        handleNetworkException();
                    }
                    while(!networkExceptionHandled);
                    failureCount = 0;
                }
            } catch (InterruptedException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }

    private Object waitForUserInputLock = new Object();
    private boolean networkExceptionHandled = false;

    private void handleNetworkException() throws InterruptedException {
        final String title = activity.getResources().getString(R.string.prompt_title_info);
        final String content = activity.getResources().getString(R.string.server_connection_down);
        final String no = "结束";
        final String yes = "重试";
        networkExceptionHandled = false;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                WbMessager.show(activity, title, content, no, yes,
                        //no click, exit meeting
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                //save current content
                                taskManager.handleTaskMsg(HostWbTask.this, TaskMsg.WhiteboardHwSnapshot);
                                handleConferenceFinished(TaskMsg.ConferenceFinished);
                                networkExceptionHandled = true;
                                synchronized (waitForUserInputLock) {
                                    waitForUserInputLock.notify();
                                }
                            }
                        },
                        //yes click, try connect
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                activity.showProgress(true);
                                //try reconnect by send heartbeat. new thread to avoid block progress bar

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ServerAPI.BaseData data = serverAPI.postHeartbeat(meetingUrl, conferenceId);
                                        if (data.getCode() == ServerError.OK) {
                                            networkExceptionHandled = true;
                                        } else {
                                            networkExceptionHandled = false;
                                        }
                                        activity.showProgress(false);
                                        synchronized (waitForUserInputLock) {
                                            waitForUserInputLock.notify();
                                        }
                                    }
                                }).run();
                            }
                        }
                );
            }
        });

        synchronized (waitForUserInputLock) {
            waitForUserInputLock.wait();
        }
    }

    private void handleConferenceFinished(TaskMsg msg) {
        stop();
        taskManager.handleTaskMsg(this, msg);
    }

    private void sleepInternal() throws InterruptedException {
        Thread.sleep(globalConfig.getPostFrameInterval());
    }

    @Override
    public void onConnected() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.showProgress(false);
                Toast.makeText(activity, R.string.bluetooth_connection_recover, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDisconnected() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.showProgress(false);
                Toast.makeText(activity, R.string.bluetooth_connect_failure, Toast.LENGTH_SHORT).show();
                handleBluetoothException();
            }
        });
    }

    @Override
    public void onDeviceNotFound() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.showProgress(false);
                Toast.makeText(activity, R.string.bluetooth_device_not_found, Toast.LENGTH_SHORT).show();
                handleBluetoothException();
            }
        });
    }

    @Override
    public void onLostConnection() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, R.string.bluetooth_connection_down, Toast.LENGTH_SHORT).show();
                handleBluetoothException();
            }
        });
    }

    @Override
    public void onSnapshotClicked() {
        taskManager.handleTaskMsg(this, TaskMsg.WhiteboardHwSnapshot);
    }

    private void handleBluetoothException() {
        String title = activity.getResources().getString(R.string.prompt_title_info);
        String content = "与设备断开连接，请重试";
        String no = "结束";
        String yes = "重试";
        WbMessager.show(activity, title, content, no, yes,
                //no click, exit meeting
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        //save current content
                        taskManager.handleTaskMsg(HostWbTask.this, TaskMsg.WhiteboardHwSnapshot);
                        handleConferenceFinished(TaskMsg.ConferenceFinished);
                    }
                },
                //yes click, try connect
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        activity.showProgress(true);
                        device.init(activity, HostWbTask.this);
                        device.connect(deviceName);
                    }
                }
        );
    }

    @Override
    public void onTouchDown(Map<Integer, List<TouchPoint>> points) {
        pathCache.put(points, false);
    }

    @Override
    public void onTouchUp(Map<Integer, List<TouchPoint>> points) {
        pathCache.put(points, true);
    }

    @Override
    public void onTouchMove(Map<Integer, List<TouchPoint>> points) {
        render(points);
        //cache these new points
        pathCache.put(points, false);
    }

    private void renderBitmap(){
        //render on surface
        Canvas currentBuffer = holder.lockCanvas();
        if (currentBuffer != null) {
            currentBuffer.drawBitmap(bitmap, 0, 0, paintTool.getBitmapPaint());
            holder.unlockCanvasAndPost(currentBuffer);
        }
    }

    private void render(Map<Integer, List<TouchPoint>> points) {
        //render on in-memory canvas first
        for (Map.Entry<Integer, List<TouchPoint>> entry : points.entrySet()) {
            //get last point of path
            int id = entry.getKey();
            List<TouchPoint> newPoints = entry.getValue();
            TouchPoint lastPoint = pathCache.getLastPointOfPath(id);
            if (lastPoint != null) {
                int c = lastPoint.getSystemColor();
                if (c == Color.WHITE) {
                    paintTool.erase(canvas, newPoints);
                } else {
                    paintTool.pen(canvas, lastPoint, newPoints);
                }
            }
        }
        renderBitmap();
    }

    private void saveResourcesToLocal(ArrayList<String> images) {
        String rootPublic = LocalConferenceRecords.getRootPath(activity) + date + "/" + conferenceId + "_" + name + "/public";
        File rootDir = new File(rootPublic);
        if (rootDir.exists()) {
            String files[] = rootDir.list();

            for (String image : images) {
                boolean imageSaved = false;
                for (String file : files) {
                    if (image.contains(file)) {
                        imageSaved = true;
                    }
                }

                if (!imageSaved){
                    try {
                        URL url = new URL(image);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setDoInput(true);
                        conn.connect();
                        InputStream is = conn.getInputStream();
                        Bitmap resource = BitmapFactory.decodeStream(is);
                        File imageFile = new File(rootDir, image.split("/")[image.split("/").length - 1]);
                        imageFile.createNewFile();
                        FileOutputStream outputStream = new FileOutputStream(imageFile);
                        int quality = 100;
                        resource.compress(Bitmap.CompressFormat.PNG, quality, outputStream);
                        outputStream.flush();
                        outputStream.close();
                    } catch (MalformedURLException e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                    } catch (IOException e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                }
            }
        }
    }

    class PathCache
    {
        private HashMap<Integer, Path> cache = new HashMap<>();
        private Set<Integer> doneSet = new HashSet<>();
        private Object lock = new Object();

        public void put(Map<Integer, List<TouchPoint>> points, boolean done) {
            synchronized (lock) {
                for (Map.Entry<Integer, List<TouchPoint>> entry : points.entrySet()) {
                    int id = entry.getKey();
                    List<TouchPoint> list = entry.getValue();
                    Path path = cache.get(id);
                    if (path == null) {
                        path = new Path();
                        path.setGroupId(id);
                        cache.put(id, path);
                    }

                    path.getPoints().addAll(list);

                    if (done) {
                        doneSet.add(id);
                    }
                }
            }
        }

        public TouchPoint getLastPointOfPath(int pathId) {
            synchronized (lock) {
                TouchPoint ret = null;
                Path path = cache.get(pathId);
                if (path != null) {
                    ret = path.getPoints().getLast();
                }

                return ret;
            }
        }

        public HashMap<Integer, Path> fetchAndPurge() {
            synchronized (lock) {
                HashMap<Integer, Path> ret = cache;
                HashMap<Integer, Path> newCahce = new HashMap<>();
                //save last point of each path
                for (Map.Entry<Integer, Path> entry : cache.entrySet()) {
                    int id = entry.getKey();
                    //if done, no need save last point
                    if (!doneSet.contains(id)) {
                        Path path = entry.getValue();
                        Path newPath = new Path();
                        newPath.addPoint(path.getPoints().getLast());
                        newCahce.put(id, newPath);
                    } else {
                        doneSet.remove(id);
                    }
                }
                cache = newCahce;
                return ret;
            }
        }
    }
}
