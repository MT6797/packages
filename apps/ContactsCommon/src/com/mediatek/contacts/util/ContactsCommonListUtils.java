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
import android.content.CursorLoader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.contacts.common.ContactPhotoManager.DefaultImageRequest;
import com.android.contacts.common.R;
import com.android.contacts.common.list.ContactListFilter;
import com.android.contacts.common.list.ProfileAndContactsLoader;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.util.AccountFilterUtil;

import com.mediatek.contacts.ContactsSystemProperties;
import com.mediatek.contacts.model.AccountWithDataSetEx;
import com.mediatek.contacts.widget.WaitCursorView;

import java.util.ArrayList;
import java.util.List;

/** define some util functions for ContactsCommon/list */
public class ContactsCommonListUtils {
    private static final String TAG = "ContactsCommonListUtils";

    /**
     * For multiuser in 3gdatasms
     */
    public static boolean isUserOwner() {
        if (ContactsSystemProperties.MTK_OWNER_SIM_SUPPORT) {
            int userId = UserHandle.myUserId();
            if (userId != UserHandle.USER_OWNER) {
                return false;
            }
        }
        return true;
    }

    /**
     * ALPS913966 cache displayname in account filter and push to intent.
     **/
    public static void addToAccountFilter(Context context, AccountWithDataSet account,
            ArrayList<ContactListFilter> accountFilters, AccountType accountType) {
        int subId = ((AccountWithDataSetEx) account).mSubId;
        String displayName = AccountFilterUtil.getAccountDisplayNameByAccount(account.type,
                account.name);
        Log.d(TAG, "[addToAccountFilter] displayName : " + displayName + " subId : " + subId);
        Drawable icon = accountType.getDisplayIconBySubId(context, subId);
        accountFilters.add(ContactListFilter.createAccountFilter(account.type, account.name,
                account.dataSet, icon, displayName));
    }

    /**
     * Bug Fix For ALPS00115673 Descriptions: add wait cursor.
     */
    public static WaitCursorView initLoadingView(Context context, View listLayout,
            View loadingContainer, TextView loadingContact, ProgressBar progress) {
        loadingContainer = listLayout.findViewById(R.id.loading_container);
        loadingContainer.setVisibility(View.GONE);
        loadingContact = (TextView) listLayout.findViewById(R.id.loading_contact);
        loadingContact.setVisibility(View.GONE);
        progress = (ProgressBar) listLayout.findViewById(R.id.progress_loading_contact);
        progress.setVisibility(View.GONE);
        return new WaitCursorView(context, loadingContainer, progress, loadingContact);
    }

    /**
     * for SIM name display
     */
    public static void setAccountTypeText(Context context, AccountType accountType,
            TextView accountTypeView, TextView accountUserNameView, ContactListFilter filter) {
        String displayName = null;
        displayName = AccountFilterUtil.getAccountDisplayNameByAccount(filter.accountType,
                filter.accountName);
        if (TextUtils.isEmpty(displayName)) {
            accountTypeView.setText(filter.accountName);
        } else {
            accountTypeView.setText(displayName);
        }
        if (AccountWithDataSetEx.isLocalPhone(accountType.accountType)) {
            accountUserNameView.setVisibility(View.GONE);
            accountTypeView.setText(accountType.getDisplayLabel(context));
        }
    }

    /**
     * For multiuser in 3gdatasms
     */
    public static boolean isAccountTypeSimUsim(AccountType accountType) {
        if (accountType != null && AccountTypeUtils.isAccountTypeIccCard(
                accountType.accountType)) {
            return true;
        }
        return false;
    }

    /**
     * Bug Fix CR ID: ALPS00112614 Descriptions: only show phone contact if it's
     * from sms
     */
    public static void configureOnlyShowPhoneContactsSelection(CursorLoader loader,
            long directoryId, ContactListFilter filter) {
        Log.d(TAG, "[configureOnlyShowPhoneContactsSelection] directoryId :" + directoryId
                + ",filter : " + filter);
        if (filter == null) {
            return;
        }

        if (directoryId != Directory.DEFAULT) {
            return;
        }

        StringBuilder selection = new StringBuilder();
        List<String> selectionArgs = new ArrayList<String>();

        selection.append(Contacts.INDICATE_PHONE_SIM + "= ?");
        selectionArgs.add("-1");

        loader.setSelection(selection.toString());
        loader.setSelectionArgs(selectionArgs.toArray(new String[0]));
    }

    /**
     * Change Feature: As Local Phone account contains null account and Phone
     * Account, the Account Query Parameter could not meet this requirement. So,
     * We should keep to query contacts with selection.
     */
    public static void buildSelectionForFilterAccount(ContactListFilter filter,
            StringBuilder selection, List<String> selectionArgs) {
        selectionArgs.add(filter.accountType);
        selectionArgs.add(filter.accountName);
        if (filter.dataSet != null) {
            selection.append(" AND " + RawContacts.DATA_SET + "=? )");
            selectionArgs.add(filter.dataSet);
        } else {
            selection.append(" AND " + RawContacts.DATA_SET + " IS NULL )");
        }
        selection.append("))");
    }

    private static Cursor loadSDN(Context context,
            ProfileAndContactsLoader profileAndContactsLoader) {
        Log.d(TAG, "[loadSDN]...");
        Cursor sdnCursor = null;
        if (null != profileAndContactsLoader.getSelection()
                && profileAndContactsLoader.getSelection().indexOf(
                        RawContacts.IS_SDN_CONTACT + " < 1") >= 0) {
            Uri uri = profileAndContactsLoader.getUri();
            String[] projection = profileAndContactsLoader.getProjection();
            String newSelection = profileAndContactsLoader.getSelection().replace(
                    RawContacts.IS_SDN_CONTACT + " < 1", RawContacts.IS_SDN_CONTACT + " = 1");
            String[] selectionArgs = profileAndContactsLoader.getSelectionArgs();
            String sortOrder = profileAndContactsLoader.getSortOrder();
            sdnCursor = context.getContentResolver().query(uri, projection, newSelection,
                    selectionArgs, sortOrder);
            if (sdnCursor == null) {
                Log.w(TAG, "[loadSDN]sdnCursor is null need to check");
                return null;
            }
            MatrixCursor matrix = new MatrixCursor(projection);
            try {
                Object[] row = new Object[projection.length];
                while (sdnCursor.moveToNext()) {
                    for (int i = 0; i < row.length; i++) {
                        row[i] = sdnCursor.getString(i);
                    }
                    matrix.addRow(row);
                }
                return matrix;
            } finally {
                if (null != sdnCursor) {
                    sdnCursor.close();
                }
            }
        }
        Log.d(TAG, "[loadSDN] return null");
        return null;
    }

    /**
     * New Feature SDN
     * */
    public static int addCursorAndSetSelection(Context context,
            ProfileAndContactsLoader profileAndContactsLoader, List<Cursor> cursors,
            int sdnContactCount) {
        String oldSelection = profileAndContactsLoader.getSelection();
        Cursor sdnCursor = loadSDN(context, profileAndContactsLoader);
        if (sdnCursor != null) {
            sdnContactCount = sdnCursor.getCount();
        }
        if (null != sdnCursor) {
            cursors.add(sdnCursor);
        }
        profileAndContactsLoader.setSelection(oldSelection);
        return sdnContactCount;
    }

    /**
     * For SIM contact there must be pass subId and sdnId, in case to draw sub
     * and sdn icons. Cursor cursor The contact cursor. String displayName
     * Contact display name. String lookupKey
     *
     */
    public static DefaultImageRequest getDefaultImageRequest(Cursor cursor, String displayName,
            String lookupKey, boolean circularPhotos) {
        DefaultImageRequest request = new DefaultImageRequest(displayName, lookupKey,
                circularPhotos);
        final int subId = cursor.getInt(cursor.getColumnIndexOrThrow(Contacts.INDICATE_PHONE_SIM));
        if (subId > 0) {
            request.subId = subId;
            request.photoId = getSdnPhotoId(cursor);
        }
        return request;
    }

    private static int getSdnPhotoId(Cursor cursor) {
        int sdnId = 0;
        int isSdnContact = cursor.getInt(cursor.getColumnIndexOrThrow(Contacts.IS_SDN_CONTACT));
        if (isSdnContact > 0) {
            sdnId = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_ID_SDN_LOCKED;
        }
        return sdnId;
    }
}
