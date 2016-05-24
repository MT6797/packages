package com.mediatek.settings.ext;

import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.widget.ImageView;
/**
 *  the class for settings misc feature plugin.
 */
public interface ISettingsMiscExt {

    /**
    * Customize strings which contains 'SIM', replace 'SIM' by
    * 'UIM/SIM','UIM','card' etc.
    * @param simString : the strings which contains SIM
    * @param soltId : 1 , slot1 0, slot0 , -1 means always.
    * @internal
    */
    String customizeSimDisplayString(String simString, int slotId);

    /**
     * Add the operator customize settings in Settings->Location
     * @param pref: the root preferenceScreen
     * @param order: the customize settings preference order
     * @internal
     */
    void initCustomizedLocationSettings(PreferenceScreen root, int order);

    /**
     * Add the operator customize settings in Settings->Location
     * Update customize settings when location mode changed
     * @internal
     */
    void updateCustomizedLocationSettings();

    /**Customize the title of factory reset settings.
     * @param obj header or activity
     * @internal
     */
    void setFactoryResetTitle(Object obj);

    /**Customize the title of screen timeout preference.
     * @param pref the screen timeout preference
     * @internal
     */
    void setTimeoutPrefTitle(Preference pref);

    /**
     * Add customize item in settings.
     * @param targetDashboardCategory header list in settings,
     *  set to object so that settings.ext do not depend on settings
     * @param add whether add operator dashboard tile
     * @internal
     */
    void addCustomizedItem(Object targetDashboardCategory, Boolean add);

    /**
     * Customize add item drawable and location.
     * @param tile the new DashboardTile which create in CT will add intent.extra.
     * @param imageView for dashboardTile imageView set the drawable
     * @internal
     */
    void customizeDashboardTile(Object tile, ImageView imageView);

    /**
     * Returns if wifi-only mode is set.
     * @return boolean
     */
    boolean isWifiOnlyModeSet();
}
