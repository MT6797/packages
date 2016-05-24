package com.mediatek.incallui.ext;

import android.app.Fragment;


/**
 * Default implementation for IICallExt.
 */
public class DefaultInCallExt implements IInCallExt {

    @Override
    public String replaceString(String defaultString, String hint) {
        return defaultString;
    }

    @Override
    public void customizeSelectPhoneAccountDialog(Fragment fragment) {
    }
}
