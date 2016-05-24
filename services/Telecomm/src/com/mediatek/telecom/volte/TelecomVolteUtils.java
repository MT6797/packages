package com.mediatek.telecom.volte;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.server.telecom.Log;
import com.android.server.telecom.R;
import com.android.server.telecom.TelecomSystem;
import com.android.server.telecom.TelephonyUtil;
import com.android.server.telecom.components.ErrorDialogActivity;
import com.mediatek.telecom.TelecomManagerEx;

public class TelecomVolteUtils {

    private static final String LOG_TAG = "TelecomVolteUtils";

    /**
     * In the past, Contacts will carry this extra to indicate the dial request is for Ims only.
     * But it will cause normal number(like 10010) cannot be dialed out without VoLTE registered.
     * So we change to use uri number(contain "@") with "tel:" scheme to indicate Ims only.
     */
    public static final String EXTRA_VOLTE_IMS_CALL_OLD = "com.mediatek.phone.extra.ims";
    /**
     * This is used to record that the dial request is Ims only request,
     * pass this info from CallIntentProcessor to CallsManager(intent -> Bundle)
     */
    public static final String EXTRA_VOLTE_IMS_CALL = "com.mediatek.telecom.extra.ims";
    public static final String ACTION_IMS_SETTING = "android.settings.WIRELESS_SETTINGS";

    public static final boolean MTK_IMS_SUPPORT = SystemProperties.get("ro.mtk_ims_support")
            .equals("1");
    public static final boolean MTK_VOLTE_SUPPORT = SystemProperties.get("ro.mtk_volte_support")
            .equals("1");

    public static boolean isVolteSupport() {
        return MTK_IMS_SUPPORT && MTK_VOLTE_SUPPORT;
    }

//----------------------------For volte ims call only------------------------------------
    public static boolean isImsCallOnlyRequest(Object obj) {
        boolean result = false;
        if (isVolteSupport() && obj != null) {
            if (obj instanceof Bundle) {
                Bundle bundle = (Bundle) obj;
                result = bundle.getBoolean(EXTRA_VOLTE_IMS_CALL, false);
            } else if (obj instanceof Intent) { // use uri to check instead of extra for now.
                Intent intent = (Intent) obj;
                Uri handle = intent.getData();
                if (handle != null) {
                    String scheme = handle.getScheme();
                    String uriString = handle.getSchemeSpecificPart();
                    if (PhoneAccount.SCHEME_TEL.equals(scheme)
                            && PhoneNumberUtils.isUriNumber(uriString)) {
                        log("isImsCallOnlyRequest()...Ims call request and set intent.");
                        result = true;
                        intent.putExtra(EXTRA_VOLTE_IMS_CALL, true);
                    }
                }
            } else {
                log("isImsCallOnlyRequest()...unexpected obj: " + obj);
            }
        }
        return result;
    }

    /**
     * re-get handle uri from intent.
     * For Ims only(tel:xxx@xx), will be changed to sip:xxx@xx in some judge, then we re-get it.
     * @param intent
     * @param defaultHandle
     * @return
     */
    public static Uri getHandleFromIntent(Intent intent, Uri defaultHandle) {
        Uri handle = defaultHandle;
        if (intent != null) {
            handle = intent.getData();
        }
        if (handle == null) {
            log("getHandleFromIntent()... handle is null, need check!");
        }
        return handle;
    }

    public static boolean isImsEnabled(Context context) {
        boolean isImsEnabled = (1 == Settings.Global.getInt(
                context.getContentResolver(), Settings.Global.ENHANCED_4G_MODE_ENABLED, 0));
        return isImsEnabled;
    }

    public static void showImsDisableDialog(Context context) {
        final Intent intent = new Intent(context, ErrorDialogActivity.class);
        intent.putExtra(ErrorDialogActivity.SHOW_IMS_DISABLE_DIALOG_EXTRA, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivityAsUser(intent, UserHandle.CURRENT);
    }

    public static void showNoImsAccountDialog(Context context) {
        // for now, we use "Call not sent."
        final Intent errorIntent = new Intent(context, ErrorDialogActivity.class);
        int errorMessageId = -1;
        errorMessageId = R.string.outgoing_call_failed;
        if (errorMessageId != -1) {
            errorIntent.putExtra(ErrorDialogActivity.ERROR_MESSAGE_ID_EXTRA, errorMessageId);
        }
        errorIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivityAsUser(errorIntent, UserHandle.CURRENT);
    }

    public static PhoneAccountHandle getPhoneAccountForVoLTE(List<PhoneAccountHandle> volteAccounts,
            PhoneAccountHandle defaultPhoneAccoutHandle) {
        PhoneAccountHandle result = defaultPhoneAccoutHandle;
        if (volteAccounts == null || volteAccounts.isEmpty()) {
            result = null;
        } else if (volteAccounts.size() == 1) {
            result = volteAccounts.get(0);
        } else if (result != null && !volteAccounts.contains(result)) {
            result = null;
        }
        Log.d(LOG_TAG, "getPhoneAccountForVoLTE()...account changed: %s => %s",
                defaultPhoneAccoutHandle, result);
        return result;
    }

    //-------------For VoLTE conference dial (one key conference)------------------
    public static boolean isConferenceDialRequest(Object obj) {
        boolean result = false;
        if (isVolteSupport() && obj != null) {
            if (obj instanceof Intent) {
                Intent intent = (Intent) obj;
                result = intent.getBooleanExtra(TelecomManagerEx.EXTRA_VOLTE_CONF_CALL_DIAL, false);
            } else if (obj instanceof Bundle) {
                Bundle bundle = (Bundle) obj;
                result = bundle.getBoolean(TelecomManagerEx.EXTRA_VOLTE_CONF_CALL_DIAL, false);
            } else {
                log("isConferenceDialRequest()...unexpected obj: " + obj);
            }
        }
        return result;
    }

    public static ArrayList<String> getConferenceDialNumbers(Object obj) {
        ArrayList<String> result = new ArrayList<String>();
        if (isVolteSupport() && obj != null) {
            if (obj instanceof Intent) {
                Intent intent = (Intent) obj;
                result.addAll(intent
                        .getStringArrayListExtra(TelecomManagerEx.EXTRA_VOLTE_CONF_CALL_NUMBERS));
            } else if (obj instanceof Bundle) {
                Bundle bundle = (Bundle) obj;
                result.addAll(bundle
                        .getStringArrayList(TelecomManagerEx.EXTRA_VOLTE_CONF_CALL_NUMBERS));
            } else {
                log("getConferenceDialNumbers()...unexpected obj: " + obj);
            }
        }
        log("getConferenceDialNumbers()...size of numbers: " + result.size());
        return result;
    }

    public static final String FAKE_NUMBER = "10010";
    /**
     * For Conference Dial, handle is meaningless;
     * But later process will check it, so here we change it to be a valid fake handle.
     * @param context
     * @param handle
     * @return
     */
    public static Uri checkHandleForConferenceDial(Context context, Uri handle) {
        Uri result = handle;
        if (TelecomSystem.getInstance().getCallsManager().isPotentialMMIOrInCallMMI(handle)
                || TelephonyUtil.shouldProcessAsEmergency(context, handle)) {
            log("checkHandleForConferenceDial()...change to fake handle from: " + handle);
            result = Uri.fromParts(PhoneAccount.SCHEME_TEL, FAKE_NUMBER, null);
        }
        return result;
    }

    public static boolean containsEccNumber(Context context, List<String> numbers) {
        boolean result = false;
        if (context != null && numbers != null && !numbers.isEmpty()) {
            for (String number : numbers) {
                result = PhoneNumberUtils.isPotentialLocalEmergencyNumber(context, number);
                if (result) {
                    break;
                }
            }
        }
        return result;
    }

    public static boolean isConferenceInvite(Bundle extras) {
        boolean result = false;
        if (extras != null && extras.containsKey(TelecomManagerEx.EXTRA_VOLTE_CONF_CALL_INCOMING)) {
            result = extras.getBoolean(TelecomManagerEx.EXTRA_VOLTE_CONF_CALL_INCOMING, false);
        }
        log("isConferenceInvite()...result : " + result);
        return result;
    }

    //-------------For VoLTE normal call switch to ECC------------------
    /**
     * This function used to judge that whether the call has been marked as Ecc by NW or not.
     * @param bundle
     * @param defaultValue
     * @return
     */
    public static boolean isEmergencyCallChanged(Bundle bundle, boolean defaultValue) {
        boolean isChanged = false;
        if (isVolteSupport() && bundle != null
                && bundle.containsKey(TelecomManagerEx.EXTRA_VOLTE_MARKED_AS_EMERGENCY)) {
            boolean isEcc = bundle.getBoolean(TelecomManagerEx.EXTRA_VOLTE_MARKED_AS_EMERGENCY);
            if (isEcc != defaultValue) {
                log("isEmergencyCallChanged: " + defaultValue + " => " + isEcc);
                isChanged = true;
            }
        }
        return isChanged;
    }

    //-------------For VoLTE PAU field------------------
    /**
     * This function used to judge that whether the pau information has been changed.
     * @param bundle
     * @param defaultValue
     * @return
     */
    public static boolean isPauFieldChanged(Bundle bundle, String defaultValue) {
        boolean isChanged = false;
        if (isVolteSupport() && bundle != null
                && bundle.containsKey(TelecomManagerEx.EXTRA_VOLTE_PAU_FIELD)) {
            String pauFiled = bundle.getString(TelecomManagerEx.EXTRA_VOLTE_PAU_FIELD);
            if (!TextUtils.equals(defaultValue, pauFiled)) {
                log("isPauFieldChanged: " + defaultValue + " => " + pauFiled);
                isChanged = true;
            }
        }
        return isChanged;
    }

    /**
     * This function used to get pau information from bundle.
     * @param bundle
     * @return
     */
    public static String getPauFieldFromBundle(Bundle bundle) {
        String pauField = "";
        if (bundle != null) {
            pauField = bundle.getString(TelecomManagerEx.EXTRA_VOLTE_PAU_FIELD);
        }
        return pauField;
    }

    /**
     * This function to get proper number from  pau information.
     * priority of "<tel" is higher than "<sip:"
     * @param pauField
     * @return
     */
    public static Uri getHandleFromPauField(String pauField) {
        Uri handle = null;
        if (!TextUtils.isEmpty(pauField)) {
            String numberInPAU = getNumberFromPAU(pauField);
            String sipNumberInPAU = getSipNumberFromPAU(pauField);
            if (!TextUtils.isEmpty(numberInPAU)) {
                handle = Uri.fromParts(PhoneAccount.SCHEME_TEL, numberInPAU, null);
            } else if (!TextUtils.isEmpty(sipNumberInPAU)) {
                handle = Uri.fromParts(PhoneAccount.SCHEME_TEL, sipNumberInPAU, null);
            }
            log("getHandleFromPauField()... handle: " + handle);
        }
        return handle;
    }

    private static final String PAU_FIELD_NUMBER = "<tel:";
    private static final String PAU_FIELD_NAME = "<name:";
    private static final String PAU_FIELD_SIP_NUMBER = "<sip:";
    private static final String PAU_FIELD_END_FLAG = ">";

    public static String getNumberFromPAU(String pau) {
        String number = "";
        if (!TextUtils.isEmpty(pau)) {
            number = getFieldValue(pau, PAU_FIELD_NUMBER);
        }
        return number;
    }

    public static String getNameFromPAU(String pau) {
        String name = "";
        if (!TextUtils.isEmpty(pau)) {
            name = getFieldValue(pau, PAU_FIELD_NAME);
        }
        return name;
    }

    public static String getSipNumberFromPAU(String pau) {
        String sipNumber = "";
        if (!TextUtils.isEmpty(pau)) {
            sipNumber = getFieldValue(pau, PAU_FIELD_SIP_NUMBER);
        }

        // If The sip number is comprised with digit only, then return number without domain name.
        //      Eg, "+14253269830@10.174.2.2" => "+14253269830".
        // and if is not only comprised with digit, then return number + domain name.
        //      Eg, "Baicolin@iptel.org", then return "Baicolin@iptel.org".
        // the first digit may contains "+" or "-", like "+10010", handle it as the first case.
        if (!TextUtils.isEmpty(sipNumber) && sipNumber.contains("@")) {
            int index = sipNumber.indexOf("@");
            String realNumber = sipNumber.substring(0, index);
            realNumber = realNumber.trim();
            if (realNumber.matches("^[+-]*[0-9]*$")) {
                sipNumber = realNumber;
            }
        }
        return sipNumber;
    }

    private static String getFieldValue(String pau, String field) {
        String value = "";
        if (TextUtils.isEmpty(pau) || TextUtils.isEmpty(field)) {
            log("getFieldValue()... pau or field is null !");
            return value;
        }

        if (!pau.contains(field)) {
            log("getFieldValue()... There is no such field in pau !" + " field / pau :"
                    + field + " / " + pau);
            return value;
        }

        int startIndex = pau.indexOf(field);
        startIndex += field.length();
        int endIndex = pau.indexOf(PAU_FIELD_END_FLAG, startIndex);
        value = pau.substring(startIndex, endIndex);
        log("getFieldValue()... value / field / pau :" + value + " / " + field + " / " + pau);
        return value;
    }

    //--------------[VoLTE_SS] notify user when volte mmi request while data off-------------
    /**
     * Check whether the disconnect call is a mmi dial request with data off case.
     * @param disconnectCause use this info to check
     */
    public static boolean isMmiWithDataOff(DisconnectCause disconnectCause) {
        boolean result = false;
        if (disconnectCause != null) {
            int disconnectCode = disconnectCause.getCode();
            String disconnectReason = disconnectCause.getReason();
            if (disconnectCode == DisconnectCause.ERROR && !TextUtils.isEmpty(disconnectReason)
                    && disconnectReason.contains(
                    TelecomManagerEx.DISCONNECT_REASON_VOLTE_SS_DATA_OFF)) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Notify user to open data connection.
     * @param context
     * @param phoneAccountHandle
     */
    public static void showNoDataDialog(Context context, PhoneAccountHandle phoneAccountHandle) {
        if (context == null || phoneAccountHandle == null) {
            log("showNoDataDialog()... context or phoneAccountHandle is null, need check!");
            return;
        }
        TelecomManager telecomManager =
                (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        PhoneAccount phoneAccount = telecomManager.getPhoneAccount(phoneAccountHandle);
        int subId = telephonyManager.getSubIdForPhoneAccount(phoneAccount);

        if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            String ErrorMessage = context.getString(
                    R.string.volte_ss_not_available_tips, getSubDisplayName(context, subId));
            final Intent errorIntent = new Intent(context, ErrorDialogActivity.class);
            errorIntent.putExtra(ErrorDialogActivity.EXTRA_ERROR_MESSAGE, ErrorMessage);
            errorIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivityAsUser(errorIntent, UserHandle.CURRENT);
        }
    }

    /**
     * Get the sub's display name.
     * @param subId the sub id
     * @return the sub's display name, may return null
     */
    private static String getSubDisplayName(Context context, int subId) {
        String displayName = "";
        SubscriptionInfo subInfo = SubscriptionManager.from(context).getActiveSubscriptionInfo(subId);
        if (subInfo != null) {
            displayName = subInfo.getDisplayName().toString();
        }
        if (TextUtils.isEmpty(displayName)) {
            log("getSubDisplayName()... subId / subInfo: " + subId + " / " + subInfo);
        }
        return displayName;
    }

    public static void dumpVolteExtra(Bundle extra) {
        if (extra == null) {
            log("dumpVolteExtra()... no extra to dump !");
            return;
        }
        log("----------dumpVolteExtra begin-----------");
        if (extra.containsKey(TelecomManagerEx.EXTRA_VOLTE_MARKED_AS_EMERGENCY)) {
            log(TelecomManagerEx.EXTRA_VOLTE_MARKED_AS_EMERGENCY + " = "
                    + extra.getBoolean(TelecomManagerEx.EXTRA_VOLTE_MARKED_AS_EMERGENCY));
        }
        if (extra.containsKey(TelecomManagerEx.EXTRA_VOLTE_PAU_FIELD)) {
            log(TelecomManagerEx.EXTRA_VOLTE_PAU_FIELD + " = "
                    + extra.getString(TelecomManagerEx.EXTRA_VOLTE_PAU_FIELD));
        }
        log("----------dumpVolteExtra end-----------");
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, ">>>>>" + msg);
    }
}
