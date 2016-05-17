package com.ifingers.yunwb.utility;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.webkit.URLUtil;

import com.ifingers.yunwb.tasks.TaskMsg;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * send/receive http request
 * Created by Macoo on 2/3/2016.
 */
public class NetworkWorker {

    /**
     * verify wireless is enabled or not
     * @param context
     * @return
     */
    public static boolean isOnline(Context context){
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        return (networkInfo == null || networkInfo.isConnected());
    }

    /**
     * create a connection obj for use
     * @param urlStr: server url
     * @param action: login/register etc
     * @return
     * @throws IOException
     */
    public static HttpURLConnection generateConnection(String urlStr, String action, String method) throws IOException {
        if (!urlStr.endsWith("/"))
            urlStr += "/";

        String actionUrl = urlStr + action;

        URL url = new URL(actionUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        //keep-alive by default;
        conn.setReadTimeout(WhiteboardTaskContext.getInstance().getReadTimeout() /* milliseconds */);
        conn.setConnectTimeout(WhiteboardTaskContext.getInstance().getConnectTimeout() /* milliseconds */);
        conn.setRequestMethod(method);
        conn.setDoInput(true);

        return conn;
    }

    public static void setJsonConnection(HttpURLConnection urlConnection){
        urlConnection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        urlConnection.setRequestProperty("Accept", "application/json");
        urlConnection.setDoOutput(true);
    }

    public static void setOctetConnection(HttpURLConnection urlConnection) {
        urlConnection.setRequestProperty("Content-Type", "application/octet-stream");
        urlConnection.setDoOutput(true);
    }
}
