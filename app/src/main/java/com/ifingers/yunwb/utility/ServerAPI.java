package com.ifingers.yunwb.utility;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ifingers.yunwb.dao.ConferenceDao;
import com.ifingers.yunwb.dao.Frame;
import com.ifingers.yunwb.dao.FrameSet;
import com.ifingers.yunwb.dao.MeetingDao;
import com.ifingers.yunwb.dao.Path;
import com.ifingers.yunwb.dao.TouchPoint;
import com.ifingers.yunwb.dao.UserDao;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by SFY on 2/19/2016.
 */
public class ServerAPI {
    private static ServerAPI ourInstance = new ServerAPI();

    public static ServerAPI getInstance() {
        return ourInstance;
    }

    private String masterUrl = "http://tn.glasslink.cn:3000/";
    private String tag = "ServerAPI";
    private Gson gson = new GsonBuilder().create();
    private String inviteUrl = masterUrl + "entry.html";
    private Logger logger = LoggerFactory.getLogger(ServerAPI.class);

    private ServerAPI() {
    }

    public String getMasterUrl() {
        return masterUrl;
    }

    public String getInviteUrl(String cid, String pwd, String name) {
        return String.format("%s?id=%s&pass=%s&name=%s", inviteUrl, cid, pwd, name);
    }

    public Bitmap getImageBitmap(String urlStr) {
        Bitmap bm = null;
        try {
            int pos = urlStr.lastIndexOf('/') + 1;
            URL url = new URL(urlStr.substring(0, pos) + URLEncoder.encode(urlStr.substring(pos), "UTF-8"));
            URLConnection conn = url.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            bm = BitmapFactory.decodeStream(bis);
            bis.close();
            is.close();
        } catch (IOException e) {
            logger.error("getImageBitmap", e);
        }
        return bm;
    }

    public UserData update(UserDao user) {
        UserData data = null;
        try {
            HttpURLConnection conn = NetworkWorker.generateConnection(masterUrl, RequestActions.UpdateUser, "POST");
            NetworkWorker.setJsonConnection(conn);

            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(gson.toJson(user));
            writer.flush();

            data = handleJSONResponse(conn, UserData.class);
        } catch (Exception e) {
            logger.error("update", e);
        }

        if (data == null)
            data = new UserData();
        return data;
    }

    public UserData login(String cellphone, String password) {
        UserData data = null;

        try {
            HttpURLConnection conn = NetworkWorker.generateConnection(masterUrl, RequestActions.Login, "POST");
            // to post json data
            NetworkWorker.setJsonConnection(conn);
            JSONObject cred = new JSONObject();
            cred.put("cellphone", cellphone);
            cred.put("password", password);

            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(cred.toString());
            wr.flush();

            data = handleJSONResponse(conn, UserData.class);
        } catch (Exception e) {
            logger.error("login", e);
        }

        if (data == null) {
            data = new UserData();
        }
        return data;
    }

    public UserData wxLogin(String openid, String name, String avatarUrl, String gender, String province, String city, String country) {
        UserData data = null;

        try {
            HttpURLConnection conn = NetworkWorker.generateConnection(masterUrl, RequestActions.WXLogin, "POST");
            NetworkWorker.setJsonConnection(conn);
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("weixinOpenId", openid);
            jsonObj.put("name", name);
            jsonObj.put("avatarUrl", avatarUrl);
            jsonObj.put("gender", gender);
            jsonObj.put("province", province);
            jsonObj.put("city", city);
            jsonObj.put("country", country);

            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(jsonObj.toString());
            writer.flush();

            data = handleJSONResponse(conn, UserData.class);
        } catch (Exception e) {
            logger.error("wxLogin", e);
        }

        if (data == null) {
            data = new UserData();
        }

        return data;
    }

    public UserListData getUserList(List<String> userIdList) {
        UserListData data = null;
        try {
            HttpURLConnection connection = NetworkWorker.generateConnection(masterUrl, RequestActions.GetUserList, "POST");
            NetworkWorker.setJsonConnection(connection);

            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write(gson.toJson(userIdList));
            wr.flush();

            data = handleJSONResponse(connection, UserListData.class);
        } catch (Exception e) {
            logger.error("getUserList", e);
        }

        if (data == null) {
            data = new UserListData();
        }

        return data;
    }

    public AvatarData uploadAvatar(String userId, byte[] image) {
        AvatarData data = null;
        try {
            String action = String.format(RequestActions.UploadAvatar, userId);
            HttpURLConnection conn = NetworkWorker.generateConnection(masterUrl, action, "POST");
            NetworkWorker.setOctetConnection(conn);

            OutputStream ostream = conn.getOutputStream();
            ostream.write(image, 0, image.length);
            ostream.flush();

            data = handleJSONResponse(conn, AvatarData.class);
        } catch (Exception e) {
            logger.error("uploadAvatar", e);
        }

        if (data == null)
            data = new AvatarData();

        return data;
    }

    public ConferenceData launchConference(ConferenceDao conferenceDao) {
        ConferenceData data = null;

        try {
            HttpURLConnection connection = NetworkWorker.generateConnection(masterUrl, RequestActions.LaunchConference, "POST");
            NetworkWorker.setJsonConnection(connection);

            Map<String, Object> map = conferenceDao.toMap();

            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write(gson.toJson(map));
            wr.flush();

            data = handleJSONResponse(connection, ConferenceData.class);
        } catch (Exception e) {
            logger.error("launchConference", e);
        }

        if (data == null) {
            data = new ConferenceData();
        }

        return data;
    }

    public BaseData endConference(ConferenceDao conferenceDao) {
        BaseData data = null;

        try {
            HttpURLConnection connection = NetworkWorker.generateConnection(masterUrl, RequestActions.EndConference, "POST");
            NetworkWorker.setJsonConnection(connection);

            Map<String, Object> map = conferenceDao.toMap();

            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write(gson.toJson(map));
            wr.flush();

            data = handleJSONResponse(connection, BaseData.class);
        } catch (Exception e) {
            logger.error("endConference", e);
        }

        if (data == null) {
            data = new BaseData();
        }

        return data;
    }

    public ConferenceData updateConference(ConferenceDao conferenceDao) {
        ConferenceData data = null;
        try {
            HttpURLConnection conn = NetworkWorker.generateConnection(masterUrl, RequestActions.UpdateConference, "POST");
            NetworkWorker.setJsonConnection(conn);

            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(gson.toJson(conferenceDao.toMap()));
            writer.flush();

            data = handleJSONResponse(conn, ConferenceData.class);
        } catch (Exception e) {
            logger.error("updateConference", e);
        }

        if (data == null)
            data = new ConferenceData();
        return data;
    }

    public ConferenceData join(String conferenceId, String password, String userId, String nickname) {
        ConferenceData data = null;

        try {
            HttpURLConnection connection = NetworkWorker.generateConnection(masterUrl, RequestActions.JoinConference, "POST");
            NetworkWorker.setJsonConnection(connection);
            JSONObject confInfo = new JSONObject();
            confInfo.put("_id", conferenceId);
            confInfo.put("password", password);

            JSONArray users = new JSONArray();
            users.put(userId);
            confInfo.put("users", users);

            JSONArray nicknames = new JSONArray();
            nicknames.put(nickname);
            confInfo.put("nicknames", nicknames);

            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write(confInfo.toString());
            wr.flush();

            data = handleJSONResponse(connection, ConferenceData.class);
        } catch (Exception e) {
            logger.error("join", e);
        }

        if (data == null) {
            data = new ConferenceData();
        }

        return data;
    }

    public ConferenceListData requestConferenceList() {
        ConferenceListData data = null;

        try {
            HttpURLConnection connection = NetworkWorker.generateConnection(masterUrl, RequestActions.AliveConference, "GET");
            connection.connect();

            data = handleJSONResponse(connection, ConferenceListData.class);
        } catch (Exception e) {
            logger.error("requestConferenceList", e);
        }

        if (data == null) {
            data = new ConferenceListData();
        }

        return data;
    }

    public ConferenceData requestConferenceInfo(String id, String password) {
        ConferenceData data = null;

        try {
            String action = String.format(RequestActions.ConferenceInfo, id, password);
            HttpURLConnection connection = NetworkWorker.generateConnection(masterUrl, action, "GET");
            connection.connect();

            data = handleJSONResponse(connection, ConferenceData.class);
        } catch (Exception e) {
            logger.error("requestConferenceInfo", e);
        }

        if (data == null) {
            data = new ConferenceData();
        }

        return data;
    }

    public BaseData postHeartbeat(String meetingUrl, String conferenceId) {
        BaseData data = new BaseData();
        try {
            String action = String.format(RequestActions.PostHeartbeat, conferenceId);
            HttpURLConnection conn = NetworkWorker.generateConnection(meetingUrl, action, "POST");
            NetworkWorker.setOctetConnection(conn);
            //nothing to write
            conn.getOutputStream().flush();

            int respCode = conn.getResponseCode();
            if (respCode == 200) {
                data.setCode(ServerError.OK);
            } else if (respCode == 501) {
                InputStreamReader reader = new InputStreamReader(conn.getErrorStream());
                data = gson.fromJson(reader, BaseData.class);
            } else {
                logger.error("postHeartbeat with http code " + respCode);
            }
        } catch (Exception e) {
            logger.error("postHeartBeat", e);
        }

        return data;
    }

    public BaseData postFrame(String meetingUrl, String conferenceId, Frame frame) {
        BaseData data = new BaseData();
        try {
            String action = String.format(RequestActions.PostFrame, conferenceId);
            HttpURLConnection conn = NetworkWorker.generateConnection(meetingUrl, action, "POST");
            NetworkWorker.setOctetConnection(conn);
            //nothing to write
            DataOutputStream writer = new DataOutputStream(conn.getOutputStream());
            writer.writeShort(frame.getNumber());
            int total = frame.getPaths().size();
            for (int i = 0; i < total; i++) {
                Path path = frame.getPaths().get(i);
                writer.writeByte(path.getGroupId());
                int pointsSize = path.getPoints().size();
                writer.writeShort(pointsSize);
                for (int indexOfPoint = 0; indexOfPoint < pointsSize; indexOfPoint++) {
                    TouchPoint point = path.getPoints().get(indexOfPoint);
                    writer.writeShort(point.getPointX());
                    writer.writeShort(point.getPointY());
                    writer.writeShort(point.getPointWidth());
                    writer.writeShort(point.getPointHeight());
                    writer.writeByte(point.getPointColor());
                }
            }
            writer.flush();

            int respCode = conn.getResponseCode();
            if (respCode == 200) {
                data.setCode(ServerError.OK);
            } else if (respCode == 501) {
                InputStreamReader reader = new InputStreamReader(conn.getErrorStream());
                data = gson.fromJson(reader, BaseData.class);
            } else {
                logger.error("post frame with http code" + respCode);
            }
        } catch (Exception e) {
            logger.error("postFrame", e);
        }

        return data;
    }

    public ConferenceData uploadImage(String conferenceId, String fileName, byte[] image) {
        ConferenceData data = new ConferenceData();
        try {
            String action = String.format(RequestActions.UploadPNGImage, conferenceId, fileName);
            HttpURLConnection conn = NetworkWorker.generateConnection(masterUrl, action, "POST");
            NetworkWorker.setOctetConnection(conn);

            OutputStream ostream = conn.getOutputStream();
            ostream.write(image, 0, image.length);
            ostream.flush();

            int respCode = conn.getResponseCode();
            if (respCode == 200) {
                data.setCode(ServerError.OK);
            } else if (respCode == 501) {
                InputStreamReader reader = new InputStreamReader(conn.getErrorStream());
                data = gson.fromJson(reader, ConferenceData.class);
            } else {
                logger.error("upload image with http code " + respCode);
            }
        } catch (Exception e) {
            logger.error("uploadImage", e);
        }

        return data;
    }

    public BaseData postSnapshot(String meetingUrl, String conferenceId, byte[] snapshot) {
        BaseData data = new BaseData();
        try {
            String action = String.format(RequestActions.PostSnapshot, conferenceId);
            HttpURLConnection conn = NetworkWorker.generateConnection(meetingUrl, action, "POST");
            NetworkWorker.setOctetConnection(conn);

            OutputStream ostream = conn.getOutputStream();
            ostream.write(snapshot, 0, snapshot.length);
            ostream.flush();

            int respCode = conn.getResponseCode();
            if (respCode == 200) {
                data.setCode(ServerError.OK);
            } else if (respCode == 501) {
                InputStreamReader reader = new InputStreamReader(conn.getErrorStream());
                data = gson.fromJson(reader, BaseData.class);
            } else {
                logger.error("post snapshot with http code " + respCode);
            }
        } catch (Exception e) {
            logger.error("postSnapshot", e);
        }

        return data;
    }

    public FrameSetData getFrameSet(String meetingUrl, String conferenceId, int seqNum) {
        FrameSetData data = new FrameSetData(ServerError.UNKNOWN_ERROR, null);

        try {
            String action = String.format(RequestActions.GetWhiteBoardFrameList, conferenceId, seqNum);
            HttpURLConnection connection = NetworkWorker.generateConnection(meetingUrl, action, "GET");
            connection.connect();

            int response = connection.getResponseCode();
            // TODO: 2016/5/11  why reponse with 200 or 500 ?
            if (response == 200) {
                DataInputStream reader = new DataInputStream(connection.getInputStream());
                FrameSet frameSet = parseFrameset(reader);
                data.setCode(ServerError.OK);
                data.setFrameset(frameSet);
            } else if (response == 501) {
                InputStreamReader reader = new InputStreamReader(connection.getErrorStream());
                data = gson.fromJson(reader, FrameSetData.class);
                logger.error("error http response code " + response + " with server code " + data.getCode() + data.getMsg());
            } else {
                logger.error("get frame set with http code " + response);
            }
        } catch (Exception e) {
            logger.error("getFrameSet", e);
            data.setCode(ServerError.UNKNOWN_ERROR);
        }

        return  data;
    }

    public SnapshotData getSnapshot(String meetingUrl, String conferenceId) {
        SnapshotData data = new SnapshotData(ServerError.UNKNOWN_ERROR, null, -1);
        int code = ServerError.UNKNOWN_ERROR;
        // pull full jpg snapshot from server
        try {
            String action = String.format(RequestActions.SyncWhiteBoard, conferenceId);
            HttpURLConnection connection = NetworkWorker.generateConnection(meetingUrl, action, "GET");
            connection.connect();
            int response = connection.getResponseCode();

            if (response == 200) {
                InputStream is = connection.getInputStream();
                DataInputStream reader = new DataInputStream(is);
                try {
                    int seqNum = reader.readInt();
                    Log.e(tag, "snapshot seq num" + seqNum);

                    Bitmap hostBitmap = BitmapFactory.decodeStream(reader);
                    if (hostBitmap != null) {
                        Bitmap bitmap = Bitmap.createScaledBitmap(hostBitmap,
                                WhiteboardTaskContext.getInstance().getWhiteBoardWidth(),
                                WhiteboardTaskContext.getInstance().getWhiteBoardHeight(), false);
                        data.setSnapshot(bitmap);
                        data.setCode(ServerError.OK);
                        data.setSeqNum(seqNum);
                    } else
                        Log.e(tag, "failed to decode snapshot stream");
                } catch (EOFException ex) {
                    //means no snapshot in response, host has not post snapshot yet
                    data.setCode(ServerError.NO_SNAPSHOT);
                }


            } else if (response == 501) {
                InputStreamReader reader = new InputStreamReader(connection.getErrorStream());
                data = gson.fromJson(reader, SnapshotData.class);
                logger.error("error http response code " + response + " with server code " + data.getCode() + data.getMsg());
            } else {
                logger.error("get snapshot with http code " + response);
            }
        } catch (Exception e) {
            logger.error("getSnapshot", e);
        }

        return data;
    }

    public MeetingData shoutIAmOnline(String meetingUrl, String meetingId, String userId) {
        MeetingData data = null;

        try {
            String action = String.format(RequestActions.ShoutIAmOnline, meetingId, userId);
            HttpURLConnection connection = NetworkWorker.generateConnection(meetingUrl, action, "GET");
            connection.connect();

            data = handleJSONResponse(connection, MeetingData.class);
        } catch (Exception e) {
            logger.error("shoutIAmOnline", e);
        }

        if (data == null) {
            data = new MeetingData();
        }

        return data;
    }

    public BaseData shoutIAmOffline(String meetingUrl, String meetingId, String userId) {
        BaseData data = null;

        try {
            String action = String.format(RequestActions.ShoutIAmOffline, meetingId, userId);
            HttpURLConnection connection = NetworkWorker.generateConnection(meetingUrl, action, "GET");
            connection.connect();

            data = handleJSONResponse(connection, BaseData.class);
        } catch (Exception e) {
            logger.error("shoutIAmOffline", e);
        }

        if (data == null) {
            data = new BaseData();
        }

        return data;
    }

    public AppConfig getAppConfig() throws Exception {
        //fetch from server
        HttpURLConnection connection = NetworkWorker.generateConnection(masterUrl, RequestActions.AppConfig, "GET");

        InputStreamReader reader = new InputStreamReader(connection.getInputStream(), "UTF-8");
        AppConfig config = gson.fromJson(reader, AppConfig.class);
        return config;
    }

    private <T extends BaseData> T handleJSONResponse(HttpURLConnection conn, Class<T> cls) throws Exception {
        T data = null;
        int respCode = conn.getResponseCode();
        if (respCode == 200) {
            InputStreamReader reader = new InputStreamReader(conn.getInputStream(), "UTF-8");
            data = gson.fromJson(reader, cls);
        } else if (respCode == 501) {
            InputStreamReader reader = new InputStreamReader(conn.getErrorStream());
            data = gson.fromJson(reader, cls);
            logger.error("error from server:" + data.getMsg());
        } else {
            logger.error("handleJSONResponse with http code " + respCode);
        }

        return data;
    }

    private FrameSet parseFrameset(DataInputStream reader) throws IOException {
        FrameSet frameSet = new FrameSet();

        frameSet.setSeqNum(reader.readInt()); //seq number
        frameSet.setFrameSize(reader.readShort()); // frame size
        int index = 0;
        while (index < frameSet.getFrameSize()) {
            Frame frame = new Frame();
            int pathsNum = reader.readShort(); // paths number
            int pathIndex = 0;
            while (pathIndex < pathsNum) {
                Path path = new Path();
                path.setGroupId(reader.readByte());
                int pointsNum = reader.readShort();
                int pIndex = 0;
                while (pIndex < pointsNum) {
                    TouchPoint p = new TouchPoint();
                    p.setPointX(reader.readShort());
                    p.setPointY(reader.readShort());
                    p.setPointWidth(reader.readShort());
                    p.setPointHeight(reader.readShort());
                    p.setPointColor(reader.readByte());
                    p.setPointId(path.getGroupId());
                    pIndex++;
                    path.addPoint(p);
                }

                frame.addPath(path);
                pathIndex++;
            }
            frameSet.addFrame(frame);
            index++;
        }
        return frameSet;
    }

    public class BaseData {
        protected int code;
        protected String msg;

        public BaseData() {
            this.code = ServerError.UNKNOWN_ERROR;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }
    }

    public class FrameSetData extends BaseData {
        private FrameSet frameset;

        public FrameSetData(int code, FrameSet frameset) {
            this.code = code;
            this.frameset = frameset;
        }

        public FrameSet getFrameset() {
            return frameset;
        }

        public void setFrameset(FrameSet frameset) {
            this.frameset = frameset;
        }
    }

    public class SnapshotData extends BaseData {
        private Bitmap snapshot;
        private int seqNum;

        public SnapshotData(int code, Bitmap snapshot, int seqNum) {
            this.code = code;
            this.snapshot = snapshot;
            this.seqNum = seqNum;
        }

        public Bitmap getSnapshot() {
            return snapshot;
        }

        public void setSnapshot(Bitmap snapshot) {
            this.snapshot = snapshot;
        }

        public int getSeqNum() {
            return seqNum;
        }

        public void setSeqNum(int seqNum) {
            this.seqNum = seqNum;
        }
    }

    public class AppConfig
    {
        private int pollingFrameInterval = 2000;
        private int postFrameInterval = 2000;
        private int maxContinuousFailureCount = 10;
        private int postSnapshotInterval = 30000;
        private int postHeartbeatInterval = 20000;
        private int clientConnectTimeout = 10000;
        private int clientReadTimeout = 10000;
        private String wxAppKey = "wx1681c34b79f4013a";
        private String wxAppSecret = "7aee473ffd21d083f0d894c7b8028ae4";
        private String wxAppState = "thisisastate";
        ///TODO: how to define the pen and rubber size??
        private int ruberMaxSize = 2812 * 4993;  //80mm / 932mm * 32767,  80mm / 525mm * 32767
        private int ruberMinSize = 1406 * 2496;  //40mm / 932mm * 32767,  40mm / 525mm * 32767
        private int penMaxSize = 360 * 630;      //10mm / 932mm * 32767,  10mm / 525mm * 32767
        private int penMinSize = 144 * 252;      //4mm / 932mm * 32767,  4mm / 525mm * 32767
        private float penWidth = 4;                //pixel
        private boolean whConverse = true;      //this is a hardware bug. Width and height data are conversed from hardware
        private boolean renderSmooth = false;

        public boolean isWhConverse() {
            return whConverse;
        }

        public void setWhConverse(boolean whConverse) {
            this.whConverse = whConverse;
        }

        public int getRuberMaxSize() {
            return ruberMaxSize;
        }

        public void setRuberMaxSize(int ruberMaxSize) {
            this.ruberMaxSize = ruberMaxSize;
        }

        public int getRuberMinSize() {
            return ruberMinSize;
        }

        public void setRuberMinSize(int ruberMinSize) {
            this.ruberMinSize = ruberMinSize;
        }

        public int getPenMaxSize() {
            return penMaxSize;
        }

        public void setPenMaxSize(int penMaxSize) {
            this.penMaxSize = penMaxSize;
        }

        public int getPenMinSize() {
            return penMinSize;
        }

        public void setPenMinSize(int penMinSize) {
            this.penMinSize = penMinSize;
        }

        public float getPenWidth() {
            return penWidth;
        }

        public void setPenWidth(float penWidth) {
            this.penWidth = penWidth;
        }

        public int getPostHeartbeatInterval() {
            return postHeartbeatInterval;
        }

        public void setPostHeartbeatInterval(int postHeartbeatInterval) {
            this.postHeartbeatInterval = postHeartbeatInterval;
        }

        public int getClientConnectTimeout() {
            return clientConnectTimeout;
        }

        public void setClientConnectTimeout(int clientConnectTimeout) {
            this.clientConnectTimeout = clientConnectTimeout;
        }

        public int getClientReadTimeout() {
            return clientReadTimeout;
        }

        public void setClientReadTimeout(int clientReadTimeout) {
            this.clientReadTimeout = clientReadTimeout;
        }

        public String getWxAppKey() {
            return wxAppKey;
        }

        public void setWxAppKey(String wxAppKey) {
            this.wxAppKey = wxAppKey;
        }

        public String getWxAppSecret() {
            return wxAppSecret;
        }

        public void setWxAppSecret(String wxAppSecret) {
            this.wxAppSecret = wxAppSecret;
        }

        public String getWxAppState() {
            return wxAppState;
        }

        public void setWxAppState(String wxAppState) {
            this.wxAppState = wxAppState;
        }

        public int getPollingFrameInterval() {
            return pollingFrameInterval;
        }

        public void setPollingFrameInterval(int pollingFrameInterval) {
            this.pollingFrameInterval = pollingFrameInterval;
        }

        public int getPostFrameInterval() {
            return postFrameInterval;
        }

        public void setPostFrameInterval(int postFrameInterval) {
            this.postFrameInterval = postFrameInterval;
        }

        public int getMaxContinuousFailureCount() {
            return maxContinuousFailureCount;
        }

        public void setMaxContinuousFailureCount(int maxContinuousFailureCount) {
            this.maxContinuousFailureCount = maxContinuousFailureCount;
        }

        public int getPostSnapshotInterval() {
            return postSnapshotInterval;
        }

        public void setPostSnapshotInterval(int postSnapshotInterval) {
            this.postSnapshotInterval = postSnapshotInterval;
        }

        public boolean isRenderSmooth() {
            return renderSmooth;
        }

        public void setRenderSmooth(boolean renderSmooth) {
            this.renderSmooth = renderSmooth;
        }
    }

    public class UserData extends BaseData
    {
        private UserDao user;

        public UserDao getUser() {
            return user;
        }

        public void setUser(UserDao user) {
            this.user = user;
        }
    }

    public class UserListData extends BaseData
    {
        private List<UserDao> users;

        public List<UserDao> getUsers() {
            return users;
        }

        public void setUsers(List<UserDao> users) {
            this.users = users;
        }
    }

    public class ConferenceData extends BaseData {
        private HashMap<String, Object> conference;
        private String meetingUrl;

        public HashMap<String, Object> getConference() {
            return conference;
        }

        public void setConference(HashMap<String, Object> conference) {
            this.conference = conference;
        }

        public String getMeetingUrl() {
            return meetingUrl;
        }

        public void setMeetingUrl(String meetingUrl) {
            this.meetingUrl = meetingUrl;
        }
    }

    public class ConferenceListData extends BaseData {
        private ArrayList<HashMap<String, Object>> conferences;

        public ArrayList<HashMap<String, Object>> getConferences() {
            return conferences;
        }

        public void setConferences(ArrayList<HashMap<String, Object>> conferences) {
            this.conferences = conferences;
        }
    }

    public class MeetingData extends BaseData {
        private MeetingDao meeting;

        public MeetingDao getMeeting() {
            return meeting;
        }
    }

    public class AvatarData extends BaseData {
        private String url;

        public String getUrl() { return url; }
    }
}
