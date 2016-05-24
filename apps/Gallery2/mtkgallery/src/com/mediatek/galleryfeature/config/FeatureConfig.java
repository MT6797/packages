package com.mediatek.galleryfeature.config;

import android.content.Context;

import android.os.Environment;
import android.os.SystemProperties;

import com.mediatek.bluetooth.ConfigHelper;
import com.mediatek.bluetooth.ProfileConfig;
import com.mediatek.galleryframework.util.MtkLog;
import com.mediatek.galleryframework.util.Utils;

import java.io.File;

/**
 * Get featureOption from FeatureConfig.
 */
public class FeatureConfig {
    private static final String TAG = "MtkGallery2/FeatureConfig";

    public static final String DRM_PROPERTY = "ro.mtk_oma_drm_support";
    public static final String CTA_PROPERTY = "ro.mtk_cta_set";
    public static final String HOTKNOT_PROPERTY = "ro.mtk_hotknot_support";
    public static final String SUBTITLE_PROPERTY = "ro.mtk_subtitle_support";
    public static final String AUDIO_CHANGE_PROPERTY = "ro.mtk_audio_change_support";
    public static final String MTK_GMO_RAM_OPTIMIZE = "ro.mtk_gmo_ram_optimize";
    public static final String SLOWMOTION_SUPPORT_PROPERTY = "ro.mtk_slow_motion_support";
    public static final String THUMBNAIL_PLAY_PROPERTY = "ro.mtk_thumbnail_play_support";
    public static final String VIDEO_HEVC_SUPPORT_PROPERTY = "";
    public static final String VIDEO_2K_SUPPORT_PROPERTY = "ro.mtk.video.4kh264_support";
    public static final String IMAGE_STEREO_SUPPORT_PROPERTY = "ro.mtk_cam_img_refocus_support";

    public static final int CPU_CORES_NUM = Runtime.getRuntime().availableProcessors();
    public static final int CPU_4_CORES = 4;

    // Whether resource consuming MTK new features should be enabled
    public static final boolean SUPPORT_HEAVY_FEATURE = (CPU_CORES_NUM >= CPU_4_CORES);

    public static final boolean SUPPORT_PANORAMA3D = true && SUPPORT_HEAVY_FEATURE;

    public static final boolean SUPPORT_CONSHOTS_IMAGES = true && SUPPORT_HEAVY_FEATURE;

    public static final boolean SUPPORT_VIDEO_THUMBNAIL_PLAY = SystemProperties.get(
            THUMBNAIL_PLAY_PROPERTY).equals("1")
            && SUPPORT_HEAVY_FEATURE;

    public static final boolean SUPPORT_FANCY_HOMEPAGE = true;

    public static final boolean SUPPORT_BLUETOOTH_PRINT = ConfigHelper
            .checkSupportedProfiles(ProfileConfig.PROFILE_ID_BPP);

    public static final boolean SUPPORT_MTK_BEAM_PLUS = SystemProperties.get(
            "ro.mtk_beam_plus_support").equals("1");

    // DRM (Digital Rights management) is developed by MediaTek.
    // Gallery3d avails MtkPlugin via android DRM framework to manage
    // digital rights of videos and images
    public static final boolean SUPPORT_DRM = SystemProperties.get(DRM_PROPERTY).equals("1");

    // CTA Data Protection
    public static final boolean SUPPORT_CTA = SystemProperties.get(CTA_PROPERTY).equals("1");

    // HEVC(Multi-Picture Object) is new video codec supported by
    // MediaTek. hevc can encode and decode. Gallery is
    // responsible to support hevc Trim/Mute
    // L default enabled this feature
    public static final boolean SUPPORT_HEVC = true; // SystemProperties.get(
    // VIDEO_HEVC_SUPPORT_PROPERTY).equals("1");

    public static final boolean SUPPORT_SLOW_MOTION = SystemProperties.get(
            SLOWMOTION_SUPPORT_PROPERTY).equals("1");

    public static final boolean SUPPORT_2K_VIDEO = SystemProperties.get(VIDEO_2K_SUPPORT_PROPERTY)
            .equals("1");

    public static final boolean SUPPORT_HOTKNOT = SystemProperties.get(HOTKNOT_PROPERTY)
            .equals("1");

    public static final boolean SUPPORT_EMULATOR = SystemProperties.get("ro.kernel.qemu").equals(
            "1");

    public static final boolean IS_TABLET = SystemProperties.get("ro.build.characteristics")
            .equals("tablet");

    public static final boolean SUPPORT_SLIDE_VIDEO_PLAY = SystemProperties.get(
            "ro.mtk_slidevideo_support").equals("1")
            && (new File(Environment.getExternalStorageDirectory(), "SUPPORTSVP")).exists()
            && !IS_TABLET;

    public static final boolean SUPPORT_AUDIO_CHANGE = SystemProperties.get(AUDIO_CHANGE_PROPERTY)
            .equals("1");

    public static final boolean SUPPORT_SUBTITLE = SystemProperties.get(SUBTITLE_PROPERTY).equals(
            "1");

    public static final boolean SUPPORT_PQ = (new File(Environment.getExternalStorageDirectory(),
            "SUPPORT_PQ")).exists();

    public static final boolean IS_GMO_RAM_OPTIMIZE = SystemProperties.get(MTK_GMO_RAM_OPTIMIZE)
            .equals("1");

    public static volatile boolean sIsLowRamDevice;

    // Picture quality enhancement feature avails Camera ISP hardware
    // to improve image quality displayed on the screen.
    public static final boolean SUPPORT_PICTURE_QUALITY_ENHANCE = true;

    public static final boolean SUPPORT_IMAGE_DC_ENHANCE = (new File(
            Environment.getExternalStorageDirectory(), "SUPPORT_DC")).exists()
            && SystemProperties.get("ro.mtk_miravision_image_dc").equals("1");

    public static boolean sSupportRefocus = false;

    /**
     * Check if support clear motion or not.
     * @param context Gallery context
     * @return        true if support, otherwise false
     */
    public static boolean isSupportClearMotion(Context context) {
        return Utils.isFileExist(context, "SUPPORT_CLEARMOTION");
    }

    public static boolean sSupportStereo = false;
    private static void updateStereoFeatureOption() {
        if (SystemProperties.get(IMAGE_STEREO_SUPPORT_PROPERTY).equals("1")) {
            sSupportStereo = true;
        } else {
            File file = new File(Environment.getExternalStorageDirectory(),
                    "SUPPORT_STEREO");
            if (file.exists()) {
                sSupportStereo = true;
            }
        }
    }

    static {
        updateStereoFeatureOption();
        MtkLog.i(TAG, "CPU_CORES_NUM = " + CPU_CORES_NUM);
        MtkLog.i(TAG, "sSupportRefocus = " + sSupportRefocus);
        MtkLog.i(TAG, "SUPPORT_HEAVY_FEATURE = " + SUPPORT_HEAVY_FEATURE);
        MtkLog.i(TAG, "SUPPORT_PANORAMA3D = " + SUPPORT_PANORAMA3D);
        MtkLog.i(TAG, "SUPPORT_CONSHOTS_IMAGES = " + SUPPORT_CONSHOTS_IMAGES);
        MtkLog.i(TAG, "SUPPORT_FANCY_HOMEPAGE = " + SUPPORT_FANCY_HOMEPAGE);
        MtkLog.i(TAG, "SUPPORT_BLUETOOTH_PRINT = " + SUPPORT_BLUETOOTH_PRINT);
        MtkLog.i(TAG, "SUPPORT_MTK_BEAM_PLUS = " + SUPPORT_MTK_BEAM_PLUS);
        MtkLog.i(TAG, "SUPPORT_DRM = " + SUPPORT_DRM);
        MtkLog.i(TAG, "SUPPORT_CTA = " + SUPPORT_CTA);
        MtkLog.i(TAG, "SUPPORT_HEVC = " + SUPPORT_HEVC);
        MtkLog.i(TAG, "SUPPORT_SLOW_MOTION = " + SUPPORT_SLOW_MOTION);
        MtkLog.i(TAG, "SUPPORT_2K_VIDEO = " + SUPPORT_2K_VIDEO);
        MtkLog.i(TAG, "SUPPORT_HOTKNOT = " + SUPPORT_HOTKNOT);
        MtkLog.i(TAG, "SUPPORT_EMULATOR = " + SUPPORT_EMULATOR);
        MtkLog.i(TAG, "SUPPORT_SLIDE_VIDEO_PLAY = " + SUPPORT_SLIDE_VIDEO_PLAY);
        MtkLog.i(TAG, "SUPPORT_AUDIO_CHANGE = " + SUPPORT_AUDIO_CHANGE);
        MtkLog.i(TAG, "SUPPORT_SUBTITLE = " + SUPPORT_SUBTITLE);
        MtkLog.i(TAG, "SUPPORT_PQ = " + SUPPORT_PQ);
        MtkLog.i(TAG, "IS_GMO_RAM_OPTIMIZE = " + IS_GMO_RAM_OPTIMIZE);
        MtkLog.i(TAG, "sIsLowRamDevice = " + sIsLowRamDevice);
        MtkLog.i(TAG, "SUPPORT_IMAGE_DC_ENHANCE = " + SUPPORT_IMAGE_DC_ENHANCE);
    }
}