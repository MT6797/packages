package com.android.server.telecom;

import com.android.server.telecom.components.ErrorDialogActivity;

import java.util.ArrayList;
import java.util.List;

import com.mediatek.telecom.volte.TelecomVolteUtils;
import com.mediatek.telecom.LogUtils;
import com.mediatek.telecom.TelecomManagerEx;
import com.mediatek.telecom.TelecomUtils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Trace;
import android.os.UserHandle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.telephony.DisconnectCause;
import android.telephony.PhoneNumberUtils;
import android.widget.Toast;

/**
 * Single point of entry for all outgoing and incoming calls.
 * {@link com.android.server.telecom.components.UserCallIntentProcessor} serves as a trampoline that
 * captures call intents for individual users and forwards it to the {@link CallIntentProcessor}
 * which interacts with the rest of Telecom, both of which run only as the primary user.
 */
public class CallIntentProcessor {

    private static final String TAG = "CallIntentProcessor";
    public static final String KEY_IS_UNKNOWN_CALL = "is_unknown_call";
    public static final String KEY_IS_INCOMING_CALL = "is_incoming_call";
    /*
     *  Whether or not the dialer initiating this outgoing call is the default dialer, or system
     *  dialer and thus allowed to make emergency calls.
     */
    public static final String KEY_IS_PRIVILEGED_DIALER = "is_privileged_dialer";

    private final Context mContext;
    private final CallsManager mCallsManager;

    public CallIntentProcessor(Context context, CallsManager callsManager) {
        this.mContext = context;
        this.mCallsManager = callsManager;
    }

    public void processIntent(Intent intent) {
        final boolean isUnknownCall = intent.getBooleanExtra(KEY_IS_UNKNOWN_CALL, false);
        Log.i(this, "onReceive - isUnknownCall: %s", isUnknownCall);

        Trace.beginSection("processNewCallCallIntent");
        if (isUnknownCall) {
            processUnknownCallIntent(mCallsManager, intent);
        } else {
            processOutgoingCallIntent(mContext, mCallsManager, intent);
        }
        Trace.endSection();
    }


    /**
     * Processes CALL, CALL_PRIVILEGED, and CALL_EMERGENCY intents.
     *
     * @param intent Call intent containing data about the handle to call.
     */
    static void processOutgoingCallIntent(
            Context context,
            CallsManager callsManager,
            Intent intent) {

        /// M: for log parser @{
        LogUtils.logIntent(intent);
        /// @}

        if (shouldPreventDuplicateVideoCall(context, callsManager, intent)) {
            return;
        }

        Uri handle = intent.getData();
        String scheme = handle.getScheme();
        String uriString = handle.getSchemeSpecificPart();

        if (!PhoneAccount.SCHEME_VOICEMAIL.equals(scheme)) {
            handle = Uri.fromParts(PhoneNumberUtils.isUriNumber(uriString) ?
                    PhoneAccount.SCHEME_SIP : PhoneAccount.SCHEME_TEL, uriString, null);
        }

        PhoneAccountHandle phoneAccountHandle = intent.getParcelableExtra(
                TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE);

        Bundle clientExtras = null;
        if (intent.hasExtra(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS)) {
            clientExtras = intent.getBundleExtra(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS);
        }
        if (clientExtras == null) {
            clientExtras = new Bundle();
        }

        final boolean isPrivilegedDialer = intent.getBooleanExtra(KEY_IS_PRIVILEGED_DIALER, false);

        /// M: For dial via specified slot. @{
        if (intent.hasExtra(TelecomUtils.EXTRA_SLOT)) {
            int slotId = intent.getIntExtra(TelecomUtils.EXTRA_SLOT, -1);
            phoneAccountHandle = TelecomUtils
                    .getPhoneAccountHandleWithSlotId(context, slotId, phoneAccountHandle);
        }
        /// @}

        /// M: for VoLTE @{
        // Here we handle all error case for VoLTE.
        boolean isImsCallRequest = TelecomVolteUtils.isImsCallOnlyRequest(intent);
        boolean isConferenceDialRequest = TelecomVolteUtils.isConferenceDialRequest(intent);
        if (isImsCallRequest || isConferenceDialRequest) {
            Log.d(TAG, "MO - VoLTE case: Ims Call / Conference Dial = %s / %s",
                    isImsCallRequest, isConferenceDialRequest);
            if (!TelecomVolteUtils.isImsEnabled(context)) {
                Log.d(TAG, "MO - VoLTE case: Ims is disabled => Abandon");
                TelecomVolteUtils.showImsDisableDialog(context);
                return;
            }
            List<PhoneAccountHandle> accounts = TelecomUtils.getVoltePhoneAccountHandles();
            if (accounts == null || accounts.isEmpty()) {
                Log.d(TAG, "MO - VoLTE case: No VoLTE account => Abandon");
                TelecomVolteUtils.showNoImsAccountDialog(context);
                return;
            }
            if (isImsCallRequest) {
                clientExtras.putBoolean(TelecomVolteUtils.EXTRA_VOLTE_IMS_CALL, true);
            }
            if (isConferenceDialRequest) {
                handle = TelecomVolteUtils.checkHandleForConferenceDial(context, handle);
            }
        }
        /// @}

        /// M: For Ip dial & suggested account & VoLTE-Ims Call &
        //         VoLTE-Conference Dial & ViLTE-Block certain ViLTE. @{
        copyExtraToBundle(intent, clientExtras);
        /// @}

        // Send to CallsManager to ensure the InCallUI gets kicked off before the broadcast returns
        Call call = callsManager.startOutgoingCall(handle, phoneAccountHandle, clientExtras);

        if (call != null) {
            /// M: ip dial. ip prefix already add, here need to change intent @{
            if (call.isIpCall()) {
                intent.setData(call.getHandle());
            }
            /// @}

            /// M: For VoLTE - Conference Dial @{
            // For Con dial, skip NewOutgoingCallIntentBroadcaster. createConnection() directly.
            if (call.isConferenceDial()) {
                call.startCreateConnection(TelecomSystem.getInstance().getPhoneAccountRegistrar());
                return;
            }
            /// @}

            // Asynchronous calls should not usually be made inside a BroadcastReceiver
            // because once
            // onReceive is complete, the BroadcastReceiver's process runs the risk of getting
            // killed if memory is scarce. However, this is OK here because the entire Telecom
            // process will be running throughout the duration of the phone call and should never
            // be killed.
            NewOutgoingCallIntentBroadcaster broadcaster = new NewOutgoingCallIntentBroadcaster(
                    context, callsManager, call, intent, isPrivilegedDialer);

            final int result = broadcaster.processIntent();
            final boolean success = result == DisconnectCause.NOT_DISCONNECTED;

            if (!success && call != null) {
                disconnectCallAndShowErrorDialog(context, call, result);
            }
        }
    }

    static void processIncomingCallIntent(CallsManager callsManager, Intent intent) {

        /// M: for log parser @{
        LogUtils.logIntent(intent);
        /// @}

        PhoneAccountHandle phoneAccountHandle = intent.getParcelableExtra(
                TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE);

        if (phoneAccountHandle == null) {
            Log.w(CallIntentProcessor.class,
                    "Rejecting incoming call due to null phone account");
            return;
        }
        if (phoneAccountHandle.getComponentName() == null) {
            Log.w(CallIntentProcessor.class,
                    "Rejecting incoming call due to null component name");
            return;
        }

        Bundle clientExtras = null;
        if (intent.hasExtra(TelecomManager.EXTRA_INCOMING_CALL_EXTRAS)) {
            clientExtras = intent.getBundleExtra(TelecomManager.EXTRA_INCOMING_CALL_EXTRAS);
        }
        if (clientExtras == null) {
            clientExtras = new Bundle();
        }

        Log.d(CallIntentProcessor.class,
                "Processing incoming call from connection service [%s]",
                phoneAccountHandle.getComponentName());
        callsManager.processIncomingCallIntent(phoneAccountHandle, clientExtras);
    }

    static void processUnknownCallIntent(CallsManager callsManager, Intent intent) {
        PhoneAccountHandle phoneAccountHandle = intent.getParcelableExtra(
                TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE);

        if (phoneAccountHandle == null) {
            Log.w(CallIntentProcessor.class, "Rejecting unknown call due to null phone account");
            return;
        }
        if (phoneAccountHandle.getComponentName() == null) {
            Log.w(CallIntentProcessor.class, "Rejecting unknown call due to null component name");
            return;
        }

        callsManager.addNewUnknownCall(phoneAccountHandle, intent.getExtras());
    }

    private static void disconnectCallAndShowErrorDialog(
            Context context, Call call, int errorCode) {
        call.disconnect();
        final Intent errorIntent = new Intent(context, ErrorDialogActivity.class);
        int errorMessageId = -1;
        switch (errorCode) {
            case DisconnectCause.INVALID_NUMBER:
            case DisconnectCause.NO_PHONE_NUMBER_SUPPLIED:
                errorMessageId = R.string.outgoing_call_error_no_phone_number_supplied;
                break;
        }
        if (errorMessageId != -1) {
            errorIntent.putExtra(ErrorDialogActivity.ERROR_MESSAGE_ID_EXTRA, errorMessageId);
            errorIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivityAsUser(errorIntent, UserHandle.CURRENT);
        }
    }

    /**
     * Whether an outgoing video call should be prevented from going out. Namely, don't allow an
     * outgoing video call if there is already an ongoing video call. Notify the user if their call
     * is not sent.
     *
     * @return {@code true} if the outgoing call is a video call and should be prevented from going
     *     out, {@code false} otherwise.
     */
    private static boolean shouldPreventDuplicateVideoCall(
            Context context,
            CallsManager callsManager,
            Intent intent) {

        int intentVideoState = intent.getIntExtra(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE,
                VideoProfile.STATE_AUDIO_ONLY);

        /// M: For ViLTE & 3G VT @{
        // Original Code:
        /*
        if (VideoProfile.isAudioOnly(intentVideoState)
                || !callsManager.hasVideoCall()) {
            return false;
        } else {
            // Display an error toast to the user.
            Toast.makeText(
                    context,
                    context.getResources().getString(R.string.duplicate_video_call_not_allowed),
                    Toast.LENGTH_LONG).show();
            return true;
        }
        */
        Uri handle = intent.getData();
        boolean isEmerency = TelephonyUtil.shouldProcessAsEmergency(context, handle);
        if (!isEmerency &&
                callsManager.shouldBlockFor3GVT(VideoProfile.isVideo(intentVideoState))) {
            Toast.makeText(context,
                    context.getResources().getString(R.string.outgoing_call_failed),
                    Toast.LENGTH_LONG).show();
            Log.d(TAG, "MO - 3G VT case: => Abandon!");
            return true;
        }
        return false;
        /// @}
    }

    /// M: For Ip dial & suggested account & VoLTE-Ims Call & VoLTE-Conference Dial. @{
    // get extra from Intent, and pass them to Bundle, which will be used later.
    private static void copyExtraToBundle(Intent intent, Bundle extras) {
        if (intent == null || extras == null) {
            return;
        }
        // Ip Dial
        if (intent.hasExtra(Constants.EXTRA_IS_IP_DIAL)) {
            extras.putBoolean(Constants.EXTRA_IS_IP_DIAL,
                    intent.getBooleanExtra(Constants.EXTRA_IS_IP_DIAL, false));
        }
        // Suggest PhoneAccount
        if (intent.hasExtra(TelecomManagerEx.EXTRA_SUGGESTED_PHONE_ACCOUNT_HANDLE)) {
            extras.putParcelable(
                  TelecomManagerEx.EXTRA_SUGGESTED_PHONE_ACCOUNT_HANDLE,
                  intent.getParcelableExtra(TelecomManagerEx.EXTRA_SUGGESTED_PHONE_ACCOUNT_HANDLE));
        }
        // VoLTE - Ims Call
        if (intent.hasExtra(TelecomVolteUtils.EXTRA_VOLTE_IMS_CALL)) {
            extras.putBoolean(TelecomVolteUtils.EXTRA_VOLTE_IMS_CALL,
                    intent.getBooleanExtra(TelecomVolteUtils.EXTRA_VOLTE_IMS_CALL, false));
        }
        // VoLTE - Conference Dial
        if (intent.hasExtra(TelecomManagerEx.EXTRA_VOLTE_CONF_CALL_DIAL)) {
            extras.putBoolean(TelecomManagerEx.EXTRA_VOLTE_CONF_CALL_DIAL,
                    intent.getBooleanExtra(TelecomManagerEx.EXTRA_VOLTE_CONF_CALL_DIAL, false));
        }
        // VoLTE - Conference Dial
        if (intent.hasExtra(TelecomManagerEx.EXTRA_VOLTE_CONF_CALL_NUMBERS)) {
            extras.putStringArrayList(TelecomManagerEx.EXTRA_VOLTE_CONF_CALL_NUMBERS,
                    intent.getStringArrayListExtra(TelecomManagerEx.EXTRA_VOLTE_CONF_CALL_NUMBERS));
        }
        // ViLTE - Block certain ViLTE @{
        // setVideoState in advance (in CallsManager.startOutgoingCall()),
        // for shouldBlockForCertainViLTE() will use it in makeRoomForOutgoingCall().
        if (intent.hasExtra(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE)) {
            int videoState = intent.getIntExtra(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE,
                    VideoProfile.STATE_AUDIO_ONLY);
            extras.putInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE, videoState);
        }
    }
    /// @}
}
