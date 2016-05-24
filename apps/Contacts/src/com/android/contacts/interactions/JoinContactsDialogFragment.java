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

package com.android.contacts.interactions;


import com.android.contacts.ContactSaveService;
import com.android.contacts.R;

import com.mediatek.contacts.util.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.DialogInterface.OnDismissListener;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.RawContacts;

import java.util.TreeSet;

/**
 * An interaction invoked to join multiple contacts together.
 */
public class JoinContactsDialogFragment extends Fragment
        implements LoaderCallbacks<Cursor> {

    private static final String FRAGMENT_TAG = "joinDialog";
    private static final String KEY_CONTACT_IDS = "contactIds";
    private static final String TAG = "JoinContactsDialogFragment";

    private boolean mIsLoaderActive;
    private TreeSet<Long> mContactIds;
    private Context mContext;
    private AlertDialog mDialog;

    private boolean mIsMergeSimContacts = false;

    public interface JoinContactsListener {
        void onContactsJoined();
    }

/* MTK no need
    public static void start(Activity activity, TreeSet<Long> contactIds) {
        final FragmentTransaction ft = activity.getFragmentManager().beginTransaction();
        final JoinContactsDialogFragment newFragment
                = JoinContactsDialogFragment.newInstance(contactIds);
        newFragment.show(ft, FRAGMENT_TAG);
    }

    private static JoinContactsDialogFragment newInstance(TreeSet<Long> contactIds) {
        final JoinContactsDialogFragment fragment = new JoinContactsDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putSerializable(KEY_CONTACT_IDS, contactIds);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final TreeSet<Long> contactIds =
                (TreeSet<Long>) getArguments().getSerializable(KEY_CONTACT_IDS);
        if (contactIds.size() <= 1) {
            return new AlertDialog.Builder(getActivity())
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(R.string.batch_merge_single_contact_warning)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        }
        return new AlertDialog.Builder(getActivity())
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(R.string.batch_merge_confirmation)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                joinContacts(contactIds);
                            }
                        }
                )
                .create();
    }
*/
    private void joinContacts(TreeSet<Long> contactIds) {
        final Long[] contactIdsArray = contactIds.toArray(new Long[contactIds.size()]);
        final long[] contactIdsArray2 = new long[contactIdsArray.length];
        for (int i = 0; i < contactIds.size(); i++) {
            contactIdsArray2[i] = contactIdsArray[i];
        }

        final Intent intent = ContactSaveService.createJoinSeveralContactsIntent(getActivity(),
                contactIdsArray2);
        getActivity().startService(intent);

        notifyListener();
    }

    private void notifyListener() {
        if (getActivity() instanceof JoinContactsListener) {
            ((JoinContactsListener) getActivity()).onContactsJoined();
        }
    }

    /**
     * Starts the fragment
     *
     * @param activity the activity within which to start the interaction
     * @param contactIds the IDs of contacts to be deleted
     * @return the newly created fragment
     */
    public static JoinContactsDialogFragment start(Activity activity, TreeSet<Long> contactIds) {
        if (contactIds == null) {
            Log.w(TAG, "[start]contactIds is null,return.");
            return null;
        }
        final FragmentManager fragmentManager = activity.getFragmentManager();
        JoinContactsDialogFragment fragment =
                (JoinContactsDialogFragment) fragmentManager.findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            Log.i(TAG, "[start]new JoinContactsDialogFragment");
            fragment = new JoinContactsDialogFragment();
            fragment.setContactIds(contactIds);
            fragmentManager.beginTransaction().add(fragment, FRAGMENT_TAG)
                    .commitAllowingStateLoss();
        } else {
            fragment.setContactIds(contactIds);
        }

        return fragment;
    }

    public void setContactIds(TreeSet<Long> contactIds) {
        Log.i(TAG, "[setContactIds]...");
        mContactIds = contactIds;
        mIsLoaderActive = true;
        if (isStarted()) {
            Log.i(TAG, "[setContactIds]isStarted.");
            Bundle args = new Bundle();
            args.putSerializable(KEY_CONTACT_IDS, mContactIds);
            getLoaderManager().restartLoader(
                    R.id.dialog_join_multiple_contact_loader_id, args, this);
        }
    }

    private boolean isStarted() {
        return isAdded();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        Log.i(TAG, "[onAttach].");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.setOnDismissListener(null);
            mDialog.dismiss();
            mDialog = null;
        }
    }

    @Override
    public void onStart() {
        Log.i(TAG, "[onStart]mIsLoaderActive = " + mIsLoaderActive + ",mIsMergeSimContacts ="
                + mIsMergeSimContacts);
        if (mIsLoaderActive && !mIsMergeSimContacts) {
            Bundle args = new Bundle();
            args.putSerializable(KEY_CONTACT_IDS, mContactIds);
            getLoaderManager().initLoader(
                    R.id.dialog_join_multiple_contact_loader_id, args, this);
        }
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mDialog != null) {
            mDialog.hide();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.i(TAG, "[onCreateLoader]...");

        final TreeSet<Long> contactIds = (TreeSet<Long>) args.getSerializable(KEY_CONTACT_IDS);
        final Object[] parameterObject = contactIds.toArray();
        final StringBuilder builder = new StringBuilder();

        /// M: fixed only can delete 1000 contacts issue
        builder.append(RawContacts.CONTACT_ID + " IN (");
        for (int i = 0; i < contactIds.size(); i++) {
            builder.append(String.valueOf(parameterObject[i]));
            if (i < contactIds.size() - 1) {
                builder.append(",");
            }
        }
        builder.append(")");

        return new CursorLoader(mContext, RawContacts.CONTENT_URI, new String[] {
                    "index_in_sim"}, builder.toString(), null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.i(TAG, "[onLoadFinished]...");

        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }

        if (!mIsLoaderActive) {
            Log.e(TAG, "[onLoadFinished]mIsLoaderActive is false, return!");
            return;
        }

        if (cursor == null || cursor.isClosed()) {
            Log.e(TAG, "Failed to load contacts");
            return;
        }
        int messageId = R.string.batch_merge_single_contact_warning;
        int index_in_sim = -1;
        for (int j = 0; j < cursor.getCount(); j++) {
            cursor.moveToPosition(j);
            index_in_sim = cursor.getInt(0);
            Log.d(TAG, "onLoadFinshed index_in_sim = " + index_in_sim);
            if (index_in_sim > 0) {
                messageId = R.string.batch_merge_sim_contacts_warning;
                mIsMergeSimContacts = true;
                break;
            }
        }
        ///M: If user select ICCcard contacts,
        /// we show AlertDialog to remind user can't use Merge/Join function
        if (cursor.getCount() <= 1 || index_in_sim > 0) {
            showDialog(messageId, false);
        } else {
            mIsMergeSimContacts = false;
            messageId = R.string.batch_merge_confirmation;
            showDialog(messageId, true);
        }

        // We don't want onLoadFinished() calls any more, which may come when the database is
        // updating.
        getLoaderManager().destroyLoader(R.id.dialog_join_multiple_contact_loader_id);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
    ///M:remove the contactIds parameter
    private void showDialog(int messageId, boolean showNegativeButton) {
        if (showNegativeButton) {
            mDialog = new AlertDialog.Builder(getActivity())
            .setMessage(messageId)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    joinContacts(mContactIds);
                }
            }).create();
        } else {
            mDialog =  new AlertDialog.Builder(getActivity())
            .setMessage(messageId)
            .setPositiveButton(android.R.string.ok, null)
            .create();
        }

        mDialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mIsLoaderActive = false;
                mDialog = null;
            }
        });
        mDialog.show();
    }
}
