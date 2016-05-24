package com.mediatek.contacts.ext;

import android.util.Log;
import android.view.ViewGroup;

public class DefaultContactsCommonPresenceExtension implements IContactsCommonPresenceExtension {
    private static final String TAG = "DefaultContactsCommonPresenceExtension";
    /**
     * Checks if contact is video call capable
     * @param number number.
     * @return true if contact is video call capable,false if not capable.
     */
    public boolean isVideoCallCapable(String number){
        Log.d(TAG, "isVideoCallCapable number:" + number);
        return true;
    }

    /**
     * Checks if plugin is active.
     * @return true if op08 plugin active.
     */
    public boolean isActivePlugin(){
        Log.d(TAG, "isActivePlugin default implementation");
        return false;
    }

    /**
     * Checks if any number in contactId is video call capable,
     * if capable, add the view in contact list item.
     * @param contactId Contact Id.
     * @param viewGroup host view.
     */
    public void addVideoCallView(long contactId, ViewGroup viewGroup){
        Log.d(TAG, "addVideoCallView contactId:" + contactId);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // do-nothing
    }

    @Override
    public void onLayout(boolean changed, int leftBound, int topBound, int rightBound,
            int bottomBound) {
        // do-nothing
    }

    @Override
    public int getWidthWithPadding(){
        Log.d(TAG, "getWidthWithPadding");
        return 0;
    }
}
