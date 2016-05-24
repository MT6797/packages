package com.android.soundrecorder;

import android.os.SystemProperties;
import android.content.Context;
import android.media.AudioManager;


public class OptionsUtil {

    private static final String TAG = "SR/OptionsUtil";
    private static final String MTK_AUDIO_HD_REC_SUPPORT = "MTK_AUDIO_HD_REC_SUPPORT";
    private static final String MTK_AUDIO_HD_REC_SUPPORT_on = "MTK_AUDIO_HD_REC_SUPPORT=true";

    /*
     * @return whether AAC encode support or not.
     */
    public static final boolean isAACEncodeSupport() {
        return SystemProperties.getBoolean("ro.have_aacencode_feature", false);
    }

    /*
     * @return whether HD record support or not.
     */
    public static final boolean isAudioHDRecordSupport(Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) {
            LogUtils.d(TAG, "isAudioHDRecordSupport get audio service is null.");
            return false;
        }
        String val = am.getParameters(MTK_AUDIO_HD_REC_SUPPORT);
        return val != null && val.equalsIgnoreCase(MTK_AUDIO_HD_REC_SUPPORT_on);
    }

    /**
     * this is for emulator load, if apk is running in
     * emulator, this will return true, otherwise false.
     */
    public static final boolean isRunningInEmulator() {
        int kqmenu = SystemProperties.getInt("ro.kernel.qemu", 0);
        return (kqmenu == 1);
    }
}
