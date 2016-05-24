package com.mediatek.dialer.util;

import android.os.SystemProperties;

public class DialerFeatureOptions {

    // [Call Account Notification] Show a notification to indicator the available call accounts
    // and the selected call account. And allow the user to select the default call account.
    // (Disable it in A1 project)
    public static final boolean CALL_ACCOUNT_NOTIFICATION = !isA1ProjectEnabled();

    // [IP Dial] IP call prefix.
    public static final boolean IP_PREFIX = true;
    // [Union Query] this feature will make a union query on Calls table and data view
    // while query the call log. So that the query result would contain contacts info.
    // and no need to query contacts info again in CallLogAdapter. It improve the call
    // log performance.
    public static final boolean CALL_LOG_UNION_QUERY = true;

    // [Multi-Delete] Support delete the multi-selected call logs
    public static final boolean MULTI_DELETE = true;

    // [Suggested Account] if true, support feature "Suggested Account",
    // otherwise not support.
    public static final boolean MTK_SUPPORT_SUGGESTED_ACCOUNT = true;

    // [Dialer Global Search] Support search call log from quick search box.
    public static final boolean DIALER_GLOBAL_SEARCH = true;

    // For dynamic control the test case
    public static boolean sIsRunTestCase = false;

    /**
     * [Call Log Account Filter] when enabled, allow user to filter out call
     * logs from specific account
     */
    public static boolean isCallLogAccountFilterEnabled() {
        String operatorSpec = SystemProperties.get("ro.operator.optr", "");
        // Return true on OP01 or OP02
        if (operatorSpec.equals("OP01") || operatorSpec.equals("OP02")) {
            return true;
        }
        return false;
    }

    /**
     * [CallLog Incoming and Outgoing Filter]
     * Whether the callLog incoming and outgoing filter is enabled or not.
     * @return true if the callLog incoming and outgoing filter feature is enabled.
     */
    public static boolean isCallLogIOFilterEnabled() {
        String operatorSpec = SystemProperties.get("ro.operator.optr", "");
        String operatorSeg = SystemProperties.get("ro.operator.seg", "");
        boolean isCtOperatorA = operatorSpec.equals("OP09") && operatorSeg.equals("SEGDEFAULT");
        // Return true on OP09 or OP02 mode
        if (operatorSpec.equals("OP02") || isCtOperatorA) {
            return true;
        }
        return false;
    }

    /**
     * [MTK Dialer Search] whether DialerSearch feature enabled on this device
     * @return ture if allowed to enable
     */
    public static boolean isDialerSearchEnabled() {
        return sIsRunTestCase ?
                false : SystemProperties.get("ro.mtk_dialer_search_support").equals("1");
    }


    /**
     * [Suggested Account] Whether suggested account is supported
     * @return true if the suggested account was supported
     */
    public static boolean isSuggestedAccountSupport() {
        return MTK_SUPPORT_SUGGESTED_ACCOUNT;
    }

    public static final boolean MTK_IMS_SUPPORT = SystemProperties.get(
            "ro.mtk_ims_support").equals("1");
    public static final boolean MTK_VOLTE_SUPPORT = SystemProperties.get(
            "ro.mtk_volte_support").equals("1");
    //[VoLTE ConfCall] Whether the VoLTE enhanced conference call (Launch
    //conference call directly from dialer) supported.
    public static final boolean MTK_ENHANCE_VOLTE_CONF_CALL = true;
    /**
     * [VoLTE ConfCall] Whether the VoLTE enhanced conference call (Launch
     * conference call directly from dialer) supported.
     *
     * @return true if the VoLTE enhanced conference call supported
     */
    public static boolean isVolteEnhancedConfCallSupport() {
        return MTK_ENHANCE_VOLTE_CONF_CALL && MTK_IMS_SUPPORT && MTK_VOLTE_SUPPORT;
    }

    //[VoLTE ConfCallLog] Whether the VoLTE conference calLog supported.
    public static final boolean MTK_VOLTE_CONFERENCE_CALLLOG = true;
    /**
     * [VoLTE ConfCallLog] Whether the VoLTE conference calLog supported.
     *
     * @return true if the VoLTE conference calLog supported
     */
    public static boolean isVolteConfCallLogSupport() {
        return MTK_VOLTE_CONFERENCE_CALLLOG && MTK_IMS_SUPPORT && MTK_VOLTE_SUPPORT;
    }

    /**
     * [IMS Call] Whether the IMS call supported
     * @return true if the IMS call supported
     */
    public static boolean isImsCallSupport() {
        return MTK_IMS_SUPPORT && MTK_VOLTE_SUPPORT;
    }

    /**
     * [MTK Audio Profiles] MTK audio profiles feature
     * @return true if MTK Audio Profiles supported
     */
    public static boolean isMtkAudioProfileEnabled() {
        return SystemProperties.get("ro.mtk_audio_profiles").equals("1");
    }


    /**
     * [C2K solution2] Whether the C2K solution2 supported
     * @return true if the C2K solution2 supported
     */
    public static boolean isC2KSolution2Support() {
        return SystemProperties.get("ro.mtk.c2k.slot2.support").equals("1");
    }

    /**
     * Whether the LTE is supported
     * @return true if the LTE is supported
     */
    public static boolean isLteSupport() {
        return SystemProperties.get("ro.mtk_lte_support").equals("1");
    }

    /**
     * Whether Android one project enable
     * @return true if is Android one project
     */
    public static boolean isA1ProjectEnabled() {
        return SystemProperties.get("ro.mtk_a1_feature").equals("1");
    }
}
