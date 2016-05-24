/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.settings.cdma;

import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.phone.PhoneUtils;
import com.android.phone.R;

/**
 * Service that check Lte data only mode and whether show dialog.
 */
public class LteSearchTimeoutCheckService extends Service {
    private static final String TAG = "LteSearchTimeoutCheckService";

    public static final String ACTION_START_SELF =
        "com.mediatek.intent.action.STARTSELF_LTE_SEARCH_TIMEOUT_CHECK";
    // CT spec defines 3 mins for the dialog.
    private static final long DELAY_MILLIS_SHOW_DIALOG = 180000;
    private AlertDialog mDialog;
    private int mStartId = -1;
    private TelephonyManager mTelephonyManager;
    private boolean mIsLteInService;
    private boolean mIsWaitingCheck;
    private boolean mIsSvlteSlotInserted;
    private boolean mIsSvlteSlotRadioOn;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private final Handler mHandler = new Handler();
    private PhoneStateListener mPhoneStateListenerForLte;
    private Phone mPhone;
    private Enable4GHandler mEnableHandler;

    private Runnable mShowDialogRunnable = new Runnable() {
            @Override
            public void run() {
                showTimeoutDialog();
            }
        };

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        createTimeoutDialog();
        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mIsSvlteSlotInserted = TelephonyUtilsEx.isSvlteSlotInserted();
        mIsSvlteSlotRadioOn = TelephonyUtilsEx.isSvlteSlotRadioOn();
        mPhone = PhoneFactory.getPhone(TelephonyUtilsEx.getMainPhoneId());
        mEnableHandler = new Enable4GHandler();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        registerReceiver(mReceiver, intentFilter);
        getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.LTE_ON_CDMA_RAT_MODE),
                true, mContentObserver);
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.MSIM_MODE_SETTING),
                true, mObserverForRadioState);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand, startId = " + startId);
        mStartId = startId;
        startCheckTimeout();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        getContentResolver().unregisterContentObserver(mContentObserver);
        getContentResolver().unregisterContentObserver(mObserverForRadioState);
        mHandler.removeCallbacks(mShowDialogRunnable);
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        if (mTelephonyManager != null && mPhoneStateListenerForLte != null) {
            mTelephonyManager.listen(mPhoneStateListenerForLte, PhoneStateListener.LISTEN_NONE);
        }
        unregisterReceiver(mReceiver);
    }

    private void startCheckTimeout() {
       Log.d(TAG, "startCheckTimeout");
        if (!checkServiceCondition()) {
            return;
        }
        mIsWaitingCheck = false;

        int mainPhoneId = TelephonyUtilsEx.getMainPhoneId();
        Phone phone = PhoneFactory.getPhone(mainPhoneId);
        if (TelephonyUtilsEx.isCDMAPhone(phone) && TelephonyUtilsEx.is4GDataOnly(this)) {
            if (mTelephonyManager != null && mDialog != null && !mDialog.isShowing()) {
                mHandler.removeCallbacks(mShowDialogRunnable);
                mHandler.postDelayed(mShowDialogRunnable, DELAY_MILLIS_SHOW_DIALOG);
                createPhoneStateListener();
                if (mPhoneStateListenerForLte != null) {
                    mTelephonyManager.listen(mPhoneStateListenerForLte,
                        PhoneStateListener.LISTEN_SERVICE_STATE);
                }

                Log.d(TAG, "startCheckTimeout ok");
            }
        }
    }

    private void createTimeoutDialog() {
         final AlertDialog.Builder builder = new AlertDialog.Builder(this);
         builder.setTitle(R.string.lte_only_dialog_title_prompt)
                .setMessage(R.string.lte_data_only_timeout)
                .setNegativeButton(R.string.lte_only_dialog_button_no, null)
                .setPositiveButton(R.string.lte_only_dialog_button_yes,
                      new DialogInterface.OnClickListener() {
                      @Override
                      public void onClick(DialogInterface dialog, int which) {
                          Log.d(TAG, "PositiveButton onClick");
                          if (!checkServiceCondition()) {
                              return;
                          }
                          mPhone.setPreferredNetworkType(
                                Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA,
                                mHandler.obtainMessage(Enable4GHandler.
                                        MESSAGE_SET_ENABLE_4G_NETWORK_TYPE));
                      }
                  })
                  .setOnDismissListener(
                            new DialogInterface.OnDismissListener() {
                                public void onDismiss(DialogInterface dialog) {
                                    if (!checkServiceCondition() || mIsLteInService
                                            || mIsWaitingCheck) {
                                        Log.d(TAG, "OnDismiss : donothing");
                                    } else {
                                        Log.d(TAG, "OnDismiss : will restart service");
                                        Intent intent = new Intent(ACTION_START_SELF);
                                        LteSearchTimeoutCheckService.this.sendBroadcast(intent);
                                        stopSelf(mStartId);
                                    }
                                }
                            });
          mDialog = builder.create();
          mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
          mDialog.setCanceledOnTouchOutside(false);
     }

    private void showTimeoutDialog() {
        if (checkServiceCondition() && mDialog != null
                && !mDialog.isShowing() && mIsLteInService == false) {
            Log.d(TAG, "showTimeoutDialog");
            mDialog.show();
        }
    }

    private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (!TelephonyUtilsEx.is4GDataOnly(LteSearchTimeoutCheckService.this)) {
                Log.d(TAG, "mContentObserver update, not 4GDataOnly,stopself");
                stopSelf(mStartId);
            }
        }
    };

    private ContentObserver mObserverForRadioState = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (mIsSvlteSlotRadioOn == TelephonyUtilsEx.isSvlteSlotRadioOn()) {
                return;
            }
            mIsSvlteSlotRadioOn = !mIsSvlteSlotRadioOn;
            Log.d(TAG, "mObserverForRadioState update mIsSvlteSlotRadioOn : "
                  + mIsSvlteSlotRadioOn);

            if (mIsSvlteSlotRadioOn) {
                startCheckTimeout();
            } else {
                stopCheck();
            }
        }
    };
    private void createPhoneStateListener() {
        if (mPhoneStateListenerForLte == null) {
            mPhoneStateListenerForLte = new PhoneStateListener(mSubId) {
                @Override
                public void onServiceStateChanged(ServiceState serviceState) {
                    Log.d(TAG, "onServiceStateChanged, mSubId : " + this.mSubId
                           + ", serviceState : " + serviceState);
                    if (serviceState.getDataRegState() == ServiceState.STATE_IN_SERVICE
                        && serviceState.getVoiceRegState() == ServiceState.STATE_OUT_OF_SERVICE
                        && serviceState.getDataNetworkType() == TelephonyManager.NETWORK_TYPE_LTE) {
                        Log.d(TAG, "LTE is in service state, cancel show dialog");
                        mIsLteInService = true;
                        mHandler.removeCallbacks(mShowDialogRunnable);
                        if (mDialog != null && mDialog.isShowing()) {
                           mDialog.dismiss();
                        }
                    } else {
                        if (mIsLteInService) {
                            mIsLteInService = false;
                            startCheckTimeout();
                        }
                    }
                }
            };
        }
    }

    private boolean checkServiceCondition() {
        return TelephonyUtilsEx.is4GDataOnly(this)
                && !TelephonyUtilsEx.isAirPlaneMode()
                && TelephonyUtilsEx.isSvlteSlotInserted()
                && TelephonyUtilsEx.isSvlteSlotRadioOn();
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive action = " + action);
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                if (intent.getBooleanExtra("state", false)) {
                    Log.d(TAG, "Action enter flight mode");
                    stopCheck();
                } else {
                    Log.d(TAG, "Action leave flight mode");
                    startCheckTimeout();
                }
            } else if (action.equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)) {
                if (mIsSvlteSlotInserted == TelephonyUtilsEx.isSvlteSlotInserted()) {
                    return;
                }
                mIsSvlteSlotInserted = !mIsSvlteSlotInserted;
                Log.d(TAG, "Action update mIsSvlteSlotInserted : " + mIsSvlteSlotInserted);
                if (mIsSvlteSlotInserted) {
                    startCheckTimeout();
                } else {
                    stopCheck();
                }
            }
        }
    };

    private void stopCheck() {
        Log.d(TAG, "stopCheck");
        mIsLteInService = false;
        mIsWaitingCheck = true;
        mHandler.removeCallbacks(mShowDialogRunnable);
        if (mDialog != null && !mDialog.isShowing()) {
            mDialog.dismiss();
        }
        if (mPhoneStateListenerForLte != null) {
            mTelephonyManager.listen(mPhoneStateListenerForLte,
                PhoneStateListener.LISTEN_NONE);
        }
    }

    private class Enable4GHandler extends Handler {

        static final int MESSAGE_SET_ENABLE_4G_NETWORK_TYPE = 0;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_SET_ENABLE_4G_NETWORK_TYPE:
                handleSetEnable4GNetworkTypeResponse(msg);
                break;
            }
        }

        private void handleSetEnable4GNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception == null) {

                Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE + mPhone.getSubId(),
                        Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA);
            } else {
                Log.d(TAG, "handleSetPreferredNetworkTypeResponse: exception in Enable4GHandler.");
                //todo handle error
            }
        }
    }

}
