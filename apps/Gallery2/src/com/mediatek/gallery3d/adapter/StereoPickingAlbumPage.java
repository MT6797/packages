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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.app.ActivityState;
import com.android.gallery3d.app.AlbumDataLoader;
import com.android.gallery3d.app.AlbumPage;
import com.android.gallery3d.app.Config;
import com.android.gallery3d.app.GalleryActionBar;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.app.LoadingListener;
import com.android.gallery3d.app.Log;
import com.android.gallery3d.app.PhotoPage;
import com.android.gallery3d.app.SinglePhotoPage;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.LocalAlbum;
import com.android.gallery3d.data.LocalImage;
import com.android.gallery3d.data.LocalMediaItem;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.FadeTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.ui.DetailsHelper;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.PhotoFallbackEffect;
import com.android.gallery3d.ui.RelativePosition;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SlotView;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.MediaSetUtils;

import com.mediatek.galleryframework.util.MtkLog;

import java.util.ArrayList;

/**
 * Page(ActivityState) for selecting stereo image to copy in Copy & Paste feature.<br/>
 * The first slot on this Page is the entrance to StereoClippingAlbumPage if there's any
 * segmented clipping on the device.
 */
public class StereoPickingAlbumPage extends ActivityState {
    private static final String TAG = "MtkGallery2/StereoPickingAlbumPage";

    public static final String KEY_MEDIA_PATH = "media-path";
    public static final String KEY_PARENT_MEDIA_PATH = "parent-media-path";
    public static final String KEY_SET_CENTER = "set-center";
    public static final String KEY_EMPTY_ALBUM = "empty-album";
    public static final String KEY_RESUME_ANIMATION = "resume_animation";
    private static final float USER_DISTANCE_METER = 0.3f;
    private static final int REQUEST_STEREO_PICK = 10;
    private static final int MSG_PICK_PHOTO = 0;
    private static final int BIT_LOADING_RELOAD = 1;
    private static final int REQUEST_CHOOSE_SEGMENT = 15;

    private boolean mIsActive = false;
    private int mLoadingBits = 0;
    private float mUserDistance; // in pixel
    private StereoSourceSlotRenderer mAlbumView;
    private Path mMediaSetPath;
    private SlotView mSlotView;
    private AlbumDataLoader mAlbumDataAdapter;
    private SelectionManager mSelectionManager;
    private MediaSet mMediaSet;
    private RelativePosition mOpenCenter = new RelativePosition();
    private Handler mHandler;
    private PhotoFallbackEffect mResumeEffect;
    private boolean mExitPage = false;

    private PhotoFallbackEffect.PositionProvider mPositionProvider
                = new PhotoFallbackEffect.PositionProvider() {
        @Override
        public Rect getPosition(int index) {
            Rect rect = mSlotView.getSlotRect(index);
            Rect bounds = mSlotView.bounds();
            rect.offset(bounds.left - mSlotView.getScrollX(), bounds.top - mSlotView.getScrollY());
            return rect;
        }

        @Override
        public int getItemIndex(Path path) {
            int start = mSlotView.getVisibleStart();
            int end = mSlotView.getVisibleEnd();
            for (int i = start; i < end; ++i) {
                MediaItem item = mAlbumDataAdapter.get(i);
                if (item != null && item.getPath() == path) {
                    return i;
                }
            }
            return -1;
        }
    };

    @Override
    protected int getBackgroundColorId() {
        return R.color.album_background;
    }

    private final GLView mRootPane = new GLView() {
        private static final int MATRIX_SIZE = 16;
        private final float mMatrix[] = new float[MATRIX_SIZE];

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

            int slotViewTop = mActivity.getGalleryActionBar().getHeight();
            int slotViewBottom = bottom - top;
            int slotViewRight = right - left;

            mAlbumView.setHighlightItemPath(null);

            // Set the mSlotView as a reference point to the open animation
            mOpenCenter.setReferencePosition(0, slotViewTop);
            mSlotView.layout(0, slotViewTop, slotViewRight, slotViewBottom);
            GalleryUtils.setViewPointMatrix(mMatrix, (right - left) / 2, (bottom - top) / 2,
                    -mUserDistance);
        }

        @Override
        protected void render(GLCanvas canvas) {
            canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
            canvas.multiplyMatrix(mMatrix, 0);
            super.render(canvas);

            if (mResumeEffect != null) {
                boolean more = mResumeEffect.draw(canvas);
                if (!more) {
                    mResumeEffect = null;
                }
                // We want to render one more time even when no more effect
                // required. So that the animated thumbnails could be draw
                // with declarations in super.render().
                invalidate();
            }
            canvas.restore();
        }
    };

    @Override
    protected void onBackPressed() {
        onUpPressed();
    }

    private void onUpPressed() {
        if (mActivity.getStateManager().getStateCount() > 1) {
            super.onBackPressed();
        } else {
            Bundle data = new Bundle(getData());

            // item path
            // get first item of this continuous shot group
            ArrayList<MediaItem> itemArray = mMediaSet.getMediaItem(0, 1);
            if (itemArray == null || itemArray.size() == 0) {
                return;
            }
            MediaItem firstItemThisGroup = itemArray.get(0);
            if (firstItemThisGroup == null || firstItemThisGroup.getMediaData() == null) {
                return;
            }
            long id = firstItemThisGroup.getMediaData().id;
            // get MediaItem of this group in PhotoPage
            ArrayList<Integer> ids = new ArrayList<Integer>();
            ids.add(Integer.valueOf((int) id));
            MediaItem[] items = LocalAlbum.getMediaItemById(
                    (GalleryApp) mActivity.getApplication(), true, ids);
            if (items == null || items.length == 0) {
                return;
            }
            data.putString(PhotoPage.KEY_MEDIA_ITEM_PATH, items[0].getPath().toString());

            // set path
            DataManager dm = mActivity.getDataManager();
            Path setPath = Path.fromString(dm.getTopSetPath(DataManager.INCLUDE_LOCAL_ALL_ONLY))
                    .getChild(((LocalImage) items[0]).getBucketId());
            data.putString(PhotoPage.KEY_MEDIA_SET_PATH, setPath.toString());

            MtkLog.i(TAG, "<onUpPressed> setPath = " + setPath + ", itemPath = "
                    + items[0].getPath());
            mActivity.getStateManager().switchState(this, SinglePhotoPage.class, data);
        }
    }

    private void onDown(int index) {
        mAlbumView.setPressedIndex(index);
    }

    private void onUp(boolean followedByLongPress) {
        if (followedByLongPress) {
            // Avoid showing press-up animations for long-press.
            mAlbumView.setPressedIndex(-1);
        } else {
            mAlbumView.setPressedUp();
        }
    }

    private void onSingleTapUp(int slotIndex) {
        if (!mIsActive) {
            return;
        }

            // Render transition in pressed state
        mAlbumView.setPressedIndex(slotIndex);
        mAlbumView.setPressedUp();
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_PICK_PHOTO, slotIndex, 0),
                FadeTexture.DURATION);
    }

    private void pickPhoto(int slotIndex) {
        if (!mIsActive) {
            MtkLog.i(TAG, "<pickPhoto> Not active, ignore the click");
            return;
        }

        MediaItem item = mAlbumDataAdapter.get(slotIndex);
        if (item == null) {
            MtkLog.i(TAG, "<pickPhoto> Item not ready yet, ignore the click");
            return;
        }
        int clippingBucketId = MediaSetUtils.STEREO_CLIPPINGS_BUCKET_ID;
        if ((slotIndex == 0) && (((LocalMediaItem) (item)).bucketId == clippingBucketId)) {
            Bundle data = new Bundle(getData());
            data.putString(AlbumPage.KEY_MEDIA_PATH, "/local/all/" + clippingBucketId);
            mActivity.getStateManager().startStateForResult(StereoClippingAlbumPage.class,
                    REQUEST_CHOOSE_SEGMENT, data);
        } else {
            Intent intent = new Intent("action_stereo_pick"/* "action_nextgen_edit" */);
            intent.setDataAndType(item.getContentUri(), item.getMimeType()).setFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String destUriString = getData().getString("COPY_DEST");
            Log.d("stst", "destUriString = " + destUriString);
            intent.putExtra("COPY_DEST_URI", destUriString);
            ((Activity) mActivity).startActivityForResult(intent, REQUEST_STEREO_PICK);
        }
    }

    @Override
    protected void onCreate(Bundle data, Bundle restoreState) {
        MtkLog.i(TAG, "<onCreate>");
        super.onCreate(data, restoreState);
        mUserDistance = GalleryUtils.meterToPixel(USER_DISTANCE_METER);
        initializeViews();
        initializeData(data);

        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                case MSG_PICK_PHOTO: {
                    pickPhoto(message.arg1);
                    break;
                }
                default:
                    throw new AssertionError(message.what);
                }
            }
        };
    }

    @Override
    protected void onResume() {
        MtkLog.i(TAG, "<onResume>");
        super.onResume();
        if (mExitPage) {
            return;
        }

        mIsActive = true;

        mResumeEffect = mActivity.getTransitionStore().get(KEY_RESUME_ANIMATION);
        if (mResumeEffect != null) {
            // mAlbumView.setSlotFilter(mResumeEffect);
            mResumeEffect.setPositionProvider(mPositionProvider);
            mResumeEffect.start();
        }

        setContentPane(mRootPane);

        boolean enableHomeButton = (mActivity.getStateManager().getStateCount() > 1);
        mActivity.getGalleryActionBar().setDisplayOptions(enableHomeButton, true);

        // Set the reload bit here to prevent it exit this page in
        // clearLoadingBit().
        setLoadingBit(BIT_LOADING_RELOAD);

        mAlbumDataAdapter.resume();

        mAlbumView.resume();
        mAlbumView.setPressedIndex(-1);
    }

    @Override
    protected void onPause() {
        MtkLog.i(TAG, "<onPause>");
        super.onPause();

        if (!mIsActive) {
            return;
        }

        mIsActive = false;

        mAlbumDataAdapter.pause();
        mAlbumView.pause();
        DetailsHelper.pause();
    }

    @Override
    protected void onDestroy() {
        MtkLog.i(TAG, "<onDestroy>");
        super.onDestroy();
        if (mAlbumDataAdapter != null) {
            mAlbumDataAdapter.setLoadingListener(null);
        }
    }

    private void initializeViews() {
        mSelectionManager = new SelectionManager(mActivity, false);
        Config.AlbumPage config = Config.AlbumPage.get(mActivity);
        mSlotView = new SlotView(mActivity, config.slotViewSpec);
        mAlbumView = new StereoSourceSlotRenderer(mActivity, mSlotView, mSelectionManager,
                config.placeholderColor);
        mSlotView.setSlotRenderer(mAlbumView);
        mRootPane.addComponent(mSlotView);
        mSlotView.setListener(new SlotView.SimpleListener() {
            @Override
            public void onDown(int index) {
                StereoPickingAlbumPage.this.onDown(index);
            }

            @Override
            public void onUp(boolean followedByLongPress) {
                StereoPickingAlbumPage.this.onUp(followedByLongPress);
            }

            @Override
            public void onSingleTapUp(int slotIndex) {
                StereoPickingAlbumPage.this.onSingleTapUp(slotIndex);
            }
        });
    }

    private void initializeData(Bundle data) {
        mMediaSetPath = Path.fromString(StereoPickingSource.STEREO_PICKING_SET);
        mMediaSet = mActivity.getDataManager().getMediaSet(mMediaSetPath);
        if (mMediaSet == null) {
            MtkLog.e(TAG, "MediaSet is null. Path = %s" + mMediaSetPath);
            return;
        }
        mSelectionManager.setSourceMediaSet(mMediaSet);
        mAlbumDataAdapter = new AlbumDataLoader(mActivity, mMediaSet);
        mAlbumDataAdapter.setLoadingListener(new MyLoadingListener());
        mAlbumView.setModel(mAlbumDataAdapter);
    }

    @Override
    protected boolean onCreateActionBar(Menu menu) {
        GalleryActionBar actionBar = mActivity.getGalleryActionBar();
        actionBar.setTitle(R.string.m_pick_photo_to_copy);
        actionBar.setSubtitle(null);
        return true;
    }

    @Override
    protected boolean onItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home: {
            onUpPressed();
            return true;
        }
        case R.id.action_cancel:
            mActivity.getStateManager().finishState(this);
            return true;
        default:
            return false;
        }
    }

    @Override
    protected void onStateResult(int request, int result, Intent data) {
        Log.d(TAG, "<onStateResult> data:" + data);

        if ((request != REQUEST_CHOOSE_SEGMENT) && (request != REQUEST_STEREO_PICK)) {
            return;
        }

        if (result == Activity.RESULT_OK) {
            if (data == null) {
                setStateResult(Activity.RESULT_CANCELED, null);
            } else {
                setStateResult(Activity.RESULT_OK, data);
            }
        }

        if (((request == REQUEST_CHOOSE_SEGMENT) && (result == Activity.RESULT_OK))
                || (request == REQUEST_STEREO_PICK)) {
            super.onBackPressed();
            mExitPage = true;
        }
    }

    private void setLoadingBit(int loadTaskBit) {
        mLoadingBits |= loadTaskBit;
    }

    private void clearLoadingBit(int loadTaskBit) {
        mLoadingBits &= ~loadTaskBit;
        if (mLoadingBits == 0 && mIsActive) {
            if (mAlbumDataAdapter.size() == 0) {
                Intent result = new Intent();
                result.putExtra(KEY_EMPTY_ALBUM, true);
                setStateResult(Activity.RESULT_CANCELED, result);
                mActivity.getStateManager().finishState(this);
                Toast.makeText(((Activity) mActivity), R.string.m_no_stereo_image,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Listner for image data loading.
     */
    private class MyLoadingListener implements LoadingListener {
        @Override
        public void onLoadingStarted() {
            setLoadingBit(BIT_LOADING_RELOAD);
        }

        @Override
        public void onLoadingFinished(boolean loadingFailed) {
            clearLoadingBit(BIT_LOADING_RELOAD);
        }
    }
}