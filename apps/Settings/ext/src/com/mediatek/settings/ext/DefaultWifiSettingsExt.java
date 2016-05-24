package com.mediatek.settings.ext;

import android.content.ContentResolver;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiConfiguration;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.view.ContextMenu;

/* Dummy implmentation , do nothing */
public class DefaultWifiSettingsExt implements IWifiSettingsExt {
    private static final String TAG = "DefaultWifiSettingsExt";

    public void registerPriorityObserver(ContentResolver contentResolver) {
    }
    public void unregisterPriorityObserver(ContentResolver contentResolver) {
    }
    public void setLastConnectedConfig(WifiConfiguration config) {
    }
    public void updatePriority() {
    }
    public  void updateContextMenu(ContextMenu menu, int menuId, DetailedState state) {

    }
    public void emptyCategory(PreferenceScreen screen) {
        screen.removeAll();
    }
    public void emptyScreen(PreferenceScreen screen) {
        screen.removeAll();
    }
    public void refreshCategory(PreferenceScreen screen) {
    }
    public void recordPriority(int selectPriority) {
    }
    public void setNewPriority(WifiConfiguration config) {
    }
    public void updatePriorityAfterSubmit(WifiConfiguration config) {
    }
    public void disconnect(WifiConfiguration wifiConfig) {
    }
    public void addPreference(PreferenceScreen screen, Preference preference, boolean isConfiged) {
        if (screen != null) {
            screen.addPreference(preference);
        }
    }
    public void addCategories(PreferenceScreen screen) {

    }
}
