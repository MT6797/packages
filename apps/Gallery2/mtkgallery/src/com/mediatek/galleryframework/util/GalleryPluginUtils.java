package com.mediatek.galleryframework.util;

import android.content.Context;

import com.mediatek.common.MPlugin;

import com.mediatek.gallery3d.ext.DefaultGalleryPickerExt;
import com.mediatek.gallery3d.ext.DefaultImageOptionsExt;
import com.mediatek.gallery3d.ext.IGalleryPickerExt;
import com.mediatek.gallery3d.ext.IImageOptionsExt;

public class GalleryPluginUtils {
    private static final String TAG = "Gallery2/GalleryPluginUtils";
    private static IImageOptionsExt sImageOptions;
    private static IGalleryPickerExt sGalleryPicker;
    private static boolean sIsImageOptionsPrepared = false;
    private static Context sContext = null;

    public static void initialize(Context context) {
        sContext = context;
    }

    private static void prepareImageOptions(Context context) {
        if (context == null) {
            return;
        }
        if (sImageOptions == null) {
            sImageOptions = (IImageOptionsExt) MPlugin.createInstance(
                    IImageOptionsExt.class.getName(), context.getApplicationContext());
            MtkLog.i(TAG, "<prepareImageOptions> sImageOptions = " + sImageOptions);

            if (sImageOptions == null) {
                MtkLog.i(TAG, "<prepareImageOptions> create DefaultImageOptionsExt!");
                sImageOptions = new DefaultImageOptionsExt();
            }
            sIsImageOptionsPrepared = true;
        }
    }

    public static IImageOptionsExt getImageOptionsPlugin() {
        if (!sIsImageOptionsPrepared) {
            prepareImageOptions(sContext);
        }
        return (sImageOptions != null ? sImageOptions : new DefaultImageOptionsExt());
    }

    /**
     * Get gallery picker plugin.
     * @return IGalleryPickerExt Gallery picker
     */
    public static IGalleryPickerExt getGalleryPickerPlugin() {
        if (sGalleryPicker == null) {
            sGalleryPicker = (IGalleryPickerExt) MPlugin.createInstance(
                    IGalleryPickerExt.class.getName(), sContext.getApplicationContext());

            MtkLog.i(TAG, "<getGalleryPickerPlugin> sGalleryPicker = " + sGalleryPicker);

            if (sGalleryPicker == null) {
                MtkLog.i(TAG, "<getGalleryPickerPlugin> create DefaultGalleryPickerExt!");
                sGalleryPicker = new DefaultGalleryPickerExt();
            }
        }
        return sGalleryPicker;
    }
}
