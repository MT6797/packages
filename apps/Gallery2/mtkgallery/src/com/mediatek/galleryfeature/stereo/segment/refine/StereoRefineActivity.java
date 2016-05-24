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

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.android.gallery3d.R;

import com.mediatek.galleryfeature.stereo.segment.ImageLoader;
import com.mediatek.galleryfeature.stereo.segment.ImageSaver;
import com.mediatek.galleryfeature.stereo.segment.ImageShow;
import com.mediatek.galleryfeature.stereo.segment.MainImageMaster;
import com.mediatek.galleryfeature.stereo.segment.SegmentUtils;
import com.mediatek.galleryfeature.stereo.segment.StereoSegmentWrapper;
import com.mediatek.galleryframework.base.Generator;
import com.mediatek.galleryframework.util.MtkLog;
import com.mediatek.storage.StorageManagerEx;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Activity for segment/refine operations.
 */
public class StereoRefineActivity extends Activity {
    // TODO many redundant code in 3 segment Activities
    // I put the in similar places in each Activity for future further work
    private static final String TAG = "MtkGallery2/SegmentApp/StereoRefineActivity";
    private static final String STEREO_CLIPPINGS_FOLDER_NAME  = "Pictures/Clippings";
    private static final String TIME_STAMP_NAME = "_yyyyMMdd_HHmmss_SSS";
    private static final String PREFIX_CLIPPING = "clip";
    private static final String POSTFIX_CLIPPING = ".png";

    public static final String ACTION_STEREO_PICK = "action_stereo_pick";
    public static final String ACTION_STEREO_SYNTH = "action_stereo_synth";
    private static final int REQUEST_MERGE_IMAGE = 12;
    private static final int UI_MODE_NONE = -1;
    private static final int UI_MODE_PICK = 1;
    private static final int UI_MODE_ENTER_REFINE = 2;
    private static final int UI_MODE_REFINE_ZOOMPAN = 3;
    private static final int UI_MODE_REFINE_ADD = 4;
    private static final int UI_MODE_REFINE_DEL = 5;
    private static final int ACTIONBAR_BACKGROUND_COLOR = 0x99333333;

    private MainImageMaster mMasterImage;
    private StereoSegmentWrapper mMaskSimulator;
    private LoadBitmapTask mLoadBitmapTask;
    private WeakReference<ProgressDialog> mSavingProgressDialog;
    private ActionBar mActionBar;
    private Menu mMenu;
    private String mAbsoluteFilePath;
    private Uri mSelectedImageUri;

    private ImageShow mCurrentImageShow;
    private ImageShow mImageShowPick;
    private ImageShow mImageShowZoomPan;
    private ImageShow mImageShowRefine;
    private View mButtonMore;
    private View mButtonSaveClipping;
    private View mButtonCancel;
    private View mButtonRefine;
    private View mButtonUndo;
    private View mButtonDone;
    private MenuItem mMenuZoomPan;
    private MenuItem mMenuAdd;
    private MenuItem mMenuDel;

    private boolean mLoadingVisible = true;
    private boolean mIsDepthImage; // useless
    // exit if file to edit has been deleted
    private boolean mNeedCheckInvalid = true;

    private int mUIMode = UI_MODE_NONE;

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
                mImageShowPick.setSegmenter(segmenter);
                mImageShowZoomPan.setSegmenter(segmenter);
                mImageShowRefine.setSegmenter(segmenter);
                mImageShowPick.setVisibility(View.VISIBLE);
                onImageChange();
            }

            mLoadBitmapTask = null;

            super.onPostExecute(result);
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
        mImageShowPick.setImageMaster(mMasterImage);
        mImageShowZoomPan.setImageMaster(mMasterImage);
        mImageShowRefine.setImageMaster(mMasterImage);

        loadMainBitmapAsync();
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
    public void onBackPressed() {
        if (mMaskSimulator != null) {
            mMaskSimulator.release();
            mMaskSimulator = null;
        }

        super.onBackPressed();
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

        updateUIByUIMode();
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
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        MtkLog.d(TAG, "<onActivityResult>,requestCode:" + requestCode);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_MERGE_IMAGE) {
                MtkLog.d(TAG, "<onActivityResult> data:" + data);
                setResult(RESULT_OK, data);
                finish();
            }
        } else {
            finish();
        }
    }

    private void onImageChange() {
        if (mCurrentImageShow != null) {
            mCurrentImageShow.invalidate();
        }
    }

    private StereoSegmentWrapper getSegmentWrapper() {
        if (mIsDepthImage && mMaskSimulator == null) {
            mMaskSimulator = new StereoSegmentWrapper(mMasterImage.getOriginalBounds().width(),
                    mMasterImage.getOriginalBounds().height(), mMasterImage.getBitmap().getWidth(),
                    mMasterImage.getBitmap().getHeight());
            boolean isSegmentValid = mMaskSimulator.initSegment(mAbsoluteFilePath, mMasterImage
                    .getBitmap());
            if (!isSegmentValid) {
                errorHandle();
                return null;
            }
        }
        return mMaskSimulator;
    }

    private boolean finishIfInvalidUri() {
        if (!mNeedCheckInvalid) {
            return false;
        }
        Uri uri = null;
        Intent intent = getIntent();
        if (intent != null) {
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
        mImageShowPick.setVisibility(View.INVISIBLE);
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
        String action = intent.getAction();
        mSelectedImageUri = intent.getData();
        if (mSelectedImageUri != null) {
            startLoadBitmap(mSelectedImageUri);
        }

        if (ACTION_STEREO_PICK.equalsIgnoreCase(action)) {
            mUIMode = UI_MODE_PICK;
        } else {
            mUIMode = UI_MODE_NONE;
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
    }

    private void loadXML() {
        setContentView(R.layout.m_stereo_refine_activity);

        mActionBar = getActionBar();
        mActionBar.setBackgroundDrawable(new ColorDrawable(ACTIONBAR_BACKGROUND_COLOR));
        mActionBar.setSubtitle(null);
        mActionBar.setCustomView(R.layout.m_stereo_actionbar);

        setActionBarDisplayOptions(mActionBar, true, true);

        configImageShows();

        configButtons();
    }

    private void setupMenu() {
        if (mMenu == null || mMasterImage == null) {
            return;
        }

        mMenuZoomPan = mMenu.findItem(R.id.refine_hand);
        mMenuAdd = mMenu.findItem(R.id.refine_add);
        mMenuDel = mMenu.findItem(R.id.refine_del);

        mMenuZoomPan.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                updateUIMode(UI_MODE_REFINE_ZOOMPAN);
                return false;
            }
        });

        mMenuAdd.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                updateUIMode(UI_MODE_REFINE_ADD);
                return false;
            }
        });

        mMenuDel.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                updateUIMode(UI_MODE_REFINE_DEL);
                return false;
            }
        });

        updateUIByUIMode();
    }

    private void errorHandle() {
        Toast.makeText(this, getString(R.string.m_general_err_tip),
                Toast.LENGTH_LONG).show();
        finish();
    }

    private void updateMenuIcon(MenuItem menuItem, int iconId) {
        mMenuZoomPan.setIcon(R.drawable.m_zoom_and_pan_photo_inactive);
        mMenuAdd.setIcon(R.drawable.m_select_more_detail_inactive);
        mMenuDel.setIcon(R.drawable.m_deselect_detail_inactive);
        menuItem.setIcon(iconId);
    }

    private void updateUIMode(int uiMode) {
        mUIMode = uiMode;
        updateUIByUIMode();
    }

    private void updateUIByUIMode() {
        if (mMenu == null) {
            return;
        }
        switch (mUIMode) {
        case UI_MODE_PICK:
            switchImageShow(mImageShowPick);
            mCurrentImageShow.invalidate();
            mActionBar.setTitle(R.string.m_refine_select_obj_title);
            mActionBar.setDisplayShowCustomEnabled(false);
            setActionBarDisplayOptions(mActionBar, true, true);
            mMenuZoomPan.setVisible(false);
            mMenuAdd.setVisible(false);
            mMenuDel.setVisible(false);
            mButtonCancel.setVisibility(View.VISIBLE);
            mButtonRefine.setVisibility(View.VISIBLE);
            mButtonUndo.setVisibility(View.GONE);
            mButtonDone.setVisibility(View.VISIBLE);
            mButtonMore.setVisibility(View.GONE);
            mButtonSaveClipping.setVisibility(View.INVISIBLE);
            break;
        case UI_MODE_ENTER_REFINE:
            mActionBar.setTitle(R.string.m_refine_refine_obj_title);
            mActionBar.setDisplayShowCustomEnabled(false);
            setActionBarDisplayOptions(mActionBar, true, true);
            mMenuZoomPan.setVisible(true);
            mMenuAdd.setVisible(true);
            mMenuDel.setVisible(true);
            mButtonCancel.setVisibility(View.VISIBLE);
            mButtonRefine.setVisibility(View.GONE);
            mButtonUndo.setVisibility(View.VISIBLE);
            mButtonUndo.setEnabled(false);
            mButtonDone.setVisibility(View.VISIBLE);
            mButtonMore.setVisibility(View.VISIBLE);
            enterZoomPanMode();
            break;
        case UI_MODE_REFINE_ZOOMPAN:
            enterZoomPanMode();
            break;
        case UI_MODE_REFINE_ADD:
            switchImageShow(mImageShowRefine);
            ((ImageShowRefine) (mImageShowRefine)).setMode(ImageShowRefine.MODE_ADD);
            mCurrentImageShow.invalidate();
            updateMenuIcon(mMenuAdd, R.drawable.m_select_more_detail);
            mButtonSaveClipping.setVisibility(View.INVISIBLE);
            break;
        case UI_MODE_REFINE_DEL:
            switchImageShow(mImageShowRefine);
            ((ImageShowRefine) (mImageShowRefine)).setMode(ImageShowRefine.MODE_DEL);
            mCurrentImageShow.invalidate();
            updateMenuIcon(mMenuDel, R.drawable.m_deselect_detail);
            mButtonSaveClipping.setVisibility(View.INVISIBLE);
            break;
        default:
            break;
        }
    }

    private String saveRefineResult() {
        final File folder = new File(StorageManagerEx.getDefaultPath().toString() + "/"
                + STEREO_CLIPPINGS_FOLDER_NAME);
        if (!folder.exists()) {
            MtkLog.i(TAG, "<saveRefineResult> create Clippings folder");
            folder.mkdir();
        }
        String filename = new SimpleDateFormat(TIME_STAMP_NAME).format(new Date(System
                .currentTimeMillis()));
        final File target = new File(folder, PREFIX_CLIPPING + filename + POSTFIX_CLIPPING);

        new Thread(new Runnable() {
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        mButtonSaveClipping.setVisibility(View.INVISIBLE);
                        showSavingProgress(/* "Clippings" */null);
                    }
                });
                Bitmap oriBitmap = ImageLoader.loadOrientedBitmap(StereoRefineActivity.this,
                        mMasterImage.getUri(), null);
                Bitmap toSaveBitmap = mMaskSimulator.getForground(oriBitmap);
                oriBitmap.recycle();
                if (!Generator.isStorageSafeForGenerating(folder.getAbsolutePath())) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(StereoRefineActivity.this,
                                    getString(R.string.m_general_err_tip), Toast.LENGTH_LONG)
                                    .show();
                            hideSavingProgress();
                        }
                    });
                    return;
                }
                ImageSaver.saveBitmapToFile(target, toSaveBitmap, Bitmap.CompressFormat.PNG);
                toSaveBitmap.recycle();
                MediaScannerConnection.scanFile(StereoRefineActivity.this, new String[] { target
                        .getAbsolutePath() }, null, null);
                runOnUiThread(new Runnable() {
                    public void run() {
                        hideSavingProgress();
                        Toast.makeText(StereoRefineActivity.this,
                                getString(R.string.m_refine_saved_clipping), Toast.LENGTH_LONG)
                                .show();
                    }
                });
            }
        }).start();

        return target.getAbsolutePath();
    }

    private void toggleButtonsInMore() {
        if (mButtonSaveClipping != null) {
            int visibility = mButtonSaveClipping.getVisibility();
            if (visibility == View.VISIBLE) {
                mButtonSaveClipping.setVisibility(View.INVISIBLE);
            } else {
                mButtonSaveClipping.setVisibility(View.VISIBLE);
            }
        }
    }

    // TODO use dispatch touch event instead
    // public void hideButtonsInMore() {
    // if (mButtonSaveClipping != null) {
    // mButtonSaveClipping.setVisibility(View.INVISIBLE);
    // }
    // }

    // /////////////// common part /////////////////////

    // ImageZoomPan params
    // 1. zoom-pan enabled

    // ImageShowRefine params
    // 1. add/del

    private void switchImageShow(ImageShow newImageShow) {
        mCurrentImageShow = newImageShow;
        mImageShowPick.setVisibility(View.GONE);
        mImageShowZoomPan.setVisibility(View.GONE);
        mImageShowRefine.setVisibility(View.GONE);
        if (newImageShow != null) {
            newImageShow.setVisibility(View.VISIBLE);
        }
    }

    private void configButtons() {
        mButtonMore = findViewById(R.id.button_more);
        mButtonSaveClipping = findViewById(R.id.button_save);
        mButtonCancel = findViewById(R.id.button_cancel);
        mButtonRefine = findViewById(R.id.button_refine);
        mButtonUndo = findViewById(R.id.button_undo);
        mButtonDone = findViewById(R.id.button_done);

        ((Button) mButtonSaveClipping).setTransformationMethod(null);

        mButtonMore.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // mButtonSaveClipping.setVisibility(View.VISIBLE);
                toggleButtonsInMore();
            }
        });
        mButtonSaveClipping.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                saveRefineResult();
            }
        });
        mButtonRefine.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUIMode(UI_MODE_ENTER_REFINE);
            }
        });
        mButtonCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((mUIMode >= UI_MODE_ENTER_REFINE)) {
                    updateUIMode(UI_MODE_PICK);
                } else {
                    onBackPressed();
                }
            }
        });
        mButtonUndo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mMaskSimulator.undo();
                updateButtons();
                mCurrentImageShow.invalidate();
            }
        });
        mButtonDone.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finishRefine();
            }
        });
    }

    private void configImageShows() {
        mImageShowPick = (ImageShow) findViewById(R.id.imageShowPick);
        mImageShowZoomPan = (ImageShow) findViewById(R.id.imageShowZoomPan);
        mImageShowRefine = (ImageShow) findViewById(R.id.imageShowRefine);

        ((ImageShowPick) mImageShowPick)
                .setOnRefineListener(new ImageShowPick.IOnRefineListener() {
                    @Override
                    public void onRefineStart() {
                        // null implementation
                    }

                    @Override
                    public void onRefineEnd() {
                        updateButtons();
                    }

                    @Override
                    public void onRefineError() {
                        errorHandle();
                    }
                });

        ((ImageShowRefine) mImageShowRefine)
                .setOnRefineListener(new ImageShowRefine.IOnRefineListener() {
                    @Override
                    public void onRefineStart() {
                        // null implementation
                    }

                    @Override
                    public void onRefineEnd() {
                        updateButtons();
                    }

                    @Override
                    public void onRefineError() {
                        errorHandle();
                    }
                });
    }

    private void finishRefine() {
        Bitmap oriBitmap = mMasterImage.getBitmap();
        Bitmap foregroundBitmap = mMaskSimulator.getForground(oriBitmap);

        mMaskSimulator.saveMaskToXmp();
        mMaskSimulator.release();
        mMaskSimulator = null;

        Intent intentMe = getIntent();
        String destUriString = intentMe.getStringExtra("COPY_DEST_URI");
        if (destUriString == null) {
            setResult(RESULT_OK);
            finish();
            return;
        }

        Intent intent = new Intent(ACTION_STEREO_SYNTH);

        Uri destUri = Uri.parse(destUriString);
        intent.putExtra("COPY_SRC_URI", mSelectedImageUri.toString());
        // TODO redundant extra, which should be aligned with COPY_SRC_URI
        intent.putExtra("COPY_SRC_FILE_PATH", mAbsoluteFilePath);
        SegmentUtils.setCommunicationBitmap(foregroundBitmap);
        intent.setDataAndType(destUri, intentMe.getType()).setFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        ((Activity) StereoRefineActivity.this).startActivityForResult(intent,
                REQUEST_MERGE_IMAGE);
    }

    private void enterZoomPanMode() {
        switchImageShow(mImageShowZoomPan);
        mCurrentImageShow.invalidate();
        updateMenuIcon(mMenuZoomPan, R.drawable.m_zoom_and_pan_photo);
        mButtonSaveClipping.setVisibility(View.INVISIBLE);
    }

    private void updateButtons() {
        if (mMaskSimulator != null && mButtonUndo != null) {
            mButtonUndo.setEnabled(mMaskSimulator.isUndoEnabled());
            Rect foregroundBound = mMaskSimulator.getClippingBox();
            boolean canSave = (foregroundBound.width() > 0)
                    && (foregroundBound.height() > 0);
            mButtonDone.setEnabled(canSave);
            mButtonSaveClipping.setEnabled(canSave);
        }
    }
}
