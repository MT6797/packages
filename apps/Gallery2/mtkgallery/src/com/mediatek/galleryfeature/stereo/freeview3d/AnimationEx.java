/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. the information contained herein is
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

import com.mediatek.galleryframework.util.MtkLog;

/**
 * This is an Frame-by-Frame animation.
 */
public class AnimationEx {
    private final static String TAG = "MtkGallery2/AnimationEx";
    public final static int FRAME_COUNT = 25;
    public static final int TYPE_ANIMATION_CONTINUOUS = 1;
    public final static int SLEEP_TIME_INTERVAL = 2;
    public final static int STEP = 100;
    private long mEndTime;
    private int mCurrentFrameIndexX = Integer.MAX_VALUE;
    private int mCurrentFrameIndexY = Integer.MAX_VALUE;
    private int mTargetFrameIndexX = Integer.MAX_VALUE;
    private int mTargetFrameIndexY = Integer.MAX_VALUE;
    private int mFrameCoutX = FRAME_COUNT;
    private int mFrameCoutY = FRAME_COUNT;
    private boolean mIsInTransitionMode = false;

    /**
     * Constructor.
     * @param frameCountX
     *            the frame count of horizontal direction.
     * @param frameCountY
     *            the frame count of vertical direction.
     * @param currentFrameIndexX
     *            the begin frame index of horizontal direction.
     * @param currentFrameIndexY
     *            the begin frame index of vertical direction.
     * @param targetFrameX
     *            the target frame index of horizontal direction.
     * @param targetFrameY
     *            the target frame index of vertical direction.
     */
    public AnimationEx(int frameCountX, int frameCountY,
            int currentFrameIndexX, int currentFrameIndexY, int targetFrameX,
            int targetFrameY) {
        initAnimation(frameCountX, frameCountY, currentFrameIndexX,
                currentFrameIndexY, targetFrameX, targetFrameY);
    }

    /**
     * Set animation frame index and type.
     * @param lastIndexX the new target frame index of horizontal direction.
     * @param lastIndexY the new target frame index of vertical direction.
     * @return true the new target frame is equal the old target frame.
     */
    public boolean initAnimation(int lastIndexX, int lastIndexY) {
        if (mFrameCoutX <= lastIndexX) {
            lastIndexX = mFrameCoutX - 1;
        }
        if (mFrameCoutY <= lastIndexY) {
            lastIndexY = mFrameCoutY - 1;
        }
        if (lastIndexX < 0) {
            lastIndexX = 0;
        }
        if (lastIndexY < 0) {
            lastIndexY = 0;
        }
        if (mCurrentFrameIndexX == Integer.MAX_VALUE
                && lastIndexX != Integer.MAX_VALUE) {
            mCurrentFrameIndexX = lastIndexX;
        }
        if (mCurrentFrameIndexY == Integer.MAX_VALUE
                && lastIndexY != Integer.MAX_VALUE) {
            mCurrentFrameIndexY = lastIndexY;
        }
        boolean newPoints = (mTargetFrameIndexX != lastIndexX || mTargetFrameIndexY != lastIndexY);
        mTargetFrameIndexX = lastIndexX;
        mTargetFrameIndexY = lastIndexY;
        return newPoints;
    }

    public boolean isInTranslateMode() {
        return mIsInTransitionMode;
    }

    /**
     * Update animation frame by animation type.
     * @return whether the animation is finished.
     */
    public boolean advanceAnimation() {
        MtkLog.d(TAG, "<advanceAnimation>  mCurrentFrameIndexX ="
                + mCurrentFrameIndexX + " mCurrentFrameIndexY = "
                + mCurrentFrameIndexY + " mTargetFrameIndexX="
                + mTargetFrameIndexX + " mTargetFrameIndexY "
                + mTargetFrameIndexY + " mFrameCoutX="
                + mFrameCoutX + " mEndTime=" + mEndTime
                + " System.currentTimeMillis()=" + System.currentTimeMillis()
                + " mIsInTransitionMode=" + mIsInTransitionMode + " " + this);
        if (mCurrentFrameIndexX == Integer.MAX_VALUE || mTargetFrameIndexY == Integer.MAX_VALUE) {
            return true;
        }

        int dValueX = mCurrentFrameIndexX - mTargetFrameIndexX;
        mCurrentFrameIndexX = dValueX > 0 ? mCurrentFrameIndexX - 1
                : (dValueX < 0 ? mCurrentFrameIndexX + 1 : mCurrentFrameIndexX);
        int dValueY = mCurrentFrameIndexY - mTargetFrameIndexY;
        mCurrentFrameIndexY = dValueY > 0 ? mCurrentFrameIndexY - 1
                : (dValueY < 0 ? mCurrentFrameIndexY + 1 : mCurrentFrameIndexY);
        return isFinished();
    }

    public int[] getCurrentFrame() {
        return new int[] { mCurrentFrameIndexX, mCurrentFrameIndexY };
    }

    public int getTargetFrameIndexY() {
        return mTargetFrameIndexY;
    }

    public int getTargetFrameIndexX() {
        return mTargetFrameIndexX;
    }

    private boolean isFinished() {
        return mCurrentFrameIndexX == mTargetFrameIndexX
                && mCurrentFrameIndexY == mTargetFrameIndexY;
    }

    private void initAnimation(int frameCountX, int frameCountY,
            int currentFrameIndexX, int currentFrameIndexY, int targetFrameX,
            int targetFrameY) {
        mFrameCoutX = frameCountX;
        mFrameCoutY = frameCountY;
        mTargetFrameIndexX = targetFrameX;
        mTargetFrameIndexY = targetFrameY;
        mCurrentFrameIndexX = currentFrameIndexX;
        mCurrentFrameIndexY = currentFrameIndexY;
    }
}
