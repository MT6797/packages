/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;

import com.mediatek.galleryfeature.stereo.RefocusImageJni;
import com.mediatek.galleryfeature.stereo.StereoImage;
import com.mediatek.galleryframework.util.MtkLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.text.SimpleDateFormat;

/**
 * Tool class for refocus.
 */
public class RefocusHelper {
    private static final String TAG = "Gallery2/Refocus/RefocusHelper";
    private static final String DEFAULT_SAVE_DIRECTORY = "RefocusLocalImages";
    private static final String TIME_STAMP_NAME = "_yyyyMMdd_HHmmss";
    private static final String PREFIX_IMG = "IMG";
    private static final String POSTFIX_JPG = ".jpg";
    private static final int MILLISEC_PER_SEC = 1000;
    private static final int DEFAULT_IMAGE_WIDTH = 1080;
    private static final int DEFAULT_IMAGE_HEIGHT = 1920;

    /**
     * Content resolver query callback.
     */
    private interface ContentResolverQueryCallback {
        void onCursorResult(Cursor cursor);
    }

    /**
     * Get abstract file path for URI.
     * @param context The context, through which it can do DB operation.
     * @param uri The relative uri of the image.
     * @return The file path.
     */
    public static String getRealFilePathFromURI(Context context, Uri uri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = null;
        String filePath = null;

        cursor = context.getContentResolver().query(uri, proj, null, null, null);
        if (cursor == null) {
            return null;
        }
        int colummIndex = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        filePath = cursor.getString(colummIndex);
        MtkLog.d(TAG, "getImageRealPathFromURI colummIndex= " + filePath);

        if (cursor != null) {
            cursor.close();
        }
        return filePath;
    }

    /**
     * Insert the information to DB.
     * @param context The context, through which it can do DB operation.
     * @param sourceUri The source uri.
     * @param file The file object.
     * @param saveFileName The file name.
     * @return DB uri.
     */
    public static Uri insertContent(Context context, Uri sourceUri, File file,
            String saveFileName) {
        long now = System.currentTimeMillis() / MILLISEC_PER_SEC;

        final ContentValues values = new ContentValues();
        values.put(Images.Media.TITLE, saveFileName);
        values.put(Images.Media.DISPLAY_NAME, file.getName());
        values.put(Images.Media.MIME_TYPE, "image/jpeg");
        values.put(Images.Media.DATE_TAKEN, now);
        values.put(Images.Media.DATE_MODIFIED, now);
        values.put(Images.Media.DATE_ADDED, now);
        values.put(Images.Media.ORIENTATION, 0);
        values.put(Images.Media.DATA, file.getAbsolutePath());
        values.put(Images.Media.SIZE, file.length());
        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            int imageLength = exif.getAttributeInt(
                    ExifInterface.TAG_IMAGE_LENGTH, 0);
            int imageWidth = exif.getAttributeInt(
                    ExifInterface.TAG_IMAGE_WIDTH, 0);
            values.put(Images.Media.WIDTH, imageWidth);
            values.put(Images.Media.HEIGHT, imageLength);
        } catch (IOException ex) {
            MtkLog.w(TAG, "ExifInterface throws IOException", ex);
        }

        final String[] projection = new String[] { ImageColumns.DATE_TAKEN,
                ImageColumns.LATITUDE, ImageColumns.LONGITUDE, };
        querySource(context, sourceUri, projection,
                new ContentResolverQueryCallback() {

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
        Uri insertUri = context.getContentResolver().insert(
                Images.Media.EXTERNAL_CONTENT_URI, values);
        MtkLog.d(TAG, "insertUri = " + insertUri);
        return insertUri;
    }

    /**
     * Update content.
     *
     * @param context
     *            context
     * @param sourceUri
     *            source uri
     * @param file
     *            output file
     * @return source uri
     */
    public static Uri updateContent(Context context, Uri sourceUri, File file) {
        long now = System.currentTimeMillis() / MILLISEC_PER_SEC;
        final ContentValues values = new ContentValues();
        values.put(Images.Media.DATE_MODIFIED, now);
        values.put(Images.Media.DATE_ADDED, now);
        values.put(Images.Media.SIZE, file.length());
        context.getContentResolver().update(sourceUri, values, null, null);
        return sourceUri;
    }

    private static void querySource(Context context, Uri sourceUri,
            String[] projection, ContentResolverQueryCallback callback) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = null;

        cursor = contentResolver.query(sourceUri, projection, null, null,
                null);
        if ((cursor != null) && cursor.moveToNext()) {
            callback.onCursorResult(cursor);
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    /**
     * get new file.
     * @param context The context, through which it can do DB operation.
     * @param sourceUri The source uri for new File.
     * @param saveFileName the new file name.
     * @return The File object is created by the new file.
     */
    public static File getNewFile(Context context, Uri sourceUri,
            String saveFileName) {
        File saveDirectory = getFinalSaveDirectory(context, sourceUri);
        return new File(saveDirectory, saveFileName + ".JPG");
    }

    /**
     * Get final save directory.
     * @param context The context, through which it can do DB operation.
     * @param sourceUri The source uri for save directory.
     * @return The save directory File object.
     */
    public static File getFinalSaveDirectory(Context context, Uri sourceUri) {
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

    /**
     * Get file path with uri.
     *
     * @param context
     *            context
     * @param sourceUri
     *            uri
     * @return file path
     */
    public static String getFilePathFromUri(Context context, Uri sourceUri) {
        final String[] path = new String[1];
        querySource(context, sourceUri, new String[] { ImageColumns.DATA },
                new ContentResolverQueryCallback() {
                    @Override
                    public void onCursorResult(Cursor cursor) {
                        path[0] = cursor.getString(0);
                    }
                });
        return path[0];
    }

    private static File getSaveDirectory(Context context, Uri sourceUri) {
        final File[] dir = new File[1];
        querySource(context, sourceUri, new String[] { ImageColumns.DATA },
                new ContentResolverQueryCallback() {
                    @Override
                    public void onCursorResult(Cursor cursor) {
                        dir[0] = new File(cursor.getString(0)).getParentFile();
                    }
                });
        return dir[0];
    }

    /**
     * Decode Bitmap.
     * @param uri The bitmap file uri.
     * @param context The context, through which it can do DB operation.
     * @return The bitmap decoded from the uri.
     */
    public static Bitmap decodeBitmap(Uri uri, Context context) {
        MtkLog.d(TAG, "uri = " + uri);
        Bitmap bitmap = null;
        if (uri == null || context == null) {
            throw new IllegalArgumentException("bad argument to loadBitmap");
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = RefocusImageJni.INSAMPLESIZE;
        options.inScaled = true;
        InputStream is = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            MtkLog.e(TAG, "file not found! is = " + is, e);
        }
        bitmap = BitmapFactory.decodeStream(is, null, options);
        try {
            if (is != null) {
                is.close();
            }
        } catch (IOException ex) {
            MtkLog.e(TAG, "exception is = " + ex);
        }
        return bitmap;
    }

    /**
     * decode stereo image.
     *
     * @param filePath
     *            source file path
     * @return decode file
     */
    public static Bitmap decodeBitmap(String filePath) {
        if (filePath == null) {
            MtkLog.d(TAG, "<decodeBitmap> params error");
            return null;
        }
        Bitmap bitmap = StereoImage.decodeJpeg(filePath, 1);
        if (bitmap.getWidth() > bitmap.getHeight()) {
            return StereoImage.resizeImage(bitmap, DEFAULT_IMAGE_HEIGHT, DEFAULT_IMAGE_WIDTH);
        } else {
            return StereoImage.resizeImage(bitmap, DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT);
        }
    }

    /**
     * Get new file from the uri.
     * @param context The context, through which it can do DB operation.
     * @param sourceUri the file uri.
     * @return The new File object.
     */
    public static File getNewFile(Context context, Uri sourceUri) {
        File saveDirectory = getFinalSaveDirectory(context, sourceUri);
        String filename = new SimpleDateFormat(TIME_STAMP_NAME)
                .format(new Date(System.currentTimeMillis()));
        return new File(saveDirectory, PREFIX_IMG + filename + POSTFIX_JPG);
    }
}
