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
package com.android.dialer.calllog;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.android.contacts.common.interactions.TouchPointManager;
import com.android.contacts.common.list.ViewPagerTabs;
import com.android.contacts.common.util.PermissionsUtil;
import com.android.contacts.commonbind.analytics.AnalyticsUtil;
import com.android.dialer.DialtactsActivity;
import com.android.dialer.R;
import com.android.dialer.voicemail.VoicemailStatusHelper;
import com.android.dialer.voicemail.VoicemailStatusHelperImpl;
import com.mediatek.dialer.activities.CallLogMultipleDeleteActivity;
import com.mediatek.dialer.ext.ExtensionManager;
import com.mediatek.dialer.ext.ICallLogAction;
import com.mediatek.dialer.util.DialerFeatureOptions;
import com.mediatek.dialer.calllog.PhoneAccountInfoHelper;
import com.mediatek.dialer.calllog.PhoneAccountInfoHelper.AccountInfoListener;
import com.mediatek.dialer.calllog.PhoneAccountPickerDialog;
import com.mediatek.dialer.util.DialerFeatureOptions;

import java.util.ArrayList;
import java.util.List;

public class CallLogActivity extends Activity implements
    ViewPager.OnPageChangeListener, /*M:*/ICallLogAction, /*M:*/AccountInfoListener {
    private Handler mHandler;
    private ViewPager mViewPager;
    private ViewPagerTabs mViewPagerTabs;
    private ViewPagerAdapter mViewPagerAdapter;
    private CallLogFragment mAllCallsFragment;
    private CallLogFragment mMissedCallsFragment;

    private CharSequence[] mTabTitles;

    private static final int TAB_INDEX_ALL = 0;
    /// M: [CallLog Incoming and Outgoing Filter] @{
    private static final boolean TAB_INCOMING_OUTGOING_ENABLE = DialerFeatureOptions
            .isCallLogIOFilterEnabled();
    private static final int TAB_INDEX_INCOMING = 1;
    private static final int TAB_INDEX_OUTGOING = 2;
    private static final int TAB_INDEX_MISSED = TAB_INCOMING_OUTGOING_ENABLE ? 3 : 1;
    private static final int TAB_INDEX_COUNT = TAB_INCOMING_OUTGOING_ENABLE ? 4 : 2;

    private CallLogFragment mIncomingCallsFragment;
    private CallLogFragment mOutgoingCallsFragment;
    /// @}

    private boolean mIsResumed;

    public class ViewPagerAdapter extends FragmentPagerAdapter {
        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            /* Original code
            switch (position) {
                case TAB_INDEX_ALL:
                    return new CallLogFragment(CallLogQueryHandler.CALL_TYPE_ALL);
                case TAB_INDEX_MISSED:
                    return new CallLogFragment(Calls.MISSED_TYPE);
            }
            */

            /** M: [CallLog Incoming and Outgoing Filter] @{ */
            if (position == TAB_INDEX_ALL) {
                return new CallLogFragment(CallLogQueryHandler.CALL_TYPE_ALL);
            } else if (position == TAB_INDEX_MISSED) {
                return new CallLogFragment(Calls.MISSED_TYPE);
            }else if (position == TAB_INDEX_INCOMING) {
                return new CallLogFragment(Calls.INCOMING_TYPE);
            } else if (position == TAB_INDEX_OUTGOING) {
                return new CallLogFragment(Calls.OUTGOING_TYPE);
            }
            /** @} */
            throw new IllegalStateException("No fragment at position " + position);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            /// M: for RTL layout direction
            position = getRtlPosition(position);
            /// M: for Plug-in @{
            position = ExtensionManager.getInstance().getCallLogExtension().getPosition(position);
            /// @}
            final CallLogFragment fragment =
                    (CallLogFragment) super.instantiateItem(container, position);
            /* Original code
            switch (position) {
                case TAB_INDEX_ALL:
                    mAllCallsFragment = fragment;
                    break;
                case TAB_INDEX_MISSED:
                    mMissedCallsFragment = fragment;
                    break;
            }
            */

            /** M: [CallLog Incoming and Outgoing Filter] @{ */
            if (position == TAB_INDEX_ALL) {
                mAllCallsFragment = fragment;
            } else if (position == TAB_INDEX_MISSED) {
                mMissedCallsFragment = fragment;
            } else if (position == TAB_INDEX_INCOMING) {
                mIncomingCallsFragment = fragment;
            } else if (position == TAB_INDEX_OUTGOING) {
                mOutgoingCallsFragment = fragment;
            }
            /** @} */
            return fragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTabTitles[position];
        }

        @Override
        public int getCount() {
            /// M: for Plug-in
            int count = ExtensionManager.getInstance().getCallLogExtension()
                    .getTabCount(CallLogActivity.this, TAB_INDEX_COUNT);
            return count;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            TouchPointManager.getInstance().setPoint((int) ev.getRawX(), (int) ev.getRawY());
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /// M: for Plug-in
        /// 1.manage the hashmap @{
        ExtensionManager.getInstance().getCallLogExtension().onCreate(this, savedInstanceState);
        /// @}

        mHandler = new Handler();

        /// M: [CallLog Incoming and Outgoing Filter] @{
        if (TAB_INCOMING_OUTGOING_ENABLE) {
            setContentView(R.layout.mtk_call_log_activity);
        } else {
            setContentView(R.layout.call_log_activity);
        }
        /// @}
        getWindow().setBackgroundDrawable(null);

        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setElevation(0);

        int startingTab = TAB_INDEX_ALL;
        final Intent intent = getIntent();
        if (intent != null) {
            final int callType = intent.getIntExtra(CallLog.Calls.EXTRA_CALL_TYPE_FILTER, -1);
            if (callType == CallLog.Calls.MISSED_TYPE) {
                startingTab = TAB_INDEX_MISSED;
            }
        }

        mTabTitles = new CharSequence[TAB_INDEX_COUNT];
        mTabTitles[0] = getString(R.string.call_log_all_title);
        mTabTitles[1] = getString(R.string.call_log_missed_title);
        /// M: [CallLog Incoming and Outgoing Filter] @{
        if (TAB_INCOMING_OUTGOING_ENABLE) {
            initTabIcons();
        }
        /// @}

        mViewPager = (ViewPager) findViewById(R.id.call_log_pager);

        mViewPagerAdapter = new ViewPagerAdapter(getFragmentManager());
        mViewPager.setAdapter(mViewPagerAdapter);
        /// M: [CallLog Incoming and Outgoing Filter]
        /// Also increase page limit when calllog incoming/outgoing enabled @{
        if (TAB_INCOMING_OUTGOING_ENABLE) {
            mViewPager.setOffscreenPageLimit(3);
        } else {
            mViewPager.setOffscreenPageLimit(1);
        }
        /// @}
        mViewPager.setOnPageChangeListener(this);

        mViewPagerTabs = (ViewPagerTabs) findViewById(R.id.viewpager_header);

        mViewPagerTabs.setViewPager(mViewPager);
        /// M: for RTL layout direction
        startingTab = getRtlPosition(startingTab);
        mViewPager.setCurrentItem(startingTab);

        /** M: Fix CR ALPS01588898. Save and restore the fragments. @{ */
        restoreFragments(savedInstanceState);
        /** @} */
        /// M: [Call Log Account Filter] @{
        if (DialerFeatureOptions.isCallLogAccountFilterEnabled()) {
            PhoneAccountInfoHelper.getInstance(this).registerForAccountChange(this);
        }
        /// @}
    }

    @Override
    protected void onResume() {
        mIsResumed = true;
        super.onResume();
        sendScreenViewForChildFragment(mViewPager.getCurrentItem());
        /// M: for Op01 Plug-in reset the reject mode flag @{
        ExtensionManager.getInstance().getCallLogExtension().resetRejectMode(this);
        /// @}
    }

    @Override
    protected void onPause() {
        mIsResumed = false;
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.call_log_options, menu);

        /// M: for Plug-in
        ExtensionManager.getInstance().getCallLogExtension()
                .createCallLogMenu(this, menu, mViewPagerTabs, this);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final MenuItem itemDeleteAll = menu.findItem(R.id.delete_all);
        if (mAllCallsFragment != null && itemDeleteAll != null) {
            // If onPrepareOptionsMenu is called before fragments are loaded, don't do anything.
            final CallLogAdapter adapter = mAllCallsFragment.getAdapter();
            /** M: Fix CR ALPS01884065. The isEmpty() be overrided with loading state of data.
             *  Here, it should not care about the loading state. So, use getCount() to check
             *  is the adapter really empty. @{ */
            itemDeleteAll.setVisible(adapter != null && adapter.getItemCount() > 0);
        }
        /// M: [Multi-Delete] for CallLog multiple delete @{
        final MenuItem itemDelete = menu.findItem(R.id.delete);
        CallLogFragment fragment = getCurrentCallLogFragment();
        if (fragment != null && itemDelete != null) {
            final CallLogAdapter adapter = fragment.getAdapter();
            itemDelete.setVisible(DialerFeatureOptions.MULTI_DELETE
                    && adapter != null && adapter.getItemCount() > 0);
        }
        ///@}
        /// M :[Call Log Account Filter] @{
        // hide choose account menu if only one or no account
        final MenuItem itemChooseAccount = menu.findItem(R.id.select_account);
        if (DialerFeatureOptions.isCallLogAccountFilterEnabled()
                && itemChooseAccount != null) {
            if (mAllCallsFragment != null) {
                itemChooseAccount.setVisible(PhoneAccountUtils
                        .hasMultipleCallCapableAccounts(this));
            } else {
                itemChooseAccount.setVisible(false);
            }
        } else {
            if (itemChooseAccount != null) {
                itemChooseAccount.setVisible(false);
            }
        }
        /// @}

        /// M: for plug-in create the CallLogFilter menu@{
        ExtensionManager.getInstance().getCallLogExtension().prepareCallLogMenu(this,
                menu, fragment, itemDeleteAll,
                fragment != null ? fragment.getAdapter().getItemCount() : 0);
        ///@}
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                /// M: for Plug-in @{
                if (ExtensionManager.getInstance().getCallLogExtension().onHomeButtonClick(this,
                        mViewPagerAdapter, item)) {
                    return true;
                }
                /// @}

                final Intent intent = new Intent(this, DialtactsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.delete_all:
                ClearCallLogDialog.show(getFragmentManager());
                return true;
            /// M: [Multi-Delete] for CallLog multiple delete @{
            case R.id.delete:
                if (DialerFeatureOptions.MULTI_DELETE) {
                    final Intent delIntent = new Intent(this, CallLogMultipleDeleteActivity.class);
                    delIntent.putExtra(CallLogQueryHandler.CALL_LOG_TYPE_FILTER,
                            getCurrentCallLogFilteType());
                    startActivity(delIntent);
                }
                /// M: CR: ALPS01883767, Ensure menu item is invisible after clicking
                //  on delete item for some wired scene@{
                item.setVisible(false);
                /// @}
                return true;
                ///@}
            /// M :[Call Log Account Filter] @{
            case R.id.select_account:
                // phone account Selection, select an account to filter out the Calllog
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.add(new PhoneAccountPickerDialog(), PhoneAccountPickerDialog.TAG);
                // show dialog fragment with allowing state loss
                ft.commitAllowingStateLoss();
                return true;
            /// @}
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mViewPagerTabs.onPageScrolled(position, positionOffset, positionOffsetPixels);
    }

    @Override
    public void onPageSelected(int position) {
        /// M: for Plug-in @{
        ExtensionManager.getInstance().getCallLogExtension().setPosition(position);
        /// @}
        if (mIsResumed) {
            sendScreenViewForChildFragment(position);
        }
        mViewPagerTabs.onPageSelected(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        mViewPagerTabs.onPageScrollStateChanged(state);
    }

    private void sendScreenViewForChildFragment(int position) {
        AnalyticsUtil.sendScreenView(CallLogFragment.class.getSimpleName(), this,
                getFragmentTagForPosition(position));
    }

    /**
     * Returns the fragment located at the given position in the {@link ViewPagerAdapter}. May
     * be null if the position is invalid.
     */
    private String getFragmentTagForPosition(int position) {
        /* Original code
        switch (position) {
            case TAB_INDEX_ALL:
                return "All";
            case TAB_INDEX_MISSED:
                return "Missed";
        }
        */

        /** M: [CallLog Incoming and Outgoing Filter] @{ */
        position = getRtlPosition(position);
        if (position == TAB_INDEX_ALL) {
            return "All";
        } else if (position == TAB_INDEX_MISSED) {
            return "Missed";
        } else if (position == TAB_INDEX_INCOMING) {
            return "Incoming";
        } else if (position == TAB_INDEX_OUTGOING) {
            return "Outgoing";
        }
        /** @} */
        return null;
    }

    /** M: Fix CR ALPS01588898. Save and restore the fragments. @{ */
    private static final String TAG = "CallLogActivity";
    private static final String FRAGMENT_TAG_ALL = "fragment_tag_all";
    private static final String FRAGMENT_TAG_MISSED = "fragment_tag_missed";
    private String mAllCallsFragmentTag = null;
    private String mMissedCallsFragmentTag = null;
    /// [CallLog Incoming and Outgoing Filter]
    private static final String FRAGMENT_TAG_INCOMING = "fragment_tag_incoming";
    private static final String FRAGMENT_TAG_OUTGOING = "fragment_tag_outgoing";
    private String mIncomingCallsFragmentTag = null;
    private String mOutgoingCallsFragmentTag = null;
    /** @} */

    /** M: Fix CR ALPS01588898. Save and restore the fragments. @{ */
    private void restoreFragments(Bundle savedInstanceState) {
        Log.d(TAG, "restoreFragments savedInstanceState= " + savedInstanceState);
        if (savedInstanceState != null) {
            mAllCallsFragmentTag = savedInstanceState.getString(FRAGMENT_TAG_ALL, null);
            mMissedCallsFragmentTag = savedInstanceState.getString(FRAGMENT_TAG_MISSED, null);
        }
        if (mAllCallsFragment == null && mAllCallsFragmentTag != null) {
            mAllCallsFragment = (CallLogFragment) getFragmentManager()
                    .findFragmentByTag(mAllCallsFragmentTag);
            Log.d(TAG, "onResume findFragment all ~ " + mAllCallsFragment);
        }
        if (mMissedCallsFragment == null && mMissedCallsFragmentTag != null) {
            mMissedCallsFragment = (CallLogFragment) getFragmentManager()
                    .findFragmentByTag(mMissedCallsFragmentTag);
            Log.d(TAG, "onResume findFragment missed ~ " + mMissedCallsFragment);
        }
        /// M: [CallLog Incoming and Outgoing Filter] @{
        if (TAB_INCOMING_OUTGOING_ENABLE) {
            if (savedInstanceState != null) {
                mIncomingCallsFragmentTag = savedInstanceState
                        .getString(FRAGMENT_TAG_INCOMING, null);
                mOutgoingCallsFragmentTag = savedInstanceState
                        .getString(FRAGMENT_TAG_OUTGOING, null);
            }
            if (mIncomingCallsFragment == null && mIncomingCallsFragmentTag != null) {
                mIncomingCallsFragment = (CallLogFragment) getFragmentManager()
                        .findFragmentByTag(mIncomingCallsFragmentTag);
                Log.d(TAG, "onResume findFragment incoming ~ " + mIncomingCallsFragment);
            }
            if (mOutgoingCallsFragment == null && mOutgoingCallsFragmentTag != null) {
                mOutgoingCallsFragment = (CallLogFragment) getFragmentManager()
                        .findFragmentByTag(mOutgoingCallsFragmentTag);
                Log.d(TAG, "onResume findFragment outgoing ~ " + mOutgoingCallsFragment);
            }
        }
        ExtensionManager.getInstance().getCallLogExtension().restoreFragments(this,
                mViewPagerAdapter, mViewPagerTabs);
        /// @}
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAllCallsFragment != null) {
            outState.putString(FRAGMENT_TAG_ALL, mAllCallsFragment.getTag());
        }
        if (mMissedCallsFragment != null) {
            outState.putString(FRAGMENT_TAG_MISSED, mMissedCallsFragment.getTag());
        }
        /// M: [CallLog Incoming and Outgoing Filter] @{
        if (TAB_INCOMING_OUTGOING_ENABLE) {
            if (mIncomingCallsFragment != null) {
                outState.putString(FRAGMENT_TAG_INCOMING, mIncomingCallsFragment.getTag());
            }
            if (mOutgoingCallsFragment != null) {
                outState.putString(FRAGMENT_TAG_OUTGOING, mOutgoingCallsFragment.getTag());
            }
        }
        ExtensionManager.getInstance().getCallLogExtension().onSaveInstanceState(this, outState);
        /// @}
    }
    /** @} */

    /** M: [CallLog Incoming and Outgoing Filter] @{ */
    private void initTabIcons() {
        CallTypeIconsView.Resources resources = new CallTypeIconsView.Resources(this);
        mTabTitles[1] = createSpannableString(resources.incoming);
        mTabTitles[2] = createSpannableString(resources.outgoing);
        mTabTitles[3] = createSpannableString(resources.missed);
    }

    private SpannableString createSpannableString(Drawable drawable) {
        //Enlarge the icon by 1.5 times
        drawable.setBounds(0, 0, (drawable.getIntrinsicWidth() * 3) / 2,
                (drawable.getIntrinsicHeight() * 3) / 2);
        SpannableString sp = new SpannableString("i");
        ImageSpan iconsp = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
        sp.setSpan(iconsp, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        return sp;
    }
    /** @} */

    /**
     * M: [Multi-Delete] Get the current displaying call log fragment. @{
     */
    public CallLogFragment getCurrentCallLogFragment() {
        int position = mViewPager.getCurrentItem();
        position = getRtlPosition(position);
        /** M: For OP01 plugin @{ */
        position = ExtensionManager.getInstance().getCallLogExtension().getPosition(position);
        /** @} */
        if (position == TAB_INDEX_ALL) {
            return mAllCallsFragment;
        } else if (position == TAB_INDEX_MISSED) {
            return mMissedCallsFragment;
        } else if (position == TAB_INDEX_INCOMING) {
            return mIncomingCallsFragment;
        } else if (position == TAB_INDEX_OUTGOING) {
            return mOutgoingCallsFragment;
        }
        return null;
    }

    public int getCurrentCallLogFilteType() {
        int position = mViewPager.getCurrentItem();
        position = getRtlPosition(position);
        /** M: For OP01 plugin @{ */
        if (ExtensionManager.getInstance().getCallLogExtension().isAutoRejectMode()) {
            return Calls.AUTO_REJECT_TYPE;
        }
        /** @} */
        if (position == TAB_INDEX_ALL) {
            return CallLogQueryHandler.CALL_TYPE_ALL;
        } else if (position == TAB_INDEX_MISSED) {
            return Calls.MISSED_TYPE;
        } else if (position == TAB_INDEX_INCOMING) {
            return Calls.INCOMING_TYPE;
        } else if (position == TAB_INDEX_OUTGOING) {
            return Calls.OUTGOING_TYPE;
        }
        return CallLogQueryHandler.CALL_TYPE_ALL;
    }
    /** @} */

    /// M: for Plug-in @{
    @Override
    public void onBackPressed() {
        ExtensionManager.getInstance().getCallLogExtension().onBackPressed(this,
            mViewPagerAdapter, this);
    }

    @Override
    public void processBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void updateCallLogScreen() {
        mViewPagerAdapter.notifyDataSetChanged();
        mViewPagerTabs.setViewPager(mViewPager);
        /// M: Fix CR ALPS01969424. reset the adapter@{
        mViewPager.setAdapter(mViewPagerAdapter);
        /// @}

        if (mAllCallsFragment != null) {
            mAllCallsFragment.forceToRefreshData();
            if (!mAllCallsFragment.isVisible() && !mAllCallsFragment.isAdded()) {
                final FragmentTransaction ftAll = getFragmentManager().beginTransaction();
                ftAll.remove(mAllCallsFragment);
                ftAll.commit();
            }
        }

        if (mMissedCallsFragment != null) {
            mMissedCallsFragment.forceToRefreshData();
            if (!mMissedCallsFragment.isVisible() && !mMissedCallsFragment.isAdded()) {
                final FragmentTransaction ftMiss = getFragmentManager().beginTransaction();
                ftMiss.remove(mMissedCallsFragment);
                ftMiss.commit();
            }
        }
    }

    @Override
    protected void onDestroy() {
        /// M: for Plug-in
        /// 1.manage the hashmap
        /// 2.[Call Log Account Filter] unregister account change listener @{
        if (DialerFeatureOptions.isCallLogAccountFilterEnabled()) {
            PhoneAccountInfoHelper.getInstance(this).unRegisterForAccountChange(this);
        }
        ExtensionManager.getInstance().getCallLogExtension().onDestroy(this);
        /// @}
        super.onDestroy();
    }

    @Override
    public void onAccountInfoUpdate() {
        invalidateOptionsMenu();
    }

    @Override
    public void onPreferAccountChanged(String id) {
        // do nothing
    }

    /**
     * M: Returns the layout direction related position for this view.
     *
     * @param position LTR position
     * @return if the layout direction is RTL, return the reverse position
     */
    private int getRtlPosition(int position) {
        if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            return mViewPagerAdapter.getCount() - 1 - position;
        }
        return position;
    }
}
