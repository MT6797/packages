package com.mediatek.galleryfeature.platform;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.mediatek.gallery3d.ext.IActivityHooker;
import com.mediatek.galleryfeature.SlideVideo.IVideoController;
import com.mediatek.galleryfeature.SlideVideo.IVideoHookerCtl;
import com.mediatek.galleryfeature.SlideVideo.IVideoPlayer;
import com.mediatek.galleryfeature.panorama.SwitchBarView;
import com.mediatek.galleryframework.base.MediaData;
import com.mediatek.galleryframework.base.Work;

/**
 * Making a bridge which connect google packages and mediatek packages.
 * In mediatek feature, we can not access google packages directly for easy
 * migration purpose, so google packages implement this
 * interface(PlatformImpl.java) and register it, then mediatek feature could
 * access google packages by PlatformImpl.
 */
public interface Platform {
    /**
     * Check if the photo is out of decoding spec.
     * @param fileSize size of this photo, in bytes
     * @param width    width of this photo
     * @param height   height of this photo
     * @param mimeType mime type of this photo
     * @return         true if out of spec, otherwise false
     */
    public boolean isOutOfDecodeSpec(long fileSize, int width, int height, String mimeType);

    /**
     * Enter ContainerPage like ContinusShot.
     * @param activity   gallery activity
     * @param data       related data of current media item
     * @param getContent flag indicating if launched by getting Content
     * @param bundleData useful information carried with bundle
     */
    public void enterContainerPage(Activity activity, MediaData data, boolean getContent,
            Bundle bundleData);

    /**
     * Switch to ContainerPage from other page.
     * @param activity   gallery activity
     * @param data       related data of current media item
     * @param getContent flag indicating if launched by getting Content
     * @param bundleData useful information carried with bundle
     */
    public void switchToContainerPage(Activity activity, MediaData data, boolean getContent,
            Bundle bundleData);

    /**
     * Create PanoramaSwitchBarView.
     * @param activity gallery activity
     * @return         SwitchBarView which has been created
     */
    public SwitchBarView createPanoramaSwitchBarView(Activity activity);

    /**
     * Create SVExtension.
     * @param context gallery activity context
     * @param data    related data of current media item
     * @return        created video player
     */
    public IVideoPlayer createSVExtension(Context context, MediaData data);

    /**
     * Create video controller.
     * @param context gallery activity context
     * @return        created video controller
     */
    public IVideoController createController(Context context);

    /**
     * Create video hooker.
     * @param activity               gallery activity context
     * @param rewindAndForwardHooker activity hooker
     * @return                       created video hooker
     */
    public IVideoHookerCtl createHooker(Activity activity, IActivityHooker rewindAndForwardHooker);

    /**
     * Submit job to Gallery thread pool.
     * @param work job that has been wrapped as work
     */
    public void submitJob(Work work);
}