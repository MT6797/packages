package com.mediatek.phone.ext;

import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.android.internal.telephony.OperatorInfo;

import java.util.List;

public class DefaultNetworkSettingExt implements INetworkSettingExt {

    @Override
    public List<OperatorInfo> customizeNetworkList(List<OperatorInfo> operatorInfoList, int subId) {
        return operatorInfoList;
    }

    @Override
    public boolean onPreferenceTreeClick(OperatorInfo operatorInfo, int subId) {
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Log.d("DefaultNetworkSettingExt", "onPreferenceTreeClick");
        return false;
    }

    @Override
    public void initOtherNetworkSetting(PreferenceScreen prefSet) {
        return ;
    }
}
