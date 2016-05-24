package com.android.providers.telephony;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.net.Uri.Builder;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.provider.BaseColumns;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.Telephony;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.os.Binder;
import android.os.SystemProperties;
import android.os.UserHandle;

import com.google.android.mms.pdu.PduHeaders;

public class RCSProvider extends ContentProvider {

    private static final String TAG = "Mms/Provider/Rcs";

    public static final String TABLE_MESSAGE = "rcs_message";
    public static final String TABLE_CONVERSATIONS = "rcs_conversations";
    private static final String TABLE_WORDS = "words";
    public static final String VIEW_RCS_THREADS = "rcs_threads";

    private static final boolean MTK_RCS_SUPPORT =
                    "1".equals(SystemProperties.get("ro.mtk_op01_rcs"));
    public static final int RCS_TABLE_TO_USE = 5;

    public static final String COLUMN_ID               = "_id";

    public static final String MESSAGE_COLUMN_DATE_SENT        = "date_sent";
    public static final String MESSAGE_COLUMN_SEEN             = "seen";
    public static final String MESSAGE_COLUMN_LOCKED           = "locked";
    public static final String MESSAGE_COLUMN_SUB_ID           = "sub_id";
    public static final String MESSAGE_COLUMN_IPMSG_ID         = "ipmsg_id";
    public static final String MESSAGE_COLUMN_CLASS            = "class";
    public static final String MESSAGE_COLUMN_FILE_PATH        = "file_path";
    public static final String MESSAGE_COLUMN_MESSAGE_ID       = "CHATMESSAGE_MESSAGE_ID";
    public static final String MESSAGE_COLUMN_CHAT_ID          = "CHATMESSAGE_CHAT_ID";
    public static final String MESSAGE_COLUMN_CONTACT_NUMBER   = "CHATMESSAGE_CONTACT_NUMBER";
    public static final String MESSAGE_COLUMN_BODY             = "CHATMESSAGE_BODY";
    public static final String MESSAGE_COLUMN_TIMESTAMP        = "CHATMESSAGE_TIMESTAMP";
    public static final String MESSAGE_COLUMN_MESSAGE_STATUS   = "CHATMESSAGE_MESSAGE_STATUS";
    public static final String MESSAGE_COLUMN_TYPE             = "CHATMESSAGE_TYPE";
    public static final String MESSAGE_COLUMN_DIRECTION        = "CHATMESSAGE_DIRECTION";
    public static final String MESSAGE_COLUMN_FLAG             = "CHATMESSAGE_FLAG";
    public static final String MESSAGE_COLUMN_ISBLOCKED        = "CHATMESSAGE_ISBLOCKED";
    public static final String MESSAGE_COLUMN_CONVERSATION     = "CHATMESSAGE_CONVERSATION";
    public static final String MESSAGE_COLUMN_MIME_TYPE        = "CHATMESSAGE_MIME_TYPE";

    public static final String CONVERSATION_COLUMN_READ          = "read";
    public static final String CONVERSATION_COLUMN_ERROR         = "error";
    public static final String CONVERSATION_COLUMN_ATTACHMENT    = "has_attachment";
    public static final String CONVERSATION_COLUMN_CLASS         = MESSAGE_COLUMN_CLASS;
    public static final String CONVERSATION_COLUMN_CONVERSATION  = MESSAGE_COLUMN_CONVERSATION;
    public static final String CONVERSATION_COLUMN_RECIPIENTS    = "CHATMESSAGE_RECIPIENTS";
    public static final String CONVERSATION_COLUMN_BODY          = MESSAGE_COLUMN_BODY;
    public static final String CONVERSATION_COLUMN_TIMESTAMP     = MESSAGE_COLUMN_TIMESTAMP;
    public static final String CONVERSATION_COLUMN_FLAG          = MESSAGE_COLUMN_FLAG;
    public static final String CONVERSATION_COLUMN_TYPE          = MESSAGE_COLUMN_TYPE;
    public static final String CONVERSATION_COLUMN_MESSAGE_COUNT = "CHATMESSAGE_MESSAGE_COUNT";
    public static final String CONVERSATION_COLUMN_UNREAD_COUNT  = "CHATMESSAGE_UNREAD_COUNT";
    public static final String CONVERSATION_COLUMN_MIME_TYPE     = MESSAGE_COLUMN_MIME_TYPE;

    public static final String DAPI_CONVERSATION_BODY            = "DAPI_CONVERSATION_BODY";
    public static final String DAPI_CONVERSATION_TIMESTAMP       = "DAPI_CONVERSATION_TIMESTAMP";
    public static final String DAPI_CONVERSATION_TYPE            = "DAPI_CONVERSATION_TYPE";
    public static final String DAPI_CONVERSATION_UNREAD_COUNT    = "DAPI_CONVERSATION_UNREAD_COUNT";
    public static final String DAPI_CONVERSATION_MESSAGE_COUNT   = "DAPI_CONVERSATION_MSG_COUNT";
    public static final String DAPI_CONVERSATION_MIMETYPE        = "DAPI_CONVERSATION_MIMETYPE";

    public static final String THREADS_COLUMN_ID               = "_id";
    public static final String THREADS_COLUMN_SNIPPET          = "snippet";
    public static final String THREADS_COLUMN_SNIPPET_CS       = "snippet_cs";
    public static final String THREADS_COLUMN_TYPE             = "type";
    public static final String THREADS_COLUMN_DATE             = "date";
    public static final String THREADS_COLUMN_READCOUNT        = "readcount";
    public static final String THREADS_COLUMN_MESSAGE_COUNT    = "message_count";
    public static final String THREADS_COLUMN_ERROR            = "error";
    public static final String THREADS_COLUMN_READ             = "read";
    public static final String THREADS_COLUMN_HAS_ATTACHMENT   = "has_attachment";
    public static final String THREADS_COLUMN_STATUS           = "status";
    public static final String THREADS_COLUMN_RECIPIENT_IDS    = "recipient_ids";
    public static final String THREADS_COLUMN_ARCHIVED         = "archived";
    public static final String THREADS_COLUMN_CLASS            = "class";
    public static final String THREADS_COLUMN_RECIPIENTS       = "CHATMESSAGE_RECIPIENTS";
    public static final String THREADS_COLUMN_FLAG             = "CHATMESSAGE_FLAG";
    public static final String THREADS_COLUMN_MESSAGE_TYPE     = "CHATMESSAGE_TYPE";
    public static final String THREADS_COLUMN_MIME_TYPE        = "CHATMESSAGE_MIME_TYPE";
    /**
     * class value
     */
    public static final int CLASS_NORMAL     = 0;
    public static final int CLASS_BURN       = 1;
    public static final int CLASS_EMOTICON   = 2;
    public static final int CLASS_CLOUD      = 3;
    public static final int CLASS_SYSTEM     = 11;
    public static final int CLASS_INVITATION = 12;

    /**
     * CHATMESSAGE_MESSAGE_STATUS values in table rcs_message
     */
    public static final int MESSAGE_STATUS_UNREAD           = 0;
    public static final int MESSAGE_STATUS_READ             = 2;
    public static final int MESSAGE_STATUS_SENDING          = 3;
    public static final int MESSAGE_STATUS_SENT             = 4;
    public static final int MESSAGE_STATUS_FAILED           = 5;
    public static final int MESSAGE_STATUS_TO_SEND          = 6;
    public static final int MESSAGE_STATUS_DELIVERED        = 7;

    /**
     * CHATMESSAGE_TYPE values
     */
    public static final int TYPE_SMSMMS     = 0;
    public static final int TYPE_IM         = 1;
    public static final int TYPE_FT         = 2;
    public static final int TYPE_XML        = 3;

    /**
     * CHATMESSAGE_FLAG values
     */
    public static final int FLAG_OTO        = 1;
    public static final int FLAG_OTM        = 2;
    public static final int FLAG_MTM        = 3;
    public static final int FLAG_OFFICIAL   = 4;

    /**
     * CHATMESSAGE_DIRECTION values in table rcs_message
     */
    public static final int DIRECTION_INCOMING      = 0;
    public static final int DIRECTION_OUTGOING      = 1;

    private static final int URI_MESSAGE                = 1;
    private static final int URI_MESSAGE_ID             = 2;
    private static final int URI_THREADS                = 3;
    private static final int URI_THREAD_MESSAGE         = 4;
    private static final int URI_DAPI_MESSAGE           = 5;
    private static final int URI_DAPI_MESSAGE_ID        = 6;
    private static final int URI_FIRST_LOCKED_MESSAGE   = 7;
    private static final int URI_THREAD_SETTINGS        = 8;
    private static final int URI_UNDELIVERED_MSG        = 9;
    private static final int URI_SEARCH_SUGGEST         = 10;
    private static final int URI_SEARCH                 = 11;
    private static final int URI_THREAD_STATUS          = 12;

    private static final String DAPI_AUTHORITY_MESSAGE = "com.cmcc.ccs.message";
    private static final Uri CONTENT_URI_DAPI_MESSAGE = Uri.parse("content://com.cmcc.ccs.message");

    private static final String AUTHORITY_MESSAGE = "rcs";
    private static final Uri CONTENT_URI_MESSAGE = Uri.parse("content://rcs");

    private static final String AUTHORITY_UNION = "mms-sms-rcs";
    public static final Uri CONTENT_URI_UNION = Uri.parse("content://mms-sms-rcs");


    private static final Uri RCS_MESSAGE_URI =
            Uri.parse("content://org.gsma.joyn.provider.chat/message");
    private static final Uri RCS_FT_URI = Uri.parse("content://org.gsma.joyn.provider.ft/ft");

    private static final String[] RCS_MSG_COLUMNS = {
        COLUMN_ID,
        MESSAGE_COLUMN_DATE_SENT,
        MESSAGE_COLUMN_SEEN,
        MESSAGE_COLUMN_LOCKED,
        MESSAGE_COLUMN_SUB_ID,
        MESSAGE_COLUMN_IPMSG_ID,
        MESSAGE_COLUMN_CLASS,
        MESSAGE_COLUMN_FILE_PATH,
        MESSAGE_COLUMN_MESSAGE_ID,
        MESSAGE_COLUMN_CHAT_ID,
        MESSAGE_COLUMN_CONTACT_NUMBER,
        MESSAGE_COLUMN_BODY,
        MESSAGE_COLUMN_TIMESTAMP,
        MESSAGE_COLUMN_MESSAGE_STATUS,
        MESSAGE_COLUMN_TYPE,
        MESSAGE_COLUMN_DIRECTION,
        MESSAGE_COLUMN_FLAG,
        MESSAGE_COLUMN_ISBLOCKED,
        MESSAGE_COLUMN_CONVERSATION,
        MESSAGE_COLUMN_MIME_TYPE};

    private static final Set<String> RCS_COLUMNS = new HashSet<String>();

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY_MESSAGE, null, URI_MESSAGE);
        uriMatcher.addURI(AUTHORITY_MESSAGE, "#", URI_MESSAGE_ID);
        uriMatcher.addURI(AUTHORITY_UNION, "conversations", URI_THREADS);
        uriMatcher.addURI(AUTHORITY_UNION, "conversations/#", URI_THREAD_MESSAGE);
        uriMatcher.addURI(DAPI_AUTHORITY_MESSAGE, null, URI_DAPI_MESSAGE);
        uriMatcher.addURI(DAPI_AUTHORITY_MESSAGE, "#", URI_DAPI_MESSAGE_ID);
        uriMatcher.addURI(AUTHORITY_UNION, "locked", URI_FIRST_LOCKED_MESSAGE);
        uriMatcher.addURI(AUTHORITY_UNION, "setting/#", URI_THREAD_SETTINGS);
        uriMatcher.addURI(AUTHORITY_UNION, "undelivered", URI_UNDELIVERED_MSG);
        uriMatcher.addURI(AUTHORITY_UNION, "searchSuggest", URI_SEARCH_SUGGEST);
        uriMatcher.addURI(AUTHORITY_UNION, "search", URI_SEARCH);
        uriMatcher.addURI(AUTHORITY_UNION, "conversations/status", URI_THREAD_STATUS);

        int rcsOnlyColumnCount = RCS_MSG_COLUMNS.length;
        for (int i = 0; i < rcsOnlyColumnCount; i++) {
            RCS_COLUMNS.add(RCS_MSG_COLUMNS[i]);
        }
    }

    private SQLiteOpenHelper mOpenHelper;

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        Log.d(TAG, "delete start, uri=" + uri + ", where=" + where + ", whereArgs=" + whereArgs);
        if (!MTK_RCS_SUPPORT) {
            return 0;
        }
        int match = uriMatcher.match(uri);
        int count = 0;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String[] columns = {MESSAGE_COLUMN_CONVERSATION};
        Cursor cursor = db.query(TABLE_MESSAGE, columns, where, whereArgs,
                MESSAGE_COLUMN_CONVERSATION, null, null);
        long[] deletedThreads = null;
        try {
            deletedThreads = new long[cursor.getCount()];
            int i = 0;
            while (cursor.moveToNext()) {
                deletedThreads[i++] = cursor.getLong(0);
            }
        } finally {
            cursor.close();
        }
        switch(match) {
            case URI_MESSAGE: {
                count = deleteMessages(getContext(), db, where, whereArgs);
                break;
            }
            case URI_MESSAGE_ID: {
                where = concatSelections("_id=" + uri.getLastPathSegment(), where);
                count = deleteMessages(getContext(), db, where, whereArgs);
                break;
            }
            default:
                throw new UnsupportedOperationException("Not Support URI " + uri);
        }
        Log.d(TAG, "delete end, count = " + count);
        if (count > 0) {
            MmsSmsDatabaseHelper.updateMultiThreads(db, deletedThreads);
            notifyChange();
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(TAG, "insert start, uri=" + uri + ", values=" + values);
        if (!MTK_RCS_SUPPORT) {
            return null;
        }
        int match = uriMatcher.match(uri);
        long id = 0;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch(match) {
            case URI_MESSAGE:
            case URI_MESSAGE_ID:
                if (!values.containsKey(MESSAGE_COLUMN_CONVERSATION)) {
                    Log.d(TAG, "insert thread_id is null");
                    return null;
                }
                id = db.insert(TABLE_MESSAGE, null, values);
                uri = ContentUris.withAppendedId(CONTENT_URI_MESSAGE, id);
                int msgType = values.getAsInteger(MESSAGE_COLUMN_CLASS);
                int type = values.getAsInteger(MESSAGE_COLUMN_TYPE);
                if (msgType == CLASS_NORMAL && type == TYPE_IM) {
                    Log.d(TAG, "insert TABLE_WORDS begin");
                    ContentValues cv = new ContentValues();
                    cv.put(Telephony.MmsSms.WordsTable.ID, (2 << 16) + id);
                    cv.put(MmsSms.WordsTable.INDEXED_TEXT, values.getAsString(MESSAGE_COLUMN_BODY));
                    cv.put(MmsSms.WordsTable.SOURCE_ROW_ID, id);
                    cv.put(MmsSms.WordsTable.TABLE_ID, RCS_TABLE_TO_USE);
                    db.insert(TABLE_WORDS, MmsSms.WordsTable.INDEXED_TEXT, cv);
                    Log.d(TAG, "insert TABLE_WORDS end");
                }
                break;
            default:
                throw new UnsupportedOperationException("Not Support URI " + uri);
        }
        Log.d(TAG, "insert end, uri=" + uri);
        if (id > 0) {
            notifyChange();
        }
        return uri;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = MmsSmsDatabaseHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "query start, uri=" + uri);
        if (!MTK_RCS_SUPPORT) {
            return null;
        }

        final boolean accessRestricted = ProviderUtil.isAccessRestricted(
                getContext(), getCallingPackage(), Binder.getCallingUid());
        final String pduTable = MmsProvider.getPduTable(accessRestricted);
        final String smsTable = SmsProvider.getSmsTable(accessRestricted);

        int match = uriMatcher.match(uri);
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        String specialSelection = concatSelections(selection,
                MESSAGE_COLUMN_CLASS + " = " + CLASS_NORMAL);
        Cursor cursor = null;
        String selectionId = null;
        switch(match) {
            case URI_THREADS:
                cursor = getRCSSimpleConversations(
                        projection, selection, selectionArgs, sortOrder);
                notifyUnreadMessageNumberChanged(getContext());
                break;
            case URI_THREAD_MESSAGE:
                cursor = getRCSConversationMessages(uri, projection,
                        selection, sortOrder, smsTable, pduTable);
                break;
            case URI_FIRST_LOCKED_MESSAGE:
                cursor = getRCSFirstLockedMessage(projection, selection, selectionArgs, sortOrder);
                break;
            case URI_UNDELIVERED_MSG:
                Boolean includeNonPermanent = uri.getBooleanQueryParameter("includeNonPermanent",
                        true);
                cursor = getRCSUndeliveredMessages(projection, selection,
                        selectionArgs, sortOrder, smsTable, pduTable, includeNonPermanent);
                break;
            case URI_DAPI_MESSAGE:
                cursor = db.query(TABLE_MESSAGE, projection, specialSelection, selectionArgs,
                        null, null, sortOrder);
                break;
            case URI_DAPI_MESSAGE_ID:
                selectionId = "_id=" + Long.valueOf(uri.getLastPathSegment());
                specialSelection = concatSelections(specialSelection, selectionId);
                cursor = db.query(TABLE_MESSAGE, projection, specialSelection, selectionArgs,
                        null, null, sortOrder);
                break;
            case URI_MESSAGE:
                cursor = db.query(TABLE_MESSAGE, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case URI_MESSAGE_ID:
                selectionId = "_id=" + Long.valueOf(uri.getLastPathSegment());
                selection = concatSelections(selection, selectionId);
                cursor = db.query(TABLE_MESSAGE, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case URI_SEARCH_SUGGEST: {
                if (       sortOrder != null
                        || selection != null
                        || selectionArgs != null
                        || projection != null) {
                    throw new IllegalArgumentException(
                            "do not specify sortOrder, selection, selectionArgs, or projection" +
                            "with this query");
                }
                Log.d(TAG, "query().URI_RCS_SEARCH_SUGGEST: uriStr = " + uri);
                String uriStr = uri.toString();
                String parameterStr = "pattern=";
                String searchString = uriStr.substring(
                        uriStr.lastIndexOf(parameterStr) + parameterStr.length()).trim();
                Log.d(TAG, "query().URI_RCS_SEARCH_SUGGEST: searchString = \"" +
                        searchString + "\"");

                String pattern = "%" + searchString + "%";
                if (searchString.trim().equals("") || searchString == null) {
                    cursor = null;
                } else {
                    HashMap<String, String> contactRes = getContactsByNumber(searchString);
                    String searchContacts = searchContacts(searchString, contactRes);
                    String smsIdQuery = String.format("SELECT _id FROM sms WHERE thread_id "
                                + searchContacts);
                    String smsIn = queryIdAndFormatIn(db, smsIdQuery);

                    String mmsIdQuery = String.format("SELECT part._id FROM part JOIN pdu " +
                            " ON part.mid=pdu._id " +
                            " WHERE part.ct='text/plain' AND pdu.thread_id " + searchContacts);
                    String mmsIn = queryIdAndFormatIn(db, mmsIdQuery);

                    String mmsPduIdQuery = String.format("SELECT _id FROM pdu" +
                            " WHERE thread_id " + searchContacts);
                    String mmsPduIn = queryIdAndFormatIn(db, mmsPduIdQuery);

                    String rcsIdQuery = String.format("SELECT _id FROM rcs_message WHERE " +
                        "(" + RCSProvider.MESSAGE_COLUMN_CLASS + "=" + RCSProvider.CLASS_NORMAL +
                        " AND " + RCSProvider.MESSAGE_COLUMN_TYPE + "=" + RCSProvider.TYPE_IM +
                        " AND " +
                        RCSProvider.MESSAGE_COLUMN_CONVERSATION + " " + searchContacts + ")");
                    String rcsIn = queryIdAndFormatIn(db, rcsIdQuery);

                    String query = "SELECT DISTINCT _id, index_text AS " +
                            SearchManager.SUGGEST_COLUMN_TEXT_1 + ", _id AS " +
                            SearchManager.SUGGEST_COLUMN_SHORTCUT_ID + ", index_text AS snippet" +
                            " FROM words WHERE index_text IS NOT NULL AND length(index_text)>0 " +
                            " AND ((index_text LIKE ? AND table_to_use!=3) " +
                            " OR (source_id " + smsIn + " AND table_to_use=1) " +
                            " OR (source_id " + mmsIn + " AND table_to_use=2) " +
                            " OR (source_id " + rcsIn + " AND table_to_use=" +
                                    RCS_TABLE_TO_USE + ") " +
                            " OR (source_id " + mmsPduIn + " AND table_to_use=" +
                                    MmsProvider.TABLE_TO_USE_SUBJECT + ")) " +
                            " ORDER BY snippet LIMIT 50";
                    cursor = db.rawQuery(query, new String[]{pattern});
                    Log.d(TAG, "search suggestion cursor count is : " + cursor.getCount());
                }
                break;
            }
            case URI_SEARCH: {
                if (       sortOrder != null
                        || selection != null
                        || selectionArgs != null
                        || projection != null) {
                    throw new IllegalArgumentException(
                            "do not specify sortOrder, selection, selectionArgs, or projection" +
                            "with this query");
                }

                String pattern = uri.getQueryParameter("pattern");
                if (pattern != null) {
                   Log.d(TAG, "URI_SEARCH pattern = " + pattern.length());
                }
                /// M: Add for Chinese subject search @{
                String pduPattern = toIsoString(pattern.getBytes());
                pduPattern = "%" + pduPattern + "%";
                /// @}
                HashMap<String, String> contactRes = getContactsByNumber(pattern);
                String searchContacts = searchContacts(pattern, contactRes);
                String searchString = "%" + pattern + "%";

                String smsProjection = "sms._id as _id,thread_id,address,body,date," +
                    " 0 as index_text,words._id,0 as charset,0 as m_type,sms.type as msg_box," +
                    " null as chat_id";
                /// M: search on words table but return the rows from the corresponding sms table
                String smsQuery = String.format(
                        "SELECT %s FROM sms,words WHERE ((sms.body LIKE ? OR thread_id %s)" +
                        " AND sms._id=words.source_id AND words.table_to_use=1 AND" +
                        " (sms.thread_id IN (SELECT _id FROM threads)))",
                        smsProjection,
                        searchContacts);

                /// M: search on words table but return the rows from the corresponding parts table
                String mmsProjection = "pdu._id,thread_id,addr.address,pdu.sub as " + "" +
                        "body,pdu.date,0 as index_text,0,addr.charset as charset," +
                        " pdu.m_type as m_type,pdu.msg_box as msg_box, null as chat_id";
                String mmsQuery = String.format(Locale.ENGLISH,
                        "SELECT %s FROM pdu left join part ON pdu._id=part.mid AND " +
                        " part.ct='text/plain' left join addr on addr.msg_id=pdu._id " +
                        " WHERE ((((addr.type=%d) AND (pdu.msg_box == %d)) OR ((addr.type=%d) AND" +
                        " (pdu.msg_box != %d))) " +
                        "AND (part.text LIKE ? OR pdu.sub LIKE ? OR thread_id %s) " +
                        "AND (pdu.thread_id IN (SELECT _id FROM threads)))",
                        mmsProjection,
                        PduHeaders.FROM,
                        Mms.MESSAGE_BOX_INBOX,
                        PduHeaders.TO,
                        Mms.MESSAGE_BOX_INBOX,
                        searchContacts);
                ///@}

                String rcsProjection = "rcs_message._id AS _id " +
                        ", " + MESSAGE_COLUMN_CONVERSATION + " AS thread_id " +
                        ", " + MESSAGE_COLUMN_CONTACT_NUMBER + " AS address " +
                        ", " + MESSAGE_COLUMN_BODY + " AS body " +
                        ", " + MESSAGE_COLUMN_TIMESTAMP + " AS date " +
                        ", 0 AS index_text " +
                        ", words._id " +
                        ", 0 AS charset " +
                        ", 0 AS m_type " +
                        ", CASE " + MESSAGE_COLUMN_MESSAGE_STATUS +
                        "     WHEN 3 THEN 4 " +
                        "     WHEN 4 THEN 2 " +
                        "     WHEN 5 THEN 5 " +
                        "     WHEN 6 THEN 3 " +
                        "     ELSE 1 END AS msg_box " +
                        ", " + MESSAGE_COLUMN_CHAT_ID + " AS chat_id ";
                String rcsQuery = String.format(Locale.ENGLISH,
                        "SELECT %s FROM rcs_message, words WHERE " +
                        " (( " + MESSAGE_COLUMN_BODY + " LIKE ? OR " +
                                MESSAGE_COLUMN_CONVERSATION + " %s) " +
                        " AND rcs_message._id=words.source_id " +
                        " AND words.table_to_use= " + RCS_TABLE_TO_USE + " " +
                        " AND (" + MESSAGE_COLUMN_CONVERSATION + " IN (SELECT _id FROM threads)))",
                        rcsProjection, searchContacts);
                //// M: join the results from sms and part (mms) and rcs_message
                String rawQuery = String.format(
                        "SELECT * FROM (%s UNION %s UNION %s) GROUP BY %s ORDER BY %s",
                        smsQuery,
                        mmsQuery,
                        rcsQuery,
                        "thread_id",
                        "date DESC");

                try {
                    cursor = db.rawQuery(rawQuery, new String[] {searchString, searchString,
                            pduPattern, searchString});
                    Log.e(TAG, "rawQuery = " + rawQuery);
                } catch (Exception ex) {
                    Log.e(TAG, "got exception: " + ex.toString());
                    return null;
                }
                break;
            }
            default:
                throw new UnsupportedOperationException("Not Support URI " + uri);
        }
        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), MmsSms.CONTENT_URI);
        }
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where,
            String[] whereArgs) {
        Log.d(TAG, "update uri=" + uri.toString() + ", values=" + values + ", where=" +
                where + ", whereArgs=" + whereArgs);
        if (!MTK_RCS_SUPPORT) {
            return 0;
        }
        int count = 0;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = uriMatcher.match(uri);
        switch(match) {
            case URI_MESSAGE:
                count = db.update(TABLE_MESSAGE, values, where, whereArgs);
                break;
            case URI_MESSAGE_ID:
                where = concatSelections(where, "_id =" + uri.getLastPathSegment());
                count = db.update(TABLE_MESSAGE, values, where, whereArgs);
                break;
            case URI_THREAD_SETTINGS:
                where = concatSelections(where, "thread_id = " + uri.getLastPathSegment());
                count = db.update(MmsSmsProvider.TABLE_THREAD_SETTINGS, values, where, whereArgs);
                break;
            case URI_THREAD_STATUS:
                String[] columns = {THREADS_COLUMN_ID, THREADS_COLUMN_RECIPIENTS};
                Cursor cursor = db.query(VIEW_RCS_THREADS, columns, where,
                        whereArgs, null, null, null);
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        long threadId = cursor.getLong(cursor.getColumnIndex(THREADS_COLUMN_ID));
                        String whereClause = "_id=" + threadId;
                        count = db.update(MmsSmsProvider.TABLE_THREADS, values, whereClause, null);
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                break;
            default:
                throw new UnsupportedOperationException("Not Support URI " + uri);
        }
        if (count > 0) {
            notifyChange();
        }
        return count;
    }

    private void notifyChange() {
        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(Telephony.MmsSms.CONTENT_URI, null, true,
                UserHandle.USER_ALL);
        cr.notifyChange(Uri.parse("content://mms-sms/conversations/"), null, true,
                UserHandle.USER_ALL);
        cr.notifyChange(Uri.parse("content://mms-sms-rcs/conversations/"), null, true,
                UserHandle.USER_ALL);
    }

    private static void asyncDeleteStackMessages(final Context context,
            final Set<Long> imIds, final Set<Long> ftIds) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                Log.d(TAG, "delete stack messages start");
                ContentResolver resolver = context.getContentResolver();
                resolver.delete(RCS_MESSAGE_URI, "msg_type=1", null);
                if (imIds != null && imIds.size() > 0) {
                    String imWhere = "_id " + toInClause(imIds);
                    resolver.delete(RCS_MESSAGE_URI, imWhere, null);
                }
                if (ftIds != null && ftIds.size() > 0) {
                    String ftWhere = "_id " + toInClause(ftIds);
                    resolver.delete(RCS_FT_URI, ftWhere, null);
                }
                Log.d(TAG, "delete stack messages end");
            }
        }).start();
    }

    private static final int DELETE_LIMIT = 100;

    static int deleteMessages(Context context, SQLiteDatabase db,
            String selection, String[] selectionArgs) {
        Log.d(TAG, "deleteMessages, start");
        String[] projection = {COLUMN_ID, MESSAGE_COLUMN_TYPE,
                MESSAGE_COLUMN_IPMSG_ID, MESSAGE_COLUMN_CONVERSATION};
        Cursor cursor = db.query(TABLE_MESSAGE, projection, selection,
                selectionArgs, null, null, null);
        if (cursor == null) {
            return 0;
        }
        Set<Long> imIds = new HashSet<Long>();
        Set<Long> ftIds = new HashSet<Long>();
        try {
            if (cursor.getCount() == 0) {
                return 0;
            }
            int typePos = cursor.getColumnIndex(MESSAGE_COLUMN_TYPE);
            int ipmsgIdPos = cursor.getColumnIndex(MESSAGE_COLUMN_IPMSG_ID);
            while (cursor.moveToNext()) {
                if (cursor.getInt(typePos) == TYPE_IM) {
                    imIds.add(cursor.getLong(ipmsgIdPos));
                } else if (cursor.getLong(typePos) == TYPE_FT) {
                    ftIds.add(cursor.getLong(ipmsgIdPos));
                }
            }
        } finally {
            cursor.close();
        }
        int deleteCount = DELETE_LIMIT;
        if (TextUtils.isEmpty(selection)) {
            selection = "_id IN (SELECT _id FROM " + TABLE_MESSAGE + " LIMIT " + DELETE_LIMIT + ")";
        } else {
            selection = "_id IN (SELECT _id FROM " + TABLE_MESSAGE + " WHERE " +
                    selection + " LIMIT " + DELETE_LIMIT + ")";
        }
        int count = 0;
        while (deleteCount > 0) {
            deleteCount = db.delete(TABLE_MESSAGE, selection, selectionArgs);
            count += deleteCount;
            Log.d(TAG, "deleteMessages, delete " + deleteCount + " rcs");
        }
        Log.d(TAG, "deleteMessages, delete rcs end, affected rows = " + count);
        if (count > 0) {
            asyncDeleteStackMessages(context, imIds, ftIds);
        }
        return count;
    }

    static void updateConversation(SQLiteDatabase db, long thread_id) {

        String selection = MESSAGE_COLUMN_CONVERSATION + " = " + thread_id;
        String nonSystemMsgSelection = MESSAGE_COLUMN_CLASS + " < " + CLASS_SYSTEM;
        String updateSQL =
                " UPDATE rcs_conversations SET " +
                CONVERSATION_COLUMN_READ + " = (CASE " +
                "           (SELECT COUNT(_id) " +
                "            FROM " + TABLE_MESSAGE +
                "            WHERE " + selection + " AND " +
                                MESSAGE_COLUMN_MESSAGE_STATUS + "=" + MESSAGE_STATUS_UNREAD + ") " +
                "        WHEN 0 THEN 1 ELSE 0 END), " +
                CONVERSATION_COLUMN_ERROR + " = (CASE " +
                "           (SELECT COUNT(_id) " +
                "            FROM " + TABLE_MESSAGE +
                "            WHERE " + selection + " AND " +
                                MESSAGE_COLUMN_MESSAGE_STATUS + "=" + MESSAGE_STATUS_FAILED + ") " +
                "        WHEN 0 THEN 0 ELSE 1 END), " +
                CONVERSATION_COLUMN_ATTACHMENT + " = (CASE " +
                "           (SELECT COUNT(_id) " +
                "            FROM " + TABLE_MESSAGE +
                "            WHERE " + selection + " AND " +
                                MESSAGE_COLUMN_TYPE + " = " + TYPE_FT + " ) " +
                "        WHEN 0 THEN 0 ELSE 1 END), " +
                CONVERSATION_COLUMN_CLASS + " = " +
                "       (SELECT " + MESSAGE_COLUMN_CLASS + " FROM " + TABLE_MESSAGE +
                "       WHERE " + selection +
                "       ORDER BY " + CONVERSATION_COLUMN_TIMESTAMP + " DESC LIMIT 1), " +
                CONVERSATION_COLUMN_BODY + " = " +
                "       (SELECT " + MESSAGE_COLUMN_BODY + " FROM " + TABLE_MESSAGE +
                "       WHERE " + selection +
                "       ORDER BY " + CONVERSATION_COLUMN_TIMESTAMP + " DESC LIMIT 1), " +
                CONVERSATION_COLUMN_TIMESTAMP + " = " +
                "       (SELECT " + CONVERSATION_COLUMN_TIMESTAMP + " FROM " + TABLE_MESSAGE +
                "       WHERE " + selection +
                "       ORDER BY " + CONVERSATION_COLUMN_TIMESTAMP + " DESC LIMIT 1), " +
                CONVERSATION_COLUMN_TYPE + " = " +
                "       (SELECT " + CONVERSATION_COLUMN_TYPE + " FROM " + TABLE_MESSAGE +
                "       WHERE " + selection +
                "       ORDER BY " + CONVERSATION_COLUMN_TIMESTAMP + " DESC LIMIT 1), " +
                CONVERSATION_COLUMN_MESSAGE_COUNT + " = (SELECT COUNT(_id) FROM " + TABLE_MESSAGE +
                "       WHERE " + selection + " AND "+ nonSystemMsgSelection + "), " +
                CONVERSATION_COLUMN_UNREAD_COUNT + " = (SELECT COUNT(_id) FROM " + TABLE_MESSAGE +
                "       WHERE " + selection + " AND " + nonSystemMsgSelection +
                "       AND " + MESSAGE_COLUMN_MESSAGE_STATUS + "=" + MESSAGE_STATUS_UNREAD + ")," +
                CONVERSATION_COLUMN_MIME_TYPE + " = " +
                "       (SELECT " + CONVERSATION_COLUMN_MIME_TYPE + " FROM " + TABLE_MESSAGE +
                "       WHERE " + selection +
                "       ORDER BY " + CONVERSATION_COLUMN_TIMESTAMP + " DESC LIMIT 1) " +
                " WHERE " + CONVERSATION_COLUMN_CONVERSATION + " = " + thread_id;
        db.execSQL(updateSQL);
        String[] projection = {COLUMN_ID};
        Cursor cursor = db.query(TABLE_MESSAGE, projection, selection, null, null, null, null);
        try {
            if (cursor != null && cursor.getCount() <= 0) {
                ContentValues values = new ContentValues();
                values.put(CONVERSATION_COLUMN_TIMESTAMP, 0);
                db.update(TABLE_CONVERSATIONS, values, selection, null);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void notifyUnreadMessageNumberChanged(final Context context) {
        int unreadNumber = getUnreadMessageNumber(context);
        MmsSmsProvider.recordUnreadMessageNumberToSys(context, unreadNumber);
        MmsSmsProvider.broadcastUnreadMessageNumber(context, unreadNumber);
    }

    private int getUnreadMessageNumber(Context context) {
        int threadsUnreadCount = 0;
        String threadsQuery = "select sum(message_count - readcount) as unreadcount " +
                "from rcs_threads where read=0 and " + Threads.TYPE + "<>" + Threads.WAPPUSH_THREAD;
        Cursor c = MmsSmsDatabaseHelper.getInstance(context)
                .getReadableDatabase().rawQuery(threadsQuery, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    threadsUnreadCount = c.getInt(0);
                    Log.d(TAG, "get threads unread message count = " + threadsUnreadCount);
                }
            } finally {
                c.close();
            }
        } else {
            Log.d(TAG, "can not get unread message count.");
        }
        return threadsUnreadCount;
    }

    private Cursor getRCSSimpleConversations(String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        // change the _id to rcs_threads._id
        for (int i = 0; i < projection.length; i++) {
            if (projection[i].equals("_id")) {
                projection[i] = "rcs_threads._id AS _id";
            }
        }
        selection = concatSelections(selection,
                "rcs_threads._id=thread_settings.thread_id");
        Log.d(TAG, "getRCSSimpleConversations, extend query selection:" + selection);
        return mOpenHelper.getReadableDatabase().query("rcs_threads,thread_settings", projection,
                selection, selectionArgs, null, null,
                "thread_settings.top DESC, date DESC");
    }

    private String[] makeRCSProjectionWithNormalizedDate(
            String[] projection, int dateMultiple) {
        int projectionSize = projection.length;
        String[] result = new String[projectionSize + 1];

        result[0] = MESSAGE_COLUMN_TIMESTAMP + " * " + dateMultiple + " AS normalized_date";
        System.arraycopy(projection, 0, result, 1, projectionSize);
        return result;
    }

    private String[] makeProjectionWithDateAndThreadId(
            String[] projection, int dateMultiple) {
        int projectionSize = projection.length;
        String[] result = new String[projectionSize + 2];

        result[0] = "thread_id AS tid";
        result[1] = "date * " + dateMultiple + " AS normalized_date";
        for (int i = 0; i < projectionSize; i++) {
            result[i + 2] = projection[i];
        }
        return result;
    }

    private String[] makeRCSProjectionWithDateAndThreadId(
            String[] projection, int dateMultiple) {
        int projectionSize = projection.length;
        String[] result = new String[projectionSize + 2];

        result[0] = MESSAGE_COLUMN_CONVERSATION + " AS tid";
        result[1] = MESSAGE_COLUMN_TIMESTAMP + " * " + dateMultiple + " AS normalized_date";
        for (int i = 0; i < projectionSize; i++) {
            result[i + 2] = projection[i];
        }
        return result;
    }

    private Cursor getRCSConversationMessages(Uri uri, String[] projection, String selection,
            String sortOrder, String smsTable, String pduTable) {
        try {
            Long.parseLong(uri.getLastPathSegment());
        } catch (NumberFormatException exception) {
            Log.e(TAG, "Thread ID must be a Long.");
            return null;
        }

        String unionQuery = buildRCSConversationQuery(uri, projection, selection,
                sortOrder, smsTable, pduTable);
        Log.d(TAG, "unionQuery = " + unionQuery);
        return mOpenHelper.getReadableDatabase().rawQuery(unionQuery,
                MmsSmsProvider.EMPTY_STRING_ARRAY);
    }

    private String buildRCSConversationQuery(Uri uri, String[] projection,
            String selection, String sortOrder, String smsTable, String pduTable) {

        boolean markAsRead = "true".equals(uri.getQueryParameter("read"));
        boolean multiDelete = "true".equals(uri.getQueryParameter("MultiDelete"));
        String threadIdString = uri.getLastPathSegment();

        SQLiteQueryBuilder mmsQueryBuilder = new SQLiteQueryBuilder();
        String[] mmsProjection = createMmsProjection(projection, pduTable);
        mmsQueryBuilder.setDistinct(true);
        mmsQueryBuilder.setTables(joinPduAndPendingMsgTables(pduTable));
        String[] mmsColumns = handleNullMessageProjection(mmsProjection);
        String[] innerMmsProjection = makeProjectionWithNormalizedDate(mmsColumns, 1000);
        Set<String> columnsPresentInTable = new HashSet<String>(MmsSmsProvider.MMS_COLUMNS);
        columnsPresentInTable.add(pduTable + "._id");
        columnsPresentInTable.add(PendingMessages.ERROR_TYPE);
        String mmsSelection = concatSelections(selection,
                Mms.MESSAGE_BOX + " <> " + Mms.MESSAGE_BOX_DRAFTS);
        mmsSelection = concatSelections(selection, "thread_id=" + threadIdString);
        if (markAsRead) {
            mmsSelection = concatSelections(mmsSelection, "read=0");
        }
        String mmsSubQuery = mmsQueryBuilder.buildUnionSubQuery(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN, innerMmsProjection,
                columnsPresentInTable, 0, "mms",
                concatSelections(mmsSelection, MmsSmsProvider.MMS_CONVERSATION_CONSTRAINT),
                null, null);

        SQLiteQueryBuilder smsQueryBuilder = new SQLiteQueryBuilder();
        smsQueryBuilder.setDistinct(true);
        smsQueryBuilder.setTables(smsTable);
        String[] smsColumns = handleNullMessageProjection(projection);
        String[] innerSmsProjection = makeProjectionWithNormalizedDate(smsColumns, 1);
        String smsSelection = concatSelections(selection, "thread_id=" + threadIdString);
        if (markAsRead) {
            smsSelection = concatSelections(smsSelection, "read=0");
        }
        String smsSubQuery = smsQueryBuilder.buildUnionSubQuery(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN, innerSmsProjection, MmsSmsProvider.SMS_COLUMNS,
                0, "sms",
                concatSelections(smsSelection, MmsSmsProvider.SMS_CONVERSATION_CONSTRAINT),
                null, null);

        /// M: add for op01 RCS feature: group chat system message. @{
        SQLiteQueryBuilder rcsQueryBuilder = new SQLiteQueryBuilder();
        rcsQueryBuilder.setDistinct(true);
        rcsQueryBuilder.setTables("rcs_message");
        String[] rcsColumns = handleNullMessageProjection(projection);
        String[] innerRcsProjection = makeRCSProjectionWithNormalizedDate(rcsColumns, 1);
        String rcsSelection = concatSelections(selection,
                MESSAGE_COLUMN_CONVERSATION + "=" + threadIdString);
        if (multiDelete) {
            rcsSelection = concatSelections(rcsSelection,
                    MESSAGE_COLUMN_CLASS + "<" + CLASS_SYSTEM);
        }
        if (markAsRead) {
            rcsSelection = concatSelections(rcsSelection,
                    MESSAGE_COLUMN_MESSAGE_STATUS + "=" + MESSAGE_STATUS_UNREAD);
        }
        String rcsSubQuery = rcsQueryBuilder.buildUnionSubQuery(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN, innerRcsProjection, RCS_COLUMNS,
                0, "rcs", rcsSelection, null, null);
        /// @}

        /// M: Code analyze 003, new feature, support for cellbroadcast.
        SQLiteQueryBuilder cbQueryBuilder = new SQLiteQueryBuilder();
        cbQueryBuilder.setDistinct(true);
        cbQueryBuilder.setTables("cellbroadcast");
        String[] cbColumns = handleNullMessageProjection(projection);
        String[] innerCbProjection = makeProjectionWithNormalizedDate(cbColumns, 1);
        String cbSelection = concatSelections(selection, "thread_id=" + threadIdString);
        String cbSubQuery = cbQueryBuilder.buildUnionSubQuery(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN, innerCbProjection, MmsSmsProvider.CB_COLUMNS,
                0, "cellbroadcast", cbSelection, null, null);
        /// @}

        SQLiteQueryBuilder unionQueryBuilder = new SQLiteQueryBuilder();
        unionQueryBuilder.setDistinct(true);
        String unionQuery = unionQueryBuilder.buildUnionQuery(
                new String[] { smsSubQuery, mmsSubQuery, cbSubQuery, rcsSubQuery},
                MmsSmsProvider.handleNullSortOrder(sortOrder), null);
        SQLiteQueryBuilder outerQueryBuilder = new SQLiteQueryBuilder();
        outerQueryBuilder.setTables("(" + unionQuery + ")");

        return outerQueryBuilder.buildQuery(
                smsColumns, null, null, null, sortOrder, null);
    }

    private Cursor getRCSFirstLockedMessage(String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        String specialSelection = null;
        boolean all = true;
        if (selectionArgs != null && selectionArgs.length > 0) {
            StringBuilder buf = new StringBuilder();
            int i = 0;
            buf.append(" IN ( ");
            for (String selectionArg : selectionArgs) {
                if (i++ > 0) {
                    buf.append(",");
                }
                buf.append(selectionArg);
            }
            buf.append(" )");
            specialSelection = buf.toString();
            all = false;
        }
        Log.d(TAG, "getRCSFirstLockedMessage start");
        String[] idColumn = new String[] { BaseColumns._ID };
        // MMS sub query
        SQLiteQueryBuilder mmsQueryBuilder = new SQLiteQueryBuilder();
        mmsQueryBuilder.setTables(MmsProvider.TABLE_PDU);
        String mmsSubQuery = mmsQueryBuilder.buildUnionSubQuery(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN, idColumn,
                null, 1, "mms",
                all ? null : "thread_id " + specialSelection,
                BaseColumns._ID, "locked=1");
        // SMS sub query
        SQLiteQueryBuilder smsQueryBuilder = new SQLiteQueryBuilder();
        smsQueryBuilder.setTables(SmsProvider.TABLE_SMS);
        String smsSubQuery = smsQueryBuilder.buildUnionSubQuery(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN, idColumn,
                null, 1, "sms",
                all ? null : "thread_id " + specialSelection,
                BaseColumns._ID, "locked=1");
        // RCS sub query
        SQLiteQueryBuilder rcsQueryBuilder = new SQLiteQueryBuilder();
        rcsQueryBuilder.setTables(TABLE_MESSAGE);
        String rcsSubQuery = rcsQueryBuilder.buildUnionSubQuery(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN, idColumn,
                null, 1, "rcs",
                all ? null : CONVERSATION_COLUMN_CONVERSATION + specialSelection,
                BaseColumns._ID, "locked=1");
        // UNION query
        SQLiteQueryBuilder unionQueryBuilder = new SQLiteQueryBuilder();
        unionQueryBuilder.setDistinct(true);
        String unionQuery = null;
        unionQuery = unionQueryBuilder.buildUnionQuery(
                new String[] { mmsSubQuery, smsSubQuery, rcsSubQuery }, null, "1");

        Cursor cursor = mOpenHelper.getReadableDatabase().rawQuery(unionQuery,
                MmsSmsProvider.EMPTY_STRING_ARRAY);

        Log.d(TAG, "getRCSFirstLockedMessage query: " + unionQuery +
                ", cursor count=" + cursor.getCount());
        return cursor;
    }

    private Cursor getRCSUndeliveredMessages(
            String[] projection, String selection, String[] selectionArgs,
            String sortOrder, String smsTable, String pduTable,
            Boolean includeNonPermanentFail
            ) {
        // build mms sub query
        String[] mmsProjection = createMmsProjection(projection, pduTable);
        SQLiteQueryBuilder mmsQueryBuilder = new SQLiteQueryBuilder();
        mmsQueryBuilder.setTables(joinPduAndPendingMsgTables(pduTable));
        String finalMmsSelection;
        if (includeNonPermanentFail) {
            Log.d(TAG, "getRCSUndeliveredMessages true");
            finalMmsSelection = concatSelections(
                    selection, "(" + Mms.MESSAGE_BOX + " = " + Mms.MESSAGE_BOX_OUTBOX
                    + " OR " + Mms.MESSAGE_BOX + " = " + Mms.MESSAGE_BOX_FAILED + ")");
        } else {
            Log.d(TAG, "getRCSUndeliveredMessages false");
            finalMmsSelection = concatSelections(
                    selection, Mms.MESSAGE_BOX + " = " + Mms.MESSAGE_BOX_OUTBOX
                    + " AND " + PendingMessages.ERROR_TYPE + " = " +
                            MmsSms.ERR_TYPE_GENERIC_PERMANENT);
        }
        String[] mmsColumns = handleNullMessageProjection(mmsProjection);
        String[] innerMmsProjection = makeProjectionWithDateAndThreadId(
                mmsColumns, 1000);
        Set<String> columnsPresentInTable = new HashSet<String>(MmsSmsProvider.MMS_COLUMNS);
        columnsPresentInTable.add(pduTable + "._id");
        columnsPresentInTable.add(PendingMessages.ERROR_TYPE);
        String mmsSubQuery = mmsQueryBuilder.buildUnionSubQuery(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN, innerMmsProjection,
                columnsPresentInTable, 1, "mms", finalMmsSelection,
                null, null);

        // build sms sub query
        SQLiteQueryBuilder smsQueryBuilder = new SQLiteQueryBuilder();
        smsQueryBuilder.setTables(smsTable);
        String finalSmsSelection = concatSelections(
                selection, "(" + Sms.TYPE + " = " + Sms.MESSAGE_TYPE_OUTBOX
                + " OR " + Sms.TYPE + " = " + Sms.MESSAGE_TYPE_FAILED
                + " OR " + Sms.TYPE + " = " + Sms.MESSAGE_TYPE_QUEUED + ")");
        String[] smsColumns = handleNullMessageProjection(projection);
        String[] innerSmsProjection = makeProjectionWithDateAndThreadId(
                smsColumns, 1);
        String smsSubQuery = smsQueryBuilder.buildUnionSubQuery(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN, innerSmsProjection,
                MmsSmsProvider.SMS_COLUMNS, 1, "sms", finalSmsSelection,
                null, null);

        // build rcs sub query
        SQLiteQueryBuilder rcsQueryBuilder = new SQLiteQueryBuilder();
        rcsQueryBuilder.setDistinct(true);
        rcsQueryBuilder.setTables(TABLE_MESSAGE);
        String[] rcsColumns = handleNullMessageProjection(projection);
        String[] innerRcsProjection = makeRCSProjectionWithDateAndThreadId(rcsColumns, 1);
        String finalRcsSelection = concatSelections(selection,
                " ( " + MESSAGE_COLUMN_MESSAGE_STATUS + "=" + MESSAGE_STATUS_FAILED
                + " OR " + MESSAGE_COLUMN_MESSAGE_STATUS + "=" + MESSAGE_STATUS_SENDING + " ) ");
        String rcsSubQuery = rcsQueryBuilder.buildUnionSubQuery(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN, innerRcsProjection, RCS_COLUMNS,
                1, "rcs", finalRcsSelection, null, null);

        SQLiteQueryBuilder unionQueryBuilder = new SQLiteQueryBuilder();
        unionQueryBuilder.setDistinct(true);
        String unionQuery = unionQueryBuilder.buildUnionQuery(
                new String[] { smsSubQuery, mmsSubQuery, rcsSubQuery }, null, null);

        SQLiteQueryBuilder outerQueryBuilder = new SQLiteQueryBuilder();
        outerQueryBuilder.setTables("(" + unionQuery + ")");

        String outerQuery = outerQueryBuilder.buildQuery(
                smsColumns, null, null, null, sortOrder, null);

        return mOpenHelper.getReadableDatabase().rawQuery(outerQuery,
                MmsSmsProvider.EMPTY_STRING_ARRAY);
    }

    private String concatSelections(String selection1, String selection2) {
        return MmsSmsProvider.concatSelections(selection1, selection2);
    }

    private String[] makeProjectionWithNormalizedDate(String[] projection, int dateMultiple) {
        return MmsSmsProvider.makeProjectionWithNormalizedDate(projection, dateMultiple);
    }

    private String[] handleNullMessageProjection(String[] projection) {
        return MmsSmsProvider.handleNullMessageProjection(projection);
    }

    private String[] createMmsProjection(String[] projection, String pduTable) {
        return MmsSmsProvider.createMmsProjection(projection, pduTable);
    }

    private String joinPduAndPendingMsgTables(String pduTable) {
        return MmsSmsProvider.joinPduAndPendingMsgTables(pduTable);
    }

    private String getValidNumber(String numberOrEmail) {
        return MmsSmsProvider.getValidNumber(numberOrEmail);
    }

    private HashMap<String, String> getContactsByNumber(String pattern) {
        Builder builder = MmsSmsProvider.PICK_PHONE_EMAIL_FILTER_URI.buildUpon();
        builder.appendPath(pattern);      /// M:  Builder will encode the query
        Log.d(TAG, "getContactsByNumber uri = " + builder.build().toString());
        Cursor cursor = null;

        /// M: query the related contact numbers and name
        HashMap<String, String> contacts = new HashMap<String, String>();
        long token = Binder.clearCallingIdentity();
        try {
            cursor = getContext().getContentResolver().query(builder.build(),
                new String[] {Phone.DISPLAY_NAME_PRIMARY, Phone.NUMBER}, null, null, "sort_key");
            Log.d(TAG, "getContactsByNumber getContentResolver query contact 1" +
                    " cursor " + cursor.getCount());
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                String number = getValidNumber(cursor.getString(1));
                Log.d(TAG, "getContactsByNumber number = " + number + " name = " + name);
                contacts.put(number, name);
            }
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, ex.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            Binder.restoreCallingIdentity(token);
        }
         return contacts;
    }

    private String searchContacts(String pattern, HashMap<String, String> contactRes) {
        String name = null;
        /* query the related thread ids */
        Set<Long> threadIds = new HashSet<Long>();
        Cursor cursor = mOpenHelper.getReadableDatabase().rawQuery(
                "SELECT " + Threads._ID + "," + Threads.RECIPIENT_IDS + " FROM threads", null);
        /// M: Query all recipients in table canonical_addresses, use result
        /// cursor instead of querying each time
        Cursor addrCursor = mOpenHelper.getReadableDatabase().rawQuery(
                "SELECT _id, address FROM canonical_addresses ", null);
        try {
            HashMap<Integer, String> contacts = new HashMap<Integer, String>();
            while (addrCursor.moveToNext()) {
                contacts.put(addrCursor.getInt(0), addrCursor.getString(1));
            }
            while (cursor.moveToNext()) {
                if (TextUtils.isEmpty(cursor.getString(1))) {
                    continue;
                }
                Long threadId = cursor.getLong(0);
                Set<String> recipients = getRecipientNumbers(cursor.getString(1), contacts);
                for (String recipient : recipients) {
                    if (recipient.toLowerCase().contains(pattern.toLowerCase())) {
                        threadIds.add(threadId);
                        break;
                    }
                    name = (String) contactRes.get(recipient);
                    /// M: fix ALPS00446245, some time coming address is +86xxxx,
                    /// but phone book saved number
                    /// is xxx. So make an enhancement. @{
                    if (name == null) {
                        Set<String> addresses = contactRes.keySet();
                        for (String addr : addresses) {
                            if (PhoneNumberUtils.compare(addr, recipient)) {
                                name = (String) contactRes.get(addr);
                                break;
                            }
                        }
                    }
                    /// @}
                    /// M: fix bug ALPS00498271, Ignore case sensitive : "Test1" contain "test".
                    if (name != null && name.toLowerCase().contains(pattern.toLowerCase())) {
                        threadIds.add(threadId);
                        break;
                    }
                }
            }
        } finally {
            cursor.close();
            addrCursor.close();
        }
        Log.d(TAG, "searchContacts getContentResolver query recipient");
        return toInClause(threadIds);
    }

    private static String toInClause(Set<Long> ids) {
        String in = " IN ";
        in += ids.toString();
        in = in.replace('[', '(');
        in = in.replace(']', ')');
        Log.d(TAG, "toInClause, In = " + in);
        return in;
    }

    private Set<String> getRecipientNumbers(String recipientIds,
            HashMap<Integer, String> contacts) {
        return MmsSmsProvider.getRecipientNumbers(recipientIds, contacts);
    }

    private String queryIdAndFormatIn(SQLiteDatabase db, String sql) {
        return MmsSmsProvider.queryIdAndFormatIn(db, sql);
    }

    private String toIsoString(byte[] bytes) {
        return MmsSmsProvider.toIsoString(bytes);
    }

    static int removeObsoleteGroupThreads(SQLiteDatabase db, long[] threadIds) {
        String in = null;
        if (threadIds != null && threadIds.length > 0) {
            boolean firstItem = true;
            for (long threadId : threadIds) {
                if (firstItem) {
                    in = " IN (" + threadId;
                    firstItem = false;
                } else {
                    in = in + ", " + threadId;
                }
            }
            in += " )";
        }
        String selection = " status <> 0 AND " +
                " _id NOT IN " +
                "    (SELECT DISTINCT " + RCSProvider.MESSAGE_COLUMN_CONVERSATION +
                "        AS thread_id FROM " + RCSProvider.TABLE_MESSAGE + " where " +
                         RCSProvider.MESSAGE_COLUMN_CONVERSATION + " NOT NULL)";
        if (in != null) {
            selection = " status <> 0 AND " +
                    " _id " + in + " AND " +
                    " _id NOT IN " +
                    "    (SELECT DISTINCT " + RCSProvider.MESSAGE_COLUMN_CONVERSATION +
                    "        AS thread_id FROM " + RCSProvider.TABLE_MESSAGE + " where " +
                             RCSProvider.MESSAGE_COLUMN_CONVERSATION + " NOT NULL)";
        }
        int count = db.delete(MmsSmsProvider.TABLE_THREADS, selection, null);
        if (count > 0) {
            MmsSmsDatabaseHelper.removeOrphanedAddresses(db);
        }
        Log.d(TAG, "removeObsoleteGroupThreads, affected rows = " + count);
        return count;
    }
}
