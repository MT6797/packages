package com.mediatek.dialer.calllog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mediatek.telecom.TelecomManagerEx;

/**
 * Listening phone account changed, notify listeners added in PhoneAccountInfoHelper
 */
public class PhoneAccountChangedReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (TelecomManagerEx.ACTION_PHONE_ACCOUNT_CHANGED.equals(action)) {
            PhoneAccountInfoHelper.getInstance(context).notifyAccountInfoUpdate();
        }
    }

}
