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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;

import com.android.launcher3.LauncherSettings.Favorites;
import com.android.launcher3.compat.LauncherActivityInfoCompat;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.compat.UserManagerCompat;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Represents a launchable icon on the workspaces and in folders.
 */
public class ShortcutInfo extends ItemInfo {

    public static final int DEFAULT = 0;

    /**
     * The shortcut was restored from a backup and it not ready to be used. This is automatically
     * set during backup/restore
     */
    public static final int FLAG_RESTORED_ICON = 1;

    /**
     * The icon was added as an auto-install app, and is not ready to be used. This flag can't
     * be present along with {@link #FLAG_RESTORED_ICON}, and is set during default layout
     * parsing.
     */
    public static final int FLAG_AUTOINTALL_ICON = 2; //0B10;

    /**
     * The icon is being installed. If {@link FLAG_RESTORED_ICON} or {@link FLAG_AUTOINTALL_ICON}
     * is set, then the icon is either being installed or is in a broken state.
     */
    public static final int FLAG_INSTALL_SESSION_ACTIVE = 4; // 0B100;

    /**
     * Indicates that the widget restore has started.
     */
    public static final int FLAG_RESTORE_STARTED = 8; //0B1000;

    /**
     * Indicates if it represents a common type mentioned in {@link CommonAppTypeParser}.
     * Upto 15 different types supported.
     */
    public static final int FLAG_RESTORED_APP_TYPE = 0B0011110000;

    /**
     * The intent used to start the application.
     */
    public Intent intent;

    /**
     * Indicates whether the icon comes from an application's resource (if false)
     * or from a custom Bitmap (if true.)
     * TODO: remove this flag
     */
    public boolean customIcon;

    /**
     * Indicates whether we're using the default fallback icon instead of something from the
     * app.
     */
    boolean usingFallbackIcon;

    /**
     * Indicates whether we're using a low res icon
     */
    boolean usingLowResIcon;

    /**
     * If isShortcut=true and customIcon=false, this contains a reference to the
     * shortcut icon as an application's resource.
     */
    public Intent.ShortcutIconResource iconResource;

    /**
     * The application icon.
     */
    private Bitmap mIcon;

    /**
     * Indicates that the icon is disabled due to safe mode restrictions.
     */
    public static final int FLAG_DISABLED_SAFEMODE = 1;

    /**
     * Indicates that the icon is disabled as the app is not available.
     */
    public static final int FLAG_DISABLED_NOT_AVAILABLE = 2;

    /**
     * Could be disabled, if the the app is installed but unavailable (eg. in safe mode or when
     * sd-card is not available).
     */
    int isDisabled = DEFAULT;

    int status;

    /**
     * The installation progress [0-100] of the package that this shortcut represents.
     */
    private int mInstallProgress;

    /**
     * Refer {@link AppInfo#firstInstallTime}.
     */
    public long firstInstallTime;

    /**
     * TODO move this to {@link status}
     */
    int flags = 0;

    /**
     * If this shortcut is a placeholder, then intent will be a market intent for the package, and
     * this will hold the original intent from the database.  Otherwise, null.
     * Refer {@link #FLAG_RESTORE_PENDING}, {@link #FLAG_INSTALL_PENDING}
     */
    Intent promisedIntent;

    //M:[OP09][CF] @{
    public ComponentName mComponentName;
    public boolean mIsVisible = true;
    public boolean mStateChanged;
    public int mIsForPadding = AppInfo.NOT_PADDING_APP;
    //M:[OP09][CF] }@

    ShortcutInfo() {
        itemType = LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT;
    }

    public Intent getIntent() {
        return intent;
    }

    ShortcutInfo(Intent intent, CharSequence title, CharSequence contentDescription,
            Bitmap icon, UserHandleCompat user) {
        this();
        this.intent = intent;
        this.title = Utilities.trim(title);
        this.contentDescription = contentDescription;
        mIcon = icon;
        this.user = user;
    }

    public ShortcutInfo(Context context, ShortcutInfo info) {
        super(info);
        title = Utilities.trim(info.title);
        intent = new Intent(info.intent);
        if (info.iconResource != null) {
            iconResource = new Intent.ShortcutIconResource();
            iconResource.packageName = info.iconResource.packageName;
            iconResource.resourceName = info.iconResource.resourceName;
        }
        mIcon = info.mIcon; // TODO: should make a copy here.  maybe we don't need this ctor at all
        customIcon = info.customIcon;
        flags = info.flags;
        firstInstallTime = info.firstInstallTime;
        user = info.user;
        status = info.status;
    }

    /** TODO: Remove this.  It's only called by ApplicationInfo.makeShortcut. */
    public ShortcutInfo(AppInfo info) {
        super(info);
        title = Utilities.trim(info.title);
        intent = new Intent(info.intent);
        customIcon = false;
        flags = info.flags;
        firstInstallTime = info.firstInstallTime;
        //M:[OP09][CF] @{
        if (LauncherExtPlugin.getInstance().getWorkspaceExt(
                  LauncherAppState.getInstance().getContext())
                .supportEditAndHideApps()) {
            mComponentName = info.componentName;
            mIsVisible = info.isVisible;
            mIsForPadding = info.isForPadding;
            mStateChanged = info.stateChanged;
        }
        //M:[OP09][CF] }@
    }

    public void setIcon(Bitmap b) {
        mIcon = b;
    }

    public Bitmap getIcon(IconCache iconCache) {
        if (mIcon == null) {
            updateIcon(iconCache);
        }
        return mIcon;
    }

    public void updateIcon(IconCache iconCache, boolean useLowRes) {
        if (itemType == Favorites.ITEM_TYPE_APPLICATION) {
            iconCache.getTitleAndIcon(this, promisedIntent != null ? promisedIntent : intent, user,
                    useLowRes);
        }
    }

    public void updateIcon(IconCache iconCache) {
        updateIcon(iconCache, shouldUseLowResIcon());
    }

    @Override
    void onAddToDatabase(Context context, ContentValues values) {
        super.onAddToDatabase(context, values);

        String titleStr = title != null ? title.toString() : null;
        values.put(LauncherSettings.BaseLauncherColumns.TITLE, titleStr);

        String uri = promisedIntent != null ? promisedIntent.toUri(0)
                : (intent != null ? intent.toUri(0) : null);
        values.put(LauncherSettings.BaseLauncherColumns.INTENT, uri);
        values.put(LauncherSettings.Favorites.RESTORED, status);

        if (customIcon) {
            values.put(LauncherSettings.BaseLauncherColumns.ICON_TYPE,
                    LauncherSettings.BaseLauncherColumns.ICON_TYPE_BITMAP);
            writeBitmap(values, mIcon);
        } else {
            if (!usingFallbackIcon) {
                writeBitmap(values, mIcon);
            }
            if (iconResource != null) {
                values.put(LauncherSettings.BaseLauncherColumns.ICON_TYPE,
                        LauncherSettings.BaseLauncherColumns.ICON_TYPE_RESOURCE);
                values.put(LauncherSettings.BaseLauncherColumns.ICON_PACKAGE,
                        iconResource.packageName);
                values.put(LauncherSettings.BaseLauncherColumns.ICON_RESOURCE,
                        iconResource.resourceName);
            }
        }
    }

    @Override
    public String toString() {
        return "ShortcutInfo(title=" + title + "intent=" + intent + "id=" + this.id
                + " type=" + this.itemType + " container=" + this.container + " screen=" + screenId
                + " cellX=" + cellX + " cellY=" + cellY + " spanX=" + spanX + " spanY=" + spanY
                + " dropPos=" + Arrays.toString(dropPos) + " unreadNum= " + unreadNum
                + " user=" + user + ")";
    }

    public static void dumpShortcutInfoList(String tag, String label,
            ArrayList<ShortcutInfo> list) {
        Log.d(tag, label + " size=" + list.size());
        for (ShortcutInfo info: list) {
            Log.d(tag, "   title=\"" + info.title + " icon=" + info.mIcon
                    + " customIcon=" + info.customIcon);
        }
    }

    public ComponentName getTargetComponent() {
        return promisedIntent != null ? promisedIntent.getComponent() : intent.getComponent();
    }

    public boolean hasStatusFlag(int flag) {
        return (status & flag) != 0;
    }


    public final boolean isPromise() {
        return hasStatusFlag(FLAG_RESTORED_ICON | FLAG_AUTOINTALL_ICON);
    }

    public int getInstallProgress() {
        return mInstallProgress;
    }

    public void setInstallProgress(int progress) {
        mInstallProgress = progress;
        status |= FLAG_INSTALL_SESSION_ACTIVE;
    }

    public boolean shouldUseLowResIcon() {
        return usingLowResIcon && container >= 0 && rank >= FolderIcon.NUM_ITEMS_IN_PREVIEW;
    }

    public static ShortcutInfo fromActivityInfo(LauncherActivityInfoCompat info, Context context) {
        final ShortcutInfo shortcut = new ShortcutInfo();
        shortcut.user = info.getUser();
        shortcut.title = Utilities.trim(info.getLabel());
        shortcut.contentDescription = UserManagerCompat.getInstance(context)
                .getBadgedLabelForUser(info.getLabel(), info.getUser());
        shortcut.customIcon = false;
        shortcut.intent = AppInfo.makeLaunchIntent(context, info, info.getUser());
        shortcut.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
        shortcut.flags = AppInfo.initFlags(info);
        shortcut.firstInstallTime = info.getFirstInstallTime();
        return shortcut;
    }

    //M:[OP09][CF] @{
    /**
    *M: makeAppInfo by switch between shortcutInfo and appInfo.
    *@return appInfo
    **/
    public AppInfo makeAppInfo() {
        final AppInfo appInfo = new AppInfo();
        // copy basic info
        appInfo.copyFrom(this);

        appInfo.componentName = this.mComponentName;

        appInfo.flags = this.flags;
        appInfo.firstInstallTime = this.firstInstallTime;
        appInfo.title = this.title;
        appInfo.iconBitmap = this.mIcon;
        appInfo.isVisible = this.mIsVisible;
        appInfo.stateChanged = this.mStateChanged;

        // copy intent
        appInfo.intent = new Intent(this.intent);
        long serialNumber = UserManagerCompat.getInstance(
                LauncherAppState.getInstance().getContext())
                .getSerialNumberForUser(user);
        intent.putExtra(EXTRA_PROFILE, serialNumber);

        if (appInfo.componentName == null
            && appInfo.intent.getComponent() != null) {
            appInfo.componentName = appInfo.intent.getComponent();
        }

        return appInfo;
    }

    ShortcutInfo copy() {
        final ShortcutInfo info = new ShortcutInfo();
        info.copyFrom(this);
        if (this.title != null) {
            info.title = this.title.toString();
        }
        if (this.contentDescription != null) {
            info.contentDescription = this.contentDescription.toString();
        }

        info.intent = new Intent(this.intent);
        if (info.iconResource != null) {
            info.iconResource = new Intent.ShortcutIconResource();
            info.iconResource.packageName = this.iconResource.packageName;
            info.iconResource.resourceName = this.iconResource.resourceName;
        }
        info.mIcon = this.mIcon;
        info.customIcon = this.customIcon;
        info.flags = this.flags;
        info.firstInstallTime = this.firstInstallTime;
        info.status = this.status;

        info.customIcon = this.customIcon;
        info.usingFallbackIcon = this.usingFallbackIcon;
        info.isDisabled = this.isDisabled;
        info.mInstallProgress = this.mInstallProgress;
        info.promisedIntent = this.promisedIntent;

        info.mComponentName = this.mComponentName;
        info.mIsVisible = this.mIsVisible;
        info.mStateChanged = this.mStateChanged;

        return info;
    }
    //[OP09][CF] }@
}

