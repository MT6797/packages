package com.mediatek.providers.contacts;

import android.annotation.SuppressLint;

import android.telephony.TelephonyManager;
import android.util.SparseArray;

import com.android.internal.telephony.PhoneConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * M: [Gemini+] A helper class for multi icc card support
 * all slot related method defined here.
 */
public final class SlotUtils {


    private static final int PHONE_SLOT_NUM = TelephonyManager.getDefault().getSimCount();
    private static final int FIRST_SLOT_ID = PhoneConstants.SIM_ID_1;

    private static final String TAG = SlotUtils.class.getSimpleName();

    private SlotUtils() {
    }

    /**
     * M: slot info package.
     */
    private static final class SlotInfo {

        int mSlotId;

        public SlotInfo(int slotId) {
            mSlotId = slotId;
        }
    }

    @SuppressLint("UseSparseArrays")
    private static Map<Integer, SlotInfo> sSlotInfoMap = new HashMap<Integer, SlotInfo>();
    static {
        for (int i = 0; i < PHONE_SLOT_NUM; i++) {
            int slotId = FIRST_SLOT_ID + i;
            sSlotInfoMap.put(slotId, new SlotInfo(slotId));
        }
    }

    private static final SparseArray<SparseArray<String>> SIM_ACCOUNT_NAME_ARRAY =
            new SparseArray<SparseArray<String>>(SlotUtils.getSlotCount());
    static {
        for (int slotId : getAllSlotIds()) {
            SparseArray<String> accountNamesForSlot = new SparseArray<String>();
            accountNamesForSlot.put(SimCardUtils.SimType.SIM_TYPE_SIM,
                    SimCardUtils.SimType.SIM_TYPE_SIM_TAG + slotId);
            accountNamesForSlot.put(SimCardUtils.SimType.SIM_TYPE_USIM,
                    SimCardUtils.SimType.SIM_TYPE_USIM_TAG + slotId);
            accountNamesForSlot.put(SimCardUtils.SimType.SIM_TYPE_UIM,
                    SimCardUtils.SimType.SIM_TYPE_UIM_TAG + slotId);
            accountNamesForSlot.put(SimCardUtils.SimType.SIM_TYPE_CSIM,
                    SimCardUtils.SimType.SIM_TYPE_CSIM_TAG + slotId);
            SIM_ACCOUNT_NAME_ARRAY.put(slotId, accountNamesForSlot);
        }
    }

    public static Integer[] getAllSlotIds() {
        return sSlotInfoMap.keySet().toArray(new Integer[sSlotInfoMap.size()]);
    }

    public static int getSlotCount() {
        return sSlotInfoMap.size();
    }

    public static int getFirstSlotId() {
        return FIRST_SLOT_ID;
    }

    /**
     * M: get an invalid slot id, to indicate that this is not a sim slot.
     *
     * @return negative value
     */
    public static int getNonSlotId() {
        return -1;
    }

    /**
     * M: in single card phone, the only slot has a slot id this method to
     * retrieve the id.
     *
     * @return the only slot id of a single card phone
     */
    public static int getSingleSlotId() {
        return FIRST_SLOT_ID;
    }

    /**
     * M: get SIM name.
     * @param slotId slotId
     * @return Names
     */
    public static Collection<String> getPossibleSimAccountNamesForSlot(int slotId) {
        SparseArray<String> accountNameArray = SIM_ACCOUNT_NAME_ARRAY.get(slotId);
        Collection<String> accountNames = new ArrayList<String>();
        for (int i = 0; i < accountNameArray.size(); ++i) {
            accountNames.add(accountNameArray.valueAt(i));
        }
        return accountNames;
    }

    /**
     * M: get SimAccountName by Slot.
     * @param simType simType
     * @param slotId slotId
     * @return name
     */
    public static String getSimAccountNameForSlot(int simType, int slotId) {
        return SIM_ACCOUNT_NAME_ARRAY.get(slotId).get(simType);
    }
}
