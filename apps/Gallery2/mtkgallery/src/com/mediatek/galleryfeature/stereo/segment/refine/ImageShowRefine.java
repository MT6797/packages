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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.mediatek.galleryfeature.stereo.segment.ImageShow;
import com.mediatek.galleryfeature.stereo.segment.StereoSegmentWrapper;

/**
 * ImageShow for refining foreground object in StereoRefineActivity. See
 * com.mediatek.galleryfeature.stereo.segment.ImageShow.
 */
public class ImageShowRefine extends ImageShow implements StereoSegmentWrapper.IRedrawListener {
    @SuppressWarnings("unused")
    private static final String LOGTAG = "MtkGallery2/SegmentApp/ImageShowRefine";

    private static final int REFINE_PATH_WIDTH = 10;
    private static final int REFINE_PATH_COLOR_MASK = 0x80FFFFFF;
    public static final int MODE_ADD = 1;
    public static final int MODE_DEL = 2;

    private Matrix mToOrig;
    private IOnRefineListener mOnRefineListener;

    private int mMode;
    private float[] mTmpPoint = new float[2]; // so we do not malloc

    /**
     * A listener to refine operation.
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
    public ImageShowRefine(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Constructor.
     *
     * @param context
     *            The Context the view is running in, through which it can
     *            access the current theme, resources, etc.
     */
    public ImageShowRefine(Context context) {
        super(context);
    }

    /**
     * Specify refine mode (add or delete selection on foreground object).
     *
     * @param mode
     *            refine mode, MODE_ADD or MODE_DEL.
     */
    public void setMode(int mode) {
        mMode = mode;
    }

    /**
     * Set listener to refine operation.
     *
     * @param listener
     *            the listener.
     */
    public void setOnRefineListener(IOnRefineListener listener) {
        mOnRefineListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() > 1) {
            boolean ret = super.onTouchEvent(event);
            return ret;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mToOrig = getScreenToImageMatrix(true);
            if (mToOrig == null) {
                return true;
            }
            mTmpPoint[0] = event.getX();
            mTmpPoint[1] = event.getY();
            mToOrig.mapPoints(mTmpPoint);
            if (mMaskSimulator != null) {
                mMaskSimulator.startNewRefine(mTmpPoint[0], mTmpPoint[1]);
            }
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE) {

            int historySize = event.getHistorySize();
            for (int h = 0; h < historySize; h++) {
                mTmpPoint[0] = event.getHistoricalX(0, h);
                mTmpPoint[1] = event.getHistoricalY(0, h);
                mToOrig.mapPoints(mTmpPoint);
                if (mMaskSimulator != null) {
                    mMaskSimulator.addRefinePoint(mTmpPoint[0], mTmpPoint[1]);
                }
            }
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            mTmpPoint[0] = event.getX();
            mTmpPoint[1] = event.getY();
            mToOrig.mapPoints(mTmpPoint);
            if (mMaskSimulator != null) {
                mOnRefineListener.onRefineStart();
                mMaskSimulator.endRefine(mTmpPoint[0], mTmpPoint[1]);
                if (mMode == MODE_ADD) {
                    mMaskSimulator.addSelection();
                } else if (mMode == MODE_DEL) {
                    mMaskSimulator.delSelection();
                }
            }
        }
        invalidate();
        return true;
    }

    @Override
    public void doDraw(Canvas canvas) {
        if (mMaskSimulator == null) {
            super.doDraw(canvas); // or return
            return;
        }

        drawImages(canvas, mMasterImage.getBitmap(), mMaskSimulator.getMaskBitmap());

        drawRefinePath(canvas);
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
            } else {
                mMaskSimulator.removeRedrawListener(this);
            }
        }
    }

    private void drawRefinePath(Canvas canvas) {
        Path path = mMaskSimulator.getRefinePath();
        Bitmap bitmap = mMasterImage.getBitmap();
        if ((path == null) || (bitmap == null)) {
            return;
        }

        Matrix m = mMasterImage.getImageToScreenMatrix(bitmap.getWidth(), bitmap.getHeight(),
                getWidth(), getHeight());

        Path transformedRefinepath = new Path();
        transformedRefinepath.addPath(path, m);

        Paint tmpPaint = new Paint();
        tmpPaint.setStyle(Style.STROKE);
        tmpPaint.setStrokeWidth(REFINE_PATH_WIDTH);
        if (mMode == MODE_ADD) {
            tmpPaint.setColor(Color.BLUE & REFINE_PATH_COLOR_MASK);
            canvas.drawPath(transformedRefinepath, tmpPaint);
        } else if (mMode == MODE_DEL) {
            tmpPaint.setColor(Color.RED & REFINE_PATH_COLOR_MASK);
            canvas.drawPath(transformedRefinepath, tmpPaint);
        }
    }
}
