package com.mediatek.gallery3d.video;


import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView.ScaleType;


import com.android.gallery3d.R;
import com.android.gallery3d.app.MovieActivity;
import com.mediatek.galleryframework.util.MtkLog;

public class SlowMotionController extends ViewGroup {

    private int mActionBarHeight;
    private Context mContext;
    private SlowMotionBar mSlowMotionBar;
    private SlowMotionSpeed mSlowMotionSpeed;
    final private boolean mHasRewindAndForward;
    private Rect mWindowInsets;
    View mActionBarView;
    //for speed view.
    private static final int MARGIN = 10; // dip
    private static final int KEY_SLOW_MOTION_FPS = 2200;
    private int mSpeedPadding;
    private int mSpeedWidth;
    private static final String TAG = "Gallery2/SlowMotionController";


    public SlowMotionController(Context context, SlowMotionBar.Listener listener) {
        super(context);
        mContext = context;
        setBackgroundResource(R.drawable.actionbar_translucent);
        mHasRewindAndForward = MtkVideoFeature.isRewindAndForwardSupport(context);

        //for speedview layout
        if (mHasRewindAndForward) {
            mSpeedWidth = 0;
            mSpeedPadding = 0;
        } else {
            DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
            Bitmap screenButton = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.m_ic_media_bigscreen);
            mSpeedWidth = screenButton.getWidth();
            mSpeedPadding = (int) (metrics.density * MARGIN);
            screenButton.recycle();
        }

        mSlowMotionBar = new SlowMotionBar(context, listener, getSpeedViewPadding());
            LayoutParams matchParent =
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

            LayoutParams wrapContent = new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT);
            addView(mSlowMotionBar, matchParent);
            if (!mHasRewindAndForward) {
                mSlowMotionSpeed = new SlowMotionSpeed(context);
        }
        getActionBarHeight();
    }

    // refresh slowmotion speed icon
    public void refreshSlowMotionSpeed() {
        if (mSlowMotionSpeed != null) {
            mSlowMotionSpeed.getCurrentSpeedIndex();
        }
    }

    public void refreshMovieInfo(IMovieItem info) {
        mSlowMotionBar.refreshMovieInfo(info);
        if (mSlowMotionSpeed != null) {
            mSlowMotionSpeed.refreshMovieInfo(info);
        }
    }

    public void setSlideVideoTexture(IMtkVideoController texture) {
        mSlowMotionBar.setSlideVideoTexture(texture);
        if (mSlowMotionSpeed != null) {
            mSlowMotionSpeed.setSlideVideoTexture(texture);
        }
    }

    public void setTime(int currentTime, int totalTime) {
        mSlowMotionBar.setTime(currentTime, totalTime);
    }

    public void setScrubbing(boolean enable) {
        mSlowMotionBar.setScrubbing(enable);
    }

    public void onContextChange(Context context) {
        mContext = context;
        getActionBarHeight();
    }

    @Override
    public void setVisibility(int visibility) {
        // TODO Auto-generated method stub
        if (mSlowMotionBar.setBarVisibility(visibility)) {
            //if return true from slow motion bar, indicator current video is slow motion video.
            if (mSlowMotionSpeed != null) {
                mSlowMotionSpeed.setVisibility(visibility);
            }
            super.setVisibility(visibility);
        } else {
            //if return false from slow motion bar, indicator current video is normal video.
            // slow motion ui is invisible for normal video.
            if (mSlowMotionSpeed != null) {
                mSlowMotionSpeed.setVisibility(GONE);
            }
            super.setVisibility(GONE);
        }

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // TODO Auto-generated method stub
        int pl = mWindowInsets.left; // the left paddings
        int pr = mWindowInsets.right;
        int pt = mWindowInsets.top;
        int pb = mWindowInsets.bottom;

        if (mHasRewindAndForward) {
            if (mSlowMotionBar != null) {
                mSlowMotionBar.layout(pl + pr, 0, r - pr, b - t);
            }
        } else {
            if (mSlowMotionBar != null && mSlowMotionSpeed != null) {
                mSlowMotionBar.layout(pl, 0,
                        r - pr - mSlowMotionSpeed.getAddedRightPadding(), b - t);
                mSlowMotionSpeed.onLayout(r, pr, b - t);
            }
        }

    }

    private int getSpeedViewPadding() {
        return mSpeedPadding * 2 + mSpeedWidth;
    }


    //Get action bar height to layout slowmotion bar.
    private void getActionBarHeight() {
        Window window = ((Activity) mContext).getWindow();
        View v = window.getDecorView();
        mActionBarView = (View) v
                .findViewById(com.android.internal.R.id.action_bar);
        MtkLog.v(TAG, "MovieControllerOverlay mActionBarView = "
                + mActionBarView);
        ViewTreeObserver vto = mActionBarView.getViewTreeObserver();
        mActionBarHeight = ((Activity) mContext).getActionBar().getHeight();
        vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int height = ((Activity) mContext).getActionBar().getHeight();
                if (height != mActionBarHeight) {
                    mActionBarHeight = height;
                    SlowMotionController.this.requestLayout();
                    MtkLog.v(TAG, "onGlobalLayout action bar height = "
                            + ((Activity) mContext).getActionBar().getHeight());
                }
            }
        });
    }


    public void onLayout(Rect windowInsets, int w, int h) {
        mWindowInsets = windowInsets;
        MtkLog.v(TAG, "onLayout() windowInsets = " + windowInsets + " w = " + w + " h = " + h
                + " mActionBarHeight " + mActionBarHeight);
        super.layout(0, mActionBarHeight, w, 2 * mActionBarHeight);
    }

    class SlowMotionSpeed implements View.OnClickListener {
        private ImageView mSpeedView;

        private IMtkVideoController mVideoView;

        private int mCurrentSpeed;
        private int mNextSpeed;
        private IMovieItem mMovieItem;
        private SlowMotionItem mSlowMotionItem;

        private int mSupportedFps = -1;
        private int mCurrentSpeedIndex;
        private int[] mCurrentSpeedRange;

        public SlowMotionSpeed(Context context) {
            LayoutParams wrapContent =
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            //add speedView
            mSpeedView = new ImageView(context);
            mSpeedView.setScaleType(ScaleType.CENTER);
            mSpeedView.setFocusable(true);
            mSpeedView.setClickable(true);
            mSpeedView.setOnClickListener(this);
            addView(mSpeedView, wrapContent);

            if (mContext instanceof MovieActivity) {
                View rootView = ((Activity) mContext).findViewById(R.id.movie_view_root);
                mVideoView = (MTKVideoView) rootView.findViewById(R.id.surface_view);
                MtkLog.i(TAG, "SlowMotionBar mVideoView = " + mVideoView);
                mMovieItem = ((MovieActivity) mContext).getMovieItem();
                mSlowMotionItem = new SlowMotionItem(mContext, mMovieItem.getUri());
                mCurrentSpeed = mSlowMotionItem.getSpeed();
                if (MtkVideoFeature.isForceAllVideoAsSlowMotion()) {
                    if (mCurrentSpeed == 0) {
                        mCurrentSpeed = SlowMotionItem.SLOW_MOTION_QUARTER_SPEED;
                    }
                }
                getCurrentSpeedIndex();
                //update slow motion speed icon.
                if (mSlowMotionItem.isSlowMotionVideo()) {
                    int speedResource = mSlowMotionItem.getSpeedIconResource(mCurrentSpeed);
                    mSpeedView.setImageResource(speedResource);
                    mVideoView.setSlowMotionSpeed(mCurrentSpeed);
                } else {
                    setVisibility(GONE);
                }
            } else {
                setVisibility(GONE);
            }

        }

        public void refreshMovieInfo(IMovieItem info) {
            MtkLog.v(TAG, "refreshMovieInfo");
            mMovieItem = info;
            if (mSlowMotionItem != null) {
                mSlowMotionItem.updateItemUri(mMovieItem.getUri());
            } else {
                mSlowMotionItem = new SlowMotionItem(mContext, mMovieItem.getUri());
            }
            mCurrentSpeed = mSlowMotionItem.getSpeed();
            if (MtkVideoFeature.isForceAllVideoAsSlowMotion()) {
                if (mCurrentSpeed == 0) {
                    mCurrentSpeed = SlowMotionItem.SLOW_MOTION_QUARTER_SPEED;
                }
            }
            getCurrentSpeedIndex();

            if (mSupportedFps == -1
                    && mSlowMotionItem.isSlowMotionVideo()) {
                int speedResource = mSlowMotionItem.getSpeedIconResource(mCurrentSpeed);
                mSpeedView.setImageResource(speedResource);
                if (mContext instanceof MovieActivity) {
                    mVideoView.setSlowMotionSpeed(mCurrentSpeed);
                }
            }
        }

        public void getCurrentSpeedIndex() {
            mSupportedFps = getSupportedFps();
            mCurrentSpeedRange = SlowMotionItem.getSupportedSpeedRange(mSupportedFps);
            mCurrentSpeedIndex = SlowMotionItem.getCurrentSpeedIndex(
                    mCurrentSpeedRange, mCurrentSpeed);
            updateSlowMotionIcon(mCurrentSpeedIndex);
        }

        public void setSlideVideoTexture(IMtkVideoController texture) {
            mVideoView = texture;
            mVideoView.setSlowMotionSpeed(mCurrentSpeed);
        }

        private void refreshSlowMotionSpeed(final int speed) {
            if (mSlowMotionItem != null) {
                mSlowMotionItem.updateItemUri(mMovieItem.getUri());
                mSlowMotionItem.setSpeed(speed);
                mSlowMotionItem.updateItemToDB();
            }
        }

        private void updateSlowMotionIcon(int index) {
            if (index < 0 || index > mCurrentSpeedRange.length) {
                MtkLog.v(TAG, "updateSlowMotionIcon index is invalide index = " + index);
                return;
            }
            int speed = mCurrentSpeedRange[index];
            int speedResource = mSlowMotionItem.getSpeedIconResource(speed);
            MtkLog.v(TAG, "updateSlowMotionIcon(" + index + ")" + "speed " + speed
                    + " speedResource " + speedResource);
            if (mSpeedView != null) {
                if (mSlowMotionItem.isSlowMotionVideo() && mVideoView != null) {
                    mSpeedView.setImageResource(speedResource);
                    refreshSlowMotionSpeed(speed);
                    mVideoView.setSlowMotionSpeed(speed);
                } else {
                    mSpeedView.setVisibility(GONE);
                }
            }
            mCurrentSpeed = speed;
        }

        public void setVisibility(int visibility) {
            mSpeedView.setVisibility(visibility);
        }

        @Override
        public void onClick(View v) {
            MtkLog.v(TAG, "onClick()");
            if (mSupportedFps == -1) {
                getCurrentSpeedIndex();
            }
            mCurrentSpeedIndex++;
            int index = mCurrentSpeedIndex % mCurrentSpeedRange.length;
            updateSlowMotionIcon(index);
        }

        public void clearInfo() {

        }

        public void onHide() {
            mSpeedView.setVisibility(View.INVISIBLE);
        }
        public void onShow() {
            mSpeedView.setVisibility(View.VISIBLE);
        }
        public void onLayout(int width, int paddingRight, int yTop) {
            // layout screen view position
            int sw = getAddedRightPadding();
                mSpeedView.layout(width - paddingRight - sw, 0, width - paddingRight,
                        yTop);
        }

        public int getAddedRightPadding() {
            return getSpeedViewPadding();
        }

        private int getSupportedFps() {
            String fps = null;
            if (mVideoView != null) {
                fps = mVideoView.getStringParameter(KEY_SLOW_MOTION_FPS);
            }
            MtkLog.d(TAG, "get supported fps is " + fps);
            if (fps == null || SlowMotionItem.NORMAL_FPS == Integer.parseInt(fps)) {
                return -1;
            }
            return Integer.parseInt(fps);
        }
    }
}
