/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.bluetooth.avrcp;

import java.util.Timer;
import java.util.TimerTask;

import android.app.PendingIntent;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAvrcp;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.IRemoteControlDisplay;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.RemoteController;
import android.media.RemoteController.MetadataEditor;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;

import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.Utils;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.mediatek.bluetooth.avrcp.AvrcpEnhance;
import com.android.bluetooth.bip.BluetoothBipCoverArt;

/**
 * support Bluetooth AVRCP profile.
 * support metadata, play status and event notification
 */
public final class Avrcp {
    private static final boolean DEBUG = true;
    private static final String TAG = "Avrcp";

    private Context mContext;
    private final AudioManager mAudioManager;
    private AvrcpMessageHandler mHandler;
    private RemoteController mRemoteController;
    private RemoteControllerWeak mRemoteControllerCb;
    private Metadata mMetadata;
    private int mTransportControlFlags;
    private int mCurrentPlayState;
    private int mPlayStatusChangedNT;
    private int mTrackChangedNT;
    private long mTrackNumber;
    private long mCurrentPosMs;
    private long mPlayStartTimeMs;
    private long mSongLengthMs;
    private long mPlaybackIntervalMs;
    private int mPlayPosChangedNT;
    private long mNextPosMs;
    private long mPrevPosMs;
    private long mSkipStartTime;
    private int mFeatures;
    private int mAbsoluteVolume;
    private int mLastSetVolume;
    private int mLastDirection;
    private final int mVolumeStep;
    private final int mAudioStreamMax;
    private boolean mVolCmdInProgress;
    private int mAbsVolRetryTimes;
    private int mSkipAmount;

    /* BTRC features */
    public static final int BTRC_FEAT_METADATA = 0x01;
    public static final int BTRC_FEAT_ABSOLUTE_VOLUME = 0x02;
    public static final int BTRC_FEAT_BROWSE = 0x04;

    /* AVRC response codes, from avrc_defs */
    private static final int AVRC_RSP_NOT_IMPL = 8;
    private static final int AVRC_RSP_ACCEPT = 9;
    private static final int AVRC_RSP_REJ = 10;
    private static final int AVRC_RSP_IN_TRANS = 11;
    private static final int AVRC_RSP_IMPL_STBL = 12;
    private static final int AVRC_RSP_CHANGED = 13;
    private static final int AVRC_RSP_INTERIM = 15;

    private static final int MESSAGE_GET_RC_FEATURES = 1;
    private static final int MESSAGE_GET_PLAY_STATUS = 2;
    private static final int MESSAGE_GET_ELEM_ATTRS = 3;
    private static final int MESSAGE_REGISTER_NOTIFICATION = 4;
    private static final int MESSAGE_PLAY_INTERVAL_TIMEOUT = 5;
    private static final int MESSAGE_VOLUME_CHANGED = 6;
    private static final int MESSAGE_ADJUST_VOLUME = 7;
    private static final int MESSAGE_SET_ABSOLUTE_VOLUME = 8;
    private static final int MESSAGE_ABS_VOL_TIMEOUT = 9;
    private static final int MESSAGE_FAST_FORWARD = 10;
    private static final int MESSAGE_REWIND = 11;
    private static final int MESSAGE_CHANGE_PLAY_POS = 12;
    private static final int MESSAGE_SET_A2DP_AUDIO_STATE = 13;
    private static final int MSG_UPDATE_STATE = 100;
    private static final int MSG_SET_METADATA = 101;
    private static final int MSG_SET_TRANSPORT_CONTROLS = 102;
    private static final int MSG_SET_GENERATION_ID = 104;

    private static final int BUTTON_TIMEOUT_TIME = 2000;
    private static final int BASE_SKIP_AMOUNT = 2000;
    private static final int KEY_STATE_PRESS = 1;
    private static final int KEY_STATE_RELEASE = 0;
    private static final int SKIP_PERIOD = 400;
    private static final int SKIP_DOUBLE_INTERVAL = 3000;
    private static final long MAX_MULTIPLIER_VALUE = 128L;
    private static final int CMD_TIMEOUT_DELAY = 2000;
    private static final int MAX_ERROR_RETRY_TIMES = 3;
    private static final int AVRCP_MAX_VOL = 127;
    private static final int AVRCP_BASE_VOLUME_STEP = 1;

    static {
        classInitNative();
    }

    private Avrcp(Context context) {
        mMetadata = new Metadata();
        mCurrentPlayState = RemoteControlClient.PLAYSTATE_NONE; // until we get a callback
        mPlayStatusChangedNT = NOTIFICATION_TYPE_CHANGED;
        mTrackChangedNT = NOTIFICATION_TYPE_CHANGED;
        mTrackNumber = -1L;
        mCurrentPosMs = 0L;
        mPlayStartTimeMs = -1L;
        mSongLengthMs = 0L;
        mPlaybackIntervalMs = 0L;
        mPlayPosChangedNT = NOTIFICATION_TYPE_CHANGED;
        mFeatures = 0;
        mAbsoluteVolume = -1;
        mLastSetVolume = -1;
        mLastDirection = 0;
        mVolCmdInProgress = false;
        mAbsVolRetryTimes = 0;

        mContext = context;

        initNative();

        /** M: Add for Media Player Selection Feature*/
        mEnhancedAvrcp = new AvrcpEnhance(mContext,
                                         new AvrcpEnhance.MediaPlayerStatusChangeCallback() {
                public void onPlayerStatusReject() {
                    sendRegisterNotificationPlayerStatusReject();
                }
            });

        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mAudioStreamMax = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mVolumeStep = Math.max(AVRCP_BASE_VOLUME_STEP, AVRCP_MAX_VOL/mAudioStreamMax);
    }

    private void start() {
        HandlerThread thread = new HandlerThread("BluetoothAvrcpHandler");
        thread.start();
        Looper looper = thread.getLooper();
        mHandler = new AvrcpMessageHandler(looper);
        mRemoteControllerCb = new RemoteControllerWeak(mHandler);
        mRemoteController = new RemoteController(mContext, mRemoteControllerCb);
        mAudioManager.registerRemoteController(mRemoteController);
        mRemoteController.setSynchronizationMode(RemoteController.POSITION_SYNCHRONIZATION_CHECK);

        /** M: Add for Media Player Selection Feature*/
        mEnhancedAvrcp.start(mHandler);
    }

    public static Avrcp make(Context context) {
        if (DEBUG) Log.d(TAG, "make");
        Avrcp ar = new Avrcp(context);
        ar.start();
        return ar;
    }

    public void doQuit() {
        mHandler.removeCallbacksAndMessages(null);
        Looper looper = mHandler.getLooper();
        if (looper != null) {
            looper.quit();
        }
        mAudioManager.unregisterRemoteController(mRemoteController);

        /** M: Add for Media Player Selection Feature*/
        if (mEnhancedAvrcp != null) {
            mEnhancedAvrcp.stop();
        }
    }

    public void cleanup() {
        /** M: Add for Media Player Selection Feature*/
        if (mEnhancedAvrcp != null) {
            mEnhancedAvrcp.cleanup();
        }
        cleanupNative();
    }

    private static class RemoteControllerWeak implements RemoteController.OnClientUpdateListener {
        private final WeakReference<Handler> mLocalHandler;

        public RemoteControllerWeak(Handler handler) {
            mLocalHandler = new WeakReference<Handler>(handler);
        }

        @Override
        public void onClientChange(boolean clearing) {
            Handler handler = mLocalHandler.get();
            if (handler != null) {
                handler.obtainMessage(MSG_SET_GENERATION_ID,
                        0, (clearing ? 1 : 0), null).sendToTarget();
            }
        }

        @Override
        public void onClientPlaybackStateUpdate(int state) {
            // Should never be called with the existing code, but just in case
            Handler handler = mLocalHandler.get();
            if (handler != null) {
                handler.obtainMessage(MSG_UPDATE_STATE, 0, state,
                        new Long(RemoteControlClient.PLAYBACK_POSITION_INVALID)).sendToTarget();
            }
        }

        @Override
        public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs,
                long currentPosMs, float speed) {
            Handler handler = mLocalHandler.get();
            if (handler != null) {
                handler.obtainMessage(MSG_UPDATE_STATE, 0, state,
                        new Long(currentPosMs)).sendToTarget();
            }
        }

        @Override
        public void onClientTransportControlUpdate(int transportControlFlags) {
            Handler handler = mLocalHandler.get();
            if (handler != null) {
                handler.obtainMessage(MSG_SET_TRANSPORT_CONTROLS, 0, transportControlFlags)
                        .sendToTarget();
            }
        }

        @Override
        public void onClientMetadataUpdate(MetadataEditor metadataEditor) {
            Handler handler = mLocalHandler.get();
            if (handler != null) {
                handler.obtainMessage(MSG_SET_METADATA, 0, 0, metadataEditor).sendToTarget();
            }
        }
    }

    /** Handles Avrcp messages. */
    private final class AvrcpMessageHandler extends Handler {
        private AvrcpMessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_UPDATE_STATE:
                    if (DEBUG) Log.d(TAG, "MSG_UPDATE_STATE:" + msg.arg2);
                    updatePlayPauseState(msg.arg2, ((Long) msg.obj).longValue());
                break;

            case MSG_SET_METADATA:
                    updateMetadata((MetadataEditor) msg.obj);
                break;

            case MSG_SET_TRANSPORT_CONTROLS:
                    updateTransportControls(msg.arg2);
                break;

            case MSG_SET_GENERATION_ID:
                if (DEBUG) Log.d(TAG, "New genId = " + msg.arg1 + ", clearing = " + msg.arg2);

                /** M: Add for Media Player Selection Feature*/
                if (mEnhancedAvrcp != null) {
                    mEnhancedAvrcp.setPlayerChangeFlag(msg.arg2);
                }
                break;

            case MESSAGE_GET_RC_FEATURES:
                String address = (String) msg.obj;
                if (DEBUG) Log.d(TAG, "MESSAGE_GET_RC_FEATURES: address=" + address +
                                                             ", features="+msg.arg1);
                mFeatures = msg.arg1;
                mAudioManager.avrcpSupportsAbsoluteVolume(address, isAbsoluteVolumeSupported());
                break;

            case MESSAGE_GET_PLAY_STATUS:
                if (DEBUG) Log.d(TAG, "MESSAGE_GET_PLAY_STATUS");
                getPlayStatusRspNative(convertPlayStateToPlayStatus(mCurrentPlayState),
                                       (int)mSongLengthMs, (int)getPlayPosition());
                break;

            case MESSAGE_GET_ELEM_ATTRS:
            {
                String[] textArray;
                int[] attrIds;
                byte numAttr = (byte) msg.arg1;
                ArrayList<Integer> attrList = (ArrayList<Integer>) msg.obj;
                if (DEBUG) Log.d(TAG, "MESSAGE_GET_ELEM_ATTRS:numAttr=" + numAttr);
                attrIds = new int[numAttr];
                textArray = new String[numAttr];
                for (int i = 0; i < numAttr; ++i) {
                    attrIds[i] = attrList.get(i).intValue();
                    textArray[i] = getAttributeString(attrIds[i]);
                }
                getElementAttrRspNative(numAttr, attrIds, textArray);
                break;
            }
            case MESSAGE_REGISTER_NOTIFICATION:
                if (DEBUG) Log.d(TAG, "MESSAGE_REGISTER_NOTIFICATION:event=" + msg.arg1 +
                                      " param=" + msg.arg2);
                processRegisterNotification(msg.arg1, msg.arg2);
                break;

            case MESSAGE_PLAY_INTERVAL_TIMEOUT:
                if (DEBUG) Log.d(TAG, "MESSAGE_PLAY_INTERVAL_TIMEOUT");
                mPlayPosChangedNT = NOTIFICATION_TYPE_CHANGED;
                registerNotificationRspPlayPosNative(mPlayPosChangedNT, (int)getPlayPosition());
                break;

            case MESSAGE_VOLUME_CHANGED:
                if (DEBUG) Log.d(TAG, "MESSAGE_VOLUME_CHANGED: volume=" + ((byte) msg.arg1 & 0x7f)
                                                        + " ctype=" + msg.arg2);

                if (msg.arg2 == AVRC_RSP_ACCEPT || msg.arg2 == AVRC_RSP_REJ) {
                    if (mVolCmdInProgress == false) {
                        Log.e(TAG, "Unsolicited response, ignored");
                        break;
                    }
                    removeMessages(MESSAGE_ABS_VOL_TIMEOUT);
                    mVolCmdInProgress = false;
                    mAbsVolRetryTimes = 0;
                }
                if (mAbsoluteVolume != msg.arg1 && (msg.arg2 == AVRC_RSP_ACCEPT ||
                                                    msg.arg2 == AVRC_RSP_CHANGED ||
                                                    msg.arg2 == AVRC_RSP_INTERIM)) {
                    byte absVol = (byte)((byte)msg.arg1 & 0x7f); // discard MSB as it is RFD
                    notifyVolumeChanged(absVol);
                    mAbsoluteVolume = absVol;
                    long pecentVolChanged = ((long)absVol * 100) / 0x7f;
                    Log.e(TAG, "percent volume changed: " + pecentVolChanged + "%");
                } else if (msg.arg2 == AVRC_RSP_REJ) {
                    Log.e(TAG, "setAbsoluteVolume call rejected");
                }
                break;

            case MESSAGE_ADJUST_VOLUME:
                if (DEBUG) Log.d(TAG, "MESSAGE_ADJUST_VOLUME: direction=" + msg.arg1);
                if (mVolCmdInProgress) {
                    if (DEBUG) Log.w(TAG, "There is already a volume command in progress.");
                    break;
                }
                // Wait on verification on volume from device, before changing the volume.
                if (mAbsoluteVolume != -1 && (msg.arg1 == -1 || msg.arg1 == 1)) {
                    int setVol = Math.min(AVRCP_MAX_VOL,
                                 Math.max(0, mAbsoluteVolume + msg.arg1*mVolumeStep));
                    if (setVolumeNative(setVol)) {
                        sendMessageDelayed(obtainMessage(MESSAGE_ABS_VOL_TIMEOUT),
                                           CMD_TIMEOUT_DELAY);
                        mVolCmdInProgress = true;
                        mLastDirection = msg.arg1;
                        mLastSetVolume = setVol;
                    }
                } else {
                    Log.e(TAG, "Unknown direction in MESSAGE_ADJUST_VOLUME");
                }
                break;

            case MESSAGE_SET_ABSOLUTE_VOLUME:
                if (DEBUG) Log.d(TAG, "MESSAGE_SET_ABSOLUTE_VOLUME");
                if (mVolCmdInProgress) {
                    if (DEBUG) Log.w(TAG, "There is already a volume command in progress.");
                    break;
                }
                if (setVolumeNative(msg.arg1)) {
                    sendMessageDelayed(obtainMessage(MESSAGE_ABS_VOL_TIMEOUT), CMD_TIMEOUT_DELAY);
                    mVolCmdInProgress = true;
                    mLastSetVolume = msg.arg1;
                }
                break;

            case MESSAGE_ABS_VOL_TIMEOUT:
                if (DEBUG) Log.d(TAG, "MESSAGE_ABS_VOL_TIMEOUT: Volume change cmd timed out.");
                mVolCmdInProgress = false;
                if (mAbsVolRetryTimes >= MAX_ERROR_RETRY_TIMES) {
                    mAbsVolRetryTimes = 0;
                } else {
                    mAbsVolRetryTimes += 1;
                    if (setVolumeNative(mLastSetVolume)) {
                        sendMessageDelayed(obtainMessage(MESSAGE_ABS_VOL_TIMEOUT),
                                           CMD_TIMEOUT_DELAY);
                        mVolCmdInProgress = true;
                    }
                }
                break;

            case MESSAGE_FAST_FORWARD:
            case MESSAGE_REWIND:
                if(msg.what == MESSAGE_FAST_FORWARD) {
                    if((mTransportControlFlags &
                        RemoteControlClient.FLAG_KEY_MEDIA_FAST_FORWARD) != 0) {
                    int keyState = msg.arg1 == KEY_STATE_PRESS ?
                        KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP;
                    KeyEvent keyEvent =
                        new KeyEvent(keyState, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
                    mRemoteController.sendMediaKeyEvent(keyEvent);
                    break;
                    }
                } else if((mTransportControlFlags &
                        RemoteControlClient.FLAG_KEY_MEDIA_REWIND) != 0) {
                    int keyState = msg.arg1 == KEY_STATE_PRESS ?
                        KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP;
                    KeyEvent keyEvent =
                        new KeyEvent(keyState, KeyEvent.KEYCODE_MEDIA_REWIND);
                    mRemoteController.sendMediaKeyEvent(keyEvent);
                    break;
                }

                int skipAmount;
                if (msg.what == MESSAGE_FAST_FORWARD) {
                    if (DEBUG) Log.d(TAG, "MESSAGE_FAST_FORWARD");
                    removeMessages(MESSAGE_FAST_FORWARD);
                    skipAmount = BASE_SKIP_AMOUNT;
                } else {
                    if (DEBUG) Log.d(TAG, "MESSAGE_REWIND");
                    removeMessages(MESSAGE_REWIND);
                    skipAmount = -BASE_SKIP_AMOUNT;
                }

                if (hasMessages(MESSAGE_CHANGE_PLAY_POS) &&
                        (skipAmount != mSkipAmount)) {
                    Log.w(TAG, "missing release button event:" + mSkipAmount);
                }

                if ((!hasMessages(MESSAGE_CHANGE_PLAY_POS)) ||
                        (skipAmount != mSkipAmount)) {
                    mSkipStartTime = SystemClock.elapsedRealtime();
                }

                removeMessages(MESSAGE_CHANGE_PLAY_POS);
                if (msg.arg1 == KEY_STATE_PRESS) {
                    mSkipAmount = skipAmount;
                    changePositionBy(mSkipAmount * getSkipMultiplier());
                    Message posMsg = obtainMessage(MESSAGE_CHANGE_PLAY_POS);
                    posMsg.arg1 = 1;
                    sendMessageDelayed(posMsg, SKIP_PERIOD);
                }

                break;

            case MESSAGE_CHANGE_PLAY_POS:
                if (DEBUG) Log.d(TAG, "MESSAGE_CHANGE_PLAY_POS:" + msg.arg1);
                changePositionBy(mSkipAmount * getSkipMultiplier());
                if (msg.arg1 * SKIP_PERIOD < BUTTON_TIMEOUT_TIME) {
                    Message posMsg = obtainMessage(MESSAGE_CHANGE_PLAY_POS);
                    posMsg.arg1 = msg.arg1 + 1;
                    sendMessageDelayed(posMsg, SKIP_PERIOD);
                }
                break;

            case MESSAGE_SET_A2DP_AUDIO_STATE:
                if (DEBUG) Log.d(TAG, "MESSAGE_SET_A2DP_AUDIO_STATE:" + msg.arg1);
                updateA2dpAudioState(msg.arg1);
                break;
            }
            // M: Add for AVRCP1.5 MediaPlayer Selection
            if (mEnhancedAvrcp != null) {
                mEnhancedAvrcp.handleMessage(msg);
            }
        }
    }

    private void updateA2dpAudioState(int state) {
        boolean isPlaying = (state == BluetoothA2dp.STATE_PLAYING);
        if (DEBUG) Log.d(TAG, "updateA2dpAudioState isPlaying:" + isPlaying + "  isPlayingState= " + isPlayingState(mCurrentPlayState));
        if (isPlaying != isPlayingState(mCurrentPlayState)) {
            /* if a2dp is streaming, check to make sure music is active */
            if ( (isPlaying) && !mAudioManager.isMusicActive())
                return;
            updatePlayPauseState(isPlaying ? RemoteControlClient.PLAYSTATE_PLAYING :
                                 RemoteControlClient.PLAYSTATE_PAUSED,
                                 RemoteControlClient.PLAYBACK_POSITION_INVALID);
        }
    }

    private void updatePlayPauseState(int state, long currentPosMs) {
        if (DEBUG) Log.d(TAG,
                "updatePlayPauseState, old=" + mCurrentPlayState + ", state=" + state);
        boolean oldPosValid = (mCurrentPosMs !=
                               RemoteControlClient.PLAYBACK_POSITION_ALWAYS_UNKNOWN);
        int oldPlayStatus = convertPlayStateToPlayStatus(mCurrentPlayState);
        int newPlayStatus = convertPlayStateToPlayStatus(state);

        if ((mCurrentPlayState == RemoteControlClient.PLAYSTATE_PLAYING) &&
            (mCurrentPlayState != state) && oldPosValid) {
            mCurrentPosMs = getPlayPosition();
        }

        if (currentPosMs != RemoteControlClient.PLAYBACK_POSITION_INVALID) {
            mCurrentPosMs = currentPosMs;
        }
        if ((state == RemoteControlClient.PLAYSTATE_PLAYING) &&
            ((currentPosMs != RemoteControlClient.PLAYBACK_POSITION_INVALID) ||
            (mCurrentPlayState != RemoteControlClient.PLAYSTATE_PLAYING))) {
            mPlayStartTimeMs = SystemClock.elapsedRealtime();
        }
        mCurrentPlayState = state;

        boolean newPosValid = (mCurrentPosMs !=
                               RemoteControlClient.PLAYBACK_POSITION_ALWAYS_UNKNOWN);
        long playPosition = getPlayPosition();
        mHandler.removeMessages(MESSAGE_PLAY_INTERVAL_TIMEOUT);
        /* need send play position changed notification when play status is changed */
        if ((mPlayPosChangedNT == NOTIFICATION_TYPE_INTERIM) &&
            ((oldPlayStatus != newPlayStatus) || (oldPosValid != newPosValid) ||
             (newPosValid && ((playPosition >= mNextPosMs) || (playPosition <= mPrevPosMs))))) {
            mPlayPosChangedNT = NOTIFICATION_TYPE_CHANGED;
            registerNotificationRspPlayPosNative(mPlayPosChangedNT, (int)playPosition);
        }
        if ((mPlayPosChangedNT == NOTIFICATION_TYPE_INTERIM) && newPosValid &&
            (state == RemoteControlClient.PLAYSTATE_PLAYING)) {
            Message msg = mHandler.obtainMessage(MESSAGE_PLAY_INTERVAL_TIMEOUT);
            mHandler.sendMessageDelayed(msg, mNextPosMs - playPosition);
        }

        if ((mPlayStatusChangedNT == NOTIFICATION_TYPE_INTERIM) && (oldPlayStatus != newPlayStatus)) {
            mPlayStatusChangedNT = NOTIFICATION_TYPE_CHANGED;
            registerNotificationRspPlayStatusNative(mPlayStatusChangedNT, newPlayStatus);
        }
    }

    private void updateTransportControls(int transportControlFlags) {
        mTransportControlFlags = transportControlFlags;
    }

    class Metadata {
        private String artist;
        private String trackTitle;
        private String albumTitle;

        public Metadata() {
            artist = null;
            trackTitle = null;
            albumTitle = null;
        }

        public String toString() {
            return "Metadata[artist=" + artist + " trackTitle=" + trackTitle + " albumTitle=" +
                   albumTitle + "]";
        }
    }

    private void updateMetadata(MetadataEditor data) {
        String oldMetadata = mMetadata.toString();
        mMetadata.artist = data.getString(MediaMetadataRetriever.METADATA_KEY_ARTIST, null);
        mMetadata.trackTitle = data.getString(MediaMetadataRetriever.METADATA_KEY_TITLE, null);
        mMetadata.albumTitle = data.getString(MediaMetadataRetriever.METADATA_KEY_ALBUM, null);
        if (!oldMetadata.equals(mMetadata.toString())) {
            mTrackNumber++;
            if (mTrackChangedNT == NOTIFICATION_TYPE_INTERIM) {
                mTrackChangedNT = NOTIFICATION_TYPE_CHANGED;
                sendTrackChangedRsp();
            }

            if (mCurrentPosMs != RemoteControlClient.PLAYBACK_POSITION_ALWAYS_UNKNOWN) {
                mCurrentPosMs = 0L;
                if (mCurrentPlayState == RemoteControlClient.PLAYSTATE_PLAYING) {
                    mPlayStartTimeMs = SystemClock.elapsedRealtime();
                }
            }
            /* need send play position changed notification when track is changed */
            if (mPlayPosChangedNT == NOTIFICATION_TYPE_INTERIM) {
                mPlayPosChangedNT = NOTIFICATION_TYPE_CHANGED;
                registerNotificationRspPlayPosNative(mPlayPosChangedNT,
                                                     (int)getPlayPosition());
                mHandler.removeMessages(MESSAGE_PLAY_INTERVAL_TIMEOUT);
            }
        }
        if (DEBUG) Log.d(TAG, "mMetadata=" + mMetadata.toString());

        mSongLengthMs = data.getLong(MediaMetadataRetriever.METADATA_KEY_DURATION,
                RemoteControlClient.PLAYBACK_POSITION_INVALID);
        if (DEBUG) Log.d(TAG, "duration=" + mSongLengthMs);
    }

    private void getRcFeatures(byte[] address, int features) {
        Message msg = mHandler.obtainMessage(MESSAGE_GET_RC_FEATURES, features, 0,
                                             Utils.getAddressStringFromByte(address));
        mHandler.sendMessage(msg);
    }

    private void getPlayStatus() {
        Message msg = mHandler.obtainMessage(MESSAGE_GET_PLAY_STATUS);
        mHandler.sendMessage(msg);
    }

    private void getElementAttr(byte numAttr, int[] attrs) {
        int i;
        ArrayList<Integer> attrList = new ArrayList<Integer>();
        for (i = 0; i < numAttr; ++i) {
            attrList.add(attrs[i]);
        }
        Message msg = mHandler.obtainMessage(MESSAGE_GET_ELEM_ATTRS, numAttr, 0, attrList);
        mHandler.sendMessage(msg);
    }

    private void registerNotification(int eventId, int param) {
        Message msg = mHandler.obtainMessage(MESSAGE_REGISTER_NOTIFICATION, eventId, param);
        mHandler.sendMessage(msg);
    }

    private void processRegisterNotification(int eventId, int param) {
        switch (eventId) {
            case EVT_PLAY_STATUS_CHANGED:
                mPlayStatusChangedNT = NOTIFICATION_TYPE_INTERIM;
                registerNotificationRspPlayStatusNative(mPlayStatusChangedNT,
                                       convertPlayStateToPlayStatus(mCurrentPlayState));
                break;

            case EVT_TRACK_CHANGED:
                mTrackChangedNT = NOTIFICATION_TYPE_INTERIM;
                sendTrackChangedRsp();
                break;

            case EVT_PLAY_POS_CHANGED:
                long songPosition = getPlayPosition();
                mPlayPosChangedNT = NOTIFICATION_TYPE_INTERIM;
                mPlaybackIntervalMs = (long)param * 1000L;
                if (mCurrentPosMs != RemoteControlClient.PLAYBACK_POSITION_ALWAYS_UNKNOWN) {
                    mNextPosMs = songPosition + mPlaybackIntervalMs;
                    mPrevPosMs = songPosition - mPlaybackIntervalMs;
                    if (mCurrentPlayState == RemoteControlClient.PLAYSTATE_PLAYING) {
                        Message msg = mHandler.obtainMessage(MESSAGE_PLAY_INTERVAL_TIMEOUT);
                        mHandler.sendMessageDelayed(msg, mPlaybackIntervalMs);
                    }
                }
                registerNotificationRspPlayPosNative(mPlayPosChangedNT, (int)songPosition);
                break;
        }
        // M: Add for AVRCP1.5 MediaPlayer Selection
        if (mEnhancedAvrcp != null) {
            mEnhancedAvrcp.processRegisterNotification(eventId, param);
        }
    }

    private void handlePassthroughCmd(int id, int keyState) {
        switch (id) {
            case BluetoothAvrcp.PASSTHROUGH_ID_REWIND:
                rewind(keyState);
                break;
            case BluetoothAvrcp.PASSTHROUGH_ID_FAST_FOR:
                fastForward(keyState);
                break;
        }
    }

    private void fastForward(int keyState) {
        Message msg = mHandler.obtainMessage(MESSAGE_FAST_FORWARD, keyState, 0);
        mHandler.sendMessage(msg);
    }

    private void rewind(int keyState) {
        Message msg = mHandler.obtainMessage(MESSAGE_REWIND, keyState, 0);
        mHandler.sendMessage(msg);
    }

    private void changePositionBy(long amount) {
        long currentPosMs = getPlayPosition();
        if (currentPosMs == -1L) return;
        long newPosMs = Math.max(0L, currentPosMs + amount);
        mRemoteController.seekTo(newPosMs);
    }

    private int getSkipMultiplier() {
        long currentTime = SystemClock.elapsedRealtime();
        long multi = (long) Math.pow(2, (currentTime - mSkipStartTime)/SKIP_DOUBLE_INTERVAL);
        return (int) Math.min(MAX_MULTIPLIER_VALUE, multi);
    }

    private void sendTrackChangedRsp() {
        byte[] track = new byte[TRACK_ID_SIZE];

        /* If no track is currently selected, then return
           0xFFFFFFFFFFFFFFFF in the interim response */
        long trackNumberRsp = -1L;

        if (mCurrentPlayState == RemoteControlClient.PLAYSTATE_PLAYING) {
            trackNumberRsp = mTrackNumber;
        }

        /* track is stored in big endian format */
        for (int i = 0; i < TRACK_ID_SIZE; ++i) {
            track[i] = (byte) (trackNumberRsp >> (56 - 8 * i));
        }
        registerNotificationRspTrackChangeNative(mTrackChangedNT, track);
    }

    private long getPlayPosition() {
        long songPosition = -1L;
        if (mCurrentPosMs != RemoteControlClient.PLAYBACK_POSITION_ALWAYS_UNKNOWN) {
            if (mCurrentPlayState == RemoteControlClient.PLAYSTATE_PLAYING) {
                songPosition = SystemClock.elapsedRealtime() -
                               mPlayStartTimeMs + mCurrentPosMs;
            } else {
                songPosition = mCurrentPosMs;
            }
        }
        if (DEBUG) Log.d(TAG, "position=" + songPosition);
        return songPosition;
    }

    private String getAttributeString(int attrId) {
        String attrStr = null;
        switch (attrId) {
            case MEDIA_ATTR_TITLE:
                attrStr = mMetadata.trackTitle;
                break;

            case MEDIA_ATTR_ARTIST:
                attrStr = mMetadata.artist;
                break;

            case MEDIA_ATTR_ALBUM:
                attrStr = mMetadata.albumTitle;
                break;

            case MEDIA_ATTR_PLAYING_TIME:
                if (mSongLengthMs != 0L) {
                    attrStr = Long.toString(mSongLengthMs);
                }
                break;

            case MEDIA_ATTR_COVER_ART:
                attrStr = BluetoothBipCoverArt.getCoverArtHandle();
                break;

        }
        if (attrStr == null) {
            attrStr = new String();
        }
        if (DEBUG) Log.d(TAG, "getAttributeString:attrId=" + attrId + " str=" + attrStr);
        return attrStr;
    }

    private int convertPlayStateToPlayStatus(int playState) {
        int playStatus = PLAYSTATUS_ERROR;
        switch (playState) {
            case RemoteControlClient.PLAYSTATE_PLAYING:
            case RemoteControlClient.PLAYSTATE_BUFFERING:
                playStatus = PLAYSTATUS_PLAYING;
                break;

            case RemoteControlClient.PLAYSTATE_STOPPED:
            case RemoteControlClient.PLAYSTATE_NONE:
                playStatus = PLAYSTATUS_STOPPED;
                break;

            case RemoteControlClient.PLAYSTATE_PAUSED:
                playStatus = PLAYSTATUS_PAUSED;
                break;

            case RemoteControlClient.PLAYSTATE_FAST_FORWARDING:
            case RemoteControlClient.PLAYSTATE_SKIPPING_FORWARDS:
                playStatus = PLAYSTATUS_FWD_SEEK;
                break;

            case RemoteControlClient.PLAYSTATE_REWINDING:
            case RemoteControlClient.PLAYSTATE_SKIPPING_BACKWARDS:
                playStatus = PLAYSTATUS_REV_SEEK;
                break;

            case RemoteControlClient.PLAYSTATE_ERROR:
                playStatus = PLAYSTATUS_ERROR;
                break;

        }
        return playStatus;
    }

    private boolean isPlayingState(int playState) {
        boolean isPlaying = false;
        switch (playState) {
            case RemoteControlClient.PLAYSTATE_PLAYING:
            case RemoteControlClient.PLAYSTATE_BUFFERING:
                isPlaying = true;
                break;
            default:
                isPlaying = false;
                break;
        }
        return isPlaying;
    }

    /**
     * This is called from AudioService. It will return whether this device supports abs volume.
     * NOT USED AT THE MOMENT.
     */
    public boolean isAbsoluteVolumeSupported() {
        return ((mFeatures & BTRC_FEAT_ABSOLUTE_VOLUME) != 0);
    }

    /**
     * We get this call from AudioService. This will send a message to our handler object,
     * requesting our handler to call setVolumeNative()
     */
    public void adjustVolume(int direction) {
        Message msg = mHandler.obtainMessage(MESSAGE_ADJUST_VOLUME, direction, 0);
        mHandler.sendMessage(msg);
    }

    public void setAbsoluteVolume(int volume) {
        int avrcpVolume = convertToAvrcpVolume(volume);
        avrcpVolume = Math.min(AVRCP_MAX_VOL, Math.max(0, avrcpVolume));
        mHandler.removeMessages(MESSAGE_ADJUST_VOLUME);
        Message msg = mHandler.obtainMessage(MESSAGE_SET_ABSOLUTE_VOLUME, avrcpVolume, 0);
        mHandler.sendMessage(msg);
    }

    /* Called in the native layer as a btrc_callback to return the volume set on the carkit in the
     * case when the volume is change locally on the carkit. This notification is not called when
     * the volume is changed from the phone.
     *
     * This method will send a message to our handler to change the local stored volume and notify
     * AudioService to update the UI
     */
    private void volumeChangeCallback(int volume, int ctype) {
        Message msg = mHandler.obtainMessage(MESSAGE_VOLUME_CHANGED, volume, ctype);
        mHandler.sendMessage(msg);
    }

    private void notifyVolumeChanged(int volume) {
        volume = convertToAudioStreamVolume(volume);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume,
                      AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_BLUETOOTH_ABS_VOLUME);
    }

    private int convertToAudioStreamVolume(int volume) {
        // Rescale volume to match AudioSystem's volume
        return (int) Math.round((double) volume*mAudioStreamMax/AVRCP_MAX_VOL);
    }

    private int convertToAvrcpVolume(int volume) {
        return (int) Math.ceil((double) volume*AVRCP_MAX_VOL/mAudioStreamMax);
    }

    /**
     * This is called from A2dpStateMachine to set A2dp audio state.
     */
    public void setA2dpAudioState(int state) {
        Message msg = mHandler.obtainMessage(MESSAGE_SET_A2DP_AUDIO_STATE, state, 0);
        mHandler.sendMessage(msg);
    }

    public void dump(StringBuilder sb) {
        sb.append("AVRCP:\n");
        ProfileService.println(sb, "mMetadata: " + mMetadata);
        ProfileService.println(sb, "mTransportControlFlags: " + mTransportControlFlags);
        ProfileService.println(sb, "mCurrentPlayState: " + mCurrentPlayState);
        ProfileService.println(sb, "mPlayStatusChangedNT: " + mPlayStatusChangedNT);
        ProfileService.println(sb, "mTrackChangedNT: " + mTrackChangedNT);
        ProfileService.println(sb, "mTrackNumber: " + mTrackNumber);
        ProfileService.println(sb, "mCurrentPosMs: " + mCurrentPosMs);
        ProfileService.println(sb, "mPlayStartTimeMs: " + mPlayStartTimeMs);
        ProfileService.println(sb, "mSongLengthMs: " + mSongLengthMs);
        ProfileService.println(sb, "mPlaybackIntervalMs: " + mPlaybackIntervalMs);
        ProfileService.println(sb, "mPlayPosChangedNT: " + mPlayPosChangedNT);
        ProfileService.println(sb, "mNextPosMs: " + mNextPosMs);
        ProfileService.println(sb, "mPrevPosMs: " + mPrevPosMs);
        ProfileService.println(sb, "mSkipStartTime: " + mSkipStartTime);
        ProfileService.println(sb, "mFeatures: " + mFeatures);
        ProfileService.println(sb, "mAbsoluteVolume: " + mAbsoluteVolume);
        ProfileService.println(sb, "mLastSetVolume: " + mLastSetVolume);
        ProfileService.println(sb, "mLastDirection: " + mLastDirection);
        ProfileService.println(sb, "mVolumeStep: " + mVolumeStep);
        ProfileService.println(sb, "mAudioStreamMax: " + mAudioStreamMax);
        ProfileService.println(sb, "mVolCmdInProgress: " + mVolCmdInProgress);
        ProfileService.println(sb, "mAbsVolRetryTimes: " + mAbsVolRetryTimes);
        ProfileService.println(sb, "mSkipAmount: " + mSkipAmount);
    }

    // Do not modify without updating the HAL bt_rc.h files.

    // match up with btrc_play_status_t enum of bt_rc.h
    final static int PLAYSTATUS_STOPPED = 0;
    final static int PLAYSTATUS_PLAYING = 1;
    final static int PLAYSTATUS_PAUSED = 2;
    final static int PLAYSTATUS_FWD_SEEK = 3;
    final static int PLAYSTATUS_REV_SEEK = 4;
    final static int PLAYSTATUS_ERROR = 255;

    // match up with btrc_media_attr_t enum of bt_rc.h
    final static int MEDIA_ATTR_TITLE = 1;
    final static int MEDIA_ATTR_ARTIST = 2;
    final static int MEDIA_ATTR_ALBUM = 3;
    final static int MEDIA_ATTR_TRACK_NUM = 4;
    final static int MEDIA_ATTR_NUM_TRACKS = 5;
    final static int MEDIA_ATTR_GENRE = 6;
    final static int MEDIA_ATTR_PLAYING_TIME = 7;
    final static int MEDIA_ATTR_COVER_ART = 8;

    // match up with btrc_event_id_t enum of bt_rc.h
    final static int EVT_PLAY_STATUS_CHANGED = 1;
    final static int EVT_TRACK_CHANGED = 2;
    final static int EVT_TRACK_REACHED_END = 3;
    final static int EVT_TRACK_REACHED_START = 4;
    final static int EVT_PLAY_POS_CHANGED = 5;
    final static int EVT_BATT_STATUS_CHANGED = 6;
    final static int EVT_SYSTEM_STATUS_CHANGED = 7;
    final static int EVT_APP_SETTINGS_CHANGED = 8;

    // match up with btrc_notification_type_t enum of bt_rc.h
    final static int NOTIFICATION_TYPE_INTERIM = 0;
    final static int NOTIFICATION_TYPE_CHANGED = 1;
    // M: Add for AVRCP1.5 MediaPlayer Selection
    final static int NOTIFICATION_TYPE_REJECTED = 2;

    // match up with BTRC_UID_SIZE of bt_rc.h
    final static int TRACK_ID_SIZE = 8;

    private native static void classInitNative();
    private native void initNative();
    private native void cleanupNative();
    private native boolean getPlayStatusRspNative(int playStatus, int songLen, int songPos);
    private native boolean getElementAttrRspNative(byte numAttr, int[] attrIds, String[] textArray);
    private native boolean registerNotificationRspPlayStatusNative(int type, int playStatus);
    private native boolean registerNotificationRspTrackChangeNative(int type, byte[] track);
    private native boolean registerNotificationRspPlayPosNative(int type, int playPos);
    private native boolean setVolumeNative(int volume);
    private native boolean sendPassThroughCommandNative(int keyCode, int keyState);

    private AvrcpEnhance mEnhancedAvrcp;

    private void sendRegisterNotificationPlayerStatusReject() {
        Log.i(TAG, "send Registerd Notification Player Status Rejected");
        if (mPlayStatusChangedNT == NOTIFICATION_TYPE_INTERIM) {
            mPlayStatusChangedNT = NOTIFICATION_TYPE_REJECTED;
            registerNotificationRspPlayStatusNative(mPlayStatusChangedNT, PLAYSTATUS_STOPPED);
        }
        if (mPlayPosChangedNT == NOTIFICATION_TYPE_INTERIM) {
            mPlayPosChangedNT = NOTIFICATION_TYPE_REJECTED;
            registerNotificationRspPlayPosNative(mPlayPosChangedNT, -1);
        }
        if (mTrackChangedNT == NOTIFICATION_TYPE_INTERIM) {
            mTrackChangedNT = NOTIFICATION_TYPE_REJECTED;
            sendTrackChangedRsp();
        }
    }
}
