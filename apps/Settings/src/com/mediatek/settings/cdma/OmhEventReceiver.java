package com.mediatek.settings.cdma;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

public class OmhEventReceiver extends BroadcastReceiver {
    private static final String TAG = "OmhEventReceiver";
    private static final String ACTION_OMH = "com.mediatek.internal.omh.cardcheck";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_OMH.equals(intent.getAction())) {
            Bundle extra = intent.getExtras();
            int subId = extra.getInt("subid", -1);
            boolean isReady = extra.getBoolean("is_ready", false);
            Log.d(TAG, "omh card ready, subid = " + subId + ", is ready = " + isReady);
            if (CdmaUtils.isNonOmhSimInOmhDevice(subId)
                    && !CdmaUtils.hasNonOmhRecord(context, subId)) {
                CdmaUtils.recordNonOmhSub(context, subId);
                Log.d(TAG, "new OMH record, send new request...");
                Message message = OmhEventHandler.getInstance(context).obtainMessage(
                        OmhEventHandler.NEW_REQUEST, OmhEventHandler.TYPE_OMH_WARNING, -1);
                message.sendToTarget();
            }
        }
    }

}
