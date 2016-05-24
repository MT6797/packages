/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package com.android.phone.vvm.omtp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telecom.PhoneAccountHandle;

import com.android.phone.PhoneUtils;
import com.android.phone.settings.VisualVoicemailSettingsUtil;
import com.android.phone.vvm.omtp.sync.OmtpVvmSourceManager;

import java.util.Set;

/**
 * When a new package is installed, check if it matches any of the vvm carrier apps of the currently
 * enabled dialer vvm sources.
 */
public class VvmPackageInstallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getData() == null) {
            return;
        }

        String packageName = intent.getData().getSchemeSpecificPart();
        if (packageName == null) {
            return;
        }

        OmtpVvmSourceManager vvmSourceManager = OmtpVvmSourceManager.getInstance(context);
        Set<PhoneAccountHandle> phoneAccounts = vvmSourceManager.getOmtpVvmSources();
        for (PhoneAccountHandle phoneAccount : phoneAccounts) {
            if (VisualVoicemailSettingsUtil.isVisualVoicemailUserSet(context, phoneAccount)) {
                // Skip the check if this voicemail source's setting is overridden by the user.
                continue;
            }

            OmtpVvmCarrierConfigHelper carrierConfigHelper = new OmtpVvmCarrierConfigHelper(
                    context, PhoneUtils.getSubIdForPhoneAccountHandle(phoneAccount));
            if (packageName.equals(carrierConfigHelper.getCarrierVvmPackageName())) {
                VisualVoicemailSettingsUtil.setVisualVoicemailEnabled(
                        context, phoneAccount, false, false);
                OmtpVvmSourceManager.getInstance(context).removeSource(phoneAccount);
            }
        }
    }
}
