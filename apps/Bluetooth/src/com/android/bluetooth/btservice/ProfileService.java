/*
* Copyright (C) 2015 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
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

package com.android.bluetooth.btservice;

import java.util.HashMap;

import com.android.bluetooth.Utils;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

public abstract class ProfileService extends Service {
    private static final boolean DBG = true;
    private static final String TAG = "BluetoothProfileService";

    //For Debugging only
    private static HashMap<String, Integer> sReferenceCount = new HashMap<String,Integer>();

    public static final String BLUETOOTH_ADMIN_PERM =
            android.Manifest.permission.BLUETOOTH_ADMIN;
    public static final String BLUETOOTH_PERM = android.Manifest.permission.BLUETOOTH;
    public static final String BLUETOOTH_PRIVILEGED =
        android.Manifest.permission.BLUETOOTH_PRIVILEGED;

    public static interface IProfileServiceBinder extends IBinder {
        public boolean cleanup();
    }
    //Profile services will not be automatically restarted.
    //They must be explicitly restarted by AdapterService
    private static final int PROFILE_SERVICE_MODE=Service.START_NOT_STICKY;
    protected String mName;
    protected BluetoothAdapter mAdapter;
    protected IProfileServiceBinder mBinder;
    protected boolean mStartError=false;
    private boolean mCleaningUp = false;

    private AdapterService mAdapterService;

    /// M: [ALPS01066845] Avoid the abnormal case when bt service receives stop intent without being started first @{
    protected boolean mProfileStarted = false;
    /// @}

    protected String getName() {
        return getClass().getSimpleName();
    }

    protected boolean isAvailable() {
        return !mStartError && !mCleaningUp;
    }

    protected abstract IProfileServiceBinder initBinder();
    protected abstract boolean start();
    protected abstract boolean stop();
    protected boolean cleanup() {
        return true;
    }

    protected ProfileService() {
        mName = getName();
        if (DBG) {
            synchronized (sReferenceCount) {
                Integer refCount = sReferenceCount.get(mName);
                if (refCount==null) {
                    refCount = 1;
                } else {
                    refCount = refCount+1;
                }
                sReferenceCount.put(mName, refCount);
                if (DBG) log("REFCOUNT: CREATED. INSTANCE_COUNT=" +refCount);
            }
        }
    }

    protected void finalize() {
        if (DBG) {
            synchronized (sReferenceCount) {
                Integer refCount = sReferenceCount.get(mName);
                if (refCount!=null) {
                    refCount = refCount-1;
                } else {
                    refCount = 0;
                }
                sReferenceCount.put(mName, refCount);
                log("REFCOUNT: FINALIZED. INSTANCE_COUNT=" +refCount);
            }
        }
    }

    @Override
    public void onCreate() {
        if (DBG) log("onCreate");
        super.onCreate();
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mBinder = initBinder();
        mAdapterService = AdapterService.getAdapterService();
        if (mAdapterService != null) {
            mAdapterService.addProfile(this);
        } else {
            Log.w(TAG, "onCreate, null mAdapterService");
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DBG) log("onStartCommand()");
        if (mStartError || mAdapter == null) {
            Log.w(mName, "Stopping profile service: device does not have BT");
            doStop(intent);
            return PROFILE_SERVICE_MODE;
        }

        if (checkCallingOrSelfPermission(BLUETOOTH_ADMIN_PERM)!=PackageManager.PERMISSION_GRANTED) {
            Log.e(mName, "Permission denied!");
            return PROFILE_SERVICE_MODE;
        }

        if (intent == null) {
            Log.d(mName, "Restarting profile service...");
            return PROFILE_SERVICE_MODE;
        } else {
            String action = intent.getStringExtra(AdapterService.EXTRA_ACTION);
            if (AdapterService.ACTION_SERVICE_STATE_CHANGED.equals(action)) {
                int state= intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if(state==BluetoothAdapter.STATE_OFF) {
                    Log.d(mName, "Received stop request...Stopping profile...");

                    /// M: [ALPS01066845] Avoid the abnormal case when bt service receives stop intent without being started first @{
                    if (false == mProfileStarted)
                    {
                        Log.w(mName, "Error stopping profile. Profile is not started yet");
                        return PROFILE_SERVICE_MODE;
                    }
                    /// @}

                    doStop(intent);
                } else if (state == BluetoothAdapter.STATE_ON) {
                    Log.d(mName, "Received start request. Starting profile...");

                    /// M: [ALPS01066845] Avoid the abnormal case when bt service receives stop intent without being started first @{
                    ///if(true == mProfileStarted)
                    ///{
                    ///    Log.w(mName, "Error starting profile. Profile is already started");
                    ///    return PROFILE_SERVICE_MODE;
                    ///}
                    /// @}

                    doStart(intent);
                }
            }
        }
        return PROFILE_SERVICE_MODE;
    }

    public IBinder onBind(Intent intent) {
        if (DBG) log("onBind");
        return mBinder;
    }

    public boolean onUnbind(Intent intent) {
        if (DBG) log("onUnbind");
        return super.onUnbind(intent);
    }

    // for dumpsys support
    public void dump(StringBuilder sb) {
        sb.append("\nProfile: " + mName + "\n");
    }

    // with indenting for subclasses
    public static void println(StringBuilder sb, String s) {
        sb.append("  ");
        sb.append(s);
        sb.append("\n");
    }

    @Override
    public void onDestroy() {
        if (DBG) log("Destroying service.");
        if (mAdapterService != null) mAdapterService.removeProfile(this);

        /// M: ALPS01829953: Notify adapter service when profile service is destroyed which is not triggered by adapter service @{
        if (mProfileStarted) {
            Log.w(mName, "mProfileStarted: " + mProfileStarted + ", service is stopped abnormally.");
            // If profile is stopped, it's a normal case. State change has been notified in doStop(), so don't notify again.
            notifyProfileServiceStateChanged(BluetoothAdapter.STATE_OFF);
        }
        /// @}
        if (mCleaningUp) {
            if (DBG) log("Cleanup already started... Skipping cleanup()...");
        } else {
            if (DBG) log("cleanup()");
            mCleaningUp = true;
            cleanup();
            if (mBinder != null) {
                mBinder.cleanup();
                mBinder= null;
            }
        }
        super.onDestroy();
        mAdapter = null;
    }

    private void doStart(Intent intent) {
        //Start service
        if (mAdapter == null) {
            Log.e(mName, "Error starting profile. BluetoothAdapter is null");
        } else {
            if (DBG) log("start()");
            mStartError = !start();
            if (!mStartError) {
                notifyProfileServiceStateChanged(BluetoothAdapter.STATE_ON);
                mProfileStarted = true;
            } else {
                Log.e(mName, "Error starting profile. BluetoothAdapter is null");
            }
        }
    }

    private void doStop(Intent intent) {
        if (stop()) {
            if (DBG) log("stop()");
            notifyProfileServiceStateChanged(BluetoothAdapter.STATE_OFF);
            stopSelf();
            mProfileStarted = false;
        } else {
            Log.e(mName, "Unable to stop profile");
        }
    }

    protected void notifyProfileServiceStateChanged(int state) {
        //Notify adapter service
        if (mAdapterService != null) {
            mAdapterService.onProfileServiceStateChanged(getClass().getName(), state);
        }
    }

    /// @M: ALPS02338670: notify user connect/disconnect @{
    /**
     * @param device BluetoothDevice
     */
    public void notifyUserDisconnect(BluetoothDevice device) {
        mAdapterService.cleanConnectOtherProfile(device);
    }

    /**
     * notify User connect.
     */
    public void notifyUserConnect() {
        mAdapterService.cleanConnectOtherProfile();
    }
    /// @}

    public void notifyProfileConnectionStateChanged(BluetoothDevice device,
            int profileId, int newState, int prevState) {
        if (mAdapterService != null) {
            mAdapterService.onProfileConnectionStateChanged(device, profileId, newState, prevState);
        }
    }

    protected BluetoothDevice getDevice(byte[] address) {
        if(mAdapter != null){
            return mAdapter.getRemoteDevice(Utils.getAddressStringFromByte(address));
        }
        return null;
    }

    protected void log(String msg) {
        Log.d(mName, msg);
    }
}
