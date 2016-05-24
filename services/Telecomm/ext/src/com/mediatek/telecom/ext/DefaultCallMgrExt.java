package com.mediatek.telecom.ext;

public class DefaultCallMgrExt implements ICallMgrExt {

    /**
     * should build call capabilities.
     *
     * @param smsCapability can response via sms.
     *
     * @return capalilities of the call.
     */
    @Override
    public int buildCallCapabilities(boolean smsCapability) {
        return 0;
    }

}