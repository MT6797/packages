package com.android.soundrecorder.tests;

import junit.framework.TestSuite;
import android.test.InstrumentationTestRunner;
import android.test.InstrumentationTestSuite;
public class SoundRecorderStressTestRunner extends InstrumentationTestRunner {
    @Override
    public TestSuite getAllTests() {
        InstrumentationTestSuite suite = new InstrumentationTestSuite(this);
        suite.addTestSuite(SoundRecorderStressTest.class);
        return suite;
    }

    @Override
    public ClassLoader getLoader() {
        return SoundRecorderStressTestRunner.class.getClassLoader();
    }
}
