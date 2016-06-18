/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.services.telephony;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.android.ims.ImsConfig;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.phone.PhoneGlobals;

import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.mediatek.phone.PhoneFeatureConstants;
import com.mediatek.telecom.TelecomManagerEx;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Owns all data we have registered with Telecom including handling dynamic addition and
 * removal of SIMs and SIP accounts.
 */
final class TelecomAccountRegistry {
    private static final boolean DBG = false; /* STOP SHIP if true */

    // This icon is the one that is used when the Slot ID that we have for a particular SIM
    // is not supported, i.e. SubscriptionManager.INVALID_SLOT_ID or the 5th SIM in a phone.
    private final static int DEFAULT_SIM_ICON =  R.drawable.ic_multi_sim;

    /// M: WFC, True if phone is capable of wifi calling @{
    private boolean mWifiCallEnabled = false;
    private int mPhoneWifiCallEnabled = -1;
    /// @}

    final class AccountEntry implements PstnPhoneCapabilitiesNotifier.Listener {
        private final Phone mPhone;
        private final PhoneAccount mAccount;
        private final PstnIncomingCallNotifier mIncomingCallNotifier;
        private final PstnPhoneCapabilitiesNotifier mPhoneCapabilitiesNotifier;
        private boolean mIsVideoCapable;
        private boolean mIsVideoPauseSupported;

        AccountEntry(Phone phone, boolean isEmergency, boolean isDummy) {
            mPhone = phone;
            mAccount = registerPstnPhoneAccount(isEmergency, isDummy);
            /**
             * M: [log optimize] useless log.
            Log.i(this, "Registered phoneAccount: %s with handle: %s",
                    mAccount, mAccount.getAccountHandle());
             */
            mIncomingCallNotifier = new PstnIncomingCallNotifier((PhoneProxy) mPhone);
            mPhoneCapabilitiesNotifier = new PstnPhoneCapabilitiesNotifier((PhoneProxy) mPhone,
                    this);
        }

        void teardown() {
            mIncomingCallNotifier.teardown();
            mPhoneCapabilitiesNotifier.teardown();
        }

        /**
         * Registers the specified account with Telecom as a PhoneAccountHandle.
         * M: For ALPS01965388. Only create a PhoneAccount but not register it to Telecom.
         */
        private PhoneAccount registerPstnPhoneAccount(boolean isEmergency,
                                                    boolean isDummyAccount) {
            String dummyPrefix = isDummyAccount ? "Dummy " : "";

            // Build the Phone account handle.
            PhoneAccountHandle phoneAccountHandle =
                    PhoneUtils.makePstnPhoneAccountHandleWithPrefix(
                            mPhone, dummyPrefix, isEmergency);

            // Populate the phone account data.
            int subId = mPhone.getSubId();
            int color = PhoneAccount.NO_HIGHLIGHT_COLOR;
            int slotId = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
            /// M: for ALPS01804842, set sub number as address, so no need line1Number. @{
            // Original code:
            // String line1Number = mTelephonyManager.getLine1NumberForSubscriber(subId);
            // if (line1Number == null) {
            //     line1Number = "";
            // }
            /// @}
            String subNumber = mPhone.getPhoneSubInfo().getLine1Number(
                    mPhone.getContext().getOpPackageName());
            /// M: delete for fix some CR.
            // if (subNumber == null) {
            //    subNumber = "";
            // }

            String label;
            String description;
            Icon icon = null;

            // We can only get the real slotId from the SubInfoRecord, we can't calculate the
            // slotId from the subId or the phoneId in all instances.
            SubscriptionInfo record =
                    mSubscriptionManager.getActiveSubscriptionInfo(subId);

            if (isEmergency) {
                label = mContext.getResources().getString(R.string.sim_label_emergency_calls);
                description =
                    mContext.getResources().getString(R.string.sim_description_emergency_calls);
            }
            /// M: for ALPS01774567, remove these code, make the account name always same. @{
            // Original code:
            // else if (mTelephonyManager.getPhoneCount() == 1) {
            // For single-SIM devices, we show the label and description as whatever the name of
            //     // the network is.
            //     description = label = mTelephonyManager.getNetworkOperatorName();
            // }
            /// @}
            else {
                // M: for ALPS01772299, don't change it if name is empty, init it as "".
                CharSequence subDisplayName = "";
                if (record != null) {
                    subDisplayName = record.getDisplayName();
                    slotId = record.getSimSlotIndex();
                    color = record.getIconTint();
                    // M: for ALPS01804842, set sub number as address
                    subNumber = record.getNumber();
                }

                String slotIdString;
                if (SubscriptionManager.isValidSlotId(slotId)) {
                    slotIdString = Integer.toString(slotId);
                } else {
                    slotIdString = mContext.getResources().getString(R.string.unknown);
                }

                /// M: for ALPS01772299, don't change it if name is empty. @{
                // original code:
                // if (TextUtils.isEmpty(subDisplayName)) {
                //     // Either the sub record is not there or it has an empty display name.
                //     Log.w(this, "Could not get a display name for subid: %d", subId);
                //     subDisplayName = mContext.getResources().getString(
                //             R.string.sim_description_default, slotIdString);
                //     subDisplayName = "";
                // }
                /// @}

                // The label is user-visible so let's use the display name that the user may
                // have set in Settings->Sim cards.
                label = dummyPrefix + subDisplayName;
                description = dummyPrefix + mContext.getResources().getString(
                                R.string.sim_description_default, slotIdString);
            }
            if (subNumber == null) {
                subNumber = "";
            }

            // By default all SIM phone accounts can place emergency calls.
            int capabilities = PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION |
                    PhoneAccount.CAPABILITY_CALL_PROVIDER |
                    PhoneAccount.CAPABILITY_PLACE_EMERGENCY_CALLS |
                    PhoneAccount.CAPABILITY_MULTI_USER;

            // M: attach extended capabilities for the PhoneAccount
            capabilities |= getExtendedCapabilities();

            mIsVideoCapable = mPhone.isVideoEnabled();
            Log.d(this, "mIsVideoCapable = " + mIsVideoCapable);
            /// M: manual open video call entry
            // if the property "manual.enable.video.call" value is 1,
            // then enable, other value disable.
            int val = SystemProperties.getInt("manual.enable.video.call", -1);
            if (mIsVideoCapable || val == 1) {
                capabilities |= PhoneAccount.CAPABILITY_VIDEO_CALLING;
            }

            if (record != null) {
                updateVideoPauseSupport(record);

                if ((capabilities & PhoneAccount.CAPABILITY_UNAVAILABLE_FOR_CALL)
                        == PhoneAccount.CAPABILITY_UNAVAILABLE_FOR_CALL) {
                    icon = Icon.createWithBitmap(record.createIconBitmap(mContext, Color.GRAY));
                } else {
                    icon = Icon.createWithBitmap(record.createIconBitmap(mContext));
                }
            }

            Resources res = mContext.getResources();
            if (icon == null) {
                // TODO: Switch to using Icon.createWithResource() once that supports tinting.
                Drawable drawable = res.getDrawable(DEFAULT_SIM_ICON, null);
                drawable.setTint(res.getColor(R.color.default_sim_icon_tint_color, null));
                drawable.setTintMode(PorterDuff.Mode.SRC_ATOP);

                int width = drawable.getIntrinsicWidth();
                int height = drawable.getIntrinsicHeight();
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);

                icon = Icon.createWithBitmap(bitmap);
            }

            PhoneAccount account = PhoneAccount.builder(phoneAccountHandle, label)
                     // M: for ALPS01804842, set sub number as address
                    .setAddress(Uri.fromParts(PhoneAccount.SCHEME_TEL, subNumber, null))
                    .setSubscriptionAddress(
                            Uri.fromParts(PhoneAccount.SCHEME_TEL, subNumber, null))
                    .setCapabilities(capabilities)
                    .setIcon(icon)
                    .setHighlightColor(color)
                    .setShortDescription(description)
                    .setSupportedUriSchemes(Arrays.asList(
                            PhoneAccount.SCHEME_TEL, PhoneAccount.SCHEME_VOICEMAIL))
                    .build();

            /// M: For ALPS01965388.
            /** Original code:
            // Register with Telecom and put into the account entry.
            mTelecomManager.registerPhoneAccount(account);
            */
            updateAccountChangeStatus(account);
            /// @}
            return account;
        }

        public PhoneAccountHandle getPhoneAccountHandle() {
            return mAccount != null ? mAccount.getAccountHandle() : null;
        }

        /**
         * Updates indicator for this {@link AccountEntry} to determine if the carrier supports
         * pause/resume signalling for IMS video calls.  The carrier setting is stored in MNC/MCC
         * configuration files.
         *
         * @param subscriptionInfo The subscription info.
         */
        private void updateVideoPauseSupport(SubscriptionInfo subscriptionInfo) {
            // Get the configuration for the MNC/MCC specified in the current subscription info.
            Configuration configuration = new Configuration();
            if (subscriptionInfo.getMcc() == 0 && subscriptionInfo.getMnc() == 0) {
                Configuration config = mContext.getResources().getConfiguration();
                configuration.mcc = config.mcc;
                configuration.mnc = config.mnc;
                Log.i(this, "updateVideoPauseSupport -- no mcc/mnc for sub: " + subscriptionInfo +
                        " using mcc/mnc from main context: " + configuration.mcc + "/" +
                        configuration.mnc);
            } else {
                Log.i(this, "updateVideoPauseSupport -- mcc/mnc for sub: " + subscriptionInfo);

                configuration.mcc = subscriptionInfo.getMcc();
                configuration.mnc = subscriptionInfo.getMnc();
            }

            // Check if IMS video pause is supported.
            PersistableBundle b =
                    PhoneGlobals.getInstance().getCarrierConfigForSubId(mPhone.getSubId());
            mIsVideoPauseSupported
                    = b.getBoolean(CarrierConfigManager.KEY_SUPPORT_PAUSE_IMS_VIDEO_CALLS_BOOL);
        }

        /**
         * Receives callback from {@link PstnPhoneCapabilitiesNotifier} when the video capabilities
         * have changed.
         *
         * @param isVideoCapable {@code true} if video is capable.
         */
        @Override
        public void onVideoCapabilitiesChanged(boolean isVideoCapable) {
            mIsVideoCapable = isVideoCapable;
        }

        /**
         * Indicates whether this account supports pausing video calls.
         * @return {@code true} if the account supports pausing video calls, {@code false}
         * otherwise.
         */
        public boolean isVideoPauseSupported() {
            return mIsVideoCapable && mIsVideoPauseSupported;
        }

        /**
         * M: get extended PhoneAccount capabilities currently.
         * @return the extended capability bit mask.
         */
        private int getExtendedCapabilities() {
            int extendedCapabilities = 0;
            /// M: add WFC capability @{
            if (mWifiCallEnabled && mPhone.getPhoneId() == mPhoneWifiCallEnabled) {
                Log.v(this, "[WFC] getExtendedCapabilities, mWifiCallEnabled true");
                extendedCapabilities |= PhoneAccount.CAPABILITY_WIFI_CALLING;
            }
            /// @}
            boolean isImsReg = false;
            boolean isImsEnabled = (ImsConfig.FeatureValueConstants.ON == Settings.Global.getInt(
                  mPhone.getContext().getContentResolver(),
                  Settings.Global.ENHANCED_4G_MODE_ENABLED, ImsConfig.FeatureValueConstants.OFF));
            if (isImsEnabled) {
                try {
                    ImsManager imsManager = ImsManager.getInstance(mContext, mPhone.getPhoneId());
                    isImsReg = imsManager.getImsRegInfo();
                    ///M: we need also check if current network type is LTE or LTE+
                    // to add VoLTE calling capability, if not remove the volte capability. @{
                    int networkType = mTelephonyManager.getNetworkType(mPhone.getSubId());
                    if (isImsReg && (networkType == TelephonyManager.NETWORK_TYPE_LTE ||
                            networkType == TelephonyManager.NETWORK_TYPE_LTEA)) {
                        extendedCapabilities |= PhoneAccount.CAPABILITY_VOLTE_CALLING;
                        /// For Volte enhanced conference feature. @{
                        if (mPhone.isFeatureSupported(Phone.FeatureType.VOLTE_ENHANCED_CONFERENCE)) {
                            extendedCapabilities |= PhoneAccount.CAPABILITY_VOLTE_CONFERENCE_ENHANCED;
                        }
                        /// @}
                    }
                    /// @}
                } catch (ImsException e) {
                    Log.v(this, "Get IMS register info fail.");
                }
            }
            Log.v(this, "Ims enabled = " + isImsEnabled + " isImsReg = " + isImsReg);

            /// Added for EVDO world phone.@{
            if (mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                extendedCapabilities |= PhoneAccount.CAPABILITY_CDMA_CALL_PROVIDER;
            }
            /// @}
            /// M: added for c2k solution 1.5/2.0 @{
            if (isPhoneUnAvailableForCall(mPhone.getSubId())) {
                extendedCapabilities |= PhoneAccount.CAPABILITY_UNAVAILABLE_FOR_CALL;
            }
            /// @}
            return extendedCapabilities;
        }
    }

    private OnSubscriptionsChangedListener mOnSubscriptionsChangedListener =
            new OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            // Any time the SubscriptionInfo changes...rerun the setup
            tearDownAccounts();
            setupAccounts();
            // M: broadcast pstn accounts changed
            broadcastAccountChanged();

            /// M: Maybe need to update PhoneStateListener.
            updatePhoneStateListeners();
        }
    };

    /**
     * M: for multi-sub, all subs should be listen, not only the default one.
     * original code:
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            int newState = serviceState.getState();
            if (newState == ServiceState.STATE_IN_SERVICE && mServiceState != newState) {
                tearDownAccounts();
                setupAccounts();
            }
            mServiceState = newState;
        }
    };
     */

    private static TelecomAccountRegistry sInstance;
    private final Context mContext;
    private final TelecomManager mTelecomManager;
    private final TelephonyManager mTelephonyManager;
    private final SubscriptionManager mSubscriptionManager;
    private List<AccountEntry> mAccounts = new LinkedList<AccountEntry>();
    /**
     * M: should keep service state for all subs.
     * original code:
    private int mServiceState = ServiceState.STATE_POWER_OFF;
     */
    private final Map<Integer, Integer> mServiceStates = new ArrayMap<Integer, Integer>();

    // TODO: Remove back-pointer from app singleton to Service, since this is not a preferred
    // pattern; redesign. This was added to fix a late release bug.
    private TelephonyConnectionService mTelephonyConnectionService;

    TelecomAccountRegistry(Context context) {
        mContext = context;
        mTelecomManager = TelecomManager.from(context);
        mTelephonyManager = TelephonyManager.from(context);
        mSubscriptionManager = SubscriptionManager.from(context);
    }

    static synchronized final TelecomAccountRegistry getInstance(Context context) {
        if (sInstance == null && context != null) {
            sInstance = new TelecomAccountRegistry(context);
        }
        return sInstance;
    }

    void setTelephonyConnectionService(TelephonyConnectionService telephonyConnectionService) {
        this.mTelephonyConnectionService = telephonyConnectionService;
    }

    TelephonyConnectionService getTelephonyConnectionService() {
        return mTelephonyConnectionService;
    }

    /**
     * Determines if the {@link AccountEntry} associated with a {@link PhoneAccountHandle} supports
     * pausing video calls.
     *
     * @param handle The {@link PhoneAccountHandle}.
     * @return {@code True} if video pausing is supported.
     */
    boolean isVideoPauseSupported(PhoneAccountHandle handle) {
        for (AccountEntry entry : mAccounts) {
            if (entry.getPhoneAccountHandle().equals(handle)) {
                return entry.isVideoPauseSupported();
            }
        }
        return false;
    }

    /**
     * Sets up all the phone accounts for SIMs on first boot.
     */
    void setupOnBoot() {
        // TODO: When this object "finishes" we should unregister by invoking
        // SubscriptionManager.getInstance(mContext).unregister(mOnSubscriptionsChangedListener);
        // This is not strictly necessary because it will be unregistered if the
        // notification fails but it is good form.

        // Register for SubscriptionInfo list changes which is guaranteed
        // to invoke onSubscriptionsChanged the first time.
        SubscriptionManager.from(mContext).addOnSubscriptionsChangedListener(
                mOnSubscriptionsChangedListener);

        // We also need to listen for changes to the service state (e.g. emergency -> in service)
        // because this could signal a removal or addition of a SIM in a single SIM phone.
        /**
         * M: for multi-sub, all subs has should be listen
         * Original code:
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
         */
        listenPhoneState();
        registerReceiver();
        /** @} */
    }

    /**
     * Determines if the list of {@link AccountEntry}(s) contains an {@link AccountEntry} with a
     * specified {@link PhoneAccountHandle}.
     *
     * @param handle The {@link PhoneAccountHandle}.
     * @return {@code True} if an entry exists.
     */
    boolean hasAccountEntryForPhoneAccount(PhoneAccountHandle handle) {
        for (AccountEntry entry : mAccounts) {
            if (entry.getPhoneAccountHandle().equals(handle)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Un-registers any {@link PhoneAccount}s which are no longer present in the list
     * {@code AccountEntry}(s).
     */
    private void cleanupPhoneAccounts() {
        ComponentName telephonyComponentName =
                new ComponentName(mContext, TelephonyConnectionService.class);
        List<PhoneAccountHandle> accountHandles =
                mTelecomManager.getCallCapablePhoneAccounts(true /* includeDisabled */);
        for (PhoneAccountHandle handle : accountHandles) {
            if (telephonyComponentName.equals(handle.getComponentName()) &&
                    !hasAccountEntryForPhoneAccount(handle)) {
                Log.i(this, "Unregistering phone account %s.", handle);
                mTelecomManager.unregisterPhoneAccount(handle);
                /// M: for ALPS01965388.@{
                mPhoneAccountChanged = true;
                /// @}
            }
        }
    }

    private void setupAccounts() {
        // Go through SIM-based phones and register ourselves -- registering an existing account
        // will cause the existing entry to be replaced.
        Phone[] phones = PhoneFactory.getPhones();
        Log.d(this, "Found %d phones.  Attempting to register.", phones.length);
        for (Phone phone : phones) {
            int subscriptionId = phone.getSubId();
            Log.d(this, "Phone with subscription id %d", subscriptionId);
            if (subscriptionId >= 0) {
                mAccounts.add(new AccountEntry(phone, false /* emergency */, false /*isDummy*/));
            }
        }

        /// M: for ALPS01809899, do not register emergency account.
        // because it just indicate emergency call show use TelephonyConnectionService, it has no
        // actually use in MO or MT, and UI never want see it. @{
        // original code:
        // // If we did not list ANY accounts, we need to provide a "default" SIM account
        // // for emergency numbers since no actual SIM is needed for dialing emergency
        // // numbers but a phone account is.
        // if (mAccounts.isEmpty()) {
        //     mAccounts.add(new AccountEntry(PhoneFactory.getDefaultPhone(), true /* emergency */,
        //             false /* isDummy */));
        // }
        /// @}

        // Add a fake account entry.
        if (DBG && phones.length > 0 && "TRUE".equals(System.getProperty("dummy_sim"))) {
            mAccounts.add(new AccountEntry(phones[0], false /* emergency */, true /* isDummy */));
        }

        /// M: Added for ALPS01965388. @{
        registerIfAccountChanged();
        /// @}

        // Clean up any PhoneAccounts that are no longer relevant
        cleanupPhoneAccounts();

        // At some point, the phone account ID was switched from the subId to the iccId.
        // If there is a default account, check if this is the case, and upgrade the default account
        // from using the subId to iccId if so.
        PhoneAccountHandle defaultPhoneAccount =
                mTelecomManager.getUserSelectedOutgoingPhoneAccount();
        ComponentName telephonyComponentName =
                new ComponentName(mContext, TelephonyConnectionService.class);

        if (defaultPhoneAccount != null &&
                telephonyComponentName.equals(defaultPhoneAccount.getComponentName()) &&
                !hasAccountEntryForPhoneAccount(defaultPhoneAccount)) {

            String phoneAccountId = defaultPhoneAccount.getId();
            if (!TextUtils.isEmpty(phoneAccountId) && TextUtils.isDigitsOnly(phoneAccountId)) {
                PhoneAccountHandle upgradedPhoneAccount =
                        PhoneUtils.makePstnPhoneAccountHandle(
                                PhoneGlobals.getPhone(Integer.parseInt(phoneAccountId)));

                if (hasAccountEntryForPhoneAccount(upgradedPhoneAccount)) {
                    mTelecomManager.setUserSelectedOutgoingPhoneAccount(upgradedPhoneAccount);
                }
            }
        }
    }

    private void tearDownAccounts() {
        for (AccountEntry entry : mAccounts) {
            entry.teardown();
        }
        mAccounts.clear();
    }


    // ---------------------------------------mtk --------------------------------------//

    // For ALPS01965388. Use to mark whether there have one or more PhoneAccounts changed.
    private boolean mPhoneAccountChanged = false;
    // For ALPS02077289. Used to save the network type for all subs.
    private final Map<Integer, Integer> mNetworkType = new ArrayMap<Integer, Integer>();
    // For ALPS02077289. For multi-sub, all subs should be listened.
    private Map<Integer, PhoneStateListener> mPhoneStateListeners
                           = new ArrayMap<Integer, PhoneStateListener>();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean rebuildAccounts;
            String action = intent.getAction();
            // Check for extended broadcast to rebuild accounts
            rebuildAccounts = needRebuildAccounts(intent);
            Log.d(this, "onReceive, action is " + action + "; rebuildAccounts is "
                    + rebuildAccounts);
            if (rebuildAccounts) {
                tearDownAccounts();
                setupAccounts();
                 // Broadcast pstn accounts changed
                broadcastAccountChanged();
            }
        }

        /**
         * Check the extended broadcast to determine whether to rebuild
         * the PhoneAccounts.
         * @param intent the intent.
         * @return true if rebuild needed
         */
        private boolean needRebuildAccounts(Intent intent) {
            boolean rebuildAccounts = false;
            String action = intent.getAction();
            if (ImsManager.ACTION_IMS_STATE_CHANGED.equals(action)) {
                /**
                 * Keys within this Intent:
                 * ImsManager.EXTRA_IMS_REG_STATE_KEY
                 * ImsManager.EXTRA_PHONE_ID
                 */
                int reg = intent.getIntExtra(ImsManager.EXTRA_IMS_REG_STATE_KEY, -1);
                mPhoneWifiCallEnabled = intent.getIntExtra(ImsManager.EXTRA_PHONE_ID, -1);
                Log.d(this, "ACTION_IMS_STATE_CHANGED, new state: %s, phoneId: %s", reg, mPhoneWifiCallEnabled);
                rebuildAccounts = true;
                /// M: control the switch of WFC @{
                boolean[] enabledFeatures = intent.getBooleanArrayExtra(
                        ImsManager.EXTRA_IMS_ENABLE_CAP_KEY);

                if (enabledFeatures != null &&
                        enabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_WIFI]) {
                    mWifiCallEnabled = true;
                } else {
                    mWifiCallEnabled = false;
                }
                Log.d(this, "needRebuildAccounts, mWifiCallEnabled: " + mWifiCallEnabled);
                /// @}
            }
            return rebuildAccounts;
        }
    };

    /**
     * Notify pstn account changed.
     * TODO: need refactory this part and broadcast account changed by PhoneAccountRegistrar.
     */
    private void broadcastAccountChanged() {
        /// Modified for ALPS01965388.@{
        if (mPhoneAccountChanged) {
            Log.d(this, "broadcastAccountChanged");
            mPhoneAccountChanged = false;
            Intent intent = new Intent(TelecomManagerEx.ACTION_PHONE_ACCOUNT_CHANGED);
            mContext.sendBroadcast(intent);
        }
        /// @}
    }

    /**
     * Register receiver to more broadcast, like IMS state changed.
     * @param intentFilter the target IntentFilter.
     */
    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        // Receive extended broadcasts like IMS state changed
        intentFilter.addAction(ImsManager.ACTION_IMS_STATE_CHANGED);
        mContext.registerReceiver(mReceiver, intentFilter);
    }

    /**
     * For multi-sub, all subs should be listened.
     */
    private void listenPhoneState() {
        Phone[] phones = PhoneFactory.getPhones();
        for (Phone phone : phones) {
            int subscriptionId = phone.getSubId();
            if (subscriptionId >= 0 && !mPhoneStateListeners.containsKey(subscriptionId)) {
                if (!mServiceStates.containsKey(subscriptionId)) {
                    mServiceStates.put(subscriptionId, ServiceState.STATE_POWER_OFF);
                }
                /// For ALPS02077289. @{
                if (!mNetworkType.containsKey(subscriptionId)) {
                    mNetworkType.put(subscriptionId,
                            mTelephonyManager.getNetworkType(subscriptionId));
                }
                /// @}
                PhoneStateListener listener = new PhoneStateListener(subscriptionId) {
                    @Override
                    public void onServiceStateChanged(ServiceState serviceState) {
                        boolean rebuildAccounts = false;
                        int newState = serviceState.getState();
                        if (newState == ServiceState.STATE_IN_SERVICE
                                && mServiceStates.get(mSubId) != newState) {
                            Log.d(this, "[PhoneStateListener]ServiceState of sub %s changed "
                                    + "%s -> IN_SERVICE, reset PhoneAccount", mSubId,
                                    mServiceStates.get(mSubId));
                            rebuildAccounts = true;
                        }
                        mServiceStates.put(mSubId, newState);

                        /// For ALPS02077289. @{
                        // After SRVCC, the network maybe GSM or other, but IMS registered
                        // state won't change before current call disconnected.
                        // But we cannot make call over IMS, so needs to update
                        // related PhoneAccounts.
                        int newNetworkType = mTelephonyManager.getNetworkType(mSubId);
                        int oldNetworkType = mNetworkType.get(mSubId);
                        if (newNetworkType != oldNetworkType && !rebuildAccounts) {
                            rebuildAccounts = true;
                            Log.d(this, "Network type changed and need rebuild PhoneAccounts.");
                        }
                        mNetworkType.put(mSubId, newNetworkType);
                        Log.d(this, "mSubId = "+ mSubId +" service state = " + newState +
                                " new network type = " + newNetworkType +
                                " old network type = " +oldNetworkType);
                        if (rebuildAccounts) {
                            // TODO: each sub-account should be reset alone
                            tearDownAccounts();
                            setupAccounts();
                            broadcastAccountChanged();
                        }
                        /// @}
                    }
                };
                mTelephonyManager.listen(listener, PhoneStateListener.LISTEN_SERVICE_STATE);
                mPhoneStateListeners.put(subscriptionId, listener);
            }
        }
    }

    private void updatePhoneStateListeners() {
        Log.d(this, "updatePhoneStateListeners");

        // Unregister phone listeners for inactive subscriptions.
        Iterator<Integer> itr = mPhoneStateListeners.keySet().iterator();
        while (itr.hasNext()) {
            int subId = itr.next();
            SubscriptionInfo record = mSubscriptionManager.getActiveSubscriptionInfo(subId);
            if (record == null) {
                // Listening to LISTEN_NONE removes the listener.
                mTelephonyManager.listen(mPhoneStateListeners.get(subId),
                        PhoneStateListener.LISTEN_NONE);
                itr.remove();
            }
        }

        listenPhoneState();
    }

    /**
     * M: Update the flag which is used to determine whether accounts does change.
     * @param account
     * For ALPS01965388.
     */
    private void updateAccountChangeStatus(PhoneAccount account) {
        if (account == null) {
            Log.d(this, "updateAccountChangeStatus, account is null!");
            return;
        }

        if (mPhoneAccountChanged) {
            return;
        }

        // Get old PhoneAccount if there has one.
        PhoneAccount oldAccount = mTelecomManager.getPhoneAccount(account.getAccountHandle());
        // Check whether the account does change.
        /// M: FIXME: equals can't compare Icon, should enhance the equals method.
        if (!account.equals(oldAccount)) {
            mPhoneAccountChanged = true;
            Log.d(this, "updateAccountChangeStatus, one account changed.");
        }
    }

    /**
     * Register all PSTN PhoneAccount if any one of them changed.
     * For ALPS01965388.
     */
    private void registerIfAccountChanged() {
        if (mPhoneAccountChanged) {
            for (AccountEntry accountEntry : mAccounts) {
                Log.d(this, "[registerIfAccountChanged]registering " + accountEntry.mAccount);
                mTelecomManager.registerPhoneAccount(accountEntry.mAccount);
            }
        } else {
            Log.d(this, "registerIfAccountChanged, no PhoneAccount changed, so do nothing");
        }
    }

    /**
     * M: add for c2k solution 1.5/2.0
     * check whether the card inserted is a CDMA card and
     * working in CDMA mode (the modem support CDMA).
     * Also see {@link #isCdmaCard(int)}
     * @param slotId slot Id
     * @return
     */
    private boolean isSupportCdma(int slotId) {
        boolean isSupportCdma = false;
        String[] type = TelephonyManagerEx.getDefault().getSupportCardType(slotId);
        if (type != null) {
            for (int i = 0; i < type.length; i++) {
                if ("RUIM".equals(type[i]) || "CSIM".equals(type[i])) {
                    isSupportCdma = true;
                }
            }
        }
        Log.d(this, "slotId = " + slotId + " isSupportCdma = " + isSupportCdma);
        return isSupportCdma;
    }

    /**
     * M: add for c2k solution 1.5/2.0
     *
     * check whether the card inserted is really a CDMA card.
     * NOTICE that it will return true even the card is a CDMA card but working as a GSM SIM.
     * Also see {@link #isSupportCdma(int)}
     * @param slotId slot Id
     * @return
     */
    private boolean isCdmaCard(int slotId) {
        boolean isCdmaCard = false;
        if (isSupportCdma(slotId) || TelephonyManagerEx.getDefault().isCt3gDualMode(slotId)) {
            isCdmaCard = true;
        }
        Log.d(this, "slotId = " + slotId + " isCdmaCard = " + isCdmaCard);
        return isCdmaCard;
    }

    /**
     * add for c2k svlte project.
     *
     * solution 2.0(ro.mtk.c2k.slot2.support = 1):
     *   C+C, disable not default data account
     * solution 1.5  (ro.mtk.c2k.slot2.support = 0):
     *   C+G: disable the C account not set as default 3/4G
     *
     * @param subId
     * @return
     */
    private boolean isPhoneUnAvailableForCall(int subId){
        int counter = 0;
        int defaultDataSubId = android.telephony.SubscriptionManager
                .getDefaultDataSubId();
        Integer typeVal = mServiceStates.get(subId);
        if ( typeVal != null && typeVal.intValue() == ServiceState.STATE_IN_SERVICE) {
            return false;
        }

        int phoneNum = mTelephonyManager.getPhoneCount();
        for (int i = 0; i < phoneNum; i++) {
            if (SubscriptionManager.isValidSlotId(i) && isCdmaCard(i)) {
                counter++;
            }
        }

        int mainSlotId = -1;
        String currLteSim = SystemProperties.get("persist.radio.simswitch", "");
        Log.d(this, "current 3/4G Sim = " + currLteSim);
        if (!TextUtils.isEmpty(currLteSim)) {
            mainSlotId = Integer.parseInt(currLteSim) - 1;
        }

        /// For ALPS02307660 @{
        // C + C, we should not disable the phone account when
        // network is in roaming status
        boolean inHome = TelephonyManagerEx.getDefault().isInHomeNetwork(subId);
        Log.d(this, "subId :"  + subId + ", isInHomeNetwork = " + inHome);
        if (counter == phoneNum && inHome
                && SubscriptionManager.getSlotId(subId) != mainSlotId) {
            return true;
        }
        /// @}

        if (!PhoneFeatureConstants.FeatureOption.isMtkSvlteSolutionSupport()) {
            int soltId = SubscriptionManager.getSlotId(subId);
            //C+G, default LTE is on G
            if (counter == 1
                    && counter < phoneNum
                    && SubscriptionManager.isValidSlotId(mainSlotId)
                    && mainSlotId != soltId
                    && !isCdmaCard(mainSlotId)) {
                return true;
            }
        }
        return false;
    }
}
