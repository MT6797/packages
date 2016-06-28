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

package com.android.incallui;

import android.telecom.Connection;
import android.telecom.Connection.VideoProvider;
import android.telecom.InCallService.VideoCall;
import android.telecom.VideoProfile;
import android.telecom.VideoProfile.CameraCapabilities;

import com.mediatek.incallui.InCallUtils;
import com.mediatek.incallui.ext.ExtensionManager;

/**
 * Implements the InCallUI VideoCall Callback.
 */
public class InCallVideoCallCallback extends VideoCall.Callback {

    /**
     * The call associated with this {@link InCallVideoCallCallback}.
     */
    private Call mCall;

    /**
     * Creates an instance of the call video client, specifying the call it is related to.
     *
     * @param call The call.
     */
    public InCallVideoCallCallback(Call call) {
        mCall = call;
    }

    /**
     * Handles an incoming session modification request.
     *
     * @param videoProfile The requested video call profile.
     */
    @Override
    public void onSessionModifyRequestReceived(VideoProfile videoProfile) {
        Log.d(this, " onSessionModifyRequestReceived videoProfile=" + videoProfile);
        int previousVideoState = CallUtils.getUnPausedVideoState(mCall.getVideoState());
        int newVideoState = CallUtils.getUnPausedVideoState(videoProfile.getVideoState());

        boolean wasVideoCall = CallUtils.isVideoCall(previousVideoState);
        boolean isVideoCall = CallUtils.isVideoCall(newVideoState);

        /// M: @{
        Log.d(this, "[previousVideoState]-->" + previousVideoState);
        Log.d(this, "[newVideoState]-->" + newVideoState);
        Log.d(this, "[wasVideoCall]-->" + wasVideoCall);
        Log.d(this, "[isVideoCall]-->" + isVideoCall);
        ///@}
        /// M: fix CR:ALPS02693003,handle concurrence local and remote request fail case. @{
        if (mCall.getSessionModificationState() != Call.SessionModificationState.NO_REQUEST) {
            Log.w(this, "onSessionModifyRequestReceived block remote request exist local request");
            mCall.getVideoCall().sendSessionModifyResponse(new VideoProfile(mCall.getVideoState()));
            return;
        }
        /// @}
        // Check for upgrades to video and downgrades to audio.
        if (wasVideoCall && !isVideoCall) {
            InCallVideoCallCallbackNotifier.getInstance().downgradeToAudio(mCall);
            // M: FIXME: What if VideoProfile.getQuality() not supported by us?
            mCall.getVideoCall().sendSessionModifyResponse(videoProfile);
            //M :remote side set state WAITING_FOR_DOWNGRADE_RESPONSE.
            //we will change to the state noRequest in CallButtonPresenter OnStateChange
            mCall.setSessionModificationState(Call.SessionModificationState.
                    WAITING_FOR_DOWNGRADE_RESPONSE);
        } else if (previousVideoState != newVideoState) {
            if (newVideoState == VideoProfile.STATE_TX_ENABLED) {
                ///M :when remote side receive pause video from local side
                //make the state WAITING_FOR_PAUSE_VIDEO_RESPONSE, it also will
                //set state no request in CallButtonPresenter OnStateChange
                mCall.getVideoCall().sendSessionModifyResponse(videoProfile);
                mCall.setSessionModificationState(Call.SessionModificationState.
                        WAITING_FOR_PAUSE_VIDEO_RESPONSE);

            } else if (newVideoState == VideoProfile.STATE_BIDIRECTIONAL
                    && previousVideoState == VideoProfile.STATE_TX_ENABLED) {
                // M: FIXED ME when remote side restart video, we set the state as
                // RECEIVED_UPGRADE_TO_VIDEO_REQUEST_ONE_WAY, will also
                // update in CallButtonPresenter OnStateChange@{
                mCall.getVideoCall().sendSessionModifyResponse(videoProfile);
                //M :remote side set state
                mCall.setSessionModificationState(Call.SessionModificationState.
                        RECEIVED_UPGRADE_TO_VIDEO_REQUEST_ONE_WAY);
                /// @}
            } else {
                InCallVideoCallCallbackNotifier.getInstance().upgradeToVideoRequest(mCall,
                        newVideoState);
            }
        }
    }

    /**
     * Handles a session modification response.
     *
     * @param status Status of the session modify request. Valid values are
     *            {@link Connection.VideoProvider#SESSION_MODIFY_REQUEST_SUCCESS},
     *            {@link Connection.VideoProvider#SESSION_MODIFY_REQUEST_FAIL},
     *            {@link Connection.VideoProvider#SESSION_MODIFY_REQUEST_INVALID}
     * @param requestedProfile
     * @param responseProfile The actual profile changes made by the peer device.
     */
    @Override
    public void onSessionModifyResponseReceived(int status, VideoProfile requestedProfile,
            VideoProfile responseProfile) {
        Log.d(this, "onSessionModifyResponseReceived status=" + status + " requestedProfile="
                + requestedProfile + " responseProfile=" + responseProfile);

        if (status != VideoProvider.SESSION_MODIFY_REQUEST_SUCCESS) {
            /// M: fix CR:ALPS02536832,handle downgrade to voice call fail case. @{
            if (!CallUtils.isVideoCall(requestedProfile.getVideoState()) &&
                    CallUtils.isVideoCall(responseProfile.getVideoState())){
                mCall.setSessionModificationState(
                        Call.SessionModificationState.NO_REQUEST);
                //show message when downgrade to voice failed
                /// M: fix CR:ALPS02707358,shouldn't show "failed to switch video call" toast
                /// when call is disconnecting or call has been disconnected. @{
                if(Call.State.isConnectingOrConnected(mCall.getState())) {
                    InCallPresenter.getInstance().showMessage(
                            com.android.incallui.R.string.video_call_downgrade_to_voice_call_failed);
                }
                /// @}
                Log.e(this, "onSessionModifyResponseReceived downgrade to audio call fail");
                return;
            }
            /// @}
            // Report the reason the upgrade failed as the new session modification state.
            if (status == VideoProvider.SESSION_MODIFY_REQUEST_TIMED_OUT) {
                mCall.setSessionModificationState(
                        Call.SessionModificationState.UPGRADE_TO_VIDEO_REQUEST_TIMED_OUT);
            } else {
                if (status == VideoProvider.SESSION_MODIFY_REQUEST_REJECTED_BY_REMOTE) {
                    mCall.setSessionModificationState(
                            Call.SessionModificationState.REQUEST_REJECTED);
                } else {
                    mCall.setSessionModificationState(
                            Call.SessionModificationState.REQUEST_FAILED);
                }
            }
            InCallVideoCallCallbackNotifier.getInstance().upgradeToVideoFail(status, mCall);
        } else if (requestedProfile != null && responseProfile != null) {
            boolean modifySucceeded = requestedProfile.getVideoState() ==
                    responseProfile.getVideoState();
            boolean isVideoCall = CallUtils.isVideoCall(responseProfile.getVideoState());
            if (modifySucceeded && isVideoCall) {
                InCallVideoCallCallbackNotifier.getInstance().upgradeToVideoSuccess(mCall);
            } else if (!modifySucceeded && isVideoCall) {
                InCallVideoCallCallbackNotifier.getInstance().upgradeToVideoFail(status, mCall);
            } else if (modifySucceeded && !isVideoCall) {
                InCallVideoCallCallbackNotifier.getInstance().downgradeToAudio(mCall);
            }
        } else {
            Log.d(this, "onSessionModifyResponseReceived request and response Profiles are null");
        }
        // Finally clear the outstanding request.
        mCall.setSessionModificationState(Call.SessionModificationState.NO_REQUEST);
    }

    /**
     * Handles a call session event.
     *
     * @param event The event.
     */
    @Override
    public void onCallSessionEvent(int event) {
        Log.d(this, "[onCallSessionEvent]event = " + event);
        InCallVideoCallCallbackNotifier.getInstance().callSessionEvent(event);
        /// M: for plugin extension: OP07/OP08git
        ExtensionManager.getVideoCallExt().onCallSessionEvent(mCall.getTelecommCall(), event);
    }

    /**
     * Handles a change to the peer video dimensions.
     *
     * @param width  The updated peer video width.
     * @param height The updated peer video height.
     */
    @Override
    public void onPeerDimensionsChanged(int width, int height) {
        Log.d(this, "[onPeerDimensionsChanged] size: " + InCallUtils.formatSize(width, height));
        InCallVideoCallCallbackNotifier.getInstance().peerDimensionsChanged(mCall, width, height);
    }

    /* M: ViLTE part start */
    /**
     * Handles a change to the peer video dimensions.
     *
     * Different from AOSP, additional parameter "rotation" is added.
     *
     * @param width  The updated peer video width.
     * @param height The updated peer video height.
     * @param rotation The updated peer video rotation.
     * @hide
     */
    @Override
    public void onPeerDimensionsWithAngleChanged(int width, int height, int rotation) {
        InCallVideoCallCallbackNotifier.getInstance().
                onPeerDimensionsWithAngleChanged(mCall, width, height, rotation);
    }
    /* M: ViLTE part end */

    /**
     * Handles a change to the video quality of the call.
     *
     * @param videoQuality The updated video call quality.
     */
    @Override
    public void onVideoQualityChanged(int videoQuality) {
        Log.d(this, "[onVideoQualityChanged]new quality: " + videoQuality);
        InCallVideoCallCallbackNotifier.getInstance().videoQualityChanged(mCall, videoQuality);
    }

    /**
     * Handles a change to the call data usage.  No implementation as the in-call UI does not
     * display data usage.
     *
     * @param dataUsage The updated data usage.
     */
    @Override
    public void onCallDataUsageChanged(long dataUsage) {
        Log.d(this, "onCallDataUsageChanged: dataUsage = " + dataUsage);
        InCallVideoCallCallbackNotifier.getInstance().callDataUsageChanged(dataUsage);
    }

    /**
     * Handles changes to the camera capabilities.  No implementation as the in-call UI does not
     * make use of camera capabilities.
     *
     * @param cameraCapabilities The changed camera capabilities.
     */
    @Override
    public void onCameraCapabilitiesChanged(CameraCapabilities cameraCapabilities) {
        if (cameraCapabilities != null) {
            Log.d(this, "[onCameraCapabilitiesChanged]new size: " + InCallUtils.formatSize(
                    cameraCapabilities.getWidth(), cameraCapabilities.getHeight()));
            InCallVideoCallCallbackNotifier.getInstance().cameraDimensionsChanged(
                    mCall, cameraCapabilities.getWidth(), cameraCapabilities.getHeight());
        }
    }
}
