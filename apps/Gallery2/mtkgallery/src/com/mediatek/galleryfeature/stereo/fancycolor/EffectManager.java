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
import android.os.Handler;

import com.mediatek.galleryfeature.platform.PlatformHelper;
import com.mediatek.galleryframework.util.MtkLog;

import java.util.ArrayList;

/**
 * EffectManager manages fancy color effects and requests.
 */
class EffectManager implements EffectRequest.EffectRequestListener,
        ThumbnailLoadRequest.DataLoadingListener {
    private final static String TAG = "MtkGallery2/FancyColor/EffectManager";
    private int mEffectCount;
    private int mSavingEffectIndex;
    private Bitmap mOriPreviewBitmap;
    private Bitmap mOriThumbnailBitmap;
    private Bitmap mOriHiResBitmap;
    private String mSourcePath;
    private FancyColorJni mFancyColorJni;
    private ArrayList<String> mAllEffectList;
    private EffectListener mEffectListener;
    private DataLoadingListener mDataLoadingListener;

    /**
     * Create EffectManager.
     *
     * @param filePath
     *            source path
     * @param selectedEffects
     *            indicates which effect will be loaded e.g.[1,2,5,6] means
     *            effect 1, 2, 5,6 in mAllEffectList will be loaded
     * @param dataLoadingListener
     *            data listener
     */
    public EffectManager(String filePath, int[] selectedEffects,
            DataLoadingListener dataLoadingListener) {
        mAllEffectList = new ArrayList<String>();
        mSourcePath = filePath;
        mDataLoadingListener = dataLoadingListener;
        PlatformHelper.submitJob(new ThumbnailLoadRequest(mSourcePath,
                FancyColorHelper.TYPE_THUMBNAIL, FancyColorHelper
                        .getTargetSize(FancyColorHelper.TYPE_THUMBNAIL), this));
        mFancyColorJni = new FancyColorJni(filePath, selectedEffects);
        mAllEffectList = mFancyColorJni.getAllFancyColorEffects();
        mEffectCount = mFancyColorJni.getAllFancyColorEffectsCount();
    }

    @Override
    public void onEffectRequestDone(int index, Bitmap bitmap, int type) {
        if (mEffectListener != null) {
            mEffectListener.onEffectDone(index, bitmap, type);
            MtkLog.d(TAG, "<onEffectRequestDone> index " + index + ", type " + type + ", bitmap "
                    + bitmap);
        }
    }

    @Override
    public void onLoadingFinish(Bitmap bitmap, int type) {
        if (type == FancyColorHelper.TYPE_PREVIEW_THUMBNAIL) {
            mOriPreviewBitmap = bitmap;
        } else if (type == FancyColorHelper.TYPE_THUMBNAIL) {
            mOriThumbnailBitmap = bitmap;
        } else if (type == FancyColorHelper.TYPE_HIGH_RES_THUMBNAIL) {
            mOriHiResBitmap = bitmap;
        }
        if (!mFancyColorJni.initMaskBuffer(type, bitmap)) {
            MtkLog.d(TAG, "<onLoadingFinish> initMaskBuffer error!!");
            return;
        }
        MtkLog.d(TAG, "<onLoadingFinish> type " + type + ", bitmap " + bitmap);
        if (mDataLoadingListener != null) {
            mDataLoadingListener.onLoadingFinish(bitmap, type);
        }
        if (type == FancyColorHelper.TYPE_THUMBNAIL) {
            PlatformHelper.submitJob(new ThumbnailLoadRequest(mSourcePath,
                    FancyColorHelper.TYPE_PREVIEW_THUMBNAIL, FancyColorHelper
                            .getTargetSize(FancyColorHelper.TYPE_PREVIEW_THUMBNAIL), this));
        }
        if (type == FancyColorHelper.TYPE_HIGH_RES_THUMBNAIL) {
            requestEffectBitmap(mSavingEffectIndex, type);
        }
    }

    public FancyColorJni getFancyColorJniObject() {
        return mFancyColorJni;
    }

    public void registerEffect(EffectListener listener) {
        if (listener != null) {
            mEffectListener = listener;
        }
    }

    public int getAllEffectsCount() {
        return mEffectCount;
    }

    public ArrayList<String> getAllEffectsName() {
        return mAllEffectList;
    }

    public void unregisterAllEffect() {
        mEffectListener = null;
        mDataLoadingListener = null;
    }

    public void setHandler(Handler handler) {
        mFancyColorJni.setHandler(handler);
    }

    public void setMaskBufferToSegment() {
        mFancyColorJni.setMaskBufferToSegment();
    }

    public void requestEffectBitmap(int index, int type) {
        Bitmap bitmap = null;
        if (type == FancyColorHelper.TYPE_PREVIEW_THUMBNAIL && mOriPreviewBitmap != null) {
            bitmap = mOriPreviewBitmap;
        } else if (type == FancyColorHelper.TYPE_THUMBNAIL && mOriThumbnailBitmap != null) {
            bitmap = mOriThumbnailBitmap;
        } else if (type == FancyColorHelper.TYPE_HIGH_RES_THUMBNAIL) {
            if (mOriHiResBitmap == null) {
                mSavingEffectIndex = index;
                PlatformHelper.submitJob(new ThumbnailLoadRequest(mSourcePath,
                        FancyColorHelper.TYPE_HIGH_RES_THUMBNAIL, FancyColorHelper
                                .getTargetSize(FancyColorHelper.TYPE_HIGH_RES_THUMBNAIL), this));
                return;
            }
            bitmap = mOriHiResBitmap;
        } else {
            MtkLog.d(TAG, "<requestEffectBitmap> error, index " + index + ", type " + type);
            return;
        }
        PlatformHelper.submitJob(new EffectRequest(mFancyColorJni, index,
                mAllEffectList.get(index), bitmap, type, this));
        MtkLog.d(TAG, "<requestEffectBitmap> submit EffectRequest index " + index);
    }

    /**
     * A listener to effect processing.
     */
    public interface EffectListener {
        public void onEffectDone(int index, Bitmap bitmap, int type);
    }

    /**
     * A listener to data loading.
     */
    public interface DataLoadingListener {
        public void onLoadingFinish(Bitmap bitmap, int type);
    }
}
