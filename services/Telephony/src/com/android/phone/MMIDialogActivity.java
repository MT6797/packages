/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
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

package com.android.phone;

import android.app.Activity;
import android.app.Dialog;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.gsm.GsmMmiCode;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;

import java.util.List;

/**
 * Used to display a dialog from within the Telephony service when running an USSD code
 */
public class MMIDialogActivity extends Activity {
    private static final String TAG = MMIDialogActivity.class.getSimpleName();

    private Dialog mMMIDialog;

    private Handler mHandler;

    private Phone mPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case PhoneGlobals.MMI_COMPLETE:
                            Log.d(TAG, "handle MMI_COMPLETE");
                            onMMIComplete((AsyncResult) msg.obj);
                            break;
                        case PhoneGlobals.MMI_CANCEL:
                            Log.d(TAG, "handle MMI_CANCEL");
                            onMMICancel();
                            break;
                    }
                }
        };

        int ID = this.getIntent().getExtras().getInt("ID");
        Log.d(TAG, "the phone id = " + ID);
        mPhone = PhoneFactory.getPhone(ID);
        if (mPhone == null) {
            Log.d(TAG, "There is no valid phone!");
            finish();
            return;
        }

        Log.d(TAG, "Register MMI_COMPLETE");
        mPhone.registerForMmiComplete(mHandler, PhoneGlobals.MMI_COMPLETE, null);

        if (mPhone.getState() == PhoneConstants.State.OFFHOOK) {
            Toast.makeText(this, R.string.incall_status_dialed_mmi, Toast.LENGTH_SHORT).show();
        }
        showMMIDialog();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()...  this = " + this);
        if (mMMIDialog != null) {
            mMMIDialog.dismiss();
        }
        if (mHandler != null) {
            mPhone.unregisterForMmiComplete(mHandler);
        }
        super.onDestroy();
    }

    private void showMMIDialog() {
        final List<? extends MmiCode> codes = mPhone.getPendingMmiCodes();
        if (codes.size() > 0) {
            final MmiCode mmiCode = codes.get(0);
            final Message message = Message.obtain(mHandler, PhoneGlobals.MMI_CANCEL);
            mMMIDialog = PhoneUtils.displayMMIInitiate(this, mmiCode, message, mMMIDialog);
        } else {
            finish();
        }
    }

    /**
     * Handles an MMI_COMPLETE event, which is triggered by telephony
     */
    private void onMMIComplete(AsyncResult r) {
        // Check the code to see if the request is ready to
        // finish, this includes any MMI state that is not
        // PENDING.
        MmiCode mmiCode = (MmiCode) r.result;
        String ussdHandler = null;
        if ((r.userObj != null) && (r.userObj instanceof String)) {
            ussdHandler = (String) r.userObj;
        }
        // if phone is a CDMA phone display feature code completed message
        int phoneType = mPhone.getPhoneType();
        if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
            PhoneUtils.displayMMIComplete(mPhone, this, mmiCode, null, null);
        } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
            if (mmiCode instanceof GsmMmiCode) {
                if (ussdHandler != null && ussdHandler.equals("stk")) {
                    Log.d(TAG, "onMMIComplete() ussd is handled by stk!");
                    dismissDialogsAndFinish();
                    return;
                }
            }
            if (mmiCode.getState() != MmiCode.State.PENDING) {
                Log.d(TAG, "Got MMI_COMPLETE, finishing dialog activity...");
                dismissDialogsAndFinish();
            }
        }
    }

    /**
     * Handles an MMI_CANCEL event, which is triggered by the button
     * (labeled either "OK" or "Cancel") on the "MMI Started" dialog.
     * @see PhoneUtils#cancelMmiCode(Phone)
     */
    private void onMMICancel() {
        Log.v(TAG, "onMMICancel()...");

        // First of all, cancel the outstanding MMI code (if possible.)
        PhoneUtils.cancelMmiCode(mPhone);

        // Regardless of whether the current MMI code was cancelable, the
        // PhoneApp will get an MMI_COMPLETE event very soon, which will
        // take us to the MMI Complete dialog (see
        // PhoneUtils.displayMMIComplete().)
        //
        // But until that event comes in, we *don't* want to stay here on
        // the in-call screen, since we'll be visible in a
        // partially-constructed state as soon as the "MMI Started" dialog
        // gets dismissed. So let's forcibly bail out right now.
        Log.d(TAG, "onMMICancel: finishing InCallScreen...");
        dismissDialogsAndFinish();
    }

    private void dismissDialogsAndFinish() {
        if (mMMIDialog != null) {
            mMMIDialog.dismiss();
            mMMIDialog = null;
        }
        if (mHandler != null) {
            mPhone.unregisterForMmiComplete(mHandler);
            mHandler = null;
        }
        finish();
    }
}
