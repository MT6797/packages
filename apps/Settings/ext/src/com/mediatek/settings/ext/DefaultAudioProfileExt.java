package com.mediatek.settings.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.media.RingtoneManager;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

/**
 * The default implement for IAudioProfileExt.
 */
public class DefaultAudioProfileExt extends ContextWrapper implements
        IAudioProfileExt {

    /**
     * Constructor.
     * @param context Application context
     */
    public DefaultAudioProfileExt(Context context) {
        super(context);
    }

    @Override
    public void setRingtonePickerParams(Intent intent) {
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_MORE_RINGTONES,
                false);
    }

    @Override
    public void addCustomizedPreference(PreferenceScreen preferenceScreen) {
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        return false;
    }

    @Override
    public void onAudioProfileSettingResumed(PreferenceFragment fragment) {
    }

    @Override
    public void onAudioProfileSettingPaused(PreferenceFragment fragment) {
    }

    /**
     * Check if audio preference is editable.
     *@return default can't editable, so return false.
     */
    @Override
    public boolean isOtherAudioProfileEditable() {
        return false;
    }
}
