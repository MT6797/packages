package com.mediatek.dialer.dialersearch;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Callable;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.DialerSearch;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.Log;

import com.android.contacts.common.preference.ContactsPreferences;
import com.google.common.base.Preconditions;

/**
 * M: [MTK Dialer Search] Data query helper class
 */
public class DialerSearchHelper {
    private static final String TAG = "DialerSearchHelper";
    private static DialerSearchHelper sSingleton = null;
    private final Context mContext;
    private static ContactsPreferences sContactsPrefs;

    // for ALPS01762713
    // workaround for code defect in ContactsPreferences
    public static void initContactsPreferences(Context context) {
        // trap in ContactsPreferences constructor , must be called in main tread
        sContactsPrefs = new ContactsPreferences(context);
    }

    private DialerSearchHelper(Context context) {
        mContext = Preconditions.checkNotNull(context, "Context must not be null");
        if (sContactsPrefs == null) {
            sContactsPrefs = new ContactsPreferences(context);
        }
    }

    /**
     * Access function to get the singleton instance of DialerDatabaseHelper.
     */
    public static synchronized DialerSearchHelper getInstance(Context context) {
        Log.d(TAG, "Getting Instance");

        if (sSingleton == null) {
            // Use application context instead of activity context because this
            // is a singleton,
            // and we don't want to leak the activity if the activity is not
            // running but the
            // dialer database helper is still doing work.
            sSingleton = new DialerSearchHelper(context.getApplicationContext());
        }
        return sSingleton;
    }

    /**
     * Query dialerSearch results from contactsProvider, use MTK algorithm.
     * @param query
     * @return DialerSearch result.
     */
    public Cursor getSmartDialerSearchResults(String query) {
        Log.d(TAG, "MTK-DialerSearch, getSmartDialerSearchResults, queryFilter: " + query);

        if (TextUtils.isEmpty(query)) {
            return null;
        }

        final ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;
        try {
            int displayOrder = sContactsPrefs.getDisplayOrder();
            int sortOrder = sContactsPrefs.getSortOrder();
            Uri baseUri = Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "dialer_search");
            Uri dialerSearchUri = baseUri.buildUpon().appendPath(query).build();
            Log.d(TAG, "MTK-DialerSearch, displayOrder: " + displayOrder + " ,sortOrder: "
                    + sortOrder);

            Uri dialerSearchParamUri = dialerSearchUri.buildUpon().appendQueryParameter(
                    ContactsContract.Preferences.DISPLAY_ORDER, String.valueOf(displayOrder))
                    .appendQueryParameter(ContactsContract.Preferences.SORT_ORDER,
                            String.valueOf(sortOrder)).build();

            cursor = resolver.query(dialerSearchParamUri, null, null, null, null);

            Log.d(TAG, "MTK-DialerSearch, cursor.getCount: " + cursor.getCount());

            return cursor;
        } catch (Exception e) {
            Log.w(TAG,
                    "Exception thrown in MTK-DialerSearch, getSmartDialerSearchResults", e);

            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
            return null;
        }
    }

    /**
     * Query dialerSearch results from contactsProvider, use google default algorithm.
     * @param query
     * @param useCallableUri, Similar to {@link Phone#CONTENT_URI}, but returns callable data
             * instead of only phone numbers.
     * @return DialerSearch results.
     */
    public Cursor getRegularDialerSearchResults(String query, boolean useCallableUri) {
        Log.d(TAG, "MTK-DialerSearch, getRegularDialerSearchResults, query: " + query);

        if (TextUtils.isEmpty(query)) {
            return null;
        }

        final ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;
        Cursor result = null;
        try {
            final Uri baseUri;
            if (useCallableUri) {
                baseUri = Callable.CONTENT_FILTER_URI;
            } else {
                baseUri = Phone.CONTENT_FILTER_URI;
            }

            final Builder builder = baseUri.buildUpon();
            builder.appendPath(query);
            builder.appendQueryParameter(ContactsContract.REMOVE_DUPLICATE_ENTRIES, "true");

            Uri regularDialerSearchUri = builder.build();

            final String[] projection;
            final String sortOrder;

            if (sContactsPrefs.getDisplayOrder()
                        == ContactsContract.Preferences.DISPLAY_ORDER_PRIMARY) {
                projection = PhoneQuery.PROJECTION_PRIMARY;
            } else {
                projection = PhoneQuery.PROJECTION_ALTERNATIVE;
            }

            if (sContactsPrefs.getSortOrder() == ContactsContract.Preferences.SORT_ORDER_PRIMARY) {
                sortOrder = Phone.SORT_KEY_PRIMARY;
            } else {
                sortOrder = Phone.SORT_KEY_ALTERNATIVE;
            }

            cursor = resolver.query(regularDialerSearchUri, projection, null, null, sortOrder);

            int cursorPos = 0;
            int count = 0;
            if (cursor != null) {
                Log.d(TAG, "MTK-DialerSearch, regularDialerSearch,cursor.getCount: "
                        + cursor.getCount());

                count = cursor.getCount();
                Object[][] objectMap = new Object[count][];

                while (cursor.moveToNext()) {
                    long contactId = cursor.getLong(cursor.getColumnIndex(Phone.CONTACT_ID));
                    long dataId = cursor.getLong(cursor.getColumnIndex(Phone._ID));
                    long photoId = cursor.getLong(cursor.getColumnIndex(Phone.PHOTO_ID));
                    int type = cursor.getInt(cursor.getColumnIndex(Phone.TYPE));
                    int numberLabel = cursor.getInt(cursor.getColumnIndex(Phone.LABEL));
                    String displayName = cursor.getString(cursor
                            .getColumnIndex(Phone.DISPLAY_NAME_PRIMARY));
                    String number = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));
                    String lookup = cursor.getString(cursor.getColumnIndex(Phone.LOOKUP_KEY));
                    int simIndicate = cursor.getInt(cursor
                            .getColumnIndex(RawContacts.INDICATE_PHONE_SIM));
                    int isSdn = cursor.getInt(cursor.getColumnIndex(RawContacts.IS_SDN_CONTACT));

                    objectMap[cursorPos++] = buildCursorRecord(0, contactId, dataId, null, 0, 0,
                            null, null, null, 0, simIndicate, 0, photoId, type, null, displayName,
                            number, lookup, isSdn, "true", query);
                }
                result = buildCursor(objectMap);
            }
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
            return result;
        } catch (Exception e) {
            Log.w(TAG,
                    "Exception thrown in MTK-DialerSearch, getRegularDialerSearchResults", e);

            if (cursor != null) {
                cursor.close();
                cursor = null;
            }

            if (result != null) {
                result.close();
                result = null;
            }
            return null;
        }
    }

    private static class PhoneQuery {
        public static final String[] PROJECTION_PRIMARY = new String[] {
            Phone._ID,                          // 0
            Phone.TYPE,                         // 1
            Phone.LABEL,                        // 2
            Phone.NUMBER,                       // 3
            Phone.CONTACT_ID,                   // 4
            Phone.LOOKUP_KEY,                   // 5
            Phone.PHOTO_ID,                     // 6
            Phone.DISPLAY_NAME_PRIMARY,         // 7
            Phone.PHOTO_THUMBNAIL_URI,          // 8

            RawContacts.INDICATE_PHONE_SIM,     // 9
            RawContacts.IS_SDN_CONTACT          // 10
        };

        public static final String[] PROJECTION_ALTERNATIVE = new String[] {
            Phone._ID,                          // 0
            Phone.TYPE,                         // 1
            Phone.LABEL,                        // 2
            Phone.NUMBER,                       // 3
            Phone.CONTACT_ID,                   // 4
            Phone.LOOKUP_KEY,                   // 5
            Phone.PHOTO_ID,                     // 6
            Phone.DISPLAY_NAME_ALTERNATIVE,     // 7
            Phone.PHOTO_THUMBNAIL_URI,          // 8

            RawContacts.INDICATE_PHONE_SIM,     // 9
            RawContacts.IS_SDN_CONTACT          // 10
        };
    }

    private Object[] buildCursorRecord(long id, long contactId, long dataId, String callDate,
            long callLogId, int callType, String geo, String phoneAccountId,
            String phoneAccountComponentName, int presentation, int simIndicator, int starred,
            long photoId, int numberType, String numberLabel, String name, String number,
            String lookup, int isSdn, String isRegularSearch, String nameOffset) {
        Object[] record = new Object[] { id, contactId, dataId, callDate, callLogId, callType, geo,
                phoneAccountId, phoneAccountComponentName, presentation, simIndicator, starred,
                photoId, numberType, numberLabel, name, number, lookup, isSdn, isRegularSearch,
                nameOffset };
        return record;
    }

    private Cursor buildCursor(Object[][] cursorValues) {
        MatrixCursor c = new MatrixCursor(DialerSearchQuery.COLUMNS);
        if (cursorValues != null) {
            for (Object[] record : cursorValues) {
                if (record == null) {
                    break;
                }
                c.addRow(record);
            }
        }
        return c;
    }

    public interface DialerSearchQuery {
        String[] COLUMNS = new String[] {
                DialerSearch.NAME_LOOKUP_ID,
                DialerSearch.CONTACT_ID,
                "data_id",
                DialerSearch.CALL_DATE,
                DialerSearch.CALL_LOG_ID,
                DialerSearch.CALL_TYPE,
                DialerSearch.CALL_GEOCODED_LOCATION,
                "phoneAccoutId",
                "phoneAccountComponentName",
                "presentation",
                DialerSearch.INDICATE_PHONE_SIM,
                DialerSearch.CONTACT_STARRED,
                DialerSearch.PHOTO_ID,
                DialerSearch.SEARCH_PHONE_TYPE,
                "numberLabel",
                DialerSearch.NAME,
                DialerSearch.SEARCH_PHONE_NUMBER,
                DialerSearch.CONTACT_NAME_LOOKUP,
                DialerSearch.IS_SDN_CONTACT,
                DialerSearch.MATCHED_DATA_OFFSET,
                DialerSearch.MATCHED_NAME_OFFSET
        };
    }
}
