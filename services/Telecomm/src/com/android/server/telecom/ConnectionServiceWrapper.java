/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright 2014, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.telecom;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.telecom.CallAudioState;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.DisconnectCause;
import android.telecom.GatewayInfo;
import android.telecom.ParcelableConference;
import android.telecom.ParcelableConnection;
import android.telecom.PhoneAccountHandle;
import android.telecom.StatusHints;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import com.android.internal.telecom.IConnectionService;
import com.android.internal.telecom.IConnectionServiceAdapter;
import com.android.internal.telecom.IVideoProvider;
import com.android.internal.telecom.RemoteServiceCallback;
import com.android.internal.util.Preconditions;
import com.mediatek.telecom.LogUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wrapper for {@link IConnectionService}s, handles binding to {@link IConnectionService} and keeps
 * track of when the object can safely be unbound. Other classes should not use
 * {@link IConnectionService} directly and instead should use this class to invoke methods of
 * {@link IConnectionService}.
 */
final class ConnectionServiceWrapper extends ServiceBinder {

    private final class Adapter extends IConnectionServiceAdapter.Stub {

        @Override
        public void handleCreateConnectionComplete(
                String callId,
                ConnectionRequest request,
                ParcelableConnection connection) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    /// M: for log parser @{
                    String number = "";
                    String action = "";
                    Uri uri = connection.getHandle();
                    if (uri != null) {
                        number = uri.getSchemeSpecificPart();
                    }
                    Call call = mCallIdMapper.getCall(callId);
                    if (call != null) {
                        if (call.isIncoming()) {
                            action = LogUtils.NOTIFY_ACTION_CREATE_MT_SUCCESS;
                        } else if (!call.isUnknown()) {
                            action = LogUtils.NOTIFY_ACTION_CREATE_MO_SUCCESS;
                        }
                    }
                    LogUtils.logCcNotify(number, action, callId, connection.toString());
                    /// @}
                    logIncoming("handleCreateConnectionComplete %s", callId);
                    if (mCallIdMapper.isValidCallId(callId)) {
                        ConnectionServiceWrapper.this
                                .handleCreateConnectionComplete(callId, request, connection);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void setActive(String callId) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("setActive %s", callId);
                    if (mCallIdMapper.isValidCallId(callId) || mCallIdMapper
                            .isValidConferenceId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        /// M: for log parser @{
                        LogUtils.logCcNotify(call, LogUtils.NOTIFY_ACTION_ACTIVE, callId, "");
                        /// @}
                        if (call != null) {
                            mCallsManager.markCallAsActive(call);
                        } else {
                            // Log.w(this, "setActive, unknown call id: %s", msg.obj);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void setRinging(String callId) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("setRinging %s", callId);
                    if (mCallIdMapper.isValidCallId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            mCallsManager.markCallAsRinging(call);
                        } else {
                            // Log.w(this, "setRinging, unknown call id: %s", msg.obj);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void setVideoProvider(String callId, IVideoProvider videoProvider) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("setVideoProvider %s", callId);
                    if (mCallIdMapper.isValidCallId(callId)
                            || mCallIdMapper.isValidConferenceId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            call.setVideoProvider(videoProvider);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void setDialing(String callId) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("setDialing %s", callId);
                    if (mCallIdMapper.isValidCallId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            mCallsManager.markCallAsDialing(call);
                        } else {
                            // Log.w(this, "setDialing, unknown call id: %s", msg.obj);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void setDisconnected(String callId, DisconnectCause disconnectCause) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("setDisconnected %s %s", callId, disconnectCause);
                    if (mCallIdMapper.isValidCallId(callId) || mCallIdMapper
                            .isValidConferenceId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        Log.d(this, "disconnect call %s %s", disconnectCause, call);
                        /// M:Ecc retry would not set disconnected, need to remove the response
                        // object for disconnected call to avoid wrong retry.
                        mPendingResponses.remove(callId);
                        /// M: for log parser @{
                        LogUtils.logCcNotify(call, LogUtils.NOTIFY_ACTION_DISCONNECT, callId,
                                disconnectCause.toString());
                        /// @}
                        if (call != null) {
                            mCallsManager.markCallAsDisconnected(call, disconnectCause);
                        } else {
                            // Log.w(this, "setDisconnected, unknown call id: %s", args.arg1);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void setOnHold(String callId) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("setOnHold %s", callId);
                    if (mCallIdMapper.isValidCallId(callId) || mCallIdMapper
                            .isValidConferenceId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        /// M: for log parser @{
                        LogUtils.logCcNotify(call, LogUtils.NOTIFY_ACTION_ONHOLD, callId, "");
                        /// @}
                        if (call != null) {
                            mCallsManager.markCallAsOnHold(call);
                        } else {
                            // Log.w(this, "setOnHold, unknown call id: %s", msg.obj);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void setRingbackRequested(String callId, boolean ringback) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("setRingbackRequested %s %b", callId, ringback);
                    if (mCallIdMapper.isValidCallId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            call.setRingbackRequested(ringback);
                        } else {
                            // Log.w(this, "setRingback, unknown call id: %s", args.arg1);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void removeCall(String callId) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("removeCall %s", callId);
                    if (mCallIdMapper.isValidCallId(callId) || mCallIdMapper
                            .isValidConferenceId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            if (call.isAlive()) {
                                mCallsManager.markCallAsDisconnected(
                                        call, new DisconnectCause(DisconnectCause.REMOTE));
                            } else {
                                mCallsManager.markCallAsRemoved(call);
                            }
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void setConnectionCapabilities(String callId, int connectionCapabilities) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("setConnectionCapabilities %s %d", callId, connectionCapabilities);
                    if (mCallIdMapper.isValidCallId(callId) || mCallIdMapper
                            .isValidConferenceId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            call.setConnectionCapabilities(connectionCapabilities);
                            /// M: For block certain ViLTE @{
                            call.setCapabilitiesRecord(connectionCapabilities);
                            /// @}
                        } else {
                            // Log.w(ConnectionServiceWrapper.this,
                            // "setConnectionCapabilities, unknown call id: %s", msg.obj);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void setIsConferenced(String callId, String conferenceCallId) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("setIsConferenced %s %s", callId, conferenceCallId);
                    Call childCall = mCallIdMapper.getCall(callId);
                    if (childCall != null) {
                        if (conferenceCallId == null) {
                            Log.d(this, "unsetting parent: %s", conferenceCallId);
                            childCall.setParentCall(null);
                        } else {
                            Call conferenceCall = mCallIdMapper.getCall(conferenceCallId);
                            childCall.setParentCall(conferenceCall);
                        }
                    } else {
                        // Log.w(this, "setIsConferenced, unknown call id: %s", args.arg1);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void setConferenceMergeFailed(String callId) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("setConferenceMergeFailed %s", callId);
                    if (mCallIdMapper.isValidCallId(callId)) {
                        // TODO: we should move the UI for indication a merge failure here
                        // from CallNotifier.onSuppServiceFailed(). This way the InCallUI can
                        // deliver the message anyway that they want. b/20530631.
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            // Just refresh the connection capabilities so that the UI
                            // is forced to reenable the merge button as the capability
                            // is still on the connection. Note when b/20530631 is fixed, we need
                            // to revisit this fix to remove this hacky way of unhiding the merge
                            // button (side effect of reprocessing the capabilities) and plumb
                            // the failure event all the way to InCallUI instead of stopping
                            // it here. That way we can also handle the UI of notifying that
                            // the merged has failed.
                            call.setConnectionCapabilities(call.getConnectionCapabilities(), true);
                        } else {
                            Log.w(this, "setConferenceMergeFailed, unknown call id: %s", callId);
                        }
                    }

                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void addConferenceCall(String callId, ParcelableConference parcelableConference) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    /// M: for log parser @{
                    LogUtils.logCcNotify(LogUtils.NUMBER_CONF_CALL,
                            LogUtils.NOTIFY_ACTION_CONF_CREATED,
                            callId,
                            parcelableConference.toString());
                    /// @}

                    logIncoming("addConferenceCall  %s %s", callId, parcelableConference);
                    if (mCallIdMapper.getCall(callId) != null) {
                        Log.w(this, "Attempting to add a conference call using an existing " +
                                "call id %s", callId);
                        return;
                    }

                    // Make sure that there's at least one valid call. For remote connections
                    // we'll get a add conference msg from both the remote connection service
                    // and from the real connection service.
                    boolean hasValidCalls = false;
                    for (String connId : parcelableConference.getConnectionIds()) {
                        if (mCallIdMapper.getCall(connId) != null) {
                            hasValidCalls = true;
                        }
                    }
                    // But don't bail out if the connection count is 0, because that is a valid
                    // IMS conference state.
                    if (!hasValidCalls && parcelableConference.getConnectionIds().size() > 0) {
                        Log.d(this, "Attempting to add a conference with no valid calls");
                        return;
                    }

                    // need to create a new Call
                    PhoneAccountHandle phAcc = null;
                    if (parcelableConference != null &&
                            parcelableConference.getPhoneAccount() != null) {
                        phAcc = parcelableConference.getPhoneAccount();
                    }
                    Call conferenceCall = mCallsManager.createConferenceCall(
                            phAcc, parcelableConference);
                    mCallIdMapper.addCall(conferenceCall, callId);
                    conferenceCall.setConnectionService(ConnectionServiceWrapper.this);

                    Log.d(this, "adding children to conference %s phAcc %s",
                            parcelableConference.getConnectionIds(), phAcc);
                    for (String connId : parcelableConference.getConnectionIds()) {
                        Call childCall = mCallIdMapper.getCall(connId);
                        Log.d(this, "found child: %s", connId);
                        if (childCall != null) {
                            childCall.setParentCall(conferenceCall);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void onPostDialWait(String callId, String remaining) throws RemoteException {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("onPostDialWait %s %s", callId, remaining);
                    if (mCallIdMapper.isValidCallId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            call.onPostDialWait(remaining);
                        } else {
                            // Log.w(this, "onPostDialWait, unknown call id: %s", args.arg1);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void onPostDialChar(String callId, char nextChar) throws RemoteException {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("onPostDialChar %s %s", callId, nextChar);
                    if (mCallIdMapper.isValidCallId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            call.onPostDialChar(nextChar);
                        } else {
                            // Log.w(this, "onPostDialChar, unknown call id: %s", args.arg1);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void queryRemoteConnectionServices(RemoteServiceCallback callback) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("queryRemoteConnectionServices %s", callback);
                    ConnectionServiceWrapper.this.queryRemoteConnectionServices(callback);
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void setVideoState(String callId, int videoState) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("setVideoState %s %d", callId, videoState);
                    if (mCallIdMapper.isValidCallId(callId)
                            || mCallIdMapper.isValidConferenceId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            call.setVideoState(videoState);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void setIsVoipAudioMode(String callId, boolean isVoip) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("setIsVoipAudioMode %s %b", callId, isVoip);
                    if (mCallIdMapper.isValidCallId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            call.setIsVoipAudioMode(isVoip);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void setStatusHints(String callId, StatusHints statusHints) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("setStatusHints %s %s", callId, statusHints);
                    if (mCallIdMapper.isValidCallId(callId)
                            || mCallIdMapper.isValidConferenceId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            call.setStatusHints(statusHints);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void setExtras(String callId, Bundle extras) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized(mLock) {
                    logIncoming("setExtras %s %s", callId, extras);
                    if (mCallIdMapper.isValidCallId(callId)
                            || mCallIdMapper.isValidConferenceId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            call.setExtras(extras);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void setAddress(String callId, Uri address, int presentation) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("setAddress %s %s %d", callId, address, presentation);
                    /// M: For VoLTE @{
                    // the call created from addExistingConnection() do not have prefix,
                    // which can not pass isValidCallId() check, so need modify.
                    // Original Code:
                    // if (mCallIdMapper.isValidCallId(callId)) {
                    if (mCallIdMapper.isValidCallId(callId)
                            || mCallIdMapper.isValidConferenceId(callId)) {
                    /// @}
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            call.setHandle(address, presentation);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void setCallerDisplayName(
                String callId, String callerDisplayName, int presentation) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("setCallerDisplayName %s %s %d", callId, callerDisplayName,
                            presentation);
                    if (mCallIdMapper.isValidCallId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            call.setCallerDisplayName(callerDisplayName, presentation);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void setConferenceableConnections(
                String callId, List<String> conferenceableCallIds) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("setConferenceableConnections %s %s", callId,
                            conferenceableCallIds);
                    if (mCallIdMapper.isValidCallId(callId) ||
                            mCallIdMapper.isValidConferenceId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            List<Call> conferenceableCalls =
                                    new ArrayList<>(conferenceableCallIds.size());
                            for (String otherId : conferenceableCallIds) {
                                Call otherCall = mCallIdMapper.getCall(otherId);
                                if (otherCall != null && otherCall != call) {
                                    conferenceableCalls.add(otherCall);
                                }
                            }
                            call.setConferenceableCalls(conferenceableCalls);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void addExistingConnection(String callId, ParcelableConnection connection) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    /// M: for log parser @{
                    String number = "";
                    Uri uri = connection.getHandle();
                    if (uri != null) {
                        number = uri.getSchemeSpecificPart();
                    }
                    LogUtils.logCcNotify(number, LogUtils.NOTIFY_ACTION_NEW_CALL_ADDED,
                            callId, null);
                    /// @}
                    logIncoming("addExistingConnection  %s %s", callId, connection);
                    Call existingCall = mCallsManager
                            .createCallForExistingConnection(callId, connection);
                    mCallIdMapper.addCall(existingCall, callId);
                    existingCall.setConnectionService(ConnectionServiceWrapper.this);
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        /* M: CC part start */
        @Override
        public void notifyConnectionLost(String callId) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("notifyConnectionLost %s", callId);
                    if (mCallIdMapper.isValidCallId(callId)
                            || mCallIdMapper.isValidConferenceId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            mCallsManager.notifyConnectionLost(call);
                        } else {
                            Log.w(this, "notifyConnectionLost, unknown call id: %s", callId);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void notifyActionFailed(String callId, int action) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("notifyActionFailed %s | %d", callId, action);
                    if (mCallIdMapper.isValidCallId(callId)
                            || mCallIdMapper.isValidConferenceId(callId)) {

                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            mCallsManager.notifyActionFailed(call, action);
                        } else {
                            Log.w(this, "notifyActionFailed, unknown call id: %s", callId);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void notifySSNotificationToast(String callId, int notiType, int type, int code, String number, int index) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("notifySSNotificationToast %s | %d |%d | %d | %s | %d", callId,
                            notiType, type, code, number, index);
                    if (mCallIdMapper.isValidCallId(callId)
                            || mCallIdMapper.isValidConferenceId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            mCallsManager.notifySSNotificationToast(
                                    call, notiType, type, code, number, index);
                        } else {
                            Log.w(this, "notifySSNotificationToast, unknown call id: %s", callId);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void notifyNumberUpdate(String callId, String number) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("notifySSNotificationToast %s | %s ", callId, number);
                    if (mCallIdMapper.isValidCallId(callId)
                            || mCallIdMapper.isValidConferenceId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            mCallsManager.notifyNumberUpdate(call, number);
                        } else {
                            Log.w(this, "notifyNumberUpdate, unknown call id: %s", callId);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void notifyIncomingInfoUpdate(String callId, int type, String alphaid, int cli_validity) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("notifySSNotificationToast %s | %d | %s | %d", callId, type,
                            alphaid, cli_validity);
                    if (mCallIdMapper.isValidCallId(callId)
                            || mCallIdMapper.isValidConferenceId(callId)) {
                    Call call = mCallIdMapper.getCall(callId);
                    if (call != null) {
                        mCallsManager.notifyIncomingInfoUpdate(call, type, alphaid, cli_validity);
                    } else {
                        Log.w(this, "notifyIncomingInfoUpdate, unknown call id: %s", callId);
                    }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void notifyCdmaCallAccepted(String callId) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("notifyCdmaCallAccepted %s", callId);
                    if (mCallIdMapper.isValidCallId(callId)
                            || mCallIdMapper.isValidConferenceId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            mCallsManager.notifyCdmaCallAccepted(call);
                        } else {
                            Log.w(this, "notifyCdmaCallAccepted, unknown call id: %s", callId);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void notifyAccountChanged(String callId, PhoneAccountHandle handle) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("notifyAccountChanged, callId:%s, handle:%s", callId, handle);
                    if (mCallIdMapper.isValidCallId(callId)
                            || mCallIdMapper.isValidConferenceId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            call.setTargetPhoneAccount(handle);
                        } else {
                            Log.w(this, "notifyAccountChanged, unknown call id: %s", callId);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        /* M: CC part end */

        /// M: For Volte @{
        @Override
        public void updateExtras(String callId, Bundle bundle) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("updateExtras %s %s", callId, bundle);
                    if (mCallIdMapper.isValidCallId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            call.updateExtras(bundle);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void handleCreateConferenceComplete(
                String conferenceId,
                ConnectionRequest request,
                ParcelableConference conference) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("handleCreateConferenceComplete %s", request);
                    if (mCallIdMapper.isValidCallId(conferenceId)) {
                        ConnectionServiceWrapper.this
                            .handleCreateConferenceComplete(conferenceId, request, conference);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        /// @}

        /// M: Add for 3G VT only @{
        @Override
        public void notifyVtStatusInfo(String callId, int status) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (mLock) {
                    logIncoming("notifyVtStatusInfo %s %d", callId, status);
                    if (mCallIdMapper.isValidCallId(callId)
                            || mCallIdMapper.isValidConferenceId(callId)) {
                        Call call = mCallIdMapper.getCall(callId);
                        if (call != null) {
                            mCallsManager.notifyVtStatusInfo(call, status);
                        } else {
                            Log.w(this, "notifyVtStatusInfo, unknown call id: %s", callId);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        /// @}
    }

    private final Adapter mAdapter = new Adapter();
    private final CallIdMapper mCallIdMapper = new CallIdMapper("ConnectionService");
    private final Map<String, CreateConnectionResponse> mPendingResponses = new HashMap<>();

    private Binder2 mBinder = new Binder2();
    private IConnectionService mServiceInterface;
    private final ConnectionServiceRepository mConnectionServiceRepository;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;
    private final CallsManager mCallsManager;

    /**
     * Creates a connection service.
     *
     * @param componentName The component name of the service with which to bind.
     * @param connectionServiceRepository Connection service repository.
     * @param phoneAccountRegistrar Phone account registrar
     * @param callsManager Calls manager
     * @param context The context.
     * @param userHandle The {@link UserHandle} to use when binding.
     */
    ConnectionServiceWrapper(
            ComponentName componentName,
            ConnectionServiceRepository connectionServiceRepository,
            PhoneAccountRegistrar phoneAccountRegistrar,
            CallsManager callsManager,
            Context context,
            TelecomSystem.SyncRoot lock,
            UserHandle userHandle) {
        super(ConnectionService.SERVICE_INTERFACE, componentName, context, lock, userHandle);
        mConnectionServiceRepository = connectionServiceRepository;
        phoneAccountRegistrar.addListener(new PhoneAccountRegistrar.Listener() {
            // TODO -- Upon changes to PhoneAccountRegistrar, need to re-wire connections
            // To do this, we must proxy remote ConnectionService objects
        });
        mPhoneAccountRegistrar = phoneAccountRegistrar;
        mCallsManager = callsManager;
    }

    /** See {@link IConnectionService#addConnectionServiceAdapter}. */
    private void addConnectionServiceAdapter(IConnectionServiceAdapter adapter) {
        if (isServiceValid("addConnectionServiceAdapter")) {
            try {
                logOutgoing("addConnectionServiceAdapter %s", adapter);
                mServiceInterface.addConnectionServiceAdapter(adapter);
            } catch (RemoteException e) {
            }
        }
    }

    /**
     * Creates a new connection for a new outgoing call or to attach to an existing incoming call.
     */
    void createConnection(final Call call, final CreateConnectionResponse response) {
        logOutgoing("createConnection(%s) via %s.", call, getComponentName());
        BindCallback callback = new BindCallback() {
            @Override
            public void onSuccess() {
                String callId = mCallIdMapper.getCallId(call);
                /// M: In some complex scenario, before binding success, the call has been
                // disconnected. So here pass a null callId to telephony will cause JE.
                if (callId == null) {
                    Log.w(this, "createConnection stop, callId is null");
                    return;
                }
                mPendingResponses.put(callId, response);

                GatewayInfo gatewayInfo = call.getGatewayInfo();
                Bundle extras = call.getIntentExtras();
                if (gatewayInfo != null && gatewayInfo.getGatewayProviderPackageName() != null &&
                        gatewayInfo.getOriginalAddress() != null) {
                    extras = (Bundle) extras.clone();
                    extras.putString(
                            TelecomManager.GATEWAY_PROVIDER_PACKAGE,
                            gatewayInfo.getGatewayProviderPackageName());
                    extras.putParcelable(
                            TelecomManager.GATEWAY_ORIGINAL_ADDRESS,
                            gatewayInfo.getOriginalAddress());
                }

                Log.event(call, Log.Events.START_CONNECTION, Log.piiHandle(call.getHandle()));
                try {
                    /// M: For VoLTE @{
                    boolean isConferenceDial = call.isConferenceDial();
                    if (isConferenceDial) {
                        logOutgoing("createConference(%s) via %s.", call, getComponentName());
                        mServiceInterface.createConference(
                                call.getConnectionManagerPhoneAccount(),
                                callId,
                                new ConnectionRequest(
                                        call.getTargetPhoneAccount(),
                                        call.getHandle(),
                                        extras,
                                        call.getVideoState()),
                                call.getConferenceParticipants(),
                                call.isIncoming());
                    } else {
                        mServiceInterface.createConnection(
                                call.getConnectionManagerPhoneAccount(),
                                callId,
                                new ConnectionRequest(
                                        call.getTargetPhoneAccount(),
                                        call.getHandle(),
                                        extras,
                                        call.getVideoState()),
                                call.isIncoming(),
                                call.isUnknown());
                    }
                    /// @}
                } catch (RemoteException e) {
                    Log.e(this, e, "Failure to createConnection -- %s", getComponentName());
                    mPendingResponses.remove(callId).handleCreateConnectionFailure(
                            new DisconnectCause(DisconnectCause.ERROR, e.toString()));
                }
            }

            @Override
            public void onFailure() {
                Log.e(this, new Exception(), "Failure to call %s", getComponentName());
                response.handleCreateConnectionFailure(new DisconnectCause(DisconnectCause.ERROR));
            }
        };

        mBinder.bind(callback, call);
    }

    /** @see IConnectionService#abort(String) */
    void abort(Call call) {
        // Clear out any pending outgoing call data
        final String callId = mCallIdMapper.getCallId(call);

        // If still bound, tell the connection service to abort.
        if (callId != null && isServiceValid("abort")) {
            try {
                logOutgoing("abort %s", callId);
                mServiceInterface.abort(callId);
            } catch (RemoteException e) {
            }
        }

        removeCall(call, new DisconnectCause(DisconnectCause.LOCAL));
    }

    /** @see IConnectionService#hold(String) */
    void hold(Call call) {
        final String callId = mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("hold")) {
            try {
                logOutgoing("hold %s", callId);
                mServiceInterface.hold(callId);
            } catch (RemoteException e) {
            }
        }
    }

    /** @see IConnectionService#unhold(String) */
    void unhold(Call call) {
        final String callId = mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("unhold")) {
            try {
                logOutgoing("unhold %s", callId);
                mServiceInterface.unhold(callId);
            } catch (RemoteException e) {
            }
        }
    }

    /** @see IConnectionService#onCallAudioStateChanged(String,CallAudioState) */
    void onCallAudioStateChanged(Call activeCall, CallAudioState audioState) {
        final String callId = mCallIdMapper.getCallId(activeCall);
        if (callId != null && isServiceValid("onCallAudioStateChanged")) {
            try {
                logOutgoing("onCallAudioStateChanged %s %s", callId, audioState);
                mServiceInterface.onCallAudioStateChanged(callId, audioState);
            } catch (RemoteException e) {
            }
        }
    }

    /** @see IConnectionService#disconnect(String) */
    void disconnect(Call call) {
        final String callId = mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("disconnect")) {
            try {
                logOutgoing("disconnect %s", callId);
                mServiceInterface.disconnect(callId);
            } catch (RemoteException e) {
            }
        }
    }

    /** @see IConnectionService#answer(String) */
    void answer(Call call, int videoState) {
        final String callId = mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("answer")) {
            try {
                logOutgoing("answer %s %d", callId, videoState);
                if (VideoProfile.isAudioOnly(videoState)) {
                    mServiceInterface.answer(callId);
                } else {
                    mServiceInterface.answerVideo(callId, videoState);
                }
            } catch (RemoteException e) {
            }
        }
    }

    /** @see IConnectionService#reject(String) */
    void reject(Call call) {
        final String callId = mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("reject")) {
            try {
                logOutgoing("reject %s", callId);
                mServiceInterface.reject(callId);
            } catch (RemoteException e) {
            }
        }
    }

    /** @see IConnectionService#playDtmfTone(String,char) */
    void playDtmfTone(Call call, char digit) {
        final String callId = mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("playDtmfTone")) {
            try {
                logOutgoing("playDtmfTone %s %c", callId, digit);
                mServiceInterface.playDtmfTone(callId, digit);
            } catch (RemoteException e) {
            }
        }
    }

    /** @see IConnectionService#stopDtmfTone(String) */
    void stopDtmfTone(Call call) {
        final String callId = mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("stopDtmfTone")) {
            try {
                logOutgoing("stopDtmfTone %s",callId);
                mServiceInterface.stopDtmfTone(callId);
            } catch (RemoteException e) {
            }
        }
    }

    void addCall(Call call) {
        if (mCallIdMapper.getCallId(call) == null) {
            mCallIdMapper.addCall(call);
        }
    }

    /**
     * Associates newCall with this connection service by replacing callToReplace.
     */
    void replaceCall(Call newCall, Call callToReplace) {
        Preconditions.checkState(callToReplace.getConnectionService() == this);
        mCallIdMapper.replaceCall(newCall, callToReplace);
    }

    void removeCall(Call call) {
        removeCall(call, new DisconnectCause(DisconnectCause.ERROR));
    }

    void removeCall(String callId, DisconnectCause disconnectCause) {
        CreateConnectionResponse response = mPendingResponses.remove(callId);
        if (response != null) {
            response.handleCreateConnectionFailure(disconnectCause);
        }

        mCallIdMapper.removeCall(callId);
    }

    void removeCall(Call call, DisconnectCause disconnectCause) {
        CreateConnectionResponse response = mPendingResponses.remove(mCallIdMapper.getCallId(call));
        if (response != null) {
            response.handleCreateConnectionFailure(disconnectCause);
        }

        mCallIdMapper.removeCall(call);
    }

    void onPostDialContinue(Call call, boolean proceed) {
        final String callId = mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("onPostDialContinue")) {
            try {
                logOutgoing("onPostDialContinue %s %b", callId, proceed);
                mServiceInterface.onPostDialContinue(callId, proceed);
            } catch (RemoteException ignored) {
            }
        }
    }

    void conference(final Call call, Call otherCall) {
        final String callId = mCallIdMapper.getCallId(call);
        final String otherCallId = mCallIdMapper.getCallId(otherCall);
        if (callId != null && otherCallId != null && isServiceValid("conference")) {
            try {
                logOutgoing("conference %s %s", callId, otherCallId);
                mServiceInterface.conference(callId, otherCallId);
            } catch (RemoteException ignored) {
            }
        }
    }

    void splitFromConference(Call call) {
        final String callId = mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("splitFromConference")) {
            try {
                logOutgoing("splitFromConference %s", callId);
                mServiceInterface.splitFromConference(callId);
            } catch (RemoteException ignored) {
            }
        }
    }

    void mergeConference(Call call) {
        final String callId = mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("mergeConference")) {
            try {
                logOutgoing("mergeConference %s", callId);
                mServiceInterface.mergeConference(callId);
            } catch (RemoteException ignored) {
            }
        }
    }

    void swapConference(Call call) {
        final String callId = mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("swapConference")) {
            try {
                logOutgoing("swapConference %s", callId);
                mServiceInterface.swapConference(callId);
            } catch (RemoteException ignored) {
            }
        }
    }

    void hangupAll(Call call) {
        if (isServiceValid("hangupAll")) {
            try {
                logOutgoing("hangupAll %s", mCallIdMapper.getCallId(call));
                mServiceInterface.hangupAll(mCallIdMapper.getCallId(call));
            } catch (RemoteException e) {
            }
        }
    }

    /// M: For volte @{
    void inviteConferenceParticipants(Call conferenceCall, List<String> numbers) {
        final String conferenceCallId = mCallIdMapper.getCallId(conferenceCall);
        if (conferenceCallId != null && isServiceValid("inviteConferenceParticipants")) {
            try {
                logOutgoing("inviteConferenceParticipants %s", conferenceCallId);
                mServiceInterface.inviteConferenceParticipants(conferenceCallId, numbers);
            } catch (RemoteException ignored) {
            }
        }
    }
    /// @}

    /** {@inheritDoc} */
    @Override
    protected void setServiceInterface(IBinder binder) {
        if (binder == null) {
            // We have lost our service connection. Notify the world that this service is done.
            // We must notify the adapter before CallsManager. The adapter will force any pending
            // outgoing calls to try the next service. This needs to happen before CallsManager
            // tries to clean up any calls still associated with this service.
            handleConnectionServiceDeath();
            mCallsManager.handleConnectionServiceDeath(this);
            mServiceInterface = null;
        } else {
            mServiceInterface = IConnectionService.Stub.asInterface(binder);
            addConnectionServiceAdapter(mAdapter);
        }
    }

    private void handleCreateConnectionComplete(
            String callId,
            ConnectionRequest request,
            ParcelableConnection connection) {
        // TODO: Note we are not using parameter "request", which is a side effect of our tacit
        // assumption that we have at most one outgoing connection attempt per ConnectionService.
        // This may not continue to be the case.
        if (connection.getState() == Connection.STATE_DISCONNECTED) {
            // A connection that begins in the DISCONNECTED state is an indication of
            // failure to connect; we handle all failures uniformly
            removeCall(callId, connection.getDisconnectCause());
        } else {
            // Successful connection
            if (mPendingResponses.containsKey(callId)) {
                String num = connection.getHandle().getSchemeSpecificPart();
                /// M: add for CMCC L + C ecc retry
                if (PhoneNumberUtils.isEmergencyNumber(num)) {
                    mPendingResponses.get(callId).
                             handleCreateConnectionSuccess(mCallIdMapper, connection);
                } else {
                    mPendingResponses.remove(callId)
                            .handleCreateConnectionSuccess(mCallIdMapper, connection);
                }
            }
        }
    }

    /// M: For VoLTE conference @{
    private void handleCreateConferenceComplete(
            String conferenceId,
            ConnectionRequest request,
            ParcelableConference conference) {
        // TODO: Note we are not using parameter "request", which is a side effect of our tacit
        // assumption that we have at most one outgoing connection attempt per ConnectionService.
        // This may not continue to be the case.
        if (conference.getState() == Connection.STATE_DISCONNECTED) {
            // A connection that begins in the DISCONNECTED state is an indication of
            // failure to connect; we handle all failures uniformly
            removeCall(conferenceId, conference.getDisconnectCause());
        } else {
            // Successful connection
            if (mPendingResponses.containsKey(conferenceId)) {
                mPendingResponses.remove(conferenceId)
                        .handleCreateConferenceSuccess(mCallIdMapper, conference);
            }
        }
    }
    /// @}

    /**
     * Called when the associated connection service dies.
     */
    private void handleConnectionServiceDeath() {
        if (!mPendingResponses.isEmpty()) {
            CreateConnectionResponse[] responses = mPendingResponses.values().toArray(
                    new CreateConnectionResponse[mPendingResponses.values().size()]);
            mPendingResponses.clear();
            for (int i = 0; i < responses.length; i++) {
                responses[i].handleCreateConnectionFailure(
                        new DisconnectCause(DisconnectCause.ERROR));
            }
        }
        mCallIdMapper.clear();
    }

    private void logIncoming(String msg, Object... params) {
        Log.d(this, "ConnectionService -> Telecom: " + msg, params);
    }

    private void logOutgoing(String msg, Object... params) {
        Log.d(this, "Telecom -> ConnectionService: " + msg, params);
    }

    private void queryRemoteConnectionServices(final RemoteServiceCallback callback) {
        // Only give remote connection services to this connection service if it is listed as
        // the connection manager.
        PhoneAccountHandle simCallManager = mPhoneAccountRegistrar.getSimCallManager();
        Log.d(this, "queryRemoteConnectionServices finds simCallManager = %s", simCallManager);
        if (simCallManager == null ||
                !simCallManager.getComponentName().equals(getComponentName())) {
            noRemoteServices(callback);
            return;
        }

        // Make a list of ConnectionServices that are listed as being associated with SIM accounts
        final Set<ConnectionServiceWrapper> simServices = Collections.newSetFromMap(
                new ConcurrentHashMap<ConnectionServiceWrapper, Boolean>(8, 0.9f, 1));
        for (PhoneAccountHandle handle : mPhoneAccountRegistrar.getSimPhoneAccounts()) {
            ConnectionServiceWrapper service = mConnectionServiceRepository.getService(
                    handle.getComponentName(), handle.getUserHandle());
            if (service != null) {
                simServices.add(service);
            }
        }

        final List<ComponentName> simServiceComponentNames = new ArrayList<>();
        final List<IBinder> simServiceBinders = new ArrayList<>();

        Log.v(this, "queryRemoteConnectionServices, simServices = %s", simServices);

        for (ConnectionServiceWrapper simService : simServices) {
            if (simService == this) {
                // Only happens in the unlikely case that a SIM service is also a SIM call manager
                continue;
            }

            final ConnectionServiceWrapper currentSimService = simService;

            currentSimService.mBinder.bind(new BindCallback() {
                @Override
                public void onSuccess() {
                    Log.d(this, "Adding simService %s", currentSimService.getComponentName());
                    simServiceComponentNames.add(currentSimService.getComponentName());
                    simServiceBinders.add(currentSimService.mServiceInterface.asBinder());
                    maybeComplete();
                }

                @Override
                public void onFailure() {
                    Log.d(this, "Failed simService %s", currentSimService.getComponentName());
                    // We know maybeComplete() will always be a no-op from now on, so go ahead and
                    // signal failure of the entire request
                    noRemoteServices(callback);
                }

                private void maybeComplete() {
                    if (simServiceComponentNames.size() == simServices.size()) {
                        setRemoteServices(callback, simServiceComponentNames, simServiceBinders);
                    }
                }
            }, null);
        }
    }

    private void setRemoteServices(
            RemoteServiceCallback callback,
            List<ComponentName> componentNames,
            List<IBinder> binders) {
        try {
            callback.onResult(componentNames, binders);
        } catch (RemoteException e) {
            Log.e(this, e, "Contacting ConnectionService %s",
                    ConnectionServiceWrapper.this.getComponentName());
        }
    }

    private void noRemoteServices(RemoteServiceCallback callback) {
        setRemoteServices(callback, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    }

    void explicitCallTransfer(Call call) {
        final String callId = mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("explicitCallTransfer")) {
            try {
                logOutgoing("explicitCallTransfer %s", callId);
                mServiceInterface.explicitCallTransfer(callId);
            } catch (RemoteException ignored) {
            }
        }
    }

    /// M: for log parser @{
    public CallIdMapper getCallIdMap() {
        return mCallIdMapper;
    }
    /// @}
}
