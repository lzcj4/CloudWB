package com.ifingers.yunwb.utility;

/**
 * Created by SFY on 2/19/2016.
 */
public class ServerError {
    public static int OK = 0;
    public static int KEY_FIELDS_NOT_EXIST = 1;
    public static int USER_HAS_EXISTED = 2;
    public static int LOGIN_FAILED = 3;
    public static int TARGET_NOT_FOUND = 4;
    public static int UNKNOWN_ACTION = 5;
    public static int INVALID_TYPE_CONVERSION = 6;
    public static int INVALID_CLIENT_DATA = 7;
    public static int MEETING_IS_NOT_ALIVE = 8;
    public static int WBDATA_TYPE_INVALID = 9;
    public static int PARAMETER_OUT_OF_RANGE = 10;
    public static int LOST_FRAME = 11;
    public static int FILE_ALREADY_EXISTS = 12;
    public static int WRONG_CONFERENCE_PWD = 13;
    public static int MEETING_ALREADY_EXIST = 14;
    public static int SERVER_OUT_OF_CAPACITY = 500;
    public static int SERVER_NOTIFY_FAILED = 501;
    public static int SERVER_NOT_FOUND = 502;
    public static int MONGO_ERROR = 1000;
    public static int FILE_ERROR = 1001;
    public static int INTERNAL_ERROR = 9999;
    public static int UNKNOWN_ERROR = 10000;

    //client defined error
    public static int NO_SNAPSHOT = 20000;
}
