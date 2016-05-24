package com.mediatek.incallui.ext;

import android.content.Context;
import android.view.View;

public interface IRCSeCallCardExt {
    /**
     * called when CallCard view created, based on CallCardFragment
     * lifecycle
     * @param context host context
     * @param rootView the CallCardFragment view
     * @internal
     */
    void onViewCreated(Context context, View rootView);

    /**
     * called when call state changed, based on onStateChange
     * @param call the call who was changed
     * @internal
     */
    void onStateChange(android.telecom.Call call);

    /**
     * called when voice record state changed
     * @param visible the record button visibility
     */
    void updateVoiceRecordIcon(boolean visible);

    /**
     * called when voice record state changed
     * @param callId the incallui call index
     * @param entry the ContactCacheEntry query completed
     */
    void onImageLoaded(String callId, Object entry);
}
