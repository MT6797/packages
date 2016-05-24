/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.phone;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telecom.PhoneAccount;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.IccOpenLogicalChannelResponse;
import android.telephony.NeighboringCellInfo;
import android.telephony.RadioAccessFamily;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ModemActivityInfo;
import android.telephony.gsm.GsmCellLocation;
import android.telephony.cdma.CdmaCellLocation;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;

import com.android.ims.ImsManager;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.CellNetworkScanResult;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.DefaultPhoneNotifier;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.SubscriptionController;
// MTK-START
import com.android.internal.telephony.uicc.IccFileHandler;
// MTK-END
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.util.HexDump;

import static com.android.internal.telephony.PhoneConstants.SUBSCRIPTION_KEY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/// M: CC053: MoMS [Mobile Managerment] @{
// 1. Permission Control for MO by 3rd APP
import com.mediatek.common.mom.IMobileManager;
import com.mediatek.common.mom.IMobileManagerService;
import com.mediatek.common.mom.SubPermissions;
import com.mediatek.common.mom.MobileManagerUtils;
/// @}

import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.dataconnection.DcTrackerBase;

import com.mediatek.internal.telephony.RadioManager;


/**
 * Implementation of the ITelephony interface.
 */
public class PhoneInterfaceManager extends ITelephony.Stub {
    private static final String LOG_TAG = "PhoneInterfaceManager";
    private static final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);
    private static final boolean DBG_LOC = false;
    private static final boolean DBG_MERGE = false;

    // Message codes used with mMainThreadHandler
    private static final int CMD_HANDLE_PIN_MMI = 1;
    private static final int CMD_HANDLE_NEIGHBORING_CELL = 2;
    private static final int EVENT_NEIGHBORING_CELL_DONE = 3;
    private static final int CMD_ANSWER_RINGING_CALL = 4;
    private static final int CMD_END_CALL = 5;  // not used yet
    private static final int CMD_TRANSMIT_APDU_LOGICAL_CHANNEL = 7;
    private static final int EVENT_TRANSMIT_APDU_LOGICAL_CHANNEL_DONE = 8;
    private static final int CMD_OPEN_CHANNEL = 9;
    private static final int EVENT_OPEN_CHANNEL_DONE = 10;
    private static final int CMD_CLOSE_CHANNEL = 11;
    private static final int EVENT_CLOSE_CHANNEL_DONE = 12;
    private static final int CMD_NV_READ_ITEM = 13;
    private static final int EVENT_NV_READ_ITEM_DONE = 14;
    private static final int CMD_NV_WRITE_ITEM = 15;
    private static final int EVENT_NV_WRITE_ITEM_DONE = 16;
    private static final int CMD_NV_WRITE_CDMA_PRL = 17;
    private static final int EVENT_NV_WRITE_CDMA_PRL_DONE = 18;
    private static final int CMD_NV_RESET_CONFIG = 19;
    private static final int EVENT_NV_RESET_CONFIG_DONE = 20;
    private static final int CMD_GET_PREFERRED_NETWORK_TYPE = 21;
    private static final int EVENT_GET_PREFERRED_NETWORK_TYPE_DONE = 22;
    private static final int CMD_SET_PREFERRED_NETWORK_TYPE = 23;
    private static final int EVENT_SET_PREFERRED_NETWORK_TYPE_DONE = 24;
    private static final int CMD_SEND_ENVELOPE = 25;
    private static final int EVENT_SEND_ENVELOPE_DONE = 26;
    private static final int CMD_INVOKE_OEM_RIL_REQUEST_RAW = 27;
    private static final int EVENT_INVOKE_OEM_RIL_REQUEST_RAW_DONE = 28;
    private static final int CMD_TRANSMIT_APDU_BASIC_CHANNEL = 29;
    private static final int EVENT_TRANSMIT_APDU_BASIC_CHANNEL_DONE = 30;
    private static final int CMD_EXCHANGE_SIM_IO = 31;
    private static final int EVENT_EXCHANGE_SIM_IO_DONE = 32;
    private static final int CMD_SET_VOICEMAIL_NUMBER = 33;
    private static final int EVENT_SET_VOICEMAIL_NUMBER_DONE = 34;
    private static final int CMD_SET_NETWORK_SELECTION_MODE_AUTOMATIC = 35;
    private static final int EVENT_SET_NETWORK_SELECTION_MODE_AUTOMATIC_DONE = 36;
    private static final int CMD_GET_MODEM_ACTIVITY_INFO = 37;
    private static final int EVENT_GET_MODEM_ACTIVITY_INFO_DONE = 38;
    private static final int CMD_PERFORM_NETWORK_SCAN = 39;
    private static final int EVENT_PERFORM_NETWORK_SCAN_DONE = 40;
    private static final int CMD_SET_NETWORK_SELECTION_MODE_MANUAL = 41;
    private static final int EVENT_SET_NETWORK_SELECTION_MODE_MANUAL_DONE = 42;

    private static final int CMD_SET_RADIO_MODE = 100;
    private static final int EVENT_SET_RADIO_MODE_DONE = 101;
    private static final int CMD_SIM_IO = 102;
    private static final int EVENT_SIM_IO_DONE = 103;
    private static final int CMD_GET_ATR = 104;
    private static final int EVENT_GET_ATR_DONE = 105;
    private static final int CMD_OPEN_CHANNEL_WITH_SW = 106;
    private static final int EVENT_OPEN_CHANNEL_WITH_SW_DONE = 107;
    // MTK-START
    private static final int CMD_LOAD_EF_TRANSPARENT = 108;
    private static final int EVENT_LOAD_EF_TRANSPARENT_DONE = 109;
    private static final int CMD_LOAD_EF_LINEARFIXEDALL = 110;
    private static final int EVENT_LOAD_EF_LINEARFIXEDALL_DONE = 111;
    // MTK-END

    /** The singleton instance. */
    private static PhoneInterfaceManager sInstance;

    private PhoneGlobals mApp;
    private Phone mPhone;
    private CallManager mCM;
    private UserManager mUserManager;
    private AppOpsManager mAppOps;
    private MainThreadHandler mMainThreadHandler;
    private SubscriptionController mSubscriptionController;
    private SharedPreferences mTelephonySharedPreferences;

    private static final String PREF_CARRIERS_ALPHATAG_PREFIX = "carrier_alphtag_";
    private static final String PREF_CARRIERS_NUMBER_PREFIX = "carrier_number_";
    private static final String PREF_CARRIERS_SUBSCRIBER_PREFIX = "carrier_subscriber_";
    private static final String PREF_ENABLE_VIDEO_CALLING = "enable_video_calling";
    private static final String PREF_CARRIERS_SIMPLIFIED_NETWORK_SETTINGS_PREFIX =
            "carrier_simplified_network_settings_";
    // MTK-START
    private static final int COMMAND_READ_BINARY = 0xb0;
    private static final int COMMAND_READ_RECORD = 0xb2;
    // MTK-END

    /**
     * A request object to use for transmitting data to an ICC.
     */
    private static final class IccAPDUArgument {
        public int channel, cla, command, p1, p2, p3;
        public String data;
        public String pathId;
        public String pin2;
        public int slotId;
        // MTK-START
        public int family;
        // MTK-END

        public IccAPDUArgument(int channel, int cla, int command,
                int p1, int p2, int p3, String data) {
            int slot = SubscriptionManager.getSlotId(SubscriptionManager.getDefaultSubId());
            if (DBG) log("IccAPDUArgument, default slot " + slot);
            this.channel = channel;
            this.cla = cla;
            this.command = command;
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
            this.pathId = null;
            this.data = data;
            this.pin2 = null;
            this.slotId = slot;
        }

        public IccAPDUArgument(int slotId, int channel, int cla, int command,
                int p1, int p2, int p3, String pathId) {
            this.channel = channel;
            this.cla = cla;
            this.command = command;
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
            this.pathId = pathId;
            this.data = null;
            this.pin2 = null;
            this.slotId = slotId;
        }

        // MTK-START
        public IccAPDUArgument(int slotId, int family, int channel, int cla, int command,
                int p1, int p2, int p3, String pathId) {
            this.channel = channel;
            this.cla = cla;
            this.command = command;
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
            this.pathId = pathId;
            this.data = null;
            this.pin2 = null;
            this.slotId = slotId;
            this.family = family;
        }
        // MTK-END

        public IccAPDUArgument(int slotId, int channel, int cla, int command,
                int p1, int p2, int p3, String data, String pathId, String pin2) {
            this.channel = channel;
            this.cla = cla;
            this.command = command;
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
            this.pathId = pathId;
            this.data = data;
            this.pin2 = pin2;
            this.slotId = slotId;
        }
    }

    /**
     * A request object for use with {@link MainThreadHandler}. Requesters should wait() on the
     * request after sending. The main thread will notify the request when it is complete.
     */
    private static final class MainThreadRequest {
        /** The argument to use for the request */
        public Object argument;
        /** The result of the request that is run on the main thread */
        public Object result;
        /** The subscriber id that this request applies to. Null if default. */
        public Integer subId;

        public MainThreadRequest(Object argument) {
            this.argument = argument;
        }

        public MainThreadRequest(Object argument, Integer subId) {
            this.argument = argument;
            this.subId = subId;
        }
    }

    private static final class IncomingThirdPartyCallArgs {
        public final ComponentName component;
        public final String callId;
        public final String callerDisplayName;

        public IncomingThirdPartyCallArgs(ComponentName component, String callId,
                String callerDisplayName) {
            this.component = component;
            this.callId = callId;
            this.callerDisplayName = callerDisplayName;
        }
    }

    /**
     * A handler that processes messages on the main thread in the phone process. Since many
     * of the Phone calls are not thread safe this is needed to shuttle the requests from the
     * inbound binder threads to the main thread in the phone process.  The Binder thread
     * may provide a {@link MainThreadRequest} object in the msg.obj field that they are waiting
     * on, which will be notified when the operation completes and will contain the result of the
     * request.
     *
     * <p>If a MainThreadRequest object is provided in the msg.obj field,
     * note that request.result must be set to something non-null for the calling thread to
     * unblock.
     */
    private final class MainThreadHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            MainThreadRequest request;
            Message onCompleted;
            AsyncResult ar;
            UiccCard uiccCard = UiccController.getInstance().getUiccCard(mPhone.getPhoneId());
            IccAPDUArgument iccArgument;
            int slot;
            int subId;
            IccFileHandler fh;

            switch (msg.what) {
                case CMD_HANDLE_PIN_MMI: {
                    request = (MainThreadRequest) msg.obj;
                    final Phone phone = getPhoneFromRequest(request);
                    request.result = phone != null ?
                            getPhoneFromRequest(request).handlePinMmi((String) request.argument)
                            : false;
                    // Wake up the requesting thread
                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;
                }

                case CMD_HANDLE_NEIGHBORING_CELL:
                    request = (MainThreadRequest) msg.obj;
                    onCompleted = obtainMessage(EVENT_NEIGHBORING_CELL_DONE,
                            request);
                    mPhone.getNeighboringCids(onCompleted);
                    break;

                case EVENT_NEIGHBORING_CELL_DONE:
                    ar = (AsyncResult) msg.obj;
                    request = (MainThreadRequest) ar.userObj;
                    if (ar.exception == null && ar.result != null) {
                        request.result = ar.result;
                    } else {
                        // create an empty list to notify the waiting thread
                        request.result = new ArrayList<NeighboringCellInfo>(0);
                    }
                    // Wake up the requesting thread
                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;

                case CMD_ANSWER_RINGING_CALL:
                    request = (MainThreadRequest) msg.obj;
                    int answer_subId = request.subId;
                    answerRingingCallInternal(answer_subId);
                    // Wake up the requesting thread
                    /// M: for ALPS02014935. 2015-04-02 @{
                    synchronized (request) {
                        request.result = "OK";
                        request.notifyAll();
                    }
                    /// @}
                    break;

                case CMD_END_CALL:
                    request = (MainThreadRequest) msg.obj;
                    int end_subId = request.subId;
                    final boolean hungUp;
                    Phone phone = getPhone(end_subId);
                    if (phone == null) {
                        if (DBG) log("CMD_END_CALL: no phone for id: " + end_subId);
                        break;
                    }
                    int phoneType = phone.getPhoneType();
                    if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                        // CDMA: If the user presses the Power button we treat it as
                        // ending the complete call session
                        hungUp = PhoneUtils.hangupRingingAndActive(getPhone(end_subId));
                    } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                        // GSM: End the call as per the Phone state
                        hungUp = PhoneUtils.hangup(mCM);
                    } else {
                        throw new IllegalStateException("Unexpected phone type: " + phoneType);
                    }
                    if (DBG) log("CMD_END_CALL: " + (hungUp ? "hung up!" : "no call to hang up"));
                    request.result = hungUp;
                    // Wake up the requesting thread
                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;

                case CMD_TRANSMIT_APDU_LOGICAL_CHANNEL:
                    request = (MainThreadRequest) msg.obj;
                    iccArgument = (IccAPDUArgument) request.argument;
                    uiccCard = UiccController.getInstance().getUiccCard(iccArgument.slotId);
                    if (uiccCard == null) {
                        loge("iccTransmitApduLogicalChannel: No UICC");
                        request.result = new IccIoResult(0x6F, 0, (byte[])null);
                        synchronized (request) {
                            request.notifyAll();
                        }
                    } else {
                        onCompleted = obtainMessage(EVENT_TRANSMIT_APDU_LOGICAL_CHANNEL_DONE,
                            request);
                        uiccCard.iccTransmitApduLogicalChannel(
                            iccArgument.channel, iccArgument.cla, iccArgument.command,
                            iccArgument.p1, iccArgument.p2, iccArgument.p3, iccArgument.data,
                            onCompleted);
                    }
                    break;

                case EVENT_TRANSMIT_APDU_LOGICAL_CHANNEL_DONE:
                    ar = (AsyncResult) msg.obj;
                    request = (MainThreadRequest) ar.userObj;
                    if (ar.exception == null && ar.result != null) {
                        request.result = ar.result;
                    } else {
                        request.result = new IccIoResult(0x6F, 0, (byte[])null);
                        if (ar.result == null) {
                            loge("iccTransmitApduLogicalChannel: Empty response");
                        } else if (ar.exception instanceof CommandException) {
                            loge("iccTransmitApduLogicalChannel: CommandException: " +
                                    ar.exception);
                        } else {
                            loge("iccTransmitApduLogicalChannel: Unknown exception");
                        }
                    }
                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;

                case CMD_TRANSMIT_APDU_BASIC_CHANNEL:
                    request = (MainThreadRequest) msg.obj;
                    iccArgument = (IccAPDUArgument) request.argument;
                    uiccCard = UiccController.getInstance().getUiccCard(iccArgument.slotId);
                    if (uiccCard == null) {
                        loge("iccTransmitApduBasicChannel: No UICC");
                        request.result = new IccIoResult(0x6F, 0, (byte[])null);
                        synchronized (request) {
                            request.notifyAll();
                        }
                    } else {
                        onCompleted = obtainMessage(EVENT_TRANSMIT_APDU_BASIC_CHANNEL_DONE,
                            request);
                        uiccCard.iccTransmitApduBasicChannel(
                            iccArgument.cla, iccArgument.command, iccArgument.p1, iccArgument.p2,
                            iccArgument.p3, iccArgument.data, onCompleted);
                    }
                    break;

                case EVENT_TRANSMIT_APDU_BASIC_CHANNEL_DONE:
                    ar = (AsyncResult) msg.obj;
                    request = (MainThreadRequest) ar.userObj;
                    if (ar.exception == null && ar.result != null) {
                        request.result = ar.result;
                    } else {
                        request.result = new IccIoResult(0x6F, 0, (byte[])null);
                        if (ar.result == null) {
                            loge("iccTransmitApduBasicChannel: Empty response");
                        } else if (ar.exception instanceof CommandException) {
                            loge("iccTransmitApduBasicChannel: CommandException: " +
                                    ar.exception);
                        } else {
                            loge("iccTransmitApduBasicChannel: Unknown exception");
                        }
                    }
                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;

                case CMD_EXCHANGE_SIM_IO:
                    request = (MainThreadRequest) msg.obj;
                    iccArgument = (IccAPDUArgument) request.argument;
                    uiccCard = UiccController.getInstance().getUiccCard(iccArgument.slotId);
                    if (uiccCard == null) {
                        loge("iccExchangeSimIO: No UICC");
                        request.result = new IccIoResult(0x6F, 0, (byte[])null);
                        synchronized (request) {
                            request.notifyAll();
                        }
                    } else {
                        onCompleted = obtainMessage(EVENT_EXCHANGE_SIM_IO_DONE,
                                request);
                        uiccCard.iccExchangeSimIO(iccArgument.cla, /* fileID */
                                iccArgument.command, iccArgument.p1, iccArgument.p2, iccArgument.p3,
                                iccArgument.data, onCompleted);
                    }
                    break;

                case EVENT_EXCHANGE_SIM_IO_DONE:
                    ar = (AsyncResult) msg.obj;
                    request = (MainThreadRequest) ar.userObj;
                    if (ar.exception == null && ar.result != null) {
                        request.result = ar.result;
                    } else {
                        request.result = new IccIoResult(0x6f, 0, (byte[])null);
                    }
                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;
                // MTK-START
                case CMD_LOAD_EF_TRANSPARENT:
                    request = (MainThreadRequest) msg.obj;
                    iccArgument = (IccAPDUArgument) request.argument;
                    log("CMD_LOAD_EF_TRANSPARENT: slot " + iccArgument.slotId);

                    fh = UiccController.getInstance().getIccFileHandler(
                            iccArgument.slotId, iccArgument.family);
                    if (fh == null) {
                        loge("loadEFTransparent: No UICC");
                        request.result = null;
                        synchronized (request) {
                            request.notifyAll();
                        }
                    } else {
                        onCompleted = obtainMessage(EVENT_LOAD_EF_TRANSPARENT_DONE,
                                request);
                        fh.loadEFTransparent(iccArgument.cla, iccArgument.pathId,
                                onCompleted);
                    }
                    break;

                case EVENT_LOAD_EF_TRANSPARENT_DONE:
                    log("EVENT_LOAD_EF_TRANSPARENT_DONE");
                    ar = (AsyncResult) msg.obj;
                    request = (MainThreadRequest) ar.userObj;
                    if (ar.exception == null && ar.result != null) {
                        request.result = (byte[]) (ar.result);
                    } else {
                        request.result = null;
                    }
                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;

                case CMD_LOAD_EF_LINEARFIXEDALL:
                    request = (MainThreadRequest) msg.obj;
                    iccArgument = (IccAPDUArgument) request.argument;
                    log("CMD_LOAD_EF_LINEARFIXEDALL: slot " + iccArgument.slotId);

                    fh = UiccController.getInstance().getIccFileHandler(
                            iccArgument.slotId, iccArgument.family);
                    if (fh == null) {
                        loge("loadEFLinearFixedAll: No UICC");
                        request.result = null;
                        synchronized (request) {
                            request.notifyAll();
                        }
                    } else {
                        onCompleted = obtainMessage(EVENT_LOAD_EF_LINEARFIXEDALL_DONE,
                                request);
                        fh.loadEFLinearFixedAll(iccArgument.cla, iccArgument.pathId,
                                onCompleted);
                    }
                    break;

                case EVENT_LOAD_EF_LINEARFIXEDALL_DONE:
                    log("EVENT_LOAD_EF_TRANSPARENT_DONE");
                    ar = (AsyncResult) msg.obj;
                    request = (MainThreadRequest) ar.userObj;
                    if (ar.exception == null && ar.result != null) {
                        request.result = (ArrayList<byte[]>) (ar.result);
                    } else {
                        request.result = null;
                    }
                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;
                // MTK-END
                case CMD_SIM_IO:
                    request = (MainThreadRequest) msg.obj;
                    IccAPDUArgument parameters = (IccAPDUArgument) request.argument;
                    log("CMD_SIM_IO: slot " + parameters.slotId);
                    uiccCard = UiccController.getInstance().getUiccCard(parameters.slotId);
                    if (uiccCard == null) {
                        loge("iccExchangeSimIOExUsingSlot: No UICC");
                        request.result = new IccIoResult(0x6F, 0, (byte[]) null);
                        synchronized (request) {
                            request.notifyAll();
                        }
                    } else {
                        onCompleted = obtainMessage(EVENT_SIM_IO_DONE, parameters.slotId, 0 , request);
                        uiccCard.exchangeSimIo(parameters.cla, /* cla is fileID here! */
                                parameters.command, parameters.p1, parameters.p2, parameters.p3,
                                parameters.pathId, parameters.data, parameters.pin2, onCompleted);
                    }
                    break;

               case EVENT_SIM_IO_DONE:
                    ar = (AsyncResult) msg.obj;
                    request = (MainThreadRequest) ar.userObj;

                    log("EVENT_SIM_IO_DONE");
                    if (ar.exception == null && ar.result != null) {
                        request.result = ar.result;
                    } else {
                        request.result = new IccIoResult(0x6f, 0, (byte[]) null);
                        if (ar.result == null) {
                            loge("iccExchangeSimIOExUsingSlot: Empty response");
                        } else if (ar.exception != null && ar.exception instanceof CommandException) {
                            loge("iccExchangeSimIOExUsingSlot: CommandException: " +
                                    ar.exception);
                        } else {
                            loge("iccExchangeSimIOExUsingSlot: Unknown exception");
                        }
                    }

                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;

                case CMD_GET_ATR:
                    request = (MainThreadRequest) msg.obj;
                    slot = ((Integer) request.argument).intValue();

                    log("CMD_GET_ATR, slot " + slot);
                    uiccCard = UiccController.getInstance().getUiccCard(slot);
                    if (uiccCard == null) {
                        loge("get ATR: No UICC");
                        request.result = "";
                        synchronized (request) {
                            request.notifyAll();
                        }
                    } else {
                        onCompleted = obtainMessage(EVENT_GET_ATR_DONE, slot, 0, request);
                        uiccCard.iccGetAtr(onCompleted);
                    }
                    break;

                case EVENT_GET_ATR_DONE:
                    ar = (AsyncResult) msg.obj;
                    request = (MainThreadRequest) ar.userObj;
                    if (ar.exception == null) {
                        log("EVENT_GET_ATR_DONE, no exception");
                        request.result = ar.result;
                    } else {
                        loge("EVENT_GET_ATR_DONE, exception happens");
                        request.result = "";
                    }
                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;

                case CMD_OPEN_CHANNEL_WITH_SW:
                    request = (MainThreadRequest) msg.obj;
                    String AID = (String) request.argument;
                    slot = request.subId;

                    log("CMD_OPEN_CHANNEL_WITH_SW: AID " + AID + "slot " + slot);
                    uiccCard =  UiccController.getInstance().getUiccCard(slot);
                    if (uiccCard == null) {
                        loge("iccOpenLogicalChannelWithSW: No UICC");
                        request.result = new IccIoResult(0x6F, 0, (byte[]) null);
                        synchronized (request) {
                            request.notifyAll();
                        }
                    } else {
                        onCompleted = obtainMessage(EVENT_OPEN_CHANNEL_WITH_SW_DONE, slot, 0 , request);

                        uiccCard.iccOpenChannelWithSw(AID, onCompleted);
                    }
                    break;

                case EVENT_OPEN_CHANNEL_WITH_SW_DONE:
                    ar = (AsyncResult) msg.obj;
                    request = (MainThreadRequest) ar.userObj;
                    request.result = ar.result;

                    log("EVENT_OPEN_CHANNEL_WITH_SW_DONE");
                    if (ar.exception == null && ar.result != null) {
                        if (DBG) log("iccOpenLogicalChannelWithSW, no exception");
                    } else {
                        request.result = new IccIoResult(0xff, 0xff, (byte[]) null);
                        if (ar.result == null) {
                            loge("iccOpenLogicalChannelWithSW: Empty response");
                        } else if (ar.exception instanceof CommandException) {
                            loge("iccOpenLogicalChannelWithSW: CommandException: " +
                                    ar.exception);
                        } else {
                            loge("iccOpenLogicalChannelWithSW: Unknown exception");
                        }
                    }
                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;
                case CMD_SEND_ENVELOPE:
                    request = (MainThreadRequest) msg.obj;
                    if (uiccCard == null) {
                        loge("sendEnvelopeWithStatus: No UICC");
                        request.result = new IccIoResult(0x6F, 0, (byte[])null);
                        synchronized (request) {
                            request.notifyAll();
                        }
                    } else {
                        onCompleted = obtainMessage(EVENT_SEND_ENVELOPE_DONE, request);
                        uiccCard.sendEnvelopeWithStatus((String)request.argument, onCompleted);
                    }
                    break;

                case EVENT_SEND_ENVELOPE_DONE:
                    ar = (AsyncResult) msg.obj;
                    request = (MainThreadRequest) ar.userObj;
                    if (ar.exception == null && ar.result != null) {
                        request.result = ar.result;
                    } else {
                        request.result = new IccIoResult(0x6F, 0, (byte[])null);
                        if (ar.result == null) {
                            loge("sendEnvelopeWithStatus: Empty response");
                        } else if (ar.exception instanceof CommandException) {
                            loge("sendEnvelopeWithStatus: CommandException: " +
                                    ar.exception);
                        } else {
                            loge("sendEnvelopeWithStatus: exception:" + ar.exception);
                        }
                    }
                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;

                case CMD_OPEN_CHANNEL:
                    request = (MainThreadRequest) msg.obj;
                    slot = request.subId;
                    log("CMD_OPEN_CHANNEL: AID " + request.argument + "slot " + slot);
                    uiccCard =  UiccController.getInstance().getUiccCard(slot);
                    if (uiccCard == null) {
                        loge("iccOpenLogicalChannel: No UICC");
                        request.result = new IccIoResult(0x6F, 0, (byte[])null);
                        synchronized (request) {
                            request.notifyAll();
                        }
                    } else {
                        onCompleted = obtainMessage(EVENT_OPEN_CHANNEL_DONE, request);
                        uiccCard.iccOpenLogicalChannel((String)request.argument, onCompleted);
                    }
                    break;

                case EVENT_OPEN_CHANNEL_DONE:
                    ar = (AsyncResult) msg.obj;
                    request = (MainThreadRequest) ar.userObj;
                    IccOpenLogicalChannelResponse openChannelResp;
                    log("handle EVENT_OPEN_CHANNEL_DONE");
                    if (ar.exception == null && ar.result != null) {
                        int[] result = (int[]) ar.result;
                        int channelId = result[0];
                        byte[] selectResponse = null;
                        if (result.length > 1) {
                            selectResponse = new byte[result.length - 1];
                            for (int i = 1; i < result.length; ++i) {
                                selectResponse[i - 1] = (byte) result[i];
                            }
                        }
                        openChannelResp = new IccOpenLogicalChannelResponse(channelId,
                            IccOpenLogicalChannelResponse.STATUS_NO_ERROR, selectResponse);
                    } else {
                        if (ar.result == null) {
                            loge("iccOpenLogicalChannel: Empty response");
                        }
                        if (ar.exception != null) {
                            loge("iccOpenLogicalChannel: Exception: " + ar.exception);
                        }

                        int errorCode = IccOpenLogicalChannelResponse.STATUS_UNKNOWN_ERROR;
                        if (ar.exception instanceof CommandException) {
                            CommandException.Error error =
                                ((CommandException) (ar.exception)).getCommandError();
                            if (error == CommandException.Error.MISSING_RESOURCE) {
                                errorCode = IccOpenLogicalChannelResponse.STATUS_MISSING_RESOURCE;
                            } else if (error == CommandException.Error.NO_SUCH_ELEMENT) {
                                errorCode = IccOpenLogicalChannelResponse.STATUS_NO_SUCH_ELEMENT;
                            }
                        }
                        openChannelResp = new IccOpenLogicalChannelResponse(
                            IccOpenLogicalChannelResponse.INVALID_CHANNEL, errorCode, null);
                    }
                    request.result = openChannelResp;
                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;

                case CMD_CLOSE_CHANNEL:
                    request = (MainThreadRequest) msg.obj;
                    int channel = ((Integer) request.argument).intValue();
                    slot = request.subId;
                    if (DBG) log("CMD_CLOSE_CHANNEL: channel " + channel + "slot " + slot);
                    uiccCard =  UiccController.getInstance().getUiccCard(slot);
                    if (uiccCard == null) {
                        loge("iccCloseLogicalChannel: No UICC");
                        request.result = new IccIoResult(0x6F, 0, (byte[])null);
                        synchronized (request) {
                            request.notifyAll();
                        }
                    } else {
                        onCompleted = obtainMessage(EVENT_CLOSE_CHANNEL_DONE, request);
                        uiccCard.iccCloseLogicalChannel((Integer) channel, onCompleted);
                    }
                    break;

                case EVENT_CLOSE_CHANNEL_DONE:
                    if (DBG) log("EVENT_CLOSE_CHANNEL_DONE");
                    handleNullReturnEvent(msg, "iccCloseLogicalChannel");
                    break;

                case CMD_NV_READ_ITEM:
                    request = (MainThreadRequest) msg.obj;
                    onCompleted = obtainMessage(EVENT_NV_READ_ITEM_DONE, request);
                    mPhone.nvReadItem((Integer) request.argument, onCompleted);
                    break;

                case EVENT_NV_READ_ITEM_DONE:
                    ar = (AsyncResult) msg.obj;
                    request = (MainThreadRequest) ar.userObj;
                    if (ar.exception == null && ar.result != null) {
                        request.result = ar.result;     // String
                    } else {
                        request.result = "";
                        if (ar.result == null) {
                            loge("nvReadItem: Empty response");
                        } else if (ar.exception instanceof CommandException) {
                            loge("nvReadItem: CommandException: " +
                                    ar.exception);
                        } else {
                            loge("nvReadItem: Unknown exception");
                        }
                    }
                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;

                case CMD_NV_WRITE_ITEM:
                    request = (MainThreadRequest) msg.obj;
                    onCompleted = obtainMessage(EVENT_NV_WRITE_ITEM_DONE, request);
                    Pair<Integer, String> idValue = (Pair<Integer, String>) request.argument;
                    mPhone.nvWriteItem(idValue.first, idValue.second, onCompleted);
                    break;

                case EVENT_NV_WRITE_ITEM_DONE:
                    handleNullReturnEvent(msg, "nvWriteItem");
                    break;

                case CMD_NV_WRITE_CDMA_PRL:
                    request = (MainThreadRequest) msg.obj;
                    onCompleted = obtainMessage(EVENT_NV_WRITE_CDMA_PRL_DONE, request);
                    mPhone.nvWriteCdmaPrl((byte[]) request.argument, onCompleted);
                    break;

                case EVENT_NV_WRITE_CDMA_PRL_DONE:
                    handleNullReturnEvent(msg, "nvWriteCdmaPrl");
                    break;

                case CMD_NV_RESET_CONFIG:
                    request = (MainThreadRequest) msg.obj;
                    onCompleted = obtainMessage(EVENT_NV_RESET_CONFIG_DONE, request);
                    mPhone.nvResetConfig((Integer) request.argument, onCompleted);
                    break;

                case EVENT_NV_RESET_CONFIG_DONE:
                    handleNullReturnEvent(msg, "nvResetConfig");
                    break;

                case CMD_GET_PREFERRED_NETWORK_TYPE:
                    request = (MainThreadRequest) msg.obj;
                    onCompleted = obtainMessage(EVENT_GET_PREFERRED_NETWORK_TYPE_DONE, request);
                    getPhoneFromRequest(request).getPreferredNetworkType(onCompleted);
                    break;

                case EVENT_GET_PREFERRED_NETWORK_TYPE_DONE:
                    ar = (AsyncResult) msg.obj;
                    request = (MainThreadRequest) ar.userObj;
                    if (ar.exception == null && ar.result != null) {
                        request.result = ar.result;     // Integer
                    } else {
                        request.result = -1;
                        if (ar.result == null) {
                            loge("getPreferredNetworkType: Empty response");
                        } else if (ar.exception instanceof CommandException) {
                            loge("getPreferredNetworkType: CommandException: " +
                                    ar.exception);
                        } else {
                            loge("getPreferredNetworkType: Unknown exception");
                        }
                    }
                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;

                case CMD_SET_PREFERRED_NETWORK_TYPE:
                    request = (MainThreadRequest) msg.obj;
                    /// M: Add for sim card be hot plug out when set prefer nw type. @ {
                    if (request.subId != null
                            && mSubscriptionController.getPhoneId(request.subId)
                            == SubscriptionManager.DEFAULT_PHONE_INDEX) {
                        if (DBG) {
                            log("CMD_SET_PREFERRED_NETWORK_TYPE, subId= " +
                                    request.subId + " not found ");
                        }
                        request.result = false;
                        synchronized (request) {
                            request.notifyAll();
                        }
                        return;
                    }
                    /// @}
                    onCompleted = obtainMessage(EVENT_SET_PREFERRED_NETWORK_TYPE_DONE, request);
                    int networkType = (Integer) request.argument;
                    getPhoneFromRequest(request).setPreferredNetworkType(networkType, onCompleted);
                    break;

                case EVENT_SET_PREFERRED_NETWORK_TYPE_DONE:
                    handleNullReturnEvent(msg, "setPreferredNetworkType");
                    break;

                case CMD_INVOKE_OEM_RIL_REQUEST_RAW:
                    request = (MainThreadRequest)msg.obj;
                    onCompleted = obtainMessage(EVENT_INVOKE_OEM_RIL_REQUEST_RAW_DONE, request);
                    mPhone.invokeOemRilRequestRaw((byte[])request.argument, onCompleted);
                    break;

                case EVENT_INVOKE_OEM_RIL_REQUEST_RAW_DONE:
                    ar = (AsyncResult)msg.obj;
                    request = (MainThreadRequest)ar.userObj;
                    request.result = ar;
                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;

                case CMD_SET_VOICEMAIL_NUMBER:
                    request = (MainThreadRequest) msg.obj;
                    onCompleted = obtainMessage(EVENT_SET_VOICEMAIL_NUMBER_DONE, request);
                    Pair<String, String> tagNum = (Pair<String, String>) request.argument;
                    getPhoneFromRequest(request).setVoiceMailNumber(tagNum.first, tagNum.second,
                            onCompleted);
                    break;

                case EVENT_SET_VOICEMAIL_NUMBER_DONE:
                    handleNullReturnEvent(msg, "setVoicemailNumber");
                    break;

                case CMD_SET_NETWORK_SELECTION_MODE_AUTOMATIC:
                    request = (MainThreadRequest) msg.obj;
                    onCompleted = obtainMessage(EVENT_SET_NETWORK_SELECTION_MODE_AUTOMATIC_DONE,
                            request);
                    getPhoneFromRequest(request).setNetworkSelectionModeAutomatic(onCompleted);
                    break;

                case EVENT_SET_NETWORK_SELECTION_MODE_AUTOMATIC_DONE:
                    handleNullReturnEvent(msg, "setNetworkSelectionModeAutomatic");
                    break;

                case CMD_PERFORM_NETWORK_SCAN:
                    request = (MainThreadRequest) msg.obj;
                    onCompleted = obtainMessage(EVENT_PERFORM_NETWORK_SCAN_DONE, request);
                    getPhoneFromRequest(request).getAvailableNetworks(onCompleted);
                    break;

                case EVENT_PERFORM_NETWORK_SCAN_DONE:
                    ar = (AsyncResult) msg.obj;
                    request = (MainThreadRequest) ar.userObj;
                    CellNetworkScanResult cellScanResult;
                    if (ar.exception == null && ar.result != null) {
                        cellScanResult = new CellNetworkScanResult(
                                CellNetworkScanResult.STATUS_SUCCESS,
                                (List<OperatorInfo>) ar.result);
                    } else {
                        if (ar.result == null) {
                            loge("getCellNetworkScanResults: Empty response");
                        }
                        if (ar.exception != null) {
                            loge("getCellNetworkScanResults: Exception: " + ar.exception);
                        }
                        int errorCode = CellNetworkScanResult.STATUS_UNKNOWN_ERROR;
                        if (ar.exception instanceof CommandException) {
                            CommandException.Error error =
                                ((CommandException) (ar.exception)).getCommandError();
                            if (error == CommandException.Error.RADIO_NOT_AVAILABLE) {
                                errorCode = CellNetworkScanResult.STATUS_RADIO_NOT_AVAILABLE;
                            } else if (error == CommandException.Error.GENERIC_FAILURE) {
                                errorCode = CellNetworkScanResult.STATUS_RADIO_GENERIC_FAILURE;
                            }
                        }
                        cellScanResult = new CellNetworkScanResult(errorCode, null);
                    }
                    request.result = cellScanResult;
                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;

                case CMD_SET_NETWORK_SELECTION_MODE_MANUAL:
                    request = (MainThreadRequest) msg.obj;
                    OperatorInfo operator = (OperatorInfo) request.argument;
                    onCompleted = obtainMessage(EVENT_SET_NETWORK_SELECTION_MODE_MANUAL_DONE,
                            request);
                    getPhoneFromRequest(request).selectNetworkManually(operator, onCompleted);
                    break;

                case EVENT_SET_NETWORK_SELECTION_MODE_MANUAL_DONE:
                    handleNullReturnEvent(msg, "setNetworkSelectionModeManual");
                    break;

                case CMD_GET_MODEM_ACTIVITY_INFO:
                    request = (MainThreadRequest) msg.obj;
                    onCompleted = obtainMessage(EVENT_GET_MODEM_ACTIVITY_INFO_DONE, request);
                    mPhone.getModemActivityInfo(onCompleted);
                    break;

                case EVENT_GET_MODEM_ACTIVITY_INFO_DONE:
                    ar = (AsyncResult) msg.obj;
                    request = (MainThreadRequest) ar.userObj;
                    if (ar.exception == null && ar.result != null) {
                        request.result = ar.result;
                    } else {
                        if (ar.result == null) {
                            loge("queryModemActivityInfo: Empty response");
                        } else if (ar.exception instanceof CommandException) {
                            loge("queryModemActivityInfo: CommandException: " +
                                    ar.exception);
                        } else {
                            loge("queryModemActivityInfo: Unknown exception");
                        }
                    }
                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;

                default:
                    Log.w(LOG_TAG, "MainThreadHandler: unexpected message code: " + msg.what);
                    break;
            }
        }

        private void handleNullReturnEvent(Message msg, String command) {
            AsyncResult ar = (AsyncResult) msg.obj;
            MainThreadRequest request = (MainThreadRequest) ar.userObj;
            if (ar.exception == null) {
                request.result = true;
            } else {
                request.result = false;
                if (ar.exception instanceof CommandException) {
                    loge(command + ": CommandException: " + ar.exception);
                } else {
                    loge(command + ": Unknown exception");
                }
            }
            synchronized (request) {
                request.notifyAll();
            }
        }
    }

    /**
     * Posts the specified command to be executed on the main thread,
     * waits for the request to complete, and returns the result.
     * @see #sendRequestAsync
     */
    private Object sendRequest(int command, Object argument) {
        return sendRequest(command, argument, null);
    }

    /**
     * Posts the specified command to be executed on the main thread,
     * waits for the request to complete, and returns the result.
     * @see #sendRequestAsync
     */
    private Object sendRequest(int command, Object argument, Integer subId) {
        if (Looper.myLooper() == mMainThreadHandler.getLooper()) {
            throw new RuntimeException("This method will deadlock if called from the main thread.");
        }

        MainThreadRequest request = new MainThreadRequest(argument, subId);
        Message msg = mMainThreadHandler.obtainMessage(command, request);
        msg.sendToTarget();

        // Wait for the request to complete
        synchronized (request) {
            while (request.result == null) {
                try {
                    request.wait();
                } catch (InterruptedException e) {
                    // Do nothing, go back and wait until the request is complete
                }
            }
        }
        return request.result;
    }

    /**
     * Asynchronous ("fire and forget") version of sendRequest():
     * Posts the specified command to be executed on the main thread, and
     * returns immediately.
     * @see #sendRequest
     */
    private void sendRequestAsync(int command) {
        mMainThreadHandler.sendEmptyMessage(command);
    }

    /**
     * Same as {@link #sendRequestAsync(int)} except it takes an argument.
     * @see {@link #sendRequest(int,Object)}
     */
    private void sendRequestAsync(int command, Object argument) {
        MainThreadRequest request = new MainThreadRequest(argument);
        Message msg = mMainThreadHandler.obtainMessage(command, request);
        msg.sendToTarget();
    }

    /**
     * Initialize the singleton PhoneInterfaceManager instance.
     * This is only done once, at startup, from PhoneApp.onCreate().
     */
    /* package */ static PhoneInterfaceManager init(PhoneGlobals app, Phone phone) {
        synchronized (PhoneInterfaceManager.class) {
            if (sInstance == null) {
                sInstance = new PhoneInterfaceManager(app, phone);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            return sInstance;
        }
    }

    /** Private constructor; @see init() */
    private PhoneInterfaceManager(PhoneGlobals app, Phone phone) {
        mApp = app;
        mPhone = phone;
        mCM = PhoneGlobals.getInstance().mCM;
        mUserManager = (UserManager) app.getSystemService(Context.USER_SERVICE);
        mAppOps = (AppOpsManager)app.getSystemService(Context.APP_OPS_SERVICE);
        mMainThreadHandler = new MainThreadHandler();
        mTelephonySharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(mPhone.getContext());
        mSubscriptionController = SubscriptionController.getInstance();

        publish();
    }

    private void publish() {
        if (DBG) log("publish: " + this);

        ServiceManager.addService("phone", this);
    }

    private Phone getPhoneFromRequest(MainThreadRequest request) {
        return (request.subId == null) ? mPhone : getPhone(request.subId);
    }

    // returns phone associated with the subId.
    private Phone getPhone(int subId) {
        return PhoneFactory.getPhone(mSubscriptionController.getPhoneId(subId));
    }
    //
    // Implementation of the ITelephony interface.
    //

    public void dial(String number) {
        dialForSubscriber(getPreferredVoiceSubscription(), number);
    }

    public void dialForSubscriber(int subId, String number) {
        if (DBG) log("dial: " + number);
        // No permission check needed here: This is just a wrapper around the
        // ACTION_DIAL intent, which is available to any app since it puts up
        // the UI before it does anything.

        String url = createTelUrl(number);
        if (url == null) {
            return;
        }

        // PENDING: should we just silently fail if phone is offhook or ringing?
        PhoneConstants.State state = mCM.getState(subId);
        if (state != PhoneConstants.State.OFFHOOK && state != PhoneConstants.State.RINGING) {
            Intent  intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mApp.startActivity(intent);
        }
    }

    public void call(String callingPackage, String number) {
        callForSubscriber(getPreferredVoiceSubscription(), callingPackage, number);
    }

    public void callForSubscriber(int subId, String callingPackage, String number) {
        if (DBG) log("call: " + number);

        /// M: CC053: MoMS [Mobile Managerment] @{
        // 1. Permission Control for MO by 3rd APP
        if (MobileManagerUtils.isSupported()) {
            if (!checkMoMSPhonePermission(number)) {
                return;
            }
        }
        /// @}

        // This is just a wrapper around the ACTION_CALL intent, but we still
        // need to do a permission check since we're calling startActivity()
        // from the context of the phone app.
        enforceCallPermission();

        if (mAppOps.noteOp(AppOpsManager.OP_CALL_PHONE, Binder.getCallingUid(), callingPackage)
                != AppOpsManager.MODE_ALLOWED) {
            return;
        }

        String url = createTelUrl(number);
        if (url == null) {
            return;
        }

        boolean isValid = false;
        final List<SubscriptionInfo> slist = getActiveSubscriptionInfoList();
        if (slist != null) {
            for (SubscriptionInfo subInfoRecord : slist) {
                if (subInfoRecord.getSubscriptionId() == subId) {
                    isValid = true;
                    break;
                }
            }
        }
        if (isValid == false) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse(url));
        intent.putExtra(SUBSCRIPTION_KEY, subId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mApp.startActivity(intent);
    }

    /**
     * End a call based on call state
     * @return true is a call was ended
     */
    public boolean endCall() {
        return endCallForSubscriber(getDefaultSubscription());
    }

    /**
     * End a call based on the call state of the subId
     * @return true is a call was ended
     */
    public boolean endCallForSubscriber(int subId) {
        enforceCallPermission();
        return (Boolean) sendRequest(CMD_END_CALL, null, new Integer(subId));
    }

    public void answerRingingCall() {
        answerRingingCallForSubscriber(getDefaultSubscription());
    }

    public void answerRingingCallForSubscriber(int subId) {
        if (DBG) log("answerRingingCall...");
        // TODO: there should eventually be a separate "ANSWER_PHONE" permission,
        // but that can probably wait till the big TelephonyManager API overhaul.
        // For now, protect this call with the MODIFY_PHONE_STATE permission.
        enforceModifyPermission();
        sendRequest(CMD_ANSWER_RINGING_CALL, null, new Integer(subId));
    }

    /**
     * Make the actual telephony calls to implement answerRingingCall().
     * This should only be called from the main thread of the Phone app.
     * @see #answerRingingCall
     *
     * TODO: it would be nice to return true if we answered the call, or
     * false if there wasn't actually a ringing incoming call, or some
     * other error occurred.  (In other words, pass back the return value
     * from PhoneUtils.answerCall() or PhoneUtils.answerAndEndActive().)
     * But that would require calling this method via sendRequest() rather
     * than sendRequestAsync(), and right now we don't actually *need* that
     * return value, so let's just return void for now.
     */
    private void answerRingingCallInternal(int subId) {
        final boolean hasRingingCall = !getPhone(subId).getRingingCall().isIdle();
        if (hasRingingCall) {
            final boolean hasActiveCall = !getPhone(subId).getForegroundCall().isIdle();
            final boolean hasHoldingCall = !getPhone(subId).getBackgroundCall().isIdle();
            if (hasActiveCall && hasHoldingCall) {
                // Both lines are in use!
                // TODO: provide a flag to let the caller specify what
                // policy to use if both lines are in use.  (The current
                // behavior is hardwired to "answer incoming, end ongoing",
                // which is how the CALL button is specced to behave.)
                PhoneUtils.answerAndEndActive(mCM, mCM.getFirstActiveRingingCall());
                return;
            } else {
                // answerCall() will automatically hold the current active
                // call, if there is one.
                PhoneUtils.answerCall(mCM.getFirstActiveRingingCall());
                return;
            }
        } else {
            // No call was ringing.
            return;
        }
    }

    /**
     * This method is no longer used and can be removed once TelephonyManager stops referring to it.
     */
    public void silenceRinger() {
        Log.e(LOG_TAG, "silenseRinger not supported");
    }

    @Override
    public boolean isOffhook(String callingPackage) {
        return isOffhookForSubscriber(getDefaultSubscription(), callingPackage);
    }

    @Override
    public boolean isOffhookForSubscriber(int subId, String callingPackage) {
        if (!canReadPhoneState(callingPackage, "isOffhookForSubscriber")) {
            return false;
        }

        final Phone phone = getPhone(subId);
        if (phone != null) {
            return (phone.getState() == PhoneConstants.State.OFFHOOK);
        } else {
            return false;
        }
    }

    @Override
    public boolean isRinging(String callingPackage) {
        return (isRingingForSubscriber(getDefaultSubscription(), callingPackage));
    }

    @Override
    public boolean isRingingForSubscriber(int subId, String callingPackage) {
        if (!canReadPhoneState(callingPackage, "isRingingForSubscriber")) {
            return false;
        }

        final Phone phone = getPhone(subId);
        if (phone != null) {
            return (phone.getState() == PhoneConstants.State.RINGING);
        } else {
            return false;
        }
    }

    @Override
    public boolean isIdle(String callingPackage) {
        return isIdleForSubscriber(getDefaultSubscription(), callingPackage);
    }

    @Override
    public boolean isIdleForSubscriber(int subId, String callingPackage) {
        if (!canReadPhoneState(callingPackage, "isIdleForSubscriber")) {
            return false;
        }

        final Phone phone = getPhone(subId);
        if (phone != null) {
            return (phone.getState() == PhoneConstants.State.IDLE);
        } else {
            return false;
        }
    }

    public boolean isSimPinEnabled(String callingPackage) {
        if (!canReadPhoneState(callingPackage, "isSimPinEnabled")) {
            return false;
        }

        return (PhoneGlobals.getInstance().isSimPinEnabled());
    }

    public boolean supplyPin(String pin) {
        return supplyPinForSubscriber(getDefaultSubscription(), pin);
    }

    public boolean supplyPinForSubscriber(int subId, String pin) {
        int [] resultArray = supplyPinReportResultForSubscriber(subId, pin);
        return (resultArray[0] == PhoneConstants.PIN_RESULT_SUCCESS) ? true : false;
    }

    public boolean supplyPuk(String puk, String pin) {
        return supplyPukForSubscriber(getDefaultSubscription(), puk, pin);
    }

    public boolean supplyPukForSubscriber(int subId, String puk, String pin) {
        int [] resultArray = supplyPukReportResultForSubscriber(subId, puk, pin);
        return (resultArray[0] == PhoneConstants.PIN_RESULT_SUCCESS) ? true : false;
    }

    /** {@hide} */
    public int[] supplyPinReportResult(String pin) {
        return supplyPinReportResultForSubscriber(getDefaultSubscription(), pin);
    }

    public int[] supplyPinReportResultForSubscriber(int subId, String pin) {
        enforceModifyPermission();
        final UnlockSim checkSimPin = new UnlockSim(getPhone(subId).getIccCard());
        checkSimPin.start();
        return checkSimPin.unlockSim(null, pin);
    }

    /** {@hide} */
    public int[] supplyPukReportResult(String puk, String pin) {
        return supplyPukReportResultForSubscriber(getDefaultSubscription(), puk, pin);
    }

    public int[] supplyPukReportResultForSubscriber(int subId, String puk, String pin) {
        enforceModifyPermission();
        final UnlockSim checkSimPuk = new UnlockSim(getPhone(subId).getIccCard());
        checkSimPuk.start();
        return checkSimPuk.unlockSim(puk, pin);
    }

    /**
     * Helper thread to turn async call to SimCard#supplyPin into
     * a synchronous one.
     */
    private static class UnlockSim extends Thread {

        private final IccCard mSimCard;

        private boolean mDone = false;
        private int mResult = PhoneConstants.PIN_GENERAL_FAILURE;
        private int mRetryCount = -1;

        // For replies from SimCard interface
        private Handler mHandler;

        // For async handler to identify request type
        private static final int SUPPLY_PIN_COMPLETE = 100;

        public UnlockSim(IccCard simCard) {
            mSimCard = simCard;
        }

        @Override
        public void run() {
            Looper.prepare();
            synchronized (UnlockSim.this) {
                mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        AsyncResult ar = (AsyncResult) msg.obj;
                        switch (msg.what) {
                            case SUPPLY_PIN_COMPLETE:
                                Log.d(LOG_TAG, "SUPPLY_PIN_COMPLETE");
                                synchronized (UnlockSim.this) {
                                    mRetryCount = msg.arg1;
                                    if (ar.exception != null) {
                                        if (ar.exception instanceof CommandException &&
                                                ((CommandException)(ar.exception)).getCommandError()
                                                == CommandException.Error.PASSWORD_INCORRECT) {
                                            mResult = PhoneConstants.PIN_PASSWORD_INCORRECT;
                                        } else {
                                            mResult = PhoneConstants.PIN_GENERAL_FAILURE;
                                        }
                                    } else {
                                        mResult = PhoneConstants.PIN_RESULT_SUCCESS;
                                    }
                                    mDone = true;
                                    UnlockSim.this.notifyAll();
                                }
                                break;
                        }
                    }
                };
                UnlockSim.this.notifyAll();
            }
            Looper.loop();
        }

        /*
         * Use PIN or PUK to unlock SIM card
         *
         * If PUK is null, unlock SIM card with PIN
         *
         * If PUK is not null, unlock SIM card with PUK and set PIN code
         */
        synchronized int[] unlockSim(String puk, String pin) {

            while (mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            Message callback = Message.obtain(mHandler, SUPPLY_PIN_COMPLETE);

            if (puk == null) {
                mSimCard.supplyPin(pin, callback);
            } else {
                mSimCard.supplyPuk(puk, pin, callback);
            }

            while (!mDone) {
                try {
                    Log.d(LOG_TAG, "wait for done");
                    wait();
                } catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                }
            }
            Log.d(LOG_TAG, "done");
            int[] resultArray = new int[2];
            resultArray[0] = mResult;
            resultArray[1] = mRetryCount;
            return resultArray;
        }
    }

    public void updateServiceLocation() {
        updateServiceLocationForSubscriber(getDefaultSubscription());

    }

    public void updateServiceLocationForSubscriber(int subId) {
        // No permission check needed here: this call is harmless, and it's
        // needed for the ServiceState.requestStateUpdate() call (which is
        // already intentionally exposed to 3rd parties.)
        final Phone phone = getPhone(subId);
        if (phone != null) {
            phone.updateServiceLocation();
        } else {
            log("getPhone is null");
        }
    }

    @Override
    public boolean isRadioOn(String callingPackage) {
        return isRadioOnForSubscriber(getDefaultSubscription(), callingPackage);
    }

    @Override
    public boolean isRadioOnForSubscriber(int subId, String callingPackage) {
        if (!canReadPhoneState(callingPackage, "isRadioOnForSubscriber")) {
            return false;
        }
        return isRadioOnForSubscriber(subId);
    }

    private boolean isRadioOnForSubscriber(int subId) {
        log("SubscriptionManager.getPhoneId(" + subId + ")="
                + SubscriptionManager.getPhoneId(subId));
        final Phone phone = getPhone(subId);
        // ServiceState serviceState = null;
        if (phone != null) {
            return phone.isRadioOn();
            /*serviceState = phone.getServiceState();
            if (serviceState != null) {
                log("ServiceState= " + serviceState.getState());
            } else {
                log("getServiceState is null");
                return false;
            }*/
        } else {
            log("getPhone is null");
            return false;
        }
        // return serviceState.getState() != ServiceState.STATE_POWER_OFF;
        // return getPhone(subId).getServiceState().getState() != ServiceState.STATE_POWER_OFF;
    }

    public void toggleRadioOnOff() {
        toggleRadioOnOffForSubscriber(getDefaultSubscription());

    }

    public void toggleRadioOnOffForSubscriber(int subId) {
        enforceModifyPermission();
        if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            log("toggleRadioOnOffForSubscriber handled by RadioManager");
            int phoneId = mSubscriptionController.getPhoneId(subId);
            RadioManager.getInstance().notifySimModeChange(!isRadioOnForSubscriber(subId), phoneId);
        } else {
            final Phone phone = getPhone(subId);
            if (phone != null) {
                phone.setRadioPower(!isRadioOnForSubscriber(subId));
            }
        }
    }

    public boolean setRadio(boolean turnOn) {
        return setRadioForSubscriber(getDefaultSubscription(), turnOn);
    }

    public boolean setRadioForSubscriber(int subId, boolean turnOn) {
        enforceModifyPermission();
        Phone phone = getPhone(subId);
        if (phone == null) {
            log("getPhone is null");
            return false;
        }
        //IMS will set SST as ON, even radio is off, so need to change to use Radio status.
        //ServiceState serviceState = phone.getServiceState();
        //if (serviceState == null) {
        //    log("getServiceState is null");
        //    return false;
        //}
        //if ((serviceState.getState() !=
        //        ServiceState.STATE_POWER_OFF) != turnOn) {
        log("setRadioForSubscriber, Radio=" + phone.isRadioOn() + "Request=" + turnOn);
        if (phone.isRadioOn() != turnOn) {
            toggleRadioOnOffForSubscriber(subId);
        }
        return true;
    }

    public boolean needMobileRadioShutdown() {
        /*
         * If any of the Radios are available, it will need to be
         * shutdown. So return true if any Radio is available.
         */
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            Phone phone = PhoneFactory.getPhone(i);
            boolean modemPowerOff = RadioManager.getInstance().isModemPowerOff(i);
            if (!modemPowerOff && phone != null && phone.isRadioAvailable()) return true;
        }
        logv(TelephonyManager.getDefault().getPhoneCount() + " Phones are shutdown.");
        return false;
    }

    public void shutdownMobileRadios() {
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            logv("Shutting down Phone " + i);
            shutdownRadioUsingPhoneId(i);
        }
    }

    private void shutdownRadioUsingPhoneId(int phoneId) {
        enforceModifyPermission();
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone != null && phone.isRadioAvailable()) {
            phone.shutdownRadio();
        }
    }

    public boolean setRadioPower(boolean turnOn) {
        return setRadioPowerForSubscriber(getDefaultSubscription(), turnOn);
    }

    public boolean setRadioPowerForSubscriber(int subId, boolean turnOn) {
        enforceModifyPermission();
        final Phone phone = getPhone(subId);
        if (phone != null) {
            phone.setRadioPower(turnOn);
            return true;
        } else {
            return false;
        }
    }

    // FIXME: subId version needed
    @Override
    public boolean enableDataConnectivity() {
        enforceModifyPermission();
        int subId = mSubscriptionController.getDefaultDataSubId();
        final Phone phone = getPhone(subId);
        if (phone != null) {
            phone.setDataEnabled(true);
            return true;
        } else {
            return false;
        }
    }

    // FIXME: subId version needed
    @Override
    public boolean disableDataConnectivity() {
        enforceModifyPermission();
        int subId = mSubscriptionController.getDefaultDataSubId();
        final Phone phone = getPhone(subId);
        if (phone != null) {
            phone.setDataEnabled(false);
            return true;
        } else {
            return false;
        }
    }

    // FIXME: subId version needed
    @Override
    public boolean isDataConnectivityPossible() {
        int subId = mSubscriptionController.getDefaultDataSubId();
        final Phone phone = getPhone(subId);
        if (phone != null) {
            return phone.isDataConnectivityPossible();
        } else {
            return false;
        }
    }

    public boolean handlePinMmi(String dialString) {
        return handlePinMmiForSubscriber(getDefaultSubscription(), dialString);
    }

    public boolean handlePinMmiForSubscriber(int subId, String dialString) {
        enforceModifyPermission();
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return false;
        }
        return (Boolean) sendRequest(CMD_HANDLE_PIN_MMI, dialString, subId);
    }

    public int getCallState() {
        return getCallStateForSubscriber(getDefaultSubscription());
    }

    public int getCallStateForSubscriber(int subId) {
        return DefaultPhoneNotifier.convertCallState(getPhone(subId).getState());
    }

    @Override
    public int getDataState() {
        Phone phone = getPhone(mSubscriptionController.getDefaultDataSubId());
        if (phone != null) {
            return DefaultPhoneNotifier.convertDataState(phone.getDataConnectionState());
        } else {
            return DefaultPhoneNotifier.convertDataState(PhoneConstants.DataState.DISCONNECTED);
        }
    }

    @Override
    public int getDataStateForSubscriber(int subId) {
        Phone phone = getPhone(subId);
        if (phone == null) {
            loge("getDataStateForSubscriber incorrect parameter [subId=" + subId + "]");
            return TelephonyManager.DATA_DISCONNECTED;
        } else {
            return DefaultPhoneNotifier.convertDataState(phone.getDataConnectionState());
        }
    }

    public int getDataActivity() {
        Phone phone = getPhone(mSubscriptionController.getDefaultDataSubId());
        if (phone != null) {
            return DefaultPhoneNotifier.convertDataActivityState(phone.getDataActivityState());
        } else {
            return TelephonyManager.DATA_ACTIVITY_NONE;
        }
    }

    public int getDataActivityForSubscriber(int subId) {
        Phone phone = getPhone(subId);
        if (phone == null) {
            loge("getDataActivityForSubscriber incorrect parameter [subId=" + subId + "]");
            return DefaultPhoneNotifier.convertDataActivityState(Phone.DataActivityState.NONE);
        } else {
            return DefaultPhoneNotifier.convertDataActivityState(phone.getDataActivityState());
        }
    }

    @Override
    public Bundle getCellLocation(String callingPackage) {
        enforceFineOrCoarseLocationPermission("getCellLocation");

        // OP_COARSE_LOCATION controls both fine and coarse location.
        if (mAppOps.noteOp(AppOpsManager.OP_COARSE_LOCATION, Binder.getCallingUid(),
                callingPackage) != AppOpsManager.MODE_ALLOWED) {
            log("getCellLocation: returning null; mode != allowed");
            return null;
        }

        if (checkIfCallerIsSelfOrForegroundUser()) {
            if (DBG_LOC) log("getCellLocation: is active user");
            Bundle data = new Bundle();
            //[ALPS01971678]-start
            try {
                for (Phone phone : PhoneFactory.getPhones()) {
                    CellLocation cellLocation = phone.getCellLocation();
                    if (cellLocation != null) {
                        if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM) {
                            if (!((GsmCellLocation) cellLocation).isEmpty()) {
                                cellLocation.fillInNotifierBundle(data);
                                log("phone.getGsmCellLocation:" + data);
                                return data;
                            }
                        } else if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                            if (!((CdmaCellLocation) cellLocation).isEmpty()) {
                                cellLocation.fillInNotifierBundle(data);
                                log("phone.getCdmaCellLocation:" + data);
                                return data;
                            }
                        } else {
                            log("phone.getCellLocation: phone type is abnormal");
                        }
                    }
                    log("phone.getCellLocation: is null");
                }
            } catch (IllegalStateException ex) {
                log("IllegalStateException");
                return null;
            }
            //Phone phone = getPhone(mSubscriptionController.getDefaultDataSubId());
            //phone.getCellLocation().fillInNotifierBundle(data);
            //[ALPS01971678]-end
            return data;
        } else {
            log("getCellLocation: suppress non-active user");
            return null;
        }
    }

    private void enforceFineOrCoarseLocationPermission(String message) {
        try {
            mApp.enforceCallingOrSelfPermission(
                    android.Manifest.permission.ACCESS_FINE_LOCATION, null);
        } catch (SecurityException e) {
            // If we have ACCESS_FINE_LOCATION permission, skip the check for ACCESS_COARSE_LOCATION
            // A failure should throw the SecurityException from ACCESS_COARSE_LOCATION since this
            // is the weaker precondition
            mApp.enforceCallingOrSelfPermission(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION, message);
        }
    }


    @Override
    public void enableLocationUpdates() {
        enableLocationUpdatesForSubscriber(getDefaultSubscription());
    }

    @Override
    public void enableLocationUpdatesForSubscriber(int subId) {
        mApp.enforceCallingOrSelfPermission(
                android.Manifest.permission.CONTROL_LOCATION_UPDATES, null);
        final Phone phone = getPhone(subId);
        if (phone != null) {
            phone.enableLocationUpdates();
        }
    }

    @Override
    public void disableLocationUpdates() {
        disableLocationUpdatesForSubscriber(getDefaultSubscription());
    }

    @Override
    public void disableLocationUpdatesForSubscriber(int subId) {
        mApp.enforceCallingOrSelfPermission(
                android.Manifest.permission.CONTROL_LOCATION_UPDATES, null);
        final Phone phone = getPhone(subId);
        if (phone != null) {
            phone.disableLocationUpdates();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<NeighboringCellInfo> getNeighboringCellInfo(String callingPackage) {
        enforceFineOrCoarseLocationPermission("getNeighboringCellInfo");

        // OP_COARSE_LOCATION controls both fine and coarse location.
        if (mAppOps.noteOp(AppOpsManager.OP_COARSE_LOCATION, Binder.getCallingUid(),
                callingPackage) != AppOpsManager.MODE_ALLOWED) {
            return null;
        }

        if (mAppOps.noteOp(AppOpsManager.OP_NEIGHBORING_CELLS, Binder.getCallingUid(),
                callingPackage) != AppOpsManager.MODE_ALLOWED) {
            return null;
        }

        if (checkIfCallerIsSelfOrForegroundUser()) {
            if (DBG_LOC) log("getNeighboringCellInfo: is active user");

            ArrayList<NeighboringCellInfo> cells = null;

            try {
                cells = (ArrayList<NeighboringCellInfo>) sendRequest(
                        CMD_HANDLE_NEIGHBORING_CELL, null, null);
            } catch (RuntimeException e) {
                Log.e(LOG_TAG, "getNeighboringCellInfo " + e);
            }
            return cells;
        } else {
            if (DBG_LOC) log("getNeighboringCellInfo: suppress non-active user");
            return null;
        }
    }


    @Override
    public List<CellInfo> getAllCellInfo(String callingPackage) {
        enforceFineOrCoarseLocationPermission("getAllCellInfo");

        // OP_COARSE_LOCATION controls both fine and coarse location.
        if (mAppOps.noteOp(AppOpsManager.OP_COARSE_LOCATION, Binder.getCallingUid(),
                callingPackage) != AppOpsManager.MODE_ALLOWED) {
            return null;
        }

        if (checkIfCallerIsSelfOrForegroundUser()) {
            if (DBG_LOC) log("getAllCellInfo: is active user");
            List<CellInfo> cellInfos = new ArrayList<CellInfo>();
            for (Phone phone : PhoneFactory.getPhones()) {
                final List<CellInfo> info = phone.getAllCellInfo();
                if (info != null) cellInfos.addAll(phone.getAllCellInfo());
            }
            return cellInfos;
        } else {
            if (DBG_LOC) log("getAllCellInfo: suppress non-active user");
            return null;
        }
    }

    @Override
    public void setCellInfoListRate(int rateInMillis) {
        mPhone.setCellInfoListRate(rateInMillis);
    }

    //
    // Internal helper methods.
    //

    private static boolean checkIfCallerIsSelfOrForegroundUser() {
        boolean ok;

        boolean self = Binder.getCallingUid() == Process.myUid();
        if (!self) {
            // Get the caller's user id then clear the calling identity
            // which will be restored in the finally clause.
            int callingUser = UserHandle.getCallingUserId();
            long ident = Binder.clearCallingIdentity();

            try {
                // With calling identity cleared the current user is the foreground user.
                int foregroundUser = ActivityManager.getCurrentUser();
                ok = (foregroundUser == callingUser);
                if (DBG_LOC) {
                    log("checkIfCallerIsSelfOrForegoundUser: foregroundUser=" + foregroundUser
                            + " callingUser=" + callingUser + " ok=" + ok);
                }
            } catch (Exception ex) {
                if (DBG_LOC) loge("checkIfCallerIsSelfOrForegoundUser: Exception ex=" + ex);
                ok = false;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            if (DBG_LOC) log("checkIfCallerIsSelfOrForegoundUser: is self");
            ok = true;
        }
        if (DBG_LOC) log("checkIfCallerIsSelfOrForegoundUser: ret=" + ok);
        return ok;
    }

    /**
     * Make sure the caller has the MODIFY_PHONE_STATE permission.
     *
     * @throws SecurityException if the caller does not have the required permission
     */
    private void enforceModifyPermission() {
        mApp.enforceCallingOrSelfPermission(android.Manifest.permission.MODIFY_PHONE_STATE, null);
    }

    /**
     * Make sure either system app or the caller has carrier privilege.
     *
     * @throws SecurityException if the caller does not have the required permission/privilege
     */
    private void enforceModifyPermissionOrCarrierPrivilege() {
        int permission = mApp.checkCallingOrSelfPermission(
                android.Manifest.permission.MODIFY_PHONE_STATE);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        log("No modify permission, check carrier privilege next.");
        if (getCarrierPrivilegeStatus() != TelephonyManager.CARRIER_PRIVILEGE_STATUS_HAS_ACCESS) {
            loge("No Carrier Privilege.");
            throw new SecurityException("No modify permission or carrier privilege.");
        }
    }

    /**
     * Make sure the caller has carrier privilege.
     *
     * @throws SecurityException if the caller does not have the required permission
     */
    private void enforceCarrierPrivilege() {
        if (getCarrierPrivilegeStatus() != TelephonyManager.CARRIER_PRIVILEGE_STATUS_HAS_ACCESS) {
            loge("No Carrier Privilege.");
            throw new SecurityException("No Carrier Privilege.");
        }
    }

    /**
     * Make sure the caller has the CALL_PHONE permission.
     *
     * @throws SecurityException if the caller does not have the required permission
     */
    private void enforceCallPermission() {
        mApp.enforceCallingOrSelfPermission(android.Manifest.permission.CALL_PHONE, null);
    }

    private void enforceConnectivityInternalPermission() {
        mApp.enforceCallingOrSelfPermission(
                android.Manifest.permission.CONNECTIVITY_INTERNAL,
                "ConnectivityService");
    }

    private String createTelUrl(String number) {
        if (TextUtils.isEmpty(number)) {
            return null;
        }

        return "tel:" + number;
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, "[PhoneIntfMgr] " + msg);
    }

    private static void logv(String msg) {
        Log.v(LOG_TAG, "[PhoneIntfMgr] " + msg);
    }

    private static void loge(String msg) {
        Log.e(LOG_TAG, "[PhoneIntfMgr] " + msg);
    }

    @Override
    public int getActivePhoneType() {
        return getActivePhoneTypeForSubscriber(getDefaultSubscription());
    }

    @Override
    public int getActivePhoneTypeForSubscriber(int subId) {
        final Phone phone = getPhone(subId);
        if (phone == null) {
            return PhoneConstants.PHONE_TYPE_NONE;
        } else {
            return getPhone(subId).getPhoneType();
        }
    }

    /**
     * Returns the CDMA ERI icon index to display
     */
    @Override
    public int getCdmaEriIconIndex(String callingPackage) {
        return getCdmaEriIconIndexForSubscriber(getDefaultSubscription(), callingPackage);
    }

    @Override
    public int getCdmaEriIconIndexForSubscriber(int subId, String callingPackage) {
        if (!canReadPhoneState(callingPackage, "getCdmaEriIconIndexForSubscriber")) {
            return -1;
        }
        final Phone phone = getPhone(subId);
        if (phone != null) {
            return phone.getCdmaEriIconIndex();
        } else {
            return -1;
        }
    }

    /**
     * Returns the CDMA ERI icon mode,
     * 0 - ON
     * 1 - FLASHING
     */
    @Override
    public int getCdmaEriIconMode(String callingPackage) {
        return getCdmaEriIconModeForSubscriber(getDefaultSubscription(), callingPackage);
    }

    @Override
    public int getCdmaEriIconModeForSubscriber(int subId, String callingPackage) {
        if (!canReadPhoneState(callingPackage, "getCdmaEriIconModeForSubscriber")) {
            return -1;
        }
        final Phone phone = getPhone(subId);
        if (phone != null) {
            return phone.getCdmaEriIconMode();
        } else {
            return -1;
        }
    }

    /**
     * Returns the CDMA ERI text,
     */
    @Override
    public String getCdmaEriText(String callingPackage) {
        return getCdmaEriTextForSubscriber(getDefaultSubscription(), callingPackage);
    }

    @Override
    public String getCdmaEriTextForSubscriber(int subId, String callingPackage) {
        if (!canReadPhoneState(callingPackage, "getCdmaEriIconTextForSubscriber")) {
            return null;
        }
        final Phone phone = getPhone(subId);
        if (phone != null) {
            return phone.getCdmaEriText();
        } else {
            return null;
        }
    }

    /**
     * Returns the CDMA MDN.
     */
    @Override
    public String getCdmaMdn(int subId) {
        enforceModifyPermissionOrCarrierPrivilege();
        final Phone phone = getPhone(subId);
        if (mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA && phone != null) {
            return phone.getLine1Number();
        } else {
            return null;
        }
    }

    /**
     * Returns the CDMA MIN.
     */
    @Override
    public String getCdmaMin(int subId) {
        enforceModifyPermissionOrCarrierPrivilege();
        final Phone phone = getPhone(subId);
        if (phone != null && phone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            return phone.getCdmaMin();
        } else {
            return null;
        }
    }

    /**
     * Returns true if CDMA provisioning needs to run.
     */
    public boolean needsOtaServiceProvisioning() {
        return mPhone.needsOtaServiceProvisioning();
    }

    /**
     * Sets the voice mail number of a given subId.
     */
    @Override
    public boolean setVoiceMailNumber(int subId, String alphaTag, String number) {
        enforceCarrierPrivilege();
        Boolean success = (Boolean) sendRequest(CMD_SET_VOICEMAIL_NUMBER,
                new Pair<String, String>(alphaTag, number), new Integer(subId));
        return success;
    }

    /**
     * Returns the unread count of voicemails
     */
    public int getVoiceMessageCount() {
        return getVoiceMessageCountForSubscriber(getDefaultSubscription());
    }

    /**
     * Returns the unread count of voicemails for a subId
     */
    @Override
    public int getVoiceMessageCountForSubscriber( int subId) {
        final Phone phone = getPhone(subId);
        if (phone != null) {
            return phone.getVoiceMessageCount();
        } else {
            return 0;
        }
    }

    /**
     * Returns the data network type.
     * Legacy call, permission-free.
     *
     * @Deprecated to be removed Q3 2013 use {@link #getDataNetworkType}.
     */
    @Override
    public int getNetworkType() {
        final Phone phone = getPhone(getDefaultSubscription());
        if (phone != null) {
            return phone.getServiceState().getDataNetworkType();
        } else {
            return TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }
    }

    /**
     * Returns the network type for a subId
     */
    @Override
    public int getNetworkTypeForSubscriber(int subId, String callingPackage) {
        if (!canReadPhoneState(callingPackage, "getNetworkTypeForSubscriber")) {
            return TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }

        final Phone phone = getPhone(subId);
        if (phone != null) {
            return phone.getServiceState().getDataNetworkType();
        } else {
            return TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }
    }

    /**
     * Returns the data network type
     */
    @Override
    public int getDataNetworkType(String callingPackage) {
        return getDataNetworkTypeForSubscriber(getDefaultSubscription(), callingPackage);
    }

    /**
     * Returns the data network type for a subId
     * @param subId the subId of the specific sim card.
     * @return get the data network type for the specific subId.
     */
    @Override

    public int getDataNetworkTypeForSubscriber(int subId, String callingPackage) {
        if (!canReadPhoneState(callingPackage, "getDataNetworkTypeForSubscriber")) {
            return TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }

        final Phone phone = getPhone(subId);
        int dataNetworkType;
        if (phone != null) {
            dataNetworkType = phone.getServiceState().getDataNetworkType();
        } else {
            dataNetworkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }
        log("getDataNetworkType=" + dataNetworkType);
        return dataNetworkType;
    }

    /**
     * Returns the Voice network type for a subId
     */
    @Override
    public int getVoiceNetworkTypeForSubscriber(int subId, String callingPackage) {
        if (!canReadPhoneState(callingPackage, "getDataNetworkTypeForSubscriber")) {
            return TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }

        final Phone phone = getPhone(subId);
        if (phone != null) {
            return phone.getServiceState().getVoiceNetworkType();
        } else {
            return TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }
    }

    /**
     * @return true if a ICC card is present
     */
    public boolean hasIccCard() {
        // FIXME Make changes to pass defaultSimId of type int
        return hasIccCardUsingSlotId(mSubscriptionController.getSlotId(getDefaultSubscription()));
    }

    /**
     * @return true if a ICC card is present for a slotId
     */
    @Override
    public boolean hasIccCardUsingSlotId(int slotId) {
        int subId[] = mSubscriptionController.getSubIdUsingSlotId(slotId);
        final Phone phone = getPhone(subId[0]);
        if (subId != null && phone != null) {
            return phone.getIccCard().hasIccCard();
        } else {
            return false;
        }
    }

    /**
     * Return if the current radio is LTE on CDMA. This
     * is a tri-state return value as for a period of time
     * the mode may be unknown.
     *
     * @param callingPackage the name of the package making the call.
     * @return {@link Phone#LTE_ON_CDMA_UNKNOWN}, {@link Phone#LTE_ON_CDMA_FALSE}
     * or {@link Phone#LTE_ON_CDMA_TRUE}
     */
    @Override
    public int getLteOnCdmaMode(String callingPackage) {
        return getLteOnCdmaModeForSubscriber(getDefaultSubscription(), callingPackage);
    }

    @Override
    public int getLteOnCdmaModeForSubscriber(int subId, String callingPackage) {
        if (!canReadPhoneState(callingPackage, "getLteOnCdmaModeForSubscriber")) {
            return PhoneConstants.LTE_ON_CDMA_UNKNOWN;
        }

        final Phone phone = getPhone(subId);
        if (phone == null) {
            return PhoneConstants.LTE_ON_CDMA_UNKNOWN;
        } else {
            return phone.getLteOnCdmaMode();
        }
    }

    public void setPhone(Phone phone) {
        mPhone = phone;
    }

    /**
     * {@hide}
     * Returns Default subId, 0 in the case of single standby.
     */
    private int getDefaultSubscription() {
        return mSubscriptionController.getDefaultSubId();
    }

    private int getPreferredVoiceSubscription() {
        return mSubscriptionController.getDefaultVoiceSubId();
    }

    /**
     * @see android.telephony.TelephonyManager.WifiCallingChoices
     */
    public int getWhenToMakeWifiCalls() {
        return Settings.System.getInt(mPhone.getContext().getContentResolver(),
                Settings.System.WHEN_TO_MAKE_WIFI_CALLS, getWhenToMakeWifiCallsDefaultPreference());
    }

    /**
     * @see android.telephony.TelephonyManager.WifiCallingChoices
     */
    public void setWhenToMakeWifiCalls(int preference) {
        if (DBG) log("setWhenToMakeWifiCallsStr, storing setting = " + preference);
        Settings.System.putInt(mPhone.getContext().getContentResolver(),
                Settings.System.WHEN_TO_MAKE_WIFI_CALLS, preference);
    }

    private static int getWhenToMakeWifiCallsDefaultPreference() {
        // TODO: Use a build property to choose this value.
        return TelephonyManager.WifiCallingChoices.ALWAYS_USE;
    }

    @Override
    public IccOpenLogicalChannelResponse iccOpenLogicalChannel(String AID) {
        enforceModifyPermissionOrCarrierPrivilege();
        // check if AID is valid
        if (AID == null || (AID != null && AID.equals(""))) {
            loge("AID is null or empty so return null immediately.");
            return null;
        }

        int slotId = SubscriptionManager.getSlotId(SubscriptionManager.getDefaultSubId());
        log("iccOpenLogicalChannel: " + AID + " default slot: " + slotId);
        IccOpenLogicalChannelResponse response = (IccOpenLogicalChannelResponse)sendRequest(
            CMD_OPEN_CHANNEL, AID, new Integer(slotId));
        log("iccOpenLogicalChannel: " + response);
        return response;
    }

    @Override
    public boolean iccCloseLogicalChannel(int channel) {
        enforceModifyPermissionOrCarrierPrivilege();

        int slotId = SubscriptionManager.getSlotId(SubscriptionManager.getDefaultSubId());
        if (DBG) log("iccCloseLogicalChannel: " + channel + " default slot: " + slotId);
        if (channel < 0) {
          return false;
        }
        Boolean success = (Boolean) sendRequest(CMD_CLOSE_CHANNEL, channel, new Integer(slotId));
        if (DBG) log("iccCloseLogicalChannel: " + success);
        return success;
    }

    @Override
    public String iccTransmitApduLogicalChannel(int channel, int cla,
            int command, int p1, int p2, int p3, String data) {
        enforceModifyPermissionOrCarrierPrivilege();

        int slotId = SubscriptionManager.getSlotId(SubscriptionManager.getDefaultSubId());
        if (DBG) {
            log("iccTransmitApduLogicalChannel: chnl=" + channel + " cla=" + cla +
                    " cmd=" + command + " p1=" + p1 + " p2=" + p2 + " p3=" + p3 +
                    " data=" + data + " default slot=" + slotId);
        }

        if (channel < 0) {
            return "";
        }

        IccIoResult response = (IccIoResult)sendRequest(CMD_TRANSMIT_APDU_LOGICAL_CHANNEL,
                new IccAPDUArgument(slotId, channel, cla, command, p1, p2, p3, data, null, null));
        if (DBG) log("iccTransmitApduLogicalChannel: " + response);

        // Append the returned status code to the end of the response payload.
        String s = Integer.toHexString(
                (response.sw1 << 8) + response.sw2 + 0x10000).substring(1);
        if (response.payload != null) {
            s = IccUtils.bytesToHexString(response.payload) + s;
        }
        return s;
    }

    @Override
    public String iccTransmitApduBasicChannel(int cla, int command, int p1, int p2,
                int p3, String data) {
        enforceModifyPermissionOrCarrierPrivilege();

        if (DBG) {
            log("iccTransmitApduBasicChannel: cla=" + cla + " cmd=" + command + " p1="
                    + p1 + " p2=" + p2 + " p3=" + p3 + " data=" + data);
        }

        IccIoResult response = (IccIoResult)sendRequest(CMD_TRANSMIT_APDU_BASIC_CHANNEL,
                new IccAPDUArgument(0, cla, command, p1, p2, p3, data));
        if (DBG) log("iccTransmitApduBasicChannel: " + response);

        // Append the returned status code to the end of the response payload.
        String s = Integer.toHexString(
                (response.sw1 << 8) + response.sw2 + 0x10000).substring(1);
        if (response.payload != null) {
            s = IccUtils.bytesToHexString(response.payload) + s;
        }
        return s;
    }

    @Override
    public byte[] iccExchangeSimIO(int fileID, int command, int p1, int p2, int p3,
            String filePath) {
        enforceModifyPermissionOrCarrierPrivilege();

        if (DBG) {
            log("Exchange SIM_IO " + fileID + ":" + command + " " +
                p1 + " " + p2 + " " + p3 + ":" + filePath);
        }

        IccIoResult response =
            (IccIoResult)sendRequest(CMD_EXCHANGE_SIM_IO,
                    new IccAPDUArgument(-1, fileID, command, p1, p2, p3, filePath));

        if (DBG) {
          log("Exchange SIM_IO [R]" + response);
        }

        byte[] result = null;
        int length = 2;
        if (response.payload != null) {
            length = 2 + response.payload.length;
            result = new byte[length];
            System.arraycopy(response.payload, 0, result, 0, response.payload.length);
        } else {
            result = new byte[length];
        }

        result[length - 1] = (byte) response.sw2;
        result[length - 2] = (byte) response.sw1;
        return result;
    }

    @Override
    public String sendEnvelopeWithStatus(String content) {
        enforceModifyPermissionOrCarrierPrivilege();

        IccIoResult response = (IccIoResult)sendRequest(CMD_SEND_ENVELOPE, content);
        if (response.payload == null) {
          return "";
        }

        // Append the returned status code to the end of the response payload.
        String s = Integer.toHexString(
                (response.sw1 << 8) + response.sw2 + 0x10000).substring(1);
        s = IccUtils.bytesToHexString(response.payload) + s;
        return s;
    }

    /**
     * Read one of the NV items defined in {@link com.android.internal.telephony.RadioNVItems}
     * and {@code ril_nv_items.h}. Used for device configuration by some CDMA operators.
     *
     * @param itemID the ID of the item to read
     * @return the NV item as a String, or null on error.
     */
    @Override
    public String nvReadItem(int itemID) {
        enforceModifyPermissionOrCarrierPrivilege();
        if (DBG) log("nvReadItem: item " + itemID);
        String value = (String) sendRequest(CMD_NV_READ_ITEM, itemID);
        if (DBG) log("nvReadItem: item " + itemID + " is \"" + value + '"');
        return value;
    }

    /**
     * Write one of the NV items defined in {@link com.android.internal.telephony.RadioNVItems}
     * and {@code ril_nv_items.h}. Used for device configuration by some CDMA operators.
     *
     * @param itemID the ID of the item to read
     * @param itemValue the value to write, as a String
     * @return true on success; false on any failure
     */
    @Override
    public boolean nvWriteItem(int itemID, String itemValue) {
        enforceModifyPermissionOrCarrierPrivilege();
        if (DBG) log("nvWriteItem: item " + itemID + " value \"" + itemValue + '"');
        Boolean success = (Boolean) sendRequest(CMD_NV_WRITE_ITEM,
                new Pair<Integer, String>(itemID, itemValue));
        if (DBG) log("nvWriteItem: item " + itemID + ' ' + (success ? "ok" : "fail"));
        return success;
    }

    /**
     * Update the CDMA Preferred Roaming List (PRL) in the radio NV storage.
     * Used for device configuration by some CDMA operators.
     *
     * @param preferredRoamingList byte array containing the new PRL
     * @return true on success; false on any failure
     */
    @Override
    public boolean nvWriteCdmaPrl(byte[] preferredRoamingList) {
        enforceModifyPermissionOrCarrierPrivilege();
        if (DBG) log("nvWriteCdmaPrl: value: " + HexDump.toHexString(preferredRoamingList));
        Boolean success = (Boolean) sendRequest(CMD_NV_WRITE_CDMA_PRL, preferredRoamingList);
        if (DBG) log("nvWriteCdmaPrl: " + (success ? "ok" : "fail"));
        return success;
    }

    /**
     * Perform the specified type of NV config reset.
     * Used for device configuration by some CDMA operators.
     *
     * @param resetType the type of reset to perform (1 == factory reset; 2 == NV-only reset)
     * @return true on success; false on any failure
     */
    @Override
    public boolean nvResetConfig(int resetType) {
        enforceModifyPermissionOrCarrierPrivilege();
        if (DBG) log("nvResetConfig: type " + resetType);
        Boolean success = (Boolean) sendRequest(CMD_NV_RESET_CONFIG, resetType);
        if (DBG) log("nvResetConfig: type " + resetType + ' ' + (success ? "ok" : "fail"));
        return success;
    }

    /**
     * {@hide}
     * Returns Default sim, 0 in the case of single standby.
     */
    public int getDefaultSim() {
        //TODO Need to get it from Telephony Devcontroller
        return 0;
    }

    public String[] getPcscfAddress(String apnType, String callingPackage) {
        if (!canReadPhoneState(callingPackage, "getPcscfAddress")) {
            return new String[0];
        }


        return mPhone.getPcscfAddress(apnType);
    }

    public void setImsRegistrationState(boolean registered) {
        enforceModifyPermission();
        mPhone.setImsRegistrationState(registered);
    }

    /**
     * Set the network selection mode to automatic.
     *
     */
    @Override
    public void setNetworkSelectionModeAutomatic(int subId) {
        enforceModifyPermissionOrCarrierPrivilege();
        if (DBG) log("setNetworkSelectionModeAutomatic: subId " + subId);
        sendRequest(CMD_SET_NETWORK_SELECTION_MODE_AUTOMATIC, null, subId);
    }

    /**
     * Set the network selection mode to manual with the selected carrier.
     */
    @Override
    public boolean setNetworkSelectionModeManual(int subId, OperatorInfo operator) {
        enforceModifyPermissionOrCarrierPrivilege();
        if (DBG) log("setNetworkSelectionModeManual: subId:" + subId + " operator:" + operator);
        return (Boolean) sendRequest(CMD_SET_NETWORK_SELECTION_MODE_MANUAL, operator, subId);
    }

    /**
     * Scans for available networks.
     */
    @Override
    public CellNetworkScanResult getCellNetworkScanResults(int subId) {
        enforceModifyPermissionOrCarrierPrivilege();
        if (DBG) log("getCellNetworkScanResults: subId " + subId);
        CellNetworkScanResult result = (CellNetworkScanResult) sendRequest(
                CMD_PERFORM_NETWORK_SCAN, null, subId);
        return result;
    }

    /**
     * Get the calculated preferred network type.
     * Used for debugging incorrect network type.
     *
     * @return the preferred network type, defined in RILConstants.java.
     */
    @Override
    public int getCalculatedPreferredNetworkType(String callingPackage) {
        if (!canReadPhoneState(callingPackage, "getCalculatedPreferredNetworkType")) {
            return RILConstants.PREFERRED_NETWORK_MODE;
        }

        return PhoneFactory.calculatePreferredNetworkType(mPhone.getContext(), 0); // wink FIXME: need to get SubId from somewhere.
    }

    /**
     * Get the preferred network type.
     * Used for device configuration by some CDMA operators.
     *
     * @return the preferred network type, defined in RILConstants.java.
     */
    @Override
    public int getPreferredNetworkType(int subId) {
        enforceModifyPermissionOrCarrierPrivilege();
        if (DBG) log("getPreferredNetworkType");
        int[] result = (int[]) sendRequest(CMD_GET_PREFERRED_NETWORK_TYPE, null, subId);
        int networkType = (result != null ? result[0] : -1);
        if (DBG) log("getPreferredNetworkType: " + networkType);
        return networkType;
    }

    /**
     * Set the preferred network type.
     * Used for device configuration by some CDMA operators.
     *
     * @param networkType the preferred network type, defined in RILConstants.java.
     * @return true on success; false on any failure.
     */
    @Override
    public boolean setPreferredNetworkType(int subId, int networkType) {
        enforceModifyPermissionOrCarrierPrivilege();
        if (DBG) log("setPreferredNetworkType: subId " + subId + " type " + networkType);
        Boolean success = (Boolean) sendRequest(CMD_SET_PREFERRED_NETWORK_TYPE, networkType, subId);
        if (DBG) log("setPreferredNetworkType: " + (success ? "ok" : "fail"));
        if (success) {
            Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                    Settings.Global.PREFERRED_NETWORK_MODE + subId, networkType);
        }
        return success;
    }

    /**
     * Check TETHER_DUN_REQUIRED and TETHER_DUN_APN settings, net.tethering.noprovisioning
     * SystemProperty, and config_tether_apndata to decide whether DUN APN is required for
     * tethering.
     *
     * @return 0: Not required. 1: required. 2: Not set.
     * @hide
     */
    @Override
    public int getTetherApnRequired() {
        enforceModifyPermissionOrCarrierPrivilege();
        int dunRequired = Settings.Global.getInt(mPhone.getContext().getContentResolver(),
                Settings.Global.TETHER_DUN_REQUIRED, 2);
        // If not set, check net.tethering.noprovisioning, TETHER_DUN_APN setting and
        // config_tether_apndata.
        if (dunRequired == 2 && mPhone.hasMatchedTetherApnSetting()) {
            dunRequired = 1;
        }
        return dunRequired;
    }

    /**
     * Set mobile data enabled
     * Used by the user through settings etc to turn on/off mobile data
     *
     * @param enable {@code true} turn turn data on, else {@code false}
     */
    @Override
    public void setDataEnabled(int subId, boolean enable) {
        enforceModifyPermission();

        if (MobileManagerUtils.isSupported()) {
            if (!checkMoMSPermission(SubPermissions.CHANGE_NETWORK_STATE_ON, null)) {
                return;
            }
        }

        int phoneId = mSubscriptionController.getPhoneId(subId);
        log("setDataEnabled: subId=" + subId + " phoneId=" + phoneId);
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone != null) {
            log("setDataEnabled: subId=" + subId + " enable=" + enable);
            phone.setDataEnabled(enable);
        } else {
            loge("setDataEnabled: no phone for subId=" + subId);
        }
    }

    /**
     * Get whether mobile data is enabled.
     *
     * Note that this used to be available from ConnectivityService, gated by
     * ACCESS_NETWORK_STATE permission, so this will accept either that or
     * our MODIFY_PHONE_STATE.
     *
     * @return {@code true} if data is enabled else {@code false}
     */
    @Override
    public boolean getDataEnabled(int subId) {
        try {
            mApp.enforceCallingOrSelfPermission(android.Manifest.permission.ACCESS_NETWORK_STATE,
                    null);
        } catch (Exception e) {
            mApp.enforceCallingOrSelfPermission(android.Manifest.permission.MODIFY_PHONE_STATE,
                    null);
        }
        int phoneId = mSubscriptionController.getPhoneId(subId);
        log("getDataEnabled: subId=" + subId + " phoneId=" + phoneId);
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone != null) {
            boolean retVal = phone.getDataEnabled();
            log("getDataEnabled: subId=" + subId + " retVal=" + retVal);
            return retVal;
        } else {
            loge("getDataEnabled: no phone subId=" + subId + " retVal=false");
            return false;
        }
    }

    @Override
    public int getCarrierPrivilegeStatus() {
        UiccCard card = UiccController.getInstance().getUiccCard(mPhone.getPhoneId());
        if (card == null) {
            loge("getCarrierPrivilegeStatus: No UICC");
            return TelephonyManager.CARRIER_PRIVILEGE_STATUS_RULES_NOT_LOADED;
        }
        return card.getCarrierPrivilegeStatusForCurrentTransaction(
                mPhone.getContext().getPackageManager());
    }

    @Override
    public int checkCarrierPrivilegesForPackage(String pkgName) {
        UiccCard card = UiccController.getInstance().getUiccCard(mPhone.getPhoneId());
        if (card == null) {
            loge("checkCarrierPrivilegesForPackage: No UICC");
            return TelephonyManager.CARRIER_PRIVILEGE_STATUS_RULES_NOT_LOADED;
        }
        return card.getCarrierPrivilegeStatus(mPhone.getContext().getPackageManager(), pkgName);
    }

    @Override
    public int checkCarrierPrivilegesForPackageAnyPhone(String pkgName) {
        int result = TelephonyManager.CARRIER_PRIVILEGE_STATUS_RULES_NOT_LOADED;
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            UiccCard card = UiccController.getInstance().getUiccCard(i);
            if (card == null) {
              // No UICC in that slot.
              continue;
            }

            result = card.getCarrierPrivilegeStatus(
                mPhone.getContext().getPackageManager(), pkgName);
            if (result == TelephonyManager.CARRIER_PRIVILEGE_STATUS_HAS_ACCESS) {
                break;
            }
        }

        return result;
    }

    @Override
    public List<String> getCarrierPackageNamesForIntentAndPhone(Intent intent, int phoneId) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            loge("phoneId " + phoneId + " is not valid.");
            return null;
        }
        UiccCard card = UiccController.getInstance().getUiccCard(phoneId);
        if (card == null) {
            loge("getCarrierPackageNamesForIntent: No UICC");
            return null ;
        }
        return card.getCarrierPackageNamesForIntent(
                mPhone.getContext().getPackageManager(), intent);
    }

    private String getIccId(int subId) {
        final Phone phone = getPhone(subId);
        UiccCard card = phone == null ? null : phone.getUiccCard();
        if (card == null) {
            loge("getIccId: No UICC");
            return null;
        }
        String iccId = card.getIccId();
        if (TextUtils.isEmpty(iccId)) {
            loge("getIccId: ICC ID is null or empty.");
            return null;
        }
        return iccId;
    }

    @Override
    public boolean setLine1NumberForDisplayForSubscriber(int subId, String alphaTag,
            String number) {
        enforceCarrierPrivilege();

        final String iccId = getIccId(subId);
        final Phone phone = getPhone(subId);
        if (phone == null) {
            return false;
        }
        final String subscriberId = phone.getSubscriberId();

        if (DBG_MERGE) {
            Slog.d(LOG_TAG, "Setting line number for ICC=" + iccId + ", subscriberId="
                    + subscriberId + " to " + number);
        }

        if (TextUtils.isEmpty(iccId)) {
        return false;
    }

        final SharedPreferences.Editor editor = mTelephonySharedPreferences.edit();

        final String alphaTagPrefKey = PREF_CARRIERS_ALPHATAG_PREFIX + iccId;
        if (alphaTag == null) {
            editor.remove(alphaTagPrefKey);
        } else {
            editor.putString(alphaTagPrefKey, alphaTag);
        }

        // Record both the line number and IMSI for this ICCID, since we need to
        // track all merged IMSIs based on line number
        final String numberPrefKey = PREF_CARRIERS_NUMBER_PREFIX + iccId;
        final String subscriberPrefKey = PREF_CARRIERS_SUBSCRIBER_PREFIX + iccId;
        if (number == null) {
            editor.remove(numberPrefKey);
            editor.remove(subscriberPrefKey);
        } else {
            editor.putString(numberPrefKey, number);
            editor.putString(subscriberPrefKey, subscriberId);
        }

        editor.commit();
        return true;
    }

    @Override
    public String getLine1NumberForDisplay(int subId, String callingPackage) {
        // This is open to apps with WRITE_SMS.
        if (!canReadPhoneNumber(callingPackage, "getLine1NumberForDisplay")) {
            return null;
        }

        String iccId = getIccId(subId);
        if (iccId != null) {
            String numberPrefKey = PREF_CARRIERS_NUMBER_PREFIX + iccId;
            return mTelephonySharedPreferences.getString(numberPrefKey, null);
        }
        return null;
    }

    @Override
    public String getLine1AlphaTagForDisplay(int subId, String callingPackage) {
        if (!canReadPhoneState(callingPackage, "getLine1AlphaTagForDisplay")) {
            return null;
        }

        String iccId = getIccId(subId);
        if (iccId != null) {
            String alphaTagPrefKey = PREF_CARRIERS_ALPHATAG_PREFIX + iccId;
            return mTelephonySharedPreferences.getString(alphaTagPrefKey, null);
        }
        return null;
    }

    @Override
    public String[] getMergedSubscriberIds(String callingPackage) {
        if (!canReadPhoneState(callingPackage, "getMergedSubscriberIds")) {
            return null;
        }
        final Context context = mPhone.getContext();
        final TelephonyManager tele = TelephonyManager.from(context);
        final SubscriptionManager sub = SubscriptionManager.from(context);

        // Figure out what subscribers are currently active
        final ArraySet<String> activeSubscriberIds = new ArraySet<>();
        // Clear calling identity, when calling TelephonyManager, because callerUid must be
        // the process, where TelephonyManager was instantiated. Otherwise AppOps check will fail.
        final long identity  = Binder.clearCallingIdentity();
        try {
            final int[] subIds = sub.getActiveSubscriptionIdList();
            for (int subId : subIds) {
                activeSubscriberIds.add(tele.getSubscriberId(subId));
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }

        // First pass, find a number override for an active subscriber
        String mergeNumber = null;
        final Map<String, ?> prefs = mTelephonySharedPreferences.getAll();
        for (String key : prefs.keySet()) {
            if (key.startsWith(PREF_CARRIERS_SUBSCRIBER_PREFIX)) {
                final String subscriberId = (String) prefs.get(key);
                if (activeSubscriberIds.contains(subscriberId)) {
                    final String iccId = key.substring(PREF_CARRIERS_SUBSCRIBER_PREFIX.length());
                    final String numberKey = PREF_CARRIERS_NUMBER_PREFIX + iccId;
                    mergeNumber = (String) prefs.get(numberKey);
                    if (DBG_MERGE) {
                        Slog.d(LOG_TAG, "Found line number " + mergeNumber
                                + " for active subscriber " + subscriberId);
                    }
                    if (!TextUtils.isEmpty(mergeNumber)) {
                        break;
                    }
                }
            }
        }

        // Shortcut when no active merged subscribers
        if (TextUtils.isEmpty(mergeNumber)) {
            return null;
        }

        // Second pass, find all subscribers under that line override
        final ArraySet<String> result = new ArraySet<>();
        for (String key : prefs.keySet()) {
            if (key.startsWith(PREF_CARRIERS_NUMBER_PREFIX)) {
                final String number = (String) prefs.get(key);
                if (mergeNumber.equals(number)) {
                    final String iccId = key.substring(PREF_CARRIERS_NUMBER_PREFIX.length());
                    final String subscriberKey = PREF_CARRIERS_SUBSCRIBER_PREFIX + iccId;
                    final String subscriberId = (String) prefs.get(subscriberKey);
                    if (!TextUtils.isEmpty(subscriberId)) {
                        result.add(subscriberId);
                    }
                }
            }
        }

        final String[] resultArray = result.toArray(new String[result.size()]);
        Arrays.sort(resultArray);
        if (DBG_MERGE) {
            Slog.d(LOG_TAG, "Found subscribers " + Arrays.toString(resultArray) + " after merge");
        }
        return resultArray;
    }

    @Override
    public boolean setOperatorBrandOverride(String brand) {
        enforceCarrierPrivilege();
        return mPhone.setOperatorBrandOverride(brand);
    }

    @Override
    public boolean setRoamingOverride(List<String> gsmRoamingList,
            List<String> gsmNonRoamingList, List<String> cdmaRoamingList,
            List<String> cdmaNonRoamingList) {
        enforceCarrierPrivilege();
        return mPhone.setRoamingOverride(gsmRoamingList, gsmNonRoamingList, cdmaRoamingList,
                cdmaNonRoamingList);
    }

    @Override
    public int invokeOemRilRequestRaw(byte[] oemReq, byte[] oemResp) {
        enforceModifyPermission();

        int returnValue = 0;
        try {
            AsyncResult result = (AsyncResult)sendRequest(CMD_INVOKE_OEM_RIL_REQUEST_RAW, oemReq);
            if(result.exception == null) {
                if (result.result != null) {
                    byte[] responseData = (byte[])(result.result);
                    if(responseData.length > oemResp.length) {
                        Log.w(LOG_TAG, "Buffer to copy response too small: Response length is " +
                                responseData.length +  "bytes. Buffer Size is " +
                                oemResp.length + "bytes.");
                    }
                    System.arraycopy(responseData, 0, oemResp, 0, responseData.length);
                    returnValue = responseData.length;
                }
            } else {
                CommandException ex = (CommandException) result.exception;
                returnValue = ex.getCommandError().ordinal();
                if(returnValue > 0) returnValue *= -1;
            }
        } catch (RuntimeException e) {
            Log.w(LOG_TAG, "sendOemRilRequestRaw: Runtime Exception");
            returnValue = (CommandException.Error.GENERIC_FAILURE.ordinal());
            if(returnValue > 0) returnValue *= -1;
        }

        return returnValue;
    }

    @Override
    public void setRadioCapability(RadioAccessFamily[] rafs) {
        try {
            ProxyController.getInstance().setRadioCapability(rafs);
        } catch (RuntimeException e) {
            Log.w(LOG_TAG, "setRadioCapability: Runtime Exception");
        }
    }

    @Override
    public int getRadioAccessFamily(int phoneId, String callingPackage) {
        if (!canReadPhoneState(callingPackage, "getRadioAccessFamily")) {
            return RadioAccessFamily.RAF_UNKNOWN;
        }

        return ProxyController.getInstance().getRadioAccessFamily(phoneId);
    }

    @Override
    public void enableVideoCalling(boolean enable) {
        enforceModifyPermission();
        SharedPreferences.Editor editor = mTelephonySharedPreferences.edit();
        editor.putBoolean(PREF_ENABLE_VIDEO_CALLING, enable);
        editor.commit();
    }

    @Override
    public boolean isVideoCallingEnabled(String callingPackage) {
        if (!canReadPhoneState(callingPackage, "isVideoCallingEnabled")) {
            return false;
        }

        // Check the user preference and the  system-level IMS setting. Even if the user has
        // enabled video calling, if IMS is disabled we aren't able to support video calling.
        // In the long run, we may instead need to check if there exists a connection service
        // which can support video calling.
        return ImsManager.isVtEnabledByPlatform(mPhone.getContext())
                && ImsManager.isEnhanced4gLteModeSettingEnabledByUser(mPhone.getContext())
                && mTelephonySharedPreferences.getBoolean(PREF_ENABLE_VIDEO_CALLING, true);
    }

    @Override
    public boolean canChangeDtmfToneLength() {
        return mApp.getCarrierConfig().getBoolean(CarrierConfigManager.KEY_DTMF_TYPE_ENABLED_BOOL);
    }

    @Override
    public boolean isWorldPhone() {
        return mApp.getCarrierConfig().getBoolean(CarrierConfigManager.KEY_WORLD_PHONE_BOOL);
    }

    @Override
    public boolean isTtyModeSupported() {
        TelecomManager telecomManager = TelecomManager.from(mPhone.getContext());
        TelephonyManager telephonyManager =
                (TelephonyManager) mPhone.getContext().getSystemService(Context.TELEPHONY_SERVICE);
        /// M: CC076: Remove Google's restriction: allow TTY for MSIM projects @{
        //return !telephonyManager.isMultiSimEnabled() && telecomManager.isTtySupported();
        return telecomManager.isTtySupported();
        /// @}
    }

    @Override
    public boolean isHearingAidCompatibilitySupported() {
        return mPhone.getContext().getResources().getBoolean(R.bool.hac_enabled);
    }

    /**
     * Returns the unique device ID of phone, for example, the IMEI for
     * GSM and the MEID for CDMA phones. Return null if device ID is not available.
     *
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#READ_PHONE_STATE READ_PHONE_STATE}
     */
    @Override
    public String getDeviceId(String callingPackage) {
        if (!canReadPhoneState(callingPackage, "getDeviceId")) {
            return null;
        }

        final Phone phone = PhoneFactory.getPhone(0);
        if (phone != null) {
            return phone.getDeviceId();
        } else {
            return null;
        }
    }

    /*
     * {@hide}
     * Returns the IMS Registration Status
     */
    @Override
    public boolean isImsRegistered() {
        return mPhone.isImsRegistered();
    }

    @Override
    public int getSubIdForPhoneAccount(PhoneAccount phoneAccount) {
        return PhoneUtils.getSubIdForPhoneAccount(phoneAccount);
    }

    /*
     * {@hide}
     * Returns the IMS Registration Status
     */
    public boolean isWifiCallingEnabled() {
        return mPhone.isWifiCallingEnabled();
    }

    /*
     * {@hide}
     * Returns the IMS Registration Status
     */
    public boolean isVolteEnabled() {
        return mPhone.isVolteEnabled();
    }

    private boolean canReadPhoneState(String callingPackage, String message) {
        try {
            mApp.enforceCallingOrSelfPermission(
                    android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE, message);

            // SKIP checking for run-time permission since caller or self has PRIVILEDGED permission
            return true;
        } catch (SecurityException e) {
            mApp.enforceCallingOrSelfPermission(android.Manifest.permission.READ_PHONE_STATE,
                    message);
        }

        if (mAppOps.noteOp(AppOpsManager.OP_READ_PHONE_STATE, Binder.getCallingUid(),
                callingPackage) != AppOpsManager.MODE_ALLOWED) {
            return false;
        }

        return true;
    }

    /**
     * Besides READ_PHONE_STATE, WRITE_SMS also allows apps to get phone numbers.
     */
    private boolean canReadPhoneNumber(String callingPackage, String message) {
        // Default SMS app can always read it.
        if (mAppOps.noteOp(AppOpsManager.OP_WRITE_SMS,
                Binder.getCallingUid(), callingPackage) == AppOpsManager.MODE_ALLOWED) {
            return true;
        }
        try {
            return canReadPhoneState(callingPackage, message);
        } catch (SecurityException e) {
            // Can be read with READ_SMS too.
            mApp.enforceCallingOrSelfPermission(android.Manifest.permission.READ_SMS, message);
            return mAppOps.noteOp(AppOpsManager.OP_READ_SMS,
                    Binder.getCallingUid(), callingPackage) == AppOpsManager.MODE_ALLOWED;
        }
    }

    @Override
    public void factoryReset(int subId) {
        enforceConnectivityInternalPermission();
        if (mUserManager.hasUserRestriction(UserManager.DISALLOW_NETWORK_RESET)) {
            return;
        }

        final long identity = Binder.clearCallingIdentity();
        try {
            if (SubscriptionManager.isUsableSubIdValue(subId) && !mUserManager.hasUserRestriction(
                    UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)) {
                // Enable data
                /// M: OP09 keep data switch status to ensure at most one data switch is on.
                if (!SystemProperties.get("ro.operator.optr").equals("OP09")) {
                    setDataEnabled(subId, true);
                }
                // Set network selection mode to automatic
                setNetworkSelectionModeAutomatic(subId);
                // Set preferred mobile network type to the best available
                setPreferredNetworkType(subId, Phone.PREFERRED_NT_MODE);
                // Turn off roaming
                SubscriptionManager.from(mApp).setDataRoaming(0, subId);
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    @Override
    public String getLocaleFromDefaultSim() {
        // We query all subscriptions instead of just the active ones, because
        // this might be called early on in the provisioning flow when the
        // subscriptions potentially aren't active yet.
        final List<SubscriptionInfo> slist = getAllSubscriptionInfoList();
        if (slist == null || slist.isEmpty()) {
            return null;
        }

        // This function may be called very early, say, from the setup wizard, at
        // which point we won't have a default subscription set. If that's the case
        // we just choose the first, which will be valid in "most cases".
        final int defaultSubId = getDefaultSubscription();
        SubscriptionInfo info = null;
        if (defaultSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            info = slist.get(0);
        } else {
            for (SubscriptionInfo item : slist) {
                if (item.getSubscriptionId() == defaultSubId) {
                    info = item;
                    break;
                }
            }

            if (info == null) {
                return null;
            }
        }

        // Try and fetch the locale from the carrier properties or from the SIM language
        // preferences (EF-PL and EF-LI)...
        final int mcc = info.getMcc();
        final Phone defaultPhone = getPhone(info.getSubscriptionId());
        String simLanguage = null;
        if (defaultPhone != null) {
            final Locale localeFromDefaultSim = defaultPhone.getLocaleFromSimAndCarrierPrefs();
            if (localeFromDefaultSim != null) {
                if (!localeFromDefaultSim.getCountry().isEmpty()) {
                    if (DBG) log("Using locale from default SIM:" + localeFromDefaultSim);
                    return localeFromDefaultSim.toLanguageTag();
                } else {
                    simLanguage = localeFromDefaultSim.getLanguage();
                }
            }
        }

        // The SIM language preferences only store a language (e.g. fr = French), not an
        // exact locale (e.g. fr_FR = French/France). So, if the locale returned from
        // the SIM and carrier preferences does not include a country we add the country
        // determined from the SIM MCC to provide an exact locale.
        final Locale mccLocale = MccTable.getLocaleFromMcc(mPhone.getContext(), mcc, simLanguage);
        if (mccLocale != null) {
            if (DBG) log("No locale from default SIM, using mcc locale:" + mccLocale);
            return mccLocale.toLanguageTag();
        }

        if (DBG) log("No locale found - returning null");
        return null;
    }

    private List<SubscriptionInfo> getAllSubscriptionInfoList() {
        final long identity = Binder.clearCallingIdentity();
        try {
            return mSubscriptionController.getAllSubInfoList(
                    mPhone.getContext().getOpPackageName());
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private List<SubscriptionInfo> getActiveSubscriptionInfoList() {
        final long identity = Binder.clearCallingIdentity();
        try {
            return mSubscriptionController.getActiveSubscriptionInfoList(
                    mPhone.getContext().getOpPackageName());
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /**
     * {@hide}
     * Returns the modem stats
     */
    @Override
    public ModemActivityInfo getModemActivityInfo() {
        return (ModemActivityInfo) sendRequest(CMD_GET_MODEM_ACTIVITY_INFO, null);
    }

    /// M: CC053: MoMS [Mobile Managerment] @{
    // 1. Permission Control for MO by 3rd APP
    private boolean checkMoMSPhonePermission(String number) {
        Bundle extraInfo = new Bundle();
        extraInfo.putString(IMobileManager.PARAMETER_PHONENUMBER, number);
        return checkMoMSPermission(SubPermissions.MAKE_CALL, extraInfo);
    }

    private boolean checkMoMSPermission(String subPermission, Bundle data) {
        try {
            int result = PackageManager.PERMISSION_GRANTED;
            IMobileManagerService mMobileManager;
            IBinder binder = ServiceManager.getService(Context.MOBILE_SERVICE);
            mMobileManager = IMobileManagerService.Stub.asInterface(binder);
            if (data != null) {
                result = mMobileManager.checkPermissionWithData(
                        subPermission, Binder.getCallingUid(), data);
            } else {
                result = mMobileManager.checkPermission(
                        subPermission, Binder.getCallingUid());
            }
            if (result != PackageManager.PERMISSION_GRANTED) {
                log("[Error]" + subPermission + " is not granted!!");
                return false;
            }
        } catch (Exception e) {
            log("[Error]Failed to chcek permission: " +  subPermission);
            return false;
        }
        return true;
    }
    /// @}

    // Added by M start
    private int getSubIdBySlot(int slot) {
        int [] subId = SubscriptionManager.getSubId(slot);
        if (subId == null || subId.length == 0) {
            return getDefaultSubscription();
        }
        if (DBG) log("getSubIdBySlot, simId " + slot + "subId " + subId[0]);
        return subId[0];
    }

    //@Override
    public String getIccAtr(int slotId) {
        if (DBG) log("> getIccAtr " + ",SimId = " + slotId);
        String response = (String) sendRequest(CMD_GET_ATR, new Integer(slotId));
        if (DBG) log("< getIccAtr: " + response);
        return response;
    }

    //@Override
    public byte[] iccOpenLogicalChannelWithSW(int slotId, String AID) {
        enforceModifyPermissionOrCarrierPrivilege();

        if (DBG) log("iccOpenLogicalChannelWithSW: " + AID + " slot " + slotId);
        IccIoResult response = (IccIoResult) sendRequest(CMD_OPEN_CHANNEL_WITH_SW,
                                                     AID, new Integer(slotId));

        byte[] result = null;
        int length = 2;

        // check if open logical channel fail
        if ((byte) response.sw1 == (byte) 0xff && (byte) response.sw2 == (byte) 0xff) {
            result = new byte[1];
            result[0] = (byte) 0x00;
            if (DBG) log("< iccOpenLogicalChannelWithSW 0");
            return result;
        }

        if (response.payload != null) {
            length = 2 + response.payload.length;
            result = new byte[length];
            System.arraycopy(response.payload, 0, result, 0, response.payload.length);
        } else {
            result = new byte[length];
        }
        result[length - 1] = (byte) response.sw2;
        result[length - 2] = (byte) response.sw1;

        String funLog = "";
        for (int i = 0 ; i < result.length; i++) {
            funLog = funLog + ((int) result[i] & 0xff) + " ";
        }
        if (DBG) log("< iccOpenLogicalChannelWithSW " + funLog);
        return result;
    }

    //@Override
    public byte[] iccExchangeSimIOUsingSlot(int slotId, int fileID, int command, int p1, int p2, int p3,
            String filePath) {
        enforceModifyPermissionOrCarrierPrivilege();

        if (DBG) log("Exchange SIM_IO slot " + slotId + " " + fileID + ":" + command + " " +
            p1 + " " + p2 + " " + p3 + ":" + filePath);

        IccIoResult response =
            (IccIoResult) sendRequest(CMD_EXCHANGE_SIM_IO,
                    new IccAPDUArgument(slotId, -1, fileID, command, p1, p2, p3, filePath));

        if (DBG) log("Exchange SIM_IO [R]" + response);

        byte[] result = null;
        int length = 2;
        if (response.payload != null) {
            length = 2 + response.payload.length;
            result = new byte[length];
            System.arraycopy(response.payload, 0, result, 0, response.payload.length);
        } else {
            result = new byte[length];
        }

        result[length - 1] = (byte) response.sw2;
        result[length - 2] = (byte) response.sw1;
        return result;
    }

    // MTK-START
    @Override
    /**
     * Returns the response APDU for a command APDU sent through SIM_IO.
     *
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#MODIFY_PHONE_STATE MODIFY_PHONE_STATE}
     * Or the calling app has carrier privileges. @see #hasCarrierPrivileges.
     *
     * @param slotId
     * @param family
     * @param fileID
     * @param filePath
     * @return The APDU response
     */
    public byte[] loadEFTransparent(int slotId, int family, int fileID, String filePath) {
        enforceModifyPermissionOrCarrierPrivilege();

        if (DBG) {
            log("loadEFTransparent slot " + slotId + " " + family + " " + fileID + ":" + filePath);
        }

        byte[] response =
                (byte[]) sendRequest(CMD_LOAD_EF_TRANSPARENT,
                new IccAPDUArgument(slotId, family, -1, fileID, COMMAND_READ_BINARY, 0, 0, 0,
                filePath));
        if (DBG) {
            log("loadEFTransparent " + response);
        }
        return response;
    }

    @Override
    /**
     * Returns the response APDU for a command APDU sent through SIM_IO.
     *
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#MODIFY_PHONE_STATE MODIFY_PHONE_STATE}
     * Or the calling app has carrier privileges. @see #hasCarrierPrivileges.
     *
     * @param slotId
     * @param family
     * @param fileID
     * @param filePath
     * @return The APDU response
     */
    public List<String> loadEFLinearFixedAll(int slotId, int family, int fileID,
            String filePath) {
        enforceModifyPermissionOrCarrierPrivilege();

        if (DBG) {
            log("loadEFLinearFixedAll slot " + slotId + " " + family + " " + fileID + ":"
                    + filePath);
        }
        ArrayList<byte[]> result =
                (ArrayList<byte[]>) sendRequest(CMD_LOAD_EF_LINEARFIXEDALL,
                new IccAPDUArgument(slotId, family, -1, fileID, COMMAND_READ_RECORD, 0, 0, 0,
                filePath));
        if (result == null) {
            return null;
        }
        List<String> response = new ArrayList<String>();
        for (int i = 0 ; i < result.size(); i++) {
            if (result.get(i) == null) {
                continue;
            }
            String res = new String(result.get(i));
            response.add(res);
        }
        if (DBG) {
            log("loadEFLinearFixedAll " + response);
        }
        return response;
    }
    // MTK-END

    public byte[] iccExchangeSimIOExUsingSlot(int slotId, int fileID, int command,
                                     int p1, int p2, int p3, String filePath, String data, String pin2) {
        int simId = slotId;

        if (DBG) log("Exchange SIM_IO Ex " + fileID + ":" + command + " " +
                 p1 + " " + p2 + " " + p3 + ":" + filePath + ", " + data + ", " + pin2 +
                 ", simId = " + simId);

        IccIoResult response =
                (IccIoResult) sendRequest(CMD_SIM_IO,
                        new IccAPDUArgument(simId, -1, fileID, command,
                        p1, p2, p3, data, filePath, pin2));

        if (DBG) log("Exchange SIM_IO Ex [R]" + response);
        byte[] result = null; int length = 2;
        if (response.payload != null) {
            length = 2 + response.payload.length;
            result = new byte[length];
            System.arraycopy(response.payload, 0, result, 0, response.payload.length);
        } else result = new byte[length];
        if (DBG) log("Exchange SIM_IO Ex [L] " + length);
        result[length - 1] = (byte) response.sw2;
        result[length - 2] = (byte) response.sw1;
        return result;
    }

    @Override
    public IccOpenLogicalChannelResponse iccOpenLogicalChannelUsingSlot(int slotId, String AID) {
        enforceModifyPermissionOrCarrierPrivilege();
        // check if AID is valid
        if (AID == null || (AID != null && AID.equals(""))) {
            loge("AID is null or empty so return null immediately.");
            return null;
        }

        if (DBG) log("iccOpenLogicalChannel: " + AID + " slot " + slotId);
        IccOpenLogicalChannelResponse response = (IccOpenLogicalChannelResponse) sendRequest(
                                   CMD_OPEN_CHANNEL, AID, new Integer(slotId));
        if (DBG) log("iccOpenLogicalChannel: " + response);
        return response;
    }

    @Override
    public boolean iccCloseLogicalChannelUsingSlot(int slotId, int channel) {
        enforceModifyPermissionOrCarrierPrivilege();

        if (DBG) log("iccCloseLogicalChannel: " + channel + " slot " + slotId);
        if (channel < 0) {
          return false;
        }
        Boolean success = (Boolean) sendRequest(CMD_CLOSE_CHANNEL, new Integer(channel), new Integer(slotId));
        if (DBG) log("iccCloseLogicalChannel: " + success);
        return success;
    }

    @Override
    public String iccTransmitApduLogicalChannelUsingSlot(int slotId, int channel, int cla,
            int command, int p1, int p2, int p3, String data) {
        enforceModifyPermissionOrCarrierPrivilege();

        if (DBG) log("iccTransmitApduLogicalChannelUsingSlot: chnl=" + channel + " cla=" + cla +
                " cmd=" + command + " p1=" + p1 + " p2=" + p2 + " p3=" + p3 +
                " data=" + data + " slot=" + slotId);

        if (channel < 0) {
            return "";
        }

        IccIoResult response = (IccIoResult) sendRequest(CMD_TRANSMIT_APDU_LOGICAL_CHANNEL,
                new IccAPDUArgument(slotId, channel, cla, command, p1, p2, p3, data, null, null));
        if (DBG) log("iccTransmitApduLogicalChannelUsingSlot: " + response);

        // If the payload is null, there was an error. Indicate that by returning
        // an empty string.
        if (response.payload == null) {
          return "";
        }

        // Append the returned status code to the end of the response payload.
        String s = Integer.toHexString(
                (response.sw1 << 8) + response.sw2 + 0x10000).substring(1);
        s = IccUtils.bytesToHexString(response.payload) + s;
        return s;
    }

    @Override
    public String iccTransmitApduBasicChannelUsingSlot(int slotId, int cla, int command, int p1, int p2,
                int p3, String data) {
        enforceModifyPermissionOrCarrierPrivilege();

        if (DBG) log("iccTransmitApduBasicChannelUsingSlot: cla=" + cla + " cmd=" + command +
                " p1=" + p1 + " p2=" + p2 + " p3=" + p3 + " data=" + data + " slot=" + slotId);

        IccIoResult response = (IccIoResult) sendRequest(CMD_TRANSMIT_APDU_BASIC_CHANNEL,
                new IccAPDUArgument(slotId, 0, cla, command, p1, p2, p3, data));
        if (DBG) log("iccTransmitApduBasicChannelUsingSlot: " + response);

        // If the payload is null, there was an error. Indicate that by returning
        // an empty string.
        if (response.payload == null) {
          return "";
        }

        // Append the returned status code to the end of the response payload.
        String s = Integer.toHexString(
                (response.sw1 << 8) + response.sw2 + 0x10000).substring(1);
        s = IccUtils.bytesToHexString(response.payload) + s;
        return s;
    }

    /**
     * Set data policy enabled with for sub
     * @hide
     */
    @Override
    public void setPolicyDataEnableForSubscriber(int subId, boolean enabled) {
        enforceModifyPermission();
        Phone phone = getPhone(subId);
        if (phone == null) {
            loge("setPolicyDataEnableForSubscriber incorrect parameter [subId=" + subId
                    + ", enable=" + enabled + "]");
        } else {
            PhoneBase phoneBase = (PhoneBase) (((PhoneProxy) phone).getActivePhone());
            DcTrackerBase dcTracker = phoneBase.mDcTracker;
            Message msg = dcTracker.obtainMessage(DctConstants.CMD_SET_POLICY_DATA_ENABLE);
            msg.arg1 = enabled ? DctConstants.ENABLED : DctConstants.DISABLED;
            dcTracker.sendMessage(msg);
        }
    }
    // Added by M end
}
