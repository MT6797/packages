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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.mediatek.galleryframework.util.MtkLog;

import java.util.ArrayList;

/**
 * Adapter class between data (db) and background thumbnail list
 * (BackgroundThumbTrack).
 */
class BackgroundThumbAdapter extends ArrayAdapter<BackgroundThumbAction> {
    private static final String LOGTAG = "MtkGallery2/SegmentApp/BackgroundThumbAdapter";
    private static final String BACKGROUND_THUMBNAIL_DB = Environment.getExternalStorageDirectory()
            .getPath() + "/mybg.db";
    private static final int MAX_THUMB_NUM = 20;
    private static final int DENSITY_SCALE = 100;

    private ArrayList<BackgroundThumbAction> mActions = new ArrayList<BackgroundThumbAction>();
    private int mItemHeight;
    private View mContainer;
    private int mItemWidth = ListView.LayoutParams.MATCH_PARENT;
    private int mSelectedPosition;
    private int mOrientation;

    public BackgroundThumbAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        mItemHeight = (int) (context.getResources().getDisplayMetrics().density * DENSITY_SCALE);
    }

    public BackgroundThumbAdapter(Context context) {
        this(context, 0);
    }

    public void loadActions(StereoBackgroundActivity activity, int verticalItemHeight) {
        MtkLog.d(LOGTAG, "<loadActions>" + BACKGROUND_THUMBNAIL_DB);
        clear();
        setItemHeight(verticalItemHeight);
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(BACKGROUND_THUMBNAIL_DB, null);
        Cursor cursor = null;

        try {
            cursor = db.rawQuery("select * from recent_bg limit 0, " + MAX_THUMB_NUM, null);
        } catch (SQLiteException se) {
            db.execSQL("create table recent_bg (_id integer primary key autoincrement"
                    + ", img_path varchar(255))");
            cursor = db.rawQuery("select * from recent_bg limit 0, " + MAX_THUMB_NUM, null);
        }

        ArrayList<String> imgs = new ArrayList<String>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                imgs.add(cursor.getString(1));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        BackgroundThumbAction action = new BackgroundThumbAction(activity,
                BackgroundThumbAction.FULL_VIEW);
        action.setOriginal(true);
        add(action);

        for (int i = 0; i < imgs.size(); i++) {
            action = new BackgroundThumbAction(activity, BackgroundThumbAction.FULL_VIEW);
            action.setOriginal(false);
            action.setUri(imgs.get(i));
            add(action);
        }

        add(new BackgroundThumbAction(activity, BackgroundThumbAction.ADD_ACTION));
    }

    public void saveActions() {
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(BACKGROUND_THUMBNAIL_DB, null);

        try {
            db.execSQL("delete from recent_bg");
            for (int i = 1; i < mActions.size() - 1; i++) {
                db.execSQL("insert into recent_bg values (null, ?)", new String[] { mActions.get(i)
                        .getUri() });
            }
        } catch (SQLiteException se) {
            MtkLog.d(LOGTAG, "<saveActions> db exception");
        }
        db.close();
    }

    public void addFront(BackgroundThumbAction action) {
        mSelectedPosition = 1;
        String imgPath = action.getUri();
        for (int i = 1; i < mActions.size() - 1; i++) {
            if (mActions.get(i).getUri().equals(imgPath)) {
                BackgroundThumbAction act = mActions.get(i);
                mActions.remove(act);
                mActions.add(1, act);
                super.clear();
                super.addAll(mActions);
                return;
            }
        }

        ArrayList<BackgroundThumbAction> actions = new ArrayList<BackgroundThumbAction>();
        actions.add(mActions.get(0)); // add original
        actions.add(action);
        if (mActions.size() > 2) {
            // add old bgs
            actions.addAll(mActions.subList(1, Math.min(MAX_THUMB_NUM, mActions.size() - 1)));
        }
        for (int i = MAX_THUMB_NUM; i < actions.size() - 1; i++) {
            // never remove gallery
            BackgroundThumbAction act = mActions.get(i);
            act.clearBitmap();
        }
        actions.add(mActions.get(mActions.size() - 1)); // add gallery
        mActions = actions;
        super.clear();
        super.addAll(mActions);
    }

    public void setItemHeight(int height) {
        mItemHeight = height;
    }

    public void setItemWidth(int width) {
        mItemWidth = width;
    }

    public void setSelected(View v) {
        int old = mSelectedPosition;
        mSelectedPosition = (Integer) v.getTag();
        if (old != -1) {
            invalidateView(old);
        }
        invalidateView(mSelectedPosition);
    }

    public boolean isSelected(View v) {
        return (Integer) v.getTag() == mSelectedPosition;
    }

    public void setContainer(View container) {
        mContainer = container;
    }

    public void imageLoaded() {
        notifyDataSetChanged();
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    @Override
    public void add(BackgroundThumbAction action) {
        super.add(action);
        mActions.add(action);
    }

    public void remove(BackgroundThumbAction action) {
        super.remove(action);
        MtkLog.d(LOGTAG, "<remove> " + action.getUri());
        action.clearBitmap();
        mActions.remove(action);
    }

    @Override
    public void clear() {
        for (int i = 0; i < getCount(); i++) {
            BackgroundThumbAction action = getItem(i);
            action.clearBitmap();
        }
        super.clear();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = new BackgroundThumbView(getContext());
        }
        BackgroundThumbView view = (BackgroundThumbView) convertView;
        view.setOrientation(mOrientation);
        BackgroundThumbAction action = getItem(position);
        view.setAction(action, this);
        int width = mItemWidth;
        int height = mItemHeight;
        if (action.getType() == BackgroundThumbAction.ADD_ACTION
                && mOrientation == BackgroundThumbView.VERTICAL) {
            height = height / 2;
        }
        view.setLayoutParams(new ListView.LayoutParams(width, height));
        view.setTag(position);
        view.invalidate();
        return view;
    }

    private void invalidateView(int position) {
        View child = null;
        if (mContainer instanceof ListView) {
            ListView lv = (ListView) mContainer;
            child = lv.getChildAt(position - lv.getFirstVisiblePosition());
        } else {
            BackgroundThumbTrack ct = (BackgroundThumbTrack) mContainer;
            child = ct.getChildAt(position);
        }
        if (child != null) {
            child.invalidate();
        }
    }
}
