/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.launcher3.allapps;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.android.launcher3.BaseRecyclerView;
import com.android.launcher3.BaseRecyclerViewFastScrollBar;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.FolderIcon;
import com.android.launcher3.FolderIcon.FolderRingAnimator;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherExtPlugin;
import com.android.launcher3.R;
import com.android.launcher3.Stats;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.Thunk;

import com.mediatek.launcher3.ext.LauncherLog;

import java.util.ArrayList;
import java.util.List;

/**
 * A RecyclerView with custom fast scroll support for the all apps view.
 */
public class AllAppsRecyclerView extends BaseRecyclerView
        implements Stats.LaunchSourceProvider {
    static final String TAG = "Launcher3.AllAppsRecyclerView";

    private static final int FAST_SCROLL_MODE_JUMP_TO_FIRST_ICON = 0;
    private static final int FAST_SCROLL_MODE_FREE_SCROLL = 1;

    private static final int FAST_SCROLL_BAR_MODE_DISTRIBUTE_BY_ROW = 0;
    private static final int FAST_SCROLL_BAR_MODE_DISTRIBUTE_BY_SECTIONS = 1;

    private AlphabeticalAppsList mApps;
    private int mNumAppsPerRow;

    @Thunk BaseRecyclerViewFastScrollBar.FastScrollFocusableView mLastFastScrollFocusedView;
    @Thunk int mPrevFastScrollFocusedPosition;
    @Thunk int mFastScrollFrameIndex;
    @Thunk final int[] mFastScrollFrames = new int[10];

    private final int mFastScrollMode = FAST_SCROLL_MODE_JUMP_TO_FIRST_ICON;
    private final int mScrollBarMode = FAST_SCROLL_BAR_MODE_DISTRIBUTE_BY_ROW;

    private ScrollPositionState mScrollPosState = new ScrollPositionState();

    private AllAppsBackgroundDrawable mEmptySearchBackground;
    private int mEmptySearchBackgroundTopOffset;

    ///M. op09_new edit mode
    private boolean mSupportEditAndHideApps;
    private Drawable mCrosshairsDrawable;
    private int numColumnsInOnePags;
    int[] mCellSize = new int[2];
    int[] mCrrentDragCell = {-1, -1};
    private float mLastTouchY = 0;

    private ArrayList<FolderRingAnimator> mFolderOuterRings =
        new ArrayList<FolderRingAnimator>();
    private Drawable sSharedOuterRingDrawable;
    private Drawable sSharedInnerRingDrawable;

    DeviceProfile mGrid;
    private int mNumAppRowsPerPage; // The row number in one page
    public int mTargetPage = 0; // The current target page
    ///M.

    public AllAppsRecyclerView(Context context) {
        this(context, null);
    }

    public AllAppsRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AllAppsRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AllAppsRecyclerView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr);

        Resources res = getResources();
        mScrollbar.setDetachThumbOnFastScroll();
        mEmptySearchBackgroundTopOffset = res.getDimensionPixelSize(
                R.dimen.all_apps_empty_search_bg_top_offset);

        /// M: [OP09_new] Edit and Hide app icons.@{
        mSupportEditAndHideApps = LauncherExtPlugin.getInstance().getWorkspaceExt(context)
                .supportEditAndHideApps();
        Resources resources = context.getResources();
        mCrosshairsDrawable = resources.getDrawable(R.drawable.ic_launcher_sort);
        sSharedOuterRingDrawable = resources.getDrawable(R.drawable.portal_ring_outer);
        sSharedInnerRingDrawable = resources.getDrawable(R.drawable.portal_ring_inner_nolip);
        //M:[OP09]}@
    }

    /**
     * Sets the list of apps in this view, used to determine the fastscroll position.
     */
    public void setApps(AlphabeticalAppsList apps) {
        mApps = apps;
    }

    /**
     * Sets the number of apps per row in this recycler view.
     */
    public void setNumAppsPerRow(DeviceProfile grid, int numAppsPerRow) {
        mNumAppsPerRow = numAppsPerRow;

        RecyclerView.RecycledViewPool pool = getRecycledViewPool();
        int approxRows = (int) Math.ceil(grid.availableHeightPx / grid.allAppsIconSizePx);
        pool.setMaxRecycledViews(AllAppsGridAdapter.EMPTY_SEARCH_VIEW_TYPE, 1);
        pool.setMaxRecycledViews(AllAppsGridAdapter.SEARCH_MARKET_DIVIDER_VIEW_TYPE, 1);
        pool.setMaxRecycledViews(AllAppsGridAdapter.SEARCH_MARKET_VIEW_TYPE, 1);
        pool.setMaxRecycledViews(AllAppsGridAdapter.ICON_VIEW_TYPE, approxRows * mNumAppsPerRow);
        pool.setMaxRecycledViews(AllAppsGridAdapter.PREDICTION_ICON_VIEW_TYPE, mNumAppsPerRow);
        pool.setMaxRecycledViews(AllAppsGridAdapter.SECTION_BREAK_VIEW_TYPE, approxRows);
        //OP09_new
        pool.setMaxRecycledViews(AllAppsGridAdapter.FOLDER_VIEW_TYPE, approxRows * mNumAppsPerRow);
 }

    /**
     * Scrolls this recycler view to the top.
     */
    public void scrollToTop() {
        // Ensure we reattach the scrollbar if it was previously detached while fast-scrolling
        if (mScrollbar.isThumbDetached()) {
            mScrollbar.reattachThumbToScroll();
        }
        scrollToPosition(0);
    }

    /**
     * We need to override the draw to ensure that we don't draw the overscroll effect beyond the
     * background bounds.
     */
    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.clipRect(mBackgroundPadding.left, mBackgroundPadding.top,
                getWidth() - mBackgroundPadding.right,
                getHeight() - mBackgroundPadding.bottom);

        /// M: [OP09] Edit and Hide app icons.@{
        if (Launcher.isInEditMode() && getChildCount() > 0) {
            int rowCount = mApps.getNumAppRows();

            /// M:Draw cross drawable when in edit mode.
            final int countX = mNumAppsPerRow;
            if (mCellSize[0] == 0 || mCellSize[1] == 0) {
                getCellSize();
            }

            final int countY = getHeight() / mCellSize[1];

            mNumAppRowsPerPage = countY;

            final Drawable d = mCrosshairsDrawable;
            final int crossWidth = d.getIntrinsicWidth();
            final int crossHeight = d.getIntrinsicHeight();
            final int measuredWidth = getMeasuredWidth();
            final int measuredHeight = getMeasuredHeight();
            final int colGap = mCellSize[0]; //(measuredWidth - crossWidth) / countX;
            final int rowGap = mCellSize[1]; //(measuredHeight - crossHeight) / countY;

            int x = 0;
            for (int col = 0; col <= countX; col++) {
                int y = getScrollY();
                for (int row = 0; row <= countY; row++) {
                    d.setBounds(x, y, x + crossWidth, y + crossHeight);
                    d.draw(canvas);
                    y += rowGap;
                }
                x += colGap;
            }

            if (mCrrentDragCell[0] != -1 && mCrrentDragCell[1] != -1 && mGrid != null) {
                //op09_new modify below part
                int previewOffset = FolderRingAnimator.sPreviewSize;
                for (int i = 0; i < mFolderOuterRings.size(); i++) {
                    FolderRingAnimator fra = mFolderOuterRings.get(i);
                    int[] tempLocation = new int[2];

                    Drawable drawable;
                    int width, height;
                    cellToPoint(fra.mCellX, fra.mCellY, tempLocation);
                    View child = getChildAt(fra.mCellX + fra.mCellY * mNumAppsPerRow);

                    if (child != null) {
                        int centerX = tempLocation[0] + mCellSize[0] / 2;
                        int centerY = tempLocation[1] + previewOffset / 2
                                + child.getPaddingTop() + mGrid.folderBackgroundOffset;

                        // Draw outer ring, if it exists
                        if (FolderIcon.HAS_OUTER_RING) {
                            drawable = FolderRingAnimator.sSharedOuterRingDrawable;
                            width = (int) (fra.getOuterRingSize() * 1);
                            height = width;
                            canvas.save();
                            canvas.translate(centerX - width / 2, centerY - height / 2);
                            drawable.setBounds(0, 0, width, height);
                            drawable.draw(canvas);
                            canvas.restore();
                        }

                        // Draw inner ring
                        drawable = FolderRingAnimator.sSharedInnerRingDrawable;
                        width = (int) (fra.getInnerRingSize() * 1);
                        height = width;
                        canvas.save();
                        canvas.translate(centerX - width / 2, centerY - width / 2);
                        drawable.setBounds(0, 0, width, height);
                        drawable.draw(canvas);
                        canvas.restore();
                    }
                }
                //op09_new modify
            }
        }
        //M: [OP09] }@

        super.dispatchDraw(canvas);
    }

    @Override
    public void onDraw(Canvas c) {
        // Draw the background
        if (mEmptySearchBackground != null && mEmptySearchBackground.getAlpha() > 0) {
            c.clipRect(mBackgroundPadding.left, mBackgroundPadding.top,
                    getWidth() - mBackgroundPadding.right,
                    getHeight() - mBackgroundPadding.bottom);

            mEmptySearchBackground.draw(c);
        }

        super.onDraw(c);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mEmptySearchBackground || super.verifyDrawable(who);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        updateEmptySearchBackgroundBounds();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // Bind event handlers
        addOnItemTouchListener(this);
    }

    @Override
    public void fillInLaunchSourceData(Bundle sourceData) {
        sourceData.putString(Stats.SOURCE_EXTRA_CONTAINER, Stats.CONTAINER_ALL_APPS);
        if (mApps.hasFilter()) {
            sourceData.putString(Stats.SOURCE_EXTRA_SUB_CONTAINER,
                    Stats.SUB_CONTAINER_ALL_APPS_SEARCH);
        } else {
            sourceData.putString(Stats.SOURCE_EXTRA_SUB_CONTAINER,
                    Stats.SUB_CONTAINER_ALL_APPS_A_Z);
        }
    }

    public void onSearchResultsChanged() {
        // Always scroll the view to the top so the user can see the changed results
        scrollToTop();

        if (mApps.hasNoFilteredResults()) {
            if (mEmptySearchBackground == null) {
                mEmptySearchBackground = new AllAppsBackgroundDrawable(getContext());
                mEmptySearchBackground.setAlpha(0);
                mEmptySearchBackground.setCallback(this);
                updateEmptySearchBackgroundBounds();
            }
            mEmptySearchBackground.animateBgAlpha(1f, 150);
        } else if (mEmptySearchBackground != null) {
            // For the time being, we just immediately hide the background to ensure that it does
            // not overlap with the results
            mEmptySearchBackground.setBgAlpha(0f);
        }
    }

    /**
     * Maps the touch (from 0..1) to the adapter position that should be visible.
     */
    @Override
    public String scrollToPositionAtProgress(float touchFraction) {
        int rowCount = mApps.getNumAppRows();
        if (rowCount == 0) {
            return "";
        }

        // Stop the scroller if it is scrolling
        stopScroll();

        // Find the fastscroll section that maps to this touch fraction
        List<AlphabeticalAppsList.FastScrollSectionInfo> fastScrollSections =
                mApps.getFastScrollerSections();
        AlphabeticalAppsList.FastScrollSectionInfo lastInfo = fastScrollSections.get(0);
        if (mScrollBarMode == FAST_SCROLL_BAR_MODE_DISTRIBUTE_BY_ROW) {
            for (int i = 1; i < fastScrollSections.size(); i++) {
                AlphabeticalAppsList.FastScrollSectionInfo info = fastScrollSections.get(i);
                if (info.touchFraction > touchFraction) {
                    break;
                }
                lastInfo = info;
            }
        } else if (mScrollBarMode == FAST_SCROLL_BAR_MODE_DISTRIBUTE_BY_SECTIONS) {
            lastInfo = fastScrollSections.get(
                (int) (touchFraction * (fastScrollSections.size() - 1)));
        } else {
            throw new RuntimeException("Unexpected scroll bar mode");
        }

        // Map the touch position back to the scroll of the recycler view
        getCurScrollState(mScrollPosState);
        int availableScrollHeight = getAvailableScrollHeight(rowCount, mScrollPosState.rowHeight);
        LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
        if (mFastScrollMode == FAST_SCROLL_MODE_FREE_SCROLL) {
            layoutManager.scrollToPositionWithOffset(0,
                (int) -(availableScrollHeight * touchFraction));
        }

        if (mPrevFastScrollFocusedPosition != lastInfo.fastScrollToItem.position) {
            mPrevFastScrollFocusedPosition = lastInfo.fastScrollToItem.position;

            // Reset the last focused view
            if (mLastFastScrollFocusedView != null) {
                mLastFastScrollFocusedView.setFastScrollFocused(false, true);
                mLastFastScrollFocusedView = null;
            }

            if (mFastScrollMode == FAST_SCROLL_MODE_JUMP_TO_FIRST_ICON) {
                smoothSnapToPosition(mPrevFastScrollFocusedPosition, mScrollPosState);
            } else if (mFastScrollMode == FAST_SCROLL_MODE_FREE_SCROLL) {
                final ViewHolder vh = findViewHolderForPosition(mPrevFastScrollFocusedPosition);
                if (vh != null &&
                        vh.itemView instanceof
                        BaseRecyclerViewFastScrollBar.FastScrollFocusableView) {
                    mLastFastScrollFocusedView =
                            (BaseRecyclerViewFastScrollBar.FastScrollFocusableView) vh.itemView;
                    mLastFastScrollFocusedView.setFastScrollFocused(true, true);
                }
            } else {
                throw new RuntimeException("Unexpected fast scroll mode");
            }
        }
        return lastInfo.sectionName;
    }

    @Override
    public void onFastScrollCompleted() {
        super.onFastScrollCompleted();
        // Reset and clean up the last focused view
        if (mLastFastScrollFocusedView != null) {
            mLastFastScrollFocusedView.setFastScrollFocused(false, true);
            mLastFastScrollFocusedView = null;
        }
        mPrevFastScrollFocusedPosition = -1;
    }

    /**
     * Updates the bounds for the scrollbar.
     */
    @Override
    public void onUpdateScrollbar(int dy) {
        List<AlphabeticalAppsList.AdapterItem> items = mApps.getAdapterItems();

        // Skip early if there are no items or we haven't been measured
        if (items.isEmpty() || mNumAppsPerRow == 0) {
                mScrollbar.setThumbOffset(-1, -1);
            return;
        }

        // Find the index and height of the first visible row (all rows have the same height)
        int rowCount = mApps.getNumAppRows();
        getCurScrollState(mScrollPosState);
        if (mScrollPosState.rowIndex < 0) {
            mScrollbar.setThumbOffset(-1, -1);
            return;
        }

        // Only show the scrollbar if there is height to be scrolled
        int availableScrollBarHeight = getAvailableScrollBarHeight();
        int availableScrollHeight = getAvailableScrollHeight(
             mApps.getNumAppRows(), mScrollPosState.rowHeight);
        if (availableScrollHeight <= 0) {
            mScrollbar.setThumbOffset(-1, -1);
            return;
        }

        // Calculate the current scroll position, the scrollY of the recycler view accounts for the
        // view padding, while the scrollBarY is drawn right up to the background padding (ignoring
        // padding)
        int scrollY = getPaddingTop() +
                (mScrollPosState.rowIndex * mScrollPosState.rowHeight) -
                mScrollPosState.rowTopOffset;
        int scrollBarY = mBackgroundPadding.top +
                (int) (((float) scrollY / availableScrollHeight) * availableScrollBarHeight);

        if (mScrollbar.isThumbDetached()) {
            int scrollBarX;
            if (Utilities.isRtl(getResources())) {
                scrollBarX = mBackgroundPadding.left;
            } else {
                scrollBarX = getWidth() - mBackgroundPadding.right - mScrollbar.getThumbWidth();
            }

            if (mScrollbar.isDraggingThumb()) {
                // If the thumb is detached, then just update the thumb to the current
                // touch position
                mScrollbar.setThumbOffset(scrollBarX, (int) mScrollbar.getLastTouchY());
            } else {
                int thumbScrollY = mScrollbar.getThumbOffset().y;
                int diffScrollY = scrollBarY - thumbScrollY;
                if (diffScrollY * dy > 0f) {
                    // User is scrolling in the same direction the thumb needs to catch up to the
                    // current scroll position.  We do this by mapping the difference in movement
                    // from the original scroll bar position to the difference in movement necessary
                    // in the detached thumb position to ensure that both speed towards the same
                    // position at either end of the list.
                    if (dy < 0) {
                        int offset = (int) ((dy * thumbScrollY) / (float) scrollBarY);
                        thumbScrollY += Math.max(offset, diffScrollY);
                    } else {
                        int offset = (int) ((dy * (availableScrollBarHeight - thumbScrollY)) /
                                (float) (availableScrollBarHeight - scrollBarY));
                        thumbScrollY += Math.min(offset, diffScrollY);
                    }
                    thumbScrollY = Math.max(0, Math.min(availableScrollBarHeight, thumbScrollY));
                    mScrollbar.setThumbOffset(scrollBarX, thumbScrollY);
                    if (scrollBarY == thumbScrollY) {
                        mScrollbar.reattachThumbToScroll();
                    }
                } else {
                    // User is scrolling in an opposite direction to the direction that the thumb
                    // needs to catch up to the scroll position.  Do nothing except for updating
                    // the scroll bar x to match the thumb width.
                    mScrollbar.setThumbOffset(scrollBarX, thumbScrollY);
                }
            }
        } else {
            synchronizeScrollBarThumbOffsetToViewScroll(mScrollPosState, rowCount);
        }
    }

    /**
     * This runnable runs a single frame of the smooth scroll animation and posts the next frame
     * if necessary.
     */
    @Thunk Runnable mSmoothSnapNextFrameRunnable = new Runnable() {
        @Override
        public void run() {
            if (mFastScrollFrameIndex < mFastScrollFrames.length) {
                scrollBy(0, mFastScrollFrames[mFastScrollFrameIndex]);
                mFastScrollFrameIndex++;
                postOnAnimation(mSmoothSnapNextFrameRunnable);
            } else {
                // Animation completed, set the fast scroll state on the target view
                final ViewHolder vh = findViewHolderForPosition(mPrevFastScrollFocusedPosition);
                if (vh != null &&
                        vh.itemView instanceof BaseRecyclerViewFastScrollBar.FastScrollFocusableView
                        && mLastFastScrollFocusedView != vh.itemView) {
                    mLastFastScrollFocusedView =
                            (BaseRecyclerViewFastScrollBar.FastScrollFocusableView) vh.itemView;
                    mLastFastScrollFocusedView.setFastScrollFocused(true, true);
                }
            }
        }
    };

    /**
     * Smoothly snaps to a given position.  We do this manually by calculating the keyframes
     * ourselves and animating the scroll on the recycler view.
     */
     //op09_new private -> public
    public void smoothSnapToPosition(final int position, ScrollPositionState scrollPosState) {
        removeCallbacks(mSmoothSnapNextFrameRunnable);

        // Calculate the full animation from the current scroll position to the final scroll
        // position, and then run the animation for the duration.
        int curScrollY = getPaddingTop() +
                (scrollPosState.rowIndex * scrollPosState.rowHeight) - scrollPosState.rowTopOffset;
        int newScrollY = getScrollAtPosition(position, scrollPosState.rowHeight);
        if (scrollPosState.rowIndex < 0 || scrollPosState.rowHeight <= 0) {
            LauncherLog.d(TAG, "smoothSnapToPosition: curScrollY=" + curScrollY
                + ", newScrollY=" + newScrollY
                + ", getPaddingTop=" + getPaddingTop()
                + ", rowIndex=" + scrollPosState.rowIndex
                + ", rowHeight=" + scrollPosState.rowHeight
                + ", rowTopOffset=" + scrollPosState.rowTopOffset
                + ", position=" + position);
        }
        int numFrames = mFastScrollFrames.length;

        for (int i = 0; i < numFrames; i++) {
            // TODO(winsonc): We can interpolate this as well.
            //op09_mew
            if (mSupportEditAndHideApps) {
                if (i == (numFrames - 1)) {
                    mFastScrollFrames[i] = (newScrollY - curScrollY)
                        - mFastScrollFrames[0] * (numFrames - 1);
                } else {
                    mFastScrollFrames[i] = (newScrollY - curScrollY) / numFrames;
                }
            } else {
                //op09_new
                mFastScrollFrames[i] = (newScrollY - curScrollY) / numFrames;
            }
        }
        mFastScrollFrameIndex = 0;
        postOnAnimation(mSmoothSnapNextFrameRunnable);
    }

    /**
     * Returns the current scroll state of the apps rows.
     */
    //op09_new private->public
    public void getCurScrollState(ScrollPositionState stateOut) {
        stateOut.rowIndex = -1;
        stateOut.rowTopOffset = -1;
        stateOut.rowHeight = -1;

        // Return early if there are no items or we haven't been measured
        List<AlphabeticalAppsList.AdapterItem> items = mApps.getAdapterItems();
        if (items.isEmpty() || mNumAppsPerRow == 0) {
            return;
        }

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            int position = getChildPosition(child);
            if (position != NO_POSITION) {
                AlphabeticalAppsList.AdapterItem item = items.get(position);
                if (item.viewType == AllAppsGridAdapter.ICON_VIEW_TYPE ||
                        item.viewType == AllAppsGridAdapter.PREDICTION_ICON_VIEW_TYPE
                        //op09_new
                        || item.viewType == AllAppsGridAdapter.FOLDER_VIEW_TYPE) {
                    stateOut.rowIndex = item.rowIndex;
                    stateOut.rowTopOffset = getLayoutManager().getDecoratedTop(child);
                    stateOut.rowHeight = child.getHeight();
                    break;
                }
            }
        }
    }

    /**
     * Returns the scrollY for the given position in the adapter.
     */
    //op09_new private
    public int getScrollAtPosition(int position, int rowHeight) {
        AlphabeticalAppsList.AdapterItem item = mApps.getAdapterItems().get(position);
        if (item.viewType == AllAppsGridAdapter.ICON_VIEW_TYPE ||
                item.viewType == AllAppsGridAdapter.PREDICTION_ICON_VIEW_TYPE
                //op09_new
                || item.viewType == AllAppsGridAdapter.FOLDER_VIEW_TYPE) {
            int offset = item.rowIndex > 0 ? getPaddingTop() : 0;
            if (mSupportEditAndHideApps && Launcher.isInEditMode()) {
                offset = 0;
            }
            return offset + item.rowIndex * rowHeight;
        } else {
            return 0;
        }
    }

    /**
     * Updates the bounds of the empty search background.
     */
    private void updateEmptySearchBackgroundBounds() {
        if (mEmptySearchBackground == null) {
            return;
        }

        // Center the empty search background on this new view bounds
        int x = (getMeasuredWidth() - mEmptySearchBackground.getIntrinsicWidth()) / 2;
        int y = mEmptySearchBackgroundTopOffset;
        mEmptySearchBackground.setBounds(x, y,
                x + mEmptySearchBackground.getIntrinsicWidth(),
                y + mEmptySearchBackground.getIntrinsicHeight());
    }

    ///op09_new
    public int[] getCellSize() {
        if (mCellSize[0] != 0 && mCellSize[1] != 0) {
            return mCellSize;
        }

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            int position = getChildPosition(child);
            if (position != NO_POSITION) {
                AlphabeticalAppsList.AdapterItem item =
                    mApps.getAdapterItems().get(position);
                if (item.viewType == AllAppsGridAdapter.ICON_VIEW_TYPE ||
                    item.viewType == AllAppsGridAdapter.PREDICTION_ICON_VIEW_TYPE
                    //op09_new
                    || item.viewType == AllAppsGridAdapter.FOLDER_VIEW_TYPE) {
                    mCellSize[0] = child.getWidth();
                    mCellSize[1] = child.getHeight();
                    break;
                }
            }
        }
        return mCellSize;
    }

    public void setDeviceProfile(DeviceProfile grid) {
        mGrid = grid;
    }

    public void setCrrentDragCell(int[] cell) {
        mCrrentDragCell[0] = cell[0];
        mCrrentDragCell[1] = cell[1];

    }
    public void showFolderAccept(FolderRingAnimator fra) {
        mFolderOuterRings.add(fra);
    }

    public void hideFolderAccept(FolderRingAnimator fra) {
        if (mFolderOuterRings.contains(fra)) {
            mFolderOuterRings.remove(fra);
        }
        invalidate();
    }

    public void clearDragOutlines() {
        //final int oldIndex = mDragOutlineCurrent;
        //mDragOutlineAnims[oldIndex].animateOut();
        //mDragCell[0] = mDragCell[1] = -1;
    }

    public void cellToPoint(int cellX, int cellY, int[] result) {
        final int hStartPadding = getPaddingLeft();
        final int vStartPadding = getPaddingTop();

        result[0] = hStartPadding + cellX * (mCellSize [0] + 0); //op09_new set mWidthGap is 0
        result[1] = vStartPadding + cellY * (mCellSize [1] + 0); //op09_new set mHeightGap is 0
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mLastTouchY = ev.getY();
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!mSupportEditAndHideApps) {
            return super.onTouchEvent(ev);
        }

        if (!Launcher.isInEditMode()) {
            return super.onTouchEvent(ev);
        }

        super.onTouchEvent(ev);

        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchY = ev.getY();

                break;
            case MotionEvent.ACTION_UP:
                stopScroll();
                getCurScrollState(mScrollPosState);

                float currentY = ev.getY();
                float thredhold =  mScrollPosState.rowHeight * 1.0f;
                int pageIndex = 0;

                if (mLastTouchY - currentY > thredhold) {
                    pageIndex = mScrollPosState.rowIndex / mNumAppRowsPerPage + 1;
                    if (pageIndex > mApps.getNumAppRows() / mNumAppRowsPerPage - 1) {
                        pageIndex = mApps.getNumAppRows() / mNumAppRowsPerPage - 1;
                    }
                } else if (currentY - mLastTouchY > thredhold) {
                    pageIndex = mScrollPosState.rowIndex / mNumAppRowsPerPage;
                    if (pageIndex < 0) {
                        pageIndex = 0;
                    }
                } else {
                    pageIndex = (mScrollPosState.rowIndex + 1) / mNumAppRowsPerPage;
                }
                mTargetPage = pageIndex;

                int pos = pageIndex * mNumAppsPerRow * mNumAppRowsPerPage + 1;
                LauncherLog.d(TAG, "onTouchEvent.ACTION_UP: pos=" + pos
                    + ", mTargetPage=" + mTargetPage
                    + ", mNumAppsPerRow=" + mNumAppsPerRow
                    + ", mNumAppRowsPerPage=" + mNumAppRowsPerPage);
                smoothSnapToPosition(pos, mScrollPosState);

                break;
        }
        return mScrollbar.isDraggingThumb();
    }

    //op09_new
}
