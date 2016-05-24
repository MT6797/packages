package com.mediatek.incallui.ext;

import android.content.Context;
import android.graphics.drawable.RippleDrawable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.HashMap;


public class DefaultRCSeCallButtonExt implements IRCSeCallButtonExt {
    @Override
    public void onViewCreated(Context context, View rootView) {
        // do nothing
    }

    @Override
    public void onStateChange(android.telecom.Call call, HashMap<String, android.telecom.Call> callMap) {
        // do nothing
    }

    @Override
    public boolean handleMenuItemClick(MenuItem menuItem) {
        return false;
    }

    @Override
    public void configureOverflowMenu(Context context, Menu menu) {
        // do nothing
    }

    @Override
    public void updateNormalBgDrawable(RippleDrawable rpDrawable) {
        // do nothing
    }
}
