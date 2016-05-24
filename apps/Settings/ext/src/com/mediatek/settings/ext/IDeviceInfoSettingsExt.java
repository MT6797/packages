package com.mediatek.settings.ext;

import android.preference.Preference;
import android.preference.PreferenceScreen;

public interface IDeviceInfoSettingsExt {
    /**
     * CT E push feature refactory,add Epush in common feature
     * @param root The preference screen to add E push entrance
     * @internal
     */
    void addEpushPreference(PreferenceScreen root);

    /**
     * Update preference summary
     * @param preference Customized preference
     * @param value Preference summary
     * @param dafaultValue Default preference summary
     * @internal
     */
    void updateSummary(Preference preference, String value, String dafaultValue);
}
