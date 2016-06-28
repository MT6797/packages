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
 * limitations under the License.
 */

package com.android.incallui;

import com.android.contacts.common.CallUtil;
import com.android.contacts.common.testing.NeededForTesting;
import com.android.incallui.CallList.Listener;
/// M: for VOLTE @{
import com.mediatek.incallui.CallDetailChangeHandler;
// M: add for performance profile
import com.mediatek.incallui.InCallTrace;
import com.mediatek.incallui.videocall.VideoFeatures;
import com.mediatek.incallui.volte.ConferenceChildrenChangeHandler;
import com.mediatek.incallui.volte.InCallUIVolteUtils;
/// @}

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.net.Uri;
import android.os.Bundle;
import android.os.Trace;
import android.telecom.DisconnectCause;
import android.telecom.GatewayInfo;
import android.telecom.InCallService.VideoCall;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Describes a single call and its state.
 */
@NeededForTesting
public class Call {
    /* Defines different states of this call */
    public static class State {
        public static final int INVALID = 0;
        public static final int NEW = 1;            /* The call is new. */
        public static final int IDLE = 2;           /* The call is idle.  Nothing active */
        public static final int ACTIVE = 3;         /* There is an active call */
        public static final int INCOMING = 4;       /* A normal incoming phone call */
        public static final int CALL_WAITING = 5;   /* Incoming call while another is active */
        public static final int DIALING = 6;        /* An outgoing call during dial phase */
        public static final int REDIALING = 7;      /* Subsequent dialing attempt after a failure */
        public static final int ONHOLD = 8;         /* An active phone call placed on hold */
        public static final int DISCONNECTING = 9;  /* A call is being ended. */
        public static final int DISCONNECTED = 10;  /* State after a call disconnects */
        public static final int CONFERENCED = 11;   /* Call part of a conference call */
        public static final int SELECT_PHONE_ACCOUNT = 12; /* Waiting for account selection */
        public static final int CONNECTING = 13;    /* Waiting for Telecomm broadcast to finish */
        /// M: [Modification for finishing Transparent InCall Screen if necessary]
        /// such as:ALPS02302461,occur JE when MT call arrive at some case. @{
        public static final int WAIT_ACCOUNT_RESPONSE = 100;
        /// @}

        public static boolean isConnectingOrConnected(int state) {
            switch(state) {
                case ACTIVE:
                case INCOMING:
                case CALL_WAITING:
                case CONNECTING:
                case DIALING:
                case REDIALING:
                case ONHOLD:
                case CONFERENCED:
                    return true;
                default:
            }
            return false;
        }

        public static boolean isDialing(int state) {
            return state == DIALING || state == REDIALING;
        }

        public static String toString(int state) {
            switch (state) {
                case INVALID:
                    return "INVALID";
                case NEW:
                    return "NEW";
                case IDLE:
                    return "IDLE";
                case ACTIVE:
                    return "ACTIVE";
                case INCOMING:
                    return "INCOMING";
                case CALL_WAITING:
                    return "CALL_WAITING";
                case DIALING:
                    return "DIALING";
                case REDIALING:
                    return "REDIALING";
                case ONHOLD:
                    return "ONHOLD";
                case DISCONNECTING:
                    return "DISCONNECTING";
                case DISCONNECTED:
                    return "DISCONNECTED";
                case CONFERENCED:
                    return "CONFERENCED";
                case SELECT_PHONE_ACCOUNT:
                    return "SELECT_PHONE_ACCOUNT";
                case CONNECTING:
                    return "CONNECTING";
                default:
                    return "UNKNOWN";
            }
        }

        /// M: add for judge incoming call sate. @{
        public static boolean isIncoming(int state) {
            return state == INCOMING || state == CALL_WAITING;
        }
        /// @}
    }

    /**
     * Defines different states of session modify requests, which are used to upgrade to video, or
     * downgrade to audio.
     */
    public static class SessionModificationState {
        ///M: add some state @{
        public static final int NO_REQUEST = 0;
        public static final int WAITING_FOR_UPGRADE_RESPONSE = 1;
        public static final int REQUEST_FAILED = 2;
        public static final int RECEIVED_UPGRADE_TO_VIDEO_REQUEST = 3;
        public static final int UPGRADE_TO_VIDEO_REQUEST_TIMED_OUT = 4;
        public static final int REQUEST_REJECTED = 5;
        public static final int WAITING_FOR_DOWNGRADE_RESPONSE = 6;
        public static final int WAITING_FOR_PAUSE_VIDEO_RESPONSE = 7;
        public static final int RECEIVED_UPGRADE_TO_VIDEO_REQUEST_ONE_WAY = 8;
        /// @}

    }

    public static class VideoSettings {
        public static final int CAMERA_DIRECTION_UNKNOWN = -1;
        public static final int CAMERA_DIRECTION_FRONT_FACING =
                CameraCharacteristics.LENS_FACING_FRONT;
        public static final int CAMERA_DIRECTION_BACK_FACING =
                CameraCharacteristics.LENS_FACING_BACK;

        private int mCameraDirection = CAMERA_DIRECTION_UNKNOWN;

        /**
         * Sets the camera direction. if camera direction is set to CAMERA_DIRECTION_UNKNOWN,
         * the video state of the call should be used to infer the camera direction.
         *
         * @see {@link CameraCharacteristics#LENS_FACING_FRONT}
         * @see {@link CameraCharacteristics#LENS_FACING_BACK}
         */
        public void setCameraDir(int cameraDirection) {
            if (cameraDirection == CAMERA_DIRECTION_FRONT_FACING
               || cameraDirection == CAMERA_DIRECTION_BACK_FACING) {
                mCameraDirection = cameraDirection;
            } else {
                mCameraDirection = CAMERA_DIRECTION_UNKNOWN;
            }
        }

        /**
         * Gets the camera direction. if camera direction is set to CAMERA_DIRECTION_UNKNOWN,
         * the video state of the call should be used to infer the camera direction.
         *
         * @see {@link CameraCharacteristics#LENS_FACING_FRONT}
         * @see {@link CameraCharacteristics#LENS_FACING_BACK}
         */
        public int getCameraDir() {
            return mCameraDirection;
        }

        public String toString() {
            return "(CameraDir:" + getCameraDir() + ")";
        }
    }


    private static final String ID_PREFIX = Call.class.getSimpleName() + "_";
    private static int sIdCounter = 0;

    private android.telecom.Call.Callback mTelecomCallCallback =
            new android.telecom.Call.Callback() {
                @Override
                public void onStateChanged(android.telecom.Call call, int newState) {
                    Log.d(this, "TelecommCallCallback onStateChanged call=" + call + " newState="
                            + newState);
                    InCallTrace.begin("telecomStateChanged");
                    update();
                    InCallTrace.end("telecomStateChanged");
                }

                @Override
                public void onParentChanged(android.telecom.Call call,
                        android.telecom.Call newParent) {
                    Log.d(this, "TelecommCallCallback onParentChanged call=" + call + " newParent="
                            + newParent);
                    update();
                }

                @Override
                public void onChildrenChanged(android.telecom.Call call,
                        List<android.telecom.Call> children) {
                    /// M: for VOLTE @{
                    handleChildrenChanged();
                    /// @}
                    update();
                }

                @Override
                public void onDetailsChanged(android.telecom.Call call,
                        android.telecom.Call.Details details) {
                    Log.d(this, "TelecommCallCallback onStateChanged call=" + call + " details="
                            + details);
                    InCallTrace.begin("telecomDetailsChanged");
                    update();
                    InCallTrace.end("telecomDetailsChanged");
                    /// M: for VOLTE @{
                    handleDetailsChanged(details);
                    /// @}
                }

                @Override
                public void onCannedTextResponsesLoaded(android.telecom.Call call,
                        List<String> cannedTextResponses) {
                    Log.d(this, "TelecommCallCallback onStateChanged call=" + call
                            + " cannedTextResponses=" + cannedTextResponses);
                    update();
                }

                @Override
                public void onPostDialWait(android.telecom.Call call,
                        String remainingPostDialSequence) {
                    Log.d(this, "TelecommCallCallback onStateChanged call=" + call
                            + " remainingPostDialSequence=" + remainingPostDialSequence);
                    update();
                }

                @Override
                public void onVideoCallChanged(android.telecom.Call call,
                        VideoCall videoCall) {
                    Log.d(this, "TelecommCallCallback onStateChanged call=" + call + " videoCall="
                            + videoCall);
                    update();
                }

                @Override
                public void onCallDestroyed(android.telecom.Call call) {
                    Log.d(this, "TelecommCallCallback onStateChanged call=" + call);
                    call.unregisterCallback(mTelecomCallCallback);
                }

                @Override
                public void onConferenceableCallsChanged(android.telecom.Call call,
                        List<android.telecom.Call> conferenceableCalls) {
                    update();
                }
            };

    private android.telecom.Call mTelecommCall;
    private final String mId;
    private int mState = State.INVALID;
    private DisconnectCause mDisconnectCause;
    private int mSessionModificationState;
    private final List<String> mChildCallIds = new ArrayList<>();
    private final VideoSettings mVideoSettings = new VideoSettings();
    /**
     * mModifyToVideoState is used to store requested upgrade / downgrade video state
     */
    private int mModifyToVideoState = VideoProfile.STATE_AUDIO_ONLY;

    private InCallVideoCallCallback mVideoCallCallback;

    /**
     * Used only to create mock calls for testing
     */
    @NeededForTesting
    Call(int state) {
        mTelecommCall = null;
        mId = ID_PREFIX + Integer.toString(sIdCounter++);
        setState(state);
    }

    public Call(android.telecom.Call telecommCall) {
        mTelecommCall = telecommCall;
        mId = ID_PREFIX + Integer.toString(sIdCounter++);
        updateFromTelecommCall();
        mTelecommCall.registerCallback(mTelecomCallCallback);
        /// M: for Volte @{
        // ALPS01792379. Init old details first.
        mOldDetails = mTelecommCall.getDetails();
        /// @}

        /// M: [voice call]manage video call features.
        mVideoFeatures = new VideoFeatures(this);
    }

    public android.telecom.Call getTelecommCall() {
        return mTelecommCall;
    }

    /**
     * @return video settings of the call, null if the call is not a video call.
     * @see VideoProfile
     */
    public VideoSettings getVideoSettings() {
        return mVideoSettings;
    }

    private void update() {
        Trace.beginSection("Update");
        int oldState = getState();
        updateFromTelecommCall();
        if (oldState != getState() && getState() == Call.State.DISCONNECTED) {
            /// M: [log optimize]
            Log.notify(this, Log.CcNotifyAction.DISCONNECTED,
                    mDisconnectCause == null ?
                            "DisconnectCause: null" : mDisconnectCause.toString());
            CallList.getInstance().onDisconnect(this);
        } else {
            CallList.getInstance().onUpdate(this);
        }
        Trace.endSection();
    }

    private void updateFromTelecommCall() {
        Log.d(this, "updateFromTelecommCall: " + mTelecommCall.toString());
        /// M: [log optimize]
        logCallStateChange(getState(), translateState(mTelecommCall.getState()));
        setState(translateState(mTelecommCall.getState()));
        setDisconnectCause(mTelecommCall.getDetails().getDisconnectCause());

        if (mTelecommCall.getVideoCall() != null) {
            if (mVideoCallCallback == null) {
                mVideoCallCallback = new InCallVideoCallCallback(this);
                /// M: [Video Call] It's not necessary to register the same CallBack so many times.
                mTelecommCall.getVideoCall().registerCallback(mVideoCallCallback);
            }
            /**
             * M: [Video Call] It's not necessary to register the same CallBack so many times.
            mTelecommCall.getVideoCall().registerCallback(mVideoCallCallback);
             */
        }

        mChildCallIds.clear();
        for (int i = 0; i < mTelecommCall.getChildren().size(); i++) {
            mChildCallIds.add(
                    CallList.getInstance().getCallByTelecommCall(
                            mTelecommCall.getChildren().get(i)).getId());
        }
    }

    private static int translateState(int state) {
        switch (state) {
            case android.telecom.Call.STATE_NEW:
            case android.telecom.Call.STATE_CONNECTING:
                return Call.State.CONNECTING;
            case android.telecom.Call.STATE_SELECT_PHONE_ACCOUNT:
                return Call.State.SELECT_PHONE_ACCOUNT;
            case android.telecom.Call.STATE_DIALING:
                return Call.State.DIALING;
            case android.telecom.Call.STATE_RINGING:
                return Call.State.INCOMING;
            case android.telecom.Call.STATE_ACTIVE:
                return Call.State.ACTIVE;
            case android.telecom.Call.STATE_HOLDING:
                return Call.State.ONHOLD;
            case android.telecom.Call.STATE_DISCONNECTED:
                return Call.State.DISCONNECTED;
            case android.telecom.Call.STATE_DISCONNECTING:
                return Call.State.DISCONNECTING;
            default:
                return Call.State.INVALID;
        }
    }

    public String getId() {
        return mId;
    }

    public String getNumber() {
        if (mTelecommCall == null) {
            return null;
        }
        if (mTelecommCall.getDetails().getGatewayInfo() != null) {
            return mTelecommCall.getDetails().getGatewayInfo()
                    .getOriginalAddress().getSchemeSpecificPart();
        }
        return getHandle() == null ? null : getHandle().getSchemeSpecificPart();
    }

    public Uri getHandle() {
        return mTelecommCall == null ? null : mTelecommCall.getDetails().getHandle();
    }

    public int getState() {
        if (mTelecommCall != null && mTelecommCall.getParent() != null) {
            return State.CONFERENCED;
        } else {
            return mState;
        }
    }

    public void setState(int state) {
        mState = state;
    }

    public int getNumberPresentation() {
        return mTelecommCall == null ? null : mTelecommCall.getDetails().getHandlePresentation();
    }

    public int getCnapNamePresentation() {
        return mTelecommCall == null ? null
                : mTelecommCall.getDetails().getCallerDisplayNamePresentation();
    }

    public String getCnapName() {
        return mTelecommCall == null ? null
                : getTelecommCall().getDetails().getCallerDisplayName();
    }

    public Bundle getIntentExtras() {
        return mTelecommCall == null ? null : mTelecommCall.getDetails().getIntentExtras();
    }

    public Bundle getExtras() {
        return mTelecommCall == null ? null : mTelecommCall.getDetails().getExtras();
    }

    /** Returns call disconnect cause, defined by {@link DisconnectCause}. */
    public DisconnectCause getDisconnectCause() {
        if (mState == State.DISCONNECTED || mState == State.IDLE) {
            return mDisconnectCause;
        }

        return new DisconnectCause(DisconnectCause.UNKNOWN);
    }

    public void setDisconnectCause(DisconnectCause disconnectCause) {
        mDisconnectCause = disconnectCause;
    }

    /** Returns the possible text message responses. */
    public List<String> getCannedSmsResponses() {
        return mTelecommCall.getCannedTextResponses();
    }

    /** Checks if the call supports the given set of capabilities supplied as a bit mask. */
    public boolean can(int capabilities) {
        int supportedCapabilities = mTelecommCall.getDetails().getCallCapabilities();

        if ((capabilities & android.telecom.Call.Details.CAPABILITY_MERGE_CONFERENCE) != 0) {
            // We allow you to merge if the capabilities allow it or if it is a call with
            // conferenceable calls.
            if (mTelecommCall.getConferenceableCalls().isEmpty() &&
                ((android.telecom.Call.Details.CAPABILITY_MERGE_CONFERENCE
                        & supportedCapabilities) == 0)) {
                // Cannot merge calls if there are no calls to merge with.
                return false;
            }
            capabilities &= ~android.telecom.Call.Details.CAPABILITY_MERGE_CONFERENCE;
        }
        return (capabilities == (capabilities & mTelecommCall.getDetails().getCallCapabilities()));
    }

    public boolean hasProperty(int property) {
        return mTelecommCall.getDetails().hasProperty(property);
    }

    /** Gets the time when the call first became active. */
    public long getConnectTimeMillis() {
        return mTelecommCall.getDetails().getConnectTimeMillis();
    }

    public boolean isConferenceCall() {
        return mTelecommCall.getDetails().hasProperty(
                android.telecom.Call.Details.PROPERTY_CONFERENCE);
    }

    public GatewayInfo getGatewayInfo() {
        return mTelecommCall == null ? null : mTelecommCall.getDetails().getGatewayInfo();
    }

    public PhoneAccountHandle getAccountHandle() {
        return mTelecommCall == null ? null : mTelecommCall.getDetails().getAccountHandle();
    }

    public VideoCall getVideoCall() {
        return mTelecommCall == null ? null : mTelecommCall.getVideoCall();
    }

    public List<String> getChildCallIds() {
        return mChildCallIds;
    }

    public String getParentId() {
        android.telecom.Call parentCall = mTelecommCall.getParent();
        if (parentCall != null) {
            return CallList.getInstance().getCallByTelecommCall(parentCall).getId();
        }
        return null;
    }

    public int getVideoState() {
        return mTelecommCall.getDetails().getVideoState();
    }

    public boolean isVideoCall(Context context) {
        return CallUtil.isVideoEnabled(context) &&
                CallUtils.isVideoCall(getVideoState());
    }

    /**
     * This method is called when we request for a video upgrade or downgrade. This handles the
     * session modification state RECEIVED_UPGRADE_TO_VIDEO_REQUEST and sets the video state we
     * want to upgrade/downgrade to.
     */
    public void setSessionModificationTo(int videoState) {
        Log.d(this, "setSessionModificationTo - video state= " + videoState);
        if (videoState == getVideoState()) {
            mSessionModificationState = Call.SessionModificationState.NO_REQUEST;
            Log.w(this,"setSessionModificationTo - Clearing session modification state");
        } else {
            mSessionModificationState =
                Call.SessionModificationState.RECEIVED_UPGRADE_TO_VIDEO_REQUEST;
            setModifyToVideoState(videoState);
            CallList.getInstance().onUpgradeToVideo(this);
        }

        Log.d(this, "setSessionModificationTo - mSessionModificationState="
            + mSessionModificationState + " video state= " + videoState);
        update();
    }

    /**
     * This method is called to handle any other session modification states other than
     * RECEIVED_UPGRADE_TO_VIDEO_REQUEST. We set the modification state and reset the video state
     * when an upgrade request has been completed or failed.
     */
    public void setSessionModificationState(int state) {
        if (state == Call.SessionModificationState.RECEIVED_UPGRADE_TO_VIDEO_REQUEST) {
            Log.e(this,
            "setSessionModificationState not to be called for RECEIVED_UPGRADE_TO_VIDEO_REQUEST");
            return;
        }

        boolean hasChanged = mSessionModificationState != state;
        mSessionModificationState = state;
        Log.d(this, "setSessionModificationState " + state + " mSessionModificationState="
                + mSessionModificationState);
        if (hasChanged) {
            CallList.getInstance().onSessionModificationStateChange(this, state);
        }

        /// add for ALPS02681041, record every time modify call video state.
        mModifyVideoStateFrom = getVideoState();
    }

    private void setModifyToVideoState(int newVideoState) {
        mModifyToVideoState = newVideoState;
    }

    public int getModifyToVideoState() {
        return mModifyToVideoState;
    }

    public static boolean areSame(Call call1, Call call2) {
        if (call1 == null && call2 == null) {
            return true;
        } else if (call1 == null || call2 == null) {
            return false;
        }

        // otherwise compare call Ids
        return call1.getId().equals(call2.getId());
    }

    public static boolean areSameNumber(Call call1, Call call2) {
        if (call1 == null && call2 == null) {
            return true;
        } else if (call1 == null || call2 == null) {
            return false;
        }

        // otherwise compare call Numbers
        return TextUtils.equals(call1.getNumber(), call2.getNumber());
    }

    public int getSessionModificationState() {
        return mSessionModificationState;
    }

    @Override
    public String toString() {
        if (mTelecommCall == null) {
            // This should happen only in testing since otherwise we would never have a null
            // Telecom call.
            return String.valueOf(mId);
        }

        return String.format(Locale.US, "[%s, %s, %s, children:%s, parent:%s, conferenceable:%s, " +
                "videoState:%s, mSessionModificationState:%d, VideoSettings:%s]",
                mId,
                State.toString(getState()),
                android.telecom.Call.Details
                        .capabilitiesToString(mTelecommCall.getDetails().getCallCapabilities()),
                mChildCallIds,
                getParentId(),
                this.mTelecommCall.getConferenceableCalls(),
                VideoProfile.videoStateToString(mTelecommCall.getDetails().getVideoState()),
                mSessionModificationState,
                getVideoSettings());
    }

    public String toSimpleString() {
        return super.toString();
    }

    //--------------------------------MediaTek-----------------------------------------------//
    /// M: For VoLTE @{
    private android.telecom.Call.Details mOldDetails;     // to record details before onDetailsChanged().
    /**
     * M: For management of video call features.
     */
    private VideoFeatures mVideoFeatures;
    public static final int INVALID_SUB_ID = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    /**
     * M: For management of video call features.
     * @return video features manager.
     */
    public VideoFeatures getVideoFeatures() {
        return mVideoFeatures;
    }

    /**
     * M: get details of the call. Wrapper for mTelecommCall.getDetails().
     * @return
     */
    public android.telecom.Call.Details getDetails() {
        if (mTelecommCall != null) {
            return mTelecommCall.getDetails();
        } else {
             Log.d(this, "getDetails()... mTelecommCall is null, need check! ");
            return null;
        }
    }

    /**
     * M: for VOLTE @{
     * This function used to check whether certain info has been changed, if changed, handle them.
     * @param newDetails
     */
    private void handleDetailsChanged(android.telecom.Call.Details newDetails) {
        CallDetailChangeHandler.getInstance().onCallDetailChanged(this, mOldDetails, newDetails);
        mOldDetails = newDetails;
    }
    /***@}**/

    /**
     * M: check whether the call is marked as Ecc by NW.
     * @return
     */
    public boolean isVolteMarkedEcc() {
        boolean isVolteEmerencyCall = false;
        isVolteEmerencyCall = InCallUIVolteUtils.isVolteMarkedEcc(getDetails());
        return isVolteEmerencyCall;
    }

    /**
     * M: get pau field received from NW.
     * @return
     */
    public String getVoltePauField() {
        String voltePauField = "";
        voltePauField = InCallUIVolteUtils.getVoltePauField(getDetails());
        return voltePauField;
    }

    /**
     * M: handle children change, notify member add or leave, only for VoLTE conference call.
     * Note: call this function before update() in onChildrenChanged(),
     * for mChildCallIds used here will be changed in update()
     */
    private void handleChildrenChanged() {
        Log.d(this, "handleChildrenChanged()...");
        if (!InCallUIVolteUtils.isVolteSupport() ||
                !hasProperty(android.telecom.Call.Details.PROPERTY_VOLTE)) {
            // below feature is only for VoLTE conference, so skip if not VoLTE conference.
            return;
        }
        List<String> newChildrenIds = new ArrayList<String>();
        for (int i = 0; i < mTelecommCall.getChildren().size(); i++) {
            newChildrenIds.add(
                    CallList.getInstance().getCallByTelecommCall(
                            mTelecommCall.getChildren().get(i)).getId());
        }
        ConferenceChildrenChangeHandler.getInstance().handleChildrenChanged(mChildCallIds, newChildrenIds);
    }

    /**
     * M: This function translates call state to status string for conference
     * caller.
     * @param context The Context object for the call.
     * @return call status to show
     */
    public String getCallStatusFromState(Context context) {
        Log.d(this, "getCallStatusFromState() mState: " + mState);
        String callStatus = "";
        switch (mState) {
            case State.ACTIVE:
                callStatus = context.getString(R.string.call_status_online);
                break;
            case State.ONHOLD:
                callStatus = context.getString(R.string.call_status_onhold);
                break;
            case State.DIALING:
            case State.REDIALING:
                callStatus = context.getString(R.string.call_status_dialing);
                break;
            case State.DISCONNECTING:
                callStatus = context.getString(R.string.call_status_disconnecting);
                break;
            case State.DISCONNECTED:
                callStatus = context.getString(R.string.call_status_disconnected);
                break;
            default:
                Log.w(this, "getCallStatusFromState() un-expected state: " + mState);
                break;
        }
        return callStatus;
    }

    /**
     * M: To judge whether the current Call is telephony or Volte Call.
     * @return true if telephony call.
     */
    public boolean isTelephonyCall() {
        Context context = InCallPresenter.getInstance().getContext();
        if (context == null) {
            return false;
        }

        TelecomManager telecomManager = (TelecomManager) context.
                getSystemService(Context.TELECOM_SERVICE);
        PhoneAccount phoneAccount = telecomManager.getPhoneAccount(getAccountHandle());
        if (phoneAccount == null) {
            return false;
        }
        return (phoneAccount.getCapabilities() & PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)
                == PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION;
    }
    /// @}

    /**
     * M: [log optimize], log the call state change from telecom.
     * @param oldState the old state for logging.
     * @param newState the new state for logging.
     */
    private void logCallStateChange(int oldState, int newState) {
        if (oldState == newState) {
            return;
        }
        // CONFERENCED is a useless state
        if (oldState == State.CONFERENCED) {
            return;
        }
        String action;
        switch (newState) {
            case State.INCOMING:
                action = Log.CcNotifyAction.INCOMING;
                break;
            case State.DIALING:
                action = Log.CcNotifyAction.DIALING;
                break;
            case State.CONNECTING:
                action = Log.CcNotifyAction.CONNECTING;
                break;
            case State.ACTIVE:
                action = Log.CcNotifyAction.ACTIVE;
                break;
            case State.ONHOLD:
                action = Log.CcNotifyAction.ONHOLD;
                break;
            case State.DISCONNECTING:
                action = Log.CcNotifyAction.DISCONNECTING;
                break;
            case State.NEW:
                action = Log.CcNotifyAction.NEW;
                break;
            default:
                // don't log other states
                return;
        }
        Log.notify(this, action, "state changed "
                + State.toString(oldState) + " -> " + State.toString(newState));
    }

    /**
     * M: Tell whether current call is held by the remote side.
     * If a call is held, it's State would still be ACTIVE.
     * we need the PROPERTY_HELD to know it was held.
     * @return true if held.
     */
    public boolean isHeld() {
        //untill now this feature is not ready, so only return false.
        //return hasProperty(android.telecom.Call.Details.PROPERTY_HELD);
        return false;
    }


    /// M: add isHidePreview to record user click hidepreview  button
    // when device rotate, we should accord to this state to restore. @{
    private boolean isHidePreview = false;

    public boolean isHidePreview() {
        return isHidePreview;
    }

    public void setHidePreview(boolean isHidePreview) {
        this.isHidePreview = isHidePreview;
    }
    ///@}

    /// M: for ALPS02681041. record every time modify call video state. @{
    private int mModifyVideoStateFrom = VideoProfile.STATE_AUDIO_ONLY;

    public int getModifyVideoStateFrom() {
        return mModifyVideoStateFrom;
    }
    /// @}
}
