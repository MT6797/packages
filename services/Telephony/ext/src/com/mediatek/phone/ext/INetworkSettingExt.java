package com.mediatek.phone.ext;

import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.android.internal.telephony.OperatorInfo;

import java.util.List;


public interface INetworkSettingExt {

    /**
     * Let plug-in customize the OperatorInfo list before display.
     *
     * @param operatorInfoList The OperatorInfo list get from framework
     * @param subId The sub id user selected
     * @return new OperatorInfo list
     * @internal
     */
    public List<OperatorInfo> customizeNetworkList(List<OperatorInfo> operatorInfoList, int subId);

    /**
     * CU feature, customize forbidden Preference click, pop up a toast.
     * @param operatorInfo Preference's operatorInfo
     * @param subId sub id
     * @return true It means the preference click will be done
     * @internal
     */
    public boolean onPreferenceTreeClick(OperatorInfo operatorInfo, int subId);

    /**
     * Handle the prefernce icon when clicked.
     * @param preferenceScreen parent preference screen
     * @param preference prefernce clicked
     * @return true if handle the preference by plugin
     */
    boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference);

    /**
     * Update the preference screen UI while entering.
     * @param prefSet parent preference screen
     */
    void initOtherNetworkSetting(PreferenceScreen prefSet);
}
