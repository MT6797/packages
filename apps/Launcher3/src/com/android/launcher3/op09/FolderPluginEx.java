/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/

package com.android.launcher3;

import android.content.Context;

import com.android.launcher3.DropTarget.DragObject;



public class FolderPluginEx {
    private static final String TAG = "Launcher3.FolderPluginEx";

    //M: [OP09][CF] @{
    private boolean mSupportEditAndHideApps;
    private boolean mIsPageViewFolder = false;
    //M: [OP09][CF] }@
    private Launcher mLauncher;
    private Folder mFolder;

    public FolderPluginEx(Context context, Folder folder) {
        mLauncher = (Launcher) context;
        mFolder = folder;

        mSupportEditAndHideApps = LauncherExtPlugin.getInstance()
            .getWorkspaceExt(mFolder.getContext()).supportEditAndHideApps();
    }

    //M:[OP09][CF] @{
    void setIsPageViewFolder(boolean isPageViewFolder) {
        this.mIsPageViewFolder = isPageViewFolder;

    }

    public boolean isPageViewFolder() {
        return mIsPageViewFolder;
    }

    FolderIcon getFolderIcon() {
        return mFolder.mFolderIcon;
    }

    /**
     * M: Update system delete of the content shortcut.
     */
    void updateContentDeleteButton() {
        int count = mFolder.mContent.getChildCount();
        for (int i = 0; i < count; i++) {
            ShortcutAndWidgetContainer container =
                mFolder.mContent.getPageAt(i).getShortcutsAndWidgets();

        int childCount = container.getChildCount();
            for (int j = 0; j < childCount; j++) {
            BubbleTextView shortcut = (BubbleTextView) container.getChildAt(i);
            if (shortcut != null) {
                shortcut.invalidate();
            }
            }
        }
    }
    //M:[OP09][CF] }@

    public boolean isSupportEditAndHideApps() {
        return mSupportEditAndHideApps;
    }

    public void onDrop(DragObject d) {
        if (mSupportEditAndHideApps && isPageViewFolder()) {
            int dragScreenId = (int) ((ItemInfo) d.dragInfo).screenId;
            int currentScreenId = (int) mFolder.mInfo.screenId;
            if (dragScreenId == currentScreenId &&
                    ((ItemInfo) d.dragInfo).container != mFolder.mInfo.id) {
                /*final AppsCustomizePagedView pageview = mLauncher.getPagedView();
                int[] emptyCell = new int[2];
                emptyCell[0] = ((ItemInfo) d.dragInfo).cellX;
                emptyCell[1] = ((ItemInfo) d.dragInfo).cellY;
                int[] targetCell = new int[2];
                targetCell[0] = mFolder.mInfo.cellX;
                targetCell[1] = mFolder.mInfo.cellY;
                LauncherLog.d(TAG, "onDrop: emptyCell = (" + emptyCell[0] + "," + emptyCell[1]
                                   + "), App.mEmptyCell=("
                                    + pageview.mViewPluginEx.mEmptyCell[0]  + ","
                                    + pageview.mViewPluginEx.mEmptyCell[1] + ")");

                if (emptyCell[0] != pageview.mViewPluginEx.mEmptyCell[0]
                    && pageview.mViewPluginEx.mEmptyCell[0] != -1) {
                    emptyCell[0] = pageview.mViewPluginEx.mEmptyCell[0];
                }
                if (emptyCell[1] != pageview.mViewPluginEx.mEmptyCell[1]
                    && pageview.mViewPluginEx.mEmptyCell[1] != -1) {
                    emptyCell[1] = pageview.mViewPluginEx.mEmptyCell[1];
                }
                AppsCustomizeCellLayout layout = (AppsCustomizeCellLayout) pageview
                    .getPageAt(dragScreenId);
                LauncherLog.d(TAG, "onDrop: dragScreenId = " + dragScreenId
                    + ", currentScreen = " + currentScreenId
                    + ", targetCell=(" + targetCell[0] + "," + targetCell[1] + "), emptyCell=("
                    + emptyCell[0]  + "," + emptyCell[1] + ")"
                    + ", layout = " + layout);
                pageview.mViewPluginEx.reorderForFolderCreateOrDelete(
                    emptyCell, targetCell, layout, true);
                // Update all apps position in the page after realTimeReorder.
                pageview.mViewPluginEx.updateItemLocationsInDatabase(layout);*/ //op09
            }
        }
    }

    public void run() {
        /* op09 if (mSupportEditAndHideApps && mIsPageViewFolder) {
            // M:[OP09][CF] @{
            final AppsCustomizePagedView pageview = mLauncher.getPagedView();
            final AppsCustomizeCellLayout celllayout = (AppsCustomizeCellLayout) pageview
                    .getPageAt((int) mFolder.mInfo.screenId);
            BubbleTextView child = null;
            LauncherLog.d(TAG, "replaceFolderWithFinalItem, count = " + mFolder.getItemCount());
            if (mFolder.getItemCount() == 1 && mFolder.mInfo.contents.size() == 1) {
                ShortcutInfo finalItem = mFolder.mInfo.contents.get(0);
                AppInfo info = finalItem.makeAppInfo();
                LauncherLog.d(TAG, "replaceFolderWithFinalItem, info = " + info);
                info.container = AllApps.CONTAINER_ALLAPP;
                info.screenId = mFolder.mInfo.screenId;
                info.cellX = mFolder.mInfo.cellX;
                info.cellY = mFolder.mInfo.cellY;
                info.mPos = mFolder.mInfo.cellY * AllApps.sAppsCellCountX + mFolder.mInfo.cellX;
                //if (AppsCustomizePagedViewPluginEx.isHideApps(info)) {  //op09
                if (false) {
                    info.isVisible = false;
                } else {
                    child = (BubbleTextView) LayoutInflater.from(mFolder.getContext()).inflate(
                            R.layout.apps_customize_application, celllayout, false);
                    if (Launcher.DISABLE_APPLIST_WHITE_BG) {
                        child.setTextColor(mFolder.getContext().getResources().getColor(
                                        R.color.quantum_panel_transparent_bg_text_color));
                    }
                    // update the delete button
                    if (Launcher.isInEditMode()) {
                        child.setDeleteButtonVisibility(!pageview.mViewPluginEx.isSystemApp(info));
                    }
                    child.applyFromApplicationInfo(info);
                    child.setOnClickListener(pageview);
                    child.setOnLongClickListener(pageview);
                    child.setOnTouchListener(pageview);
                    child.setOnKeyListener(pageview);
                }
                // todolei:the last final item need how to do ?
                LauncherModelPluginEx.moveAllAppsItemInDatabase(mLauncher, info,
                        AllApps.CONTAINER_ALLAPP, mFolder.mInfo.screenId
                        , mFolder.mInfo.cellX, mFolder.mInfo.cellY);
                if (info.isVisible) {
                    pageview.mViewPluginEx.addOrRemoveApp(info, true);
                } else {
                    pageview.mViewPluginEx.addAppToList(info);
                }

                mFolder.mInfo.contents.remove(0);
                final View v = mFolder.getViewForInfo(finalItem);
                if (v != null) {
                    mFolder.mContent.removeView(v);
                }
            }

            if (mFolder.getItemCount() <= 1) {
                // Remove the folder
                LauncherLog.d(TAG, "replaceFolderWithFinalItem, remove folder");
                LauncherModelPluginEx.deleteFolderItemFromDatabase(mFolder.getContext(),
                    mFolder.mInfo);
                if (celllayout != null) {
                    // b/12446428 -- sometimes the cell layout has already gone away?
                    celllayout.removeView(mFolder.mFolderIcon);
                }
                if (mFolder.mFolderIcon instanceof DropTarget) {
                    mFolder.mDragController.removeDropTarget((DropTarget) mFolder.mFolderIcon);
                }
                //Remove folder
                mLauncher.getPagedView().mViewPluginEx.addOrRemoveFolder(mFolder.mInfo, false);
            }
            if (child != null) {
                //update cell info again after drag and drop before use add by lei:
                int pos = mFolder.mInfo.cellY * AllApps.sAppsCellCountX + mFolder.mInfo.cellX;

                LauncherLog.d(TAG, "replaceFolderWithFinalItem, pos = " + pos
                   + ", child=" + child + ",x=" + mFolder.mInfo.cellX + ",y="
                   + mFolder.mInfo.cellY);

                celllayout.addViewToCellLayout(child, -1, pos,
                        new CellLayout.LayoutParams(mFolder.mInfo.cellX
                        , mFolder.mInfo.cellY, 1, 1), false);
            }
            //M:[OP09][CF] }@
        }*/
    }

}

