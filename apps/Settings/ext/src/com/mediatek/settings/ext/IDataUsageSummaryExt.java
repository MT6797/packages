package com.mediatek.settings.ext;

import java.util.Map;

import android.app.Activity;
import android.view.View;
import android.widget.Switch;

public interface IDataUsageSummaryExt {

    static final String TAG_BG_DATA_SWITCH = "bgDataSwitch";
    static final String TAG_BG_DATA_SUMMARY = "bgDataSummary";
    static final String TAG_BG_DATA_APP_DIALOG_TITLE = "bgDataDialogTitle";
    static final String TAG_BG_DATA_APP_DIALOG_MESSAGE = "bgDataDialogMessage";
    static final String TAG_BG_DATA_MENU_DIALOG_MESSAGE = "bgDataMenuDialogMessage";
    static final String TAG_BG_DATA_RESTRICT_DENY_MESSAGE = "bgDataRestrictDenyMessage";

    /**
     * Customize data usage background data restrict string by tag.
     * @param: default string.
     * @param: tag string.
     * @return: customized summary string.
     * @internal
     */
    String customizeBackgroundString(String defStr, String tag);

    /**
     * Customize for Orange
     * Show popup informing user about data enable/disable
     * @param mDataEnabledView : data enabled view for which click listener will be set by plugin
     * @param mDataEnabledDialogListerner : click listener for dialog created by plugin
     * @param isChecked : whether data is enabled or not
     * @internal
     */
    boolean setDataEnableClickListener(Activity activity, View dataEnabledView,
            Switch dataEnabled, View.OnClickListener dataEnabledDialogListerner);
    /**
     * For different operator to show a host dialog
     * @internal
     */
    boolean needToShowDialog();

    /**
     * Called when DataUsageSummary updateBody()
     * @param subId
     * @internal
     */
    public void setCurrentTab(int subId);

    /**
     * Called when DataUsageSummary onCreate()
     * @param mobileDataEnabled
     * @internal
     */
    public void create(Map<String, Boolean> mobileDataEnabled);

    /**
     * Called when DataUsageSummary onDestory()
     * @internal
     */
    public void destroy( );

    /**
     * Called when DataUsageSummary need set data switch state such as clickable.
     * @param view View
     * @param subId current tab's SIM subId
     * @return true if allow data switch.
     */
    public boolean isAllowDataEnable(View view, int subId);
}

