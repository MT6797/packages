/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.ims.ImsConfig;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.settings.widget.SwitchBar;

import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import com.mediatek.settings.ext.IWfcSettingsExt;


/**
 * "Wi-Fi Calling settings" screen.  This preference screen lets you
 * enable/disable Wi-Fi Calling and change Wi-Fi Calling mode.
 */
public class WifiCallingSettings extends SettingsPreferenceFragment
        implements SwitchBar.OnSwitchChangeListener,
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "WifiCallingSettings";

    //String keys for preference lookup
    private static final String BUTTON_WFC_MODE = "wifi_calling_mode";

    //UI objects
    private SwitchBar mSwitchBar;
    private Switch mSwitch;
    private ListPreference mButtonWfcMode;
    private TextView mEmptyView;

    private boolean mValidListener = false;

    /// M: Wfc plugin
    IWfcSettingsExt mWfcExt;

    /// M: fix Google bug: only listen to default sub, listen to Phone state change instead @{
    /*
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
    */
         /*
         * Enable/disable controls when in/out of a call and depending on
         * TTY mode and TTY support over VoLTE.
         * @see android.telephony.PhoneStateListener#onCallStateChanged(int,
         * java.lang.String)
         */
    /*
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            final SettingsActivity activity = (SettingsActivity) getActivity();
            boolean isNonTtyOrTtyOnVolteEnabled = ImsManager
                    .isNonTtyOrTtyOnVolteEnabled(activity);
            final SwitchBar switchBar = activity.getSwitchBar();
            boolean isWfcEnabled = switchBar.getSwitch().isChecked()
                    && isNonTtyOrTtyOnVolteEnabled;

            switchBar.setEnabled((state == TelephonyManager.CALL_STATE_IDLE)
                    && isNonTtyOrTtyOnVolteEnabled);

            Preference pref = getPreferenceScreen().findPreference(BUTTON_WFC_MODE);
            if (pref != null) {
                pref.setEnabled(isWfcEnabled
                        && (state == TelephonyManager.CALL_STATE_IDLE));
            }
        }
    };
    */
    /// @}
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SettingsActivity activity = (SettingsActivity) getActivity();

        mSwitchBar = activity.getSwitchBar();
        mSwitch = mSwitchBar.getSwitch();
        mSwitchBar.show();

        mEmptyView = (TextView) getView().findViewById(android.R.id.empty);
        getListView().setEmptyView(mEmptyView);
        mEmptyView.setText(R.string.wifi_calling_off_explanation);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSwitchBar.hide();
    }

    private void showAlert(Intent intent) {
        Context context = getActivity();

        CharSequence title = intent.getCharSequenceExtra(ImsPhone.EXTRA_KEY_ALERT_TITLE);
        CharSequence message = intent.getCharSequenceExtra(ImsPhone.EXTRA_KEY_ALERT_MESSAGE);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message)
                .setTitle(title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private IntentFilter mIntentFilter;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive()... " + action);
            if (action.equals(ImsManager.ACTION_IMS_REGISTRATION_ERROR)) {
                // If this fragment is active then we are immediately
                // showing alert on screen. There is no need to add
                // notification in this case.
                //
                // In order to communicate to ImsPhone that it should
                // not show notification, we are changing result code here.
                setResultCode(Activity.RESULT_CANCELED);

                // UX requirement is to disable WFC in case of "permanent" registration failures.
                mSwitch.setChecked(false);

                showAlert(intent);
            /// M: listen to WFC config changes and update the screen @{
            } else if (action.equals(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED)) {
                if (!ImsManager.isWfcEnabledByPlatform(context)) {
                    Log.d(TAG, "carrier config changed, finish WFC activity");
                    getActivity().finish();
                }
            } else if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
                updateScreen();
            }
            /// @}
        }
    };

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.WIFI_CALLING;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.wifi_calling_settings);

        /// M: for plug-in, make wfc setting plugin & add custom preferences @{
        mWfcExt = UtilsExt.getWfcSettingsPlugin(getActivity());
        mWfcExt.initPlugin(this);
        /// @}

        mButtonWfcMode = (ListPreference) findPreference(BUTTON_WFC_MODE);
        mButtonWfcMode.setOnPreferenceChangeListener(this);

        /// M: Add custom preferences & listener/register/observers for these preferences
        mWfcExt.addOtherCustomPreference();

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ImsManager.ACTION_IMS_REGISTRATION_ERROR);

        /// M: listen to Carrier config changes
        mIntentFilter.addAction(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        mIntentFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);

    }

    @Override
    public void onResume() {
        super.onResume();

        final Context context = getActivity();

        if (ImsManager.isWfcEnabledByPlatform(context)) {
            /// M: fix Google bug: only listen to default sub, @{
            // listen to Phone state change instead
            /*
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            */
            /// @}
            mSwitchBar.addOnSwitchChangeListener(this);

            mValidListener = true;
        }

        // NOTE: Buttons will be enabled/disabled in mPhoneStateListener
        boolean wfcEnabled = ImsManager.isWfcEnabledByUser(context)
                && ImsManager.isNonTtyOrTtyOnVolteEnabled(context);
        mSwitch.setChecked(wfcEnabled);
        int wfcMode = ImsManager.getWfcMode(context);
        mButtonWfcMode.setValue(Integer.toString(wfcMode));

        /// M: for plug-in
        mWfcExt.initPlugin(this);
        mWfcExt.updateWfcModePreference(getPreferenceScreen(), mButtonWfcMode, wfcEnabled, wfcMode);

        /// M: update screen
        updateScreen();
        /// @}
        context.registerReceiver(mIntentReceiver, mIntentFilter);

        Intent intent = getActivity().getIntent();
        if (intent.getBooleanExtra(ImsPhone.EXTRA_KEY_ALERT_SHOW, false)) {
            showAlert(intent);
        }

        /// M: for plug-in
        mWfcExt.onWfcSettingsEvent(DefaultWfcSettingsExt.RESUME);
    }

    @Override
    public void onPause() {
        super.onPause();

        final Context context = getActivity();

        if (mValidListener) {
            mValidListener = false;
            /// M: fix Google bug: only listen to default sub, @{
            // listen to Phone state change instead
            /*
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            */
            /// @}
            mSwitchBar.removeOnSwitchChangeListener(this);
        }

        context.unregisterReceiver(mIntentReceiver);

        /// M: for plug-in
        mWfcExt.onWfcSettingsEvent(DefaultWfcSettingsExt.PAUSE);
    }

    /**
     * Listens to the state change of the switch.
     */
    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        Log.d(TAG, "OnSwitchChanged");
        /// M:  Decide whether wfc switch is to be toggled or not @{
        /* Revert user action with toast, if IMS is enabling or disabling */
        if (isInSwitchProcess()) {
            Log.d(TAG, "[onClick] Switching process ongoing");
            Toast.makeText(getActivity(), R.string.Switch_not_in_use_string, Toast.LENGTH_SHORT)
                    .show();
            mSwitch.setChecked(!isChecked);
            return;
        }
        /// @}

        final Context context = getActivity();

        ImsManager.setWfcSetting(context, isChecked);

        int wfcMode = ImsManager.getWfcMode(context);

        /// M: for plug-in
        mWfcExt.updateWfcModePreference(getPreferenceScreen(), mButtonWfcMode, isChecked, wfcMode);

        if (isChecked) {
            MetricsLogger.action(getActivity(), getMetricsCategory(), wfcMode);
        } else {
            MetricsLogger.action(getActivity(), getMetricsCategory(), -1);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final Context context = getActivity();
        if (preference == mButtonWfcMode) {
            mButtonWfcMode.setValue((String) newValue);
            int buttonMode = Integer.valueOf((String) newValue);
            int currentMode = ImsManager.getWfcMode(context);
            if (buttonMode != currentMode) {
                ImsManager.setWfcMode(context, buttonMode);
                mButtonWfcMode.setSummary(getWfcModeSummary(context, buttonMode));
                MetricsLogger.action(getActivity(), getMetricsCategory(), buttonMode);
            }
        }
        return true;
    }

    static int getWfcModeSummary(Context context, int wfcMode) {
        int resId = com.android.internal.R.string.wifi_calling_off_summary;
        if (ImsManager.isWfcEnabledByUser(context)) {
            switch (wfcMode) {
                case ImsConfig.WfcModeFeatureValueConstants.WIFI_ONLY:
                    resId = com.android.internal.R.string.wfc_mode_wifi_only_summary;
                    break;
                case ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED:
                    resId = com.android.internal.R.string.wfc_mode_cellular_preferred_summary;
                    break;
                case ImsConfig.WfcModeFeatureValueConstants.WIFI_PREFERRED:
                    resId = com.android.internal.R.string.wfc_mode_wifi_preferred_summary;
                    break;
                default:
                    Log.e(TAG, "Unexpected WFC mode value: " + wfcMode);
            }
        }
        return resId;
    }

    /// M: @{
    /* Is IMS enabling or disabling */
    private boolean isInSwitchProcess() {
        int imsState = PhoneConstants.IMS_STATE_DISABLED;
        try {
         imsState = ImsManager.getInstance(getActivity(), SubscriptionManager
                .getDefaultVoiceSubId()).getImsState();
        } catch (ImsException e) {
           return false;
        }
        Log.d("@M_" + TAG, "isInSwitchProcess , imsState = " + imsState);
        return imsState == PhoneConstants.IMS_STATE_DISABLING
                || imsState == PhoneConstants.IMS_STATE_ENABLING;
    }
    /// @}

    private void updateScreen() {
        final SettingsActivity activity = (SettingsActivity) getActivity();
        if (activity == null) {
            return;
        }
        boolean isNonTtyOrTtyOnVolteEnabled = ImsManager
                .isNonTtyOrTtyOnVolteEnabled(activity);
        final SwitchBar switchBar = activity.getSwitchBar();
        boolean isWfcEnabled = switchBar.getSwitch().isChecked()
                && isNonTtyOrTtyOnVolteEnabled;
        boolean isCallStateIdle = !TelecomManager.from(activity).isInCall();
        Log.d(TAG, "isWfcEnabled: " + isWfcEnabled
                + ", isCallStateIdle: " + isCallStateIdle);
        switchBar.setEnabled(isCallStateIdle && isNonTtyOrTtyOnVolteEnabled);

        Preference pref = getPreferenceScreen().findPreference(BUTTON_WFC_MODE);
        if (pref != null) {
            pref.setEnabled(isWfcEnabled && isCallStateIdle);
        }
    }
}
