
package com.mediatek.providers.contacts.dialersearchtestcase;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Preferences;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;

import com.android.providers.contacts.BaseContactsProvider2Test;
import com.android.providers.contacts.CallLogProvider;
import com.android.providers.contacts.CallLogProviderTest.TestCallLogProvider;
import com.android.providers.contacts.testutil.DataUtil;
import com.mtk.at.BasicFuncTest;

public class DialerSearchSupportTest extends BaseContactsProvider2Test {

    public void test01QueryContactWithCtFunction() {
//        Uri baseUri = Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "dialer_search");
//        executeCheckAction(baseUri, false);
    }

    public void test02QueryContactWithOldFunction() {
//        Uri baseUri = Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "dialer_search")
//                .buildUpon().appendQueryParameter("search_behavior", "old").build();
//        executeCheckAction(baseUri, true);
    }

    CallLogProvider mCallLogProvider;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setUpWithVoicemailPermissions();
        mCallLogProvider = (CallLogProvider) addProvider(TestCallLogProvider.class,
                CallLog.AUTHORITY);
        mResolver.delete(Calls.CONTENT_URI_WITH_VOICEMAIL, null, null);
        mResolver.delete(Contacts.CONTENT_URI, null, null);
    }

    @Override
    protected void tearDown() throws Exception {
//        setUpWithVoicemailPermissions();
//        mResolver.delete(Calls.CONTENT_URI_WITH_VOICEMAIL, null, null);
//        mResolver.delete(Contacts.CONTENT_URI, null, null);
        super.tearDown();
    }

    @BasicFuncTest
    public void test03DialerSearchContacts() {
        final String phoneNumber = "18001234567";
        ContentValues values = new ContentValues();
        long rawContactId = createRawContact(values, phoneNumber, 0);
        DataUtil.insertStructuredName(mResolver, rawContactId, "Terry", "Autumn");

        String dsUri = "content://com.android.contacts/dialer_search/";

        // contacts name query
        assertQuery(dsUri, "83779", 1);    //Terry
        assertQuery(dsUri, "8288866", 1);    // TAutumn
        assertQuery(dsUri, "829", 0);    // TA?

        // contacts number query
        assertQuery(dsUri, phoneNumber.substring(0, 5), 1);
        assertQuery(dsUri, phoneNumber.substring(0, 5) + "9", 0);
        assertQuery(dsUri, phoneNumber.substring(0, 10), 1);
    }

    public void test04DialerSearchCallLogs() {
        final String phoneNumber = "13914725836";
        ContentValues values = new ContentValues();
        long callId = createCallLog(values, phoneNumber, 0);

        String dsUri = "content://com.android.contacts/dialer_search/";

        // call log number query
        assertQuery(dsUri, phoneNumber.substring(0, 5), 1);
        assertQuery(dsUri, phoneNumber.substring(0, 10), 1);
        assertQuery(dsUri, phoneNumber.substring(0, 5) + "9", 0);
    }

    private void assertQuery(String baseUriString, String searchKey, int resultCount) {
        int inputLen = searchKey == null ? 0 : searchKey.length();
        Uri queryUri = inputLen == 0 ? Uri.parse(baseUriString) : Uri.parse(baseUriString
                + searchKey);
        Cursor c = mResolver.query(queryUri, null, null, null, null);
        assertEquals(resultCount, c.getCount());
        if (c != null) {
            c.close();
        }
    }

    private long createRawContact(ContentValues values, String phoneNumber, int timesContacted) {
        values.put(RawContacts.CUSTOM_RINGTONE, "beethoven5");
        values.put(RawContacts.TIMES_CONTACTED, timesContacted);
        values.put(RawContacts.SEND_TO_VOICEMAIL, 1);
        Uri insertionUri = RawContacts.CONTENT_URI;
        Uri rawContactUri = mResolver.insert(insertionUri, values);
        long rawContactId = ContentUris.parseId(rawContactUri);
        Uri photoUri = insertPhoto(rawContactId);
        long photoId = ContentUris.parseId(photoUri);
        values.put(Contacts.PHOTO_ID, photoId);
        if (!TextUtils.isEmpty(phoneNumber)) {
            insertPhoneNumber(rawContactId, phoneNumber);
        }
        return rawContactId;
    }

    private long createCallLog(ContentValues values, String phoneNumber, int timesContacted) {
        values.put(Calls.NUMBER, phoneNumber);
        values.put(Calls.DATE, System.currentTimeMillis());
        values.put(Calls.DURATION, 1000);
        Uri insertionUri = Calls.CONTENT_URI;
        Uri callUri = mResolver.insert(insertionUri, values);
        long callId = ContentUris.parseId(callUri);
        return callId;
    }

    static final String ADD_VOICEMAIL_PERMISSION =
            "com.android.voicemail.permission.ADD_VOICEMAIL";
    /*
     * Permission to allow querying voicemails
     */
    static final String READ_VOICEMAIL_PERMISSION =
            "com.android.voicemail.permission.READ_VOICEMAIL";
    /*
     * Permission to allow deleting and updating voicemails
     */
    static final String WRITE_VOICEMAIL_PERMISSION =
            "com.android.voicemail.permission.WRITE_VOICEMAIL";

    private void setUpWithVoicemailPermissions() {
        mActor.addPermissions(ADD_VOICEMAIL_PERMISSION);
        mActor.addPermissions(READ_VOICEMAIL_PERMISSION);
        mActor.addPermissions(WRITE_VOICEMAIL_PERMISSION);
    }
}
