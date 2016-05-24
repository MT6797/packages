package com.mediatek.dialer.ext;

/**
 * for op01 plug-in can callback to host through this interface to do specific
 * things
 */
public interface ICallLogAction {
    void updateCallLogScreen();
    void processBackPressed();
}
