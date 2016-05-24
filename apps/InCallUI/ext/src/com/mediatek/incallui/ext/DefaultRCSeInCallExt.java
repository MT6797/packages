package com.mediatek.incallui.ext;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class DefaultRCSeInCallExt implements IRCSeInCallExt {
    @Override
    public void onCreate(Bundle icicle, Activity inCallActivity, IInCallScreenExt iInCallScreenExt) {
        // do nothing
    }

    @Override
    public void onNewIntent(Intent intent) {
        // do nothing
    }

    @Override
    public void onDestroy(Activity inCallActivity) {
        // do nothing
    }

    @Override
    public void onRCSeRequestPermissionsResult(
            final int requestCode, final String permissions[], final int[] grantResults) {
        // do nothing
    }
}
