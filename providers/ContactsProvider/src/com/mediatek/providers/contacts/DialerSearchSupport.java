package com.mediatek.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;

import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.AggregationExceptions;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.DialerSearch;
import android.provider.ContactsContract.Preferences;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.ImsCall;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import com.android.providers.contacts.ContactsDatabaseHelper;
import com.android.providers.contacts.ContactsDatabaseHelper.AggregationExceptionColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.ContactsColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.DataColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.MimetypesColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.NameLookupColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.RawContactsColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.SearchIndexColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.ContactsDatabaseHelper.Views;
import com.android.providers.contacts.util.UserUtils;
import com.android.providers.contacts.ContactsProvider2;
import com.android.providers.contacts.HanziToPinyin;
import com.mediatek.providers.contacts.LogUtils;
import com.mediatek.providers.contacts.ContactsProviderUtils;

public class DialerSearchSupport {
    private static final String TAG = "DialerSearchSupport";
    private static final boolean DS_DBG = ContactsProviderUtils.DBG_DIALER_SEARCH;

    public interface DialerSearchLookupColumns {
        public static final String _ID = BaseColumns._ID;
        public static final String RAW_CONTACT_ID = "raw_contact_id";
        public static final String DATA_ID = "data_id";
        public static final String NORMALIZED_NAME = "normalized_name";
        public static final String NAME_TYPE = "name_type";
        public static final String CALL_LOG_ID = "call_log_id";
        public static final String NUMBER_COUNT = "number_count";
        public static final String SEARCH_DATA_OFFSETS = "search_data_offsets";
        public static final String NORMALIZED_NAME_ALTERNATIVE = "normalized_name_alternative";
        public static final String SEARCH_DATA_OFFSETS_ALTERNATIVE =
                "search_data_offsets_alternative";
        public static final String IS_VISIABLE = "is_visiable";
        public static final String SORT_KEY = "sort_key";
        public static final String TIMES_USED = "times_used";
    }

    /**
     * M: for DialerSearchLookupType.
     */
    public final static class DialerSearchLookupType {
        public static final int PHONE_EXACT = 8;
        public static final int NO_NAME_CALL_LOG = 8;
        public static final int NAME_EXACT = 11;
    }

    /**
     * M: for DialerSearchQuery.
     */
    public interface DialerSearchQuery {
        String TABLE = Tables.DIALER_SEARCH;
        String[] COLUMNS = new String[] {
                DialerSearch.NAME_LOOKUP_ID,
                DialerSearch.CONTACT_ID,
                DialerSearchLookupColumns.DATA_ID,
                DialerSearch.CALL_DATE,
                DialerSearch.CALL_LOG_ID,
                DialerSearch.CALL_TYPE,
                DialerSearch.CALL_GEOCODED_LOCATION,
                DialerSearch.PHONE_ACCOUNT_ID,
                DialerSearch.PHONE_ACCOUNT_COMPONENT_NAME,
                DialerSearch.NUMBER_PRESENTATION,
                DialerSearch.INDICATE_PHONE_SIM,
                DialerSearch.CONTACT_STARRED,
                DialerSearch.PHOTO_ID,
                DialerSearch.SEARCH_PHONE_TYPE,
                DialerSearch.SEARCH_PHONE_LABEL,
                DialerSearch.NAME,
                DialerSearch.SEARCH_PHONE_NUMBER,
                DialerSearch.CONTACT_NAME_LOOKUP,
                DialerSearch.IS_SDN_CONTACT,
                DialerSearch.MATCHED_DATA_OFFSET,
                DialerSearch.MATCHED_NAME_OFFSET
        };

        /// M: fix CR:ALPS01563203,SDN icon not show lock icon in Dialer.
        public static final int NAME_LOOKUP_ID_INDEX = 0;
        public static final int CONTACT_ID_INDEX = 1;
        public static final int DATA_ID_INDEX = 2;
        public static final int CALL_LOG_DATE_INDEX = 3;
        public static final int CALL_LOG_ID_INDEX = 4;
        public static final int CALL_TYPE_INDEX = 5;
        public static final int CALL_GEOCODED_LOCATION_INDEX = 6;
        public static final int PHONE_ACCOUNT_ID = 7;
        public static final int PHONE_ACCOUNT_COMPONENT_NAME = 8;
        public static final int PRESENTATION = 9;
        public static final int INDICATE_PHONE_SIM_INDEX = 10;
        public static final int CONTACT_STARRED_INDEX = 11;
        public static final int PHOTO_ID_INDEX = 12;
        public static final int SEARCH_PHONE_TYPE_INDEX = 13;
        public static final int NUMBER_LABEL = 14;
        public static final int NAME_INDEX = 15;
        public static final int SEARCH_PHONE_NUMBER_INDEX = 16;
        public static final int CONTACT_NAME_LOOKUP_INDEX = 17;
        public static final int IS_SDN_CONTACT = 18;
        public static final int DS_MATCHED_DATA_OFFSETS = 19;
        public static final int DS_MATCHED_NAME_OFFSETS = 20;
    }

    private static final String TEMP_DIALER_SEARCH_TABLE = "temp_dialer_search_table";

    private static final int BACKGROUND_TASK_CREATE_CACHE = 0;
    private static final int BACKGROUND_TASK_REMOVE_CACHE = 1;
    private ContactsDatabaseHelper mDbHelper;
    private Context mContext;
    private boolean mIsCached = false;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private static DialerSearchSupport sDialerSearchSupport;
    private static final int CACHE_TASK_DELAY_MILLIS = 10000;
    private static final String sCachedOffsetsTempTableHead = "ds_offsets_temp_table_";

    private boolean mUseStrictPhoneNumberComparation;
    private int mNumberCount = 0;
    private int mDisplayOrder = -1;
    private int mSortOrder = -1;
    private int mPrevSearchNumberLen = 0;

    private DialerSearchSupport(Context context) {
        mContext = context;
        mDbHelper = ContactsDatabaseHelper.getInstance(mContext);

        mBackgroundThread = new HandlerThread("DialerSearchWorker",
                Process.THREAD_PRIORITY_BACKGROUND);
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                performBackgroundTask(msg.what);
            }
        };
    }

    /**
     * M: get a instance.
     * @param context context
     * @return DialerSearchSupport
     */
    public static synchronized DialerSearchSupport getInstance(Context context) {
        if (sDialerSearchSupport == null) {
            sDialerSearchSupport = new DialerSearchSupport(context);
            sDialerSearchSupport.initialize();
        }
        return sDialerSearchSupport;
    }

    /**
     * M: initialize.
     */
    public void initialize() {
        scheduleBackgroundTask(BACKGROUND_TASK_CREATE_CACHE);
    }

    private void performBackgroundTask(int task) {
        LogUtils.d(TAG, "performBackgroundTask," +
                    " mIsCached always should be: " + mIsCached + " | task: " + task);
        if (task == BACKGROUND_TASK_CREATE_CACHE) {
            mIsCached = true;
            createDsTempTableZero2Nine(mDbHelper.getWritableDatabase());
        } else if (task == BACKGROUND_TASK_REMOVE_CACHE) {
            removeDsTempTableZero2Nine(mDbHelper.getWritableDatabase());
        }
    }

//    public static String computeNormalizedNumber(String number) {
//        String normalizedNumber = null;
//        if (number != null) {
//            normalizedNumber = PhoneNumberUtils.getStrippedReversed(number);
//        }
//        return normalizedNumber;
//    }

    /**
     * M: create dialerSearch table.
     * @param db db
     */
    public static void createDialerSearchTable(SQLiteDatabase db) {
        if (ContactsProviderUtils.isSearchDbSupport()) {
            db.execSQL("CREATE TABLE " + Tables.DIALER_SEARCH + " ("
                    + DialerSearchLookupColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + DialerSearchLookupColumns.DATA_ID
                        + " INTEGER REFERENCES data(_id) NOT NULL,"
                    + DialerSearchLookupColumns.RAW_CONTACT_ID
                        + " INTEGER REFERENCES raw_contacts(_id) NOT NULL,"
                    + DialerSearchLookupColumns.NAME_TYPE + " INTEGER NOT NULL,"
                    + DialerSearchLookupColumns.CALL_LOG_ID + " INTEGER DEFAULT 0,"
                    + DialerSearchLookupColumns.NUMBER_COUNT + " INTEGER NOT NULL DEFAULT 0, "
                    + DialerSearchLookupColumns.IS_VISIABLE + " INTEGER NOT NULL DEFAULT 1, "
                    + DialerSearchLookupColumns.NORMALIZED_NAME + " VARCHAR DEFAULT NULL,"
                    + DialerSearchLookupColumns.SEARCH_DATA_OFFSETS + " VARCHAR DEFAULT NULL,"
                    + DialerSearchLookupColumns.NORMALIZED_NAME_ALTERNATIVE
                        + " VARCHAR DEFAULT NULL,"
                    + DialerSearchLookupColumns.SEARCH_DATA_OFFSETS_ALTERNATIVE
                        + " VARCHAR DEFAULT NULL " + ");");
            db.execSQL("CREATE INDEX dialer_data_id_index ON "
                    + Tables.DIALER_SEARCH + " ("
                    + DialerSearchLookupColumns.DATA_ID + ");");
            db.execSQL("CREATE INDEX dialer_search_raw_contact_id_index ON "
                    + Tables.DIALER_SEARCH + " ("
                    + DialerSearchLookupColumns.RAW_CONTACT_ID + ","
                    + DialerSearchLookupColumns.NAME_TYPE + ");");
            db.execSQL("CREATE INDEX dialer_search_call_log_id_index ON "
                    + Tables.DIALER_SEARCH + " ("
                    + DialerSearchLookupColumns.CALL_LOG_ID + ");");
        }
    }

    private static final String DIALER_SEARCH_TEMP_TABLE = "dialer_search_temp_table";

    private static void createDialerSearchTempTable(SQLiteDatabase db, String contactsSelect,
            String rawContactsSelect, String calllogSelect) {
        if (ContactsProviderUtils.isSearchDbSupport()) {
            db.execSQL("DROP TABLE IF EXISTS " + DIALER_SEARCH_TEMP_TABLE + ";");
            String dsNumberTableName = "t_ds_number";
            String dsCallsTableName = "t_ds_calls";
            String dsDataViewName = "v_ds_data";

            String dsCallsTable = " (SELECT "
                    + Calls._ID + ", "
                    + Calls.DATE + ", "
                    + Calls.TYPE + ", "
                    + Calls.PHONE_ACCOUNT_ID + ", "
                    + Calls.PHONE_ACCOUNT_COMPONENT_NAME + ", "
                    + Calls.NUMBER_PRESENTATION + ", "
                    + Calls.GEOCODED_LOCATION
                    + " FROM " + Tables.CALLS
                    + " WHERE (" + calllogSelect + ")"
                    + " ) AS " + dsCallsTableName;

            String dsDataView = " (SELECT "
                    + Data._ID + ", "
                    + Data.RAW_CONTACT_ID + ", "
                    + RawContacts.CONTACT_ID + ", "
                    + RawContacts.DISPLAY_NAME_PRIMARY + ", "
                    + RawContacts.DISPLAY_NAME_ALTERNATIVE + ", "
                    + Data.DATA2 + ", "
                    + Data.DATA3 + ", "
                    + Contacts.LOOKUP_KEY + ", "
                    + Contacts.PHOTO_ID + ", "
                    + Contacts.STARRED + ", "
                    + Contacts.INDICATE_PHONE_SIM + ", "
                    + Contacts.TIMES_CONTACTED + ", "
                    + Contacts.IS_SDN_CONTACT + ", "
                    + RawContacts.SORT_KEY_PRIMARY + ", "
                    + RawContacts.SORT_KEY_ALTERNATIVE
                    + " FROM " + Views.DATA
                    + " WHERE (" + contactsSelect + ")"
                    + " ) AS " + dsDataViewName;

            String dsViewSelect = " SELECT "
                    + dsNumberTableName
                    + "."
                    + DialerSearchLookupColumns._ID
                    + " AS "
                    + DialerSearch.NAME_LOOKUP_ID
                    + ","
                    + dsNumberTableName
                    + "."
                    + DialerSearchLookupColumns.DATA_ID
                    + " AS "
                    + DialerSearchLookupColumns.DATA_ID
                    + ","
                    + dsNumberTableName
                    + "."
                    + DialerSearchLookupColumns.RAW_CONTACT_ID
                    + " AS "
                    + DialerSearch.RAW_CONTACT_ID
                    + ","
                    + dsNumberTableName
                    + "."
                    + DialerSearchLookupColumns.CALL_LOG_ID
                    + " AS "
                    + DialerSearch.CALL_LOG_ID
                    + ","
                    + dsNumberTableName
                    + "."
                    + DialerSearchLookupColumns.NORMALIZED_NAME
                    + " AS "
                    + DialerSearch.SEARCH_PHONE_NUMBER
                    + ","
                    + dsNumberTableName
                    + "."
                    + DialerSearchLookupColumns.SEARCH_DATA_OFFSETS
                    + "," // Add to calculate offset
                    + dsNumberTableName
                    + "."
                    + DialerSearchLookupColumns.NAME_TYPE
                    + "," // Add to calculate offset

                    + dsCallsTableName + "." + Calls.DATE + " AS " + DialerSearch.CALL_DATE + ","
                    + dsCallsTableName + "." + Calls.TYPE + " AS " + DialerSearch.CALL_TYPE + ","
                    + dsCallsTableName + "." + Calls.PHONE_ACCOUNT_ID + " AS "
                    + DialerSearch.PHONE_ACCOUNT_ID + "," + dsCallsTableName + "."
                    + Calls.PHONE_ACCOUNT_COMPONENT_NAME + " AS "
                    + DialerSearch.PHONE_ACCOUNT_COMPONENT_NAME + "," + dsCallsTableName + "."
                    + Calls.NUMBER_PRESENTATION + " AS " + DialerSearch.NUMBER_PRESENTATION + ","
                    + dsCallsTableName + "." + Calls.GEOCODED_LOCATION + " AS "
                    + DialerSearch.CALL_GEOCODED_LOCATION + ","

                    + dsDataViewName + "." + RawContacts.CONTACT_ID + " AS "
                    + DialerSearch.CONTACT_ID + "," + dsDataViewName + "."
                    + RawContacts.DISPLAY_NAME_PRIMARY + " AS " + DialerSearch.NAME + ","
                    + dsDataViewName + "." + RawContacts.DISPLAY_NAME_ALTERNATIVE + " AS "
                    + DialerSearch.NAME_ALTERNATIVE + "," + dsDataViewName + "." + Data.DATA2
                    + " AS " + DialerSearch.SEARCH_PHONE_TYPE + "," + dsDataViewName + "."
                    + Data.DATA3 + " AS " + DialerSearch.SEARCH_PHONE_LABEL + "," + dsDataViewName
                    + "." + Contacts.LOOKUP_KEY + " AS " + DialerSearch.CONTACT_NAME_LOOKUP + ","
                    + dsDataViewName + "." + Contacts.PHOTO_ID + " AS " + DialerSearch.PHOTO_ID
                    + "," + dsDataViewName + "." + Contacts.STARRED + " AS "
                    + DialerSearch.CONTACT_STARRED + "," + dsDataViewName + "."
                    + Contacts.TIMES_CONTACTED + " AS " + DialerSearchLookupColumns.TIMES_USED
                    + "," + dsDataViewName + "." + Contacts.INDICATE_PHONE_SIM + " AS "
                    + DialerSearch.INDICATE_PHONE_SIM + "," + dsDataViewName + "."
                    + Contacts.IS_SDN_CONTACT + " AS " + DialerSearch.IS_SDN_CONTACT + ","
                    + dsDataViewName + "." + RawContacts.SORT_KEY_PRIMARY + " AS "
                    + DialerSearch.SORT_KEY_PRIMARY + "," + dsDataViewName + "."
                    + RawContacts.SORT_KEY_ALTERNATIVE + " AS " + DialerSearch.SORT_KEY_ALTERNATIVE

                    + " FROM (SELECT * FROM " + Tables.DIALER_SEARCH + " WHERE "
                    + DialerSearchLookupColumns.NAME_TYPE + " = "
                    + DialerSearchLookupType.PHONE_EXACT + " AND (" + rawContactsSelect + ")"
                    + " ) AS " + dsNumberTableName + " LEFT JOIN " + dsCallsTable + " ON "
                    + dsCallsTableName + "." + Calls._ID + " = " + dsNumberTableName + "."
                    + DialerSearchLookupColumns.CALL_LOG_ID + " LEFT JOIN " + dsDataView + " ON "
                    + dsDataViewName + "." + Data._ID + " = " + dsNumberTableName + "."
                    + DialerSearchLookupColumns.DATA_ID;

            db.execSQL("CREATE TEMP TABLE " + DIALER_SEARCH_TEMP_TABLE + " AS " + dsViewSelect);
        }
    }

    /**
     * M: create contacts triggers for dialer search.
     * @param db db
     */
    public static void createContactsTriggersForDialerSearch(SQLiteDatabase db) {
        /** Trigger cannot cause cache to update, replaced by java code*/
        db.execSQL("DROP TRIGGER IF EXISTS " + Tables.AGGREGATION_EXCEPTIONS + "_splite_contacts");
        /** db.execSQL("CREATE TRIGGER " + Tables.AGGREGATION_EXCEPTIONS
                + "_splite_contacts AFTER INSERT ON " + Tables.AGGREGATION_EXCEPTIONS
                + " BEGIN "
                + "   UPDATE " + Tables.DIALER_SEARCH
                + "     SET " + DialerSearchLookupColumns.RAW_CONTACT_ID + "="
                                + "(SELECT " + DialerSearchLookupColumns.RAW_CONTACT_ID
                                + " FROM " + Tables.DATA +
                                " WHERE " + Tables.DATA + "." + Data._ID
                                + "=" + Tables.DIALER_SEARCH
                                + "." + DialerSearchLookupColumns.DATA_ID + ")"
                + "   WHERE " + DialerSearchLookupColumns.RAW_CONTACT_ID
                                + " IN (" + "NEW." + AggregationExceptions.RAW_CONTACT_ID1
                                    + ",NEW." + AggregationExceptions.RAW_CONTACT_ID2 + ")"
                                + " AND " + DialerSearchLookupColumns.IS_VISIABLE + "=1"
                                + " AND " + DialerSearchLookupColumns.NAME_TYPE
                                    + "=" + DialerSearchLookupType.PHONE_EXACT
                                + " AND " + "NEW." + AggregationExceptions.TYPE + "=2;"

                + "   UPDATE " + Tables.DIALER_SEARCH
                + "     SET " + DialerSearchLookupColumns.IS_VISIABLE + "=1"
                + "   WHERE " + DialerSearchLookupColumns.RAW_CONTACT_ID
                                + " IN (" + "NEW." + AggregationExceptions.RAW_CONTACT_ID1
                                    + ",NEW." + AggregationExceptions.RAW_CONTACT_ID2 + ")"
                                + " AND " + DialerSearchLookupColumns.IS_VISIABLE + "=0"
                                + " AND " + DialerSearchLookupColumns.NAME_TYPE
                                    + "=" + DialerSearchLookupType.NAME_EXACT
                                + " AND " + "NEW." + AggregationExceptions.TYPE + "=2"
                                + ";"
                + " END");
          */
    }

    SQLiteStatement mUpdateNameWhenContactsUpdated;
    private void updateNameValueForContactUpdated(SQLiteDatabase db, long rawContactId,
            String displayNamePrimary, String displayNameAlternative) {
        if (mUpdateNameWhenContactsUpdated == null) {
            mUpdateNameWhenContactsUpdated = db.compileStatement("UPDATE "
                    + Tables.DIALER_SEARCH + " SET "
                    + DialerSearchLookupColumns.NORMALIZED_NAME + "=?,"
                    + DialerSearchLookupColumns.SEARCH_DATA_OFFSETS + "=?,"
                    + DialerSearchLookupColumns.NORMALIZED_NAME_ALTERNATIVE + "=?,"
                    + DialerSearchLookupColumns.SEARCH_DATA_OFFSETS_ALTERNATIVE + "=?"
                    + " WHERE " + DialerSearchLookupColumns.RAW_CONTACT_ID + "=? AND "
                    + DialerSearchLookupColumns.NAME_TYPE + "="
                    + DialerSearchLookupType.NAME_EXACT);
        }

        StringBuilder dialerSearchNameOffsets = new StringBuilder();
        String normalizedDialerSearchName = HanziToPinyin.getInstance()
                .getTokensForDialerSearch(displayNamePrimary, dialerSearchNameOffsets);
        StringBuilder dialerSearchNameOffsetsAlt = new StringBuilder();
        String normalizedDialerSearchNameAlt = HanziToPinyin.getInstance()
                .getTokensForDialerSearch(displayNameAlternative, dialerSearchNameOffsetsAlt);

        bindStringOrNull(mUpdateNameWhenContactsUpdated, 1, normalizedDialerSearchName);
        bindStringOrNull(mUpdateNameWhenContactsUpdated, 2, dialerSearchNameOffsets.toString());
        bindStringOrNull(mUpdateNameWhenContactsUpdated, 3, normalizedDialerSearchNameAlt);
        bindStringOrNull(mUpdateNameWhenContactsUpdated, 4, dialerSearchNameOffsetsAlt.toString());
        mUpdateNameWhenContactsUpdated.bindLong(5, rawContactId);
        mUpdateNameWhenContactsUpdated.execute();
    }

    SQLiteStatement mUpdateCallableWhenContactsUpdated;

    private void updateCallableValueForContactUpdated(SQLiteDatabase db, long dataId,
            String normalizedName) {
        if (mUpdateCallableWhenContactsUpdated == null) {
            mUpdateCallableWhenContactsUpdated = db.compileStatement("UPDATE "
                    + Tables.DIALER_SEARCH + " SET " + DialerSearchLookupColumns.NORMALIZED_NAME
                    + " = ?, " + DialerSearchLookupColumns.NORMALIZED_NAME_ALTERNATIVE + " = ?"
                    + " WHERE " + DialerSearchLookupColumns.DATA_ID + " = ?");
        }
        mUpdateCallableWhenContactsUpdated.bindString(1, normalizedName);
        mUpdateCallableWhenContactsUpdated.bindString(2, normalizedName);
        mUpdateCallableWhenContactsUpdated.bindLong(3, dataId);
        mUpdateCallableWhenContactsUpdated.execute();
    }

    private SQLiteStatement mUpdateNameVisibleForContactsJoinOrSplit;
    private SQLiteStatement mUpdateNumberNameRawIdForContactsJoinOrSplit;

    /**
     * M: handle Contacts Join Or Split.
     * @param db db
     */
    public void handleContactsJoinOrSplit(SQLiteDatabase db) {
        try {
            if (mUpdateNameVisibleForContactsJoinOrSplit == null) {
                mUpdateNameVisibleForContactsJoinOrSplit = db.compileStatement(
                        "UPDATE "
                        + Tables.DIALER_SEARCH + " SET " + DialerSearchLookupColumns.IS_VISIABLE
                        + "=" + "(CASE WHEN " + Tables.DIALER_SEARCH + "."
                        + DialerSearchLookupColumns.RAW_CONTACT_ID + " IN (SELECT "
                        + Contacts.NAME_RAW_CONTACT_ID + " FROM " + Tables.CONTACTS
                        + ") THEN 1 ELSE 0 END)"

                        + " WHERE " + DialerSearchLookupColumns.NAME_TYPE + "="
                        + DialerSearchLookupType.NAME_EXACT + ";");
            }

            if (mUpdateNumberNameRawIdForContactsJoinOrSplit == null) {
                mUpdateNumberNameRawIdForContactsJoinOrSplit = db.compileStatement("UPDATE "
                        + Tables.DIALER_SEARCH + " SET " + DialerSearchLookupColumns.RAW_CONTACT_ID
                        + "=" + "(SELECT " + Contacts.NAME_RAW_CONTACT_ID + " FROM " + Tables.DATA
                        + " JOIN " + Tables.RAW_CONTACTS + " ON ("
                        + DataColumns.CONCRETE_RAW_CONTACT_ID + "="
                        + RawContactsColumns.CONCRETE_ID + ")" + " JOIN " + Tables.CONTACTS
                        + " ON (" + RawContactsColumns.CONCRETE_CONTACT_ID + "="
                        + ContactsColumns.CONCRETE_ID + ")"

                        + " WHERE " + Tables.DATA + "." + Data._ID + "=" + Tables.DIALER_SEARCH
                        + "." + DialerSearchLookupColumns.DATA_ID + ")"

                        + " WHERE " + DialerSearchLookupColumns.NAME_TYPE + "="
                        + DialerSearchLookupType.PHONE_EXACT + " AND "
                        + DialerSearchLookupColumns.RAW_CONTACT_ID + " in (SELECT "
                        + AggregationExceptions.RAW_CONTACT_ID1 + " as "
                        + DialerSearchLookupColumns.RAW_CONTACT_ID + " FROM "
                        + Tables.AGGREGATION_EXCEPTIONS + " UNION SELECT "
                        + AggregationExceptions.RAW_CONTACT_ID2 + " as "
                        + DialerSearchLookupColumns.RAW_CONTACT_ID + " FROM "
                        + Tables.AGGREGATION_EXCEPTIONS + ")");
            }

            mUpdateNameVisibleForContactsJoinOrSplit.execute();
            mUpdateNumberNameRawIdForContactsJoinOrSplit.execute();

        } catch (SQLiteException e) {
            LogUtils.e(TAG, "handleContactsJoinOrSplit: " + e);
        }
        updateDialerSearchCache();
    }

    public static Cursor queryPhoneLookupByNumber(SQLiteDatabase db,
            ContactsDatabaseHelper dbHelper, String number, String[] projection, String selection,
            String[] selectionArgs, String groupBy, String having, String sortOrder, String limit) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String numberE164 = PhoneNumberUtils.formatNumberToE164(number,
                dbHelper.getCurrentCountryIso());
        String normalizedNumber = PhoneNumberUtils.normalizeNumber(number);
        dbHelper.buildPhoneLookupAndContactQuery(qb, normalizedNumber, numberE164);
        qb.setStrict(true);
        boolean foundResult = false;
        Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy, having,
                sortOrder, limit);
        try {
            if (c.getCount() > 0) {
                foundResult = true;
                return c;
            } else {
                qb = new SQLiteQueryBuilder();
                dbHelper.buildFallbackPhoneLookupAndContactQuery(qb, normalizedNumber);
                qb.setStrict(true);
            }
        } finally {
            if (!foundResult) {
                // We'll be returning a different cursor, so close this one.
                c.close();
            }
        }
        return qb.query(db, projection, selection, selectionArgs, groupBy, having,
                sortOrder, limit);
    }

    private static void createDsTempTableZero2Nine(SQLiteDatabase db) {
        long begin = System.currentTimeMillis();

        try {
            for (int i = 0; i <= 9; i++) {
                String filterNum = String.valueOf(i);
                String cachedOffsetsTempTable = sCachedOffsetsTempTableHead + filterNum;
                String offsetsSelect = createCacheOffsetsTableSelect(filterNum);

                db.execSQL("CREATE TEMP TABLE IF NOT EXISTS " + cachedOffsetsTempTable + " AS "
                        + offsetsSelect);
            }
        } catch (SQLiteException e) {
            LogUtils.w(TAG, "createDsTempTableZero2Nine SQLiteException:" + e);
        }

        long end = System.currentTimeMillis();
        LogUtils.d(TAG, "[createDsTempTableZero2Nine] create cache table cost:" + (end - begin));
    }

    /**
     * Drop dialer search view, it is out of date.
     * @param db the writable database
     */
    public static void dropDialerSearchView(SQLiteDatabase db) {
        if (ContactsProviderUtils.isSearchDbSupport()) {
            db.execSQL("DROP VIEW IF EXISTS " + Views.DIALER_SEARCH_VIEW + ";");
        }
    }

    private static void removeDsTempTableZero2Nine(SQLiteDatabase db) {
        long begin = System.currentTimeMillis();

        try {
            for (int i = 0; i <= 9; i++) {
                String filterNum = String.valueOf(i);
                String cachedOffsetsTempTable = sCachedOffsetsTempTableHead + filterNum;
                db.execSQL("DROP TABLE IF EXISTS " + cachedOffsetsTempTable + ";");
            }
        } catch (SQLiteException e) {
            LogUtils.w(TAG, "removeDsTempTableZero2Nine SQLiteException:" + e);
        }

        long end = System.currentTimeMillis();
        LogUtils.d(TAG, "[removeDsTempTableZero2Nine] remove cache table cost:" + (end - begin));
    }

    private static String createCacheOffsetsTableSelect(String filterNum) {
        String offsetsSelect = " SELECT "
                + getOffsetsTempTableColumns(ContactsContract.Preferences.DISPLAY_ORDER_PRIMARY,
                        filterNum) + ","
                + RawContacts.CONTACT_ID
                + " FROM "
                + Tables.DIALER_SEARCH

                + " LEFT JOIN ("
                + "SELECT _id as raw_id, contact_id," + RawContacts.SORT_KEY_PRIMARY
                + " FROM " + Tables.RAW_CONTACTS + ") AS raw_contact_info ON "
                + "raw_contact_info.raw_id=" + DialerSearchLookupColumns.RAW_CONTACT_ID

                //visible
                + " WHERE " + DialerSearchLookupColumns.IS_VISIABLE + " = 1"

                //have number
                + " AND " + DialerSearchLookupColumns.RAW_CONTACT_ID + " IN ( SELECT "
                + DialerSearchLookupColumns.RAW_CONTACT_ID + " FROM " + Tables.DIALER_SEARCH
                + " WHERE " + DialerSearchLookupColumns.NAME_TYPE
                + "="
                + DialerSearchLookupType.PHONE_EXACT
                + ")"

                //name matched or number matched
                + " AND DIALER_SEARCH_MATCH_FILTER("
                + DialerSearchLookupColumns.NORMALIZED_NAME + ","
                + DialerSearchLookupColumns.SEARCH_DATA_OFFSETS + ","
                + DialerSearchLookupColumns.NAME_TYPE + ",'"
                + filterNum + "'" + ")"

                + " ORDER BY offset COLLATE MATCHTYPE DESC,"
                + DialerSearchLookupColumns.SORT_KEY + " COLLATE PHONEBOOK,"
                + DialerSearchLookupColumns.CALL_LOG_ID + " DESC";
        return offsetsSelect;
    }

    /**
     * This method should be called to sync info when contacts inserted.
     * when contacts data inserted, insert dialer_search row with the same data_id
     *
     * @param db the writable database
     * @param rawContactId raw contact id of the inserted data
     * @param dataId inserted contact's dataId
     * @param dataValue name/phone/sip/ims
     * @param mimeType mimeType of name/phone/sip/ims
     */
    public void handleContactsInserted(SQLiteDatabase db, long rawContactId, long dataId,
            String dataValue, String mimeType) {
        try {
            if (TextUtils.isEmpty(mimeType) || TextUtils.isEmpty(dataValue)) {
                LogUtils.w(TAG, "Cannot insert dialer search with empty mimeType or Value");
                return;
            }

            long nameType = DialerSearchLookupType.PHONE_EXACT;
            String normalizedName = dataValue;

            //for contacts, dialer only need contact info, so call_log_id=0
            int callLogId = 0;
            switch (mimeType) {
            case StructuredName.CONTENT_ITEM_TYPE:
                nameType = DialerSearchLookupType.NAME_EXACT;
                normalizedName = null; // Do not insert name here, will be updated later.
                break;

            case Phone.CONTENT_ITEM_TYPE:
                // Don't normalize Uri phone number
                if (!PhoneNumberUtils.isUriNumber(dataValue)) {
                    normalizedName = DialerSearchUtils.stripSpecialCharInNumber(dataValue);
                }
            // fall-through
            case SipAddress.CONTENT_ITEM_TYPE:
            case ImsCall.CONTENT_ITEM_TYPE:
                break;

            default:
                return;
            }

            insertDialerSearchNewRecord(db, rawContactId, dataId, normalizedName, nameType,
                    callLogId);

        } catch (SQLiteException e) {
            LogUtils.e(TAG, "handleContactsInserted: " + e);
        }
        updateDialerSearchCache();
    }

    /**
     * This method should be called to sync info when contacts deleted.
     * delete dialer search rows which is not in raw_contacts table
     *
     * @param db the writable database
     */
    public void handleContactsDeleted (SQLiteDatabase db) {
        try {
            removeDeletedRecords(db);
        } catch (SQLiteException e) {
            LogUtils.e(TAG, "handleContactsDeleted: " + e);
        }
    }

    /**
     * This method should be called to sync info when contacts updated
     * a) when contacts data updated, update dialer_search rows with the same data_id
     * b) when contacts display name updated, update dialer search name
     *
     * @param db the writable database
     * @param rawContactId the updated rawcontactId
     * @param dataId data Id
     * @param dataValue data Value
     * @param dataValueAlt data Value Alt
     * @param mimeType mimeType
     */
    public void handleContactsUpdated (SQLiteDatabase db, long rawContactId,
            long dataId, String dataValue, String dataValueAlt, String mimeType) {
        try {
            if (TextUtils.isEmpty(mimeType) || TextUtils.isEmpty(dataValue)) {
                LogUtils.w(TAG, "Cannot insert dialer search with empty mimeType or Value");
                return;
            }

            String normalizedName = dataValue;
            switch (mimeType) {
            case StructuredName.CONTENT_ITEM_TYPE:
                updateNameValueForContactUpdated(db, rawContactId, dataValue, dataValueAlt);
                break;

            case Phone.CONTENT_ITEM_TYPE:
                // Don't normalize Uri phone number
                if (!PhoneNumberUtils.isUriNumber(dataValue)) {
                    normalizedName = DialerSearchUtils.stripSpecialCharInNumber(dataValue);
                }
            // fall-through
            case SipAddress.CONTENT_ITEM_TYPE:
            case ImsCall.CONTENT_ITEM_TYPE:
                updateCallableValueForContactUpdated(db, dataId, normalizedName);
                break;

            default:
                return;
            }
        } catch (SQLiteException e) {
            LogUtils.e(TAG, "handleContactsUpdated: " + e);
        }

        updateDialerSearchCache();
    }

    /**
     * This method should be called to sync info when call logs inserted.
     * insert dialer_search rows when no contact has same number
     *
     * @param db the writable database
     * @param callLogId the inserted call log's id
     */
    public void handleCallLogsInserted(SQLiteDatabase db, long callLogId, String callable) {
        if (TextUtils.isEmpty(callable) || callLogId < 0) {
            LogUtils.w(TAG, "Cannot insert dialer search with empty callable");
            return;
        }

        try {
            insertCallsOnlyRecords(db);
        } catch (SQLiteException e) {
            LogUtils.e(TAG, "handleCallLogsInserted: " + e);
        }

        updateDialerSearchCache();
        notifyDialerSearchChanged();
    }

    /**
     * This method should be called to sync info when call logs deleted.
     *
     * @param db the writable database
     */
    public void handleCallLogsDeleted(SQLiteDatabase db) {
        try {
            removeDeletedRecords(db);
            // for ALPS02274846, refresh the calls only dialer_search info.
            insertCallsOnlyRecords(db);
        } catch (SQLiteException e) {
            LogUtils.e(TAG, "handleCallLogsDeleted: " + e);
        }
        updateDialerSearchCache();
        notifyDialerSearchChanged();
    }

    /**
     * when contacts changed, call logs updated, remove/insert calls only data.
     *
     * @param db the writable database
     * @param isUpdatedByContactsRemoved call log updated is caused by contacts updated/deleted
     */
    public void handleCallLogsUpdated(SQLiteDatabase db, boolean isUpdatedByContactsRemoved) {
        try {
            removeContactCalls(db);
            if (isUpdatedByContactsRemoved) {
                insertCallsOnlyRecords(db);
            }
        } catch (SQLiteException e) {
            LogUtils.e(TAG, "handleCallLogsUpdated: " + e);
        }
        updateDialerSearchCache();
        notifyDialerSearchChanged();
    }

    /**
     * This method should be called to sync info when contacts data deleted.
     * delete dialer search rows which is not in data table
     *
     * @param db the writable database
     */
    public void handleDataDeleted (SQLiteDatabase db) {
        try {
            removeDeletedRecords(db);
        } catch (SQLiteException e) {
            LogUtils.e(TAG, "handleDataDeleted: " + e);
        }
    }

    /**
     * M: query by uri.
     * @param db db
     * @param uri uri
     * @return
     */
    public Cursor query(SQLiteDatabase db, Uri uri) {
        String filterParam = uri.getLastPathSegment();
        if (filterParam.isEmpty()) {
            LogUtils.d(TAG, "DialerSearch Uri with empty filter: " + uri);
            return new MatrixCursor(DialerSearchQuery.COLUMNS);
        }

        String displayOrder = uri.getQueryParameter(Preferences.DISPLAY_ORDER);
        String sortOrder = uri.getQueryParameter(Preferences.SORT_ORDER);
        if (!TextUtils.isEmpty(displayOrder) && !TextUtils.isEmpty(sortOrder)) {
            mDisplayOrder = Integer.parseInt(displayOrder);
            mSortOrder = Integer.parseInt(sortOrder);
        }
        LogUtils.d(TAG, "MTK-DialerSearch, handleDialerSearchExQuery begin. filterParam:"
                + filterParam);

        long begin = System.currentTimeMillis();
        Cursor cursor = null;

        String offsetsSelectedTable = "selected_offsets_temp_table";
        int firstNum = -1;

        try {
            firstNum = Integer.parseInt(String.valueOf(filterParam.charAt(0)));
        } catch (NumberFormatException e) {
            LogUtils.d(TAG, "MTK-DialerSearch, Cannot Parse as Int:" + filterParam);
        }

        try {
            db.beginTransaction();

            LogUtils.d(TAG, "handleDialerSearchQueryEx, mIsCached:" + mIsCached);

            String currentRawContactsSelect = DialerSearchLookupColumns.RAW_CONTACT_ID
                    + " in (SELECT _id FROM " + Tables.RAW_CONTACTS + ")";
            String currentCallsSelect = DialerSearchLookupColumns.CALL_LOG_ID
                    + " in (SELECT _id FROM " + Tables.CALLS + ")";
            String currentSelect = "(" + currentCallsSelect + ") OR (" + currentRawContactsSelect
                    + ")";

            // Only when first char is number and contacts preference is primary,
            // can using cached table to enhance dialer search performance.
            if (firstNum >= 0 && firstNum <= 9 && mIsCached
                    && mDisplayOrder == ContactsContract.Preferences.DISPLAY_ORDER_PRIMARY
                    && mSortOrder == ContactsContract.Preferences.SORT_ORDER_PRIMARY) {
                String cachedOffsetsTable = sCachedOffsetsTempTableHead + firstNum;
                String offsetsSelect = null;

                // CREATE TEMP TABLE called in transaction, Cannot be used next time.
                db.execSQL("CREATE TEMP TABLE IF NOT EXISTS " + cachedOffsetsTable
                        + " AS " + createCacheOffsetsTableSelect(String.valueOf(firstNum)));

                // If length of user input(e.g: 1) is 1, then select the first
                // 150 data from cached table directly;
                // Otherwise choose the data matched the user input(e.g: 123)
                // base on the cached table.
                if (filterParam.length() == 1) {
                    offsetsSelect = "SELECT * FROM " + cachedOffsetsTable + " WHERE "
                            + currentSelect + " LIMIT 150 ";
                } else {
                    offsetsSelect = " SELECT "
                        + getOffsetsTempTableColumns(
                                ContactsContract.Preferences.DISPLAY_ORDER_PRIMARY, filterParam)
                        + "," + RawContacts.CONTACT_ID
                        + " FROM " + cachedOffsetsTable
                        + " JOIN " + "(select "
                        + DialerSearchLookupColumns._ID + ","
                        + DialerSearchLookupColumns.NORMALIZED_NAME + ","
                        + DialerSearchLookupColumns.SEARCH_DATA_OFFSETS + " from "
                        + Tables.DIALER_SEARCH + ") AS ds_info " + " ON (_id = ds_id)"

                        + " WHERE (" + currentSelect + ") AND "
                        + "DIALER_SEARCH_MATCH_FILTER("
                        + DialerSearchLookupColumns.NORMALIZED_NAME + ","
                        + DialerSearchLookupColumns.SEARCH_DATA_OFFSETS + ","
                        + DialerSearchLookupColumns.NAME_TYPE + ",'"
                        + filterParam + "'" + ")"

                        + " ORDER BY offset COLLATE MATCHTYPE DESC, "
                        + DialerSearchLookupColumns.SORT_KEY + " COLLATE PHONEBOOK,"
                        + DialerSearchLookupColumns.CALL_LOG_ID + " DESC "
                        + " LIMIT 150";
                }
                db.execSQL("DROP TABLE IF EXISTS " + offsetsSelectedTable + ";");
                db.execSQL("CREATE TEMP TABLE " + offsetsSelectedTable + " AS " + offsetsSelect);
                long createTempTable1End = System.currentTimeMillis();
                LogUtils.d(TAG, "[handleDialerSearchQueryEx] create TEMP TABLE1 Cost:"
                        + (createTempTable1End - begin));
            } else {
                String offsetsTable = "ds_offsets_temp_table";
                String dsOffsetsTable = " SELECT "
                        + getOffsetsTempTableColumns(mDisplayOrder, filterParam)
                        + "," + RawContacts.CONTACT_ID
                        + " FROM " + Tables.DIALER_SEARCH

                        + " LEFT JOIN ("
                        + "SELECT _id as raw_id, contact_id,"
                        + ((mSortOrder == ContactsContract.Preferences.SORT_ORDER_ALTERNATIVE)
                                ? RawContacts.SORT_KEY_ALTERNATIVE + " AS "
                                + RawContacts.SORT_KEY_PRIMARY : RawContacts.SORT_KEY_PRIMARY)
                        + " FROM " + Tables.RAW_CONTACTS + ") AS raw_contact_info ON "
                        + "raw_contact_info.raw_id=" + DialerSearchLookupColumns.RAW_CONTACT_ID

                        + " WHERE " + DialerSearchLookupColumns.IS_VISIABLE + " = 1"
                        + " AND (" + currentSelect + ")"
                        + " AND " + DialerSearchLookupColumns.RAW_CONTACT_ID + " IN ( SELECT "
                        + DialerSearchLookupColumns.RAW_CONTACT_ID + " FROM "
                        + Tables.DIALER_SEARCH + " WHERE "
                        + DialerSearchLookupColumns.NAME_TYPE + "="
                        + DialerSearchLookupType.PHONE_EXACT + ")"
                        + " AND DIALER_SEARCH_MATCH_FILTER("
                        + DialerSearchLookupColumns.NORMALIZED_NAME + ","
                        + DialerSearchLookupColumns.SEARCH_DATA_OFFSETS + ","
                        + DialerSearchLookupColumns.NAME_TYPE + ",'"
                        + filterParam + "'" + ")";

                db.execSQL("DROP TABLE IF EXISTS " + offsetsTable + ";");
                db.execSQL("CREATE TEMP TABLE " + offsetsTable + " AS " + dsOffsetsTable);
                long createTempTable1End = System.currentTimeMillis();
                LogUtils.d(TAG, "[handleDialerSearchQueryEx] create TEMP TABLE1 Cost:"
                        + (createTempTable1End - begin));

                String offsetsSelect = "SELECT * FROM " + offsetsTable

                        + " ORDER BY offset COLLATE MATCHTYPE DESC, "
                        + DialerSearchLookupColumns.SORT_KEY + " COLLATE PHONEBOOK,"
                        + DialerSearchLookupColumns.CALL_LOG_ID + " DESC "
                        + " LIMIT 150 ";

                db.execSQL("DROP TABLE IF EXISTS " + offsetsSelectedTable + ";");
                db.execSQL("CREATE TEMP TABLE " + offsetsSelectedTable + " AS " + offsetsSelect);
                long createTempTable2End = System.currentTimeMillis();
                LogUtils.d(TAG, "[handleDialerSearchQueryEx] create TEMP TABLE2 Cost:"
                        + (createTempTable2End - createTempTable1End));
            }

            String contactsSelect = "contact_id IN (SELECT contact_id FROM " + offsetsSelectedTable
                    + ")";
            String rawContactsSelect = "raw_contact_id IN (SELECT raw_contact_id FROM "
                    + offsetsSelectedTable + ")";
            String calllogSelect = Calls._ID + " IN (SELECT call_log_id FROM "
                    + offsetsSelectedTable + ")";
            createDialerSearchTempTable(db, contactsSelect, rawContactsSelect, calllogSelect);

            long createTempTableEnd = System.currentTimeMillis();
            LogUtils.d(TAG, "[handleDialerSearchQueryEx] create TEMP TABLE Cost:"
                    + (createTempTableEnd - begin));

            String nameOffsets = "SELECT raw_contact_id as name_raw_id, offset as name_offset FROM "
                                    + offsetsSelectedTable
                                    + " WHERE name_type=" + DialerSearchLookupType.NAME_EXACT;

            String joinedOffsetTable = "SELECT"
                    + " ds_id, raw_contact_id, offset as offset_order, name_raw_id, name_type, "
                    + " (CASE WHEN " + DialerSearchLookupColumns.NAME_TYPE
                    + " = " + DialerSearchLookupType.PHONE_EXACT
                    + " THEN offset ELSE NULL END) AS " + DialerSearch.MATCHED_DATA_OFFSET + ", "
                    + " (CASE WHEN " + DialerSearchLookupColumns.NAME_TYPE + " = "
                    + DialerSearchLookupType.NAME_EXACT
                    + " THEN offset ELSE name_offset END) AS " + DialerSearch.MATCHED_NAME_OFFSET

                    + " FROM "
                    + offsetsSelectedTable
                    + " LEFT JOIN (" + nameOffsets + ") AS name_offsets"
                    + " ON (" + offsetsSelectedTable
                    + ".name_type=" + DialerSearchLookupType.PHONE_EXACT
                    + " AND " + offsetsSelectedTable + ".raw_contact_id=name_offsets.name_raw_id)";

            cursor = db.rawQuery("SELECT "
                  + getDialerSearchResultColumns(mDisplayOrder, mSortOrder)
                  + ", offset_order"
                  + ", " + DialerSearchLookupColumns.TIMES_USED

                  + " FROM "
                  + " (" + joinedOffsetTable + " ) AS offset_table"
                  + " JOIN "
                  + DIALER_SEARCH_TEMP_TABLE
                  + " ON (" + DIALER_SEARCH_TEMP_TABLE + "."
                  + DialerSearch._ID + "=offset_table.ds_id)"
                  + " OR ( offset_table.name_type="
                  + DialerSearchLookupType.NAME_EXACT + " AND "
                  + DIALER_SEARCH_TEMP_TABLE + "."
                  + DialerSearch.RAW_CONTACT_ID + "=offset_table.raw_contact_id )"

                    + " WHERE NOT" + "( offset_table.name_type="
                    + DialerSearchLookupType.NAME_EXACT + " AND "
                    + "_id IN (SELECT ds_id as _id FROM " + offsetsSelectedTable
                    + " WHERE name_type=" + DialerSearchLookupType.PHONE_EXACT + ") )"

                    + " ORDER BY " + DialerSearch.MATCHED_NAME_OFFSET + " COLLATE MATCHTYPE DESC,"
                    + DialerSearch.MATCHED_DATA_OFFSET + " COLLATE MATCHTYPE DESC,"
                  + DialerSearchLookupColumns.TIMES_USED + " DESC,"
                  + DialerSearch.SORT_KEY_PRIMARY + " COLLATE PHONEBOOK,"
                  + DialerSearch.CALL_LOG_ID + " DESC ", null);

            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            LogUtils.w(TAG, "handleDialerSearchQueryEx SQLiteException:" + e);
        } finally {
            db.endTransaction();
        }

        if (cursor == null) {
            LogUtils.d(TAG, "DialerSearch Cusor is null, Uri: " + uri);
            cursor = new MatrixCursor(DialerSearchQuery.COLUMNS);
        }

        return cursor;
    }

    private String getDialerSearchResultColumns(int displayOrder, int sortOrder) {
        StringBuilder sb = new StringBuilder();
        sb.append(DialerSearch.NAME_LOOKUP_ID + ","
                  + DialerSearch.CONTACT_ID + ","
                  + DialerSearchLookupColumns.DATA_ID + ","
                  + DialerSearch.CALL_DATE + ","
                  + DialerSearch.CALL_LOG_ID + ","
                  + DialerSearch.CALL_TYPE + ","
                  + DialerSearch.CALL_GEOCODED_LOCATION + ","
                  + DialerSearch.PHONE_ACCOUNT_ID + ","
                  + DialerSearch.PHONE_ACCOUNT_COMPONENT_NAME + ","
                  + DialerSearch.NUMBER_PRESENTATION + ","
                  + DialerSearch.INDICATE_PHONE_SIM + ","
                  + DialerSearch.CONTACT_STARRED + ","
                  + DialerSearch.PHOTO_ID + ","
                  + DialerSearch.SEARCH_PHONE_TYPE + ","
                  + DialerSearch.SEARCH_PHONE_LABEL + ","
                  + ((displayOrder == ContactsContract.Preferences.DISPLAY_ORDER_ALTERNATIVE)
                          ? DialerSearch.NAME_ALTERNATIVE + " AS "
                          + DialerSearch.NAME : DialerSearch.NAME) + ","
                  + DialerSearch.SEARCH_PHONE_NUMBER + ","
                  + DialerSearch.CONTACT_NAME_LOOKUP + ","
                  + DialerSearch.IS_SDN_CONTACT + ","
                  + DialerSearch.MATCHED_DATA_OFFSET + ","
                  + DialerSearch.MATCHED_NAME_OFFSET);

        sb.append(",").append((sortOrder == ContactsContract.Preferences.SORT_ORDER_ALTERNATIVE)
                ? DialerSearch.SORT_KEY_ALTERNATIVE + " AS "
                + DialerSearch.SORT_KEY_PRIMARY : DialerSearch.SORT_KEY_PRIMARY);

        return sb.toString();
    }

    private static String getOffsetsTempTableColumns(int displayOrder, String filterParam) {
        StringBuilder builder = new StringBuilder();

        builder.append(DialerSearchLookupColumns._ID + " AS ds_id");
        builder.append(", ");
        builder.append(DialerSearchLookupColumns.RAW_CONTACT_ID);
        builder.append(", ");
        builder.append(DialerSearchLookupColumns.CALL_LOG_ID);
        builder.append(", ");
        builder.append(DialerSearchLookupColumns.NAME_TYPE);
        builder.append(", ");
        builder.append(RawContacts.SORT_KEY_PRIMARY + " AS " + DialerSearchLookupColumns.SORT_KEY);
        builder.append(", ");
        builder.append(getOffsetColumn(displayOrder, filterParam) + " AS " + "offset");

        return builder.toString();
    }

    private static String getOffsetColumn(int displayOrder, String filterParam) {
        String searchParamList = "";
        if (displayOrder == ContactsContract.Preferences.DISPLAY_ORDER_ALTERNATIVE) {
            searchParamList = DialerSearchLookupColumns.NORMALIZED_NAME_ALTERNATIVE
                    + "," + DialerSearchLookupColumns.SEARCH_DATA_OFFSETS_ALTERNATIVE
                    + "," + DialerSearchLookupColumns.NAME_TYPE
                    + ",'" + filterParam + "'";
        } else {
            searchParamList = DialerSearchLookupColumns.NORMALIZED_NAME
                    + "," + DialerSearchLookupColumns.SEARCH_DATA_OFFSETS
                    + "," + DialerSearchLookupColumns.NAME_TYPE
                    + ",'" + filterParam + "'";
        }
        return "DIALER_SEARCH_MATCH(" + searchParamList + ")";
    }

    private SQLiteStatement mDialerSearchNewRecordInsert;
    private void insertDialerSearchNewRecord(SQLiteDatabase db, long rawContactId,
            long dataId, String normalizedName, long nameType, long callLogId) {
        if (mDialerSearchNewRecordInsert == null) {
        mDialerSearchNewRecordInsert = db.compileStatement("INSERT INTO "
                    + Tables.DIALER_SEARCH + "("
                    + DialerSearchLookupColumns.RAW_CONTACT_ID + ","
                    + DialerSearchLookupColumns.DATA_ID + ","
                    + DialerSearchLookupColumns.NORMALIZED_NAME + ","
                    + DialerSearchLookupColumns.NAME_TYPE + ","
                    + DialerSearchLookupColumns.CALL_LOG_ID + ","
                    + DialerSearchLookupColumns.NORMALIZED_NAME_ALTERNATIVE + ")"
                    + " VALUES (?,?,?,?,?,?)");
        }

        mDialerSearchNewRecordInsert.bindLong(1, rawContactId);
        mDialerSearchNewRecordInsert.bindLong(2, dataId);
        bindStringOrNull(mDialerSearchNewRecordInsert, 3, normalizedName);
        mDialerSearchNewRecordInsert.bindLong(4, nameType);
        mDialerSearchNewRecordInsert.bindLong(5, callLogId);
        bindStringOrNull(mDialerSearchNewRecordInsert, 6, normalizedName);

        mDialerSearchNewRecordInsert.executeInsert();
    }

    private SQLiteStatement mCallsOnlyRecordsInserter;
    private void insertCallsOnlyRecords(SQLiteDatabase db) {
        // remove calls only first to avoid repeat data
        removeCallsOnly(db);

        if (mCallsOnlyRecordsInserter == null) {
            mCallsOnlyRecordsInserter = db.compileStatement("INSERT INTO " + Tables.DIALER_SEARCH
                    + " SELECT " + "NULL,0,0," + DialerSearchLookupType.PHONE_EXACT + ","
                    + Calls._ID + ",0,1," + Calls.NUMBER + ",NULL," + Calls.NUMBER + ",NULL FROM "
                    + Tables.CALLS + " WHERE (" + Calls.DATA_ID + " IS NULL OR " + Calls.DATA_ID
                    + "<=0 )"
                    // + " AND " + Calls._ID + " NOT IN (SELECT "
                    // + DialerSearchLookupColumns.CALL_LOG_ID + " FROM " +
                    // Tables.DIALER_SEARCH + ")"
                    + " GROUP BY " + Calls.NUMBER);
        }

        mCallsOnlyRecordsInserter.executeInsert();
    }

    private SQLiteStatement mRemoveContactCalls;
    private void removeContactCalls(SQLiteDatabase db) {
        if (mRemoveContactCalls == null) {
            mRemoveContactCalls = db.compileStatement("DELETE FROM " + Tables.DIALER_SEARCH
                    + " WHERE " + DialerSearchLookupColumns.CALL_LOG_ID + " IN (SELECT "
                    + Calls._ID + " FROM " + Tables.CALLS + " WHERE " + Calls.DATA_ID + ">0)");
        }
        mRemoveContactCalls.execute();
    }

    private SQLiteStatement mRemoveCallsOnly;
    private void removeCallsOnly(SQLiteDatabase db) {
        if (mRemoveCallsOnly == null) {
            mRemoveCallsOnly = db.compileStatement("DELETE FROM " + Tables.DIALER_SEARCH
                    + " WHERE " + DialerSearchLookupColumns.CALL_LOG_ID + ">0");
        }
        mRemoveCallsOnly.execute();
    }

    private void bindStringOrNull(SQLiteStatement stmt, int index, String value) {
        if (value == null) {
            stmt.bindNull(index);
        } else {
            stmt.bindString(index, value);
        }
    }

    private SQLiteStatement mRemoveDeletedRecords;
    private void removeDeletedRecords(SQLiteDatabase db) {
        if (mRemoveDeletedRecords == null) {
            mRemoveDeletedRecords = db.compileStatement("DELETE FROM "
                    + Tables.DIALER_SEARCH + " WHERE "
                    + "(" + DialerSearchLookupColumns.RAW_CONTACT_ID + " NOT IN ("
                            + " SELECT " + RawContacts._ID + " FROM " + Tables.RAW_CONTACTS
                            + " WHERE " + RawContacts.DELETED + "=0" + ")" + " OR "
                    + DialerSearchLookupColumns.DATA_ID + " NOT IN ("
                            + " SELECT " + Data._ID + " FROM " + Tables.DATA + "))" + " AND "
                    + DialerSearchLookupColumns.CALL_LOG_ID + " NOT IN ("
                            + " SELECT " + Calls._ID + " FROM " + Tables.CALLS + ")");
        }

        mRemoveDeletedRecords.execute();
    }

    private void removeSameNumberCallsOnlyRecords(SQLiteDatabase db, String callable) {
        SQLiteStatement removeNakedCallLogRecords = db.compileStatement("DELETE FROM "
                    + Tables.DIALER_SEARCH + " WHERE ("
                    + DialerSearchLookupColumns.NAME_TYPE + "=" + DialerSearchLookupType.PHONE_EXACT
                    + " AND " + DialerSearchLookupColumns.RAW_CONTACT_ID + "<0"
                    + " AND " + buildDialerSearchSameNumberFilter(callable)
                    + ");");

        removeNakedCallLogRecords.execute();
        removeNakedCallLogRecords.close();
    }

    private static String buildDialerSearchSameNumberFilter(String callable) {
        boolean bIsUriNumber = PhoneNumberUtils.isUriNumber(callable);
        String filter = null;
        if (bIsUriNumber) {
            filter = DialerSearchLookupColumns.NORMALIZED_NAME + "='" + callable + "'";
        } else {
            filter = "PHONE_NUMBERS_EQUAL("
                    + DialerSearchLookupColumns.NORMALIZED_NAME + ", '" + callable + "')";
        }
        return filter;
    }

    private void scheduleBackgroundTask(int task) {
        mBackgroundHandler.sendEmptyMessage(task);
    }

    private void scheduleBackgroundTask(int task, long delayMillus) {
        mBackgroundHandler.sendEmptyMessageDelayed(task, delayMillus);
    }

    private void updateDialerSearchCache() {
        mBackgroundHandler.removeMessages(BACKGROUND_TASK_CREATE_CACHE);
        LogUtils.d(TAG, "updateDialerSearchCache, mIsCached:" + mIsCached);
        if (mIsCached) {
            mIsCached = false; //disable cache before caches removed when data changed.
            scheduleBackgroundTask(BACKGROUND_TASK_REMOVE_CACHE);
        }
        scheduleBackgroundTask(BACKGROUND_TASK_CREATE_CACHE, CACHE_TASK_DELAY_MILLIS);
    }

    private void notifyDialerSearchChanged() {
        mContext.getContentResolver().notifyChange(
                ContactsContract.AUTHORITY_URI.buildUpon().appendPath("dialer_search")
                        .appendPath("call_log").build(), null, false);
    }
}
