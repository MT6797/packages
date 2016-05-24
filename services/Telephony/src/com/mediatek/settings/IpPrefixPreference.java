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
package com.mediatek.settings;

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;

import com.android.internal.telephony.Phone;
import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneGlobals.SubInfoUpdateListener;
import com.android.phone.R;
import com.android.phone.SubscriptionInfoHelper;


public class IpPrefixPreference extends PreferenceActivity
        implements OnPreferenceChangeListener, TextWatcher,
        SubInfoUpdateListener {
    private static final String IP_PREFIX_NUMBER_EDIT_KEY = "button_ip_prefix_edit_key";
    private static final String TAG = "IpPrefixPreference";
    private EditTextPreference mButtonIpPrefix = null;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;
    private Phone mPhone;

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.mtk_ip_prefix_setting);
        mButtonIpPrefix = (EditTextPreference) this.findPreference(IP_PREFIX_NUMBER_EDIT_KEY);
        mButtonIpPrefix.setOnPreferenceChangeListener(this);

        mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        mPhone = mSubscriptionInfoHelper.getPhone();
        if (mPhone == null || (mPhone != null && !TelephonyUtils.isRadioOn(
                mPhone.getSubId(), this))) {
            Log.d(TAG, "onCreate, Phone is null, or radio is off, so finish!!!");
            finish();
            return;
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
            mSubscriptionInfoHelper.setActionBarTitle(
                    actionBar, getResources(), R.string.ip_prefix_setting_lable);
        }

        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
        updateIpPrefix();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mButtonIpPrefix.setSummary(newValue.toString());
        mButtonIpPrefix.setText(newValue.toString());
        if (newValue == null || "".equals(newValue)) {
            mButtonIpPrefix.setSummary(R.string.ip_prefix_edit_default_sum);
        }
        saveIpPrefix(newValue.toString());
        return false;
    }

    private void updateIpPrefix() {
        String preFix = getIpPrefix();
        Log.d(TAG, "preFix: " + preFix);
        if ((preFix != null) && (!"".equals(preFix))) {
            mButtonIpPrefix.setSummary(preFix);
            mButtonIpPrefix.setText(preFix);
        } else {
            mButtonIpPrefix.setSummary(R.string.ip_prefix_edit_default_sum);
            mButtonIpPrefix.setText("");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        switch (itemId) {
        case android.R.id.home:
            finish();
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveIpPrefix(String str) {
        Log.d(TAG, "save str: " + str);
        String key = getIpPrefixKey();
        if (!Settings.System.putString(this.getContentResolver(), key, str)) {
            Log.d(TAG, "Store ip prefix error!");
        }
    }

    private String getIpPrefix() {
        String key = getIpPrefixKey();
        return Settings.System.getString(this.getContentResolver(), key);
    }

    public void beforeTextChanged(CharSequence s, int start,
            int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    public void afterTextChanged(Editable s) {
    }

    @Override
    protected void onDestroy() {
        PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);
        super.onDestroy();
    }

    /**
     * the prefix value key depends on simId.
     * @return
     */
    private String getIpPrefixKey() {
        String key = "ipprefix";
        key += Integer.valueOf(mPhone.getSubId()).toString();
        Log.d(TAG, "getIpPrefixKey key : " + key);
        return key;
    }

    @Override
    public void handleSubInfoUpdate() {
        finish();
    }
}
