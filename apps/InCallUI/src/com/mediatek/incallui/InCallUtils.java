package com.mediatek.incallui;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.Toast;
import com.android.incallui.Call;
import com.android.incallui.CallList;
import com.android.incallui.Log;
import com.mediatek.common.dm.DmAgent;
import com.mediatek.telecom.TelecomManagerEx;
import java.util.List;

public final class InCallUtils {

    private static final String TAG = InCallUtils.class.getSimpleName();
    public static final String EXTRA_IS_IP_DIAL = "com.android.phone.extra.ip";
    private static final String TELECOM_PACKAGE_NAME = "com.android.server.telecom";
    private static final String OUTGOING_FAILED_MSG_RES_ID = "outgoing_call_failed";

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

        Log.d(TAG, "isDMLocked(): locked = " + locked);
        return locked;
    }

    private static boolean mPrivacyProtectOpen = false;
    /**
     * check whether PrivacyProtect open.
     * @return
     */
    public static boolean isprivacyProtectOpen() {
        Log.d(TAG, "mPrivacyProtectOpen: " + mPrivacyProtectOpen);
        return mPrivacyProtectOpen;
    }

    /**
     * set privacyProtectOpen value.
     * @param isPrivacyProtectOpen
     */
    public static void setprivacyProtectEnabled(boolean isPrivacyProtectOpen) {
        Log.d(TAG, "isPrivacyProtectOpen: " + isPrivacyProtectOpen);
        mPrivacyProtectOpen = isPrivacyProtectOpen;
    }

    /**
     * when hold call have the ECT capable call,it will be true,otherwise false.
     */
    public static boolean canSetEct() {
        final Call call = CallList.getInstance().getBackgroundCall();
        if (call != null && call.can(android.telecom.Call.Details.CAPABILITY_ECT)) {
            return true;
        }
        return false;
    }

    /**
     * When there have more than one active call or background call and has no
     * incoming, it will be true, otherwise false.
     */
    public static boolean canHangupAllCalls() {
        CallList callList = CallList.getInstance();
        Call call = callList.getFirstCall();
        if (call != null && !Call.State.isIncoming(call.getState())
                && callList.getActiveAndHoldCallsCount() > 1) {
            return true;
        }
        return false;
    }

    /**
     * When there have more than one active call or background call and has no
     * incoming, it will be true, otherwise false.
     */
    public static boolean canHangupAllHoldCalls() {
        CallList callList = CallList.getInstance();
        Call call = callList.getFirstCall();
        if (call != null && !Call.State.isIncoming(call.getState())
                && callList.getActiveAndHoldCallsCount() > 1) {
            return true;
        }
        return false;
    }

    /**
     * When there has one active call and a incoming call which can be answered,
     * it will be true, otherwise false.
     */
    public static boolean canHangupActiveAndAnswerWaiting() {
        CallList callList = CallList.getInstance();
        Call call = callList.getFirstCall();
        if (call != null && Call.State.isIncoming(call.getState())
                && callList.getActiveCall() != null
                && !isCdmaCall(call)) {
            return true;
        }
        return false;
    }

    /*
     * Get ip prefix from provider.
     */
    public static String getIpPrefix(Context context, String subId) {
        String ipPrefix = null;
        if (!TextUtils.isEmpty(subId)) {
            ipPrefix = Settings.System.getString(context.getContentResolver(),
                            "ipprefix" + subId);
        }
        Log.d(TAG, "ip prefix = " + ipPrefix);
        return ipPrefix;
    }

    /**
     * Whether this call is ip dial but without IPPrefix.
     * @param context
     * @param call
     * @return
     */
    public static boolean isIpCallWithoutPrefix(Context context, Call call) {
        if (call == null || call.getAccountHandle() == null) {
            Log.d(TAG, "isIpCallWithoutPrefix, call or account handle is null, do nothing.");
            return false;
        }

        Bundle extras = call.getTelecommCall().getDetails().getExtras();
        boolean isIpDial = (extras != null) && extras.getBoolean(EXTRA_IS_IP_DIAL, false);
        if (isIpDial) {
            String ipPrefix = getIpPrefix(context, call.getAccountHandle().getId());
            if (TextUtils.isEmpty(ipPrefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Added for suggesting phone account feature.
     * @param call
     * @return
     */
    public static PhoneAccountHandle getSuggestedPhoneAccountHandle(Call call) {
        if (call == null) {
            return null;
        }
        Bundle extras = call.getTelecommCall().getDetails().getIntentExtras();
        final PhoneAccountHandle suggestedPhoneAccountHandle;
        if (extras != null) {
            suggestedPhoneAccountHandle = extras
                    .getParcelable(TelecomManagerEx.EXTRA_SUGGESTED_PHONE_ACCOUNT_HANDLE);
        } else {
            suggestedPhoneAccountHandle = null;
        }
        Log.d(TAG, "getSuggestedPhoneAccountHandle(), suggestedPhoneAccountHandle is "
                + suggestedPhoneAccountHandle);
        return suggestedPhoneAccountHandle;
    }

    /**
     * Check if the call's account has CAPABILITY_CDMA_CALL_PROVIDER.
     */
    public static boolean isCdmaCall(Call call) {
        if (null == call) {
            return false;
        }

        Context context = com.android.incallui.InCallPresenter.getInstance()
                .getContext();

        PhoneAccountHandle accountHandle = call.getAccountHandle();

        TelecomManager telecomManager = (TelecomManager) context
                .getSystemService(Context.TELECOM_SERVICE);
        if (telecomManager == null) {
            return false;
        }

        PhoneAccount account = telecomManager.getPhoneAccount(accountHandle);

        if (account != null) {
            return account.hasCapabilities(PhoneAccount.CAPABILITY_CDMA_CALL_PROVIDER);
        }
        return false;
    }

    /**
     *  M: add for get subId for this call.
     * @return subId
     */
    public static int getSubId(Call call) {
        if(call.getAccountHandle() == null || !call.isTelephonyCall()) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }

        Context context = com.android.incallui.InCallPresenter.getInstance()
                .getContext();

        PhoneAccountHandle phoneAccountHandle = call.getAccountHandle();

        TelecomManager telecomManager = (TelecomManager) context
                .getSystemService(Context.TELECOM_SERVICE);

        if (telecomManager == null) {
            Log.e(TAG, "getSubId(): telecomManager is null");
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }

        PhoneAccount phoneAccount = telecomManager.getPhoneAccount(phoneAccountHandle);

        TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);

        if (telephonyManager == null) {
            Log.e(TAG, "getSubId(): telephonyManager is null");
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }

        int subId = telephonyManager.getSubIdForPhoneAccount(phoneAccount);

        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Log.e(TAG, "getSubId(): subId is invalid");
        }
        return subId;
    }

    /**
     * M: format the width/height string.
     * @param width width.
     * @param height height.
     * @return the formatted string.
     */
    public static String formatSize(int width, int height) {
        return String.format("[%d,%d]", width, height);
    }

    /**
     * M: show the same error message as Telecom when can't MO.
     * typically, when one call is in upgrading to video progress, someone
     * is responsible to prevent new outgoing call. Currently, we have nowhere
     * to do this except InCallUI itself.
     * TODO: the Telecom or Lower layer should be responsible to stop new outgoing call while
     * upgrading instead of InCallUI.
     *
     * @param context the ApplicationContext
     * @param call
     */
    public static void showOutgoingFailMsg(Context context, android.telecom.Call call) {
        if (context == null || call == null ||
                android.telecom.Call.STATE_RINGING == call.getState()) {
            return;
        }

        final PackageManager pm = context.getPackageManager();
        Resources telecomResources = null;
        try {
            telecomResources = pm.getResourcesForApplication(TELECOM_PACKAGE_NAME);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "telecomResources not found");
        }

        if (telecomResources != null) {
            int resId = telecomResources.getIdentifier(
                    OUTGOING_FAILED_MSG_RES_ID, "string", TELECOM_PACKAGE_NAME);
            String msg = telecomResources.getString(resId);
            Log.d(TAG, "showOutgoingFailMsg msg-->" + msg);

            if (!TextUtils.isEmpty(msg)) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
