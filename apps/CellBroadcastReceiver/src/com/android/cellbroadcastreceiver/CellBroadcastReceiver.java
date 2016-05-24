/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.cellbroadcastreceiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.telephony.CellBroadcastMessage;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaSmsCbProgramData;
import android.telephony.cdma.CdmaSmsCbProgramResults;
import android.util.Log;
import com.android.internal.telephony.PhoneConstants;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.cdma.sms.SmsEnvelope;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.uicc.IccCardProxy;

import java.util.List;
import java.util.ArrayList;

public class CellBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "[ETWS]CellBroadcastReceiver";
    static final boolean DBG = true;    // STOPSHIP: change to false before ship

    private static int mServiceState = -1;
    private static final String GET_LATEST_CB_AREA_INFO_ACTION =
            "android.cellbroadcastreceiver.GET_LATEST_CB_AREA_INFO";
    public static final String SMS_STATE_CHANGED_ACTION =
        "android.provider.Telephony.SMS_STATE_CHANGED";

    @Override
    public void onReceive(Context context, Intent intent) {
        onReceiveWithPrivilege(context, intent, false);
    }

    protected void onReceiveWithPrivilege(Context context, Intent intent, boolean privileged) {
        if (DBG) log("CellBroadcastReceiver onReceive " + intent);

        String action = intent.getAction();

        if (TelephonyIntents.ACTION_SERVICE_STATE_CHANGED.equals(action)) {
            if (DBG) log("Intent ACTION_SERVICE_STATE_CHANGED");
            int subId = intent.getExtras().getInt(PhoneConstants.SUBSCRIPTION_KEY);
            Log.d(TAG, "subscriptionId = " + subId);
            if (!SubscriptionManager.isValidSubscriptionId(subId)) {
                return;
            }
            ServiceState serviceState = ServiceState.newFromBundle(intent.getExtras());
            int newState = serviceState.getState();
            if (newState != mServiceState) {
                Log.d(TAG, "Service state changed! " + newState + " Full: " + serviceState +
                        " Current state=" + mServiceState);
                mServiceState = newState;

                if (((newState == ServiceState.STATE_IN_SERVICE) ||
                        (newState == ServiceState.STATE_EMERGENCY_ONLY)) &&
                        (UserHandle.myUserId() == UserHandle.USER_OWNER)) {
                    startConfigService(context.getApplicationContext(), subId);
                }
            }
        } else if (IccCardProxy.ACTION_INTERNAL_SIM_STATE_CHANGED.equals(action)){
            String simStatus = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
            if (IccCardConstants.INTENT_VALUE_ICC_LOADED.equals(simStatus)) {
                List<SubscriptionInfo> subscriptionInfoList = SubscriptionManager.from(
                        context).getActiveSubscriptionInfoList();
                if (subscriptionInfoList != null) {
                    for (SubscriptionInfo subInfo : subscriptionInfoList) {
                        startConfigService(context, subInfo.getSubscriptionId());
                    }
                }
        }
    } else if (Telephony.Sms.Intents.SMS_EMERGENCY_CB_RECEIVED_ACTION.equals(action)) {
            // If 'privileged' is false, it means that the intent was delivered to the base
            // no-permissions receiver class.  If we get an SMS_CB_RECEIVED message that way, it
            // means someone has tried to spoof the message by delivering it outside the normal
            // permission-checked route, so we just ignore it.
            /* SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean enableCB = prefs.getBoolean(
                    CellBroadcastSettings.KEY_ENABLE_ETWS_ALERT, false);
            if (!enableCB) {
                log("ignore CB RECEIVED ACTION because disabled enable_cell_broadcast");
                return;
            }*/
            if (privileged) {
                intent.setClass(context, CellBroadcastAlertService.class);
                context.startService(intent);
            } else {
                loge("ignoring unprivileged action received " + action);
            }
        } else if (Telephony.Sms.Intents.SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED_ACTION
                .equals(action)) {
            if (privileged) {
                String sender = intent.getStringExtra("sender");
                if (sender == null) {
                    Log.e(TAG, "SCPD intent received with no originating address");
                    return;
                }

                ArrayList<CdmaSmsCbProgramData> programData =
                        intent.getParcelableArrayListExtra("program_data");
                if (programData == null) {
                    Log.e(TAG, "SCPD intent received with no program_data");
                    return;
                }

                int subId = intent.getExtras().getInt(PhoneConstants.SUBSCRIPTION_KEY);
                Log.d(TAG, "subscriptionId = " + subId);
                ArrayList<CdmaSmsCbProgramResults> results = handleCdmaSmsCbProgramData(context,
                        programData, subId);
                Bundle extras = new Bundle();
                extras.putString("sender", sender);
                extras.putParcelableArrayList("results", results);
                setResult(Activity.RESULT_OK, null, extras);
            } else {
                loge("ignoring unprivileged action received " + action);
            }
        } else if (GET_LATEST_CB_AREA_INFO_ACTION.equals(action)) {
            if (privileged) {
                CellBroadcastMessage message = CellBroadcastReceiverApp.getLatestAreaInfo();
                if (message != null) {
                    Intent areaInfoIntent = new Intent(
                            CellBroadcastAlertService.CB_AREA_INFO_RECEIVED_ACTION);
                    areaInfoIntent.putExtra("message", message);
                    // Send broadcast twice, once for apps that have PRIVILEGED permission and once
                    // for those that have the runtime one
                    context.sendBroadcastAsUser(areaInfoIntent, UserHandle.ALL,
                            android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE);
                    context.sendBroadcastAsUser(areaInfoIntent, UserHandle.ALL,
                            android.Manifest.permission.READ_PHONE_STATE);
                }
            } else {
                Log.e(TAG, "caller missing READ_PHONE_STATE permission, returning");
            }
        } else if (TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE.equals(action)) {
            int subId = intent.getIntExtra(SubscriptionManager.UNIQUE_KEY_SUBSCRIPTION_ID,
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            CellBroadcastListItem.simInfoMap.remove(subId);
        CellBroadcastListItem.getSubString(context, subId);
    } else {
            Log.w(TAG, "onReceive() unexpected action " + action);
        }
    }

    /**
     * Handle Service Category Program Data message and return responses.
     *
     * @param context the context to use
     * @param programDataList an array of SCPD operations
     * @return the SCP results ArrayList to send to the message center
     */
    private static ArrayList<CdmaSmsCbProgramResults> handleCdmaSmsCbProgramData(Context context,
            ArrayList<CdmaSmsCbProgramData> programDataList, int subId) {
        ArrayList<CdmaSmsCbProgramResults> results
                = new ArrayList<CdmaSmsCbProgramResults>(programDataList.size());

        for (CdmaSmsCbProgramData programData : programDataList) {
            int result;
            switch (programData.getOperation()) {
                case CdmaSmsCbProgramData.OPERATION_ADD_CATEGORY:
                    result = tryCdmaSetCategory(context, programData.getCategory(), true, subId);
                    break;

                case CdmaSmsCbProgramData.OPERATION_DELETE_CATEGORY:
                    result = tryCdmaSetCategory(context, programData.getCategory(), false, subId);
                    break;

                case CdmaSmsCbProgramData.OPERATION_CLEAR_CATEGORIES:
                    tryCdmaSetCategory(context,
                            SmsEnvelope.SERVICE_CATEGORY_CMAS_EXTREME_THREAT, false, subId);
                    tryCdmaSetCategory(context,
                            SmsEnvelope.SERVICE_CATEGORY_CMAS_SEVERE_THREAT, false, subId);
                    tryCdmaSetCategory(context,
                            SmsEnvelope.SERVICE_CATEGORY_CMAS_CHILD_ABDUCTION_EMERGENCY,
                            false, subId);
                    tryCdmaSetCategory(context,
                            SmsEnvelope.SERVICE_CATEGORY_CMAS_TEST_MESSAGE, false, subId);
                    result = CdmaSmsCbProgramResults.RESULT_SUCCESS;
                    break;

                default:
                    Log.e(TAG, "Ignoring unknown SCPD operation " + programData.getOperation());
                    result = CdmaSmsCbProgramResults.RESULT_UNSPECIFIED_FAILURE;
            }
            results.add(new CdmaSmsCbProgramResults(programData.getCategory(),
                    programData.getLanguage(), result));
        }
        return results;
    }

    /**
     * Enables or disables a CMAS category.
     * @param context the context to use
     * @param category the CDMA service category
     * @param enable true to enable; false to disable
     * @return the service category program result code for this request
     */

    private static int tryCdmaSetCategory(Context context, int category,
    boolean enable, int subId) {
        String key;
        switch (category) {
            case SmsEnvelope.SERVICE_CATEGORY_CMAS_EXTREME_THREAT:
                key = SubscriptionManager.CB_EXTREME_THREAT_ALERT;
                break;

            case SmsEnvelope.SERVICE_CATEGORY_CMAS_SEVERE_THREAT:
                key = SubscriptionManager.CB_SEVERE_THREAT_ALERT;
                break;

            case SmsEnvelope.SERVICE_CATEGORY_CMAS_CHILD_ABDUCTION_EMERGENCY:
                key = SubscriptionManager.CB_AMBER_ALERT;
                break;

            case SmsEnvelope.SERVICE_CATEGORY_CMAS_TEST_MESSAGE:
                key = SubscriptionManager.CB_CMAS_TEST_ALERT;
                break;

            default:
                Log.w(TAG, "SCPD category " + category + " is unknown, not setting to " + enable);
                return CdmaSmsCbProgramResults.RESULT_UNSPECIFIED_FAILURE;
        }
        boolean oldValue = SubscriptionManager.getBooleanSubscriptionProperty(subId,
                        key,
                        (category != SmsEnvelope.SERVICE_CATEGORY_CMAS_TEST_MESSAGE), context);
        if (enable && oldValue) {
            Log.d(TAG, "SCPD category " + category + " is already enabled.");
            return CdmaSmsCbProgramResults.RESULT_CATEGORY_ALREADY_ADDED;
        } else if (!enable && !oldValue) {
            Log.d(TAG, "SCPD category " + category + " is already disabled.");
            return CdmaSmsCbProgramResults.RESULT_CATEGORY_ALREADY_DELETED;
        } else {
            Log.d(TAG, "SCPD category " + category + " is now " + enable);
            //fix build error sharedPrefs.edit().putBoolean(key, enable).apply();
            SubscriptionManager.setSubscriptionProperty(
                                        subId,
                                        key,
                                        (enable ? "1" : "0"));
            return CdmaSmsCbProgramResults.RESULT_SUCCESS;
        }
    }

    /**
     * Tell {@link CellBroadcastConfigService} to enable the CB channels.
     * @param context the broadcast receiver context
     */
    static void startConfigService(Context context, int subId) {
        Intent serviceIntent = new Intent(CellBroadcastConfigService.ACTION_ENABLE_CHANNELS,
                null, context, CellBroadcastConfigService.class);
        serviceIntent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, subId);
        context.startService(serviceIntent);
    }

    /**
     * @return true if the phone is a CDMA phone type
     */
    static boolean phoneIsCdma(int subId) {
        boolean isCdma = false;
        try {
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            if (phone != null) {
                isCdma = (phone.getActivePhoneTypeForSubscriber(subId) ==
                        TelephonyManager.PHONE_TYPE_CDMA);
            }
        } catch (RemoteException e) {
            Log.w(TAG, "phone.getActivePhoneType() failed", e);
        }
        return isCdma;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    private static void loge(String msg) {
        Log.e(TAG, msg);
    }
}
