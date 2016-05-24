/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2015. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */
package com.mediatek.galleryfeature.stereo.freeview3d;

import android.app.DialogFragment;
import android.view.View;

import com.android.gallery3d.R;
import com.mediatek.galleryframework.base.BottomControlLayer;
import com.mediatek.galleryframework.base.Player;
import com.mediatek.galleryframework.util.MtkLog;
import com.mediatek.galleryframework.util.ProgressFragment;

import java.lang.ref.WeakReference;

/**
 * Display freeview3D for Depth image. Response for Gyrosensor and touch event.
 */
public class FreeView3DLayer extends BottomControlLayer {
    private final static String TAG = "MTKGallery2/FreeView3DLayer";
    private final static int DEFAULT_STEP = 5;
    private FreeView3DPlayer mPlayer;
    private float[] mTouchPoints = new float[] { 0, 0 };
    private int mStep = DEFAULT_STEP;
    private WeakReference<DialogFragment> mLoadingProgressDialog;

    @Override
    public void setPlayer(Player player) {
        super.setPlayer(player);
        MtkLog.d(TAG, "<setPlayer>  player=" + player);
        if (player == null) {
            mPlayer = null;
        } else if (player instanceof FreeView3DPlayer) {
            mPlayer = (FreeView3DPlayer) player;
            mPlayer.setMode(onFreeviewMode());
            mPlayer.enterTouchView((!supportGyroSensor())
                    && onStereoTouchView());
        } else {
            throw new IllegalArgumentException("<setPlayer>, wrong Player type");
        }
    }

    @Override
    public boolean onSingleTapUp(float x, float y) {
        MtkLog.d(TAG, "<onSingleTapUp> onSingleTapUp");
        mPlayer.mIsOnTouched = false;
        return super.onSingleTapUp(x, y);
    }

    @Override
    public boolean onScroll(float dx, float dy, float totalX, float totalY) {
        if (onFreeviewMode()) {
            float x = mTouchPoints[0] + totalX;
            float y = mTouchPoints[1] + totalY;
            if (mPlayer != null
                    && mPlayer.getCurrentState() == FreeView3DPlayer.STATE_START
                    && mPlayer.inBound(new float[] { x, y })) {
                float[] points = new float[] {
                        -dx * mPlayer.mRatio * mStep,
                        dy * mPlayer.mRatio * mStep };
                int[] movePoints = mPlayer.getSteps(points);
                float[] targetPoints = mPlayer.getAnimationTargetPoint();
                if (targetPoints != null) {
                    mPlayer.initAnimation(
                            (int) targetPoints[0] + movePoints[0],
                            (int) targetPoints[1] + movePoints[1]);
                }
            }
        }
        return super.onScroll(dx, dy, totalX, totalY);
    }

    @Override
    public void onDown(float x, float y) {
        mPlayer.mIsOnTouched = true;
        mTouchPoints[0] = x;
        mTouchPoints[1] = y;
        MtkLog.d(TAG, "<onDown> mTouchPoints[0] = " + mTouchPoints[0] + " "
                + mTouchPoints[1]);
    }

    @Override
    public void onUp() {
        MtkLog.d(TAG, "<onUp> onUp");
        mPlayer.mIsOnTouched = false;
        mTouchPoints[0] = 0;
        mTouchPoints[1] = 0;
    }

    @Override
    public void onClickEvent(View v) {
        super.onClickEvent(v);
        if (mPlayer != null) {
            mPlayer.setMode(onFreeviewMode());
            if (onStereoTouchView()) {
                mPlayer.showDialog();
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        boolean state = super.onBackPressed();
        if (mPlayer != null) {
            mPlayer.setMode(onFreeviewMode());
        }
        return state;
    }

    @Override
    public boolean onUpPressed() {
        return onBackPressed();
    }

    @Override
    public boolean supportStereo() {
        return true;
    }

    @Override
    public boolean supportGyroSensor() {
        if (mPlayer != null) {
            return mPlayer.hasGyroSensor();
        } else {
            return false;
        }
    }

    private boolean onFreeviewMode() {
        return (supportGyroSensor() && onStereoStartView())
                || (!supportGyroSensor() && onStereoTouchView());
    }

    @Override
    public boolean supportTouchFreeview() {
        return !supportGyroSensor();
    }

    @Override
    public void onPause() {
        hideLodingProgress();
    }

    @Override
    public void onChange(Player player, int what, int arg, Object obj) {
        switch (what) {
        case FreeView3DPlayer.MSG_HIDE_DIALOG:
            hideLodingProgress();
            break;
        case FreeView3DPlayer.MSG_SHOW_DIALOG:
            showLoadingProgress();
            break;
        default:
            break;
        }
    }

    private void showLoadingProgress() {
        DialogFragment fragment;
        if (mLoadingProgressDialog != null) {
            fragment = mLoadingProgressDialog.get();
            if (fragment != null) {
                fragment.show(mActivity.getFragmentManager(), null);
                return;
            }
        }
        final DialogFragment genProgressDialog = new ProgressFragment(
                R.string.loading_image);
        genProgressDialog.setCancelable(false);
        genProgressDialog.show(mActivity.getFragmentManager(), null);
        genProgressDialog.setStyle(R.style.RefocusDialog,
                genProgressDialog.getTheme());
        mLoadingProgressDialog = new WeakReference<DialogFragment>(
                genProgressDialog);
        MtkLog.d(TAG, "<showLoadingProgress>");
    }

    private void hideLodingProgress() {
        MtkLog.d(TAG, "<hideLodingProgress>");
        if (mLoadingProgressDialog != null) {
            DialogFragment fragment = mLoadingProgressDialog.get();
            if (fragment != null) {
                fragment.dismiss();
            }
        }
    }

    @Override
    public void onActivityPause() {
        super.onActivityPause();
        if (mPlayer != null) {
            MtkLog.d(TAG, "<onActivityPause>...");
            mPlayer.quit();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
