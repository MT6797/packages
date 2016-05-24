package com.mediatek.settings.ext;

import android.app.Activity;
import android.content.Context;
import android.preference.PreferenceScreen;
import android.telephony.SubscriptionInfo;
import android.util.Log;

import com.android.internal.telephony.SmsApplication;

import java.util.List;


public class DefaultRCSSettings implements IRCSSettings {
    private static final String TAG = "DefaultRCSSettings";

    /**
     * Add rcs setting preference in wireless settings.
     * @param activity The activity of wireless settings
     * @param screen The PreferenceScreen of wireless settings
     */
    public void addRCSPreference(Activity activity, PreferenceScreen screen) {
        Log.d("@M_" + TAG, "DefaultRCSSettings");
    }

    /**
     * Judge whether or not  the AskFirstItem should be reserved.
     * @return true if plug-in want to go host flow.
     */
    public boolean isNeedAskFirstItemForSms() {
        Log.d("@M_" + TAG, "isNeedAskFirstItemForSms");
        return true;
    }

    /**
     * Get default SmsClickContent.
     * @param subInfoList SubscriptionInfo
     * @param value Value
     * @param subId Subid
     * @return subId.
     */
    public int getDefaultSmsClickContentExt(final List<SubscriptionInfo> subInfoList,
            int value, int subId) {
        Log.d("@M_" + TAG, "getDefaultSmsClickContent");
        return subId;
    }

    @Override
    public void setDefaultSmsApplication(String packageName, Context context) {
        SmsApplication.setDefaultApplication(packageName, context);
    }
}
