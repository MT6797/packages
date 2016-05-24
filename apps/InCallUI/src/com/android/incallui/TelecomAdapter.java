/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.incallui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
/// M: add for sms. @{
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
/// @}
import android.os.Looper;
import android.telecom.InCallService;
import android.telecom.PhoneAccountHandle;

/// M: add for sms. @{
import android.telephony.TelephonyManager;
import com.android.internal.os.SomeArgs;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SmsApplication;
/// @}

import com.google.common.base.Preconditions;
/// M: add for sms.
import com.mediatek.incallui.Toast;
/// M: add for volte.
import com.mediatek.incallui.volte.InCallUIVolteUtils;
import com.mediatek.incallui.InCallUtils;
import java.util.List;
/// M: change TelecomAdapter to public @{
//final class TelecomAdapter implements InCallServiceListener {
public final class TelecomAdapter implements InCallServiceListener {
///@}
    private static final String ADD_CALL_MODE_KEY = "add_call_mode";

    private static TelecomAdapter sInstance;
    private InCallService mInCallService;

    /// M: update package access to public access. @{
    /*
     * Google code:
    static TelecomAdapter getInstance() {
     */
    public static TelecomAdapter getInstance() {
    /// @}
        Preconditions.checkState(Looper.getMainLooper().getThread() == Thread.currentThread());
        if (sInstance == null) {
            sInstance = new TelecomAdapter();
        }
        return sInstance;
    }

    private TelecomAdapter() {
    }

    @Override
    public void setInCallService(InCallService inCallService) {
        mInCallService = inCallService;
    }

    @Override
    public void clearInCallService() {
        mInCallService = null;
    }

    private android.telecom.Call getTelecommCallById(String callId) {
        Call call = CallList.getInstance().getCallById(callId);
        return call == null ? null : call.getTelecommCall();
    }

    void answerCall(String callId, int videoState) {
        android.telecom.Call call = getTelecommCallById(callId);
        if (call != null) {
            call.answer(videoState);
        } else {
            Log.e(this, "error answerCall, call not in call list: " + callId);
        }
    }

    void rejectCall(String callId, boolean rejectWithMessage, String message) {
        android.telecom.Call call = getTelecommCallById(callId);
        if (call != null) {
            call.reject(rejectWithMessage, message);
        } else {
            Log.e(this, "error rejectCall, call not in call list: " + callId);
        }
    }

    void disconnectCall(String callId) {
        android.telecom.Call call = getTelecommCallById(callId);
        if (call != null) {
            call.disconnect();
        } else {
            Log.e(this, "error disconnectCall, call not in call list " + callId);
        }
    }

    void holdCall(String callId) {
        android.telecom.Call call = getTelecommCallById(callId);
        if (call != null) {
            call.hold();
        } else {
            Log.e(this, "error holdCall, call not in call list " + callId);
        }
    }

    void unholdCall(String callId) {
        android.telecom.Call call = getTelecommCallById(callId);
        if (call != null) {
            call.unhold();
        } else {
            Log.e(this, "error unholdCall, call not in call list " + callId);
        }
    }

    void mute(boolean shouldMute) {
        if (mInCallService != null) {
            mInCallService.setMuted(shouldMute);
        } else {
            Log.e(this, "error mute, mInCallService is null");
        }
    }

    void setAudioRoute(int route) {
        if (mInCallService != null) {
            mInCallService.setAudioRoute(route);
        } else {
            Log.e(this, "error setAudioRoute, mInCallService is null");
        }
    }

    void separateCall(String callId) {
        android.telecom.Call call = getTelecommCallById(callId);
        if (call != null) {
            call.splitFromConference();
        } else {
            Log.e(this, "error separateCall, call not in call list " + callId);
        }
    }

    void merge(String callId) {
        android.telecom.Call call = getTelecommCallById(callId);
        if (call != null) {
            List<android.telecom.Call> conferenceable = call.getConferenceableCalls();
            if (!conferenceable.isEmpty()) {
                call.conference(conferenceable.get(0));
            } else {
                if (call.getDetails().can(
                        android.telecom.Call.Details.CAPABILITY_MERGE_CONFERENCE)) {
                    call.mergeConference();
                }
            }
        } else {
            Log.e(this, "error merge, call not in call list " + callId);
        }
    }

    void swap(String callId) {
        android.telecom.Call call = getTelecommCallById(callId);
        if (call != null) {
            if (call.getDetails().can(
                    android.telecom.Call.Details.CAPABILITY_SWAP_CONFERENCE)) {
                call.swapConference();
            }
        } else {
            Log.e(this, "error swap, call not in call list " + callId);
        }
    }

    void addCall() {
        if (mInCallService != null) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // when we request the dialer come up, we also want to inform
            // it that we're going through the "add call" option from the
            // InCallScreen / PhoneUtils.
            intent.putExtra(ADD_CALL_MODE_KEY, true);
            try {
                Log.d(this, "Sending the add Call intent");
                mInCallService.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                // This is rather rare but possible.
                // Note: this method is used even when the phone is encrypted. At that moment
                // the system may not find any Activity which can accept this Intent.
                Log.e(this, "Activity for adding calls isn't found.", e);
            }
        }
    }

    void playDtmfTone(String callId, char digit) {
        android.telecom.Call call = getTelecommCallById(callId);
        if (call != null) {
            call.playDtmfTone(digit);
        } else {
            Log.e(this, "error playDtmfTone, call not in call list " + callId);
        }
    }

    void stopDtmfTone(String callId) {
        android.telecom.Call call = getTelecommCallById(callId);
        if (call != null) {
            call.stopDtmfTone();
        } else {
            Log.e(this, "error stopDtmfTone, call not in call list " + callId);
        }
    }

    void postDialContinue(String callId, boolean proceed) {
        android.telecom.Call call = getTelecommCallById(callId);
        if (call != null) {
            call.postDialContinue(proceed);
        } else {
            Log.e(this, "error postDialContinue, call not in call list " + callId);
        }
    }

    void phoneAccountSelected(String callId, PhoneAccountHandle accountHandle, boolean setDefault) {
        if (accountHandle == null) {
            Log.e(this, "error phoneAccountSelected, accountHandle is null");
            // TODO: Do we really want to send null accountHandle?
        }

        android.telecom.Call call = getTelecommCallById(callId);
        if (call != null) {
            call.phoneAccountSelected(accountHandle, setDefault);
        } else {
            Log.e(this, "error phoneAccountSelected, call not in call list " + callId);
        }
    }

    boolean canAddCall() {
        // Default to true if we are not connected to telecom.
        return mInCallService == null ? true : mInCallService.canAddCall();
    }

    // ---------------------------------MTK--------------------------------------
    /**
     * M: Start voice recording
     */
    void startVoiceRecording() {
        if (mInCallService != null) {
            mInCallService.startVoiceRecording();
        } else {
            Log.e(this, "error startVoiceRecording, mInCallService is null");
        }
    }

    /**
     * M: Stop voice recording
     */
    void stopVoiceRecording() {
        if (mInCallService != null) {
            mInCallService.stopVoiceRecording();
        } else {
            Log.e(this, "error stopVoiceRecording, mInCallService is null");
        }
    }

    /**
     * M: Handle ECT.
     */
    void explicitCallTransfer(String callId) {
        if (mInCallService != null) {
            mInCallService.explicitCallTransfer(callId);
        } else {
            Log.e(this, "error explicitCallTransfer, mInCallService is null");
        }
    }

    /**
     * M: Instructs Telecom to disconnect all the calls.
     */
    void hangupAll() {
        if (mInCallService != null) {
            mInCallService.hangupAll();
        } else {
            Log.e(this, "error hangupAll, mInCallService is null");
        }
    }

    /**
     * M: Instructs Telecom to disconnect all the HOLDING calls.
     */
    void hangupAllHoldCalls() {
        if (mInCallService != null) {
            mInCallService.hangupAllHoldCalls();
        } else {
            Log.e(this, "error hangupAllHoldCalls, mInCallService is null");
        }
    }

    /**
     * M: Instructs Telecom to disconnect active call and answer waiting call.
     */
    void hangupActiveAndAnswerWaiting() {
        if (mInCallService != null) {
            mInCallService.hangupActiveAndAnswerWaiting();
        } else {
            Log.e(this, "error hangupActiveAndAnswerWaiting, mInCallService is null");
        }
    }

    /**
     * M: ALPS01766524. If Call ended, then send sms.
     */
    void sendMessageIfCallEnded(Context context, String callId, String phoneNumber,
            String textMessage) {
        if (mInCallService != null && getTelecommCallById(callId) != null) {
            return;
        }

        if (textMessage != null) {
            final ComponentName component =
                    SmsApplication.getDefaultRespondViaMessageApplication(context,
                            false /*updateIfNeeded*/);
            if (component != null) {
                // Build and send the intent
                final Uri uri = Uri.fromParts("smsto", phoneNumber, null);
                final Intent intent = new Intent(TelephonyManager.ACTION_RESPOND_VIA_MESSAGE, uri);
                intent.putExtra(Intent.EXTRA_TEXT, textMessage);
                final Call call = CallList.getInstance().getCallById(callId);
                intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, InCallUtils.getSubId(call));

                showMessageSentToast(phoneNumber, context);
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = phoneNumber;
                args.arg2 = context;
                intent.setComponent(component);
                context.startService(intent);
                Log.d(this, "sendMessageIfCallEnded message: " + textMessage);
            }
        }
    }

    /**
     * M: ALPS01784391. Show toast for sms.
     */
    private void showMessageSentToast(final String phoneNumber, final Context context) {
        // ...and show a brief confirmation to the user (since
        // otherwise it's hard to be sure that anything actually
        // happened.)
        final Resources res = context.getResources();
        final String formatString = res.getString(
                R.string.respond_via_sms_confirmation_format);
        final String confirmationMsg = String.format(formatString, phoneNumber);
        Toast.getInstance().show(confirmationMsg);
    }

    /**
     * M: For VoLTE invitation
     * @param conferenceCallId
     * @param numbers
     */
    public void inviteConferenceParticipants(String conferenceCallId, List<String> numbers) {
        if (mInCallService != null && getTelecommCallById(conferenceCallId) != null) {
            getTelecommCallById(conferenceCallId).inviteConferenceParticipants(numbers);
        } else {
            Log.e(this, "inviteConferenceParticipants()... mInCallService is null");
        }
    }

}
