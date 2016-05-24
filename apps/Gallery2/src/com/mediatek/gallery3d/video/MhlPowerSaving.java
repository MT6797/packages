/*
* Copyright (C) 2016 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.mediatek.gallery3d.video;

import com.android.gallery3d.R;
import com.android.gallery3d.app.Log;
import com.android.gallery3d.app.MovieActivity;
import com.android.gallery3d.app.MoviePlayer;
import com.android.gallery3d.common.ApiHelper;
import com.mediatek.galleryframework.util.MtkLog;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Presentation;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplayStatus;
import android.media.MediaRouter;
import android.media.MediaRouter.RouteInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Display;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.Settings.System;

public class MhlPowerSaving {
    private static final String TAG = "Gallery2/MhlPowerSaving";
    private final IMtkVideoController mVideoView;
    private final View mRootView;
    private final View mVideoRoot;
    private final Handler mHandler;
    private MediaRouter mMediaRouter;
    private MediaRouter.RouteInfo mRouteInfo;
    private Display presentationDisplay;
    private MhlPresentation mPresentation;
    private PowerManager mPowerManager;
    private Window mWindow;
    private float mScreenBrightness;
    private Activity mMovieActivity;
    private int mLastSystemUiVis = 0;
    private boolean mIsAutoBrithtness = true;
    private static final int MHL_DISPLAY_STATE_NOT_CONNECTED = 0;
    private static final int MHL_DISPLAY_STATE_CONNECTED = 1;
    private static final int MHL_CONNECTED_DELAY = 500;
    private static final int MHL_DISCONNECTED_DELAY = 1000;
    private static final int MHL_SET_BACKLIGHT_OFF_DELAY = 10000;
    private static final int MHL_CREATE_ROUTE_RETYR_TIMES = 3;
    //record MHL is connected or not
    private boolean mIsMhlConnected = false;
    ///M: @{ record current position and if just quit from MHL for restore play
    private int mhlCurrentPosition = 0;
    private boolean mIsSurfaceViewRemoved = false;
    private boolean mIsPlayingBeforeEnterExtensionMode = true;
    /// @}
    private boolean mIsMhlReceiverRegistered = false;
    private int mCreatedRouteTimes = 0;

    public MhlPowerSaving(final View rootView, final Activity activity,
                                final IMtkVideoController videoview, final Handler handler) {
        MtkLog.v(TAG, "MhlPowerSaving contrustor");
        mPowerManager = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        mWindow = activity.getWindow();
        // Get the media router service.
        mMediaRouter = (MediaRouter) activity.getSystemService(Context.MEDIA_ROUTER_SERVICE);
        mRootView = rootView;
        mVideoRoot = rootView.findViewById(R.id.video_root);
        mVideoView = videoview;
        mMovieActivity = activity;
        mHandler = new Handler(mMovieActivity.getMainLooper());
        // Get the brightness mode
        mIsAutoBrithtness = getBrightnessMode();
        mIsSurfaceViewRemoved = false;
        mIsMhlConnected = false;
    }

    public boolean isMhlConnected() {
        return mIsMhlConnected;
    }

    public boolean isSurfaceRemoved() {
        return mIsSurfaceViewRemoved;
    }

    public void setIsSurfaceRemoved(boolean isSurfaceRemoved) {
        mIsSurfaceViewRemoved = isSurfaceRemoved;
    }

    public int getMhlCurrentPosition() {
        return mhlCurrentPosition;
    }

    public boolean isPlayingBeforeExtMode() {
        return mIsPlayingBeforeEnterExtensionMode;
    }

    public void refreshMhlPara() {
        mIsAutoBrithtness = getBrightnessMode();
        mIsSurfaceViewRemoved = false;
        mIsMhlConnected = false;
    }

    public void registerMhlReceiver() {
        MtkLog.v(TAG, "registerMhlReceiver()" + "mIsMhlReceiverRegistered"
            + mIsMhlReceiverRegistered);
        IntentFilter mhlFilter = new IntentFilter(Intent.ACTION_HDMI_PLUG);
        if (!mIsMhlReceiverRegistered) {
            mMovieActivity.registerReceiver(mMhlReceiver, mhlFilter);
            mIsMhlReceiverRegistered = true;
        }
    }

    public void unregisterMhlReceiver() {
        MtkLog.v(TAG, "unregisterMhlReceiver()" + "mIsMhlReceiverRegistered"
            + mIsMhlReceiverRegistered);
        if (mIsMhlReceiverRegistered) {
            mMovieActivity.unregisterReceiver(mMhlReceiver);
            mIsMhlReceiverRegistered = false;
        }
    }

    public Activity getCurrentActivity() {
        return mMovieActivity;
    }

    public void showController() {
    }
    public void restoreSystemUiListener() {
    }

    //for OnPause case: remove video view from presentation and add it to local video
    public void dismissPresentaion() {
        MtkLog.v(TAG, "dismissPresentaion()");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mPresentation != null) {
                    Log.i(TAG,"Dismissing presentation because video player quite");
                    mPresentation.removeSurfaceView();
                    mPresentation.dismiss();
                    mPresentation = null;
                    ((ViewGroup) mVideoRoot).addView((View) mVideoView, 0);
                    unregisterOnSystemUiVisibilityChangeListener();
                }
            }
        });
    }

    private boolean getBrightnessMode(){
        int BrightnessMode = Settings.System.getInt(mMovieActivity.getContentResolver(),
                System.SCREEN_BRIGHTNESS_MODE, System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        if (BrightnessMode == System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            return true;
        } else {
            return false;
        }
    }

    private BroadcastReceiver mMhlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            MtkLog.v(TAG, "mMhlReceiver onReceive action: " + action);
            if (action != null && action.equals(Intent.ACTION_HDMI_PLUG)) {
                int state = intent.getIntExtra("state", MHL_DISPLAY_STATE_NOT_CONNECTED);
                MtkLog.v(TAG, "MHL state = " + state);
                if (state == MHL_DISPLAY_STATE_NOT_CONNECTED) {
                    mIsMhlConnected = false;
                    leaveMhlExtensionMode();
                } else {
                    mIsMhlConnected = true;
                    enterMhlExtensionMode();
                }
            }
        }
    };

    private final Runnable mMhlSetBacklightOffRunnable = new Runnable() {
        @Override
        public void run() {
            MtkLog.v(TAG, "mMhlSetBacklightOffRunnable run");
            mMovieActivity.closeOptionsMenu();
            mPowerManager.setBacklightOffForWfd(true);
        }
    };

    private final Runnable mSelectMediaRouteRunnable = new Runnable() {
        @Override
        public void run() {
            mCreatedRouteTimes++;
            // Get the current route and its presentation display.
            mRouteInfo = mMediaRouter.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_VIDEO);
            presentationDisplay = mRouteInfo != null ? mRouteInfo.getPresentationDisplay() : null;
            if (presentationDisplay == null) {
                mHandler.postDelayed(mSelectMediaRouteRunnable, MHL_CONNECTED_DELAY);
            } else if (presentationDisplay != null || mCreatedRouteTimes >= MHL_CREATE_ROUTE_RETYR_TIMES ) {
                mCreatedRouteTimes = 0;
                updatePresentation();
            }
            MtkLog.v(TAG, "mCreateMediaRouteRunnable" + " mCreatedRouteTimes = " + mCreatedRouteTimes
                     + " presentationDisplay = " + presentationDisplay);
        }
    };

    private final Runnable mUnselectMediaRouteRunnable = new Runnable() {
        @Override
        public void run() {
            MtkLog.v(TAG, "mMhlUpdatePresentationRunnable");
            // Get the current route and its presentation display.
            mRouteInfo = mMediaRouter.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_VIDEO);
            presentationDisplay = mRouteInfo != null ? mRouteInfo.getPresentationDisplay() : null;
            updatePresentation();
        }
    };

    ///M: for remove MHL @{
    public void leaveMhlExtensionMode() {
        MtkLog.v(TAG, "leaveMhlExtensionMode()" + "mPresentation = " + mPresentation);
        // keep current position before surface view removed for restore playback
        mhlCurrentPosition = mVideoView.getCurrentPosition();
        mHandler.removeCallbacks(mUnselectMediaRouteRunnable);
        //delay to wait Medie Router disconnect
        mHandler.postDelayed(mUnselectMediaRouteRunnable, MHL_DISCONNECTED_DELAY);
        cancelCountDown();
    }

    public void cancelCountDown() {
        //restore brightness model, and turn on backlight
        MtkLog.v(TAG, "cancelCountDown()");
        mHandler.removeCallbacks(mMhlSetBacklightOffRunnable);
        if (mIsAutoBrithtness) {
            //When leave MHL restore brightness mode
            Settings.System.putInt(mMovieActivity.getContentResolver(),
            System.SCREEN_BRIGHTNESS_MODE, System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        }
        showController();
        mPowerManager.setBacklightOffForWfd(false);
    }
    /// @}

    ///M: for insert MHL @{
    private void enterMhlExtensionMode() {
        MtkLog.v(TAG, "enterMhlExtensionMode()");
        mHandler.removeCallbacks(mSelectMediaRouteRunnable);
        //connect Medie Router
        mHandler.post(mSelectMediaRouteRunnable);
        setOnSystemUiVisibilityChangeListener();
        if (mVideoView.isPlaying()) {
            startCountDown();
        }
    }

    public void startCountDown() {
        //turn off auto brightness, and start to turn off backlight
        MtkLog.v(TAG, "startCountDown", new Throwable());
        mHandler.removeCallbacks(mMhlSetBacklightOffRunnable);
        //If the brightness mode is auto, turn to manual
        if (mIsAutoBrithtness) {
            Settings.System.putInt(mMovieActivity.getContentResolver(),
            System.SCREEN_BRIGHTNESS_MODE, System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        }
        mHandler.postDelayed(mMhlSetBacklightOffRunnable, MHL_SET_BACKLIGHT_OFF_DELAY);
    }
    /// @}

    private void unregisterOnSystemUiVisibilityChangeListener() {
        mRootView.setOnSystemUiVisibilityChangeListener(null);
        restoreSystemUiListener();
    }

    private void updatePresentation() {
        MtkLog.v(TAG, "updatePresentation" + " mPresentation = " + mPresentation
                 + " presentationDisplay = " + presentationDisplay);

        // Dismiss the current presentation if the display has changed.
        if (mPresentation != null && mPresentation.getDisplay() != presentationDisplay) {
            Log.i(TAG,"Dismissing presentation because the current route no longer "
                    + "has a presentation display.");
            mPresentation.removeSurfaceView();
            mPresentation.dismiss();
            mPresentation = null;
            ((ViewGroup) mVideoRoot).addView((View) mVideoView, 0);
            unregisterOnSystemUiVisibilityChangeListener();
        }

        // Show a new presentation if needed.
        if (mPresentation == null && presentationDisplay != null) {
            Log.i(TAG, "Showing presentation on display:" + presentationDisplay);
            if (((View) mVideoView).getParent() != null) {
                // keep current position before surface view removed for restore playback
                mhlCurrentPosition = mVideoView.getCurrentPosition();
                mIsPlayingBeforeEnterExtensionMode = mVideoView.isPlaying();
                Log.d(TAG,"mhlCurrentPosition= " + mhlCurrentPosition);
                ((ViewGroup) ((View) mVideoView).getParent()).removeView((View) mVideoView);
                mIsSurfaceViewRemoved = true;
            }
            mPresentation = new MhlPresentation(mMovieActivity, presentationDisplay, mVideoView);
            //mPresentation.setOnDismissListener(mOnDismissListener);
            try {
                mPresentation.show();
            } catch (WindowManager.InvalidDisplayException ex) {
                Log.w(TAG, "Couldn't show presentation!  Display was removed in " + "the meantime.", ex);
                mPresentation = null;
            }
        }
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setOnSystemUiVisibilityChangeListener() {
        if (!ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_HIDE_NAVIGATION) return;
        // When the user touches the screen or uses some hard key, the framework
        // will change system ui visibility from invisible to visible. We show
        // the media control and enable system UI (e.g. ActionBar) to be visible at this point
        MtkLog.v(TAG, "setOnSystemUiVisibilityChangeListener");
        mRootView.setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                int diff = mLastSystemUiVis ^ visibility;
                MtkLog.v(TAG, "onSystemUiVisibilityChange(" + mLastSystemUiVis + ")");
                mLastSystemUiVis = visibility;
                if ((diff & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0
                        && (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0
                        || (diff & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0
                        && (visibility & View.SYSTEM_UI_FLAG_LOW_PROFILE) == 0) {
                    showController();
                }
                MtkLog.v(TAG, "onSystemUiVisibilityChange(" + visibility + ")");
            }
        });
    }

    /**
    * The presentation to show on the secondary display.
    * <p>
    * Note that this display may have different metrics from the display on which
    * the main activity is showing so we must be careful to use the presentation's
    * own {@link Context} whenever we load resources.
    * </p>
    */
    private final class MhlPresentation extends Presentation {
        private IMtkVideoController mSurfaceView;
        private TextView mTextView;
        private View mRootView;
        private RelativeLayout mRoot;

        public MhlPresentation(Context context, Display display) {
            super(context, display);
        }

        public MhlPresentation(Context context, Display display, IMtkVideoController surfaceview) {
            super(context, display);
            Log.d(TAG, "MhlPresentation construct");
            mSurfaceView = surfaceview;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            // Be sure to call the super class.
            super.onCreate(savedInstanceState);
            Log.d(TAG, "WfdPresentation onCreate");
            // Inflate the layout.
            setContentView(R.layout.m_presentation_with_media_router_content);
            mRoot = (RelativeLayout) findViewById(R.id.view_root);
            RelativeLayout.LayoutParams wrapContent = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
                wrapContent.addRule(RelativeLayout.CENTER_IN_PARENT);
                mRoot.addView((View) mSurfaceView, wrapContent);
            }

        public void removeSurfaceView() {
            ((ViewGroup) mRoot).removeView((View) mSurfaceView);
            mIsSurfaceViewRemoved = true;
            Log.d(TAG, "removeSurfaceView()" + " mhlCurrentPosition= " + mhlCurrentPosition);
        }
    }
}
