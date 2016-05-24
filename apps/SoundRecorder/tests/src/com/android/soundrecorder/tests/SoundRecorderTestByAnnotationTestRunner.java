package com.android.soundrecorder.tests;

import junit.framework.TestSuite;
import android.test.InstrumentationTestRunner;
import android.test.InstrumentationTestSuite;

public class SoundRecorderTestByAnnotationTestRunner extends InstrumentationTestRunner {
    @Override
    public TestSuite getAllTests() {
        InstrumentationTestSuite suite = new InstrumentationTestSuite(this);
        suite.addTestSuite(SoundRecorderTestByAnnotation.class);
        suite.addTestSuite(SoundRecorderTest.class);
        return suite;
    }

    @Override
    public ClassLoader getLoader() {
        return SoundRecorderTestByAnnotationTestRunner.class.getClassLoader();
    }
}
