package com.mediatek.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract.CommonDataKinds.ImsCall;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import com.android.providers.contacts.ContactsDatabaseHelper;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.ContactsDatabaseHelper.PhoneLookupColumns;
import com.android.providers.contacts.DataRowHandler;
import com.android.providers.contacts.TransactionContext;
import com.android.providers.contacts.DataRowHandler.DataDeleteQuery;
import com.android.providers.contacts.DataRowHandler.DataUpdateQuery;
import com.android.providers.contacts.SearchIndexManager.IndexBuilder;
import com.android.providers.contacts.aggregation.AbstractContactAggregator;

/**
 * Handler for note data rows.
 */
public class DataRowHandlerForImsCall extends DataRowHandler {
    private static final String TAG = "DataRowHandlerForImsCall";

    public DataRowHandlerForImsCall(Context context, ContactsDatabaseHelper dbHelper,
            AbstractContactAggregator aggregator) {
        super(context, dbHelper, aggregator, ImsCall.CONTENT_ITEM_TYPE);
    }

    @Override
    public boolean hasSearchableData() {
        return true;
    }

    @Override
    public boolean containsSearchableColumns(ContentValues values) {
        return values.containsKey(ImsCall.URL);
    }

    @Override
    public void appendSearchableData(IndexBuilder builder) {
        builder.appendContentFromColumn(ImsCall.URL);
    }

    @Override
    public long insert(SQLiteDatabase db, TransactionContext txContext,
            long rawContactId, ContentValues values) {

        /// Change for ALPS02216776, normalize the number first. @{
        if (values.containsKey(ImsCall.DATA)
                && !PhoneNumberUtils.isUriNumber(values.getAsString(ImsCall.DATA))) {
            values.put(ImsCall.DATA,
                    PhoneNumberUtils.normalizeNumber(values.getAsString(ImsCall.DATA)));
        }
        /// @}

        long dataId = super.insert(db, txContext, rawContactId, values);

        /// insert IMS number into phone_lookup table. @{
        if (values.containsKey(ImsCall.DATA)) {
            String imsNumber = values.getAsString(ImsCall.DATA);
            updatePhoneLookup(db, rawContactId, dataId, imsNumber);
        }
        /// @}

        return dataId;
    }

    @Override
    public boolean update(SQLiteDatabase db, TransactionContext txContext,
            ContentValues values, Cursor c, boolean callerIsSyncAdapter) {

        /// Change for ALPS02216776, normalize the number first. @{
        if (values.containsKey(ImsCall.DATA)
                && !PhoneNumberUtils.isUriNumber(values.getAsString(ImsCall.DATA))) {
            values.put(ImsCall.DATA,
                    PhoneNumberUtils.normalizeNumber(values.getAsString(ImsCall.DATA)));
        }
        /// @}

        if (!super.update(db, txContext, values, c, callerIsSyncAdapter)) {
            return false;
        }

        /// update IMS number in phone_lookup table. @{
        if (values.containsKey(ImsCall.DATA)) {
            long dataId = c.getLong(DataUpdateQuery._ID);
            long rawContactId = c.getLong(DataUpdateQuery.RAW_CONTACT_ID);
            String imsNumber = values.getAsString(ImsCall.DATA);
            updatePhoneLookup(db, rawContactId, dataId, imsNumber);
        }
        /// @}
        return true;

    }

    @Override
    public int delete(SQLiteDatabase db, TransactionContext txContext, Cursor c) {

        int count = super.delete(db, txContext, c);

        /// delete IMS number in phone_lookup table. @{
        long dataId = c.getLong(DataDeleteQuery._ID);
        long rawContactId = c.getLong(DataDeleteQuery.RAW_CONTACT_ID);
        updatePhoneLookup(db, rawContactId, dataId, null);
        /// @}

        return count;
    }

    /**
     * Update phone look up data when IMS number change.
     */
    private void updatePhoneLookup(SQLiteDatabase db, long rawContactId, long dataId,
            String number) {
        mSelectionArgs1[0] = String.valueOf(dataId);
        db.delete(Tables.PHONE_LOOKUP, PhoneLookupColumns.DATA_ID + "=?", mSelectionArgs1);
        if (!TextUtils.isEmpty(number) && PhoneNumberUtils.isGlobalPhoneNumber(number)) {
            ContentValues values = new ContentValues();
            values.put(PhoneLookupColumns.RAW_CONTACT_ID, rawContactId);
            values.put(PhoneLookupColumns.DATA_ID, dataId);
            values.put(PhoneLookupColumns.NORMALIZED_NUMBER, number);
            values.put(PhoneLookupColumns.MIN_MATCH, PhoneNumberUtils.toCallerIDMinMatch(number));
            db.insert(Tables.PHONE_LOOKUP, null, values);
        }
    }
}
