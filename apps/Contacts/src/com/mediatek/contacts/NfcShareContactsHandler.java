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

package com.mediatek.contacts;

import android.app.Activity;
import android.content.ContentResolver;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.provider.ContactsContract.Contacts;

import com.mediatek.contacts.list.MultiSharePickerFragment;
import com.mediatek.contacts.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class implements sharing the currently displayed contact to another
 * device using NFC. NFC sharing is only enabled when the activity is in the
 * foreground and resumed. When an NFC link is established,
 * {@link #createMessage} will be called to create the data to be sent over the
 * link, which is a vCard in this case.
 */
public class NfcShareContactsHandler implements NfcAdapter.CreateNdefMessageCallback {
    private static final String TAG = "NfcShareContactsHandler";

    private final MultiSharePickerFragment mMultiContactsShareFragment;

    public static void register(Activity activity,
            MultiSharePickerFragment multiContactsShareFragment) {
        Log.i(TAG, "[register]...");
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(activity.getApplicationContext());
        if (adapter == null) {
            Log.e(TAG, "[register]adapter is null!");
            return; // NFC not available on this device
        }
        adapter.setNdefPushMessageCallback(new NfcShareContactsHandler(multiContactsShareFragment),
                activity);
    }

    public NfcShareContactsHandler(MultiSharePickerFragment multiContactsShareFragment) {
        mMultiContactsShareFragment = multiContactsShareFragment;
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        // Get the current contact URI
        Uri contactUri = mMultiContactsShareFragment.getUri();
        ContentResolver resolver = mMultiContactsShareFragment.getActivity().getContentResolver();
        Log.i(TAG, "[createNdefMessage]contactUri = " + contactUri);
        if (contactUri != null) {
            // final String lookupKey =
            // Uri.encode(contactUri.getPathSegments().get(2));
            final String lookupKey = contactUri.getPathSegments().get(2);
            final Uri shareUri;
            // TODO find out where to get this constant from, or find another
            // way
            // of determining this.
            shareUri = Contacts.CONTENT_MULTI_VCARD_URI.buildUpon().appendPath(lookupKey)
                    .appendQueryParameter(Contacts.QUERY_PARAMETER_VCARD_NO_PHOTO, "true").build();
            ByteArrayOutputStream ndefBytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int r;
            try {
                InputStream vcardInputStream = resolver.openInputStream(shareUri);
                while ((r = vcardInputStream.read(buffer)) > 0) {
                    ndefBytes.write(buffer, 0, r);
                }
                vcardInputStream.close();

                NdefRecord record = NdefRecord.createMime("text/x-vcard", ndefBytes.toByteArray());
                return new NdefMessage(record);
            } catch (IOException e) {
                Log.e(TAG, "[createNdefMessage]IOException creating vcard.");
                e.printStackTrace();
                return null;
            }
        } else {
            Log.w(TAG, "[createNdefMessage]No contact URI to share.");
            return null;
        }
    }
}
