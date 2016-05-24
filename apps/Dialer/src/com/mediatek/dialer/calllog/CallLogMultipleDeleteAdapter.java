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

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.CheckBox;


import com.android.dialer.R;
import com.android.dialer.calllog.CallLogAdapter;
import com.android.dialer.calllog.CallLogListItemViewHolder;
import com.android.dialer.calllog.CallLogQuery;
import com.android.dialer.calllog.ContactInfoHelper;
import com.mediatek.dialer.activities.CallLogMultipleDeleteActivity;

import java.util.ArrayList;

/**
 * M: Add for [Multi-Delete] feature.
 */
public class CallLogMultipleDeleteAdapter extends CallLogAdapter {

    private static final String LOG_TAG = "CallLogMultipleDeleteAdapter";
    private Cursor mCursor;


    /** M: Fix CR ALPS01569024. Save the selected call log id list. @{ */
    private final ArrayList<Integer> mSelectedCallLogIdList = new ArrayList<Integer>();
    private int mSelectedItemCount = 0;
    // A listener to listen the selected item change
    public interface SelectedItemChangeListener {
        public void onSelectedItemCountChange(int count);
    }
    private SelectedItemChangeListener mSelectedItemChangeListener;

    /**
     * The OnClickListener used to expand or collapse the action buttons of a call log entry.
     */
    private View.OnClickListener mItemClickListener;
    /** @} */

    /**
     * Construct function
     *
     * @param context context
     * @param callFetcher Callfetcher
     * @param contactInfoHelper contactinfohelper
     * @param voicemailNumber voicemailNumber
     */
    public CallLogMultipleDeleteAdapter(Context context, CallFetcher callFetcher,
            ContactInfoHelper contactInfoHelper) {
        super(context, callFetcher, contactInfoHelper, null, false);
    }

    /**
     * @param cursor cursor
     */
    public void changeCursor(Cursor cursor) {
        log("changeCursor(), cursor = " + cursor);
        if (null != cursor) {
            log("cursor count = " + cursor.getCount());
        }
        if (mCursor != cursor) {
            mCursor = cursor;
        }
        super.changeCursor(cursor);

        /** M: Fix CR ALPS01569024. Reconcile the selected call log ids
         *  with the ids in the new cursor. @{ */
        reconcileSeletetedItems(cursor);
        if (mSelectedItemChangeListener != null) {
            mSelectedItemChangeListener.onSelectedItemCountChange(
                    mSelectedItemCount);
        }
        /** @} */
    }

    /**
     * Binds the view holder for the call log list item view.
     *
     * @param viewHolder The call log list item view holder.
     * @param position The position of the list item.
     */

    protected void bindCallLogListViewHolder(ViewHolder viewHolder, int position) {
        Cursor c = (Cursor) getItem(position);
        if (c == null) {
            return;
        }
        log("bindCallLogListViewHolder, cursor: " + c + " count: " + getGroupSize(c.getPosition()));

        /** M: Fix CR ALPS01569024. Use the call log id to identify the select item. @{ */
        boolean checkState = false;
        if (mSelectedCallLogIdList.size() > 0) {
            int cursorPosition = c.getPosition();
            for (int i = 0; i < getGroupSize(cursorPosition); i++) {
                if (!c.moveToPosition(cursorPosition + i)) {
                    continue;
                }
                if (mSelectedCallLogIdList.contains(c.getInt(CallLogQuery.ID))) {
                    checkState = true;
                    break;
                }
            }
            c.moveToPosition(cursorPosition);
        }
        /** @} */

        super.bindCallLogListViewHolder(viewHolder, position);

        // set clickable false
        final CallLogListItemViewHolder views = (CallLogListItemViewHolder) viewHolder;
        // hide the action views
        views.showActions(false);
        views.primaryActionView.setOnClickListener(mItemClickListener);
        // Disable the long press context menu
        views.primaryActionView.setOnCreateContextMenuListener(null);
        // Disable the quick contact view
        views.quickContactView.setEnabled(false);
        views.primaryActionButtonView.setVisibility(View.GONE);
        views.callLogEntryView.setAccessibilityDelegate(null);

        // add check box for call log item
        CheckBox checkBox = (CheckBox) views.rootView.findViewById(R.id.checkbox);
        if (checkBox == null) {
            final ViewStub stub = (ViewStub) views.rootView.findViewById(R.id.checkbox_container);
            View inflatedView = null;
            if (stub != null) {
                inflatedView = stub.inflate();
                checkBox = (CheckBox) inflatedView.findViewById(R.id.checkbox);
            }
        }
        if (checkBox != null) {
            checkBox.setFocusable(false);
            checkBox.setClickable(false);
            checkBox.setVisibility(View.VISIBLE);
            checkBox.setChecked(checkState);
        }

        /// M: for LandScape UI @{
        View selectedIcon = views.rootView.findViewById(R.id.selected_icon);
        if (selectedIcon != null) {
            selectedIcon.setVisibility(View.GONE);
        }
        /// @}
    }

    /**
     * select all items
     *
     * @return the selected items numbers
     */
    public int selectAllItems() {
        log("selectAllItems()");
        /** M: Fix CR ALPS01569024. Use the call log id to identify the select item. @{ */
        mSelectedItemCount = getItemCount();
        mSelectedCallLogIdList.clear();
        mCursor.moveToPosition(-1);
        while (mCursor.moveToNext()) {
            mSelectedCallLogIdList.add(mCursor.getInt(CallLogQuery.ID));
        }
        return mSelectedItemCount;
        /** @} */
    }

    /**
     * unselect all items
     */
    public void unSelectAllItems() {
        log("unSelectAllItems()");
        /** M: Fix CR ALPS01569024. Use the call log id to identify the select item. @{ */
        mSelectedItemCount = 0;
        mSelectedCallLogIdList.clear();
        /** @} */
    }

    /**
     * get delete filter
     *
     * @return the delete selection
     */
    public String getDeleteFilter() {
        log("getDeleteFilter()");
        StringBuilder where = new StringBuilder("_id in ");
        where.append("(");
        /** M: Fix CR ALPS01569024. Use the call log id to identify the select item. @{ */
        if (mSelectedCallLogIdList.size() > 0) {
            boolean isFirst = true;
            for (int id : mSelectedCallLogIdList) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    where.append(",");
                }
                where.append("\'");
                where.append(id);
                where.append("\'");
            }
        } else {
            where.append(-1);
        }
        /** @} */

        where.append(")");
        log("getDeleteFilter() where ==  " + where.toString());
        return where.toString();
    }

    /**
     * change selected status to map
     *
     * @param listPosition position to change
     * @return int
     */
    public int changeSelectedStatusToMap(final int listPosition) {
        log("changeSelectedStatusToMap()");
        int count = 0;
        if (isGroupHeader(listPosition)) {
            count = getGroupSize(listPosition);
        } else {
            count = 1;
        }

        /** M: Fix CR ALPS01569024. Use the call log id to identify the select item. @{ */
        Cursor cursor = (Cursor) getItem(listPosition);
        if (cursor == null) {
            return mSelectedItemCount;
        }
        int position = cursor.getPosition();
        int firstId = cursor.getInt(CallLogQuery.ID);
        boolean shouldSelected = false;
        if (mSelectedCallLogIdList.contains(firstId)) {
            shouldSelected = false;
            mSelectedItemCount--;
        } else {
            shouldSelected = true;
            mSelectedItemCount++;
        }
        for (int i = 0; i < count; i++) {
            if (!cursor.moveToPosition(position + i)) {
                continue;
            }
            int id = cursor.getInt(CallLogQuery.ID);
            if (shouldSelected) {
                mSelectedCallLogIdList.add(id);
            } else {
                mSelectedCallLogIdList.remove((Integer) id);
            }
        }
        cursor.moveToPosition(position);
        return mSelectedItemCount;
        /** @} */
    }

    /**
     * get selected items count
     *
     * @return the count of selected
     */
    public int getSelectedItemCount() {
        log("getSelectedItemCount()");
        /// M: Fix CR ALPS01569024. Use the call log id to identify the select item.
        return mSelectedItemCount;
    }

    private void log(final String log) {
        Log.d(LOG_TAG, log);
    }

    /** M: Fix CR ALPS01569024. @{ */
    /**
     * Get the id list of selected call log.
     * @return the id list of selected call log.
     */
    public ArrayList<Integer> getSelectedCallLogIds() {
        return new ArrayList<Integer>(mSelectedCallLogIdList);
    }

    /**
     * Set the id list of selected call log.
     */
    public void setSelectedCallLogIds(ArrayList<Integer> idList) {
        mSelectedCallLogIdList.clear();
        mSelectedCallLogIdList.addAll(idList);
    }

    /**
     * Reconcile the selected call log ids with the ids in the new cursor.
     */
    private boolean reconcileSeletetedItems(Cursor newCursor) {
        if (mSelectedCallLogIdList.isEmpty()) {
            return false;
        }
        if (newCursor == null || newCursor.getCount() <= 0) {
            mSelectedCallLogIdList.clear();
            mSelectedItemCount = 0;
            return true;
        }
        ArrayList<Integer> idList = new ArrayList<Integer>();
        ArrayList<Integer> groupIdList = new ArrayList<Integer>();
        int newSelectedItemCount = 0;
        for (int i = 0; i < getItemCount(); ++i) {
            int count = 0;
            Cursor cursor = (Cursor) getItem(i);
            if (cursor == null) {
                continue;
            }
            int position = cursor.getPosition();
            if (isGroupHeader(i)) {
                count = getGroupSize(i);
            } else {
                count = 1;
            }
            boolean haveSelectedCallLog = false;
            groupIdList.clear();
            for (int j = 0; j < count; j++) {
                if (!mCursor.moveToPosition(position + j)) {
                    continue;
                }
                int id = mCursor.getInt(CallLogQuery.ID);
                groupIdList.add(id);
                if (!haveSelectedCallLog && mSelectedCallLogIdList.contains(id)) {
                    haveSelectedCallLog = true;
                }
            }
            if (haveSelectedCallLog) {
                newSelectedItemCount++;
                idList.addAll(groupIdList);
            }
        }
        mSelectedCallLogIdList.clear();
        mSelectedCallLogIdList.addAll(idList);
        mSelectedItemCount = newSelectedItemCount;
        return true;
    }

    public void setSelectedItemChangeListener(SelectedItemChangeListener listener) {
        mSelectedItemChangeListener = listener;
    }

    public void setItemClickListener(View.OnClickListener listener) {
        mItemClickListener = listener;
    }
    /** @} */

}
