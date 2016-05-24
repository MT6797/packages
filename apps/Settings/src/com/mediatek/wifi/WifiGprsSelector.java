package com.mediatek.wifi;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.ApnPreference;
import com.android.settings.R;
import com.android.settings.wifi.WifiSettings;

import com.mediatek.internal.telephony.CellConnMgr;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * class WifiGprsSelector for operator.
 */
public class WifiGprsSelector extends WifiSettings
        implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "WifiGprsSelector";
    private static final String KEY_APN_LIST = "apn_list";
    private static final String KEY_ADD_WIFI_NETWORK = "add_network";
    private static final String KEY_DATA_ENABLER = "data_enabler";
    private static final String KEY_DATA_ENABLER_GEMINI = "data_enabler_gemini";
    private static final String KEY_DATA_ENABLER_CATEGORY = "data_enabler_category";
    private static final int DIALOG_WAITING = 1001;
    private static final int DIALOG_CELL_STATE_SIM_LOCKED = 1002;

    //time out message event
    private static final int EVENT_DETACH_TIME_OUT = 2000;
    private static final int EVENT_ATTACH_TIME_OUT = 2001;
    //time out length
    private static final int DETACH_TIME_OUT_LENGTH = 10000;
    private static final int ATTACH_TIME_OUT_LENGTH = 30000;

    private static final int SIM_CARD_1 = 0;
    private static final int SIM_CARD_2 = 1;
    private static final int SIM_CARD_SINGLE = 2;
    private static final int SIM_CARD_UNDEFINED = -1;
    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;
    private static final int TYPES_INDEX = 3;
    private static final int SOURCE_TYPE_INDEX = 4;

    private static final int COLOR_INDEX_ZERO = 0;
    private static final int COLOR_INDEX_SEVEN = 7;
    private static final int COLOR_INDEX_EIGHT = 8;
    private static final int SIM_NUMBER_LEN = 4;

    private static final String[] PROJECTION_ARRAY = new String[] {
        Telephony.Carriers._ID,     // 0
        Telephony.Carriers.NAME,    // 1
        Telephony.Carriers.APN,     // 2
        Telephony.Carriers.TYPE,    // 3
        Telephony.Carriers.SOURCE_TYPE,    // 4
    };

    private boolean mIsCallStateIdle = true;
    private boolean mAirplaneModeEnabled = false;

    private TelephonyManager mTelephonyManager;
    private String mSelectedKey;

    private IntentFilter mMobileStateFilter;
    private static final String TRANSACTION_START = "com.android.mms.transaction.START";
    private static final String TRANSACTION_STOP = "com.android.mms.transaction.STOP";

    private static final String PREFERRED_APN_URI = "content://telephony/carriers/preferapn";
    private static final Uri PREFERAPN_URI = Uri.parse(PREFERRED_APN_URI);

    private static final String APN_ID = "apn_id";

    private int mSubId;
    private Uri mUri;
    private Uri mRestoreCarrierUri;

    private PreferenceCategory mApnList;
    private Preference mAddWifiNetwork;
    private CheckBoxPreference mDataEnabler;
    private Preference mDataEnablerGemini;
    private boolean mIsSIMExist = true;
    private WifiManager mWifiManager;

    private static final int DISPLAY_NONE = 0;
    private static final int DISPLAY_FIRST_FOUR = 1;
    private static final int DISPLAY_LAST_FOUR = 2;
    private static final int PIN1_REQUEST_CODE = 302;
    private Map<Integer, SubscriptionInfo> mSimMap;
    private List<Integer> mSimMapKeyList = null;
    private TelephonyManagerEx mTelephonyManagerEx;
    private CellConnMgr mCellConnMgr;
    private int mInitValue;
    private boolean mScreenEnable = true;
    private boolean mIsGprsSwitching = false;
    private int mSelectedDataSubId = -1;

    private final BroadcastReceiver mMobileStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED.equals(action)) {
                long subId = intent.getLongExtra(PhoneConstants.SUBSCRIPTION_KEY,
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                Log.d("@M_" + TAG, "changed default data subId: " + subId);
                mTimeHandler.removeMessages(EVENT_ATTACH_TIME_OUT);
                if (isResumed()) {
                    removeDialog(DIALOG_WAITING);
                }
                mIsGprsSwitching = false;
                updateDataEnabler();
            } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                mAirplaneModeEnabled = intent.getBooleanExtra("state", false);
                Log.d("@M_" + TAG, "AIRPLANE_MODE state changed: " + mAirplaneModeEnabled + ";");
                mApnList.setEnabled(!mAirplaneModeEnabled);
                updateDataEnabler();
            } else if (action.equals(TRANSACTION_START)) {
                Log.d("@M_" + TAG, "ssr: TRANSACTION_START in ApnSettings" + ";");
                mScreenEnable = false;
                mApnList.setEnabled(!mAirplaneModeEnabled && mScreenEnable);
            } else if (action.equals(TRANSACTION_STOP)) {
                Log.d("@M_" + TAG, "ssr: TRANSACTION_STOP in ApnSettings" + ";");
                mScreenEnable = true;
                mApnList.setEnabled(!mAirplaneModeEnabled && mScreenEnable);
            } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                handleWifiStateChanged(intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN));
            }  else if (TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE.equals(action)) {
                Log.d("@M_" + TAG, "receive ACTION_SIM_INFO_UPDATE");
                List<SubscriptionInfo> simList = SubscriptionManager.from(getActivity())
                        .getActiveSubscriptionInfoList();
                if (simList != null) {
                    mSubId = getSubId();
                    updateDataEnabler();
                }
            }
        }
    };

    ContentObserver mGprsConnectObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Log.i("@M_" + TAG, "Gprs connection changed");
            mSubId = getSubId();
            updateDataEnabler();
        }
    };
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            super.onServiceStateChanged(serviceState);

             mIsCallStateIdle =
                mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE;
        }

    };
    private Runnable mServiceComplete = new Runnable() {
        public void run() {

        }
    };
    Handler mTimeHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case EVENT_ATTACH_TIME_OUT:
                    Log.d("@M_" + TAG, "attach time out......");
                    if (isResumed()) {
                        removeDialog(DIALOG_WAITING);
                    }
                    mIsGprsSwitching = false;
                    updateDataEnabler();
                    break;
                case EVENT_DETACH_TIME_OUT:
                    Log.d("@M_" + TAG, "detach time out......");
                    if (isResumed()) {
                        removeDialog(DIALOG_WAITING);
                    }
                    mIsGprsSwitching = false;
                    updateDataEnabler();
                    break;
                default:
                    break;
            }
        };
    };

    private static PhoneConstants.DataState getMobileDataState(Intent intent) {
        String str = intent.getStringExtra(PhoneConstants.STATE_KEY);
        if (str == null) {
            return PhoneConstants.DataState.DISCONNECTED;
        } else {
            return Enum.valueOf(PhoneConstants.DataState.class, str);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d("@M_" + TAG, "onActivityCreated()");
        addPreferencesFromResource(R.xml.wifi_access_points_and_gprs);
        mApnList = (PreferenceCategory) findPreference(KEY_APN_LIST);
        mAddWifiNetwork = findPreference(KEY_ADD_WIFI_NETWORK);

        PreferenceCategory dataEnableCategory =
            (PreferenceCategory) findPreference(KEY_DATA_ENABLER_CATEGORY);
        if (isGeminiSupport()) {
            mDataEnablerGemini = findPreference(KEY_DATA_ENABLER_GEMINI);
            dataEnableCategory.removePreference(findPreference(KEY_DATA_ENABLER));
        } else {
            mDataEnabler = (CheckBoxPreference) findPreference(KEY_DATA_ENABLER);
            mDataEnabler.setOnPreferenceChangeListener(this);
            dataEnableCategory.removePreference(
                    findPreference(KEY_DATA_ENABLER_GEMINI));
        }



        initPhoneState();
        mMobileStateFilter = new IntentFilter(
                TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        mMobileStateFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mMobileStateFilter.addAction(TRANSACTION_START);
        mMobileStateFilter.addAction(TRANSACTION_STOP);
        mMobileStateFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mMobileStateFilter.addAction(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        mMobileStateFilter.addAction(TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE);
        getActivity().setTitle(R.string.wifi_gprs_selector_title);
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        init();
        setHasOptionsMenu(false);
    }

    @Override
    public void onResume() {
        Log.d("@M_" + TAG, "onResume");
        super.onResume();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        getActivity().registerReceiver(mMobileStateReceiver, mMobileStateFilter);
        mAirplaneModeEnabled = Settings.Global.getInt(getActivity().getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0 ? true : false;
        /// M: WifiManager memory leak , change context to getApplicationContext @{
        mWifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(
                Context.WIFI_SERVICE);
        ///@}
        handleWifiStateChanged(mWifiManager.getWifiState());

        mScreenEnable = isMMSNotTransaction();

        fillList(mSubId);
        updateDataEnabler();
        if (isGeminiSupport()) {
            mCellConnMgr = new CellConnMgr(getActivity());
            getContentResolver().registerContentObserver(Settings.System.getUriFor(
                    Settings.System.GPRS_CONNECTION_SIM_SETTING), false, mGprsConnectObserver);
        }
        if (mIsGprsSwitching) {
            showDialog(DIALOG_WAITING);
        }
    }
    private boolean isMMSNotTransaction() {
        boolean isMMSNotProcess = true;
        ConnectivityManager cm = (ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
            if (networkInfo != null) {
                NetworkInfo.State state = networkInfo.getState();
                Log.d("@M_" + TAG, "mms state = " + state);
                isMMSNotProcess = (state != NetworkInfo.State.CONNECTING
                    && state != NetworkInfo.State.CONNECTED);
            }
        }
        return isMMSNotProcess;
    }
    private boolean init() {
        Log.d("@M_" + TAG, "init()");

        mIsSIMExist = mTelephonyManager.hasIccCard();

        return true;
    }

    @Override
    public void onPause() {
        Log.d("@M_" + TAG, "onPause");
        super.onPause();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        getActivity().unregisterReceiver(mMobileStateReceiver);
        if (isGeminiSupport()) {
            getContentResolver().unregisterContentObserver(mGprsConnectObserver);
        }
        if (mIsGprsSwitching) {
            removeDialog(DIALOG_WAITING);
        }
    }
    @Override
    public void onDestroy() {
        mTimeHandler.removeMessages(EVENT_ATTACH_TIME_OUT);
        mTimeHandler.removeMessages(EVENT_DETACH_TIME_OUT);
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

    }
    private void initPhoneState() {
        Log.d("@M_" + TAG, "initPhoneState()");
        //if (isGeminiSupport()) {
        Intent it = getActivity().getIntent();
        mSubId = it.getIntExtra("simId", SIM_CARD_UNDEFINED);

        //mTelephonyManagerEx = TelephonyManagerEx.getDefault();
        mSimMap = new HashMap<Integer, SubscriptionInfo>();
        initSimMap();

        if (mSubId == -1) {
            mSubId = getSubId();
        }
        Log.d("@M_" + TAG, "GEMINI_SIM_ID_KEY = " + mSubId + ";");

    }

    private void fillList(int subId) {
        mApnList.removeAll();
        if (subId < 0 || subId > 2) {
            return;
        }
        Log.d("@M_" + TAG, "fillList(), subId=" + subId + ";");
        String where;

        where = "numeric=\"" + getQueryWhere(subId) + "\"";
        ///M: For lte should not display ia type apn.
        where += " AND NOT (type='ia' AND (apn=\'\' OR apn IS NULL))";
        /// M: for non-volte project,do not show ims apn @{
        if (!SystemProperties.get("ro.mtk_volte_support").equals("1")) {
            where += " AND NOT type='ims'";
        }

        Log.d("@M_" + TAG, "where = " + where + ";");
        Cursor cursor = getActivity().managedQuery(
                mUri, // Telephony.Carriers.CONTENT_URI,
                PROJECTION_ARRAY, where,
                Telephony.Carriers.DEFAULT_SORT_ORDER);

        ArrayList<Preference> mmsApnList = new ArrayList<Preference>();

        boolean keySetChecked = false;
        mSelectedKey = getSelectedApnKey();
        Log.d("@M_" + TAG, "mSelectedKey = " + mSelectedKey + ";");

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String name = cursor.getString(NAME_INDEX);
            String apn = cursor.getString(APN_INDEX);
            String key = cursor.getString(ID_INDEX);
            String type = cursor.getString(TYPES_INDEX);
            int sourcetype = cursor.getInt(SOURCE_TYPE_INDEX);


            ApnPreference pref = new ApnPreference(getActivity());

            pref.setSubId(subId);   //set pre sim id info to the ApnEditor
            pref.setKey(key);
            pref.setTitle(name);
            pref.setSummary(apn);
            //pref.setSourceType(sourcetype);
            pref.setPersistent(false);
            pref.setOnPreferenceChangeListener(this);

            boolean selectable = ((type == null) || (!type.equals("mms")
                    && !type.equals("cmmail") && !type.equals("ims")));
            pref.setSelectable(selectable);
            if (selectable) {
                if ((mSelectedKey != null) && mSelectedKey.equals(key)) {
                    setSelectedApnKey(key);
                    pref.setChecked();
                    keySetChecked = true;
                    Log.d("@M_" + TAG, "apn key: " + key + " set." + ";");
                }
                Log.d("@M_" + TAG, "key:  " + key + " added!" + ";");
                mApnList.addPreference(pref);
                if (isGeminiSupport()) {
                    pref.setDependency(KEY_DATA_ENABLER_GEMINI);
                } else {
                    pref.setDependency(KEY_DATA_ENABLER);
                }
            } else {
                mmsApnList.add(pref);
            }
            cursor.moveToNext();
        }

        int mSelectableApnCount = mApnList.getPreferenceCount();
        //if no key selected, choose the 1st one.
        if (!keySetChecked && mSelectableApnCount > 0) {
            ApnPreference apnPref = (ApnPreference) mApnList.getPreference(0);
            if (apnPref != null) {
                setSelectedApnKey(apnPref.getKey());
                apnPref.setChecked();
                Log.d("@M_" + TAG, "Key does not match.Set key: " + apnPref.getKey() + ".");
            }

        }

        mIsCallStateIdle = mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE;
        int slotId = SubscriptionManager.getSlotId(subId);
        boolean simReady = (TelephonyManager.SIM_STATE_READY ==
                mTelephonyManager.getSimState(slotId));
        mApnList.setEnabled(mScreenEnable && mIsCallStateIdle && !mAirplaneModeEnabled && simReady);

    }

    private String getQueryWhere(int subId) {
        String where = "";
        String numeric = TelephonyManager.getDefault().getSimOperator(subId);
        where = numeric;
        mUri = Telephony.Carriers.CONTENT_URI.buildUpon()
                       .appendPath("subId")
                       .appendPath(Integer.toString(subId))
                       .build();

        mRestoreCarrierUri = PREFERAPN_URI.buildUpon()
                       .appendPath("subId")
                       .appendPath(Integer.toString(subId))
                       .build();
        Log.d("@M_" + TAG, "where = " + where + ";");
        Log.d("@M_" + TAG, "mUri = " + mUri + ";");
        return where;
    }

    /** {@inheritDoc} */
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d("@M_" + TAG, "onPreferenceChange(): Preference - " + preference
                + ", newValue - " + newValue + ", newValue type - "
                + newValue.getClass());
        String key = (preference == null ? "" : preference.getKey());
        if (KEY_DATA_ENABLER.equals(key)) {
            final boolean checked = ((Boolean) newValue).booleanValue();
            Log.d("@M_" + TAG, "Data connection enabled?" + checked);
            dealWithConnChange(checked);
        }  else {
            if (newValue instanceof String) {
                setSelectedApnKey((String) newValue);
            }
        }

        return true;
    }
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        String key = preference.getKey();
        if (KEY_ADD_WIFI_NETWORK.equals(key)) {
            if (mWifiManager.isWifiEnabled()) {
                Log.d("@M_" + TAG, "add network");
                super.addNetworkForSelector();
            }
        } else if (KEY_DATA_ENABLER_GEMINI.equals(key)) {
            //connect data connection
            SimItem simitem;
            final List<SimItem> simItemList = new ArrayList<SimItem>();
            for (Integer simid: mSimMapKeyList) {
                SubscriptionInfo subinfo = mSimMap.get(simid);

                if (subinfo != null) {
                    simitem = new SimItem(subinfo, this);
                    simitem.mState = mTelephonyManager.getSimState(subinfo.getSimSlotIndex());
                    simItemList.add(simitem);
                }
            }
            final int simListSize = simItemList.size();
            Log.d("@M_" + TAG, "simListSize = " + simListSize);
            int offItem = simListSize - 1;
            int index = -1;
            int dataConnectId = SubscriptionManager.getDefaultDataSubId();
            Log.d("@M_" + TAG, "getSimSlot,dataConnectId = " + dataConnectId);
            for (int i = 0; i < offItem; i++) {
                if (simItemList.get(i).mSubId == dataConnectId) {
                    index = i;
                }
            }
            mInitValue = index == -1 ? offItem : index;
            Log.d("@M_" + TAG, "mInitValue = " + mInitValue);

            SelectionListAdapter mAdapter = new SelectionListAdapter(simItemList);
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
            //.setCancelable(false)
            .setSingleChoiceItems(mAdapter, mInitValue, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("@M_" + TAG, "which = " + which);
                        SimItem simItem = simItemList.get(which);
                        mSubId = simItem.mSubId;
                        Log.d("@M_" + TAG, "mSubId = " + mSubId);
                        Log.d("@M_" + TAG, "mIsSim=" + simItem.mIsSim + ",mState=" + simItem.mState
                            + ",SIM_INDICATOR_LOCKED=" + 1);

                        if (simItem.mIsSim) {
                            int state = mCellConnMgr.getCurrentState(
                                simItem.mSubId, CellConnMgr.STATE_SIM_LOCKED);
                            if (mCellConnMgr != null && state == CellConnMgr.STATE_SIM_LOCKED) {
                                Log.d("@M_" + TAG, "mCellConnMgr.handleCellConn");
                                showDialog(DIALOG_CELL_STATE_SIM_LOCKED);
                                //mCellConnMgr.handleCellConn(simItem.mSlot, PIN1_REQUEST_CODE);
                            } else {
                                switchGprsDefautlSIM(simItem.mSubId);
                            }
                        } else {
                            switchGprsDefautlSIM(0);
                        }
                        dialog.dismiss();
                    }
                })
            .setTitle(R.string.data_conn_category_title)
            .setNegativeButton(com.android.internal.R.string.no,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
            .create();
            dialog.show();
        } else {
            return super.onPreferenceTreeClick(screen, preference);
        }
        return true;
    }
    private int getSubId() {
        return SubscriptionManager.getDefaultDataSubId();

    }
    private void handleWifiStateChanged(int state) {
        Log.d("@M_" + TAG, "handleWifiStateChanged(), new state=" + state + ";");
        Log.d("@M_" + TAG, "[0- stoping 1-stoped 2-starting 3-started 4-unknown]");
        if (state == WifiManager.WIFI_STATE_ENABLED) {
            mAddWifiNetwork.setEnabled(true);
        } else {
            mAddWifiNetwork.setEnabled(false);
        }
    }

    private void setSelectedApnKey(String key) {
        mSelectedKey = key;
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(APN_ID, mSelectedKey);
        resolver.update(mRestoreCarrierUri, values, null, null);
    }

    private String getSelectedApnKey() {
        String key = null;
        Cursor cursor = getActivity().managedQuery(mRestoreCarrierUri,
                new String[] {"_id"},
                null,
                Telephony.Carriers.DEFAULT_SORT_ORDER);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            key = cursor.getString(ID_INDEX);
        }
        //cursor.close();
        return key;
    }

    private void updateDataEnabler() {
        if (isGeminiSupport()) {
            Log.d("@M_" + TAG, "updateDataEnabler, mSubId=" + mSubId);
            fillList(mSubId);
            mDataEnablerGemini.setEnabled(mIsSIMExist && !mAirplaneModeEnabled);
        } else {
            boolean enabled = mTelephonyManager.getDataEnabled();
            Log.d("@M_" + TAG, "updateDataEnabler(), current state=" + enabled);
            mDataEnabler.setChecked(enabled);
            Log.d("@M_" + TAG, "single card mDataEnabler, true");
            mDataEnabler.setEnabled(mIsSIMExist && !mAirplaneModeEnabled);
        }
    }

    /**
     * To enable/disable data connection.
     * @param enabled
     */
    private void dealWithConnChange(boolean enabled) {
        if (isGeminiSupport()) {
            Log.d("@M_" + TAG, "only sigle SIM load can controling data connection");
            return;
        }
        Log.d("@M_" + TAG, "dealWithConnChange(),new request state is enabled?" + enabled + ";");
        mTelephonyManager.setDataEnabled(enabled);
        showDialog(DIALOG_WAITING);
        mIsGprsSwitching = true;
        if (enabled) {
            mTimeHandler.sendEmptyMessageDelayed(EVENT_ATTACH_TIME_OUT, ATTACH_TIME_OUT_LENGTH);
        } else {
            mTimeHandler.sendEmptyMessageDelayed(EVENT_DETACH_TIME_OUT, DETACH_TIME_OUT_LENGTH);
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        if (id == DIALOG_WAITING) {
            dialog.setMessage(getResources().getString(R.string.data_enabler_waiting_message));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            return dialog;
        } else if (id == DIALOG_CELL_STATE_SIM_LOCKED) {
            /* Create sim locked dialog */
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            ArrayList<String> simStatusStrings = new ArrayList<String>();
            simStatusStrings = mCellConnMgr.getStringUsingState(
                    mSelectedDataSubId, CellConnMgr.STATE_SIM_LOCKED);
            builder.setTitle(simStatusStrings.get(0));
            builder.setMessage(simStatusStrings.get(1));
            builder.setPositiveButton(simStatusStrings.get(2),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        /* cell connection manager will handle this lock status */
                        mCellConnMgr.handleRequest(mSelectedDataSubId,
                                    CellConnMgr.STATE_SIM_LOCKED);
                    }
            });
            builder.setNegativeButton(simStatusStrings.get(3),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing
                    }
            });
            return builder.create();
        } else {
            return super.onCreateDialog(id);
        }
    }

    private void initSimMap() {

        List<SubscriptionInfo> simList = SubscriptionManager.from(getActivity())
                .getActiveSubscriptionInfoList();
        if (simList != null) {
            mSimMap.clear();
            Log.i("@M_" + TAG, "sim number is " + simList.size());
            for (SubscriptionInfo subinfo : simList) {
                mSimMap.put(subinfo.getSubscriptionId(), subinfo);
            }
            mSimMapKeyList = (List<Integer>) (new ArrayList(mSimMap.keySet()));
        }
    }


    /**
     * switch data connection default SIM.
     * @param value: sim id of the new default SIM
     */
    private void switchGprsDefautlSIM(int subId) {

        if (subId < 0) {
            return;
        }
        if (SubscriptionManager.isValidSubscriptionId(subId) &&
                subId != SubscriptionManager.getDefaultDataSubId()) {
            SubscriptionManager.from(getActivity()).setDefaultDataSubId(subId);
            showDialog(DIALOG_WAITING);
            mIsGprsSwitching = true;
            if (subId > 0) {
                mTimeHandler.sendEmptyMessageDelayed(EVENT_ATTACH_TIME_OUT, ATTACH_TIME_OUT_LENGTH);
                Log.d("@M_" + TAG, "set ATTACH_TIME_OUT");
            } else {
                mTimeHandler.sendEmptyMessageDelayed(EVENT_DETACH_TIME_OUT, DETACH_TIME_OUT_LENGTH);
                Log.d("@M_" + TAG, "set DETACH_TIME_OUT");
            }
        }

    }

    /*public int getSimColorResource(int color) {
        if ((color >= COLOR_INDEX_ZERO) && (color <= COLOR_INDEX_SEVEN)) {
            return SimInfoManager.SimBackgroundDarkRes[color];
        } else {
            return -1;
        }
    }*/

    /**
     * getStatusResource.
     * @param state PhoneConstants state
     * @return drawable id
     */
    public int getStatusResource(int state) {
        switch (state) {
        case 1:/* PhoneConstants.SIM_INDICATOR_RADIOOFF: */
            return com.mediatek.internal.R.drawable.sim_radio_off;
        case 2:/*PhoneConstants.SIM_INDICATOR_LOCKED: */
            return com.mediatek.internal.R.drawable.sim_locked;
        case 3:/*PhoneConstants.SIM_INDICATOR_INVALID:*/
            return com.mediatek.internal.R.drawable.sim_invalid;
        case 4:/*PhoneConstants.SIM_INDICATOR_SEARCHING:*/
            return com.mediatek.internal.R.drawable.sim_searching;
        case 6:/*PhoneConstants.SIM_INDICATOR_ROAMING:*/
            return com.mediatek.internal.R.drawable.sim_roaming;
        case 7:/*PhoneConstants.SIM_INDICATOR_CONNECTED:*/
            return com.mediatek.internal.R.drawable.sim_connected;
        case 8:/*PhoneConstants.SIM_INDICATOR_ROAMINGCONNECTED:*/
            return com.mediatek.internal.R.drawable.sim_roaming_connected;
        default:
            return -1;
        }
    }

    /**
     * get gemini support status.
     */
    private boolean isGeminiSupport() {
        TelephonyManager.MultiSimVariants config
            = TelephonyManager.getDefault().getMultiSimConfiguration();
        if (config == TelephonyManager.MultiSimVariants.DSDS
            || config == TelephonyManager.MultiSimVariants.DSDA) {
            return true;
        }
        return false;
    }

    /**
     * class SelectionListAdapter for operator.
     */
    class SelectionListAdapter extends BaseAdapter {
        List<SimItem> mSimItemList;

        public SelectionListAdapter(List<SimItem> simItemList) {
            mSimItemList = simItemList;
        }

        public int getCount() {
            return mSimItemList.size();
        }

        public Object getItem(int position) {
            return mSimItemList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater mFlater = LayoutInflater.from(getActivity());
                convertView = mFlater.inflate(R.layout.preference_sim_default_select, null);
                holder = new ViewHolder();
                setViewHolderId(holder, convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            SimItem simItem = (SimItem) getItem(position);
            setNameAndNum(holder.mTextName, holder.mTextNum, simItem);
            setImageSim(holder.mImageSim, simItem);
            setImageStatus(holder.mImageStatus, simItem);
            setTextNumFormat(holder.mTextNumFormat, simItem);
            holder.mCkRadioOn.setChecked(mInitValue == position);
            if (simItem.mState == 1/*PhoneConstants.SIM_INDICATOR_RADIOOFF*/) {
                convertView.setEnabled(false);
                holder.mTextName.setEnabled(false);
                holder.mTextNum.setEnabled(false);
                holder.mCkRadioOn.setEnabled(false);
            } else {
                convertView.setEnabled(true);
                holder.mTextName.setEnabled(true);
                holder.mTextNum.setEnabled(true);
                holder.mCkRadioOn.setEnabled(true);
            }
            return convertView;
        }

        private void setTextNumFormat(TextView textNumFormat, SimItem simItem) {
            if (simItem.mIsSim) {
                if (simItem.mNumber != null) {
                    switch (simItem.mDispalyNumberFormat) {
                        case DISPLAY_NONE:
                            textNumFormat.setVisibility(View.GONE);
                            break;
                        case DISPLAY_FIRST_FOUR:
                            textNumFormat.setVisibility(View.VISIBLE);
                            if (simItem.mNumber.length() >= SIM_NUMBER_LEN) {
                                textNumFormat.setText(simItem.mNumber.substring(0, SIM_NUMBER_LEN));
                            } else {
                                textNumFormat.setText(simItem.mNumber);
                            }
                            break;
                        case DISPLAY_LAST_FOUR:
                            textNumFormat.setVisibility(View.VISIBLE);
                            if (simItem.mNumber.length() >= SIM_NUMBER_LEN) {
                                textNumFormat.setText(simItem.mNumber.substring(
                                    simItem.mNumber.length() - SIM_NUMBER_LEN));
                            } else {
                                textNumFormat.setText(simItem.mNumber);
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        private void setImageStatus(ImageView imageStatus, SimItem simItem) {
            if (simItem.mIsSim) {
                int res = getStatusResource(simItem.mState);
                if (res == -1) {
                    imageStatus.setVisibility(View.GONE);
                } else {
                    imageStatus.setVisibility(View.VISIBLE);
                    imageStatus.setImageResource(res);
                }
            }
        }

        private void setImageSim(RelativeLayout imageSim, SimItem simItem) {
            if (simItem.mIsSim) {
                Bitmap resColor = simItem.mSimIconBitmap;
                if (resColor != null) {
                    Drawable drawable = new BitmapDrawable(resColor);
                    imageSim.setVisibility(View.VISIBLE);
                    imageSim.setBackground(drawable);
                }
            } else if (simItem.mColor == COLOR_INDEX_EIGHT) {
                imageSim.setVisibility(View.VISIBLE);
                imageSim.setBackgroundResource(com.mediatek.internal.R.drawable.sim_background_sip);
            } else {
                imageSim.setVisibility(View.GONE);
            }
        }

        private void setViewHolderId(ViewHolder holder, View convertView) {
            holder.mTextName = (TextView) convertView.findViewById(R.id.simNameSel);
            holder.mTextNum = (TextView) convertView.findViewById(R.id.simNumSel);
            holder.mImageStatus = (ImageView) convertView.findViewById(R.id.simStatusSel);
            holder.mTextNumFormat = (TextView) convertView.findViewById(R.id.simNumFormatSel);
            holder.mCkRadioOn = (RadioButton) convertView.findViewById(R.id.Enable_select);
            holder.mImageSim = (RelativeLayout) convertView.findViewById(R.id.simIconSel);
        }

        private void setNameAndNum(TextView textName, TextView textNum, SimItem simItem) {
            if (simItem.mName == null) {
                textName.setVisibility(View.GONE);
            } else {
                textName.setVisibility(View.VISIBLE);
                textName.setText(simItem.mName);
            }
            if ((simItem.mIsSim) && ((simItem.mNumber != null) &&
                    (simItem.mNumber.length() != 0))) {
                textNum.setVisibility(View.VISIBLE);
                textNum.setText(simItem.mNumber);
            } else {
                textNum.setVisibility(View.GONE);
            }
        }

        /**
         * class ViewHolder.
         */
        class ViewHolder {
            TextView mTextName;
            TextView mTextNum;
            RelativeLayout mImageSim;
            ImageView mImageStatus;
            TextView mTextNumFormat;
            RadioButton mCkRadioOn;
        }
    }

    /**
     * class SimItem.
     */
    static class SimItem {
        public boolean mIsSim = true;
        public String mName = null;
        public String mNumber = null;
        public int mDispalyNumberFormat = 0;
        public int mColor = -1;
        public int mSlot = -1;
        public int mState = 5; //PhoneConstants.SIM_INDICATOR_NORMAL;
        public int mSubId = -1;
        public Bitmap mSimIconBitmap = null;
        private WifiGprsSelector mWifiGprsSeletor = null;

        //Constructor for not real sim
        public SimItem(String name, int color, long simID, WifiGprsSelector wifiGprsSelector) {
            mName = name;
            mColor = color;
            mIsSim = false;
            mWifiGprsSeletor = wifiGprsSelector;
        }
        //constructor for sim
        public SimItem(SubscriptionInfo subinfo, WifiGprsSelector wifiGprsSelector) {
            mIsSim = true;
            mName = subinfo.getDisplayName().toString();
            mNumber = subinfo.getNumber();
            mColor = subinfo.getIconTint();
            mSlot = subinfo.getSimSlotIndex();
            mSubId = subinfo.getSubscriptionId();
            mWifiGprsSeletor = wifiGprsSelector;
            mSimIconBitmap = subinfo.createIconBitmap(wifiGprsSelector.getActivity());
        }
    }
}
