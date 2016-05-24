package com.mediatek.phone.ext;

import android.app.Activity;

/**
 * Telecom account registry extension plugin for op09.
 */
public interface IEmergencyDialerExt {

    /**
     * Called when need oncreate dial buttons.
     *
     * @param activity need to update.
     * @internal
     */
    void onCreate(Activity activity);

    /**
     * Called when destroy emergency dialer.
     *
     * @param activity need to update
     * @internal
     */
    void onDestroy(Activity activity);
}