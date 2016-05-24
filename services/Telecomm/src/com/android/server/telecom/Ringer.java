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
 * limitations under the License.
 */

package com.android.server.telecom;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.SystemVibrator;
import android.os.Vibrator;
import android.provider.Settings;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.mediatek.audioprofile.AudioProfileManager;
import com.mediatek.telecom.TelecomUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * Controls the ringtone player.
 * TODO: Turn this into a proper state machine: Ringing, CallWaiting, Stopped.
 */
final class Ringer extends CallsManagerListenerBase {
    private static final long[] VIBRATION_PATTERN = new long[] {
        0, // No delay before starting
        1000, // How long to vibrate
        1000, // How long to wait before vibrating again
    };

    private static final int STATE_RINGING = 1;
    private static final int STATE_CALL_WAITING = 2;
    private static final int STATE_STOPPED = 3;

    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .build();

    /** Indicate that we want the pattern to repeat at the step which turns on vibration. */
    private static final int VIBRATION_PATTERN_REPEAT = 1;

    private final AsyncRingtonePlayer mRingtonePlayer;

    /**
     * Used to keep ordering of unanswered incoming calls. There can easily exist multiple incoming
     * calls and explicit ordering is useful for maintaining the proper state of the ringer.
     */
    private final List<Call> mRingingCalls = new LinkedList<>();

    private final CallAudioManager mCallAudioManager;
    private final CallsManager mCallsManager;
    private final InCallTonePlayer.Factory mPlayerFactory;
    private final Context mContext;
    private final Vibrator mVibrator;
    private int mState = STATE_STOPPED;
    private final static int KEY_MO_VIBRATE_CONFIG = 0x00000002;
    private final static long MO_CALL_VIBRATE_TIME = 200;
    private InCallTonePlayer mCallWaitingPlayer;

    /**
     * Used to track the status of {@link #mVibrator} in the case of simultaneous incoming calls.
     */
    private boolean mIsVibrating = false;

    /** Initializes the Ringer. */
    Ringer(
            CallAudioManager callAudioManager,
            CallsManager callsManager,
            InCallTonePlayer.Factory playerFactory,
            Context context) {

        mCallAudioManager = callAudioManager;
        mCallsManager = callsManager;
        mPlayerFactory = playerFactory;
        mContext = context;
        // We don't rely on getSystemService(Context.VIBRATOR_SERVICE) to make sure this
        // vibrator object will be isolated from others.
        mVibrator = new SystemVibrator(context);
        mRingtonePlayer = new AsyncRingtonePlayer(context);
    }

    @Override
    public void onCallAdded(final Call call) {
        if (call.isIncoming() && call.getState() == CallState.RINGING) {
            if (mRingingCalls.contains(call)) {
                Log.wtf(this, "New ringing call is already in list of unanswered calls");
            }
            mRingingCalls.add(call);
            updateRinging(call);
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        removeFromUnansweredCall(call);
    }

    @Override
    public void onCallStateChanged(Call call, int oldState, int newState) {
        if (newState != CallState.RINGING) {
            removeFromUnansweredCall(call);
        }
        ///M: vibrate when MO call connected. @{
        if (newState == CallState.ACTIVE
                && (oldState == CallState.DIALING || oldState == CallState.CONNECTING)
                /// M: CDMA MO call special handling. @{
                // For cdma call, framework will vibrate when the call be 'really' answered
                // by remote side, at this point the CDMA MO call maybe not in
                // real ACTIVE state, so skip this for CDMA MO call.
                && !TelecomUtils.hasCdmaCallCapability(mContext,
                        call.getTargetPhoneAccount())) {
                /// @}
            int emSetting = Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.TELEPHONY_MISC_FEATURE_CONFIG, KEY_MO_VIBRATE_CONFIG);
            boolean enabled = (emSetting & KEY_MO_VIBRATE_CONFIG) != 0;
            if (enabled) {
                mVibrator.vibrate(MO_CALL_VIBRATE_TIME);
            }
        }
        /// @}
    }

    @Override
    public void onIncomingCallAnswered(Call call) {
        onRespondedToIncomingCall(call);
    }

    @Override
    public void onIncomingCallRejected(Call call, boolean rejectWithMessage, String textMessage) {
        onRespondedToIncomingCall(call);
    }

    @Override
    public void onForegroundCallChanged(Call oldForegroundCall, Call newForegroundCall) {
        Call ringingCall = null;
        if (mRingingCalls.contains(newForegroundCall)) {
            ringingCall = newForegroundCall;
        } else if (mRingingCalls.contains(oldForegroundCall)) {
            ringingCall = oldForegroundCall;
        }
        if (ringingCall != null) {
            updateRinging(ringingCall);
        }
    }

    /**
     * Silences the ringer for any actively ringing calls.
     */
    void silence() {
        // Remove all calls from the "ringing" set and then update the ringer.
        mRingingCalls.clear();
        updateRinging(null);
    }

    private void onRespondedToIncomingCall(Call call) {
        // Only stop the ringer if this call is the top-most incoming call.
        if (getTopMostUnansweredCall() == call) {
            removeFromUnansweredCall(call);
        }
    }

    private Call getTopMostUnansweredCall() {
        return mRingingCalls.isEmpty() ? null : mRingingCalls.get(0);
    }

    /**
     * Removes the specified call from the list of unanswered incoming calls and updates the ringer
     * based on the new state of {@link #mRingingCalls}. Safe to call with a call that is not
     * present in the list of incoming calls.
     */
    private void removeFromUnansweredCall(Call call) {
        mRingingCalls.remove(call);
        updateRinging(call);
    }

    private void updateRinging(Call call) {
        if (mRingingCalls.isEmpty()) {
            stopRinging(call, "No more ringing calls found");
            stopCallWaiting(call);
        } else {
            startRingingOrCallWaiting(call);
        }
    }

    private void startRingingOrCallWaiting(Call call) {
        Call foregroundCall = mCallsManager.getForegroundCall();
        Log.v(this, "startRingingOrCallWaiting, foregroundCall: %s.", foregroundCall);

        ///M: ALPS02310009
        // do not play ringtone when ringcall as foreground
        // and there have active or hold calls @{
        Log.i(this, "CallsManager.getInstance().hasActiveOrHoldingCall() = "
                + mCallsManager.hasActiveOrHoldingCall());
        if (mRingingCalls.contains(foregroundCall)
            && !mCallsManager.hasActiveOrHoldingCall()) {
        //@}
            ///M: ALPS01778496: not change call waiting to ringtone @{
            if (mCallWaitingPlayer != null) {
                return;
            }
            /// @}

            // The foreground call is one of incoming calls so play the ringer out loud.
            stopCallWaiting(call);

            if (!shouldRingForContact(foregroundCall.getContactUri())) {
                ///M: ALPS01786536: to request audio focus even interruption is on @{
                mCallAudioManager.setIsRinging(call, true);
                /// @}
                return;
            }

            AudioManager audioManager =
                    (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.getStreamVolume(AudioManager.STREAM_RING) > 0) {
                if (mState != STATE_RINGING) {
                    Log.event(call, Log.Events.START_RINGER);
                    mState = STATE_RINGING;
                }
                mCallAudioManager.setIsRinging(call, true);

                if (SystemProperties.get("ro.mtk_multisim_ringtone").equals("1")) {
                    PhoneAccountHandle phoneAccountHandle = foregroundCall.getTargetPhoneAccount();
                    Log.v(this, "phoneAccountHandle = " + phoneAccountHandle);
                    int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
                    if (phoneAccountHandle != null) {
                        TelephonyManager tem = (TelephonyManager) mContext
                            .getSystemService(Context.TELEPHONY_SERVICE);
                        TelecomManager tm = (TelecomManager)mContext
                            .getSystemService(Context.TELECOM_SERVICE);
                        if (tem != null && tm != null) {
                            try {
                                PhoneAccount account = tm.getPhoneAccount(phoneAccountHandle);
                                if (account != null) {
                                    subId = tem.getSubIdForPhoneAccount(account);
                                }
                            } catch(Exception e) {
                                e.printStackTrace();
                                Log.d(this, "getSubIdForPhoneAccount error: " + e.toString());
                            }
                        }
                    }
                    AudioProfileManager audioProfileMgr = (AudioProfileManager) mContext
                            .getSystemService(Context.AUDIO_PROFILE_SERVICE);
                    Uri ringtoneUri = audioProfileMgr.getRingtoneUri(audioProfileMgr.getActiveProfileKey()
                            , AudioProfileManager.TYPE_RINGTONE, subId);
                    if (false == RingtoneManager.isRingtoneExist(mContext, ringtoneUri)) {
                        ringtoneUri = null;
                    }
                    Log.d(this, "subscriber id: " + subId + " ringtoneUri: " + ringtoneUri);
                    Uri contactRingtoneUri = foregroundCall.getRingtone();
                    mRingtonePlayer.play(contactRingtoneUri == null ? ringtoneUri : contactRingtoneUri);
                } else {

                    // Because we wait until a contact info query to complete before processing a
                    // call (for the purposes of direct-to-voicemail), the information about custom
                    // ringtones should be available by the time this code executes. We can safely
                    // request the custom ringtone from the call and expect it to be current.
                    mRingtonePlayer.play(foregroundCall.getRingtone());
                }
            } else {
                Log.v(this, "startRingingOrCallWaiting, skipping because volume is 0");
            }

            if (shouldVibrate(mContext) && !mIsVibrating) {
                mVibrator.vibrate(VIBRATION_PATTERN, VIBRATION_PATTERN_REPEAT,
                        VIBRATION_ATTRIBUTES);
                mIsVibrating = true;
            }
        } else if (foregroundCall != null) {
            ///M: ALPS01978768
            // do not play call-waiting tone when a pre_dial_wait call exists
            // directly play ringtone when pre_dial_wait call is disconnected @{
            if (foregroundCall.getState() == CallState.SELECT_PHONE_ACCOUNT) {
                return;
            }
            /// @}

            ///M: ALPS02009942
            // do not change ringtone to call-waiting
            // when one ringing call is disconnected by remote or rejected(two incoming calls exist)
            // this happens dsda project @{
            if ((foregroundCall.getState() == CallState.DISCONNECTED && !mRingingCalls.isEmpty())
                    || mCallsManager.isAllCallRinging()) {
                Log.v(this, "do not change ringtone to call-waiting when one ringing call is disconnected(two incoming calls exist)");
                return;
            }
            /// @}

            // The first incoming call added to Telecom is not a foreground call at this point
            // in time. If the current foreground call is null at point, don't play call-waiting
            // as the call will eventually be promoted to the foreground call and play the
            // ring tone.
            Log.v(this, "Playing call-waiting tone.");

            // All incoming calls are in background so play call waiting.
            stopRinging(call, "Stop for call-waiting");


            if (mState != STATE_CALL_WAITING) {
                Log.event(call, Log.Events.START_CALL_WAITING_TONE);
                mState = STATE_CALL_WAITING;
            }

            if (mCallWaitingPlayer == null) {
                mCallWaitingPlayer =
                        mPlayerFactory.createPlayer(InCallTonePlayer.TONE_CALL_WAITING);
                mCallWaitingPlayer.startTone();
            }
        }
    }

    private boolean shouldRingForContact(Uri contactUri) {
        final NotificationManager manager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        final Bundle extras = new Bundle();
        if (contactUri != null) {
            extras.putStringArray(Notification.EXTRA_PEOPLE, new String[] {contactUri.toString()});
        }
        return manager.matchesCallFilter(extras);
    }

    private void stopRinging(Call call, String reasonTag) {
        if (mState == STATE_RINGING) {
            Log.event(call, Log.Events.STOP_RINGER, reasonTag);
            mState = STATE_STOPPED;
        }

        mRingtonePlayer.stop();

        if (mIsVibrating) {
            mVibrator.cancel();
            mIsVibrating = false;
        }

        // Even though stop is asynchronous it's ok to update the audio manager. Things like audio
        // focus are voluntary so releasing focus too early is not detrimental.
        mCallAudioManager.setIsRinging(call, false);
    }

    private void stopCallWaiting(Call call) {
        Log.v(this, "stop call waiting.");
        if (mCallWaitingPlayer != null) {
            mCallWaitingPlayer.stopTone();
            mCallWaitingPlayer = null;
        }

        if (mState == STATE_CALL_WAITING) {
            Log.event(call, Log.Events.STOP_CALL_WAITING_TONE);
            mState = STATE_STOPPED;
        }
    }

    private boolean shouldVibrate(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        /// M: use MTK_AUDIO_PROFILES on non-bsp @{
        if (SystemProperties.get("ro.mtk_audio_profiles").equals("1")
                && !SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            //return audioManager.shouldVibrate(AudioManager.VIBRATE_TYPE_RINGER);
            //ALPS01813593: change to use AudioProfileManager, to sync with Dial vibrate settings
            AudioProfileManager audioProfileMgr = (AudioProfileManager) context.getSystemService(Context.AUDIO_PROFILE_SERVICE);
            String profileKey = audioProfileMgr.getActiveProfileKey();
            return audioProfileMgr.isVibrationEnabled(profileKey);
        }
        /// @}

        int ringerMode = audioManager.getRingerModeInternal();
        if (getVibrateWhenRinging(context)) {
            return ringerMode != AudioManager.RINGER_MODE_SILENT;
        } else {
            return ringerMode == AudioManager.RINGER_MODE_VIBRATE;
        }
    }

    private boolean getVibrateWhenRinging(Context context) {
        if (!mVibrator.hasVibrator()) {
            return false;
        }
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.VIBRATE_WHEN_RINGING, 0) != 0;
    }
}
