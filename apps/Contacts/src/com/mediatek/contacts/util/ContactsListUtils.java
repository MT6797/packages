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
import android.content.Intent;
import android.provider.ContactsContract.ProviderStatus;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.contacts.R;
import com.android.contacts.list.ContactsRequest;
import com.android.contacts.list.ProviderStatusWatcher;

import com.mediatek.contacts.widget.WaitCursorView;
/** define some util functions for contact/list */
public class ContactsListUtils {
    private static final String CONTACT_PHOTO = "contactPhoto";
    private static final int DISPLAYNAME_LENGTH = 18;

    /**
     * Change Feature.
     * CR ID: ALPS00111821. Descriptions: change layout.­
     */
    public static ContactsRequest setActionCodeForContentItemType(
            Intent intent, ContactsRequest request) {
        boolean getPhoto = intent.getBooleanExtra(CONTACT_PHOTO, false);
        if (!getPhoto) {
            request.setActionCode(ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT);
        } else {
            request.setActionCode(ContactsRequest.ACTION_PICK_CONTACT);
        }
        return request;
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
     * Bug Fix for ALPS00117275
     */
    public static String getBlurb(Context context, String displayName) {
        String blurb;
        if (displayName != null && displayName.length() > DISPLAYNAME_LENGTH) {
            String strTemp = displayName.subSequence(0, DISPLAYNAME_LENGTH).toString();
            strTemp = strTemp + "...";
            blurb = context.getString(R.string.blurbJoinContactDataWith, strTemp);
        } else {
            blurb = context.getString(R.string.blurbJoinContactDataWith, displayName);
        }
        return blurb;
    }

    /**
     * Bug Fix. CR ID: ALPS00115673 Descriptions: add wait cursor
     */
    public static boolean isNoAccountsNoContacts(Boolean destroyed,
            int providerStatus) {
        //return destroyed || providerStatus == ProviderStatus.STATUS_NO_ACCOUNTS_NO_CONTACTS;
        //TODO:STATUS_NO_ACCOUNTS_NO_CONTACTS is 4?
        return destroyed || providerStatus == 4;
    }
}