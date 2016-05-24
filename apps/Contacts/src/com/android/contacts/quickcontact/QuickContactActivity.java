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

package com.android.contacts.quickcontact;

import android.accounts.Account;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Trace;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Identity;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Relation;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.DisplayNameSources;
import android.provider.ContactsContract.DataUsageFeedback;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.QuickContact;
import android.provider.ContactsContract.RawContacts;
import android.support.v7.graphics.Palette;
import android.telecom.PhoneAccount;
import android.telecom.TelecomManager;
import android.text.BidiFormatter;
import android.text.SpannableString;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.Toolbar;

import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsActivity;
import com.android.contacts.ContactsApplication;
import com.android.contacts.NfcHandler;
import com.android.contacts.R;
import com.android.contacts.activities.PeopleActivity;
import com.android.contacts.common.CallUtil;
import com.android.contacts.common.ClipboardUtils;
import com.android.contacts.common.Collapser;
import com.android.contacts.common.ContactsUtils;
import com.android.contacts.common.activity.RequestPermissionsActivity;
import com.android.contacts.common.editor.SelectAccountDialogFragment;
import com.android.contacts.common.interactions.TouchPointManager;
import com.android.contacts.common.lettertiles.LetterTileDrawable;
import com.android.contacts.common.list.ShortcutIntentBuilder;
import com.android.contacts.common.list.ShortcutIntentBuilder.OnShortcutIntentCreatedListener;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.Contact;
import com.android.contacts.common.model.ContactLoader;
import com.android.contacts.common.model.RawContact;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.model.dataitem.DataItem;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.common.model.dataitem.EmailDataItem;
import com.android.contacts.common.model.dataitem.EventDataItem;
import com.android.contacts.common.model.dataitem.GroupMembershipDataItem;
import com.android.contacts.common.model.dataitem.ImDataItem;
import com.android.contacts.common.model.dataitem.NicknameDataItem;
import com.android.contacts.common.model.dataitem.NoteDataItem;
import com.android.contacts.common.model.dataitem.OrganizationDataItem;
import com.android.contacts.common.model.dataitem.PhoneDataItem;
import com.android.contacts.common.model.dataitem.RelationDataItem;
import com.android.contacts.common.model.dataitem.SipAddressDataItem;
import com.android.contacts.common.model.dataitem.StructuredNameDataItem;
import com.android.contacts.common.model.dataitem.StructuredPostalDataItem;
import com.android.contacts.common.model.dataitem.WebsiteDataItem;
import com.android.contacts.common.util.Constants;
import com.android.contacts.common.util.ImplicitIntentsUtil;
import com.android.contacts.common.util.DateUtils;
import com.android.contacts.common.util.MaterialColorMapUtils;
import com.android.contacts.common.util.MaterialColorMapUtils.MaterialPalette;
import com.android.contacts.common.util.ViewUtil;
import com.android.contacts.common.vcard.VCardCommonArguments;
import com.android.contacts.detail.ContactDisplayUtils;
import com.android.contacts.editor.ContactEditorFragment;
import com.android.contacts.editor.EditorIntents;
import com.android.contacts.interactions.CalendarInteractionsLoader;
import com.android.contacts.interactions.CallLogInteractionsLoader;
import com.android.contacts.interactions.ContactDeletionInteraction;
import com.android.contacts.interactions.ContactInteraction;
import com.android.contacts.interactions.SmsInteractionsLoader;
import com.android.contacts.quickcontact.ExpandingEntryCardView.Entry;
import com.android.contacts.quickcontact.ExpandingEntryCardView.EntryContextMenuInfo;
import com.android.contacts.quickcontact.ExpandingEntryCardView.EntryTag;
import com.android.contacts.quickcontact.ExpandingEntryCardView.ExpandingEntryCardViewListener;
import com.android.contacts.quickcontact.WebAddress.ParseException;
import com.android.contacts.util.ImageViewDrawableSetter;
import com.android.contacts.util.PhoneCapabilityTester;
import com.android.contacts.util.SchedulingUtils;
import com.android.contacts.util.StructuredPostalUtils;
import com.android.contacts.widget.MultiShrinkScroller;
import com.android.contacts.widget.MultiShrinkScroller.MultiShrinkScrollerListener;
import com.android.contacts.widget.QuickContactImageView;
import com.android.contactsbind.HelpUtils;

import com.google.common.collect.Lists;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.quickcontact.QuickContactUtils;
import com.mediatek.contacts.ContactsSystemProperties;
import com.mediatek.contacts.HotKnotHandler;
import com.mediatek.contacts.model.dataitem.ImsCallDataItem;
import com.mediatek.hotknot.HotKnotAdapter;
import com.mediatek.contacts.util.Log;

import java.lang.SecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mostly translucent {@link Activity} that shows QuickContact dialog. It loads
 * data asynchronously, and then shows a popup with details centered around
 * {@link Intent#getSourceBounds()}.
 */
public class QuickContactActivity extends ContactsActivity {

    /**
     * QuickContacts immediately takes up the full screen. All possible information is shown.
     * This value for {@link android.provider.ContactsContract.QuickContact#EXTRA_MODE}
     * should only be used by the Contacts app.
     */
    public static final int MODE_FULLY_EXPANDED = 4;

    private static final String TAG = "QuickContactActivity";

    private static final String KEY_THEME_COLOR = "theme_color";

    private static final int ANIMATION_STATUS_BAR_COLOR_CHANGE_DURATION = 150;
    private static final int REQUEST_CODE_CONTACT_EDITOR_ACTIVITY = 1;
    private static final int SCRIM_COLOR = Color.argb(0xC8, 0, 0, 0);
    private static final int REQUEST_CODE_CONTACT_SELECTION_ACTIVITY = 2;
    private static final String MIMETYPE_SMS = "vnd.android-dir/mms-sms";

    /** This is the Intent action to install a shortcut in the launcher. */
    private static final String ACTION_INSTALL_SHORTCUT =
            "com.android.launcher.action.INSTALL_SHORTCUT";

    @SuppressWarnings("deprecation")
    private static final String LEGACY_AUTHORITY = android.provider.Contacts.AUTHORITY;

    private static final String MIMETYPE_GPLUS_PROFILE =
            "vnd.android.cursor.item/vnd.googleplus.profile";
    private static final String GPLUS_PROFILE_DATA_5_ADD_TO_CIRCLE = "addtocircle";
    private static final String GPLUS_PROFILE_DATA_5_VIEW_PROFILE = "view";
    private static final String MIMETYPE_HANGOUTS =
            "vnd.android.cursor.item/vnd.googleplus.profile.comm";
    private static final String HANGOUTS_DATA_5_VIDEO = "hangout";
    private static final String HANGOUTS_DATA_5_MESSAGE = "conversation";
    private static final String CALL_ORIGIN_QUICK_CONTACTS_ACTIVITY =
            "com.android.contacts.quickcontact.QuickContactActivity";
    private static final int MENU_ITEM_PRINT = 1010;

    /**
     * The URI used to load the the Contact. Once the contact is loaded, use Contact#getLookupUri()
     * instead of referencing this URI.
     */
    private Uri mLookupUri;
    private String[] mExcludeMimes;
    private int mExtraMode;
    private String mExtraPrioritizedMimeType;
    private int mStatusBarColor;
    private boolean mHasAlreadyBeenOpened;
    private boolean mOnlyOnePhoneNumber;
    private boolean mOnlyOneEmail;

    private QuickContactImageView mPhotoView;
    private ExpandingEntryCardView mContactCard;
    /// M:[for RCS-e] show Joyn Card(rcs-e plugin) under ContactCard.
    private ExpandingEntryCardView mJoynCard;
    private ExpandingEntryCardView mNoContactDetailsCard;
    private ExpandingEntryCardView mRecentCard;
    private ExpandingEntryCardView mAboutCard;
    private MultiShrinkScroller mScroller;
    private SelectAccountDialogFragmentListener mSelectAccountFragmentListener;
    private AsyncTask<Void, Void, Cp2DataCardModel> mEntriesAndActionsTask;
    private AsyncTask<Void, Void, Void> mRecentDataTask;
    /**
     * The last copy of Cp2DataCardModel that was passed to {@link #populateContactAndAboutCard}.
     */
    private Cp2DataCardModel mCachedCp2DataCardModel;
    /**
     *  This scrim's opacity is controlled in two different ways. 1) Before the initial entrance
     *  animation finishes, the opacity is animated by a value animator. This is designed to
     *  distract the user from the length of the initial loading time. 2) After the initial
     *  entrance animation, the opacity is directly related to scroll position.
     */
    private ColorDrawable mWindowScrim;
    private boolean mIsEntranceAnimationFinished;
    private MaterialColorMapUtils mMaterialColorMapUtils;
    private boolean mIsExitAnimationInProgress;
    private boolean mHasComputedThemeColor;

    /**
     * Used to stop the ExpandingEntry cards from adjusting between an entry click and the intent
     * being launched.
     */
    private boolean mHasIntentLaunched;

    private Contact mContactData;
    private ContactLoader mContactLoader;
    private PorterDuffColorFilter mColorFilter;
    private int mColorFilterColor;

    private final ImageViewDrawableSetter mPhotoSetter = new ImageViewDrawableSetter();

    /**
     * {@link #LEADING_MIMETYPES} is used to sort MIME-types.
     *
     * <p>The MIME-types in {@link #LEADING_MIMETYPES} appear in the front of the dialog,
     * in the order specified here.</p>
     */
    private static final List<String> LEADING_MIMETYPES = Lists.newArrayList(
            Phone.CONTENT_ITEM_TYPE, SipAddress.CONTENT_ITEM_TYPE, Email.CONTENT_ITEM_TYPE,
            StructuredPostal.CONTENT_ITEM_TYPE);

    private static final List<String> SORTED_ABOUT_CARD_MIMETYPES = Lists.newArrayList(
            Nickname.CONTENT_ITEM_TYPE,
            // Phonetic name is inserted after nickname if it is available.
            // No mimetype for phonetic name exists.
            Website.CONTENT_ITEM_TYPE,
            Organization.CONTENT_ITEM_TYPE,
            Event.CONTENT_ITEM_TYPE,
            Relation.CONTENT_ITEM_TYPE,
            Im.CONTENT_ITEM_TYPE,
            GroupMembership.CONTENT_ITEM_TYPE,
            Identity.CONTENT_ITEM_TYPE,
            Note.CONTENT_ITEM_TYPE);

    private static final BidiFormatter sBidiFormatter = BidiFormatter.getInstance();

    /** Id for the background contact loader */
    private static final int LOADER_CONTACT_ID = 0;

    private static final String KEY_LOADER_EXTRA_PHONES =
            QuickContactActivity.class.getCanonicalName() + ".KEY_LOADER_EXTRA_PHONES";

    /** Id for the background Sms Loader */
    private static final int LOADER_SMS_ID = 1;
    private static final int MAX_SMS_RETRIEVE = 3;

    /** Id for the back Calendar Loader */
    private static final int LOADER_CALENDAR_ID = 2;
    private static final String KEY_LOADER_EXTRA_EMAILS =
            QuickContactActivity.class.getCanonicalName() + ".KEY_LOADER_EXTRA_EMAILS";
    private static final int MAX_PAST_CALENDAR_RETRIEVE = 3;
    private static final int MAX_FUTURE_CALENDAR_RETRIEVE = 3;
    private static final long PAST_MILLISECOND_TO_SEARCH_LOCAL_CALENDAR =
            1L * 24L * 60L * 60L * 1000L /* 1 day */;
    private static final long FUTURE_MILLISECOND_TO_SEARCH_LOCAL_CALENDAR =
            7L * 24L * 60L * 60L * 1000L /* 7 days */;

    /** Id for the background Call Log Loader */
    private static final int LOADER_CALL_LOG_ID = 3;
    private static final int MAX_CALL_LOG_RETRIEVE = 3;
    private static final int MIN_NUM_CONTACT_ENTRIES_SHOWN = 3;
    private static final int MIN_NUM_COLLAPSED_RECENT_ENTRIES_SHOWN = 3;
    private static final int CARD_ENTRY_ID_EDIT_CONTACT = -2;


    private static final int[] mRecentLoaderIds = new int[]{
        LOADER_SMS_ID,
        LOADER_CALENDAR_ID,
        LOADER_CALL_LOG_ID};
    /**
     * ConcurrentHashMap constructor params: 4 is initial table size, 0.9f is
     * load factor before resizing, 1 means we only expect a single thread to
     * write to the map so make only a single shard
     */
    private Map<Integer, List<ContactInteraction>> mRecentLoaderResults =
        new ConcurrentHashMap<>(4, 0.9f, 1);

    private static final String FRAGMENT_TAG_SELECT_ACCOUNT = "select_account_fragment";

    final OnClickListener mEntryClickHandler = new OnClickListener() {
        @Override
        public void onClick(View v) {
            final Object entryTagObject = v.getTag();
            if (entryTagObject == null || !(entryTagObject instanceof EntryTag)) {
                Log.w(TAG, "EntryTag was not used correctly");
                return;
            }
            final EntryTag entryTag = (EntryTag) entryTagObject;
            final Intent intent = entryTag.getIntent();
            final int dataId = entryTag.getId();
            Log.d(TAG, "[onClick]intent = " + intent + ",dataId = " + dataId);
            if (dataId == CARD_ENTRY_ID_EDIT_CONTACT) {
                editContact();
                return;
            }

            /// M: Delete the same code, it caused start activity two times. @{
            /*// Pass the touch point through the intent for use in the InCallUI
            if (Intent.ACTION_CALL.equals(intent.getAction())) {
                if (TouchPointManager.getInstance().hasValidPoint()) {
                    Bundle extras = new Bundle();
                    extras.putParcelable(TouchPointManager.TOUCH_POINT,
                            TouchPointManager.getInstance().getPoint());
                    intent.putExtra(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, extras);
                }
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            mHasIntentLaunched = true;
            try {
                startActivity(intent);
            } catch (SecurityException ex) {
                Toast.makeText(QuickContactActivity.this, R.string.missing_app,
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "QuickContacts does not have permission to launch "
                        + intent);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(QuickContactActivity.this, R.string.missing_app,
                        Toast.LENGTH_SHORT).show();
            }*/
            /// @}

            // Default to USAGE_TYPE_CALL. Usage is summed among all types for sorting each data id
            // so the exact usage type is not necessary in all cases
            String usageType = DataUsageFeedback.USAGE_TYPE_CALL;

            final Uri intentUri = intent.getData();
            if ((intentUri != null && intentUri.getScheme() != null &&
                    intentUri.getScheme().equals(ContactsUtils.SCHEME_SMSTO)) ||
                    (intent.getType() != null && intent.getType().equals(MIMETYPE_SMS))) {
                usageType = DataUsageFeedback.USAGE_TYPE_SHORT_TEXT;
            }

            // Data IDs start at 1 so anything less is invalid
            if (dataId > 0) {
                final Uri dataUsageUri = DataUsageFeedback.FEEDBACK_URI.buildUpon()
                        .appendPath(String.valueOf(dataId))
                        .appendQueryParameter(DataUsageFeedback.USAGE_TYPE, usageType)
                        .build();
                try {
                final boolean successful = getContentResolver().update(
                        dataUsageUri, new ContentValues(), null, null) > 0;
                if (!successful) {
                    Log.w(TAG, "DataUsageFeedback increment failed");
                }
                } catch (SecurityException ex) {
                    Log.w(TAG, "DataUsageFeedback increment failed", ex);
                }
            } else {
                Log.w(TAG, "Invalid Data ID");
            }

            // Pass the touch point through the intent for use in the InCallUI
            if (Intent.ACTION_CALL.equals(intent.getAction())) {
                if (TouchPointManager.getInstance().hasValidPoint()) {
                    Bundle extras = new Bundle();
                    extras.putParcelable(TouchPointManager.TOUCH_POINT,
                            TouchPointManager.getInstance().getPoint());
                    intent.putExtra(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, extras);
                }
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            mHasIntentLaunched = true;
            try {
                ImplicitIntentsUtil.startActivityInAppIfPossible(QuickContactActivity.this, intent);
            } catch (SecurityException ex) {
                Toast.makeText(QuickContactActivity.this, R.string.missing_app,
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "QuickContacts does not have permission to launch "
                        + intent);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(QuickContactActivity.this, R.string.missing_app,
                        Toast.LENGTH_SHORT).show();
            }
        }
    };

    final ExpandingEntryCardViewListener mExpandingEntryCardViewListener
            = new ExpandingEntryCardViewListener() {
        @Override
        public void onCollapse(int heightDelta) {
            mScroller.prepareForShrinkingScrollChild(heightDelta);
        }

        @Override
        public void onExpand() {
            mScroller.setDisableTouchesForSuppressLayout(/* areTouchesDisabled = */ true);
        }

        @Override
        public void onExpandDone() {
            mScroller.setDisableTouchesForSuppressLayout(/* areTouchesDisabled = */ false);
        }
    };

    private interface ContextMenuIds {
        static final int COPY_TEXT = 0;
        static final int CLEAR_DEFAULT = 1;
        static final int SET_DEFAULT = 2;
        /// M: add ip call
        static final int IP_CALL = 3;
    }

    private final OnCreateContextMenuListener mEntryContextMenuListener =
            new OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            if (menuInfo == null) {
                return;
            }
            final EntryContextMenuInfo info = (EntryContextMenuInfo) menuInfo;
            menu.setHeaderTitle(info.getCopyText());
            /// M: add ip call
            if (Phone.CONTENT_ITEM_TYPE.equals(info.getMimeType()) &&
                    PhoneCapabilityTester.isPhone(ContactsApplication.getInstance())) {
                menu.add(ContextMenu.NONE, ContextMenuIds.IP_CALL,
                    ContextMenu.NONE, getString(R.string.contact_detail_ip_call));
            }
            menu.add(ContextMenu.NONE, ContextMenuIds.COPY_TEXT,
                    ContextMenu.NONE, getString(R.string.copy_text));

            // Don't allow setting or clearing of defaults for non-editable contacts
            if (!isContactEditable()) {
                return;
            }

            final String selectedMimeType = info.getMimeType();

            // Defaults to true will only enable the detail to be copied to the clipboard.
            boolean onlyOneOfMimeType = true;

            // Only allow primary support for Phone and Email content types
            if (Phone.CONTENT_ITEM_TYPE.equals(selectedMimeType)) {
                onlyOneOfMimeType = mOnlyOnePhoneNumber;
            } else if (Email.CONTENT_ITEM_TYPE.equals(selectedMimeType)) {
                onlyOneOfMimeType = mOnlyOneEmail;
            }

            // Checking for previously set default
            if (info.isSuperPrimary()) {
                menu.add(ContextMenu.NONE, ContextMenuIds.CLEAR_DEFAULT,
                        ContextMenu.NONE, getString(R.string.clear_default));
            } else if (!onlyOneOfMimeType) {
                menu.add(ContextMenu.NONE, ContextMenuIds.SET_DEFAULT,
                        ContextMenu.NONE, getString(R.string.set_default));
            }
        }
    };

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        EntryContextMenuInfo menuInfo;
        try {
            menuInfo = (EntryContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        switch (item.getItemId()) {
            /// M: add ip call
            case ContextMenuIds.IP_CALL:
                QuickContactUtils.dialIpCall(this, menuInfo.getCopyText());
                return true;
            case ContextMenuIds.COPY_TEXT:
                ClipboardUtils.copyText(this, menuInfo.getCopyLabel(), menuInfo.getCopyText(),
                        true);
                return true;
            case ContextMenuIds.SET_DEFAULT:
                final Intent setIntent = ContactSaveService.createSetSuperPrimaryIntent(this,
                        menuInfo.getId());
                this.startService(setIntent);
                return true;
            case ContextMenuIds.CLEAR_DEFAULT:
                final Intent clearIntent = ContactSaveService.createClearPrimaryIntent(this,
                        menuInfo.getId());
                this.startService(clearIntent);
                return true;
            default:
                throw new IllegalArgumentException("Unknown menu option " + item.getItemId());
        }
    }

    /**
     * Headless fragment used to handle account selection callbacks invoked from
     * {@link DirectoryContactUtil}.
     */
    public static class SelectAccountDialogFragmentListener extends Fragment
            implements SelectAccountDialogFragment.Listener {

        private QuickContactActivity mQuickContactActivity;

        public SelectAccountDialogFragmentListener() {}

        @Override
        public void onAccountChosen(AccountWithDataSet account, Bundle extraArgs) {
            DirectoryContactUtil.createCopy(mQuickContactActivity.mContactData.getContentValues(),
                    account, mQuickContactActivity);
        }

        @Override
        public void onAccountSelectorCancelled() {}

        /**
         * Set the parent activity. Since rotation can cause this fragment to be used across
         * more than one activity instance, we need to explicitly set this value instead
         * of making this class non-static.
         */
        public void setQuickContactActivity(QuickContactActivity quickContactActivity) {
            mQuickContactActivity = quickContactActivity;
        }
    }

    final MultiShrinkScrollerListener mMultiShrinkScrollerListener
            = new MultiShrinkScrollerListener() {
        @Override
        public void onScrolledOffBottom() {
            finish();
        }

        @Override
        public void onEnterFullscreen() {
            updateStatusBarColor();
        }

        @Override
        public void onExitFullscreen() {
            updateStatusBarColor();
        }

        @Override
        public void onStartScrollOffBottom() {
            mIsExitAnimationInProgress = true;
        }

        @Override
        public void onEntranceAnimationDone() {
            mIsEntranceAnimationFinished = true;
        }

        @Override
        public void onTransparentViewHeightChange(float ratio) {
            if (mIsEntranceAnimationFinished) {
                mWindowScrim.setAlpha((int) (0xFF * ratio));
            }
        }
    };


    /**
     * Data items are compared to the same mimetype based off of three qualities:
     * 1. Super primary
     * 2. Primary
     * 3. Times used
     */
    private final Comparator<DataItem> mWithinMimeTypeDataItemComparator =
            new Comparator<DataItem>() {
        @Override
        public int compare(DataItem lhs, DataItem rhs) {
            if (!lhs.getMimeType().equals(rhs.getMimeType())) {
                Log.wtf(TAG, "Comparing DataItems with different mimetypes lhs.getMimeType(): " +
                        lhs.getMimeType() + " rhs.getMimeType(): " + rhs.getMimeType());
                return 0;
            }

            if (lhs.isSuperPrimary()) {
                return -1;
            } else if (rhs.isSuperPrimary()) {
                return 1;
            } else if (lhs.isPrimary() && !rhs.isPrimary()) {
                return -1;
            } else if (!lhs.isPrimary() && rhs.isPrimary()) {
                return 1;
            } else {
                final int lhsTimesUsed =
                        lhs.getTimesUsed() == null ? 0 : lhs.getTimesUsed();
                final int rhsTimesUsed =
                        rhs.getTimesUsed() == null ? 0 : rhs.getTimesUsed();

                return rhsTimesUsed - lhsTimesUsed;
            }
        }
    };

    /**
     * Sorts among different mimetypes based off:
     * 1. Whether one of the mimetypes is the prioritized mimetype
     * 2. Number of times used
     * 3. Last time used
     * 4. Statically defined
     */
    private final Comparator<List<DataItem>> mAmongstMimeTypeDataItemComparator =
            new Comparator<List<DataItem>> () {
        @Override
        public int compare(List<DataItem> lhsList, List<DataItem> rhsList) {
            final DataItem lhs = lhsList.get(0);
            final DataItem rhs = rhsList.get(0);
            final String lhsMimeType = lhs.getMimeType();
            final String rhsMimeType = rhs.getMimeType();

            // 1. Whether one of the mimetypes is the prioritized mimetype
            if (!TextUtils.isEmpty(mExtraPrioritizedMimeType) && !lhsMimeType.equals(rhsMimeType)) {
                if (rhsMimeType.equals(mExtraPrioritizedMimeType)) {
                    return 1;
                }
                if (lhsMimeType.equals(mExtraPrioritizedMimeType)) {
                    return -1;
                }
            }

            // 2. Number of times used
            final int lhsTimesUsed = lhs.getTimesUsed() == null ? 0 : lhs.getTimesUsed();
            final int rhsTimesUsed = rhs.getTimesUsed() == null ? 0 : rhs.getTimesUsed();
            final int timesUsedDifference = rhsTimesUsed - lhsTimesUsed;
            if (timesUsedDifference != 0) {
                return timesUsedDifference;
            }

            // 3. Last time used
            final long lhsLastTimeUsed =
                    lhs.getLastTimeUsed() == null ? 0 : lhs.getLastTimeUsed();
            final long rhsLastTimeUsed =
                    rhs.getLastTimeUsed() == null ? 0 : rhs.getLastTimeUsed();
            final long lastTimeUsedDifference = rhsLastTimeUsed - lhsLastTimeUsed;
            if (lastTimeUsedDifference > 0) {
                return 1;
            } else if (lastTimeUsedDifference < 0) {
                return -1;
            }

            // 4. Resort to a statically defined mimetype order.
            if (!lhsMimeType.equals(rhsMimeType)) {
            for (String mimeType : LEADING_MIMETYPES) {
                if (lhsMimeType.equals(mimeType)) {
                    return -1;
                } else if (rhsMimeType.equals(mimeType)) {
                    return 1;
                }
            }
            }
            return 0;
        }
    };

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            TouchPointManager.getInstance().setPoint((int) ev.getRawX(), (int) ev.getRawY());
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("onCreate()");
        super.onCreate(savedInstanceState);

        if (RequestPermissionsActivity.startPermissionActivity(this)) {
            return;
        }

        getWindow().setStatusBarColor(Color.TRANSPARENT);

        processIntent(getIntent());

        // Show QuickContact in front of soft input
         getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        setContentView(R.layout.quickcontact_activity);

        mMaterialColorMapUtils = new MaterialColorMapUtils(getResources());

        mScroller = (MultiShrinkScroller) findViewById(R.id.multiscroller);

        mContactCard = (ExpandingEntryCardView) findViewById(R.id.communication_card);
        /// M: [for rcs-e]
        mJoynCard = (ExpandingEntryCardView) ExtensionManager.getInstance()
            .getViewCustomExtension().getQuickContactCardViewCustom().createCardView(
                    (LinearLayout)findViewById(R.id.card_container),
                    (View) mContactCard, mLookupUri, this);
        mNoContactDetailsCard = (ExpandingEntryCardView) findViewById(R.id.no_contact_data_card);
        mRecentCard = (ExpandingEntryCardView) findViewById(R.id.recent_card);
        mAboutCard = (ExpandingEntryCardView) findViewById(R.id.about_card);

        mNoContactDetailsCard.setOnClickListener(mEntryClickHandler);
        mContactCard.setOnClickListener(mEntryClickHandler);
        mContactCard.setExpandButtonText(
        getResources().getString(R.string.expanding_entry_card_view_see_all));
        mContactCard.setOnCreateContextMenuListener(mEntryContextMenuListener);

        mRecentCard.setOnClickListener(mEntryClickHandler);
        mRecentCard.setTitle(getResources().getString(R.string.recent_card_title));

        mAboutCard.setOnClickListener(mEntryClickHandler);
        mAboutCard.setOnCreateContextMenuListener(mEntryContextMenuListener);

        mPhotoView = (QuickContactImageView) findViewById(R.id.photo);

        //M:OP01 RCS will go to contact detail activity, update photo from rcs server.@{
        ExtensionManager.getInstance().getRcsExtension()
                .updateContactPhotoFromRcsServer(mLookupUri, mPhotoView, this);
        /** @} */

        final View transparentView = findViewById(R.id.transparent_view);
        if (mScroller != null) {
            transparentView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mScroller.scrollOffBottom();
                }
            });
        }

        // Allow a shadow to be shown under the toolbar.
        ViewUtil.addRectangularOutlineProvider(findViewById(R.id.toolbar_parent), getResources());

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);
        getActionBar().setTitle(null);
        // Put a TextView with a known resource id into the ActionBar. This allows us to easily
        // find the correct TextView location & size later.
        toolbar.addView(getLayoutInflater().inflate(R.layout.quickcontact_title_placeholder, null));

        mHasAlreadyBeenOpened = savedInstanceState != null;
        mIsEntranceAnimationFinished = mHasAlreadyBeenOpened;
        mWindowScrim = new ColorDrawable(SCRIM_COLOR);
        mWindowScrim.setAlpha(0);
        getWindow().setBackgroundDrawable(mWindowScrim);

        mScroller.initialize(mMultiShrinkScrollerListener, mExtraMode == MODE_FULLY_EXPANDED);
        // mScroller needs to perform asynchronous measurements after initalize(), therefore
        // we can't mark this as GONE.
        mScroller.setVisibility(View.INVISIBLE);

        setHeaderNameText(R.string.missing_name);

        mSelectAccountFragmentListener= (SelectAccountDialogFragmentListener) getFragmentManager()
                .findFragmentByTag(FRAGMENT_TAG_SELECT_ACCOUNT);
        if (mSelectAccountFragmentListener == null) {
            mSelectAccountFragmentListener = new SelectAccountDialogFragmentListener();
            getFragmentManager().beginTransaction().add(0, mSelectAccountFragmentListener,
                    FRAGMENT_TAG_SELECT_ACCOUNT).commit();
            mSelectAccountFragmentListener.setRetainInstance(true);
        }
        mSelectAccountFragmentListener.setQuickContactActivity(this);

        SchedulingUtils.doOnPreDraw(mScroller, /* drawNextFrame = */ true,
                new Runnable() {
                    @Override
                    public void run() {
                        if (!mHasAlreadyBeenOpened) {
                            // The initial scrim opacity must match the scrim opacity that would be
                            // achieved by scrolling to the starting position.
                            final float alphaRatio = mExtraMode == MODE_FULLY_EXPANDED ?
                                    1 : mScroller.getStartingTransparentHeightRatio();
                            final int duration = getResources().getInteger(
                                    android.R.integer.config_shortAnimTime);
                            final int desiredAlpha = (int) (0xFF * alphaRatio);
                            ObjectAnimator o = ObjectAnimator.ofInt(mWindowScrim, "alpha", 0,
                                    desiredAlpha).setDuration(duration);

                            o.start();
                        }
                    }
                });

        if (savedInstanceState != null) {
            final int color = savedInstanceState.getInt(KEY_THEME_COLOR, 0);
            SchedulingUtils.doOnPreDraw(mScroller, /* drawNextFrame = */ false,
                    new Runnable() {
                        @Override
                        public void run() {
                            // Need to wait for the pre draw before setting the initial scroll
                            // value. Prior to pre draw all scroll values are invalid.
                            if (mHasAlreadyBeenOpened) {
                                mScroller.setVisibility(View.VISIBLE);
                                mScroller.setScroll(mScroller.getScrollNeededToBeFullScreen());
                            }
                            // Need to wait for pre draw for setting the theme color. Setting the
                            // header tint before the MultiShrinkScroller has been measured will
                            // cause incorrect tinting calculations.
                            if (color != 0) {
                                setThemeColor(mMaterialColorMapUtils
                                        .calculatePrimaryAndSecondaryColor(color));
                            }
                        }
                    });
        }

        Trace.endSection();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CONTACT_EDITOR_ACTIVITY &&
                resultCode == ContactDeletionInteraction.RESULT_CODE_DELETED) {
            // The contact that we were showing has been deleted.
            finish();
        } else if (requestCode == REQUEST_CODE_CONTACT_SELECTION_ACTIVITY &&
                resultCode != RESULT_CANCELED) {
            processIntent(data);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mHasAlreadyBeenOpened = true;
        mIsEntranceAnimationFinished = true;
        mHasComputedThemeColor = false;
        processIntent(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (mColorFilter != null) {
            savedInstanceState.putInt(KEY_THEME_COLOR, mColorFilterColor);
        }
    }

    private void processIntent(Intent intent) {
        if (intent == null) {
            Log.w(TAG, "[processIntent]intent is null,return!");
            finish();
            return;
        }
        Uri lookupUri = intent.getData();
        Log.d(TAG, "The original uri from intent: " + lookupUri);

        // Check to see whether it comes from the old version.
        if (lookupUri != null && LEGACY_AUTHORITY.equals(lookupUri.getAuthority())) {
            final long rawContactId = ContentUris.parseId(lookupUri);
            lookupUri = RawContacts.getContactLookupUri(getContentResolver(),
                    ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId));
            Log.d(TAG, "The uri from old version: " + lookupUri);
        }
        mExtraMode = getIntent().getIntExtra(QuickContact.EXTRA_MODE, QuickContact.MODE_LARGE);
        mExtraPrioritizedMimeType = getIntent().getStringExtra(
                QuickContact.EXTRA_PRIORITIZED_MIMETYPE);
        final Uri oldLookupUri = mLookupUri;

        if (lookupUri == null) {
            Log.w(TAG, "[processIntent]lookupUri is null,return!");
            finish();
            return;
        }
        mLookupUri = lookupUri;
        mExcludeMimes = intent.getStringArrayExtra(QuickContact.EXTRA_EXCLUDE_MIMES);
        if (oldLookupUri == null) {
            mContactLoader = (ContactLoader) getLoaderManager().initLoader(
                    LOADER_CONTACT_ID, null, mLoaderContactCallbacks);
        } else if (oldLookupUri != mLookupUri) {
            // After copying a directory contact, the contact URI changes. Therefore,
            // we need to restart the loader and reload the new contact.
            destroyInteractionLoaders();
            mContactLoader = (ContactLoader) getLoaderManager().restartLoader(
                    LOADER_CONTACT_ID, null, mLoaderContactCallbacks);
            mCachedCp2DataCardModel = null;
        }

        NfcHandler.register(this, mLookupUri);
    }

    private void destroyInteractionLoaders() {
        for (int interactionLoaderId : mRecentLoaderIds) {
            getLoaderManager().destroyLoader(interactionLoaderId);
        }
    }

    private void runEntranceAnimation() {
        if (mHasAlreadyBeenOpened) {
            return;
        }
        mHasAlreadyBeenOpened = true;
        mScroller.scrollUpForEntranceAnimation(mExtraMode != MODE_FULLY_EXPANDED);
    }

    /** Assign this string to the view if it is not empty. */
    private void setHeaderNameText(int resId) {
        if (mScroller != null) {
            mScroller.setTitle(getText(resId) == null ? null : getText(resId).toString());
        }
    }

    /** Assign this string to the view if it is not empty. */
    private void setHeaderNameText(String value) {
        if (!TextUtils.isEmpty(value)) {
            if (mScroller != null) {
                mScroller.setTitle(value);
            }
        }
    }

    /**
     * Check if the given MIME-type appears in the list of excluded MIME-types
     * that the most-recent caller requested.
     */
    private boolean isMimeExcluded(String mimeType) {
        if (mExcludeMimes == null) return false;
        for (String excludedMime : mExcludeMimes) {
            if (TextUtils.equals(excludedMime, mimeType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handle the result from the ContactLoader
     */
    private void bindContactData(final Contact data) {
        Trace.beginSection("bindContactData");
        mContactData = data;
        invalidateOptionsMenu();

        Trace.endSection();
        Trace.beginSection("Set display photo & name");

        mPhotoView.setIsBusiness(mContactData.isDisplayNameFromOrganization());
        mPhotoSetter.setupContactPhoto(data, mPhotoView);
        extractAndApplyTintFromPhotoViewAsynchronously();
        String phoneticName = ContactDisplayUtils.getPhoneticName(this, data);
        String displayName = ContactDisplayUtils.getDisplayName(this, data).toString();
        if (mContactData.getDisplayNameSource() != DisplayNameSources.STRUCTURED_PHONETIC_NAME
                && !TextUtils.isEmpty(phoneticName)) {
            displayName = getResources().getString(
                    R.string.quick_contact_display_name_with_phonetic, displayName, phoneticName);
            setHeaderNameText(displayName);
        } else {
            setHeaderNameText(displayName);
        }

        Trace.endSection();

        mEntriesAndActionsTask = new AsyncTask<Void, Void, Cp2DataCardModel>() {

            @Override
            protected Cp2DataCardModel doInBackground(
                    Void... params) {
                return generateDataModelFromContact(data);
            }

            @Override
            protected void onPostExecute(Cp2DataCardModel cardDataModel) {
                super.onPostExecute(cardDataModel);
                // Check that original AsyncTask parameters are still valid and the activity
                // is still running before binding to UI. A new intent could invalidate
                // the results, for example.
                if (data == mContactData && !isCancelled()) {
                    bindDataToCards(cardDataModel);
                    showActivity();
                }
            }
        };
        mEntriesAndActionsTask.execute();
    }

    private void bindDataToCards(Cp2DataCardModel cp2DataCardModel) {
        startInteractionLoaders(cp2DataCardModel);
        populateContactAndAboutCard(cp2DataCardModel);
    }

    private void startInteractionLoaders(Cp2DataCardModel cp2DataCardModel) {
        final Map<String, List<DataItem>> dataItemsMap = cp2DataCardModel.dataItemsMap;
        final List<DataItem> phoneDataItems = dataItemsMap.get(Phone.CONTENT_ITEM_TYPE);
        if (phoneDataItems != null) {
            /// M: Reset the value as the size may change,
            //  otherwise when size > 1, the value will always be true.
            mOnlyOnePhoneNumber = phoneDataItems.size() == 1 ? true : false;
        }
        String[] phoneNumbers = null;
        if (phoneDataItems != null) {
            phoneNumbers = new String[phoneDataItems.size()];
            for (int i = 0; i < phoneDataItems.size(); ++i) {
                phoneNumbers[i] = ((PhoneDataItem) phoneDataItems.get(i)).getNumber();
            }
        }
        final Bundle phonesExtraBundle = new Bundle();
        phonesExtraBundle.putStringArray(KEY_LOADER_EXTRA_PHONES, phoneNumbers);

        Trace.beginSection("start sms loader");
        getLoaderManager().initLoader(
                LOADER_SMS_ID,
                phonesExtraBundle,
                mLoaderInteractionsCallbacks);
        Trace.endSection();

        Trace.beginSection("start call log loader");
        getLoaderManager().initLoader(
                LOADER_CALL_LOG_ID,
                phonesExtraBundle,
                mLoaderInteractionsCallbacks);
        Trace.endSection();


        Trace.beginSection("start calendar loader");
        final List<DataItem> emailDataItems = dataItemsMap.get(Email.CONTENT_ITEM_TYPE);
        if (emailDataItems != null) {
            /// M: Reset the value as the size may change,
            //  otherwise when size > 1, the value will always be true.
            mOnlyOneEmail = emailDataItems.size() == 1 ? true : false;
        }
        String[] emailAddresses = null;
        if (emailDataItems != null) {
            emailAddresses = new String[emailDataItems.size()];
            for (int i = 0; i < emailDataItems.size(); ++i) {
                emailAddresses[i] = ((EmailDataItem) emailDataItems.get(i)).getAddress();
            }
        }
        final Bundle emailsExtraBundle = new Bundle();
        emailsExtraBundle.putStringArray(KEY_LOADER_EXTRA_EMAILS, emailAddresses);
        getLoaderManager().initLoader(
                LOADER_CALENDAR_ID,
                emailsExtraBundle,
                mLoaderInteractionsCallbacks);
        Trace.endSection();
    }

    private void showActivity() {
        if (mScroller != null) {
            mScroller.setVisibility(View.VISIBLE);
            SchedulingUtils.doOnPreDraw(mScroller, /* drawNextFrame = */ false,
                    new Runnable() {
                        @Override
                        public void run() {
                            runEntranceAnimation();
                        }
                    });
        }
    }

    private List<List<Entry>> buildAboutCardEntries(Map<String, List<DataItem>> dataItemsMap) {
        final List<List<Entry>> aboutCardEntries = new ArrayList<>();
        for (String mimetype : SORTED_ABOUT_CARD_MIMETYPES) {
            final List<DataItem> mimeTypeItems = dataItemsMap.get(mimetype);
            if (mimeTypeItems == null) {
                continue;
            }
            // Set aboutCardTitleOut = null, since SORTED_ABOUT_CARD_MIMETYPES doesn't contain
            // the name mimetype.
            final List<Entry> aboutEntries = dataItemsToEntries(mimeTypeItems,
                    /* aboutCardTitleOut = */ null);
            if (aboutEntries.size() > 0) {
                aboutCardEntries.add(aboutEntries);
            }
        }
        return aboutCardEntries;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // If returning from a launched activity, repopulate the contact and about card
        if (mHasIntentLaunched) {
            mHasIntentLaunched = false;
            populateContactAndAboutCard(mCachedCp2DataCardModel);
        }

        // When exiting the activity and resuming, we want to force a full reload of all the
        // interaction data in case something changed in the background. On screen rotation,
        // we don't need to do this. And, mCachedCp2DataCardModel will be null, so we won't.
        if (mCachedCp2DataCardModel != null) {
            destroyInteractionLoaders();
            startInteractionLoaders(mCachedCp2DataCardModel);
        }
        ///M:[for rcs] update Rcs contact on the top left@{
        ExtensionManager.getInstance().getRcsExtension()
                .getQuickContactRcsScroller()
                .updateRcsContact(mContactLoader.getLookupUri(), false);
        ///@}
    }

    private void populateContactAndAboutCard(Cp2DataCardModel cp2DataCardModel) {
        mCachedCp2DataCardModel = cp2DataCardModel;
        if (mHasIntentLaunched || cp2DataCardModel == null) {
            return;
        }
        Trace.beginSection("bind contact card");

        final List<List<Entry>> contactCardEntries = cp2DataCardModel.contactCardEntries;
        final List<List<Entry>> aboutCardEntries = cp2DataCardModel.aboutCardEntries;
        final String customAboutCardName = cp2DataCardModel.customAboutCardName;

        if (contactCardEntries.size() > 0) {
            final boolean firstEntriesArePrioritizedMimeType =
                    !TextUtils.isEmpty(mExtraPrioritizedMimeType) &&
                    mCachedCp2DataCardModel.dataItemsMap.containsKey(mExtraPrioritizedMimeType) &&
                    mCachedCp2DataCardModel.dataItemsMap.get(mExtraPrioritizedMimeType).size() != 0;
            mContactCard.initialize(contactCardEntries,
                    /* numInitialVisibleEntries = */ MIN_NUM_CONTACT_ENTRIES_SHOWN,
                    /* isExpanded = */ mContactCard.isExpanded(),
                    /* isAlwaysExpanded = */ false,
                    mExpandingEntryCardViewListener,
                    mScroller,
                    firstEntriesArePrioritizedMimeType);
            mContactCard.setVisibility(View.VISIBLE);
        } else {
            mContactCard.setVisibility(View.GONE);
        }
        Trace.endSection();

        Trace.beginSection("bind about card");

        // Phonetic name is not a data item, so the entry needs to be created separately
        // final String phoneticName = mContactData.getPhoneticName();
        // if (!TextUtils.isEmpty(phoneticName)) {
            // Entry phoneticEntry = new Entry(/* viewId = */ -1,
            // /* icon = */ null,
            // getResources().getString(R.string.name_phonetic),
            // phoneticName,
            // /* subHeaderIcon = */ null,
            // /* text = */ null,
            // /* textIcon = */ null,
            // /* primaryContentDescription = */ null,
            // /* intent = */ null,
            // /* alternateIcon = */ null,
            // /* alternateIntent = */ null,
            // /* alternateContentDescription = */ null,
            // /* shouldApplyColor = */ false,
            // /* isEditable = */ false,
            // /* EntryContextMenuInfo = */ // new EntryContextMenuInfo(phoneticName,
            // getResources().getString(R.string.name_phonetic),
            // /* mimeType = */ null, /* id = */ -1, /* isPrimary = */ false),
            // /* thirdIcon = */ null,
            // /* thirdIntent = */ null,
            // /* thirdContentDescription = */ null,
            // /* iconResourceId = */ 0);
            // List<Entry> phoneticList = new ArrayList<>();
            // phoneticList.add(phoneticEntry);
            // Phonetic name comes after nickname. Check to see if the first entry type is nickname
            // if (aboutCardEntries.size() > 0 && aboutCardEntries.get(0).get(0).getHeader().equals(
                // getResources().getString(R.string.header_nickname_entry))) {
                // aboutCardEntries.add(1, phoneticList);
                // } else {
                // aboutCardEntries.add(0, phoneticList);
            // }
        // }

        /// M: Bug fix ALPS01775443, after deleted name in editor, need refresh about card.
        mAboutCard.setTitle(customAboutCardName);

        if (aboutCardEntries.size() > 0) {
            mAboutCard.initialize(aboutCardEntries,
                    /* numInitialVisibleEntries = */ 1,
                    /* isExpanded = */ true,
                    /* isAlwaysExpanded = */ true,
                    mExpandingEntryCardViewListener,
                    mScroller);
        } else {
            /// M: Bug fix ALPS01763309, after deleted all about card informations
            //  in editor, need refresh about card. @{
            mAboutCard.initialize(aboutCardEntries, 1, true, true,
                    mExpandingEntryCardViewListener, mScroller);
            mAboutCard.setVisibility(View.GONE);
            /// @}
        }

        if (contactCardEntries.size() == 0 && aboutCardEntries.size() == 0) {
            initializeNoContactDetailCard();
        } else {
            mNoContactDetailsCard.setVisibility(View.GONE);
        }

        // If the Recent card is already initialized (all recent data is loaded), show the About
        // card if it has entries. Otherwise About card visibility will be set in bindRecentData()
        if (isAllRecentDataLoaded() && aboutCardEntries.size() > 0) {
            mAboutCard.setVisibility(View.VISIBLE);
        }
        Trace.endSection();
    }

    /**
     * Create a card that shows "Add email" and "Add phone number" entries in grey.
     */
    private void initializeNoContactDetailCard() {
        final Drawable phoneIcon = getResources().getDrawable(
                R.drawable.ic_phone_24dp).mutate();
        final Entry phonePromptEntry = new Entry(CARD_ENTRY_ID_EDIT_CONTACT,
                phoneIcon, getString(R.string.quickcontact_add_phone_number),
                /* subHeader = */ null, /* subHeaderIcon = */ null, /* text = */ null,
                /* textIcon = */ null, /* primaryContentDescription = */ null,
                getEditContactIntent(),
                /* alternateIcon = */ null, /* alternateIntent = */ null,
                /* alternateContentDescription = */ null, /* shouldApplyColor = */ true,
                /* isEditable = */ false, /* EntryContextMenuInfo = */ null,
                /* thirdIcon = */ null, /* thirdIntent = */ null,
                /* thirdContentDescription = */ null, R.drawable.ic_phone_24dp);

        final Drawable emailIcon = getResources().getDrawable(
                R.drawable.ic_email_24dp).mutate();
        final Entry emailPromptEntry = new Entry(CARD_ENTRY_ID_EDIT_CONTACT,
                emailIcon, getString(R.string.quickcontact_add_email), /* subHeader = */ null,
                /* subHeaderIcon = */ null,
                /* text = */ null, /* textIcon = */ null, /* primaryContentDescription = */ null,
                getEditContactIntent(), /* alternateIcon = */ null,
                /* alternateIntent = */ null, /* alternateContentDescription = */ null,
                /* shouldApplyColor = */ true, /* isEditable = */ false,
                /* EntryContextMenuInfo = */ null, /* thirdIcon = */ null,
                /* thirdIntent = */ null, /* thirdContentDescription = */ null,
                R.drawable.ic_email_24dp);

        final List<List<Entry>> promptEntries = new ArrayList<>();
        promptEntries.add(new ArrayList<Entry>(1));
        promptEntries.add(new ArrayList<Entry>(1));
        promptEntries.get(0).add(phonePromptEntry);
        promptEntries.get(1).add(emailPromptEntry);

        final int subHeaderTextColor = getResources().getColor(
                R.color.quickcontact_entry_sub_header_text_color);
        final PorterDuffColorFilter greyColorFilter =
                new PorterDuffColorFilter(subHeaderTextColor, PorterDuff.Mode.SRC_ATOP);
        mNoContactDetailsCard.initialize(promptEntries, 2, /* isExpanded = */ true,
                /* isAlwaysExpanded = */ true, mExpandingEntryCardViewListener, mScroller);
        mNoContactDetailsCard.setVisibility(View.VISIBLE);
        mNoContactDetailsCard.setEntryHeaderColor(subHeaderTextColor);
        mNoContactDetailsCard.setColorAndFilter(subHeaderTextColor, greyColorFilter);
    }

    /**
     * Builds the {@link DataItem}s Map out of the Contact.
     * @param data The contact to build the data from.
     * @return A pair containing a list of data items sorted within mimetype and sorted
     *  amongst mimetype. The map goes from mimetype string to the sorted list of data items within
     *  mimetype
     */
    private Cp2DataCardModel generateDataModelFromContact(
            Contact data) {
        Trace.beginSection("Build data items map");

        final Map<String, List<DataItem>> dataItemsMap = new HashMap<>();

        final ResolveCache cache = ResolveCache.getInstance(this);
        for (RawContact rawContact : data.getRawContacts()) {
            for (DataItem dataItem : rawContact.getDataItems()) {
                dataItem.setRawContactId(rawContact.getId());

                final String mimeType = dataItem.getMimeType();
                if (mimeType == null) continue;

                final AccountType accountType = rawContact.getAccountType(this);
                final DataKind dataKind = AccountTypeManager.getInstance(this)
                        .getKindOrFallback(accountType, mimeType);
                if (dataKind == null) continue;

                dataItem.setDataKind(dataKind);

                final boolean hasData = !TextUtils.isEmpty(dataItem.buildDataString(this,
                        dataKind));

                if (isMimeExcluded(mimeType) || !hasData) continue;

                List<DataItem> dataItemListByType = dataItemsMap.get(mimeType);
                if (dataItemListByType == null) {
                    dataItemListByType = new ArrayList<>();
                    dataItemsMap.put(mimeType, dataItemListByType);
                }
                dataItemListByType.add(dataItem);
            }
        }
        Trace.endSection();

        Trace.beginSection("sort within mimetypes");
        /*
         * Sorting is a multi part step. The end result is to a have a sorted list of the most
         * used data items, one per mimetype. Then, within each mimetype, the list of data items
         * for that type is also sorted, based off of {super primary, primary, times used} in that
         * order.
         */
        final List<List<DataItem>> dataItemsList = new ArrayList<>();
        for (List<DataItem> mimeTypeDataItems : dataItemsMap.values()) {
            // Remove duplicate data items
            Collapser.collapseList(mimeTypeDataItems, this);
            // Sort within mimetype
            Collections.sort(mimeTypeDataItems, mWithinMimeTypeDataItemComparator);
            // Add to the list of data item lists
            dataItemsList.add(mimeTypeDataItems);
        }
        Trace.endSection();

        Trace.beginSection("sort amongst mimetypes");
        // Sort amongst mimetypes to bubble up the top data items for the contact card
        Collections.sort(dataItemsList, mAmongstMimeTypeDataItemComparator);
        Trace.endSection();

        Trace.beginSection("cp2 data items to entries");

        final List<List<Entry>> contactCardEntries = new ArrayList<>();
        final List<List<Entry>> aboutCardEntries = buildAboutCardEntries(dataItemsMap);
        final MutableString aboutCardName = new MutableString();

        /// M: Bug Fix for ALPS01747019.
        QuickContactUtils.buildPhoneticNameToAboutEntry(this, data, aboutCardEntries);
        for (int i = 0; i < dataItemsList.size(); ++i) {
            final List<DataItem> dataItemsByMimeType = dataItemsList.get(i);
            final DataItem topDataItem = dataItemsByMimeType.get(0);
            if (SORTED_ABOUT_CARD_MIMETYPES.contains(topDataItem.getMimeType())) {
                // About card mimetypes are built in buildAboutCardEntries, skip here
                continue;
            } else {
                List<Entry> contactEntries = dataItemsToEntries(dataItemsList.get(i),
                        aboutCardName);
                if (contactEntries.size() > 0) {
                    contactCardEntries.add(contactEntries);
                }
            }
        }

        Trace.endSection();

        final Cp2DataCardModel dataModel = new Cp2DataCardModel();
        dataModel.customAboutCardName = aboutCardName.value;
        dataModel.aboutCardEntries = aboutCardEntries;
        dataModel.contactCardEntries = contactCardEntries;
        dataModel.dataItemsMap = dataItemsMap;
        return dataModel;
    }

    /**
     * Class used to hold the About card and Contact cards' data model that gets generated
     * on a background thread. All data is from CP2.
     */
    private static class Cp2DataCardModel {
        /**
         * A map between a mimetype string and the corresponding list of data items. The data items
         * are in sorted order using mWithinMimeTypeDataItemComparator.
         */
        public Map<String, List<DataItem>> dataItemsMap;
        public List<List<Entry>> aboutCardEntries;
        public List<List<Entry>> contactCardEntries;
        public String customAboutCardName;
    }

    private static class MutableString {
        public String value;
    }

    /**
     * Converts a {@link DataItem} into an {@link ExpandingEntryCardView.Entry} for display.
     * If the {@link ExpandingEntryCardView.Entry} has no visual elements, null is returned.
     *
     * This runs on a background thread. This is set as static to avoid accidentally adding
     * additional dependencies on unsafe things (like the Activity).
     *
     * @param dataItem The {@link DataItem} to convert.
     * @param secondDataItem A second {@link DataItem} to help build a full entry for some
     *  mimetypes
     * @return The {@link ExpandingEntryCardView.Entry}, or null if no visual elements are present.
     */
    private static Entry dataItemToEntry(DataItem dataItem, DataItem secondDataItem,
            Context context, Contact contactData,
            final MutableString aboutCardName) {
        Drawable icon = null;
        String header = null;
        String subHeader = null;
        Drawable subHeaderIcon = null;
        String text = null;
        Drawable textIcon = null;
        StringBuilder primaryContentDescription = new StringBuilder();
        Intent intent = null;
        boolean shouldApplyColor = true;
        Drawable alternateIcon = null;
        Intent alternateIntent = null;
        StringBuilder alternateContentDescription = new StringBuilder();
        final boolean isEditable = false;
        EntryContextMenuInfo entryContextMenuInfo = null;
        Drawable thirdIcon = null;
        Intent thirdIntent = null;
        String thirdContentDescription = null;
        int iconResourceId = 0;

        context = context.getApplicationContext();
        final Resources res = context.getResources();
        DataKind kind = dataItem.getDataKind();

        QuickContactUtils.resetSipAddress();
    ///M: Fix ALPS01995031
        if (contactData == null) {
            Log.w(TAG, "contact data is null.");
            return null;
        }
        if (dataItem instanceof ImDataItem) {
            final ImDataItem im = (ImDataItem) dataItem;
            intent = ContactsUtils.buildImIntent(context, im).first;
            final boolean isEmail = im.isCreatedFromEmail();
            final int protocol;
            if (!im.isProtocolValid()) {
                protocol = Im.PROTOCOL_CUSTOM;
            } else {
                protocol = isEmail ? Im.PROTOCOL_GOOGLE_TALK : im.getProtocol();
            }
            if (protocol == Im.PROTOCOL_CUSTOM) {
                // If the protocol is custom, display the "IM" entry header as well to distinguish
                // this entry from other ones
                header = res.getString(R.string.header_im_entry);
                subHeader = Im.getProtocolLabel(res, protocol,
                        im.getCustomProtocol()).toString();
                text = im.getData();
            } else {
                header = Im.getProtocolLabel(res, protocol,
                        im.getCustomProtocol()).toString();
                subHeader = im.getData();
            }
            entryContextMenuInfo = new EntryContextMenuInfo(im.getData(), header,
                    dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
        } else if (dataItem instanceof OrganizationDataItem) {
            final OrganizationDataItem organization = (OrganizationDataItem) dataItem;
            header = res.getString(R.string.header_organization_entry);
            subHeader = organization.getCompany();
            entryContextMenuInfo = new EntryContextMenuInfo(subHeader, header,
                    dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
            text = organization.getTitle();
        } else if (dataItem instanceof NicknameDataItem) {
            final NicknameDataItem nickname = (NicknameDataItem) dataItem;
            // Build nickname entries
            final boolean isNameRawContact =
                (contactData.getNameRawContactId() == dataItem.getRawContactId());

            final boolean duplicatesTitle =
                isNameRawContact
                && contactData.getDisplayNameSource() == DisplayNameSources.NICKNAME;

            if (!duplicatesTitle) {
                header = res.getString(R.string.header_nickname_entry);
                subHeader = nickname.getName();
                entryContextMenuInfo = new EntryContextMenuInfo(subHeader, header,
                        dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
            }
        } else if (dataItem instanceof NoteDataItem) {
            final NoteDataItem note = (NoteDataItem) dataItem;
            header = res.getString(R.string.header_note_entry);
            subHeader = note.getNote();
            entryContextMenuInfo = new EntryContextMenuInfo(subHeader, header,
                    dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
        } else if (dataItem instanceof WebsiteDataItem) {
            final WebsiteDataItem website = (WebsiteDataItem) dataItem;
            header = res.getString(R.string.header_website_entry);
            subHeader = website.getUrl();
            entryContextMenuInfo = new EntryContextMenuInfo(subHeader, header,
                    dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
            try {
                final WebAddress webAddress = new WebAddress(website.buildDataStringForDisplay
                        (context, kind));
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webAddress.toString()));
            } catch (final ParseException e) {
                Log.e(TAG, "Couldn't parse website: " + website.buildDataStringForDisplay(
                        context, kind));
            }
        } else if (dataItem instanceof EventDataItem) {
            final EventDataItem event = (EventDataItem) dataItem;
            final String dataString = event.buildDataStringForDisplay(context, kind);
            final Calendar cal = DateUtils.parseDate(dataString, false);
            if (cal != null) {
                final Date nextAnniversary =
                        DateUtils.getNextAnnualDate(cal);
                final Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
                builder.appendPath("time");
                ContentUris.appendId(builder, nextAnniversary.getTime());
                intent = new Intent(Intent.ACTION_VIEW).setData(builder.build());
            }
            header = res.getString(R.string.header_event_entry);
            if (event.hasKindTypeColumn(kind)) {
                subHeader = Event.getTypeLabel(res, event.getKindTypeColumn(kind),
                        event.getLabel()).toString();
            }
            text = DateUtils.formatDate(context, dataString);
            entryContextMenuInfo = new EntryContextMenuInfo(text, header,
                    dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
        } else if (dataItem instanceof RelationDataItem) {
            final RelationDataItem relation = (RelationDataItem) dataItem;
            final String dataString = relation.buildDataStringForDisplay(context, kind);
            if (!TextUtils.isEmpty(dataString)) {
                intent = new Intent(Intent.ACTION_SEARCH);
                intent.putExtra(SearchManager.QUERY, dataString);
                intent.setType(Contacts.CONTENT_TYPE);
            }
            header = res.getString(R.string.header_relation_entry);
            subHeader = relation.getName();
            entryContextMenuInfo = new EntryContextMenuInfo(subHeader, header,
                    dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
            if (relation.hasKindTypeColumn(kind)) {
                text = Relation.getTypeLabel(res,
                        relation.getKindTypeColumn(kind),
                        relation.getLabel()).toString();
            }
        } else if (dataItem instanceof PhoneDataItem) {
            final PhoneDataItem phone = (PhoneDataItem) dataItem;
            if (!TextUtils.isEmpty(phone.getNumber())) {
                primaryContentDescription.append(res.getString(R.string.call_other)).append(" ");
                header = sBidiFormatter.unicodeWrap(phone.buildDataStringForDisplay(context, kind),
                        TextDirectionHeuristics.LTR);
                entryContextMenuInfo = new EntryContextMenuInfo(header,
                        res.getString(R.string.phoneLabelsGroup), dataItem.getMimeType(),
                        dataItem.getId(), dataItem.isSuperPrimary());
                if (phone.hasKindTypeColumn(kind)) {
                    text = Phone.getTypeLabel(res, phone.getKindTypeColumn(kind),
                            phone.getLabel()).toString();
                    ///M:[for AAS]show Primary Number/Additional Number@{
                    int subId = contactData.getIndicate();
                    subHeader = ExtensionManager.getInstance().getAasExtension().getSubheaderString(
                            subId, dataItem.getContentValues().getAsInteger(Data.DATA2));
                    text = (String) ExtensionManager.getInstance().getAasExtension()
                    .getTypeLabel(dataItem.getContentValues().getAsInteger(Data.DATA2),
                            (CharSequence) dataItem.getContentValues().getAsString(Data.DATA3),
                            (String) text, subId);
                    ///@}
                    primaryContentDescription.append(text).append(" ");
                }
                primaryContentDescription.append(header);
                icon = res.getDrawable(R.drawable.ic_phone_24dp);
                iconResourceId = R.drawable.ic_phone_24dp;
                if (PhoneCapabilityTester.isPhone(context)) {
                    intent = CallUtil.getCallIntent(phone.getNumber());
                }
                if (PhoneCapabilityTester.isSupportSms(context)) {
                    alternateIntent = new Intent(Intent.ACTION_SENDTO,
                            Uri.fromParts(ContactsUtils.SCHEME_SMSTO, phone.getNumber(), null));

                    alternateIcon = res.getDrawable(R.drawable.ic_message_24dp);
                    alternateContentDescription.append(res.getString(R.string.sms_custom, header));
                }

                // Add video call button if supported
                if ((CallUtil.isVideoEnabled(context) && PhoneCapabilityTester.isPhone(context)) ||
                        (ExtensionManager.getInstance().getContactsCommonPresenceExtension()
                        .isActivePlugin())) {
                    Log.i(TAG, "isVideoEnabled. true");
                    thirdIcon = res.getDrawable(R.drawable.ic_videocam);
                    thirdIntent = CallUtil.getVideoCallIntent(phone.getNumber(),
                            CALL_ORIGIN_QUICK_CONTACTS_ACTIVITY);
                    thirdContentDescription =
                            res.getString(R.string.description_video_call);
                }
                if(!ExtensionManager.getInstance().getContactsCommonPresenceExtension()
                        .isVideoCallCapable(phone.getNumber()) && thirdIcon != null) {
                    Log.i(TAG, "Video capabilities false, so set alpha transaprency value");
                    thirdIcon.setAlpha(100);
                }
            }
        } else if (dataItem instanceof EmailDataItem) {
            final EmailDataItem email = (EmailDataItem) dataItem;
            final String address = email.getData();
            if (!TextUtils.isEmpty(address)) {
                primaryContentDescription.append(res.getString(R.string.email_other)).append(" ");
                final Uri mailUri = Uri.fromParts(ContactsUtils.SCHEME_MAILTO, address, null);
                intent = new Intent(Intent.ACTION_SENDTO, mailUri);
                header = email.getAddress();
                entryContextMenuInfo = new EntryContextMenuInfo(header,
                        res.getString(R.string.emailLabelsGroup), dataItem.getMimeType(),
                        dataItem.getId(), dataItem.isSuperPrimary());
                if (email.hasKindTypeColumn(kind)) {
                    text = Email.getTypeLabel(res, email.getKindTypeColumn(kind),
                            email.getLabel()).toString();
                    primaryContentDescription.append(text).append(" ");
                }
                primaryContentDescription.append(header);
                icon = res.getDrawable(R.drawable.ic_email_24dp);
                iconResourceId = R.drawable.ic_email_24dp;
            }
        } else if (dataItem instanceof StructuredPostalDataItem) {
            StructuredPostalDataItem postal = (StructuredPostalDataItem) dataItem;
            final String postalAddress = postal.getFormattedAddress();
            if (!TextUtils.isEmpty(postalAddress)) {
                primaryContentDescription.append(res.getString(R.string.map_other)).append(" ");
                intent = StructuredPostalUtils.getViewPostalAddressIntent(postalAddress);
                header = postal.getFormattedAddress();
                entryContextMenuInfo = new EntryContextMenuInfo(header,
                        res.getString(R.string.postalLabelsGroup), dataItem.getMimeType(),
                        dataItem.getId(), dataItem.isSuperPrimary());
                if (postal.hasKindTypeColumn(kind)) {
                    text = StructuredPostal.getTypeLabel(res,
                            postal.getKindTypeColumn(kind), postal.getLabel()).toString();
                    primaryContentDescription.append(text).append(" ");
                }
                primaryContentDescription.append(header);
                alternateIntent =
                        StructuredPostalUtils.getViewPostalAddressDirectionsIntent(postalAddress);
                alternateIcon = res.getDrawable(R.drawable.ic_directions_24dp);
                alternateContentDescription.append(res.getString(
                        R.string.content_description_directions)).append(" ").append(header);
                icon = res.getDrawable(R.drawable.ic_place_24dp);
                iconResourceId = R.drawable.ic_place_24dp;
            }
        } else if (dataItem instanceof SipAddressDataItem) {
            final SipAddressDataItem sip = (SipAddressDataItem) dataItem;
            final String address = sip.getSipAddress();
            if (!TextUtils.isEmpty(address)) {
                QuickContactUtils.setSipAddress(address);
                primaryContentDescription.append(res.getString(R.string.call_other)).append(
                        " ");
                if (PhoneCapabilityTester.isSipPhone(context)) {
                    final Uri callUri = Uri.fromParts(PhoneAccount.SCHEME_SIP, address, null);
                    intent = CallUtil.getCallIntent(callUri);
                }
                header = address;
                entryContextMenuInfo = new EntryContextMenuInfo(header,
                        res.getString(R.string.phoneLabelsGroup), dataItem.getMimeType(),
                        dataItem.getId(), dataItem.isSuperPrimary());
                if (sip.hasKindTypeColumn(kind)) {
                    text = SipAddress.getTypeLabel(res,
                            sip.getKindTypeColumn(kind), sip.getLabel()).toString();
                    primaryContentDescription.append(text).append(" ");
                }
                primaryContentDescription.append(header);
                icon = res.getDrawable(R.drawable.ic_dialer_sip_black_24dp);
                iconResourceId = R.drawable.ic_dialer_sip_black_24dp;
            }
        } else if (dataItem instanceof StructuredNameDataItem) {
            // If the name is already set and this is not the super primary value then leave the
            // current value. This way we show the super primary value when we are able to.
            if (dataItem.isSuperPrimary() || aboutCardName.value == null
                    || aboutCardName.value.isEmpty()) {
                final String givenName = ((StructuredNameDataItem) dataItem).getGivenName();
                if (!TextUtils.isEmpty(givenName)) {
                    aboutCardName.value = res.getString(R.string.about_card_title) +
                            " " + givenName;
                } else {
                    aboutCardName.value = res.getString(R.string.about_card_title);
                }
            }
        } else if (dataItem instanceof ImsCallDataItem) { // M: add IMS Call
            if (ContactsSystemProperties.MTK_VOLTE_SUPPORT &&
                    ContactsSystemProperties.MTK_IMS_SUPPORT) {
                final ImsCallDataItem ims = (ImsCallDataItem) dataItem;
                String imsUri = ims.getUrl();
                if (!TextUtils.isEmpty(imsUri)) {
                    String imsLabel = ims.getLabel();
                    Log.d(TAG, "imsUri: " + imsUri + ", imsLabel: " + imsLabel);
                    intent = CallUtil.getCallIntent(Uri.fromParts(PhoneAccount.SCHEME_TEL,
                            imsUri, null), null, Constants.DIAL_NUMBER_INTENT_IMS);
                    icon = res.getDrawable(R.drawable.ic_dialer_ims_black);
                    text = res.getString(R.string.imsCallLabelsGroup);
                    header = imsUri;
                }
            }
        /// M: Group member ship.
        } else if (dataItem instanceof GroupMembershipDataItem) {
            final GroupMembershipDataItem groupDataItem = (GroupMembershipDataItem) dataItem;
            String groupTitle = QuickContactUtils.getGroupTitle(contactData.getGroupMetaData(),
                    groupDataItem.getGroupRowId());
            if (!TextUtils.isEmpty(groupTitle)) {
                header = res.getString(R.string.contact_detail_group_list_title);
                subHeader = groupTitle;
            }
        } else {
            // Custom DataItem
            header = dataItem.buildDataStringForDisplay(context, kind);
            text = kind.typeColumn;
            intent = new Intent(Intent.ACTION_VIEW);
            final Uri uri = ContentUris.withAppendedId(Data.CONTENT_URI, dataItem.getId());
            intent.setDataAndType(uri, dataItem.getMimeType());

            if (intent != null) {
                final String mimetype = intent.getType();

                // Build advanced entry for known 3p types. Otherwise default to ResolveCache icon.
                switch (mimetype) {
                    case MIMETYPE_GPLUS_PROFILE:
                        // If a secondDataItem is available, use it to build an entry with
                        // alternate actions
                        if (secondDataItem != null) {
                            icon = res.getDrawable(R.drawable.ic_google_plus_24dp);
                            alternateIcon = res.getDrawable(R.drawable.ic_add_to_circles_black_24);
                            final GPlusOrHangoutsDataItemModel itemModel =
                                    new GPlusOrHangoutsDataItemModel(intent, alternateIntent,
                                            dataItem, secondDataItem, alternateContentDescription,
                                            header, text, context);

                            populateGPlusOrHangoutsDataItemModel(itemModel);
                            intent = itemModel.intent;
                            alternateIntent = itemModel.alternateIntent;
                            alternateContentDescription = itemModel.alternateContentDescription;
                            header = itemModel.header;
                            text = itemModel.text;
                        } else {
                            if (GPLUS_PROFILE_DATA_5_ADD_TO_CIRCLE.equals(
                                    intent.getDataString())) {
                                icon = res.getDrawable(R.drawable.ic_add_to_circles_black_24);
                            } else {
                                icon = res.getDrawable(R.drawable.ic_google_plus_24dp);
                            }
                        }
                        break;
                    case MIMETYPE_HANGOUTS:
                        // If a secondDataItem is available, use it to build an entry with
                        // alternate actions
                        if (secondDataItem != null) {
                            icon = res.getDrawable(R.drawable.ic_hangout_24dp);
                            alternateIcon = res.getDrawable(R.drawable.ic_hangout_video_24dp);
                            final GPlusOrHangoutsDataItemModel itemModel =
                                    new GPlusOrHangoutsDataItemModel(intent, alternateIntent,
                                            dataItem, secondDataItem, alternateContentDescription,
                                            header, text, context);

                            populateGPlusOrHangoutsDataItemModel(itemModel);
                            intent = itemModel.intent;
                            alternateIntent = itemModel.alternateIntent;
                            alternateContentDescription = itemModel.alternateContentDescription;
                            header = itemModel.header;
                            text = itemModel.text;
                        } else {
                            if (HANGOUTS_DATA_5_VIDEO.equals(intent.getDataString())) {
                                icon = res.getDrawable(R.drawable.ic_hangout_video_24dp);
                            } else {
                                icon = res.getDrawable(R.drawable.ic_hangout_24dp);
                            }
                        }
                        break;
                    default:
                        entryContextMenuInfo = new EntryContextMenuInfo(header, mimetype,
                                dataItem.getMimeType(), dataItem.getId(),
                                dataItem.isSuperPrimary());
                        icon = ResolveCache.getInstance(context).getIcon(
                                dataItem.getMimeType(), intent);
                        // Call mutate to create a new Drawable.ConstantState for color filtering
                        if (icon != null) {
                            icon.mutate();
                        }
                        shouldApplyColor = false;
                }
            }
        }

        if (intent != null) {
            // Do not set the intent is there are no resolves
            if (!PhoneCapabilityTester.isIntentRegistered(context, intent)) {
                intent = null;
            }
        }

        if (alternateIntent != null) {
            // Do not set the alternate intent is there are no resolves
            if (!PhoneCapabilityTester.isIntentRegistered(context, alternateIntent)) {
                alternateIntent = null;
            } else if (TextUtils.isEmpty(alternateContentDescription)) {
                // Attempt to use package manager to find a suitable content description if needed
                alternateContentDescription.append(getIntentResolveLabel(alternateIntent, context));
            }
        }

        // If the Entry has no visual elements, return null
        if (icon == null && TextUtils.isEmpty(header) && TextUtils.isEmpty(subHeader) &&
                subHeaderIcon == null && TextUtils.isEmpty(text) && textIcon == null) {
            return null;
        }

        // Ignore dataIds from the Me profile.
        final int dataId = dataItem.getId() > Integer.MAX_VALUE ?
                -1 : (int) dataItem.getId();

        /* M: add sim icon & sim name */
        return new Entry(dataId, icon, header, subHeader, subHeaderIcon, text, textIcon, null, null,
                new SpannableString(primaryContentDescription.toString()),
                intent, alternateIcon, alternateIntent,
                alternateContentDescription.toString(), shouldApplyColor, isEditable,
                entryContextMenuInfo, thirdIcon, thirdIntent, thirdContentDescription,
                iconResourceId);
    }

    private List<Entry> dataItemsToEntries(List<DataItem> dataItems,
            MutableString aboutCardTitleOut) {
        // Hangouts and G+ use two data items to create one entry.
        if (dataItems.get(0).getMimeType().equals(MIMETYPE_GPLUS_PROFILE) ||
                dataItems.get(0).getMimeType().equals(MIMETYPE_HANGOUTS)) {
            return gPlusOrHangoutsDataItemsToEntries(dataItems);
        } else {
            final List<Entry> entries = new ArrayList<>();
            for (DataItem dataItem : dataItems) {
                final Entry entry = dataItemToEntry(dataItem, /* secondDataItem = */ null,
                        this, mContactData, aboutCardTitleOut);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            return entries;
        }
    }

    /**
     * G+ and Hangout entries are unique in that a single ExpandingEntryCardView.Entry consists
     * of two data items. This method attempts to build each entry using the two data items if
     * they are available. If there are more or less than two data items, a fall back is used
     * and each data item gets its own entry.
     */
    private List<Entry> gPlusOrHangoutsDataItemsToEntries(List<DataItem> dataItems) {
        final List<Entry> entries = new ArrayList<>();
        final Map<Long, List<DataItem>> buckets = new HashMap<>();
        // Put the data items into buckets based on the raw contact id
        for (DataItem dataItem : dataItems) {
            List<DataItem> bucket = buckets.get(dataItem.getRawContactId());
            if (bucket == null) {
                bucket = new ArrayList<>();
                buckets.put(dataItem.getRawContactId(), bucket);
            }
            bucket.add(dataItem);
        }

        // Use the buckets to build entries. If a bucket contains two data items, build the special
        // entry, otherwise fall back to the normal entry.
        for (List<DataItem> bucket : buckets.values()) {
            if (bucket.size() == 2) {
                // Use the pair to build an entry
                final Entry entry = dataItemToEntry(bucket.get(0),
                        /* secondDataItem = */ bucket.get(1), this, mContactData,
                        /* aboutCardName = */ null);
                if (entry != null) {
                    entries.add(entry);
                }
            } else {
                for (DataItem dataItem : bucket) {
                    final Entry entry = dataItemToEntry(dataItem, /* secondDataItem = */ null,
                            this, mContactData, /* aboutCardName = */ null);
                    if (entry != null) {
                        entries.add(entry);
                    }
                }
            }
        }
        return entries;
    }

    /**
     * Used for statically passing around G+ or Hangouts data items and entry fields to
     * populateGPlusOrHangoutsDataItemModel.
     */
    private static final class GPlusOrHangoutsDataItemModel {
        public Intent intent;
        public Intent alternateIntent;
        public DataItem dataItem;
        public DataItem secondDataItem;
        public StringBuilder alternateContentDescription;
        public String header;
        public String text;
        public Context context;

        public GPlusOrHangoutsDataItemModel(Intent intent, Intent alternateIntent,
                DataItem dataItem,
                DataItem secondDataItem, StringBuilder alternateContentDescription, String header,
                String text, Context context) {
            this.intent = intent;
            this.alternateIntent = alternateIntent;
            this.dataItem = dataItem;
            this.secondDataItem = secondDataItem;
            this.alternateContentDescription = alternateContentDescription;
            this.header = header;
            this.text = text;
            this.context = context;
        }
    }

    private static void populateGPlusOrHangoutsDataItemModel(
            GPlusOrHangoutsDataItemModel dataModel) {
        final Intent secondIntent = new Intent(Intent.ACTION_VIEW);
        secondIntent.setDataAndType(ContentUris.withAppendedId(Data.CONTENT_URI,
                dataModel.secondDataItem.getId()), dataModel.secondDataItem.getMimeType());
        // There is no guarantee the order the data items come in. Second
        // data item does not necessarily mean it's the alternate.
        // Hangouts video and Add to circles should be alternate. Swap if needed
        if (HANGOUTS_DATA_5_VIDEO.equals(
                dataModel.dataItem.getContentValues().getAsString(Data.DATA5)) ||
                GPLUS_PROFILE_DATA_5_ADD_TO_CIRCLE.equals(
                        dataModel.dataItem.getContentValues().getAsString(Data.DATA5))) {
            dataModel.alternateIntent = dataModel.intent;
            dataModel.alternateContentDescription = new StringBuilder(dataModel.header);

            dataModel.intent = secondIntent;
            dataModel.header = dataModel.secondDataItem.buildDataStringForDisplay(dataModel.context,
                    dataModel.secondDataItem.getDataKind());
            dataModel.text = dataModel.secondDataItem.getDataKind().typeColumn;
        } else if (HANGOUTS_DATA_5_MESSAGE.equals(
                dataModel.dataItem.getContentValues().getAsString(Data.DATA5)) ||
                GPLUS_PROFILE_DATA_5_VIEW_PROFILE.equals(
                        dataModel.dataItem.getContentValues().getAsString(Data.DATA5))) {
            dataModel.alternateIntent = secondIntent;
            dataModel.alternateContentDescription = new StringBuilder(
                    dataModel.secondDataItem.buildDataStringForDisplay(dataModel.context,
                            dataModel.secondDataItem.getDataKind()));
        }
    }

    private static String getIntentResolveLabel(Intent intent, Context context) {
        final List<ResolveInfo> matches = context.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);

        // Pick first match, otherwise best found
        ResolveInfo bestResolve = null;
        final int size = matches.size();
        if (size == 1) {
            bestResolve = matches.get(0);
        } else if (size > 1) {
            bestResolve = ResolveCache.getInstance(context).getBestResolve(intent, matches);
        }

        if (bestResolve == null) {
            return null;
        }

        return String.valueOf(bestResolve.loadLabel(context.getPackageManager()));
    }

    /**
     * Asynchronously extract the most vibrant color from the PhotoView. Once extracted,
     * apply this tint to {@link MultiShrinkScroller}. This operation takes about 20-30ms
     * on a Nexus 5.
     */
    private void extractAndApplyTintFromPhotoViewAsynchronously() {
        if (mScroller == null) {
            return;
        }
        final Drawable imageViewDrawable = mPhotoView.getDrawable();
        new AsyncTask<Void, Void, MaterialPalette>() {
            @Override
            protected MaterialPalette doInBackground(Void... params) {
                if (mContactData == null) {
                    Log.w(TAG, "[doInBackground] mContactData is null...");
                }

                if (imageViewDrawable instanceof BitmapDrawable && mContactData != null
                        && mContactData.getThumbnailPhotoBinaryData() != null
                        && mContactData.getThumbnailPhotoBinaryData().length > 0) {
                    // Perform the color analysis on the thumbnail instead of the full sized
                    // image, so that our results will be as similar as possible to the Bugle
                    // app.
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(
                            mContactData.getThumbnailPhotoBinaryData(), 0,
                            mContactData.getThumbnailPhotoBinaryData().length);
                    try {
                        final int primaryColor = colorFromBitmap(bitmap);
                        if (primaryColor != 0) {
                            return mMaterialColorMapUtils.calculatePrimaryAndSecondaryColor(
                                    primaryColor);
                        }
                    } finally {
                        bitmap.recycle();
                    }
                }
                if (imageViewDrawable instanceof LetterTileDrawable) {
                    final int primaryColor = ((LetterTileDrawable) imageViewDrawable).getColor();
                    return mMaterialColorMapUtils.calculatePrimaryAndSecondaryColor(primaryColor);
                }
                return MaterialColorMapUtils.getDefaultPrimaryAndSecondaryColors(getResources());
            }

            @Override
            protected void onPostExecute(MaterialPalette palette) {
                super.onPostExecute(palette);
                //M:OP01 RCS will get photo from rcs server, and refresh thumbnail Photo.@{
                mHasComputedThemeColor = ExtensionManager.getInstance().getRcsExtension().
                        needUpdateContactPhoto(imageViewDrawable instanceof LetterTileDrawable,
                        mHasComputedThemeColor);
                /** @} */
                if (mHasComputedThemeColor) {
                    // If we had previously computed a theme color from the contact photo,
                    // then do not update the theme color. Changing the theme color several
                    // seconds after QC has started, as a result of an updated/upgraded photo,
                    // is a jarring experience. On the other hand, changing the theme color after
                    // a rotation or onNewIntent() is perfectly fine.
                    return;
                }
                // Check that the Photo has not changed. If it has changed, the new tint
                // color needs to be extracted
                if (imageViewDrawable == mPhotoView.getDrawable()) {
                    mHasComputedThemeColor = true;
                    setThemeColor(palette);
                }
            }
        }.execute();
    }

    private void setThemeColor(MaterialPalette palette) {
        // If the color is invalid, use the predefined default
        mColorFilterColor = palette.mPrimaryColor;
        mScroller.setHeaderTintColor(mColorFilterColor);
        mStatusBarColor = palette.mSecondaryColor;
        updateStatusBarColor();

        mColorFilter =
                new PorterDuffColorFilter(mColorFilterColor, PorterDuff.Mode.SRC_ATOP);
        mContactCard.setColorAndFilter(mColorFilterColor, mColorFilter);
        mRecentCard.setColorAndFilter(mColorFilterColor, mColorFilter);
        mAboutCard.setColorAndFilter(mColorFilterColor, mColorFilter);
        /// M: [for RCS-e]
        if (mJoynCard != null) {
            mJoynCard.setColorAndFilter(mColorFilterColor, mColorFilter);
        }
    }

    private void updateStatusBarColor() {
        if (mScroller == null) {
            return;
        }
        final int desiredStatusBarColor;
        // Only use a custom status bar color if QuickContacts touches the top of the viewport.
        if (mScroller.getScrollNeededToBeFullScreen() <= 0) {
            desiredStatusBarColor = mStatusBarColor;
        } else {
            desiredStatusBarColor = Color.TRANSPARENT;
        }
        // Animate to the new color.
        final ObjectAnimator animation = ObjectAnimator.ofInt(getWindow(), "statusBarColor",
                getWindow().getStatusBarColor(), desiredStatusBarColor);
        animation.setDuration(ANIMATION_STATUS_BAR_COLOR_CHANGE_DURATION);
        animation.setEvaluator(new ArgbEvaluator());
        animation.start();
    }

    private int colorFromBitmap(Bitmap bitmap) {
        // Author of Palette recommends using 24 colors when analyzing profile photos.
        final int NUMBER_OF_PALETTE_COLORS = 24;
        final Palette palette = Palette.generate(bitmap, NUMBER_OF_PALETTE_COLORS);
        if (palette != null && palette.getVibrantSwatch() != null) {
            return palette.getVibrantSwatch().getRgb();
        }
        return 0;
    }

    private List<Entry> contactInteractionsToEntries(List<ContactInteraction> interactions) {
        final List<Entry> entries = new ArrayList<>();
        for (ContactInteraction interaction : interactions) {
            if (interaction == null) {
                continue;
            }
            entries.add(new Entry(/* id = */ -1,
                    interaction.getIcon(this),
                    interaction.getViewHeader(this),
                    interaction.getViewBody(this),
                    interaction.getBodyIcon(this),
                    interaction.getViewFooter(this),
                    interaction.getFooterIcon(this),
                    /* M: add sim icon @ { */
                    interaction.getSimIcon(this),
                    interaction.getSimName(this),
                    /* @ } */
                    interaction.getContentDescription(this),
                    interaction.getIntent(),
                    /* alternateIcon = */ null,
                    /* alternateIntent = */ null,
                    /* alternateContentDescription = */ null,
                    /* shouldApplyColor = */ true,
                    /* isEditable = */ false,
                    /* EntryContextMenuInfo = */ null,
                    /* thirdIcon = */ null,
                    /* thirdIntent = */ null,
                    /* thirdContentDescription = */ null,
                    interaction.getIconResourceId()));
        }
        return entries;
    }

    private final LoaderCallbacks<Contact> mLoaderContactCallbacks =
            new LoaderCallbacks<Contact>() {
        @Override
        public void onLoaderReset(Loader<Contact> loader) {
            Log.d(TAG, "[onLoaderReset], mContactData been set null");
            mContactData = null;
        }

        @Override
        public void onLoadFinished(Loader<Contact> loader, Contact data) {
            Trace.beginSection("onLoadFinished()");
            try {

                if (isFinishing()) {
                    return;
                }
                if (data.isError()) {
                    // This means either the contact is invalid or we had an
                    // internal error such as an acore crash.
                    Log.i(TAG, "Failed to load contact: " + ((ContactLoader)loader).getLookupUri());
                    Toast.makeText(QuickContactActivity.this, R.string.invalidContactMessage,
                            Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                if (data.isNotFound()) {
                    Log.i(TAG, "No contact found: " + ((ContactLoader)loader).getLookupUri());
                    Toast.makeText(QuickContactActivity.this, R.string.invalidContactMessage,
                            Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                Log.d(TAG, "onLoadFinished " + " | data.getContactId() : "
                        + data.getContactId() + " | data.getUri() : " + data.getUri());

                bindContactData(data);

                ///M:[for rcs] update Rcs contact on the top left@{
                ExtensionManager.getInstance().getRcsExtension()
                        .getQuickContactRcsScroller()
                        .updateRcsContact(mContactLoader.getLookupUri(), true);
                ///@}
            } finally {
                Trace.endSection();
            }
        }

        @Override
        public Loader<Contact> onCreateLoader(int id, Bundle args) {
            if (mLookupUri == null) {
                Log.wtf(TAG, "Lookup uri wasn't initialized. Loader was started too early");
            }
            // Load all contact data. We need loadGroupMetaData=true to determine whether the
            // contact is invisible. If it is, we need to display an "Add to Contacts" MenuItem.
            return new ContactLoader(getApplicationContext(), mLookupUri,
                    true /*loadGroupMetaData*/, false /*loadInvitableAccountTypes*/,
                    true /*postViewNotification*/, true /*computeFormattedPhoneNumber*/);
        }
    };

    @Override
    public void onBackPressed() {
        if (mScroller != null) {
            if (!mIsExitAnimationInProgress) {
                mScroller.scrollOffBottom();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void finish() {
        super.finish();

        // override transitions to skip the standard window animations
        overridePendingTransition(0, 0);
    }

    private final LoaderCallbacks<List<ContactInteraction>> mLoaderInteractionsCallbacks =
            new LoaderCallbacks<List<ContactInteraction>>() {

        @Override
        public Loader<List<ContactInteraction>> onCreateLoader(int id, Bundle args) {
            Loader<List<ContactInteraction>> loader = null;
            switch (id) {
                case LOADER_SMS_ID:
                    loader = new SmsInteractionsLoader(
                            QuickContactActivity.this,
                            args.getStringArray(KEY_LOADER_EXTRA_PHONES),
                            MAX_SMS_RETRIEVE);
                    break;
                case LOADER_CALENDAR_ID:
                    final String[] emailsArray = args.getStringArray(KEY_LOADER_EXTRA_EMAILS);
                    List<String> emailsList = null;
                    if (emailsArray != null) {
                        emailsList = Arrays.asList(args.getStringArray(KEY_LOADER_EXTRA_EMAILS));
                    }
                    loader = new CalendarInteractionsLoader(
                            QuickContactActivity.this,
                            emailsList,
                            MAX_FUTURE_CALENDAR_RETRIEVE,
                            MAX_PAST_CALENDAR_RETRIEVE,
                            FUTURE_MILLISECOND_TO_SEARCH_LOCAL_CALENDAR,
                            PAST_MILLISECOND_TO_SEARCH_LOCAL_CALENDAR);
                    break;
                case LOADER_CALL_LOG_ID:
                    loader = new CallLogInteractionsLoader(
                            QuickContactActivity.this,
                            args.getStringArray(KEY_LOADER_EXTRA_PHONES),
                            MAX_CALL_LOG_RETRIEVE);
            }
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<List<ContactInteraction>> loader,
                List<ContactInteraction> data) {
            mRecentLoaderResults.put(loader.getId(), data);

            if (isAllRecentDataLoaded()) {
                bindRecentData();
            }
        }

        @Override
        public void onLoaderReset(Loader<List<ContactInteraction>> loader) {
            mRecentLoaderResults.remove(loader.getId());
        }
    };

    private boolean isAllRecentDataLoaded() {
        return mRecentLoaderResults.size() == mRecentLoaderIds.length;
    }

    private void bindRecentData() {
        final List<ContactInteraction> allInteractions = new ArrayList<>();
        final List<List<Entry>> interactionsWrapper = new ArrayList<>();

        // Serialize mRecentLoaderResults into a single list. This should be done on the main
        // thread to avoid races against mRecentLoaderResults edits.
        for (List<ContactInteraction> loaderInteractions : mRecentLoaderResults.values()) {
            allInteractions.addAll(loaderInteractions);
        }

        mRecentDataTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Trace.beginSection("sort recent loader results");

                // Sort the interactions by most recent
                Collections.sort(allInteractions, new Comparator<ContactInteraction>() {
                    @Override
                    public int compare(ContactInteraction a, ContactInteraction b) {
                        if (a == null && b == null) {
                            return 0;
                        }
                        if (a == null) {
                            return 1;
                        }
                        if (b == null) {
                            return -1;
                        }
                        if (a.getInteractionDate() > b.getInteractionDate()) {
                            return -1;
                        }
                        if (a.getInteractionDate() == b.getInteractionDate()) {
                            return 0;
                        }
                        return 1;
                    }
                });

                Trace.endSection();
                Trace.beginSection("contactInteractionsToEntries");

                // Wrap each interaction in its own list so that an icon is displayed for each entry
                for (Entry contactInteraction : contactInteractionsToEntries(allInteractions)) {
                    List<Entry> entryListWrapper = new ArrayList<>(1);
                    entryListWrapper.add(contactInteraction);
                    interactionsWrapper.add(entryListWrapper);
                }

                Trace.endSection();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Trace.beginSection("initialize recents card");

                if (allInteractions.size() > 0) {
                    mRecentCard.initialize(interactionsWrapper,
                    /* numInitialVisibleEntries = */ MIN_NUM_COLLAPSED_RECENT_ENTRIES_SHOWN,
                    /* isExpanded = */ mRecentCard.isExpanded(), /* isAlwaysExpanded = */ false,
                            mExpandingEntryCardViewListener, mScroller);
                    mRecentCard.setVisibility(View.VISIBLE);
                } else {
                    /// M: Fix ALPS01763309
                    mRecentCard.setVisibility(View.GONE);
                }

                Trace.endSection();

                // About card is initialized along with the contact card, but since it appears after
                // the recent card in the UI, we hold off until making it visible until the recent
                // card is also ready to avoid stuttering.
                if (mAboutCard.shouldShow()) {
                    mAboutCard.setVisibility(View.VISIBLE);
                } else {
                    mAboutCard.setVisibility(View.GONE);
                }
                mRecentDataTask = null;
            }
        };
        mRecentDataTask.execute();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mEntriesAndActionsTask != null) {
            // Once the activity is stopped, we will no longer want to bind mEntriesAndActionsTask's
            // results on the UI thread. In some circumstances Activities are killed without
            // onStop() being called. This is not a problem, because in these circumstances
            // the entire process will be killed.
            mEntriesAndActionsTask.cancel(/* mayInterruptIfRunning = */ false);
        }
        if (mRecentDataTask != null) {
            mRecentDataTask.cancel(/* mayInterruptIfRunning = */ false);
        }
    }

    /**
     * M: sdn contact isn't possible to edit.
     * Returns true if it is possible to edit the current contact.
     */
    private boolean isContactEditable() {
        return mContactData != null && !mContactData.isDirectoryEntry() &&
            !mContactData.isSdnContacts();
    }

    /**
     * Returns true if it is possible to share the current contact.
     */
    private boolean isContactShareable() {
        return mContactData != null && !mContactData.isDirectoryEntry();
    }

    /// M: add isEditingUserProfile flag for user profile feature
    private Intent getEditContactIntent() {
        return EditorIntents.createCompactEditContactIntent(
                mContactData.getLookupUri(),
                mHasComputedThemeColor
                        ? new MaterialPalette(mColorFilterColor, mStatusBarColor) : null,
                /* updatedPhotos =*/ null,
                mContactData.getPhotoId(),
                mContactData.getNameRawContactId(),
                /*isEditingUserProfile = */ false);
    }

    private void editContact() {
        mHasIntentLaunched = true;
        mContactLoader.cacheResult();
        startActivityForResult(getEditContactIntent(), REQUEST_CODE_CONTACT_EDITOR_ACTIVITY);
    }

    private void deleteContact() {
        final Uri contactUri = mContactData.getLookupUri();
        ContactDeletionInteraction.start(this, contactUri, /* finishActivityWhenDone =*/ true);
    }

    private void toggleStar(MenuItem starredMenuItem) {
        // Make sure there is a contact
        if (mContactData != null) {
            // Read the current starred value from the UI instead of using the last
            // loaded state. This allows rapid tapping without writing the same
            // value several times
            final boolean isStarred = starredMenuItem.isChecked();

            // To improve responsiveness, swap out the picture (and tag) in the UI already
            ContactDisplayUtils.configureStarredMenuItem(starredMenuItem,
                    mContactData.isDirectoryEntry(), mContactData.isUserProfile(),
                    !isStarred);

            // Now perform the real save
            final Intent intent = ContactSaveService.createSetStarredIntent(
                    QuickContactActivity.this, mContactData.getLookupUri(), !isStarred);
            startService(intent);

            final CharSequence accessibilityText = !isStarred
                    ? getResources().getText(R.string.description_action_menu_add_star)
                    : getResources().getText(R.string.description_action_menu_remove_star);
            // Accessibility actions need to have an associated view. We can't access the MenuItem's
            // underlying view, so put this accessibility action on the root view.
            mScroller.announceForAccessibility(accessibilityText);
        }
    }
    ///M:
     /**
     * Calls into the contacts provider to get a pre-authorized version of the given URI.
     */
    private Uri getPreAuthorizedUri(Uri uri) {
        final Bundle uriBundle = new Bundle();
        uriBundle.putParcelable(ContactsContract.Authorization.KEY_URI_TO_AUTHORIZE, uri);
        final Bundle authResponse = getContentResolver().call(
                ContactsContract.AUTHORITY_URI,
                ContactsContract.Authorization.AUTHORIZATION_METHOD,
                null,
                uriBundle);
        if (authResponse != null) {
            return (Uri) authResponse.getParcelable(
                    ContactsContract.Authorization.KEY_AUTHORIZED_URI);
        } else {
            return uri;
        }
    }

    private void shareContact() {
        Log.d(TAG,"[shareContact]");
        final String lookupKey = mContactData.getLookupKey();
        Uri shareUri = Uri.withAppendedPath(Contacts.CONTENT_VCARD_URI, lookupKey);
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(Contacts.CONTENT_VCARD_TYPE);
        if (mContactData.isUserProfile()) {
            // User is sharing the profile.  We don't want to force the receiver to have
            // the highly-privileged READ_PROFILE permission, so we need to request a
            // pre-authorized URI from the provider.
            shareUri = getPreAuthorizedUri(shareUri);
            /** M for ALPS01752410 @{*/
            intent.putExtra("userProfile", "true");
        } else {
            intent.putExtra("contactId", String.valueOf(mContactData.getContactId()));
            /** @} */
        }
        intent.putExtra(Intent.EXTRA_STREAM, shareUri);
        /// M: Bug fix ALPS01749969, google default bug, need add the extra ARG_CALLING_ACTIVITY.
        intent.putExtra(VCardCommonArguments.ARG_CALLING_ACTIVITY,
                PeopleActivity.class.getName());

        // Launch chooser to share contact via
        final CharSequence chooseTitle = getText(R.string.share_via);
        final Intent chooseIntent = Intent.createChooser(intent, chooseTitle);

        try {
            mHasIntentLaunched = true;
            ImplicitIntentsUtil.startActivityOutsideApp(this, chooseIntent);
        } catch (final ActivityNotFoundException ex) {
            Toast.makeText(this, R.string.share_error, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Creates a launcher shortcut with the current contact.
     */
    private void createLauncherShortcutWithContact() {
        final ShortcutIntentBuilder builder = new ShortcutIntentBuilder(this,
                new OnShortcutIntentCreatedListener() {

                    @Override
                    public void onShortcutIntentCreated(Uri uri, Intent shortcutIntent) {
                        // Broadcast the shortcutIntent to the launcher to create a
                        // shortcut to this contact
                        shortcutIntent.setAction(ACTION_INSTALL_SHORTCUT);
                        QuickContactActivity.this.sendBroadcast(shortcutIntent);

                        // Send a toast to give feedback to the user that a shortcut to this
                        // contact was added to the launcher.
                        Toast.makeText(QuickContactActivity.this,
                                R.string.createContactShortcutSuccessful,
                                Toast.LENGTH_SHORT).show();
                    }

                });
        builder.createContactShortcutIntent(mContactData.getLookupUri());
    }

    private boolean isShortcutCreatable() {
        if (mContactData == null || mContactData.isUserProfile() ||
                mContactData.isDirectoryEntry()) {
            return false;
        }
        final Intent createShortcutIntent = new Intent();
        createShortcutIntent.setAction(ACTION_INSTALL_SHORTCUT);
        final List<ResolveInfo> receivers = getPackageManager()
                .queryBroadcastReceivers(createShortcutIntent, 0);
        return receivers != null && receivers.size() > 0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.quickcontact, menu);
        /// M: Add print item for bluetooth.
        if (ContactsSystemProperties.isSupportBtProfileBpp()) {
            menu.add(Menu.NONE, MENU_ITEM_PRINT, Menu.NONE, getResources().
                    getString(R.string.menu_print));;
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mContactData != null) {
            final MenuItem starredMenuItem = menu.findItem(R.id.menu_star);
            ContactDisplayUtils.configureStarredMenuItem(starredMenuItem,
                    mContactData.isDirectoryEntry(), mContactData.isUserProfile(),
                    mContactData.getStarred());

            /// M: Disable sim contact star menu.
            if (mContactData.getIndicate() > 0) {
                starredMenuItem.setVisible(false);
            }
            /// M: Configure hotknot MenuItem, as planner's design if hotknot supported in the
            // project, replace edit nenu with hotknot nenu, or it will keep google's design.
            final MenuItem editMenuItem = menu.findItem(R.id.menu_edit);
            final MenuItem hotknotMenuItem = menu.findItem(R.id.menu_hotknot);
            Log.d(TAG, "is sdn contact: " + mContactData.isSdnContacts());
            if (ContactsSystemProperties.MTK_HOTKNOT_SUPPORT
                    // M: Add for ALPS02192621, check whether we can use HotKnot now.
                    && HotKnotAdapter.getDefaultAdapter(this) != null
                    && !DirectoryContactUtil.isDirectoryContact(mContactData)) {
                hotknotMenuItem.setVisible(true);
                hotknotMenuItem.setIcon(R.drawable.ic_contacts_hotknot);
                /// M: hide edit nenu if it is a sdn contact.
                if (mContactData.isSdnContacts()) {
                    editMenuItem.setVisible(false);
                }
            } else {
                hotknotMenuItem.setVisible(false);
                // Configure edit MenuItem
                /// M: hide edit nenu if it is a sdn contact.
                if (mContactData.isSdnContacts()) {
                    editMenuItem.setVisible(false);
                } else {
                    editMenuItem.setVisible(true);
                    editMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    if (DirectoryContactUtil.isDirectoryContact(mContactData) ||
                            InvisibleContactUtil.isInvisibleAndAddable(mContactData, this)) {
                        editMenuItem.setIcon(R.drawable.ic_person_add_tinted_24dp);
                        editMenuItem.setTitle(R.string.menu_add_contact);
                    } else if (isContactEditable()) {
                        editMenuItem.setIcon(R.drawable.ic_create_24dp);
                        editMenuItem.setTitle(R.string.menu_editContact);
                    } else {
                        editMenuItem.setVisible(false);
                    }
                }
            }

            final MenuItem deleteMenuItem = menu.findItem(R.id.menu_delete);
            deleteMenuItem.setVisible(isContactEditable());

            final MenuItem shareMenuItem = menu.findItem(R.id.menu_share);
            shareMenuItem.setVisible(isContactShareable());

            ///M:add print item for bluetooth@{
            if (ContactsSystemProperties.isSupportBtProfileBpp()) {
                MenuItem printItem = menu.findItem(MENU_ITEM_PRINT);
                printItem.setTitle(R.string.menu_print);
                printItem.setVisible(isContactShareable());
            }
            ///@}

            final MenuItem shortcutMenuItem = menu.findItem(R.id.menu_create_contact_shortcut);
            /// M: hide the shortcut menu when it is sim contact.
            if (mContactData != null && mContactData.getIndicate() >= 0) {
                shortcutMenuItem.setVisible(false);
                Log.d(TAG, "contact indicator: " + mContactData.getIndicate());
            } else {
                shortcutMenuItem.setVisible(isShortcutCreatable());
            }
            final MenuItem helpMenu = menu.findItem(R.id.menu_help);
            helpMenu.setVisible(HelpUtils.isHelpAndFeedbackAvailable());
            //M:OP01 RCS will add quick contact menu item @{
            ExtensionManager.getInstance().getRcsExtension().
                    addQuickContactMenuOptions(menu, mLookupUri, this);
            /** @} */
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_star:
                toggleStar(item);
                return true;
            case R.id.menu_hotknot:
                HotKnotHandler.register(this,
                        mContactData != null ? mContactData.getLookupUri() : null);
                return true;
            case R.id.menu_edit:
                if (DirectoryContactUtil.isDirectoryContact(mContactData)) {
                    // This action is used to launch the contact selector, with the option of
                    // creating a new contact. Creating a new contact is an INSERT, while selecting
                    // an exisiting one is an edit. The fields in the edit screen will be
                    // prepopulated with data.

                    final Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                    intent.setType(Contacts.CONTENT_ITEM_TYPE);

                    ArrayList<ContentValues> values = mContactData.getContentValues();

                    // Only pre-fill the name field if the provided display name is an nickname
                    // or better (e.g. structured name, nickname)
                    if (mContactData.getDisplayNameSource() >= DisplayNameSources.NICKNAME) {
                        intent.putExtra(Intents.Insert.NAME, mContactData.getDisplayName());
                    } else if (mContactData.getDisplayNameSource()
                            == DisplayNameSources.ORGANIZATION) {
                        // This is probably an organization. Instead of copying the organization
                        // name into a name entry, copy it into the organization entry. This
                        // way we will still consider the contact an organization.
                        final ContentValues organization = new ContentValues();
                        organization.put(Organization.COMPANY, mContactData.getDisplayName());
                        organization.put(Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE);
                        values.add(organization);
                    }

                    // Last time used and times used are aggregated values from the usage stat
                    // table. They need to be removed from data values so the SQL table can insert
                    // properly
                    for (ContentValues value : values) {
                        value.remove(Data.LAST_TIME_USED);
                        value.remove(Data.TIMES_USED);
                    }
                    intent.putExtra(Intents.Insert.DATA, values);

                    // If the contact can only export to the same account, add it to the intent.
                    // Otherwise the ContactEditorFragment will show a dialog for selecting an
                    // account.
                    if (mContactData.getDirectoryExportSupport() ==
                            Directory.EXPORT_SUPPORT_SAME_ACCOUNT_ONLY) {
                        intent.putExtra(Intents.Insert.EXTRA_ACCOUNT,
                                new Account(mContactData.getDirectoryAccountName(),
                                        mContactData.getDirectoryAccountType()));
                        intent.putExtra(Intents.Insert.EXTRA_DATA_SET,
                                mContactData.getRawContacts().get(0).getDataSet());
                    }

                    // Add this flag to disable the delete menu option on directory contact joins
                    // with local contacts. The delete option is ambiguous when joining contacts.
                    intent.putExtra(ContactEditorFragment.INTENT_EXTRA_DISABLE_DELETE_MENU_OPTION,
                            true);

                    QuickContactUtils.addSipExtra(intent);
                    startActivityForResult(intent, REQUEST_CODE_CONTACT_SELECTION_ACTIVITY);
                } else if (InvisibleContactUtil.isInvisibleAndAddable(mContactData, this)) {
                    InvisibleContactUtil.addToDefaultGroup(mContactData, this);
                } else if (isContactEditable()) {
                    editContact();
                }
                return true;
            case R.id.menu_delete:
                if (isContactEditable()) {
                deleteContact();
                }
                return true;
            case R.id.menu_share:
                if (isContactShareable()) {
                    shareContact();
                }
                return true;
            case R.id.menu_create_contact_shortcut:
                if (isShortcutCreatable()) {
                createLauncherShortcutWithContact();
                }
                return true;
            case R.id.menu_help:
                HelpUtils.launchHelpAndFeedbackForContactScreen(this);
                return true;
            case MENU_ITEM_PRINT:
                return QuickContactUtils.printContact(this, mContactData);
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
