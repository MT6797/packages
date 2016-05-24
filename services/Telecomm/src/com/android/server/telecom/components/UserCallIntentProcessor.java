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

package com.android.server.telecom.components;

import com.android.server.telecom.CallIntentProcessor;
import com.android.server.telecom.Log;
import com.android.server.telecom.R;
import com.android.server.telecom.TelephonyUtil;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.UserHandle;
import android.os.UserManager;
import android.telecom.DefaultDialerManager;
import android.telecom.PhoneAccount;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.widget.Toast;

// TODO: Needed for move to system service: import com.android.internal.R;

/**
 * Handles system CALL actions and forwards them to {@link CallIntentProcessor}.
 * Handles all three CALL action types: CALL, CALL_PRIVILEGED, and CALL_EMERGENCY.
 *
 * Pre-L, the only way apps were were allowed to make outgoing emergency calls was the
 * ACTION_CALL_PRIVILEGED action (which requires the system only CALL_PRIVILEGED permission).
 *
 * In L, any app that has the CALL_PRIVILEGED permission can continue to make outgoing emergency
 * calls via ACTION_CALL_PRIVILEGED.
 *
 * In addition, the default dialer (identified via
 * {@link android.telecom.TelecomManager#getDefaultDialerPackage()} will also be granted the
 * ability to make emergency outgoing calls using the CALL action. In order to do this, it must
 * use the {@link TelecomManager#placeCall(Uri, android.os.Bundle)} method to allow its package
 * name to be passed to {@link UserCallIntentProcessor}. Calling startActivity will continue to
 * work on all non-emergency numbers just like it did pre-L.
 */
public class UserCallIntentProcessor {

    private final Context mContext;
    private final UserHandle mUserHandle;

    /**
     * M: Identifier for intent extra for sending an empty Flash message for
     * CDMA networks.
     */
    public static final String EXTRA_SEND_EMPTY_FLASH =
            "com.android.phone.extra.SEND_EMPTY_FLASH";

    public UserCallIntentProcessor(Context context, UserHandle userHandle) {
        mContext = context;
        mUserHandle = userHandle;
    }

    /**
     * Processes intents sent to the activity.
     *
     * @param intent The intent.
     */
    public void processIntent(Intent intent, String callingPackageName,
            boolean canCallNonEmergency) {
        // Ensure call intents are not processed on devices that are not capable of calling.
        if (!isVoiceCapable()) {
            return;
        }

        String action = intent.getAction();

        if (Intent.ACTION_CALL.equals(action) ||
                Intent.ACTION_CALL_PRIVILEGED.equals(action) ||
                Intent.ACTION_CALL_EMERGENCY.equals(action)) {
            processOutgoingCallIntent(intent, callingPackageName, canCallNonEmergency);
        }
    }

    private void processOutgoingCallIntent(Intent intent, String callingPackageName,
            boolean canCallNonEmergency) {
        Uri handle = intent.getData();
        String scheme = handle.getScheme();
        String uriString = handle.getSchemeSpecificPart();

        /// M: Do noting for CDMA empty flash at present
        if (intent.getBooleanExtra(EXTRA_SEND_EMPTY_FLASH, false)) {
            Log.w(this, "Empty flash obtained from the call intent.");
            return;
        }

        if (!PhoneAccount.SCHEME_VOICEMAIL.equals(scheme)) {
            handle = Uri.fromParts(PhoneNumberUtils.isUriNumber(uriString) ?
                    PhoneAccount.SCHEME_SIP : PhoneAccount.SCHEME_TEL, uriString, null);
        }

        final UserManager userManager =
                (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        if (userManager.hasUserRestriction(UserManager.DISALLOW_OUTGOING_CALLS, mUserHandle)
                && !TelephonyUtil.shouldProcessAsEmergency(mContext, handle)) {
            // Only emergency calls are allowed for users with the DISALLOW_OUTGOING_CALLS
            // restriction.
            showErrorDialogForRestrictedOutgoingCall(mContext,
                    R.string.outgoing_call_not_allowed_user_restriction);
            Log.w(this, "Rejecting non-emergency phone call due to DISALLOW_OUTGOING_CALLS "
                    + "restriction");
            return;
        }

        if (!canCallNonEmergency && !TelephonyUtil.shouldProcessAsEmergency(mContext, handle)) {
            showErrorDialogForRestrictedOutgoingCall(mContext,
                    R.string.outgoing_call_not_allowed_no_permission);
            Log.w(this, "Rejecting non-emergency phone call because "
                    + android.Manifest.permission.CALL_PHONE + " permission is not granted.");
            return;
        }

        int videoState = intent.getIntExtra(
                TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE,
                VideoProfile.STATE_AUDIO_ONLY);
        Log.d(this, "processOutgoingCallIntent videoState = " + videoState);

        if (VideoProfile.isVideo(videoState)
                && TelephonyUtil.shouldProcessAsEmergency(mContext, handle)) {
            Log.d(this, "Emergency call...Converting video call to voice...");
            videoState = VideoProfile.STATE_AUDIO_ONLY;
            intent.putExtra(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE,
                    videoState);
        }

        if (VideoProfile.isVideo(videoState) && isTtyModeEnabled()) {
            Toast.makeText(mContext, mContext.getResources().getString(R.string.
                    video_call_not_allowed_if_tty_enabled), Toast.LENGTH_SHORT).show();
            Log.d(this, "Rejecting video calls as tty is enabled");
            return;
        }

        intent.putExtra(CallIntentProcessor.KEY_IS_PRIVILEGED_DIALER,
                isDefaultOrSystemDialer(callingPackageName));
        sendBroadcastToReceiver(intent);
    }

    private boolean isTtyModeEnabled() {
        return (android.provider.Settings.Secure.getInt(
                mContext.getContentResolver(),
                android.provider.Settings.Secure.PREFERRED_TTY_MODE,
                TelecomManager.TTY_MODE_OFF) != TelecomManager.TTY_MODE_OFF);
    }

    private boolean isDefaultOrSystemDialer(String callingPackageName) {
        if (TextUtils.isEmpty(callingPackageName)) {
            return false;
        }

        final String defaultDialer = DefaultDialerManager.getDefaultDialerApplication(mContext,
                mUserHandle.getIdentifier());
        if (TextUtils.equals(defaultDialer, callingPackageName)) {
            return true;
        }

        final TelecomManager telecomManager =
                (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
        return TextUtils.equals(telecomManager.getSystemDialerPackage(), callingPackageName);
    }

    /**
     * Returns whether the device is voice-capable (e.g. a phone vs a tablet).
     *
     * @return {@code True} if the device is voice-capable.
     */
    private boolean isVoiceCapable() {
        return mContext.getApplicationContext().getResources().getBoolean(
                com.android.internal.R.bool.config_voice_capable);
    }

    /**
     * Trampolines the intent to the broadcast receiver that runs only as the primary user.
     */
    private boolean sendBroadcastToReceiver(Intent intent) {
        intent.putExtra(CallIntentProcessor.KEY_IS_INCOMING_CALL, false);
        intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.setClass(mContext, PrimaryCallReceiver.class);
        Log.d(this, "Sending broadcast as user to CallReceiver");
        mContext.sendBroadcastAsUser(intent, UserHandle.OWNER);
        return true;
    }

    private static void showErrorDialogForRestrictedOutgoingCall(Context context, int stringId) {
        final Intent intent = new Intent(context, ErrorDialogActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ErrorDialogActivity.ERROR_MESSAGE_ID_EXTRA, stringId);
        context.startActivityAsUser(intent, UserHandle.CURRENT);
    }
}
