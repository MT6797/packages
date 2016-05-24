package com.mediatek.settings.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.preference.PreferenceGroup;
import android.provider.SearchIndexableData;
import android.util.Log;

import java.util.List;

public class DefaultPermissionControlExt extends ContextWrapper implements IPermissionControlExt {
    private static final String TAG = "DefaultPermissionControlExt";
    public DefaultPermissionControlExt(Context context) {
        super(context);
    }

    public void addPermSwitchPrf(PreferenceGroup prefGroup) {
        Log.d(TAG, "will not add permission preference");
    }

    public void enablerResume() {
        Log.d(TAG, "enablerResume() default");
    }

    public void enablerPause() {
        Log.d(TAG, "enablerPause() default");
    }

    public void addAutoBootPrf(PreferenceGroup prefGroup) {
        Log.d(TAG, "will not add auto boot entry preference");
    }

    public List<SearchIndexableData> getRawDataToIndex(boolean enabled) {
        Log.d(TAG, "default , null");
        return null;
    }

}
