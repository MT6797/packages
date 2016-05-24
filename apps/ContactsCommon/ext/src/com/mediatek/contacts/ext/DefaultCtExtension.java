package com.mediatek.contacts.ext;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;



public class DefaultCtExtension implements ICtExtension {

    private static final String TAG = "DefaultCtExtension";

    @Override
    public Drawable getPhotoDrawableBySub(Resources res, int subId, Drawable photoDrawable) {
        Log.d(TAG, "getPhotoDrawableBySub");
        return photoDrawable;
    }

    @Override
    public void loadSimCardIconBitmap(Resources res) {
        Log.d(TAG, "loadSimCardIconBitmap");
        // do nothing in common
    }

    @Override
    public int showAlwaysAskIndicate(int defaultValue) {
        return defaultValue;
    }

}
