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

package com.mediatek.galleryfeature.stereo.segment.refine;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.android.gallery3d.R;

import com.mediatek.galleryframework.util.MtkLog;

/**
 * Animation indicator implementation for selecting foreground object.
 */
public class FocusIndicatorLayout extends ViewGroup {
    private static final String TAG = "MtkGallery2/SegmentApp/FocusIndicatorLayout";

    private static final int SCALING_UP_TIME = 500;
    private static final int SCALING_DOWN_TIME = 200;
    private static final int DISAPPEAR_TIMEOUT = 200;
    private static final float END_SCALE = 1.5f;

    private Runnable mDisappear = new Disappear();
    private Runnable mEndAction = new EndAction();

    /**
     * Focus state enumeration.
     */
    private enum FocusState {
        STATE_IDLE, STATE_FOCUSING,
    }

    private FocusState mState = FocusState.STATE_IDLE;
    private View mChild;

    /**
     * Constructor.
     *
     * @param context
     *            The Context the view is running in, through which it can
     *            access the current theme, resources, etc.
     * @param attrs
     *            The attributes of the XML tag that is inflating the view.
     */
    public FocusIndicatorLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void showStart() {
        MtkLog.d(TAG, "<showStart> mState = " + mState);
        if (mState == FocusState.STATE_IDLE) {
            setDrawable(R.drawable.ic_focus);
            animate().withLayer().setDuration(SCALING_UP_TIME).scaleX(END_SCALE).scaleY(END_SCALE)
                    .withEndAction(mEndAction);
            mState = FocusState.STATE_FOCUSING;
        }
    }

    void clear() {
        MtkLog.d(TAG, "<clear> mState = " + mState);
        if (mState != FocusState.STATE_IDLE) {
            animate().cancel();
            removeCallbacks(mDisappear);
            mDisappear.run();
            setScaleX(1f);
            setScaleY(1f);
            mState = FocusState.STATE_IDLE;
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mChild = getChildAt(0);
        mChild.setPivotX(0);
        mChild.setPivotY(0);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l;
        int height = b - t;
        mChild.layout(0, 0, width, height);

        MtkLog.d(TAG, "<onLayout>, width:" + width + ", height:" + height);
    }

    @Override
    protected void onMeasure(int widhtSpec, int heightSpec) {
        int w = 0;
        int h = 0;
        measureChild(mChild, widhtSpec, heightSpec);
        w = mChild.getMeasuredWidth();
        h = mChild.getMeasuredHeight();

        setMeasuredDimension(w, h);

        MtkLog.d(TAG, "<onMeasure> w:" + w + "h:" + h);
    }

    private void setDrawable(int resid) {
        mChild.setBackgroundDrawable(getResources().getDrawable(resid));
    }

    /**
     * Action executed when indicator animation ends.
     */
    private class EndAction implements Runnable {
        @Override
        public void run() {
            animate().withLayer().setDuration(SCALING_DOWN_TIME).scaleX(1f).scaleY(1f);
            // Keep the focus indicator for some time.
            postDelayed(mDisappear, DISAPPEAR_TIMEOUT);
        }
    }

    /**
     * Runnable on UI thread to make indicator disappear.
     */
    private class Disappear implements Runnable {
        @Override
        public void run() {
            MtkLog.d(TAG, "<Disappear> run mState = " + mState);
            mChild.setBackgroundDrawable(null);
        }
    }
}
