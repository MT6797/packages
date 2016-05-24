package com.mediatek.gallery3d.ext;

import android.view.View;

/**
 * ServerTimeout extension interface.
 */
public interface IRewindAndForwardExtension {
    /**
      *@return RewindAndForward View.
      *@internal
      */
    View getView();
    /**
      *@return right padding added.
      *@internal
      */
    int getPaddingRight();
    /**
      * @return the button of rewindAndForward height
      * @internal
      */
    int getHeight();
    /**
     * controller button position specified.
     * @return position.
     * @internal
     */
    int getControllerButtonPosition();
    /**
      * hide RewindAndForward View
      * @internal
      */
    void hide();
    /**
      * show RewindAndForward View
      * @internal
      */
    void show();
    /**
      * start do RewindAndForward View hiding animation.
      * @internal
      */
    void startHideAnimation();
    /**
      * cancle the hide anmiation
      * @internal
      */
    void cancelHideAnimation();
    /**
      * button response when click.
      * @param v
      * @internal
      */
    void onClick(View v);
    /**
      * set RewindAndForward View enabled or not
      * @param isEnabled
      * @internal
      */
    void setViewEnabled(boolean isEnabled);
    /**
      * onLayout of RewindAndForward View.
      * @param l
      * @param r
      * @param b
      * @param pr
      * @internal
      */
    void onLayout(int l, int r, int b, int pr);
    /**
      * update RewindAndForward UI.
      * @internal
      */
    void updateView();

}
