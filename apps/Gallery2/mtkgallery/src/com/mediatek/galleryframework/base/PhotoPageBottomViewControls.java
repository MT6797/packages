package com.mediatek.galleryframework.base;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import com.android.gallery3d.R;
import com.mediatek.galleryframework.util.MtkLog;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Control stereo bottom view visibility.
 * and keep the same animation as google bottom view.
 */
public class PhotoPageBottomViewControls implements OnClickListener {
    private final static String TAG = "MtkGallery2/PhotopageBottomViewControls";
    private static final int CONTAINER_ANIM_DURATION_MS = 190;
    private static final int CONTROL_ANIM_DURATION_MS = 140;
    private int mCurrentId;
    private Activity mActivity;
    private ViewGroup mRootView;
    private MediaData mMediaData;
    private LayoutInflater mFlater;
    private Animation mContainerAnimIn = new AlphaAnimation(0f, 1f);
    private Animation mContainerAnimOut = new AlphaAnimation(1f, 0f);
    private Map<String, ViewGroup> mContainerMaps = new HashMap<String, ViewGroup>();
    private Map<View, Boolean> mCurrentVisible = new HashMap<View, Boolean>();

    private LayerManager.IBackwardContoller mBackwardContoller;
    private BottomControlLayer mLayer;

    /**
     * Constructor.
     * @param activity the relative activity.
     * @param root the root view.
     * @param layer the layer that for PhotoPageBottomViewControls.
     */
    public PhotoPageBottomViewControls(Activity activity, final ViewGroup root,
            BottomControlLayer layer) {
        MtkLog.d(TAG, "<onCreate> onCreate");
        mRootView = root;
        mActivity = activity;
        mLayer = layer;
        mFlater = LayoutInflater.from(mActivity);
        mContainerAnimIn.setDuration(CONTAINER_ANIM_DURATION_MS);
        mContainerAnimOut.setDuration(CONTAINER_ANIM_DURATION_MS);
        inflateContainer(R.layout.m_start_stereo);
    }

    public void setBackwardContoller(LayerManager.IBackwardContoller backwardContoller) {
        mBackwardContoller = backwardContoller;
    }

    /**
     * Inflate the layer for the view id.
     * @param id view id.
     */
    public void inflateLayer(int id) {
        inflateContainer(id);
    }

    /**
     * Response for back press Event.
     * @return whether stay in the stereo layer or back up to photo view.
     */
    public boolean onBackPressed() {
        if (mCurrentId == R.layout.m_stereo || mCurrentId == R.layout.m_stereo_touch) {
            inflateContainer(R.layout.m_start_stereo);
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param visible The visibility of the view.
     * @param hasAnimation whether has animation for visibility changed.
     * @return successful refresh.
     */
    public boolean fresh(boolean visible, boolean hasAnimation) {
        boolean isVisiable = getContainerVisibility(visible);
        refresh(isVisiable, false, hasAnimation);
        return true;
    }

    public void setData(MediaData data) {
        mMediaData = data;
    }

    /**
     * Hide all view.
     */
    public void hideAll() {
        Iterator<Entry<String, ViewGroup>> iter = mContainerMaps.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, ViewGroup> entry = iter.next();
            ViewGroup view = entry.getValue();
            updateBottomView(view, false, true, false);
            mRootView.removeView(view);
        }
    }

    /**
     * On single photo mode Means, can not do scale and sliding operation.
     * @return whether on single photo mode.
     */
    public boolean onSinglePhotoMode() {
        return !onStereoStartView();
    }

    /**
     * Determine whether on stereo view.
     * @return whether on stereo view.
     */
    public boolean onStereoView() {
        return mCurrentId == R.layout.m_stereo;
    }

    /**
     * Determine whether on stereo start view.
     * @return whether on stereo start view.
     */
    public boolean onStereoStartView() {
        return mCurrentId == R.layout.m_start_stereo;
    }

    /**
     * Determine whether on stereo touch view.
     * @return whether on stereo touch view.
     */
    public boolean onStereoTouchView() {
        return mCurrentId == R.layout.m_stereo_touch;
    }

    /**
     * Hide current view.
     */
    public void hide() {
        ViewGroup currentView = getCurrentViewGroup(mCurrentId);
        if (currentView != null) {
            MtkLog.d(TAG, "<clean> currentView = " + currentView);
            updateBottomView(currentView, false, false, true);
        }
    }

    /**
     * Release all view.
     */
    public void destory() {
        Iterator<Entry<String, ViewGroup>> iter = mContainerMaps.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, ViewGroup> entry = iter.next();
            ViewGroup view = entry.getValue();
            release(view);
            mRootView.removeView(view);
        }
        mContainerMaps.clear();
        mCurrentVisible.clear();
    }

    private void release(ViewGroup currentView) {
        for (int i = currentView.getChildCount() - 1; i >= 0; i--) {
            View child = currentView.getChildAt(i);
            if (child != null && child instanceof IconView) {
                ((IconView) child).recycle();
            }
        }
    }

    private void inflateContainer(int id) {
        if (mLayer != null) {
            boolean isNewView = create(id);
            refresh(false, false, true);
            add();
            if (id == R.layout.m_start_stereo) {
                mLayer.sendMessageToNotifier(Layer.MSG_BOTTOM_CONTROL_HIDE);
            } else if (id == R.layout.m_stereo) {
                mLayer.sendMessageToNotifier(Layer.MSG_BOTTOM_CONTROL_SHOW);
            }
            if (mBackwardContoller != null) {
                mBackwardContoller.refreshBottomControls(!onSinglePhotoMode());
            }
        }
    }

    private ViewGroup getCurrentViewGroup(int id) {
        return mContainerMaps.get(Integer.toString(id));
    }

    private Animation getControlAnimForVisibility(boolean visible) {
        Animation anim = visible ? new AlphaAnimation(0f, 1f) : new AlphaAnimation(1f, 0f);
        anim.setDuration(CONTROL_ANIM_DURATION_MS);
        return anim;
    }

    private void updateVisiable(View child, boolean visiable, boolean hasAnimation) {
        boolean isVisiable = mCurrentVisible.get(child);
        MtkLog.d(TAG, "<updateVisiable> GONE child = " + child + " isVisiable = " + isVisiable
                + " visiable = " + visiable + " hasAnimation = " + hasAnimation
                + " CONTROL_ANIM_DURATION_MS = " + CONTROL_ANIM_DURATION_MS
                + " CONTAINER_ANIM_DURATION_MS = " + CONTAINER_ANIM_DURATION_MS);

        if (isVisiable == visiable) {
            return;
        }
        child.clearAnimation();
        if (hasAnimation) {
            child.startAnimation(getControlAnimForVisibility(visiable));
        }
        if (child instanceof IconView) {
            ((IconView) child).update(visiable, this);
        } else {
            child.setVisibility(visiable ? View.VISIBLE : View.INVISIBLE);
            child.setClickable(visiable);
            child.setOnClickListener(visiable ? this : null);
        }
    }

    private boolean getContainerVisibility(boolean visiable) {
        if (mLayer != null) {
            return mLayer.getVisibility(mCurrentId, visiable);
        }
        return false;
    }

    private void refresh(boolean visiable, boolean force, boolean hasAnimation) {
        Iterator<Entry<String, ViewGroup>> iter = mContainerMaps.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, ViewGroup> entry = iter.next();
            ViewGroup view = entry.getValue();
            if (Integer.toString(mCurrentId).equals(entry.getKey())) {
                updateBottomView(view, visiable, force, hasAnimation);
                view.requestLayout();
            } else {
                updateBottomView(view, false, true, false);
            }
        }
    }

    private void updateBottomView(ViewGroup currentView, boolean visiable, boolean force,
            boolean hasAnimation) {
        boolean containerVisible = mCurrentVisible.get(currentView);
        boolean containerVisibilityChanged = (visiable != containerVisible);
        MtkLog.d(TAG, "<updateBottomView> currentView = " + currentView + " force = " + force
                + " containerVisible = " + containerVisible + " visiable = " + visiable
                + " hasAnimation = " + hasAnimation);
        if (containerVisibilityChanged || force) {
            updateContainerVisiable(currentView, visiable, hasAnimation);
            mCurrentVisible.put(currentView, visiable);
        }
        if (!visiable) {
            return;
        }
        for (int i = currentView.getChildCount() - 1; i >= 0; i--) {
            View child = currentView.getChildAt(i);
            if (child != null) {
                visiable = mLayer.getVisibility(child.getId());
                updateDrawable(child);
                updateVisiable(child, visiable, hasAnimation);
                mCurrentVisible.put(child, visiable);
            }
        }
    }

    private void updateDrawable(View child) {
        MtkLog.d(TAG, "<updateDrawable> mMediaData =" + mMediaData);
        if (mMediaData == null) {
            return;
        }
        if (mCurrentId == R.layout.m_start_stereo) {
            return;
        }
        if (child instanceof IconView) {
            ((IconView) child).updateImage(mMediaData.depth_image == 1);
        }
    }

    private void updateContainerVisiable(ViewGroup currentView, boolean visiable,
            boolean hasAnimation) {
        currentView.clearAnimation();
        if (visiable) {
            if (hasAnimation) {
                mContainerAnimIn.reset();
                currentView.startAnimation(mContainerAnimIn);
            }
            currentView.setVisibility(View.VISIBLE);
        } else {
            if (hasAnimation) {
                mContainerAnimOut.reset();
                currentView.startAnimation(mContainerAnimOut);
            }
            currentView.setVisibility(View.INVISIBLE);
        }
    }

    private boolean create(int id) {
        mCurrentId = id;
        boolean isNewView = false;
        ViewGroup currentView = getCurrentViewGroup(id);
        if (currentView == null) {
            ViewGroup view = (ViewGroup) mFlater.inflate(id, mRootView, false);
            mCurrentVisible.put(view, false);
            for (int i = view.getChildCount() - 1; i >= 0; i--) {
                View child = view.getChildAt(i);
                mCurrentVisible.put(child, false);
            }
            mContainerMaps.put(Integer.toString(id), view);
            isNewView = true;
        }
        return isNewView;
    }

    private void add() {
        Iterator<Entry<String, ViewGroup>> iter = mContainerMaps.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, ViewGroup> entry = iter.next();
            ViewGroup view = entry.getValue();
            if (Integer.toString(mCurrentId).equals(entry.getKey())) {
                mRootView.addView(view);
            } else {
                mRootView.removeView(view);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (mLayer != null) {
            int id = v.getId();
            switch (id) {
            case R.id.m_stereo_start_menu:
                inflateContainer(R.layout.m_stereo);
                break;
            case R.id.m_stereo_touch_menu:
                inflateContainer(R.layout.m_stereo_touch);
                break;
            case R.id.m_stereo_refocus:
            case R.id.m_stereo_copy_paste:
            case R.id.m_stereo_background:
            case R.id.m_stereo_fancy_color:
                hideAll();
                break;
            default:
                break;
            }
            mLayer.onClickEvent(v);
        }
    }
}
