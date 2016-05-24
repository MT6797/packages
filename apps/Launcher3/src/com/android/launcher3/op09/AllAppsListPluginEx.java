/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.launcher3;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Xml;

import java.util.ArrayList;
import java.io.IOException;

import com.android.internal.util.XmlUtils;
import com.mediatek.launcher3.ext.LauncherLog;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AllAppsListPluginEx {
    private static final String TAG = "AllAppsListPluginEx";

    public static final boolean DEBUG_LOADERS_REORDER = false;  //OP09 private
    /// M: add for top packages.
    private static final String TAG_TOPPACKAGES = "toppackages";
    private static final String WIFI_SETTINGPKGNAME = "com.android.settings";
    private static final String WIFI_SETTINGCLASSNAME =
        "com.android.settings.Settings$WifiSettingsActivity";
    /// M: OP01: 0P02: add for top packages.
    public static ArrayList<TopPackage> sTopPackages = null;

    public AllAppsList mAllAppsList;

    public static class TopPackage {
        public TopPackage(String pkgName, String clsName, int index) {
            packageName = pkgName;
            className = clsName;
            order = index;
        }

        public String packageName;
        public String className;
        public int order;
    }
    /// M.

    public AllAppsListPluginEx(AllAppsList allAppsList) {
        mAllAppsList = allAppsList;

    }

    /**
     * M: OP01, 0P02,  Load the default set of default top packages from an xml file.
     *
     * @param context
     * @return true if load successful.
     */
    static boolean loadTopPackage(final Context context) {
        boolean bRet = false;
        if (sTopPackages != null) {
            return bRet;
        }

        sTopPackages = new ArrayList<TopPackage>();

        try {
            XmlResourceParser parser = context.getResources().getXml(R.xml.default_toppackage);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            XmlUtils.beginDocument(parser, TAG_TOPPACKAGES);

            final int depth = parser.getDepth();

            int type = -1;
            while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth)
                    && type != XmlPullParser.END_DOCUMENT) {

                if (type != XmlPullParser.START_TAG) {
                    continue;
                }

                TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TopPackage);

                sTopPackages.add(new TopPackage(a.getString(R.styleable.TopPackage_topPackageName),
                        a.getString(R.styleable.TopPackage_topClassName), a.getInt(
                                R.styleable.TopPackage_topOrder, 0)));

                LauncherLog.d(TAG, "loadTopPackage: packageName = "
                        + a.getString(R.styleable.TopPackage_topPackageName)
                        + ", className = "
                        + a.getString(R.styleable.TopPackage_topClassName));

                a.recycle();
            }
        } catch (XmlPullParserException e) {
            LauncherLog.w(TAG, "Got XmlPullParserException while parsing toppackage.", e);
        } catch (IOException e) {
            LauncherLog.w(TAG, "Got IOException while parsing toppackage.", e);
        }

        return bRet;
    }

    /**
     * M: Get the index for the given appInfo in the top packages.
     *
     * @param appInfo
     * @return the index of the given appInfo.
     */
    static int getTopPackageIndex(final AppInfo appInfo) {
        int retIndex = -1;
        if (sTopPackages == null || sTopPackages.isEmpty() || appInfo == null) {
            return retIndex;
        }

        for (TopPackage tp : sTopPackages) {
            if (appInfo.componentName.getPackageName().equals(tp.packageName)
                    && appInfo.componentName.getClassName().equals(tp.className)) {
                retIndex = tp.order;
                break;
            }
        }

        return retIndex;
    }
    /*
    * M: 0P02, OP09, ensure the items from top_package.xml is in order,
    * for some special case of top_package.xml will make the arraylist out of bound.
    */

   static void ensureTopPackageOrdered() {
       ArrayList<TopPackage> tpOrderList =
               new ArrayList<TopPackage>(AllAppsList.DEFAULT_APPLICATIONS_NUMBER);
       boolean bFirst = true;
       for (TopPackage tp : sTopPackages) {
           if (bFirst) {
               tpOrderList.add(tp);
               bFirst = false;
           } else {
               for (int i = tpOrderList.size() - 1; i >= 0; i--) {
                   TopPackage tpItor = tpOrderList.get(i);
                   if (0 == i) {
                       if (tp.order < tpOrderList.get(0).order) {
                           tpOrderList.add(0, tp);
                       } else {
                           tpOrderList.add(1, tp);
                       }
                       break;
                   }

                   if ((tp.order < tpOrderList.get(i).order)
                       && (tp.order >= tpOrderList.get(i - 1).order)) {
                       tpOrderList.add(i, tp);
                       break;
                   } else if (tp.order > tpOrderList.get(i).order) {
                       tpOrderList.add(i + 1, tp);
                       break;
                   }
               }
           }
       }

       if (sTopPackages.size() == tpOrderList.size()) {
           sTopPackages = (ArrayList<TopPackage>) tpOrderList.clone();
           tpOrderList = null;
           LauncherLog.d(TAG, "ensureTopPackageOrdered done");
       } else {
           LauncherLog.d(TAG, "some mistake may occur when ensureTopPackageOrdered");
       }
   }

    /**
     * M:  OP02,OP09,  Reorder all apps index according to TopPackages.
     */
    void reorderApplist() {
        final long sortTime = DEBUG_LOADERS_REORDER ? SystemClock.uptimeMillis() : 0;

        if (sTopPackages == null || sTopPackages.isEmpty()) {
            return;
        }
        ensureTopPackageOrdered();

        final ArrayList<AppInfo> dataReorder = new ArrayList<AppInfo>(
                AllAppsList.DEFAULT_APPLICATIONS_NUMBER);

        for (TopPackage tp : sTopPackages) {
            int loop = 0;
            for (AppInfo ai : mAllAppsList.added) {
                if (DEBUG_LOADERS_REORDER) {
                    LauncherLog.d(TAG, "reorderApplist: remove loop = " + loop);
                }

                if (ai.componentName.getPackageName().equals(tp.packageName)
                        && ai.componentName.getClassName().equals(tp.className)) {
                    if (DEBUG_LOADERS_REORDER) {
                        LauncherLog.d(TAG, "reorderApplist: remove packageName = "
                                + ai.componentName.getPackageName());
                    }
                    mAllAppsList.data.remove(ai);
                    dataReorder.add(ai);
                    dumpData();
                    break;
                }
                loop++;
            }
        }

        for (TopPackage tp : sTopPackages) {
            int loop = 0;
            int newIndex = 0;
            for (AppInfo ai : dataReorder) {
                if (DEBUG_LOADERS_REORDER) {
                    LauncherLog.d(TAG, "reorderApplist: added loop = " + loop + ", packageName = "
                            + ai.componentName.getPackageName());
                }

                if (ai.componentName.getPackageName().equals(tp.packageName)
                        && ai.componentName.getClassName().equals(tp.className)) {
                    newIndex = Math.min(Math.max(tp.order, 0), mAllAppsList.added.size());
                    if (DEBUG_LOADERS_REORDER) {
                        LauncherLog.d(TAG, "reorderApplist: added newIndex = " + newIndex);
                    }
                    /// M: make sure the array list not out of bound
                    if (newIndex < mAllAppsList.data.size()) {
                        mAllAppsList.data.add(newIndex, ai);
                    } else {
                        mAllAppsList.data.add(ai);
                    }
                    dumpData();
                    break;
                }
                loop++;
            }
        }

        if (mAllAppsList.added.size() == mAllAppsList.data.size()) {
            mAllAppsList.added = (ArrayList<AppInfo>) mAllAppsList.data.clone();
            LauncherLog.d(TAG, "reorderApplist added.size() == data.size()");
        }

        if (DEBUG_LOADERS_REORDER) {
            LauncherLog.d(TAG, "sort and reorder took "
                + (SystemClock.uptimeMillis() - sortTime) + "ms");
        }
    }

    /**
     * Dump application informations in data.
     */
    void dumpData() {
        int loop2 = 0;
        for (AppInfo ai : mAllAppsList.data) {
            if (AllAppsListPluginEx.DEBUG_LOADERS_REORDER) {
                LauncherLog.d(TAG, "reorderApplist data loop2 = " + loop2);
                LauncherLog.d(TAG, "reorderApplist data packageName = "
                        + ai.componentName.getPackageName());
            }
            loop2++;
        }
    }

}
