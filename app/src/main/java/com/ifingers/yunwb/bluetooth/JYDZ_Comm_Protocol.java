package com.ifingers.yunwb.bluetooth;
//import android.util.Log;

import android.text.TextUtils;
import android.util.Log;

import com.ifingers.yunwb.utility.DataLog;
import com.ifingers.yunwb.utility.HexUtil;

import java.util.ArrayList;
import java.util.Arrays;

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
    private byte commdataFeatrue;
    private int commControlCode;

    private int commDataCtr;
    private int commCheckSum;
    public int mPointCount = 0;
    public int[] dataBuffer;
    private TouchScreen JY_TouchScreen;

    public JYDZ_Comm_Protocol(TouchScreen TouchScreen) {
        JY_TouchScreen = TouchScreen;
        commStatus = COMM_STATUS_GET_HEADER;
        commLength = 0;
        commCmdType = DATAFEATURE_00;
        commCmdState = COMM_CMD_FALSE;
        commDataCtr = 0;
        dataBuffer = new int[40];
        mPointCount = 0;
        commLastStatus = COMM_STATUS_GET_HEADER;
        commdataFeatrue = DATAFEATURE_00;
        commControlCode = CONTROLCODE_NUSB;
    }

    private boolean compareChecksum() {
        int dataLen = commLength - 2;
        int sum = commLength + commCmdType + JYDZ_PROTOCOL_HEADER;
        for (int i = 0; i < dataLen; i++) {
            sum += dataBuffer[i];
        }
        sum = sum & 0xFF;
        boolean result = commCheckSum == sum;
        return result;
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

    byte[] changeDataFeature() {
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

    byte[] lastUnhandleData = new byte[0];

    private byte[] combineBytes(byte[] data1, byte[] data2) {
        byte[] result = new byte[0];
        if (null == data1 && null == data2) {
            return result;
        }
        if (null == data2) {
            result = data1;
        }

        if (null == data1) {
            result = data2;
        }

        if (null != data1 && null != data2) {
            result = new byte[data1.length + data2.length];
            System.arraycopy(data1, 0, result, 0, data1.length);
            System.arraycopy(data2, 0, result, data1.length, data2.length);
        }

        return result;
    }

    int handlerIncomeData(int numOfBytes, byte[] data) {
        int errorCode = 0;
        if (numOfBytes <= 0) {
            return errorCode;
        }

        String hexStr = HexUtil.byteToString(TAG, data);
        if (!TextUtils.isEmpty(hexStr)) {
            DataLog.getInstance().writeInData(hexStr);
            DataLog.getInstance().writeInLineData(hexStr);
        }

        byte[] totalData = combineBytes(lastUnhandleData, data);
        int[] tempData = new int[totalData.length];
        if (lastUnhandleData.length != 0) {
            lastUnhandleData = new byte[0];
        }
        resetProtocol();
        Arrays.fill(dataBuffer, 0);
        int len = tempData.length;
        ArrayList<Byte> validData = new ArrayList<>();

        for (int index = 0; index < len; index++) {
            tempData[index] = HexUtil.byteToUnsignedByte(totalData[index]);
            validData.add(totalData[index]);

            switch (commStatus) {
                case JYDZ_Comm_Protocol.COMM_STATUS_GET_HEADER:
                    if (tempData[index] == JYDZ_Comm_Protocol.JYDZ_PROTOCOL_HEADER) {
                        commStatus = JYDZ_Comm_Protocol.COMM_STATUS_GET_LENGTH;
                    }
                    if (index == len - 1) {
                        lastUnhandleData = new byte[]{JYDZ_Comm_Protocol.JYDZ_PROTOCOL_HEADER};
                    }
                    break;

                case JYDZ_Comm_Protocol.COMM_STATUS_GET_LENGTH:
                    commLength = tempData[index];
                    commLastStatus = commStatus;
                    if (commLength >= 2) {
                        commStatus = JYDZ_Comm_Protocol.COMM_STATUS_GET_FEATURE;
                        //For incomplete data
                        if (index + commLength >= totalData.length) {
                            lastUnhandleData = Arrays.copyOfRange(totalData, index - 1, len);
                        }
                    } else {
                        errorCode = COMM_STATUS_GET_LENGTH_ERROR;
                        commStatus = JYDZ_Comm_Protocol.COMM_STATUS_ERROR;
                    }
                    break;

                case JYDZ_Comm_Protocol.COMM_STATUS_GET_FEATURE:
                    commCmdType = tempData[index];
                    if (commCmdType == DATAFEATURE_00) {
                        mPointCount = (commLength - 2) / 5;
                    } else if (commCmdType == DATAFEATURE_01) {
                        mPointCount = (commLength - 2) / 6;
                    } else if (commCmdType == DATAFEATURE_02) {
                        mPointCount = (commLength - 2) / 10;
                    }
                    JY_TouchScreen.setNumOfPoints(mPointCount);
                    commLastStatus = commStatus;

                    if (checkDataLength()) {
                        commStatus = commLength == 2 ?
                                JYDZ_Comm_Protocol.COMM_STATUS_GET_CHECKSUM :
                                JYDZ_Comm_Protocol.COMM_STATUS_GET_DATA;
                    } else {
                        errorCode = COMM_STATUS_GET_FEATURE_ERROR;
                        commStatus = JYDZ_Comm_Protocol.COMM_STATUS_ERROR;
                    }
                    break;

                case JYDZ_Comm_Protocol.COMM_STATUS_GET_DATA:
                    if (commDataCtr < dataBuffer.length - 1) {
                        dataBuffer[commDataCtr++] = tempData[index];
                    }
                    if (commDataCtr >= commLength - 2) {
                        commLastStatus = commStatus;
                        commStatus = JYDZ_Comm_Protocol.COMM_STATUS_GET_CHECKSUM;
                    }
                    break;

                case JYDZ_Comm_Protocol.COMM_STATUS_GET_CHECKSUM:
                    commCheckSum = tempData[index];
                    commLastStatus = commStatus;
                    if (compareChecksum()) {
                        commCmdState = JYDZ_Comm_Protocol.COMM_CMD_OK;
                        commStatus = JYDZ_Comm_Protocol.COMM_STATUS_GET_HEADER;
                    } else {
                        errorCode = COMM_STATUS_GET_CHECKSUM_ERROR;
                        commStatus = JYDZ_Comm_Protocol.COMM_STATUS_ERROR;
                    }
                    break;
                default:
                    break;
            }

            if (commStatus == JYDZ_Comm_Protocol.COMM_STATUS_ERROR) {
                Log.e(TAG, "protocol parse error " + errorCode);
                resetProtocol();
                validData.clear();
            }

            if (commCmdState == JYDZ_Comm_Protocol.COMM_CMD_OK) {
                resetProtocol();
                hexStr = HexUtil.byteToString(TAG, validData);
                if (!TextUtils.isEmpty(hexStr)) {
                    DataLog.getInstance().writeOutData(hexStr);
                }
                validData.clear();
                switch (commCmdType) {
                    case JYDZ_Comm_Protocol.DATAFEATURE_00:
                        errorCode = COMM_STATUS_CHANGE_FORMAT;
                        break;
                    case JYDZ_Comm_Protocol.DATAFEATURE_01:
                        errorCode = COMM_STATUS_CHANGE_FORMAT;
                        break;
                    case JYDZ_Comm_Protocol.DATAFEATURE_02:
                        JY_TouchScreen.parsePoints(mPointCount, dataBuffer);
                        errorCode = COMM_STATUS_DATA_GET_OK;
                        break;
                    case JYDZ_Comm_Protocol.SCREENFEATURE:
                        JY_TouchScreen.setIrTouchFeature(dataBuffer);
                        errorCode = COMM_STATUS_SCREENFEATURE_GET;
                        break;
                    case JYDZ_Comm_Protocol.GESTURE:
                        JY_TouchScreen.setmGuesture(dataBuffer[0]);
                        errorCode = COMM_STATUS_GESTURE_GET;
                        break;
                    case JYDZ_Comm_Protocol.SNAPSHOT:
                        JY_TouchScreen.setSnapShot(dataBuffer[0]);
                        errorCode = COMM_STATUS_SNAPSHOT_GET;
                        break;
                    case JYDZ_Comm_Protocol.IDENTI:
                        JY_TouchScreen.setID(dataBuffer[0] + dataBuffer[1] * 256 + dataBuffer[2] * 256 * 256 + dataBuffer[3] * 256 * 256 * 256);
                        errorCode = COMM_STATUS_IDENTI_GET;
                        break;
                    default:
                        break;
                }
            }
        }
        return errorCode;
    }
}