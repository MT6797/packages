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

import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.android.launcher3.compat.LauncherActivityInfoCompat;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.LauncherModel.Callbacks;

import com.mediatek.launcher3.ext.AllApps;
//import com.mediatek.launcher3.ext.LauncherExtPlugin;
import com.mediatek.launcher3.ext.LauncherLog;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class LauncherModelPluginEx {
    static final String TAG = "LauncherModelPluginEx";

    static final boolean DEBUG_LOADERS = true;  //false

    private Context mContext;
    private LauncherModel mLauncherModel;

    public static int sMaxAppsPageIndex = 0;
    private int mCurrentPosInMaxPage = 0;
    private static final HashMap<Long, FolderInfo> sAllAppFolders = new HashMap<Long, FolderInfo>();
    private static final ArrayList<ItemInfo> sAllItems = new ArrayList<ItemInfo>();
    private static final ArrayList<AppInfo> sAllApps = new ArrayList<AppInfo>();
    private static final ArrayList<FolderInfo> sAllFolders = new ArrayList<FolderInfo>();

    /// M: sBgAddAppItems record apps added to database for that when add item to DB not finish
    /// but need to bind items.
    static final ArrayList<AppInfo> sBgAddAppItems = new ArrayList<AppInfo>();

    /// M: sBgAddAppItems record apps added to database for that when delete item in DB not finish
    /// but need to bind items.
    static final ArrayList<AppInfo> sBgDelAppItems = new ArrayList<AppInfo>();


    public LauncherModelPluginEx(Context context, LauncherModel launcherModel) {
        mContext = context;
        mLauncherModel = launcherModel;
    }

    //M:[OP09] start @{
    /**
     * M: Add an item to the database. Sets the screen, cellX and cellY fields of
     * the item. Also assigns an ID to the item, add for OP09 start.
     */
    static void addAllAppsItemToDatabase(Context context, final ItemInfo item,
            final int screen, final int cellX, final int cellY, final boolean notify) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "addAllAppsItemToDatabase item = " + item + ", screen = " + screen
                    + ", cellX " + cellX + ", cellY = " + cellY + ", notify = " + notify);
        }

        item.cellX = cellX;
        item.cellY = cellY;
        item.screenId = screen;
        boolean visible = true;
        if (item instanceof AppInfo) {
             //visible = ((AppInfo) item).isVisible;
        }

        final ContentValues values = new ContentValues();
        final ContentResolver cr = context.getContentResolver();
        item.onAddToDatabase(context, values);

        item.id = LauncherExtPlugin.getInstance().getLoadDataExt(context)
            .generateNewIdForAllAppsList();
        values.put(AllApps._ID, item.id);
        values.put(AllApps.CONTAINER, item.container);
        values.put(AllApps.SCREEN, screen);
        values.put(AllApps.CELLX, cellX);
        values.put(AllApps.CELLY, cellY);
        values.put(AllApps.SPANX, item.spanX);
        values.put(AllApps.SPANY, item.spanX);
        values.put(AllApps.VISIBLE_FLAG, visible);
        long serialNumber = UserManagerCompat.getInstance(context)
                                 .getSerialNumberForUser(item.user);
        values.put(AllApps.PROFILE_ID, serialNumber);

        Runnable r = new Runnable() {
            public void run() {
                cr.insert(notify ? AllApps.CONTENT_URI
                        : AllApps.CONTENT_URI_NO_NOTIFICATION, values);
            }
        };
        LauncherModel.runOnWorkerThread(r);
    }

   static void addFolderItemToDatabase(Context context, final FolderInfo item, final int screen,
           final int cellX, final int cellY, final boolean notify) {
       if (LauncherLog.DEBUG) {
           LauncherLog.d(TAG, "addFolderItemToDatabase <AllApps> item = " + item + ", screen = "
                   + screen + ", cellX " + cellX + ", cellY = " + cellY + ", notify = " + notify);
       }

       item.cellX = cellX;
       item.cellY = cellY;
       item.screenId = screen;

       final ContentValues values = new ContentValues();
       final ContentResolver cr = context.getContentResolver();
       item.onAddToDatabase(context, values);

       item.id = LauncherExtPlugin.getInstance().getLoadDataExt(context)
           .generateNewIdForAllAppsList();
       values.put(AllApps._ID, item.id);
       values.put(AllApps.SCREEN, screen);
       values.put(AllApps.CELLX, cellX);
       values.put(AllApps.CELLY, cellY);
       long serialNumber = UserManagerCompat.getInstance(context)
                                .getSerialNumberForUser(item.user);
       values.put(AllApps.PROFILE_ID, serialNumber);

       Runnable r = new Runnable() {
           public void run() {
               LauncherLog.d(TAG, "addFolderItemToDatabase values = " + values);
               cr.insert(notify ? AllApps.CONTENT_URI
                       : AllApps.CONTENT_URI_NO_NOTIFICATION, values);
           }
       };
       LauncherModel.runOnWorkerThread(r);
   }


    /**
     * M: Add an item to the database in a specified container. Sets the container, screen, cellX
     * and cellY fields of the item. Also assigns an ID to the item.
     */
    static void addFolderItemToDatabase(Context context, final FolderInfo item,
            final long container, final long screenId, final int cellX, final int cellY,
            final boolean notify) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "addFolderItemToDatabase <Favorites> item = " + item
                    + ", container = " + container + ", screenId = " + screenId
                    + ", cellX " + cellX + ", cellY = " + cellY + ", notify = " + notify);
        }

        // Add folder
        LauncherModel.addItemToDatabase(context, item, container, screenId, cellX, cellY);

        // Add folder contents
        final ArrayList<ShortcutInfo> contents = item.contents;
        for (ShortcutInfo info : contents) {
            LauncherModel.addItemToDatabase(context, info, item.id, info.screenId,
                info.cellX, info.cellY);
        }
    }

   /**
     * Update an item to the database in a specified container.
     */
    static void updateAllAppsItemInDatabase(Context context, final ItemInfo item) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "updateItemInDatabase: item = " + item);
        }

        final ContentValues values = new ContentValues();
        item.onAddToDatabase(context, values);
        updateAllAppsItemInDatabaseHelper(context, values, item, "updateItemInDatabase");
    }

    /**
     * M: Update an item with values to the database, Also assigns an ID to the item.
     * @param context
     * @param values
     * @param item
     * @param callingFunction
     */
    static void updateAllAppsItemInDatabaseHelper(Context context, final ContentValues values,
            final ItemInfo item, final String callingFunction) {
        final long itemId = item.id;
        final Uri uri = AllApps.getContentUri(itemId, false);
        final ContentResolver cr = context.getContentResolver();
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "updateAllAppsItemInDatabaseHelper: values = " + values
                    + ", item = " + item + ",itemId = " + itemId + ", uri = " + uri.toString());
        }

        final Runnable r = new Runnable() {
            public void run() {
                if (LauncherLog.DEBUG) {
                    LauncherLog.d(TAG, "updateAllAppsItemInDatabaseHelper in run: values = "
                            + values + ", item = " + item
                            + ", uri = " + uri.toString());
                }
                cr.update(uri, values, null, null);
            }
        };
        LauncherModel.runOnWorkerThread(r);
    }

    static void updateAllAppsItemsInDatabaseHelper(Context context,
            final ArrayList<ContentValues> valuesList,
            final ArrayList<ItemInfo> items,
            final String callingFunction) {
        final ContentResolver cr = context.getContentResolver();

        final Runnable r = new Runnable() {
            public void run() {
                ArrayList<ContentProviderOperation> ops =
                        new ArrayList<ContentProviderOperation>();
                int count = items.size();
                for (int i = 0; i < count; i++) {
                    ItemInfo item = items.get(i);
                    final long itemId = item.id;
                    final Uri uri = AllApps.getContentUri(itemId, false);
                    ContentValues values = valuesList.get(i);

                    ops.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());
                }
                try {
                    cr.applyBatch(LauncherProvider.AUTHORITY, ops);
                } catch (OperationApplicationException e) {
                    LauncherLog.d(TAG, "updateAllAppsItemsInDatabaseHelper Exception", e);
                } catch (RemoteException e) {
                    LauncherLog.d(TAG, "updateAllAppsItemsInDatabaseHelper Exception", e);
                }
            }
        };
        LauncherModel.runOnWorkerThread(r);
    }

    /**
     * M: Move an item in the DB to a new <screen, cellX, cellY>.
     */
    static void moveAllAppsItemInDatabase(Context context, final ItemInfo item, final int screen,
            final int cellX, final int cellY) {
        moveAllAppsItemInDatabase(context, item, item.container, screen, cellX, cellY);
    }

    /**
     * M: Move an item in the DB to a new <container, screen, cellX, cellY>.
     */
    static void moveAllAppsItemInDatabase(Context context, final ItemInfo item,
            final long container, final long screenId, final int cellX, final int cellY) {
        final String transaction = "DbDebug    Modify item (" + item.title + ") in db, id: "
                + item.id + " (" + item.container + ", " + item.screenId + ", " + item.cellX + ", "
                + item.cellY + ") --> " + "(" + container + ", " + screenId + ", " + cellX + ", "
                + cellY + ")";
        Launcher.sDumpLogs.add(transaction);
        LauncherLog.d(TAG, transaction);

        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "moveAllAppsItemInDatabase: item = " + item
                    + ", container = " + container + ", screenId = " + screenId
                    + ", cellX = " + cellX + ", cellY = " + cellY + ", context = " + context);
        }

        item.container = container;
        item.screenId = screenId;
        item.cellX = cellX;
        item.cellY = cellY;

        final ContentValues values = new ContentValues();
        values.put(AllApps.CONTAINER, item.container);
        values.put(AllApps.SCREEN, item.screenId);
        values.put(AllApps.CELLX, item.cellX);
        values.put(AllApps.CELLY, item.cellY);
        values.put(AllApps.SPANX, item.spanX);
        values.put(AllApps.SPANY, item.spanY);
        if (item instanceof AppInfo) {
            //values.put(AllApps.VISIBLE_FLAG, ((AppInfo) item).isVisible);
        }

        updateAllAppsItemInDatabaseHelper(context, values, item, "moveAllAppsItemInDatabase");
    }

    /**
     * Move items in the DB to a new <container, screen, cellX, cellY>. We assume that the
     * cellX, cellY have already been updated on the ItemInfos.
     */
    static void moveAllAppsItemsInDatabase(Context context, final ArrayList<ItemInfo> items,
            final long container, final int screen) {
        ArrayList<ContentValues> contentValues = new ArrayList<ContentValues>();
        int count = items.size();

        for (int i = 0; i < count; i++) {
            ItemInfo item = items.get(i);
            item.container = container;
            item.screenId = screen;

            final ContentValues values = new ContentValues();
            values.put(AllApps.CONTAINER, item.container);
            values.put(AllApps.SCREEN, item.screenId);
            values.put(AllApps.CELLX, item.cellX);
            values.put(AllApps.CELLY, item.cellY);
            values.put(AllApps.SPANX, item.spanX);
            values.put(AllApps.SPANY, item.spanY);
            if (item instanceof AppInfo) {
                //values.put(AllApps.VISIBLE_FLAG, ((AppInfo) item).isVisible);
            }

            contentValues.add(values);
        }
        updateAllAppsItemsInDatabaseHelper(context, contentValues, items,
                "moveAllAppsItemsInDatabase");
    }

   /**
     * M:Removes the specified item from the database.
     *
     * @param context
     * @param item
     */
    static void deleteAllAppsItemFromDatabase(Context context, final ItemInfo item) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "deleteAllAppsItemFromDatabase: item = " + item);
        }

        final ContentResolver cr = context.getContentResolver();
        final Uri uriToDelete = AllApps.getContentUri(item.id, false);
        Runnable r = new Runnable() {
            public void run() {
                cr.delete(uriToDelete, null, null);
                if (LauncherLog.DEBUG) {
                    LauncherLog.d(TAG, "deleteAllAppsItemFromDatabase remove id : " + item.id);
                }
            }
        };
        LauncherModel.runOnWorkerThread(r);
    }

    static void deleteFolderItemFromDatabase(Context context, final FolderInfo item) {
        if (LauncherLog.DEBUG) {
          LauncherLog.d(TAG, "deleteFolderItemFromDatabase: item = " + item);
        }

        final ContentResolver cr = context.getContentResolver();
        final Uri uriToDelete = AllApps.getContentUri(item.id, false);
        Runnable r = new Runnable() {
          public void run() {
              cr.delete(uriToDelete, null, null);
              if (LauncherLog.DEBUG) {
                  LauncherLog.d(TAG, "deleteFolderItemFromDatabase remove id : " + item.id);
              }
          }
        };
        LauncherModel.runOnWorkerThread(r);
    }

   /**
     * M: Get application info, add for OP09.
     *
     * @param manager the package manager
     * @param intent the intent for app
     * @param context context enviroment
     * @param c the database cursor
     * @param titleIndex the database column index
     * @return the app info by packagemanager
     */
    public AppInfo getApplicationInfo(PackageManager manager, Intent intent, Context context,
            Cursor c, int titleIndex) {
        final AppInfo info = new AppInfo();
        //public AppInfo(Context context, LauncherActivityInfoCompat info, UserHandleCompat user,
        //        IconCache iconCache, HashMap<Object, CharSequence> labelCache)

        ComponentName componentName = intent.getComponent();
        if (componentName == null) {
            return null;
        }

        try {
            final String packageName = componentName.getPackageName();
            //List<LauncherActivityInfoCompat> app = mLauncherApps.
            //                  getActivityList(packageName, user);
            PackageInfo pi = manager.getPackageInfo(packageName, 0);
            if (!pi.applicationInfo.enabled) {
                // If we return null here, the corresponding item will be removed from the launcher
                // db and will not appear in the workspace.
                return null;
            }

            final int appFlags = manager.getApplicationInfo(packageName, 0).flags;
            if ((appFlags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                info.flags |= AppInfo.DOWNLOADED_FLAG;

                if ((appFlags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    info.flags |= AppInfo.UPDATED_SYSTEM_APP_FLAG;
                }
            }
            info.firstInstallTime = manager.getPackageInfo(packageName, 0).firstInstallTime;
        } catch (NameNotFoundException e) {
            Log.d(TAG, "getPackInfo failed for componentName " + componentName);
            return null;
        }

        // from the db
        if (info.title == null) {
            if (c != null) {
                info.title =  c.getString(titleIndex);
            }
        }
        // fall back to the class name of the activity
        if (info.title == null) {
            info.title = componentName.getClassName();
        }
        info.componentName = componentName;
        info.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
        return info;
    }


    /**
    * M: Comparator for application item, use their screen and pos to compare.
    * @param info
    */
    public static class AppListPositionComparator implements Comparator<ItemInfo> {
        /**
        * M: Comparator for application item, use their screen and pos to compare.
        * @param a the first app info
        * @param b the second app info
        * @return the bigger result or not
        */
        public final int compare(ItemInfo a, ItemInfo b) {
            if (a.screenId < b.screenId) {
                return -1;
            } else if (a.screenId > b.screenId) {
                return 1;
            } else {
                if (a.mPos < b.mPos) {
                    return -1;
                } else if (a.mPos > b.mPos) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
    }

    /// M: [OP09]End. }@



    ///below part from LoadTask.
    /**
     * M: Load and bind all apps list, add for OP09.
     */
    public void loadAndBindAllAppsExt() {  //op09 private
        final long t = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;
        if (DEBUG_LOADERS) {
            LauncherLog.d(TAG, "loadAndBindAllAppsExt start: " + t);
        }

        // Don't use these two variables in any of the callback runnables.
        // Otherwise we hold a reference to them.
        final Callbacks oldCallbacks = mLauncherModel.mCallbacks.get();
        if (oldCallbacks == null) {
            // This launcher has exited and nobody bothered to tell us. Just
            // bail.
            Log.w(TAG, "LoaderTask running with no launcher (loadAndBindAllAppsList)");
            return;
        }

        mLauncherModel.mBgAllAppsList.clear();
        sAllAppFolders.clear();
        sAllItems.clear();
        sAllApps.clear();
        sAllFolders.clear();

        final List<UserHandleCompat> profiles = mLauncherModel.mUserManager.getUserProfiles();

        ItemInfo item = null;
        for (UserHandleCompat user : profiles) {
            final ArrayList<ItemInfo> allItems = new ArrayList<ItemInfo>();
            final ArrayList<AppInfo> allApps = new ArrayList<AppInfo>();
            final ArrayList<FolderInfo> allFolders = new ArrayList<FolderInfo>();

            final long loadTime = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;
            loadAllAppsExt(user, allItems, allApps, allFolders);
            if (DEBUG_LOADERS) {
                Log.d(TAG, "load took " + (SystemClock.uptimeMillis() - loadTime) + "ms");
            }

            sAllItems.addAll(allItems);
            sAllApps.addAll(allApps);
            sAllFolders.addAll(allFolders);

            final int itemSize = allItems.size();
            final int appSize = allApps.size();
            final int foldersSize = allFolders.size();
            LauncherLog.i(TAG, "loadAndBindAllAppsExt"
                    + ", allItems=" + itemSize
                    + ", allApps=" + appSize
                    + ", allFolders=" + foldersSize);
            for (int i = 0; i < itemSize; i++) {
                item = allItems.get(i);
                if (item instanceof AppInfo) {
                    mLauncherModel.mBgAllAppsList.add((AppInfo) item);
                }
            }

            ///M: 0P01, OP02,
            mLauncherModel.mBgAllAppsList.mPluginEx.reorderApplist();
            ///M.

            final Callbacks callbacks = mLauncherModel.mLoaderTask.tryGetCallbacks(oldCallbacks);
            final ArrayList<AppInfo> added = mLauncherModel.mBgAllAppsList.added;
            mLauncherModel.mBgAllAppsList.added = new ArrayList<AppInfo>();

            mLauncherModel.mHandler.post(new Runnable() {
                public void run() {
                    final long t = SystemClock.uptimeMillis();
                    if (callbacks != null) {
                        callbacks.bindAllItems(added, allApps, allFolders);
                        if (DEBUG_LOADERS) {
                            LauncherLog.d(TAG, "bound " + added.size() + " apps in "
                                    + (SystemClock.uptimeMillis() - t) + "ms");
                        }
                    } else {
                        LauncherLog.i(TAG, "not binding apps: no Launcher activity");
                    }
                }
            });
        }
    }

    public void onlyBindAllAppsExt() {
        final Callbacks oldCallbacks = mLauncherModel.mCallbacks.get();
        if (oldCallbacks == null) {
            // This launcher has exited and nobody bothered to tell us. Just bail.
            Log.w(TAG, "LoaderTask running with no launcher (onlyBindAllAppsExt)");
            return;
        }

        if (DEBUG_LOADERS) {
            LauncherLog.d(TAG, "onlyBindAllAppsExt: oldCallbacks =" + oldCallbacks
                    + ", this = " + mLauncherModel);
        }

        // shallow copy
        final ArrayList<AppInfo> allApps =
            (ArrayList<AppInfo>) mLauncherModel.mBgAllAppsList.data.clone();
        final ArrayList<AppInfo> apps = (ArrayList<AppInfo>) sAllApps.clone();
        final ArrayList<FolderInfo> folders = (ArrayList<FolderInfo>) sAllFolders.clone();
        final Runnable r = new Runnable() {
            public void run() {
                final long t = SystemClock.uptimeMillis();
                final Callbacks callbacks = mLauncherModel.mLoaderTask.tryGetCallbacks(
                    oldCallbacks);
                if (callbacks != null) {
                    ///M. ALPS02132644 , modify apps to allApps.
                    callbacks.bindAllItems(allApps, allApps, folders);
                }
                if (DEBUG_LOADERS) {
                    Log.d(TAG, "bound all " + allApps.size() + " apps from cache in "
                            + (SystemClock.uptimeMillis() - t) + "ms");
                }
            }
        };
        boolean isRunningOnMainThread = !(mLauncherModel.sWorkerThread.getThreadId()
            == Process.myTid());
        if (isRunningOnMainThread) {
            r.run();
        } else {
            mLauncherModel.mHandler.post(r);
        }
    }

    /**
     * M: Only load all apps list, we need to do this by two steps, first
     * load from the default all apps list(database), then load all remains
     * by querying package manager service, add for OP09.
     * @param user
     * @param allApps
     * @param allFolders
     */
    public void loadAllAppsExt(UserHandleCompat user, final ArrayList<ItemInfo> allItems,
            final ArrayList<AppInfo> allApps, final ArrayList<FolderInfo> allFolders) {
        if (LauncherLog.DEBUG_LOADER) {
            LauncherLog.d(TAG, "loadAllAppsExt start, user = " + user.toString());
        }

        sMaxAppsPageIndex = 0;
        mCurrentPosInMaxPage = 0;

        final Context context = mContext;
        final ContentResolver contentResolver = context.getContentResolver();
        final PackageManager packageManager = context.getPackageManager();
        final boolean isSafeMode = packageManager.isSafeMode();

        // Make sure the default app list is loaded.
        final boolean loadDefault = LauncherExtPlugin.getInstance().getLoadDataExt(mContext)
                .loadDefaultAllAppsIfNecessary(LauncherProvider.getSQLiteDatabase(), mContext);
        final int screenCount = LauncherExtPlugin.getInstance().getLoadDataExt(mContext)
                .getMaxScreenIndexForAllAppsList(LauncherProvider.getSQLiteDatabase()) + 1;

        final String selection = "profileId = " + UserManagerCompat.getInstance(mContext)
                .getSerialNumberForUser(user);
        LauncherLog.d(TAG, "loadAllApps: selection =" + selection);
        final Cursor c = contentResolver
                .query(AllApps.CONTENT_URI, null, selection, null, null);

        if (LauncherLog.DEBUG_LOADER) {
            LauncherLog.d(TAG, "loadAllApps: stone, loadDefault = "
                    + loadDefault + ",screenCount = "
                    + screenCount + ", db item count = " + c.getCount() + ", isSafeMode = "
                    + isSafeMode + ", sAppsCellCountX=" + AllApps.sAppsCellCountX
                    + ", sAppsCellCountY=" + AllApps.sAppsCellCountY);
        }

        final ItemInfo occupied[][] = new ItemInfo[screenCount][AllApps.sAppsCellCountX
                * AllApps.sAppsCellCountY];
        final ArrayList<ItemPosition> invalidAppItemPositions = new ArrayList<ItemPosition>();
        final ArrayList<ItemInfo> overlapAppItems = new ArrayList<ItemInfo>();
        final HashSet<Integer> emptyCellScreens = new HashSet<Integer>();

        try {
            final int idIndex = c.getColumnIndexOrThrow(AllApps._ID);
            final int intentIndex = c.getColumnIndexOrThrow(AllApps.INTENT);
            final int titleIndex = c.getColumnIndexOrThrow(AllApps.TITLE);
            final int itemTypeIndex = c.getColumnIndexOrThrow(AllApps.ITEM_TYPE);
            final int containerIndex = c.getColumnIndexOrThrow(AllApps.CONTAINER);
            final int screenIndex = c.getColumnIndexOrThrow(AllApps.SCREEN);
            final int cellXIndex = c.getColumnIndexOrThrow(AllApps.CELLX);
            final int cellYIndex = c.getColumnIndexOrThrow(AllApps.CELLY);
            final int spanXIndex = c.getColumnIndexOrThrow(AllApps.SPANX);
            final int spanYIndex = c.getColumnIndexOrThrow(AllApps.SPANY);
            final int visibleIndex = c.getColumnIndexOrThrow(AllApps.VISIBLE_FLAG);
            final int profileIdIndex = c.getColumnIndexOrThrow(AllApps.PROFILE_ID);

            AppInfo info;
            String intentDescription;
            int container = 0;
            long id;
            Intent intent;
            int visible;
            int itemType;

            while (!mLauncherModel.mLoaderTask.mStopped && c.moveToNext()) {
                itemType = c.getInt(itemTypeIndex);
                if (AllApps.ITEM_TYPE_APPLICATION == itemType) {
                    intentDescription = c.getString(intentIndex);
                    if (TextUtils.isEmpty(intentDescription)) {
                        LauncherLog.w(TAG, "loadAllApps, intentDescription is null, continue.");
                        continue;
                    }
                    try {
                        intent = Intent.parseUri(intentDescription, 0);
                    } catch (URISyntaxException e) {
                        LauncherLog.w(TAG, "loadAllApps, parse Intent Uri error: "
                                           + intentDescription);
                        continue;
                    }

                    info = getApplicationInfo(packageManager, intent, context, c, titleIndex);
                    visible = c.getInt(visibleIndex);

                    // When the device is in safemode and not system app,if yes,
                    //don't add in applist.
                    if (info != null && (!isSafeMode || Utilities.isSystemApp(info))) {
                        id = c.getLong(idIndex);
                        info.id = id;
                        info.intent = intent;
                        container = c.getInt(containerIndex);
                        info.container = container;
                        info.screenId = c.getInt(screenIndex);
                        //info.isVisible = (visible == 1);
                        info.cellX = c.getInt(cellXIndex);
                        info.cellY = c.getInt(cellYIndex);
                        info.spanX = 1;
                        info.spanY = 1;

                        /*if (info.isVisible) {
                            info.mPos = info.cellY * AllApps.sAppsCellCountX + info.cellX;
                            if (info.screenId > sMaxAppsPageIndex) {
                                sMaxAppsPageIndex = (int) info.screenId;
                                mCurrentPosInMaxPage = info.mPos;
                            }

                            if (info.screenId ==  sMaxAppsPageIndex
                                 && info.mPos > mCurrentPosInMaxPage) {
                                mCurrentPosInMaxPage = info.mPos;
                            }
                        } else {
                            info.mPos = -(info.cellY * AllApps.sAppsCellCountX + info.cellX);
                        }*/
                        info.user = user;
                        ResolveInfo resolveInfo = packageManager.resolveActivity(intent, 0);

                        /// M: Remove the item whose resolve info is null
                        if (resolveInfo == null
                                || resolveInfo.activityInfo.packageName == null) {
                            invalidAppItemPositions.add(new ItemPosition((int) info.screenId,
                                    info.mPos));
                            id = c.getLong(idIndex);
                            contentResolver
                                    .delete(AllApps.getContentUri(id, false), null, null);
                            LauncherLog.w(TAG, "loadAllApps: Error getting application info "
                                    + id + ", removing it");
                        } else {
                            LauncherActivityInfoCompat launcherActInfo =
                                mLauncherModel.mLauncherApps.resolveActivity(intent, user);
                            mLauncherModel.mIconCache.getTitleAndIcon(
                                info, launcherActInfo, false); //op09
                            LauncherLog.i(TAG, "loadAllApps stone: add app info = " + info);

                            // Item is in AllApps
                            /*if (container < 0) {
                                if (info.isVisible) {
                                    checkAppItemPlacement(occupied, overlapAppItems, info);
                                }
                                allApps.add(info);
                            } else {
                                // Item is in a user folder, Add Appinfo to FolderInfo.
                                final FolderInfo folderInfo = mLauncherModel.findOrMakeFolder(
                                    LauncherModel.sBgFolders, container);
                                final ShortcutInfo appShortcutInfo = new ShortcutInfo(
                                        info);
                                folderInfo.add(appShortcutInfo);
                                if (LauncherLog.DEBUG_LOADER) {
                                    LauncherLog.d(TAG, "loadAllApps add: "
                                            + appShortcutInfo + " to folder: " + folderInfo);
                                }
                            }*/

                            allItems.add(info);
                            if (LauncherLog.DEBUG_LOADER) {
                                LauncherLog.d(TAG, "loadAllApps allItems.add = " + info);
                            }
                        }
                    } else {
                        // Failed to load the shortcut, probably because the activity manager
                        //couldn't resolve it (maybe the app was uninstalled), or the db row
                        //was somehow screwed up.Delete it.
                        final int pos = c.getInt(cellYIndex) * AllApps.sAppsCellCountX
                                + c.getInt(cellXIndex);
                        invalidAppItemPositions
                                .add(new ItemPosition(c.getInt(screenIndex), pos));
                        id = c.getLong(idIndex);
                        contentResolver.delete(AllApps.getContentUri(id, false), null, null);
                        LauncherLog.w(TAG, "loadAllApps: Error getting application info "
                                + id + ", removing it");
                    }
                } else if (AllApps.ITEM_TYPE_FOLDER == itemType) {
                    id = c.getLong(idIndex);
                    final FolderInfo folderInfo = mLauncherModel.findOrMakeFolder(
                        LauncherModel.sBgFolders, container);
                    folderInfo.title = c.getString(titleIndex);
                    folderInfo.id = id;
                    container = c.getInt(containerIndex);
                    folderInfo.container = container;
                    folderInfo.screenId = c.getInt(screenIndex);
                    folderInfo.cellX = c.getInt(cellXIndex);
                    folderInfo.cellY = c.getInt(cellYIndex);
                    folderInfo.spanX = 1;
                    folderInfo.spanY = 1;

                    folderInfo.mPos = folderInfo.cellY * AllApps.sAppsCellCountX
                            + folderInfo.cellX;
                    if (folderInfo.screenId > sMaxAppsPageIndex) {
                        sMaxAppsPageIndex = (int) folderInfo.screenId;
                        mCurrentPosInMaxPage = folderInfo.mPos;
                    }

                    if (folderInfo.screenId == sMaxAppsPageIndex
                            && folderInfo.mPos > mCurrentPosInMaxPage) {
                        mCurrentPosInMaxPage = folderInfo.mPos;
                    }

                    folderInfo.user = user;

                    // check & update map of what's occupied
                    checkAppItemPlacement(occupied, overlapAppItems, folderInfo);

                    allItems.add(folderInfo);
                    allFolders.add(folderInfo);
                    sAllAppFolders.put(folderInfo.id, folderInfo);
                    if (LauncherLog.DEBUG_LOADER) {
                        LauncherLog.d(TAG, "loadAllApps sAllAppFolders.put = " + folderInfo);
                    }
                }
            }
        } finally {
            c.close();
        }

        if (mLauncherModel.mLoaderTask.mStopped) {
            LauncherLog.i(TAG, "loadAllApps force stopped.");
            return;
        }

        if (!sBgAddAppItems.isEmpty()) {
            for (AppInfo info : sBgAddAppItems) {
                LauncherLog.i(TAG, "loadAllApps bg app item: " + info);
                if (allApps.indexOf(info) == -1) {
                    LauncherLog.i(TAG, "loadAllApps add bg app item to all apps: " + info);
                    allApps.add(info);
                }
            }
        }

        if (!sBgDelAppItems.isEmpty()) {
            for (AppInfo info : sBgDelAppItems) {
                LauncherLog.i(TAG, "loadAllApps delete bg app item to all apps: " + info);
                allApps.remove(info);
            }
        }

        // The following logic is a recovery mechanism for invalid app,
        // empty cell and overlapped apps. The new check/recovery mechanism
        // may cost about 40ms more when the app list size is 68.
        if (LauncherLog.DEBUG_LOADER) {
            LauncherLog.i(TAG, "loadAllAppsExt"
                    + ", allItems=" + allItems.size()
                    + ", allApps=" + allApps.size()
                    + ", allFolders=" + allFolders.size()
                    + ", overlapAppItems=" + overlapAppItems.size());

            dumpItemInfoList(allItems, "Before sort allItems");
            dumpItemInfoList(allApps, "Before sort allApps");
            dumpItemInfoList(allFolders, "Before sort allFolders");
            dumpItemInfoList(overlapAppItems, "Before sort overlapAppItems");
        }

        // Apps + Folders
        final ArrayList<ItemInfo> allAppsAndFolders = new ArrayList<ItemInfo>(
                allApps.size() + allFolders.size());
        allAppsAndFolders.addAll(allApps);
        allAppsAndFolders.addAll(allFolders);

        // Sort all apps list to make items in order.
        Collections.sort(allAppsAndFolders, new AppListPositionComparator());
        if (LauncherLog.DEBUG_LOADER) {
            LauncherLog.d(TAG, "loadAllApps, invalidAppItemPositions = "
                    + invalidAppItemPositions);
        }
        if (!invalidAppItemPositions.isEmpty()) {
            reorderAllAppsForInvalidAppsRemoved(allAppsAndFolders, invalidAppItemPositions);
        }

        checkEmptyCells(occupied, emptyCellScreens, screenCount,
                AllApps.sAppsCellCountX * AllApps.sAppsCellCountY);
        if (LauncherLog.DEBUG_LOADER) {
            LauncherLog.d(TAG, "loadAllApps: emptyCellScreens = " + emptyCellScreens
                    + ", overlapApps = " + overlapAppItems);
        }
        if (!emptyCellScreens.isEmpty()) {
            reorderAppsForEmptyCell(allAppsAndFolders, overlapAppItems, emptyCellScreens);
        }

        // Get the max item index for each screen, the app list is in order
        // currently.
        final ArrayList<ItemPosition> maxPosInScreens = new ArrayList<ItemPosition>();
        final int allAppsAndFoldersSize = allAppsAndFolders.size();
        long curScreen = Integer.MAX_VALUE;
        for (int i = allAppsAndFoldersSize - 1; i >= 0; i--) {
            final ItemInfo item = allAppsAndFolders.get(i);
            if (LauncherLog.DEBUG_LOADER) {
                LauncherLog.d(TAG, "loadAllApps: i = " + i + ", item= " + item);
            }
            if (item.screenId < curScreen) {
                final ItemPosition itemPos = new ItemPosition((int) item.screenId, item.mPos);
                maxPosInScreens.add(itemPos);
                curScreen = item.screenId;
            }
        }
        Collections.reverse(maxPosInScreens);

        if (LauncherLog.DEBUG_LOADER) {
            LauncherLog.d(TAG, "repositionOverlapApps: maxPosInScreens = " + maxPosInScreens
                    + ", overlapApps = " + overlapAppItems);
        }
        if (!overlapAppItems.isEmpty()) {
            repositionOverlapApps(allAppsAndFolders, overlapAppItems, maxPosInScreens);
        }

        // Check PMS or not, decided by whether Launcher is first started.
        //LauncherApplication app = (LauncherApplication) context.getApplicationContext();
        //LauncherLog.d(TAG, "loadAllApps, stone, total=" + app.isTotalStart());
        //op09 if (app.isTotalStart()) {
        if (false) {
            final ItemPosition lastPos = maxPosInScreens.get(maxPosInScreens.size() - 1);
            sMaxAppsPageIndex = lastPos.mScreen;
            mCurrentPosInMaxPage = lastPos.mPos;
            if (LauncherLog.DEBUG_LOADER) {
                LauncherLog.d(TAG, "Load total " + allApps.size()
                        + " apps before check PMS, lastPos = " + lastPos);
            }

            final ArrayList<AppInfo> appsInPM = getAppsInPMButNotInDB(user, allItems);
            if (!appsInPM.isEmpty()) {
                addAppsInPMButNotInDB(appsInPM);
                allItems.addAll(appsInPM);
                allApps.addAll(appsInPM);
                if (LauncherLog.DEBUG_LOADER) {
                    dumpItemInfoList(appsInPM, "addAppsInPMButNotInDB");
                }
            }
            //app.resetTotalStartFlag();
        }

        if (LauncherLog.DEBUG_LOADER) {
            dumpItemInfoList(allItems, "LoadApps end allItems");
            dumpItemInfoList(allApps, "LoadApps end allApps");
            dumpItemInfoList(allFolders, "LoadApps end allFolders");

            dumpAllAppLayout(occupied);

            LauncherLog.d(TAG, "loadAndBindAllAppsExt end");
        }
    }

    /**
     * M: To dispose the APKs which are in db but can't query from
     * PackageManager, so after remove them, the left ones need reorder.
     */
    private void reorderAllAppsForInvalidAppsRemoved(ArrayList<ItemInfo> allApps,
            ArrayList<ItemPosition> itemsRemoved) {
        final ArrayList<ItemInfo> itemsInTheSameScreenButAfterPosition
                                        = new ArrayList<ItemInfo>();
        final ArrayList<ItemInfo> itemsInTheAfterScreen = new ArrayList<ItemInfo>();
        LauncherLog.d(TAG, "reorderAllAppsForInvalidAppsRemoved: itemsRemoved = "
                + itemsRemoved);

        for (ItemPosition removedItemPosition : itemsRemoved) {
            LauncherLog.d(TAG, "reorderAllApps: The removed items is at screen="
                    + removedItemPosition.mScreen + ", pos=" + removedItemPosition.mPos);
            itemsInTheSameScreenButAfterPosition.clear();
            itemsInTheAfterScreen.clear();
            boolean bOnlyOneItemInTheScreen = true;

            for (ItemInfo appInfo : allApps) {
                if (appInfo.screenId == removedItemPosition.mScreen
                        && appInfo.mPos > removedItemPosition.mPos) {
                    LauncherLog.d(TAG, "Add one item which are in the same screen "
                            + "with removed item and at cellX=" + appInfo.cellX + ", cellY="
                            + appInfo.cellY);
                    itemsInTheSameScreenButAfterPosition.add(appInfo);
                }

                if (bOnlyOneItemInTheScreen && appInfo.screenId
                               == removedItemPosition.mScreen) {
                    bOnlyOneItemInTheScreen = false;
                }
            }

            if (bOnlyOneItemInTheScreen) {
                for (ItemInfo appInfo : allApps) {
                    if (appInfo.screenId > removedItemPosition.mScreen) {
                        itemsInTheAfterScreen.add(appInfo);
                    }
                }
            }

            if (itemsInTheSameScreenButAfterPosition != null
                    && itemsInTheSameScreenButAfterPosition.size() > 0) {
                LauncherLog.d(TAG, "reorderAllApps: itemsInTheSameScreenAndAfterPosition is "
                    + itemsInTheSameScreenButAfterPosition.size());
                int newX = -1;
                int newY = -1;
                for (ItemInfo appInfo : itemsInTheSameScreenButAfterPosition) {
                    appInfo.mPos -= 1;

                    newX = appInfo.mPos % AllApps.sAppsCellCountX;
                    newY = appInfo.mPos / AllApps.sAppsCellCountX;
                    LauncherLog.d(TAG, "reorderAllApps: move item from (" + appInfo.cellX + ","
                            + appInfo.cellY + ") to (" + newX + "," + newY + ").");

                    moveAllAppsItemInDatabase(mContext, appInfo, (int) appInfo.screenId,
                                                    newX, newY);
                }
            } else {
                if (itemsInTheAfterScreen != null && itemsInTheAfterScreen.size() > 0) {
                    LauncherLog.d(TAG, "reorderAllApps: itemsInBiggerScreen number is "
                            + itemsInTheAfterScreen.size());
                    for (ItemInfo appInfo : itemsInTheAfterScreen) {
                        LauncherLog.d(TAG, "reorderAllApps: move item (" + appInfo.cellX + ","
                                + appInfo.cellY + "). from screen " + appInfo.screenId
                                + " to the forward one.");
                        moveAllAppsItemInDatabase(mContext, appInfo,
                                                  (int) (appInfo.screenId - 1),
                                appInfo.cellX, appInfo.cellY);
                    }
                }
            }
        }
    }

    /**
     * M: Reposition the overlap apps, find a valid position from the current
     * screen and add move the overlapped app to it.
     *
     * @param allApps
     * @param overlapApps
     * @param maxPosInScreens
    */
    public void repositionOverlapApps(ArrayList<ItemInfo> allApps,  //op09 private
            ArrayList<ItemInfo> overlapApps, ArrayList<ItemPosition> maxPosInScreens) {
        // Handle overlap apps reversely, that means handle the apps with
        // largest screen index and pos.
        Collections.sort(overlapApps, new AppListPositionComparator());
        Collections.reverse(overlapApps);

        for (ItemInfo appInfo : overlapApps) {
            final ItemPosition itemPos = findNextAvailablePostion(maxPosInScreens, appInfo);
            if (LauncherLog.DEBUG) {
                LauncherLog.d(TAG, "repositionOverlapApps: appInfo = " + appInfo
                        + ", itemPos = " + itemPos);
            }

            appInfo.screenId = itemPos.mScreen;
            appInfo.mPos = itemPos.mPos;
            appInfo.cellX = appInfo.mPos % AllApps.sAppsCellCountX;
            appInfo.cellY = appInfo.mPos / AllApps.sAppsCellCountX;

            moveAllAppsItemInDatabase(mContext, appInfo, (int) appInfo.screenId, appInfo.cellX,
                    appInfo.cellY);
        }
    }

    /**
    * M: Find the first empty position from the item screen, update the
    * maxPosInScreens if new screen is added.
    *
    * @param maxPosInScreens Max index of each screen.
    * @param item To be added item.
    * @return The position of the to be added item.
    */
    private ItemPosition findNextAvailablePostion(ArrayList<ItemPosition> maxPosInScreens,
           ItemInfo item) {
        final ItemPosition targetPos = new ItemPosition(-1, 0);
        final int onePageAppsNumber = AllApps.sAppsCellCountX * AllApps.sAppsCellCountY;
        int startScreen = (int) item.screenId;

        for (ItemPosition itemPos : maxPosInScreens) {
           if (itemPos.mScreen == startScreen) {
               if (itemPos.mPos < onePageAppsNumber - 1) {
                   targetPos.mScreen = itemPos.mScreen;
                   targetPos.mPos = itemPos.mPos + 1;
                   itemPos.mPos += 1;
                   break;
               } else {
                   startScreen++;
               }
           }
        }

        if (targetPos.mScreen == -1) {
           int maxScreenIndex = maxPosInScreens.get(maxPosInScreens.size() - 1).mScreen;
           targetPos.mScreen = maxScreenIndex + 1;
           ItemPosition newScreenMaxPos = new ItemPosition(targetPos.mScreen, 0);
           maxPosInScreens.add(newScreenMaxPos);
        }
        return targetPos;
    }

    /**
    * M: Check whether there is overlap, if overlap happens, add the
    * overlapped app to the list, it's only for visible apps.
    *
    * @param occupied
    * @param overlapApps
    * @param item
    * @return Return true when it is overlap.
    */
    private boolean checkAppItemPlacement(ItemInfo occupied[][],
           ArrayList<ItemInfo> overlapApps, ItemInfo item) {
       LauncherLog.i(TAG, "checkAppItemPlacement item.screenID = " + item.screenId
           + ", item.mPos=" + item.mPos + ", item = " + item);
       if (occupied[(int) item.screenId][item.mPos] == null) {
           occupied[(int) item.screenId][item.mPos] = item;
           return false;
       } else {
           overlapApps.add(item);
           LauncherLog.i(TAG, "checkAppItemPlacement found overlap app: screen = "
                   + item.screenId + ", pos = " + item.mPos + ",cur app = "
                   + occupied[(int) item.screenId][item.mPos] + ", overlap app = " + item);
           return true;
       }
    }

    /**
     * M: Check whether there is empty cell in the all apps list, be noticed
     * that the items in allApps should be in order.
     *
     * @param occupied
     * @param emptyCellScreens
     * @param screenCount
     * @param itemCount
     */
    private void checkEmptyCells(ItemInfo occupied[][],
            HashSet<Integer> emptyCellScreens, int screenCount, int itemCount) {
        for (int i = 0; i < screenCount; i++) {
            boolean suspectEndFound = false;
            for (int j = 0; j < itemCount; j++) {
                if (occupied[i][j] == null) {
                    if (LauncherLog.DEBUG) {
                        LauncherLog.d(TAG, "checkEmptyCells find suspect end: i = " + i
                                + ", j = " + j);
                    }
                    suspectEndFound = true;
                } else {
                    // If there is item after the suspect end, it means
                    // there is empty cell.
                    if (suspectEndFound) {
                        emptyCellScreens.add(i);
                        break;
                    }
                }
            }
        }
    }

    /**
         * M: Reorder apps in screen with empty cells, be noticed that the items in
         * allApps should be in order, move the item if there is empty cell
         * before.
         *
         * When the repositioned item was in an overlapped position,
         * that means there is one less item in the overlap position, remove one
         * item with the right poistion from the overlap apps list.
         *
         * @param allApps
         * @param overlapApps
         * @param emptyCellScreens
         */
    private void reorderAppsForEmptyCell(ArrayList<ItemInfo> allApps,
            ArrayList<ItemInfo> overlapApps, HashSet<Integer> emptyCellScreens) {
        for (Integer screenIndex : emptyCellScreens) {
            int nextItemPosition = 0;
            int newX = -1;
            int newY = -1;
            for (ItemInfo appInfo : allApps) {
                // Ignore invisible apps.
                //if (appInfo instanceof AppInfo && !((AppInfo) appInfo).isVisible) {
                 //   continue;
                //}

                if (appInfo.screenId == screenIndex) {
                    if (appInfo.mPos > nextItemPosition) {
                        for (ItemInfo overlapApp : overlapApps) {
                            if (overlapApp.screenId == appInfo.screenId
                                    && overlapApp.cellX == appInfo.cellX
                                    && overlapApp.cellY == appInfo.cellY) {
                                LauncherLog.d(TAG, "Remove item from overlap: overlapApp = "
                                        + overlapApp + ",appInfo = " + appInfo);
                                overlapApps.remove(overlapApp);
                                break;
                            }
                        }
                        appInfo.mPos = nextItemPosition;
                        newX = appInfo.mPos % AllApps.sAppsCellCountX;
                        newY = appInfo.mPos / AllApps.sAppsCellCountX;
                        if (LauncherLog.DEBUG) {
                            LauncherLog.d(TAG, "reorderAppsForEmptyCell: move item " + appInfo
                                    + " from (" + appInfo.cellX + "," + appInfo.cellY
                                    + ") to (" + newX + "," + newY + ").");
                        }
                        moveAllAppsItemInDatabase(mContext, appInfo, (int) appInfo.screenId,
                                             newX, newY);
                        nextItemPosition++;
                    } else if (appInfo.mPos == nextItemPosition) {
                        nextItemPosition = appInfo.mPos + 1;
                    } else {
                        LauncherLog.w(TAG, "This should never happen: appInfo = " + appInfo
                                + ",nextItemPosition = " + nextItemPosition);
                    }
                }
            }
        }
    }

    private final void dumpItemInfoList(ArrayList<? extends ItemInfo> items, String prefix) {
        for (ItemInfo info : items) {
            LauncherLog.d(TAG, prefix + " loadAllAppsExt: load " + info);
        }
    }

    private final void dumpAllAppLayout(final ItemInfo[][] screen) {
        LauncherLog.d(TAG, "AllApp layout: ");

        LauncherAppState app = LauncherAppState.getInstance();
        InvariantDeviceProfile profile = app.getInvariantDeviceProfile();
        int countX = (int) profile.numColumns;
        int countY = (int) profile.numRows;

        for (int y = 0; y < countY; y++) {
            String line = "";
            if (y > 0) {
                line += " | ";
            }

            for (int x = 0; x < countX; x++) {
                if (x < screen.length && y < screen[x].length) {
                    line += (screen[x][y] != null) ? "#" : ".";
                } else {
                    line += "!";
                }
            }
            LauncherLog.d(TAG, "[ " + line + " ]");
        }
    }

    /**
     * M: Check and get the apps which are in PMS but not in Launcher.db.
     */
    private final ArrayList<AppInfo>  getAppsInPMButNotInDB(UserHandleCompat user,
            ArrayList<ItemInfo> allItems) {
        final ArrayList<AppInfo> appInfoInPM = new ArrayList<AppInfo>();

        // Query for the set of apps
        final long qiaTime = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;
        List<LauncherActivityInfoCompat> apps =
            mLauncherModel.mLauncherApps.getActivityList(null, user);
        if (DEBUG_LOADERS) {
            Log.d(TAG, "getActivityList took "
                    + (SystemClock.uptimeMillis() - qiaTime) + "ms for user " + user);
            Log.d(TAG, "getActivityList got " + apps.size() + " apps for user " + user);
        }

        // Fail if we don't have any apps
        if (apps == null || apps.isEmpty()) {
            return appInfoInPM;
        }

        // Sort the applications by name
        final long sortTime = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;
        // op09 Collections.sort(apps, new LauncherModel.ShortcutNameComparator(
        //    mLauncherModel.mLoaderTask.mLabelCache));
        if (DEBUG_LOADERS) {
            Log.d(TAG, "sort took " + (SystemClock.uptimeMillis() - sortTime) + "ms");
        }

        // Store all the app info by ApplicationInfos
        for (int i = 0; i < apps.size(); i++) {
            LauncherActivityInfoCompat app = apps.get(i);
            // This builds the icon bitmaps.
            appInfoInPM.add(new AppInfo(mContext, app, user, mLauncherModel.mIconCache));
        }

        // Compare and remove the repeat ones
        for (ItemInfo item : allItems) {
            if (item instanceof AppInfo) {
                final AppInfo appInfo = (AppInfo) item;
                for (AppInfo app : appInfoInPM) {
                    if (app.componentName.equals(appInfo.componentName)) {
                        appInfoInPM.remove(app);
                        break;
                    }
                }
            }
        }

        return appInfoInPM;
    }

    /**
     * M: Add apps in PM not in DB.
     */
    private final void addAppsInPMButNotInDB(ArrayList<AppInfo> appsInPM) {
        final int onePageAppsNumber = AllApps.sAppsCellCountX * AllApps.sAppsCellCountY;
        AppInfo appInfo = null;
        int leftAppNumber = appsInPM.size();
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "checkPackageManagerForAppsNotInDB, there are " + leftAppNumber
                    + " apps left in PMS.");
        }

        for (int i = 0; i < leftAppNumber; ++i) {
            appInfo = appsInPM.get(i);
            appInfo.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;

            if (mCurrentPosInMaxPage >= onePageAppsNumber - 1) {
                sMaxAppsPageIndex += 1;
                mCurrentPosInMaxPage = 0;
            } else {
                mCurrentPosInMaxPage += 1;
            }

            if (LauncherLog.DEBUG) {
                LauncherLog.d(TAG, "checkPackageManagerForAppsNotInDB, Max page is "
                        + sMaxAppsPageIndex + ", current pos in max page is "
                        + mCurrentPosInMaxPage +
                        ", app user = " + appInfo.user);
            }

            appInfo.screenId = sMaxAppsPageIndex;
            appInfo.mPos = mCurrentPosInMaxPage;
            appInfo.cellX = appInfo.mPos % AllApps.sAppsCellCountX;
            appInfo.cellY = appInfo.mPos / AllApps.sAppsCellCountX;
            //appInfo.isVisible = true;

            if (LauncherLog.DEBUG) {
                LauncherLog.d(TAG, "checkPackageManagerForAppsNotInDB, insert " + " page="
                        + appInfo.screenId + ", cellX=" + appInfo.cellX
                        + ", cellY=" + appInfo.cellY + ", pos=" + appInfo.mPos);
            }

            addAllAppsItemToDatabase(mContext, appInfo, (int) appInfo.screenId, appInfo.cellX,
                    appInfo.cellY, false);
        }
    }
    //M:[OP09] End }@

    /**
     * M: mark the item's position and screen.
     */
    private class ItemPosition {
        int mScreen;
        int mPos;

        public ItemPosition(int screen, int pos) {
            this.mScreen = screen;
            this.mPos = pos;
        }
    }
}
