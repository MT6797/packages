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

import android.accounts.Account;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;

import com.android.contacts.R;
import com.android.contacts.activities.ContactEditorActivity;
import com.android.contacts.common.model.RawContactDelta;

import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.SubContactsUtils;
import com.mediatek.contacts.SubContactsUtils.NamePhoneTypePair;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SlotUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.simservice.SIMProcessorManager.ProcessorCompleteListener;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.ContactsGroupUtils.USIMGroup;
import com.mediatek.contacts.util.ContactsGroupUtils.USIMGroupException;
import com.mediatek.contacts.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SIMEditProcessor extends SIMProcessorBase {
    private static final String TAG = "SIMEditProcessor";

    private static final int MODE_DEFAULT = 0;
    private static final int MODE_INSERT = 1;
    private static final int MODE_EDIT = 2;

    public static final String RESULT = "result";
    public static final int RESULT_CANCELED = 0;
    public static final int RESULT_OK = 1;
    public static final int RESULT_NO_DATA = 2;
    public static final int DEFAULT_ID = -1;

    public static final String EDIT_SIM_ACTION = "com.mediatek.contacts.simservice.EDIT_SIM";

    private static final String KEY_NEED_QUIT_EDIT = "isQuitEdit";
    private static final String KEY_SIM_DATA = "simData";
    private static final String KEY_OLD_SIM_DATA = "oldsimData";

    private static final String SIM_NUM_PATTERN = "[+]?[[0-9][*#pw,;]]+[[0-9][*#pw,;]]*";
    private static final String USIM_EMAIL_PATTERN =
            "[[0-9][a-z][A-Z][_]][[0-9][a-z][A-Z][-_.]]*@[[0-9][a-z][A-Z][-_.]]+";
    private static final String[] ADDRESS_BOOK_COLUMN_NAMES = new String[] { "name", "number",
            "emails", "additionalNumber", "groupIds" };

    private static String sName = null;
    private static String sEmail = null;
    private static String sPhone = null;
    private static String sOtherPhone = null;
    private static String sAfterPhone = null;
    private static String sAfterOtherPhone = null;

    private Context mContext;
    private Intent mIntent = null;
    private Uri mLookupUri = null;

    private Account mAccount = null;

    HashMap<Long, String> mGroupAddList = new HashMap<Long, String>();
    HashMap<Long, String> mOldGroupAddList = new HashMap<Long, String>();

    private ArrayList<RawContactDelta> mSimData = new ArrayList<RawContactDelta>();
    private ArrayList<RawContactDelta> mSimOldData = new ArrayList<RawContactDelta>();

    private String mAccountType = null;
    private String mAccountName = null;
    private String mOldName = null;
    private String mOldPhone = null;
    private String mOldOtherPhone = null;
    private String mOldEmail = null;
    private String mUpdateName = null;
    private String mUpdatephone = null;
    private String mUpdatemail = null;
    private String mUpdateAdditionalNumber = null;
    private String mSimType = SimCardUtils.SimType.SIM_TYPE_USIM_TAG;
    private String mSimTypeTag = "UNKNOWN";

    private int mGroupNum = 0;
    private long mSlotId = SlotUtils.getNonSlotId();
    private int mSubId = SubInfoUtils.getInvalidSubId();
    private int mSaveMode = MODE_DEFAULT;
    private int mSaveFailToastStrId = -1;
    private int mContactId = 0;

    private long mIndicate = RawContacts.INDICATE_PHONE;
    private int mIndexInSim = -1;
    private long mRawContactId = -1;

    private boolean mPhbReady = false;
    private boolean mDoublePhoneNumber = false;
    private boolean mNumberInvalid = false;
    private boolean mQuitEdit = false;
    private boolean mNumberIsNull = false;
    private boolean mFixNumberInvalid = false;
    private boolean mAllEditorInvalid = false;

    private static List<Listener> sListeners = new ArrayList<Listener>();

    private static Map<Listener, Handler> sListenerHolder = new HashMap<Listener, Handler>();

    public interface Listener {
        public void onSIMEditCompleted(Intent callbackIntent);
    }

    public static void registerListener(Listener listener, Handler handler) {
        if (!(listener instanceof Activity)) {
            throw new ClassCastException("Only activities can be registered to"
                    + " receive callback from " + SIMProcessorService.class.getName());
        }
        Log.d(TAG, "[registerListener]listener added to SIMEditProcessor: " + listener);
        sListeners.add(listener);
        sListenerHolder.put(listener, handler);
        Log.d(TAG, "sListenerHolder = " + listener.hashCode() + " mHandler = "
                        + handler.hashCode());
    }

    public static void unregisterListener(Listener listener) {
        Log.d(TAG, "[unregisterListener]listener removed from SIMEditProcessor: " + listener);
        Handler handler = sListenerHolder.get(listener);
        if (handler != null) {
            Log.d(TAG,
                    "handler = " + handler.hashCode() + " listener = " + listener.hashCode());
            handler = null;
            sListeners.remove(listener);
            sListenerHolder.remove(listener);
            listener = null;
        }
    }

    // / fix ALPS01028420.register a handler for sim edit processor again
    public static boolean isNeedRegisterHandlerAgain(Handler handler) {
        Log.d(TAG, "[isNeedRegisterHandlerAgain] handler: " + handler);
        for (Listener listener : sListeners) {
            if (handler.equals(sListenerHolder.get(listener))) {
                return false;
            }
        }
        return true;
    }

    public SIMEditProcessor(Context context, int subId, Intent intent,
            ProcessorCompleteListener listener) {
        super(intent, listener);
        mContext = context;
        mSubId = subId;
        mIntent = intent;
        Log.i(TAG, "[SIMEditProcessor]new mSubId = " + mSubId);
    }

    @Override
    public int getType() {
        return SIMServiceUtils.SERVICE_WORK_EDIT;
    }

    @Override
    public void doWork() {
        if (isCancelled()) {
            Log.w(TAG, "[dowork]cancel remove work. Thread id = "
                    + Thread.currentThread().getId());
            return;
        }
        // if PHB is not ready, no need do other check, just return and show error message.
        if (isPhbReady()) {
            mQuitEdit = false;
        } else {
            Log.w(TAG, "[dowork] phb not ready, need quit eidtor UI");
            mQuitEdit = true;
            setSaveFailToastText();
            deliverCallbackOnError();
            return;
        }

        mSimData = mIntent.getParcelableArrayListExtra(KEY_SIM_DATA);
        mSimOldData = mIntent.getParcelableArrayListExtra(KEY_OLD_SIM_DATA);
        mAccountType = mSimData.get(0).getValues().getAsString(RawContacts.ACCOUNT_TYPE);

        if (mAccountType.equals(AccountTypeUtils.ACCOUNT_TYPE_USIM)
                || mAccountType.equals(AccountTypeUtils.ACCOUNT_TYPE_CSIM)) {
            mGroupNum = mIntent.getIntExtra("groupNum", 0);
            Log.i(TAG, "[dowork]groupNum : " + mGroupNum);
        }

        // TBD, need to do refactoring: how to avoid null point exception?
        mAccountName = mSimData.get(0).getValues().getAsString(RawContacts.ACCOUNT_NAME);
        if (mAccountType != null && mAccountName != null) {
            mAccount = new Account(mAccountName, mAccountType);
        } else {
            Log.w(TAG, "[dowork]accountType :" + mAccountType + "accountName:" + mAccountName);
            mQuitEdit = true;
            deliverCallbackOnError();
            return;
        }

        mIndicate = mIntent.getIntExtra(RawContacts.INDICATE_PHONE_SIM, RawContacts.INDICATE_PHONE);
        mIndexInSim = mIntent.getIntExtra("simIndex", -1);
        // 1 for new contact, 2 for existing contact
        mSaveMode = mIntent.getIntExtra("simSaveMode", MODE_DEFAULT);
        mLookupUri = mIntent.getData();
        Log.d(TAG, "[dowork]the mSubId is =" + mSubId + " the mIndicate is =" + mIndicate
                + " the mSaveMode = " + mSaveMode + " the accounttype is = " + mAccountType
                + " the uri is  = " + mLookupUri + " | mIndexInSim : " + mIndexInSim);

        // The UIM and SIM type has same property, CSIM and USIM also same.
        // So in this process, we set there has only two type 'SIM' and 'USIM'
        // Notice that, the type is not account type.
        mSimTypeTag = SimCardUtils.getSimTypeTagBySubId(mSubId);
        if (SimCardUtils.isUsimOrCsimType(mSubId)) {
            mSimType = SimCardUtils.SimType.SIM_TYPE_USIM_TAG;
        } else {
            mSimType = SimCardUtils.SimType.SIM_TYPE_SIM_TAG;
        }
        Log.d(TAG, "[doWork]mSimType:" + mSimType);
        // ALPS02050655:init static value first.
        initStaticValues();

        // the kind number
        int kindCount = mSimData.get(0).getContentValues().size();
        Log.d(TAG, "[dowork]kindCount: " + kindCount);

        String[] additionalNumberBuffer = new String[2];
        String[] numberBuffer = new String[2];
        long[] groupBuffer = new long[mGroupNum];

        getRawContactDataFromIntent(kindCount, additionalNumberBuffer, numberBuffer, groupBuffer);

        // Put group id and groupName to hashmap
        if (mAccountType.equals(AccountTypeUtils.ACCOUNT_TYPE_USIM)) {
            setGroupFromIntent(groupBuffer);
        }
        // if user chose two "mobile" phone type
        if ((!TextUtils.isEmpty(additionalNumberBuffer[1]))
                || (!TextUtils.isEmpty(numberBuffer[1]))) {
            mDoublePhoneNumber = true;
            Log.w(TAG, "[dowork] double phone number, two mobile phone type, return");
            setSaveFailToastText();
            deliverCallbackAndBackToFragment();
            return;
        } else {
            sOtherPhone = additionalNumberBuffer[0];
            sPhone = numberBuffer[0];
        }

        Log.d(TAG, "[dowork]the sName is = " + sName + " the sPhone is =" + sPhone
                + " the sOtherPhone is = " + sOtherPhone + "the email is =" + sEmail);

        if (isPhoneNumberInvaild()) {
            Log.w(TAG, "[dowork] Phone Number Invaild, need return");
            mNumberInvalid = true;
            setSaveFailToastText();
            deliverCallbackAndBackToFragment();
            return;
        }

        boolean hasImported = SIMServiceUtils.isServiceRunning(mSubId);
        // check hasSimContactsImported in ContactsUtils
        if (hasImported) {
            Log.i(TAG, "[dowork]hasImported,return.");
            showToastMessage(R.string.msg_loading_sim_contacts_toast, null);
            deliverCallbackOnError();
            return;
        }

        saveSimContact(mSaveMode);
        Log.d(TAG, "[dowork]the sName is = " + sName + ",the sPhone is =" + sPhone
                + " the buffer[] is " + additionalNumberBuffer[0] + ",the sOtherPhone is = "
                + sOtherPhone + ",the email is =" + sEmail);
    }

    private boolean isPhoneNumberInvaild() {
        Log.d(TAG, "[isPhoneNumberInvaild]initial phone number:" + sPhone);
        sAfterPhone = sPhone;
        if (!TextUtils.isEmpty(sPhone)) {
            if (PhoneNumberUtils.isUriNumber(sPhone)) {
                Log.e(TAG, "input phone number is invaild sPhone = " + sPhone);
                return true;
            }
            sAfterPhone = PhoneNumberUtils.stripSeparators(sPhone);
            if (!Pattern.matches(SIM_NUM_PATTERN,
                    PhoneNumberUtils.extractCLIRPortion(sAfterPhone))) {
                return true;
            }
        }

        Log.d(TAG, "[isPhoneNumberInvaild]initial sOtherPhone number:" + sOtherPhone);
        sAfterOtherPhone = sOtherPhone;
        if (!TextUtils.isEmpty(sOtherPhone)) {
            if (PhoneNumberUtils.isUriNumber(sOtherPhone)) {
                Log.e(TAG, "input other phone number is invaild sOtherPhone = " + sOtherPhone);
                return true;
            }
            sAfterOtherPhone = PhoneNumberUtils.stripSeparators(sOtherPhone);
            if (!Pattern.matches(SIM_NUM_PATTERN,
                    PhoneNumberUtils.extractCLIRPortion(sAfterOtherPhone))) {
                return true;
            }
        }

        return false;
    }

    private void getRawContactDataFromIntent(int kindCount, String[] additionalNumberBuffer,
            String[] numberBuffer, long[] groupBuffer) {
        Log.d(TAG, "[getRawContactDataFromIntent]...");

        int additionalNumberIndex = 0;
        int groupIndex = 0;
        int numberIndex = 0;
        String mimeType = null;
        String data = null;
        for (int countIndex = 0; countIndex < kindCount; countIndex++) {
            mimeType = mSimData.get(0).getContentValues().get(countIndex)
                    .getAsString(Data.MIMETYPE);
            data = mSimData.get(0).getContentValues().get(countIndex).getAsString(Data.DATA1);
            Log.d(TAG, "[getRawContactDataFromIntent]countIndex:" + countIndex + ",mimeType:"
                    + mimeType + "data:" + data);

            if (StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)) {
                sName = data;
            } else if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                // AAS, store AAS index(use Data.DATA3).[COMMD_FOR_AAS]
                {
                    // TBD: 7?
                    if ("7".equals(mSimData.get(0).getContentValues().get(countIndex).getAsString(
                           Data.DATA2))
                            || ExtensionManager.getInstance().getAasExtension().checkAasEntry(
                                            mSimData.get(0).getContentValues().get(countIndex))) {
                        additionalNumberBuffer[additionalNumberIndex] = data;
                        additionalNumberIndex++;
                    } else {
                        numberBuffer[numberIndex] = data;
                        numberIndex++;
                    }
                }
            } else if (Email.CONTENT_ITEM_TYPE.equals(mimeType)) {
                sEmail = data;
            } else if (GroupMembership.CONTENT_ITEM_TYPE.equals(mimeType)
                    && (groupBuffer.length > groupIndex)) {
                // make sure that the bufferGroup exists used to store data.
                groupBuffer[groupIndex] = mSimData.get(0).getContentValues().get(countIndex)
                        .getAsLong(Data.DATA1);
                groupIndex++;
            }
        }
    }

    private void setGroupFromIntent(long[] groupBuffer) {
        String[] groupNames = new String[mGroupNum];
        long[] groupIds = new long[mGroupNum];
        groupNames = mIntent.getStringArrayExtra("groupName");
        groupIds = mIntent.getLongArrayExtra("groupId");
        Log.d(TAG, "[setGroupFromIntent]groupBuffer len :" + groupBuffer.length);

        for (int groupBufferIndex = 0; groupBufferIndex < groupBuffer.length; groupBufferIndex++) {
            for (int grNum = 0; grNum < mGroupNum; grNum++) {
                if (groupBuffer[groupBufferIndex] == groupIds[grNum]) {
                    String groupName = groupNames[grNum];
                    long groupId = groupBuffer[groupBufferIndex];
                    mGroupAddList.put(groupId, groupName);
                }
            }
        }
    }

    /**
     * removing white space characters and '-' characters from the beginning and
     * end of the string.
     *
     * @param number
     *            always additional number
     * @return
     */
    private String replaceCharOnNumber(String number) {
        String trimNumber = number;
        if (!TextUtils.isEmpty(trimNumber)) {
            Log.d(TAG, "[replaceCharOnNumber]befor replaceall number : " + trimNumber);
            trimNumber = trimNumber.replaceAll("-", "");
            trimNumber = trimNumber.replaceAll(" ", "");
            Log.d(TAG, "[replaceCharOnNumber]after replaceall number : " + trimNumber);
        }
        return trimNumber;
    }

    private void setSaveFailToastText() {
        mSaveFailToastStrId = -1;
        if (!mPhbReady) {
            mSaveFailToastStrId = R.string.icc_phone_book_invalid;
            mQuitEdit = true;
        } /*
           * else if (mAirPlaneModeOn) { mSaveFailToastStrId =
           * R.string.AirPlane_mode_on; mAirPlaneModeOn = false; mQuitEdit =
           * true; } else if (mFDNEnabled) { mSaveFailToastStrId =
           * R.string.FDNEnabled; mFDNEnabled = false; mQuitEdit = true; } else
           * if (mSIMInvalid) { mSaveFailToastStrId = R.string.sim_invalid;
           * mSIMInvalid = false; mQuitEdit = true; }
           */
        else if (mNumberIsNull) {
            mSaveFailToastStrId = R.string.cannot_insert_null_number;
            mNumberIsNull = false;
        } else if (mNumberInvalid) {
            mSaveFailToastStrId = R.string.sim_invalid_number;
            mNumberInvalid = false;
        } else if (mFixNumberInvalid) {
            mSaveFailToastStrId = R.string.sim_invalid_fix_number;
            mFixNumberInvalid = false;
        } else if (mDoublePhoneNumber) {
            mSaveFailToastStrId = R.string.has_double_phone_number;
            mDoublePhoneNumber = false;
        } else if (mAllEditorInvalid) {
            mSaveFailToastStrId = R.string.contactSavedErrorToast;
            mAllEditorInvalid = false;
        }
        Log.i(TAG, "[setSaveFailToastText]mSaveFailToastStrId is:" + mSaveFailToastStrId
                + ",mPhbReady:" + mPhbReady);
        if (mSaveFailToastStrId >= 0) {
            if (mSaveFailToastStrId == R.string.err_icc_no_phone_book) {
                String specialErrorText = mContext.getResources().getString(mSaveFailToastStrId,
                        mSimTypeTag);
                showToastMessage(DEFAULT_ID, specialErrorText);
            } else {
                showToastMessage(mSaveFailToastStrId, null);
            }
        }
    }

    private boolean checkIfSaveFail(Uri checkUri) {
        if (checkUri != null && "error".equals(checkUri.getPathSegments().get(0))) {
            mSaveFailToastStrId = -1;
            if ("-1".equals(checkUri.getPathSegments().get(1))) {
                mSaveFailToastStrId = R.string.number_too_long;
            } else if ("-2".equals(checkUri.getPathSegments().get(1))) {
                mSaveFailToastStrId = R.string.name_too_long;
            } else if ("-3".equals(checkUri.getPathSegments().get(1))) {
                mSaveFailToastStrId = R.string.storage_full;
                mQuitEdit = true;
            } else if ("-6".equals(checkUri.getPathSegments().get(1))) {
                mSaveFailToastStrId = R.string.fix_number_too_long;
            } else if ("-10".equals(checkUri.getPathSegments().get(1))) {
                mSaveFailToastStrId = R.string.generic_failure;
                mQuitEdit = true;
            } else if ("-11".equals(checkUri.getPathSegments().get(1))) {
                mSaveFailToastStrId = R.string.err_icc_no_phone_book;
                mQuitEdit = true;
            } else if ("-12".equals(checkUri.getPathSegments().get(1))) {
                mSaveFailToastStrId = R.string.error_save_usim_contact_email_lost;
            } else if ("-13".equals(checkUri.getPathSegments().get(1))) {
                mSaveFailToastStrId = R.string.email_too_long;
            } else if ("0".equals(checkUri.getPathSegments().get(1))) {
                mSaveFailToastStrId = R.string.fail_reason_unknown;
                mQuitEdit = true;
            }
            Log.i(TAG, "setSaveFailToastText uri,mSaveFailToastStrId is:"
                    + mSaveFailToastStrId);
            if (mSaveFailToastStrId >= 0) {
                if (mSaveFailToastStrId == R.string.err_icc_no_phone_book) {
                    String specialErrorText = mContext.getResources().getString(
                            mSaveFailToastStrId, mSimTypeTag);
                    showToastMessage(DEFAULT_ID, specialErrorText);
                } else {
                    showToastMessage(mSaveFailToastStrId, null);
                }
                return true;
            }
            return false;
        }
        return !(checkUri != null);
    }

    private boolean updateFailToastText(int result) {
        mSaveFailToastStrId = -1;
        if (result == -1) {
            mSaveFailToastStrId = R.string.number_too_long;
        } else if (result == -2) {
            mSaveFailToastStrId = R.string.name_too_long;
        } else if (result == -3) {
            mSaveFailToastStrId = R.string.storage_full;
            mQuitEdit = true;
        } else if (result == -6) {
            mSaveFailToastStrId = R.string.fix_number_too_long;
        } else if (result == -10) {
            mSaveFailToastStrId = R.string.generic_failure;
            mQuitEdit = true;
        } else if (result == -11) {
            mSaveFailToastStrId = R.string.err_icc_no_phone_book;
            mQuitEdit = true;
        } else if (result == -12) {
            mSaveFailToastStrId = R.string.error_save_usim_contact_email_lost;
        } else if (result == -13) {
            mSaveFailToastStrId = R.string.email_too_long;
        }
        if (mSaveFailToastStrId >= 0) {
            showToastMessage(mSaveFailToastStrId, null);
            return true;
        }
        return false;
    }

    private void showResultToastText(int errorType) {
        Log.i(TAG, "[showResultToastText]errorType :" + errorType);

        String toastMsg = null;
        if (errorType == -1) {
            toastMsg = mContext.getString(R.string.contactSavedToast);
        } else {
            toastMsg = mContext.getString(USIMGroupException.getErrorToastId(errorType));
        }

        if (errorType == -1) {
            if (compareData()) {
                Log.i(TAG, "[showResultToastText]saved compareData = true ,return.");
                return;
            }
        }
        Log.d(TAG, "[showResultToastText]showToastMessage default.");
        showToastMessage(DEFAULT_ID, toastMsg);

    }

    private boolean isContactEditable() {
        Intent intent = mIntent;
        ContentResolver resolver = mContext.getContentResolver();
        Uri uri = mLookupUri;
        if (isRawContactIdInvalid(intent, resolver, uri)) {
            showToastMessage(R.string.icc_phone_book_invalid, null);
            deliverCallbackOnError();
            Log.d(TAG, "[isContactEditable]isRawContactIdInvalid is true,return false.");
            return false;
        }
        // /M:fix CR ALPS01065879,sim Info Manager API Remove
        SubscriptionInfo subscriptionInfo = SubInfoUtils.getSubInfoUsingSubId((int) mIndicate);
        if (subscriptionInfo == null) {
            mSlotId = -1;
        } else {
            mSlotId = subscriptionInfo.getSimSlotIndex();
        }
        ArrayList<Long> oldbufferGroup = new ArrayList<Long>();
        setOldRawContactData(oldbufferGroup);
        Log.i(TAG, "the mIndicate: " + mIndicate + " | the mSlotId : " + mSlotId);
        // put group id and title to hasmap
        if (mAccountType.equals(AccountTypeUtils.ACCOUNT_TYPE_USIM)) {
            setOldGroupAddList(intent, oldbufferGroup);
        }

        return true;
    }

    private void setOldGroupAddList(Intent intent, ArrayList<Long> oldbufferGroup) {
        String[] groupName = new String[mGroupNum];
        long[] groupId = new long[mGroupNum];
        Log.i(TAG, "[getOldGroupAddList]oldbufferGroup.size() : " + oldbufferGroup.size());
        groupName = intent.getStringArrayExtra("groupName");
        groupId = intent.getLongArrayExtra("groupId");
        for (int groupIndex = 0; groupIndex < oldbufferGroup.size(); groupIndex++) {
            for (int grNum = 0; grNum < mGroupNum; grNum++) {
                if (oldbufferGroup.get(groupIndex) == groupId[grNum]) {
                    String title = groupName[grNum];
                    long groupid = oldbufferGroup.get(groupIndex);
                    mOldGroupAddList.put(groupid, title);
                }
            }
        }
    }

    private void setOldRawContactData(ArrayList<Long> oldbufferGroup) {
        int oldCount = mSimOldData.get(0).getContentValues().size();
        String mimeType = null;
        String data = null;

        for (int oldIndex = 0; oldIndex < oldCount; oldIndex++) {
            mimeType = mSimOldData.get(0).getContentValues().get(oldIndex)
                    .getAsString(Data.MIMETYPE);
            data = mSimOldData.get(0).getContentValues().get(oldIndex).getAsString(Data.DATA1);
            Log.d(TAG, "[setOldRawContactData]Data.MIMETYPE: " + mimeType + ",data:" + data);

            if (StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)) {
                mOldName = mSimOldData.get(0).getContentValues().get(oldIndex)
                        .getAsString(Data.DATA1);
            } else if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                {
                    if (mSimOldData.get(0).getContentValues().get(oldIndex).getAsString(Data.DATA2)
                        .equals("7") || ExtensionManager.getInstance().getAasExtension()
                        .checkAasEntry(mSimOldData.get(0).getContentValues().get(oldIndex))) { //7?
                        mOldOtherPhone = data;
                    } else {
                        mOldPhone = data;
                    }
                }
            } else if (Email.CONTENT_ITEM_TYPE.equals(mimeType)) {
                mOldEmail = data;
            } else if (GroupMembership.CONTENT_ITEM_TYPE.equals(mimeType)) {
                Log.d(TAG, "[setOldRawContactData] oldIndex = " + oldIndex);
                oldbufferGroup.add(mSimOldData.get(0).getContentValues().get(oldIndex)
                        .getAsLong(Data.DATA1));
            }
        }

        Log.d(TAG, "[setOldRawContactData]The mOldName is: " + mOldName + " ,mOldOtherPhone: "
                + mOldOtherPhone + " ,mOldPhone: " + mOldPhone + " ,mOldEmail: " + mOldEmail);
    }

    private boolean isRawContactIdInvalid(Intent intent, ContentResolver resolver, Uri uri) {
        final String authority = uri.getAuthority();
        final String mimeType = intent.resolveType(resolver);

        if (ContactsContract.AUTHORITY.equals(authority)) {
            if (Contacts.CONTENT_ITEM_TYPE.equals(mimeType)) {
                mRawContactId = SubContactsUtils.queryForRawContactId(resolver,
                        ContentUris.parseId(uri));
            } else if (RawContacts.CONTENT_ITEM_TYPE.equals(mimeType)) {
                mRawContactId = ContentUris.parseId(uri);
            }
        }

        Log.d(TAG, "[isRawContactIdInvalid]authority:" + authority + ",mimeType:" + mimeType
                + ",mRawContactId:" + mRawContactId);

        if (mRawContactId < 1) {
            return true;
        }

        return false;
    }

    private void saveSimContact(int mode) {
        Log.d(TAG, "[saveSimContact]mode: " + mode);

        if (mode == MODE_EDIT) {
            if (mLookupUri != null) {
                boolean editable = isContactEditable();
                if (!editable) {
                    Log.i(TAG, "[dowork]isContactEditable is false ,return.");
                    return;
                }
            } else {
                Log.i(TAG, "[dowork]mLookupUri is null,return.");
                deliverCallbackOnError();
                return;
            }
        }

        Uri checkUri = null;
        int result = 0;
        final ContentResolver resolver = mContext.getContentResolver();
        mUpdateName = sName;
        mUpdatephone = sAfterPhone;
        mUpdatephone = replaceCharOnNumber(mUpdatephone);

        ContentValues values = new ContentValues();
        values.put("tag", TextUtils.isEmpty(mUpdateName) ? "" : mUpdateName);
        values.put("number", TextUtils.isEmpty(mUpdatephone) ? "" : mUpdatephone);

        if (SimCardUtils.SimType.SIM_TYPE_USIM_TAG.equals(mSimType)) {
            updateUSIMSpecValues(values);
        }

        if (mode == MODE_INSERT) {
            Log.i(TAG, "[saveSimContact]mode is MODE_INSERT");
            insertSIMContact(resolver, values);
        } else if (mode == MODE_EDIT) {
            Log.i(TAG, "[saveSimContact]mode is MODE_EDIT");
            editSIMContact(resolver);
        }
    }

    private void insertSIMContact(final ContentResolver resolver, ContentValues values) {
        Uri checkUri;
        if (isInsertValuesInvalid()) {
            Log.w(TAG, "[insertSIMContact]isInsertValuesInvalid is true,return.");
            deliverCallbackAndBackToFragment();
            return;
        }

        // AAS, Set all anr into usim and db. here we assume anr as
        // "anr","anr2","anr3".., and so as aas.[COMMD_FOR_AAS]
        ExtensionManager.getInstance().getAasExtension().updateValues(mIntent, mSubId, values);
        ExtensionManager.getInstance().getSneExtension().updateValues(mIntent, mSubId, values);

        Log.i(TAG, "[insertSIMContact]insert to SIM card---");
        checkUri = resolver.insert(SubInfoUtils.getIccProviderUri(mSubId), values);
        Log.i(TAG, "[insertSIMContact]values is: " + values + ",checkUri is: " + checkUri);

        if (checkIfSaveFail(checkUri)) {
            Log.w(TAG, "[insertSIMContact]checkIfSaveFail is true,return.");
            deliverCallbackAndBackToFragment();
            return;
        }
        // index in SIM
        long indexFromUri = ContentUris.parseId(checkUri);
        Log.i(TAG, "[insertSIMContact]insert to db,mSimType :" + mSimType);

        // USIM group begin, only USIM card can insert Group, not contained CSIM
        int errorType = -1;
        if (SimCardUtils.isUsimType(mSubId)) {
            errorType = insertGroupToUSIMCard(indexFromUri, errorType);
        }
        // USIM group end

        Uri lookupUri = SubContactsUtils.insertToDB(mAccount, mUpdateName, sPhone, mUpdatemail,
                sOtherPhone, resolver, mIndicate, mSimType, indexFromUri, mGroupAddList.keySet());
        if (lookupUri != null) {
            ExtensionManager.getInstance().getSneExtension()
                .editSimSne(mIntent, indexFromUri, mSubId, ContentUris.parseId(lookupUri));
        } else {
            Log.e(TAG, "[insertSIMContact]insert fail, lookupUri = null");
            deliverCallbackOnError();
            return;
        }

        Log.d(TAG, "[insertSIMContact]errorType: " + errorType);
        showResultToastText(errorType);
        if (errorType == -1) {
            deliverCallback(lookupUri);
        } else {
            deliverCallbackOnError();
        }
    }

    private void editSIMContact(final ContentResolver resolver) {
        int result;
        ContentValues updatevalues = new ContentValues();
        setUpdateValues(updatevalues);
        Cursor cursor = null;

        setContactId();

        Log.d(TAG, "origianl name: " + mUpdateName);
        if (!TextUtils.isEmpty(mUpdateName)) {
            NamePhoneTypePair pair = new NamePhoneTypePair(mUpdateName);
            mUpdateName = pair.name;
            Log.d(TAG, "fixed name: " + mUpdateName);
        }
        Log.d(TAG, "[editSIMContact]mSimType:" + mSimType);
        if (SimCardUtils.SimType.SIM_TYPE_SIM_TAG.equals(mSimType)) {
            if (isSIMUpdateValuesInvalid()) {
                Log.w(TAG, "[editSIMContact]isSIMUpdateValuesInvalid is true,return.");
                deliverCallbackAndBackToFragment();
                return;
            }
        } else if (SimCardUtils.SimType.SIM_TYPE_USIM_TAG.equals(mSimType)) {
            if (isUSIMUpdateValuesInvalid()) {
                Log.w(TAG, "[editSIMContact]isUSIMUpdateValuesInvalid is true,return.");
                deliverCallbackAndBackToFragment();
                return;
            }
        }
        // AAS, Set all anr into usim and db. here we assume anr as
        // "anr","anr2","anr3".., and so as aas.[COMMD_FOR_AAS]
        ExtensionManager.getInstance().getAasExtension()
                .updateValues(mIntent, mSubId, updatevalues);
        ExtensionManager.getInstance().getSneExtension()
                .updateValues(mIntent, mSubId, updatevalues);
        // query phone book to load contacts to cache for update

        cursor = resolver.query(SubInfoUtils.getIccProviderUri(mSubId), ADDRESS_BOOK_COLUMN_NAMES,
                null, null, null);
        /** @ } */
        if (cursor != null) {
            try {
                result = resolver.update(SubInfoUtils.getIccProviderUri(mSubId), updatevalues,
                        null, null);
                Log.d(TAG, "[editSIMContact]result:" + result);
                if (updateFailToastText(result)) {
                    Log.i(TAG, "[editSIMContact]updateFailToastText,return.");
                    deliverCallbackAndBackToFragment();
                    return;
                }
            } finally {
                cursor.close();
            }
        }

        updateNameToDB(resolver);
        updatePhoneNumberToDB(resolver);
        int errorType = -1;
        if (SimCardUtils.SimType.SIM_TYPE_USIM_TAG.equals(mSimType)) {
            updateEmail(resolver);
            updateAdditionalNumberToDB(resolver);
            errorType = upateGroup(resolver, errorType);
            ExtensionManager.getInstance().getSneExtension()
                    .editSimSne(mIntent, mIndexInSim, mSubId, mRawContactId);
        }
        showResultToastText(errorType);

        Log.d(TAG, "[editSIMContact]errorType :" + errorType);
        if (errorType == -1) {
            // / Bug fix for ALPS02015883 @{
            final Uri rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI,
                    mRawContactId);
            Uri lookupUri = RawContacts.getContactLookupUri(resolver, rawContactUri);
            deliverCallback(lookupUri);
            // / @}
        } else {
            deliverCallbackOnError();
        }
    }

    private void setContactId() {
        Cursor c = mContext.getContentResolver().query(RawContacts.CONTENT_URI,
                new String[] { RawContacts.CONTACT_ID }, RawContacts._ID + "=" + mRawContactId,
                null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                mContactId = c.getInt(0);
                Log.d(TAG, "[setContactId]contactId:" + mContactId);
            }
            c.close();
        }
    }

    private void setUpdateValues(ContentValues updatevalues) {
        mUpdateAdditionalNumber = sAfterOtherPhone;
        if (!TextUtils.isEmpty(mUpdateAdditionalNumber)) {
            Log.d(TAG, "[setUpdateValues] befor replaceall mUpdateAdditionalNumber : "
                    + mUpdateAdditionalNumber);

            mUpdateAdditionalNumber = mUpdateAdditionalNumber.replaceAll("-", "");
            mUpdateAdditionalNumber = mUpdateAdditionalNumber.replaceAll(" ", "");
            Log.d(TAG, "[setUpdateValues] after replaceall mUpdateAdditionalNumber : "
                    + mUpdateAdditionalNumber);
        }
        // to comment old values for index in SIM
        updatevalues.put("newTag", TextUtils.isEmpty(mUpdateName) ? "" : mUpdateName);
        updatevalues.put("newNumber", TextUtils.isEmpty(mUpdatephone) ? "" : mUpdatephone);
        updatevalues.put("newAnr", TextUtils.isEmpty(mUpdateAdditionalNumber) ? ""
                : mUpdateAdditionalNumber);

        updatevalues.put("newEmails", TextUtils.isEmpty(mUpdatemail) ? "" : mUpdatemail);

        updatevalues.put("index", mIndexInSim);
        Log.d(TAG, "[setUpdateValues]updatevalues: " + updatevalues);
    }

    private boolean isSIMUpdateValuesInvalid() {
        if (TextUtils.isEmpty(mUpdateName) && TextUtils.isEmpty(mUpdatephone)) {
            /*
            // if name and number is null, delete this contact
            String where;
            Uri iccUriForSim = SubInfoUtils.getIccProviderUri(mSubId);
            // empty name and phone number
            // use the new 'where' for index in SIM
            where = "index = " + mIndexInSim;
            Log.d(TAG, "[isSIMUpdateValuesInvalid]where:" + where + ",iccUriForSim:"
                    + iccUriForSim);
            int deleteDone = mContext.getContentResolver().delete(iccUriForSim, where, null);
            Log.i(TAG, "[isSIMUpdateValuesInvalid]deleteDone result:" + deleteDone);
            if (deleteDone == 1) {
                Uri deleteUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, mContactId);
                int deleteDB = mContext.getContentResolver().delete(deleteUri, null, null);
                Log.i(TAG, "[isSIMUpdateValuesInvalid]deleteDB result: " + deleteDB);
            }
            */
            mAllEditorInvalid = true;
        } else if (!TextUtils.isEmpty(mUpdatephone)) {
            if (!Pattern.matches(SIM_NUM_PATTERN,
                    PhoneNumberUtils.extractCLIRPortion(mUpdatephone))) {
                mNumberInvalid = true;
            }
        }

        Log.i(TAG, "[isSIMUpdateValuesInvalid]mNumberInvalid:" + mNumberInvalid
                + ", mAllEditorInvalid:" + mAllEditorInvalid);

        setSaveFailToastText();
        if (mSaveFailToastStrId >= 0) {
            Log.i(TAG, "[isSIMUpdateValuesInvalid]setSaveFailToastText,return true.");
            return true;
        }
        return false;
    }

    private boolean isUSIMUpdateValuesInvalid() {
        // if all items are empty, delete this contact
        if (TextUtils.isEmpty(mUpdatephone)
                && TextUtils.isEmpty(mUpdateName)
                && TextUtils.isEmpty(mUpdatemail)
                && TextUtils.isEmpty(mUpdateAdditionalNumber)
                && mGroupAddList.isEmpty()
                && (ExtensionManager.getInstance().getSneExtension()
                        .checkNickName(this, mIntent, false, mSubId) == 1)) {//sne is empty
            /*
            String where;
            Uri iccUriForUsim = SubInfoUtils.getIccProviderUri(mSubId);
            // use the new 'where' for index in SIM
            where = "index = " + mIndexInSim;
            Log.d(TAG, "[isUSIMUpdateValuesInvalid]where :" + where + ",iccUriForUsim:"
                    + iccUriForUsim);

            int deleteDone = mContext.getContentResolver().delete(iccUriForUsim, where, null);
            Log.d(TAG, "[isUSIMUpdateValuesInvalid]deleteDone result: " + deleteDone);
            if (deleteDone == 1) {
                Uri deleteUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, mContactId);
                int deleteDB = mContext.getContentResolver().delete(deleteUri, null, null);
                Log.d(TAG, "[isUSIMUpdateValuesInvalid]deleteDB result:" + deleteDB);
            }
            */
            mAllEditorInvalid = true;
        } else if (TextUtils.isEmpty(mUpdatephone)
                && TextUtils.isEmpty(mUpdateName)
                && (!TextUtils.isEmpty(mUpdatemail) || !TextUtils.isEmpty(mUpdateAdditionalNumber)
                        || !mGroupAddList.isEmpty() || !mOldGroupAddList.isEmpty() ||
                        (ExtensionManager.getInstance().getSneExtension()
                        .checkNickName(this, mIntent, false, mSubId) != 1))) {//sne is empty
            mNumberIsNull = true;
        } else if (!TextUtils.isEmpty(mUpdatephone)) {
            if (!Pattern.matches(SIM_NUM_PATTERN,
                    PhoneNumberUtils.extractCLIRPortion(mUpdatephone))) {
                mNumberInvalid = true;
            }
        }
        if (!TextUtils.isEmpty(mUpdateAdditionalNumber)) {
            if (!Pattern.matches(SIM_NUM_PATTERN,
                    PhoneNumberUtils.extractCLIRPortion(mUpdateAdditionalNumber))) {
                mFixNumberInvalid = true;
            }
        }
        Log.i(TAG, "mNumberIsNull:" + mNumberIsNull + ",mNumberInvalid:" + mNumberInvalid
                + ", mFixNumberInvalid:" + mFixNumberInvalid
                + ", mAllEditorInvalid:" + mAllEditorInvalid);

        setSaveFailToastText();
        if (mSaveFailToastStrId >= 0) {
            Log.i(TAG, "[editSIMContact]setSaveFailToastText,return true.");
            return true;
        }

        // after all check,default should be false,mean valid
        boolean invalid = false;
        // current sne may too long
        invalid = ExtensionManager.getInstance().getSneExtension()
                .checkNickName(this, mIntent, true, mSubId) == 2;
        return invalid;
    }

    private void updateNameToDB(final ContentResolver resolver) {
        ContentValues namevalues = new ContentValues();
        String wherename = Data.RAW_CONTACT_ID + " = \'" + mRawContactId + "\'" + " AND "
                + Data.MIMETYPE + "='" + StructuredName.CONTENT_ITEM_TYPE + "'";
        Log.d(TAG, "[updateNameToDB]wherename:" + wherename + ",mUpdateName:" + mUpdateName);

        // update name
        if (!TextUtils.isEmpty(mUpdateName)) {
            if (!TextUtils.isEmpty(mOldName)) {
                namevalues.put(StructuredName.DISPLAY_NAME, mUpdateName);
                /** Bug Fix for ALPS00370295 @{ */
                namevalues.putNull(StructuredName.GIVEN_NAME);
                namevalues.putNull(StructuredName.FAMILY_NAME);
                namevalues.putNull(StructuredName.PREFIX);
                namevalues.putNull(StructuredName.MIDDLE_NAME);
                namevalues.putNull(StructuredName.SUFFIX);
                /** @} */
                int upRet = resolver.update(Data.CONTENT_URI, namevalues, wherename, null);
                Log.d(TAG, "[updateNameToDB]update ret:" + upRet);
            } else {
                namevalues.put(StructuredName.RAW_CONTACT_ID, mRawContactId);
                namevalues.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
                namevalues.put(StructuredName.DISPLAY_NAME, mUpdateName);
                Uri insertRetUri = resolver.insert(Data.CONTENT_URI, namevalues);
                Log.d(TAG, "[updateNameToDB]uri insert ret:" + insertRetUri);
            }
        } else {
            // update name is null,delete name row
            int delRet = resolver.delete(Data.CONTENT_URI, wherename, null);
            Log.d(TAG, "[updateNameToDB]delete ret:" + delRet);
        }
    }

    private void updatePhoneNumberToDB(final ContentResolver resolver) {
        // update number
        ContentValues phonevalues = new ContentValues();
        String wherephone = Data.RAW_CONTACT_ID + " = \'" + mRawContactId + "\'" + " AND "
                + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'" + " AND "
                + Data.IS_ADDITIONAL_NUMBER + "= 0";
        Log.d(TAG, "[updatePhoneNumberToDB]wherephone:" + wherephone + ",mOldPhone:"
                + mOldPhone + ",mUpdatephone:" + sPhone);

        if (!TextUtils.isEmpty(mUpdatephone)) {
            if (!TextUtils.isEmpty(mOldPhone)) {
                phonevalues.put(Phone.NUMBER, sPhone);
                int upRet = resolver.update(Data.CONTENT_URI, phonevalues, wherephone, null);
                Log.i(TAG, "[updatePhoneNumberToDB]update ret:" + upRet);
            } else {
                phonevalues.put(Phone.RAW_CONTACT_ID, mRawContactId);
                phonevalues.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                phonevalues.put(Phone.NUMBER, mUpdatephone);
                phonevalues.put(Data.IS_ADDITIONAL_NUMBER, 0);
                phonevalues.put(Phone.TYPE, 2);

                Uri insertRet = resolver.insert(Data.CONTENT_URI, phonevalues);
                Log.i(TAG, "[updatePhoneNumberToDB]Uri insert ret:" + insertRet);
            }
        } else {
            int delRet = resolver.delete(Data.CONTENT_URI, wherephone, null);
            Log.i(TAG, "[updatePhoneNumberToDB]delete ret: " + delRet);
        }
    }

    private void updateAdditionalNumberToDB(final ContentResolver resolver) {
        // the logic of plguin & host write AdditionalNumber is not same
        if (ExtensionManager.getInstance().getAasExtension()
                .updateAdditionalNumberToDB(mIntent, mRawContactId)) {
            Log.i(TAG, "updateAdditionalNumberToDB(),handle by plugin..");
            return;
        }
        // update additional number
        ContentValues additionalvalues = new ContentValues();
        String whereadditional = Data.RAW_CONTACT_ID + " = \'" + mRawContactId + "\'" + " AND "
                + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'" + " AND "
                + Data.IS_ADDITIONAL_NUMBER + " =1";
        Log.d(TAG, "[updateAdditionalNumberToDB]whereadditional:" + whereadditional);

        if (!TextUtils.isEmpty(mUpdateAdditionalNumber)) {
            if (!TextUtils.isEmpty(mOldOtherPhone)) {
                additionalvalues.put(Phone.NUMBER, sOtherPhone);
                int upRet = resolver.update(Data.CONTENT_URI, additionalvalues, whereadditional,
                        null);
                ;
                Log.d(TAG, "[updateAdditionalNumberToDB]update ret:" + upRet);
            } else {
                additionalvalues.put(Phone.RAW_CONTACT_ID, mRawContactId);
                additionalvalues.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                additionalvalues.put(Phone.NUMBER, mUpdateAdditionalNumber);
                additionalvalues.put(Data.IS_ADDITIONAL_NUMBER, 1);
                additionalvalues.put(Data.DATA2, 7);
                Uri insertRetUri = resolver.insert(Data.CONTENT_URI, additionalvalues);
                Log.d(TAG, "[updateAdditionalNumberToDB]url insert ret:" + insertRetUri);
            }
        } else {
            int delRet = resolver.delete(Data.CONTENT_URI, whereadditional, null);
            Log.d(TAG, "[updateAdditionalNumberToDB]delete ret: " + delRet);
        }
    }

    private void updateEmail(final ContentResolver resolver) {
        // update emails
        ContentValues emailvalues = new ContentValues();
        emailvalues.put(Email.TYPE, Email.TYPE_MOBILE);
        String wheremail = Data.RAW_CONTACT_ID + " = \'" + mRawContactId + "\'" + " AND "
                + Data.MIMETYPE + "='" + Email.CONTENT_ITEM_TYPE + "'";
        Log.d(TAG, "[updateEmail]wheremail:" + wheremail);

        if (!TextUtils.isEmpty(mUpdatemail)) {
            if (!TextUtils.isEmpty(mOldEmail)) {
                emailvalues.put(Email.DATA, mUpdatemail);
                int upRet = resolver.update(Data.CONTENT_URI, emailvalues, wheremail, null);
                Log.d(TAG, "[updateEmail]update ret:" + upRet);
            } else {
                emailvalues.put(Email.RAW_CONTACT_ID, mRawContactId);
                emailvalues.put(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
                emailvalues.put(Email.DATA, mUpdatemail);
                Uri insertRetUri = resolver.insert(Data.CONTENT_URI, emailvalues);
                Log.d(TAG, "[updateEmail]Uri insert ret:" + insertRetUri);
            }
        } else {
            // update email is null,delete email row
            int delRet = resolver.delete(Data.CONTENT_URI, wheremail, null);
            Log.d(TAG, "[updateEmail]delete ret:" + delRet);
        }
    }

    private int upateGroup(final ContentResolver resolver, int errorType) {
        if (mOldGroupAddList.size() > 0) {
            for (Entry<Long, String> entry : mOldGroupAddList.entrySet()) {
                long grpId = entry.getKey();
                String grpName = entry.getValue();
                int ugrpId = -1;
                try {
                    ugrpId = USIMGroup.hasExistGroup(mSubId, grpName);
                } catch (RemoteException e) {
                    ugrpId = -1;
                }
                if (ugrpId > 0) {
                    USIMGroup.deleteUSIMGroupMember(mSubId, mIndexInSim, ugrpId);
                }
                int delCount = resolver
                        .delete(Data.CONTENT_URI, Data.MIMETYPE + "='"
                                + GroupMembership.CONTENT_ITEM_TYPE + "' AND "
                                + Data.RAW_CONTACT_ID + "=" + mRawContactId + " AND "
                                + ContactsContract.Data.DATA1 + "=" + grpId, null);
                Log.d(TAG, "[upateGroup]DB deleteCount:" + delCount);
            }
        }

        if (mGroupAddList.size() > 0) {
            Iterator<Entry<Long, String>> iter = mGroupAddList.entrySet().iterator();
            while (iter.hasNext()) {
                Entry<Long, String> entry = iter.next();
                long grpId = entry.getKey();
                String grpName = entry.getValue();
                int ugrpId = -1;
                try {
                    ugrpId = USIMGroup.syncUSIMGroupNewIfMissing(mSubId, grpName);
                } catch (RemoteException e) {
                    Log.w(TAG, "[upateGroup]RemoteException:" + e.toString());
                    ugrpId = -1;
                } catch (USIMGroupException e) {
                    errorType = e.getErrorType();
                    ugrpId = -1;
                    Log.w(TAG, "[upateGroup]errorType:" + errorType + ",USIMGroupException:"
                            + e.toString());
                }
                if (ugrpId > 0) {
                    boolean suFlag = USIMGroup.addUSIMGroupMember(mSubId, mIndexInSim, ugrpId);
                    Log.d(TAG, "[upateGroup]addUSIMGroupMember suFlag:" + suFlag + ",ugrpId:"
                            + ugrpId);
                    // insert into contacts DB
                    ContentValues groupValues = new ContentValues();
                    groupValues.put(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
                    groupValues.put(GroupMembership.GROUP_ROW_ID, grpId);
                    groupValues.put(Data.RAW_CONTACT_ID, mRawContactId);
                    resolver.insert(Data.CONTENT_URI, groupValues);
                }
            }
        }

        return errorType;
    }

    private int insertGroupToUSIMCard(long indexFromUri, int errorType) {
        int ugrpId = -1;
        Iterator<Entry<Long, String>> iter = mGroupAddList.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<Long, String> entry = iter.next();
            long grpId = entry.getKey();
            String grpName = entry.getValue();
            try {
                ugrpId = USIMGroup.syncUSIMGroupNewIfMissing(mSubId, grpName);
            } catch (RemoteException e) {
                Log.w(TAG, "[insertGroupToUSIMCard]RemoteException: " + e.toString());
                ugrpId = -1;
            } catch (USIMGroupException e) {
                errorType = e.getErrorType();
                Log.w(TAG, "[insertGroupToUSIMCard]errorType:" + errorType
                        + ",USIMGroupException: " + e.toString());
                ugrpId = -1;
            }
            if (ugrpId > 0) {
                boolean suFlag = USIMGroup.addUSIMGroupMember(mSubId, (int) indexFromUri, ugrpId);
                Log.d(TAG, "[insertGroupToUSIMCard]addUSIMGroupMember suFlag:" + suFlag
                        + ",ugrpId:" + ugrpId);
            } else {
                iter.remove();
            }
        }

        return errorType;
    }

    private boolean isInsertValuesInvalid() {
        if (SimCardUtils.SimType.SIM_TYPE_USIM_TAG.equals(mSimType)) {
            String fixedName = mUpdateName;
            if (!TextUtils.isEmpty(mUpdateName)) {
                NamePhoneTypePair namePhoneTypePair = new NamePhoneTypePair(mUpdateName);
                fixedName = namePhoneTypePair.name;
                Log.d(TAG, "fix name: " + fixedName);
            }
            if (TextUtils.isEmpty(fixedName)
                    && TextUtils.isEmpty(mUpdatephone)
                    && TextUtils.isEmpty(mUpdatemail)
                    && TextUtils.isEmpty(mUpdateAdditionalNumber)
                    && mGroupAddList.isEmpty()
                    && (ExtensionManager.getInstance().getSneExtension()
                            .checkNickName(this, mIntent, false, mSubId) == 1)) {//sne is empty
                mAllEditorInvalid = true;
            } else if (TextUtils.isEmpty(mUpdatephone)
                    && TextUtils.isEmpty(mUpdateName)
                    && (!TextUtils.isEmpty(mUpdatemail)
                            || !TextUtils.isEmpty(mUpdateAdditionalNumber)
                            || !mGroupAddList.isEmpty() || (ExtensionManager.getInstance()
                            .getSneExtension().checkNickName(this, mIntent, false, mSubId) != 1)
                    )) {
                mNumberIsNull = true;
            } else if (!TextUtils.isEmpty(mUpdatephone)) {
                if (!Pattern.matches(SIM_NUM_PATTERN,
                        PhoneNumberUtils.extractCLIRPortion(mUpdatephone))) {
                    mNumberInvalid = true;
                }
            }
            if (!TextUtils.isEmpty(mUpdateAdditionalNumber)) {
                if (!Pattern.matches(SIM_NUM_PATTERN,
                        PhoneNumberUtils.extractCLIRPortion(mUpdateAdditionalNumber))) {
                    mFixNumberInvalid = true;
                }
            }
        } else {
            if (TextUtils.isEmpty(mUpdatephone) && TextUtils.isEmpty(mUpdateName)) {
                mAllEditorInvalid = true;
            } else if (!TextUtils.isEmpty(mUpdatephone)) {
                if (!Pattern.matches(SIM_NUM_PATTERN,
                        PhoneNumberUtils.extractCLIRPortion(mUpdatephone))) {
                    mNumberInvalid = true;
                }
            }
        }

        setSaveFailToastText();
        if (mSaveFailToastStrId >= 0) {
            Log.i(TAG, "[isInsertValuesInvalid]setSaveFailToastText,return true.");
            return true;
        }

        // after all check,default should be false,mean valid
        boolean invalid = false;
        // current sne may too long
        invalid = ExtensionManager.getInstance().getSneExtension()
                .checkNickName(this, mIntent, true, mSubId) == 2;
        return invalid;
    }

    private void updateUSIMSpecValues(ContentValues values) {
        mUpdatemail = sEmail;

        mUpdateAdditionalNumber = sAfterOtherPhone;
        Log.d(TAG, "[updateUSIMSpecValues]before replace, mUpdateAdditionalNumber is:"
                + mUpdateAdditionalNumber);
        if (!TextUtils.isEmpty(mUpdateAdditionalNumber)) {
            mUpdateAdditionalNumber = mUpdateAdditionalNumber.replaceAll("-", "");
            mUpdateAdditionalNumber = mUpdateAdditionalNumber.replaceAll(" ", "");
            Log.i(TAG, "[updateUSIMSpecValues]after replace, mUpdateAdditionalNumber is: "
                    + mUpdateAdditionalNumber);
        }
        values.put("anr", TextUtils.isEmpty(mUpdateAdditionalNumber) ?
                "" : mUpdateAdditionalNumber);

        values.put("emails", TextUtils.isEmpty(mUpdatemail) ? "" : mUpdatemail);
    }

    private boolean compareData() {
        boolean compareName = false;
        if (!TextUtils.isEmpty(sName) && !TextUtils.isEmpty(mOldName)) {
            if (sName.equals(mOldName)) {
                compareName = true;
            }
        } else if (TextUtils.isEmpty(sName) && TextUtils.isEmpty(mOldName)) {
            compareName = true;
        }

        boolean comparePhone = false;
        if (!TextUtils.isEmpty(sPhone) && !TextUtils.isEmpty(mOldPhone)) {
            if (sPhone.equals(mOldPhone)) {
                comparePhone = true;
            }
        } else if (TextUtils.isEmpty(sPhone) && TextUtils.isEmpty(mOldPhone)) {
            comparePhone = true;
        }

        boolean compareEmail = false;
        if (!TextUtils.isEmpty(sEmail) && !TextUtils.isEmpty(mOldEmail)) {
            if (sEmail.equals(mOldEmail)) {
                compareEmail = true;
            }
        } else if (TextUtils.isEmpty(sEmail) && TextUtils.isEmpty(mOldEmail)) {
            compareEmail = true;
        }

        boolean compareOther = false;
        if (!TextUtils.isEmpty(sOtherPhone) && !TextUtils.isEmpty(mOldOtherPhone)) {
            if (sOtherPhone.equals(mOldOtherPhone)) {
                compareOther = true;
            }
        } else if (TextUtils.isEmpty(sOtherPhone) && TextUtils.isEmpty(mOldOtherPhone)) {
            compareOther = true;
        }

        boolean compareGroup = false;
        if (mGroupAddList != null && mOldGroupAddList != null) {
            if (mGroupAddList.equals(mOldGroupAddList)) {
                compareGroup = true;
            }
        } else if (mGroupAddList == null && mOldGroupAddList == null) {
            compareGroup = true;
        }
        Log.i(TAG, "[compareData]compareName : " + compareName + " | comparePhone : "
                + comparePhone + " | compareOther : " + compareOther + " | compareEmail: "
                + compareEmail + " | compareGroup : " + compareGroup);

        Log.i(TAG, "[compareData] sName : " + sName + " | mOldName : " + mOldName
                + " | sEmail : " + sEmail + " | mOldEmail : " + mOldEmail + " | sPhone : " + sPhone
                + " | mOldPhone: " + mOldPhone + " | sOtherPhone : " + sOtherPhone
                + " | mOldOtherPhone :" + " | mOldOtherPhone : " + mOldOtherPhone);

        return (compareName && comparePhone && compareOther && compareEmail && compareGroup);
    }

    private boolean isPhbReady() {
        mPhbReady = SimCardUtils.isPhoneBookReady(mSubId);
        return mPhbReady;
    }

    private void deliverCallback(Uri lookupUri) {
        Log.i(TAG, "[deliverCallback]RESULT_OK---");

        Intent callbackIntent =
                mIntent.getParcelableExtra(SIMProcessorService.EXTRA_CALLBACK_INTENT);
        if (callbackIntent != null) {
            callbackIntent.putExtra(RESULT, RESULT_OK);
            callbackIntent.setAction(EDIT_SIM_ACTION);
            /// M: Bug fix for ALPS02015883
            callbackIntent.setData(lookupUri);
            deliverCallbackOnUiThread(callbackIntent);
        } else {
            Log.w(TAG, "IllegalStateException: callbackIntent == NULL!");
        }
    }

    private void deliverCallbackOnUiThread(final Intent intent) {
        for (final Listener listener : sListeners) {
            Handler handler = sListenerHolder.get(listener);
            if (handler != null) {
                handler.post(new Runnable() {
                    public void run() {
                        listener.onSIMEditCompleted(intent);
                    }
                });
            }
        }
    }

    private void deliverCallbackOnError() {
        Log.i(TAG, "[deliverCallbackOnError]RESULT_NO_DATA---");

        Intent callbackIntent =
                mIntent.getParcelableExtra(SIMProcessorService.EXTRA_CALLBACK_INTENT);
        Intent intent = new Intent();
        if (callbackIntent != null) {
            intent = callbackIntent;
        }
        intent.putExtra(RESULT, RESULT_NO_DATA);
        intent.putExtra(KEY_NEED_QUIT_EDIT, mQuitEdit);
        intent.setAction(EDIT_SIM_ACTION);
        deliverCallbackOnUiThread(intent);
    }

    public void deliverCallbackAndBackToFragment() {
        Log.i(TAG, "[deliverCallbackAndBackToFragment]RESULT_CANCELED---");
        final Intent intent = new Intent();
        intent.putParcelableArrayListExtra(KEY_SIM_DATA, mSimData);
        intent.putExtra(KEY_NEED_QUIT_EDIT, mQuitEdit);
        intent.putExtra(RESULT, RESULT_CANCELED);
        intent.setAction(EDIT_SIM_ACTION);
        deliverCallbackOnUiThread(intent);
    }

    private void showToastMessage(int resourceId, String content) {
        for (Listener listener : sListeners) {
            Handler handler = sListenerHolder.get(listener);
            if (handler != null) {
                Message msg = handler.obtainMessage();
                msg.arg1 = resourceId;
                Bundle bundle = new Bundle();
                bundle.putString("content", content);
                msg.setData(bundle);
                handler.sendMessage(msg);
                Log.i(TAG, "show toast message");
            }
        }
    }


    private static final char PLUS_SIGN_CHAR = '+';
    private static final String PLUS_SIGN_STRING = "+";
    private static final String NANP_IDP_STRING = "011";
    private static final int NANP_LENGTH = 10;

    private void initStaticValues() {
        sName = null;
        sEmail = null;
        sPhone = null;
        sOtherPhone = null;
        sAfterPhone = null;
        sAfterOtherPhone = null;
    }
}
