package com.ifingers.yunwb.utility;

/**
 * Created by Macoo on 2/3/2016.
 */
public class RequestActions {
    public final static String Login = "user/login";
    public final static String Register = "user/register";
    public final static String UpdateUser = "user/update";
    public final static String GetUserList = "user/list";
    public final static String AppConfig = "appconfig";
    public final static String LaunchConference = "conference/launch";
    public final static String EndConference = "conference/end";
    public final static String JoinConference = "conference/join";
    public final static String UpdateConference = "conference/update";
    public final static String UploadJPGImage = "conference/uploadImage/%s/jpg/%s";
    public final static String UploadPNGImage = "conference/uploadImage/%s/png/%s";
    public final static String SyncWhiteBoard = "meeting/%s/snapshot";
    public final static String PostHeartbeat = "meeting/%s/heartbeat";
    public final static String PostFrame = "meeting/%s/frame";
    public final static String PostSnapshot = "meeting/%s/snapshot";
    public final static String GetWhiteBoardFrameList = "meeting/%s/frames/%d";
    public final static String AliveConference = "conference/alive";
    public final static String ConferenceInfo = "conference/%s/%s";
    public final static String WXLogin = "user/wxlogin";
    public final static String ShoutIAmOnline = "meeting/%s/iamonline/%s";
    public final static String ShoutIAmOffline = "meeting/%s/iamoffline/%s";
    public final static String UploadAvatar = "user/%s/avatar";
}
