
package com.mediatek.telecom;

import android.content.Context;
import android.content.Intent;
import android.location.CountryDetector;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.server.telecom.Call;
import com.android.server.telecom.Constants;
import com.android.server.telecom.Log;
import com.android.server.telecom.PhoneAccountRegistrar;
import com.android.server.telecom.TelecomSystem;
import com.android.server.telecom.TelephonyUtil;
import com.android.server.telecom.components.ErrorDialogActivity;
import com.mediatek.common.dm.DmAgent;
import com.mediatek.internal.telephony.ITelephonyEx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TelecomUtils {

    private static final String TAG = TelecomUtils.class.getSimpleName();
    // add to enable specify a slot to MO.
    // using cmd:adb shell am start -a android.intent.action.CALL
    // -d tel:10010 --ei com.android.phone.extra.slot 1
    public static final String EXTRA_SLOT = "com.android.phone.extra.slot";

    // Add temp feature option for ip dial.
    public static final boolean MTK_IP_PREFIX_SUPPORT = true;

    /*
     * M: get initial number from intent.
     */
    public static String getInitialNumber(Context context, Intent intent) {
        Log.d(TAG, "getInitialNumber(): " + intent);

        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return "";
        }

        // If the EXTRA_ACTUAL_NUMBER_TO_DIAL extra is present, get the phone
        // number from there.  (That extra takes precedence over the actual data
        // included in the intent.)
        if (intent.hasExtra(Constants.EXTRA_ACTUAL_NUMBER_TO_DIAL)) {
            String actualNumberToDial =
                intent.getStringExtra(Constants.EXTRA_ACTUAL_NUMBER_TO_DIAL);
            return actualNumberToDial;
        }

        return PhoneNumberUtils.getNumberFromIntent(intent, context);
    }

    public static boolean isDMLocked() {
        boolean locked = false;
        try {
            IBinder binder = ServiceManager.getService("DmAgent");
            DmAgent agent = null;
            if (binder != null) {
                agent = DmAgent.Stub.asInterface(binder);
            }
            if (agent != null) {
                locked = agent.isLockFlagSet();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return locked;
    }


    /// M: For Ip dial @{
    /**
     * This function used to check whether the dial request is a Ip dial request.
     * If airplane mode is on, do not check ip prefix.
     * @param context
     * @param call
     * @param extras
     * @return
     */
    public static boolean isIpDialRequest(Context context, Call call, Bundle extras) {
        boolean result = false;
        boolean isEmergencyCall = TelephonyUtil.shouldProcessAsEmergency(context,
                call.getHandle());
        if (TelecomUtils.MTK_IP_PREFIX_SUPPORT
                && extras.getBoolean(Constants.EXTRA_IS_IP_DIAL, false)
                && !isEmergencyCall
                && call.getHandle() != null
                && PhoneAccount.SCHEME_TEL.equals(call.getHandle().getScheme())
                && !isAirPlaneModeOn(context)) {
            result = true;
        }
        return result;
    }

    /**
     * This function used to get certain phoneAccount for Ip dial.
     * simAccounts.size() == 0, => no sim account, set null.
     * simAccounts.size() == 1, => only one sim account, use it.
     * simAccounts.size()  > 1, => if valid default account exist, do nothing; or set null(select)
     * @param simAccounts
     * @param defaultPhoneAccoutHandle
     * @return
     */
    public static PhoneAccountHandle getPhoneAccountForIpDial(List<PhoneAccountHandle> simAccounts,
            PhoneAccountHandle defaultPhoneAccoutHandle) {
        PhoneAccountHandle result = defaultPhoneAccoutHandle;
        if (simAccounts == null || simAccounts.isEmpty()) {
            result = null;
        } else if (simAccounts.size() == 1) {
            result = simAccounts.get(0);
        } else if (result != null && !simAccounts.contains(result)) {
            result = null;
        }
        Log.d(TAG, "getPhoneAccountForIpDial()...account changed: %s => %s",
                defaultPhoneAccoutHandle, result);
        return result;
    }

    /**
     * to check if the airplane mode is on or off.
     * @param ctx
     * @return boolean  true is on
     */
    public static boolean isAirPlaneModeOn(Context ctx) {
        int airplaneMode = Settings.Global.getInt(ctx.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0);
        if (airplaneMode == 0) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * This function used to set ip prefix for certain call.
     * @param context
     * @param call
     * @return true for success, false for fail.
     */
    public static boolean handleIpPrefixForCall(Context context, Call call) {
        boolean result = true;
        if (call.isIpCall() && call.getTargetPhoneAccountEx() != null) {
            String ipPrefix = getIpPrefix(context, call.getTargetPhoneAccountEx());
            Log.d(TAG, "handleIpPrefixForCall()...ipPrefix = %s", ipPrefix);
            // If radio is off, do not go to Call Setting;
            // just pass it to Telephony, which will return error message. see ALPS02400819;
            if (TextUtils.isEmpty(ipPrefix)
                    && TelecomUtils.isRadioOn(call.getTargetPhoneAccountEx(), context)) {
                Log.d(TAG, "handleIpPrefixForCall()...go to ip prefix setting");
                TelecomUtils.gotoIpPrefixSetting(context, call.getTargetPhoneAccountEx());
                result = false;
            } else {
                Uri newHandle = TelecomUtils.rebuildHandleWithIpPrefix(context,
                        call.getHandle(), ipPrefix);
                call.setHandle(newHandle, TelecomManager.PRESENTATION_ALLOWED);
                Log.d(TAG, "handleIpPrefixForCall()...handle changed: %s => %s", call.getHandle(),
                        newHandle);
                result = true;
            }
        }
        Log.d(TAG, "handleIpPrefixForCall()...result = %s", result);
        return result;
    }

    /**
     * This function used to get ip prefix based on certain phoneAccountHandle.
     * @param context
     * @param account
     * @return
     */
    public static String getIpPrefix(Context context, PhoneAccountHandle account) {
        String ipPrefix = "";
        if (context != null && account != null) {
            int subId = getSubIdByAccount(context, account);
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                ipPrefix = Settings.System.getString(context.getContentResolver(),
                        "ipprefix" + subId);
            }
        }
        return ipPrefix;
    }

    /**
     * This function used to guide user to setting UI to set ip prefix.
     * @param context
     * @param account
     */
    public static void gotoIpPrefixSetting(Context context, PhoneAccountHandle account) {
        if (context != null && account != null) {
            int subId = getSubIdByAccount(context, account);
            final Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(Constants.PHONE_PACKAGE, Constants.IP_PREFIX_SETTING_CLASS_NAME);
            intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, subId);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        }
    }

    /**
     * This function used to get sub id based on certain phoneAccountHandle.
     * @param context
     * @param accountHandle
     * @return
     */
    public static int getSubIdByAccount(Context context, PhoneAccountHandle accountHandle) {
        int result = -1;
        if (context != null && accountHandle != null) {
            PhoneAccount account = TelecomManager.from(context).getPhoneAccount(accountHandle);
            result = TelephonyManager.from(context).getSubIdForPhoneAccount(account);
        }
        return result;
    }

    /**
     * check the radio is on or off by phone account.
     *
     * @param PhoneAccountHandle the selected phone account
     * @return true if radio on
     */
    public static boolean isRadioOn(PhoneAccountHandle account, Context context) {
        int subId = getSubIdByAccount(context, account);
        Log.d(TAG, "[isRadioOn]subId:" + subId);
        boolean isRadioOn = true;
        final ITelephony iTel = ITelephony.Stub.asInterface(
                ServiceManager.getService(Context.TELEPHONY_SERVICE));
        if (iTel != null) {
            try {
                isRadioOn = iTel.isRadioOnForSubscriber(subId, context.getPackageName());
            } catch (RemoteException e) {
                Log.d(TAG, "[isRadioOn] failed to get radio state for sub " + subId);
                isRadioOn = false;
            }
        } else {
            Log.d(TAG, "[isRadioOn]failed to check radio");
        }
        Log.d(TAG, "[isRadioOn]isRadioOn:" + isRadioOn);
        return isRadioOn;
    }

    /**
     * rebuild handle with ip prefix; if ip prefix is null, return default handle.
     * @param context
     * @param defaultHandle
     * @param ipPrefix
     * @return
     */
    public static Uri rebuildHandleWithIpPrefix(Context context, Uri defaultHandle,
            String ipPrefix) {
        Uri resultHandle = defaultHandle;
        if (context != null && !TextUtils.isEmpty(ipPrefix) && defaultHandle != null) {
            String uriString = defaultHandle.getSchemeSpecificPart();
            if (uriString.indexOf(ipPrefix) < 0) {
                uriString = ipPrefix + filtCountryCode(context, uriString);
            }
            resultHandle = Uri.fromParts(defaultHandle.getScheme(), uriString, null);
        }
        return resultHandle;
    }

    /**
     * remove the country code from the number in international format.
     *
     * @param number
     * @return
     */
    private static String filtCountryCode(Context context, String number) {
        String countryIso = null;
        if (!TextUtils.isEmpty(number) && number.contains("+")) {
            try {
                CountryDetector mDetector = (CountryDetector) context
                        .getSystemService(Context.COUNTRY_DETECTOR);
                PhoneNumberUtil numUtil = PhoneNumberUtil.getInstance();
                if (mDetector != null && mDetector.detectCountry() != null) {
                    countryIso = mDetector.detectCountry().getCountryIso();
                } else {
                    countryIso = context.getResources().getConfiguration().locale
                            .getCountry();
                }
                PhoneNumber num = numUtil.parse(number, countryIso);
                return num == null ? number : String.valueOf(num
                        .getNationalNumber());
            } catch (NumberParseException e) {
                e.printStackTrace();
                Log.d(TAG, "parse phone number ... " + e);
            }
        }
        return number;
    }
    /// @}

    /**
     * Update default account handle when there has a valid suggested account
     * handle which not same with default.
     * @param extras The extra got from Intent.
     * @param accounts The all available accounts for current call.
     * @param defaultAccountHandle The default account handle.
     * @return newAccountHandle
     */
    public static boolean shouldShowAccountSuggestion(Bundle extras,
            List<PhoneAccountHandle> accounts, PhoneAccountHandle defaultAccountHandle) {
        boolean shouldShowAccountSuggestion = false;
        PhoneAccountHandle suggestedAccountHandle = getSuggestedPhoneAccountHandle(extras);

        if (accounts != null && defaultAccountHandle != null && suggestedAccountHandle != null
                && accounts.contains(suggestedAccountHandle)
                && !suggestedAccountHandle.equals(defaultAccountHandle)) {
            shouldShowAccountSuggestion = true;
        }
        Log.d(TAG, "shouldShowAccountSuggestion: " + shouldShowAccountSuggestion);
        return shouldShowAccountSuggestion;
    }

    /**
     * Added for suggesting phone account feature.
     * @param extras The extra got from Intent.
     * @param accounts The available PhoneAccounts.
     * @return The available suggested PhoneAccountHandle.
     */
    public static PhoneAccountHandle getSuggestedPhoneAccountHandle(Bundle extras) {
        PhoneAccountHandle suggestedAccountHandle = null;
        if (extras != null) {
            suggestedAccountHandle = extras
                    .getParcelable(TelecomManagerEx.EXTRA_SUGGESTED_PHONE_ACCOUNT_HANDLE);
        }
        Log.d(TAG, "Suggested PhoneAccountHandle is " + suggestedAccountHandle);
        return suggestedAccountHandle;
    }

    /**
     * original defined in CallsManager, we add it here for prevent MMI call when current is guest user
     * however, we still keep the original implementation in CallsManager.
     * @param handle
     * @return
     */
    public static boolean isPotentialMMICode(Uri handle) {
        return (handle != null && handle.getSchemeSpecificPart() != null
                && handle.getSchemeSpecificPart().contains("#"));
    }

    /**
     * Check account capability with given PhoneAccountHandle.
     * @param context The context for call service.
     * @param handle The PhoneAccountHandle used for check account.
     * @param capabilities The capabilities need to be checked.
     * @return The boolean result for check.
     */
    private static boolean hasAccountCapability(Context context, PhoneAccountHandle handle,
            int capabilities) {
        if (handle == null) {
            return false;
        }

        TelecomManager telecomManager = (TelecomManager) context
                .getSystemService(Context.TELECOM_SERVICE);
        PhoneAccount account = telecomManager.getPhoneAccount(handle);

        if (account != null) {
            return account.hasCapabilities(capabilities);
        }

        return false;
    }

    /**
     * Check if an account has cdma call capability.
     * @param context The context for call service.
     * @param handle The account handle.
     * @return The result for check.
     */
    public static boolean hasCdmaCallCapability(Context context, PhoneAccountHandle handle) {
        return hasAccountCapability(context, handle, PhoneAccount.CAPABILITY_CDMA_CALL_PROVIDER);
    }

    /**
     * Check if the account has registered to network.
     * @param context The context for get service.
     * @param account The account for check.
     * @return A boolean indicates the check result.
     */
    static boolean isAccountInService(Context context, PhoneAccount account) {
        boolean result = false;
        ITelephonyEx iTelephonyEx = ITelephonyEx.Stub.asInterface(ServiceManager
                .getService("phoneEx"));
        TelephonyManager tem = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        if (iTelephonyEx == null) {
            Log.d(TAG, "iTelephonyEx is Null.");
            return result;
        }

        int subId = -1;
        try {
            subId = tem.getSubIdForPhoneAccount(account);
        } catch (NumberFormatException e) {
            Log.d(TAG, "account sub id error.");
            return result;
        }

        ServiceState ss = null;
        Log.d(TAG, "isAccountInService subId = " + subId);
        try {
            ss = ServiceState.newFromBundle(iTelephonyEx.getServiceState(subId));
            Log.d(TAG, "isAccountInService = " + ss);
            if (ss.getVoiceRegState() == ServiceState.STATE_IN_SERVICE) {
                result = true;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            result = false;
        }
        Log.d(TAG, "isAccountInService account = " + account + " result = " + result);
        return result;
    }

    /**
     * Check if a account support MMI code.
     * @param context The context.
     * @param handle The handle for account.
     * @return A boolean indicates the check result.
     */
    public static boolean isSupportMMICode(Context context, PhoneAccountHandle handle) {
        return !hasCdmaCallCapability(context, handle);
    }

    /**
     * M: Add for 3G VT only
     * @return
     */
    public static boolean isSupport3GVT() {
        return SystemProperties.get("ro.mtk_vt3g324m_support").equals("1");
    }

    /**
     * This function used to get PhoneAccountHandle(s) which support VoLTE.
     * @return
     */
    public static List<PhoneAccountHandle> getVoltePhoneAccountHandles() {
        List<PhoneAccountHandle> phoneAccountHandles = new ArrayList<PhoneAccountHandle>();
        PhoneAccountRegistrar phoneAccountRegistrar = TelecomSystem.getInstance().
                getPhoneAccountRegistrar();
        if (phoneAccountRegistrar != null) {
            phoneAccountHandles.addAll(phoneAccountRegistrar.getVolteCallCapablePhoneAccounts());
        }
        return phoneAccountHandles;
    }

    /**
     * This function used to get PhoneAccountHandle(s), which is sim based.
     * @return
     */
    public static List<PhoneAccountHandle> getSimPhoneAccountHandles() {
        List<PhoneAccountHandle> simPhoneAccountHandles = new ArrayList<PhoneAccountHandle>();
        PhoneAccountRegistrar phoneAccountRegistrar = TelecomSystem.getInstance().
                getPhoneAccountRegistrar();
        if (phoneAccountRegistrar != null) {
            simPhoneAccountHandles.addAll(phoneAccountRegistrar.getSimPhoneAccounts());
        }
        return simPhoneAccountHandles;
    }

    /**
     * This function used to get PhoneAccountHandle by slot id.
     * @param context
     * @param slotId
     * @return
     */
    public static PhoneAccountHandle getPhoneAccountHandleWithSlotId(Context context,
            int slotId, PhoneAccountHandle defaultPhoneAccountHandle) {
        PhoneAccountHandle result = defaultPhoneAccountHandle;
        if (SubscriptionManager.isValidSlotId(slotId)) {
            SubscriptionInfo subInfo = SubscriptionManager.from(context)
                    .getActiveSubscriptionInfoForSimSlotIndex(slotId);
            List<PhoneAccountHandle> phoneAccountHandles = getSimPhoneAccountHandles();
            if (subInfo != null && phoneAccountHandles != null && !phoneAccountHandles.isEmpty()) {
                for (PhoneAccountHandle accountHandle : phoneAccountHandles) {
                    if (Objects.equals(accountHandle.getId(), subInfo.getIccId())) {
                        result = accountHandle;
                        break;
                    }
                }
            }
        }
        Log.d(TAG, "getPhoneAccountHandleWithSlotId()... slotId = %s; account changed: %s => %s",
                slotId, defaultPhoneAccountHandle, result);
        return result;
    }

    /**
     * This function used to start ErrorDialogActivity to show error message.
     * @param context
     * @param msgId
     */
    public static void showErrorDialog(Context context, int msgId) {
        if (context == null) {
            return;
        }
        final Intent errorIntent = new Intent(context, ErrorDialogActivity.class);
        errorIntent.putExtra(ErrorDialogActivity.ERROR_MESSAGE_ID_EXTRA, msgId);
        errorIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivityAsUser(errorIntent, UserHandle.CURRENT);
    }

    /**
     * To make the phoneaccount selection UI
     * show the accounts in ascend sequence,
     * we sort the PhoneAccount by slotId ascend.
     * since slotId is start from 0, so we only need to
     * put the PhoneAccount object to an ArrayList with
     * the index as its slotId.
     */
    public static void sortPhoneAccountsBySlotIdAscend(List phoneAccounts){
        if (phoneAccounts == null || phoneAccounts.size() <= 1) {
            return ;
        }

        List<PhoneAccount> sortedList = new ArrayList<PhoneAccount>();
        sortedList.addAll(phoneAccounts);

        Collections.sort(sortedList, new java.util.Comparator<PhoneAccount>(){
            @Override
            public int compare(PhoneAccount a, PhoneAccount b) {
                int subId1 = TelephonyManager.getDefault()
                        .getSubIdForPhoneAccount((PhoneAccount) a);
                int subId2 = TelephonyManager.getDefault()
                        .getSubIdForPhoneAccount((PhoneAccount) b);
                int slotId1 = -1;
                int slotId2 = -1;
                if (SubscriptionManager.isValidSubscriptionId(subId1)) {
                    slotId1 = SubscriptionManager.getSlotId(subId1);
                } else {
                    return 0;
                }
                if (SubscriptionManager.isValidSubscriptionId(subId2)) {
                    slotId2 = SubscriptionManager.getSlotId(subId2);
                } else {
                    return 0;
                }
                return slotId1 - slotId2;
            }
        });

        if (sortedList.size() > 0) {
            phoneAccounts.clear();
            phoneAccounts.addAll(sortedList);
        }
    }

    /// M: For block certain ViLTE @{
    public enum FeatureType {
        ViLTE_BLOCK_HOLD,       // Hold ViLTE Call.
        ViLTE_BLOCK_NEW_CALL,   // ViLTE exist, block all MO; Voice exits, block new ViLTE.
    }

    public static boolean isFeatureEnabled(Context context, PhoneAccountHandle accountHandle,
            FeatureType featureType) {
        // For now, above three features are all open or all close, so no need check one by one.
        final List<String> cMccMncMccList =
                Arrays.asList("46000", "46002", "46007", "46008", "46011");
        String simOperator = getSimOperator(context, accountHandle);
        return cMccMncMccList.contains(simOperator);
    }

    /**
     * Get certain phoneAccountHandle's corresponding PLMN.
     * @param context
     * @param call
     * @return
     */
    public static String getSimOperator(Context context, PhoneAccountHandle accountHandle) {
        String simOperator = "";
        if (context != null && accountHandle != null) {
            PhoneAccount phoneAccount = TelecomManager.from(context).getPhoneAccount(accountHandle);
            TelephonyManager telephonyManager = TelephonyManager.from(context);
            int subId = telephonyManager.getSubIdForPhoneAccount(phoneAccount);
            simOperator = telephonyManager.getSimOperator(subId);
        }
        return simOperator;
    }
    /// @}
}