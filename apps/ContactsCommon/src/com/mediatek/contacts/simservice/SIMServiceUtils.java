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
package com.mediatek.contacts.simservice;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.UserHandle;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;

import com.android.internal.telephony.PhoneConstants;

import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.Log;

import java.util.ArrayList;
import java.util.List;

public class SIMServiceUtils {
    private static final String TAG = "SIMServiceUtils";

    private static SIMProcessorState sSimProcessorState;

    public static final String ACTION_PHB_LOAD_FINISHED =
            "com.android.contacts.ACTION_PHB_LOAD_FINISHED";

    public static final String SERVICE_SUBSCRIPTION_KEY = "subscription_key";
    public static final String SERVICE_SLOT_KEY = "which_slot";
    public static final String SERVICE_WORK_TYPE = "work_type";

    public static final int SERVICE_WORK_NONE = 0;
    public static final int SERVICE_WORK_IMPORT = 1;
    public static final int SERVICE_WORK_REMOVE = 2;
    public static final int SERVICE_WORK_EDIT = 3;
    public static final int SERVICE_WORK_DELETE = 4;
    public static final int SERVICE_WORK_UNKNOWN = -1;
    public static final int SERVICE_IDLE = 0;
    public static final int SERVICE_FORCE_REMOVE_SUB_ID = -20;

    public static final int SERVICE_DELETE_CONTACTS = 1;
    public static final int SERVICE_QUERY_SIM = 2;
    public static final int SERVICE_IMPORT_CONTACTS = 3;

    public static final int SIM_TYPE_SIM = SimCardUtils.SimType.SIM_TYPE_SIM;
    public static final int SIM_TYPE_USIM = SimCardUtils.SimType.SIM_TYPE_USIM;
    public static final int SIM_TYPE_UIM = SimCardUtils.SimType.SIM_TYPE_UIM;
    public static final int SIM_TYPE_CSIM = SimCardUtils.SimType.SIM_TYPE_CSIM;
    public static final int SIM_TYPE_UNKNOWN = SimCardUtils.SimType.SIM_TYPE_UNKNOWN;

    public static final int TYPE_IMPORT = 1;
    public static final int TYPE_REMOVE = 2;

    public static class ServiceWorkData {
        public int mSubId = -1;
        public int mSimType = SIM_TYPE_UNKNOWN;
        public Cursor mSimCursor = null;

        ServiceWorkData() {
        }

        ServiceWorkData(int subId, int simType, Cursor simCursor) {
            mSubId = subId;
            mSimType = simType;
            mSimCursor = simCursor;
        }
    }

    public static void deleteSimContact(Context context, int subId) {
        Log.i(TAG, "[deleteSimContact]subId:" + subId);
        ArrayList<Integer> validSubIds = new ArrayList<Integer>();
        List<SubscriptionInfo> subscriptionInfoList = SubInfoUtils.getActivatedSubInfoList();
        if (subId != SERVICE_FORCE_REMOVE_SUB_ID && subscriptionInfoList != null
                && subscriptionInfoList.size() > 0) {
            for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
                if (subscriptionInfo.getSubscriptionId() != subId) {
                    validSubIds.add(subscriptionInfo.getSubscriptionId());
                }
            }
        }

        // Be going to delete the invalid SIM contacts records.
        StringBuilder delSelection = new StringBuilder();
        String filter = null;
        for (int id : validSubIds) {
            delSelection.append(id).append(",");
        }
        if (delSelection.length() > 0) {
            delSelection.deleteCharAt(delSelection.length() - 1);
            filter = delSelection.toString();
        }
        filter = TextUtils.isEmpty(filter) ? RawContacts.INDICATE_PHONE_SIM + " > 0 "
                : RawContacts.INDICATE_PHONE_SIM + " > 0 " + " AND "
                        + RawContacts.INDICATE_PHONE_SIM + " NOT IN (" + filter + ")";
        Log.d(TAG, "[deleteSimContact]sim contacts filter:" + filter);
        int count = context.getContentResolver().delete(
                RawContacts.CONTENT_URI.buildUpon().appendQueryParameter("sim", "true").build(),
                filter, null);
        // add for ALPS01964765.
        Log.d(TAG, "[deleteSimContact]the current user is: " + UserHandle.myUserId() +
                ",count = " + count);

        // Be going to delete the invalid USIM group records.
        delSelection = new StringBuilder();
        filter = null;
        for (int id : validSubIds) {
            delSelection.append("'" + "USIM" + id + "'" + ",");
        }

        if (delSelection.length() > 0) {
            delSelection.deleteCharAt(delSelection.length() - 1);
            filter = delSelection.toString();
        }
        filter = TextUtils.isEmpty(filter) ? (Groups.ACCOUNT_TYPE + "='USIM Account'")
                : (Groups.ACCOUNT_NAME + " NOT IN " + "(" + filter + ")" + " AND "
                        + Groups.ACCOUNT_TYPE + "='USIM Account'");
        Log.d(TAG, "[deleteSimContact]usim group filter:" + filter);
        count = context.getContentResolver().delete(Groups.CONTENT_URI, filter, null);
        Log.d(TAG, "[deleteSimContact] group count:" + count);

        sendFinishIntent(context, subId);
    }

    /**
     * check PhoneBook State is ready if ready, then return true.
     *
     * @param subId
     * @return
     */
    static boolean checkPhoneBookState(final int subId) {
        return SimCardUtils.isPhoneBookReady(subId);
    }

    static void sendFinishIntent(Context context, int subId) {
        Log.i(TAG, "[sendFinishIntent]subId:" + subId);
        Intent intent = new Intent(ACTION_PHB_LOAD_FINISHED);

        intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, subId);
        context.sendBroadcast(intent);
    }

    public static boolean isServiceRunning(int subId) {
        if (sSimProcessorState != null) {
            return sSimProcessorState.isImportRemoveRunning(subId);
        }

        return false;
    }

    public static int getServiceState(int subId) {
        return 0;
    }

    public static void setSIMProcessorState(SIMProcessorState processorState) {
        sSimProcessorState = processorState;
    }

    public interface SIMProcessorState {
        public boolean isImportRemoveRunning(int subId);
    }
}
