
package com.android.wallpaper.test;

import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.service.wallpaper.IWallpaperEngine;
import android.service.wallpaper.IWallpaperService;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;

import com.android.wallpaper.PreviewStubActivity;


public class NexusStubTest extends ActivityInstrumentationTestCase2<PreviewStubActivity> {

    private static final String TAG = "NexusStubTest";
    private static final String TARGET_PACKAGE = "com.android.wallpaper";
    private static final String TARGET_PREVIEW_CLASS = ".PreviewStubActivity";
    private static final String TARGET_WALLPAPER_CLASS = "com.android.wallpaper.nexus.NexusWallpaper";

    private static final String VAR_WALLPAPER_CONNECTION = "mWallpaperConnection";
    private static final String VAR_INTENT = "mIntent";
    private static final String VAR_IWALLPAPER_SERVICE = "mService";
    private static final String VAR_IWALLPAPER_ENGINE = "mEngine";
    private static final String VAR_CONNECTED = "mConnected";
    private static final String WALLPAPER_CONNECTION = "WallpaperConnection";

    private Context mContext;
    private Instrumentation mInstrumentation;
    private Intent mWallpaperIntent;
    private PreviewStubActivity mStubActivity;

    public NexusStubTest() {
        super(PreviewStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = getInstrumentation();
        mContext = mInstrumentation.getTargetContext();

        mWallpaperIntent = new Intent();
        mWallpaperIntent.setAction(Intent.ACTION_MAIN);
        mWallpaperIntent.setComponent(new ComponentName(TARGET_PACKAGE, TARGET_PREVIEW_CLASS));
        mWallpaperIntent.putExtra(PreviewStubActivity.PACKAGE_NAME, TARGET_PACKAGE);
        mWallpaperIntent.putExtra(PreviewStubActivity.CLASS_NAME, TARGET_WALLPAPER_CLASS);

        setActivityIntent(mWallpaperIntent);
        mStubActivity = getActivity();
    }

    @Override
    protected void tearDown() throws Exception {
        if (mStubActivity != null) {
            mStubActivity.finish();
            mStubActivity = null;
        }
        super.tearDown();
    }

    /**
     * Live wallpaper should be launched and binded to PreviewStubActivity.
     */
    @MediumTest
    public void testCase1StartLiveWallpaper() {
        Log.d("@M_" + TAG, "test_Case1_StartLiveWallpaper start: G4 = " + mStubActivity);
        assertNotNull("activity should be launched successfully", mStubActivity);
        assertNotNull(mInstrumentation);
        assertNotNull(mContext);

        // / using Reflection to check if Service was binded successfully. @{
        Class testClass = mStubActivity.getClass();
        Class mClassWallpaperConnectionClass = ReflectionHelper.getNonPublicInnerClass(
                mStubActivity.getClass(), WALLPAPER_CONNECTION);

        Object f1 = ReflectionHelper.getObjectValue(testClass, VAR_WALLPAPER_CONNECTION,
                mStubActivity);
        Intent intent = (Intent) ReflectionHelper.getObjectValue(mClassWallpaperConnectionClass,
                VAR_INTENT, f1);
        IWallpaperService wallpaperService = (IWallpaperService) ReflectionHelper.getObjectValue(
                mClassWallpaperConnectionClass, VAR_IWALLPAPER_SERVICE, f1);
        IWallpaperEngine wallpaperEngine = (IWallpaperEngine) ReflectionHelper.getObjectValue(
                mClassWallpaperConnectionClass, VAR_IWALLPAPER_ENGINE, f1);
        boolean connected = ReflectionHelper.getBooleanValue(mClassWallpaperConnectionClass,
                VAR_CONNECTED, f1);
        // / @}

        Log.d("@M_" + TAG, "WallpaperConnection f1 = " + f1);
        Log.d("@M_" + TAG, "mEngine = " + wallpaperEngine);
        Log.d("@M_" + TAG, "mWallpaperService = " + wallpaperService);

        assertEquals(TARGET_WALLPAPER_CLASS, intent.getComponent().getClassName());
        assertNotNull("WallpaperConnection should be not null", f1);
        assertNotNull("mEngine should not be null", wallpaperEngine);
        assertNotNull("mWallpaperService should be not null", wallpaperService);
        assertTrue(connected);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
