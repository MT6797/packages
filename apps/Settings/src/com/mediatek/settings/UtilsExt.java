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

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.storage.IMountService;
import android.os.RemoteException;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;


import java.net.Inet4Address;
import java.net.Inet6Address;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Locale;


import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import com.android.internal.widget.LockPatternUtils;
import com.mediatek.common.MPlugin;
import com.mediatek.storage.StorageManagerEx;
import com.mediatek.settings.ext.*;
import com.android.settings.R;


public class UtilsExt {
    private static final String TAG = "UtilsExt";

    ///M: DHCPV6 change feature
    private static final String INTERFACE_NAME = "wlan0";
    private static final int BEGIN_INDEX = 0;
    private static final int SEPARATOR_LENGTH = 2;
    // for HetComm feature
    public static final String PKG_NAME_HETCOMM = "com.mediatek.hetcomm";

    // disable apps list file location
    private static final String FILE_DISABLE_APPS_LIST = "/system/etc/disableapplist.txt";
    // read the file to get the need special disable app list
    public static ArrayList<String> disableAppList = readFile(FILE_DISABLE_APPS_LIST);

    /**
     * Returns the WIFI IP Addresses, if any, taking into account IPv4 and IPv6 style addresses.
     * @return the formatted and comma-separated IP addresses, or null if none.
     */
    public static String getWifiIpAddresses() {
        NetworkInterface wifiNetwork = null;
        String addresses = "";
        try {
            wifiNetwork = NetworkInterface.getByName(INTERFACE_NAME);
        } catch (SocketException e) {
            e.printStackTrace();
            return null;
        }
        if (wifiNetwork == null) {
            Log.d(TAG, "wifiNetwork is null");
            return null;
        }
        Enumeration<InetAddress> enumeration = wifiNetwork.getInetAddresses();
        if (enumeration == null) {
            Log.d(TAG, "enumeration is null");
            return null;
        }
        while (enumeration.hasMoreElements()) {
            InetAddress inet = enumeration.nextElement();
            String hostAddress = inet.getHostAddress();
            if (hostAddress.contains("%")) {
                hostAddress = hostAddress.substring(BEGIN_INDEX,
                        hostAddress.indexOf("%")); // remove %10, %wlan0
            }
            Log.d(TAG, "InetAddress = " + inet.toString());
            Log.d(TAG, "hostAddress = " + hostAddress);
            if (inet instanceof Inet6Address) {
                Log.d(TAG, "IPV6 address = " + hostAddress);
                addresses += hostAddress + "; ";
            } else if (inet instanceof Inet4Address) {
                Log.d(TAG, "IPV4 address = " + hostAddress);
                addresses = hostAddress + ", " + addresses;
            }
        }
        Log.d(TAG, "IP addresses = " + addresses);
        if (!("").equals(addresses) && (addresses.endsWith(", ") || addresses.endsWith("; "))) {
            addresses = addresses.substring(BEGIN_INDEX, addresses.length() - SEPARATOR_LENGTH);
        } else if (("").equals(addresses)) {
            addresses = null;
        }
        Log.d(TAG, "The result of IP addresses = " + addresses);
        return addresses;
    }

    /* M: create settigns plugin object
     * @param context Context
     * @return ISettingsMiscExt
     */
    public static ISettingsMiscExt getMiscPlugin(Context context) {
        ISettingsMiscExt ext;
        ext = (ISettingsMiscExt) MPlugin.createInstance(
                     ISettingsMiscExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultSettingsMiscExt(context);
        }
        return ext;
    }

    /**
     * M: create wifi plugin object
     * @param context Context
     * @return IWifiExt
     */
    public static IWifiExt getWifiPlugin(Context context) {
        IWifiExt ext;
        ext = (IWifiExt) MPlugin.createInstance(
                     IWifiExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultWifiExt(context);
        }
        return ext;
    }
    /**
     * M: create wifi settings plugin object
     * @param context Context context
     * @return IWifiSettingsExt
     */
    public static IWifiSettingsExt getWifiSettingsPlugin(Context context) {
        IWifiSettingsExt ext;
        ext = (IWifiSettingsExt) MPlugin.createInstance(
                     IWifiSettingsExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultWifiSettingsExt();
        }
        return ext;
    }

    /**
     * M: create wifi ap dialog plugin object
     * @param context Context
     * @return IWifiApDialogExt
     */
    public static IWifiApDialogExt getWifiApDialogPlugin(Context context) {
        IWifiApDialogExt ext;
        ext = (IWifiApDialogExt) MPlugin.createInstance(
                     IWifiApDialogExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultWifiApDialogExt();
        }
        return ext;
    }


    /**
    * M: create SMS ap dialog plugin object
    * @param context Context
    * @return ISmsDialogExt
    */
    public static ISmsDialogExt getSMSApDialogPlugin(Context context) {
        ISmsDialogExt ext;
        ext = (ISmsDialogExt) MPlugin.createInstance(
                    ISmsDialogExt.class.getName(), context);
        if (ext == null) {
        ext = new DefaultSmsDialogExt(context);
        }
        return ext;
    }

    /**
    * M: create Wireless plugin plugin object
    * @param context Context
    * @return ISmsDialogExt
    */
    public static ISmsPreferenceExt getSmsPreferencePlugin(Context context) {
        ISmsPreferenceExt ext;
        ext = (ISmsPreferenceExt) MPlugin.createInstance(
                    ISmsPreferenceExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultSmsPreferenceExt();
        }
        return ext;
    }


    /**
    * M: create App list  plugin object
    * @param context Context
    * @return ISmsDialogExt
    */
    public static IAppListExt getAppListPlugin(Context context) {
        IAppListExt ext;
        ext = (IAppListExt) MPlugin.createInstance(
        IAppListExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultAppListExt(context);
        }
        return ext;
    }

    //M: for Development Settings
    public static IDevExt getDevExtPlugin(Context context) {
        IDevExt ext;
        ext = (IDevExt) MPlugin.createInstance(
                IDevExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultDevExt(context);
        }
        return ext;
    }

    /**
     * M: create device info settings plugin object
     * @param context Context
     * @return IDeviceInfoSettingsExt
     */
    public static IDeviceInfoSettingsExt getDeviceInfoSettingsPlugin(Context context) {
        IDeviceInfoSettingsExt ext;
        ext = (IDeviceInfoSettingsExt) MPlugin.createInstance(
                     IDeviceInfoSettingsExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultDeviceInfoSettingsExt();
        }
        return ext;
    }

    /**to judge the packageName apk is installed or not.
     * * @param context Context
     * @param packageName name of package
     * @return true if the package is exist
     */
    public static boolean isPackageExist(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName,
                    PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException  e) {
            return false;
        }
        return true;
    }

    /**
     * M: create audio profile plugin object.
     *
     * @param context
     *            Context
     * @return IAudioProfileExt
     */
    public static IAudioProfileExt getAudioProfilePlugin(Context context) {
        IAudioProfileExt ext;
        ext = (IAudioProfileExt) MPlugin.createInstance(
                     IAudioProfileExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultAudioProfileExt(context);
        }
        return ext;
    }

    public static IRCSSettings getRcsSettingsPlugin(Context context) {
        IRCSSettings ext = null;
        ext = (IRCSSettings) MPlugin.createInstance(
                     IRCSSettings.class.getName(), context);
        if (ext == null) {
            ext = new DefaultRCSSettings();
        }
        return ext;
    }

    public static IWWOPJoynSettingsExt getJoynSettingsPlugin(Context context) {
        IWWOPJoynSettingsExt ext = null;
        ext = (IWWOPJoynSettingsExt) MPlugin.createInstance(
                     IWWOPJoynSettingsExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultWWOPJoynSettingsExt();
        }
        return ext;
    }

    //M: Add for MTK in house Data-Protection.
    public static IDataProtectionExt getDataProectExtPlugin(Context context) {
        IDataProtectionExt ext;
        ext = (IDataProtectionExt) MPlugin.createInstance(
                     IDataProtectionExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultDataProtectionExt(context);
        }
        return ext;
    }

    //M: Add for Privacy Protection Lock Settings Entry.
    public static IPplSettingsEntryExt getPrivacyProtectionLockExtPlugin(Context context) {
        IPplSettingsEntryExt ext;
        ext = (IPplSettingsEntryExt) MPlugin.createInstance(
                     IPplSettingsEntryExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultPplSettingsEntryExt(context);
        }
        return ext;
    }

    //M: Add for Mediatek-DM Permission Control.
    public static IMdmPermissionControlExt getMdmPermControlExtPlugin(Context context) {
        IMdmPermissionControlExt ext;
        ext = (IMdmPermissionControlExt) MPlugin.createInstance(
                     IMdmPermissionControlExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultMdmPermControlExt(context);
        }
        return ext;
    }

    //M: Add for MTK in house Permission Control.
    public static IPermissionControlExt getPermControlExtPlugin(Context context) {
        IPermissionControlExt ext;
        ext = (IPermissionControlExt) MPlugin.createInstance(
                     IPermissionControlExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultPermissionControlExt(context);
        }
        return ext;
    }

    /**
     * For sim management update preference
     * @param context Context
     * @return ISimManagementExt
     */
    public static ISimManagementExt getSimManagmentExtPlugin(Context context) {
        ISimManagementExt ext;
        ext = (ISimManagementExt) MPlugin.createInstance(
                     ISimManagementExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultSimManagementExt();
        }
        return ext;
    }

    /**
     * create APN settings plug-in object
     * @param context Context
     * @return IApnSettingsExt
     */
    public static IApnSettingsExt getApnSettingsPlugin(Context context) {
        IApnSettingsExt ext;
        ext = (IApnSettingsExt) MPlugin.createInstance(
                     IApnSettingsExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultApnSettingsExt();
        }
        return ext;
    }

    /**
     * create DataUsageSummary plug-in object
     * @param context Context
     * @return IDataUsageSummaryExt
     */
    public static IDataUsageSummaryExt getDataUsageSummaryPlugin(Context context) {
        IDataUsageSummaryExt ext;
        ext = (IDataUsageSummaryExt) MPlugin.createInstance(
                     IDataUsageSummaryExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultDataUsageSummaryExt(context);
        }
        return ext;
    }

    /**
     * create Sim Roaming plug-in object
     * @param context Context
     * @return ISimRoamingExt
     */
    public static ISimRoamingExt getSimRoamingExtPlugin(Context context) {
        ISimRoamingExt ext;
        ext = (ISimRoamingExt) MPlugin.createInstance(
                     ISimRoamingExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultSimRoamingExt();
        }
        return ext;
    }

    /**
     * create Rcse APN plug-in object
     * @param context
     * @return IRcseOnlyApnExtension
     */
    public static IRcseOnlyApnExt getRcseApnPlugin(Context context) {
        IRcseOnlyApnExt ext = null;
        ext = (IRcseOnlyApnExt) MPlugin.createInstance(
                     IRcseOnlyApnExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultRcseOnlyApnExt();
        }
        return ext;
    }

    /**
     * create Sim status plug-in object
     * @param context Context
     * @return IStatusExt
     */
    public static IStatusExt getStatusExtPlugin(Context context) {
        IStatusExt ext;
        ext = (IStatusExt) MPlugin.createInstance(
                     IStatusExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultStatusExt();
        }
        return ext;
    }

      /*** M: for update status of operator name.
     * @param context Context
     * @return IStatusExt
     */
    public static IStatusBarPlmnDisplayExt getStatusBarPlmnPlugin(Context context) {
        IStatusBarPlmnDisplayExt ext;
        ext = (IStatusBarPlmnDisplayExt) MPlugin.createInstance(
                     IStatusBarPlmnDisplayExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultStatusBarPlmnDisplayExt(context);
        }
        return ext;
    }

    //M: For Apps
    public static IAppsExt getAppsExtPlugin(Context context) {
        IAppsExt ext;
        ext = (IAppsExt) MPlugin.createInstance(
                IAppsExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultAppsExt(context);
        }
        return ext;
    }

    /*** M: for Wfc settings.
     * @param context Context
     * @return IWfcSettingsExt
     */
    public static IWfcSettingsExt getWfcSettingsPlugin(Context context) {
        IWfcSettingsExt ext = null;
        ext = (IWfcSettingsExt) MPlugin.createInstance(
                     IWfcSettingsExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultWfcSettingsExt();
        }
        return ext;
    }

    /*** M: for display settings.
     * @param context Context
     * @return IdisplayExt
     */
    public static IDisplaySettingsExt getDisplaySettingsPlugin(Context context) {
        IDisplaySettingsExt ext = null;
        ext = (IDisplaySettingsExt) MPlugin.createInstance(
                     IDisplaySettingsExt.class.getName(), context);
        if (ext == null) {
            ext = new DefaultDisplaySettingsExt(context);
        }
        return ext;
    }

    /**
     * read the file by line
     * @param path path
     * @return ArrayList
     */
    public static ArrayList<String> readFile(String path) {
         ArrayList<String> appsList = new ArrayList<String>();
         appsList.clear();
         File file = new File(path);
          FileReader fr = null;
          BufferedReader br = null;
         try {
               if (file.exists()) {
                   fr = new FileReader(file);
              } else {
                  Log.d(TAG, "file in " + path + " does not exist!");
                  return null;
             }
               br = new BufferedReader(fr);
               String line;
               while ((line = br.readLine()) != null) {
                     Log.d(TAG, " read line " + line);
                     appsList.add(line);
               }
               return appsList;
         } catch (IOException io) {
                Log.d(TAG, "IOException");
                 io.printStackTrace();
         } finally {
                   try {
                      if (br != null) {
                          br.close();
                         }
                      if (fr != null) {
                         fr.close();
                         }
                      } catch (IOException io) {
                         io.printStackTrace();
                      }
         }
         return null;
     }

    /**
     * do not show SIM Activity Dialog for auto sanity.
     * 1.FeatureOption.MTK_AUTOSANITY is true
     * 2.FeatureOption.MTK_BUILD_TYPE is ENG
     * @return true disable SIM Dialog
     */
    public static boolean shouldDisableForAutoSanity() {
        boolean autoSanity = SystemProperties.get("ro.mtk.autosanity").equals("1");
        String buildType = SystemProperties.get("ro.build.type", "");
        Log.d(TAG, "autoSanity: " + autoSanity + " buildType: " + buildType);
        if (autoSanity && (!TextUtils.isEmpty(buildType)) && buildType.endsWith("eng")) {
            Log.d(TAG, "ShouldDisableForAutoSanity()...");
            return true;
        }
        return false;
    }

    /// M: Check if is GMS build
    public static boolean isGmsBuild(Context context) {
        boolean isGms = false;
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(
                    "com.google.android.gms", 0);
            if (info != null && (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                isGms = true;
            } else {
                isGms = false;
            }
        } catch (NameNotFoundException e) {
        }
        Log.d(TAG, "Is GMS Build? " + isGms);
        return isGms;
    }
}
