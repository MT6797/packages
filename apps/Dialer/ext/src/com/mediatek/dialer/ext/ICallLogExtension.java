package com.mediatek.dialer.ext;

import java.util.List;
import java.util.HashMap;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.drawable.Drawable;
import android.support.v13.app.FragmentPagerAdapter;
import android.telecom.PhoneAccountHandle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ListView;


import com.android.dialer.calllog.ContactInfo;


public interface ICallLogExtension {

    /**
     * for OP09
     * set account for call log list
     *
     * @param Context context
     * @param View view
     * @param PhoneAccountHandle phoneAccountHandle
     * @internal
     */
    public void setCallAccountForCallLogList(Context context, View view,
             PhoneAccountHandle phoneAccountHandle);

    /**
     * for op01
     * called when host create menu, to add plug-in own menu here
     * @param menu
     * @param tabs the ViewPagerTabs used in activity
     * @param callLogAction callback plug-in need if things need to be done by host
     * @internal
     */
    void createCallLogMenu(Activity activity, Menu menu, HorizontalScrollView tabs,
            ICallLogAction callLogAction);

    /**
     * for op01
     * called when host prepare menu, prepare plug-in own menu here
     * @param activity the current activity
     * @param menu the Menu Created
     * @param fragment the current fragment
     * @param itemDeleteAll the optionsmenu delete all item
     * @param adapterCount adapterCount
     * @internal
     */
    public void prepareCallLogMenu(Activity activity, Menu menu,
            Fragment fragment, MenuItem itemDeleteAll, int adapterCount);

    /**
     * for op01
     * called when call log query, plug-in should customize own query here
     * @param typeFiler current query type
     * @param builder the query selection Stringbuilder, modify to change query selection
     * @param selectionArgs the query selection args, modify to change query selection
     * @internal
     */
    void appendQuerySelection(int typeFiler, StringBuilder builder, List<String> selectionArgs);

    /**
     * for op01
     * called when home button in actionbar clicked
     * @param activity the current activity
     * @param pagerAdapter the view pager adapter used in activity
     * @param menu the optionsmenu itmes
     * @return true if do not need further operation in host
     * @internal
     */
    boolean onHomeButtonClick(Activity activity, FragmentPagerAdapter pagerAdapter, MenuItem menu);

    /**
     * for op01
     * Called when calllog activity onBackPressed
     * @param activity the current activity
     * @param pagerAdapter the view pager adapter used in activity
     * @param callLogAction callback plug-in need if things need to be done by host
     * @internal
     */
    void onBackPressed(Activity activity, FragmentPagerAdapter pagerAdapter,
            ICallLogAction callLogAction);

    /**
     * for op01
     * called when updating tab count
     * @param activity the current activity
     * @param count count
     * @return tab count
     * @internal
     */
    public int getTabCount(Activity activity, int count);

    /**
     * for op01
     * @param context the current context
     * @param pagerAdapter the view pager adapter used in activity
     * @param tabs the ViewPagerTabs used in activity
     * @internal
     */
    void restoreFragments(Context context,
            FragmentPagerAdapter pagerAdapter, HorizontalScrollView tabs);

    /**
     * for op01
     * @param activity the current activity
     * @param outState save state
     * @internal
     */
    void onSaveInstanceState(Activity activity, Bundle outState);

    /**.
     * for op01
     * plug-in set position
     * @param position to set
     * @internal
     */
    public void setPosition(int position);

    /**.
     * for op01
     * plug-in modify current position
     * @param position position
     * @return get the position
     * @internal
     */
    public int getPosition(int position);

    /**.
     * for op01
     * plug-in manage the state and unregister receiver
     * @param activity the current activity
     * @internal
     */
    public void onDestroy(Activity activity);

    /**.
     * for op01
     * plug-in init the reject mode in the host
     * @param activity the current activity
     * @param bundle bundle
     * @internal
     */
    public void onCreate(Activity activity, Bundle bundle);

    /**.
     * for op01
     * plug-in reset the reject mode in the host
     * @param activity the current activity
     * @internal
     */
    public void resetRejectMode(Activity activity);

    /**.
     * for op01
     * plug-in get the auto reject icon
     * @return return the auto reject icon
     * @internal
     */
    public Drawable getAutoRejectDrawable();

    /**
     * for op01.
     * plug-in whether is auto reject mode
     * @return call log show state
     * @internal
     */
    public boolean isAutoRejectMode();

    /**
     * for op01.
     * plug-in insert auto reject icon resource for dialer search
     * @param callTypeDrawable callTypeDrawable
     * @internal
     */
    public void addResourceForDialerSearch(HashMap<Integer, Drawable>
            callTypeDrawable);

    /**
     * for op09.
     * plug-in whether show sim label account or not
     * @return Account null or not
     */
    public boolean shouldReturnAccountNull();

    /**
     * for op01.
     * plug-in always show video call back button
     * @return true in op01
     */
    public boolean showVideoForAllCallLog();

}
