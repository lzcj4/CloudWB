package com.ifingers.yunwb.tasks;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.net.Uri;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

import com.ifingers.yunwb.R;
import com.ifingers.yunwb.dao.ConferenceDao;
import com.ifingers.yunwb.dao.Frame;
import com.ifingers.yunwb.dao.FrameSet;
import com.ifingers.yunwb.dao.Path;
import com.ifingers.yunwb.dao.TouchPoint;
import com.ifingers.yunwb.dao.UserDao;
import com.ifingers.yunwb.utility.LocalConferenceRecords;
import com.ifingers.yunwb.utility.PaintTool;
import com.ifingers.yunwb.utility.ServerAPI;
import com.ifingers.yunwb.utility.ServerError;
import com.ifingers.yunwb.utility.WhiteboardTaskContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * pull bitmap data from server
 * Created by Macoo on 2/6/2016.
 */
public class ClientWbTask implements Runnable, ICommonTask<Bitmap> {

    private final static String TAG = "ClientWbTask";
    private boolean isAlive = true;
    private Bitmap bitmap = null;
    private Canvas canvas = null;
    private TaskManagers taskManager;
    private int lastSeqNum = -1;
    private String conferenceId;
    private String password;
    private ConferenceDao conferenceDao = new ConferenceDao();
    private String meetingUrl;
    private ServerAPI serverApi = ServerAPI.getInstance();
    private Context context;
    private String date;
    private String name;
    private SurfaceHolder holder;
    private WhiteboardTaskContext globalConfig = WhiteboardTaskContext.getInstance();
    private UserDao user = globalConfig.getUserInfo();
    private PaintTool paintTool = PaintTool.getInstance();

    public ClientWbTask(String conferenceId, String password, TaskManagers taskManager, SurfaceHolder holder, Context context, String date, String name) {
        this.holder = holder;

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

        this.conferenceId = conferenceId;
        this.taskManager = taskManager;
        this.password = password;
        this.context = context;
        this.date = date;
        this.name = name;
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
                try {
                    int count = 0;
                    final int max = 5;
                    do {
                        ServerAPI.BaseData data = serverApi.shoutIAmOffline(meetingUrl, conferenceId, user.get_id());
                        int code = data.getCode();
                        if (code == ServerError.OK || code == ServerError.MEETING_IS_NOT_ALIVE) {
                            break;
                        } else {
                            Thread.sleep(400);
                        }
                    } while (++count < max);
                    if (count == max) {
                        Log.e(TAG, "try shoutIAmOffline failed " + max + " times");
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
        renderSurface();
    }

    @Override
    public void run() {
        if (globalConfig.isRenderSmooth()) {
            smoothLoop();
        } else {
            BumpLoop();
        }
    }

    //these for smoothLoop
    List<TouchPoint> pointList = new ArrayList<>();
    int pointIndex = 0;
    long now = 0;
    long lastGotFrameTime = 0;
    long pollingInterval = globalConfig.getPollingFrameInterval();
    double dPollingInterval = (double) pollingInterval;

    private void smoothLoop() {
        int maxFailureCount = WhiteboardTaskContext.getInstance().getMaxContinuousFailureCount();
        int lostCount = maxFailureCount + 1;//thus first time we will join meeting
        int roundInterval = 20;//ms, 50hz
        long diff = 0;

        while (isAlive) {
            try {
                //join and sync whiteboard
                if (lostCount > maxFailureCount) {
                    ServerAPI.ConferenceData data = serverApi.join(conferenceId, password, user.get_id(), user.getName());
                    int code = data.getCode();
                    if (code == ServerError.MEETING_IS_NOT_ALIVE) {
                        handleConferenceFinished(TaskMsg.ConferenceFinished);
                        break;
                    } else if (code != ServerError.OK) {
                        Log.e(TAG, "join meeting failed with code: " + code);
                        handleConferenceFinished(TaskMsg.JoinConferenceFailed);
                        break;
                    } else {
                        this.meetingUrl = data.getMeetingUrl();
                        conferenceDao.fill(data.getConference());

                        saveResourcesToLocal(conferenceDao.getImages());
                        lostCount = 0;
                        Log.i(TAG, "joined conference");
                        //get meeting snapshot
                        ServerAPI.SnapshotData snapshot = serverApi.getSnapshot(meetingUrl, conferenceId);
                        int errCode = snapshot.getCode();
                        if (errCode == ServerError.OK) {
                            render(snapshot);
                        } else if (errCode == ServerError.NO_SNAPSHOT) {
                            //nothing to do
                        }
                        else {
                            Log.e(TAG, "get snapshot failed with code: " + snapshot.getCode());
                            handleConferenceFinished(TaskMsg.JoinConferenceFailed);
                            break;
                        }
                    }
                }

                now = new Date().getTime();
                diff = now - lastGotFrameTime;
                if (diff >= pollingInterval) {
                    renderStep(1.0);
                    pointList.clear();
                    //get frame and info from server
                    ServerAPI.FrameSetData data = serverApi.getFrameSet(meetingUrl, conferenceId, lastSeqNum);
                    int code = data.getCode();
                    if (code == ServerError.OK) {
                        FrameSet frameSet = data.getFrameset();
                        lastSeqNum = frameSet.getSeqNum();
                        for (Frame frame : frameSet.getFrames()) {
                            for (Path path : frame.getPaths()) {
                                pointList.addAll(path.getPoints());
                            }
                        }
                        pointIndex = 0;
                        lastGotFrameTime = now;
                    } else if (code == ServerError.MEETING_IS_NOT_ALIVE) {
                        handleConferenceFinished(TaskMsg.ConferenceFinished);
                        break;
                    } else if (code == ServerError.LOST_FRAME) {
                        handleConferenceFinished(TaskMsg.LostFrame);
                        break;
                    } else {
                        lostCount++;
                    }

                    ServerAPI.MeetingData meetingData = serverApi.shoutIAmOnline(meetingUrl, conferenceId, user.get_id());
                    code = meetingData.getCode();
                    if (code == ServerError.OK) {
                        conferenceDao.fill(meetingData.getMeeting().getConference());
                        globalConfig.setMeetingInfo(meetingData.getMeeting());
                        saveResourcesToLocal(conferenceDao.getImages());
                    } else if (code == ServerError.MEETING_IS_NOT_ALIVE) {
                        handleConferenceFinished(TaskMsg.ConferenceFinished);
                        break;
                    } else {
                        lostCount++;
                    }
                } else { //diff < pollingInteravl
                    //do render smoothly
                    double p = (now - lastGotFrameTime) / dPollingInterval; //[0, 1);
                    renderStep(p);

                    Thread.sleep(roundInterval);
                }
            } catch (Exception e) {
                lostCount++;
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }

    private void renderStep(double p) {
        int lastIndex = (int)(p * pointList.size());
        if (lastIndex <= pointIndex)
            return;

        int pid = -1;
        pointIndex = --pointIndex < 0 ? 0 : pointIndex;//concat the last point

        List<TouchPoint> line = new ArrayList<>();
        for (; pointIndex < lastIndex; pointIndex++) {
            TouchPoint point = pointList.get(pointIndex);
            //new line
            if (point.getPointId() != pid) {
                //draw the line
                if (line.size() > 0) {
                    TouchPoint firstPoint = line.get(0);
                    int c = firstPoint.getSystemColor();
                    if (c == Color.WHITE) {
                        paintTool.erase(canvas, line);
                    } else {
                        paintTool.pen(canvas, firstPoint, line);
                    }
                }

                //create new line
                line.clear();
                pid = point.getPointId();
            }

            line.add(point);
        }

        //draw the line
        if (line.size() > 0) {
            TouchPoint firstPoint = line.get(0);
            int c = firstPoint.getSystemColor();
            if (c == Color.WHITE) {
                paintTool.erase(canvas, line);
            } else {
                paintTool.pen(canvas, firstPoint, line);
            }
        }

        renderSurface();
    }

    private void BumpLoop() {
        int maxFailureCount = WhiteboardTaskContext.getInstance().getMaxContinuousFailureCount();
        int lostCount = maxFailureCount + 1;//thus first time we will join meeting
        while (isAlive) {
            try {
                //join and sync whiteboard
                if (lostCount > maxFailureCount) {
                    ServerAPI.ConferenceData data = serverApi.join(conferenceId, password, user.get_id(), user.getName());
                    int code = data.getCode();
                    if (code == ServerError.MEETING_IS_NOT_ALIVE) {
                        handleConferenceFinished(TaskMsg.ConferenceFinished);
                        break;
                    } else if (code != ServerError.OK) {
                        Log.e(TAG, "join meeting failed with code: " + code);
                        handleConferenceFinished(TaskMsg.JoinConferenceFailed);
                        break;
                    } else {
                        this.meetingUrl = data.getMeetingUrl();
                        conferenceDao.fill(data.getConference());

                        saveResourcesToLocal(conferenceDao.getImages());
                        lostCount = 0;
                        Log.i(TAG, "joined conference");
                        //get meeting snapshot
                        ServerAPI.SnapshotData snapshot = serverApi.getSnapshot(meetingUrl, conferenceId);
                        int errCode = snapshot.getCode();
                        if (errCode == ServerError.OK) {
                            render(snapshot);
                        } else if (errCode == ServerError.NO_SNAPSHOT) {
                            //nothing to do
                        } else {
                            Log.e(TAG, "get snapshot failed with code: " + snapshot.getCode());
                            handleConferenceFinished(TaskMsg.JoinConferenceFailed);
                            break;
                        }
                    }
                }

                ServerAPI.FrameSetData data = serverApi.getFrameSet(meetingUrl, conferenceId, lastSeqNum);
                int code = data.getCode();
                if (code == ServerError.OK) {
                    render(data.getFrameset());
                } else if (code == ServerError.MEETING_IS_NOT_ALIVE) {
                    handleConferenceFinished(TaskMsg.ConferenceFinished);
                    break;
                } else {
                    lostCount++;
                }

                ServerAPI.MeetingData meetingData = serverApi.shoutIAmOnline(meetingUrl, conferenceId, user.get_id());
                code = data.getCode();
                if (code == ServerError.OK) {
                    conferenceDao.fill(meetingData.getMeeting().getConference());
                    globalConfig.setMeetingInfo(meetingData.getMeeting());
                    saveResourcesToLocal(conferenceDao.getImages());
                } else if (code == ServerError.MEETING_IS_NOT_ALIVE) {
                    handleConferenceFinished(TaskMsg.ConferenceFinished);
                    break;
                } else if (code == ServerError.LOST_FRAME) {
                    handleConferenceFinished(TaskMsg.LostFrame);
                    break;
                } else {
                    lostCount++;
                }

                sleepInternal();
            } catch (Exception e) {
                lostCount++;
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }

    private void render(ServerAPI.SnapshotData snapshot) {
        //render on in-memory canvas
        this.bitmap = snapshot.getSnapshot();
        this.canvas = new Canvas(bitmap);
        this.lastSeqNum = snapshot.getSeqNum();

        //render on surface
        Canvas currentBuffer = holder.lockCanvas();
        if (currentBuffer != null) {
            currentBuffer.drawBitmap(bitmap, 0, 0, paintTool.getBitmapPaint());
            holder.unlockCanvasAndPost(currentBuffer);
        }
    }

    private void renderSurface(){
        //render on surface
        Canvas currentBuffer = holder.lockCanvas();
        if (currentBuffer != null) {
            currentBuffer.drawBitmap(bitmap, 0, 0, paintTool.getBitmapPaint());
            holder.unlockCanvasAndPost(currentBuffer);
        }
    }

    private void render(FrameSet frameSet) {
        if (frameSet.getFrameSize() > 0) {
            lastSeqNum = frameSet.getSeqNum();
            for (Frame frame : frameSet.getFrames()) {
                for (Path path : frame.getPaths()) {
                    android.graphics.Path drawPath = new android.graphics.Path();
                    List<TouchPoint> points = path.getPoints();
                    TouchPoint firstPoint = points.get(0);
                    int c = firstPoint.getSystemColor();
                    if (c == Color.WHITE) {
                        paintTool.erase(canvas, points);
                    } else {
                        paintTool.pen(canvas, firstPoint, points);
                    }
                }
            }

            renderSurface();
        }
    }

    private void handleConferenceFinished(TaskMsg msg) {
        stop();
        taskManager.handleTaskMsg(this, msg);
    }

    private void sleepInternal() throws InterruptedException {
        Thread.sleep(WhiteboardTaskContext.getInstance().getPollingFrameInterval());
    }

    private void saveResourcesToLocal(ArrayList<String> images) {
        String rootPublic = LocalConferenceRecords.getRootPath(context) + date + "/" + conferenceId + "_" + name + "/public";
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
                        int pos = image.lastIndexOf('/') + 1;
                        URL url = new URL(image.substring(0, pos) + URLEncoder.encode(image.substring(pos), "UTF-8"));
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

}