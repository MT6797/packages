/*
 * This class used to add log for log parser.
 * There are three types of log :
 *      OP - operations from InCallUI;
 *      NOTIFY - notify from Telephony;
 *      DUMP - dump certain information when call was got updated;
 * Pattern:
 *      OP -        [Debug][CC][Module][OP][Action][Phone Number][local-call-id] Msg.String
 *      NOTIFY -    [Debug][CC][Module][Notify][Action][Phone Number][local-call-id] Msg.String
 *      DUMP -      [Debug][CC][Module][Dump][Phone Number][local-call-id] Msg.String: [name:value]
 * Examples:
 *      OP -        [Debug][CC][Telecom][OP][Hold][10010][InCall@3] extra inforamtion
 *      NOTIFY -    [Debug][CC][Telecom][Notify][Onhold][10010][ConnectionService@4] Msg.String
 *      DUMP -      [Debug][CC][Telecom][Dump][10010][ConnectionService@4]-[state:Onhold]
 *                                            [inConfCall:0][isInConfCall:0]-
 */
package com.mediatek.telecom;

import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.server.telecom.Call;
import com.android.server.telecom.CallState;
import com.android.server.telecom.Log;
import com.mediatek.telecom.volte.TelecomVolteUtils;

public class LogUtils {

    public static final int DBG_LEVEL = 3;
    private static final boolean DBG =
            (DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);

    public static final String LOG_TAG = "Telecom-LogUtils";
    public static final String LOG_PREFIX_OP = "[Debug][CC][Telecom][OP]";
    public static final String LOG_PREFIX_NOTIFY = "[Debug][CC][Telecom][Notify]";
    public static final String LOG_PREFIX_DUMP = "[Debug][CC][Telecom][Dump]";

    // below lists all operate action
    public static final String OP_ACTION_DIAL = "Dial";
    public static final String OP_ACTION_HOLD = "Hold";
    public static final String OP_ACTION_UNHOLD = "Unhold";
    public static final String OP_ACTION_HANGUP = "Hangup";
    public static final String OP_ACTION_ANSWER = "Answer";
    public static final String OP_ACTION_REJECT = "Reject";
    public static final String OP_ACTION_CONFERENCE = "Conference";
    public static final String OP_ACTION_ADD_MEMBER = "AddMember";
    public static final String OP_ACTION_REMOVE_MEMBER = "RemoveMember";
    public static final String OP_ACTION_DIAL_CONF = "DialConf";
    public static final String OP_ACTION_SWAP = "Swap";
    public static final String OP_ACTION_SPLIT = "Split";

    // below lists all notify action
    public static final String NOTIFY_ACTION_CREATE_MO_SUCCESS = "CreateMoSuccess";
    public static final String NOTIFY_ACTION_CREATE_MT_SUCCESS = "CreateMtSuccess";
    public static final String NOTIFY_ACTION_MT = "MT";
    public static final String NOTIFY_ACTION_ALERTING = "Alerting";
    public static final String NOTIFY_ACTION_ACTIVE = "Active";
    public static final String NOTIFY_ACTION_ONHOLD = "Onhold";
    public static final String NOTIFY_ACTION_DISCONNECT = "Disconnected";
    public static final String NOTIFY_ACTION_CONF_CREATED = "ConfCreated";
    public static final String NOTIFY_ACTION_NEW_CALL_ADDED = "NewCallAdded";

    // below lists all values that we interest when dump
    public static final String DUMP_KEY_STATE = "state";
    public static final String DUMP_KEY_IS_CONFCALL = "isConfCall";
    public static final String DUMP_KEY_IS_IN_CONFCALL = "isInConfCall";

    //
    public static final String NUMBER_CONF_CALL = "ConferenceCall";

    public static void logCcOp(String number, String action, String callId, String extraMsg) {
        if (!DBG) {
            return;
        }
        String numberPart = "[" + number + "]";
        String actionPart = "[" + action + "]";
        String callIdPart = "[" + callId + "]";
        String extraMsgPart = "" + extraMsg;
        Log.d(LOG_TAG, LOG_PREFIX_OP + actionPart + numberPart + callIdPart + extraMsgPart);
    }

    public static void logCcOp(Call call, String action, String callId, String extraMsg) {
        if (!DBG) {
            return;
        }
        if (call == null) {
            return;
        }
        String numberPart = "";
        String actionPart = "" + action;
        String callIdPart = "" + callId;
        String extraMsgPart = "" + extraMsg;
        // handle number part
        if (isConferenceEx(call)) {
            numberPart = NUMBER_CONF_CALL;
        } else {
            numberPart = getNumberEx(call);
        }
        // handle action part
        if (OP_ACTION_HANGUP.equals(action) && isInConferenceEx(call)) {
            action = OP_ACTION_REMOVE_MEMBER;
        }

        logCcOp(numberPart, actionPart, callIdPart, extraMsgPart);
    }

    public static void logCcNotify(String number, String action, String callId, String extraMsg) {
        if (!DBG) {
            return;
        }
        String numberPart = "[" + number + "]";
        String actionPart = "[" + action + "]";
        String callIdPart = "[" + callId + "]";
        String extraMsgPart = "" + extraMsg;
        Log.d(LOG_TAG, LOG_PREFIX_NOTIFY + actionPart + numberPart + callIdPart + extraMsgPart);
    }

    public static void logCcNotify(Call call, String action, String callId, String extraMsg) {
        if (!DBG) {
            return;
        }
        if (call == null) {
            return;
        }
        String numberPart = "";
        String actionPart = "" + action;
        String callIdPart = "" + callId;
        String extraMsgPart = "" + extraMsg;
        // handle number part
        if (isConferenceEx(call)) {
            numberPart = NUMBER_CONF_CALL;
        } else {
            numberPart = getNumberEx(call);
        }
        logCcNotify(numberPart, actionPart, callIdPart, extraMsgPart);
    }

    // Here we handle three cases: OP-Dial; OP-DialConf; NOTIFY-MT
    public static void logIntent(Intent intent) {
        if (!DBG) {
            return;
        }
        if (intent == null) {
            return;
        }
        String action = "";
        String number = "";
        String callId = "";
        String extraMsg = "";
        String intentAction = intent.getAction();
        if (TelecomManager.ACTION_INCOMING_CALL.equals(intentAction)) {
            action = NOTIFY_ACTION_MT;
            Bundle clientExtras = null;
            if (intent.hasExtra(TelecomManager.EXTRA_INCOMING_CALL_EXTRAS)) {
                clientExtras = intent.getBundleExtra(TelecomManager.EXTRA_INCOMING_CALL_EXTRAS);
            }
            if (clientExtras != null) {
                Uri handle = clientExtras.getParcelable(TelephonyManager.EXTRA_INCOMING_NUMBER);
                if (handle != null) {
                    number = handle.getSchemeSpecificPart();
                }
                boolean isConfInvite = clientExtras.getBoolean(
                        TelecomManagerEx.EXTRA_VOLTE_CONF_CALL_INCOMING, false);
                extraMsg = "is confernce invite: " + isConfInvite;
            }
            logCcNotify(number, action, callId, extraMsg);
        } else if (Intent.ACTION_CALL.equals(intentAction) ||
                Intent.ACTION_CALL_PRIVILEGED.equals(intentAction) ||
                Intent.ACTION_CALL_EMERGENCY.equals(intentAction)) {
            if (TelecomVolteUtils.isConferenceDialRequest(intent)) {
                action = OP_ACTION_DIAL_CONF;
                number = NUMBER_CONF_CALL;
                extraMsg += numbersToString(TelecomVolteUtils.getConferenceDialNumbers(intent));
            } else {
                action = OP_ACTION_DIAL;
                Uri handle = intent.getData();
                if (handle != null) {
                    number = handle.getSchemeSpecificPart();
                }
                if (!TextUtils.isEmpty(number)) {
                    boolean isUriNumber = PhoneNumberUtils.isUriNumber(number);
                    if (!isUriNumber) {
                        number = PhoneNumberUtils.convertKeypadLettersToDigits(number);
                        number = PhoneNumberUtils.stripSeparators(number);
                    }
                }
            }
            logCcOp(number, action, callId, extraMsg);
        }
    }

    public static String numbersToString(List<String> numbers) {
        String result = "numbers: ";
        if (numbers != null) {
            for (String number : numbers) {
                result = result + number + " / ";
            }
        }
        return result;
    }

    public static void logCcDump(String number, String callId, String extraMsg) {
        if (!DBG) {
            return;
        }
        String numberPart = "[" + number + "]";
        String callIdPart = "[" + callId + "]";
        String extraMsgPart = "" + extraMsg;
        Log.d(LOG_TAG, LOG_PREFIX_DUMP + numberPart + callIdPart + extraMsgPart);
    }

    private static final int INT_FOR_BOOLEAN_TRUE = 1;
    private static final int INT_FOR_BOOLEAN_FALSE = 0;

    public static void logCcDump(Call call, String extraMsg) {
        if (!DBG) {
            return;
        }
        if (call == null) {
            return;
        }
        String numberPart = "" + getNumberEx(call);
        String callIdPart = "" + call.getCallIdEx();
        String callState = "" + CallState.toString(getStateEx(call));
        callState = callState.toLowerCase();
        String extraMsgPart = "-";
        extraMsgPart += "[" + DUMP_KEY_STATE + ":" + callState + "]";
        boolean isConfCall = isConferenceEx(call);
        boolean isInConfCall = isInConferenceEx(call);
        extraMsgPart += "[" + DUMP_KEY_IS_CONFCALL + ":"
                + (isConfCall ? INT_FOR_BOOLEAN_TRUE : INT_FOR_BOOLEAN_FALSE) + "]";
        extraMsgPart += "[" + DUMP_KEY_IS_IN_CONFCALL + ":"
                + (isInConfCall ? INT_FOR_BOOLEAN_TRUE : INT_FOR_BOOLEAN_FALSE) + "]";
        extraMsgPart += extraMsg + "-";
        logCcDump(numberPart, callIdPart, extraMsgPart);
        //  may be we can add information of parent id and children id into extraMsg.
    }

    /// M:  below functions are wrapper for Call.java @{
    // We changed those functions' type from "packages" to be "public",
    // we keep the usage of those modified functions only here.
    private static boolean isConferenceEx(Call call) {
        boolean result = false;
        if (call != null) {
            result = call.isConference();
        }
        return result;
    }

    private static final int CALL_STATE_UNKNOWN = 101;
    // here use an value that CallState.java do not define,
    // so if call == null, we can log the call's state as "UNKNOWN".
    private static int getStateEx(Call call) {
        int result = CALL_STATE_UNKNOWN;
        if (call != null) {
            result = call.getState();
        }
        return result;
    }

    private static String getNumberEx(Call call) {
        String number = "";
        if (call != null && call.getHandle() != null) {
            Uri handle = call.getHandle();
            number = handle.getSchemeSpecificPart();
        }
        return number;
    }

    private static boolean isInConferenceEx(Call call) {
        boolean result = false;
        if (call != null && call.getParentCall() != null) {
            result = true;
        }
        return result;
    }
    /// @}
}
