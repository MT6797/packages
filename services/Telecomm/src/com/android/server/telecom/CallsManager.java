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

package com.android.server.telecom;

import android.content.Context;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.PowerManager;
import android.os.Trace;
import android.os.Message;
import android.os.SystemClock;
import android.provider.CallLog.Calls;
import android.telecom.CallAudioState;
import android.telecom.Conference;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.GatewayInfo;
import android.telecom.ParcelableConference;
import android.telecom.ParcelableConnection;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.VideoProfile;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.telecom.ConnectionServiceRepository;
import com.android.server.telecom.components.ErrorDialogActivity;

import java.util.Collection;

import com.mediatek.telecom.TelecomManagerEx;
import com.mediatek.telecom.TelecomUtils;
import com.mediatek.telecom.TelecomUtils.FeatureType;
///M: add for OP09 plug-in
import com.mediatek.telecom.ext.ExtensionManager;
import com.mediatek.telecom.recording.PhoneRecorderHandler;
import com.mediatek.telecom.volte.TelecomVolteUtils;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import android.widget.Toast;

import android.os.UserHandle;
import android.content.Intent;
/**
 * Singleton.
 *
 * NOTE: by design most APIs are package private, use the relevant adapter/s to allow
 * access from other packages specifically refraining from passing the CallsManager instance
 * beyond the com.android.server.telecom package boundary.
 */
@VisibleForTesting
public class CallsManager extends Call.ListenerBase implements VideoProviderProxy.Listener {

    // TODO: Consider renaming this CallsManagerPlugin.
    interface CallsManagerListener {
        void onCallAdded(Call call);
        void onCallRemoved(Call call);
        void onCallStateChanged(Call call, int oldState, int newState);
        void onConnectionServiceChanged(
                Call call,
                ConnectionServiceWrapper oldService,
                ConnectionServiceWrapper newService);
        void onIncomingCallAnswered(Call call);
        void onIncomingCallRejected(Call call, boolean rejectWithMessage, String textMessage);
        void onForegroundCallChanged(Call oldForegroundCall, Call newForegroundCall);
        void onCallAudioStateChanged(CallAudioState oldAudioState, CallAudioState newAudioState);
        void onRingbackRequested(Call call, boolean ringback);
        void onIsConferencedChanged(Call call);
        void onIsVoipAudioModeChanged(Call call);
        void onVideoStateChanged(Call call);
        void onCanAddCallChanged(boolean canAddCall);
        void onSessionModifyRequestReceived(Call call, VideoProfile videoProfile);
        /* M: CC part start */
        void onConnectionLost(Call call);
        void onCdmaCallAccepted(Call call);
        /* M: CC part end */
        ///M: Add for 3G VT only.
        void onVtStatusInfoChanged(Call call, int status);
    }

    private static final String TAG = "CallsManager";

    private static final int MAXIMUM_LIVE_CALLS = 1;
    private static final int MAXIMUM_HOLD_CALLS = 1;
    private static final int MAXIMUM_RINGING_CALLS = 1;
    private static final int MAXIMUM_DIALING_CALLS = 1;
    private static final int MAXIMUM_OUTGOING_CALLS = 1;
    private static final int MAXIMUM_TOP_LEVEL_CALLS = 2;

    private static final int[] OUTGOING_CALL_STATES =
            {CallState.CONNECTING, CallState.SELECT_PHONE_ACCOUNT, CallState.DIALING};

    private static final int[] LIVE_CALL_STATES =
            {CallState.CONNECTING, CallState.SELECT_PHONE_ACCOUNT, CallState.DIALING, CallState.ACTIVE};

    /**
     * The main call repository. Keeps an instance of all live calls. New incoming and outgoing
     * calls are added to the map and removed when the calls move to the disconnected state.
     *
     * ConcurrentHashMap constructor params: 8 is initial table size, 0.9f is
     * load factor before resizing, 1 means we only expect a single thread to
     * access the map so make only a single shard
     */
    private final Set<Call> mCalls = Collections.newSetFromMap(
            new ConcurrentHashMap<Call, Boolean>(8, 0.9f, 1));

    private final ConnectionServiceRepository mConnectionServiceRepository;
    private final DtmfLocalTonePlayer mDtmfLocalTonePlayer;
    private final InCallController mInCallController;
    private final CallAudioManager mCallAudioManager;
    private RespondViaSmsManager mRespondViaSmsManager;
    private final Ringer mRinger;
    private final InCallWakeLockController mInCallWakeLockController;
    // For this set initial table size to 16 because we add 13 listeners in
    // the CallsManager constructor.
    private final Set<CallsManagerListener> mListeners = Collections.newSetFromMap(
            new ConcurrentHashMap<CallsManagerListener, Boolean>(16, 0.9f, 1));
    private final HeadsetMediaButton mHeadsetMediaButton;
    private final WiredHeadsetManager mWiredHeadsetManager;
    private final DockManager mDockManager;
    private final TtyManager mTtyManager;
    private final ProximitySensorManager mProximitySensorManager;
    private final PhoneStateBroadcaster mPhoneStateBroadcaster;
    private final CallLogManager mCallLogManager;
    private final Context mContext;
    private final TelecomSystem.SyncRoot mLock;
    private final ContactsAsyncHelper mContactsAsyncHelper;
    private final CallerInfoAsyncQueryFactory mCallerInfoAsyncQueryFactory;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;
    private final MissedCallNotifier mMissedCallNotifier;
    private final Set<Call> mLocallyDisconnectingCalls = new HashSet<>();
    private final Set<Call> mPendingCallsToDisconnect = new HashSet<>();
    /* Handler tied to thread in which CallManager was initialized. */
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private boolean mCanAddCall = true;
    /// M: Added for update screen wake state.@{
    private TelecomUtils mTelecomUtils;
    /// @}

    /**
     * The call the user is currently interacting with. This is the call that should have audio
     * focus and be visible in the in-call UI.
     */
    private Call mForegroundCall;

    private Runnable mStopTone;

    /** Add for ECC */
    private Call mPendingECCCall;
    private boolean mHasPendingECC;
    /* added for CDMA India optr*/
    private static final String ACTION_ESN_MO_CALL =
                     "com.android.server.telecom.ESN_OUTGOING_CALL_PLACED";

    /// M: Add to keep toast information. @{
    String mToastInformation;
    /// @}

    /// M: MSMA call control @{
    private final Map<Call, PendingCallAction> mPendingCallActions = new HashMap<>();
    /// @}

    /**
     * Initializes the required Telecom components.
     */
    CallsManager(
            Context context,
            TelecomSystem.SyncRoot lock,
            ContactsAsyncHelper contactsAsyncHelper,
            CallerInfoAsyncQueryFactory callerInfoAsyncQueryFactory,
            MissedCallNotifier missedCallNotifier,
            PhoneAccountRegistrar phoneAccountRegistrar,
            HeadsetMediaButtonFactory headsetMediaButtonFactory,
            ProximitySensorManagerFactory proximitySensorManagerFactory,
            InCallWakeLockControllerFactory inCallWakeLockControllerFactory) {
        mContext = context;
        mLock = lock;
        mContactsAsyncHelper = contactsAsyncHelper;
        mCallerInfoAsyncQueryFactory = callerInfoAsyncQueryFactory;
        mPhoneAccountRegistrar = phoneAccountRegistrar;
        mMissedCallNotifier = missedCallNotifier;
        StatusBarNotifier statusBarNotifier = new StatusBarNotifier(context, this);
        mWiredHeadsetManager = new WiredHeadsetManager(context);
        mDockManager = new DockManager(context);
        mCallAudioManager = new CallAudioManager(
                context, mLock, statusBarNotifier, mWiredHeadsetManager, mDockManager, this);
        InCallTonePlayer.Factory playerFactory = new InCallTonePlayer.Factory(mCallAudioManager, lock);
        mRinger = new Ringer(mCallAudioManager, this, playerFactory, context);
        mHeadsetMediaButton = headsetMediaButtonFactory.create(context, this, mLock);
        mTtyManager = new TtyManager(context, mWiredHeadsetManager);
        mProximitySensorManager = proximitySensorManagerFactory.create(context, this);
        mPhoneStateBroadcaster = new PhoneStateBroadcaster(this);
        mCallLogManager = new CallLogManager(context);
        mInCallController = new InCallController(context, mLock, this);
        mDtmfLocalTonePlayer = new DtmfLocalTonePlayer(context);
        mConnectionServiceRepository =
                new ConnectionServiceRepository(mPhoneAccountRegistrar, mContext, mLock, this);
        mInCallWakeLockController = inCallWakeLockControllerFactory.create(context, this);

        mListeners.add(statusBarNotifier);
        mListeners.add(mCallLogManager);
        mListeners.add(mPhoneStateBroadcaster);
        mListeners.add(mInCallController);
        mListeners.add(mRinger);
        mListeners.add(new RingbackPlayer(this, playerFactory));
        mListeners.add(new InCallToneMonitor(playerFactory, this));
        mListeners.add(mCallAudioManager);
        mListeners.add(missedCallNotifier);
        mListeners.add(mDtmfLocalTonePlayer);
        mListeners.add(mHeadsetMediaButton);
        mListeners.add(mProximitySensorManager);

        mMissedCallNotifier.updateOnStartup(
                mLock, this, mContactsAsyncHelper, mCallerInfoAsyncQueryFactory);
    }

    public void setRespondViaSmsManager(RespondViaSmsManager respondViaSmsManager) {
        if (mRespondViaSmsManager != null) {
            mListeners.remove(mRespondViaSmsManager);
        }
        mRespondViaSmsManager = respondViaSmsManager;
        mListeners.add(respondViaSmsManager);
    }

    public RespondViaSmsManager getRespondViaSmsManager() {
        return mRespondViaSmsManager;
    }

    @Override
    public void onSuccessfulOutgoingCall(Call call, int callState) {
        Log.v(this, "onSuccessfulOutgoingCall, %s", call);

        setCallState(call, callState, "successful outgoing call");
        if (!mCalls.contains(call)) {
            // Call was not added previously in startOutgoingCall due to it being a potential MMI
            // code, so add it now.
            addCall(call);
        }

        // The call's ConnectionService has been updated.
        for (CallsManagerListener listener : mListeners) {
            listener.onConnectionServiceChanged(call, null, call.getConnectionService());
        }

        //ALPS01781841, do not mark Ecc call as dialing state this time point
        //Ecc call is marked as dialing state only when FWK call state event(MSG_SET_DIALING) post to ConnectionServiceWrapper.Adapter
        if (!call.isEmergencyCall()) {
            markCallAsDialing(call);
        }
    }

    @Override
    public void onFailedOutgoingCall(Call call, DisconnectCause disconnectCause) {
        Log.v(this, "onFailedOutgoingCall, call: %s", call);

        markCallAsRemoved(call);
    }

    @Override
    public void onSuccessfulIncomingCall(Call incomingCall) {
        Log.d(this, "onSuccessfulIncomingCall");
        setCallState(incomingCall, CallState.RINGING, "successful incoming call");

        /// M: [ALPS01833793]when ECC in progress, should reject incoming call, like sip call.
        // Add for 3G VT: video call and voice call could not exist at the same time.
        // Add prevent dialing + ringing co-exist: Dialing + Ringing => two active if both answered.
        if (hasEmergencyCall() || hasMaximumRingingCalls() || hasMaximumDialingCalls()
                || shouldBlockFor3GVT(incomingCall.isVideoCall())) {
        /// @}
            incomingCall.reject(false, null);
            // since the call was not added to the list of calls, we have to call the missed
            // call notifier and the call logger manually.
            mMissedCallNotifier.showMissedCallNotification(incomingCall);
            mCallLogManager.logCall(incomingCall, Calls.MISSED_TYPE);
        } else {
            addCall(incomingCall);
        }
    }

    @Override
    public void onFailedIncomingCall(Call call) {
        setCallState(call, CallState.DISCONNECTED, "failed incoming call");
        call.removeListener(this);
    }

    @Override
    public void onSuccessfulUnknownCall(Call call, int callState) {
        setCallState(call, callState, "successful unknown call");
        Log.i(this, "onSuccessfulUnknownCall for call %s", call);
        addCall(call);
    }

    @Override
    public void onFailedUnknownCall(Call call) {
        Log.i(this, "onFailedUnknownCall for call %s", call);
        setCallState(call, CallState.DISCONNECTED, "failed unknown call");
        call.removeListener(this);
    }

    @Override
    public void onRingbackRequested(Call call, boolean ringback) {
        for (CallsManagerListener listener : mListeners) {
            listener.onRingbackRequested(call, ringback);
        }
    }

    @Override
    public void onPostDialWait(Call call, String remaining) {
        mInCallController.onPostDialWait(call, remaining);
    }

    @Override
    public void onPostDialChar(final Call call, char nextChar) {
        if (PhoneNumberUtils.is12Key(nextChar)) {
            // Play tone if it is one of the dialpad digits, canceling out the previously queued
            // up stopTone runnable since playing a new tone automatically stops the previous tone.
            if (mStopTone != null) {
                mHandler.removeCallbacks(mStopTone);
            }

            mDtmfLocalTonePlayer.playTone(call, nextChar);

            // TODO: Create a LockedRunnable class that does the synchronization automatically.
            mStopTone = new Runnable() {
                @Override
                public void run() {
                    synchronized (mLock) {
                        // Set a timeout to stop the tone in case there isn't another tone to follow.
                        mDtmfLocalTonePlayer.stopTone(call);
                    }
                }
            };
            mHandler.postDelayed(
                    mStopTone,
                    Timeouts.getDelayBetweenDtmfTonesMillis(mContext.getContentResolver()));
        } else if (nextChar == 0 || nextChar == TelecomManager.DTMF_CHARACTER_WAIT ||
                nextChar == TelecomManager.DTMF_CHARACTER_PAUSE) {
            // Stop the tone if a tone is playing, removing any other stopTone callbacks since
            // the previous tone is being stopped anyway.
            if (mStopTone != null) {
                mHandler.removeCallbacks(mStopTone);
            }
            mDtmfLocalTonePlayer.stopTone(call);
        } else {
            Log.w(this, "onPostDialChar: invalid value %d", nextChar);
        }
    }

    @Override
    public void onParentChanged(Call call) {
        // parent-child relationship affects which call should be foreground, so do an update.
        updateCallsManagerState();
        for (CallsManagerListener listener : mListeners) {
            listener.onIsConferencedChanged(call);
        }
    }

    @Override
    public void onChildrenChanged(Call call) {
        // parent-child relationship affects which call should be foreground, so do an update.
        updateCallsManagerState();
        /// M: for ALPS01771880 @{
        // stop record if conf-call members changed
        if (mForegroundCall == call) {
            PhoneRecorderHandler.getInstance().stopRecording();
        }
        ///@}
        for (CallsManagerListener listener : mListeners) {
            listener.onIsConferencedChanged(call);
        }
    }

    @Override
    public void onIsVoipAudioModeChanged(Call call) {
        for (CallsManagerListener listener : mListeners) {
            listener.onIsVoipAudioModeChanged(call);
        }
    }

    @Override
    public void onVideoStateChanged(Call call) {
        for (CallsManagerListener listener : mListeners) {
            listener.onVideoStateChanged(call);
        }
    }

    @Override
    public boolean onCanceledViaNewOutgoingCallBroadcast(final Call call) {
        mPendingCallsToDisconnect.add(call);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                synchronized (mLock) {
                    if (mPendingCallsToDisconnect.remove(call)) {
                        Log.i(this, "Delayed disconnection of call: %s", call);
                        call.disconnect();
                    }
                }
            }
        }, Timeouts.getNewOutgoingCallCancelMillis(mContext.getContentResolver()));

        return true;
    }

    /**
     * Handles changes to the {@link Connection.VideoProvider} for a call.  Adds the
     * {@link CallsManager} as a listener for the {@link VideoProviderProxy} which is created
     * in {@link Call#setVideoProvider(IVideoProvider)}.  This allows the {@link CallsManager} to
     * respond to callbacks from the {@link VideoProviderProxy}.
     *
     * @param call The call.
     */
    @Override
    public void onVideoCallProviderChanged(Call call) {
        VideoProviderProxy videoProviderProxy = call.getVideoProviderProxy();

        if (videoProviderProxy == null) {
            return;
        }

        videoProviderProxy.addListener(this);
    }

    /**
     * Handles session modification requests received via the {@link TelecomVideoCallCallback} for
     * a call.  Notifies listeners of the {@link CallsManager.CallsManagerListener} of the session
     * modification request.
     *
     * @param call The call.
     * @param videoProfile The {@link VideoProfile}.
     */
    @Override
    public void onSessionModifyRequestReceived(Call call, VideoProfile videoProfile) {
        int videoState = videoProfile != null ? videoProfile.getVideoState() :
                VideoProfile.STATE_AUDIO_ONLY;
        Log.v(TAG, "onSessionModifyRequestReceived : videoProfile = " + VideoProfile
                .videoStateToString(videoState));

        for (CallsManagerListener listener : mListeners) {
            listener.onSessionModifyRequestReceived(call, videoProfile);
        }

    }

    Collection<Call> getCalls() {
        return Collections.unmodifiableCollection(mCalls);
    }

    Call getForegroundCall() {
        return mForegroundCall;
    }

    Ringer getRinger() {
        return mRinger;
    }

    InCallController getInCallController() {
        return mInCallController;
    }

    boolean hasEmergencyCall() {
        for (Call call : mCalls) {
            if (call.isEmergencyCall()) {
                return true;
            }
        }
        return false;
    }

    /**
     * use to check if current mCalls contains a emergency call,
     * need to exclude the current outgoing cemergency call.
     * @param onGoingcall
     * @return
     */
    private boolean hasOtherEmergencyCall(Call onGoingcall) {
        for (Call call : mCalls) {
            if (Objects.equals(onGoingcall, call)) {
                continue;
            }
            if (call.isEmergencyCall()) {
                return true;
            }
        }
        return false;
    }

    boolean hasVideoCall() {
        for (Call call : mCalls) {
            if (VideoProfile.isVideo(call.getVideoState())) {
                return true;
            }
        }
        return false;
    }

    CallAudioState getAudioState() {
        return mCallAudioManager.getCallAudioState();
    }

    boolean isTtySupported() {
        return mTtyManager.isTtySupported();
    }

    int getCurrentTtyMode() {
        return mTtyManager.getCurrentTtyMode();
    }

    void addListener(CallsManagerListener listener) {
        mListeners.add(listener);
    }

    void removeListener(CallsManagerListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Starts the process to attach the call to a connection service.
     *
     * @param phoneAccountHandle The phone account which contains the component name of the
     *        connection service to use for this call.
     * @param extras The optional extras Bundle passed with the intent used for the incoming call.
     */
    void processIncomingCallIntent(PhoneAccountHandle phoneAccountHandle, Bundle extras) {
        Log.d(this, "processIncomingCallIntent");
        Uri handle = extras.getParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS);
        if (handle == null) {
            // Required for backwards compatibility
            handle = extras.getParcelable(TelephonyManager.EXTRA_INCOMING_NUMBER);
        }
        Call call = new Call(
                mContext,
                this,
                mLock,
                mConnectionServiceRepository,
                mContactsAsyncHelper,
                mCallerInfoAsyncQueryFactory,
                handle,
                null /* gatewayInfo */,
                null /* connectionManagerPhoneAccount */,
                phoneAccountHandle,
                true /* isIncoming */,
                false /* isConference */);

        call.setIntentExtras(extras);
        /// M: For VoLTE @{
        if (TelecomVolteUtils.isConferenceInvite(extras)) {
            call.setIsIncomingFromConfServer(true);
        }
        /// @}

        // TODO: Move this to be a part of addCall()
        call.addListener(this);
        call.startCreateConnection(mPhoneAccountRegistrar);
    }

    void addNewUnknownCall(PhoneAccountHandle phoneAccountHandle, Bundle extras) {
        Uri handle = extras.getParcelable(TelecomManager.EXTRA_UNKNOWN_CALL_HANDLE);
        Log.i(this, "addNewUnknownCall with handle: %s", Log.pii(handle));
        Call call = new Call(
                mContext,
                this,
                mLock,
                mConnectionServiceRepository,
                mContactsAsyncHelper,
                mCallerInfoAsyncQueryFactory,
                handle,
                null /* gatewayInfo */,
                null /* connectionManagerPhoneAccount */,
                phoneAccountHandle,
                // Use onCreateIncomingConnection in TelephonyConnectionService, so that we attach
                // to the existing connection instead of trying to create a new one.
                true /* isIncoming */,
                false /* isConference */);
        call.setIsUnknown(true);
        call.setIntentExtras(extras);
        call.addListener(this);
        call.startCreateConnection(mPhoneAccountRegistrar);
    }

    private boolean areHandlesEqual(Uri handle1, Uri handle2) {
        if (handle1 == null || handle2 == null) {
            return handle1 == handle2;
        }

        if (!TextUtils.equals(handle1.getScheme(), handle2.getScheme())) {
            return false;
        }

        final String number1 = PhoneNumberUtils.normalizeNumber(handle1.getSchemeSpecificPart());
        final String number2 = PhoneNumberUtils.normalizeNumber(handle2.getSchemeSpecificPart());
        return TextUtils.equals(number1, number2);
    }

    private Call getNewOutgoingCall(Uri handle) {
        // First check to see if we can reuse any of the calls that are waiting to disconnect.
        // See {@link Call#abort} and {@link #onCanceledViaNewOutgoingCall} for more information.
        Call reusedCall = null;
        for (Call pendingCall : mPendingCallsToDisconnect) {
            if (reusedCall == null && areHandlesEqual(pendingCall.getHandle(), handle)) {
                mPendingCallsToDisconnect.remove(pendingCall);
                Log.i(this, "Reusing disconnected call %s", pendingCall);
                reusedCall = pendingCall;
            } else {
                Log.i(this, "Not reusing disconnected call %s", pendingCall);
                pendingCall.disconnect();
            }
        }
        if (reusedCall != null) {
            return reusedCall;
        }

        // Create a call with original handle. The handle may be changed when the call is attached
        // to a connection service, but in most cases will remain the same.
        Log.d(this, "start outgoing call ....");
        return new Call(
                mContext,
                this,
                mLock,
                mConnectionServiceRepository,
                mContactsAsyncHelper,
                mCallerInfoAsyncQueryFactory,
                handle,
                null /* gatewayInfo */,
                null /* connectionManagerPhoneAccount */,
                null /* phoneAccountHandle */,
                false /* isIncoming */,
                false /* isConference */);
    }

    /**
     * Kicks off the first steps to creating an outgoing call so that InCallUI can launch.
     *
     * @param handle Handle to connect the call with.
     * @param phoneAccountHandle The phone account which contains the component name of the
     *        connection service to use for this call.
     * @param extras The optional extras Bundle passed with the intent used for the incoming call.
     */
    Call startOutgoingCall(Uri handle, PhoneAccountHandle phoneAccountHandle, Bundle extras) {
        Call call = getNewOutgoingCall(handle);

        List<PhoneAccountHandle> accounts =
                mPhoneAccountRegistrar.getCallCapablePhoneAccounts(handle.getScheme(), false);

        Log.v(this, "startOutgoingCall found accounts = " + accounts);

        /// M: For recording the PhoneAccount, which is passed in. @{
        PhoneAccountHandle passedInPhoneAccountHandle = phoneAccountHandle;
        Log.v(this, "MO - startOutgoingCall account passed in = " + passedInPhoneAccountHandle);
        /// @}

        /// M: For InCallMMI, use use foreground call's account. @{
        /** Google code:
        if (mForegroundCall != null) {
        */
        if (mForegroundCall != null && isPotentialInCallMMICode(handle)) {
        /// @}
            Call ongoingCall = mForegroundCall;
            // If there is an ongoing call, use the same phone account to place this new call.
            // If the ongoing call is a conference call, we fetch the phone account from the
            // child calls because we don't have targetPhoneAccount set on Conference calls.
            // TODO: Set targetPhoneAccount for all conference calls (b/23035408).
            if (ongoingCall.getTargetPhoneAccount() == null &&
                    !ongoingCall.getChildCalls().isEmpty()) {
                ongoingCall = ongoingCall.getChildCalls().get(0);
            }
            if (ongoingCall.getTargetPhoneAccount() != null) {
                phoneAccountHandle = ongoingCall.getTargetPhoneAccount();
            }
        }

        // Only dial with the requested phoneAccount if it is still valid. Otherwise treat this call
        // as if a phoneAccount was not specified (does the default behavior instead).
        // Note: We will not attempt to dial with a requested phoneAccount if it is disabled.
        if (phoneAccountHandle != null) {
            if (!accounts.contains(phoneAccountHandle)) {
                phoneAccountHandle = null;
            }
        }

        if (phoneAccountHandle == null) {
            // No preset account, check if default exists that supports the URI scheme for the
            // handle.
            phoneAccountHandle =
                    mPhoneAccountRegistrar.getOutgoingPhoneAccountForScheme(handle.getScheme());

            /// M: For OP09 plug-in, ignoring default phone account @{
            if (!ExtensionManager.getPhoneAccountExt().shouldRemoveDefaultPhoneAccount(accounts)) {
                Log.i(this, "MO - OP09 case: account changed: %s => null", phoneAccountHandle);
                phoneAccountHandle = null;
            }
            /// @}
        }

        /// M: For Ip dial @{
        if (TelecomUtils.isIpDialRequest(mContext, call, extras)) {
            call.setIsIpCall(true);
            accounts = mPhoneAccountRegistrar.getSimPhoneAccounts();
            phoneAccountHandle = TelecomUtils.getPhoneAccountForIpDial(accounts,
                    phoneAccountHandle);
            Log.i(this, "MO - Ip dial case: account / accounts changed to be : %s / %s",
                    phoneAccountHandle, accounts);
        }
        /// @}

        /// M: Added for suggesting phone account feature. @{
        // When suggested PhoneAccountHandle is not same with defaultAccountHandle, need let user
        // to pick. @{
        if (TelecomUtils.shouldShowAccountSuggestion(extras, accounts, phoneAccountHandle)) {
            Log.i(this, "MO - Sugguest case: account changed: %s => null", phoneAccountHandle);
            phoneAccountHandle = null;
        }
        /// @}

        /// M: For VoLTE @{
        // Case of no VoLTE phoneAccount has been handled in processOutgoingCall().
        // For now, only one account supports VoLTE.
        boolean isImsCallRequest = TelecomVolteUtils.isImsCallOnlyRequest(extras);
        boolean isConferenceDialRequest = TelecomVolteUtils.isConferenceDialRequest(extras);
        if (isImsCallRequest || isConferenceDialRequest) {
            accounts = mPhoneAccountRegistrar.getVolteCallCapablePhoneAccounts();
            phoneAccountHandle = TelecomVolteUtils.getPhoneAccountForVoLTE(accounts,
                    phoneAccountHandle);
            Log.i(this, "MO - VoLTE case: account / accounts changed to be : %s / %s",
                    phoneAccountHandle, accounts);
            if (isConferenceDialRequest) {
                call.setIsConferenceDial(true);
                call.setConferenceParticipants(TelecomVolteUtils.getConferenceDialNumbers(extras));
            }
        }
        /// @}

        call.setTargetPhoneAccount(phoneAccountHandle);

        boolean isEmergencyCall = TelephonyUtil.shouldProcessAsEmergency(mContext,
                call.getHandle());
        boolean isPotentialInCallMMICode = isPotentialInCallMMICode(handle);

        /// M: For block certain ViLTE @{
        // Call.setVideoState() here, makeRoomForOutgoingCall() will use it.
        if (!isEmergencyCall
                && extras.containsKey(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE)) {
            int videoState = extras.getInt(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE,
                    VideoProfile.STATE_AUDIO_ONLY);
            call.setVideoState(videoState);
        }
        /// @}

        // Do not support any more live calls.  Our options are to move a call to hold, disconnect
        // a call, or cancel this call altogether.
        /// M: Do not allow to dial IncallMMI if already have emergency call.
        if (!makeRoomForOutgoingCall(call, isEmergencyCall)) {
            // just cancel at this point.
            Log.i(this, "No remaining room for outgoing call: %s", call);
            showToastInfomation(mContext.getResources()
                    .getString(R.string.outgoing_call_failed));
            if (mCalls.contains(call)) {
                // This call can already exist if it is a reused call,
                // See {@link #getNewOutgoingCall}.
                call.disconnect();
            }
            return null;
        }

        boolean needsAccountSelection = phoneAccountHandle == null && accounts.size() > 1 &&
                !isEmergencyCall;

        Log.i(this, "MO - dump: needSelect = %s; phoneAccountHandle = %s; accounts.size() = %s.",
                needsAccountSelection, phoneAccountHandle, accounts.size());

        /// M: CreateConnectionProcessor will choose accounts automatically. @{
        if (isEmergencyCall) {
            call.setTargetPhoneAccount(null);
            // Valid phone account for ECC is inputed.
            if (passedInPhoneAccountHandle != null
                    && accounts.contains(passedInPhoneAccountHandle)) {
                Log.i(this, "MO - Ecc passedInPhoneAccount: %s", phoneAccountHandle);
                call.setTargetPhoneAccount(passedInPhoneAccountHandle);
            }
        }
        /// @}

        /// M: For Ip dial @{
        // If we do have a sim phoneAccountHandle here, check Ip-prefix first.
        if (!TelecomUtils.handleIpPrefixForCall(mContext, call)) {
            disconnectCall(call);
            return null;
        };
        /// @}

        if (needsAccountSelection) {
            // This is the state where the user is expected to select an account
            call.setState(CallState.SELECT_PHONE_ACCOUNT, "needs account selection");
            // Create our own instance to modify (since extras may be Bundle.EMPTY)
            extras = new Bundle(extras);
            extras.putParcelableList(android.telecom.Call.AVAILABLE_PHONE_ACCOUNTS, accounts);
        } else {
            call.setState(
                    CallState.CONNECTING,
                    phoneAccountHandle == null ? "no-handle" : phoneAccountHandle.toString());
        }

        call.setIntentExtras(extras);

        // Do not add the call if it is a potential MMI code.
        if ((isPotentialMMICode(handle) || isPotentialInCallMMICode) && !needsAccountSelection) {
            call.addListener(this);
            /// M: If no account for MMI Code, show a dialog with "No SIM or SIM error" message. @{
            if (phoneAccountHandle == null) {
                Log.d(this, "MO - MMI with no sim: show error dialog and return");
                TelecomUtils.showErrorDialog(mContext, R.string.callFailed_simError);
                disconnectCall(call);
                return null;
            }
            /// @}
        } else if (!mCalls.contains(call)) {
            // We check if mCalls already contains the call because we could potentially be reusing
            // a call which was previously added (See {@link #getNewOutgoingCall}).
            addCall(call);
        }

        return call;
    }

    /**
     * Check if ok to dial ECC, the phone state should be idle, no call exsit.
     */
    boolean isOkForECC() {
        if (mCalls.size() == 0) {
            return true;
        }
        return false;
    }

    /**
     * Attempts to issue/connect the specified call.
     *
     * @param handle Handle to connect the call with.
     * @param gatewayInfo Optional gateway information that can be used to route the call to the
     *        actual dialed handle via a gateway provider. May be null.
     * @param speakerphoneOn Whether or not to turn the speakerphone on once the call connects.
     * @param videoState The desired video state for the outgoing call.
     */
    void placeOutgoingCall(Call call, Uri handle, GatewayInfo gatewayInfo, boolean speakerphoneOn,
            int videoState) {
        if (call == null) {
            // don't do anything if the call no longer exists
            Log.i(this, "Canceling unknown call.");
            return;
        }
        /// M: For CDMA India optr @{
        broadcastCallPlacedIntent(call);
        /// @}

        final Uri uriHandle = (gatewayInfo == null) ? handle : gatewayInfo.getGatewayAddress();

        if (gatewayInfo == null) {
            Log.i(this, "Creating a new outgoing call with handle: %s", Log.piiHandle(uriHandle));
        } else {
            Log.i(this, "Creating a new outgoing call with gateway handle: %s, original handle: %s",
                    Log.pii(uriHandle), Log.pii(handle));
        }

        call.setHandle(uriHandle);
        call.setGatewayInfo(gatewayInfo);
        call.setVideoState(videoState);

        if (speakerphoneOn) {
            Log.i(this, "%s Starting with speakerphone as requested", call);
        } else {
            Log.i(this, "%s Starting with speakerphone because car is docked.", call);
        }

        if (neededForceSpeakerOn()) {
            call.setStartWithSpeakerphoneOn(true);
        } else {
            call.setStartWithSpeakerphoneOn(speakerphoneOn || mDockManager.isDocked());
        }

        boolean isEmergencyCall = TelephonyUtil.shouldProcessAsEmergency(mContext,
                call.getHandle());
        Log.i(this, "placeOutgoingCall isEmergencyCall = " + isEmergencyCall);
        if (isEmergencyCall) {
            /**
             * M: For two dial icon, valid phone account for ECC should be reserved.
             * android original code:
            // Emergency -- CreateConnectionProcessor will choose accounts automatically
            call.setTargetPhoneAccount(null);
             */
            mCalls.remove(call);
            if (!isOkForECC()) {
                Log.i(this, "placeOutgoingCall now is not ok for ECC, waiting ......");
                mPendingECCCall = call;
                mHasPendingECC = true;
                disconnectAllCalls();
                mCalls.add(call);
                return;
            }
            mCalls.add(call);
        }

        /// M: ALPS02035599 Since NewOutgoingCallIntentBroadcaster and the SELECT_PHONE_ACCOUNT @{
        // sequence run in parallel, this call may be already disconnected in the  select phone
        // account sequence.
        if (call.getState() == CallState.DISCONNECTED) {
            return;
        }
        /// @}

        if (call.getTargetPhoneAccount() != null || isEmergencyCall) {
            // If the account has been set, proceed to place the outgoing call.
            // Otherwise the connection will be initiated when the account is set by the user.
            call.startCreateConnection(mPhoneAccountRegistrar);
        }
    }

    /**
     * Attempts to start a conference call for the specified call.
     *
     * @param call The call to conference.
     * @param otherCall The other call to conference with.
     */
    void conference(Call call, Call otherCall) {
        call.conferenceWith(otherCall);
    }

    /**
     * Instructs Telecom to answer the specified call. Intended to be invoked by the in-call
     * app through {@link InCallAdapter} after Telecom notifies it of an incoming call followed by
     * the user opting to answer said call.
     *
     * @param call The call to answer.
     * @param videoState The video state in which to answer the call.
     */
    void answerCall(Call call, int videoState) {
        if (!mCalls.contains(call)) {
            Log.i(this, "Request to answer a non-existent call %s", call);
        } else {
            // If the foreground call is not the ringing call and it is currently isActive() or
            // STATE_DIALING, put it on hold before answering the call.
            if (mForegroundCall != null && mForegroundCall != call &&
                    (mForegroundCall.isActive() ||
                     mForegroundCall.getState() == CallState.DIALING)) {
                if (0 == (mForegroundCall.getConnectionCapabilities()
                        & Connection.CAPABILITY_HOLD)) {
                    // This call does not support hold.  If it is from a different connection
                    // service, then disconnect it, otherwise allow the connection service to
                    // figure out the right states.
                    if (mForegroundCall.getConnectionService() != call.getConnectionService()) {
                        mForegroundCall.disconnect();
                    }
                } else {
                    Call heldCall = getHeldCall();
                    if (heldCall != null) {
                        Log.v(this, "Disconnecting held call %s before holding active call.",
                                heldCall);
                        heldCall.disconnect();
                        /// M: MSMA call control. @{
                        // Add answer waiting call as a pending call action. Some modem cann't
                        // handle disconnect and answer at the same time. So here need to wait
                        // disconnect complete.
                        addPendingCallAction(heldCall, call,
                                PendingCallAction.PENDING_ACTION_ANSWER, videoState);
                        Log.v(this, "Holding active call %s before answering incoming call %s.",
                                mForegroundCall, call);
                        mForegroundCall.hold();
                        return;
                        /// @}
                    }

                    Log.v(this, "Holding active/dialing call %s before answering incoming call %s.",
                            mForegroundCall, call);
                    mForegroundCall.hold();
                }
                // TODO: Wait until we get confirmation of the active call being
                // on-hold before answering the new call.
                // TODO: Import logic from CallManager.acceptCall()
            }

            for (CallsManagerListener listener : mListeners) {
                listener.onIncomingCallAnswered(call);
            }

            // We do not update the UI until we get confirmation of the answer() through
            // {@link #markCallAsActive}.
            call.answer(videoState);
            if (VideoProfile.isVideo(videoState) &&
                !mWiredHeadsetManager.isPluggedIn() &&
                !mCallAudioManager.isBluetoothDeviceAvailable() &&
                isSpeakerEnabledForVideoCalls()) {
                call.setStartWithSpeakerphoneOn(true);
            }
        }
    }

    private static boolean isSpeakerEnabledForVideoCalls() {
        return (SystemProperties.getInt(TelephonyProperties.PROPERTY_VIDEOCALL_AUDIO_OUTPUT,
                PhoneConstants.AUDIO_OUTPUT_DEFAULT) ==
                PhoneConstants.AUDIO_OUTPUT_ENABLE_SPEAKER);
    }

    /**
     * Instructs Telecom to reject the specified call. Intended to be invoked by the in-call
     * app through {@link InCallAdapter} after Telecom notifies it of an incoming call followed by
     * the user opting to reject said call.
     */
    void rejectCall(Call call, boolean rejectWithMessage, String textMessage) {
        if (!mCalls.contains(call)) {
            Log.i(this, "Request to reject a non-existent call %s", call);
        } else {
            for (CallsManagerListener listener : mListeners) {
                listener.onIncomingCallRejected(call, rejectWithMessage, textMessage);
            }
            call.reject(rejectWithMessage, textMessage);
        }
    }

    /**
     * Instructs Telecom to play the specified DTMF tone within the specified call.
     *
     * @param digit The DTMF digit to play.
     */
    void playDtmfTone(Call call, char digit) {
        if (!mCalls.contains(call)) {
            Log.i(this, "Request to play DTMF in a non-existent call %s", call);
        } else {
            call.playDtmfTone(digit);
            mDtmfLocalTonePlayer.playTone(call, digit);
        }
    }

    /**
     * Instructs Telecom to stop the currently playing DTMF tone, if any.
     */
    void stopDtmfTone(Call call) {
        if (!mCalls.contains(call)) {
            Log.i(this, "Request to stop DTMF in a non-existent call %s", call);
        } else {
            call.stopDtmfTone();
            mDtmfLocalTonePlayer.stopTone(call);
        }
    }

    /**
     * Instructs Telecom to continue (or not) the current post-dial DTMF string, if any.
     */
    void postDialContinue(Call call, boolean proceed) {
        //ALPS01833456 call maybe null
        if (call != null) {
            if (!mCalls.contains(call)) {
                Log.i(this, "Request to continue post-dial string in a non-existent call %s", call);
            } else {
                call.postDialContinue(proceed);
            }
        }
    }

    /**
     * Instructs Telecom to disconnect the specified call. Intended to be invoked by the
     * in-call app through {@link InCallAdapter} for an ongoing call. This is usually triggered by
     * the user hitting the end-call button.
     */
    void disconnectCall(Call call) {
        Log.v(this, "disconnectCall %s", call);

        if (!mCalls.contains(call)) {
            Log.w(this, "Unknown call (%s) asked to disconnect", call);
        } else {
            mLocallyDisconnectingCalls.add(call);
            /// M: CC start. @{
            // All childCalls whithin a conference call should not be updated as foreground
            for (Call childCall : call.getChildCalls()) {
                mLocallyDisconnectingCalls.add(childCall);
            }
            /// @}
            call.disconnect();
        }
    }

    /**
     * Instructs Telecom to disconnect all calls.
     */
    void disconnectAllCalls() {
        Log.v(this, "disconnectAllCalls");

        for (Call call : mCalls) {
            /// M: only disconnect top level calls. @{
            if (call.getParentCall() != null) {
                continue;
            }
            /// @}
            disconnectCall(call);
        }
    }

    /**
     * Instructs Telecom to put the specified call on hold. Intended to be invoked by the
     * in-call app through {@link InCallAdapter} for an ongoing call. This is usually triggered by
     * the user hitting the hold button during an active call.
     */
    void holdCall(Call call) {
        if (!mCalls.contains(call)) {
            Log.w(this, "Unknown call (%s) asked to be put on hold", call);
        } else {
            Log.d(this, "Putting call on hold: (%s)", call);
            call.hold();
        }
        /// M: When have active call and hold call in different account, hold operation will
        // swap the two call.
        Call heldCall = getHeldCall();
        if (heldCall != null &&
                !Objects.equals(call.getTargetPhoneAccount(), heldCall.getTargetPhoneAccount())) {
            heldCall.unhold();
        }
        /// @}
    }

    /**
     * Instructs Telecom to release the specified call from hold. Intended to be invoked by
     * the in-call app through {@link InCallAdapter} for an ongoing call. This is usually triggered
     * by the user hitting the hold button during a held call.
     */
    void unholdCall(Call call) {
        if (!mCalls.contains(call)) {
            Log.w(this, "Unknown call (%s) asked to be removed from hold", call);
        } else {
            Log.d(this, "unholding call: (%s)", call);
            /* Google Original code
            for (Call c : mCalls) {
                // Only attempt to hold parent calls and not the individual children.
                if (c != null && c.isAlive() && c != call && c.getParentCall() == null) {
                    c.hold();
                }
            } */
            /// M: If foreground call doesn't support hold, should not do unhold. @{
            if (mForegroundCall != null && mForegroundCall != call) {
                if (mForegroundCall.can(Connection.CAPABILITY_HOLD)) {
                    mForegroundCall.hold();
                } else {
                    return;
                }
            }
            /// @}
            call.unhold();
        }
    }

    /** Called by the in-call UI to change the mute state. */
    void mute(boolean shouldMute) {
        mCallAudioManager.mute(shouldMute);
    }

    /**
      * Called by the in-call UI to change the audio route, for example to change from earpiece to
      * speaker phone.
      */
    void setAudioRoute(int route) {
        mCallAudioManager.setAudioRoute(route);
    }

    /** Called by the in-call UI to turn the proximity sensor on. */
    void turnOnProximitySensor() {
        mProximitySensorManager.turnOn();
    }

    /**
     * Called by the in-call UI to turn the proximity sensor off.
     * @param screenOnImmediately If true, the screen will be turned on immediately. Otherwise,
     *        the screen will be kept off until the proximity sensor goes negative.
     */
    void turnOffProximitySensor(boolean screenOnImmediately) {
        mProximitySensorManager.turnOff(screenOnImmediately);
    }

    void phoneAccountSelected(Call call, PhoneAccountHandle account, boolean setDefault) {
        if (setDefault) {
            mPhoneAccountRegistrar.setUserSelectedOutgoingPhoneAccount(account);
        }
        if (!mCalls.contains(call) && !isPotentialMMICode(call.getHandle())
                && !isPotentialInCallMMICode(call.getHandle())) {
            Log.i(this, "Attempted to add account to unknown call %s", call);
        } else {
            // TODO: There is an odd race condition here. Since NewOutgoingCallIntentBroadcaster and
            // the SELECT_PHONE_ACCOUNT sequence run in parallel, if the user selects an account before the
            // NEW_OUTGOING_CALL sequence finishes, we'll start the call immediately without
            // respecting a rewritten number or a canceled number. This is unlikely since
            // NEW_OUTGOING_CALL sequence, in practice, runs a lot faster than the user selecting
            // a phone account from the in-call UI.
            call.setTargetPhoneAccount(account);

            /// M: For Ip dial @{
            if (!TelecomUtils.handleIpPrefixForCall(mContext, call)) {
                disconnectCall(call);
                return;
            };
            /// @}

            // Note: emergency calls never go through account selection dialog so they never
            // arrive here.
            if (makeRoomForOutgoingCall(call, false /* isEmergencyCall */)) {
                call.startCreateConnection(mPhoneAccountRegistrar);
            } else {
                showToastInfomation(mContext.getResources()
                        .getString(R.string.outgoing_call_failed));
                call.disconnect();
            }
            /// M: For CDMA India optr @{
            broadcastCallPlacedIntent(call);
            /// @}
        }
    }

    /// M: For CDMA India optr @{
    private void broadcastCallPlacedIntent(Call call) {
        Log.d(this, "broadcastCallPlacedIntent Entry");
        if (SystemProperties.get("persist.sys.esn_track_switch").equals("1")) {
            PhoneAccountHandle phoneAccount = call.getTargetPhoneAccount();
            Log.d(this, "broadcastCallPlacedIntent phoneAccount= " + phoneAccount);
            if (phoneAccount != null) {
                String subIdOld = phoneAccount.getId();
                Log.d(this, "broadcastCallPlacedIntent phoneAccount not null and subId= "
                                                                + subIdOld);
                TelephonyManager tem = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);

                TelecomManager tcm = (TelecomManager) mContext
                .getSystemService(Context.TELECOM_SERVICE);

                PhoneAccount account = tcm.getPhoneAccount(phoneAccount);
                int subIdforM = tem.getSubIdForPhoneAccount(account);
                Log.d(this, "broadcastCallPlacedIntent phoneAccountHandle " + phoneAccount);
                Log.d(this, "broadcastCallPlacedIntent phoneAccount account = " + account);
                Log.d(this, "broadcastCallPlacedIntent phoneAccount not null and subIdforM= "
                                                                     + subIdforM);
                String subId = Integer.toString(subIdforM);
                if (subId != null && subId.length() > 0) {
                    try {
                        int subIdInt = Integer.parseInt(subId);
                        Log.d(this, "phoneAccountSelected cdma subIdInt= " + subIdInt);
                        mContext.sendBroadcast(new Intent(ACTION_ESN_MO_CALL)
                                     .putExtra(PhoneConstants.SUBSCRIPTION_KEY, subIdInt));
                    } catch (NumberFormatException nfe) {
                        Log.d(this, "NumberFormatException occured");
                        return ;
                    }
                }
                Log.d(this, "broadcastCallPlacedIntent subid check fail");
            }
            Log.d(this, "broadcastCallPlacedIntent phoneAccount null");
        }
    }
    /// @}

    /** Called when the audio state changes. */
    void onCallAudioStateChanged(CallAudioState oldAudioState, CallAudioState newAudioState) {
        Log.v(this, "onAudioStateChanged, audioState: %s -> %s", oldAudioState, newAudioState);
        for (CallsManagerListener listener : mListeners) {
            listener.onCallAudioStateChanged(oldAudioState, newAudioState);
        }
    }

    void markCallAsRinging(Call call) {
        setCallState(call, CallState.RINGING, "ringing set explicitly");
    }

    void markCallAsDialing(Call call) {
        setCallState(call, CallState.DIALING, "dialing set explicitly");
        maybeMoveToSpeakerPhone(call);
    }

    void markCallAsActive(Call call) {
        setCallState(call, CallState.ACTIVE, "active set explicitly");
        maybeMoveToSpeakerPhone(call);
    }

    void markCallAsOnHold(Call call) {
        setCallState(call, CallState.ON_HOLD, "on-hold set explicitly");
    }

    /**
     * Marks the specified call as STATE_DISCONNECTED and notifies the in-call app. If this was the
     * last live call, then also disconnect from the in-call controller.
     *
     * @param disconnectCause The disconnect cause, see {@link android.telecom.DisconnectCause}.
     */
    void markCallAsDisconnected(Call call, DisconnectCause disconnectCause) {

        /// M: for volte @{
        // TODO: if disconnectCause is IMS_EMERGENCY_REREG, redial it and do not notify disconnect.
//        placeOutgoingCall(call, handle, gatewayInfo, speakerphoneOn, videoState);
        /// @}

        call.setDisconnectCause(disconnectCause);
        setCallState(call, CallState.DISCONNECTED, "disconnected set explicitly");
    }

    /**
     * Removes an existing disconnected call, and notifies the in-call app.
     */
    void markCallAsRemoved(Call call) {
        removeCall(call);
        if (mLocallyDisconnectingCalls.contains(call)) {
            mLocallyDisconnectingCalls.remove(call);
            if (mForegroundCall != null && mForegroundCall.getState() == CallState.ON_HOLD
                    //// M: ALPS01765683 Disconnect a member in conference (in HOLD status),
                    // the conference should still in hold status. @{
                    && !(call.getChildCalls().size() > 0
                    && call.getChildCalls().contains(mForegroundCall))) {
                    //// @}
                mForegroundCall.unhold();
            }
        }
        /// M: after disconnect all calls, if there is a pending ecc, call immediately @{
        if (mHasPendingECC) {
            mCalls.remove(mPendingECCCall);
            Log.i(this, "markCallAsDisconnected mCalls size = " + mCalls.size());
            if (mCalls.size() == 0) {
                Log.i(this, "markCallAsDisconnected re-dial ECC!");
                mPendingECCCall.startCreateConnection(mPhoneAccountRegistrar);
                mHasPendingECC = false;
            }
            mCalls.add(mPendingECCCall);
        }
        /// @}
    }

    /**
     * Cleans up any calls currently associated with the specified connection service when the
     * service binder disconnects unexpectedly.
     *
     * @param service The connection service that disconnected.
     */
    void handleConnectionServiceDeath(ConnectionServiceWrapper service) {
        if (service != null) {
            for (Call call : mCalls) {
                if (call.getConnectionService() == service) {
                    if (call.getState() != CallState.DISCONNECTED) {
                        markCallAsDisconnected(call, new DisconnectCause(DisconnectCause.ERROR));
                    }
                    markCallAsRemoved(call);
                }
            }
        }
    }

    boolean hasAnyCalls() {
        return !mCalls.isEmpty();
    }

    boolean hasActiveOrHoldingCall() {
        return getFirstCallWithState(CallState.ACTIVE, CallState.ON_HOLD) != null;
    }

    boolean hasRingingCall() {
        return getFirstCallWithState(CallState.RINGING) != null ||
               ///M: checking "NEW" incoming call to avoid timing issue.
               getFirstCallWithState(CallState.NEW) != null &&
               getFirstCallWithState(CallState.NEW).isIncoming();
    }

    boolean onMediaButton(int type) {
        if (hasAnyCalls()) {
            if (HeadsetMediaButton.SHORT_PRESS == type) {
                Call ringingCall = getFirstCallWithState(CallState.RINGING);
                if (ringingCall == null) {
                    mCallAudioManager.toggleMute();
                    return true;
                } else {
                    //ringingCall.answer(ringingCall.getVideoState());
                    answerCall(ringingCall, ringingCall.getVideoState());
                    return true;
                }
            } else if (HeadsetMediaButton.LONG_PRESS == type) {
                Log.d(this, "handleHeadsetHook: longpress -> hangup");
                Call callToHangup = getFirstCallWithState(
                        CallState.RINGING, CallState.DIALING, CallState.ACTIVE, CallState.ON_HOLD);
                if (callToHangup != null) {
                    /// M: ALPS01790323. disconnect call through CallsManager
                    /*
                     * original code
                     * callToHangup.disconnect();
                     */
                    disconnectCall(callToHangup);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if telecom supports adding another top-level call.
     */
    boolean canAddCall() {
        /// M: MSMA call control. Need to support more than 2 top level calls. @{
        if (hasEmergencyCall() || hasRingingCall()) {
            return false;
        }
        if (getFirstCallWithState(OUTGOING_CALL_STATES) != null) {
            return false;
        }

        int count = 0;
        for (Call call : mCalls) {
            if (call.isEmergencyCall()) {
                // We never support add call if one of the calls is an emergency call.
                return false;
            } else  if (!call.getChildCalls().isEmpty() && !call.can(Connection.CAPABILITY_HOLD)) {
                // This is to deal with CDMA conference calls. CDMA conference calls do not
                // allow the addition of another call when it is already in a 3 way conference.
                // So, we detect that it is a CDMA conference call by checking if the call has
                // some children and it does not support the CAPABILILTY_HOLD
                // TODO: This maybe cleaner if the lower layers can explicitly signal to telecom
                // about this limitation (b/22880180).
                return false;

            /// M: ALPS02445100. We should not dial another call when there exists a dialing cdma
            /// call. So we disable the add call capapility here. We need to check if the call is
            /// is outgoing, because when ring call changed to active the HOLD capability is not
            /// added at once, and can not refresh the add callbutton. @{
            } else if(TelecomUtils.hasCdmaCallCapability(mContext, call.getTargetPhoneAccount()) &&
                    !call.can(Connection.CAPABILITY_HOLD) && !call.isIncoming() &&
                    call.getParentCall() == null) {
                return false;
            /// @}
            } else if (call.getParentCall() == null) {
                count++;
            }

            // We do not check states for canAddCall. We treat disconnected calls the same
            // and wait until they are removed instead. If we didn't count disconnected calls,
            // we could put InCallServices into a state where they are showing two calls but
            // also support add-call. Technically it's right, but overall looks better (UI-wise)
            // and acts better if we wait until the call is removed.
            if (count >= MAXIMUM_TOP_LEVEL_CALLS) {
                return false;
            }
        }
        return true;
    }

    @VisibleForTesting
    public Call getRingingCall() {
        return getFirstCallWithState(CallState.RINGING);
    }

    ///M: add to get CallsManager instance.
    public Call getActiveCall() {
        return getFirstCallWithState(CallState.ACTIVE);
    }

    Call getDialingCall() {
        return getFirstCallWithState(CallState.DIALING);
    }

    Call getHeldCall() {
        return getFirstCallWithState(CallState.ON_HOLD);
    }

    int getNumHeldCalls() {
        int count = 0;
        for (Call call : mCalls) {
            if (call.getParentCall() == null && call.getState() == CallState.ON_HOLD) {
                count++;
            }
        }
        return count;
    }

    Call getOutgoingCall() {
        return getFirstCallWithState(OUTGOING_CALL_STATES);
    }

    Call getFirstCallWithState(int... states) {
        return getFirstCallWithState(null, states);
    }

    /**
     * Returns the first call that it finds with the given states. The states are treated as having
     * priority order so that any call with the first state will be returned before any call with
     * states listed later in the parameter list.
     *
     * @param callToSkip Call that this method should skip while searching
     */
    Call getFirstCallWithState(Call callToSkip, int... states) {
        for (int currentState : states) {
            // check the foreground first
            if (mForegroundCall != null && mForegroundCall.getState() == currentState) {
                return mForegroundCall;
            }

            for (Call call : mCalls) {
                if (Objects.equals(callToSkip, call)) {
                    continue;
                }

                // Only operate on top-level calls
                if (call.getParentCall() != null) {
                    continue;
                }

                if (currentState == call.getState()) {
                    return call;
                }
            }
        }
        return null;
    }

    Call createConferenceCall(
            PhoneAccountHandle phoneAccount,
            ParcelableConference parcelableConference) {

        // If the parceled conference specifies a connect time, use it; otherwise default to 0,
        // which is the default value for new Calls.
        long connectTime =
                parcelableConference.getConnectTimeMillis() ==
                        Conference.CONNECT_TIME_NOT_SPECIFIED ? 0 :
                        parcelableConference.getConnectTimeMillis();

        Call call = new Call(
                mContext,
                this,
                mLock,
                mConnectionServiceRepository,
                mContactsAsyncHelper,
                mCallerInfoAsyncQueryFactory,
                null /* handle */,
                null /* gatewayInfo */,
                null /* connectionManagerPhoneAccount */,
                phoneAccount,
                false /* isIncoming */,
                true /* isConference */,
                connectTime);

        setCallState(call, Call.getStateFromConnectionState(parcelableConference.getState()),
                "new conference call");
        call.setConnectionCapabilities(parcelableConference.getConnectionCapabilities());
        call.setVideoState(parcelableConference.getVideoState());
        call.setVideoProvider(parcelableConference.getVideoProvider());
        call.setStatusHints(parcelableConference.getStatusHints());
        call.setExtras(parcelableConference.getExtras());

        // TODO: Move this to be a part of addCall()
        call.addListener(this);
        addCall(call);
        return call;
    }

    /**
     * @return the call state currently tracked by {@link PhoneStateBroadcaster}
     */
    int getCallState() {
        return mPhoneStateBroadcaster.getCallState();
    }

    /**
     * Retrieves the {@link PhoneAccountRegistrar}.
     *
     * @return The {@link PhoneAccountRegistrar}.
     */
    PhoneAccountRegistrar getPhoneAccountRegistrar() {
        return mPhoneAccountRegistrar;
    }

    /**
     * Retrieves the {@link MissedCallNotifier}
     * @return The {@link MissedCallNotifier}.
     */
    MissedCallNotifier getMissedCallNotifier() {
        return mMissedCallNotifier;
    }

    /**
     * Adds the specified call to the main list of live calls.
     *
     * @param call The call to add.
     */
    private void addCall(Call call) {
        Trace.beginSection("addCall");
        Log.v(this, "addCall(%s)", call);
        call.addListener(this);
        mCalls.add(call);

        // TODO: Update mForegroundCall prior to invoking
        // onCallAdded for calls which immediately take the foreground (like the first call).
        for (CallsManagerListener listener : mListeners) {
            if (Log.SYSTRACE_DEBUG) {
                Trace.beginSection(listener.getClass().toString() + " addCall");
            }
            listener.onCallAdded(call);
            if (Log.SYSTRACE_DEBUG) {
                Trace.endSection();
            }
        }
        updateCallsManagerState();
        Trace.endSection();
    }

    private void removeCall(Call call) {
        Trace.beginSection("removeCall");
        Log.v(this, "removeCall(%s)", call);

        call.setParentCall(null);  // need to clean up parent relationship before destroying.
        call.removeListener(this);
        call.clearConnectionService();

        boolean shouldNotify = false;
        if (mCalls.contains(call)) {
            mCalls.remove(call);
            shouldNotify = true;
        }

        call.destroy();

        // Only broadcast changes for calls that are being tracked.
        if (shouldNotify) {
            for (CallsManagerListener listener : mListeners) {
                if (Log.SYSTRACE_DEBUG) {
                    Trace.beginSection(listener.getClass().toString() + " onCallRemoved");
                }
                listener.onCallRemoved(call);
                if (Log.SYSTRACE_DEBUG) {
                    Trace.endSection();
                }
            }
            updateCallsManagerState();
        }
        Trace.endSection();
    }

    /**
     * Sets the specified state on the specified call.
     *
     * @param call The call.
     * @param newState The new state of the call.
     */
    private void setCallState(Call call, int newState, String tag) {
        if (call == null) {
            return;
        }
        int oldState = call.getState();
        Log.i(this, "setCallState %s -> %s, call: %s", CallState.toString(oldState),
                CallState.toString(newState), call);
        if (newState != oldState) {
            // Unfortunately, in the telephony world the radio is king. So if the call notifies
            // us that the call is in a particular state, we allow it even if it doesn't make
            // sense (e.g., STATE_ACTIVE -> STATE_RINGING).
            // TODO: Consider putting a stop to the above and turning CallState
            // into a well-defined state machine.
            // TODO: Define expected state transitions here, and log when an
            // unexpected transition occurs.
            call.setState(newState, tag);

            /// M: Set the voice recording capability
            int capabilities = call.getConnectionCapabilities();
            boolean hasRecordCap = (capabilities & Connection.CAPABILITY_VOICE_RECORD) == 0
                    ? false : true;
            boolean okToRecord = okToRecordVoice(call);
            if (okToRecord && !hasRecordCap) {
                call.setConnectionCapabilities(capabilities
                        | Connection.CAPABILITY_VOICE_RECORD);
            }
            if (!okToRecord && hasRecordCap) {
                PhoneRecorderHandler.getInstance().stopRecording();
                call.setConnectionCapabilities(capabilities
                      & ~Connection.CAPABILITY_VOICE_RECORD);
            }

            Trace.beginSection("onCallStateChanged");
            // Only broadcast state change for calls that are being tracked.
            if (mCalls.contains(call)) {
                for (CallsManagerListener listener : mListeners) {
                    if (Log.SYSTRACE_DEBUG) {
                        Trace.beginSection(listener.getClass().toString() + " onCallStateChanged");
                    }
                    listener.onCallStateChanged(call, oldState, newState);
                    if (Log.SYSTRACE_DEBUG) {
                        Trace.endSection();
                    }
                }
                updateCallsManagerState();
            }
            /// M: MSMA call control, first call action finished. @{
            handleActionProcessComplete(call);
            /// @}
            Trace.endSection();
        }
    }

    /**
     * Checks which call should be visible to the user and have audio focus.
     */
    private void updateForegroundCall() {
        Trace.beginSection("updateForegroundCall");
        Call newForegroundCall = null;
        for (Call call : mCalls) {
            // TODO: Foreground-ness needs to be explicitly set. No call, regardless
            // of its state will be foreground by default and instead the connection service should
            // be notified when its calls enter and exit foreground state. Foreground will mean that
            // the call should play audio and listen to microphone if it wants.

            // Only top-level calls can be in foreground
            if (call.getParentCall() != null) {
                continue;
            }
            /// M: CC start. @{
            // All childCalls whithin a conference call should not be updated as foreground
            if (mLocallyDisconnectingCalls.contains(call)) {
                continue;
            }
            /// @}

            /// M:ALPS01833814 can not disconnect outgoing call when answer the ringing call. @{
            // Active and outgoing calls have priority.
            if (call.isActive() || call.getState() == CallState.DIALING
                    || call.getState() == CallState.CONNECTING) {
                newForegroundCall = call;
                break;
            }
            /// @}

            /**
             * M: [ALPS01752136]if there is no Active call, the Ringing one should have a
             * higher priority than hold ones, so as to play Incoming Ringtone.
             * google original code:
             *
            if (call.isAlive() || call.getState() == CallState.RINGING) {
                newForegroundCall = call;
                // Don't break in case there's an active call that has priority.
            }
             */
        }

        /// M: [ALPS01752136]if no Active call, Ringing > Holding @{
        newForegroundCall = pickForegroundCallEx(newForegroundCall);
        /// @}

        if (newForegroundCall != mForegroundCall) {
            /// M: Need to stop recording when foregroundcall changed, e.g. when merge calls
            PhoneRecorderHandler.getInstance().stopRecording();
            /// M: ALPS01750786. If new forground call and old forground call state are same,
            /// do not change.
            if (mForegroundCall == null || newForegroundCall == null
                    || !(newForegroundCall.getState() == mForegroundCall.getState()
                        && mForegroundCall.getParentCall() != newForegroundCall)) {
                Log.v(this, "Updating foreground call, %s -> %s.", mForegroundCall, newForegroundCall);
                Call oldForegroundCall = mForegroundCall;
                mForegroundCall = newForegroundCall;

                /* ALPS01778496 & ALPS01762509, follow L default flow, play ringtone when background call is hold
                /// M: ALPS01762509. Do not notify the change to avoid play ringtone instead of call waiting tone
                if ((oldForegroundCall != null && oldForegroundCall.getState() == CallState.ON_HOLD)
                        && (mForegroundCall != null && mForegroundCall.getState() == CallState.RINGING)) {
                    return;
                }
                */

                for (CallsManagerListener listener : mListeners) {
                    if (Log.SYSTRACE_DEBUG) {
                        Trace.beginSection(listener.getClass().toString() + " updateForegroundCall");
                    }
                    listener.onForegroundCallChanged(oldForegroundCall, mForegroundCall);
                    if (Log.SYSTRACE_DEBUG) {
                        Trace.endSection();
                    }
                }
            }
        }
        Trace.endSection();
    }

    private void updateCanAddCall() {
        boolean newCanAddCall = canAddCall();
        if (newCanAddCall != mCanAddCall) {
            mCanAddCall = newCanAddCall;
            for (CallsManagerListener listener : mListeners) {
                if (Log.SYSTRACE_DEBUG) {
                    Trace.beginSection(listener.getClass().toString() + " updateCanAddCall");
                }
                listener.onCanAddCallChanged(mCanAddCall);
                if (Log.SYSTRACE_DEBUG) {
                    Trace.endSection();
                }
            }
        }
    }

    private boolean isPotentialMMICode(Uri handle) {
        /** M:android default code @{
        return (handle != null && handle.getSchemeSpecificPart() != null
                && handle.getSchemeSpecificPart().contains("#"));
        @ } */
        String number = handle != null ? handle.getSchemeSpecificPart() : null;
        if (TextUtils.isEmpty(number)) {
            return false;
        }

        ///M: add for test case TC31.9.1.1 @{
        if (number.trim().equals("7") ||
                number.trim().equals("36")) {
              int currentMode = SystemProperties.getInt("gsm.gcf.testmode", 0);
              if (currentMode == 2) {// mode 2 stands for FTA Mode.
                 return true;
              }
        }
        /// @}

        ///M: MMI doesn't contain "@"
        return (number.contains("#") && !number.contains("@"));
    }

    private void updateCallsManagerState() {
        updateForegroundCall();
        updateCanAddCall();
    }

    /**
     * Determines if a dialed number is potentially an In-Call MMI code.  In-Call MMI codes are
     * MMI codes which can be dialed when one or more calls are in progress.
     * <P>
     * Checks for numbers formatted similar to the MMI codes defined in:
     * {@link com.android.internal.telephony.gsm.GSMPhone#handleInCallMmiCommands(String)}
     * and
     * {@link com.android.internal.telephony.imsphone.ImsPhone#handleInCallMmiCommands(String)}
     *
     * @param handle The URI to call.
     * @return {@code True} if the URI represents a number which could be an in-call MMI code.
     */
    private boolean isPotentialInCallMMICode(Uri handle) {
        if (handle != null && handle.getSchemeSpecificPart() != null &&
                handle.getScheme().equals(PhoneAccount.SCHEME_TEL)) {

            String dialedNumber = handle.getSchemeSpecificPart();
            return (dialedNumber.equals("0") ||
                    (dialedNumber.startsWith("1") && dialedNumber.length() <= 2) ||
                    (dialedNumber.startsWith("2") && dialedNumber.length() <= 2) ||
                    dialedNumber.equals("3") ||
                    dialedNumber.equals("4") ||
                    dialedNumber.equals("5"));
        }
        return false;
    }

    private int getNumCallsWithState(int... states) {
        int count = 0;
        for (int state : states) {
            for (Call call : mCalls) {
                if (call.getParentCall() == null && call.getState() == state) {
                // Only top-level calls will be counted.
                    count++;
                }
            }
        }
        return count;
    }

    private boolean hasMaximumLiveCalls() {
        return MAXIMUM_LIVE_CALLS <= getNumCallsWithState(LIVE_CALL_STATES);
    }

    private boolean hasMaximumHoldingCalls() {
        return MAXIMUM_HOLD_CALLS <= getNumCallsWithState(CallState.ON_HOLD);
    }

    private boolean hasMaximumRingingCalls() {
        return MAXIMUM_RINGING_CALLS <= getNumCallsWithState(CallState.RINGING);
    }

    private boolean hasMaximumOutgoingCalls() {
        return MAXIMUM_OUTGOING_CALLS <= getNumCallsWithState(OUTGOING_CALL_STATES);
    }

    private boolean hasMaximumDialingCalls() {
        return MAXIMUM_DIALING_CALLS <= getNumCallsWithState(CallState.DIALING);
    }

    private boolean makeRoomForOutgoingCall(Call call, boolean isEmergency) {
        /// M: Add some outgoing call control rule @{
        if (hasOtherEmergencyCall(call)) {
            return false;
        }
        if (isEmergency) {
            return true;
        }
        if (preventCallFromOtherSimBasedAccountForDsds(call)) {
            return false;
        }
        Uri handle = call.getHandle();
        if (isPotentialMMICode(handle) || isPotentialInCallMMICode(handle)) {
            return true;
        }
        if (hasRingingCall()) {
            Log.i(this, "can not start outgoing call, have ringing call.");
            return false;
        }
        /// @}

        /// M: For block certain ViLTE @{
        if (shouldBlockForCertainViLTE(call)) {
            Log.i(this, "makeRoomForOutgoingCall: Block certain ViLTE!");
            return false;
        }
        /// @}

        if (hasMaximumLiveCalls()) {
            // NOTE: If the amount of live calls changes beyond 1, this logic will probably
            // have to change.
            Call liveCall = getFirstCallWithState(call, LIVE_CALL_STATES);
            Log.i(this, "makeRoomForOutgoingCall call = " + call + " livecall = " +
                   liveCall);

            /// M: If exist only one live call, and it is the new outgoing call itself,
            // the liveCall calculated above will be null. @{
            if (liveCall == null || call == liveCall) {
            /// @}
                // If the call is already the foreground call, then we are golden.
                // This can happen after the user selects an account in the SELECT_PHONE_ACCOUNT
                // state since the call was already populated into the list.
                return true;
            }

            /// M: Should skip the new outgoing call itself. @{
            Call outgoingCall = getFirstCallWithState(call, OUTGOING_CALL_STATES);
            if (outgoingCall != null) {
            /// @}
                if (isEmergency && !outgoingCall.isEmergencyCall()) {
                    // Disconnect the current outgoing call if it's not an emergency call. If the
                    // user tries to make two outgoing calls to different emergency call numbers,
                    // we will try to connect the first outgoing call.
                    outgoingCall.disconnect();
                    return true;
                }
                if (outgoingCall.getState() == CallState.SELECT_PHONE_ACCOUNT) {
                    // If there is an orphaned call in the {@link CallState#SELECT_PHONE_ACCOUNT}
                    // state, just disconnect it since the user has explicitly started a new call.
                    outgoingCall.disconnect();
                    return true;
                }
                return false;
            }

            if (hasMaximumHoldingCalls()) {
                // There is no more room for any more calls, unless it's an emergency.
                if (isEmergency) {
                    // Kill the current active call, this is easier then trying to disconnect a
                    // holding call and hold an active call.
                    liveCall.disconnect();
                    return true;
                }
                return false;  // No more room!
            }

            // We have room for at least one more holding call at this point.

            // TODO: Remove once b/23035408 has been corrected.
            // If the live call is a conference, it will not have a target phone account set.  This
            // means the check to see if the live call has the same target phone account as the new
            // call will not cause us to bail early.  As a result, we'll end up holding the
            // ongoing conference call.  However, the ConnectionService is already doing that.  This
            // has caused problems with some carriers.  As a workaround until b/23035408 is
            // corrected, we will try and get the target phone account for one of the conference's
            // children and use that instead.
            PhoneAccountHandle liveCallPhoneAccount = liveCall.getTargetPhoneAccount();
            if (liveCallPhoneAccount == null && liveCall.isConference() &&
                    !liveCall.getChildCalls().isEmpty()) {
                liveCallPhoneAccount = getFirstChildPhoneAccount(liveCall);
                Log.i(this, "makeRoomForOutgoingCall: using child call PhoneAccount = " +
                        liveCallPhoneAccount);
            }

            // First thing, if we are trying to make a call with the same phone account as the live
            // call, then allow it so that the connection service can make its own decision about
            // how to handle the new call relative to the current one.
            if (Objects.equals(liveCallPhoneAccount, call.getTargetPhoneAccount())) {
                /// M: ALPS02445100. We should not dial another call when there exists
                /// a dialing cdma call. @{
                if (TelecomUtils.hasCdmaCallCapability(mContext,
                       liveCall.getTargetPhoneAccount()) &&
                       !liveCall.can(Connection.CAPABILITY_HOLD)) {
                    return false;
                }
                /// @}
                Log.i(this, "makeRoomForOutgoingCall: phoneAccount matches.");
                return true;
            } else if (call.getTargetPhoneAccount() == null) {
                // Without a phone account, we can't say reliably that the call will fail.
                // If the user chooses the same phone account as the live call, then it's
                // still possible that the call can be made (like with CDMA calls not supporting
                // hold but they still support adding a call by going immediately into conference
                // mode). Return true here and we'll run this code again after user chooses an
                // account.
                return true;
            }

            // Try to hold the live call before attempting the new outgoing call.
            if (liveCall.can(Connection.CAPABILITY_HOLD)) {
                Log.i(this, "makeRoomForOutgoingCall: holding live call.");
                liveCall.hold();
                return true;
            }

            // The live call cannot be held so we're out of luck here.  There's no room.
            return false;
        }
        return true;
    }

    /**
     * Given a call, find the first non-null phone account handle of its children.
     *
     * @param parentCall The parent call.
     * @return The first non-null phone account handle of the children, or {@code null} if none.
     */
    private PhoneAccountHandle getFirstChildPhoneAccount(Call parentCall) {
        for (Call childCall : parentCall.getChildCalls()) {
            PhoneAccountHandle childPhoneAccount = childCall.getTargetPhoneAccount();
            if (childPhoneAccount != null) {
                return childPhoneAccount;
            }
        }
        return null;
    }

    /**
     * Checks to see if the call should be on speakerphone and if so, set it.
     */
    private void maybeMoveToSpeakerPhone(Call call) {
        if (call.getStartWithSpeakerphoneOn()) {
            setAudioRoute(CallAudioState.ROUTE_SPEAKER);
            call.setStartWithSpeakerphoneOn(false);
        }
    }

    /**
     * Creates a new call for an existing connection.
     *
     * @param callId The id of the new call.
     * @param connection The connection information.
     * @return The new call.
     */
    Call createCallForExistingConnection(String callId, ParcelableConnection connection) {
        Call call = new Call(
                mContext,
                this,
                mLock,
                mConnectionServiceRepository,
                mContactsAsyncHelper,
                mCallerInfoAsyncQueryFactory,
                connection.getHandle() /* handle */,
                null /* gatewayInfo */,
                null /* connectionManagerPhoneAccount */,
                connection.getPhoneAccount(), /* targetPhoneAccountHandle */
                false /* isIncoming */,
                false /* isConference */,
                connection.getConnectTimeMillis() /* connectTimeMillis */);

        setCallState(call, Call.getStateFromConnectionState(connection.getState()),
                "existing connection");
        call.setConnectionCapabilities(connection.getConnectionCapabilities());
        call.setCallerDisplayName(connection.getCallerDisplayName(),
                connection.getCallerDisplayNamePresentation());

        call.addListener(this);
        addCall(call);

        return call;
    }

    /**
     * Dumps the state of the {@link CallsManager}.
     *
     * @param pw The {@code IndentingPrintWriter} to write the state to.
     */
    public void dump(IndentingPrintWriter pw) {
        mContext.enforceCallingOrSelfPermission(android.Manifest.permission.DUMP, TAG);
        if (mCalls != null) {
            pw.println("mCalls: ");
            pw.increaseIndent();
            for (Call call : mCalls) {
                pw.println(call);
            }
            pw.decreaseIndent();
        }
        pw.println("mForegroundCall: " + (mForegroundCall == null ? "none" : mForegroundCall));

        if (mCallAudioManager != null) {
            pw.println("mCallAudioManager:");
            pw.increaseIndent();
            mCallAudioManager.dump(pw);
            pw.decreaseIndent();
        }

        if (mTtyManager != null) {
            pw.println("mTtyManager:");
            pw.increaseIndent();
            mTtyManager.dump(pw);
            pw.decreaseIndent();
        }

        if (mInCallController != null) {
            pw.println("mInCallController:");
            pw.increaseIndent();
            mInCallController.dump(pw);
            pw.decreaseIndent();
        }

        if (mConnectionServiceRepository != null) {
            pw.println("mConnectionServiceRepository:");
            pw.increaseIndent();
            mConnectionServiceRepository.dump(pw);
            pw.decreaseIndent();
        }
    }

    /**
     * Separate one command to two actions. After process the first action, according to the
     * result, continue to handle or cancel the secondary action.
     *
     * @param call: the first action related call.
     */
    public void handleActionProcessComplete(Call call) {
        Log.d(this, "have pending call actions: %s", mPendingCallActions.containsKey(call));
        if (mPendingCallActions.containsKey(call) && (call.getState() == CallState.ON_HOLD
                || call.getState() == CallState.DISCONNECTED)) {
            PendingCallAction pendingAction = removePendingCallAction(call);

            pendingAction.handleActionProcessSuccessful();
        }
    }

    /**
     * Add a new pending call action.
     * @param firstActionCall The first call action will operate on this call
     * @param pendingCall The second call action will operate on this call
     * @param pendingAction
     * @param videoState Only will be used when pending action is answer.
     */
    private void addPendingCallAction(Call firstActionCall, Call pendingCall, String pendingAction,
            int videoState) {
        PendingCallAction pendingCallAction = new PendingCallAction(
                pendingCall,
                pendingAction,
                videoState);
        mPendingCallActions.put(firstActionCall, pendingCallAction);
    }

    /**
     * Remove pending call action from hash map.
     * @param firstActionCall: key for hash map.
     * @return
     */
    private PendingCallAction removePendingCallAction(Call firstActionCall) {
        return (PendingCallAction) mPendingCallActions.remove(firstActionCall);
    }

    /**
     * Keep the info of the secondary pending action of a command.
     */
    private class PendingCallAction {

        public static final String PENDING_ACTION_ANSWER      =  "answer";

        private Call mPendingCall;
        private String mPendingAction;
        private int mVideoState;

        public PendingCallAction(Call call, String action, int videoState) {
            mPendingCall = call;
            mPendingAction = action;
            mVideoState = videoState;
        }

        /**
         * To handle the pending call action after action finished successfully.
         */
        public void handleActionProcessSuccessful() {
            Log.d(this, "pending action = %s, call= %s", mPendingAction, mPendingCall);

            if (mPendingAction.equals(PENDING_ACTION_ANSWER)) {
                for (CallsManagerListener listener : mListeners) {
                    listener.onIncomingCallAnswered(mPendingCall);
                }

                // We do not update the UI until we get confirmation of the answer() through
                // {@link #markCallAsActive}.
                if (mPendingCall.getState() == CallState.RINGING) {

                    // After first action finished, do answer.
                    mPendingCall.answer(mVideoState);
                    if (VideoProfile.isVideo(mVideoState) &&
                        !mWiredHeadsetManager.isPluggedIn() &&
                        !mCallAudioManager.isBluetoothDeviceAvailable() &&
                        isSpeakerEnabledForVideoCalls()) {
                        mPendingCall.setStartWithSpeakerphoneOn(true);
                    }
                }
            }
        }

        public void handleActionProcessFailed() {
            Log.d(this, "handleActionProcessFailed, call= %s", mPendingCall);
        }
    }

    /**
     * Broadcast the connection lost of the call.
     *
     * @param call: the related call.
     */
    void notifyConnectionLost(Call call) {
        Log.d(this, "notifyConnectionLost, call:%s", call);
        for (CallsManagerListener listener : mListeners) {
            listener.onConnectionLost(call);
        }
    }

    /**
     * Clear the pending call action if the first action failed.
     *
     * @param call: the related call.
     */
    void notifyActionFailed(Call call, int action) {
        Log.d(this, "notifyActionFailed, call:%s", call);
        ///M: Set waiting call unanswered if answer action failed @{
        if (call != null && call.getState() == Connection.STATE_RINGING) {
            call.setUnanswered();
        }
        ///@}
        if (mPendingCallActions.containsKey(call)) {
            Log.i(this, "notifyActionFailed, remove pending action");
            PendingCallAction pendingAction = mPendingCallActions.remove(call);
            pendingAction.handleActionProcessFailed();
        }
        SuppMessageHelper suppMessageHelper = new SuppMessageHelper();
        String msg = mContext.getResources()
                .getString(suppMessageHelper.getActionFailedMessageId(action));
        showToastInfomation(msg);
    }

    /**
     * show SS notification.
     *
     * @param call: the related call.
     */
    void notifySSNotificationToast(Call call, int notiType, int type, int code, String number, int index) {
        Log.d(this, "notifySSNotificationToast, call:%s", call);
        String msg = "";
        SuppMessageHelper suppMessageHelper = new SuppMessageHelper();
        if (notiType == 0) {
            msg = suppMessageHelper.getSuppServiceMOString(code, index, number);
        } else if (notiType == 1) {
            String str = "";
            msg = suppMessageHelper.getSuppServiceMTString(code, index);
            if (type == 0x91) {
                if (number != null && number.length() != 0) {
                    str = " +" + number;
                }
            }
            msg = msg + str;
            /// M: For ViLTE @{
            // notify that the call has been held by remote side to UI.
            if (Objects.equals(code, SuppMessageHelper.MT_CODE_CALL_ON_HOLD)) {
                call.setIsHeld(true);
            } else if (Objects.equals(code, SuppMessageHelper.MT_CODE_CALL_RETRIEVED)) {
                call.setIsHeld(false);
            }
            /// @}
        }
        showToastInfomation(msg);
    }

    /**
     * show SS notification.
     *
     * @param call: the related call.
     */
    void notifyNumberUpdate(Call call, String number) {
        Log.d(this, "notifyNumberUpdate, call:%s", call);
        if (number != null && number.length() != 0) {
            Uri handle = Uri.fromParts(PhoneNumberUtils.isUriNumber(number) ?
                    PhoneAccount.SCHEME_SIP : PhoneAccount.SCHEME_TEL, number, null);
            call.setHandle(handle);
        }
    }

    /**
     * update incoming call info..
     *
     * @param call: the related call.
     */
    void notifyIncomingInfoUpdate(Call call, int type, String alphaid, int cli_validity) {
        Log.d(this, "notifyIncomingInfoUpdate, call:%s", call);
        // The definition of "0 / 1 / 2" is in SuppCrssNotification.java
        int handlePresentation = -1;
        switch (cli_validity) {
            case 0:
                handlePresentation = TelecomManager.PRESENTATION_ALLOWED;
                break;
            case 1:
                handlePresentation = TelecomManager.PRESENTATION_RESTRICTED;
                break;
            case 2:
                handlePresentation = TelecomManager.PRESENTATION_UNKNOWN;
                break;
            default:
                break;
        }
        // TODO: For I'm not sure what is stand for handle, SuppCrssNotification.number, or SuppCrssNotification.alphaid?
        // So I do not update handle here. Need confirm with framework, and re-check this part.
        if (handlePresentation != -1 && call != null) {
            call.setHandle(call.getHandle(), handlePresentation);
        }
    }

    void notifyCdmaCallAccepted(Call call) {
        call.setConnectTimeMillis(System.currentTimeMillis());
        Log.d(this, "notifyCdmaCallAccepted, call:%s", call);
        for (CallsManagerListener listener : mListeners) {
            listener.onCdmaCallAccepted(call);
        }
    }

    /// M: Update CanAddCall when capability changed @{
    @Override
    public void onConnectionCapabilitiesChanged(Call call) {
        if(TelecomUtils.hasCdmaCallCapability(mContext, call.getTargetPhoneAccount())) {
            updateCanAddCall();
        }
    }
    /// @}

    /// M: For 3G VT only @{
    /**
     * notify CallAudioManager of the VT status info to set to AudioManager
     *
     * @param call: the related call.
     * @param status: vt status.
     *     - 0: active
     *     - 1: disconnected
     */
    void notifyVtStatusInfo(Call call, int status) {
        Log.d(this, "notifyVtStatusInfo, call:%s", call);
        for (CallsManagerListener listener : mListeners) {
            listener.onVtStatusInfoChanged(call, status);
        }
    }
    /// @}

    public class SuppMessageHelper {
        //action code
        private static final int ACTION_UNKNOWN = 0;
        private static final int ACTION_SWITCH = 1;
        private static final int ACTION_SEPARATE = 2;
        private static final int ACTION_TRANSFER = 3;
        private static final int ACTION_CONFERENCE = 4;
        private static final int ACTION_REJECT = 5;
        private static final int ACTION_HANGUP = 6;

        //MO code
        private static final int MO_CODE_UNCONDITIONAL_CF_ACTIVE = 0;
        private static final int MO_CODE_SOME_CF_ACTIVE = 1;
        private static final int MO_CODE_CALL_FORWARDED = 2;
        private static final int MO_CODE_CALL_IS_WAITING = 3;
        private static final int MO_CODE_CUG_CALL = 4;
        private static final int MO_CODE_OUTGOING_CALLS_BARRED = 5;
        private static final int MO_CODE_INCOMING_CALLS_BARRED = 6;
        private static final int MO_CODE_CLIR_SUPPRESSION_REJECTED = 7;
        private static final int MO_CODE_CALL_DEFLECTED = 8;
        private static final int MO_CODE_CALL_FORWARDED_TO = 9;

        //MT code
        private static final int MT_CODE_FORWARDED_CALL = 0;
        private static final int MT_CODE_CUG_CALL = 1;
        private static final int MT_CODE_CALL_ON_HOLD = 2;
        private static final int MT_CODE_CALL_RETRIEVED = 3;
        private static final int MT_CODE_MULTI_PARTY_CALL = 4;
        private static final int MT_CODE_ON_HOLD_CALL_RELEASED = 5;
        private static final int MT_CODE_FORWARD_CHECK_RECEIVED = 6;
        private static final int MT_CODE_CALL_CONNECTING_ECT = 7;
        private static final int MT_CODE_CALL_CONNECTED_ECT = 8;
        private static final int MT_CODE_DEFLECTED_CALL = 9;
        private static final int MT_CODE_ADDITIONAL_CALL_FORWARDED = 10;
        private static final int MT_CODE_FORWARDED_CF = 11;
        private static final int MT_CODE_FORWARDED_CF_UNCOND = 12;
        private static final int MT_CODE_FORWARDED_CF_COND = 13;
        private static final int MT_CODE_FORWARDED_CF_BUSY = 14;
        private static final int MT_CODE_FORWARDED_CF_NO_REPLY = 15;
        private static final int MT_CODE_FORWARDED_CF_NOT_REACHABLE = 16;

        public int getActionFailedMessageId(int action) {
            int errMsgId = -1;
            switch (action) {
            case ACTION_SWITCH:
                errMsgId = R.string.incall_error_supp_service_switch;
                break;
            case ACTION_SEPARATE:
                errMsgId = R.string.incall_error_supp_service_separate;
                break;
            case ACTION_TRANSFER:
                errMsgId = R.string.incall_error_supp_service_transfer;
                break;
            case ACTION_CONFERENCE:
                errMsgId = R.string.incall_error_supp_service_conference;
                break;
            case ACTION_REJECT:
                errMsgId = R.string.incall_error_supp_service_reject;
                break;
            case ACTION_HANGUP:
                errMsgId = R.string.incall_error_supp_service_hangup;
                break;
            case ACTION_UNKNOWN:
            default:
                errMsgId = R.string.incall_error_supp_service_unknown;
                break;
            }
            return errMsgId;
        }

        public String getSuppServiceMOString(int code, int index, String number) {
            String moStr = "";
            switch (code) {
            case MO_CODE_UNCONDITIONAL_CF_ACTIVE:
                moStr = mContext.getResources()
                        .getString(R.string.mo_code_unconditional_cf_active);
                break;
            case MO_CODE_SOME_CF_ACTIVE:
                moStr = mContext.getResources().getString(R.string.mo_code_some_cf_active);
                break;
            case MO_CODE_CALL_FORWARDED:
                moStr = mContext.getResources().getString(R.string.mo_code_call_forwarded);
                break;
            case MO_CODE_CALL_IS_WAITING:
                moStr = mContext.getResources().getString(R.string.call_waiting_indication);
                break;
            case MO_CODE_CUG_CALL:
                moStr = mContext.getResources().getString(R.string.mo_code_cug_call);
                moStr = moStr + " " + index;
                break;
            case MO_CODE_OUTGOING_CALLS_BARRED:
                moStr = mContext.getResources().getString(R.string.mo_code_outgoing_calls_barred);
                break;
            case MO_CODE_INCOMING_CALLS_BARRED:
                moStr = mContext.getResources().getString(R.string.mo_code_incoming_calls_barred);
                break;
            case MO_CODE_CLIR_SUPPRESSION_REJECTED:
                moStr = mContext.getResources().getString(
                        R.string.mo_code_clir_suppression_rejected);
                break;
            case MO_CODE_CALL_DEFLECTED:
                moStr = mContext.getResources().getString(R.string.mo_code_call_deflected);
                break;
            case MO_CODE_CALL_FORWARDED_TO:
                // here we just show "call forwarding...",
                // and number will be updated via pau later if needed.
                moStr = mContext.getResources().getString(R.string.mo_code_call_forwarding);
                break;
            default:
                // Attempt to use a service we don't recognize or support
                // ("Unsupported service" or "Selected service failed")
                moStr = mContext.getResources().getString(
                        R.string.incall_error_supp_service_unknown);
                break;
            }
            return moStr;
        }

        public String getSuppServiceMTString(int code, int index) {
            String mtStr = "";
            switch (code) {
            case MT_CODE_FORWARDED_CALL:
                mtStr = mContext.getResources().getString(R.string.mt_code_forwarded_call);
                break;
            case MT_CODE_CUG_CALL:
                mtStr = mContext.getResources().getString(R.string.mt_code_cug_call);
                mtStr = mtStr + " " + index;
                break;
            case MT_CODE_CALL_ON_HOLD:
                mtStr = mContext.getResources().getString(R.string.mt_code_call_on_hold);
                break;
            case MT_CODE_CALL_RETRIEVED:
                mtStr = mContext.getResources().getString(R.string.mt_code_call_retrieved);
                break;
            case MT_CODE_MULTI_PARTY_CALL:
                mtStr = mContext.getResources().getString(R.string.mt_code_multi_party_call);
                break;
            case MT_CODE_ON_HOLD_CALL_RELEASED:
                mtStr = mContext.getResources().getString(R.string.mt_code_on_hold_call_released);
                break;
            case MT_CODE_FORWARD_CHECK_RECEIVED:
                mtStr = mContext.getResources().getString(R.string.mt_code_forward_check_received);
                break;
            case MT_CODE_CALL_CONNECTING_ECT:
                mtStr = mContext.getResources().getString(R.string.mt_code_call_connecting_ect);
                break;
            case MT_CODE_CALL_CONNECTED_ECT:
                mtStr = mContext.getResources().getString(R.string.mt_code_call_connected_ect);
                break;
            case MT_CODE_DEFLECTED_CALL:
                mtStr = mContext.getResources().getString(R.string.mt_code_deflected_call);
                break;
            case MT_CODE_ADDITIONAL_CALL_FORWARDED:
                mtStr = mContext.getResources().getString(
                        R.string.mt_code_additional_call_forwarded);
                break;
            case MT_CODE_FORWARDED_CF:
                mtStr = mContext.getResources().getString(R.string.mt_code_forwarded_call) + "("
                        + mContext.getResources().getString(R.string.mt_code_forwarded_cf) + ")";
                break;
            case MT_CODE_FORWARDED_CF_UNCOND:
                mtStr = mContext.getResources().getString(R.string.mt_code_forwarded_call) + "("
                        + mContext.getResources().getString(R.string.mt_code_forwarded_cf_uncond)
                        + ")";
                break;
            case MT_CODE_FORWARDED_CF_COND:
                mtStr = mContext.getResources().getString(R.string.mt_code_forwarded_call) + "("
                        + mContext.getResources().getString(R.string.mt_code_forwarded_cf_cond)
                        + ")";
                break;
            case MT_CODE_FORWARDED_CF_BUSY:
                mtStr = mContext.getResources().getString(R.string.mt_code_forwarded_call) + "("
                        + mContext.getResources().getString(R.string.mt_code_forwarded_cf_busy)
                        + ")";
                break;
            case MT_CODE_FORWARDED_CF_NO_REPLY:
                mtStr = mContext.getResources().getString(R.string.mt_code_forwarded_call) + "("
                        + mContext.getResources().getString(R.string.mt_code_forwarded_cf_no_reply)
                        + ")";
                break;
            case MT_CODE_FORWARDED_CF_NOT_REACHABLE:
                mtStr = mContext.getResources().getString(R.string.mt_code_forwarded_call)
                        + "("
                        + mContext.getResources().getString(
                                R.string.mt_code_forwarded_cf_not_reachable) + ")";
                break;
            default:
                // Attempt to use a service we don't recognize or support
                // ("Unsupported service" or "Selected service failed")
                mtStr = mContext.getResources().getString(
                        R.string.incall_error_supp_service_unknown);
                break;
            }
            return mtStr;
        }
    }

    /**
     * M: Post to main thread to show toast.
     */
    private void showToastInfomation(String msg) {
        mToastInformation = msg;
        Runnable showToast = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, mToastInformation, Toast.LENGTH_SHORT).show();
            }
        };
        mHandler.post(showToast);
    }

    /**
     * M: Whether can record voice for a call
     * @return true if it can, false for not
     */
    public boolean okToRecordVoice(Call call) {
        // Use System.getProperties()
//        if (!FeatureOption.MTK_PHONE_VOICE_RECORDING) {
//            //For dualtalk solution, because of audio's limitation, don't support voice record
//            return retval;
//        }
        if (call.getState() != CallState.ACTIVE) {
            return false;
        }

        PhoneAccountHandle accountHandle = call.getTargetPhoneAccount();
        if (accountHandle != null) {
            ComponentName name = accountHandle.getComponentName();
            if (TelephonyUtil.isPstnComponentName(name)) {
                Log.v(this, "okToRecordVoice isPstnComponentName");
                return true;
            }
        }

        return false;
    }

    /**
     * M: Start voice recording
     */
    void startVoiceRecording() {
        PhoneRecorderHandler.getInstance().startVoiceRecord(
                PhoneRecorderHandler.PHONE_RECORDING_VOICE_CALL_CUSTOM_VALUE);
    }

    /**
     * M: Stop voice recording
     */
    void stopVoiceRecording() {
        PhoneRecorderHandler.getInstance().stopRecording();
    }

    /**
     * M: Power on/off device when connecting to smart book
     */
    void updatePowerForSmartBook(boolean onOff) {
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        Log.d(TAG, "SmartBook power onOff: " + onOff);
        /*TODO  mark smartbook code to let build pass first.
        if (onOff) {
            pm.wakeUpByReason(SystemClock.uptimeMillis(), PowerManager.WAKE_UP_REASON_SMARTBOOK);
        } else {
            pm.goToSleep(SystemClock.uptimeMillis(), PowerManager.GO_TO_SLEEP_REASON_SMARTBOOK, 0);
        }
        */
    }

    boolean neededForceSpeakerOn() {
        boolean result = false;
        Log.i(TAG, "neededForceSpeakerOn");
        if (android.os.SystemProperties.get("ro.mtk_tb_call_speaker_on").equals("1")) {
            Log.i(TAG, "neededForceSpeakerOn, ro.mtk_tb_call_speaker_on == 1");
            if (!mWiredHeadsetManager.isPluggedIn()
                    && !mCallAudioManager.isBluetoothDeviceAvailable()) {
                Log.i(TAG, "neededForceSpeakerOn, ro.mtk_tb_call_speaker_on == 1 && no bt!");
                if (mCallAudioManager.getCallAudioState().getRoute()
                                      != CallAudioState.ROUTE_SPEAKER) {
                    result = true;
                    Log.i(TAG, "neededForceSpeakerOn, set route to speaker");
                }
            }
        }
        return result;
    }

    /**
     * M: [ALPS01752136]set the foreground priority for non-active Calls.
     * @param previousCall the Active Call or null.
     * @return new foreground call or null.
     */
    private Call pickForegroundCallEx(Call previousCall) {
        if (previousCall != null) {
            return previousCall;
        }

        Call ringingCall = null;
        Call aliveCall = null;
        for (Call call : mCalls) {
            if (call.getParentCall() != null) {
                continue;
            }

            if (call.getState() == CallState.RINGING) {
                /// FIXME: M: we can't distinguish multiple ringing calls
                ringingCall = call;
                break;
            }
            if (call.isAlive()) {
                /// FIXME: M: we can't distinguish multiple alive calls
                aliveCall = call;
            }
        }
        return ringingCall != null ? ringingCall : aliveCall;
    }

    /**
     * M: Handle explicit call transfer.
     */
    void explicitCallTransfer(Call call) {
        if (call != null) {
            final ConnectionServiceWrapper service = call.getConnectionService();
            service.explicitCallTransfer(call);
        } else {
            Log.w(this, "explicitCallTransfer failed, call is null");
        }
    }

    /**
     * M: Instructs Telecom to hang up all calls.
     */
    public void hangupAll() {
        Log.v(this, "hangupAll");

        for (Call call : mCalls) {
            if (call.getParentCall() != null) {
                continue;
            }
            call.hangupAll();
        }
    }

    /**
     * M: Instructs Telecom to disconnect all ON_HOLD calls.
     */
    public void hangupAllHoldCalls() {
        Log.v(this, "hangupAllHoldCalls");

        for (Call call : mCalls) {
            if (call.getParentCall() != null) {
                continue;
            }
            if (call.getState() == CallState.ON_HOLD) {
                disconnectCall(call);
            }
        }
    }

    /**
     * M: Instructs Telecom to disconnect active call and answer waiting call.
     */
    public void hangupActiveAndAnswerWaiting() {
        Log.v(this, "hangupActiveAndAnswerWaiting");
        Call ringingCall = getRingingCall();
        if (!mCalls.contains(ringingCall)) {
            Log.i(this, "Request to answer a non-existent call %s", ringingCall);
            return;
        }
        if (mForegroundCall != null && mForegroundCall.isActive()) {
            mPendingCallActions.put(mForegroundCall, new PendingCallAction(ringingCall,
                    PendingCallAction.PENDING_ACTION_ANSWER, ringingCall.getVideoState()));

            mForegroundCall.disconnect();
        }
    }

    /**
     * M: [ALPS01798317]: judge whether all calls are ringing call
     * @return true: all calls are ringing.
     */
    public boolean isAllCallRinging() {
        for (Call call : mCalls) {
            if (call.getState() != CallState.RINGING) {
                return false;
            }
        }

        return true;
    }

    /**
     * M: Help to check whether have pending ecc.
     * @return
     */
    public boolean hasPendingEcc() {
        return mHasPendingECC;
    }

    // expose API of isPotentialInCallMMICode() and isPotentialInCallMMICode() to other package.
    public boolean isPotentialMMIOrInCallMMI(Uri handle) {
        return isPotentialMMICode(handle) || isPotentialInCallMMICode(handle);
    }
    /// @}

    /// M: Update voice record capability. ALPS02026591 @{
    // For conference call, at first time, the Account will be null,
    // so check the CAPABILITY_VOICE_RECORD when the account changed.
    @Override
    public void onTargetPhoneAccountChanged(Call call) {
        Log.d(this, "onTargetPhoneAccountChanged()...");
        int capability = call.getConnectionCapabilities();
        if (okToRecordVoice(call) && !call.can(Connection.CAPABILITY_VOICE_RECORD)) {
            call.setConnectionCapabilities(capability | Connection.CAPABILITY_VOICE_RECORD);
        }
    }
    /// @}

    /// M: Add for 3G VT only @{
    /**
     * Here judge whether can accept a new call(MO / MT) based on logic of that
     * video call can not co-exist with any other call. Eg, 3G VT.
     * @param isVideoRequest
     * @return
     */
    public boolean shouldBlockFor3GVT(boolean isVideoRequest) {
        boolean result = false;
        // The logic is only for 3G VT.
        if (TelecomUtils.isSupport3GVT()) {
            if (isVideoRequest) {
                // if new call is video call, check if exist any valid call.
                for (Call call : mCalls) {
                    if (call.getState() != CallState.SELECT_PHONE_ACCOUNT) {
                        result = true;
                        break;
                    }
                }
            } else {
                // if new call is voice call, check if exist any valid video call.
                for (Call call : mCalls) {
                    if (call.getState() != CallState.SELECT_PHONE_ACCOUNT
                            && VideoProfile.isVideo(call.getVideoState())) {
                        result = true;
                        break;
                    }
                }
            }
        }
        return result;
    }
    /// @}

    /**
     * Check whether we should block the ViLTE request(MO/MT),
     * if exist any video call, block any request on same SIM;
     * if exist any non-video call, block any video request on same SIM.
     * TODO: should combine with 3G VT check?
     * @param newCall
     * @return
     */
    public boolean shouldBlockForCertainViLTE(Call newCall) {
        boolean result = false;
        PhoneAccountHandle accountHandle = newCall.getTargetPhoneAccount();
        boolean isFeatureEnabled = TelecomUtils.isFeatureEnabled(mContext, accountHandle,
                FeatureType.ViLTE_BLOCK_NEW_CALL);
        Log.d(this, "newCall / accountHandle / isFeatureEnabled = %s / %s / %s",
                newCall, accountHandle, isFeatureEnabled);
        if (isFeatureEnabled && newCall != null && accountHandle != null) {
            if (newCall.isVideoCall()) {
                // if new call is video call, check if exist any valid call on same SIM.
                for (Call call : mCalls) {
                    if (call.getState() != CallState.SELECT_PHONE_ACCOUNT
                            && Objects.equals(accountHandle, call.getTargetPhoneAccount())) {
                        result = true;
                        break;
                    }
                }
            } else {
                // if new call is voice call, check if exist any valid video call on same SIM.
                for (Call call : mCalls) {
                    if (call.getState() != CallState.SELECT_PHONE_ACCOUNT
                            && Objects.equals(accountHandle, call.getTargetPhoneAccount())
                            && VideoProfile.isVideo(call.getVideoState())) {
                        result = true;
                        break;
                    }
                }
            }
        }
        Log.d(this, "shouldBlockForCertainViLTE()...result = %s", result);
        return result;
    }

    /**
     * M: Prevent dialing from another SIM based account if already exist a call on
     * SIM account for Dsds project.
     *
     * @param call
     * @return
     */
    private boolean preventCallFromOtherSimBasedAccountForDsds(Call call) {
        if (!TelephonyManagerEx.getDefault().isInDsdaMode() && call.getTargetPhoneAccount() != null
                && TelephonyUtil.isPstnComponentName(call.getTargetPhoneAccount()
                .getComponentName())) {

            for (Call otherCall : mCalls) {
                if (Objects.equals(otherCall, call)) {
                    continue;
                }
                PhoneAccountHandle otherAccount = otherCall.getTargetPhoneAccount();
                if (otherAccount != null
                        && TelephonyUtil.isPstnComponentName(otherAccount.getComponentName())
                        && !Objects.equals(otherAccount, call.getTargetPhoneAccount())) {
                    Log.d(this, "Need to stop dialing a second call from other sim.");
                    return true;
                }
            }
        }
        return false;
    }

    /// M: ALPS02302619 @{
    /**
     * Get the Ecc call
     * @return Ecc call if exists, or null
     */
    Call getEmergencyCall() {
        for (Call call : mCalls) {
            if (call.isEmergencyCall()) {
                Log.i(this, "get Emergency call: " + call);
                return call;
            }
        }
        return null;
    }
    /// @}
}
