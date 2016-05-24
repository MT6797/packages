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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.widget.Toast;

import com.android.contacts.R;
import com.android.contacts.activities.PeopleActivity;
import com.android.contacts.common.vcard.VCardCommonArguments;

import com.mediatek.contacts.list.MultiBasePickerAdapter.PickListItemCache.PickListItemData;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.MtkToast;

public class MultiSharePickerFragment extends MultiBasePickerFragment {
    private static final String TAG = "MultiSharePickerFragment";
    /**
     * M: change for CR ALPS00779683. Binder on one process can use 2M memory,
     * but there has 16 binder thread, every binder thread can't put data large
     * than 128KB (2M/16). Origin MAX_DATA_SIZE = 400000; So change shareUri max
     * data size less 124KB.
     */
    private static final int MAX_DATA_SIZE = 124 * 1024;

    /**
     * The max multi choice count limit for share. The Cursor window allocates
     * 2M bytes memory for each client. If the data size is very big, the cursor
     * window would not allocate the memory for Cursor.moveWindow. To avoid
     * malicious operations, we only allow user to handle 1000 items.
     */
    public static final int MULTI_CHOICE_MAX_COUNT_FOR_SHARE = 1000;

    @Override
    public void onOptionAction() {
        final int selectedCount = getCheckedItemIds().length;
        if (selectedCount == 0) {
            Log.w(TAG, "[onOptionAction]selectedCount = 0");
            Toast.makeText(getContext(), R.string.multichoice_no_select_alert, Toast.LENGTH_SHORT)
                    .show();
            return;
        } else if (selectedCount > getMultiChoiceLimitCount()) {
            Log.w(TAG,
                    "[onOptionAction]selectedCount > getMultiChoiceLimitCount,selectedCount = "
                            + selectedCount);
            String msg = getResources().getString(R.string.share_contacts_limit,
                    getMultiChoiceLimitCount());
            MtkToast.toast(getActivity().getApplicationContext(), msg);
            return;
        }

        final String[] uriArray = getLoopUriArray();
        final Intent retIntent = new Intent();
        retIntent.putExtra(RESULT_INTENT_EXTRA_NAME, uriArray);
        boolean result = doShareVisibleContacts("Multi_Contact", null, uriArray);
        if (result) {
            getActivity().setResult(Activity.RESULT_OK, retIntent);
            getActivity().finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private boolean doShareVisibleContacts(String type, Uri uri, String[] idArrayUriLookUp) {
        if (idArrayUriLookUp == null || idArrayUriLookUp.length == 0) {
            Log.w(TAG, "[doShareVisibleContacts]error, return,idArrayUriLookUp = "
                    + idArrayUriLookUp);
            return true;
        }

        StringBuilder uriListBuilder = new StringBuilder();
        int index = 0;
        for (int i = 0; i < idArrayUriLookUp.length; i++) {
            if (index != 0) {
                uriListBuilder.append(":");
            }
            // find lookup key
            uriListBuilder.append(idArrayUriLookUp[i]);
            index++;
        }
        int dataSize = uriListBuilder.length();
        Uri shareUri = Uri.withAppendedPath(Contacts.CONTENT_MULTI_VCARD_URI,
                Uri.encode(uriListBuilder.toString()));
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(Contacts.CONTENT_VCARD_TYPE);
        intent.putExtra(Intent.EXTRA_STREAM, shareUri);
        intent.putExtra(VCardCommonArguments.ARG_CALLING_ACTIVITY, PeopleActivity.class.getName());

        /**
         * M: Bug Fix for CR: ALPS00395378 @{ Original Code:
         * intent.putExtra("LOOKUPURIS", uriListBuilder.toString());
         */
        /** @} M: Bug fix for CR: ALPS00395378 */

        // Launch chooser to share contact via
        final CharSequence chooseTitle = getText(R.string.share_via);
        final Intent chooseIntent = Intent.createChooser(intent, chooseTitle);

        try {
            Log.i(TAG, "[doShareVisibleContacts] dataSize : " + dataSize);
            if (dataSize < MAX_DATA_SIZE) {
                startActivity(chooseIntent);
                return true;
            } else {
                Toast.makeText(getContext(), R.string.share_too_large, Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (ActivityNotFoundException ex) {
            Log.w(TAG, "[doShareVisibleContacts]ActivityNotFoundException = " + ex);
            Toast.makeText(getContext(), R.string.share_error, Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    public Uri getUri() {
        final long[] checkArray = getCheckedItemIds();
        final int selectedCount = checkArray.length;
        if (selectedCount == 0) {
            Log.w(TAG, "[getUri]selectedCount = 0");
            Toast.makeText(getContext(), R.string.multichoice_no_select_alert, Toast.LENGTH_SHORT)
                    .show();
            return null;
        } else if (selectedCount > getMultiChoiceLimitCount()) {
            Log.w(TAG, "[getUri]selectedCount > getMultiChoiceLimitCount,selectedCount = "
                    + selectedCount);
            String msg = getResources().getString(R.string.share_contacts_limit,
                    getMultiChoiceLimitCount());
            MtkToast.toast(getActivity().getApplicationContext(), msg);
            return null;
        }

        final String[] uriArray = getLoopUriArray();

        StringBuilder uriListBuilder = new StringBuilder();
        boolean isFirstItem = true;
        for (String uri : uriArray) {
            if (isFirstItem) {
                isFirstItem = false;
            } else {
                uriListBuilder.append(":");
            }
            // find lookup key
            uriListBuilder.append(uri);
        }
        Uri shareUri = Uri.withAppendedPath(Contacts.CONTENT_MULTI_VCARD_URI,
                Uri.encode(uriListBuilder.toString()));

        return shareUri;
    }

    private String[] getLoopUriArray() {
        final long[] checkArray = getCheckedItemIds();
        final int selectedCount = checkArray.length;
        final MultiBasePickerAdapter adapter =
                (MultiBasePickerAdapter) getAdapter();

        int curArray = 0;
        String[] uriArray = new String[selectedCount];

        for (long id : checkArray) {
            if (curArray > selectedCount) {
                break;
            }
            PickListItemData item = adapter.getListItemCache().getItemData(id);
            if (item != null) {
                uriArray[curArray++] = item.lookupUri;
            } else {
                Log.e(TAG,
                        "[getLoopUriArray]the item is null. may some error happend.curArray:"
                                + curArray + ",id:" + id + ",checkArray.length:" + selectedCount
                                + ",ListViewCheckedCount:" + getListView().getCheckedItemCount());
            }
        }

        return uriArray;
    }

    @Override
    public int getMultiChoiceLimitCount() {
        return MULTI_CHOICE_MAX_COUNT_FOR_SHARE;
    }
}
