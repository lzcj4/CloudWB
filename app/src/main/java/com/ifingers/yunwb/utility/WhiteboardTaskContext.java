package com.ifingers.yunwb.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.util.Log;

import com.ifingers.yunwb.dao.MeetingDao;
import com.ifingers.yunwb.dao.UserDao;
import com.ifingers.yunwb.services.IWBDevice;
import com.ifingers.yunwb.services.WBDevice;

/**
 * store task context
 * Created by Macoo on 2/2/2016.
 */
public class WhiteboardTaskContext {
    private static String tag = WhiteboardTaskContext.class.getName();
    private int maxContinuousFailureCount = 10;
    private int pollingFrameInterval = 2000;
    private int postFrameInterval = 2000;
    private int postSnapshotInterval = 30000;
    private int postHeartbeatInterval = 20000;
    private int connectTimeout = 10000;
    private int readTimeout = 10000;
    private UserDao userInfo = null;
    private int whiteBoardWidth;
    private int whiteBoardHeight;
    private float scaleFactorX;
    private float scaleFactorY;
    private boolean isHost;
    private String wxAppKey = "wx1681c34b79f4013a";
    private String wxAppSecret = "7aee473ffd21d083f0d894c7b8028ae4";
    private String wxState = "thisisastate";
    private String wxRefreshToken = null;
    private String wxAccessToken = null;
    private String techBridgeAppKey = "test";
    private String techBridgeSite = "121.40.94.192";
    private IWBDevice wbDevice = WBDevice.getInstance();
    //private IWBDevice wbDevice = WBMockDevice.getInstance();
    // TODO: 2016/5/11   how to define the pen and rubber size??
    private int rubberMaxSize = 2812 * 4993;  //80mm / 932mm * 32767,  80mm / 525mm * 32767
    private int rubberMinSize = 1406 * 2496;  //40mm / 932mm * 32767,  40mm / 525mm * 32767
    private int penMaxSize = 360 * 630;      //10mm / 932mm * 32767,  10mm / 525mm * 32767
    private int penMinSize = 144 * 252;      //4mm / 932mm * 32767,  4mm / 525mm * 32767
    private float penWidth = 4;               //pixel
    private MeetingDao meetingInfo;
    private boolean renderSmooth;
    private boolean whConverse = true;      //this is a hardware bug. Width and height data are conversed from hardware
    private float hardwareWHRatio = 525.0f / 932.0f;

    private Context context;

    private final String USER_KEY = "user";
    private final String USER_ID_KEY = "id";
    private final String USER_PWD_KEY = "password";
    private final String USER_CELLPHONE_KEY = "cellphone";
    private final String USER_NAME_KEY = "name";
    private final String USER_COMPANY_KEY = "company";
    private final String USER_JOB_KEY = "job";
    private final String USER_WEIXINID_KEY = "weixinid";
    private final String USER_AVATAR_KEY = "avatar";
    private final String USER_GENDER_KEY = "gender";
    private final String USER_PROVINCE_KEY = "province";
    private final String USER_CITY_KEY = "city";
    private final String USER_COUNTRY_KEY = "country";

    public boolean DEBUG = false;

    private WhiteboardTaskContext() {
    }

    public void init(Context context) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);

        this.context = context;

        try {
            ServerAPI.AppConfig config = ServerAPI.getInstance().getAppConfig();
            instance.setMaxContinuousFailureCount(config.getMaxContinuousFailureCount());
            instance.setPollingFrameInterval(config.getPollingFrameInterval());
            instance.setPostFrameInterval(config.getPostFrameInterval());
            instance.setPostSnapshotInterval(config.getPostSnapshotInterval());
            instance.setReadTimeout(config.getClientReadTimeout());
            instance.setConnectTimeout(config.getClientConnectTimeout());
            instance.setWxAppKey(config.getWxAppKey());
            instance.setWxAppSecret(config.getWxAppSecret());
            instance.setWxState(config.getWxAppState());
            instance.setPostHeartbeatInterval(config.getPostHeartbeatInterval());
            instance.setRubberMaxSize(config.getRuberMaxSize());
            instance.setRubberMinSize(config.getRuberMinSize());
            instance.setPenMaxSize(config.getPenMaxSize());
            instance.setPenMinSize(config.getPenMinSize());
            instance.setPenWidth(config.getPenWidth());
            instance.setWhConverse(config.isWhConverse());
            instance.setRenderSmooth(config.isRenderSmooth());
        } catch (Exception e) {
            Log.e(tag, e.toString());
            Log.e(tag, "failed to get app config from server, use default setting");
        }
    }

    public void clearUserInfoAtLocal() {
        SharedPreferences sp = context.getSharedPreferences(USER_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.commit();
    }

    public void saveUserInfoToLocal(UserDao user) {
        SharedPreferences sp = context.getSharedPreferences(USER_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(USER_ID_KEY, user.get_id());
        editor.putString(USER_PWD_KEY, user.getPassword());
        editor.putString(USER_CELLPHONE_KEY, user.getCellphone());
        editor.putString(USER_NAME_KEY, user.getName());
        editor.putString(USER_COMPANY_KEY, user.getCompany());
        editor.putString(USER_JOB_KEY, user.getJobTitle());
        editor.putString(USER_WEIXINID_KEY, user.getWeixinOpenId());
        editor.putString(USER_AVATAR_KEY, user.getAvatarUrl());
        editor.putString(USER_GENDER_KEY, user.getGender());
        editor.putString(USER_PROVINCE_KEY, user.getProvince());
        editor.putString(USER_CITY_KEY, user.getCity());
        editor.putString(USER_COUNTRY_KEY, user.getCountry());

        editor.commit();
    }

    public UserDao loadUserInfoFromLocal() {
        UserDao ret = null;
        SharedPreferences sp = context.getSharedPreferences(USER_KEY, Context.MODE_PRIVATE);
        String id = sp.getString(USER_ID_KEY, null);
        if (id != null) {
            ret = new UserDao();
            ret.set_id(id);

            String val = sp.getString(USER_PWD_KEY, "");
            ret.setPassword(val);

            val = sp.getString(USER_CELLPHONE_KEY, "");
            ret.setCellphone(val);

            val = sp.getString(USER_NAME_KEY, "");
            ret.setName(val);

            val = sp.getString(USER_COMPANY_KEY, "");
            ret.setCompany(val);

            val = sp.getString(USER_JOB_KEY, "");
            ret.setJobTitle(val);

            val = sp.getString(USER_WEIXINID_KEY, "");
            ret.setWeixinOpenId(val);

            val = sp.getString(USER_AVATAR_KEY, "");
            ret.setAvatarUrl(val);

            val = sp.getString(USER_GENDER_KEY, "");
            ret.setGender(val);

            val = sp.getString(USER_PROVINCE_KEY, "");
            ret.setProvince(val);

            val = sp.getString(USER_CITY_KEY, "");
            ret.setCity(val);

            val = sp.getString(USER_COUNTRY_KEY, "");
            ret.setCountry(val);
        }

        return ret;
    }

    public float getHardwareWHRatio() {
        return hardwareWHRatio;
    }

    public String getTechBridgeAppKey() {
        return techBridgeAppKey;
    }

    public String getTechBridgeSite() {
        return techBridgeSite;
    }

    public boolean isWhConverse() {
        return whConverse;
    }

    public void setWhConverse(boolean whConverse) {
        this.whConverse = whConverse;
    }

    public int getRubberMaxSize() {
        return rubberMaxSize;
    }

    public void setRubberMaxSize(int rubberMaxSize) {
        this.rubberMaxSize = rubberMaxSize;
    }

    public int getRubberMinSize() {
        return rubberMinSize;
    }

    public void setRubberMinSize(int rubberMinSize) {
        this.rubberMinSize = rubberMinSize;
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

    public IWBDevice getWbDevice() {
        return wbDevice;
    }

    public String getWxAppKey() {
        return wxAppKey;
    }

    public String getWxAppSecret() {
        return wxAppSecret;
    }

    public String getWxState() {
        return wxState;
    }

    public void setWxState(String wxState) {
        this.wxState = wxState;
    }

    public void setWxAppSecret(String wxAppSecret) {
        this.wxAppSecret = wxAppSecret;
    }

    public void setWxAppKey(String wxAppKey) {
        this.wxAppKey = wxAppKey;
    }

    public String getWxAccessToken() {
        return wxAccessToken;
    }

    public void setWxAccessToken(String wxAccessToken) {
        this.wxAccessToken = wxAccessToken;
    }

    public String getWxRefreshToken() {
        return wxRefreshToken;
    }

    public void setWxRefreshToken(String wxRefreshToken) {
        this.wxRefreshToken = wxRefreshToken;
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

    public int getPostSnapshotInterval() {
        return postSnapshotInterval;
    }

    public void setPostSnapshotInterval(int postSnapshotInterval) {
        this.postSnapshotInterval = postSnapshotInterval;
    }

    public int getPostHeartbeatInterval() {
        return postHeartbeatInterval;
    }

    public void setPostHeartbeatInterval(int postHeartbeatInterval) {
        this.postHeartbeatInterval = postHeartbeatInterval;
    }

    public float getScaleFactorX() {
        return scaleFactorX;
    }

    public float getScaleFactorY() {
        return scaleFactorY;
    }

    public int getWhiteBoardHeight() {
        return whiteBoardHeight;
    }

    public void setWhiteBoardHeight(int whiteBoardHeight) {
        this.whiteBoardHeight = whiteBoardHeight;
        this.scaleFactorY = whiteBoardHeight / 32767.0f;
    }

    public int getWhiteBoardWidth() {
        return whiteBoardWidth;
    }

    public void setWhiteBoardWidth(int whiteBoardWidth) {
        this.whiteBoardWidth = whiteBoardWidth;
        this.scaleFactorX = whiteBoardWidth / 32767.0f;
    }

    public String getUserId() {
        return userInfo.get_id();
    }

    private static WhiteboardTaskContext instance;

    public UserDao getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserDao userInfo) {
        this.userInfo = userInfo;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getMaxContinuousFailureCount() {
        return maxContinuousFailureCount;
    }

    public void setMaxContinuousFailureCount(int maxContinuousFailureCount) {
        this.maxContinuousFailureCount = maxContinuousFailureCount;
    }

    public MeetingDao getMeetingInfo() {
        return meetingInfo;
    }

    public void setMeetingInfo(MeetingDao meetingInfo) {
        this.meetingInfo = meetingInfo;
    }

    public boolean isRenderSmooth() {
        return renderSmooth;
    }

    public void setRenderSmooth(boolean renderSmooth) {
        this.renderSmooth = renderSmooth;
    }

    public static WhiteboardTaskContext getInstance() {
        if (instance == null) {
            synchronized (WhiteboardTaskContext.class) {
                if (instance == null) {
                    instance = new WhiteboardTaskContext();
                }
            }
        }

        return instance;
    }

    public boolean isHost() {
        return isHost;
    }

    public void setIsHost(boolean isHost) {
        this.isHost = isHost;
    }
}
