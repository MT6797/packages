package com.mediatek.dialer.dialersearch;

import com.android.dialer.dialpad.LatinSmartDialMap;

/**
 *  [MTK Dialer Search] extends the dialpad character which is support
 *  by mtk dialer search and can be entered from the dialpad
 *  */

public class SmartDialPadMap extends LatinSmartDialMap {

    @Override
    public boolean isValidDialpadAlphabeticChar(char ch) {
        return (super.isValidDialpadAlphabeticChar(ch) || (ch >= 'A' && ch <= 'Z'));
    }

    @Override
    public boolean isValidDialpadNumericChar(char ch) {
        return (super.isValidDialpadNumericChar(ch) || ch == '*' || ch == '#' || ch == '+'
                || ch == ',' || ch == ';');
    }

}
