package com.mediatek.galleryframework.util;

import android.os.SystemProperties;
import android.util.Log;

/**
 * Adapter for log system.
 */
public final class MtkLog {
    public static final String TAG = "MtkGallery2/MtkLog";
    public static final boolean DBG;
    static {
        DBG = SystemProperties.getInt("Gallery_DBG", 0) == 1 ? true : false;
        Log.i("@M_" + TAG, "DBG = " + DBG);
    }

    private MtkLog() {
    }

    public static int v(String tag, String msg) {
        return Log.v("@M_" + tag, msg);
    }

    public static int v(String tag, String msg, Throwable tr) {
        return Log.v("@M_" + tag, msg, tr);
    }

    public static int d(String tag, String msg) {
        return Log.d("@M_" + tag, msg);
    }

    public static int d(String tag, String msg, Throwable tr) {
        return Log.d("@M_" + tag, msg, tr);
    }

    public static int i(String tag, String msg) {
        return Log.i("@M_" + tag, msg);
    }

    public static int i(String tag, String msg, Throwable tr) {
        return Log.i("@M_" + tag, msg, tr);
    }

    public static int w(String tag, String msg) {
        return Log.w("@M_" + tag, msg);
    }

    public static int w(String tag, String msg, Throwable tr) {
        return Log.w("@M_" + tag, msg, tr);
    }

    public static int w(String tag, Throwable tr) {
        return Log.w("@M_" + tag, "", tr);
    }

    public static int e(String tag, String msg) {
        return Log.e("@M_" + tag, msg);
    }

    public static int e(String tag, String msg, Throwable tr) {
        return Log.e("@M_" + tag, msg, tr);
    }
}
