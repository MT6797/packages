package com.mediatek.galleryfeature.pq;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemProperties;
import android.view.MenuItem;

import com.android.gallery3d.R;
import com.mediatek.galleryfeature.config.FeatureConfig;
import com.mediatek.galleryframework.util.MtkLog;
import com.mediatek.pq.PictureQuality;
import com.mediatek.pq.PictureQuality.Hist;

import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * Calculate and save the image Histogram for ImageDC effect.
 */
public class ImageDC {
    private static final String TAG = "MtkGallery2/ImageDC";
    private static final String DC = "com.android.gallery3d.ImageDC";
    private static final String DCNAME = "ImageDC";
    private static boolean sAvailable = (SystemProperties.get("DC").equals("1"));
    private static HashMap<String, int[]> sHistogramHashMap
        = new HashMap<String, int[]>();
    private static final int SIZE_ARGB = 4;
    private String mFilePath;
    private int mOrientation;
    private String mMimeType = "";
    private int[] mHistogram;

    /**
     * Constructor.
     * @param filePath the abstract path of the image.
     * @param orientation the orientation of the image.
     * @param mimeType the mimeType of the image.
     */
    public ImageDC(String filePath, int orientation, String mimeType) {
        mFilePath = filePath;
        mOrientation = orientation;
        mMimeType = mimeType;
    }

    /**
     * Check if need histogram.
     * @return whether need histogram or not.
     */
    public boolean isNeedHistogram() {
        return ("image/jpeg".equals(mMimeType) && sAvailable);
    }

    public boolean isNeedToGetThumbFromCache() {
        return true && !("image/jpeg".equals(mMimeType) && sAvailable);
    }

    /**
     * Check if has histogram.
     * @return whether has histogram or not.
     */
    public boolean hasHistorgram() {
        int[] hist = sHistogramHashMap.get(mFilePath);
        if (hist != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get histogram of the item.
     * @return the histogram.
     */
    public int[] getHist() {
        MtkLog.d(TAG, "<getHist> mFilePath=" + mFilePath);
        return sHistogramHashMap.get(mFilePath);
    }

    /**
     * Generate histogram.
     * @param bitmap the resize bitmap for calculate histogram.
     * @return whether success calculate histogram or not.
     */
    public boolean generateHistogram(Bitmap bitmap) {
        if (bitmap == null) {
            return false;
        }
        MtkLog.d(TAG,
                " <generateHistogram> bitmap w="
                        + bitmap.getWidth() + " height=" + bitmap.getHeight());
        int length = bitmap.getWidth() * bitmap.getHeight() * SIZE_ARGB;
        if (!FeatureConfig.SUPPORT_IMAGE_DC_ENHANCE) {
            return false;
        } else {
            int[] histogram = sHistogramHashMap.get(mFilePath);
            if (histogram == null) {
                byte[] array = null;
                ByteBuffer buffer = ByteBuffer.allocate(length);
                bitmap.copyPixelsToBuffer(buffer);
                array = buffer.array();
                boolean result = generateHistogram(array, bitmap.getWidth(),
                        bitmap.getHeight(), mFilePath);
                if (buffer != null) {
                    buffer.clear();
                }
                return result;
            } else {
                return true;
            }
        }
    }

    private boolean generateHistogram(byte[] array, int width, int height,
            String filePath) {
        long begin = System.currentTimeMillis();
        MtkLog.d(TAG,
                " <generateHistogram (, , )> get Histogram :mMediaData.filePath="
                        + filePath);

        Hist mHist = PictureQuality.getDynamicContrastHistogram(array, width,
                height);
        if (mHist != null) {
            int[] histogram = mHist.info;
            sHistogramHashMap.put(filePath, histogram);
            int lenght = histogram.length;
            for (int i = 0; i < lenght; i++) {
                MtkLog.d(TAG, "<generateHistogram> histogram[" + i + "]="
                        + histogram[i]);
            }
            MtkLog.d(TAG, " <generateHistogram> get Histogram use Time = "
                    + (System.currentTimeMillis() - begin));
            return true;
        } else {
            return false;
        }
    }

    /**
     * Add Histogram flag for imageDC effect.
     * @param option add histogram flag to the option.
     */
    public void addFlag(BitmapFactory.Options option) {
        option.inDynmicConFlag = true;
        option.inDynamicCon = getHist();
    }

    /**
     * Clear histogram flag.
     * @param option remove histogram flag from the option.
     */
    public void clearFlag(BitmapFactory.Options option) {
        option.inDynmicConFlag = false;
    }

    /**
     * Get the histogram by filePath.
     * @param filePath the image abstract filePath.
     * @return the histogram of this image item.
     */
    public static int[] getHist(String filePath) {
        return sHistogramHashMap.get(filePath);
    }

    /**
     * Get filePath of the image item.
     * @return the filePath of the image item.
     */
    public String getFilePath() {
        return mFilePath;
    }

    /**
     * Reset imageDC effect state while Gallery Activity onCreate state.
     * @param context get SharedPreferences object by the context.
     */
    public static void resetImageDC(Context context) {
        if (FeatureConfig.SUPPORT_IMAGE_DC_ENHANCE) {
            SharedPreferences sp = context.getSharedPreferences(ImageDC.DC,
                    Context.MODE_PRIVATE);
            if (null != sp) {
                sAvailable = sp.getBoolean(ImageDC.DCNAME, false);
            }
            MtkLog.d(TAG,
                    " <resetImageDC> sharePreference sAvailable = "
                            + sAvailable);
        }
    }

    public static void setStatus(boolean avaliable) {
        sAvailable = avaliable;
    }

    public static boolean getStatus() {
        return sAvailable;
    }

    /**
     * Set ImageDC menu String .
     * @param dcItem imageDCItem.
     */
    public static void setMenuItemTile(MenuItem dcItem) {
        String text;
        if (sAvailable) {
            dcItem.setTitle(R.string.m_dc_open);
        } else {
            dcItem.setTitle(R.string.m_dc_close);
        }
    }

    /**
     * Reset ImageDC status while check ImageDC menu.
     * @param context get SharedPreferences object by the context.
     */
    public static void resetStatus(Context context) {
        SharedPreferences sp = context.getSharedPreferences(ImageDC.DC,
                Context.MODE_PRIVATE);
        final Editor editor = sp.edit();
        editor.putBoolean(ImageDC.DCNAME, !sAvailable);
        editor.commit();
        sAvailable = !sAvailable;
    }
}
