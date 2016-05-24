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

import android.content.Context;
import android.os.UserHandle;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.util.AccountsListAdapter.AccountListFilter;
import com.android.contacts.common.model.account.ExchangeAccountType;

import com.mediatek.contacts.ContactsSystemProperties;
import com.mediatek.contacts.model.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SlotUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * List-Adapter for Account selection
 */
public final class AccountsListAdapterUtils {
    private static final String TAG = "AccountsListAdapterUtils";

    /**
     * @param accountTypeManager
     *            Account type manager.
     */
    public static ArrayList<AccountWithDataSet> getGroupAccount(
            AccountTypeManager accountTypeManager) {
        List<AccountWithDataSet> accountList = accountTypeManager.getGroupWritableAccounts();
        List<AccountWithDataSet> newAccountList = new ArrayList<AccountWithDataSet>();
        Log.i(TAG, "[getGroupAccount]accountList size:" + accountList.size());
        for (AccountWithDataSet account : accountList) {
            if (account instanceof AccountWithDataSetEx) {
                int subId = ((AccountWithDataSetEx) account).getSubId();
                Log.d(TAG, "[getGroupAccount]subId:" + subId);
                // For MTK multiuser in 3gdatasms @{
                if (ContactsSystemProperties.MTK_OWNER_SIM_SUPPORT) {
                    int userId = UserHandle.myUserId();
                    AccountType accountType = accountTypeManager.getAccountType(account.type,
                            account.dataSet);
                    if (userId != UserHandle.USER_OWNER) {
                        if (accountType != null && accountType.isIccCardAccount()) {
                            continue;
                        }
                    }
                }

                if (SimCardUtils.isUsimType(subId)) {
                    Log.d(TAG, "[getGroupAccount]getUSIMGrpMaxNameLen:"
                                    + SlotUtils.getUsimGroupMaxNameLength(subId));
                    if (SlotUtils.getUsimGroupMaxNameLength(subId) > 0) {
                        newAccountList.add(account);
                    }
                } else {
                    newAccountList.add(account);
                }
            } else {
                newAccountList.add(account);
            }
        }
        return new ArrayList<AccountWithDataSet>(newAccountList);
    }

    /**
     * @param accountTypeManager
     *            Account type manager
     * @param accountListFilter
     *            the filter of the account type
     */
    public static ArrayList<AccountWithDataSet> getAccountForMultiUser(
            AccountTypeManager accountTypeManager, AccountListFilter accountListFilter) {
        if (ContactsSystemProperties.MTK_OWNER_SIM_SUPPORT) {
            int userId = UserHandle.myUserId();
            if (userId != UserHandle.USER_OWNER) {
                if (accountListFilter == AccountListFilter.ACCOUNTS_CONTACT_WRITABLE) {
                    List<AccountWithDataSet> accountList = accountTypeManager
                            .getGroupWritableAccounts();
                    List<AccountWithDataSet> newAccountList = new ArrayList<AccountWithDataSet>();
                    for (AccountWithDataSet account : accountList) {
                        if (account instanceof AccountWithDataSetEx) {
                            AccountType accountType = accountTypeManager.getAccountType(
                                    account.type, account.dataSet);
                            if (accountType != null && accountType.isIccCardAccount()) {
                                continue;
                            }
                            newAccountList.add(account);
                        } else {
                            newAccountList.add(account);
                        }
                    }
                    return new ArrayList<AccountWithDataSet>(newAccountList);
                }
            }
        }
        return null;
    }

    /**
     * @param context
     *            the context for resrouce
     * @param account
     *            the current account data set
     * @param accountType
     *            the current account type
     * @param text2
     *            the sim name
     * @param icon
     *            the sim icon
     */
    public static void getViewForName(Context context, AccountWithDataSet account,
            AccountType accountType, TextView text2, ImageView icon) {
        int subId = -1;
        if (account instanceof AccountWithDataSetEx) {
            subId = ((AccountWithDataSetEx) account).getSubId();
            String displayName = ((AccountWithDataSetEx) account).getDisplayName();
            if (TextUtils.isEmpty(displayName)) {
                displayName = account.name;
            }
            text2.setText(displayName);
        } else {
            text2.setText(account.name);
        }
        boolean isLocalPhone = AccountWithDataSetEx.isLocalPhone(accountType.accountType);
        int activtedSubInfoCount = SubInfoUtils.getActivatedSubInfoCount();
        Log.d(TAG, "[getViewForName]isLocalPhone:" + isLocalPhone + ",activtedSubInfoCount = "
                + activtedSubInfoCount + "accountType.accountType = " + accountType.accountType
                + ",account.name = " + account.name);
        if (isLocalPhone || activtedSubInfoCount <= 1) {
            text2.setVisibility(View.GONE);
        } else {
            text2.setVisibility(View.VISIBLE);
        }

        if (ExchangeAccountType.isExchangeType(accountType.accountType)) {
            text2.setVisibility(View.VISIBLE);
        }
        text2.setEllipsize(TruncateAt.MIDDLE);
        if (accountType != null && accountType.isIccCardAccount()) {
            icon.setImageDrawable(accountType.getDisplayIconBySubId(context, subId));
        } else {
            Log.d(TAG, "[getViewForName] set image");
            icon.setImageDrawable(accountType.getDisplayIcon(context));
        }
    }

}
