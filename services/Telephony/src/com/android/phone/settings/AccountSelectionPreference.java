/*
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
 * limitations under the License
 */

package com.android.phone.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.telecom.PhoneAccountHandle;
import android.telecom.PhoneAccount;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.mediatek.phone.PhoneFeatureConstants.FeatureOption;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.settings.TelephonyUtils;
import com.mediatek.settings.cdma.TelephonyUtilsEx;

import java.util.List;
import java.util.Objects;

public class AccountSelectionPreference extends ListPreference implements
        Preference.OnPreferenceChangeListener {

    public interface AccountSelectionListener {
        boolean onAccountSelected(AccountSelectionPreference pref, PhoneAccountHandle account);
        void onAccountSelectionDialogShow(AccountSelectionPreference pref);
        void onAccountChanged(AccountSelectionPreference pref);
    }
    private static final String TAG = "settings/AccountSelectionPreference";
    private final Context mContext;
    private AccountSelectionListener mListener;
    private PhoneAccountHandle[] mAccounts;
    private String[] mEntryValues;
    private CharSequence[] mEntries;
    private boolean mShowSelectionInSummary = true;
    private AlertDialog mDialog;

    public AccountSelectionPreference(Context context) {
        this(context, null);
    }

    public AccountSelectionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setOnPreferenceChangeListener(this);
    }

    public void setListener(AccountSelectionListener listener) {
        mListener = listener;
    }

    public void setShowSelectionInSummary(boolean value) {
        mShowSelectionInSummary = value;
    }

    public void setModel(
            TelecomManager telecomManager,
            List<PhoneAccountHandle> accountsList,
            PhoneAccountHandle currentSelection,
            CharSequence nullSelectionString) {

        mAccounts = accountsList.toArray(new PhoneAccountHandle[accountsList.size()]);
        mEntryValues = new String[mAccounts.length + 1];
        mEntries = new CharSequence[mAccounts.length + 1];

        PackageManager pm = mContext.getPackageManager();

        int selectedIndex = mAccounts.length;  // Points to nullSelectionString by default

        /// M: For OP09 reset the selectedIndex to the first one @{
        selectedIndex = ExtensionManager.getPhoneMiscExt().getSelectedIndex(selectedIndex);
        /// }@
        int i = 0;
        for ( ; i < mAccounts.length; i++) {
            CharSequence label = telecomManager.getPhoneAccount(mAccounts[i]).getLabel();
            if (label != null) {
                label = pm.getUserBadgedLabel(label, mAccounts[i].getUserHandle());
            }
            mEntries[i] = label == null ? null : label.toString();
            mEntryValues[i] = Integer.toString(i);
            if (Objects.equals(currentSelection, mAccounts[i])) {
                selectedIndex = i;
            }
        }
        mEntryValues[i] = Integer.toString(i);
        mEntries[i] = nullSelectionString;

        /// M: remove the "Ask first" item from the call with selection list.
        mEntryValues = ExtensionManager.getPhoneMiscExt()
                 .removeAskFirstFromSelectionListIndex(mEntryValues);
        mEntries = ExtensionManager.getPhoneMiscExt()
                 .removeAskFirstFromSelectionListValue(mEntries);

        setEntryValues(mEntryValues);
        setEntries(mEntries);
        setValueIndex(selectedIndex);
        if (mShowSelectionInSummary) {
            setSummary(mEntries[selectedIndex]);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        /// M: for C2K Solution 2, if change the default calling account.
        ///    may cause last SIM card can't be used. @{
        final int index = Integer.parseInt((String) newValue);
        final PhoneAccountHandle accountHandle = index < mAccounts.length ? mAccounts[index] : null;
        Log.d(TAG, "[onPreferenceChange] index = " + index);
        if (shouldShowChangeCdmaCapabilityTip(accountHandle)) {
            showChangeCdmaCapabilityDialog(index, accountHandle);
            return true;
        }
        /// M: for C2K solution 1.5 @{
        if (!FeatureOption.isMtkSvlteSolutionSupport() && TelephonyUtilsEx.isCGCardInserted()
                && hasSimCapability(accountHandle)) {
            final int phoneAccountSubId = PhoneUtils.getSubIdForPhoneAccountHandle(accountHandle);
            int phoneId = SubscriptionManager.from(mContext).getPhoneId(phoneAccountSubId);

            if (TelephonyUtilsEx.isCapabilityOnGCard()
                    && !TelephonyUtilsEx.isGCardInserted(phoneId)) {

                if (TelecomManager.from(mContext).isInCall()) {
                    Toast.makeText(mContext, R.string.default_call_switch_err_msg1,
                            Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "--- is in call ---");
                    return false;
                }

                TelephonyUtilsEx.setMainPhone(phoneId);
            }
        }
        /// @}
        /// @}
        if (mListener != null) {
//            int index = Integer.parseInt((String) newValue);
//            PhoneAccountHandle account = index < mAccounts.length ? mAccounts[index] : null;
            if (mListener.onAccountSelected(this, accountHandle)) {
                if (mShowSelectionInSummary) {
                    setSummary(mEntries[index]);
                }
                if (index != findIndexOfValue(getValue())) {
                    setValueIndex(index);
                    mListener.onAccountChanged(this);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Modifies the dialog to change the default "Cancel" button to "Choose Accounts", which
     * triggers the {@link PhoneAccountSelectionPreferenceActivity} to be shown.
     *
     * @param builder The {@code AlertDialog.Builder}.
     */
    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        // Notify the listener that the dialog is about to be built.  This is important so that the
        // list of enabled accounts can be updated prior to showing the dialog.
        mListener.onAccountSelectionDialogShow(this);

        super.onPrepareDialogBuilder(builder);
    }

    /**
     * 1. User select Account is not always ask
     * 2. Two CDMA SIM card
     * 3. Change default account to unserviceable SIM card
     *    We think default data's SIM card is serviceable
     * 4. In China mainland && Macau;
     * @return
     */
    private boolean shouldShowChangeCdmaCapabilityTip(PhoneAccountHandle accountHandle) {
        if (accountHandle == null) {
            Log.d(TAG, "[shouldShowChangeCdmaCapabilityTip] accountHandle is null");
        } else if (TelephonyUtilsEx.isMultiCdmaCardInsertedInHomeNetwork()
                && !isServiceableSimAccount(accountHandle)
                && hasSimCapability(accountHandle)) {
            return true;
        }
        return false;
    }

    /**
     * Judge whether the account has sim capability or not
     * @param accountHandle handle of the account
     * @return true, is sim card, or not sim card.
     */
    private boolean hasSimCapability(PhoneAccountHandle accountHandle) {
        boolean result = false;
        if (accountHandle != null) {
            PhoneAccount phoneAccount = TelecomManager.from(
                    getContext()).getPhoneAccount(accountHandle);
            if (phoneAccount != null) {
                boolean hasSimCapabilities = phoneAccount.hasCapabilities(
                        PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION);

                Log.d(TAG, "[isSimCapability] hasSimCapabilities =" + hasSimCapabilities);

                result = hasSimCapabilities ? true : false;
            }

        } else {
            Log.d(TAG, "[isSimCapability] accountHandle is null");
        }
        return result;
    }

    /**
     * Get whether the account is a serviceable account
     * 1. The account is a SIM account
     * 2. The SIM account is the same as default data SIM.
     * @param account
     * @return
     */
    private boolean isServiceableSimAccount(PhoneAccountHandle accountHandle) {
        if (hasSimCapability(accountHandle)) {
            final int phoneAccountSubId = PhoneUtils.getSubIdForPhoneAccountHandle(accountHandle);

            final int mainSubId = getCapabilityPhoneSubId();

            Log.d(TAG, "[isServiceableSimAccount] is a SIM account."
                    + "; accountHandlerSubId = " + phoneAccountSubId
                    + "; mainSubId = " + mainSubId);

            if (phoneAccountSubId == mainSubId) {
                return true;
            }
        } else {
            Log.d(TAG, "[isServiceableSimAccount] accountHandle is null");
        }
        return false;
    }

    /**
     * Show the tips dialog, if user select yes, it will trigger default Data & SMS.
     * @param index
     * @param newHandler
     */
    private void showChangeCdmaCapabilityDialog(
            final int index, final PhoneAccountHandle newHandler) {

        if (TelecomManager.from(mContext).isInCall()) {
            Toast.makeText(mContext, R.string.default_call_switch_err_msg1,
                    Toast.LENGTH_SHORT).show();
            Log.d(TAG, "--- is in call ---");
            return;
        }

        /// Last time user select always ask. and should show dialog,
        /// we can think that fromSubId is from default data sub id.
        final int fromSubId = getCapabilityPhoneSubId();
        final int toSubId = PhoneUtils.getSubIdForPhoneAccountHandle(newHandler);

        Log.d(TAG, "[showChangeCdmaCapabilityDialog] fromSubId = " + fromSubId
                + "; toSubId = " + toSubId);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(getContext().getResources().getString(
                R.string.change_cdma_capability_dialog, PhoneUtils.getSubDisplayName(fromSubId),
                PhoneUtils.getSubDisplayName(fromSubId), PhoneUtils.getSubDisplayName(toSubId)));

        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                boolean isAirplaneModeEnabled = TelephonyUtils.isAirplaneModeOn(getContext());

                Log.d(TAG, "mAirplaneModeEnabled : " + isAirplaneModeEnabled);

                if (isAirplaneModeEnabled) {
                    dialog.dismiss();
                    return;
                }

                if (TelecomManager.from(mContext).isInCall()) {
                    Toast.makeText(mContext, R.string.default_call_switch_err_msg1,
                            Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "--- is in call ---");
                    return;
                }

                SubscriptionManager subscriptionManager = SubscriptionManager.from(getContext());
                if (subscriptionManager != null) {
                    Log.d(TAG, "[showChangeCdmaCapabilityDialog] Enter change default flow.");
                    SubscriptionInfo defSms = subscriptionManager.getDefaultSmsSubscriptionInfo();
                    subscriptionManager.setDefaultDataSubId(toSubId);
                    setDefaultPhoneAccount(index, newHandler);

                    /// If default sms sub id is don't the same as from one, don't change.
                    if (defSms != null && fromSubId == defSms.getSubscriptionId()) {
                        subscriptionManager.setDefaultSmsSubId(toSubId);
                    } else {
                        Log.d(TAG, "[showChangeCdmaCapabilityDialog] don't change sms sub.");
                    }

                } else {
                    Log.d(TAG, "[showChangeCdmaCapabilityDialog] SubscriptionManager is null!");
                }
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        mDialog = builder.show();
    }

    public void onStop() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    private void setDefaultPhoneAccount(int index, PhoneAccountHandle newHandler) {
        if (mListener != null) {
            if (mListener.onAccountSelected(this, newHandler)) {
                if (mShowSelectionInSummary) {
                    setSummary(mEntries[index]);
                }
                if (index != findIndexOfValue(getValue())) {
                    setValueIndex(index);
                    mListener.onAccountChanged(this);
                }
            }
        }
    }

    private int getCapabilityPhoneSubId() {
        final int mainPhoneId = TelephonyUtilsEx.getMainPhoneId();
        Phone phone = PhoneFactory.getPhone(mainPhoneId);
        final int mainSubId = phone.getSubId();

        Log.d(TAG, "getCapabilityPhoneSubId: " + mainSubId);
        return mainSubId;
    }
    /// @}
}
