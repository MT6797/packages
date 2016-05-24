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
package com.mediatek.galleryfeature.stereo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;

import com.mediatek.xmp.SegmentMaskOperator.SegmentMaskInfo;
import com.mediatek.xmp.XmpOperator;

import junit.framework.Assert;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * use to generate segment object.
 *
 */
public class SegmentJni {
    private static final String TAG = "MtkGallery2/SegmentJni";
    // scenario
    public static final int SCENARIO_AUTO = 0;
    public static final int SCENARIO_SELECTION = 1;
    public static final int SCENARIO_SCRIBBLE_FG = 2;
    public static final int SCENARIO_SCRIBBLE_BG = 3;
    // mode
    public static final int MODE_OBJECT = 0;
    public static final int MODE_FOREGROUND = 1;

    private static final Bitmap.Config mConfig = Bitmap.Config.ARGB_8888;

    private static final int RECT_LEN = 2000;
    private static final int HALF_RECT_LEN = RECT_LEN / 2;

    private static final int MASK_COLOR = 0xBBFF6666;
    private static final int MASK_ALPHA = 0xFF000000;
    private static final int MASK_WHITE = 0x00FFFFFF;
    private static final int BYTE_PER_PIXEL = 4;
    private static final int ROI_MARGIN = 20;
    private static final int TEMP_NUM3 = 3;
    // if other component set mask buffer to segment
    // segment use this mask and set to algorithm
    private static SegmentMaskInfo sMaskInfo = null;
    private final XmpOperator mXmpOperator;

    private ByteBuffer mByteBuffer = null;
    private String mFilePath;
    private boolean mIsReadMaskFromXmp = true;

    private int mImageWidth = 0;
    private int mImageHeight = 0;
    private int mNewImageWidth = 0;
    private int mNewImageHeight = 0;
    private int mScribbleWidth = 0;
    private int mScribbleHeight = 0;

    /**
     * constructor, if exist file "NOT_READ_MASK"->regenerate mask. initialize
     * XmpOperator
     */
    public SegmentJni() {
        File file = new File(Environment.getExternalStorageDirectory(), "NOT_READ_MASK");
        if (file.exists()) {
            mIsReadMaskFromXmp = false;
        }
        mXmpOperator = new XmpOperator();
    }

    /**
     * if other component set mask buffer to segment. segment will use this mask
     * replace JPEG header information.
     *
     * @param maskInfo
     *            mask buffer and information
     */
    public static void setMaskBuffer(SegmentMaskInfo maskInfo) {
        sMaskInfo = maskInfo;
    }

    /**
     * initialize segment.
     *
     * @param sourceFilePath
     *            bitmap file path
     * @param bitmap
     *            origin bitmap
     * @param orientation
     *            bitmap orientation
     * @return success->true, fail->false
     */
    public boolean initSegment(String sourceFilePath, Bitmap bitmap, int orientation) {

        Log.d(TAG, "[initSegment] bitmap:" + bitmap + ",sourceFilePath:" + sourceFilePath
                + ",imgWidth:" + bitmap.getWidth() + ",imgHeight:" + bitmap.getHeight()
                + ",orientation:" + orientation);

        mFilePath = sourceFilePath;
        mImageWidth = bitmap.getWidth();
        mImageHeight = bitmap.getHeight();
        mScribbleWidth = mImageWidth;
        mScribbleHeight = mImageHeight;
        mByteBuffer = ByteBuffer.allocate(mImageWidth * mImageHeight * BYTE_PER_PIXEL);
        synchronized (RefocusImageJni.SLOCK) {
            // mXmpOperator = new XmpOperator();
            boolean res = mXmpOperator.initialize(sourceFilePath);
            if (!res) {
                Log.d(TAG, "mXmpOperator.initialize fail!!!!!");
                return false;
            }
            XmpOperator.DepthBufferInfo depthBufferInfo = mXmpOperator
                    .getDepthBufferInfoFromFile(sourceFilePath);

            if (depthBufferInfo == null || depthBufferInfo.depthData == null
                    || depthBufferInfo.depthBufferWidth <= 0
                    || depthBufferInfo.depthBufferHeight <= 0
                    || depthBufferInfo.metaBufferWidth <= 0
                    || depthBufferInfo.metaBufferHeight <= 0) {
                RefocusImageJni refocus = new RefocusImageJni();
                refocus.createImageRefocus(sourceFilePath,
                        RefocusImageJni.RefocusMode.REFOCUS_MODE_DEPTH_AND_XMP);
                res = refocus.initRefocusNoDepthMap(sourceFilePath, mImageWidth,
                        mImageHeight);
                if (res) {
                    res = refocus.generateDepth();
                }
                if (!res) {
                    Log.d(TAG, "<initSegment>  initRefocusNoDepthMap fail!!!!!");
                    refocus.release();
                    return false;
                }

                depthBufferInfo = refocus.getDepthInfo();
                refocus.release();
                mXmpOperator.initialize(sourceFilePath);
            }

            return initSegmentWithJpsAndDepth(sourceFilePath, bitmap, depthBufferInfo,
                    orientation);
        }
    }

    private boolean initSegmentWithJpsAndDepth(String sourceFilePath, Bitmap bitmap,
            XmpOperator.DepthBufferInfo depthBufferInfo, int orientation) {
        Rect[] faceRect = null;
        byte[] depthBuf = null;
        byte[] occBuf = null;
        int depthWidth = -1;
        int depthHeight = -1;
        int metaWidth = -1;
        int metaHeight = -1;
        int faceNum = 0;
        int[] faceRip = { 0 };
        XmpOperator.StereoDebugInfo stereoInfo =
                mXmpOperator.getStereoDebugInfoFromFile(sourceFilePath);
        XmpOperator.FaceDetectionInfo fdInfo = null;
        Log.d(TAG, "<initSegment> faceNum:" + stereoInfo.faceCount);
        if (stereoInfo.faceCount >= 1 && stereoInfo.fdInfoArray != null
                && stereoInfo.fdInfoArray.size() >= 1) {
            faceNum = stereoInfo.faceCount;
            faceRip = new int[faceNum];
            faceRect = new Rect[faceNum];
            for (int i = 0; i < faceNum; i++) {
                fdInfo = stereoInfo.fdInfoArray.get(i);
                if (fdInfo.mFaceLeft >= -HALF_RECT_LEN && fdInfo.mFaceLeft <= HALF_RECT_LEN) {
                    faceRect[i] = StereoUtils.getFaceRect(mImageWidth, mImageHeight,
                            fdInfo.mFaceLeft, fdInfo.mFaceTop, fdInfo.mFaceRight,
                            fdInfo.mFaceBottom);
                    faceRip[i] = fdInfo.mFaceRip;
                    Log.d(TAG, "<initSegment> left:" + faceRect[i].left + ",top:"
                            + faceRect[i].top + ",right:" + faceRect[i].right + ",bottom:"
                            + faceRect[i].bottom);
                } else {
                    Assert.assertTrue(false);
                }
            }
        }
        if (depthBufferInfo != null && depthBufferInfo.depthData != null
                && depthBufferInfo.depthBufferWidth > 0 && depthBufferInfo.depthBufferHeight > 0
                && depthBufferInfo.metaBufferWidth > 0 && depthBufferInfo.metaBufferHeight > 0) {
            depthWidth = depthBufferInfo.depthBufferWidth;
            depthHeight = depthBufferInfo.depthBufferHeight;
            metaWidth = depthBufferInfo.metaBufferWidth;
            metaHeight = depthBufferInfo.metaBufferHeight;
            depthBuf = Arrays.copyOfRange(depthBufferInfo.depthData, 0, depthWidth * depthHeight);
            occBuf = Arrays.copyOfRange(depthBufferInfo.depthData, depthWidth * depthHeight
                    + metaWidth * metaHeight * TEMP_NUM3, depthWidth * depthHeight + metaWidth
                    * metaHeight * BYTE_PER_PIXEL);
            int length = bitmap.getWidth() * bitmap.getHeight() * BYTE_PER_PIXEL;
            byte[] imageData = new byte[length];
            mByteBuffer.clear();
            bitmap.copyPixelsToBuffer(mByteBuffer);
            mByteBuffer.rewind();
            mByteBuffer.get(imageData);
            nativeInit(imageData, mImageWidth, mImageHeight, depthBuf, depthWidth, depthHeight,
                    occBuf, metaWidth, metaHeight, mScribbleWidth, mScribbleHeight, faceNum,
                    faceRect, faceRip, orientation);
            return true;
        }
        return false;
    }

    /**
     * do segment according to different parameter.
     *
     * @param scenario
     *            SCENARIO_SCRIBBLE_FG SCENARIO_SCRIBBLE_BG SCENARIO_SELECTION
     *            SCENARIO_AUTO
     * @param mode
     *            MODE_OBJECT MODE_FOREGROUND
     * @param scribble
     *            scribble image, foreground->255, background->0
     * @param roiRect
     *            roiRect
     * @param point
     *            point
     * @return success->true, fail->false
     */
    public boolean doSegment(int scenario, int mode, Bitmap scribble, Rect roiRect, Point point) {

        Log.d(TAG, "[doSegment] scenario:" + scenario + ",mode:" + mode + ",roiRect:"
                + roiRect + ",scribble:" + scribble);

        if ((scenario == SCENARIO_SCRIBBLE_FG || SCENARIO_SCRIBBLE_BG == scenario)
                && (scribble == null || roiRect == null)) {
            Log.d(TAG, "null scribble or roiRect!!!!");
            Assert.assertTrue(false);
        }

        if (scenario == SCENARIO_SELECTION && point == null) {
            Log.d(TAG, "null point!!!!");
            Assert.assertTrue(false);
        }

        if (scribble != null && (scribble.getWidth() != mScribbleWidth
                || scribble.getHeight() != mScribbleHeight)) {
            Log.d(TAG, "error scribble size,width:" + scribble.getWidth() + ",height:"
                    + scribble.getHeight());
            Assert.assertTrue(false);
        }

        if (scenario == SCENARIO_AUTO) {
            if (sMaskInfo != null) {
                boolean res = initSegmentMask(sMaskInfo);
                sMaskInfo.mMaskBuffer = null;
                sMaskInfo = null;
                if (res) {
                    Log.d(TAG, "get maskInfo from buffer");
                    return true;
                }
            }
            if (getMaskFromXmp()) {
                Log.d(TAG, "get maskInfo from xmp");
                return true;
            }
        }

        byte[] scribbleBuf = null;
        if (scribble != null) {
            scribbleBuf = new byte[mScribbleWidth * mScribbleHeight * BYTE_PER_PIXEL];
            mByteBuffer.clear();
            mByteBuffer.rewind();
            scribble.copyPixelsToBuffer(mByteBuffer);
            mByteBuffer.rewind();
            mByteBuffer.get(scribbleBuf);
        }

        Rect rect = getRoiRect(scenario, roiRect, point);
        return nativeDoSegment(scenario, MODE_OBJECT, scribbleBuf, rect);
    }

    /**
     * undo segment, show previous operation image.
     *
     * @return success->true, fail->false
     */
    public boolean undoSegment() {
        return nativeUndoSegment();
    }

    /**
     * get current segment mask.
     *
     * @return mask
     */
    public byte[] getSegmentMask() {
        byte[] mask = nativeGetSegmentMask(mImageWidth, mImageHeight, false);
        return mask;
    }

    /**
     * get current segment mask point.
     *
     * @return point
     */
    public Point getSegmentPoint() {
        Point point = (Point) nativeGetSegmentPoint(false);
        Log.d(TAG, "<getSegmentPoint>,x:" + point.x + ",y:" + point.y);
        return point;
    }

    /**
     * get current segment mask rect.
     *
     * @return mask rect
     */
    public Rect getSegmentRect() {
        Rect rect = (Rect) nativeGetSegmentRect(false);
        return rect;
    }

    /**
     * get current cover bitmap, foreground clear show and background blur show.
     *
     * @return cover bitmap
     */
    public Bitmap getCoverBitmap() {
        Bitmap alphaBmp = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ALPHA_8);
        Canvas canvas = new Canvas(alphaBmp);
        canvas.drawColor(MASK_COLOR & MASK_ALPHA);

        alphaBmp = (Bitmap) nativeFillMaskToImg(alphaBmp, mImageWidth, mImageHeight);

        Bitmap resultBmp = Bitmap.createBitmap(mImageWidth, mImageHeight, mConfig);
        Canvas cvs = new Canvas(resultBmp);
        cvs.drawColor(MASK_COLOR & MASK_WHITE);
        cvs.drawBitmap(alphaBmp, new Matrix(), null);
        alphaBmp.recycle();
        cvs.drawColor(MASK_COLOR, PorterDuff.Mode.SRC_IN);

        return resultBmp;
    }

    /**
     * get current segment bitmap.
     *
     * @param bitmap
     *            original bitmap
     * @return segment bitmap
     */
    public Bitmap getSegmentBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            Log.d(TAG, "<getSegmentBitmap> ERROR, null bitmap,fail!!!!");
            return null;
        }
        Rect rect = (Rect) nativeGetSegmentRect(false);
        Bitmap maskBitmap = Bitmap.createBitmap(rect.width(), rect.height(), mConfig);
        return (Bitmap) nativeGetSegmentImg(bitmap, bitmap.getWidth(), bitmap.getHeight(),
                maskBitmap, maskBitmap.getWidth(), maskBitmap.getHeight(), false);
    }

    /**
     * set new bitmap to segment, you can get new mask/point/rect from JNI.
     *
     * @param bitmap
     *            new bitmap
     * @param mask
     *            old mask, if you do segment use same instance, set it NULL
     * @param maskWidth
     *            mask width, if mask is null, invalid
     * @param maskHeight
     *            mask height, if mask is null, invalid
     * @return success->true, fail->false
     */
    public boolean setNewBitmap(Bitmap bitmap, byte[] mask, int maskWidth, int maskHeight) {
        if (bitmap.getWidth() < mImageWidth || bitmap.getHeight() < mImageHeight) {
            Log.d(TAG, "<setNewBitmap> ERROR, smaller bitmap,fail!!!");
            return false;
        }
        mNewImageWidth = bitmap.getWidth();
        mNewImageHeight = bitmap.getHeight();
        Log.d(TAG, "<setNewBitmap> bitmap:" + bitmap + ",width:" + mNewImageWidth + ",height:" +
                mNewImageHeight + ",mask:" + mask + ",maskWidth:" + maskWidth + ",maskHeight:"
                + maskHeight);
        return nativeSetNewBitmap(bitmap, mNewImageWidth, mNewImageHeight, mask, maskWidth,
                maskHeight);
    }

    /**
     * get new segment mask. use it after setNewBitmap.
     *
     * @return new segment mask
     */
    public byte[] getNewSegmentMask() {
        return nativeGetSegmentMask(mNewImageWidth, mNewImageHeight, true);
    }

    /**
     * get new segment rect, use it after setNewBitmap.
     *
     * @return new segmetn rect
     */
    public Rect getNewSegmentRect() {
        return (Rect) nativeGetSegmentRect(true);
    }

    /**
     * get new segment point, use it after setNewBitmap.
     *
     * @return new segment point
     */
    public Point getNewSegmentPoint() {
        return (Point) nativeGetSegmentPoint(true);
    }

    /**
     * get new segment bitmap, use it after setNewBitmap.
     *
     * @param bitmap
     *            original bitmap
     * @return segment bitmap
     */
    public Bitmap getNewSegmentBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            Log.d(TAG, "<getNewSegmentBitmap> ERROR: null bitmap,fail!!!");
            return null;
        }
        Rect rect = (Rect) nativeGetSegmentRect(true);
        Bitmap maskBitmap = Bitmap.createBitmap(rect.width(), rect.height(), mConfig);
        return (Bitmap) nativeGetSegmentImg(bitmap, bitmap.getWidth(), bitmap.getHeight(),
                maskBitmap, maskBitmap.getWidth(), maskBitmap.getHeight(), true);
    }

    /**
     * save mask info to XMP.
     */
    public void saveMaskToXmp() {
        Log.d(TAG, "<saveMaskToXmp>");

        Point point = (Point) nativeGetSegmentPoint(false);
        Rect rect = (Rect) nativeGetSegmentRect(false);
        byte[] mask = nativeGetSegmentMask(mImageWidth, mImageHeight, false);

        SegmentMaskInfo maskInfo = new SegmentMaskInfo();
        maskInfo.mMaskBuffer = mask;
        maskInfo.mMaskWidth = mImageWidth;
        maskInfo.mMaskHeight = mImageHeight;
        maskInfo.mSegmentX = point.x;
        maskInfo.mSegmentY = point.y;
        maskInfo.mSegmentBottom = rect.bottom;
        maskInfo.mSegmentLeft = rect.left;
        maskInfo.mSegmentTop = rect.top;
        maskInfo.mSegmentRight = rect.right;
        synchronized (RefocusImageJni.SLOCK) {
            mXmpOperator.writeSegmentMaskInfoToJpg(mFilePath, maskInfo);
        }
    }

    /**
     * release memory.
     */
    public void release() {
        nativeRelease();
    }

    private Rect getRoiRect(int scenario, Rect roiRect, Point point) {
        int left = 0;
        int top = 0;
        int right = mImageWidth - 1;
        int bottom = mImageHeight - 1;

        if (scenario == SCENARIO_SELECTION) {

            left = point.x - ROI_MARGIN;
            top = point.y - ROI_MARGIN;
            right = point.x + ROI_MARGIN;
            bottom = point.y + ROI_MARGIN;

            if (left < 0) {
                right = right - left;
                left = 0;
            }
            if (right > (mImageWidth - 1)) {
                left = left - (right - (mImageWidth - 1));
                right = mImageWidth - 1;
            }

            if (top < 0) {
                bottom = bottom - top;
                top = 0;
            }
            if (bottom > mImageHeight - 1) {
                top = top - (bottom - (mImageHeight - 1));
                bottom = mImageHeight - 1;
            }
        }

        if (scenario == SCENARIO_SCRIBBLE_FG || scenario == SCENARIO_SCRIBBLE_BG) {
            Assert.assertTrue(roiRect != null);
            left = roiRect.left;
            top = roiRect.top;
            right = roiRect.right;
            bottom = roiRect.bottom;
        }
        Rect rect = new Rect(left, top, right, bottom);
        return rect;
    }

    private boolean getMaskFromXmp() {
        if (!mIsReadMaskFromXmp) {
            Log.d(TAG, "<getMaskFromXmp> isReadMaskFromXmp:false!!");
            return false;
        }
        synchronized (RefocusImageJni.SLOCK) {
            SegmentMaskInfo maskInfo = mXmpOperator.getSegmentMaskInfoFromFile(mFilePath);

            return initSegmentMask(maskInfo);
        }
    }

    private boolean initSegmentMask(SegmentMaskInfo maskInfo) {
        if (maskInfo == null) {
            Log.d(TAG, "<getMaskFromXmp> can't find mask info");
            return false;
        }

        byte[] mask;
        Rect rect;
        Point point;
        Log.d(TAG, "<getMaskFromXmp>, width:" + maskInfo.mMaskWidth + ",height:"
                + maskInfo.mMaskHeight + "imageWidth:" + mImageWidth + ",imageHeight:"
                + mImageHeight);

        if (maskInfo.mMaskWidth != mImageWidth || maskInfo.mMaskHeight != mImageHeight) {
            return false;
        } else {
            mask = maskInfo.mMaskBuffer;
            rect = new Rect(maskInfo.mSegmentLeft, maskInfo.mSegmentTop, maskInfo.mSegmentRight,
                    maskInfo.mSegmentBottom);
            point = new Point(maskInfo.mSegmentX, maskInfo.mSegmentY);
        }
        return nativeInitSegmentMask(mask, rect, point);
    }

    static {
        // The runtime will add "lib" on the front and ".o" on the end of
        // the name supplied to loadLibrary.
        System.loadLibrary("jni_segment");
    }

    private native boolean nativeInit(byte[] bitmap, int imgWidth, int imgHeight, byte[] maskBuf,
            int maskWidth, int maskHeight, byte[] occImgBuf, int occImgWidth, int occImgHeight,
            int scribbleWidth, int scribbleHeight, int faceNum, Object[] faceRect, int[] faceRip,
            int orientation);

    private native boolean nativeDoSegment(int scenario, int mode, byte[] scribbleBuf,
            Object roiRect);

    private native boolean nativeUndoSegment();

    private native boolean nativeInitSegmentMask(byte[] mask, Object rect, Object point);

    private native byte[] nativeGetSegmentMask(int widht, int height, boolean isNew);

    private native Object nativeGetSegmentPoint(boolean isNew);

    private native Object nativeGetSegmentRect(boolean isNew);

    private native Object nativeGetSegmentImg(Object oriImg, int oriWidth, int oriHeight,
            Object newImg, int newWidth, int newHeight, boolean isNew);

    private native Object nativeFillMaskToImg(Object bitmap, int width, int height);

    private native boolean nativeSetNewBitmap(Object bitmap, int bitmapWidth, int bitmapHeight,
            byte[] mask, int maskWidth, int maskHeight);

    private native void nativeRelease();
}
