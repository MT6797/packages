/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
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
 * limitations under the License.
 */

package com.android.services.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;

import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.internal.os.SomeArgs;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;

/// M: CC035: [ALPS01785370] Use RadioManager for powering on radio during ECC @{
import com.mediatek.internal.telephony.RadioManager;
/// @}

import com.mediatek.telephony.TelephonyManagerEx;

/**
 * Helper class that implements special behavior related to emergency calls. Specifically, this
 * class handles the case of the user trying to dial an emergency number while the radio is off
 * (i.e. the device is in airplane mode), by forcibly turning the radio back on, waiting for it to
 * come up, and then retrying the emergency call.
 */
public class EmergencyCallHelper {

    /**
     * Receives the result of the EmergencyCallHelper's attempt to turn on the radio.
     */
    interface Callback {
        void onComplete(boolean isRadioReady);
    }

    // Number of times to retry the call, and time between retry attempts.
    public static final int MAX_NUM_RETRIES = 5;
    public static final long TIME_BETWEEN_RETRIES_MILLIS = 5000;  // msec

    // Handler message codes; see handleMessage()
    private static final int MSG_START_SEQUENCE = 1;
    private static final int MSG_SERVICE_STATE_CHANGED = 2;
    private static final int MSG_RETRY_TIMEOUT = 3;
    /// M: SS project ECC change feature @{
    private static final int MSG_START_SWITCH_PHONE = 4;
    private static final int MSG_SWITCH_PHONE_TIMEOUT = 5;
    private static final int MSG_MODE_SWITCH_RESULT = 6;
    /// @}

    private final Context mContext;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_SEQUENCE:
                    SomeArgs args = (SomeArgs) msg.obj;
                    Phone phone = (Phone) args.arg1;
                    EmergencyCallHelper.Callback callback =
                            (EmergencyCallHelper.Callback) args.arg2;
                    args.recycle();

                    startSequenceInternal(phone, callback);
                    break;
                case MSG_SERVICE_STATE_CHANGED:
                    onServiceStateChanged((ServiceState) ((AsyncResult) msg.obj).result);
                    break;
                case MSG_RETRY_TIMEOUT:
                    onRetryTimeout();
                    break;
                /// M: SS project ECC change feature @{
                case MSG_START_SWITCH_PHONE:
                    startSwitchPhoneInternal();
                    break;
                case MSG_SWITCH_PHONE_TIMEOUT:
                    Log.d(EmergencyCallHelper.this, "MSG_SWITCH_PHONE_TIMEOUT");
                    makePhoneCallCheck();
                    break;
                case MSG_MODE_SWITCH_RESULT:
                    Log.d(EmergencyCallHelper.this, "MSG_MODE_SWITCH_RESULT");
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        Log.d(EmergencyCallHelper.this, "Fail to switch now, mFailRetryCount:"
                                + mFailRetryCount);
                        if (mFailRetryCount++ < MAX_FAIL_RETRY_COUNT) {
                            mHandler.sendMessageDelayed(
                                    mHandler.obtainMessage(MSG_START_SWITCH_PHONE),
                                    RETRY_SWITCH_PHONE_MILLIS);
                        } else {
                            mSwitchPhoneRetryCount++;
                            makePhoneCallCheck();
                        }
                    } else {
                        Log.d(EmergencyCallHelper.this, "Start switch phone!");
                        mSwitchPhoneRetryCount++;
                        mHandler.removeMessages(MSG_START_SWITCH_PHONE);
                        startSwitchPhoneTimer();
                        if (!mRegisterReceiver) {
                            IntentFilter intentFilter = new IntentFilter(
                                    TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
                            intentFilter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
                            mReceiver = new MyBroadcastReceiver();
                            mContext.registerReceiver(mReceiver, intentFilter);
                            mRegisterReceiver = true;
                        }
                    }
                    break;
                /// @}
                default:
                    Log.wtf(this, "handleMessage: unexpected message: %d.", msg.what);
                    break;
            }
        }
    };

    private Callback mCallback;  // The callback to notify upon completion.
    private Phone mPhone;  // The phone that will attempt to place the call.
    private int mNumRetriesSoFar;

    /// M: SS project ECC change feature @{
    private static final int RETRY_SWITCH_PHONE_MILLIS = 2000;
    private static final int SWITCH_PHONE_TIMEOUT_MILLIS = 10000;
    private static final int MODE_GSM = 1;
    private static final int MODE_C2K = 4;
    private static final int MAX_RETRY_COUNT = 2;
    private static final int MAX_FAIL_RETRY_COUNT = 10;
    private MyBroadcastReceiver mReceiver;
    private boolean mRegisterReceiver = false;
    private int mTargetPhoneType = PhoneConstants.PHONE_TYPE_NONE;
    private int mInitialPhoneType = PhoneConstants.PHONE_TYPE_NONE;
    private TelephonyManager mTm;
    private int mSwitchPhoneRetryCount = 0;
    private int mFailRetryCount = 0;
    private AirplaneModeObserver mAirplaneModeObserver;
    /// @}

    public EmergencyCallHelper(Context context) {
        Log.d(this, "EmergencyCallHelper constructor.");
        mContext = context;
        /// M: SS project ECC change feature @{
        mTm = TelephonyManager.getDefault();
        mAirplaneModeObserver = new AirplaneModeObserver(mHandler, mContext);
        /// @}
        /// M: For delay SIM switch flow @{
        TelephonyManagerEx.getDefault().setEccInProgress(true);
        Intent intent = new Intent(TelephonyManagerEx.ACTION_ECC_IN_PROGRESS);
        intent.putExtra(TelephonyManagerEx.EXTRA_IN_PROGRESS, true);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        /// @}
    }

    /**
     * Starts the "turn on radio" sequence. This is the (single) external API of the
     * EmergencyCallHelper class.
     *
     * This method kicks off the following sequence:
     * - Power on the radio.
     * - Listen for the service state change event telling us the radio has come up.
     * - Retry if we've gone {@link #TIME_BETWEEN_RETRIES_MILLIS} without any response from the
     *   radio.
     * - Finally, clean up any leftover state.
     *
     * This method is safe to call from any thread, since it simply posts a message to the
     * EmergencyCallHelper's handler (thus ensuring that the rest of the sequence is entirely
     * serialized, and runs only on the handler thread.)
     */
    public void startTurnOnRadioSequence(Phone phone, Callback callback) {
        Log.d(this, "startTurnOnRadioSequence");

        SomeArgs args = SomeArgs.obtain();
        args.arg1 = phone;
        args.arg2 = callback;
        mHandler.obtainMessage(MSG_START_SEQUENCE, args).sendToTarget();
    }

    /**
     * Actual implementation of startTurnOnRadioSequence(), guaranteed to run on the handler thread.
     * @see #startTurnOnRadioSequence
     */
    private void startSequenceInternal(Phone phone, Callback callback) {
        Log.d(this, "startSequenceInternal()");

        // First of all, clean up any state left over from a prior emergency call sequence. This
        // ensures that we'll behave sanely if another startTurnOnRadioSequence() comes in while
        // we're already in the middle of the sequence.
        cleanup();

        mPhone = phone;
        mCallback = callback;


        // No need to check the current service state here, since the only reason to invoke this
        // method in the first place is if the radio is powered-off. So just go ahead and turn the
        // radio on.

        powerOnRadio();  // We'll get an onServiceStateChanged() callback
                         // when the radio successfully comes up.

        // Next step: when the SERVICE_STATE_CHANGED event comes in, we'll retry the call; see
        // onServiceStateChanged(). But also, just in case, start a timer to make sure we'll retry
        // the call even if the SERVICE_STATE_CHANGED event never comes in for some reason.
        startRetryTimer();
    }

    /**
     * Handles the SERVICE_STATE_CHANGED event. Normally this event tells us that the radio has
     * finally come up. In that case, it's now safe to actually place the emergency call.
     */
    private void onServiceStateChanged(ServiceState state) {
        Log.d(this, "onServiceStateChanged(), new state = %s.", state);

        /// M: SS project ECC change feature @{
        if (mPhone == null) {
            Log.d(this, "onServiceStateChanged(), mPhone is null");
            return;
        }
        Log.d(this, "onServiceStateChanged(), isEmergencyOnly:" + state.isEmergencyOnly()
                + ", phonetype:" + mPhone.getPhoneType() + ", hasCard:" + mTm.hasIccCard(0));
        if (mTargetPhoneType != PhoneConstants.PHONE_TYPE_NONE) {
            if (state.getState() != ServiceState.STATE_POWER_OFF
                && mPhone.getPhoneType() != mTargetPhoneType
                && !mTm.hasIccCard(0)
                && mSwitchPhoneRetryCount < MAX_RETRY_COUNT) {
                Log.d(this, "onServiceStateChanged, need to switch phone");
                unregisterForServiceStateChanged();
                mHandler.obtainMessage(MSG_START_SWITCH_PHONE).sendToTarget();
                return;
            }
            if (mPhone.getPhoneType() != mInitialPhoneType) {
                Log.d(this, "onServiceStateChanged, phone type changed");
                registerForServiceStateChanged();
            }
        }
        /// @}

        // Possible service states:
        // - STATE_IN_SERVICE        // Normal operation
        // - STATE_OUT_OF_SERVICE    // Still searching for an operator to register to,
        //                           // or no radio signal
        // - STATE_EMERGENCY_ONLY    // Phone is locked; only emergency numbers are allowed
        // - STATE_POWER_OFF         // Radio is explicitly powered off (airplane mode)

        if (isOkToCall(state.getState(), mPhone.getState())
        /// M: CC038: [ALPS01933697]Allow ECC if ServiceState isEmergencyOnly is true @{
                || (state.isEmergencyOnly()
                        && mPhone.getPhoneType() != PhoneConstants.PHONE_TYPE_CDMA)) {
        /// @}
            // Woo hoo!  It's OK to actually place the call.
            Log.d(this, "onServiceStateChanged: ok to call!");

            onComplete(true);
            cleanup();
        } else {
            // The service state changed, but we're still not ready to call yet. (This probably was
            // the transition from STATE_POWER_OFF to STATE_OUT_OF_SERVICE, which happens
            // immediately after powering-on the radio.)
            //
            // So just keep waiting; we'll probably get to either STATE_IN_SERVICE or
            // STATE_EMERGENCY_ONLY very shortly. (Or even if that doesn't happen, we'll at least do
            // another retry when the RETRY_TIMEOUT event fires.)
            Log.d(this, "onServiceStateChanged: not ready to call yet, keep waiting.");
        }
    }

    private boolean isOkToCall(int serviceState, PhoneConstants.State phoneState) {
        // Once we reach either STATE_IN_SERVICE or STATE_EMERGENCY_ONLY, it's finally OK to place
        // the emergency call.
        return ((phoneState == PhoneConstants.State.OFFHOOK)
                || (serviceState == ServiceState.STATE_IN_SERVICE)
                || (serviceState == ServiceState.STATE_EMERGENCY_ONLY)) ||

                // Allow STATE_OUT_OF_SERVICE if we are at the max number of retries.
                (mNumRetriesSoFar == MAX_NUM_RETRIES &&
                serviceState == ServiceState.STATE_OUT_OF_SERVICE) ||
                /// M: [ALPS02185470] Only retry once for WFC ECC. @{
                TelephonyManager.from(mContext).isWifiCallingEnabled();
                /// @}
    }

    /**
     * Handles the retry timer expiring.
     */
    private void onRetryTimeout() {
        PhoneConstants.State phoneState = mPhone.getState();
        int serviceState = mPhone.getServiceState().getState();
        Log.d(this, "onRetryTimeout():  phone state = %s, service state = %d, retries = %d.",
               phoneState, serviceState, mNumRetriesSoFar);

        /// M: SS project ECC change feature @{
        Log.d(this, "onRetryTimeout(), emergencyOnly:" + mPhone.getServiceState().isEmergencyOnly()
                + ", phonetype:" + mPhone.getPhoneType() + ", hasCard:" + mTm.hasIccCard(0));
        if (mTargetPhoneType != PhoneConstants.PHONE_TYPE_NONE) {
            if (serviceState != ServiceState.STATE_POWER_OFF
                && mPhone.getPhoneType() != mTargetPhoneType
                && !mTm.hasIccCard(0)
                && mSwitchPhoneRetryCount < MAX_RETRY_COUNT) {
                Log.d(this, "onRetryTimeout, need to switch phone");
                unregisterForServiceStateChanged();
                mHandler.obtainMessage(MSG_START_SWITCH_PHONE).sendToTarget();
                return;
            }
            if (mPhone.getPhoneType() != mInitialPhoneType) {
                Log.d(this, "onRetryTimeout, phone type changed");
                registerForServiceStateChanged();
            }
        }
        /// @}

        // - If we're actually in a call, we've succeeded.
        // - Otherwise, if the radio is now on, that means we successfully got out of airplane mode
        //   but somehow didn't get the service state change event.  In that case, try to place the
        //   call.
        // - If the radio is still powered off, try powering it on again.

        if (isOkToCall(serviceState, phoneState)
        /// M: CC038: [ALPS01933697]Allow ECC if ServiceState isEmergencyOnly is true @{
                || (mPhone.getServiceState().isEmergencyOnly()
                        && mPhone.getPhoneType() != PhoneConstants.PHONE_TYPE_CDMA)) {
        /// @}
            Log.d(this, "onRetryTimeout: Radio is on. Cleaning up.");

            // Woo hoo -- we successfully got out of airplane mode.
            onComplete(true);
            cleanup();
        } else {
            // Uh oh; we've waited the full TIME_BETWEEN_RETRIES_MILLIS and the radio is still not
            // powered-on.  Try again.

            mNumRetriesSoFar++;
            Log.d(this, "mNumRetriesSoFar is now " + mNumRetriesSoFar);
            if (mNumRetriesSoFar > MAX_NUM_RETRIES) {
                Log.w(this, "Hit MAX_NUM_RETRIES; giving up.");
                cleanup();
            } else {
                Log.d(this, "Trying (again) to turn on the radio.");
                powerOnRadio();  // Again, we'll (hopefully) get an onServiceStateChanged() callback
                                 // when the radio successfully comes up.
                startRetryTimer();
            }
        }
    }

    /**
     * Attempt to power on the radio (i.e. take the device out of airplane mode.)
     * Additionally, start listening for service state changes; we'll eventually get an
     * onServiceStateChanged() callback when the radio successfully comes up.
     */
    private void powerOnRadio() {
        Log.d(this, "powerOnRadio().");

        // We're about to turn on the radio, so arrange to be notified when the sequence is
        // complete.
        registerForServiceStateChanged();

        // If airplane mode is on, we turn it off the same way that the Settings activity turns it
        // off.
        if (Settings.Global.getInt(mContext.getContentResolver(),
                                   Settings.Global.AIRPLANE_MODE_ON, 0) > 0) {
            Log.d(this, "==> Turning off airplane mode.");

            // Change the system setting
            Settings.Global.putInt(mContext.getContentResolver(),
                                   Settings.Global.AIRPLANE_MODE_ON, 0);

            // Post the broadcast intend for change in airplane mode
            // TODO: We really should not be in charge of sending this broadcast.
            //     If changing the setting is sufficent to trigger all of the rest of the logic,
            //     then that should also trigger the broadcast intent.
            Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", false);
            /// M: [ALPS02229806] @{
            intent.putExtra("forceChanged", true);
            /// @}
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } else {
            // Otherwise, for some strange reason the radio is off (even though the Settings
            // database doesn't think we're in airplane mode.)  In this case just turn the radio
            // back on.
            Log.d(this, "==> (Apparently) not in airplane mode; manually powering radio on.");
            /// M: CC035: [ALPS01785370] Use RadioManager for powering on radio during ECC @{
            if (RadioManager.isMSimModeSupport()) {
                //RadioManager will help to turn on radio even this iccid is off by sim management
                Log.d(this, "isMSimModeSupport true, use RadioManager forceSetRadioPower");
                RadioManager.getInstance().forceSetRadioPower(true, mPhone.getPhoneId(), true);
            } else {
                //android's default action
                Log.d(this, "isMSimModeSupport false, use default setRadioPower");
                mPhone.setRadioPower(true);
            }
            /// @}
        }
    }

    /**
     * Clean up when done with the whole sequence: either after successfully turning on the radio,
     * or after bailing out because of too many failures.
     *
     * The exact cleanup steps are:
     * - Notify callback if we still hadn't sent it a response.
     * - Double-check that we're not still registered for any telephony events
     * - Clean up any extraneous handler messages (like retry timeouts) still in the queue
     *
     * Basically this method guarantees that there will be no more activity from the
     * EmergencyCallHelper until someone kicks off the whole sequence again with another call to
     * {@link #startTurnOnRadioSequence}
     *
     * TODO: Do the work for the comment below:
     * Note we don't call this method simply after a successful call to placeCall(), since it's
     * still possible the call will disconnect very quickly with an OUT_OF_SERVICE error.
     */
    private void cleanup() {
        Log.d(this, "cleanup()");

        // This will send a failure call back if callback has yet to be invoked.  If the callback
        // was already invoked, it's a no-op.
        onComplete(false);

        unregisterForServiceStateChanged();
        cancelRetryTimer();

        // Used for unregisterForServiceStateChanged() so we null it out here instead.
        mPhone = null;
        mNumRetriesSoFar = 0;

        /// M: SS project ECC change feature @{
        cancelSwitchPhoneTimer();
        /// @}
    }

    private void startRetryTimer() {
        cancelRetryTimer();
        mHandler.sendEmptyMessageDelayed(MSG_RETRY_TIMEOUT, TIME_BETWEEN_RETRIES_MILLIS);
    }

    private void cancelRetryTimer() {
        mHandler.removeMessages(MSG_RETRY_TIMEOUT);
    }

    private void registerForServiceStateChanged() {
        // Unregister first, just to make sure we never register ourselves twice.  (We need this
        // because Phone.registerForServiceStateChanged() does not prevent multiple registration of
        // the same handler.)
        unregisterForServiceStateChanged();
        mPhone.registerForServiceStateChanged(mHandler, MSG_SERVICE_STATE_CHANGED, null);
    }

    private void unregisterForServiceStateChanged() {
        // This method is safe to call even if we haven't set mPhone yet.
        if (mPhone != null) {
            mPhone.unregisterForServiceStateChanged(mHandler);  // Safe even if unnecessary
        }
        mHandler.removeMessages(MSG_SERVICE_STATE_CHANGED);  // Clean up any pending messages too
    }

    private void onComplete(boolean isRadioReady) {
        if (mCallback != null) {
            Callback tempCallback = mCallback;
            mCallback = null;
            tempCallback.onComplete(isRadioReady);
        }
    }

    /// M: SS project ECC change feature @{
    public void startSwitchPhone(String number, Phone phone, boolean needToPowerOn,
            Callback callback) {
        mPhone = phone;
        mInitialPhoneType = mPhone.getPhoneType();
        if (TelephonyConnectionServiceUtil.getInstance().isGsmPreferredNumber(number)) {
            mTargetPhoneType = PhoneConstants.PHONE_TYPE_GSM;
        } else {
            mTargetPhoneType = PhoneConstants.PHONE_TYPE_CDMA;
        }
        Log.d(this, "startSwitchPhone, number:" + number + ", phoneType:" + phone.getPhoneType()
                + ", mTargetPhoneType:" + mTargetPhoneType);
        if (needToPowerOn) {
            startTurnOnRadioSequence(phone, callback);
        } else {
            mCallback = callback;
            mHandler.obtainMessage(MSG_START_SWITCH_PHONE).sendToTarget();
        }
    }

    private void startSwitchPhoneInternal() {
        Log.d(this, "startSwitchPhoneInternal()");
        if (!mTm.hasIccCard(0)) {
            if (mTargetPhoneType == PhoneConstants.PHONE_TYPE_GSM) {
                mPhone.exitEmergencyCallbackMode();
            }
            mPhone.triggerModeSwitchByEcc(mTargetPhoneType ==
                    PhoneConstants.PHONE_TYPE_CDMA ? MODE_C2K : MODE_GSM,
                    mHandler.obtainMessage(MSG_MODE_SWITCH_RESULT));
        } else {
            Log.d(this, "startSwitchPhoneInternal, no need to switch phone!");
            makePhoneCallCheck();
        }
    }

    private void makePhoneCallCheck() {
        PhoneConstants.State phoneState = mPhone.getState();
        int serviceState = mPhone.getServiceState().getState();
        Log.d(this, "makePhoneCallCheck, phonetype:" + mPhone.getPhoneType()
                + ", phoneState:" + phoneState
                + ", serviceState:" + mPhone.getServiceState());
        if (isOkToCall(serviceState, phoneState)
        /// M: CC038: [ALPS01933697]Allow ECC if ServiceState isEmergencyOnly is true @{
                || (mPhone.getServiceState().isEmergencyOnly()
                && mPhone.getPhoneType() != PhoneConstants.PHONE_TYPE_CDMA)) {
        /// @}
            // Woo hoo -- we successfully got out of airplane mode.
            onComplete(true);
            cleanup();
        } else {
            //startTurnOnRadioSequence(mPhone, mCallback);
            powerOnRadio();
            startRetryTimer();
        }
    }

    private void startSwitchPhoneTimer() {
        cancelRetryTimer();
        mHandler.sendEmptyMessageDelayed(MSG_SWITCH_PHONE_TIMEOUT, SWITCH_PHONE_TIMEOUT_MILLIS);
    }

    private void cancelSwitchPhoneTimer() {
        mHandler.removeMessages(MSG_SWITCH_PHONE_TIMEOUT);
        mHandler.removeMessages(MSG_START_SWITCH_PHONE);
    }

    public void onDestroy() {
        Log.d(this, "onDestroy, mRegisterReceiver:" + mRegisterReceiver);
        if (mRegisterReceiver) {
            mContext.unregisterReceiver(mReceiver);
            mRegisterReceiver = false;
        }
        mContext.getContentResolver().unregisterContentObserver(mAirplaneModeObserver);
        cleanup();
        /// M: For delay SIM switch flow @{
        TelephonyManagerEx.getDefault().setEccInProgress(false);
        Intent intent = new Intent(TelephonyManagerEx.ACTION_ECC_IN_PROGRESS);
        intent.putExtra(TelephonyManagerEx.EXTRA_IN_PROGRESS, false);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        /// @}
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(EmergencyCallHelper.this, "Received:" + action);
            if (mPhone != null) {
                Log.d(EmergencyCallHelper.this, "mPhone:" + mPhone + ", activePhone:"
                        + ((com.android.internal.telephony.PhoneProxy) mPhone).getActivePhone()
                        + ", service state:" + mPhone.getServiceState().getState()
                        + ", phoneType:" + mPhone.getPhoneType()
                        + ", mTargetPhoneType:" + mTargetPhoneType);
                if (TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED.equals(action)) {
                    if (mPhone.getPhoneType() == mTargetPhoneType) {
                        cancelSwitchPhoneTimer();
                        makePhoneCallCheck();
                    }
                } else if (TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED.equals(action)) {
                    SubscriptionManager sm = SubscriptionManager.from(mContext);
                    if (sm.getActiveSubscriptionInfoCount() > 0) {
                        Log.d(EmergencyCallHelper.this, "No need to switch phone anymore!");
                        mHandler.removeMessages(MSG_START_SWITCH_PHONE);
                        makePhoneCallCheck();
                    }
                }
            }
        }
    }

    private class AirplaneModeObserver extends ContentObserver {
        private Context mMyContext;
        public AirplaneModeObserver(Handler handler, Context context) {
            super(handler);
            mMyContext = context;
            mMyContext.getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            boolean isAirplaneModeOn = Settings.Global.getInt(
                    mContext.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) > 0;
            Log.d(EmergencyCallHelper.this, "onChange, isAirplaneModeOn:" + isAirplaneModeOn);
            if (isAirplaneModeOn) {
                cleanup();
                return;
            }
        }
    }
    /// @}
}
