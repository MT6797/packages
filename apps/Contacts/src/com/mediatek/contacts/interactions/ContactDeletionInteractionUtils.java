/*
 * Copyright (C) 2014 MediaTek Inc.
 * Modification based on code covered by the mentioned copyright
 * and/or permission notice(s).
 */
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

package com.mediatek.contacts.interactions;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

import com.android.contacts.R;

import com.mediatek.contacts.simservice.SIMDeleteProcessor;
import com.mediatek.contacts.simservice.SIMProcessorService;
import com.mediatek.contacts.simservice.SIMServiceUtils;
import com.mediatek.contacts.util.Log;

/**
 * An interaction invoked to delete a contact.
 */
public class ContactDeletionInteractionUtils {
    private static final String TAG = "ContactDeletionInteractionUtils";

    public static boolean doDeleteSimContact(Context context, Uri contactUri, Uri simUri,
            int simIndex, int subId, Fragment fragment) {
        Log.i(TAG, "[doDeleteSimContact]simUri: " + simUri + ",simIndex = " + simIndex
                + ",subId = " + subId);
        if (simUri != null && fragment.isAdded()) {
            Intent intent = new Intent(context, SIMProcessorService.class);
            intent.setData(simUri);
            intent.putExtra(SIMDeleteProcessor.SIM_INDEX, simIndex);
            intent.putExtra(SIMServiceUtils.SERVICE_SUBSCRIPTION_KEY, subId);
            intent.putExtra(SIMServiceUtils.SERVICE_WORK_TYPE, SIMServiceUtils.SERVICE_WORK_DELETE);
            intent.putExtra(SIMDeleteProcessor.LOCAL_CONTACT_URI, contactUri);
            context.startService(intent);
            return true;
        }
        return false;
    }

    public static class ProgressHandler extends Handler {
        private static final int SHOW_PROGRESS_DIALOG = 0;
        private static final int CANCEL_PROGRESS_DIALOG = 1;

        private Context mContext;
        private Fragment mFragment;
        private final ProgressDialog mDialog;

        public ProgressHandler(Context context, Fragment fragment) {
            mContext = context;
            mDialog = new ProgressDialog(mContext);
            mFragment = fragment;
        }

        /**
         * clear the progress message, and send a message to cancel the dialog
         */
        public void cancelProgressDialogIfNeeded() {
            removeMessages(SHOW_PROGRESS_DIALOG);
            sendMessage(obtainMessage(CANCEL_PROGRESS_DIALOG));
        }

        @Override
        public void handleMessage(Message msg) {
            if (!mFragment.isAdded()) {
                Log.w(TAG, "[handleMessage]fragment not added, ignore the message: "
                        + msg.what);
                return;
            }
            Log.i(TAG, "[handleMessage]message: " + msg.what);
            switch (msg.what) {
            case SHOW_PROGRESS_DIALOG:
                mDialog.setMessage(mContext.getString(R.string.please_wait));
                mDialog.setCanceledOnTouchOutside(false);
                mDialog.show();
                break;
            case CANCEL_PROGRESS_DIALOG:
                if (mDialog.isShowing()) {
                    mDialog.dismiss();
                }
                break;
            default:
                break;
            }
        };
    };

}
