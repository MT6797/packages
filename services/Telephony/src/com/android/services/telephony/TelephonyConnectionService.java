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
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
/// M: @{
import android.provider.Settings;
/// @}
/// M: For VoLTE enhanced conference call. @{
import android.telecom.Conference;
/// @}
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
/// M: @{
import android.widget.Toast;
/// @}

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.cdma.CDMAPhone;
/// M: @{
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
/// @}
import com.android.phone.MMIDialogActivity;
import com.android.phone.PhoneUtils;
import com.android.phone.R;

/// M: CC081 Choose 3G-capable phone for ECC @{
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
/// @}

/// M: CC022: Error message due to VoLTE SS checking @{
import com.mediatek.telecom.TelecomManagerEx;
/// @}

import java.util.ArrayList;
/// M: Add for cdma call handle
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/// M: CC093:  Get iccid from system property @{
import com.mediatek.telephony.TelephonyManagerEx;
/// @}

/**
 * Service for making GSM and CDMA connections.
 */
public class TelephonyConnectionService extends ConnectionService {

    // If configured, reject attempts to dial numbers matching this pattern.
    private static final Pattern CDMA_ACTIVATION_CODE_REGEX_PATTERN =
            Pattern.compile("\\*228[0-9]{0,2}");

    private final TelephonyConferenceController mTelephonyConferenceController =
            new TelephonyConferenceController(this);
    private final CdmaConferenceController mCdmaConferenceController =
            new CdmaConferenceController(this);
    private final ImsConferenceController mImsConferenceController =
            new ImsConferenceController(this);

    private ComponentName mExpectedComponentName = null;
    private EmergencyCallHelper mEmergencyCallHelper;
    private EmergencyTonePlayer mEmergencyTonePlayer;

    /**
     * A listener to actionable events specific to the TelephonyConnection.
     */
    private final TelephonyConnection.TelephonyConnectionListener mTelephonyConnectionListener =
            new TelephonyConnection.TelephonyConnectionListener() {
        @Override
        public void onOriginalConnectionConfigured(TelephonyConnection c) {
            addConnectionToConferenceController(c);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mExpectedComponentName = new ComponentName(this, this.getClass());
        mEmergencyTonePlayer = new EmergencyTonePlayer(this);
        TelecomAccountRegistry.getInstance(this).setTelephonyConnectionService(this);
        /// M: CC023: Use TelephonyConnectionServiceUtil @{
        TelephonyConnectionServiceUtil.getInstance().setService(this);
        /// @}
    }

    /// M: CC023: Use TelephonyConnectionServiceUtil @{
    @Override
    public void onDestroy() {
        TelephonyConnectionServiceUtil.getInstance().unsetService();
        mCdmaConferenceController.onDestroy();
        /// M: SS project ECC change feature @{
        if (mEmergencyCallHelper != null) {
            mEmergencyCallHelper.onDestroy();
        }
        /// @}
        super.onDestroy();
    }
    /// @}

    @Override
    public Connection onCreateOutgoingConnection(
            PhoneAccountHandle connectionManagerPhoneAccount,
            final ConnectionRequest request) {
        Log.i(this, "onCreateOutgoingConnection, request: " + request);
        /// M: clarify the correct PhoneAccountHandle used.@{
        Log.d(this, "onCreateOutgoingConnection, ConnectionRequest." + request.getAccountHandle());
        /// @}

        Uri handle = request.getAddress();
        if (handle == null) {
            Log.d(this, "onCreateOutgoingConnection, handle is null");
            return Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.NO_PHONE_NUMBER_SUPPLIED,
                            "No phone number supplied"));
        }

        /// M: [ALPS02340908] To avoid JE @{
        if (request.getAccountHandle() == null) {
            Log.d(this, "onCreateOutgoingConnection, PhoneAccountHandle is null");
            return Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.NO_PHONE_NUMBER_SUPPLIED,
                            "No phone number supplied"));
        }
        /// @}
        /// M: ECC Retry @{
        if (TelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
            int phoneId = SubscriptionManager.INVALID_PHONE_INDEX;
            try {
                phoneId = Integer.parseInt(request.getAccountHandle().getId());
            } catch (NumberFormatException e) {
                phoneId = SubscriptionManager.INVALID_PHONE_INDEX;
            } finally {
                if (PhoneFactory.getPhone(phoneId) == null) {
                   Log.d(this, "ECC Retry : clear ECC param due to unmatched phoneId");
                   TelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
                }
            }
        }
        /// @}

        String scheme = handle.getScheme();
        final String number;
        if (PhoneAccount.SCHEME_VOICEMAIL.equals(scheme)) {
            // TODO: We don't check for SecurityException here (requires
            // CALL_PRIVILEGED permission).
            final Phone phone = getPhoneForAccount(request.getAccountHandle(), false);
            if (phone == null) {
                Log.d(this, "onCreateOutgoingConnection, phone is null");
                return Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                android.telephony.DisconnectCause.OUT_OF_SERVICE,
                                "Phone is null"));
            }
            number = phone.getVoiceMailNumber();
            if (TextUtils.isEmpty(number)) {
                Log.d(this, "onCreateOutgoingConnection, no voicemail number set.");
                return Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                android.telephony.DisconnectCause.VOICEMAIL_NUMBER_MISSING,
                                "Voicemail scheme provided but no voicemail number set."));
            }

            // Convert voicemail: to tel:
            handle = Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null);
        } else {
            /// M: [ALPS01906649] For VoLTE, Allow SIP URI to be dialed out @{
            if (!PhoneAccount.SCHEME_TEL.equals(scheme) && !PhoneAccount.SCHEME_SIP.equals(scheme)) {
                Log.d(this, "onCreateOutgoingConnection, Handle %s is not type tel or sip", scheme);
                return Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                android.telephony.DisconnectCause.INVALID_NUMBER,
                                "Handle scheme is not type tel or sip"));
            }
            /// @}

            number = handle.getSchemeSpecificPart();
            if (TextUtils.isEmpty(number)) {
                Log.d(this, "onCreateOutgoingConnection, unable to parse number");
                return Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                android.telephony.DisconnectCause.INVALID_NUMBER,
                                "Unable to parse number"));
            }


            /// M: ECC Retry @{
            //final Phone phone = getPhoneForAccount(request.getAccountHandle(), false);
            Phone phone = null;
            if (!TelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                phone = getPhoneForAccount(request.getAccountHandle(), false);
            }
            /// @}

            if (phone != null && CDMA_ACTIVATION_CODE_REGEX_PATTERN.matcher(number).matches()) {
                // Obtain the configuration for the outgoing phone's SIM. If the outgoing number
                // matches the *228 regex pattern, fail the call. This number is used for OTASP, and
                // when dialed could lock LTE SIMs to 3G if not prohibited..
                boolean disableActivation = false;
                CarrierConfigManager cfgManager = (CarrierConfigManager)
                        phone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
                if (cfgManager != null) {
                    disableActivation = cfgManager.getConfigForSubId(phone.getSubId())
                            .getBoolean(CarrierConfigManager.KEY_DISABLE_CDMA_ACTIVATION_CODE_BOOL);
                }

                if (disableActivation) {
                    return Connection.createFailedConnection(
                            DisconnectCauseUtil.toTelecomDisconnectCause(
                                    android.telephony.DisconnectCause
                                            .CDMA_ALREADY_ACTIVATED,
                                    "Tried to dial *228"));
                }
            }
        }

        boolean isEmergencyNumber = PhoneNumberUtils.isLocalEmergencyNumber(this, number);

        /// M: ECC special handle @{
        // request.getAccountHandle() won't be null
        Phone phone = null;
        if (isEmergencyNumber) {
            phone = TelephonyConnectionServiceUtil.getInstance()
                    .selectPhoneBySpecialEccRule(request.getAccountHandle(), number);
        }
        /// @}

        // Get the right phone object from the account data passed in.
        if (phone == null) {
            phone = getPhoneForAccount(request.getAccountHandle(), isEmergencyNumber);
        }

        if (phone == null) {
            Log.d(this, "onCreateOutgoingConnection, phone is null");
            /// M: CC021: Error message due to CellConnMgr checking @{
            Log.d(this, "onCreateOutgoingConnection, use default phone for cellConnMgr");
            if (TelephonyConnectionServiceUtil.getInstance().
                    cellConnMgrShowAlerting(PhoneFactory.getDefaultPhone().getSubId())) {
                Log.d(this, "onCreateOutgoingConnection, cellConnMgrShowAlerting() check fail");
                return Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                android.telephony.DisconnectCause.OUTGOING_CANCELED_BY_SERVICE,
                                "cellConnMgrShowAlerting() check fail"));
            }
            /// @}
            return Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.OUT_OF_SERVICE, "Phone is null"));
        }

        ///M: add for plug in.@{
        if (TelephonyConnectionServiceUtil.getInstance().
                isDataOnlyMode(phone)) {
            return Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.OUTGOING_CANCELED, null));
        }
        /// @}

        // Check both voice & data RAT to enable normal CS call,
        // when voice RAT is OOS but Data RAT is present.
        int state = phone.getServiceState().getState();
        if (state == ServiceState.STATE_OUT_OF_SERVICE) {
            if (phone.getServiceState().getDataNetworkType() == TelephonyManager.NETWORK_TYPE_LTE) {
                state = phone.getServiceState().getDataRegState();
            }
        }

        /// M : WFC <TO make MO call when WFC is on and radio is off> @{
        boolean isWfcEnabled = ((TelephonyManager)phone.getContext()
                .getSystemService(Context.TELEPHONY_SERVICE)).isWifiCallingEnabled();
        if (!phone.isRadioOn() && isWfcEnabled) {
                state  = ServiceState.STATE_IN_SERVICE;;
        }
        ///@}
        /// M: Timing issue, radio maybe on even airplane mode on @{
        boolean isAirplaneModeOn = false;
        if (Settings.Global.getInt(phone.getContext().getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) > 0) {
            isAirplaneModeOn = true;
        }
        Log.d(this, " Service state:" + state + ", isAirplaneModeOn:" + isAirplaneModeOn);
        ///@}
        boolean useEmergencyCallHelper = false;

        if (isEmergencyNumber) {
            /// M: ECC Retry @{
            if (!TelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                Log.d(this, "ECC Retry : set param with Intial ECC.");
                TelephonyConnectionServiceUtil.getInstance().setEccRetryParams(
                        request,
                        phone.getPhoneId());
            }
            /// @}

            if (!phone.isRadioOn() || isAirplaneModeOn) {
                useEmergencyCallHelper = true;
            }
        } else {
            /// M: CC022: Error message due to VoLTE SS checking @{
            if (TelephonyConnectionServiceUtil.getInstance().
                    shouldOpenDataConnection(number, phone)) {
                Log.d(this, "onCreateOutgoingConnection, shouldOpenDataConnection() check fail");
                return Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                android.telephony.DisconnectCause.VOLTE_SS_DATA_OFF,
                                TelecomManagerEx.DISCONNECT_REASON_VOLTE_SS_DATA_OFF));
            }
            /// @}

            /// M: CC021: Error message due to CellConnMgr checking @{
            if (TelephonyConnectionServiceUtil.getInstance().
                    cellConnMgrShowAlerting(phone.getSubId())) {
                Log.d(this, "onCreateOutgoingConnection, cellConnMgrShowAlerting() check fail");
                return Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                android.telephony.DisconnectCause.OUTGOING_CANCELED_BY_SERVICE,
                                "cellConnMgrShowAlerting() check fail"));
            }
            /// @}

            switch (state) {
                case ServiceState.STATE_IN_SERVICE:
                case ServiceState.STATE_EMERGENCY_ONLY:
                    break;
                case ServiceState.STATE_OUT_OF_SERVICE:
                    return Connection.createFailedConnection(
                            DisconnectCauseUtil.toTelecomDisconnectCause(
                                    android.telephony.DisconnectCause.OUT_OF_SERVICE,
                                    "ServiceState.STATE_OUT_OF_SERVICE"));
                case ServiceState.STATE_POWER_OFF:
                    return Connection.createFailedConnection(
                            DisconnectCauseUtil.toTelecomDisconnectCause(
                                    android.telephony.DisconnectCause.POWER_OFF,
                                    "ServiceState.STATE_POWER_OFF"));
                default:
                    Log.d(this, "onCreateOutgoingConnection, unknown service state: %d", state);
                    return Connection.createFailedConnection(
                            DisconnectCauseUtil.toTelecomDisconnectCause(
                                    android.telephony.DisconnectCause.OUTGOING_FAILURE,
                                    "Unknown service state " + state));
            }

            /// M: CC027: Proprietary scheme to build Connection Capabilities @{
            if (!canDial(request.getAccountHandle(), number)) {
                Log.d(this, "onCreateOutgoingConnection, canDial() check fail");
                return Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                android.telephony.DisconnectCause.OUTGOING_FAILURE,
                                "canDial() check fail"));
            }
            /// @}
        }

        /// M: SS project ECC change feature @{
        //final TelephonyConnection connection =
        //        createConnectionFor(phone, null, true /* isOutgoing */, request.getAccountHandle());
        int switchPhoneType = TelephonyConnectionServiceUtil.getInstance().getSwitchPhoneType(
                number, phone, useEmergencyCallHelper);
        final TelephonyConnection connection;
        if (switchPhoneType != PhoneConstants.PHONE_TYPE_NONE) {
            connection = createConnectionForSwitchPhone(switchPhoneType, null, true,
                    request.getAccountHandle());
        } else {
            connection = createConnectionFor(phone, null, true /* isOutgoing */,
                    request.getAccountHandle());
        }
        /// @}
        if (connection == null) {
            /// M: ECC Retry @{
            // Not trigger retry since connection is null should be a bug
            // Assume only one ECC exists
            if (isEmergencyNumber
                    && TelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                Log.d(this, "ECC Retry : clear ECC param");
                TelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
            }
            /// @}

            return Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.OUTGOING_FAILURE,
                            "Invalid phone type"));
        }

        /// M: CC036: [ALPS01794357] Set PhoneAccountHandle for ECC @{
        if (isEmergencyNumber) {
            final PhoneAccountHandle phoneAccountHandle;
            /// M: CC093:  Get iccid from system property @{
            // when IccRecords is null, (updated as RILD is reinitialized).
            // [ALPS02312211] [ALPS02325107]
            String phoneIccId = phone.getIccSerialNumber();
            int slotId = SubscriptionController.getInstance().getSlotId(phone.getSubId());
            if (slotId != SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
                phoneIccId = !TextUtils.isEmpty(phoneIccId) ?
                        phoneIccId : TelephonyManagerEx.getDefault().getSimSerialNumber(slotId);
            }
            /// @}
            if (TextUtils.isEmpty(phoneIccId)) {
                // If No SIM is inserted, the corresponding IccId will be null,
                // take phoneId as PhoneAccountHandle::mId which is IccId originally
                phoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(
                        Integer.toString(phone.getPhoneId()));
            } else {
                phoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(phoneIccId);
            }
            Log.d(this, "ECC PhoneAccountHandle mId: %s , iccId: %s",
                    phoneAccountHandle.getId(), phoneIccId);
            connection.setAccountHandle(phoneAccountHandle);
        }
        /// @}

        /// M: ECC Retry @{
        connection.setEmergency(isEmergencyNumber);
        /// @}

        connection.setAddress(handle, PhoneConstants.PRESENTATION_ALLOWED);
        connection.setInitializing();
        connection.setVideoState(request.getVideoState());

        /// M: SS project ECC change feature @{
        if (TelephonyConnectionServiceUtil.getInstance().mayNeedToSwitchPhone(number,
                useEmergencyCallHelper)) {
            if (switchPhoneType == PhoneConstants.PHONE_TYPE_NONE && !useEmergencyCallHelper) {
                placeOutgoingConnection(connection, phone, request);
            } else {
                if (mEmergencyCallHelper == null) {
                    mEmergencyCallHelper = new EmergencyCallHelper(this);
                }
                final Phone eccPhone = phone;
                mEmergencyCallHelper.startSwitchPhone(number, eccPhone,
                        useEmergencyCallHelper,
                        new EmergencyCallHelper.Callback() {
                            @Override
                            public void onComplete(boolean isRadioReady) {
                                Log.d(this, "onComplete, isRadioReady:" + isRadioReady);
                                if (connection.getState() == Connection.STATE_DISCONNECTED) {
                                    Log.d(this, "onCreateOutgoingConnection, conn disconnected");
                                    // If the connection has already been disconnected, do nothing.
                                } else if (isRadioReady) {
                                    connection.setInitialized();
                                    placeOutgoingConnection(connection, eccPhone, request);
                                } else {
                                    /// M: ECC Retry @{
                                    // Assume only one ECC exists
                                    // Not trigger retry since Modem fails to power on should be a bug
                                    if (TelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                                        Log.d(this, "ECC Retry : clear ECC param");
                                        TelephonyConnectionServiceUtil.getInstance()
                                                .clearEccRetryParams();
                                    }
                                    /// @}
                                    Log.d(this, "onCreateOutgoingConnection, failed to power on");
                                    connection.setDisconnected(
                                            DisconnectCauseUtil.toTelecomDisconnectCause(
                                                    android.telephony.DisconnectCause.POWER_OFF,
                                                    "Failed to turn on radio."));
                                    connection.destroy();
                                }
                            }
                        });
            }
        /// @}
        } else if (useEmergencyCallHelper) {
            if (mEmergencyCallHelper == null) {
                mEmergencyCallHelper = new EmergencyCallHelper(this);
            }
            final Phone eccPhone = phone;
            mEmergencyCallHelper.startTurnOnRadioSequence(eccPhone,
                    new EmergencyCallHelper.Callback() {
                        @Override
                        public void onComplete(boolean isRadioReady) {
                            if (connection.getState() == Connection.STATE_DISCONNECTED) {
                                Log.d(this, "onCreateOutgoingConnection, connection disconnected");
                                // If the connection has already been disconnected, do nothing.
                            } else if (isRadioReady) {
                                connection.setInitialized();
                                placeOutgoingConnection(connection, eccPhone, request);
                            } else {
                                /// M: ECC Retry @{
                                // Assume only one ECC exists
                                // Not trigger retry since Modem fails to power on should be a bug
                                if (TelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                                    Log.d(this, "ECC Retry : clear ECC param");
                                    TelephonyConnectionServiceUtil.getInstance()
                                            .clearEccRetryParams();
                                }
                                /// @}

                                Log.d(this, "onCreateOutgoingConnection, failed to turn on radio");
                                connection.setDisconnected(
                                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                                android.telephony.DisconnectCause.POWER_OFF,
                                                "Failed to turn on radio."));
                                connection.destroy();
                            }
                        }
                    });

        } else {
            placeOutgoingConnection(connection, phone, request);
        }

        return connection;
    }

    @Override
    public Connection onCreateIncomingConnection(
            PhoneAccountHandle connectionManagerPhoneAccount,
            ConnectionRequest request) {
        Log.i(this, "onCreateIncomingConnection, request: " + request);
        // If there is an incoming emergency CDMA Call (while the phone is in ECBM w/ No SIM),
        // make sure the PhoneAccount lookup retrieves the default Emergency Phone.
        PhoneAccountHandle accountHandle = request.getAccountHandle();
        boolean isEmergency = false;
        if (accountHandle != null && PhoneUtils.EMERGENCY_ACCOUNT_HANDLE_ID.equals(
                accountHandle.getId())) {
            Log.i(this, "Emergency PhoneAccountHandle is being used for incoming call... " +
                    "Treat as an Emergency Call.");
            isEmergency = true;
        }
        Phone phone = getPhoneForAccount(accountHandle, isEmergency);
        if (phone == null) {
            return Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.ERROR_UNSPECIFIED,
                            "Phone is null"));
        }

        Call call = phone.getRingingCall();
        if (!call.getState().isRinging()) {
            Log.i(this, "onCreateIncomingConnection, no ringing call");
            return Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.INCOMING_MISSED,
                            "Found no ringing call"));
        }

        com.android.internal.telephony.Connection originalConnection =
                call.getState() == Call.State.WAITING ?
                    call.getLatestConnection() : call.getEarliestConnection();
        if (isOriginalConnectionKnown(originalConnection)) {
            Log.i(this, "onCreateIncomingConnection, original connection already registered");
            return Connection.createCanceledConnection();
        }

        Connection connection =
                createConnectionFor(phone, originalConnection, false /* isOutgoing */,
                        request.getAccountHandle());
        if (connection == null) {
            return Connection.createCanceledConnection();
        } else {
            return connection;
        }
    }

    @Override
    public Connection onCreateUnknownConnection(PhoneAccountHandle connectionManagerPhoneAccount,
            ConnectionRequest request) {
        Log.i(this, "onCreateUnknownConnection, request: " + request);
        // Use the registered emergency Phone if the PhoneAccountHandle is set to Telephony's
        // Emergency PhoneAccount
        PhoneAccountHandle accountHandle = request.getAccountHandle();
        boolean isEmergency = false;
        if (accountHandle != null && PhoneUtils.EMERGENCY_ACCOUNT_HANDLE_ID.equals(
                accountHandle.getId())) {
            Log.i(this, "Emergency PhoneAccountHandle is being used for unknown call... " +
                    "Treat as an Emergency Call.");
            isEmergency = true;
        }
        Phone phone = getPhoneForAccount(accountHandle, isEmergency);
        if (phone == null) {
            return Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.ERROR_UNSPECIFIED,
                            "Phone is null"));
        }

        final List<com.android.internal.telephony.Connection> allConnections = new ArrayList<>();
        final Call ringingCall = phone.getRingingCall();
        if (ringingCall.hasConnections()) {
            allConnections.addAll(ringingCall.getConnections());
        }
        final Call foregroundCall = phone.getForegroundCall();
        if (foregroundCall.hasConnections()) {
            allConnections.addAll(foregroundCall.getConnections());
        }
        final Call backgroundCall = phone.getBackgroundCall();
        if (backgroundCall.hasConnections()) {
            allConnections.addAll(phone.getBackgroundCall().getConnections());
        }

        com.android.internal.telephony.Connection unknownConnection = null;
        for (com.android.internal.telephony.Connection telephonyConnection : allConnections) {
            if (!isOriginalConnectionKnown(telephonyConnection)) {
                unknownConnection = telephonyConnection;
                break;
            }
        }

        if (unknownConnection == null) {
            Log.i(this, "onCreateUnknownConnection, did not find previously unknown connection.");
            return Connection.createCanceledConnection();
        }

        TelephonyConnection connection =
                createConnectionFor(phone, unknownConnection,
                        !unknownConnection.isIncoming() /* isOutgoing */,
                        request.getAccountHandle());

        if (connection == null) {
            return Connection.createCanceledConnection();
        } else {
            connection.updateState();
            return connection;
        }
    }

    @Override
    public void onConference(Connection connection1, Connection connection2) {
        if (connection1 instanceof TelephonyConnection &&
                connection2 instanceof TelephonyConnection) {
            ((TelephonyConnection) connection1).performConference(
                (TelephonyConnection) connection2);
        }

    }

    private void placeOutgoingConnection(
            TelephonyConnection connection, Phone phone, ConnectionRequest request) {
        String number = connection.getAddress().getSchemeSpecificPart();
        boolean isEmergencyNumber = PhoneNumberUtils.isLocalEmergencyNumber(this, number);
        /// M: CC036: [ALPS01794357] Set PhoneAccountHandle for ECC @{
        if (isEmergencyNumber) {
            final PhoneAccountHandle phoneAccountHandle;
            String phoneIccId = phone.getIccSerialNumber();
            int slotId = SubscriptionController.getInstance().getSlotId(phone.getSubId());
            if (slotId != SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
                phoneIccId = !TextUtils.isEmpty(phoneIccId) ?
                        phoneIccId : TelephonyManagerEx.getDefault().getSimSerialNumber(slotId);
            }
            if (TextUtils.isEmpty(phoneIccId)) {
                // If No SIM is inserted, the corresponding IccId will be null,
                // take phoneId as PhoneAccountHandle::mId which is IccId originally
                phoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(
                        Integer.toString(phone.getPhoneId()));
            } else {
                phoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(phoneIccId);
            }
            Log.d(this, "placeOutgoingConnection, set back account mId: %s, iccId: %s",
                    phoneAccountHandle.getId(), phoneIccId);
            connection.setAccountHandle(phoneAccountHandle);
        }
        /// @}
        com.android.internal.telephony.Connection originalConnection;
        try {
            originalConnection =
                    phone.dial(number, null, request.getVideoState(), request.getExtras());
        } catch (CallStateException e) {
            Log.e(this, e, "placeOutgoingConnection, phone.dial exception: " + e);
            int cause = android.telephony.DisconnectCause.OUTGOING_FAILURE;
            if (e.getError() == CallStateException.ERROR_DISCONNECTED) {
                cause = android.telephony.DisconnectCause.OUT_OF_SERVICE;
            }
            /// M: Since ussd is through 3G protocol  it will cause ims call is disconnected. @{
            if (ImsPhone.USSD_DURING_IMS_INCALL.equals(e.getMessage())) {
                Context context = phone.getContext();
                Toast.makeText(context,
                    context.getString(R.string.incall_error_call_failed), Toast.LENGTH_SHORT)
                        .show();
            }
            /// @}
            /// M: ECC Retry @{
            // Assume only one ECC exists
            if (TelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                Log.d(this, "ECC Retry : clear ECC param");
                TelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
            }
            /// @}
            connection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(
                    cause, e.getMessage()));
            /// M: CC095: Destroy TelephonyConnection if framework fails to dial @{
            connection.destroy();
            /// @}
            return;
        }

        if (originalConnection == null) {
            int telephonyDisconnectCause = android.telephony.DisconnectCause.OUTGOING_FAILURE;
            // On GSM phones, null connection means that we dialed an MMI code
            if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM) {
                Log.d(this, "dialed MMI code");
                telephonyDisconnectCause = android.telephony.DisconnectCause.DIALED_MMI;
                final Intent intent = new Intent(this, MMIDialogActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                /// M: CC037: Pass phoneID via intent to MMIDialog @{
                intent.putExtra("ID", phone.getPhoneId());
                /// @}
                startActivity(intent);
            }
            Log.d(this, "placeOutgoingConnection, phone.dial returned null");
            /// M: ECC Retry @{
            // Assume only one ECC exists
            if (TelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
                Log.d(this, "ECC Retry : clear ECC param");
                TelephonyConnectionServiceUtil.getInstance().clearEccRetryParams();
            }
            /// @}
            connection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(
                    telephonyDisconnectCause, "Connection is null"));
            /// M: CC095: Destroy TelephonyConnection if framework fails to dial @{
            connection.destroy();
            /// @}
        } else {
            connection.setOriginalConnection(originalConnection);
        }
    }

    private TelephonyConnection createConnectionFor(
            Phone phone,
            com.android.internal.telephony.Connection originalConnection,
            boolean isOutgoing,
            PhoneAccountHandle phoneAccountHandle) {
        TelephonyConnection returnConnection = null;
        int phoneType = phone.getPhoneType();
        if (phoneType == TelephonyManager.PHONE_TYPE_GSM) {
            returnConnection = new GsmConnection(originalConnection);
        } else if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
            boolean allowMute = allowMute(phone);
            returnConnection = new CdmaConnection(
                    originalConnection, mEmergencyTonePlayer, allowMute, isOutgoing);
        }
        if (returnConnection != null) {
            // Listen to Telephony specific callbacks from the connection
            returnConnection.addTelephonyConnectionListener(mTelephonyConnectionListener);
            returnConnection.setVideoPauseSupported(
                    TelecomAccountRegistry.getInstance(this).isVideoPauseSupported(
                            phoneAccountHandle));
        }
        return returnConnection;
    }

    private boolean isOriginalConnectionKnown(
            com.android.internal.telephony.Connection originalConnection) {
        for (Connection connection : getAllConnections()) {
            if (connection instanceof TelephonyConnection) {
                TelephonyConnection telephonyConnection = (TelephonyConnection) connection;
                if (telephonyConnection.getOriginalConnection() == originalConnection) {
                    return true;
                }
            }
        }
        return false;
    }

    private Phone getPhoneForAccount(PhoneAccountHandle accountHandle, boolean isEmergency) {
        /// M: CC092 pick best phone for ECC @{
        /*
        if (isEmergency) {
            return PhoneFactory.getDefaultPhone();
        }
        */
        /// @}

        int subId = PhoneUtils.getSubIdForPhoneAccountHandle(accountHandle);
        if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            int phoneId = SubscriptionController.getInstance().getPhoneId(subId);
            return PhoneFactory.getPhone(phoneId);
        }

        /// M: CC092 pick best phone for ECC @{
        if (isEmergency) {
            // If this is an emergency number and we've been asked to dial it using a PhoneAccount
            // which does not exist, then default to whatever subscription is available currently.
            return pickBestPhoneForEmergencyCall();
        }
        /// @}

        return null;
    }

    /// M: CC092 pick best phone for ECC @{
    /**
     * Pick the best phone for ECC.
     *
     * @return  1. in service phone.
     *          2. if not match, radio on and SIM card inserted phone.
     *          3. if not match, radio on phone.
     *          4. else, 3G-capable phone  rather than default phone
     */
    private Phone pickBestPhoneForEmergencyCall() {
        Phone selectPhone = null;
        for (int i = 0; i < TelephonyManager.getDefault().getSimCount(); i++) {
            int[] subIds = SubscriptionController.getInstance().getSubIdUsingSlotId(i);
            if (subIds == null || subIds.length == 0)
                continue;

            int phoneId = SubscriptionController.getInstance().getPhoneId(subIds[0]);
            Phone phone = PhoneFactory.getPhone(phoneId);
            if (phone == null)
                continue;

            if (ServiceState.STATE_IN_SERVICE == phone.getServiceState().getState()) {
                // the slot is radio on & state is in service
                Log.d(this, "pickBestPhoneForEmergencyCall, radio on & in service, slotId:" + i);
                return phone;
            } else if (ServiceState.STATE_POWER_OFF != phone.getServiceState().getState()) {
                // the slot is radio on & with SIM card inserted.
                if (TelephonyManager.getDefault().hasIccCard(i)) {
                    Log.d(this, "pickBestPhoneForEmergencyCall," +
                            "radio on and SIM card inserted, slotId:" + i);
                    selectPhone = phone;
                } else if (selectPhone == null) {
                    Log.d(this, "pickBestPhoneForEmergencyCall, radio on, slotId:" + i);
                    selectPhone = phone;
                }
            }
        }

        if (selectPhone == null) {
            /// M: CC081 Choose 3G-capable phone for ECC @{
            // Default phone is the voice/data default sub on Setting, or first active sub.
            /*
            Log.d(this, "pickBestPhoneForEmergencyCall, return default phone");
            selectPhone = PhoneFactory.getDefaultPhone();
            */
            int phoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
            Log.d(this, "pickBestPhoneForEmergencyCall, return 3G-capable phoneId:" + phoneId);
            selectPhone = PhoneFactory.getPhone(phoneId);
            /// @}
        }

        return selectPhone;
    }
    /// @}

    /**
     * Determines if the connection should allow mute.
     *
     * @param phone The current phone.
     * @return {@code True} if the connection should allow mute.
     */
    private boolean allowMute(Phone phone) {
        // For CDMA phones, check if we are in Emergency Callback Mode (ECM).  Mute is disallowed
        // in ECM mode.
        if (phone.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
            PhoneProxy phoneProxy = (PhoneProxy)phone;
            CDMAPhone cdmaPhone = (CDMAPhone)phoneProxy.getActivePhone();
            if (cdmaPhone != null) {
                if (cdmaPhone.isInEcm()) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void removeConnection(Connection connection) {
        /// M: ECC Retry @{
        if (TelephonyConnectionServiceUtil.getInstance().isEccRetryOn()) {
            Log.d(this, "ECC Retry: remove connection.");
            TelephonyConnectionServiceUtil.getInstance().setEccRetryCallId(
                    super.removeConnectionInternal(connection));
        } else { //Original flow
            super.removeConnection(connection);
        }
        /// @}
        if (connection instanceof TelephonyConnection) {
            TelephonyConnection telephonyConnection = (TelephonyConnection) connection;
            telephonyConnection.removeTelephonyConnectionListener(mTelephonyConnectionListener);
        }
    }

    /**
     * When a {@link TelephonyConnection} has its underlying original connection configured,
     * we need to add it to the correct conference controller.
     *
     * @param connection The connection to be added to the controller
     */
    public void addConnectionToConferenceController(TelephonyConnection connection) {
        // TODO: Do we need to handle the case of the original connection changing
        // and triggering this callback multiple times for the same connection?
        // If that is the case, we might want to remove this connection from all
        // conference controllers first before re-adding it.
        if (connection.isImsConnection()) {
            Log.d(this, "Adding IMS connection to conference controller: " + connection);
            mImsConferenceController.add(connection);
        } else {
            int phoneType = connection.getCall().getPhone().getPhoneType();
            if (phoneType == TelephonyManager.PHONE_TYPE_GSM
                    /// M: SS project ECC change feature @{
                    && connection instanceof GsmConnection) {
                    /// @}
                Log.d(this, "Adding GSM connection to conference controller: " + connection);
                mTelephonyConferenceController.add(connection);
            } else if (phoneType == TelephonyManager.PHONE_TYPE_CDMA &&
                    connection instanceof CdmaConnection) {
                Log.d(this, "Adding CDMA connection to conference controller: " + connection);
                mCdmaConferenceController.add((CdmaConnection) connection);
            }
            Log.d(this, "Removing connection from IMS conference controller: " + connection);
            mImsConferenceController.remove(connection);
        }
    }

    /// M: CC027: Proprietary scheme to build Connection Capabilities @{
    protected TelephonyConnection getFgConnection() {

        for (Connection c : getAllConnections()) {

            if (!(c instanceof TelephonyConnection)) {
                // the connection may be ConferenceParticipantConnection.
                continue;
            }

            TelephonyConnection tc = (TelephonyConnection) c;

            if (tc.getCall() == null) {
                continue;
            }

            Call.State s = tc.getCall().getState();

            // it assume that only one Fg call at the same time
            if (s == Call.State.ACTIVE || s == Call.State.DIALING || s == Call.State.ALERTING) {
                return tc;
            }
        }
        return null;
    }

    protected List<TelephonyConnection> getBgConnection() {

        ArrayList<TelephonyConnection> connectionList = new ArrayList<TelephonyConnection>();

        for (Connection c : getAllConnections()) {

            if (!(c instanceof TelephonyConnection)) {
                // the connection may be ConferenceParticipantConnection.
                continue;
            }

            TelephonyConnection tc = (TelephonyConnection) c;

            if (tc.getCall() == null) {
                continue;
            }

            Call.State s = tc.getCall().getState();

            // it assume the ringing call won't have more than one connection
            if (s == Call.State.HOLDING) {
                connectionList.add(tc);
            }
        }
        return connectionList;
    }

    protected List<TelephonyConnection> getRingingConnection() {

        ArrayList<TelephonyConnection> connectionList = new ArrayList<TelephonyConnection>();

        for (Connection c : getAllConnections()) {

            if (!(c instanceof TelephonyConnection)) {
                // the connection may be ConferenceParticipantConnection.
                continue;
            }

            TelephonyConnection tc = (TelephonyConnection) c;

            if (tc.getCall() == null) {
                continue;
            }

            // it assume the ringing call won't have more than one connection
            if (tc.getCall().getState().isRinging()) {
                connectionList.add(tc);
            }
        }
        return connectionList;
    }

    protected int getFgCallCount() {
        if (getFgConnection() != null) {
            return 1;
        }
        return 0;
    }

    protected int getBgCallCount() {
        return getBgConnection().size();
    }

    protected int getRingingCallCount() {
        return getRingingConnection().size();
    }

    @Override
    public boolean canDial(PhoneAccountHandle accountHandle, String dialString) {

        boolean hasRingingCall = (getRingingCallCount() > 0);
        boolean hasActiveCall = (getFgCallCount() > 0);
        boolean bIsInCallMmiCommands = isInCallMmiCommands(dialString);
        Call.State fgCallState = Call.State.IDLE;

        Phone pphone = getPhoneForAccount(accountHandle, false);
        Phone phone = (pphone == null) ? null : ((PhoneProxy) pphone).getActivePhone();

        /* bIsInCallMmiCommands == true only when dialphone == activephone */
        if (bIsInCallMmiCommands && hasActiveCall) {
            /// M: ALPS02123516. IMS incall MMI checking. @{
            /// M: ALPS02344383. null pointer check. @{
            if (phone != null && phone != getFgConnection().getPhone()
                    && phone.getImsPhone() != null
                    && phone.getImsPhone() != getFgConnection().getPhone()) {
                bIsInCallMmiCommands = false;
                Log.d(this, "phone is different, set bIsInCallMmiCommands to false");
            }
            /// @}
        }

        TelephonyConnection fConnection = getFgConnection();
        if (fConnection != null) {
            Call fCall = fConnection.getCall();
            if (fCall != null) {
                fgCallState = fCall.getState();
            }
        }

        /* Block dial if one of the following cases happens
        * 1. ECC exists in either phone
        * 2. has ringing call and the current dialString is not inCallMMI
        * 3. foreground connections in TelephonyConnectionService (both phones) are DISCONNECTING
        *
        * Different from AOSP canDial() in CallTracker which only checks state of current phone
        */
        boolean isECCExists = TelephonyConnectionServiceUtil.getInstance().isECCExists();
        boolean result = (!isECCExists
                && !(hasRingingCall && !bIsInCallMmiCommands)
                && (fgCallState != Call.State.DISCONNECTING));

        if (result == false) {
            Log.d(this, "canDial"
                    + " hasRingingCall=" + hasRingingCall
                    + " hasActiveCall=" + hasActiveCall
                    + " fgCallState=" + fgCallState
                    + " getFgConnection=" + fConnection
                    + " getRingingConnection=" + getRingingConnection()
                    + " bECCExists=" + isECCExists);
        }
        return result;
    }

    private boolean isInCallMmiCommands(String dialString) {
        boolean result = false;
        char ch = dialString.charAt(0);

        switch (ch) {
            case '0':
            case '3':
            case '4':
            case '5':
                if (dialString.length() == 1) {
                    result = true;
                }
                break;

            case '1':
            case '2':
                if (dialString.length() == 1 || dialString.length() == 2) {
                    result = true;
                }
                break;

            default:
                break;
        }

        return result;
    }
    /// @}

    /// M: CC041: Interface for ECT @{
    @Override
    public boolean canTransfer(Connection bgConnection) {

        if (bgConnection == null) {
            Log.d(this, "canTransfer: connection is null");
            return false;
        }

        if (!(bgConnection instanceof TelephonyConnection)) {
            // the connection may be ConferenceParticipantConnection.
            Log.d(this, "canTransfer: the connection isn't telephonyConnection");
            return false;
        /// M: ALPS02014255 We still don't support transfer on VoLTE @{
        } else if (((TelephonyConnection) bgConnection).isImsConnection()) {
            Log.d(this, "canTransfer: the connection is an IMS connection");
            return false;
        /// @}
        }

        TelephonyConnection bConnection = (TelephonyConnection) bgConnection;

        Phone activePhone = null;
        Phone heldPhone = null;

        TelephonyConnection fConnection = getFgConnection();
        if (fConnection != null) {
            activePhone = fConnection.getPhone();
        }

        if (bgConnection != null) {
            heldPhone = bConnection.getPhone();
        }

        return (heldPhone == activePhone && activePhone.canTransfer());
    }
    /// @}

    /// M: CC030: CRSS notification @{
    @Override
    protected void forceSuppMessageUpdate(Connection conn) {
        TelephonyConnectionServiceUtil.getInstance().forceSuppMessageUpdate(
                (TelephonyConnection) conn);
    }
    /// @}

    /// M: For VoLTE enhanced conference call. @{
    /**
     * This can be used by telecom to either create a new outgoing conference call or
     * attach to an existing incoming conference call.
     */
    @Override
    protected Conference onCreateConference(
            final PhoneAccountHandle callManagerAccount,
            final String conferenceCallId,
            final ConnectionRequest request,
            final List<String> numbers,
            boolean isIncoming) {

        if (!canDial(request.getAccountHandle(), numbers.get(0))) {
            Log.d(this, "onCreateConference(), canDial check fail");
            /// M: ALPS02331568.  Should reture the failed conference. @{
            return TelephonyConnectionServiceUtil.getInstance().createFailedConference(
                android.telephony.DisconnectCause.OUTGOING_FAILURE,
                "canDial() check fail");
            /// @}
        }

        Phone phone = getPhoneForAccount(request.getAccountHandle(), false);

        /// M: ALPS02209724. Toast if there are more than 5 numbers.
        /// M: ALPS02331568. Take away null-check for numbers. @{
        if (!isIncoming
                && numbers.size() > ImsConference.IMS_CONFERENCE_MAX_SIZE) {
            Log.d(this, "onCreateConference(), more than 5 numbers");
            if (phone != null) {
                ImsConference.toastWhenConferenceIsFull(phone.getContext());
            }
            return TelephonyConnectionServiceUtil.getInstance().createFailedConference(
                    android.telephony.DisconnectCause.OUTGOING_FAILURE,
                    "more than 5 numbers");
        }
        /// @}

        return TelephonyConnectionServiceUtil.getInstance().createConference(
            mImsConferenceController,
            phone,
            request,
            numbers,
            isIncoming);
    }
    /// @}

    /// M: For VoLTE conference SRVCC. @{
    /**
     * perform Ims Conference SRVCC.
     * @param imsConf the ims conference.
     * @param radioConnections the new created radioConnection
     * @hide
     */
    void performImsConferenceSRVCC(
            Conference imsConf,
            ArrayList<com.android.internal.telephony.Connection> radioConnections) {
        if (imsConf == null) {
            Log.e(this, new CallStateException(),
                "performImsConferenceSRVCC(): abnormal case, imsConf is null");
            return;
        }

        if (radioConnections == null || radioConnections.size() < 2) {
            Log.e(this, new CallStateException(),
                "performImsConferenceSRVCC(): abnormal case, newConnections is null");
            return;
        }

        if (radioConnections.get(0) == null || radioConnections.get(0).getCall() == null ||
                radioConnections.get(0).getCall().getPhone() == null) {
            Log.e(this, new CallStateException(),
                "performImsConferenceSRVCC(): abnormal case, can't get phone instance");
            return;
        }

        /// M: CC088: new TelephonyConference with phoneAccountHandle @{
        Phone phone = radioConnections.get(0).getCall().getPhone();
        PhoneAccountHandle handle = PhoneUtils.makePstnPhoneAccountHandle(phone);
        TelephonyConference newConf = new TelephonyConference(handle);
        /// @}

        replaceConference(imsConf, (Conference) newConf);
        mTelephonyConferenceController.setHandoveredConference(newConf);

        // we need to follow the order below:
        // 1. new empty GsmConnection
        // 2. addExistingConnection (and it will be added to TelephonyConferenceController)
        // 3. config originalConnection.
        // Then UI will not flash the participant calls during SRVCC.
        ArrayList<GsmConnection> newGsmConnections = new ArrayList<GsmConnection>();
        for (com.android.internal.telephony.Connection radioConn : radioConnections) {
            GsmConnection connection = new GsmConnection(null);
            /// M: ALPS02136977. Sets address first for formatted dump log.
            connection.setAddress(
                    Uri.fromParts(PhoneAccount.SCHEME_TEL, radioConn.getAddress(), null),
                    PhoneConstants.PRESENTATION_ALLOWED);
            newGsmConnections.add(connection);

            addExistingConnection(handle, connection);
            connection.addTelephonyConnectionListener(mTelephonyConnectionListener);
        }

        for (int i = 0; i < newGsmConnections.size(); i++) {
            newGsmConnections.get(i).setOriginalConnection(radioConnections.get(i));
        }
    }
    /// @}

    /// M: SS project ECC change feature @{
    private TelephonyConnection createConnectionForSwitchPhone(
            int phoneType,
            com.android.internal.telephony.Connection originalConnection,
            boolean isOutgoing,
            PhoneAccountHandle phoneAccountHandle) {
        TelephonyConnection returnConnection = null;
        Log.d(this, "createConnectionForSwitchPhone, phoneType:" + phoneType);
        if (phoneType == TelephonyManager.PHONE_TYPE_GSM) {
            returnConnection = new GsmConnection(originalConnection);
        } else if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
            /*String inEcm = SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE, "false");
            boolean allowMute = inEcm.equals("false");*/
            returnConnection = new CdmaConnection(
                    originalConnection, mEmergencyTonePlayer, true, isOutgoing);
        }
        if (returnConnection != null) {
            // Listen to Telephony specific callbacks from the connection
            returnConnection.addTelephonyConnectionListener(mTelephonyConnectionListener);
            returnConnection.setVideoPauseSupported(
                    TelecomAccountRegistry.getInstance(this).isVideoPauseSupported(
                            phoneAccountHandle));
        }
        return returnConnection;
    }
    /// @}
}

