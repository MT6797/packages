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

package com.mediatek.galleryfeature.stereo.segment.background;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.mediatek.galleryfeature.stereo.segment.ImageLoader;
import com.mediatek.galleryfeature.stereo.segment.ImageShow;
import com.mediatek.galleryframework.util.MtkLog;

/**
 * ImageShow in StereoBackgroundActivity. See
 * com.mediatek.galleryfeature.stereo.segment.ImageShow.
 */
public class ImageShowBackground extends ImageShow {
    private static final String LOGTAG = "MtkGallery2/SegmentApp/ImageShowBackground";

    private static final int INVALID_VIEW_WIDTH = -100;
    private RectF mRectForegroundObjScreen = new RectF();
    private RectF mRectForegroundObjImage = new RectF();
    private Uri mOriginalUri;
    private int mOriginalWidth;
    private int mOriginalHeight;
    private float mOriginalViewWidth = INVALID_VIEW_WIDTH;
    private float mForegroundScale = 1;

    /**
     * Constructor.
     *
     * @param context
     *            The Context the view is running in, through which it can
     *            access the current theme, resources, etc.
     * @param attrs
     *            The attributes of the XML tag that is inflating the view.
     */
    public ImageShowBackground(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Constructor.
     *
     * @param context
     *            The Context the view is running in, through which it can
     *            access the current theme, resources, etc.
     */
    public ImageShowBackground(Context context) {
        super(context);
    }

    /**
     * Get the synthesized bitmap after background substitution.
     *
     * @return the background substituted bitmap.
     */
    public Bitmap getSynthBitmap() {
        boolean result;

        // create a blank photo of the same dimension to the original photo
        Bitmap bmp = createBlankPhoto();
        Canvas cvs = new Canvas(bmp);

        // draw new background on the blank photo
        result = drawHighResBackground(cvs);
        if (!result) {
            return bmp;
        }

        // and then draw foreground object of the origianl photo on the photo
        drawHighResForeground(cvs);

        return bmp;
    }

    @Override
    public void doDraw(Canvas canvas) {
        if (mMaskSimulator == null) {
            super.doDraw(canvas);
            return;
        }

        if (!isScreenRotated()) {
            // TODO
            // we only need to constrain translation in onDraw() after screen
            // rotating
            // because gesture listener will handle the other cases.
            // here we constrain translation in any case; it is not
            // time-consuming.
            float scaleFactor = mMasterImage.getScaleFactor();
            Point translation = mMasterImage.getTranslation();
            constrainTranslation(translation, scaleFactor);
            mMasterImage.setTranslation(translation);
        }

        canvas.save();

        drawImages(canvas, mMasterImage.getBitmap());

        drawForeground(canvas);

        canvas.restore();
    }

    private void drawForeground(Canvas canvas) {
        updateForegroundRectsIfNeeded();

        if (!mMasterImage.getUri().equals(mOriginalUri)) {
            mRectForegroundObjImage = new RectF(mMaskSimulator.getClippingBox());
            mRectForegroundObjScreen.left = mRectForegroundObjImage.left
                    * (getLayoutParams().width) / (mOriginalWidth - 1);
            mRectForegroundObjScreen.right = mRectForegroundObjImage.right
                    * (getLayoutParams().width) / (mOriginalWidth - 1);
            mRectForegroundObjScreen.top = mRectForegroundObjImage.top * (getLayoutParams().height)
                    / (mOriginalHeight - 1);
            mRectForegroundObjScreen.bottom = mRectForegroundObjImage.bottom
                    * (getLayoutParams().height) / (mOriginalHeight - 1);
            Bitmap fore = mMaskSimulator.getForground(null);
            canvas.drawBitmap(fore, null, mRectForegroundObjScreen, mPaint);
            fore.recycle();
        }
    }

    private boolean isScreenRotated() {
        MtkLog.d(LOGTAG, "<isScreenRotated> getWidth() = " + getWidth() + ", getHeight() = "
                + getHeight());
        if (getLayoutParams().width == ViewGroup.LayoutParams.MATCH_PARENT
                && getLayoutParams().height == ViewGroup.LayoutParams.MATCH_PARENT
                && mOriginalUri != null) {
            return true;
        } else {
            return false;
        }
    }

    private void fixPhotoFrame() {
        if (mOriginalWidth != 0) {
            int viewWidth = ((ViewGroup) (getParent())).getWidth();
            int viewHeight = ((ViewGroup) (getParent())).getHeight();
            Matrix mat = new Matrix();
            RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
            MtkLog.d(LOGTAG, "<fixPhotoFrame> viewRect = " + viewRect);
            RectF imgRect = new RectF(0, 0, mOriginalWidth, mOriginalHeight);
            mat.setRectToRect(imgRect, viewRect, Matrix.ScaleToFit.CENTER);
            mat.mapRect(imgRect);
            imgRect.intersect(viewRect);
            MtkLog.d(LOGTAG, "<fixPhotoFrame> imgRect = " + imgRect);
            getLayoutParams().width = (int) (imgRect.width());
            getLayoutParams().height = (int) (imgRect.height());
            if (mOriginalViewWidth < 0) {
                mOriginalViewWidth = imgRect.width();
            } else {
                mForegroundScale = imgRect.width() / mOriginalViewWidth;
                mOriginalViewWidth = (int) (imgRect.width());
                mMasterImage.setTranslation(new Point(
                        (int) (mMasterImage.getTranslation().x * mForegroundScale),
                        (int) (mMasterImage.getTranslation().y * mForegroundScale)));
            }
            requestLayout();
        }
    }

    private void updateForegroundRectsIfNeeded() {
        if (mOriginalUri == null) {
            mOriginalUri = mMasterImage.getUri();
            MtkLog.d(LOGTAG, "<updateForegroundRectsIfNeeded> init original uri " + mOriginalUri);
            mOriginalWidth = mMasterImage.getBitmap().getWidth();
            mOriginalHeight = mMasterImage.getBitmap().getHeight();
            fixPhotoFrame();
        }

        if (isScreenRotated()) {
            fixPhotoFrame();
        }
    }

    private Bitmap createBlankPhoto() {
        Rect originalPhotoFrame = ImageLoader.loadBitmapBounds(getContext(), mOriginalUri);
        if ((originalPhotoFrame.width() > originalPhotoFrame.height())
                ^ (getWidth() > getHeight())) {
            originalPhotoFrame = new Rect(0, 0, originalPhotoFrame.height(), originalPhotoFrame
                    .width());
        }
        Bitmap bmp = Bitmap.createBitmap(originalPhotoFrame.width(), originalPhotoFrame.height(),
                Bitmap.Config.ARGB_8888);
        return bmp;
    }

    private boolean drawHighResForeground(Canvas cvs) {
        // 1. decode original photo without downsampling
        Bitmap originalPhoto = loadBitmap(getContext(), mOriginalUri, null);
        if (originalPhoto == null) {
            MtkLog.e(LOGTAG, "<drawHighResForeground> loading original photo failed!");
            return false;
        }

        // 2. get foreground object on original photo
        Bitmap highResForeground = mMaskSimulator.getForground(originalPhoto);

        // 3. get foreground object rect on original photo
        int originalWidth = originalPhoto.getWidth();
        int originalHeight = originalPhoto.getHeight();
        Rect foregroundRect = mMaskSimulator.getClippingBox();
        foregroundRect.scale(originalPhoto.getWidth() / (float) (mOriginalWidth));
        foregroundRect.intersect(new Rect(0, 0, originalWidth, originalHeight));
        originalPhoto.recycle();
        MtkLog.d(LOGTAG, "foregroundRect = " + foregroundRect);

        // 4. draw
        cvs.drawBitmap(highResForeground, null, foregroundRect, mPaint);
        highResForeground.recycle();

        return true;
    }

    private boolean drawHighResBackground(Canvas cvs) {
        // 1. decode new background without downsampling
        Bitmap newBackground = loadBitmap(getContext(), mMasterImage.getUri(), null);
        if (newBackground == null) {
            MtkLog.e(LOGTAG, "<drawHighResBackground> loading new background failed!");
            return false;
        }

        // 2. calculate the rect of the visible background region on the decoded bitmap
        Matrix viewToImageMatrix = getScreenToImageMatrix(true);
        if (viewToImageMatrix == null) {
            return false;
        }
        RectF tempR = new RectF(0, 0, getWidth(), getHeight());
        viewToImageMatrix.mapRect(tempR);
        Rect newBackgroundBound = mMasterImage.getOriginalBounds();
        Rect visibleRectOnNewBackground = new Rect((int) tempR.left, (int) tempR.top,
                (int) tempR.right, (int) tempR.bottom);
        visibleRectOnNewBackground.intersect(newBackgroundBound);
        MtkLog.d(LOGTAG, "visibleRectOnNewBackground = " + visibleRectOnNewBackground);

        // 3. calculate the rect of the region on the blank photo to put background
        Matrix centerMatrix = new Matrix();
        centerMatrix.reset();
        RectF placeRectOnCanvas = new RectF(0, 0, visibleRectOnNewBackground.width(),
                visibleRectOnNewBackground.height());
        RectF canvasRect = new RectF(0, 0, cvs.getWidth(), cvs.getHeight());
        centerMatrix.setRectToRect(placeRectOnCanvas, canvasRect, Matrix.ScaleToFit.CENTER);
        centerMatrix.mapRect(placeRectOnCanvas);
        MtkLog.d(LOGTAG, "imageRect = " + placeRectOnCanvas);

        // 4. draw
        cvs.drawBitmap(newBackground, visibleRectOnNewBackground, placeRectOnCanvas, mPaint);
        newBackground.recycle();

        return true;
    }
}
