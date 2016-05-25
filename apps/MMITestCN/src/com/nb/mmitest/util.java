package com.nb.mmitest;
import android.content.Context;
import android.media.AudioManager;
public class util {
    private static final String MTK_DUAL_MIC_SUPPORT = "MTK_DUAL_MIC_SUPPORT";
    private static final String MTK_DUAL_MIC_SUPPORT_on = "MTK_DUAL_MIC_SUPPORT=true";
    private final static String ONE = "1";
    public static boolean isMtkDualMicSupport(Context context) {
        String state = null;
        AudioManager audioManager = (AudioManager)
        		context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            state = audioManager.getParameters(MTK_DUAL_MIC_SUPPORT);
            if (state.equalsIgnoreCase(MTK_DUAL_MIC_SUPPORT_on)) {
                return true;
            }
        }
        return false;
    }
}
