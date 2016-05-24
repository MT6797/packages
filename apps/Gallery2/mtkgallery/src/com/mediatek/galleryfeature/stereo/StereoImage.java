package com.mediatek.galleryfeature.stereo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import com.mediatek.xmp.XmpOperator;

/**
 * decode stereo image.
 *
 */
public class StereoImage {
    private static final String TAG = "StereoImage";

    /**
     * <1>get JPEG from app1(XMP). <2>if app1 contain JPEG, decode app1 JPEG,
     * els decode main image.
     *
     * @param filePath
     *            source file path
     * @param sampSize
     *            decode sample size
     * @return success->bitmap, fail->null
     */
    public static Bitmap decodeStereoImage(String filePath, int sampSize) {
        Log.d(TAG, "<decodeStereoImage> filePath:" + filePath + ",sampSize:" + sampSize);
        byte[] jpegBuf = null;
        synchronized (RefocusImageJni.SLOCK) {
            final XmpOperator xmp = new XmpOperator();
            if ((filePath == null) || !xmp.initialize(filePath)) {
                Log.d(TAG, "<decodeStereoImage> xmp.initialize fail!!");
                return null;
            }
            jpegBuf = xmp.getClearImage(filePath);
        }
        if (jpegBuf == null) {
            return decodeJpeg(filePath, sampSize);
        } else {
            return decodeJpeg(jpegBuf, sampSize);
        }
    }

    /**
     * get clear image from XMP.
     * @param filePath source file path
     * @return success->clear image, fail->null
     */
    public static byte[] getClearImage(String filePath) {
        synchronized (RefocusImageJni.SLOCK) {
            final XmpOperator xmp = new XmpOperator();
            if (!xmp.initialize(filePath)) {
                Log.d(TAG, "<<getClearImage> xmp.initialize fail!!");
                return null;
            }
            return xmp.getClearImage(filePath);
        }
    }

    public static Bitmap decodeJpeg(String path, int sampSize) {
        synchronized (RefocusImageJni.SLOCK) {
            Log.d(TAG, "<decodeJpeg> path:" + path + ",sampSize:" + sampSize);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);
            options.inSampleSize = sampSize;
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeFile(path, options);
        }
    }
    /**
     * resize image to target width and height
     *
     * @param bitmap
     *            original bitmap
     * @param outWidth
     *            target width
     * @param outHeight
     *            target height
     * @return output bitmap
     */
    public static Bitmap resizeImage(Bitmap bitmap, int outWidth, int outHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scaleWidth = (float) outWidth/width;
        float scaleHeight = (float) outHeight/height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    private static Bitmap decodeJpeg(byte[] data, int sampSize) {
        Log.d(TAG, "<decodeJpeg> data:" + data + ",sampSize:" + sampSize);
        if (data == null) {
            Log.d(TAG, "null jpeg buffer");
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        options.inSampleSize = sampSize;
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }
}
