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
 */

package com.mediatek.settings;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.android.internal.view.RotationPolicy;
import com.android.internal.view.RotationPolicy.RotationPolicyListener;
import com.android.settings.DropDownPreference;
import com.android.settings.R;

import com.mediatek.hdmi.HdmiDef;
import com.mediatek.hdmi.IMtkHdmiManager;

import com.mediatek.settings.ext.IDisplaySettingsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.ext.IStatusBarPlmnDisplayExt;

import java.util.List;

public class DisplaySettingsExt implements OnPreferenceClickListener {
    private static final String TAG = "mediatek.DisplaySettings";
    // add for HDMI Settings @ {
    private static final String KEY_HDMI_SETTINGS = "hdmi_settings";
    private Preference mHdmiSettings;
    private IMtkHdmiManager mHdmiManager;
    // @ }

    private static final String KEY_SCREEN_SAVER = "screensaver";

    private static final int TYPE_PREFERENCE = 0;
    private static final int TYPE_CHECKBOX = 1;
    private static final int TYPE_LIST = 2;

    // Declare the first preference ClearMotion order here,
    // other preference order over this value.
    private static final int PREFERENCE_ORDER_FIRST = -100;

    private Context mContext;

    // add for MiraVision @ {
    private static final String KEY_MIRA_VISION = "mira_vision";
    private Preference mMiraVision;
    private Intent mMiraIntent = new Intent("com.android.settings.MIRA_VISION");
    // @ }

    // add for clearMotion @ {
    private static final String KEY_CLEAR_MOTION = "clearMotion";
    private Preference mClearMotion;
    // @}

    // timeout preference key
    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";
    private Preference mScreenTimeoutPreference;

    // plugin to add PlnmName display
    private IStatusBarPlmnDisplayExt mPlmnName;

    private ISettingsMiscExt mExt;
    // ALPS01751214 Rotate function on statusbar cannot matched in display settings @ {
    private DropDownPreference mRotatePreference;

    public DisplaySettingsExt(Context context) {
        Log.d(TAG, "DisplaySettingsExt");
        mContext = context;
    }

    /**
     *
     * @param type : 0:Preference;
                     1:CheckBoxPreference;
     *               2:ListPreference
     * @param titleRes
     * @param key
     * @return
     */
    private Preference createPreference(int type, int titleRes, String key) {
        Preference preference = null;
        switch (type) {
        case TYPE_PREFERENCE:
            preference = new Preference(mContext);
            break;
        case TYPE_CHECKBOX:
            preference = new CheckBoxPreference(mContext);
            preference.setOnPreferenceClickListener(this);
            break;
        case TYPE_LIST:
            preference = new ListPreference(mContext);
            preference.setOnPreferenceClickListener(this);
            break;
        default:
            break;
        }
        preference.setKey(key);
        preference.setTitle(titleRes);
        return preference;
    }

    /*
     * initPreference:
     *    1. new all mtk feature preference
     *
     * @screen : UI Screen
     */
    private void initPreference(PreferenceScreen screen) {
        // add for clearMotion @ {
        mClearMotion = createPreference(TYPE_PREFERENCE, R.string.clear_motion_title,
                KEY_CLEAR_MOTION);
        mClearMotion.setOrder(PREFERENCE_ORDER_FIRST);
        mClearMotion.setSummary(R.string.clear_motion_summary);
        if (FeatureOption.MTK_CLEARMOTION_SUPPORT) {
            screen.addPreference(mClearMotion);
        }
        // @ }
        // add for MiraVision @ {
        mMiraVision = createPreference(TYPE_PREFERENCE, R.string.mira_vision_title,
                KEY_MIRA_VISION);
        mMiraVision.setSummary(R.string.mira_vision_summary);
        mMiraVision.setOrder(PREFERENCE_ORDER_FIRST + 1);
        if (FeatureOption.MTK_MIRAVISION_SETTING_SUPPORT && !(android.os.UserHandle.myUserId() != 0
                && FeatureOption.MTK_PRODUCT_IS_TABLET)) {
            Log.d(TAG, "No MiraVision support");
            screen.addPreference(mMiraVision);
        }
        // @ }
        // add for HDMI Settings @ {
        mHdmiManager = IMtkHdmiManager.Stub.asInterface(ServiceManager
                .getService(Context.HDMI_SERVICE));
        if (mHdmiManager != null) {
            mHdmiSettings = createPreference(TYPE_PREFERENCE, R.string.hdmi_settings,
                    KEY_HDMI_SETTINGS);
            mHdmiSettings.setSummary(R.string.hdmi_settings_summary);
            mHdmiSettings.setFragment("com.mediatek.hdmi.HdmiSettings");
            try {
                String hdmi = mContext.getString(R.string.hdmi_replace_hdmi);
                if (mHdmiManager.getDisplayType() == HdmiDef.DISPLAY_TYPE_MHL) {
                    String mhl = mContext.getString(R.string.hdmi_replace_mhl);
                    mHdmiSettings.setTitle(mHdmiSettings.getTitle().toString()
                            .replaceAll(hdmi, mhl));
                    mHdmiSettings.setSummary(mHdmiSettings.getSummary().toString().replaceAll(hdmi,
                            mhl));
                } else if (mHdmiManager.getDisplayType() == HdmiDef.DISPLAY_TYPE_SLIMPORT) {
                    String slimport = mContext.getString(R.string.slimport_replace_hdmi);
                    mHdmiSettings.setTitle(mHdmiSettings.getTitle().toString()
                            .replaceAll(hdmi, slimport));
                    mHdmiSettings.setSummary(mHdmiSettings.getSummary().toString().replaceAll(hdmi,
                            slimport));
                }
            } catch (RemoteException e) {
                Log.d(TAG, "getDisplayType RemoteException");
            }
            mHdmiSettings.setOrder(PREFERENCE_ORDER_FIRST + 2);
            screen.addPreference(mHdmiSettings);
        }
        // @ }

        // find timeout preference
        mScreenTimeoutPreference = screen.findPreference(KEY_SCREEN_TIMEOUT);
        mExt = UtilsExt.getMiscPlugin(mContext);
        mExt.setTimeoutPrefTitle(mScreenTimeoutPreference);

        // call plugin to add PLMN name display
        mPlmnName = UtilsExt.getStatusBarPlmnPlugin(mContext);
        mPlmnName.createCheckBox(screen, 1000);

        // remove Daydream when MTK_GMO_RAM_OPTIMIZE is true
        if (screen.findPreference(KEY_SCREEN_SAVER) != null
                && FeatureOption.MTK_GMO_RAM_OPTIMIZE) {
            screen.removePreference(screen.findPreference(KEY_SCREEN_SAVER));
        }

        /// M: add for plugin Settings @ {
        IDisplaySettingsExt displayExt = UtilsExt.getDisplaySettingsPlugin(mContext);
        displayExt.addPreference(mContext, screen);
        /// @ }
    }

    public void onCreate(PreferenceScreen screen) {
        if (FeatureOption.MTK_A1_FEATURE) {
            return;
        }
        Log.d(TAG, "onCreate");
        initPreference(screen);
    }

    public void onResume() {
        if (FeatureOption.MTK_A1_FEATURE) {
            return;
        }
        Log.d(TAG, "onResume of DisplaySettings");
        // ALPS01751214 Rotate function on statusbar cannot matched in display settings @ {
        if (RotationPolicy.isRotationSupported(mContext)) {
            RotationPolicy.registerRotationPolicyListener(mContext,
                    mRotationPolicyListener);
        }
        // @ }
    }

    public void onPause() {
        if (FeatureOption.MTK_A1_FEATURE) {
            return;
        }
        Log.d(TAG, "onPause of DisplaySettings");
        // ALPS01751214 Rotate function on statusbar cannot matched in display settings @ {
        if (RotationPolicy.isRotationSupported(mContext)) {
            RotationPolicy.unregisterRotationPolicyListener(mContext,
                    mRotationPolicyListener);
        }
        // @ }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (FeatureOption.MTK_A1_FEATURE) {
            return false;
        }
        if (preference == mClearMotion) {
            // add for clearMotion
            Intent intent = new Intent();
            intent.setClass(mContext, ClearMotionSettings.class);
            mContext.startActivity(intent);
        } else if (preference == mMiraVision) {
            // add for MiraVision
            mContext.startActivity(mMiraIntent);
        }

        return true;
    }

    // ALPS01751214 Rotate function on statusbar cannot matched in display settings @ {
    public void setRotatePreference(DropDownPreference preference) {
        mRotatePreference = preference;
    }

    private RotationPolicyListener mRotationPolicyListener = new RotationPolicyListener() {
        @Override
        public void onChange() {
            if (mRotatePreference != null) {
                mRotatePreference.setSelectedItem(RotationPolicy.isRotationLocked(mContext) ?
                      1 : 0);
            }
        }
    };
    // @ }
}
