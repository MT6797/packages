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

import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.mediatek.contacts.util.Log;

public class VoiceSearchIndicator {
    private static final String TAG = "VoiceSearchIndicator";

    // vcs msg seciton
    private static final int MSG_UPDATE_INDICATOR_ICON = 100;
    // reactivate the icon every 600ms
    private static final long DELAY_TIME_INDICATOR = 600;

    private int mIconDisable = com.android.contacts.R.drawable.ic_voice_search_off;
    private int mIconLight = com.android.contacts.R.drawable.ic_voice_search_holo_light;
    private int mIconDark = com.android.contacts.R.drawable.ic_voice_search_holo_dark;

    private MenuItem mMenuItem = null;
    private ImageView mImageView = null;
    private boolean mIsIndicatorEnable = false;
    private int mIndicatorIcon = mIconDisable;

    public VoiceSearchIndicator(MenuItem item) {
        mMenuItem = item;
    }

    public VoiceSearchIndicator(ImageView view) {
        mImageView = view;
    }

    /** Bug Fix for ALPS01694037 @{ */
    public VoiceSearchIndicator(ImageView view, TextView textView) {
        mImageView = view;
    }
    /** @} */

    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_UPDATE_INDICATOR_ICON:
                updateIndicator();
                break;
            default:
                Log.i(TAG, "[handleMessage] [vcs] default message.");
                break;
            }
        }
    };

    public void updateIndicator(boolean enable) {
        mIsIndicatorEnable = enable;
        updateIndicator();
    }

    public boolean isIndicatorEnable() {
        return mIsIndicatorEnable;
    }


    private void updateIndicator() {
        if (!isIndicatorEnable()) {
            Log.i(TAG, "[updateIndicator] [vcs] Disable Indicator..");
            mHandler.removeMessages(MSG_UPDATE_INDICATOR_ICON);
            setIndicatorIcon(mIconDisable);
            return;
        }

        if (getIndicatorIcon() == mIconDark) {
            setIndicatorIcon(mIconLight);
        } else {
            setIndicatorIcon(mIconDark);
        }

        Message msg = Message.obtain();
        msg.what = MSG_UPDATE_INDICATOR_ICON;
        mHandler.sendMessageDelayed(msg, DELAY_TIME_INDICATOR);
    }

    private void setIndicatorIcon(int iconRes) {
        mIndicatorIcon = iconRes;
        if (mImageView != null) {
            mImageView.setImageResource(mIndicatorIcon);
        } else {
            mMenuItem.setIcon(mIndicatorIcon);
        }
    }

    private int getIndicatorIcon() {
        return mIndicatorIcon;
    }

    public void setOffIcon(int res) {
        mIconDisable = res;
    }

    public void setDrakIcon(int res) {
        mIconDark = res;
    }

    public void setLightIcon(int res) {
        mIconLight = res;
    }

    /** Bug Fix for ALPS01706025 @{ */
    public void removeHandel() {
        Log.i(TAG, "[removeHandel]mHandler = " + mHandler);
        if (null != mHandler) {
            mHandler.removeMessages(MSG_UPDATE_INDICATOR_ICON);
        }
    }
    /** @} */
}
