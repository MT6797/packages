/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.nfc.handover;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.SoundPool;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

/// M: @ {
import java.util.List;
import java.util.ArrayList;
/// }

import com.android.nfc.R;

public class PeripheralHandoverService extends Service implements BluetoothPeripheralHandover.Callback {
    static final String TAG = "PeripheralHandoverService";
    static final boolean DBG = true;

    static final int MSG_PAUSE_POLLING = 0;

    public static final String BUNDLE_TRANSFER = "transfer";
    public static final String EXTRA_PERIPHERAL_DEVICE = "device";
    public static final String EXTRA_PERIPHERAL_NAME = "headsetname";
    public static final String EXTRA_PERIPHERAL_TRANSPORT = "transporttype";

    // Amount of time to pause polling when connecting to peripherals
    private static final int PAUSE_POLLING_TIMEOUT_MS = 35000;
    private static final int PAUSE_DELAY_MILLIS = 300;

    private static final Object sLock = new Object();

    // Variables below only accessed on main thread
    final Messenger mMessenger;

    SoundPool mSoundPool;
    int mSuccessSound;
    int mStartId;

    BluetoothAdapter mBluetoothAdapter;
    NfcAdapter mNfcAdapter;
    Handler mHandler;
    BluetoothPeripheralHandover mBluetoothPeripheralHandover;
    boolean mBluetoothHeadsetConnected;
    boolean mBluetoothEnabledByNfc;

    /// M: @ {
    List<Integer> mServiceId = new ArrayList<Integer>();
    //Object mLock = new Object();
    boolean mRegister = false;
    /// }

    class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PAUSE_POLLING:
                    mNfcAdapter.pausePolling(PAUSE_POLLING_TIMEOUT_MS);
                    break;
            }
        }
    }

    final BroadcastReceiver mBluetoothStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                handleBluetoothStateChanged(intent);
            }
        }
    };

    public PeripheralHandoverService() {

        Log.d(TAG, " PeripheralHandoverService() ");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = new MessageHandler();
        mMessenger = new Messenger(mHandler);
        mBluetoothHeadsetConnected = false;
        mBluetoothEnabledByNfc = false;
        mStartId = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, " onStartCommand()  startId:" + startId);


        synchronized (sLock) {
            if (mStartId != 0) {
            /// M: @ {
                Log.d(TAG, " mServiceId.add()  startId:" + startId);
                mServiceId.add(startId);
            /// }
                mStartId = startId;
                // already running
                return START_STICKY;
            }
            mStartId = startId;
        }

        if (intent == null) {
            if (DBG) Log.e(TAG, "Intent is null, can't do peripheral handover.");
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        if (doPeripheralHandover(intent.getExtras())) {
            return START_STICKY;
        } else {
            stopSelf(startId);
            return START_NOT_STICKY;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");


        mSoundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
        mSuccessSound = mSoundPool.load(this, R.raw.end, 1);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothStatusReceiver, filter);
        mRegister = true;


        Log.d(TAG, "listen BluetoothAdapter.ACTION_STATE_CHANGED");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();


        Log.d(TAG, "onDestroy() mRegister:" + mRegister);
        if (mSoundPool != null) {
            mSoundPool.release();
        }


        if (mRegister == true) {
            Log.d(TAG, " unregisterReceiver(mBluetoothStatusReceiver) ");
        unregisterReceiver(mBluetoothStatusReceiver);
            mRegister = false;
        }
    }

    boolean doPeripheralHandover(Bundle msgData) {

        Log.d(TAG, "doPeripheralHandover()");
        if (mBluetoothPeripheralHandover != null) {
            Log.d(TAG, "Ignoring pairing request, existing handover in progress.");
            return true;
        }

        if (msgData == null) {
            return false;
        }

        BluetoothDevice device = msgData.getParcelable(EXTRA_PERIPHERAL_DEVICE);
        String name = msgData.getString(EXTRA_PERIPHERAL_NAME);
        int transport = msgData.getInt(EXTRA_PERIPHERAL_TRANSPORT);

        mBluetoothPeripheralHandover = new BluetoothPeripheralHandover(
                this, device, name, transport, this);

        if (transport == BluetoothDevice.TRANSPORT_LE) {
            mHandler.sendMessageDelayed(
                    mHandler.obtainMessage(MSG_PAUSE_POLLING), PAUSE_DELAY_MILLIS);
        }


        Log.d(TAG, " call mBluetoothAdapter.isEnabled()");
        if (mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothPeripheralHandover.start()) {
                mHandler.removeMessages(MSG_PAUSE_POLLING);
                mNfcAdapter.resumePolling();
            }
        } else {
            // Once BT is enabled, the headset pairing will be started
            if (!enableBluetooth()) {
                Log.e(TAG, "Error enabling Bluetooth.");
                mBluetoothPeripheralHandover = null;
                return false;
            }
        }

        Log.d(TAG, "doPeripheralHandover  return true");

        return true;
    }

    private void handleBluetoothStateChanged(Intent intent) {
        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                BluetoothAdapter.ERROR);

        Log.d(TAG, "handleBluetoothStateChanged  state:" + state);
        if (state == BluetoothAdapter.STATE_ON) {
            // If there is a pending device pairing, start it
            if (mBluetoothPeripheralHandover != null &&
                    !mBluetoothPeripheralHandover.hasStarted()) {
                if (!mBluetoothPeripheralHandover.start()) {
                    mNfcAdapter.resumePolling();
                }
            }
        } else if (state == BluetoothAdapter.STATE_OFF) {
            mBluetoothEnabledByNfc = false;
            mBluetoothHeadsetConnected = false;
        }
    }

    @Override
    public void onBluetoothPeripheralHandoverComplete(boolean connected) {
        // Called on the main thread
        int transport = mBluetoothPeripheralHandover.mTransport;
        mBluetoothPeripheralHandover = null;
        mBluetoothHeadsetConnected = connected;

        Log.d(TAG, "onBluetoothPeripheralHandoverComplete() mRegister:" + mRegister + " transport:" + transport + "  connected:" + connected);

        // <hack> resume polling immediately if the connection failed,
        // otherwise just wait for polling to come back up after the timeout
        // This ensures we don't disconnect if the user left the volantis
        // on the tag after pairing completed, which results in automatic
        // disconnection </hack>
        if (transport == BluetoothDevice.TRANSPORT_LE && !connected) {
            if (mHandler.hasMessages(MSG_PAUSE_POLLING)) {
                mHandler.removeMessages(MSG_PAUSE_POLLING);
            }

            // do this unconditionally as the polling could have been paused as we were removing
            // the message in the handler. It's a no-op if polling is already enabled.
            mNfcAdapter.resumePolling();
        }
        disableBluetoothIfNeeded();


        synchronized (sLock) {
            Log.d(TAG, " stopSelf()  mStartId:" + mStartId);
            stopSelf(mStartId);
            mStartId = 0;

            /// M: @ {
            for (int serviceId : mServiceId) {
                Log.d(TAG, " stopSelf()  serviceId:" + serviceId);
                stopSelf(serviceId);
            }
            /// }

        }

        /// M: @ {
        if (mRegister == true) {
            Log.d(TAG, " unregisterReceiver(mBluetoothStatusReceiver)  first");
            unregisterReceiver(mBluetoothStatusReceiver);
            mRegister = false;
        }
        /// }

    }


    boolean enableBluetooth() {

        Log.d(TAG, " enableBluetooth()");
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothEnabledByNfc = true;
            return mBluetoothAdapter.enableNoAutoConnect();
        }
        return true;
    }

    void disableBluetoothIfNeeded() {
        Log.d(TAG, " disableBluetoothIfNeeded()");

        if (!mBluetoothEnabledByNfc) return;

        if (!mBluetoothHeadsetConnected) {
            mBluetoothAdapter.disable();
            mBluetoothEnabledByNfc = false;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, " onBind() intent:" + intent);

        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // prevent any future callbacks to the client, no rebind call needed.

        Log.d(TAG, " onUnbind() intent:" + intent);
        return false;
    }
}
