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
 * Gallery picker plugin interface.
 */
public interface IGalleryPickerExt {
    /**
     * When AlbumPage/ContainerPage is created.
     *
     * @param activity activity instance
     * @param data data bundle from intent
     * @param actionMode ActionModeHandler
     * @param selectMgr selection manager
     * @return ActionModeHandler ActionModeHandler used in plugin
     * @internal
     */
    public ActionModeHandler onCreate(AbstractGalleryActivity activity, Bundle data,
            ActionModeHandler actionMode, SelectionManager selectMgr);

    /**
     * When AlbumPage/ContainerPage is resume.
     *
     * @param selectMgr selection manager instance
     * @internal
     */
    public void onResume(SelectionManager selectMgr);

    /**
     * When AlbumPage/ContainerPage is pause.
     * @internal
     */
    public void onPause();

    /**
     * When AlbumPage/ContainerPage is Destroy.
     * @internal
     */
    public void onDestroy();

    /**
     * When create action bar.
     *
     * @param menu menu instace
     * @internal
     */
    public void onCreateActionBar(Menu menu);

    /**
     * When single tap up event happens on specific slot.
     *
     * @param slotView slotview instance to update UI
     * @param item single tap up media item
     * @return boolean the single tap up event is handled or not
     * @internal
     */
    public boolean onSingleTapUp(SlotView slotView, MediaItem item);

    /**
     * When menu item is selected.
     *
     * @param item menu item instace
     * @return boolean the event is handled or not
     * @internal
     */
    public boolean onItemSelected(MenuItem item);
}
