package com.mediatek.incallui;

import android.content.Context;

/**
 * A Toast helper for InCallUI, to avoid toast timing issue.
 */
public class Toast {
    private static Toast sInstance = new Toast();
    private android.widget.Toast mToast;

    private Toast() {
    }

    /**
     * Get the Toast instance.
     * @return Toast instance.
     */
    public static Toast getInstance() {
        return sInstance;
    }

    /**
     * Called in #InCallServiceImpl to set the context.
     * @param context Context to set.
     */
    public void init(Context context) {
        if (mToast == null) {
            mToast = android.widget.Toast.makeText(context, "", android.widget.Toast.LENGTH_SHORT);
        }
    }

    /**
     * Called when #InCallService quit.
     */
    public void deinit() {
        mToast = null;
    }

    /**
     * Like android.widget.Toast.show(), to show the Toast message.
     * @param resId message resId.
     */
    public void show(int resId) {
        if (mToast != null) {
            mToast.setText(resId);
            mToast.show();
        }
    }

    /**
     * Like android.widget.Toast.show(), to show the Toast message.
     * @param msg message text.
     */
    public void show(CharSequence msg) {
        if (mToast != null) {
            mToast.setText(msg);
            mToast.show();
        }
    }
}
