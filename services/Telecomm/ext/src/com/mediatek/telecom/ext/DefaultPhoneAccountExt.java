package com.mediatek.telecom.ext;

import android.telecom.PhoneAccountHandle;

import java.util.List;

public class DefaultPhoneAccountExt implements IPhoneAccountExt {

    /**
     * should remove the default MO phone account.
     *
     * @param accountHandleList
     *            capable account list
     * @return true if need to remove.
     */
    @Override
    public boolean shouldRemoveDefaultPhoneAccount(List<PhoneAccountHandle> accountHandleList) {
        return true;
    }

}
