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
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.WindowManager;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.mediatek.phone.PhoneFeatureConstants.FeatureOption;

/**
 * Service that check Lte data only mode and whether show dialog.
 */
public class LteDataOnlyManagerService extends Service {
    private static final String TAG = "LteDataOnlyManagerService";

    private AlertDialog mDialog;
    private int mStartId = -1;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private Phone mPhone;
    private Enable4GHandler mHandler;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

        super.onCreate();
        createPermissionDialog();

        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.MSIM_MODE_SETTING),
                true, mObserverForRadioState);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mStartId = startId;
        mSubId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        mPhone = PhoneUtils.getPhoneUsingSubId(mSubId);
        mHandler = new Enable4GHandler();

        Log.d(TAG, "onStartCommand, startId = " + startId + "; sub = " + mSubId);

        getContentResolver().registerContentObserver(Settings.Global.getUriFor(
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE + mPhone.getSubId()),
                true, mContentObserver);

        showPermissionDialog();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        getContentResolver().unregisterContentObserver(mContentObserver);
        getContentResolver().unregisterContentObserver(mObserverForRadioState);
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        unregisterReceiver(mReceiver);
    }

    private void createPermissionDialog() {
         final AlertDialog.Builder builder = new AlertDialog.Builder(this);
         builder.setTitle(R.string.lte_only_dialog_title_prompt)
                .setMessage(R.string.lte_data_only_prompt)
                .setNegativeButton(R.string.lte_only_dialog_button_no,
                    new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopSelf(mStartId);
                    }
                })
                .setPositiveButton(R.string.lte_only_dialog_button_yes,
                      new DialogInterface.OnClickListener() {
                      @Override
                      public void onClick(DialogInterface dialog, int which) {
                          if (!checkServiceCondition()) {
                              Log.d(TAG,
                                "PositiveButton onClick :checkServiceCondition failed, stop");
                              stopSelf(mStartId);
                              return;
                          }
                          mPhone.setPreferredNetworkType(Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA,
                                  mHandler.obtainMessage(
                                          Enable4GHandler.MESSAGE_SET_ENABLE_4G_NETWORK_TYPE));
                      }
                  })
                  .setOnDismissListener(new DialogInterface.OnDismissListener() {
                      public void onDismiss(DialogInterface dialog) {
                          Log.d(TAG, "OnDismissListener :stopSelf(), mStartId = " + mStartId);
                          stopSelf(mStartId);
                      }
                  });
          mDialog = builder.create();
          mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
          mDialog.setCanceledOnTouchOutside(false);
     }

    private void showPermissionDialog() {
        if (!checkServiceCondition()) {
            stopSelf(mStartId);
            return;
        }
        if (mDialog != null && !mDialog.isShowing()) {
            mDialog.show();
        }
    }

    private boolean checkServiceCondition() {
        return FeatureOption.isMtkTddDataOnlySupport()
                && TelephonyUtilsEx.is4GDataOnly(this)
                && !TelephonyUtilsEx.isAirPlaneMode()
                && TelephonyUtilsEx.isSvlteSlotInserted()
                && TelephonyUtilsEx.isSvlteSlotRadioOn();
    }

     private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (mDialog != null && mDialog.isShowing()
                    && !TelephonyUtilsEx.is4GDataOnly(LteDataOnlyManagerService.this)) {
                mDialog.dismiss();
            }
        }
    };

    private ContentObserver mObserverForRadioState = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (mDialog != null && mDialog.isShowing() && !TelephonyUtilsEx.isSvlteSlotRadioOn()) {
                mDialog.dismiss();
            }
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Log.d(TAG, "onReceive action = " + action);

            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                if (intent.getBooleanExtra("state", false)) {
                    Log.d(TAG, "Action stop service");
                    stopSelf(mStartId);
                }
            } else if (action.equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)) {
                if (!TelephonyUtilsEx.isSvlteSlotInserted()) {
                    Log.d(TAG, "Action stop service");
                    stopSelf(mStartId);
                }
            }
        }
    };

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
