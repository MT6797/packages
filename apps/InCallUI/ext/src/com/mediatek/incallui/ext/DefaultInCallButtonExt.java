package com.mediatek.incallui.ext;

import android.util.Log;

public class DefaultInCallButtonExt implements IInCallButtonExt {
    private static final String TAG = "DefaulInCallButtonExt";

    /**
     * Checks if contact is video call capable through presence
     * @param number number to get video capability.
     * @return true if contact is video call capable.
     */
    public boolean isVideoCallCapable(String number) {
        Log.d(TAG, "isVideoCallCapable number:" + number );
        return false;
    }

}
