package com.mediatek.phone.ext;

import android.os.Message;
import com.android.internal.telephony.PhoneProxy;

public class DefaultIncomingCallExt implements IIncomingCallExt {
    @Override
    public boolean handlePhoneEvent(Message msg, PhoneProxy phone) {
        return false;
    }

    /**
     * change the disconnect cause when user reject a call.
     * @param disconnectCause disconnectCause
     * @return disconnectCause after modified
     */
    @Override
    public int changeDisconnectCause(int disconnectCause) {
        return disconnectCause;
    }
}

