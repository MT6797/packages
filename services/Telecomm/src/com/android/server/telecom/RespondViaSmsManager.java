/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.server.telecom;

// TODO: Needed for move to system service: import com.android.internal.R;
import com.android.internal.os.SomeArgs;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SmsApplication;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telecom.Response;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to manage the "Respond via Message" feature for incoming calls.
 */
public class RespondViaSmsManager extends CallsManagerListenerBase {
    private static final int MSG_SHOW_SENT_TOAST = 2;

    private final CallsManager mCallsManager;
    private final TelecomSystem.SyncRoot mLock;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SHOW_SENT_TOAST: {
                    SomeArgs args = (SomeArgs) msg.obj;
                    try {
                        String toastMessage = (String) args.arg1;
                        Context context = (Context) args.arg2;
                        showMessageSentToast(toastMessage, context);
                    } finally {
                        args.recycle();
                    }
                    break;
                }
            }
        }
    };

    public RespondViaSmsManager(CallsManager callsManager, TelecomSystem.SyncRoot lock) {
        mCallsManager = callsManager;
        mLock = lock;
    }

    /**
     * Read the (customizable) canned responses from SharedPreferences,
     * or from defaults if the user has never actually brought up
     * the Settings UI.
     *
     * The interface of this method is asynchronous since it does disk I/O.
     *
     * @param response An object to receive an async reply, which will be called from
     *                 the main thread.
     * @param context The context.
     */
    public void loadCannedTextMessages(final Response<Void, List<String>> response,
            final Context context) {
        new Thread() {
            @Override
            public void run() {
                Log.d(RespondViaSmsManager.this, "loadCannedResponses() starting");

                // This function guarantees that QuickResponses will be in our
                // SharedPreferences with the proper values considering there may be
                // old QuickResponses in Telephony pre L.
                QuickResponseUtils.maybeMigrateLegacyQuickResponses(context);

                final SharedPreferences prefs = context.getSharedPreferences(
                        QuickResponseUtils.SHARED_PREFERENCES_NAME,
                        Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
                final Resources res = context.getResources();

                final ArrayList<String> textMessages = new ArrayList<>(
                        QuickResponseUtils.NUM_CANNED_RESPONSES);

                // Note the default values here must agree with the corresponding
                // android:defaultValue attributes in respond_via_sms_settings.xml.
                textMessages.add(0, prefs.getString(QuickResponseUtils.KEY_CANNED_RESPONSE_PREF_1,
                        res.getString(R.string.respond_via_sms_canned_response_1)));
                textMessages.add(1, prefs.getString(QuickResponseUtils.KEY_CANNED_RESPONSE_PREF_2,
                        res.getString(R.string.respond_via_sms_canned_response_2)));
                textMessages.add(2, prefs.getString(QuickResponseUtils.KEY_CANNED_RESPONSE_PREF_3,
                        res.getString(R.string.respond_via_sms_canned_response_3)));
                textMessages.add(3, prefs.getString(QuickResponseUtils.KEY_CANNED_RESPONSE_PREF_4,
                        res.getString(R.string.respond_via_sms_canned_response_4)));

                Log.d(RespondViaSmsManager.this,
                        "loadCannedResponses() completed, found responses: %s",
                        textMessages.toString());

                synchronized (mLock) {
                    response.onResult(null, textMessages);
                }
            }
        }.start();
    }

    @Override
    public void onIncomingCallRejected(Call call, boolean rejectWithMessage, String textMessage) {
        if (rejectWithMessage && call.getHandle() != null) {
            int subId = mCallsManager.getPhoneAccountRegistrar().getSubscriptionIdForPhoneAccount(
                    call.getTargetPhoneAccount());
            rejectCallWithMessage(call.getContext(), call.getHandle().getSchemeSpecificPart(),
                    textMessage, subId);
        }
    }

    private void showMessageSentToast(final String phoneNumber, final Context context) {
        // ...and show a brief confirmation to the user (since
        // otherwise it's hard to be sure that anything actually
        // happened.)
        final Resources res = context.getResources();
        final String formatString = res.getString(
                R.string.respond_via_sms_confirmation_format);
        final String confirmationMsg = String.format(formatString, phoneNumber);
        Toast.makeText(context, confirmationMsg,
                Toast.LENGTH_LONG).show();

        // TODO: If the device is locked, this toast won't actually ever
        // be visible!  (That's because we're about to dismiss the call
        // screen, which means that the device will return to the
        // keyguard.  But toasts aren't visible on top of the keyguard.)
        // Possible fixes:
        // (1) Is it possible to allow a specific Toast to be visible
        //     on top of the keyguard?
        // (2) Artificially delay the dismissCallScreen() call by 3
        //     seconds to allow the toast to be seen?
        // (3) Don't use a toast at all; instead use a transient state
        //     of the InCallScreen (perhaps via the InCallUiState
        //     progressIndication feature), and have that state be
        //     visible for 3 seconds before calling dismissCallScreen().
    }

    /**
     * Reject the call with the specified message. If message is null this call is ignored.
     */

    private void rejectCallWithMessage(Context context, String phoneNumber, String textMessage,
            int subId) {
        if (textMessage != null) {
            final ComponentName component =
                    SmsApplication.getDefaultRespondViaMessageApplication(context,
                            true /*updateIfNeeded*/);
            if (component != null) {
                // Build and send the intent
                final Uri uri = Uri.fromParts(Constants.SCHEME_SMSTO, phoneNumber, null);
                final Intent intent = new Intent(TelephonyManager.ACTION_RESPOND_VIA_MESSAGE, uri);
                intent.putExtra(Intent.EXTRA_TEXT, textMessage);
                if (SubscriptionManager.isValidSubscriptionId(subId)) {
                    intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, subId);
                }

                SomeArgs args = SomeArgs.obtain();
                args.arg1 = phoneNumber;
                args.arg2 = context;
                mHandler.obtainMessage(MSG_SHOW_SENT_TOAST, args).sendToTarget();
                intent.setComponent(component);
                context.startService(intent);
            }
        }
    }
}
