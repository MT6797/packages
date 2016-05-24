package com.mediatek.gallery3d.ext;

import android.view.View;

/**
 * Default implemention class of IRewindAndForwardExtension.
 */
public class DefaultRewindAndForwardExtension extends DefaultActivityHooker implements IRewindAndForwardExtension {

    @Override
    public View getView() {
        return null;
    }

    @Override
    public int getPaddingRight() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public int getControllerButtonPosition() {
        return 0;
    }

    @Override
    public void hide() {
    }

    @Override
    public void show() {
    }

    @Override
    public void startHideAnimation() {
    }

    @Override
    public void cancelHideAnimation() {
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public void setViewEnabled(boolean isEnabled) {
    }

    @Override
    public void onLayout(int l, int r, int b, int pr) {
    }

    @Override
    public void updateView(){
    }

}
