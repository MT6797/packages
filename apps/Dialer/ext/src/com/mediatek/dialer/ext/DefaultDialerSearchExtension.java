package com.mediatek.dialer.ext;

import android.content.Context;
import android.telecom.PhoneAccountHandle;
import android.util.Log;
import android.view.View;

public class DefaultDialerSearchExtension implements IDialerSearchExtension {

    private static final String TAG = "DefaultDialerSearchExtension";

    /**
     * Remove call account info if it exists in contact item
     * @param Context context
     * @param View view
     * @param PhoneAccountHandle phoneAccountHandle
     */
    public void setCallAccountForDialerSearch(Context context, View view,
            PhoneAccountHandle phoneAccountHandle) {
        log("setCallAccountForDialerSearch");
    }

    /**
     * for OP09
     * @param Context context
     * @param View view
     */
    public void removeCallAccountForDialerSearch(Context context, View view) {
        log("removeCallAccountForDialerSearch");
    }

    private void log(String msg) {
        Log.d(TAG, msg + " default");
    }
}
