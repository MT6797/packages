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

import com.android.dialer.util.TelecomUtil;
import com.android.incallui.InCallPresenter.InCallState;

import android.telecom.VideoProfile;

import java.util.List;

/**
 * Presenter for the Incoming call widget. The {@link AnswerPresenter} handles the logic during
 * incoming calls. It is also in charge of responding to incoming calls, so there needs to be
 * an instance alive so that it can receive onIncomingCall callbacks.
 *
 * An instance of {@link AnswerPresenter} is created by InCallPresenter at startup, registers
 * for callbacks via InCallPresenter, and shows/hides the {@link AnswerFragment} via IncallActivity.
 *
 */
public class AnswerPresenter extends Presenter<AnswerPresenter.AnswerUi>
        implements CallList.CallUpdateListener, InCallPresenter.InCallUiListener,
                InCallPresenter.IncomingCallListener,
                CallList.Listener {

    private static final String TAG = AnswerPresenter.class.getSimpleName();

    private String mCallId;
    private Call mCall = null;
    private boolean mHasTextMessages = false;

    @Override
    public void onUiShowing(boolean showing) {
        if (showing) {
            final CallList calls = CallList.getInstance();
            Call call;
            call = calls.getIncomingCall();
            if (call != null) {
                processIncomingCall(call);
            }
            call = calls.getVideoUpgradeRequestCall();
            Log.d(this, "getVideoUpgradeRequestCall call =" + call);
            if (call != null) {
                processVideoUpgradeRequestCall(call);
            }
            /// M: fix ALPS02273765, hide answer ui if no incoming call exists. @{
            if (calls.getIncomingCall() == null && calls.getVideoUpgradeRequestCall() == null) {
                Log.d(this, "[onUiShowing] hide answer ui!");
                showAnswerUi(false);
            }
            /// @}
            /// M: [Video call] need onUpdateToVideo() @{
            calls.addListener(this);
           /// @}
        } else {
            // This is necessary because the activity can be destroyed while an incoming call exists.
            // This happens when back button is pressed while incoming call is still being shown.
            if (mCallId != null) {
                CallList.getInstance().removeCallUpdateListener(mCallId, this);
            }
            /// M: [Video call] need onUpdateToVideo() @{
            CallList.getInstance().removeListener(this);
           /// @}
        }
    }

    @Override
    public void onIncomingCall(InCallState oldState, InCallState newState, Call call) {
        Log.d(this, "onIncomingCall: " + this);
        /// M: fix ALPS02212771, avoid null point exception. @{
        if (getUi() == null) {
            return;
        }
        /// @}
        Call modifyCall = CallList.getInstance().getVideoUpgradeRequestCall();
        if (modifyCall != null) {
            showAnswerUi(false);
            Log.d(this, "declining upgrade request id: ");
            CallList.getInstance().removeCallUpdateListener(mCallId, this);
            InCallPresenter.getInstance().declineUpgradeRequest(getUi().getContext());
        }
        if (!call.getId().equals(mCallId)) {
            // A new call is coming in.
            /// M: when another incoming call coming, need dismiss dialog. @{
            getUi().dismissPendingDialogs();
            /// @}
            processIncomingCall(call);
        }
    }

    @Override
    public void onIncomingCall(Call call) {
    }

    @Override
    public void onCallListChange(CallList list) {
    }

    @Override
    public void onDisconnect(Call call) {
        // no-op
    }

    public void onSessionModificationStateChange(int sessionModificationState) {
        boolean isUpgradePending = sessionModificationState ==
                Call.SessionModificationState.RECEIVED_UPGRADE_TO_VIDEO_REQUEST;

        if (!isUpgradePending) {
            // Stop listening for updates.
            CallList.getInstance().removeCallUpdateListener(mCallId, this);
            showAnswerUi(false);
        }
    }

    private boolean isVideoUpgradePending(Call call) {
        return call.getSessionModificationState()
                == Call.SessionModificationState.RECEIVED_UPGRADE_TO_VIDEO_REQUEST;
    }

    @Override
    public void onUpgradeToVideo(Call call) {
        Log.d(this, "onUpgradeToVideo: " + this + " call=" + call);
        /// M: fix ALPS02518886,not process the video call request if multiple call exist.@{
        if (!call.getVideoFeatures().canUpgradeToVideoCall()) {
            return;
        }
        /// @}
        if (getUi() == null) {
            Log.d(this, "onUpgradeToVideo ui is null");
            ///M: when in MO state , the answer fragment is not init
            // so need to new AnswerFragment @{
            showAnswerUi();
            //return;
            ///@}
        }
        boolean isUpgradePending = isVideoUpgradePending(call);
        InCallPresenter inCallPresenter = InCallPresenter.getInstance();
        if (isUpgradePending
                && inCallPresenter.getInCallState() == InCallPresenter.InCallState.INCOMING) {
            Log.d(this, "declining upgrade request");
            //If there is incoming call reject upgrade request
            inCallPresenter.declineUpgradeRequest(getUi().getContext());
        } else if (isUpgradePending) {
            Log.d(this, "process upgrade request as no MT call");
            processVideoUpgradeRequestCall(call);
        }
    }

    private void processIncomingCall(Call call) {
        mCallId = call.getId();
        mCall = call;

        // Listen for call updates for the current call.
        CallList.getInstance().addCallUpdateListener(mCallId, this);

        Log.d(TAG, "Showing incoming for call id: " + mCallId + " " + this);
        if (showAnswerUi(true)) {
            final List<String> textMsgs = CallList.getInstance().getTextResponses(call.getId());
            configureAnswerTargetsForSms(call, textMsgs);
        }

        ///M: ALPS01856138
        // dismiss message dialog if showing call has not RESPOND_VIA_TEXT capability @{
        if (getUi() != null
                && !call.can(android.telecom.Call.Details.CAPABILITY_RESPOND_VIA_TEXT)) {
            getUi().dismissPendingDialogs();
        }
        /// @}
    }

    private boolean showAnswerUi(boolean show) {
        final InCallActivity activity = InCallPresenter.getInstance().getActivity();
        if (activity != null) {
            activity.showAnswerFragment(show);
            if (getUi() != null) {
                getUi().onShowAnswerUi(show);
            }
            return true;
        } else {
            return false;
        }
    }

    private void processVideoUpgradeRequestCall(Call call) {
        Log.d(this, " processVideoUpgradeRequestCall call=" + call);
        mCallId = call.getId();
        mCall = call;

        // Listen for call updates for the current call.
        CallList.getInstance().addCallUpdateListener(mCallId, this);

        final int currentVideoState = call.getVideoState();
        final int modifyToVideoState = call.getModifyToVideoState();

        if (currentVideoState == modifyToVideoState) {
            Log.w(this, "processVideoUpgradeRequestCall: Video states are same. Return.");
            return;
        }
        ///M: fix bug for ALPS02509401, when dial with video upgrade request,
        //we should always show answerfragment. @{
        showAnswerUi(true);
        AnswerUi ui = getUi();

        if (ui == null) {
            Log.e(this, "Ui is null. Can't process upgrade request");
            return;
        }
        //showAnswerUi(true);
        ///@ }
        ui.showTargets(AnswerFragment.TARGET_SET_FOR_VIDEO_ACCEPT_REJECT_REQUEST,
                modifyToVideoState);
    }

    private boolean isEnabled(int videoState, int mask) {
        return (videoState & mask) == mask;
    }

    @Override
    public void onCallChanged(Call call) {
        Log.d(this, "onCallStateChange() " + call + " " + this);
        /// M: For ALPS02014302. When onCallChanged callback, getUi() maybe
        // null. @{
        if (getUi() == null) {
            Log.d(this, "onCallChanged, ui is null, do nothing! ");
            return;
        }
        /// @}
        if (call.getState() != Call.State.INCOMING) {
            boolean isUpgradePending = isVideoUpgradePending(call);
            if (!isUpgradePending) {
                // Stop listening for updates.
                CallList.getInstance().removeCallUpdateListener(mCallId, this);
            }

            final Call incall = CallList.getInstance().getIncomingCall();
            if (incall != null || isUpgradePending) {
                /// M: if foreground incoming call state changed, need dismiss dialog. @{
                if (call.getId().equals(mCallId)) {
                    getUi().dismissPendingDialogs();
                }
                /// @}
                showAnswerUi(true);
            } else {
                showAnswerUi(false);
            }

            mHasTextMessages = false;
        } else if (!mHasTextMessages) {
            final List<String> textMsgs = CallList.getInstance().getTextResponses(call.getId());
            if (textMsgs != null) {
                configureAnswerTargetsForSms(call, textMsgs);
            }
        }
    }

    public void onAnswer(int videoState, Context context) {
        if (mCallId == null) {
            return;
        }

        if (mCall.getSessionModificationState()
                == Call.SessionModificationState.RECEIVED_UPGRADE_TO_VIDEO_REQUEST) {
            Log.d(this, "onAnswer (upgradeCall) mCallId=" + mCallId + " videoState=" + videoState);
            InCallPresenter.getInstance().acceptUpgradeRequest(videoState, context);
        } else {
            Log.d(this, "onAnswer (answerCall) mCallId=" + mCallId + " videoState=" + videoState);
            /// M: [log optimize]
            Log.op(mCall, Log.CcOpAction.ANSWER, "answer in AnswerFragment: " + videoState);
            TelecomAdapter.getInstance().answerCall(mCall.getId(), videoState);
        }
    }

    /**
     * TODO: We are using reject and decline interchangeably. We should settle on
     * reject since it seems to be more prevalent.
     */
    public void onDecline(Context context) {
        Log.d(this, "onDecline " + mCallId);
        if (mCall.getSessionModificationState()
                == Call.SessionModificationState.RECEIVED_UPGRADE_TO_VIDEO_REQUEST) {
            InCallPresenter.getInstance().declineUpgradeRequest(context);
        } else {
            /// M: [log optimize]
            Log.op(mCall, Log.CcOpAction.REJECT, "reject in AnswerFragment.");
            TelecomAdapter.getInstance().rejectCall(mCall.getId(), false, null);
        }
    }

    public void onText() {
        if (getUi() != null) {
            TelecomUtil.silenceRinger(getUi().getContext());
            getUi().showMessageDialog();
        }
    }

    public void rejectCallWithMessage(String message) {
        Log.d(this, "sendTextToDefaultActivity()...");
        /// M: [log optimize]
        Log.op(mCall, Log.CcOpAction.REJECT, "reject with message.");
        TelecomAdapter.getInstance().rejectCall(mCall.getId(), true, message);
        /// M: ALPS01766524. If Call ended, then send sms. @{
        if (mCall.getHandle() != null && mCall.can(android.telecom.Call.Details.CAPABILITY_RESPOND_VIA_TEXT)) {
            TelecomAdapter.getInstance().sendMessageIfCallEnded(getUi().getContext(),
                    mCall.getId(), mCall.getHandle().getSchemeSpecificPart(), message);
        }
        /// @}

        onDismissDialog();
    }

    public void onDismissDialog() {
        InCallPresenter.getInstance().onDismissDialog();
    }

    private void configureAnswerTargetsForSms(Call call, List<String> textMsgs) {
        if (getUi() == null) {
            return;
        }
        mHasTextMessages = textMsgs != null;
        boolean withSms =
                call.can(android.telecom.Call.Details.CAPABILITY_RESPOND_VIA_TEXT)
                && mHasTextMessages;

        // Only present the user with the option to answer as a video call if the incoming call is
        // a bi-directional video call.
        if (VideoProfile.isBidirectional((call.getVideoState()))) {
            /**
             * M: [Video call]3G video call can't answer as voice, nor reject via SMS. @{
             */
            if (!call.getVideoFeatures().supportsAnswerAsVoice()) {
                getUi().showTargets(AnswerFragment.TARGET_SET_FOR_VIDEO_WITHOUT_SMS_AUDIO);
                return;
            }
            /** @} */
            if (withSms) {
                getUi().showTargets(AnswerFragment.TARGET_SET_FOR_VIDEO_WITH_SMS);
                getUi().configureMessageDialog(textMsgs);
            } else {
                getUi().showTargets(AnswerFragment.TARGET_SET_FOR_VIDEO_WITHOUT_SMS);
            }
        } else {
            if (withSms) {
                getUi().showTargets(AnswerFragment.TARGET_SET_FOR_AUDIO_WITH_SMS);
                getUi().configureMessageDialog(textMsgs);
            } else {
                getUi().showTargets(AnswerFragment.TARGET_SET_FOR_AUDIO_WITHOUT_SMS);
            }
        }
    }

    interface AnswerUi extends Ui {
        public void onShowAnswerUi(boolean shown);
        public void showTargets(int targetSet);
        public void showTargets(int targetSet, int videoState);
        public void showMessageDialog();
        public void configureMessageDialog(List<String> textResponses);
        public Context getContext();

        /// M: when another incoming call coming, need dismiss dialog @{
        public void dismissPendingDialogs();
        /// @}
    }

    /// M: Add for recording. @{
    @Override
    public void onStorageFull() {
        // no-op
    }

    @Override
    public void onUpdateRecordState(int state, int customValue) {
        // no-op
    }
    /// @}

    /**
     * M: we use it to new answerfragment.
     */
    private void showAnswerUi() {
        final InCallActivity activity = InCallPresenter.getInstance().getActivity();
        if (activity != null) {
            activity.showAnswerFragment(true);
        } else {
            Log.e(this, "InCallActivity is null");
        }
    }

}
