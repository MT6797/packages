package com.mediatek.settings.ext;

import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Switch;

public class DefaultDataUsageSummaryExt implements IDataUsageSummaryExt {

    public DefaultDataUsageSummaryExt(Context context) {
    }

    @Override
    public String customizeBackgroundString(String defStr, String tag) {
        return defStr;
    }

    @Override
    public boolean needToShowDialog() {
            return true;
    }

    @Override
    public boolean setDataEnableClickListener(Activity activity, View dataEnabledView,
            Switch dataEnabled, OnClickListener dataEnabledDialogListerner) {
        return false;
    }

    /**
     * Called when DataUsageSummary updateBody()
     * @param subId
     */
    public void setCurrentTab(int subId) {
    }

    /**
     * Called when DataUsageSummary onCreate()
     * @param mobileDataEnabled
     */
    public void create(Map<String, Boolean> mobileDataEnabled) {
    }

    /**
     * Called when DataUsageSummary onDestory()
     */
    public void destroy( ) {
    }

    /**
     * Called when DataUsageSummary need set data switch state such as clickable.
     * @param view View
     * @param subId current tab's SIM subId
     * @return true if allow data switch.
     */
    public boolean isAllowDataEnable(View view, int subId) {
        return true;
    }
}
