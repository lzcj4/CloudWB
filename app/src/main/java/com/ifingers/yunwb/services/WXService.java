package com.ifingers.yunwb.services;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ifingers.yunwb.utility.ServerAPI;
import com.ifingers.yunwb.utility.ServerError;
import com.ifingers.yunwb.utility.WhiteboardTaskContext;
import com.tencent.mm.sdk.modelbase.BaseReq;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.modelmsg.SendAuth;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXImageObject;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXTextObject;
import com.tencent.mm.sdk.modelmsg.WXWebpageObject;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

/**
 * Created by SFY on 2/24/2016.
 */
public class WXService implements IWXAPIEventHandler {
    private static WXService ourInstance = new WXService();

    public static WXService getInstance() {
        return ourInstance;
    }

    private String tag = "WXService";
    private WhiteboardTaskContext globalConfig = WhiteboardTaskContext.getInstance();
    private Gson gson = new GsonBuilder().create();
    private IWXAPI wxapi;
    private String wxAppKey = globalConfig.getWxAppKey();
    private String wxAppSecret = globalConfig.getWxAppSecret();
    private String wxState = globalConfig.getWxState();
    private Timer refreshTimer = null;
    private final int ONE_HOUR = 60 * 60 * 1000;
    private Context context = null;

    private WXService() {
    }

    public void init(Context context) {
        if (wxapi == null) {
            this.context = context;
            wxapi = WXAPIFactory.createWXAPI(context, wxAppKey, true);
            wxapi.registerApp(wxAppKey);
            isWXInstalled(context);
            try {
                //enable https with unknown cert
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, new TrustManager[]{new TrustManager()}, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier());
            } catch (Exception e) {
                Log.e(tag, e.toString());
            }
        }
    }

    /**
     * 检查微信是否安装在手机上
     * @param context
     * @return
     */
    public boolean isWXInstalled(Context context) {
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> list = pm.getInstalledPackages(0);
        boolean result = false;
        for (PackageInfo item : list) {
            if (item.packageName.contains("com.tencent.mm")) {
                result = true;
                break;
            }
        }
        return result;
    }

    public void handleIntent(Intent it) {
        wxapi.handleIntent(it, this);
    }

    public void startLogin() {
        SendAuth.Req req = new SendAuth.Req();
        req.scope = "snsapi_userinfo";
        req.state = wxState;
        wxapi.sendReq(req);
    }

    public void shareWeb(String url, String title, String desc) {
        WXWebpageObject webpage = new WXWebpageObject();
        webpage.webpageUrl = url;
        WXMediaMessage msg = new WXMediaMessage(webpage);
        msg.title = title;
        msg.description = desc;

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("webpage");
        req.message = msg;
        req.scene = SendMessageToWX.Req.WXSceneSession;
        wxapi.sendReq(req);
    }

    public void shareText(String title, String content) {
        WXTextObject textObj = new WXTextObject();
        textObj.text = content;

        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = textObj;
        msg.description = title;

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("text");
        req.message = msg;
        req.scene = SendMessageToWX.Req.WXSceneSession;

        wxapi.sendReq(req);
    }

    public void shareImage(Bitmap image) {
        WXImageObject imgObj = new WXImageObject(image);
        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = imgObj;

        Bitmap thumb = Bitmap.createScaledBitmap(image, 150, 150, true);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        thumb.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        msg.thumbData = bos.toByteArray();

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("img");
        req.message = msg;
        req.scene = SendMessageToWX.Req.WXSceneSession;

        wxapi.sendReq(req);
    }

    @Override
    public void onReq(BaseReq baseReq) {

    }

    @Override
    public void onResp(BaseResp baseResp) {
        if (baseResp instanceof SendMessageToWX.Resp) {
            return;
        } else if (baseResp instanceof SendAuth.Resp) {
            SendAuth.Resp resp = (SendAuth.Resp) baseResp;
            boolean ok = false;
            String errMsg = "登录失败";
            if (resp.errCode == 0 && resp.state.equals(globalConfig.getWxState())) {
                String accessTokenUrl = String.format("https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
                        globalConfig.getWxAppKey(), globalConfig.getWxAppSecret(), resp.code);

                try {
                    HttpsURLConnection conn = createGetAndConnect(accessTokenUrl);

                    if (conn.getResponseCode() == 200) {
                        InputStreamReader reader = new InputStreamReader(conn.getInputStream(), "UTF-8");
                        WXTokenResult result = gson.fromJson(reader, WXTokenResult.class);
                        globalConfig.setWxAccessToken(result.getAccess_token());
                        globalConfig.setWxRefreshToken(result.getRefresh_token());
                        //get info
                        String infoUrl = String.format("https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s",
                                result.getAccess_token(), result.getOpenid());
                        conn = createGetAndConnect(infoUrl);

                        if (conn.getResponseCode() == 200) {
                            reader = new InputStreamReader(conn.getInputStream(), "UTF-8");
                            WXUserInfoResult userInfo = gson.fromJson(reader, WXUserInfoResult.class);

                            ServerAPI.UserData userData = ServerAPI.getInstance().wxLogin(userInfo.getOpenid(), userInfo.getNickname(), userInfo.getHeadimgurl(),
                                    userInfo.getGender(), userInfo.getProvince(), userInfo.getCity(), userInfo.getCountry());
                            if (userData.getCode() == ServerError.OK) {
                                globalConfig.setUserInfo(userData.getUser());
                                globalConfig.saveUserInfoToLocal(userData.getUser());
                                ok = true;
                            } else {
                                Log.e(tag, "wxlogin failed with code = " + userData.getCode());
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(tag, e.toString());
                }
            }

            if (ok) {
                startRefreshTimer();
            } else {
                if (errMsg != null) {
                    Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }

    private HttpsURLConnection createGetAndConnect(String url) throws Exception {
        HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
        conn.setReadTimeout(globalConfig.getReadTimeout());
        conn.setConnectTimeout(globalConfig.getConnectTimeout());
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.connect();

        return conn;
    }

    private void startRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer.purge();
        }

        refreshTimer = new Timer();
        refreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (globalConfig.getWxAccessToken() != null && globalConfig.getWxRefreshToken() != null) {
                    int retryCount = 0;
                    while (retryCount < 3) {
                        try {
                            retryCount++;
                            Thread.sleep(10 * 1000);
                            refreshAccessToken();
                            break;//go out of while
                        } catch (Exception e) {
                            Log.e(tag, e.toString());
                        }
                    }
                }
            }
        }, 0, ONE_HOUR);
    }

    private void refreshAccessToken() throws Exception {
        String url = String.format("https://api.weixin.qq.com/sns/oauth2/refresh_token?appid=%s&grant_type=refresh_token&refresh_token=%s",
                globalConfig.getWxAppKey(), globalConfig.getWxRefreshToken());

        HttpsURLConnection conn = createGetAndConnect(url);
        if (conn.getResponseCode() == 200) {
            InputStreamReader reader = new InputStreamReader(conn.getInputStream(), "UTF-8");
            WXTokenResult result = gson.fromJson(reader, WXTokenResult.class);
            globalConfig.setWxAccessToken(result.getAccess_token());
            globalConfig.setWxRefreshToken(result.getRefresh_token());
            Log.i(tag, String.format("access token = %s, refresh token = %s", result.getAccess_token(), result.getRefresh_token()));
        }
    }

    class HostnameVerifier implements javax.net.ssl.HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    class TrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    class WXUserInfoResult {
        private String openid;
        private String nickname;
        private int sex;
        private String province;
        private String city;
        private String country;
        private String headimgurl;
        private ArrayList<String> privilege;
        private String unionid;

        public String getOpenid() {
            return openid;
        }

        public String getNickname() {
            return nickname;
        }

        public int getSex() {
            return sex;
        }

        public String getGender() {
            if (sex == 1)
                return "男";
            else if (sex == 2)
                return "女";
            else
                return "未知";
        }

        public String getProvince() {
            return province;
        }

        public String getCity() {
            return city;
        }

        public String getCountry() {
            return country;
        }

        public String getHeadimgurl() {
            return headimgurl;
        }

        public ArrayList<String> getPrivilege() {
            return privilege;
        }

        public String getUnionid() {
            return unionid;
        }
    }

    class WXTokenResult {
        private String access_token;
        private int expires_in;
        private String refresh_token;
        private String openid;
        private String scope;

        public String getAccess_token() {
            return access_token;
        }

        public int getExpires_in() {
            return expires_in;
        }

        public String getRefresh_token() {
            return refresh_token;
        }

        public String getOpenid() {
            return openid;
        }

        public String getScope() {
            return scope;
        }
    }
}
