
package com.mediatek.incallui.wrapper;

import android.os.SystemProperties;

public class FeatureOptionWrapper {

    private static final String TAG = "FeatureOptionWrapper";
    private static final String VIDEO_DISPLAY_VIEW_TRANSLATION_KEY = "incall_video_display_trans";

    private FeatureOptionWrapper() {
    }

    /**
     * @see FeatureOption.MTK_GEMINI_SUPPORT
     * @see FeatureOption.MTK_GEMINI_3SIM_SUPPORT
     * @see FeatureOption.MTK_GEMINI_4SIM_SUPPORT
     * @return true if the device has 2 or more slots
     */
    public static boolean isSupportGemini() {
        //return PhoneConstants.GEMINI_SIM_NUM >= 2;
        return true;
    }

    /**
     * @return MTK_PHONE_VOICE_RECORDING
     */
    public static boolean isSupportPhoneVoiceRecording() {
//        return com.mediatek.featureoption.FeatureOption.MTK_PHONE_VOICE_RECORDING;
        return true;
    }

    public static boolean isSupportPrivacyProtect() {
    //    boolean isSupportPrivacyProtect = com.mediatek.common.featureoption.FeatureOption.MTK_PRIVACY_PROTECTION_LOCK;
    //    return isSupportPrivacyProtect;
        return true;
    }

    /// M: for VoLTE Conference Call @{
    public static final boolean MTK_IMS_SUPPORT = SystemProperties.get("ro.mtk_ims_support")
            .equals("1");
    public static final boolean MTK_VOLTE_SUPPORT = SystemProperties.get("ro.mtk_volte_support")
            .equals("1");
    // local "feature option" to control add member function of VoLTE conference call.
    public static final boolean LOCAL_OPTION_ENABLE_ADD_MEMBER = true;
    /// @}

    private static final boolean MTK_CTA_SET = "1".equals(SystemProperties.get("ro.mtk_cta_set"));

    /**
     * M: [CTA] is a set of test cases in China.
     * @return if current product supports CTA, return true.
     */
    public static boolean isCta() {
        return MTK_CTA_SET;
    }

    /**
     * M: [ALPS02292879] Remove google's Ecc callback number display feature.
     * Currently, this feature is unstable. Sometimes the callback number show
     * and sometimes not. We found it hard to unionize the behaviors in all scenarios.
     * So we remove this feature since L1.
     * TODO: We should add it back if it were required in future.
     * @return option of the ECC Callback number display. Now return false only.
     */
    public static boolean supportsEccCallbackNumber() {
        return false;
    }

    /**
     * M: whether support video display view translate feature.
     * @return
     */
    public static boolean isSupportVideoDisplayTrans() {
        return "1".equals(SystemProperties.get(VIDEO_DISPLAY_VIEW_TRANSLATION_KEY));
    }
}

