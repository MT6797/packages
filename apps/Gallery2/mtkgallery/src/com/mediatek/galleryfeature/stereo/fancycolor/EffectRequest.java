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

import com.mediatek.gallery3d.util.TraceHelper;
import com.mediatek.galleryframework.base.Work;
import com.mediatek.galleryframework.util.MtkLog;

/**
 * Encapsulation of effect request.
 */
class EffectRequest implements Work<Void> {
    private final static String TAG = "MtkGallery2/FancyColor/EffectRequest";
    private int mIndex;
    private int mType;
    private Bitmap mOriBitmap;
    private String mEffectName;
    private FancyColorJni mFancyColorJni;
    private EffectRequestListener mListener;

    public EffectRequest(FancyColorJni fancyColorJni, int index, String effectName, Bitmap bitmap,
            int type, EffectRequestListener listener) {
        mEffectName = effectName;
        mFancyColorJni = fancyColorJni;
        mIndex = index;
        mOriBitmap = bitmap;
        mType = type;
        mListener = listener;
    }

    @Override
    public Void run() {
        long start = System.currentTimeMillis();
        TraceHelper.traceBegin(">>>>FancyColor-EffectRequest");
        Bitmap bitmap = mFancyColorJni.getFancyColorEffectImage(mOriBitmap, mEffectName, mType);
        long cost = System.currentTimeMillis() - start;
        MtkLog.d(TAG, "<run> EffectRequest index " + mIndex + ", " + mEffectName + ", mType "
                + mType + " costs " + cost);
        if (mListener != null) {
            mListener.onEffectRequestDone(mIndex, bitmap, mType);
        }
        TraceHelper.traceEnd();
        return null;
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    /**
     * EffectRequestListener.
     *
     */
    public interface EffectRequestListener {
        /**
         * onEffectRequestDone.
         *
         * @param index
         *            index
         * @param bitmap
         *            bitmap
         * @param type
         *            type
         */
        public void onEffectRequestDone(int index, Bitmap bitmap, int type);
    }
}
