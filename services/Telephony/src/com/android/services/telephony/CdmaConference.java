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

import android.content.Context;
import android.content.res.Resources;
/// M: @{
import android.os.Handler;
import android.os.Message;
/// @}
import android.os.PersistableBundle;
/// M: @{
import android.os.SystemProperties;
/// @}
import android.telecom.Conference;
import android.telecom.Connection;
import android.telecom.PhoneAccountHandle;
import android.telephony.CarrierConfigManager;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
/// M: @{
import com.android.internal.telephony.TelephonyProperties;
/// @}
import com.android.phone.PhoneGlobals;
import com.android.phone.common.R;

import java.util.List;

/**
 * CDMA-based conference call.
 */
public class CdmaConference extends Conference {
    private int mCapabilities;

    public CdmaConference(PhoneAccountHandle phoneAccount) {
        super(phoneAccount);
        setActive();
    }

    public void updateCapabilities(int capabilities) {
        capabilities |= Connection.CAPABILITY_MUTE | Connection.CAPABILITY_GENERIC_CONFERENCE;
        /// M: Not allow mute in ECBM and update after exit ECBM @{
        mCapabilities |= capabilities;
        /// @}
        setConnectionCapabilities(buildConnectionCapabilities());
    }

    /// M: For CDMA conference @{
    public void removeCapabilities(int capabilities) {
        mCapabilities &= ~capabilities;
        setConnectionCapabilities(buildConnectionCapabilities());
    }
    /// @}

    /**
     * Invoked when the Conference and all it's {@link Connection}s should be disconnected.
     */
    @Override
    public void onDisconnect() {
        Call call = getOriginalCall();
        if (call != null) {
            Log.d(this, "Found multiparty call to hangup for conference.");
            try {
                call.hangup();
            } catch (CallStateException e) {
                Log.e(this, e, "Exception thrown trying to hangup conference");
            }
        }
    }

    @Override
    public void onSeparate(Connection connection) {
        Log.e(this, new Exception(), "Separate not supported for CDMA conference call.");
    }

    @Override
    public void onHold() {
        /// M: cdma call fake hold handling. @{
        //Log.e(this, new Exception(), "Hold not supported for CDMA conference call.");
        Log.d(this, "donothing, just set the hold status.");
        mHandler.sendMessageDelayed(Message.obtain(mHandler, MSG_CDMA_CALL_SWITCH, FAKE_HOLD, 0),
                MSG_CDMA_CALL_SWITCH_DELAY);
        /// @}
    }

    /**
     * Invoked when the conference should be moved from hold to active.
     */
    @Override
    public void onUnhold() {
        /// M: cdma call fake hold handling. @{
        //Log.e(this, new Exception(), "Unhold not supported for CDMA conference call.");
        Log.d(this, "donothing, just set the unhold status.");
        mHandler.sendMessageDelayed(Message.obtain(mHandler, MSG_CDMA_CALL_SWITCH, FAKE_UNHOLD, 0),
                MSG_CDMA_CALL_SWITCH_DELAY);
        /// @}
    }

    @Override
    public void onMerge() {
        Log.i(this, "Merging CDMA conference call.");
        // Can only merge once
        mCapabilities &= ~Connection.CAPABILITY_MERGE_CONFERENCE;
        // Once merged, swap is enabled.
        if (isSwapSupportedAfterMerge()) {
            mCapabilities |= Connection.CAPABILITY_SWAP_CONFERENCE;
        }
        updateCapabilities(mCapabilities);
        sendFlash();

        /// M: ALPS02142143 @{
        // If the call in HOLDING status before merging, then unhold
        // it, avoid the call keep holding status and no "unhold" button
        // to recover it.
        if (getState() == Connection.STATE_HOLDING) {
            onUnhold();
        }
        /// @}
    }

    @Override
    public void onPlayDtmfTone(char c) {
        final CdmaConnection connection = getFirstConnection();
        if (connection != null) {
            connection.onPlayDtmfTone(c);
        } else {
            Log.w(this, "No CDMA connection found while trying to play dtmf tone.");
        }
    }

    @Override
    public void onStopDtmfTone() {
        final CdmaConnection connection = getFirstConnection();
        if (connection != null) {
            connection.onStopDtmfTone();
        } else {
            Log.w(this, "No CDMA connection found while trying to stop dtmf tone.");
        }
    }

    @Override
    public void onSwap() {
        Log.i(this, "Swapping CDMA conference call.");
        sendFlash();
    }

    private void sendFlash() {
        Call call = getOriginalCall();
        if (call != null) {
            try {
                // For CDMA calls, this just sends a flash command.
                call.getPhone().switchHoldingAndActive();
            } catch (CallStateException e) {
                Log.e(this, e, "Error while trying to send flash command.");
            }
        }
    }

    private Call getMultipartyCallForConnection(Connection connection) {
        com.android.internal.telephony.Connection radioConnection =
                getOriginalConnection(connection);
        if (radioConnection != null) {
            Call call = radioConnection.getCall();
            if (call != null && call.isMultiparty()) {
                return call;
            }
        }
        return null;
    }

    private Call getOriginalCall() {
        List<Connection> connections = getConnections();
        if (!connections.isEmpty()) {
            com.android.internal.telephony.Connection originalConnection =
                    getOriginalConnection(connections.get(0));
            if (originalConnection != null) {
                return originalConnection.getCall();
            }
        }
        return null;
    }

    /**
     * Return whether network support swap after merge conference call.
     *
     * @return true to support, false not support.
     */
    private final boolean isSwapSupportedAfterMerge() {
        boolean supportSwapAfterMerge = true;
        Context context = PhoneGlobals.getInstance();

        if (context != null) {
            CarrierConfigManager configManager = (CarrierConfigManager) context.getSystemService(
                    Context.CARRIER_CONFIG_SERVICE);
            PersistableBundle b = configManager.getConfig();
            if (b != null) {
                supportSwapAfterMerge =
                        b.getBoolean(CarrierConfigManager.KEY_SUPPORT_SWAP_AFTER_MERGE_BOOL);
                Log.d(this, "Current network support swap after call merged capability is "
                        + supportSwapAfterMerge);
            }
        }
        return supportSwapAfterMerge;
    }

    private com.android.internal.telephony.Connection getOriginalConnection(Connection connection) {
        if (connection instanceof CdmaConnection) {
            return ((CdmaConnection) connection).getOriginalConnection();
        } else {
            Log.e(this, null, "Non CDMA connection found in a CDMA conference");
            return null;
        }
    }

    private CdmaConnection getFirstConnection() {
        final List<Connection> connections = getConnections();
        if (connections.isEmpty()) {
            return null;
        }
        return (CdmaConnection) connections.get(0);
    }

    /// M: cdma call fake hold handling. @{
    private static final int MSG_CDMA_CALL_SWITCH = 3;
    private static final int MSG_CDMA_CALL_SWITCH_DELAY = 200;
    private static final int FAKE_HOLD = 1;
    private static final int FAKE_UNHOLD = 0;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CDMA_CALL_SWITCH:
                    handleFakeHold(msg.arg1);
                    break;
                default:
                    break;
            }
        }

    };

    void handleFakeHold(int fakeOp) {
        Log.d(this, "handleFakeHold with operation %s", fakeOp);
        if (FAKE_HOLD == fakeOp) {
            setOnHold();
        } else if (FAKE_UNHOLD == fakeOp) {
            setActive();
        }
        resetConnectionState();
        updateCapabilities(mCapabilities);
    }

    void resetConnectionState() {
        int state = getState();
        if (state != Connection.STATE_ACTIVE
                && state != Connection.STATE_HOLDING) {
            return;
        }

        List<Connection> conns = getConnections();
        for (Connection c : conns) {
            if (c.getState() != state) {
                if (state == Connection.STATE_ACTIVE) {
                    c.setActive();
                } else {
                    c.setOnHold();
                }
                if (c instanceof CdmaConnection) {
                    CdmaConnection cc = (CdmaConnection) c;
                    /// M: The access control is changed to protected
                    cc.fireOnCallState();
                }
            }
        }
    }
    /// @}

    /// M: Not allow mute in ECBM and update after exit ECBM @{
    @Override
    protected int buildConnectionCapabilities() {
        Log.i(this, "buildConnectionCapabilities");

        if (getConnections() == null || getConnections().size() == 0) {
            Log.d(this, "No connection exist, update capability to 0.");
            return 0;
        }

        String inEcm = SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE, "false");
        if (inEcm.equals("false")) {
            mCapabilities |= Connection.CAPABILITY_MUTE;
        } else {
            mCapabilities &= ~Connection.CAPABILITY_MUTE;
        }

        /// M: cdma call fake hold handling. @{
        mCapabilities |= Connection.CAPABILITY_SUPPORT_HOLD;
        if (getState() == Connection.STATE_ACTIVE || getState() == Connection.STATE_HOLDING) {
            mCapabilities |= Connection.CAPABILITY_HOLD;
        }
        /// @}

        Log.d(this, Connection.capabilitiesToString(mCapabilities));
        return mCapabilities;
    }
    /// @}

    /// M: CC026: Interface for hangup all connections @{
    /**
     * Hangup all connections in the conference.
     * CDMA hangup all is different to GSM, no CHLD=6 to hang up all calls in a phone
     */
    @Override
    public void onHangupAll() {
        Log.v(this, "onHangupAll");
        if (getFirstConnection() != null) {
            try {
                Call call = getFirstConnection()
                        .getOriginalConnection().getCall();
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
}
