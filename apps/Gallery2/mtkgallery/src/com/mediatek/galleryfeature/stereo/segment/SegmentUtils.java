package com.mediatek.galleryfeature.stereo.segment;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;

import com.mediatek.galleryframework.util.MtkLog;

/**
 * Utility Class in Segment feature.
 */
public class SegmentUtils {
    private static final String LOGTAG = "MtkGallery2/SegmentApp/SegmentUtils";

    public static final int ROTATE_90 = 90;
    public static final int ROTATE_180 = 180;
    public static final int ROTATE_270 = 270;

    private static final int COUNT_YYYYMMDD = 8;
    private static final int COUNT_HHMMSS = 6;
    private static final String CAPTION_MATCHER_STEREO = "^IMG_[0-9]{"
            + COUNT_YYYYMMDD + "}+_[0-9]{" + COUNT_HHMMSS
            + "}+(|_[0-9]+)_STEREO(|_RAW)$";

    // bitmap to pass from its sender to its receiver
    private static Bitmap sBitmapCommunication;

    /**
     * Take an orientation and a bitmap, and return the bitmap transformed to
     * that orientation.
     *
     * @param bitmap
     *            the Bitamp.
     * @param ori
     *            the orientation.
     * @return the transformed relsult Bitmap.
     */
    public static Bitmap orientBitmap(Bitmap bitmap, int ori) {
        Matrix matrix = new Matrix();
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (ori == ExifInterface.ORIENTATION_ROTATE_90
                || ori == ExifInterface.ORIENTATION_ROTATE_270
                || ori == ExifInterface.ORIENTATION_TRANSPOSE
                || ori == ExifInterface.ORIENTATION_TRANSVERSE) {
            int tmp = w;
            w = h;
            h = tmp;
        }
        switch (ori) {
        case ExifInterface.ORIENTATION_ROTATE_90:
            matrix.setRotate(ROTATE_90, w / 2f, h / 2f);
            break;
        case ExifInterface.ORIENTATION_ROTATE_180:
            matrix.setRotate(ROTATE_180, w / 2f, h / 2f);
            break;
        case ExifInterface.ORIENTATION_ROTATE_270:
            matrix.setRotate(ROTATE_270, w / 2f, h / 2f);
            break;
        case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
            matrix.preScale(-1, 1);
            break;
        case ExifInterface.ORIENTATION_FLIP_VERTICAL:
            matrix.preScale(1, -1);
            break;
        case ExifInterface.ORIENTATION_TRANSPOSE:
            matrix.setRotate(ROTATE_90, w / 2f, h / 2f);
            matrix.preScale(1, -1);
            break;
        case ExifInterface.ORIENTATION_TRANSVERSE:
            matrix.setRotate(ROTATE_270, w / 2f, h / 2f);
            matrix.preScale(1, -1);
            break;
        case ExifInterface.ORIENTATION_NORMAL:
        default:
            return bitmap;
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix,
                true);
    }

    /**
     * Resize bitmap by scale factor.
     *
     * @param bitmap
     *            the original bitmap.
     * @param scale
     *            the desired scale factor.
     * @param recycle
     *            should original bitmap be recycled when we get the scaled one.
     * @return the new scaled bitmap.
     */
    public static Bitmap resizeBitmapByScale(Bitmap bitmap, float scale, boolean recycle) {
        int width = Math.round(bitmap.getWidth() * scale);
        int height = Math.round(bitmap.getHeight() * scale);
        // fix certain wbmp no thumbnail issue.@{
        if (width < 1 || height < 1) {
            MtkLog.i(LOGTAG, "<resizeBitmapByScale> scaled width or height < 1, no need to resize");
            return bitmap;
        }
        if (width == bitmap.getWidth() && height == bitmap.getHeight()) {
            return bitmap;
        }
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            config = Bitmap.Config.ARGB_8888;
        }
        Bitmap target = Bitmap.createBitmap(width, height, config);
        Canvas canvas = new Canvas(target);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        if (recycle) {
            bitmap.recycle();
        }
        return target;
    }

    /**
     * Judge if a string looks like a title of a depth image.
     * A title of a depth image would match the pattern:
     * ^IMG_[0-9]{8}+_[0-9]{6}+_STEREO$
     *
     * @param title
     *            the string to judge.
     * @return true if the string looks like a tile of a depth image.
     */
    public static boolean isDepthImageTitlePattern(String title) {
        return title.matches(CAPTION_MATCHER_STEREO);
    }

    /**
     * set Communication bitmap from sender.
     * @param bmp the bitmap to send.
     */
    public static void setCommunicationBitmap(Bitmap bmp) {
        sBitmapCommunication = bmp;
    }

    /**
     * get Communication bitmap from receiver.
     * @return the bitmap set by sender.
     */
    public static Bitmap getCommunicationBitmap() {
        Bitmap bitmap = sBitmapCommunication;
        sBitmapCommunication = null;
        return bitmap;
    }
}
