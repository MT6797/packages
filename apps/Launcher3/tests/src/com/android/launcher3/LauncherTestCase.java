
package com.android.launcher3;

import android.test.AndroidTestCase;

/**
 * Base class that does Launcher specific setup.
 */
public class LauncherTestCase extends AndroidTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Mockito stuff.
        System.setProperty("dexmaker.dexcache", mContext.getCacheDir().getPath());
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
    }
}
