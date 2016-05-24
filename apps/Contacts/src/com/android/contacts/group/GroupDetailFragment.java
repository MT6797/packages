/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2011 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.contacts.group;

import android.app.Activity;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Groups;
import android.text.TextUtils;
//import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.contacts.ContactSaveService;
import com.android.contacts.GroupMemberLoader;
import com.android.contacts.GroupMetaDataLoader;
import com.android.contacts.R;
import com.android.contacts.common.CallUtil;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.util.ImplicitIntentsUtil;
import com.android.contacts.interactions.GroupDeletionDialogFragment;
import com.android.contacts.common.list.ContactTileAdapter;
import com.android.contacts.common.list.ContactTileView;
import com.android.contacts.list.GroupMemberTileAdapter;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.util.WeakAsyncTask;

import android.widget.ProgressBar;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.content.ContentResolver;
import android.app.ProgressDialog;

import com.mediatek.contacts.ContactsApplicationEx;
import com.mediatek.contacts.ContactsSystemProperties;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.list.ContactListMultiChoiceActivity;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.PhbStateHandler;
import com.mediatek.contacts.util.MtkToast;
import com.mediatek.contacts.widget.WaitCursorView;

import android.accounts.Account;

import com.android.contacts.ContactSaveService.DeleteEndListener;
import com.android.contacts.util.PhoneCapabilityTester;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.os.UserHandle;
/**
 * Displays the details of a group and shows a list of actions possible for the group.
 */
public class GroupDetailFragment extends Fragment implements
        OnScrollListener, PhbStateHandler.Listener {

    public static interface Listener {
        /**
         * The group title has been loaded
         */
        public void onGroupTitleUpdated(String title);

        /**
         * The number of group members has been determined
         */
        public void onGroupSizeUpdated(String size);

        /**
         * The account type and dataset have been determined.
         */
        public void onAccountTypeUpdated(String accountTypeString, String dataSet);

        /**
         * User decided to go to Edit-Mode
         */
        public void onEditRequested(Uri groupUri);

        /**
         * Contact is selected and should launch details page
         */
        public void onContactSelected(Uri contactUri);

        /**
         * Group is deleted and should finish details page
         */
        public void onGroupNotFound();
    }

    private static final String TAG = "GroupDetailFragment";

    private static final int LOADER_METADATA = 0;
    private static final int LOADER_MEMBERS = 1;

    private Context mContext;

    private View mRootView;
    private ViewGroup mGroupSourceViewContainer;
    private View mGroupSourceView;
    private TextView mGroupTitle;
    private TextView mGroupSize;
    private ListView mMemberListView;
    private View mEmptyView;

    private Listener mListener;

    private ContactTileAdapter mAdapter;
    private ContactPhotoManager mPhotoManager;
    private AccountTypeManager mAccountTypeManager;

    private Uri mGroupUri;
    private long mGroupId;
    private String mGroupName;
    private String mAccountTypeString;
    private String mDataSet;
    private boolean mIsReadOnly;
    private boolean mIsMembershipEditable;

    private boolean mShowGroupActionInActionBar;
    private boolean mOptionsMenuGroupDeletable;
    private boolean mOptionsMenuGroupEditable;
    private boolean mCloseActivityAfterDelete;

    public GroupDetailFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        mAccountTypeManager = AccountTypeManager.getInstance(mContext);

        Resources res = getResources();
        int columnCount = res.getInteger(R.integer.contact_tile_column_count);

        mAdapter = new GroupMemberTileAdapter(activity, mContactTileListener, columnCount);

        configurePhotoLoader();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        ContactSaveService.setDeleteEndListener(mDeleteEndListener);
        setHasOptionsMenu(true);
        mRootView = inflater.inflate(R.layout.group_detail_fragment, container, false);
        mGroupTitle = (TextView) mRootView.findViewById(R.id.group_title);
        mGroupSize = (TextView) mRootView.findViewById(R.id.group_size);
        mGroupSourceViewContainer = (ViewGroup) mRootView.findViewById(
                R.id.group_source_view_container);
        mEmptyView = mRootView.findViewById(android.R.id.empty);
        mMemberListView = (ListView) mRootView.findViewById(android.R.id.list);
        mMemberListView.setItemsCanFocus(true);
        mMemberListView.setAdapter(mAdapter);


        /*
         * Bug Fix by Mediatek Begin. Original Android's code: CR ID:
         * ALPS00115673 Descriptions: add wait cursor
         */
        if (PhoneCapabilityTester.isUsingTwoPanes(this.getActivity())) {
          mMemberListView.setEmptyView(mEmptyView);
        }

        mLoadingContainer = mRootView.findViewById(R.id.loading_container);
        mLoadingContact = (TextView) mRootView.findViewById(R.id.loading_contact);
        mLoadingContact.setVisibility(View.GONE);
        mProgress = (ProgressBar) mRootView.findViewById(R.id.progress_loading_contact);
        mProgress.setVisibility(View.GONE);

        mWaitCursorView = new WaitCursorView(mContext, mLoadingContainer, mProgress,
                mLoadingContact);
        /*
         * Bug Fix by Mediatek End.
         */


        return mRootView;
    }

    public void loadGroup(Uri groupUri) {
        mGroupUri = groupUri;
        startGroupMetadataLoader();
    }

    public void setQuickContact(boolean enableQuickContact) {
        mAdapter.enableQuickContact(enableQuickContact);
    }

    private void configurePhotoLoader() {
        if (mContext != null) {
            if (mPhotoManager == null) {
                mPhotoManager = ContactPhotoManager.getInstance(mContext);
            }
            if (mMemberListView != null) {
                mMemberListView.setOnScrollListener(this);
            }
            if (mAdapter != null) {
                mAdapter.setPhotoLoader(mPhotoManager);
            }
        }
    }

    public void setListener(Listener value) {
        mListener = value;
    }

    public void setShowGroupSourceInActionBar(boolean show) {
        mShowGroupActionInActionBar = show;
    }

    public Uri getGroupUri() {
        return mGroupUri;
    }

    /**
     * Start the loader to retrieve the metadata for this group.
     */
    private void startGroupMetadataLoader() {
        getLoaderManager().restartLoader(LOADER_METADATA, null, mGroupMetadataLoaderListener);
    }

    /**
     * Start the loader to retrieve the list of group members.
     */
    private void startGroupMembersLoader() {
        if (!isAdded()) {
            Log.w(TAG, "#startGroupMembersLoader(),the fragment is not attach to  Activity.");
            return;
        }

        getLoaderManager().restartLoader(LOADER_MEMBERS, null, mGroupMemberListLoaderListener);
    }

    private final ContactTileView.Listener mContactTileListener =
            new ContactTileView.Listener() {

        @Override
        public void onContactSelected(Uri contactUri, Rect targetRect) {
            mListener.onContactSelected(contactUri);
        }

        @Override
        public void onCallNumberDirectly(String phoneNumber) {
            // No need to call phone number directly from People app.
            Log.w(TAG, "unexpected invocation of onCallNumberDirectly()");
        }

        @Override
        public int getApproximateTileWidth() {
            return getView().getWidth() / mAdapter.getColumnCount();
        }
    };

    /**
     * The listener for the group metadata loader.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupMetadataLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            /*
             * Bug Fix by Mediatek Begin. Original Android's code: CR ID:
             * ALPS00115673 Descriptions: add wait cursor
             */
            Log.i(TAG, "onCreateLoader");

            mWaitCursorView.startWaitCursor();
            isFinished = false;

            /*
             * Bug Fix by Mediatek End.
             */
            OCL = System.currentTimeMillis();
            Log.i(TAG,
                    "GroupDetailFragment mGroupMetadataLoaderListener onCreateLoader OCL : "
                            + OCL);
            return new GroupMetaDataLoader(mContext, mGroupUri);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data == null || data.isClosed()) {
                Log.e(TAG, "Failed to load group metadata");
                return;
            }

            /// M:check whether the fragment still in Activity@{
            if (!isAdded()) {
                Log.w(TAG, "onLoadFinished(),This Fragment is not add to the Activity now.");
                if (data != null) {
                    data.close();
                }
                return;
            }
            /// @}

            // The following lines are provided and maintained by Mediatek Inc.
            OLF = System.currentTimeMillis();
            Log.i(TAG,
                    "GroupDetailFragment mGroupMetadataLoaderListener onLoadFinished OLF : "
                            + OLF + " | OLF-OCL = " + (OLF - OCL));
            // The previous lines are provided and maintained by Mediatek Inc.
            /**
             * M: fix bug for ALPS00336957 je happen when press back key from sms
             */
            if (null != data) {
                data.moveToPosition(-1);
                if (data.moveToNext()) {
                    boolean deleted = data.getInt(GroupMetaDataLoader.DELETED) == 1;
                    if (!deleted) {
                        bindGroupMetaData(data);

                        // Retrieve the list of members
                        ///M: in onLoadFinished() can't call restart loader directly,
                        ///so we should use a handler to avoid Fragment commit failure.
                        Handler restartLoaderHandler = new Handler() {
                            @Override
                            public void handleMessage(Message msg) {
                                Log.d(TAG, "[handleMessage] to restart group memeber loader");
                                startGroupMembersLoader();
                            }
                        };
                        restartLoaderHandler.sendEmptyMessage(0);
                        return;
                    }
                }
            }
            /**
             * M: fix bug for ALPS00336957 end
             */
            // The following lines are provided and maintained by Mediatek Inc.
            // if needn't query members, dismiss the loading cursor!
            Log.i(TAG, "No member data to load!! isFinished:" + isFinished);
            isFinished = true;
            mWaitCursorView.stopWaitCursor();
            // The previous lines are provided and maintained by Mediatek Inc.

            updateSize(-1);
            updateTitle(null);

            /** M: If group has been deleted, just finish the details page @{ */
            if (mListener != null) {
                mListener.onGroupNotFound();
                return;
            }
            /** @} */
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    };

    /**
     * The listener for the group members list loader
     */
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupMemberListLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            // The following lines are provided and maintained by Mediatek Inc.
            OCL1 = System.currentTimeMillis();
            Log.i(TAG,
                    "GroupDetailFragment mGroupMemberListLoaderListener onCreateLoader OCL1 : "
                            + OCL1);
            // The previous lines are provided and maintained by Mediatek Inc.
            updateMoveMenuStatus();
            return GroupMemberLoader.constructLoaderForGroupDetailQuery(mContext, mGroupId);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data == null || data.isClosed()) {
                Log.e(TAG, "Failed to load group members");
                return;
            }
            /// M:check whether the fragment still in Activity@{
            if (!isAdded()) {
                Log.w(TAG, "onLoadFinished,This Fragment is not add to the Activity.");
                if (data != null) {
                    data.close();
                }
                return;
            }
            /// @}

            updateSize(data.getCount());
            // The following lines are provided and maintained by Mediatek Inc.
            OLF1 = System.currentTimeMillis();
            Log.i(TAG,
                    "GroupDetailFragment mGroupMemberListLoaderListener onLoadFinished OLF1 : "
                            + OLF1 + " | OLF1-OCL1 = " + (OLF1 - OCL1));
            /*
             * Bug Fix by Mediatek Begin. Original Android's code: CR ID:
             * ALPS00115673 Descriptions: add wait cursor
             */
            mWaitCursorView.stopWaitCursor();
            isFinished = true;

            Log.i(TAG, "ohonefavoriterfragmetn onloadfinished");

            /*
             * Bug Fix by Mediatek End.
             */

            groupMemberSize = data.getCount();
            if (DEBUG) {
                Log.i(TAG, groupMemberSize + "---groupMemberSize mGroupMemberListLoaderListener");
            }

            getActivity().invalidateOptionsMenu();
            // The previous  lines are provided and maintained by Mediatek Inc.
            mAdapter.setContactCursor(data);
            mMemberListView.setEmptyView(mEmptyView);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    };

    private void bindGroupMetaData(Cursor cursor) {
        cursor.moveToPosition(-1);
        if (cursor.moveToNext()) {
            mAccountTypeString = cursor.getString(GroupMetaDataLoader.ACCOUNT_TYPE);
            // The following lines are provided and maintained by Mediatek Inc.
            mAccountName = cursor.getString(GroupMetaDataLoader.ACCOUNT_NAME);
            // The previous lines are provided and maintained by Mediatek Inc.
            mDataSet = cursor.getString(GroupMetaDataLoader.DATA_SET);
            mGroupId = cursor.getLong(GroupMetaDataLoader.GROUP_ID);
            mGroupName = cursor.getString(GroupMetaDataLoader.TITLE);
            mIsReadOnly = cursor.getInt(GroupMetaDataLoader.IS_READ_ONLY) == 1;
            updateTitle(mGroupName);
            // Must call invalidate so that the option menu will get updated
            getActivity().invalidateOptionsMenu();

            final String accountTypeString = cursor.getString(GroupMetaDataLoader.ACCOUNT_TYPE);
            final String dataSet = cursor.getString(GroupMetaDataLoader.DATA_SET);
            updateAccountType(accountTypeString, dataSet);
        }
    }

    private void updateTitle(String title) {
        if (mGroupTitle != null) {
            mGroupTitle.setText(title);
        } else {
            mListener.onGroupTitleUpdated(title);
        }
    }

    /**
     * Display the count of the number of group members.
     * @param size of the group (can be -1 if no size could be determined)
     */
    private void updateSize(int size) {
        String groupSizeString;
        if (size == -1) {
            groupSizeString = null;
        } else {
            AccountType accountType = mAccountTypeManager.getAccountType(mAccountTypeString,
                    mDataSet);
            final CharSequence dispLabel = accountType.getDisplayLabel(mContext);
            if (!TextUtils.isEmpty(dispLabel)) {
                String groupSizeTemplateString = getResources().getQuantityString(
                        R.plurals.num_contacts_in_group, size);
                groupSizeString = String.format(groupSizeTemplateString, size, dispLabel);
            } else {
                String groupSizeTemplateString = getResources().getQuantityString(
                        R.plurals.group_list_num_contacts_in_group, size);
                groupSizeString = String.format(groupSizeTemplateString, size);
            }
        }

        if (mGroupSize != null) {
            mGroupSize.setText(groupSizeString);
        } else {
            mListener.onGroupSizeUpdated(groupSizeString);
        }
    }

    /**
     * Once the account type, group source action, and group source URI have been determined
     * (based on the result from the {@link Loader}), then we can display this to the user in 1 of
     * 2 ways depending on screen size and orientation: either as a button in the action bar or as
     * a button in a static header on the page.
     * We also use isGroupMembershipEditable() of accountType to determine whether or not we should
     * display the Edit option in the Actionbar.
     */
    private void updateAccountType(final String accountTypeString, final String dataSet) {
        final AccountTypeManager manager = AccountTypeManager.getInstance(getActivity());
        final AccountType accountType =
                manager.getAccountType(accountTypeString, dataSet);

        mIsMembershipEditable = accountType.isGroupMembershipEditable();

        // If the group action should be shown in the action bar, then pass the data to the
        // listener who will take care of setting up the view and click listener. There is nothing
        // else to be done by this {@link Fragment}.
        if (mShowGroupActionInActionBar) {
            mListener.onAccountTypeUpdated(accountTypeString, dataSet);
            return;
        }

        // Otherwise, if the {@link Fragment} needs to create and setup the button, then first
        // verify that there is a valid action.
        if (!TextUtils.isEmpty(accountType.getViewGroupActivity())) {
            if (mGroupSourceView == null) {
                mGroupSourceView = GroupDetailDisplayUtils.getNewGroupSourceView(mContext);
                // Figure out how to add the view to the fragment.
                // If there is a static header with a container for the group source view, insert
                // the view there.
                if (mGroupSourceViewContainer != null) {
                    mGroupSourceViewContainer.addView(mGroupSourceView);
                }
            }

            // Rebind the data since this action can change if the loader returns updated data
            mGroupSourceView.setVisibility(View.VISIBLE);
            GroupDetailDisplayUtils.bindGroupSourceView(mContext, mGroupSourceView,
                    accountTypeString, dataSet);
            mGroupSourceView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Uri uri = ContentUris.withAppendedId(Groups.CONTENT_URI, mGroupId);
                    final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.setClassName(accountType.syncAdapterPackageName,
                            accountType.getViewGroupActivity());
                    try {
                        ImplicitIntentsUtil.startActivityInApp(getActivity(), intent);
                    } catch (ActivityNotFoundException e) {
                        Log.e(TAG, "startActivity() failed: " + e);
                        Toast.makeText(getActivity(), R.string.missing_app,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else if (mGroupSourceView != null) {
            mGroupSourceView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == OnScrollListener.SCROLL_STATE_FLING) {
            mPhotoManager.pause();
        } else {
            mPhotoManager.resume();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.view_group, menu);
    }

    public boolean isOptionsMenuChanged() {
        return mOptionsMenuGroupDeletable != isGroupDeletable() &&
                mOptionsMenuGroupEditable != isGroupEditableAndPresent();
    }

    public boolean isGroupDeletable() {
        return mGroupUri != null && !mIsReadOnly;
    }

    public boolean isGroupEditableAndPresent() {
        return mGroupUri != null && mIsMembershipEditable;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        /*
         * Bug Fix by Mediatek Begin Original Android's code:
         * mOptionsMenuGroupDeletable = isGroupDeletable() && isVisible()
         * mOptionsMenuGroupEditable = isGroupEditableAndPresent() && isVisible(); CR ID
         * :ALPS000252546 Descriptions: when loading data ,move the menu
         */
         mOptionsMenuGroupDeletable = isGroupDeletable() && isVisible() && isFinished;
         mOptionsMenuGroupEditable = isGroupEditableAndPresent() && isVisible() && isFinished;
        /*
         * Bug Fix by Mediatek End
         */
        final MenuItem editMenu = menu.findItem(R.id.menu_edit_group);
        editMenu.setVisible(mOptionsMenuGroupEditable);

        final MenuItem deleteMenu = menu.findItem(R.id.menu_delete_group);
        deleteMenu.setVisible(mOptionsMenuGroupDeletable);

        // The following lines are provided and maintained by Mediatek Inc.
        if (DEBUG) {
            Log.i(TAG, groupMemberSize
                    + "------groupMemberSize onPrepareOptionsMenu [fragment]");
        }
        final MenuItem moveMenu = menu.findItem(R.id.menu_move_group);
        final MenuItem sendMsgMenu = menu.findItem(R.id.menu_message_group);
        final MenuItem sendEmailMenu = menu.findItem(R.id.menu_email_group);
        if (groupMemberSize <= 0) {
            moveMenu.setVisible(false);
            sendMsgMenu.setVisible(false);
            sendEmailMenu.setVisible(false);
        } else {
            if (DISABLE_MOVE_MENU == true) {
                moveMenu.setVisible(false);
            } else {
                moveMenu.setVisible(true);
            }
            sendMsgMenu.setVisible(true);
            sendEmailMenu.setVisible(true);
        }

        /// M: For MTK multiuser in 3gdatasms @{
        if (ContactsSystemProperties.MTK_OWNER_SIM_SUPPORT) {
            int userId = UserHandle.myUserId();
            if (userId != UserHandle.USER_OWNER) {
                sendMsgMenu.setVisible(false);
            }
        }
        /// @}
        /** M: Bug Fix for ALPS01451311 @{ */
        // Remove sendMMS button when it not support mms
        if (null == PhoneCapabilityTester.getSmsComponent(mContext)
                || !PhoneCapabilityTester.isSupportSms(mContext)) {
            sendMsgMenu.setVisible(false);
        }
        /** @} */
        if (PhoneCapabilityTester.isUsingTwoPanes(mContext)) {
            Log.i(TAG, "it is tablet");
            /** M: Bug Fix for ALPS01594286 @{ */
            final Thread thread = new Thread() {
                @Override
                public void run() {
                    needcheckGroupData(editMenu, deleteMenu);
                }
            };
            thread.start();
            /** @} */
        }
        //M:OP01 RCS will add group detail menu item @{
        ExtensionManager.getInstance().getRcsExtension().
                addGroupDetailMenuOptions(menu, mGroupId, mContext, groupMemberSize);
        /** @} */
        // The previous lines are provided and maintained by Mediatek Inc.
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // / M: judge contactsapplication is busy or not? fixed cr ALPS00567939
        // & ALPS00542175 @{
        if (ContactsApplicationEx.isContactsApplicationBusy()) {
            Log.w(TAG, "[onOptionsItemSelected]contacts busy doing something");
            MtkToast.toast(getActivity(), R.string.phone_book_busy);
            return false;
        }
        // / @}
        switch (item.getItemId()) {
            case R.id.menu_edit_group: {
                // The following lines are provided and maintained by Mediatek Inc.
                mGroupUri = mGroupUri.buildUpon().appendPath(String.valueOf(mSubId)).build();
                // The previous  lines are provided and maintained by Mediatek Inc.
                if (mListener != null) {
                    mListener.onEditRequested(mGroupUri);
                }
                break;
            }
            case R.id.menu_delete_group: {
                // The following lines are provided and maintained by Mediatek Inc.
                GroupDeletionDialogFragment.show(getFragmentManager(), mGroupId, mGroupName,
                        mCloseActivityAfterDelete, mSubId);
                // The previous lines are provided and maintained by Mediatek Inc.
                return true;
            }
            // The following lines are provided and maintained by Mediatek Inc.
            case R.id.menu_move_group: {

                Intent moveIntent = new Intent(getActivity(), ContactListMultiChoiceActivity.class);
                moveIntent.setAction(com.mediatek.contacts.util.
                        ContactsIntent.LIST.ACTION_GROUP_MOVE_MULTI_CONTACTS);
                moveIntent.putExtra("mGroupName", mGroupName);
                moveIntent.putExtra("mSubId", mSubId);
                moveIntent.putExtra("mGroupId", mGroupId);
                moveIntent.putExtra("mAccountName", mAccountName);
                if (!TextUtils.isEmpty(mAccountName)
                    && !TextUtils.isEmpty(mAccountTypeString)) {
                    Account tmpAccount = new Account(mAccountName, mAccountTypeString);
                    moveIntent.putExtra("account", tmpAccount);
                }

                this.startActivity(moveIntent);
                if (!PhoneCapabilityTester.isUsingTwoPanes(this.getActivity())) {
                    getActivity().finish();
                }
                break;
            }
            case R.id.menu_message_group: {
                new SendGroupSmsTask(this.getActivity()).execute(mGroupName);
                break;
            }
            case R.id.menu_email_group: {
                new SendGroupEmailTask(this.getActivity()).execute(mGroupName);
                break;
            // The previous  lines are provided and maintained by Mediatek Inc.
            }
        }
        return false;
    }

    public void closeActivityAfterDelete(boolean closeActivity) {
        mCloseActivityAfterDelete = closeActivity;
    }

    public long getGroupId() {
        return mGroupId;
    }

    // The following lines are provided and maintained by Mediatek Inc.
    private static final boolean DEBUG = true;
    private String mCategoryId = null;
    private int mSubId = -1;
    private String mSimName;
    private String mAccountName;
    ///M: For move to other group feature.
    private int mAccountGroupMemberCount;
    private int groupMemberSize = -1;
    private boolean DISABLE_MOVE_MENU = false;
    private WeakReference<ProgressDialog> mProgressDialog;
    public void loadExtras(String CategoryId, int subId, String simName, int count) {
        mCategoryId = CategoryId;
        mSubId = subId;
        mSimName = simName;
        ///M: For move to other group feature.
        mAccountGroupMemberCount = count;
        PhbStateHandler.getInstance().register(this);
    }

    public void loadExtras(int subId) {
        mSubId = subId;
        PhbStateHandler.getInstance().register(this);
    }

    private class SendGroupSmsTask extends
            WeakAsyncTask<String, Void, String, Activity> {

        public SendGroupSmsTask(Activity target) {
            super(target);
        }

        @Override
        protected void onPreExecute(Activity target) {
            mProgressDialog = new WeakReference<ProgressDialog>(ProgressDialog.show(
                    target, null,
                    target.getText(R.string.please_wait), true));
        }

        @Override
        protected String doInBackground(final Activity target, String... group) {
            return getSmsAddressFromGroup(target.getBaseContext(), getGroupId());
        }

        @Override
        protected void onPostExecute(final Activity target, String address) {
            ProgressDialog progress = mProgressDialog.get();
            if (progress != null && progress.isShowing()
                    && getActivity() != null && !getActivity().isFinishing()) {
                progress.dismiss();
            }
            if (address == null || address.length() == 0) {
                Toast.makeText(target, R.string.no_valid_number_in_group,
                        Toast.LENGTH_SHORT).show();
            } else {
                String[] list = address.split(";");
                if (list.length > 1) {
                    String promptStr = address.substring(list[0].length() + 1);
                    Toast.makeText(target, promptStr, Toast.LENGTH_SHORT).show();
                }
                address = list[0];
                if (address == null || address.length() == 0) {
                    return;
                }
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.fromParts(CallUtil.SCHEME_SMSTO, address,
                        null));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    target.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(target,
                            getString(R.string.quickcontact_missing_app),
                            Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "ActivityNotFoundException for secondaryIntent");
                }
            }
        }

        public String getSmsAddressFromGroup(Context context, long groupId) {
            Log.d(TAG, "groupId:" + groupId);
            StringBuilder builder = new StringBuilder();
            ContentResolver resolver = context.getContentResolver();

            HashSet<Long> allContacts = new HashSet<Long>();
            StringBuilder where = getWhere(resolver, groupId, allContacts);
            if (null == where) {
                return "";
            }
            where.append(Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'");
            Log.i(TAG, "getSmsAddressFromGroup where " + where);
            Cursor cursor = resolver.query(Data.CONTENT_URI,
                    new String[] {Data.DATA1, Phone.TYPE, Data.CONTACT_ID, Data.IS_PRIMARY},
                    where.toString(), null, Data.CONTACT_ID + " ASC, " + Data._ID + " ASC ");
            if (cursor != null) {
                long candidateContactId = -1;
                int candidateType = -1;
                String candidateAddress = "";
                // The following lines are provided and maintained by Mediatek Inc.
                int isDefault = 0;
                // The previous lines are provided and maintained by Mediatek Inc.
                while (cursor.moveToNext()) {
                    Long id = cursor.getLong(2);
                    if (allContacts.contains(id)) {
                        allContacts.remove(id);
                    }
                    int type = cursor.getInt(1);
                    String number = cursor.getString(0);
                    // The following lines are provided and maintained by Mediatek Inc.
                    isDefault = cursor.getInt(3);
                    // The previous lines are provided and maintained by Mediatek Inc.
                    int numIndex = number.indexOf(",");
                    int tempIndex = -1;
                    if ((tempIndex = number.indexOf(";")) >= 0) {
                        if (numIndex < 0) {
                            numIndex = tempIndex;
                        } else {
                            numIndex = numIndex < tempIndex ? numIndex
                                    : tempIndex;
                        }
                    }
                    if (numIndex == 0) {
                        continue;
                    } else if (numIndex > 0) {
                        number = number.substring(0, numIndex);
                    }

                    if (candidateContactId == -1) {
                        candidateContactId = id;
                        candidateType = type;
                        candidateAddress = number;
                    } else {
                        if (candidateContactId != id) {
                            if (candidateAddress != null
                                    && candidateAddress.length() > 0) {
                                if (builder.length() > 0) {
                                    builder.append(",");
                                }
                                builder.append(candidateAddress);
                            }
                            candidateContactId = id;
                            candidateType = type;
                            candidateAddress = number;
                            // The following lines are provided and maintained by Mediatek Inc.
                        } else if (isDefault == 1) {
                            candidateContactId = id;
                            candidateType = type;
                            candidateAddress = number;
                            // The previous lines are provided and maintained by Mediatek Inc.
                        } else {
                            if (candidateType != Phone.TYPE_MOBILE
                                    && type == Phone.TYPE_MOBILE) {
                                candidateContactId = id;
                                candidateType = type;
                                candidateAddress = number;
                            }
                        }
                    }
                    if (cursor.isLast()) {
                        if (candidateAddress != null
                                && candidateAddress.length() > 0) {
                            if (builder.length() > 0) {
                                builder.append(",");
                            }
                            builder.append(candidateAddress);
                        }
                    }

                }
                cursor.close();
            }
            Log.i(TAG, "[getSmsAddressFromGroup]address:" + builder);

            return showNoTelphoneOrEmailToast(context, builder, resolver,
                    allContacts, "sms");
        }
    }

    /**
     *  M: to show hints those contacts with no valid tel or email adds.
     * @param context
     * @param builder
     * @param resolver
     * @param allContacts
     * @param emailOrSms
     * @return  The hints Msg String.
     */
    private String showNoTelphoneOrEmailToast(Context context,
            StringBuilder builder, ContentResolver resolver,
            HashSet<Long> allContacts, String emailOrSms) {
        StringBuilder ids;
        StringBuilder where;
        ids = new StringBuilder();
        where = new StringBuilder();
        List<String> noNumberContactList = new ArrayList<String>();
        if (allContacts.size() > 0) {
            Long[] allContactsArray = allContacts.toArray(new Long[0]);
            for (Long id : allContactsArray) {
                if (ids.length() > 0) {
                    ids.append(",");
                }
                ids.append(id.toString());
            }
        }

        /** M: to fix ALPS00962454, Send group sms/email hints. */
        if (ids.length() > 0) {
            ids = getAllRawContactIds(resolver, ids);
            Log.i(TAG, "[getSmsAddressFromGroup]the best available rawcontactsids.");
        } else {
            Log.i(TAG, "[getSmsAddressFromGroup]length is null,return.");
            return builder.toString();
        }

        if (ids.length() > 0) {
            where.append(RawContacts._ID + " IN(");
            where.append(ids.toString());
            where.append(")");
        } else {
            return builder.toString();
        }
        where.append(" AND ");
        where.append(RawContacts.DELETED + "= 0");
        Log.i(TAG, "[getSmsAddressFromGroup]query no name cursor selection:" + where.toString());

        Cursor cursor = resolver.query(RawContacts.CONTENT_URI,
                new String[] { RawContacts.DISPLAY_NAME_PRIMARY }, where.toString(), null,
                Data.CONTACT_ID + " ASC ");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                noNumberContactList.add(cursor.getString(0));
            }
            cursor.close();
        }
        String str = "";
        /** M: Bug Fix for ALPS01439046 @{ */
        String name1 = "";
        if (noNumberContactList.size() > 0) {
            name1 = noNumberContactList.get(0);
            if (TextUtils.isEmpty(name1)) {
                name1 = context.getString(R.string.missing_name);
            }
        }
        if (noNumberContactList.size() == 1) {
            str = context.getString(emailOrSms.equals("sms") ? R.string.send_groupsms_no_number_1
                    : R.string.send_groupemail_no_number_1, name1);
        } else if (noNumberContactList.size() == 2) {
            String name2 = noNumberContactList.get(1);
            if (TextUtils.isEmpty(name2)) {
                name2 = context.getString(R.string.missing_name);
            }
            str = context.getString(emailOrSms.equals("sms") ? R.string.send_groupsms_no_number_2
                    : R.string.send_groupemail_no_number_2, name1, name2);
        } else if (noNumberContactList.size() > 2) {
            str = context.getString(
                    emailOrSms.equals("sms") ? R.string.send_groupsms_no_number_more
                            : R.string.send_groupemail_no_number_more, name1, String
                            .valueOf(noNumberContactList.size() - 1));
        }
        /** @} */
        String result = builder.toString();
        Log.i(TAG, "[getSmsAddressFromGroup]result:" + result);
        if (str != null && str.length() > 0) {
            return result + ";" + str;
        } else {
            return result;
        }
    }

    private class SendGroupEmailTask extends
            WeakAsyncTask<String, Void, String, Activity> {

        public SendGroupEmailTask(Activity target) {
            super(target);
        }

        @Override
        protected void onPreExecute(Activity target) {
            mProgressDialog = new WeakReference<ProgressDialog>(ProgressDialog.show(
                    target, null, target.getText(R.string.please_wait)));
        }

        @Override
        protected String doInBackground(final Activity target, String... group) {
            return getEmailAddressFromGroup(target, getGroupId());
        }

        @Override
        protected void onPostExecute(final Activity target, String address) {
            ProgressDialog progress = mProgressDialog.get();
            if (progress != null && progress.isShowing()
                    && getActivity() != null && !getActivity().isFinishing()) {
                progress.dismiss();
            }
            try {
                // Intent intent = new Intent(Intent.ACTION_SENDTO,
                // Uri.fromParts(Constants.SCHEME_MAILTO, address, null));
                // String[] addrList = address.split(",");
                //
                // Intent intent = new Intent(Intent.ACTION_SEND);
                // intent.setType("*/*");
                // intent.putExtra(Intent.EXTRA_EMAIL, addrList);
                Uri dataUri = null;

                if (address == null || address.length() == 0) {
                    Toast.makeText(target, R.string.no_valid_email_in_group,
                            Toast.LENGTH_SHORT).show();
                } else {
                    String[] list = address.split(";");
                    if (list.length > 1) {
                        String promptStr = address.substring(list[0].length() + 1);
                        Toast.makeText(target, promptStr, Toast.LENGTH_SHORT).show();
                    }
                    address = list[0];
                    if (address == null || address.length() == 0) {
                        return;
                    }
                    dataUri = Uri.fromParts(CallUtil.SCHEME_MAILTO, address, null);
                    Intent intent = new Intent(Intent.ACTION_SENDTO, dataUri);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    target.startActivity(intent);
                }
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "No activity found for Eamil");
                Toast
                        .makeText(target, R.string.email_error,
                                Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "SendGroupEmail error", e);
            }
        }

        public String getEmailAddressFromGroup(Context context, long groupId) {
            Log.d(TAG, "groupId:" + groupId);
            StringBuilder builder = new StringBuilder();
            ContentResolver resolver = context.getContentResolver();

            HashSet<Long> allContacts = new HashSet<Long>();
            StringBuilder where = getWhere(resolver, groupId, allContacts);
            if (null == where) {
                return "";
            }
            where.append(Data.MIMETYPE + "='" + Email.CONTENT_ITEM_TYPE + "'");
            Log.i(TAG, "[getEmailAddressFromGroup]where " + where);
            Cursor cursor = resolver.query(Data.CONTENT_URI,
            // The following lines are provided and maintained by Mediatek Inc.
                    new String[] {Data.DATA1, Phone.TYPE, Data.CONTACT_ID, Data.IS_PRIMARY},
                    // The previous lines are provided and maintained by Mediatek Inc.
                    where.toString(), null, Data.CONTACT_ID + " ASC ");
            if (cursor != null) {
                long candidateContactId = -1;
                String candidateAddress = "";
                // The following lines are provided and maintained by Mediatek Inc.
                int isDefault = 0;
                // The previous lines are provided and maintained by Mediatek Inc.
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(2);
                    if (allContacts.contains(id)) {
                        allContacts.remove(id);
                    }
                    int type = cursor.getInt(1);
                    String email = cursor.getString(0);
                 // The following lines are provided and maintained by Mediatek Inc.
                    isDefault = cursor.getInt(3);
                 // The previous lines are provided and maintained by Mediatek Inc.
                    if (candidateContactId == -1) {
                        candidateContactId = id;
                        candidateAddress = email;
                    } else {
                        if (candidateContactId != id) {
                            if (candidateAddress != null && candidateAddress.length() > 0) {
                                if (builder.length() > 0) {
                                    builder.append(",");
                                }
                                builder.append(candidateAddress);
                            }
                            candidateContactId = id;
                            candidateAddress = email;
                        // The following lines are provided and maintained by Mediatek Inc.
                        } else if (isDefault == 1) {
                            candidateContactId = id;
                            candidateAddress = email;
                        }
                        // The previous lines are provided and maintained by Mediatek Inc.
                    }
                    if (cursor.isLast()) {
                        if (candidateAddress != null && candidateAddress.length() > 0) {
                            if (builder.length() > 0) {
                                builder.append(",");
                            }
                            builder.append(candidateAddress);
                        }
                    }
                }
                cursor.close();
            }
            Log.i(TAG, "[getEmailAddressFromGroup]builder String:" + builder.toString());
            return showNoTelphoneOrEmailToast(context, builder, resolver,
                    allContacts, "email");
        }
    }

    private StringBuilder getWhere(ContentResolver resolver, long groupId,
            HashSet<Long> allContacts) {
        Cursor contactCursor = resolver.query(Data.CONTENT_URI,
                new String[] {Data.CONTACT_ID},
                Data.MIMETYPE + "=? AND " + GroupMembership.GROUP_ROW_ID + "=?",
                new String[] {GroupMembership.CONTENT_ITEM_TYPE, String.valueOf(groupId)}, null);
        StringBuilder ids = new StringBuilder();
        if (contactCursor != null) {
            Log.d(TAG, "contactCusor count:" + contactCursor.getCount());
            while (contactCursor.moveToNext()) {
                Long contactId = contactCursor.getLong(0);
                if (!allContacts.contains(contactId)) {
                    ids.append(contactId).append(",");
                    allContacts.add(contactId);
                }
            }
            contactCursor.close();
        }
        StringBuilder where = new StringBuilder();
        if (ids.length() > 0) {
            ids.deleteCharAt(ids.length() - 1);
            where.append(Data.CONTACT_ID + " IN (");
            where.append(ids.toString());
            where.append(")");
        } else {
            return null;
        }
        where.append(" AND ");
        return where;
    }

    @Override
    public void onDestroy() {
        PhbStateHandler.getInstance().unRegister(this);
        super.onDestroy();
        //MTK Window leak happend in case async task still under running@{
        if (mProgressDialog != null) {
            ProgressDialog progress = mProgressDialog.get();
            if (progress != null && progress.isShowing()) {
                Log.i(TAG, "Dismiss the dialog");
                progress.dismiss();
            }
        }
        //@}
    }

    /** M: Bug Fix for CR ALPS00463033 @{ */
    private static MyProgressDialog sProgressDialog;

    public void showDialog() {
        sProgressDialog = new MyProgressDialog();
        sProgressDialog.setTargetFragment(GroupDetailFragment.this, 0);

        if (GroupDetailFragment.this.getFragmentManager() != null) {
            sProgressDialog.show(GroupDetailFragment.this.getFragmentManager(), "wait");
            sProgressDialog.mIsDismiss = false;
            sProgressDialog.setCancelable(false);
        }
    }

    public void dismissDialog() {
        if (sProgressDialog != null && sProgressDialog.getDialog() != null
                && sProgressDialog.getDialog().isShowing()) {
            if (sProgressDialog.mShouldDismiss) {
                sProgressDialog.mIsDismiss = true;
                return;
            }
            /// M: Bug fix ALPS01952840, change dismiss() -> dismissAllowingStateLoss().
            if (sProgressDialog.getTargetFragment() != null
                    && sProgressDialog.getTargetFragment().isResumed()) {
                sProgressDialog.dismissAllowingStateLoss();
                sProgressDialog.mIsDismiss = false;
            }
        }
    }
    public static class MyProgressDialog extends DialogFragment {
        private boolean mIsDismiss = false;
        private boolean mShouldDismiss = false;
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            ProgressDialog dialog = new ProgressDialog(getActivity());
            dialog.setMessage(getActivity().getString(R.string.please_wait));
            sProgressDialog = this;
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }
        @Override
        public void onPause() {
            super.onPause();
            mShouldDismiss = true;
        }
        @Override
        public void onResume() {
            super.onResume();
            mShouldDismiss = false;
            if (mIsDismiss) {
                if (sProgressDialog != null && sProgressDialog.getDialog() != null
                        && sProgressDialog.getDialog().isShowing()) {
                    sProgressDialog.dismiss();
                    mIsDismiss = false;
                }
            }
        }
    }

    /** @} */
    // The previous lines are provided and maintained by Mediatek Inc.
    /*
     * Bug Fix by Mediatek Begin. Original Android's code: CR ID: ALPS00115673
     * Descriptions: add wait cursor
     */
    private long OCL;
    private long OLF;
    private long OCL1;
    private long OLF1;
    private TextView mLoadingContact;

    private ProgressBar mProgress;

    private View mLoadingContainer;

    private static boolean isFinished = false;
    private WaitCursorView mWaitCursorView;
    private class GroupDeleteHandler extends Handler implements DeleteEndListener {

        private static final int DELETE_START = 0;
        private static final int DELETE_END = 1;
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DELETE_START:
                    /** M: Bug Fix for ALPS00694524 @{ */
                    if (!isAdded()) {
                        Log.w(TAG, "The fragment is not attached to  Activity.");
                        /** @} */
                        /** M: fixed cr ALPS00748882 @{ */
                    } else if (!isFragmentVisible()) {
                        Log.w(TAG, "The Fragment is inVisible!");
                        /** @} */
                    } else {
                        showDialog();
                    }
                    break;
                case DELETE_END:
                    dismissDialog();
                    /** M: Bug Fix for ALPS00796572 @{ */
                    if (null != getActivity()
                            && PhoneCapabilityTester.isUsingTwoPanes(getActivity())) {
                        Log.w(TAG, "The GroupDetailActivity is not Active with tablet");
                    /** @} */
                    } else {
                        if (getActivity() != null) {
                            getActivity().finish();
                        }
                    }
                    break;
                default:
                    Log.w(TAG, "[handleMessage] unexpected message: " + msg.what);
                    break;
            }
        }
        @Override
        public void onDeleteEnd() {
            sendEmptyMessage(DELETE_END);
        }
        @Override
        public void onDeleteStart() {
            sendEmptyMessage(DELETE_START);
        }
    }
    private DeleteEndListener mDeleteEndListener = new GroupDeleteHandler();
    /*
     * Bug Fix by Mediatek End.
     */

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ContactSaveService.removeDeleteEndListener(mDeleteEndListener);
    }

    /**
     * M: To get all sole raw_contact_id of common contact and joined contacts.
     * @param resolver
     * @param ids
     * @return the best available rawcontactId for next query.
     */
    private StringBuilder getAllRawContactIds(ContentResolver resolver, StringBuilder ids) {

        StringBuilder whereId = new StringBuilder();
        StringBuilder rawContactIds = new StringBuilder();

        whereId.append(Contacts._ID + " IN(");
        whereId.append(ids.toString());
        whereId.append(")");

        Log.i(TAG, "[getAllRawContactIds]query allrawcontactids cursor selection:"
                        + whereId.toString());

        /** to query all name_raw_contact_id of contacts table. */
        Cursor cursor = resolver.query(Contacts.CONTENT_URI,
                new String[] { Contacts.Entity.NAME_RAW_CONTACT_ID }, whereId.toString(), null,
                null);
        HashSet<Long> rawContactIdSet = new HashSet<Long>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Long rawContactId = cursor.getLong(0);
                rawContactIdSet.add(rawContactId);
            }
            cursor.close();
        }

        /** to build a raw_contact_id stringbuilder. */
        if (!rawContactIdSet.isEmpty()) {
            Long[] allRawContactsIdArray = rawContactIdSet.toArray(new Long[0]);
            for (Long id : allRawContactsIdArray) {
                if (rawContactIds.length() > 0) {
                    rawContactIds.append(",");
                }
                rawContactIds.append(id.toString());
            }
        }

        return rawContactIds;
    }

    /**
     * M: fixed cr ALPS00748882 @{
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        mIsVisible = false;
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mIsVisible = true;
    }

    @Override
    public void onStop() {
        mIsVisible = false;
        super.onStop();
    }

    public boolean isFragmentVisible() {
        return mIsVisible;
    }

    private boolean mIsVisible = false;
    /** @} */

    @Override
    public void onStart() {
        super.onStart();
        mAdapter.notifyDataSetChanged();
    }

    /// M: For move to other groups feature.
    private void updateMoveMenuStatus() {
        if (mAccountGroupMemberCount <= 1) {
            DISABLE_MOVE_MENU = true;
        }
    }

    /** M: Bug Fix for ALPS01594286 @{ */
    private void needcheckGroupData(MenuItem editMenu, MenuItem deleteMenu) {
        boolean removeBtn = false;
        final Cursor cursor = mContext.getContentResolver().query(
                Groups.CONTENT_URI,
                new String[] {
                        Groups._ID, Groups.TITLE
                },
                            Groups.DELETED + "=0 "
                                    + "AND " + Groups.ACCOUNT_NAME + "= '" + mAccountName
                                    + "'", null, null);

        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            if (mGroupId == cursor.getLong(0)) {
                break;
            }
        }

        if (cursor.isAfterLast() == true) {
            removeBtn = true;
        }
        cursor.close();
        if (removeBtn) {
            MyMenu mMenu = new MyMenu();
            mMenu.mDelte = deleteMenu;
            mMenu.mEdit = editMenu;
            mMyHandler.sendMessage(mMyHandler.obtainMessage(mMenuMessage, mMenu));
        }
    }
    private int mMenuMessage = 8123;

    /**
     * A private class for same Edit/Delete menu.
     */
    private class MyMenu {
        private MenuItem mEdit;
        private MenuItem mDelte;
    }

    private Handler mMyHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (null != msg && mMenuMessage == msg.what) {
                MyMenu myMenu = (MyMenu) msg.obj;
                myMenu.mDelte.setVisible(false);
                myMenu.mEdit.setVisible(false);
            }
        }
    };

    /// M: add for sim contact
    @Override
    public void onPhbStateChange(int subId) {
        if (subId == mSubId) {
            Log.d(TAG , "onReceive,subId:" + subId + ",finish Group EditorActivity.");
            getActivity().finish();
            return;
        }
    }
    /** @} */
}
