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

package com.mediatek.galleryfeature.stereo.segment;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.android.gallery3d.R;

import com.mediatek.galleryframework.util.MtkLog;

/**
 * A view specialized to to show a bitmap mastered by MainImageMaster.<br/>
 * It references a StereoSegmentWrapper to do segment operations as needed.
 * And It also provides common interfaces for gesture interaction.
 */
public class ImageShow extends View implements OnGestureListener,
        ScaleGestureDetector.OnScaleGestureListener, OnDoubleTapListener {
    private static final String LOGTAG = "MtkGallery2/SegmentApp/ImageShow";

    private static final boolean ENABLE_ZOOMED_COMPARISON = false;
    private static final int EDGE_LEFT = 1;
    private static final int EDGE_TOP = 2;
    private static final int EDGE_RIGHT = 3;
    private static final int EDGE_BOTTOM = 4;
    private static final int DEFAULT_EDGE_SIZE = 100;
    private static final int mAnimationSnapDelay = 200;
    private static final int mAnimationZoomDelay = 400;

    /**
     * Interaction mode enumeration.
     */
    private enum InteractionMode {
        NONE, SCALE, MOVE
    }

    protected boolean mIsZoomPanSupported;

    protected Paint mPaint = new Paint();
    protected MainImageMaster mMasterImage;
    protected StereoSegmentWrapper mMaskSimulator;
    protected Activity mActivity;

    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private Point mTouchDown = new Point();
    private Point mTouch = new Point();
    private Point mOriginalTranslation = new Point();
    private ValueAnimator mAnimatorScale;
    private ValueAnimator mAnimatorTranslateX;
    private ValueAnimator mAnimatorTranslateY;
    private EdgeEffectCompat mEdgeEffect;
    private InteractionMode mInteractionMode = InteractionMode.NONE;

    private boolean mZoomIn = false;
    private float mStartFocusX;
    private float mStartFocusY;
    private int mCurrentEdgeEffect = 0;
    private int mEdgeSize = DEFAULT_EDGE_SIZE;

    /**
     * Constructor.
     *
     * @param context
     *            The Context the view is running in, through which it can
     *            access the current theme, resources, etc.
     * @param attrs
     *            The attributes of the XML tag that is inflating the view.
     * @param defStyle
     *            An attribute in the current theme that contains a reference to
     *            a style resource that supplies default values for the view.
     *            Can be 0 to not look for defaults.
     */
    public ImageShow(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setupImageShow(context);
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
    public ImageShow(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupImageShow(context);
    }

    /**
     * Constructor.
     *
     * @param context
     *            The Context the view is running in, through which it can
     *            access the current theme, resources, etc.
     */
    public ImageShow(Context context) {
        super(context);
        setupImageShow(context);
    }

    /**
     * Set the MainImageMaster.
     * @param image the MainImageMaster.
     */
    public void setImageMaster(MainImageMaster image) {
        mMasterImage = image;
    }

    /**
     * Set the StereoSegmentWrapper.
     * @param segmenter the StereoSegmentWrapper.
     */
    public void setSegmenter(StereoSegmentWrapper segmenter) {
        mMaskSimulator = segmenter;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        int action = event.getAction();
        action = action & MotionEvent.ACTION_MASK;

        mGestureDetector.onTouchEvent(event);
        mScaleGestureDetector.onTouchEvent(event);
        if (mInteractionMode == InteractionMode.SCALE) {
            return true;
        }

        int ex = (int) event.getX();
        int ey = (int) event.getY();
        if (action == MotionEvent.ACTION_DOWN) {
            mInteractionMode = InteractionMode.MOVE;
            mTouchDown.x = ex;
            mTouchDown.y = ey;
            mMasterImage.setOriginalTranslation(mMasterImage.getTranslation());
        }

        if (action == MotionEvent.ACTION_MOVE && mInteractionMode == InteractionMode.MOVE) {
            mTouch.x = ex;
            mTouch.y = ey;

            float scaleFactor = mMasterImage.getScaleFactor();
            if (scaleFactor > 1 && (!ENABLE_ZOOMED_COMPARISON || event.getPointerCount() == 2)) {
                float translateX = (mTouch.x - mTouchDown.x) / scaleFactor;
                float translateY = (mTouch.y - mTouchDown.y) / scaleFactor;
                Point originalTranslation = mMasterImage.getOriginalTranslation();
                Point translation = mMasterImage.getTranslation();
                translation.x = (int) (originalTranslation.x + translateX);
                translation.y = (int) (originalTranslation.y + translateY);
                mMasterImage.setTranslation(translation);
            }
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_OUTSIDE) {
            mInteractionMode = InteractionMode.NONE;
            mTouchDown.x = 0;
            mTouchDown.y = 0;
            mTouch.x = 0;
            mTouch.y = 0;
            if (mMasterImage.getScaleFactor() <= 1) {
                mMasterImage.setScaleFactor(1);
                mMasterImage.resetTranslation();
            }
        }
        float scaleFactor = mMasterImage.getScaleFactor();
        Point translation = mMasterImage.getTranslation();
        constrainTranslation(translation, scaleFactor);
        mMasterImage.setTranslation(translation);

        invalidate();
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        if (!mIsZoomPanSupported) {
            return false;
        }
        mZoomIn = !mZoomIn;
        float scale = 1.0f;
        final float x = event.getX();
        final float y = event.getY();
        if (mZoomIn) {
            scale = mMasterImage.getMaxScaleFactor();
        }
        if (scale != mMasterImage.getScaleFactor()) {
            if (mAnimatorScale != null) {
                mAnimatorScale.cancel();
            }
            mAnimatorScale = ValueAnimator.ofFloat(mMasterImage.getScaleFactor(), scale);
            float translateX = (getWidth() / 2 - x);
            float translateY = (getHeight() / 2 - y);
            Point translation = mMasterImage.getTranslation();
            int startTranslateX = translation.x;
            int startTranslateY = translation.y;
            if (scale != 1.0f) {
                translation.x = (int) (mOriginalTranslation.x + translateX);
                translation.y = (int) (mOriginalTranslation.y + translateY);
            } else {
                translation.x = 0;
                translation.y = 0;
            }
            constrainTranslation(translation, scale);
            startAnimTranslation(startTranslateX, translation.x, startTranslateY, translation.y,
                    mAnimationZoomDelay);
            mAnimatorScale.setDuration(mAnimationZoomDelay);
            mAnimatorScale.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mMasterImage.setScaleFactor((Float) animation.getAnimatedValue());
                    invalidate();
                }
            });
            mAnimatorScale.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    applyTranslationConstraints();
                    invalidate();
                }
                @Override
                public void onAnimationCancel(Animator animation) {
                }
                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            mAnimatorScale.start();
        }
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent arg0) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent arg0) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent arg0) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent startEvent, MotionEvent endEvent, float arg2, float arg3) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent arg0) {
    }

    @Override
    public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent arg0) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        return false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (!mIsZoomPanSupported) {
            return false;
        }
        float scaleFactor = mMasterImage.getScaleFactor();

        scaleFactor = scaleFactor * detector.getScaleFactor();
        if (scaleFactor > mMasterImage.getMaxScaleFactor()) {
            scaleFactor = mMasterImage.getMaxScaleFactor();
        }
        if (scaleFactor < 1.0f) {
            scaleFactor = 1.0f;
        }
        mMasterImage.setScaleFactor(scaleFactor);
        scaleFactor = mMasterImage.getScaleFactor();
        float focusx = detector.getFocusX();
        float focusy = detector.getFocusY();
        float translateX = (focusx - mStartFocusX) / scaleFactor;
        float translateY = (focusy - mStartFocusY) / scaleFactor;
        Point translation = mMasterImage.getTranslation();
        translation.x = (int) (mOriginalTranslation.x + translateX);
        translation.y = (int) (mOriginalTranslation.y + translateY);
        mMasterImage.setTranslation(translation);
        invalidate();
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        if (!mIsZoomPanSupported) {
            return false;
        }
        Point pos = mMasterImage.getTranslation();
        mOriginalTranslation.x = pos.x;
        mOriginalTranslation.y = pos.y;
        mStartFocusX = detector.getFocusX();
        mStartFocusY = detector.getFocusY();
        mInteractionMode = InteractionMode.SCALE;
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        if (!mIsZoomPanSupported) {
            return;
        }
        mInteractionMode = InteractionMode.NONE;
        if (mMasterImage.getScaleFactor() < 1) {
            mMasterImage.setScaleFactor(1);
            invalidate();
        }
    }

    public void setZoomPanSupported(boolean isZoomPanSupported) {
        mIsZoomPanSupported = isZoomPanSupported;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(parentWidth, parentHeight);
    }

    /**
     * This function calculates a screen to image Transformation matrix.
     *
     * @param reflectRotation
     *            set true if you want the rotation encoded.
     *            TODO useless, to be removed.
     * @return Screen to Image transformation matrix
     */
    protected Matrix getScreenToImageMatrix(boolean reflectRotation) {
        Rect originalBounds = mMasterImage.getOriginalBounds();
        if (originalBounds == null) {
            return null;
        }
        int imgWidth = originalBounds.width();
        int imgHeight = originalBounds.height();

        Matrix m = mMasterImage
                .getImageToScreenMatrix(imgWidth, imgHeight, getWidth(), getHeight());
        Matrix invert = new Matrix();
        m.invert(invert);
        return invert;
    }

    protected Bitmap loadBitmap(Context context, Uri uri, BitmapFactory.Options o) {
        return ImageLoader.loadOrientedBitmap(context, uri, o);
    }

    // override this
    protected void doDraw(Canvas canvas) {
        Bitmap preview = mMasterImage.getBitmap();
        canvas.save();
        drawImages(canvas, preview);
        canvas.restore();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        // skip when bitmap not ready
        if (mMasterImage.getOriginalBounds() == null) {
            MtkLog.v(LOGTAG, "<onDraw> bitmap not ready, skip this onDraw pass");
            return;
        }
        mMasterImage.setImageShowSize(getWidth(), getHeight());

        doDraw(canvas);
    }

    protected void drawImages(Canvas canvas, Bitmap ...images) {
        Bitmap bitmap = images[0];
        if (bitmap == null) {
            return;
        }

        Matrix m = mMasterImage.getImageToScreenMatrix(bitmap.getWidth(), bitmap.getHeight(),
                getWidth(), getHeight());
        if (m == null) {
            return;
        }

        for (int i = 0; i < images.length; i++) {
            bitmap = images[i];
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, m, mPaint);
            }
        }
    }

    protected void constrainTranslation(Point translation, float scale) {
        int currentEdgeEffect = 0;
        if (scale <= 1) {
            finishEdgeEffect();
            return;
        }
        Rect originalBounds = mMasterImage.getOriginalBounds();
        Matrix originalToScreen = mMasterImage.getImageToScreenMatrix(originalBounds.width(),
                originalBounds.height(), getWidth(), getHeight());
        if (originalToScreen == null) {
            finishEdgeEffect();
            return;
        }
        RectF screenPos = new RectF(originalBounds);
        originalToScreen.mapRect(screenPos);
        boolean rightConstraint = screenPos.right < getWidth();
        boolean leftConstraint = screenPos.left > 0;
        boolean topConstraint = screenPos.top > 0;
        boolean bottomConstraint = screenPos.bottom < getHeight();
        if (screenPos.width() > getWidth()) {
            if (rightConstraint && !leftConstraint) {
                float tx = screenPos.right - translation.x * scale;
                translation.x = (int) ((getWidth() - tx) / scale);
                currentEdgeEffect = EDGE_RIGHT;
            } else if (leftConstraint && !rightConstraint) {
                float tx = screenPos.left - translation.x * scale;
                translation.x = (int) ((-tx) / scale);
                currentEdgeEffect = EDGE_LEFT;
            }
        } else {
            float tx = screenPos.right - translation.x * scale;
            float dx = (getWidth() - screenPos.width()) / 2f;
            translation.x = (int) ((getWidth() - tx - dx) / scale);
        }
        if (screenPos.height() > getHeight()) {
            if (bottomConstraint && !topConstraint) {
                float ty = screenPos.bottom - translation.y * scale;
                translation.y = (int) ((getHeight() - ty) / scale);
                currentEdgeEffect = EDGE_BOTTOM;
            } else if (topConstraint && !bottomConstraint) {
                float ty = screenPos.top - translation.y * scale;
                translation.y = (int) ((-ty) / scale);
                currentEdgeEffect = EDGE_TOP;
            }
        } else {
            float ty = screenPos.bottom - translation.y * scale;
            float dy = (getHeight() - screenPos.height()) / 2f;
            translation.y = (int) ((getHeight() - ty - dy) / scale);
        }
        if (mCurrentEdgeEffect != currentEdgeEffect) {
            if (mCurrentEdgeEffect == 0 || currentEdgeEffect != 0) {
                mCurrentEdgeEffect = currentEdgeEffect;
                mEdgeEffect.finish();
            }
            mEdgeEffect.setSize(getWidth(), mEdgeSize);
        }
        if (currentEdgeEffect != 0) {
            mEdgeEffect.onPull(mEdgeSize);
        }
    }

    private void finishEdgeEffect() {
        mCurrentEdgeEffect = 0;
        mEdgeEffect.finish();
    }

    private void setupImageShow(Context context) {
        Resources res = context.getResources();
        res.getColor(R.color.background_screen);
        mGestureDetector = new GestureDetector(context, this);
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        mActivity = (Activity) context;
        mEdgeEffect = new EdgeEffectCompat(context);
        mEdgeSize = res.getDimensionPixelSize(R.dimen.edge_glow_size);
    }

    private void startAnimTranslation(int fromX, int toX, int fromY, int toY, int delay) {
        if (fromX == toX && fromY == toY) {
            return;
        }
        if (mAnimatorTranslateX != null) {
            mAnimatorTranslateX.cancel();
        }
        if (mAnimatorTranslateY != null) {
            mAnimatorTranslateY.cancel();
        }
        mAnimatorTranslateX = ValueAnimator.ofInt(fromX, toX);
        mAnimatorTranslateY = ValueAnimator.ofInt(fromY, toY);
        mAnimatorTranslateX.setDuration(delay);
        mAnimatorTranslateY.setDuration(delay);
        mAnimatorTranslateX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Point translation = mMasterImage.getTranslation();
                translation.x = (Integer) animation.getAnimatedValue();
                mMasterImage.setTranslation(translation);
                invalidate();
            }
        });
        mAnimatorTranslateY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Point translation = mMasterImage.getTranslation();
                translation.y = (Integer) animation.getAnimatedValue();
                mMasterImage.setTranslation(translation);
                invalidate();
            }
        });
        mAnimatorTranslateX.start();
        mAnimatorTranslateY.start();
    }

    private void applyTranslationConstraints() {
        float scaleFactor = mMasterImage.getScaleFactor();
        Point translation = mMasterImage.getTranslation();
        int x = translation.x;
        int y = translation.y;
        constrainTranslation(translation, scaleFactor);

        if (x != translation.x || y != translation.y) {
            startAnimTranslation(x, translation.x, y, translation.y, mAnimationSnapDelay);
        }
    }
}