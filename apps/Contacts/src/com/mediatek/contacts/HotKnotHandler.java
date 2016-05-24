/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Profile;

import com.mediatek.contacts.util.Log;
import com.mediatek.hotknot.HotKnotAdapter;

public class HotKnotHandler implements HotKnotAdapter.OnHotKnotCompleteCallback {
    private static final String TAG = "HotKnotHandler";

    private static final String PROFILE_LOOKUP_KEY = "profile";
    private static final String INTENT_KEY = "intent";
    private static final String HOTKNOT_VCARD_ACTION =
            "com.mediatek.hotknot.action.VCARD_DISCOVERD";
    private static final String MIME_TYPE_KEY = "mimetype";
    private static final String MIME_TYPE_VALUE = "text/vcard";

    private static final String ACTION_SHARE = "com.mediatek.hotknot.action.SHARE";
    private static final String EXTRA_SHARE_URIS = "com.mediatek.hotknot.extra.SHARE_URIS";

    public static void register(Activity activity, Uri uri) {
        Log.i(TAG, "[register]");
        if (activity == null) {
            Log.e(TAG, "[register]activity is null!");
            return;
        }

        HotKnotAdapter hotKnotAdapter = HotKnotAdapter.getDefaultAdapter(activity
                .getApplicationContext());
        if (hotKnotAdapter == null) {
            Log.e(TAG, "[register]Not support HotKnot");
            return;
        }

        Uri contactUri = uri;
        if (contactUri != null) {
            final String lookupKey = Uri.encode(contactUri.getPathSegments().get(2));
            final Uri shareUri;
            Builder builder = null;
            if (lookupKey.equals(PROFILE_LOOKUP_KEY)) {
                builder = Profile.CONTENT_VCARD_URI.buildUpon();
            } else {
                builder = Contacts.CONTENT_VCARD_URI.buildUpon().appendPath(lookupKey);
            }

            builder.appendQueryParameter(INTENT_KEY, HOTKNOT_VCARD_ACTION).appendQueryParameter(
                    MIME_TYPE_KEY, MIME_TYPE_VALUE);
            shareUri = builder.build();
            Log.d(TAG, "HotKnot share contact Uri: " + shareUri);

            HotKnotHandler hotKnotHandler = new HotKnotHandler();
            hotKnotAdapter.setOnHotKnotCompleteCallback(hotKnotHandler, activity);

            Intent intent = new Intent(ACTION_SHARE);
            intent.putExtra(EXTRA_SHARE_URIS, new Uri[] { shareUri });
            // add for ALPS02292779.
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT);

            try {
                activity.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "[register]HotKnot share activity not found!");
            }
        }
    }

    public HotKnotHandler() {
    }

    @Override
    public void onHotKnotComplete(int reason) {
        Log.i(TAG, "[onHotKnotCompletereason: " + reason);
        switch (reason) {
        case HotKnotAdapter.ERROR_SUCCESS:
            break;
        case HotKnotAdapter.ERROR_DATA_TOO_LARGE:
            break;
        default:
            Log.i(TAG, "HotKnot unknown error: " + reason);
            break;
        }
    }
}
