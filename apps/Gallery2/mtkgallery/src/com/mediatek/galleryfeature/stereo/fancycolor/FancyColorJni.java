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
package com.mediatek.galleryfeature.stereo.fancycolor;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.text.format.Time;

import com.mediatek.gallery3d.util.TraceHelper;
import com.mediatek.galleryfeature.stereo.SegmentJni;
import com.mediatek.galleryframework.util.MtkLog;
import com.mediatek.xmp.SegmentMaskOperator.SegmentMaskInfo;
import com.mediatek.xmp.XmpOperator;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Fancy Color Jni.
 */
class FancyColorJni {
    private final static String TAG = "MtkGallery2/FancyColor/FancyColorJni";
    private final static int H_VALUE = 230; // 180 + 50;
    private static final int BIT_SHIFT_BLUE = 0;
    private static final int BIT_SHIFT_GREEN = 8;
    private static final int BIT_SHIFT_RED = 16;
    private static final int WHITE_COLOR_MASK = 0xFF;
    private static final int SMALL_BM_THRESHOLD = 64;
    private static final int LARGE_BM_THRESHOLD = 256;
    private static final int K_MEANS_P = 4;

    private Point mMaskPoint = new Point();
    private Rect mMaskRect = new Rect();

    private byte[] mImageMask;
    private byte[] mPreviewMask;

    private int mPreviewMaskWidth;
    private int mPreviewMaskHeight;
    private int mMaskWidth;
    private int mMaskHeight;
    private Point mPreviewMaskPoint = new Point();
    private Rect mPreviewRect = new Rect();

    private Handler mHandler;
    private int mEffectCount;
    private String mFilePath;
    private Bitmap mOriPreviewBitmap;
    private Bitmap mOriThumbnailBitmap;
    private Bitmap mOriHiResBitmap;
    private ArrayList<String> mAllEffectArray = new ArrayList<String>();
    private ArrayList<String> mSelectedEffectArray = new ArrayList<String>();

    static {
        System.loadLibrary("jni_fancycolor");
    }

    public FancyColorJni(String filePath, int[] selectedEffects) {
        mFilePath = filePath;
        if (mAllEffectArray.size() == 0) {
            int effectCount = getFancyColorEffectsCount();
            Object[] effects = getFancyColorEffects();
            for (int i = 0; i < effectCount; i++) {
                mAllEffectArray.add(i, (String) effects[i]);
            }
            mAllEffectArray.add(effectCount, "imageFilterBwFilter");
            mAllEffectArray.add(effectCount + 1, "imageFilterKMeans");
            if (selectedEffects == null) {
                mSelectedEffectArray = mAllEffectArray;
            } else {
                for (int i = 0; i < selectedEffects.length; i++) {
                    mSelectedEffectArray.add(i, mAllEffectArray.get(selectedEffects[i]));
                    MtkLog.d(TAG, "<FancyColorJni> load effect: " + mSelectedEffectArray.get(i));
                }
            }
            mEffectCount = mSelectedEffectArray.size();
        }
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public ArrayList<String> getAllFancyColorEffects() {
        return mSelectedEffectArray;
    }

    public int getAllFancyColorEffectsCount() {
        return mEffectCount;
    }

    public Bitmap getFancyColorEffectImage(Bitmap src, String effectName, int type) {
        if (src == null || effectName == null || mSelectedEffectArray == null
                || !mSelectedEffectArray.contains(effectName)) {
            MtkLog.d(TAG, "<getFancyColorEffectImage> params check error!");
            return null;
        }

        Bitmap temp = src.copy(Bitmap.Config.ARGB_8888, true);
        if (temp == null) {
            MtkLog.d(TAG, "<getFancyColorEffectImage> " + effectName + " copy bitmap error!");
            return null;
        }

        byte[] mask;
        Rect rect;
        Point point;
        Bitmap effectBitmap;
        if (type == FancyColorHelper.TYPE_PREVIEW_THUMBNAIL) {
            mask = mPreviewMask;
            rect = mPreviewRect;
            point = mPreviewMaskPoint;
        } else {
            mask = mImageMask;
            rect = mMaskRect;
            point = mMaskPoint;
        }
        MtkLog.d(TAG, "<getFancyColorEffectImage> effectName " + effectName + ", src bitmap w "
                + src.getWidth() + ", h " + src.getHeight() + ", mask size " + mask.length);
        TraceHelper.traceBegin(">>>>FancyColor-getFancyColorEffectImage: " + effectName + ": type "
                + type);
        switch (effectName) {
        case FancyColorHelper.EFFECT_NAME_MONO_CHROME:
            effectBitmap = imageFilterBwFilter(temp, mask);
            break;
        case FancyColorHelper.EFFECT_NAME_POSTERIZE:
            effectBitmap = imageFilterKMeans(temp, mask);
            break;
        default:
            int effectIndex = mAllEffectArray.indexOf(effectName);
            effectBitmap = (Bitmap) getFancyColorEffectImage(effectIndex, temp, mask,
                    src.getWidth(), src.getHeight(), rect, point.x, point.y);
        }
        TraceHelper.traceEnd();
        return effectBitmap;
    }

    public boolean initMaskBuffer(int type, Bitmap bitmap) {
        TraceHelper.traceBegin(">>>>FancyColor-initMaskBuffer");
        if (mImageMask == null) {
            getImageMask();
        }
        switch (type) {
        case FancyColorHelper.TYPE_THUMBNAIL:
            mOriThumbnailBitmap = bitmap;
            if (mImageMask == null) {
                return getThumbnailMask(bitmap, SegmentJni.SCENARIO_AUTO, null);
            }
            break;
        case FancyColorHelper.TYPE_PREVIEW_THUMBNAIL:
            return getPreviewMask(bitmap);
        case FancyColorHelper.TYPE_HIGH_RES_THUMBNAIL:
            return getHightResMask(bitmap);
        default:
            Assert.assertTrue(false);
            break;
        }
        TraceHelper.traceEnd();
        return true;
    }

    public void setMaskBufferToSegment() {
        SegmentMaskInfo maskInfo = new SegmentMaskInfo();
        maskInfo.mMaskBuffer = mImageMask;
        maskInfo.mMaskWidth = mMaskWidth;
        maskInfo.mMaskHeight = mMaskHeight;
        maskInfo.mSegmentX = mMaskPoint.x;
        maskInfo.mSegmentY = mMaskPoint.y;
        maskInfo.mSegmentLeft = mMaskRect.left;
        maskInfo.mSegmentTop = mMaskRect.top;
        maskInfo.mSegmentRight = mMaskRect.right;
        maskInfo.mSegmentBottom = mMaskRect.bottom;
        SegmentJni.setMaskBuffer(maskInfo);
    }

    public boolean reloadMaskBuffer(Point point) {
        MtkLog.d(TAG, "<reloadMaskBuffer> point:" + point);
        TraceHelper.traceBegin(">>>>FancyColor-reloadMaskBuffer");
        boolean res = false;
        if (point != null) {
            res = getThumbnailMask(mOriThumbnailBitmap, SegmentJni.SCENARIO_SELECTION, point);
        } else {
            res = getImageMask();
        }
        if (!res) {
            MtkLog.d(TAG, "<reloadMaskBuffer> fail!!!");
            return false;
        }
        res = getPreviewMask(mOriPreviewBitmap);
        TraceHelper.traceEnd();
        return res;
    }

    private boolean getImageMask() {
        XmpOperator xmpOperator = new XmpOperator();
        if (!xmpOperator.initialize(mFilePath)) {
            MtkLog.d(TAG, "mXmpOperator.initialize fail!!!!!");
            return false;
        }
        TraceHelper.traceBegin(">>>>FancyColor-getSegmentMaskInfoFromFile");
        SegmentMaskInfo maskInfo = xmpOperator.getSegmentMaskInfoFromFile(mFilePath);
        TraceHelper.traceEnd();
        if (maskInfo != null) {
            mImageMask = maskInfo.mMaskBuffer;
            mMaskWidth = maskInfo.mMaskWidth;
            mMaskHeight = maskInfo.mMaskHeight;
            mMaskPoint.x = maskInfo.mSegmentX;
            mMaskPoint.y = maskInfo.mSegmentY;
            mMaskRect.left = maskInfo.mSegmentLeft;
            mMaskRect.top = maskInfo.mSegmentTop;
            mMaskRect.right = maskInfo.mSegmentRight;
            mMaskRect.bottom = maskInfo.mSegmentBottom;
            MtkLog.d(TAG, "<initMaskBuffer> mask buffer exists in XMP, mMaskWidth " + mMaskWidth
                    + ", mMaskHeight " + mMaskHeight);
        }
        xmpOperator.deInitialize();
        return true;
    }

    private boolean getThumbnailMask(Bitmap bitmap, int scenario, Point point) {
        if (bitmap == null) {
            MtkLog.d(TAG, "<getThumbnailMask> bitmap is null, return!!");
            return false;
        }
        synchronized (this) {
            TraceHelper.traceBegin(">>>>FancyColor-getThumbnailMask");
            boolean val = false;
            SegmentJni segment = new SegmentJni();

            TraceHelper.traceBegin(">>>>FancyColor-getThumbnailMask-initSegment");
            val = segment.initSegment(mFilePath, bitmap, 0);
            TraceHelper.traceEnd();
            if (!val) {
                mHandler.sendEmptyMessage(FancyColorHelper.MSG_STATE_ERROR);
                MtkLog.d(TAG, "<getThumbnailMask> segment error, return!!");
                segment.release();
                return false;
            }

            TraceHelper.traceBegin(">>>>FancyColor-getThumbnailMask-doSegment");
            val = segment.doSegment(scenario, SegmentJni.MODE_OBJECT, null, null, point);
            TraceHelper.traceEnd();
            if (!val) {
                mHandler.sendEmptyMessage(FancyColorHelper.MSG_STATE_ERROR);
                MtkLog.d(TAG, "<getThumbnailMask> segment error, return!!");
                segment.release();
                return false;
            }

            mImageMask = segment.getSegmentMask();
            mMaskPoint = segment.getSegmentPoint();
            mMaskRect = segment.getSegmentRect();
            mMaskWidth = bitmap.getWidth();
            mMaskHeight = bitmap.getHeight();
            MtkLog.d(TAG, "<getThumbnailMask>,mImageMask size:" + mImageMask.length
                    + ", mMaskWidth " + mMaskWidth + ", mMaskHeight " + mMaskHeight);

            TraceHelper.traceBegin(">>>>FancyColor-getThumbnailMask-release");
            segment.release();
            TraceHelper.traceEnd();

            TraceHelper.traceEnd();
        }
        return true;
    }

    private boolean getPreviewMask(Bitmap bitmap) {
        if (mImageMask == null || bitmap == null) {
            Assert.assertTrue(false);
            return false;
        }
        MtkLog.d(TAG, "<getPreviewMask> mImageMask size" + mImageMask.length + ", mMaskWidth "
                + mMaskWidth + ", mMaskHeight " + mMaskHeight + ", bitmapWidth "
                + bitmap.getWidth() + ", bitmapHeight " + bitmap.getHeight());

        TraceHelper.traceBegin(">>>>FancyColor-getPreviewMask");
        mPreviewMask = resizeMask(mImageMask, mMaskWidth, mMaskHeight, bitmap.getWidth(),
                bitmap.getHeight());
        mPreviewMaskPoint = resizePoint(mMaskPoint, mMaskWidth, mMaskHeight, bitmap.getWidth(),
                bitmap.getHeight());
        mPreviewRect = resizeRect(mMaskRect, mMaskWidth, mMaskHeight, bitmap.getWidth(),
                bitmap.getHeight());
        mPreviewMaskWidth = bitmap.getWidth();
        mPreviewMaskHeight = bitmap.getHeight();
        mOriPreviewBitmap = bitmap;
        TraceHelper.traceEnd();

        return true;
    }

    private boolean getHightResMask(Bitmap bitmap) {
        if (mImageMask == null || bitmap == null) {
            Assert.assertTrue(false);
            return false;
        }
        MtkLog.d(TAG, "<getHightResMask> type:" + "TYPE_HIGH_RES_THUMBNAIL" + ",width:"
                + bitmap.getWidth() + ",height:" + bitmap.getHeight());
        TraceHelper.traceBegin(">>>>FancyColor-getHightResMask");
        synchronized (this) {
            TraceHelper.traceBegin(">>>>FancyColor-setNewBitmap");
            SegmentJni segment = new SegmentJni();
            segment.setNewBitmap(bitmap, mImageMask, mMaskWidth, mMaskHeight);
            TraceHelper.traceEnd();
            mImageMask = segment.getNewSegmentMask();
            mMaskPoint = segment.getNewSegmentPoint();
            mMaskRect = segment.getNewSegmentRect();
            mMaskWidth = bitmap.getWidth();
            mMaskHeight = bitmap.getHeight();
            segment.release();
        }
        TraceHelper.traceEnd();
        return true;
    }

    // effect mono chrome
    private static Bitmap imageFilterBwFilter(Bitmap bitmap, byte[] maskBuffer) {
        if (bitmap == null) {
            return null;
        }
        if (maskBuffer.length != bitmap.getWidth() * bitmap.getHeight()) {
            return null;
        }
        float[] hsv = new float[] { H_VALUE, 1, 1 };
        int rgb = Color.HSVToColor(hsv);
        int r = WHITE_COLOR_MASK & (rgb >> BIT_SHIFT_RED);
        int g = WHITE_COLOR_MASK & (rgb >> BIT_SHIFT_GREEN);
        int b = WHITE_COLOR_MASK & (rgb >> BIT_SHIFT_BLUE);
        return (Bitmap) imageFilterBwFilter((Object) bitmap, maskBuffer, bitmap.getWidth(),
                bitmap.getHeight(), r, g, b);
    }

    // effect posterize
    private static Bitmap imageFilterKMeans(Bitmap bitmap, byte[] maskBuffer) {
        if (bitmap == null) {
            return null;
        }
        if (maskBuffer.length != bitmap.getWidth() * bitmap.getHeight()) {
            return null;
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Bitmap largeBmDs = bitmap;
        Bitmap smallBmDs = bitmap;

        // find width/height for larger downsampled bitmap
        int lw = w;
        int lh = h;
        while (lw > LARGE_BM_THRESHOLD && lh > LARGE_BM_THRESHOLD) {
            lw /= 2;
            lh /= 2;
        }
        if (lw != w) {
            largeBmDs = Bitmap.createScaledBitmap(bitmap, lw, lh, true);
        }

        // find width/height for smaller downsampled bitmap
        int sw = lw;
        int sh = lh;
        while (sw > SMALL_BM_THRESHOLD && sh > SMALL_BM_THRESHOLD) {
            sw /= 2;
            sh /= 2;
        }
        if (sw != lw) {
            smallBmDs = Bitmap.createScaledBitmap(largeBmDs, sw, sh, true);
        }
        Time t = new Time();
        t.setToNow();
        int seed = (int) t.toMillis(false);
        return (Bitmap) imageFilterKMeans((Object) bitmap, maskBuffer, w, h, largeBmDs, lw, lh,
                smallBmDs, sw, sh, K_MEANS_P, seed);
    }

    private Point resizePoint(Point point, int width, int height, int destWidth, int destHeight) {
        Point dest = new Point();
        dest.x = point.x / (width / destWidth);
        dest.y = point.y / (height / destHeight);
        return dest;
    }

    private Rect resizeRect(Rect rect, int width, int height, int destWidth, int destHeight) {
        Rect dest = new Rect();
        dest.left = rect.left / (width / destWidth);
        dest.top = rect.top / (height / destHeight);
        dest.right = rect.right / (width / destWidth);
        dest.bottom = rect.bottom / (height / destHeight);
        return dest;
    }

    private static byte[] resizeMask(byte[] mask, int width, int height, int dstWidth,
            int dstHeight) {
        byte[] dst = nativeResizeMask(mask, width, height, dstWidth, dstHeight);
        return dst;
    }

    /********** exported native methods. ***********/
    private static native Object imageFilterKMeans(Object bitmap, byte[] mask, int widht,
            int height, Object largeDsBitmap, int lwidth, int lheight, Object smallDsBitmap,
            int swidth, int sheight, int p, int seed);

    private static native Object imageFilterBwFilter(Object bitmap, byte[] mask, int width,
            int height, int rw, int gw, int bw);

    private native int getFancyColorEffectsCount();

    private native Object[] getFancyColorEffects();

    private native Object getFancyColorEffectImage(int effectIndex, Object bitmap, byte[] mask,
            int width, int height, Object rect, int centerX, int centerY);

    private static native byte[] nativeResizeMask(byte[] mask, int width, int height,
            int dstWidth, int dstHeight);
}