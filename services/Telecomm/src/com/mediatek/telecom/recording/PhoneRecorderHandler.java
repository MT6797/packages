/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.telecom.recording;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.android.server.telecom.R;
import com.android.server.telecom.TelecomSystem;

public class PhoneRecorderHandler {

    private static final String LOG_TAG = "PhoneRecorderHandler";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    private Intent mRecorderServiceIntent = new Intent(TelecomSystem.getInstance().getContext(),
            PhoneRecorderServices.class);
    private IPhoneRecorder mPhoneRecorder;
    private int mPhoneRecorderState = PhoneRecorder.IDLE_STATE;
    private Listener mListener;

    public static final long PHONE_RECORD_LOW_STORAGE_THRESHOLD = 2L * 1024L * 1024L; // unit is BYTE, totally 2MB
    public static final int PHONE_RECORDING_VOICE_CALL_CUSTOM_VALUE = 0;

    public interface Listener {
        /**
         *
         * @param state
         * @param customValue
         */
        void requestUpdateRecordState(final int state, final int customValue);

        void onStorageFull();
    }

    private PhoneRecorderHandler() {
    }

    private static PhoneRecorderHandler sInstance = new PhoneRecorderHandler();

    public static synchronized PhoneRecorderHandler getInstance() {
        return sInstance;
    }

    /**
     *
     * @param listener
     */
    public void setListener(Listener listener) {
        mListener = listener;
    }

    public Listener getListener() {
        return mListener;
    }

    /**
     *
     * @param listener
     */
    public void clearListener(Listener listener) {
        if (listener == mListener) {
            mListener = null;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mPhoneRecorder = IPhoneRecorder.Stub.asInterface(service);
            try {
                log("onServiceConnected");
                if (null != mPhoneRecorder) {
                    mPhoneRecorder.listen(mPhoneRecordStateListener);
                    if (TelecomSystem.getInstance().getCallsManager().getActiveCall() != null) {
                        mPhoneRecorder.startRecord();
                    }
                }
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "onServiceConnected: couldn't register to record service",
                        new IllegalStateException());
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            log("[onServiceDisconnected]");
            mPhoneRecorder = null;
        }
    };

    private IPhoneRecordStateListener mPhoneRecordStateListener = new IPhoneRecordStateListener.Stub() {
        /**
         *
         * @param state
         */
        public void onStateChange(int state) {
            log("[onStateChange] state is " + state);
            mPhoneRecorderState = state;
            if (null != mListener) {
                mListener.requestUpdateRecordState(state, PHONE_RECORDING_VOICE_CALL_CUSTOM_VALUE);
            }
        }

        public void onStorageFull() {
            log("[onStorageFull] " + mListener);
            if (null != mListener) {
                mListener.onStorageFull();
            }
        }

        /**
        *
        * @param iError
        */
       public void onError(int iError) {
           mHandler.sendEmptyMessage(convertStatusToEventId(iError));
           mPhoneRecorderState = PhoneRecorder.IDLE_STATE;
       }

       public void onFinished(int cause, String data) {
           int eventId = convertStatusToEventId(cause);
           if (data == null) {
               mHandler.sendEmptyMessage(eventId);
           } else {
               Message msg = mHandler.obtainMessage();
               msg.what = eventId;
               msg.obj = data;
               mHandler.sendMessage(msg);
           }
       }
    };

    /**
     *
     * @param customValue
     */
    public void startVoiceRecord(final int customValue) {
        if (TelecomSystem.getInstance().getCallsManager().getActiveCall() == null) {
            log("startVoiceRecord()... no active call, do not record.");
            return;
        }
        mPhoneRecorderState = PhoneRecorder.RECORDING_STATE;
        if (null != mRecorderServiceIntent && null == mPhoneRecorder) {
            TelecomSystem.getInstance().getContext().bindService(mRecorderServiceIntent,
                    mConnection, Context.BIND_AUTO_CREATE);
        } else if (null != mRecorderServiceIntent && null != mPhoneRecorder) {
            try {
                mPhoneRecorder.startRecord();
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "start Record failed", new IllegalStateException());
            }
        }
    }

    public void stopVoiceRecord() {
        if (isServiceRecording()) {
            stopVoiceRecord(true);
        }
    }

    private void stopVoiceRecord(boolean isMount) {
        try {
            log("stopRecord");
            if (null != mPhoneRecorder) {
                mPhoneRecorder.stopRecord(isMount);
            }
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "stopRecord: couldn't call to record service",
                    new IllegalStateException());
        }
    }

    public void stopRecording() {
        log("Stop record");
        stopVoiceRecord();
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private boolean isServiceRecording() {
        try {
            if (mPhoneRecorder != null) {
                return mPhoneRecorder.isRecording();
            }
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "stopVoiceRecord failed", new IllegalStateException());
        }
        return false;
    }

    private int convertStatusToEventId(int statusCode) {
        int eventId = EVENT_INTERNAL_ERROR;
        switch (statusCode) {
            case Recorder.SDCARD_ACCESS_ERROR:
                eventId = EVENT_SDCARD_ACCESS_ERROR;
                break;
            case Recorder.SUCCESS:
                eventId = EVENT_SAVE_SUCCESS;
                break;
            case Recorder.STORAGE_FULL:
                eventId = EVENT_STORAGE_FULL;
                break;
            case Recorder.STORAGE_UNMOUNTED:
                eventId = EVENT_STORAGE_UNMOUNTED;
                break;
            case Recorder.INTERNAL_ERROR:
            default:
                eventId = EVENT_INTERNAL_ERROR;
                break;
        }
        return eventId;
    }

    public static final int EVENT_STORAGE_FULL = 0;
    public static final int EVENT_SAVE_SUCCESS = 1;
    public static final int EVENT_STORAGE_UNMOUNTED = 2;
    public static final int EVENT_SDCARD_ACCESS_ERROR = 3;
    public static final int EVENT_INTERNAL_ERROR = 4;

    public MyHandler mHandler = new MyHandler(Looper.getMainLooper());
    class MyHandler extends Handler {

        public static final int EVENT_STORAGE_FULL = 0;
        public static final int EVENT_SAVE_SECCESS = 1;
        public static final int EVENT_STORAGE_UNMOUNTED = 2;
        public static final int EVENT_SDCARD_ACCESS_ERROR = 3;
        public static final int EVENT_INTERNAL_ERROR = 4;

        public MyHandler(Looper loop) {
            super(loop);
        }
        private Context getServiceContext() {
            return TelecomSystem.getInstance().getContext();
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_STORAGE_FULL:
                    Toast.makeText(
                            getServiceContext(),
                            getServiceContext().getApplicationContext().getResources()
                                    .getText(R.string.confirm_device_info_full), Toast.LENGTH_LONG)
                            .show();
                    break;
                case EVENT_SAVE_SECCESS:
                    String path = (String) msg.obj;
                    Toast.makeText(getServiceContext(), path, Toast.LENGTH_LONG).show();
                    break;
                case EVENT_STORAGE_UNMOUNTED:
                    Toast.makeText(
                            getServiceContext(),
                            getServiceContext().getApplicationContext()
                                    .getText(R.string.ext_media_badremoval_notification_title),
                            Toast.LENGTH_LONG).show();
                    break;
                case EVENT_SDCARD_ACCESS_ERROR:
                    Toast.makeText(
                            getServiceContext(),
                            getServiceContext().getApplicationContext().getResources().getString(
                                    R.string.error_sdcard_access),
                            Toast.LENGTH_LONG).show();
                    break;
                case EVENT_INTERNAL_ERROR:
                    Toast.makeText(
                            getServiceContext(),
                            getServiceContext().getApplicationContext().getResources().getString(
                            R.string.alert_device_error),
                            Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
            if (mPhoneRecorderState == PhoneRecorder.IDLE_STATE && mPhoneRecorder != null
                    && !isServiceRecording()) {
                log("Ready to unbind service");
                TelecomSystem.getInstance().getContext().unbindService(mConnection);
                mPhoneRecorder = null;
            }
        }
    };
}
