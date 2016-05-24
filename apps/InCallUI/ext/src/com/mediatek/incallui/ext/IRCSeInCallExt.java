package com.mediatek.incallui.ext;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public interface IRCSeInCallExt {

    /**
     * called when onCreate(), notify plugin to do initialization.
     * @param icicle the Bundle InCallActivity got
     * @param inCallActivity the InCallActivity instance
     * @param IInCallScreenExt the call back interface for UI updating
     * @internal
     */
    void onCreate(Bundle icicle, Activity inCallActivity, IInCallScreenExt iInCallScreenExt);

    /**
      * called when onNewIntent(), notify plugin activity may reenter
      * @param intent
      */
    public void onNewIntent(Intent intent);

    /**
     * called when onDestroy()
     * @param inCallActivity the InCallActivity instance
     * @internal
     */
    void onDestroy(Activity inCallActivity);

     /**
     * This is invoked to plugin when get result from request permissions
     * @param requestCode Request Code of the permission requested
     * @param permissions List of permissions
     * @param grantResults results of all permissions requested
     */
    public void onRCSeRequestPermissionsResult(
            final int requestCode, final String permissions[], final int[] grantResults);
}
