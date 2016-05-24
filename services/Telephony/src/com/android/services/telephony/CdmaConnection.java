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

import android.os.Handler;
import android.os.Message;
/// M: @{
import android.os.SystemProperties;
/// @}

import android.provider.Settings;
import android.telephony.DisconnectCause;
import android.telephony.PhoneNumberUtils;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;

import com.android.phone.settings.SettingsConstants;
/// M: @{
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyProperties;
/// @}

import java.util.LinkedList;
import java.util.Queue;

/**
 * Manages a single phone call handled by CDMA.
 */
final class CdmaConnection extends TelephonyConnection {

    private static final int MSG_CALL_WAITING_MISSED = 1;
    private static final int MSG_DTMF_SEND_CONFIRMATION = 2;
    private static final int TIMEOUT_CALL_WAITING_MILLIS = 20 * 1000;
    /// M: cdma call fake hold handling. @{
    private static final int MSG_CDMA_CALL_SWITCH = 3;
    private static final int MSG_CDMA_CALL_SWITCH_DELAY = 200;
    private static final int FAKE_HOLD = 1;
    private static final int FAKE_UNHOLD = 0;
    /// @}

    private final Handler mHandler = new Handler() {

        /** ${inheritDoc} */
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CALL_WAITING_MISSED:
                    hangupCallWaiting(DisconnectCause.INCOMING_MISSED);
                    break;
                case MSG_DTMF_SEND_CONFIRMATION:
                    handleBurstDtmfConfirmation();
                    break;
                /// M: cdma call fake hold handling. @{
                case MSG_CDMA_CALL_SWITCH:
                    handleFakeHold(msg.arg1);
                    break;
                /// @}
                default:
                    break;
            }
        }

    };

    /**
     * {@code True} if the CDMA connection should allow mute.
     */
    /// M: Not allow mute in ECBM and update after exit ECBM @{
    //private final boolean mAllowMute;
    private boolean mAllowMute;
    /// @}
    private final boolean mIsOutgoing;
    // Queue of pending short-DTMF characters.
    private final Queue<Character> mDtmfQueue = new LinkedList<>();
    private final EmergencyTonePlayer mEmergencyTonePlayer;

    // Indicates that the DTMF confirmation from telephony is pending.
    private boolean mDtmfBurstConfirmationPending = false;
    private boolean mIsCallWaiting;

    /// M: Add flag to indicate if the CDMA call is fake dialing @{
    // For cdma third part call, if the second call is MO call,
    // the state will changed to ACTIVE during force dialing,
    // so need to check if need to update the ACTIVE to telecom.
    private boolean mIsForceDialing = false;
    /// @}

    CdmaConnection(
            Connection connection,
            EmergencyTonePlayer emergencyTonePlayer,
            boolean allowMute,
            boolean isOutgoing) {
        super(connection);
        mEmergencyTonePlayer = emergencyTonePlayer;
        mAllowMute = allowMute;
        mIsOutgoing = isOutgoing;
        mIsCallWaiting = connection != null && connection.getState() == Call.State.WAITING;
        if (mIsCallWaiting) {
            startCallWaitingTimer();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onPlayDtmfTone(char digit) {
        if (useBurstDtmf()) {
            Log.i(this, "sending dtmf digit as burst");
            sendShortDtmfToNetwork(digit);
        } else {
            Log.i(this, "sending dtmf digit directly");
            getPhone().startDtmf(digit);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onStopDtmfTone() {
        if (!useBurstDtmf()) {
            getPhone().stopDtmf();
        }
    }

    @Override
    public void onReject() {
        Connection connection = getOriginalConnection();
        if (connection != null) {
            switch (connection.getState()) {
                case INCOMING:
                    // Normal ringing calls are handled the generic way.
                    super.onReject();
                    break;
                case WAITING:
                    hangupCallWaiting(DisconnectCause.INCOMING_REJECTED);
                    break;
                default:
                    Log.e(this, new Exception(), "Rejecting a non-ringing call");
                    // might as well hang this up, too.
                    super.onReject();
                    break;
            }
        }
    }

    @Override
    public void onAnswer() {
        mHandler.removeMessages(MSG_CALL_WAITING_MISSED);
        super.onAnswer();
    }

    /**
     * Clones the current {@link CdmaConnection}.
     * <p>
     * Listeners are not copied to the new instance.
     *
     * @return The cloned connection.
     */
    @Override
    public TelephonyConnection cloneConnection() {
        CdmaConnection cdmaConnection = new CdmaConnection(getOriginalConnection(),
                mEmergencyTonePlayer, mAllowMute, mIsOutgoing);
        return cdmaConnection;
    }

    @Override
    public void onStateChanged(int state) {
        Connection originalConnection = getOriginalConnection();
        mIsCallWaiting = originalConnection != null &&
                originalConnection.getState() == Call.State.WAITING;

        if (mEmergencyTonePlayer != null) {
            if (state == android.telecom.Connection.STATE_DIALING) {
                if (isEmergency()) {
                    mEmergencyTonePlayer.start();
                }
            } else {
                // No need to check if it is an emergency call, since it is a no-op if it
                // isn't started.
                mEmergencyTonePlayer.stop();
            }
        }

        super.onStateChanged(state);
    }

    @Override
    protected int buildConnectionCapabilities() {
        int capabilities = super.buildConnectionCapabilities();
        /// M: cdma call fake hold handling. @{
        // Google default don't support HOLD for CDMA call
        boolean isRealConnected = false;
        if (getOriginalConnection() instanceof
                com.android.internal.telephony.cdma.CdmaConnection) {
            com.android.internal.telephony.cdma.CdmaConnection cc =
                    (com.android.internal.telephony.cdma.CdmaConnection) getOriginalConnection();
            if (cc != null) {
                isRealConnected = cc.isRealConnected();
            }
        } else {
            Log.e(this, new Exception(), "buildConnectionCapabilities, not CdmaConnection");
        }
        Log.d(this, "buildConnectionCapabilities, isRealConnected:" + isRealConnected);
        capabilities |= CAPABILITY_SUPPORT_HOLD;
        if ((getState() == STATE_ACTIVE
                && ((mIsOutgoing && isRealConnected && !isEmergency()) || !mIsOutgoing))
                || getState() == STATE_HOLDING) {
            capabilities |= CAPABILITY_HOLD;
        }
        /// @}

        /// M: Not allow mute in ECBM and update after exit ECBM @{
        String inEcm = SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE, "false");
        mAllowMute = inEcm.equals("false");
        /// @}

        if (mAllowMute) {
            capabilities |= CAPABILITY_MUTE;
        }
        return capabilities;
    }

    @Override
    public void performConference(TelephonyConnection otherConnection) {
        if (isImsConnection()) {
            super.performConference(otherConnection);
        } else {
            Log.w(this, "Non-IMS CDMA Connection attempted to call performConference.");
        }
    }

    void forceAsDialing(boolean isDialing) {
        if (isDialing) {
            setDialing();
            /// M: Add flag to indicate if the CDMA call is fake dialing @{
            mIsForceDialing = true;
            /// @}
        } else {
            /// M: Add flag to indicate if the CDMA call is fake dialing @{
            mIsForceDialing = false;
            updateState(true);
            //updateState();
            /// @}
        }
    }

    boolean isOutgoing() {
        return mIsOutgoing;
    }

    boolean isCallWaiting() {
        return mIsCallWaiting;
    }

    /**
     * We do not get much in the way of confirmation for Cdma call waiting calls. There is no
     * indication that a rejected call succeeded, a call waiting call has stopped. Instead we
     * simulate this for the user. We allow TIMEOUT_CALL_WAITING_MILLIS milliseconds before we
     * assume that the call was missed and reject it ourselves. reject the call automatically.
     */
    private void startCallWaitingTimer() {
        mHandler.sendEmptyMessageDelayed(MSG_CALL_WAITING_MISSED, TIMEOUT_CALL_WAITING_MILLIS);
    }

    private void hangupCallWaiting(int telephonyDisconnectCause) {
        Connection originalConnection = getOriginalConnection();
        if (originalConnection != null) {
            try {
                originalConnection.hangup();
            } catch (CallStateException e) {
                Log.e(this, e, "Failed to hangup call waiting call");
            }
            setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(telephonyDisconnectCause));
        }
    }

    /**
     * Read the settings to determine which type of DTMF method this CDMA phone calls.
     */
    private boolean useBurstDtmf() {
        if (isImsConnection()) {
            Log.d(this, "in ims call, return false");
            return false;
        }
        int dtmfTypeSetting = Settings.System.getInt(
                getPhone().getContext().getContentResolver(),
                Settings.System.DTMF_TONE_TYPE_WHEN_DIALING,
                SettingsConstants.DTMF_TONE_TYPE_NORMAL);
        return dtmfTypeSetting == SettingsConstants.DTMF_TONE_TYPE_NORMAL;
    }

    private void sendShortDtmfToNetwork(char digit) {
        synchronized(mDtmfQueue) {
            if (mDtmfBurstConfirmationPending) {
                mDtmfQueue.add(new Character(digit));
            } else {
                sendBurstDtmfStringLocked(Character.toString(digit));
            }
        }
    }

    private void sendBurstDtmfStringLocked(String dtmfString) {
        /// M: Add null check to avoid timing issue. @{
        Phone phone = getPhone();
        if (phone != null) {
            phone.sendBurstDtmf(
                    dtmfString, 0, 0, mHandler.obtainMessage(MSG_DTMF_SEND_CONFIRMATION));
            mDtmfBurstConfirmationPending = true;
        }
        /// @}
    }

    private void handleBurstDtmfConfirmation() {
        String dtmfDigits = null;
        synchronized(mDtmfQueue) {
            mDtmfBurstConfirmationPending = false;
            if (!mDtmfQueue.isEmpty()) {
                StringBuilder builder = new StringBuilder(mDtmfQueue.size());
                while (!mDtmfQueue.isEmpty()) {
                    builder.append(mDtmfQueue.poll());
                }
                dtmfDigits = builder.toString();

                // It would be nice to log the digit, but since DTMF digits can be passwords
                // to things, or other secure account numbers, we want to keep it away from
                // the logs.
                Log.i(this, "%d dtmf character[s] removed from the queue", dtmfDigits.length());
            }
            if (dtmfDigits != null) {
                sendBurstDtmfStringLocked(dtmfDigits);
            }
        }
    }

    private boolean isEmergency() {
        Phone phone = getPhone();
        return phone != null &&
                PhoneNumberUtils.isLocalEmergencyNumber(
                    phone.getContext(), getAddress().getSchemeSpecificPart());
    }

    /// M: CC026: Interface for hangup all connections @{
    /**
     * CDMA hangup all is different to GSM, no CHLD=6 to hang up all calls in a phone
     */
    @Override
    public void onHangupAll() {
        Log.v(this, "onHangupAll");
        if (getOriginalConnection() != null) {
            try {
                Call call = getOriginalConnection().getCall();
                if (call != null) {
                    call.hangup();
                } else {
                    Log.w(this, "Attempting to hangupAll a connection without backing phone.");
                }
            } catch (CallStateException e) {
                Log.e(this, e, "Call to phone.hangupAll() failed with exception");
            }
        }
    }
    /// @}

    /// M: cdma call fake hold handling. @{
    public void performHold() {
        Log.d(this, "performHold");
        Log.d(this, "donothing, just set the hold status.");
        mHandler.sendMessageDelayed(Message.obtain(mHandler, MSG_CDMA_CALL_SWITCH, FAKE_HOLD, 0),
                MSG_CDMA_CALL_SWITCH_DELAY);
    }

    public void performUnhold() {
        Log.d(this, "performUnhold");
        Log.d(this, "donothing, just set the active status.");
        mHandler.sendMessageDelayed(Message.obtain(mHandler, MSG_CDMA_CALL_SWITCH, FAKE_UNHOLD, 0),
                MSG_CDMA_CALL_SWITCH_DELAY);
    }

    void handleFakeHold(int fakeOp) {
        Log.d(this, "handleFakeHold with operation %s", fakeOp);
        if (FAKE_HOLD == fakeOp) {
            setOnHold();
        } else if (FAKE_UNHOLD == fakeOp) {
            setActive();
        }
        fireOnCallState();
    }
    /// @}

    /// M: Add flag to indicate if the CDMA call is fake dialing @{
    /**
     * Used for cdma special handle.
     * @return
     */
    @Override
    boolean isForceDialing() {
        return mIsForceDialing;
    }
    /// @}
}
