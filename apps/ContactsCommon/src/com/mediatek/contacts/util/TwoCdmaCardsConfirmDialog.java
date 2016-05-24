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
package com.mediatek.contacts.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.contacts.common.R;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.telephony.TelephonyManagerEx.SetDefaultSubResultCallback;

/**
 * This dialog which shows a message, two buttons (ok/cancel) will display when the users switch
 * account between two CDMA cards.
 */
public class TwoCdmaCardsConfirmDialog extends DialogFragment {
    public static final String DIALOG_TAG = "confirm-dialog";
    public static final String INTENT_KEY = "intent";

    public TwoCdmaCardsConfirmDialog() {}

    public static TwoCdmaCardsConfirmDialog getInstance(Intent intent) {
        TwoCdmaCardsConfirmDialog fragment = new TwoCdmaCardsConfirmDialog();
        Bundle args = new Bundle();
        args.putParcelable(INTENT_KEY, intent);
        fragment.setArguments(args);
        return fragment;
    }

    public Dialog onCreateDialog(Bundle saveInstancestate) {
        final Intent intent = getArguments().getParcelable(INTENT_KEY);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getMessage(intent));
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
              SetIndicatorUtils selectionUtil = SetIndicatorUtils.getInstance();
              if (selectionUtil.isNotificationShown()) {
                  updateSelectedAccount(intent);
                  selectionUtil.setSimIndicatorVisibility(true);
              }
          }
      });

      return builder.create();
    }

    private String getMessage(Intent intent) {
        Context context = getActivity();
        PhoneAccountHandle selectedHandle =
                (PhoneAccountHandle) intent.getParcelableExtra(SetIndicatorUtils.EXTRA_ACCOUNT);
        int selectedSubId = TelephonyManager.from(context).getSubIdForPhoneAccount(
                TelecomManager.from(context).getPhoneAccount(selectedHandle));
        int currentSubId = SubscriptionManager.from(context).getDefaultDataSubId();
        String selectedLabel = getSubDisplayName(context, selectedSubId);
        String currentLabel = getSubDisplayName(context, currentSubId);
        String message = TextUtils.expandTemplate(
                context.getResources().getString(R.string.call_account_switch_tips),
                currentLabel, currentLabel, selectedLabel).toString();
        return message;
    }

    /**
     * Change the default call account, the other two default set of data, message will change also.
     * @param intent the intent includes the information of selected account
     */
    public void updateSelectedAccount(Intent intent) {
        Context context = getActivity();
        PhoneAccountHandle handle = (PhoneAccountHandle) intent.getParcelableExtra(
                SetIndicatorUtils.EXTRA_ACCOUNT);
        if (handle != null) {
        int selectedSubId = TelephonyManager.from(context).getSubIdForPhoneAccount(
                    TelecomManager.from(context).getPhoneAccount(handle));
            if (SubscriptionManager.isValidSubscriptionId(selectedSubId)) {
                TelephonyManagerEx.getDefault().setDefaultSubIdForAll(
                        TelephonyManagerEx.DEFAULT_VOICE, selectedSubId, mCallback);
            }
        }
    }

    private static SetDefaultSubResultCallback mCallback =
            new SetDefaultSubResultCallback() {
        public void onComplete(boolean result) {
            if (result) {
                Log.d(DIALOG_TAG, "onComplete success.");
            } else {
                Log.d(DIALOG_TAG, "onComplete failure.");
            }
        }
    };

    /**
     * Get the sub's display name.
     * @param subId the sub id
     * @param context the context
     * @return the sub's display name, may return null
     */
    public String getSubDisplayName(Context context, int subId) {
        CharSequence displayName = "";
        SubscriptionInfo subInfo = SubscriptionManager
                .from(context).getActiveSubscriptionInfo(subId);
        if (subInfo != null) {
            displayName = subInfo.getDisplayName();
        }
        return displayName.toString();
    }
}
