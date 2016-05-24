package com.mediatek.dialer.ext;

import android.content.Intent;

/**
 * for OP09 the dialpad action host provided
 */
public interface DialpadExtensionAction {
    void doCallOptionHandle(Intent intent);
    void handleDialButtonClickWithEmptyDigits();
}
