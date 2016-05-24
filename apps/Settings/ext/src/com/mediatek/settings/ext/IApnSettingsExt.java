package com.mediatek.settings.ext;

import java.util.ArrayList;

import android.R.integer;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.Menu;

public interface IApnSettingsExt {

    /**
     * Called when ApnSettings fragment or ApnEditor fragment onDestory
     * @internal
     */
    void onDestroy();

    /**
     * Called when ApnSettings created
     * @param pref
     * @internal
     */
    void initTetherField(PreferenceFragment pref);

    /**
     * whether allowed to edit the present apn, called at fill apn list,
     * in ApnSettings
     * @param type the apn type
     * @param apn name to query
     * @param numeric the mcc + mnc, such as "46000"
     * @param sourcetype
     * @return if the specified apn could be edited.
     * @internal
     */
     boolean isAllowEditPresetApn(String type, String apn, String numeric, int sourcetype);

    /**
     * customize Tethering APN setting, called at init Tether settings in TetherSettins
     * @param root through root PreferenceScreen, you can customize your preference.
     * @internal
     */
    void customizeTetherApnSettings(PreferenceScreen root);

    /**
     * Called when forge a query string for APN
     * @param where the original query string from host
     * @param mccmnc current mccmnc used in query string
     * @return the original string where, or a customized query string by plug-in
     * @internal
     */
    String getFillListQuery(String where, String mccmnc);

    /**
     * Called when ApnSettings update prefer carrier uri
     * @param subId which sub on updating
     * @return
     * @internal
     */
    Uri getPreferCarrierUri(Uri defaultUri, int subId);

    /**
     * Called when ApnSettings fragment onResume
     * @internal
     */
    void updateTetherState();

    /**
     * set APN type preference state (enable or disable), called at update UI in
     * ApnEditor
     * @param preference
     *            The preference to set state
     * @internal
     */
    void setApnTypePreferenceState(Preference preference, String apnType);

    /**
     * Called when ApnEditor insert a new APN item
     * @param defaultUri the Uri host used
     * @param context
     * @param intent
     * @return inserted Uri, return a customized one if necessary or just return the default Uri
     * @internal
     */
    Uri getUriFromIntent(Uri defaultUri, Context context, Intent intent);

    /**
     * Called when host forge the APN type array
     * @param defaultApnArray the default array host used
     * @param context
     * @param apnType
     * @return a customized array if necessary, or just return the defaultApnArray
     * @internal
     */
    String[] getApnTypeArray(String[] defaultApnArray, Context context, String apnType);

    /**
     * Judge the APN can be selected or not, called in ApnSettings.
     * @param type the apn type
     * @internal
     */
    boolean isSelectable(String type);

    /**
     * add option menu item called at onCreateOptionsMenu() in ApnSettings.
     * @param menu
     * @param menuId
     * @param numeric
     * @internal
     */
    void updateMenu(Menu menu, int newMenuId, int restoreMenuId, String numeric);

    /**
     * add intent extra for start ApnEditor.
     * @param it  the intent to start ApnEditor
     * @internal
     */
    void addApnTypeExtra(Intent it);

    /**
     * judge the screen if enable or disable.
     * @param subId sub id
     * @param activity for the screen
     * @return true screen should enable.
     * @return false screen should disable.
     * @internal
     */
    boolean getScreenEnableState(int subId, Activity activity);

    /**
     * Called when host update the screen state
     * @param subId
     * @param sourceType
     * @param root
     * @internal
     */
    void updateFieldsStatus(int subId, int sourceType, PreferenceScreen root);

    /**
     * set the preference text and summary according to the slotId.
     * called at update UI, in ApnEditor.
     * @param subId sub id.
     * @param text the text for the preference
     * @internal
     */
    void setPreferenceTextAndSummary(int subId, String text);

    /**
     * customize Preference according to the slotId. called at onCreate once.
     * in ApnEditor.
     * @param subId sub id
     * @param root though the root PreferenceScreen , you can customize any preference
     * @internal
     */
     void customizePreference(int subId, PreferenceScreen root);

     /**
      * customize apn projection, such as add Telephony.Carriers.PPP
      * Called at onCreate in ApnEditor.
      * @param projection: the default source String[]
      * @return customized projection
      * @internal
      */
     String[] customizeApnProjection(String[] projection);

     /**
      * save the added apn values called when save the added apn vaule,
      * in ApnEditor.
      * @param contentValues the default content vaules
      * @internal
      */
     void saveApnValues(ContentValues contentValues);

   /**
    * for CT feature ,update CTWAP,CTNET display.
    * @param name
    * @param source type
    * @internal
    */
    public String updateApnName(String name, int sourcetype);

    /**
     * Called when OmacpApnReceiver update APN
     * @param defaultReplacedNum
     * @param context
     * @param uri
     * @param apn
     * @param name
     * @param values
     * @param numeric
     * @return
     * @internal
     */
    long replaceApn(long defaultReplaceNum, Context context, Uri uri, String apn, String name,
            ContentValues values, String numeric);

    /**
     * for CT, clear the unselectable APN.
     *
     * @param mnoApnList the unselectable mno apn list
     * @param mvnoApnList the unselectable mvno apn list
     * @param subId
     * @internal
     */
    void customizeUnselectableApn(ArrayList<Preference> mnoApnList,
            ArrayList<Preference> mvnoApnList, int subId);

    /**
     * set MVNO preference state (enable or disable), called at
     * update UI in ApnEditor
     * @param preference The preference to set state
     * @internal
     */
    void setMvnoPreferenceState(Preference mvnoType, Preference mvnoMatchData);

    /**
     * for CU feature ,if sort APN by the name.
     * @param order sort by the name.
     * @return the sort string.
     */
    public String getApnSortOrder(String order);
}
