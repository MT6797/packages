/*
* Copyright (C) 2011-2014 Mediatek.inc.
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

package com.mediatek.settings;

import android.preference.PreferenceActivity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

import android.provider.Settings;
import android.util.Log;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;

import android.os.RemoteException;
import android.os.ServiceManager;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.phone.R;

import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.List;



public class MobileNetworkSettingsOmEx {
    private static final String LOG_TAG = "MobileNetworkSettingsOmEx";

    private PreferenceScreen mPreferenceScreen;
    private ListPreference mListPreference;
    public static final String BUTTON_PREFERED_NETWORK_MODE = "preferred_network_mode_key";
    public static final String BUTTON_ENABLED_NETWORK_MODE = "enabled_networks_key";
    public static final String BUTTON_NETWORK_MODE_LTE_KEY = "button_network_mode_LTE_key";
    private static final String BUTTON_ROAMING_KEY = "button_roaming_key";
    public static final String SIM = "SIM";
    private final static String LTE_SUPPORT = "1";
    private static final boolean RADIO_POWER_OFF = false;
    private static final boolean RADIO_POWER_ON = true;
    private static final int MODE_PHONE1_ONLY = 1;
    private Context mContext = null;
    private int mCurrentTab = -1;
    private boolean mFlag = false;
    private static final String[] MCCMNC_TABLE_TYPE_CT = {
        "45502", "46003", "46011", "46012", "46013"};
    private static final String ENABLE_4G_DATA = "enable_4g_data";

    public MobileNetworkSettingsOmEx(Context context) {
        if (!SystemProperties.get("ro.cmcc_light_cust_support").equals("1")
            || !isLTESupport()) {
            return;
        }
        mContext = context;
        log("mContext =" + mContext);
    }

    /**
     * init CMCC network preference screen
     * @param activity
     * @param currentTab current Tab
     */
    public void initMobileNetworkSettings(PreferenceActivity activity, int currentTab) {
        if (!SystemProperties.get("ro.cmcc_light_cust_support").equals("1")
            || !isLTESupport()) {
            return;
        }
        log("init currentTab: " + currentTab);
        mCurrentTab = currentTab;
        if (activity != null) {
            if (mFlag == false) {
                log("init, mFlag==false");
                IntentFilter filter= new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
                mContext.registerReceiver(mReceiver, filter);
                mFlag = true;
            }
        } else {
            log("init with preferenceScreen null");
            return;
        }
        mPreferenceScreen = activity.getPreferenceScreen();
        if (mPreferenceScreen == null) {
            Log.d(LOG_TAG, "init mPreferenceScreen null");
            return;
        }

        ListPreference buttonEnabledNetworkPreference =
            (ListPreference) mPreferenceScreen.findPreference(BUTTON_ENABLED_NETWORK_MODE);
        ListPreference buttonPreferredNetworkPreference =
            (ListPreference) mPreferenceScreen.findPreference(BUTTON_PREFERED_NETWORK_MODE);
        ListPreference buttonNetworkModeLtePreference =
            (ListPreference) mPreferenceScreen.findPreference(BUTTON_NETWORK_MODE_LTE_KEY);
        ListPreference ctButtonEnabledNetworks = null;

       // config CdmaSettingsItems.
       if (isCTCard(mCurrentTab)) {
            if (mPreferenceScreen.findPreference(BUTTON_ENABLED_NETWORK_MODE) == null) {
                Log.d(LOG_TAG, "init, CT card add ENABLED_NETWORK_MODE");
                ctButtonEnabledNetworks = new ListPreference(activity);
                ctButtonEnabledNetworks.setTitle(
                        mContext.getString(R.string.preferred_network_mode_title));
                ctButtonEnabledNetworks.setKey(BUTTON_ENABLED_NETWORK_MODE);
                ctButtonEnabledNetworks.setSummary(R.string.preferred_network_mode_cmcc_om);
                ctButtonEnabledNetworks.setEnabled(false);

                SwitchPreference buttonDataRoam = (SwitchPreference) mPreferenceScreen.
                    findPreference(BUTTON_ROAMING_KEY);
                int order = buttonDataRoam != null ? buttonDataRoam.getOrder() : 0;
                ctButtonEnabledNetworks.setOrder(order + 1);
                mPreferenceScreen.addPreference(ctButtonEnabledNetworks);
            }
        }

        if (buttonEnabledNetworkPreference != null) {
            updateNetworkTypeSummary(buttonEnabledNetworkPreference);
            mListPreference = buttonEnabledNetworkPreference;
        } else if (buttonPreferredNetworkPreference != null) {
            updateNetworkTypeSummary(buttonPreferredNetworkPreference);
            mListPreference = buttonPreferredNetworkPreference;
        } else if ((ctButtonEnabledNetworks != null) &&
                (isNotPrimarySIM(get34GCapabilitySIMSlotId()))) {
            updateNetworkTypeSummary(ctButtonEnabledNetworks);
            mListPreference = ctButtonEnabledNetworks;
        } else if(buttonNetworkModeLtePreference != null) {
            updateNetworkTypeSummary(buttonNetworkModeLtePreference);
            mListPreference = buttonNetworkModeLtePreference;
        }
    }

    /**
     *up is internal use, under is host to call
     * @param preference
     */
    public void updateNetworkTypeSummary(ListPreference preference) {
        if (!SystemProperties.get("ro.cmcc_light_cust_support").equals("1")
            || !isLTESupport()
            || (preference == null)) {
            return;
        }

        int subId = get34GCapabilitySubId();
        int slotId = get34GCapabilitySIMSlotId();
        String type = getSIMType(subId);
        log("update, slotId:" + slotId + "type: " + type);

        if (slotId == -1 || isNotPrimarySIM(slotId) ||
                !isLTEModeEnable(slotId, preference.getContext())) {
            if (mPreferenceScreen == null) {
                log("update, mPreferenceScreen = null");
                return;
            } else {
                log("update, removePreference");
                mPreferenceScreen.removePreference(preference);
            }
        } else {
            Phone phone = getPhoneUsingSubId(subId);
            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    phone.getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + subId,
                    Phone.NT_MODE_LTE_GSM_WCDMA);
            log("update mode:" +  settingsNetworkMode);
            preference.setSummary(R.string.preferred_network_mode_cmcc_om);
            preference.setEnabled(false);
            log("update mode, 4G/3G/2G(AUTO) disable");
       }
    }

    /**
     * update LTE mode status
     * @param preference
     */
    public void updateLTEModeStatus(ListPreference preference) {
        if (!SystemProperties.get("ro.cmcc_light_cust_support").equals("1")
            || !isLTESupport()) {
            return;
        }
        log("updateLTEModeStatus");
        updateNetworkTypeSummary(preference);
        if (!preference.isEnabled()) {
            Dialog dialog = preference.getDialog();
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
                log("updateLTEModeStatus: dismiss dialog ");
            }
        }
    }

    /**
     * is LTE Mode Enable
     * @param slotId
     * @param context
     * @return true or false
     */
    private boolean isLTEModeEnable(int slotId, Context context) {
        log("isLTEModeEnable, slotId = " + slotId);
        if(getRadioStateForSlotId(slotId, context) == RADIO_POWER_OFF ||
                getSimOperator(slotId) == null || getSimOperator(slotId).equals("")) {
            log("RadioState OFF, or SimOperator is null");
            return false;
        }
        log("isLTEModeEnable, should enable");
        return true;
    }

    /**
     * get 3G/4G capability slotId
     * @return the SIM id which support 3G/4G.
     */
    private int get34GCapabilitySIMSlotId() {
        int slotId = -1;
        int subId = get34GCapabilitySubId();
        if(subId >= 0) {
            slotId = SubscriptionManager.getSlotId(subId);
        }
        log("get4GCapabilitySIMSlotId, slotId: " + slotId);
        return slotId;
    }

    /**
     * get the 3G/4G Capability subId.
     * @return the 3G/4G Capability subId
     */
    private int get34GCapabilitySubId() {
        int subId = -1;
        ITelephonyEx iTelEx = ITelephonyEx.Stub.asInterface(ServiceManager.
                getService(Context.TELEPHONY_SERVICE_EX));
        if (iTelEx != null) {
            try {
            int phoneId = iTelEx.getMainCapabilityPhoneId();
            log("subId : " + subId + ", PhoneId : " + phoneId);
            if (phoneId >= 0) {
                subId = SubscriptionManager.getSubIdUsingPhoneId(iTelEx.
                        getMainCapabilityPhoneId());
                }
            } catch (RemoteException e) {
                log("get34GCapabilitySubId FAIL to getSubId" + e.getMessage());
            }
        }
        return subId;
    }

    /**
     * get radio state for slot id
     * @param slotId
     * @param context
     * @return radio state, on or off
     */
    private boolean getRadioStateForSlotId(final int slotId, Context context) {
        int currentSimMode = Settings.System.getInt(context.getContentResolver(),
                Settings.System.MSIM_MODE_SETTING, -1);
        boolean radiosState = ((currentSimMode & (MODE_PHONE1_ONLY << slotId)) == 0) ?
                RADIO_POWER_OFF : RADIO_POWER_ON;
        log("soltId: " + slotId + ", radiosState : " + radiosState);
        return radiosState;
    }

    /**
     * check the slotId value.
     * @param slotId
     * @return true or false
    */
    private boolean isValidSlot(int slotId) {
        final int[] geminiSlots = {0, 1};
        for (int i = 0; i < geminiSlots.length; i++) {
            if (geminiSlots[i] == slotId) {
                return true;
            }
        }
        return false;
     }

    /**
      * @get SimConfig by TelephonyManager.getDefault().getMultiSimConfiguration().
      * @return true if the device has 2 or more slots
      */
    private boolean isGeminiSupport() {
        TelephonyManager.MultiSimVariants mSimConfig = TelephonyManager.
                getDefault().getMultiSimConfiguration();
        if (mSimConfig == TelephonyManager.MultiSimVariants.DSDS ||
                mSimConfig == TelephonyManager.MultiSimVariants.DSDA) {
            return true;
        }
        return false;
    }

    /**
     * get the state of the device SIM card
     * @param slotId
     * @return return SIM state.
     */
    private int getSimState(int slotId) {
        int status;
        if (isGeminiSupport() && isValidSlot(slotId)) {
            status = TelephonyManagerEx.getDefault().getSimState(slotId);
        } else {
            status = TelephonyManager.getDefault().getSimState();
        }
        log("getSimState, slotId = " + slotId + "; status = " + status);
        return status;
    }

    /**
     * Gets the MCC+MNC (mobile country code + mobile network code) of the provider of the SIM.
     * 5 or 6 decimal digits.
     * Availability: The result of calling getSimState() must be 
     * android.telephony.TelephonyManager.SIM_STATE_READY.
     * @param slotId  Indicates which SIM to query.
     * @return MCC+MNC (mobile country code + mobile network code) of the provider of the SIM.
     *         5 or 6 decimal digits.
     */
    private String getSimOperator(int slotId) {
       String simOperator = null;
       boolean isSimStateReady = false;
       isSimStateReady = TelephonyManager.SIM_STATE_READY == getSimState(slotId);
       if (isSimStateReady) {
           if (isGeminiSupport()) {
               simOperator = TelephonyManagerEx.getDefault().getSimOperator(slotId);
           } else {
               simOperator = TelephonyManager.getDefault().getSimOperator();
           }
       }
       log("getSimOperator, simOperator = " + simOperator + " slotId = " + slotId);
       return simOperator;
    }

    /**
     * app use to judge LTE open
     * @return true is LTE open
     */
    private boolean isLTESupport() {
        boolean isSupport = LTE_SUPPORT.equals(
                SystemProperties.get("ro.mtk_lte_support")) ? true : false;
        return isSupport;
    }

    private String getSIMType(int subId) {
        String type = null;
        if (subId > 0) {
            try {
               type = ITelephonyEx.Stub.asInterface(ServiceManager.
                       getService("phoneEx")).getIccCardType(subId);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "getSIMType, exception: ", e);
            }
        }
        return type;
     }

    /**
     * Get the sub's display name.
     * @param subId the sub id
     * @return the sub's display name, may return null
     */
    private String getSubDisplayName(int subId) {
        String displayName = "";
        SubscriptionInfo subInfo = null;
        if(subId > 0) {
            subInfo = SubscriptionManager.from(mContext).getSubscriptionInfo(subId);
            if (subInfo != null) {
                displayName = subInfo.getDisplayName().toString();
            }
        }
        return displayName;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            log("action: " + action);
            if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)
                    && mListPreference != null) {
                updateNetworkTypeSummary(mListPreference);
            }
        }
    };

    /**
     * Get phone by sub id.
     * @param subId the sub id
     * @return phone according to the sub id
     */
    private Phone getPhoneUsingSubId(int subId) {
        log("getPhoneUsingSubId subId:" + subId);
        int phoneId = SubscriptionManager.getPhoneId(subId);
        if ((phoneId < 0) || (phoneId >= TelephonyManager.getDefault().getPhoneCount()) || (phoneId == Integer.MAX_VALUE)) {
            return PhoneFactory.getPhone(0);
        }
        return PhoneFactory.getPhone(phoneId);
    }

    /**
     * adjust not primary SIM.
     * @param slotId the sub id.
     * @return true if not primary SIM.
     */
    private boolean isNotPrimarySIM(int slotId) {
        List<SubscriptionInfo> result = SubscriptionManager.from(mContext)
                .getActiveSubscriptionInfoList();
        if (result == null) {
            log("isNotPrimarySIM false, result null");
        } else {
            if (isGeminiSupport() && result.size() > 1 && slotId != mCurrentTab) {
                return true;
            }
        }
        log("isNotPrimarySIM false, slotId:" + slotId);
        return false;
    }

    /**
     * unRegister mReceiver.
     */
    public void unRegister() {
        if (!SystemProperties.get("ro.cmcc_light_cust_support").equals("1")
            || !isLTESupport()) {
            return;
        }
        log("unRegister");
        if (mFlag == true && mReceiver != null) {
             log("unRegister, mFlag = true");
             mContext.unregisterReceiver(mReceiver);
             mFlag = false;
        }
        mListPreference = null;
        mPreferenceScreen = null;
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    /**
     * app use to judge the Card is  CT.
     * @param slotId the slotId
     * @return true is CT
     */
    private boolean isCTCard(int slotId) {
        log("isCTCard, slotId = " + slotId);
        String simOperator = null;
        simOperator = getSimOperator(slotId);
        if (simOperator != null) {
            for (String mccmnc : MCCMNC_TABLE_TYPE_CT) {
                if (simOperator.equals(mccmnc)) {
                    return true;
                }
            }
        }
        return false;
    }

}
