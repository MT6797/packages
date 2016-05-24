/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.telephony.CellBroadcastMessage;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.DefaultPhoneNotifier;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.settings.InstrumentedPreferenceActivity;
import com.android.settings.R;
import com.android.settings.Utils;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.cdma.CdmaSimStatus;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.sim.SimHotSwapHandler;
import com.mediatek.settings.sim.SimHotSwapHandler.OnSimHotSwapListener;

import android.view.View;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;

import java.util.ArrayList;
import java.util.List;


/**
 * Display the following information
 * # Phone Number
 * # Network
 * # Roaming
 * # Device Id (IMEI in GSM and MEID in CDMA)
 * # Network type
 * # Operator info (area info cell broadcast for Brazil)
 * # Signal Strength
 *
 */
public class SimStatus extends InstrumentedPreferenceActivity {
    private static final String TAG = "SimStatus";

    private static final String KEY_DATA_STATE = "data_state";
    private static final String KEY_SERVICE_STATE = "service_state";
    private static final String KEY_OPERATOR_NAME = "operator_name";
    private static final String KEY_ROAMING_STATE = "roaming_state";
    private static final String KEY_NETWORK_TYPE = "network_type";
    private static final String KEY_LATEST_AREA_INFO = "latest_area_info";
    private static final String KEY_PHONE_NUMBER = "number";
    private static final String KEY_SIGNAL_STRENGTH = "signal_strength";
    private static final String KEY_IMEI = "imei";
    private static final String KEY_IMEI_SV = "imei_sv";
    private static final String COUNTRY_ABBREVIATION_BRAZIL = "br";

    static final String CB_AREA_INFO_RECEIVED_ACTION =
            "android.cellbroadcastreceiver.CB_AREA_INFO_RECEIVED";

    static final String GET_LATEST_CB_AREA_INFO_ACTION =
            "android.cellbroadcastreceiver.GET_LATEST_CB_AREA_INFO";

    // Require the sender to have this permission to prevent third-party spoofing.
    static final String CB_AREA_INFO_SENDER_PERMISSION =
            "android.permission.RECEIVE_EMERGENCY_BROADCAST";


    private TelephonyManager mTelephonyManager;
    private Phone mPhone = null;
    private Resources mRes;
    private Preference mSignalStrength;
    private SubscriptionInfo mSir;
    private boolean mShowLatestAreaInfo;

    // Default summary for items
    private String mDefaultText;

    private TabHost mTabHost;
    private TabWidget mTabWidget;
    private ListView mListView;
    private List<SubscriptionInfo> mSelectableSubInfos;

    private PhoneStateListener mPhoneStateListener;
    private BroadcastReceiver mAreaInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (CB_AREA_INFO_RECEIVED_ACTION.equals(action)) {
                Bundle extras = intent.getExtras();
                if (extras == null) {
                    return;
                }
                CellBroadcastMessage cbMessage = (CellBroadcastMessage) extras.get("message");
                if (cbMessage != null
                        && cbMessage.getServiceCategory() == 50
                        && mSir.getSubscriptionId() == cbMessage.getSubId()) {
                    String latestAreaInfo = cbMessage.getMessageBody();
                    updateAreaInfo(latestAreaInfo);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        mSelectableSubInfos = SubscriptionManager.from(this).getActiveSubscriptionInfoList();

        addPreferencesFromResource(R.xml.device_info_sim_status);

        mRes = getResources();
        mDefaultText = mRes.getString(R.string.device_info_default);
        // Note - missing in zaku build, be careful later...
        mSignalStrength = findPreference(KEY_SIGNAL_STRENGTH);

        /// M: for [C2K SIM Status]
        mCdmaSimStatus = new CdmaSimStatus(this, null);

        if (mSelectableSubInfos == null) {
            mSir = null;
            /// M: for [C2K SIM Status]
            mCdmaSimStatus.setSubscriptionInfo(mSir);
        } else {
            mSir = mSelectableSubInfos.size() > 0 ? mSelectableSubInfos.get(0) : null;

            /// M: for [C2K SIM Status]
            mCdmaSimStatus.setSubscriptionInfo(mSir);

            if (mSelectableSubInfos.size() > 1) {
                setContentView(com.android.internal.R.layout.common_tab_settings);

                mTabHost = (TabHost) findViewById(android.R.id.tabhost);
                mTabWidget = (TabWidget) findViewById(android.R.id.tabs);
                mListView = (ListView) findViewById(android.R.id.list);

                mTabHost.setup();
                mTabHost.setOnTabChangedListener(mTabListener);
                mTabHost.clearAllTabs();

                for (int i = 0; i < mSelectableSubInfos.size(); i++) {
                    mTabHost.addTab(buildTabSpec(String.valueOf(i),
                            String.valueOf(mSelectableSubInfos.get(i).getDisplayName())));
                }
            }
        }
        updatePhoneInfos();

        /// M: for [SIM Hot Swap] @{
        mSimHotSwapHandler = new SimHotSwapHandler(getApplicationContext());
        mSimHotSwapHandler.registerOnSimHotSwap(new OnSimHotSwapListener() {
            @Override
            public void onSimHotSwap() {
                Log.d(TAG, "onSimHotSwap, finish Activity~~");
                finish();
            }
        });
        ///@}

        /// M: for plug-in
        customizeTitle();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DEVICEINFO_SIM_STATUS;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPhone != null) {
            updatePreference();

            updateSignalStrength(mPhone.getSignalStrength());
            updateServiceState(mPhone.getServiceState());
            updateDataState();
            mTelephonyManager.listen(mPhoneStateListener,
                    PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                    | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                    | PhoneStateListener.LISTEN_SERVICE_STATE);
            if (mShowLatestAreaInfo) {
                registerReceiver(mAreaInfoReceiver, new IntentFilter(CB_AREA_INFO_RECEIVED_ACTION),
                        CB_AREA_INFO_SENDER_PERMISSION, null);
                // Ask CellBroadcastReceiver to broadcast the latest area info received
                Intent getLatestIntent = new Intent(GET_LATEST_CB_AREA_INFO_ACTION);
                sendBroadcastAsUser(getLatestIntent, UserHandle.ALL,
                        CB_AREA_INFO_SENDER_PERMISSION);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mPhone != null) {
            mTelephonyManager.listen(mPhoneStateListener,
                    PhoneStateListener.LISTEN_NONE);
        }
        if (mShowLatestAreaInfo) {
            unregisterReceiver(mAreaInfoReceiver);
        }
    }

    /**
     * Removes the specified preference, if it exists.
     * @param key the key for the Preference item
     */
    private void removePreferenceFromScreen(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private void setSummaryText(String key, String text) {
        if (TextUtils.isEmpty(text)) {
            text = mDefaultText;
        }
        // some preferences may be missing
        final Preference preference = findPreference(key);
        if (preference != null) {
            preference.setSummary(text);
        }
    }

    private void updateNetworkType() {
        // Whether EDGE, UMTS, etc...
        String networktype = null;
        final int subId = mSir.getSubscriptionId();
        final int actualDataNetworkType = mTelephonyManager.getDataNetworkType(
                mSir.getSubscriptionId());
        final int actualVoiceNetworkType = mTelephonyManager.getVoiceNetworkType(
                mSir.getSubscriptionId());
        Log.d(TAG, "updateNetworkType(), actualDataNetworkType = " + actualDataNetworkType
                + "actualVoiceNetworkType = " + actualVoiceNetworkType);
        if (TelephonyManager.NETWORK_TYPE_UNKNOWN != actualDataNetworkType) {
            networktype = mTelephonyManager.getNetworkTypeName(actualDataNetworkType);
        } else if (TelephonyManager.NETWORK_TYPE_UNKNOWN != actualVoiceNetworkType) {
            networktype = mTelephonyManager.getNetworkTypeName(actualVoiceNetworkType);
        }

        boolean show4GForLTE = false;
        try {
            Context con = createPackageContext("com.android.systemui", 0);
            int id = con.getResources().getIdentifier("config_show4GForLTE",
                    "bool", "com.android.systemui");
            show4GForLTE = con.getResources().getBoolean(id);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "NameNotFoundException for show4GFotLTE");
        }

        if (networktype != null && networktype.equals("LTE") && show4GForLTE) {
            networktype = "4G";
        }
        setSummaryText(KEY_NETWORK_TYPE, networktype);
        /// M: for [C2K SIM Status]
        mCdmaSimStatus.updateNetworkType(KEY_NETWORK_TYPE, networktype);
    }

    private void updateDataState() {
        final int state =
                DefaultPhoneNotifier.convertDataState(mPhone.getDataConnectionState());

        String display = mRes.getString(R.string.radioInfo_unknown);

        switch (state) {
            case TelephonyManager.DATA_CONNECTED:
                display = mRes.getString(R.string.radioInfo_data_connected);
                break;
            case TelephonyManager.DATA_SUSPENDED:
                display = mRes.getString(R.string.radioInfo_data_suspended);
                break;
            case TelephonyManager.DATA_CONNECTING:
                display = mRes.getString(R.string.radioInfo_data_connecting);
                break;
            case TelephonyManager.DATA_DISCONNECTED:
                display = mRes.getString(R.string.radioInfo_data_disconnected);
                break;
        }

        setSummaryText(KEY_DATA_STATE, display);
    }

    private void updateServiceState(ServiceState serviceState) {
        final int state = serviceState.getState();
        String display = mRes.getString(R.string.radioInfo_unknown);

        switch (state) {
            case ServiceState.STATE_IN_SERVICE:
                display = mRes.getString(R.string.radioInfo_service_in);
                break;
            case ServiceState.STATE_OUT_OF_SERVICE:
                // Set signal strength to 0 when service state is STATE_OUT_OF_SERVICE
                mSignalStrength.setSummary("0");
            case ServiceState.STATE_EMERGENCY_ONLY:
                // Set summary string of service state to radioInfo_service_out when
                // service state is both STATE_OUT_OF_SERVICE & STATE_EMERGENCY_ONLY
                display = mRes.getString(R.string.radioInfo_service_out);
                break;
            case ServiceState.STATE_POWER_OFF:
                display = mRes.getString(R.string.radioInfo_service_off);
                // Also set signal strength to 0
                mSignalStrength.setSummary("0");
                break;
        }

        setSummaryText(KEY_SERVICE_STATE, display);

        if (serviceState.getRoaming()) {
            setSummaryText(KEY_ROAMING_STATE, mRes.getString(R.string.radioInfo_roaming_in));
        } else {
            setSummaryText(KEY_ROAMING_STATE, mRes.getString(R.string.radioInfo_roaming_not));
        }
        setSummaryText(KEY_OPERATOR_NAME, serviceState.getOperatorAlphaLong());
        /// M: for [C2K SIM Status]
        mCdmaSimStatus.setServiceState(serviceState);
    }

    private void updateAreaInfo(String areaInfo) {
        if (areaInfo != null) {
            setSummaryText(KEY_LATEST_AREA_INFO, areaInfo);
        }
    }

    void updateSignalStrength(SignalStrength signalStrength) {
        if (mSignalStrength != null) {
            final int state = mPhone.getServiceState().getState();
            Resources r = getResources();

            if ((ServiceState.STATE_OUT_OF_SERVICE == state) ||
                    (ServiceState.STATE_POWER_OFF == state)) {
                mSignalStrength.setSummary("0");
                return;
            }

            int signalDbm = signalStrength.getDbm();
            int signalAsu = signalStrength.getAsuLevel();

            if (-1 == signalDbm) {
                signalDbm = 0;
            }

            if (-1 == signalAsu) {
                signalAsu = 0;
            }

            Log.d(TAG, "updateSignalStrength(), signalDbm = " + signalDbm + " signalAsu = "
                    + signalAsu);
            mSignalStrength.setSummary(r.getString(R.string.sim_signal_strength,
                        signalDbm, signalAsu));
            /// M: for [C2K SIM Status]
            mCdmaSimStatus.updateSignalStrength(signalStrength, mSignalStrength);
        }
    }

    private void updatePreference() {
        if (mPhone.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) {
            // only show area info when SIM country is Brazil
            if (COUNTRY_ABBREVIATION_BRAZIL.equals(mTelephonyManager.getSimCountryIso(
                            mSir.getSubscriptionId()))) {
                mShowLatestAreaInfo = true;
            }
        }

        String rawNumber = mTelephonyManager.getLine1NumberForSubscriber(mSir.getSubscriptionId());
        String formattedNumber = null;
        if (!TextUtils.isEmpty(rawNumber)) {
            formattedNumber = PhoneNumberUtils.formatNumber(rawNumber);
        }
        // If formattedNumber is null or empty, it'll display as "Unknown".
        setSummaryText(KEY_PHONE_NUMBER, formattedNumber);
        /// M: for ALPS02424703, should use MEID for CDMAPhone
        // TODO: also should display title as "MEID" instead of "IMEI" for CDMAPhone
        String deviceId = (mPhone.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) ? mPhone
                .getImei() : mPhone.getMeid();
        setSummaryText(KEY_IMEI, deviceId);
        setSummaryText(KEY_IMEI_SV, mPhone.getDeviceSvn());
        /// M: For ALPS02430910. the MEID replace IMEI for CDMA. @{
        final Preference preference = findPreference(KEY_IMEI);
        if (preference != null) {
            int titleResId = mPhone.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA
                    ? R.string.status_meid_number
                    : R.string.status_imei;
            preference.setTitle(titleResId);
        }
        /// @}
        if (!mShowLatestAreaInfo) {
            removePreferenceFromScreen(KEY_LATEST_AREA_INFO);
        }
        /// M: for [C2K SIM Status]
        mCdmaSimStatus.updateCdmaPreference(this, mSir);
    }

    private void updatePhoneInfos() {
        if (mSir != null) {
            final Phone phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(
                        mSir.getSubscriptionId()));
            if (UserHandle.myUserId() == UserHandle.USER_OWNER
                    && SubscriptionManager.isValidSubscriptionId(mSir.getSubscriptionId())) {
                if (phone == null) {
                    Log.e(TAG, "Unable to locate a phone object for the given Subscription ID.");
                    return;
                }

                mPhone = phone;
                /// M: for [C2K SIM Status] @{
                mCdmaSimStatus.setPhoneInfos(mPhone);
                /// @}

                /// M: Google issue, when tabchange, the old one will still active, and if
                // current tab is sim2, sim 1's phone state listener will also change sim 2
                // preference with sim 1 property.
                // So when ever new the listener, firstly remove from telephony manager register.@{
                if (mPhoneStateListener != null) {
                    Log.d(TAG, "remove the phone state listener mPhoneStateListener = "
                            + mPhoneStateListener);
                    mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
                }
                /// @}

                mPhoneStateListener = new PhoneStateListener(mSir.getSubscriptionId()) {
                    @Override
                    public void onDataConnectionStateChanged(int state) {
                        updateDataState();
                        updateNetworkType();
                    }

                    @Override
                    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                        updateSignalStrength(signalStrength);
                    }

                    @Override
                    public void onServiceStateChanged(ServiceState serviceState) {
                        Log.d(TAG,"onServiceStateChanged subId = " + mSir.getSubscriptionId());
                        updateServiceState(serviceState);
                    }
                };
            }
        }
    }
    private OnTabChangeListener mTabListener = new OnTabChangeListener() {
        @Override
        public void onTabChanged(String tabId) {
            final int slotId = Integer.parseInt(tabId);
            mSir = mSelectableSubInfos.get(slotId);
            /// M: for [C2K SIM Status]
            mCdmaSimStatus.setSubscriptionInfo(mSir);

            // The User has changed tab; update the SIM information.
            updatePhoneInfos();
            mTelephonyManager.listen(mPhoneStateListener,
                    PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                    | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                    | PhoneStateListener.LISTEN_SERVICE_STATE);
            updateDataState();
            updateNetworkType();
            updatePreference();
        }
    };

    private TabContentFactory mEmptyTabContent = new TabContentFactory() {
        @Override
        public View createTabContent(String tag) {
            return new View(mTabHost.getContext());
        }
    };

    private TabSpec buildTabSpec(String tag, String title) {
        return mTabHost.newTabSpec(tag).setIndicator(title).setContent(
                mEmptyTabContent);
    }

    ///----------------------------------------MTK------------------------------------------------
    /// M: for [SIM Hot Swap]
    private SimHotSwapHandler mSimHotSwapHandler;
    private CdmaSimStatus mCdmaSimStatus;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /// M: for [SIM Hot Swap]
        mSimHotSwapHandler.unregisterOnSimHotSwap();
    };

    /**
     * only for plug-in
     */
    private void customizeTitle() {
        String title = getTitle().toString();
        Log.d(TAG, "title = " + title);
        if (title.equals(getString(R.string.sim_status_title))) {
            ISettingsMiscExt ext = UtilsExt.getMiscPlugin(this);
            title = ext.customizeSimDisplayString(getTitle().toString(),
                            SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            Log.d(TAG, "title = " + title);
            setTitle(title);
        }
    }
}
