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

package com.android.phone.common.mail.store.imap;

import android.util.Log;

import com.android.phone.common.mail.FixedLengthInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * Subclass of {@link ImapString} used for literals backed by an in-memory byte array.
 */
public class ImapMemoryLiteral extends ImapString {
    private final String TAG = "ImapMemoryLiteral";
    private byte[] mData;

    /* package */ ImapMemoryLiteral(FixedLengthInputStream in) throws IOException {
        // We could use ByteArrayOutputStream and IOUtils.copy, but it'd perform an unnecessary
        // copy....
        mData = new byte[in.getLength()];
        int pos = 0;
        while (pos < mData.length) {
            int read = in.read(mData, pos, mData.length - pos);
            if (read < 0) {
                break;
            }
            pos += read;
        }
        if (pos != mData.length) {
            Log.w(TAG, "");
        }
    }

    @Override
    public void destroy() {
        mData = null;
        super.destroy();
    }

    @Override
    public String getString() {
        try {
            return new String(mData, "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unsupported encoding: ", e);
        }
        return null;
    }

    @Override
    public InputStream getAsStream() {
        return new ByteArrayInputStream(mData);
    }

    @Override
    public String toString() {
        return String.format("{%d byte literal(memory)}", mData.length);
    }
}