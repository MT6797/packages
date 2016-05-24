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

package com.android.settings;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.cdma.CdmaApnSetting;
import com.mediatek.settings.cdma.CdmaUtils;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.ext.IApnSettingsExt;
import com.mediatek.settings.ext.IRcseOnlyApnExt;
import com.mediatek.settings.ext.IRcseOnlyApnExt.OnRcseOnlyApnStateChangedListener;
import com.mediatek.settings.sim.MsimRadioValueObserver;
import com.mediatek.settings.sim.SimHotSwapHandler;
import com.mediatek.settings.sim.TelephonyUtils;
import com.mediatek.settings.sim.SimHotSwapHandler.OnSimHotSwapListener;

import android.telephony.TelephonyManager;

import java.util.ArrayList;

public class ApnSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    static final String TAG = "ApnSettings";

    public static final String EXTRA_POSITION = "position";
    public static final String RESTORE_CARRIERS_URI =
        "content://telephony/carriers/restore";
    public static final String PREFERRED_APN_URI =
        "content://telephony/carriers/preferapn";

    public static final String APN_ID = "apn_id";
    public static final String SUB_ID = "sub_id";
    public static final String MVNO_TYPE = "mvno_type";
    public static final String MVNO_MATCH_DATA = "mvno_match_data";

    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;
    private static final int TYPES_INDEX = 3;
    private static final int MVNO_TYPE_INDEX = 4;
    private static final int MVNO_MATCH_DATA_INDEX = 5;
    /// M: check source type, some types are not editable
    private static final int SOURCE_TYPE_INDEX = 6;

    private static final int MENU_NEW = Menu.FIRST;
    private static final int MENU_RESTORE = Menu.FIRST + 1;

    private static final int EVENT_RESTORE_DEFAULTAPN_START = 1;
    private static final int EVENT_RESTORE_DEFAULTAPN_COMPLETE = 2;

    private static final int DIALOG_RESTORE_DEFAULTAPN = 1001;

    private static final Uri DEFAULTAPN_URI = Uri.parse(RESTORE_CARRIERS_URI);
    private static final Uri PREFERAPN_URI = Uri.parse(PREFERRED_APN_URI);

    private static boolean mRestoreDefaultApnMode;

    private RestoreApnUiHandler mRestoreApnUiHandler;
    private RestoreApnProcessHandler mRestoreApnProcessHandler;
    private HandlerThread mRestoreDefaultApnThread;
    private SubscriptionInfo mSubscriptionInfo;
    private UiccController mUiccController;
    private String mMvnoType;
    private String mMvnoMatchData;

    private UserManager mUm;

    private String mSelectedKey;

    private IntentFilter mMobileStateFilter;

    private boolean mUnavailable;

    private final BroadcastReceiver mMobileStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
                PhoneConstants.DataState state = getMobileDataState(intent);
                Log.d(TAG, "onReceive ACTION_ANY_DATA_CONNECTION_STATE_CHANGED,state = " + state);
                switch (state) {
                case CONNECTED:
                    if (!mRestoreDefaultApnMode) {
                        fillList();
                    } else {
                        /// M: for ALPS02326359, should not show dialog here
                        // showDialog(DIALOG_RESTORE_DEFAULTAPN);
                    }
                    break;
                }
                /// M: disable screen when MMS in transaction
                updateScreenForDataStateChange(context, intent);
            } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(intent.getAction())) {
                /// M: Update the screen enable status, if airplane mode change
                updateScreenEnableState(context);
            }
        }
    };

    private static PhoneConstants.DataState getMobileDataState(Intent intent) {
        String str = intent.getStringExtra(PhoneConstants.STATE_KEY);
        if (str != null) {
            return Enum.valueOf(PhoneConstants.DataState.class, str);
        } else {
            return PhoneConstants.DataState.DISCONNECTED;
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APN;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Activity activity = getActivity();
        final int subId = activity.getIntent().getIntExtra(SUB_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        mUm = (UserManager) getSystemService(Context.USER_SERVICE);

        mMobileStateFilter = new IntentFilter(
                TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        /// M: for Airplane mode check
        mMobileStateFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);

        if (!mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)) {
            setHasOptionsMenu(true);
        }

        mSubscriptionInfo = SubscriptionManager.from(activity).getActiveSubscriptionInfo(subId);
        mUiccController = UiccController.getInstance();

        /// M: @{
        if (mSubscriptionInfo == null) {
            Log.d(TAG, "onCreate()... Invalid subId: " + subId);
            getActivity().finish();
        }
        mRadioValueObserver = new MsimRadioValueObserver(getActivity());
        /// @}

        /// M: for [SIM Hot Swap] @{
        mSimHotSwapHandler = new SimHotSwapHandler(getActivity().getApplicationContext());
        mSimHotSwapHandler.registerOnSimHotSwap(new OnSimHotSwapListener() {
            @Override
            public void onSimHotSwap() {
                Log.d(TAG, "onSimHotSwap, finish activity");
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });
        /// @}

        /// M: for plug-in @{
        mApnExt = UtilsExt.getApnSettingsPlugin(activity);
        mApnExt.initTetherField(this);

        mRcseApnExt = UtilsExt.getRcseApnPlugin(activity);
        mRcseApnExt.onCreate(new OnRcseOnlyApnStateChangedListener() {
            @Override
            public void OnRcseOnlyApnStateChanged() {
                Log.d(TAG, "OnRcseOnlyApnStateChanged()");
                fillList();
            }
        }, subId);
        /// @}
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        TextView empty = (TextView) getView().findViewById(android.R.id.empty);
        if (empty != null) {
            empty.setText(R.string.apn_settings_not_available);
            getListView().setEmptyView(empty);
        }

        if (mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)
                || UserHandle.myUserId()!= UserHandle.USER_OWNER) {
            mUnavailable = true;
            setPreferenceScreen(new PreferenceScreen(getActivity(), null));
            return;
        }

        addPreferencesFromResource(R.xml.apn_settings);

        getListView().setItemsCanFocus(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mUnavailable) {
            return;
        }

        /// M: check Msim mode @{
        mRadioValueObserver.registerMsimObserver(new MsimRadioValueObserver.Listener() {
            @Override
            public void onChange(int msimModevalue, boolean selfChange) {
                if (getActivity() != null) {
                    updateScreenEnableState(getActivity());
                    getActivity().invalidateOptionsMenu();
                }
            }
        });
        /// @}

        getActivity().registerReceiver(mMobileStateReceiver, mMobileStateFilter);

        if (!mRestoreDefaultApnMode) {
            fillList();
            /// M: In case dialog not dismiss as activity is in background, so when resume back,
            // need to remove the dialog @{
            removeDialog(DIALOG_RESTORE_DEFAULTAPN);
            /// @}
        }

        /// M: for plug-in
        mApnExt.updateTetherState();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mUnavailable) {
            return;
        }

        getActivity().unregisterReceiver(mMobileStateReceiver);

        /// M: check msim mode
        mRadioValueObserver.ungisterMsimObserver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mRestoreDefaultApnThread != null) {
            mRestoreDefaultApnThread.quit();
        }

        /// M: for [SIM Hot Swap]
        mSimHotSwapHandler.unregisterOnSimHotSwap();

        /// M: for plug-in @{
        mApnExt.onDestroy();
        mRcseApnExt.onDestory();
        /// @}
    }

    private void fillList() {
        final TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        final String mccmnc = mSubscriptionInfo == null ? ""
            : tm.getSimOperator(mSubscriptionInfo.getSubscriptionId());
        Log.d(TAG, "mccmnc = " + mccmnc);
        String where = "numeric=\""
            + mccmnc
            + "\" AND NOT (type='ia' AND (apn=\"\" OR apn IS NULL))";

        /// M: for [C2K APN Customization] @{
        if (mSubscriptionInfo != null) {
            int subId = mSubscriptionInfo.getSubscriptionId();
            if (CdmaUtils.isSupportCdma(subId)) {
                where = CdmaApnSetting.customizeQuerySelectionforCdma(where, mccmnc, subId);
            }
        }
        /// @}

        /// M: for plug-in
        where = mApnExt.getFillListQuery(where, mccmnc);

        /// M: for VoLTE, do not show ims apn for non-VoLTE project @{
        if (!FeatureOption.MTK_VOLTE_SUPPORT) {
            where += " AND NOT type='ims'";
        }
        /// @}
        Log.d(TAG, "fillList where: " + where);

        /// M: for CU default APN set.
        /*
        Cursor cursor = getContentResolver().query(
                Telephony.Carriers.CONTENT_URI,
                new String[] { "_id", "name", "apn", "type", "mvno_type", "mvno_match_data",
                        "sourcetype" }, where, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
        */
        String order = mApnExt.getApnSortOrder(Telephony.Carriers.DEFAULT_SORT_ORDER);
        Log.d(TAG, "fillList sort: " + order);
        Cursor cursor = getContentResolver().query(
                Telephony.Carriers.CONTENT_URI,
                new String[] { "_id", "name", "apn", "type", "mvno_type", "mvno_match_data",
                        "sourcetype" }, where, null, order);
        /// @}

        if (cursor != null) {
            Log.d(TAG, "fillList, cursor count: " + cursor.getCount());
            IccRecords r = null;
            if (mUiccController != null && mSubscriptionInfo != null) {
                r = mUiccController.getIccRecords(SubscriptionManager.getPhoneId(
                        mSubscriptionInfo.getSubscriptionId()), UiccController.APP_FAM_3GPP);
            }
            PreferenceGroup apnList = (PreferenceGroup) findPreference("apn_list");
            apnList.removeAll();

            /// M: for plug-in, use Preference instead ApnPreference for the
            // convenience of plug-in side
            ArrayList<Preference> mnoApnList = new ArrayList<Preference>();
            ArrayList<Preference> mvnoApnList = new ArrayList<Preference>();
            ArrayList<Preference> mnoMmsApnList = new ArrayList<Preference>();
            ArrayList<Preference> mvnoMmsApnList = new ArrayList<Preference>();

            mSelectedKey = getSelectedApnKey();
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                String name = cursor.getString(NAME_INDEX);
                String apn = cursor.getString(APN_INDEX);
                String key = cursor.getString(ID_INDEX);
                String type = cursor.getString(TYPES_INDEX);
                String mvnoType = cursor.getString(MVNO_TYPE_INDEX);
                String mvnoMatchData = cursor.getString(MVNO_MATCH_DATA_INDEX);
                /// M: check source type, some types are not editable
                int sourcetype = cursor.getInt(SOURCE_TYPE_INDEX);

                /// M: skip specific APN type
                if(shouldSkipApn(type)) {
                    cursor.moveToNext();
                    continue;
                }

                /// M: for plug-in
                name = mApnExt.updateApnName(name, sourcetype);

                ApnPreference pref = new ApnPreference(getActivity());

                pref.setKey(key);
                pref.setTitle(name);
                pref.setSummary(apn);
                pref.setPersistent(false);
                pref.setOnPreferenceChangeListener(this);

                /// M: for [Read Only APN]
                pref.setApnEditable(mApnExt.isAllowEditPresetApn(type, apn, mccmnc, sourcetype));
                pref.setSubId(mSubscriptionInfo == null ? null : mSubscriptionInfo
                        .getSubscriptionId());

                boolean selectable = ((type == null) || (!type.equals("mms")
                        && !type.equals("ia") && !type.equals("ims")))
                        /// M: for plug-in
                        && mApnExt.isSelectable(type);
                pref.setSelectable(selectable);
                Log.d(TAG, "mSelectedKey = " + mSelectedKey + " key = " + key + " name = " + name +
                        " selectable=" + selectable);
                if (selectable) {
                    /// M: select prefer APN later, as the apn list are not solid now @{
                    /*
                    if ((mSelectedKey != null) && mSelectedKey.equals(key)) {
                        pref.setChecked();
                    }
                    */
                    /// @}
                    addApnToList(pref, mnoApnList, mvnoApnList, r, mvnoType, mvnoMatchData);
                } else {
                    addApnToList(pref, mnoMmsApnList, mvnoMmsApnList, r, mvnoType, mvnoMatchData);
                    /// M: for plug-in
                    mApnExt.customizeUnselectableApn(mnoMmsApnList, mvnoMmsApnList,
                            mSubscriptionInfo == null ? null : mSubscriptionInfo
                                    .getSubscriptionId());
                }
                cursor.moveToNext();
            }
            cursor.close();

            if (!mvnoApnList.isEmpty()) {
                mnoApnList = mvnoApnList;
                mnoMmsApnList = mvnoMmsApnList;

                // Also save the mvno info
            }

            for (Preference preference : mnoApnList) {
                apnList.addPreference(preference);
            }
            for (Preference preference : mnoMmsApnList) {
                apnList.addPreference(preference);
            }

            /// M: always set a prefer APN
            setPreferApnChecked(mnoApnList);

            /// M: update screen enable state according to airplane mode, SIM radio status, etc.
            updateScreenEnableState(getActivity());
        }
    }

    private void addApnToList(ApnPreference pref, ArrayList<Preference> mnoList,
                              ArrayList<Preference> mvnoList, IccRecords r, String mvnoType,
                              String mvnoMatchData) {
        if (r != null && !TextUtils.isEmpty(mvnoType) && !TextUtils.isEmpty(mvnoMatchData)) {
            if (ApnSetting.mvnoMatches(r, mvnoType, mvnoMatchData)) {
                mvnoList.add(pref);
                // Since adding to mvno list, save mvno info
                mMvnoType = mvnoType;
                mMvnoMatchData = mvnoMatchData;
            }
        } else {
            mnoList.add(pref);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!mUnavailable) {
            menu.add(0, MENU_NEW, 0,
                    getResources().getString(R.string.menu_new))
                    .setIcon(android.R.drawable.ic_menu_add)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            menu.add(0, MENU_RESTORE, 0,
                    getResources().getString(R.string.menu_restore))
                    .setIcon(android.R.drawable.ic_menu_upload);
        }

        /// M: for plug-in
        mApnExt.updateMenu(menu, MENU_NEW, MENU_RESTORE,
                TelephonyManager.getDefault().getSimOperator(
                mSubscriptionInfo != null ? mSubscriptionInfo.getSubscriptionId()
                        : SubscriptionManager.INVALID_SUBSCRIPTION_ID));
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_NEW:
            addNewApn();
            return true;

        case MENU_RESTORE:
            restoreDefaultApn();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addNewApn() {
        Intent intent = new Intent(Intent.ACTION_INSERT, Telephony.Carriers.CONTENT_URI);
        int subId = mSubscriptionInfo != null ? mSubscriptionInfo.getSubscriptionId()
                : SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        intent.putExtra(SUB_ID, subId);
        if (!TextUtils.isEmpty(mMvnoType) && !TextUtils.isEmpty(mMvnoMatchData)) {
            intent.putExtra(MVNO_TYPE, mMvnoType);
            intent.putExtra(MVNO_MATCH_DATA, mMvnoMatchData);
        }

        /// M: for plug-in
        mApnExt.addApnTypeExtra(intent);

        startActivity(intent);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        int pos = Integer.parseInt(preference.getKey());
        Uri url = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, pos);
        startActivity(new Intent(Intent.ACTION_EDIT, url));
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange(): Preference - " + preference
                + ", newValue - " + newValue + ", newValue type - "
                + newValue.getClass());
        if (newValue instanceof String) {
            setSelectedApnKey((String) newValue);
        }

        return true;
    }

    private void setSelectedApnKey(String key) {
        mSelectedKey = key;
        ContentResolver resolver = getContentResolver();

        ContentValues values = new ContentValues();
        values.put(APN_ID, mSelectedKey);

        /// M: add sub id for prefer APN
        // resolver.update(PREFERAPN_URI, values, null, null);
        resolver.update(getPreferApnUri(mSubscriptionInfo.getSubscriptionId()), values,
                null, null);
    }

    private String getSelectedApnKey() {
        String key = null;

        /// M: add sub id for prefer APN @{
        /*
        Cursor cursor = getContentResolver().query(PREFERAPN_URI, new String[] {"_id"},
                null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
                */
        int subId = mSubscriptionInfo.getSubscriptionId();
        Cursor cursor = getContentResolver().query(getPreferApnUri(subId), new String[] { "_id" },
                null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
        /// @}
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            key = cursor.getString(ID_INDEX);
        }
        cursor.close();
        Log.d(TAG,"getSelectedApnKey(), key = " + key);
        return key;
    }

    private boolean restoreDefaultApn() {
        showDialog(DIALOG_RESTORE_DEFAULTAPN);
        mRestoreDefaultApnMode = true;

        if (mRestoreApnUiHandler == null) {
            mRestoreApnUiHandler = new RestoreApnUiHandler();
        }

        if (mRestoreApnProcessHandler == null ||
            mRestoreDefaultApnThread == null) {
            mRestoreDefaultApnThread = new HandlerThread(
                    "Restore default APN Handler: Process Thread");
            mRestoreDefaultApnThread.start();
            mRestoreApnProcessHandler = new RestoreApnProcessHandler(
                    mRestoreDefaultApnThread.getLooper(), mRestoreApnUiHandler);
        }

        mRestoreApnProcessHandler
                .sendEmptyMessage(EVENT_RESTORE_DEFAULTAPN_START);
        return true;
    }

    private class RestoreApnUiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTAPN_COMPLETE:
                    Log.d(TAG, "restore APN complete~~");
                    Activity activity = getActivity();
                    if (activity == null) {
                        mRestoreDefaultApnMode = false;
                        return;
                    }
                    fillList();
                    updateScreenEnableState(activity);
                    mRestoreDefaultApnMode = false;
                    removeDialog(DIALOG_RESTORE_DEFAULTAPN);
                    Toast.makeText(
                        activity,
                        getResources().getString(
                                R.string.restore_default_apn_completed),
                        Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    private class RestoreApnProcessHandler extends Handler {
        private Handler mRestoreApnUiHandler;

        public RestoreApnProcessHandler(Looper looper, Handler restoreApnUiHandler) {
            super(looper);
            this.mRestoreApnUiHandler = restoreApnUiHandler;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTAPN_START:
                    Log.d(TAG, "restore APN start~~");
                    ContentResolver resolver = getContentResolver();
                    /// M: add sub id for APN
                    // resolver.delete(DEFAULTAPN_URI, null, null);
                    resolver.delete(
                            getDefaultApnUri(mSubscriptionInfo.getSubscriptionId()), null, null);
                    mRestoreApnUiHandler
                        .sendEmptyMessage(EVENT_RESTORE_DEFAULTAPN_COMPLETE);
                    break;
            }
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_RESTORE_DEFAULTAPN) {
            ProgressDialog dialog = new ProgressDialog(getActivity());
            dialog.setMessage(getResources().getString(R.string.restore_default_apn));
            dialog.setCancelable(false);
            return dialog;
        }
        return null;
    }

    ///---------------------------------------MTK-------------------------------------------------
    /// M: for [SIM Hot Swap]
    private SimHotSwapHandler mSimHotSwapHandler;

    private IApnSettingsExt mApnExt;
    private IRcseOnlyApnExt mRcseApnExt;

    private MsimRadioValueObserver mRadioValueObserver;

    private void updateScreenForDataStateChange(Context context, Intent intent) {
        String apnType  = intent.getStringExtra(PhoneConstants.DATA_APN_TYPE_KEY);
        Log.d(TAG, "Receiver,send MMS status, get type = " + apnType);
        if (PhoneConstants.APN_TYPE_MMS.equals(apnType)) {
            getPreferenceScreen().setEnabled(
                    !isMmsInTransaction(context)
                            /// M: for plug-in @{
                            && mApnExt.getScreenEnableState(mSubscriptionInfo
                                    .getSubscriptionId(), getActivity()));
                            /// @}
        }
    }

    private void updateScreenEnableState(Context context) {
        int subId = mSubscriptionInfo.getSubscriptionId();
        boolean simReady = TelephonyManager.SIM_STATE_READY == TelephonyManager.getDefault()
                .getSimState(SubscriptionManager.getSlotId(subId));
        boolean airplaneModeEnabled = android.provider.Settings.System.getInt(context
                .getContentResolver(), android.provider.Settings.System.AIRPLANE_MODE_ON, -1) == 1;

        boolean isMultiSimMode = Settings.System.getInt(context.getContentResolver(),
                Settings.System.MSIM_MODE_SETTING, -1) != 0;
        boolean enable = !airplaneModeEnabled && simReady && isMultiSimMode;
        Log.d(TAG, "updateScreenEnableState(), subId = " + subId + " ,airplaneModeEnabled = "
                + airplaneModeEnabled + " ,simReady = " + simReady + " , isMultiSimMode = "
                + isMultiSimMode);
        getPreferenceScreen().setEnabled(
                          /// M: for plug-in
                enable && mApnExt.getScreenEnableState(subId, getActivity()));
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    private boolean isMmsInTransaction(Context context) {
        boolean isMmsInTransaction = false;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
            if (networkInfo != null) {
                NetworkInfo.State state = networkInfo.getState();
                Log.d(TAG, "mms state = " + state);
                isMmsInTransaction = (state == NetworkInfo.State.CONNECTING
                    || state == NetworkInfo.State.CONNECTED);
            }
        }
        return isMmsInTransaction;
    }

    public boolean shouldSkipApn(String type) {
        /// M: for plug-in
        return "cmmail".equals(type) || !mRcseApnExt.isRcseOnlyApnEnabled(type);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        int size = menu.size();
        boolean isAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(getActivity());
        Log.d(TAG,"onPrepareOptionsMenu isAirplaneModeOn = " + isAirplaneModeOn);
        // When airplane mode on need to disable options menu
        for (int i = 0;i< size;i++) {
            menu.getItem(i).setEnabled(!isAirplaneModeOn);
        }
        super.onPrepareOptionsMenu(menu);
    }

    private Uri getPreferApnUri(int subId) {
        Uri preferredUri = Uri.withAppendedPath(Uri.parse(PREFERRED_APN_URI), "/subId/" + subId);
        Log.d(TAG, "getPreferredApnUri: " + preferredUri);
        /// M: for plug-in
        preferredUri = mApnExt.getPreferCarrierUri(preferredUri, subId);
        return preferredUri;
    }

    private Uri getDefaultApnUri(int subId) {
        return Uri.withAppendedPath(DEFAULTAPN_URI, "/subId/" + subId);
    }

    // compare prefer apn and set preference checked state
    private void setPreferApnChecked(ArrayList<Preference> apnList) {
        if (apnList == null || apnList.isEmpty()) {
            return;
        }

        String selectedKey = null;
        if (mSelectedKey != null) {
            for (Preference pref : apnList) {
                if (mSelectedKey.equals(pref.getKey())) {
                    ((ApnPreference) pref).setChecked();
                    selectedKey = mSelectedKey;
                }
            }
        }

        // can't find prefer APN in the list, reset to the first one
        if (selectedKey == null && apnList.get(0) != null) {
            ((ApnPreference) apnList.get(0)).setChecked();
            selectedKey = apnList.get(0).getKey();
        }

        // save the new APN
        if (selectedKey != null && selectedKey != mSelectedKey) {
            setSelectedApnKey(selectedKey);
            mSelectedKey = selectedKey;
        }

        Log.d(TAG, "setPreferApnChecked, APN = " + mSelectedKey);
    }
}
