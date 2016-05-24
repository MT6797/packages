package com.mediatek.settings.ext;

import android.preference.PreferenceScreen;

public interface IStatusExt {
    /**
    * customize imei & imei sv display name.
    * @param imeikey: the name of imei
    * @param imeiSvKey: the name of imei software version
    * @param parent: parent preference
    * @param slotId: slot id
    * @internal
    */
    void customizeImei(String imeiKey, String imeiSvKey, PreferenceScreen parent, int slotId);
}
