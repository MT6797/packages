package com.mediatek.dialer.ext;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import java.util.List;

public interface IDialPadExtension {

    /**
     * for OP09
     * called when DialPadFragment onCreate, plug-in should do init work here if needed
     * @param fragment dialpadFragment
     * @param action the actions host provided
     * @internal
     */
    void onCreate(Context context, Fragment fragment, DialpadExtensionAction action);

    /**
     * for OP09
     * called when handle special chars, plug-in should do customize chars handling here
     * @param context
     * @param input the input digits (text)
     * @return whether plug-in handled the chars
     * @internal
     */
    boolean handleChars(Context context, String input);

    /**
     * for OP09
     * called when dialpad create view, plug-in should customize view here
     * @param inflater the host inflater
     * @param container parent ViewGroup
     * @param savedState previous state saved for create view
     * @param resultView the host view created
     * @return customized view by plug-in
     * @internal
     */
    View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState,
            View resultView);

    /**
     * for OP09
     * called when dialpad destroy
     * @internal
     */
    void onDestroy();

    /**
     * for OP09
     * the dialpad hidden changed
     * @param scaleIn If true, scaleIn If false
     * @param delayMs delay time
     * @internal
     */
    void onHiddenChanged(boolean scaleIn, int delayMs);

    /**
     * for OP09
     * called when to show dialpadChooser
     * @param hidden true to be enabled
     * @internal
     */
    void showDialpadChooser(boolean enabled);

    /**
     * for OP03 OP09
     * called when dialpad fragment construct popup menu, plug-in should construct own menu here
     * @param popupMenu the created popupMenu object
     * @param anchorView the anchor for the menu to popup
     * @param menu the menu item of the popupmenu
     * @internal
     */
    void constructPopupMenu(PopupMenu popupMenu, View anchorView, Menu menu);

    /**
     * for op09
     * called when handle handle secret code in dialpad, plug-in should customize the input string
     * @param input the string to be handled
     * @return The result of the "input" be handled.
     * @internal
     */
    String handleSecretCode(String input);

    /**
     * for OP01
     * called when init option menu
     * @param activity the activity who call this func
     * @param menu the activity option menu
     * @internal
     */
    void buildOptionsMenu(Activity activity, Menu menu);

    /**
     * for OP01
     * called onViewCreated
     * @param activity the activity who call this func
     * @param view the dialpad view
     * @internal
     */
    void onViewCreated(Activity activity, View view);

    /**
     * for OP01
     * called when user input *#06#, single IMEI meaning
     * gemini project will only show one imei
     * @param List<String>, the IMEI input
     * @param List<String>, the IMEI output
     * @internal
     */
    List<String> getSingleIMEI(List<String> list);
}
