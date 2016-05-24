package com.mediatek.phone.ext;

import android.app.AlertDialog;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public interface IMobileNetworkSettingsExt {
    /**
     * called in onCreate() of the Activity
     * Plug-in can init itself, preparing for it's function
     * @param activity the MobileNetworkSettings activity
     * @param subId sub id
     * @internal
     */
    void initOtherMobileNetworkSettings(PreferenceActivity activity, int subId);

    /**
     * called in onCreate() of the Activity.
     * Plug-in can init itself, preparing for it's function
     * @param activity the MobileNetworkSettings activity
     * @param currentTab current Tab
     * @internal
     */
    void initMobileNetworkSettings(PreferenceActivity activity, int currentTab);

    /**
     * Attention, returning false means nothing but telling host to go on its own flow.
     * host would never return plug-in's "false" to the caller of onPreferenceTreeClick()
     *
     * @param preferenceScreen the clicked preference screen
     * @param preference the clicked preference
     * @return true if plug-in want to skip host flow. whether return true or false, host will
     * return true to its real caller.
     * @internal
     */
    boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference);

    /**
     * This interface is for updating the MobileNetworkSettings' item "Preferred network type"
     * @param preference there are two cases:
     *                   1. mButtonPreferredNetworkMode in host APP
     *                   2. mButtonEnabledNetworks in host APP
     * @internal
     */
    void updateNetworkTypeSummary(ListPreference preference);

    /**
     * TODO: Clear about what is this interface for
     * @param preference
     * @internal
     */
    void updateLTEModeStatus(ListPreference preference);

    /**
     * Allow Plug-in to customize the AlertDialog passed.
     * This API should be called right before builder.create().
     * Plug-in should check the preference to determine how the Dialog should act.
     * @param preference the clicked preference
     * @param builder the AlertDialog.Builder passed from host APP
     * @internal
     */
    void customizeAlertDialog(Preference preference, AlertDialog.Builder builder);


    /**
     * Update the ButtonPreferredNetworkMode's summary and enable when sim2 is CU card.
     * @param listPreference ButtonPreferredNetworkMode
     * @internal
     */
    void customizePreferredNetworkMode(ListPreference listPreference, int subId);

    /**
     * Preference Change, update network preference value and summary
     * @param preference the clicked preference
     * @param objValue choose obj value
     * @internal
     */
    void onPreferenceChange(Preference preference, Object objValue);

    /**
     * For Plug-in to update Preference.
     * @internal
     */
    void onResume();

    /**
     * For Plug-in to pause event and listener registration.
     * @internal
     */
    void unRegister();

    /**
     * for CT feature , CT Plug-in should return true.
     * @return true,if is CT Plug-in
     */
    boolean isCtPlugin();

    /**
     * for CT feature, CT Plug-in should return true.
     * @return true, if is CT test card
     */
    boolean useCTTestcard();

    /**
     * For changing string name in summary.
     * @param buttonEnabledNetworks list preference
     * @param networkMode network mode
     */
    void changeString(ListPreference buttonEnabledNetworks, int networkMode);

    /**
     * For changing entry names in list preference dialog box.
     * @param buttonEnabledNetworks list preference
     */
    void changeEntries(ListPreference buttonEnabledNetworks);

}
