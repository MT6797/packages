/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.storage.StorageVolume;
import android.util.Log;

public class PhoneRecorderServices extends Service {

    private static final String LOG_TAG = "RecorderServices";
    private static final String PHONE_VOICE_RECORD_STATE_CHANGE_MESSAGE = "com.android.phone.VoiceRecorder.STATE";
    private static final int REQUEST_START_RECORDING = 1;
    private static final int REQUEST_STOP_RECORDING = 2;
    private static final int REQUEST_QUIT = -1;

    private PhoneRecorder mPhoneRecorder;
    IPhoneRecordStateListener mStateListener;
    private HandlerThread mWorkerThread;
    private Handler mRecordHandler;

    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        mPhoneRecorder = PhoneRecorder.getInstance(this);
        if (null != mPhoneRecorder) {
            mPhoneRecorder.setOnStateChangedListener(mPhoneRecorderStateListener);
        }
        mWorkerThread = new HandlerThread("RecordWorker");
        mWorkerThread.start();
        mRecordHandler = new RecordHandler(mWorkerThread.getLooper());
        mRecordHandler.postDelayed(mRecordDiskCheck, 500);
        return mBinder;
    }

    public boolean onUnbind(Intent intent) {
        Log.d(LOG_TAG, "onUnbind");
        mRecordHandler.sendMessage(mRecordHandler.obtainMessage(REQUEST_STOP_RECORDING,
                new Boolean(true)));
        mRecordHandler.removeCallbacks(mRecordDiskCheck);
        mRecordHandler.sendEmptyMessage(REQUEST_QUIT);
        return super.onUnbind(intent);
    }

    public void onCreate() {
        super.onCreate();
        log("onCreate");
        registerMediaStateReceiver();
    }

    public void onDestroy() {
        super.onDestroy();
        log("onDestroy");
        unregisterMediaStateReceiver();
    }

    private void registerMediaStateReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addDataScheme("file");
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void unregisterMediaStateReceiver() {
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }
    }
    public void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private PhoneRecorder.OnStateChangedListener mPhoneRecorderStateListener = new PhoneRecorder.OnStateChangedListener() {
        public void onStateChanged(int state) {
            log("[onStateChanged]state = " + state);
            if (null != mStateListener) {
                try {
                    log("[onStateChanged]notify listener");
                    mStateListener.onStateChange(state);
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "PhoneRecordService: call listener onStateChange failed",
                            new IllegalStateException());
                }
            }
        }

        public void onError(int error) {
            log("[onError]error = " + error);
            if (null != mStateListener) {
                try {
                    log("[onError]notify listener");
                    mStateListener.onError(error);
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "PhoneRecordService: call listener onError() failed",
                            new IllegalStateException());
                }
            }
        }

        public void onFinished(int cause, String data) {
            if (null != mStateListener) {
                try {
                    log("[onFinished]notify listener");
                    mStateListener.onFinished(cause, data);
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "PhoneRecordService: call listener onError() failed",
                            new IllegalStateException());
                }
            }
        }

    };

    private final IPhoneRecorder.Stub mBinder = new IPhoneRecorder.Stub() {
        public void listen(IPhoneRecordStateListener callback) {
            log("listen");
            if (null != callback) {
                mStateListener = callback;
            }
        }

        @Deprecated
        public void remove() {
            log("remove is deprecated, do nothing. listener will be removed automatically");
        }

        public void startRecord() {
            log("startRecord");
            mRecordHandler.sendEmptyMessage(REQUEST_START_RECORDING);
        }

        public void stopRecord(boolean isMounted) {
            log("stopRecord");
            mRecordHandler.sendMessage(mRecordHandler.obtainMessage(REQUEST_STOP_RECORDING, new Boolean(isMounted)));
        }

        public boolean isRecording() {
            if (mPhoneRecorder != null) {
                return PhoneRecorder.isRecording();
            } else {
                return false;
            }
        }
    };

    /**
     * Handler base on worker thread Looper.
     * it will deal with the time consuming operations, such as start/stop recording
     */
    private class RecordHandler extends Handler {
        public RecordHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            log("[handleMessage]what = " + msg.what);
            switch (msg.what) {
                case REQUEST_START_RECORDING:
                    if (null != mPhoneRecorder) {
                        log("[handleMessage]do start recording");
                        mRecordStoragePath = RecorderUtils.getExternalStorageDefaultPath();
                        mPhoneRecorder.startRecord();
                    }
                    break;
                case REQUEST_STOP_RECORDING:
                    Boolean isMounted = (Boolean) msg.obj;
                    if (null != mPhoneRecorder) {
                        log("[handleMessage]do stop recording");
                        mPhoneRecorder.stopRecord(isMounted);
                    }
                    mRecordStoragePath = null;
                    break;
                case REQUEST_QUIT:
                    log("[handleMessage]quit worker thread and clear handler");
                    // quit to avoid looper leakage, and make sure the pending operations can finish before really quit
                    mWorkerThread.quit();
                    break;
                default:
                    log("[handleMessage]unexpected message: " + msg.what);
            }
        }
    }


    private String mRecordStoragePath;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (mPhoneRecorder != null && (Intent.ACTION_MEDIA_EJECT.equals(intent.getAction())
                    || Intent.ACTION_MEDIA_UNMOUNTED.equals(intent.getAction()))) {
                StorageVolume storageVolume =
                        (StorageVolume) intent
                                .getParcelableExtra(StorageVolume.EXTRA_STORAGE_VOLUME);
                if (null == storageVolume) {
                    log("storageVolume is null");
                    return;
                }
                String currentPath = storageVolume.getPath();
                if (null == mRecordStoragePath || !currentPath.equals(mRecordStoragePath)) {
                    log("not current used storage unmount or eject");
                    return;
                }
                if (PhoneRecorder.isRecording()) {
                    mRecordHandler.removeCallbacks(mRecordDiskCheck);
                    log("Current used sd card is ejected, stop voice record");
                    mRecordHandler.sendMessage(mRecordHandler.obtainMessage(REQUEST_STOP_RECORDING,
                            false));
                }
            }
        }
    };

    private void checkRecordDisk() {
        if (PhoneRecorder.isRecording()
                && !RecorderUtils.diskSpaceAvailable(mRecordStoragePath,
                        PhoneRecorderHandler.PHONE_RECORD_LOW_STORAGE_THRESHOLD)) {
            mRecordHandler.removeCallbacks(mRecordDiskCheck);
            Log.e("AN: ", "Checking result, disk is full, stop recording...");
            mRecordHandler.sendMessage(mRecordHandler.obtainMessage(REQUEST_STOP_RECORDING,
                    false));
            if (null != mStateListener) {
                try {
                    mStateListener.onStorageFull();
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "PhoneRecordService: call listener onStorageFull failed",
                            new IllegalStateException());
                }
            }
        } else {
            mRecordHandler.postDelayed(mRecordDiskCheck, 50);
        }
    }

    private Runnable mRecordDiskCheck = new Runnable() {
        public void run() {
            checkRecordDisk();
        }
    };
}
