/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/**
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
import android.media.ToneGenerator;
/// M: CC020: [ALPS01808788] Not send POWER OFF error description to Telecom when modem reset @{
import android.provider.Settings;
/// @}
import android.telecom.DisconnectCause;

import com.android.phone.PhoneGlobals;
import com.android.phone.common.R;
import com.android.phone.ImsUtil;

import com.mediatek.phone.ext.ExtensionManager;


public class DisconnectCauseUtil {

   /**
    * Converts from a disconnect code in {@link android.telephony.DisconnectCause} into a more generic
    * {@link android.telecom.DisconnectCause}.object, possibly populated with a localized message
    * and tone.
    *
    * @param context The context.
    * @param telephonyDisconnectCause The code for the reason for the disconnect.
    */
    public static DisconnectCause toTelecomDisconnectCause(int telephonyDisconnectCause) {
        return toTelecomDisconnectCause(telephonyDisconnectCause, null /* reason */);
    }

   /**
    * Converts from a disconnect code in {@link android.telephony.DisconnectCause} into a more generic
    * {@link android.telecom.DisconnectCause}.object, possibly populated with a localized message
    * and tone.
    *
    * @param context The context.
    * @param telephonyDisconnectCause The code for the reason for the disconnect.
    * @param reason Description of the reason for the disconnect, not intended for the user to see..
    */
    public static DisconnectCause toTelecomDisconnectCause(
            int telephonyDisconnectCause, String reason) {
        Context context = PhoneGlobals.getInstance();
        /// M:OP01 change the disconnect cause from IMCOMING_REJECTED to MISSED @{
        telephonyDisconnectCause = ExtensionManager.getIncomingCallExt().changeDisconnectCause(
                telephonyDisconnectCause);
        /// @}
        return new DisconnectCause(
                toTelecomDisconnectCauseCode(telephonyDisconnectCause),
                toTelecomDisconnectCauseLabel(context, telephonyDisconnectCause),
                toTelecomDisconnectCauseDescription(context, telephonyDisconnectCause),
                toTelecomDisconnectReason(telephonyDisconnectCause, reason),
                toTelecomDisconnectCauseTone(telephonyDisconnectCause));
    }

    /**
     * Convert the {@link android.telephony.DisconnectCause} disconnect code into a
     * {@link android.telecom.DisconnectCause} disconnect code.
     * @return The disconnect code as defined in {@link android.telecom.DisconnectCause}.
     */
    private static int toTelecomDisconnectCauseCode(int telephonyDisconnectCause) {
        switch (telephonyDisconnectCause) {
            case android.telephony.DisconnectCause.LOCAL:
                return DisconnectCause.LOCAL;

            case android.telephony.DisconnectCause.NORMAL:
                return DisconnectCause.REMOTE;

            case android.telephony.DisconnectCause.OUTGOING_CANCELED:
            /// M: CC021: Error message due to CellConnMgr checking @{
            case android.telephony.DisconnectCause.OUTGOING_CANCELED_BY_SERVICE:
            /// @}
                return DisconnectCause.CANCELED;

            case android.telephony.DisconnectCause.INCOMING_MISSED:
                return DisconnectCause.MISSED;

            case android.telephony.DisconnectCause.INCOMING_REJECTED:
                return DisconnectCause.REJECTED;

            case android.telephony.DisconnectCause.BUSY:
                return DisconnectCause.BUSY;

            case android.telephony.DisconnectCause.CALL_BARRED:
            case android.telephony.DisconnectCause.CDMA_ACCESS_BLOCKED:
            case android.telephony.DisconnectCause.CDMA_NOT_EMERGENCY:
            case android.telephony.DisconnectCause.CS_RESTRICTED:
            case android.telephony.DisconnectCause.CS_RESTRICTED_EMERGENCY:
            case android.telephony.DisconnectCause.CS_RESTRICTED_NORMAL:
            case android.telephony.DisconnectCause.EMERGENCY_ONLY:
            case android.telephony.DisconnectCause.FDN_BLOCKED:
            case android.telephony.DisconnectCause.LIMIT_EXCEEDED:
                return DisconnectCause.RESTRICTED;

            case android.telephony.DisconnectCause.CDMA_ACCESS_FAILURE:
            case android.telephony.DisconnectCause.CDMA_ALREADY_ACTIVATED:
            case android.telephony.DisconnectCause.CDMA_CALL_LOST:
            case android.telephony.DisconnectCause.CDMA_DROP:
            case android.telephony.DisconnectCause.CDMA_INTERCEPT:
            case android.telephony.DisconnectCause.CDMA_LOCKED_UNTIL_POWER_CYCLE:
            case android.telephony.DisconnectCause.CDMA_PREEMPTED:
            case android.telephony.DisconnectCause.CDMA_REORDER:
            case android.telephony.DisconnectCause.CDMA_RETRY_ORDER:
            case android.telephony.DisconnectCause.CDMA_SO_REJECT:
            case android.telephony.DisconnectCause.CONGESTION:
            case android.telephony.DisconnectCause.ICC_ERROR:
            case android.telephony.DisconnectCause.INVALID_CREDENTIALS:
            case android.telephony.DisconnectCause.INVALID_NUMBER:
            case android.telephony.DisconnectCause.LOST_SIGNAL:
            case android.telephony.DisconnectCause.NO_PHONE_NUMBER_SUPPLIED:
            case android.telephony.DisconnectCause.NUMBER_UNREACHABLE:
            case android.telephony.DisconnectCause.OUTGOING_FAILURE:
            case android.telephony.DisconnectCause.OUT_OF_NETWORK:
            case android.telephony.DisconnectCause.OUT_OF_SERVICE:
            case android.telephony.DisconnectCause.POWER_OFF:
            case android.telephony.DisconnectCause.SERVER_ERROR:
            case android.telephony.DisconnectCause.SERVER_UNREACHABLE:
            case android.telephony.DisconnectCause.TIMED_OUT:
            case android.telephony.DisconnectCause.UNOBTAINABLE_NUMBER:
            case android.telephony.DisconnectCause.VOICEMAIL_NUMBER_MISSING:
            case android.telephony.DisconnectCause.ERROR_UNSPECIFIED:
            /// M: CC022: Error message due to VoLTE SS checking @{
            case android.telephony.DisconnectCause.VOLTE_SS_DATA_OFF:
            /// @}
                return DisconnectCause.ERROR;

            case android.telephony.DisconnectCause.DIALED_MMI:
            case android.telephony.DisconnectCause.EXITED_ECM:
            case android.telephony.DisconnectCause.MMI:
            case android.telephony.DisconnectCause.IMS_MERGED_SUCCESSFULLY:
                return DisconnectCause.OTHER;

            ///M: WFC @{
            case android.telephony.DisconnectCause.WFC_WIFI_SIGNAL_LOST:
            case android.telephony.DisconnectCause.WFC_ISP_PROBLEM:
            case android.telephony.DisconnectCause.WFC_HANDOVER_WIFI_FAIL:
            case android.telephony.DisconnectCause.WFC_HANDOVER_LTE_FAIL:
                  return DisconnectCause.WFC_CALL_ERROR;
            /// @}

            case android.telephony.DisconnectCause.NOT_VALID:
            case android.telephony.DisconnectCause.NOT_DISCONNECTED:
                return DisconnectCause.UNKNOWN;

            default:
                Log.w("DisconnectCauseUtil.toTelecomDisconnectCauseCode",
                        "Unrecognized Telephony DisconnectCause "
                        + telephonyDisconnectCause);
                return DisconnectCause.UNKNOWN;
        }
    }

    /**
     * Returns a label for to the disconnect cause to be shown to the user.
     */
    private static CharSequence toTelecomDisconnectCauseLabel(
            Context context, int telephonyDisconnectCause) {
        if (context == null ) {
            return "";
        }

        Integer resourceId = null;
        switch (telephonyDisconnectCause) {
            case android.telephony.DisconnectCause.BUSY:
                resourceId = R.string.callFailed_userBusy;
                break;

            case android.telephony.DisconnectCause.CONGESTION:
                resourceId = R.string.callFailed_congestion;
                break;

            case android.telephony.DisconnectCause.TIMED_OUT:
                resourceId = R.string.callFailed_timedOut;
                break;

            case android.telephony.DisconnectCause.SERVER_UNREACHABLE:
                resourceId = R.string.callFailed_server_unreachable;
                break;

            case android.telephony.DisconnectCause.NUMBER_UNREACHABLE:
                resourceId = R.string.callFailed_number_unreachable;
                break;

            case android.telephony.DisconnectCause.INVALID_CREDENTIALS:
                resourceId = R.string.callFailed_invalid_credentials;
                break;

            case android.telephony.DisconnectCause.SERVER_ERROR:
                resourceId = R.string.callFailed_server_error;
                break;

            case android.telephony.DisconnectCause.OUT_OF_NETWORK:
                resourceId = R.string.callFailed_out_of_network;
                break;

            case android.telephony.DisconnectCause.LOST_SIGNAL:
            case android.telephony.DisconnectCause.CDMA_DROP:
                resourceId = R.string.callFailed_noSignal;
                break;

            case android.telephony.DisconnectCause.LIMIT_EXCEEDED:
                resourceId = R.string.callFailed_limitExceeded;
                break;

            case android.telephony.DisconnectCause.POWER_OFF:
                resourceId = R.string.callFailed_powerOff;
                break;

            case android.telephony.DisconnectCause.ICC_ERROR:
                resourceId = R.string.callFailed_simError;
                break;

            case android.telephony.DisconnectCause.OUT_OF_SERVICE:
                resourceId = R.string.callFailed_outOfService;
                break;

            case android.telephony.DisconnectCause.INVALID_NUMBER:
            case android.telephony.DisconnectCause.UNOBTAINABLE_NUMBER:
                resourceId = R.string.callFailed_unobtainable_number;
                break;

            /// M: WFC @{
            case android.telephony.DisconnectCause.WFC_WIFI_SIGNAL_LOST:
                resourceId = R.string.wfc_wifi_call_drop;
                break;
            case android.telephony.DisconnectCause.WFC_ISP_PROBLEM:
                resourceId = R.string.wfc_internet_connection_lost;
                break;
            case android.telephony.DisconnectCause.WFC_HANDOVER_WIFI_FAIL:
                resourceId = R.string.wfc_wifi_call_drop;
                break;
            case android.telephony.DisconnectCause.WFC_HANDOVER_LTE_FAIL:
                resourceId = R.string.wfc_no_network;
                break;
            ///@}

            default:
                break;
        }
        return resourceId == null ? "" : context.getResources().getString(resourceId);
    }

    /**
     * Returns a description of the disconnect cause to be shown to the user.
     */
    private static CharSequence toTelecomDisconnectCauseDescription(
            Context context, int telephonyDisconnectCause) {
        if (context == null ) {
            return "";
        }

        Integer resourceId = null;
        switch (telephonyDisconnectCause) {
            case android.telephony.DisconnectCause.CALL_BARRED:
                resourceId = R.string.callFailed_cb_enabled;
                break;

            case android.telephony.DisconnectCause.CDMA_ALREADY_ACTIVATED:
                resourceId = R.string.callFailed_cdma_activation;
                break;

            case android.telephony.DisconnectCause.FDN_BLOCKED:
                resourceId = R.string.callFailed_fdn_only;
                break;

            case android.telephony.DisconnectCause.CS_RESTRICTED:
                resourceId = R.string.callFailed_dsac_restricted;
                break;

            case android.telephony.DisconnectCause.CS_RESTRICTED_EMERGENCY:
                resourceId = R.string.callFailed_dsac_restricted_emergency;
                break;

            case android.telephony.DisconnectCause.CS_RESTRICTED_NORMAL:
                resourceId = R.string.callFailed_dsac_restricted_normal;
                break;

            case android.telephony.DisconnectCause.OUTGOING_FAILURE:
                // We couldn't successfully place the call; there was some
                // failure in the telephony layer.
                // TODO: Need UI spec for this failure case; for now just
                // show a generic error.
                resourceId = R.string.incall_error_call_failed;
                break;

            case android.telephony.DisconnectCause.POWER_OFF:
                // Radio is explictly powered off because the device is in airplane mode.

                // TODO: Offer the option to turn the radio on, and automatically retry the call
                // once network registration is complete.

                if (ImsUtil.isWfcModeWifiOnly(context)) {
                    resourceId = R.string.incall_error_wfc_only_no_wireless_network;
                } else if (ImsUtil.isWfcEnabled(context)) {
                    resourceId = R.string.incall_error_power_off_wfc;
                } else {
                    /// M: CC020: Not send POWER OFF err description to Telecom when modem reset @{
                    // Avoid to send power off error description (UI show turn off
                    // airplane mdoe) to Telecom when modem reset because airplane mode
                    // is off(power on) in Setting actually.
                    if (Settings.Global.getInt(context.getContentResolver(),
                            Settings.Global.AIRPLANE_MODE_ON, 0) > 0) {
                        resourceId = R.string.incall_error_power_off;
                    }
                    //resourceId = R.string.incall_error_power_off;
                    /// @}
                }
                break;

            case android.telephony.DisconnectCause.EMERGENCY_ONLY:
                // Only emergency numbers are allowed, but we tried to dial
                // a non-emergency number.
                resourceId = R.string.incall_error_emergency_only;
                break;

            case android.telephony.DisconnectCause.OUT_OF_SERVICE:
                // No network connection.
                if (ImsUtil.isWfcModeWifiOnly(context)) {
                    resourceId = R.string.incall_error_wfc_only_no_wireless_network;
                } else if (ImsUtil.isWfcEnabled(context)) {
                    resourceId = R.string.incall_error_out_of_service_wfc;
                } else {
                    resourceId = R.string.incall_error_out_of_service;
                }
                break;

            case android.telephony.DisconnectCause.NO_PHONE_NUMBER_SUPPLIED:
                // The supplied Intent didn't contain a valid phone number.
                // (This is rare and should only ever happen with broken
                // 3rd-party apps.) For now just show a generic error.
                resourceId = R.string.incall_error_no_phone_number_supplied;
                break;

            case android.telephony.DisconnectCause.VOICEMAIL_NUMBER_MISSING:
                // TODO: Need to bring up the "Missing Voicemail Number" dialog, which
                // will ultimately take us to the Call Settings.
                resourceId = R.string.incall_error_missing_voicemail_number;
                break;

            /// M: CC022: Error message due to VoLTE SS checking @{
            case android.telephony.DisconnectCause.VOLTE_SS_DATA_OFF:
                resourceId = R.string.volte_ss_not_available_tips;
                break;
            /// @}
            /// M: ALPS02151583. UI doesn't show response when dialing an invalid number. @{
            case android.telephony.DisconnectCause.NUMBER_UNREACHABLE:
                resourceId = R.string.callFailed_number_unreachable;
                break;
            /// @}
            ///M: WFC @{
            case android.telephony.DisconnectCause.WFC_WIFI_SIGNAL_LOST:
                resourceId = R.string.wfc_wifi_call_drop_summary;
                break;
            case android.telephony.DisconnectCause.WFC_ISP_PROBLEM:
                resourceId = R.string.wfc_internet_lost_summary;
                break;
            case android.telephony.DisconnectCause.WFC_HANDOVER_WIFI_FAIL:
                resourceId = R.string.wfc_wifi_handover_fail;
                break;
            case android.telephony.DisconnectCause.WFC_HANDOVER_LTE_FAIL:
                resourceId = R.string.wfc_wifi_lte_handover_fail;
                break;
            /// @}
            /// M: CC021: Error message due to CellConnMgr checking @{
            case android.telephony.DisconnectCause.OUTGOING_CANCELED_BY_SERVICE:
            /// @}
            case android.telephony.DisconnectCause.OUTGOING_CANCELED:
                // We don't want to show any dialog for the canceled case since the call was
                // either canceled by the user explicitly (end-call button pushed immediately)
                // or some other app canceled the call and immediately issued a new CALL to
                // replace it.
            default:
                break;
        }
        return resourceId == null ? "" : context.getResources().getString(resourceId);
    }

    private static String toTelecomDisconnectReason(int telephonyDisconnectCause, String reason) {
        String causeAsString = android.telephony.DisconnectCause.toString(telephonyDisconnectCause);
        if (reason == null) {
            return causeAsString;
        } else {
            return reason + ", " + causeAsString;
        }
    }

    /**
     * Returns the tone to play for the disconnect cause, or UNKNOWN if none should be played.
     */
    private static int toTelecomDisconnectCauseTone(int telephonyDisconnectCause) {
        switch (telephonyDisconnectCause) {
            case android.telephony.DisconnectCause.BUSY:
                return ToneGenerator.TONE_SUP_BUSY;

            case android.telephony.DisconnectCause.CONGESTION:
                return ToneGenerator.TONE_SUP_CONGESTION;

            case android.telephony.DisconnectCause.CDMA_REORDER:
                return ToneGenerator.TONE_CDMA_REORDER;

            case android.telephony.DisconnectCause.CDMA_INTERCEPT:
                return ToneGenerator.TONE_CDMA_ABBR_INTERCEPT;

            case android.telephony.DisconnectCause.CDMA_DROP:
            case android.telephony.DisconnectCause.OUT_OF_SERVICE:
                return ToneGenerator.TONE_CDMA_CALLDROP_LITE;

            case android.telephony.DisconnectCause.UNOBTAINABLE_NUMBER:
                return ToneGenerator.TONE_SUP_ERROR;

            case android.telephony.DisconnectCause.ERROR_UNSPECIFIED:
            case android.telephony.DisconnectCause.LOCAL:
            case android.telephony.DisconnectCause.NORMAL:
                return ToneGenerator.TONE_PROP_PROMPT;

            case android.telephony.DisconnectCause.IMS_MERGED_SUCCESSFULLY:
                // Do not play any tones if disconnected because of a successful merge.
            default:
                return ToneGenerator.TONE_UNKNOWN;
        }
    }
}
