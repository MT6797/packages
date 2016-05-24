/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.contacts.list;

import com.android.contacts.common.list.ContactListAdapter;
import com.android.contacts.common.list.ContactListItemView;
import com.android.contacts.common.list.DefaultContactListAdapter;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.list.MultiSelectEntryContactListAdapter.SelectedContactsListener;
import com.android.contacts.R;

import android.R.integer;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.mediatek.contacts.util.Log;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Fragment containing a contact list used for browsing contacts and optionally selecting
 * multiple contacts via checkboxes.
 */
public class MultiSelectContactsListFragment extends DefaultContactBrowseListFragment
        implements SelectedContactsListener {
    private static final String TAG = "MultiSelectContactsListFragment";

    public interface OnCheckBoxListActionListener {
        void onStartDisplayingCheckBoxes();
        void onSelectedContactIdsChanged();
        void onStopDisplayingCheckBoxes();
    }

    private static final String EXTRA_KEY_SELECTED_CONTACTS = "selected_contacts";
    /// M: If it need to show checkbox, after screen rotation.
    private static final String EXTRA_KEY_SHOW_CHECKBOX = "show_checkbox";

    private OnCheckBoxListActionListener mCheckBoxListListener;
    // M: DefaultMode default loaderId
    private static final int DEFAULTMODE_LOADERID = 0;

    public void setCheckBoxListListener(OnCheckBoxListActionListener checkBoxListListener) {
        mCheckBoxListListener = checkBoxListListener;
    }

    @Override
    public void onSelectedContactsChanged() {
        Log.d(TAG, "[onSelectedContactsChanged]");
        if (mCheckBoxListListener != null) {
            mCheckBoxListListener.onSelectedContactIdsChanged();
        }
    }

    @Override
    public void onSelectedContactsChangedViaCheckBox() {
        Log.d(TAG, "[onSelectedContactsChangedViaCheckBox]");
        if (getAdapter().getSelectedContactIds().size() == 0) {
            // Last checkbox has been unchecked. So we should stop displaying checkboxes.
            mCheckBoxListListener.onStopDisplayingCheckBoxes();
        } else {
            onSelectedContactsChanged();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "[onActivityCreated]");
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            final TreeSet<Long> selectedContactIds = (TreeSet<Long>)
                    savedInstanceState.getSerializable(EXTRA_KEY_SELECTED_CONTACTS);
            /// M: If it need to show checkbox, after screen rotation.
            boolean showCheckBoxes = savedInstanceState.getBoolean(EXTRA_KEY_SHOW_CHECKBOX);
            getAdapter().setDisplayCheckBoxes(showCheckBoxes);
            getAdapter().setSelectedContactIds(selectedContactIds);
            if (mCheckBoxListListener != null) {
                mCheckBoxListListener.onSelectedContactIdsChanged();
            }
        }
    }

    public TreeSet<Long> getSelectedContactIds() {
        final MultiSelectEntryContactListAdapter adapter = getAdapter();
        return adapter.getSelectedContactIds();
    }

    @Override
    public MultiSelectEntryContactListAdapter getAdapter() {
        return (MultiSelectEntryContactListAdapter) super.getAdapter();
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();
        getAdapter().setSelectedContactsListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EXTRA_KEY_SELECTED_CONTACTS, getSelectedContactIds());
        /// M: If it need to show checkbox, after screen rotation.
        outState.putBoolean(EXTRA_KEY_SHOW_CHECKBOX,
                getSelectedContactIds().size() > 0 ? true : false);

    }

    public void displayCheckBoxes(boolean displayCheckBoxes) {
        getAdapter().setDisplayCheckBoxes(displayCheckBoxes);
        if (!displayCheckBoxes) {
            clearCheckBoxes();
        }
    }

    public void clearCheckBoxes() {
        getAdapter().setSelectedContactIds(new TreeSet<Long>());
    }

    @Override
    protected boolean onItemLongClick(int position, long id) {
        Log.d(TAG, "[onItemLongClick]position = " + position + ",id = " + id);
        final int previouslySelectedCount = getAdapter().getSelectedContactIds().size();
        final Uri uri = getAdapter().getContactUri(position);
        /// M: If is SDN number, can't do long click to multiSelect
        if (uri != null && (position > 0 || !getAdapter().hasProfile())
                && !getAdapter().isSdnNumber(position)) {
            final String contactId = uri.getLastPathSegment();
            if (!TextUtils.isEmpty(contactId)) {
                if (mCheckBoxListListener != null) {
                    mCheckBoxListListener.onStartDisplayingCheckBoxes();
                }
                getAdapter().toggleSelectionOfContactId(Long.valueOf(contactId));
            }
        }
        final int nowSelectedCount = getAdapter().getSelectedContactIds().size();
        if (mCheckBoxListListener != null
                && previouslySelectedCount != 0 && nowSelectedCount == 0) {
            // Last checkbox has been unchecked. So we should stop displaying checkboxes.
            Log.d(TAG, "[onItemLongClick]onStopDisplayingCheckBoxes");
            mCheckBoxListListener.onStopDisplayingCheckBoxes();
        }
        return true;
    }

    @Override
    protected void onItemClick(int position, long id) {
        Log.d(TAG, "[onItemClick]position = " + position + ",id = " + id);
        final Uri uri = getAdapter().getContactUri(position);
        if (uri == null) {
            Log.w(TAG, "[onItemClick]uri is null,return.");
            return;
        }
        /// M: If is SDN number, can't do click on multiSelection
        if (getAdapter().isDisplayingCheckBoxes() && !getAdapter().isSdnNumber(position)) {
            final String contactId = uri.getLastPathSegment();
            if (!TextUtils.isEmpty(contactId)) {
                getAdapter().toggleSelectionOfContactId(Long.valueOf(contactId));
            }
        } else {
            super.onItemClick(position, id);
        }
        if (mCheckBoxListListener != null && getAdapter().getSelectedContactIds().size() == 0) {
            mCheckBoxListListener.onStopDisplayingCheckBoxes();
        }
    }

    @Override
    protected ContactListAdapter createListAdapter() {
        Log.d(TAG, "[createListAdapter]");
        DefaultContactListAdapter adapter = new MultiSelectEntryContactListAdapter(getContext());
        adapter.setSectionHeaderDisplayEnabled(isSectionHeaderDisplayEnabled());
        adapter.setDisplayPhotos(true);
        adapter.setPhotoPosition(
                ContactListItemView.getDefaultPhotoPosition(/* opposite = */ false));
        return adapter;
    }

    /// M: Add for SelectAll/DeSelectAll Feature. @{
    // SelectAll or DeselectAll items.
    private boolean mIsSelectedAll = false;

    public void updateSelectedItemsView() {
        int count = getAdapter().getCount();
        TextView labelTextView = (TextView)getView().findViewById(R.id.label);
        if (labelTextView != null ){
            count--;
        }
        if (getAdapter().hasProfile()) {
            count--;
        }
        if (getAdapter().getSdnNumber() > 0) {
            count -= getAdapter().getSdnNumber();
        }
        Log.d(TAG, "[updateSelectedItemsView]count = " + count);
        int checkCount = getAdapter().getSelectedContactIds().size();
        // Add consideration of "0" case
        if (count != 0 && count == checkCount) {
            mIsSelectedAll = true;
        } else {
            mIsSelectedAll = false;
        }
    }

    /**
     * @return mIsSelectedAll
     */
    public boolean isSelectedAll() {
        return mIsSelectedAll;
    }

    public void updateCheckBoxState(boolean checked) {
        /// M: Fix select number not right issue after search. @{
        int position = 0;
        final int count = getAdapter().getCount();
        TextView labelTextView = (TextView)getView().findViewById(R.id.label);
        if (labelTextView != null ){
            position++;
        }
        Log.d(TAG, "[updateCheckBoxState]checked = " +checked + ",count = " + count);
        long contactId = -1;
        if (checked) {
            for (; position < count; position++) {
        /// @}
                if (getAdapter().isUserProfile(position) || getAdapter().isSdnNumber(position)) {
                    continue;
                }
                if (!getListView().isItemChecked(position)) {
                    getListView().setItemChecked(position, checked);
                    contactId = getAdapter().getContactId(position);
                    getSelectedContactIds().add(contactId);
                }
            }
        } else {
            getSelectedContactIds().clear();
        }
    }
    /*@}*/

    /// M: Fix CR ALPS02273774: Change airplane mode when select contacts. @{
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(TAG, "[onLoadFinished]");
        super.onLoadFinished(loader, data);

        long dataId = -1;
        int position = 0;
        Set<Long> newDataSet = new HashSet<Long>();
        if (data != null && data.getCount() != 0) {
            data.moveToPosition(-1);
            while (data.moveToNext()) {
                dataId = -1;
                dataId = data.getLong(0);
                if (dataId != -1) {
                    newDataSet.add(dataId);
                }
            }
        }
        int sizeBefore = getAdapter().getSelectedContactIds().size();
        // M: fix ALPS02459978,remove selected should judge loaderiId caused by search mode
        // will start other loader,every loader finished,will start this follow.@{
        int loaderId = loader.getId();
        Log.d(TAG,"[onLoadFinished]sizeBefore = " + sizeBefore + ",loader =" + loader +
                ",loaderId = " + loaderId + ",newDataSet: " + newDataSet.toString() +
                ",currentSelected: " + getAdapter().getSelectedContactIds().toString());
        for (Iterator<Long> it = getAdapter().getSelectedContactIds().iterator(); it.hasNext();) {
            Long id = it.next();
            if ((loaderId == DEFAULTMODE_LOADERID) && !newDataSet.contains(id)) {
                Log.d(TAG, "[onLoadFinished] selected removeId = " + id);
            // @}
                it.remove();
            }
        }
        int sizeAfter = getAdapter().getSelectedContactIds().size();
        Log.d(TAG,"[onLoadFinished]sizeAfter = " + sizeAfter);
        if (data != null && data.getCount() != 0) {
            data.moveToPosition(-1);
            while (data.moveToNext()) {
                dataId = -1;
                dataId = data.getLong(0);
                if (getAdapter().getSelectedContactIds().contains(dataId)) {
                    getListView().setItemChecked(position, true);
                }
                ++position;
            }
        }

        if (sizeAfter > 0) {
            onSelectedContactsChanged();
            updateSelectedItemsView();
        /// M: Add for fix vcs indicator flashing issue. @{
        } else if (sizeBefore > 0 || getAdapter().getCount() == 0) {
            if (mCheckBoxListListener != null) {
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        mCheckBoxListListener.onStopDisplayingCheckBoxes();//exit the select state
                    }
                });
            }
        }
        /// @}
    }
    /// @}
}
