package com.mediatek.settings.ext;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.telecom.PhoneAccountHandle;
import android.telephony.SubscriptionInfo;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class DefaultSimManagementExt implements ISimManagementExt {

    @Override
    public void onResume(Context context) {
    }

    public void onPause() {
    }

    public void updateSimEditorPref(PreferenceFragment pref) {
        return;
    }

    @Override
    public void updateDefaultSmsSummary(Preference pref) {
    }

    @Override
    public void showChangeDataConnDialog(PreferenceFragment prefFragment, boolean isResumed) {
        return;
    }

    @Override
    public void hideSimEditorView(View view, Context context) {
    }

    @Override
    public void setSmsAutoItemIcon(ImageView view, int dialogId, int position) {
    }

    @Override
    public int getDefaultSmsSubIdForAuto() {
        return 0;
    }

    @Override
    public void initAutoItemForSms(ArrayList<String> list,
            ArrayList<SubscriptionInfo> smsSubInfoList) {
    }

    /**
     * Called before setDefaultDataSubId
     * @param subid
     */
    @Override
    public void setDataState(int subId) {
    }

    /**
     * Called after setDefaultDataSubId
     * @param subid
     */
    @Override
    public void setDataStateEnable(int subId) {
    }

    @Override
    public boolean switchDefaultDataSub(Context context, int subId) {
        return false;
    }

    @Override
    public void customizeListArray(List<String> strings){
    }

    @Override
    public void customizeSubscriptionInfoArray(List<SubscriptionInfo> subscriptionInfo){
    }

    @Override
    public int customizeValue(int value) {
        return value;
    }

    /**
     * Called when SIM dialog is about to show for SIM info changed
     * @return false if plug-in do not need SIM dialog
     */
    public boolean isSimDialogNeeded() {
        return true;
    }

    @Override
    public boolean useCtTestcard() {
        return false;
    }

    /**
     * Called when set radio power state for a specific sub
     * @param subId  the slot to set radio power state
     * @param turnOn  on or off
     */
    public void setRadioPowerState(int subId, boolean turnOn) {
    }

    /**
     * Called when set default subId for sms or data
     * @param context
     * @param sir
     * @param type sms type or data type
     * @return
     */
    public SubscriptionInfo setDefaultSubId(Context context, SubscriptionInfo sir, String type) {
        return sir;
    }

    /**
     * Called when set default phoneAccount for call
     * @param phoneAccount
     * @return
     */
    public PhoneAccountHandle setDefaultCallValue(PhoneAccountHandle phoneAccount) {
        return phoneAccount;
    }

    /**
     * config SimPreferenceScreen.
     * @param simPref simPref
     * @param type type
     * @param size size
     */
    public void configSimPreferenceScreen(Preference simPref, String type, int size) {
    }
}
