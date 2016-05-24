package com.mediatek.galleryframework.gl;

import android.graphics.Bitmap;

import junit.framework.Assert;

// BitmapTexture is a texture whose content is specified by a fixed Bitmap.
//
//The texture does not own the Bitmap.while the texture uploaded to GPU,should recycle the bitmap.
public class MBitmapTexture extends MUploadedTexture {
    protected Bitmap mContentBitmap;

    public MBitmapTexture(Bitmap bitmap) {
        this(bitmap, false);
    }

    public MBitmapTexture(Bitmap bitmap, boolean hasBorder) {
        super(hasBorder);
        Assert.assertTrue(bitmap != null && !bitmap.isRecycled());
        mContentBitmap = bitmap;
    }

    @Override
    protected void onFreeBitmap(Bitmap bitmap) {
        // while the texture uploaded to GPU ,should recycle the bitmap.
        if (!inFinalizer() && bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
    }

    @Override
    protected Bitmap onGetBitmap() {
        return mContentBitmap;
    }

    public Bitmap getBitmap() {
        return mContentBitmap;
    }
}
