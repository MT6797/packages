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

package com.mediatek.galleryfeature.stereo.segment.refine;

import android.app.Activity;
import android.widget.FrameLayout;

import com.android.gallery3d.R;

import com.mediatek.galleryframework.util.MtkLog;

import junit.framework.Assert;

/**
 * Animation indicator wrapper for selecting foreground object.
 */
public class FocusIndicator {
    private final static String TAG = "MtkGallery2/SegmentApp/FocusIndicator";

    private final FocusIndicatorLayout mFocusIndicator;

    /**
     * Constructor.
     *
     * @param activity
     *            contextual activity.
     */
    public FocusIndicator(Activity activity) {
        MtkLog.d(TAG, "<FocusIndicator>");
        Assert.assertNotNull(activity);
        mFocusIndicator = (FocusIndicatorLayout) activity.findViewById(R.id.focus_indicator_layout);
        Assert.assertNotNull(mFocusIndicator);
    }

    /**
     * Callback for single tap up event.
     *
     * @param ex
     *            x coordinate.
     * @param ey
     *            y coordinate.
     */
    public void onSingleTapUp(int ex, int ey) {
        MtkLog.d(TAG, "<onSingleTapUp> ex:" + ex + ",ey:" + ey);

        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) mFocusIndicator.getLayoutParams();

        int width = mFocusIndicator.getMeasuredWidth();
        int height = mFocusIndicator.getMeasuredHeight();

        MtkLog.d(TAG, "<onSingleTapUp> width:" + width + ",height:" + height);

        int left = Math.max(0, ex - width / 2);
        int top = Math.max(0, ey - height / 2);
        p.setMargins(left, top, 0, 0);

        MtkLog.d(TAG, "<onSingleTapUp> left:" + left + ",top:" + top);

        mFocusIndicator.clear();
        mFocusIndicator.showStart();
        mFocusIndicator.requestLayout();
    }
}
