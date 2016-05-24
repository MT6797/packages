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
package com.mediatek.galleryframework.base;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.android.gallery3d.R;
import com.mediatek.galleryframework.util.MtkLog;

import java.util.ArrayList;
/**
 * Displays an image, the top is an icon image, and the bottom is an string.
 */
public abstract class IconView extends ImageView {
    private final static String TAG = "MtkGallery2/IconView";
    public final static float ALPHA_ABLE_VIEW = 1.0f;
    public final static float ALPHA_DISABLE_VIEW = 0.3f;
    public static int sHorizontalMargin = -1;
    protected Bitmap mAbleBimtap = null;
    protected Bitmap mDisableBitmap = null;
    protected float mDisableAlpha = ALPHA_DISABLE_VIEW;
    protected boolean mIsDepthImage;
    protected Context mContext = null;
    protected int mTextId;
    protected int mDrawableId;
    protected int mDrawableDisableId;
    private static ArrayList<IconView> sViewList = new ArrayList<IconView>();
    private Paint mTextPaint = null;

    protected Bitmap getDrawableBitmap(Context context, int drawableId,
            int textId, float alpha) {
        String text = context.getResources().getString(textId);
        Paint paint = getTextPaint();
        FontMetricsInt fontMetrics = paint.getFontMetricsInt();
        paint.setColor(alpha == 1 ? Color.WHITE : Color.GRAY);
        int lenght = (int) paint.measureText(text);
        int textheight = (int) Math.ceil(fontMetrics.descent
                - fontMetrics.ascent);
        Options option = new Options();
        option.inMutable = true;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),
                drawableId, option);
        int rightMargin = (sHorizontalMargin - bitmap.getWidth()) / 2;
        MtkLog.d(TAG, " fontMetrics.descent = " + fontMetrics.descent
                + " fontMetrics.ascent = " + fontMetrics.ascent
                + " TextLenght = " + lenght + " rightMargin= " + rightMargin);
        Bitmap newBitmap = Bitmap.createBitmap(
                rightMargin * 2 + bitmap.getWidth(), bitmap.getHeight()
                        + textheight, Bitmap.Config.ARGB_8888);
        Canvas temp = new Canvas(newBitmap);
        temp.drawBitmap(bitmap, rightMargin, 0, null);
        temp.drawText(text, newBitmap.getWidth() / 2, bitmap.getHeight()
                - fontMetrics.ascent, paint);
        return newBitmap;
    }

    /**
     * Constructor that is called when inflating a view from XML. This is called
     * when a view is being constructed from an XML file, supplying attributes
     * that were specified in the XML file. This version uses a default style of
     * 0, so the only attribute values applied are those in the Context's Theme
     * and the given AttributeSet.
     * The method onFinishInflate() will be called after all children have been
     * added.
     * @param context the Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @param attrs the attributes of the XML tag that is inflating the view.
     * @see #View(Context, AttributeSet)
     */
    public IconView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a
     * theme attribute. This constructor of View allows subclasses to use their
     * own base style when they are inflating.
     * @param context the Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @param attrs the attributes of the XML tag that is inflating the view.
     * @param defStyle an attribute in the current theme that contains a
     *        reference to a style resource that supplies default values for
     *        the view. Can be 0 to not look for defaults.
     * @see #View(Context, AttributeSet, int)
     */
    public IconView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        sViewList.add(this);
        MtkLog.i(TAG, "<IconView> this = " + this);
    }

    public void setMediaType(boolean isDepthImage) {
        mIsDepthImage = isDepthImage;
    }

    /**
     * Use for update current image.
     * @param isDepthImage flag to indicate that this view use able bitmap
     * or disable bitmap
     */
    public void updateImage(boolean isDepthImage) {
        mIsDepthImage = isDepthImage;
        createBitmap(mContext);
        setImageBitmap(mIsDepthImage ? mAbleBimtap : mDisableBitmap);
    }

    /**
     * Set the enabled state of this view.
     * @param visiable one of {@link #VISIBLE}, {@link #INVISIBLE}, or {@link #GONE}.
     * @param listener the callback that will run.
     */
    public void update(boolean visiable, OnClickListener listener) {
        super.setVisibility(visiable ? View.VISIBLE : View.GONE);
        setClickable(visiable);
        setOnClickListener(visiable ? listener : null);
    }

    /**
     * Recycle able bitmap and disable bitmap.
     */
    public void recycle() {
        MtkLog.d(TAG, "<recycle> this = " + this);
        sHorizontalMargin = -1;
        if (mAbleBimtap != null) {
            mAbleBimtap.recycle();
            mAbleBimtap = null;
        }
        if (mDisableBitmap != null) {
            mDisableBitmap.recycle();
            mDisableBitmap = null;
        }
        sViewList.clear();
    }

    private void createBitmap(Context context) {
        if (sHorizontalMargin == -1) {
            sHorizontalMargin = getMaxTextLenght();
            MtkLog.d(TAG, "<calculateTextLenght> sHorizontalMargin = " + sHorizontalMargin);
        }
        if (mAbleBimtap == null || mDisableBitmap == null) {
            mAbleBimtap = getDrawableBitmap(context, mDrawableId,
                    mTextId, ALPHA_ABLE_VIEW);
            mDisableBitmap = getDrawableBitmap(context, mDrawableDisableId,
                    mTextId, ALPHA_DISABLE_VIEW);
        }
    }

    protected int calculateTextLenght() {
        String text = mContext.getResources().getString(mTextId);
        Paint paint = getTextPaint();
        MtkLog.d(TAG, "<calculateTextLenght> textlenght = " + paint.measureText(text));
        return (int) paint.measureText(text);
    }

    private Paint getTextPaint() {
        if (mTextPaint == null) {
            mTextPaint = new Paint();
            mTextPaint.setAntiAlias(true);
            mTextPaint.setStyle(Paint.Style.FILL);
            mTextPaint.setStrokeWidth(1);
            int textSize = mContext.getResources().getDimensionPixelSize(
                    R.dimen.m_stereo_font_size);
            mTextPaint.setTextSize(textSize);
            mTextPaint.setTextAlign(Paint.Align.CENTER);
        }
        return mTextPaint;
    }

    private int getMaxTextLenght() {
        int textLength = 0;
        int size = sViewList.size();
        for (int i = 0; i < size; i++) {
            IconView view = sViewList.get(i);
            textLength = Math.max(textLength, view.calculateTextLenght());
        }
        return textLength;
    }
}
