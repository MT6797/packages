package com.mediatek.incallui.ext;

public interface IStatusBarExt {
    /**
     * Show status bar hd icon when the call have property of HIGH_DEF_AUDIO.
     * Plugin need to use call capability to show or dismiss statuar bar icon.
     *
     * @param obj    the incallui call
     */
    void updateInCallNotification(Object obj);
}