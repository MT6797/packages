package com.mediatek.incallui.ext;

import android.telecom.Call;

/**
 * Plugin APIs for video call.
 */
public interface IVideoCallExt {
    void onCallSessionEvent(Call telecomCall, int event);
}
