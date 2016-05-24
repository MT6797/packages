/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.cellbroadcastreceiver;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.os.UserManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
// add for gemini
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
//import android.provider.Telephony.SIMInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;

import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings activity for the cell broadcast receiver.
 */
public class CellBroadcastSettings extends PreferenceActivity {
    public static final String TAG = "[ETWS]CellBroadcastSettings";
    // Preference key for whether to enable emergency notifications (default enabled).
    public static final String KEY_ENABLE_EMERGENCY_ALERTS = "enable_emergency_alerts";

    // Duration of alert sound (in seconds).
    public static final String KEY_ALERT_SOUND_DURATION = "alert_sound_duration";

    // Default alert duration (in seconds).
    public static final String ALERT_SOUND_DEFAULT_DURATION = "4";

    // Enable vibration on alert (unless master volume is silent).
    public static final String KEY_ENABLE_ALERT_VIBRATE = "enable_alert_vibrate";

    // Speak contents of alert after playing the alert sound.
    public static final String KEY_ENABLE_ALERT_SPEECH = "enable_alert_speech";

    // Preference category for emergency alert and CMAS settings.
    public static final String KEY_CATEGORY_ALERT_SETTINGS = "category_alert_settings";

    // Preference category for ETWS related settings.
    public static final String KEY_CATEGORY_ETWS_SETTINGS = "category_etws_settings";

    // Whether to display CMAS extreme threat notifications (default is enabled).
    public static final String KEY_ENABLE_CMAS_EXTREME_THREAT_ALERTS =
            "enable_cmas_extreme_threat_alerts";

    // Whether to display CMAS severe threat notifications (default is enabled).
    public static final String KEY_ENABLE_CMAS_SEVERE_THREAT_ALERTS =
            "enable_cmas_severe_threat_alerts";

    // Whether to display CMAS amber alert messages (default is enabled).
    public static final String KEY_ENABLE_CMAS_AMBER_ALERTS = "enable_cmas_amber_alerts";

    // Preference category for development settings (enabled by settings developer options toggle).
    public static final String KEY_CATEGORY_DEV_SETTINGS = "category_dev_settings";

    // Whether to display ETWS test messages (default is disabled).
    public static final String KEY_ENABLE_ETWS_TEST_ALERTS = "enable_etws_test_alerts";

    // Whether to display CMAS monthly test messages (default is disabled).
    public static final String KEY_ENABLE_CMAS_TEST_ALERTS = "enable_cmas_test_alerts";

    // Preference category for Brazil specific settings.
    public static final String KEY_CATEGORY_BRAZIL_SETTINGS = "category_brazil_settings";

    // Preference key for whether to enable channel 50 notifications
    // Enabled by default for phones sold in Brazil, otherwise this setting may be hidden.
    public static final String KEY_ENABLE_CHANNEL_50_ALERTS = "enable_channel_50_alerts";

    // Preference key for initial opt-in/opt-out dialog.
    public static final String KEY_SHOW_CMAS_OPT_OUT_DIALOG = "show_cmas_opt_out_dialog";

    // Alert reminder interval ("once" = single 2 minute reminder).
    public static final String KEY_ALERT_REMINDER_INTERVAL = "alert_reminder_interval";

    // cb message filter entry
    public static final String CELL_BROADCAST = "pref_key_cell_broadcast";
    public static final String SUB_TITLE_NAME = "sub_title_name";
    private Preference mCBsettingPref;

     private Preference mAlertVolumePref;
    // add for gemini
    //private static int mSimId = -1;
     private static int mSubId = -1;

    public static final String KEY_ENABLE_ETWS_ALERT = "enable_etws_alerts";
    private MediaPlayer mMediaPlayer;
    private Handler handler;

    // Default reminder interval.
    public static final String ALERT_REMINDER_INTERVAL = "0";

    private TelephonyManager mTelephonyManager;
    private SubscriptionInfo mSir;
    private TabHost mTabHost;
    private TabWidget mTabWidget;
    private List<SubscriptionInfo> mSelectableSubInfos;

    private CheckBoxPreference mExtremeCheckBox;
    private CheckBoxPreference mSevereCheckBox;
    private CheckBoxPreference mAmberCheckBox;
    private CheckBoxPreference mEmergencyCheckBox;
    private ListPreference mAlertDuration;
    private ListPreference mReminderInterval;
    private CheckBoxPreference mVibrateCheckBox;
    private CheckBoxPreference mSpeechCheckBox;
    private CheckBoxPreference mEtwsTestCheckBox;
    private CheckBoxPreference mChannel50CheckBox;
    private CheckBoxPreference mCmasCheckBox;
    private CheckBoxPreference mOptOutCheckBox;
    private PreferenceCategory mAlertCategory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "CellBroadcastSetting::onCreate()++");

        UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
        if (userManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_CELL_BROADCASTS)) {
            setContentView(R.layout.cell_broadcast_disallowed_preference_screen);
            return;
        }

        mTelephonyManager = (TelephonyManager) getSystemService(
                Context.TELEPHONY_SERVICE);
        mSelectableSubInfos = new ArrayList<SubscriptionInfo>();
        for (int i = 0; i < mTelephonyManager.getSimCount(); i++) {
            final SubscriptionInfo sir =
                    findRecordBySlotId(getApplicationContext(), i);
            if (sir != null) {
                mSelectableSubInfos.add(sir);
            }
        }

        addPreferencesFromResource(R.xml.preferences);
        mSir = mSelectableSubInfos.size() > 0 ? mSelectableSubInfos.get(0) : null;
        if (mSelectableSubInfos.size() > 1) {
            setContentView(com.android.internal.R.layout.common_tab_settings);

            mTabHost = (TabHost) findViewById(android.R.id.tabhost);
            mTabHost.setup();
            mTabHost.setOnTabChangedListener(mTabListener);
            mTabHost.clearAllTabs();

            for (int i = 0; i < mSelectableSubInfos.size(); i++) {
                mTabHost.addTab(buildTabSpec(String.valueOf(i),
                        String.valueOf(mSelectableSubInfos.get(i).getDisplayName())));
            }
        }
        updatePreferences();
        getActionBar().setDisplayHomeAsUpEnabled(true);
        handler = new Handler();
    }

    private void updatePreferences() {

        PreferenceScreen prefScreen = getPreferenceScreen();

        if (prefScreen != null) {
            prefScreen.removeAll();
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            mEmergencyCheckBox = (CheckBoxPreference)
                    findPreference(KEY_ENABLE_EMERGENCY_ALERTS);
            mAlertDuration = (ListPreference)
                    findPreference(KEY_ALERT_SOUND_DURATION);
            mReminderInterval = (ListPreference)
                    findPreference(KEY_ALERT_REMINDER_INTERVAL);
            mVibrateCheckBox = (CheckBoxPreference)
                    findPreference(KEY_ENABLE_ALERT_VIBRATE);
            mSpeechCheckBox = (CheckBoxPreference)
                    findPreference(KEY_ENABLE_ALERT_SPEECH);
            mEtwsTestCheckBox = (CheckBoxPreference)
                    findPreference(KEY_ENABLE_ETWS_TEST_ALERTS);
            mAlertCategory = (PreferenceCategory)
                    findPreference(KEY_CATEGORY_ALERT_SETTINGS);

            if(mSir == null) {
                mEmergencyCheckBox.setEnabled(false);
                mReminderInterval.setEnabled(false);
                mAlertDuration.setEnabled(false);
                mVibrateCheckBox.setEnabled(false);
                mSpeechCheckBox.setEnabled(false);
                mEtwsTestCheckBox.setEnabled(false);
                return;
            }

            // Handler for settings that require us to reconfigure enabled channels in radio
            Preference.OnPreferenceChangeListener startConfigServiceListener =
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference pref, Object newValue) {
                    Log.i(TAG, "onPreferenceChange::pref.getKey() = " + pref.getKey());
                            int newVal = (((Boolean) newValue).booleanValue()) ? 1 : 0;

                            switch (pref.getKey()) {
                                case KEY_ENABLE_EMERGENCY_ALERTS:
                                    SubscriptionManager
                                            .setSubscriptionProperty(mSir.getSubscriptionId(),
                                                    SubscriptionManager.CB_EMERGENCY_ALERT,
                                                    newVal + "");
                                    break;
                                case KEY_ENABLE_ETWS_TEST_ALERTS:
                                    SubscriptionManager
                                            .setSubscriptionProperty(mSir.getSubscriptionId(),
                                                    SubscriptionManager.CB_ETWS_TEST_ALERT,
                                                    newVal + "");
                                    break;
                                default:
                                    Log.d(TAG, "Invalid preference changed");

                            }

                            CellBroadcastReceiver.startConfigService(pref.getContext(),
                                    mSir.getSubscriptionId());
                            return true;
                        }
                    };

            // Show extra settings when developer options is enabled in settings.
            boolean enableDevSettings = Settings.Global.getInt(getContentResolver(),
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;

            boolean showEtwsSettings = SubscriptionManager.getResourcesForSubId(
                    getApplicationContext(), mSir.getSubscriptionId())
                    .getBoolean(R.bool.show_etws_settings);

            String queryReturnVal;
            // alert reminder interval
            queryReturnVal = SubscriptionManager.getIntegerSubscriptionProperty(
                    mSir.getSubscriptionId(), SubscriptionManager.CB_ALERT_REMINDER_INTERVAL,
                    Integer.parseInt(ALERT_REMINDER_INTERVAL), this) + "";

            mReminderInterval.setValue(queryReturnVal);
            mReminderInterval.setSummary(mReminderInterval
                    .getEntries()[mReminderInterval.findIndexOfValue(queryReturnVal)]);

            mReminderInterval.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference pref, Object newValue) {
                    final ListPreference listPref = (ListPreference) pref;
                    final int idx = listPref.findIndexOfValue((String) newValue);
                    listPref.setSummary(listPref.getEntries()[idx]);
                            SubscriptionManager.setSubscriptionProperty(mSir.getSubscriptionId(),
                                    SubscriptionManager.CB_ALERT_REMINDER_INTERVAL,
                                    (String) newValue);
                    return true;
                }
            });
            //TODO: duration.setDependency(KEY_ENABLE_ETWS_ALERT);

            //TODO: alert sound volume
            // Fix build error
            Preference volume = findPreference(KEY_ALERT_SOUND_VOLUME);
            OnPreferenceClickListener l = new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Log.i(TAG, "getAlertVolumeListener onclicked ");
                    final AlertDialog.Builder dialog =
                    new AlertDialog.Builder(CellBroadcastSettings.this);
                    LayoutInflater flater = getLayoutInflater();
                    View v = flater.inflate(R.layout.alert_dialog_view, null);
                    SeekBar sb = (SeekBar) v.findViewById(R.id.seekbar);
                    // set bar's progress
                    SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(CellBroadcastSettings.this);
                    float pro = 1.0f;
                    pro = prefs.getFloat(KEY_ALERT_SOUND_VOLUME, 1.0f);
                    int progress = (int) (pro * 100);
                    if (progress < 0) {
                        progress = 0;
                    } else if (progress > 100) {
                        progress = 100;
                    }
                    //Xlog.d(TAG, "open volume setting,progress:"+progress+",pro:"+pro);
                    sb.setProgress(progress);
                    sb.setOnSeekBarChangeListener(CellBroadcastSettings.this.getSeekBarListener());
                    dialog.setTitle(R.string.alert_sound_volume)
                    .setView(v)
                    .setPositiveButton(R.string.button_dismiss, new OnClickListener() {

                        public void onClick(DialogInterface arg0, int arg1) {
                            // TODO Auto-generated method stub
                            SharedPreferences prefs =
                            PreferenceManager.getDefaultSharedPreferences
                            (CellBroadcastSettings.this);
                            SharedPreferences.Editor editor = prefs.edit();

                            editor.putFloat(KEY_ALERT_SOUND_VOLUME, mAlertVolume);

                            editor.commit();
                        }
                    })
                    .setNegativeButton(R.string.button_cancel, new OnClickListener() {

                        public void onClick(DialogInterface arg0, int arg1) {
                            // TODO Auto-generated method stub
                            arg0.dismiss();
                        }
                    })
                    .show();
                    return true;
                }
            };

            volume.setOnPreferenceClickListener(l);
            //volume.setDependency(KEY_ENABLE_ETWS_ALERT);

            boolean forceDisableEtwsCmasTest =
                    isEtwsCmasTestMessageForcedDisabled(this, mSir.getSubscriptionId());

            // Show alert settings and ETWS categories for ETWS builds and developer mode.
            if (enableDevSettings || showEtwsSettings) {
                // enable/disable all alerts
                if (mEmergencyCheckBox != null) {
                    if (SubscriptionManager.getBooleanSubscriptionProperty(mSir.getSubscriptionId(),
                            SubscriptionManager.CB_EMERGENCY_ALERT, true, this)) {
                        mEmergencyCheckBox.setChecked(true);
                    } else {
                        mEmergencyCheckBox.setChecked(false);
                    }
                    mEmergencyCheckBox.setOnPreferenceChangeListener(startConfigServiceListener);
                }

                // alert sound duration
                queryReturnVal = SubscriptionManager.getIntegerSubscriptionProperty(
                        mSir.getSubscriptionId(), SubscriptionManager.CB_ALERT_SOUND_DURATION,
                        Integer.parseInt(ALERT_SOUND_DEFAULT_DURATION), this) + "";
                mAlertDuration.setValue(queryReturnVal);
                mAlertDuration.setSummary(mAlertDuration
                        .getEntries()[mAlertDuration.findIndexOfValue(queryReturnVal)]);
                mAlertDuration.setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(Preference pref, Object newValue) {
                                final ListPreference listPref = (ListPreference) pref;
                                final int idx = listPref.findIndexOfValue((String) newValue);
                                listPref.setSummary(listPref.getEntries()[idx]);
                                SubscriptionManager.setSubscriptionProperty(
                                        mSir.getSubscriptionId(),
                                        SubscriptionManager.CB_ALERT_SOUND_DURATION,
                                        (String) newValue);
                                return true;
                            }
                        });
                if (forceDisableEtwsCmasTest) {
                    // Remove ETWS test preference.
                    prefScreen.removePreference(findPreference(KEY_CATEGORY_ETWS_SETTINGS));

                    PreferenceCategory devSettingCategory =
                            (PreferenceCategory) findPreference(KEY_CATEGORY_DEV_SETTINGS);

                    // Remove CMAS test preference.
                    if (devSettingCategory != null) {
                        devSettingCategory.removePreference(
                                findPreference(KEY_ENABLE_CMAS_TEST_ALERTS));
                    }
                }
            } else {
                // Remove general emergency alert preference items (not shown for CMAS builds).
                mAlertCategory.removePreference(findPreference(KEY_ENABLE_EMERGENCY_ALERTS));
                mAlertCategory.removePreference(findPreference(KEY_ALERT_SOUND_DURATION));
                mAlertCategory.removePreference(findPreference(KEY_ENABLE_ALERT_SPEECH));
                // Remove ETWS test preference category.
                prefScreen.removePreference(findPreference(KEY_CATEGORY_ETWS_SETTINGS));
            }

            // TODO: interval.setDependency(KEY_ENABLE_ETWS_ALERT);
            if (!enableDevSettings) {
            PreferenceCategory devSettingCategory =
                            (PreferenceCategory) findPreference(KEY_CATEGORY_DEV_SETTINGS);
            if (devSettingCategory != null) {
                prefScreen.removePreference(findPreference(KEY_CATEGORY_DEV_SETTINGS));
            }
           }

            if (mSpeechCheckBox != null) {
                if (SubscriptionManager.getBooleanSubscriptionProperty(mSir.getSubscriptionId(),
                        SubscriptionManager.CB_ALERT_SPEECH, true, this)) {
                    mSpeechCheckBox.setChecked(true);
                } else {
                    mSpeechCheckBox.setChecked(false);
                }
                mSpeechCheckBox.setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(Preference pref, Object newValue) {
                                int newVal = (((Boolean) newValue).booleanValue()) ? 1 : 0;
                                SubscriptionManager.setSubscriptionProperty(
                                        mSir.getSubscriptionId(),
                                        SubscriptionManager.CB_ALERT_SPEECH, newVal + "");
                                return true;
                            }
                        });
            }

            if (mVibrateCheckBox != null) {
                if (SubscriptionManager.getBooleanSubscriptionProperty(mSir.getSubscriptionId(),
                        SubscriptionManager.CB_ALERT_VIBRATE, true, this)) {
                    mVibrateCheckBox.setChecked(true);
                } else {
                    mVibrateCheckBox.setChecked(false);
                }
                mVibrateCheckBox.setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(Preference pref, Object newValue) {
                                int newVal = (((Boolean) newValue).booleanValue()) ? 1 : 0;
                                SubscriptionManager.setSubscriptionProperty(
                                        mSir.getSubscriptionId(),
                                        SubscriptionManager.CB_ALERT_VIBRATE, newVal + "");
                                return true;
                            }
                        });
            }

          //TODO: vibrate.setDependency(KEY_ENABLE_ETWS_ALERT);
            //TODO: speech.setDependency(KEY_ENABLE_ETWS_ALERT);
            //TODO: enableEtwsTestAlerts.setDependency(KEY_ENABLE_ETWS_ALERT);
            if (mEtwsTestCheckBox != null) {
                if (!forceDisableEtwsCmasTest &&
                        SubscriptionManager.getBooleanSubscriptionProperty(mSir.getSubscriptionId(),
                        SubscriptionManager.CB_ETWS_TEST_ALERT, false, this)) {
                    mEtwsTestCheckBox.setChecked(true);
                } else {
                    mEtwsTestCheckBox.setChecked(false);
                }
                mEtwsTestCheckBox.setOnPreferenceChangeListener(startConfigServiceListener);
            }
    }
  }

    // Check if ETWS/CMAS test message is forced disabled on the device.
    public static boolean isEtwsCmasTestMessageForcedDisabled(Context context, int subId) {

        if (context == null) {
            return false;
        }

        CarrierConfigManager configManager =
                (CarrierConfigManager) context.getSystemService(Context.CARRIER_CONFIG_SERVICE);

        if (configManager != null) {
            PersistableBundle carrierConfig =
                    configManager.getConfigForSubId(subId);

            if (carrierConfig != null) {
                return carrierConfig.getBoolean(
                        CarrierConfigManager.KEY_CARRIER_FORCE_DISABLE_ETWS_CMAS_TEST_BOOL);
            }
        }

        return false;
    }


    private OnTabChangeListener mTabListener = new OnTabChangeListener() {
        @Override
        public void onTabChanged(String tabId) {
            final int slotId = Integer.parseInt(tabId);
            mSir = mSelectableSubInfos.get(slotId);
            updatePreferences();
        }
    };

    private TabContentFactory mEmptyTabContent = new TabContentFactory() {
        @Override
        public View createTabContent(String tag) {
            return new View(mTabHost.getContext());
        }
    };

    private TabSpec buildTabSpec(String tag, String title) {
        return mTabHost.newTabSpec(tag).setIndicator(title).setContent(
                mEmptyTabContent);

    }

    public SubscriptionInfo findRecordBySlotId(Context context, final int slotId) {
        final List<SubscriptionInfo> subInfoList =
                SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        if (subInfoList != null) {
            final int subInfoLength = subInfoList.size();

            for (int i = 0; i < subInfoLength; ++i) {
                final SubscriptionInfo sir = subInfoList.get(i);
                if (sir.getSimSlotIndex() == slotId) {
                    return sir;
                }
            }
        }

        return null;
    }



    // add for gemini functions
    // the default value here is the same as preferences.xml
    public static final boolean ENABLE_EMERGENCY_ALERTS_DEFAULT = true;
    // alert sound duration default value is exist
    // alert sound volume
    public static final String KEY_ALERT_SOUND_VOLUME = "alert_sound_volume";
    private static float mAlertVolume = 1.0f;
    public static final boolean ENABLE_ETWS_TEST_ALERTS_DEFAULT = true;
    public static final boolean ENABLE_ALERT_SPEECH_DEFAULT = true;
    public static final boolean ENABLE_CMAS_EXTREME_THREAT_ALERTS_DEFAULT = true;
    public static final boolean ENABLE_CMAS_SEVERE_THREAT_ALERTS_DEFAULT = true;
    public static final boolean ENABLE_CMAS_AMBER_ALERTS_DEFAULT = false;
    public static final boolean ENABLE_CMAS_TEST_ALERTS_DEFAULT = false;
    public static final boolean ENABLE_CHANNEL_50_ALERTS_DEFAULT = true;

    private void initGeminiPreference(Intent it) {
        Log.i(TAG, "initGeminiPreference ++");
        //mSimId = getIntent().getIntExtra("sim_id", -1);
        //Xlog.d(TAG, "mSimId:" + mSimId);
        mSubId = it.getIntExtra("subscription", -1);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Preference.OnPreferenceChangeListener startConfigServiceListener =
        new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object newValue) {
        //fix build errorCellBroadcastReceiver.startConfigServiceGemini(pref.getContext(),mSubId);
                return true;
            }
        };

        // show ETWS
        CheckBoxPreference enableEtwsAlerts = (CheckBoxPreference) findPreference(KEY_ENABLE_ETWS_ALERT);
        if (enableEtwsAlerts != null) {
            String newKey = KEY_ENABLE_ETWS_ALERT + "_" + mSubId;
            enableEtwsAlerts.setKey(newKey);
            enableEtwsAlerts.setOnPreferenceChangeListener(startConfigServiceListener);

            enableEtwsAlerts.setChecked(prefs.getBoolean(newKey, true));
        }

        ListPreference alertSoundDuration = (ListPreference) findPreference(KEY_ALERT_SOUND_DURATION);
        if (alertSoundDuration != null) {
            String newKey = KEY_ALERT_SOUND_DURATION + "_" + mSubId;
            alertSoundDuration.setKey(newKey);
            alertSoundDuration.setSummary(prefs.getString(newKey, ALERT_SOUND_DEFAULT_DURATION));
            alertSoundDuration.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference pref, Object newValue) {
                    final ListPreference listPref = (ListPreference) pref;
                    final int idx = listPref.findIndexOfValue((String) newValue);
                    listPref.setSummary(listPref.getEntries()[idx]);
                    return true;
                }
            });
            alertSoundDuration.setValue(prefs.getString(newKey, ALERT_SOUND_DEFAULT_DURATION));
            alertSoundDuration.setDependency(KEY_ENABLE_ETWS_ALERT + "_" + mSubId);
        }

        // alert reminder interval
        ListPreference reminder = (ListPreference) findPreference(KEY_ALERT_REMINDER_INTERVAL);
        reminder.setKey(KEY_ALERT_REMINDER_INTERVAL + "_" + mSubId);
        reminder.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference pref, Object newValue) {
                final ListPreference listPref = (ListPreference) pref;
                final int idx = listPref.findIndexOfValue((String) newValue);
                listPref.setSummary(listPref.getEntries()[idx]);
                return true;
            }
        });
        String deafult = ((CharSequence[]) reminder.getEntries())[0].toString();
        reminder.setValue(prefs.getString(KEY_ALERT_REMINDER_INTERVAL + "_" + mSubId, deafult));
        reminder.setSummary(reminder.getEntry());
        reminder.setDependency(KEY_ENABLE_ETWS_ALERT + "_" + mSubId);

        CheckBoxPreference alertVibrate = (CheckBoxPreference) findPreference(KEY_ENABLE_ALERT_VIBRATE);
        alertVibrate.setKey(KEY_ENABLE_ALERT_VIBRATE + "_" + mSubId);
        alertVibrate.setChecked(prefs.getBoolean(KEY_ENABLE_ALERT_VIBRATE + "_" + mSubId, true));
        alertVibrate.setDependency(KEY_ENABLE_ETWS_ALERT + "_" + mSubId);

        CheckBoxPreference enableAlertSpeech = (CheckBoxPreference) findPreference(KEY_ENABLE_ALERT_SPEECH);
        if (enableAlertSpeech != null) {
            String newKey = KEY_ENABLE_ALERT_SPEECH + "_" + mSubId;
            enableAlertSpeech.setKey(newKey);
            enableAlertSpeech.setChecked(prefs.getBoolean(newKey, ENABLE_ALERT_SPEECH_DEFAULT));
            enableAlertSpeech.setOnPreferenceChangeListener(startConfigServiceListener);
            enableAlertSpeech.setDependency(KEY_ENABLE_ETWS_ALERT + "_" + mSubId);
        }

        CheckBoxPreference enableEtwsTestAlerts = (CheckBoxPreference) findPreference(KEY_ENABLE_ETWS_TEST_ALERTS);
        if (enableEtwsTestAlerts != null) {
            String newKey = KEY_ENABLE_ETWS_TEST_ALERTS + "_" + mSubId;
            enableEtwsTestAlerts.setKey(newKey);
            enableEtwsTestAlerts.setChecked(prefs.getBoolean(newKey, ENABLE_ETWS_TEST_ALERTS_DEFAULT));
            enableEtwsTestAlerts.setDependency(KEY_ENABLE_ETWS_ALERT + "_" + mSubId);
        }

        // update title as sim card name
        //SIMInfo simInfo = SIMInfo.getSIMInfoById(this, mSimId);
        //TODO: done
        SubscriptionInfo si = SubscriptionManager.from(this).getActiveSubscriptionInfo(mSubId);
        if (si != null) {
            setTitle(si.getDisplayName().toString());
        }

        Preference alertVolume = (Preference) findPreference(KEY_ALERT_SOUND_VOLUME);
        if (alertVolume != null) {
            alertVolume.setOnPreferenceClickListener(getAlertVolumeListener());
            alertVolume.setDependency(KEY_ENABLE_ETWS_ALERT + "_" + mSubId);
        }
    }

    private OnPreferenceClickListener getAlertVolumeListener() {
        Log.i(TAG, "getAlertVolumeListener ++ ");
        OnPreferenceClickListener l = new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Log.i(TAG, "getAlertVolumeListener onclicked ");
                final AlertDialog.Builder dialog=new AlertDialog.Builder(getApplicationContext());
                LayoutInflater flater = getLayoutInflater();
                View v = flater.inflate(R.layout.alert_dialog_view, null);
                SeekBar sb = (SeekBar) v.findViewById(R.id.seekbar);
                // set bar's progress
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CellBroadcastSettings.this);
                float pro = 1.0f;
                //if (SystemProperties.get("ro.mtk_gemini_support").equals("1")) {
                if (mTelephonyManager.getPhoneCount() > 1) {
                    pro = prefs.getFloat(KEY_ALERT_SOUND_VOLUME + "_" + mSubId, 1.0f);
                } else {
                    pro = prefs.getFloat(KEY_ALERT_SOUND_VOLUME, 1.0f);
                }
                int progress = (int) (pro * 100);
                if (progress < 0) {
                    progress = 0;
                } else if (progress > 100) {
                    progress = 100;
                }
                //Xlog.d(TAG, "open volume setting,progress:"+progress+",pro:"+pro);
                sb.setProgress(progress);
                sb.setOnSeekBarChangeListener(getSeekBarListener());
                dialog.setTitle(R.string.alert_sound_volume)
                .setView(v)
                .setPositiveButton(R.string.button_dismiss, new PositiveButtonListener())
                .setNegativeButton(R.string.button_cancel, new NegativeButtonListener())
                .show();
                return true;
            }
        };
        return l;
    }

    private SeekBar.OnSeekBarChangeListener getSeekBarListener() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mAlertVolume = progress / 100.0f;
                    Log.d(TAG, "volume:" + mAlertVolume);
                }
                            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (mMediaPlayer != null) {
                    mMediaPlayer.stop();
                } else {
                    mMediaPlayer = new MediaPlayer();
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                    try {
                        AssetFileDescriptor afd = getResources().openRawResourceFd(
                                R.raw.attention_signal);
                        if (afd != null) {
                            mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd
                                    .getStartOffset(), afd.getLength());
                            afd.close();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "exception onStartTrackingTouch: " + e);
                    }
                }
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // make some sample sound
                try {
                    mMediaPlayer.setVolume(mAlertVolume, mAlertVolume);
                    mMediaPlayer.prepare();
                    mMediaPlayer.seekTo(0);
                    mMediaPlayer.start();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                                mMediaPlayer.stop();
                                Log.d(TAG, "handler post stop at 100 millis");
                            }
                        }
                    }, 100);
                } catch (Exception e) {
                    Log.e(TAG, "exception onStopTrackingTouch: " + e);
                }
            }
        };
    }

    private class PositiveButtonListener implements OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
            //save alert sound volume
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CellBroadcastSettings.this);
            SharedPreferences.Editor editor = prefs.edit();
            //if (SystemProperties.get("ro.mtk_gemini_support").equals("1")) {
            if (mTelephonyManager.getPhoneCount() > 1) {
                editor.putFloat(KEY_ALERT_SOUND_VOLUME + "_" + mSubId, mAlertVolume);

            } else {
                editor.putFloat(KEY_ALERT_SOUND_VOLUME, mAlertVolume);
            }
            editor.commit();
            if (mMediaPlayer != null) {
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        }
    }

    private class NegativeButtonListener implements OnClickListener {
        public void onClick(DialogInterface dialog, int which) {
            // cancel
            dialog.dismiss();
            if (mMediaPlayer != null) {
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                break;
        }

        return true;
    }
}
