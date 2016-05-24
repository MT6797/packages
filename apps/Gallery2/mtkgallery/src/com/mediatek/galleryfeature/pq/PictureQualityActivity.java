package com.mediatek.galleryfeature.pq;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.gallery3d.R;
import com.mediatek.galleryfeature.pq.PresentImage.RenderingRequestListener;
import com.mediatek.galleryfeature.pq.adapter.PQDataAdapter;
import com.mediatek.galleryframework.util.MtkLog;

/**
 * PictureQualityActivity is for tuning PQ parameter, and display the bitmap
 * that add PQ effect.
 */
public class PictureQualityActivity extends Activity implements
        RenderingRequestListener {
    private static final String TAG = "MtkGallery2/PictureQualityActivity";
    public static final String ACTION_PQ = "android.media.action.PQ";
    private static final int TOAST_DISPLAY_DELAY = 2000;
    public static final int ITEM_HEIGHT = 85;
    public static float sDensity = 1.0f;
    private ImageView mImageView;
    private ListView mListView;
    private PQDataAdapter mAdapter;
    private int mHeight;
    private String mUri = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        Bundle bundle = ((Activity) this).getIntent().getExtras();
        if (bundle != null) {
            mUri = bundle.getString("PQUri");
        }
        mHeight = getViewHeight();
        setContentView(R.layout.m_pq_main);

        mImageView = (ImageView) findViewById(R.id.m_imageview);
        mListView = (ListView) findViewById(R.id.m_getInfo);
        try {
            mAdapter = new PQDataAdapter((Context) this, mUri);
        } catch (java.lang.UnsatisfiedLinkError e) {
            MtkLog.d(TAG, "<onCreate> PictureQualityActivity onCreate issue!");
            Toast toast = Toast.makeText(this,
                    "UnsatisfiedLinkError Please Check!!", TOAST_DISPLAY_DELAY);
            toast.show();
            e.printStackTrace();
            finish();
        } catch (java.lang.NoClassDefFoundError e) {
            MtkLog.d(TAG, "<onCreate>PictureQualityActivity onCreate issue!");
            Toast toast = Toast.makeText(this,
                    "NoClassDefFoundError Please Check!!", TOAST_DISPLAY_DELAY);
            toast.show();
            e.printStackTrace();
            finish();
        }

        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        sDensity = displayMetrics.density;
        MtkLog.d(TAG, "<onCreate>sDensity=" + sDensity);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.m_pq_actionbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.cancel) {
            recoverIndex();
        }
        finish();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        recoverIndex();
    }

    @Override
    public void onResume() {
        super.onResume();
        mListView.setAdapter(mAdapter);
        if (mAdapter != null) {
            mAdapter.setListView(mListView);
            mAdapter.onResume();
        }
        setListViewHeightBasedOnChildren(mListView);
        PresentImage.getPresentImage().setListener(this, this);
        PresentImage.getPresentImage().loadBitmap(mUri);
    }

    @Override
    public void onPause() {
        super.onPause();
        PresentImage.getPresentImage().stopLoadBitmap();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAdapter != null) {
            mAdapter.onDestroy();
        }
        releseResource();
    }

    /*
     * Get each item height for PQ bar.
     */
    public int getDefaultItemHeight() {
        return (int) (ITEM_HEIGHT * sDensity);
    }

    /**
     * Get action bar menu height.
     * @return menu height.
     */
    public int getActionBarHeight() {
        int actionBarHeight = this.getActionBar().getHeight();
        if (actionBarHeight != 0) {
            return actionBarHeight;
        }
        final TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize,
                tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,
                    getResources().getDisplayMetrics());
        }
        return actionBarHeight;
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        mHeight = getViewHeight();
        MtkLog.d(TAG, "<onConfigurationChanged>onConfigurationChanged  height="
                + mHeight);
        if (mListView != null) {
            setListViewHeightBasedOnChildren(mListView);
        }
        toggleStatusBarByOrientation();
    }

    /**
     * Call back by PresentImage, while success decode bitmap.
     * @param bitmap the bitmap for display.
     * @param uri the uri of my current bitmap.
     * @return whether set image bitmap for ImageView or no.
     */
    public boolean available(Bitmap bitmap, String uri) {
        if (mUri != null && mUri == uri) {
            mImageView.setImageBitmap(bitmap);
            return true;
        } else {
            bitmap.recycle();
            bitmap = null;
            return false;
        }
    }

    private void recoverIndex() {
        if (mAdapter != null) {
            mAdapter.restoreIndex();
        }
    }

    private void releseResource() {
        mImageView.setImageBitmap(null);
        PresentImage.getPresentImage().free();
    }

    private void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View list = listAdapter.getView(i, null, null);
            list.measure(0, 0);
        }
        int start = 0;
        int height = mHeight;
        start = getActionBarHeight();
        height = mHeight - 2 * getActionBarHeight();
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        ((MarginLayoutParams) params).setMargins(0, start, 0, 0);
        ((MarginLayoutParams) params).height = height;
        listView.setLayoutParams(params);
    }

    private int getViewHeight() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }

    private void toggleStatusBarByOrientation() {
        Window win = getWindow();
        if (null == win) {
            return;
        }
        win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

}
