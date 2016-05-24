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
 * limitations under the License
 */

package com.mediatek.providers.contacts;

import static com.android.providers.contacts.util.DbQueryUtils.checkForSupportedColumns;
import android.content.Context;
import static com.android.providers.contacts.util.DbQueryUtils.getEqualityClause;
import static com.android.providers.contacts.util.DbQueryUtils.getInequalityClause;
import android.app.AppOpsManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.database.sqlite.SQLiteDiskIOException;
import android.net.Uri;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Process;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.CallLog.ConferenceCalls;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.ImsCall;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.Settings;
import android.text.TextUtils;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.app.SearchManager;

import com.android.providers.contacts.Constants;
import com.android.providers.contacts.ContactsDatabaseHelper;
import com.android.providers.contacts.ContactsDatabaseHelper.DataColumns;
import com.android.providers.contacts.DatabaseModifier;
import com.android.providers.contacts.DbModifierWithNotification;
import com.android.providers.contacts.NameNormalizer;
import com.android.providers.contacts.VoicemailPermissions;
import com.android.providers.contacts.ContactsDatabaseHelper.DbProperties;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.util.SelectionBuilder;
import com.android.providers.contacts.util.UserUtils;
import com.android.providers.contacts.ContactsDatabaseHelper.Views;
import com.android.providers.contacts.ContactsDatabaseHelper.PhoneLookupColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.SearchIndexColumns;
import com.mediatek.providers.contacts.DialerSearchSupport.DialerSearchLookupColumns;
import com.mediatek.providers.contacts.DialerSearchSupport.DialerSearchLookupType;
import com.mediatek.providers.contacts.LogUtils;
import com.mediatek.providers.contacts.ContactsProviderUtils;
import com.mediatek.providers.contacts.CallLogSearchSupport;
import com.android.providers.contacts.CallLogProvider;
import com.mediatek.providers.contacts.DialerSearchUtils;
import com.mediatek.providers.contacts.ConstantsUtils;
import com.mediatek.common.mom.SubPermissions;
import com.google.common.annotations.VisibleForTesting;
import java.util.HashMap;
import java.util.List;
import com.android.providers.contacts.ContactsProvider2;

/**
 * Call log content provider.
 */
public class CallLogProviderEx {
    private static final String TAG = CallLogProviderEx.class.getSimpleName();

    private final Context mContext;
    private ContactsDatabaseHelper mDbHelper;
    private DatabaseUtils.InsertHelper mCallsInserter;
    private static final int CALLS_SEARCH_FILTER = 4;
    private static final int CALLS_JION_DATA_VIEW = 5;
    private static final int CALLS_JION_DATA_VIEW_ID = 6;
    private static final int CONFERENCE_CALLS = 7;
    private static final int CONFERENCE_CALLS_ID = 8;
    private static final int SEARCH_SUGGESTIONS = 10001;
    private static final int SEARCH_SHORTCUT = 10002;
    private CallLogSearchSupport mCallLogSearchSupport;
    private CallLogProvider mCallLogProvider;

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {

        sURIMatcher.addURI(CallLog.AUTHORITY, "calls/search_filter/*", CALLS_SEARCH_FILTER);
        sURIMatcher.addURI(CallLog.AUTHORITY, "callsjoindataview", CALLS_JION_DATA_VIEW);
        sURIMatcher.addURI(CallLog.AUTHORITY, "callsjoindataview/#", CALLS_JION_DATA_VIEW_ID);
        sURIMatcher.addURI(CallLog.AUTHORITY,
                SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGESTIONS);
        sURIMatcher.addURI(CallLog.AUTHORITY,
                SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGESTIONS);
        sURIMatcher.addURI(CallLog.AUTHORITY,
                SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/*", SEARCH_SHORTCUT);
        sURIMatcher.addURI(CallLog.AUTHORITY, "conference_calls", CONFERENCE_CALLS);
        sURIMatcher.addURI(CallLog.AUTHORITY, "conference_calls/#", CONFERENCE_CALLS_ID);
    }

    private static final String sStableCallsJoinData = Tables.CALLS
            + " LEFT JOIN "
            + Tables.CONFERENCE_CALLS + " ON "
            + Calls.CONFERENCE_CALL_ID + "=" + Tables.CONFERENCE_CALLS + "." + ConferenceCalls._ID
            + " LEFT JOIN "
            + " (SELECT * FROM " +  Views.DATA + " WHERE " + Data._ID + " IN " + "(SELECT "
            +  Calls.DATA_ID + " FROM " + Tables.CALLS + ")) AS " + Views.DATA + " ON("
            + Tables.CALLS + "." + Calls.DATA_ID + " = " + Views.DATA + "." + Data._ID + ")";

    // Must match the definition in CallLogQuery - begin.
    private static final String CALL_NUMBER_TYPE = "calllognumbertype";
    private static final String CALL_NUMBER_TYPE_ID = "calllognumbertypeid";
    // Must match the definition in CallLogQuery - end.

    private static final HashMap<String, String> sCallsJoinDataViewProjectionMap;
    static {
        // Calls Join view_data projection map
        sCallsJoinDataViewProjectionMap = new HashMap<String, String>();
        sCallsJoinDataViewProjectionMap.put(Calls._ID, Tables.CALLS + "._id as " + Calls._ID);
        sCallsJoinDataViewProjectionMap.put(Calls.NUMBER, Calls.NUMBER);
        sCallsJoinDataViewProjectionMap.put(Calls.NUMBER_PRESENTATION, Calls.NUMBER_PRESENTATION);
        sCallsJoinDataViewProjectionMap.put(Calls.DATE, Calls.DATE);
        sCallsJoinDataViewProjectionMap.put(Calls.DURATION, Calls.DURATION);
        sCallsJoinDataViewProjectionMap.put(Calls.DATA_USAGE, Calls.DATA_USAGE);
        sCallsJoinDataViewProjectionMap.put(Calls.TYPE, Calls.TYPE);
        sCallsJoinDataViewProjectionMap.put(Calls.FEATURES, Calls.FEATURES);
        sCallsJoinDataViewProjectionMap.put(
                Calls.PHONE_ACCOUNT_COMPONENT_NAME, Calls.PHONE_ACCOUNT_COMPONENT_NAME);
        sCallsJoinDataViewProjectionMap.put(Calls.PHONE_ACCOUNT_ID, Calls.PHONE_ACCOUNT_ID);
        sCallsJoinDataViewProjectionMap.put(Calls.NEW, Calls.NEW);
        sCallsJoinDataViewProjectionMap.put(Calls.VOICEMAIL_URI, Calls.VOICEMAIL_URI);
        sCallsJoinDataViewProjectionMap.put(Calls.TRANSCRIPTION, Calls.TRANSCRIPTION);
        sCallsJoinDataViewProjectionMap.put(Calls.IS_READ, Calls.IS_READ);
        sCallsJoinDataViewProjectionMap.put(Calls.COUNTRY_ISO, Calls.COUNTRY_ISO);
        sCallsJoinDataViewProjectionMap.put(Calls.GEOCODED_LOCATION, Calls.GEOCODED_LOCATION);
        sCallsJoinDataViewProjectionMap.put(Calls.RAW_CONTACT_ID, Tables.CALLS + "."
                + Calls.RAW_CONTACT_ID + " AS " + Calls.RAW_CONTACT_ID);
        sCallsJoinDataViewProjectionMap.put(Calls.DATA_ID, Calls.DATA_ID);

        sCallsJoinDataViewProjectionMap.put(Contacts.DISPLAY_NAME,
                Views.DATA + "." + Contacts.DISPLAY_NAME + " AS " + Contacts.DISPLAY_NAME);
        sCallsJoinDataViewProjectionMap.put(CALL_NUMBER_TYPE_ID,
                Views.DATA + "." + Data.DATA2 + " AS " + CALL_NUMBER_TYPE_ID);
        sCallsJoinDataViewProjectionMap.put(CALL_NUMBER_TYPE,
                Views.DATA + "." + Data.DATA3 + " AS " + CALL_NUMBER_TYPE);
        sCallsJoinDataViewProjectionMap.put(Data.PHOTO_ID,
                Views.DATA + "." + Data.PHOTO_ID + " AS " + Data.PHOTO_ID);
        sCallsJoinDataViewProjectionMap.put(
                RawContacts.INDICATE_PHONE_SIM, RawContacts.INDICATE_PHONE_SIM);
        sCallsJoinDataViewProjectionMap.put(
                RawContacts.IS_SDN_CONTACT, RawContacts.IS_SDN_CONTACT);
        sCallsJoinDataViewProjectionMap.put(RawContacts.CONTACT_ID, RawContacts.CONTACT_ID);
        sCallsJoinDataViewProjectionMap.put(Contacts.LOOKUP_KEY, Views.DATA + "."
                + Contacts.LOOKUP_KEY + " AS " + Contacts.LOOKUP_KEY);
        sCallsJoinDataViewProjectionMap.put(
                Data.PHOTO_URI, Views.DATA + "." + Data.PHOTO_URI + " AS " + Data.PHOTO_URI);
        sCallsJoinDataViewProjectionMap.put(Calls.IP_PREFIX, Calls.IP_PREFIX);
        sCallsJoinDataViewProjectionMap.put(Calls.CONFERENCE_CALL_ID, Calls.CONFERENCE_CALL_ID);
        sCallsJoinDataViewProjectionMap.put(Calls.SORT_DATE, "(CASE WHEN " +
                Calls.CONFERENCE_CALL_ID + ">0 THEN " + ConferenceCalls.CONFERENCE_DATE
                + " ELSE " + Calls.DATE + " END) AS " + Calls.SORT_DATE);
    }

    private VoicemailPermissions mVoicemailPermissions;
    private DialerSearchSupport mDialerSearchSupport;
    private static CallLogProviderEx sCallLogProviderEx;

    private CallLogProviderEx(Context context) {
        mContext = context;
    }

    public static synchronized CallLogProviderEx getInstance(Context context) {
        if (sCallLogProviderEx == null) {
            sCallLogProviderEx = new CallLogProviderEx(context);
            sCallLogProviderEx.initialize();
        }
        return sCallLogProviderEx;
    }

    private void initialize() {
        mVoicemailPermissions = new VoicemailPermissions(mContext);
        mCallLogSearchSupport = new CallLogSearchSupport(mContext);
        mDbHelper = getDatabaseHelper(mContext);
        mDialerSearchSupport = DialerSearchSupport.getInstance(mContext);
    }

    public SQLiteQueryBuilder queryCallLog(Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder, int match,
            SQLiteQueryBuilder qb, SelectionBuilder selectionBuilder, Long parseCallId) {
        String groupBy = null;
        switch (match) {
        case CALLS_SEARCH_FILTER: {
            String query = uri.getPathSegments().get(2);
            String nomalizeName = NameNormalizer.normalize(query);
            final String SNIPPET_CONTACT_ID = "snippet_contact_id";
                String table = Tables.CALLS + " LEFT JOIN " + Tables.CONFERENCE_CALLS + " ON "
                        + Calls.CONFERENCE_CALL_ID + "=" + Tables.CONFERENCE_CALLS + "."
                        + ConferenceCalls._ID + " LEFT JOIN " + Views.DATA + " ON (" + Views.DATA
                        + "." + Data._ID + "=" + Tables.CALLS + "." + Calls.DATA_ID + ")"
                        + " LEFT JOIN (SELECT " + SearchIndexColumns.CONTACT_ID
                        + " AS "
                        + SNIPPET_CONTACT_ID
                        + " FROM "
                        + Tables.SEARCH_INDEX
                        // M: fix Cr:ALPS01790297,modify match to glob to ensure
                        // query call log contacts name in quickSearchBox right.
                        + " WHERE " + SearchIndexColumns.NAME + " GLOB '*" + nomalizeName + "*') "
                        + " ON (" + SNIPPET_CONTACT_ID + "=" + Views.DATA + "." + Data.CONTACT_ID
                        + ")";

            qb.setTables(table);
            qb.setProjectionMap(sCallsJoinDataViewProjectionMap);

            /// M: keep this logic same with CallogManager's logCall, just strip separators with
            /// phone number before query happen, don't normalize it totally.
            /// Otherwise, some non-separator char in calllog can't be searched
            /// like "*" "#", etc. @{
            String number = query;
            if (mCallLogSearchSupport.isPhoneNumber(query)) {
                number = PhoneNumberUtils.stripSeparators(query);
            }
            /// @}

            StringBuilder sb = new StringBuilder();
            sb.append(Tables.CALLS + "." + Calls.NUMBER + " GLOB '*");
            sb.append(number);
            sb.append("*' OR (" + SNIPPET_CONTACT_ID + ">0 AND " + Tables.CALLS + "."
                    + Calls.RAW_CONTACT_ID + ">0) ");
            qb.appendWhere(sb);
            groupBy = Tables.CALLS + "." + Calls._ID;

            LogUtils.d(TAG, " CallLogProvider.CALLS_SEARCH_FILTER, table="
                    + table + ", query=" + query + ", sb=" + sb.toString());
            break;
        }

        case CALLS_JION_DATA_VIEW: {
            qb.setTables(sStableCallsJoinData);
            qb.setProjectionMap(sCallsJoinDataViewProjectionMap);
            qb.setStrict(true);
            break;
        }

        case CALLS_JION_DATA_VIEW_ID: {
            qb.setTables(sStableCallsJoinData);
            qb.setProjectionMap(sCallsJoinDataViewProjectionMap);
            qb.setStrict(true);
            selectionBuilder.addClause(
                    getEqualityClause(Tables.CALLS + "." + Calls._ID, parseCallId));
            break;
        }

        case CONFERENCE_CALLS_ID: {
            LogUtils.d(TAG, "CallLogProvider.CONFERENCE_CALLS_ID. Uri:" + uri);
            qb.setTables(sStableCallsJoinData);
            qb.setProjectionMap(sCallsJoinDataViewProjectionMap);
            long confCallId = ContentUris.parseId(uri);
            qb.appendWhere(Calls.CONFERENCE_CALL_ID + "=" + confCallId);
            break;
        }

        }
        return qb;
    }

    public Cursor queryGlobalSearch(Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder, int match) {
        final SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor c = null;
        switch (match) {
        case SEARCH_SUGGESTIONS: {
            LogUtils.d(TAG, "CallLogProvider.SEARCH_SUGGESTIONS");
            c = mCallLogSearchSupport.handleSearchSuggestionsQuery(db, uri, getLimit(uri));
            break;
        }

        case SEARCH_SHORTCUT: {
            LogUtils.d(TAG, "CallLogProvider.SEARCH_SHORTCUT. Uri:" + uri);
            String callId = uri.getLastPathSegment();
            String filter = uri.getQueryParameter(SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA);
            c = mCallLogSearchSupport.handleSearchShortcutRefresh(db, projection, callId, filter);
            break;
        }
        }
        return c;
    }

    public Uri insertConferenceCall(Uri uri, ContentValues values) {
        if (CONFERENCE_CALLS == sURIMatcher.match(uri)) {
            final SQLiteDatabase db = mDbHelper.getWritableDatabase();
            final long confCallId = db.insert(
                    Tables.CONFERENCE_CALLS, ConferenceCalls.GROUP_ID, values);

            if (confCallId < 0) {
                LogUtils.w(TAG, "Insert Conference Call Failed, Uri:" + uri);
                return null;
            }
            return ContentUris.withAppendedId(uri, confCallId);
        }

        return null;
    }

    public Uri insert(Uri uri, ContentValues values) {
        LogUtils.d(TAG, "[insert]uri: " + uri);
        SQLiteDatabase db = null;
        try {
            db = mDbHelper.getWritableDatabase();
        } catch (SQLiteDiskIOException err) {
            err.printStackTrace();
            LogUtils.d(TAG, "insert()- 1 SQLiteDiskIOException");
            return null;
        }

        String strInsNumber = values.getAsString(Calls.NUMBER);
        LogUtils.d(TAG, "[insert] get default insert number:" + strInsNumber);

        if (mCallsInserter == null) {
            mCallsInserter = new DatabaseUtils.InsertHelper(db, Tables.CALLS);
        }

        try {
            db.beginTransaction();
            appendComputedRawContactForCalls(db, strInsNumber, values);
            LogUtils.d(TAG, "insert into calls table");
            long rowId = getDatabaseModifier(mCallsInserter).insert(values);
            LogUtils.d(TAG, "insert into calls table, rowId:" + rowId);

            if (rowId > 0 && ContactsProviderUtils.isSearchDbSupport()) {
                mDialerSearchSupport.handleCallLogsInserted(db, rowId, strInsNumber);
            }

            if (rowId > 0) {
                uri = ContentUris.withAppendedId(uri, rowId);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return uri;
    }

    public int delete(Uri uri, String selection,
            String[] selectionArgs, SelectionBuilder selectionBuilder) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int count = 0;

            try {
                db.beginTransaction();
                count = getDatabaseModifier(db).delete(Tables.CALLS,
                        selectionBuilder.build(), selectionArgs);

                if (count > 0 && ContactsProviderUtils.isSearchDbSupport()) {
                    mDialerSearchSupport.handleCallLogsDeleted(db);
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            LogUtils.d(TAG, "[delete] delete Calls. count: " + count);
            return count;
        }

    private static final boolean DBG_DIALER_SEARCH = ContactsProviderUtils.DBG_DIALER_SEARCH;

    private void notifyDialerSearchChange() {
        mContext.getContentResolver().notifyChange(
                ContactsContract.AUTHORITY_URI.buildUpon().appendPath("dialer_search")
                        .appendPath("call_log").build(), null, false);
    }

    // send new Calls broadcast to launcher to update unread icon
    public static final void notifyNewCallsCount(Context context) {
        SQLiteDatabase db = null;
        Cursor c = null;
        int newCallsCount = 0;
        try {
            db = getDatabaseHelper(context).getReadableDatabase();

            if (db == null || context == null) {
                LogUtils.w(TAG, "[notifyNewCallsCount] Cannot notify with null db or context.");
                return;
            }

            c = db.rawQuery("SELECT count(*) FROM " + Tables.CALLS
                    + " WHERE " + Calls.TYPE + " in (" + Calls.MISSED_TYPE
                    + "," + Calls.VOICEMAIL_TYPE
                    + ") AND " + Calls.NEW + "=1", null);

            if (c != null && c.moveToFirst()) {
                newCallsCount = c.getInt(0);
            }
        } catch (SQLiteException e) {
            LogUtils.w(TAG, "[notifyNewCallsCount] SQLiteException:" + e);
            return;
        } finally {
            if (c != null) {
                c.close();
            }
        }

        LogUtils.i(TAG, "[notifyNewCallsCount] newCallsCount = " + newCallsCount);
        //send count=0 to clear the unread icon
        if (newCallsCount >= 0) {
            Intent newIntent = new Intent(Intent.ACTION_UNREAD_CHANGED);
            newIntent.putExtra(Intent.EXTRA_UNREAD_NUMBER, newCallsCount);
            newIntent.putExtra(Intent.EXTRA_UNREAD_COMPONENT,
                    new ComponentName(ConstantsUtils.CONTACTS_PACKAGE,
                    ConstantsUtils.CONTACTS_DIALTACTS_ACTIVITY));
            context.sendBroadcast(newIntent);
            // use the public key CONTACTS_UNREAD_KEY that statement in Setting Provider.
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.CONTACTS_UNREAD_KEY, Integer.valueOf(newCallsCount));
        }
    }

    protected static ContactsDatabaseHelper getDatabaseHelper(final Context context) {
        return ContactsDatabaseHelper.getInstance(context);
    }

    /**
     * Returns a {@link DatabaseModifier} that takes care of sending necessary notifications
     * after the operation is performed.
     */
    private DatabaseModifier getDatabaseModifier(SQLiteDatabase db) {
        return new DbModifierWithNotification(Tables.CALLS, db, mContext);
    }

    /**
     * Same as {@link #getDatabaseModifier(SQLiteDatabase)} but used for insert helper operations
     * only.
     */
    private DatabaseModifier getDatabaseModifier(DatabaseUtils.InsertHelper insertHelper) {
        return new DbModifierWithNotification(Tables.CALLS, insertHelper, mContext);
    }

    /**
     * Gets the value of the "limit" URI query parameter.
     *
     * @return A string containing a non-negative integer, or <code>null</code> if
     *         the parameter is not set, or is set to an invalid value.
     */
    public String getLimit(Uri uri) {
        String limitParam = uri.getQueryParameter("limit");
        if (limitParam == null) {
            return null;
        }
        // make sure that the limit is a non-negative integer
        try {
            int l = Integer.parseInt(limitParam);
            if (l < 0) {
                Log.w(TAG, "Invalid limit parameter: " + limitParam);
                return null;
            }
            return String.valueOf(l);
        } catch (NumberFormatException ex) {
            Log.w(TAG, "Invalid limit parameter: " + limitParam);
            return null;
        }
    }

    /**
     * This method should be called to sync info when contacts Updated.
     *
     * @param db the writable database
     * @param rawContactId raw contact id of the inserted data
     * @param dataId inserted contact's dataId
     * @param dataValue name/phone/sip/ims
     * @param mimeType mimeType of name/phone/sip/ims
     */
    public void handleContactsDataInserted (SQLiteDatabase db,
            long rawContactId, long dataId, String dataValue, String mimeType) {
        if (TextUtils.isEmpty(mimeType) || TextUtils.isEmpty(dataValue)) {
            LogUtils.w(TAG, "Cannot update calls with empty mimeType or Value");
            return;
        }

        try {
            String normalizedCallable = dataValue;

            switch (mimeType) {
            case Phone.CONTENT_ITEM_TYPE:
                // Don't normalize Uri phone number
                if (!PhoneNumberUtils.isUriNumber(dataValue)) {
                    normalizedCallable = DialerSearchUtils.stripSpecialCharInNumber(dataValue);
                }
                // fall-through
            case SipAddress.CONTENT_ITEM_TYPE:
            case ImsCall.CONTENT_ITEM_TYPE:
                int count = updateCallsWithNewCallable(db,
                        rawContactId, dataId, normalizedCallable);
                if (count > 0) {
                    mDialerSearchSupport.handleCallLogsUpdated(db, false);
                }
                break;

            default:
                return;
            }
        } catch (SQLiteException e) {
            LogUtils.e(TAG, "handleContactsDataInserted: " + e);
        }
    }

    /**
     * This method should be called to sync info when contacts Updated.
     *
     * @param db the writable database
     * @param rawContactId raw contact id of the inserted data
     * @param dataId inserted contact's dataId
     * @param dataValue name/phone/sip/ims
     * @param mimeType mimeType of name/phone/sip/ims
     */
    public void handleContactsDataUpdated (SQLiteDatabase db,
            long rawContactId, long dataId, String dataValue, String mimeType) {
        if (TextUtils.isEmpty(mimeType) || TextUtils.isEmpty(dataValue)) {
            LogUtils.w(TAG, "Cannot update calls with empty mimeType or Value");
            return;
        }

        try {
            String normalizedCallable = dataValue;

            switch (mimeType) {
            case Phone.CONTENT_ITEM_TYPE:
                // Don't normalize Uri phone number
                if (!PhoneNumberUtils.isUriNumber(dataValue)) {
                    normalizedCallable = DialerSearchUtils.stripSpecialCharInNumber(dataValue);
                }
                // fall-through
            case SipAddress.CONTENT_ITEM_TYPE:
            case ImsCall.CONTENT_ITEM_TYPE:
                //update treat as delete+insert
                int cntDelete = updateOldDataIdCalls(db, dataId);
                int cntInsert = updateCallsWithNewCallable(db,
                        rawContactId, dataId, normalizedCallable);
                //cntDelete <= 0, no call number is same as the old(deleted) callable value
                if (cntDelete > 0 || cntInsert > 0) {
                    mDialerSearchSupport.handleCallLogsUpdated(db, (cntDelete > 0 ? true : false));
                }
                break;

            default:
                return;
            }
        } catch (SQLiteException e) {
            LogUtils.e(TAG, "handleContactsDataUpdated: " + e);
        }
    }

    /**
     * This method should be called to sync info when contacts deleted.
     *
     * @param db the writable database
     */
    public void handleContactsDataDeleted (SQLiteDatabase db) {
        try {
            if (updateCallsWhenContactDeleted(db) > 0) {;
                mDialerSearchSupport.handleCallLogsUpdated(db, true);
            }
        } catch (SQLiteException e) {
            LogUtils.e(TAG, "handleContactsDataDeleted: " + e);
        }
    }

    /**
     *
     * @param db the writable database
     * @param callable the inserting callable value
     * @param values the ContentValues which need appending dataId and rawContactId
     * @return whether have contacts with same callable
     */
    public boolean appendComputedRawContactForCalls(SQLiteDatabase db,
            String callable, ContentValues values) {
        boolean bIsUriNumber = PhoneNumberUtils.isUriNumber(callable);
        boolean hasSameContacts = false;

        Cursor nameCursor = null;

        if (bIsUriNumber) {
            nameCursor = db.query(Views.DATA, new String[] { Data._ID, Data.RAW_CONTACT_ID },
                    buildDataSameNumberFilter(callable), null, null, null, null);
        } else {
            String normalizedNumber = DialerSearchUtils.stripSpecialCharInNumber(callable);
            /*
             * Use key "lookup" to get right data_id and raw_contact_id. The
             * former one which uses "normalizedNumber" to search phone_lookup
             * table would cause to get the dirty data.
             *
             * The previous code is: nameCursor =
             * getContext().getContentResolver().query(
             * Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
             * Uri.encode(strInsNumber)), new String[]
             * {PhoneLookupColumns.DATA_ID, PhoneLookupColumns.RAW_CONTACT_ID},
             * null, null, null);
             */
            nameCursor = DialerSearchUtils.queryPhoneLookupByNumber(db, mDbHelper, callable,
                    new String[] { PhoneLookupColumns.DATA_ID,
                    PhoneLookupColumns.RAW_CONTACT_ID, Phone.NUMBER },
                    null, null, null, null, null, null);
        }

        if (nameCursor == null) {
            return hasSameContacts;
        }

        try {
            if (nameCursor.moveToFirst()) {
                long dataId = nameCursor.getLong(0);
                long rawContactId = nameCursor.getLong(1);
                values.put(Calls.DATA_ID, dataId);
                values.put(Calls.RAW_CONTACT_ID, rawContactId);
                hasSameContacts = true;
            }
        } finally {
            nameCursor.close();
            return hasSameContacts;
        }
    }

    public static String buildLatestMatchedCallLogIdQuery(String callable) {
        StringBuilder queryBuilder = new StringBuilder();

        // Get the latest call log id for the same number
        queryBuilder.append("SELECT (CASE WHEN COUNT(" + Calls._ID + ")>0 THEN "
                + Calls._ID + " ELSE 0 END ) FROM " + Tables.CALLS + " WHERE ");
        queryBuilder.append(buildCallLogSameNumberFilter(callable));
        queryBuilder.append(" ORDER BY " + Calls.DATE + " DESC LIMIT 1");

        return queryBuilder.toString();
    }

    private int updateCallsWithNewCallable(SQLiteDatabase db,
            long rawContactId, long dataId, String callable) {
        SQLiteStatement updateCallsWithNewCallable = db.compileStatement(
                " UPDATE " + Tables.CALLS
                + " SET " + Calls.RAW_CONTACT_ID + "=?, "
                + Calls.DATA_ID + "=? "
                + " WHERE " + buildCallLogSameNumberFilter(callable)
                );

        updateCallsWithNewCallable.bindLong(1, rawContactId);
        updateCallsWithNewCallable.bindLong(2, dataId);
        int count = updateCallsWithNewCallable.executeUpdateDelete();
        updateCallsWithNewCallable.close();
        Log.d(TAG, "updateCallsWithNewCallable:" + updateCallsWithNewCallable);
        return count;
    }

    private int updateCallsWhenContactDeleted(SQLiteDatabase db) {
        // Change for ALPS02260458, add check whether Data info exist. If user change the contact
        // number, the old data info will be deleted but the raw contact is still exist, need to
        // check the data info exist to decide whether delete the related dialer search data.
        String where = Calls.RAW_CONTACT_ID + ">0" + " AND " + "(" + Calls.RAW_CONTACT_ID
                + " NOT IN " + " (SELECT " + RawContacts._ID + " FROM " + Tables.RAW_CONTACTS
                + " WHERE " + RawContacts.DELETED + "=0)" + " OR " + Calls.DATA_ID + " NOT IN "
                + " (SELECT " + Data._ID + " FROM " + Tables.DATA + ")" + ")";
        return updateOutdatedDataIdCalls(db, where);
    }

    private int updateOldDataIdCalls(SQLiteDatabase db, long dataId) {
        String where = Calls.DATA_ID + "=" + dataId;
        return updateOutdatedDataIdCalls(db, where);
    }

    private int updateOutdatedDataIdCalls(SQLiteDatabase db, String where) {
        SQLiteStatement updateCallsWithOldDataId = db.compileStatement(" UPDATE " + Tables.CALLS
                + " SET " + Calls.DATA_ID + "=(SELECT " + Data._ID + " FROM " + Tables.DATA
                + " WHERE " + Data.RAW_CONTACT_ID + " IN " + " (SELECT " + RawContacts._ID
                + " FROM " + Tables.RAW_CONTACTS + " WHERE " + RawContacts.DELETED + "=0)"
                + " AND ((" + mDbHelper.getMimeTypeId(Phone.CONTENT_ITEM_TYPE) + "="
                + DataColumns.CONCRETE_MIMETYPE_ID + " AND PHONE_NUMBERS_EQUAL(" + Data.DATA1
                + ", " + Tables.CALLS + "." + Calls.NUMBER + ")" + " ) OR ("
                + DataColumns.CONCRETE_MIMETYPE_ID + " IN " + getUriCallableMimetypeIdsString()
                + " AND " + Data.DATA1 + "=" + Tables.CALLS + "." + Calls.NUMBER + "))"
                + " LIMIT 1), " + Calls.RAW_CONTACT_ID + "=0 " + " WHERE " + where);

        int count = updateCallsWithOldDataId.executeUpdateDelete();
        updateCallsWithOldDataId.close();

        if (count > 0) {
            SQLiteStatement updateRawContactId = db.compileStatement(" UPDATE " + Tables.CALLS
                    + " SET " + Calls.RAW_CONTACT_ID + "=(SELECT " + Data.RAW_CONTACT_ID + " FROM "
                    + Tables.DATA + " WHERE " + Data._ID + " = " + Tables.CALLS + "."
                    + Calls.DATA_ID + ")" + " WHERE " + Calls.DATA_ID + ">0 AND "
                    + Calls.RAW_CONTACT_ID + "=0");

            updateRawContactId.execute();
            updateRawContactId.close();
        }
        return count;
    }

    private String getUriCallableMimetypeIdsString() {
        StringBuilder builder = new StringBuilder("(");
        builder.append(mDbHelper.getMimeTypeId(ImsCall.CONTENT_ITEM_TYPE));
        builder.append(",");
        builder.append(mDbHelper.getMimeTypeId(SipAddress.CONTENT_ITEM_TYPE));
        builder.append(")");
        return builder.toString();
    }
    private static String buildCallLogSameNumberFilter(String callable) {
        boolean bIsUriNumber = PhoneNumberUtils.isUriNumber(callable);
        String filter = null;
        if (bIsUriNumber) {
            filter = Calls.NUMBER + "='" + callable + "'";
        } else {
            filter = "PHONE_NUMBERS_EQUAL(" + Calls.NUMBER + ", '" + callable + "')";
        }
        return filter;
    }

    private static String buildDataSameNumberFilter(String callable) {
        boolean bIsUriNumber = PhoneNumberUtils.isUriNumber(callable);
        StringBuilder filter = new StringBuilder();
        filter.append(Data.MIMETYPE + " IN ('" + SipAddress.CONTENT_ITEM_TYPE + "', '"
                + ImsCall.CONTENT_ITEM_TYPE + "', '" + Phone.CONTENT_ITEM_TYPE + "') AND ");
        if (bIsUriNumber) {
            filter.append(Data.DATA1 + "='" + callable + "'");
        } else {
            filter.append("PHONE_NUMBERS_EQUAL(" + Data.DATA1 + ", '" + callable + "')");
        }
        return filter.toString();
    }
}
