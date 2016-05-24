/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.mediatek.wifi.hotspot;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.preference.SwitchPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.util.Log;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settingslib.TetherUtil;
import com.android.settings.TetherService;
import com.android.settings.widget.SwitchBar;
import com.mediatek.settings.ext.IWfcSettingsExt;
import com.mediatek.settings.ext.IWifiExt;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.TetherSettingsExt;
import com.mediatek.settings.UtilsExt;

import java.util.ArrayList;

public class TetherWifiApEnabler extends Fragment
        implements SwitchBar.OnSwitchChangeListener {
    static final String TAG = "TetherWifiApEnabler";
    private Context mContext;
    private SwitchPreference mSwitch;
    private CharSequence mOriginalSummary;

    private WifiManager mWifiManager;
    private IntentFilter mIntentFilter;
    private TetherSettingsExt mTetherSettingsEx;
    private static final int WIFI_IPV4 = 0x0f;
    private static final int WIFI_IPV6 = 0xf0;

    ConnectivityManager mCm;
    private String[] mWifiRegexs;

    /// M: @{
    private static final String WIFI_SWITCH_SETTINGS = "wifi_tether_settings";
    private static final int INVALID             = -1;
    private static final int WIFI_TETHERING      = 0;
    IWifiExt mExt;
    IWfcSettingsExt mWfcSettingsExt;
    private SwitchBar mSwitchBar;
    private boolean mStateMachineEvent;

    private int mTetherChoice = INVALID;
    /* Stores the package name and the class name of the provisioning app */
    private String[] mProvisionApp;
    private static final int PROVISION_REQUEST = 0;
    private static final String ACTION_WIFI_TETHERED_SWITCH = "action.wifi.tethered_switch";
    /// @}

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(action)) {
                handleWifiApStateChanged(intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_AP_STATE, WifiManager.WIFI_AP_STATE_FAILED));
            } else if (ConnectivityManager.ACTION_TETHER_STATE_CHANGED.equals(action)) {
                ArrayList<String> available = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_AVAILABLE_TETHER);
                ArrayList<String> active = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ACTIVE_TETHER);
                ArrayList<String> errored = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ERRORED_TETHER);
                if (available != null && active != null && errored != null) {
                    if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
                        updateTetherStateForIpv6(available.toArray(),
                                active.toArray(), errored.toArray());
                    } else {
                        updateTetherState(available.toArray(), active.toArray(), errored.toArray());
                    }
                }
            } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                enableWifiSwitch();
            }
        }
    };

    /** M: fix not commit WifiApEnabler this fragment,
     * can't use getActivity() or onActivityResult
     */
    public TetherWifiApEnabler() {
        ///M: WFC  @ {
        mWfcSettingsExt = UtilsExt.getWfcSettingsPlugin(mContext);
        /// @}
    }

    public TetherWifiApEnabler(SwitchBar switchBar, Context context) {
        mContext = context;
        mSwitchBar = switchBar;
        setupSwitchBar();
        init(context);
        /** M: fix not commit WifiApEnabler this fragment,
         * can't use getActivity() or onActivityResult
         */
        commitFragment();
        ///M: WFC  @ {
        mWfcSettingsExt = UtilsExt.getWfcSettingsPlugin(mContext);
        /// @}
    }

    public void setupSwitchBar() {
        mSwitchBar.addOnSwitchChangeListener(this);
        mSwitchBar.show();
    }

    public void teardownSwitchBar() {
        mSwitchBar.removeOnSwitchChangeListener(this);
        mSwitchBar.hide();
    }

    public void init(Context context) {
        /// M: WifiManager memory leak @{
        //mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mWifiManager = (WifiManager) context.getApplicationContext().
                getSystemService(Context.WIFI_SERVICE);
        /// @}
        mCm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        mWifiRegexs = mCm.getTetherableWifiRegexs();

        mIntentFilter = new IntentFilter(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);

        mProvisionApp = mContext.getResources().getStringArray(
                com.android.internal.R.array.config_mobile_hotspot_provision_app);
    }

    public void resume() {
        mContext.registerReceiver(mReceiver, mIntentFilter);
        enableWifiSwitch();
    }

    public void pause() {
        mContext.unregisterReceiver(mReceiver);
    }

    private void enableWifiSwitch() {
        boolean isAirplaneMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        if (!isAirplaneMode) {
            setSwitchEnabled(true);
        } else {
            if (mSwitchBar == null) {
                mSwitch.setSummary(mOriginalSummary);
            }
            setSwitchEnabled(false);
        }
    }

    public void setSoftapEnabled(boolean enable) {
        if (TetherUtil.setWifiTethering(enable, mContext)) {
            /* Disable here, enabled on receiving success broadcast */
            //mSwitch.setEnabled(false);
            setSwitchEnabled(false);
        } else {
            mSwitch.setSummary(R.string.wifi_error);
        }
    }

    public void updateConfigSummary(WifiConfiguration wifiConfig) {
        String s = com.mediatek.custom.CustomProperties.getString(
                    com.mediatek.custom.CustomProperties.MODULE_WLAN,
                    com.mediatek.custom.CustomProperties.SSID,
                    mContext.getString(
                        com.android.internal.R.string.wifi_tether_configure_ssid_default));
        if (mSwitchBar == null) {
            mSwitch.setSummary(String.format(mContext.getString(
                R.string.wifi_tether_enabled_subtext),
                    (wifiConfig == null) ? s : wifiConfig.SSID));
        }
    }

    private void updateTetherStateForIpv6(Object[] available, Object[] tethered, Object[] errored) {
        boolean wifiTethered = false;
        boolean wifiErrored = false;

        int wifiErrorIpv4 = ConnectivityManager.TETHER_ERROR_NO_ERROR;
        int wifiErrorIpv6 = ConnectivityManager.TETHER_ERROR_IPV6_NO_ERROR;
        for (Object o : available) {
            String s = (String) o;
            for (String regex : mWifiRegexs) {
                if (s.matches(regex)) {
                    if (wifiErrorIpv4 == ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                        wifiErrorIpv4 = (mCm.getLastTetherError(s) & WIFI_IPV4);
                    }
                    if (wifiErrorIpv6 == ConnectivityManager.TETHER_ERROR_IPV6_NO_ERROR) {
                        wifiErrorIpv6 = (mCm.getLastTetherError(s) & WIFI_IPV6);
                    }
                }
            }
        }

        for (Object o : tethered) {
            String s = (String) o;
            for (String regex : mWifiRegexs) {
                if (s.matches(regex)) {
                    wifiTethered = true;
                    if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
                        if (wifiErrorIpv6 == ConnectivityManager.TETHER_ERROR_IPV6_NO_ERROR) {
                            wifiErrorIpv6 = (mCm.getLastTetherError(s) & WIFI_IPV6);
                        }
                    }
                }
            }
        }

        for (Object o: errored) {
            String s = (String) o;
            for (String regex : mWifiRegexs) {
                if (s.matches(regex)) {
                    wifiErrored = true;
                }
            }
        }

        if (wifiTethered) {
            WifiConfiguration wifiConfig = mWifiManager.getWifiApConfiguration();
            updateConfigSummary(wifiConfig);
            String s = mContext.getString(
                    com.android.internal.R.string.wifi_tether_configure_ssid_default);
            String tetheringActive = String.format(
                mContext.getString(R.string.wifi_tether_enabled_subtext),
                (wifiConfig == null) ? s : wifiConfig.SSID);

            if (mTetherSettingsEx != null && mSwitchBar == null) {
                mSwitch.setSummary(tetheringActive +
                        mTetherSettingsEx.getIPV6String(wifiErrorIpv4, wifiErrorIpv6));
            }
        } else if (wifiErrored) {
            if (mSwitchBar == null) {
                mSwitch.setSummary(R.string.wifi_error);
            }
        }
    }


    /**
     * set the TetherSettings.
     * @param TetherSettings
     * @return void.
     */
    public void setTetherSettings(TetherSettingsExt tetherSettingsEx) {
        mTetherSettingsEx = tetherSettingsEx;
    }

    private void updateTetherState(Object[] available, Object[] tethered, Object[] errored) {
        boolean wifiTethered = false;
        boolean wifiErrored = false;

        for (Object o : tethered) {
            String s = (String)o;
            for (String regex : mWifiRegexs) {
                if (s.matches(regex)) wifiTethered = true;
            }
        }
        for (Object o: errored) {
            String s = (String)o;
            for (String regex : mWifiRegexs) {
                if (s.matches(regex)) wifiErrored = true;
            }
        }

        if (wifiTethered) {
            WifiConfiguration wifiConfig = mWifiManager.getWifiApConfiguration();
            updateConfigSummary(wifiConfig);
        } else if (wifiErrored) {
            if (mSwitchBar == null) {
                mSwitch.setSummary(R.string.wifi_error);
            }
        }
    }

    private void handleWifiApStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_AP_STATE_ENABLING:
                 setSwitchEnabled(false);
                 setStartTime(false);
                 if (mSwitchBar == null) {
                     mSwitch.setSummary(R.string.wifi_tether_starting);
                 }
                 break;
            case WifiManager.WIFI_AP_STATE_ENABLED:
                 /**
                  * Summary on enable is handled by tether
                  * broadcast notice
                  */
                 long eableEndTime = System.currentTimeMillis();
                 Log.i("WifiHotspotPerformanceTest",
                     "[Performance test][Settings][wifi hotspot]" +
                     " wifi hotspot turn on end [" + eableEndTime + "]");
                 setSwitchChecked(true);
                 setSwitchEnabled(true);
                 setStartTime(true);
                 break;
            case WifiManager.WIFI_AP_STATE_DISABLING:
                 setSwitchChecked(false);
                 setSwitchEnabled(false);
                 if (mSwitchBar == null) {
                     Log.d(TAG, "wifi_stopping");
                     mSwitch.setSummary(R.string.wifi_tether_stopping);
                 }
                 break;
            case WifiManager.WIFI_AP_STATE_DISABLED:
                 long disableEndTime = System.currentTimeMillis();
                 Log.i("WifiHotspotPerformanceTest",
                     "[Performance test][Settings][wifi hotspot]" +
                     " wifi hotspot turn off end [" + disableEndTime + "]");
                 setSwitchChecked(false);
                 setSwitchEnabled(true);
                 if (mSwitchBar == null) {
                     mSwitch.setSummary(mOriginalSummary);
                 }
                 enableWifiSwitch();
                 break;
            default:
                 enableWifiSwitch();
                 break;
        }
    }
    private void setSwitchChecked(boolean checked) {
        mStateMachineEvent = true;
        if (mSwitchBar != null) {
            mSwitchBar.setChecked(checked);
        }
        sendBroadcast(); // M: ALPS01831234
        Log.d(TAG, "setSwitchChecked checked = " + checked);
        mStateMachineEvent = false;
    }
    private void setSwitchEnabled(boolean enabled) {
        mStateMachineEvent = true;
        if (mSwitchBar != null) {
            mSwitchBar.setEnabled(enabled);
        }
        mStateMachineEvent = false;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        sendBroadcast(); // M: ALPS01831234
        //Do nothing if called as a result of a state machine event
        if (mStateMachineEvent) {
            return;
        }
        Log.d(TAG, "onSwitchChanged, hotspot switch isChecked:" + isChecked);
        if (isChecked) {
            startProvisioningIfNecessary(WIFI_TETHERING);
        } else {
            setSoftapEnabled(false);
        }
    }

    boolean isProvisioningNeeded() {
        return mProvisionApp != null ?
            mProvisionApp.length == 2 : false;
    }

    private void startProvisioningIfNecessary(int choice) {
        mTetherChoice = choice;
        if (isProvisioningNeeded()) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(mProvisionApp[0], mProvisionApp[1]);
            startActivityForResult(intent, PROVISION_REQUEST);
            Log.d(TAG,
                "startProvisioningIfNecessary, startActivityForResult");
        } else {
            startTethering();
        }
    }

    public void onActivityResult(int requestCode,
            int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == PROVISION_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                startTethering();
            }
        }
    }

    private void startTethering() {
        if (mTetherChoice == WIFI_TETHERING) {
            Log.d(TAG, "startTethering, setSoftapEnabled");
            //M:WFC : PLugin to show alert on enabling hotpspot
            if (!mWfcSettingsExt.showWfcTetheringAlertDialog(mContext)) {
                Log.d(TAG, "startTethering, setSoftapEnabled continued");
                setSoftapEnabled(true);
            }
        }
    }

    private void setStartTime(boolean enable) {
        long startTime = Settings.System.getLong(
                mContext.getContentResolver(),
                Settings.System.WIFI_HOTSPOT_START_TIME,
                Settings.System.WIFI_HOTSPOT_DEFAULT_START_TIME);
        if (enable) {
            if (startTime == Settings.System.WIFI_HOTSPOT_DEFAULT_START_TIME) {
                Settings.System.putLong(mContext.getContentResolver(),
                        Settings.System.WIFI_HOTSPOT_START_TIME,
                        System.currentTimeMillis());
                Log.d(TAG, "enable value: " + System.currentTimeMillis());
            }
        } else {
            long newValue = Settings.System.WIFI_HOTSPOT_DEFAULT_START_TIME;
            Log.d(TAG, "disable value: " + newValue);
            Settings.System.putLong(mContext.getContentResolver(),
                    Settings.System.WIFI_HOTSPOT_START_TIME, newValue);
        }
    }

    /**
     * M: fix not commit WifiApEnabler this fragment, can't use getActivity() or onActivityResult
     */
    private void commitFragment() {
        if (mContext != null) {
            final FragmentTransaction ft = ((Activity) mContext).
                getFragmentManager().beginTransaction();
            ft.add(this, TAG);
            ft.commitAllowingStateLoss();
        }

    }

    /* M: send broadcast to tell the action:  Wifi tethered switch changed
     * ALPS01831234: IPV6 Preference state is not right
     * **/
    private void sendBroadcast() {
        Intent wifiTetherIntent = new Intent(ACTION_WIFI_TETHERED_SWITCH);
        mContext.sendBroadcast(wifiTetherIntent);
    }
}
