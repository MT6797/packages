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
package com.mediatek.contacts.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;

import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.internal.telephony.TelephonyIntents;

import com.mediatek.contacts.GlobalEnv;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.Log;

import java.util.List;

public class AccountTypeManagerEx {
    private static final String TAG = "AccountTypeManagerEx";

    /**
     * @param subId
     * @return the account type for this sub id
     */
    public static String getAccountTypeUsingSubId(int subId) {
        int simtype = -1;
        String simAccountType = null;

        simtype = SimCardUtils.getSimTypeBySubId(subId);
        if (SimCardUtils.SimType.SIM_TYPE_USIM == simtype) {
            simAccountType = AccountTypeUtils.ACCOUNT_TYPE_USIM;
        } else if (SimCardUtils.SimType.SIM_TYPE_SIM == simtype) {
            simAccountType = AccountTypeUtils.ACCOUNT_TYPE_SIM;
        } else if (SimCardUtils.SimType.SIM_TYPE_UIM == simtype) {
            simAccountType = AccountTypeUtils.ACCOUNT_TYPE_UIM;
        } else if (SimCardUtils.SimType.SIM_TYPE_CSIM == simtype) {
            simAccountType = AccountTypeUtils.ACCOUNT_TYPE_CSIM;
        }
        Log.d(TAG, "[getAccountTypeUsingSubId]subId:" + subId + ",AccountType:"
                + simAccountType);

        return simAccountType;
    }

    /**
     * @param subId
     *            sub id
     * @return the account name for this sub id
     */
    public static String getAccountNameUsingSubId(int subId) {
        String accountName = null;
        String iccCardType = SimCardUtils.getIccCardType(subId);
        if (iccCardType != null) {
            accountName = iccCardType + subId;
        }
        Log.d(TAG, "[getAccountNameUsingSubId]subId:" + subId + ",iccCardType =" + iccCardType
                + ",accountName:" + accountName);

        return accountName;
    }

    public static void registerReceiverOnSimStateAndInfoChanged(Context context,
            BroadcastReceiver broadcastReceiver) {
        Log.i(TAG, "[registerReceiverOnSimStateAndInfoChanged]...");
        IntentFilter simFilter = new IntentFilter();
        // For SIM Info Changed
        simFilter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        simFilter.addAction(TelephonyIntents.ACTION_PHB_STATE_CHANGED);
        simFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        context.registerReceiver(broadcastReceiver, simFilter);
    }

    public static void loadSimAndLocalAccounts(final List<AccountWithDataSet> allAccounts,
            final List<AccountWithDataSet> contactWritableAccounts,
            final List<AccountWithDataSet> groupWritableAccounts) {
        // /[Gemini+] add sim account @{
        List<SubscriptionInfo> subscriptionInfoList = SubInfoUtils.getActivatedSubInfoList();
        Log.d(TAG, "[loadSimAndLocalAccounts]subscriptionInfoList: " + subscriptionInfoList);
        if (subscriptionInfoList != null && subscriptionInfoList.size() > 0) {
            for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
                int subId = subscriptionInfo.getSubscriptionId();
                String accountName = AccountTypeManagerEx.getAccountNameUsingSubId(subId);
                String accountType = AccountTypeManagerEx.getAccountTypeUsingSubId(subId);
                if (!TextUtils.isEmpty(accountName) && !TextUtils.isEmpty(accountType)) {
                    allAccounts.add(new AccountWithDataSetEx(accountName, accountType, subId));
                    contactWritableAccounts.add(new AccountWithDataSetEx(accountName, accountType,
                            subId));
                    if (SimCardUtils.isUsimType(subId)) {
                        groupWritableAccounts.add(new AccountWithDataSetEx(accountName,
                                accountType, subId));
                    }
                    Log.d(TAG,
                            "[loadSimAndLocalAccounts]new AccountWithDataSetEx, AccountName: "
                                    + accountName + ", AccountType: " + accountType);
                }
            }
        }
        // /@}

        // Add Phone Local Type
        /*
         * Bug Fix by Mediatek Begin. Original Android's code:
         *
         * CR ID: ALPS00258229 Descriptions: if it is tablet let accountName is
         * tablet
         */
        if (GlobalEnv.isUsingTwoPanes()) {
            Log.i(TAG, "[loadSimAndLocalAccounts]it is tablet");
            allAccounts.add(new AccountWithDataSet("Tablet",
                    AccountTypeUtils.ACCOUNT_TYPE_LOCAL_PHONE, null));
            contactWritableAccounts.add(new AccountWithDataSet("Tablet",
                    AccountTypeUtils.ACCOUNT_TYPE_LOCAL_PHONE, null));
            groupWritableAccounts.add(new AccountWithDataSet("Tablet",
                    AccountTypeUtils.ACCOUNT_TYPE_LOCAL_PHONE, null));
        } else {
            Log.i(TAG, "[loadSimAndLocalAccounts]it is phone");
            allAccounts.add(new AccountWithDataSet("Phone",
                    AccountTypeUtils.ACCOUNT_TYPE_LOCAL_PHONE, null));
            contactWritableAccounts.add(new AccountWithDataSet("Phone",
                    AccountTypeUtils.ACCOUNT_TYPE_LOCAL_PHONE, null));
            groupWritableAccounts.add(new AccountWithDataSet("Phone",
                    AccountTypeUtils.ACCOUNT_TYPE_LOCAL_PHONE, null));
        }
        /*
         * Bug Fix by Mediatek End.
         */
    }
}
