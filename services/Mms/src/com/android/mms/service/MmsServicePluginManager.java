package com.android.mms.service;

import android.content.Context;
import android.util.Log;

import com.mediatek.common.MPlugin;
import com.mediatek.mms.service.ext.DefaultMmsServiceCancelDownloadExt;
import com.mediatek.mms.service.ext.DefaultMmsServiceFailedNotifyExt;
import com.mediatek.mms.service.ext.IMmsServiceCancelDownloadExt;
import com.mediatek.mms.service.ext.IMmsServiceFailedNotifyExt;

public class MmsServicePluginManager {
    /// M: op09 feature: cancel download mms @{
    public static final int MMS_PLUGIN_TYPE_MMS_SERVICE_CANCEL_DOWNLOAD = 0x0002;
    public static final int MMS_PLUGIN_TYPE_MMS_SERVICE_TRANSACTION_FAILED_NOTIFY = 0x003;
    /// @}

    private static final String TAG = "MmsServicePluginManager";

    /// M: OP09 Feature: @{
    private static IMmsServiceCancelDownloadExt sMmsServiceCancelDownloadExt = null;
    private static IMmsServiceFailedNotifyExt sMmsServiceFailedNotifyExt = null;
    /// @}

    public static void initPlugins(Context context) {
        sMmsServiceCancelDownloadExt = (IMmsServiceCancelDownloadExt) MPlugin.createInstance(
            IMmsServiceCancelDownloadExt.class.getName(), context);
        if (sMmsServiceCancelDownloadExt == null) {
            Log.d(TAG, "default sMmsServiceCancelDownloadExt =" + sMmsServiceCancelDownloadExt);
            sMmsServiceCancelDownloadExt = new DefaultMmsServiceCancelDownloadExt(context);
        }

        sMmsServiceFailedNotifyExt = (IMmsServiceFailedNotifyExt) MPlugin.createInstance(
            IMmsServiceFailedNotifyExt.class.getName(), context);
        if (sMmsServiceFailedNotifyExt == null) {
            Log.d(TAG, "default sMmsServiceFailedNotifyExt =" + sMmsServiceFailedNotifyExt);
            sMmsServiceFailedNotifyExt = new DefaultMmsServiceFailedNotifyExt(context);
        }
    }

    public static Object getMmsPluginObject(int type) {
        Object obj = null;
        Log.d(TAG, "getMmsPlugin, type = " + type);
        switch (type) {
            case MMS_PLUGIN_TYPE_MMS_SERVICE_CANCEL_DOWNLOAD:
                obj = sMmsServiceCancelDownloadExt;
                break;

            case MMS_PLUGIN_TYPE_MMS_SERVICE_TRANSACTION_FAILED_NOTIFY:
                obj = sMmsServiceFailedNotifyExt;
                break;

            default:
                Log.e(TAG, "mms plugin type = " + type + " don't exist.");
                break;

        }
        return obj;
    }
}
