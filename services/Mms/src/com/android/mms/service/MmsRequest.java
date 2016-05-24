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

package com.android.mms.service;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.service.carrier.CarrierMessagingService;
import android.service.carrier.ICarrierMessagingCallback;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.mms.service.exception.ApnException;
import com.android.mms.service.exception.MmsHttpException;
import com.android.mms.service.exception.MmsNetworkException;
import com.mediatek.mms.service.ext.IMmsServiceCancelDownloadExt;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.internal.telephony.MmsConfigInfo;

/**
 * Base class for MMS requests. This has the common logic of sending/downloading MMS.
 */
public abstract class MmsRequest {
    private static final int RETRY_TIMES = 3;
    private static final String TAG = "OMH_MMS";

    /**
     * Interface for certain functionalities from MmsService
     */
    public static interface RequestManager {
        /**
         * Enqueue an MMS request
         *
         * @param request the request to enqueue
         */
        public void addSimRequest(MmsRequest request);

        /*
         * @return Whether to auto persist received MMS
         */
        public boolean getAutoPersistingPref();

        /**
         * Read pdu (up to maxSize bytes) from supplied content uri
         * @param contentUri content uri from which to read
         * @param maxSize maximum number of bytes to read
         * @return read pdu (else null in case of error or too big)
         */
        public byte[] readPduFromContentUri(final Uri contentUri, final int maxSize);

        /**
         * Write pdu to supplied content uri
         * @param contentUri content uri to which bytes should be written
         * @param pdu pdu bytes to write
         * @return true in case of success (else false)
         */
        public boolean writePduToContentUri(final Uri contentUri, final byte[] pdu);
    }
    // The URI of persisted message
    protected Uri mMessageUri;
    // The reference to the pending requests manager (i.e. the MmsService)
    protected RequestManager mRequestManager;
    // The SIM id
    protected int mSubId;
    // The creator app
    protected String mCreator;
    // MMS config
    protected Bundle mMmsConfig;
    // MMS config overrides that will be applied to mMmsConfig when we eventually load it.
    protected Bundle mMmsConfigOverrides;
    // Context used to get TelephonyManager.
    protected Context mContext;

    /// M: For Op09 Feature: Cancel Download
    IMmsServiceCancelDownloadExt mMmsServiceCancelDownloadPlugin =
        (IMmsServiceCancelDownloadExt) MmsServicePluginManager
            .getMmsPluginObject(MmsServicePluginManager
                .MMS_PLUGIN_TYPE_MMS_SERVICE_CANCEL_DOWNLOAD);

    public MmsRequest(RequestManager requestManager, Uri messageUri, int subId, String creator,
            Bundle configOverrides, Context context) {
        mRequestManager = requestManager;
        mMessageUri = messageUri;
        mSubId = subId;
        mCreator = creator;
        mMmsConfigOverrides = configOverrides;
        mMmsConfig = null;
        mContext = context;
    }

    public int getSubId() {
        return mSubId;
    }

    private boolean ensureMmsConfigLoaded() {
        if (mMmsConfig == null) {
            // Not yet retrieved from mms config manager. Try getting it.
            final Bundle config = MmsConfigManager.getInstance().getMmsConfigBySubId(mSubId);
            if (config != null) {
                mMmsConfig = config;
                // TODO: Make MmsConfigManager authoritative for user agent and don't consult
                // TelephonyManager.
                final TelephonyManager telephonyManager =
                        (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
                final String userAgent = telephonyManager.getMmsUserAgent();
                if (!TextUtils.isEmpty(userAgent)) {
                    config.putString(SmsManager.MMS_CONFIG_USER_AGENT, userAgent);
                }
                final String userAgentProfileUrl = telephonyManager.getMmsUAProfUrl();
                if (!TextUtils.isEmpty(userAgentProfileUrl)) {
                    config.putString(SmsManager.MMS_CONFIG_UA_PROF_URL, userAgentProfileUrl);
                }
                // Apply overrides
                if (mMmsConfigOverrides != null) {
                    mMmsConfig.putAll(mMmsConfigOverrides);
                }
            }
        }
        return mMmsConfig != null;
    }

    /**
     * Execute the request
     *
     * @param context The context
     * @param networkManager The network manager to use
     */
    public void execute(Context context, MmsNetworkManager networkManager) {
        final String requestId = this.toString();
        LogUtil.i(requestId, "Executing...");
        int result = SmsManager.MMS_ERROR_UNSPECIFIED;
        int httpStatusCode = 0;
        byte[] response = null;
        boolean isRequestSuccessed = false;
        // TODO: add mms data channel check back to fast fail if no way to send mms,
        // when telephony provides such API.
        if (!ensureMmsConfigLoaded()) { // Check mms config
            LogUtil.e(requestId, "mms config is not loaded yet");
            result = SmsManager.MMS_ERROR_CONFIGURATION_ERROR;
        } else if (!prepareForHttpRequest()) { // Prepare request, like reading pdu data from user
            LogUtil.e(requestId, "Failed to prepare for request");
            result = SmsManager.MMS_ERROR_IO_ERROR;
        } else { // Execute
            long retryDelaySecs;
            int retryTimes;
            TelephonyManagerEx telephonyManagerEx;
            MmsConfigInfo mmsConfigInfo = null;
            boolean isOmhEnable = false;
            boolean isOmhCard =false;
            int omhRetryTimes = 0;
            long omhRetryInterval = 0;
            try {
                telephonyManagerEx = TelephonyManagerEx.getDefault();
                mmsConfigInfo = telephonyManagerEx.getMmsConfigInfo(mSubId);
                isOmhEnable = telephonyManagerEx.isOmhEnable(mSubId);
                isOmhCard = telephonyManagerEx.isOmhCard(mSubId);
                LogUtil.d(TAG, "MmsRequest.execute:isOmhEnable="+isOmhEnable
                        +" isOmhCard="+isOmhCard);
                if (mmsConfigInfo != null) {
                    omhRetryTimes = mmsConfigInfo.mRetryTimes;
                    omhRetryInterval = mmsConfigInfo.mRetryInterval;
                    LogUtil.d(TAG, "MmsRequest.execute:"
                            +" mmsConfigInfo != null"
                            +" mmsConfigInfo.mRetryTimes ="+omhRetryTimes
                            +" mmsConfigInfo.mRetryInterval ="+omhRetryInterval);
                }
            } catch (Exception e) {
                // TODO: handle exception
                LogUtil.d(TAG,"MmsRequest.execute:" + e);
            }
            if (isOmhEnable && isOmhCard && mmsConfigInfo != null) {
                if (omhRetryTimes != 0) {
                    retryTimes = omhRetryTimes;
                } else {
                    retryTimes = RETRY_TIMES;
                }
                if (omhRetryInterval != 0) {
                    retryDelaySecs = omhRetryInterval;
                } else {
                    retryDelaySecs = 2;
                }
            } else {
                retryDelaySecs = 2;
                retryTimes = RETRY_TIMES;
            }
            // Try multiple times of MMS HTTP request
            for (int i = 0; i < retryTimes; i++) {
                try {
                    ///M: For OP09 Feature: CancelDownload.
                    mMmsServiceCancelDownloadPlugin
                            .addAcquireNetworkUri(mMmsServiceCancelDownloadPlugin
                                    .getCachedLocationUri(this.hashCode()));
                    networkManager.acquireNetwork(requestId);
                    ///M: For OP09 Feature: CancelDownload.
                    mMmsServiceCancelDownloadPlugin
                            .removeAcquireNetWorkUri(mMmsServiceCancelDownloadPlugin
                                    .getCachedLocationUri(this.hashCode()));
                    final String apnName = networkManager.getApnName();
                    LogUtil.d(requestId, "APN name is " + apnName);
                    try {
                        ApnSettings apn = null;
                        try {
                            apn = ApnSettings.load(context, apnName, mSubId, requestId);
                        } catch (ApnException e) {
                            // If no APN could be found, fall back to trying without the APN name
                            if (apnName == null) {
                                // If the APN name was already null then don't need to retry
                                throw (e);
                            }
                            LogUtil.i(requestId, "No match with APN name: "
                                    + apnName + ", try with no name");
                            apn = ApnSettings.load(context, null, mSubId, requestId);
                        }
                        LogUtil.i(requestId, "Using " + apn.toString());
                        response = doHttp(context, networkManager, apn);
                        result = checkResponse(response);
                        // Success
                        isRequestSuccessed = true;
                        break;
                    } finally {
                        networkManager.releaseNetwork(requestId);
                    }
                } catch (ApnException e) {
                    LogUtil.e(requestId, "APN failure", e);
                    result = SmsManager.MMS_ERROR_INVALID_APN;
                    break;
                } catch (MmsNetworkException e) {
                    LogUtil.e(requestId, "MMS network acquiring failure", e);
                    result = SmsManager.MMS_ERROR_UNABLE_CONNECT_MMS;
                    ///M: For OP09 Feature: CancelDownload.
                    if (mMmsServiceCancelDownloadPlugin.isCancelDownloadEnable()) {
                        String locationUri = mMmsServiceCancelDownloadPlugin
                                .getCachedLocationUri(this.hashCode());
                        mMmsServiceCancelDownloadPlugin.removeAcquireNetWorkUri(locationUri);
                        if (mMmsServiceCancelDownloadPlugin.needBeCanceled(locationUri)) {
                            mMmsServiceCancelDownloadPlugin.removeCanceledUri(locationUri);
                            result = SmsManager.MMS_ERROR_UNSPECIFIED;
                            LogUtil.e(requestId, "set result = MMS_ERROR_UNSPECIFIED");
                            break;
                        }
                    }
                    /// @}
                    // Retry
                } catch (MmsHttpException e) {
                    LogUtil.e(requestId, "HTTP or network I/O failure", e);
                    result = SmsManager.MMS_ERROR_HTTP_FAILURE;
                    httpStatusCode = e.getStatusCode();
                    ///M: For OP09 Feature: CancelDownload.
                    if (mMmsServiceCancelDownloadPlugin.isCancelDownloadEnable()) {
                        String locationUri = mMmsServiceCancelDownloadPlugin
                                .getCachedLocationUri(this.hashCode());
                        mMmsServiceCancelDownloadPlugin.removeAcquireNetWorkUri(locationUri);
                        if (mMmsServiceCancelDownloadPlugin.needBeCanceled(locationUri)) {
                            mMmsServiceCancelDownloadPlugin.removeCanceledUri(locationUri);
                            result = SmsManager.MMS_ERROR_UNSPECIFIED;
                            LogUtil.e(requestId, "set result = MMS_ERROR_UNSPECIFIED");
                            break;
                        }
                    }
                    /// @}
                    // Retry
                } catch (Exception e) {
                    LogUtil.e(requestId, "Unexpected failure", e);
                    result = SmsManager.MMS_ERROR_UNSPECIFIED;
                    break;
                }
                try {
                    LogUtil.d(requestId, "sleep: " + retryDelaySecs * 1000);
                    Thread.sleep(retryDelaySecs * 1000, 0/*nano*/);
                } catch (InterruptedException e) {}
                if (!(isOmhEnable && isOmhCard && mmsConfigInfo != null
                        && omhRetryInterval != 0)) {
                    LogUtil.d(TAG, "MmsRequest.execute_default:isOmhEnable="
                            +isOmhEnable
                            +" isOmhCard="+isOmhCard
                            +" (mmsConfigInfo != null)="+(mmsConfigInfo != null));
                    retryDelaySecs <<= 1;
                }
            }
            /// M: For Op09 Feature : Cancel download mms.
            mMmsServiceCancelDownloadPlugin.removeCachedLocationUri(this.hashCode());
        }
        if (result != Activity.RESULT_OK) {
            LogUtil.d(requestId, "pdp connect FAIL. result = " + result);
        }
        processResult(context, result, response, httpStatusCode);
        if (isRequestSuccessed && networkManager.isNetworkReleased()) {
            try {
                LogUtil.d(requestId, "success sleep 0.5s");
                Thread.sleep(500, 0/*nano*/);
            } catch (InterruptedException e) {}
        }
    }

    /**
     * Process the result of the completed request, including updating the message status
     * in database and sending back the result via pending intents.
     *  @param context The context
     * @param result The result code of execution
     * @param response The response body
     * @param httpStatusCode The optional http status code in case of http failure
     */
    public void processResult(Context context, int result, byte[] response, int httpStatusCode) {
        final Uri messageUri = persistIfRequired(context, result, response);
        LogUtil.d(this.toString(), "processResult: prepare");
        // Return MMS HTTP request result via PendingIntent
        final PendingIntent pendingIntent = getPendingIntent();
        if (pendingIntent != null) {
            boolean succeeded = true;
            // Extra information to send back with the pending intent
            Intent fillIn = new Intent();
            if (response != null) {
                succeeded = transferResponse(fillIn, response);
            }
            if (messageUri != null) {
                LogUtil.d(this.toString(), "processResult: uri = " + mMessageUri);
                fillIn.putExtra("uri", messageUri.toString());
            }
            putOriginalUrl(fillIn);
            if (result == SmsManager.MMS_ERROR_HTTP_FAILURE && httpStatusCode != 0) {
                fillIn.putExtra(SmsManager.EXTRA_MMS_HTTP_STATUS, httpStatusCode);
            }
            try {
                if (!succeeded) {
                    result = SmsManager.MMS_ERROR_IO_ERROR;
                }
                pendingIntent.send(context, result, fillIn);
            } catch (PendingIntent.CanceledException e) {
                LogUtil.e(this.toString(), "Sending pending intent canceled", e);
            }
        }

        revokeUriPermission(context);
    }

    /**
     * Returns true if sending / downloading using the carrier app has failed and completes the
     * action using platform API's, otherwise false.
     */
    protected boolean maybeFallbackToRegularDelivery(int carrierMessagingAppResult) {
        if (carrierMessagingAppResult
                == CarrierMessagingService.SEND_STATUS_RETRY_ON_CARRIER_NETWORK
                || carrierMessagingAppResult
                        == CarrierMessagingService.DOWNLOAD_STATUS_RETRY_ON_CARRIER_NETWORK) {
            LogUtil.d(this.toString(), "Sending/downloading MMS by IP failed.");
            mRequestManager.addSimRequest(MmsRequest.this);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Converts from {@code carrierMessagingAppResult} to a platform result code.
     */
    protected static int toSmsManagerResult(int carrierMessagingAppResult) {
        switch (carrierMessagingAppResult) {
            case CarrierMessagingService.SEND_STATUS_OK:
                return Activity.RESULT_OK;
            case CarrierMessagingService.SEND_STATUS_RETRY_ON_CARRIER_NETWORK:
                return SmsManager.MMS_ERROR_RETRY;
            default:
                return SmsManager.MMS_ERROR_UNSPECIFIED;
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '@' + Integer.toHexString(hashCode());
    }

    /**
     * Making the HTTP request to MMSC
     *
     * @param context The context
     * @param netMgr The current {@link MmsNetworkManager}
     * @param apn The APN setting
     * @return The HTTP response data
     * @throws MmsHttpException If any network error happens
     */
    protected abstract byte[] doHttp(Context context, MmsNetworkManager netMgr, ApnSettings apn)
            throws MmsHttpException;

    /**
     * @return The PendingIntent associate with the MMS sending invocation
     */
    protected abstract PendingIntent getPendingIntent();

    /**
     * @return The queue should be used by this request, 0 is sending and 1 is downloading
     */
    protected abstract int getQueueType();

    /**
     * Persist message into telephony if required (i.e. when auto-persisting is on or
     * the calling app is non-default sms app for sending)
     *
     * @param context The context
     * @param result The result code of execution
     * @param response The response body
     * @return The persisted URI of the message or null if we don't persist or fail
     */
    protected abstract Uri persistIfRequired(Context context, int result, byte[] response);

    /**
     * Prepare to make the HTTP request - will download message for sending
     * @return true if preparation succeeds (and request can proceed) else false
     */
    protected abstract boolean prepareForHttpRequest();

    /**
     * Transfer the received response to the caller
     *
     * @param fillIn the intent that will be returned to the caller
     * @param response the pdu to transfer
     * @return true if response transfer succeeds else false
     */
    protected abstract boolean transferResponse(Intent fillIn, byte[] response);

    /**
     * Revoke the content URI permission granted by the MMS app to the phone package.
     *
     * @param context The context
     */
    protected abstract void revokeUriPermission(Context context);

    /**
     * Base class for handling carrier app send / download result.
     */
    protected abstract class CarrierMmsActionCallback extends ICarrierMessagingCallback.Stub {
        @Override
        public void onSendSmsComplete(int result, int messageRef) {
            LogUtil.e("Unexpected onSendSmsComplete call with result: " + result);
        }

        @Override
        public void onSendMultipartSmsComplete(int result, int[] messageRefs) {
            LogUtil.e("Unexpected onSendMultipartSmsComplete call with result: " + result);
        }

        @Override
        public void onFilterComplete(boolean keepMessage) {
            LogUtil.e("Unexpected onFilterComplete call with result: " + keepMessage);
        }
    }

    /**
     * Check the response to find transaction successed or failed
     * @param response The response pdu bytes array
     * @return the result code of transaction
     */
    protected abstract int checkResponse(byte[] response);


    /**
     * Put the content url or location url in the intent.
     *
     * @param intent The intent which put the url.
     */
    protected abstract void putOriginalUrl(Intent intent);
}
