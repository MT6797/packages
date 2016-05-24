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
package com.mediatek.galleryfeature.stereo.segment.synth;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

import com.android.gallery3d.R;
import com.mediatek.galleryframework.util.MtkLog;

/**
 * The controller for over layer view.
 */
public class OverLayController {
    private static final String TAG = "MtkGallery2/OverLayController";
    public static final int MIN_SILE_SIZE = 90;
    public static final int TOLERANCE = 50;
    public static final int MARGIN = 32;
    public static final int SHADOW_COLOR = 0xCF000000;
    public static final int ORITATION_90 = 90;
    public static final int ORITATION_180 = 180;
    public static final int ORITATION_270 = 270;
    public static final int ORITATION_360 = 360;
    public static final int OFFSET = 4;
    public static final int OFFSET_TIMES_ONE = 1;
    public static final int OFFSET_TIMES_TWO = 2;
    public static final int OFFSET_TIMES_THREE = 3;
    private RectF mImageBoundsForCrop = new RectF();
    private RectF mScreenBounds = new RectF();
    private RectF mScreenCropBounds = new RectF();
    private Paint mPaintForCrop = new Paint();

    private OverLayerObject mCropObj = null;
    private Drawable mCropIndicator;
    private final int mIndicatorSize = 70;
    private int mRotation = 0;
    private Matrix mDisplayMatrix = null;
    private Matrix mDisplayMatrixInverse = null;
    private boolean mDirty = false;
    private int mMargin = MARGIN;
    private float mPrevX = 0;
    private float mPrevY = 0;

    private float mPrevScreenX = 0;
    private float mPrevScreenY = 0;

    private int mOverlayShadowColor = SHADOW_COLOR;
    private int mMinSideSize = MIN_SILE_SIZE;
    private int mTouchTolerance = TOLERANCE;

    private Bitmap mIconBitmap;

    private Matrix mRotationMatrix;
    private Matrix mRotationMatrixInvert;

    private float[] mCurrentPointsForAngles = { 0.0f, 0.0f };

    private float[] mStartPointsForAngles = { 0.0f, 0.0f };
    private Mode mState = Mode.NONE;
    private StateListener mListener;
    private boolean mHasDrawCropBitmap = false;
    private boolean mEnableTouchMotion = true;
    private float mAngle = 0.0f;
    private float mPreAngle = 0.0f;

    /**
     * The Mode for on click operation.
     */
    private enum Mode {
        NONE, MOVE, MODIFY_TOUCH_POINTER
    }

    /**
     * The listener for dirty state.
     */
    public interface StateListener {
        /**
         * invalidate current state.
         */
        public void invaliable();
    }

    /**
     * Constructor.
     * @param context
     *            the context.
     * @param listener
     *            the listener for dirty state.
     */
    public OverLayController(Context context, StateListener listener) {
        setup(context);
        mListener = listener;
    }

    /**
     * Init the controller.
     * @param image
     *            the background for the layer.
     * @param iconBitmap
     *            the foreground for the layer.
     * @param newCropBounds
     *            the display rect for foreground bitmap.
     * @param newPhotoBounds
     *            the display rect for background bitmap.
     * @param rotation
     *            the angle for the display.
     */
    public void initialize(Bitmap image, Bitmap iconBitmap,
            RectF newCropBounds, RectF newPhotoBounds, int rotation) {
        mIconBitmap = iconBitmap;
        mRotationMatrix = new Matrix();
        mRotationMatrixInvert = new Matrix();
        if (mCropObj != null) {
            RectF crop = mCropObj.getInnerBounds();
            RectF containing = mCropObj.getOuterBounds();
            if (crop != newCropBounds || containing != newPhotoBounds
                    || mRotation != rotation) {
                mRotation = rotation;
                mCropObj.resetBoundsTo(newCropBounds, newPhotoBounds);
                clearDisplay();
            }
        } else {
            mRotation = rotation;
            mCropObj = new OverLayerObject(newPhotoBounds, newCropBounds, 0);
            clearDisplay();
        }
    }

    public RectF getCrop() {
        return mCropObj.getInnerBounds();
    }

    public RectF getPhoto() {
        return mCropObj.getOuterBounds();
    }

    /**
     * Get touch event and change state.
     * @param event
     *            the MotionEvent.
     * @return true if the event was handled, false otherwise.
     */
    public boolean getTouchEvent(MotionEvent event) {
        if (!mHasDrawCropBitmap) {
            return false;
        }
        if (!mEnableTouchMotion) {
            return true;
        }
        if (event.getPointerCount() >= 2) {
            return false;
        }
        float[] point = getPoint(event);
        float x = point[0];
        float y = point[1];

        if (mDisplayMatrix == null || mDisplayMatrixInverse == null) {
            return true;
        }
        float[] touchPoint = { x, y };
        float[] touchPointNoRotation = { x, y };
        mDisplayMatrixInverse.mapPoints(touchPointNoRotation);
        mRotationMatrixInvert.mapPoints(touchPoint);
        mDisplayMatrixInverse.mapPoints(touchPoint);

        x = touchPoint[0];
        y = touchPoint[1];
        mDisplayMatrix.mapPoints(touchPoint);
        mRotationMatrix.mapPoints(touchPoint);
        float[] pointF = getPoint(event);
        MtkLog.d(TAG, " pointF[0] = " + pointF[0] + " pointF[1] = " + pointF[1]
                + " || X= " + x + " Y=" + y + "|||| " + touchPoint[0] + " [] "
                + touchPoint[1]);
        handleMotionEvent(event.getActionMasked(), x, y, touchPointNoRotation, pointF);
        if (null != mListener) {
            mListener.invaliable();
        }
        return true;
    }

    /**
     *  Set width and height.
     * @param width the width for display.
     * @param height the height for display.
     */
    public void applyAspect(float width, float height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Bad arguments to applyAspect");
        }
        // If we are rotated by 90 degrees from horizontal, swap x and y
        if (((mRotation < 0) ? -mRotation : mRotation) % ORITATION_180
                == ORITATION_90) {
            float tmp = width;
            width = height;
            height = tmp;
        }
        if (!mCropObj.setInnerAspectRatio(width, height)) {
            MtkLog.w(TAG, "failed to set aspect ratio");
        }
        if (mListener != null) {
            mListener.invaliable();
        }
    }

    /**
     * Draw the bitmap on canvas.
     * @param canvas the canvas for bitmap draw.
     */
    public void drawBitmap(Canvas canvas) {
        RectF currentCanvasbounds = new RectF(0, 0, canvas.getWidth(),
                canvas.getHeight());
        Matrix saveMatrix = new Matrix();
        if (!OverLayerDrawingUtils.setImageToScreenMatrix(saveMatrix,
                mImageBoundsForCrop, currentCanvasbounds, mRotation)) {
            MtkLog.w(TAG, "<saveBitmap>failed to get screen matrix");
        }
        mCropObj.getInnerBounds(mScreenCropBounds);
        if (saveMatrix.mapRect(mScreenCropBounds)) {
            canvas.save();
            canvas.rotate(getCurrentAngle(), mScreenCropBounds.centerX(),
                    mScreenCropBounds.centerY());
            canvas.drawBitmap(mIconBitmap, null, mScreenCropBounds,
                    mPaintForCrop);
            canvas.restore();
        }
    }

    /**
     * Draw background and foreground on Canvas.
     * @param canvas the canvas for draw.
     * @param backgroundBitmap the background draw on canvas.
     * @param cropBitmap the foreground draw on canvas.
     * @return true if finish draw.
     */
    public boolean drawBitmap(Canvas canvas, Bitmap backgroundBitmap,
            Bitmap cropBitmap) {
        if (mDirty) {
            mDirty = false;
            clearDisplay();
            mScreenBounds = new RectF(0, 0, canvas.getWidth(),
                    canvas.getHeight());
        }
        if (cropBitmap != null && !mHasDrawCropBitmap
                && backgroundBitmap != null) {
            mScreenBounds = new RectF(0, 0, canvas.getWidth(),
                    canvas.getHeight());
            mImageBoundsForCrop = new RectF(0, 0, backgroundBitmap.getWidth(),
                    backgroundBitmap.getHeight());
            RectF mIconRectBounds = new RectF(
                    backgroundBitmap.getWidth() / 2 - cropBitmap.getWidth() / 2,
                    backgroundBitmap.getHeight() / 2 - cropBitmap.getHeight()
                            / 2,
                    backgroundBitmap.getWidth() / 2 + cropBitmap.getWidth() / 2,
                    backgroundBitmap.getHeight() / 2 + cropBitmap.getHeight()
                            / 2);
            RectF backgroundF = new RectF(0 - mIconRectBounds.width(),
                    0 - mIconRectBounds.height(),
                    backgroundBitmap.getWidth() + mIconRectBounds.width(),
                    backgroundBitmap.getHeight() + mIconRectBounds.height());
            initialize(null, cropBitmap, mIconRectBounds, backgroundF, 0);
            applyAspect(mIconRectBounds.width(), mIconRectBounds.height());
            mHasDrawCropBitmap = true;
        }
        if (!mHasDrawCropBitmap) {
            return false;
        }
        if (mCropObj == null) {
            reset();
            mCropObj = new OverLayerObject(mImageBoundsForCrop, mImageBoundsForCrop,
                    0);
        }
        return draw(canvas);
    }

    protected void configChanged() {
        MtkLog.d(TAG, "<configChanged> ");
        mDirty = true;
    }

    private boolean draw(Canvas canvas) {
        if (mCropObj == null) {
            return false;
        }
        if (mDisplayMatrix == null || mDisplayMatrixInverse == null) {
            mDisplayMatrix = new Matrix();
            mDisplayMatrix.reset();
            if (!OverLayerDrawingUtils.setImageToScreenMatrix(mDisplayMatrix,
                    mImageBoundsForCrop, mScreenBounds, mRotation)) {
                MtkLog.w(TAG, "failed to get screen matrix");
                mDisplayMatrix = null;
                return false;
            }
            mDisplayMatrixInverse = new Matrix();
            mDisplayMatrixInverse.reset();
            if (!mDisplayMatrix.invert(mDisplayMatrixInverse)) {
                MtkLog.w(TAG, "could not invert display matrix");
                mDisplayMatrixInverse = null;
                return false;
            }
            mMinSideSize = (int) Math.min(
                    Math.min(mImageBoundsForCrop.width(),
                            mImageBoundsForCrop.height()), mMinSideSize);
            mCropObj.setMinInnerSideSize(mMinSideSize);
            mCropObj.setTouchTolerance(mDisplayMatrixInverse
                    .mapRadius(mTouchTolerance));
            return false;
        }
        mPaintForCrop.setAntiAlias(true);
        mPaintForCrop.setFilterBitmap(true);
        mCropObj.getInnerBounds(mScreenCropBounds);

        if (mDisplayMatrix.mapRect(mScreenCropBounds)) {
            mRotationMatrix.setRotate(getCurrentAngle(),
                    mScreenCropBounds.centerX(), mScreenCropBounds.centerY());
            mRotationMatrix.invert(mRotationMatrixInvert);
            canvas.save();
            canvas.rotate(getCurrentAngle(), mScreenCropBounds.centerX(),
                    mScreenCropBounds.centerY());
            canvas.drawBitmap(mIconBitmap, null, mScreenCropBounds,
                    mPaintForCrop);
            Paint p = new Paint();
            p.setColor(mOverlayShadowColor);
            p.setStyle(Paint.Style.FILL);
            OverLayerDrawingUtils.drawCropRect(canvas, mScreenCropBounds);
            OverLayerDrawingUtils.drawIndicators(canvas, mCropIndicator,
                    mIndicatorSize, mScreenCropBounds,
                    mCropObj.isFixedAspect(),
                    decode(mCropObj.getSelectState(), mRotation));
            float[] touchPoint = { mCropObj.mCurrentPointX,
                    mCropObj.mCurrentPointY };
            mDisplayMatrix.mapPoints(touchPoint);
            RectF temp = new RectF();
            mCropObj.getInnerBounds(temp);
            float[] touchPointCenter = { temp.centerX(), temp.centerY() };
            mDisplayMatrix.mapPoints(touchPointCenter);
            canvas.restore();
            return true;
        }
        return false;
    }

    private float[] getPoint(MotionEvent event) {
        if (event.getPointerCount() == 1) {
            float[] touchPoint = { event.getX(), event.getY() };
            return touchPoint;
        } else {
            float[] touchPoint = { (event.getX(0) + event.getX(1)) / 2,
                    (event.getY(0) + event.getY(1)) / 2 };
            return touchPoint;
        }
    }

    private void saveAngle() {
        mPreAngle = mAngle + mPreAngle;
        mAngle = 0.0f;
    }

    private float getCurrentAngle() {
        return (mAngle + mPreAngle) % ORITATION_360;
    }

    private float rotation() {
        if (mState != Mode.MOVE) {
            return mAngle;
        }
        RectF innerBound = new RectF();
        mCropObj.getInnerBounds(innerBound);
        mDisplayMatrix.mapRect(innerBound);
        float centerX = innerBound.centerX();
        float centerY = innerBound.centerY();
        float[] center = { centerX, centerY };
        double startAngle = calculateAngle(center, mStartPointsForAngles);
        double endAngle = calculateAngle(center, mCurrentPointsForAngles);

        mAngle = (float) (endAngle - startAngle);
        return (float) mAngle;
    }

    private double calculateAngle(float[] center, float[] point) {
        double distance = distance(center, point);
        double dx = distanceDX(center, point);
        double dy = distanceDY(center, point);
        double cosA = dx / distance;
        double arcA;
        if (cosA > 1) {
            arcA = Math.acos(1);
        } else if (cosA < -1) {
            arcA = Math.acos(-1);
        } else {
            arcA = Math.acos(cosA);
        }
        double angleA = arcA * ORITATION_180 / Math.PI;
        if (dy < 0) {
            angleA = ORITATION_360 - angleA;
        }
        return angleA;
    }

    private double distanceDX(float[] center, float[] point) {
        return (point[0] - center[0]);
    }

    private double distanceDY(float[] center, float[] point) {
        return (point[1] - center[1]);
    }

    private double distance(float[] center, float[] point) {
        return Math.sqrt((center[0] - point[0]) * (center[0] - point[0])
                + (center[1] - point[1]) * (center[1] - point[1]));
    }

    private void setCurrentPointForTest(float[] pointF) {
        if (mCropObj != null) {
            mRotationMatrixInvert.mapPoints(pointF);
            mDisplayMatrixInverse.mapPoints(pointF);
            mCropObj.mCurrentPointX = pointF[0];
            mCropObj.mCurrentPointY = pointF[1];
        }
    }

    private void setStartsPoint(float[] pointF) {
        mStartPointsForAngles[0] = pointF[0];
        mStartPointsForAngles[1] = pointF[1];
    }

    private void setCurrentPoint(float[] pointF) {
        mCurrentPointsForAngles[0] = pointF[0];
        mCurrentPointsForAngles[1] = pointF[1];
    }

    private float[] getScreenDistance(float[] startPoint, float[] currenPoint) {
        mRotationMatrixInvert.mapPoints(startPoint);
        mRotationMatrixInvert.mapPoints(currenPoint);
        float[] dis = { currenPoint[0] - startPoint[0],
                currenPoint[1] - startPoint[1] };
        return dis;
    }

    private void setup(Context context) {
        Resources rsc = context.getResources();
        mCropIndicator = rsc.getDrawable(R.drawable.m_overlay_indicate);
        mMargin = (int) rsc.getDimension(R.dimen.overlay_preview_margin);
        mMinSideSize = (int) rsc.getDimension(R.dimen.overlay_min_side);
        mTouchTolerance = (int) rsc
                .getDimension(R.dimen.overlay_touch_tolerance);
    }

    /**
     * Rotates first d bits in integer x to the left some number of times.
     */
    private int bitCycleLeft(int x, int times, int d) {
        int mask = (1 << d) - 1;
        int mout = x & mask;
        times %= d;
        int hi = mout >> (d - times);
        int low = (mout << times) & mask;
        int ret = x & ~mask;
        ret |= low;
        ret |= hi;
        return ret;
    }

    private void reset() {
        MtkLog.w(TAG, "crop reset called");
        mState = Mode.NONE;
        mCropObj = null;
        mRotation = 0;
        clearDisplay();
    }

    /**
     * Find the selected edge or corner in screen coordinates.
     */
    private int decode(int movingEdges, float rotation) {
        int rot = OverlayerMath.constrainedRotation(rotation);
        switch (rot) {
        case ORITATION_90:
            return bitCycleLeft(movingEdges, OFFSET_TIMES_ONE, OFFSET);
        case ORITATION_180:
            return bitCycleLeft(movingEdges, OFFSET_TIMES_TWO, OFFSET);
        case ORITATION_270:
            return bitCycleLeft(movingEdges, OFFSET_TIMES_THREE, OFFSET);
        default:
            return movingEdges;
        }
    }

    private void clearDisplay() {
        mDisplayMatrix = null;
        mDisplayMatrixInverse = null;
        if (mListener != null) {
            mListener.invaliable();
        }
    }

    private void resetPoints(float pointX, float pointY, float[] touchPointNoRotation) {
        mPrevX = pointX;
        mPrevY = pointY;
        mPrevScreenX = touchPointNoRotation[0];
        mPrevScreenY = touchPointNoRotation[1];
    }

    private void handleMotionEvent(int mask, float x, float y,
            float[] touchPointNoRotation, float[] pointF) {
        switch (mask) {
        case (MotionEvent.ACTION_DOWN):
            setStartsPoint(pointF);
            setCurrentPoint(pointF);
            setCurrentPointForTest(pointF);
            if (mState == Mode.NONE) {
                if (!mCropObj.selectEdge(x, y) && mCropObj.inBounds(x, y)) {
                    mCropObj.selectEdge(OverLayerObject.MOVE_BLOCK);
                }
                if (mCropObj.hasSelectedEdge()) {
                    resetPoints(x, y, touchPointNoRotation);
                    mState = Mode.MOVE;
                }
            }
            break;
        case (MotionEvent.ACTION_UP):
            if (mState == Mode.MOVE) {
                mCropObj.selectEdge(OverLayerObject.MOVE_NONE);
                resetPoints(x, y, touchPointNoRotation);
                mState = Mode.NONE;
                saveAngle();
            }
            setStartsPoint(pointF);
            setCurrentPoint(pointF);
            break;
        case (MotionEvent.ACTION_MOVE):
            if (mCropObj.getSelectState()
                    != OverLayerObject.MOVE_BLOCK && mState == Mode.MOVE) {
                setCurrentPoint(pointF);
                rotation();
            }
            setCurrentPointForTest(pointF);
            if (mState == Mode.MOVE || mState == Mode.MODIFY_TOUCH_POINTER) {
                if (mState == Mode.MOVE) {
                    float dx = x - mPrevX;
                    float dy = y - mPrevY;
                    float[] temp = { mPrevX, mPrevY };
                    float[] tempCurrentPoint = { x, y };
                    getScreenDistance(temp, tempCurrentPoint);
                    mCropObj.moveCurrentSelection(dx, dy,
                            touchPointNoRotation[0] - mPrevScreenX,
                            touchPointNoRotation[1] - mPrevScreenY);
                } else if (mState == Mode.MODIFY_TOUCH_POINTER) {
                    mState = Mode.MOVE;
                }
                resetPoints(x, y, touchPointNoRotation);
            }
            break;
        case (MotionEvent.ACTION_POINTER_DOWN):
        case (MotionEvent.ACTION_POINTER_UP):
            mState = Mode.MODIFY_TOUCH_POINTER;
            break;
        default:
            break;
        }
    }

}
