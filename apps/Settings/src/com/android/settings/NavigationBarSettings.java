package com.android.settings;

import java.util.List;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;

import com.android.settings.search.Indexable;
import com.android.settings.widget.RadioPreference;
import com.mediatek.settings.FeatureOption;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
public class NavigationBarSettings extends SettingsPreferenceFragment implements
		Indexable {
	private final String TAG = "NavigationBarSettings";
	private RadioPreference mNavigationMode1;
	private RadioPreference mNavigationMode2;
	private RadioPreference mNavigationMode3;
	private RadioPreference mNavigationMode4;
	private SwitchPreference mNavigationSwitch;
	private final String NAVIGATION_MODE1 = "navigation_mode1";
	private final String NAVIGATION_MODE2 = "navigation_mode2";
	private final String NAVIGATION_MODE3 = "navigation_mode3";
	private final String NAVIGATION_MODE4 = "navigation_mode4";
	private final String NAVIGATION_SWITCH = "navigation_switch";

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		addPreferencesFromResource(R.xml.navigation_bar_settings);
		mNavigationMode1 = (RadioPreference) findPreference(NAVIGATION_MODE1);
		mNavigationMode1.setImg(R.drawable.virtual_key_type_1);
		mNavigationMode2 = (RadioPreference) findPreference(NAVIGATION_MODE2);
		mNavigationMode2.setImg(R.drawable.virtual_key_type_2);
		mNavigationMode3 = (RadioPreference) findPreference(NAVIGATION_MODE3);
		mNavigationMode3.setImg(R.drawable.virtual_key_type_3);
		mNavigationMode4 = (RadioPreference) findPreference(NAVIGATION_MODE4);
		mNavigationMode4.setImg(R.drawable.virtual_key_type_4);

		mNavigationSwitch = (SwitchPreference) findPreference(NAVIGATION_SWITCH);

	}

	@Override
	public void onResume() {
		super.onResume();
		updatePreference();
	}
    	@Override
    	protected int getMetricsCategory() {
        	return MetricsLogger.NAVIGATION_BAR_CUSTOM;
    	}
	private void updatePreference() {
		Log.d(TAG, "updatePreference");
		int mode = Settings.System.getInt(getContentResolver(),
				android.provider.Settings.System.NAVIGATION_BAR_MODE, -1);
		mNavigationMode1.setChecked(false);
		mNavigationMode2.setChecked(false);
		mNavigationMode3.setChecked(false);
		mNavigationMode4.setChecked(false);
		switch (mode) {
		case 1:
			mNavigationMode1.setChecked(true);
			break;
		case 2:
			mNavigationMode2.setChecked(true);
			break;
		case 3:
			mNavigationMode3.setChecked(true);
			break;
		case 4:
			mNavigationMode4.setChecked(true);
			break;
		default:
			mNavigationMode1.setChecked(true);
			break;
		}
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		final String key = preference.getKey();
		if (NAVIGATION_SWITCH.equals(key)) {
			Settings.System.putInt(getContentResolver(),
					android.provider.Settings.System.NAVIGATION_BAR_HIDE,
					mNavigationSwitch.isChecked() ? 1 : 0);
			if (mNavigationSwitch.isChecked()) {
				int mode = Settings.System.getInt(getContentResolver(),
						android.provider.Settings.System.NAVIGATION_BAR_MODE,
						-1);
				if (mode == -1)
					Settings.System
							.putInt(getContentResolver(),
									android.provider.Settings.System.NAVIGATION_BAR_MODE,
									1);
			}
		} else if (NAVIGATION_MODE1.equals(key)) {
			Settings.System.putInt(getContentResolver(),
					android.provider.Settings.System.NAVIGATION_BAR_MODE, 1);
		} else if (NAVIGATION_MODE2.equals(key)) {
			Settings.System.putInt(getContentResolver(),
					android.provider.Settings.System.NAVIGATION_BAR_MODE, 2);
		} else if (NAVIGATION_MODE3.equals(key)) {
			Settings.System.putInt(getContentResolver(),
					android.provider.Settings.System.NAVIGATION_BAR_MODE, 3);
		} else if (NAVIGATION_MODE4.equals(key)) {
			Settings.System.putInt(getContentResolver(),
					android.provider.Settings.System.NAVIGATION_BAR_MODE, 4);
		}
		updatePreference();
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

}
