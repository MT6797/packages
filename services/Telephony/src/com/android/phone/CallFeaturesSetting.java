/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.phone;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.ims.ImsConfig;
import com.android.ims.ImsManager;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.phone.PhoneGlobals.SubInfoUpdateListener;
import com.android.phone.common.util.SettingsUtil;
import com.android.phone.settings.AccountSelectionPreference;
import com.android.phone.settings.PhoneAccountSettingsFragment;
import com.android.phone.settings.VoicemailSettingsActivity;
import com.android.phone.settings.fdn.FdnSetting;
import com.android.services.telephony.sip.SipUtil;

import com.mediatek.phone.ext.DefaultCallFeaturesSettingExt;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.phone.ext.ICallFeaturesSettingExt;
import com.mediatek.settings.CallBarring;
import com.mediatek.settings.CallFeaturesSettingExt;
import com.mediatek.settings.TelephonyUtils;
import com.mediatek.settings.cdg.CdgCallSettings;
import com.mediatek.settings.cdg.CdgUtils;
import com.mediatek.settings.cdma.CdmaCallForwardOptions;
import com.mediatek.settings.cdma.CdmaCallWaitOptions;

import java.lang.String;
import java.util.ArrayList;
import java.util.List;

/**
 * Top level "Call settings" UI; see res/xml/call_feature_setting.xml
 *
 * This preference screen is the root of the "Call settings" hierarchy available from the Phone
 * app; the settings here let you control various features related to phone calls (including
 * voicemail settings, the "Respond via SMS" feature, and others.)  It's used only on
 * voice-capable phone devices.
 *
 * Note that this activity is part of the package com.android.phone, even
 * though you reach it from the "Phone" app (i.e. DialtactsActivity) which
 * is from the package com.android.contacts.
 *
 * For the "Mobile network settings" screen under the main Settings app,
 * See {@link MobileNetworkSettings}.
 *
 * @see com.android.phone.MobileNetworkSettings
 */
public class CallFeaturesSetting extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener,
                SubInfoUpdateListener {
    private static final String LOG_TAG = "CallFeaturesSetting";
    private static final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);
    // STOPSHIP if true. Flag to override behavior default behavior to hide VT setting.
    // Remove video preference temporarily for developing not ready
    private static final boolean ENABLE_VT_FLAG = false;

    // String keys for preference lookup
    // TODO: Naming these "BUTTON_*" is confusing since they're not actually buttons(!)
    // TODO: Consider moving these strings to strings.xml, so that they are not duplicated here and
    // in the layout files. These strings need to be treated carefully; if the setting is
    // persistent, they are used as the key to store shared preferences and the name should not be
    // changed unless the settings are also migrated.
    private static final String VOICEMAIL_SETTING_SCREEN_PREF_KEY = "button_voicemail_category_key";
    private static final String BUTTON_FDN_KEY   = "button_fdn_key";
    private static final String BUTTON_RETRY_KEY       = "button_auto_retry_key";
    private static final String BUTTON_GSM_UMTS_OPTIONS = "button_gsm_more_expand_key";
    private static final String BUTTON_CDMA_OPTIONS = "button_cdma_more_expand_key";

    /// M: add for call private voice feature @{
    private static final String BUTTON_CP_KEY          = "button_voice_privacy_key";
    /// @}

    private static final String CALL_FORWARDING_KEY = "call_forwarding_key";
    private static final String ADDITIONAL_GSM_SETTINGS_KEY = "additional_gsm_call_settings_key";
    /// M: GSM type phone call settings item --> call barring
    private static final String BUTTON_CB_EXPAND = "button_cb_expand_key";

    /// M: CDMA type phone call settings item --> call forward & call wait
    private static final String KEY_CALL_FORWARD = "button_cf_expand_key";
    private static final String KEY_CALL_WAIT = "button_cw_key";

    private static final String PHONE_ACCOUNT_SETTINGS_KEY =
            "phone_account_settings_preference_screen";

    private static final String ENABLE_VIDEO_CALLING_KEY = "button_enable_video_calling";

    private Phone mPhone;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;
    private TelecomManager mTelecomManager;

    private CheckBoxPreference mButtonAutoRetry;
    private PreferenceScreen mVoicemailSettingsScreen;
    private CheckBoxPreference mEnableVideoCalling;

    /*
     * Click Listeners, handle click based on objects attached to UI.
     */

    // Click listener for all toggle events
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        /// M: Add for our inner features @{
        if (onPreferenceTreeClickMTK(preferenceScreen, preference)) {
            return true;
        }
        /// @}
        if (preference == mButtonAutoRetry) {
            android.provider.Settings.Global.putInt(getApplicationContext().getContentResolver(),
                    android.provider.Settings.Global.CALL_AUTO_RETRY,
                    mButtonAutoRetry.isChecked() ? 1 : 0);
            return true;
        }
        return false;
    }

    /**
     * Implemented to support onPreferenceChangeListener to look for preference
     * changes.
     *
     * @param preference is the preference to be changed
     * @param objValue should be the value of the selection, NOT its localized
     * display value.
     */
    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (DBG) log("onPreferenceChange: \"" + preference + "\" changed to \"" + objValue + "\"");

        if (mCallFeaturesSettingExt != null
                && mCallFeaturesSettingExt.onPreferenceChange(preference)) {
            return true;
        } else if (preference == mEnableVideoCalling) {
            if (ImsManager.isEnhanced4gLteModeSettingEnabledByUser(mPhone.getContext())) {
                PhoneGlobals.getInstance().phoneMgr.enableVideoCalling((boolean) objValue);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                DialogInterface.OnClickListener networkSettingsClickListener =
                        new Dialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(mPhone.getContext(),
                                        com.android.phone.MobileNetworkSettings.class));
                            }
                        };
                builder.setMessage(getResources().getString(
                                R.string.enable_video_calling_dialog_msg))
                        .setNeutralButton(getResources().getString(
                                R.string.enable_video_calling_dialog_settings),
                                networkSettingsClickListener)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return false;
            }
        }

        // Always let the preference setting proceed.
        return true;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (DBG) log("onCreate: Intent is " + getIntent());

        // Make sure we are running as the primary user.
        if (UserHandle.myUserId() != UserHandle.USER_OWNER) {
            Toast.makeText(this, R.string.call_settings_primary_user_only,
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        mSubscriptionInfoHelper.setActionBarTitle(
                getActionBar(), getResources(), R.string.call_settings_with_label);
        mPhone = mSubscriptionInfoHelper.getPhone();
        mTelecomManager = TelecomManager.from(this);

        /// M: Add for MTK hotswap
        if (mPhone == null) {
            log("onCreate: mPhone is null, finish!!!");
            finish();
            return;
        }
        /// M: Add for MTK features
        onCreateMTK();
    }

    @Override
    protected void onResume() {
        super.onResume();

        /** M: For ALPS01928994 Remove this for screen update @{
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            preferenceScreen.removeAll();
        }

        addPreferencesFromResource(R.xml.call_feature_setting);

        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        Preference phoneAccountSettingsPreference = findPreference(PHONE_ACCOUNT_SETTINGS_KEY);
        if (telephonyManager.isMultiSimEnabled() || !SipUtil.isVoipSupported(mPhone.getContext())) {
            getPreferenceScreen().removePreference(phoneAccountSettingsPreference);
        }

        PreferenceScreen prefSet = getPreferenceScreen();
        mVoicemailSettingsScreen =
                (PreferenceScreen) findPreference(VOICEMAIL_SETTING_SCREEN_PREF_KEY);
        mVoicemailSettingsScreen.setIntent(mSubscriptionInfoHelper.getIntent(
                VoicemailSettingsActivity.class));

        mButtonAutoRetry = (CheckBoxPreference) findPreference(BUTTON_RETRY_KEY);

        mEnableVideoCalling = (CheckBoxPreference) findPreference(ENABLE_VIDEO_CALLING_KEY);

        PersistableBundle carrierConfig =
                PhoneGlobals.getInstance().getCarrierConfigForSubId(mPhone.getSubId());

        if (carrierConfig.getBoolean(CarrierConfigManager.KEY_AUTO_RETRY_ENABLED_BOOL)) {
            mButtonAutoRetry.setOnPreferenceChangeListener(this);
            int autoretry = Settings.Global.getInt(
                    getContentResolver(), Settings.Global.CALL_AUTO_RETRY, 0);
            mButtonAutoRetry.setChecked(autoretry != 0);
        } else {
            prefSet.removePreference(mButtonAutoRetry);
            mButtonAutoRetry = null;
        }

        Preference cdmaOptions = prefSet.findPreference(BUTTON_CDMA_OPTIONS);
        Preference gsmOptions = prefSet.findPreference(BUTTON_GSM_UMTS_OPTIONS);
        if (carrierConfig.getBoolean(CarrierConfigManager.KEY_WORLD_PHONE_BOOL)) {
            cdmaOptions.setIntent(mSubscriptionInfoHelper.getIntent(CdmaCallOptions.class));
            gsmOptions.setIntent(mSubscriptionInfoHelper.getIntent(GsmUmtsCallOptions.class));
        } else {
            prefSet.removePreference(cdmaOptions);
            prefSet.removePreference(gsmOptions);

            int phoneType = mPhone.getPhoneType();
            Preference fdnButton = prefSet.findPreference(BUTTON_FDN_KEY);
            if (carrierConfig.getBoolean(
                    CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL)) {
                prefSet.removePreference(fdnButton);
            } else {
                if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                    prefSet.removePreference(fdnButton);

                    if (!carrierConfig.getBoolean(
                            CarrierConfigManager.KEY_VOICE_PRIVACY_DISABLE_UI_BOOL)) {
                        addPreferencesFromResource(R.xml.cdma_call_privacy);
                    }
                } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                    fdnButton.setIntent(mSubscriptionInfoHelper.getIntent(FdnSetting.class));

                    if (carrierConfig.getBoolean(
                            CarrierConfigManager.KEY_ADDITIONAL_CALL_SETTING_BOOL)) {
                        addPreferencesFromResource(R.xml.gsm_umts_call_options);
                        GsmUmtsCallOptions.init(prefSet, mSubscriptionInfoHelper);
                    }
                } else {
                    throw new IllegalStateException("Unexpected phone type: " + phoneType);
                }
            }
        }

        if (ImsManager.isVtEnabledByPlatform(mPhone.getContext()) && ENABLE_VT_FLAG) {
            boolean currentValue =
                    ImsManager.isEnhanced4gLteModeSettingEnabledByUser(mPhone.getContext())
                    ? PhoneGlobals.getInstance().phoneMgr.isVideoCallingEnabled(
                            getOpPackageName()) : false;
            mEnableVideoCalling.setChecked(currentValue);
            mEnableVideoCalling.setOnPreferenceChangeListener(this);
        } else {
            prefSet.removePreference(mEnableVideoCalling);
        }

        if (ImsManager.isVolteEnabledByPlatform(this) &&
                !carrierConfig.getBoolean(
                        CarrierConfigManager.KEY_CARRIER_VOLTE_TTY_SUPPORTED_BOOL)) {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        }

        Preference wifiCallingSettings = findPreference(
                getResources().getString(R.string.wifi_calling_settings_key));

        final PhoneAccountHandle simCallManager = mTelecomManager.getSimCallManager();
        if (simCallManager != null) {
            Intent intent = PhoneAccountSettingsFragment.buildPhoneAccountConfigureIntent(
                    this, simCallManager);
            if (intent != null) {
                wifiCallingSettings.setTitle(R.string.wifi_calling);
                wifiCallingSettings.setSummary(null);
                wifiCallingSettings.setIntent(intent);
            } else {
                prefSet.removePreference(wifiCallingSettings);
            }
        } else if (!ImsManager.isWfcEnabledByPlatform(mPhone.getContext())) {
            prefSet.removePreference(wifiCallingSettings);
        } else {
            int resId = com.android.internal.R.string.wifi_calling_off_summary;
            if (ImsManager.isWfcEnabledByUser(mPhone.getContext())) {
                int wfcMode = ImsManager.getWfcMode(mPhone.getContext());
                switch (wfcMode) {
                    case ImsConfig.WfcModeFeatureValueConstants.WIFI_ONLY:
                        resId = com.android.internal.R.string.wfc_mode_wifi_only_summary;
                        break;
                    case ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED:
                        resId = com.android.internal.R.string.wfc_mode_cellular_preferred_summary;
                        break;
                    case ImsConfig.WfcModeFeatureValueConstants.WIFI_PREFERRED:
                        resId = com.android.internal.R.string.wfc_mode_wifi_preferred_summary;
                        break;
                    default:
                        if (DBG) log("Unexpected WFC mode value: " + wfcMode);
                }
            }
            wifiCallingSettings.setSummary(resId);
        }
        /// }*/

        /// M: Add for MTK features
        mCallFeaturesSettingExt.updatePrefScreen(getPreferenceScreen());
        mCallFeaturesSettingExt.init();
        mCallFeaturesSettingExt.updateScreenStatus();
        /// @}

        updateWFC();

        /// M: WFC @{
        ExtensionManager.getCallFeaturesSettingExt().initOtherCallFeaturesSetting(this);
        ExtensionManager.getCallFeaturesSettingExt()
                .onCallFeatureSettingsEvent(DefaultCallFeaturesSettingExt.RESUME);
        /// @}
    }

    @Override
    protected void onNewIntent(Intent newIntent) {
        setIntent(newIntent);

        mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        mSubscriptionInfoHelper.setActionBarTitle(
                getActionBar(), getResources(), R.string.call_settings_with_label);
        mPhone = mSubscriptionInfoHelper.getPhone();
        mCallFeaturesSettingExt.updatePhone(mPhone);
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {  // See ActionBar#setDisplayHomeAsUpEnabled()
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Finish current Activity and go up to the top level Settings ({@link CallFeaturesSetting}).
     * This is useful for implementing "HomeAsUp" capability for second-level Settings.
     */
    public static void goUpToTopLevelSetting(
            Activity activity, SubscriptionInfoHelper subscriptionInfoHelper) {
        Intent intent = subscriptionInfoHelper.getIntent(CallFeaturesSetting.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
        activity.finish();
    }

    // -------------------- Mediatek ---------------------
    /// M: Add for plug-in @{
    private ICallFeaturesSettingExt mExt;
    /// Host app's Ext
    private CallFeaturesSettingExt mCallFeaturesSettingExt;
    /// Add for CDG OMH
    private CdgCallSettings mCdgCallSettings = null;

    private void onCreateMTK() {

        mCallFeaturesSettingExt = new CallFeaturesSettingExt(
                this, mSubscriptionInfoHelper);
        mCallFeaturesSettingExt.registerCallback();
        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);

        log("onCreateMTK");
        /// For ALPS01928994 Move Google's onResume actions to onCreate @{
        initUi();
        /// @}
    }

    private void initUi() {
        addPreferencesFromResource(R.xml.call_feature_setting);

        PreferenceScreen prefSet = getPreferenceScreen();
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        Preference phoneAccountSettingsPreference = findPreference(PHONE_ACCOUNT_SETTINGS_KEY);
        if (telephonyManager.isMultiSimEnabled() || !SipUtil.isVoipSupported(mPhone.getContext())) {
            prefSet.removePreference(phoneAccountSettingsPreference);
        }

        mVoicemailSettingsScreen =
                (PreferenceScreen) findPreference(VOICEMAIL_SETTING_SCREEN_PREF_KEY);
        mVoicemailSettingsScreen.setIntent(mSubscriptionInfoHelper.getIntent(
                VoicemailSettingsActivity.class));

        mButtonAutoRetry = (CheckBoxPreference) findPreference(BUTTON_RETRY_KEY);

        mEnableVideoCalling = (CheckBoxPreference) findPreference(ENABLE_VIDEO_CALLING_KEY);

        PersistableBundle carrierConfig =
                PhoneGlobals.getInstance().getCarrierConfigForSubId(mPhone.getSubId());

        if (carrierConfig.getBoolean(CarrierConfigManager.KEY_AUTO_RETRY_ENABLED_BOOL)) {
            mButtonAutoRetry.setOnPreferenceChangeListener(this);
            int autoretry = Settings.Global.getInt(
                    getContentResolver(), Settings.Global.CALL_AUTO_RETRY, 0);
            mButtonAutoRetry.setChecked(autoretry != 0);
        } else {
            prefSet.removePreference(mButtonAutoRetry);
            mButtonAutoRetry = null;
        }

        Preference cdmaOptions = prefSet.findPreference(BUTTON_CDMA_OPTIONS);
        Preference gsmOptions = prefSet.findPreference(BUTTON_GSM_UMTS_OPTIONS);
        if (carrierConfig.getBoolean(CarrierConfigManager.KEY_WORLD_PHONE_BOOL)) {
            cdmaOptions.setIntent(mSubscriptionInfoHelper.getIntent(CdmaCallOptions.class));
            gsmOptions.setIntent(mSubscriptionInfoHelper.getIntent(GsmUmtsCallOptions.class));
        } else {
            prefSet.removePreference(cdmaOptions);
            prefSet.removePreference(gsmOptions);

            int phoneType = mPhone.getPhoneType();
            Preference fdnButton = prefSet.findPreference(BUTTON_FDN_KEY);
            if (carrierConfig.getBoolean(
                    CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL)) {
                prefSet.removePreference(fdnButton);
            } else {
                if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                    /// Add for CDG OMH, show fdn when CDG OMH SIM card. @{
                    if(CdgUtils.isCdgOmhSimCard(mPhone.getSubId())) {
                        fdnButton.setIntent(mSubscriptionInfoHelper.getIntent(FdnSetting.class));
                    } else {
                    /// @}
                        prefSet.removePreference(fdnButton);
                    }

                    if (!carrierConfig.getBoolean(
                            CarrierConfigManager.KEY_VOICE_PRIVACY_DISABLE_UI_BOOL)) {
                        addPreferencesFromResource(R.xml.cdma_call_privacy);
                        /// M: for ALPS02087723, get the right cdma phone instance @{
                        CdmaVoicePrivacyCheckBoxPreference ccp =
                                (CdmaVoicePrivacyCheckBoxPreference)findPreference(BUTTON_CP_KEY);
                        if (ccp != null) {
                            ccp.setPhone(mPhone);
                        }
                        /// @}
                    }

                  /// M: For C2K project to group GSM and C2K Call Settings @{
                    log("CallFeartueSetting call isCdmaSupport");
                    log("isCdmaSupport = " + TelephonyUtils.isCdmaSupport());
                    if (TelephonyUtils.isCdmaSupport()) {
                        log("CallFeartueSetting call showCallOption");
                        //mCallFeaturesSettingExt.showCallOption(prefSet);
                        addPreferencesFromResource(R.xml.mtk_cdma_call_options);
                    }
                    /// @}
                } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                    fdnButton.setIntent(mSubscriptionInfoHelper.getIntent(FdnSetting.class));

                    if (carrierConfig.getBoolean(
                            CarrierConfigManager.KEY_ADDITIONAL_CALL_SETTING_BOOL)) {
                        addPreferencesFromResource(R.xml.gsm_umts_call_options);
                        GsmUmtsCallOptions.init(prefSet, mSubscriptionInfoHelper);
                    }
                } else {
                    throw new IllegalStateException("Unexpected phone type: " + phoneType);
                }
            }
        }

        if (ImsManager.isVtEnabledByPlatform(mPhone.getContext()) && ENABLE_VT_FLAG) {
            boolean currentValue =
                    ImsManager.isEnhanced4gLteModeSettingEnabledByUser(mPhone.getContext())
                    ? PhoneGlobals.getInstance().phoneMgr.isVideoCallingEnabled(
                            getOpPackageName()) : false;
            mEnableVideoCalling.setChecked(currentValue);
            mEnableVideoCalling.setOnPreferenceChangeListener(this);
        } else {
            prefSet.removePreference(mEnableVideoCalling);
        }

        if (ImsManager.isVolteEnabledByPlatform(this) &&
                !mPhone.getContext().getResources().getBoolean(
                        com.android.internal.R.bool.config_carrier_volte_tty_supported)) {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        }

        updateWFC();

        ///M: Add for CDG OMH @{
        if (CdgUtils.isCdgOmhSimCard(mSubscriptionInfoHelper.getSubId())) {
            log("new CdgCallSettings.");
            mCdgCallSettings = new CdgCallSettings(this, mSubscriptionInfoHelper);
            if (prefSet.findPreference(KEY_CALL_FORWARD) != null) {
                prefSet.removePreference(prefSet.findPreference(KEY_CALL_FORWARD));
            }
            if (prefSet.findPreference(KEY_CALL_WAIT) != null) {
                prefSet.removePreference(prefSet.findPreference(KEY_CALL_WAIT));
            }
        }
        /// @}
    }

    @Override
    protected void onDestroy() {
        /// For ALPS01973041, when the app call finish before it run to create's
        /// new CallFeaturesSettingExt. Only when the mCallFeaturesSettingExt is not
        /// null, can we unRegisterCallBack & removeSubInfoUpdateListener. @{
        if (mCallFeaturesSettingExt != null) {
            mCallFeaturesSettingExt.unRegisterCallback();
            PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);
        }
        /// @}
        /// M: WFC @{
        ExtensionManager.getCallFeaturesSettingExt()
                .onCallFeatureSettingsEvent(DefaultCallFeaturesSettingExt.DESTROY);
        /// @}
        super.onDestroy();
    }

    @Override
    public void handleSubInfoUpdate() {
        finish();
    }

    /**
     * For internal features
     * @param preferenceScreen
     * @param preference
     * @return
     */
    private boolean onPreferenceTreeClickMTK(
            PreferenceScreen preferenceScreen, Preference preference) {

        log("onPreferenceTreeClickMTK" + preference.getKey());
        /// Add for [VoLTE_SS] @{
        if (preference == preferenceScreen.findPreference(CALL_FORWARDING_KEY) ||
            preference == preferenceScreen.findPreference(ADDITIONAL_GSM_SETTINGS_KEY) ||
            preference == preferenceScreen.findPreference(BUTTON_CB_EXPAND)) {

            if (TelephonyUtils.shouldShowOpenMobileDataDialog(
                    this, mSubscriptionInfoHelper.getPhone().getSubId())) {
                TelephonyUtils.showOpenMobileDataDialog(
                        this, mSubscriptionInfoHelper.getPhone().getSubId());
            } else {
                Intent intent;
                if (preference == preferenceScreen.findPreference(CALL_FORWARDING_KEY)) {
                    intent = mSubscriptionInfoHelper.getIntent(GsmUmtsCallForwardOptions.class);
                } else if (preference == preferenceScreen.findPreference(BUTTON_CB_EXPAND)) {
                    intent = mSubscriptionInfoHelper.getIntent(CallBarring.class);
                } else {
                    intent = mSubscriptionInfoHelper.getIntent(GsmUmtsAdditionalCallOptions.class);
                }
                startActivity(intent);
            }
            return true;
        }
        /// @}
        /// M: CDMA type phone call setting item click handling
        if (preference == preferenceScreen.findPreference(KEY_CALL_FORWARD) ||
            preference == preferenceScreen.findPreference(KEY_CALL_WAIT)) {
            if (preference == preferenceScreen.findPreference(KEY_CALL_FORWARD)) {
                Intent intent;
                intent = mSubscriptionInfoHelper.getIntent(CdmaCallForwardOptions.class);
                startActivity(intent);
            } else { //(preference == preferenceScreen.findPreference(KEY_CALL_WAIT))
                /// M: remove CNIR and move CW option to cdma call option.
                ///TODO: Check whether need mForeground
                showDialog(CdmaCallWaitOptions.CW_MODIFY_DIALOG);
            }
            return true;
        }
        /// Add for CDG OMH @{
        if (mCdgCallSettings != null && mCdgCallSettings.onPreferenceTreeClick(
                preferenceScreen, preference)) {
            log("onPreferenceTreeClickMTK, handled by CDG call settings.");
            return true;
        }
        /// @}
        return false;
    }

    // dialog creation method, called by showDialog()
    @Override
    protected Dialog onCreateDialog(int dialogId) {
        /// M: remove CNIR and move CW option to cdma call option.
        if (dialogId == CdmaCallWaitOptions.CW_MODIFY_DIALOG) {
            return new CdmaCallWaitOptions(this, mPhone).createDialog();
        }

        /// Add for CDG OMH @{
        if (mCdgCallSettings != null) {
            return mCdgCallSettings.onCreateDialog(dialogId);
        }
        /// @}
        return null;
    }

    private void updateWFC() {
        PreferenceScreen prefSet = getPreferenceScreen();
        Preference wifiCallingSettings = findPreference(
                getResources().getString(R.string.wifi_calling_settings_key));
        if (wifiCallingSettings == null) {
            log("wfc already removed");

            return;
        }

        final PhoneAccountHandle simCallManager = mTelecomManager.getSimCallManager();
        if (simCallManager != null) {
            Intent intent = PhoneAccountSettingsFragment.buildPhoneAccountConfigureIntent(
                    this, simCallManager);
            if (intent != null) {
                wifiCallingSettings.setTitle(R.string.wifi_calling);
                wifiCallingSettings.setSummary(null);
                wifiCallingSettings.setIntent(intent);
            } else {
                prefSet.removePreference(wifiCallingSettings);
            }
        } else if (!ImsManager.isWfcEnabledByPlatform(mPhone.getContext())) {
            prefSet.removePreference(wifiCallingSettings);
        } else {
            int resId = com.android.internal.R.string.wifi_calling_off_summary;
            if (ImsManager.isWfcEnabledByUser(mPhone.getContext())) {
                int wfcMode = ImsManager.getWfcMode(mPhone.getContext());
                switch (wfcMode) {
                    case ImsConfig.WfcModeFeatureValueConstants.WIFI_ONLY:
                        resId = com.android.internal.R.string.wfc_mode_wifi_only_summary;
                        break;
                    case ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED:
                        resId = com.android.internal.R.string.wfc_mode_cellular_preferred_summary;
                        break;
                    case ImsConfig.WfcModeFeatureValueConstants.WIFI_PREFERRED:
                        resId = com.android.internal.R.string.wfc_mode_wifi_preferred_summary;
                        break;
                    default:
                        if (DBG) log("Unexpected WFC mode value: " + wfcMode);
                }
            }
            wifiCallingSettings.setSummary(resId);
            /// M: WFC: customize Wifi calling settings @{
            ExtensionManager.getCallFeaturesSettingExt().initPlugin(this, wifiCallingSettings);
            wifiCallingSettings.setSummary(ExtensionManager.getCallFeaturesSettingExt()
                    .getWfcSummary(mPhone.getContext(), resId));
            ExtensionManager.getCallFeaturesSettingExt().initOtherCallFeaturesSetting(this);
            /// @}
        }
    }
}
