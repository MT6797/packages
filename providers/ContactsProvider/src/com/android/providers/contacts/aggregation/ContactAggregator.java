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

package com.android.providers.contacts.aggregation;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.ContactsContract.AggregationExceptions;
import android.provider.ContactsContract.CommonDataKinds.Identity;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Contacts.AggregationSuggestions;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.Log;

import com.android.providers.contacts.ContactsDatabaseHelper;
import com.android.providers.contacts.ContactsDatabaseHelper.DataColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.NameLookupColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.NameLookupType;
import com.android.providers.contacts.ContactsDatabaseHelper.RawContactsColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.ContactsProvider2;
import com.android.providers.contacts.NameSplitter;
import com.android.providers.contacts.PhotoPriorityResolver;
import com.android.providers.contacts.ReorderingCursorWrapper;
import com.android.providers.contacts.TransactionContext;
import com.android.providers.contacts.aggregation.util.CommonNicknameCache;
import com.android.providers.contacts.aggregation.util.ContactMatcher;
import com.android.providers.contacts.aggregation.util.MatchScore;
import com.android.providers.contacts.database.ContactsTableUtil;
import com.google.android.collect.Sets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * ContactAggregator deals with aggregating contact information coming from different sources.
 * Two John Doe contacts from two disjoint sources are presumed to be the same
 * person unless the user declares otherwise.
 */
public class ContactAggregator extends AbstractContactAggregator {

    // Return code for the canJoinIntoContact method.
    private static final int JOIN = 1;
    private static final int KEEP_SEPARATE = 0;
    private static final int RE_AGGREGATE = -1;

    private final ContactMatcher mMatcher = new ContactMatcher();

    /**
     * Constructor.
     */
    public ContactAggregator(ContactsProvider2 contactsProvider,
            ContactsDatabaseHelper contactsDatabaseHelper,
            PhotoPriorityResolver photoPriorityResolver, NameSplitter nameSplitter,
            CommonNicknameCache commonNicknameCache) {
        super(contactsProvider, contactsDatabaseHelper, photoPriorityResolver, nameSplitter,
                commonNicknameCache);
    }

  /**
     * Given a specific raw contact, finds all matching aggregate contacts and chooses the one
     * with the highest match score.  If no such contact is found, creates a new contact.
     */
    synchronized void aggregateContact(TransactionContext txContext, SQLiteDatabase db,
            long rawContactId, long accountId, long currentContactId,
            MatchCandidateList candidates) {

        if (VERBOSE_LOGGING) {
            Log.v(TAG, "aggregateContact: rid=" + rawContactId + " cid=" + currentContactId);
        }

        int aggregationMode = RawContacts.AGGREGATION_MODE_DEFAULT;

        Integer aggModeObject = mRawContactsMarkedForAggregation.remove(rawContactId);
        if (aggModeObject != null) {
            aggregationMode = aggModeObject;
        }

        long contactId = -1; // Best matching contact ID.
        boolean needReaggregate = false;

        final ContactMatcher matcher = new ContactMatcher();
        final Set<Long> rawContactIdsInSameAccount = new HashSet<Long>();
        final Set<Long> rawContactIdsInOtherAccount = new HashSet<Long>();
        if (aggregationMode == RawContacts.AGGREGATION_MODE_DEFAULT) {
            candidates.clear();
            matcher.clear();

            contactId = pickBestMatchBasedOnExceptions(db, rawContactId, matcher);
            if (contactId == -1) {

                // If this is a newly inserted contact or a visible contact, look for
                // data matches.
                if (currentContactId == 0
                        || mDbHelper.isContactInDefaultDirectory(db, currentContactId)) {
                    contactId = pickBestMatchBasedOnData(db, rawContactId, candidates, matcher);
                }

                // If we found an best matched contact, find out if the raw contact can be joined
                // into it
                if (contactId != -1 && contactId != currentContactId) {
                    // List all raw contact ID and their account ID mappings in contact
                    // [contactId] excluding raw_contact [rawContactId].

                    // Based on the mapping, create two sets of raw contact IDs in
                    // [rawContactAccountId] and not in [rawContactAccountId]. We don't always
                    // need them, so lazily initialize them.
                    mSelectionArgs2[0] = String.valueOf(contactId);
                    mSelectionArgs2[1] = String.valueOf(rawContactId);
                    final Cursor rawContactsToAccountsCursor = db.rawQuery(
                            "SELECT " + RawContacts._ID + ", " + RawContactsColumns.ACCOUNT_ID +
                                    " FROM " + Tables.RAW_CONTACTS +
                                    " WHERE " + RawContacts.CONTACT_ID + "=?" +
                                    " AND " + RawContacts._ID + "!=?",
                            mSelectionArgs2);
                    try {
                        rawContactsToAccountsCursor.moveToPosition(-1);
                        while (rawContactsToAccountsCursor.moveToNext()) {
                            final long rcId = rawContactsToAccountsCursor.getLong(0);
                            final long rc_accountId = rawContactsToAccountsCursor.getLong(1);
                            if (rc_accountId == accountId) {
                                rawContactIdsInSameAccount.add(rcId);
                            } else {
                                rawContactIdsInOtherAccount.add(rcId);
                            }
                        }
                    } finally {
                        rawContactsToAccountsCursor.close();
                    }
                    final int actionCode;
                    final int totalNumOfRawContactsInCandidate = rawContactIdsInSameAccount.size()
                            + rawContactIdsInOtherAccount.size();
                    if (totalNumOfRawContactsInCandidate >= AGGREGATION_CONTACT_SIZE_LIMIT) {
                        if (VERBOSE_LOGGING) {
                            Log.v(TAG, "Too many raw contacts (" + totalNumOfRawContactsInCandidate
                                    + ") in the best matching contact, so skip aggregation");
                        }
                        actionCode = KEEP_SEPARATE;
                    } else {
                        actionCode = canJoinIntoContact(db, rawContactId,
                                rawContactIdsInSameAccount, rawContactIdsInOtherAccount);
                    }
                    if (actionCode == KEEP_SEPARATE) {
                        contactId = -1;
                    } else if (actionCode == RE_AGGREGATE) {
                        needReaggregate = true;
                    }
                }
            }
        } else if (aggregationMode == RawContacts.AGGREGATION_MODE_DISABLED) {
            return;
        }

        // # of raw_contacts in the [currentContactId] contact excluding the [rawContactId]
        // raw_contact.
        long currentContactContentsCount = 0;

        if (currentContactId != 0) {
            mRawContactCountQuery.bindLong(1, currentContactId);
            mRawContactCountQuery.bindLong(2, rawContactId);
            currentContactContentsCount = mRawContactCountQuery.simpleQueryForLong();
        }

        // If there are no other raw contacts in the current aggregate, we might as well reuse it.
        // Also, if the aggregation mode is SUSPENDED, we must reuse the same aggregate.
        if (contactId == -1
                && currentContactId != 0
                && (currentContactContentsCount == 0
                        || aggregationMode == RawContacts.AGGREGATION_MODE_SUSPENDED)) {
            contactId = currentContactId;
        }

        if (contactId == currentContactId) {
            // Aggregation unchanged
            markAggregated(db, String.valueOf(rawContactId));
            if (VERBOSE_LOGGING) {
                Log.v(TAG, "Aggregation unchanged");
            }
        } else if (contactId == -1) {
            // create new contact for [rawContactId]
            createContactForRawContacts(db, txContext, Sets.newHashSet(rawContactId), null);
            if (currentContactContentsCount > 0) {
                updateAggregateData(txContext, currentContactId);
            }
            if (VERBOSE_LOGGING) {
                Log.v(TAG, "create new contact for rid=" + rawContactId);
            }
        } else if (needReaggregate) {
            // re-aggregate
            final Set<Long> allRawContactIdSet = new HashSet<Long>();
            allRawContactIdSet.addAll(rawContactIdsInSameAccount);
            allRawContactIdSet.addAll(rawContactIdsInOtherAccount);
            // If there is no other raw contacts aggregated with the given raw contact currently,
            // we might as well reuse it.
            currentContactId = (currentContactId != 0 && currentContactContentsCount == 0)
                    ? currentContactId : 0;
            reAggregateRawContacts(txContext, db, contactId, currentContactId, rawContactId,
                    allRawContactIdSet);
            if (VERBOSE_LOGGING) {
                Log.v(TAG, "Re-aggregating rid=" + rawContactId + " and cid=" + contactId);
            }
        } else {
            // Joining with an existing aggregate
            if (currentContactContentsCount == 0) {
                // Delete a previous aggregate if it only contained this raw contact
                ContactsTableUtil.deleteContact(db, currentContactId);

                mAggregatedPresenceDelete.bindLong(1, currentContactId);
                mAggregatedPresenceDelete.execute();
            }

            clearSuperPrimarySetting(db, contactId, rawContactId);
            setContactIdAndMarkAggregated(rawContactId, contactId);
            computeAggregateData(db, contactId, mContactUpdate);
            mContactUpdate.bindLong(ContactReplaceSqlStatement.CONTACT_ID, contactId);
            mContactUpdate.execute();
            mDbHelper.updateContactVisible(txContext, contactId);
            updateAggregatedStatusUpdate(contactId);
            // Make sure the raw contact does not contribute to the current contact
            if (currentContactId != 0) {
                updateAggregateData(txContext, currentContactId);
            }
            if (VERBOSE_LOGGING) {
                Log.v(TAG, "Join rid=" + rawContactId + " with cid=" + contactId);
            }
        }
    }

    /**
     * Find out which mime-types are shared by raw contact of {@code rawContactId} and raw contacts
     * of {@code contactId}. Clear the is_super_primary settings for these mime-types.
     */
    private void clearSuperPrimarySetting(SQLiteDatabase db, long contactId, long rawContactId) {
        final String[] args = {String.valueOf(contactId), String.valueOf(rawContactId)};

        // Find out which mime-types exist with is_super_primary=true on both the raw contact of
        // rawContactId and raw contacts of contactId
        int index = 0;
        final StringBuilder mimeTypeCondition = new StringBuilder();
        mimeTypeCondition.append(" AND " + DataColumns.MIMETYPE_ID + " IN (");

        final Cursor c = db.rawQuery(
                "SELECT DISTINCT(a." + DataColumns.MIMETYPE_ID + ")" +
                " FROM (SELECT " + DataColumns.MIMETYPE_ID + " FROM " + Tables.DATA + " WHERE " +
                        Data.IS_SUPER_PRIMARY + " =1 AND " +
                        Data.RAW_CONTACT_ID + " IN (SELECT " + RawContacts._ID + " FROM " +
                        Tables.RAW_CONTACTS + " WHERE " + RawContacts.CONTACT_ID + "=?1)) AS a" +
                " JOIN  (SELECT " + DataColumns.MIMETYPE_ID + " FROM " + Tables.DATA + " WHERE " +
                        Data.IS_SUPER_PRIMARY + " =1 AND " +
                        Data.RAW_CONTACT_ID + "=?2) AS b" +
                " ON a." + DataColumns.MIMETYPE_ID + "=b." + DataColumns.MIMETYPE_ID,
                args);
        try {
            c.moveToPosition(-1);
            while (c.moveToNext()) {
                if (index > 0) {
                    mimeTypeCondition.append(',');
                }
                mimeTypeCondition.append(c.getLong((0)));
                index++;
            }
        } finally {
            c.close();
        }

        if (index == 0) {
            return;
        }

        // Clear is_super_primary setting for all the mime-types with is_super_primary=true
        // in both raw contact of rawContactId and raw contacts of contactId
        String superPrimaryUpdateSql = "UPDATE " + Tables.DATA +
                " SET " + Data.IS_SUPER_PRIMARY + "=0" +
                " WHERE (" +  Data.RAW_CONTACT_ID +
                        " IN (SELECT " + RawContacts._ID +  " FROM " + Tables.RAW_CONTACTS +
                        " WHERE " + RawContacts.CONTACT_ID + "=?1)" +
                        " OR " +  Data.RAW_CONTACT_ID + "=?2)";

        mimeTypeCondition.append(')');
        superPrimaryUpdateSql += mimeTypeCondition.toString();
        db.execSQL(superPrimaryUpdateSql, args);
    }

    /**
     * @return JOIN if the raw contact of {@code rawContactId} can be joined into the existing
     * contact of {@code contactId}. KEEP_SEPARATE if the raw contact of {@code rawContactId}
     * cannot be joined into the existing contact of {@code contactId}. RE_AGGREGATE if raw contact
     * of {@code rawContactId} and all the raw contacts of contact of {@code contactId} need to be
     * re-aggregated.
     *
     * If contact of {@code contactId} doesn't contain any raw contacts from the same account as
     * raw contact of {@code rawContactId}, join raw contact with contact if there is no identity
     * mismatch between them on the same namespace, otherwise, keep them separate.
     *
     * If contact of {@code contactId} contains raw contacts from the same account as raw contact of
     * {@code rawContactId}, join raw contact with contact if there's at least one raw contact in
     * those raw contacts that shares at least one email address, phone number, or identity;
     * otherwise, re-aggregate raw contact and all the raw contacts of contact.
     */
    private int canJoinIntoContact(SQLiteDatabase db, long rawContactId,
            Set<Long> rawContactIdsInSameAccount, Set<Long> rawContactIdsInOtherAccount ) {

        if (rawContactIdsInSameAccount.isEmpty()) {
            final String rid = String.valueOf(rawContactId);
            final String ridsInOtherAccts = TextUtils.join(",", rawContactIdsInOtherAccount);
            // If there is no identity match between raw contact of [rawContactId] and
            // any raw contact in other accounts on the same namespace, and there is at least
            // one identity mismatch exist, keep raw contact separate from contact.
            if (DatabaseUtils.longForQuery(db, buildIdentityMatchingSql(rid, ridsInOtherAccts,
                    /* isIdentityMatching =*/ true, /* countOnly =*/ true), null) == 0 &&
                    DatabaseUtils.longForQuery(db, buildIdentityMatchingSql(rid, ridsInOtherAccts,
                            /* isIdentityMatching =*/ false, /* countOnly =*/ true), null) > 0) {
                if (VERBOSE_LOGGING) {
                    Log.v(TAG, "canJoinIntoContact: no duplicates, but has no matching identity " +
                            "and has mis-matching identity on the same namespace between rid=" +
                            rid + " and ridsInOtherAccts=" + ridsInOtherAccts);
                }
                return KEEP_SEPARATE; // has identity and identity doesn't match
            } else {
                if (VERBOSE_LOGGING) {
                    Log.v(TAG, "canJoinIntoContact: can join the first raw contact from the same " +
                            "account without any identity mismatch.");
                }
                return JOIN; // no identity or identity match
            }
        }
        if (VERBOSE_LOGGING) {
            Log.v(TAG, "canJoinIntoContact: " + rawContactIdsInSameAccount.size() +
                    " duplicate(s) found");
        }


        final Set<Long> rawContactIdSet = new HashSet<Long>();
        rawContactIdSet.add(rawContactId);
        if (rawContactIdsInSameAccount.size() > 0 &&
                isDataMaching(db, rawContactIdSet, rawContactIdsInSameAccount)) {
            if (VERBOSE_LOGGING) {
                Log.v(TAG, "canJoinIntoContact: join if there is a data matching found in the " +
                        "same account");
            }
            return JOIN;
        } else {
            if (VERBOSE_LOGGING) {
                Log.v(TAG, "canJoinIntoContact: re-aggregate rid=" + rawContactId +
                        " with its best matching contact to connected component");
            }
            return RE_AGGREGATE;
        }
    }

    /**
     * If there's any identity, email address or a phone number matching between two raw contact
     * sets.
     */
    private boolean isDataMaching(SQLiteDatabase db, Set<Long> rawContactIdSet1,
            Set<Long> rawContactIdSet2) {
        final String rawContactIds1 = TextUtils.join(",", rawContactIdSet1);
        final String rawContactIds2 = TextUtils.join(",", rawContactIdSet2);
        // First, check for the identity
        if (isFirstColumnGreaterThanZero(db, buildIdentityMatchingSql(
                rawContactIds1, rawContactIds2,  /* isIdentityMatching =*/ true,
                /* countOnly =*/true))) {
            if (VERBOSE_LOGGING) {
                Log.v(TAG, "canJoinIntoContact: identity match found between " + rawContactIds1 +
                        " and " + rawContactIds2);
            }
            return true;
        }

        // Next, check for the email address.
        if (isFirstColumnGreaterThanZero(db,
                buildEmailMatchingSql(rawContactIds1, rawContactIds2, true))) {
            if (VERBOSE_LOGGING) {
                Log.v(TAG, "canJoinIntoContact: email match found between " + rawContactIds1 +
                        " and " + rawContactIds2);
            }
            return true;
        }

        // Lastly, the phone number.
        if (isFirstColumnGreaterThanZero(db,
                buildPhoneMatchingSql(rawContactIds1, rawContactIds2, true))) {
            if (VERBOSE_LOGGING) {
                Log.v(TAG, "canJoinIntoContact: phone match found between " + rawContactIds1 +
                        " and " + rawContactIds2);
            }
            return true;
        }
        return false;
    }

    /**
     * Re-aggregate rawContact of {@code rawContactId} and all the raw contacts of
     * {@code existingRawContactIds} into connected components. This only happens when a given
     * raw contacts cannot be joined with its best matching contacts directly.
     *
     *  Two raw contacts are considered connected if they share at least one email address, phone
     *  number or identity. Create new contact for each connected component except the very first
     *  one that doesn't contain rawContactId of {@code rawContactId}.
     */
    private void reAggregateRawContacts(TransactionContext txContext, SQLiteDatabase db,
            long contactId, long currentContactId, long rawContactId,
            Set<Long> existingRawContactIds) {
        // Find the connected component based on the aggregation exceptions or
        // identity/email/phone matching for all the raw contacts of [contactId] and the give
        // raw contact.
        final Set<Long> allIds = new HashSet<Long>();
        allIds.add(rawContactId);
        allIds.addAll(existingRawContactIds);
        final Set<Set<Long>> connectedRawContactSets = findConnectedRawContacts(db, allIds);

        if (connectedRawContactSets.size() == 1) {
            // If everything is connected, create one contact with [contactId]
            createContactForRawContacts(db, txContext, connectedRawContactSets.iterator().next(),
                    contactId);
        } else {
            for (Set<Long> connectedRawContactIds : connectedRawContactSets) {
                if (connectedRawContactIds.contains(rawContactId)) {
                    // crate contact for connect component containing [rawContactId], reuse
                    // [currentContactId] if possible.
                    createContactForRawContacts(db, txContext, connectedRawContactIds,
                            currentContactId == 0 ? null : currentContactId);
                    connectedRawContactSets.remove(connectedRawContactIds);
                    break;
                }
            }
            // Create new contact for each connected component except the last one. The last one
            // will reuse [contactId]. Only the last one can reuse [contactId] when all other raw
            // contacts has already been assigned new contact Id, so that the contact aggregation
            // stats could be updated correctly.
            int index = connectedRawContactSets.size();
            for (Set<Long> connectedRawContactIds : connectedRawContactSets) {
                if (index > 1) {
                    createContactForRawContacts(db, txContext, connectedRawContactIds, null);
                    index--;
                } else {
                    createContactForRawContacts(db, txContext, connectedRawContactIds, contactId);
                }
            }
        }
    }

    /**
     * Ensures that automatic aggregation rules are followed after a contact
     * becomes visible or invisible. Specifically, consider this case: there are
     * three contacts named Foo. Two of them come from account A1 and one comes
     * from account A2. The aggregation rules say that in this case none of the
     * three Foo's should be aggregated: two of them are in the same account, so
     * they don't get aggregated; the third has two affinities, so it does not
     * join either of them.
     * <p>
     * Consider what happens if one of the "Foo"s from account A1 becomes
     * invisible. Nothing stands in the way of aggregating the other two
     * anymore, so they should get joined.
     * <p>
     * What if the invisible "Foo" becomes visible after that? We should split the
     * aggregate between the other two.
     */
    public void updateAggregationAfterVisibilityChange(long contactId) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        boolean visible = mDbHelper.isContactInDefaultDirectory(db, contactId);
        if (visible) {
            markContactForAggregation(db, contactId);
        } else {
            // Find all contacts that _could be_ aggregated with this one and
            // rerun aggregation for all of them
            mSelectionArgs1[0] = String.valueOf(contactId);
            Cursor cursor = db.query(RawContactIdQuery.TABLE, RawContactIdQuery.COLUMNS,
                    RawContactIdQuery.SELECTION, mSelectionArgs1, null, null, null);
            try {
                while (cursor.moveToNext()) {
                    long rawContactId = cursor.getLong(RawContactIdQuery.RAW_CONTACT_ID);
                    mMatcher.clear();

                    updateMatchScoresBasedOnIdentityMatch(db, rawContactId, mMatcher);
                    updateMatchScoresBasedOnNameMatches(db, rawContactId, mMatcher);
                    List<MatchScore> bestMatches =
                            mMatcher.pickBestMatches(ContactMatcher.SCORE_THRESHOLD_PRIMARY);
                    for (MatchScore matchScore : bestMatches) {
                        markContactForAggregation(db, matchScore.getContactId());
                    }

                    mMatcher.clear();
                    updateMatchScoresBasedOnEmailMatches(db, rawContactId, mMatcher);
                    updateMatchScoresBasedOnPhoneMatches(db, rawContactId, mMatcher);
                    bestMatches =
                            mMatcher.pickBestMatches(ContactMatcher.SCORE_THRESHOLD_SECONDARY);
                    for (MatchScore matchScore : bestMatches) {
                        markContactForAggregation(db, matchScore.getContactId());
                    }
                }
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Updates the contact ID for the specified contact and marks the raw contact as aggregated.
     */
    private void setContactIdAndMarkAggregated(long rawContactId, long contactId) {
        mContactIdAndMarkAggregatedUpdate.bindLong(1, contactId);
        mContactIdAndMarkAggregatedUpdate.bindLong(2, rawContactId);
        mContactIdAndMarkAggregatedUpdate.execute();
    }

    interface AggregateExceptionQuery {
        String TABLE = Tables.AGGREGATION_EXCEPTIONS
            + " JOIN raw_contacts raw_contacts1 "
                    + " ON (agg_exceptions.raw_contact_id1 = raw_contacts1._id) "
            + " JOIN raw_contacts raw_contacts2 "
                    + " ON (agg_exceptions.raw_contact_id2 = raw_contacts2._id) ";

        String[] COLUMNS = {
            AggregationExceptions.TYPE,
            AggregationExceptions.RAW_CONTACT_ID1,
            "raw_contacts1." + RawContacts.CONTACT_ID,
            "raw_contacts1." + RawContactsColumns.AGGREGATION_NEEDED,
            "raw_contacts2." + RawContacts.CONTACT_ID,
            "raw_contacts2." + RawContactsColumns.AGGREGATION_NEEDED,
        };

        int TYPE = 0;
        int RAW_CONTACT_ID1 = 1;
        int CONTACT_ID1 = 2;
        int AGGREGATION_NEEDED_1 = 3;
        int CONTACT_ID2 = 4;
        int AGGREGATION_NEEDED_2 = 5;
    }

    /**
     * Computes match scores based on exceptions entered by the user: always match and never match.
     * Returns the aggregate contact with the always match exception if any.
     */
    private long pickBestMatchBasedOnExceptions(SQLiteDatabase db, long rawContactId,
            ContactMatcher matcher) {
        if (!mAggregationExceptionIdsValid) {
            prefetchAggregationExceptionIds(db);
        }

        // If there are no aggregation exceptions involving this raw contact, there is no need to
        // run a query and we can just return -1, which stands for "nothing found"
        if (!mAggregationExceptionIds.contains(rawContactId)) {
            return -1;
        }

        final Cursor c = db.query(AggregateExceptionQuery.TABLE,
                AggregateExceptionQuery.COLUMNS,
                AggregationExceptions.RAW_CONTACT_ID1 + "=" + rawContactId
                        + " OR " + AggregationExceptions.RAW_CONTACT_ID2 + "=" + rawContactId,
                null, null, null, null);

        try {
            while (c.moveToNext()) {
                int type = c.getInt(AggregateExceptionQuery.TYPE);
                long rawContactId1 = c.getLong(AggregateExceptionQuery.RAW_CONTACT_ID1);
                long contactId = -1;
                if (rawContactId == rawContactId1) {
                    if (c.getInt(AggregateExceptionQuery.AGGREGATION_NEEDED_2) == 0
                            && !c.isNull(AggregateExceptionQuery.CONTACT_ID2)) {
                        contactId = c.getLong(AggregateExceptionQuery.CONTACT_ID2);
                    }
                } else {
                    if (c.getInt(AggregateExceptionQuery.AGGREGATION_NEEDED_1) == 0
                            && !c.isNull(AggregateExceptionQuery.CONTACT_ID1)) {
                        contactId = c.getLong(AggregateExceptionQuery.CONTACT_ID1);
                    }
                }
                if (contactId != -1) {
                    if (type == AggregationExceptions.TYPE_KEEP_TOGETHER) {
                        matcher.keepIn(contactId);
                    } else {
                        matcher.keepOut(contactId);
                    }
                }
            }
        } finally {
            c.close();
        }

        return matcher.pickBestMatch(MatchScore.MAX_SCORE, true);
    }

    /**
     * Picks the best matching contact based on matches between data elements.  It considers
     * name match to be primary and phone, email etc matches to be secondary.  A good primary
     * match triggers aggregation, while a good secondary match only triggers aggregation in
     * the absence of a strong primary mismatch.
     * <p>
     * Consider these examples:
     * <p>
     * John Doe with phone number 111-111-1111 and Jon Doe with phone number 111-111-1111 should
     * be aggregated (same number, similar names).
     * <p>
     * John Doe with phone number 111-111-1111 and Deborah Doe with phone number 111-111-1111 should
     * not be aggregated (same number, different names).
     */
    private long pickBestMatchBasedOnData(SQLiteDatabase db, long rawContactId,
            MatchCandidateList candidates, ContactMatcher matcher) {

        // Find good matches based on name alone
        long bestMatch = updateMatchScoresBasedOnDataMatches(db, rawContactId, matcher);
        if (bestMatch == ContactMatcher.MULTIPLE_MATCHES) {
            // We found multiple matches on the name - do not aggregate because of the ambiguity
            return -1;
        } else if (bestMatch == -1) {
            // We haven't found a good match on name, see if we have any matches on phone, email etc
            bestMatch = pickBestMatchBasedOnSecondaryData(db, rawContactId, candidates, matcher);
            if (bestMatch == ContactMatcher.MULTIPLE_MATCHES) {
                return -1;
            }
        }

        return bestMatch;
    }


    /**
     * Picks the best matching contact based on secondary data matches.  The method loads
     * structured names for all candidate contacts and recomputes match scores using approximate
     * matching.
     */
    private long pickBestMatchBasedOnSecondaryData(SQLiteDatabase db,
            long rawContactId, MatchCandidateList candidates, ContactMatcher matcher) {
        List<Long> secondaryContactIds = matcher.prepareSecondaryMatchCandidates(
                ContactMatcher.SCORE_THRESHOLD_PRIMARY);
        if (secondaryContactIds == null || secondaryContactIds.size() > SECONDARY_HIT_LIMIT) {
            return -1;
        }

        loadNameMatchCandidates(db, rawContactId, candidates, true);

        mSb.setLength(0);
        mSb.append(RawContacts.CONTACT_ID).append(" IN (");
        for (int i = 0; i < secondaryContactIds.size(); i++) {
            if (i != 0) {
                mSb.append(',');
            }
            mSb.append(secondaryContactIds.get(i));
        }

        // We only want to compare structured names to structured names
        // at this stage, we need to ignore all other sources of name lookup data.
        mSb.append(") AND " + STRUCTURED_NAME_BASED_LOOKUP_SQL);

        matchAllCandidates(db, mSb.toString(), candidates, matcher,
                ContactMatcher.MATCHING_ALGORITHM_CONSERVATIVE, null);

        return matcher.pickBestMatch(ContactMatcher.SCORE_THRESHOLD_SECONDARY, false);
    }

    /**
     * Computes scores for contacts that have matching data rows.
     */
    private long updateMatchScoresBasedOnDataMatches(SQLiteDatabase db, long rawContactId,
            ContactMatcher matcher) {

        updateMatchScoresBasedOnIdentityMatch(db, rawContactId, matcher);
        updateMatchScoresBasedOnNameMatches(db, rawContactId, matcher);
        long bestMatch = matcher.pickBestMatch(ContactMatcher.SCORE_THRESHOLD_PRIMARY, false);
        if (bestMatch != -1) {
            return bestMatch;
        }

        updateMatchScoresBasedOnEmailMatches(db, rawContactId, matcher);
        updateMatchScoresBasedOnPhoneMatches(db, rawContactId, matcher);

        return -1;
    }

    private interface IdentityLookupMatchQuery {
        final String TABLE = Tables.DATA + " dataA"
                + " JOIN " + Tables.DATA + " dataB" +
                " ON (dataA." + Identity.NAMESPACE + "=dataB." + Identity.NAMESPACE +
                " AND dataA." + Identity.IDENTITY + "=dataB." + Identity.IDENTITY + ")"
                + " JOIN " + Tables.RAW_CONTACTS +
                " ON (dataB." + Data.RAW_CONTACT_ID + " = "
                + Tables.RAW_CONTACTS + "." + RawContacts._ID + ")";

        final String SELECTION = "dataA." + Data.RAW_CONTACT_ID + "=?1"
                + " AND dataA." + DataColumns.MIMETYPE_ID + "=?2"
                + " AND dataA." + Identity.NAMESPACE + " NOT NULL"
                + " AND dataA." + Identity.IDENTITY + " NOT NULL"
                + " AND dataB." + DataColumns.MIMETYPE_ID + "=?2"
                + " AND " + RawContactsColumns.AGGREGATION_NEEDED + "=0"
                + " AND " + RawContacts.CONTACT_ID + " IN " + Tables.DEFAULT_DIRECTORY;

        final String[] COLUMNS = new String[] {
            RawContacts.CONTACT_ID
        };

        int CONTACT_ID = 0;
    }

    /**
     * Finds contacts with exact identity matches to the the specified raw contact.
     */
    private void updateMatchScoresBasedOnIdentityMatch(SQLiteDatabase db, long rawContactId,
            ContactMatcher matcher) {
        mSelectionArgs2[0] = String.valueOf(rawContactId);
        mSelectionArgs2[1] = String.valueOf(mMimeTypeIdIdentity);
        Cursor c = db.query(IdentityLookupMatchQuery.TABLE, IdentityLookupMatchQuery.COLUMNS,
                IdentityLookupMatchQuery.SELECTION,
                mSelectionArgs2, RawContacts.CONTACT_ID, null, null);
        try {
            while (c.moveToNext()) {
                final long contactId = c.getLong(IdentityLookupMatchQuery.CONTACT_ID);
                matcher.matchIdentity(contactId);
            }
        } finally {
            c.close();
        }

    }

    private interface NameLookupMatchQuery {
        String TABLE = Tables.NAME_LOOKUP + " nameA"
                + " JOIN " + Tables.NAME_LOOKUP + " nameB" +
                " ON (" + "nameA." + NameLookupColumns.NORMALIZED_NAME + "="
                        + "nameB." + NameLookupColumns.NORMALIZED_NAME + ")"
                + " JOIN " + Tables.RAW_CONTACTS +
                " ON (nameB." + NameLookupColumns.RAW_CONTACT_ID + " = "
                        + Tables.RAW_CONTACTS + "." + RawContacts._ID + ")";

        String SELECTION = "nameA." + NameLookupColumns.RAW_CONTACT_ID + "=?"
                + " AND " + RawContactsColumns.AGGREGATION_NEEDED + "=0"
                + " AND " + RawContacts.CONTACT_ID + " IN " + Tables.DEFAULT_DIRECTORY;

        String[] COLUMNS = new String[] {
            RawContacts.CONTACT_ID,
            "nameA." + NameLookupColumns.NORMALIZED_NAME,
            "nameA." + NameLookupColumns.NAME_TYPE,
            "nameB." + NameLookupColumns.NAME_TYPE,
        };

        int CONTACT_ID = 0;
        int NAME = 1;
        int NAME_TYPE_A = 2;
        int NAME_TYPE_B = 3;
    }

    /**
     * Finds contacts with names matching the name of the specified raw contact.
     */
    private void updateMatchScoresBasedOnNameMatches(SQLiteDatabase db, long rawContactId,
            ContactMatcher matcher) {
        mSelectionArgs1[0] = String.valueOf(rawContactId);
        Cursor c = db.query(NameLookupMatchQuery.TABLE, NameLookupMatchQuery.COLUMNS,
                NameLookupMatchQuery.SELECTION,
                mSelectionArgs1, null, null, null, PRIMARY_HIT_LIMIT_STRING);
        try {
            while (c.moveToNext()) {
                long contactId = c.getLong(NameLookupMatchQuery.CONTACT_ID);
                String name = c.getString(NameLookupMatchQuery.NAME);
                int nameTypeA = c.getInt(NameLookupMatchQuery.NAME_TYPE_A);
                int nameTypeB = c.getInt(NameLookupMatchQuery.NAME_TYPE_B);
                matcher.matchName(contactId, nameTypeA, name,
                        nameTypeB, name, ContactMatcher.MATCHING_ALGORITHM_EXACT);
                if (nameTypeA == NameLookupType.NICKNAME &&
                        nameTypeB == NameLookupType.NICKNAME) {
                    matcher.updateScoreWithNicknameMatch(contactId);
                }
            }
        } finally {
            c.close();
        }
    }

    private void updateMatchScoresBasedOnEmailMatches(SQLiteDatabase db, long rawContactId,
            ContactMatcher matcher) {
        mSelectionArgs2[0] = String.valueOf(rawContactId);
        mSelectionArgs2[1] = String.valueOf(mMimeTypeIdEmail);
        Cursor c = db.query(EmailLookupQuery.TABLE, EmailLookupQuery.COLUMNS,
                EmailLookupQuery.SELECTION,
                mSelectionArgs2, null, null, null, SECONDARY_HIT_LIMIT_STRING);
        try {
            while (c.moveToNext()) {
                long contactId = c.getLong(EmailLookupQuery.CONTACT_ID);
                matcher.updateScoreWithEmailMatch(contactId);
            }
        } finally {
            c.close();
        }
    }

    private void updateMatchScoresBasedOnPhoneMatches(SQLiteDatabase db, long rawContactId,
            ContactMatcher matcher) {
        mSelectionArgs2[0] = String.valueOf(rawContactId);
        mSelectionArgs2[1] = mDbHelper.getUseStrictPhoneNumberComparisonParameter();
        Cursor c = db.query(PhoneLookupQuery.TABLE, PhoneLookupQuery.COLUMNS,
                PhoneLookupQuery.SELECTION,
                mSelectionArgs2, null, null, null, SECONDARY_HIT_LIMIT_STRING);
        try {
            while (c.moveToNext()) {
                long contactId = c.getLong(PhoneLookupQuery.CONTACT_ID);
                matcher.updateScoreWithPhoneNumberMatch(contactId);
            }
        } finally {
            c.close();
        }
    }

    /**
     * Loads name lookup rows for approximate name matching and updates match scores based on that
     * data.
     */
    private void lookupApproximateNameMatches(SQLiteDatabase db, MatchCandidateList candidates,
            ContactMatcher matcher) {
        HashSet<String> firstLetters = new HashSet<String>();
        for (int i = 0; i < candidates.mCount; i++) {
            final NameMatchCandidate candidate = candidates.mList.get(i);
            if (candidate.mName.length() >= 2) {
                String firstLetter = candidate.mName.substring(0, 2);
                if (!firstLetters.contains(firstLetter)) {
                    firstLetters.add(firstLetter);
                    final String selection = "(" + NameLookupColumns.NORMALIZED_NAME + " GLOB '"
                            + firstLetter + "*') AND "
                            + "(" + NameLookupColumns.NAME_TYPE + " IN("
                                    + NameLookupType.NAME_COLLATION_KEY + ","
                                    + NameLookupType.EMAIL_BASED_NICKNAME + ","
                                    + NameLookupType.NICKNAME + ")) AND "
                            + RawContacts.CONTACT_ID + " IN " + Tables.DEFAULT_DIRECTORY;
                    matchAllCandidates(db, selection, candidates, matcher,
                            ContactMatcher.MATCHING_ALGORITHM_APPROXIMATE,
                            String.valueOf(FIRST_LETTER_SUGGESTION_HIT_LIMIT));
                }
            }
        }
    }

    private interface ContactNameLookupQuery {
        String TABLE = Tables.NAME_LOOKUP_JOIN_RAW_CONTACTS;

        String[] COLUMNS = new String[] {
                RawContacts.CONTACT_ID,
                NameLookupColumns.NORMALIZED_NAME,
                NameLookupColumns.NAME_TYPE
        };

        int CONTACT_ID = 0;
        int NORMALIZED_NAME = 1;
        int NAME_TYPE = 2;
    }

    /**
     * Loads all candidate rows from the name lookup table and updates match scores based
     * on that data.
     */
    private void matchAllCandidates(SQLiteDatabase db, String selection,
            MatchCandidateList candidates, ContactMatcher matcher, int algorithm, String limit) {
        final Cursor c = db.query(ContactNameLookupQuery.TABLE, ContactNameLookupQuery.COLUMNS,
                selection, null, null, null, null, limit);

        try {
            while (c.moveToNext()) {
                Long contactId = c.getLong(ContactNameLookupQuery.CONTACT_ID);
                String name = c.getString(ContactNameLookupQuery.NORMALIZED_NAME);
                int nameType = c.getInt(ContactNameLookupQuery.NAME_TYPE);

                // Note the N^2 complexity of the following fragment. This is not a huge concern
                // since the number of candidates is very small and in general secondary hits
                // in the absence of primary hits are rare.
                for (int i = 0; i < candidates.mCount; i++) {
                    NameMatchCandidate candidate = candidates.mList.get(i);
                    matcher.matchName(contactId, candidate.mLookupType, candidate.mName,
                            nameType, name, algorithm);
                }
            }
        } finally {
            c.close();
        }
    }
/*    private interface RawContactsQuery {
        String SQL_FORMAT =
                "SELECT "
                        + RawContactsColumns.CONCRETE_ID + ","
                        + RawContactsColumns.DISPLAY_NAME + ","
                        + RawContactsColumns.DISPLAY_NAME_SOURCE + ","
                        + AccountsColumns.CONCRETE_ACCOUNT_TYPE + ","
                        + AccountsColumns.CONCRETE_ACCOUNT_NAME + ","
                        + AccountsColumns.CONCRETE_DATA_SET + ","
                        + RawContacts.SOURCE_ID + ","
                        + RawContacts.CUSTOM_RINGTONE + ","
                        + RawContacts.SEND_TO_VOICEMAIL + ","
                        + RawContacts.LAST_TIME_CONTACTED + ","
                        + RawContacts.TIMES_CONTACTED + ","
                        + RawContacts.STARRED + ","
                        + RawContacts.PINNED + ","
                        + RawContacts.NAME_VERIFIED + ","
                        + DataColumns.CONCRETE_ID + ","
                        + DataColumns.CONCRETE_MIMETYPE_ID + ","
                        + Data.IS_SUPER_PRIMARY + ","
                        + Photo.PHOTO_FILE_ID + ","
                        + RawContacts.INDICATE_PHONE_SIM + ","  /** M: New Feature Gemini added */
/*                      + RawContacts.INDEX_IN_SIM + ","        /** M: New Feature Gemini added */
/*                  + RawContacts.SEND_TO_VOICEMAIL_VT + ","    /** M: New Feature Gemini added */
 /*                 + RawContacts.SEND_TO_VOICEMAIL_SIP + ","   /** M: New Feature Gemini added */
 /*                 + RawContactsColumns.IS_SDN_CONTACT +   /** M: New Feature Gemini added */
 /*               " FROM " + Tables.RAW_CONTACTS +
                " JOIN " + Tables.ACCOUNTS + " ON ("
                    + AccountsColumns.CONCRETE_ID + "=" + RawContactsColumns.CONCRETE_ACCOUNT_ID
                    + ")" +
                " LEFT OUTER JOIN " + Tables.DATA +
                " ON (" + DataColumns.CONCRETE_RAW_CONTACT_ID + "=" + RawContactsColumns.CONCRETE_ID
                        + " AND ((" + DataColumns.MIMETYPE_ID + "=%d"
                                + " AND " + Photo.PHOTO + " NOT NULL)"
                        + " OR (" + DataColumns.MIMETYPE_ID + "=%d"
                                + " AND " + Phone.NUMBER + " NOT NULL)))";

        String SQL_FORMAT_BY_RAW_CONTACT_ID = SQL_FORMAT +
                " WHERE " + RawContactsColumns.CONCRETE_ID + "=?";

        String SQL_FORMAT_BY_CONTACT_ID = SQL_FORMAT +
                " WHERE " + RawContacts.CONTACT_ID + "=?"
                + " AND " + RawContacts.DELETED + "=0";

        int RAW_CONTACT_ID = 0;
        int DISPLAY_NAME = 1;
        int DISPLAY_NAME_SOURCE = 2;
        int ACCOUNT_TYPE = 3;
        int ACCOUNT_NAME = 4;
        int DATA_SET = 5;
        int SOURCE_ID = 6;
        int CUSTOM_RINGTONE = 7;
        int SEND_TO_VOICEMAIL = 8;
        int LAST_TIME_CONTACTED = 9;
        int TIMES_CONTACTED = 10;
        int STARRED = 11;
        int PINNED = 12;
        int NAME_VERIFIED = 13;
        int DATA_ID = 14;
        int MIMETYPE_ID = 15;
        int IS_SUPER_PRIMARY = 16;
        int PHOTO_FILE_ID = 17;
        int INDICATE_PHONE_SIM = PHOTO_FILE_ID + 1;    /** M: New Feature Gemini added */
 /*     int INDEX_IN_SIM = INDICATE_PHONE_SIM + 1;          /** M: New Feature Gemini added */
 /*     int SEND_TO_VOICEMAIL_VT = INDEX_IN_SIM + 1;  /** M: New Feature Gemini added */
 /*     int SEND_TO_VOICEMAIL_SIP = SEND_TO_VOICEMAIL_VT + 1; /** M: New Feature Gemini added */
  /*    int IS_SDN_CONTACT = SEND_TO_VOICEMAIL_SIP + 1;        /** M: New Feature Gemini added */
  /*  }

    private interface ContactReplaceSqlStatement {
        String UPDATE_SQL =
                "UPDATE " + Tables.CONTACTS +
                " SET "
                        + Contacts.NAME_RAW_CONTACT_ID + "=?, "
                        + Contacts.PHOTO_ID + "=?, "
                        + Contacts.PHOTO_FILE_ID + "=?, "
                        + Contacts.SEND_TO_VOICEMAIL + "=?, "
                        + Contacts.CUSTOM_RINGTONE + "=?, "
                        + Contacts.LAST_TIME_CONTACTED + "=?, "
                        + Contacts.TIMES_CONTACTED + "=?, "
                        + Contacts.STARRED + "=?, "
                        + Contacts.PINNED + "=?, "
                        + Contacts.HAS_PHONE_NUMBER + "=?, "
                        + Contacts.LOOKUP_KEY + "=?, "
                        + Contacts.CONTACT_LAST_UPDATED_TIMESTAMP + "=?, "
                     + Contacts.INDICATE_PHONE_SIM + "=?, "      /** M: New Feature Gemini added */
  /*                 + Contacts.INDEX_IN_SIM + "=?, "            /** M: New Feature Gemini added */
   /*                + Contacts.SEND_TO_VOICEMAIL_VT + "=?, "    /** M: New Feature Gemini added */
   /*                + Contacts.SEND_TO_VOICEMAIL_SIP + "=? " +  /** M: New Feature Gemini added */
  /*                 ", " + Contacts.IS_SDN_CONTACT + "=? " +    /** M: New Feature Gemini added */
  /*                 " WHERE " + Contacts._ID + "=?";

        String INSERT_SQL =
                "INSERT INTO " + Tables.CONTACTS + " ("
                        + Contacts.NAME_RAW_CONTACT_ID + ", "
                        + Contacts.PHOTO_ID + ", "
                        + Contacts.PHOTO_FILE_ID + ", "
                        + Contacts.SEND_TO_VOICEMAIL + ", "
                        + Contacts.CUSTOM_RINGTONE + ", "
                        + Contacts.LAST_TIME_CONTACTED + ", "
                        + Contacts.TIMES_CONTACTED + ", "
                        + Contacts.STARRED + ", "
                        + Contacts.PINNED + ", "
                        + Contacts.HAS_PHONE_NUMBER + ", "
                        + Contacts.LOOKUP_KEY + ", "
                        + Contacts.CONTACT_LAST_UPDATED_TIMESTAMP + ","
                        + Contacts.INDICATE_PHONE_SIM + ", "    /** M: New Feature Gemini added */
  /*                      + Contacts.INDEX_IN_SIM + ", "          /** M: New Feature Gemini added */
  /*                      + Contacts.SEND_TO_VOICEMAIL_VT + ", "  /** M: New Feature Gemini added */
  /*                      + Contacts.SEND_TO_VOICEMAIL_SIP + ", " /** M: New Feature Gemini added */
  /*                      + Contacts.IS_SDN_CONTACT +  ") " +     /** M: New Feature Gemini added */
   /*             " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";  /** M: New Feature Gemini added */

  /*      int NAME_RAW_CONTACT_ID = 1;
        int PHOTO_ID = 2;
        int PHOTO_FILE_ID = 3;
        int SEND_TO_VOICEMAIL = 4;
        int CUSTOM_RINGTONE = 5;
        int LAST_TIME_CONTACTED = 6;
        int TIMES_CONTACTED = 7;
        int STARRED = 8;
        int PINNED = 9;
        int HAS_PHONE_NUMBER = 10;
        int LOOKUP_KEY = 11;
        int CONTACT_LAST_UPDATED_TIMESTAMP = 12;

 int INDICATE_PHONE_SIM = CONTACT_LAST_UPDATED_TIMESTAMP + 1;    /** M: New Feature Gemini added */
  /*      int INDEX_IN_SIM = INDICATE_PHONE_SIM + 1;          /** M: New Feature Gemini added */
 /*       int SEND_TO_VOICEMAIL_VT = INDEX_IN_SIM + 1;  /** M: New Feature Gemini added */
 /*       int SEND_TO_VOICEMAIL_SIP = SEND_TO_VOICEMAIL_VT + 1; /** M: New Feature Gemini added */
 /*       int IS_SDN_CONTACTS = SEND_TO_VOICEMAIL_SIP + 1;       /** M: New Feature Gemini added */
/*
        int CONTACT_ID = IS_SDN_CONTACTS + 1;  /** M: google original code: int CONTACT_ID = 13*/

    /// }

    /**
     * Computes aggregate-level data from constituent raw contacts.
     */
/*    private void computeAggregateData(final SQLiteDatabase db, String sql, String[] sqlArgs,
            SQLiteStatement statement) {
        long currentRawContactId = -1;
        long bestPhotoId = -1;
        long bestPhotoFileId = 0;
        PhotoEntry bestPhotoEntry = null;
        boolean foundSuperPrimaryPhoto = false;
        int photoPriority = -1;
        int totalRowCount = 0;
        int contactSendToVoicemail = 0;
        String contactCustomRingtone = null;
        long contactLastTimeContacted = 0;
        int contactTimesContacted = 0;
        int contactStarred = 0;
        int contactPinned = Integer.MAX_VALUE;
        int hasPhoneNumber = 0;
        StringBuilder lookupKey = new StringBuilder();

        /** M: New Feature Gemini added @{ */
/*        int contactSendToVoicemailVT = 0;
        int contactSendToVoicemailSIP = 0;
        int simIndicater = -1;
        int indexInSim = -1;
        int isSdnContact = 0;
        /** @} */

 /*       mDisplayNameCandidate.clear();

        Cursor c = db.rawQuery(sql, sqlArgs);
        try {
            while (c.moveToNext()) {
                long rawContactId = c.getLong(RawContactsQuery.RAW_CONTACT_ID);
                if (rawContactId != currentRawContactId) {
                    currentRawContactId = rawContactId;
                    totalRowCount++;

                    // Assemble sub-account.
                    String accountType = c.getString(RawContactsQuery.ACCOUNT_TYPE);
                    String dataSet = c.getString(RawContactsQuery.DATA_SET);
                    String accountWithDataSet = (!TextUtils.isEmpty(dataSet))
                            ? accountType + "/" + dataSet
                            : accountType;

                    // Display name
                    String displayName = c.getString(RawContactsQuery.DISPLAY_NAME);
                    int displayNameSource = c.getInt(RawContactsQuery.DISPLAY_NAME_SOURCE);
                    int nameVerified = c.getInt(RawContactsQuery.NAME_VERIFIED);
                    processDisplayNameCandidate(rawContactId, displayName, displayNameSource,
                            mContactsProvider.isWritableAccountWithDataSet(accountWithDataSet),
                            nameVerified != 0);

                    // Contact options
                    if (!c.isNull(RawContactsQuery.SEND_TO_VOICEMAIL)) {
                        boolean sendToVoicemail =
                                (c.getInt(RawContactsQuery.SEND_TO_VOICEMAIL) != 0);
                        if (sendToVoicemail) {
                            contactSendToVoicemail++;
                        }
                    }

                    if (contactCustomRingtone == null
                            && !c.isNull(RawContactsQuery.CUSTOM_RINGTONE)) {
                        contactCustomRingtone = c.getString(RawContactsQuery.CUSTOM_RINGTONE);
                    }

                    long lastTimeContacted = c.getLong(RawContactsQuery.LAST_TIME_CONTACTED);
                    if (lastTimeContacted > contactLastTimeContacted) {
                        contactLastTimeContacted = lastTimeContacted;
                    }

                    int timesContacted = c.getInt(RawContactsQuery.TIMES_CONTACTED);
                    if (timesContacted > contactTimesContacted) {
                        contactTimesContacted = timesContacted;
                    }

                    if (c.getInt(RawContactsQuery.STARRED) != 0) {
                        contactStarred = 1;
                    }

                    // contactPinned should be the lowest value of its constituent raw contacts,
                    // excluding negative integers
                    final int rawContactPinned = c.getInt(RawContactsQuery.PINNED);
                    if (rawContactPinned > PinnedPositions.UNPINNED) {
                        contactPinned = Math.min(contactPinned, rawContactPinned);
                    }

                    /** M: New Feature Gemini added @{ */
 /*                   if (!c.isNull(RawContactsQuery.SEND_TO_VOICEMAIL_VT)) {
                        boolean sendToVoicemailVT =
                                (c.getInt(RawContactsQuery.SEND_TO_VOICEMAIL_VT) != 0);
                        if (sendToVoicemailVT) {
                            contactSendToVoicemailVT++;
                        }
                    }

                    if (!c.isNull(RawContactsQuery.SEND_TO_VOICEMAIL_SIP)) {
                        boolean sendToVoicemailSIP =
                                (c.getInt(RawContactsQuery.SEND_TO_VOICEMAIL_SIP) != 0);
                        if (sendToVoicemailSIP) {
                            contactSendToVoicemailSIP++;
                        }
                    }

                    if (simIndicater == -1
                            && !c.isNull(RawContactsQuery.INDICATE_PHONE_SIM)) {
                        simIndicater = c
                                .getInt(RawContactsQuery.INDICATE_PHONE_SIM);
                    }

                    if (isSdnContact == 0 && !c.isNull(RawContactsQuery.IS_SDN_CONTACT)) {
                        isSdnContact = c.getInt(RawContactsQuery.IS_SDN_CONTACT);
                    }

                    // index in SIM
                    if (indexInSim == -1
                            && !c.isNull(RawContactsQuery.INDEX_IN_SIM)) {
                        indexInSim = c.getInt(RawContactsQuery.INDEX_IN_SIM);
                    }
                    /** @} */

 /*                   appendLookupKey(
                            lookupKey,
                            accountWithDataSet,
                            c.getString(RawContactsQuery.ACCOUNT_NAME),
                            rawContactId,
                            c.getString(RawContactsQuery.SOURCE_ID),
                            displayName);
                }

                if (!c.isNull(RawContactsQuery.DATA_ID)) {
                    long dataId = c.getLong(RawContactsQuery.DATA_ID);
                    long photoFileId = c.getLong(RawContactsQuery.PHOTO_FILE_ID);
                    int mimetypeId = c.getInt(RawContactsQuery.MIMETYPE_ID);
                    boolean superPrimary = c.getInt(RawContactsQuery.IS_SUPER_PRIMARY) != 0;
                    if (mimetypeId == mMimeTypeIdPhoto) {
                        if (!foundSuperPrimaryPhoto) {
                            // Lookup the metadata for the photo, if available.  Note that data set
                            // does not come into play here, since accounts are looked up in the
                            // account manager in the priority resolver.
                            PhotoEntry photoEntry = getPhotoMetadata(db, photoFileId);
                            String accountType = c.getString(RawContactsQuery.ACCOUNT_TYPE);
                            int priority = mPhotoPriorityResolver.getPhotoPriority(accountType);
                            if (superPrimary || hasHigherPhotoPriority(
                                    photoEntry, priority, bestPhotoEntry, photoPriority)) {
                                bestPhotoEntry = photoEntry;
                                photoPriority = priority;
                                bestPhotoId = dataId;
                                bestPhotoFileId = photoFileId;
                                foundSuperPrimaryPhoto |= superPrimary;
                            }
                        }
                    } else if (mimetypeId == mMimeTypeIdPhone) {
                        hasPhoneNumber = 1;
                    }
                }
            }
        } finally {
            c.close();
        }

        if (contactPinned == Integer.MAX_VALUE) {
            contactPinned = PinnedPositions.UNPINNED;
        }

        statement.bindLong(ContactReplaceSqlStatement.NAME_RAW_CONTACT_ID,
                mDisplayNameCandidate.rawContactId);

        if (bestPhotoId != -1) {
            statement.bindLong(ContactReplaceSqlStatement.PHOTO_ID, bestPhotoId);
        } else {
            statement.bindNull(ContactReplaceSqlStatement.PHOTO_ID);
        }

        if (bestPhotoFileId != 0) {
            statement.bindLong(ContactReplaceSqlStatement.PHOTO_FILE_ID, bestPhotoFileId);
        } else {
            statement.bindNull(ContactReplaceSqlStatement.PHOTO_FILE_ID);
        }

        statement.bindLong(ContactReplaceSqlStatement.SEND_TO_VOICEMAIL,
                totalRowCount == contactSendToVoicemail ? 1 : 0);

        DatabaseUtils.bindObjectToProgram(statement, ContactReplaceSqlStatement.CUSTOM_RINGTONE,
                contactCustomRingtone);
        statement.bindLong(ContactReplaceSqlStatement.LAST_TIME_CONTACTED,
                contactLastTimeContacted);
        statement.bindLong(ContactReplaceSqlStatement.TIMES_CONTACTED,
                contactTimesContacted);
        statement.bindLong(ContactReplaceSqlStatement.STARRED,
                contactStarred);
        statement.bindLong(ContactReplaceSqlStatement.PINNED,
                contactPinned);
        statement.bindLong(ContactReplaceSqlStatement.HAS_PHONE_NUMBER,
                hasPhoneNumber);
        statement.bindString(ContactReplaceSqlStatement.LOOKUP_KEY,
                Uri.encode(lookupKey.toString()));
        statement.bindLong(ContactReplaceSqlStatement.CONTACT_LAST_UPDATED_TIMESTAMP,
                Clock.getInstance().currentTimeMillis());

        /** M: New Feature Gemini added @{ */
 /*       statement.bindLong(ContactReplaceSqlStatement.SEND_TO_VOICEMAIL_VT,
                totalRowCount == contactSendToVoicemailVT ? 1 : 0);
        statement.bindLong(ContactReplaceSqlStatement.SEND_TO_VOICEMAIL_SIP,
                totalRowCount == contactSendToVoicemailSIP ? 1 : 0);
        statement.bindLong(ContactReplaceSqlStatement.INDICATE_PHONE_SIM,
                simIndicater);
        statement.bindLong(ContactReplaceSqlStatement.INDEX_IN_SIM, indexInSim);
        statement.bindLong(ContactReplaceSqlStatement.IS_SDN_CONTACTS, isSdnContact);
        /** @} */
//    }

    /**
     * Uses the supplied values to determine if they represent a "better" display name
     * for the aggregate contact currently evaluated.  If so, it updates
     * {@link #mDisplayNameCandidate} with the new values.
     */
/*    private void processDisplayNameCandidate(long rawContactId, String displayName,
            int displayNameSource, boolean writableAccount, boolean verified) {

        boolean replace = false;
        if (mDisplayNameCandidate.rawContactId == -1) {
            // No previous values available
            replace = true;
        } else if (!TextUtils.isEmpty(displayName)) {
            if (!mDisplayNameCandidate.verified && verified) {
                // A verified name is better than any other name
                replace = true;
            } else if (mDisplayNameCandidate.verified == verified) {
                if (mDisplayNameCandidate.displayNameSource < displayNameSource) {
                    // New values come from an superior source, e.g. structured name vs phone number
                    replace = true;
                } else if (mDisplayNameCandidate.displayNameSource == displayNameSource) {
                    if (!mDisplayNameCandidate.writableAccount && writableAccount) {
                        replace = true;
                    } else if (mDisplayNameCandidate.writableAccount == writableAccount) {
                        if (NameNormalizer.compareComplexity(displayName,
                                mDisplayNameCandidate.displayName) > 0) {
                            // New name is more complex than the previously found one
                            replace = true;
                        }
                    }
                }
            }
        }

        if (replace) {
            mDisplayNameCandidate.rawContactId = rawContactId;
            mDisplayNameCandidate.displayName = displayName;
            mDisplayNameCandidate.displayNameSource = displayNameSource;
            mDisplayNameCandidate.verified = verified;
            mDisplayNameCandidate.writableAccount = writableAccount;
        }
    }

    private interface PhotoIdQuery {
        final String[] COLUMNS = new String[] {
            AccountsColumns.CONCRETE_ACCOUNT_TYPE,
            DataColumns.CONCRETE_ID,
            Data.IS_SUPER_PRIMARY,
            Photo.PHOTO_FILE_ID,
        };

        int ACCOUNT_TYPE = 0;
        int DATA_ID = 1;
        int IS_SUPER_PRIMARY = 2;
        int PHOTO_FILE_ID = 3;
    }

    public void updatePhotoId(SQLiteDatabase db, long rawContactId) {

        long contactId = mDbHelper.getContactId(rawContactId);
        if (contactId == 0) {
            return;
        }

        long bestPhotoId = -1;
        long bestPhotoFileId = 0;
        int photoPriority = -1;

        long photoMimeType = mDbHelper.getMimeTypeId(Photo.CONTENT_ITEM_TYPE);

        String tables = Tables.RAW_CONTACTS
                + " JOIN " + Tables.ACCOUNTS + " ON ("
                    + AccountsColumns.CONCRETE_ID + "=" + RawContactsColumns.CONCRETE_ACCOUNT_ID
                    + ")"
                + " JOIN " + Tables.DATA + " ON("
                + DataColumns.CONCRETE_RAW_CONTACT_ID + "=" + RawContactsColumns.CONCRETE_ID
                + " AND (" + DataColumns.MIMETYPE_ID + "=" + photoMimeType + " AND "
                        + Photo.PHOTO + " NOT NULL))";

        mSelectionArgs1[0] = String.valueOf(contactId);
        final Cursor c = db.query(tables, PhotoIdQuery.COLUMNS,
                RawContacts.CONTACT_ID + "=?", mSelectionArgs1, null, null, null);
        try {
            PhotoEntry bestPhotoEntry = null;
            while (c.moveToNext()) {
                long dataId = c.getLong(PhotoIdQuery.DATA_ID);
                long photoFileId = c.getLong(PhotoIdQuery.PHOTO_FILE_ID);
                boolean superPrimary = c.getInt(PhotoIdQuery.IS_SUPER_PRIMARY) != 0;
                PhotoEntry photoEntry = getPhotoMetadata(db, photoFileId);

                // Note that data set does not come into play here, since accounts are looked up in
                // the account manager in the priority resolver.
                String accountType = c.getString(PhotoIdQuery.ACCOUNT_TYPE);
                int priority = mPhotoPriorityResolver.getPhotoPriority(accountType);
                if (superPrimary || hasHigherPhotoPriority(
                        photoEntry, priority, bestPhotoEntry, photoPriority)) {
                    bestPhotoEntry = photoEntry;
                    photoPriority = priority;
                    bestPhotoId = dataId;
                    bestPhotoFileId = photoFileId;
                    if (superPrimary) {
                        break;
                    }
                }
            }
        } finally {
            c.close();
        }

        if (bestPhotoId == -1) {
            mPhotoIdUpdate.bindNull(1);
        } else {
            mPhotoIdUpdate.bindLong(1, bestPhotoId);
        }

        if (bestPhotoFileId == 0) {
            mPhotoIdUpdate.bindNull(2);
        } else {
            mPhotoIdUpdate.bindLong(2, bestPhotoFileId);
        }

        mPhotoIdUpdate.bindLong(3, contactId);
        mPhotoIdUpdate.execute();
    }

    private interface PhotoFileQuery {
        final String[] COLUMNS = new String[] {
                PhotoFiles.HEIGHT,
                PhotoFiles.WIDTH,
                PhotoFiles.FILESIZE
        };

        int HEIGHT = 0;
        int WIDTH = 1;
        int FILESIZE = 2;
    }

    private class PhotoEntry implements Comparable<PhotoEntry> {
        // Pixel count (width * height) for the image.
        final int pixelCount;

        // File size (in bytes) of the image.  Not populated if the image is a thumbnail.
        final int fileSize;

        private PhotoEntry(int pixelCount, int fileSize) {
            this.pixelCount = pixelCount;
            this.fileSize = fileSize;
        }

        @Override
        public int compareTo(PhotoEntry pe) {
            if (pe == null) {
                return -1;
            }
            if (pixelCount == pe.pixelCount) {
                return pe.fileSize - fileSize;
            } else {
                return pe.pixelCount - pixelCount;
            }
        }
    }

    private PhotoEntry getPhotoMetadata(SQLiteDatabase db, long photoFileId) {
        if (photoFileId == 0) {
            // Assume standard thumbnail size.  Don't bother getting a file size for priority;
            // we should fall back to photo priority resolver if all we have are thumbnails.
            int thumbDim = mContactsProvider.getMaxThumbnailDim();
            return new PhotoEntry(thumbDim * thumbDim, 0);
        } else {
            Cursor c = db.query(Tables.PHOTO_FILES, PhotoFileQuery.COLUMNS, PhotoFiles._ID + "=?",
                    new String[]{String.valueOf(photoFileId)}, null, null, null);
            try {
                if (c.getCount() == 1) {
                    c.moveToFirst();
                    int pixelCount =
                            c.getInt(PhotoFileQuery.HEIGHT) * c.getInt(PhotoFileQuery.WIDTH);
                    return new PhotoEntry(pixelCount, c.getInt(PhotoFileQuery.FILESIZE));
                }
            } finally {
                c.close();
            }
        }
        return new PhotoEntry(0, 0);
    }

    private interface DisplayNameQuery {
        String[] COLUMNS = new String[] {
            RawContacts._ID,
            RawContactsColumns.DISPLAY_NAME,
            RawContactsColumns.DISPLAY_NAME_SOURCE,
            RawContacts.NAME_VERIFIED,
            RawContacts.SOURCE_ID,
            RawContacts.ACCOUNT_TYPE_AND_DATA_SET,
        };

        int _ID = 0;
        int DISPLAY_NAME = 1;
        int DISPLAY_NAME_SOURCE = 2;
        int NAME_VERIFIED = 3;
        int SOURCE_ID = 4;
        int ACCOUNT_TYPE_AND_DATA_SET = 5;
    }

    public void updateDisplayNameForRawContact(SQLiteDatabase db, long rawContactId) {
        long contactId = mDbHelper.getContactId(rawContactId);
        if (contactId == 0) {
            return;
        }

        updateDisplayNameForContact(db, contactId);
    }

    public void updateDisplayNameForContact(SQLiteDatabase db, long contactId) {
        boolean lookupKeyUpdateNeeded = false;

        mDisplayNameCandidate.clear();

        mSelectionArgs1[0] = String.valueOf(contactId);
        final Cursor c = db.query(Views.RAW_CONTACTS, DisplayNameQuery.COLUMNS,
                RawContacts.CONTACT_ID + "=?", mSelectionArgs1, null, null, null);
        try {
            while (c.moveToNext()) {
                long rawContactId = c.getLong(DisplayNameQuery._ID);
                String displayName = c.getString(DisplayNameQuery.DISPLAY_NAME);
                int displayNameSource = c.getInt(DisplayNameQuery.DISPLAY_NAME_SOURCE);
                int nameVerified = c.getInt(DisplayNameQuery.NAME_VERIFIED);
                String accountTypeAndDataSet = c.getString(
                        DisplayNameQuery.ACCOUNT_TYPE_AND_DATA_SET);
                processDisplayNameCandidate(rawContactId, displayName, displayNameSource,
                        mContactsProvider.isWritableAccountWithDataSet(accountTypeAndDataSet),
                        nameVerified != 0);

                // If the raw contact has no source id, the lookup key is based on the display
                // name, so the lookup key needs to be updated.
                lookupKeyUpdateNeeded |= c.isNull(DisplayNameQuery.SOURCE_ID);
            }
        } finally {
            c.close();
        }

        if (mDisplayNameCandidate.rawContactId != -1) {
            mDisplayNameUpdate.bindLong(1, mDisplayNameCandidate.rawContactId);
            mDisplayNameUpdate.bindLong(2, contactId);
            mDisplayNameUpdate.execute();
        }

        if (lookupKeyUpdateNeeded) {
            updateLookupKeyForContact(db, contactId);
        }
    }


    /**
     * Updates the {@link Contacts#HAS_PHONE_NUMBER} flag for the aggregate contact containing the
     * specified raw contact.
     */
/*    public void updateHasPhoneNumber(SQLiteDatabase db, long rawContactId) {

        long contactId = mDbHelper.getContactId(rawContactId);
        if (contactId == 0) {
            return;
        }

        final SQLiteStatement hasPhoneNumberUpdate = db.compileStatement(
                "UPDATE " + Tables.CONTACTS +
                " SET " + Contacts.HAS_PHONE_NUMBER + "="
                        + "(SELECT (CASE WHEN COUNT(*)=0 THEN 0 ELSE 1 END)"
                        + " FROM " + Tables.DATA_JOIN_RAW_CONTACTS
                        + " WHERE " + DataColumns.MIMETYPE_ID + "=?"
                                + " AND " + Phone.NUMBER + " NOT NULL"
                                + " AND " + RawContacts.CONTACT_ID + "=?)" +
                " WHERE " + Contacts._ID + "=?");
        try {
            hasPhoneNumberUpdate.bindLong(1, mDbHelper.getMimeTypeId(Phone.CONTENT_ITEM_TYPE));
            hasPhoneNumberUpdate.bindLong(2, contactId);
            hasPhoneNumberUpdate.bindLong(3, contactId);
            hasPhoneNumberUpdate.execute();
        } finally {
            hasPhoneNumberUpdate.close();
        }
    }

    private interface LookupKeyQuery {
        String TABLE = Views.RAW_CONTACTS;
        String[] COLUMNS = new String[] {
            RawContacts._ID,
            RawContactsColumns.DISPLAY_NAME,
            RawContacts.ACCOUNT_TYPE_AND_DATA_SET,
            RawContacts.ACCOUNT_NAME,
            RawContacts.SOURCE_ID,
        };

        int ID = 0;
        int DISPLAY_NAME = 1;
        int ACCOUNT_TYPE_AND_DATA_SET = 2;
        int ACCOUNT_NAME = 3;
        int SOURCE_ID = 4;
    }

    public void updateLookupKeyForRawContact(SQLiteDatabase db, long rawContactId) {
        long contactId = mDbHelper.getContactId(rawContactId);
        if (contactId == 0) {
            return;
        }

        updateLookupKeyForContact(db, contactId);
    }

    private void updateLookupKeyForContact(SQLiteDatabase db, long contactId) {
        String lookupKey = computeLookupKeyForContact(db, contactId);

        if (lookupKey == null) {
            mLookupKeyUpdate.bindNull(1);
        } else {
            mLookupKeyUpdate.bindString(1, Uri.encode(lookupKey));
        }
        mLookupKeyUpdate.bindLong(2, contactId);

        mLookupKeyUpdate.execute();
    }

    protected String computeLookupKeyForContact(SQLiteDatabase db, long contactId) {
        StringBuilder sb = new StringBuilder();
        mSelectionArgs1[0] = String.valueOf(contactId);
        final Cursor c = db.query(LookupKeyQuery.TABLE, LookupKeyQuery.COLUMNS,
                RawContacts.CONTACT_ID + "=?", mSelectionArgs1, null, null, RawContacts._ID);
        try {
            while (c.moveToNext()) {
                ContactLookupKey.appendToLookupKey(sb,
                        c.getString(LookupKeyQuery.ACCOUNT_TYPE_AND_DATA_SET),
                        c.getString(LookupKeyQuery.ACCOUNT_NAME),
                        c.getLong(LookupKeyQuery.ID),
                        c.getString(LookupKeyQuery.SOURCE_ID),
                        c.getString(LookupKeyQuery.DISPLAY_NAME));
            }
        } finally {
            c.close();
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /**
     * Execute {@link SQLiteStatement} that will update the
     * {@link Contacts#STARRED} flag for the given {@link RawContacts#_ID}.
     */
/*    public void updateStarred(long rawContactId) {
        long contactId = mDbHelper.getContactId(rawContactId);
        if (contactId == 0) {
            return;
        }

        mStarredUpdate.bindLong(1, contactId);
        mStarredUpdate.execute();
    }

    /**
     * Execute {@link SQLiteStatement} that will update the
     * {@link Contacts#PINNED} flag for the given {@link RawContacts#_ID}.
     */
 /*   public void updatePinned(long rawContactId) {
        long contactId = mDbHelper.getContactId(rawContactId);
        if (contactId == 0) {
            return;
        }
        mPinnedUpdate.bindLong(1, contactId);
        mPinnedUpdate.execute();
    }

    /**
     * Finds matching contacts and returns a cursor on those.
     */
 /*   public Cursor queryAggregationSuggestions(SQLiteQueryBuilder qb,
            String[] projection, long contactId, int maxSuggestions, String filter,
            ArrayList<AggregationSuggestionParameter> parameters) {
        final SQLiteDatabase db = mDbHelper.getReadableDatabase();
        db.beginTransaction();
        try {
            List<MatchScore> bestMatches = findMatchingContacts(db, contactId, parameters);
            return queryMatchingContacts(qb, db, projection, bestMatches, maxSuggestions, filter);
        } finally {
            db.endTransaction();
        }
    }

    private interface ContactIdQuery {
        String[] COLUMNS = new String[] {
            Contacts._ID
        };

        int _ID = 0;
    }

    /**
     * Loads contacts with specified IDs and returns them in the order of IDs in the
     * supplied list.
     */
 /*   private Cursor queryMatchingContacts(SQLiteQueryBuilder qb, SQLiteDatabase db,
            String[] projection, List<MatchScore> bestMatches, int maxSuggestions, String filter) {
        StringBuilder sb = new StringBuilder();
        sb.append(Contacts._ID);
        sb.append(" IN (");
        for (int i = 0; i < bestMatches.size(); i++) {
            MatchScore matchScore = bestMatches.get(i);
            if (i != 0) {
                sb.append(",");
            }
            sb.append(matchScore.getContactId());
        }
        sb.append(")");

        if (!TextUtils.isEmpty(filter)) {
            sb.append(" AND " + Contacts._ID + " IN ");
            mContactsProvider.appendContactFilterAsNestedQuery(sb, filter);
        }

        // Run a query and find ids of best matching contacts satisfying the filter (if any)
        HashSet<Long> foundIds = new HashSet<Long>();
        Cursor cursor = db.query(qb.getTables(), ContactIdQuery.COLUMNS, sb.toString(),
                null, null, null, null);
        try {
            while(cursor.moveToNext()) {
                foundIds.add(cursor.getLong(ContactIdQuery._ID));
            }
        } finally {
            cursor.close();
        }

        // Exclude all contacts that did not match the filter
        Iterator<MatchScore> iter = bestMatches.iterator();
        while (iter.hasNext()) {
            long id = iter.next().getContactId();
            if (!foundIds.contains(id)) {
                iter.remove();
            }
        }

        // Limit the number of returned suggestions
        final List<MatchScore> limitedMatches;
        if (bestMatches.size() > maxSuggestions) {
            limitedMatches = bestMatches.subList(0, maxSuggestions);
        } else {
            limitedMatches = bestMatches;
        }

        // Build an in-clause with the remaining contact IDs
        sb.setLength(0);
        sb.append(Contacts._ID);
        sb.append(" IN (");
        for (int i = 0; i < limitedMatches.size(); i++) {
            MatchScore matchScore = limitedMatches.get(i);
            if (i != 0) {
                sb.append(",");
            }
            sb.append(matchScore.getContactId());
        }
        sb.append(")");

        // Run the final query with the required projection and contact IDs found by the first query
        cursor = qb.query(db, projection, sb.toString(), null, null, null, Contacts._ID);

        // Build a sorted list of discovered IDs
        ArrayList<Long> sortedContactIds = new ArrayList<Long>(limitedMatches.size());
        for (MatchScore matchScore : limitedMatches) {
            sortedContactIds.add(matchScore.getContactId());
        }

        Collections.sort(sortedContactIds);

        // Map cursor indexes according to the descending order of match scores
        int[] positionMap = new int[limitedMatches.size()];
        for (int i = 0; i < positionMap.length; i++) {
            long id = limitedMatches.get(i).getContactId();
            positionMap[i] = sortedContactIds.indexOf(id);
        }

        return new ReorderingCursorWrapper(cursor, positionMap);
    }
*/

    private interface ContactIdQuery {
        String[] COLUMNS = new String[] {
            Contacts._ID
        };

        int _ID = 0;
    }
    /**
     * Finds contacts with data matches and returns a list of {@link MatchScore}'s in the
     * descending order of match score.
     * @param parameters
     */
     protected List<MatchScore> findMatchingContacts(final SQLiteDatabase db, long contactId,
            ArrayList<AggregationSuggestionParameter> parameters) {

        MatchCandidateList candidates = new MatchCandidateList();
        ContactMatcher matcher = new ContactMatcher();

        // Don't aggregate a contact with itself
        matcher.keepOut(contactId);

        if (parameters == null || parameters.size() == 0) {
            final Cursor c = db.query(RawContactIdQuery.TABLE, RawContactIdQuery.COLUMNS,
                    RawContacts.CONTACT_ID + "=" + contactId, null, null, null, null);
            try {
                while (c.moveToNext()) {
                    long rawContactId = c.getLong(RawContactIdQuery.RAW_CONTACT_ID);
                    updateMatchScoresForSuggestionsBasedOnDataMatches(db, rawContactId, candidates,
                            matcher);
                }
            } finally {
                c.close();
            }
        } else {
            updateMatchScoresForSuggestionsBasedOnDataMatches(db, candidates,
                    matcher, parameters);
        }

        return matcher.pickBestMatches(ContactMatcher.SCORE_THRESHOLD_SUGGEST);
    }

    /**
     * Computes scores for contacts that have matching data rows.
     */
    private void updateMatchScoresForSuggestionsBasedOnDataMatches(SQLiteDatabase db,
            long rawContactId, MatchCandidateList candidates, ContactMatcher matcher) {

        updateMatchScoresBasedOnIdentityMatch(db, rawContactId, matcher);
        updateMatchScoresBasedOnNameMatches(db, rawContactId, matcher);
        updateMatchScoresBasedOnEmailMatches(db, rawContactId, matcher);
        updateMatchScoresBasedOnPhoneMatches(db, rawContactId, matcher);
        loadNameMatchCandidates(db, rawContactId, candidates, false);
        lookupApproximateNameMatches(db, candidates, matcher);
    }

    private void updateMatchScoresForSuggestionsBasedOnDataMatches(SQLiteDatabase db,
            MatchCandidateList candidates, ContactMatcher matcher,
            ArrayList<AggregationSuggestionParameter> parameters) {
        for (AggregationSuggestionParameter parameter : parameters) {
            if (AggregationSuggestions.PARAMETER_MATCH_NAME.equals(parameter.kind)) {
                updateMatchScoresBasedOnNameMatches(db, parameter.value, candidates, matcher);
            }

            // TODO: add support for other parameter kinds
        }
    }

    /// M: The following lines are provided and maintained by Mediatek inc. @{
    public Cursor queryAggregationSuggestions(SQLiteQueryBuilder qb, String[] projection,
            long contactId, int maxSuggestions, String filter,
            ArrayList<AggregationSuggestionParameter> parameters, String where) {
        final SQLiteDatabase db = mDbHelper.getReadableDatabase();
        db.beginTransaction();
        try {
            List<MatchScore> bestMatches = findMatchingContacts(db, contactId, parameters);
            return queryMatchingContacts(qb, db, projection, bestMatches, maxSuggestions,
                    filter, where);
        } finally {
            db.endTransaction();
        }

    }

    private Cursor queryMatchingContacts(SQLiteQueryBuilder qb, SQLiteDatabase db,
            String[] projection, List<MatchScore> bestMatches, int maxSuggestions, String filter,
            String where) {

        StringBuilder sb = new StringBuilder();
        sb.append(Contacts._ID);
        sb.append(" IN (");
        for (int i = 0; i < bestMatches.size(); i++) {
            MatchScore matchScore = bestMatches.get(i);
            if (i != 0) {
                sb.append(",");
            }
            sb.append(matchScore.getContactId());
        }
        sb.append(")");

        if (!TextUtils.isEmpty(where)) {
            sb.append(" AND ");
            sb.append(where);
        }

        if (!TextUtils.isEmpty(filter)) {
            sb.append(" AND " + Contacts._ID + " IN ");
            mContactsProvider.appendContactFilterAsNestedQuery(sb, filter);
        }

        // Run a query and find ids of best matching contacts satisfying the filter (if any)
        HashSet<Long> foundIds = new HashSet<Long>();
        Cursor cursor = db.query(qb.getTables(), ContactIdQuery.COLUMNS, sb.toString(),
                null, null, null, null);
        try {
            while (cursor.moveToNext()) {
                foundIds.add(cursor.getLong(ContactIdQuery._ID));
            }
        } finally {
            cursor.close();
        }

        // Exclude all contacts that did not match the filter
        Iterator<MatchScore> iter = bestMatches.iterator();
        while (iter.hasNext()) {
            long id = iter.next().getContactId();
            if (!foundIds.contains(id)) {
                iter.remove();
            }
        }

        // Limit the number of returned suggestions
        if (bestMatches.size() > maxSuggestions) {
            bestMatches = bestMatches.subList(0, maxSuggestions);
        }

        // Build an in-clause with the remaining contact IDs
        sb.setLength(0);
        sb.append(Contacts._ID);
        sb.append(" IN (");
        for (int i = 0; i < bestMatches.size(); i++) {
            MatchScore matchScore = bestMatches.get(i);
            if (i != 0) {
                sb.append(",");
            }
            sb.append(matchScore.getContactId());
        }
        sb.append(")");

        // Run the final query with the required projection and contact IDs found by the first query
        cursor = qb.query(db, projection, sb.toString(), null, null, null, Contacts._ID);

        // Build a sorted list of discovered IDs
        ArrayList<Long> sortedContactIds = new ArrayList<Long>(bestMatches.size());
        for (MatchScore matchScore : bestMatches) {
            sortedContactIds.add(matchScore.getContactId());
        }

        Collections.sort(sortedContactIds);

        // Map cursor indexes according to the descending order of match scores
        int[] positionMap = new int[bestMatches.size()];
        for (int i = 0; i < positionMap.length; i++) {
            long id = bestMatches.get(i).getContactId();
            positionMap[i] = sortedContactIds.indexOf(id);
        }

        return new ReorderingCursorWrapper(cursor, positionMap);
    }
    /// @}
}
