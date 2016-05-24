
package com.android.bluetooth.bip;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

/** BIP service. */
public class BluetoothBipService extends Service {
    private static final String TAG = "BluetoothBipService";
    private static final boolean D = true;
    private static final boolean V = true;

    private Context mContext;
    private BluetoothAdapter mAdapter;
    private BluetoothBipCoverArt mCoverArt = null;

    /** onCreate. */
    public void onCreate() {
        Log.i(TAG, "Bluetooth Bip Service is created");
        mContext = getApplicationContext();
        mAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /** onStartCommand.
     * @param intent intent
     * @param flags flags
     * @param startId startId
     * @return int
     */
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = null;
        int state = BluetoothAdapter.ERROR;

        Log.i(TAG, "Bluetooth Bip Service is started");
        if (intent != null) {
            action = intent.getStringExtra("action");
            state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            Log.i(TAG, "action=" + action + "state=" + state);
        }

        if (mAdapter == null || mAdapter.getState() != BluetoothAdapter.STATE_ON ||
                (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)
                 && state == BluetoothAdapter.STATE_TURNING_OFF)) {
            /* TODO...? */
        } else {
            if (mCoverArt == null) {
                Log.i(TAG, "Start BIP cover art session...");
                mCoverArt = new BluetoothBipCoverArt(mContext);
            }
        }
        return START_STICKY;
    }

    /** onDestroy. */
    public void onDestroy() {
        Log.i(TAG, "Bluetooth Bip Service is destroyed");
        if (mCoverArt != null) {
            mCoverArt.close();
        }
        mCoverArt = null;
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        }
    };

    /** onBind.
    * @param intent intent
    * @return IBinder
    */
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Enter onBind(), action = " + intent.getAction());
        return null;
    }

}
