/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.dialer.calllog;

import static com.google.common.collect.Lists.newArrayList;

import android.database.MatrixCursor;
import android.provider.CallLog.Calls;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.List;

/**
 * Unit tests for {@link CallLogGroupBuilder}
 */
@SmallTest
public class CallLogGroupBuilderTest extends AndroidTestCase {
    /** A phone number for testing. */
    private static final String TEST_NUMBER1 = "14125551234";
    /** A phone number for testing. */
    private static final String TEST_NUMBER2 = "14125555555";

    /** The object under test. */
    private CallLogGroupBuilder mBuilder;
    /** Records the created groups. */
    private FakeGroupCreator mFakeGroupCreator;
    /** Cursor to store the values. */
    private MatrixCursor mCursor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mFakeGroupCreator = new FakeGroupCreator();
        mBuilder = new CallLogGroupBuilder(mFakeGroupCreator);
        createCursor();
    }

    @Override
    protected void tearDown() throws Exception {
        mCursor = null;
        mBuilder = null;
        mFakeGroupCreator = null;
        super.tearDown();
    }

    public void testAddGroups_NoCalls() {
        mBuilder.addGroups(mCursor);
        assertEquals(0, mFakeGroupCreator.groups.size());
    }

    public void testAddGroups_OneCall() {
        addCallLogEntry(TEST_NUMBER1, Calls.INCOMING_TYPE);
        mBuilder.addGroups(mCursor);
        assertEquals(0, mFakeGroupCreator.groups.size());
    }

    public void testAddGroups_TwoCallsNotMatching() {
        addCallLogEntry(TEST_NUMBER1, Calls.INCOMING_TYPE);
        addCallLogEntry(TEST_NUMBER2, Calls.INCOMING_TYPE);
        mBuilder.addGroups(mCursor);
        assertEquals(0, mFakeGroupCreator.groups.size());
    }

    public void testAddGroups_ThreeCallsMatching() {
        addCallLogEntry(TEST_NUMBER1, Calls.INCOMING_TYPE);
        addCallLogEntry(TEST_NUMBER1, Calls.INCOMING_TYPE);
        addCallLogEntry(TEST_NUMBER1, Calls.INCOMING_TYPE);
        mBuilder.addGroups(mCursor);
        assertEquals(1, mFakeGroupCreator.groups.size());
        assertGroupIs(0, 3, false, mFakeGroupCreator.groups.get(0));
    }

    public void testAddGroups_MatchingIncomingAndOutgoing() {
        addCallLogEntry(TEST_NUMBER1, Calls.INCOMING_TYPE);
        addCallLogEntry(TEST_NUMBER1, Calls.OUTGOING_TYPE);
        addCallLogEntry(TEST_NUMBER1, Calls.INCOMING_TYPE);
        mBuilder.addGroups(mCursor);
        assertEquals(1, mFakeGroupCreator.groups.size());
        assertGroupIs(0, 3, false, mFakeGroupCreator.groups.get(0));
    }

    public void testAddGroups_Voicemail() {
        // Does not group with other types of calls, include voicemail themselves.
        assertCallsAreNotGrouped(Calls.VOICEMAIL_TYPE, Calls.MISSED_TYPE);
        //assertCallsAreNotGrouped(Calls.VOICEMAIL_TYPE, Calls.MISSED_TYPE, Calls.MISSED_TYPE);
        assertCallsAreNotGrouped(Calls.VOICEMAIL_TYPE, Calls.VOICEMAIL_TYPE);
        assertCallsAreNotGrouped(Calls.VOICEMAIL_TYPE, Calls.INCOMING_TYPE);
        assertCallsAreNotGrouped(Calls.VOICEMAIL_TYPE, Calls.OUTGOING_TYPE);
    }

    public void testAddGroups_Missed() {
        // Groups with one or more missed calls.
        assertCallsAreGrouped(Calls.MISSED_TYPE, Calls.MISSED_TYPE);
        assertCallsAreGrouped(Calls.MISSED_TYPE, Calls.MISSED_TYPE, Calls.MISSED_TYPE);
        // Does not group with other types of calls.
        assertCallsAreNotGrouped(Calls.MISSED_TYPE, Calls.VOICEMAIL_TYPE);
        assertCallsAreGrouped(Calls.MISSED_TYPE, Calls.INCOMING_TYPE);
        assertCallsAreGrouped(Calls.MISSED_TYPE, Calls.OUTGOING_TYPE);
    }

    public void testAddGroups_Incoming() {
        // Groups with one or more incoming or outgoing.
        assertCallsAreGrouped(Calls.INCOMING_TYPE, Calls.INCOMING_TYPE);
        assertCallsAreGrouped(Calls.INCOMING_TYPE, Calls.OUTGOING_TYPE);
        assertCallsAreGrouped(Calls.INCOMING_TYPE, Calls.INCOMING_TYPE, Calls.OUTGOING_TYPE);
        assertCallsAreGrouped(Calls.INCOMING_TYPE, Calls.OUTGOING_TYPE, Calls.INCOMING_TYPE);
        assertCallsAreGrouped(Calls.INCOMING_TYPE, Calls.MISSED_TYPE);
        // Does not group with voicemail and missed calls.
        assertCallsAreNotGrouped(Calls.INCOMING_TYPE, Calls.VOICEMAIL_TYPE);
    }

    public void testAddGroups_Outgoing() {
        // Groups with one or more incoming or outgoing.
        assertCallsAreGrouped(Calls.OUTGOING_TYPE, Calls.INCOMING_TYPE);
        assertCallsAreGrouped(Calls.OUTGOING_TYPE, Calls.OUTGOING_TYPE);
        assertCallsAreGrouped(Calls.OUTGOING_TYPE, Calls.INCOMING_TYPE, Calls.OUTGOING_TYPE);
        assertCallsAreGrouped(Calls.OUTGOING_TYPE, Calls.OUTGOING_TYPE, Calls.INCOMING_TYPE);
        assertCallsAreGrouped(Calls.INCOMING_TYPE, Calls.MISSED_TYPE);
        // Does not group with voicemail and missed calls.
        assertCallsAreNotGrouped(Calls.INCOMING_TYPE, Calls.VOICEMAIL_TYPE);
    }

    public void testAddGroups_Mixed() {
        addMultipleCallLogEntries(TEST_NUMBER1,
                Calls.VOICEMAIL_TYPE,  // Stand-alone
                Calls.INCOMING_TYPE,  // Group 1: 1-4
                Calls.OUTGOING_TYPE,
                Calls.MISSED_TYPE,
                Calls.MISSED_TYPE,
                Calls.VOICEMAIL_TYPE,  // Stand-alone
                Calls.INCOMING_TYPE,  // Stand-alone
                Calls.VOICEMAIL_TYPE,  // Stand-alone
                Calls.MISSED_TYPE, // Group 2: 8-10
                Calls.MISSED_TYPE,
                Calls.OUTGOING_TYPE);
        mBuilder.addGroups(mCursor);
        assertEquals(2, mFakeGroupCreator.groups.size());
        assertGroupIs(1, 4, false, mFakeGroupCreator.groups.get(0));
        assertGroupIs(8, 3, false, mFakeGroupCreator.groups.get(1));
    }

    public void testEqualPhoneNumbers() {
        // Identical.
        assertTrue(mBuilder.equalNumbers("6505555555", "6505555555"));
        assertTrue(mBuilder.equalNumbers("650 555 5555", "650 555 5555"));
        // Formatting.
        assertTrue(mBuilder.equalNumbers("6505555555", "650 555 5555"));
        assertTrue(mBuilder.equalNumbers("6505555555", "(650) 555-5555"));
        assertTrue(mBuilder.equalNumbers("650 555 5555", "(650) 555-5555"));
        // Short codes.
        assertTrue(mBuilder.equalNumbers("55555", "55555"));
        assertTrue(mBuilder.equalNumbers("55555", "555 55"));
        // Different numbers.
        assertFalse(mBuilder.equalNumbers("6505555555", "650555555"));
        assertFalse(mBuilder.equalNumbers("6505555555", "6505555551"));
        assertFalse(mBuilder.equalNumbers("650 555 5555", "650 555 555"));
        assertFalse(mBuilder.equalNumbers("650 555 5555", "650 555 5551"));
        assertFalse(mBuilder.equalNumbers("55555", "5555"));
        assertFalse(mBuilder.equalNumbers("55555", "55551"));
        // SIP addresses.
        assertTrue(mBuilder.equalNumbers("6505555555@host.com", "6505555555@host.com"));
        assertTrue(mBuilder.equalNumbers("6505555555@host.com", "6505555555@HOST.COM"));
        assertTrue(mBuilder.equalNumbers("user@host.com", "user@host.com"));
        assertTrue(mBuilder.equalNumbers("user@host.com", "user@HOST.COM"));
        assertFalse(mBuilder.equalNumbers("USER@host.com", "user@host.com"));
        assertFalse(mBuilder.equalNumbers("user@host.com", "user@host1.com"));
        // SIP address vs phone number.
        assertFalse(mBuilder.equalNumbers("6505555555@host.com", "6505555555"));
        assertFalse(mBuilder.equalNumbers("6505555555", "6505555555@host.com"));
        assertFalse(mBuilder.equalNumbers("user@host.com", "6505555555"));
        assertFalse(mBuilder.equalNumbers("6505555555", "user@host.com"));
        // Nulls.
        assertTrue(mBuilder.equalNumbers(null, null));
        assertFalse(mBuilder.equalNumbers(null, "6505555555"));
        assertFalse(mBuilder.equalNumbers("6505555555", null));
        assertFalse(mBuilder.equalNumbers(null, "6505555555@host.com"));
        assertFalse(mBuilder.equalNumbers("6505555555@host.com", null));
    }

    public void testCompareSipAddresses() {
        // Identical.
        assertTrue(mBuilder.compareSipAddresses("6505555555@host.com", "6505555555@host.com"));
        assertTrue(mBuilder.compareSipAddresses("user@host.com", "user@host.com"));
        // Host is case insensitive.
        assertTrue(mBuilder.compareSipAddresses("6505555555@host.com", "6505555555@HOST.COM"));
        assertTrue(mBuilder.compareSipAddresses("user@host.com", "user@HOST.COM"));
        // Userinfo is case sensitive.
        assertFalse(mBuilder.compareSipAddresses("USER@host.com", "user@host.com"));
        // Different hosts.
        assertFalse(mBuilder.compareSipAddresses("user@host.com", "user@host1.com"));
        // Different users.
        assertFalse(mBuilder.compareSipAddresses("user1@host.com", "user@host.com"));
        // Nulls.
        assertTrue(mBuilder.compareSipAddresses(null, null));
        assertFalse(mBuilder.compareSipAddresses(null, "6505555555@host.com"));
        assertFalse(mBuilder.compareSipAddresses("6505555555@host.com", null));
    }

    /** Creates (or recreates) the cursor used to store the call log content for the tests. */
    private void createCursor() {
        mCursor = new MatrixCursor(CallLogQuery._PROJECTION);
    }

    /** Clears the content of the {@link FakeGroupCreator} used in the tests. */
    private void clearFakeGroupCreator() {
        mFakeGroupCreator.groups.clear();
    }

    /** Asserts that calls of the given types are grouped together into a single group. */
    private void assertCallsAreGrouped(int... types) {
        createCursor();
        clearFakeGroupCreator();
        addMultipleCallLogEntries(TEST_NUMBER1, types);
        mBuilder.addGroups(mCursor);
        assertEquals(1, mFakeGroupCreator.groups.size());
        assertGroupIs(0, types.length, false, mFakeGroupCreator.groups.get(0));

    }

    /** Asserts that calls of the given types are not grouped together at all. */
    private void assertCallsAreNotGrouped(int... types) {
        createCursor();
        clearFakeGroupCreator();
        addMultipleCallLogEntries(TEST_NUMBER1, types);
        mBuilder.addGroups(mCursor);
        assertEquals(0, mFakeGroupCreator.groups.size());
    }

    /** Adds a set of calls with the given types, all from the same number, in the old section. */
    private void addMultipleCallLogEntries(String number, int... types) {
        for (int type : types) {
            addCallLogEntry(number, type);
        }
    }
    /** Adds a call log entry with the given number and type to the cursor. */
    private void addCallLogEntry(String number, int type) {
        mCursor.moveToNext();
        Object[] values = CallLogQueryTestUtils.createTestValues();
        values[CallLogQuery.ID] = mCursor.getPosition();
        values[CallLogQuery.NUMBER] = number;
        values[CallLogQuery.CALL_TYPE] = type;
        mCursor.addRow(values);
    }

    /** Adds a call log entry with a header to the cursor. */
    private void addCallLogHeader(int section) {
        mCursor.moveToNext();
        Object[] values = CallLogQueryTestUtils.createTestValues();
        values[CallLogQuery.ID] = mCursor.getPosition();
        mCursor.addRow(values);
    }

    /** Asserts that the group matches the given values. */
    private void assertGroupIs(int cursorPosition, int size, boolean expanded, GroupSpec group) {
        assertEquals(cursorPosition, group.cursorPosition);
        assertEquals(size, group.size);
        assertEquals(expanded, group.expanded);
    }

    /** Defines an added group. Used by the {@link FakeGroupCreator}. */
    private static class GroupSpec {
        /** The starting position of the group. */
        public final int cursorPosition;
        /** The number of elements in the group. */
        public final int size;
        /** Whether the group should be initially expanded. */
        public final boolean expanded;

        public GroupSpec(int cursorPosition, int size, boolean expanded) {
            this.cursorPosition = cursorPosition;
            this.size = size;
            this.expanded = expanded;
        }
    }

    /** Fake implementation of a GroupCreator which stores the created groups in a member field. */
    private static class FakeGroupCreator implements CallLogGroupBuilder.GroupCreator {
        /** The list of created groups. */
        public final List<GroupSpec> groups = newArrayList();

        @Override
        public void addGroup(int cursorPosition, int size, boolean expanded) {
            groups.add(new GroupSpec(cursorPosition, size, expanded));
        }

        @Override
        public void setDayGroup(long rowId, int dayGroup) {
            //No-op
        }

        @Override
        public void clearDayGroups() {
            //No-op
        }
    }
}
