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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.ChangeNotifier;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.LocalImage;
import com.android.gallery3d.data.LocalMediaItem;
import com.android.gallery3d.data.LocalVideo;
import com.android.gallery3d.data.Log;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.MediaSetUtils;

import com.mediatek.galleryframework.base.MediaData;
import com.mediatek.galleryframework.util.MtkLog;
import com.mediatek.galleryframework.util.Utils;

import java.util.ArrayList;

/**
 * MediaSet providing images for copy in Copy & Paste feature.<br/>
 * It contains all stereo images on device, and an item to denote segmented clippings folder
 * if there's any segmented clipping available.
 */
class StereoPickingSet extends MediaSet {
    private static final String TAG = "MtkGallery2/StereoPickingSet";

    private static final int COUNT_YYYYMMDD = 8;
    private static final int COUNT_HHMMSS = 6;
    private static final String CAPTION_MATCHER_STEREO = "^IMG_[0-9]{"
            + COUNT_YYYYMMDD + "}+_[0-9]{" + COUNT_HHMMSS
            + "}+(|_[0-9]+)_STEREO(|_RAW)$";
    private static final String STREO_WHERE_CLAUSE = "(" + ImageColumns.TITLE
            + " REGEXP '" + CAPTION_MATCHER_STEREO + "')";

    private static final String[] COUNT_PROJECTION = { "count(*)" };
    private static final int INVALID_COUNT = -1;
    private static final Path ITEM_PATH = Path.fromString("/local/image/item");

    private final Path mItemPath;
    private ChangeNotifier mNotifier;
    private GalleryApp mApp;
    private String mName;
    private Uri mBaseUri;

    private int mCachedCount = INVALID_COUNT;

    public StereoPickingSet(Path path, GalleryApp application) {
        super(path, nextVersionNumber());

        MtkLog.i(TAG, "<StereoPickingSet> path:" + path);
        mApp = application;
        mName = "Pick a photo to copy";
        mBaseUri = Images.Media.EXTERNAL_CONTENT_URI;
        mItemPath = ITEM_PATH;
        mNotifier = new ChangeNotifier(this, mBaseUri, application);
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public long reload() {
        if (mNotifier.isDirty()) {
            mDataVersion = nextVersionNumber();
            mCachedCount = INVALID_COUNT;
        }
        return mDataVersion;
    }

    public int getMediaItemCount() {
        if (mCachedCount == INVALID_COUNT) {
            int ret = 0;
            ContentResolver mResolver = mApp.getContentResolver();
            Cursor cursor = null;
            // TODO use real clipping bucket id instead, and remember: never
            // cache
            int clippingBucketId = MediaSetUtils.STEREO_CLIPPINGS_BUCKET_ID;
            cursor = getClippingsCoverCursor(mApp.getAndroidContext(), clippingBucketId);
            if (cursor != null) {
                ret = 1;
                cursor.close();
            }
            try {
                cursor = mResolver
                        .query(mBaseUri, COUNT_PROJECTION, STREO_WHERE_CLAUSE, null, null);
            } catch (IllegalStateException e) {
                Log.w(TAG, "<getMediaItemCount> query IllegalStateException:" + e.getMessage());
                return ret;
            } catch (SQLiteException e) {
                Log.w(TAG, "<getMediaItemCount> query SQLiteException:" + e.getMessage());
                return ret;
            }
            if (cursor == null) {
                Log.w(TAG, "query fail");
                return ret;
            }
            try {
                Utils.assertTrue(cursor.moveToNext());
                mCachedCount = cursor.getInt(0);
                mCachedCount += ret;
            } finally {
                cursor.close();
            }
        }
        return mCachedCount;
    }

    public ArrayList<MediaItem> getMediaItem(int start, int count) {
        ArrayList<MediaItem> list = new ArrayList<MediaItem>();
        // TODO add clippings cover here
        // TODO use real clipping bucket id instead, and remember: never cache
        int clippingBucketId = MediaSetUtils.STEREO_CLIPPINGS_BUCKET_ID;
        // TODO after this invocation, there's at most 1 item in cursor (to be
        // confirmed)
        Cursor cursor = getClippingsCoverCursor(mApp.getAndroidContext(), clippingBucketId);
        if (cursor != null) {
            try {
                if (start == 0) {
                    int id = cursor.getInt(0); // _id must be in the first column
                    Path childPath = mItemPath.getChild(id);
                    MediaItem item = loadOrUpdateItem(childPath, cursor, mApp.getDataManager(),
                            mApp, true);
                    list.add(item);
                    count--; // cus clippings takes 1 account
                } else {
                    start--; // cus index 0 => clippings
                }
            } finally {
                cursor.close();
            }
        }

        cursor = getStereoImageCursor(mApp.getAndroidContext(), start, count);
        if (cursor == null) {
            MtkLog.i(TAG, "<getMediaItem> return null");
            return list;
        }

        try {
            do {
                int id = cursor.getInt(0); // _id must be in the first column
                Path childPath = mItemPath.getChild(id);
                MediaItem item = loadOrUpdateItem(childPath, cursor, mApp.getDataManager(), mApp,
                        true);
                list.add(item);
            } while (cursor.moveToNext());
        } finally {
            cursor.close();
        }
        return list;
    }

    @Override
    public boolean isLeafAlbum() {
        return true;
    }

    // TODO bucket id moved inside
    private static Cursor getClippingsCoverCursor(Context context, int bucketId) {
        String whereClause = ImageColumns.BUCKET_ID + "= ?";
        String[] whereClauseArgs = new String[] { String.valueOf(bucketId) };
        String orderClause = ImageColumns.DATE_TAKEN + " DESC, " + ImageColumns._ID + " DESC";
        Uri baseUri = Images.Media.EXTERNAL_CONTENT_URI;
        // TODO only fetch one record
        Uri uri = baseUri.buildUpon().appendQueryParameter("limit", 0 + "," + 1).build();
        Cursor cursor = MediaData.queryImage(context, uri, whereClause, whereClauseArgs,
                orderClause);
        return cursor;
    }

    private static MediaItem loadOrUpdateItem(Path path, Cursor cursor, DataManager dataManager,
            GalleryApp app, boolean isImage) {
        synchronized (DataManager.LOCK) {
            LocalMediaItem item = (LocalMediaItem) dataManager.peekMediaObject(path);
            if (item == null) {
                if (isImage) {
                    item = new LocalImage(path, app, cursor);
                } else {
                    item = new LocalVideo(path, app, cursor);
                }
            } else {
                item.updateContent(cursor);
            }
            return item;
        }
    }

    private Cursor getStereoImageCursor(Context context, int start, int count) {
        String[] whereClauseArgs = null;
        String orderClause = ImageColumns.DATE_TAKEN + " DESC, " + ImageColumns._ID + " DESC";
        Uri baseUri = Images.Media.EXTERNAL_CONTENT_URI;
        Uri uri = baseUri.buildUpon().appendQueryParameter("limit", start + "," + count).build();
        Cursor cursor = MediaData.queryImage(context, uri, STREO_WHERE_CLAUSE, whereClauseArgs,
                orderClause);
        return cursor;
    }
}
