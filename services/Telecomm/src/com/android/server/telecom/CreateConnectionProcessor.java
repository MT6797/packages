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

import android.content.Context;
import android.telecom.DisconnectCause;
import android.telecom.ParcelableConference;
import android.telecom.ParcelableConnection;
import android.telecom.Phone;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;

import com.mediatek.telecom.TelecomUtils;
// TODO: Needed for move to system service: import com.android.internal.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Objects;

/**
 * This class creates connections to place new outgoing calls or to attach to an existing incoming
 * call. In either case, this class cycles through a set of connection services until:
 *   - a connection service returns a newly created connection in which case the call is displayed
 *     to the user
 *   - a connection service cancels the process, in which case the call is aborted
 */
final class CreateConnectionProcessor {

    // Describes information required to attempt to make a phone call
    private static class CallAttemptRecord {
        // The PhoneAccount describing the target connection service which we will
        // contact in order to process an attempt
        public final PhoneAccountHandle connectionManagerPhoneAccount;
        // The PhoneAccount which we will tell the target connection service to use
        // for attempting to make the actual phone call
        public final PhoneAccountHandle targetPhoneAccount;

        public CallAttemptRecord(
                PhoneAccountHandle connectionManagerPhoneAccount,
                PhoneAccountHandle targetPhoneAccount) {
            this.connectionManagerPhoneAccount = connectionManagerPhoneAccount;
            this.targetPhoneAccount = targetPhoneAccount;
        }

        @Override
        public String toString() {
            return "CallAttemptRecord("
                    + Objects.toString(connectionManagerPhoneAccount) + ","
                    + Objects.toString(targetPhoneAccount) + ")";
        }

        /**
         * Determines if this instance of {@code CallAttemptRecord} has the same underlying
         * {@code PhoneAccountHandle}s as another instance.
         *
         * @param obj The other instance to compare against.
         * @return {@code True} if the {@code CallAttemptRecord}s are equal.
         */
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof CallAttemptRecord) {
                CallAttemptRecord other = (CallAttemptRecord) obj;
                return Objects.equals(connectionManagerPhoneAccount,
                        other.connectionManagerPhoneAccount) &&
                        Objects.equals(targetPhoneAccount, other.targetPhoneAccount);
            }
            return false;
        }
    }

    private final Call mCall;
    private final ConnectionServiceRepository mRepository;
    private List<CallAttemptRecord> mAttemptRecords;
    private Iterator<CallAttemptRecord> mAttemptRecordIterator;
    private CreateConnectionResponse mResponse;
    private DisconnectCause mLastErrorDisconnectCause;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;
    private final Context mContext;
    private boolean mShouldUseConnectionManager = true;
    private CreateConnectionTimeout mTimeout;

    CreateConnectionProcessor(
            Call call, ConnectionServiceRepository repository, CreateConnectionResponse response,
            PhoneAccountRegistrar phoneAccountRegistrar, Context context) {
        Log.v(this, "CreateConnectionProcessor created for Call = %s", call);
        mCall = call;
        mRepository = repository;
        mResponse = response;
        mPhoneAccountRegistrar = phoneAccountRegistrar;
        mContext = context;
    }

    boolean isProcessingComplete() {
        return mResponse == null;
    }

    boolean isCallTimedOut() {
        return mTimeout != null && mTimeout.isCallTimedOut();
    }

    void process() {
        Log.v(this, "process");
        clearTimeout();
        mAttemptRecords = new ArrayList<>();
        if (mCall.getTargetPhoneAccount() != null) {
            mAttemptRecords.add(new CallAttemptRecord(
                    mCall.getTargetPhoneAccount(), mCall.getTargetPhoneAccount()));
        }
        adjustAttemptsForConnectionManager();
        adjustAttemptsForEmergency();
        mAttemptRecordIterator = mAttemptRecords.iterator();
        attemptNextPhoneAccount();
    }

    boolean hasMorePhoneAccounts() {
        return mAttemptRecordIterator.hasNext();
    }

    void continueProcessingIfPossible(CreateConnectionResponse response,
            DisconnectCause disconnectCause) {
        Log.v(this, "continueProcessingIfPossible");
        mResponse = response;
        mLastErrorDisconnectCause = disconnectCause;
        attemptNextPhoneAccount();
    }

    void abort() {
        Log.v(this, "abort");

        // Clear the response first to prevent attemptNextConnectionService from attempting any
        // more services.
        CreateConnectionResponse response = mResponse;
        mResponse = null;
        clearTimeout();

        ConnectionServiceWrapper service = mCall.getConnectionService();
        if (service != null) {
            service.abort(mCall);
            mCall.clearConnectionService();
        }
        if (response != null) {
            response.handleCreateConnectionFailure(new DisconnectCause(DisconnectCause.LOCAL));
        }
    }

    private void attemptNextPhoneAccount() {
        Log.v(this, "attemptNextPhoneAccount");
        CallAttemptRecord attempt = null;
        if (mAttemptRecordIterator.hasNext()) {
            attempt = mAttemptRecordIterator.next();

            if (!mPhoneAccountRegistrar.phoneAccountRequiresBindPermission(
                    attempt.connectionManagerPhoneAccount)) {
                Log.w(this,
                        "Connection mgr does not have BIND_TELECOM_CONNECTION_SERVICE for "
                                + "attempt: %s", attempt);
                attemptNextPhoneAccount();
                return;
            }

            // If the target PhoneAccount differs from the ConnectionManager phone acount, ensure it
            // also requires the BIND_TELECOM_CONNECTION_SERVICE permission.
            if (!attempt.connectionManagerPhoneAccount.equals(attempt.targetPhoneAccount) &&
                    !mPhoneAccountRegistrar.phoneAccountRequiresBindPermission(
                            attempt.targetPhoneAccount)) {
                Log.w(this,
                        "Target PhoneAccount does not have BIND_TELECOM_CONNECTION_SERVICE for "
                                + "attempt: %s", attempt);
                attemptNextPhoneAccount();
                return;
            }
        }

        if (mResponse != null && attempt != null) {
            Log.i(this, "Trying attempt %s", attempt);
            PhoneAccountHandle phoneAccount = attempt.connectionManagerPhoneAccount;
            ConnectionServiceWrapper service =
                    mRepository.getService(
                            phoneAccount.getComponentName(),
                            phoneAccount.getUserHandle());
            if (service == null) {
                Log.i(this, "Found no connection service for attempt %s", attempt);
                attemptNextPhoneAccount();
            } else {
                mCall.setConnectionManagerPhoneAccount(attempt.connectionManagerPhoneAccount);
                /// M: Valid phone account for ECC may have been set.@{
                if (mCall.getTargetPhoneAccount() == null) {
                    mCall.setTargetPhoneAccount(attempt.targetPhoneAccount);
                }
                /// @}
                mCall.setConnectionService(service);
                setTimeoutIfNeeded(service, attempt);

                service.createConnection(mCall, new Response(service));
            }
        } else {
            Log.v(this, "attemptNextPhoneAccount, no more accounts, failing");
            if (mResponse != null) {
                clearTimeout();
                mResponse.handleCreateConnectionFailure(mLastErrorDisconnectCause != null ?
                        mLastErrorDisconnectCause : new DisconnectCause(DisconnectCause.ERROR));
                mResponse = null;
                mCall.clearConnectionService();
            }
        }
    }

    private void setTimeoutIfNeeded(ConnectionServiceWrapper service, CallAttemptRecord attempt) {
        clearTimeout();

        CreateConnectionTimeout timeout = new CreateConnectionTimeout(
                mContext, mPhoneAccountRegistrar, service, mCall);
        if (timeout.isTimeoutNeededForCall(getConnectionServices(mAttemptRecords),
                attempt.connectionManagerPhoneAccount)) {
            mTimeout = timeout;
            timeout.registerTimeout();
        }
    }

    private void clearTimeout() {
        if (mTimeout != null) {
            mTimeout.unregisterTimeout();
            mTimeout = null;
        }
    }

    private boolean shouldSetConnectionManager() {
        if (!mShouldUseConnectionManager) {
            return false;
        }

        if (mAttemptRecords.size() == 0) {
            return false;
        }

        if (mAttemptRecords.size() > 1) {
            Log.d(this, "shouldSetConnectionManager, error, mAttemptRecords should not have more "
                    + "than 1 record");
            return false;
        }

        PhoneAccountHandle connectionManager = mPhoneAccountRegistrar.getSimCallManager();
        if (connectionManager == null) {
            return false;
        }

        PhoneAccountHandle targetPhoneAccountHandle = mAttemptRecords.get(0).targetPhoneAccount;
        if (Objects.equals(connectionManager, targetPhoneAccountHandle)) {
            return false;
        }

        // Connection managers are only allowed to manage SIM subscriptions.
        // TODO: Should this really be checking the "calling user" test for phone account?
        PhoneAccount targetPhoneAccount = mPhoneAccountRegistrar.getPhoneAccountCheckCallingUser(
                targetPhoneAccountHandle);
        if (targetPhoneAccount == null) {
            Log.d(this, "shouldSetConnectionManager, phone account not found");
            return false;
        }
        boolean isSimSubscription = (targetPhoneAccount.getCapabilities() &
                PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION) != 0;
        if (!isSimSubscription) {
            return false;
        }

        return true;
    }

    // If there exists a registered connection manager then use it.
    private void adjustAttemptsForConnectionManager() {
        if (shouldSetConnectionManager()) {
            CallAttemptRecord record = new CallAttemptRecord(
                    mPhoneAccountRegistrar.getSimCallManager(),
                    mAttemptRecords.get(0).targetPhoneAccount);
            Log.v(this, "setConnectionManager, changing %s -> %s", mAttemptRecords.get(0), record);
            mAttemptRecords.set(0, record);
        } else {
            Log.v(this, "setConnectionManager, not changing");
        }
    }

    // If we are possibly attempting to call a local emergency number, ensure that the
    // plain PSTN connection services are listed, and nothing else.
    private void adjustAttemptsForEmergency()  {
        if (TelephonyUtil.shouldProcessAsEmergency(mContext, mCall.getHandle())) {
            Log.i(this, "Emergency number detected");
            mAttemptRecords.clear();
            List<PhoneAccount> allAccounts = mPhoneAccountRegistrar.getAllPhoneAccounts();
            // First, add SIM phone accounts which can place emergency calls.
            for (PhoneAccount phoneAccount : allAccounts) {
                if (phoneAccount.hasCapabilities(PhoneAccount.CAPABILITY_PLACE_EMERGENCY_CALLS) &&
                        phoneAccount.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)) {
                    Log.i(this, "Will try PSTN account %s for emergency",
                            phoneAccount.getAccountHandle());
                    mAttemptRecords.add(
                            new CallAttemptRecord(
                                    phoneAccount.getAccountHandle(),
                                    phoneAccount.getAccountHandle()));
                }
            }

            // Next, add the connection manager account as a backup if it can place emergency calls.
            PhoneAccountHandle callManagerHandle = mPhoneAccountRegistrar.getSimCallManager();
            if (mShouldUseConnectionManager && callManagerHandle != null) {
                // TODO: Should this really be checking the "calling user" test for phone account?
                PhoneAccount callManager = mPhoneAccountRegistrar
                        .getPhoneAccountCheckCallingUser(callManagerHandle);
                if (callManager != null && callManager.hasCapabilities(
                        PhoneAccount.CAPABILITY_PLACE_EMERGENCY_CALLS)) {
                    CallAttemptRecord callAttemptRecord = new CallAttemptRecord(callManagerHandle,
                            mPhoneAccountRegistrar.
                                    getOutgoingPhoneAccountForScheme(mCall.getHandle().getScheme())
                    );

                    if (!mAttemptRecords.contains(callAttemptRecord)) {
                        Log.i(this, "Will try Connection Manager account %s for emergency",
                                callManager);
                        mAttemptRecords.add(callAttemptRecord);
                    }
                }
            }

            // M: for ALPS01809899, never register emergency account to TelecomManager,
            // because it just use at here, AP never want it.
            // so create emergency account at here and do not register. @{
            if (mAttemptRecords.isEmpty()) {
                // If the list of phone accounts is empty at this point, it means Telephony hasn't
                // registered emergency account. Add a fallback emergency phone account so
                // that emergency calls can still go through.
                PhoneAccount emergencyAccount = TelephonyUtil.getDefaultEmergencyPhoneAccount();
                mAttemptRecords.add(
                        new CallAttemptRecord(
                                emergencyAccount.getAccountHandle(),
                                emergencyAccount.getAccountHandle()));
            }
            /// @}
        }
    }

    /** Returns all connection services used by the call attempt records. */
    private static Collection<PhoneAccountHandle> getConnectionServices(
            List<CallAttemptRecord> records) {
        HashSet<PhoneAccountHandle> result = new HashSet<>();
        for (CallAttemptRecord record : records) {
            result.add(record.connectionManagerPhoneAccount);
        }
        return result;
    }

    private class Response implements CreateConnectionResponse {
        private final ConnectionServiceWrapper mService;

        Response(ConnectionServiceWrapper service) {
            mService = service;
        }

        @Override
        public void handleCreateConnectionSuccess(
                CallIdMapper idMapper,
                ParcelableConnection connection) {
            if (mResponse == null) {
                // Nobody is listening for this connection attempt any longer; ask the responsible
                // ConnectionService to tear down any resources associated with the call
                mService.abort(mCall);
            } else {
                // Success -- share the good news and remember that we are no longer interested
                // in hearing about any more attempts
                mResponse.handleCreateConnectionSuccess(idMapper, connection);
                ///M: for emergency call retry,not to set mResponse to null before dialing success
                // to avoid call abort.
                // mark the google original code below  @{
                //  mResponse = null;

                //for Ecc retry, we will call handleCreateConnectionSuccess when first
                // return "connecting" state, then after dial out,it will call it again with
                // state "dialing", if we set "mResponse" to null, the next time call would abort.
                // so only Emergency && connecting state would not reset mResponse to null. @{
                String number = connection.getHandle() == null ?
                        null : connection.getHandle().getSchemeSpecificPart();
                if (!PhoneNumberUtils.isEmergencyNumber(number)) {
                    mResponse = null;
                }
                /// @}

                // If there's a timeout running then don't clear it. The timeout can be triggered
                // after the call has successfully been created but before it has become active.
            }
        }

        private boolean shouldFallbackToNoConnectionManager(DisconnectCause cause) {
            PhoneAccountHandle handle = mCall.getConnectionManagerPhoneAccount();
            if (handle == null || !handle.equals(mPhoneAccountRegistrar.getSimCallManager())) {
                return false;
            }

            ConnectionServiceWrapper connectionManager = mCall.getConnectionService();
            if (connectionManager == null) {
                return false;
            }

            if (cause.getCode() == DisconnectCause.CONNECTION_MANAGER_NOT_SUPPORTED) {
                Log.d(CreateConnectionProcessor.this, "Connection manager declined to handle the "
                        + "call, falling back to not using a connection manager");
                return true;
            }

            if (!connectionManager.isServiceValid("createConnection")) {
                Log.d(CreateConnectionProcessor.this, "Connection manager unbound while trying "
                        + "create a connection, falling back to not using a connection manager");
                return true;
            }

            return false;
        }

        @Override
        public void handleCreateConnectionFailure(DisconnectCause errorDisconnectCause) {
            // Failure of some sort; record the reasons for failure and try again if possible
            Log.d(CreateConnectionProcessor.this, "Connection failed: (%s)", errorDisconnectCause);
            mLastErrorDisconnectCause = errorDisconnectCause;
            if (shouldFallbackToNoConnectionManager(errorDisconnectCause)) {
                mShouldUseConnectionManager = false;
                // Restart from the beginning.
                process();
            } else {
                attemptNextPhoneAccount();
            }
        }

        @Override
        public void handleCreateConferenceSuccess(
                CallIdMapper idMapper,
                ParcelableConference conference) {
            if (mResponse == null) {
                // Nobody is listening for this connection attempt any longer; ask the responsible
                // ConnectionService to tear down any resources associated with the call
                mService.abort(mCall);
            } else {
                // Success -- share the good news and remember that we are no longer interested
                // in hearing about any more attempts
                mResponse.handleCreateConferenceSuccess(idMapper, conference);
                mResponse = null;
            }
        }
    }

    /** M: add for for Ecc retry.
     * after emergency call disconnected, reset "mResponse" to null; @{
     **/
    void resetReponseObj(){
        if (mResponse != null) {
            mResponse = null;
        }
    }
    /** @} */
}
