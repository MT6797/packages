package com.mediatek.contacts.ext;

import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.widget.EditText;

public class DefaultOp01Extension implements IOp01Extension {
    //---------------------for Editor-------------------//
    /**
     * OP01 will filter phone number.
     * let the op01 set its own listener for phone number input.
     * set the key listener
     * @param fieldView the view to set listener
     */
    @Override
    public void setViewKeyListener(EditText fieldView){
        //do-nothing
    }

    //-----------------------------for Multi Choise------------------//
    /**
     * for op01 Host max count is 3500,OP01 will custom to 5000
     * ContactsMultiDeletionFragment.java
     * @return the max count in multi choice list
     */
    @Override
    public int getMultiChoiceLimitCount(int defaultCount) {
      //default return defaultCount
        return defaultCount;
    }

    //--------------for PeopleActivity----------------//
    /**
     * for op01,add for "show sim Capacity" in people list
     * @param menu The menu to be add options, context Host context
     */
    @Override
    public void addOptionsMenu(Context context, Menu menu){
      //do-nothing
    }

    //--------------for SIMImportProcessor----------------//
    /**
     * Op01 will format Number, filter some char
     * @param number to be filter
     * @param bundle is intent data
     */
    @Override
    public String formatNumber(String defaultNumber, Bundle bundle) {
        //default return defaultNumber
        return defaultNumber;
    }

    //--------------for blacklist----------------//
    /**
     * Op01 will will show blacklist action item even though no contact
     * @param providerStatus contact status
     */
    @Override
    public boolean areContactAvailable(Integer providerStatus) {
        return false;
    }

    //--------------for QuickContact----------------//
    /**
     * Op01 will show auto reject icon if recent data contains auto reject call
     * @param type call type
     * @param callArrowIcon is calltype icon
     */
    @Override
    public Drawable getArrowIcon(int type, Drawable callArrowIcon) {
        return callArrowIcon;
    }

    //--------------for add group----------------//
    /**
     * Op01 will allow user add contact by group
     * @param context application context
     * @param menu The menu to be add options
     * @param fragment current fragment
     */
    @Override
    public void addGroupMenu(final Context context, Menu menu, Fragment fragment){
        return;
    }
}
