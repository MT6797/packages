/**
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.gallery3d.common;

import android.app.ActivityManager;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemProperties;
import android.util.Log;
import android.widget.Toast;

import com.android.launcher3.R;
import com.android.photos.BitmapRegionTileSource;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class BitmapCropTask extends AsyncTask<Void, Void, Boolean> {

    public interface OnBitmapCroppedHandler {
        public void onBitmapCropped(byte[] imageBytes);
    }

    private static final int DEFAULT_COMPRESS_QUALITY = 90;
    private static final String LOGTAG = "BitmapCropTask";

    Uri mInUri = null;
    Context mContext;
    String mInFilePath;
    byte[] mInImageBytes;
    int mInResId = 0;
    RectF mCropBounds = null;
    int mOutWidth, mOutHeight;
    int mRotation;
    boolean mSetWallpaper;
    boolean mSaveCroppedBitmap;
    Bitmap mCroppedBitmap;
    Runnable mOnEndRunnable;
    Resources mResources;
    BitmapCropTask.OnBitmapCroppedHandler mOnBitmapCroppedHandler;
    boolean mNoCrop;

    //M. ALPS01885181, sync with gallery, check the image size.
    private final static String MIME_GIF = "image/gif";
    private final static String MIME_BMP = "image/bmp";
    private final static String MIME_JPEG = "image/jpeg";
    // GIF: None LCA:20MB, LCA:10MB
    private final static int MAX_GIF_FILE_SIZE_NONE_LCA = 20 * 1024 * 1024;
    private final static int MAX_GIF_FILE_SIZE_LCA = 10 * 1024 * 1024;

    private final static long MAX_GIF_FRAME_PIXEL_SIZE = (long) (1.5f * 1024 * 1024); // 1.5MB

    // BMP & WBMP: NONE-LCA file size < 52MB, LCA file size < 6MB
    private final static int MAX_BMP_FILE_SIZE_NONE_LCA = 52 * 1024 * 1024;
    private final static int MAX_BMP_FILE_SIZE_LCA = 6 * 1024 * 1024;

    // JPGE: Height < 8192, Width < 8192
    private final static int MAX_JPEG_DECODE_LENGTH = 8192;
    /// M.

    ///M.
    int mCropWidth;
    int mCropHeight;
    static final int WALLPAPER_CROP_REGION_SIZE_LIMIT = 2048;

    private static final boolean mIsOmaDrmSupport =
          SystemProperties.getInt("ro.mtk_oma_drm_support", 0) == 1;
    ///M.

    public BitmapCropTask(Context c, String filePath,
            RectF cropBounds, int rotation, int outWidth, int outHeight,
            boolean setWallpaper, boolean saveCroppedBitmap, Runnable onEndRunnable) {
        mContext = c;
        mInFilePath = filePath;
        init(cropBounds, rotation,
                outWidth, outHeight, setWallpaper, saveCroppedBitmap, onEndRunnable);
    }

    public BitmapCropTask(byte[] imageBytes,
            RectF cropBounds, int rotation, int outWidth, int outHeight,
            boolean setWallpaper, boolean saveCroppedBitmap, Runnable onEndRunnable) {
        mInImageBytes = imageBytes;
        init(cropBounds, rotation,
                outWidth, outHeight, setWallpaper, saveCroppedBitmap, onEndRunnable);
    }

    public BitmapCropTask(Context c, Uri inUri,
            RectF cropBounds, int rotation, int outWidth, int outHeight,
            boolean setWallpaper, boolean saveCroppedBitmap, Runnable onEndRunnable) {
        mContext = c;
        mInUri = inUri;
        init(cropBounds, rotation,
                outWidth, outHeight, setWallpaper, saveCroppedBitmap, onEndRunnable);
    }

    public BitmapCropTask(Context c, Resources res, int inResId,
            RectF cropBounds, int rotation, int outWidth, int outHeight,
            boolean setWallpaper, boolean saveCroppedBitmap, Runnable onEndRunnable) {
        mContext = c;
        mInResId = inResId;
        mResources = res;
        init(cropBounds, rotation,
                outWidth, outHeight, setWallpaper, saveCroppedBitmap, onEndRunnable);
    }

    private void init(RectF cropBounds, int rotation, int outWidth, int outHeight,
            boolean setWallpaper, boolean saveCroppedBitmap, Runnable onEndRunnable) {
        mCropBounds = cropBounds;
        mRotation = rotation;
        mOutWidth = outWidth;
        mOutHeight = outHeight;
        mSetWallpaper = setWallpaper;
        mSaveCroppedBitmap = saveCroppedBitmap;
        mOnEndRunnable = onEndRunnable;
        ///M.
        mCropWidth = 0;
        mCropHeight = 0;
        ///M.
    }

    public void setOnBitmapCropped(BitmapCropTask.OnBitmapCroppedHandler handler) {
        mOnBitmapCroppedHandler = handler;
    }

    public void setNoCrop(boolean value) {
        mNoCrop = value;
    }

    public void setOnEndRunnable(Runnable onEndRunnable) {
        mOnEndRunnable = onEndRunnable;
    }

    ///M.
    public void setCropSize(int width, int height) {
        mCropWidth = width;
        mCropHeight = height;
    }
    ///M.

    // Helper to setup input stream
    private InputStream regenerateInputStream() {
        if (mInUri == null && mInResId == 0 && mInFilePath == null && mInImageBytes == null) {
            Log.w(LOGTAG, "cannot read original file, no input URI, resource ID, or " +
                    "image byte array given");
        } else {
            try {
                if (mInUri != null) {
                    // M: DRM file
                    if (mIsOmaDrmSupport && BitmapRegionTileSource
                            .isDrmFormat(mContext, mInUri)) {
                        String filePath = BitmapRegionTileSource
                                .getDrmFilePath(mContext, mInUri);
                        if (filePath != null) {
                            byte[] buffer = BitmapRegionTileSource
                                    .forceDecryptFile(filePath, false);
                            Bitmap bitmap = null;
                            if (buffer != null) {
                                bitmap = BitmapFactory.decodeByteArray(buffer,
                                    0, buffer.length, null);
                            } else {
                                Log.w(LOGTAG, "buffer is null");
                            }

                            if (bitmap != null) {
                                return BitmapRegionTileSource.getByteArrayInputStream(bitmap);
                            } else {
                                Log.w(LOGTAG, "bitmap is null");
                                return new BufferedInputStream(
                                    mContext.getContentResolver().openInputStream(mInUri));
                            }
                        } else {
                            Log.w(LOGTAG, "file path is null");
                        }
                    } else {
                        return new BufferedInputStream(
                                mContext.getContentResolver().openInputStream(mInUri));
                    }
                } else if (mInFilePath != null) {
                    return mContext.openFileInput(mInFilePath);
                } else if (mInImageBytes != null) {
                    return new BufferedInputStream(new ByteArrayInputStream(mInImageBytes));
                } else {
                    return new BufferedInputStream(mResources.openRawResource(mInResId));
                }
            } catch (FileNotFoundException e) {
                Log.w(LOGTAG, "cannot read file: " + mInUri.toString(), e);
            } catch (SecurityException e) {
                Log.w(LOGTAG, "security exception: " + mInUri.toString(), e);
            }
        }
        return null;
    }

    public Point getImageBounds() {
        InputStream is = regenerateInputStream();
        if (is != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            Utils.closeSilently(is);
            if (options.outWidth != 0 && options.outHeight != 0) {
                return new Point(options.outWidth, options.outHeight);
            }
        }
        return null;
    }

    public void setCropBounds(RectF cropBounds) {
        mCropBounds = cropBounds;
    }

    /// M.
    public int computeSampleSize(int scale) {
        return scale <= 8 ? Utils.prevPowerOf2(scale) : scale / 8 * 8;
    }

    public Bitmap getCroppedBitmap() {
        return mCroppedBitmap;
    }
    public boolean cropBitmap() {
        boolean failure = false;

        //M. ALPS01885181, sync with gallery, check the image size.
        if (isOutOfSpecLimit()) {
            Log.i(LOGTAG, "cropBitmap,image out of spec limit, mInUri:" + mInUri);
            return failure;
        }
        ///M.

        WallpaperManager wallpaperManager = null;
        if (mSetWallpaper) {
            wallpaperManager = WallpaperManager.getInstance(mContext.getApplicationContext());
        }

        if (mSetWallpaper && mNoCrop) {
            try {
                InputStream is = regenerateInputStream();
                if (is != null) {
                    wallpaperManager.setStream(is);
                    Utils.closeSilently(is);
                }
            } catch (IOException e) {
                Log.w(LOGTAG, "cannot write stream to wallpaper", e);
                failure = true;
            }
            return !failure;
        } else {
            // Find crop bounds (scaled to original image size)
            Rect roundedTrueCrop = new Rect();
            Matrix rotateMatrix = new Matrix();
            Matrix inverseRotateMatrix = new Matrix();

            Point bounds = getImageBounds();
            if (mRotation > 0) {
                rotateMatrix.setRotate(mRotation);
                inverseRotateMatrix.setRotate(-mRotation);

                mCropBounds.roundOut(roundedTrueCrop);
                mCropBounds = new RectF(roundedTrueCrop);

                if (bounds == null) {
                    Log.w(LOGTAG, "cannot get bounds for image");
                    failure = true;
                    return false;
                }

                float[] rotatedBounds = new float[] { bounds.x, bounds.y };
                rotateMatrix.mapPoints(rotatedBounds);
                rotatedBounds[0] = Math.abs(rotatedBounds[0]);
                rotatedBounds[1] = Math.abs(rotatedBounds[1]);

                mCropBounds.offset(-rotatedBounds[0]/2, -rotatedBounds[1]/2);
                inverseRotateMatrix.mapRect(mCropBounds);
                mCropBounds.offset(bounds.x/2, bounds.y/2);

            }

            mCropBounds.roundOut(roundedTrueCrop);

            if (roundedTrueCrop.width() <= 0 || roundedTrueCrop.height() <= 0) {
                Log.w(LOGTAG, "crop has bad values for full size image");
                failure = true;
                return false;
            }

            ///M. ALPS02021242, mOutWidth and mOutHeight can't be zero.
            if (mOutWidth <= 0) {
                Log.w(LOGTAG, "mOutWidth is zero, mOutWidth:" + mOutWidth);
                mOutWidth = 1;
            }

            if (mOutHeight <= 0) {
                Log.w(LOGTAG, "mOutHeight is zero, mOutHeight:" + mOutHeight);
                mOutHeight = 1;
            }
            ///M.

            // See how much we're reducing the size of the image
            int scaleDownSampleSize = Math.max(1, Math.min(roundedTrueCrop.width() / mOutWidth,
                    roundedTrueCrop.height() / mOutHeight));
            // Attempt to open a region decoder
            BitmapRegionDecoder decoder = null;
            InputStream is = null;
            Bitmap crop = null;
            if (roundedTrueCrop.width() * roundedTrueCrop.height() <=
                    WALLPAPER_CROP_REGION_SIZE_LIMIT * WALLPAPER_CROP_REGION_SIZE_LIMIT) {
                try {
                    is = regenerateInputStream();
                    if (is == null) {
                        Log.w(LOGTAG, "cannot get input stream for uri=" + mInUri.toString());
                        failure = true;
                        return false;
                    }
                    decoder = BitmapRegionDecoder.newInstance(is, false);
                    Utils.closeSilently(is);
                } catch (IOException e) {
                    Log.w(LOGTAG, "cannot open region decoder for file: " + mInUri.toString(), e);
                } finally {
                   Utils.closeSilently(is);
                   is = null;
                }

                if (decoder != null) {
                    // Do region decoding to get crop bitmap
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    if (scaleDownSampleSize > 1) {
                        options.inSampleSize = scaleDownSampleSize;
                    }
                    crop = decoder.decodeRegion(roundedTrueCrop, options);
                    decoder.recycle();
                }
            } else {
                Log.w(LOGTAG, "crop region is too large: " + roundedTrueCrop);
            }

            if (crop == null) {
                // BitmapRegionDecoder has failed, try to crop in-memory
                is = regenerateInputStream();
                Bitmap fullSize = null;
                if (is != null) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    if (scaleDownSampleSize > 1) {
                             /// M: it need to be power of 2
                            scaleDownSampleSize = computeSampleSize(scaleDownSampleSize);
                        options.inSampleSize = scaleDownSampleSize;
                    }

                        /// M. for large size image.
                        try {
                    fullSize = BitmapFactory.decodeStream(is, null, options);
                        } catch (OutOfMemoryError e) {
                            Log.w(LOGTAG, "Can't decode large size image");
                            failure = true;
                            return false;
                        }
                        ///M.

                    Utils.closeSilently(is);
                }

                if (fullSize != null) {
                    // Find out the true sample size that was used by the decoder
                    scaleDownSampleSize = bounds.x / fullSize.getWidth();
                        /**M: The scaleDownSampleSize SHOULD NOT be 0.@{**/
                        if (scaleDownSampleSize == 0) {
                            scaleDownSampleSize = 1;
                        }
                        /**@}**/
                    mCropBounds.left /= scaleDownSampleSize;
                    mCropBounds.top /= scaleDownSampleSize;
                    mCropBounds.bottom /= scaleDownSampleSize;
                    mCropBounds.right /= scaleDownSampleSize;
                    mCropBounds.roundOut(roundedTrueCrop);

                    if (roundedTrueCrop.left < 0) {
                        roundedTrueCrop.left = 0;
                    }
                    if (roundedTrueCrop.top < 0) {
                        roundedTrueCrop.top = 0;
                    }

                    /**M: removed the wrong solution from Google and replaced
                                    with below MTK solution. ALPS01669050. @{**/
                    /*
                    // Adjust values to account for issues related to rounding
                    if (roundedTrueCrop.width() > fullSize.getWidth()) {
                        // Adjust the width
                        roundedTrueCrop.right = roundedTrueCrop.left + fullSize.getWidth();
                    }
                    if (roundedTrueCrop.right > fullSize.getWidth()) {
                        // Adjust the left value
                        int adjustment = roundedTrueCrop.left -
                                Math.max(0, roundedTrueCrop.right - roundedTrueCrop.width());
                        roundedTrueCrop.left -= adjustment;
                        roundedTrueCrop.right -= adjustment;
                    }
                    if (roundedTrueCrop.height() > fullSize.getHeight()) {
                        // Adjust the height
                        roundedTrueCrop.bottom = roundedTrueCrop.top + fullSize.getHeight();
                    }
                    if (roundedTrueCrop.bottom > fullSize.getHeight()) {
                        // Adjust the top value
                        int adjustment = roundedTrueCrop.top -
                                Math.max(0, roundedTrueCrop.bottom - roundedTrueCrop.height());
                        roundedTrueCrop.top -= adjustment;
                        roundedTrueCrop.bottom -= adjustment;
                    }*/
                        /**@}**/

                    /**M: added to resolve the issue
                                 ALPS01669050.@{**/
                    if (roundedTrueCrop.left + roundedTrueCrop.width() > fullSize.getWidth()) {
                        roundedTrueCrop.right -= roundedTrueCrop.left
                            + roundedTrueCrop.width() - fullSize.getWidth();
                    }
                    if (roundedTrueCrop.top + roundedTrueCrop.height() > fullSize.getHeight()) {
                        roundedTrueCrop.bottom -= roundedTrueCrop.top
                            + roundedTrueCrop.height() - fullSize.getHeight();
                    }
                    /**@}**/

                    ///M: ALPS02203708, check the width and height.
                    if (roundedTrueCrop.width() <= 0 || roundedTrueCrop.height() <= 0) {
                        Log.w(LOGTAG, "width or height is below 0. width: "
                            + roundedTrueCrop.width() + ",height:"
                            + roundedTrueCrop.height());
                        failure = true;
                        return false;
                    }
                    ///M.

                    crop = Bitmap.createBitmap(fullSize, roundedTrueCrop.left,
                            roundedTrueCrop.top, roundedTrueCrop.width(),
                            roundedTrueCrop.height());
                }
            }

            if (crop == null) {
                Log.w(LOGTAG, "cannot decode file: " + mInUri.toString());
                failure = true;
                return false;
            }
            if (mOutWidth > 0 && mOutHeight > 0 || mRotation > 0) {
                float[] dimsAfter = new float[] { crop.getWidth(), crop.getHeight() };
                rotateMatrix.mapPoints(dimsAfter);
                dimsAfter[0] = Math.abs(dimsAfter[0]);
                dimsAfter[1] = Math.abs(dimsAfter[1]);

                if (!(mOutWidth > 0 && mOutHeight > 0)) {
                    mOutWidth = Math.round(dimsAfter[0]);
                    mOutHeight = Math.round(dimsAfter[1]);
                }

                RectF cropRect = new RectF(0, 0, dimsAfter[0], dimsAfter[1]);
                RectF returnRect = new RectF(0, 0, mOutWidth, mOutHeight);

                Matrix m = new Matrix();
                if (mRotation == 0) {
                    m.setRectToRect(cropRect, returnRect, Matrix.ScaleToFit.FILL);
                } else {
                    Matrix m1 = new Matrix();
                    m1.setTranslate(-crop.getWidth() / 2f, -crop.getHeight() / 2f);
                    Matrix m2 = new Matrix();
                    m2.setRotate(mRotation);
                    Matrix m3 = new Matrix();
                    m3.setTranslate(dimsAfter[0] / 2f, dimsAfter[1] / 2f);
                    Matrix m4 = new Matrix();
                    m4.setRectToRect(cropRect, returnRect, Matrix.ScaleToFit.FILL);

                    Matrix c1 = new Matrix();
                    c1.setConcat(m2, m1);
                    Matrix c2 = new Matrix();
                    c2.setConcat(m4, m3);
                    m.setConcat(c2, c1);
                }

                try {
                    Bitmap tmp = Bitmap.createBitmap((int) returnRect.width(),
                        (int) returnRect.height(), Bitmap.Config.ARGB_8888);
                    if (tmp != null) {
                        Canvas c = new Canvas(tmp);
                        Paint p = new Paint();
                        p.setFilterBitmap(true);
                        c.drawBitmap(crop, m, p);
                        crop = tmp;
                    }
                } catch (OutOfMemoryError e) {
                    Log.w(LOGTAG, "Can't create large bitmap, width = " + returnRect.width()
                            + ", height = " + returnRect.height());
                    failure = true;
                    return false;
                }
            }

            if (mSaveCroppedBitmap) {
                mCroppedBitmap = crop;
            }

            // Compress to byte array
            ByteArrayOutputStream tmpOut = new ByteArrayOutputStream(2048);
            if (crop.compress(CompressFormat.JPEG, DEFAULT_COMPRESS_QUALITY, tmpOut)) {
                // If we need to set to the wallpaper, set it
                if (mSetWallpaper && wallpaperManager != null) {
                    try {
                        byte[] outByteArray = tmpOut.toByteArray();
                        wallpaperManager.setStream(new ByteArrayInputStream(outByteArray));
                        if (mOnBitmapCroppedHandler != null) {
                            mOnBitmapCroppedHandler.onBitmapCropped(outByteArray);
                        }
                    } catch (IOException e) {
                        Log.w(LOGTAG, "cannot write stream to wallpaper", e);
                        failure = true;
                    }
                }
            } else {
                Log.w(LOGTAG, "cannot compress bitmap");
                failure = true;
            }
        }
        return !failure; // True if any of the operations failed
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        return cropBitmap();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (mOnEndRunnable != null) {
            mOnEndRunnable.run();
        }
        Log.w(LOGTAG, "onPostExecute:result:" + result);
        if (!result) {
            Toast.makeText(mContext, mContext.getString(R.string.wallpaper_load_fail),
                  Toast.LENGTH_LONG).show();
        }
    }

    //M. ALPS01885181, sync with gallery, check the image size.
    public boolean isOutOfSpecLimit() {
        InputStream inputStream = regenerateInputStream();
        if (inputStream == null) {
            return true;
        }

        // get file size
        int fileSize;
        try {
             fileSize = inputStream.available();
        } catch (IOException e) {
             Utils.closeSilently(inputStream);
             return true;
        }

        // get MimeType & width & height
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, option);
        String mimeType = option.outMimeType;
        long imageWidth = (long) option.outWidth;
        long imageHeight = (long) option.outHeight;
        long framePixelSize = (long) option.outWidth * (long) option.outHeight;

        if (option.outWidth == -1 || option.outHeight == -1) {
            BitmapRegionDecoder decoder = null;
            try {
                 decoder = BitmapRegionDecoder.newInstance(inputStream, false);
            } catch (IOException e) {
                 Log.w(LOGTAG, "BitmapRegionDecoder failed", e);
                 return true;
            }

            if (decoder != null) {
                imageWidth = (long) decoder.getWidth();
                imageHeight = (long) decoder.getHeight();
                framePixelSize = imageWidth * imageHeight;
            }
        }

        boolean isLcaDevice;
        if (mContext != null) {
            isLcaDevice = ((ActivityManager) mContext
                .getSystemService("activity")).isLowRamDevice();
        } else {
            isLcaDevice = false;
        }
        Log.w(LOGTAG, "isOutOfSpecLimit:isLcaDevice:" + isLcaDevice
                + ", mContext:" + mContext);

        Utils.closeSilently(inputStream);
        if (mimeType != null) {
            if (mimeType.equals(MIME_GIF)) {
                int maxGifFileSize = isLcaDevice ?
                      MAX_GIF_FILE_SIZE_LCA : MAX_GIF_FILE_SIZE_NONE_LCA;
                if (fileSize > maxGifFileSize || framePixelSize > MAX_GIF_FRAME_PIXEL_SIZE) {
                    return true;
                }
            } else if (mimeType.equals(MIME_BMP)) {
                int maxBmpFileSize = isLcaDevice ?
                      MAX_BMP_FILE_SIZE_LCA : MAX_BMP_FILE_SIZE_NONE_LCA;
                if (fileSize > maxBmpFileSize) {
                    return true;
                }
            }
        }
        return false;
    }
}
