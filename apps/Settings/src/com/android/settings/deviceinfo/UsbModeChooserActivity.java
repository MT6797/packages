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
 * limitations under the License.
 */

package com.android.settings.deviceinfo;

import android.annotation.Nullable;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserManager;

import com.android.settings.R;

/**
 * UI for the USB chooser dialog.
 *
 */
public class UsbModeChooserActivity extends Activity {

    private UsbManager mUsbManager;
    private String[] mFunctions;
    private boolean mIsUnlocked;

    /// M: Add for Built-in CD-ROM.
    private static final String PROPERTY_USB_BICR = "ro.sys.usb.bicr";
    private static final String FUNCTION_SUPPORT = "yes";
    private static final String FUNCTION_NOT_SUPPORT = "no";

    /// M: Add for USB Mass Storage.
    private static final String PROPERTY_USB_TYPE = "ro.sys.usb.storage.type";
    private static final String DEFAULT_USB_TYPE = "mtp";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Intent i = getBaseContext().registerReceiver(null, new IntentFilter(UsbManager.ACTION_USB_STATE));
        mIsUnlocked = i.getBooleanExtra(UsbManager.USB_DATA_UNLOCKED, false);

        super.onCreate(savedInstanceState);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        boolean isFileTransferRestricted = ((UserManager) getSystemService(Context.USER_SERVICE))
                .hasUserRestriction(UserManager.DISALLOW_USB_FILE_TRANSFER);
        CharSequence[] items;
        if (isFileTransferRestricted) {
            items = new CharSequence[] { getText(R.string.usb_use_charging_only), getText(R.string.usb_use_MIDI)};
            mFunctions = new String[] { null, UsbManager.USB_FUNCTION_MIDI };
        } else {
            items = new CharSequence[] {
                    getText(R.string.usb_use_charging_only), getText(R.string.usb_use_file_transfers),
                    getText(R.string.usb_use_photo_transfers), getText(R.string.usb_use_MIDI)};
            mFunctions = new String[] { null, UsbManager.USB_FUNCTION_MTP,
                    UsbManager.USB_FUNCTION_PTP, UsbManager.USB_FUNCTION_MIDI };

            items = addCustomizationUsbType(items);
        }

        final AlertDialog levelDialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.usb_use);

        builder.setSingleChoiceItems(items, getCurrentFunction(),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!ActivityManager.isUserAMonkey()) {
                            setCurrentFunction(which);
                        }
                        dialog.dismiss();
                        UsbModeChooserActivity.this.finish();
                    }
                });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                UsbModeChooserActivity.this.finish();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                UsbModeChooserActivity.this.finish();
            }
        });
        levelDialog = builder.create();
        levelDialog.show();
    }

    private int getCurrentFunction() {
        if (!mIsUnlocked) {
            return 0;
        }

        for (int i = 1; i < mFunctions.length; i++) {
            if (mUsbManager.isFunctionEnabled(mFunctions[i])) {
                return i;
            }
        }
        return 0;
    }

    private void setCurrentFunction(int which) {
        if (which == 0) {
            mUsbManager.setCurrentFunction(null);
            mUsbManager.setUsbDataUnlocked(false);
            return;
        }

        mUsbManager.setCurrentFunction(mFunctions[which]);
        mUsbManager.setUsbDataUnlocked(true);
    }

    private CharSequence[] addCustomizationUsbType(CharSequence[] items) {
        /// M: Add USB Mass Storage Mode.
        String umsConfig = android.os.SystemProperties.get(
                PROPERTY_USB_TYPE, DEFAULT_USB_TYPE);
        boolean umsSupport = umsConfig.equals(UsbManager.USB_FUNCTION_MTP + ","
                + UsbManager.USB_FUNCTION_MASS_STORAGE);

        /// M: Add Built-in CD-ROM Mode.
        String bicrConfig = SystemProperties.get(PROPERTY_USB_BICR, FUNCTION_NOT_SUPPORT);
        boolean bicrSupport = bicrConfig.equals(FUNCTION_SUPPORT);

        if (umsSupport && bicrSupport) {
            items = new CharSequence[] {
                     getText(R.string.usb_use_charging_only),
                     getText(R.string.usb_use_file_transfers),
                     getText(R.string.usb_use_photo_transfers),
                     getText(R.string.usb_use_mass_storage),
                     getText(R.string.usb_use_MIDI),
                     getText(R.string.usb_use_built_in_cd_rom)};
            mFunctions = new String[] {
                     null,
                     UsbManager.USB_FUNCTION_MTP,
                     UsbManager.USB_FUNCTION_PTP,
                     UsbManager.USB_FUNCTION_MASS_STORAGE,
                     UsbManager.USB_FUNCTION_MIDI,
                     UsbManager.USB_FUNCTION_BICR};
        } else if (umsSupport) {
            items = new CharSequence[] {
                     getText(R.string.usb_use_charging_only),
                     getText(R.string.usb_use_file_transfers),
                     getText(R.string.usb_use_photo_transfers),
                     getText(R.string.usb_use_mass_storage),
                     getText(R.string.usb_use_MIDI),
                     getText(R.string.usb_use_built_in_cd_rom)};
            mFunctions = new String[] {
                     null,
                     UsbManager.USB_FUNCTION_MTP,
                     UsbManager.USB_FUNCTION_PTP,
                     UsbManager.USB_FUNCTION_MASS_STORAGE,
                     UsbManager.USB_FUNCTION_MIDI};
        } else if (bicrSupport) {
            items = new CharSequence[] {
                     getText(R.string.usb_use_charging_only),
                     getText(R.string.usb_use_file_transfers),
                     getText(R.string.usb_use_photo_transfers),
                     getText(R.string.usb_use_MIDI),
                     getText(R.string.usb_use_built_in_cd_rom)};
            mFunctions = new String[] {
                     null,
                     UsbManager.USB_FUNCTION_MTP,
                     UsbManager.USB_FUNCTION_PTP,
                     UsbManager.USB_FUNCTION_MIDI,
                     UsbManager.USB_FUNCTION_BICR};
        }

        return items;
    }
}
