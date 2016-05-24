package com.mediatek.providers.contacts;

import android.os.SystemProperties;
import android.telephony.TelephonyManager;

/**
 * M: add for Contacts Provider.
 */
public class ContactsProviderUtils {

    public static final boolean DBG_DIALER_SEARCH = true;

    /**
     * [DialerSearch] whether DialerSearch feature enabled on this device.
     * @return ture if allowed to enable
     */
    public static boolean isDialerSearchSupport() {
        return ("1").equals(SystemProperties.get("ro.mtk_dialer_search_support"));
    }

    /**
     * [Gemini] whether Gemini feature enabled on this device.
     * @return ture if allowed to enable
     */
    public static boolean isGeminiSupport() {
        return TelephonyManager.getDefault().getSimCount() > 1;
    }

    /**
     * [SearchDB] whether SearchDb feature enabled on this device.
     * @return ture if allowed to enable
     */
    public static boolean isSearchDbSupport() {
        return ("1").equals(SystemProperties.get("ro.mtk_search_db_support"));
    }

    /**
     * [Phone_Number_Geodescription] whether PhoneNumberGeodescription feature
     * enabled on this device.
     * @return ture if allowed to enable
     */
    public static boolean isPhoneNumberGeo() {
        return ("1").equals(SystemProperties.get("ro.mtk_phone_number_geo"));
    }

    /**
    * [VOLTE/IMS] whether VOLTE feature enabled on this device.
    * @return ture if allowed to enable
    */
   public static boolean isVolteEnabled() {
       return ("1").equals(SystemProperties.get("ro.mtk_volte_support"));
   }

    /**
    * [VOLTE/IMS] whether ImsCall feature enabled on this device.
    * @return ture if allowed to enable
    */
   public static boolean isImsCallEnabled() {
       return ("1").equals(SystemProperties.get("ro.mtk_ims_support"));
   }
}