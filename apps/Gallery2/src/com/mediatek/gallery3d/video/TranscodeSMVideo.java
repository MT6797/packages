/*
 * Copyright (C) 2014 MediaTek Inc.
 * Modification based on code covered by the mentioned copyright
 * and/or permission notice(s).
 */
/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.mediatek.gallery3d.video;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Files;
import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.common.BlobCache;
import com.android.gallery3d.util.CacheManager;

import com.mediatek.galleryframework.util.MtkLog;
import com.mediatek.storage.StorageManagerEx;

public class TranscodeSMVideo {

    private static final String TAG = "Gallery2/TranscodeSMVideo";
    private static final int TRANSCODE_COMPLETE = 3;
    private static final int TRANSCODE_ERROR = 5;
    private static final int TRANSCODE_UNSUPPORTED_VIDEO = 10;
    private static final int TRANSCODE_UNSUPPORTED_AUDIO = 11;
    public static final int TRANSCODE_FULL_STORAGE = 110;
    private static final int RESULT_CODE_OK = 0;

    private static final String FILE_URI_START = "file://";
    private static final String TEMP_FOLDER_NAMER = "/.sTempGallery/"; // transcode
                                                                       // tmp
                                                                       // folder
                                                                       // name.

    private static final String BOOKMARK_CACHE_FILE = "bookmark";
    private static final String BOOKMARK_KEY = "SlowMotion";
    private static final int BOOKMARK_CACHE_MAX_ENTRIES = 100;
    private static final int BOOKMARK_CACHE_MAX_BYTES = 10 * 1024;
    private static final int BOOKMARK_CACHE_VERSION = 1;

    private final Context mContext;
    private int mResultCode;
    private int mResultMsgResId;
    private SlowMotionItem mItem;
    private SlowMotionTranscode mTranscode;
    private Thread mTanscodeStopTask;

    private String mSrcVideoPath;
    private String mDstVideoPath;
    private String mDstVideoName;
    private String mSlowMotionInfo;

    private Uri mUri;
    private Uri mResultUri;

    private Object mWaitLock;

    private boolean mIsSingleShare = false;
    private int mTotalShareCounts;

    private int mErrorCode;

    public TranscodeSMVideo(final Context context) {
        mContext = context;
        mWaitLock = new Object();
    }

    /**
     * @Title initialize
     * @Description initialize TranscodeSMVideo, it should be invoked firstly
     * @param isSingleShare indicates single share or multi-share
     */
    public void initialize(boolean isSingleShare, int totalShareCounts) {
        MtkLog.v(TAG, "initialize, isSingleShare = " + isSingleShare + ", totalShareCounts = "
                + totalShareCounts);
        // record isSingleShare
        mIsSingleShare = isSingleShare;
        mTotalShareCounts = totalShareCounts;
        // delete temp files
        if (!mIsSingleShare) {
            deleteFileDir(getDefaultPath() + TEMP_FOLDER_NAMER);
        }
    }

    /**
     * @Title promptMessage
     * @Description toast relevent information when share wieh 1/16x video
     * @param toastInfo slow what kind inof would be toast
     */
    private void promptMessage(final int msgId) {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(mContext,
                        msgId, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * @Title startTranscode
     * @Description transcode slow motion video to 30fps
     * @param mItem current slow motiom video item speed the speed of slow
     *            motion video which going to share
     * @return Uri 30fps video uri
     */
    private void startTranscode(SlowMotionItem mItem, int speed) {
        mTranscode = new SlowMotionTranscode(mContext);
        mTranscode
                .setOnInfoListener(new SlowMotionTranscode.OnInfoListener() {
                    @Override
                    public boolean onInfo(int msg, int ext1, int ext2) {
                        if (TRANSCODE_COMPLETE == msg) {
                            if (!mIsStopTaskRunning) {
                                stopTranscodeAsync();
                            }
                        } else if (TRANSCODE_ERROR == msg
                                || TRANSCODE_UNSUPPORTED_VIDEO == msg
                                || TRANSCODE_UNSUPPORTED_AUDIO == msg
                                || TRANSCODE_FULL_STORAGE == msg) {
                            setErrorCode(msg);
                            mTranscode.stopSaveSpeedEffect();
                            synchronized (mWaitLock) {
                                mWaitLock.notifyAll();
                            }
                        }
                        return true;
                    }
                });

        /** check start time and end time, and set default value if 0 */
        int duration = mItem.getDuration();
        //Calculate total seconds.
        int totalSeconds =  (int) duration / 1000;
        //get slow motion bar time from db.
        int startTime = 0;
        int endTime = 0;
        // get slow motion section start time.
        startTime = mItem.getSectionStartTime();
        // get slow motion section end time.
        endTime = mItem.getSectionEndTime();
        // if end time is 0, the video is a new one, use default section for it.
        if (endTime == 0) {
            if (totalSeconds >= 3) {
                //if total seconds >= 3s,
                //the default section is 1/5  slow motion bar in the center.
                startTime = duration * 2 / 5;
                endTime = duration * 3 / 5;
            } else {
                //if total seconds < 3s, default section is the whole slow motion bar.
                startTime = 0;
                endTime = duration;
            }
        }

        mTranscode.setSpeedEffectParams(startTime, endTime,
                  "slow-motion-speed=" + speed);
        try {
            mTranscode.startSaveSpeedEffect(mSrcVideoPath, mDstVideoPath);
            // mState = TranscodeState.BUSY;
        } catch (IOException e) {
            e.printStackTrace();
            throw new AssertionError("startSaveSpeedEffect IOException");
        }
        // block here
        synchronized (mWaitLock) {
            try {
                MtkLog.v(TAG, "transcode before lock");
                mWaitLock.wait();
                MtkLog.v(TAG, "transcode after lock");
            } catch (InterruptedException ex) {
                throw new IllegalStateException("wait lock error");
            }
        }
    }

    /**
     * @Title transcode
     * @Description transcode slow motion video to 30fps
     * @param inUri slow motion video uri
     * @return Uri 30fps video uri
     */
    public Uri transcode(final Uri inUri) {
        MtkLog.v(TAG, "transcode inUri " + inUri);
        mUri = inUri;
        mItem = new SlowMotionItem(mContext, inUri);
        mSlowMotionInfo = mItem.getSlowMotionInfo();

        int speed = mItem.getSpeed();
        if (speed == SlowMotionItem.SLOW_MOTION_ONE_SIXTEENTH_SPEED
                || speed == SlowMotionItem.SLOW_MOTION_ONE_THIRTY_TWO_SPEED) {
            if (mIsSingleShare && mTotalShareCounts == 1) {
                MtkLog.v(TAG,
                        "only one slowmotion video will share and speed" +
                        " is 16x or 32x, show toast and return");
                promptMessage(R.string.not_avaliable_high_speed_single_share);
                return null;
            } else {
                // downgrade speed to 1/4x or 1/8x
                speed = SlowMotionItem.mMinSpeed;
                MtkLog.v(TAG, "downgrade speed to " + speed);
                promptMessage(R.string.not_avaliable_high_speed_multi_share);
            }
        }

        updateTranscodeInfo(inUri);
        setErrorCode(RESULT_CODE_OK);

        // Before start transcode, should check if the tmp file has already
        // exit.
        if (isNeedTranscode(mUri)) {
            startTranscode(mItem, speed);
        }
        MtkLog.v(TAG, "transcode mResultUri " + mResultUri);
        return mResultUri;
    }

    /**
     * @Title getErrorCode
     * @return error code
     */
    public int getErrorCode() {
        return this.mErrorCode;
    }

    public void setErrorCode(int errorCode) {
        this.mErrorCode = errorCode;
    }

    private boolean mIsStopTaskRunning = false;

    private void stopTranscodeAsync() {
        MtkLog.v(TAG, "stopTranscodeAsync");

        mTanscodeStopTask = new stopTask();
        mTanscodeStopTask.start();
        mIsStopTaskRunning = true;
    }

    private class stopTask extends Thread {
        @Override
        public void run() {
            MtkLog.v(TAG, "stopTask run");
            // stop action will take some time, do not call it in UI thread.
            mTranscode.stopSaveSpeedEffect();

            // update new file to DB.
            updateNewFileToDB();
            // when transcode complete, should save info to bookmark.
            if (mIsSingleShare) {
                setBookmark(mUri, mItem.getSlowMotionInfo(), mDstVideoPath, mResultUri);
            }

            setErrorCode(RESULT_CODE_OK);

            synchronized (mWaitLock) {
                mWaitLock.notifyAll();
            }

            mIsStopTaskRunning = false;
        }
    }

    private void updateTranscodeInfo(Uri uri) {

        mSrcVideoPath = getVideoPath(mContext, uri);
        if(mSrcVideoPath == null) {
            MtkLog.e(TAG, "updateTranscodeInfo, getVideoPath return null");
            return;
        }
        final File srcFile = new File(mSrcVideoPath);
        String defaultPath = getDefaultPath() + TEMP_FOLDER_NAMER;
        MtkLog.v(TAG, "after getDefaultPath defaultPath " + defaultPath);
        mkFileDir(defaultPath);
        mDstVideoPath = defaultPath + srcFile.getName();
        mDstVideoName = srcFile.getName();
        MtkLog.v(TAG, "updateTranscodeInfo mUri " + uri + " mDstVideoPath " + mDstVideoPath);
    }

    // private static StorageManager getStorageManager() {
    // if (sStorageManager == null) {
    // try {
    // sStorageManager = new StorageManager(null, null);
    // } catch (RemoteException e) {
    // e.printStackTrace();
    // }
    // }
    // return sStorageManager;
    // }

    private static String getDefaultPath() {

        return StorageManagerEx.getDefaultPath();
    }

    private static void mkFileDir(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            MtkLog.d(TAG, "dir not exit,will create this");
            dir.mkdirs();
        }
    }

    // delete folder and all subfiles
    private static void deleteFileDir(String path) {
        MtkLog.v(TAG, "deleteFileDir, path = " + path);
        File dir = new File(path);
        if (!dir.exists()) {
            MtkLog.d(TAG, "dir not exit, can not delete");
            return;
        }
        File subFiles[] = dir.listFiles();
        try {
            for (int i = 0; i < subFiles.length; i++) {
                if (subFiles[i].isDirectory()) {
                    deleteFileDir(path + subFiles[i].getName() + "//");
                } else {
                    subFiles[i].delete();
                }
            }
            dir.delete();
        } catch (Exception e) {

        }
    }

    // /M: for rename file from filemanager case, get absolute path from uri.@{
    private String getVideoPath(final Context context, Uri uri) {
        String videoPath = null;
        Cursor cursor = null;
        MtkLog.v(TAG, "getVideoPath(" + uri + ")");
        try {
            // query from "content://....."
            cursor = context.getContentResolver().query(uri,
                    new String[] {
                        MediaStore.Video.Media.DATA
                    }, null, null,
                    null);
            // query from "file:///......"
            if (cursor == null) {
                String data = Uri.decode(uri.toString());
                data = data.replaceAll("'", "''");
                final String where = "_data LIKE '%" + data.replaceFirst("file:///", "") + "'";
                cursor = context.getContentResolver().query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        new String[] {
                            MediaStore.Video.Media.DATA
                        }, where, null, null);
            }
            if (cursor != null && cursor.moveToFirst()) {
                videoPath = cursor.getString(0);
            }
        } catch (final SQLiteException ex) {
            ex.printStackTrace();
        } catch (IllegalArgumentException e) {
            // if this exception happen, return false.
            MtkLog.v(TAG, "ContentResolver query IllegalArgumentException");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return videoPath;
    }

    private boolean isNeedTranscode(Uri uri) {
        if (!mIsSingleShare) {
            MtkLog.v(TAG, "isNeedTranscode - !mIsSingleShare, return true");
            return true;
        }
        try {
            BlobCache cache = CacheManager.getCache(mContext,
                    BOOKMARK_CACHE_FILE, BOOKMARK_CACHE_MAX_ENTRIES,
                    BOOKMARK_CACHE_MAX_BYTES, BOOKMARK_CACHE_VERSION);
            if (cache == null) {
                MtkLog.v(
                        TAG,
                        "isNeedTranscode(" + uri + ") cache=null. hashCode()="
                                + BOOKMARK_KEY.hashCode());
                return true;
            }

            byte[] data = cache.lookup(BOOKMARK_KEY.hashCode());
            if (data == null) {
                MtkLog.v(
                        TAG,
                        "isNeedTranscode(" + uri + ") data=null. hashCode()="
                                + BOOKMARK_KEY.hashCode());
                return true;
            }

            DataInputStream dis = new DataInputStream(
                    new ByteArrayInputStream(data));

            String uriString = DataInputStream.readUTF(dis);
            String slowmotioninfo = DataInputStream.readUTF(dis);
            String dstPath = DataInputStream.readUTF(dis);
            String dstUriString = DataInputStream.readUTF(dis);
            MtkLog.v(TAG, "isNeedTranscode(" + uri + ") uriString=" + uriString
                    + ", slowmotioninfo="
                    + slowmotioninfo + ", dstPath=" + dstPath + ", dstUriString= " + dstUriString);
            MtkLog.v(TAG, "isNeedTranscode(" + uri + ") uri.toString()=" + uri.toString()
                    + ", mSlowMotionInfo="
                    + mSlowMotionInfo);

            // 1. check tmp file is exit or not.
            File tmp = new File(mDstVideoPath);
            if (!tmp.exists()) {
                deleteVideoFile(dstPath);
                deleteOldDBInfo(Uri.parse(dstUriString));
                MtkLog.v(TAG, "Dst file is not exit!");
                return true;
            }
            // 2. check current uri is recorded in bookmark or not.
            if (!uriString.equals(uri.toString())) {
                // if not equal, delete tmp file and return true.
                deleteVideoFile(dstPath);
                deleteOldDBInfo(Uri.parse(dstUriString));
                MtkLog.v(TAG, "Uri is not equal!");
                return true;
            }
            // 3. check slowmotioninfo is equal or not.
            if (!mSlowMotionInfo.equals(slowmotioninfo)) {
                // if not equal, delete tmp file and return true.
                deleteVideoFile(dstPath);
                deleteOldDBInfo(Uri.parse(dstUriString));
                MtkLog.v(TAG, "SlowMotionInfo is not equal!");
                return true;
            }
            mResultUri = Uri.parse(dstUriString);
            return false;
        } catch (IOException t) {
            MtkLog.e(TAG, "getBookmark failed", t);
        }
        return true;
    }

    private void deleteVideoFile(String fileName) {
        File f = new File(fileName);
        if (!f.delete()) {
            MtkLog.v(TAG, "Could not delete " + fileName);
        }
    }

    private void deleteOldDBInfo(Uri uri) {
        mContext.getContentResolver().delete(uri, null, null);
    }

    private void updateNewFileToDB() {
        Uri url = MediaStore.Files.getContentUri("external");

        int result = mContext.getContentResolver().delete(url,
                "_data=?",
                new String[] {
                    mDstVideoPath
                }
                );
        MtkLog.i(TAG, "updateNewFileToDB result " + result);
        final ContentValues values = new ContentValues(1);
        values.put(Files.FileColumns.DATA, mDstVideoPath);
        values.put(Files.FileColumns.DISPLAY_NAME, mDstVideoName);

        mResultUri = mContext.getContentResolver().insert(url, values);
        MtkLog.v(TAG, "updateNewFileToDB mNewVideoUri " + mResultUri);
    }

    // here should record three parameter:
    // 1. source video uri(the uri should use "content" format, if play a video
    // from file manager, should to convert to "content" format)
    // 2. current video start&end time ,speed.
    // 3. target path.
    private void setBookmark(Uri srcUri, String slowmotioninfo, String dstPath, Uri dstUri) {
        MtkLog.v(TAG, "setBookmark(" + srcUri + ", " + slowmotioninfo + ", " + dstPath + ","
                + dstUri + ")");
        try {
            BlobCache cache = CacheManager.getCache(mContext,
                    BOOKMARK_CACHE_FILE, BOOKMARK_CACHE_MAX_ENTRIES,
                    BOOKMARK_CACHE_MAX_BYTES, BOOKMARK_CACHE_VERSION);
            if (cache == null) {
                MtkLog.v(
                        TAG,
                        "setBookmark(" + srcUri + ") cache=null. hashCode()="
                                + BOOKMARK_KEY.hashCode());
                return;
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF(srcUri.toString());
            dos.writeUTF(slowmotioninfo);
            dos.writeUTF(dstPath);
            dos.writeUTF(dstUri.toString());
            dos.flush();
            cache.insert(BOOKMARK_KEY.hashCode(), bos.toByteArray());
        } catch (IOException t) {
            MtkLog.w(TAG, "setBookmark failed", t);
        }
    }
}
