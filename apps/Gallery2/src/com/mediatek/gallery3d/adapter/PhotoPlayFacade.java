package com.mediatek.gallery3d.adapter;

import android.app.Activity;
import android.content.Context;

import com.android.gallery3d.app.GalleryAppImpl;

import com.mediatek.galleryfeature.config.FeatureConfig;
import com.mediatek.galleryfeature.container.ConshotFilter;
import com.mediatek.galleryfeature.container.ContainerMember;
import com.mediatek.galleryfeature.drm.DrmFilter;
import com.mediatek.galleryfeature.drm.DrmMember;
import com.mediatek.galleryfeature.dynamic.LayerManagerImpl;
import com.mediatek.galleryfeature.dynamic.PhotoPlayEngine;
import com.mediatek.galleryfeature.dynamic.ThumbnailPlayEngine;
import com.mediatek.galleryfeature.filter.Video4kFilter;
import com.mediatek.galleryfeature.gif.GifMember;
import com.mediatek.galleryfeature.panorama.PanoramaHelper;
import com.mediatek.galleryfeature.panorama.PanoramaMember;
import com.mediatek.galleryfeature.platform.PlatformHelper;
import com.mediatek.galleryfeature.raw.RawMember;
import com.mediatek.galleryfeature.stereo.freeview3d.DepthImageMember;
import com.mediatek.galleryfeature.video.VideoMember;
import com.mediatek.galleryframework.base.LayerManager;
import com.mediatek.galleryframework.base.MediaCenter;
import com.mediatek.galleryframework.base.MediaFilter;
import com.mediatek.galleryframework.base.MediaMember;
import com.mediatek.galleryframework.base.PlayEngine;
import com.mediatek.galleryframework.base.ThumbType;
import com.mediatek.galleryframework.gl.GLIdleExecuter;
import com.mediatek.galleryframework.util.MtkLog;
import com.mediatek.galleryframework.util.Utils;

import java.util.ArrayList;

public class PhotoPlayFacade {
    private final static String TAG = "MtkGallery2/PhotoPlayFacade";

    private static MediaCenter sMediaCenter;

    public static void initialize(GalleryAppImpl context, int microThumbnailSize,
            int thumbnailSize, int highQualitySize) {
        Utils.initialize(context);
        PanoramaHelper.initialize(context);
        ThumbType.MICRO.setTargetSize(microThumbnailSize);
        ThumbType.MIDDLE.setTargetSize(thumbnailSize);
        ThumbType.HIGHQUALITY.setTargetSize(highQualitySize);
        PlatformHelper.setPlatform(new PlatformImpl(context));

        // register filter
        if (FeatureConfig.SUPPORT_DRM) {
            MediaFilter.registerFilter(new DrmFilter());
        }
        if (FeatureConfig.SUPPORT_2K_VIDEO) {
            MediaFilter.registerFilter(new Video4kFilter());
        }
        if (FeatureConfig.SUPPORT_CONSHOTS_IMAGES) {
            MediaFilter.registerFilter(new ConshotFilter());
        }
    }

    public static MediaCenter getMediaCenter() {
        if (sMediaCenter == null) {
            sMediaCenter = new MediaCenter();
        }
        return sMediaCenter;
    }

    public static int getFullScreenPlayCount() {
        return Config.FULL_PLAY_COUNT;
    }

    public static int getThumbPlayCount() {
        return Config.THUMB_PLAY_COUNT;
    }

    public static int getFullScreenTotalCount() {
        return Config.FULL_TOTAL_COUNT;
    }

    public static int getThumbTotalCount() {
        return Config.THUMB_TOTAL_COUNT;
    }

    public static void registerMedias(Context context, GLIdleExecuter exe) {
        MtkLog.i(TAG, "<registerMedias> Context = " + context
                + ", GLIdleExecuter = " + exe);
        MediaCenter mc = getMediaCenter();

        // put MediaMember to ArrayList
        ArrayList<MediaMember> memberList = new ArrayList<MediaMember>();
        if (FeatureConfig.SUPPORT_DRM) {
            memberList.add(new DrmMember(context, mc));
        }

        // Stereo
        if (FeatureConfig.sSupportStereo) {
            memberList.add(new DepthImageMember(context, exe));
        }
        memberList.add(new VideoMember(context, exe));
        if (FeatureConfig.SUPPORT_PANORAMA3D) {
            memberList.add(new PanoramaMember(context));
        }
        memberList.add(new GifMember(context, exe));
        if (FeatureConfig.SUPPORT_CONSHOTS_IMAGES) {
            memberList.add(new ContainerMember(context));
        }
        memberList.add(new RawMember(context));
        memberList.add(new MediaMember(context));

        // register
        mc.registerMedias(memberList);
    }

    public static void registerWidgetMedias(Context context) {
        MtkLog.i(TAG, "<registerWidgetMedias> context = " + context);
        MediaCenter mc = getMediaCenter();

        // put MediaMember to ArrayList
        ArrayList<MediaMember> memberList = new ArrayList<MediaMember>();
        if (FeatureConfig.SUPPORT_DRM) {
            memberList.add(new DrmMember(context, mc));
        }
        memberList.add(new GifMember(context));
        memberList.add(new MediaMember(context));

        // register
        mc.registerMedias(memberList);
    }

    public static PlayEngine createPlayEngineForFullScreen() {
        return new PhotoPlayEngine(getMediaCenter(), Config.FULL_TOTAL_COUNT,
                Config.FULL_PLAY_COUNT, Config.FULL_WORK_THREAD_NUM,
                ThumbType.MIDDLE);
    }

    public static PlayEngine createPlayEngineForThumbnail(Context context) {
        return new ThumbnailPlayEngine(context, getMediaCenter(),
                Config.THUMB_TOTAL_COUNT, Config.THUMB_PLAY_COUNT,
                Config.THUMB_WORK_THREAD_NUM, ThumbType.MICRO);
    }

    public static PlayEngine createPlayEngineForThumbnail(Context context,
            ThumbType type) {
        return new ThumbnailPlayEngine(context, getMediaCenter(),
                Config.THUMB_TOTAL_COUNT, Config.THUMB_PLAY_COUNT,
                Config.THUMB_WORK_THREAD_NUM, type);
    }

    public static LayerManager createLayerMananger(Activity activity) {
        return new LayerManagerImpl(activity, getMediaCenter());
    }

    private PhotoPlayFacade() {
    }
}
