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

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.BitmapFactory;
import android.graphics.drawable.InsetDrawable;
import android.support.v7.widget.RecyclerView;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.android.launcher3.Alarm;
import com.android.launcher3.AppInfo;
import com.android.launcher3.BaseContainerView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DeleteDropTarget;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.DragSource;
import com.android.launcher3.DragScroller;
import com.android.launcher3.DragController;
import com.android.launcher3.DragView;
import com.android.launcher3.DropTarget;
import com.android.launcher3.Folder;
import com.android.launcher3.FolderIcon;
import com.android.launcher3.FolderIcon.FolderRingAnimator;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherExtPlugin;
import com.android.launcher3.LauncherModelPluginEx;
import com.android.launcher3.LauncherTransitionable;
import com.android.launcher3.OnAlarmListener;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.allapps.AlphabeticalAppsList.AdapterItem;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.Thunk;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.List;

import com.mediatek.launcher3.ext.AllApps;
import com.mediatek.launcher3.ext.LauncherLog;


/**
 * A merge algorithm that merges every section indiscriminately.
 */
final class FullMergeAlgorithm implements AlphabeticalAppsList.MergeAlgorithm {

    @Override
    public boolean continueMerging(AlphabeticalAppsList.SectionInfo section,
           AlphabeticalAppsList.SectionInfo withSection,
           int sectionAppCount, int numAppsPerRow, int mergeCount) {
        // Don't merge the predicted apps
        if (section.firstAppItem.viewType != AllAppsGridAdapter.ICON_VIEW_TYPE
            //op09_new
            && section.firstAppItem.viewType != AllAppsGridAdapter.FOLDER_VIEW_TYPE) {
            return false;
        }
        // Otherwise, merge every other section
        return true;
    }
}

/**
 * The logic we use to merge multiple sections.  We only merge sections when their final row
 * contains less than a certain number of icons, and stop at a specified max number of merges.
 * In addition, we will try and not merge sections that identify apps from different scripts.
 */
final class SimpleSectionMergeAlgorithm implements AlphabeticalAppsList.MergeAlgorithm {

    private int mMinAppsPerRow;
    private int mMinRowsInMergedSection;
    private int mMaxAllowableMerges;
    private CharsetEncoder mAsciiEncoder;

    public SimpleSectionMergeAlgorithm(int minAppsPerRow,
            int minRowsInMergedSection, int maxNumMerges) {
        mMinAppsPerRow = minAppsPerRow;
        mMinRowsInMergedSection = minRowsInMergedSection;
        mMaxAllowableMerges = maxNumMerges;
        mAsciiEncoder = Charset.forName("US-ASCII").newEncoder();
    }

    @Override
    public boolean continueMerging(AlphabeticalAppsList.SectionInfo section,
           AlphabeticalAppsList.SectionInfo withSection,
           int sectionAppCount, int numAppsPerRow, int mergeCount) {
        // Don't merge the predicted apps
        if (section.firstAppItem.viewType != AllAppsGridAdapter.ICON_VIEW_TYPE
            //op09_new
            && section.firstAppItem.viewType != AllAppsGridAdapter.FOLDER_VIEW_TYPE) {
            return false;
        }

        // Continue merging if the number of hanging apps on the final row is less than some
        // fixed number (ragged), the merged rows has yet to exceed some minimum row count,
        // and while the number of merged sections is less than some fixed number of merges
        int rows = sectionAppCount / numAppsPerRow;
        int cols = sectionAppCount % numAppsPerRow;

        // Ensure that we do not merge across scripts, currently we only allow for english and
        // native scripts so we can test if both can just be ascii encoded
        boolean isCrossScript = false;
        if (section.firstAppItem != null && withSection.firstAppItem != null) {
            isCrossScript = mAsciiEncoder.canEncode(section.firstAppItem.sectionName) !=
                    mAsciiEncoder.canEncode(withSection.firstAppItem.sectionName);
        }
        return (0 < cols && cols < mMinAppsPerRow) &&
                rows < mMinRowsInMergedSection &&
                mergeCount < mMaxAllowableMerges &&
                !isCrossScript;
    }
}

/**
 * The all apps view container.
 */
public class AllAppsContainerView extends BaseContainerView implements DragSource,
        LauncherTransitionable, View.OnTouchListener, View.OnLongClickListener,
        AllAppsSearchBarController.Callbacks,
        //below part for op09_new
        DragScroller, DropTarget, DragController.DragListener
        {

    private static final String TAG = "Launcher3.AllAppsContainerView";
    private static final int MIN_ROWS_IN_MERGED_SECTION_PHONE = 3;
    private static final int MAX_NUM_MERGES_PHONE = 2;

    @Thunk Launcher mLauncher;
    public AlphabeticalAppsList mApps;  //op09_new
    public AllAppsGridAdapter mAdapter; //op09_new private
    private RecyclerView.LayoutManager mLayoutManager;
    private RecyclerView.ItemDecoration mItemDecoration;

    @Thunk View mContent;
    @Thunk View mContainerView;
    @Thunk View mRevealView;
    @Thunk AllAppsRecyclerView mAppsRecyclerView;
    @Thunk AllAppsSearchBarController mSearchBarController;
    private ViewGroup mSearchBarContainerView;
    private View mSearchBarView;
    private SpannableStringBuilder mSearchQueryBuilder = null;

    private int mSectionNamesMargin;
    private int mNumAppsPerRow;
    private int mNumPredictedAppsPerRow;
    private int mRecyclerViewTopBottomPadding;
    // This coordinate is relative to this container view
    private final Point mBoundsCheckLastTouchDownPos = new Point(-1, -1);
    // This coordinate is relative to its parent
    private final Point mIconLastTouchPos = new Point();

    /// M: [OP09] @{
    private boolean mSupportEditAndHideApps = false;
    ///M.  edit mode:
    public boolean mIsInEditMode;
    private final int mTouchDelta = 8;
    private DragController mDragController;
    private View mDraggingView = null;
    private AppInfo mDraggingPaddingAppInfo = null;
    private int[] mCellSize = new int[2];
    private int[] mLastDragPos = new int[2];
    private int mScrollZone = 0;
    private boolean mDraggingInSrollZone = false;
    private float mMaxDistanceForFolderCreation;
    private int mDragMode = Workspace.DRAG_MODE_NONE;
    private int[] mTargetCell = new int[2];
    //private int mTargetPage = -1;
    private int mRealTargetPage = -1;
    private int mMaxAppsCountInOnePage = 0;
    ///M.}@

    private View.OnClickListener mSearchClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent searchIntent = (Intent) v.getTag();
            mLauncher.startActivitySafely(v, searchIntent, null);
        }
    };

    public AllAppsContainerView(Context context) {
        this(context, null);
    }

    public AllAppsContainerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AllAppsContainerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Resources res = context.getResources();

        mLauncher = (Launcher) context;
        mSectionNamesMargin = res.getDimensionPixelSize(R.dimen.all_apps_grid_view_start_margin);
        mApps = new AlphabeticalAppsList(context);
        mAdapter = new AllAppsGridAdapter(mLauncher, mApps, this, mLauncher, this);
        mApps.setAdapter(mAdapter);
        mLayoutManager = mAdapter.getLayoutManager();
        mItemDecoration = mAdapter.getItemDecoration();
        mRecyclerViewTopBottomPadding =
                res.getDimensionPixelSize(R.dimen.all_apps_list_top_bottom_padding);

        mSearchQueryBuilder = new SpannableStringBuilder();
        Selection.setSelection(mSearchQueryBuilder, 0);

        /// M: [OP09_new]Add for edit and hide apps for .
        mSupportEditAndHideApps = LauncherExtPlugin.getInstance().getWorkspaceExt(context)
                .supportEditAndHideApps();
        mDragController = mLauncher.getDragController();

        mScrollZone = mLauncher.getResources()
            .getDimensionPixelSize(R.dimen.scroll_zone);

        DeviceProfile grid = mLauncher.getDeviceProfile();
        mMaxDistanceForFolderCreation = (0.55f * grid.iconSizePx);

        ///M.
    }

    /**
     * Sets the current set of predicted apps.
     */
    public void setPredictedApps(List<ComponentKey> apps) {
        mApps.setPredictedApps(apps);
    }

    /**
     * Sets the current set of apps.
     */
    public void setApps(List<AppInfo> apps) {
        mApps.setApps(apps);
    }

    /**
     * Adds new apps to the list.
     */
    public void addApps(List<AppInfo> apps) {
        mApps.addApps(apps);
    }

    /**
     * Updates existing apps in the list
     */
    public void updateApps(List<AppInfo> apps) {
        mApps.updateApps(apps);
    }

    /**
     * Removes some apps from the list.
     */
    public void removeApps(List<AppInfo> apps) {
        mApps.removeApps(apps);
    }

    /**
     * Sets the search bar that shows above the a-z list.
     */
    public void setSearchBarController(AllAppsSearchBarController searchController) {
        if (mSearchBarController != null) {
            throw new RuntimeException("Expected search bar controller to only be set once");
        }
        mSearchBarController = searchController;
        mSearchBarController.initialize(mApps, this);

        // Add the new search view to the layout
        View searchBarView = searchController.getView(mSearchBarContainerView);
        mSearchBarContainerView.addView(searchBarView);
        mSearchBarContainerView.setVisibility(View.VISIBLE);
        mSearchBarView = searchBarView;
        setHasSearchBar();

        updateBackgroundAndPaddings();
    }

    /**
     * Scrolls this list view to the top.
     */
    public void scrollToTop() {
        mAppsRecyclerView.scrollToTop();
    }

    /**
     * Returns the content view used for the launcher transitions.
     */
    public View getContentView() {
        return mContainerView;
    }

    /**
     * Returns the all apps search view.
     */
    public View getSearchBarView() {
        return mSearchBarView;
    }

    /**
     * Returns the reveal view used for the launcher transitions.
     */
    public View getRevealView() {
        return mRevealView;
    }

    /**
     * Returns an new instance of the default app search controller.
     */
    public AllAppsSearchBarController newDefaultAppSearchController() {
        return new DefaultAppSearchController(getContext(), this, mAppsRecyclerView);
    }

    /**
     * Focuses the search field and begins an app search.
     */
    public void startAppsSearch() {
        if (mSearchBarController != null) {
            mSearchBarController.focusSearchField();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        boolean isRtl = Utilities.isRtl(getResources());
        mAdapter.setRtl(isRtl);
        mContent = findViewById(R.id.content);

        // This is a focus listener that proxies focus from a view into the list view.  This is to
        // work around the search box from getting first focus and showing the cursor.
        View.OnFocusChangeListener focusProxyListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mAppsRecyclerView.requestFocus();
                }
            }
        };
        mSearchBarContainerView = (ViewGroup) findViewById(R.id.search_box_container);
        mSearchBarContainerView.setOnFocusChangeListener(focusProxyListener);
        mContainerView = findViewById(R.id.all_apps_container);
        mContainerView.setOnFocusChangeListener(focusProxyListener);
        mRevealView = findViewById(R.id.all_apps_reveal);

        // Load the all apps recycler view
        mAppsRecyclerView = (AllAppsRecyclerView) findViewById(R.id.apps_list_view);
        mAppsRecyclerView.setApps(mApps);
        mAppsRecyclerView.setLayoutManager(mLayoutManager);
        mAppsRecyclerView.setAdapter(mAdapter);
        mAppsRecyclerView.setHasFixedSize(true);

        //op09_new
        DeviceProfile grid = mLauncher.getDeviceProfile();
        mAppsRecyclerView.setDeviceProfile(grid);

        if (mItemDecoration != null) {
            mAppsRecyclerView.addItemDecoration(mItemDecoration);
        }

        updateBackgroundAndPaddings();
    }

    @Override
    public void onBoundsChanged(Rect newBounds) {
        mLauncher.updateOverlayBounds(newBounds);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Update the number of items in the grid before we measure the view
        int availableWidth = !mContentBounds.isEmpty() ? mContentBounds.width() :
                MeasureSpec.getSize(widthMeasureSpec);
        DeviceProfile grid = mLauncher.getDeviceProfile();
        grid.updateAppsViewNumCols(getResources(), availableWidth);
        if (mNumAppsPerRow != grid.allAppsNumCols ||
                mNumPredictedAppsPerRow != grid.allAppsNumPredictiveCols) {
            mNumAppsPerRow = grid.allAppsNumCols;
            mNumPredictedAppsPerRow = grid.allAppsNumPredictiveCols;

            // If there is a start margin to draw section names, determine how we are going to merge
            // app sections
            boolean mergeSectionsFully = mSectionNamesMargin == 0 || !grid.isPhone;
            AlphabeticalAppsList.MergeAlgorithm mergeAlgorithm = mergeSectionsFully ?
                    new FullMergeAlgorithm() :
                    new SimpleSectionMergeAlgorithm((int) Math.ceil(mNumAppsPerRow / 2f),
                            MIN_ROWS_IN_MERGED_SECTION_PHONE, MAX_NUM_MERGES_PHONE);

            mAppsRecyclerView.setNumAppsPerRow(grid, mNumAppsPerRow);
            mAdapter.setNumAppsPerRow(mNumAppsPerRow);
            mApps.setNumAppsPerRow(mNumAppsPerRow, mNumPredictedAppsPerRow, mergeAlgorithm);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * Update the background and padding of the Apps view and children.  Instead of insetting the
     * container view, we inset the background and padding of the recycler view to allow for the
     * recycler view to handle touch events (for fast scrolling) all the way to the edge.
     */
    @Override
    protected void onUpdateBackgroundAndPaddings(Rect searchBarBounds, Rect padding) {
        boolean isRtl = Utilities.isRtl(getResources());

        // TODO: Use quantum_panel instead of quantum_panel_shape
        InsetDrawable background = new InsetDrawable(
                getResources().getDrawable(R.drawable.quantum_panel_shape), padding.left, 0,
                padding.right, 0);
        Rect bgPadding = new Rect();
        background.getPadding(bgPadding);
        mContainerView.setBackground(background);
        mRevealView.setBackground(background.getConstantState().newDrawable());
        mAppsRecyclerView.updateBackgroundPadding(bgPadding);
        mAdapter.updateBackgroundPadding(bgPadding);

        // Hack: We are going to let the recycler view take the full width, so reset the padding on
        // the container to zero after setting the background and apply the top-bottom padding to
        // the content view instead so that the launcher transition clips correctly.
        mContent.setPadding(0, padding.top, 0, padding.bottom);
        mContainerView.setPadding(0, 0, 0, 0);

        // Pad the recycler view by the background padding plus the start margin (for the section
        // names)
        int startInset = Math.max(mSectionNamesMargin, mAppsRecyclerView.getMaxScrollbarWidth());
        int topBottomPadding = mRecyclerViewTopBottomPadding;
        if (isRtl) {
            mAppsRecyclerView.setPadding(padding.left + mAppsRecyclerView.getMaxScrollbarWidth(),
                    topBottomPadding, padding.right + startInset, topBottomPadding);
        } else {
            mAppsRecyclerView.setPadding(padding.left + startInset, topBottomPadding,
                    padding.right + mAppsRecyclerView.getMaxScrollbarWidth(), topBottomPadding);
        }

        // Inset the search bar to fit its bounds above the container
        if (mSearchBarView != null) {
            Rect backgroundPadding = new Rect();
            if (mSearchBarView.getBackground() != null) {
                mSearchBarView.getBackground().getPadding(backgroundPadding);
            }
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)
                    mSearchBarContainerView.getLayoutParams();
            lp.leftMargin = searchBarBounds.left - backgroundPadding.left;
            lp.topMargin = searchBarBounds.top - backgroundPadding.top;
            lp.rightMargin = (getMeasuredWidth() - searchBarBounds.right) - backgroundPadding.right;
            mSearchBarContainerView.requestLayout();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Determine if the key event was actual text, if so, focus the search bar and then dispatch
        // the key normally so that it can process this key event
        if (!mSearchBarController.isSearchFieldFocused() &&
                event.getAction() == KeyEvent.ACTION_DOWN) {
            final int unicodeChar = event.getUnicodeChar();
            final boolean isKeyNotWhitespace = unicodeChar > 0 &&
                    !Character.isWhitespace(unicodeChar) && !Character.isSpaceChar(unicodeChar);
            if (isKeyNotWhitespace) {
                boolean gotKey = TextKeyListener.getInstance().onKeyDown(this, mSearchQueryBuilder,
                        event.getKeyCode(), event);
                if (gotKey && mSearchQueryBuilder.length() > 0) {
                    mSearchBarController.focusSearchField();
                }
            }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return handleTouchEvent(ev);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return handleTouchEvent(ev);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                mIconLastTouchPos.set((int) ev.getX(), (int) ev.getY());
                break;
        }
        return false;
    }

    @Override
    public boolean onLongClick(View v) {
        // Return early if this is not initiated from a touch
        if (!v.isInTouchMode()) return false;
        // When we have exited all apps or are in transition, disregard long clicks
        if (!mLauncher.isAppsViewVisible() ||
                mLauncher.getWorkspace().isSwitchingState()) return false;
        // Return if global dragging is not enabled
        if (!mLauncher.isDraggingEnabled()) return false;

        // Start the drag
        mLauncher.getWorkspace().beginDragShared(v, mIconLastTouchPos, this, false);

        ///M. ALPS02295402, add this for RtoL layout. in RtoL,
        ///enter spring loaded mode should later, or else, it has time sequence issue.
        if (!(mSupportEditAndHideApps && mIsInEditMode)) {
        if (Utilities.isRtl(getResources())) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Enter spring loaded mode
                    mLauncher.enterSpringLoadedDragMode();
                }
            }, 100);
        } else {
        ///M.
            mLauncher.enterSpringLoadedDragMode();
        }
        }
        return false;
    }

    @Override
    public boolean supportsFlingToDelete() {
        return true;
    }

    @Override
    public boolean supportsAppInfoDropTarget() {
        return true;
    }

    @Override
    public boolean supportsDeleteDropTarget() {
        return false;
    }

    @Override
    public float getIntrinsicIconScaleFactor() {
        DeviceProfile grid = mLauncher.getDeviceProfile();
        return (float) grid.allAppsIconSizePx / grid.iconSizePx;
    }

    @Override
    public void onFlingToDeleteCompleted() {
        // We just dismiss the drag when we fling, so cleanup here
        mLauncher.exitSpringLoadedDragModeDelayed(true,
                Launcher.EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT, null);
        mLauncher.unlockScreenOrientation(false);
    }

    @Override
    public void onDropCompleted(View target, DropTarget.DragObject d, boolean isFlingToDelete,
            boolean success) {
        if (isFlingToDelete || !success || (target != mLauncher.getWorkspace() &&
                !(target instanceof DeleteDropTarget) && !(target instanceof Folder))) {
            // Exit spring loaded mode if we have not successfully dropped or have not handled the
            // drop in Workspace

            ///M. ALPS02295402, add this for RtoL layout. in RtoL,
            ///the time should be longer, or else, it has time sequence issue.
            if (Utilities.isRtl(getResources())) {
                mLauncher.exitSpringLoadedDragModeDelayed(true,
                        Launcher.EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT_RTOL, null);
            } else {
                mLauncher.exitSpringLoadedDragModeDelayed(true,
                        Launcher.EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT, null);
            }
            ///M.
        }
        mLauncher.unlockScreenOrientation(false);

        // Display an error message if the drag failed due to there not being enough space on the
        // target layout we were dropping on.
        if (!success) {
            boolean showOutOfSpaceMessage = false;
            if (target instanceof Workspace) {
                int currentScreen = mLauncher.getCurrentWorkspaceScreen();
                Workspace workspace = (Workspace) target;
                CellLayout layout = (CellLayout) workspace.getChildAt(currentScreen);
                ItemInfo itemInfo = (ItemInfo) d.dragInfo;
                if (layout != null) {
                    showOutOfSpaceMessage =
                            !layout.findCellForSpan(null, itemInfo.spanX, itemInfo.spanY);
                }
            }
            if (showOutOfSpaceMessage) {
                mLauncher.showOutOfSpaceMessage(false);
            }

            d.deferDragViewCleanupPostAnimation = false;
        }
    }

    @Override
    public void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace) {
        // Do nothing
    }

    @Override
    public void onLauncherTransitionStart(Launcher l, boolean animated, boolean toWorkspace) {
        // Do nothing
    }

    @Override
    public void onLauncherTransitionStep(Launcher l, float t) {
        // Do nothing
    }

    @Override
    public void onLauncherTransitionEnd(Launcher l, boolean animated, boolean toWorkspace) {
        if (toWorkspace) {
            // Reset the search bar and base recycler view after transitioning home
            mSearchBarController.reset();
            mAppsRecyclerView.reset();
        } else {
        ///M: OP09
            if (mIsInEditMode) {
                enterEditMode();
                mAdapter.notifyDataSetChanged();
            }
        ///M.
        }
    }

    /**
     * Handles the touch events to dismiss all apps when clicking outside the bounds of the
     * recycler view.
     */
    private boolean handleTouchEvent(MotionEvent ev) {
        DeviceProfile grid = mLauncher.getDeviceProfile();
        int x = (int) ev.getX();
        int y = (int) ev.getY();

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mContentBounds.isEmpty()) {
                    // Outset the fixed bounds and check if the touch is outside all apps
                    Rect tmpRect = new Rect(mContentBounds);
                    tmpRect.inset(-grid.allAppsIconSizePx / 2, 0);
                    if (ev.getX() < tmpRect.left || ev.getX() > tmpRect.right) {
                        mBoundsCheckLastTouchDownPos.set(x, y);
                        return true;
                    }
                } else {
                    // Check if the touch is outside all apps
                    if (ev.getX() < getPaddingLeft() ||
                            ev.getX() > (getWidth() - getPaddingRight())) {
                        mBoundsCheckLastTouchDownPos.set(x, y);
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mBoundsCheckLastTouchDownPos.x > -1) {
                    ViewConfiguration viewConfig = ViewConfiguration.get(getContext());
                    float dx = ev.getX() - mBoundsCheckLastTouchDownPos.x;
                    float dy = ev.getY() - mBoundsCheckLastTouchDownPos.y;
                    float distance = (float) Math.hypot(dx, dy);
                    if (distance < viewConfig.getScaledTouchSlop()) {
                        // The background was clicked, so just go home
                        Launcher launcher = (Launcher) getContext();
                        launcher.showWorkspace(true);
                        return true;
                    }
                }
                // Fall through
            case MotionEvent.ACTION_CANCEL:
                mBoundsCheckLastTouchDownPos.set(-1, -1);
                break;
        }
        return false;
    }

    @Override
    public void onSearchResult(String query, ArrayList<ComponentKey> apps) {
        if (apps != null) {
            mApps.setOrderedFilter(apps);
            mAdapter.setLastSearchQuery(query);
            mAppsRecyclerView.onSearchResultsChanged();
        }
    }

    @Override
    public void clearSearchResult() {
        mApps.setOrderedFilter(null);
        mAppsRecyclerView.onSearchResultsChanged();

        // Clear the search query
        mSearchQueryBuilder.clear();
        mSearchQueryBuilder.clearSpans();
        Selection.setSelection(mSearchQueryBuilder, 0);
    }

    public void updateAppsUnreadChanged(ComponentName componentName, int unreadNum) {
        List<AdapterItem> mAdapterItems = mApps.getAdapterItems();
        final int size = mAdapterItems.size();
        AdapterItem adapterItem = null;
        for (int i = 0; i < size; i++) {
            adapterItem = mAdapterItems.get(i);
            if (adapterItem.appInfo != null && adapterItem.appInfo.intent != null
                    && adapterItem.appInfo.intent.getComponent().equals(componentName)) {
                adapterItem.appInfo.unreadNum = unreadNum;
            }
            if (adapterItem.folderInfo != null) {
                FolderInfo folderInfo = adapterItem.folderInfo;
                for (ShortcutInfo info : folderInfo.contents) {
                    if (info.intent != null
                        && info.intent.getComponent() != null
                        && info.intent.getComponent().equals(componentName)) {
                        folderInfo.unreadNum += unreadNum - info.unreadNum;
                        info.unreadNum = unreadNum;
                    }
                }
            }
        }
        // Refresh the recycler view
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

///M:[OP09_new] Support edit app. start @{
    /**
     * M: Enter edit mode, allow user to rearrange application icons.
     */
    public void enterEditMode() {
        //mView.invalidatePageData(mView.mCurrentPage);
        LauncherLog.d(TAG, "enterEditMode:");

        // Make apps customized pane can receive drag and drop event.
        mDragController.setDragScoller(this);
        mDragController.setMoveTarget(this);
        mDragController.addDropTarget(this);
        mDragController.addDragListener(this);

        if (mCellSize[0] == 0 || mCellSize[1] == 0) {
            mCellSize = mAppsRecyclerView.getCellSize();
        }

        if (mMaxAppsCountInOnePage == 0 && mAppsRecyclerView.getChildCount() > 0) {
            LauncherLog.d(TAG, "onDragEnter:mAppsRecyclerView.getHeight():"
                + mAppsRecyclerView.getHeight() + ", mCellSize[1]:" + mCellSize[1]
                + ",mNumAppsPerRow: " + mNumAppsPerRow);
            int num = mAppsRecyclerView.getHeight() / mCellSize[1];
            mMaxAppsCountInOnePage = mNumAppsPerRow * num;
            mApps.setMaxAppNumInPage(mMaxAppsCountInOnePage);
        }

        // Reset the target page to 0
        mAppsRecyclerView.mTargetPage = 0;

        mApps.refreshView(true);

        LauncherLog.d(TAG, "enterEditMode: mMaxAppsCountInOnePage="
            + mMaxAppsCountInOnePage);
    }

    /**
     * M: Exit edit mode.
     */
    public void exitEditMode() {
        mIsInEditMode = false;

        // Make apps customized pane can't receive drag and drop event when exit edit mode.
        mDragController.setDragScoller(mLauncher.getWorkspace());
        mDragController.setMoveTarget(mLauncher.getWorkspace());
        mDragController.removeDropTarget(this);
        mDragController.removeDragListener(this);

        //check folder is closed or not
        mLauncher.closeFolder();

        mApps.refreshView(true);
        LauncherLog.d(TAG, "exitEditMode:");
    }
    public void setEditModeFlag() {
        mIsInEditMode = true;
    }

    /**
     * M: Exit edit mode, add for OP09.
     */
    public void clearEditModeFlag() {
        mIsInEditMode = false;
    }

    public void scrollLeft() {

    }

    public void scrollRight() {

    }

    /**
     * The touch point has entered the scroll area; a scroll is imminent.
     * This event will only occur while a drag is active.
     *
     * @param direction The scroll direction
     */
    public boolean onEnterScrollArea(int x, int y, int direction) {
        return false;
    }

    /**
     * The touch point has left the scroll area.
     * NOTE: This may not be called, if a drop occurs inside the scroll area.
     */
    public boolean onExitScrollArea() {
        return false;
    }

   /**
     * Used to temporarily disable certain drop targets
     *
     * @return boolean specifying whether this drop target is currently enabled
     */
    public boolean isDropEnabled() {
        return mIsInEditMode;
    }


    /**
     * Handle an object being dropped on the DropTarget
     *
     * @param source DragSource where the drag started
     * @param x X coordinate of the drop location
     * @param y Y coordinate of the drop location
     * @param xOffset Horizontal offset with the object being dragged where the original
     *          touch happened
     * @param yOffset Vertical offset with the object being dragged where the original
     *          touch happened
     * @param dragView The DragView that's being dragged around on screen.
     * @param dragInfo Data associated with the object being dragged
     *
     */
    public void onDrop(DragObject d) {
        d.deferDragViewCleanupPostAnimation = false;

        ItemInfo dragInfo = (ItemInfo) (d.dragInfo);
        int bakupDragInfoPage = (int) dragInfo.screenId;
        int bakupDragInfoContainer = (int) dragInfo.container;

        ///drag a app from folder to app list.
        if (dragInfo instanceof ShortcutInfo) {
            AppInfo info =  ((ShortcutInfo) dragInfo).makeAppInfo();
            dragInfo = info;
            dragInfo.container = AppInfo.NO_ID;
            // should add it back.
            mApps.mItemsBackup.add(info);
        }

        //update the origin page info to data base.
        if (mDragMode == Workspace.DRAG_MODE_REORDER) {
            int index = mApps.mItems.indexOf(mDraggingPaddingAppInfo);
            LauncherLog.d(TAG, "onDrop,index:" + index + ",mDraggingPaddingAppInfo:"
                + mDraggingPaddingAppInfo);
            dragInfo.screenId = mDraggingPaddingAppInfo.screenId;
            dragInfo.mPos = mDraggingPaddingAppInfo.mPos;
            dragInfo.cellX = mDraggingPaddingAppInfo.cellX;

            if (index < 0) {
                if (dragInfo instanceof AppInfo) {
                    mApps.mItems.add((AppInfo) dragInfo);
                } else if (dragInfo instanceof FolderInfo) {
                    mApps.mItems.add((FolderInfo) dragInfo);
                }
            } else {
                if (dragInfo instanceof AppInfo) {
                    mApps.mItems.set(index, (AppInfo) dragInfo);
                } else if (dragInfo instanceof FolderInfo) {
                    mApps.mItems.set(index, (FolderInfo) dragInfo);
                }
            }
        } else if (mDragMode == Workspace.DRAG_MODE_CREATE_FOLDER) {
            createUserFolderIfNecessary(d.dragView, (AppInfo) dragInfo);

        } else if (mDragMode == Workspace.DRAG_MODE_ADD_TO_FOLDER) {
            int pageId = mAppsRecyclerView.mTargetPage <= 0
                            ? 0 : mAppsRecyclerView.mTargetPage;
            int pos = mAppsRecyclerView.mTargetPage * mMaxAppsCountInOnePage
                        + mTargetCell[1] * mNumAppsPerRow + mTargetCell[0];

            FolderInfo folderInfo = null;
            if (pos < mApps.mItems.size()) {
                ItemInfo itemInfo = mApps.mItems.get(pos);
                folderInfo = (itemInfo instanceof FolderInfo) ? (FolderInfo) itemInfo : null;
            }

            dragInfo.cellX = mTargetCell[0];
            dragInfo.cellY = mTargetCell[1];
            dragInfo.screenId = pageId;
            dragInfo.mPos = folderInfo.mPos;
            dragInfo.container = folderInfo.id;
            ShortcutInfo dragTag = ((AppInfo) dragInfo).makeShortcut();
            folderInfo.add(dragTag);

            mApps.mItems.remove(dragInfo);
            mApps.mItemsBackup.remove(dragInfo);

            //update the origin page info to data base.
            LauncherModelPluginEx.moveAllAppsItemInDatabase(mLauncher, dragInfo,
                folderInfo.id, dragInfo.screenId, mTargetCell[0], mTargetCell[1]);

        }

        mApps.mItems.remove(mDraggingPaddingAppInfo);
        mApps.updateAllItemOrderByPos(mApps.mItems);

        int tempCell[] = {-1, -1};
        mAppsRecyclerView.setCrrentDragCell(tempCell);

        mApps.refreshView(true);

        for (ItemInfo info : mApps.mItems) {
            mApps.updateItemInDatabase(info);
        }

        setDragMode(Workspace.DRAG_MODE_NONE);
    }

    public void onDragEnter(DragObject d) {
        if (mSupportEditAndHideApps && mIsInEditMode) {
            //if it in search mode, clear it.
            if (mSearchBarController.isSearchFieldFocused()) {
                mSearchBarController.reset();
            }
            ItemInfo info = (ItemInfo) (d.dragInfo);

            LauncherLog.d(TAG, "onDragEnter: targetPage="
                + mAppsRecyclerView.mTargetPage
                + ", mMaxAppsCountInOnePage=" + mMaxAppsCountInOnePage);

            if (mDraggingPaddingAppInfo == null) {
                LauncherLog.d(TAG, "onDragEnter: mDraggingPaddingAppInfo is null");
                if (info instanceof FolderInfo) {
                    AppInfo tempinfo = ((FolderInfo) info).contents.get(0).makeAppInfo();
                    mDraggingPaddingAppInfo = new AppInfo(tempinfo);
                } else if (info instanceof ShortcutInfo) {
                    AppInfo tempinfo = ((ShortcutInfo) info).makeAppInfo();
                    mDraggingPaddingAppInfo = new AppInfo(tempinfo);
                } else {
                    mDraggingPaddingAppInfo = new AppInfo((AppInfo) info);
                }
                mDraggingPaddingAppInfo.isForPadding = AppInfo.DRAGGING_PADDING_APP;
                mDraggingPaddingAppInfo.isVisible = true;
                mDraggingPaddingAppInfo.title = "DraggingPadding";
                ComponentName cn = new ComponentName("com.op09.launcher",
                    "com.op09.draggingPadding");
                mDraggingPaddingAppInfo.intent.setComponent(cn);
                mDraggingPaddingAppInfo.componentName = cn;
                mDraggingPaddingAppInfo.iconBitmap = BitmapFactory.decodeResource(
                    mLauncher.getResources(), R.drawable.ic_launcher_edit_holo); //padding_app_icon
                mDraggingPaddingAppInfo.unreadNum = 0;
            }

            mDraggingPaddingAppInfo.mPos = info.mPos;
            mDraggingPaddingAppInfo.screenId =  info.screenId;
            mDraggingPaddingAppInfo.cellX = info.cellX;
            mDraggingPaddingAppInfo.cellY = info.cellY;


            if (info instanceof AppInfo) {
                int index = mApps.mItems.indexOf(info);
                if (index == -1) {
                    mApps.mItems.remove(mDraggingPaddingAppInfo);
                    mApps.mItems.add(mDraggingPaddingAppInfo);
                } else {
                    mApps.mItems.set(index, mDraggingPaddingAppInfo);
                }
                LauncherLog.d(TAG, "onDragEnter_App,mDraggingPaddingAppInfo:"
                    + mDraggingPaddingAppInfo + ",index:" + index);
            } else if (info instanceof FolderInfo) {
                int index = mApps.mItems.indexOf(info);
                mApps.mItems.set(index, mDraggingPaddingAppInfo);
                LauncherLog.d(TAG, "onDragEnter_folder,mDraggingPaddingAppInfo:"
                    + mDraggingPaddingAppInfo + ",index:" + index);
            } else if (info instanceof ShortcutInfo) {
                ///drag a app from folder to app list.
                boolean find = false;
                for (ItemInfo itemInfo : mApps.mItems) {
                    if (itemInfo instanceof FolderInfo &&
                            (((FolderInfo) itemInfo).id == info.container)) {
                        FolderInfo folderInfo = (FolderInfo) itemInfo;
                        folderInfo.remove((ShortcutInfo) info);
                        break;
                    }
                }
                LauncherLog.d(TAG, "onDragEnter_app in folder,mDraggingPaddingAppInfo:"
                    + mDraggingPaddingAppInfo);
            }

            mApps.refreshView(false);

            mLastDragPos[0] = -1;
            mLastDragPos[1] = -1;

            setDragMode(Workspace.DRAG_MODE_REORDER);
            mDraggingInSrollZone = false;
        }

    }

    public void onDragOver(DragObject d) {
        ItemInfo item = (ItemInfo) d.dragInfo;

        ///drag a app from folder to app list.
        if (item instanceof ShortcutInfo) {
            AppInfo info =  ((ShortcutInfo) item).makeAppInfo();
            item = info;
        }

        float[] r = getDragViewVisualCenter(d.x, d.y, d.xOffset, d.yOffset, d.dragView, null);

        ///map the view center location to recycler view.
        int[] location = new int[2];
        mAppsRecyclerView.getLocationInWindow(location);
        r[1] = r[1] - location[1];

        AllAppsRecyclerView.ScrollPositionState scrollPosState =
            new AllAppsRecyclerView.ScrollPositionState();

        mAppsRecyclerView.getCurScrollState(scrollPosState);

        //check srcoll area
        if (r[1] < mScrollZone && !mDraggingInSrollZone) {
            //mApps.mItems.remove(mDraggingPaddingAppInfo);

            if (scrollPosState.rowIndex < 0 || scrollPosState.rowHeight <= 0) {
                LauncherLog.d(TAG, "onDragOver scroll up: invalid scrollPosState"
                    + ", rowIndex=" + scrollPosState.rowIndex
                    + ", rowHeight=" + scrollPosState.rowHeight);
                return;
            }

            mAppsRecyclerView.mTargetPage = mAppsRecyclerView.mTargetPage <= 0
                ? 0 : mAppsRecyclerView.mTargetPage - 1;
            LauncherLog.d(TAG, "onDragOver scroll up: targetPage="
                + mAppsRecyclerView.mTargetPage);

            mAppsRecyclerView.smoothSnapToPosition(
                mAppsRecyclerView.mTargetPage * mMaxAppsCountInOnePage + 1,
                scrollPosState);
            mDraggingInSrollZone = true;
            setDragMode(Workspace.DRAG_MODE_NONE);
            return;
        } else if ((r[1] > mAppsRecyclerView.getHeight() - mScrollZone)
                && !mDraggingInSrollZone) {
            //remove padding app info when scroll to next page.
            //mApps.mItems.remove(mDraggingPaddingAppInfo);

            if (scrollPosState.rowIndex < 0 || scrollPosState.rowHeight <= 0) {
                LauncherLog.d(TAG, "onDragOver scroll down: invalid scrollPosState"
                    + ", rowIndex=" + scrollPosState.rowIndex
                    + ", rowHeight=" + scrollPosState.rowHeight);
                return;
            }
            int pageNum = (mApps.mItems.size() + mMaxAppsCountInOnePage - 1)
                    / mMaxAppsCountInOnePage;
            mAppsRecyclerView.mTargetPage = (mAppsRecyclerView.mTargetPage >= pageNum - 1)
                ? (pageNum - 1) : mAppsRecyclerView.mTargetPage + 1;
            LauncherLog.d(TAG, "onDragOver scroll down: targetPage="
                + mAppsRecyclerView.mTargetPage);

            mAppsRecyclerView.smoothSnapToPosition(
                mAppsRecyclerView.mTargetPage * mMaxAppsCountInOnePage + 1,
                scrollPosState);
            mDraggingInSrollZone = true;
            setDragMode(Workspace.DRAG_MODE_NONE);
            return;
        } else {
            if ((r[1] >= mScrollZone) &&
                (r[1] <= mAppsRecyclerView.getHeight() - mScrollZone)) {
                mDraggingInSrollZone = false;
            } else {
                return;
            }
        }

        r[0] = r[0] < 0 ? 0 : r[0];
        r[1] = r[1] < 0 ? 0 : r[1];

        int cellX = (int) (r[0] / mCellSize[0]);
        int cellY = (int) (r[1] / mCellSize[1]);

        if (mLastDragPos[0] == -1 &&
                mLastDragPos[1] == -1) {
            mLastDragPos[0] = cellX;
            mLastDragPos[1] = cellY;
            return;
        }

        mApps.mItems.remove(mDraggingPaddingAppInfo);

        mTargetCell[0] = cellX;
        mTargetCell[1] = cellY;
        float dis = getDistanceFromCell(r[0], r[1], mTargetCell);
        int pos = mAppsRecyclerView.mTargetPage * mMaxAppsCountInOnePage
                + cellY * mNumAppsPerRow + cellX;

        //op09_modif
        ItemInfo tempInfo = null;
        if (pos < mApps.mItems.size()) {
            tempInfo = mApps.mItems.get(pos);
        }

        if ((item instanceof AppInfo) && (pos < mApps.mItems.size())
            && willCreateUserFolder(tempInfo, dis)) {

            mAppsRecyclerView.setCrrentDragCell(mTargetCell);
            mApps.refreshView(false);

            mLastDragPos[0] = -1;
            mLastDragPos[1] = -1;

            if (tempInfo instanceof AppInfo) {
                setDragMode(Workspace.DRAG_MODE_CREATE_FOLDER);
                if (!mFolderCreationAlarm.alarmPending()) {
                    mFolderCreationAlarm.setOnAlarmListener(new
                            FolderCreationAlarmListener(mAppsRecyclerView,
                            mTargetCell[0], mTargetCell[1]));
                    mFolderCreationAlarm.setAlarm(Workspace.FOLDER_CREATION_TIMEOUT);
                    return;
                }

            } else if (tempInfo instanceof FolderInfo) {
                if (willAddToExistingUserFolder(d.dragInfo, mAppsRecyclerView,
                        mTargetCell, dis)) {

                    View dropOverView = null;
                    pos = mAppsRecyclerView.mTargetPage * mMaxAppsCountInOnePage
                            + mTargetCell[1] * mNumAppsPerRow + mTargetCell[0];

                    if (pos < mAppsRecyclerView.getChildCount()) {
                        dropOverView = mAppsRecyclerView.getChildAt(pos + 1);
                    }

                    mDragOverFolderIcon = ((FolderIcon) dropOverView);
                    mDragOverFolderIcon.onDragEnter(item);
                }
                setDragMode(Workspace.DRAG_MODE_ADD_TO_FOLDER);
            }
            return;
        }
        setDragMode(Workspace.DRAG_MODE_REORDER);

        int tempCell[] = {-1, -1};
        mAppsRecyclerView.setCrrentDragCell(tempCell);

        mAppsRecyclerView.getCurScrollState(scrollPosState);

        //op09_modif
        pos = mAppsRecyclerView.mTargetPage * mMaxAppsCountInOnePage
                + cellY * mNumAppsPerRow + cellX;
        //mDraggingPaddingAppInfo.cellX = pos;
        if (pos >= mApps.mItems.size()) {
            mApps.mItems.add(mDraggingPaddingAppInfo);
        } else {
            mApps.mItems.add(pos, mDraggingPaddingAppInfo);
        }

        mApps.updateAllItemOrderByPos(mApps.mItems);

        mApps.refreshView(false);

    }

    public void onDragExit(DragObject d) {
        if (mDragOverFolderIcon != null) {
            mDragOverFolderIcon.onDragExit();
        }

        int tempCell[] = {-1, -1};
        mAppsRecyclerView.setCrrentDragCell(tempCell);
    }

    /**
     * Handle an object being dropped as a result of flinging to delete and will be called in place
     * of onDrop().  (This is only called on objects that are set as the DragController's
     * fling-to-delete target.
     */
    public void onFlingToDelete(DragObject dragObject, PointF vec) {

    }

    /**
     * Check if a drop action can occur at, or near, the requested location.
     * This will be called just before onDrop.
     *
     * @param source DragSource where the drag started
     * @param x X coordinate of the drop location
     * @param y Y coordinate of the drop location
     * @param xOffset Horizontal offset with the object being dragged where the
     *            original touch happened
     * @param yOffset Vertical offset with the object being dragged where the
     *            original touch happened
     * @param dragView The DragView that's being dragged around on screen.
     * @param dragInfo Data associated with the object being dragged
     * @return True if the drop will be accepted, false otherwise.
     */
    public boolean acceptDrop(DragObject dragObject) {
        return true;
    }

    public void prepareAccessibilityDrop() {

    }

    // These methods are implemented in Views
    public void getHitRectRelativeToDragLayer(Rect outRect) {
        mLauncher.getDragLayer().getDescendantRectRelativeToSelf(this, outRect);
    }

    public void getLocationInDragLayer(int[] loc) {

    }

     /**
         * A drag has begun
         *
         * @param source An object representing where the drag originated
         * @param info The data associated with the object that is being dragged
         * @param dragAction The drag action: either {@link DragController#DRAG_ACTION_MOVE}
         *        or {@link DragController#DRAG_ACTION_COPY}
         */
     public void onDragStart(DragSource source, Object info, int dragAction) {

     }

     /**
         * The drag has ended
         */
      public void onDragEnd() {
        //op09_new if there is only one item in folder,
        //remove the folder,and put the item in list directly.
         for (ItemInfo itemInfo : mApps.mItemsBackup) {
            if (itemInfo instanceof FolderInfo) {
                if (((FolderInfo) itemInfo).contents.size() == 1) {
                    AppInfo appInfo = ((FolderInfo) itemInfo).contents
                            .get(0).makeAppInfo();
                    appInfo.container = AllApps.CONTAINER_ALLAPP;
                    appInfo.screenId = 0;
                    appInfo.cellX = itemInfo.cellX;
                    appInfo.cellY = itemInfo.cellY;

                    int index = mApps.mItemsBackup.indexOf(itemInfo);
                    mApps.mItemsBackup.set(index, appInfo);

                    index = mApps.mItems.indexOf(itemInfo);
                    mApps.mItems.set(index, appInfo);

                    mApps.updateItemInDatabase(appInfo);
                    mApps.deleteItemInDatabase(itemInfo);
                    break;
                }
            }
        }
        mApps.refreshView(false);
     }

     /**
     * M: This is used to compute the visual center of the dragView. This point
     * is then used to visualize drop locations and determine where to drop an
     * item. The idea is that the visual center represents the user's
     * interpretation of where the item is, and hence is the appropriate point
     * to use when determining drop location, merge from Workspace.
     *
     * @param x
     * @param y
     * @param xOffset
     * @param yOffset
     * @param dragView
     * @param recycle
     * @return
     */
    private float[] getDragViewVisualCenter(int x, int y, int xOffset, int yOffset,
            DragView dragView, float[] recycle) {
        // NOTICE: Launcher3 add the scrollX to the x param in
        // DragController(Launcher2 don't), we need to minus the mScrollX when
        // calculating the visual center in Launcher3.
        float res[];
        if (recycle == null) {
            res = new float[2];
        } else {
            res = recycle;
        }

        // First off, the drag view has been shifted in a way that is not
        // represented in the x and y values or the x/yOffsets. Here we account
        // for that shift.
        x += 0; //getResources().getDimensionPixelSize(0);
        y += 0; //getResources().getDimensionPixelSize(0);

        // These represent the visual top and left of drag view if a dragRect
        // was provided.
        // If a dragRect was not provided, then they correspond to the actual
        // view left and top, as the dragRect is in that case taken to be the entire dragView.
        // R.dimen.dragViewOffsetY.
        int left = x - xOffset - mNumAppsPerRow; //mScrollX;
        int top = y - yOffset;

        // In order to find the visual center, we shift by half the dragRect
        res[0] = left + dragView.getDragRegion().width() / 2;
        res[1] = top + dragView.getDragRegion().height() / 2;

        return res;
    }


    public float getDistanceFromCell(float x, float y, int[] cell) {
        //cellToCenterPoint(cell[0], cell[1], mTmpPoint);

        int[] cellCenterPoint = new int[2];
        if (mCellSize[0] == 0 || mCellSize[1] == 0) {
            mCellSize = mAppsRecyclerView.getCellSize();
        }

        cellCenterPoint[0] = cell[0] * mCellSize[0] + mCellSize[0] / 2;
        cellCenterPoint[1] = cell[1] * mCellSize[1] + mCellSize[1] / 2;

        float distance = (float) Math.sqrt(Math.pow(x - cellCenterPoint[0], 2) +
             Math.pow(y - cellCenterPoint[1], 2));
        return distance;
    }


    protected boolean manageFolderFeedback(ItemInfo info, /*CellLayout targetLayout,*/
            int[] targetCell, float distance, int pos /*View dragOverView*/) {
         boolean userFolderPending = willCreateUserFolder(info, distance);
         if (userFolderPending) {
            mFolderCreationAlarm.setOnAlarmListener(new
                    FolderCreationAlarmListener(null, targetCell[0], targetCell[1]));
            mFolderCreationAlarm.setAlarm(0);
            return true;
        }
         return false;
    }

    boolean willCreateUserFolder(ItemInfo info, float distance) {
        LauncherLog.d(TAG, "willCreateUserFolder:distance:" + distance
            + ", mMaxDistanceForFolderCreation: " + mMaxDistanceForFolderCreation);
        if (info == null) {
            return false;
        }

        if (distance > mMaxDistanceForFolderCreation) {
            return false;
        }
        if ((info instanceof AppInfo) &&
            (((AppInfo) info).isForPadding != AppInfo.NOT_PADDING_APP)) {
            return false;
        }
        return true;
    }

    boolean willAddToExistingUserFolder(Object dragInfo, AllAppsRecyclerView target,
            int[] targetCell, float distance) {
        if (distance > mMaxDistanceForFolderCreation) {
            return false;
        }
        //View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);
        View dropOverView = null;

        int pos = mAppsRecyclerView.mTargetPage * mMaxAppsCountInOnePage
                + targetCell[1] * mNumAppsPerRow + targetCell[0];

        if (pos < mAppsRecyclerView.getChildCount()) {
            dropOverView = mAppsRecyclerView.getChildAt(pos + 1);
        }

        if (dropOverView instanceof FolderIcon) {
            FolderIcon fi = (FolderIcon) dropOverView;
            if (fi.acceptDrop(dragInfo)) {
                return true;
            }
        }
        return false;
    }


    void setDragMode(int dragMode) {
        if (dragMode != mDragMode) {
            if (dragMode == Workspace.DRAG_MODE_NONE) {
                //cleanupAddToFolder();
                // We don't want to cancel the re-order alarm every time the target cell changes
                // as this feels to slow / unresponsive.
                //cleanupReorder(false);
                cleanupFolderCreation();
            } else if (dragMode == Workspace.DRAG_MODE_ADD_TO_FOLDER) {
               // cleanupReorder(true);
               cleanupFolderCreation();
            } else if (dragMode == Workspace.DRAG_MODE_CREATE_FOLDER) {
                //cleanupAddToFolder();
                //cleanupReorder(true);
            } else if (dragMode == Workspace.DRAG_MODE_REORDER) {
                //cleanupAddToFolder();
                cleanupFolderCreation();
            }
            mDragMode = dragMode;
        }
    }

    public void setItems(ArrayList<ItemInfo> allItems) {
        //op09_new
        if (allItems.size() > 0) {
            ItemInfo info = allItems.get(0);
            if (info instanceof FolderInfo) {
                AppInfo tempinfo = ((FolderInfo) info).contents.get(0).makeAppInfo();
                mDraggingPaddingAppInfo = new AppInfo(tempinfo);
            } else {
                mDraggingPaddingAppInfo = new AppInfo((AppInfo) info);
            }
            mDraggingPaddingAppInfo.isForPadding = AppInfo.DRAGGING_PADDING_APP;
            mDraggingPaddingAppInfo.isVisible = true;
            mDraggingPaddingAppInfo.title = "DraggingPadding";
            ComponentName cn = new ComponentName("com.op09.launcher",
                "com.op09.draggingPadding");
            mDraggingPaddingAppInfo.intent.setComponent(cn);
            mDraggingPaddingAppInfo.componentName = cn;
            mDraggingPaddingAppInfo.iconBitmap = BitmapFactory.decodeResource(
                mLauncher.getResources(), R.drawable.ic_launcher_edit_holo); //padding_app_icon
            mDraggingPaddingAppInfo.unreadNum = 0;
        }
        mApps.setItems(allItems);
    }

    boolean createUserFolderIfNecessary(/*View newView, CellLayout target,
        int[] targetCell, float distance, boolean external, DragView dragView,
        Runnable postAnimationRunnable*/DragView dragView, AppInfo dragInfo) {

        AllAppsRecyclerView.ScrollPositionState scrollPosState =
            new AllAppsRecyclerView.ScrollPositionState();

        mAppsRecyclerView.stopScroll();
        mAppsRecyclerView.getCurScrollState(scrollPosState);
        if (scrollPosState.rowIndex <= -1) {
            scrollPosState.rowIndex = 0;
        }
        int pageId = mAppsRecyclerView.mTargetPage <= 0
                        ? 0 : mAppsRecyclerView.mTargetPage;
        int pos = mAppsRecyclerView.mTargetPage * mMaxAppsCountInOnePage
                    + mTargetCell[1] * mNumAppsPerRow + mTargetCell[0];
        LauncherLog.d(TAG, "createUserFolderIfNecessary: pos:"
            +  pos + ",mTargetCell[1]:" + mTargetCell[1]
            + ",mTargetCell[0]:" + mTargetCell[0]);

        final FolderInfo folderInfo = new FolderInfo();
        folderInfo.title = mLauncher.getResources().getText(R.string.folder_name);
        folderInfo.cellX = pos;
        folderInfo.cellY = 0;
        folderInfo.screenId = 0;
        folderInfo.mPos = pos;

        LauncherModelPluginEx.addFolderItemToDatabase(
                mLauncher, folderInfo, 0, folderInfo.cellX, 0, false);

        dragInfo.cellX = 1;
        dragInfo.cellY = 0;
        dragInfo.screenId = pageId;
        dragInfo.mPos = folderInfo.mPos;
        dragInfo.container = folderInfo.id;
        ShortcutInfo dragTag = dragInfo.makeShortcut();

        mApps.mItems.remove(dragInfo);
        mApps.mItemsBackup.remove(dragInfo);

        LauncherModelPluginEx.moveAllAppsItemInDatabase(mLauncher, dragInfo,
            dragInfo.container, dragInfo.screenId, dragInfo.cellX, dragInfo.cellY);

        //destapp.
        AppInfo destAppInfo = (AppInfo) mApps.mItems.get(pos);
        destAppInfo.cellX = 0;
        destAppInfo.cellY = 0;
        destAppInfo.container = folderInfo.id;
        ShortcutInfo destTag = destAppInfo.makeShortcut();

        LauncherModelPluginEx.moveAllAppsItemInDatabase(mLauncher, destAppInfo,
            destAppInfo.container, destAppInfo.screenId, destAppInfo.cellX, destAppInfo.cellY);

        folderInfo.add(dragTag);
        folderInfo.add(destTag);

        int index  = mApps.mItems.indexOf(destAppInfo);
        mApps.mItems.set(index, folderInfo);
        index = mApps.mItemsBackup.indexOf(destAppInfo);
        if (index >= 0 && index < mApps.mItemsBackup.size()) {
            mApps.mItemsBackup.set(index, folderInfo);
        } else {
            mApps.mItemsBackup.add(folderInfo);
        }

        return true;
    }

    private void cleanupFolderCreation() {
        if (mDragFolderRingAnimator != null) {
            mDragFolderRingAnimator.animateToNaturalState();
            mDragFolderRingAnimator = null;
        }
        mFolderCreationAlarm.setOnAlarmListener(null);
        mFolderCreationAlarm.cancelAlarm();
    }

    private final Alarm mFolderCreationAlarm = new Alarm();
    private FolderRingAnimator mDragFolderRingAnimator = null;
    private FolderIcon mDragOverFolderIcon = null;

    /**
      *M: create alarm listener.
      */
    public class FolderCreationAlarmListener implements OnAlarmListener {
        //op09_new modify celllayout to recyclerView
        //CellLayout mLayout;
        RecyclerView mRecyclerView;
        int mCellX;
        int mCellY;

        /**
        *M: create alarm listener.
        * @param layout which layout to create folder
        * @param cellX the x position
        * @param cellY the y position
        */
        //op09_new modify celllayout to recyclerView
        public FolderCreationAlarmListener(RecyclerView recyclerView,
                int cellX, int cellY) {
            //op09_new modify celllayout to recyclerView
            //this.mLayout = layout;
            this.mRecyclerView = recyclerView;
            this.mCellX = cellX;
            this.mCellY = cellY;
        }

        /**
        *M: when alarm is executed, we will jump this function.
        * @param alarm the alarm which to be trigged
        */
        public void onAlarm(Alarm alarm) {
            if (mDragFolderRingAnimator != null) {
                // This shouldn't happen ever, but just in case, make sure we clean up the mess.
                mDragFolderRingAnimator.animateToNaturalState();
            }
            mDragFolderRingAnimator = new FolderRingAnimator(mLauncher, null);
            mDragFolderRingAnimator.mIsPageViewFolderIcon = true;
            mDragFolderRingAnimator.setCell(mCellX, mCellY);
            //op09_new modify celllayout to recyclerView
            //mDragFolderRingAnimator.setCellLayout(mLayout);
            mDragFolderRingAnimator.setRecyclerView(mRecyclerView);
            mDragFolderRingAnimator.animateToAcceptState();

            //mLayout.showFolderAccept(mDragFolderRingAnimator);
           // mLayout.clearDragOutlines();
            ((AllAppsRecyclerView) mRecyclerView).showFolderAccept(mDragFolderRingAnimator);
            ((AllAppsRecyclerView) mRecyclerView).clearDragOutlines();

            LauncherLog.d(TAG, "FolderCreationAlarmListener, onAlarm");
            //setDragMode(Workspace.DRAG_MODE_CREATE_FOLDER);
        }
    }

    //M:[OP09_new] End }@
}
