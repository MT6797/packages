package com.android.soundrecorder.tests;

import android.test.InstrumentationTestRunner;
import android.test.InstrumentationTestSuite;

import junit.framework.TestSuite;

public class SoundRecorderTestRunner extends InstrumentationTestRunner {
    @Override
    public TestSuite getAllTests() {
        InstrumentationTestSuite suite = new InstrumentationTestSuite(this);
        suite.addTestSuite(SoundRecorderTest.class);
        suite.addTestSuite(RecordingFileListTest.class);
        //suite.addTestSuite(SoundRecorderFromMMSTest.class);
        return suite;
    }

    @Override
    public ClassLoader getLoader() {
        return SoundRecorderTestRunner.class.getClassLoader();
    }
}
