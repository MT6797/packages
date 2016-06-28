/*
* Copyright (C) 2011-2014 MediaTek Inc.
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

/// M: for VoLTE enhanced conference call. @{
import android.telecom.Conference;
import android.telecom.ConnectionRequest;
/// @}

import android.telephony.PhoneNumberUtils;
/// M: for VoLTE enhanced conference call. @{
import android.telephony.ServiceState;
/// @}
import android.telephony.TelephonyManager;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SubscriptionController;


import com.android.internal.telephony.gsm.GsmMmiCode;
import com.android.internal.telephony.cdma.CdmaMmiCode;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.cdma.CDMAPhone;
import com.android.internal.telephony.gsm.GSMPhone;

import android.os.SystemProperties;

import java.util.List;
import java.util.ArrayList;

// for cell conn manager
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.telephony.SubscriptionManager;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.TelephonyIntents;
import com.android.phone.PhoneGlobals;
import com.mediatek.internal.telephony.CellConnMgr;
import com.mediatek.phone.SimErrorDialog;

import com.mediatek.telephony.TelephonyManagerEx;

/* M: Get the caller info, add for OP01 Plug in. @{ */
import android.net.Uri;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;

import com.mediatek.phone.ext.ExtensionManager;
/** @} */

/// M: CC077: 2/3G CAPABILITY_HIGH_DEF_AUDIO @{
import com.mediatek.services.telephony.SpeechCodecType;
/// @}

/// M: CC030: CRSS notification @{
import com.mediatek.services.telephony.SuppMessageManager;
/// @}

//for [VoLTE_SS] notify user for volte mmi request while data off
import com.mediatek.settings.TelephonyUtils;

/// M: ECC Retry @{
import com.mediatek.services.telephony.EmergencyRetryHandler;
/// @}

/// M: ECC special handle @{
import android.telecom.PhoneAccountHandle;

import com.mediatek.services.telephony.EmergencyRuleHandler;
/// @}

/**
 * Service for making GSM and CDMA connections.
 */
public class TelephonyConnectionServiceUtil {

    private static final TelephonyConnectionServiceUtil INSTANCE = new TelephonyConnectionServiceUtil();
    private TelephonyConnectionService mService;

    // for cell conn manager
    private int mCurrentDialSubId;
    private int mCurrentDialSlotId;
    private CellConnMgr mCellConnMgr;
    private int mCellConnMgrCurrentRun;
    private int mCellConnMgrTargetRun;
    private int mCellConnMgrState;
    private ArrayList<String> mCellConnMgrStringArray;
    private Context mContext;
    private SimErrorDialog mSimErrorDialog;
    /// M: CC030: CRSS notification @{
    private SuppMessageManager mSuppMessageManager;
    /// @}
    /// M: ECC Retry @{
    private EmergencyRetryHandler mEccRetryHandler;
    /// @}

    /// M: CC077: 2/3G CAPABILITY_HIGH_DEF_AUDIO @{
    private static final String PROPERTY_SPEECH_CODEC_INFO = "af.ril.speech.codec.info";
    private static final String PROPERTY_HD_VOICE_STATUS = "af.ril.hd.voice.status";
    private SpeechCodecType mSpeechType;
    /// @}

    /// M: SS project ECC change feature @{
    private static final String[] GSM_PREFERRED_NUMBERS = {
        "112", "911", "999", "000", "08", "118"
    };
    /// @}

    // for cell conn manager
    private final BroadcastReceiver mCellConnMgrReceiver = new TcsBroadcastReceiver();
    /// M: CC: PPL (Phone Privacy Lock Service)
    private final BroadcastReceiver mPplReceiver = new TcsBroadcastReceiver();

    TelephonyConnectionServiceUtil() {
        mService = null;
        mContext = null;
        mSimErrorDialog = null;
        /// M: CC030: CRSS notification @{
        mSuppMessageManager = null;
        /// @}
        /// M: ECC Retry @{
        mEccRetryHandler = null;
        /// @}
        /// M: CC077: 2/3G CAPABILITY_HIGH_DEF_AUDIO @{
        mSpeechType = SpeechCodecType.fromInt(0);
        Log.d(this, "isHighDefAudio = " + mSpeechType.isHighDefAudio());
        /// @}
    }

    public static TelephonyConnectionServiceUtil getInstance() {
        return INSTANCE;
    }

    public void setService(TelephonyConnectionService s) {
        Log.d(this, "setService: " + s);
        mService = s;
        mContext = mService.getApplicationContext();
        enableSuppMessage(s);
        /// M: ECC Retry @{
        mEccRetryHandler = null;
        /// @}
        /// M: CC: PPL @{
        IntentFilter intentFilter = new IntentFilter("com.mediatek.ppl.NOTIFY_LOCK");
        mContext.registerReceiver(mPplReceiver, intentFilter);
        /// @}
    }

    /**
     * unset TelephonyConnectionService to be bind.
     */
    public void unsetService() {
        Log.d(this, "unSetService: " + mService);
        mService = null;
        disableSuppMessage();
        /// M: ECC Retry @{
        mEccRetryHandler = null;
        /// @}
        /// M: CC077: 2/3G CAPABILITY_HIGH_DEF_AUDIO @{
        mSpeechType = SpeechCodecType.fromInt(0);
        Log.d(this, "isHighDefAudio = " + mSpeechType.isHighDefAudio());
        /// @}
        /// M: CC: PPL
        mContext.unregisterReceiver(mPplReceiver);

    }

    /// M: ECC Retry @{
    /**
     * Check if ECC Retry is running.
     * @return {@code true} if ECC Retry is running and {@code false} otherwise.
     */
    public boolean isEccRetryOn() {
        boolean bIsOn = (mEccRetryHandler != null);
        Log.d(this, "ECC Retry : isEccRetryOn = " + bIsOn);
        return bIsOn;
    }

    /**
     * Save ECC Retry requested paramsRegister once ECC is created.
     * @param request ConnectionRequest
     * @param initPhoneId PhoneId of the initial ECC
     */
    public void setEccRetryParams(ConnectionRequest request, int initPhoneId) {
        //Check UE is set to test mode or not   (CTA =1,FTA =2 , IOT=3 ...)
        // Skip ECC Retry for TC26.9.6.2.2
        if (SystemProperties.getInt("gsm.gcf.testmode", 0) == 2) {
            Log.d(this, "setEccRetryParams, skip for FTA mode");
            return;
        }

        Log.d(this, "setEccRetryParams, request: " + request + " initPhoneId: " + initPhoneId);
        if (mEccRetryHandler == null) {
            mEccRetryHandler = new EmergencyRetryHandler(request, initPhoneId);
        }
    }

    public void clearEccRetryParams() {
        Log.d(this, "clearEccRetryParams");
        mEccRetryHandler = null;
    }

    /**
     * Set original ECC Call Id
     * @param id CallId
     */
    public void setEccRetryCallId(String id) {
        Log.d(this, "ECC Retry : setEccRetryCallId = " + id);
        if (mEccRetryHandler != null) {
            mEccRetryHandler.setCallId(id);
        }
    }

    /**
     * If ECC Retry timeout
     * @return {@code true} if ECC Retry timeout {@code false} otherwise.
     */
    public boolean eccRetryTimeout() {
        boolean bIsTimeout = false;
        if (mEccRetryHandler != null) {
            if (mEccRetryHandler.isTimeout()) {
                mEccRetryHandler = null;
                bIsTimeout = true;
            }
        }
        Log.d(this, "ECC Retry : eccRetryTimeout = " + bIsTimeout);
        return bIsTimeout;
    }

    /**
     * Perform ECC Retry
     */
    public void performEccRetry() {
        Log.d(this, "performEccRetry");
        if (mEccRetryHandler != null) {
            ConnectionRequest retryRequest = new ConnectionRequest(
                                        mEccRetryHandler.getNextAccountHandle(),
                                        mEccRetryHandler.getRequest().getAddress(),
                                        mEccRetryHandler.getRequest().getExtras(),
                                        mEccRetryHandler.getRequest().getVideoState());
            mService.createConnectionInternal(mEccRetryHandler.getCallId(), retryRequest);
        }
    }
    /// @}

    /// M: CC030: CRSS notification @{
    /**
     * Register for Supplementary Messages once TelephonyConnection is created.
     * @param cs TelephonyConnectionService
     * @param conn TelephonyConnection
     */
    private void enableSuppMessage(TelephonyConnectionService cs) {
        Log.d(this, "enableSuppMessage for " + cs);
        if (mSuppMessageManager == null) {
            mSuppMessageManager = new SuppMessageManager(cs);
            mSuppMessageManager.registerSuppMessageForPhones();
        }
    }

    /**
     * Unregister for Supplementary Messages  once TelephonyConnectionService is destroyed.
     */
    private void disableSuppMessage() {
        Log.d(this, "disableSuppMessage");
        if (mSuppMessageManager != null) {
            mSuppMessageManager.unregisterSuppMessageForPhones();
            mSuppMessageManager = null;
        }
    }

    /**
     * Force Supplementary Message update once TelephonyConnection is created.
     * @param conn The connection to update supplementary messages.
     */
    public void forceSuppMessageUpdate(TelephonyConnection conn) {
        if (mSuppMessageManager != null) {
            Phone p = conn.getPhone();
            if (p != null) {
                Log.d(this, "forceSuppMessageUpdate for " + conn + ", " + p
                        + " phone " + p.getPhoneId());
                mSuppMessageManager.forceSuppMessageUpdate(conn, p);
            }
        }
    }

    /// @}

    public boolean isDualtalk() {
        /// M: SVLTE project special handling. @{
        // For SVLTE project, CS is dual but PS is not, therefore
        // mtk_dt_support is not enabled. However, for this kind of C+G configuration,
        // CS domain behaves like DSDA.
        if (SystemProperties.get("ro.mtk_svlte_support").equals("1")) {
            return TelephonyManager.getDefault().getPhoneCount() >= 2;
        } else {
        /// @}
            return (TelephonyManager.getDefault().getMultiSimConfiguration()
                    == TelephonyManager.MultiSimVariants.DSDA);
        }
    }

    public boolean isECCExists() {

        if (mService == null) {
            // it means that never a call exist
            // so still not register in telephonyConnectionService
            // ECC doesn't exist
            return false;
        }

        if (mService.getFgConnection() == null) {
            return false;
        }
        if (mService.getFgConnection().getCall() == null ||
            mService.getFgConnection().getCall().getEarliestConnection() == null) {
            return false;
        }

        String activeCallAddress = mService.getFgConnection().getCall().
                getEarliestConnection().getAddress();

        boolean bECCExists;

        bECCExists = (PhoneNumberUtils.isEmergencyNumber(activeCallAddress)
                     && !PhoneNumberUtils.isSpecialEmergencyNumber(activeCallAddress));

        if (bECCExists) {
            Log.d(this, "ECC call exists.");
        }
        else {
            Log.d(this, "ECC call doesn't exists.");
        }

        return bECCExists;
    }

    public boolean isUssdNumber(Phone phone, String dialString) {
        boolean bIsUssdNumber = false;
        PhoneProxy phoneProxy = (PhoneProxy) phone;
        int slot = SubscriptionController.getInstance().getSlotId(phone.getSubId());

        if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM) {
            UiccCardApplication cardApp = UiccController.getInstance().
                    getUiccCardApplication(slot, UiccController.APP_FAM_3GPP);
            Log.d(this, "isUssdNumber [UiccCardApplication]cardApp " + cardApp);

            GSMPhone gsmPhone = (GSMPhone) phoneProxy.getActivePhone();
            bIsUssdNumber = GsmMmiCode.isUssdNumber(dialString, gsmPhone, cardApp);
        } else if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            UiccCardApplication cardApp = UiccController.getInstance().
                    getUiccCardApplication(slot, UiccController.APP_FAM_3GPP2);
            Log.d(this, "isUssdNumber [UiccCardApplication]cardApp " + cardApp);

            CDMAPhone cdmaPhone = (CDMAPhone) phoneProxy.getActivePhone();
            CdmaMmiCode cdmaMmiCode = CdmaMmiCode.newFromDialString(dialString, cdmaPhone, cardApp);
            if (cdmaMmiCode != null) {
                bIsUssdNumber = cdmaMmiCode.isUssdRequest();
            }
        }

        Log.d(this, "isUssdNumber = " + bIsUssdNumber);
        return bIsUssdNumber;
    }

    /// M: CC032: Proprietary incoming call handling @{
    public void setIncomingCallIndicationResponse(GSMPhone phone) {

        if (mService == null) {
            // it means that never a call exist
            // so still not register in telephonyConnectionService
            // Accept the MT
            phone.setIncomingCallIndicationResponse(true);
            return;
        }

        boolean isRejectNewRingCall = false;
        boolean isECCExists = isECCExists();

        if (!isDualtalk()) {
            if (mService.getRingingCallCount() > 0) {
                isRejectNewRingCall = true;
            }
        } else {
            if (mService.getRingingCallCount() > 1) {
                isRejectNewRingCall = true;
            }
        }

        // only gsmphone sends this event
        /// M: ALPS01971565  @{
        // For ECC case, need to generate a miss call notification.
        if (/*isECCExists || */isRejectNewRingCall) {
        /// @}
            phone.setIncomingCallIndicationResponse(false);
        } else {
            phone.setIncomingCallIndicationResponse(true);
        }
    }
    /// @}

    /// M: CC021: Error message due to CellConnMgr checking @{
    /**
     * register broadcast Receiver.
     */
    private void cellConnMgrRegisterForSubEvent() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        mContext.registerReceiver(mCellConnMgrReceiver, intentFilter);
    }

    /**
     * unregister broadcast Receiver.
     */
    private void cellConnMgrUnregisterForSubEvent() {
        mContext.unregisterReceiver(mCellConnMgrReceiver);
    }

   /**
     For SIM unplugged, PhoneAccountHandle is null, hence TelephonyConnectionService returns OUTGOING_FAILURE,
     without CellConnMgr checking, UI will show "Call not Sent" Google default dialog.
     For SIM plugged, under
     (1) Flight mode on, MTK SimErrorDialog will show FLIGHT MODE string returned by CellConnMgr.
          Only turning off flight mode via notification bar can dismiss the dialog.
     (2) SIM off, MTK SimErrorDialog will show SIM OFF string returned by CellConnMgr.
          Turning on flight mode, or unplugging SIM can dismiss the dialog.
     (3) SIM locked, MTK SimErrorDialog will show SIM LOCKED string returned by CellConnMgr.
          Turning on flight mode, or unplugging SIM can dismiss the dialog.
     */

    /**
     * Listen to intent of Airplane mode and Sim mode.
     * In case of Airplane mode off or Sim Hot Swap, dismiss SimErrorDialog
     */
    private class TcsBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isInitialStickyBroadcast()) {
                Log.d(this, "Skip initial sticky broadcast");
                return;
            }
            String action = intent.getAction();
            switch (action) {
                /// M: CC: PPL @{
                case "com.mediatek.ppl.NOTIFY_LOCK":
                    Log.d(this, "Receives com.mediatek.ppl.NOTIFY_LOCK");
                    for (android.telecom.Connection conn : mService.getAllConnections()) {
                        if (conn instanceof CdmaConnection) {
                            conn.onDisconnect();
                        } else if (conn instanceof TelephonyConnection) {
                            conn.onHangupAll();
                            break;
                        }
                    }
                    break;
                /// @}
                case Intent.ACTION_AIRPLANE_MODE_CHANGED:
                    Log.d(this, "SimErrorDialog finish due to ACTION_AIRPLANE_MODE_CHANGED");
                    mSimErrorDialog.dismiss();
                    break;
                case TelephonyIntents.ACTION_SIM_STATE_CHANGED:
                    String simState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                    int slotId = intent.getIntExtra(PhoneConstants.SLOT_KEY,
                            SubscriptionManager.INVALID_SIM_SLOT_INDEX);
                    Log.d(this, "slotId: " + slotId + " simState: " + simState);
                    if ((slotId != SubscriptionManager.INVALID_SIM_SLOT_INDEX) &&
                            (slotId == mCurrentDialSlotId) &&
                            (simState.equals(IccCardConstants.INTENT_VALUE_ICC_ABSENT))) {
                        Log.d(this, "SimErrorDialog finish due hot plug out of SIM " +
                                (slotId + 1));
                        mSimErrorDialog.dismiss();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    public void cellConnMgrSetSimErrorDialogActivity(SimErrorDialog dialog) {
        if (mContext == null) {
            Log.d(this, "cellConnMgrSetSimErrorDialogActivity, mContext is null");
            return;
        }

        if (mSimErrorDialog == dialog) {
            Log.d(this, "cellConnMgrSetSimErrorDialogActivity, skip duplicate");
            return;
        }

        mSimErrorDialog = dialog;
        if (mSimErrorDialog != null) {
            cellConnMgrRegisterForSubEvent();
            Log.d(this, "cellConnMgrRegisterForSubEvent for setSimErrorDialogActivity");
        } else {
            cellConnMgrUnregisterForSubEvent();
            Log.d(this, "cellConnMgrUnregisterForSubEvent for setSimErrorDialogActivity");
        }
    }

    public boolean cellConnMgrShowAlerting(int subId) {
        if (mContext == null) {
            Log.d(this, "cellConnMgrShowAlerting, mContext is null");
            return false;
        }

        /// M: To check if WiFi Calling enabled. @{
        /// If WiFi calling is enabled, return directly.
        if (TelephonyManagerEx.getDefault().isWifiCallingEnabled(subId)) {
            Log.d(this, "WiFi calling is enabled, return directly.");
            return false;
        }
        /// @}

        mCellConnMgr = new CellConnMgr(mContext);
        mCurrentDialSubId = subId;
        mCurrentDialSlotId = SubscriptionController.getInstance().getSlotId(subId);

        //Step1. Query state by indicated request type, the return value are the combination of current states
        mCellConnMgrState = mCellConnMgr.getCurrentState(mCurrentDialSubId, CellConnMgr.STATE_FLIGHT_MODE |
            CellConnMgr.STATE_RADIO_OFF | CellConnMgr.STATE_SIM_LOCKED | CellConnMgr.STATE_ROAMING);

        // check if need to notify user to do something
        // Since UX might change, check the size of mCellConnMgrStringArray to show dialog.
        if (mCellConnMgrState != CellConnMgr.STATE_READY) {

            //Step2. Query string used to show dialog
            mCellConnMgrStringArray = mCellConnMgr.getStringUsingState(mCurrentDialSubId, mCellConnMgrState);
            mCellConnMgrCurrentRun = 0;
            mCellConnMgrTargetRun = mCellConnMgrStringArray.size() / 4;

            Log.d(this, "cellConnMgrShowAlerting, slotId: " + mCurrentDialSlotId +
                " state: " + mCellConnMgrState + " size: " + mCellConnMgrStringArray.size());

            if (mCellConnMgrTargetRun > 0) {
                cellConnMgrShowAlertingInternal();
                return true;
            }
        }
        return false;
    }

    public void cellConnMgrHandleEvent() {

        //Handle the request if user click on positive button
        mCellConnMgr.handleRequest(mCurrentDialSubId, mCellConnMgrState);

        mCellConnMgrCurrentRun++;

        if (mCellConnMgrCurrentRun != mCellConnMgrTargetRun) {
            cellConnMgrShowAlertingInternal();
        } else {
            cellConnMgrShowAlertingFinalize();
        }
    }

    private void cellConnMgrShowAlertingInternal() {

        //Show confirm dialog with returned dialog title, description, negative button and positive button

        ArrayList<String> stringArray = new ArrayList<String>();
        stringArray.add(mCellConnMgrStringArray.get(mCellConnMgrCurrentRun * 4));
        stringArray.add(mCellConnMgrStringArray.get(mCellConnMgrCurrentRun * 4 + 1));
        stringArray.add(mCellConnMgrStringArray.get(mCellConnMgrCurrentRun * 4 + 2));
        stringArray.add(mCellConnMgrStringArray.get(mCellConnMgrCurrentRun * 4 + 3));

        for (int i = 0; i < stringArray.size(); i++) {
            Log.d(this, "cellConnMgrShowAlertingInternal, string(" + i + ")=" + stringArray.get(i));
        }

        // call dialog ...
        Log.d(this, "cellConnMgrShowAlertingInternal");
//        final Intent intent = new Intent(mContext, SimErrorDialogActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
//        intent.putStringArrayListExtra(SimErrorDialogActivity.DIALOG_INFORMATION, stringArray);
//        mContext.startActivity(intent);
        if (stringArray.size() < 4) {
            Log.d(this, "cellConnMgrShowAlertingInternal, stringArray is illegle, do nothing.");
            return;
        }
        mSimErrorDialog = new SimErrorDialog(mContext, stringArray);
        mSimErrorDialog.show();
    }

    public void cellConnMgrShowAlertingFinalize() {
        Log.d(this, "cellConnMgrShowAlertingFinalize");
        mCellConnMgrCurrentRun = -1;
        mCellConnMgrTargetRun = 0;
        mCurrentDialSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        mCurrentDialSlotId = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
        mCellConnMgrState = -1;
        mCellConnMgr = null;
    }

    public boolean isCellConnMgrAlive() {
        return (mCellConnMgr != null);
    }
    /// @}

    /// M: CC022: Error message due to VoLTE SS checking @{
    //--------------[VoLTE_SS] notify user when volte mmi request while data off-------------
    /**
     * This function used to judge whether the dialed mmi needs to be blocked (which needs XCAP)
     * Disallow SS setting/query.
     * Allow CLIR temporary mode, and USSD.
     * @param phone The phone to dial
     * @param number The number to dial
     * @return {@code true} if the number has MMI format to be blocked and {@code false} otherwise.
     */
    private boolean isBlockedMmi(Phone phone, String dialString) {
        boolean bIsBlockedMmi = false;
        if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM) {

            int slot = SubscriptionController.getInstance().getSlotId(phone.getSubId());
            UiccCardApplication cardApp = UiccController.getInstance().
                    getUiccCardApplication(slot, UiccController.APP_FAM_3GPP);
            Log.d(this, "isBlockedMmi [UiccCardApplication]cardApp " + cardApp);

            PhoneProxy phoneProxy = (PhoneProxy) phone;
            GSMPhone gsmPhone = (GSMPhone) phoneProxy.getActivePhone();

            String newDialString = PhoneNumberUtils.stripSeparators(dialString);
            String networkPortion = PhoneNumberUtils.extractNetworkPortionAlt(newDialString);
            GsmMmiCode gsmMmiCode = GsmMmiCode.newFromDialString(
                    networkPortion, gsmPhone, cardApp);

            if (gsmMmiCode == null) {
                bIsBlockedMmi = false;
            } else if (gsmMmiCode.isTemporaryModeCLIR() ||
                    isUssdNumber(phone, dialString)) {
                bIsBlockedMmi = false;
            } else {
                bIsBlockedMmi = true;
            }
        }

        Log.d(this, "isBlockedMmi = " + bIsBlockedMmi);
        return bIsBlockedMmi;
    }

    /**
     * This function used to check whether we should notify user to open data connection.
     * For now, we judge certain mmi code + "IMS-phoneAccount" + data connection is off.
     * @param number The number to dial
     * @param phone The target phone
     * @return {@code true} if the notification should pop up and {@code false} otherwise.
     */
    public boolean shouldOpenDataConnection(String number,  Phone phone) {
        return (isBlockedMmi(phone, number) &&
                TelephonyUtils.shouldShowOpenMobileDataDialog(mContext, phone.getSubId()));
    }
    /// @}

    /// M: CC077: 2/3G CAPABILITY_HIGH_DEF_AUDIO @{
    /**
     * This function used to check whether the input value is of HD type.
     * @param value The speech codec type value
     * @return {@code true} if the codec type is of HD type and {@code false} otherwise.
     */
    public boolean isHighDefAudio(int value) {
        String op = null;
        String hdStat = null;
        op = SystemProperties.get("ro.operator.optr", "OM");
        Log.d(this, "ro.operator.optr= " + op);
        op = op.concat("=");
        hdStat = SystemProperties.get(PROPERTY_HD_VOICE_STATUS, "");
        if (hdStat != null && !hdStat.equals("")) {
            Log.d(this, "HD voice status: " + hdStat);
            boolean findOp = hdStat.indexOf(op) != -1;
            boolean findOm = hdStat.indexOf("OM=") != -1;
            int start = 0;
            if (findOp && !op.equals("OM=")) {
                start = hdStat.indexOf(op) + op.length(); //OPXX=Y
            } else if (findOm) {
                start = hdStat.indexOf("OM=") + 3; //OM=Y
            }
            // Ex: ril.hd.voice.status="OM=Y;OP07=N;OP12=Y;"
            String isHd = hdStat.length() > (start + 1) ? hdStat.substring(start, start + 1) : "";
            if (isHd.equals("Y")) {
                return true;
            } else {
                return false;
            }
        } else {
            if (value != 0) {
                mSpeechType = SpeechCodecType.fromInt(value);
            }
            boolean isHd = mSpeechType.isHighDefAudio();
            Log.d(this, "isHighDefAudio = " + isHd);
            return isHd;
        }
    }
    /// @}

    /// M: for VoLTE Conference. @{
    boolean isVoLTEConferenceFull(ImsConferenceController imsConfController) {
        if (imsConfController == null) {
            return false;
        }

        // we assume there is only one ImsPhone at the same time.
        // and we dont support two conference at the same time.
        ArrayList<ImsConference> curImsConferences =
            imsConfController.getCurrentConferences();
        if (curImsConferences.size() == 0
                || curImsConferences.get(0).getNumbOfParticipants() < 5) {
            return false;
        } else {
            return true;
        }
    }

    boolean canHoldImsConference(ImsConference conference) {
        if (conference == null) {
            return false;
        }

        Phone phone = conference.getPhone();
        if (phone == null) {
            return false;
        }

        int state = conference.getState();

        if ((state == android.telecom.Connection.STATE_ACTIVE)
                && (phone.getBackgroundCall().isIdle())) {
            Log.d(this, "canHold conference=" + conference);
            return true;
        } else {
            Log.d(this, "canHold"
                    + " state=" + state
                    + " BgCall is Idle = " + phone.getBackgroundCall().isIdle());
            return false;
        }
    }

    boolean canUnHoldImsConference(ImsConference conference) {
        if (conference == null) {
            return false;
        }

        Phone phone = conference.getPhone();
        if (phone == null) {
            return false;
        }

        int state = conference.getState();

        if ((state == android.telecom.Connection.STATE_HOLDING)
                && (phone.getForegroundCall().isIdle())) {
            Log.d(this, "canUnHold conference=" + conference);
            return true;
        } else {
            Log.d(this, "canUnHold"
                + " state=" + state
                + " FgCall is Idle = " + phone.getForegroundCall().isIdle());
            return false;
        }
    }
    /// @}

    /// M: For VoLTE enhanced conference call. @{
    /**
     * Create a conference connection given an incoming request. This is used to attach to existing
     * incoming calls.
     *
     * @param request Details about the incoming call.
     * @return The {@code GsmConnection} object to satisfy this call, or {@code null} to
     *         not handle the call.
     */
    private android.telecom.Connection createIncomingConferenceHostConnection(
            Phone phone, ConnectionRequest request) {
        Log.i(this, "createIncomingConferenceHostConnection, request: " + request);

        if (mService == null || phone == null) {
            return android.telecom.Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.ERROR_UNSPECIFIED));
        }

        Call call = phone.getRingingCall();
        if (!call.getState().isRinging()) {
            Log.i(this, "onCreateIncomingConferenceHostConnection, no ringing call");
            return android.telecom.Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.INCOMING_MISSED,
                            "Found no ringing call"));
        }

        com.android.internal.telephony.Connection originalConnection =
                call.getState() == Call.State.WAITING ?
                    call.getLatestConnection() : call.getEarliestConnection();

        for (android.telecom.Connection connection : mService.getAllConnections()) {
            if (connection instanceof TelephonyConnection) {
                TelephonyConnection telephonyConnection = (TelephonyConnection) connection;
                if (telephonyConnection.getOriginalConnection() == originalConnection) {
                    Log.i(this, "original connection already registered");
                    return android.telecom.Connection.createCanceledConnection();
                }
            }
        }

        GsmConnection connection = new GsmConnection(originalConnection);
        return connection;
    }

    /**
     * Create a conference connection given an outgoing request. This is used to initiate new
     * outgoing calls.
     *
     * @param request Details about the outgoing call.
     * @return The {@code GsmConnection} object to satisfy this call, or the result of an invocation
     *         of {@link Connection#createFailedConnection(DisconnectCause)} to not handle the call.
     */
    private android.telecom.Connection createOutgoingConferenceHostConnection(
            Phone phone, final ConnectionRequest request, List<String> numbers) {
        Log.i(this, "createOutgoingConferenceHostConnection, request: " + request);

        if (phone == null) {
            Log.d(this, "createOutgoingConferenceHostConnection, phone is null");
            return android.telecom.Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.OUTGOING_FAILURE, "Phone is null"));
        }

        if (TelephonyConnectionServiceUtil.getInstance().
                cellConnMgrShowAlerting(phone.getSubId())) {
            Log.d(this,
                "createOutgoingConferenceHostConnection, cellConnMgrShowAlerting() check fail");
            return android.telecom.Connection.createFailedConnection(
                    DisconnectCauseUtil.toTelecomDisconnectCause(
                            android.telephony.DisconnectCause.OUTGOING_CANCELED_BY_SERVICE,
                            "cellConnMgrShowAlerting() check fail"));
        }

        int state = phone.getServiceState().getState();
        switch (state) {
            case ServiceState.STATE_IN_SERVICE:
            case ServiceState.STATE_EMERGENCY_ONLY:
                break;
            case ServiceState.STATE_OUT_OF_SERVICE:
                return android.telecom.Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                android.telephony.DisconnectCause.OUT_OF_SERVICE,
                                "ServiceState.STATE_OUT_OF_SERVICE"));
            case ServiceState.STATE_POWER_OFF:
                return android.telecom.Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                android.telephony.DisconnectCause.POWER_OFF,
                                "ServiceState.STATE_POWER_OFF"));
            default:
                Log.d(this, "onCreateOutgoingConnection, unknown service state: %d", state);
                return android.telecom.Connection.createFailedConnection(
                        DisconnectCauseUtil.toTelecomDisconnectCause(
                                android.telephony.DisconnectCause.OUTGOING_FAILURE,
                                "Unknown service state " + state));
        }

        // Don't call createConnectionFor() because we can't add this connection to
        // GsmConferenceController
        GsmConnection connection = new GsmConnection(null);
        connection.setInitializing();
        connection.setVideoState(request.getVideoState());

        placeOutgoingConferenceHostConnection(connection, phone, request, numbers);

        return connection;
    }

    private void placeOutgoingConferenceHostConnection(
            TelephonyConnection connection, Phone phone, ConnectionRequest request,
            List<String> numbers) {

        com.android.internal.telephony.Connection originalConnection;
        try {
            originalConnection = phone.dial(numbers, request.getVideoState());
        } catch (CallStateException e) {
            Log.e(this, e, "placeOutgoingConfHostConnection, phone.dial exception: " + e);
            connection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(
                    android.telephony.DisconnectCause.OUTGOING_FAILURE,
                    e.getMessage()));
            return;
        }

        if (originalConnection == null) {
            int telephonyDisconnectCause = android.telephony.DisconnectCause.OUTGOING_FAILURE;
            Log.d(this, "placeOutgoingConnection, phone.dial returned null");
            connection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(
                    telephonyDisconnectCause, "Connection is null"));
        } else {
            connection.setOriginalConnection(originalConnection);
        }
    }


    /**
     * This can be used by telecom to either create a new outgoing conference call or attach
     * to an existing incoming conference call.
     */
    Conference createConference(
            ImsConferenceController imsConfController,
            Phone phone,
            final ConnectionRequest request,
            final List<String> numbers,
            boolean isIncoming) {
        if (imsConfController == null) {
            return null;
        }

        android.telecom.Connection connection = isIncoming ?
            createIncomingConferenceHostConnection(phone, request)
                : createOutgoingConferenceHostConnection(phone, request, numbers);
        Log.d(this, "onCreateConference, connection: %s", connection);

        if (connection == null) {
            Log.d(this, "onCreateConference, connection: %s");
            return null;
        } else if (connection.getState() ==
                android.telecom.Connection.STATE_DISCONNECTED) {
            Log.d(this, "the host connection is dicsonnected");
            return createFailedConference(connection.getDisconnectCause());
        } else if (!(connection instanceof GsmConnection)) {
            Log.d(this, "abnormal case, the host connection isn't GsmConnection");
            int telephonyDisconnectCause = android.telephony.DisconnectCause.ERROR_UNSPECIFIED;
            connection.setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(
                    telephonyDisconnectCause));
            return createFailedConference(telephonyDisconnectCause, "unexpected error");
        }

        return imsConfController.createConference((TelephonyConnection) connection);
    }

    Conference createFailedConference(int disconnectCause, String reason) {
        return createFailedConference(
            DisconnectCauseUtil.toTelecomDisconnectCause(disconnectCause, reason));
    }

    Conference createFailedConference(android.telecom.DisconnectCause disconnectCause) {
        Conference failedConference = new Conference(null) { };
        failedConference.setDisconnected(disconnectCause);
        return failedConference;
    }
    /// @}


    /**
     * M: check data only add for OP09 Plug in. @{
     * @param phone The target phone
     */
    public boolean isDataOnlyMode(Phone phone) {
        if (ExtensionManager.getTelephonyConnectionServiceExt(
                ).isDataOnlyMode(PhoneGlobals.getInstance().
                    getApplicationContext(), phone)) {
            return true;
        }
        return false;
    }
    /** @} */

    /// M: ALPS02072589 @{
    /**
     * Register Supplementary Messages for ImsPhone.
     * @param phone ImsPhone
     */
    public void registerSuppMessageForImsPhone(Phone phone) {
        if (mSuppMessageManager == null) {
            return;
        }
        mSuppMessageManager.registerSuppMessageForPhone(phone);
    }

    /**
     * Unregister Supplementary Messages for ImsPhone.
     * @param phone ImsPhone
     */
    public void unregisterSuppMessageForImsPhone(Phone phone) {
        if (mSuppMessageManager == null) {
            return;
        }
        mSuppMessageManager.unregisterSuppMessageForPhone(phone);
    }
    /// @}

    /// M: ECC Retry @{
    /// M: Add for GSM+CDMA ecc. @{
    /**
     * Checked if the ecc request need to handle by internal rules.
     * @param accountHandle The target PhoneAccountHandle.
     * @param number The ecc number.
     */
    public Phone selectPhoneBySpecialEccRule(
            PhoneAccountHandle accountHandle,
            String number) {
        EmergencyRuleHandler eccRuleHandler =
                new EmergencyRuleHandler(accountHandle, number, isEccRetryOn());
        return eccRuleHandler.getPreferredPhone();
    }
    /// @}

    /// M: SS project ECC change feature @{
    public boolean isGsmPreferredNumber(String number) {
        for (int i = 0; i < GSM_PREFERRED_NUMBERS.length; i++) {
            if (GSM_PREFERRED_NUMBERS[i].equals(number)) {
                return true;
            }
        }
        return false;
    }

    public int getSwitchPhoneType(String number, Phone phone, boolean useEmergencyCallHelper) {
        Log.d(this, "getSwitchPhoneType, phoneType:" + phone.getPhoneType()
                + ", number:" + number);
        if (mayNeedToSwitchPhone(number, useEmergencyCallHelper)) {
            if (isGsmPreferredNumber(number)) {
                if (phone.getPhoneType() != PhoneConstants.PHONE_TYPE_GSM) {
                    return PhoneConstants.PHONE_TYPE_GSM;
                }
            } else {
                if (phone.getPhoneType() != PhoneConstants.PHONE_TYPE_CDMA) {
                    return PhoneConstants.PHONE_TYPE_CDMA;
                }
            }
        }
        return PhoneConstants.PHONE_TYPE_NONE;
    }

    public boolean mayNeedToSwitchPhone(String number, boolean useEmergencyCallHelper) {
        TelephonyManager tm = TelephonyManager.getDefault();
        if (SystemProperties.get("ro.mtk_c2k_support").equals("1")
                && PhoneNumberUtils.isEmergencyNumber(number)
                && tm.getSimCount() == 1
                && (!tm.hasIccCard(0) || useEmergencyCallHelper)) {
            return true;
        } else {
            return false;
        }
    }
    /// @}
}
