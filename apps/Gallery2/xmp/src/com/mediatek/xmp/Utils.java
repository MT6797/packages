package com.mediatek.xmp;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * XMP utils function.
 */
public class Utils {
    private static final String TAG = "Utils";
    private static final int DEFAULT_COMPRESS_QUALITY = 100;
    private static final byte DEFAULT_COLOR = (byte) 255;
    private static final int BYTES_PER_PIXEL = 4;
    private static final int NUM_3 = 3;

    /**
     * encode mask to PNG, first generate bitmap, then compress to bitmap.
     *
     * @param data
     *            mask data
     * @param width
     *            mask width
     * @param height
     *            mask height
     * @return encode data
     */
    public static byte[] encodePng(byte[] data, int width, int height) {

        Log.d(TAG, "<encodePng> data:" + data + ",width:" + width + ",height:" + height);

        if (data == null) {
            Log.d(TAG, "<encodePng> null data, return null!");
            return null;
        }

        long startTime = System.currentTimeMillis();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);

        byte[] array = new byte[data.length * BYTES_PER_PIXEL];
        Arrays.fill(array, DEFAULT_COLOR);
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                array[(h * width + w) * BYTES_PER_PIXEL + NUM_3] = data[h * width + w];
            }
        }

        ByteBuffer buffer = ByteBuffer.allocate(array.length);
        buffer.put(array);
        buffer.rewind();
        bitmap.copyPixelsFromBuffer(buffer);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.PNG, DEFAULT_COMPRESS_QUALITY, stream);
        byte[] res = stream.toByteArray();
        long endTime = System.currentTimeMillis();
        Log.d(TAG, "<encodePng> spend time:" + (endTime - startTime));

        return res;
    }

    /**
     * <1>decode PNG to bitmap. <2>get mask buffer from bitmap.
     *
     * @param data
     *            PNG data
     * @return success->mask buffer, fail->null
     */
    public static byte[] decodePng(byte[] data) {
        if (data == null) {
            Log.d(TAG, "<decodePng> data is null!!!");
            return null;
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (bitmap == null) {
            Log.d(TAG, "decode png fail!!!");
            return null;
        }
        int length = bitmap.getWidth() * bitmap.getHeight() * BYTES_PER_PIXEL;
        ByteBuffer buffer = ByteBuffer.allocate(length);
        byte[] dst = new byte[length];
        bitmap.copyPixelsToBuffer(buffer);
        buffer.rewind();
        buffer.get(dst);
        return dst;
    }
}
