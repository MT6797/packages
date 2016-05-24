package com.mediatek.wifi;

import static android.net.wifi.WifiConfiguration.INVALID_NETWORK_ID;

import android.content.ContentResolver;
import android.content.Context;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig.Eap;
import android.net.wifi.WifiManager;
import android.os.ServiceManager;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.settingslib.wifi.AccessPoint;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.mediatek.settings.ext.IWifiSettingsExt;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;

import com.mediatek.telephony.TelephonyManagerEx;

public class WifiSettingsExt {
    private static final String TAG = "WifiSettingsExt";
    private static final int MENU_INDEX_WPS_PBC = 0;
    private static final int MENU_INDEX_ADD_NETWORK = 1;
    private static final int MENU_ID_DISCONNECT = Menu.FIRST + 100;

    private static final String TRUST_AP = "trust_access_points";
    private static final String CONFIGED_AP = "configed_access_points";
    private static final String NEW_AP = "new_access_points";

    // add for plug in
    IWifiSettingsExt mExt;

    // Wifi Wps EM
    WifiWpsP2pEmSettings mWpsP2pEmSettings;

    private Context mActivity;

    public WifiSettingsExt(Context context) {
        mActivity = context;
    }

    public void onCreate() {
        // get plug in
        mExt = UtilsExt.getWifiSettingsPlugin(mActivity);
    }

    public void onActivityCreated(SettingsPreferenceFragment fragment,
                       WifiManager wifiManager) {
        // register priority observer
        mExt.registerPriorityObserver(mActivity.getContentResolver());

        // Wifi Wps EM
        if (FeatureOption.MTK_WIFIWPSP2P_NFC_SUPPORT) {
            mWpsP2pEmSettings = new WifiWpsP2pEmSettings(mActivity, wifiManager);
        }

        mExt.addCategories(fragment.getPreferenceScreen());
    }

    public void updatePriority() {
        // update priority after connnect AP
        Log.d(TAG, "mConnectListener or mSaveListener");
        mExt.updatePriority();
    }

    public void onResume() {
        // Wifi Wps EM
        if (FeatureOption.MTK_WIFIWPSP2P_NFC_SUPPORT && mWpsP2pEmSettings != null) {
            mWpsP2pEmSettings.resume();
        }

        //update priority when resume
        mExt.updatePriority();
    }

    /**
     * 1. fix menu bug 2. add WPS NFC feature
     * @param setupWizardMode
     * @param wifiIsEnabled
     * @param menu
     */
    public void onCreateOptionsMenu(boolean wifiIsEnabled, Menu menu) {
        // Wifi Wps EM
        if (FeatureOption.MTK_WIFIWPSP2P_NFC_SUPPORT && mWpsP2pEmSettings != null) {
            mWpsP2pEmSettings.createOptionsMenu(menu);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (FeatureOption.MTK_WIFIWPSP2P_NFC_SUPPORT && mWpsP2pEmSettings != null) {
            return mWpsP2pEmSettings.optionsItemSelected(item);
        }
        return false;
    }

    /**
     * 1. add cmcc disconnect menu 2. WPS NFC feature
     * @param menu
     * @param state
     * @param accessPoint
     */
    public void onCreateContextMenu(ContextMenu menu, DetailedState state,
                            AccessPoint accessPoint) {
        //current connected AP, add a disconnect option to it
        mExt.updateContextMenu(menu, MENU_ID_DISCONNECT, state);
    }

    public boolean onContextItemSelected(MenuItem item, WifiConfiguration wifiConfig) {
        switch (item.getItemId()) {
            case MENU_ID_DISCONNECT:
                mExt.disconnect(wifiConfig);
                return true;
             default:
                    break;
        }

        return false;
    }

    public void recordPriority(WifiConfiguration config) {
        // record priority of selected ap
        if (config != null) {
            //store the former priority value before user modification
            mExt.recordPriority(config.priority);
        } else {
            //the last added AP will have highest priority, mean all other
            //AP's priority will be adjusted, the same as adjust this new
            //added one's priority from lowest to highest
            mExt.recordPriority(-1);
        }
    }

    public void submit(WifiConfiguration config, AccessPoint accessPoint,
                                DetailedState state) {
        Log.d(TAG, "submit, config = " + config);
        if (config == null) {
            /*if (accessPoint != null && networkId != INVALID_NETWORK_ID && state != null) {
                Log.d(TAG, "submit, disconnect, networkId = " + networkId);
                mExt.disconnect(networkId);
            }*/
        } else if (config.networkId != INVALID_NETWORK_ID && accessPoint != null) {
            // save priority
            Log.d(TAG, "submit, setNewPriority");
            mExt.setNewPriority(config);
        } else {
            // update priority
            Log.d(TAG, "submit, updatePriorityAfterSubmit");
            mExt.updatePriorityAfterSubmit(config);
        }
        // set last connected config
        Log.d(TAG, "submit, setLastConnectedConfig");
        mExt.setLastConnectedConfig(config);

    }

    public void unregisterPriorityObserver(ContentResolver cr) {
        mExt.unregisterPriorityObserver(cr);
    }

    public void addPreference(PreferenceScreen screen,
                    Preference preference, boolean isConfiged) {
        mExt.addPreference(screen, preference, isConfiged);
    }

    public void emptyCategory(PreferenceScreen screen) {
        mExt.emptyCategory(screen);
    }

    public void emptyScreen(PreferenceScreen screen) {
        mExt.emptyScreen(screen);
    }

    public void refreshCategory(PreferenceScreen screen) {
        mExt.refreshCategory(screen);
    }
}
