package com.android.server.telecom.testapps;

/**
 * M: added for debugging.
 */
public class Log {
    private static final String TAG = "TelecomTest";

    public static void i(String tag, String msg) {
        android.util.Log.i(TAG, getMessage(tag, msg));
    }

    public static void e(String tag, String msg) {
        android.util.Log.e(TAG, getMessage(tag, msg));
    }

    public static void w(String tag, String msg) {
        android.util.Log.w(TAG, getMessage(tag, msg));
    }

    public static void d(String tag, String msg) {
        android.util.Log.d(TAG, getMessage(tag, msg));
    }

    public static void v(String tag, String msg) {
        android.util.Log.v(TAG, getMessage(tag, msg));
    }

    private static String getMessage(String tag, String msg) {
        return tag + " - " + msg;
    }
}
