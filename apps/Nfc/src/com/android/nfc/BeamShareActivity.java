/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
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

package com.android.nfc;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ClipData;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.BeamShareData;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import android.webkit.URLUtil;

import com.android.internal.R;

/// M: @ {
import com.mediatek.nfc.Util;
/// }

/**
 * This class is registered by NfcService to handle
 * ACTION_SHARE intents. It tries to parse data contained
 * in ACTION_SHARE intents in either a content/file Uri,
 * which can be sent using NFC handover, or alternatively
 * it tries to parse texts and URLs to store them in a simple
 * Text or Uri NdefRecord. The data is then passed on into
 * NfcService to transmit on NFC tap.
 *
 */
public class BeamShareActivity extends Activity {
    static final String TAG ="BeamShareActivity";
    static final boolean DBG = true;

    ArrayList<Uri> mUris;
    NdefMessage mNdefMessage;
    NfcAdapter mNfcAdapter;
    Intent mLaunchIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()");
        mUris = new ArrayList<Uri>();
        mNdefMessage = null;
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mLaunchIntent = getIntent();
        if (mNfcAdapter == null) {
            Log.e(TAG, "NFC adapter not present.");
            finish();
        } else {
            if (!mNfcAdapter.isEnabled()) {

                /// M: @ {
                int p2pFlag = mNfcAdapter.getModeFlag(NfcAdapter.MODE_P2P);
                if (p2pFlag == NfcAdapter.FLAG_OFF) {
                    showNfcP2pDialogAndExit(false,
                        com.android.nfc.R.string.mtk_beam_requires_nfc_p2p_enabled);
                    return;
                }
                /// }

                showNfcDialogAndExit(com.android.nfc.R.string.beam_requires_nfc_enabled);
            } else {

                /// M: @ {
                int p2pFlag = mNfcAdapter.getModeFlag(NfcAdapter.MODE_P2P);
                Log.d(TAG, "mNfcAdapter.getModeFlag(NfcAdapter.MODE_P2P):" + p2pFlag);
                if (p2pFlag == NfcAdapter.FLAG_OFF) {
                    showNfcP2pDialogAndExit(true,
                        com.android.nfc.R.string.mtk_beam_requires_nfc_p2p_enabled);
                } else {
                parseShareIntentAndFinish(mLaunchIntent);
            }
                /// }
            }
        }
    }


    private void showNfcDialogAndExit(int msgId) {

        Log.d(TAG, "showNfcDialogAndExit() , registerReceiver");
        IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
        registerReceiverAsUser(mReceiver, UserHandle.ALL, filter, null, null);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this,
                AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        dialogBuilder.setMessage(msgId);
        dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                finish();
            }
        });
        dialogBuilder.setPositiveButton(R.string.yes,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        Log.d(TAG, "Positive onClick()");
                        if (!mNfcAdapter.isEnabled()) {
                            mNfcAdapter.enable();
                            // Wait for enable broadcast
                        } else {
                            parseShareIntentAndFinish(mLaunchIntent);
                        }
                    }
                });
        dialogBuilder.setNegativeButton(R.string.no,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });

        /// M: @ {
        boolean ret = isFinishing();
        Log.d(TAG, " isFinishing():" + ret);
        if (ret == false) {
            Log.d(TAG, " dialogBuilder.show()");
        dialogBuilder.show();
    }
        /// }
    }

    void tryUri(Uri uri) {
        if (uri.getScheme().equalsIgnoreCase("content") ||
                uri.getScheme().equalsIgnoreCase("file")) {
            // Typically larger data, this can be shared using NFC handover

            if (DBG) Log.d(TAG, "uri.add  uri.toString():" + uri.toString());
            mUris.add(uri);
        } else {
            // Just put this Uri in an NDEF message
            mNdefMessage = new NdefMessage(NdefRecord.createUri(uri));
        }
    }

    void tryText(String text) {
        if (URLUtil.isValidUrl(text)) {
            Uri parsedUri = Uri.parse(text);
            tryUri(parsedUri);
        } else {
            mNdefMessage = new NdefMessage(NdefRecord.createTextRecord(null, text));
        }
    }

    public void parseShareIntentAndFinish(Intent intent) {

        Log.d(TAG, "parseShareIntentAndFinish()");

        if (intent == null || (!intent.getAction().equalsIgnoreCase(Intent.ACTION_SEND) &&
                !intent.getAction().equalsIgnoreCase(Intent.ACTION_SEND_MULTIPLE))) return;

        // First, see if the intent contains clip-data, and if so get data from there
        ClipData clipData = intent.getClipData();
        if (clipData != null && clipData.getItemCount() > 0) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item item = clipData.getItemAt(i);
                // First try to get an Uri
                Uri uri = item.getUri();

                /// M: @ { add try catch
                try {

                Log.d(TAG, "==== VCARD test ====");
                String type = intent.getType();
                Log.d(TAG, " intent.getType() " + type);


                String plainText = item.coerceToText(this).toString();

                Log.d(TAG, "plainText:" + plainText);
                if (uri != null) {
                    if (DBG) Log.d(TAG, "Found uri in ClipData.");
                    tryUri(uri);
                } else if (plainText != null) {
                    if (DBG) Log.d(TAG, "Found text in ClipData.");
                    tryText(plainText);
                } else {
                    if (DBG) Log.d(TAG, "Did not find any shareable data in ClipData.");
                }

                } catch (Exception e) {
                    Log.e(TAG, " parseShareIntentAndFinish()  Exception :" + e);
                    e.printStackTrace();
                }
                /// }

            }
        } else {
            if (intent.getAction().equalsIgnoreCase(Intent.ACTION_SEND)) {
                final Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                final CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
                if (uri != null) {
                    if (DBG) Log.d(TAG, "Found uri in ACTION_SEND intent.");
                    tryUri(uri);
                } else if (text != null) {
                    if (DBG) Log.d(TAG, "Found EXTRA_TEXT in ACTION_SEND intent.");
                    tryText(text.toString());
                } else {
                    if (DBG) Log.d(TAG, "Did not find any shareable data in ACTION_SEND intent.");
                }
            } else {
                final ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                final ArrayList<CharSequence> texts = intent.getCharSequenceArrayListExtra(
                        Intent.EXTRA_TEXT);

                if (uris != null && uris.size() > 0) {
                    for (Uri uri : uris) {
                        if (DBG) Log.d(TAG, "Found uri in ACTION_SEND_MULTIPLE intent.");
                        tryUri(uri);
                    }
                } else if (texts != null && texts.size() > 0) {
                    // Try EXTRA_TEXT, but just for the first record
                    if (DBG) Log.d(TAG, "Found text in ACTION_SEND_MULTIPLE intent.");
                    tryText(texts.get(0).toString());
                } else {
                    if (DBG) Log.d(TAG, "Did not find any shareable data in " +
                            "ACTION_SEND_MULTIPLE intent.");
                }
            }
        }

        /// M: @ {
        try {

            Log.d(TAG, "Create BeamShareData..");
            if (mUris != null) {
                Log.d(TAG, "mUris.size():" + mUris.size());

                for (Uri printUri: mUris) {
                    Log.d(TAG, "uri.toString():" + printUri.toString());

                }
            }
            if (mNdefMessage != null) {
                String ResultStr = "";
                byte[] PrintNdefByteArray = mNdefMessage.toByteArray();
                ResultStr += "  Length:" + PrintNdefByteArray.length;
                if (PrintNdefByteArray.length < 200) {
                    ResultStr += "  Array::" + Util.bytesToString(PrintNdefByteArray);
                } else {
                    ResultStr += "  Array:: not print RAW " ;
                }

                Log.d(TAG, "mNdefMessage " + ResultStr);
            }
        } catch (Exception e) {
            Log.e(TAG, " parseShareIntentAndFinish()  Exception :" + e);
            e.printStackTrace();
        }
        /// }


        BeamShareData shareData = null;
        UserHandle myUserHandle = new UserHandle(UserHandle.myUserId());
        if (mUris.size() > 0) {
            // Uris have our first preference for sharing
            Uri[] uriArray = new Uri[mUris.size()];
            int numValidUris = 0;
            for (Uri uri : mUris) {
                try {
                    grantUriPermission("com.android.nfc", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    uriArray[numValidUris++] = uri;
                    if (DBG) Log.d(TAG, "Found uri: " + uri);
                } catch (SecurityException e) {
                    Log.e(TAG, "Security exception granting uri permission to NFC process.");
                    numValidUris = 0;
                    break;
                }
            }
            if (numValidUris > 0) {
                shareData = new BeamShareData(null, uriArray, myUserHandle, 0);
            } else {
                // No uris left
                shareData = new BeamShareData(null, null, myUserHandle, 0);
            }
        } else if (mNdefMessage != null) {
            shareData = new BeamShareData(mNdefMessage, null, myUserHandle, 0);
            if (DBG) Log.d(TAG, "Created NDEF message:" + mNdefMessage.toString());
        } else {
            if (DBG) Log.d(TAG, "Could not find any data to parse.");
            // Activity may have set something to share over NFC, so pass on anyway
            shareData = new BeamShareData(null, null, myUserHandle, 0);
        }

        /// M: @ {
        if (mUris.size() != 0 || mNdefMessage != null) {
            Log.d(TAG, " mNfcAdapter.invokeBeam()");
            mNfcAdapter.invokeBeam(shareData);
        }
        /// }

        finish();
    }

    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE,
                        NfcAdapter.STATE_OFF);
                if (state == NfcAdapter.STATE_ON) {
                    parseShareIntentAndFinish(mLaunchIntent);
                }
            }
        }
    };

    /// M: @ {
    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "super. onDestroy() ");
        try {
           Log.d(TAG, "unregister() ");
           unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "[False Alarm], catch Unregister IllegalArgumentException :" + e);
            //e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, " unregisterReceiver()  Exception :" + e);
            e.printStackTrace();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, " onResume() : call setVisible(true)" );
        setVisible(true);
    }


    private void showNfcP2pDialogAndExit(boolean isNfcEnable, int msgId) {

        Log.d(TAG, "showNfcP2pDialogAndExit() , isNfcEnable:" + isNfcEnable);

        if (isNfcEnable == false) {
            Log.d(TAG, "  , registerReceiver (ACTION_ADAPTER_STATE_CHANGED)");
            IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
            registerReceiverAsUser(mReceiver, UserHandle.ALL, filter, null, null);
        }

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this,
                AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        dialogBuilder.setMessage(msgId);
        dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                finish();
            }
        });
        dialogBuilder.setPositiveButton(R.string.yes,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        Log.d(TAG, "Positive onClick()");
                        if (!mNfcAdapter.isEnabled()) {
                            //NFC is OFF, we  set p2p mode to true, and enable NFC

                            mNfcAdapter.setModeFlag(NfcAdapter.MODE_P2P, NfcAdapter.FLAG_ON);
                            mNfcAdapter.enable();
                            // Wait for enable broadcast

                        } else {
                            //NFC is on, we just set p2p mode to true, act now
                            mNfcAdapter.setModeFlag(NfcAdapter.MODE_P2P, NfcAdapter.FLAG_ON);
                            parseShareIntentAndFinish(mLaunchIntent);
                        }
                    }
                });
        dialogBuilder.setNegativeButton(R.string.no,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d(TAG, "Negative onClick()");
                        finish();
                    }
                });

        /// M: @ {
        boolean ret = isFinishing();
        Log.d(TAG, " isFinishing():" + ret);
        if (ret == false) {
            Log.d(TAG, " dialogBuilder.show()");
        dialogBuilder.show();
    }
        /// }
    }


    /// }

}
