/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.soundrecorder;

import android.util.Log;


public class LogUtils {
    private static final boolean SR_XLOG_ENABLED = true;

    public static final int v(String tag, String msg) {
        int result;
        if (SR_XLOG_ENABLED) {
            result = Log.v("@M_" + tag, msg);
        } else {
            result = Log.v(tag, msg);
        }
        return result;
    }

    public static final int v(String tag, String msg, Throwable tr) {
        int result;
        if (SR_XLOG_ENABLED) {
            result = Log.v("@M_" + tag, msg, tr);
        } else {
            result = Log.v(tag, msg, tr);
        }
        return result;
    }

    public static final int d(String tag, String msg) {
        int result;
        if (SR_XLOG_ENABLED) {
            result = Log.d("@M_" + tag, msg);
        } else {
            result = Log.d(tag, msg);
        }
        return result;
    }

    public static final int d(String tag, String msg, Throwable tr) {
        int result;
        if (SR_XLOG_ENABLED) {
            result = Log.d("@M_" + tag, msg, tr);
        } else {
            result = Log.d(tag, msg, tr);
        }
        return result;
    }

    public static final int i(String tag, String msg) {
        int result;
        if (SR_XLOG_ENABLED) {
            result = Log.i("@M_" + tag, msg);
        } else {
            result = Log.i(tag, msg);
        }
        return result;
    }

    public static final int i(String tag, String msg, Throwable tr) {
        int result;
        if (SR_XLOG_ENABLED) {
            result = Log.i("@M_" + tag, msg, tr);
        } else {
            result = Log.i(tag, msg, tr);
        }
        return result;
    }

    public static final int w(String tag, String msg) {
        int result;
        if (SR_XLOG_ENABLED) {
            result = Log.w("@M_" + tag, msg);
        } else {
            result = Log.w(tag, msg);
        }
        return result;
    }

    public static final int w(String tag, String msg, Throwable tr) {
        int result;
        if (SR_XLOG_ENABLED) {
            result = Log.w("@M_" + tag, msg, tr);
        } else {
            result = Log.w(tag, msg, tr);
        }
        return result;
    }

    public static final int e(String tag, String msg) {
        int result;
        if (SR_XLOG_ENABLED) {
            result = Log.e("@M_" + tag, msg);
        } else {
            result = Log.e(tag, msg);
        }
        return result;
    }

    public static final int e(String tag, String msg, Throwable tr) {
        int result;
        if (SR_XLOG_ENABLED) {
            result = Log.e("@M_" + tag, msg, tr);
        } else {
            result = Log.e(tag, msg, tr);
        }
        return result;
    }

}
