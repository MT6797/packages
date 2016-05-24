/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 */

package com.mediatek.settings.cdma;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.sim.SimDialogActivity;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.sim.TelephonyUtils;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.List;

public class CdmaUtils {

    private static final String TAG = "CdmaUtils";

    private static final String CDMA_SIM_DIALOG = "com.mediatek.settings.cdma.SIM_DIALOG";

    // for [C2K OMH Warning]
    private static final String SUB_INFO = "sub_info";
    private static final String NON_OMH_SUB_LIST = "non_omh_sub_list";

    /**
     * check whether the card inserted is a CDMA card and
     * working in CDMA mode (the modem support CDMA).
     * Also see {@link #isCdmaCard(int)}
     * @param subId sub Id
     * @return
     */
    public static boolean isSupportCdma(int subId) {
        boolean isSupportCdma = false;
        if (TelephonyManager.getDefault().getCurrentPhoneType(subId)
                == TelephonyManager.PHONE_TYPE_CDMA) {
            isSupportCdma = true;
        }
        Log.d(TAG, " isSupportCdma = " + isSupportCdma + ", subId = " + subId);
        return isSupportCdma;
    }

    /**
     * check whether the card inserted is really a CDMA card.
     * NOTICE that it will return true even the card is a CDMA card but working as a GSM SIM.
     * Also see {@link #isSupportCdma(int)}
     * @param slotId slot Id
     * @return
     */
    public static boolean isCdmaCard(int slotId) {
        boolean isCdmaCard = false;
        if ((TelephonyManagerEx.getDefault().getIccAppFamily(slotId)
                & TelephonyManagerEx.APP_FAM_3GPP2) != 0) {
            isCdmaCard = true;
        }
        Log.d(TAG, "slotId = " + slotId + " isCdmaCard = " + isCdmaCard);
        return isCdmaCard;
    }

    /**
     * check the CDMA SIM inserted status, launch a warning dialog if two new CDMA SIM detected.
     *
     * @param context
     *            Context
     * @param simDetectNum
     *            New SIM number detected
     */
    public static void checkCdmaSimStatus(Context context, int simDetectNum) {
        Log.d(TAG, "startCdmaWaringDialog," + " simDetectNum = " + simDetectNum);
        boolean twoCdmaInsert = true;
        if (simDetectNum > 1) {
            for (int i = 0; i < simDetectNum; i++) {
                if (!isCdmaCard(i)) {
                    twoCdmaInsert = false;
                }
            }
        } else {
            twoCdmaInsert = false;
        }

        Log.d(TAG, "twoCdmaInsert = " + twoCdmaInsert);
        if (twoCdmaInsert) {
            Intent intent = new Intent(CDMA_SIM_DIALOG);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            intent.putExtra(CdmaSimDialogActivity.DIALOG_TYPE_KEY,
                    CdmaSimDialogActivity.TWO_CDMA_CARD);
            context.startActivity(intent);
        }
    }

    /**
     * enter {@link CdmaSimDialogActivity} activity.
     * @param context context
     * @param targetSubId subId
     * @param actionType type
     */
    public static void startAlertCdmaDialog(Context context, int targetSubId, int actionType) {
        Intent intent = new Intent(CDMA_SIM_DIALOG);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtra(CdmaSimDialogActivity.DIALOG_TYPE_KEY,
                CdmaSimDialogActivity.ALERT_CDMA_CARD);
        intent.putExtra(CdmaSimDialogActivity.TARGET_SUBID_KEY, targetSubId);
        intent.putExtra(CdmaSimDialogActivity.ACTION_TYPE_KEY, actionType);
        context.startActivity(intent);
    }

    /**
     * For C2K C+C case, only one SIM card register network, other card can recognition.
     * and can not register the network
     * 1. two CDMA cards.
     * 2. two cards is competitive. only one modem can register CDMA network.
     * @param context context
     * @return true
     */
    public static boolean isCdmaCardCompetion(Context context) {
        boolean isCdmaCard = true;
        boolean isCompetition = true;
        int simCount = 0;
        if (context != null) {
            simCount = TelephonyManager.from(context).getSimCount();
        }
        if (simCount == 2) {
            for (int i = 0; i < simCount ; i++) {
                isCdmaCard = isCdmaCard && isCdmaCard(i);
                SubscriptionInfo subscriptionInfo =
                        SubscriptionManager.from(context).
                        getActiveSubscriptionInfoForSimSlotIndex(i);
                if (subscriptionInfo != null) {
                    isCompetition = isCompetition &&
                            TelephonyManagerEx.getDefault().isInHomeNetwork(
                                    subscriptionInfo.getSubscriptionId());
                } else {
                    isCompetition = false;
                    break;
                }
            }
        } else {
            isCdmaCard = false;
            isCompetition = false;
        }
        Log.d(TAG, "isCdmaCard: " + isCdmaCard + " isCompletition: " + isCompetition
                + " is Suppport SIM switch: " + FeatureOption.MTK_DISABLE_CAPABILITY_SWITCH);
        return isCdmaCard && isCompetition && (!FeatureOption.MTK_DISABLE_CAPABILITY_SWITCH);
    }

    /**
     * 1. two CDMA cards.
     * 2. two cards is competitive. only one modem can register CDMA network.
     * @param context Context
     * @return true
     */
    public static boolean isCdmaCardCompetionForData(Context context) {
        return isCdmaCardCompetion(context);
    }

    /**
     * 1. two CDMA cards.
     * 2. two cards is competitive. only one modem can register CDMA network.
     * 3. valid subId
     * 4 .capability switch
     * @param context context
     * @param targetItem targetItem
     * @return true
     */
    public static boolean isCdmaCardCompetionForSms(Context context, int targetItem) {
        Log.d(TAG, "targetItem: " + targetItem);
        return SubscriptionManager.isValidSubscriptionId(targetItem)
                && isCdmaCardCompetion(context)
                && (TelephonyUtils.getMainCapabilitySlotId(context) !=
                SubscriptionManager.getSlotId(targetItem));
    }

    /**
     * 1. user click SIM card.
     * 2. two CDMA cards.
     * 3. two cards is competitive. only one modem can register CDMA network.
     * 4. capability switch
     * @param context context
     * @param targetItem targetItem
     * @return true
     */
    public static boolean isCdmaCardCompetionForCalls(Context context, int targetItem) {
        boolean shouldDisplay = false;
        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        if (context != null) {
            final TelecomManager telecomManager = TelecomManager.from(context);
            final List<PhoneAccountHandle> phoneAccountsList =
                    telecomManager.getCallCapablePhoneAccounts();
            PhoneAccountHandle handle =
                    targetItem < 1 ? null : phoneAccountsList.get(targetItem - 1);
            subId = TelephonyUtils.phoneAccountHandleTosubscriptionId(context, handle);
        }
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            shouldDisplay = TelephonyUtils.getMainCapabilitySlotId(context) !=
                    SubscriptionManager.getSlotId(subId);
        }
        Log.d(TAG, "shouldDisplay: " + shouldDisplay + " targetItem: " + targetItem);
        return shouldDisplay && isCdmaCardCompetion(context);
    }

    /**
     * For C2K signal disturb, When CDMA can not coexist with LTE and WCDMA,
     * should caution user the result than change default data.
     * 1. two SIM cards.
     * 2. one SIM Card is CDMA card, other is GSM card
     * 3. change default card to G card
     * 4. the main capability is CDMA card
     *
     * @param context context
     * @param targetSubId targetSubId
     * @return true can not coexist
     */
    public static boolean isSwitchCdmaCardToGsmCard(Context context, int targetSubId) {
        boolean isGsmCardForTarget = false;
        boolean isCdmaCardForMainCapability = false;
        if (!FeatureOption.MTK_C2K_SLOT2_SUPPORT
                && SubscriptionManager.isValidSubscriptionId(targetSubId)
                && (!FeatureOption.MTK_DISABLE_CAPABILITY_SWITCH)) {
            int mainCapabilitySlotId = TelephonyUtils.getMainCapabilitySlotId(context);
            isCdmaCardForMainCapability = CdmaUtils.isCdmaCard(mainCapabilitySlotId);
            int targetDataSlotId = SubscriptionManager.getSlotId(targetSubId);
            isGsmCardForTarget = !CdmaUtils.isCdmaCard(targetDataSlotId);
        }
        Log.d(TAG, "isGsmCardForTarget: " + isGsmCardForTarget
                + " isCdmaCardForMainCapability: " + isCdmaCardForMainCapability
                + " sim switch is support: " + FeatureOption.MTK_DISABLE_CAPABILITY_SWITCH);
        return isGsmCardForTarget && isCdmaCardForMainCapability;
    }

    /**
     * For C2K signal disturb, and the Main Capability is GSM Card
     * and change default from default card to CDMA Card. should switch
     * main capability to target subId by reason of C Card can not
     * register network now
     * 1. two SIM cards.
     * 2. one SIM Card is CDMA card, other is GSM card
     * 3. change default card to C card
     * 4. the main capability is GSM card
     * @param context context
     * @param targetSubId targetSubId
     * @return true switch
     */
    public static boolean shouldSwitchCapability(Context context, int targetSubId) {
        boolean isCdmaCardForTarget = false;
        boolean isGsmCardForMainCapability = false;
        if (!FeatureOption.MTK_C2K_SLOT2_SUPPORT
                && SubscriptionManager.isValidSubscriptionId(targetSubId)
                && (!FeatureOption.MTK_DISABLE_CAPABILITY_SWITCH)) {
           int mainCapabilitySlotId = TelephonyUtils.getMainCapabilitySlotId(context);
            isGsmCardForMainCapability = !CdmaUtils.isCdmaCard(mainCapabilitySlotId);
            int targetSmsSlotId = SubscriptionManager.getSlotId(targetSubId);
            isCdmaCardForTarget = CdmaUtils.isCdmaCard(targetSmsSlotId);
        }
        Log.d(TAG, "isCdmaCardForTarget: " + isCdmaCardForTarget
                + " isGsmCardForMainCapability: " + isGsmCardForMainCapability
                + " sim switch is support: " + FeatureOption.MTK_DISABLE_CAPABILITY_SWITCH);
        return isCdmaCardForTarget && isGsmCardForMainCapability;
    }

    /**
     * For C2K signal disturb, and the Main Capability is GSM Card
     * and change default from default card to CDMA Card.should switch
     * main capability to target subId by reason of C Card can not
     * register network now.
     * 1. two SIM cards.
     * 2. one SIM Card is CDMA card, other is GSM card
     * 3. change default card to C card
     * 4. the main capability is GSM card
     * @param context context
     * @param handle handle
     * @return true switch
     */
    public static boolean shouldSwichCapabilityForCalls(Context context,
            PhoneAccountHandle handle) {
        boolean isCdmaCardForTarget = false;
        boolean isGsmCardForMainCapability = false;
        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        if (context != null) {
            subId = TelephonyUtils.phoneAccountHandleTosubscriptionId(context, handle);
        }
        if (SubscriptionManager.isValidSubscriptionId(subId)
                && !FeatureOption.MTK_C2K_SLOT2_SUPPORT
                && (!FeatureOption.MTK_DISABLE_CAPABILITY_SWITCH)) {
            int mainCapabilitySlotId = TelephonyUtils.getMainCapabilitySlotId(context);
            isGsmCardForMainCapability = !CdmaUtils.isCdmaCard(mainCapabilitySlotId);
            int targetCallsSlotId = SubscriptionManager.getSlotId(subId);
            isCdmaCardForTarget = CdmaUtils.isCdmaCard(targetCallsSlotId);
        }
        Log.d(TAG, "isCdmaCardForTarget: " + isCdmaCardForTarget
                + " isGsmCardForMainCapability: " + isGsmCardForMainCapability
                + " sim switch is support: " + FeatureOption.MTK_DISABLE_CAPABILITY_SWITCH);
        return isCdmaCardForTarget && isGsmCardForMainCapability;
    }

    /**
     * one card is CDMA card. other is GSM card.
     * @param context context
     * @return true
     */
    public static boolean isCdamCardAndGsmCard(Context context) {
        boolean isCdmaCard = false;
        boolean isGsmCard = false;
        int simCount = 0;
        if (context != null) {
            simCount = TelephonyManager.from(context).getSimCount();
        }
        for (int i = 0; i < simCount; i++) {
            if (CdmaUtils.isCdmaCard(i)) {
                isCdmaCard = true;
            } else {
                isGsmCard = true;
            }
        }
        Log.d(TAG, "isCdmaCard: " + isCdmaCard + " isGsmCard: " + isGsmCard
                + " solution2 support: " + FeatureOption.MTK_C2K_SLOT2_SUPPORT
                + " sim switch is support: " + FeatureOption.MTK_DISABLE_CAPABILITY_SWITCH);
        return isCdmaCard && isGsmCard && (!FeatureOption.MTK_C2K_SLOT2_SUPPORT)
                && (!FeatureOption.MTK_DISABLE_CAPABILITY_SWITCH);
    }

    /**
     * check whether the specified sub is an non-OMH R-UIM, it will always return false if the sub
     * do not support OMH
     * @param subId target sub
     * @return true if the sub is an OMH sub
     */
    public static boolean isNonOmhSimInOmhDevice(int subId) {
        boolean isOmhEnable = TelephonyManagerEx.getDefault().isOmhEnable(subId);
        boolean isOmhCard = TelephonyManagerEx.getDefault().isOmhCard(subId);
        Log.d(TAG, "isOmhEnable = " + isOmhEnable + "isOmhCard = " + isOmhCard);
        return isOmhEnable && !isOmhCard;
    }

    /**
     * start the OMH warning dialog
     * @param context context
     */
    public static void startOmhWarningDialog(Context context) {
        Intent intent = new Intent(CDMA_SIM_DIALOG);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtra(CdmaSimDialogActivity.DIALOG_TYPE_KEY,
                CdmaSimDialogActivity.ALERT_OMH_WARNING);
        context.startActivity(intent);
    }

    /**
     * start the OMH data pick dialog to let user confirm
     * @param context context
     * @param targetSubId subId
     * @param actionType type
     */
    public static void startOmhDataPickDialog(Context context, int targetSubId) {
        Intent intent = new Intent(CDMA_SIM_DIALOG);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtra(CdmaSimDialogActivity.DIALOG_TYPE_KEY,
                CdmaSimDialogActivity.ALERT_OMH_DATA_PICK);
        intent.putExtra(CdmaSimDialogActivity.TARGET_SUBID_KEY, targetSubId);
        intent.putExtra(CdmaSimDialogActivity.ACTION_TYPE_KEY, SimDialogActivity.DATA_PICK);
        context.startActivity(intent);
    }

    /**
     * record the sub id into Non-OMH List
     * @param context
     * @param subId
     */
    public static void recordNonOmhSub(Context context, int subId) {
        Log.d(TAG, "recordNonOmhSub, subId = " + subId);
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return;
        }
        SharedPreferences sp = context.getSharedPreferences(SUB_INFO,
                Context.MODE_PRIVATE);
        String subList = sp.getString(NON_OMH_SUB_LIST,"");
        Log.d(TAG, "recordNonOmhSub, subList = " + subList);
        StringBuilder builder = new StringBuilder(subList);
        if (subList.isEmpty()) {
            builder.append(Integer.toString(subId));
        } else {
            builder.append(",").append(Integer.toString(subId));
        }
        sp.edit().putString(NON_OMH_SUB_LIST, builder.toString()).commit();
    }

    /**
     * check whether the Non-OMH sub was inserted before
     * @param context
     * @param subId
     * @return
     */
    public static boolean hasNonOmhRecord(Context context, int subId) {
        SharedPreferences sp = context.getSharedPreferences(SUB_INFO, Context.MODE_PRIVATE);
        String subList = sp.getString(NON_OMH_SUB_LIST, "");
        Log.d(TAG, "hasNonOmhRecord, subId = " + subId + ", subList = " + subList);
        String[] records = subList.split(",");
        if (records != null) {
            for (int i = 0; i < records.length; i++) {
                if (!TextUtils.isEmpty(records[i]) && TextUtils.isDigitsOnly(records[i])
                        && Integer.parseInt(records[i]) == subId) {
                    Log.d(TAG, "record hit~~");
                    return true;
                }
            }
        }
        return false;
    }
}
