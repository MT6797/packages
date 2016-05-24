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
package com.mediatek.contacts.list;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListView;

import com.android.contacts.common.list.ContactListFilter;
import com.android.contacts.common.list.ContactListItemView;

import com.mediatek.contacts.util.Log;

public class MultiGroupAddPickerAdapter extends MultiBasePickerAdapter {
    private static final String TAG = "MultiGroupAddPickerAdapter";

    public final String mMembersSelection = " IN " + "(SELECT " + RawContacts.CONTACT_ID + " FROM "
            + "view_raw_contacts" + " WHERE " + "view_raw_contacts.contact_id" + " NOT IN ";

    private String mAccountName;
    private String mAccountType;
    private long[] mExistMemberContactIds;

    public MultiGroupAddPickerAdapter(Context context, ListView lv) {
        super(context, lv);
    }

    public void setGroupAccount(String account, String type) {
        mAccountName = account;
        mAccountType = type;
    }

    public void setExistMemberList(long[] ids) {
        mExistMemberContactIds = ids;
    }

    @Override
    public void configureLoader(CursorLoader loader, long directoryId) {
        super.configureLoader(loader, directoryId);

        if (isSearchMode()) {
            String query = getQueryString();
            if (query == null) {
                query = "";
            }
            query = query.trim();
            if (!TextUtils.isEmpty(query)) {
                configureSelection(loader, directoryId, null);
            }
        }
    }

    @Override
    protected void configureSelection(CursorLoader loader, long directoryId,
            ContactListFilter filter) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (long id : this.mExistMemberContactIds) {
            sb.append(String.valueOf(id));
            sb.append(",");
        }
        if (mExistMemberContactIds.length > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append(")");
        Log.d(TAG, "[configureSelection]id string " + sb.toString());
        String selection = Contacts._ID + mMembersSelection + sb.toString();
        if (mAccountName != null && mAccountType != null) {
            String accountFilter = " AND view_raw_contacts." + RawContacts.ACCOUNT_NAME + "='"
                    + mAccountName + "' AND view_raw_contacts." + RawContacts.ACCOUNT_TYPE + "='"
                    + mAccountType + "'";
            selection += accountFilter;
        } else {
            String accountFilter = " AND view_raw_contacts." + RawContacts.ACCOUNT_NAME
                    + " IS NULL " + " AND view_raw_contacts." + RawContacts.ACCOUNT_TYPE
                    + " IS NULL ";
            selection += accountFilter;
        }
        selection += " )";
        Log.d(TAG, "[configureSelection]new selection " + selection.toString());
        loader.setSelection(selection);
    }

    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        super.bindView(itemView, partition, cursor, position);
        final ContactListItemView view = (ContactListItemView) itemView;
        if (isSearchMode()) {
            /** M: set snippet show */
            view.showSnippet(cursor, ContactQuery.CONTACT_SNIPPET);
        }
    }
}
