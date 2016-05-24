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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.mediatek.galleryfeature.stereo.segment.ImageShow;
import com.mediatek.galleryframework.util.MtkLog;

/**
 * ImageShow for StereoSynthActivity. See
 * com.mediatek.galleryfeature.stereo.segment.ImageShow.
 */
public class ImageShowSynth extends ImageShow implements OverLayController.StateListener {
    private static final String LOGTAG = "MtkGallery2/SegmentApp/ImageShowSynth";

    public static final int MODE_SRC_FOREMOST = 1;
    public static final int MODE_OBJ_FOREMOST = 2;

    private OverLayController mOverLayController;
    private Bitmap mBitmapCopySource;

    private int mMode;

    /**
     * Constructor.
     *
     * @param context
     *            The Context the view is running in, through which it can
     *            access the current theme, resources, etc.
     * @param attrs
     *            The attributes of the XML tag that is inflating the view.
     */
    public ImageShowSynth(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (mOverLayController == null) {
            mOverLayController = new OverLayController(context, this);
        }
    }

    /**
     * Constructor.
     *
     * @param context
     *            The Context the view is running in, through which it can
     *            access the current theme, resources, etc.
     */
    public ImageShowSynth(Context context) {
        super(context);
        if (mOverLayController == null) {
            mOverLayController = new OverLayController(context, this);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed && mOverLayController != null) {
            mOverLayController.configChanged();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mOverLayController.getTouchEvent(event);
        super.onTouchEvent(event);
        return true;
    }

    @Override
    public void doDraw(Canvas canvas) {
        Bitmap preview = mMasterImage.getBitmap();

        canvas.save();

        drawImages(canvas, preview);

        if (mBitmapCopySource != null) {
            if (mMode == MODE_SRC_FOREMOST || mMaskSimulator == null) {
                mOverLayController.drawBitmap(canvas, preview, mBitmapCopySource);
                // DebugUtils.dumpBitmap(fgBmp, "synth_foreground");
            } else {
                mOverLayController.drawBitmap(canvas, preview, mBitmapCopySource);
                Bitmap fgBmp = Bitmap.createBitmap(preview.getWidth(), preview.getHeight(), preview
                        .getConfig());
                Canvas fgCanvas = new Canvas(fgBmp);
                fgCanvas.drawBitmap(mMaskSimulator.getForground(preview), mMaskSimulator
                        .getClippingBox().left, mMaskSimulator.getClippingBox().top, mPaint);
                drawImages(canvas, fgBmp);
                fgBmp.recycle();
                fgBmp = null;
            }
        }

        canvas.restore();
    }

    @Override
    public void invaliable() {
        invalidate();
    }

    /**
     * Get the synthesized bitmap after editing.
     *
     * @return the background substituted bitmap.
     */
    public Bitmap getSynthBitmap() {
        if (mBitmapCopySource != null) {
            BitmapFactory.Options ops = new BitmapFactory.Options();
            ops.inMutable = true;
            Bitmap bgBmp = loadBitmap(getContext(), mMasterImage.getUri(), ops);
            if (bgBmp == null) {
                MtkLog.e(LOGTAG, "<getSynthBitmap> loading target bitmap failed!");
                return mMasterImage.getBitmap();
            }
            // Copy to make bitmap mutable
            // TODO StereoImage.decodeStereoImage() may accept ops to avoid copying.
            bgBmp = bgBmp.copy(Bitmap.Config.ARGB_8888, true);
            if (bgBmp == null) {
                MtkLog.e(LOGTAG, "<getSynthBitmap> fail to mutate target bitmap!");
                return mMasterImage.getBitmap();
            }
            Canvas bgCanvas = new Canvas(bgBmp);

            Bitmap fgBmp = Bitmap.createBitmap(bgBmp.getWidth(), bgBmp
                    .getHeight(), bgBmp.getConfig());
            Canvas fgCanvas = new Canvas(fgBmp);
            if (mMode == MODE_SRC_FOREMOST || mMaskSimulator == null) {
                if (mOverLayController != null) {
                    mOverLayController.drawBitmap(fgCanvas);
                }
            } else {
                if (mOverLayController != null) {
                    mOverLayController.drawBitmap(fgCanvas);
                }
                float scale = bgBmp.getWidth() / ((float) (mMasterImage.getBitmap().getWidth()));
                int left = (int) (mMaskSimulator.getClippingBox().left * scale);
                int top = (int) (mMaskSimulator.getClippingBox().top * scale);
                fgCanvas.drawBitmap(mMaskSimulator.getForground(bgBmp), left, top, mPaint);
            }

            bgCanvas.drawBitmap(fgBmp, 0, 0, mPaint);
            fgBmp.recycle();
            fgBmp = null;
            return bgBmp;
        }

        MtkLog.i(LOGTAG, "<getSynthBitmap> no source bitmap!");
        return mMasterImage.getBitmap();
    }

    public void setMode(int mode) {
        mMode = mode;
    }

    public void setCopySource(Bitmap src) {
        mBitmapCopySource = src;
    }
}
