package com.mediatek.dialer.ext;

import android.content.Context;

public interface ICallLogCommonPresenceExtension {

    /**
     * Checks if contact is video call capable
     * @param number number to get video capability.
     * @param isAnonymous is number saved in contact list.
     * @return true if contact is video call capable.
     */
    boolean isVideoCallCapable(String number, boolean isAnonymous);

}
