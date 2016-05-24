package com.mediatek.contacts.ext;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

public interface ICtExtension {
    public interface StringName {
        String LOADING_SIM_CONTACTS = "msg_loading_sim_contacts_toast_ct";
        String LOSE_ACCOUNT_OR_STORAGE = "xport_error_one_account";
        String NOTIFIER_FAILURE_SIM_NOTREADY = "notifier_failure_sim_notready";
        String NOTIFIER_FAILURE_BY_SIM_FULL = "notifier_failure_by_sim_full";
    }

    /**
     * for op09 from old API:getEnhancementAccountSimIndicator() get the sim.
     *
     * @param res
     *            resource
     * @param subId
     *            simid
     * @param photoDrawable
     *            photo drawable object
     * @return get the Drawable for sim
     * @internal
     */
    Drawable getPhotoDrawableBySub(Resources res, int subId, Drawable photoDrawable);

    /**
     * for op09 load CT sim card icon bitmap from FW.
     *
     * @param res
     *            resource
     * @internal
     */
    void loadSimCardIconBitmap(Resources res);

    /**
     * for op09.
     *
     * @param defaultValue
     *            always icon res id
     * @return res id
     * @internal
     */
    int showAlwaysAskIndicate(int defaultValue);

}
