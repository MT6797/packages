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

import android.os.Environment;

import com.mediatek.gallery3d.util.TraceHelper;
import com.mediatek.galleryfeature.stereo.RefocusImageJni;
import com.mediatek.galleryframework.util.MtkLog;
import com.mediatek.xmp.XmpOperator;

import java.io.File;

/**
 * Encapsulation for freeview3D JNI.
 */
public class FreeViewJni {
    private static final String TAG = "MtkGallery2/FreeViewJni";

    private final long mHandle;
    private XmpOperator mXmpOperator;

    private int mDepthBufferWidth;
    private int mDepthBufferHeight;
    private int mOrientation;

    private byte[] mDepthBuffer = null;
    private boolean mRegenerateDepth = false;

    private StepParameter mStepParameter;
    // used in GL thread (avoid frequent variable allocation
    private StepParameter mTempStepParameter = new StepParameter();

    static {
        System.loadLibrary("jni_fv3d");
    }
    /**
     * Save current points.
     */
    public class StepParameter {
        public int x;
        public int y;
        /**
         * Set current points.
         * @param x x coordinates.
         * @param y y coordinates.
         * @return whether the points is updated.
         */
        public synchronized boolean set(int x, int y) {
            boolean newPoints = false;
            if (this.x != x || this.y != y) {
                newPoints = true;
            }
            this.x = x;
            this.y = y;
            return newPoints;
        }
        /**
         * get points.
         * @param result the current points.
         */
        public synchronized void get(StepParameter result) {
            result.x = this.x;
            result.y = this.y;
        }
    }

    /**
     * Generate depth info.
     * get depth from XMP, failed-> regenerate depth
     * @param filePath the file path from which generate depth Info.
     * @param targetWidth the width of depth buffer.
     * @param targetHeight the height of depth buffer.
     * @return whether success generate depth buffer or not.
     */
    public boolean generateDepthInfo(String filePath, int targetWidth,
            int targetHeight) {
        synchronized (RefocusImageJni.SLOCK) {
            MtkLog.d(TAG, "<generateDepthInfo>, filePath:" + filePath
                    + ",targetWidth:" + targetWidth + ",targetHeight:"
                    + targetHeight);
            mXmpOperator = new XmpOperator();
            boolean initialized = mXmpOperator.initialize(filePath);
            if (!initialized) {
                MtkLog.d(TAG, "<generateDepthInfo> initialized fail!!!");
                return false;
            }
            XmpOperator.DepthBufferInfo depthBufferInfo = mXmpOperator
                    .getDepthBufferInfoFromFile(filePath);
            if (depthBufferInfo == null || depthBufferInfo.depthData == null
                    || mRegenerateDepth) {
                RefocusImageJni refocus = new RefocusImageJni();
                refocus.createImageRefocus(filePath,
                        RefocusImageJni.RefocusMode.REFOCUS_MODE_DEPTH_AND_XMP);
                boolean res = refocus.initRefocusNoDepthMap(filePath,
                        targetWidth, targetHeight);
                if (res) {
                    res = refocus.generateDepth();
                }
                if (!res) {
                    MtkLog.d(TAG,
                            "<generateDepthInfo> initRefocusNoDepthMap fail!!!");
                    refocus.release();
                    return false;
                }

                depthBufferInfo = refocus.getDepthInfo();
                refocus.release();
            }

            if (depthBufferInfo != null) {
                mDepthBuffer = depthBufferInfo.depthData;
                mDepthBufferWidth = depthBufferInfo.depthBufferWidth;
                mDepthBufferHeight = depthBufferInfo.depthBufferHeight;
            }

            XmpOperator.StereoDebugInfo stereoInfo = mXmpOperator
                    .getStereoDebugInfoFromFile(filePath);
            mOrientation = stereoInfo.orientation;

            if (mDepthBuffer != null && mDepthBufferWidth != 0
                    && mDepthBufferHeight != 0) {
                return true;
            }
            MtkLog.d(TAG, "<generateDepthInfo> fail!!!");
            return false;
        }
    }

    /**
     * Create FreeviewJni object.
     * @return the freeviewJni object.
     */
    public static final FreeViewJni cretate() {
        return new FreeViewJni();
    }

    /**
     * initialize free view (allocate memory,etc).
     * @param bitmap the free view bitmap.
     * @param inputWidth the width of free view bitmap.
     * @param inputHeight the height of free view bitmap.
     * @param outputWidth the width of out put buffer.
     * @param outputHeight the height of out put buffer.
     * @return whether success initialize free view or not.
     */
    public boolean initFreeView(byte[] bitmap, int inputWidth, int inputHeight,
            int outputWidth, int outputHeight) {
        MtkLog.d(TAG, "<initFreeView>, inputWidth:" + inputWidth
                + ",inputHeight:" + inputHeight + ",outputWidth:" + outputWidth
                + ",outputHeight:" + outputHeight);

        if (mDepthBuffer == null || mDepthBufferWidth <= 0
                || mDepthBufferHeight <= 0) {
            MtkLog.d(TAG, "<initFreeView>, error depth info");
            return false;
        }

        boolean nativeResult = nativeInitFreeView(mHandle, bitmap, inputWidth,
                inputHeight, mDepthBuffer, mDepthBufferWidth,
                mDepthBufferHeight, outputWidth, outputHeight, mOrientation);

        return nativeResult;
    }

    /**
     * Set coordinate for this free view.
     * @param x the X coordinate.
     * @param y the Y coordinate.
     * @return if the points is updated.
     */
    public boolean setStepParameter(int x, int y) {
        MtkLog.d(TAG, "<setProcessParameter> x = " + x + ", y = " + y);
        if (null == mStepParameter) {
            mStepParameter = new StepParameter();
        }
        return mStepParameter.set(x, y);
    }

    /**
     * Get coordinate.
     * @return get coordinate.
     */
    public int[] getParameter() {
        if (null == mStepParameter) {
            return null;
        } else {
            return new int[] { mStepParameter.x, mStepParameter.y };

        }
    }

    /**
     * Get frame buffer by current coordinates, which only called in GL thread.
     * @param outputTextureId the texture id for frame buffer.
     * @return whether success get frame buffer or not.
     */
    public boolean step(int outputTextureId) {
        if (null == mStepParameter || outputTextureId <= 0) {
            return false;
        }
        mStepParameter.get(mTempStepParameter);
        MtkLog.d(TAG, "< step > mHandle=" + mHandle + " mTempStepParameter.x="
                + mTempStepParameter.x + " mTempStepParameter.y="
                + mTempStepParameter.y + " mTempStepParameter.outputTexId="
                + outputTextureId);
        /// M: [DEBUG.ADD] @{
        TraceHelper.traceBegin(">>>>FreeView3DPlayer-step");
        /// @}
        boolean nativeResult = nativeStep(mHandle, mTempStepParameter.x,
                mTempStepParameter.y, outputTextureId);
        /// M: [DEBUG.ADD] @{
        TraceHelper.traceEnd();
        /// @}
        return nativeResult;
    }

    /**
     *  Release resource.
     */
    public void release() {
        nativeRelease(mHandle);
    }

    private FreeViewJni() {
        MtkLog.d(TAG, "<FreeView3DObject.constructor>");
        mHandle = nativeCreate();

        File file = new File(Environment.getExternalStorageDirectory(),
                "regenerate_depth");
        if (file.exists()) {
            mRegenerateDepth = true;
        }
    }

    private static native long nativeCreate();

    private native boolean nativeDecodeJpeg(long handle, String filePath,
            int inputWidth, int inputHeight);

    private native boolean nativeInitFreeView(long handl, byte[] bitmap,
            int inputWidth, int inputHeight, byte[] depthBuffer,
            int depthWidth, int depthHeight, int outputWidth, int outputHeight,
            int orientation);

    private native boolean nativeInit(long handle, byte[] depthBuffer,
            int depthWidth, int depthHeight, int outputWidth, int outputHeight);

    private native boolean nativeStep(long handle, int x, int y, int outputTexId);

    private native void nativeRelease(long handle);

}
