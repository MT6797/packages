package com.mediatek.gallery3d.video;

import java.util.Locale;

import android.view.Menu;
import android.view.MenuItem;

import com.android.gallery3d.R;
import com.mediatek.galleryframework.util.MtkLog;

public class SlowMotionHooker extends MovieHooker {
    private static final String TAG = "Gallery2/SlowMotionHooker";

    private static final int MENU_SLOW_MOTION = 1;
    private static final int KEY_SLOW_MOTION_FPS = 2200;

    private IMtkVideoController mVideoView;
    private MenuItem mMenuSlowMotion;
    private int mCurrentSpeed;
    private int mNextSpeed;

    private int mSupportedFps;
    private int mCurrentSpeedIndex;
    private int[] mCurrentSpeedRange;

    private SlowMotionItem mSlowMotionItem;

    @Override
    public void setParameter(String key, Object value) {
        // TODO Auto-generated method stub
        super.setParameter(key, value);
        MtkLog.v(TAG, "setParameter(" + key + ", " + value + ")");
        if (value instanceof IMtkVideoController) {
            mVideoView = (IMtkVideoController) value;
            mVideoView.setSlowMotionSpeed(mCurrentSpeed);
            //When get a new videoview, should update current speed range.
            updateCurrentSpeedRange();
        }
    }

    private void updateCurrentSpeedRange() {
        mSupportedFps = getSupportedFps();
        mCurrentSpeedRange = SlowMotionItem.getSupportedSpeedRange(mSupportedFps);
        mCurrentSpeedIndex = SlowMotionItem.getCurrentSpeedIndex(
                mCurrentSpeedRange, mCurrentSpeed);
    }

    private void refreshSlowMotionSpeed(final int speed) {
        if (getMovieItem() != null) {
            mSlowMotionItem.updateItemUri(getMovieItem().getUri());
            mSlowMotionItem.setSpeed(speed);
            mSlowMotionItem.updateItemToDB();
        }
    }

    private void getCurrentSpeedIndex() {
        mSupportedFps = getSupportedFps();
        mCurrentSpeedRange = SlowMotionItem.getSupportedSpeedRange(mSupportedFps);
        mCurrentSpeedIndex = SlowMotionItem.getCurrentSpeedIndex(
                mCurrentSpeedRange, mCurrentSpeed);
        updateSlowMotionIcon(mCurrentSpeedIndex);
    }

    private void updateSlowMotionIcon(int index) {
        if (index < 0 || index > mCurrentSpeedRange.length) {
            MtkLog.v(TAG, "updateSlowMotionIcon index is invalide index = " + index);
            return;
        }
        if (mSlowMotionItem == null) {
            MtkLog.e(TAG, "updateSlowMotionIcon, mSlowMotionItem is null");
            return;
        }
        int speed = mCurrentSpeedRange[index];
        int speedResource = mSlowMotionItem.getSpeedIconResource(speed);
        MtkLog.v(TAG, "updateSlowMotionIcon(" + index + ")" + "speed " + speed
                + " speedResource " + speedResource);
        if (mMenuSlowMotion != null) {
            if (mSlowMotionItem.isSlowMotionVideo()) {
                mMenuSlowMotion.setIcon(speedResource);
                refreshSlowMotionSpeed(speed);
                mVideoView.setSlowMotionSpeed(speed);
                mMenuSlowMotion.setVisible(true);
            } else {
                mMenuSlowMotion.setVisible(false);
            }
        }
        mCurrentSpeed = speed;
    }

    private void initialSlowMotionIcon(final int speed) {
        MtkLog.v(TAG, "initialSlowMotionIcon() speed " + speed);
        if (mMenuSlowMotion != null) {
            mCurrentSpeed = speed;
            if (mCurrentSpeed != 0) {
                getCurrentSpeedIndex();
            } else {
                mMenuSlowMotion.setVisible(false);
            }
        }
    }

    @Override
    public void onMovieItemChanged(final IMovieItem item) {
        MtkLog.v(TAG, "onMovieItemChanged() " + mMenuSlowMotion);
        if (mMenuSlowMotion != null) {
            if (mSlowMotionItem == null) {
                mSlowMotionItem = new SlowMotionItem(getContext(),
                        item.getUri());
            } else {
                mSlowMotionItem.updateItemUri(item.getUri());
            }
            initialSlowMotionIcon(mSlowMotionItem.getSpeed());
        }
    }


    @Override
    public void setVisibility(boolean visible) {
        if (mMenuSlowMotion != null && mSlowMotionItem != null
                && mSlowMotionItem.isSlowMotionVideo()
                && mSupportedFps != -1) {
            mMenuSlowMotion.setVisible(visible);
            MtkLog.v(TAG, "setVisibility() visible=" + visible);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        MtkLog.v(TAG, "onCreateOptionsMenu()");
        mMenuSlowMotion = menu.add(MENU_HOOKER_GROUP_ID, getMenuActivityId(MENU_SLOW_MOTION), 0,
                R.string.slow_motion_speed);
        mMenuSlowMotion.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        if (getMovieItem() != null) {
            mSlowMotionItem = new SlowMotionItem(getContext(), getMovieItem().getUri());
            if (MtkVideoFeature.isForceAllVideoAsSlowMotion()) {
                if (mSlowMotionItem.getSpeed() == 0) {
                    initialSlowMotionIcon(SlowMotionItem.SLOW_MOTION_QUARTER_SPEED);

                } else {
                    initialSlowMotionIcon(mSlowMotionItem.getSpeed());
                }
            } else {
                initialSlowMotionIcon(mSlowMotionItem.getSpeed());
            }
        } else {
            mMenuSlowMotion.setVisible(false);
        }
        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MtkLog.v(TAG, "onPrepareOptionsMenu()");
        if (mSlowMotionItem != null && !mSlowMotionItem.isSlowMotionVideo()) {
            mMenuSlowMotion.setVisible(false);
        } else if (mSupportedFps == -1) {
            getCurrentSpeedIndex();
        }
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (getMenuOriginalId(item.getItemId())) {
        case MENU_SLOW_MOTION:
            MtkLog.v(TAG, "onOptionsItemSelected()");
            if (mSupportedFps == -1) {
                getCurrentSpeedIndex();
            }
            mCurrentSpeedIndex++;
            int index = mCurrentSpeedIndex % mCurrentSpeedRange.length;
            updateSlowMotionIcon(index);
            return true;
        default:
            return false;
        }
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
