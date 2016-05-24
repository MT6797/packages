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
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
package com.mediatek.contacts.util;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.net.Uri;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.RadioAccessFamily;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.contacts.common.R;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;

import com.mediatek.contacts.ExtensionManager;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.telecom.TelecomManagerEx;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.widget.CustomAccountRemoteViews.AccountInfo;
import com.mediatek.widget.DefaultAccountSelectionBar;

import java.util.ArrayList;
import java.util.List;

public class SetIndicatorUtils {
    private static final String TAG = "SetIndicatorUtils";

    private static final String PEOPLEACTIVITY = "com.android.contacts.activities.PeopleActivtiy";
    private static final String QUICKCONTACTACTIVITY =
            "com.android.contacts.quickcontact.QuickContactActivity";
    private static final String INDICATE_TYPE = "CONTACTS";
    private static final String ACTION_OUTGOING_CALL_PHONE_ACCOUNT_CHANGED =
            "com.android.contacts.ACTION_OUTGOING_CALL_PHONE_ACCOUNT_CHANGED";
    private static final String PROPERTY_3G_SIM = "persist.radio.simswitch";
    public static final String EXTRA_ACCOUNT = "extra_account";

    private static SetIndicatorUtils sInstance = null;
    private DefaultAccountSelectionBar mDefaultAccountSelectionBar = null;
    private boolean mShowSimIndicator = false;
    private BroadcastReceiver mReceiver = null;

    // In PeopleActivity, if quickContact is show, quickContactIsShow = true,
    // PeopleActivity.onPause cannot hide the Indicator.
    private boolean mIsQuickContactShow = false;
    private Activity mActivity = null;
    private boolean mIsRegister = false;

    public static SetIndicatorUtils getInstance() {
        if (sInstance == null) {
            sInstance = new SetIndicatorUtils();
        }
        return sInstance;
    }

    public void showIndicator(Activity activity, boolean visible) {
        if (UserHandle.myUserId() != UserHandle.USER_OWNER) {
            // None user owner don't show notification bar.
            Log.i(TAG, "[showIndicator]None user owner don't show notification bar.");
            return;
        }

        Log.i(TAG, "[showIndicator]visible : " + visible);
        mActivity = activity;
        mShowSimIndicator = visible;

        if (mDefaultAccountSelectionBar == null) {
            Context context = mActivity;
            mDefaultAccountSelectionBar = new DefaultAccountSelectionBar(context,
                    context.getPackageName(), null);
        }

        setSimIndicatorVisibility(visible);
    }

    public void registerReceiver(Activity activity) {
        Log.d(TAG, "[registerReceiver] activity : " + activity + ",register:" + mIsRegister);
        if (!mIsRegister) {
            if (mReceiver == null) {
                mReceiver = new MyBroadcastReceiver();
            }

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_OUTGOING_CALL_PHONE_ACCOUNT_CHANGED);
            intentFilter.addAction(TelecomManagerEx.ACTION_PHONE_ACCOUNT_CHANGED);
            activity.registerReceiver(mReceiver, intentFilter);
            mIsRegister = true;
        }
    }

    public void unregisterReceiver(Activity activity) {
        Log.d(TAG, "[unregisterReceiver] activity : " + activity + ",unregister:"
                + mIsRegister);
        if (mIsRegister) {
            activity.unregisterReceiver(mReceiver);
            mIsRegister = false;
            /// M: Clear the instance so we can get a new one when Activity recreated.
            sInstance = null;
        }
    }

    private SetIndicatorUtils() {

    }

    public void setSimIndicatorVisibility(boolean visible) {
        if (visible) {
            List<AccountInfo> accountList = getPhoneAccountInfos(mActivity);
            Log.d(TAG, "[setSimIndicatorVisibility] accountList size " + accountList.size());
            if (accountList.size() > 2) {
                mDefaultAccountSelectionBar.updateData(accountList);
                mDefaultAccountSelectionBar.show();
                showSIMIndicatorAtStatusbar(mActivity);
            } else {
                mDefaultAccountSelectionBar.hide();
                hideSIMIndicatorAtStatusbar(mActivity);
                mShowSimIndicator = false;
            }
            registerReceiver(mActivity);

        } else {
            mDefaultAccountSelectionBar.hide();
            mShowSimIndicator = false;
            unregisterReceiver(mActivity);
            hideSIMIndicatorAtStatusbar(mActivity);
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ACTION_OUTGOING_CALL_PHONE_ACCOUNT_CHANGED.equals(action)) {
                /* For solution2 C+C case, Show a confirmation dialog to user when they
                 * switch account between two CDMA cards. If click "Ok", switch to the
                 * selected account. If click "Cancel", do nothing.
                 */
                if (isAccountSwitchBetweenTwoCdmaCards(context, intent)) {
                    hideNotification();
                    if (!TelephonyManagerEx.getDefault().canSwitchDefaultSubId()) {
                        showAlertToast(context, R.string.can_not_switch_account_temporarily);
                    } else {
                        showConfirmDialog(intent);
                    }
                    return;
                }

                /* For solution1.5 C+G case, only support one CDMA card, when GSM card has
                 * 4G capability, the CDMA card can't register network.
                 * When switch call account to CDMA card, need switch 3G/4G capability to CDMA card.
                 */
                if (!isC2KSolution2Support()) {
                    if (isSiwtchAccountAllowed(context)) {
                        if (isSwitchFromGsmCardToCdmaCard(context, intent)) {
                            if(!setRadioCapability(context, intent)) {
                                Log.d(TAG, "Fail to set 3G/4G capability, return");
                                hideNotification();
                                return;
                            }
                        }
                    } else {
                        Log.d(TAG, "Not allowed to switch account in C+G case, return");
                        hideNotification();
                        return;
                    }
                }
                if (mShowSimIndicator) {
                    updateSelectedAccount(intent);
                    setSimIndicatorVisibility(true);
                }
                hideNotification();
            } else if (TelecomManagerEx.ACTION_PHONE_ACCOUNT_CHANGED.equals(action)) {
                setSimIndicatorVisibility(true);
            }
        }
    }

    private void hideNotification() {
        Intent intent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        mActivity.sendBroadcast(intent);
    }

    private List<AccountInfo> getPhoneAccountInfos(Context context) {
        TelecomManager telecomManager = (TelecomManager) context
                .getSystemService(Context.TELECOM_SERVICE);
        List<PhoneAccountHandle> accountHandles = telecomManager.getCallCapablePhoneAccounts();

        Log.d(TAG, "[getPhoneAccountInfos] accountHandles.size" + accountHandles.size());
        List<AccountInfo> accountInfos = new ArrayList<AccountInfo>();
        PhoneAccountHandle selectedAccountHandle = telecomManager
                .getUserSelectedOutgoingPhoneAccount();
        // Add the always ask item
        AccountInfo alwaysAskInfo = createAlwaysAskAccountInfo(context,
                selectedAccountHandle == null);
        accountInfos.add(alwaysAskInfo);

        for (PhoneAccountHandle handle : accountHandles) {
            final PhoneAccount account = telecomManager.getPhoneAccount(handle);
            if (account == null) {
                continue;
            }

            String label = account.getLabel() != null ? account.getLabel().toString() : null;
            Uri sddress = account.getAddress();
            Uri subAddress = account.getSubscriptionAddress();
            String number = null;

            if (subAddress != null) {
                number = subAddress.getSchemeSpecificPart();
            } else if (sddress != null) {
                number = sddress.getSchemeSpecificPart();
            }

            Intent intent = new Intent(ACTION_OUTGOING_CALL_PHONE_ACCOUNT_CHANGED);
            intent.putExtra(EXTRA_ACCOUNT, handle);
            boolean isSelected = false;

            if (handle.equals(selectedAccountHandle)) {
                isSelected = true;
            }
            boolean isSubAccount = isSubscriptionAccount(context, handle);
            AccountInfo info = new AccountInfo(0 /*account.getIcon().getResId()*/,
                    drawableToBitmap(account.getIcon().loadDrawable(context)),
                    label, number, intent, isSelected, isSubAccount);
            accountInfos.add(info);
        }
        return accountInfos;
    }

    private AccountInfo createAlwaysAskAccountInfo(Context context, boolean isSelected) {
        Intent intent = new Intent(ACTION_OUTGOING_CALL_PHONE_ACCOUNT_CHANGED);
        String label = context.getString(com.mediatek.R.string.account_always_ask_title);
        int iconResId = ExtensionManager.getInstance().getCtExtension()
                .showAlwaysAskIndicate(com.mediatek.R.drawable.account_always_ask_icon);
        Log.d(TAG, "[createAlwaysAskAccountInfo] iconResId : " + iconResId);
        return new AccountInfo(iconResId, null, label, null, intent, isSelected);
    }

    public void updateSelectedAccount(Intent intent) {
        PhoneAccountHandle handle = (PhoneAccountHandle) intent.getParcelableExtra(EXTRA_ACCOUNT);
        Context context = mActivity;
        TelecomManager telecomManager = (TelecomManager) context
                .getSystemService(Context.TELECOM_SERVICE);

        telecomManager.setUserSelectedOutgoingPhoneAccount(handle);
    }

    /**
     * DefaultAccountSelectionBar only accept the bitmap, so if drawable, need
     * covert it to bitmap.
     *
     * @param drawable
     *            the original drawable.
     * @return the converted bitmap.
     */
    private Bitmap drawableToBitmap(Drawable drawable) {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
        return bitmapDrawable.getBitmap();
    }

    /**
     * Show the under line indicator for default SIM
     * @param context the Context
     */
    public static void showSIMIndicatorAtStatusbar(Context context) {
        TelecomManager telecomManager = (TelecomManager) context
                .getSystemService(Context.TELECOM_SERVICE);
        PhoneAccountHandle selectedAccountHandle = telecomManager
                .getUserSelectedOutgoingPhoneAccount();
        PhoneAccount account = telecomManager.getPhoneAccount(selectedAccountHandle);
        ComponentName sipComponentName = new ComponentName("com.android.phone",
                "com.android.services.telephony.sip.SipConnectionService");
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int subId = telephonyManager.getSubIdForPhoneAccount(account);
        StatusBarManager statusbar = (StatusBarManager) context
                .getSystemService(Context.STATUS_BAR_SERVICE);
        if (selectedAccountHandle == null) {
            // Call Statusbar api to show always ask (-1)
            statusbar.showDefaultAccountStatus(StatusBarManager.STATUS_ALWAYS_ASK);
        } else if (account != null
                && account.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)) {
            // Call Statusbar api to Show sim indicator with SubId
            statusbar.showDefaultAccountStatus(subId);
        } else if (selectedAccountHandle.getComponentName().equals(sipComponentName)) {
            // It is SIP phone account
            statusbar.showDefaultAccountStatus(StatusBarManager.STATUS_SIP);
        } else {
            // Call Statusbar api to hide sim indicator
            statusbar.hideDefaultAccountStatus();
        }
    }

    /**
     * Hide the under line indicator for default SIM
     * @param context the Context
     */
    public static void hideSIMIndicatorAtStatusbar(Context context) {
        // Call Statusbar api to hide sim indicator
        StatusBarManager statusbar = (StatusBarManager) context
                .getSystemService(Context.STATUS_BAR_SERVICE);
        statusbar.hideDefaultAccountStatus();
    }

    private boolean isAccountSwitchBetweenTwoCdmaCards(final Context context, final Intent intent) {
        int simCount = TelephonyManager.from(context).getSimCount();
        if (simCount < 2) {
            return false;
        }

        PhoneAccountHandle selectedHandle = (PhoneAccountHandle) intent.getParcelableExtra(
                EXTRA_ACCOUNT);
        if (selectedHandle == null || !isSubscriptionAccount(context, selectedHandle)) {
            Log.d(TAG, "It is not SubScriptionAccount ");
            return false;
        }
        int defaultDataSubId = SubscriptionManager.from(context).getDefaultDataSubId();
        int selectedSubId = TelephonyManager.from(context).getSubIdForPhoneAccount(
                TelecomManager.from(context).getPhoneAccount(selectedHandle));
        if (!SubscriptionManager.isValidSubscriptionId(selectedSubId) ||
                selectedSubId == defaultDataSubId) {
            Log.d(TAG, "It is invalid subId, or it is defaultDataSub. " + selectedSubId);
            return false;
        }

        int selectedSlotId = SubscriptionManager.getSlotId(selectedSubId);
        int defaultSlotId = SubscriptionManager.getSlotId(defaultDataSubId);
        // Judge weather the accounts are CDMA cards and they are in home network.
        if (isCDMACardAndInHomeNetwork(selectedSlotId, selectedSubId) &&
                isCDMACardAndInHomeNetwork(defaultSlotId, defaultDataSubId)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isCDMACardAndInHomeNetwork(int slotId, int subId) {
        if (isCdmaCard(slotId) &&
                TelephonyManagerEx.getDefault().isInHomeNetwork(subId)) {
            return true;
        }
        Log.d(TAG, "this account isn't CDMA card or it isn't in home network, slotId = "
                + slotId + ", subId = " + subId);
        return false;
    }

    private void showConfirmDialog(final Intent intent) {
        if (mActivity.isFinishing() || mActivity.isDestroyed()) {
            return;
        }

        FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
        Fragment prev = mActivity.getFragmentManager().findFragmentByTag(
                TwoCdmaCardsConfirmDialog.DIALOG_TAG);
        if (prev != null) {
            ((DialogFragment) prev).dismiss();
        }
        ft.addToBackStack(null);

        DialogFragment newFragment = TwoCdmaCardsConfirmDialog.getInstance(intent);
        newFragment.show(ft, TwoCdmaCardsConfirmDialog.DIALOG_TAG);
    }

    public boolean isNotificationShown() {
        return mShowSimIndicator;
    }

    /**
     * In C + G case and 4G capability on Gsm card, it is not allowed to switch account
     * when modem reset, in call or in airplane mode.
     */
    private boolean isSiwtchAccountAllowed(Context context) {
        int cdmaCardNum = 0;
        int gsmCardNum = 0;
        int simCount = TelephonyManager.from(context).getSimCount();
        for (int slotId = 0; slotId < simCount && simCount >= 2; slotId++) {
            int[] subId = SubscriptionManager.getSubId(slotId);
            if (subId != null && subId.length > 0) {
                if (isCdmaCard(slotId)
                        && TelephonyManagerEx.getDefault().isInHomeNetwork(subId[0])) {
                    cdmaCardNum ++;
                } else {
                    gsmCardNum ++;
                }
            }
        }
        int mainCapabilitySlotId = getMainCapabilitySlotId();
        boolean isGsmCardHasMainCapability = isGsmCard(mainCapabilitySlotId);
        if (cdmaCardNum > 0 && gsmCardNum > 0) {
            if (isGsmCardHasMainCapability && isAirplaneModeOn(context)) {
                showAlertToast(context, R.string.can_not_switch_account_temporarily);
                return false;
            }

            ITelephonyEx iTelEx = ITelephonyEx.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
            try {
                if (iTelEx != null && iTelEx.isCapabilitySwitching()) {
                    showAlertToast(context, R.string.can_not_switch_account_temporarily);
                    return false;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.d(TAG, "fail to judge isCapabilitySwitching, RemoteException");
            }

            if (TelecomManager.from(context).isInCall()) {
                showAlertToast(context, R.string.can_not_switch_account_during_call);
                return false;
            }
        }
        return true;
    }

    /**
     * For C2K solution1.5, judge whether changing account from GSM card to CDMA card,
     * need check the following three items:
     * 1. has two or more SIMcard
     * 2. the selected account is CDMA card
     * 3. the 3G/4G capability is on GSM card
     * @return true if switch from GSM card to CDMA card
     */
    private boolean isSwitchFromGsmCardToCdmaCard(Context context, Intent intent) {
        boolean isSelectedCdmaCard = false;
        boolean isSelectedCdmaCardInHome = false;
        boolean isGsmCardHasMainCapability = false;

        final int simCount = TelephonyManager.getDefault().getSimCount();
        if (simCount < 2) {
            Log.d(TAG, "IsSwitchOnTwoCTKCard simCount = " + simCount + ", return false");
            return false;
        }

        PhoneAccountHandle selectedHandle = (PhoneAccountHandle) intent.getParcelableExtra(
                EXTRA_ACCOUNT);
        if (selectedHandle == null || !isSubscriptionAccount(context, selectedHandle)) {
            Log.d(TAG, "It is not SubScriptionAccount ");
            return false;
        }

        int selectedSubId = TelephonyManager.from(context).getSubIdForPhoneAccount(
                TelecomManager.from(context).getPhoneAccount(selectedHandle));
        if (SubscriptionManager.isValidSubscriptionId(selectedSubId)) {
            int selectedSlotId = SubscriptionManager.getSlotId(selectedSubId);
            // Which slot has the Main Capability(3G/4G).
            int mainCapabilitySlotId = getMainCapabilitySlotId();
            isGsmCardHasMainCapability = isGsmCard(mainCapabilitySlotId);
            isSelectedCdmaCard = isCdmaCard(selectedSlotId);
            if (isSelectedCdmaCard) {
                isSelectedCdmaCardInHome = TelephonyManagerEx.getDefault().isInHomeNetwork(
                        selectedSubId);
            }
        }
        Log.d(TAG, ", isSelectedCdmaCard = " + isSelectedCdmaCard + ", isSelectedCdmaCardInHome = "
                + isSelectedCdmaCardInHome + ", isGsmCardHasMainCapability = "
                + isGsmCardHasMainCapability);
        return isGsmCardHasMainCapability && isSelectedCdmaCard && isSelectedCdmaCardInHome;
    }

    /**
     * Set the 3G/4G capability of the SIM card
     * @param intent which has the PhoneAccountHanlde of the target SIM card
     * @return true if switch 3G/4G capability successfully
     */
    private boolean setRadioCapability(Context context, Intent intent) {
        int phoneNum = TelephonyManager.from(context).getPhoneCount();
        int[] phoneRat = new int[phoneNum];
        boolean isSwitchSuccess = true;

        PhoneAccountHandle selectedHandle = (PhoneAccountHandle) intent.getParcelableExtra(
                EXTRA_ACCOUNT);
        int selectedSubId = TelephonyManager.from(context).getSubIdForPhoneAccount(
                TelecomManager.from(context).getPhoneAccount(selectedHandle));
        int phoneId = SubscriptionManager.getPhoneId(selectedSubId);

        Log.d(TAG, "setCapability: " + phoneId);

        String curr3GSim = SystemProperties.get(PROPERTY_3G_SIM, "");
        Log.d(TAG, "current 3G Sim = " + curr3GSim);

        if (curr3GSim != null && !curr3GSim.equals("")) {
            int curr3GSlotId = Integer.parseInt(curr3GSim);
            if (curr3GSlotId == (phoneId + 1)) {
                Log.d(TAG, "Current 3G phone equals target phone, don't trigger switch");
                return isSwitchSuccess;
            }
        }

        try {
            ITelephony iTel = ITelephony.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE));
            ITelephonyEx iTelEx = ITelephonyEx.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));

            if (null == iTel || null == iTelEx) {
                Log.e(TAG, "Can not get phone service");
                return false;
            }

            int currRat = iTel.getRadioAccessFamily(phoneId, context.getPackageName());
            Log.d(TAG, "Current phoneRat:" + currRat);

            RadioAccessFamily[] rat = new RadioAccessFamily[phoneNum];
            for (int i = 0; i < phoneNum; i++) {
                if (phoneId == i) {
                    Log.d(TAG, "SIM switch to Phone" + i);
                    if (isLteSupport()) {
                        phoneRat[i] = RadioAccessFamily.RAF_LTE
                                | RadioAccessFamily.RAF_UMTS
                                | RadioAccessFamily.RAF_GSM;
                    } else {
                        phoneRat[i] = RadioAccessFamily.RAF_UMTS
                                | RadioAccessFamily.RAF_GSM;
                    }
                } else {
                    phoneRat[i] = RadioAccessFamily.RAF_GSM;
                }
                rat[i] = new RadioAccessFamily(i, phoneRat[i]);
            }
            if (false  == iTelEx.setRadioCapability(rat)) {
                Log.d(TAG, "Set phone rat fail!!!");
                isSwitchSuccess = false;
            }
        } catch (RemoteException ex) {
            Log.d(TAG, "Set phone rat fail!!!");
            ex.printStackTrace();
            isSwitchSuccess = false;
        }
        Log.d(TAG, "setRadioCapability isSwitchSuccess = " + isSwitchSuccess);
        return isSwitchSuccess;
    }

    private static int getMainCapabilitySlotId() {
        int slotId = SystemProperties.getInt(PhoneConstants.PROPERTY_CAPABILITY_SWITCH, 1) - 1;
        Log.d(TAG, "getMainCapabilitySlotId()... slotId: " + slotId);
        return slotId;
    }

    private boolean isAirplaneModeOn(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private void showAlertToast(Context context, int resId) {
        String textErr = context.getResources().getString(resId);
        Toast.makeText(context, textErr, Toast.LENGTH_SHORT).show();
    }

    /**
     *  M: Return true if given account is subscription/SIM account.
     */
    private static boolean isSubscriptionAccount(Context context,
            PhoneAccountHandle accountHandle) {
        PhoneAccount account = getAccountOrNull(context, accountHandle);
        if (account != null && account.hasCapabilities(
                PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)) {
            return true;
        }
        return false;
    }

    /**
     * Retrieve the account metadata, but if the account does not exist or the
     * device has only a single registered and enabled account, return null.
     */
    private static PhoneAccount getAccountOrNull(Context context, PhoneAccountHandle phoneAccount) {
        final TelecomManager telecommManager = (TelecomManager) context
                .getSystemService(Context.TELECOM_SERVICE);
        final PhoneAccount account = telecommManager.getPhoneAccount(phoneAccount);
        ///M: !telecommManager.hasMultipleCallCapableAccounts
        if (account == null || !(telecommManager.getCallCapablePhoneAccounts().size() > 1)) {
            Log.w(TAG, "[getAccountOrNull]account = " + account);
            return null;
        }
        return account;
    }

    /**
     * [C2K solution2] Whether the C2K solution2 supported
     * @return true if the C2K solution2 supported
     */
    private static boolean isC2KSolution2Support() {
        return SystemProperties.get("ro.mtk.c2k.slot2.support").equals("1");
    }

    /**
     * Whether the LTE is supported
     * @return true if the LTE is supported
     */
    private static boolean isLteSupport() {
        return SystemProperties.get("ro.mtk_lte_support").equals("1");
    }

    /**
     * check whether the card inserted is a CDMA card and
     * working in CDMA mode (the modem support CDMA).
     * @param slotId slot Id
     * @return
     */
    private boolean isCdmaCard(int slotId) {
        boolean isSupportCdma = false;
        TelephonyManagerEx telephonyMgrEx = TelephonyManagerEx.getDefault();
        String[] types = telephonyMgrEx.getSupportCardType(slotId);
        if (types != null) {
            for (String type : types) {
                if ("RUIM".equals(type) || "CSIM".equals(type)) {
                    isSupportCdma = true;
                    break;
                } else if ("SIM".equals(type) && telephonyMgrEx.isCt3gDualMode(slotId)) {
                    isSupportCdma = true;
                    break;
                }
            }
        }
        Log.d(TAG, "isSupportCdma, slotId = " + slotId + ", type = " + types
                + ", isSupportCdma = " + isSupportCdma);
        return isSupportCdma;
    }

    /**
     * Check whether the card inserted into the slot is a GSM card
     */
    private boolean isGsmCard(int slotId) {
        boolean isSupportGsm = false;
        TelephonyManagerEx telephonyMgrEx = TelephonyManagerEx.getDefault();
        String[] types = telephonyMgrEx.getSupportCardType(slotId);
        if (types != null && !isCdmaCard(slotId)) {
            for (String type : types) {
                if ("SIM".equals(type) || "USIM".equals(type)) {
                    isSupportGsm = true;
                    break;
                }
            }
        }
        Log.d(TAG, "isGsmCard, slotId = " + slotId + ", type = " + types
                + ", isSupportGsm = " + isSupportGsm);
        return isSupportGsm;
    }
}
