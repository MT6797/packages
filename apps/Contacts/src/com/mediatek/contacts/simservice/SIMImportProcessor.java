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

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;

import com.android.contacts.common.GeoUtil;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.i18n.phonenumbers.AsYouTypeFormatter;
import com.android.i18n.phonenumbers.PhoneNumberUtil;

import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.SubContactsUtils.NamePhoneTypePair;
import com.mediatek.contacts.ext.IAasExtension;
import com.mediatek.contacts.model.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.simservice.SIMProcessorManager.ProcessorCompleteListener;
import com.mediatek.contacts.simservice.SIMServiceUtils.ServiceWorkData;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.ContactsGroupUtils.USIMGroup;
import com.mediatek.contacts.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class SIMImportProcessor extends SIMProcessorBase {
    private static final String TAG = "SIMImportProcessor";

    private static final String[] COLUMN_NAMES = new String[] { "index", "name", "number",
            "emails", "additionalNumber", "groupIds" };

    protected static final int INDEX_COLUMN = 0; // index in SIM
    protected static final int NAME_COLUMN = 1;
    protected static final int NUMBER_COLUMN = 2;
    protected static final int EMAIL_COLUMN = 3;
    protected static final int ADDITIONAL_NUMBER_COLUMN = 4;
    protected static final int GROUP_COLUMN = 5;

    // In order to prevent locking DB too long,
    // set the max operation count 90 in a batch.
    private static final int MAX_OP_COUNT_IN_ONE_BATCH = 90;

    private HashMap<Integer, Integer> mGroupIdMap;

    private int mSubId;
    private Context mContext;

    public SIMImportProcessor(Context context, int subId, Intent intent,
            ProcessorCompleteListener listener) {
        super(intent, listener);
        Log.i(TAG, "[SIMImportProcessor]new...subId = " + subId);
        mContext = context;
        mSubId = subId;
        mGroupIdMap = new HashMap<Integer, Integer>();
    }

    @Override
    public int getType() {
        return SIMServiceUtils.TYPE_IMPORT;
    }

    @Override
    public void doWork() {
        Log.i(TAG, "[dowork]Processor [subId =" + mSubId + "] running...Thread id="
                + Thread.currentThread().getId());
        if (isCancelled()) {
            Log.i(TAG, "[dowork]cancel import work. Thread id="
                    + Thread.currentThread().getId());
            return;
        }
        SIMServiceUtils.deleteSimContact(mContext, mSubId);
        if (isCancelled()) {
            Log.i(TAG, "[dowork]cancel import work after deleteSimContact. Thread id="
                    + Thread.currentThread().getId());
            return;
        }

        if (!checkPhbStateReady()) {
            // fixed issue that the USIM groups will appear again after enable
            // FDN
            SIMServiceUtils.sendFinishIntent(mContext, mSubId);
            Log.i(TAG, "[doWork]simStateReady is not ready, return!");
            return;
        }

        int simType = SimCardUtils.getSimTypeBySubId(mSubId);
        final Uri iccUri = SubInfoUtils.getIccProviderUri(mSubId);
        Cursor simCursor = querySimContact(mContext, mSubId, simType, iccUri);
        Log.i(TAG, "[dowork]simType = " + simType + ",simType =" + simType + ",mSubId = " + mSubId);
        importAllSimContacts(mContext, mSubId, simCursor, simType);
        if (simCursor != null) {
            simCursor.close();
        }
    }

    private void importAllSimContacts(Context context, int subId, Cursor simCursor, int simType) {
        Log.d(TAG, "[importAllSimContacts]subId:" + subId + ",sim type:" + simType);
        if (isCancelled()) {
            Log.d(TAG, "[importAllSimContacts]cancel import work,Thread id="
                    + Thread.currentThread().getId());
            return;
        }

        final ContentResolver resolver = context.getContentResolver();
        if (simCursor != null) {
            if (subId > 0) {
                synchronized (this) {
                    importAllSimContacts(context, simCursor, resolver, subId, simType, null, false);
                }
            }
            simCursor.close();

            if (SimCardUtils.isPhoneBookReady(subId)) {
                Cursor sdnCursor = null;
                final Uri iccSdnUri = SubInfoUtils.getIccProviderSdnUri(subId);
                Log.d(TAG, "[importAllSimContacts]iccSdnUri" + iccSdnUri);
                sdnCursor = resolver.query(iccSdnUri, COLUMN_NAMES, null, null, null);
                if (sdnCursor != null) {
                    Log.d(TAG,
                            "[importAllSimContacts]sdnCursor.getCount() = " + sdnCursor.getCount());
                    try {
                        if (sdnCursor.getCount() > 0) {
                            importAllSimContacts(context, sdnCursor, resolver, subId, simType,
                                    null, true);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "[importAllSimContacts]exception:" + e.toString());
                    } finally {
                        sdnCursor.close();
                    }
                }
            }
        }

        if (isCancelled()) {
            Log.i(TAG, "[ImportAllSimContactsThread] cancel.");
            return;
        }
        SIMServiceUtils.sendFinishIntent(context, subId);
    }

    private void importAllSimContacts(Context context, final Cursor cursor,
            final ContentResolver resolver, int subId, int simType, HashSet<Long> insertSimIdSet,
            boolean importSdnContacts) {
        Log.d(TAG, "[importAllSimContacts]subId = " + subId + ",simType = " + simType
                + ",importSdnContacts = " + importSdnContacts);
        if (isCancelled()) {
            Log.i(TAG, "[importAllSimContacts]cancel, Thread id="
                    + Thread.currentThread().getId());
            return;
        }

        AccountTypeManager atm = AccountTypeManager.getInstance(context);
        List<AccountWithDataSet> lac = atm.getAccounts(true);
        boolean isUsim = (simType == SIMServiceUtils.SIM_TYPE_USIM ||
                simType == SIMServiceUtils.SIM_TYPE_CSIM);

        int accountSubId = -1;
        AccountWithDataSetEx account = null;
        for (AccountWithDataSet accountData : lac) {
            if (accountData instanceof AccountWithDataSetEx) {
                AccountWithDataSetEx accountEx = (AccountWithDataSetEx) accountData;
                accountSubId = accountEx.getSubId();
                Log.i(TAG, "[importAllSimContacts]accountSubId =" + accountSubId);
                if (accountSubId == subId) {
                    int accountSimType = (AccountTypeUtils.ACCOUNT_TYPE_USIM.equals(accountEx.type))
                            ? SIMServiceUtils.SIM_TYPE_USIM : SIMServiceUtils.SIM_TYPE_SIM;
                    // UIM
                    if (accountEx.type.equals(AccountTypeUtils.ACCOUNT_TYPE_UIM)) {
                        accountSimType = SIMServiceUtils.SIM_TYPE_UIM;
                    } else if (accountEx.type.equals(AccountTypeUtils.ACCOUNT_TYPE_CSIM)) {
                        accountSimType = SIMServiceUtils.SIM_TYPE_CSIM;
                    }
                    if (accountSimType == simType) {
                        account = accountEx;
                        break;
                    }
                    break;
                }
            }
        }

        if (account == null) {
            // String accountName = isUsim ? "USIM" + slot : "SIM" + slot;
            // String accountType = isUsim ? AccountType.ACCOUNT_TYPE_USIM :
            // AccountType.ACCOUNT_TYPE_SIM;
            // TBD: use default sim name and sim type.
            Log.i(TAG, "[importAllSimContacts]account is null!");
        }

        final ArrayList<ContentProviderOperation> operationList =
                new ArrayList<ContentProviderOperation>();

        if (cursor != null) {
            cursor.moveToPosition(-1);
            // Bug Fix ALPS00289127:
            String countryCode = GeoUtil.getCurrentCountryIso(context);
            Log.i(TAG, "[importAllSimContacts] countryCode :" + countryCode);
            int i = 0;
            while (cursor.moveToNext()) {
                long indexInSim = cursor.getLong(INDEX_COLUMN); // index in SIM
                // Do nothing if sim contacts is already inserted into contacts
                // DB.
                if (insertSimIdSet != null && insertSimIdSet.contains(indexInSim)) {
                    Log.d(TAG, "[importAllSimContacts]sub id:" + subId + "||indexInSim:"
                            + indexInSim + "||isInserted is true,contine to do next.");
                    continue;
                }

                i = actuallyImportOneSimContact(context, cursor, resolver, subId, simType,
                        indexInSim, importSdnContacts, operationList, i, account, isUsim,
                        accountSubId, countryCode);

                if (i > MAX_OP_COUNT_IN_ONE_BATCH) {
                    try {
                        // TBD: The deleting and inserting of SIM contacts will
                        // be controled in the same operation queue in the
                        // future.
                        if (!SIMServiceUtils.checkPhoneBookState(subId)/*
                                                                        * !
                                                                        * SIMServiceUtils
                                                                        * .
                                                                        * checkSimState
                                                                        * (
                                                                        * context
                                                                        * .
                                                                        * getContentResolver
                                                                        * (),
                                                                        * slotId
                                                                        * )
                                                                        */) {
                            Log.d(TAG, "[importAllSimContacts]check sim State: false");
                            break;
                        }
                        Log.d(TAG, "[importAllSimContacts]Before applyBatch. ");
                        resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
                        Log.d(TAG, "[importAllSimContacts]After applyBatch ");
                    } catch (RemoteException e) {
                        Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                    } catch (OperationApplicationException e) {
                        Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                    }
                    i = 0;
                    operationList.clear();
                }
            }

            // fix CR ALPS00754984
            mGroupIdMap.clear();
            if (isCancelled()) {
                Log.d(TAG,
                        "[importAllSimContacts]cancel import work on after while{}. Thread id="
                                + Thread.currentThread().getId());
                return;
            }
            try {
                Log.d(TAG, "[importAllSimContacts]final,Before applyBatch ");
                if (SIMServiceUtils.checkPhoneBookState(subId)) {
                    Log.d(TAG, "[importSimContactcheck] sim State: true");
                    if (!operationList.isEmpty()) {
                        resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
                    }
                }
                Log.d(TAG, "[importAllSimContacts]final,After applyBatch ");
            } catch (RemoteException e) {
                Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            } catch (OperationApplicationException e) {
                Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            }
        }
    }

    private int actuallyImportOneSimContact(Context context, final Cursor cursor,
            final ContentResolver resolver, int subId, int simType, long indexInSim,
            boolean importSdnContacts, ArrayList<ContentProviderOperation> operationList,
            int loopCheck, AccountWithDataSetEx account, boolean isUsim, int accountSubId,
            String countryCode) {
        int i = loopCheck;
        if (isCancelled()) {
            Log.d(TAG, "[actuallyImportOneSimContact]cancel, Thread id="
                    + Thread.currentThread().getId());
            return i;
        }

        final NamePhoneTypePair namePhoneTypePair = new NamePhoneTypePair(
                cursor.getString(NAME_COLUMN));
        final String name = namePhoneTypePair.name;
        final int phoneType = namePhoneTypePair.phoneType;
        final String phoneTypeSuffix = namePhoneTypePair.phoneTypeSuffix;
        String phoneNumber = cursor.getString(NUMBER_COLUMN);
        Log.d(TAG, "indexInSim = " + indexInSim + ",phoneType = " + phoneType
                + ",phoneTypeSuffix" + phoneTypeSuffix + ",name = " + name + ",phoneNumber = "
                + phoneNumber);

        int j = 0;
        String accountType = null;

        ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(RawContacts.CONTENT_URI);
        ContentValues values = new ContentValues();

        if (account != null) {
            accountType = account.type;
            values.put(RawContacts.ACCOUNT_NAME, account.name);
            values.put(RawContacts.ACCOUNT_TYPE, account.type);
        }
        values.put(RawContacts.INDICATE_PHONE_SIM, subId);
        values.put(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DISABLED);
        values.put(RawContacts.INDEX_IN_SIM, indexInSim);

        if (importSdnContacts) {
            values.put(RawContacts.IS_SDN_CONTACT, 1);
        }

        builder.withValues(values);
        operationList.add(builder.build());
        j++;

        if (!TextUtils.isEmpty(phoneNumber)) {
            Log.d(TAG, "[actuallyImportOneSimContact] phoneNumber before : " + phoneNumber);
            AsYouTypeFormatter mFormatter = PhoneNumberUtil.getInstance().getAsYouTypeFormatter(
                    countryCode);
            char[] cha = phoneNumber.toCharArray();
            int ii = cha.length;
            for (int num = 0; num < ii; num++) {
                phoneNumber = mFormatter.inputDigit(cha[num]);
            }
            // / M: Op01 will format Number, filter some char
            phoneNumber = ExtensionManager.getInstance().getOp01Extension()
                    .formatNumber(phoneNumber, null);
            /** @} */
            Log.d(TAG, "[actuallyImportOneSimContact] phoneNumber after : " + phoneNumber);

            builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference(Phone.RAW_CONTACT_ID, i);
            builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
            // builder.withValue(Phone.TYPE, phoneType);

            builder.withValue(Data.DATA2, 2);
            // AAS primary number doesn't have type. [COMMD_FOR_AAS]
            ExtensionManager
                    .getInstance()
                    .getAasExtension()
                    .updateOperation(accountType, builder, cursor,
                            IAasExtension.TYPE_FOR_PHONE_NUMBER);

            builder.withValue(Phone.NUMBER, phoneNumber);
            if (!TextUtils.isEmpty(phoneTypeSuffix)) {
                builder.withValue(Data.DATA15, phoneTypeSuffix);
            }
            operationList.add(builder.build());
            j++;
        }

        if (!TextUtils.isEmpty(name)) {
            builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference(StructuredName.RAW_CONTACT_ID, i);
            builder.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
            builder.withValue(StructuredName.DISPLAY_NAME, name);
            operationList.add(builder.build());
            j++;
        }

        // if USIM
        if (isUsim) {
            j = importUSimPart(cursor, operationList, i, j, countryCode, accountType, accountSubId);
        }
        i = i + j;
        return i;
    }

    private int importUSimPart(final Cursor cursor,
            ArrayList<ContentProviderOperation> operationList, int loopCheck, int loop,
            String countryCode, String accountType, int accountSubId) {
        int i = loopCheck;
        int j = loop;
        ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(Data.CONTENT_URI);

        // insert USIM email
        final String emailAddresses = cursor.getString(EMAIL_COLUMN);
        Log.d(TAG, "[importUSimPart]import a USIM contact.emailAddresses:" + emailAddresses);
        if (!TextUtils.isEmpty(emailAddresses)) {
            final String[] emailAddressArray;
            emailAddressArray = emailAddresses.split(",");
            for (String emailAddress : emailAddressArray) {
                Log.d(TAG, "[actuallyImportOneSimContact]emailAddress IS " + emailAddress);
                if (!TextUtils.isEmpty(emailAddress) && !emailAddress.equals("null")) {
                    builder.withValueBackReference(Email.RAW_CONTACT_ID, i);
                    builder.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
                    builder.withValue(Email.TYPE, Email.TYPE_MOBILE);
                    builder.withValue(Email.DATA, emailAddress);
                    operationList.add(builder.build());
                    j++;
                }
            }
        }

        // insert USIM additional number
        String additionalNumber = cursor.getString(ADDITIONAL_NUMBER_COLUMN);
        Log.d(TAG, "[importUSimPart]additionalNumber:" + additionalNumber);
        if (!TextUtils.isEmpty(additionalNumber)) {
            Log.i(TAG, "[importUSimPart] additionalNumber before : " + additionalNumber);
            AsYouTypeFormatter mFormatter = PhoneNumberUtil.getInstance().getAsYouTypeFormatter(
                    countryCode);
            char[] cha = additionalNumber.toCharArray();
            int ii = cha.length;
            for (int num = 0; num < ii; num++) {
                additionalNumber = mFormatter.inputDigit(cha[num]);
            }
            // / M: Op01 will format Number, filter some char
            additionalNumber = ExtensionManager.getInstance().getOp01Extension()
                    .formatNumber(additionalNumber, null);
            /** @} */
            Log.i(TAG, "[importUSimPart] additionalNumber after : " + additionalNumber);
            builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference(Phone.RAW_CONTACT_ID, i);
            builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
            // builder.withValue(Phone.TYPE, phoneType);

            builder.withValue(Data.DATA2, 7);
            ExtensionManager
                    .getInstance()
                    .getAasExtension()
                    .updateOperation(accountType, builder, cursor,
                            IAasExtension.TYPE_FOR_ADDITIONAL_NUMBER);

            builder.withValue(Phone.NUMBER, additionalNumber);
            builder.withValue(Data.IS_ADDITIONAL_NUMBER, 1);
            operationList.add(builder.build());
            j++;
        }

        j += ExtensionManager.getInstance().getSneExtension()
                .importSimSne(operationList, cursor, i);

        // USIM group
        final String ugrpStr = cursor.getString(GROUP_COLUMN);
        Log.d(TAG, "[importUSimPart]sim group id string: " + ugrpStr);
        if (!TextUtils.isEmpty(ugrpStr)) {
            String[] ugrpIdArray = null;
            if (!TextUtils.isEmpty(ugrpStr)) {
                ugrpIdArray = ugrpStr.split(",");
            }
            for (String ugrpIdStr : ugrpIdArray) {
                int ugrpId = -1;
                try {
                    if (!TextUtils.isEmpty(ugrpIdStr)) {
                        ugrpId = Integer.parseInt(ugrpIdStr);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "[importUSimPart] catched exception");
                    e.printStackTrace();
                    continue;
                }
                Log.d(TAG, "[importUSimPart] sim group id ugrpId: " + ugrpId);
                if (ugrpId > 0) {
                    // / M: fix CR ALPS00754984
                    Integer grpId = mGroupIdMap.get(ugrpId);
                    Log.d(TAG, "[importUSimPart]simgroup mapping group grpId: " + grpId);
                    if (grpId == null) {
                        Log.e(TAG, "[USIM Group] Error. Catch unhandled "
                                + "SIM group error. ugrp: " + ugrpId);
                        continue;
                    }
                    builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                    builder.withValue(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
                    builder.withValue(GroupMembership.GROUP_ROW_ID, grpId);
                    builder.withValueBackReference(Phone.RAW_CONTACT_ID, i);
                    operationList.add(builder.build());
                    j++;
                }
            }
        }

        return j;
    }

    private boolean checkPhbStateReady() {
        boolean phbStateReady = SIMServiceUtils.checkPhoneBookState(mSubId);
        int i = 10;
        while (i > 0) {
            if (!phbStateReady) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    Log.w(TAG, "[checkPhbStateReady]excepiotn:" + e.toString());
                }
                phbStateReady = SIMServiceUtils.checkPhoneBookState(mSubId);
            } else {
                break;
            }
            i--;
        }
        return phbStateReady;
    }

    private Cursor querySimContact(Context context, int subId, int simType, Uri iccUri) {
        Log.d(TAG, "[querySimContact]subId:" + subId + "|simType:" + simType + ",iccUri = "
                + iccUri);
        if (isCancelled()) {
            Log.d(TAG, "[querySimContact]canceled,return.");
            return null;
        }

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(iccUri, COLUMN_NAMES, null, null, null);
        } catch (java.lang.NullPointerException e) {
            Log.e(TAG, "[querySimContact]exception:" + e.toString());
            return null;
        }
        if (cursor != null) {
            int count = cursor.getCount();
            Log.d(TAG, "[querySimContact]count:" + count);
        }

        if (simType == SIMServiceUtils.SIM_TYPE_USIM) {
            mGroupIdMap.clear();
            ServiceWorkData workData = new ServiceWorkData(subId, simType, cursor);
            USIMGroup.syncUSIMGroupContactsGroup(context, workData, mGroupIdMap);
        } else {
            USIMGroup.deleteUSIMGroupOnPhone(context, subId);
        }
        return cursor;
    }
}
