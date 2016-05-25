package com.ifingers.yunwb.bluetooth;
//import android.util.Log;

import android.util.Log;

/**
 * Created by Administrator on 2015/10/30.
 */
public class JYDZ_Comm_Protocol {
    public static final int COMM_STATUS_GET_HEADER = 0;
    public static final int COMM_STATUS_GET_LENGTH = 1;
    public static final int COMM_STATUS_GET_FEATURE = 2;
    public static final int COMM_STATUS_GET_DATA = 3;
    public static final int COMM_STATUS_GET_CHECKSUM = 4;
    public static final int COMM_STATUS_CHANGE_FORMAT = 5;
    public static final int COMM_STATUS_DATA_GET_OK = 6;
    public static final int COMM_STATUS_GESTURE_GET = 7;
    public static final int COMM_STATUS_SNAPSHOT_GET = 8;
    public static final int COMM_STATUS_IDENTI_GET = 9;
    public static final int COMM_STATUS_SCREENFEATURE_GET = 10;

    public static final int COMM_STATUS_ERROR = -1;
    public static final int COMM_STATUS_GET_LENGTH_ERROR = -2;
    public static final int COMM_STATUS_GET_FEATURE_ERROR = -3;
    public static final int COMM_STATUS_GET_CHECKSUM_ERROR = -4;

    public static final int DATAFEATURE_00 = 0;
    public static final int DATAFEATURE_01 = 1;
    public static final int DATAFEATURE_02 = 2;
    public static final int SCREENFEATURE = 0X60;
    public static final int GESTURE = 0X70;
    public static final int CONTROLCODE = 0X80;
    public static final int PACKAGE_TRANSCMD = 0X81;
    public static final int CONTROLCODE_USB = 1;
    public static final int CONTROLCODE_NUSB = 0;

    public static final int SNAPSHOT = 0X71;
    public static final int IDENTI = 0X73;

    public static final byte[] PACKAGE_TRANSCMD_BYTE00 = {0x68, 3, -127, 0, -20};
    public static final byte[] PACKAGE_TRANSCMD_BYTE01 = {0x68, 3, -127, 1, -19};
    public static final byte[] PACKAGE_TRANSCMD_BYTE02 = {0x68, 3, -127, 2, -18};

    public static final String TAG = "JY_PROTOCOL";

    public static final int MESSAGE_UART_CMD_GET = 100;

    public static final byte JYDZ_PROTOCOL_HEADER = 0x68;
    public static final int JYDZ_PROTOCOL_MAX_LENGTH = 40;

    public static final int COMM_CMD_OK = 1;
    public static final int COMM_CMD_FALSE = 0;

    private int commStatus;
    private int commLastStatus;
    private int commLength;
    private int commCmdType;
    private int commCmdState;
    private int commdataFeatrue;
    private int commControlCode;

    private int commDataCtr;
    private int commCheckSum;
    public int mPointCount = 0;
    public int[] dataBuffer;
    private TouchScreen JY_TouchScreen;
    private IrmtInterface mHandler;

    public JYDZ_Comm_Protocol(TouchScreen TouchScreen, IrmtInterface handler) {
        JY_TouchScreen = TouchScreen;
        commStatus = COMM_STATUS_GET_HEADER;
        commLength = 0;
        commCmdType = DATAFEATURE_00;
        commCmdState = COMM_CMD_FALSE;
        commDataCtr = 0;
        dataBuffer = new int[400];
        mPointCount = 0;
        commLastStatus = COMM_STATUS_GET_HEADER;
        commdataFeatrue = DATAFEATURE_00;
        commControlCode = CONTROLCODE_NUSB;
        mHandler = handler;
    }

    private boolean CheckSum() {
        int mSum = 0, Length = commLength - 2, ii;
        mSum = commLength + commCmdType + JYDZ_PROTOCOL_HEADER;
        for (ii = 0; ii < Length; ii++) {
            mSum += dataBuffer[ii];
        }
        mSum &= 0xFF;
        if (commCheckSum == mSum)
            return true;
        else
            return false;
    }

    private boolean checkDataLength() {
        int desiredLength = 0;
        switch (commCmdType) {
            case DATAFEATURE_00:
                desiredLength = 2 + mPointCount * 5;
                break;
            case DATAFEATURE_01:
                desiredLength = 2 + mPointCount * 6;
                break;
            case DATAFEATURE_02:
                desiredLength = 2 + mPointCount * 10;
                break;
            case SCREENFEATURE:
                desiredLength = 11;
                break;
            case GESTURE:
                desiredLength = 3;
                break;

            case SNAPSHOT:
                desiredLength = 3;
                break;

            case IDENTI:
                desiredLength = 6;
                break;
            default:
                desiredLength = -1;
                break;
        }
        if (desiredLength == commLength)
            return true;
        else
            return false;
    }

    void resetProtocol() {
        commStatus = JYDZ_Comm_Protocol.COMM_STATUS_GET_HEADER;
        commLastStatus = JYDZ_Comm_Protocol.COMM_STATUS_GET_HEADER;
        commDataCtr = 0;
        commCmdState = JYDZ_Comm_Protocol.COMM_CMD_FALSE;
    }

    byte[] ChangeDataFeatrue() {
        if (commdataFeatrue == DATAFEATURE_00) {
            commdataFeatrue = DATAFEATURE_01;
            return PACKAGE_TRANSCMD_BYTE01;
        } else if (commdataFeatrue == DATAFEATURE_01) {
            commdataFeatrue = DATAFEATURE_02;
            return PACKAGE_TRANSCMD_BYTE02;
        } else if (commdataFeatrue == DATAFEATURE_02) {
            commdataFeatrue = DATAFEATURE_00;
            return PACKAGE_TRANSCMD_BYTE00;
        } else
            return null;
    }

    int[] localCache = new int[1024];
    int handlerIncomeData(int numOfBytes, byte[] dataBuf) {
        int errorcode = 0;
        for (int index = 0; index < numOfBytes; index++) {
            localCache[index] = dataBuf[index] & 0xFF;

            switch (commStatus) {
                case JYDZ_Comm_Protocol.COMM_STATUS_GET_HEADER:
                    if (localCache[index] == JYDZ_Comm_Protocol.JYDZ_PROTOCOL_HEADER)
                        commStatus = JYDZ_Comm_Protocol.COMM_STATUS_GET_LENGTH;
                    break;

                case JYDZ_Comm_Protocol.COMM_STATUS_GET_LENGTH:
                    commLength = localCache[index];
                    if ((commLength >= 2)) {
                        commLastStatus = commStatus;
                        commStatus = JYDZ_Comm_Protocol.COMM_STATUS_GET_FEATURE;
                    } else {
                        errorcode = COMM_STATUS_GET_LENGTH_ERROR;
                        commLastStatus = commStatus;
                        commStatus = JYDZ_Comm_Protocol.COMM_STATUS_ERROR;
                    }
                    break;

                case JYDZ_Comm_Protocol.COMM_STATUS_GET_FEATURE:
                    commCmdType = localCache[index];
                    if (commCmdType == DATAFEATURE_00) {
                        mPointCount = (commLength - 2) / 5;
                    } else if (commCmdType == DATAFEATURE_01) {
                        mPointCount = (commLength - 2) / 6;
                    } else if (commCmdType == DATAFEATURE_02) {
                        mPointCount = (commLength - 2) / 10;
                    }
                    JY_TouchScreen.setNumOfPoints(mPointCount);

                    if (checkDataLength()) {
                        if (commLength == 2) {
                            commLastStatus = commStatus;
                            commStatus = JYDZ_Comm_Protocol.COMM_STATUS_GET_CHECKSUM;
                        } else {
                            commLastStatus = commStatus;
                            commStatus = JYDZ_Comm_Protocol.COMM_STATUS_GET_DATA;
                        }
                    } else {
                        errorcode = COMM_STATUS_GET_FEATURE_ERROR;
                        commLastStatus = commStatus;
                        commStatus = JYDZ_Comm_Protocol.COMM_STATUS_ERROR;
                    }
                    break;

                case JYDZ_Comm_Protocol.COMM_STATUS_GET_DATA:
                    dataBuffer[commDataCtr++] = localCache[index];
                    if (commDataCtr >= commLength - 2) {
                        commLastStatus = commStatus;
                        commStatus = JYDZ_Comm_Protocol.COMM_STATUS_GET_CHECKSUM;
                    }
                    break;

                case JYDZ_Comm_Protocol.COMM_STATUS_GET_CHECKSUM:
                    commCheckSum = localCache[index];
                    if (CheckSum()) {
                        commCmdState = JYDZ_Comm_Protocol.COMM_CMD_OK;
                        commLastStatus = commStatus;
                        commStatus = JYDZ_Comm_Protocol.COMM_STATUS_GET_HEADER;
                    } else {
                        errorcode = COMM_STATUS_GET_CHECKSUM_ERROR;
                        commLastStatus = commStatus;
                        commStatus = JYDZ_Comm_Protocol.COMM_STATUS_ERROR;
                    }
                    break;
                default:
                    break;
            }

            if (commStatus == JYDZ_Comm_Protocol.COMM_STATUS_ERROR) {
                Log.e("debug", "protoocal parse error " + errorcode);
                resetProtocol();
            }

            if (commCmdState == JYDZ_Comm_Protocol.COMM_CMD_OK) {
                resetProtocol();
                switch (commCmdType) {
                    case JYDZ_Comm_Protocol.DATAFEATURE_00:
                        errorcode = COMM_STATUS_CHANGE_FORMAT;
                        break;
                    case JYDZ_Comm_Protocol.DATAFEATURE_01:
                        errorcode = COMM_STATUS_CHANGE_FORMAT;
                        break;
                    case JYDZ_Comm_Protocol.DATAFEATURE_02:
                        JY_TouchScreen.parsePoints(mPointCount, dataBuffer);
                        if (JY_TouchScreen.mTouchDownList.size() > 0) {
                            mHandler.onTouchDown(JY_TouchScreen.mTouchDownList);
                            JY_TouchScreen.mTouchDownList.clear();
                        }
                        if (JY_TouchScreen.mTouchUpList.size() > 0) {
                            mHandler.onTouchUp(JY_TouchScreen.mTouchUpList);
                            JY_TouchScreen.mTouchUpList.clear();
                        }
                        errorcode = COMM_STATUS_DATA_GET_OK;
                        break;
                    case JYDZ_Comm_Protocol.SCREENFEATURE:
                        JY_TouchScreen.setIrTouchFeature(dataBuffer);
                        errorcode = COMM_STATUS_SCREENFEATURE_GET;
                        break;
                    case JYDZ_Comm_Protocol.GESTURE:
                        JY_TouchScreen.setmGuesture(dataBuffer[0]);
                        errorcode = COMM_STATUS_GESTURE_GET;
                        break;
                    case JYDZ_Comm_Protocol.SNAPSHOT:
                        JY_TouchScreen.setSnapShot(dataBuffer[0]);
                        errorcode = COMM_STATUS_SNAPSHOT_GET;
                        break;
                    case JYDZ_Comm_Protocol.IDENTI:
                        JY_TouchScreen.setID(dataBuffer[0] + dataBuffer[1] * 256 + dataBuffer[2] * 256 * 256 + dataBuffer[3] * 256 * 256 * 256);
                        errorcode = COMM_STATUS_IDENTI_GET;
                        break;
                    default:
                        break;
                }
            }
        }
        return errorcode;
    }
}