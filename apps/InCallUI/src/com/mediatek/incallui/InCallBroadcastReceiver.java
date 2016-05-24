
package com.mediatek.incallui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.android.incallui.Log;
import com.android.incallui.StatusBarNotifier;

/**
 * M: This BroadcastReceiver is registered in the AndroidManifest.xml so as to receive
 * broadcast even if the whole process has dead.
 * This gain InCallUI a chance to clear the Notification after the bind with Telecom
 * break for any unexpected reason.
 * Find more info in the InCallController.java in the Telecom package.
 */
public class InCallBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION_UPDATE_UI_FORCED =
            "com.android.incallui.ACTION_UPDATE_UI_FORCED";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.i(this, "Broadcast from Telecom: " + action);

        if (action.equals(ACTION_UPDATE_UI_FORCED)) {
            StatusBarNotifier.clearAllCallNotifications(context);
        } else {
            Log.d(this, "Unkown type action. ");
        }
    }
}
