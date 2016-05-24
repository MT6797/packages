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
package com.mediatek.contacts.quickcontact;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents.Insert;
import android.telecom.PhoneAccount;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.contacts.R;
import com.android.contacts.common.GroupMetaData;
import com.android.contacts.common.model.Contact;
import com.android.contacts.common.util.Constants;
import com.android.contacts.quickcontact.ExpandingEntryCardView;
import com.android.contacts.quickcontact.ExpandingEntryCardView.Entry;
import com.android.contacts.quickcontact.ExpandingEntryCardView.EntryContextMenuInfo;

import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.ext.IViewCustomExtension.QuickContactCardViewCustom;
import com.mediatek.contacts.util.Log;
///
import java.util.ArrayList;
import java.util.List;

/**
 * Extract some util method in QuickContactActivity to this class.
 */
public class QuickContactUtils {
    private static String TAG = "QuickContactUtils";

    private static String sSipAddress = null;

    /**
     * Bug fix ALPS01747019.
     *
     * @param context
     *            Context
     * @param contactData
     *            Contact
     * @param aboutCardEntries
     *            List<List<Entry>>
     */
    public static void buildPhoneticNameToAboutEntry(Context context, Contact contactData,
            List<List<Entry>> aboutCardEntries) {
        // Phonetic name is not a data item, so the entry needs to be created separately
        final String phoneticName = contactData.getPhoneticName();
        Log.i(TAG, "[buildPhoneticNameToAboutEntry]phoneticName = " + phoneticName);
        if (!TextUtils.isEmpty(phoneticName)) {
            Entry phoneticEntry = new Entry(/* viewId = */ -1,
                    /* icon = */ null,
                    context.getResources().getString(R.string.name_phonetic),
                    phoneticName,
                    /* subHeaderIcon = */ null,
                    /* text = */ null,
                    /* textIcon = */ null,
                    /* primaryContentDescription = */ null,
                    /* intent = */ null,
                    /* alternateIcon = */ null,
                    /* alternateIntent = */ null,
                    /* alternateContentDescription = */ null,
                    /* shouldApplyColor = */ false,
                    /* isEditable = */ false,
                    /* EntryContextMenuInfo = */ new EntryContextMenuInfo(phoneticName,
                            context.getResources().getString(R.string.name_phonetic),
                    /* mimeType = */ null, /* id = */ -1, /* isPrimary = */ false),
                    /* thirdIcon = */ null,
                    /* thirdIntent = */ null,
                    /* thirdContentDescription = */ null,
                    /* iconResourceId = */ 0);
            List<Entry> phoneticList = new ArrayList<>();
            phoneticList.add(phoneticEntry);
            // Phonetic name comes after nickname. Check to see if the first entry type is nickname
            if (aboutCardEntries.size() > 0 && aboutCardEntries.get(0).get(0).getHeader().equals(
                    context.getResources().getString(R.string.header_nickname_entry))) {
                aboutCardEntries.add(1, phoneticList);
            } else {
                aboutCardEntries.add(0, phoneticList);
            }
        }
    }

    /**
     * Dial IP call.
     *
     * @param context
     *            Context
     * @param number
     *            String
     * @return true if send intent successfully, else false.
     */
    public static boolean dialIpCall(Context context, String number) {
        Log.i(TAG, "[dialIpCall]number = " + number);
        if (number == null) {
            return false;
        }
        Uri callUri = Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null);
        final Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, callUri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constants.EXTRA_IS_IP_DIAL, true);
        context.startActivity(intent);
        return true;
    }

    /**
     * Get group title based on the groupId.
     *
     * @param groupMetaData
     *            List<GroupMetaData>
     * @param groupId
     *            long
     * @return group title
     */
    public static String getGroupTitle(List<GroupMetaData> groupMetaData, long groupId) {
        if (groupMetaData == null) {
            Log.w(TAG, "[getGroupTitle]groupMetaData is null,return. ");
            return null;
        }

        for (GroupMetaData group : groupMetaData) {
            if (group.getGroupId() == groupId) {
                if (!group.isDefaultGroup() && !group.isFavorites()) {
                    String title = group.getTitle();
                    if (!TextUtils.isEmpty(title)) {
                        return title;
                    }
                }
                break;
            }
        }

        return null;
    }

    /**
     * Send this contact to bluetooth for print.
     *
     * @param context
     *            Context
     * @param contactData
     *            Contact
     * @return true if send PRINT action,false else.
     */
    public static boolean printContact(Context context, Contact contactData) {
        if (contactData == null) {
            Log.w(TAG, "[printContact]contactData is null,return. ");
            return false;
        }
        final String lookupKey = contactData.getLookupKey();
        final Uri shareUri = Uri.withAppendedPath(Contacts.CONTENT_VCARD_URI, lookupKey);
        final Intent intent = new Intent();
        intent.setAction("mediatek.intent.action.PRINT");
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setType(Contacts.CONTENT_VCARD_TYPE);
        intent.putExtra(Intent.EXTRA_STREAM, shareUri);

        try {
            context.startActivity(Intent.createChooser(intent,
                    context.getText(R.string.printContact)));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(context, R.string.no_way_to_print, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    // Bug fix for ALPS01907789 @{
    public static void setSipAddress(String address) {
        sSipAddress = address;
    }

    public static void resetSipAddress() {
        sSipAddress = null;
    }

    public static void addSipExtra(Intent intent) {
        if (intent != null && sSipAddress != null) {
            intent.putExtra(Insert.SIP_ADDRESS, sSipAddress);
        }
    }
    // @}
}
