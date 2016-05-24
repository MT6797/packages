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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.mediatek.gallery3d.util.TraceHelper;
import com.mediatek.galleryfeature.stereo.freeview3d.GyroSensorEx.GyroPositionListener;
import com.mediatek.galleryfeature.stereo.freeview3d.RenderThreadEx.OnDrawFrameListener;
import com.mediatek.galleryframework.base.MediaData;
import com.mediatek.galleryframework.base.Player;
import com.mediatek.galleryframework.base.ThumbType;
import com.mediatek.galleryframework.gl.GLIdleExecuter;
import com.mediatek.galleryframework.gl.GLIdleExecuter.GLIdleCmd;
import com.mediatek.galleryframework.gl.MBasicTexture;
import com.mediatek.galleryframework.gl.MBitmapTexture;
import com.mediatek.galleryframework.gl.MGLCanvas;
import com.mediatek.galleryframework.gl.MRawTexture;
import com.mediatek.galleryframework.gl.MTexture;
import com.mediatek.galleryframework.util.MtkLog;

import java.nio.ByteBuffer;

/**
 * Generate texture frame for freeview3D animation.
 */
public class FreeView3DPlayer extends Player implements GyroPositionListener,
        OnDrawFrameListener {
    private static final String TAG = "MtkGallery2/FreeView3DPlayer";
    public static final int STATE_UNLOADED = 0;
    public static final int STATE_PREPARE = 4;
    public static final int STATE_START = 5;
    public static final int STATE_PAUSE = 6;
    public static final int STATE_STOP = 7;
    public static final int MSG_HIDE_DIALOG = 8;
    public static final int MSG_SHOW_DIALOG = 9;
    public static final int FRAME_COUNT = 100;
    public static final int BUFFER_NUMBER_ARGB = 4;
    public boolean mIsOnTouched = false;
    public float mRatio;
    private static RenderThreadEx sRenderThreadEx = null;
    private MBitmapTexture mTexture;
    private MRawTexture mTextureOutPut;
    private MBasicTexture mCurrentTexture;
    private GyroPositionCalculator mGsensorAngle = null;
    private GyroSensorEx mGyroSensor = null;
    private Bitmap mBitmap = null;
    private ByteBuffer mBitmapBuf = null;
    private AnimationEx mAnimation;
    private boolean mIsInitFreeView = false;
    private boolean mIsGenerateDepth = false;
    private int mTextureWidth;
    private int mTextureHeight;
    private volatile int mState = STATE_UNLOADED;
    private int mFrameCountX = FRAME_COUNT;
    private int mFrameCountY = FRAME_COUNT;
    private int mStepX = 0;
    private int mStepY = 0;
    private boolean mNeedUpdateRender = false;
    private boolean mOnFreeViewMode = false;
    private boolean mHasGyroSensor = false;
    private RectF mDisplayRectF = new RectF();
    private int mCanvasWidth;
    private int mCanvasHeight;
    private FreeViewJni mFV = null;
    private boolean mIsShowDialog = false;
    private boolean mInTouchView;
    private GLIdleExecuter mGlExecuter;

    /**
     * Constructor.
     * @param context
     *            for Gyrosensor and canvas.
     * @param data
     *            the depth image mediatData.
     * @param outputType
     *            out put type maybe Texture or bitmap.
     * @param thumbType
     *            resize the resource from which.
     * @param glExecuter
     *            add implementation which running GLRender thread.
     */
    public FreeView3DPlayer(Context context, MediaData data,
            OutputType outputType, ThumbType thumbType,
            GLIdleExecuter glExecuter) {
        super(context, data, outputType);
        if (mGyroSensor == null) {
            mGyroSensor = new GyroSensorEx(mContext);
            mHasGyroSensor = mGyroSensor.hasGyroSensor();
        }
        mGlExecuter = glExecuter;
    }

    @Override
    public MTexture getTexture(MGLCanvas canvas) {
        if (canvas.getWidth() != mCanvasWidth
                || canvas.getHeight() != mCanvasHeight) {
            calculateDisplayRect(canvas);
        }
        if (mOnFreeViewMode && mState == STATE_START) {
            if (mNeedUpdateRender && init()) {
                canvas.beginRenderTarget(mTextureOutPut);
                if (setStep(mTextureOutPut.getId())) {
                    mCurrentTexture = mTextureOutPut;
                }
                canvas.endRenderTarget();
            }
        }
        return mCurrentTexture;
    }

    /**
     * Show Dialog for calculating depth buffer.
     */
    public void showDialog() {
        synchronized (this) {
            if (mState == STATE_PREPARE) {
                sendNotify(MSG_SHOW_DIALOG, -1, null);
                mIsShowDialog = true;
            }
        }
        mNeedUpdateRender = true;
        this.inValidate();
    }

    @Override
    protected boolean onPause() {
        mState = STATE_PAUSE;
        mIsInitFreeView = false;
        if (mGyroSensor != null) {
            mGyroSensor.removeGyroPositionListener(this);
        }
        if (null != mGlExecuter) {
            mGlExecuter.addOnGLIdleCmd(new GLIdleCmd() {
                @Override
                public boolean onGLIdle(MGLCanvas canvas) {
                    MtkLog.d(TAG, "<onGLIdle> releaseFV + mState = " + mState);
                    releaseFV();
                    return false;
                }
            });
        } else {
            releaseFV();
        }
        MtkLog.d(TAG, "<onPause> FreeView3DPlayer = " + this
                + " mTextureOutPut.getId() = " + mTextureOutPut.getId());
        return true;
    }

    @Override
    protected boolean onPrepare() {
        MtkLog.d(TAG, "<onPrepare>= this=" + this + " mMediaData.filePath = "
                + mMediaData.filePath);
        /// M: [DEBUG.ADD] @{
        TraceHelper.traceBegin(">>>>FreeView3DPlayer-onPrepare-decodeBitmap");
        /// @}
        mBitmap = FreeView3DUtile.decodeBitmap(mMediaData.filePath);
        /// M: [DEBUG.ADD] @{
        TraceHelper.traceEnd();
        /// @}
        if (mBitmap != null) {
            mTexture = new MBitmapTexture(mBitmap);
            mCurrentTexture = mTexture;
            mTextureOutPut = new MRawTexture(mBitmap.getWidth(),
                    mBitmap.getHeight(), false);
            mTextureWidth = mBitmap.getWidth();
            mTextureHeight = mBitmap.getHeight();
            MtkLog.d(TAG, " mTextureWidth=" + mTextureWidth
                    + " mTextureHeight=" + mTextureHeight);
            int length = mBitmap.getWidth() * mBitmap.getHeight()
                    * BUFFER_NUMBER_ARGB;
            mBitmapBuf = ByteBuffer.allocate(length);
            mBitmap.copyPixelsToBuffer(mBitmapBuf);
            mAnimation = createAnimation();
        } else {
            MtkLog.d(TAG, "<onPrepare> bitmap == null");
            return false;
        }
        mState = STATE_PREPARE;
        calculateRatio();
        return true;
    }

    @Override
    protected void onRelease() {
        MtkLog.d(TAG, "<onRelease> FreeView3DPlayer = " + this);
        long beginTime = System.currentTimeMillis();
        if (mTextureOutPut != null) {
            mTextureOutPut.recycle();
            mTextureOutPut = null;
        }
        if (mTexture != null) {
            mTexture.recycle();
            mTexture = null;
        }
        mInTouchView = false;
        MtkLog.d(TAG, "<onRelease> use Time = "
                + (System.currentTimeMillis() - beginTime));
    }

    @Override
    protected boolean onStart() {
        MtkLog.d(TAG, "<onStart> FreeView3DPlayer = " + this);
        /// M: [DEBUG.ADD] @{
        TraceHelper.traceBegin(">>>>FreeView3DPlayer-onStart");
        /// @}
        if (mInTouchView) {
            sendNotify(MSG_SHOW_DIALOG, -1, null);
            mIsShowDialog = true;
        }
        long beginTime = System.currentTimeMillis();
        mFV = FreeViewJni.cretate();
        mIsGenerateDepth = mFV.generateDepthInfo(mMediaData.filePath,
                mMediaData.width / 2, mMediaData.height / 2);
        MtkLog.d(TAG, "<onStart> use time for generate Depth Info = " +
                (System.currentTimeMillis() - beginTime));
        if (!mIsGenerateDepth) {
            /// M: [DEBUG.ADD] @{
            TraceHelper.traceEnd();
            /// @}
            return false;
        }
        mGsensorAngle = new GyroPositionCalculator();
        if (null == sRenderThreadEx) {
            sRenderThreadEx = new RenderThreadEx();
            sRenderThreadEx.start();
        }
        sRenderThreadEx.setOnDrawFrameListener(this);
        RenderThreadEx.setRenderRequester(true);
        if (mGyroSensor != null) {
            mGyroSensor.setGyroPositionListener(this);
        }
        mIsInitFreeView = false;
        mState = STATE_START;
        synchronized (this) {
            if (mIsShowDialog) {
                sendNotify(MSG_HIDE_DIALOG, -1, null);
                mNeedUpdateRender = true;
                this.inValidate();
                mIsShowDialog = false;
            }
        }
        /// M: [DEBUG.ADD] @{
        TraceHelper.traceEnd();
        /// @}
        return true;
    }

    @Override
    protected boolean onStop() {
        MtkLog.d(TAG, "<onStop> FreeView3DPlayer = " + this);
        mState = STATE_STOP;
        return true;
    }

    @Override
    public float[] onCalculateAngle(long newTimestamp, float eventValues0,
            float eventValues1, int newRotation) {
        if (mFV == null || !mOnFreeViewMode) {
            inValidate();
            return null;
        }
        float[] angles = null;
        if (mIsOnTouched) {
            return null;
        }
        if (mGsensorAngle != null) {
            angles = mGsensorAngle.calculateAngle(newTimestamp, eventValues0,
                    eventValues1, newRotation);
        }
        if (mState == STATE_START && angles != null) {
            int x = (int) (angles[0] * (mFrameCountX));
            int y = (int) (angles[1] * (mFrameCountY));
            initAnimation(x, y);
        }
        return angles;
    }

    public int getOutputWidths() {
        return mTextureWidth;
    }

    public int getOutputHeights() {
        return mTextureHeight;
    }

    /**
     * Has freeview animation on freeviewMode.
     * @param freeviewMode whether on freeview3D mode.
     */
    public void setMode(boolean freeviewMode) {
        MtkLog.d(TAG, "<setMode>freeviewMode = " + freeviewMode);
        mOnFreeViewMode = freeviewMode;
    }

    /**
     * For no Gysensor devices, should enter touch view for freeview animation.
     * @param isInTouchView  whether in touch view.
     */
    public void enterTouchView(boolean isInTouchView) {
        mInTouchView = isInTouchView;
    }

    /**
     * Quit render thread, which update frame index.
     */
    public void quit() {
        MtkLog.d(TAG, "<quit> ......");
        if (null != sRenderThreadEx) {
            RenderThreadEx.setRenderRequester(false);
        }
        sendNotify(MSG_HIDE_DIALOG, -1, null);
        mState = STATE_PAUSE;
    }

    @Override
    public boolean advanceAnimation() {
        boolean isfinished = true;
        if (mFV != null) {
            if (mAnimation == null) {
                isfinished &= false;
            } else {
                isfinished &= mAnimation.advanceAnimation();
            }
        }
        return isfinished;
    }

    @Override
    public void drawFrame() {
        boolean requestrender = false;
        if (mFV != null && mState == STATE_START) {
            int[] parameter = mFV.getParameter();
            int[] animationFrameIndex = mAnimation.getCurrentFrame();
            int[] currentPosition = getPosition(animationFrameIndex);
            if (parameter == null || currentPosition == null) {
                requestrender = true;
            } else {
                requestrender = (!(parameter[0] == currentPosition[0]
                        && parameter[1] == currentPosition[1]));
            }
            if (currentPosition != null) {
                mNeedUpdateRender = mFV.setStepParameter(currentPosition[0],
                        currentPosition[1]);
            }
        }
        if (requestrender) {
            sendFrameAvailable();
        }
    }

    @Override
    public int getSleepTime() {
        return AnimationEx.SLEEP_TIME_INTERVAL;

    }

    /**
     * Get the number of steps.
     * @param distance the distance of slide the flinger.
     * @return the number of steps.
     */
    public int[] getSteps(float[] distance) {
        return new int[] { (int) (distance[0] / mStepX),
                (int) (distance[1] / mStepY) };
    }

    /**
     * Init Animation.
     * @param targetFrameX the target frame index of x coordinate.
     * @param targetFrameY the target frame index of y coordinate.
     */
    public void initAnimation(int targetFrameX, int targetFrameY) {
        if (mState == STATE_START) {
            boolean needRefresh = mAnimation.initAnimation(targetFrameX,
                    targetFrameY);
            if (needRefresh) {
                MtkLog.d(TAG, "targetFrameX =" + targetFrameX
                        + " targetFrameY=" + targetFrameY);
                RenderThreadEx.setRenderRequester(true);
            }
        }
    }

    public int getCurrentState() {
        return mState;
    }

    /**
     * Whether is Gyrosensor devices or not.
     * @return true if that is Gyrosensor devices.
     */
    public boolean hasGyroSensor() {
        return mHasGyroSensor;
    }

    /**
     * get animation target point.
     * @return current animation target point.
     */
    public float[] getAnimationTargetPoint() {
        if (mAnimation != null) {
            return new float[] { mAnimation.getTargetFrameIndexX(),
                    mAnimation.getTargetFrameIndexY() };
        } else {
            return null;
        }
    }

    /**
     * Check the points if in frame display bounds or not.
     * @param points the touch point.
     * @return whether in bound or not.
     */
    public boolean inBound(float[] points) {
        return mDisplayRectF.contains(points[0], points[1]);
    }

    private boolean init() {
        if (mBitmapBuf != null && (!mIsInitFreeView) && mTexture != null) {
            long currentTime = System.currentTimeMillis();
            int inputWidth = mBitmap.getWidth();
            int inputHeight = mBitmap.getHeight();
            byte[] imageData = new byte[inputWidth * inputHeight * BUFFER_NUMBER_ARGB];
            mBitmapBuf.rewind();
            mBitmapBuf.get(imageData);
            mIsInitFreeView = mFV.initFreeView(imageData, inputWidth,
                    inputHeight, mTextureWidth, mTextureHeight);
            MtkLog.d(TAG, " <init> use time ="
                    + (System.currentTimeMillis() - currentTime));
        }
        return mIsInitFreeView;
    }


    private AnimationEx createAnimation() {
        if (mTextureWidth <= mTextureHeight) {
            mFrameCountX = AnimationEx.STEP;
            mFrameCountY = (int) (((float) (mTextureHeight * mFrameCountX)) / mTextureWidth);
        } else {
            mFrameCountY = AnimationEx.STEP;
            mFrameCountX = (int) (((float) (mTextureWidth * mFrameCountY)) / mTextureHeight);
        }

        mStepX = mTextureWidth / mFrameCountX;
        mStepY = mTextureHeight / mFrameCountY;

        return new AnimationEx(mFrameCountX, mFrameCountY, mFrameCountX / 2,
                mFrameCountY / 2, mFrameCountX / 2, mFrameCountY / 2);
    }

    private boolean setStep(int outputTextureId) {
        if (mFV != null) {
            return mFV.step(outputTextureId);
        }
        return false;
    }

    private void calculateRatio() {
        DisplayMetrics reMetrics = new DisplayMetrics();
        ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getMetrics(reMetrics);
        mRatio = Math.max(((float) mTextureWidth) / reMetrics.widthPixels,
                ((float) mTextureHeight) / reMetrics.heightPixels);
        mDisplayRectF.set(0, 0, reMetrics.widthPixels, reMetrics.heightPixels);
    }

    private void calculateDisplayRect(MGLCanvas canvas) {
        mCanvasWidth = canvas.getWidth();
        mCanvasHeight = canvas.getHeight();
        DisplayMetrics reMetrics = new DisplayMetrics();
        ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getMetrics(reMetrics);
        RectF imageRect = new RectF(0, 0, mTextureWidth, mTextureHeight);
        mDisplayRectF.set(imageRect);
        RectF canvasRectF = new RectF(0, 0, mCanvasWidth, mCanvasHeight);
        Matrix disMatrix = new Matrix();
        disMatrix.setRectToRect(imageRect, canvasRectF,
                Matrix.ScaleToFit.CENTER);
        disMatrix.mapRect(mDisplayRectF);
        MtkLog.d(TAG, "mDisplayRectF = " + mDisplayRectF.toString());
    }

    private int[] getPosition(int[] index) {
        if (mStepX != 0 && mStepY != 0) {
            return new int[] { mStepX * index[0], mStepY * index[1] };
        } else {
            return null;
        }
    }

    private void inValidate() {
        if (mFrameAvailableListener != null) {
            mFrameAvailableListener.onFrameAvailable(this);
        }
    }

    private boolean releaseFV() {
        if (mFV != null) {
            mFV.release();
        }
        return false;
    }
}
