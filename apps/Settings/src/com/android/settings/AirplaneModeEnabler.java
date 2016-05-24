/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.android.internal.telephony.TelephonyProperties;
import com.android.settingslib.WirelessUtils;
import com.mediatek.internal.telephony.ITelephonyEx;

public class AirplaneModeEnabler implements Preference.OnPreferenceChangeListener {

    private final Context mContext;
    private PhoneStateIntentReceiver mPhoneStateReceiver;
    private final SwitchPreference mSwitchPref;

    private static final int EVENT_SERVICE_STATE_CHANGED = 3;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_SERVICE_STATE_CHANGED:
                    onAirplaneModeChanged();
                    break;
            }
        }
    };

    private ContentObserver mAirplaneModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            /// M: for ALPS02476322, if the same value it means set by self, no need to update
            if (mSwitchPref.isChecked() != WirelessUtils.isAirplaneModeOn(mContext)) {
                Log.d(TAG, "airplanemode changed by others, update UI...");
                onAirplaneModeChanged();
            }
        }
    };

    public AirplaneModeEnabler(Context context, SwitchPreference airplaneModeSwitchPreference) {
        mContext = context;
        mSwitchPref = airplaneModeSwitchPreference;

        airplaneModeSwitchPreference.setPersistent(false);

        mPhoneStateReceiver = new PhoneStateIntentReceiver(mContext, mHandler);
        mPhoneStateReceiver.notifyServiceState(EVENT_SERVICE_STATE_CHANGED);

    }

    public void resume() {
        mSwitchPref.setChecked(WirelessUtils.isAirplaneModeOn(mContext));
        mPhoneStateReceiver.registerIntent();
        mSwitchPref.setOnPreferenceChangeListener(this);
        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON), true,
                mAirplaneModeObserver);

        /// M: for [Enhanced Airplane Mode] @{
        mSwitchPref.setEnabled(isAirplaneModeAvailable());
        IntentFilter intentFilter = new IntentFilter(ACTION_AIRPLANE_CHANGE_DONE);
        mContext.registerReceiver(mReceiver, intentFilter);
        /// @}
    }

    public void pause() {
        mPhoneStateReceiver.unregisterIntent();
        mSwitchPref.setOnPreferenceChangeListener(null);
        mContext.getContentResolver().unregisterContentObserver(mAirplaneModeObserver);

        /// M: for [Enhanced Airplane Mode] @{
        mContext.unregisterReceiver(mReceiver);
        /// @}
    }

    private void setAirplaneModeOn(boolean enabling) {
        // Change the system setting
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON,
                                enabling ? 1 : 0);
        // Update the UI to reflect system setting
        mSwitchPref.setChecked(enabling);

        // Post the intent
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", enabling);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);

        /// M: for [Enhanced Airplane Mode]
        // disable the switch to prevent quick click until switch is done
        mSwitchPref.setEnabled(false);
    }

    /**
     * Called when we've received confirmation that the airplane mode was set.
     * TODO: We update the checkbox summary when we get notified
     * that mobile radio is powered up/down. We should not have dependency
     * on one radio alone. We need to do the following:
     * - handle the case of wifi/bluetooth failures
     * - mobile does not send failure notification, fail on timeout.
     */
    private void onAirplaneModeChanged() {
        mSwitchPref.setChecked(WirelessUtils.isAirplaneModeOn(mContext));

        /// M: for [Enhanced Airplane Mode]
        mSwitchPref.setEnabled(isAirplaneModeAvailable());
    }

    /**
     * Called when someone clicks on the checkbox preference.
     */
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (Boolean.parseBoolean(
                    SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {
            // In ECM mode, do not update database at this point
        } else {
            Boolean value = (Boolean) newValue;
            MetricsLogger.action(mContext, MetricsLogger.ACTION_AIRPLANE_TOGGLE, value);
            setAirplaneModeOn(value);
        }
        return true;
    }

    public void setAirplaneModeInECM(boolean isECMExit, boolean isAirplaneModeOn) {
        if (isECMExit) {
            // update database based on the current checkbox state
            setAirplaneModeOn(isAirplaneModeOn);
        } else {
            // update summary
            onAirplaneModeChanged();
        }
    }

    ///-------------------------------------------------MTK---------------------------------------
    private static final String TAG = "AirplaneModeEnabler";

    /// M: for [Enhanced Airplane Mode] @{
    private static final String ACTION_AIRPLANE_CHANGE_DONE
                                    = "com.mediatek.intent.action.AIRPLANE_CHANGE_DONE";
    private static final String EXTRA_AIRPLANE_MODE = "airplaneMode";
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(ACTION_AIRPLANE_CHANGE_DONE.equals(action)) {
                boolean airplaneMode = intent.getBooleanExtra(EXTRA_AIRPLANE_MODE, false);
                Log.d(TAG, "onReceive, ACTION_AIRPLANE_CHANGE_DONE, " + airplaneMode);
                mSwitchPref.setEnabled(isAirplaneModeAvailable());
            }
        }
    };

    private boolean isAirplaneModeAvailable() {
        ITelephonyEx telephonyEx = ITelephonyEx.Stub.asInterface(
                ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
        boolean isAvailable = false;
        try {
            if (telephonyEx != null) {
                isAvailable = telephonyEx.isAirplanemodeAvailableNow();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "isAirplaneModeAvailable = " + isAvailable);
        return isAvailable;
    }
    /// @}
}
