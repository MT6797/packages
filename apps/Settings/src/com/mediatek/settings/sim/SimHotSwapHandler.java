package com.mediatek.settings.sim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.internal.telephony.TelephonyIntents;

import java.util.Arrays;

public class SimHotSwapHandler {

    private static final String TAG = "SimHotSwapHandler";
    private SubscriptionManager mSubscriptionManager;
    private Context mContext;
    private int[] mSubscriptionIdListCache;
    private OnSimHotSwapListener mListener;
    private BroadcastReceiver mSubReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleHotSwap();
        }
    };

    public SimHotSwapHandler(Context context) {
        mContext = context;
        mSubscriptionManager = SubscriptionManager.from(context);
        mSubscriptionIdListCache = mSubscriptionManager.getActiveSubscriptionIdList();
        print("Cache list: ", mSubscriptionIdListCache);
    }

    public void registerOnSimHotSwap(OnSimHotSwapListener listener) {
        if (mContext != null) {
            mContext.registerReceiver(mSubReceiver, new IntentFilter(
                    TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED));
            mListener = listener;
        }
    }

    public void unregisterOnSimHotSwap() {
        if (mContext != null) {
            mContext.unregisterReceiver(mSubReceiver);
        }
        mListener = null;
    }

    private void handleHotSwap() {
        int[] subscriptionIdListCurrent = mSubscriptionManager.getActiveSubscriptionIdList();
        print("handleHotSwap, current subId list: ", subscriptionIdListCurrent);
        boolean isEqual = Arrays.equals(mSubscriptionIdListCache, subscriptionIdListCurrent);
        Log.d(TAG, "isEqual: " + isEqual);
        if (!isEqual && mListener != null) {
            mListener.onSimHotSwap();
        }
    }

    public interface OnSimHotSwapListener {
        void onSimHotSwap();
    }

    private void print(String msg, int[] lists) {
        if (lists != null) {
            for (int i : lists) {
                Log.d(TAG, msg + i);
            }
        } else {
            Log.d(TAG, msg + "is null");
        }
    }
}
