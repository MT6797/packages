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

import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore.Images.ImageColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.gallery3d.R;

import com.mediatek.galleryfeature.stereo.segment.ImageSaver;
import com.mediatek.galleryfeature.stereo.segment.ImageShow;
import com.mediatek.galleryfeature.stereo.segment.MainImageMaster;
import com.mediatek.galleryfeature.stereo.segment.SegmentUtils;
import com.mediatek.galleryfeature.stereo.segment.StereoSegmentWrapper;
import com.mediatek.galleryframework.util.MtkLog;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Activity for background substitution.
 */
public class StereoBackgroundActivity extends FragmentActivity {
    // TODO many redundant code in 3 segment Activities
    // I put the in similar places in each Activity for future further work
    private static final String TAG = "MtkGallery2/SegmentApp/StereoBackgroundActivity";

    private static final int ACTIONBAR_BACKGROUND_COLOR = 0x99333333;
    private static final String BACKGROUNDTHUMB_FRAGMENT_TAG = "MainPanel";
    static final int REQUEST_CODE_PICK_FROM_GALLERY = 211;
    private static final int REQUEST_CODE_REFINE = 10;
    private static final int THUMBNAIL_TRACK_ANIMATION_DURATION = 500;
    private static final int SAVE_QUALITY = 100;

    private MainImageMaster mMasterImage;
    private StereoSegmentWrapper mMaskSimulator;
    private LoadBitmapTask mLoadBitmapTask;
    private WeakReference<ProgressDialog> mSavingProgressDialog;
    private ActionBar mActionBar;
    private Menu mMenu;
    private String mAbsoluteFilePath;
    private Uri mSelectedImageUri;

    private View mSaveButton;
    private View mButtonCollapse;
    private View mBackgroundThumbnailList;
    private View mBackgroundStrip;
    private ImageShow mImageShowBackground;
    private BackgroundThumbAdapter mThumbAdapter;
    private MenuItem mMenuRefineOriginal;
    private Uri mOriginalRefineImage; // == mSelectedUri
    private ForegroundSegmenterParameter mForegroundSegmenterParameter;
    private Bitmap mBitmapCollapse;
    private Bitmap mBitmapExpand;

    private boolean mLoadingVisible = true;
    private boolean mIsDepthImage; // useless
    // exit if file to edit has been deleted
    private boolean mNeedCheckInvalid = true;

    private boolean mIsBottomStripIn = true;

    /**
     * Async task to load main bitmap for ImageShow.
     */
    private class LoadBitmapTask extends AsyncTask<Uri, Boolean, Boolean> {
        private int mBitmapSize;

        public LoadBitmapTask() {
            DisplayMetrics outMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
            mBitmapSize = Math.max(outMetrics.heightPixels, outMetrics.widthPixels);
        }

        @Override
        protected Boolean doInBackground(Uri... params) {
            if (!mMasterImage.loadBitmap(params[0], mBitmapSize)) {
                return false;
            }
            mBitmapCollapse = BitmapFactory.decodeResource(getResources(),
                    R.drawable.m_hide_bgthumbs);
            Matrix matrix = new Matrix();
            matrix.postScale(1, -1);
            mBitmapExpand = Bitmap.createBitmap(mBitmapCollapse, 0, 0, mBitmapCollapse.getWidth(),
                    mBitmapCollapse.getHeight(), matrix, true);
            return true;
        }

        @Override
        protected void onProgressUpdate(Boolean... values) {
            super.onProgressUpdate(values);
            if (isCancelled()) {
                return;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (mLoadingVisible) {
                stopLoadingIndicator();
            }
            if (isCancelled()) {
                return;
            }

            if (!result) {
                cannotLoadImage();
                return;
            }

            StereoSegmentWrapper segmenter = getSegmentWrapper();
            if (segmenter != null) {
                mImageShowBackground.setSegmenter(segmenter);
                mImageShowBackground.setVisibility(View.VISIBLE);
                onImageChange();
            }

            mLoadBitmapTask = null;

            super.onPostExecute(result);
        }
    }

    /**
     * Helper data structure for quick segment restore.
     */
    private class ForegroundSegmenterParameter {
        final int mOriginalWidth;
        final int mOriginalHeight;
        final String mFilePath;
        final Bitmap mBitmap;

        public ForegroundSegmenterParameter(int originalWidth, int originalHeight, String filePath,
                Bitmap bitmap) {
            mOriginalWidth = originalWidth;
            mOriginalHeight = originalHeight;
            mFilePath = filePath;
            mBitmap = bitmap;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        // exit if file to edit has been deleted @{
        // it mainly aims to handle language switch or LCA case here
        boolean needExit = finishIfInvalidUri();
        mNeedCheckInvalid = false;
        if (needExit) {
            return;
        }

        // avoid flash when launching
        getWindow().setBackgroundDrawable(new ColorDrawable(0));

        loadXML();

        mMasterImage = new MainImageMaster(this);
        mImageShowBackground.setImageMaster(mMasterImage);

        loadMainBitmapAsync();

        loadThumbnails();

        loadMainPanel();
    }

    @Override
    protected void onDestroy() {
        hideSavingProgress();

        if (mLoadBitmapTask != null) {
            mLoadBitmapTask.cancel(false);
        }

        if (mMaskSimulator != null) {
            mMaskSimulator.release();
            mMaskSimulator = null;
        }

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.m_stereo_refine_activity_menu, menu);
        mMenu = menu;
        setupMenu();
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        finishIfInvalidUri();
        mNeedCheckInvalid = true;

        Fragment panel = getSupportFragmentManager()
                .findFragmentByTag(BACKGROUNDTHUMB_FRAGMENT_TAG);
        if (panel != null) {
            if (panel instanceof MainPanel) {
                MainPanel mainPanel = (MainPanel) panel;
                mainPanel.loadBackgroundThumbPanel(true);
                MtkLog.d(TAG, "<onResume> loadCategoryLookPanel");
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        default:
            return false;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO do we really need to response onConfigurationChange?
        mImageShowBackground.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
        mImageShowBackground.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        MtkLog.d(TAG, "<onActivityResult>, requestCode:" + requestCode);
        if (requestCode == REQUEST_CODE_PICK_FROM_GALLERY) {
            if (resultCode == RESULT_OK) {
                Uri selectedImageUri = data.getData();
                changeBitmap(selectedImageUri);
                updateRecentBackground(selectedImageUri);
            }
        } else if (requestCode == REQUEST_CODE_REFINE) {
            mMaskSimulator = new StereoSegmentWrapper(mForegroundSegmenterParameter.mOriginalWidth,
                    mForegroundSegmenterParameter.mOriginalHeight,
                    mForegroundSegmenterParameter.mBitmap.getWidth(),
                    mForegroundSegmenterParameter.mBitmap.getHeight());
            boolean isSegmentValid = mMaskSimulator.initSegment(
                    mForegroundSegmenterParameter.mFilePath, mForegroundSegmenterParameter.mBitmap);
            if (!isSegmentValid) {
                mImageShowBackground.setSegmenter(null);
                errorHandle();
                return;
            }
            mImageShowBackground.setSegmenter(mMaskSimulator);
            mImageShowBackground.invalidate();
        }
    }

    void changeBitmap(Uri uri) {
        if (uri == null) {
            uri = mOriginalRefineImage;
            if (uri == null) {
                return;
            }
        }
        if (mOriginalRefineImage == null) {
            mOriginalRefineImage = mSelectedImageUri;
            MtkLog.d(TAG, "<changeBitmap> mOriginalRefineImage = " + mOriginalRefineImage);
        }
        if (uri.equals(mOriginalRefineImage)) {
            mSaveButton.setVisibility(View.INVISIBLE);
            mImageShowBackground.setZoomPanSupported(false);
        }
        mLoadBitmapTask = new LoadBitmapTask();
        mLoadBitmapTask.execute(uri);
    }

    BackgroundThumbAdapter getBackgroundThumbAdapter() {
        return mThumbAdapter;
    }

    private void onImageChange() {
        boolean isCurrentOriginal = ((mOriginalRefineImage == null) || (mMasterImage.getUri()
                .equals(mOriginalRefineImage)));
        if (mImageShowBackground != null) {
            mMasterImage.setTranslation(new Point(0, 0));
            mMasterImage.setScaleFactor(1);
            mImageShowBackground.invalidate();
            mImageShowBackground.setZoomPanSupported(!isCurrentOriginal);
        }
        if (mSaveButton != null) {
            if (isCurrentOriginal) {
                mSaveButton.setVisibility(View.INVISIBLE);
            } else {
                mSaveButton.setVisibility(View.VISIBLE);
            }
        }
    }

    private StereoSegmentWrapper getSegmentWrapper() {
        if (mIsDepthImage && mMaskSimulator == null) {
            mMaskSimulator = new StereoSegmentWrapper(mMasterImage.getOriginalBounds().width(),
                    mMasterImage.getOriginalBounds().height(), mMasterImage.getBitmap().getWidth(),
                    mMasterImage.getBitmap().getHeight());
            boolean isSegmentValid = mMaskSimulator.initSegment(mAbsoluteFilePath,
                    mMasterImage.getBitmap());
            if (!isSegmentValid) {
                errorHandle();
                return null;
            }
            mForegroundSegmenterParameter = new ForegroundSegmenterParameter(mMasterImage
                    .getOriginalBounds().width(), mMasterImage.getOriginalBounds().height(),
                    mAbsoluteFilePath, mMasterImage.getBitmap());
        }
        return mMaskSimulator;
    }

    private boolean finishIfInvalidUri() {
        if (!mNeedCheckInvalid) {
            return false;
        }
        Uri uri = (mMasterImage != null) ? mMasterImage.getUri() : null;
        Intent intent = getIntent();
        if ((uri == null) && (intent != null)) {
            uri = intent.getData();
        }
        MtkLog.d(TAG, "<finishIfInvalidUri> uri:" + uri);
        if (uri != null) {
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                return false;
            }
            // fix cursor leak
            final String[] projection = new String[] { ImageColumns.DATA,
                    ImageColumns.TITLE };
            android.database.Cursor cursor = null;
            cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor == null) {
                MtkLog.d(TAG, "<finishIfInvalidUri> cannot get cursor for:" + uri);
                return true;
            }
            try {
                if (cursor.moveToNext()) {
                    mAbsoluteFilePath = cursor.getString(0);
                    MtkLog.d(TAG, "<finishIfInvalidUri> mAbsoluteFilePath = " + mAbsoluteFilePath);
                    mIsDepthImage = (SegmentUtils.isDepthImageTitlePattern(cursor.getString(1)));
                    MtkLog.d(TAG, "<finishIfInvalidUri> mIsDepthImage = " + mIsDepthImage);
                } else {
                    MtkLog.d(TAG, "<finishIfInvalidUri> cannot find data for: " + uri);
                    return true;
                }
            } finally {
                cursor.close();
            }
        }
        return false;
    }

    private void showSavingProgress(String albumName) {
        ProgressDialog progress;
        if (mSavingProgressDialog != null) {
            progress = mSavingProgressDialog.get();
            if (progress != null) {
                progress.show();
                return;
            }
        }
        // TODO: Allow cancellation of the saving process
        String progressText;
        if (albumName == null) {
            progressText = getString(R.string.saving_image);
        } else {
            progressText = getString(R.string.filtershow_saving_image, albumName);
        }
        progress = ProgressDialog.show(this, "", progressText, true, false);
        mSavingProgressDialog = new WeakReference<ProgressDialog>(progress);
    }

    private void hideSavingProgress() {
        if (mSavingProgressDialog != null) {
            ProgressDialog progress = mSavingProgressDialog.get();
            if (progress != null) {
                progress.dismiss();
            }
        }
    }

    private void startLoadingIndicator() {
        final View loading = findViewById(R.id.loading);
        mLoadingVisible = true;
        loading.setVisibility(View.VISIBLE);
    }

    private void stopLoadingIndicator() {
        final View loading = findViewById(R.id.loading);
        loading.setVisibility(View.GONE);
        mLoadingVisible = false;
    }

    private void startLoadBitmap(Uri uri) {
        mImageShowBackground.setVisibility(View.INVISIBLE);
        startLoadingIndicator();
        mLoadBitmapTask = new LoadBitmapTask();
        mLoadBitmapTask.execute(uri);
    }

    private void cannotLoadImage() {
        Toast.makeText(this, R.string.cannot_load_image, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void loadMainBitmapAsync() {
        Intent intent = getIntent();
        mSelectedImageUri = intent.getData();
        if (mSelectedImageUri != null) {
            startLoadBitmap(mSelectedImageUri);
        }
    }

    private void setActionBarDisplayOptions(ActionBar actionBar, boolean displayHomeAsUp,
            boolean showTitle) {
        if (actionBar == null) {
            return;
        }
        int options = 0;
        if (displayHomeAsUp) {
            options |= ActionBar.DISPLAY_HOME_AS_UP;
        }
        if (showTitle) {
            options |= ActionBar.DISPLAY_SHOW_TITLE;
        }

        actionBar.setDisplayOptions(options, ActionBar.DISPLAY_HOME_AS_UP
                | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setHomeButtonEnabled(displayHomeAsUp);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setTitle(R.string.m_save);
    }

    private void loadXML() {
        setContentView(R.layout.m_stereo_background_activity);

        mActionBar = getActionBar();
        mActionBar.setBackgroundDrawable(new ColorDrawable(ACTIONBAR_BACKGROUND_COLOR));
        mActionBar.setSubtitle(null);
        mActionBar.setCustomView(R.layout.m_stereo_actionbar);
        mSaveButton = mActionBar.getCustomView();
        View clickableRegion = mSaveButton.findViewById(R.id.stereo_segment_save);
        clickableRegion.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                saveSynthedImage();
            }
        });

        setActionBarDisplayOptions(mActionBar, true, true);

        mImageShowBackground = (ImageShow) findViewById(R.id.imageShowBackground);
        mButtonCollapse = findViewById(R.id.collapse);
        mBackgroundThumbnailList = findViewById(R.id.main_panel_container);
        mBackgroundStrip = findViewById(R.id.mainPanel);

        mButtonCollapse.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsBottomStripIn) {
                    ((ImageView) (mButtonCollapse)).setImageBitmap(mBitmapExpand);
                    ObjectAnimator animator = ObjectAnimator.ofFloat(mBackgroundStrip,
                            "translationY", mBackgroundThumbnailList.getHeight());
                    animator.setDuration(THUMBNAIL_TRACK_ANIMATION_DURATION).start();
                    mIsBottomStripIn = false;
                } else {
                    ((ImageView) (mButtonCollapse)).setImageBitmap(mBitmapCollapse);
                    ObjectAnimator animator = ObjectAnimator.ofFloat(mBackgroundStrip,
                            "translationY", 0);
                    animator.setDuration(THUMBNAIL_TRACK_ANIMATION_DURATION).start();
                    mIsBottomStripIn = true;
                }
            }
        });
    }

    private void setupMenu() {
        if (mMenu == null || mMasterImage == null) {
            return;
        }

        mMenuRefineOriginal = mMenu.findItem(R.id.refine_refine_foreground);
        mMenuRefineOriginal.setVisible(true);

        mMenuRefineOriginal.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent("action_stereo_pick");
                intent.setDataAndType(getIntent().getData(), getIntent().getType()).setFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (mMaskSimulator != null) {
                    mMaskSimulator.release();
                    mMaskSimulator = null;
                }

                startActivityForResult(intent, REQUEST_CODE_REFINE);

                return false;
            }
        });
    }

    private void errorHandle() {
        Toast.makeText(this, getString(R.string.m_general_err_tip), Toast.LENGTH_LONG).show();
        finish();
    }

    private void saveSynthedImage() {
        new Thread(new Runnable() {
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        showSavingProgress(/* "..." */null);
                    }
                });

                Bitmap toSaveBitmap = ((ImageShowBackground) (mImageShowBackground))
                        .getSynthBitmap();

                File target = ImageSaver.getNewFile(StereoBackgroundActivity.this,
                        mSelectedImageUri);
                ImageSaver.saveBitmapToFile(target, toSaveBitmap, Bitmap.CompressFormat.JPEG);
                long time = System.currentTimeMillis();
                final Uri dstUri = ImageSaver.linkNewFileToUri(StereoBackgroundActivity.this,
                        mSelectedImageUri, target, toSaveBitmap, time);
                toSaveBitmap.recycle();
                runOnUiThread(new Runnable() {
                    public void run() {
                        hideSavingProgress();
                        MtkLog.d(TAG, "<saveSynthedImage> uri:" + dstUri);
                        setResult(RESULT_OK, new Intent().setData(dstUri));
                        StereoBackgroundActivity.this.finish();
                    }
                });
            }
        }).start();
    }

    private void loadMainPanel() {
        if (findViewById(R.id.main_panel_container) == null) {
            return;
        }
        MainPanel panel = new MainPanel();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_panel_container, panel, BACKGROUNDTHUMB_FRAGMENT_TAG);
        transaction.commitAllowingStateLoss();
    }

    private void loadThumbnails() {
        MtkLog.d(TAG, "<loadThumbnails>");
        if (mThumbAdapter != null) {
            mThumbAdapter.clear();
        }
        mThumbAdapter = new BackgroundThumbAdapter(this);
        int verticalItemHeight = (int) getResources().getDimension(R.dimen.action_item_height);
        mThumbAdapter.loadActions(this, verticalItemHeight);
    }

    private void updateRecentBackground(Uri bgUri) {
        BackgroundThumbAction action = new BackgroundThumbAction(this,
                BackgroundThumbAction.FULL_VIEW);
        action.setOriginal(false);
        action.setUri(bgUri.toString());
        mThumbAdapter.addFront(action);
        mThumbAdapter.saveActions();
    }
}
