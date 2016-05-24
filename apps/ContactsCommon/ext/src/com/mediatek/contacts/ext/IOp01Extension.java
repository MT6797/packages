package com.mediatek.contacts.ext;

import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.widget.EditText;

/**
 * for op01 plugin
 */
public interface IOp01Extension {
    // ---------------------for Editor-------------------//
    /**
     * OP01 will filter phone number. let the op01 set its own listener for
     * phone number input. set the key listener
     *
     * @param fieldView
     *            the view to set listener
     * @internal
     */
    void setViewKeyListener(EditText fieldView);

    // -----------------------------for Multi Choise------------------//
    /**
     * for op01 Host max count is 3500,OP01 will custom to 5000
     * ContactsMultiDeletionFragment.java
     *
     * @return the max count in multi choice list
     * @internal
     */
    int getMultiChoiceLimitCount(int defaultCount);

    // --------------for PeopleActivity----------------//
    /**
     * for op01,add for "show sim Capacity" in people list
     *
     * @param menu
     *            The menu to be add options, context Host context
     * @internal
     */
    void addOptionsMenu(Context context, Menu menu);

    // --------------for SIMImportProcessor----------------//
    /**
     * Op01 will format Number, filter some char
     *
     * @param number
     *            to be filter
     * @param bundle
     *            is intent data
     * @internal
     */
    String formatNumber(String number, Bundle bundle);

    //--------------for blacklist----------------//
    /**
     * Op01 will will show blacklist action item even though no contact
     * @param providerStatus contact status
     * @internal
     */
    boolean areContactAvailable(Integer providerStatus);

    //--------------for QuickContact----------------//
    /**
     * Op01 will show auto reject icon if recent data contains auto reject call
     * @param type call type
     * @param callArrowIcon is calltype icon
     * @internal
     */
    Drawable getArrowIcon(int type, Drawable callArrowIcon);

    //--------------for add group----------------//
    /**
     * Op01 will allow user add contact by group
     * @param context application context
     * @param menu The menu to be add options
     * @param fragment current fragment
     */
    void addGroupMenu(final Context context, Menu menu, Fragment fragment);
}
