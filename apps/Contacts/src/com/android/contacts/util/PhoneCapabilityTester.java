/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2010 The Android Open Source Project
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
 * limitations under the License.
 */

package com.android.contacts.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.net.sip.SipManager;
import android.provider.MediaStore;
import android.provider.Telephony;
import android.telephony.TelephonyManager;

import com.android.contacts.R;

import com.android.contacts.common.ContactsUtils;

import com.mediatek.contacts.util.Log;

import java.util.List;

/**
 * Provides static functions to quickly test the capabilities of this device. The static
 * members are not safe for threading
 */
public final class PhoneCapabilityTester {
    private static boolean sIsInitialized;
    private static boolean sIsPhone;
    private static boolean sIsSipPhone;
    private static boolean sIsSms;
    private static final String TAG = "PhoneCapabilityTester";

    /**
     * Tests whether the Intent has a receiver registered. This can be used to show/hide
     * functionality (like Phone, SMS)
     */
    public static boolean isIntentRegistered(Context context, Intent intent) {
        final PackageManager packageManager = context.getPackageManager();
        final List<ResolveInfo> receiverList = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return receiverList.size() > 0;
    }

    /**
     * Returns true if this device can be used to make phone calls
     */
    public static boolean isPhone(Context context) {
        if (!sIsInitialized) initialize(context);
        // Is the device physically capabable of making phone calls?
        Log.d(TAG,"isPhone,sIsPhone = " + sIsPhone);
        return sIsPhone;
    }

    private static void initialize(Context context) {
        final TelephonyManager telephonyManager
                = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        sIsPhone = telephonyManager.isVoiceCapable();
        Log.d(TAG,"initialize,sIsPhone = " + sIsPhone);
        sIsSms = telephonyManager.isSmsCapable();
        sIsSipPhone = sIsPhone && SipManager.isVoipSupported(context);
        sIsInitialized = true;
    }

    /**
     * Returns true if this device can be used to make sip calls
     */
    public static boolean isSipPhone(Context context) {
        if (!sIsInitialized) initialize(context);
        return sIsSipPhone;
    }

    /**
     * Returns the component name to use for sending to sms or null.
     */
    public static ComponentName getSmsComponent(Context context) {
        String smsPackage = Telephony.Sms.getDefaultSmsPackage(context);
        if (smsPackage != null) {
            final PackageManager packageManager = context.getPackageManager();
            final Intent intent = new Intent(Intent.ACTION_SENDTO,
                    Uri.fromParts(ContactsUtils.SCHEME_SMSTO, "", null));
            final List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 0);
            for (ResolveInfo resolveInfo : resolveInfos) {
                if (smsPackage.equals(resolveInfo.activityInfo.packageName)) {
                    return new ComponentName(smsPackage, resolveInfo.activityInfo.name);
                }
            }
        }
        return null;
    }

    /**
     * Returns true if there is a camera on the device
     */
    public static boolean isCameraIntentRegistered(Context context) {
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        return isIntentRegistered(context, intent);
    }

    /**
     * M
     * Return true if this device support SMS feature.
     */
    public static boolean isSupportSms(Context context) {
        if (!sIsInitialized) {
            initialize(context);
        }
        Log.i(TAG, "sIsSms : " + sIsSms);
        return sIsSms;
    }

    /**
     * M
     * @return True if we are using two-pane layouts ("tablet mode"),
     * false if we are using single views ("phone mode")
     */
    public static boolean isUsingTwoPanes(Context context) {
        return context.getResources().getBoolean(R.bool.config_use_two_panes);
    }
}
