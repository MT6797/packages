package com.mediatek.dialer.activities;

import android.content.Context;
import android.telecom.TelecomManager;

import com.android.contacts.common.activity.TransactionSafeActivity;
import com.google.common.annotations.VisibleForTesting;

/**
 * Class for injecting some mocked system service, such Telecom, Telephony
 */
public abstract class NeedTestActivity extends TransactionSafeActivity {
    // use to override system real service
    private TelecomManager mTelecomManager;

    @Override
    public Object getSystemService(String name) {
        if (Context.TELECOM_SERVICE.equals(name) && mTelecomManager != null) {
            return mTelecomManager;
        }
        return super.getSystemService(name);
    }

    @VisibleForTesting
    public void setTelecomManager(TelecomManager telecom) {
        mTelecomManager = telecom;
    };
}
