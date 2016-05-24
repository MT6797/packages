package com.mediatek.providers.contacts;

import android.test.InstrumentationTestRunner;

import com.android.providers.contacts.CallLogInsertionHelperTest;
import com.android.providers.contacts.CallLogProviderTest;
import com.android.providers.contacts.VoicemailCleanupServiceTest;
import com.android.providers.contacts.VoicemailProviderTest;
import com.mediatek.providers.contacts.dialersearchtestcase.DialerSearchTestSuite;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * M: Add for running the CallLog related modules tests.
 * Include CallLogProvider, VoicemailProvider, DialerSearch.
 */
public class CallLogTestRunner extends InstrumentationTestRunner{

    @Override
    public TestSuite getAllTests() {
        TestSuite suite = new TestSuite();
        suite.addTest(getCallLogProviderTestSuite());
        suite.addTest(getVoicemailProviderTestSuite());
        suite.addTest(DialerSearchTestSuite.suite());
        return suite;
    }

    private Test getCallLogProviderTestSuite() {
        TestSuite suite = new TestSuite("CallLogProvider TestSuite");
        suite.addTestSuite(CallLogProviderTest.class);
        suite.addTestSuite(CallLogInsertionHelperTest.class);
        return suite;
    }

    private Test getVoicemailProviderTestSuite() {
        TestSuite suite = new TestSuite("VoicemailProvider TestSuite");
        suite.addTestSuite(VoicemailCleanupServiceTest.class);
        suite.addTestSuite(VoicemailProviderTest.class);
        return suite;
    }
}
