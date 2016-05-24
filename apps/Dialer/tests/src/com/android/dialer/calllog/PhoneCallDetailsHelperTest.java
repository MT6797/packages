/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.Context;
import android.content.res.Resources;
import android.provider.CallLog.Calls;
import android.test.AndroidTestCase;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;

import com.android.dialer.PhoneCallDetails;
import com.android.dialer.R;
import com.android.dialer.util.LocaleTestUtils;

import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Unit tests for {@link PhoneCallDetailsHelper}.m
 */
public class PhoneCallDetailsHelperTest extends AndroidTestCase {
    /** The number to be used to access the voicemail. */
    private static final String TEST_VOICEMAIL_NUMBER = "125";
    /** The date of the call log entry. */
    private static final long TEST_DATE =
        new GregorianCalendar(2011, 5, 3, 13, 0, 0).getTimeInMillis();
    /** A test duration value for phone calls. */
    private static final long TEST_DURATION = 62300;
    /** The number of the caller/callee in the log entry. */
    private static final String TEST_NUMBER = "14125555555";
    /** The formatted version of {@link #TEST_NUMBER}. */
    private static final String TEST_FORMATTED_NUMBER = "1-412-255-5555";
    /** The country ISO name used in the tests. */
    private static final String TEST_COUNTRY_ISO = "US";
    /** The geocoded location used in the tests. */
    private static final String TEST_GEOCODE = "United States";
    /** Empty geocode label */
    private static final String EMPTY_GEOCODE = "";

    /** The object under test. */
    private PhoneCallDetailsHelper mHelper;
    /** The views to fill. */
    private PhoneCallDetailsViews mViews;
    private TextView mNameView;
    private LocaleTestUtils mLocaleTestUtils;
    private TestTelecomCallLogCache mPhoneUtils;

    private Context mContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        Resources resources = mContext.getResources();
        mPhoneUtils = new TestTelecomCallLogCache(mContext, TEST_VOICEMAIL_NUMBER);
        final TestTelecomCallLogCache phoneUtils = new TestTelecomCallLogCache(
                mContext, TEST_VOICEMAIL_NUMBER);
        mHelper = new PhoneCallDetailsHelper(mContext, resources, phoneUtils);
        mHelper.setCurrentTimeForTest(
                new GregorianCalendar(2011, 5, 4, 13, 0, 0).getTimeInMillis());
        mViews = PhoneCallDetailsViews.createForTest(mContext);
        mNameView = new TextView(mContext);
        mLocaleTestUtils = new LocaleTestUtils(mContext);
        mLocaleTestUtils.setLocale(Locale.US);
    }

    @Override
    protected void tearDown() throws Exception {
        mLocaleTestUtils.restoreLocale();
        mNameView = null;
        mViews = null;
        mHelper = null;
        super.tearDown();
    }

    public void testSetPhoneCallDetails_Unknown() {
        setPhoneCallDetailsWithNumber("", Calls.PRESENTATION_UNKNOWN, "");
        assertNameEqualsResource(R.string.unknown);
    }

    public void testSetPhoneCallDetails_Private() {
        setPhoneCallDetailsWithNumber("", Calls.PRESENTATION_RESTRICTED, "");
        assertNameEqualsResource(R.string.private_num);
    }

    public void testSetPhoneCallDetails_Payphone() {
        setPhoneCallDetailsWithNumber("", Calls.PRESENTATION_PAYPHONE, "");
        assertNameEqualsResource(R.string.payphone);
    }

    public void testSetPhoneCallDetails_Voicemail() {
        setPhoneCallDetailsWithNumber(TEST_VOICEMAIL_NUMBER,
                Calls.PRESENTATION_ALLOWED, TEST_VOICEMAIL_NUMBER);
        assertNameEqualsResource(R.string.voicemail);
    }

    public void testSetPhoneCallDetails_Normal() {
        setPhoneCallDetailsWithNumber("14125551212",
                Calls.PRESENTATION_ALLOWED, "1-412-555-1212");
        assertTrue(mViews.callLocationAndDate.getText().toString().contains("Yesterday"));
    }

    /** Asserts that a char sequence is actually a Spanned corresponding to the expected HTML. */
    private void assertEqualsHtml(String expectedHtml, CharSequence actualText) {
        // In order to contain HTML, the text should actually be a Spanned.
        assertTrue(actualText instanceof Spanned);
        Spanned actualSpanned = (Spanned) actualText;
        // Convert from and to HTML to take care of alternative formatting of HTML.
        assertEquals(Html.toHtml(Html.fromHtml(expectedHtml)), Html.toHtml(actualSpanned));

    }

    public void testSetPhoneCallDetails_Date() {
        mHelper.setCurrentTimeForTest(
                new GregorianCalendar(2011, 5, 3, 13, 0, 0).getTimeInMillis());

        setPhoneCallDetailsWithDate(
                new GregorianCalendar(2011, 5, 3, 13, 0, 0).getTimeInMillis());
        assertDateEquals("0 min. ago");

        setPhoneCallDetailsWithDate(
                new GregorianCalendar(2011, 5, 3, 12, 0, 0).getTimeInMillis());
        assertDateEquals("1 hr. ago");

        setPhoneCallDetailsWithDate(
                new GregorianCalendar(2011, 5, 2, 13, 0, 0).getTimeInMillis());
        assertDateEquals("Yesterday");

        setPhoneCallDetailsWithDate(
                new GregorianCalendar(2011, 5, 1, 13, 0, 0).getTimeInMillis());
        assertDateEquals("2 days ago");
    }

    public void testSetPhoneCallDetails_CallTypeIcons() {
        setPhoneCallDetailsWithCallTypeIcons(Calls.INCOMING_TYPE);
        assertCallTypeIconsEquals(Calls.INCOMING_TYPE);

        setPhoneCallDetailsWithCallTypeIcons(Calls.OUTGOING_TYPE);
        assertCallTypeIconsEquals(Calls.OUTGOING_TYPE);

        setPhoneCallDetailsWithCallTypeIcons(Calls.MISSED_TYPE);
        assertCallTypeIconsEquals(Calls.MISSED_TYPE);

        setPhoneCallDetailsWithCallTypeIcons(Calls.VOICEMAIL_TYPE);
        assertCallTypeIconsEquals(Calls.VOICEMAIL_TYPE);
    }

    /**
     * Tests a case where the video call feature is present.
     */
    public void testSetPhoneCallDetails_Video() {
        PhoneCallDetails details = getPhoneCallDetails();
        details.features = Calls.FEATURES_VIDEO;
        mHelper.setPhoneCallDetails(mViews, details);

        assertIsVideoCall(true);
    }

    /**
     * Tests a case where the video call feature is not present.
     */
    public void testSetPhoneCallDetails_NoVideo() {
        PhoneCallDetails details = getPhoneCallDetails();
        details.features = 0;
        mHelper.setPhoneCallDetails(mViews, details);

        assertIsVideoCall(false);
    }

    public void testSetPhoneCallDetails_MultipleCallTypeIcons() {
        setPhoneCallDetailsWithCallTypeIcons(Calls.INCOMING_TYPE, Calls.OUTGOING_TYPE);
        assertCallTypeIconsEquals(Calls.INCOMING_TYPE, Calls.OUTGOING_TYPE);

        setPhoneCallDetailsWithCallTypeIcons(Calls.MISSED_TYPE, Calls.MISSED_TYPE);
        assertCallTypeIconsEquals(Calls.MISSED_TYPE, Calls.MISSED_TYPE);
    }

    public void testSetPhoneCallDetails_MultipleCallTypeIconsLastOneDropped() {
        setPhoneCallDetailsWithCallTypeIcons(Calls.MISSED_TYPE, Calls.MISSED_TYPE,
                Calls.INCOMING_TYPE, Calls.OUTGOING_TYPE);
        assertCallTypeIconsEqualsPlusOverflow("(4)",
                Calls.MISSED_TYPE, Calls.MISSED_TYPE, Calls.INCOMING_TYPE);
    }

    public void testSetPhoneCallDetails_Geocode() {
        setPhoneCallDetailsWithNumberAndGeocode("+14125555555", "1-412-555-5555", "Pennsylvania");
        assertNameEquals("1-412-555-5555");  // The phone number is shown as the name.
        assertLabelEquals("Pennsylvania"); // The geocode is shown as the label.
    }

    public void testSetPhoneCallDetails_NoGeocode() {
        setPhoneCallDetailsWithNumberAndGeocode("+14125555555", "1-412-555-5555", null);
        assertNameEquals("1-412-555-5555");  // The phone number is shown as the name.
        assertLabelEquals(EMPTY_GEOCODE); // The empty geocode is shown as the label.
    }

    public void testSetPhoneCallDetails_EmptyGeocode() {
        setPhoneCallDetailsWithNumberAndGeocode("+14125555555", "1-412-555-5555", "");
        assertNameEquals("1-412-555-5555");  // The phone number is shown as the name.
        assertLabelEquals(EMPTY_GEOCODE); // The empty geocode is shown as the label.
    }

    public void testSetPhoneCallDetails_NoGeocodeForVoicemail() {
        setPhoneCallDetailsWithNumberAndGeocode(TEST_VOICEMAIL_NUMBER, "", "United States");
        assertLabelEquals(EMPTY_GEOCODE); // The empty geocode is shown as the label.
    }

    public void testSetPhoneCallDetails_Highlighted() {
        setPhoneCallDetailsWithNumber(TEST_VOICEMAIL_NUMBER,
                Calls.PRESENTATION_ALLOWED, "");
    }

    public void testSetCallDetailsHeader_NumberOnly() {
        setCallDetailsHeaderWithNumber(TEST_NUMBER, Calls.PRESENTATION_ALLOWED);
        assertEquals(View.VISIBLE, mNameView.getVisibility());
        assertEquals("1-412-255-5555", mNameView.getText().toString());
    }

    public void testSetCallDetailsHeader_UnknownNumber() {
        setCallDetailsHeaderWithNumber("", Calls.PRESENTATION_UNKNOWN);
        assertEquals(View.VISIBLE, mNameView.getVisibility());
        assertEquals("Unknown", mNameView.getText().toString());
    }

    public void testSetCallDetailsHeader_PrivateNumber() {
        setCallDetailsHeaderWithNumber("", Calls.PRESENTATION_RESTRICTED);
        assertEquals(View.VISIBLE, mNameView.getVisibility());
        assertEquals("Private number", mNameView.getText().toString());
    }

    public void testSetCallDetailsHeader_PayphoneNumber() {
        setCallDetailsHeaderWithNumber("", Calls.PRESENTATION_PAYPHONE);
        assertEquals(View.VISIBLE, mNameView.getVisibility());
        assertEquals("Payphone", mNameView.getText().toString());
    }

    public void testSetCallDetailsHeader_VoicemailNumber() {
        PhoneCallDetails details = getPhoneCallDetails(
                TEST_VOICEMAIL_NUMBER,
                Calls.PRESENTATION_ALLOWED,
                TEST_FORMATTED_NUMBER);
        mHelper.setCallDetailsHeader(mNameView, details);
        assertEquals(View.VISIBLE, mNameView.getVisibility());
        assertEquals("Voicemail", mNameView.getText().toString());
    }

    public void testSetCallDetailsHeader() {
        setCallDetailsHeader("John Doe");
        assertEquals(View.VISIBLE, mNameView.getVisibility());
        assertEquals("John Doe", mNameView.getText().toString());
    }

    /** Asserts that the name text field contains the value of the given string resource. */
    private void assertNameEqualsResource(int resId) {
        assertNameEquals(getContext().getString(resId));
    }

    /** Asserts that the name text field contains the given string value. */
    private void assertNameEquals(String text) {
        assertEquals(text, mViews.nameView.getText().toString());
    }

    /** Asserts that the label text field contains the given string value. */
    private void assertLabelEquals(String text) {
        assertTrue(mViews.callLocationAndDate.getText().toString().contains(text));
    }

    /** Asserts that the date text field contains the given string value. */
    private void assertDateEquals(String text) {
        assertTrue(mViews.callLocationAndDate.getText().toString().contains(text));
    }

    /** Asserts that the video icon is shown. */
    private void assertIsVideoCall(boolean isVideoCall) {
        assertEquals(isVideoCall, mViews.callTypeIcons.isVideoShown());
    }

    /** Asserts that the call type contains the images with the given drawables. */
    private void assertCallTypeIconsEquals(int... ids) {
        assertEquals(ids.length, mViews.callTypeIcons.getCount());
        for (int index = 0; index < ids.length; ++index) {
            int id = ids[index];
            assertEquals(id, mViews.callTypeIcons.getCallType(index));
        }
        assertEquals(View.VISIBLE, mViews.callTypeIcons.getVisibility());
        assertTrue(mViews.callLocationAndDate.getText().toString().contains("Yesterday"));
    }

    /**
     * Asserts that the call type contains the images with the given drawables and shows the given
     * text next to the icons.
     */
    private void assertCallTypeIconsEqualsPlusOverflow(String overflowText, int... ids) {
        assertEquals(ids.length, mViews.callTypeIcons.getCount());
        for (int index = 0; index < ids.length; ++index) {
            int id = ids[index];
            assertEquals(id, mViews.callTypeIcons.getCallType(index));
        }
        assertEquals(View.VISIBLE, mViews.callTypeIcons.getVisibility());
        assertTrue(mViews.callLocationAndDate.getText().toString().contains(overflowText));
        assertTrue(mViews.callLocationAndDate.getText().toString().contains("Yesterday"));
    }

    /** Sets the phone call details with default values and the given number. */
    private void setPhoneCallDetailsWithNumber(String number, int presentation,
            String formattedNumber) {
        PhoneCallDetails details = getPhoneCallDetails(number, presentation, formattedNumber);
        details.callTypes = new int[]{ Calls.VOICEMAIL_TYPE };
        mHelper.setPhoneCallDetails(mViews, details);
    }

    /** Sets the phone call details with default values and the given number. */
    private void setPhoneCallDetailsWithNumberAndGeocode(
            String number, String formattedNumber, String geocodedLocation) {
        PhoneCallDetails details = getPhoneCallDetails(
                number, Calls.PRESENTATION_ALLOWED, formattedNumber);
        details.geocode = geocodedLocation;
        mHelper.setPhoneCallDetails(mViews, details);
    }

    /** Sets the phone call details with default values and the given date. */
    private void setPhoneCallDetailsWithDate(long date) {
        PhoneCallDetails details = getPhoneCallDetails();
        details.date = date;
        mHelper.setPhoneCallDetails(mViews, details);
    }

    /** Sets the phone call details with default values and the given call types using icons. */
    private void setPhoneCallDetailsWithCallTypeIcons(int... callTypes) {
        PhoneCallDetails details = getPhoneCallDetails();
        details.callTypes = callTypes;
        mHelper.setPhoneCallDetails(mViews, details);
    }

    private void setCallDetailsHeaderWithNumber(String number, int presentation) {
        mHelper.setCallDetailsHeader(mNameView,
                getPhoneCallDetails(number, presentation, TEST_FORMATTED_NUMBER));
    }

    private void setCallDetailsHeader(String name) {
        PhoneCallDetails details = getPhoneCallDetails();
        details.name = name;
        mHelper.setCallDetailsHeader(mNameView, details);
    }

    private PhoneCallDetails getPhoneCallDetails() {
        PhoneCallDetails details = new PhoneCallDetails(
                mContext,
                TEST_NUMBER,
                Calls.PRESENTATION_ALLOWED,
                TEST_FORMATTED_NUMBER,
                false /* isVoicemail */);
        setDefaultDetails(details);
        return details;
    }

    private PhoneCallDetails getPhoneCallDetails(
            String number, int presentation, String formattedNumber) {
        PhoneCallDetails details = new PhoneCallDetails(
                mContext,
                number,
                presentation,
                formattedNumber,
                isVoicemail(number));
        setDefaultDetails(details);
        return details;
    }

    private void setDefaultDetails(PhoneCallDetails details) {
        details.callTypes = new int[]{ Calls.INCOMING_TYPE };
        details.countryIso = TEST_COUNTRY_ISO;
        details.date = TEST_DATE;
        details.duration = TEST_DURATION;
        details.geocode = TEST_GEOCODE;
    }

    private boolean isVoicemail(String number) {
        return number.equals(TEST_VOICEMAIL_NUMBER);
    }
}
