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

package com.mediatek.galleryfeature.stereo.segment;

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

import com.mediatek.gallery3d.util.TraceHelper;
import com.mediatek.galleryframework.util.MtkLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;

/**
 * Helper class to save image.<br/>
 * Use getNewFile() to create a new file, and then you can save something in the
 * new file.<br/>
 * Use linkNewFileToUri() to update the new file in MediaStore database if it is
 * necessray to do so after you've done saving in the file. TODO We will use
 * this class in Segment app in the future.
 */
public class ImageSaver {
    private static final String LOGTAG = "MtkGallery2/SegmentApp/ImageSaver";

    private static final String TIME_STAMP_NAME = "_yyyyMMdd_HHmmss_SSS";
    private static final String PREFIX_PANO = "PANO";
    private static final String PREFIX_IMG = "IMG";
    private static final String POSTFIX_JPG = ".jpg";
    private static final String DEFAULT_SAVE_DIRECTORY = "EditedOnlinePhotos";
    private static final int TIME_SCALE = 1000;
    private static final int SAVE_QUALITY = 100;

    /**
     * Callback interface when query by ContentResolver.
     */
    private interface ContentResolverQueryCallback {
        void onCursorResult(Cursor cursor);
    }

    /**
     * Create a new file named by time stamp for saving image. The new file is
     * under the same parent folder to that of sourceUri.
     *
     * @param context
     *            the Android context.
     * @param sourceUri
     *            whose parent folder would be the parent folder of the new
     *            file.
     * @return new file.
     */
    public static File getNewFile(Context context, Uri sourceUri) {
        File saveDirectory = getFinalSaveDirectory(context, sourceUri);
        String filename = new SimpleDateFormat(TIME_STAMP_NAME).format(new Date(System
                .currentTimeMillis()));
        if (hasPanoPrefix(context, sourceUri)) {
            return new File(saveDirectory, PREFIX_PANO + filename + POSTFIX_JPG);
        }
        return new File(saveDirectory, PREFIX_IMG + filename + POSTFIX_JPG);
    }

    /**
     * Insert new file to MediaStore and return the related uri.
     *
     * @param context
     *            the Android context.
     * @param sourceUri
     *            whose MediaStore values would be referenced by new file. TODO
     *            maybe need removing.
     * @param file
     *            the new file.
     * @param bitmap
     *            whose width and height would be used. TODO remove this
     *            parameter.
     * @param time
     *            the time stamp. TODO remove this parameter.
     * @return uri in MediaStore.
     */
    public static Uri linkNewFileToUri(Context context, Uri sourceUri, File file, Bitmap bitmap,
            long time) {
        final ContentValues values = new ContentValues();

        time /= TIME_SCALE;
        values.put(Images.Media.TITLE, file.getName());
        values.put(Images.Media.DISPLAY_NAME, file.getName());
        values.put(Images.Media.MIME_TYPE, "image/jpeg");
        values.put(Images.Media.DATE_TAKEN, time);
        values.put(Images.Media.DATE_MODIFIED, time);
        values.put(Images.Media.DATE_ADDED, time);
        values.put(Images.Media.ORIENTATION, 0);
        values.put(Images.Media.DATA, file.getAbsolutePath());
        values.put(Images.Media.SIZE, file.length());
        values.put(Images.Media.WIDTH, bitmap.getWidth());
        values.put(Images.Media.HEIGHT, bitmap.getHeight());
        // This is a workaround to trigger the MediaProvider to re-generate the
        // thumbnail.
        values.put(Images.Media.MINI_THUMB_MAGIC, 0);
        final String[] projection = new String[] { ImageColumns.DATE_TAKEN, ImageColumns.LATITUDE,
                ImageColumns.LONGITUDE};

        querySource(context, sourceUri, projection, new ContentResolverQueryCallback() {

            @Override
            public void onCursorResult(Cursor cursor) {
                values.put(Images.Media.DATE_TAKEN, cursor.getLong(0));

                double latitude = cursor.getDouble(1);
                double longitude = cursor.getDouble(2);
                if ((latitude != 0f) || (longitude != 0f)) {
                    values.put(Images.Media.LATITUDE, latitude);
                    values.put(Images.Media.LONGITUDE, longitude);
                }
            }
        });

        Uri result = context.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
        return result;
    }

    /**
     * Save a given bitmap in a given file of a given format.
     *
     * @param target
     *            the file to save the bitmap.
     * @param toSaveBitmap
     *            the bitmap to save.
     * @param format
     *            bitmap compressing format.
     */
    public static void saveBitmapToFile(final File target, Bitmap toSaveBitmap,
            Bitmap.CompressFormat format) {
        TraceHelper.traceBegin(">>>>ImageSaver-saveBitmapToFile");
        try {
            FileOutputStream fos = new FileOutputStream(target);
            toSaveBitmap.compress(format, SAVE_QUALITY, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            MtkLog.i(LOGTAG, "<saveRefineResult> FileNotFoundException");
        } catch (IOException e) {
            MtkLog.i(LOGTAG, "<saveRefineResult> IOException");
        }
        TraceHelper.traceEnd();
    }

    private static boolean hasPanoPrefix(Context context, Uri src) {
        String name = getTrueFilename(context, src);
        return name != null && name.startsWith(PREFIX_PANO);
    }

    private static String getTrueFilename(Context context, Uri src) {
        if (context == null || src == null) {
            return null;
        }
        final String[] trueName = new String[1];
        querySource(context, src, new String[] { ImageColumns.DATA },
                new ContentResolverQueryCallback() {
                    @Override
                    public void onCursorResult(Cursor cursor) {
                        trueName[0] = new File(cursor.getString(0)).getName();
                    }
                });
        return trueName[0];
    }

    private static File getFinalSaveDirectory(Context context, Uri sourceUri) {
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

    private static File getSaveDirectory(Context context, Uri sourceUri) {
        File file = getLocalFileFromUri(context, sourceUri);
        if (file != null) {
            return file.getParentFile();
        } else {
            return null;
        }
    }

    private static File getLocalFileFromUri(Context context, Uri srcUri) {
        if (srcUri == null) {
            MtkLog.e(LOGTAG, "<getLocalFileFromUri> srcUri is null.");
            return null;
        }

        String scheme = srcUri.getScheme();
        if (scheme == null) {
            MtkLog.e(LOGTAG, "<getLocalFileFromUri> scheme is null.");
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

    private static void querySource(Context context, Uri sourceUri, String[] projection,
            ContentResolverQueryCallback callback) {
        ContentResolver contentResolver = context.getContentResolver();
        querySourceFromContentResolver(contentResolver, sourceUri, projection, callback);
    }

    private static void querySourceFromContentResolver(ContentResolver contentResolver,
            Uri sourceUri, String[] projection, ContentResolverQueryCallback callback) {
        Cursor cursor = null;
        cursor = contentResolver.query(sourceUri, projection, null, null, null);
        if ((cursor != null) && cursor.moveToNext()) {
            callback.onCursorResult(cursor);
        }
        if (cursor != null) {
            cursor.close();
        }
    }
}
