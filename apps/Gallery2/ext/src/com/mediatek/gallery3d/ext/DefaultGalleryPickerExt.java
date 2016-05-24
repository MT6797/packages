package com.mediatek.gallery3d.ext;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.ui.ActionModeHandler;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SlotView;

/**
 * Default gallery picker.
 */
public class DefaultGalleryPickerExt implements IGalleryPickerExt {
    private static final String TAG = "Gallery2/DefaultGalleryPickerExt";

    @Override
    public ActionModeHandler onCreate(AbstractGalleryActivity activity, Bundle data,
            ActionModeHandler actionMode, SelectionManager selectMgr) {
        return actionMode;
    }

    @Override
    public void onResume(SelectionManager selectMgr) {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void onCreateActionBar(Menu menu) {
    }

    @Override
    public boolean onSingleTapUp(SlotView slotView, MediaItem item) {
        return false;
    }

    @Override
    public boolean onItemSelected(MenuItem item) {
        return false;
    }
}
