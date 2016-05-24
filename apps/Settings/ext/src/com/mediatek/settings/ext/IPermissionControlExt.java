package com.mediatek.settings.ext;


import android.preference.PreferenceGroup;
import android.provider.SearchIndexableData;

import java.util.List;

public interface IPermissionControlExt {

    /**
     * to add a permission control button.
     * @param prefGroup The added preference parent group
     * @internal
     */
    public void addPermSwitchPrf(PreferenceGroup prefGroup);

    /**
     * Resume callback.
     * @internal
     */
    public void enablerResume();

    /**
     * Pause callback.
     * @internal
     */
    public void enablerPause();

    /**
     * Add auto boot preference.
     * @param prefGroup The added preference parent group
     * @internal
     */
    public void addAutoBootPrf(PreferenceGroup prefGroup);

    /**
     * For search index.
     * @param enabled
     * @return
     * @internal
     */
    public  List<SearchIndexableData> getRawDataToIndex(boolean enabled);

}