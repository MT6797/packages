package com.mediatek.settings.ext;

import android.preference.PreferenceGroup;

public interface IMdmPermissionControlExt {
    /**
     * to add a phone security lock button
     * @param prefGroup The added preference parent group
     * @internal
     */
    public void addMdmPermCtrlPrf(PreferenceGroup prefGroup);
}
