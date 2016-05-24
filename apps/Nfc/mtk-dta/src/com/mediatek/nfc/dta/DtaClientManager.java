package com.mediatek.nfc.dta;

import android.content.Context;
import android.util.Log;

public class DtaClientManager {
    private static final String TAG = "DtaClientManager";

    private Context mContext;

    public DtaClientManager(Context context) {
        mContext = context;
    }

    public void enable() {
        Log.d(TAG, "enable");
    }

    public void disable() {
        Log.d(TAG, "disable");
    }

    public void onLlcpActivated() {
        Log.d(TAG, "onLlcpActivated");
    }

    public void onLlcpDeactivated() {
        Log.d(TAG, "onLlcpDeactivated");
    }

}