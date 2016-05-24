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
package com.mediatek.galleryframework.base;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.android.gallery3d.R;
import com.mediatek.galleryfeature.stereo.fancycolor.FancyColorActivity;
import com.mediatek.galleryfeature.stereo.refocus.RefocusActivity;
import com.mediatek.galleryframework.base.LayerManager.IBackwardContoller.IOnActivityResultListener;
import com.mediatek.galleryframework.gl.MGLView;
import com.mediatek.galleryframework.util.MtkLog;

/**
 * Displays an bottom icon(contains image and string) and response for ClickEvent, which has
 * the same animation with google bottom view. Such as Stereo icon.
 */
public class BottomControlLayer extends Layer {
    private final static String TAG = "MtkGallery2/BottomControlLayer";
    // added for Image refocus.
    public static final int REQUEST_REFOCUS = 8;
    public static final int REQUEST_FANCY_COLOR = 10;
    public static final int REQUEST_BACKGROUND = 11;
    public static final int REQUEST_COPY_PAST = 12;
    protected Activity mActivity;
    protected MediaData mMediaData;
    private PhotoPageBottomViewControls mPhotopageBottomViewControls;
    private IOnActivityResultListener mResultListener;

    @Override
    public void onCreate(Activity activity, final ViewGroup root) {
        MtkLog.d(TAG, "<onCreate> onCreate this=" + this);
        mActivity = activity;
        mPhotopageBottomViewControls = new PhotoPageBottomViewControls(
                activity, root, this);
        mResultListener = new IOnActivityResultListener() {
            @Override
            public boolean onActivityResult(int requestCode, int resultCode,
                    Intent data) {
                if (resultCode == Activity.RESULT_OK) {
                    mPhotopageBottomViewControls
                            .inflateLayer(R.layout.m_start_stereo);
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    mPhotopageBottomViewControls
                            .inflateLayer(R.layout.m_stereo);
                }
                return false;
            }
        };
    }

    @Override
    public void onResume(boolean isFilmMode) {
        fresh(false);
    }

    @Override
    public void setData(MediaData data) {
        mMediaData = data;
        mPhotopageBottomViewControls.setData(data);
    }

    @Override
    public void setBackwardController(
            LayerManager.IBackwardContoller backwardControllerForLayer) {
        super.setBackwardController(backwardControllerForLayer);
        mPhotopageBottomViewControls
                .setBackwardContoller(backwardControllerForLayer);
    }

    /**
     * Response for click event.
     * @param v the view that response for.
     */
    public void onClickEvent(View v) {
        int id = v.getId();
        switch (id) {
        case R.id.m_stereo_refocus:
            launchRefocusActivity();
            break;
        case R.id.m_stereo_copy_paste:
            launchCopyAndPasteActivity();
            break;
        case R.id.m_stereo_background:
            launchBackgroundSubstituteActivity();
            break;
        case R.id.m_stereo_fancy_color:
            launchFancyColorActivity();
            break;
        case R.id.m_stereo_start_menu:
        case R.id.m_stereo_touch_menu:
            mActivity.invalidateOptionsMenu();
            break;
        default:
            throw new AssertionError();
        }
    }

    @Override
    public boolean fresh(boolean visible, boolean hasAnimation) {
        return mPhotopageBottomViewControls.fresh(visible, hasAnimation);
    }

    @Override
    public boolean fresh(boolean hasAnimation) {
        return mPhotopageBottomViewControls.fresh(
                mBackwardContoller.getBottomMenuVisibility(), hasAnimation);
    }

    @Override
    public boolean onBackPressed() {
        boolean rollback = mPhotopageBottomViewControls.onBackPressed();
        if (mPhotopageBottomViewControls.onStereoStartView()) {
            mActivity.invalidateOptionsMenu();
        }
        return rollback;
    }

    @Override
    public boolean onUpPressed() {
        return onBackPressed();
    }
    @Override
    public boolean onSingleTapUp(float x, float y) {
        MtkLog.w(TAG, "<onSingleTapUp> ");
        return false;
    }

    @Override
    public boolean onDoubleTap(float x, float y) {
        return mPhotopageBottomViewControls.onSinglePhotoMode();
    }

    /**
     * Check whether on stereo view.
     * @return true if on stereo view, false otherwise.
     */
    public boolean onStereoView() {
        return mPhotopageBottomViewControls.onStereoView();
    }

    /**
     * Check whether on stereo start view.
     * @return true if on stereo start view, false otherwise.
     */
    public boolean onStereoStartView() {
        return mPhotopageBottomViewControls.onStereoStartView();
    }

    /**
     * Check whether on stereo start view.
     * @return return true if on stereo start view, false otherwise.
     */
    public boolean onStereoTouchView() {
        return mPhotopageBottomViewControls.onStereoTouchView();
    }

    @Override
    public boolean onScroll(float dx, float dy, float totalX, float totalY) {
        return mPhotopageBottomViewControls.onSinglePhotoMode();
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
            float velocityY) {
        return mPhotopageBottomViewControls.onSinglePhotoMode();
    }

    @Override
    public boolean onScaleBegin(float focusX, float focusY) {
        return mPhotopageBottomViewControls.onSinglePhotoMode();
    }

    @Override
    public boolean onScale(float focusX, float focusY, float scale) {
        return mPhotopageBottomViewControls.onSinglePhotoMode();
    }

    @Override
    public void onActivityPause() {
        mPhotopageBottomViewControls.hide();
    }

    @Override
    public void onPause() {
        fresh(false, false);
    }

    /**
     * Launch FancyColorActivity.
     */
    public void launchFancyColorActivity() {
        MtkLog.i(TAG, "<launchFancyColorActivity> ");
        Activity activity = mActivity;
        Intent intent = new Intent(FancyColorActivity.FANCY_COLOR_ACTION);
        intent.setClass(activity, FancyColorActivity.class);
        if (mMediaData == null) {
            return;
        }
        Uri baseUri = Images.Media.EXTERNAL_CONTENT_URI;
        Uri uri = baseUri.buildUpon().appendPath(String.valueOf(mMediaData.id))
                .build();
        intent.setDataAndType(uri, mMediaData.mimeType);
        Bundle bundle = new Bundle();
        bundle.putString(FancyColorActivity.KEY_FILE_PATH, mMediaData.filePath);
        bundle.putInt(FancyColorActivity.KEY_SRC_BMP_WIDTH, mMediaData.width);
        bundle.putInt(FancyColorActivity.KEY_SRC_BMP_HEIGHT, mMediaData.height);
        intent.putExtras(bundle);
        if (mBackwardContoller != null) {
            mBackwardContoller.startActivityForResult(intent,
                    REQUEST_FANCY_COLOR, mResultListener);
        }
    }

    /**
     * Launch RefocusActivity.
     */
    public void launchRefocusActivity() {
        MtkLog.i(TAG, "<launchRefocusActivity> mMediaData:" + mMediaData);
        if (mMediaData == null) {
            return;
        }
        Activity activity = mActivity;
        Intent intent = new Intent(RefocusActivity.REFOCUS_ACTION);
        intent.setClass(activity, RefocusActivity.class);
        Uri baseUri = Images.Media.EXTERNAL_CONTENT_URI;
        Uri uri = baseUri.buildUpon().appendPath(String.valueOf(mMediaData.id))
                .build();
        intent.setDataAndType(uri, mMediaData.mimeType);
        Bundle bundle = new Bundle();
        MtkLog.i(TAG, "<launchRefocusActivity> REFOCUS_IMAGE_WIDTH = "
                + mMediaData.width + ", REFOCUS_IMAGE_HEIGHT = "
                + mMediaData.height);
        bundle.putInt(RefocusActivity.REFOCUS_IMAGE_WIDTH, mMediaData.width);
        bundle.putInt(RefocusActivity.REFOCUS_IMAGE_HEIGHT, mMediaData.height);
        bundle.putInt(RefocusActivity.REFOCUS_IMAGE_ORIENTATION,
                mMediaData.orientation);
        bundle.putString(RefocusActivity.REFOCUS_IMAGE_NAME, mMediaData.caption);
        bundle.putString(RefocusActivity.REFOCUS_IMAGE_PATH, mMediaData.filePath);
        intent.putExtras(bundle);
        if (mBackwardContoller != null) {
            mBackwardContoller.startActivityForResult(intent, REQUEST_REFOCUS,
                    mResultListener);
        }
    }

    /**
     * Launch CopyAndPasteActivity.
     */
    public void launchCopyAndPasteActivity() {
        MtkLog.i(TAG, "<launchCopyAndPasteActivity> mMediaData:" + mMediaData);
        if (mMediaData == null) {
            return;
        }
        // go to stereo refine activity
        Uri baseUri = Images.Media.EXTERNAL_CONTENT_URI;
        Uri uri = baseUri.buildUpon().appendPath(String.valueOf(mMediaData.id))
                .build();
        Intent intent = new Intent();
        intent.putExtra("COPY_DEST", uri.toString());
        if (mBackwardContoller != null) {
            mBackwardContoller.startActivityForResult(intent,
                    REQUEST_COPY_PAST, mResultListener);
        }
    }

    /**
     * Launch BackgroundSubstituteActivity.
     */
    public void launchBackgroundSubstituteActivity() {
        MtkLog.i(TAG, "<launchBackgroundSubstituteActivity> mMediaData"
                + mMediaData);
        if (mMediaData == null) {
            return;
        }
        Intent intent = new Intent("action_stereo_bgsubs");
        Uri baseUri = Images.Media.EXTERNAL_CONTENT_URI;
        Uri uri = baseUri.buildUpon().appendPath(String.valueOf(mMediaData.id))
                .build();
        intent.setDataAndType(uri, mMediaData.mimeType).setFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (mBackwardContoller != null) {
            mBackwardContoller.startActivityForResult(intent,
                    REQUEST_BACKGROUND, mResultListener);
        }
    }

    /**
     * Get visibility of the view.
     * @param id The view id.
     * @return true if visible, false otherwise.
     */
    public boolean getVisibility(int id) {
        return getVisibility(id, true);
    }

    /**
     * Get visibility of the view.
     * @param id the view id.
     * @param visiable the default visibility of the view.
     * @return true if visible, false otherwise.
     */
    public boolean getVisibility(int id, boolean visiable) {
        switch (id) {
        case R.layout.m_start_stereo:
            return supportStereo() && visiable;
        case R.id.m_stereo_touch_menu:
            return supportTouchFreeview() && visiable;
        case R.id.m_stereo_start_menu:
            return visiable;
        case R.layout.m_stereo:
            return true;
        default:
            return true;
        }
    }

    /**
     * Whether support Gyro-sensorfor freeview. Subclass should override this
     * method.
     * @return false for this layer.
     */
    public boolean supportGyroSensor() {
        return false;
    }

    /**
     * Whether support Touch event for freeview. Subclass should override this
     * method.
     * @return false for this layer.
     */
    public boolean supportTouchFreeview() {
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MtkLog.d(TAG, "<onCreateOptionsMenu> menu = " + menu);
        if (onStereoView() || onStereoTouchView()) {
            menu.clear();
        }
        return true;
    }

    @Override
    public MGLView getMGLView() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public View getView() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onDestroy() {
        mPhotopageBottomViewControls.destory();
    }

    @Override
    public void setPlayer(Player player) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onChange(Player player, int what, int arg, Object obj) {
        // TODO Auto-generated method stub

    }

    protected boolean supportStereo() {
        if (mMediaData != null) {
            return ("image/jpeg".equals(mMediaData.mimeType));
        } else {
            return false;
        }
    }
}
