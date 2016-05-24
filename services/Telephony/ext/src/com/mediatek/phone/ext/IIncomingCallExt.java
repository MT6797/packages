package com.mediatek.phone.ext;

import android.os.Message;
import com.android.internal.telephony.PhoneProxy;

public interface IIncomingCallExt {
    /**
     * called when receive incoming call event, for OP01 call rejection
     * @param msg
     * @param phone
     * @internal
     */
    public boolean handlePhoneEvent(Message msg, PhoneProxy phone);

    /**
     * change the disconnect cause when user reject a call.
     * @param disconnectCause disconnectCause
     * @return disconnectCause after modified
     */
    public int changeDisconnectCause(int disconnectCause);
}
