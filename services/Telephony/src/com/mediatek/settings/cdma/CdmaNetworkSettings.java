package com.mediatek.settings.cdma;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.phone.MobileNetworkSettings;
import com.android.phone.R;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.IccCardConstants.CardType;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;

import com.mediatek.phone.PhoneFeatureConstants.FeatureOption;
import com.mediatek.settings.cdma.TelephonyUtilsEx;
import com.mediatek.telephony.TelephonyManagerEx;

/**
 * CDMA network setting features.
 */
public class CdmaNetworkSettings {

    private static final String TAG = "CdmaNetworkSettings";
    private static final String SINGLE_LTE_DATA = "single_lte_data";
    private static final String ENABLE_4G_DATA = "enable_4g_data";
    private static final String ROAMING_KEY = "button_roaming_key";
    private static final int preferredNetworkMode = Phone.PREFERRED_NT_MODE;

    private SwitchPreference mEnableSigle4GDataPreference;
    private SwitchPreference mEnable4GDataPreference;
    private Phone mPhone;
    private PreferenceActivity mActivity;

    private IntentFilter mIntentFilter;
    private Enable4GHandler mEnable4GHandler;
    private PreferenceScreen mPreferenceScreen;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "on receive broadcast action = " + action);

            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
                int slotId = intent.getIntExtra(PhoneConstants.SLOT_KEY,
                        SubscriptionManager.INVALID_SIM_SLOT_INDEX);
                Log.d(TAG, "slotId: " + slotId);

                if (slotId == SubscriptionManager.getSlotId(mPhone.getSubId())) {
                    updateSwitch();
                }
            }
        }
    };

    /**
     * Constructor.
     * @param activity the activity of the preference.
     * @param preference the preference to be used.
     * @param phone the phone object.
     */
    public CdmaNetworkSettings(PreferenceActivity activity, PreferenceScreen prefS, Phone phone) {
        mActivity = activity;
        mPhone = phone;
        mEnable4GHandler = new Enable4GHandler();
        mPreferenceScreen = prefS;

        /// M: remove GSM items @{
        if (mPreferenceScreen.findPreference(
                MobileNetworkSettings.BUTTON_ENABLED_NETWORKS_KEY) != null) {
            mPreferenceScreen.removePreference(mPreferenceScreen
                    .findPreference(MobileNetworkSettings.BUTTON_ENABLED_NETWORKS_KEY));
        }
        if (mPreferenceScreen.findPreference(
                MobileNetworkSettings.BUTTON_PREFERED_NETWORK_MODE) != null) {
            mPreferenceScreen.removePreference(mPreferenceScreen
                    .findPreference(MobileNetworkSettings.BUTTON_PREFERED_NETWORK_MODE));
        }
        // When in roaming, we still need show the PLMN item.
        if (!TelephonyUtilsEx.isRoaming(mPhone) && mPreferenceScreen.findPreference(
                MobileNetworkSettings.BUTTON_PLMN_LIST) != null) {
            mPreferenceScreen.removePreference(
                    mPreferenceScreen.findPreference(MobileNetworkSettings.BUTTON_PLMN_LIST));
        }
        /// @}

        addEnable4GNetworkItem();
        /// TODO: Temple solution for 4G data only @{
        if (FeatureOption.isMtkTddDataOnlySupport()) {
            addEnable4GSingleDataOnlyItem();
        }
        /// @}

        mIntentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        mActivity.registerReceiver(mReceiver, mIntentFilter);
    }

    /**
     * Handle the onResume event.
     */
    public void onResume() {
        Log.d(TAG, "onResume");

        updateSwitch();
    }

    /**
     * Handle the onDestroy event.
     */
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        mActivity.unregisterReceiver(mReceiver);
    }

    /**
     * Handle the preference item click event.
     *
     * @param preferenceScreen
     *            the preference screen.
     * @param preference
     *            the clicked preference object.
     * @return true if the event is handled.
     */
    public boolean onPreferenceTreeClick(
            PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals(SINGLE_LTE_DATA)) {
            handleEnable4GDataOnlyClick(preference);
            return true;
        } else if (preference.getKey().equals(ENABLE_4G_DATA)) {
            handleEnable4GDataClick(preference);
            return true;
        }
        return false;
    }

    private void addEnable4GSingleDataOnlyItem() {
        mEnableSigle4GDataPreference = new SwitchPreference(mActivity);
        mEnableSigle4GDataPreference.setTitle(R.string.only_use_LTE_data);
        mEnableSigle4GDataPreference.setKey(SINGLE_LTE_DATA);
        mEnableSigle4GDataPreference.setSummaryOn(R.string.only_use_LTE_data_summary);
        mEnableSigle4GDataPreference.setSummaryOff(R.string.only_use_LTE_data_summary);
        mEnableSigle4GDataPreference.setOrder(
                mPreferenceScreen.findPreference(ENABLE_4G_DATA).getOrder() + 1);
        mPreferenceScreen.addPreference(mEnableSigle4GDataPreference);
    }

    private void addEnable4GNetworkItem() {
        if (mEnable4GDataPreference == null) {
            mEnable4GDataPreference = new SwitchPreference(mActivity);
            mEnable4GDataPreference.setTitle(R.string.enable_4G_data);
            mEnable4GDataPreference.setKey(ENABLE_4G_DATA);
            mEnable4GDataPreference.setSummary(R.string.enable_4G_data_summary);
            Preference pref = mPreferenceScreen.findPreference(ROAMING_KEY);
            if (pref != null) {
                mEnable4GDataPreference.setOrder(pref.getOrder() + 1);
            }
        }
        mPreferenceScreen.addPreference(mEnable4GDataPreference);
    }

    private boolean isCallStateIDLE() {
        boolean result = false;
        TelephonyManager telephonyManager =
            (TelephonyManager) mActivity.getSystemService(Context.TELEPHONY_SERVICE);
        int currPhoneCallState = telephonyManager.getCallState();

        result = (currPhoneCallState == TelephonyManager.CALL_STATE_IDLE);

        Log.d(TAG, "isCallStateIDLE: " + result);

        return result;
    }

    private boolean isLteCardReady() {
        boolean result = false;
        boolean airPlaneMode = TelephonyUtilsEx.isAirPlaneMode();
        boolean callStateIdle = isCallStateIDLE();
        boolean isCdma4GCard = TelephonyUtilsEx.isCdma4gCard(mPhone.getSubId());

        result = isCdma4GCard && !airPlaneMode && callStateIdle;
        Log.d(TAG, "isLteCardReady: " + result + ";isCdma4GCard:" + isCdma4GCard);
        return result;
    }

    private void updateSwitch() {
        int settingsNetworkMode = android.provider.Settings.Global.getInt(
                mPhone.getContext().getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE + mPhone.getSubId(),
                preferredNetworkMode);

        boolean enable = isLteCardReady() && TelephonyUtilsEx.isCapabilityPhone(mPhone);

        Log.d(TAG, " enable = " + enable + " ,settingsNetworkMode: " + settingsNetworkMode);

        mEnable4GDataPreference.setEnabled(enable);
        mEnable4GDataPreference.setChecked(enable
                && (settingsNetworkMode == Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA));

        /// TODO: Temple solution for 4G data only @{
        if (FeatureOption.isMtkTddDataOnlySupport()) {
            mEnableSigle4GDataPreference.setEnabled(enable);
            mEnableSigle4GDataPreference.setChecked(enable
                    && settingsNetworkMode == Phone.NT_MODE_LTE_TDD_ONLY);
        }
        /// @}
    }

    private void handleEnable4GDataClick(Preference preference) {
        SwitchPreference switchPre = (SwitchPreference) preference;
        boolean isChecked = switchPre.isChecked();
        int modemNetworkMode = isChecked ?
                Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA : Phone.NT_MODE_GLOBAL;

        Log.d(TAG, "handleEnable4GDataClick isChecked = " + isChecked);

        Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE
                + mPhone.getSubId(), modemNetworkMode);

        mPhone.setPreferredNetworkType(modemNetworkMode,
                mEnable4GHandler.obtainMessage(
                        mEnable4GHandler.MESSAGE_SET_ENABLE_4G_NETWORK_TYPE));
    }

    private void handleEnable4GDataOnlyClick(Preference preference) {
        SwitchPreference swp = (SwitchPreference) preference;
        boolean isChecked = swp.isChecked();
        int modemNetworkMode = isChecked ?
                Phone.NT_MODE_LTE_TDD_ONLY : Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA;

        Log.d(TAG, "handleEnable4GDataOnlyClick isChecked = " + isChecked);

        Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE
                + mPhone.getSubId(), modemNetworkMode);

        mPhone.setPreferredNetworkType(modemNetworkMode,
                mEnable4GHandler.obtainMessage(
                        mEnable4GHandler.MESSAGE_SET_ENABLE_SIGLE_4G_DATA_NETWORK_TYPE));
    }

    /**
     * judge if sim state is ready.
     * sim state:SIM_STATE_UNKNOWN = 0;SIM_STATE_ABSENT = 1
     * SIM_STATE_PIN_REQUIRED = 2;SIM_STATE_PUK_REQUIRED = 3;
     * SIM_STATE_NETWORK_LOCKED = 4;SIM_STATE_READY = 5;
     * SIM_STATE_CARD_IO_ERROR = 6;
     * @param context Context
     * @param simId sim id
     * @return true if is SIM_STATE_READY
     */
    static boolean isSimStateReady(Context context, int simId) {
        int simState = TelephonyManager.from(context).getSimState(simId);

        Log.d(TAG, "isSimStateReady simState=" + simState);
        return simState == TelephonyManager.SIM_STATE_READY;
    }

    private void updateEnable4GNetworkUIFromDb() {

        int settingsNetworkMode = android.provider.Settings.Global.getInt(
                mPhone.getContext().getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE + mPhone.getSubId(),
                preferredNetworkMode);

        Log.d(TAG, "updateEnable4GNetworkUIFromDb: settingsNetworkMode = " + settingsNetworkMode);

        boolean isChecked = (settingsNetworkMode == Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA)
                ? true : false;
        mEnable4GDataPreference.setChecked(isChecked);
    }

    private void updateEnableSingle4GDataNetworkUIFromDb() {

        int settingsNetworkMode = android.provider.Settings.Global.getInt(
                mPhone.getContext().getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE + mPhone.getSubId(),
                preferredNetworkMode);

        Log.d(TAG, "updateEnableSingle4GDataNetworkUIFromDb: settingsNetworkMode = "
        + settingsNetworkMode);

        boolean isChecked = settingsNetworkMode == Phone.NT_MODE_LTE_TDD_ONLY ? true : false;
        mEnable4GDataPreference.setChecked(isChecked);
    }

    private class Enable4GHandler extends Handler {

        static final int MESSAGE_SET_ENABLE_4G_NETWORK_TYPE = 0;
        static final int MESSAGE_SET_ENABLE_SIGLE_4G_DATA_NETWORK_TYPE = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_SET_ENABLE_4G_NETWORK_TYPE:
                handleSetEnable4GNetworkTypeResponse(msg);
                break;
            case MESSAGE_SET_ENABLE_SIGLE_4G_DATA_NETWORK_TYPE:
                handleSetEnableSigle4GDataNetworkTypeResponse(msg);
                break;
            }
        }

        private void handleSetEnable4GNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception == null) {
                if (mEnable4GDataPreference != null) {
                    boolean isChecked = mEnable4GDataPreference.isChecked();
                    Log.d(TAG, "isChecked = " + isChecked );
                }
            } else {
                Log.d(TAG, "handleSetEnable4GNetworkTypeResponse: exception.");
                updateEnable4GNetworkUIFromDb();
            }
        }

        private void handleSetEnableSigle4GDataNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception == null) {

                if (mEnableSigle4GDataPreference != null) {
                    boolean isChecked = mEnableSigle4GDataPreference.isChecked();
                    int modemNetworkMode = isChecked ? Phone.NT_MODE_LTE_TDD_ONLY
                            : Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA;

                    Log.d(TAG, "isChecked = " + isChecked);

                    Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                            android.provider.Settings.Global.PREFERRED_NETWORK_MODE
                            + mPhone.getSubId(), modemNetworkMode);
                }
            } else {
                Log.d(TAG, "handleSetEnableSigle4GDataNetworkTypeResponse: exception.");

                updateEnableSingle4GDataNetworkUIFromDb();
            }
        }
    }
}
