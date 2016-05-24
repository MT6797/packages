/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.phone.vvm.omtp.fetch;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.VoicemailContract.Voicemails;
import android.util.Log;

import com.android.phone.vvm.omtp.imap.VoicemailPayload;

import libcore.io.IoUtils;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Callback for when a voicemail payload is fetched. It copies the returned stream to the data
 * file corresponding to the voicemail.
 */
public class VoicemailFetchedCallback {
    private static final String TAG = "VoicemailFetchedCallback";

    private ContentResolver mContentResolver;
    private Uri mUri;

    VoicemailFetchedCallback(Context context, Uri uri) {
        mContentResolver = context.getContentResolver();
        mUri = uri;
    }

    /**
     * Saves the voicemail payload data into the voicemail provider then sets the "has_content" bit
     * of the voicemail to "1".
     *
     * @param voicemailPayload The object containing the content data for the voicemail
     */
    public void setVoicemailContent(VoicemailPayload voicemailPayload) {
        Log.d(TAG, String.format("Writing new voicemail content: %s", mUri));
        OutputStream outputStream = null;

        try {
            outputStream = mContentResolver.openOutputStream(mUri);
            byte[] inputBytes = voicemailPayload.getBytes();
            if (inputBytes != null) {
                outputStream.write(inputBytes);
            }
        } catch (IOException e) {
            Log.w(TAG, String.format("File not found for %s", mUri));
            return;
        } finally {
            IoUtils.closeQuietly(outputStream);
        }

        // Update mime_type & has_content after we are done with file update.
        ContentValues values = new ContentValues();
        values.put(Voicemails.MIME_TYPE, voicemailPayload.getMimeType());
        values.put(Voicemails.HAS_CONTENT, true);
        int updatedCount = mContentResolver.update(mUri, values, null, null);
        if (updatedCount != 1) {
            Log.e(TAG, "Updating voicemail should have updated 1 row, was: " + updatedCount);
        }
    }
}
