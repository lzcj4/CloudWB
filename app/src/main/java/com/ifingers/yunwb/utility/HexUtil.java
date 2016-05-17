package com.ifingers.yunwb.utility;

import android.util.Log;

import java.util.List;

/**
 * Created by Nick_PC on 2016/5/17.
 */
public class HexUtil {

    public static int byteToUnsignedByte(byte b) {
        return b & 0xFF;
    }

    public static String byteToString(String tag, byte[] bytes) {
        if (null == bytes || bytes.length == 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        String result = sb.toString();
        Log.d(tag, String.format("data len=%s ,data detail:%s", bytes.length, result));
        return result;
    }

    public static String byteToString(String tag, List<Byte> list) {
        byte[] bytes = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            bytes[i] = list.get(i);
        }
        return byteToString(tag, bytes);
    }
}
