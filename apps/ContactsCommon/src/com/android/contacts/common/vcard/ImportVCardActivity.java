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
 * limitations under the License.
 */

package com.android.contacts.common.vcard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.OpenableColumns;
import android.text.format.DateUtils;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;

import android.widget.Button;
import android.widget.Toast;

import com.android.contacts.common.R;
import com.android.contacts.common.activity.RequestImportVCardPermissionsActivity;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.util.AccountSelectionUtil;
import com.android.internal.telephony.TelephonyIntents;
import com.android.vcard.VCardEntryCounter;
import com.android.vcard.VCardParser;
import com.android.vcard.VCardParser_V21;
import com.android.vcard.VCardParser_V30;
import com.android.vcard.VCardSourceDetector;
import com.android.vcard.exception.VCardException;
import com.android.vcard.exception.VCardNestedException;
import com.android.vcard.exception.VCardVersionException;

import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.ImportExportUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.MtkToast;
import com.mediatek.contacts.util.VcardUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * The class letting users to import vCard. This includes the UI part for letting them select
 * an Account and posssibly a file if there's no Uri is given from its caller Activity.
 *
 * Note that this Activity assumes that the instance is a "one-shot Activity", which will be
 * finished (with the method {@link Activity#finish()}) after the import and never reuse
 * any Dialog in the instance. So this code is careless about the management around managed
 * dialogs stuffs (like how onCreateDialog() is used).
 */
public class ImportVCardActivity extends Activity {
    private static final String LOG_TAG = "ImportVCardActivity";

    private static final int SELECT_ACCOUNT = 0;

    /* package */ static final String VCARD_URI_ARRAY = "vcard_uri";
    /* package */ static final String ESTIMATED_VCARD_TYPE_ARRAY = "estimated_vcard_type";
    /* package */ static final String ESTIMATED_CHARSET_ARRAY = "estimated_charset";
    /* package */ static final String VCARD_VERSION_ARRAY = "vcard_version";
    /* package */ static final String ENTRY_COUNT_ARRAY = "entry_count";

    /* package */ final static int VCARD_VERSION_AUTO_DETECT = 0;
    /* package */ final static int VCARD_VERSION_V21 = 1;
    /* package */ final static int VCARD_VERSION_V30 = 2;

    private static final int REQUEST_OPEN_DOCUMENT = 100;

    private static final String SECURE_DIRECTORY_NAME = ".android_secure";

    /**
     * Notification id used when error happened before sending an import request to VCardServer.
     */
    private static final int FAILURE_NOTIFICATION_ID = 1;

    final static String CACHED_URIS = "cached_uris";

    private AccountWithDataSet mAccount;

    private ProgressDialog mProgressDialogForScanVCard;
    private ProgressDialog mProgressDialogForCachingVCard;

    private List<VCardFile> mAllVCardFileList;
    private VCardScanThread mVCardScanThread;

    private VCardCacheThread mVCardCacheThread;
    private ImportRequestConnection mConnection;
    /// M: For Vcard file select dialog.
    private AlertDialog mVcardFileSelectDialog;
    /* package */ VCardImportExportListener mListener;

    private String mErrorMessage;

    private Handler mHandler = new Handler();

    // M: add listen to Sdcard and sim state Dynamicly @{
    private BroadcastReceiver mSdCardStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(LOG_TAG, "[SdCardState_onReceive] action = " + action);
            if ((Intent.ACTION_MEDIA_EJECT).equals(action)) {
                if (!isFinishing()) {
                    Log.w(LOG_TAG, "[SdCardState_onReceive] sd card is removed,importActivity" +
                            " will finished!");
                    finish();
                }
            }
        }
    };

    private BroadcastReceiver mSubStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(LOG_TAG, "[SubState_onReceive] action = " + action);
            if ((TelephonyIntents.ACTION_PHB_STATE_CHANGED).equals(action)) {
                if (!isFinishing()) {
                    Log.w(LOG_TAG, "[SubState_onReceive] sim card is changed,import Activity"
                            + " will finished!");
                    finish();
                }
            }
        }
    };

    private void registerListenSDCardAndSubState() {
        Log.d(LOG_TAG, "[registerListenSDCardAndSubState]");
        registerListenSDCardState();
        registerListenSubState();
    }

    private void unRegisterListenSDCardAndSubState() {
        Log.d(LOG_TAG, "[unRegisterListenSDCardAndSubState]");
        unregisterReceiver(mSdCardStateReceiver);
        unregisterReceiver(mSubStateReceiver);
    }

    private void registerListenSDCardState() {
        IntentFilter sdcardFilter = new IntentFilter();
        sdcardFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        sdcardFilter.addDataScheme("file");
        registerReceiver(mSdCardStateReceiver, sdcardFilter);
    }

    private void registerListenSubState() {
        registerReceiver(mSubStateReceiver, new IntentFilter(
                TelephonyIntents.ACTION_PHB_STATE_CHANGED));
    }
    //@}

    private static class VCardFile {
        private final String mName;
        private final String mCanonicalPath;
        private final long mLastModified;

        public VCardFile(String name, String canonicalPath, long lastModified) {
            mName = name;
            mCanonicalPath = canonicalPath;
            mLastModified = lastModified;
        }

        public String getName() {
            return mName;
        }

        public String getCanonicalPath() {
            return mCanonicalPath;
        }

        public long getLastModified() {
            return mLastModified;
        }
    }

    // Runs on the UI thread.
    private class DialogDisplayer implements Runnable {
        private final int mResId;
        public DialogDisplayer(int resId) {
            mResId = resId;
        }
        public DialogDisplayer(String errorMessage) {
            mResId = R.id.dialog_error_with_message;
            mErrorMessage = errorMessage;
        }
        @Override
        public void run() {
            if (!isFinishing()) {
                showDialog(mResId);
            }
        }
    }

    private class CancelListener
        implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            /** M: Bug Fix CR ID: ALPS00110214 */
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
        @Override
        public void onCancel(DialogInterface dialog) {
            /** M: Bug Fix CR ID: ALPS00110214 */
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    private CancelListener mCancelListener = new CancelListener();

    private class ImportRequestConnection implements ServiceConnection {
        private VCardService mService;

        public void sendImportRequest(final List<ImportRequest> requests) {
            Log.i(LOG_TAG, "Send an import request");
            mService.handleImportRequest(requests, mListener);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mService = ((VCardService.MyBinder) binder).getService();
            /// M: Error handling, Activity killed might cause mVCardCacheThread un-inited. @{
            if (mVCardCacheThread == null) {
                Log.e(LOG_TAG,
                        "[onServiceConnected]mVCardCacheThread is null, some error happens.");
                VcardUtils.showErrorInfo(R.string.vcard_import_request_rejected_message,
                        ImportVCardActivity.this);
                return;
            }
            /// @}
            Log.i(LOG_TAG,
                    String.format("Connected to VCardService. Kick a vCard cache thread (uri: %s)",
                            Arrays.toString(mVCardCacheThread.getSourceUris())));
            mVCardCacheThread.start();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(LOG_TAG, "Disconnected from VCardService");
        }

        /**
         * M: @return true if the service has been binded,false else.
         */
        public boolean isServiceBinded() {
            return mService != null;
        }

        /**
         * M: ALPS01022668 first vcard import finished, second vcard still
         * caching, can't stop service in this case. @{
         */
        public void setVCardCaching(boolean cache) {
            Log.d(LOG_TAG, "setVCardCaching: " + cache);
            mService.mCaching = cache;
        }
        /** @} */
    }

    /**
     * Caches given vCard files into a local directory, and sends actual import request to
     * {@link VCardService}.
     *
     * We need to cache given files into local storage. One of reasons is that some data (as Uri)
     * may have special permissions. Callers may allow only this Activity to access that content,
     * not what this Activity launched (like {@link VCardService}).
     */
    private class VCardCacheThread extends Thread
            implements DialogInterface.OnCancelListener {
        private boolean mCanceled;
        private PowerManager.WakeLock mWakeLock;
        private VCardParser mVCardParser;
        private final Uri[] mSourceUris;  // Given from a caller.
        private final byte[] mSource;
        private final String mDisplayName;

        public VCardCacheThread(final Uri[] sourceUris) {
            mSourceUris = sourceUris;
            mSource = null;
            final Context context = ImportVCardActivity.this;
            final PowerManager powerManager =
                    (PowerManager)context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_DIM_WAKE_LOCK |
                    PowerManager.ON_AFTER_RELEASE, LOG_TAG);
            mDisplayName = null;
        }

        @Override
        public void finalize() {
            if (mWakeLock != null && mWakeLock.isHeld()) {
                Log.w(LOG_TAG, "WakeLock is being held.");
                mWakeLock.release();
            }
        }

        @Override
        public void run() {
            Log.i(LOG_TAG, "vCard cache thread starts running.");
            if (mConnection == null) {
                throw new NullPointerException("vCard cache thread must be launched "
                        + "after a service connection is established");
            }

            mWakeLock.acquire();
            try {
                /// M: ALPS01022668 first vcard import finished,
                //  second vcard still caching, can't stop service in this case.
                mConnection.setVCardCaching(true);
                if (mCanceled == true) {
                    Log.i(LOG_TAG, "vCard cache operation is canceled.");
                    return;
                }

                final Context context = ImportVCardActivity.this;
                // Uris given from caller applications may not be opened twice: consider when
                // it is not from local storage (e.g. "file:///...") but from some special
                // provider (e.g. "content://...").
                // Thus we have to once copy the content of Uri into local storage, and read
                // it after it.
                //
                // We may be able to read content of each vCard file during copying them
                // to local storage, but currently vCard code does not allow us to do so.
                int cache_index = 0;
                ArrayList<ImportRequest> requests = new ArrayList<ImportRequest>();
                if (mSource != null) {
                    try {
                        requests.add(constructImportRequest(mSource, null, mDisplayName));
                    } catch (VCardException e) {
                        Log.e(LOG_TAG, "Maybe the file is in wrong format", e);
                        showFailureNotification(R.string.fail_reason_not_supported);
                        return;
                    }
                } else {
                    final ContentResolver resolver =
                            ImportVCardActivity.this.getContentResolver();
                    for (Uri sourceUri : mSourceUris) {
                        String filename = null;
                        // Note: caches are removed by VCardService.
                        while (true) {
                            filename = VCardService.CACHE_FILE_PREFIX + cache_index + ".vcf";
                            final File file = context.getFileStreamPath(filename);
                            if (!file.exists()) {
                                break;
                            } else {
                                if (cache_index == Integer.MAX_VALUE) {
                                    throw new RuntimeException("Exceeded cache limit");
                                }
                                cache_index++;
                            }
                        }
                        Uri localDataUri = null;

                        try {
                            localDataUri = copyTo(sourceUri, filename);
                        } catch (SecurityException e) {
                            Log.e(LOG_TAG, "SecurityException", e);
                            showFailureNotification(R.string.fail_reason_io_error);
                            return;
                        }
                        if (mCanceled) {
                            Log.i(LOG_TAG, "vCard cache operation is canceled.");
                            break;
                        }
                        if (localDataUri == null) {
                            Log.w(LOG_TAG, "destUri is null");
                            break;
                        }

                        String displayName = null;
                        Cursor cursor = null;
                        // Try to get a display name from the given Uri. If it fails, we just
                        // pick up the last part of the Uri.
                        try {
                            cursor = resolver.query(sourceUri,
                                    new String[] { OpenableColumns.DISPLAY_NAME },
                                    null, null, null);
                            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                                if (cursor.getCount() > 1) {
                                    Log.w(LOG_TAG, "Unexpected multiple rows: "
                                            + cursor.getCount());
                                }
                                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                                if (index >= 0) {
                                    displayName = cursor.getString(index);
                                }
                            }
                        /// M: @{
                        } catch (Exception e) {
                            Log.w(LOG_TAG, "exception caught ", e);
                        /// @}
                        } finally {
                            if (cursor != null) {
                                cursor.close();
                            }
                        }
                        if (TextUtils.isEmpty(displayName)) {
                            displayName = sourceUri.getLastPathSegment();
                        }

                        final ImportRequest request;
                        try {
                            request = constructImportRequest(null, localDataUri, displayName);
                        } catch (VCardException e) {
                            Log.e(LOG_TAG, "Maybe the file is in wrong format", e);
                            /// M: Fixed cr ALPS00598462 @{
                            String reason = getString(R.string.fail_reason_not_supported);
                            VcardUtils.showFailureNotification(ImportVCardActivity.this, reason ,
                                    displayName , cache_index, mHandler);
                            continue;
                            /// @}
                        /// Bug fix ALPS00318987 @{
                        } catch (IllegalArgumentException e) {
                            Log.e(LOG_TAG, "Maybe the file is in wrong format", e);
                            /// M: Fixed cr ALPS00598462
                            String reason = getString(R.string.fail_reason_not_supported);
                            VcardUtils.showFailureNotification(ImportVCardActivity.this, reason ,
                                    displayName , cache_index, mHandler);
                            continue;
                        /// @}
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "Unexpected IOException", e);
                            /// M: Fixed cr ALPS00598462 @{
                            String reason = getString(R.string.fail_reason_io_error);
                            VcardUtils.showFailureNotification(ImportVCardActivity.this, reason ,
                                    displayName , cache_index, mHandler);
                            continue;
                            /// @}
                        }
                        if (mCanceled) {
                            Log.i(LOG_TAG, "vCard cache operation is canceled.");
                            return;
                        }
                        requests.add(request);
                    }
                }
                if (!requests.isEmpty()) {
                    mConnection.sendImportRequest(requests);
                } else {
                    Log.w(LOG_TAG, "Empty import requests. Ignore it.");
                }
            } catch (OutOfMemoryError e) {
                Log.e(LOG_TAG, "OutOfMemoryError occured during caching vCard");
                System.gc();
                runOnUiThread(new DialogDisplayer(
                        getString(R.string.fail_reason_low_memory_during_import)));
            } catch (FileNotFoundException e) {
                // / M: Fix ALPS00874343,if the vcf file is not exist,toast
                // error msg. @{
                Log.w(LOG_TAG, "[run] the vcf file is not found when import! exception:" + e);
                VcardUtils.showErrorInfo(R.string.import_failure_no_vcard_file,
                        ImportVCardActivity.this);
                // / @}
            } catch (IOException e) {
                Log.e(LOG_TAG, "IOException during caching vCard", e);
                runOnUiThread(new DialogDisplayer(
                        getString(R.string.fail_reason_io_error)));
            } finally {
                Log.i(LOG_TAG, "Finished caching vCard.");
                /// M: ALPS01022668 first vcard import finished,
                //  second vcard still caching, can't stop service in this case.
                mConnection.setVCardCaching(false);
                mWakeLock.release();
                /// M: @{
                if (!ImportVCardActivity.this.isFinishing() && (null != mConnection) &&
                        mConnection.isServiceBinded()) {
                    unbindService(mConnection);
                    mConnection = null;
                } else {
                    Log.d(LOG_TAG, "in VcardCacheThread, Run(), mConnection==null !!! ");
                }
                /// @}
                mProgressDialogForCachingVCard.dismiss();
                mProgressDialogForCachingVCard = null;
                finish();
            }
        }

        /**
         * Copy the content of sourceUri to the destination.
         */
        private Uri copyTo(final Uri sourceUri, String filename) throws IOException {
            Log.i(LOG_TAG, String.format("Copy a Uri to app local storage (%s -> %s)",
                    sourceUri, filename));
            final Context context = ImportVCardActivity.this;
            final ContentResolver resolver = context.getContentResolver();
            ReadableByteChannel inputChannel = null;
            WritableByteChannel outputChannel = null;
            Uri destUri = null;
            try {
                inputChannel = Channels.newChannel(resolver.openInputStream(sourceUri));
                destUri = Uri.parse(context.getFileStreamPath(filename).toURI().toString());
                outputChannel = context.openFileOutput(filename, Context.MODE_PRIVATE).getChannel();
                final ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
                while (inputChannel.read(buffer) != -1) {
                    if (mCanceled) {
                        Log.d(LOG_TAG, "Canceled during caching " + sourceUri);
                        return null;
                    }
                    buffer.flip();
                    outputChannel.write(buffer);
                    buffer.compact();
                }
                buffer.flip();
                while (buffer.hasRemaining()) {
                    outputChannel.write(buffer);
                }
            } finally {
                if (inputChannel != null) {
                    try {
                        inputChannel.close();
                    } catch (IOException e) {
                        Log.w(LOG_TAG, "Failed to close inputChannel.");
                    }
                }
                if (outputChannel != null) {
                    try {
                        outputChannel.close();
                    } catch (IOException e) {
                        Log.w(LOG_TAG, "Failed to close outputChannel");
                    }
                }
            }
            return destUri;
        }

        /**
         * Reads localDataUri (possibly multiple times) and constructs {@link ImportRequest} from
         * its content.
         *
         * @arg localDataUri Uri actually used for the import. Should be stored in
         * app local storage, as we cannot guarantee other types of Uris can be read
         * multiple times. This variable populates {@link ImportRequest#uri}.
         * @arg displayName Used for displaying information to the user. This variable populates
         * {@link ImportRequest#displayName}.
         */
        private ImportRequest constructImportRequest(final byte[] data,
                final Uri localDataUri, final String displayName)
                throws IOException, VCardException {
            final ContentResolver resolver = ImportVCardActivity.this.getContentResolver();
            VCardEntryCounter counter = null;
            VCardSourceDetector detector = null;
            int vcardVersion = VCARD_VERSION_V21;
            try {
                boolean shouldUseV30 = false;
                InputStream is;
                if (data != null) {
                    is = new ByteArrayInputStream(data);
                } else {
                    is = resolver.openInputStream(localDataUri);
                }
                mVCardParser = new VCardParser_V21();
                try {
                    counter = new VCardEntryCounter();
                    detector = new VCardSourceDetector();
                    mVCardParser.addInterpreter(counter);
                    mVCardParser.addInterpreter(detector);
                    mVCardParser.parse(is);
                } catch (VCardVersionException e1) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }

                    shouldUseV30 = true;
                    if (data != null) {
                        is = new ByteArrayInputStream(data);
                    } else {
                        is = resolver.openInputStream(localDataUri);
                    }
                    mVCardParser = new VCardParser_V30();
                    try {
                        counter = new VCardEntryCounter();
                        detector = new VCardSourceDetector();
                        mVCardParser.addInterpreter(counter);
                        mVCardParser.addInterpreter(detector);
                        mVCardParser.parse(is);
                    } catch (VCardVersionException e2) {
                        throw new VCardException("vCard with unspported version.");
                    }
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                        }
                    }
                }

                vcardVersion = shouldUseV30 ? VCARD_VERSION_V30 : VCARD_VERSION_V21;
            } catch (VCardNestedException e) {
                Log.w(LOG_TAG, "Nested Exception is found (it may be false-positive).");
                // Go through without throwing the Exception, as we may be able to detect the
                // version before it
            }
            return new ImportRequest(mAccount,
                    data, localDataUri, displayName,
                    detector.getEstimatedType(),
                    detector.getEstimatedCharset(),
                    vcardVersion, counter.getCount());
        }

        public Uri[] getSourceUris() {
            return mSourceUris;
        }

        public void cancel() {
            mCanceled = true;
            if (mVCardParser != null) {
                mVCardParser.cancel();
            }
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            Log.i(LOG_TAG, "Cancel request has come. Abort caching vCard.");
            cancel();
        }
    }

    private class ImportTypeSelectedListener implements
            DialogInterface.OnClickListener {
        public static final int IMPORT_ONE = 0;
        public static final int IMPORT_MULTIPLE = 1;
        public static final int IMPORT_ALL = 2;
        public static final int IMPORT_TYPE_SIZE = 3;

        private int mCurrentIndex;

        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                switch (mCurrentIndex) {
                case IMPORT_ALL:
                    importVCardFromSDCard(mAllVCardFileList);
                    break;
                case IMPORT_MULTIPLE:
                    showDialog(R.id.dialog_select_multiple_vcard);
                    /**
                     * M: Default show dialog_select_multiple_vcard,should
                     * disable the OK button @{
                     */
                    Button btnOK = mVcardFileSelectDialog
                            .getButton(DialogInterface.BUTTON_POSITIVE);
                    btnOK.setEnabled(false);
                    /** @} */
                    break;
                default:
                    showDialog(R.id.dialog_select_one_vcard);
                    break;
                }
                /** M: Bug Fix, CR ID: ALPS00110214 */
                setResult(ImportExportUtils.RESULT_CODE);
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                finish();
            } else {
                mCurrentIndex = which;
            }
        }
    }

    private class VCardSelectedListener implements
            DialogInterface.OnClickListener, DialogInterface.OnMultiChoiceClickListener {
        private int mCurrentIndex;
        private Set<Integer> mSelectedIndexSet;

        public VCardSelectedListener(boolean multipleSelect) {
            mCurrentIndex = 0;
            if (multipleSelect) {
                mSelectedIndexSet = new HashSet<Integer>();
            }
        }

        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                if (mSelectedIndexSet != null) {
                    List<VCardFile> selectedVCardFileList = new ArrayList<VCardFile>();
                    final int size = mAllVCardFileList.size();
                    // We'd like to sort the files by its index, so we do not use Set iterator.
                    for (int i = 0; i < size; i++) {
                        if (mSelectedIndexSet.contains(i)) {
                            selectedVCardFileList.add(mAllVCardFileList.get(i));
                        }
                    }
                    importVCardFromSDCard(selectedVCardFileList);
                } else {
                    importVCardFromSDCard(mAllVCardFileList.get(mCurrentIndex));
                }
                /** M: Bug Fix CR ID: ALPS00110214 */
                setResult(ImportExportUtils.RESULT_CODE);
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                finish();
            } else {
                // Some file is selected.
                mCurrentIndex = which;
                if (mSelectedIndexSet != null) {
                    /** M: When selected file is none, should disable the OK button @{ */
                    Button btnOK = mVcardFileSelectDialog.
                            getButton(DialogInterface.BUTTON_POSITIVE);
                    boolean enabled = true;
                    if (mSelectedIndexSet.contains(which)) {
                        mSelectedIndexSet.remove(which);
                        if (mSelectedIndexSet.size() == 0) {
                            enabled = false;
                        }
                    } else {
                        mSelectedIndexSet.add(which);
                    }
                    btnOK.setEnabled(enabled);
                    /** @} */
                }
            }
        }

        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
            if (mSelectedIndexSet == null || (mSelectedIndexSet.contains(which) == isChecked)) {
                Log.e(LOG_TAG, String.format("Inconsist state in index %d (%s)", which,
                        mAllVCardFileList.get(which).getCanonicalPath()));
            } else {
                onClick(dialog, which);
            }
        }
    }

    /**
     * Thread scanning VCard from SDCard. After scanning, the dialog which lets a user select
     * a vCard file is shown. After the choice, VCardReadThread starts running.
     */
    private class VCardScanThread extends Thread implements OnCancelListener, OnClickListener {
        private boolean mCanceled;
        private boolean mGotIOException;
        private File mRootDirectory;

        // To avoid recursive link.
        private Set<String> mCheckedPaths;
        private PowerManager.WakeLock mWakeLock;

        private class CanceledException extends Exception {
        }

        public VCardScanThread(File sdcardDirectory) {
            mCanceled = false;
            mGotIOException = false;
            mRootDirectory = sdcardDirectory;
            mCheckedPaths = new HashSet<String>();
            PowerManager powerManager = (PowerManager)ImportVCardActivity.this.getSystemService(
                    Context.POWER_SERVICE);
            mWakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_DIM_WAKE_LOCK |
                    PowerManager.ON_AFTER_RELEASE, LOG_TAG);
        }

        @Override
        public void run() {
            mAllVCardFileList = new Vector<VCardFile>();
            try {
                mWakeLock.acquire();
                getVCardFileRecursively(mRootDirectory);
            } catch (CanceledException e) {
                mCanceled = true;
            } catch (IOException e) {
                mGotIOException = true;
            } finally {
                mWakeLock.release();
            }

            if (mCanceled) {
                mAllVCardFileList = null;
            }

            mProgressDialogForScanVCard.dismiss();
            mProgressDialogForScanVCard = null;

            if (mGotIOException) {
                runOnUiThread(new DialogDisplayer(R.id.dialog_io_exception));
            } else if (mCanceled) {
                finish();
            } else {
                int size = mAllVCardFileList.size();
                final Context context = ImportVCardActivity.this;
                if (size == 0) {
                    runOnUiThread(new DialogDisplayer(R.id.dialog_vcard_not_found));
                } else {
                    startVCardSelectAndImport();
                }
            }
        }

        private void getVCardFileRecursively(File directory)
                throws CanceledException, IOException {
            if (mCanceled) {
                throw new CanceledException();
            }

            // e.g. secured directory may return null toward listFiles().
            final File[] files = directory.listFiles();
            if (files == null) {
                final String currentDirectoryPath = directory.getCanonicalPath();
                final String secureDirectoryPath =
                        mRootDirectory.getCanonicalPath().concat(SECURE_DIRECTORY_NAME);
                if (!TextUtils.equals(currentDirectoryPath, secureDirectoryPath)) {
                    Log.w(LOG_TAG, "listFiles() returned null (directory: " + directory + ")");
                }
                return;
            }
            for (File file : directory.listFiles()) {
                if (mCanceled) {
                    throw new CanceledException();
                }
                String canonicalPath = file.getCanonicalPath();
                if (mCheckedPaths.contains(canonicalPath)) {
                    continue;
                }

                mCheckedPaths.add(canonicalPath);

                if (file.isDirectory()) {
                    getVCardFileRecursively(file);
                } else if (canonicalPath.toLowerCase().endsWith(".vcf") &&
                        file.canRead()) {
                    String fileName = file.getName();
                    VCardFile vcardFile = new VCardFile(
                            fileName, canonicalPath, file.lastModified());
                    mAllVCardFileList.add(vcardFile);
                }
            }
        }

        public void onCancel(DialogInterface dialog) {
            mCanceled = true;
        }

        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_NEGATIVE) {
                mCanceled = true;
            }
        }
    }

    private void startVCardSelectAndImport() {
        int size = mAllVCardFileList.size();
        if (getResources().getBoolean(R.bool.config_import_all_vcard_from_sdcard_automatically) ||
                size == 1) {
            /** M: Bug Fix, CR ID: ALPS00120656 @{ */
            if (size == 1) {
                setResult(ImportExportUtils.RESULT_CODE);
            }
            /** @} */
            importVCardFromSDCard(mAllVCardFileList);
        } else if (getResources().getBoolean(R.bool.config_allow_users_select_all_vcard_import)) {
            runOnUiThread(new DialogDisplayer(R.id.dialog_select_import_type));
        } else {
            runOnUiThread(new DialogDisplayer(R.id.dialog_select_one_vcard));
        }
    }

    private void importVCardFromSDCard(final List<VCardFile> selectedVCardFileList) {
        Log.d(LOG_TAG, "[importVCardFromSDCard]");
        final int size = selectedVCardFileList.size();
        String[] uriStrings = new String[size];
        int i = 0;
        for (VCardFile vcardFile : selectedVCardFileList) {
            uriStrings[i] = getEncodeUriString(vcardFile.getCanonicalPath());
            i++;
        }
        importVCard(uriStrings);
    }

    private void importVCardFromSDCard(final VCardFile vcardFile) {
        Log.d(LOG_TAG, "[importVCardFromSDCard:vcardFile]");
        importVCard(new Uri[] {Uri.parse(getEncodeUriString(vcardFile.getCanonicalPath()))});
    }

    private void importVCard(final Uri uri) {
        importVCard(new Uri[] {uri});
    }

    private void importVCard(final String[] uriStrings) {
        final int length = uriStrings.length;
        final Uri[] uris = new Uri[length];
        for (int i = 0; i < length; i++) {
            uris[i] = Uri.parse(uriStrings[i]);
        }
        importVCard(uris);
    }

    private void importVCard(final Uri[] uris) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    mVCardCacheThread = new VCardCacheThread(uris);
                    mListener = new NotificationImportExportListener(ImportVCardActivity.this);
                    showDialog(R.id.dialog_cache_vcard);
                }
            }
        });
    }

    ///M:
    private Dialog getSelectImportTypeDialog() {
        /// M: ALPS02413700 Don't show this dialog if vcard list is empty.  @{
        final int size = mAllVCardFileList.size();
        if (size == 0) {
            Log.d(LOG_TAG, "getSelectImportTypeDialog size: " + size);
            return null;
        }
        // @}

        final DialogInterface.OnClickListener listener = new ImportTypeSelectedListener();
        final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.select_vcard_title)
                .setPositiveButton(android.R.string.ok, listener)
                .setOnCancelListener(mCancelListener)
                .setNegativeButton(android.R.string.cancel, mCancelListener);

        final String[] items = new String[ImportTypeSelectedListener.IMPORT_TYPE_SIZE];
        items[ImportTypeSelectedListener.IMPORT_ONE] =
                getString(R.string.import_one_vcard_string);
        items[ImportTypeSelectedListener.IMPORT_MULTIPLE] =
                getString(R.string.import_multiple_vcard_string);
        items[ImportTypeSelectedListener.IMPORT_ALL] =
                getString(R.string.import_all_vcard_string);
        builder.setSingleChoiceItems(items, ImportTypeSelectedListener.IMPORT_ONE, listener);
        return builder.create();
    }

    private Dialog getVCardFileSelectDialog(boolean multipleSelect) {
        final int size = mAllVCardFileList.size();
        /// M: ALPS02413700 Don't show this dialog if vcard list is empty. @{
        if (size == 0) {
            Log.d(LOG_TAG, "getVCardFileSelectDialog size: " + size);
            return null;
        }
        // @}

        final VCardSelectedListener listener = new VCardSelectedListener(multipleSelect);
        final AlertDialog.Builder builder =
                new AlertDialog.Builder(this)
                        .setTitle(R.string.select_vcard_title)
                        .setPositiveButton(android.R.string.ok, listener)
                        .setOnCancelListener(mCancelListener)
                        .setNegativeButton(android.R.string.cancel, mCancelListener);

        CharSequence[] items = new CharSequence[size];
        //DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < size; i++) {
            VCardFile vcardFile = mAllVCardFileList.get(i);
            SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
            stringBuilder.append(vcardFile.getName());
            stringBuilder.append('\n');
            int indexToBeSpanned = stringBuilder.length();
            // Smaller date text looks better, since each file name becomes easier to read.
            // The value set to RelativeSizeSpan is arbitrary. You can change it to any other
            // value (but the value bigger than 1.0f would not make nice appearance :)

            /// M:fix ALPS02371510.resolved show file time not sync setting @{
            stringBuilder.append(
                    "(" + DateUtils.formatDateTime (this,vcardFile.getLastModified(),
                            DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME |
                            DateUtils.FORMAT_SHOW_YEAR) + ")");
            //stringBuilder.append(
            //      "(" + dateFormat.format(new Date(vcardFile.getLastModified())) + ")");
            // @}

            stringBuilder.setSpan(
                    new RelativeSizeSpan(0.7f), indexToBeSpanned, stringBuilder.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            items[i] = stringBuilder;
        }
        if (multipleSelect) {
            builder.setMultiChoiceItems(items, (boolean[])null, listener);
        } else {
            builder.setSingleChoiceItems(items, 0, listener);
        }
        return builder.create();
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        //M: add to listen sd/sim card plugout state
        registerListenSDCardAndSubState();

        if (RequestImportVCardPermissionsActivity.startPermissionActivity(this)) {
            return;
        }

        String accountName = null;
        String accountType = null;
        String dataSet = null;
        final Intent intent = getIntent();
        if (intent != null) {
            accountName = intent.getStringExtra(SelectAccountActivity.ACCOUNT_NAME);
            accountType = intent.getStringExtra(SelectAccountActivity.ACCOUNT_TYPE);
            dataSet = intent.getStringExtra(SelectAccountActivity.DATA_SET);
            /// M: New Feature ALPS00276020, support multiple storages for Contacts import/export.
            mSourcePath = intent.getStringExtra("source_path");
        } else {
            Log.e(LOG_TAG, "intent does not exist");
        }

        if (!TextUtils.isEmpty(accountName) && !TextUtils.isEmpty(accountType)) {
            mAccount = new AccountWithDataSet(accountName, accountType, dataSet);
        } else {
            final AccountTypeManager accountTypes = AccountTypeManager.getInstance(this);
            final List<AccountWithDataSet> accountList = accountTypes.getAccounts(true);
            if (accountList.size() == 0) {
                mAccount = null;
            } else if (accountList.size() == 1) {
                mAccount = accountList.get(0);
            } else {
                /** M: Bug Fix for CR: ALPS00403117 @{ */
                int validAccountSize = accountList.size();
                for (AccountWithDataSet account : accountList) {
                    if (AccountTypeUtils.isAccountTypeIccCard(account.type)) {
                        validAccountSize--;
                    } else {
                        mAccount = account;
                    }
                }
                if (validAccountSize > 1) {
                    startActivityForResult(new Intent(this, SelectAccountActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), SELECT_ACCOUNT);
                    return ;
                }
                /** @} */
            }
        }

        startImport();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == SELECT_ACCOUNT) {
            if (resultCode == Activity.RESULT_OK) {
                mAccount = new AccountWithDataSet(
                        intent.getStringExtra(SelectAccountActivity.ACCOUNT_NAME),
                        intent.getStringExtra(SelectAccountActivity.ACCOUNT_TYPE),
                        intent.getStringExtra(SelectAccountActivity.DATA_SET));
                startImport();
            } else {
                if (resultCode != Activity.RESULT_CANCELED) {
                    Log.w(LOG_TAG, "Result code was not OK nor CANCELED: " + resultCode);
                }
                finish();
            }
        } else if (requestCode == REQUEST_OPEN_DOCUMENT) {
            if (resultCode == Activity.RESULT_OK) {
                final ClipData clipData = intent.getClipData();
                if (clipData != null) {
                    final ArrayList<Uri> uris = new ArrayList<>();
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        final Uri uri = item.getUri();
                        if (uri != null) {
                            uris.add(uri);
                        }
                    }
                    if (uris.isEmpty()) {
                        Log.w(LOG_TAG, "No vCard was selected for import");
                        finish();
                    } else {
                        Log.i(LOG_TAG, "Multiple vCards selected for import: " + uris);
                        importVCard(uris.toArray(new Uri[0]));
                    }
                } else {
                    final Uri uri = intent.getData();
                    if (uri != null) {
                        Log.i(LOG_TAG, "vCard selected for import: " + uri);
                        importVCard(uri);
                    } else {
                        Log.w(LOG_TAG, "No vCard was selected for import");
                        finish();
                    }
                }
            } else {
                if (resultCode != Activity.RESULT_CANCELED) {
                    Log.w(LOG_TAG, "Result code was not OK nor CANCELED" + resultCode);
                }
                finish();
            }
        }
    }

    private void startImport() {
        // Handle inbound files
        Uri uri = getIntent().getData();
        if (uri != null) {
            Log.i(LOG_TAG, "Starting vCard import using Uri " + uri);
            importVCard(uri);
        } else {
            Log.i(LOG_TAG, "Start vCard without Uri. The user will select vCard manually.");
           /**
             * M: @{
            final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType(VCardService.X_VCARD_MIME_TYPE);
           intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
           intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            startActivityForResult(intent, REQUEST_OPEN_DOCUMENT);
             @}*/
            doScanExternalStorageAndImportVCard();
        }
    }

    @Override
    protected Dialog onCreateDialog(int resId, Bundle bundle) {
        switch (resId) {
            ///M:@{
            case R.id.dialog_searching_vcard: {
                Log.d(LOG_TAG, "[onCreateDialog]dialog_search_vcard");
                ///Change for CR ALPS00683398. This will be called from
                //  Activity.restoreManagedDialogs() when the contacts process has been killed;
                //  So add checking "mVCardScanThread != null" before use it.
                if (mProgressDialogForScanVCard == null && mVCardScanThread != null) {
                    String message = getString(R.string.searching_vcard_message);
                    mProgressDialogForScanVCard =
                        ProgressDialog.show(this, "", message, true, false);
                    mProgressDialogForScanVCard.setOnCancelListener(mVCardScanThread);
                    mVCardScanThread.start();
                }
                return mProgressDialogForScanVCard;
            }
            case R.id.dialog_sdcard_not_found: {
                Log.d(LOG_TAG, "[onCreateDialog]dialog_sdcard_not_found");
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setMessage(R.string.no_sdcard_message)
                    .setOnCancelListener(mCancelListener)
                    .setPositiveButton(android.R.string.ok, mCancelListener);
                return builder.create();
            }
            case R.id.dialog_vcard_not_found: {
                Log.d(LOG_TAG, "[onCreateDialog]dialog_vcard_not_found");
                final String message = getString(R.string.import_no_vcard_dialog_text, mVolumeName);
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setMessage(message)
                        .setOnCancelListener(mCancelListener)
                        .setPositiveButton(android.R.string.ok, mCancelListener);
                return builder.create();
            }
            case R.id.dialog_select_import_type: {
                Log.d(LOG_TAG, "[onCreateDialog]dialog_select_import_type");
                return getSelectImportTypeDialog();
            }
            case R.id.dialog_select_multiple_vcard: {
                Log.d(LOG_TAG, "[onCreateDialog]dialog_select_multiple_vcard");
                mVcardFileSelectDialog = (AlertDialog) getVCardFileSelectDialog(true);
                return mVcardFileSelectDialog;
            }
            case R.id.dialog_select_one_vcard: {
                Log.d(LOG_TAG, "[onCreateDialog]dialog_select_one_vcard");
                return getVCardFileSelectDialog(false);
            }
            case R.id.dialog_io_exception: {
                Log.d(LOG_TAG, "[onCreateDialog]dialog_io_exception");
                String message = (getString(R.string.scanning_sdcard_failed_message,
                        getString(R.string.fail_reason_io_error)));
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setMessage(message)
                    .setOnCancelListener(mCancelListener)
                    .setPositiveButton(android.R.string.ok, mCancelListener);
                return builder.create();
            }
            ///M: @}
            case R.id.dialog_cache_vcard: {
                Log.d(LOG_TAG, "[onCreateDialog]dialog_cache_vcard");
                if (mProgressDialogForCachingVCard == null) {
                    Log.d(LOG_TAG, "[onCreateDialog][dialog_cache_vcard]Dialog first created");
                    final String title = getString(R.string.caching_vcard_title);
                    final String message = getString(R.string.caching_vcard_message);
                    mProgressDialogForCachingVCard = new ProgressDialog(this);
                    mProgressDialogForCachingVCard.setTitle(title);
                    mProgressDialogForCachingVCard.setMessage(message);
                    mProgressDialogForCachingVCard.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    mProgressDialogForCachingVCard.setOnCancelListener(mVCardCacheThread);
                    startVCardService();
                }
                return mProgressDialogForCachingVCard;
            }
            case R.id.dialog_error_with_message: {
                Log.d(LOG_TAG, "[onCreateDialog]dialog_error_with_message");
                String message = mErrorMessage;
                if (TextUtils.isEmpty(message)) {
                    Log.e(LOG_TAG, "Error message is null while it must not.");
                    message = getString(R.string.fail_reason_unknown);
                }
                final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.reading_vcard_failed_title))
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(message)
                    .setOnCancelListener(mCancelListener)
                    .setPositiveButton(android.R.string.ok, mCancelListener);
                return builder.create();
            }
            default: {
                Log.w(LOG_TAG, "[onCreateDialog]res id is invalid: " + resId);
            }
        }

        return super.onCreateDialog(resId, bundle);
    }

    /* package */ void startVCardService() {
        mConnection = new ImportRequestConnection();

        Log.i(LOG_TAG, "Bind to VCardService.");
        // We don't want the service finishes itself just after this connection.
        Intent intent = new Intent(this, VCardService.class);
        startService(intent);
        bindService(new Intent(this, VCardService.class),
                mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (mProgressDialogForCachingVCard != null) {
            Log.i(LOG_TAG, "Cache thread is still running. Show progress dialog again.");
            showDialog(R.id.dialog_cache_vcard);
        }
    }

    /**
     *  M:Scans vCard in external storage (typically SDCard) and tries to import it.
     * - When there's no SDCard available, an error dialog is shown.
     * - When multiple vCard files are available, asks a user to select one.
     */
    private void doScanExternalStorageAndImportVCard() {
        // TODO: should use getExternalStorageState().
        /// M: New Feature ALPS00276020,
        //  support multiple storages for import/export. @{
        String path = VcardUtils.getExternalPath(mSourcePath);
        Log.d(LOG_TAG,"[doScanExternalStorageAndImportVCard]path : " + path);
        final File file = VcardUtils.getDirectory(path,
                Environment.getExternalStorageDirectory().toString());
        /// @}
        /** M: Bug Fix, CR ID: ALPS00301464 */
        mVolumeName = VcardUtils.getVolumeName(path, this);

        if (!file.exists() || !file.isDirectory() || !file.canRead()) {
            showDialog(R.id.dialog_sdcard_not_found);
        } else {
            mVCardScanThread = new VCardScanThread(file);
            showDialog(R.id.dialog_searching_vcard);
        }
    }

    /* package */ void showFailureNotification(int reasonId) {
        final NotificationManager notificationManager =
                (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        final Notification notification =
                NotificationImportExportListener.constructImportFailureNotification(
                        ImportVCardActivity.this,
                        getString(reasonId));
        notificationManager.notify(NotificationImportExportListener.FAILURE_NOTIFICATION_TAG,
                FAILURE_NOTIFICATION_ID, notification);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                /// M: To modify the toast last a long time issue.
                MtkToast.toast(ImportVCardActivity.this, getString(R.string.vcard_import_failed),
                        Toast.LENGTH_LONG);
            }
        });
    }

    /// M: @{
    private String mSourcePath = null;
    private String mVolumeName = null;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "[onDestroy]");

        //M: add to listen sd/sim card plugout state
        unRegisterListenSDCardAndSubState();

        if (mVCardCacheThread != null && mVCardCacheThread.isAlive()) {
            Log.w(LOG_TAG, "[onDestroy]should not finish Activity when work did not finished");
            mVCardCacheThread.cancel();
        }
    }

    /**
     * fix ALPS02671394. encode vcf fileName.
     * because some vcf filename include some special letter,such as tmp#.vcf,tmp%.vcf.
     * "#","%" cannot encode right,will make FileNotFoundException.
     * full filePathString = filePath + "/"+ uri.encode(fileName).
     */
    private String getEncodeUriString(String filePathString) {
        Log.i(LOG_TAG, "[getEncodeUriString] filePathString: " + filePathString);
        if (TextUtils.isEmpty(filePathString)) {
            Log.e(LOG_TAG, "[getEncodeUriString] filePathString is wrong !");
            return null;
        }
        int lastSeparatorIndex = filePathString.lastIndexOf(File.separator);
        StringBuilder rstPathString = new StringBuilder("file://").append(
                filePathString.substring(0, lastSeparatorIndex)).append(File.separator);
        String encodeFileName = Uri.encode(filePathString.substring(lastSeparatorIndex + 1,
                filePathString.length()));
        rstPathString.append(encodeFileName);
        Log.i(LOG_TAG, "[getEncodeUriString] rstPathString: " + rstPathString.toString());
        return rstPathString.toString();
    }
    /// @}
}
