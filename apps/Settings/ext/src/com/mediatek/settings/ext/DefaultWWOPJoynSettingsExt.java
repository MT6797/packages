package com.mediatek.settings.ext;

import android.util.Log;

public class DefaultWWOPJoynSettingsExt implements IWWOPJoynSettingsExt {
    private static final String TAG = "DefaultWWOPJoynSettingsExt";

    /**
     * If true, Add rcs setting preference in wireless settings.
     * @return true if plug-in want to go add joyn settings.
     */
    public boolean isJoynSettingsEnabled() {
        Log.d("@M_" + TAG, "isJoynSettingsEnabled");
        return false;
    }

}
