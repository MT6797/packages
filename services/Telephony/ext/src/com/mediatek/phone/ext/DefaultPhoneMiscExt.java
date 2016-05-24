package com.mediatek.phone.ext;


public class DefaultPhoneMiscExt implements IPhoneMiscExt {

    @Override
    public boolean publishBinderDirectly() {
        return false;

    }

    /**
     * remove "Ask First" item index from call with selection list.
     *
     * @param String[] entryValues
     * @return entryValues after update object
     */
    @Override
    public String[] removeAskFirstFromSelectionListIndex(String[] entryValues) {
        return entryValues;
    }

    /**
     * remove "Ask First" item value from call with selection list.
     *
     * @param String[] entries
     * @return entries after remove object.
     */
    @Override
    public CharSequence[] removeAskFirstFromSelectionListValue(CharSequence[] entries) {
        return entries;
    }


    /**
     * For OP09 Set the selectedIndex to the first one When remove "Ask First".
     *
     * @param selectedIndex the default index
     * @return return the first index of phone account.
     */
    @Override
    public int getSelectedIndex(int selectedIndex) {
        return selectedIndex;
    }
}
