package com.mediatek.dialer.ext;

import android.content.Context;
import android.telecom.PhoneAccountHandle;
import android.view.View;

public interface IDialerSearchExtension {

    /**
     * Remove call account info if it exists in contact item
     * @param Context context
     * @param View view
     * @param PhoneAccountHandle phoneAccountHandle
     * @internal
     */
    public void setCallAccountForDialerSearch(Context context, View view,
            PhoneAccountHandle phoneAccountHandle);

    /**
     * for OP09
     * @param Context context
     * @param View view
     * @internal
     */
    public void removeCallAccountForDialerSearch(Context context, View view);
}
