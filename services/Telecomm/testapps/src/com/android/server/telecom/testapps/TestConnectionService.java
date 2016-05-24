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

package com.android.server.telecom.testapps;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.telecom.Conference;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccount;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;

import com.android.server.telecom.testapps.R;

import java.lang.String;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import android.telecom.Connection.VideoProvider;
/**
 * Service which provides fake calls to test the ConnectionService interface.
 * TODO: Rename all classes in the directory to Dummy* (e.g., DummyConnectionService).
 */
public class TestConnectionService extends ConnectionService {
    /**
     * Intent extra used to pass along whether a call is video or audio based on the user's choice
     * in the notification.
     */
    public static final String EXTRA_IS_VIDEO_CALL = "extra_is_video_call";

    public static final String EXTRA_HANDLE = "extra_handle";

    /**
     * Random number generator used to generate phone numbers.
     */
    private Random mRandom = new Random();
    /// M: extension @{
    private int mAngel = 90 ;
    private boolean mCameraClosed = true;
    /// @}
    private final class TestConference extends Conference {

        private final Connection.Listener mConnectionListener = new Connection.Listener() {
            @Override
            public void onDestroyed(Connection c) {
                removeConnection(c);
                if (getConnections().size() == 0) {
                    setDisconnected(new DisconnectCause(DisconnectCause.REMOTE));
                    destroy();
                }
            }
        };

        public TestConference(Connection a, Connection b) {
            super(null);
            setConnectionCapabilities(
                    Connection.CAPABILITY_SUPPORT_HOLD |
                    Connection.CAPABILITY_HOLD |
                    Connection.CAPABILITY_MUTE |
                    Connection.CAPABILITY_MANAGE_CONFERENCE |
                    /// M: extension     @{
                    Connection.CAPABILITY_VOLTE);
                    /// @}
            addConnection(a);
            addConnection(b);

            a.addConnectionListener(mConnectionListener);
            b.addConnectionListener(mConnectionListener);

            a.setConference(this);
            b.setConference(this);

            setActive();
        }

        @Override
        public void onDisconnect() {
            for (Connection c : getConnections()) {
                c.setDisconnected(new DisconnectCause(DisconnectCause.REMOTE));
                c.destroy();
            }
        }

        @Override
        public void onSeparate(Connection connection) {
            if (getConnections().contains(connection)) {
                connection.setConference(null);
                removeConnection(connection);
                connection.removeConnectionListener(mConnectionListener);
            }
        }

        @Override
        public void onHold() {
            for (Connection c : getConnections()) {
                c.setOnHold();
            }
            setOnHold();
        }

        @Override
        public void onUnhold() {
            for (Connection c : getConnections()) {
                c.setActive();
            }
            setActive();
        }

        ///M: extesion @{
        @Override
        public VideoProvider getVideoProvider() {
            return TestConnectionService.this.getVideoProvider();
        }

        @Override
        public int getVideoState() {
            return TestConnectionService.this.getVideoState();
        }
        /// @}
    }

    final class TestConnection extends Connection {
        private final boolean mIsIncoming;

        /** Used to cleanup camera and media when done with connection. */
        private TestVideoProvider mTestVideoCallProvider;

        private BroadcastReceiver mHangupReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                /**
                 * M: extend this receiver. @{
                 * google default:
                 setDisconnected(new DisconnectCause(DisconnectCause.MISSED));
                 */
                this.disconnectCall(intent);
                destroyCall(TestConnection.this);
                destroy();
            }

            /**
             * M: extend the receiver.
             * @param intent
             */
            private void disconnectCall(Intent intent) {
                String action = intent.getAction();
                log("[onReceive]" + action);
                if (TestCallActivity.ACTION_HANGUP_CALLS.equals(action)) {
                    setDisconnected(new DisconnectCause(DisconnectCause.MISSED));
                } else if (CallNotificationReceiver.ACTION_DISCONNECT_ERROR.equals(action)) {
                    setDisconnected(new DisconnectCause(DisconnectCause.ERROR,
                            "Label_test_error", "Description_test_error", "Reason_test_error"));
                } else if (CallNotificationReceiver.ACTION_DISCONNECT_UNKNOWN.equals(action)) {
                    setDisconnected(new DisconnectCause(DisconnectCause.UNKNOWN,
                            "Label_test_unknown", "Description_test_unknown",
                            "Reason_test_unknown"));
                }
            }
        };
        /// M: refactorying ,reuse this for change angle function
        private BroadcastReceiver mRemoteOperateRequestReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int request = Integer.parseInt(intent.getData().getSchemeSpecificPart());
                if (request != Integer.parseInt(TestCallActivity.CHANGE_ANGLE_ITEM)) {
                    if (request == 1) {
                        request = mCameraClosed ?
                                (getVideoState() & ~VideoProfile.STATE_RX_ENABLED)
                                : (getVideoState() | VideoProfile.STATE_RX_ENABLED);
                        mCameraClosed = !mCameraClosed;
                    }
                    final VideoProfile videoProfile = new VideoProfile(request);
                    mTestVideoCallProvider.receiveSessionModifyRequest(videoProfile);
                    setVideoState(request);
                } else {
                    mTestVideoCallProvider.changePeerDimensionsWithAngle(720, 1280, mAngel);
                    mAngel += 90;
                }
            }
        };
        /// @}

        TestConnection(boolean isIncoming) {
            mIsIncoming = isIncoming;
            // Assume all calls are video capable.
            int capabilities = getConnectionCapabilities();
            capabilities |= CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL;
            capabilities |= CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL;
            capabilities |= CAPABILITY_CAN_UPGRADE_TO_VIDEO;
            capabilities |= CAPABILITY_MUTE;
            capabilities |= CAPABILITY_SUPPORT_HOLD;
            capabilities |= CAPABILITY_HOLD;
            capabilities |= CAPABILITY_RESPOND_VIA_TEXT;
            ///M:add volte capability @{
            capabilities |= CAPABILITY_VOLTE;
            /// @}
            setConnectionCapabilities(capabilities);

            /**
             * M: extend the receiver. @{
             */
            final IntentFilter hangupFilter = new IntentFilter();
            hangupFilter.addAction(TestCallActivity.ACTION_HANGUP_CALLS);
            hangupFilter.addAction(CallNotificationReceiver.ACTION_DISCONNECT_ERROR);
            hangupFilter.addAction(CallNotificationReceiver.ACTION_DISCONNECT_UNKNOWN);
            /** @} */
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                    mHangupReceiver, hangupFilter);
            /// M:reuse mRemoteRequestReceiver for change angle
            final IntentFilter filter =
                    new IntentFilter();
            filter.addAction(TestCallActivity.ACTION_SEND_UPGRADE_REQUEST);
            filter.addAction(TestCallActivity.ACTION_CHANGE_ANGLE);
            filter.addDataScheme("int");

            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                    mRemoteOperateRequestReceiver, filter);
            /// @}
        }

        void startOutgoing() {
            setDialing();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setActive();
                    activateCall(TestConnection.this);
                }
             /// M: wait longer for observing the Dialing UI.
            }, 10000);
        }

        /** ${inheritDoc} */
        @Override
        public void onAbort() {
            destroyCall(this);
            destroy();
        }

        /** ${inheritDoc} */
        @Override
        public void onAnswer(int videoState) {
            setVideoState(videoState);
            activateCall(this);
            setActive();
            updateConferenceable();
        }

        /** ${inheritDoc} */
        @Override
        public void onPlayDtmfTone(char c) {
            if (c == '1') {
                setDialing();
            }
        }

        /** ${inheritDoc} */
        @Override
        public void onStopDtmfTone() { }

        /** ${inheritDoc} */
        @Override
        public void onDisconnect() {
            setDisconnected(new DisconnectCause(DisconnectCause.REMOTE));
            destroyCall(this);
            destroy();
        }

        /** ${inheritDoc} */
        @Override
        public void onHold() {
            //M: when hold, the video can only send @{
            if (getVideoState() == VideoProfile.STATE_BIDIRECTIONAL) {
                setVideoState(VideoProfile.STATE_TX_ENABLED);
            }
            /// @}
            setOnHold();
        }

        /** ${inheritDoc} */
        @Override
        public void onReject() {
            setDisconnected(new DisconnectCause(DisconnectCause.REJECTED));
            destroyCall(this);
            destroy();
        }

        /** ${inheritDoc} */
        @Override
        public void onUnhold() {
            //M: when hold, the video can only send @{
            if (getVideoState() == VideoProfile.STATE_TX_ENABLED) {
                setVideoState(VideoProfile.STATE_BIDIRECTIONAL);
            }
            /// @}
            setActive();
        }

        public void setTestVideoCallProvider(TestVideoProvider testVideoCallProvider) {
            mTestVideoCallProvider = testVideoCallProvider;
        }

        public void cleanup() {
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                    mHangupReceiver);
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                    mRemoteOperateRequestReceiver);
        }

        /**
         * Stops playback of test videos.
         */
        private void stopAndCleanupMedia() {
            if (mTestVideoCallProvider != null) {
                mTestVideoCallProvider.stopAndCleanupMedia();
                mTestVideoCallProvider.stopCamera();
            }
        }

        /// M: for multiply call @{
        @Override
        public void onSwapWithBackgroundCall() {
            for (Connection c : TestConnectionService.this.getAllConnections()) {
                if (c.getState() == STATE_ACTIVE) {
                    c.setOnHold() ;
                } else if (c.getState() == STATE_HOLDING) {
                    c.setActive();
                }
            }
        }
        /// @}

    }

    private final List<TestConnection> mCalls = new ArrayList<>();
    private final Handler mHandler = new Handler();

    /** Used to play an audio tone during a call. */
    private MediaPlayer mMediaPlayer;

    @Override
    public boolean onUnbind(Intent intent) {
        log("onUnbind");
        mMediaPlayer = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onConference(Connection a, Connection b) {
        //M: when hold, the video can only send @{
        TestConference conference = new TestConference(a, b);
        if (a.getVideoProvider() != null) {
            setVideoProvider(a.getVideoProvider());
            setVideoState(3);
        } else if (b.getVideoProvider() != null) {
            setVideoProvider(b.getVideoProvider());
            setVideoState(3);
        }
        addConference(conference);
        /// @}
    }

    @Override
    public Connection onCreateOutgoingConnection(
            PhoneAccountHandle connectionManagerAccount,
            final ConnectionRequest originalRequest) {

        final Uri handle = originalRequest.getAddress();
        String number = originalRequest.getAddress().getSchemeSpecificPart();
        log("call, number: " + number);

        // Crash on 555-DEAD to test call service crashing.
        if ("5550340".equals(number)) {
            throw new RuntimeException("Goodbye, cruel world.");
        }

        Bundle extras = originalRequest.getExtras();
        String gatewayPackage = extras.getString(TelecomManager.GATEWAY_PROVIDER_PACKAGE);
        Uri originalHandle = extras.getParcelable(TelecomManager.GATEWAY_ORIGINAL_ADDRESS);

        log("gateway package [" + gatewayPackage + "], original handle [" +
                originalHandle + "]");

        final TestConnection connection = new TestConnection(false /* isIncoming */);
        setAddress(connection, handle);

        // If the number starts with 555, then we handle it ourselves. If not, then we
        // use a remote connection service.
        // TODO: Have a special phone number to test the account-picker dialog flow.
        if (number != null && number.startsWith("555")) {
            // Normally we would use the original request as is, but for testing purposes, we are
            // adding ".." to the end of the number to follow its path more easily through the logs.
            final ConnectionRequest request = new ConnectionRequest(
                    originalRequest.getAccountHandle(),
                    Uri.fromParts(handle.getScheme(),
                    handle.getSchemeSpecificPart() + "..", ""),
                    originalRequest.getExtras(),
                    originalRequest.getVideoState());
            connection.setVideoState(originalRequest.getVideoState());
            /// M: only VideoCall addVideoProvider @{
            if (originalRequest.getVideoState() == VideoProfile.STATE_BIDIRECTIONAL) {
                addVideoProvider(connection);
            }
            /// @}

            addCall(connection);
            connection.startOutgoing();

            for (Connection c : getAllConnections()) {
                c.setOnHold();
            }
        } else {
            log("Not a test number");
        }
        return connection;
    }

    @Override
    public Connection onCreateIncomingConnection(
            PhoneAccountHandle connectionManagerAccount,
            final ConnectionRequest request) {
        PhoneAccountHandle accountHandle = request.getAccountHandle();
        ComponentName componentName = new ComponentName(this, TestConnectionService.class);

        if (accountHandle != null && componentName.equals(accountHandle.getComponentName())) {
            final TestConnection connection = new TestConnection(true);
            // Get the stashed intent extra that determines if this is a video call or audio call.
            Bundle extras = request.getExtras();
            boolean isVideoCall = extras.getBoolean(EXTRA_IS_VIDEO_CALL);
            Uri providedHandle = extras.getParcelable(EXTRA_HANDLE);

            // Use dummy number for testing incoming calls.
            Uri address = providedHandle == null ?
                    Uri.fromParts(PhoneAccount.SCHEME_TEL, getDummyNumber(isVideoCall), null)
                    : providedHandle;

            int videoState = isVideoCall ?
                    VideoProfile.STATE_BIDIRECTIONAL :
                    VideoProfile.STATE_AUDIO_ONLY;
            connection.setVideoState(videoState);
            setAddress(connection, address);

            addVideoProvider(connection);

            addCall(connection);

            ConnectionRequest newRequest = new ConnectionRequest(
                    request.getAccountHandle(),
                    address,
                    request.getExtras(),
                    videoState);
            connection.setVideoState(videoState);
            return connection;
        } else {
            return Connection.createFailedConnection(new DisconnectCause(DisconnectCause.ERROR,
                    "Invalid inputs: " + accountHandle + " " + componentName));
        }
    }

    @Override
    public Connection onCreateUnknownConnection(PhoneAccountHandle connectionManagerPhoneAccount,
            final ConnectionRequest request) {
        PhoneAccountHandle accountHandle = request.getAccountHandle();
        ComponentName componentName = new ComponentName(this, TestConnectionService.class);
        if (accountHandle != null && componentName.equals(accountHandle.getComponentName())) {
            final TestConnection connection = new TestConnection(false);
            final Bundle extras = request.getExtras();
            final Uri providedHandle = extras.getParcelable(EXTRA_HANDLE);

            Uri handle = providedHandle == null ?
                    Uri.fromParts(PhoneAccount.SCHEME_TEL, getDummyNumber(false), null)
                    : providedHandle;

            connection.setAddress(handle,  TelecomManager.PRESENTATION_ALLOWED);
            connection.setDialing();

            addCall(connection);
            return connection;
        } else {
            return Connection.createFailedConnection(new DisconnectCause(DisconnectCause.ERROR,
                    "Invalid inputs: " + accountHandle + " " + componentName));
        }
    }

    private void addVideoProvider(TestConnection connection) {
        TestVideoProvider testVideoCallProvider =
                new TestVideoProvider(getApplicationContext(), connection);
        connection.setVideoProvider(testVideoCallProvider);

        // Keep reference to original so we can clean up the media players later.
        connection.setTestVideoCallProvider(testVideoCallProvider);
    }

    private void activateCall(TestConnection connection) {
        if (mMediaPlayer == null) {
            mMediaPlayer = createMediaPlayer();
        }
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
        }
    }

    private void destroyCall(TestConnection connection) {
        connection.cleanup();
        mCalls.remove(connection);
        /// M:restore mAngel @{
        mAngel = 90 ;
        /// @}
        // Ensure any playing media and camera resources are released.
        connection.stopAndCleanupMedia();

        // Stops audio if there are no more calls.
        if (mCalls.isEmpty() && mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = createMediaPlayer();
        }

        updateConferenceable();
    }

    private void addCall(TestConnection connection) {
        mCalls.add(connection);
        updateConferenceable();
    }

    private void updateConferenceable() {
        List<Connection> freeConnections = new ArrayList<>();
        freeConnections.addAll(mCalls);
        for (int i = 0; i < freeConnections.size(); i++) {
            if (freeConnections.get(i).getConference() != null) {
                freeConnections.remove(i);
            }
        }
        for (int i = 0; i < freeConnections.size(); i++) {
            Connection c = freeConnections.remove(i);
            c.setConferenceableConnections(freeConnections);
            freeConnections.add(i, c);
        }
    }

    private void setAddress(Connection connection, Uri address) {
        connection.setAddress(address, TelecomManager.PRESENTATION_ALLOWED);
        if ("5551234".equals(address.getSchemeSpecificPart())) {
            connection.setCallerDisplayName("Hello World", TelecomManager.PRESENTATION_ALLOWED);
        }
    }

    private MediaPlayer createMediaPlayer() {
        // Prepare the media player to play a tone when there is a call.
        MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.beep_boop);
        mediaPlayer.setLooping(true);
        return mediaPlayer;
    }

    private static void log(String msg) {
        Log.w("telecomtestcs", "[TestConnectionService] " + msg);
    }

    /**
     * Generates a random phone number of format 555YXXX.  Where Y will be {@code 1} if the
     * phone number is for a video call and {@code 0} for an audio call.  XXX is a randomly
     * generated phone number.
     *
     * @param isVideo {@code True} if the call is a video call.
     * @return The phone number.
     */
    private String getDummyNumber(boolean isVideo) {
        int videoDigit = isVideo ? 1 : 0;
        int number = mRandom.nextInt(999);
        return String.format("555%s%03d", videoDigit, number);
    }
    ///M: extension @{
    public VideoProvider getVideoProvider() {
        return mVideoProvider;
    }

    public void setVideoProvider(VideoProvider videoProvider) {
        this.mVideoProvider = videoProvider;
    }

    private VideoProvider mVideoProvider = null;

    public int getVideoState() {
        return mVideoState;
    }

    public void setVideoState(int videoState) {
        this.mVideoState = videoState;
    }

    private int mVideoState = VideoProfile.STATE_AUDIO_ONLY;
    ///@}
}

