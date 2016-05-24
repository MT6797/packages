package com.mediatek.telecom.recording;

interface IPhoneRecordStateListener {
    void onStateChange(int state);
    void onStorageFull();
    void onError(int iError);
    void onFinished(int cause, String data);
}
