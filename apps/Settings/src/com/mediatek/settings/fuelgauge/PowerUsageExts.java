package com.mediatek.settings.fuelgauge;

import android.content.Context;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.mediatek.settings.FeatureOption;

public class PowerUsageExts {

    private static final String TAG = "PowerUsageSummary";

    private static final String KEY_BACKGROUND_POWER_SAVING = "background_power_saving";
    // Declare the first preference BgPowerSavingPrf order here,
    // other preference order over this value.
    private static final int PREFERENCE_ORDER_FIRST = -100;
    private Context mContext;
    private PreferenceScreen mPowerUsageScreen;
    private SwitchPreference mBgPowerSavingPrf;
    // performance and power balance {@
    public static final String KEY_PERFORMANCE_AND_POWER = "performance_and_power";
    private static final String PROPERTY_POWER_MODE = "persist.sys.power.mode";
    private ListPreference mPerformancePower;
    // @}

    public PowerUsageExts(Context context, PreferenceScreen appListGroup) {
        mContext = context;
        mPowerUsageScreen = appListGroup;
    }

    // init power usage extends items
    public void initPowerUsageExtItems() {
        // background power saving
        if (FeatureOption.MTK_BG_POWER_SAVING_SUPPORT
                && FeatureOption.MTK_BG_POWER_SAVING_UI_SUPPORT) {
            mBgPowerSavingPrf = new SwitchPreference(mContext);
            mBgPowerSavingPrf.setKey(KEY_BACKGROUND_POWER_SAVING);
            mBgPowerSavingPrf.setTitle(R.string.bg_power_saving_title);
            mBgPowerSavingPrf.setOrder(PREFERENCE_ORDER_FIRST + 1);
            mBgPowerSavingPrf.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.BG_POWER_SAVING_ENABLE, 1) != 0);
            mPowerUsageScreen.addPreference(mBgPowerSavingPrf);
        }
        // performance and power balance {@
        if (FeatureOption.MTK_POWER_PERFORMANCE_STRATEGY_SUPPORT) {
            mPerformancePower = new ListPreference(mContext);
            mPerformancePower.setKey(KEY_PERFORMANCE_AND_POWER);
            mPerformancePower.setTitle(R.string.performance_and_power_title);
            mPerformancePower.setEntries(R.array.performance_and_power_entries);
            mPerformancePower.setEntryValues(R.array.performance_and_power_values);
            mPerformancePower.setOrder(PREFERENCE_ORDER_FIRST);
            int performanceAndPowertype =
                Integer.valueOf(SystemProperties.get(PROPERTY_POWER_MODE, "0"));
            mPerformancePower.setSummary(mContext.getResources()
                .getStringArray(R.array.performance_and_power_entries)[performanceAndPowertype]);
            mPerformancePower.setValueIndex(performanceAndPowertype);
            mPowerUsageScreen.addPreference(mPerformancePower);
        }
        // @}
    }

    // on click
    public boolean onPowerUsageExtItemsClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (KEY_BACKGROUND_POWER_SAVING.equals(preference.getKey())) {
            if (preference instanceof SwitchPreference) {
                SwitchPreference pref = (SwitchPreference) preference;
                int bgState = pref.isChecked() ? 1 : 0;
                Log.d(TAG, "background power saving state: " + bgState);
                Settings.System.putInt(mContext.getContentResolver(),
                        Settings.System.BG_POWER_SAVING_ENABLE, bgState);
                if (mBgPowerSavingPrf != null) {
                    mBgPowerSavingPrf.setChecked(pref.isChecked());
                }
            }
            // If user click on PowerSaving preference just return here
            return true;
        }
        return false;
    }

    // performance and power balance {@
    /**
     * Called when a Preference has been changed by the user. This is
     * called before the state of the Preference is about to be updated and
     * before the state is persisted.
     *
     * @param preference The changed Preference.
     * @param newValue The new value of the Preference.
     * @return True to update the state of the Preference with the new value.
     */
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (KEY_PERFORMANCE_AND_POWER.equals(preference.getKey())) {
            Log.d(TAG, "onPreferenceChange KEY_PERFORMANCE_AND_POWER ");
            setPerformaceAndPowerType(Integer.parseInt(newValue.toString()));
            return true;
        }
        return false;
    }

    private void setPerformaceAndPowerType(int type) {
        Log.d(TAG, "setPerformaceAndPowerType : " + type);
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        switch (type) {
        case 0:
            pm.powerHint(PowerManager.POWER_HINT_PERFORMANCE_BOOST, 0);
            break;
        case 1:
            pm.powerHint(PowerManager.POWER_HINT_BALANCE, 0);
            break;
        case 2:
            pm.powerHint(PowerManager.POWER_HINT_POWER_SAVING, 0);
            break;
        default:
            break;
        }
        mPerformancePower.setSummary(mContext.getResources()
                .getStringArray(R.array.performance_and_power_entries)[type]);
        mPerformancePower.setValueIndex(type);
        SystemProperties.set(PROPERTY_POWER_MODE, String.valueOf(type));
    }
    // @}
}
