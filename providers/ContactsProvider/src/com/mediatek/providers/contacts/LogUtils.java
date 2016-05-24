package com.mediatek.providers.contacts;

import android.util.Log;

/**
 * M: All log entrance.
 */
public class LogUtils {

    /**
     * M: use for Log.v.
     * @param tag tag
     * @param msg msg
     */
    public static void v(String tag, String msg) {
        Log.v(tag, msg);
    }

    /**
     * M: use for Log.v.
     * @param tag tag
     * @param msg msg
     * @param e e
     */
    public static void v(String tag, String msg, Throwable e) {
        Log.v(tag, msg, e);
    }

    /**
     * M: use for Log.d.
     * @param tag tag
     * @param msg msg
     */
    public static void d(String tag, String msg) {
        Log.d(tag, msg);
    }

    /**
     * M: use for Log.d.
     * @param tag tag
     * @param msg msg
     * @param e e
     */
    public static void d(String tag, String msg, Throwable e) {
        Log.d(tag, msg, e);
    }

    /**
     * M: use for Log.i.
     * @param tag tag
     * @param msg msg
     */
    public static void i(String tag, String msg) {
        Log.i(tag, msg);
    }

    /**
     * M: use for Log.i.
     * @param tag tag
     * @param msg msg
     * @param e e
     */
    public static void i(String tag, String msg, Throwable e) {
        Log.i(tag, msg, e);
    }

    /**
     * M: use for Log.w.
     * @param tag tag
     * @param msg msg
     */
    public static void w(String tag, String msg) {
        Log.w(tag, msg);
    }

    /**
     * M: use for Log.w.
     * @param tag tag
     * @param msg msg
     * @param e e
     */
    public static void w(String tag, String msg, Throwable e) {
        Log.w(tag, msg, e);
    }

    /**
     * M: use for Log.e.
     * @param tag tag
     * @param msg msg
     */
    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }

    /**
     * M: use for Log.e.
     * @param tag tag
     * @param msg msg
     * @param e e
     */
    public static void e(String tag, String msg, Throwable e) {
        Log.e(tag, msg, e);
    }

}