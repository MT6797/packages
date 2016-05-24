package com.mediatek.settings.sim;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PhoneServiceStateHandler {

    private static final String TAG = "PhoneServiceStateHandler";
    private int[] mSubs;
    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;
    private Listener mListenerCallBack;
    private Map<Integer, PhoneStateListener> mListeners =
            new ConcurrentHashMap<Integer, PhoneStateListener>();

    /**
     * listen all selectable subInfos.
     * @param context context
     */
    public PhoneServiceStateHandler(Context context) {
        mTelephonyManager = TelephonyManager.from(context);
        mSubscriptionManager = SubscriptionManager.from(context);
        mSubs = mSubscriptionManager.getActiveSubscriptionIdList();
    }

    public void registerOnPhoneServiceStateChange(Listener listener) {
        mListenerCallBack = listener;
        registerPhoneStateListener();
    }

    public void unregisterOnPhoneServiceStateChange() {
        mListenerCallBack = null;
        unregisterPhoneStateListener();
    }

    private void registerPhoneStateListener() {
        for (Integer subId : mSubs) {
            registerPhoneStateListener(subId);
        }
     }

    private void registerPhoneStateListener(int subId) {
        Log.d(TAG, "Register PhoneStateListener, subId : " +  subId);
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            PhoneStateListener phoneStateListener = getPhoneStateListener(subId);
            mListeners.put(subId, phoneStateListener);
            mTelephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        } else {
            Log.d(TAG, "invalid subId: " + subId);
        }
     }

    private PhoneStateListener getPhoneStateListener(final int subId) {
         return new PhoneStateListener(subId) {
             @Override
             public void onServiceStateChanged(ServiceState state) {
                 Log.d(TAG, "PhoneStateListener:onServiceStateChanged: subId: " + subId
                         + ", state: " + state);
                 if (mListenerCallBack != null) {
                     mListenerCallBack.onServiceStateChanged(state, subId);
                }
             }
         };
     }

    private void unregisterPhoneStateListener() {
        for (int subId : mListeners.keySet()) {
            unregisterPhoneStateListener(subId);
        }
     }

    private void unregisterPhoneStateListener(int subId) {
         Log.d(TAG, "Register unregisterPhoneStateListener subId : " + subId);
         if (SubscriptionManager.isValidSubscriptionId(subId)) {
             mTelephonyManager.listen(mListeners.get(subId), PhoneStateListener.LISTEN_NONE);
             mListeners.remove(subId);
         } else {
             Log.d(TAG, "invalid subId: " + subId);
        }
    }

     public interface Listener {
         public void onServiceStateChanged(ServiceState state, int subId);
     }
}
