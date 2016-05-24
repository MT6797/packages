package com.android.phone.settings;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.net.sip.SipManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyIntents;
import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;
import com.android.services.telephony.sip.SipAccountRegistry;
import com.android.services.telephony.sip.SipSharedPreferences;
import com.android.services.telephony.sip.SipUtil;
import com.mediatek.phone.PhoneFeatureConstants.FeatureOption;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.settings.TelephonyUtils;
import com.mediatek.settings.cdma.TelephonyUtilsEx;
import com.mediatek.telecom.TelecomManagerEx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class PhoneAccountSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener,
                AccountSelectionPreference.AccountSelectionListener {

    private static final String ACCOUNTS_LIST_CATEGORY_KEY =
            "phone_accounts_accounts_list_category_key";

    private static final String DEFAULT_OUTGOING_ACCOUNT_KEY = "default_outgoing_account";
    private static final String ALL_CALLING_ACCOUNTS_KEY = "phone_account_all_calling_accounts";

    private static final String SIP_SETTINGS_CATEGORY_PREF_KEY =
            "phone_accounts_sip_settings_category_key";
    private static final String USE_SIP_PREF_KEY = "use_sip_calling_options_key";
    private static final String SIP_RECEIVE_CALLS_PREF_KEY = "sip_receive_calls_key";

    private static final String LEGACY_ACTION_CONFIGURE_PHONE_ACCOUNT =
            "android.telecom.action.CONNECTION_SERVICE_CONFIGURE";

    /**
     * Value to start ordering of phone accounts relative to other preferences. By setting this
     * value on the phone account listings, we ensure that anything that is ordered before
     * {value} in the preference XML comes before the phone account list and anything with
     * a value significantly larger will list after.
     */
    private static final int ACCOUNT_ORDERING_START_VALUE = 100;

    private static final String LOG_TAG = PhoneAccountSettingsFragment.class.getSimpleName();

    private TelecomManager mTelecomManager;
    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;

    private PreferenceCategory mAccountList;

    private AccountSelectionPreference mDefaultOutgoingAccount;

    private ListPreference mUseSipCalling;
    private CheckBoxPreference mSipReceiveCallsPreference;
    private SipSharedPreferences mSipSharedPreferences;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mTelecomManager = TelecomManager.from(getActivity());
        mTelephonyManager = TelephonyManager.from(getActivity());
        mSubscriptionManager = SubscriptionManager.from(getActivity());
        ///M: Add for C2K solution 2 improvement & 1.5 @{
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_DONE);
        intentFilter.addAction(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_FAILED);
        intentFilter.addAction(TelecomManagerEx.ACTION_PHONE_ACCOUNT_CHANGED);
        getActivity().registerReceiver(mReceiver, intentFilter);
        /// @}
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getPreferenceScreen() != null) {
            getPreferenceScreen().removeAll();
        }

        addPreferencesFromResource(R.xml.phone_account_settings);

        /**
         * Here we make decisions about what we will and will not display with regards to phone-
         * account settings.  The basic settings structure is this:
         * (1) <Make Calls With...>  // Lets user pick a default account for outgoing calls
         * (2) <Account List>
         *       <Account>
         *       ...
         *       <Account>
         *     </Account List>
         * (3) <All Accounts>  // Lets user enable/disable third-party accounts. SIM-based accounts
         *                     // are always enabled and so aren't relevant here.
         *
         * Here are the rules that we follow:
         * - (1) is only shown if there are multiple enabled accounts, including SIM accounts.
         *   This can be 2+ SIM accounts, 2+ third party accounts or any combination.
         * - (2) The account list only lists (a) enabled third party accounts and (b) SIM-based
         *   accounts. However, for single-SIM devices, if the only account to show is the
         *   SIM-based account, we don't show the list at all under the assumption that the user
         *   already knows about the account.
         * - (3) Is only shown if there exist any third party accounts.  If none exist, then the
         *   option is hidden since there is nothing that can be done in it.
         *
         * By far, the most common case for users will be the single-SIM device without any
         * third party accounts. IOW, the great majority of users won't see any of these options.
         */
        mAccountList = (PreferenceCategory) getPreferenceScreen().findPreference(
                ACCOUNTS_LIST_CATEGORY_KEY);
        List<PhoneAccountHandle> allNonSimAccounts =
                getCallingAccounts(false /* includeSims */, true /* includeDisabled */);
        // Check to see if we should show the entire section at all.
        if (shouldShowConnectionServiceList(allNonSimAccounts)) {
            List<PhoneAccountHandle> enabledAccounts =
                    getCallingAccounts(true /* includeSims */, false /* includeDisabled */);
            // Initialize the account list with the set of enabled & SIM accounts.
            initAccountList(enabledAccounts);

            mDefaultOutgoingAccount = (AccountSelectionPreference)
                    getPreferenceScreen().findPreference(DEFAULT_OUTGOING_ACCOUNT_KEY);
            mDefaultOutgoingAccount.setListener(this);

            // Only show the 'Make Calls With..." option if there are multiple accounts.
            if (enabledAccounts.size() > 1) {
                updateDefaultOutgoingAccountsModel();
            } else {
                mAccountList.removePreference(mDefaultOutgoingAccount);
            }

            Preference allAccounts = getPreferenceScreen().findPreference(ALL_CALLING_ACCOUNTS_KEY);
            // If there are no third party (nonSim) accounts, then don't show enable/disable dialog.
            if (allNonSimAccounts.isEmpty() && allAccounts != null) {
                mAccountList.removePreference(allAccounts);
            }
        } else {
            getPreferenceScreen().removePreference(mAccountList);
        }

        if (SipUtil.isVoipSupported(getActivity())) {
            mSipSharedPreferences = new SipSharedPreferences(getActivity());

            mUseSipCalling = (ListPreference)
                    getPreferenceScreen().findPreference(USE_SIP_PREF_KEY);
            mUseSipCalling.setEntries(!SipManager.isSipWifiOnly(getActivity())
                    ? R.array.sip_call_options_wifi_only_entries
                    : R.array.sip_call_options_entries);
            mUseSipCalling.setOnPreferenceChangeListener(this);

            int optionsValueIndex =
                    mUseSipCalling.findIndexOfValue(mSipSharedPreferences.getSipCallOption());
            if (optionsValueIndex == -1) {
                // If the option is invalid (eg. deprecated value), default to SIP_ADDRESS_ONLY.
                mSipSharedPreferences.setSipCallOption(
                        getResources().getString(R.string.sip_address_only));
                optionsValueIndex =
                        mUseSipCalling.findIndexOfValue(mSipSharedPreferences.getSipCallOption());
            }
            mUseSipCalling.setValueIndex(optionsValueIndex);
            mUseSipCalling.setSummary(mUseSipCalling.getEntry());

            mSipReceiveCallsPreference = (CheckBoxPreference)
                    getPreferenceScreen().findPreference(SIP_RECEIVE_CALLS_PREF_KEY);
            mSipReceiveCallsPreference.setEnabled(SipUtil.isPhoneIdle(getActivity()));
            mSipReceiveCallsPreference.setChecked(
                    mSipSharedPreferences.isReceivingCallsEnabled());
            mSipReceiveCallsPreference.setOnPreferenceChangeListener(this);
        } else {
            getPreferenceScreen().removePreference(
                    getPreferenceScreen().findPreference(SIP_SETTINGS_CATEGORY_PREF_KEY));
        }

        /// M: Add for plug-in @{
        ExtensionManager.getCallFeaturesSettingExt().initOtherCallFeaturesSetting(this);
        /// @}

        ///M: add mtk features @{
        configCallFeatures();
        /// @}

        ///M: make sure update default phoneAccount in time @{
        updateDefaultOutgoingAccountPreference();
        /// @}
    }

    /**
     * Handles changes to the preferences.
     *
     * @param pref The preference changed.
     * @param objValue The changed value.
     * @return True if the preference change has been handled, and false otherwise.
     */
    @Override
    public boolean onPreferenceChange(Preference pref, Object objValue) {
        /// M: Add MTK features @{
        if (onPreferenceChangeEx(pref)) {
            return true;
        } else
        /// @}
            if (pref == mUseSipCalling) {
            String option = objValue.toString();
            mSipSharedPreferences.setSipCallOption(option);
            mUseSipCalling.setValueIndex(mUseSipCalling.findIndexOfValue(option));
            mUseSipCalling.setSummary(mUseSipCalling.getEntry());
            return true;
        } else if (pref == mSipReceiveCallsPreference) {
            final boolean isEnabled = !mSipReceiveCallsPreference.isChecked();
            new Thread(new Runnable() {
                public void run() {
                    handleSipReceiveCallsOption(isEnabled);
                }
            }).start();
            return true;
        }
        return false;
    }

    /**
     * Handles a phone account selection for the default outgoing phone account.
     *
     * @param pref The account selection preference which triggered the account selected event.
     * @param account The account selected.
     * @return True if the account selection has been handled, and false otherwise.
     */
    @Override
    public boolean onAccountSelected(AccountSelectionPreference pref, PhoneAccountHandle account) {
        if (pref == mDefaultOutgoingAccount) {
            mTelecomManager.setUserSelectedOutgoingPhoneAccount(account);
            Log.d(LOG_TAG, "onAccountSelected updateDefaultOutgoingAccountPreference");
            updateDefaultOutgoingAccountPreference();
            return true;
        }
        return false;
    }

    /**
     * Repopulate the dialog to pick up changes before showing.
     *
     * @param pref The account selection preference dialog being shown.
     */
    @Override
    public void onAccountSelectionDialogShow(AccountSelectionPreference pref) {
        if (pref == mDefaultOutgoingAccount) {
            updateDefaultOutgoingAccountsModel();
        }
    }

    @Override
    public void onAccountChanged(AccountSelectionPreference pref) {
        /// M: Add for C2K solution 2 & 1.5 @{
        if (pref == mDefaultOutgoingAccount) {
            Log.d(LOG_TAG, "[onAccountChanged] updateDefaultOutgoingAccountPreference");
            updateDefaultOutgoingAccountPreference();
        }
        /// @}
    }

    private synchronized void handleSipReceiveCallsOption(boolean isEnabled) {
        Context context = getActivity();
        if (context == null) {
            // Return if the fragment is detached from parent activity before executed by thread.
            return;
        }

        mSipSharedPreferences.setReceivingCallsEnabled(isEnabled);

        SipUtil.useSipToReceiveIncomingCalls(context, isEnabled);

        // Restart all Sip services to ensure we reflect whether we are receiving calls.
        SipAccountRegistry sipAccountRegistry = SipAccountRegistry.getInstance();
        sipAccountRegistry.restartSipService(context);
    }

    /**
     * Queries the telcomm manager to update the default outgoing account selection preference
     * with the list of outgoing accounts and the current default outgoing account.
     */
    private void updateDefaultOutgoingAccountsModel() {
        mDefaultOutgoingAccount.setModel(
                mTelecomManager,
                getCallingAccounts(true /* includeSims */, false /* includeDisabled */),
                mTelecomManager.getUserSelectedOutgoingPhoneAccount(),
                getString(R.string.phone_accounts_ask_every_time));
    }

    private void initAccountList(List<PhoneAccountHandle> enabledAccounts) {

        ///M: Add for C2K solution2 @{
        removeOldAccounts(mAccountList);
        /// @}

        boolean isMultiSimDevice = mTelephonyManager.isMultiSimEnabled();

        // On a single-SIM device, do not list any accounts if the only account is the SIM-based
        // one. This is because on single-SIM devices, we do not expose SIM settings through the
        // account listing entry so showing it does nothing to help the user. Nor does the lack of
        // action match the "Settings" header above the listing.
        if (!isMultiSimDevice && getCallingAccounts(
                false /* includeSims */, false /* includeDisabled */).isEmpty()){
            return;
        }

        // Obtain the list of phone accounts.
        List<PhoneAccount> accounts = new ArrayList<>();
        for (PhoneAccountHandle handle : enabledAccounts) {
            PhoneAccount account = mTelecomManager.getPhoneAccount(handle);
            if (account != null) {
                accounts.add(account);
            }
        }

        // Sort the accounts according to how we want to display them.
        Collections.sort(accounts, new Comparator<PhoneAccount>() {
            @Override
            public int compare(PhoneAccount account1, PhoneAccount account2) {
                int retval = 0;

                // SIM accounts go first
                boolean isSim1 = account1.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION);
                boolean isSim2 = account2.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION);
                if (isSim1 != isSim2) {
                    retval = isSim1 ? -1 : 1;
                }

                // Then order by package
                if (retval == 0) {
                    String pkg1 = account1.getAccountHandle().getComponentName().getPackageName();
                    String pkg2 = account2.getAccountHandle().getComponentName().getPackageName();
                    retval = pkg1.compareTo(pkg2);
                }

                // Finally, order by label
                if (retval == 0) {
                    String label1 = nullToEmpty(account1.getLabel().toString());
                    String label2 = nullToEmpty(account2.getLabel().toString());
                    retval = label1.compareTo(label2);
                }

                // Then by hashcode
                if (retval == 0) {
                    retval = account1.hashCode() - account2.hashCode();
                }
                return retval;
            }
        });

        int order = ACCOUNT_ORDERING_START_VALUE;

        ///M: Add for C2K solution2
        ///   When there are two CDMA cards, and is in home network.
        ///   There must be one SIM card(not the default data one) can't use.
        boolean shouldCheckDisable = TelephonyUtilsEx.isMultiCdmaCardInsertedInHomeNetwork();
        Log.d(LOG_TAG, "[initAccountList] shouldCheckDisable = " + shouldCheckDisable);

        /// M: Add for C2K solution 1.5 @{
        boolean shouldCheckCCardDisable = false;
        if (!FeatureOption.isMtkSvlteSolutionSupport()
                && TelephonyUtilsEx.isCGCardInserted()
                && TelephonyUtilsEx.isCapabilityOnGCard()) {

            shouldCheckCCardDisable = true;
        }

        Log.d(LOG_TAG, "shouldCheckCCardDisable: " + shouldCheckCCardDisable);
        /// @}

        // Add an entry for each account.
        for (PhoneAccount account : accounts) {
            PhoneAccountHandle handle = account.getAccountHandle();
            Intent intent = null;

            ///M: Add for C2K solution2
            boolean shouldEnable = true;

            // SIM phone accounts use a different setting intent and are thus handled differently.
            if (account.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)) {

                // For SIM-based accounts, we only expose the settings through the account list
                // if we are on a multi-SIM device. For single-SIM devices, the settings are
                // more spread out so there is no good single place to take the user, so we don't.
                if (isMultiSimDevice) {
                    SubscriptionInfo subInfo = mSubscriptionManager.getActiveSubscriptionInfo(
                            mTelephonyManager.getSubIdForPhoneAccount(account));

                    if (subInfo != null) {
                        intent = new Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        SubscriptionInfoHelper.addExtrasToIntent(intent, subInfo);
                        /// M: Add for C2K solution2 @{
                        if (shouldCheckDisable && TelephonyUtilsEx.getMainPhoneId()
                                != SubscriptionManager.getPhoneId(subInfo.getSubscriptionId())) {
                            shouldEnable = false;
                        }
                        /// @}

                        /// M: Add for C2K solution 1.5 @{
                        if (shouldCheckCCardDisable) {
                            int phoneId = SubscriptionManager.from(
                                    getActivity()).getPhoneId(
                                            subInfo.getSubscriptionId());
                            if (!TelephonyUtilsEx.isGCardInserted(phoneId)) {
                                shouldEnable = false;
                            }
                        }
                      /// @}
                    }
                }
            } else {
                intent = buildPhoneAccountConfigureIntent(getActivity(), handle);
            }

            // Create the preference & add the label
            Preference accountPreference = new Preference(getActivity());
            accountPreference.setTitle(account.getLabel());

            /// M: add for C2K solution2 @{
            mAccountNumber += 1;
            accountPreference.setKey("account" + mAccountNumber);
            /// @}

            // Add an icon.
            Icon icon = account.getIcon();
            if (icon != null) {
                accountPreference.setIcon(icon.loadDrawable(getActivity()));
            }

            // Add an intent to send the user to the account's settings.
            if (intent != null) {
                accountPreference.setIntent(intent);
            }

            accountPreference.setOrder(order++);
            mAccountList.addPreference(accountPreference);

            ///M: Add for C2K solution2 @{
            Log.d(LOG_TAG, "[initAccountList] shouldEnable " + shouldEnable);
            accountPreference.setEnabled(shouldEnable);
            /// @}
        }
    }

    private boolean shouldShowConnectionServiceList(List<PhoneAccountHandle> allNonSimAccounts) {
        return mTelephonyManager.isMultiSimEnabled() || allNonSimAccounts.size() > 0;
    }

    private List<PhoneAccountHandle> getCallingAccounts(
            boolean includeSims, boolean includeDisabledAccounts) {
        PhoneAccountHandle emergencyAccountHandle = getEmergencyPhoneAccount();

        List<PhoneAccountHandle> accountHandles =
                mTelecomManager.getCallCapablePhoneAccounts(includeDisabledAccounts);
        for (Iterator<PhoneAccountHandle> i = accountHandles.iterator(); i.hasNext();) {
            PhoneAccountHandle handle = i.next();
            if (handle.equals(emergencyAccountHandle)) {
                // never include emergency call accounts in this piece of code.
                i.remove();
                continue;
            }

            PhoneAccount account = mTelecomManager.getPhoneAccount(handle);
            if (account == null) {
                i.remove();
            } else if (!includeSims &&
                    account.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)) {
                i.remove();
            }
        }
        return accountHandles;
    }

    private String nullToEmpty(String str) {
        return str == null ? "" : str;
    }

    private PhoneAccountHandle getEmergencyPhoneAccount() {
        return PhoneUtils.makePstnPhoneAccountHandleWithPrefix(
                (Phone) null, "" /* prefix */, true /* isEmergency */);
    }

    public static Intent buildPhoneAccountConfigureIntent(
            Context context, PhoneAccountHandle accountHandle) {
        Intent intent = buildConfigureIntent(
                context, accountHandle, TelecomManager.ACTION_CONFIGURE_PHONE_ACCOUNT);

        if (intent == null) {
            // If the new configuration didn't work, try the old configuration intent.
            intent = buildConfigureIntent(
                    context, accountHandle, LEGACY_ACTION_CONFIGURE_PHONE_ACCOUNT);
            if (intent != null) {
                Log.w(LOG_TAG, "Phone account using old configuration intent: " + accountHandle);
            }
        }
        return intent;
    }

    private static Intent buildConfigureIntent(
            Context context, PhoneAccountHandle accountHandle, String actionStr) {
        if (accountHandle == null || accountHandle.getComponentName() == null ||
                TextUtils.isEmpty(accountHandle.getComponentName().getPackageName())) {
            return null;
        }

        // Build the settings intent.
        Intent intent = new Intent(actionStr);
        intent.setPackage(accountHandle.getComponentName().getPackageName());
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle);

        // Check to see that the phone account package can handle the setting intent.
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolutions = pm.queryIntentActivities(intent, 0);
        if (resolutions.size() == 0) {
            intent = null;  // set no intent if the package cannot handle it.
        }

        return intent;
    }

    //----------------------------MTK------------------------
    private static final String TAG = "PhoneAccountSettingsFragment";
    private CheckBoxPreference mButtonDualMic;
    /// Add for [ANC] (Active Noise Reduction)
    private CheckBoxPreference mButtonANC;
    /// Add for [MagiConference]
    private CheckBoxPreference mButtonMagiConference;
    /// Add for [HAC]
    private CheckBoxPreference mButtonHAC;

    private static final String BUTTON_DUAL_MIC_KEY = "button_dual_mic_key";
    private static final String BUTTON_ANC_KEY = "button_anc_key";
    private static final String BUTTON_MAGI_CONFERENCE_KEY = "button_magi_conference_key";
    private static final String BUTTON_HAC_KEY = "button_hac_key";

    ///M: Add for C2K solution2 @{
    private int mAccountNumber = 0;
    /// @}

    private void configCallFeatures() {
        mButtonDualMic = (CheckBoxPreference) getPreferenceScreen()
                .findPreference(BUTTON_DUAL_MIC_KEY);
        mButtonANC = (CheckBoxPreference) getPreferenceScreen().findPreference(BUTTON_ANC_KEY);
        mButtonMagiConference = (CheckBoxPreference) getPreferenceScreen().findPreference(
                BUTTON_MAGI_CONFERENCE_KEY);
        mButtonHAC = (CheckBoxPreference) getPreferenceScreen().findPreference(BUTTON_HAC_KEY);

        Log.d(TAG, "MicSupport: " + FeatureOption.isMtkDualMicSupport());
        Log.d(TAG, "ANCSupport: " + TelephonyUtils.isANCSupport());
        Log.d(TAG, "MagiConferenceSupport: " + TelephonyUtils.isMagiConferenceSupport());
        Log.d(TAG, "HacSupport(): " + TelephonyUtils.isHacSupport());

        if (TelephonyUtils.isGeminiProject()) {
            if (mButtonDualMic != null) {
                if (FeatureOption.isMtkDualMicSupport()) {
                    mButtonDualMic.setChecked(TelephonyUtils.isDualMicModeEnabled());
                    mButtonDualMic.setOnPreferenceChangeListener(this);
                } else {
                    mAccountList.removePreference(mButtonDualMic);
                    mButtonDualMic = null;
                }
            }

            if (mButtonANC != null) {
                if (TelephonyUtils.isANCSupport()) {
                    mButtonANC.setChecked(TelephonyUtils.isANCEnabled());
                    mButtonANC.setOnPreferenceChangeListener(this);
                } else {
                    mAccountList.removePreference(mButtonANC);
                    mButtonANC = null;
                }
            }

            if (mButtonMagiConference != null) {
                if (TelephonyUtils.isMagiConferenceSupport()) {
                    mButtonMagiConference.setChecked(TelephonyUtils.isMagiConferenceEnable());
                    mButtonMagiConference.setOnPreferenceChangeListener(this);
                } else {
                    mAccountList.removePreference(mButtonMagiConference);
                    mButtonMagiConference = null;
                }
            }

            if (mButtonHAC != null) {
                if (TelephonyUtils.isHacSupport()) {
                    ContentResolver contentResolver = getActivity().getContentResolver();

                    int hac = Settings.System.getInt(
                            contentResolver, Settings.System.HEARING_AID, 0);
                    /// Add for ALPS01973645 Mota upgrade. @{
                    int hacModem = TelephonyUtils.isHacEnable();
                    Log.d(TAG, "[initUi] hac : hacModem = " + hac + " : " + hacModem);

                    if (hac != hacModem) {
                        Settings.System.putInt(
                                contentResolver, Settings.System.HEARING_AID, hacModem);
                        hac = hacModem;
                    }
                    /// @}
                    mButtonHAC.setChecked(hac != 0);
                    mButtonHAC.setOnPreferenceChangeListener(this);
                } else {
                    mAccountList.removePreference(mButtonHAC);
                    mButtonHAC = null;
                }
            }
        } else {
            if (mButtonDualMic != null) {
                mAccountList.removePreference(mButtonDualMic);
            }
            if (mButtonANC != null) {
                mAccountList.removePreference(mButtonANC);
            }
            if (mButtonMagiConference != null) {
                mAccountList.removePreference(mButtonMagiConference);
            }
            if (mButtonHAC != null) {
                mAccountList.removePreference(mButtonHAC);
            }
        }
    }

    private boolean onPreferenceChangeEx(Preference preference) {
        if (preference.getKey().equals(BUTTON_DUAL_MIC_KEY)) {
            boolean isChecked = mButtonDualMic.isChecked();
            Log.d(TAG, "onPreferenceChange mButtonDualmic turn on : " + isChecked);

            TelephonyUtils.setDualMicMode(isChecked ? "0" : "1");
            return true;
        } else if (preference.getKey().equals(BUTTON_ANC_KEY)) {
            boolean isChecked = mButtonANC.isChecked();
            TelephonyUtils.setANCEnable(isChecked);
            Log.d(TAG, "onPreferenceChange mButtonANC turn on : " + isChecked);

            mButtonANC.setSummary(isChecked ? R.string.anc_off : R.string.anc_on);
            return true;
        } else if (preference.getKey().equals(BUTTON_MAGI_CONFERENCE_KEY)) {
            boolean isChecked = mButtonMagiConference.isChecked();
            Log.d(TAG, "onPreferenceChange mButtonMagiConference turn on : " + isChecked);

            TelephonyUtils.setMagiConferenceEnable(!isChecked);
            return true;
        } else if (preference.getKey().equals(BUTTON_HAC_KEY)) {
            int hac = mButtonHAC.isChecked() ? 0 : 1;
            Log.d(TAG, "onPreferenceChange mButtonHAC turn on : " + hac);

            // Update HAC value in Settings database
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HEARING_AID, hac);

            AudioManager audioManager = (AudioManager)
                    getActivity().getSystemService(Context.AUDIO_SERVICE);
            // Update HAC Value in AudioManager
            audioManager.setParameter(
                    SettingsConstants.HAC_KEY,
                    hac == SettingsConstants.HAC_ENABLED
                            ? SettingsConstants.HAC_VAL_ON : SettingsConstants.HAC_VAL_OFF);
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    ///M: Add for C2K solution2 @{
    @Override
    public void onStop() {
        if (mDefaultOutgoingAccount != null) {
            Dialog dialog = mDefaultOutgoingAccount.getDialog();
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            mDefaultOutgoingAccount.onStop();
        }
        super.onStop();
    }
    /// @}

    /**
     * Add for C2K solution 2 improvement & 1.5 Should disable
     * mDefaultOutgoingAccount when: change account may cause SIM switch &
     * airplane mode is on.
     */
    private void updateDefaultOutgoingAccountPreference() {
        if (mDefaultOutgoingAccount != null) {

            List<PhoneAccountHandle> enabledAccounts =
                    getCallingAccounts(true /* includeSims */, false /* includeDisabled */);

            initAccountList(enabledAccounts);

            Log.d(LOG_TAG, "enabledAccounts: " + enabledAccounts.size());

            if (enabledAccounts.size() < 2) {
                mDefaultOutgoingAccount.setEnabled(false);
                return;
            }

            boolean mayCauseSimSwitch =
                    TelephonyUtilsEx.isMultiCdmaCardInsertedInHomeNetwork() ? true : false;

            if (!FeatureOption.isMtkSvlteSolutionSupport()
                    && FeatureOption.isMTKSimSwitchSupport()) {
                if (TelephonyUtilsEx.isCGCardInserted()) {
                    mayCauseSimSwitch = true;
                }
            }

            Log.d(LOG_TAG, "updateDefaultOutgoingAccount mayCauseSimSiwtch:" + mayCauseSimSwitch);

            if (mayCauseSimSwitch) {
                boolean isAirplaneModeEnabled = TelephonyUtils.isAirplaneModeOn(
                        PhoneGlobals.getInstance());
                boolean isInSwitching = TelephonyUtilsEx.isCapabilitySwitching();
                Log.d(LOG_TAG, "onReceive mAirplaneModeEnabled : " + isAirplaneModeEnabled
                        + "isInSwitching : " + isInSwitching);

                mDefaultOutgoingAccount.setEnabled(!isAirplaneModeEnabled && !isInSwitching);
                updateDefaultOutgoingAccountsModel();
            }
        }
    }

    ///M: Add for C2K solution2 @{
    private void removeOldAccounts(PreferenceCategory accountList) {
        for (; mAccountNumber > 0 ; mAccountNumber --) {
            Preference accountPreference = accountList.findPreference("account" + mAccountNumber);
            if (accountPreference != null) {
                accountList.removePreference(accountPreference);
            }
        }
    }
    /// @}

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(LOG_TAG, "onReceive action = " + action);
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED) ||
                    action.equals(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_DONE) ||
                    action.equals(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_FAILED) ||
                    action.equals(TelecomManagerEx.ACTION_PHONE_ACCOUNT_CHANGED)) {
                updateDefaultOutgoingAccountPreference();
            }
        }
    };
}
