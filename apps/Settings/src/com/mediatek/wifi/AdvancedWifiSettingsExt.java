package com.mediatek.wifi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.mediatek.settings.ext.IWifiExt;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;

public class AdvancedWifiSettingsExt {
    private static final String TAG = "AdvancedWifiSettingsExt";
    private static final String KEY_SLEEP_POLICY = "sleep_policy";
    private static final String KEY_NOTIFY_OPEN_NETWORKS = "notify_open_networks";
    private WifiManager mWifiManager;
    private static final String KEY_CURRENT_IP_ADDRESS = "current_ip_address";
    private static final String KEY_CURRENT_IPV6_ADDRESS = "current_ipv6_address";
    private static final String KEY_MAC_ADDRESS = "mac_address";
    private SettingsPreferenceFragment mFragment;
    private Activity mActivity;
    private IWifiExt mExt;

    //bug fix for refresh notifiyOpenNetworks when turn on/off wifi
    private IntentFilter mIntentFilter;
    private SwitchPreference mNotifyOpenNetworks;

    // add for DHCPV6
    protected static final int NOT_FOUND_STRING = -1;
    private static final int ONLY_ONE_IP_ADDRESS = 1;
    private Preference mMacAddressPref;
    private Preference mIpAddressPref;
    private Preference mIpv6AddressPref;

    /**
     * bug fix for refresh notifiyOpenNetworks when turn on/off wifi
     */
   private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
       @Override
       public void onReceive(Context context, Intent intent) {
           String action = intent.getAction();
           if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
               int state = intent.getIntExtra(
                       WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
               if (state == WifiManager.WIFI_STATE_ENABLED) {
                   mNotifyOpenNetworks.setEnabled(true);
               } else if (state == WifiManager.WIFI_STATE_DISABLED) {
                   mNotifyOpenNetworks.setEnabled(false);
               }
           }
       }
   };

    public AdvancedWifiSettingsExt(SettingsPreferenceFragment fragment) {
        Log.d(TAG , "AdvancedWifiSettingsExt");
        mFragment = fragment;
        if (fragment != null) {
            mActivity = fragment.getActivity();
        }
    }

    /**
     * create plugin
     */
    public void onCreate() {
        Log.d(TAG , "onCreate");
        mExt = UtilsExt.getWifiPlugin(mActivity);
        mIntentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
    }

    /**
     * init some views: plugin view and ipv4 ipv6
     * @param cr
     */
    public void onActivityCreated(ContentResolver cr) {
        Log.d(TAG , "onActivityCreated");
        //init view
        mExt.initConnectView(mActivity, mFragment.getPreferenceScreen());
        mWifiManager = (WifiManager) mActivity.getSystemService(Context.WIFI_SERVICE);
        mExt.initPreference(cr);
        addWifiInfoPreference();
        mExt.initNetworkInfoView(mFragment.getPreferenceScreen());

    }

    /**
     * update views
     */
    public void onResume() {
        Log.d(TAG , "onResume");
        initPreferences();
        refreshWifiInfo();
        // register receiver
        mActivity.registerReceiver(mReceiver, mIntentFilter);

    }

    public void onPause() {
        // unregister receiver
        mActivity.unregisterReceiver(mReceiver);
    }

    private void initPreferences() {
        mNotifyOpenNetworks = (SwitchPreference) mFragment.findPreference(KEY_NOTIFY_OPEN_NETWORKS);
        ListPreference sleepPolicyPref = (ListPreference) mFragment.
                findPreference(KEY_SLEEP_POLICY);
        if (sleepPolicyPref != null) {
            mExt.setSleepPolicyPreference(sleepPolicyPref,
                mFragment.getResources().getStringArray((Utils.isWifiOnly(mActivity)
                ? R.array.wifi_sleep_policy_entries_wifi_only : R.array.wifi_sleep_policy_entries)),
                mFragment.getResources().getStringArray(R.array.wifi_sleep_policy_values));

        }
    }

    /**
     * update WifiInfo, ipv4/ipv6 and plugin's wifiInfo
     */
    private void refreshWifiInfo() {

        if (FeatureOption.MTK_DHCPV6C_WIFI) {
            //refresh wifi ip address preference
            String ipAddress = UtilsExt.getWifiIpAddresses();
            Log.d(TAG, "refreshWifiInfo, the ipAddress is : " + ipAddress);
            if (ipAddress != null) {
                String[] ipAddresses = ipAddress.split(", ");
                int ipAddressesLength = ipAddresses.length;
                Log.d(TAG, "ipAddressesLength is : " + ipAddressesLength);
                for (int i = 0; i < ipAddressesLength; i++) {
                    if (ipAddresses[i].indexOf(":") == NOT_FOUND_STRING) {
                        Log.d(TAG, "ipAddresses[i] is : " + ipAddresses[i]);
                        mIpAddressPref.setSummary(ipAddresses[i] == null ?
                                mActivity.getString(R.string.status_unavailable) : ipAddresses[i]);
                        if (ipAddressesLength == ONLY_ONE_IP_ADDRESS) {
                            mFragment.getPreferenceScreen().removePreference(mIpv6AddressPref);
                        }
                    } else  {
                        //set one ipv6 address one line
                        String ipSummary = "";
                        if(ipAddresses[i] == null) {
                            ipSummary = mActivity.getString(R.string.status_unavailable);
                        } else {
                            String[] ipv6Addresses = ipAddresses[i].split("; ");
                            for (int j = 0; j < ipv6Addresses.length; j++) {
                                ipSummary += ipv6Addresses[j]+"\n";
                            }
                        }
                        mIpv6AddressPref.setSummary(ipSummary);
                        //mIpv6AddressPref.setSummary(ipAddresses[i] == null ?
                        //mActivity.getString(R.string.status_unavailable) : ipAddresses[i]);
                        if (ipAddressesLength == ONLY_ONE_IP_ADDRESS) {
                            mFragment.getPreferenceScreen().removePreference(mIpAddressPref);
                        }
                    }
                }
            } else {
                mFragment.getPreferenceScreen().removePreference(mIpv6AddressPref);
                setDefaultIPAddress();
            }
        } else {
            setDefaultIPAddress();
        }

        mExt.refreshNetworkInfoView();
    }

    private void setDefaultIPAddress()  {
        String ipAddress = Utils.getWifiIpAddresses(mActivity);
        Log.d(TAG, "default ipAddress = " + ipAddress);
        if (mIpAddressPref == null) {
            return;
        }
        mIpAddressPref.setSummary(ipAddress == null ?
            mActivity.getString(R.string.status_unavailable) : ipAddress);
    }
    /**
     *  add wifi mac & ip address preference
     */
    private void addWifiInfoPreference() {
        mMacAddressPref = mFragment.findPreference(KEY_MAC_ADDRESS);
        mIpAddressPref = mFragment.findPreference(KEY_CURRENT_IP_ADDRESS);
        if(mMacAddressPref == null || mIpAddressPref == null) {
            return;
        }
        PreferenceScreen screen = mFragment.getPreferenceScreen();
        // set macaddress and ipaddress's order
        int order = 0;
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            Preference preference = screen.getPreference(i);
            if (!KEY_MAC_ADDRESS.equals(preference.getKey())
                    && !KEY_CURRENT_IP_ADDRESS.equals(preference.getKey())) {
                preference.setOrder(order++);
            }
        }
        mMacAddressPref.setOrder(order++);
        if (mIpAddressPref != null) {
            mIpAddressPref.setOrder(order++);
        }

        //add ipv6 preference
        if (FeatureOption.MTK_DHCPV6C_WIFI) {
            mIpAddressPref.setTitle(R.string.wifi_advanced_ipv4_address_title);
            mIpv6AddressPref = new Preference(mActivity, null,
                    android.R.attr.preferenceInformationStyle);
            mIpv6AddressPref.setTitle(R.string.wifi_advanced_ipv6_address_title);
            mIpv6AddressPref.setKey(KEY_CURRENT_IPV6_ADDRESS);
            mFragment.getPreferenceScreen().addPreference(mIpv6AddressPref);
        }
    }

}
