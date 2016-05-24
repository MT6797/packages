/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.gallery3d.video;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.sqlite.SQLiteException;
import android.media.AudioManager;
import android.media.ClosedCaptionRenderer;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.SubtitleController;
import android.media.TtmlRenderer;
import android.media.WebVttRenderer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.media.MediaPlayer.TrackInfo;
import android.media.MediaPlayer.OnTimedTextListener;
import android.media.Metadata;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.widget.VideoView;

import com.android.gallery3d.ui.Log;
import com.mediatek.gallery3d.video.ScreenModeManager.ScreenModeListener;
import com.mediatek.galleryframework.util.MtkLog;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;

import java.io.InputStream;
import java.io.IOException;
import java.util.Map;

/**
 * MTKVideoView enhance the streaming videoplayer process and UI.
 * It only supports MTKMediaController.
 * If you set android's default MediaController,
 * some state will not be shown well.
 * Moved from the package android.widget
 */
public class MTKVideoView extends VideoView implements ScreenModeListener, IMtkVideoController {
    private static final String TAG = "Gallery2/VideoPlayer/MTKVideoView";
    private static final boolean LOG = true;

    //add info listener to get info whether can get meta data or not for rtsp.
    private MediaPlayer.OnInfoListener mOnInfoListener;
    private MediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener;
    private MediaPlayer.OnVideoSizeChangedListener mVideoSizeListener;
    //M: FOR MTK_SUBTITLE_SUPPORT //@{
    private MediaPlayer.OnTimedTextListener mOnTimedTextListener;
    //@}
    private boolean mHasGotPreparedCallBack = false;
    private int mDuration;
    //for slow motion.
    private int mSlowMotionSpeed = 0;
    private String mSlowMotionSection;
    private boolean mEnableSlowMotionSpeed = false;
    private static int KEY_SLOW_MOTION_SPEED = 1800;
    private static int KEY_SLOW_MOTION_SECTION = 1900;
    // / M: Handle for non-google notify messages make CTS fail,MTK_PLAYBACK
    // value 1 means the player is MTK video player@{
    private static int KEY_PLAYBACK_PARAMETER = 2100;
    private static int MTK_PLAYBACK_VALUE = 1;
    // / @}
    private static int MEDIA_ERROR_BASE = -1000;
    private static int ERROR_BUFFER_DEQUEUE_FAIL = MEDIA_ERROR_BASE - 100 - 6;
    ///M:@ { add for dismiss error dialog when activity onResume
    private final String errorDialogTag = "ERROR_DIALOG_TAG";
    private FragmentManager fragmentManager ;
    public void dismissAllowingStateLoss() {
        if (fragmentManager == null) {
            fragmentManager = ((Activity) mContext).getFragmentManager();
        }
        DialogFragment oldFragment = (DialogFragment) fragmentManager
                .findFragmentByTag(errorDialogTag);
        if (null != oldFragment) {
            oldFragment.dismissAllowingStateLoss();
        }
    }
    /// @}
    private final MediaPlayer.OnInfoListener mInfoListener = new MediaPlayer.OnInfoListener() {

        public boolean onInfo(final MediaPlayer mp, final int what, final int extra) {
            if (LOG) {
                MtkLog.v(TAG, "onInfo() what:" + what + " extra:" + extra);
            }
            if (mOnInfoListener != null && mOnInfoListener.onInfo(mp, what, extra)) {
                return true;
            }
            return false;
        }

    };

    private void doPreparedIfReady(final MediaPlayer mp) {
        if (LOG) {
            MtkLog.v(TAG, "doPreparedIfReady() mHasGotPreparedCallBack=" + mHasGotPreparedCallBack
                    + ", mNeedWaitLayout=" + mNeedWaitLayout + ", mCurrentState=" + mCurrentState);
        }
        if (mHasGotPreparedCallBack && !mNeedWaitLayout) {
            mHandler.removeMessages(MSG_LAYOUT_READY, null);
            doPrepared(mp);
        }
    }

    public MTKVideoView(final Context context) {
        super(context);
        initialize();
    }

    public MTKVideoView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public MTKVideoView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    private void initialize() {
        mPreparedListener = new MediaPlayer.OnPreparedListener() {
            public void onPrepared(final MediaPlayer mp) {
                if (LOG) {
                    MtkLog.v(TAG, "mPreparedListener.onPrepared(" + mp + ")");
                }
                //Here we can get meta data from mediaplayer.
                // Get the capabilities of the player for this stream
                final Metadata data = mp.getMetadata(MediaPlayer.METADATA_ALL,
                                          MediaPlayer.BYPASS_METADATA_FILTER);
                if (data != null) {
                    mCanPause = !data.has(Metadata.PAUSE_AVAILABLE)
                            || data.getBoolean(Metadata.PAUSE_AVAILABLE);
                    mCanSeekBack = !data.has(Metadata.SEEK_BACKWARD_AVAILABLE)
                            || data.getBoolean(Metadata.SEEK_BACKWARD_AVAILABLE);
                    mCanSeekForward = !data.has(Metadata.SEEK_FORWARD_AVAILABLE)
                            || data.getBoolean(Metadata.SEEK_FORWARD_AVAILABLE);
                } else {
                    mCanPause = true;
                    mCanSeekBack = true;
                    mCanSeekForward = true;
                    MtkLog.w(TAG, "Metadata is null!");
                }
                if (LOG) {
                    MtkLog.v(TAG, "mPreparedListener.onPrepared() mCanPause=" + mCanPause);
                }

                /// M: [PERF.ADD] @{
                MoviePerfHelper.trigger("onPrepared");
                /// @}

                mHasGotPreparedCallBack = true;
                doPreparedIfReady(mMediaPlayer);
            }
        };

        mErrorListener = new MediaPlayer.OnErrorListener() {
            public boolean onError(
                    final MediaPlayer mp, final int frameworkErr, final int implErr) {
                Log.d(TAG, "Error: " + frameworkErr + "," + implErr);
                if (mCurrentState == STATE_ERROR) {
                    Log.w(TAG, "Duplicate error message. error message has been sent! " +
                            "error=(" + frameworkErr + "," + implErr + ")");
                    return true;
                }
                //record error position and duration
                //here disturb the original logic
                mSeekWhenPrepared = getCurrentPosition();
                if (LOG) {
                    Log.v(TAG, "onError() mSeekWhenPrepared=" + mSeekWhenPrepared
                        + ", mDuration=" + mDuration);
                }
                //for old version Streaming server, getduration is not valid.
                mDuration = Math.abs(mDuration);
                mCurrentState = STATE_ERROR;
                mTargetState = STATE_ERROR;
                if (mMediaController != null) {
                    mMediaController.hide();
                }

                /* If an error handler has been supplied, use it and finish. */
                if (mOnErrorListener != null) {
                    if (mOnErrorListener.onError(mMediaPlayer, frameworkErr, implErr)) {
                        return true;
                    }
                }

                /* Otherwise, pop up an error dialog so the user knows that
                 * something bad has happened. Only try and pop up the dialog
                 * if we're attached to a window. When we're going away and no
                 * longer have a window, don't bother showing the user an error.
                 */
                if (getWindowToken() != null) {
                    final Resources r = mContext.getResources();
                    int messageId;

                    if (frameworkErr == MEDIA_ERROR_BAD_FILE) {
                        if (implErr == ERROR_BUFFER_DEQUEUE_FAIL) {
                            return true;
                        }
                        messageId = com.mediatek.R.string.VideoView_error_text_bad_file;
                    } else if (frameworkErr == MEDIA_ERROR_CANNOT_CONNECT_TO_SERVER) {
                        messageId =
                            com.mediatek.R.string.VideoView_error_text_cannot_connect_to_server;
                    } else if (frameworkErr == MEDIA_ERROR_TYPE_NOT_SUPPORTED) {
                        messageId = com.mediatek.R.string.VideoView_error_text_type_not_supported;
                    } else if (frameworkErr == MEDIA_ERROR_DRM_NOT_SUPPORTED) {
                        messageId = com.mediatek.R.string.VideoView_error_text_drm_not_supported;
                    } else if (frameworkErr == MEDIA_ERROR_INVALID_CONNECTION) {
                        messageId =
                            com.mediatek.internal.R.string.VideoView_error_text_invalid_connection;
                    } else if (frameworkErr ==
                            MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
                        messageId =
                            com.android.internal.R.string.VideoView_error_text_invalid_progressive_playback;
                    } else {
                        messageId = com.android.internal.R.string.VideoView_error_text_unknown;
                    }
                    dismissAllowingStateLoss();
                    // do not call error dialog showing when activity has finished
                    if (!((Activity) mContext).isFinishing()) {
                        DialogFragment newFragment = ErrorDialogFragment.newInstance(messageId);
                        newFragment.show(fragmentManager, errorDialogTag);
                        fragmentManager.executePendingTransactions();
                    }
                }
                return true;
            }
        };

        mBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
            public void onBufferingUpdate(final MediaPlayer mp, final int percent) {
                mCurrentBufferPercentage = percent;
                if (mOnBufferingUpdateListener != null) {
                    mOnBufferingUpdateListener.onBufferingUpdate(mp, percent);
                }
                if (LOG) {
                    MtkLog.v(TAG, "onBufferingUpdate() Buffering percent: " + percent);
                }
                if (LOG) {
                    MtkLog.v(TAG, "onBufferingUpdate() mTargetState=" + mTargetState);
                }
                if (LOG) {
                    MtkLog.v(TAG, "onBufferingUpdate() mCurrentState=" + mCurrentState);
                }
            }
        };

        mSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener() {
            public void onVideoSizeChanged(
                    final MediaPlayer mp, final int width, final int height) {
                mVideoWidth = mp.getVideoWidth();
                mVideoHeight = mp.getVideoHeight();

                //Add for Video Zoom.
                if (mZoomController != null) {
                    mZoomController.setVideoSize(mVideoWidth, mVideoHeight);
                }

                if (LOG) {
                    MtkLog.v(TAG, "OnVideoSizeChagned(" + width + "," + height + ")");
                }
                if (LOG) {
                    MtkLog.v(TAG, "OnVideoSizeChagned(" + mVideoWidth + "," + mVideoHeight + ")");
                }
                if (LOG) {
                    MtkLog.v(TAG, "OnVideoSizeChagned() mCurrentState=" + mCurrentState);
                }
                if (mVideoWidth != 0 && mVideoHeight != 0) {
                    //need follow google design
                    getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                    /// M: [PERF.ADD] @{
                    MoviePerfHelper.trigger("onVideoSizeChanged", mCurrentState);
                    /// @}
                    if (mCurrentState == STATE_PREPARING) {
                        mNeedWaitLayout = true;
                    }
                }
                if (mVideoSizeListener != null) {
                    mVideoSizeListener.onVideoSizeChanged(mp, width, height);
                }
                MTKVideoView.this.requestLayout();
            }
        };

        getHolder().removeCallback(mSHCallback);
        mSHCallback = new SurfaceHolder.Callback() {
            public void surfaceChanged(
                    final SurfaceHolder holder, final int format, final int w, final int h) {
                if (LOG) {
                    Log.v(TAG,
                        "surfaceChanged(" + holder + ", " + format + ", " + w + ", " + h + ")");
                }
                if (LOG) {
                    Log.v(TAG, "surfaceChanged() mMediaPlayer=" + mMediaPlayer
                        + ", mTargetState=" + mTargetState + ", mVideoWidth="
                        + mVideoWidth + ", mVideoHeight=" + mVideoHeight);
                }
                mSurfaceWidth = w;
                mSurfaceHeight = h;
                final boolean isValidState =  (mTargetState == STATE_PLAYING);
                final boolean hasValidSize = (mVideoWidth == w && mVideoHeight == h);
                if (mMediaPlayer != null && isValidState && hasValidSize
                        && mCurrentState != STATE_PLAYING) {
                    if (mSeekWhenPrepared != 0) {
                        seekTo(mSeekWhenPrepared);
                    }
                    Log.v(TAG, "surfaceChanged() start()");
                    MoviePerfHelper.trigger("surfaceChanged start");
                    start();
                }
            }

            public void surfaceCreated(final SurfaceHolder holder) {
                if (LOG) {
                    Log.v(TAG, "surfaceCreated(" + holder + ")");
                }
                mSurfaceHolder = holder;
                /// M: [PERF.ADD] @{
                MoviePerfHelper.trigger("surfaceCreated");
                /// @}
                openVideo();
             }

            public void surfaceDestroyed(final SurfaceHolder holder) {
                // after we return from this we can't use the surface any more
                if (LOG) {
                    Log.v(TAG, "surfaceDestroyed(" + holder + ")");
                }
                mSurfaceHolder = null;
                if (mMediaController != null) {
                    mMediaController.hide();
                }
                release(true);
            }
        };
        getHolder().addCallback(mSHCallback);
    }

    private VideoZoomController mZoomController;
    public void setVideoZoomController(VideoZoomController controller) {
        mZoomController = controller;
    }
    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
        int screenMode = ScreenModeManager.SCREENMODE_BIGSCREEN;
        if (mScreenManager != null) {
            screenMode = mScreenManager.getScreenMode();
        }
        if (mZoomController != null) {
            mZoomController.setScreenMode(screenMode);
        }

        switch (screenMode) {
        case ScreenModeManager.SCREENMODE_BIGSCREEN:
            if (mVideoWidth > 0 && mVideoHeight > 0) {
                if (mVideoWidth * height  > width * mVideoHeight) {
                    //Log.i("@@@", "image too tall, correcting");
                    height = width * mVideoHeight / mVideoWidth;
                } else if (mVideoWidth * height  < width * mVideoHeight) {
                    //Log.i("@@@", "image too wide, correcting");
                    width = height * mVideoWidth / mVideoHeight;
                } /*else {
                    //Log.i("@@@", "aspect ratio is correct: " +
                            //width+"/"+height+"="+
                            //mVideoWidth+"/"+mVideoHeight);
                }*/
            }
            break;
        case ScreenModeManager.SCREENMODE_FULLSCREEN:
            break;
        case ScreenModeManager.SCREENMODE_CROPSCREEN:
            if (mVideoWidth > 0 && mVideoHeight > 0) {
                if (mVideoWidth * height  > width * mVideoHeight) {
                    //extend width to be cropped
                    width = height * mVideoWidth / mVideoHeight;
                } else if (mVideoWidth * height  < width * mVideoHeight) {
                    //extend height to be cropped
                    height = width * mVideoHeight / mVideoWidth;
                }
            }
            break;
        default:
            width = 0;
            height = 0;
            MtkLog.w(TAG, "wrong screen mode : " + screenMode);
            break;
        }
        if (LOG) {
            MtkLog.v(TAG, "onMeasure() set size: " + width + 'x' + height);
            MtkLog.v(TAG, "onMeasure() video size: " + mVideoWidth + 'x' + mVideoHeight);
            MtkLog.v(TAG, "onMeasure() mNeedWaitLayout=" + mNeedWaitLayout);
        }
        if (mZoomController != null) {
            if (mZoomController.isInZoomState()) {
                width = mZoomController.getDispScaleWidth();
                height = mZoomController.getDispScaleHeight();
            }

        }
        setMeasuredDimension(width, height);
        if (mNeedWaitLayout) { //when OnMeasure ok, start video.
            /// M: [PERF.ADD] @{
            MoviePerfHelper.trigger("onMeasure need wait layout");
            /// @}
            mNeedWaitLayout = false;
            mHandler.sendEmptyMessage(MSG_LAYOUT_READY);
        }
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent ev) {
//        if (LOG) Log.v(TAG, "onTouchEvent(" + ev + ")");
//        if (mMediaController != null) {
//            toggleMediaControlsVisiblity();
//        }
//        return false;
//    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        final boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK &&
                                     keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
                                     keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
                                     keyCode != KeyEvent.KEYCODE_VOLUME_MUTE &&
                                     keyCode != KeyEvent.KEYCODE_MENU &&
                                     keyCode != KeyEvent.KEYCODE_CALL &&
                                     keyCode != KeyEvent.KEYCODE_ENDCALL &&
                                     keyCode != KeyEvent.KEYCODE_CAMERA;
        if (isInPlaybackState() && isKeyCodeSupported && mMediaController != null) {
            if (event.getRepeatCount() == 0 && (keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                    keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)) {
                if (mMediaPlayer.isPlaying()) {
                    pause();
                    mMediaController.show();
                } else {
                    start();
                    mMediaController.hide();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                if (!mMediaPlayer.isPlaying()) {
                    start();
                    mMediaController.hide();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                    || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                if (mMediaPlayer.isPlaying()) {
                    pause();
                    mMediaController.show();
                }
                return true;
            } else if (keyCode ==  KeyEvent.KEYCODE_MEDIA_FAST_FORWARD ||
                    keyCode ==  KeyEvent.KEYCODE_MEDIA_NEXT ||
                    keyCode ==  KeyEvent.KEYCODE_MEDIA_PREVIOUS ||
                    keyCode ==  KeyEvent.KEYCODE_MEDIA_REWIND ||
                    keyCode ==  KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                    keyCode ==  KeyEvent.KEYCODE_HEADSETHOOK) {
                //consume media action, so if video view if front,
                //other media player will not play any sounds.
                return true;
            } else {
                toggleMediaControlsVisiblity();
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void setVideoURI(final Uri uri, final Map<String, String> headers) {
        if (LOG) {
            MtkLog.v(TAG, "setVideoURI(" + uri + ", " + headers + ")");
        }
        mDuration = -1;
        setResumed(true);
        super.setVideoURI(uri, headers);
    }

    private void clearVideoInfo() {
        if (LOG) {
            Log.v(TAG, "clearVideoInfo()");
        }
        mHasGotPreparedCallBack = false;
        mNeedWaitLayout = false;
    }

    @Override
    protected void openVideo() {
        if (LOG) {
            Log.v(TAG, "openVideo() mUri=" + mUri + ", mSurfaceHolder=" + mSurfaceHolder
                    + ", mSeekWhenPrepared=" + mSeekWhenPrepared + ", mMediaPlayer="
                    + mMediaPlayer + ", mOnResumed=" + mOnResumed);
        }
        clearVideoInfo();
        if (!mOnResumed || mUri == null || mSurfaceHolder == null) {
            // not ready for playback just yet, will try again later
            return;
        }

        // Tell the music playback service to pause
        // TODO: these constants need to be published somewhere in the framework.
        final Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "pause");
        mContext.sendBroadcast(i);

        // we shouldn't clear the target state, because somebody might have
        // called start() previously
        release(false);
        if ("".equalsIgnoreCase(String.valueOf(mUri))) {
            Log.w(TAG, "Unable to open content: " + mUri);
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        }
        try {
            mMediaPlayer = new MediaPlayer();
            // TODO: create SubtitleController in MediaPlayer, but we need
            // a context for the subtitle renderers
            final Context context = getContext();
            final SubtitleController controller = new SubtitleController(
                    context, mMediaPlayer.getMediaTimeProvider(), mMediaPlayer);
            controller.registerRenderer(new WebVttRenderer(context));
            controller.registerRenderer(new TtmlRenderer(context));
            controller.registerRenderer(new ClosedCaptionRenderer(context));
            mMediaPlayer.setSubtitleAnchor(controller, this);

            //end update status.
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            // subtile FOR MTK_SUBTITLE_SUPPORT
            //@{
            if (MtkVideoFeature.isSubTitleSupport()) {
                 mMediaPlayer.setOnTimedTextListener(mOnTimedTextListener);
            }
            //@}
            //mDuration = -1;
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mMediaPlayer.setOnInfoListener(mInfoListener);
            mCurrentBufferPercentage = 0;
            Log.w(TAG, "openVideo setDataSource()");

            /// M: [PERF.ADD] @{
            MoviePerfHelper.traceBegin("Begin setDataSource");
            /// @}
            mMediaPlayer.setDataSource(mContext, mUri, mHeaders);
            // / M: Handle for non-google notify messages make CTS fail@{
            mMediaPlayer.setParameter(KEY_PLAYBACK_PARAMETER, MTK_PLAYBACK_VALUE);
            // / @}
            /// M: [PERF.ADD] @{
            MoviePerfHelper.traceEnd();
            /// @}

            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            Log.w(TAG, "openVideo prepareAsync()");
            /// M: [PERF.ADD] @{
            MoviePerfHelper.traceBegin("Begin prepareAsync");
            /// @}
            mMediaPlayer.prepareAsync();
            /// M: [PERF.ADD] @{
            MoviePerfHelper.traceEnd();
            /// @}

            // we don't set the target state here either, but preserve the
            // target state that was there before.
            mCurrentState = STATE_PREPARING;

            for (Pair<InputStream, MediaFormat> pending : mPendingSubtitleTracks) {
                try {
                    mMediaPlayer.addSubtitleSource(pending.first,
                            pending.second);
                } catch (IllegalStateException e) {
                    mInfoListener.onInfo(mMediaPlayer,
                            MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE, 0);
                }
            }

            if (mEnableSlowMotionSpeed && mSlowMotionSpeed != 0) {
                MtkLog.i(TAG, "set slow motion speed when open video " + mSlowMotionSpeed);
                mMediaPlayer.setParameter(KEY_SLOW_MOTION_SPEED, mSlowMotionSpeed);
            }
            if (mSlowMotionSection != null) {
                MtkLog.i(TAG, "set slow motion section when open video " + mSlowMotionSection);
                mMediaPlayer.setParameter(KEY_SLOW_MOTION_SECTION, mSlowMotionSection);
            }
            //attachMediaController();
        } catch (final IOException ex) {
            Log.w(TAG, "IOException, Unable to open content: " + mUri, ex);
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } catch (final IllegalArgumentException ex) {
            Log.w(TAG, "IllegalArgumentException, Unable to open content: " + mUri, ex);
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } catch (final SQLiteException ex) {
            Log.w(TAG, "SQLiteException, Unable to open content: " + mUri, ex);
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } catch (final IllegalStateException ex) {
            Log.w(TAG, "IllegalStateException, Unable to open content: " + mUri, ex);
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } finally {
            mPendingSubtitleTracks.clear();
        }

        if (LOG) {
            Log.v(TAG, "openVideo() mUri=" + mUri + ", mSurfaceHolder=" + mSurfaceHolder
                    + ", mSeekWhenPrepared=" + mSeekWhenPrepared
                    + ", mMediaPlayer=" + mMediaPlayer);
        }
    }

    private void doPrepared(final MediaPlayer mp) {
        if (LOG) {
            MtkLog.v(TAG, "doPrepared(" + mp + ") start");
        }
        mCurrentState = STATE_PREPARED;
        if (mOnPreparedListener != null) {
            mOnPreparedListener.onPrepared(mMediaPlayer);
        }
        mVideoWidth = mp.getVideoWidth();
        mVideoHeight = mp.getVideoHeight();

        // getDuration() before playback start
        getDuration();

        //Add for Video Zoom.
        if (mZoomController != null) {
            mZoomController.setVideoSize(mVideoWidth, mVideoHeight);
        }

        // mSeekWhenPrepared may be changed after seekTo() call
        final int seekToPosition = mSeekWhenPrepared;
        if (seekToPosition != 0) {
            seekTo(seekToPosition);
        }
        if (mVideoWidth != 0 && mVideoHeight != 0) {
            getHolder().setFixedSize(mVideoWidth, mVideoHeight);
        }

        /// M: [PERF.ADD] @{
        MoviePerfHelper.trigger("doPrepared", mTargetState);
        /// @}

        if (mTargetState == STATE_PLAYING) {
            start();
        }
        if (LOG) {
            MtkLog.v(TAG, "doPrepared() end video size: " + mVideoWidth + "," + mVideoHeight
                    + ", mTargetState=" + mTargetState + ", mCurrentState=" + mCurrentState);
        }
    }

    private boolean mOnResumed;
    /**
     * surfaceCreate will invoke openVideo after the activity stoped.
     * Here set this flag to avoid openVideo after the activity stoped.
     * @param resume
     */
    public void setResumed(final boolean resume) {
        if (LOG) {
            MtkLog.v(TAG,
                "setResumed(" + resume + ") mUri=" + mUri + ", mOnResumed=" + mOnResumed);
        }
        mOnResumed = resume;
    }


    @Override
    public void start() {
        if (isInPlaybackState()) {
            /// M: [PERF.ADD] @{
            MoviePerfHelper.trigger("Call Media Player start");
            /// @}
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
        }
        mTargetState = STATE_PLAYING;
    }

    @Override
    public void resume() {
        if (LOG) {
            MtkLog.v(TAG, "resume() mTargetState=" + mTargetState
                + ", mCurrentState=" + mCurrentState);
        }
        setResumed(true);
        openVideo();
    }

    @Override
    public void suspend() {
        if (LOG) {
            MtkLog.v(TAG, "suspend() mTargetState=" + mTargetState
                + ", mCurrentState=" + mCurrentState);
        }
        setResumed(false);
        super.suspend();
    }

    public void setOnInfoListener(final OnInfoListener l) {
        mOnInfoListener = l;
        if (LOG) {
            MtkLog.v(TAG, "setInfoListener(" + l + ")");
        }
    }

   //FOR MTK_SUBTITLE_SUPPORT
    public void setOnTimedTextListener(final OnTimedTextListener l) {
        mOnTimedTextListener = l;
        if (LOG) {
            MtkLog.v(TAG, "audioAndSubtltle setOnTimedTextListener(" + l + ")");
        }
    }

    public void setOnBufferingUpdateListener(final OnBufferingUpdateListener l) {
        mOnBufferingUpdateListener = l;
        if (LOG) {
            MtkLog.v(TAG, "setOnBufferingUpdateListener(" + l + ")");
        }
    }

    public void setOnVideoSizeChangedListener(final OnVideoSizeChangedListener l) {
        mVideoSizeListener = l;
        if (LOG) {
            Log.i(TAG, "setOnVideoSizeChangedListener(" + l + ")");
        }
    }

    @Override
    public int getCurrentPosition() {
        int position = 0;
        if (mSeekWhenPrepared > 0) {
            //if connecting error before seek,
            //we should remember this position for retry
            position = mSeekWhenPrepared;
        } else if (isInPlaybackState()) {
            MtkLog.v(TAG, "mCurrentState = " + mCurrentState);
            position = mMediaPlayer.getCurrentPosition();
        }
        if (LOG) {
            MtkLog.v(TAG, "getCurrentPosition() return " + position
                    + ", mSeekWhenPrepared=" + mSeekWhenPrepared);
        }
        return position;
    }

    //clear the seek position any way.
    //this will effect the case: stop video before it's seek completed.
    public void clearSeek() {
        if (LOG) {
            MtkLog.v(TAG, "clearSeek() mSeekWhenPrepared=" + mSeekWhenPrepared);
        }
        mSeekWhenPrepared = 0;
    }

    public boolean isTargetPlaying() {
        if (LOG) {
            Log.v(TAG, "isTargetPlaying() mTargetState=" + mTargetState);
        }
        return mTargetState == STATE_PLAYING;
    }

    public boolean isCurrentPlaying() {
        if (LOG) {
            Log.v(TAG, "isCurrentPlaying() mCurrentState=" + mCurrentState);
        }
        return mCurrentState == STATE_PLAYING;
    }

    public void dump() {
        if (LOG) {
            Log.v(TAG, "dump() mUri=" + mUri
                    + ", mTargetState=" + mTargetState + ", mCurrentState=" + mCurrentState
                    + ", mSeekWhenPrepared=" + mSeekWhenPrepared
                    + ", mVideoWidth=" + mVideoWidth + ", mVideoHeight=" + mVideoHeight
                    + ", mMediaPlayer=" + mMediaPlayer + ", mSurfaceHolder=" + mSurfaceHolder);
        }
    }

    @Override
    public void seekTo(final int msec) {
        if (LOG) {
            Log.v(TAG, "seekTo(" + msec + ") isInPlaybackState()=" + isInPlaybackState());
        }
        super.seekTo(msec);
    }

    @Override
    public void stopPlayback() {
        if (LOG) {
            Log.v(TAG, "stopPlayback");
        }
        super.stopPlayback();
        mSlowMotionSection = null;
        mEnableSlowMotionSpeed = false;
    }

    @Override
    protected void release(final boolean cleartargetstate) {
        if (LOG) {
            Log.v(TAG, "release(" + cleartargetstate + ") mMediaPlayer=" + mMediaPlayer);
        }
        super.release(cleartargetstate);

    }

    //for duration displayed
    public void setDuration(final int duration) {
        if (LOG) {
            Log.v(TAG, "setDuration(" + duration + ")");
        }
        mDuration = (duration > 0 ? -duration : duration);
    }

    @Override
    public int getDuration() {
        final boolean inPlaybackState = isInPlaybackState();
        if (LOG) {
            Log.v(TAG, "getDuration() mDuration=" + mDuration
                + ", inPlaybackState=" + inPlaybackState);
        }
        if (inPlaybackState) {
            if (mDuration > 0) {
                return mDuration;
            }
            mDuration = mMediaPlayer.getDuration();
            if (LOG) {
                Log.v(TAG, "getDuration() when mDuration<0, mMediaPlayer.getDuration() is "
                        + mDuration);
            }
            return mDuration;
        }
        //mDuration = -1;
        return mDuration;
    }

    public void setSlowMotionSpeed(int speed) {
        if (LOG) {
            MtkLog.v(TAG, "setSlowMotionSpeed(" + speed + ") mEnableSlowMotionSpeed = "
                    + mEnableSlowMotionSpeed);
        }
        if (mMediaPlayer != null && mEnableSlowMotionSpeed && speed != 0) {
            mMediaPlayer.setParameter(KEY_SLOW_MOTION_SPEED, speed);
        }
        mSlowMotionSpeed = speed;
    }

    public void setSlowMotionSection(String section) {
        MtkLog.v(TAG, "setSlowMotionSection(" + section + ")");
        if (mMediaPlayer != null) {
            setParameter(KEY_SLOW_MOTION_SECTION, section);
        } else {
            mSlowMotionSection = section;
        }
    }

    public void enableSlowMotionSpeed() {
        MtkLog.v(TAG, "enableSlowMotionSpeed mEnableSlowMotionSpeed " + mEnableSlowMotionSpeed);
        if (!mEnableSlowMotionSpeed) {
            mEnableSlowMotionSpeed = true;
            setSlowMotionSpeed(mSlowMotionSpeed);
        }
    }

    public void disableSlowMotionSpeed() {
        MtkLog.v(TAG, "disableSlowMotionSpeed mEnableSlowMotionSpeed " + mEnableSlowMotionSpeed);
        if (mEnableSlowMotionSpeed) {
            if (mMediaPlayer != null) {
                mMediaPlayer.setParameter(KEY_SLOW_MOTION_SPEED, 1);
            }
            mEnableSlowMotionSpeed = false;
        }
    }

    public void clearDuration() {
        if (LOG) {
            MtkLog.v(TAG, "clearDuration() mDuration=" + mDuration);
        }
        mDuration = -1;
    }

    //for video size changed before started issue
    private static final int MSG_LAYOUT_READY = 1;
    private boolean mNeedWaitLayout = false;
    private final Handler mHandler = new Handler() {
        public void handleMessage(final Message msg) {
            if (LOG) {
                MtkLog.v(TAG, "handleMessage() to do prepare. msg=" + msg);
            }
            switch(msg.what) {
            case MSG_LAYOUT_READY:
                if (mMediaPlayer == null || mUri == null) {
                    MtkLog.w(TAG, "Cannot prepare play! mMediaPlayer="
                        + mMediaPlayer + ", mUri=" + mUri);
                } else {
                    doPreparedIfReady(mMediaPlayer);
                }
                break;
            default:
                MtkLog.w(TAG, "Unhandled message " + msg);
                break;
            }
        }
    };

    private ScreenModeManager mScreenManager;
    public void setScreenModeManager(final ScreenModeManager manager) {
        mScreenManager = manager;
        if (mScreenManager != null) {
            mScreenManager.addListener(this);
        }
        if (LOG) {
            MtkLog.v(TAG, "setScreenModeManager(" + manager + ")");
        }
    }

    @Override
    public void onScreenModeChanged(final int newMode) {
        this.requestLayout();
    }

 // : MTK_SUBTITLE_SUPPORT & MTK_AUDIO_CHANGE_SUPPORT
    ///@{
    private static final int RETURN_OK = 0;
    private static final int RETURN_ERROR = -1;
    private static final int RETURN_ILL_STATE = -2;

    /**
     *  get Track Information from mediaPlayer
     */
    public TrackInfo[] getTrackInfo() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.getTrackInfo();
        } else {
            return null;
        }
    }

    /**
     * select the Track of the index
     * @param index
     * @return
     */
    public int selectTrack(int index) {
        try {
            Log.d(TAG, "AudioAndSubtitle selectTrack: before selectTrack index = " + index);
            mMediaPlayer.selectTrack(index);
            Log.d(TAG, "AudioAndSubtitle selectTrack: after selectTrack index = " + index);
        } catch (final IllegalStateException ex) {
            Log.w(TAG, "AudioAndSubtitle selectTrack: Unable to selectTrack"
                + " IllegalStateException: index  " + index, ex);
            return RETURN_ILL_STATE;
        } catch (final Exception ex) {
            Log.w(TAG, "AudioAndSubtitle selectTrack: Unable to selectTrack"
                + " Exception: index  " + index, ex);
            return RETURN_ERROR;
        }
        return RETURN_OK;
    }

   /**
    * deselect the Track of the index
    * @param index
    */
    public void deselectTrack(int index) {
        try {
            Log.d(TAG, "AudioAndSubtitle deselectTrack: before deselectTrack index = " + index);
            mMediaPlayer.deselectTrack(index);
            Log.d(TAG, "AudioAndSubtitle deselectTrack: after deselectTrack index = " + index);
        } catch (final IllegalStateException ex) {
            Log.w(TAG, "AudioAndSubtitle deselectTrack: Unable to deselectTrack"
                + " IllegalStateException: index = " + index, ex);
            return;
        }
    }

    /**
     * add the external subtitle source file
     * @param path the absolute file path
     * @param mimeType the mime type of the file
     */
    public void addExtTimedTextSource(String path, String mimeType) {
        try {
            Log.d(TAG, "AudioAndSubtitle addExtTimedTextSource:"
                        + " before addExtTimedTextSource path = " + path);
            mMediaPlayer.addTimedTextSource(path, mimeType);
            Log.d(TAG, "AudioAndSubtitle addExtTimedTextSource:" +
                          " after addExtTimedTextSource path = " + path);
        } catch (final IllegalStateException ex) {
            Log.w(TAG, "AudioAndSubtitle addExtTimedTextSource: Unable to addExtTimedTextSource"
                + " IllegalStateException: path  " + path, ex);
            return;
        } catch (final IOException ex) {
            Log.w(TAG, "AudioAndSubtitle addExtTimedTextSource: Unable to addExtTimedTextSource"
                + " IOException: path  " + path, ex);
            return;
        } catch (final IllegalArgumentException ex) {
            Log.w(TAG, "AudioAndSubtitle addExtTimedTextSource: Unable to addExtTimedTextSource"
                + " IllegalArgumentException: path  " + path, ex);
            return;
        }
    }
    ///@}

    @Override
    public int getAudioSessionId() {
        return super.getAudioSessionId();
    }

    @Override
    public boolean isInPlaybackState() {
        return super.isInPlaybackState();
    }

    public String getStringParameter(int key) {
        if (mMediaPlayer != null) {
            return mMediaPlayer.getStringParameter(key);
        }
        return null;
    }


    public boolean setParameter(int key, String value) {
        MtkLog.v(TAG, "setParameter(" + value + ")" + " mMediaPlayer " + mMediaPlayer);
        if (mMediaPlayer != null) {
            return mMediaPlayer.setParameter(key, value);
        }
        return false;
    }

    @Override
    public boolean isInFilmMode() {
        return false;
    }
}
