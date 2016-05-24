package com.mediatek.providers.contacts;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.android.providers.contacts.ContactsDatabaseHelper;

/**
 * M: Add for dialer search.
 */
public class DialerSearchUtils {

    private static final String TAG = "DialerSearchUtils";

    /**
     * M: compute Normalized Number.
     * @param number number
     * @return number
     */
    public static String computeNormalizedNumber(String number) {
        String normalizedNumber = null;
        if (number != null) {
            normalizedNumber = PhoneNumberUtils.getStrippedReversed(number);
        }
        return normalizedNumber;
    }

    /**
     * M: strip special char in the number.
     * @param number number
     * @return number
     */
    public static String stripSpecialCharInNumber(String number) {
        if (number == null) {
            return null;
        }
        int len = number.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            char c = number.charAt(i);

            /// M:fix CR ALPS00758625, need to consider 'p' or 'w', not to filter these character.
            /// These character will be valid as phone number.
            if (PhoneNumberUtils.isNonSeparator(c) || c == 'p' || c == 'w' || c == 'P'
                    || c == 'W') {
                sb.append(c);
            } else if (c == ' ' || c == '-' || c == '(' || c == ')') {
                // strip blank and hyphen
            } else {
                /*
                 * Bug fix by Mediatek begin
                 * CR ID: ALPS00293790
                 * Description:
                 * Original Code: break;
                 */
                continue;
                /*
                 * Bug fix by Mediatek end
                 */
            }
        }
        return sb.toString();
    }

    /**
     * M: query PhoneLookup By Number.
     * @param db
     * @param dbHelper
     * @param number
     * @param projection
     * @param selection
     * @param selectionArgs
     * @param groupBy
     * @param having
     * @param sortOrder
     * @param limit
     * @return
     */
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
        Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy, having, sortOrder,
                limit);
        try {
            if (c.getCount() > 0) {
                foundResult = true;
                Cursor exactCursor = findNumberExactMatchedCursor(c, normalizedNumber);
                if (exactCursor != null) {
                    Log.i(TAG,
                            "queryPhoneLookupByNumber: has found the exact number match Contact!");
                    c.close();
                    return exactCursor;
                }
                return c;
            } else {
                qb = new SQLiteQueryBuilder();
                //dbHelper.buildMinimalPhoneLookupAndContactQuery(qb, normalizedNumber);
                // use the raw number instead of the normalized number because
                // phone_number_compare_loose in SQLite works only with non-normalized
                // numbers
                dbHelper.buildFallbackPhoneLookupAndContactQuery(qb, number);
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

    /**
     * To find out the exactly number which has a same normalizedNumber with the input number.
     * @param cursor
     *        origin Cursor
     * @param normalizedNumber
     *        the input number's normalizedNumber
     * @return
     *        the exactly number matched contacts cursor
     */
    public static Cursor findNumberExactMatchedCursor(Cursor cursor, String normalizedNumber) {
        /**
         * M: [ALPS00565568]we need to find out the right "Phone.NUMBER" in case
         * there are more than record in the cursor. the cursor which would be
         * handled must contain "Phone.NUMBER" field && count > 1 &&
         * normalizedNumber is valid. otherwise, we would do nothing and return
         * null.
         */

        /// M: For ALPS01450219 @{
        // We need to check the PhoneLookup column as well.
        if (cursor == null
                || cursor.getCount() <= 1
                || TextUtils.isEmpty(normalizedNumber)
                || (cursor.getColumnIndex(Phone.NUMBER) < 0 && cursor
                        .getColumnIndex(PhoneLookup.NUMBER) < 0)) {
            Log.i(TAG, "findNumberExactMatchedCursor: did not match the filter rule!");
            return null;
        }
        /// @}

        String data1 = null;
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {

            /// M: For ALPS01450219 @{
            // We need to check the PhoneLookup column as well.
            int numberColumnIndex = cursor.getColumnIndex(Phone.NUMBER);
            if (numberColumnIndex < 0) {
                numberColumnIndex = cursor.getColumnIndex(PhoneLookup.NUMBER);
            }
            /// @}

            data1 = cursor.getString(numberColumnIndex);
            data1 = PhoneNumberUtils.normalizeNumber(data1);
            Log.i(TAG, "findNumberExactMatchedCursor: number = " + data1);
            Log.i(TAG, "findNumberExactMatchedCursor: normalizedNumber = " + normalizedNumber);
            if (normalizedNumber.equals(data1)) {
                MatrixCursor exactCursor = matrixCursorFromCursorRow(cursor, cursor.getPosition());
                cursor.close();
                return exactCursor;
            }
            cursor.moveToNext();
        }
        return null;
    }

    /**
     * Copy one cursor row to another cursor which has the same value.
     *
     * @param cursor
     *            the src cursor
     * @param index
     *            the row to convert
     * @return the new cursor with a single row
     */
    private static MatrixCursor matrixCursorFromCursorRow(Cursor cursor, int index) {
        MatrixCursor newCursor = new MatrixCursor(cursor.getColumnNames(), 1);
        int numColumns = cursor.getColumnCount();
        String data[] = new String[numColumns];
        if (-1 < index && index < cursor.getCount()) {
            cursor.moveToPosition(index);
        }
        for (int i = 0; i < numColumns; i++) {
            data[i] = cursor.getString(i);
        }
        newCursor.addRow(data);
        return newCursor;
    }
}
