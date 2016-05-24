package com.mediatek.settings.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.widget.ImageView;

/**Dummy implmentation , do nothing.
 */
public class DefaultSettingsMiscExt extends ContextWrapper implements ISettingsMiscExt {
    static final String TAG = "DefaultSettingsMiscExt";
    public DefaultSettingsMiscExt(Context base) {
        super(base);
    }

    public String customizeSimDisplayString(String simString, int slotId) {
        return simString;
    }

    public void initCustomizedLocationSettings(PreferenceScreen root, int order) {
    }

    public void updateCustomizedLocationSettings() {
    }

    public void setFactoryResetTitle(Object obj) {
    }

    public void setTimeoutPrefTitle(Preference pref) {

    }

    @Override
    public void addCustomizedItem(Object targetDashboardCategory, Boolean add) {
        android.util.Log.i(TAG, "DefaultSettingsMisc addCustomizedItem method going");
    }

   @Override
    public void customizeDashboardTile(Object tile, ImageView tileIcon) {
    }

   @Override
    public boolean isWifiOnlyModeSet() {
       return false;
    }
}

