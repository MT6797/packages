/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.Manifest.permission;
import android.animation.LayoutTransition;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.Manifest;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.gallery3d.common.BitmapCropTask;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.launcher3.util.Thunk;
import com.android.launcher3.util.WallpaperUtils;
import com.android.photos.BitmapRegionTileSource;
import com.android.photos.BitmapRegionTileSource.BitmapSource;
import com.android.photos.views.TiledImageRenderer.TileSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.mediatek.drm.OmaDrmStore;
import java.io.InputStream;
import android.content.ContentResolver;
public class WallpaperPickerActivity extends WallpaperCropActivity {
    static final String TAG = "Launcher.WallpaperPickerActivity";

    public static final int IMAGE_PICK = 5;
    public static final int PICK_WALLPAPER_THIRD_PARTY_ACTIVITY = 6;
    private static final String TEMP_WALLPAPER_TILES = "TEMP_WALLPAPER_TILES";
    private static final String SELECTED_INDEX = "SELECTED_INDEX";
    private static final int FLAG_POST_DELAY_MILLIS = 200;

    @Thunk View mSelectedTile;
    @Thunk boolean mIgnoreNextTap;
    @Thunk OnClickListener mThumbnailOnClickListener;

    @Thunk LinearLayout mWallpapersView;
    @Thunk HorizontalScrollView mWallpaperScrollContainer;
    @Thunk View mWallpaperStrip;

    @Thunk ActionMode.Callback mActionModeCallback;
    @Thunk ActionMode mActionMode;

    @Thunk View.OnLongClickListener mLongClickListener;

    ArrayList<Uri> mTempWallpaperTiles = new ArrayList<Uri>();
    private SavedWallpaperImages mSavedImages;
    @Thunk int mSelectedIndex = -1;

    ///M. ALPS02025663, aviod pick image can' be click twice.
    FrameLayout mPickImageTile;

    /// M: Runtime permission check
    private static final int PERMISSIONS_REQUEST_READ_STROAGE = 1;

    public static abstract class WallpaperTileInfo {
        protected View mView;
        public Drawable mThumb;

        public void setView(View v) {
            mView = v;
        }
        public void onClick(WallpaperPickerActivity a) {}
        public void onSave(WallpaperPickerActivity a) {}
        public void onDelete(WallpaperPickerActivity a) {}
        public boolean isSelectable() { return false; }
        public boolean isNamelessWallpaper() { return false; }
        public void onIndexUpdated(CharSequence label) {
            if (isNamelessWallpaper()) {
                mView.setContentDescription(label);
            }
        }
    }

    public static class PickImageInfo extends WallpaperTileInfo {
        @Override
        public void onClick(WallpaperPickerActivity a) {
            ///M. ALPS02025663, aviod pick image can't be click twice.
            a.mPickImageTile.setClickable(false);

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");

            ///M: Filter DRM files.
            intent.putExtra(OmaDrmStore.DrmExtra.EXTRA_DRM_LEVEL,
                    OmaDrmStore.DrmExtra.LEVEL_FL);

            a.startActivityForResultSafely(intent, IMAGE_PICK);
        }
    }

    public static class UriWallpaperInfo extends WallpaperTileInfo {
        private Uri mUri;
        public UriWallpaperInfo(Uri uri) {
            mUri = uri;
        }
        @Override
        public void onClick(final WallpaperPickerActivity a) {
            a.setWallpaperButtonEnabled(false);
            final BitmapRegionTileSource.UriBitmapSource bitmapSource =
                    new BitmapRegionTileSource.UriBitmapSource(a.getContext(), mUri);
            a.setCropViewTileSource(bitmapSource, true, false, null, new Runnable() {

                @Override
                public void run() {
                    if (bitmapSource.getLoadingState() == BitmapSource.State.LOADED) {
                        a.selectTile(mView);
                        a.setWallpaperButtonEnabled(true);
                    } else {
                        ViewGroup parent = (ViewGroup) mView.getParent();
                        if (parent != null) {
                            parent.removeView(mView);
                            Toast.makeText(a.getContext(), R.string.image_load_fail,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }
        @Override
        public void onSave(final WallpaperPickerActivity a) {
            boolean finishActivityWhenDone = true;
            //add modify by liliang.bao begin  set lockscreen wallpaper 
            int wallPaperType = android.provider.Settings.System.getInt(a.getContentResolver(), "lockscreen_type", Launcher.WALLPAPER_TYPE);
            if(Launcher.WALLPAPER_TYPE == wallPaperType)
             {
            BitmapCropTask.OnBitmapCroppedHandler h = new BitmapCropTask.OnBitmapCroppedHandler() {
                public void onBitmapCropped(byte[] imageBytes) {
                    Point thumbSize = getDefaultThumbnailSize(a.getResources());
                    // rotation is set to 0 since imageBytes has already been correctly rotated
                    Bitmap thumb = createThumbnail(
                            thumbSize, null, null, imageBytes, null, 0, 0, true);
                    a.getSavedImages().writeImage(thumb, imageBytes);
                }
            };
            a.cropImageAndSetWallpaper(mUri, h, finishActivityWhenDone);
          }else
            {
				saveLockScreenPic(a.getContentResolver(), mUri);
				android.provider.Settings.System.putInt(a.getContentResolver(), "lockscreen_type", Launcher.WALLPAPER_TYPE); 
				a.setResult(Activity.RESULT_OK);
				a.finish();
            }
            //add modify by liliang.bao end
        }
        @Override
        public void onDelete(WallpaperPickerActivity a) {
            a.mTempWallpaperTiles.remove(mUri);
        }
        @Override
        public boolean isSelectable() {
            return true;
        }
        @Override
        public boolean isNamelessWallpaper() {
            return true;
        }
    }

    public static class FileWallpaperInfo extends WallpaperTileInfo {
        private File mFile;

        public FileWallpaperInfo(File target, Drawable thumb) {
            mFile = target;
            mThumb = thumb;
        }
        @Override
        public void onClick(final WallpaperPickerActivity a) {
            a.setWallpaperButtonEnabled(false);
            final BitmapRegionTileSource.UriBitmapSource bitmapSource =
                    new BitmapRegionTileSource.UriBitmapSource(a.getContext(), Uri.fromFile(mFile));
            a.setCropViewTileSource(bitmapSource, false, true, null, new Runnable() {

                @Override
                public void run() {
                    if (bitmapSource.getLoadingState() == BitmapSource.State.LOADED) { 
                        a.setWallpaperButtonEnabled(true);
                    }
                }
            });
        }
        @Override
        public void onSave(WallpaperPickerActivity a) {
            // modify by liliang.bao begin  set lockscreen wallpaper 
            int wallPaperType = android.provider.Settings.System.getInt(a.getContentResolver(), "lockscreen_type", Launcher.WALLPAPER_TYPE);
            if(Launcher.WALLPAPER_TYPE == wallPaperType)
            	a.setWallpaper(Uri.fromFile(mFile), true);
            else
            {
				saveLockScreenPic(a.getContentResolver(), Uri.fromFile(mFile));
				android.provider.Settings.System.putInt(a.getContentResolver(), "lockscreen_type", Launcher.WALLPAPER_TYPE); 
				a.setResult(Activity.RESULT_OK);
				a.finish();
            }
            //modify by liliang.bao end
        }
        @Override
        public boolean isSelectable() {
            return true;
        }
        @Override
        public boolean isNamelessWallpaper() {
            return true;
        }
    }

    public static class ResourceWallpaperInfo extends WallpaperTileInfo {
        private Resources mResources;
        private int mResId;

        public ResourceWallpaperInfo(Resources res, int resId, Drawable thumb) {
            mResources = res;
            mResId = resId;
            mThumb = thumb;
        }
        @Override
        public void onClick(final WallpaperPickerActivity a) {
            a.setWallpaperButtonEnabled(false);
            final BitmapRegionTileSource.ResourceBitmapSource bitmapSource =
                    new BitmapRegionTileSource.ResourceBitmapSource(mResources, mResId);
            a.setCropViewTileSource(bitmapSource, false, false, new CropViewScaleProvider() {

                @Override
                public float getScale(TileSource src) {
                    Point wallpaperSize = WallpaperUtils.getDefaultWallpaperSize(
                            a.getResources(), a.getWindowManager());
                    RectF crop = Utils.getMaxCropRect(
                            src.getImageWidth(), src.getImageHeight(),
                            wallpaperSize.x, wallpaperSize.y, false);
                    return wallpaperSize.x / crop.width();
                }
            }, new Runnable() {

                @Override
                public void run() {
                    if (bitmapSource.getLoadingState() == BitmapSource.State.LOADED) {
                        a.setWallpaperButtonEnabled(true);
                    }
                }
            });
        }
        @Override
        public void onSave(WallpaperPickerActivity a) {
            boolean finishActivityWhenDone = true;
            // modify by liliang.bao begin  set lockscreen wallpaper 
            int wallPaperType = android.provider.Settings.System.getInt(a.getContentResolver(), "lockscreen_type", Launcher.WALLPAPER_TYPE);
            if(Launcher.WALLPAPER_TYPE == wallPaperType)
            		a.cropImageAndSetWallpaper(mResources, mResId, finishActivityWhenDone);
            else
            	{
					saveLockScreenPic(mResources,mResId);
					android.provider.Settings.System.putInt(a.getContentResolver(), "lockscreen_type", Launcher.WALLPAPER_TYPE); 
					a.setResult(Activity.RESULT_OK);
					a.finish();
            	}
            // modify by liliang.bao end
        }
        @Override
        public boolean isSelectable() {
            return true;
        }
        @Override
        public boolean isNamelessWallpaper() {
            return true;
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static class DefaultWallpaperInfo extends WallpaperTileInfo {
        public DefaultWallpaperInfo(Drawable thumb) {
            mThumb = thumb;
        }
        @Override
        public void onClick(WallpaperPickerActivity a) {
            CropView c = a.getCropView();

            if (a.mProgressView != null) {
                Log.d(TAG, "DefaultWallpaperInfo.onClick()," +
                    "a.mProgressView.setVisibility(View.INVISIBLE)");
                a.mProgressView.setVisibility(View.INVISIBLE);
            }

            Drawable defaultWallpaper = WallpaperManager.getInstance(a.getContext())
                    .getBuiltInDrawable(c.getWidth(), c.getHeight(), false, 0.5f, 0.5f);
            if (defaultWallpaper == null) {
                Log.w(TAG, "Null default wallpaper encountered.");
                c.setTileSource(null, null);
                return;
            }

            LoadRequest req = new LoadRequest();
            req.moveToLeft = false;
            req.touchEnabled = false;
            req.scaleProvider = new CropViewScaleProvider() {

                @Override
                public float getScale(TileSource src) {
                    return 1f;
                }
            };
            req.result = new DrawableTileSource(a.getContext(),
                    defaultWallpaper, DrawableTileSource.MAX_PREVIEW_SIZE);
            a.onLoadRequestComplete(req, true);
        }
        @Override
        public void onSave(WallpaperPickerActivity a) {
        	 //add modify by liliang.bao begin  set lockscreen wallpaper 
            int wallPaperType = android.provider.Settings.System.getInt(a.getContentResolver(), "lockscreen_type", Launcher.WALLPAPER_TYPE);
           if(Launcher.WALLPAPER_TYPE == wallPaperType)
            {
            try {
                WallpaperManager.getInstance(a.getContext()).clear();
                a.setResult(Activity.RESULT_OK);
            } catch (IOException e) {
                Log.w("Setting wallpaper to default threw exception", e);
            }
            a.finish();
           }
          else
           {
				saveLockScreenPic(a.getResources());
				android.provider.Settings.System.putInt(a.getContentResolver(), "lockscreen_type", Launcher.WALLPAPER_TYPE); 
				a.setResult(Activity.RESULT_OK);
				a.finish();
           }
           //modify by liliang.bao end
        }
        @Override
        public boolean isSelectable() {
            return true;
        }
        @Override
        public boolean isNamelessWallpaper() {
            return true;
        }
    }

    /**
     * shows the system wallpaper behind the window and hides the {@link
     * #mCropView} if visible
     * @param visible should the system wallpaper be shown
     */
    protected void setSystemWallpaperVisiblity(final boolean visible) {
        // hide our own wallpaper preview if necessary
        if(!visible) {
            mCropView.setVisibility(View.VISIBLE);
        } else {
            changeWallpaperFlags(visible);
        }
        // the change of the flag must be delayed in order to avoid flickering,
        // a simple post / double post does not suffice here
        mCropView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!visible) {
                    changeWallpaperFlags(visible);
                } else {
                    mCropView.setVisibility(View.INVISIBLE);
                }
            }
        }, FLAG_POST_DELAY_MILLIS);
    }

    @Thunk void changeWallpaperFlags(boolean visible) {
        int desiredWallpaperFlag = visible ? WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER : 0;
        int currentWallpaperFlag = getWindow().getAttributes().flags
                & WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
        if (desiredWallpaperFlag != currentWallpaperFlag) {
            getWindow().setFlags(desiredWallpaperFlag,
                    WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        }
    }

    @Override
    protected void onLoadRequestComplete(LoadRequest req, boolean success) {
        super.onLoadRequestComplete(req, success);
        if (success) {
            setSystemWallpaperVisiblity(false);
        }
    }

    protected void onResume() {
        super.onResume();
        /// M: Runtime permission check @ {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            ((AlphaDisableableButton) mSetWallpaperButton).setClickable(true);

            ///M. ALPS02025663, aviod pick image can't be click twice.
            mPickImageTile.setClickable(true);
            ///M.
        }
        /// @ }
    }

    // called by onCreate; this is subclassed to overwrite WallpaperCropActivity
    protected void init() {
        /// M: Runtime permission check @ {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "requestPermissions");
            if (!mPermRequesting) {
                requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_READ_STROAGE);
                mPermRequesting = true;
            }
        } else {
        /// @ }
        setContentView(R.layout.wallpaper_picker);

        mCropView = (CropView) findViewById(R.id.cropView);
        mCropView.setVisibility(View.INVISIBLE);

        mProgressView = findViewById(R.id.loading);
        mWallpaperScrollContainer = (HorizontalScrollView) findViewById(R.id.wallpaper_scroll_container);
        mWallpaperStrip = findViewById(R.id.wallpaper_strip);
        mCropView.setTouchCallback(new CropView.TouchCallback() {
            ViewPropertyAnimator mAnim;
            @Override
            public void onTouchDown() {
                if (mAnim != null) {
                    mAnim.cancel();
                }
                if (mWallpaperStrip.getAlpha() == 1f) {
                    mIgnoreNextTap = true;
                }
                mAnim = mWallpaperStrip.animate();
                mAnim.alpha(0f)
                    .setDuration(150)
                    .withEndAction(new Runnable() {
                        public void run() {
                            mWallpaperStrip.setVisibility(View.INVISIBLE);
                        }
                    });
                mAnim.setInterpolator(new AccelerateInterpolator(0.75f));
                mAnim.start();
            }
            @Override
            public void onTouchUp() {
                mIgnoreNextTap = false;
            }
            @Override
            public void onTap() {
                boolean ignoreTap = mIgnoreNextTap;
                mIgnoreNextTap = false;
                if (!ignoreTap) {
                    if (mAnim != null) {
                        mAnim.cancel();
                    }
                    mWallpaperStrip.setVisibility(View.VISIBLE);
                    mAnim = mWallpaperStrip.animate();
                    mAnim.alpha(1f)
                         .setDuration(150)
                         .setInterpolator(new DecelerateInterpolator(0.75f));
                    mAnim.start();
                }
            }
        });

        mThumbnailOnClickListener = new OnClickListener() {
            public void onClick(View v) {
                if (mActionMode != null) {
                    // When CAB is up, clicking toggles the item instead
                    if (v.isLongClickable()) {
                        mLongClickListener.onLongClick(v);
                    }
                    return;
                }
                setWallpaperButtonEnabled(true);
                WallpaperTileInfo info = (WallpaperTileInfo) v.getTag();

                /// M: ALPS01665621, info maybe null.
                if (info == null) {
                    return;
                }
                /// M.

                if (info.isSelectable() && v.getVisibility() == View.VISIBLE) {
                    selectTile(v);
                }
                info.onClick(WallpaperPickerActivity.this);
            }
        };
        mLongClickListener = new View.OnLongClickListener() {
            // Called when the user long-clicks on someView
            public boolean onLongClick(View view) {
                CheckableFrameLayout c = (CheckableFrameLayout) view;
                c.toggle();

                if (mActionMode != null) {
                    mActionMode.invalidate();
                } else {
                    // Start the CAB using the ActionMode.Callback defined below
                    mActionMode = startActionMode(mActionModeCallback);

                    ///M. ALPS01952726, becuase of action mode changed in L1,
                    ///We should call invalidate after startActionMode. Or else,
                    ///onPrepareActionMode will not be called.
                    mActionMode.invalidate();
                    ///M.

                    int childCount = mWallpapersView.getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        mWallpapersView.getChildAt(i).setSelected(false);
                    }
                }
                return true;
            }
        };

        // Populate the built-in wallpapers
        ArrayList<WallpaperTileInfo> wallpapers = findBundledWallpapers();
        mWallpapersView = (LinearLayout) findViewById(R.id.wallpaper_list);
        SimpleWallpapersAdapter ia = new SimpleWallpapersAdapter(getContext(), wallpapers);
        populateWallpapersFromAdapter(mWallpapersView, ia, false);

        // Populate the saved wallpapers
        mSavedImages = new SavedWallpaperImages(getContext());
        mSavedImages.loadThumbnailsAndImageIdList();
        populateWallpapersFromAdapter(mWallpapersView, mSavedImages, true);

        //modify by liliang.bao begin
       int wallPaperType = android.provider.Settings.System.getInt(getContentResolver(), "lockscreen_type", Launcher.WALLPAPER_TYPE);
       if(Launcher.LOCKSCREEN_WALLPAPER_TYPE != wallPaperType)
        {
        // Populate the live wallpapers
        final LinearLayout liveWallpapersView =
                (LinearLayout) findViewById(R.id.live_wallpaper_list);
        final LiveWallpaperListAdapter a = new LiveWallpaperListAdapter(getContext());
        a.registerDataSetObserver(new DataSetObserver() {
            public void onChanged() {
                liveWallpapersView.removeAllViews();
                populateWallpapersFromAdapter(liveWallpapersView, a, false);
                initializeScrollForRtl();
                updateTileIndices();
            }
          });
        }
     //modify by liliang.bao end
       
        // Populate the third-party wallpaper pickers
        final LinearLayout thirdPartyWallpapersView =
                (LinearLayout) findViewById(R.id.third_party_wallpaper_list);
        final ThirdPartyWallpaperPickerListAdapter ta =
                new ThirdPartyWallpaperPickerListAdapter(getContext());
        populateWallpapersFromAdapter(thirdPartyWallpapersView, ta, false);

        // Add a tile for the Gallery
        LinearLayout masterWallpaperList = (LinearLayout) findViewById(R.id.master_wallpaper_list);
        FrameLayout pickImageTile = (FrameLayout) getLayoutInflater().
                inflate(R.layout.wallpaper_picker_image_picker_item, masterWallpaperList, false);

        ///M. ALPS02025663, aviod pick image can't be click twice.
        mPickImageTile = pickImageTile;

        masterWallpaperList.addView(pickImageTile, 0);

        // Make its background the last photo taken on external storage
        /*Bitmap lastPhoto = getThumbnailOfLastPhoto();
        if (lastPhoto != null) {
            ImageView galleryThumbnailBg =
                    (ImageView) pickImageTile.findViewById(R.id.wallpaper_image);
            galleryThumbnailBg.setImageBitmap(lastPhoto);
            int colorOverlay = getResources().getColor(R.color.wallpaper_picker_translucent_gray);
            galleryThumbnailBg.setColorFilter(colorOverlay, PorterDuff.Mode.SRC_ATOP);
        }*/

        ///* M alps01601180, use AsnycTask get last thumbnail.
        GetLastThumbnailTask task = new GetLastThumbnailTask(this, pickImageTile);
        task.execute();

        PickImageInfo pickImageInfo = new PickImageInfo();
        pickImageTile.setTag(pickImageInfo);
        pickImageInfo.setView(pickImageTile);
        pickImageTile.setOnClickListener(mThumbnailOnClickListener);

        // Select the first item; wait for a layout pass so that we initialize the dimensions of
        // cropView or the defaultWallpaperView first
        mCropView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if ((right - left) > 0 && (bottom - top) > 0) {
                    if (mSelectedIndex >= 0 && mSelectedIndex < mWallpapersView.getChildCount()) {
                        mThumbnailOnClickListener.onClick(
                                mWallpapersView.getChildAt(mSelectedIndex));
                        setSystemWallpaperVisiblity(false);
                    }
                    v.removeOnLayoutChangeListener(this);
                }
            }
        });

        updateTileIndices();

        // Update the scroll for RTL
        initializeScrollForRtl();

        // Create smooth layout transitions for when items are deleted
        final LayoutTransition transitioner = new LayoutTransition();
        transitioner.setDuration(200);
        transitioner.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0);
        transitioner.setAnimator(LayoutTransition.DISAPPEARING, null);
        mWallpapersView.setLayoutTransition(transitioner);

        // Action bar
        // Show the custom action bar view
        final ActionBar actionBar = getActionBar();
        actionBar.setCustomView(R.layout.actionbar_set_wallpaper);
        actionBar.getCustomView().setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Ensure that a tile is slelected and loaded.
                        if (mSelectedTile != null && mCropView.getTileSource() != null) {
                            // Prevent user from selecting any new tile.
                            mWallpaperStrip.setVisibility(View.GONE);
                            actionBar.hide();

                            WallpaperTileInfo info = (WallpaperTileInfo) mSelectedTile.getTag();
                            info.onSave(WallpaperPickerActivity.this);
                        } else {
                            // no tile was selected, so we just finish the activity and go back
                            setResult(Activity.RESULT_OK);
                            finish();
                        }
                    }
                });
        mSetWallpaperButton = findViewById(R.id.set_wallpaper_button);
        //add by liliang.bao begin
        if(Launcher.WALLPAPER_TYPE == wallPaperType)
        	 ((TextView)mSetWallpaperButton).setText(R.string.wallpaper_instructions);
        else
        	((TextView)mSetWallpaperButton).setText(R.string.Set_lockscreen_wallpaper);
      //add by liliang.bao end 
        // CAB for deleting items
        mActionModeCallback = new ActionMode.Callback() {
            // Called when the action mode is created; startActionMode() was called
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                // Inflate a menu resource providing context menu items
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.cab_delete_wallpapers, menu);
                return true;
            }

            private int numCheckedItems() {
                int childCount = mWallpapersView.getChildCount();
                int numCheckedItems = 0;
                for (int i = 0; i < childCount; i++) {
                    CheckableFrameLayout c = (CheckableFrameLayout) mWallpapersView.getChildAt(i);
                    if (c.isChecked()) {
                        numCheckedItems++;
                    }
                }
                return numCheckedItems;
            }

            // Called each time the action mode is shown. Always called after onCreateActionMode,
            // but may be called multiple times if the mode is invalidated.
            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                int numCheckedItems = numCheckedItems();
                if (numCheckedItems == 0) {
                    mode.finish();
                    return true;
                } else {
                    mode.setTitle(getResources().getQuantityString(
                            R.plurals.number_of_items_selected, numCheckedItems, numCheckedItems));
                    return true;
                }
            }

            // Called when the user selects a contextual menu item
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_delete) {
                    int childCount = mWallpapersView.getChildCount();
                    ArrayList<View> viewsToRemove = new ArrayList<View>();
                    boolean selectedTileRemoved = false;
                    for (int i = 0; i < childCount; i++) {
                        CheckableFrameLayout c =
                                (CheckableFrameLayout) mWallpapersView.getChildAt(i);
                        if (c.isChecked()) {
                            WallpaperTileInfo info = (WallpaperTileInfo) c.getTag();
                            info.onDelete(WallpaperPickerActivity.this);
                            viewsToRemove.add(c);
                            if (i == mSelectedIndex) {
                                selectedTileRemoved = true;
                            }
                        }
                    }
                    for (View v : viewsToRemove) {
                        mWallpapersView.removeView(v);
                    }
                    if (selectedTileRemoved) {
                        mSelectedIndex = -1;
                        mSelectedTile = null;
                        /// M:ALPS01901591, if it's delete the selected tile,
                        /// don't setSystemWallpaperVisiblity, or else the cropView can't be touched
                        //setSystemWallpaperVisiblity(true);
                        ///M.
                    }
                    updateTileIndices();
                    mode.finish(); // Action picked, so close the CAB

                    /// M: ALPS01901591, if it's delete the selected tile, click the first view.
                    if (selectedTileRemoved) {
                        mThumbnailOnClickListener.onClick(
                            mWallpapersView.getChildAt(0));
                    }
                    /// M.
                    return true;
                } else {
                    return false;
                }
            }

            // Called when the user exits the action mode
            @Override
            public void onDestroyActionMode(ActionMode mode) {
                int childCount = mWallpapersView.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    CheckableFrameLayout c = (CheckableFrameLayout) mWallpapersView.getChildAt(i);
                    c.setChecked(false);
                }
                if (mSelectedTile != null) {
                    mSelectedTile.setSelected(true);
                }
                mActionMode = null;
            }
        };
      }
    }

    public void setWallpaperButtonEnabled(boolean enabled) {
        mSetWallpaperButton.setEnabled(enabled);
    }

    @Thunk void selectTile(View v) {
        if (mSelectedTile != null) {
            mSelectedTile.setSelected(false);
            mSelectedTile = null;
        }
        mSelectedTile = v;
        v.setSelected(true);
        mSelectedIndex = mWallpapersView.indexOfChild(v);
        // TODO: Remove this once the accessibility framework and
        // services have better support for selection state.
        v.announceForAccessibility(
                getContext().getString(R.string.announce_selection, v.getContentDescription()));
    }

    @Thunk void initializeScrollForRtl() {
        if (Utilities.isRtl(getResources())) {
            final ViewTreeObserver observer = mWallpaperScrollContainer.getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                public void onGlobalLayout() {
                    LinearLayout masterWallpaperList =
                            (LinearLayout) findViewById(R.id.master_wallpaper_list);
                    mWallpaperScrollContainer.scrollTo(masterWallpaperList.getWidth(), 0);
                    mWallpaperScrollContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        }
    }

    protected Bitmap getThumbnailOfLastPhoto() {
        boolean canReadExternalStorage = getActivity().checkPermission(
                Manifest.permission.READ_EXTERNAL_STORAGE, Process.myPid(), Process.myUid()) ==
                PackageManager.PERMISSION_GRANTED;

        if (!canReadExternalStorage) {
            // MediaStore.Images.Media.EXTERNAL_CONTENT_URI requires
            // the READ_EXTERNAL_STORAGE permission
            return null;
        }

        Cursor cursor = MediaStore.Images.Media.query(getContext().getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Images.ImageColumns._ID,
                    MediaStore.Images.ImageColumns.DATE_TAKEN},
                null, null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC LIMIT 1");

        Bitmap thumb = null;
        if (cursor != null) {
            if (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                thumb = MediaStore.Images.Thumbnails.getThumbnail(getContext().getContentResolver(),
                        id, MediaStore.Images.Thumbnails.MINI_KIND, null);
            }
            cursor.close();
        }
        return thumb;
    }

    public void onStop() {
        super.onStop();
        mWallpaperStrip = findViewById(R.id.wallpaper_strip);
        /// M: Runtime permission check
        if (mWallpaperStrip != null && mWallpaperStrip.getAlpha() < 1f) {
            mWallpaperStrip.setAlpha(1f);
            mWallpaperStrip.setVisibility(View.VISIBLE);
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        /// M: ALPS02402662 rotate device will show twice permission dialog
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(TEMP_WALLPAPER_TILES, mTempWallpaperTiles);
        outState.putInt(SELECTED_INDEX, mSelectedIndex);
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        ArrayList<Uri> uris = savedInstanceState.getParcelableArrayList(TEMP_WALLPAPER_TILES);
        for (Uri uri : uris) {
            addTemporaryWallpaperTile(uri, true);
        }
        mSelectedIndex = savedInstanceState.getInt(SELECTED_INDEX, -1);
    }

    @Thunk void populateWallpapersFromAdapter(ViewGroup parent, BaseAdapter adapter,
            boolean addLongPressHandler) {
        for (int i = 0; i < adapter.getCount(); i++) {
            FrameLayout thumbnail = (FrameLayout) adapter.getView(i, null, parent);
            parent.addView(thumbnail, i);
            WallpaperTileInfo info = (WallpaperTileInfo) adapter.getItem(i);
            thumbnail.setTag(info);
            info.setView(thumbnail);
            if (addLongPressHandler) {
                addLongPressHandler(thumbnail);
            }
            thumbnail.setOnClickListener(mThumbnailOnClickListener);
        }
    }

    @Thunk void updateTileIndices() {
        LinearLayout masterWallpaperList = (LinearLayout) findViewById(R.id.master_wallpaper_list);
        final int childCount = masterWallpaperList.getChildCount();
        final Resources res = getResources();

        // Do two passes; the first pass gets the total number of tiles
        int numTiles = 0;
        for (int passNum = 0; passNum < 2; passNum++) {
            int tileIndex = 0;
            for (int i = 0; i < childCount; i++) {
                View child = masterWallpaperList.getChildAt(i);
                LinearLayout subList;

                int subListStart;
                int subListEnd;
                if (child.getTag() instanceof WallpaperTileInfo) {
                    subList = masterWallpaperList;
                    subListStart = i;
                    subListEnd = i + 1;
                } else { // if (child instanceof LinearLayout) {
                    subList = (LinearLayout) child;
                    subListStart = 0;
                    subListEnd = subList.getChildCount();
                }

                for (int j = subListStart; j < subListEnd; j++) {
                    WallpaperTileInfo info = (WallpaperTileInfo) subList.getChildAt(j).getTag();
                       if (info != null && info.isNamelessWallpaper()) {
                        if (passNum == 0) {
                            numTiles++;
                        } else {
                            CharSequence label = res.getString(
                                    R.string.wallpaper_accessibility_name, ++tileIndex, numTiles);
                            info.onIndexUpdated(label);
                        }
                    }
                }
            }
        }
    }

    ///M. ALPS2008466, MOVE getDefaultThumbnailSize(), createThumbnail()
    ///    to WallpaperCropperActivity.
    ///M.

    private void addTemporaryWallpaperTile(final Uri uri, final boolean fromRestore) {
        addTemporaryWallpaperTile(uri, fromRestore, false);
    }
    private void addTemporaryWallpaperTile(final Uri uri, final boolean fromRestore,
        final boolean update) {
        mTempWallpaperTiles.add(uri);
        // Add a tile for the image picked from Gallery
        final FrameLayout pickedImageThumbnail = (FrameLayout) getLayoutInflater().
                inflate(R.layout.wallpaper_picker_item, mWallpapersView, false);
        pickedImageThumbnail.setVisibility(View.GONE);
        mWallpapersView.addView(pickedImageThumbnail, 0);

        // Load the thumbnail
        final ImageView image = (ImageView) pickedImageThumbnail.findViewById(R.id.wallpaper_image);
        final Point defaultSize = getDefaultThumbnailSize(this.getResources());
        final Context context = getContext();
        new AsyncTask<Void, Bitmap, Bitmap>() {
            protected Bitmap doInBackground(Void...args) {
                try {
                    int rotation = BitmapUtils.getRotationFromExif(context, uri);
                    return createThumbnail(defaultSize, context, uri, null, null, 0, rotation, false);
                } catch (SecurityException securityException) {
                    if (isActivityDestroyed()) {
                        // Temporarily granted permissions are revoked when the activity
                        // finishes, potentially resulting in a SecurityException here.
                        // Even though {@link #isDestroyed} might also return true in different
                        // situations where the configuration changes, we are fine with
                        // catching these cases here as well.
                        cancel(false);
                    } else {
                        // otherwise it had a different cause and we throw it further
                        throw securityException;
                    }
                    return null;
                }
            }
            protected void onPostExecute(Bitmap thumb) {
                if (!isCancelled() && thumb != null) {
                    image.setImageBitmap(thumb);
                    Drawable thumbDrawable = image.getDrawable();
                    thumbDrawable.setDither(true);
                    pickedImageThumbnail.setVisibility(View.VISIBLE);
                } else {
                    Log.e(TAG, "Error loading thumbnail for uri=" + uri);

                    ///M : Add a toast when it is a broken pic.
                    String str = getString(R.string.wallpaper_load_fail);
                    Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
                    mWallpapersView.removeViewAt(0);
                    mTempWallpaperTiles.remove(uri);
                    return;
                }
            }
        }.execute();

        ///M: ALPS02340795, Check whether it is valid.
        if (isInValidImagewallpaper(context, defaultSize, uri)) {
            return ;
        }

        UriWallpaperInfo info = new UriWallpaperInfo(uri);
        pickedImageThumbnail.setTag(info);
        info.setView(pickedImageThumbnail);
        addLongPressHandler(pickedImageThumbnail);
        updateTileIndices();
        pickedImageThumbnail.setOnClickListener(mThumbnailOnClickListener);
        if (!fromRestore) {
            mThumbnailOnClickListener.onClick(pickedImageThumbnail);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                addTemporaryWallpaperTile(uri, false);
            }
        } else if (requestCode == PICK_WALLPAPER_THIRD_PARTY_ACTIVITY
                && resultCode == Activity.RESULT_OK) {
            // Something was set on the third-party activity.
            setResult(Activity.RESULT_OK);
            finish();
        }
    }

    private void addLongPressHandler(View v) {
        v.setOnLongClickListener(mLongClickListener);

        // Enable stylus button to also trigger long click.
        final StylusEventHelper stylusEventHelper = new StylusEventHelper(v);
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                return stylusEventHelper.checkAndPerformStylusEvent(event);
            }
        });
    }

    private ArrayList<WallpaperTileInfo> findBundledWallpapers() {
        final PackageManager pm = getContext().getPackageManager();
        final ArrayList<WallpaperTileInfo> bundled = new ArrayList<WallpaperTileInfo>(24);

        Partner partner = Partner.get(pm);
        if (partner != null) {
            final Resources partnerRes = partner.getResources();
            final int resId = partnerRes.getIdentifier(Partner.RES_WALLPAPERS, "array",
                    partner.getPackageName());
            if (resId != 0) {
                addWallpapers(bundled, partnerRes, partner.getPackageName(), resId);
            }

            // Add system wallpapers
            File systemDir = partner.getWallpaperDirectory();
            if (systemDir != null && systemDir.isDirectory()) {
                for (File file : systemDir.listFiles()) {
                    if (!file.isFile()) {
                        continue;
                    }
                    String name = file.getName();
                    int dotPos = name.lastIndexOf('.');
                    String extension = "";
                    if (dotPos >= -1) {
                        extension = name.substring(dotPos);
                        name = name.substring(0, dotPos);
                    }

                    if (name.endsWith("_small")) {
                        // it is a thumbnail
                        continue;
                    }

                    File thumbnail = new File(systemDir, name + "_small" + extension);
                    Bitmap thumb = BitmapFactory.decodeFile(thumbnail.getAbsolutePath());
                    if (thumb != null) {
                        bundled.add(new FileWallpaperInfo(file, new BitmapDrawable(thumb)));
                    }
                }
            }
        }

        Pair<ApplicationInfo, Integer> r = getWallpaperArrayResourceId();
        if (r != null) {
            try {
                Resources wallpaperRes = getContext().getPackageManager()
                        .getResourcesForApplication(r.first);
                addWallpapers(bundled, wallpaperRes, r.first.packageName, r.second);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }

        if (partner == null || !partner.hideDefaultWallpaper()) {
            // Add an entry for the default wallpaper (stored in system resources)
            WallpaperTileInfo defaultWallpaperInfo = Utilities.ATLEAST_KITKAT
                    ? getDefaultWallpaper() : getPreKKDefaultWallpaperInfo();
            if (defaultWallpaperInfo != null) {
                bundled.add(0, defaultWallpaperInfo);
            }
        }
        return bundled;
    }

    private boolean writeImageToFileAsJpeg(File f, Bitmap b) {
        try {
            f.createNewFile();
            FileOutputStream thumbFileStream =
                    getContext().openFileOutput(f.getName(), Context.MODE_PRIVATE);
            b.compress(Bitmap.CompressFormat.JPEG, 95, thumbFileStream);
            thumbFileStream.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error while writing bitmap to file " + e);
            f.delete();
        }
        return false;
    }

    private File getDefaultThumbFile() {
        return new File(getContext().getFilesDir(), Build.VERSION.SDK_INT
                + "_" + LauncherFiles.DEFAULT_WALLPAPER_THUMBNAIL);
    }

    private boolean saveDefaultWallpaperThumb(Bitmap b) {
        // Delete old thumbnails.
        new File(getContext().getFilesDir(), LauncherFiles.DEFAULT_WALLPAPER_THUMBNAIL_OLD).delete();
        new File(getContext().getFilesDir(), LauncherFiles.DEFAULT_WALLPAPER_THUMBNAIL).delete();

        for (int i = Build.VERSION_CODES.JELLY_BEAN; i < Build.VERSION.SDK_INT; i++) {
            new File(getContext().getFilesDir(), i + "_"
                    + LauncherFiles.DEFAULT_WALLPAPER_THUMBNAIL).delete();
        }
        return writeImageToFileAsJpeg(getDefaultThumbFile(), b);
    }

    private ResourceWallpaperInfo getPreKKDefaultWallpaperInfo() {
        Resources sysRes = Resources.getSystem();
        int resId = sysRes.getIdentifier("default_wallpaper", "drawable", "android");

        File defaultThumbFile = getDefaultThumbFile();
        Bitmap thumb = null;
        boolean defaultWallpaperExists = false;
        if (defaultThumbFile.exists()) {
            thumb = BitmapFactory.decodeFile(defaultThumbFile.getAbsolutePath());
            defaultWallpaperExists = true;
        } else {
            Resources res = getResources();
            Point defaultThumbSize = getDefaultThumbnailSize(res);
            int rotation = BitmapUtils.getRotationFromExif(res, resId);
            thumb = createThumbnail(
                    defaultThumbSize, getContext(), null, null, sysRes, resId, rotation, false);
            if (thumb != null) {
                defaultWallpaperExists = saveDefaultWallpaperThumb(thumb);
            }
        }
        if (defaultWallpaperExists) {
            return new ResourceWallpaperInfo(sysRes, resId, new BitmapDrawable(thumb));
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private DefaultWallpaperInfo getDefaultWallpaper() {
        File defaultThumbFile = getDefaultThumbFile();
        Bitmap thumb = null;
        boolean defaultWallpaperExists = false;
        if (defaultThumbFile.exists()) {
            thumb = BitmapFactory.decodeFile(defaultThumbFile.getAbsolutePath());
            defaultWallpaperExists = true;
        }

        ///M. ALPS2029155, maybe the defaultThumbFile exist, but the content is null.
        if (thumb == null) {
            Resources res = getResources();
            Point defaultThumbSize = getDefaultThumbnailSize(res);
            Drawable wallpaperDrawable = WallpaperManager.getInstance(getContext()).getBuiltInDrawable(
                    defaultThumbSize.x, defaultThumbSize.y, true, 0.5f, 0.5f);
            if (wallpaperDrawable != null) {
                thumb = Bitmap.createBitmap(
                        defaultThumbSize.x, defaultThumbSize.y, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(thumb);
                wallpaperDrawable.setBounds(0, 0, defaultThumbSize.x, defaultThumbSize.y);
                wallpaperDrawable.draw(c);
                c.setBitmap(null);
            }
            if (thumb != null) {
                defaultWallpaperExists = saveDefaultWallpaperThumb(thumb);
            }
        }
        if (defaultWallpaperExists) {
            return new DefaultWallpaperInfo(new BitmapDrawable(thumb));
        }
        return null;
    }

    public Pair<ApplicationInfo, Integer> getWallpaperArrayResourceId() {
        // Context.getPackageName() may return the "original" package name,
        // com.android.launcher3; Resources needs the real package name,
        // com.android.launcher3. So we ask Resources for what it thinks the
        // package name should be.
        final String packageName = getResources().getResourcePackageName(R.array.wallpapers);
        try {
            ApplicationInfo info = getContext().getPackageManager().getApplicationInfo(packageName, 0);
            return new Pair<ApplicationInfo, Integer>(info, R.array.wallpapers);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private void addWallpapers(ArrayList<WallpaperTileInfo> known, Resources res,
            String packageName, int listResId) {
        final String[] extras = res.getStringArray(listResId);
        for (String extra : extras) {
            int resId = res.getIdentifier(extra, "drawable", packageName);
            if (resId != 0) {
                final int thumbRes = res.getIdentifier(extra + "_small", "drawable", packageName);

                if (thumbRes != 0) {
                    ResourceWallpaperInfo wallpaperInfo =
                            new ResourceWallpaperInfo(res, resId, res.getDrawable(thumbRes));
                    known.add(wallpaperInfo);
                    // Log.d(TAG, "add: [" + packageName + "]: " + extra + " (" + res + ")");
                }
            } else {
                Log.e(TAG, "Couldn't find wallpaper " + extra);
            }
        }
    }

    public CropView getCropView() {
        return mCropView;
    }

    public SavedWallpaperImages getSavedImages() {
        return mSavedImages;
    }

    private static class SimpleWallpapersAdapter extends ArrayAdapter<WallpaperTileInfo> {
        private final LayoutInflater mLayoutInflater;

        SimpleWallpapersAdapter(Context context, ArrayList<WallpaperTileInfo> wallpapers) {
            super(context, R.layout.wallpaper_picker_item, wallpapers);
            mLayoutInflater = LayoutInflater.from(context);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            Drawable thumb = getItem(position).mThumb;
            if (thumb == null) {
                Log.e(TAG, "Error decoding thumbnail for wallpaper #" + position);
            }
            return createImageTileView(mLayoutInflater, convertView, parent, thumb);
        }
    }

    public static View createImageTileView(LayoutInflater layoutInflater,
            View convertView, ViewGroup parent, Drawable thumb) {
        View view;

        if (convertView == null) {
            view = layoutInflater.inflate(R.layout.wallpaper_picker_item, parent, false);
        } else {
            view = convertView;
        }

        ImageView image = (ImageView) view.findViewById(R.id.wallpaper_image);

        if (thumb != null) {
            image.setImageDrawable(thumb);
            thumb.setDither(true);
        }

        return view;
    }

    public void startActivityForResultSafely(Intent intent, int requestCode) {
        Utilities.startActivityForResultSafely(getActivity(), intent, requestCode);
    }

    @Override
    public boolean enableRotation() {
        // Check if rotation is enabled for this device.
        if (Utilities.isRotationAllowedForDevice(getContext()))
            return true;

        // Check if the user has specifically enabled rotation via preferences.
        return Utilities.isAllowRotationPrefEnabled(getApplicationContext(), true);
    }


    ///M: ALPS016011980, use AsyncTask, when get last thumbnail.
    protected static class GetLastThumbnailTask extends AsyncTask<Void, Void, Boolean> {
        Bitmap mLastPhoto;
        Context mContext;
        FrameLayout mPickImageTile;

        public GetLastThumbnailTask(Context c, FrameLayout pickImageTile) {
            mContext = c;
            mPickImageTile = pickImageTile;
        }

        protected Boolean doInBackground(Void... params) {
            Log.i(TAG, "GetLastThumbnailTask: doInBackground_in");

            Cursor cursor = MediaStore.Images.Media.query(mContext.getContentResolver(),
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            new String[] { MediaStore.Images.ImageColumns._ID,
                            MediaStore.Images.ImageColumns.DATE_TAKEN},
                            null, null, MediaStore.Images.ImageColumns.DATE_TAKEN
                            + " DESC LIMIT 1");
            if (cursor == null) {
                return false;
            }

            try {
                if (cursor.moveToNext()) {
                    int id = cursor.getInt(0);
                    mLastPhoto = MediaStore.Images.Thumbnails.getThumbnail(
                        mContext.getContentResolver(), id,
                        MediaStore.Images.Thumbnails.MINI_KIND, null);
                }
            } finally {
                cursor.close();
            }

            Log.i(TAG, "GetLastThumbnailTask: out");
            return true;
        }

        protected void onPostExecute(Boolean result) {
            Log.i(TAG, "GetLastThumbnailTask: onPostExecute");
            if (mLastPhoto != null) {
                ImageView galleryThumbnailBg =
                    (ImageView) mPickImageTile.findViewById(R.id.wallpaper_image);
                galleryThumbnailBg.setImageBitmap(mLastPhoto);
                int colorOverlay = mContext.getResources()
                    .getColor(R.color.wallpaper_picker_translucent_gray);
                galleryThumbnailBg.setColorFilter(colorOverlay, PorterDuff.Mode.SRC_ATOP);
            }
        }
    }

    /// M: Runtime permission check @{
    @Override
    public void onRequestPermissionsResult(int requestCode,
            String permissions[], int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult requestResult: " + grantResults[0]);
        mPermRequesting = true;
        if (requestCode == PERMISSIONS_REQUEST_READ_STROAGE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                if (!shouldShowRequestPermissionRationale(
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Toast.makeText(this,
                            com.mediatek.internal.R.string.denied_required_permission,
                            Toast.LENGTH_SHORT).show();
                }
                finish();
            } else {
                Intent intent = getIntent();
                setResult(Activity.RESULT_OK);
                finish();
                startActivity(intent);
            }
        }
    }
    /// @}

    ///M: ALPS02340795, invalid wallpaper check.
    private boolean isInValidImagewallpaper(Context context, Point size, Uri uri) {
        int rotation = BitmapUtils.getRotationFromExif(context, uri);
        BitmapCropTask cropTask = new BitmapCropTask(
               context, uri, null, rotation, size.x, size.y, false, true, null);
        return cropTask.isOutOfSpecLimit();
    }
    
    private static void saveLockScreenPic(Resources res,int resid)
    {
   		InputStream ios = res.openRawResource(resid);
   		String dir = "/data/data/com.android.launcher3/lockWallPaper";
   		String savePath = dir + "/lockscreenwallpaper.jpg";
   		File file = new File(dir);
   		try {
   			if (!file.exists()) {
   				file.mkdir();
   				Runtime.getRuntime().exec("chmod 775 " + dir).waitFor();
   			}
   			Log.e(TAG, "saveLockScreenPic begin");
   			FileOutputStream fos = new FileOutputStream(savePath);
   			byte[] buffer = new byte[32768];
   			int amt;
   			while ((amt = ios.read(buffer)) > 0) {
   				fos.write(buffer, 0, amt);
   			}
   			ios.close();
   			fos.close();
   			if ((new File(savePath)).exists())
   				Runtime.getRuntime().exec("chmod 755 " + savePath).waitFor();
   			Log.e(TAG, "saveLockScreenPic end");
   		} catch (Exception e) {
   		}
   		 
   	}
      
      private static void saveLockScreenPic(Resources res)
    {
   	   Resources sysRes = Resources.getSystem();
   	   int resId = sysRes.getIdentifier("default_wallpaper", "drawable", "android");
   		InputStream ios = res.openRawResource(resId);
   		String dir = "/data/data/com.android.launcher3/lockWallPaper";
   		String savePath = dir + "/lockscreenwallpaper.jpg";
   		File file = new File(dir);
   		try {
   			if (!file.exists()) {
   				file.mkdir();
   				Runtime.getRuntime().exec("chmod 775 " + dir).waitFor();
   			}
   			Log.e(TAG, "saveLockScreenPic begin");
   			FileOutputStream fos = new FileOutputStream(savePath);
   			byte[] buffer = new byte[32768];
   			int amt;
   			while ((amt = ios.read(buffer)) > 0) {
   				fos.write(buffer, 0, amt);
   			}
   			ios.close();
   			fos.close();
   			if ((new File(savePath)).exists())
   				Runtime.getRuntime().exec("chmod 755 " + savePath).waitFor();
   			Log.e(TAG, "saveLockScreenPic end");
   		} catch (Exception e) {
   		}
   		
   	}
      
      private static void saveLockScreenPic(ContentResolver contentRes, Uri uri)
    {
   		try {
   			InputStream ios = contentRes.openInputStream(uri);
   			String dir = "/data/data/com.android.launcher3/lockWallPaper";
   			String savePath = dir + "/lockscreenwallpaper.jpg";
   			File file = new File(dir);

   			if (!file.exists()) {
   				file.mkdir();
   				Runtime.getRuntime().exec("chmod 775 " + dir).waitFor();
   			}
   			Log.e(TAG, "saveLockScreenPic begin");
   			FileOutputStream fos = new FileOutputStream(savePath);
   			byte[] buffer = new byte[32768];
   			int amt;
   			while ((amt = ios.read(buffer)) > 0) {
   				fos.write(buffer, 0, amt);
   			}
   			ios.close();
   			fos.close();
   			if ((new File(savePath)).exists())
   				Runtime.getRuntime().exec("chmod 755 " + savePath).waitFor();
   			Log.e(TAG, "saveLockScreenPic end");
   		} catch (Exception e) {
   		}
   		
   	}
}
