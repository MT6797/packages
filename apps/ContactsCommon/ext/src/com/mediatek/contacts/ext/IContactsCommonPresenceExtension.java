package com.mediatek.contacts.ext;

import android.content.Context;
import android.view.ViewGroup;

public interface IContactsCommonPresenceExtension {
    /**
     * Checks if contact is video call capable.
     * @param number number.
     * @return true if contact is video call capable.
     */
    boolean isVideoCallCapable(String number);

    /**
     * Checks if plugin is active.
     * @return true if op08 plugin active.
     */
    boolean isActivePlugin();

    /**
     * Checks if any number in contactId is video call capable,
     * if capable, add the view in contact list item.
     * @param contactId Contact Id.
     * @param viewGroup host view.
     */
    void addVideoCallView(long contactId, ViewGroup viewGroup);
    /**
    * @param widthMeasureSpec
    * @param heightMeasureSpec
    */
   void onMeasure(int widthMeasureSpec, int heightMeasureSpec);

   /**
    * @param changed
    * @param leftBound
    * @param topBound
    * @param rightBound
    * @param bottomBound
    */
   void onLayout(boolean changed, int leftBound, int topBound, int rightBound,
           int bottomBound);
   /**
    * getWidthWithPadding.
    * @return padding width
    */
    int getWidthWithPadding();
}
