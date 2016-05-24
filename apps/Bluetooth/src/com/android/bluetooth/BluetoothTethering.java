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

package com.android.bluetooth;

import android.content.Context;
import android.content.Intent;

import android.net.LinkAddress;
import android.net.ConnectivityManager;
import android.net.INetworkManagementEventObserver;
import android.net.RouteInfo;

import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

//import com.mediatek.xlog.Xlog;

public class BluetoothTethering extends INetworkManagementEventObserver.Stub {

    private Context mContext;

    private static final String TAG = "BluetoothTethering";

    private static BluetoothTethering sBtTethering = null;

    private INetworkManagementService mNetworkManagementService;

    private String[] mTetherableBTRegexs;

    private static boolean sBtTetheringRegistered = false;

    public static final String BLUETOOTH_INTERFACE_ADDED =
        "android.bluetooth.BluetoothTethering.BLUETOOTH_INTERFACE_ADDED";

    public static final String BLUETOOTH_INTERFACE_UP =
        "android.bluetooth.BluetoothTethering.BLUETOOTH_INTERFACE_UP";

    public static final String BLUETOOTH_INTERFACE_LINK_UP =
        "android.bluetooth.BluetoothTethering.BLUETOOTH_INTERFACE_LINK_UP";

    public static final String BLUETOOTH_INTERFACE_NAME =
        "android.bluetooth.BluetoothTethering.BLUETOOTH_INTERFACE_NAME";

    public static final String BLUETOOTH_INTERFACE_STATE =
        "android.bluetooth.BluetoothTethering.BLUETOOTH_INTERFACE_STATE";

    private BluetoothTethering() {
        Log.d(TAG, "Tethering starting");
    }

    public static BluetoothTethering getBluetoothTetheringInstance() {
        if (sBtTethering == null) {
            sBtTethering = new BluetoothTethering();
            return sBtTethering;
        } else {
            return sBtTethering;
        }
    }

    public void registerBTTether(Context context) {
        if (!sBtTetheringRegistered) {
            Log.d(TAG, "BluetoothTethering setContext");
            mContext = context;
            // register for notifications from NetworkManagement Service
            IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
            mNetworkManagementService = INetworkManagementService.Stub.asInterface(b);
            if (mNetworkManagementService == null) {
                Log.e(TAG, "Error get INetworkManagementService");
                return;
            }
            try {
                mNetworkManagementService.registerObserver(this);
            } catch (RemoteException e) {
                Log.e(TAG, "Error registering observer :" + e);
            }

            ConnectivityManager cm = (ConnectivityManager) mContext
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            mTetherableBTRegexs = cm.getTetherableBluetoothRegexs();
            Log.d(TAG, "registerBTTether bt regexs: " + mTetherableBTRegexs[0]);

            sBtTetheringRegistered = true;
        }
    }

    public void unregisterBTTether() {
        if (sBtTetheringRegistered) {
            Log.d(TAG, "unregister Bluetooth Tether");
            try {
                mNetworkManagementService.unregisterObserver(this);
            } catch (RemoteException e) {
                Log.e(TAG, "Error registering observer :" + e);
            }
            sBtTetheringRegistered = false;
        }
    }

    private boolean isBT(String iface) {
        for (String regex : mTetherableBTRegexs) {
            if (iface.matches(regex)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Interface configuration status has changed.
     *
     * @param iface The interface.
     * @param up True if the interface has been enabled.
     */
    public void interfaceStatusChanged(String iface, boolean up) {
        Log.d(TAG, "interfaceStatusChanged, iface: " + iface + " up: " + up);
        if (!isBT(iface)) {
            return;
        }

        Log.d(TAG, "interfaceAdded :" + iface);
        Intent in = new Intent(BLUETOOTH_INTERFACE_UP);
        in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        in.putExtra(BLUETOOTH_INTERFACE_NAME, iface);
        in.putExtra(BLUETOOTH_INTERFACE_STATE, up);
        mContext.sendBroadcast(in);
    }

    /**
     * Interface physical-layer link state has changed. For Ethernet, this
     * method is invoked when the cable is plugged in or unplugged.
     *
     * @param iface The interface.
     * @param up True if the physical link-layer connection signal is valid.
     */
    public void interfaceLinkStateChanged(String iface, boolean up) {
        Log.d(TAG, "interfaceLinkStateChanged, iface: " + iface + " up: " + up);
        if (!isBT(iface)) {
            return;
        }

        Log.d(TAG, "interfaceAdded :" + iface);
        Intent in = new Intent(BLUETOOTH_INTERFACE_LINK_UP);
        in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        in.putExtra(BLUETOOTH_INTERFACE_NAME, iface);
        in.putExtra(BLUETOOTH_INTERFACE_STATE, up);
        mContext.sendBroadcast(in);
    }

    /**
     * An interface has been added to the system
     *
     * @param iface The interface.
     */
    public void interfaceAdded(String iface) {
        Log.d(TAG, "interfaceAdded, iface: " + iface);
        if (!isBT(iface)) {
            // Log.d(mTAG, iface + " is not a BT tetherable iface, ignoring");
            return;
        }

        Log.d(TAG, "interfaceAdded :" + iface);
        Intent in = new Intent(BLUETOOTH_INTERFACE_ADDED);
        in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        in.putExtra(BLUETOOTH_INTERFACE_NAME, iface);
        mContext.sendBroadcast(in);
    }

    /**
     * An interface has been removed from the system
     *
     * @param iface The interface.
     */
    public void interfaceRemoved(String iface) {
    }

    /* L Migration @{ */
    public void addressUpdated(String iface, LinkAddress address) {
    }

    public void addressRemoved(String iface, LinkAddress address) {
    }
    /* @} */

    /**
     * A networking quota limit has been reached. The quota might not be
     * specific to an interface.
     *
     * @param limitName The name of the limit that triggered.
     * @param iface The interface on which the limit was detected.
     */
    public void limitReached(String limitName, String iface) {
    }

    /* L Migration @{ */
    public void interfaceClassDataActivityChanged(String label, boolean active, long tsNanos) {
    }

    public void interfaceDnsServerInfo(String iface, long lifetime, String[] servers) {
    }

    public void routeUpdated(RouteInfo route) {
    }

    public void routeRemoved(RouteInfo route) {
    }
    /* @} */

    /** M: ipv6 tethering @{ */
    public void interfaceStatusChangedIpv6(String iface, boolean up) {
        // Ignored.
    }
    /** @} */

    /* MR1 added interface. */
    public void interfaceClassDataActivityChanged(String label, boolean active){
    }

    /**
     * An interface address has been added or updated
     *
     * @param address The address.
     * @param iface The interface.
     * @param flags The address flags.
     * @param scope The address scope.
     */
    public void addressUpdated(String address, String iface, int flags, int scope) {
    }

    /**
     * An interface address has been removed
     *
     * @param address The address.
     * @param iface The interface.
     * @param flags The address flags.
     * @param scope The address scope.
     */
    public void addressRemoved(String address, String iface, int flags, int scope) {
    }
}
