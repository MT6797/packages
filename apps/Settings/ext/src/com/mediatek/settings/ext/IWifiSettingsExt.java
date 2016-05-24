package com.mediatek.settings.ext;

import android.content.ContentResolver;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiConfiguration;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.view.ContextMenu;

public interface IWifiSettingsExt {

    /**
     * Called when register priority observer
     * @param contentResolver The parent contentResolver
     * @internal
     */
    void registerPriorityObserver(ContentResolver contentResolver);
    /**
     * Called when unregister priority observer
     * @param contentResolver The parent contentResolver
     * @internal
     */
    void unregisterPriorityObserver(ContentResolver contentResolver);
    /**
     * Remember the configration of last connected access point
     * @param config The configration of last connected access point
     * @internal
     */
    void setLastConnectedConfig(WifiConfiguration config);
    /**
     * Update priority for access point
     * @internal
     */
    void updatePriority();
    /**
     * update the context menu for the current selected access point
     * @internal
     */
    void updateContextMenu(ContextMenu menu, int menuId, DetailedState state);
    /**
     * Remove all prefereces in every catogory
     * @param screen The parent screen
     * @internal
     */
    void emptyCategory(PreferenceScreen screen);
    /**
     * Remove all prefereces in the screen
     * @param screen The parent screen
     * @internal
     */
    void emptyScreen(PreferenceScreen screen);
    /**
     * Refresh the category
     * @param screen The parent screen
     * @internal
     */
    void refreshCategory(PreferenceScreen screen);
    /**
     * Record priority of the selected access points
     * @param selectPriority The priority of the selected access points
     * @internal
     */
    void recordPriority(int selectPriority);
    /**
     * update priority of access points
     * @param config The configuration of the latest connect access point
     * @internal
     */
    void setNewPriority(WifiConfiguration config);
    /**
     * update priority of access points after click submit button
     * @param config The configuration of the wifi dialog
     * @internal
     */
    void updatePriorityAfterSubmit(WifiConfiguration config);
    /**
     * Disconnect current connected access point
     * @param networkId The network id of the access point
     * @internal
     */
    void disconnect(WifiConfiguration wifiConfig);

    /**
     * add all accessPoints to screen
     * @param screen The current screen
     * @param preference the current AccessPoint
     * @param isConfiged:true or false; false:  newCategory; true: add to common screen
     * @internal
     */
    void addPreference(PreferenceScreen screen, Preference preference, boolean isConfiged);

    /**
     * add all Category to screen
     * @param screen The current screen
     * @internal
     */
    void addCategories(PreferenceScreen screen);
}
