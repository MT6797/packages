/*
 * Copyright 2014, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.telecom;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;

/**
 * Play a call-related tone (ringback, busy signal, etc.) through ToneGenerator. To use, create an
 * instance using InCallTonePlayer.Factory (passing in the TONE_* constant for the tone you want)
 * and start() it. Implemented on top of {@link Thread} so that the tone plays in its own thread.
 */
public final class InCallTonePlayer extends Thread {

    /**
     * Factory used to create InCallTonePlayers. Exists to aid with testing mocks.
     */
    public static class Factory {
        private final CallAudioManager mCallAudioManager;
        private final TelecomSystem.SyncRoot mLock;

        Factory(CallAudioManager callAudioManager, TelecomSystem.SyncRoot lock) {
            mCallAudioManager = callAudioManager;
            mLock = lock;
        }

        InCallTonePlayer createPlayer(int tone) {
            return new InCallTonePlayer(tone, mCallAudioManager, mLock);
        }
    }

    // The possible tones that we can play.
    public static final int TONE_INVALID = 0;
    public static final int TONE_BUSY = 1;
    public static final int TONE_CALL_ENDED = 2;
    public static final int TONE_OTA_CALL_ENDED = 3;
    public static final int TONE_CALL_WAITING = 4;
    public static final int TONE_CDMA_DROP = 5;
    public static final int TONE_CONGESTION = 6;
    public static final int TONE_INTERCEPT = 7;
    public static final int TONE_OUT_OF_SERVICE = 8;
    public static final int TONE_REDIAL = 9;
    public static final int TONE_REORDER = 10;
    public static final int TONE_RING_BACK = 11;
    public static final int TONE_UNOBTAINABLE_NUMBER = 12;
    public static final int TONE_VOICE_PRIVACY = 13;
    public static final int TONE_VIDEO_UPGRADE = 14;

    private static final int RELATIVE_VOLUME_EMERGENCY = 100;
    private static final int RELATIVE_VOLUME_HIPRI = 80;
    private static final int RELATIVE_VOLUME_LOPRI = 50;

    // Buffer time (in msec) to add on to the tone timeout value. Needed mainly when the timeout
    // value for a tone is exact duration of the tone itself.
    private static final int TIMEOUT_BUFFER_MILLIS = 20;

    // The tone state.
    private static final int STATE_OFF = 0;
    private static final int STATE_ON = 1;
    private static final int STATE_STOPPED = 2;

    /**
     * Keeps count of the number of actively playing tones so that we can notify CallAudioManager
     * when we need focus and when it can be release. This should only be manipulated from the main
     * thread.
     */
    private static int sTonesPlaying = 0;

    private final CallAudioManager mCallAudioManager;

    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

    /** The ID of the tone to play. */
    private final int mToneId;

    /** Current state of the tone player. */
    private int mState;

    /** Telecom lock object. */
    private final TelecomSystem.SyncRoot mLock;

    /**
     * Initializes the tone player. Private; use the {@link Factory} to create tone players.
     *
     * @param toneId ID of the tone to play, see TONE_* constants.
     */
    private InCallTonePlayer(
            int toneId,
            CallAudioManager callAudioManager,
            TelecomSystem.SyncRoot lock) {
        mState = STATE_OFF;
        mToneId = toneId;
        mCallAudioManager = callAudioManager;
        mLock = lock;
    }

    /** {@inheritDoc} */
    @Override
    public void run() {
        ToneGenerator toneGenerator = null;
        try {
            Log.d(this, "run(toneId = %s)", mToneId);

            final int toneType;  // Passed to ToneGenerator.startTone.
            final int toneVolume;  // Passed to the ToneGenerator constructor.
            final int toneLengthMillis;

            switch (mToneId) {
                case TONE_BUSY:
                    // TODO: CDMA-specific tones
                    toneType = ToneGenerator.TONE_SUP_BUSY;
                    toneVolume = RELATIVE_VOLUME_HIPRI;
                    toneLengthMillis = 4000;
                    break;
                case TONE_CALL_ENDED:
                    toneType = ToneGenerator.TONE_PROP_PROMPT;
                    toneVolume = RELATIVE_VOLUME_HIPRI;
                    toneLengthMillis = 200;
                    break;
                case TONE_OTA_CALL_ENDED:
                    // TODO: fill in
                    throw new IllegalStateException("OTA Call ended NYI.");
                case TONE_CALL_WAITING:
                    toneType = ToneGenerator.TONE_SUP_CALL_WAITING;
                    toneVolume = RELATIVE_VOLUME_HIPRI;
                    toneLengthMillis = Integer.MAX_VALUE - TIMEOUT_BUFFER_MILLIS;
                    break;
                case TONE_CDMA_DROP:
                    toneType = ToneGenerator.TONE_CDMA_CALLDROP_LITE;
                    toneVolume = RELATIVE_VOLUME_LOPRI;
                    toneLengthMillis = 375;
                    break;
                case TONE_CONGESTION:
                    toneType = ToneGenerator.TONE_SUP_CONGESTION;
                    toneVolume = RELATIVE_VOLUME_HIPRI;
                    toneLengthMillis = 4000;
                    break;
                case TONE_INTERCEPT:
                    toneType = ToneGenerator.TONE_CDMA_ABBR_INTERCEPT;
                    toneVolume = RELATIVE_VOLUME_LOPRI;
                    toneLengthMillis = 500;
                    break;
                case TONE_OUT_OF_SERVICE:
                    toneType = ToneGenerator.TONE_CDMA_CALLDROP_LITE;
                    toneVolume = RELATIVE_VOLUME_LOPRI;
                    toneLengthMillis = 375;
                    break;
                case TONE_REDIAL:
                    toneType = ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE;
                    toneVolume = RELATIVE_VOLUME_LOPRI;
                    toneLengthMillis = 5000;
                    break;
                case TONE_REORDER:
                    toneType = ToneGenerator.TONE_CDMA_REORDER;
                    toneVolume = RELATIVE_VOLUME_HIPRI;
                    toneLengthMillis = 4000;
                    break;
                case TONE_RING_BACK:
                    toneType = ToneGenerator.TONE_SUP_RINGTONE;
                    toneVolume = RELATIVE_VOLUME_HIPRI;
                    toneLengthMillis = Integer.MAX_VALUE - TIMEOUT_BUFFER_MILLIS;
                    break;
                case TONE_UNOBTAINABLE_NUMBER:
                    toneType = ToneGenerator.TONE_SUP_ERROR;
                    toneVolume = RELATIVE_VOLUME_HIPRI;
                    toneLengthMillis = 4000;
                    break;
                case TONE_VOICE_PRIVACY:
                    // TODO: fill in.
                    throw new IllegalStateException("Voice privacy tone NYI.");
                case TONE_VIDEO_UPGRADE:
                    // Similar to the call waiting tone, but does not repeat.
                    toneType = ToneGenerator.TONE_SUP_CALL_WAITING;
                    toneVolume = RELATIVE_VOLUME_HIPRI;
                    toneLengthMillis = 4000;
                    break;
                default:
                    throw new IllegalStateException("Bad toneId: " + mToneId);
            }

            int stream = AudioManager.STREAM_VOICE_CALL;
            if (mCallAudioManager.isBluetoothAudioOn()) {
                stream = AudioManager.STREAM_BLUETOOTH_SCO;
            }

            // If the ToneGenerator creation fails, just continue without it. It is a local audio
            // signal, and is not as important.
            try {
                Log.v(this, "Creating generator");
                toneGenerator = new ToneGenerator(stream, toneVolume);
            } catch (RuntimeException e) {
                Log.w(this, "Failed to create ToneGenerator.", e);
                return;
            }

            // TODO: Certain CDMA tones need to check the ringer-volume state before
            // playing. See CallNotifier.InCallTonePlayer.

            // TODO: Some tones play through the end of a call so we need to inform
            // CallAudioManager that we want focus the same way that Ringer does.

            synchronized (this) {
                if (mState != STATE_STOPPED) {
                    mState = STATE_ON;
                    toneGenerator.startTone(toneType);
                    try {
                        Log.v(this, "Starting tone %d...waiting for %d ms.", mToneId,
                                toneLengthMillis + TIMEOUT_BUFFER_MILLIS);
                        wait(toneLengthMillis + TIMEOUT_BUFFER_MILLIS);
                    } catch (InterruptedException e) {
                        Log.w(this, "wait interrupted", e);
                    }
                }
            }
            mState = STATE_OFF;
        } finally {
            if (toneGenerator != null) {
                toneGenerator.release();
            }
            cleanUpTonePlayer();
        }
    }

    void startTone() {
        sTonesPlaying++;
        if (sTonesPlaying == 1) {
            mCallAudioManager.setIsTonePlaying(true);
        }

        start();
    }

    /**
     * Stops the tone.
     */
    void stopTone() {
        synchronized (this) {
            if (mState == STATE_ON) {
                Log.d(this, "Stopping the tone %d.", mToneId);
                notify();
            }
            mState = STATE_STOPPED;
        }
    }

    private void cleanUpTonePlayer() {
        // Release focus on the main thread.
        mMainThreadHandler.post(new Runnable() {
            @Override public void run() {
                synchronized (mLock) {
                    if (sTonesPlaying == 0) {
                        Log.wtf(this, "Over-releasing focus for tone player.");
                    } else if (--sTonesPlaying == 0) {
                        mCallAudioManager.setIsTonePlaying(false);
                    }
                }
            }
        });
    }
}
