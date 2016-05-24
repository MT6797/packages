package com.mediatek.incallui.ext;

public class DefaultStatusBarExt implements IStatusBarExt {
    /**
     * Show status bar hd icon when the call have property of HIGH_DEF_AUDIO.
     * Plugin need to use call capability to show or dismiss statuar bar icon.
     *
     * @param obj    the incallui call
     */
    @Override
    public void updateInCallNotification(Object obj) {
        // do nothing.
    }
}