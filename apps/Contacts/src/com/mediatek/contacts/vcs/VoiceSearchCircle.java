/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
package com.mediatek.contacts.vcs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

import com.android.contacts.R;

import com.mediatek.contacts.util.Log;

import java.util.ArrayList;

public class VoiceSearchCircle extends View {
    private static final String TAG = "VoiceSearchCircle";

    private final Context mContext;
    private ArrayList<SubCircle> mSubCircleList;

    private int mWidth;
    private int mHeight;
    private float mVelocity;
    private float mBitmapRadius;
    private float mOriginalRadius;
    private float mMaxRadius;
    private boolean mDrawLastCircle;
    private boolean mStopDrawCircle;

    private static final int TOTAL_TIME = 250; // ms
    private static final int DELAY = 33; // ms
    private static final int MAX_RADIUS = 70; // dp
    private static final int CIRCLE_STROKE_WIDTH = 3;
    private static final int MSG_DRAW = 0;

    CircleDrawListener mDrawListener;

    public VoiceSearchCircle(Context context) {
        this(context, null);
    }

    public VoiceSearchCircle(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.i(TAG, "[VoiceSearchCircle]new...");
        mContext = context;
        mSubCircleList = new ArrayList<SubCircle>();
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_voice_search);
        mBitmapRadius = bitmap.getWidth() / 2;
        mOriginalRadius = mBitmapRadius + dip2px(4);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        Log.d(TAG, "[onWindowFocusChanged]..");
        mWidth = getWidth();
        mHeight = getHeight();
        mMaxRadius = dip2px(MAX_RADIUS);
        mVelocity = (mMaxRadius / TOTAL_TIME);
        Log.d(TAG, "[onWindowFocusChanged] mMaxRadius: " + mMaxRadius + "getWidth(): "
                + getWidth() + "getHeight(): " + getHeight() + "mVelocity: " + mVelocity);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mStopDrawCircle) {
            Log.d(TAG, "[onDraw]Stop draw..");
            return;
        }

        if (mWidth > 0 && mHeight > 0) {
            Log.d(TAG, "[onDraw] mRadius: " + mOriginalRadius + " ,getWidth()" + mWidth
                    + " ,getHeight(): " + mHeight);
            if (mSubCircleList.size() <= 0) {
                mSubCircleList.add(generateSubCircle());
            }
            for (int i = 0; i < mSubCircleList.size(); i++) {
                if (mDrawLastCircle) {
                    // tell search panel moved
                    setVisibility(View.INVISIBLE);
                    mDrawLastCircle = false;
                    mDrawListener.circleDrawDone();
                    return;
                }

                SubCircle circle = mSubCircleList.get(i);
                int count = (TOTAL_TIME / DELAY) / 3;

                float radius = mOriginalRadius/** BitMap.getwidth()/2 **/
                + count * mVelocity * DELAY/** mMaxRadius/2 **/
                ;
                Log.d(TAG, "[onDraw]circle.mCount : " + circle.mCount + ",count: " + count
                        + ",radius: " + radius);
                circle.onDraw(canvas);
                if (circle.mRadius >= mMaxRadius + mOriginalRadius) {
                    mSubCircleList.remove(circle);
                } else if (circle.mRadius == radius && !mDrawLastCircle) {
                    Log.d(TAG, "[onDraw]circle.mRadius == radius:" + radius);
                    mSubCircleList.add(generateSubCircle());
                }
            }
        }

        if (mDrawLastCircle && mSubCircleList.size() <= 0) {
            // tell search panel moved
            setVisibility(View.INVISIBLE);
            mDrawLastCircle = false;
            mDrawListener.circleDrawDone();
        } else {
            mHander.sendEmptyMessageDelayed(MSG_DRAW, DELAY);
        }
    }

    interface CircleDrawListener {
        /**
         * Means the last circle draw done
         */
        void circleDrawDone();
    }

    private SubCircle generateSubCircle() {
        float searchImageMargin = mContext.getResources().getDimension(
                R.dimen.vcs_search_image_margin);
        float searchPanelWidth = mContext.getResources().getDimension(R.dimen.vcs_people_row_width);
        float cx = (getWidth() - searchPanelWidth) / 2 + searchImageMargin + mBitmapRadius;
        float cy = getHeight() / 2;
        SubCircle circle = new SubCircle(mContext);
        circle.mCx = cx;
        circle.mCy = cy;
        circle.mRadius = mOriginalRadius;
        return circle;
    }

    public Handler mHander = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (MSG_DRAW == msg.what) {
                invalidate();
            }
        }
    };

    protected void show() {
        mStopDrawCircle = false;
        setVisibility(View.VISIBLE);
    }

    protected void dismiss() {
        mStopDrawCircle = true;
        setVisibility(View.INVISIBLE);
    }

    protected void drawLastCircle() {
        mDrawLastCircle = true;
    }

    protected int dip2px(float dpValue) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    class SubCircle {

        public int mCount;
        private Paint mPaint;
        private float mCx;
        private float mCy;
        private float mRadius;

        public SubCircle(Context context) {
            mPaint = new Paint();
            mPaint.setColor(mContext.getResources().getColor(R.color.vcs_people_name));
            mPaint.setAntiAlias(true);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(CIRCLE_STROKE_WIDTH);
        }

        protected void onDraw(Canvas canvas) {
            float alpha0 = 255f - mCount * (255f / TOTAL_TIME * DELAY);
            int alpha = (int) alpha0;

            mPaint.setAlpha(alpha >= 0 ? alpha : 0);
            mRadius = mOriginalRadius + mCount * mVelocity * DELAY;
            Log.d(TAG, "SubCircle onDraw radius: " + mRadius);
            Log.d(TAG, "[onDraw]alpha0: " + alpha0 + " ,alpha:" + alpha + ",mRadius = "
                    + mRadius);
            if (mRadius <= mMaxRadius + mOriginalRadius) {
                canvas.drawCircle(mCx, mCy, mRadius, mPaint);
            }
            mCount++;
        }
    }
}
