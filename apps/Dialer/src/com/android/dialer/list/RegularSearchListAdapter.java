/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.dialer.list;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.android.contacts.common.CallUtil;
import com.android.contacts.common.list.DirectoryPartition;
import com.android.contacts.common.util.PhoneNumberHelper;
import com.android.dialer.calllog.ContactInfo;
import com.android.dialer.service.CachedNumberLookupService;
import com.android.dialer.service.CachedNumberLookupService.CachedContactInfo;
import com.mediatek.dialer.dialersearch.DialerSearchCursorLoader;

import com.mediatek.dialer.util.DialerFeatureOptions;

/**
 * List adapter to display regular search results.
 */
public class RegularSearchListAdapter extends DialerPhoneNumberListAdapter {
    private boolean mIsQuerySipAddress;

    public RegularSearchListAdapter(Context context) {
        super(context);
        setShortcutEnabled(SHORTCUT_CREATE_NEW_CONTACT, false);
        setShortcutEnabled(SHORTCUT_ADD_TO_EXISTING_CONTACT, false);
    }

    public CachedContactInfo getContactInfo(
            CachedNumberLookupService lookupService, int position) {
        ContactInfo info = new ContactInfo();
        CachedContactInfo cacheInfo = lookupService.buildCachedContactInfo(info);
        final Cursor item = (Cursor) getItem(position);
        if (item != null) {
            info.name = item.getString(PhoneQuery.DISPLAY_NAME);
            info.type = item.getInt(PhoneQuery.PHONE_TYPE);
            info.label = item.getString(PhoneQuery.PHONE_LABEL);
            info.number = item.getString(PhoneQuery.PHONE_NUMBER);
            final String photoUriStr = item.getString(PhoneQuery.PHOTO_URI);
            info.photoUri = photoUriStr == null ? null : Uri.parse(photoUriStr);

            cacheInfo.setLookupKey(item.getString(PhoneQuery.LOOKUP_KEY));

            final int partitionIndex = getPartitionForPosition(position);
            final DirectoryPartition partition =
                (DirectoryPartition) getPartition(partitionIndex);
            final long directoryId = partition.getDirectoryId();
            final String sourceName = partition.getLabel();
            if (isExtendedDirectory(directoryId)) {
                cacheInfo.setExtendedSource(sourceName, directoryId);
            } else {
                cacheInfo.setDirectorySource(sourceName, directoryId);
            }
        }
        return cacheInfo;
    }

    @Override
    public String getFormattedQueryString() {
        if (mIsQuerySipAddress) {
            // Return unnormalized SIP address
            return getQueryString();
        }
        return super.getFormattedQueryString();
    }

    @Override
    public void setQueryString(String queryString) {
        /** M: fix for ALPS01759137, set mFormattedQueryString in advance @{ */
        super.setQueryString(queryString);
        /** @} */
        // Don't show actions if the query string contains a letter.
        final boolean showNumberShortcuts = !TextUtils.isEmpty(getFormattedQueryString())
                && hasDigitsInQueryString();
        // Email addresses that could be SIP addresses are an exception.
        mIsQuerySipAddress = PhoneNumberHelper.isUriNumber(queryString);
        boolean changed = false;
        changed |= setShortcutEnabled(SHORTCUT_DIRECT_CALL,
                showNumberShortcuts || mIsQuerySipAddress);
        changed |= setShortcutEnabled(SHORTCUT_SEND_SMS_MESSAGE, showNumberShortcuts);
        /* M: using internal method instead of CallUtil.isVideoEnable*/
        changed |= setShortcutEnabled(SHORTCUT_MAKE_VIDEO_CALL,
                showNumberShortcuts && isVideoEnabled(queryString));
        /// M: [IMS Call] For Volte call @{
        boolean showImsCallItems = DialerFeatureOptions.isImsCallSupport() &&
                mIsQuerySipAddress;
        changed |= setShortcutEnabled(SHORTCUT_MAKE_IMS_CALL, showImsCallItems);
        changed |= setShortcutEnabled(SHORTCUT_ADD_TO_EXISTING_CONTACT, showImsCallItems);
        /// @}

        if (changed) {
            notifyDataSetChanged();
        }
//        super.setQueryString(queryString);
    }

    /**
     * Whether there is at least one digit in the query string.
     */
    private boolean hasDigitsInQueryString() {
        String queryString = getQueryString();
        int length = queryString.length();
        for (int i = 0; i < length; i++) {
            if (Character.isDigit(queryString.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * M: [MTK Dialer Search] Sets query for the DialerSearchCursorLoader.
     */
    public void configureLoader(DialerSearchCursorLoader loader) {
        if (getQueryString() == null) {
            loader.configureQuery("", false);
        } else {
            loader.configureQuery(getQueryString(), false);
        }
    }

    /**
     *  M: [MTK Dialer Search] phone number column index changed due to dialer search
     * @see com.android.contacts.common.list.PhoneNumberListAdapter#getPhoneNumber(int)
     */
    @Override
    public String getPhoneNumber(int position) {
        if (!DialerFeatureOptions.isDialerSearchEnabled()) {
            return super.getPhoneNumber(position);
        }

        Cursor cursor = ((Cursor) getItem(position));
        if (cursor != null) {
            String phoneNumber = cursor.getString(SEARCH_PHONE_NUMBER_INDEX);
            return phoneNumber;
        } else {
            return null;
        }
    }
}
