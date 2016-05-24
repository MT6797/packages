/*
* Copyright (C) 2011-2014 MediaTek Inc.
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
package com.mediatek.services.telephony;

import android.telecom.ConnectionRequest;
import android.telecom.PhoneAccountHandle;

import android.telephony.TelephonyManager;

import android.util.Log;

import com.android.internal.telephony.SubscriptionController;

import com.android.phone.PhoneUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The emergency call handler.
 * Selected the proper Phone for setting up the ecc call.
 */
public class EmergencyRetryHandler {
    static final String TAG = "EmergencyRetryHandler";
    static final boolean DBG = true;

    private static final int MAX_NUM_RETRIES = TelephonyManager.getDefault().getPhoneCount();

    private ConnectionRequest mRequest = null;
    private int mNumRetriesSoFar = 0;
    private List<PhoneAccountHandle> mAttemptRecords;
    private Iterator<PhoneAccountHandle> mAttemptRecordIterator;
    private String mCallId = null;

    /**
     * Init the EmergencyRetryHandler.
     * @param request ConnectionRequest
     * @param initPhoneId PhoneId of the initial ECC
     */
    public EmergencyRetryHandler(ConnectionRequest request, int initPhoneId) {
        mRequest = request;
        mNumRetriesSoFar = 0;
        mAttemptRecords = new ArrayList<>();

        PhoneAccountHandle phoneAccountHandle;
        int num = 0;

        do {
            // 1. Add other phone rather than initPhone sequentially
            for (int i = 0; i < TelephonyManager.getDefault().getSimCount(); i++) {
                int[] subIds = SubscriptionController.getInstance().getSubIdUsingSlotId(i);
                if (subIds == null || subIds.length == 0)
                    continue;

                int phoneId = SubscriptionController.getInstance().getPhoneId(subIds[0]);
                if (initPhoneId != phoneId) {
                    // If No SIM is inserted, the corresponding IccId will be null,
                    // take phoneId as PhoneAccountHandle::mId which is IccId originally
                    phoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(
                            Integer.toString(phoneId));
                    mAttemptRecords.add(phoneAccountHandle);
                    num ++;
                    Log.d(TAG, "Add #" + num + " to ECC Retry list: " + phoneAccountHandle);
                }
            }

            // 2. Add initPhone at last
            phoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(
                    Integer.toString(initPhoneId));
            mAttemptRecords.add(phoneAccountHandle);
            num ++;
            Log.d(TAG, "Add #" + num + " to ECC Retry list: " + phoneAccountHandle);
        } while (num <  MAX_NUM_RETRIES);

        mAttemptRecordIterator = mAttemptRecords.iterator();

    }

    public void setCallId(String id) {
        Log.d(TAG, "setCallId = " + id);
        mCallId = id;
    }

    public String getCallId() {
        Log.d(TAG, "getCallId = " + mCallId);
        return mCallId;
    }

    public boolean isTimeout() {
        Log.d(TAG, "mNumRetriesSoFar = " + mNumRetriesSoFar);
        return (mNumRetriesSoFar >= MAX_NUM_RETRIES);
    }

    public ConnectionRequest getRequest() {
        Log.d(TAG, "mRequest = " + mRequest);
        return mRequest;
    }

    public PhoneAccountHandle getNextAccountHandle() {
        if (mAttemptRecordIterator.hasNext()) {
            mNumRetriesSoFar ++;
            Log.d(TAG, "getNextAccountHandle has Next");
            return mAttemptRecordIterator.next();
        }
        Log.d(TAG, "getNextAccountHandle is null");
        return null;
    }

    /*
    public PhoneAccountHandle getInitAccountHandle() {
        return mRequest.getAccountHandle()();
    }
    */
}
