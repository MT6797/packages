package com.mediatek.incallui.videocall;

import android.os.SystemClock;

import android.telecom.VideoProfile;
import com.android.incallui.Call;
import com.android.incallui.CallList;
import com.android.incallui.CallTimer;
import com.android.incallui.InCallPresenter;
import com.android.incallui.InCallVideoCallCallbackNotifier;
import com.android.incallui.Log;

import com.google.common.base.Preconditions;

/**
 * M: [Video Call] A helper to downgrade video call if necessary.
 * Especially downgrade when UI in background or quit.
 */
public class VideoSessionController implements InCallPresenter.InCallStateListener,
        InCallPresenter.IncomingCallListener,
        InCallVideoCallCallbackNotifier.SessionModificationListener {
    private static final boolean DEBUG = true;
    private static final int DEFAULT_COUNT_DOWN_SECONDS = 20;
    private static final long MILLIS_PER_SECOND = 1000;
    private static VideoSessionController sInstance;
    private InCallPresenter mInCallPresenter;
    private Call mPrimaryCall;
    private AutoDeclineTimer mAutoDeclineTimer = new AutoDeclineTimer();

    private VideoSessionController() {
        // do nothing
    }

    /**
     * M: get the VideoSessionController instance.
     * @return the instance.
     */
    public static VideoSessionController getInstance() {
        if (sInstance == null) {
            sInstance = new VideoSessionController();
        }
        return sInstance;
    }

    /**
     * M: setup when InCallPresenter setUp.
     * @param inCallPresenter the InCallPresenter instance.
     */
    public void setUp(InCallPresenter inCallPresenter) {
        logd("setUp");
        mInCallPresenter = Preconditions.checkNotNull(inCallPresenter);
        mInCallPresenter.addListener(this);
        mInCallPresenter.addIncomingCallListener(this);

        //register session modification listener for local.
        InCallVideoCallCallbackNotifier.getInstance().addSessionModificationListener(this);
    }

    /**
     * M: tearDown when InCallPresenter tearDown.
     */
    public void tearDown() {
        logd("tearDown...");
        mInCallPresenter.removeListener(this);
        mInCallPresenter.removeIncomingCallListener(this);

        //unregister session modification listener.
        InCallVideoCallCallbackNotifier.getInstance().removeSessionModificationListener(this);

        clear();
    }

    /**
     * M: get the countdown second number.
     * @return countdown number.
     */
    public long getAutoDeclineCountdownSeconds() {
        return mAutoDeclineTimer.getAutoDeclineCountdown();
    }

    @Override
    public void onStateChange(InCallPresenter.InCallState oldState,
                              InCallPresenter.InCallState newState, CallList callList) {
        Call call;
        if (newState == InCallPresenter.InCallState.INCOMING) {
            call = callList.getIncomingCall();
        } else if (newState == InCallPresenter.InCallState.WAITING_FOR_ACCOUNT) {
            call = callList.getWaitingForAccountCall();
        } else if (newState == InCallPresenter.InCallState.PENDING_OUTGOING) {
            call = callList.getPendingOutgoingCall();
        } else if (newState == InCallPresenter.InCallState.OUTGOING) {
            call = callList.getOutgoingCall();
        } else {
            call = callList.getActiveOrBackgroundCall();
        }

        if (!Call.areSame(call, mPrimaryCall)) {
            onPrimaryCallChanged(call);
        }
    }

    @Override
    public void onIncomingCall(InCallPresenter.InCallState oldState,
                               InCallPresenter.InCallState newState, Call call) {
        if (!Call.areSame(call, mPrimaryCall)) {
            onPrimaryCallChanged(call);
        }
    }

    /**
     * M: When upgrade request received, start timing.
     * @param call the call upgrading.
     */
    public void startTimingForAutoDecline(Call call) {
        logi("[startTimingForAutoDecline] for call: " + getId(call));
        if (!Call.areSame(call, mPrimaryCall)) {
            Log.e(this, "[startTimingForAutoDecline]Abnormal case for a non-primary call " +
                    "receiving upgrade request.");
            onPrimaryCallChanged(call);
        }
        mAutoDeclineTimer.startTiming();
    }

    /**
     * M: stop timing when the request accepted or declined.
     */
    public void stopTiming() {
        mAutoDeclineTimer.stopTiming();
    }

    private void onPrimaryCallChanged(Call call) {
        logi("[onPrimaryCallChanged] " + getId(mPrimaryCall) + " -> " + getId(call));
        if (call != null && mPrimaryCall != null && mPrimaryCall.getSessionModificationState()
                == Call.SessionModificationState.RECEIVED_UPGRADE_TO_VIDEO_REQUEST) {
            /**
             * force decline upgrade request if primary call changed.
             */
            mInCallPresenter.declineUpgradeRequest(mInCallPresenter.getContext());
        }
        mPrimaryCall = call;
    }

    private void clear() {
        mInCallPresenter = null;
        // when mInCallPresenter is null ,eg peer disconnect call,
        // local should stop timer.
        stopTiming();
    }

    private void logd(String msg) {
        if (DEBUG) {
            Log.d(this, msg);
        }
    }

    private void logw(String msg) {
        if (DEBUG) {
            Log.w(this, msg);
        }
    }

    private void logi(String msg) {
        Log.i(this, msg);
    }

    private static String getId(Call call) {
        return call == null ? "null" : call.getId();
    }

    @Override
    public void onUpgradeToVideoRequest(Call call, int videoState) {
        logd("onUpgradeToVideoRequest callId = " + getId(call) + " new video state = "
                + videoState);
        if (mPrimaryCall == null || !Call.areSame(mPrimaryCall, call)) {
            logw("UpgradeToVideoRequest received for non-primary call");
        }

        if (call == null) {
            logw("UpgradeToVideoRequest the current call is null");
            return;
        }

        call.setSessionModificationTo(videoState);
    }

    @Override
    public void onUpgradeToVideoSuccess(Call call) {
        logd("onUpgradeToVideoSuccess callId=" + getId(call));
        if (mPrimaryCall == null || !Call.areSame(mPrimaryCall, call)) {
            logw("onUpgradeToVideoSuccess received for non-primary call");
        }

        if (call == null) {
            logw("onUpgradeToVideoSuccess the current call is null");
            return;
        }

        /// fix ALPS02681041,show message only when upgrade to video from voice call successfully.
        /// M: [ALPS02671613] Changed to one-way state from 2-ways, nothing would happen, neither.
        /// TODO: Currently no well-support to one-way video call, so change from/to
        /// one-way video call success/fail would have no prompt.
        /// M: fix CR:ALPS02707358,shouldn't show "failed to switch video call" toast
        /// when call is disconnecting or call has been disconnected. @{
        if (VideoProfile.isAudioOnly(call.getModifyVideoStateFrom()) &&
                Call.State.isConnectingOrConnected(call.getState())) {
            InCallPresenter.getInstance().showMessage(
                    com.android.incallui.R.string.video_call_upgrade_to_video_call);
        } else {
            logd("onUpgradeToVideoSuccess call is disconnecting or call has been disconnected");
        }
        /// @}
        ///fix ALPS02497928,stop recording if switch to video call
        // requested by local.@{
        if (InCallPresenter.getInstance().isRecording()) {
            InCallPresenter.getInstance().stopVoiceRecording();
        }
        /// @}
    }

    @Override
    public void onUpgradeToVideoFail(int status, Call call) {
        logd("onUpgradeToVideoFail callId=" + getId(call));
        if (mPrimaryCall == null || !Call.areSame(mPrimaryCall, call)) {
            logw("onUpgradeToVideoFail received for non-primary call");
        }

        if (call == null) {
            logw("onUpgradeToVideoFail the current call is nul");
            return;
        }

        /// M: show message when upgrade to video fail
        /// TODO: Currently no well-support to one-way video call, so change from/to
        /// one-way video call success/fail would have no prompt.
        /// Note: there's no way to change from audio to one-way. So no prompt for "pause fail"
        /// neither. ref: [ALPS02704527]
        /// M: fix CR:ALPS02707358,shouldn't show "failed to switch video call" toast
        /// when call is disconnecting or call has been disconnected. @{
        if (VideoProfile.isAudioOnly(call.getModifyVideoStateFrom()) &&
                Call.State.isConnectingOrConnected(call.getState())) {
            InCallPresenter.getInstance().showMessage(
                    com.android.incallui.R.string.video_call_upgrade_to_video_call_failed);
        } else {
            logd("onUpgradeToVideoFail call is disconnecting or call has been disconnected");
        }
        /// @}
    }

    @Override
    public void onDowngradeToAudio(Call call) {
        logd("[onDowngradeToAudio]for callId: " + getId(call));
        if (call == null) {
            logw("onDowngradeToAudio the current call is nul");
            return;
        }
        //reset hide preview flag
        call.setHidePreview(false);

        //Google source code
        call.setSessionModificationState(Call.SessionModificationState.NO_REQUEST);

        //show message when downgrade to voice
        /// M: fix CR:ALPS02707358,shouldn't show "failed to switch video call" toast
        /// when call is disconnecting or call has been disconnected. @{
        if (Call.State.isConnectingOrConnected(call.getState())) {
            InCallPresenter.getInstance().showMessage(
                com.android.incallui.R.string.video_call_downgrade_to_voice_call);
        } else {
            logd("onDowngradeToAudio call is disconnecting or call has been disconnected");
        }
        /// @}
    }

    /**
     * M: Timer to countdown.
     */
    private class AutoDeclineTimer {
        private int mCountdownSeconds;
        private CallTimer mTimer;
        private long mTimingStartMillis;
        private long mRemainSecondsBeforeDecline = -1;

        AutoDeclineTimer() {
            mTimer = new CallTimer(new Runnable() {
                @Override
                public void run() {
                    updateCountdown();
                }
            });
        }

        public void startTiming() {
            //TODO: customer might need some other value for this.
            mCountdownSeconds = DEFAULT_COUNT_DOWN_SECONDS;
            mRemainSecondsBeforeDecline = mCountdownSeconds;
            mTimingStartMillis = SystemClock.uptimeMillis();
            mTimer.start(MILLIS_PER_SECOND);
        }

        public long getAutoDeclineCountdown() {
            return mRemainSecondsBeforeDecline;
        }

        public void stopTiming() {
            mTimer.cancel();
            mRemainSecondsBeforeDecline = -1;
        }

        private void updateCountdown() {
            long currentMillis = SystemClock.uptimeMillis();
            long elapsedSeconds = (currentMillis - mTimingStartMillis) / MILLIS_PER_SECOND;
            if (elapsedSeconds > mCountdownSeconds) {
                if(mInCallPresenter == null) {
                    logd("[updateCountdown]mInCallPresenter is null return");
                    return;
                }
                mInCallPresenter.declineUpgradeRequest(mInCallPresenter.getContext());
            } else {
                mRemainSecondsBeforeDecline = mCountdownSeconds - elapsedSeconds;
                updateRelatedUi();
            }
        }

        private void updateRelatedUi() {
            logd("[updateRelatedUi]remain seconds: " + mRemainSecondsBeforeDecline);
            if(mInCallPresenter == null) {
                logd("[updateRelatedUi]mInCallPresenter is null return");
                return;
            }
            mInCallPresenter.onAutoDeclineCountdownChanged();
        }
    }

}
