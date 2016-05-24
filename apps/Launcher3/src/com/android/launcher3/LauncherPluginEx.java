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

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

//import com.mediatek.launcher3.ext.LauncherExtPlugin;
import com.mediatek.launcher3.ext.LauncherLog;

import java.util.ArrayList;

public class LauncherPluginEx {
    static final String TAG = "LauncherPluginEx";

    /// M: [OP09]request to hide application. @{
    public static final int REQUEST_HIDE_APPS = 12;
    private static final String HIDE_PACKAGE_NAME = "com.android.launcher3";
    private static final String HIDE_ACTIVITY_NAME = "com.android.launcher3.HideAppsActivity";

    /// M: whether the apps customize pane is in edit mode, add for OP09.
    public static boolean sIsInEditMode = false;  //OP09 private
    //M:[OP09] }@

    //M:[OP09] Market icon start @{
    private Intent mAppMarketIntent = null;
    private static final boolean DISABLE_MARKET_BUTTON = true;
    //M:[OP09] end }@

    private Launcher mLauncher;

    public LauncherPluginEx(Launcher launcher) {
        mLauncher = launcher;
    }

    //M:[OP09]Add for edit/hide apps. Start@{
    /**
     * M: Make the apps customize pane enter edit mode, user can rearrange the
     * application icons in this mode, add for OP09 start.
     */
    private void enterEditMode() {
        if (LauncherLog.DEBUG_EDIT) {
            LauncherLog.d(TAG, "enterEditMode: mIsInEditMode = " + sIsInEditMode);
        }

        sIsInEditMode = true;
        //TODO
        //final View marketButton = findViewById(R.id.market_button);
        //marketButton.setVisibility(View.GONE);

        //op09 mLauncher.mAppsCustomizeTabHost.enterEditMode();
    }

    /**
     * M: Make the apps customize pane exit edit mode.
     */
    public void exitEditMode() {
        if (LauncherLog.DEBUG_EDIT) {
            LauncherLog.d(TAG, "exitEditMode: mIsInEditMode = " + sIsInEditMode);
        }

        sIsInEditMode = false;
        updateAppMarketIcon();
        //op09 mLauncher.mAppsCustomizeTabHost.exitEditMode();
    }

    public void setHiddenAndEditButton(ViewGroup overviewPanel, View[] overviewPanelButtons) {
        LauncherExtPlugin.getInstance().getWorkspaceExt(mLauncher.getApplicationContext())
                .customizeOverviewPanel(overviewPanel, overviewPanelButtons);
        final View editAppsButton = overviewPanelButtons[3];
        final View hideAppsButton = overviewPanelButtons[4];

        editAppsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                LauncherLog.d(TAG, "onClick:  v = " + v);
                /// OP09 edit mode, to-do.
                //enterEditMode();
                //op09 mLauncher.showAllApps(false,
                //     AppsCustomizePagedView.ContentType.Applications, true);
                //mLauncher.showAppsView(true /* animated */, false /* resetListToTop */,
                //   true /* updatePredictedApps */, false /* focusSearchBar */);
            }
        });
        editAppsButton.setOnTouchListener(mLauncher.getHapticFeedbackTouchListener());

        hideAppsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                LauncherLog.d(TAG, "onClick:  arg0 = " + arg0);
                startHideAppsActivity();
            }
        });
        hideAppsButton.setOnTouchListener(mLauncher.getHapticFeedbackTouchListener());
    }

    /**
     * M: Start hideAppsActivity.
     */
    public void startHideAppsActivity() { //op09 private
        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setClassName(HIDE_PACKAGE_NAME, HIDE_ACTIVITY_NAME);
        startActivityForResultSafely(intent, REQUEST_HIDE_APPS);
    }

    /**
     * M: Enable pending apps queue to block all package add/change/removed
     * events to protect the right order in apps customize paged view, this
     * would be called when entering edit mode.
     */
    public static void enablePendingAppsQueue() {
        Launcher.sUsePendingAppsQueue = true;
    }

    /**
     * M: Disable pending queue and flush pending queue to handle all pending
     * package add/change/removed events.
     *
     * @param appsCustomizePagedView
     */
    /*op09 public static void disableAndFlushPendingAppsQueue(
            AppsCustomizePagedView appsCustomizePagedView) {
        Launcher.sUsePendingAppsQueue = false;
        flushPendingAppsQueue(appsCustomizePagedView);
    }*/

  /**
     * M: Flush pending queue and handle all pending package add/change/removed
     * events.
     *
     * @param appsCustomizePagedView
     */
    /*op09private static void flushPendingAppsQueue(AppsCustomizePagedView appsCustomizePagedView) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "flushPendingAppsQueue: numbers = "
                + Launcher.sPendingChangedApps.size());
        }
        Iterator<Launcher.PendingChangedApplications> iter =
            Launcher.sPendingChangedApps.iterator();
        // TODO: maybe we can optimize this to avoid some applications
        // installed/uninstall/changed many times during user in edit mode.
        final boolean listEmpty = Launcher.sPendingChangedApps.isEmpty();
        while (iter.hasNext()) {
            processPendingChangedApplications(appsCustomizePagedView, iter.next());
            iter.remove();
        }

        if (!listEmpty) {
            appsCustomizePagedView.mViewPluginEx.processPendingPost();
            appsCustomizePagedView.onPackagesUpdated(
                LauncherModel.getSortedWidgetsAndShortcuts(
                       LauncherAppState.getInstance().getContext()));
        }
    }*/

   /**
     * M: Process pending changes application list, these apps are changed
     * during editing all apps list.
     */
    /* op09 private static void processPendingChangedApplications(
            AppsCustomizePagedView appsCustomizePagedView,
            Launcher.PendingChangedApplications pendingApps) {
        if (LauncherLog.DEBUG_EDIT) {
            LauncherLog.d(TAG, "processPendingChangedApplications: type = " + pendingApps.mType
                    + ",apps = " + pendingApps.mAppInfos);
        }

        switch (pendingApps.mType) {
            case Launcher.PendingChangedApplications.TYPE_ADDED:
                appsCustomizePagedView.mViewPluginEx.processPendingAddApps(pendingApps.mAppInfos);
                break;
            case Launcher.PendingChangedApplications.TYPE_UPDATED:
                appsCustomizePagedView.mViewPluginEx.processPendingUpdateApps(
                    pendingApps.mAppInfos);
                break;
            case Launcher.PendingChangedApplications.TYPE_REMOVED:
                appsCustomizePagedView.mViewPluginEx.processPendingRemoveApps(
                    pendingApps.mRemovedPackages);
                break;
            default:
                break;
        }
    }*/

     //TODO: updateAppMarket Icon
     /** M:Sets the app market icon.
      */
    public void updateAppMarketIcon() {
        if (!DISABLE_MARKET_BUTTON) {
            //final View marketButton = findViewById(R.id.market_button);
            Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MARKET);
            // Find the app market activity by resolving an intent.
            // (If multiple app markets are installed, it will return the ResolverActivity.)
            ComponentName activityName = intent.resolveActivity(mLauncher.getPackageManager());
            if (activityName != null) {
                int coi = mLauncher.getCurrentOrientationIndexForGlobalIcons();
                mAppMarketIntent = intent;
                /*sAppMarketIcon[coi] = updateTextButtonWithIconFromExternalActivity(
                        R.id.market_button, activityName, R.drawable.ic_launcher_market_holo,
                        TOOLBAR_ICON_METADATA_NAME);*/
                //marketButton.setVisibility(View.VISIBLE);
            } else {
                // We should hide and disable the view so that we don't try and
                //restore the visibility  of it when we swap between
                //drag & normal states from IconDropTarget subclasses.
                //marketButton.setVisibility(View.GONE);
                //marketButton.setEnabled(false);
            }
        }
    }

    private void updateAppMarketIcon(Drawable.ConstantState d) {
        if (!DISABLE_MARKET_BUTTON) {
            // Ensure that the new drawable we are creating has the approprate toolbar icon bounds
            Resources r = mLauncher.getResources();
            Drawable marketIconDrawable = d.newDrawable(r);
            int w = r.getDimensionPixelSize(R.dimen.toolbar_external_icon_width);
            int h = r.getDimensionPixelSize(R.dimen.toolbar_external_icon_height);
            marketIconDrawable.setBounds(0, 0, w, h);

            //updateTextButtonWithDrawable(R.id.market_button, marketIconDrawable);
        }
    }

    /**
     * M: Start Activity For Result Safely.
     */
    private void startActivityForResultSafely(Intent intent, int requestCode) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "startActivityForResultSafely: intent = " + intent
                    + ", requestCode = " + requestCode);
        }

        try {
            mLauncher.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mLauncher, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(mLauncher, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Launcher does not have the permission to launch " + intent +
                    ". Make sure to create a MAIN intent-filter for the corresponding activity " +
                    "or use the exported attribute for this activity.", e);
        }
    }

    //override
    public void bindAllItems(ArrayList<AppInfo> allApps, ArrayList<AppInfo> apps,
            ArrayList<FolderInfo> folders) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "bindAllItems: "
                    + "\n allApps = " + allApps
                    + "\n apps = " + apps
                    + "\n folders = " + folders);
        }

        /*if (!LauncherAppState.isDisableAllApps()) {
             op09 if (mLauncher.getAppsView() != null) {
                mLauncher.getAppsView().setItems(allApps, apps, folders);
                mLauncher.getAppsView().onPackagesUpdated(
                        LauncherModel.getSortedWidgetsAndShortcuts(mLauncher));
                mLauncher.mBindingAppsFinished = true;
                if (LauncherLog.DEBUG) {
                    LauncherLog.d(TAG, "bindAllApplications: finished!");
                }
            }
        }*/

        /** M: If unread information load completed, we need to update information in app list.@{**/
        if (mLauncher.mUnreadLoadCompleted) {
            //unread AppsCustomizePagedView.updateUnreadNumInAppInfo(allApps);
        }
        /**@}**/
    }



}
