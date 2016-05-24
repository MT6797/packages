package com.mediatek.incallui.ext;

public interface IInCallButtonExt {

    /**
     * Checks if contact is video call capable through presence
     * @param number number to get video capability.
     * @return true if contact is video call capable.
     */
    boolean isVideoCallCapable(String number);

}
