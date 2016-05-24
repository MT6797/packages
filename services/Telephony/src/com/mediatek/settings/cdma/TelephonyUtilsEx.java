package com.mediatek.settings.cdma;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.RadioAccessFamily;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.IccCardConstants.CardType;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.uicc.UiccController;
import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneUtils;

import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.phone.PhoneFeatureConstants.FeatureOption;
import com.mediatek.telephony.TelephonyManagerEx;

/**
 * Some util functions for C2K features.
 */
public class TelephonyUtilsEx {

    private static final String TAG = "TelephonyUtilsEx";
    private static final String PROPERTY_3G_SIM = "persist.radio.simswitch";

    static final int preferredNetworkMode = Phone.PREFERRED_NT_MODE;
    public static final int CT_SIM = TelephonyManagerEx.APP_FAM_3GPP2;
    public static final int GSM_SIM = TelephonyManagerEx.APP_FAM_3GPP;
    public static final int C_G_SIM = CT_SIM | GSM_SIM;
    public static final int SIM_TYPE_NONE = TelephonyManagerEx.APP_FAM_NONE;
    private static final int MODE_PHONE1_ONLY = 1;
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE = {
        "gsm.ril.fulluicctype",
        "gsm.ril.fulluicctype.2",
        "gsm.ril.fulluicctype.3",
        "gsm.ril.fulluicctype.4",
    };

    /**
     * Whether is CDMA phone.
     * @param phone the phone object.
     * @return true if is cdma phone.
     */
    public static boolean isCDMAPhone(Phone phone) {
        boolean result = false;

        if (phone != null) {
            int phoneType = phone.getPhoneType();
            if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                result = true;
            }
        }
        Log.d(TAG, "isCDMAPhone: " + result);

        return result;
    }

    /**
     * Get sim type.
     * @return sim type.
     */
    public static int getSimType(int slotId) {
        int simType = SIM_TYPE_NONE;
        TelephonyManagerEx telephonyManagerEx = TelephonyManagerEx.getDefault();
        if (telephonyManagerEx != null) {
            simType = telephonyManagerEx.getIccAppFamily(slotId);
        }

        Log.d(TAG, "simType: " + simType);
        return simType;
    }

    /**
     * Check is airplane mode on.
     * @return true if airplane mode on
    */
    public static boolean isAirPlaneMode() {
        boolean isAirPlaneMode = Settings.System.getInt(
                PhoneGlobals.getInstance().getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, -1) == 1;

        Log.d(TAG, "isAirPlaneMode = " + isAirPlaneMode);

        return isAirPlaneMode;
    }

    /**
     * Check lte data only mode.
     * @param context for getContentResolver
     * @return true if it is LteDataOnly mode
    */
    public static boolean is4GDataOnly(Context context) {
        if (context == null) {
            return false;
        }

        boolean result = false;
        int mainPhoneId = getMainPhoneId();
        Phone phone = PhoneFactory.getPhone(mainPhoneId);
        if (isCDMAPhone(phone)) {
            int networkMode = Settings.Global.getInt(context.getContentResolver(),
                    android.provider.Settings.Global.USER_PREFERRED_NETWORK_MODE + phone.getSubId(),
                    preferredNetworkMode);
            if (networkMode == Phone.NT_MODE_LTE_TDD_ONLY) {
                result = true;
           }
        }
        Log.d(TAG, "is4GDataOnly: " + result);

        return result;
    }

    /**
     * Check is svlte slot inserted.
     * @return true if svlte slot inserted
     */
    public static boolean isSvlteSlotInserted() {
        boolean result = false;

        int mainPhoneId = getMainPhoneId();
        Phone phone = PhoneFactory.getPhone(mainPhoneId);
        if (isCDMAPhone(phone)) {
            int slotId = SubscriptionManager.getSlotId(phone.getSubId());
            if (slotId != -1) {
                TelephonyManagerEx telephonyManagerEx = TelephonyManagerEx.getDefault();
                if (telephonyManagerEx != null) {
                    result = telephonyManagerEx.hasIccCard(slotId);
                }
            }
        }
        Log.d(TAG, "isSvlteSlotInserted = " + result);

        return result;
    }

    /**
     * Check Radio State by target slot.
     * @param slotId for check
     * @return true if radio is on
    */
    public static boolean getRadioStateForSlotId(final int slotId) {
        int currentSimMode = Settings.System.getInt(PhoneGlobals.getInstance().getContentResolver(),
                Settings.System.MSIM_MODE_SETTING, -1);
        boolean radiosState = ((currentSimMode & (MODE_PHONE1_ONLY << slotId)) == 0) ?
                false : true;
        Log.d(TAG, "soltId: " + slotId + ", radiosState : " + radiosState);

        return radiosState;
    }

    /**
     * Check is svlte slot Radio On.
     * @return true if svlte slot Radio on
     */
    public static boolean isSvlteSlotRadioOn() {
        boolean result = false;

        int mainPhoneId = getMainPhoneId();
        Phone phone = PhoneFactory.getPhone(mainPhoneId);
        if (isCDMAPhone(phone)) {
            int slotId = SubscriptionManager.getSlotId(phone.getSubId());
            result = slotId != -1 ? getRadioStateForSlotId(slotId) : false;
        }
        Log.d(TAG, "isSvlteSlotRadioOn = " + result);

        return result;
    }

    /**
     * Check whether Roaming or not.
     * @return true if Roaming
     */
    public static boolean isRoaming(Phone phone) {
        boolean result = false;

        ServiceState state = phone.getServiceState();
        if (state.getRoaming()) {
            result = true;
        }
        Log.d(TAG, "isSvlteRoaming? " + result);

        return result;
    }

    /**
     * Get the status whether there are two CDMA SIM cards are inserted.
     * And both them are in home network.
     * @return
     */
    public static boolean isMultiCdmaCardInsertedInHomeNetwork() {
        final int slotSum = TelephonyManager.getDefault().getPhoneCount();

        int insertedCdmaCardNum = 0;
        boolean isAllInHomeNetwork = true;
        int cardType = SIM_TYPE_NONE;

        for (int slotIndex = 0; slotIndex < slotSum; slotIndex++) {
            cardType = getSimType(slotIndex);
            /// CDMA card
            if (CT_SIM == cardType || C_G_SIM == cardType) {
                insertedCdmaCardNum++;
                /// Whether in home network
                int[] subIds = SubscriptionManager.getSubId(slotIndex);
                if (subIds != null &&
                        !TelephonyManagerEx.getDefault().isInHomeNetwork(subIds[0])) {
                    Log.d(TAG, "[isMultiCdmaCardInserted] subId " + subIds[0] + "out of home");
                    isAllInHomeNetwork = false;
                }
            }
        }
        Log.d(TAG, "[isMultiCdmaCardInserted] insertedCdmaCardNum = " + insertedCdmaCardNum
                + "isAllInHomeNetwork: " + isAllInHomeNetwork);

        if (insertedCdmaCardNum >= 2 && isAllInHomeNetwork) {
            return true;
        }
        return false;
    }

    /**
     * Get status of the sim cards inserted in UE.
     * @return true if one C card & one G card.
     */
    public static boolean isCGCardInserted() {
        final int slotSum = TelephonyManager.getDefault().getPhoneCount();
        int cardType = SIM_TYPE_NONE;
        int cCardNum = 0;
        int gCardNum = 0;

        boolean result = false;

        for(int slotIndex = 0; slotIndex < slotSum; slotIndex++) {
            cardType = getSimType(slotIndex);
            if (CT_SIM == cardType || C_G_SIM == cardType) {
                cCardNum += 1;
            } else if (GSM_SIM == cardType) {
                gCardNum += 1;
            }
        }

        Log.d(TAG, "cCardNum: " + cCardNum + " ,gCardNum: " + gCardNum);

        if (cCardNum == 1 && gCardNum == 1) {
            result = true;
        } else {
            result = false;
        }
        Log.d(TAG, "isCGCardInserted: " + result);

        return result;
    }

    /**
     * Get current g & main phone status on current UE
     * @return true is g & main phone, or not.
     */
    public static boolean isCapabilityOnGCard() {
        final int phoneNum = TelephonyManager.getDefault().getPhoneCount();
        boolean result = false;
        int mainPhoneId = getMainPhoneId();
        int cardType = getSimType(mainPhoneId);
        if(GSM_SIM == cardType) {
            result = true;
        } else {
            result = false;
        }
        Log.d(TAG, "isGMainPhoneExist: " + result);

        return result;
    }

    /**
     * Judge whether current phone is g & main phone or not
     * @param phoneId
     * @return
     */
    public static boolean isGCardInserted(int phoneId) {
        boolean result = false;
        int cardType = getSimType(phoneId);

        if (GSM_SIM == cardType) {
            result = true;
        } else {
            result = false;
        }
        Log.d(TAG, "isGCardInserted: " + result + " ,current phone: " + phoneId);

        return result;
    }

    /**
     * Get the main phone id
     * @return
     */
    public static int getMainPhoneId() {
        int mainPhoneId = SubscriptionManager.INVALID_PHONE_INDEX;

        String curr3GSim = SystemProperties.get(PROPERTY_3G_SIM, "");
        Log.d(TAG, "current 3G Sim = " + curr3GSim);

        if (!TextUtils.isEmpty(curr3GSim)) {
            int curr3GPhoneId = Integer.parseInt(curr3GSim);
            mainPhoneId = curr3GPhoneId - 1;
        }
        Log.d(TAG, "getMainPhoneId: " + mainPhoneId);

        return mainPhoneId;
    }

    /**
     * Switch capability to the designated phone
     * @param phoneId
     * @return
     */
    public static void setMainPhone(int phoneId) {
        final int phoneNum = TelephonyManager.getDefault().getPhoneCount();
        int[] phoneRat = new int[phoneNum];
        try {
            ITelephony iTel = ITelephony.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE));
            if (null == iTel) {
                Log.e(TAG, "Can not get phone service");
                return;
            }

            int currRat = iTel.getRadioAccessFamily(phoneId, "com.mediatek.settings.cdma");
            Log.d(TAG, "Current phoneRat:" + currRat);

            RadioAccessFamily[] rat = new RadioAccessFamily[phoneNum];
            for (int i = 0; i < phoneNum; i++) {
                if (phoneId == i) {
                    Log.d(TAG, "SIM switch to Phone: " + i);

                    phoneRat[i] = RadioAccessFamily.RAF_LTE
                            | RadioAccessFamily.RAF_UMTS
                            | RadioAccessFamily.RAF_GSM;
                } else {
                    phoneRat[i] = RadioAccessFamily.RAF_GSM;
                }
                rat[i] = new RadioAccessFamily(i, phoneRat[i]);
            }

            iTel.setRadioCapability(rat);

        } catch (RemoteException ex) {
            Log.d(TAG, "Set phone rat fail!!!");

            ex.printStackTrace();
        }
    }

    public static boolean isCapabilitySwitching() {
        boolean isSwitching = false;
        ITelephonyEx telephonyEx = ITelephonyEx.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
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
     * Check if phone has 4G capability.
     */
    public static boolean isCapabilityPhone(Phone phone) {
        boolean result = TelephonyUtilsEx.getMainPhoneId() == phone.getPhoneId();
        Log.d(TAG, "isCapabilityPhone result = " + result
                + " phoneId = " + phone.getPhoneId());

        return result;
    }

    public static boolean isCdma4gCard(int subId) {
        boolean result = false;
        CardType cardType = TelephonyManagerEx.getDefault().getCdmaCardType(
                SubscriptionManager.getSlotId(subId));
        if (cardType != null) {
            result = cardType.is4GCard();
        } else {
            Log.d(TAG, "isCdma4gCard: cardType == null ");
        }
        Log.d(TAG, "isCdma4gCard result = " + result + "; subId = " + subId);
        return result;
    }
}
