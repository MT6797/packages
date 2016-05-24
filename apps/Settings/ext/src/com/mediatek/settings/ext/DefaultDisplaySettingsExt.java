package com.mediatek.settings.ext;

import android.content.Context;
import android.preference.PreferenceScreen;

/**Dummy implmentation , do nothing.
 */
public class DefaultDisplaySettingsExt implements IDisplaySettingsExt {
    private static final String TAG = "DefaultDisplaySettingsExt";
    private Context mContext;

    /**
     * DefaultDisplaySettingsExt.
     * @param context The Context
     */
    public DefaultDisplaySettingsExt(Context context) {
        mContext = context;
    }

    @Override
    public void addPreference(Context context, PreferenceScreen screen) {
    }
}
