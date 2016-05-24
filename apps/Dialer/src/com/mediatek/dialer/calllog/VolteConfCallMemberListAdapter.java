/* Copyright Statement:
*
* This software/firmware and related documentation ("MediaTek Software") are
* protected under relevant copyright laws. The information contained herein
* is confidential and proprietary to MediaTek Inc. and/or its licensors.
* Without the prior written permission of MediaTek inc. and/or its licensors,
* any reproduction, modification, use or disclosure of MediaTek Software,
* and information contained herein, in whole or in part, shall be strictly prohibited.
*
* MediaTek Inc. (C) 2014. All rights reserved.
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

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.dialer.R;
import com.android.dialer.calllog.CallDetailHistoryAdapter;
import com.android.dialer.calllog.CallLogAdapter;
import com.android.dialer.calllog.CallLogListItemViewHolder;
import com.android.dialer.calllog.CallLogQuery;
import com.android.dialer.calllog.ContactInfoHelper;
import com.android.dialer.calllog.CallLogAdapter.CallFetcher;
import com.android.dialer.util.DialerUtils;

import java.util.ArrayList;

/**
 * M: [VoLTE ConfCall] The Volte Conference call member list adapter
 */
public class VolteConfCallMemberListAdapter extends CallLogAdapter {
    private final static String TAG = "VolteConfCallMemberListAdapter";
    private final static int VIEW_TYPE_CALL_HISTORY_LIST_ITEM_HEADER = 50;
    private final static int VIEW_TYPE_CALL_HISTORY_LIST_ITEM = 51;

    private CallDetailHistoryAdapter mCallDetailHistoryAdapter;

    public VolteConfCallMemberListAdapter(Context context,
            ContactInfoHelper contactInfoHelper) {
        super(context, new CallFetcher() {
            @Override
            public void fetchCalls() {
                // Do nothings
            }
        }, contactInfoHelper, null, false);
        setIsConfCallMemberList(true);
    }

    /**
     * For show the call history list item views. Only one conference call history item
     * @param adapter the CallDetailHistoryAdapter
     */
    public void setCallDetailHistoryAdapter(CallDetailHistoryAdapter adapter) {
        mCallDetailHistoryAdapter = adapter;
    }

    @Override
    protected void addGroups(Cursor cursor) {
        //Do nothing, no need to group the member list
    }

    @Override
    public int getItemCount() {
        // For conference call history list header and item views
        return super.getItemCount() + 2;
    }

    @Override
    public int getItemViewType(int position) {
        // Conference call history list header and item views
        if (position == getItemCount() - 1) {
            return VIEW_TYPE_CALL_HISTORY_LIST_ITEM;
        } else if (position == getItemCount() - 2) {
            return VIEW_TYPE_CALL_HISTORY_LIST_ITEM_HEADER;
        }
        return super.getItemViewType(position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_CALL_HISTORY_LIST_ITEM_HEADER) {
            return CallHistoryViewHolder.createHeader(mContext, parent);
        } else if (viewType == VIEW_TYPE_CALL_HISTORY_LIST_ITEM) {
            return CallHistoryViewHolder.create(mContext, parent);
        }
        return super.onCreateViewHolder(parent, viewType);
    }

    @Override
    protected void bindCallLogListViewHolder(ViewHolder viewHolder, int position) {
        Log.d(TAG, "bindCallLogListViewHolder(), viewHolder = " + viewHolder
                + " position = " + position);
        // Conference call history list header and item views
        if (getItemViewType(position) == VIEW_TYPE_CALL_HISTORY_LIST_ITEM_HEADER) {
            return;
        } else if (getItemViewType(position) == VIEW_TYPE_CALL_HISTORY_LIST_ITEM
                && mCallDetailHistoryAdapter != null) {
            CallHistoryViewHolder holder = (CallHistoryViewHolder)viewHolder;
            mCallDetailHistoryAdapter.getView(1, holder.view, null);
            return;
        }
        super.bindCallLogListViewHolder(viewHolder, position);
        Cursor c = (Cursor) getItem(position);
        if (c == null) {
            return;
        }
        CallLogListItemViewHolder views = (CallLogListItemViewHolder) viewHolder;
        // Conference member list title
        if (position == 0) {
            views.dayGroupHeader.setVisibility(View.VISIBLE);
            views.dayGroupHeader.setText(R.string.conf_call_member_list);
        } else {
            views.dayGroupHeader.setVisibility(View.GONE);
        }
        long duration = c.getLong(CallLogQuery.DURATION);
        // Hide the account label
        views.phoneCallDetailsViews.callAccountLabel.setVisibility(View.GONE);
        // Add the "Missed" or "Answered"
        ArrayList<CharSequence> texts = new ArrayList<CharSequence>();
        texts.add(views.phoneCallDetailsViews.callLocationAndDate.getText());
        texts.add(mContext
                .getText(duration > 0 ? R.string.conf_call_participant_answered
                        : R.string.conf_call_participant_missed));
        views.phoneCallDetailsViews.callLocationAndDate.setText(DialerUtils
                .join(mContext.getResources(), texts));
    }

    // Call history list item view holder
    static class CallHistoryViewHolder extends RecyclerView.ViewHolder {
        public View view;

        private CallHistoryViewHolder(final Context context, View view) {
            super(view);
            this.view = view;
        }

        public static CallHistoryViewHolder create(Context context, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.call_detail_history_item, parent, false);
            return new CallHistoryViewHolder(context, view);
        }

        public static CallHistoryViewHolder createHeader(Context context, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.call_detail_history_header, parent, false);
            return new CallHistoryViewHolder(context, view);
        }
    }
}
