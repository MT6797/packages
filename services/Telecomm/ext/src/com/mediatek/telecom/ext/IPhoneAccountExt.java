package com.mediatek.telecom.ext;

import android.telecom.PhoneAccountHandle;

import java.util.List;

public interface IPhoneAccountExt {

    /**
     * should remove the default MO phone account.
     *
     * @param accountHandleList
     *            a list of all Phone accounts.
     * @return true if need to reset outgoing phone account.
     *
     * @internal
     */
    boolean shouldRemoveDefaultPhoneAccount(List<PhoneAccountHandle> accountHandleList);
}