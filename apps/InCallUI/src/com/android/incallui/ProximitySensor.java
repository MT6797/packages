/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.incallui;

import android.content.Context;
import android.content.res.Configuration;
import android.os.PowerManager;
import android.telecom.CallAudioState;

import android.telecom.VideoProfile;
import com.android.incallui.AudioModeProvider.AudioModeListener;
import com.android.incallui.InCallPresenter.InCallState;
import com.android.incallui.InCallPresenter.InCallStateListener;
import com.android.services.telephony.common.AudioMode;
import com.google.common.base.Objects;

/**
 * Class manages the proximity sensor for the in-call UI.
 * We enable the proximity sensor while the user in a phone call. The Proximity sensor turns off
 * the touchscreen and display when the user is close to the screen to prevent user's cheek from
 * causing touch events.
 * The class requires special knowledge of the activity and device state to know when the proximity
 * sensor should be enabled and disabled. Most of that state is fed into this class through
 * public methods.
 */
public class ProximitySensor implements AccelerometerListener.OrientationListener,
        InCallStateListener, AudioModeListener, InCallPresenter.InCallDetailsListener {
    private static final String TAG = ProximitySensor.class.getSimpleName();

    private final PowerManager mPowerManager;
    private final PowerManager.WakeLock mProximityWakeLock;
    private final AudioModeProvider mAudioModeProvider;
    private final AccelerometerListener mAccelerometerListener;
    private int mOrientation = AccelerometerListener.ORIENTATION_UNKNOWN;
    private boolean mUiShowing = false;
    private boolean mIsPhoneOffhook = false;
    private boolean mDialpadVisible;

    // True if the keyboard is currently *not* hidden
    // Gets updated whenever there is a Configuration change
    private boolean mIsHardKeyboardOpen;

    private Context mContext;
    ///M: fix ALPS02535607,add a parameter to record current video state
    private int mCurVideoState = VideoProfile.STATE_AUDIO_ONLY;

    public ProximitySensor(Context context, AudioModeProvider audioModeProvider) {
        mContext = context;
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (mPowerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            mProximityWakeLock = mPowerManager.newWakeLock(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, TAG);
        } else {
            Log.w(TAG, "Device does not support proximity wake lock.");
            mProximityWakeLock = null;
        }
        mAccelerometerListener = new AccelerometerListener(context, this);
        mAudioModeProvider = audioModeProvider;

        mAudioModeProvider.addListener(this);
    }

    public void tearDown() {
        mAudioModeProvider.removeListener(this);
        mAccelerometerListener.enable(false);

        turnOffProximitySensor(true);
    }

    /**
     * Called to identify when the device is laid down flat.
     */
    @Override
    public void orientationChanged(int orientation) {
        mOrientation = orientation;
        updateProximitySensorMode();
    }

    /**
     * Called to keep track of the overall UI state.
     */
    @Override
    public void onStateChange(InCallState oldState, InCallState newState, CallList callList) {
        // We ignore incoming state because we do not want to enable proximity
        // sensor during incoming call screen. We check hasLiveCall() because a disconnected call
        // can also put the in-call screen in the INCALL state.
        boolean hasOngoingCall = InCallState.INCALL == newState && callList.hasLiveCall();
        boolean isOffhook = (InCallState.OUTGOING == newState) || hasOngoingCall;

        if (isOffhook != mIsPhoneOffhook) {
            mIsPhoneOffhook = isOffhook;

            mOrientation = AccelerometerListener.ORIENTATION_UNKNOWN;
            mAccelerometerListener.enable(mIsPhoneOffhook);

            updateProximitySensorMode();
        }
    }

    @Override
    public void onSupportedAudioMode(int modeMask) {
    }

    @Override
    public void onMute(boolean muted) {
    }

    /**
     * Called when the audio mode changes during a call.
     */
    @Override
    public void onAudioMode(int mode) {
        updateProximitySensorMode();
    }

    public void onDialpadVisible(boolean visible) {
        mDialpadVisible = visible;
        updateProximitySensorMode();
    }

    /**
     * Called by InCallActivity to listen for hard keyboard events.
     */
    public void onConfigurationChanged(Configuration newConfig) {
        mIsHardKeyboardOpen = newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;

        // Update the Proximity sensor based on keyboard state
        updateProximitySensorMode();
    }

    /**
     * Used to save when the UI goes in and out of the foreground.
     */
    public void onInCallShowing(boolean showing) {
        if (showing) {
            mUiShowing = true;

        // We only consider the UI not showing for instances where another app took the foreground.
        // If we stopped showing because the screen is off, we still consider that showing.
        } else if (mPowerManager.isScreenOn()) {
            mUiShowing = false;
        }
        updateProximitySensorMode();
    }

    /**
     * TODO: There is no way to determine if a screen is off due to proximity or if it is
     * legitimately off, but if ever we can do that in the future, it would be useful here.
     * Until then, this function will simply return true of the screen is off.
     */
    public boolean isScreenReallyOff() {
        return !mPowerManager.isScreenOn();
    }

    private void turnOnProximitySensor() {
        if (mProximityWakeLock != null) {
            if (!mProximityWakeLock.isHeld()) {
                Log.i(this, "Acquiring proximity wake lock");
                mProximityWakeLock.acquire();
            } else {
                Log.i(this, "Proximity wake lock already acquired");
            }
        }
    }

    private void turnOffProximitySensor(boolean screenOnImmediately) {
        if (mProximityWakeLock != null) {
            if (mProximityWakeLock.isHeld()) {
                Log.i(this, "Releasing proximity wake lock");
                int flags =
                    (screenOnImmediately ? 0 : PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY);
                mProximityWakeLock.release(flags);
            } else {
                Log.i(this, "Proximity wake lock already released");
            }
        }
    }

    /**
     * Updates the wake lock used to control proximity sensor behavior,
     * based on the current state of the phone.
     *
     * On devices that have a proximity sensor, to avoid false touches
     * during a call, we hold a PROXIMITY_SCREEN_OFF_WAKE_LOCK wake lock
     * whenever the phone is off hook.  (When held, that wake lock causes
     * the screen to turn off automatically when the sensor detects an
     * object close to the screen.)
     *
     * This method is a no-op for devices that don't have a proximity
     * sensor.
     *
     * Proximity wake lock will *not* be held if any one of the
     * conditions is true while on a call:
     * 1) If the audio is routed via Bluetooth
     * 2) If a wired headset is connected
     * 3) if the speaker is ON
     * 4) If the slider is open(i.e. the hardkeyboard is *not* hidden)
     */
    private synchronized void updateProximitySensorMode() {
        final int audioMode = mAudioModeProvider.getAudioMode();
        /// M: Add for video call. @{
        CallList callList = CallList.getInstance();
        boolean isVideoCall = callList.getFirstCall() != null
                && callList.getFirstCall().isVideoCall(mContext);
        /// @}

        // turn proximity sensor off and turn screen on immediately if
        // we are using a headset, the keyboard is open, or the device
        // is being held in a horizontal position.
            boolean screenOnImmediately = (CallAudioState.ROUTE_WIRED_HEADSET == audioMode
                    || CallAudioState.ROUTE_SPEAKER == audioMode
                    || CallAudioState.ROUTE_BLUETOOTH == audioMode
                    || mIsHardKeyboardOpen);

            // We do not keep the screen off when the user is outside in-call screen and we are
            // horizontal, but we do not force it on when we become horizontal until the
            // proximity sensor goes negative.
            final boolean horizontal =
                    (mOrientation == AccelerometerListener.ORIENTATION_HORIZONTAL);
            /// M: fix CR:ALPS02324455,cover the p-sensor,press power key,
            /// the screen always keep off if phone was put horizontal.
            /// for ALPS01230940, the PowerMangerService would force requiring wake lock
            /// while user press power key.(In case turning on screen even if the p-sensor
            /// was broken) The InCallUI's release and PMS's acquire to the wake lock
            /// would always conflict due to AMS lifecycle. So we do nothing special for
            /// horizontal case in InCallUI in order to light up screen anytime power pressed.
            /// so delete google code:
            /// screenOnImmediately |= !mUiShowing && horizontal;

            // We do not keep the screen off when dialpad is visible, we are horizontal, and
            // the in-call screen is being shown.
            // At that moment we're pretty sure users want to use it, instead of letting the
            // proximity sensor turn off the screen by their hands.
            screenOnImmediately |= mDialpadVisible && horizontal;

            Log.v(this, "screenonImmediately: ", screenOnImmediately);

            Log.i(this, Objects.toStringHelper(this)
                    .add("keybrd", mIsHardKeyboardOpen ? 1 : 0)
                    .add("dpad", mDialpadVisible ? 1 : 0)
                    .add("offhook", mIsPhoneOffhook ? 1 : 0)
                    .add("hor", horizontal ? 1 : 0)
                    .add("ui", mUiShowing ? 1 : 0)
                    .add("aud", CallAudioState.audioRouteToString(audioMode))
                    .toString());

            /// M: disable Proximity Sensor during VT Call
            if (mIsPhoneOffhook && !screenOnImmediately && !isVideoCall) {
                Log.d(this, "Turning on proximity sensor");
                // Phone is in use!  Arrange for the screen to turn off
                // automatically when the sensor detects a close object.

                /// M: for ALPS01275578 @{
                // when reject a incoming call, the call state is INCALL, but we should NOT
                // acquire wake lock in this case
                if (!shouldSkipAcquireProximityLock()) {
                    turnOnProximitySensor();
                }

            } else {
                Log.d(this, "Turning off proximity sensor");
                // Phone is either idle, or ringing.  We don't want any special proximity sensor
                // behavior in either case.

                /// M: For ALPS01769498 @{
                // Screen on immediately for incoming call, this give user a chance to notice
                // the new incoming call when speaking on an existed call.
                if (InCallPresenter.getInstance().getPotentialStateFromCallList(callList)
                        == InCallState.INCOMING) {
                    Log.d(this, "Screen on immediately for incoming call");
                    screenOnImmediately = true;
                }
                /// @}
                turnOffProximitySensor(screenOnImmediately);
            }
        }

    /**
     * M: check whether should skip take proximityLock for some special cases
     * eg: no active calls
     */
    private boolean shouldSkipAcquireProximityLock() {
        CallList callList = CallList.getInstance();
        if (InCallPresenter.getInstance() != null && callList != null) {
            if (InCallPresenter.getInstance().getInCallState() == InCallState.INCALL
                    && callList.getActiveCall() == null && callList.getBackgroundCall() == null) {
                Log.d(this, "no active call when INCALL state, skip Acquire Proximity Lock~~");
                return true;
            }
        }
        return false;
    }

    /**
     * M:FIXME, add onDetailChange for update updateProximitySensorMode again in vilte call
     * downgrade to volte call.
     * If more than 1 calls, there might be some unnecessary update, and might cause some problems
     * like performance decline or P-sensor defects.
     * @param call
     * @param details
     */
    @Override
    public void onDetailsChanged(Call call, android.telecom.Call.Details details) {
        if (call == null) {
            Log.e(this, "onDetailsChanged call is null ");
        }
        if (!CallUtils.isVideoCall(call) && mCurVideoState != call.getVideoState()) {
            updateProximitySensorMode();
        }
        mCurVideoState = call.getVideoState();
    }
}
