
package com.android.bluetooth.bip;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.util.Log;

/** BIP broadcast receiver. */
public class BluetoothBipReceiver extends BroadcastReceiver {

    private static final String TAG = "BluetoothBipReceiver";
    private static final boolean D = true;
    private static final String FEATURE_COVER_ART_ON = "1";

    @Override
    public void onReceive(Context context, Intent intent) {

        int btState;
        String action = intent.getAction();
        String featureOption = SystemProperties.get("bt.profiles.bip.coverart.enable");
        Log.i(TAG, "[onReceive] featureOption = " + featureOption);

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED) &&
            FEATURE_COVER_ART_ON.equals(featureOption)) {

            btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            Log.i(TAG, "[onReceive] ACTION_STATE_CHANGED, btState = " + btState);

            if (btState == BluetoothAdapter.STATE_ON) {
                context.startService(new Intent(context, BluetoothBipService.class));
            } else if (btState == BluetoothAdapter.STATE_OFF) {
                context.stopService(new Intent(context, BluetoothBipService.class));
            }
        }

    }

}



