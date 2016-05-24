package com.mediatek.galleryfeature.raw;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.gallery3d.R;
import com.mediatek.galleryframework.base.Layer;
import com.mediatek.galleryframework.base.MediaData;
import com.mediatek.galleryframework.base.Player;
import com.mediatek.galleryframework.gl.MGLView;
import com.mediatek.galleryframework.util.MtkLog;

/** When current photo is related with a .dng file,
 *  use this class to show raw indicator at the right-bottom corner,
 *  and show reminder toast.
 */
public class RawLayer extends Layer {
    private static final String TAG = "MtkGallery2/RawLayer";
    private static final String SHARED_PREFERENCES_RAW = "gallery_raw";
    private static final String REMINDER_NUM = "reminder_num";
    private static final int MAX_REMINDER_NUM = 5;

    private static boolean sIsFristTimeOnResume = true;

    private Activity mActivity;
    private ViewGroup mRawIndicator;
    private boolean mIsFilmMode;
    private MediaData mMediaData;
    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mSharePrefEditor;
    private boolean mBottomControlShowing = false;
    private boolean mIsRawReally = false;

    @Override
    public void onCreate(Activity activity, ViewGroup root) {
        MtkLog.i(TAG, "<onCreate>");
        mActivity = activity;
        LayoutInflater flater = LayoutInflater.from(activity);
        mRawIndicator = (ViewGroup) flater.inflate(R.layout.m_raw, null, false);
        mRawIndicator.setVisibility(View.INVISIBLE);
        mSharedPref = mActivity.getSharedPreferences(SHARED_PREFERENCES_RAW, Context.MODE_PRIVATE);
        mSharePrefEditor = mSharedPref.edit();
    }

    @Override
    public void onResume(boolean isFilmMode) {
        MtkLog.i(TAG, "<onResume>");
        mIsFilmMode = isFilmMode;
        updateIndicatorVisibility();
        if (sIsFristTimeOnResume && mIsRawReally) {
            showReminderIfNeed();
            sIsFristTimeOnResume = false;
        }
    }

    private void showReminderIfNeed() {
        int showCount = mSharedPref.getInt(REMINDER_NUM, 0);
        MtkLog.i(TAG, "<showReminderIfNeed> get showCount = " + showCount);
        if (showCount < MAX_REMINDER_NUM) {
            mSharePrefEditor.putInt(REMINDER_NUM, showCount + 1);
            mSharePrefEditor.commit();
            Toast.makeText(mActivity, R.string.m_raw_reminder, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPause() {
        MtkLog.i(TAG, "<onPause>");
        mRawIndicator.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onDestroy() {
        MtkLog.i(TAG, "<onDestroy>");
        sIsFristTimeOnResume = true;
    }

    @Override
    public void setData(MediaData data) {
        mMediaData = data;
        if (data != null) {
            mIsRawReally = RawHelper.isRawJpg(mMediaData)
                    && RawHelper.hasRawFile(mActivity, mMediaData.caption);
        } else {
            mIsRawReally = false;
        }
    }

    @Override
    public void setPlayer(Player player) {
    }

    @Override
    public View getView() {
        return mRawIndicator;
    }

    @Override
    public MGLView getMGLView() {
        return null;
    }

    @Override
    public void onChange(Player player, int what, int arg, Object obj) {
    }

    @Override
    public void onFilmModeChange(boolean isFilmMode) {
        mIsFilmMode = isFilmMode;
        updateIndicatorVisibility();
    }

    @Override
    public void onReceiveMessage(int message) {
        if (message == Layer.MSG_BOTTOM_CONTROL_SHOW) {
            mBottomControlShowing = true;
        } else if (message == Layer.MSG_BOTTOM_CONTROL_HIDE) {
            mBottomControlShowing = false;
        }
        updateIndicatorVisibility();
    }

    private void updateIndicatorVisibility() {
        if (mIsFilmMode || mBottomControlShowing || !mIsRawReally) {
            mRawIndicator.setVisibility(View.INVISIBLE);
        } else {
            mRawIndicator.setVisibility(View.VISIBLE);
        }
    }
}