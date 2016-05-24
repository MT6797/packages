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
package com.mediatek.contacts.vcs;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;

import com.mediatek.common.voicecommand.IVoiceCommandListener;
import com.mediatek.common.voicecommand.IVoiceCommandManagerService;
import com.mediatek.common.voicecommand.VoiceCommandListener;

import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.vcs.VcsAppGuide.OnGuideFinishListener;

import java.util.ArrayList;

public class VoiceSearchManager {
    private static final String TAG = "VoiceSearchManager";

    private int mState = STATE.STATE_NONE;
    private boolean mHasCalledVoiceStart = false;
    private VoiceListener mVoiceListener = null;
    private SpeechLister mSpeechLister = null;
    private Context mContext = null;
    private VcsAppGuide mAppGuide = null;
    private IVoiceCommandManagerService mVoiceCommandService = null;

    private IVoiceCommandListener mVoiceCommandListener = new IVoiceCommandListener.Stub() {
        @Override
        public void onVoiceCommandNotified(int mainAction, int subAction, Bundle extraData)
                throws RemoteException {
            Log.d(TAG, "[onVoiceCommandNotified]mainAction:" + mainAction);
            switch (mainAction) {
            case VoiceCommandListener.ACTION_MAIN_VOICE_CONTACTS:
                Message msg = Message.obtain();
                msg.what = subAction;
                msg.obj = extraData;
                mHandler.sendMessage(msg);
                break;
            default:
                Log.i(TAG, "[onVoiceCommandNotified] running in default");
                break;
            }
        }
    };

    private ServiceConnection mVoiceCommandConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "[onServiceConnected]...");
            mState = STATE.STATE_CREATE;
            mVoiceCommandService = IVoiceCommandManagerService.Stub.asInterface(service);
            mVoiceListener.onVoiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "[onServiceDisconnected]...");
            mState = STATE.STATE_DESTORY;
            mVoiceListener.onVoiceStop();
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = (Bundle) msg.obj;
            int errorCode = bundle.getInt(VoiceCommandListener.ACTION_EXTRA_RESULT);
            if (VoiceCommandListener.ACTION_EXTRA_RESULT_SUCCESS != errorCode) {
                Log.w(TAG, "[handleMessage] subAction:" + msg.what + " failed:" + errorCode);
                return;
            }
            Log.d(TAG, "[handleMessage]msg.what:" + msg.what);
            switch (msg.what) {
            case VoiceCommandListener.ACTION_VOICE_CONTACTS_START:
                // Enable Voice RECOGNITION when start
                Log.w(TAG, "[handleMessage] ACTION_VOICE_CONTACTS_START...");
                mState = STATE.STATE_START;
                enableVoice();
                break;
            case VoiceCommandListener.ACTION_VOICE_CONTACTS_RECOGNITION_ENABLE:
                mState = STATE.STATE_ENABLE;
                mVoiceListener.onVoiceEnable();
                break;
            case VoiceCommandListener.ACTION_VOICE_CONTACTS_RECOGNITION_DISABLE:
                mState = STATE.STATE_DISABLE;
                mVoiceListener.onVoiceDisble();
                break;
            case VoiceCommandListener.ACTION_VOICE_CONTACTS_STOP:
                Log.w(TAG, "[handleMessage] ACTION_VOICE_CONTACTS_STOP...");
                mState = STATE.STATE_STOP;
                mVoiceListener.onVoiceStop();
                break;
            case VoiceCommandListener.ACTION_VOICE_CONTACTS_SPEECH_DETECTED:
                Log.i(TAG, "ACTION_VOICE_CONTACTS_SPEECH_DETECTED");
                mSpeechLister.onSpeechDetected();
                break;
            case VoiceCommandListener.ACTION_VOICE_CONTACTS_NOTIFY:
                String[] names = bundle
                        .getStringArray(VoiceCommandListener.ACTION_EXTRA_RESULT_INFO);
                ArrayList<String> nameList = new ArrayList<String>();
                if (names == null) {
                    Log.i(TAG, "[handleMessage] vcs names from voice command service is null");
                } else {
                    StringBuffer sb = new StringBuffer();
                    sb.append("vcs names from voice command service:");
                    for (String name : names) {
                        sb.append(name + ",");
                        nameList.add(name);
                    }
                    Log.i(TAG, sb.toString());
                }
                mSpeechLister.onSpeechResult(nameList);
                break;
            case VoiceCommandListener.ACTION_VOICE_CONTACTS_SELECTED:
                // do-nothing
                break;
            case VoiceCommandListener.ACTION_VOICE_CONTACTS_INTENSITY:
                // do-nothing
                break;
            default:
                break;
            }
        }
    };

    private interface STATE {
        /**
         * Before Connect to Voice Command Service
         */
        int STATE_NONE = -1;
        /**
         * Have Connect to Voice Command Service
         */
        int STATE_CREATE = 0;
        /**
         * Voice Device have been start
         */
        int STATE_START = 1;
        /**
         * Voice Device Recognition been Enable
         */
        int STATE_ENABLE = 2;
        /**
         * oice Device Recognition been Disable
         */
        int STATE_DISABLE = 3;
        /**
         * Voice Device been stop
         */
        int STATE_STOP = 4;
        /**
         * After unbing to Voice Command Service
         */
        int STATE_DESTORY = 5;
    }

    /**
     * Voice Life cycle Listener
     */
    public static interface VoiceListener {
        void onVoiceConnected();

        void onVoiceEnable();

        void onVoiceDisble();

        void onVoiceStop();
    }

    /**
     * Voice Speech Listener
     */
    public static interface SpeechLister {
        /**
         * Call Back when speech detected
         */
        void onSpeechDetected();

        /**
         * Call Back for speech result .
         *
         * @param nameList
         *            The names match from Voice Command
         */
        void onSpeechResult(ArrayList<String> nameList);
    }

    public VoiceSearchManager(Context context) {
        Log.i(TAG, "[VoiceSearchManager]new...");
        mContext = context;
        mAppGuide = new VcsAppGuide(mContext);
        bindVoiceCommandService();
    }

    // ---------------private method------------------------//
    private void bindVoiceCommandService() {
        Log.d(TAG, "[bindVoiceCommandService]...");
        Intent intnet = new Intent();
        intnet.setAction(VoiceCommandListener.VOICE_SERVICE_ACTION);
        intnet.addCategory(VoiceCommandListener.VOICE_SERVICE_CATEGORY);
        intnet.setPackage(VoiceCommandListener.VOICE_SERVICE_PACKAGE_NAME);
        mContext.bindService(intnet, mVoiceCommandConnection, Context.BIND_AUTO_CREATE);
    }

    private void unBindVoiceCommandService() {
        Log.d(TAG, "[unBindVoiceCommandService]...");
        mContext.unbindService(mVoiceCommandConnection);
    }

    private void sendVoiceCommandAction(int subAction, Bundle extraData) {
        if (mState == STATE.STATE_NONE || mState == STATE.STATE_DESTORY) {
            Log.w(TAG, "[sendVoiceCommandAction] Cann't send action:" + subAction + ",mState:"
                    + mState);
            return;
        }
        try {
            int errorCode = mVoiceCommandService.sendCommand(mContext.getPackageName(),
                    VoiceCommandListener.ACTION_MAIN_VOICE_CONTACTS, subAction, extraData);

            if (errorCode != VoiceCommandListener.VOICE_NO_ERROR) {
                Log.w(TAG, "[sendVoiceCommandAction] action:" + subAction + " failed:"
                        + errorCode + ",mState:" + mState);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "[sendVoiceCommandAction]e = " + e + ",mState:" + mState);
        }
    }

    private boolean registerVoiceCommand(IVoiceCommandListener lister) {
        if (mState >= STATE.STATE_START && mState < STATE.STATE_STOP) {
            Log.w(TAG, "[registerVoiceCommand] has registed agao,mState:" + mState);
            return false;
        }

        if (mState < STATE.STATE_CREATE) {
            Log.w(TAG, "[registerVoiceCommand] bind Service failed,mState:" + mState);
            return false;
        }

        try {
            int errorCode = mVoiceCommandService
                    .registerListener(mContext.getPackageName(), lister);
            if (errorCode != VoiceCommandListener.VOICE_NO_ERROR) {
                Log.w(TAG, "[registerVoiceCommand] Register voice Listener falil:" + errorCode
                        + ",mState:" + mState);
                return false;
            }
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "[registerVoiceCommand] Register  RemoteException = " + e + ",mState:"
                    + mState);
        }
        return false;
    }

    private boolean unRegisterVoiceCommand(IVoiceCommandListener lister) {
        if (mState < STATE.STATE_START) {
            Log.w(TAG, "[unRegisterVoiceCommand] can not unRegisterVoiceCommand,mState:"
                    + mState);
            return false;
        }
        try {
            int errorCode = mVoiceCommandService
                    .registerListener(mContext.getPackageName(), lister);
            if (errorCode != VoiceCommandListener.VOICE_NO_ERROR) {
                Log.w(TAG, "[unRegisterVoiceCommand] unRegister voice Listener falil:"
                        + errorCode);
                return false;
            }
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "[unRegisterVoiceCommand] unRegister RemoteException = " + e);
        }
        return false;
    }

    private void setVoiceReconEnable(boolean enable) {
        int subAction = enable ? VoiceCommandListener.ACTION_VOICE_CONTACTS_RECOGNITION_ENABLE
                : VoiceCommandListener.ACTION_VOICE_CONTACTS_RECOGNITION_DISABLE;
        sendVoiceCommandAction(subAction, null);
    }

    // ------------------------public method---------------------------//
    public void setVoiceListener(VoiceListener listener) {
        mVoiceListener = listener;
    }

    public void setSpeechLister(SpeechLister lister) {
        mSpeechLister = lister;
    }

    public void enableVoice() {
        Log.i(TAG, "[enableVoice]mState = " + mState + ",mHasCalledVoiceStart = "
                + mHasCalledVoiceStart);
        // change for ALPS02392475, do nothing if state is STATE_NONE.
        if (mState == STATE.STATE_CREATE || mState == STATE.STATE_STOP) {
            // voice not start,start it...
            if (mHasCalledVoiceStart) {
                return;
            }
            registerVoiceCommand(mVoiceCommandListener);
            Bundle bundle = new Bundle();
            bundle.putInt(VoiceCommandListener.ACTION_EXTRA_SEND_INFO, mContext.getResources()
                    .getConfiguration().orientation);
            mHasCalledVoiceStart = true;
            sendVoiceCommandAction(VoiceCommandListener.ACTION_VOICE_CONTACTS_START, bundle);
        } else if (mState == STATE.STATE_START || mState == STATE.STATE_DISABLE) {
            setVoiceReconEnable(true);
        }
    }

    public void disableVoice() {
        Log.i(TAG, "[disableVoice]mState = " + mState);
        if (mState == STATE.STATE_ENABLE) {
            setVoiceReconEnable(false);
        }
    }

    public void stopVoice() {
        Log.i(TAG, "[stopVoice]mHasCalledVoiceStart = " + mHasCalledVoiceStart);
        if (!mHasCalledVoiceStart) {
            return;
        }
        sendVoiceCommandAction(VoiceCommandListener.ACTION_VOICE_CONTACTS_STOP, null);
        // unregister the listener directly
        unRegisterVoiceCommand(mVoiceCommandListener);
        // the mVoiceCommandListener will not be called,set mState to Stop here.
        mState = STATE.STATE_STOP;
        if (mVoiceListener != null) {
            mVoiceListener.onVoiceStop();
        }
        mHasCalledVoiceStart = false;
    }

    public void destoryVoice() {
        Log.i(TAG, "[destoryVoice]...");
        unBindVoiceCommandService();
    }

    public void setVoiceLearn(String name) {
        Log.i(TAG, "[setVoiceLearn] learning name:" + name);
        Bundle extraData = new Bundle();
        extraData.putString(VoiceCommandListener.ACTION_EXTRA_SEND_INFO, name);
        sendVoiceCommandAction(VoiceCommandListener.ACTION_VOICE_CONTACTS_SELECTED, extraData);
    }

    /**
     * show VCS App Guide if VCS feature is Enable.
     *
     * @param activity
     */
    public boolean setVcsAppGuideVisibility(Activity activity, boolean visibility,
            OnGuideFinishListener listener) {
        if (VcsUtils.isVcsFeatureEnable()) {
            return mAppGuide.setVcsAppGuideVisibility(activity, visibility, listener);
        }
        return false;
    }

    /** Bug Fix for ALPS01706025 @{ */
    public boolean isInEnableStatus() {
        Log.i(TAG, "[isInEnableStatus]...mState : " + mState);
        return (mState == STATE.STATE_ENABLE);
    }
    /** @} */
}
