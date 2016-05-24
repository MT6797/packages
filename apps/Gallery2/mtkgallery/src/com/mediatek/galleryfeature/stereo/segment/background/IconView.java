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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import com.android.gallery3d.R;

import java.io.IOException;
import java.io.InputStream;

/**
 * A View rendered by image and text.
 */
class IconView extends View {
    public static final int VERTICAL = 0;
    public static final int HORIZONTAL = 1;
    private static final int DEFAULT_MARGIN = 16;
    private static final int DEFAULT_TEXT_SIZE = 32;
    private static final int START_COLOR = Color.argb(0, 0, 0, 0);
    private static final int END_COLOR = Color.argb(200, 0, 0, 0);
    private static final int OUTTER_TEXT_STROKE_WIDTH = 3;

    protected Bitmap mBitmap;

    private String mText;
    private Paint mPaint = new Paint();
    private Rect mTextBounds = new Rect();
    private Rect mBitmapBounds;
    private int mTextColor;
    private int mBackgroundColor;
    private int mMargin = DEFAULT_MARGIN;
    private int mOrientation = HORIZONTAL;
    private int mTextSize = DEFAULT_TEXT_SIZE;
    private boolean mUseOnlyDrawable = false;

    public IconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
        int bitmapRsc = attrs.getAttributeResourceValue(
                "http://schemas.android.com/apk/res/android", "src", 0);
        Resources res = context.getResources();
        InputStream is = res.openRawResource(bitmapRsc);
        Bitmap bitmap = BitmapFactory.decodeStream(is);
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setBitmap(bitmap);
        setUseOnlyDrawable(true);
    }

    public IconView(Context context) {
        super(context);
        setup(context);
    }

    public int getOrientation() {
        return mOrientation;
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    public int getMargin() {
        return mMargin;
    }

    public int getTextSize() {
        return mTextSize;
    }

    public int getTextColor() {
        return mTextColor;
    }

    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    public void setText(String text) {
        mText = text;
    }

    public String getText() {
        return mText;
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public void setUseOnlyDrawable(boolean value) {
        mUseOnlyDrawable = value;
    }

    public Rect getBitmapBounds() {
        return mBitmapBounds;
    }

    @Override
    public CharSequence getContentDescription() {
        return mText;
    }

    public boolean isHalfImage() {
        return false;
    }

    public void computeBitmapBounds() {
        if (mUseOnlyDrawable) {
            mBitmapBounds = new Rect(mMargin / 2, mMargin, getWidth() - mMargin / 2, getHeight()
                    - mTextSize - 2 * mMargin);
        } else {
            if (getOrientation() == VERTICAL && isHalfImage()) {
                mBitmapBounds = new Rect(mMargin / 2, mMargin, getWidth() / 2, getHeight());
            } else {
                mBitmapBounds = new Rect(mMargin / 2, mMargin, getWidth() - mMargin / 2,
                        getHeight());
            }
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        canvas.drawColor(mBackgroundColor);
        computeBitmapBounds();
        computeTextPosition(getText());
        if (mBitmap != null) {
            canvas.save();
            canvas.clipRect(mBitmapBounds);
            Matrix m = new Matrix();
            if (mUseOnlyDrawable) {
                mPaint.setFilterBitmap(true);
                m.setRectToRect(new RectF(0, 0, mBitmap.getWidth(), mBitmap.getHeight()),
                        new RectF(mBitmapBounds), Matrix.ScaleToFit.CENTER);
            } else {
                float scaleWidth = mBitmapBounds.width() / (float) mBitmap.getWidth();
                float scaleHeight = mBitmapBounds.height() / (float) mBitmap.getHeight();
                float scale = Math.max(scaleWidth, scaleHeight);
                float dx = (mBitmapBounds.width() - (mBitmap.getWidth() * scale)) / 2f;
                float dy = (mBitmapBounds.height() - (mBitmap.getHeight() * scale)) / 2f;
                dx += mBitmapBounds.left;
                dy += mBitmapBounds.top;
                m.postScale(scale, scale);
                m.postTranslate(dx, dy);
            }

            canvas.drawBitmap(mBitmap, m, mPaint);
            canvas.restore();
        }

        if (!mUseOnlyDrawable) {
            float start = getHeight() - 2 * mMargin - 2 * mTextSize;
            float end = getHeight();
            Shader shader = new LinearGradient(0, start, 0, end, START_COLOR, END_COLOR,
                    Shader.TileMode.CLAMP);
            mPaint.setShader(shader);
            float startGradient = 0;
            if (getOrientation() == VERTICAL && isHalfImage()) {
                startGradient = getWidth() / 2;
            }
            canvas.drawRect(new RectF(startGradient, start, getWidth(), end), mPaint);
            mPaint.setShader(null);
        }
    }

    public void onDrawText(Canvas canvas, String text) {
        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        canvas.drawColor(mBackgroundColor);
        computeBitmapBounds();
        computeTextPosition(/* getText() */text);
        drawOutlinedText(canvas, /* getText() */text);
    }

    protected boolean needsCenterText() {
        if (mOrientation == HORIZONTAL) {
            return true;
        }
        return false;
    }

    protected void computeTextPosition(String text) {
        if (text == null) {
            return;
        }
        mPaint.setTextSize(mTextSize);
        if (getOrientation() == VERTICAL) {
            text = text.toUpperCase();
            // TODO: set this in xml
            mPaint.setTypeface(Typeface.DEFAULT_BOLD);
        }
        mPaint.getTextBounds(text, 0, text.length(), mTextBounds);
    }

    protected void drawText(Canvas canvas, String text) {
        if (text == null) {
            return;
        }
        float textWidth = mPaint.measureText(text);
        int x = (int) (canvas.getWidth() - textWidth - 2 * mMargin);
        if (needsCenterText()) {
            x = (int) ((canvas.getWidth() - textWidth) / 2.0f);
        }
        if (x < 0) {
            // If the text takes more than the view width,
            // justify to the left.
            x = mMargin;
        }
        int y = canvas.getHeight() - 2 * mMargin;
        y = (int) ((canvas.getHeight()/* - mPaint.measureText("oo") */) / 2 + mMargin);
        canvas.drawText(text, x, y, mPaint);
    }

    protected void drawOutlinedText(Canvas canvas, String text) {
        mPaint.setColor(getBackgroundColor());
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(OUTTER_TEXT_STROKE_WIDTH);
        drawText(canvas, text);
        mPaint.setColor(getTextColor());
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeWidth(1);
        drawText(canvas, text);
    }

    private void setup(Context context) {
        Resources res = getResources();
        mTextColor = res.getColor(R.color.m_background_thumbnail_track_text);
        mBackgroundColor = res.getColor(R.color.m_background_thumbnail_track_background);
        mMargin = res.getDimensionPixelOffset(R.dimen.category_panel_margin);
        mTextSize = res.getDimensionPixelSize(R.dimen.category_panel_text_size);
    }
}
