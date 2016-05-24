package com.mediatek.settings.cdg;

import com.android.phone.SubscriptionInfoHelper;

import android.app.Dialog;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager.OnPreferenceTreeClickListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioGroup;

import com.android.phone.R;

public class CdgCallSettings {

    private static final String LOG_TAG = "CdgCallSettings";

    private PreferenceActivity mPreActivity;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;
    private PreferenceScreen mCallForwarding;
    private PreferenceScreen mCallWaiting;
    private PreferenceScreen mDoNotDisturb;
    private PreferenceScreen mVoiceMessageRetrieve;
    private PreferenceScreen mCustomizedEcc;

    private Preference mClickedItem;
    private static final String CDG_CALL_FORWARDING_KEY = "cdg_call_forwarding";
    private static final String CDG_CALL_WAITING_KEY = "cdg_call_waiting";
    private static final String CDG_DO_NOT_DISTURB_KEY = "cdg_do_not_disturb";
    private static final String CDG_VOICE_MESSAGE_RETRIEVE_KEY = "cdg_voice_message_retrieve";
    private static final String CDG_CUSTOMIZED_ECC_KEY = "cdg_customized_ecc_key";

    public CdgCallSettings(PreferenceActivity preActivity,
            SubscriptionInfoHelper subscriptionInfoHelper) {

        if (preActivity == null) {
            log("init CdgCallSettings, but preActivity is null!!!");
            return;
        }

        mPreActivity = preActivity;
        mSubscriptionInfoHelper = subscriptionInfoHelper;

        initUi();
    }

    private void initUi() {
        log("initUI add xml");
        mPreActivity.addPreferencesFromResource(R.layout.mtk_cdg_call_setting_options);
        mCallForwarding = (PreferenceScreen) mPreActivity.findPreference(CDG_CALL_FORWARDING_KEY);
        mCallWaiting = (PreferenceScreen) mPreActivity.findPreference(CDG_CALL_WAITING_KEY);
        mDoNotDisturb = (PreferenceScreen) mPreActivity.findPreference(CDG_DO_NOT_DISTURB_KEY);
        mVoiceMessageRetrieve = (PreferenceScreen) mPreActivity.findPreference(
                CDG_VOICE_MESSAGE_RETRIEVE_KEY);
        mCustomizedEcc = (PreferenceScreen)mPreActivity.findPreference(CDG_CUSTOMIZED_ECC_KEY);

        initCallForwarding();
        initCallWaiting();
        initDoNotDisturb();
        initVoiceMessageRetrieve();
        initUserCustomizedEcc();

    }

    /**
     * Init UserCustomizedEcc item
     */
    private void initUserCustomizedEcc() {
        if (!CdgUtils.isCdgOmhSimCard(mSubscriptionInfoHelper.getSubId())) {
            log("initUserCustomizedEcc don't support");
            mPreActivity.getPreferenceScreen().removePreference(mCustomizedEcc);
        } else {
            log("initUserCustomizedEcc, OK");
        }
    }

    /**
     * If don't support remove the item; else set intent.
     */
    private void initCallForwarding() {
        if (!CdgUtils.isSupportCallForwarding(
                mSubscriptionInfoHelper.getSubId())) {
            log("initCallForwarding don't support.. ");
            mPreActivity.getPreferenceScreen().removePreference(mCallForwarding);
        } else {
            mCallForwarding.setIntent(mSubscriptionInfoHelper.getIntent(
                    CdgCallForwardingPreference.class));
        }
    }

    /**
     * Init CallWaiting item
     */
    private void initCallWaiting() {
        if (!CdgUtils.isSupportCallWaiting(mSubscriptionInfoHelper.getSubId())) {
            log("initCallWaiting do't support.. ");
            mPreActivity.getPreferenceScreen().removePreference(mCallWaiting);
        }
    }

    /**
     * Init DoNotDisturb item
     */
    private void initDoNotDisturb() {
        if (!CdgUtils.isSupportDoNotDisturb(mSubscriptionInfoHelper.getSubId())) {
            log("initDoNotDisturb do't support.. ");
            mPreActivity.getPreferenceScreen().removePreference(mDoNotDisturb);
        }
    }

    /**
     * Init VoiceMessageRetrieve item
     */
    private void initVoiceMessageRetrieve() {
        if (!CdgUtils.isSupportVoiceMessageRetrieve(
                mSubscriptionInfoHelper.getSubId())) {
            log("initVoiceMessageRetrieve do't support.. ");
            mPreActivity.getPreferenceScreen().removePreference(mVoiceMessageRetrieve);
        }
    }

    /**
     * Handle click event of CDG call settings item.
     * @param preferenceScreen
     * @param preference
     * @return
     */
    public boolean onPreferenceTreeClick(
            PreferenceScreen preferenceScreen, Preference preference) {
        int subId = mSubscriptionInfoHelper.getSubId();
        log("Enter onPreferenceTreeClick ...subid = " + subId);

        if (preference == mCallWaiting || preference == mDoNotDisturb) {
            if (mPreActivity != null) {
                /// Use title resource id identify different dialogs
                log("Enter onPreferenceTreeClick show dialog");
                mClickedItem = preference;
                mPreActivity.showDialog(preference.getTitleRes());
            }
            return true;
        } else if (preference == mVoiceMessageRetrieve) {
            String fc = Integer.toString(
                    CdgUtils.getVoiceMessageRetrieveFc(subId)[0]);
            log("onPreferenceTreeClick dial out fc = " + fc);
            CdgUtils.dialOutSsCode(mPreActivity, subId, fc);
            return true;
        } else {
            log("onPreferenceTreeClick pre activity is null!!");
        }
        return false;
    }

    /**
     * Pop up dialog for Call Waiting & Do Not Disturb
     * @param dialogId Dialog title resource id.
     * @return
     */
    public Dialog onCreateDialog(int dialogId) {
        log("onCreateDialog dialogId=" + dialogId + "; title = "
                + mPreActivity.getString(dialogId));

        Dialog dialog = new Dialog(mPreActivity, R.style.CdgOmhDialogTheme);
        dialog.setContentView(R.layout.mtk_cdg_ss_option_dialog);
        dialog.setTitle(mPreActivity.getString(dialogId));
        setDialogButtonsListener(dialog);
        return dialog;
    }

    private void setDialogButtonsListener (final Dialog dialog) {
        Button btnOk = (Button) dialog.findViewById(R.id.button_ok);
        Button btnCancel = (Button) dialog.findViewById(R.id.button_cancel);
        if (btnOk != null && btnCancel != null) {
            btnOk.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int selectId = -1;
                    int subId = mSubscriptionInfoHelper.getSubId();
                    RadioGroup rg = (RadioGroup) dialog.findViewById(R.id.ss_options);
                    if (rg.getCheckedRadioButtonId() == R.id.ss_option_turn_on) {
                        selectId = 0;
                    } else if (rg.getCheckedRadioButtonId() == R.id.ss_option_turn_off) {
                        selectId = 1;
                    } else {
                        log("onClick error no matched item.");
                    }
                    log("onClick id=" + selectId + ";subId=" + subId);
                    String fc = "";
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                        if (mClickedItem == mCallWaiting) {
                            log("Click call waiting item.");
                            fc = Integer.toString(
                                    CdgUtils.getCallWaitingFc(subId)[selectId]);
                            CdgUtils.dialOutSsCode(mPreActivity, subId, fc);
                        } else if (mClickedItem == mDoNotDisturb) {
                            log("Click do not disturb item.");
                            fc = Integer.toString(
                                    CdgUtils.getDoNotDisturbFc(subId)[selectId]);
                            CdgUtils.dialOutSsCode(mPreActivity, subId, fc);
                        }
                    }
                    log("onClick fc=" + fc);
                }
            });
            btnCancel.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                }
            });
        }
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

}
