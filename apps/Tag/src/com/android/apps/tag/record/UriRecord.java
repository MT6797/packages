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

package com.android.apps.tag.record;

import com.android.apps.tag.R;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.primitives.Bytes;

import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.nfc.NdefRecord;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * A parsed record containing a Uri.
 */
public class UriRecord extends ParsedNdefRecord implements OnClickListener {
    private static final String TAG = "UriRecord";
    private static View Uri_view;
    public static final String RECORD_TYPE = "UriRecord";
    private static final int REQUEST_CALL = 0;
    private final Uri mUri;
    private static String[] PERMISSIONS_CALL = {Manifest.permission.CALL_PHONE};

    private UriRecord(Uri uri) {
        this.mUri = Preconditions.checkNotNull(uri);
    }

    public Intent getIntentForUri() {
        String scheme = mUri.getScheme();
        if ("tel".equals(scheme)) {
            return new Intent(Intent.ACTION_CALL, mUri);
        } else if ("sms".equals(scheme) || "smsto".equals(scheme)) {
            return new Intent(Intent.ACTION_SENDTO, mUri);
        } else {
            return new Intent(Intent.ACTION_VIEW, mUri);
        }
    }

    public String getPrettyUriString(Context context) {
        String scheme = mUri.getScheme();
        boolean tel = "tel".equals(scheme);
        boolean sms = "sms".equals(scheme) || "smsto".equals(scheme);
        if (tel || sms) {
            String ssp = mUri.getSchemeSpecificPart();
            int offset = ssp.indexOf('?');
            if (offset >= 0) {
                ssp = ssp.substring(0, offset);
            }
            if (tel) {
                return context.getString(R.string.action_call, PhoneNumberUtils.formatNumber(ssp));
            } else {
                return context.getString(R.string.action_text, PhoneNumberUtils.formatNumber(ssp));
            }
        } else {
            return mUri.toString();
        }
    }

    @Override
    public View getView(Activity activity, LayoutInflater inflater, ViewGroup parent, int offset) {
        return RecordUtils.getViewsForIntent(activity, inflater, parent, this, getIntentForUri(),
                getPrettyUriString(activity));
    }

    @Override
    public String getSnippet(Context context, Locale locale) {
        return getPrettyUriString(context);
    }

    @Override
    public void onClick(View view){
        RecordUtils.ClickInfo info = (RecordUtils.ClickInfo) view.getTag();
        String scheme = mUri.getScheme();
        Uri_view = view;
        Context cntxt = info.activity.getApplicationContext();
        if ("tel".equals(scheme) && cntxt.checkSelfPermission(Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED){
            Log.i(TAG, "Call permissions has NOT been granted. Requesting permissions.");
             info.activity.requestPermissions(PERMISSIONS_CALL, REQUEST_CALL);
        }
        else {
        try {
            info.activity.startActivity(info.intent);
            info.activity.finish();
        } catch (ActivityNotFoundException e) {
            // The activity wansn't found for some reason. Don't crash, but don't do anything.
            Log.e(TAG, "Failed to launch activity for intent " + info.intent, e);
        }
        }
    }

    @VisibleForTesting
    public Uri getUri() {
        return mUri;
    }

    /**
     * Convert {@link android.nfc.NdefRecord} into a {@link android.net.Uri}. This will handle
     * both TNF_WELL_KNOWN / RTD_URI and TNF_ABSOLUTE_URI.
     *
     * @throws IllegalArgumentException if the NdefRecord is not a
     *     record containing a URI.
     */
    public static UriRecord parse(NdefRecord record) {
        Uri uri = record.toUri();
        if (uri == null) throw new IllegalArgumentException("not a uri");
        return new UriRecord(uri);
    }


    public static void onUriPermissionsResult(int requestCode,  String[] permissions,
             int[] grantResults) {
        View view = Uri_view;
        RecordUtils.ClickInfo info = (RecordUtils.ClickInfo) view.getTag();
        Context cntxt = info.activity.getApplicationContext();
        if (requestCode == REQUEST_CALL) {
            Log.i(TAG, "Received response for Call permissions request.");

            // We have requested multiple permissions for call, so all of them need to be
            // checked.
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // All required permissions have been granted, display calls fragment.
                Log.i(TAG,
                    "Call related permissions have been granted. Displaying call activity.");
                try {
                    info.activity.startActivity(info.intent);
                    info.activity.finish();
                } catch (ActivityNotFoundException e) {
                // The activity wansn't found for some reason. Don't crash, but don't do anything.
                Log.e(TAG, "Failed to launch activity for intent " + info.intent, e);
                }
            }
            else {
              if(info.activity.shouldShowRequestPermissionRationale(
                  Manifest.permission.CALL_PHONE)){
                  Log.i(TAG, "Call permissions were NOT granted for Record");
                  Toast.makeText(info.activity.getApplicationContext(),
                  cntxt.getResources().getString(com.mediatek.R.string.denied_required_permission),
                  Toast.LENGTH_SHORT)
                  .show();
                  info.activity.finish();
               }
               else {
                Log.i(TAG, "Call related  permissions were NOT granted");
                Toast.makeText(info.activity.getApplicationContext(),
                cntxt.getResources().getString(com.mediatek.R.string.denied_required_permission),
                Toast.LENGTH_SHORT)
                .show();
                info.activity.finish();
               }
            }
        }
        else{
            info.activity.finish();
        }
    }

    public static boolean isUri(NdefRecord record) {
        return record.toUri() != null;
    }

    /**
     * Convert a {@link Uri} to an {@link NdefRecord}
     */
    public static NdefRecord newUriRecord(Uri uri) {
        return NdefRecord.createUri(uri);
    }
}
