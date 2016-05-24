package com.mediatek.incallui.ext;

import android.telecom.Call;

/**
 * Default implementation for IVideoCallExt.
 */
public class DefaultVideoCallExt implements IVideoCallExt {
    @Override
    public void onCallSessionEvent(Call telecomCall, int event) {
        // do nothing.
    }
}
