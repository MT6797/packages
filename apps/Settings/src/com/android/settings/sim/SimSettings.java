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

package com.android.settings.sim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.R;

import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.cdma.CdmaUtils;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.ext.ISimManagementExt;
import com.mediatek.settings.sim.PhoneServiceStateHandler;
import com.mediatek.settings.sim.RadioPowerPreference;
import com.mediatek.settings.sim.SimHotSwapHandler;
import com.mediatek.settings.sim.TelephonyUtils;
import com.mediatek.settings.sim.SimHotSwapHandler.OnSimHotSwapListener;
import com.mediatek.telecom.TelecomManagerEx;

import java.util.ArrayList;
import java.util.List;

public class SimSettings extends RestrictedSettingsFragment implements Indexable,
        PhoneServiceStateHandler.Listener {
    private static final String TAG = "SimSettings";
    private static final boolean DBG = true;

    private static final String DISALLOW_CONFIG_SIM = "no_config_sim";
    private static final String SIM_CARD_CATEGORY = "sim_cards";
    private static final String KEY_CELLULAR_DATA = "sim_cellular_data";
    private static final String KEY_CALLS = "sim_calls";
    private static final String KEY_SMS = "sim_sms";
    public static final String EXTRA_SLOT_ID = "slot_id";

    /**
     * By UX design we use only one Subscription Information(SubInfo) record per SIM slot.
     * mAvalableSubInfos is the list of SubInfos we present to the user.
     * mSubInfoList is the list of all SubInfos.
     * mSelectableSubInfos is the list of SubInfos that a user can select for data, calls, and SMS.
     */
    private List<SubscriptionInfo> mAvailableSubInfos = null;
    private List<SubscriptionInfo> mSubInfoList = null;
    private List<SubscriptionInfo> mSelectableSubInfos = null;
    private PreferenceScreen mSimCards = null;
    private SubscriptionManager mSubscriptionManager;
    private int mNumSlots;
    private Context mContext;

    public SimSettings() {
        super(DISALLOW_CONFIG_SIM);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.SIM;
    }

    @Override
    public void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        mContext = getActivity();

        mSubscriptionManager = SubscriptionManager.from(getActivity());
        final TelephonyManager tm =
                (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        addPreferencesFromResource(R.xml.sim_settings);

        mNumSlots = tm.getSimCount();
        mSimCards = (PreferenceScreen)findPreference(SIM_CARD_CATEGORY);
        mAvailableSubInfos = new ArrayList<SubscriptionInfo>(mNumSlots);
        mSelectableSubInfos = new ArrayList<SubscriptionInfo>();
        SimSelectNotification.cancelNotification(getActivity());

        /// M: for [SIM Hot Swap], [SIM Radio On/Off] etc.
        initForSimStateChange();

        /// M: for Plug-in @{
        mSimManagementExt = UtilsExt.getSimManagmentExtPlugin(getActivity());
        mMiscExt = UtilsExt.getMiscPlugin(getActivity());
        /// @}
    }

    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener
            = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            if (DBG) log("onSubscriptionsChanged:");
            updateSubscriptions();
        }
    };

    private void updateSubscriptions() {
        mSubInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        for (int i = 0; i < mNumSlots; ++i) {
            Preference pref = mSimCards.findPreference("sim" + i);
            if (pref instanceof SimPreference) {
                mSimCards.removePreference(pref);
            }
        }
        mAvailableSubInfos.clear();
        mSelectableSubInfos.clear();

        for (int i = 0; i < mNumSlots; ++i) {
            final SubscriptionInfo sir = mSubscriptionManager
                    .getActiveSubscriptionInfoForSimSlotIndex(i);
            SimPreference simPreference = new SimPreference(mContext, sir, i);
            simPreference.setOrder(i-mNumSlots);
            /// M: for [SIM Radio On/Off]
            simPreference
                    .bindRadioPowerState(sir == null ? SubscriptionManager.INVALID_SUBSCRIPTION_ID
                            : sir.getSubscriptionId());
            mSimCards.addPreference(simPreference);
            mAvailableSubInfos.add(sir);
            if (sir != null) {
                mSelectableSubInfos.add(sir);
            }
        }
        updateAllOptions();
    }

    private void updateAllOptions() {
        updateSimSlotValues();
        updateActivitesCategory();
    }

    private void updateSimSlotValues() {
        final int prefSize = mSimCards.getPreferenceCount();
        for (int i = 0; i < prefSize; ++i) {
            Preference pref = mSimCards.getPreference(i);
            if (pref instanceof SimPreference) {
                ((SimPreference)pref).update();
            }
        }
    }

    private void updateActivitesCategory() {
        updateCellularDataValues();
        updateCallValues();
        updateSmsValues();
        updateSimPref();
    }

    private void updateSmsValues() {
        final Preference simPref = findPreference(KEY_SMS);
        if (simPref != null) {
            SubscriptionInfo sir = mSubscriptionManager.getDefaultSmsSubscriptionInfo();
            simPref.setTitle(R.string.sms_messages_title);
            if (DBG) log("[updateSmsValues] mSubInfoList=" + mSubInfoList);

            /// M: for plug-in
            sir = mSimManagementExt.setDefaultSubId(getActivity(), sir, KEY_SMS);

            if (sir != null) {
                simPref.setSummary(sir.getDisplayName());
            } else if (sir == null) {
                /// M: for [Always Ask]
                // simPref.setSummary(R.string.sim_selection_required_pref);
                simPref.setSummary(R.string.sim_calls_ask_first_prefs_title);
                /// M: for plug-in
                mSimManagementExt.updateDefaultSmsSummary(simPref);
            }

            /// M: [C2K solution 2 enhancement] [C2K solution 1.5] @{
            if (!shouldDisableActivitesCategory(getActivity())) {
            /// @}
                simPref.setEnabled(mSelectableSubInfos.size() >= 1);
                /// M: for plug-in
               mSimManagementExt.configSimPreferenceScreen(simPref, KEY_SMS,
                       mSelectableSubInfos.size());
            }
        }
    }

    private void updateCellularDataValues() {
        final Preference simPref = findPreference(KEY_CELLULAR_DATA);
        if (simPref != null) {
            SubscriptionInfo sir = mSubscriptionManager.getDefaultDataSubscriptionInfo();
            simPref.setTitle(R.string.cellular_data_title);
            if (DBG) log("[updateCellularDataValues] mSubInfoList=" + mSubInfoList);

            /// M: for plug-in
            sir = mSimManagementExt.setDefaultSubId(getActivity(), sir, KEY_CELLULAR_DATA);

            if (sir != null) {
                simPref.setSummary(sir.getDisplayName());
            } else if (sir == null) {
                simPref.setSummary(R.string.sim_selection_required_pref);
            }
            /// M: [SIM Capability Switch] to prevent continuous click when SIM switch @{
            // simPref.setEnabled(mSelectableSubInfos.size() >= 1);
            simPref.setEnabled(isDataPrefEnable());
            mSimManagementExt.configSimPreferenceScreen(simPref, KEY_CELLULAR_DATA, -1);
            /// @}
        }
    }

    private void updateCallValues() {
        final Preference simPref = findPreference(KEY_CALLS);
        if (simPref != null) {
            final TelecomManager telecomManager = TelecomManager.from(mContext);
            PhoneAccountHandle phoneAccount =
                telecomManager.getUserSelectedOutgoingPhoneAccount();
            final List<PhoneAccountHandle> allPhoneAccounts =
                telecomManager.getCallCapablePhoneAccounts();

            phoneAccount = mSimManagementExt.setDefaultCallValue(phoneAccount);

            simPref.setTitle(R.string.calls_title);
            /// M: for ALPS02320747 @{
            // phoneaccount may got unregistered, need to check null here
            /*
            simPref.setSummary(phoneAccount == null
                    ? mContext.getResources().getString(R.string.sim_calls_ask_first_prefs_title)
                    : (String)telecomManager.getPhoneAccount(phoneAccount).getLabel());
             */
            PhoneAccount defaultAccount = phoneAccount == null ? null : telecomManager
                    .getPhoneAccount(phoneAccount);
            simPref.setSummary(defaultAccount == null
                    ? mContext.getResources().getString(R.string.sim_calls_ask_first_prefs_title)
                    : (String)defaultAccount.getLabel());
            /// @}
            /// M: [C2K solution 2 enhancement] [C2K solution 1.5] @{
            if (!shouldDisableActivitesCategory(getActivity())) {
                simPref.setEnabled(allPhoneAccounts.size() > 1);
                mSimManagementExt.configSimPreferenceScreen(simPref, KEY_CALLS,
                        allPhoneAccounts.size());
            }
            /// @}
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        /// M: fix Google bug: only listen to default sub, listen to Phone state change instead @{
        /*
        final TelephonyManager tm =
                (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        */
        /// @}

        /// M: for [Tablet]
        removeItemsForTablet();

        updateSubscriptions();

        /// M: for Plug-in @{
        customizeSimDisplay();
        mSimManagementExt.onResume(getActivity());
        /// @}

    }

    @Override
    public void onPause() {
        super.onPause();
        mSubscriptionManager.removeOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        /// M: Google bug: only listen to default sub, listen to Phone state change instead @{
        /*
        final TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        */
        ///@}

        /// M: for Plug-in
        mSimManagementExt.onPause();
    }

    /// M: Google bug: only listen to default sub, listen to Phone state change instead @{
    /*
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        // Disable Sim selection for Data when voice call is going on as changing the default data
        // sim causes a modem reset currently and call gets disconnected
        // ToDo : Add subtext on disabled preference to let user know that default data sim cannot
        // be changed while call is going on
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (DBG) log("PhoneStateListener.onCallStateChanged: state=" + state);
             final Preference pref = findPreference(KEY_CELLULAR_DATA);
            if (pref != null) {
                final boolean ecbMode = SystemProperties.getBoolean(
                        TelephonyProperties.PROPERTY_INECM_MODE, false);
                pref.setEnabled((state == TelephonyManager.CALL_STATE_IDLE) && !ecbMode);
                mSimManagementExt.configSimPreferenceScreen(pref,
                        KEY_CELLULAR_DATA, -1);
            }
        }
    };
    */
    /// @}

    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen,
            final Preference preference) {
        final Context context = mContext;
        Intent intent = new Intent(context, SimDialogActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

        if (preference instanceof SimPreference) {
            Intent newIntent = new Intent(context, SimPreferenceDialog.class);
            newIntent.putExtra(EXTRA_SLOT_ID, ((SimPreference)preference).getSlotId());
            startActivity(newIntent);
        } else if (findPreference(KEY_CELLULAR_DATA) == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.DATA_PICK);
            context.startActivity(intent);
        } else if (findPreference(KEY_CALLS) == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.CALLS_PICK);
            context.startActivity(intent);
        } else if (findPreference(KEY_SMS) == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.SMS_PICK);
            context.startActivity(intent);
        }

        return true;
    }
    /// M: for [SIM Radio On/Off]
    // private class SimPreference extends Preference {
    private class SimPreference extends RadioPowerPreference{
        private SubscriptionInfo mSubInfoRecord;
        private int mSlotId;
        Context mContext;

        public SimPreference(Context context, SubscriptionInfo subInfoRecord, int slotId) {
            super(context);

            mContext = context;
            mSubInfoRecord = subInfoRecord;
            mSlotId = slotId;
            setKey("sim" + mSlotId);
            update();
        }

        public void update() {
            final Resources res = mContext.getResources();

            setTitle(String.format(mContext.getResources()
                    .getString(R.string.sim_editor_title), (mSlotId + 1)));

            /// M: for Plug-in
            customizePreferenceTitle();

            if (mSubInfoRecord != null) {
                if (TextUtils.isEmpty(getPhoneNumber(mSubInfoRecord))) {
                    setSummary(mSubInfoRecord.getDisplayName());
                } else {
                    setSummary(mSubInfoRecord.getDisplayName() + " - " +
                            getPhoneNumber(mSubInfoRecord));
                    setEnabled(true);
                }
                setIcon(new BitmapDrawable(res, (mSubInfoRecord.createIconBitmap(mContext))));
                /// M: add for radio on/off @{
                setRadioEnabled(!mIsAirplaneModeOn
                        && isRadioSwitchComplete(mSubInfoRecord.getSubscriptionId()));
                setRadioOn(TelephonyUtils.isRadioOn(mSubInfoRecord.getSubscriptionId(),
                        getContext()));
                /// @}
            } else {
                setSummary(R.string.sim_slot_empty);
                setFragment(null);
                setEnabled(false);
            }
        }

        private int getSlotId() {
            return mSlotId;
        }

        /**
         * only for plug-in, change "SIM" to "UIM/SIM".
         */
        private void customizePreferenceTitle() {
            int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            if (mSubInfoRecord != null) {
                subId = mSubInfoRecord.getSubscriptionId();
            }
            setTitle(String.format(mMiscExt.customizeSimDisplayString(mContext.getResources()
                    .getString(R.string.sim_editor_title), subId), (mSlotId + 1)));
        }
    }

    // Returns the line1Number. Line1number should always be read from TelephonyManager since it can
    // be overridden for display purposes.
    private String getPhoneNumber(SubscriptionInfo info) {
        final TelephonyManager tm =
            (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getLine1NumberForSubscriber(info.getSubscriptionId());
    }

    private void log(String s) {
        Log.d(TAG, s);
    }

    /**
     * For search
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    if (Utils.showSimCardTile(context)) {
                        SearchIndexableResource sir = new SearchIndexableResource(context);
                        sir.xmlResId = R.xml.sim_settings;
                        result.add(sir);
                    }

                    return result;
                }
            };

     ///----------------------------------------MTK-----------------------------------------------

    private static final String KEY_SIM_ACTIVITIES = "sim_activities";

    // / M: for Plug in @{
    private ISettingsMiscExt mMiscExt;
    private ISimManagementExt mSimManagementExt;
    // / @}

    private ITelephonyEx mTelephonyEx;
    private SimHotSwapHandler mSimHotSwapHandler;
    private boolean mIsAirplaneModeOn = false;

    private static final int MODE_PHONE1_ONLY = 1;
    private PhoneServiceStateHandler mStateHandler;

            // Receiver to handle different actions
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "mReceiver action = " + action);
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                handleAirplaneModeChange(intent);
            } else if (action.equals(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
                updateCellularDataValues();
            } else if (action.equals(TelecomManagerEx.ACTION_DEFAULT_ACCOUNT_CHANGED)
                    || action.equals(TelecomManagerEx.ACTION_PHONE_ACCOUNT_CHANGED)) {
                updateCallValues();
            } else if (action.equals(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_DONE)
                    || action.equals(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_FAILED)) {
                updateCellularDataValues();
            } else if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
                updateCellularDataValues();
            }
        }
    };

    @Override
    public void onServiceStateChanged(ServiceState state, int subId) {
        Log.d(TAG, "PhoneStateListener:onServiceStateChanged: subId: " + subId
                + ", state: " + state);
        if (isRadioSwitchComplete(subId)) {
            handleRadioPowerSwitchComplete();
        }
        updateSimPref();
    }

    /**
     * init for sim state change, including SIM hot swap, airplane mode, etc.
     */
    private void initForSimStateChange() {
        mTelephonyEx = ITelephonyEx.Stub.asInterface(
                ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
        /// M: for [SIM Hot Swap] @{
        mSimHotSwapHandler = new SimHotSwapHandler(getActivity().getApplicationContext());
        mSimHotSwapHandler.registerOnSimHotSwap(new OnSimHotSwapListener() {
            @Override
            public void onSimHotSwap() {
                if (getActivity() != null) {
                    log("onSimHotSwap, finish Activity~~");
                    getActivity().finish();
                }
            }
        });
        /// @}

        mIsAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(getActivity().getApplicationContext());
        Log.d(TAG, "init()... air plane mode is: " + mIsAirplaneModeOn);

        mStateHandler = new PhoneServiceStateHandler(getActivity().getApplicationContext());
        mStateHandler.registerOnPhoneServiceStateChange(this);

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        // For PhoneAccount
        intentFilter.addAction(TelecomManagerEx.ACTION_DEFAULT_ACCOUNT_CHANGED);
        intentFilter.addAction(TelecomManagerEx.ACTION_PHONE_ACCOUNT_CHANGED);
        // For radio on/off
        intentFilter.addAction(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_DONE);
        intentFilter.addAction(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_FAILED);

        // listen to PHONE_STATE_CHANGE
        intentFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        getActivity().registerReceiver(mReceiver, intentFilter);
    }

    /**
     * whether radio switch finish on subId, according to the radio state.
     */
    private boolean isRadioSwitchComplete(final int subId) {
        if (getActivity() == null) {
            Log.d(TAG, "isRadioSwitchComplete()... activity is null");
            return false;
        }
        int slotId = SubscriptionManager.getSlotId(subId);
        int currentSimMode = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.MSIM_MODE_SETTING, -1);
        boolean expectedRadioOn = (currentSimMode & (MODE_PHONE1_ONLY << slotId)) != 0;
        Log.d(TAG, "soltId: " + slotId + ", expectedRadioOn : " + expectedRadioOn);
        if (expectedRadioOn && TelephonyUtils.isRadioOn(subId, getActivity())) {
            return true;
        } else if (!TelephonyUtils.isRadioOn(subId, mContext)) {
            return true;
        }
        return false;
    }

    /**
     * update SIM values after radio switch
     */
    private void handleRadioPowerSwitchComplete() {
        if(isResumed()) {
            updateSimSlotValues();
        }
        // for plug-in
        mSimManagementExt.showChangeDataConnDialog(this, isResumed());
    }

    /**
     * When airplane mode is on, some parts need to be disabled for prevent some telephony issues
     * when airplane on.
     * Default data is not able to switch as may cause modem switch
     * SIM radio power switch need to disable, also this action need operate modem
     * @param airplaneOn airplane mode state true on, false off
     */
    private void handleAirplaneModeChange(Intent intent) {
        mIsAirplaneModeOn = intent.getBooleanExtra("state", false);
        Log.d(TAG, "air plane mode is = " + mIsAirplaneModeOn);
        updateSimSlotValues();
        updateCellularDataValues();
        updateSimPref();
        removeItemsForTablet();
    }

    /**
     * remove unnecessary items for tablet
     */
    private void removeItemsForTablet() {
        // remove some item when in 4gds wifi-only
        if (FeatureOption.MTK_PRODUCT_IS_TABLET) {
            Preference sim_call_Pref = findPreference(KEY_CALLS);
            Preference sim_sms_Pref = findPreference(KEY_SMS);
            Preference sim_data_Pref = findPreference(KEY_CELLULAR_DATA);
            PreferenceCategory mPreferenceCategoryActivities =
                (PreferenceCategory) findPreference(KEY_SIM_ACTIVITIES);
            TelephonyManager tm = TelephonyManager.from(getActivity());
            if (!tm.isSmsCapable() && sim_sms_Pref != null) {
                mPreferenceCategoryActivities.removePreference(sim_sms_Pref);
            }
            if (!tm.isMultiSimEnabled() && sim_data_Pref != null && sim_sms_Pref != null) {
                mPreferenceCategoryActivities.removePreference(sim_data_Pref);
                mPreferenceCategoryActivities.removePreference(sim_sms_Pref);
            }
            if (!tm.isVoiceCapable() && sim_call_Pref != null) {
                mPreferenceCategoryActivities.removePreference(sim_call_Pref);
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        getActivity().unregisterReceiver(mReceiver);
        mSimHotSwapHandler.unregisterOnSimHotSwap();
        mStateHandler.unregisterOnPhoneServiceStateChange();
        super.onDestroy();
    }

    /**
     * only for plug-in, change "SIM" to "UIM/SIM".
     */
    private void customizeSimDisplay() {
        if (mSimCards != null) {
            mSimCards.setTitle(mMiscExt.customizeSimDisplayString(
                    getString(R.string.sim_settings_title),
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID));
        }
        getActivity().setTitle(
                mMiscExt.customizeSimDisplayString(getString(R.string.sim_settings_title),
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID));
    }


    private void updateSimPref() {
        if (shouldDisableActivitesCategory(getActivity())) {
            final Preference simCallsPref = findPreference(KEY_CALLS);
            if (simCallsPref != null) {
                final TelecomManager telecomManager = TelecomManager.from(getActivity());
                int accoutSum = telecomManager.getCallCapablePhoneAccounts().size();
                Log.d(TAG, "accountSum: " + accoutSum);
                simCallsPref.setEnabled(accoutSum > 1
                        && (!TelephonyUtils.isCapabilitySwitching())
                        && (!mIsAirplaneModeOn));
            }
            final Preference simSmsPref = findPreference(KEY_SMS);
            if (simSmsPref != null) {
                simSmsPref.setEnabled(mSelectableSubInfos.size() > 1
                        && (!TelephonyUtils.isCapabilitySwitching())
                        && (!mIsAirplaneModeOn));
            }
        }
    }

    private boolean isDataPrefEnable() {
        final boolean ecbMode = SystemProperties.getBoolean(
                TelephonyProperties.PROPERTY_INECM_MODE, false);
        log("isEcbMode()... isEcbMode: " + ecbMode);
        return mSelectableSubInfos.size() >= 1
                && (!TelephonyUtils.isCapabilitySwitching())
                && (!mIsAirplaneModeOn)
                && !TelecomManager.from(mContext).isInCall()
                && !ecbMode;
    }

    private static boolean shouldDisableActivitesCategory(Context context) {
        boolean shouldDisable = false;
        shouldDisable = CdmaUtils.isCdmaCardCompetion(context)
                || CdmaUtils.isCdamCardAndGsmCard(context);
        Log.d(TAG, "shouldDisableActivitesCategory() .. shouldDisable :" + shouldDisable);
        return shouldDisable;
    }
}
