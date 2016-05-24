package com.mediatek.contacts;

/**
 * This utility class is the main entrance to print log with Android Log class.
 * Our application should always use this class to print MTK extension logs.
 */
public final class LogUtils {
    public static final boolean DEBUG = true;
    private static final String LOG_TAG = "ContactsApp/";

    public static void v(String tag, String msg) {
        if (DEBUG) {
            android.util.Log.v(LOG_TAG + tag, msg);
        }
    }

    public static void v(String tag, String msg, Throwable t) {
        if (DEBUG) {
            android.util.Log.v(LOG_TAG + tag, msg, t);
        }
    }

    public static void d(String tag, String msg) {
        if (DEBUG) {
            android.util.Log.d(LOG_TAG + tag, msg);
        }
    }

    public static void d(String tag, String msg, Throwable t) {
        if (DEBUG) {
            android.util.Log.d(LOG_TAG + tag, msg, t);
        }
    }

    public static void i(String tag, String msg) {
        if (DEBUG) {
            android.util.Log.i(LOG_TAG + tag, msg);
        }
    }

    public static void i(String tag, String msg, Throwable t) {
        if (DEBUG) {
            android.util.Log.i(LOG_TAG + tag, msg, t);
        }
    }

    public static void w(String tag, String msg) {
        if (DEBUG) {
            android.util.Log.w(LOG_TAG + tag, msg);
        }
    }

    public static void w(String tag, String msg, Throwable t) {
        if (DEBUG) {
            android.util.Log.w(LOG_TAG + tag, msg, t);
        }
    }

    public static void e(String tag, String msg) {
        if (DEBUG) {
            android.util.Log.e(LOG_TAG + tag, msg);
        }
    }

    public static void e(String tag, String msg, Throwable t) {
        if (DEBUG) {
            android.util.Log.e(LOG_TAG + tag, msg, t);
        }
    }

    public static void wtf(String tag, String msg) {
        if (DEBUG) {
            android.util.Log.wtf(LOG_TAG + tag, msg);
        }
    }

    public static void wtf(String tag, String msg, Throwable t) {
        if (DEBUG) {
            android.util.Log.wtf(LOG_TAG + tag, msg, t);
        }
    }
}
