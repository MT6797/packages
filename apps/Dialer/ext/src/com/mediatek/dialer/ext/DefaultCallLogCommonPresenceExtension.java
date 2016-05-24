package com.mediatek.dialer.ext;


import android.content.Context;
import android.util.Log;

public class DefaultCallLogCommonPresenceExtension implements ICallLogCommonPresenceExtension {
    private static final String TAG = "DefaultCallLogCommonPresenceExtension";

    /**
     * Checks if contact is video call capable
     * @param number number to get video capability.
     * @param isAnonymous is number saved in contact list.
     * @return true if contact is video call capable.
     */
    public boolean isVideoCallCapable(String number, boolean isAnonymous) {
        Log.d(TAG, "isVideoCallCapable number:" + number +
                ", isAnon:" + isAnonymous);
        return true;
    }

}
