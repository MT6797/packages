package com.mediatek.settings.cdg;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.phone.PhoneUtils;
import com.mediatek.telephony.TelephonyManagerEx;

/**
 * This class is the util class for CDG (CDMA Develop Group)
 * OMH (Open Market Handset) project of call setting parts.
 */
public class CdgUtils {

    private static final String LOG_TAG = "CallSettings/CdgUtils";

    /// This order is defined in the spec of 3GPP2 (Reasons)
    public static final int CF_ALL = 1;
    public static final int CF_BUSY = 2;
    public static final int CF_DEFAULT = 3;
    public static final int CF_NOT_ANSWER = 4;
    public static final int CF_ALWAYS = 5;
    /// This order is defined by planner (Options)
    public static final int CF_TO_NUMBER = 1;
    public static final int CF_TO_VOICE_MAIL = 2;
    public static final int CF_STOP = 3;
    public static final int UNDEFINED_FC = -1;
    /// We should add a * before the feature code and dial out.
    public static final String SS_PROFIX = "*";

    /**
     * Whether the SIM card support CDG OMH or not.
     * @return
     */
    public static boolean isCdgOmhSimCard(int subId) {
        boolean isOmhEnable = TelephonyManagerEx.getDefault().isOmhEnable(subId);
        boolean isOmhCard = TelephonyManagerEx.getDefault().isOmhCard(subId);
        log("isCdgOmhSimCard isOmhEnable=" + isOmhEnable +"; isOmhCard=" + isOmhCard);
        return isOmhEnable && isOmhCard;
    }

    /**
     * This function get the status of whether the SIM card
     * support Call Forwarding or not. If it support one of the
     * four kinds forwarding(always, busy, unanswered, default),
     * it should return true, else return false.
     * @return
     */
    public static boolean isSupportCallForwarding(int subId) {
        /// cffc means Call forwarding feature code.
        int[] cffc = TelephonyManagerEx.getDefault().getCallForwardingFc(
                CF_ALL, subId);

        if (cffc != null) {
            if (!isAllFcUndefined(cffc)) {
                log("isSupportCallForwarding result = true");
                return true;
            }
        }
        log("isSupportCallForwarding result = false");
        return false;
    }

    /**
     *
     * @param subId
     * @param reason
     * @param option
     * @return
     */
    public static String getCallForwardingFc (int subId, int reason, int option) {
        log("getCallForwardingFc subId=" + subId +
                ";reason=" + reason + ";option=" + option);
        String result = "";

        if (reason < CF_BUSY || reason > CF_ALWAYS
                || option < CF_TO_NUMBER || option > CF_STOP) {
            log("getCallForwardingFc error ");
            return result;
        }
        int[] cffc = TelephonyManagerEx.getDefault().getCallForwardingFc(
                reason, subId);

        /// cffc will spec of 3Gpp2 have five numbers:
        /// 1, register; 2, to voice mail;
        /// 3, deRegister; 4, active; 5, deActive.
        /// But UX spec only have 3 values.
        /// 1,to phone number; 2,to voice mail; 3, stop.
        /// So UX spec's 1 to 3GPP2's 1/4; UX spec's 2 to 3GPP2's 2;
        ///    UX spec's  to 3GPP2's 3/5;
        if (cffc != null) {
            if (CF_TO_NUMBER == option) {
                if (UNDEFINED_FC != cffc[0]) {
                    result = Integer.toString(cffc[0]);
                } else if (UNDEFINED_FC != cffc[3]) {
                    result = Integer.toString(cffc[3]);
                }
            } else if (CF_TO_VOICE_MAIL == option) {
                if (UNDEFINED_FC != cffc[1]) {
                    result = Integer.toString(cffc[1]);
                }
            } else if (CF_STOP == option) {
                if (UNDEFINED_FC != cffc[2]) {
                    result = Integer.toString(cffc[2]);
                } else if (UNDEFINED_FC != cffc[4]) {
                    result = Integer.toString(cffc[4]);
                }
            }
        }
        log("getCallForwardingFc result = " + result);
        return result;
    }

    /**
     * Get the status of whether the SIM card support Call Waiting or not.
     * @return
     */
    public static int[] getCallWaitingFc(int subId) {
        return TelephonyManagerEx.getDefault().getCallWaitingFc(subId);
    }

    public static boolean isSupportCallWaiting(int subId) {
        int[] cwfc = getCallWaitingFc(subId);
        if (cwfc != null) {
            if (!isAllFcUndefined(cwfc)) {
                log("isSupportCallWaiting result = true");
                return true;
            }
        }
        log("isSupportCallWaiting result = false");
        return false;
    }

    /**
     * Get the do not disturb feature code.
     * @return
     */
    public static int[] getDoNotDisturbFc(int subId) {
        return TelephonyManagerEx.getDefault().getDonotDisturbFc(subId);
    }

    public static boolean isSupportDoNotDisturb(int subId) {
        int[] dndfc = getDoNotDisturbFc(subId);
        if (dndfc != null) {
            if (!isAllFcUndefined(dndfc)) {
                log("isSupportDoNotDisturb result = true");
                return true;
            }
        }
        log("isSupportDoNotDisturb result = false");
        return false;
    }

    /**
     * Get voice message retrieve feature code.
     * @return
     */
    public static int[] getVoiceMessageRetrieveFc(int subId) {
        return TelephonyManagerEx.getDefault().getVMRetrieveFc(subId);
    }

    public static boolean isSupportVoiceMessageRetrieve(int subId) {
        int[] vmrfc = getVoiceMessageRetrieveFc(subId);
        if (vmrfc != null) {
            if (!isAllFcUndefined(vmrfc)) {
                log("isSupportVoiceMessageRetrieve result = true");
                return true;
            }
        }
        log("isSupportVoiceMessageRetrieve result = false");
        return false;
    }

    /**
     * This function is for enable/disable ss function through
     * dial out a MMI (Feature code) code.
     * @param context
     * @param subId
     * @param fc the string want to dial out.
     */
    public static void dialOutSsCode(Context context, int subId, String fc) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            log("dialOutSsCode invalid subid " + subId);
            return;
        }
        log("dialOutSsCode fc=" + fc +";subId=" + subId);
        Intent intent = new Intent(Intent.ACTION_CALL);
        int phoneId = SubscriptionManager.getPhoneId(subId);
        PhoneAccountHandle phoneAccountHandle =
            PhoneUtils.makePstnPhoneAccountHandle(phoneId);

        intent.setData(Uri.parse("tel:" + SS_PROFIX + fc));
        intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle);
        context.startActivity(intent);
    }

    /**
     * Whether call feature code is undefined,
     * @param fc should be not null!!
     * @return
     */
    private static boolean isAllFcUndefined(int[] fc) {
        boolean result = true;
        for (int index = 0; index < fc.length; index ++) {
            if (UNDEFINED_FC != fc[index]) {
                result = false;
            }
        }
        log("isAllUndefined result = " + result);
        return result;
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
