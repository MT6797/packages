package com.mediatek.incallui.ext;

import android.app.Fragment;


/**
 * Interface for overall InCallUI. most of them are MISC interfaces.
 */
public interface IInCallExt {

    /**
     * Hint. When show the "No SIM or SIM error" alert dialog, plugin can replace
     * the error string.
     */
    public static final String HINT_ERROR_MSG_SIM_ERROR = "hint_error_sim_error";
    /**
     * Hint. When show account dialog, whether to show the "set account as default"
     * check box or not. If plugin decide to show the check box, replace default value
     * with true, otherwise replace with false.
     */
    public static final String HINT_BOOLEAN_SHOW_ACCOUNT_DIALOG
            = "hint_boolean_show_account_dialog";

    /**
     * Called when need to replace error message by hint string.
     * FIXME: should replaced by #replaceValue
     *
     * @param defaultString the default text of host.
     * @param hint the hint for plugin about where it is called.
     * @return new String.
     * @deprecated use #replaceValue instead.
     */
    @Deprecated
    String replaceString(String defaultString, String hint);

    /**
     * customize SelectPhoneAccountDialogFragment.
     * @param fragment SelectPhoneAccountDialogFragment
     */
    void customizeSelectPhoneAccountDialog(Fragment fragment);
}
