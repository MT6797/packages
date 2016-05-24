package com.mediatek.phone.basic;

import java.util.List;

import com.android.internal.telephony.PhoneConstants;
import com.android.phone.CallFeaturesSetting;
import com.android.phone.GsmUmtsAdditionalCallOptions;
import com.android.phone.GsmUmtsCallForwardOptions;
import com.android.phone.MobileNetworkSettings;
import com.android.phone.NetworkSetting;
import com.android.phone.settings.fdn.FdnList;
import com.android.phone.settings.fdn.FdnSetting;
import com.mediatek.settings.CallBarring;
import com.mediatek.settings.IpPrefixPreference;
import com.mediatek.settings.PLMNListPreference;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

public class GeneralUITest extends ActivityInstrumentationTestCase2<CallFeaturesSetting> {
    private static final String TAG = "GeneralUITest";
    private static final String TEST_PACKAGE = "com.android.phone";
    // Extra on intent containing the id of a subscription.
    private static final String SUB_ID_EXTRA =
            "com.android.phone.settings.SubscriptionInfoHelper.SubscriptionId";
    // Extra on intent containing the label of a subscription.
    private static final String SUB_LABEL_EXTRA =
            "com.android.phone.settings.SubscriptionInfoHelper.SubscriptionLabel";
    private Context mContext;
    private Instrumentation mInst;
    private SubscriptionInfo mSubscriptionInfo;

    public GeneralUITest() {
        super(CallFeaturesSetting.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInst = getInstrumentation();
        mContext = mInst.getTargetContext();
        final Context context = mContext;
        TestUtils.waitUntil(TAG, new TestUtils.Condition() {
            @Override
            public boolean isMet() {
                return SubscriptionManager.from(context).getActiveSubscriptionInfoCount() > 0;
            }
        }, 5, 200);
        List<SubscriptionInfo> subs =
                SubscriptionManager.from(mContext).getActiveSubscriptionInfoList();
        Log.d(TAG, "setup: " + (subs == null ? "null" : subs.size()));
        if (subs != null && subs.size() > 0) {
            mSubscriptionInfo = subs.get(0);
        }
    }

    public void testLaunchCallFeaturesSetting() {
        assertNotNull(mSubscriptionInfo);
        Intent intent = new Intent();
        intent.setAction(TelecomManager.ACTION_SHOW_CALL_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(SUB_ID_EXTRA, mSubscriptionInfo.getSubscriptionId());
        ///Add for CallSettings inner activity pass subid to other activity.
        intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, mSubscriptionInfo.getSubscriptionId());
        intent.putExtra(SUB_LABEL_EXTRA, mSubscriptionInfo.getDisplayName().toString());
        setActivityIntent(intent);
        CallFeaturesSetting setting = null;
        try {
            setting = getActivity();
            final CallFeaturesSetting fSetting = setting;
            mInst.waitForIdleSync();
            TestUtils.waitUntil(TAG, new TestUtils.Condition() {
                @Override
                public boolean isMet() {
                    return fSetting.isResumed();
                }
            }, 5, 200);
            assertTrue(setting.isResumed());
        } finally {
            if (setting != null && ! setting.isFinishing()) {
                setting.finish();
                mInst.waitForIdleSync();
            }
        }
    }

    public void testCallForwardSetting() {
        assertNotNull(mSubscriptionInfo);
        Intent intent = new Intent();
        intent.putExtra(SUB_ID_EXTRA, mSubscriptionInfo.getSubscriptionId());
        Activity activity = null;
        try {
            activity = launchActivityWithIntent(
                    TEST_PACKAGE, GsmUmtsCallForwardOptions.class, intent);
            final Activity fActivity = activity;
            setActivity(activity);
            mInst.waitForIdleSync();
            TestUtils.waitUntil(TAG, new TestUtils.Condition() {
                @Override
                public boolean isMet() {
                    return fActivity.isResumed();
                }
            }, 5, 500);
        } finally {
            if (activity != null && ! activity.isFinishing()) {
                activity.finish();
                mInst.waitForIdleSync();
            }
        }
    }

    public void testMobileNetworkSettings() {
        assertNotNull(mSubscriptionInfo);
        Activity activity = null;
        try {
            activity = launchActivity(
                    TEST_PACKAGE, MobileNetworkSettings.class, null);
            final Activity fActivity = activity;
            setActivity(activity);
            mInst.waitForIdleSync();
            TestUtils.waitUntil(TAG, new TestUtils.Condition() {
                @Override
                public boolean isMet() {
                    return fActivity.isResumed();
                }
            }, 5, 500);
            assertTrue(fActivity.isResumed());
        } finally {
            if (activity != null && ! activity.isFinishing()) {
                activity.finish();
                mInst.waitForIdleSync();
            }
        }
    }

    public void testPLMN() {
        assertNotNull(mSubscriptionInfo);
        Intent intent = new Intent();
        intent.putExtra(SUB_ID_EXTRA, mSubscriptionInfo.getSubscriptionId());
        Activity activity = null;
        try {
            activity = launchActivityWithIntent(
                    TEST_PACKAGE, PLMNListPreference.class, intent);
            final Activity fActivity = activity;
            setActivity(activity);
            mInst.waitForIdleSync();
            TestUtils.waitUntil(TAG, new TestUtils.Condition() {
                @Override
                public boolean isMet() {
                    return fActivity.isResumed();
                }
            }, 5, 500);
            assertTrue(fActivity.isResumed());
        } finally {
            if (activity != null && ! activity.isFinishing()) {
                activity.finish();
                mInst.waitForIdleSync();
            }
        }
    }

    public void testIPPrefix() {
        assertNotNull(mSubscriptionInfo);
        Intent intent = new Intent();
        intent.putExtra(SUB_ID_EXTRA, mSubscriptionInfo.getSubscriptionId());
        Activity activity = null;
        try {
            activity = launchActivityWithIntent(
                    TEST_PACKAGE, IpPrefixPreference.class, intent);
            final Activity fActivity = activity;
            setActivity(activity);
            mInst.waitForIdleSync();
            TestUtils.waitUntil(TAG, new TestUtils.Condition() {
                @Override
                public boolean isMet() {
                    return fActivity.isResumed();
                }
            }, 5, 500);
            assertTrue(fActivity.isResumed());
        } finally {
            if (activity != null && ! activity.isFinishing()) {
                activity.finish();
                mInst.waitForIdleSync();
            }
        }
    }

    public void testFdnSetting() {
        assertNotNull(mSubscriptionInfo);
        Intent intent = new Intent();
        intent.putExtra(SUB_ID_EXTRA, mSubscriptionInfo.getSubscriptionId());
        Activity activity = null;
        try {
            activity = launchActivityWithIntent(
                    TEST_PACKAGE, FdnSetting.class, intent);
            final Activity fActivity = activity;
            setActivity(activity);
            mInst.waitForIdleSync();
            TestUtils.waitUntil(TAG, new TestUtils.Condition() {
                @Override
                public boolean isMet() {
                    return fActivity.isResumed();
                }
            }, 5, 500);
            assertTrue(fActivity.isResumed());
        } finally {
            if (activity != null && ! activity.isFinishing()) {
                activity.finish();
                mInst.waitForIdleSync();
            }
        }
    }

    public void testFdnlist() {
        assertNotNull(mSubscriptionInfo);
        Intent intent = new Intent();
        intent.putExtra(SUB_ID_EXTRA, mSubscriptionInfo.getSubscriptionId());
        Activity activity = null;
        try {
            activity = launchActivityWithIntent(
                    TEST_PACKAGE, FdnList.class, intent);
            final Activity fActivity = activity;
            setActivity(activity);
            mInst.waitForIdleSync();
            TestUtils.waitUntil(TAG, new TestUtils.Condition() {
                @Override
                public boolean isMet() {
                    return fActivity.isResumed();
                }
            }, 5, 500);
        } finally {
            if (activity != null && ! activity.isFinishing()) {
                activity.finish();
                mInst.waitForIdleSync();
            }
        }
    }

    public void testCallBarring() {
        assertNotNull(mSubscriptionInfo);
        Intent intent = new Intent();
        intent.putExtra(SUB_ID_EXTRA, mSubscriptionInfo.getSubscriptionId());
        Activity activity = null;
        try {
            activity = launchActivityWithIntent(
                    TEST_PACKAGE, CallBarring.class, intent);
            final Activity fActivity = activity;
            setActivity(activity);
            mInst.waitForIdleSync();
            TestUtils.waitUntil(TAG, new TestUtils.Condition() {
                @Override
                public boolean isMet() {
                    return fActivity.isResumed();
                }
            }, 5, 500);
        } finally {
            if (activity != null && ! activity.isFinishing()) {
                activity.finish();
                mInst.waitForIdleSync();
            }
        }
    }

    public void testAdditionalSetting() {
        assertNotNull(mSubscriptionInfo);
        Intent intent = new Intent();
        intent.putExtra(SUB_ID_EXTRA, mSubscriptionInfo.getSubscriptionId());
        Activity activity = null;
        try {
            activity = launchActivityWithIntent(
                    TEST_PACKAGE, GsmUmtsAdditionalCallOptions.class, intent);
            final Activity fActivity = activity;
            setActivity(activity);
            mInst.waitForIdleSync();
            TestUtils.waitUntil(TAG, new TestUtils.Condition() {
                @Override
                public boolean isMet() {
                    return fActivity.isResumed();
                }
            }, 5, 500);
        } finally {
            if (activity != null && ! activity.isFinishing()) {
                activity.finish();
                mInst.waitForIdleSync();
            }
        }
    }

    public void testNetworkSetting() {
        assertNotNull(mSubscriptionInfo);
        Intent intent = new Intent();
        intent.putExtra(SUB_ID_EXTRA, mSubscriptionInfo.getSubscriptionId());
        Activity activity = null;
        try {
            activity = launchActivityWithIntent(
                    TEST_PACKAGE, NetworkSetting.class, intent);
            final Activity fActivity = activity;
            setActivity(activity);
            mInst.waitForIdleSync();
            TestUtils.waitUntil(TAG, new TestUtils.Condition() {
                @Override
                public boolean isMet() {
                    return fActivity.isResumed();
                }
            }, 5, 500);
        } finally {
            if (activity != null && ! activity.isFinishing()) {
                activity.finish();
                mInst.waitForIdleSync();
            }
        }
    }
}
