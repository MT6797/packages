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

package com.mediatek.gallery3d.adapter;

import android.graphics.Bitmap;

import com.android.gallery3d.R;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.AlbumDataLoader;
import com.android.gallery3d.app.Config;
import com.android.gallery3d.data.DataSourceType;
import com.android.gallery3d.data.LocalMediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.BitmapTexture;
import com.android.gallery3d.glrenderer.ColorTexture;
import com.android.gallery3d.glrenderer.FadeInTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.Texture;
import com.android.gallery3d.glrenderer.TiledTexture;
import com.android.gallery3d.ui.AbstractSlotRenderer;
import com.android.gallery3d.ui.AlbumLabelMaker;
import com.android.gallery3d.ui.AlbumSetSlotRenderer.LabelSpec;
import com.android.gallery3d.ui.AlbumSlidingWindow;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SlotView;
import com.android.gallery3d.util.MediaSetUtils;
import com.android.gallery3d.util.ThreadPool;

import com.mediatek.galleryfeature.config.FeatureConfig;
import com.mediatek.galleryframework.util.MtkLog;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * StereoSourceSlotRenderer is responsible to render each slot in StereoPickingAlbumPage.
 */
class StereoSourceSlotRenderer extends AbstractSlotRenderer {
    private static final String TAG = "MtkGallery2/StereoSourceSlotRenderer";
    private static final int CACHE_SIZE_ON_LOW_RAM_DEVICE = 32;
    private static final int CACHE_SIZE_ON_NORMAL_DEVICE = 96;
    private static final int CACHE_SIZE =
            FeatureConfig.sIsLowRamDevice ?
                    CACHE_SIZE_ON_LOW_RAM_DEVICE : CACHE_SIZE_ON_NORMAL_DEVICE;

    private final int mPlaceholderColor;

    private AlbumSlidingWindow mDataWindow;
    private final AbstractGalleryActivity mActivity;
    private final ColorTexture mWaitLoadingTexture;
    private final SlotView mSlotView;
    private final SelectionManager mSelectionManager;

    private int mPressedIndex = -1;
    private boolean mAnimatePressedUp;
    private Path mHighlightItemPath = null;
    private boolean mInSelectionMode;

    private AlbumLabelMaker mLabelMaker;
    private BitmapTexture mLoadingLabel;
    private LabelSpec mLabelSpec;

    /**
     * Cache for lables of the first slot.<br/>
     * Mainly cache lables of 2 dimensions. One is for landscape diplay, and the other
     * if for portrait diaplay.
     */
    private static class ClippingsOverlayCache {
        private static Map<Integer, BitmapTexture> sMap = new HashMap<Integer, BitmapTexture>();

        public static Texture getTexture(int width) {
            return sMap.get(width);
        }

        public static void putTexture(int width, BitmapTexture texture) {
            MtkLog.d(TAG, "<ClippingsOverlayCache.putTexture> " + width);
            sMap.put(width, texture);
        }

        public static void recycle() {
            Iterator<Map.Entry<Integer, BitmapTexture>> iter = sMap.entrySet().iterator();
            Map.Entry<Integer, BitmapTexture> entry;
            BitmapTexture texture;
            while (iter.hasNext()) {
                entry = iter.next();
                MtkLog.d(TAG, "<ClippingsOverlayCache.recycle> " + entry.getKey());
                texture = entry.getValue();
                texture.recycle();
            }
            sMap.clear();
        }
    }

    public StereoSourceSlotRenderer(AbstractGalleryActivity activity, SlotView slotView,
            SelectionManager selectionManager, int placeholderColor) {
        super(activity);
        mActivity = activity;
        mSlotView = slotView;
        mSelectionManager = selectionManager;
        mPlaceholderColor = placeholderColor;

        mWaitLoadingTexture = new ColorTexture(mPlaceholderColor);
        mWaitLoadingTexture.setSize(1, 1);
    }

    public void setPressedIndex(int index) {
        if (mPressedIndex == index) {
            return;
        }
        mPressedIndex = index;
        mSlotView.invalidate();
    }

    public void setPressedUp() {
        if (mPressedIndex == -1) {
            return;
        }
        mAnimatePressedUp = true;
        mSlotView.invalidate();
    }

    public void setHighlightItemPath(Path path) {
        if (mHighlightItemPath == path) {
            return;
        }
        mHighlightItemPath = path;
        mSlotView.invalidate();
    }

    public void setModel(AlbumDataLoader model) {
        if (mDataWindow != null) {
            mDataWindow.setListener(null);
            mSlotView.setSlotCount(0);
            mDataWindow = null;
        }
        if (model != null) {
            mDataWindow = new AlbumSlidingWindow(mActivity, model, CACHE_SIZE);
            mDataWindow.setListener(new MyDataModelListener());
            mSlotView.setSlotCount(model.size());
        }
    }

    @Override
    public int renderSlot(GLCanvas canvas, int index, int pass, int width, int height) {
        AlbumSlidingWindow.AlbumEntry entry = mDataWindow.get(index);

        int renderRequestFlags = 0;

        Texture content = checkTexture(entry.content);
        if (content == null) {
            content = mWaitLoadingTexture;
            entry.isWaitDisplayed = true;
        } else if (entry.isWaitDisplayed) {
            entry.isWaitDisplayed = false;
            content = entry.bitmapTexture;
            entry.content = content;
        }
        drawContent(canvas, content, width, height, entry.rotation);
        if ((content instanceof FadeInTexture) &&
                ((FadeInTexture) content).isAnimating()) {
            renderRequestFlags |= SlotView.RENDER_MORE_FRAME;
        }

        if (entry.mediaType == MediaObject.MEDIA_TYPE_VIDEO
                && entry.item.getMediaData() != null
                && !(entry.item.getMediaData().isSlowMotion)) {
            drawVideoOverlay(canvas, width, height);
        }
        if (index == 0) {
            // TODO
            int clippingBucketId = MediaSetUtils.STEREO_CLIPPINGS_BUCKET_ID;
            if ((((LocalMediaItem) (entry.item)) != null)
                    && ((LocalMediaItem) (entry.item)).bucketId == clippingBucketId) {
                drawClippingsOverlay(canvas, width, height);
            }
        }

        renderRequestFlags |= renderOverlay(canvas, index, entry, width, height);

        return renderRequestFlags;
    }

    /**
     * Listener for data model change.
     */
    private class MyDataModelListener implements AlbumSlidingWindow.Listener {
        @Override
        public void onContentChanged() {
            mSlotView.invalidate();
        }

        @Override
        public void onSizeChanged(int size) {
            mSlotView.setSlotCount(size);
            mSlotView.invalidate();
        }
    }

    public void resume() {
        mDataWindow.resume();
    }

    public void pause() {
        mDataWindow.pause();
        ClippingsOverlayCache.recycle();
    }

    @Override
    public void prepareDrawing() {
        mInSelectionMode = mSelectionManager.inSelectionMode();
    }

    @Override
    public void onVisibleRangeChanged(int visibleStart, int visibleEnd) {
        if (mDataWindow != null) {
            mDataWindow.setActiveWindow(visibleStart, visibleEnd);
        }
    }

    @Override
    public void onSlotSizeChanged(int width, int height) {
        // Do nothing
    }

    private static Texture checkTexture(Texture texture) {
        return (texture instanceof TiledTexture)
                && !((TiledTexture) texture).isReady()
                ? null
                : texture;
    }

    private void drawClippingsOverlay(GLCanvas canvas, int width, int height) {
        if (mLabelMaker == null) {
            Config.AlbumSetPage config = Config.AlbumSetPage.get(mActivity);
            mLabelSpec = config.labelSpec;
            mLabelMaker = new AlbumLabelMaker(mActivity.getAndroidContext(), mLabelSpec);
        }

        Texture content = ClippingsOverlayCache.getTexture(width);
        if (content == null) {
            mLabelMaker.setLabelWidth(width);
            Bitmap bitmap = mLabelMaker.requestLabel(mActivity.getString(R.string.m_clippings), "",
                    DataSourceType.TYPE_LOCAL).run(ThreadPool.JOB_CONTEXT_STUB);
            mLoadingLabel = new BitmapTexture(bitmap);
            mLoadingLabel.setOpaque(false);
            content = mLoadingLabel;
            ClippingsOverlayCache.putTexture(width, mLoadingLabel);
        }
        int b = AlbumLabelMaker.getBorderSize();
        int h = mLabelSpec.labelBackgroundHeight;
        content.draw(canvas, -b, height - h + b, width + b + b, h);
    }

    private int renderOverlay(GLCanvas canvas, int index,
            AlbumSlidingWindow.AlbumEntry entry, int width, int height) {
        int renderRequestFlags = 0;
        if (mPressedIndex == index) {
            if (mAnimatePressedUp) {
                drawPressedUpFrame(canvas, width, height);
                renderRequestFlags |= SlotView.RENDER_MORE_FRAME;
                if (isPressedUpFrameFinished()) {
                    mAnimatePressedUp = false;
                    mPressedIndex = -1;
                }
            } else {
                drawPressedFrame(canvas, width, height);
            }
        } else if ((entry.path != null) && (mHighlightItemPath == entry.path)) {
            drawSelectedFrame(canvas, width, height);
        } else if (mInSelectionMode && mSelectionManager.isItemSelected(entry.path)) {
            drawSelectedFrame(canvas, width, height);
        }
        return renderRequestFlags;
    }
}
