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

import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.SimpleArrayMap;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.gallery3d.R;

import com.mediatek.gallery3d.util.TraceHelper;
import com.mediatek.galleryfeature.platform.PlatformHelper;
import com.mediatek.galleryfeature.stereo.segment.refine.StereoRefineActivity;
import com.mediatek.galleryframework.util.MtkLog;
import com.mediatek.galleryframework.util.ProgressFragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Activity for fancy color effect.
 */
public class FancyColorActivity extends Activity implements EffectManager.DataLoadingListener,
        EffectManager.EffectListener, EffectView.LayoutListener, EffectView.ItemClickListener,
        SavingRequest.SavingRequestListener, ThumbView.ThumbViewClickListener,
        ReloadMaskRequest.ReloadMaskListener {
    private final static String TAG = "MtkGallery2/FancyColor/FancyColorActivity";
    public final static String FANCY_COLOR_ACTION = "com.android.gallery3d.action.fancycolor";
    public final static String KEY_FILE_PATH = "filePath";
    public final static String KEY_SRC_BMP_WIDTH = "srcBmpWidth";
    public final static String KEY_SRC_BMP_HEIGHT = "srcBmpHeight";

    private final static String STEREO_PICK_ACTION = "action_stereo_pick";
    private final static String GRID_VIEW_ROW_COUNT_STRING = "fancy_color_row_count";
    private final static String GRID_VIEW_COLUM_COUNT_STRING = "fancy_color_colum_count";
    private final static int REGISTER_ALL_EFFECTS = -1;
    private final static int REQUEST_STEREO_PICK = 10;
    private final static int NORMAL_EFFECT_INDEX = 0;

    private EffectView mEffectView;
    private EffectManager mEffectManager;
    private Handler mHandler;
    private ActionBar mActionBar;
    private View mActionBarCustomerView;
    private View mSaveIcon;
    private Intent mIntent;
    private Uri mSourceUri;
    private String mSourcePath;
    private Bitmap mFinalEffectBitmap;

    private WeakReference<DialogFragment> mSavingProgressDialog;
    private WeakReference<DialogFragment> mLoadingProgressDialog;
    private ArrayList<String> mEffectNameList;
    private SimpleArrayMap<String, Integer> mEffectNameId;
    private Bitmap[] mPreviousEffectBitmapPreview;
    private int[] mEffectForLoading;
    private boolean mIsAtPreview = true;
    private boolean mIsSaving = false;
    private int mCurrentThumbViewIndex;
    private int mEffectCount = 0;
    private int mViewWidth;
    private int mViewHeight;
    private int mPreviewBitmapWidth;
    private int mPreviewBitmapHeight;
    private int mThumbBitmapWidth;
    private int mThumbBitmapHeight;
    private int mSourceBitmapWidth;
    private int mSourceBitmapHeight;
    private int mGridViewRowCount;
    private int mGridViewColumCount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        TraceHelper.traceBegin(">>>>FancyColor-onCreate");
        MtkLog.d(TAG, "<onCreate> begin!");
        super.onCreate(savedInstanceState);
        if (!checkIntentValid()) {
            return;
        }
        initializeData();
        initializeView();
        showLoadingProgress();
        mHandler = new Handler() {
            public void handleMessage(Message message) {
                switch (message.what) {
                case FancyColorHelper.MSG_UPDATE_VIEW:
                    updateView(message.arg1, (Bitmap) message.obj);
                    return;
                case FancyColorHelper.MSG_LOADING_FINISH:
                    if (message.arg1 == FancyColorHelper.TYPE_PREVIEW_THUMBNAIL) {
                        switchView(true, REGISTER_ALL_EFFECTS);
                    }
                    return;
                case FancyColorHelper.MSG_SAVING_FINISH:
                    setResult(RESULT_OK, new Intent().setData((Uri) message.obj));
                    finish();
                    return;
                case FancyColorHelper.MSG_RELOAD_THUMB_VIEW:
                    onThumbViewReady(mCurrentThumbViewIndex);
                    return;
                case FancyColorHelper.MSG_STATE_ERROR:
                    errorHandle();
                    return;
                case FancyColorHelper.MSG_HIDE_LOADING_PROGRESS:
                    hideLoadingProgress();
                    break;
                default:
                    throw new AssertionError();
                }
            }
        };
        mEffectManager.setHandler(mHandler);
        MtkLog.d(TAG, "<onCreate> end!");
        TraceHelper.traceEnd();
    }

    @Override
    public void onBackPressed() {
        TraceHelper.traceBegin(">>>>FancyColor-onBackPressed");
        MtkLog.d(TAG, "<onBackPressed> begin!");
        if (mIsAtPreview) {
            super.onBackPressed();
        } else {
            switchView(true, REGISTER_ALL_EFFECTS);
            showLoadingProgress();
        }
        MtkLog.d(TAG, "<onBackPressed> end!");
        TraceHelper.traceEnd();
    }

    @Override
    public void onLoadingFinish(Bitmap bitmap, int type) {
        if (bitmap == null) {
            MtkLog.d(TAG, "<onLoadingFinish> type " + type + " bitmap is null");
            return;
        }
        if (type == FancyColorHelper.TYPE_PREVIEW_THUMBNAIL) {
            mPreviewBitmapWidth = bitmap.getWidth();
            mPreviewBitmapHeight = bitmap.getHeight();
            mHandler.obtainMessage(FancyColorHelper.MSG_LOADING_FINISH, type, -1, null)
                    .sendToTarget();
        } else if (type == FancyColorHelper.TYPE_THUMBNAIL) {
            mThumbBitmapWidth = bitmap.getWidth();
            mThumbBitmapHeight = bitmap.getHeight();
        }
    }

    @Override
    public void onEffectDone(int index, Bitmap bitmap, int type) {
        MtkLog.d(TAG, "<onEffectDone> index " + index + ", bitmap " + bitmap);
        if (type == FancyColorHelper.TYPE_HIGH_RES_THUMBNAIL) {
            mFinalEffectBitmap = bitmap;
            PlatformHelper.submitJob(new SavingRequest((Context) this, mSourceUri,
                    mFinalEffectBitmap, mSourceBitmapWidth, mSourceBitmapHeight, this));
        } else {
            mHandler.obtainMessage(FancyColorHelper.MSG_UPDATE_VIEW, index, -1, bitmap)
                    .sendToTarget();
            if (type == FancyColorHelper.TYPE_THUMBNAIL) {
                mHandler.sendEmptyMessage(FancyColorHelper.MSG_HIDE_LOADING_PROGRESS);
            }
            if (type == FancyColorHelper.TYPE_PREVIEW_THUMBNAIL) {
                mEffectCount++;
                if (mEffectCount == mGridViewRowCount * mGridViewColumCount) {
                    mHandler.sendEmptyMessage(FancyColorHelper.MSG_HIDE_LOADING_PROGRESS);
                    mEffectCount = 0;
                }
            }
        }
    }

    @Override
    public void onSavingDone(Uri result) {
        MtkLog.d(TAG, "<onSavingDone> uri " + result);
        mHandler.obtainMessage(FancyColorHelper.MSG_SAVING_FINISH, result).sendToTarget();
    }

    @Override
    public void onItemClick(int position) {
        TraceHelper.traceBegin(">>>>FancyColor-onItemClick");
        MtkLog.d(TAG, "<onItemClick> click position " + position);
        switchView(false, position);
        showLoadingProgress();
        TraceHelper.traceEnd();
    }

    @Override
    public void onGridViewReady(int index) {
        MtkLog.d(TAG, "<onGridViewReady> index " + index);
        mEffectManager.requestEffectBitmap(index, FancyColorHelper.TYPE_PREVIEW_THUMBNAIL);
    }

    @Override
    public void onThumbViewReady(int index) {
        MtkLog.d(TAG, "<onThumbViewReady> index " + index);
        mEffectManager.requestEffectBitmap(index, FancyColorHelper.TYPE_THUMBNAIL);
        mCurrentThumbViewIndex = index;
    }

    @Override
    public void onThumbViewClick(int viewW, int viewH, float x, float y) {
        if (mCurrentThumbViewIndex == NORMAL_EFFECT_INDEX) {
            return;
        }
        MtkLog.d(TAG, "<onThumbViewClick> viewW " + viewW + ", viewH " + viewH + ", x " + x
                + ", y " + y);
        Point point = calcClickPosition(viewW, viewH, x, y);
        if (point != null) {
            showLoadingProgress();
            PlatformHelper.submitJob(new ReloadMaskRequest(mEffectManager.getFancyColorJniObject(),
                    point, this));
        }
    }

    @Override
    public void onReloadMaskDone() {
        MtkLog.d(TAG, "<onReloadMaskDone> index " + mCurrentThumbViewIndex);
        mHandler.obtainMessage(FancyColorHelper.MSG_RELOAD_THUMB_VIEW, null).sendToTarget();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mIsAtPreview) {
            mEffectView.onOrientationChange();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.m_fancy_color_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int action = item.getItemId();
        switch (action) {
        case R.id.action_refine:
            mEffectManager.setMaskBufferToSegment();
            startRefineActivity();
            break;
        default:
            break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        MtkLog.d(TAG, "<onResume> begin!");
        super.onResume();
        MtkLog.d(TAG, "<onResume> end!");
    }

    @Override
    protected void onPause() {
        MtkLog.d(TAG, "<onPause> begin!");
        hideSavingProgress();
        super.onPause();
        MtkLog.d(TAG, "<onPause> end!");
    }

    @Override
    protected void onDestroy() {
        MtkLog.d(TAG, "<onDestroy> begin!");
        super.onDestroy();
        mEffectManager.unregisterAllEffect();
        MtkLog.d(TAG, "<onDestroy> end!");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        MtkLog.d(TAG, "<onActivityResult> resultCode:" + resultCode);
        if (requestCode == REQUEST_STEREO_PICK && resultCode == RESULT_OK) {
            showLoadingProgress();
            PlatformHelper.submitJob(new ReloadMaskRequest(mEffectManager.getFancyColorJniObject(),
                    null, this));
        }
    }

    private Point calcClickPosition(int viewW, int viewH, float x, float y) {
        int thumbW = mThumbBitmapWidth;
        int thumbH = mThumbBitmapHeight;
        int gap;
        int mapX;
        int mapY;
        Rect validRange = new Rect();
        if (thumbH >= thumbW) {
            // bitmap height fulfills view height
            gap = (viewW - viewH * thumbW / thumbH) / 2;
            validRange.left = gap;
            validRange.right = viewW - gap;
            validRange.top = 0;
            validRange.bottom = viewH;
            mapX = thumbW * ((int) x - gap) / validRange.width();
            mapY = thumbH * (int) y / validRange.height();
        } else {
            // bitmap width fullfills view width
            gap = (viewH - viewW * thumbH / thumbW) / 2;
            validRange.left = 0;
            validRange.right = viewW;
            validRange.top = gap;
            validRange.bottom = viewH - gap;
            mapX = thumbW * (int) x / validRange.width();
            mapY = thumbH * ((int) y - gap) / validRange.height();
        }
        MtkLog.d(TAG, "<calcClickPosition> thumbW " + thumbW + ", thumbH " + thumbH + ", gap "
                + gap + ", rect lrtb " + validRange.left + ", " + validRange.right + ", "
                + validRange.top + ", " + validRange.bottom + ", mapX " + mapX + ", mapY " + mapY);
        if (!validRange.contains((int) x, (int) y)) {
            MtkLog.d(TAG, "<calcClickPosition> invalid click");
            return null;
        }
        return new Point(mapX, mapY);
    }

    private void releaseBitmap(int index, Bitmap bitmap) {
        if (mPreviousEffectBitmapPreview[index] != null) {
            mPreviousEffectBitmapPreview[index].recycle();
        }
        mPreviousEffectBitmapPreview[index] = bitmap;
    }

    private void updateView(int index, Bitmap bitmap) {
        mEffectView.updateView(mIsAtPreview, index, bitmap);
        releaseBitmap(index, bitmap);
    }

    private void registerEffects() {
        mEffectManager.registerEffect(this);
    }

    /**
     * Switch between gridview and imageview.
     * @param isAtPreview
     *            indicate at preview or not
     * @param index
     *            if isAtPreview is true, do not care index, register all
     *            effects if isAtPreview is false, just only register the effect
     *            of this index
     */
    private void switchView(boolean isAtPreview, int index) {
        MtkLog.d(TAG, "<switchView> isAtPreview " + isAtPreview + ", index " + index);
        if (isAtPreview) {
            // switch to gridview
            if (mActionBar != null) {
                mActionBar.hide();
            }
            mEffectView.init(this, mPreviewBitmapWidth, mPreviewBitmapHeight);
        } else {
            // switch to imageview
            if (mActionBar != null) {
                mActionBar.show();
            }
            mEffectView.init(this, index);
        }
        mIsAtPreview = isAtPreview;
    }

    private void setActionBar() {
        mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.hide();
            mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            mActionBar.setCustomView(R.layout.m_refocus_actionbar);
            mSaveIcon = mActionBar.getCustomView();
            mSaveIcon.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    saveImage(mCurrentThumbViewIndex);
                }
            });
        }
    }

    private void initViewSize() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        mViewWidth = metrics.widthPixels;
        mViewHeight = metrics.heightPixels;
        FancyColorHelper.setViewSize(mViewWidth, mViewHeight);
    }

    private void initializeData() {
        mSourceUri = mIntent.getData();
        mSourcePath = mIntent.getExtras().getString(KEY_FILE_PATH);
        mSourceBitmapWidth = mIntent.getExtras().getInt(KEY_SRC_BMP_WIDTH);
        mSourceBitmapHeight = mIntent.getExtras().getInt(KEY_SRC_BMP_HEIGHT);
        initViewSize();
        loadSpecFromConfig();
        mPreviousEffectBitmapPreview = new Bitmap[mGridViewRowCount * mGridViewColumCount];
        mEffectManager = new EffectManager(mSourcePath, mEffectForLoading, this);
        fillEffectNames(mEffectManager.getAllEffectsName());
        registerEffects();
    }

    private void initializeView() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setActionBar();
        mEffectView = new EffectView(mEffectNameList, this, this);
        mEffectView.setGridViewSpec(mGridViewRowCount, mGridViewColumCount);
    }

    private void saveImage(int index) {
        if (!mIsAtPreview && !mIsSaving) {
            MtkLog.d(TAG, "<saveImage> begin saving image");
            mIsSaving = true;
            showSavingProgress(null);
            mEffectManager.requestEffectBitmap(index, FancyColorHelper.TYPE_HIGH_RES_THUMBNAIL);
        }
    }

    private void showSavingProgress(String albumName) {
        DialogFragment fragment;

        if (mSavingProgressDialog != null) {
            fragment = mSavingProgressDialog.get();
            if (fragment != null) {
                if (!fragment.isAdded()) {
                    fragment.show(getFragmentManager(), null);
                }
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

    private void hideSavingProgress() {
        if (mSavingProgressDialog != null) {
            DialogFragment progress = mSavingProgressDialog.get();
            if (progress != null) {
                progress.dismissAllowingStateLoss();
            }
            MtkLog.d(TAG, "<hideSavingProgress>");
        }
    }

    private void showLoadingProgress() {
        MtkLog.d(TAG, "<showLoadingProgress>");
        DialogFragment fragment;
        if (mLoadingProgressDialog != null) {
            fragment = mLoadingProgressDialog.get();
            if (fragment != null) {
                if (!fragment.isAdded()) {
                    fragment.show(getFragmentManager(), null);
                }
                return;
            }
        }

        final DialogFragment genProgressDialog = new ProgressFragment(" ");
        genProgressDialog.setCancelable(false);
        genProgressDialog.show(getFragmentManager(), null);
        genProgressDialog.setStyle(R.style.RefocusDialog, genProgressDialog.getTheme());
        mLoadingProgressDialog = new WeakReference<DialogFragment>(genProgressDialog);
    }

    private void hideLoadingProgress() {
        if (mLoadingProgressDialog != null) {
            DialogFragment fragment = mLoadingProgressDialog.get();
            if (fragment != null) {
                fragment.dismiss();
                MtkLog.d(TAG, "<hideLoadingProgress>");
            }
        }
    }

    private void showWaitToast() {
        CharSequence text = getString(R.string.please_wait);
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void errorHandle() {
        Toast.makeText(this, getString(R.string.m_general_err_tip), Toast.LENGTH_LONG).show();
        finish();
    }

    private void fillEffectNames(ArrayList<String> effectNameList) {
        mEffectNameList = new ArrayList<String>();
        mEffectNameId = new SimpleArrayMap<String, Integer>();
        mEffectNameId.put(FancyColorHelper.EFFECT_NAME_NORMAL,
                (Integer) R.string.m_fancy_color_effect_normal);
        mEffectNameId.put(FancyColorHelper.EFFECT_NAME_MONO_CHROME,
                (Integer) R.string.m_fancy_color_effect_monochrome);
        mEffectNameId.put(FancyColorHelper.EFFECT_NAME_POSTERIZE,
                (Integer) R.string.m_fancy_color_effect_posterize);
        mEffectNameId.put(FancyColorHelper.EFFECT_NAME_RADIAL_BLUR,
                (Integer) R.string.m_fancy_color_effect_radial_blur);
        mEffectNameId.put(FancyColorHelper.EFFECT_NAME_STROKE,
                (Integer) R.string.m_fancy_color_effect_stroke);
        mEffectNameId.put(FancyColorHelper.EFFECT_NAME_SIHOUETTE,
                (Integer) R.string.m_fancy_color_effect_silhouette);
        mEffectNameId.put(FancyColorHelper.EFFECT_NAME_WHITE_BOARD,
                (Integer) R.string.m_fancy_color_effect_whiteboard);
        mEffectNameId.put(FancyColorHelper.EFFECT_NAME_BLACK_BOARD,
                (Integer) R.string.m_fancy_color_effect_blackboard);
        mEffectNameId.put(FancyColorHelper.EFFECT_NAME_NEGATIVE,
                (Integer) R.string.m_fancy_color_effect_negative);
        for (int i = 0; i < effectNameList.size(); i++) {
            int resId = (mEffectNameId.get(effectNameList.get(i))).intValue();
            String effect = getString(resId);
            mEffectNameList.add(effect);
        }
    }

    private void loadSpecFromConfig() {
        mGridViewRowCount = FancyColorHelper.getRowCount();
        mGridViewColumCount = FancyColorHelper.getColumCount();
        mEffectForLoading = FancyColorHelper.getEffectsFromConfig();
    }

    private boolean checkIntentValid() {
        mIntent = getIntent();
        if (mIntent == null || mIntent.getData() == null
                || mIntent.getExtras().getString(KEY_FILE_PATH) == null) {
            MtkLog.d(TAG, "<checkIntentValid> source error, finish");
            finish();
            return false;
        }
        return true;
    }

    private void startRefineActivity() {
        Intent intent = new Intent(STEREO_PICK_ACTION);
        intent.setClass(FancyColorActivity.this, StereoRefineActivity.class);
        intent.setDataAndType(mSourceUri, mIntent.getType()).setFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_STEREO_PICK);
    }
}
