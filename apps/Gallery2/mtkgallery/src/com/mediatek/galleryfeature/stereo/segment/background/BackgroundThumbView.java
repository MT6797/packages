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
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.view.View;

import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryActivity;

import com.mediatek.galleryfeature.stereo.segment.ImageLoader;
import com.mediatek.galleryframework.util.MtkLog;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A thumbnail on the bottom background thumbnail track.
 */
class BackgroundThumbView extends IconView implements View.OnClickListener {
    private static final String LOGTAG = "MtkGallery2/SegmentApp/BackgroundThumbView";

    public static final int VERTICAL = 0;
    public static final int HORIZONTAL = 1;
    private static final int MAX_SIDE_DECODE_LENGTH = 240;
    private static final int MAX_SIDE_DISPLAY_LENGTH = 160;
    private static final int STROKE_RATIO = 3;

    /**
     * Thumbanil generating info encapsulation.<br/>
     * TODO move this to adapter.
     */
    private static class ThumbViewGenerator {
        final BackgroundThumbView mThumbView;

        public ThumbViewGenerator(BackgroundThumbView thumbView) {
            mThumbView = thumbView;
        }
    }

    /**
     * Work thread to generate thubmanils.<br/>
     * TODO move this work to adapter.
     */
    private static class Secretary extends Thread {
        // TODO: use blocking stack is more friendly, to be modified
        private final BlockingQueue<ThumbViewGenerator> mThumbViewGeneratorQueue;
        private ThumbViewGenerator mCurrentThumbViewGenerator;

        public Secretary(String threadName) {
            super("Segment - BackgroundThumbView - Secretary" + threadName);
            mThumbViewGeneratorQueue = new LinkedBlockingQueue<ThumbViewGenerator>();
        }

        public void run() {
            try {
                ThumbViewGenerator currentMediaGenerator;
                while (!Thread.currentThread().isInterrupted()) {
                    currentMediaGenerator = mThumbViewGeneratorQueue.take();
                    synchronized (Secretary.this) {
                        mCurrentThumbViewGenerator = currentMediaGenerator;
                    }

                    final BackgroundThumbView thumbView = mCurrentThumbViewGenerator.mThumbView;
                    String imgPath = thumbView.mAction.getUri();
                    if (thumbView.mBitmap == null && imgPath != null) {
                        BitmapFactory.Options ops = new BitmapFactory.Options();
                        ops.inSampleSize = getSampleSizeByMaxSideLength(Uri.parse(imgPath),
                                thumbView.getContext(), MAX_SIDE_DECODE_LENGTH, false);
                        Bitmap bitmap = ImageLoader.loadOrientedBitmap(thumbView.getContext(),
                                Uri.parse(imgPath), ops);
                        if (bitmap != null && bitmap.getWidth() > MAX_SIDE_DISPLAY_LENGTH) {
                            int sw = MAX_SIDE_DISPLAY_LENGTH;
                            int sh = (int) (sw * (float) bitmap.getHeight() / bitmap.getWidth());
                            if (sh == 0) {
                                sh = 1;
                            }
                            bitmap = Bitmap.createScaledBitmap(bitmap, sw, sh, true);
                        }
                        if (bitmap != null) {
                            MtkLog.d(LOGTAG, "<Secretary.run> imgPath = " + imgPath + ", bitmap = "
                                    + bitmap + ", size [" + bitmap.getWidth() + ", "
                                    + bitmap.getHeight() + "]");
                            thumbView.setBitmap(bitmap);
                            thumbView.postInvalidate();
                        } else {
                            thumbView.mAction.getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mThumbViewGeneratorQueue.clear();   // clear to re-enqueue
                                    thumbView.mAdapter.remove(thumbView.mAction);
                                    thumbView.mAdapter.saveActions();
                                    thumbView.invalidate();
                                }
                            });
                        }
                    }
                }
            } catch (InterruptedException e) {
                MtkLog.e(LOGTAG, "<Secretary.run> Terminating " + getName());
                this.interrupt();
            }
        }

        private void submit(ThumbViewGenerator mediaItem) {
            if (isAlive()) {
                MtkLog.e(LOGTAG, "<Secretary.submit> " + mediaItem.mThumbView.mAction.getUri());
                mThumbViewGeneratorQueue.add(mediaItem);
            }
        }

        private static int getSampleSizeByMaxSideLength(Uri uri, Context context,
                int maxSideLength, boolean useMin) {
            if (maxSideLength <= 0 || uri == null || context == null) {
                throw new IllegalArgumentException("bad argument to getScaledBitmap");
            }

            Rect storedBounds = ImageLoader.loadBitmapBounds(context, uri);
            int w = storedBounds.width();
            int h = storedBounds.height();

            // If bitmap cannot be decoded, return null
            if (w <= 0 || h <= 0) {
                return 1;
            }

            // Find best downsampling size
            int imageSide = 0;
            if (useMin) {
                imageSide = Math.min(w, h);
            } else {
                imageSide = Math.max(w, h);
            }
            int sampleSize = 1;
            while (imageSide > maxSideLength) {
                imageSide >>>= 1;
                sampleSize <<= 1;
            }

            // Make sure sample size is reasonable
            if (sampleSize <= 0 || 0 >= (int) (Math.min(w, h) / sampleSize)) {
                return 1;
            }
            return sampleSize;
        }
    }

    private BackgroundThumbAction mAction;
    private Paint mSelectPaint;
    private BackgroundThumbAdapter mAdapter;
    private Paint mBorderPaint;
    private int mSelectionStroke;
    private int mBorderStroke;
    private int mSelectionColor = Color.WHITE;
    private int mSpacerColor = Color.WHITE;

    private static Secretary sSecretary = new Secretary("yeah");
    static {
        sSecretary.start();
    }

    public BackgroundThumbView(Context context) {
        super(context);
        setOnClickListener(this);
        Resources res = getResources();
        mSelectionStroke = res.getDimensionPixelSize(R.dimen.thumbnail_margin);
        mSelectPaint = new Paint();
        mSelectPaint.setStyle(Paint.Style.FILL);
        mSelectionColor = res.getColor(R.color.filtershow_category_selection);
        mSpacerColor = res.getColor(R.color.filtershow_categoryview_text);

        mSelectPaint.setColor(mSelectionColor);
        mBorderPaint = new Paint(mSelectPaint);
        mBorderPaint.setColor(Color.BLACK);
        mBorderStroke = mSelectionStroke / STROKE_RATIO;
    }

    public void setAction(BackgroundThumbAction action, BackgroundThumbAdapter adapter) {
        mAction = action;
        // setText(mAction.getName());
        mAdapter = adapter;
        setUseOnlyDrawable(false);
        if (mAction.getType() == BackgroundThumbAction.ADD_ACTION) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.filtershow_add);
            setBitmap(bitmap);
            setUseOnlyDrawable(true);
            setText(getResources().getString(R.string.filtershow_add_button_looks));
        } else {
            String imgPath = mAction.getUri();
            if (mBitmap == null && imgPath != null) {
                sSecretary.submit(new ThumbViewGenerator(this));
            }
        }
        invalidate();
    }

    @Override
    public boolean isHalfImage() {
        if ((mAction != null)
                && ((mAction.getType() == BackgroundThumbAction.CROP_VIEW
                || mAction.getType() == BackgroundThumbAction.ADD_ACTION))) {
            return true;
        }
        return false;
    }

    @Override
    public boolean needsCenterText() {
        if (mAction != null && mAction.getType() == BackgroundThumbAction.ADD_ACTION) {
            return true;
        }
        return super.needsCenterText();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mAction == null) {
            return;
        }

        if (mAction.getType() == BackgroundThumbAction.ADD_ACTION) {
            onDrawText(canvas, getResources().getString(R.string.m_gallery));
        } else if (mAction.isOriginal()) {
            onDrawText(canvas, getResources().getString(R.string.m_original));
        } else {
            super.onDraw(canvas);
        }

        if (mAdapter.isSelected(this)) {
            drawSelection(canvas, 0, 0, getWidth(), getHeight(), mSelectionStroke, mSelectPaint,
                    mBorderStroke, mBorderPaint);
        }
    }

    @Override
    public void onClick(View view) {
        StereoBackgroundActivity activity = mAction.getActivity();
        if (mAction.getType() == BackgroundThumbAction.ADD_ACTION) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setClass(activity, GalleryActivity.class);
            intent.putExtra(SegmentBackgroundFilter.FILTER_TAG, SegmentBackgroundFilter.FILTER_TAG);
            activity.startActivityForResult(intent,
                    StereoBackgroundActivity.REQUEST_CODE_PICK_FROM_GALLERY);
        } else {
            if (mAction.isOriginal()) {
                activity.changeBitmap(null);
            } else {
                String imgPath = mAction.getUri();
                Uri uri = Uri.parse(imgPath);
                activity.changeBitmap(uri);
            }
            mAdapter.setSelected(this);
        }
    }

    private static void drawSelection(Canvas canvas, int left, int top, int right, int bottom,
            int stroke, Paint selectPaint, int border, Paint borderPaint) {
        canvas.drawRect(left, top, right, top + stroke, selectPaint);
        canvas.drawRect(left, bottom - stroke, right, bottom, selectPaint);
        canvas.drawRect(left, top, left + stroke, bottom, selectPaint);
        canvas.drawRect(right - stroke, top, right, bottom, selectPaint);
        canvas.drawRect(left + stroke, top + stroke, right - stroke, top + stroke + border,
                borderPaint);
        canvas.drawRect(left + stroke, bottom - stroke - border, right - stroke, bottom - stroke,
                borderPaint);
        canvas.drawRect(left + stroke, top + stroke, left + stroke + border, bottom - stroke,
                borderPaint);
        canvas.drawRect(right - stroke - border, top + stroke, right - stroke, bottom - stroke,
                borderPaint);
    }
}
