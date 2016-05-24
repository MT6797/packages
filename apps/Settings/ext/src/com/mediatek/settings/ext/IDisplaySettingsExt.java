package com.mediatek.settings.ext;

import android.content.Context;
import android.preference.PreferenceScreen;


/**
 *  the class for dispaly settings feature plugin.
 */
public interface IDisplaySettingsExt {

    /**
     * add display extended preference.
     * @param context The Context of the screen
     * @param screen PreferenceScreen
     */
    void addPreference(Context context, PreferenceScreen screen);

}
