package com.mediatek.settings.cdg;

import android.app.ActionBar;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;
import com.mediatek.settings.TelephonyUtils;
import com.mediatek.settings.cdma.TelephonyUtilsEx;

/**
 * This class is call forwarding activity, contains:
 * 1,Always; 2,Busy; 3,Unanswered; 4,Default kinds of call forwarding items.
 */
public class CdgCallForwardingPreference extends PreferenceActivity
                implements PhoneGlobals.SubInfoUpdateListener {

    private static final String LOG_TAG = "CallSettings/CdgCallForwardingPreference";

    /// Always, Busy, unanswered, Default
    private static final String BUTTON_CFU_KEY   = "button_cfu_key";
    private static final String BUTTON_CFB_KEY   = "button_cfb_key";
    private static final String BUTTON_CFNRY_KEY = "button_cfnry_key";
    private static final String BUTTON_CFNRC_KEY = "button_cfnrc_key";

    private static final String[] KINDS_OF_CF_KEYS = {
        BUTTON_CFU_KEY, BUTTON_CFB_KEY, BUTTON_CFNRY_KEY, BUTTON_CFNRC_KEY
    };
    private static final int[] CF_REASONS = {
        CdgUtils.CF_ALWAYS, CdgUtils.CF_BUSY,
        CdgUtils.CF_NOT_ANSWER, CdgUtils.CF_DEFAULT
    };

    /// Get contact from contacts app.
    private static final int DIALOG_SELECT_CONTACT = 11;
    private static final int GET_CONTACTS = 100;
    private static final String NUM_PROJECTION[] = {Phone.NUMBER};
    private EditText mEditNumber = null;
    /// This id is used indicate on which type of call forwarding does the
    /// user press to get contact from contacts app.
    private int mCfReasonId = -1;
    private int mShowingDialogId = -1;
    private int mCfOptionCheckedId = -1;

    private SubscriptionInfoHelper mSubscriptionInfoHelper;
    private RadioGroup mCfOption;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.layout.mtk_cdg_call_forwarding_options);

        mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());

        /// Enable action bar's back key.
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        /// Listen event for subscription info changed & airplane mode.
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        int dialogId = preference.getOrder();
        showDialog(dialogId);
        mShowingDialogId = preference.getOrder();
        /// Before the dialog dismiss remember the id.
        mCfReasonId = dialogId;
        log("onPreferenceTreeClick order : " + dialogId);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();

        switch (itemId) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(final int id) {
        log("onCreateDialog, id = " + id);
        Dialog dialog = null;
        if (DIALOG_SELECT_CONTACT != id) {

            dialog = new Dialog(this, R.style.CdgOmhDialogTheme);
            dialog.setContentView(R.layout.mtk_cdg_call_forwarding_option_dialog);
            dialog.setTitle(getPreferenceScreen().getPreference(id).getTitle());
            RadioGroup cfOption = (RadioGroup) dialog.findViewById(R.id.cf_options);
            mCfOptionCheckedId = cfOption.getCheckedRadioButtonId();

            cfOption.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    mCfOptionCheckedId = checkedId;
                    log("onCheckedChanged, checkedId = " + checkedId);
                }
            });
        } else {
            dialog = new Dialog(this, R.style.CdgOmhDialogTheme);
            dialog.setContentView(R.layout.mtk_cdg_call_forwarding_contact_dialog);
            dialog.setTitle(getString(R.string.title_cf_option_phone_number));

            ImageButton getContact = (ImageButton) dialog.findViewById(
                    R.id.select_contact);
            getContact.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    getContacts();
                }
            });
        }
        setDialogButtonsListener(dialog);
        return dialog;
    }

    @Override
    public void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        // Do not initialize mEditNumber in onCreateDialog, it is only called
        // when Dialog is created.
        if (mShowingDialogId == DIALOG_SELECT_CONTACT) {
            log("onPrepareDialog init EditText");
            mEditNumber = (EditText) dialog.findViewById(R.id.edt_contact);
        }
    }

    private void setDialogButtonsListener (final Dialog dialog) {
        Button btnOk = (Button) dialog.findViewById(R.id.button_ok);
        Button btnCancel = (Button) dialog.findViewById(R.id.button_cancel);
        if (btnOk != null) {
            btnOk.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    onBtnOkClick();
                }
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }
    }

    private void onBtnOkClick() {
        int subId = mSubscriptionInfoHelper.getSubId();
        log("onBtnOkClick subId = " + subId);
        /// CF option dialog
        if (mShowingDialogId != DIALOG_SELECT_CONTACT) {
            log("onBtnOkClick selectedOptionId = " + mCfOptionCheckedId);
            switch (mCfOptionCheckedId) {
                /// to phone number
                case R.id.cf_option_phone_number:
                    mShowingDialogId = DIALOG_SELECT_CONTACT;
                    showDialog(DIALOG_SELECT_CONTACT);
                    return;
                /// to voice mail
                case R.id.cf_option_voice_mail:
                    CdgUtils.dialOutSsCode(this, subId, CdgUtils.getCallForwardingFc(
                            subId, CF_REASONS[mCfReasonId], CdgUtils.CF_TO_VOICE_MAIL));
                    return;
                case R.id.cf_option_stop:
                    CdgUtils.dialOutSsCode(this, subId, CdgUtils.getCallForwardingFc(
                            subId, CF_REASONS[mCfReasonId], CdgUtils.CF_STOP));
                    return;
            }
        } else if (mShowingDialogId == DIALOG_SELECT_CONTACT) {
            log("onBtnOkClick, set contacts mCfReasonId=" + mCfReasonId);
            /// reset to default value.
            CdgUtils.dialOutSsCode(this, subId, CdgUtils.getCallForwardingFc(
                    subId, CF_REASONS[mCfReasonId], CdgUtils.CF_TO_NUMBER)
                    + mEditNumber.getText());
        } else {
            log("onBtnOkClick, mShowingDialogId = " + mShowingDialogId);
        }
    }

    /**
     * Function to get contact from contacts app.
     */
    private void getContacts() {
        Intent intent = new Intent("android.intent.action.PICK");
        intent.setType(Phone.CONTENT_TYPE);

        startActivityForResult(intent, GET_CONTACTS);
    }

    @Override
    protected void onActivityResult(
            final int requestCode, final int resultCode, final Intent data) {

        log("onActivityResult resultCode=" + resultCode);
        if (resultCode != RESULT_OK || requestCode != GET_CONTACTS
                || data == null) {
            log("onActivityResult error!!");
            return;
        }

        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(data.getData(),
                    NUM_PROJECTION, null, null, null);
            if (cursor != null && cursor.moveToFirst() && mEditNumber != null) {
                mEditNumber.setText(cursor.getString(0));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public void handleSubInfoUpdate() {
        log("handleSubInfoUpdate finish");
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUi();
    }

    private void updateUi() {
        boolean isShouldEnabled = false;
        boolean isIdle = (TelephonyManager.getDefault().getCallState(
                mSubscriptionInfoHelper.getSubId()) == TelephonyManager.CALL_STATE_IDLE);
        boolean airplaneModeEnabled = TelephonyUtilsEx.isAirPlaneMode();
        isShouldEnabled = isIdle && (!airplaneModeEnabled) &&
                TelephonyUtils.isRadioOn(mSubscriptionInfoHelper.getSubId(), this);
        log("updateUi, isIdle=" + isIdle + "; isShouldEnabled=" + isShouldEnabled);
        getPreferenceScreen().setEnabled(isShouldEnabled);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                if (intent.getBooleanExtra("state", false)) {
                    finish();
                }
            }
        }
    };

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

}
