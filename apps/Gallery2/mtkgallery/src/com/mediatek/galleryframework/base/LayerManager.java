package com.mediatek.galleryframework.base;

import android.content.Intent;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.mediatek.galleryframework.gl.MGLCanvas;
import com.mediatek.galleryframework.gl.MGLView;

public interface LayerManager {
    public void init(ViewGroup rootView, MGLView glRootView);
    public void resume();

    public void pause();

    public void destroy();

    public void switchLayer(Player player, MediaData data);

    public void drawLayer(MGLCanvas canvas, int width, int height);

    public boolean onTouch(MotionEvent event);

    public void onLayout(boolean changeSize, int left, int top, int right,
            int bottom);

    public void onKeyEvent(KeyEvent event);
    /**
     * Called by back key event.
     * @return true if current layer do back press operation.
     */
    public boolean onBackPressed();
    /**
     * Called action bar up menu pressed.
     * @return true if current layer do up press operation.
     */
    public boolean onUpPressed();
    public boolean onCreateOptionsMenu(Menu menu);
    public boolean onPrepareOptionsMenu(Menu menu);
    public boolean onOptionsItemSelected(MenuItem item);

    // call back the caller(Gallery) to do sth.
    // keep in mind: run on UI thread
    public interface IBackwardContoller {
        public interface IOnActivityResultListener {
            public boolean onActivityResult(int requestCode, int resultCode, Intent data);
        }
        /**
         * toggle visibility of host(Gallery)'s ActionBar
         * @param visibility the visibility to arrive at
         * @param allowAutoHideByHost whether to allow host(Gallery) to auto hide ActionBar
         */
        public void toggleBars(boolean visibility);
        public void redirectCurrentMedia(Uri uri, boolean fromActivityResult);
        public void startActivityForResult(Intent intent, int requestCode,
                IOnActivityResultListener resultListener);
        public void notifyDataChange(MediaData mediaData);
        /**
         * Layer fresh the the listener to change visibility for bottom view.
         * @param showBottomControls the visibility of view.
         */
        public void refreshBottomControls(boolean showBottomControls);
        /**
         * Get photoPage bottom view visibility for fresh layer.
         * @return true if photoPage bottom menu is visible.
         */
        public boolean getBottomMenuVisibility();
    }

    public boolean onActionBarVisibilityChange(boolean newVisibility);

    public void onFilmModeChange(boolean isFilmMode);

    /**
     * Set backward controller to layer, This is an bright for photoPage and layer.
     * @param backwardControllerForLayer the callback.
     */
    public void setBackwardController(
            LayerManager.IBackwardContoller backwardControllerForLayer);

    /**
     * Fresh layer and called by main handler.
     * @param newVisibility current visibility of bottom view.
     * @return whether success fresh layer.
     */
    public boolean freshLayers(boolean newVisibility);
}