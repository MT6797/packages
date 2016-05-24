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
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.mediatek.galleryfeature.stereo.segment.ImageShow;
import com.mediatek.galleryfeature.stereo.segment.StereoSegmentWrapper;
import com.mediatek.galleryframework.util.MtkLog;

/**
 * ImageShow for picking foreground object in StereoRefineActivity. See
 * com.mediatek.galleryfeature.stereo.segment.ImageShow.
 */
public class ImageShowPick extends ImageShow implements StereoSegmentWrapper.IRedrawListener {
    private static final String LOGTAG = "MtkGallery2/SegmentApp/ImageShowPick";

    private FocusIndicator mFocusIndicator;
    private IOnRefineListener mOnRefineListener;

    private float[] mViewPos = new float[2];
    private float[] mImgPos = new float[] { -1, -1 };

    /**
     * A listener to segment operation.
     */
    public interface IOnRefineListener {
        /**
         * Callback when start a refine operation.
         */
        void onRefineStart();

        /**
         * Callback when end a refine operation.
         */
        void onRefineEnd();

        /**
         * Callback when an error happens during refine operation.
         */
        void onRefineError();
    }

    /**
     * Constructor.
     *
     * @param context
     *            The Context the view is running in, through which it can
     *            access the current theme, resources, etc.
     * @param attrs
     *            The attributes of the XML tag that is inflating the view.
     */
    public ImageShowPick(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Constructor.
     *
     * @param context
     *            The Context the view is running in, through which it can
     *            access the current theme, resources, etc.
     */
    public ImageShowPick(Context context) {
        super(context);
    }

    /**
     * Set listener for segment operation.
     *
     * @param listener
     *            the listener.
     */
    public void setOnRefineListener(IOnRefineListener listener) {
        mOnRefineListener = listener;
    }

    @Override
    public void doDraw(Canvas canvas) {
        if (mMaskSimulator == null) {
            super.doDraw(canvas); // or return
            return;
        }

        drawImages(canvas, mMasterImage.getBitmap(), mMaskSimulator.getMaskBitmap());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        int action = event.getAction();
        action = action & MotionEvent.ACTION_MASK;
        int ex = (int) event.getX();
        int ey = (int) event.getY();
        if (action == MotionEvent.ACTION_DOWN) {

            mViewPos[0] = ex;
            mViewPos[1] = ey;
            Matrix mat = getScreenToImageMatrix(true);
            if (mat == null) {
                return true;
            }
            mat.mapPoints(mImgPos, mViewPos);
            MtkLog.d(LOGTAG, "<onTouchEvent> mImgPos = {" + mImgPos[0] + ", " + mImgPos[1] + "}");
            MtkLog.d(LOGTAG, "<onTouchEvent> mViewPos = {" + mViewPos[0]
                    + ", " + mViewPos[1] + "}");

            int iX = (int) (mImgPos[0]);
            int iY = (int) (mImgPos[1]);
            Rect imageBounds = mMasterImage.getOriginalBounds();
            if (imageBounds != null && imageBounds.contains(iX, iY)) {
                showFocusIndicator(ex, ey);
                mMaskSimulator.setMaskCenter(iX, iY);
            }
        }
        return true;
    }

    @Override
    public void setSegmenter(StereoSegmentWrapper segmenter) {
        super.setSegmenter(segmenter);
        if (segmenter == null) {
            return;
        }

        if (getVisibility() == VISIBLE) {
            mMaskSimulator.addReDrawListener(this);
        }
        Rect originalBounds = mMasterImage.getOriginalBounds();
        int imgWidth = originalBounds.width();
        int imgHeight = originalBounds.height();
        MtkLog.d(LOGTAG, "<setSegmenter> imgWidth = " + imgWidth + ", imgHeight = " + imgHeight);
        float[] initSegmentPoint = mMaskSimulator.getSegmentPoint();
        Matrix mat = mMasterImage.getImageToScreenMatrix(imgWidth, imgHeight, getWidth(),
                getHeight());
        mat.mapPoints(mViewPos, initSegmentPoint);
        showFocusIndicator((int) mViewPos[0], (int) mViewPos[1]);
    }

    @Override
    public void onRedraw(boolean result) {
        if (!result) {
            if (mOnRefineListener != null) {
                mOnRefineListener.onRefineError();
            }
            return;
        } else {
            mOnRefineListener.onRefineEnd();
            invalidate();
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        if ((changedView == this) && (mMaskSimulator != null)) {
            if (visibility == VISIBLE) {
                mMaskSimulator.addReDrawListener(this);
                // TODO doing, handle cancel
                int iX = (int) (mImgPos[0]);
                int iY = (int) (mImgPos[1]);
                if (iX >= 0 || iY >= 0) {
                    MtkLog.d(LOGTAG, "request to manualy pick object");
                    mMaskSimulator.setMaskCenter(iX, iY);
                } else {
                    MtkLog.d(LOGTAG, "request to auto pick object");
                    mMaskSimulator.autoSegment();
                }
            } else {
                mMaskSimulator.removeRedrawListener(this);
            }
        }
    }

    private void showFocusIndicator(int ex, int ey) {
        if (mFocusIndicator != null) {
            mFocusIndicator.onSingleTapUp(ex, ey);
        } else if (mActivity != null) {
            mFocusIndicator = new FocusIndicator(mActivity);
            mFocusIndicator.onSingleTapUp(ex, ey);
        }
    }
}