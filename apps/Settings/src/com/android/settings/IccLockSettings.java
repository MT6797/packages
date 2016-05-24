/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.ext.ISimRoamingExt;
import com.mediatek.settings.sim.SimHotSwapHandler;
import com.mediatek.settings.sim.TelephonyUtils;
import com.mediatek.settings.sim.SimHotSwapHandler.OnSimHotSwapListener;

/**
 * Implements the preference screen to enable/disable ICC lock and
 * also the dialogs to change the ICC PIN. In the former case, enabling/disabling
 * the ICC lock will prompt the user for the current PIN.
 * In the Change PIN case, it prompts the user for old pin, new pin and new pin
 * again before attempting to change it. Calls the SimCard interface to execute
 * these operations.
 *
 */
public class IccLockSettings extends InstrumentedPreferenceActivity
        implements EditPinPreference.OnPinEnteredListener {
    private static final String TAG = "IccLockSettings";
    private static final boolean DBG = true;

    private static final int OFF_MODE = 0;
    // State when enabling/disabling ICC lock
    private static final int ICC_LOCK_MODE = 1;
    // State when entering the old pin
    private static final int ICC_OLD_MODE = 2;
    // State when entering the new pin - first time
    private static final int ICC_NEW_MODE = 3;
    // State when entering the new pin - second time
    private static final int ICC_REENTER_MODE = 4;

    // Keys in xml file
    private static final String PIN_DIALOG = "sim_pin";
    private static final String PIN_TOGGLE = "sim_toggle";
    // Keys in icicle
    private static final String DIALOG_STATE = "dialogState";
    private static final String DIALOG_PIN = "dialogPin";
    private static final String DIALOG_ERROR = "dialogError";
    private static final String ENABLE_TO_STATE = "enableState";

    // Save and restore inputted PIN code when configuration changed
    // (ex. portrait<-->landscape) during change PIN code
    private static final String OLD_PINCODE = "oldPinCode";
    private static final String NEW_PINCODE = "newPinCode";

    private static final int MIN_PIN_LENGTH = 4;
    private static final int MAX_PIN_LENGTH = 8;
    // Which dialog to show next when popped up
    private int mDialogState = OFF_MODE;

    private String mPin;
    private String mOldPin;
    private String mNewPin;
    private String mError;
    // Are we trying to enable or disable ICC lock?
    private boolean mToState;

    private TabHost mTabHost;
    private TabWidget mTabWidget;
    private ListView mListView;

    private Phone mPhone;

    private EditPinPreference mPinDialog;
    private SwitchPreference mPinToggle;

    private Resources mRes;

    // For async handler to identify request type
    private static final int MSG_ENABLE_ICC_PIN_COMPLETE = 100;
    private static final int MSG_CHANGE_ICC_PIN_COMPLETE = 101;
    private static final int MSG_SIM_STATE_CHANGED = 102;

    // For replies from IccCard interface
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            switch (msg.what) {
                case MSG_ENABLE_ICC_PIN_COMPLETE:
                    iccLockChanged(ar.exception, msg.arg1);
                    break;
                case MSG_CHANGE_ICC_PIN_COMPLETE:
                    iccPinChanged(ar.exception, msg.arg1);
                    break;
                case MSG_SIM_STATE_CHANGED:
                    updatePreferences();
                    break;
            }

            return;
        }
    };

    private final BroadcastReceiver mSimStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
                mHandler.sendMessage(mHandler.obtainMessage(MSG_SIM_STATE_CHANGED));
            /// M: check airplane mode @{
            } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                mIsAirplaneModeOn = intent.getBooleanExtra("state", false);
                updatePreferences();
            }
            /// @}
        }
    };

    // For top-level settings screen to query
    static boolean isIccLockEnabled() {
        return PhoneFactory.getDefaultPhone().getIccCard().getIccLockEnabled();
    }

    static String getSummary(Context context) {
        Resources res = context.getResources();
        String summary = isIccLockEnabled()
                ? res.getString(R.string.sim_lock_on)
                : res.getString(R.string.sim_lock_off);
        return summary;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = getApplicationContext();
        final TelephonyManager tm =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        final int numSims = tm.getSimCount();

        if (Utils.isMonkeyRunning()) {
            finish();
            return;
        }

        /// M: check airplane mode
        mIsAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(getApplicationContext());

        /// M: for plug in @{
        mMiscExt = UtilsExt.getMiscPlugin(this);
        mSimRoamingExt = UtilsExt.getSimRoamingExtPlugin(this);
        /// @}

        addPreferencesFromResource(R.xml.sim_lock_settings);

        mPinDialog = (EditPinPreference) findPreference(PIN_DIALOG);
        mPinToggle = (SwitchPreference) findPreference(PIN_TOGGLE);
        if (savedInstanceState != null && savedInstanceState.containsKey(DIALOG_STATE)) {
            mDialogState = savedInstanceState.getInt(DIALOG_STATE);
            mPin = savedInstanceState.getString(DIALOG_PIN);
            mError = savedInstanceState.getString(DIALOG_ERROR);
            mToState = savedInstanceState.getBoolean(ENABLE_TO_STATE);

            // Restore inputted PIN code
            switch (mDialogState) {
                case ICC_NEW_MODE:
                    mOldPin = savedInstanceState.getString(OLD_PINCODE);
                    break;

                case ICC_REENTER_MODE:
                    mOldPin = savedInstanceState.getString(OLD_PINCODE);
                    mNewPin = savedInstanceState.getString(NEW_PINCODE);
                    break;

                case ICC_LOCK_MODE:
                case ICC_OLD_MODE:
                default:
                    break;
            }
        }

        mPinDialog.setOnPinEnteredListener(this);

        // Don't need any changes to be remembered
        getPreferenceScreen().setPersistent(false);

        if (numSims > 1) {
            setContentView(R.layout.icc_lock_tabs);

            mTabHost = (TabHost) findViewById(android.R.id.tabhost);
            mTabWidget = (TabWidget) findViewById(android.R.id.tabs);
            mListView = (ListView) findViewById(android.R.id.list);

            mTabHost.setup();
            mTabHost.setOnTabChangedListener(mTabListener);
            mTabHost.clearAllTabs();

            SubscriptionManager sm = SubscriptionManager.from(this);
            for (int i = 0; i < numSims; ++i) {
                final SubscriptionInfo subInfo = sm.getActiveSubscriptionInfoForSimSlotIndex(i);
                mTabHost.addTab(buildTabSpec(String.valueOf(i),
                        String.valueOf(subInfo == null
                            ? context.getString(R.string.sim_editor_title, i + 1)
                            : subInfo.getDisplayName())));
            }
            final SubscriptionInfo sir = sm.getActiveSubscriptionInfoForSimSlotIndex(0);

            mPhone = (sir == null) ? null
                : PhoneFactory.getPhone(SubscriptionManager.getPhoneId(sir.getSubscriptionId()));
        } else {
            mPhone = PhoneFactory.getDefaultPhone();
            /// M: if SIM count > 1, onTabChanged() will be invoked when addTab()
            // so no need to call changeSimTitle(). Call this API only when no tab added.
            changeSimTitle();
        }
        Log.d(TAG, "onCreate()... mPhone: " + mPhone);
        mRes = getResources();
        updatePreferences();

        /// M: for [SIM Hot Swap] @{
        mSimHotSwapHandler = new SimHotSwapHandler(getApplicationContext());
        mSimHotSwapHandler.registerOnSimHotSwap(new OnSimHotSwapListener() {
            @Override
            public void onSimHotSwap() {
                Log.d(TAG, "onSimHotSwap, finish Activity~~");
                finish();
            }
        });
        /// @}

        /// M: for plug-in
        setTitle(mMiscExt.customizeSimDisplayString(
                    getTitle().toString(),
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID));
    }

    private void updatePreferences() {
        /// M: check airplane mode
        mPinDialog.setEnabled(mPhone != null && !mIsAirplaneModeOn);
        mPinToggle.setEnabled(mPhone != null && !mIsAirplaneModeOn);

        if (mPhone != null) {
            mPinToggle.setChecked(mPhone.getIccCard().getIccLockEnabled());
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.ICC_LOCK;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ACTION_SIM_STATE_CHANGED is sticky, so we'll receive current state after this call,
        // which will call updatePreferences().
        final IntentFilter filter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        /// M: check airplane mode
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(mSimStateReceiver, filter);

        /// M: for ALPS02323548 @{
        // need to update preference when activity resume
        mIsAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(getApplicationContext());
        updatePreferences();
        /// @}
        if (mDialogState != OFF_MODE) {
            showPinDialog();
        } else {
            // Prep for standard click on "Change PIN"
            resetDialogState();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mSimStateReceiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        // Need to store this state for slider open/close
        // There is one case where the dialog is popped up by the preference
        // framework. In that case, let the preference framework store the
        // dialog state. In other cases, where this activity manually launches
        // the dialog, store the state of the dialog.
        if (mPinDialog.isDialogOpen()) {
            out.putInt(DIALOG_STATE, mDialogState);
            out.putString(DIALOG_PIN, mPinDialog.getEditText().getText().toString());
            out.putString(DIALOG_ERROR, mError);
            out.putBoolean(ENABLE_TO_STATE, mToState);

            // Save inputted PIN code
            switch (mDialogState) {
                case ICC_NEW_MODE:
                    out.putString(OLD_PINCODE, mOldPin);
                    break;

                case ICC_REENTER_MODE:
                    out.putString(OLD_PINCODE, mOldPin);
                    out.putString(NEW_PINCODE, mNewPin);
                    break;

                case ICC_LOCK_MODE:
                case ICC_OLD_MODE:
                default:
                    break;
            }
        } else {
            super.onSaveInstanceState(out);
        }
    }

    private void showPinDialog() {
        if (mDialogState == OFF_MODE) {
            return;
        }
        setDialogValues();

        mPinDialog.showPinDialog();
    }

    private void setDialogValues() {
        mPinDialog.setText(mPin);
        String message = "";
        switch (mDialogState) {
            case ICC_LOCK_MODE:
                message = mRes.getString(R.string.sim_enter_pin);
                mPinDialog.setDialogTitle(mToState
                        ? mRes.getString(R.string.sim_enable_sim_lock)
                        : mRes.getString(R.string.sim_disable_sim_lock));
                break;
            case ICC_OLD_MODE:
                message = mRes.getString(R.string.sim_enter_old);
                mPinDialog.setDialogTitle(mRes.getString(R.string.sim_change_pin));
                break;
            case ICC_NEW_MODE:
                message = mRes.getString(R.string.sim_enter_new);
                mPinDialog.setDialogTitle(mRes.getString(R.string.sim_change_pin));
                break;
            case ICC_REENTER_MODE:
                message = mRes.getString(R.string.sim_reenter_new);
                mPinDialog.setDialogTitle(mRes.getString(R.string.sim_change_pin));
                break;
        }
        if (mError != null) {
            message = mError + "\n" + message;
            mError = null;
        }
        Log.d(TAG, "setDialogValues mDialogState = " + mDialogState);
        mPinDialog.setDialogMessage(message);
        /// M: for plug-in
        changeDialogStrings(mPinDialog.getDialogTitle().toString(), message);
    }

    public void onPinEntered(EditPinPreference preference, boolean positiveResult) {
        if (!positiveResult) {
            resetDialogState();
            return;
        }

        mPin = preference.getText();
        if (!reasonablePin(mPin)) {
            // inject error message and display dialog again
            mError = mRes.getString(R.string.sim_bad_pin);
            /// M: for ALPS02367598, make sure activity is running when showing dialog
            if (isResumed()) {
                showPinDialog();
            }
            return;
        }
        switch (mDialogState) {
            case ICC_LOCK_MODE:
                tryChangeIccLockState();
                break;
            case ICC_OLD_MODE:
                mOldPin = mPin;
                mDialogState = ICC_NEW_MODE;
                mError = null;
                mPin = null;
                showPinDialog();
                break;
            case ICC_NEW_MODE:
                mNewPin = mPin;
                mDialogState = ICC_REENTER_MODE;
                mPin = null;
                showPinDialog();
                break;
            case ICC_REENTER_MODE:
                if (!mPin.equals(mNewPin)) {
                    mError = mRes.getString(R.string.sim_pins_dont_match);
                    mDialogState = ICC_NEW_MODE;
                    mPin = null;
                    showPinDialog();
                } else {
                    mError = null;
                    tryChangePin();
                }
                break;
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mPinToggle) {
            // Get the new, preferred state
            mToState = mPinToggle.isChecked();
            // Flip it back and pop up pin dialog
            mPinToggle.setChecked(!mToState);
            mDialogState = ICC_LOCK_MODE;
            showPinDialog();
        } else if (preference == mPinDialog) {
            mDialogState = ICC_OLD_MODE;
            return false;
        }
        return true;
    }

    private void tryChangeIccLockState() {
        // Try to change icc lock. If it succeeds, toggle the lock state and
        // reset dialog state. Else inject error message and show dialog again.
        Message callback = Message.obtain(mHandler, MSG_ENABLE_ICC_PIN_COMPLETE);
        if (mPhone != null) {
            mPhone.getIccCard().setIccLockEnabled(mToState, mPin, callback);
            // Disable the setting till the response is received.
            mPinToggle.setEnabled(false);
        }
    }

    private void iccLockChanged(Throwable exception, int attemptsRemaining) {
        boolean success = (exception == null);
        if (success) {
            mPinToggle.setChecked(mToState);
            /// M: for plug-in
            mSimRoamingExt.showPinToast(mToState);
        } else {
            Toast.makeText(this, getPinPasswordErrorMessage(attemptsRemaining, exception),
                    Toast.LENGTH_LONG)
                    .show();
        }
        mPinToggle.setEnabled(true);
        resetDialogState();
    }

    private void iccPinChanged(Throwable exception, int attemptsRemaining) {
        boolean success = (exception == null);
        if (!success) {
            Toast.makeText(this, getPinPasswordErrorMessage(attemptsRemaining, exception),
                    Toast.LENGTH_LONG)
                    .show();
        } else {
            /// M: for plug-in @{
            /*
            Toast.makeText(this, mRes.getString(R.string.sim_change_succeeded),
                    Toast.LENGTH_SHORT)
                    .show();
            */
            String successMsg = mRes.getString(R.string.sim_change_succeeded);
            successMsg = mMiscExt.customizeSimDisplayString(successMsg, mPhone.getSubId());
            Toast.makeText(this, successMsg,
                    Toast.LENGTH_SHORT)
                    .show();
            /// @}

        }
        resetDialogState();
    }

    private void tryChangePin() {
        Message callback = Message.obtain(mHandler, MSG_CHANGE_ICC_PIN_COMPLETE);
        if (mPhone != null) {
            mPhone.getIccCard().changeIccLockPassword(mOldPin,
                    mNewPin, callback);
        }
    }

    private String getPinPasswordErrorMessage(int attemptsRemaining, Throwable exception) {
        String displayMessage;

        /// M: when enable pin or change pin and fail, reason is not pin wrong, @{
        // but is CommandException.Error.GENERIC_FAILURE, should return
        // pin fail
        if (exception instanceof CommandException
                && ((CommandException) exception).getCommandError() ==
                CommandException.Error.GENERIC_FAILURE) {
            displayMessage = mRes.getString(R.string.pin_failed);
        /// @}
        } else if (attemptsRemaining == 0) {
            displayMessage = mRes.getString(R.string.wrong_pin_code_pukked);
        } else if (attemptsRemaining > 0) {
            displayMessage = mRes
                    .getQuantityString(R.plurals.wrong_pin_code, attemptsRemaining,
                            attemptsRemaining);
        } else {
            displayMessage = mRes.getString(R.string.pin_failed);
        }
        /// M: for plug-in
        displayMessage = mMiscExt.customizeSimDisplayString(displayMessage, mPhone.getSubId());
        if (DBG) Log.d(TAG, "getPinPasswordErrorMessage:"
                + " attemptsRemaining=" + attemptsRemaining + " displayMessage=" + displayMessage);
        return displayMessage;
    }

    private boolean reasonablePin(String pin) {
        if (pin == null || pin.length() < MIN_PIN_LENGTH || pin.length() > MAX_PIN_LENGTH) {
            return false;
        } else {
            return true;
        }
    }

    private void resetDialogState() {
        mError = null;
        mDialogState = ICC_OLD_MODE; // Default for when Change PIN is clicked
        mPin = "";
        setDialogValues();
        mDialogState = OFF_MODE;
    }

    private OnTabChangeListener mTabListener = new OnTabChangeListener() {
        @Override
        public void onTabChanged(String tabId) {
            final int slotId = Integer.parseInt(tabId);
            final SubscriptionInfo sir = SubscriptionManager.from(getBaseContext())
                    .getActiveSubscriptionInfoForSimSlotIndex(slotId);

            mPhone = (sir == null) ? null
                : PhoneFactory.getPhone(SubscriptionManager.getPhoneId(sir.getSubscriptionId()));
            Log.d(TAG, "onTabChanged()... mPhone: " + mPhone);
            // The User has changed tab; update the body.
            updatePreferences();

            /// M: for plug-in.
            changeSimTitle();
        }
    };

    private TabContentFactory mEmptyTabContent = new TabContentFactory() {
        @Override
        public View createTabContent(String tag) {
            return new View(mTabHost.getContext());
        }
    };

    private TabSpec buildTabSpec(String tag, String title) {
        return mTabHost.newTabSpec(tag).setIndicator(title).setContent(
                mEmptyTabContent);
    }

    ///-----------------------------------------MTK-----------------------------------------------

    /// M: check airplane mode
    private boolean mIsAirplaneModeOn = false;

    /// M: for plug in @{
    private ISimRoamingExt mSimRoamingExt;
    private ISettingsMiscExt mMiscExt;
    /// @}

    /// M: for [SIM Hot Swap]
    SimHotSwapHandler mSimHotSwapHandler;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /// M: for [SIM Hot Swap]
        mSimHotSwapHandler.unregisterOnSimHotSwap();
    };

    /**
     * for plug-in, Replace sim to sim/uim.
     */
    private void changeSimTitle() {
        if (mPhone != null) {
            int subId = mPhone.getSubId();
            Log.d(TAG, "changeSimTitle subId = " + subId);
            ///M: replace sim to sim/uim check box title
            mPinToggle.setTitle(mMiscExt.customizeSimDisplayString(
                    getResources().getString(R.string.sim_pin_toggle), subId));

            ///M: replace sim to sim/uim pin dialog
            mPinDialog.setTitle(mMiscExt.customizeSimDisplayString(
                    getResources().getString(R.string.sim_pin_change), subId));
        }
    }

    /**
     * for plug-in, Replace sim to sim/uim.
     *
     * @param dialogTitle the string of dialog title.
     * @param dialogMessage the string of dialog message.
     */
    private void changeDialogStrings(String dialogTitle, String dialogMessage) {
        if (mPhone != null) {
            int subId = mPhone.getSubId();
            Log.d(TAG, "changeSimTitle subId = " + subId);
            mPinDialog.setDialogTitle(mMiscExt.customizeSimDisplayString(
                    dialogTitle, subId));
            mPinDialog.setDialogMessage(mMiscExt.customizeSimDisplayString(
                    dialogMessage, subId));
        }
    }
}
