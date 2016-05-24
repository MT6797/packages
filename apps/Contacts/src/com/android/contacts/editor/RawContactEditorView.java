/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.contacts.editor;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mediatek.contacts.ExtensionManager;
import com.android.contacts.GroupMetaDataLoader;
import com.android.contacts.R;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.RawContactModifier;
import com.android.contacts.common.model.ValuesDelta;

import com.google.common.base.Objects;

import com.mediatek.contacts.editor.ContactEditorUtilsEx;
import com.mediatek.contacts.ext.IAasExtension;
import com.mediatek.contacts.model.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.Log;

import java.util.ArrayList;

/**
 * Custom view that provides all the editor interaction for a specific
 * {@link Contacts} represented through an {@link RawContactDelta}. Callers can
 * reuse this view and quickly rebuild its contents through
 * {@link #setState(RawContactDelta, AccountType, ViewIdGenerator)}.
 * <p>
 * Internal updates are performed against {@link ValuesDelta} so that the
 * source {@link RawContact} can be swapped out. Any state-based changes, such as
 * adding {@link Data} rows or changing {@link EditType}, are performed through
 * {@link RawContactModifier} to ensure that {@link AccountType} are enforced.
 */
public class RawContactEditorView extends BaseRawContactEditorView {
    private static final String KEY_SUPER_INSTANCE_STATE = "superInstanceState";

    private LayoutInflater mInflater;

    private StructuredNameEditorView mName;
    private PhoneticNameEditorView mPhoneticName;
    private TextFieldsEditorView mNickName;

    private GroupMembershipView mGroupMembershipView;

    private ViewGroup mFields;

    private View mAccountSelector;
    private TextView mAccountSelectorTypeTextView;
    private TextView mAccountSelectorNameTextView;

    private View mAccountHeader;
    private TextView mAccountHeaderTypeTextView;
    private TextView mAccountHeaderNameTextView;

    private long mRawContactId = -1;
    private boolean mAutoAddToDefaultGroup = true;
    private Cursor mGroupMetaData;
    private DataKind mGroupMembershipKind;
    private RawContactDelta mState;

    public RawContactEditorView(Context context) {
        super(context);
    }

    public RawContactEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        View view = getPhotoEditor();
        if (view != null) {
            view.setEnabled(enabled);
        }

        if (mName != null) {
            mName.setEnabled(enabled);
        }

        if (mPhoneticName != null) {
            mPhoneticName.setEnabled(enabled);
        }

        if (mFields != null) {
            int count = mFields.getChildCount();
            for (int i = 0; i < count; i++) {
                mFields.getChildAt(i).setEnabled(enabled);
            }
        }

        if (mGroupMembershipView != null) {
            mGroupMembershipView.setEnabled(enabled);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mInflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mName = (StructuredNameEditorView)findViewById(R.id.edit_name);
        mName.setDeletable(false);

        mPhoneticName = (PhoneticNameEditorView)findViewById(R.id.edit_phonetic_name);
        mPhoneticName.setDeletable(false);

        mNickName = (TextFieldsEditorView)findViewById(R.id.edit_nick_name);

        mFields = (ViewGroup)findViewById(R.id.sect_fields);

        mAccountHeader = findViewById(R.id.account_header_container);
        mAccountHeaderTypeTextView = (TextView) findViewById(R.id.account_type);
        mAccountHeaderNameTextView = (TextView) findViewById(R.id.account_name);

        mAccountSelector = findViewById(R.id.account_selector_container);
        mAccountSelectorTypeTextView = (TextView) findViewById(R.id.account_type_selector);
        mAccountSelectorNameTextView = (TextView) findViewById(R.id.account_name_selector);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        // super implementation of onSaveInstanceState returns null
        bundle.putParcelable(KEY_SUPER_INSTANCE_STATE, super.onSaveInstanceState());
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            super.onRestoreInstanceState(bundle.getParcelable(KEY_SUPER_INSTANCE_STATE));
            return;
        }
        super.onRestoreInstanceState(state);
    }

    /**
     * Set the internal state for this view, given a current
     * {@link RawContactDelta} state and the {@link AccountType} that
     * apply to that state.
     */
    @Override
    public void setState(RawContactDelta state, AccountType type, ViewIdGenerator vig,
            boolean isProfile) {

        Log.d(TAG, "[setState]state: " + state + ", AccountType: " + type + ", isProfile: "
                + isProfile);
        mState = state;

        // Remove any existing sections
        mFields.removeAllViews();

        // Bail if invalid state or account type
        if (state == null || type == null) return;

        setId(vig.getId(state, null, null, ViewIdGenerator.NO_VIEW_INDEX));

        // Make sure we have a StructuredName
        RawContactModifier.ensureKindExists(state, type, StructuredName.CONTENT_ITEM_TYPE);

        mRawContactId = state.getRawContactId();

        /// M: For displaying SIM name. @{
        String accountName = state.getValues().getAsString(RawContacts.ACCOUNT_NAME);
        if (type.isIccCardAccount()) {
            mSubId = AccountTypeUtils.getSubIdBySimAccountName(mContext, accountName);
            Log.d(TAG, "[setState]got subId " + mSubId + " from AccountName: " + accountName);
            if (SubInfoUtils.getDisplaynameUsingSubId(mSubId) != null) {
                accountName = SubInfoUtils.getDisplaynameUsingSubId(mSubId);
                Log.d(TAG, "[setState]accountName: " + accountName);
            }
            /// M: OP09 icon replace.
            ContactEditorUtilsEx.setDefaultIconForEditor(mSubId, getPhotoEditor());
            /// M: Bug fix ALPS01413181
            if (ContactEditorUtilsEx.finishActivityIfInvalidSubId(getContext(), mSubId)) {
                Log.d(TAG, "[setState]finishActivityIfInvalidSubId,return.");
                return;
            }
        }
        /// @}

        ///M:
        int activatedSubInfoCount = SubInfoUtils.getActivatedSubInfoCount();
        Log.d(TAG,"[setState]activatedSubInfoCount = " + activatedSubInfoCount
                + ",accountType = " + type.accountType);

        // Fill in the account info
        final Pair<String,String> accountInfo = EditorUiUtils.getAccountInfo(getContext(),
                isProfile, state.getAccountName(), type);
        if (accountInfo == null) {
            // Hide this view so the other text view will be centered vertically
            mAccountHeaderNameTextView.setVisibility(View.GONE);
        } else {
            if (accountInfo.first == null) {
                mAccountHeaderNameTextView.setVisibility(View.GONE);
            } else {
                mAccountHeaderNameTextView.setVisibility(View.VISIBLE);
                mAccountHeaderNameTextView.setText(accountInfo.first);
            }
            /// M: Modify for SIM indicator feature. @{
            if (AccountTypeUtils.isAccountTypeIccCard(type.accountType)) {
                if(activatedSubInfoCount <= 1){
                    mAccountHeaderNameTextView.setVisibility(View.GONE);
                } else {
                if (accountInfo.first != null) {
                    int subId = AccountTypeUtils.getSubIdBySimAccountName(getContext(),
                            accountInfo.first);
                    String account_name = SubInfoUtils.getDisplaynameUsingSubId(subId);
                    mAccountHeaderNameTextView.setText(account_name);
                }
            }
            }
          /// @}

            /// M: Bug fix ALPS00453091, if local phone account, set string to "Phone contact" @{
            if (AccountWithDataSetEx.isLocalPhone(type.accountType)) {
                CharSequence accountType = type.getDisplayLabel(mContext);
                if (TextUtils.isEmpty(accountType)) {
                    accountType = mContext.getString(R.string.account_phone_only);
                }
                mAccountHeaderTypeTextView.setText(accountType);
            } else {
                mAccountHeaderTypeTextView.setText(accountInfo.second);
            }
            // @}
        }

        updateAccountHeaderContentDescription();

        // The account selector and header are both used to display the same information.
        mAccountSelectorTypeTextView.setText(mAccountHeaderTypeTextView.getText());
        mAccountSelectorTypeTextView.setVisibility(mAccountHeaderTypeTextView.getVisibility());
        mAccountSelectorNameTextView.setText(mAccountHeaderNameTextView.getText());

        /// M: Modify for SIM indicator feature. @{
        if (AccountTypeUtils.isAccountTypeIccCard(type.accountType)) {
            if(activatedSubInfoCount <= 1){
            mAccountSelectorNameTextView.setVisibility(View.GONE);
        } else {
                mAccountSelectorNameTextView.setVisibility(
                        mAccountHeaderNameTextView.getVisibility());
            }
        }
        /// @}

        // Showing the account header at the same time as the account selector drop down is
        // confusing. They should be mutually exclusive.
        mAccountHeader.setVisibility(mAccountSelector.getVisibility() == View.GONE
                ? View.VISIBLE : View.GONE);

        // Show photo editor when supported
        RawContactModifier.ensureKindExists(state, type, Photo.CONTENT_ITEM_TYPE);
        setHasPhotoEditor((type.getKindForMimetype(Photo.CONTENT_ITEM_TYPE) != null));
        getPhotoEditor().setEnabled(isEnabled());
        mName.setEnabled(isEnabled());

        mPhoneticName.setEnabled(isEnabled());

        // Show and hide the appropriate views
        mFields.setVisibility(View.VISIBLE);
        mName.setVisibility(View.VISIBLE);
        mPhoneticName.setVisibility(View.VISIBLE);

        /// M:AAS[COMMD_FOR_AAS] mPhoneticName -> GONE.
        ExtensionManager.getInstance().getAasExtension()
                .updateView(state, mPhoneticName, null, IAasExtension.VIEW_UPDATE_VISIBILITY);

        mGroupMembershipKind = type.getKindForMimetype(GroupMembership.CONTENT_ITEM_TYPE);
        if (mGroupMembershipKind != null) {
            mGroupMembershipView = (GroupMembershipView)mInflater.inflate(
                    R.layout.item_group_membership, mFields, false);
            mGroupMembershipView.setKind(mGroupMembershipKind);
            mGroupMembershipView.setEnabled(isEnabled());
        }

        // Create editor sections for each possible data kind
        for (DataKind kind : type.getSortedDataKinds()) {
            // Skip kind of not editable
            if (!kind.editable) continue;

            final String mimeType = kind.mimeType;
            if (StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)) {
                // Handle special case editor for structured name
                final ValuesDelta primary = state.getPrimaryEntry(mimeType);
                mName.setValues(
                        type.getKindForMimetype(DataKind.PSEUDO_MIME_TYPE_DISPLAY_NAME),
                        primary, state, false, vig);
                mPhoneticName.setValues(
                        type.getKindForMimetype(DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME),
                        primary, state, false, vig);
                // It is useful to use Nickname outside of a KindSectionView so that we can treat it
                // as a part of StructuredName's fake KindSectionView, even though it uses a
                // different CP2 mime-type. We do a bit of extra work below to make this possible.
                final DataKind nickNameKind = type.getKindForMimetype(Nickname.CONTENT_ITEM_TYPE);
                if (nickNameKind != null) {
                    ValuesDelta primaryNickNameEntry = state.getPrimaryEntry(nickNameKind.mimeType);
                    if (primaryNickNameEntry == null) {
                        primaryNickNameEntry = RawContactModifier.insertChild(state, nickNameKind);
                    }
                    mNickName.setValues(nickNameKind, primaryNickNameEntry, state, false, vig);
                    mNickName.setDeletable(false);
                } else {
                    mPhoneticName.setPadding(0, 0, 0, (int) getResources().getDimension(
                            R.dimen.editor_padding_between_editor_views));
                    mNickName.setVisibility(View.GONE);
                }
            } else if (Photo.CONTENT_ITEM_TYPE.equals(mimeType)) {
                // Handle special case editor for photos
                final ValuesDelta primary = state.getPrimaryEntry(mimeType);
                getPhotoEditor().setValues(kind, primary, state, false, vig);
                Log.d(TAG, "set photo, primary: " + primary);
            } else if (GroupMembership.CONTENT_ITEM_TYPE.equals(mimeType)) {
                if (mGroupMembershipView != null) {
                    mGroupMembershipView.setState(state);
                    /// M: Bug Fix for ALPS00440157, add isProfile check.
                    if (!isProfile) {
                        mFields.addView(mGroupMembershipView);
                    }
                }
            /// M: Bug fix ALPS00566570,some USIM card do not support storing Email address. @{
            } else if (Email.CONTENT_ITEM_TYPE.equals(mimeType) && type.isUSIMAccountType()
                    && SimCardUtils.getIccCardEmailCount(mSubId) <= 0) {
                Log.d(TAG, "[setState] It's USIM account and no Email field in subId: " + mSubId);
                /// M: Bug fix ALPS01583209, the state may already have email entry when switch
                //  from AccountSwitcher, here need to clear email address entry.
                if (state.hasMimeEntries(Email.CONTENT_ITEM_TYPE)) {
                    state.removeEntry(Email.CONTENT_ITEM_TYPE);
                }
                continue;
            /// @}
            } else if (DataKind.PSEUDO_MIME_TYPE_DISPLAY_NAME.equals(mimeType)
                    || DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME.equals(mimeType)
                    || Nickname.CONTENT_ITEM_TYPE.equals(mimeType)) {
                // Don't create fields for each of these mime-types. They are handled specially.
                continue;
            } else {
                // Otherwise use generic section-based editors
                if (kind.fieldList == null) continue;
                final KindSectionView section = (KindSectionView)mInflater.inflate(
                        R.layout.item_kind_section, mFields, false);
                section.setShowOneEmptyEditor(true);
                section.setEnabled(isEnabled());
                section.setState(kind, state, /* readOnly =*/ false, vig);
                mFields.addView(section);
            }
        }
        Log.d(TAG, "[setState]addToDefaultGroupIfNeeded. ");
        addToDefaultGroupIfNeeded();
    }

    @Override
    public void setGroupMetaData(Cursor groupMetaData) {
        mGroupMetaData = groupMetaData;
        addToDefaultGroupIfNeeded();
        if (mGroupMembershipView != null) {
            mGroupMembershipView.setGroupMetaData(groupMetaData);
        }
    }

    /**
     * M: For sim/usim contact.
     */
    @Override
    public void setSubId(int subId) {
        if (mGroupMembershipView != null) {
            mGroupMembershipView.setSubId(subId);
        }
    }

    public void setAutoAddToDefaultGroup(boolean flag) {
        this.mAutoAddToDefaultGroup = flag;
    }

    /**
     * If automatic addition to the default group was requested (see
     * {@link #setAutoAddToDefaultGroup}, checks if the raw contact is in any
     * group and if it is not adds it to the default group (in case of Google
     * contacts that's "My Contacts").
     */
    private void addToDefaultGroupIfNeeded() {
        if (!mAutoAddToDefaultGroup || mGroupMetaData == null || mGroupMetaData.isClosed()
                || mState == null) {
            return;
        }

        boolean hasGroupMembership = false;
        ArrayList<ValuesDelta> entries = mState.getMimeEntries(GroupMembership.CONTENT_ITEM_TYPE);
        if (entries != null) {
            for (ValuesDelta values : entries) {
                Long id = values.getGroupRowId();
                if (id != null && id.longValue() != 0) {
                    hasGroupMembership = true;
                    break;
                }
            }
        }

        if (!hasGroupMembership) {
            long defaultGroupId = getDefaultGroupId();
            if (defaultGroupId != -1) {
                ValuesDelta entry = RawContactModifier.insertChild(mState, mGroupMembershipKind);
                if (entry != null) {
                    entry.setGroupRowId(defaultGroupId);
                }
            }
        }
    }

    /**
     * Returns the default group (e.g. "My Contacts") for the current raw contact's
     * account.  Returns -1 if there is no such group.
     */
    private long getDefaultGroupId() {
        String accountType = mState.getAccountType();
        String accountName = mState.getAccountName();
        String accountDataSet = mState.getDataSet();
        mGroupMetaData.moveToPosition(-1);
        while (mGroupMetaData.moveToNext()) {
            String name = mGroupMetaData.getString(GroupMetaDataLoader.ACCOUNT_NAME);
            String type = mGroupMetaData.getString(GroupMetaDataLoader.ACCOUNT_TYPE);
            String dataSet = mGroupMetaData.getString(GroupMetaDataLoader.DATA_SET);
            if (name.equals(accountName) && type.equals(accountType)
                    && Objects.equal(dataSet, accountDataSet)) {
                long groupId = mGroupMetaData.getLong(GroupMetaDataLoader.GROUP_ID);
                if (!mGroupMetaData.isNull(GroupMetaDataLoader.AUTO_ADD)
                            && mGroupMetaData.getInt(GroupMetaDataLoader.AUTO_ADD) != 0) {
                    return groupId;
                }
            }
        }
        return -1;
    }

    public StructuredNameEditorView getNameEditor() {
        return mName;
    }

    public TextFieldsEditorView getPhoneticNameEditor() {
        return mPhoneticName;
    }

    public TextFieldsEditorView getNickNameEditor() {
        return mNickName;
    }

    @Override
    public long getRawContactId() {
        return mRawContactId;
    }

    /** M: For sim/usim contact and bug fix. @{ */
    private static final String TAG = "RawContactEditorView";
    private int mSubId = SubInfoUtils.getInvalidSubId();
    /** @} **/
}
