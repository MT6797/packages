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

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.gallery3d.R;

import com.mediatek.galleryframework.util.MtkLog;

import java.util.ArrayList;

/**
 * View to display fancy color effect.
 */
class EffectView implements AdapterView.OnItemClickListener {
    private final static String TAG = "MtkGallery2/FancyColor/EffectView";
    private final static int ITEM_VIEW_PADDING = 2;
    private final static int VERTICAL_SPACE = 9;
    private final static int HORIZONTAL_SPACE = 3;
    private int mColumnCount;
    private int mRowCount;
    private int mEffectCount;

    private int mBitmapWidth;
    private int mBitmapHeight;
    private int mDisplayWidthWoGap;
    private int mDisplayHeightWoGap;
    private int mGridViewWidth;
    private int mGridViewHeight;
    private int mItemViewWidth;
    private int mItemViewHeight;

    private GridView mGridView;
    private ThumbView mThumbView;
    private FancyColorActivity mContext;
    private ViewHandle[] mViewHandles;
    private ArrayList<String> mEffectNameList;
    private LayoutListener mLayoutListener;
    private ItemClickListener mItemClickListener;

    /**
     * Layout listener.
     */
    public interface LayoutListener {
        public void onGridViewReady(int index);

        public void onThumbViewReady(int index);
    }

    /**
     * Item click listener.
     */
    public interface ItemClickListener {
        public void onItemClick(int position);
    }

    public EffectView(ArrayList<String> effectNameList, LayoutListener listener,
            ItemClickListener clickListener) {
        mLayoutListener = listener;
        mItemClickListener = clickListener;
        mEffectNameList = effectNameList;
    }

    public void init(FancyColorActivity context, int width, int height) {
        mContext = context;
        mBitmapWidth = width;
        mBitmapHeight = height;
        context.setContentView(R.layout.m_fancy_color_preview);
        setGridView(true);
    }

    public void init(FancyColorActivity context, int index) {
        if (mContext == null) {
            mContext = context;
        }
        ((Activity) mContext).setContentView(R.layout.m_fancy_color);
        mThumbView = (ThumbView) context.findViewById(R.id.thumbview);
        mThumbView.setThumbViewClickListener(context);
        if (mLayoutListener != null) {
            mLayoutListener.onThumbViewReady(index);
        }
    }

    public void setGridViewSpec(int rowCount, int columCount) {
        mColumnCount = columCount;
        mRowCount = rowCount;
        mEffectCount = mColumnCount * mRowCount;
        mViewHandles = new ViewHandle[mEffectCount];
    }

    public void updateView(boolean isAtPreview, int index, Bitmap bitmap) {
        if (bitmap == null) {
            MtkLog.d(TAG, "<updateView> error, index " + index + ", bitmap is null");
            return;
        }
        ImageView view = null;
        if (isAtPreview) {
            ViewHandle handle = mViewHandles[index];
            if (handle == null) {
                MtkLog.d(TAG, "<updateView> error, index " + index + ", handle is null");
                return;
            }
            view = handle.mItemView;
        } else {
            view = mThumbView;
        }
        if (view != null) {
            MtkLog.d(TAG, "<updateView> setBitmap, index" + index + ", bitmap " + bitmap);
            view.setImageBitmap(bitmap);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mItemClickListener != null) {
            mItemClickListener.onItemClick(position);
        }
    }

    /**
     * Wrapper class for index and image item.
     */
    public class ViewHandle {
        int mIndex;
        ImageView mItemView;

        public ViewHandle(int index, ImageView view) {
            mIndex = index;
            mItemView = view;
        }
    }

    public void onOrientationChange() {
        setGridView(false);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mItemViewWidth,
                mItemViewHeight);
        ImageView view = null;
        for (int i = 0; i < mEffectCount; i++) {
            if (mViewHandles[i] != null) {
                view = mViewHandles[i].mItemView;
            }
            if (view != null) {
                view.setLayoutParams(params);
            }
        }
    }

    private void setGridView(boolean resetAdapter) {
        if (mContext == null) {
            MtkLog.d(TAG, "<setGridView> mContext is null, return ");
            return;
        }
        WindowManager wm = (WindowManager) ((Activity) mContext)
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        MtkLog.d(TAG, "<setGridView> device width " + metrics.widthPixels + " height "
                + metrics.heightPixels);
        mDisplayHeightWoGap = metrics.heightPixels - (mRowCount - 1) * VERTICAL_SPACE;
        mDisplayWidthWoGap = metrics.widthPixels - (mColumnCount - 1) * HORIZONTAL_SPACE;
        calcGridViewSize();
        if (resetAdapter) {
            mGridView = (GridView) mContext.findViewById(R.id.fancy_color_preview_gridview);
            mGridView.setAdapter(new ViewAdapter(mContext));
            mGridView.setOnItemClickListener(this);
        }
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mGridViewWidth,
                mGridViewHeight);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        mGridView.setLayoutParams(params);
        mGridView.setVerticalSpacing(VERTICAL_SPACE);
        mGridView.setHorizontalSpacing(HORIZONTAL_SPACE);
        mGridView.setNumColumns(mColumnCount);
    }

    private void setViewHandle(int index, ImageView view) {
        if (view == null || index < 0 || index > mEffectCount - 1) {
            MtkLog.d(TAG, "<setViewHandle> error!");
            return;
        }
        ViewHandle handle = new ViewHandle(index, view);
        mViewHandles[index] = handle;
        if (mLayoutListener != null) {
            mLayoutListener.onGridViewReady(index);
        }
        MtkLog.d(TAG, "<setViewHandle> index " + index + ", imageview " + view);
    }

    private void calcGridViewSize() {
        float scaledDisplayHeight = (float) mDisplayWidthWoGap * (float) mBitmapHeight * mRowCount
                / (float) (mBitmapWidth * mColumnCount);
        float scaledDisplayWidth = (float) mDisplayHeightWoGap * (float) mBitmapWidth
                * mColumnCount / (float) (mBitmapHeight * mRowCount);
        float ratioHeight = scaledDisplayHeight / (float) mDisplayHeightWoGap;
        float ratioWidth = scaledDisplayWidth / (float) mDisplayWidthWoGap;
        if (ratioHeight > ratioWidth) {
            // scale width
            mGridViewWidth = (int) scaledDisplayWidth + (mColumnCount - 1) * HORIZONTAL_SPACE;
            // keep height original size
            mGridViewHeight = mDisplayHeightWoGap + (mRowCount - 1) * VERTICAL_SPACE;
            mItemViewWidth = (int) (scaledDisplayWidth / (float) mColumnCount);
            mItemViewHeight = (int) ((float) mDisplayHeightWoGap / (float) mRowCount);
        } else {
            // keep width original size
            mGridViewWidth = mDisplayWidthWoGap + (mColumnCount - 1) * HORIZONTAL_SPACE;
            // scale height
            mGridViewHeight = (int) scaledDisplayHeight + (mRowCount - 1) * VERTICAL_SPACE;
            mItemViewWidth = (int) ((float) mDisplayWidthWoGap / (float) mColumnCount);
            mItemViewHeight = (int) (scaledDisplayHeight / (float) mRowCount);
        }
    }

    /**
     * Adapter between effect datas and thubmanil grid view.
     */
    private class ViewAdapter extends BaseAdapter {
        private LayoutInflater mLayoutInflater;
        private boolean mHasSetPosition0 = false;

        public ViewAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mEffectCount;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MtkLog.d(TAG, "<getView> convertView:" + convertView + ", position:" + position);
            ViewHolder holder = null;
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.m_effect_item, null);
                holder = new ViewHolder();
                holder.mItemView = (ImageView) convertView.findViewById(R.id.itemview);
                if (position != 0) {
                    setViewHandle(position, holder.mItemView);
                } else {
                    /*
                     * for positon0, it will callback more than once just save
                     * 1st viewhandle
                     */
                    if (!mHasSetPosition0) {
                        setViewHandle(position, holder.mItemView);
                        mHasSetPosition0 = true;
                    }
                }
                holder.mTextView = (TextView) convertView.findViewById(R.id.effects_name);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        mItemViewWidth, mItemViewHeight);
                holder.mItemView.setLayoutParams(params);
                holder.mPosition = position;
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.mTextView.setText(mEffectNameList.get(position));
            return convertView;
        }
    }

    /**
     * Wrapper data structure for on slot on the grid view.
     */
    private class ViewHolder {
        ImageView mItemView;
        TextView mTextView;
        int mPosition;
    }
}