package com.mediatek.settings.sim;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.RadioAccessFamily;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.settings.FeatureOption;

import java.util.Iterator;

public class TelephonyUtils {
    private static final String TAG = "TelephonyUtils";

    /**
     * Get whether airplane mode is in on.
     * @param context Context.
     * @return True for on.
     */
    public static boolean isAirplaneModeOn(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    /**
     * Calling API to get subId is in on.
     * @param subId Subscribers ID.
     * @return {@code true} if radio on
     */
    public static boolean isRadioOn(int subId, Context context) {
        ITelephony phone = ITelephony.Stub.asInterface(ServiceManager
                .getService(Context.TELEPHONY_SERVICE));
        boolean isOn = false;
        try {
            // for ALPS02460942, during SIM switch, radio is unavailable, consider it as OFF
            if (phone != null && !isCapabilitySwitching()) {
                isOn = subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID ? false :
                    phone.isRadioOnForSubscriber(subId, context.getPackageName());
            } else {
                Log.d(TAG, "capability switching, or phone is null ? " + (phone == null));
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "isOn = " + isOn + ", subId: " + subId);
        return isOn;
    }

    /**
     * check all slot radio on.
     * @param context context
     * @return is all slots radio on;
     */
    public static boolean isAllSlotRadioOn(Context context) {
        boolean isAllRadioOn = true;
        int[] subs = SubscriptionManager.from(context).getActiveSubscriptionIdList();
        for (int i = 0; i < subs.length; ++i) {
            isAllRadioOn = isAllRadioOn && isRadioOn(subs[i], context);
        }
        Log.d(TAG, "isAllSlotRadioOn()... isAllRadioOn: " + isAllRadioOn);
        return isAllRadioOn;
    }

    /**
     * capability switch.
     * @return true : switching
     */
    public static boolean isCapabilitySwitching() {
        ITelephonyEx telephonyEx = ITelephonyEx.Stub.asInterface(
                ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
        boolean isSwitching = false;
        try {
            if (telephonyEx != null) {
                isSwitching = telephonyEx.isCapabilitySwitching();
            } else {
                Log.d(TAG, "mTelephonyEx is null, returen false");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException = " + e);
        }
        Log.d(TAG, "isSwitching = " + isSwitching);
        return isSwitching;
    }

    /**
     * convert PhoneAccountHandle to subId.
     * @param context context
     * @param handle PhoneAccountHandle
     * @return subId
     */
    public static int phoneAccountHandleTosubscriptionId(Context context,
            PhoneAccountHandle handle) {
        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        if (handle != null) {
            final PhoneAccount phoneAccount = TelecomManager.from(context).getPhoneAccount(handle);
            subId = TelephonyManager.from(context).getSubIdForPhoneAccount(phoneAccount);
        }
        Log.d(TAG, "PhoneAccountHandleTosubscriptionId()... subId: " + subId);
        return subId;
    }

    /**
     * convert subId to PhoneAccountHandle.
     * @param context context
     * @param subId subId
     * @return PhoneAccountHandle
     */
    public static PhoneAccountHandle subscriptionIdToPhoneAccountHandle(Context context,
            final int subId) {
        final TelecomManager telecomManager = TelecomManager.from(context);
        final TelephonyManager telephonyManager = TelephonyManager.from(context);
        final Iterator<PhoneAccountHandle> phoneAccounts =
                telecomManager.getCallCapablePhoneAccounts().listIterator();

        while (phoneAccounts.hasNext()) {
            final PhoneAccountHandle phoneAccountHandle = phoneAccounts.next();
            final PhoneAccount phoneAccount = telecomManager.getPhoneAccount(phoneAccountHandle);
            if (subId == telephonyManager.getSubIdForPhoneAccount(phoneAccount)) {
                return phoneAccountHandle;
            }
        }

        return null;
    }

    /**
     * which slot have the Main Capability(3G/4G).
     * @param context context
     * @return main capability slotId.
     */
    public static int getMainCapabilitySlotId(Context context) {
        ITelephonyEx iTelEx = ITelephonyEx.Stub.asInterface(
                ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
        int phoneId = SubscriptionManager.INVALID_PHONE_INDEX;
        try {
            if (null != iTelEx) {
                phoneId = iTelEx.getMainCapabilityPhoneId();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        int subId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
        int slotId = SubscriptionManager.getSlotId(subId);
        Log.d(TAG, "getMainCapabilitySlotId()... slotId: " + slotId);
        return slotId;
    }

    /**
     * switch main capability to targetSubId.
     * @param context context
     * @param targetSubId subId
     * @return true   success
     */
    public static boolean setRadioCapability(Context context, int targetSubId) {
        int phoneNum = TelephonyManager.from(context).getPhoneCount();
        boolean isSwitchSuccess = true;
        Log.d(TAG, "setRadioCapability()...  targetSubId: " + targetSubId);

        String curr3GSim = SystemProperties.get(PhoneConstants.PROPERTY_CAPABILITY_SWITCH, "");
        Log.d(TAG, "current 3G Sim = " + curr3GSim);

        if (curr3GSim != null && !curr3GSim.equals("")) {
            int curr3GPhoneId = Integer.parseInt(curr3GSim);
            int phoneId = SubscriptionManager.getPhoneId(targetSubId);
            if (curr3GPhoneId == (phoneId + 1)) {
                Log.d(TAG, "Current 3G phone equals target phone, don't trigger switch");
                return isSwitchSuccess;
            }
        }

        try {
            ITelephony iTel = ITelephony.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE));
            ITelephonyEx iTelEx = ITelephonyEx.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
            if (null == iTel) {
                Log.e(TAG, "Can not get phone service");
                return false;
            }
            boolean isLteSupport = FeatureOption.MTK_LTE_SUPPORT;

            RadioAccessFamily[] rafs = new RadioAccessFamily[phoneNum];
            for (int phoneId = 0; phoneId < phoneNum; phoneId++) {
                int raf = iTel.getRadioAccessFamily(phoneId, context.getPackageName());
                int id = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
                Log.d(TAG, " phoneId=" + phoneId + " subId=" + id + " RAF=" + raf);
                raf |= RadioAccessFamily.RAF_GSM;
                if (id == targetSubId) {
                    raf |= RadioAccessFamily.RAF_UMTS;
                    if (isLteSupport) {
                        raf |= RadioAccessFamily.RAF_LTE;
                    }
                } else {
                    raf &= ~RadioAccessFamily.RAF_UMTS;
                    if (isLteSupport) {
                        raf &= ~RadioAccessFamily.RAF_LTE;
                    }
                }
                Log.d(TAG, " newRAF=" + raf);
                rafs[phoneId] = new RadioAccessFamily(phoneId, raf);
            }
            if (false == iTelEx.setRadioCapability(rafs)) {
                Log.d(TAG, "Set phone rat fail!!!");
                isSwitchSuccess = false;
            }
        } catch (RemoteException ex) {
            Log.d(TAG, "Set phone rat fail!!!");
            ex.printStackTrace();
            isSwitchSuccess = false;
        }

        return isSwitchSuccess;
    }

    /**
     * Set default data sub ID without invoking capability switch.
     * @param context context
     * @param subId subId
     */
    public static void setDefaultDataSubIdWithoutCapabilitySwitch(Context context, int subId) {
        SubscriptionManager.from(context).setDefaultDataSubIdWithoutCapabilitySwitch(subId);
    }
}