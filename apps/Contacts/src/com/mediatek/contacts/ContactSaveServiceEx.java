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
package com.mediatek.contacts;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Groups;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.widget.Toast;

import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.editor.ContactEditorFragment;
import com.android.contacts.activities.ContactEditorBaseActivity.ContactEditor.SaveMode;

import com.mediatek.contacts.util.ContactsGroupUtils;
import com.mediatek.contacts.util.ContactsGroupUtils.USIMGroupException;
import com.mediatek.contacts.util.Log;

import java.util.ArrayList;

public class ContactSaveServiceEx {
    private static final String TAG = "ContactSaveServiceEx";

    // / add to update group infos intent keys from groupeditorfragment@{
    public static final String EXTRA_SIM_INDEX_TO_ADD = "simIndexToAdd";
    public static final String EXTRA_SIM_INDEX_TO_REMOVE = "simIndexToRemove";
    public static final String EXTRA_ORIGINAL_GROUP_NAME = "originalGroupName";
    public static final String EXTRA_SIM_INDEX_ARRAY = "simIndexArray";
    public static final String EXTRA_SUB_ID = "subId";
    // add new group name as back item of GroupCreationDialogFragment
    public static final String EXTRA_NEW_GROUP_NAME = "addGroupName";
    // @}

    // :[Gemini+] all possible slot error can be safely put in this sparse int
    // array.
    private static SparseIntArray mSubIdError = new SparseIntArray();

    private static Handler mMainHandler = new Handler(Looper.getMainLooper());

    /**
     * Shows a toast on the UI thread.
     */
    private static void showToast(final int message) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ContactsApplicationEx.getContactsApplication(), message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * add usim group info to create group
     *
     * @param intent
     *            The intent used to create new group
     * @param label
     *            group label
     * @param simIndexArray
     *            sim index add to group
     * @param subId
     *            the sub id
     * @return the Intent contains usim info
     */
    public static void addIccForCreateNewGroupIntent(Intent intent, String label,
            final int[] simIndexArray, int subId) {
        intent.putExtra(EXTRA_SIM_INDEX_ARRAY, simIndexArray);
        intent.putExtra(EXTRA_SUB_ID, subId);
        Intent callbackIntent = intent.getParcelableExtra(ContactSaveService.EXTRA_CALLBACK_INTENT);
        callbackIntent.putExtra(EXTRA_SUB_ID, subId);
        callbackIntent.putExtra(EXTRA_NEW_GROUP_NAME, label);
    }

    /**
     * add Icc info for update usim group
     *
     * @param intent
     *            The intent used to update icc group
     * @param OriginalGroupName
     *            old group name
     * @param subId
     * @param simIndexToAddArray
     *            the sim index will add to this group
     * @param simIndexToRemoveArray
     *            the sim index will removed from this group
     * @param account
     *            group account
     */
    public static void addIccForGroupUpdateIntent(Intent intent, String OriginalGroupName,
            int subId, int[] simIndexToAddArray, int[] simIndexToRemoveArray,
            AccountWithDataSet account) {
        intent.putExtra(EXTRA_SUB_ID, subId);
        intent.putExtra(EXTRA_SIM_INDEX_TO_ADD, simIndexToAddArray);
        intent.putExtra(EXTRA_SIM_INDEX_TO_REMOVE, simIndexToRemoveArray);
        intent.putExtra(EXTRA_ORIGINAL_GROUP_NAME, OriginalGroupName);
        intent.putExtra(ContactSaveService.EXTRA_ACCOUNT_TYPE, account.type);
        intent.putExtra(ContactSaveService.EXTRA_ACCOUNT_NAME, account.name);
        intent.putExtra(ContactSaveService.EXTRA_DATA_SET, account.dataSet);
        Intent callbackIntent = intent.getParcelableExtra(ContactSaveService.EXTRA_CALLBACK_INTENT);
        callbackIntent.putExtra(EXTRA_SUB_ID, subId);
    }

    /**
     * add Icc info for delete usim group
     *
     * @param intent
     *            The intent used to delete group
     * @param subId
     *            sub id
     * @param groupLabel
     *            the gorup name to be delete
     */
    public static void addIccForGroupDeletionIntent(Intent intent, int subId, String groupLabel) {
        intent.putExtra(EXTRA_SUB_ID, subId);
        intent.putExtra(ContactSaveService.EXTRA_GROUP_LABEL, groupLabel);
    }

    /**
     * M: delete group in icc card,like usim
     *
     * @param intent
     * @param groupId
     * @return true if success,false else.
     */
    public static boolean deleteGroupInIcc(ContactSaveService contactSaveService, Intent intent,
            long groupId) {
        String groupLabel = intent.getStringExtra(contactSaveService.EXTRA_GROUP_LABEL);
        int subId = intent.getIntExtra(EXTRA_SUB_ID, -1);

        if (subId <= 0 || TextUtils.isEmpty(groupLabel)) {
            Log.w(TAG, "[deleteGroupInIcc] subId:" + subId + ",groupLabel:" + groupLabel
                    + " have errors");
            return false;
        }

        // check whether group exists
        int ugrpId = -1;
        try {
            ugrpId = ContactsGroupUtils.USIMGroup.hasExistGroup(subId, groupLabel);
            Log.d(TAG, "[deleteGroupInIcc]ugrpId:" + ugrpId);
        } catch (RemoteException e) {
            e.printStackTrace();
            ugrpId = -1;
        }
        if (ugrpId > 0) {
            // fix ALPS01002380. should not use groupLabel for groupuri,because
            // groupname "/"
            // will lead to SQLite exception.
            Uri groupUri = ContentUris.withAppendedId(Contacts.CONTENT_GROUP_URI, groupId);
            Cursor c = contactSaveService.getContentResolver().query(groupUri,
                    new String[] { Contacts._ID, Contacts.INDEX_IN_SIM },
                    Contacts.INDICATE_PHONE_SIM + " = " + subId, null, null);
            Log.d(TAG, "[deleteGroupInIcc]simId:" + subId + "|member count:"
                    + (c == null ? "null" : c.getCount()));
            try {
                while (c != null && c.moveToNext()) {
                    int indexInSim = c.getInt(1);
                    boolean ret = ContactsGroupUtils.USIMGroup.deleteUSIMGroupMember(subId,
                            indexInSim, ugrpId);
                    Log.d(
                            TAG,
                            "[deleteGroupInIcc]subId:" + subId + "ugrpId:" + ugrpId + "|simIndex:"
                                    + indexInSim + "|Result:" + ret + " | contactid : "
                                    + c.getLong(0));
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
            // Delete USIM group
            int error = ContactsGroupUtils.USIMGroup.deleteUSIMGroup(subId, groupLabel);
            Log.d(TAG, "[deleteGroupInIcc]error:" + error);
            if (error != 0) {
                showToast(R.string.delete_group_failure);
                return false;
            }
        }
        return true;
    }

    public static int updateGroupToIcc(ContactSaveService contactSaveService, Intent intent) {
        int[] simIndexToAddArray = intent.getIntArrayExtra(EXTRA_SIM_INDEX_TO_ADD);
        int[] simIndexToRemoveArray = intent.getIntArrayExtra(EXTRA_SIM_INDEX_TO_REMOVE);
        int subId = intent.getIntExtra(EXTRA_SUB_ID, -1);
        String originalName = intent.getStringExtra(EXTRA_ORIGINAL_GROUP_NAME);
        String accountType = intent.getStringExtra(ContactSaveService.EXTRA_ACCOUNT_TYPE);
        String accountName = intent.getStringExtra(ContactSaveService.EXTRA_ACCOUNT_NAME);
        String groupName = intent.getStringExtra(ContactSaveService.EXTRA_GROUP_LABEL);
        long groupId = intent.getLongExtra(ContactSaveService.EXTRA_GROUP_ID, -1);
        int groupIdInIcc = -1;

        if (subId < 0) {
            Log.w(TAG, "[updateGroupToIcc] subId is error.subId:" + subId);
            return groupIdInIcc;
        }

        Log.d(TAG, "[updateGroupToIcc]groupName:" + groupName + " |groupId:" + groupId
                + "|originalName:" + originalName + " |subId:" + subId + " |accountName:"
                + accountName + " |accountType:" + accountType);

        try {
            groupIdInIcc = ContactsGroupUtils.USIMGroup.syncUSIMGroupUpdate(subId, originalName,
                    groupName);
        } catch (RemoteException e) {
            Log.e(TAG, "[updateGroupToIcc]e : " + e);
        } catch (USIMGroupException e) {
            Log.e(
                    TAG,
                    "[updateGroupToIcc] catched USIMGroupException." + " ErrorType: "
                            + e.getErrorType());
            mSubIdError.put(e.getErrorSubId(), e.getErrorType());
            checkAllSlotErrors();
            Intent callbackIntent = intent
                    .getParcelableExtra(ContactSaveService.EXTRA_CALLBACK_INTENT);
            if (e.getErrorType() == USIMGroupException.GROUP_NAME_OUT_OF_BOUND) {
                callbackIntent.putExtra(ContactEditorFragment.SAVE_MODE_EXTRA_KEY, SaveMode.RELOAD);
            }

            contactSaveService.deliverCallback(callbackIntent);
            return groupIdInIcc;
        }
        if (groupIdInIcc <= 0) {

            Intent callbackIntent = intent
                    .getParcelableExtra(ContactSaveService.EXTRA_CALLBACK_INTENT);
            Log.d(TAG, ContactSaveService.EXTRA_CALLBACK_INTENT);

            Log.d(TAG, "[updateGroupToIcc]groupIdInIcc = " + groupIdInIcc
                    + ",callback intent:" + ContactSaveService.EXTRA_CALLBACK_INTENT);
            contactSaveService.deliverCallback(callbackIntent);
        }
        return groupIdInIcc;
    }

    public static int createGroupToIcc(ContactSaveService contactSaveService, Intent intent) {
        String accountType = intent.getStringExtra(ContactSaveService.EXTRA_ACCOUNT_TYPE);
        String accountName = intent.getStringExtra(ContactSaveService.EXTRA_ACCOUNT_NAME);
        String groupName = intent.getStringExtra(ContactSaveService.EXTRA_GROUP_LABEL);
        int groupIdInIcc = -1;

        int subId = intent.getIntExtra(EXTRA_SUB_ID, -1);
        if (subId <= 0) {
            Log.w(TAG, "[createGroupToIcc] subId error..subId:" + subId);
            return groupIdInIcc;
        }

        try {
            groupIdInIcc = ContactsGroupUtils.USIMGroup.syncUSIMGroupNewIfMissing(subId, groupName);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (USIMGroupException e) {
            Log.w(TAG, "[createGroupToIcc] ceate grop fail type:" + e.getErrorType()
                    + ",fail subId:" + e.getErrorSubId());
            mSubIdError.put(e.getErrorSubId(), e.getErrorType());
            checkAllSlotErrors();
            Intent callbackIntent = intent
                    .getParcelableExtra(ContactSaveService.EXTRA_CALLBACK_INTENT);
            if (e.getErrorType() == USIMGroupException.GROUP_NAME_OUT_OF_BOUND) {
                callbackIntent.putExtra(ContactEditorFragment.SAVE_MODE_EXTRA_KEY, SaveMode.RELOAD);
            }
            contactSaveService.deliverCallback(callbackIntent);
        }
        return groupIdInIcc;
    }

    public static boolean checkGroupNameExist(ContactSaveService saveService, String groupName,
            String accountName, String accountType, boolean showTips) {
        boolean nameExists = false;

        if (TextUtils.isEmpty(groupName)) {
            if (showTips) {
                showToast(R.string.name_needed);
            }
            return false;
        }
        Cursor cursor = saveService.getContentResolver().query(
                Groups.CONTENT_SUMMARY_URI,
                new String[] { Groups._ID },
                Groups.TITLE + "=? AND " + Groups.ACCOUNT_NAME + " =? AND " + Groups.ACCOUNT_TYPE
                        + "=? AND " + Groups.DELETED + "=0",
                new String[] { groupName, accountName, accountType }, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                nameExists = true;
            }
            cursor.close();
        }
        // If group name exists, make a toast and return false.
        if (nameExists) {
            if (showTips) {
                showToast(R.string.group_name_exists);
            }
            return false;
        } else {
            return true;
        }
    }

    /**
     * [Gemini+] check all slot to find whether is there any error happened
     */
    public static void checkAllSlotErrors() {
        for (int i = 0; i < mSubIdError.size(); i++) {
            int subId = mSubIdError.keyAt(i);
            int errorCode = mSubIdError.valueAt(i);
            showMoveUSIMGroupErrorToast(errorCode, subId);
        }
        mSubIdError.clear();
    }

    public static void showMoveUSIMGroupErrorToast(int errCode, int subId) {
        Log.d(TAG, "[showMoveUSIMGroupErrorToast]errCode:" + errCode + "|subId:" + subId);
        /** Bug Fix for CR ALPS00451441 @{ */
        String toastMsg;
        if (errCode == USIMGroupException.GROUP_GENERIC_ERROR) {
            toastMsg = ContactsApplicationEx.getContactsApplication().getString(
                    R.string.save_group_fail);
        } else {
            toastMsg = ContactsApplicationEx.getContactsApplication().getString(
                    ContactsGroupUtils.USIMGroupException.getErrorToastId(errCode));
        }
        /** @} */
        final String msg = toastMsg;
        if (toastMsg != null) {
            Log.d(TAG, "[showMoveUSIMGroupErrorToast]toastMsg:" + toastMsg);
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ContactsApplicationEx.getContactsApplication(), msg,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * fix ALPS00272729
     *
     * @param operations
     * @param resolver
     */
    public static void bufferOperations(ArrayList<ContentProviderOperation> operations,
            ContentResolver resolver) {
        try {
            Log.d(TAG, "[bufferOperatation] begin applyBatch ");
            resolver.applyBatch(ContactsContract.AUTHORITY, operations);
            Log.d(TAG, "[bufferOperatation] end applyBatch");
            operations.clear();
        } catch (RemoteException e) {
            Log.e(TAG, "[bufferOperatation]RemoteException:", e);
            showToast(R.string.contactSavedErrorToast);
        } catch (OperationApplicationException e) {
            Log.e(TAG, "[bufferOperatation]OperationApplicationException:", e);
            showToast(R.string.contactSavedErrorToast);
        }
    }
}
