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
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.android.contacts.R;

import com.mediatek.contacts.util.Log;

public class VcsAppGuide {
    private static final String TAG = "VcsAppGuide";

    private static final String SHARED_PREFERENCE_NAME = "application_guide";
    private static final String KEY_VCS_GUIDE = "vcs_guide";
    private SharedPreferences mSharedPrefs;
    private Context mContext;
    private Dialog mAppGuideDialog;
    private OnGuideFinishListener mFinishListener;

    public VcsAppGuide(Context context) {
        mContext = context;
    }

    /**
     * Called when the app want to show VCS application guide
     *
     * @param activity
     *            The parent activity
     * @param commd
     *            The commd fotrwhich Plugin Implements will run
     */
    public boolean setVcsAppGuideVisibility(Activity activity, boolean isShow,
            OnGuideFinishListener onFinishListener) {
        Log.i(TAG, "[setVcsAppGuideVisibility]isShow = " + isShow);
        if (isShow) {
            mFinishListener = onFinishListener;
            mSharedPrefs = activity.getSharedPreferences(SHARED_PREFERENCE_NAME,
                    Context.MODE_WORLD_WRITEABLE);
            if (mSharedPrefs.getBoolean(KEY_VCS_GUIDE, false)) {
                Log.d(TAG, "[setVcsAppGuideVisibility]already show VCS guide, return");
                return false;
            }
            if (mAppGuideDialog == null) {
                Log.d(TAG, "[setVcsAppGuideVisibility]mAppGuideDialog == null");
                mAppGuideDialog = new AppGuideDialog(activity);
                mAppGuideDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT);
            }
            mAppGuideDialog.show();
            return true;
        } else {
            dismissVcsAppGuide();
            return false;
        }
    }

    private void dismissVcsAppGuide() {
        if (mAppGuideDialog != null) {
            Log.d(TAG, "[dismissVcsAppGuide]");
            mAppGuideDialog.dismiss();
            mAppGuideDialog = null;
        }
    }

    private void onGuideFinish() {
        if (mFinishListener != null) {
            Log.d(TAG, "[onGuideFinish]...");
            mFinishListener.onGuideFinish();
        }
    }

    class AppGuideDialog extends Dialog {

        private Activity mActivity;
        private Button mOkBtn;

        /**
         * ok button listner, finish app guide.
         */
        private View.OnClickListener mOkListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSharedPrefs.edit().putBoolean(KEY_VCS_GUIDE, true).commit();
                onGuideFinish();
                onBackPressed();
            }
        };

        public AppGuideDialog(Activity activity) {
            super(activity, android.R.style.Theme_Translucent_NoTitleBar);
            mActivity = activity;
        }

        @Override
        public void onBackPressed() {
            dismissVcsAppGuide();
            onGuideFinish();
            super.onBackPressed();
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            // PluginLayoutInflater inflater = new
            // PluginLayoutInflater(mContext);
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.vcs_guide_full_bg_layout, null);
            mOkBtn = (Button) view.findViewById(R.id.vcs_ok_btn);
            mOkBtn.setText(android.R.string.ok);
            mOkBtn.setOnClickListener(mOkListener);
            setContentView(view);
        }
    }

    public static interface OnGuideFinishListener {
        void onGuideFinish();
    }
}
