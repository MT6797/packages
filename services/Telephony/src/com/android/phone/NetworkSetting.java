/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2006 The Android Open Source Project
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

import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;

import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.android.ims.ImsManager;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.mediatek.phone.PhoneFeatureConstants;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.settings.cdma.TelephonyUtilsEx;

/**
 * "Networks" settings UI for the Phone app.
 */
public class NetworkSetting extends PreferenceActivity
         implements DialogInterface.OnCancelListener, PhoneGlobals.SubInfoUpdateListener,
         DialogInterface.OnDismissListener, DialogInterface.OnShowListener {

    private static final String LOG_TAG = "phone";
    private static final boolean DBG = true;

    private static final int EVENT_NETWORK_SCAN_COMPLETED = 100;
    private static final int EVENT_NETWORK_SELECTION_DONE = 200;
    private static final int EVENT_AUTO_SELECT_DONE = 300;

    //dialog ids
    private static final int DIALOG_NETWORK_SELECTION = 100;
    private static final int DIALOG_NETWORK_LIST_LOAD = 200;
    private static final int DIALOG_NETWORK_AUTO_SELECT = 300;

    /// M: add for all network is forbidden
    private static final int DIALOG_ALL_FORBIDDEN = 400;
    //String keys for preference lookup
    private static final String LIST_NETWORKS_KEY = "list_networks_key";
    private static final String BUTTON_SRCH_NETWRKS_KEY = "button_srch_netwrks_key";
    private static final String BUTTON_AUTO_SELECT_KEY = "button_auto_select_key";

    //map of network controls to the network data.
    private HashMap<Preference, OperatorInfo> mNetworkMap;

    private int mSubId = INVALID_SUBSCRIPTION_ID;

    int mPhoneId = SubscriptionManager.INVALID_PHONE_INDEX;
    protected boolean mIsForeground = false;

    private UserManager mUm;
    private boolean mUnavailable;

    /** message for network selection */
    String mNetworkSelectMsg;

    //preference objects
    private PreferenceGroup mNetworkList;
    private Preference mSearchButton;
    private Preference mAutoSelect;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
                case EVENT_NETWORK_SCAN_COMPLETED:
                    networksListLoaded ((List<OperatorInfo>) msg.obj, msg.arg1);
                    break;

                case EVENT_NETWORK_SELECTION_DONE:
                    if (DBG) log("hideProgressPanel");
                    removeDialog(DIALOG_NETWORK_SELECTION);
                    getPreferenceScreen().setEnabled(true);

                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        if (DBG) log("manual network selection: failed!");
                        displayNetworkSelectionFailed(ar.exception);
                    } else {
                        if (DBG) log("manual network selection: succeeded!");
                        displayNetworkSelectionSucceeded();
                    }

                    break;
                case EVENT_AUTO_SELECT_DONE:
                    if (DBG) log("hideProgressPanel");

                    // Always try to dismiss the dialog because activity may
                    // be moved to background after dialog is shown.
                    try {
                        dismissDialog(DIALOG_NETWORK_AUTO_SELECT);
                    } catch (IllegalArgumentException e) {
                        // "auto select" is always trigged in foreground, so "auto select" dialog
                        //  should be shown when "auto select" is trigged. Should NOT get
                        // this exception, and Log it.
                        Log.w(LOG_TAG, "[NetworksList] Fail to dismiss auto select dialog", e);
                    }
                    getPreferenceScreen().setEnabled(true);

                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        if (DBG) log("automatic network selection: failed!");
                        displayNetworkSelectionFailed(ar.exception);
                    } else {
                        if (DBG) log("automatic network selection: succeeded!");
                        displayNetworkSelectionSucceeded();
                    }

                    break;
            }

            return;
        }
    };

    /**
     * Service connection code for the NetworkQueryService.
     * Handles the work of binding to a local object so that we can make
     * the appropriate service calls.
     */

    /** Local service interface */
    private INetworkQueryService mNetworkQueryService = null;

    /** Service connection */
    private final ServiceConnection mNetworkQueryServiceConnection = new ServiceConnection() {

        /** Handle the task of binding the local object to the service */
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (DBG) log("connection created, binding local service.");
            /// M: modify for plug-in, class of <service> may be difference
            /// if publishBinderDirectly return different value @{
            if (ExtensionManager.getPhoneMiscExt().publishBinderDirectly()) {
                mNetworkQueryService = INetworkQueryService.Stub.asInterface(service);
            } else {
                mNetworkQueryService = ((NetworkQueryService.LocalBinder) service).getService();
            }
            /// @}
            // as soon as it is bound, run a query.
            loadNetworksList();
        }

        /** Handle the task of cleaning up the local binding */
        public void onServiceDisconnected(ComponentName className) {
            if (DBG) log("connection disconnected, cleaning local binding.");
            mNetworkQueryService = null;
        }
    };

    /**
     * This implementation of INetworkQueryServiceCallback is used to receive
     * callback notifications from the network query service.
     */
    private final INetworkQueryServiceCallback mCallback = new INetworkQueryServiceCallback.Stub() {

        /** place the message on the looper queue upon query completion. */
        public void onQueryComplete(List<OperatorInfo> networkInfoArray, int status) {
            if (DBG) log("notifying message loop of query completion.");
            Message msg = mHandler.obtainMessage(EVENT_NETWORK_SCAN_COMPLETED,
                    status, 0, networkInfoArray);
            msg.sendToTarget();
        }
    };

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean handled = false;

        /// M: Add for ALPS02416707 When CDMA in roaming to out of roaming,
        /// we should don't give the user choices to search network. @{
        if (TelephonyUtilsEx.isCDMAPhone(PhoneFactory.getPhone(mPhoneId))) {
            log("onPreferenceTreeClick, CDMA phone Finished...");
            finish();
            return true;
        }
        /// @}

        /// M: for plug-in @{
        handled = ExtensionManager.getNetworkSettingExt()
                .onPreferenceTreeClick(preferenceScreen, preference);
        if (handled) {
            return true;
        }
        /// @}

        if (preference == mSearchButton) {
            loadNetworksList();
            handled = true;
        } else if (preference == mAutoSelect) {
            selectNetworkAutomatic();
            handled = true;
            /// M: For CSG feature @{
        } else if (preference == mManuSelectFemtocell) {
            selectFemtocellManually();
            handled = true;
            /// @}
        } else {
            Preference selectedCarrier = preference;

            String networkStr = selectedCarrier.getTitle().toString();
            if (DBG) log("selected network: " + networkStr);

            Message msg = mHandler.obtainMessage(EVENT_NETWORK_SELECTION_DONE);
            Phone phone = PhoneFactory.getPhone(mPhoneId);

            if (mNetworkMap != null) {
                if (!ExtensionManager.getNetworkSettingExt()
                        .onPreferenceTreeClick(mNetworkMap.get(selectedCarrier), mSubId)) {
                    if (phone != null) {
                        phone.selectNetworkManually(mNetworkMap.get(selectedCarrier), msg);
                        displayNetworkSeletionInProgress(networkStr);
                        handled = true;
                    } else {
                        log("Error selecting network. phone is null.");
                    }
                } else {
                    handled = true;
                }
            } else {
                log("[onPreferenceTreeClick] select on PLMN, but mNetworkMap == null !!!");
            }
        }

        return handled;
    }
    /// M : <check if PS call ongoing and Wfc registered> @{
    private boolean isWfcCallOngoing() {
        boolean result = false;
        if (ImsManager.isWfcEnabledByPlatform(this)) {
            Phone phone = PhoneFactory.getPhone(mPhoneId);
            if (phone != null) {
                ImsPhone imsPhone = (ImsPhone) phone.getImsPhone();
                log("isWfcCallOngoing imsPhone" + imsPhone);
                boolean isWfcEnabled = ((TelephonyManager) phone.getContext()
                        .getSystemService(Context.TELEPHONY_SERVICE)).isWifiCallingEnabled();
                if (isWfcEnabled && ((imsPhone != null)
                        && (!(imsPhone.getBackgroundCall().isIdle())
                        || !(imsPhone.getForegroundCall().isIdle())
                        || !(imsPhone.getRingingCall().isIdle())))) {
                        result = true;
                }
            } else {
                log("Error phone is null.");
            }
        }
        log("isWfcCallOngoing" + result);
        return result;
    }
    /// @}

    //implemented for DialogInterface.OnCancelListener
    public void onCancel(DialogInterface dialog) {
        // request that the service stop the query with this callback object.
        try {
            mNetworkQueryService.stopNetworkQuery(mCallback);
        } catch (RemoteException e) {
            log("onCancel: exception from stopNetworkQuery " + e);
        }
        finish();
    }

    public String getNormalizedCarrierName(OperatorInfo ni) {
        if (ni != null) {
            return ni.getOperatorAlphaLong() + " (" + ni.getOperatorNumeric() + ")";
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mUm = (UserManager) getSystemService(Context.USER_SERVICE);

        if (mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)) {
            setContentView(R.layout.telephony_disallowed_preference_screen);
            mUnavailable = true;
            return;
        }

        addPreferencesFromResource(R.xml.carrier_select);

        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
            mSubId = mSubscriptionInfoHelper.getSubId();
            if (SubscriptionManager.isValidSubscriptionId(mSubId)) {
                mPhoneId = SubscriptionManager.getPhoneId(mSubId);
            } else {
                log("mSubId is invalid,activity finish!!!");
                finish();
                return;
            }
        }

        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
        registerPhoneState();

        mNetworkList = (PreferenceGroup) getPreferenceScreen().findPreference(LIST_NETWORKS_KEY);
        mNetworkMap = new HashMap<Preference, OperatorInfo>();

        mSearchButton = getPreferenceScreen().findPreference(BUTTON_SRCH_NETWRKS_KEY);
        mAutoSelect = getPreferenceScreen().findPreference(BUTTON_AUTO_SELECT_KEY);

        // Start the Network Query service, and bind it.
        // The OS knows to start he service only once and keep the instance around (so
        // long as startService is called) until a stopservice request is made.  Since
        // we want this service to just stay in the background until it is killed, we
        // don't bother stopping it from our end.
        intent = new Intent(this, NetworkQueryService.class);
        intent.putExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, mSubId);
        startService(intent);
        bindService(new Intent(this, NetworkQueryService.class),
                mNetworkQueryServiceConnection, Context.BIND_AUTO_CREATE);
        /// M: Add for CSG @{
        newManuSelectFemetocellPreference(getPreferenceScreen());
        /// @}

        /// M: for plug-in @{
        ExtensionManager.getNetworkSettingExt().initOtherNetworkSetting(getPreferenceScreen());
        /// @}
    }

    @Override
    public void onResume() {
        super.onResume();
        mIsForeground = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        mIsForeground = false;

        cancelNetworkSearch();
    }

    /**
     * Override onDestroy() to unbind the query service, avoiding service
     * leak exceptions.
     */
    @Override
    protected void onDestroy() {
        if (!mUnavailable && mNetworkQueryService != null) {
            try {
                // used to un-register callback
                mNetworkQueryService.unregisterCallback(mCallback);
            } catch (RemoteException e) {
                log("onDestroy: exception from unregisterCallback " + e);
            }
            unbindService(mNetworkQueryServiceConnection);
        }
        PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);
        unRegisterPhoneState();
        super.onDestroy();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        /// M: when all the network forbidden show dialog remind user @{
        if (id == DIALOG_ALL_FORBIDDEN) {
            Builder builder = new AlertDialog.Builder(this);
            AlertDialog alertDlg;
            builder.setTitle(android.R.string.dialog_alert_title);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage(R.string.network_setting_all_forbidden_dialog);
            builder.setPositiveButton(android.R.string.yes, null);
            alertDlg = builder.create();
            return alertDlg;
        }
        /// @}

        if ((id == DIALOG_NETWORK_SELECTION) || (id == DIALOG_NETWORK_LIST_LOAD) ||
                (id == DIALOG_NETWORK_AUTO_SELECT)) {
            ProgressDialog dialog = new ProgressDialog(this);
            switch (id) {
                case DIALOG_NETWORK_SELECTION:
                    // It would be more efficient to reuse this dialog by moving
                    // this setMessage() into onPreparedDialog() and NOT use
                    // removeDialog().  However, this is not possible since the
                    // message is rendered only 2 times in the ProgressDialog -
                    // after show() and before onCreate.
                    dialog.setMessage(mNetworkSelectMsg);
                    dialog.setCancelable(false);
                    dialog.setIndeterminate(true);
                    break;
                case DIALOG_NETWORK_AUTO_SELECT:
                    dialog.setMessage(getResources().getString(R.string.register_automatically));
                    dialog.setCancelable(false);
                    dialog.setIndeterminate(true);
                    break;
                case DIALOG_NETWORK_LIST_LOAD:
                    // M: ALPS01261105 Set show & dismiss listener @{
                    dialog.setOnDismissListener(this);
                    dialog.setOnShowListener(this);
                    // @}
                default:
                    // reinstate the cancelablity of the dialog.
                    dialog.setMessage(getResources().getString(R.string.load_networks_progress));
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.setOnCancelListener(this);
                    break;
            }
            return dialog;
        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        if ((id == DIALOG_NETWORK_SELECTION) || (id == DIALOG_NETWORK_LIST_LOAD) ||
                (id == DIALOG_NETWORK_AUTO_SELECT)) {
            // when the dialogs come up, we'll need to indicate that
            // we're in a busy state to dissallow further input.
            getPreferenceScreen().setEnabled(false);
        }
    }

    private void displayEmptyNetworkList(boolean flag) {
        mNetworkList.setTitle(flag ? R.string.empty_networks_list : R.string.label_available);
    }

    private void displayNetworkSeletionInProgress(String networkStr) {
        // TODO: use notification manager?
        mNetworkSelectMsg = getResources().getString(R.string.register_on_network, networkStr);

        if (mIsForeground) {
            showDialog(DIALOG_NETWORK_SELECTION);
        }
    }

    private void displayNetworkQueryFailed(int error) {
        String status = getResources().getString(R.string.network_query_error);

        final PhoneGlobals app = PhoneGlobals.getInstance();
        app.notificationMgr.postTransientNotification(
                NotificationMgr.NETWORK_SELECTION_NOTIFICATION, status);
    }

    private void displayNetworkSelectionFailed(Throwable ex) {
        String status;

        if ((ex != null && ex instanceof CommandException) &&
                ((CommandException)ex).getCommandError()
                  == CommandException.Error.ILLEGAL_SIM_OR_ME)
        {
            status = getResources().getString(R.string.not_allowed);
        } else {
            status = getResources().getString(R.string.connect_later);
        }

        final PhoneGlobals app = PhoneGlobals.getInstance();
        app.notificationMgr.postTransientNotification(
                NotificationMgr.NETWORK_SELECTION_NOTIFICATION, status);
    }

    private void displayNetworkSelectionSucceeded() {
        String status = getResources().getString(R.string.registration_done);

        final PhoneGlobals app = PhoneGlobals.getInstance();
        app.notificationMgr.postTransientNotification(
                NotificationMgr.NETWORK_SELECTION_NOTIFICATION, status);

        mHandler.postDelayed(new Runnable() {
            public void run() {
                finish();
            }
        }, 3000);
    }

    private void loadNetworksList() {
        if (DBG) log("load networks list...");

        if (mIsForeground) {
            showDialog(DIALOG_NETWORK_LIST_LOAD);
        }

        // delegate query request to the service.
        try {
            mNetworkQueryService.startNetworkQuery(mCallback, mPhoneId);
        } catch (RemoteException e) {
            log("loadNetworksList: exception from startNetworkQuery " + e);
            if (mIsForeground) {
                try {
                    dismissDialog(DIALOG_NETWORK_LIST_LOAD);
                } catch (IllegalArgumentException e1) {
                    // do nothing
                }
            }
        }

        displayEmptyNetworkList(false);
    }

    /**
     * networksListLoaded has been rewritten to take an array of
     * OperatorInfo objects and a status field, instead of an
     * AsyncResult.  Otherwise, the functionality which takes the
     * OperatorInfo array and creates a list of preferences from it,
     * remains unchanged.
     */
    private void networksListLoaded(List<OperatorInfo> result, int status) {
        if (DBG) log("networks list loaded");

        // used to un-register callback
        try {
            mNetworkQueryService.unregisterCallback(mCallback);
        } catch (RemoteException e) {
            log("networksListLoaded: exception from unregisterCallback " + e);
        }

        // update the state of the preferences.
        if (DBG) log("hideProgressPanel");

        /// M: Add for plug-in @{
        result = ExtensionManager.getNetworkSettingExt().customizeNetworkList(result, mSubId);
        /// @}

        // Always try to dismiss the dialog because activity may
        // be moved to background after dialog is shown.
        try {
            dismissDialog(DIALOG_NETWORK_LIST_LOAD);
        } catch (IllegalArgumentException e) {
            // It's not a error in following scenario, we just ignore it.
            // "Load list" dialog will not show, if NetworkQueryService is
            // connected after this activity is moved to background.
            if (DBG) log("Fail to dismiss network load list dialog " + e);
        }

        getPreferenceScreen().setEnabled(true);
        clearList();

        if (status != NetworkQueryService.QUERY_OK) {
            if (DBG) log("error while querying available networks");
            displayNetworkQueryFailed(status);
            displayEmptyNetworkList(true);
        } else {
            if (result != null){
                displayEmptyNetworkList(false);

                // create a preference for each item in the list.
                // just use the operator name instead of the mildly
                // confusing mcc/mnc.
                /// M: add forbidden at the end of operator name
                int forbiddenCount = 0;
                for (OperatorInfo ni : result) {
                    String forbidden = "";
                    if (ni.getState() == OperatorInfo.State.FORBIDDEN) {
                        forbidden = "(" + getResources().getString(
                                R.string.network_forbidden) + ")";
                        forbiddenCount++;
                    }
                    Preference carrier = new Preference(this, null);
                    carrier.setTitle(getNetworkTitle(ni) + forbidden);
                    carrier.setPersistent(false);
                    mNetworkList.addPreference(carrier);
                    mNetworkMap.put(carrier, ni);

                    if (DBG) log("  " + ni);
                }
                if (mIsForeground && forbiddenCount == result.size()) {
                    if (DBG) {
                        log("show DIALOG_ALL_FORBIDDEN forbiddenCount:" + forbiddenCount);
                    }
                    showDialog(DIALOG_ALL_FORBIDDEN);
                }
            } else {
                displayEmptyNetworkList(true);
            }
        }
    }

    /**
     * Returns the title of the network obtained in the manual search.
     *
     * @param OperatorInfo contains the information of the network.
     *
     * @return Long Name if not null/empty, otherwise Short Name if not null/empty,
     * else MCCMNC string.
     */

    private String getNetworkTitle(OperatorInfo ni) {
        if (!TextUtils.isEmpty(ni.getOperatorAlphaLong())) {
            return ni.getOperatorAlphaLong();
        } else if (!TextUtils.isEmpty(ni.getOperatorAlphaShort())) {
            return ni.getOperatorAlphaShort();
        } else {
            BidiFormatter bidiFormatter = BidiFormatter.getInstance();
            return bidiFormatter.unicodeWrap(ni.getOperatorNumeric(), TextDirectionHeuristics.LTR);
        }
    }

    private void clearList() {
        for (Preference p : mNetworkMap.keySet()) {
            mNetworkList.removePreference(p);
        }
        mNetworkMap.clear();
    }

    private void selectNetworkAutomatic() {
        if (DBG) log("select network automatically...");
        if (mIsForeground) {
            showDialog(DIALOG_NETWORK_AUTO_SELECT);
        }

        Message msg = mHandler.obtainMessage(EVENT_AUTO_SELECT_DONE);
        Phone phone = PhoneFactory.getPhone(mPhoneId);
        if (phone != null) {
            phone.setNetworkSelectionModeAutomatic(msg);
        }
    }

    private void log(String msg) {
        Log.d(LOG_TAG, "[NetworksList] " + msg);
    }

    // -------   ----------------------------MTK----------------------------------------------
    /// M: Add for CSG @{
    private Preference mManuSelectFemtocell;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;
    private PhoneStateListener mPhoneStateListener;
    private TelephonyManager mTelephonyManager;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SubscriptionInfoHelper.SUB_ID_EXTRA, mSubId);
    }

    @Override
    public void handleSubInfoUpdate() {
        log("handleSubInfoUpdate...");
        finish();
    }

    private void cancelNetworkSearch() {
        log("cancel network search");
        if (!mUnavailable && mNetworkQueryService != null) {
            // unbind the service.
            try {
                if (!isWfcCallOngoing()) {
                    mNetworkQueryService.stopNetworkQuery(mCallback);
                    dismissDialog(DIALOG_NETWORK_LIST_LOAD);
                }
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            } catch (IllegalArgumentException e) {
                if (DBG) log("Fail to dismiss network load list dialog");
            }
            getPreferenceScreen().setEnabled(true);
        }
    }

    private void newManuSelectFemetocellPreference(PreferenceScreen root) {
        if (PhoneFeatureConstants.FeatureOption.isMtkFemtoCellSupport() &&
                !isNetworkModeSetGsmOnly()) {
            mManuSelectFemtocell = new Preference(getApplicationContext());
            mManuSelectFemtocell.setTitle(R.string.sum_search_femtocell_networks);
            root.addPreference(mManuSelectFemtocell);
        }
    }

    /**
     * Get the network mode is GSM Only or not.
     * @return if ture is GSM only else not
     */
    private boolean isNetworkModeSetGsmOnly() {
        return Phone.NT_MODE_GSM_ONLY == android.provider.Settings.Global.getInt(
                getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                Phone.PREFERRED_NT_MODE);
    }

    private void selectFemtocellManually() {
        log("selectFemtocellManually()");
        Intent intent = new Intent();
        intent.setClassName("com.android.phone", "com.mediatek.settings.FemtoPointList");
        intent.putExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, mSubId);
        startActivity(intent);
    }
    /// Add for CSG @}

    private void registerPhoneState() {
        mPhoneStateListener = new PhoneStateListener(mSubId) {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);

                Log.d(LOG_TAG, "onCallStateChanged ans state is " + state);

                boolean isIdle = (state == TelephonyManager.CALL_STATE_IDLE);
                if (!isIdle) {
                    cancelNetworkSearch();
                    finish();
                }
            }
        };

        if (mTelephonyManager != null) {
            mTelephonyManager.listen(
                    mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    private void unRegisterPhoneState() {
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(
                    mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
    }

    @Override
    public void onShow(DialogInterface dialog) {
        /// ALPS01261105. Keep activity screen on to prevent the screen from timing out.
        // when screen time out, the system call onPause() and make the query cancel.
        Window window = getWindow();
        if (window == null) {
            Log.i(LOG_TAG, "[onShow]window is null, skip adding flags");
            return;
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        /// ALPS01261105, clear flags in order to execute system time out.
        Window window = getWindow();
        if (window == null) {
            Log.i(LOG_TAG, "[onDismiss]window is null, skip clearing flags");
            return;
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        /// @}
    }
}
