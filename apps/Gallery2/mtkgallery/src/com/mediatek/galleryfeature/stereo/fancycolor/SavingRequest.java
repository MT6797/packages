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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;

import com.android.gallery3d.exif.ExifInterface;

import com.mediatek.gallery3d.util.TraceHelper;
import com.mediatek.galleryframework.base.Work;
import com.mediatek.galleryframework.util.MtkLog;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Helper class to encapsulate image saving work into request.
 */
class SavingRequest implements Work<Void> {
    private static final String TAG = "MtkGallery2/FancyColor/SavingRequest";
    private static final String PREFIX_IMG = "IMG";
    private static final String POSTFIX_JPG = ".jpg";
    private static final String JPEG_MIME_TYPE = "image/jpeg";
    private static final String DEFAULT_SAVE_DIRECTORY = "EditedOnlinePhotos";
    private static final String TIME_STAMP_NAME = "_yyyyMMdd_HHmmss_SSS";
    private static final int QUALITY = 100;
    private static final int TIME_SCALE = 1000;
    private int mSourceBitmapWidth;
    private int mSourceBitmapHeight;
    private Context mContext;
    private Bitmap mBitmap;
    private Uri mUri;
    private SavingRequestListener mListener;

    /**
     * Callback interface when query by ContentResolver.
     */
    public interface ContentResolverQueryCallback {
        void onCursorResult(Cursor cursor);
    }

    /**
     * Callback interface when saving done.
     */
    public interface SavingRequestListener {
        public void onSavingDone(Uri result);
    }

    public SavingRequest(Context context, Uri uri, Bitmap bitmap, int srcBmpWidth,
            int srcBmpHeight, SavingRequestListener listener) {
        mContext = context;
        mUri = uri;
        mBitmap = bitmap;
        mSourceBitmapWidth = srcBmpWidth;
        mSourceBitmapHeight = srcBmpHeight;
        mListener = listener;
    }

    @Override
    public Void run() {
        TraceHelper.traceBegin(">>>>FancyColor-SavingRequest");
        ExifInterface exif = getExifData(mUri);
        long time = System.currentTimeMillis();
        updateExifData(exif, time);
        File out = getNewFile(mContext, mUri);
        MtkLog.i(TAG, "<run> out path: " + out.getPath());
        mBitmap = resizeBitmap(mBitmap, mSourceBitmapWidth, mSourceBitmapHeight);
        putExifData(out, exif, mBitmap, QUALITY);
        time = System.currentTimeMillis();
        Uri result = linkNewFileToUri(mContext, mUri, out, time);
        updataImageDimensionInDB(mContext, out, mBitmap.getWidth(), mBitmap.getHeight());
        if (mListener != null) {
            mListener.onSavingDone(result);
        }
        TraceHelper.traceEnd();
        return null;
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    private Bitmap resizeBitmap(Bitmap src, int dstWidth, int dstHeight) {
        if (src == null || dstWidth <= 0 || dstHeight <= 0) {
            MtkLog.d(TAG, "<resizeBitmap> params error!!");
            return src;
        }
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(src, dstWidth, dstHeight, true);
        if (src != null) {
            src.recycle();
        }
        return resizedBitmap;
    }

    private File getSaveDirectory(Context context, Uri sourceUri) {
        File file = getLocalFileFromUri(context, sourceUri);
        if (file != null) {
            return file.getParentFile();
        } else {
            return null;
        }
    }

    private File getLocalFileFromUri(Context context, Uri srcUri) {
        if (srcUri == null) {
            MtkLog.i(TAG, "<getLocalFileFromUri> srcUri is null.");
            return null;
        }

        String scheme = srcUri.getScheme();
        if (scheme == null) {
            MtkLog.i(TAG, "<getLocalFileFromUri> scheme is null.");
            return null;
        }

        final File[] file = new File[1];
        // sourceUri can be a file path or a content Uri, it need to be handled
        // differently.
        if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
            if (srcUri.getAuthority().equals(MediaStore.AUTHORITY)) {
                querySource(context, srcUri, new String[] { ImageColumns.DATA },
                        new ContentResolverQueryCallback() {

                            @Override
                            public void onCursorResult(Cursor cursor) {
                                file[0] = new File(cursor.getString(0));
                            }
                        });
            }
        } else if (scheme.equals(ContentResolver.SCHEME_FILE)) {
            file[0] = new File(srcUri.getPath());
        }
        return file[0];
    }

    private File getFinalSaveDirectory(Context context, Uri sourceUri) {
        File saveDirectory = getSaveDirectory(context, sourceUri);
        if ((saveDirectory == null) || !saveDirectory.canWrite()) {
            saveDirectory = new File(Environment.getExternalStorageDirectory(),
                    DEFAULT_SAVE_DIRECTORY);
        }
        // Create the directory if it doesn't exist
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs();
        }
        return saveDirectory;
    }

    private File getNewFile(Context context, Uri sourceUri) {
        File saveDirectory = getFinalSaveDirectory(context, sourceUri);
        MtkLog.i(TAG, "<getNewFile> saveDirectory path: " + saveDirectory.getPath());
        String filename = new SimpleDateFormat(TIME_STAMP_NAME).format(new Date(System
                .currentTimeMillis()));
        MtkLog.i(TAG, "<getNewFile> filename: " + filename);
        return new File(saveDirectory, PREFIX_IMG + filename + POSTFIX_JPG);
    }

    private ExifInterface getExifData(Uri source) {
        ExifInterface exif = new ExifInterface();
        String mimeType = mContext.getContentResolver().getType(source);
        if (mimeType.equals(JPEG_MIME_TYPE)) {
            InputStream inStream = null;
            try {
                inStream = mContext.getContentResolver().openInputStream(source);
                exif.readExif(inStream);
            } catch (FileNotFoundException e) {
                MtkLog.i(TAG, "<getExifData> Cannot find file: " + source, e);
            } catch (IOException e) {
                MtkLog.i(TAG, "<getExifData> Cannot read exif for: " + source, e);
            } finally {
                closeSilently(inStream);
            }
        }
        return exif;
    }

    private void updateExifData(ExifInterface exif, long time) {
        // Set tags
        exif.addDateTimeStampTag(ExifInterface.TAG_DATE_TIME, time, TimeZone.getDefault());
        exif.setTag(exif
                .buildTag(ExifInterface.TAG_ORIENTATION, ExifInterface.Orientation.TOP_LEFT));
        // Remove old thumbnail
        exif.removeCompressedThumbnail();
    }

    private boolean putExifData(File file, ExifInterface exif,
            Bitmap image, int jpegCompressQuality) {
        boolean ret = false;
        OutputStream s = null;
        try {
            s = exif.getExifWriterStream(file.getAbsolutePath());
            image.compress(Bitmap.CompressFormat.JPEG,
                    (jpegCompressQuality > 0) ? jpegCompressQuality : 1, s);
            s.flush();
            s.close();
            s = null;
            ret = true;
        } catch (FileNotFoundException e) {
            MtkLog.i(TAG, "<putExifData> File not found: " + file.getAbsolutePath(), e);
        } catch (IOException e) {
            MtkLog.i(TAG, "<putExifData> Could not write exif: ", e);
        } finally {
            closeSilently(s);
        }
        return ret;
    }

    private void closeSilently(Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (IOException t) {
            MtkLog.w(TAG, "<closeSilently> fail to close Closeable", t);
        }
    }

    private void querySource(Context context, Uri sourceUri, String[] projection,
            ContentResolverQueryCallback callback) {
        ContentResolver contentResolver = context.getContentResolver();
        querySourceFromContentResolver(contentResolver, sourceUri, projection, callback);
    }

    private void querySourceFromContentResolver(ContentResolver contentResolver, Uri sourceUri,
            String[] projection, ContentResolverQueryCallback callback) {
        Cursor cursor = null;
        cursor = contentResolver.query(sourceUri, projection, null, null, null);
        if ((cursor != null) && cursor.moveToNext()) {
            callback.onCursorResult(cursor);
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    private Uri linkNewFileToUri(Context context, Uri sourceUri, File file, long time) {
        final ContentValues values = getContentValues(context, sourceUri, file, time);
        Uri result = context.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
        return result;
    }

    private boolean updataImageDimensionInDB(Context context, File file, int width, int height) {
        if (file == null) {
            return false;
        }
        final ContentValues values = new ContentValues();
        values.put(Images.Media.WIDTH, width);
        values.put(Images.Media.HEIGHT, height);
        values.put(Images.Media.SIZE, file.length());
        int r = context.getContentResolver().update(Images.Media.EXTERNAL_CONTENT_URI, values,
                Images.Media.DATA + "=?", new String[] { file.getAbsolutePath() });
        MtkLog.i(TAG, "<updataImageDimensionInDB> for " + file.getAbsolutePath() + ", r = " + r);
        return (r > 0);
    }

    private ContentValues getContentValues(Context context, Uri sourceUri, File file, long time) {
        final ContentValues values = new ContentValues();

        time /= TIME_SCALE;
        values.put(Images.Media.TITLE, file.getName());
        values.put(Images.Media.DISPLAY_NAME, file.getName());
        values.put(Images.Media.MIME_TYPE, JPEG_MIME_TYPE);
        values.put(Images.Media.DATE_TAKEN, time);
        values.put(Images.Media.DATE_MODIFIED, time);
        values.put(Images.Media.DATE_ADDED, time);
        values.put(Images.Media.ORIENTATION, 0);
        values.put(Images.Media.DATA, file.getAbsolutePath());
        values.put(Images.Media.SIZE, file.length());
        // This is a workaround to trigger the MediaProvider to re-generate the
        // thumbnail.
        values.put(Images.Media.MINI_THUMB_MAGIC, 0);
        final String[] projection = new String[] { ImageColumns.DATE_TAKEN, ImageColumns.LATITUDE,
                ImageColumns.LONGITUDE, };

        querySource(context, sourceUri, projection, new ContentResolverQueryCallback() {

            @Override
            public void onCursorResult(Cursor cursor) {
                values.put(Images.Media.DATE_TAKEN, cursor.getLong(0));

                double latitude = cursor.getDouble(1);
                double longitude = cursor.getDouble(2);
                // TODO: Change || to && after the default location
                // issue is fixed.
                if ((latitude != 0f) || (longitude != 0f)) {
                    values.put(Images.Media.LATITUDE, latitude);
                    values.put(Images.Media.LONGITUDE, longitude);
                }
            }
        });
        return values;
    }
}
