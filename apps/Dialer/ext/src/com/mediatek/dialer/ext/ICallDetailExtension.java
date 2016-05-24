package com.mediatek.dialer.ext;

import android.content.Context;
import android.telecom.PhoneAccountHandle;
import android.view.Menu;
import android.widget.TextView;

public interface ICallDetailExtension {

    /**
     * for op01
     * called when updating call list, plug-in should customize the duration view if needed
     * @param durationView the duration text
     * @internal
     */
    public void setDurationViewVisibility(TextView durationView);

    /**
     * for op01,add for "blacklist" in call detail.
     * @param menu blacklist menu.
     * @param number phone number.
     * @param name contact name.
     * @internal
     */
    public void onPrepareOptionsMenu(Context context, Menu menu, CharSequence number,
            CharSequence name);

    /**
     * for OP09, add sim indicator in call detail.
     * @param context context
     * @param phoneAccountHandle phoneAccountHandle
     * @internal
     */
    public void setCallAccountForCallDetail(Context context,
            PhoneAccountHandle phoneAccountHandle);

    /**
     * for OP01, change call type text.
     * @param context context
     * @param callTypeTextView callTypeTextView
     * @param isVideoCall isVideoCall
     * @param callType callType
     */
    public void changeVideoTypeText(Context context, TextView callTypeTextView,
            boolean isVideoCall, int callType);
}
