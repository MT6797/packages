package com.mediatek.phone.ext;

public interface IPhoneMiscExt {

    /**
     * called in NetworkQueryService.onBind()
     * google default behavior defined a LocalBinder to prevent  NetworkQueryService
     * being accessed by outside components.
     * but, there is a situation that plug-in need the mBinder. LocalBinder can't be
     * accessed by plug-in.
     * it would be risk if plug-in really returns true directly without any security check.
     * if this happen, other 3rd party component can access this binder, too.
     *
     * @return true if Plug-in need to get the binder
     * @internal
     */
    boolean publishBinderDirectly();


    /**
     * remove "Ask First" item index from call with selection list.
     *
     * @param String[] entryValues
     * @return entryValues after update object
     * @internal
     */
    String[] removeAskFirstFromSelectionListIndex(String[] entryValues);

    /**
     * remove "Ask First" item value from call with selection list.
     *
     * @param String[] entries
     * @return entries after remove object.
     * @internal
     */
    CharSequence[] removeAskFirstFromSelectionListValue(CharSequence[] entries);

    /**
     * For OP09 Set the selectedIndex to the first one When remove "Ask First".
     *
     * @param selectedIndex the default index
     * @return return the first index of phone account.
     * @internal
     */
    public int getSelectedIndex(int selectedIndex);

}
