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

/// M: Not allow mute in ECBM and update after exit ECBM @{
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
/// @}

import android.os.Handler;
import android.telecom.Connection;
import android.telecom.DisconnectCause;

/// M: ALPS02300293 @{
import android.telecom.PhoneAccountHandle;
/// @}

/// M: Not allow mute in ECBM and update after exit ECBM @{
import com.android.internal.telephony.TelephonyIntents;
import com.android.phone.PhoneGlobals;
/// @}
/// M: ALPS02300293 @{
import com.android.phone.PhoneUtils;
/// @}

import java.util.ArrayList;
import java.util.List;

/**
 * Manages CDMA conference calls. CDMA conference calls are much more limited than GSM conference
 * calls. Two main points of difference:
 * 1) Users cannot manage individual calls within a conference
 * 2) Whether a conference call starts off as a conference or as two distinct calls is a matter of
 *    physical location (some antennas are different than others). Worst still, there's no
 *    indication given to us as to what state they are in.
 *
 * To make life easier on the user we do the following: Whenever there exist 2 or more calls, we
 * say that we are in a conference call with {@link Connection#CAPABILITY_GENERIC_CONFERENCE}.
 * Generic indicates that this is a simple conference that doesn't support conference management.
 * The conference call will also support "MERGE" to begin with and stop supporting it the first time
 * we are asked to actually execute a merge. I emphasize when "we are asked" because we get no
 * indication whether the merge succeeds from CDMA, we just assume it does. Thats the best we
 * can do. Also, we do not kill a conference call once it is created unless all underlying
 * connections also go away.
 *
 * Outgoing CDMA calls made while another call exists would normally trigger a conference to be
 * created. To avoid this and make it seem like there is a "dialing" state, we fake it and prevent
 * the conference from being created for 3 seconds. This is a more pleasant experience for the user.
 */
final class CdmaConferenceController {
    private final Connection.Listener mConnectionListener = new Connection.Listener() {
                @Override
                public void onStateChanged(Connection c, int state) {
                    /// M: For CDMA conference@{
                    Log.d(CdmaConferenceController.this, "onStateChanged, conn:" + c
                            + ", state:" + state);
                    if (state != android.telecom.Connection.STATE_DISCONNECTED) {
                        recalculateConference();
                    }
                    //recalculateConference();
                    /// @}
                }

                @Override
                public void onDisconnected(Connection c, DisconnectCause disconnectCause) {
                    /// M: For CDMA conference@{
                    Log.d(CdmaConferenceController.this, "onDisconnected, conn:" + c
                            + ", disconnectCause:" + disconnectCause);
                    //recalculateConference();
                    /// @}
                }

                @Override
                public void onDestroyed(Connection c) {
                    /// M: For CDMA conference@{
                    Log.d(CdmaConferenceController.this, "onDestroyed, conn:" + c);
                    /// @}
                    remove((CdmaConnection) c);
                }
            };

    private static final int ADD_OUTGOING_CONNECTION_DELAY_MILLIS = 6000;

    /// M: @{
    private static final int UPDATE_CALL_CAPABILITIE_DELAY_MILLIS = 200;
    private static final int ADD_WAITING_CONNECTION_DELAY_MILLIS = 1000;
    /// @}

    /** The known CDMA connections. */
    private final List<CdmaConnection> mCdmaConnections = new ArrayList<>();

    /**
     * Newly added connections.  We keep track of newly added outgoing connections because we do not
     * create a conference until a second outgoing call has existing for
     * {@link #ADD_OUTGOING_CONNECTION_DELAY_MILLIS} milliseconds.  This allows the UI to show the
     * call as "Dialing" for a certain amount of seconds.
     */
    private final List<CdmaConnection> mPendingOutgoingConnections = new ArrayList<>();

    private final TelephonyConnectionService mConnectionService;

    private final Handler mHandler = new Handler();

    private MyBroadcastReceiver mReceiver;
    private int mConfConnCount = 0;

    public CdmaConferenceController(TelephonyConnectionService connectionService) {
        mConnectionService = connectionService;
        /// M: Not allow mute in ECBM and update after exit ECBM @{
        mReceiver = new MyBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(
                TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED);
        PhoneGlobals.getInstance().registerReceiver(mReceiver, intentFilter);
        /// @}
    }

    /** The CDMA conference connection object. */
    private CdmaConference mConference;

    /// M: ALPS02326007 @{
    // We need to set the second MO call to active immediately when receive waiting call,
    // so add new member to record the connection and runnable.
    private CdmaConnection mSecondCall = null;
    private List<CdmaConnection> mConnectionsToReset = new ArrayList<CdmaConnection>();
    private Runnable mDelayRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(this, "mDelayRunnable, mSecondCall:" + mSecondCall);
            if (mSecondCall != null) {
                mSecondCall.forceAsDialing(false);
                addInternal(mSecondCall);
                mSecondCall = null;
            }
            Log.d(this, "mDelayRunnable, mConnectionsToReset:" + mConnectionsToReset);
            for (CdmaConnection current : mConnectionsToReset) {
                Log.d(this, "mDelayRunnable, reset state for:" + current);
                current.resetStateForConference();
            }
            mConnectionsToReset.clear();
        }
    };
    /// @}

    void add(final CdmaConnection connection) {
        /// M: For CDMA conference@{
        Log.d(this, "add: current connection list = " + mCdmaConnections);
        Log.d(this, "add: connection = %s", connection);
        /// @}
        if (!mCdmaConnections.isEmpty() && connection.isOutgoing()) {
            // There already exists a connection, so this will probably result in a conference once
            // it is added. For outgoing connections which are added while another connection
            // exists, we mark them as "dialing" for a set amount of time to give the user time to
            // see their new call as "Dialing" before it turns into a conference call.
            // During that time, we also mark the other calls as "held" or else it can cause issues
            // due to having an ACTIVE and a DIALING call simultaneously.
            connection.forceAsDialing(true);

            mSecondCall = connection;
            mConnectionsToReset.clear();
            for (CdmaConnection current : mCdmaConnections) {
                Log.d(this, "current's state:" + current.getState());
                if (current.setHoldingForConference()) {
                    mConnectionsToReset.add(current);
                } else {
                    Log.d(this, "Fail to setHoldingForConference");
                }
            }
            Log.d(this, "Add second connection, mConnectionsToReset:" + mConnectionsToReset);
            mHandler.postDelayed(mDelayRunnable, ADD_OUTGOING_CONNECTION_DELAY_MILLIS);

            /*final List<CdmaConnection> connectionsToReset =
                    new ArrayList<>(mCdmaConnections.size());
            for (CdmaConnection current : mCdmaConnections) {
                if (current.setHoldingForConference()) {
                    connectionsToReset.add(current);
                }
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    connection.forceAsDialing(false);
                    addInternal(connection);
                    for (CdmaConnection current : connectionsToReset) {
                        current.resetStateForConference();
                    }
                }
            }, ADD_OUTGOING_CONNECTION_DELAY_MILLIS);*/

        /// M: For bluetooth special reqeust @{
        // After added the fake HOLDING status, before answer the waiting call,
        // we make sure the HOLDING call can't change to ACTIVE:
        // a) Add listenter to the waiting connection;
        // b) Once the waiting call changes to active, this means the call be answered;
        // c) Reset the holding call to active;
        // d) Add and notify the conference call to telecom.
        } else if (!mCdmaConnections.isEmpty() && connection.isCallWaiting()) {
            Log.d(this, "Waiting call arrives, mSecondCall:" + mSecondCall
                    + ", hasCallbacks:" + mHandler.hasCallbacks(mDelayRunnable));
            if (mSecondCall != null && mHandler.hasCallbacks(mDelayRunnable)) {
                Log.d(this, "Merge the second call now");
                mHandler.removeCallbacks(mDelayRunnable);
                mSecondCall.forceAsDialing(false);
                addInternal(mSecondCall);
                mSecondCall = null;
                for (CdmaConnection current : mConnectionsToReset) {
                    current.resetStateForConference();
                }
                mConnectionsToReset.clear();
            }

            Log.d(this, "add waiting call connection listenner.");
            connection.addConnectionListener(new Connection.Listener() {
                public void onStateChanged(final Connection c, int state) {
                    Log.d("CdmaConferenceController", "Waiting call onStateChanged %s, state[%s]"
                            + ", mCdmaConnections.size:%d", c, state, mCdmaConnections.size());
                    if (state == Connection.STATE_ACTIVE) {
                        c.removeConnectionListener(this);
                        mHandler.postDelayed(new Runnable() {
                            public void run() {
                                addInternal((CdmaConnection) c);
                            }
                        }, ADD_WAITING_CONNECTION_DELAY_MILLIS);
                    } else if (state == Connection.STATE_DISCONNECTED) {
                        c.removeConnectionListener(this);
                    }
                }
            });
        /// @}
        } else {
            // This is the first connection, or it is incoming, so let it flow through.
            addInternal(connection);
        }
    }

    private void addInternal(CdmaConnection connection) {
        mCdmaConnections.add(connection);
        connection.addConnectionListener(mConnectionListener);
        recalculateConference();
    }

    private void remove(CdmaConnection connection) {
        connection.removeConnectionListener(mConnectionListener);
        mCdmaConnections.remove(connection);
        recalculateConference();
    }

    private void recalculateConference() {
        List<CdmaConnection> conferenceConnections = new ArrayList<>(mCdmaConnections.size());
        for (CdmaConnection connection : mCdmaConnections) {
            // We do not include call-waiting calls in conferences.
            if (!connection.isCallWaiting() &&
                    connection.getState() != Connection.STATE_DISCONNECTED) {
                conferenceConnections.add(connection);
            }
        }

        Log.d(this, "recalculating conference calls %d", conferenceConnections.size());
        Log.i(this, "mConfConnCount:" + mConfConnCount);
        if (conferenceConnections.size() >= 2) {
            boolean isNewlyCreated = false;

            // There are two or more CDMA connections. Do the following:
            // 1) Create a new conference connection if it doesn't exist.
            if (mConference == null) {
                Log.i(this, "Creating new Cdma conference call");

                /// M: ALPS02300293 @{
                // Record audio function needs PhoneAccountHandle to set the capability
                PhoneAccountHandle handle = null;
                for (CdmaConnection connection : conferenceConnections) {
                    com.android.internal.telephony.Connection radioConnection =
                            connection.getOriginalConnection();
                    if (radioConnection != null && radioConnection.getCall() != null) {
                        handle = PhoneUtils.makePstnPhoneAccountHandle(
                                radioConnection.getCall().getPhone());
                        break;
                    }
                }
                Log.d(this, "Handle for CdmaConference:" + handle);
                mConference = new CdmaConference(handle);
                /// @}

                /// M: For CDMA conference@{
                Log.d(this, "First conn:" + mCdmaConnections.get(0));
                if (mCdmaConnections.get(0).getOriginalConnection() != null) {
                    mConference.setConnectTimeMillis(
                            mCdmaConnections.get(0).getOriginalConnection().getConnectTime());
                } else {
                    Log.d(this, "Orig conn is null!");
                }
                /// @}
                isNewlyCreated = true;
            }

            CdmaConnection newConnection = mCdmaConnections.get(mCdmaConnections.size() - 1);
            if (newConnection.isOutgoing()) {
                // Only an outgoing call can be merged with an ongoing call.
                /// M: ALPS02122225 @{
                // Only add CAPABILITY_MERGE_CONFERENCE for new created connection
                // and the new connection is outgoing call.
                //if (Connection.can(mConference.getConnectionCapabilities(),
                //        Connection.CAPABILITY_SWAP_CONFERENCE)) {
                if (mConfConnCount == conferenceConnections.size()) {
                    Log.i(this, "The conference call has been merged, so do nothing.");
                } else {
                /// @}
                    Log.i(this, "Update merge capability");
                    mConference.updateCapabilities(Connection.CAPABILITY_MERGE_CONFERENCE);
                    mConference.removeCapabilities(Connection.CAPABILITY_SWAP_CONFERENCE);
                }
            } else {
                // If the most recently added connection was an incoming call, enable
                // swap instead of merge.
                mConference.updateCapabilities(Connection.CAPABILITY_SWAP_CONFERENCE);
                /// M: For CDMA conference@{
                mConference.removeCapabilities(Connection.CAPABILITY_MERGE_CONFERENCE);
                Log.i(this, "Update swap capability");
                /// @}
            }

            // 2) Add any new connections to the conference
            /// M: For CDMA conference@{
            boolean addNewConnection = false;
            /// @}
            List<Connection> existingChildConnections =
                    new ArrayList<>(mConference.getConnections());
            for (CdmaConnection connection : conferenceConnections) {
                if (!existingChildConnections.contains(connection)) {
                    Log.i(this, "Adding connection to conference call: %s", connection);
                    mConference.addConnection(connection);
                    /// M: For CDMA conference@{
                    addNewConnection = true;
                    /// @}
                }
                existingChildConnections.remove(connection);
            }

            // 3) Remove any lingering old/disconnected/destroyed connections
            for (Connection oldConnection : existingChildConnections) {
                mConference.removeConnection(oldConnection);
                Log.i(this, "Removing connection from conference call: %s", oldConnection);
            }

            // 4) Add the conference to the connection service if it is new.
            if (isNewlyCreated) {
                Log.d(this, "Adding the conference call");
                /// M: For CDMA conference@{
                mConference.resetConnectionState();
                /// @}
                mConnectionService.addConference(mConference);
            /// M: For CDMA conference@{
            } else if (addNewConnection) {
                mConference.setActive();
                mConference.resetConnectionState();
            }
            if (mConference.getConnectionCapabilities() !=
                     mConference.buildConnectionCapabilities()) {
                /// M: Give some time for telecom knows the conference call.
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mConference != null) {
                            mConference.updateConnectionCapabilities();
                        }
                    }
                }, UPDATE_CALL_CAPABILITIE_DELAY_MILLIS);
            /// @}
            }
        } else if (conferenceConnections.isEmpty()) {
            // There are no more connection so if we still have a conference, lets remove it.
            if (mConference != null) {
                Log.i(this, "Destroying the CDMA conference connection.");
                mConference.destroy();
                mConference = null;

                mSecondCall = null;
                mConnectionsToReset.clear();
            }
        }
        mConfConnCount = conferenceConnections.size();
    }

    /// M: Not allow mute in ECBM and update after exit ECBM @{
    /**
     * Receive the ecm change intent.
     */
    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED.equals(action)) {
                boolean isEcm = intent.getBooleanExtra("phoneinECMState", false);
                Log.d("CdmaConferenceController", "Received ECM changed, isEcm:" + isEcm);
                if (mConference != null) {
                    mConference.updateConnectionCapabilities();
                } else {
                    for (CdmaConnection current : mCdmaConnections) {
                        current.updateConnectionCapabilities();
                    }
                }
            }
        }
    }

    public void onDestroy() {
        PhoneGlobals.getInstance().unregisterReceiver(mReceiver);
    }
    /// @}
}
