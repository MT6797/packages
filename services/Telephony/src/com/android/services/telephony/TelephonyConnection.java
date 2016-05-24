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

package com.android.services.telephony;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.AsyncResult;
/// M: IMS feature. @{
import android.os.Bundle;
/// @}
import android.os.Handler;
import android.os.Message;
import android.telecom.CallAudioState;
import android.telecom.ConferenceParticipant;
import android.telecom.Connection;
import android.telecom.ConnectionService;
import android.telecom.PhoneAccount;
import android.telecom.StatusHints;
/// M: IMS feature. @{
import android.telecom.VideoProfile;
/// @}

/// M: ECC Retry @{
import android.telephony.PhoneNumberUtils;
/// @}

/// M: VILTE feature. @{
import android.widget.Toast;
/// @}

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection.PostDialListener;
import com.android.internal.telephony.Phone;

import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import com.android.phone.R;

/// M: ALPS02136977. Prints debug logs for telephony.
import com.mediatek.telecom.FormattedLog;

/// M: IMS feature. @{
import com.mediatek.telecom.TelecomManagerEx;
/// @}

import java.lang.Override;
/// M: IMS feature. @{
import java.util.ArrayList;
/// @}
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for CDMA and GSM connections.
 */
/// M: CC030: CRSS notification @{
// Declare as public for SuppMessageManager to use.
public abstract class TelephonyConnection extends Connection {
/// @}
    private static final int MSG_PRECISE_CALL_STATE_CHANGED = 1;
    private static final int MSG_RINGBACK_TONE = 2;
    private static final int MSG_HANDOVER_STATE_CHANGED = 3;
    private static final int MSG_DISCONNECT = 4;
    private static final int MSG_MULTIPARTY_STATE_CHANGED = 5;
    private static final int MSG_CONFERENCE_MERGE_FAILED = 6;

    private static final int MTK_EVENT_BASE = 1000;
    /// M: CC031: Radio off notification @{
    private static final int EVENT_RADIO_OFF_OR_NOT_AVAILABLE       = MTK_EVENT_BASE;
    /// @}
    /// M: CC077: 2/3G CAPABILITY_HIGH_DEF_AUDIO @{
    private static final int EVENT_SPEECH_CODEC_INFO                = MTK_EVENT_BASE + 1;
    /// @}
    /// M: For 3G VT only @{
    private static final int EVENT_VT_STATUS_INFO                   = MTK_EVENT_BASE + 2;
    /// @}
    /// M: For CDMA call accepted @{
    private static final int EVENT_CDMA_CALL_ACCEPTED               = MTK_EVENT_BASE + 3;
    /// @}

    /// M: ECC Retry @{
    private boolean mIsEmergency = false;
    /// @}

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PRECISE_CALL_STATE_CHANGED:
                    Log.v(TelephonyConnection.this, "MSG_PRECISE_CALL_STATE_CHANGED");
                    /// M: ALPS02136977. Prints debug messages for telephony. @{
                    if ((!mIsMultiParty || !isImsConnection())
                            && mOriginalConnection != null
                            && mOriginalConnectionState != mOriginalConnection.getState()) {
                        logDebugMsgWithNotifyFormat(
                                callStateToFormattedNotifyString(mOriginalConnection.getState()),
                                null);
                    }
                    /// @}
                    updateState();
                    break;
                case MSG_HANDOVER_STATE_CHANGED:
                    Log.v(TelephonyConnection.this, "MSG_HANDOVER_STATE_CHANGED");
                    AsyncResult ar = (AsyncResult) msg.obj;
                    com.android.internal.telephony.Connection connection =
                         (com.android.internal.telephony.Connection) ar.result;
                    if (mOriginalConnection != null) {
                        if (connection != null &&
                            ((connection.getAddress() != null &&
                            mOriginalConnection.getAddress() != null &&
                            mOriginalConnection.getAddress().contains(connection.getAddress())) ||
                            /// M: fix Google bug. @{
                            // it should use mOriginalConnection.getStateBeforeHandover().
                            // connection.getStateBeforeHandover() == mOriginalConnection
                            //            .getState())) {
                            mOriginalConnection.getStateBeforeHandover() == connection
                                    .getState())) {
                            /// @}
                            Log.d(TelephonyConnection.this,
                                    "SettingOriginalConnection " + mOriginalConnection.toString()
                                            + " with " + connection.toString());
                            /// M: ALPS02528198 Show toast when VILTE SRVCC @{
                            if (mOriginalConnection.getVideoState() !=
                                    VideoProfile.STATE_AUDIO_ONLY &&
                                    connection.getVideoState() == VideoProfile.STATE_AUDIO_ONLY) {
                                Context context = getPhone().getContext();
                                Toast.makeText(context,
                                        context.getString(R.string.vilte_srvcc_tip),
                                        Toast.LENGTH_LONG).show();
                                Log.d(TelephonyConnection.this,
                                        "Video call change to vocie call during SRVCC");
                            }
                            /// @}
                            setOriginalConnection(connection);
                            mWasImsConnection = false;
                        }
                    } else {
                        Log.w(TelephonyConnection.this,
                                "MSG_HANDOVER_STATE_CHANGED: mOriginalConnection==null - invalid state (not cleaned up)");
                    }
                    break;
                case MSG_RINGBACK_TONE:
                    Log.v(TelephonyConnection.this, "MSG_RINGBACK_TONE");
                    // TODO: This code assumes that there is only one connection in the foreground
                    // call, in other words, it punts on network-mediated conference calling.
                    if (getOriginalConnection() != getForegroundConnection()) {
                        Log.v(TelephonyConnection.this, "handleMessage, original connection is " +
                                "not foreground connection, skipping");
                        return;
                    }
                    setRingbackRequested((Boolean) ((AsyncResult) msg.obj).result);
                    break;
                case MSG_DISCONNECT:
                    /// M: @{
                    int cause = mOriginalConnection == null
                            ? android.telephony.DisconnectCause.NOT_VALID
                            : mOriginalConnection.getDisconnectCause();
                    Log.v(TelephonyConnection.this, "handle MSG_DISCONNECT ... cause = " + cause);

                    if (mOriginalConnection != null && cause ==
                            android.telephony.DisconnectCause.IMS_EMERGENCY_REREG) {
                        try {
                            com.android.internal.telephony.Connection conn =
                                getPhone().dial(mOriginalConnection.getOrigDialString(),
                                    VideoProfile.STATE_AUDIO_ONLY);
                            setOriginalConnection(conn);
                            notifyEcc();
                        } catch (CallStateException e) {
                            Log.e(TelephonyConnection.this, e, "Fail to redial as ECC");
                        }
                    } else {
                        /// M: ALPS02136977. Prints debug messages for telephony. @{
                        if ((!mIsMultiParty || !isImsConnection())
                                && mOriginalConnection != null
                                && mOriginalConnectionState != mOriginalConnection.getState()) {
                            logDebugMsgWithNotifyFormat(callStateToFormattedNotifyString(
                                    mOriginalConnection.getState()), null);
                        }
                        /// @}
                        /// M: ECC Retry @{
                        if (cause != android.telephony.DisconnectCause.NORMAL
                                && cause != android.telephony.DisconnectCause.LOCAL) {
                            Log.d(this, "ECC Retry : check whether need to retry");
                            // Assume only one ECC exists
                            if (mIsEmergency
                                    && TelephonyConnectionServiceUtil.getInstance().isEccRetryOn()
                                    && mOriginalConnectionState.isDialing()
                                    && !TelephonyConnectionServiceUtil.getInstance()
                                            .eccRetryTimeout()) {
                                Log.d(this, "ECC Retry : Meet retry condition");
                                close(); // To removeConnection
                                TelephonyConnectionServiceUtil.getInstance().performEccRetry();
                                break;
                            }
                        }
                        /// @}
                        updateState();
                    }
                    /// @}
                    break;
                /// M: CC031: Radio off notification @{
                case EVENT_RADIO_OFF_OR_NOT_AVAILABLE:
                    notifyConnectionLost();
                    break;
                /// @}
                /// M: CC077: 2/3G CAPABILITY_HIGH_DEF_AUDIO @{
                case EVENT_SPEECH_CODEC_INFO:
                    int value = (int) ((AsyncResult) msg.obj).result;
                    Log.v(TelephonyConnection.this, "EVENT_SPEECH_CODEC_INFO : " + value);
                    if (TelephonyConnectionServiceUtil.getInstance().isHighDefAudio(value)) {
                        setAudioQuality(com.android.internal.telephony.Connection.
                                AUDIO_QUALITY_HIGH_DEFINITION);
                    } else {
                        setAudioQuality(com.android.internal.telephony.Connection.
                                AUDIO_QUALITY_STANDARD);
                    }
                    break;
                /// @}
                /// M: For 3G VT only @{
                case EVENT_VT_STATUS_INFO:
                    int status = ((int[]) ((AsyncResult) msg.obj).result)[0];
                    notifyVtStatusInfo(status);
                    break;
                /// @}
                case MSG_MULTIPARTY_STATE_CHANGED:
                    boolean isMultiParty = (Boolean) msg.obj;
                    Log.i(this, "Update multiparty state to %s", isMultiParty ? "Y" : "N");
                    mIsMultiParty = isMultiParty;
                    if (isMultiParty) {
                        notifyConferenceStarted();
                    }
                case MSG_CONFERENCE_MERGE_FAILED:
                    notifyConferenceMergeFailed();
                    break;
                /// M: For CDMA call accepted @{
                case EVENT_CDMA_CALL_ACCEPTED:
                    Log.v(TelephonyConnection.this, "EVENT_CDMA_CALL_ACCEPTED");
                    updateConnectionCapabilities();
                    fireOnCdmaCallAccepted();
                    break;
                /// @}
            }
        }
    };

    /**
     * A listener/callback mechanism that is specific communication from TelephonyConnections
     * to TelephonyConnectionService (for now). It is more specific that Connection.Listener
     * because it is only exposed in Telephony.
     */
    public abstract static class TelephonyConnectionListener {
        public void onOriginalConnectionConfigured(TelephonyConnection c) {}

        /**
         * For VoLTE enhanced conference call, notify invite conf. participants completed.
         * @param isSuccess is success or not.
         */
        public void onConferenceParticipantsInvited(boolean isSuccess) {}

        /**
         * For VoLTE conference SRVCC, notify when new participant connections maded.
         * @param radioConnections new participant connections.
         */
        public void onConferenceConnectionsConfigured(
            ArrayList<com.android.internal.telephony.Connection> radioConnections) {}

    }

    private final PostDialListener mPostDialListener = new PostDialListener() {
        @Override
        public void onPostDialWait() {
            Log.v(TelephonyConnection.this, "onPostDialWait");
            if (mOriginalConnection != null) {
                setPostDialWait(mOriginalConnection.getRemainingPostDialString());
            }
        }

        @Override
        public void onPostDialChar(char c) {
            Log.v(TelephonyConnection.this, "onPostDialChar: %s", c);
            if (mOriginalConnection != null) {
                setNextPostDialChar(c);
            }
        }
    };

    /**
     * Listener for listening to events in the {@link com.android.internal.telephony.Connection}.
     */
    private final com.android.internal.telephony.Connection.Listener mOriginalConnectionListener =
            new com.android.internal.telephony.Connection.ListenerBase() {
        @Override
        public void onVideoStateChanged(int videoState) {
            setVideoState(videoState);
        }

        /**
         * The {@link com.android.internal.telephony.Connection} has reported a change in local
         * video capability.
         *
         * @param capable True if capable.
         */
        @Override
        public void onLocalVideoCapabilityChanged(boolean capable) {
            setLocalVideoCapable(capable);
        }

        /**
         * The {@link com.android.internal.telephony.Connection} has reported a change in remote
         * video capability.
         *
         * @param capable True if capable.
         */
        @Override
        public void onRemoteVideoCapabilityChanged(boolean capable) {
            setRemoteVideoCapable(capable);
        }

        /**
         * The {@link com.android.internal.telephony.Connection} has reported a change in the
         * video call provider.
         *
         * @param videoProvider The video call provider.
         */
        @Override
        public void onVideoProviderChanged(VideoProvider videoProvider) {
            setVideoProvider(videoProvider);
        }

        /**
         * Used by {@link com.android.internal.telephony.Connection} to report a change in whether
         * the call is being made over a wifi network.
         *
         * @param isWifi True if call is made over wifi.
         */
        @Override
        public void onWifiChanged(boolean isWifi) {
            setWifi(isWifi);
        }

        /**
         * Used by the {@link com.android.internal.telephony.Connection} to report a change in the
         * audio quality for the current call.
         *
         * @param audioQuality The audio quality.
         */
        @Override
        public void onAudioQualityChanged(int audioQuality) {
            setAudioQuality(audioQuality);
        }
        /**
         * Handles a change in the state of conference participant(s), as reported by the
         * {@link com.android.internal.telephony.Connection}.
         *
         * @param participants The participant(s) which changed.
         */
        @Override
        public void onConferenceParticipantsChanged(List<ConferenceParticipant> participants) {
            updateConferenceParticipants(participants);
        }
        /*
         * Handles a change to the multiparty state for this connection.
         *
         * @param isMultiParty {@code true} if the call became multiparty, {@code false}
         *      otherwise.
         */
        @Override
        public void onMultipartyStateChanged(boolean isMultiParty) {
            handleMultipartyStateChange(isMultiParty);
        }

        /**
         * Handles the event that the request to merge calls failed.
         */
        @Override
        public void onConferenceMergedFailed() {
            handleConferenceMergeFailed();
        }

        /// M: For VoLTE conference call. @{
        /**
         * For VoLTE enhanced conference call, notify invite conf. participants completed.
         * @param isSuccess is success or not.
         */
        @Override
        public void onConferenceParticipantsInvited(boolean isSuccess) {
            notifyConferenceParticipantsInvited(isSuccess);
        }

        /**
         * For VoLTE conference SRVCC, notify when new participant connections maded.
         * @param radioConnections new participant connections.
         */
        @Override
        public void onConferenceConnectionsConfigured(
                ArrayList<com.android.internal.telephony.Connection> radioConnections) {
            notifyConferenceConnectionsConfigured(radioConnections);
        }
        /// @}

        /// M: ALPS02256671. For PAU information changed. @{
        @Override
        public void onPauInfoUpdated(String pau) {
            Log.v(TelephonyConnection.this, "onPauInfoUpdated: %s", pau);
            Bundle bundle = new Bundle();
            bundle.putString(TelecomManagerEx.EXTRA_VOLTE_PAU_FIELD, pau);
            setCallInfo(bundle);
        }
        /// @}
    };

    private com.android.internal.telephony.Connection mOriginalConnection;
    private Call.State mOriginalConnectionState = Call.State.IDLE;

    private boolean mWasImsConnection;

    /**
     * Tracks the multiparty state of the ImsCall so that changes in the bit state can be detected.
     */
    private boolean mIsMultiParty = false;

    /**
     * Determines if the {@link TelephonyConnection} has local video capabilities.
     * This is used when {@link TelephonyConnection#updateConnectionCapabilities()}} is called,
     * ensuring the appropriate capabilities are set.  Since capabilities
     * can be rebuilt at any time it is necessary to track the video capabilities between rebuild.
     * The capabilities (including video capabilities) are communicated to the telecom
     * layer.
     */
    private boolean mLocalVideoCapable;

    /**
     * Determines if the {@link TelephonyConnection} has remote video capabilities.
     * This is used when {@link TelephonyConnection#updateConnectionCapabilities()}} is called,
     * ensuring the appropriate capabilities are set.  Since capabilities can be rebuilt at any time
     * it is necessary to track the video capabilities between rebuild. The capabilities (including
     * video capabilities) are communicated to the telecom layer.
     */
    private boolean mRemoteVideoCapable;

    /// M: CC034: Stop DTMF when TelephonyConnection is disconnected @{
    protected boolean mDtmfRequestIsStarted = false;
    /// @}

    /**
     * Determines if the {@link TelephonyConnection} is using wifi.
     * This is used when {@link TelephonyConnection#updateConnectionCapabilities} is called to
     * indicate wheter a call has the {@link Connection#CAPABILITY_WIFI} capability.
     */
    private boolean mIsWifi;

    /**
     * Determines the audio quality is high for the {@link TelephonyConnection}.
     * This is used when {@link TelephonyConnection#updateConnectionCapabilities}} is called to
     * indicate whether a call has the {@link Connection#CAPABILITY_HIGH_DEF_AUDIO} capability.
     */
    private boolean mHasHighDefAudio;

    /**
     * For video calls, indicates whether the outgoing video for the call can be paused using
     * the {@link android.telecom.VideoProfile#STATE_PAUSED} VideoState.
     */
    private boolean mIsVideoPauseSupported;

    /**
     * Listeners to our TelephonyConnection specific callbacks
     */
    private final Set<TelephonyConnectionListener> mTelephonyListeners = Collections.newSetFromMap(
            new ConcurrentHashMap<TelephonyConnectionListener, Boolean>(8, 0.9f, 1));

    protected TelephonyConnection(com.android.internal.telephony.Connection originalConnection) {
        if (originalConnection != null) {
            setOriginalConnection(originalConnection);
        }
    }

    /**
     * Creates a clone of the current {@link TelephonyConnection}.
     *
     * @return The clone.
     */
    public abstract TelephonyConnection cloneConnection();

    @Override
    public void onCallAudioStateChanged(CallAudioState audioState) {
        // TODO: update TTY mode.
        if (getPhone() != null) {
            getPhone().setEchoSuppressionEnabled();
        }
    }

    @Override
    public void onStateChanged(int state) {
        Log.v(this, "onStateChanged, state: " + Connection.stateToString(state));
        updateStatusHints();
    }

    @Override
    public void onDisconnect() {
        Log.v(this, "onDisconnect");
        hangup(android.telephony.DisconnectCause.LOCAL);
    }

    /**
     * Notifies this Connection of a request to disconnect a participant of the conference managed
     * by the connection.
     *
     * @param endpoint the {@link Uri} of the participant to disconnect.
     */
    @Override
    public void onDisconnectConferenceParticipant(Uri endpoint) {
        Log.v(this, "onDisconnectConferenceParticipant %s", endpoint);

        if (mOriginalConnection == null) {
            return;
        }

        mOriginalConnection.onDisconnectConferenceParticipant(endpoint);
    }

    @Override
    public void onSeparate() {
        Log.v(this, "onSeparate");
        if (mOriginalConnection != null) {
            try {
                mOriginalConnection.separate();
            } catch (CallStateException e) {
                Log.e(this, e, "Call to Connection.separate failed with exception");
            }
        }
    }

    @Override
    public void onAbort() {
        Log.v(this, "onAbort");
        hangup(android.telephony.DisconnectCause.LOCAL);
    }

    @Override
    public void onHold() {
        performHold();
    }

    @Override
    public void onUnhold() {
        performUnhold();
    }

    @Override
    public void onAnswer(int videoState) {
        Log.v(this, "onAnswer");
        if (isValidRingingCall() && getPhone() != null) {
            try {
                getPhone().acceptCall(videoState);
            } catch (CallStateException e) {
                Log.e(this, e, "Failed to accept call.");
            }
        }
    }

    @Override
    public void onReject() {
        Log.v(this, "onReject");
        if (isValidRingingCall()) {
            hangup(android.telephony.DisconnectCause.INCOMING_REJECTED);
        }
        super.onReject();
    }

    @Override
    public void onPostDialContinue(boolean proceed) {
        Log.v(this, "onPostDialContinue, proceed: " + proceed);
        if (mOriginalConnection != null) {
            if (proceed) {
                mOriginalConnection.proceedAfterWaitChar();
            } else {
                mOriginalConnection.cancelPostDial();
            }
        }
    }

    /// M: CC041: Interface for ECT @{
    @Override
    public void onExplicitCallTransfer() {
        Log.v(this, "onExplicitCallTransfer");
        Phone phone = mOriginalConnection.getCall().getPhone();
        try {
            phone.explicitCallTransfer();
        } catch (CallStateException e) {
            Log.e(this, e, "Exception occurred while trying to do ECT.");
        }
    }
    /// @}

    /// M: CC026: Interface for hangup all connections @{
    @Override
    public void onHangupAll() {
        Log.v(this, "onHangupAll");
        if (mOriginalConnection != null) {
            try {
                Phone phone = getPhone();
                if (phone != null) {
                    phone.hangupAll();
                } else {
                    Log.w(this, "Attempting to hangupAll a connection without backing phone.");
                }
            } catch (CallStateException e) {
                Log.e(this, e, "Call to phone.hangupAll() failed with exception");
            }
        }
    }
    /// @}

    /// M: CC042: [ALPS01850287] TelephonyConnection destroy itself upon user hanging up @{
    void onLocalDisconnected() {
        Log.v(this, "mOriginalConnection is null, local disconnect the call");
        /// M: ECC Retry @{
        if (mIsEmergency
                && TelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
            Log.d(this, "ECC Retry : clear ECC param");
            TelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
        }
        /// @}
        setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(
                android.telephony.DisconnectCause.LOCAL));
        close();
        updateConnectionCapabilities();
    }
    /// @}

    public void performHold() {
        Log.v(this, "performHold");
        // TODO: Can dialing calls be put on hold as well since they take up the
        // foreground call slot?
        if (Call.State.ACTIVE == mOriginalConnectionState) {
            Log.v(this, "Holding active call");
            try {
                Phone phone = mOriginalConnection.getCall().getPhone();
                Call ringingCall = phone.getRingingCall();

                // Although the method says switchHoldingAndActive, it eventually calls a RIL method
                // called switchWaitingOrHoldingAndActive. What this means is that if we try to put
                // a call on hold while a call-waiting call exists, it'll end up accepting the
                // call-waiting call, which is bad if that was not the user's intention. We are
                // cheating here and simply skipping it because we know any attempt to hold a call
                // while a call-waiting call is happening is likely a request from Telecom prior to
                // accepting the call-waiting call.
                // TODO: Investigate a better solution. It would be great here if we
                // could "fake" hold by silencing the audio and microphone streams for this call
                // instead of actually putting it on hold.
                if (ringingCall.getState() != Call.State.WAITING) {
                    phone.switchHoldingAndActive();
                }

                // TODO: Cdma calls are slightly different.
            } catch (CallStateException e) {
                Log.e(this, e, "Exception occurred while trying to put call on hold.");
            }
        } else {
            Log.w(this, "Cannot put a call that is not currently active on hold.");
        }
    }

    public void performUnhold() {
        Log.v(this, "performUnhold");
        if (Call.State.HOLDING == mOriginalConnectionState) {
            try {
                // Here's the deal--Telephony hold/unhold is weird because whenever there exists
                // more than one call, one of them must always be active. In other words, if you
                // have an active call and holding call, and you put the active call on hold, it
                // will automatically activate the holding call. This is weird with how Telecom
                // sends its commands. When a user opts to "unhold" a background call, telecom
                // issues hold commands to all active calls, and then the unhold command to the
                // background call. This means that we get two commands...each of which reduces to
                // switchHoldingAndActive(). The result is that they simply cancel each other out.
                // To fix this so that it works well with telecom we add a minor hack. If we
                // have one telephony call, everything works as normally expected. But if we have
                // two or more calls, we will ignore all requests to "unhold" knowing that the hold
                // requests already do what we want. If you've read up to this point, I'm very sorry
                // that we are doing this. I didn't think of a better solution that wouldn't also
                // make the Telecom APIs very ugly.

                if (!hasMultipleTopLevelCalls()) {
                    mOriginalConnection.getCall().getPhone().switchHoldingAndActive();
                } else {
                    Log.i(this, "Skipping unhold command for %s", this);
                }
            } catch (CallStateException e) {
                Log.e(this, e, "Exception occurred while trying to release call from hold.");
            }
        } else {
            Log.w(this, "Cannot release a call that is not already on hold from hold.");
        }
    }

    public void performConference(TelephonyConnection otherConnection) {
        Log.d(this, "performConference - %s", this);
        if (getPhone() != null) {
            try {
                // We dont use the "other" connection because there is no concept of that in the
                // implementation of calls inside telephony. Basically, you can "conference" and it
                // will conference with the background call.  We know that otherConnection is the
                // background call because it would never have called setConferenceableConnections()
                // otherwise.
                getPhone().conference();
            } catch (CallStateException e) {
                Log.e(this, e, "Failed to conference call.");
            }
        }
    }

    /**
     * Builds call capabilities common to all TelephonyConnections. Namely, apply IMS-based
     * capabilities.
     */
    protected int buildConnectionCapabilities() {
        int callCapabilities = 0;
        /// M: for 3GPP-CR C1-124200, ECC could not be held. @{
        // Put the logic into TelephonyConnectionService.canHold().
        /*
        if (isImsConnection()) {
            if (mOriginalConnection.isIncoming()) {
                callCapabilities |= CAPABILITY_SPEED_UP_MT_AUDIO;
            }
            callCapabilities |= CAPABILITY_SUPPORT_HOLD;
            if (getState() == STATE_ACTIVE || getState() == STATE_HOLDING) {
                callCapabilities |= CAPABILITY_HOLD;
            }
        }
        */
        /// @}
        // If the phone is in ECM mode, mark the call to indicate that the callback number should be
        // shown.
        Phone phone = getPhone();
        if (phone != null && phone.isInEcm()) {
            callCapabilities |= CAPABILITY_SHOW_CALLBACK_NUMBER;
        }
        return callCapabilities;
    }

    protected final void updateConnectionCapabilities() {
        int newCapabilities = buildConnectionCapabilities();

        newCapabilities = changeCapability(newCapabilities,
                CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL, mRemoteVideoCapable);
        newCapabilities = changeCapability(newCapabilities,
                CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL, mLocalVideoCapable);
        newCapabilities = changeCapability(newCapabilities,
                CAPABILITY_HIGH_DEF_AUDIO, mHasHighDefAudio);
        newCapabilities = changeCapability(newCapabilities, CAPABILITY_WIFI, mIsWifi);
        newCapabilities = changeCapability(newCapabilities, CAPABILITY_CAN_PAUSE_VIDEO,
                mIsVideoPauseSupported && mRemoteVideoCapable && mLocalVideoCapable);

        newCapabilities = applyConferenceTerminationCapabilities(newCapabilities);

        if (getConnectionCapabilities() != newCapabilities) {
            setConnectionCapabilities(newCapabilities);
        }
    }

    protected final void updateAddress() {
        updateConnectionCapabilities();
        if (mOriginalConnection != null) {
            Uri address = getAddressFromNumber(mOriginalConnection.getAddress());
            int presentation = mOriginalConnection.getNumberPresentation();
            if (!Objects.equals(address, getAddress()) ||
                    presentation != getAddressPresentation()) {
                Log.v(this, "updateAddress, address changed");
                setAddress(address, presentation);
            }

            String name = mOriginalConnection.getCnapName();
            int namePresentation = mOriginalConnection.getCnapNamePresentation();
            if (!Objects.equals(name, getCallerDisplayName()) ||
                    namePresentation != getCallerDisplayNamePresentation()) {
                Log.v(this, "updateAddress, caller display name changed");
                setCallerDisplayName(name, namePresentation);
            }
        }
    }

    void onRemovedFromCallService() {
        // Subclass can override this to do cleanup.
    }

    void setOriginalConnection(com.android.internal.telephony.Connection originalConnection) {
        Log.v(this, "new TelephonyConnection, originalConnection: " + originalConnection);
        clearOriginalConnection();

        mOriginalConnection = originalConnection;
        getPhone().registerForPreciseCallStateChanged(
                mHandler, MSG_PRECISE_CALL_STATE_CHANGED, null);
        getPhone().registerForHandoverStateChanged(
                mHandler, MSG_HANDOVER_STATE_CHANGED, null);
        getPhone().registerForRingbackTone(mHandler, MSG_RINGBACK_TONE, null);
        getPhone().registerForDisconnect(mHandler, MSG_DISCONNECT, null);
        /// M: CC031: Radio off notification @{
        getPhone().registerForRadioOffOrNotAvailable(
                mHandler, EVENT_RADIO_OFF_OR_NOT_AVAILABLE, null);
        /// @}
        /// M: CC077: 2/3G CAPABILITY_HIGH_DEF_AUDIO @{
        /// In order to align CS audio codec solution, we will register IMS connection to
        /// ImsPhone's default phone which is GSMPhone.
        if (getPhone() instanceof ImsPhone) {
            ((ImsPhone) getPhone()).getDefaultPhone().registerForSpeechCodecInfo(mHandler,
                    EVENT_SPEECH_CODEC_INFO, null);
        } else {
            getPhone().registerForSpeechCodecInfo(mHandler, EVENT_SPEECH_CODEC_INFO, null);
        }
        /// @}
        /// M: For 3G VT only @{
        getPhone().registerForVtStatusInfo(mHandler, EVENT_VT_STATUS_INFO, null);
        /// @}
        /// M: For CDMA call accepted @{
        getPhone().registerForCdmaCallAccepted(mHandler, EVENT_CDMA_CALL_ACCEPTED, null);
        /// @}

        /// M: Makes SuppMessageManager register for ImsPhone. @{
        registerSuppMessageManager(getPhone());
        /// @}

        mOriginalConnection.addPostDialListener(mPostDialListener);
        mOriginalConnection.addListener(mOriginalConnectionListener);

        // Set video state and capabilities
        setVideoState(mOriginalConnection.getVideoState());
        //updateState();
        setLocalVideoCapable(mOriginalConnection.isLocalVideoCapable());
        setRemoteVideoCapable(mOriginalConnection.isRemoteVideoCapable());
        setWifi(mOriginalConnection.isWifi());
        setVideoProvider(mOriginalConnection.getVideoProvider());
        setAudioQuality(mOriginalConnection.getAudioQuality());

        if (isImsConnection()) {
            mWasImsConnection = true;
        }
        mIsMultiParty = mOriginalConnection.isMultiparty();

        fireOnOriginalConnectionConfigured();
        updateAddress();
        /// M: If state is DISCONNECTED, mOriginalConnection will be set to null. @{
        updateState();
        /// @}
    }

    /**
     * Un-sets the underlying radio connection.
     */
    void clearOriginalConnection() {
        if (mOriginalConnection != null) {
            if (getPhone() != null) {
                /// M: CC034: Stop DTMF when TelephonyConnection is disconnected @{
                if (mDtmfRequestIsStarted) {
                    onStopDtmfTone();
                    mDtmfRequestIsStarted = false;
                }
                /// @}

                /// M: CC031: Radio off notification @{
                getPhone().unregisterForRadioOffOrNotAvailable(mHandler);
                /// @}
                /// M: CC077: 2/3G CAPABILITY_HIGH_DEF_AUDIO @{
                if (getPhone() instanceof ImsPhone) {
                    ((ImsPhone) getPhone()).getDefaultPhone()
                            .unregisterForSpeechCodecInfo(mHandler);
                } else {
                    getPhone().unregisterForSpeechCodecInfo(mHandler);
                }
                /// @}
                /// M: For 3G VT only @{
                getPhone().unregisterForVtStatusInfo(mHandler);
                /// @}
                /// M: For CDMA call accepted @{
                getPhone().unregisterForCdmaCallAccepted(mHandler);
                /// @}
                getPhone().unregisterForPreciseCallStateChanged(mHandler);
                getPhone().unregisterForRingbackTone(mHandler);
                getPhone().unregisterForHandoverStateChanged(mHandler);
                getPhone().unregisterForDisconnect(mHandler);
                /// M: Makes SuppMessageManager unregister for ImsPhone. @{
                unregisterSuppMessageManager(getPhone());
                /// @}
            }
            mOriginalConnection.removePostDialListener(mPostDialListener);
            mOriginalConnection.removeListener(mOriginalConnectionListener);
            mOriginalConnection = null;
        }
    }

    /// M: CC040: The telephonyDisconnectCode is not used here.
    protected void hangup(int telephonyDisconnectCode) {
        if (mOriginalConnection != null) {
            try {
                // Hanging up a ringing call requires that we invoke call.hangup() as opposed to
                // connection.hangup(). Without this change, the party originating the call will not
                // get sent to voicemail if the user opts to reject the call.
                if (isValidRingingCall()) {
                    Call call = getCall();
                    if (call != null) {
                        call.hangup();
                    } else {
                        Log.w(this, "Attempting to hangup a connection without backing call.");
                    }
                } else {
                    // We still prefer to call connection.hangup() for non-ringing calls in order
                    // to support hanging-up specific calls within a conference call. If we invoked
                    // call.hangup() while in a conference, we would end up hanging up the entire
                    // conference call instead of the specific connection.
                    mOriginalConnection.hangup();
                }
            } catch (CallStateException e) {
                Log.e(this, e, "Call to Connection.hangup failed with exception");
            }
        }
        /// M: CC042: [ALPS01850287] TelephonyConnection destroy itself upon user hanging up @{
        else {
            onLocalDisconnected();
        }
        /// @}
    }

    /// M: CC030: CRSS notification @{
    // Declare as public for SuppMessageManager to use.
    public com.android.internal.telephony.Connection getOriginalConnection() {
    /// @}
        return mOriginalConnection;
    }

    protected Call getCall() {
        if (mOriginalConnection != null) {
            return mOriginalConnection.getCall();
        }
        return null;
    }

    Phone getPhone() {
        Call call = getCall();
        if (call != null) {
            return call.getPhone();
        }
        return null;
    }

    private boolean hasMultipleTopLevelCalls() {
        int numCalls = 0;
        Phone phone = getPhone();
        if (phone != null) {
            if (!phone.getRingingCall().isIdle()) {
                numCalls++;
            }
            if (!phone.getForegroundCall().isIdle()) {
                numCalls++;
            }
            if (!phone.getBackgroundCall().isIdle()) {
                numCalls++;
            }
        }
        return numCalls > 1;
    }

    private com.android.internal.telephony.Connection getForegroundConnection() {
        if (getPhone() != null) {
            return getPhone().getForegroundCall().getEarliestConnection();
        }
        return null;
    }

     /**
     * Checks for and returns the list of conference participants
     * associated with this connection.
     */
    public List<ConferenceParticipant> getConferenceParticipants() {
        if (mOriginalConnection == null) {
            Log.v(this, "Null mOriginalConnection, cannot get conf participants.");
            return null;
        }
        return mOriginalConnection.getConferenceParticipants();
    }

    /**
     * Checks to see the original connection corresponds to an active incoming call. Returns false
     * if there is no such actual call, or if the associated call is not incoming (See
     * {@link Call.State#isRinging}).
     */
    private boolean isValidRingingCall() {
        if (getPhone() == null) {
            Log.v(this, "isValidRingingCall, phone is null");
            return false;
        }

        Call ringingCall = getPhone().getRingingCall();
        if (!ringingCall.getState().isRinging()) {
            Log.v(this, "isValidRingingCall, ringing call is not in ringing state");
            return false;
        }

        if (ringingCall.getEarliestConnection() != mOriginalConnection) {
            Log.v(this, "isValidRingingCall, ringing call connection does not match");
            return false;
        }

        Log.v(this, "isValidRingingCall, returning true");
        return true;
    }

    /// M: CC046: Force updateState for Connection once its ConnectionService is set @{
    @Override
    protected void fireOnCallState() {
        updateState();
    }
    /// @}

    void updateState() {
       updateState(false);
    }

    void updateState(boolean force) {
        if (mOriginalConnection == null) {
            return;
        }

        Call.State newState = mOriginalConnection.getState();
        Log.v(this, "Update state from %s to %s for %s", mOriginalConnectionState, newState, this);
        if (mOriginalConnectionState != newState || force) {
            mOriginalConnectionState = newState;
            /// M: CC077: 2/3G CAPABILITY_HIGH_DEF_AUDIO @{
            // Force update when call state is changed to DIALING/ACTIVE/INCOMING(Foreground)
            switch (newState) {
                case ACTIVE:
                case DIALING:
                case ALERTING:
                case INCOMING:
                case WAITING:
                    if (TelephonyConnectionServiceUtil.getInstance().isHighDefAudio(0)) {
                        setAudioQuality(com.android.internal.telephony.Connection.
                                AUDIO_QUALITY_HIGH_DEFINITION);
                    } else {
                        setAudioQuality(com.android.internal.telephony.Connection.
                                AUDIO_QUALITY_STANDARD);
                    }
                    break;
                default:
                    break;
            }
            /// @}

            switch (newState) {
                case IDLE:
                    break;
                case ACTIVE:
                    /// M: ECC Retry @{
                    // Assume only one ECC exists
                    if (mIsEmergency
                            && TelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                        Log.d(this, "ECC Retry : clear ECC param");
                        TelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
                    }
                    /// @}
                    setActiveInternal();
                    break;
                case HOLDING:
                    setOnHold();
                    break;
                case DIALING:
                case ALERTING:
                    setDialing();
                    break;
                case INCOMING:
                case WAITING:
                    setRinging();
                    break;
                case DISCONNECTED:
                    /// M: ECC Retry @{
                    // Assume only one ECC exists
                    if (mIsEmergency
                            && TelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                        Log.d(this, "ECC Retry : clear ECC param");
                        TelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
                    }
                    /// @}
                    setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(
                            mOriginalConnection.getDisconnectCause(),
                            mOriginalConnection.getVendorDisconnectCause()));
                    close();
                    break;
                case DISCONNECTING:
                    break;
            }
        }
        updateStatusHints();
        /// M: @{
        // Remove duplicate updateConnectionCapabilities(), for it's called in updateAddress()
        //updateConnectionCapabilities();
        /// @}
        updateAddress();
        updateMultiparty();
    }

    /**
     * Checks for changes to the multiparty bit.  If a conference has started, informs listeners.
     */
    private void updateMultiparty() {
        if (mOriginalConnection == null) {
            return;
        }

        if (mIsMultiParty != mOriginalConnection.isMultiparty()) {
            mIsMultiParty = mOriginalConnection.isMultiparty();

            if (mIsMultiParty) {
                notifyConferenceStarted();
            }
        }
    }

    /**
     * Handles a failure when merging calls into a conference.
     * {@link com.android.internal.telephony.Connection.Listener#onConferenceMergedFailed()}
     * listener.
     */
    private void handleConferenceMergeFailed(){
        mHandler.obtainMessage(MSG_CONFERENCE_MERGE_FAILED).sendToTarget();
    }

    /**
     * Handles requests to update the multiparty state received via the
     * {@link com.android.internal.telephony.Connection.Listener#onMultipartyStateChanged(boolean)}
     * listener.
     * <p>
     * Note: We post this to the mHandler to ensure that if a conference must be created as a
     * result of the multiparty state change, the conference creation happens on the correct
     * thread.  This ensures that the thread check in
     * {@link com.android.internal.telephony.PhoneBase#checkCorrectThread(android.os.Handler)}
     * does not fire.
     *
     * @param isMultiParty {@code true} if this connection is multiparty, {@code false} otherwise.
     */
    private void handleMultipartyStateChange(boolean isMultiParty) {
        Log.i(this, "Update multiparty state to %s", isMultiParty ? "Y" : "N");
        mHandler.obtainMessage(MSG_MULTIPARTY_STATE_CHANGED, isMultiParty).sendToTarget();
    }

    private void setActiveInternal() {
        if (getState() == STATE_ACTIVE) {
            Log.w(this, "Should not be called if this is already ACTIVE");
            return;
        }

        // When we set a call to active, we need to make sure that there are no other active
        // calls. However, the ordering of state updates to connections can be non-deterministic
        // since all connections register for state changes on the phone independently.
        // To "optimize", we check here to see if there already exists any active calls.  If so,
        // we issue an update for those calls first to make sure we only have one top-level
        // active call.
        if (getConnectionService() != null) {
            for (Connection current : getConnectionService().getAllConnections()) {
                if (current != this && current instanceof TelephonyConnection) {
                    TelephonyConnection other = (TelephonyConnection) current;
                    if (other.getState() == STATE_ACTIVE) {
                        other.updateState();
                    }
                }
            }
        }

        /// M: Add flag to indicate if the CDMA call is fake dialing @{
        // If the cdma connection is in FORCE DIALING status,
        // not update the state to ACTIVE, this will be done
        // after stop the FORCE DIALING
        if (isForceDialing()) {
            return;
        }
        /// @}
        setActive();
    }

    private void close() {
        Log.v(this, "close");
        clearOriginalConnection();
        destroy();
    }

    /**
     * Applies capabilities specific to conferences termination to the
     * {@code CallCapabilities} bit-mask.
     *
     * @param capabilities The {@code CallCapabilities} bit-mask.
     * @return The capabilities with the IMS conference capabilities applied.
     */
    private int applyConferenceTerminationCapabilities(int capabilities) {
        int currentCapabilities = capabilities;

        // An IMS call cannot be individually disconnected or separated from its parent conference.
        // If the call was IMS, even if it hands over to GMS, these capabilities are not supported.
        /// M: CC085 Fix Google bug. Check IMS current status for CS conference call @{
        //if (!mWasImsConnection) {
        if (!isImsConnection()) {
        /// @}
            currentCapabilities |= CAPABILITY_DISCONNECT_FROM_CONFERENCE;
            currentCapabilities |= CAPABILITY_SEPARATE_FROM_CONFERENCE;
        }

        return currentCapabilities;
    }

    /**
     * Returns the local video capability state for the connection.
     *
     * @return {@code True} if the connection has local video capabilities.
     */
    public boolean isLocalVideoCapable() {
        return mLocalVideoCapable;
    }

    /**
     * Returns the remote video capability state for the connection.
     *
     * @return {@code True} if the connection has remote video capabilities.
     */
    public boolean isRemoteVideoCapable() {
        return mRemoteVideoCapable;
    }

    /**
     * Sets whether video capability is present locally.  Used during rebuild of the
     * capabilities to set the video call capabilities.
     *
     * @param capable {@code True} if video capable.
     */
    public void setLocalVideoCapable(boolean capable) {
        mLocalVideoCapable = capable;
        updateConnectionCapabilities();
    }

    /**
     * Sets whether video capability is present remotely.  Used during rebuild of the
     * capabilities to set the video call capabilities.
     *
     * @param capable {@code True} if video capable.
     */
    public void setRemoteVideoCapable(boolean capable) {
        mRemoteVideoCapable = capable;
        updateConnectionCapabilities();
    }

    /**
     * Sets whether the call is using wifi. Used when rebuilding the capabilities to set or unset
     * the {@link Connection#CAPABILITY_WIFI} capability.
     */
    public void setWifi(boolean isWifi) {
        mIsWifi = isWifi;
        updateConnectionCapabilities();
        updateStatusHints();
    }

    /**
     * Whether the call is using wifi.
     */
    boolean isWifi() {
        return mIsWifi;
    }

    /**
     * Sets the current call audio quality. Used during rebuild of the capabilities
     * to set or unset the {@link Connection#CAPABILITY_HIGH_DEF_AUDIO} capability.
     *
     * @param audioQuality The audio quality.
     */
    public void setAudioQuality(int audioQuality) {
        mHasHighDefAudio = audioQuality ==
                com.android.internal.telephony.Connection.AUDIO_QUALITY_HIGH_DEFINITION;
        updateConnectionCapabilities();
    }

    void resetStateForConference() {
        if (getState() == Connection.STATE_HOLDING) {
            if (mOriginalConnection.getState() == Call.State.ACTIVE) {
                setActive();
            }
        }
    }

    boolean setHoldingForConference() {
        if (getState() == Connection.STATE_ACTIVE) {
            setOnHold();
            return true;
        }
        return false;
    }

    /**
     * For video calls, sets whether this connection supports pausing the outgoing video for the
     * call using the {@link android.telecom.VideoProfile#STATE_PAUSED} VideoState.
     *
     * @param isVideoPauseSupported {@code true} if pause state supported, {@code false} otherwise.
     */
    public void setVideoPauseSupported(boolean isVideoPauseSupported) {
        mIsVideoPauseSupported = isVideoPauseSupported;
    }

    /**
     * Whether the original connection is an IMS connection.
     * @return {@code True} if the original connection is an IMS connection, {@code false}
     *     otherwise.
     */
    protected boolean isImsConnection() {
        return getOriginalConnection() instanceof ImsPhoneConnection;
    }

    /**
     * Whether the original connection was ever an IMS connection, either before or now.
     * @return {@code True} if the original connection was ever an IMS connection, {@code false}
     *     otherwise.
     */
    public boolean wasImsConnection() {
        return mWasImsConnection;
    }

    private static Uri getAddressFromNumber(String number) {
        // Address can be null for blocked calls.
        if (number == null) {
            number = "";
        }
        return Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null);
    }

    /**
     * Changes a capabilities bit-mask to add or remove a capability.
     *
     * @param capabilities The capabilities bit-mask.
     * @param capability The capability to change.
     * @param enabled Whether the capability should be set or removed.
     * @return The capabilities bit-mask with the capability changed.
     */
    private int changeCapability(int capabilities, int capability, boolean enabled) {
        if (enabled) {
            return capabilities | capability;
        } else {
            return capabilities & ~capability;
        }
    }

    private void updateStatusHints() {
        boolean isIncoming = isValidRingingCall();
        if (mIsWifi && (isIncoming || getState() == STATE_ACTIVE)) {
            int labelId = isIncoming
                    ? R.string.status_hint_label_incoming_wifi_call
                    : R.string.status_hint_label_wifi_call;

            Context context = getPhone().getContext();
            setStatusHints(new StatusHints(
                    context.getString(labelId),
                    Icon.createWithResource(
                            context.getResources(),
                            R.drawable.ic_signal_wifi_4_bar_24dp),
                    null /* extras */));
        } else {
            setStatusHints(null);
        }
    }

    /**
     * Register a listener for {@link TelephonyConnection} specific triggers.
     * @param l The instance of the listener to add
     * @return The connection being listened to
     */
    public final TelephonyConnection addTelephonyConnectionListener(TelephonyConnectionListener l) {
        mTelephonyListeners.add(l);
        // If we already have an original connection, let's call back immediately.
        // This would be the case for incoming calls.
        if (mOriginalConnection != null) {
            fireOnOriginalConnectionConfigured();
        }
        return this;
    }

    /**
     * Remove a listener for {@link TelephonyConnection} specific triggers.
     * @param l The instance of the listener to remove
     * @return The connection being listened to
     */
    public final TelephonyConnection removeTelephonyConnectionListener(
            TelephonyConnectionListener l) {
        if (l != null) {
            mTelephonyListeners.remove(l);
        }
        return this;
    }

    /**
     * Fire a callback to the various listeners for when the original connection is
     * set in this {@link TelephonyConnection}
     */
    private final void fireOnOriginalConnectionConfigured() {
        for (TelephonyConnectionListener l : mTelephonyListeners) {
            l.onOriginalConnectionConfigured(this);
        }
    }

    /**
     * Creates a string representation of this {@link TelephonyConnection}.  Primarily intended for
     * use in log statements.
     *
     * @return String representation of the connection.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[TelephonyConnection objId:");
        sb.append(System.identityHashCode(this));
        sb.append(" type:");
        if (isImsConnection()) {
            sb.append("ims");
        } else if (this instanceof com.android.services.telephony.GsmConnection) {
            sb.append("gsm");
        } else if (this instanceof CdmaConnection) {
            sb.append("cdma");
        }
        sb.append(" state:");
        sb.append(Connection.stateToString(getState()));
        sb.append(" capabilities:");
        sb.append(capabilitiesToString(getConnectionCapabilities()));
        sb.append(" address:");
        sb.append(Log.pii(getAddress()));
        sb.append(" originalConnection:");
        sb.append(mOriginalConnection);
        sb.append(" partOfConf:");
        if (getConference() == null) {
            sb.append("N");
        } else {
            sb.append("Y");
        }
        sb.append("]");
        return sb.toString();
    }

    /// M: @{
    /**
     * Notify this connection is ECC.
     */
    public void notifyEcc() {
        Bundle bundleECC = new Bundle();
        bundleECC.putBoolean(TelecomManagerEx.EXTRA_VOLTE_MARKED_AS_EMERGENCY, true);
        setCallInfo(bundleECC);
    }
    /// @}

    /// M: For VoLTE enhanced conference call. @{
    void performInviteConferenceParticipants(List<String> numbers) {
        if (mOriginalConnection == null) {
            Log.e(this, new CallStateException(), "no orginal connection to inviteParticipants");
            return;
        }

        if (!isImsConnection()) {
            Log.e(this, new CallStateException(), "CS connection doesn't support invite!");
            return;
        }

        ((ImsPhoneConnection) mOriginalConnection).onInviteConferenceParticipants(numbers);
    }

    /**
     * This function used to notify the onInviteConferenceParticipants() operation is done.
     * @param isSuccess is success or not
     * @hide
     */
    protected void notifyConferenceParticipantsInvited(boolean isSuccess) {
        for (TelephonyConnectionListener l : mTelephonyListeners) {
            l.onConferenceParticipantsInvited(isSuccess);
        }
    }

    /**
     * For conference SRVCC.
     * @param radioConnections new participant connections
     */
    private void notifyConferenceConnectionsConfigured(
            ArrayList<com.android.internal.telephony.Connection> radioConnections) {
        for (TelephonyConnectionListener l : mTelephonyListeners) {
            l.onConferenceConnectionsConfigured(radioConnections);
        }
    }
    /// @}

    /// M: register/unregister ImsPhone to SuppMessageManager @{
    private void registerSuppMessageManager(Phone phone) {
        if ((phone instanceof ImsPhone) != true) {
            return;
        }
        TelephonyConnectionServiceUtil.getInstance().registerSuppMessageForImsPhone(phone);
    }

    private void unregisterSuppMessageManager(Phone phone) {
        if ((phone instanceof ImsPhone) != true) {
            return;
        }
        TelephonyConnectionServiceUtil.getInstance().unregisterSuppMessageForImsPhone(phone);
    }
    /// @}

    /// M: ALPS02136977. Prints debug logs for telephony. @{
    @Override
    protected FormattedLog.Builder configDumpLogBuilder(FormattedLog.Builder builder) {
        if (builder == null) {
            return null;
        }

        // Dont dump info for muliti-party ims connection, since it is a conference host connection.
        if (mIsMultiParty && isImsConnection()) {
            // But there is an exception case for the conference host connection disconnected in
            // ImsConferenceController.startConference().
            android.telecom.DisconnectCause cause = DisconnectCauseUtil.toTelecomDisconnectCause(
                android.telephony.DisconnectCause.IMS_MERGED_SUCCESSFULLY);
            if (getState() != Connection.STATE_DISCONNECTED || getDisconnectCause() != null ||
                    !getDisconnectCause().equals(cause)) {
                return null;
            }
        }

        super.configDumpLogBuilder(builder);

        /// M: ALPS02288178. mAddress might be null. @{
        if (getAddress() == null) {
            if (mOriginalConnection != null
                    && mOriginalConnection.getAddress() != null) {
                builder.setCallNumber(mOriginalConnection.getAddress());
            }
        }
        ///@}

        builder.setServiceName("Telephony");
        if (isImsConnection()) {
            builder.setStatusInfo("type", "ims");
            //builder.setStatusInfo("isWifi", isWifi());
        } else if (this instanceof com.android.services.telephony.GsmConnection) {
            builder.setStatusInfo("type", "gsm");
        } else if (this instanceof CdmaConnection) {
            builder.setStatusInfo("type", "cdma");
        }

        return builder;
    }

    /**
     * Logs unified debug log messages, for "Notify".
     * Format: [category][Module][Notify][Action][call-number][local-call-ID] Msg. String
     *
     * @param action the action name. (e.q. Dial, Hold, MT, Onhold, etc.)
     * @param msg the optional messages
     * @hide
     */
    private void logDebugMsgWithNotifyFormat(String action, String msg) {
        if (action == null) {
            return;
        }

        FormattedLog formattedLog = new FormattedLog.Builder()
                .setCategory("CC")
                .setServiceName("Telephony")
                .setOpType(FormattedLog.OpType.NOTIFY)
                .setActionName(action)
                .setCallNumber(getAddress().getSchemeSpecificPart())
                .setCallId(Integer.toString(System.identityHashCode(this)))
                .setExtraMessage(msg)
                .buildDebugMsg();
        if (formattedLog != null) {
            Log.v(this, formattedLog.toString());
        }
    }

    /**
     * Translate from Call.State to notify action string.
     *
     * @param state Call.State
     * @return formatted notify action string.
     * @hide
     */
    static String callStateToFormattedNotifyString(Call.State state) {
        switch (state) {
            case ACTIVE:
                return "Active";
            case HOLDING:
                return "Onhold";
            case DIALING:
            case ALERTING:
                return "Alerting";
            case INCOMING:
            case WAITING:
                return "MT";
            case DISCONNECTED:
                return "Disconnected";

            case IDLE:
            case DISCONNECTING:
            default:
                return null;    // Ignore these states.
        }
    }
    /// @}

    /// M: Add flag to indicate if the CDMA call is fake dialing @{
    /**
     * Used for cdma special handle.
     * @return
     */
    boolean isForceDialing() {
        return false;
    }
    /// @}

    /// M: ECC Retry @{
    protected void setEmergency(boolean isEmergency) {
        mIsEmergency = isEmergency;
        Log.d(this, "ECC Retry : mIsEmergency = " + mIsEmergency);
    }
    /// @}
}
