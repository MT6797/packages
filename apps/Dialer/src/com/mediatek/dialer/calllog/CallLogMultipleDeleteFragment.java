/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2011. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 */

package com.mediatek.dialer.calllog;

import java.util.ArrayList;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;

import com.android.common.io.MoreCloseables;
import com.android.dialer.R;
import com.android.contacts.common.GeoUtil;
import com.android.dialer.calllog.CallLogAdapter;
import com.android.dialer.calllog.CallLogListItemViewHolder;
import com.android.dialer.calllog.CallLogQueryHandler;
import com.android.dialer.calllog.ContactInfoHelper;

import com.mediatek.contacts.util.VvmUtils;
import com.mediatek.dialer.activities.CallLogMultipleDeleteActivity;
import com.mediatek.dialer.util.DialerConstants;

/**
 * M: Add for[Multi-Delete], Displays a list of call log entries.
 */
public class CallLogMultipleDeleteFragment extends Fragment implements
                    CallLogQueryHandler.Listener, CallLogAdapter.CallFetcher {
    private static final String TAG = "CallLogMultipleDeleteFragment";

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private CallLogMultipleDeleteAdapter mAdapter;
    private CallLogQueryHandler mCallLogQueryHandler;
    private boolean mScrollToTop;
    private ProgressDialog mProgressDialog;
    //TODO private int mCallLogMultipleChoiceTypeFilter = Constants.FILTER_TYPE_UNKNOWN;

    /**
     * The OnClickListener used to select or unselect a call log entry.
     */
    private final View.OnClickListener mItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            CallLogListItemViewHolder viewHolder = (CallLogListItemViewHolder) v.getTag();

            if (viewHolder == null) {
                return;
            }
            log("onListItemClick: position:" + viewHolder.getAdapterPosition());

            CheckBox checkBox = (CheckBox) viewHolder.rootView.findViewById(R.id.checkbox);
            if (null != checkBox) {
                boolean isChecked = checkBox.isChecked();
                ((CallLogMultipleDeleteActivity) getActivity()).updateSelectedItemsView(mAdapter
                        .changeSelectedStatusToMap(viewHolder.getAdapterPosition()));
                checkBox.setChecked(!isChecked);
            }

        }
    };

    @Override
    public void onCreate(Bundle state) {
        log("onCreate()");
        super.onCreate(state);

        ContentResolver cr = getActivity().getContentResolver();
        mCallLogQueryHandler = new CallLogQueryHandler(getActivity(), cr, this);
        cr.registerContentObserver(CallLog.CONTENT_URI, true,
                mCallLogObserver);
        cr.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI, true, mContactsObserver);
    }

    /** Called by the CallLogQueryHandler when the list of calls has been fetched or updated. */
    @Override
    public boolean onCallsFetched(Cursor cursor) {
        log("onCallsFetched(), cursor = " + cursor);
        if (getActivity() == null || getActivity().isFinishing()) {
            if (null != cursor) {
                cursor.close();
            }
            return false;
        }
        mAdapter.setLoading(false);
        mAdapter.changeCursor(cursor);

        if (mScrollToTop) {
            // The smooth-scroll animation happens over a fixed time period.
            // As a result, if it scrolls through a large portion of the list,
            // each frame will jump so far from the previous one that the user
            // will not experience the illusion of downward motion.  Instead,
            // if we're not already near the top of the list, we instantly jump
            // near the top, and animate from there.
            if (mLayoutManager.findFirstVisibleItemPosition() > 5) {
                // TODO: Jump to near the top, then begin smooth scroll.
                mRecyclerView.smoothScrollToPosition(0);
            }
            // Workaround for framework issue: the smooth-scroll doesn't
            // occur if setSelection() is called immediately before.
            mHandler.post(new Runnable() {
               @Override
               public void run() {
                   if (getActivity() == null || getActivity().isFinishing()) {
                       return;
                   }
                   mRecyclerView.smoothScrollToPosition(0);
               }
            });

            mScrollToTop = false;
        }
        return true;
    }

    /** Called by the CallLogQueryHandler when the list of calls has been fetched or updated. */
    @Override
    public void onCallsDeleted() {
        log("onCallsDeleted()");
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        if (null != mProgressDialog) {
            mProgressDialog.dismiss();
        }
        getActivity().finish();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        log("onCreateView()");
        View view = inflater.inflate(R.layout.mtk_call_log_multiple_delete_fragment,
                                     container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        String currentCountryIso = GeoUtil.getCurrentCountryIso(getActivity());
        mAdapter = new CallLogMultipleDeleteAdapter(getActivity(), this,
                new ContactInfoHelper(getActivity(), currentCountryIso));
        mAdapter.setItemClickListener(mItemClickListener);
        mRecyclerView.setAdapter(mAdapter);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        log("onViewCreated()");
        super.onViewCreated(view, savedInstanceState);
        refreshData();

        /** M: Fix CR ALPS01569024. Restore selected call log. @{ */
        mAdapter.setSelectedItemChangeListener(mSelectedItemChangeListener);
        if (savedInstanceState != null) {
            ArrayList<Integer> idList = savedInstanceState.getIntegerArrayList(SELECT_ITEM_IDS);
            if (idList != null) {
                mAdapter.setSelectedCallLogIds(idList);
            }
        }
        /** @} */

    }

    @Override
    public void onStart() {
        mScrollToTop = true;
        super.onStart();
    }

    @Override
    public void onDestroy() {
        log("onDestroy");
        super.onDestroy();
        /** M: Fix CR ALPS01569024. Remove the SelectedItemChangeListener. @{ */
        mAdapter.setSelectedItemChangeListener(null);
        /** @} */
        mAdapter.changeCursor(null);

        getActivity().getContentResolver().unregisterContentObserver(mCallLogObserver);
        getActivity().getContentResolver().unregisterContentObserver(mContactsObserver);
    }

    @Override
    public void fetchCalls() {
        /// do nothing
    }

    /**
     * M: start call log query
     */
    public void startCallsQuery() {
        mAdapter.setLoading(true);
        Intent intent = this.getActivity().getIntent();
        int callType = intent.getIntExtra(CallLogQueryHandler.CALL_LOG_TYPE_FILTER,
                CallLogQueryHandler.CALL_TYPE_ALL);
        // TODO:: for global search
        // For deleting calllog from global search
        if (intent.getBooleanExtra(DialerConstants.BUNDLE_GLOBAL_SEARCH, false)) {
            String data = intent.getStringExtra(SearchManager.USER_QUERY);
            log("Is google search mode, startCallsQuery() data==" + data);
            Uri uri = Uri.withAppendedPath(DialerConstants.CALLLOG_SEARCH_URI_BASE, data);
            /// M: ALPS01903212 support search Voicemail calllog
            uri = VvmUtils.buildVvmAllowedUri(uri);
            mCallLogQueryHandler.fetchSearchCalls(uri);
        } else {
            mCallLogQueryHandler.fetchCalls(callType, 0);
        }
    }

    /**
     * get delete selection
     * @return delete selection
     */
    public String getSelections() {
        return mAdapter.getDeleteFilter();
    }

    /** Requests updates to the data to be shown. */
    public void refreshData() {
        log("refreshData()");
        startCallsQuery();
    }

    /**
     * set all item selected
     * @return selected count
     */
    public int selectAllItems() {
        int iCount = mAdapter.selectAllItems();
        mAdapter.notifyDataSetChanged();
        return iCount;
    }

    /**
     * cancel select all items
     */
    public void unSelectAllItems() {
        mAdapter.unSelectAllItems();
        mAdapter.notifyDataSetChanged();
    }

    /**
     * delete selected call log items
     */
    public void deleteSelectedCallItems() {
        if (mAdapter.getSelectedItemCount() > 0) {
            mProgressDialog = ProgressDialog.show(getActivity(), "",
                    getString(R.string.deleting_call_log));
        }
        mCallLogQueryHandler.deleteSpecifiedCalls(mAdapter.getDeleteFilter());
    }

    /**
     * to do nothing
     * @param statusCursor cursor
     */
    public void onVoicemailStatusFetched(Cursor statusCursor) {
        /// M: ALPS01260098 @{
        // Cursor Leak check.
        MoreCloseables.closeQuietly(statusCursor);
        /// @}
    }

    /**
     * get selected item count
     * @return count
     */
    public int getSelectedItemCount() {
        return mAdapter.getSelectedItemCount();
    }

    /**
     * Get the count of the call log list items
     * @return the count of list items
     */
    public int getItemCount() {
        return mAdapter.getItemCount();
    }

    private void log(String log) {
        Log.i(TAG, log);
    }
    /**
     *
     * @return if all selected
     */
    public boolean isAllSelected() {
        // get total count of list items.
        int count = getItemCount();
        return count == getSelectedItemCount();
    }
    /// M: for ALPS00918795 @{
    // listen simInfo. After plug out SIM slot,simIndicator will be grey.
    private static final int SIM_INFO_UPDATE_MESSAGE = 100;
    private static final int MSG_DB_CHANGED = 101;

    private Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SIM_INFO_UPDATE_MESSAGE:
                    if (mAdapter != null) {
                        mAdapter.invalidateCache();
                    }
                    startCallsQuery();
                    break;
                case MSG_DB_CHANGED:
                    removeMessages(MSG_DB_CHANGED);
                    if (isResumed()) {
                        refreshData();
                    }
                    break;
                default:
                    break;
            }
        }
    };
    /// @}

    /// M: fix ALPS01524672, save checked states of list item @{
    private final static String KEY = "KEY";
    private final static String VALUE = "VALUE";
    /** M: Fix CR ALPS01569024. Define the SelectedItemChangeListener. @{ */
    private final static String SELECT_ITEM_IDS = "select_item_ids";
    private CallLogMultipleDeleteAdapter.SelectedItemChangeListener mSelectedItemChangeListener =
            new CallLogMultipleDeleteAdapter.SelectedItemChangeListener() {
        @Override
        public void onSelectedItemCountChange(int count) {
            if (getActivity() != null) {
                ((CallLogMultipleDeleteActivity) getActivity()).updateSelectedItemsView(count);
            }
        }
    };
    /** @} */

    @Override
    public void onSaveInstanceState(Bundle outState) {
        /** M: Fix CR ALPS01569024. Save the selected call log id list. @{ */
        outState.putIntegerArrayList(SELECT_ITEM_IDS, mAdapter.getSelectedCallLogIds());
        super.onSaveInstanceState(outState);
        /** @} */
    }
    /// @}

    private final ContentObserver mCallLogObserver = new CustomContentObserver();
    private final ContentObserver mContactsObserver = new CustomContentObserver();

    private class CustomContentObserver extends ContentObserver {

        public CustomContentObserver() {
            super(mHandler);
        }
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            mHandler.sendEmptyMessageDelayed(MSG_DB_CHANGED, 1000);
        }
    }
}
