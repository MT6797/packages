package com.mediatek.settings.ext;

import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

public interface IAudioProfileExt {

    /**
     * set different params of RingtonePickerActivity for different operator.
     * @param intent the intent that will send to RingtonePickerActivity
     * @internal
     */
    void setRingtonePickerParams(Intent intent);

    /**
     * Add customized Preference.
     * @param preferenceScreen The root PreferenceScreen to add preference
     * @internal
     */
    void addCustomizedPreference(PreferenceScreen preferenceScreen);

    /**
     * Add customized Preference.
     * @param preferenceScreen The {@link PreferenceScreen} that the
     *        preference is located in.
     * @param preference The preference that was clicked.
     * @return Whether the click was handled.
     * @internal
     */
    boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference);

    /**
     * Call back of AudioProfileSetting fragment resumed.
     * @param fragment The instance of AudioProifileSettings fragment
     * @internal
     */
    void onAudioProfileSettingResumed(PreferenceFragment fragment);

    /**
     * Call back of AudioProfileSettin Activity paused.
     * @param fragment The instance of AudioProifileSettings fragment
     * @internal
     */
    void onAudioProfileSettingPaused(PreferenceFragment fragment);

    /**
     * Check if audio preference is editable.
     * @return default can't editable, so return false.
     * @internal
     */
    boolean isOtherAudioProfileEditable();

}
