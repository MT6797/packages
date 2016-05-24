/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.contacts.simcontact;

import java.util.HashMap;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.contacts.common.activity.RequestPermissionsActivity;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;

import android.os.SystemProperties;
import com.mediatek.contacts.ContactsSystemProperties;
import com.mediatek.contacts.simservice.SIMProcessorService;
import com.mediatek.contacts.simservice.SIMServiceUtils;
import com.mediatek.contacts.util.Log;

import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

public class BootCmpReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCmpReceiver";
    public static final String NEED_REFRESH_SIM_CONTACTS = "need_refresh_sim_contacts";
    public static final String ACTION_REFRESH_SIM_CONTACT =
            "com.android.contacts.REFRESH_SIM_CONTACT";

    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.d(TAG, "onReceive, action is " + action);

        // add for multi-user ALPS01964765, whether the current user is running.
        // if not , will do nothing.
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        boolean isRunning = userManager.isUserRunning(new UserHandle(UserHandle.myUserId()));
        Log.d(TAG, "the current user is: " + UserHandle.myUserId()
                + " isRunning: " + isRunning);
        if (!isRunning) {
            return;
        }

        /// Change for ALPS02377518, should prevent accessing SubInfo if has no
        // basic permissions.
        // Mark NEED_REFRESH_SIM_CONTACTS as true if has no permission to sync SIM contacts. @{
        SharedPreferences perferences = context.getSharedPreferences(context.getPackageName(),
                Context.MODE_PRIVATE);
        if (RequestPermissionsActivity.hasBasicPermissions(context)) {
            if (action.equals(TelephonyIntents.ACTION_PHB_STATE_CHANGED)) {
                processPhoneBookChanged(context, intent);
            } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                if (!isPhbReady()) {
                    processBootComplete(context);
                }
            }
        } else if (action.equals(TelephonyIntents.ACTION_PHB_STATE_CHANGED)
                || action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            perferences.edit().putBoolean(NEED_REFRESH_SIM_CONTACTS, true).apply();
            Log.i(TAG, "Contact has no basic permissions");
        }
        /// @}

        /**
         * M: Bug Fix for CR ALPS01328816: when other owner, do not show sms
         * when share contact @{
         */
        if (action.equals("android.intent.action.USER_SWITCHED_FOR_MULTIUSER_APP")
                && ContactsSystemProperties.MTK_OWNER_SIM_SUPPORT) {
            if (UserHandle.myUserId() == UserHandle.USER_OWNER) {
                context.getPackageManager().setComponentEnabledSetting(
                        new ComponentName("com.android.contacts",
                                "com.mediatek.contacts.ShareContactViaSMSActivity"),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
            } else {
                context.getPackageManager().setComponentEnabledSetting(
                        new ComponentName("com.android.contacts",
                                "com.mediatek.contacts.ShareContactViaSMSActivity"),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            }
        }
        /** @} */

        /// Add for ALPS02383518, when BootCmpReceiver received PHB_CHANGED intent but has no
        // READ_PHONE permission, marked NEED_REFRESH_SIM_CONTACTS as true. So refresh
        // all SIM contacts after open all permission and back to contacts,
        // this action is sent from PeopleActivity$onCreate. @{
        if(action.equals(ACTION_REFRESH_SIM_CONTACT)) {
            resfreshAllSimContacts(context);
            perferences.edit().putBoolean(NEED_REFRESH_SIM_CONTACTS, false).apply();
        }
        /// @}
    }

    private boolean isPhbReady() {
        final int simCount = TelephonyManager.getDefault().getSimCount();
        Log.i(TAG, "isPhbReady simCount: " + simCount);
        for (int slotId = 0; slotId < simCount; slotId++) {
            int[] subId = SubscriptionManager.getSubId(slotId);
            if (subId != null && subId.length > 0 && SimCardUtils.isPhoneBookReady(subId[0])) {
                Log.i(TAG, "isPhbReady ready! ");
                return true;
            }
        }
        return false;
    }

    private void startSimService(Context context, int subId, int workType) {
        Intent intent = null;
        intent = new Intent(context, SIMProcessorService.class);
        intent.putExtra(SIMServiceUtils.SERVICE_SUBSCRIPTION_KEY, subId);
        intent.putExtra(SIMServiceUtils.SERVICE_WORK_TYPE, workType);
        Log.d(TAG, "[startSimService]subId:" + subId + "|workType:" + workType);
        context.startService(intent);
    }

    private void processPhoneBookChanged(Context context, Intent intent) {
        Log.d(TAG, "processPhoneBookChanged");
        boolean phbReady = intent.getBooleanExtra("ready", false);
        int subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY, -10);
        Log.d(TAG, "[processPhoneBookChanged]phbReady:" + phbReady + "|subId:" + subId);
        //if the PHB state has been changed,reset the phone book info
        //Only update the info when first used to avoid ANR in onReceiver when Boot Complete
        SlotUtils.clearActiveUsimPhbInfoMap();
        if (phbReady && subId > 0) {
            startSimService(context, subId, SIMServiceUtils.SERVICE_WORK_IMPORT);
        } else if (subId > 0 && !phbReady) {
            startSimService(context, subId, SIMServiceUtils.SERVICE_WORK_REMOVE);
        }
    }

    /**
     * fix for [PHB Status Refatoring] ALPS01003520
     * when boot complete,remove the contacts if the card of a slot had been removed
     */
    private void processBootComplete(Context context) {
        Log.d(TAG, "processBootComplete");
        startSimService(context, SIMServiceUtils.SERVICE_FORCE_REMOVE_SUB_ID,
            SIMServiceUtils.SERVICE_WORK_REMOVE);
    }

    public void resfreshAllSimContacts(Context context) {
        Log.i(TAG, "resfreshSimContacts");
        startSimService(context, SIMServiceUtils.SERVICE_FORCE_REMOVE_SUB_ID,
                SIMServiceUtils.SERVICE_WORK_REMOVE);
        List<SubscriptionInfo> subscriptionInfoList = SubInfoUtils.getActivatedSubInfoList();
        if (subscriptionInfoList == null || subscriptionInfoList.size() == 0) {
            Log.i(TAG, "resfreshSimContacts has no sim.");
            return;
        }
        for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
            Log.i(TAG, "resfreshSimContacts start get sub " + subscriptionInfo.getSubscriptionId());
            startSimService(context, subscriptionInfo.getSubscriptionId(),
                    SIMServiceUtils.SERVICE_WORK_IMPORT);
        }
    }
}
