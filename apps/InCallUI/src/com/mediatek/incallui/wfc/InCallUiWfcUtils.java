package com.mediatek.incallui.wfc;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telecom.DisconnectCause;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;

import com.android.ims.ImsManager;
import com.android.incallui.Call;
import com.android.incallui.CallList;
import com.android.incallui.Call.State;
import com.android.incallui.InCallPresenter;
import com.android.incallui.Log;

import com.mediatek.wfo.IWifiOffloadService;
import com.mediatek.wfo.WifiOffloadManager;


public class InCallUiWfcUtils {
    private static AlertDialog sGeneralDialog;
    private static Context sContext;
    private static final String KEY_IS_FIRST_WIFI_CALL = "key_first_wifi_call";
    private static final String LOG_TAG = "InCallUiWfcUtils";
    private static final Handler mHandler = new Handler();



    public static boolean maybeShowWfcError(Context context, DisconnectCause disconnectCause) {
        if(ImsManager.isWfcEnabledByUser(context)
                && (disconnectCause.getCode() == DisconnectCause.WFC_CALL_ERROR)) {
            Log.d(LOG_TAG, "[wfc]maybeShowWfcError maybeShowWfcError ");
            maybeShowError(context, disconnectCause.getLabel(),
                    disconnectCause.getDescription());
            return true;
        } else {
            return false;
        }
    }

    public static void maybeShowError(Context context, CharSequence label,
            CharSequence description) {
        Log.i(LOG_TAG, "[WFC]maybeShowError");
        sContext = context;
        final Intent intent = new Intent(sContext, WfcDialogActivity.class);
        intent.putExtra(WfcDialogActivity.SHOW_WFC_CALL_ERROR_POPUP, true);
        intent.putExtra(WfcDialogActivity.WFC_ERROR_LABEL, label);
        intent.putExtra(WfcDialogActivity.WFC_ERROR_DECRIPTION, description);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sContext.startActivity(intent);
    }

    public static boolean maybeShowCongratsPopup(Context context, DisconnectCause disconnectCause) {
        boolean result = false;
        Log.i(LOG_TAG, "[WFC]maybeShowCongratsPopup");
        if(ImsManager.isWfcEnabledByUser(context)
                && !(disconnectCause.getCode() == DisconnectCause.OTHER)
                && !(disconnectCause.getCode() == DisconnectCause.REJECTED)
                && !(disconnectCause.getCode() == DisconnectCause.MISSED)
                && !(disconnectCause.getCode() == DisconnectCause.ERROR)) {
            sContext = context;
            SharedPreferences  pref = PreferenceManager.getDefaultSharedPreferences(context);
            Log.i(LOG_TAG, "[WFC]maybeShowCongratsPopup pref.getBoolean(KEY_IS_FIRST_WIFI_CALL"
                    + pref.getBoolean(KEY_IS_FIRST_WIFI_CALL, true));
            Call call = CallList.getInstance().getActiveOrBackgroundCall();
            if (pref.getBoolean(KEY_IS_FIRST_WIFI_CALL, true)
                    && (call != null && call.hasProperty(android.telecom.Call.Details.PROPERTY_WIFI))) {
                showCongratsPopup();
                result = true;
            }
        }
        return result;
    }

    public static void showCongratsPopup() {
        int TIMER = 500;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Log.i(LOG_TAG, "[WFC]CongratsPopup shown");
                final Intent intent = new Intent(sContext, WfcDialogActivity.class);
                intent.putExtra(WfcDialogActivity.SHOW_CONGRATS_POPUP, true);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                sContext.startActivity(intent);
            }
        };
        mHandler.postDelayed(runnable, TIMER);
    }

    private static void onDialogDismissed() {
        if (sGeneralDialog != null) {
            sGeneralDialog.dismiss();
            sGeneralDialog = null;
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    public static class RoveOutReceiver extends WifiOffloadManager.Listener  {
        private Context mContext;
        private AlertDialog mDialog = null;
        private int mCount = 0;
        private Message mMsg = null;
        private static final int COUNT_TIMES = 3;
        private static final String LOG_TAG = "RoveOutReceiver";
        private static final int EVENT_RESET_TIMEOUT = 1;
        private static final int CALL_ROVE_OUT_TIMER = 1800000;
        private IWifiOffloadService mWfoService = null;

       public RoveOutReceiver(Context context) {
           mContext = context;
           IBinder b = ServiceManager.getService(WifiOffloadManager.WFO_SERVICE);
           mWfoService = IWifiOffloadService.Stub.asInterface(b);
           Log.d(LOG_TAG, "[WFC]RoveOutReceiver mWfoService" + mWfoService );
       }

       public void register(Context context) {
           if (mWfoService != null){
               try {
                   Log.d(LOG_TAG, "[WFC]onRoveOut register mWfoService");
                   mWfoService.registerForHandoverEvent(this);
               } catch (RemoteException e) {
                   Log.i(LOG_TAG, "RemoteException RoveOutReceiver()");
               }
           }
      }

       public void unregister(Context context) {
           if (mWfoService != null){
               try {
                   Log.d(LOG_TAG, "[WFC]onRoveOut unregister mWfoService ");
                   mWfoService.unregisterForHandoverEvent(this);
               } catch (RemoteException e) {
                   Log.i(LOG_TAG, "RemoteException RoveOutReceiver()");
               }
               WfcDialogActivity.sCount = 0;
               if (mMsg != null) {
                   mHandler.removeMessages(mMsg.what);
               }
           }
       }

       @Override
       public void onRoveOut(boolean roveOut, int rssi) {
           boolean wfcRoveOut = roveOut;
           Log.d(LOG_TAG, "[WFC]onRoveOut : " + wfcRoveOut);
           Call call = CallList.getInstance().getActiveOrBackgroundCall();
           if (wfcRoveOut) {
               if ((call != null && call.hasProperty(android.telecom.Call.Details.PROPERTY_WIFI))
                       && (WfcDialogActivity.sCount < COUNT_TIMES)
                       && !WfcDialogActivity.sIsShowing) {
                   final Intent intent1 = new Intent(mContext, WfcDialogActivity.class);
                   intent1.putExtra(WfcDialogActivity.SHOW_WFC_ROVE_OUT_POPUP, true);
                   intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                   mContext.startActivity(intent1);
                   if (WfcDialogActivity.sCount == 0) {
                       mMsg = mHandler.obtainMessage(EVENT_RESET_TIMEOUT);
                       mHandler.removeMessages(mMsg.what);
                       mHandler.sendMessageDelayed(mMsg, CALL_ROVE_OUT_TIMER);
                       Log.i(LOG_TAG, "[WFC]in WfcSignalReceiver sendMessageDelayed ");
                   }
               }
           }
       }

       private Handler mHandler = new Handler() {
           @Override
           public void handleMessage(Message msg) {
               switch (msg.what) {
                   case EVENT_RESET_TIMEOUT:
                       Log.i(LOG_TAG, "[WFC] in WfcSignalReceiver EVENT_RESET_TIMEOUT ");
                       WfcDialogActivity.sCount = 0;
                       break;
                   default:
                       Log.i(LOG_TAG, "[WFC]Message not expected: ");
                       break;
               }
           }
       };
  }
}
