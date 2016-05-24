package com.mediatek.settings.ext;

import android.content.Context;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

/**
 * Interface for WFC Settings plug-in.
 */
public interface IWfcSettingsExt {

    /** Initialize plug-in with essential values.
     * @param pf preferenceFragment
     * @return
     * @internal
     */
    void initPlugin(PreferenceFragment pf);

    /** get operator specific customized summary for WFC button.
     * Used in WirelessSettings
     * @param context context
     * @param defaultSummaryResId default summary res id
     * @return res id of summary to be displayed
     * @internal
     */
    String getWfcSummary(Context context, int defaultSummaryResId);

    /** Called on events like onResume/onPause etc from WirelessSettings.
    * @param event resume/pause etc.
    * @return
    * @internal
    */
    void onWirelessSettingsEvent(int event);

    /** Called on events like onResume/onPause etc from WfcSettings.
    * @param event resume/pause etc.
    * @return
    * @internal
    */
    void onWfcSettingsEvent(int event);

    /** Add other WFC preference, if any.
    * @param
    * @return
    * @internal
    */
    void addOtherCustomPreference();

    /** Takes required action on wfc list preference on switch change.
     * @param root preference screen
     * @param wfcModePref AOSP wfcMode preference
     * @param wfcEnabled whether switch on/off
     * @param wfcMode current wfc mode
     * @return
     * @internal
     */
    void updateWfcModePreference(PreferenceScreen root, ListPreference wfcModePref,
            boolean wfcEnabled, int wfcMode);

    /** Shows alert dialog to confirm whether to turn on hotspot or not.
     * @param  context context
     * @return  true: if alert is shown, false: if alert is not shown
     */
    boolean showWfcTetheringAlertDialog(Context context);

    /** Customize the WFC settings preference.
     * Used in Wireless Settings
     *  @param  context context
     *  @param  preferenceScreen preferenceScreen
     */
    void customizedWfcPreference(Context context, PreferenceScreen preferenceScreen);

}
