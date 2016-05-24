/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.galleryfeature.stereo.refocus;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ActivityChooserModel;
import android.widget.ActivityChooserModel.OnChooseActivityListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.data.LocalAlbum;
import com.android.gallery3d.util.GalleryUtils;

import com.mediatek.gallery3d.util.TraceHelper;
import com.mediatek.galleryfeature.hotknot.HotKnot;
import com.mediatek.galleryfeature.stereo.RefocusImageJni;
import com.mediatek.galleryfeature.stereo.refocus.ReFocusView.RefocusListener;
import com.mediatek.galleryframework.util.DebugUtils;
import com.mediatek.galleryframework.util.MtkLog;
import com.mediatek.galleryframework.util.ProgressFragment;
import com.mediatek.galleryframework.util.Utils;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * RefocusActivity for stereo refocus feature.
 *
 */
public class RefocusActivity extends Activity implements
        OnChooseActivityListener, RefocusListener,
        SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "Gallery2/Refocus/RefocusActivity";
    private static final int TOTAL_DURATION_TIME = 600;
    private static final int FIRST_DURATION_TIME = 500;
    public static final String REFOCUS_ACTION = "com.android.gallery3d.action.REFOCUS";
    public static final String REFOCUS_IMAGE_WIDTH = "image-width";
    public static final String REFOCUS_IMAGE_HEIGHT = "image-height";
    public static final String REFOCUS_IMAGE_ORIENTATION = "image-orientation";
    public static final String REFOCUS_IMAGE_NAME = "image-name";
    public static final String REFOCUS_IMAGE_PATH = "image-path";
    public static final String MIME_TYPE = "image/*";
    public static final String REFOCUS_MIME_TYPE = "mimeType";
    private static final String ENABLE_DEBUG_STRING = "debug.gallery.enable";
    private static final String ENABLE_DUMP_BITMAP_STRING = "dump.gallery.enable";
    private static final String ANIMATION_LEN_SETTING_STRING = "animation.duration";
    private static final boolean ENABLE_DEBUG = SystemProperties.get(
            ENABLE_DEBUG_STRING).equals("1");
    private static final boolean ENABLE_DUMP_BITMAP = SystemProperties.get(
            ENABLE_DUMP_BITMAP_STRING).equals("1");
    private static final int ANIMATION_LEN_FOR_DEBUG = SystemProperties.getInt(
            ANIMATION_LEN_SETTING_STRING, 0);
    private static final int MSG_INIT_FINISH = 1;
    private static final int MSG_GENERATE_IMAGE = 2;
    private static final int MSG_GENERATE_DONE = 3;
    private static final int MSG_REFOCUS_ERROR = 4;
    private static final int MSG_HIDE_DOF_VIEW = 5;

    private static final int REQUEST_SET_AS = 1;
    private static final int PROGRESS_PER_DOF = 15;
    //private static final int SEEKBAR_PROCESS_INIT = 24 * PROGRESS_PER_DOF;
    private static final int DEFAULT_DOF_LEVEL = 24;
    private static final int MIN_DOF_LEVEL = 0;
    private static final int MAX_DOF_LEVEL = 36;
    private static final float DEFAULT_X_COORD_FACTOR = 0.5f;
    private static final float DEFAULT_Y_COORD_FACTOR = 0.5f;
    private static final int HALF_TRANSPARENT_COLOR = 0x99333333;
    private static final int DOF_VIEW_DELAY_TIME = 1000;
    private static final String[] DOFDATA = { "F16", "F13", "F11", "F10",
            "F9.0", "F8.0", "F7.2", "F6.3", "F5.6", "F4.5", "F3.6", "F2.8",
            "F2.2", "F1.8", "F1.4", "F1.2", "F1.0", "F0.8", "F0.6" };

    private WeakReference<DialogFragment> mSavingProgressDialog;
    private WeakReference<DialogFragment> mLoadingProgressDialog;

    private float mImageWidth;
    private float mImageHeight;
    private int mImageOrientation;
    private String mImageName;
    private String mSourceFilePath;
    private Bitmap mOriginalBitmap = null;
    private Uri mSourceUri = null;
    private ReFocusView mRefocusView = null;
    private RefocusImageJni mRefocusImage = null;
    private View mSaveButton = null;
    private Handler mHandler;
    private LoadBitmapTask mLoadBitmapTask = null;
    private ShareActionProvider mShareActionProvider;
    private GeneRefocusImageTask mGeneRefocusImageTask;
    private SeekBar mRefocusSeekBar;
    private HotKnot mHotKnot;
    private Intent mShareIntent;
    private MenuItem mShareMenuItem;
    private ActivityChooserModel mDataModel;
    private TextView mDofView = null;

    private Intent mIntent = null;
    private Uri mInsertUri = null;
    private String mFilePath = null;
    private int mDepth = DEFAULT_DOF_LEVEL;
    private int[] mTouchBitmapCoord = new int[2];
    private int mShowImageTotalDurationTime = TOTAL_DURATION_TIME;
    private int mShowImageFirstDurationTime = FIRST_DURATION_TIME;

    private boolean mIsSetDepthOnly = false;
    private boolean mIsSharingImage = false;
    private boolean mIsSetPictureAs = false;
    private boolean mIsShareHotKnot = false;
    private boolean mIsCancelThread = false;
    // true -> generate refocus image and show it 
    // when first launch
    // false -> just generate refocus image, but show
    // original image
    private boolean mIsShowRefocusImage = false;
    private boolean mIsFirstLaunch = true;
    private boolean mIsDoingRefocus = false;

    @Override
    public void onCreate(Bundle bundle) {
        TraceHelper.traceBegin(">>>>Refocus-onCreate");
        MtkLog.d(TAG, "<onCreate> begin");
        super.onCreate(bundle);
        mIntent = getIntent();
        if (null == mIntent.getData()) {
            MtkLog.d(TAG, "<onCreate> mSourceUri is null,so finish!!!");
            finish();
            return;
        }
        initializeViews();
        initializeData();
        mHandler = new Handler() {
            @SuppressWarnings("unchecked")
            public void handleMessage(Message message) {
                switch (message.what) {
                case MSG_INIT_FINISH:
                    hideLodingProgress();
                    setSaveState(true);
                    return;
                case MSG_GENERATE_IMAGE:
                    if (mGeneRefocusImageTask != null) {
                        mGeneRefocusImageTask.notifyDirty();
                    }
                    return;
                case MSG_GENERATE_DONE:
                    setSaveState(true);
                    return;
                case MSG_REFOCUS_ERROR:
                    errorHandleWhenRefocus();
                    return;
                case MSG_HIDE_DOF_VIEW:
                    mDofView.setVisibility(View.GONE);
                    break;
                default:
                    throw new AssertionError();
                }
            }
        };
        MtkLog.d(TAG, "<onCreate> end");
        TraceHelper.traceEnd();
    }

    // Shows status bar in portrait view, hide in landscape view
    private void toggleStatusBarByOrientation() {
        Window win = getWindow();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            win.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private void initRefocusSeekBar() {
        MtkLog.d(TAG, "<initRefocusSeekBar>");
        ImageView small = (ImageView) this.findViewById(R.id.small_aperture);
        small.setVisibility(View.VISIBLE);

        ImageView big = (ImageView) this.findViewById(R.id.big_aperture);
        big.setVisibility(View.VISIBLE);

        mDofView = (TextView) this.findViewById(R.id.dof_view);

        mRefocusSeekBar = (SeekBar) this.findViewById(R.id.refocusSeekBar);
        mRefocusSeekBar.setProgress(mDepth * PROGRESS_PER_DOF);
        mRefocusSeekBar.setVisibility(View.VISIBLE);
        mRefocusSeekBar.setOnSeekBarChangeListener(this);
    }

    @Override
    public void onProgressChanged(SeekBar refocusSeekBar, int progress,
            boolean fromuser) {
        int depth = progress / PROGRESS_PER_DOF;
        if (depth == 0) {
            depth = 1;
        }
        mDofView.setText(DOFDATA[depth / 2]);
    }

    @Override
    public void onStartTrackingTouch(SeekBar refocusSeekBar) {
        mHandler.removeMessages(MSG_HIDE_DOF_VIEW);
        mDofView.setText(DOFDATA[mDepth / 2]);
        mDofView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStopTrackingTouch(SeekBar refocusSeekBar) {
        if (mIsDoingRefocus) {
            showWaitToast();
            MtkLog.d(TAG, "<onStopTrackingTouch> please wait!");
            return;
        }
        mIsDoingRefocus = true;
        mIsSetDepthOnly = true;
        mIsCancelThread = false;
        mDepth = refocusSeekBar.getProgress() / PROGRESS_PER_DOF;
        if (mDepth == 0) {
            mDepth = 1;
        }
        mHandler.sendEmptyMessageDelayed(MSG_HIDE_DOF_VIEW, DOF_VIEW_DELAY_TIME);
        MtkLog.d(TAG, "<onStopTrackingTouch> Seekbar reset mDepth = " + mDepth);
        mHandler.sendEmptyMessage(MSG_GENERATE_IMAGE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.m_refocus_activity_menu, menu);
        mHotKnot.updateMenu(menu, R.id.menu_refocus_share, R.id.action_hotknot, true);
        mShareMenuItem = menu.findItem(R.id.menu_refocus_share);
        initShareActionProvider();
        return true;
    }

    @Override
    public boolean onChooseActivity(ActivityChooserModel host, Intent intent) {
        MtkLog.d(TAG, "<onChooseActivity> enter, intent " + intent);
        mShareIntent = intent;
        mIsSharingImage = true;
        showSavingProgress(mImageName);
        startSaveBitmap(mSourceUri);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_setas:
            mIsSetPictureAs = true;
            showSavingProgress(mImageName);
            startSaveBitmap(mSourceUri);
            return true;
        case R.id.action_hotknot:
            mIsShareHotKnot = true;
            showSavingProgress(mImageName);
            startSaveBitmap(mSourceUri);
            return true;
        default:
            break;
        }
        return false;
    }

    private void setSaveState(boolean enable) {
        if (mSaveButton != null) {
            mSaveButton.setEnabled(enable);
        }
    }

    @Override
    protected void onDestroy() {
        MtkLog.d(TAG, "<onDestroy>");
        if (mLoadBitmapTask != null) {
            mLoadBitmapTask.cancel(false);
            mLoadBitmapTask = null;
        }
        if (mRefocusImage != null) {
            mRefocusImage.release();
        }
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void startLoadBitmap(String filePath) {
        MtkLog.d(TAG, "<startLoadBitmap> filePath:" + filePath);
        if (filePath != null) {
            setSaveState(false);
            showLoadingProgress();
            mLoadBitmapTask = new LoadBitmapTask();
            mLoadBitmapTask.execute(filePath);
        } else {
            showImageLoadFailToast();
            finish();
        }
    }

    private void requestFirstRefocus() {
        Rect faceRect = mRefocusImage.getDefaultFaceRect((int) mImageWidth, (int) mImageHeight);
        if (faceRect != null) {
            int faceCenterX = (faceRect.left + faceRect.right) / 2;
            int faceCenterY = (faceRect.top + faceRect.bottom) / 2;
            MtkLog.d(TAG, "<requestFirstRefocus> faceRect " + faceRect + ", faceCenterX "
                    + faceCenterX + ", faceCenterY " + faceCenterY);
            setRefocusImage(faceCenterX / mImageWidth, faceCenterY / mImageHeight);
            return;
        }

        int[] coord = mRefocusImage.getDefaultFocusCoord((int) mImageWidth, (int) mImageHeight);
        if (coord[0] > 0 && coord[0] < mImageWidth && coord[1] > 0 && coord[1] < mImageHeight) {
            MtkLog.d(TAG, "<requestFirstRefocus> xCoord " + coord[0] + ", yCoord " + coord[1]);
            setRefocusImage(coord[0] / mImageWidth, coord[1] / mImageHeight);
        } else {
            MtkLog.d(TAG, "<requestFirstRefocus> refocus to image center");
            setRefocusImage(DEFAULT_X_COORD_FACTOR, DEFAULT_Y_COORD_FACTOR);
        }
    }

    private void showImageLoadFailToast() {
        CharSequence text = getString(R.string.cannot_load_image);
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void showWaitToast() {
        CharSequence text = getString(R.string.m_please_wait);
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * Load bitmap task.
     */
    private class LoadBitmapTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... params) {
            long beginTime = System.currentTimeMillis();
            String filePath = params[0];
            long decodeTimestart = System.currentTimeMillis();

            TraceHelper.traceBegin(">>>>Refocus-LoadBitmapTask");
            Bitmap bitmap = RefocusHelper.decodeBitmap(filePath);
            TraceHelper.traceEnd();

            long decodeTime = System.currentTimeMillis() - decodeTimestart;
            MtkLog.d(TAG, "<LoadBitmapTask> decode time = " + decodeTime);
            long spendTime = System.currentTimeMillis() - beginTime;
            MtkLog.d(TAG, "<LoadBitmapTask> doInbackground time = " + spendTime);
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            mOriginalBitmap = result;
            if (mOriginalBitmap != null && mImageWidth != 0 && mImageHeight != 0) {
                long beginTime1 = System.currentTimeMillis();

                TraceHelper.traceBegin(">>>>Refocus-setImageActor");
                mRefocusView.setImageActor(mOriginalBitmap, mOriginalBitmap.getWidth(),
                        mOriginalBitmap.getHeight());
                TraceHelper.traceEnd();

                Bitmap bmp = mOriginalBitmap.copy(mOriginalBitmap.getConfig(), false);

                TraceHelper.traceBegin(">>>>Refocus-setImageActorNew");
                mRefocusView.setImageActorNew(bmp);
                TraceHelper.traceEnd();

                long needtime = System.currentTimeMillis() - beginTime1;
                MtkLog.d(TAG, "<LoadBitmapTask> setImageActor time = " + needtime);
                long beginTime2 = System.currentTimeMillis();
                needtime = System.currentTimeMillis() - beginTime2;
                MtkLog.d(TAG, "<LoadBitmapTask> setTransitionTime time = " + needtime);
                long spendTime = System.currentTimeMillis() - beginTime1;
                MtkLog.d(TAG, "<LoadBitmapTask> onPostExecute costs time = " + spendTime);
                initRefocusImages();
                initRefocusSeekBar();

                TraceHelper.traceBegin(">>>>Refocus-initRefocusImages");

                requestFirstRefocus();

                TraceHelper.traceEnd();
            } else {
                MtkLog.d(TAG, "<LoadBitmapTask> could not load image for Refocus!!");
                if (mOriginalBitmap != null) {
                    mOriginalBitmap.recycle();
                    mOriginalBitmap = null;
                }
                showImageLoadFailToast();
                setResult(RESULT_CANCELED, new Intent());
                finish();
            }
        }
    }

    protected void saveRefocusBitmap() {
        setSaveState(false);
        File saveDir = RefocusHelper.getFinalSaveDirectory(this, mSourceUri);
        int bucketId = GalleryUtils.getBucketId(saveDir.getPath());
        String albumName = LocalAlbum.getLocalizedName(getResources(), bucketId, null);
        showSavingProgress(albumName);
        startSaveBitmap(mSourceUri);
    }

    private void startSaveBitmap(Uri sourceUri) {
        SaveBitmapTask saveTask = new SaveBitmapTask(sourceUri);
        saveTask.execute();
    }

    /**
     * Save bitmap task.
     */
    private class SaveBitmapTask extends AsyncTask<Bitmap, Void, Boolean> {
        Uri mSourceUri;

        public SaveBitmapTask(Uri sourceUri) {
            mSourceUri = sourceUri;
        }

        @Override
        protected Boolean doInBackground(Bitmap... params) {
            if (mSourceUri == null) {
                return false;
            }
            MtkLog.d(TAG, "<SaveBitmapTask> start");
            TraceHelper.traceBegin(">>>>Refocus-SaveBitmapTask");
            mInsertUri = mRefocusImage.saveRefocusImage(mSourceUri,
                    RefocusActivity.this.getApplicationContext(), mTouchBitmapCoord, mDepth,
                    mImageWidth, mImageHeight, true);
            TraceHelper.traceEnd();
            MtkLog.d(TAG, "<SaveBitmapTask> end");
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            finishSaveBitmap(mInsertUri);
        }
    }

    private void finishSaveBitmap(Uri destUri) {
        MtkLog.d(TAG, "<finishSaveBitmap> destUri:" + destUri);
        if (destUri == null) {
            showSaveFailToast();
            MtkLog.d(TAG, "<finishSaveBitmap> saving fail");
            return;
        }
        MtkLog.d(TAG, "<finishSaveBitmap> saving finish");
        if (mIsSharingImage) {
            MtkLog.d(TAG, "<finishSaveBitmap> normal share");
            mShareIntent.removeExtra(Intent.EXTRA_STREAM);
            mShareIntent.putExtra(Intent.EXTRA_STREAM, destUri);
            startActivity(mShareIntent);
            MtkLog.d(TAG, "<finishSaveBitmap> start share intent done");
        } else if (mIsSetPictureAs) {
            MtkLog.d(TAG, "<finishSaveBitmap> set picture as");
            Intent intent = new Intent(Intent.ACTION_ATTACH_DATA)
                    .setDataAndType(destUri, MIME_TYPE).addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(REFOCUS_MIME_TYPE, intent.getType());
            // doesn't exit refocus when setAs
            startActivityForResult(Intent.createChooser(intent, getString(R.string.set_as)),
                    REQUEST_SET_AS);
            return;
        } else if (mIsShareHotKnot) {
            MtkLog.d(TAG, "<finishSaveBitmap> share via hotnot");
            Uri contentUri = destUri;
            mHotKnot.sendUri(contentUri, MIME_TYPE);
        }
        setResult(RESULT_OK, new Intent().setData(destUri));
        MtkLog.d(TAG, "<finishSaveBitmap> set result and finish activity");
        finish();
    }

    private void initRefocusImages() {
        boolean initResult = false;
        mFilePath = RefocusHelper.getRealFilePathFromURI(getApplicationContext(), mSourceUri);
        long refocusImageInitTimestart = System.currentTimeMillis();
        if (mFilePath != null) {
            mRefocusImage.createImageRefocus(mFilePath,
                    RefocusImageJni.RefocusMode.REFOCUS_MODE_FULL);
            initResult = mRefocusImage.initImageRefocus(mFilePath, (int) mImageWidth,
                    (int) mImageHeight);
        }
        if (!initResult) {
            MtkLog.d(TAG, "<initRefocusImages> error, abort init");
            mHandler.sendEmptyMessage(MSG_REFOCUS_ERROR);
            return;
        }
        long refocusImageInitSpentTime = System.currentTimeMillis() - refocusImageInitTimestart;
        MtkLog.d(TAG, "<initRefocusImages> performance RefocusImageInitSpent time = "
                + refocusImageInitSpentTime);
        mDepth = mIsShowRefocusImage ? DEFAULT_DOF_LEVEL : mRefocusImage.getDefaultDofLevel();
        if (mDepth < MIN_DOF_LEVEL || mDepth > MAX_DOF_LEVEL) {
            mDepth = DEFAULT_DOF_LEVEL;
        }
        if (mDepth == 0) {
            mDepth = 1;
        }
        if (ENABLE_DEBUG) {
            int depBufWidth = mRefocusImage.getDepBufWidth();
            int depBufHeight = mRefocusImage.getDepBufHeight();
            MtkLog.d(TAG, "<initRefocusImages> depBufWidth = " + depBufWidth + ", depBufHeight = "
                    + depBufHeight);
            mRefocusView.setDepthActor(mRefocusImage.getDepthBuffer(), 0, 1, depBufWidth,
                    depBufHeight);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        MtkLog.d(TAG, "<onBackPressed>");
        if (mGeneRefocusImageTask != null) {
            mGeneRefocusImageTask.terminate();
            mGeneRefocusImageTask = null;
        }
    }

    @Override
    protected void onPause() {
        MtkLog.d(TAG, "<OnPause>");
        super.onPause();
        hideLodingProgress();
        hideSavingProgress();
        if (mGeneRefocusImageTask != null) {
            mGeneRefocusImageTask.terminate();
            mGeneRefocusImageTask = null;
        }
        if (mDataModel != null) {
            mDataModel.setOnChooseActivityListener(null);
            MtkLog.d(TAG, "<OnPause> clear OnChooseActivityListener");
        }
    }

    @Override
    protected void onResume() {
        MtkLog.d(TAG, "<onResume>");
        super.onResume();
        mGeneRefocusImageTask = new GeneRefocusImageTask();
        mGeneRefocusImageTask.start();
        if (mDataModel != null) {
            mDataModel.setOnChooseActivityListener(this);
            MtkLog.d(TAG, "<onResume> setOnChooseActivityListener ");
        }
    }

    private void showSavingProgress(String albumName) {
        DialogFragment fragment;

        if (mSavingProgressDialog != null) {
            fragment = mSavingProgressDialog.get();
            if (fragment != null) {
                fragment.show(getFragmentManager(), null);
                return;
            }
        }
        String progressText;
        if (albumName == null) {
            progressText = getString(R.string.saving_image);
        } else {
            progressText = getString(R.string.m_refocus_saving_image, albumName);
        }
        final DialogFragment genProgressDialog = new ProgressFragment(progressText);
        genProgressDialog.setCancelable(false);
        genProgressDialog.show(getFragmentManager(), null);
        genProgressDialog.setStyle(R.style.RefocusDialog, genProgressDialog.getTheme());
        mSavingProgressDialog = new WeakReference<DialogFragment>(genProgressDialog);
    }

    private void showLoadingProgress() {
        DialogFragment fragment;
        if (mLoadingProgressDialog != null) {
            fragment = mLoadingProgressDialog.get();
            if (fragment != null) {
                fragment.show(getFragmentManager(), null);
                return;
            }
        }
        final DialogFragment genProgressDialog = new ProgressFragment(R.string.loading_image);
        genProgressDialog.setCancelable(false);
        genProgressDialog.show(getFragmentManager(), null);
        genProgressDialog.setStyle(R.style.RefocusDialog, genProgressDialog.getTheme());
        mLoadingProgressDialog = new WeakReference<DialogFragment>(genProgressDialog);
    }

    private void hideLodingProgress() {
        if (mLoadingProgressDialog != null) {
            DialogFragment fragment = mLoadingProgressDialog.get();
            if (fragment != null) {
                fragment.dismiss();
            }
        }
    }

    private void hideSavingProgress() {
        if (mSavingProgressDialog != null) {
            DialogFragment progress = mSavingProgressDialog.get();
            if (progress != null) {
                progress.dismissAllowingStateLoss();
            }
        }
    }

    private void showSaveFailToast() {
        CharSequence text = getString(R.string.m_refocus_save_fail);
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    public void setRefocusImage(float x, float y) {
        if (mIsDoingRefocus) {
            MtkLog.d(TAG, "<setRefocusImage> please wait!");
            showWaitToast();
            return;
        }
        mIsDoingRefocus = true;
        setSaveState(false);
        mTouchBitmapCoord[0] = (int) (x * mImageWidth);
        mTouchBitmapCoord[1] = (int) (y * mImageHeight);
        mIsSetDepthOnly = false;
        MtkLog.d(TAG, "<setRefocusImage> x = " + x + ", y = " + y + "mTouchBitmapCoord[0] = "
                + mTouchBitmapCoord[0] + " mTouchBitmapCoord[1] = " + mTouchBitmapCoord[1]);
        mGeneRefocusImageTask.notifyDirty();
    }

    /**
     * Generate refocus image task.
     */
    private class GeneRefocusImageTask extends Thread {
        private volatile boolean mDirty = false;
        private volatile boolean mActive = true;
        private int mIndex = 0;

        public GeneRefocusImageTask() {
            setName("GeneRefocusImageTask");
        }

        @Override
        public void run() {
            while (mActive) {
                synchronized (this) {
                    if (!mDirty && mActive) {
                        MtkLog.d(TAG, "<GeneRefocusImageTask> wait");
                        Utils.waitWithoutInterrupt(this);
                        continue;
                    }
                }
                mDirty = false;
                MtkLog.d(TAG, "<GeneRefocusImageTask> mDepth = " + mDepth + ",x:" +
                        mTouchBitmapCoord[0] + ",y:" + mTouchBitmapCoord[1]);

                if (mIsCancelThread) {
                    MtkLog.d(TAG, "<GeneRefocusImageTask> cancel generate task.");
                    continue;
                }
                if (!mIsShowRefocusImage && mIsFirstLaunch) {
                    mRefocusImage.generateDepth();
                } else {
                    TraceHelper.traceBegin(">>>>Refocus-generateRefocusImage");
                    Bitmap newBitmap = mRefocusImage.generateRefocusImage(mTouchBitmapCoord[0],
                            mTouchBitmapCoord[1], mDepth);
                    TraceHelper.traceEnd();
                    if (ENABLE_DUMP_BITMAP) {
                        DebugUtils.dumpBitmap(newBitmap, "GeneratedBitmap_" + mIndex + ".jpg");
                        mIndex++;
                    }
                    if (mIsSetDepthOnly) {
                        mRefocusView.setImageActor(newBitmap, -1, -1);
                        mIsSetDepthOnly = false;
                    } else if (mIsShowRefocusImage) {
                        mRefocusView.setImageActor(newBitmap, newBitmap.getWidth(),
                                newBitmap.getHeight());
                    } else {
                        mRefocusView.setImageActorNew(newBitmap);
                    }
                }
                if (mIsFirstLaunch) {
                    mHandler.sendEmptyMessage(MSG_INIT_FINISH);
                    mIsFirstLaunch = false;
                }
                mIsDoingRefocus = false;
                mHandler.sendEmptyMessage(MSG_GENERATE_DONE);
            }
        }

        public synchronized void notifyDirty() {
            MtkLog.d(TAG, "<GeneRefocusImageTask> notifyDirty");
            mDirty = true;
            notifyAll();
        }

        public synchronized void terminate() {
            mActive = false;
            notifyAll();
        }
    }

    private void initializeViews() {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.m_refocus_activity);
        initActionBar();
        toggleStatusBarByOrientation();
        mRefocusView = (ReFocusView) findViewById(R.id.refocus_view);
        mRefocusView.setRefocusListener(this);
        mRefocusView.setZOrderOnTop(false);
        if (ANIMATION_LEN_FOR_DEBUG > 0) {
            mShowImageTotalDurationTime = ANIMATION_LEN_FOR_DEBUG;
            MtkLog.d(TAG, "<initializeViews> reset animation duration to "
                    + ANIMATION_LEN_FOR_DEBUG);
        }
        MtkLog.d(TAG, "<initializeViews> mShowImagekTotalDurationTime "
                + mShowImageTotalDurationTime);
        mRefocusView.setTransitionTime(mShowImageTotalDurationTime,
                mShowImageFirstDurationTime);
    }

    private void initializeData() {
        setResult(RESULT_CANCELED, new Intent());
        mRefocusImage = new RefocusImageJni();
        mHotKnot = new HotKnot(this);
        mSourceUri = mIntent.getData();
        MtkLog.d(TAG, "<initializeData> mSourceUri = " + mSourceUri);
        mImageWidth = mIntent.getExtras().getInt(REFOCUS_IMAGE_WIDTH);
        mImageHeight = mIntent.getExtras().getInt(REFOCUS_IMAGE_HEIGHT);
        mImageName = mIntent.getExtras().getString(REFOCUS_IMAGE_NAME);
        mSourceFilePath = mIntent.getExtras().getString(REFOCUS_IMAGE_PATH);
        mImageOrientation = mIntent.getExtras().getInt(REFOCUS_IMAGE_ORIENTATION);
        // set image width and height as default value
        mTouchBitmapCoord[0] = (int) (mImageWidth * DEFAULT_X_COORD_FACTOR);
        mTouchBitmapCoord[1] = (int) (mImageHeight * DEFAULT_Y_COORD_FACTOR);
        startLoadBitmap(mSourceFilePath);
    }

    private void initActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(
                    HALF_TRANSPARENT_COLOR));
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.m_refocus_actionbar);
            mSaveButton = actionBar.getCustomView();
            mSaveButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    saveRefocusBitmap();
                }
            });
        }
    }

    private void initShareActionProvider() {
        mShareActionProvider = (ShareActionProvider) mShareMenuItem
                .getActionProvider();
        mDataModel = ActivityChooserModel.get(this,
                ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
        Intent tempIntent = new Intent(Intent.ACTION_SEND);
        tempIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        tempIntent.setType(GalleryUtils.MIME_TYPE_IMAGE);
        tempIntent.putExtra(Intent.EXTRA_STREAM, mSourceUri);
        if (mShareActionProvider != null) {
            MtkLog.d(TAG, "<initShareActionProvider> setShareIntent");
            mShareActionProvider.setShareIntent(tempIntent);
        }
        if (mDataModel != null) {
            mDataModel.setOnChooseActivityListener(this);
            MtkLog.d(TAG, "<initShareActionProvider> setOnChooseActivityListener ");
        }
    }

    private void errorHandleWhenRefocus() {
        Toast.makeText(this, getString(R.string.m_general_err_tip), Toast.LENGTH_LONG).show();
        finish();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SET_AS) {
            MtkLog.d(TAG, "<onActivityResult> get result from setAs and finish");
            setResult(RESULT_OK, new Intent().setData(mInsertUri));
            finish();
        }
    }
}