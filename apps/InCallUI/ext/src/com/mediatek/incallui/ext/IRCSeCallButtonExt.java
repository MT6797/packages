package com.mediatek.incallui.ext;

import android.content.Context;
import android.graphics.drawable.RippleDrawable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.HashMap;


public interface IRCSeCallButtonExt {
    /**
     * called when CallButtonFragment view created.
     * customize this view
     * @param context host Context
     * @param rootView the CallButtonFragment view
     * @internal
     */
    void onViewCreated(Context context, View rootView);

    /**
     * called when call state changed
     * notify the foreground call to plug-in
     * @param call current foreground call
     * @param callMap a mapping of callId -> call for all current calls
     * @internal
     */
    void onStateChange(android.telecom.Call call, HashMap<String, android.telecom.Call> callMap);

    /**
     * called when popup menu item in CallButtonFragment clicked.
     * involved popup menus such as audio mode, vt
     * @param menuItem the clicked menu item
     * @return true if this menu event has already handled by plugin
     * @internal
     */
    boolean handleMenuItemClick(MenuItem menuItem);

    /**
     * TODO: [M migration]should find somewhere to add it back.
     * called when configure overflow menu item in CallButtonFragment.
     * @param context the Activity context
     * @param menu the Activity overflow menu
     */
    void configureOverflowMenu(Context context, Menu menu);

    /**
     * called when update backgroung drawable of callbutton in callbutton fragment.
     * Plugin should sync the color of message button with host button color
     * @param rpDrawable the background drawable need to changed to
     */
    void updateNormalBgDrawable(RippleDrawable rpDrawable);
}
