package com.mediatek.incallui.ext;

import android.content.Context;
import android.view.View;


public class DefaultRCSeCallCardExt implements IRCSeCallCardExt {
    @Override
    public void onViewCreated(Context context, View rootView) {
        // do nothing
    }

    @Override
    public void onStateChange(android.telecom.Call call) {
        // do nothing
    }

    @Override
    public void updateVoiceRecordIcon(boolean visible) {
        // do nothing
    }

    @Override
    public void onImageLoaded(String callId, Object entry) {
        //do nothing
    }

}
