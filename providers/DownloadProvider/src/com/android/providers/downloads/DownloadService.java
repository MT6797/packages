/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.providers.downloads;

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static com.android.providers.downloads.Constants.TAG;

import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.provider.Downloads;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.IndentingPrintWriter;
import com.google.android.collect.Maps;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Performs background downloads as requested by applications that use
 * {@link DownloadManager}. Multiple start commands can be issued at this
 * service, and it will continue running until no downloads are being actively
 * processed. It may schedule alarms to resume downloads in future.
 * <p>
 * Any database updates important enough to initiate tasks should always be
 * delivered through {@link Context#startService(Intent)}.
 */
public class DownloadService extends Service {
    // TODO: migrate WakeLock from individual DownloadThreads out into
    // DownloadReceiver to protect our entire workflow.

    private static final boolean DEBUG_LIFECYCLE = false;

    @VisibleForTesting
    SystemFacade mSystemFacade;

    private AlarmManager mAlarmManager;

    /** Observer to get notified when the content observer's data changes */
    private DownloadManagerContentObserver mObserver;

    /** Class to handle Notification Manager updates */
    private DownloadNotifier mNotifier;

    /** Scheduling of the periodic cleanup job */
    private JobInfo mCleanupJob;

    private static final int CLEANUP_JOB_ID = 1;
    private static final long CLEANUP_JOB_PERIOD = 1000 * 60 * 60 * 24; // one day
    private static ComponentName sCleanupServiceName = new ComponentName(
            DownloadIdleService.class.getPackage().getName(),
            DownloadIdleService.class.getName());

    /**
     * The Service's view of the list of downloads, mapping download IDs to the corresponding info
     * object. This is kept independently from the content provider, and the Service only initiates
     * downloads based on this data, so that it can deal with situation where the data in the
     * content provider changes or disappears.
     */
    @GuardedBy("mDownloads")
    private final Map<Long, DownloadInfo> mDownloads = Maps.newHashMap();

    private final ExecutorService mExecutor = buildDownloadExecutor();

    private static ExecutorService buildDownloadExecutor() {
        final int maxConcurrent = Resources.getSystem().getInteger(
                com.android.internal.R.integer.config_MaxConcurrentDownloadsAllowed);

        // Create a bounded thread pool for executing downloads; it creates
        // threads as needed (up to maximum) and reclaims them when finished.
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(
                maxConcurrent, maxConcurrent, 10, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>()) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);

                if (t == null && r instanceof Future<?>) {
                    try {
                        ((Future<?>) r).get();
                    } catch (CancellationException ce) {
                        t = ce;
                    } catch (ExecutionException ee) {
                        t = ee.getCause();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }

                if (t != null) {
                    Log.w(TAG, "Uncaught exception", t);
                }
            }
        };
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    private DownloadScanner mScanner;

    private HandlerThread mUpdateThread;
    private Handler mUpdateHandler;

    private volatile int mLastStartId;

    /**
     * Receives notifications when the data in the content provider changes
     */
    private class DownloadManagerContentObserver extends ContentObserver {
        public DownloadManagerContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(final boolean selfChange) {
            enqueueUpdate();
        }
    }

    /**
     * Returns an IBinder instance when someone wants to connect to this
     * service. Binding to this service is not allowed.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public IBinder onBind(Intent i) {
        throw new UnsupportedOperationException("Cannot bind to Download Manager Service");
    }

    /**
     * Initializes the service when it is first created
     */
    @Override
    public void onCreate() {
        super.onCreate();
        if (Constants.LOGVV) {
            Log.v(Constants.TAG, "Service onCreate");
        }

        if (mSystemFacade == null) {
            mSystemFacade = new RealSystemFacade(this);
        }

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        mUpdateThread = new HandlerThread(TAG + "-UpdateThread");
        mUpdateThread.start();
        mUpdateHandler = new Handler(mUpdateThread.getLooper(), mUpdateCallback);

        mScanner = new DownloadScanner(this);

        mNotifier = new DownloadNotifier(this);
        mNotifier.cancelAll();

        mObserver = new DownloadManagerContentObserver();
        getContentResolver().registerContentObserver(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                true, mObserver);

        JobScheduler js = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (needToScheduleCleanup(js)) {
            final JobInfo job = new JobInfo.Builder(CLEANUP_JOB_ID, sCleanupServiceName)
                    .setPeriodic(CLEANUP_JOB_PERIOD)
                    .setRequiresCharging(true)
                    .setRequiresDeviceIdle(true)
                    .build();
            js.schedule(job);
        }
    }

    private boolean needToScheduleCleanup(JobScheduler js) {
        List<JobInfo> myJobs = js.getAllPendingJobs();
        if (myJobs != null) {
            final int N = myJobs.size();
            for (int i = 0; i < N; i++) {
                if (myJobs.get(i).getId() == CLEANUP_JOB_ID) {
                    // It's already been (persistently) scheduled; no need to do it again
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int returnValue = super.onStartCommand(intent, flags, startId);
        if (Constants.LOGVV) {
            Log.v(Constants.TAG, "Service onStart");
        }
        mLastStartId = startId;
        enqueueUpdate();
        return returnValue;
    }

    @Override
    public void onDestroy() {
        getContentResolver().unregisterContentObserver(mObserver);
        mScanner.shutdown();
        mUpdateThread.quit();
        if (Constants.LOGVV) {
            Log.v(Constants.TAG, "Service onDestroy");
        }
        super.onDestroy();
    }

    /**
     * Enqueue an {@link #updateLocked()} pass to occur in future.
     */
    public void enqueueUpdate() {
        if (mUpdateHandler != null) {
            mUpdateHandler.removeMessages(MSG_UPDATE);
            mUpdateHandler.obtainMessage(MSG_UPDATE, mLastStartId, -1).sendToTarget();
        }
    }

    /**
     * Enqueue an {@link #updateLocked()} pass to occur after delay, usually to
     * catch any finished operations that didn't trigger an update pass.
     */
    private void enqueueFinalUpdate() {
        mUpdateHandler.removeMessages(MSG_FINAL_UPDATE);
        mUpdateHandler.sendMessageDelayed(
                mUpdateHandler.obtainMessage(MSG_FINAL_UPDATE, mLastStartId, -1),
                5 * MINUTE_IN_MILLIS);
    }

    private static final int MSG_UPDATE = 1;
    private static final int MSG_FINAL_UPDATE = 2;

    private Handler.Callback mUpdateCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            final int startId = msg.arg1;
            if (DEBUG_LIFECYCLE) Log.v(TAG, "Updating for startId " + startId);

            // Since database is current source of truth, our "active" status
            // depends on database state. We always get one final update pass
            // once the real actions have finished and persisted their state.

            // TODO: switch to asking real tasks to derive active state
            // TODO: handle media scanner timeouts

            final boolean isActive;
            synchronized (mDownloads) {
                isActive = updateLocked();
            }

            if (msg.what == MSG_FINAL_UPDATE) {
                // Dump thread stacks belonging to pool
                for (Map.Entry<Thread, StackTraceElement[]> entry :
                        Thread.getAllStackTraces().entrySet()) {
                    if (entry.getKey().getName().startsWith("pool")) {
                        Log.d(TAG, entry.getKey() + ": " + Arrays.toString(entry.getValue()));
                    }
                }

                // Dump speed and update details
                mNotifier.dumpSpeeds();

                Log.d(TAG, "Final update pass triggered, isActive=" + isActive
                        + "; someone didn't update correctly.");
            }

            if (isActive) {
                // Still doing useful work, keep service alive. These active
                // tasks will trigger another update pass when they're finished.

                // Enqueue delayed update pass to catch finished operations that
                // didn't trigger an update pass; these are bugs.
                enqueueFinalUpdate();

            } else {
                // No active tasks, and any pending update messages can be
                // ignored, since any updates important enough to initiate tasks
                // will always be delivered with a new startId.

                if (stopSelfResult(startId)) {
                    if (DEBUG_LIFECYCLE) Log.v(TAG, "Nothing left; stopped");
                    getContentResolver().unregisterContentObserver(mObserver);
                    mScanner.shutdown();
                    mUpdateThread.quit();
                }
            }

            return true;
        }
    };

    /**
     * Update {@link #mDownloads} to match {@link DownloadProvider} state.
     * Depending on current download state it may enqueue {@link DownloadThread}
     * instances, request {@link DownloadScanner} scans, update user-visible
     * notifications, and/or schedule future actions with {@link AlarmManager}.
     * <p>
     * Should only be called from {@link #mUpdateThread} as after being
     * requested through {@link #enqueueUpdate()}.
     *
     * @return If there are active tasks being processed, as of the database
     *         snapshot taken in this update.
     */
    private boolean updateLocked() {
        final long now = mSystemFacade.currentTimeMillis();

        boolean isActive = false;
        long nextActionMillis = Long.MAX_VALUE;

        final Set<Long> staleIds = Sets.newHashSet(mDownloads.keySet());

        final ContentResolver resolver = getContentResolver();
        final Cursor cursor = resolver.query(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
                null, null, null, null);
        try {
            final DownloadInfo.Reader reader = new DownloadInfo.Reader(resolver, cursor);
            final int idColumn = cursor.getColumnIndexOrThrow(Downloads.Impl._ID);
            while (cursor.moveToNext()) {
                final long id = cursor.getLong(idColumn);
                staleIds.remove(id);

                DownloadInfo info = mDownloads.get(id);
                if (info != null) {
                    updateDownload(reader, info, now);
                } else {
                    info = insertDownloadLocked(reader, now);
                }

                if (info.mDeleted) {
                    // Delete download if requested, but only after cleaning up
                    if (!TextUtils.isEmpty(info.mMediaProviderUri)) {
                        resolver.delete(Uri.parse(info.mMediaProviderUri), null, null);
                    }

                    deleteFileIfExists(info.mFileName);
                    resolver.delete(info.getAllDownloadsUri(), null, null);

                } else {
                    // Kick off download task if ready
                    final boolean activeDownload = info.startDownloadIfReady(mExecutor);

                    // Kick off media scan if completed
                    final boolean activeScan = info.startScanIfReady(mScanner);

                    if (DEBUG_LIFECYCLE && (activeDownload || activeScan)) {
                        Log.v(TAG, "Download " + info.mId + ": activeDownload=" + activeDownload
                                + ", activeScan=" + activeScan);
                    }

                    isActive |= activeDownload;
                    isActive |= activeScan;
                }

                // Keep track of nearest next action
                nextActionMillis = Math.min(info.nextActionMillis(now), nextActionMillis);
            }
        } finally {
            cursor.close();
        }

        // Clean up stale downloads that disappeared
        for (Long id : staleIds) {
            deleteDownloadLocked(id);
        }

        // Update notifications visible to user
        mNotifier.updateWith(mDownloads.values());

        // Set alarm when next action is in future. It's okay if the service
        // continues to run in meantime, since it will kick off an update pass.
        if (nextActionMillis > 0 && nextActionMillis < Long.MAX_VALUE) {
            if (Constants.LOGV) {
                Log.v(TAG, "scheduling start in " + nextActionMillis + "ms");
            }

            final Intent intent = new Intent(Constants.ACTION_RETRY);
            intent.setClass(this, DownloadReceiver.class);
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, now + nextActionMillis,
                    PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_ONE_SHOT));
        }

        return isActive;
    }

    /**
     * Keeps a local copy of the info about a download, and initiates the
     * download if appropriate.
     */
    private DownloadInfo insertDownloadLocked(DownloadInfo.Reader reader, long now) {
        final DownloadInfo info = reader.newDownloadInfo(this, mSystemFacade, mNotifier);
        mDownloads.put(info.mId, info);

        if (Constants.LOGVV) {
            Log.v(Constants.TAG, "processing inserted download " + info.mId);
        }

        return info;
    }

    /**
     * Updates the local copy of the info about a download.
     */
    private void updateDownload(DownloadInfo.Reader reader, DownloadInfo info, long now) {
        reader.updateFromDatabase(info);
        if (Constants.LOGVV) {
            Log.v(Constants.TAG, "processing updated download " + info.mId +
                    ", status: " + info.mStatus);
        }
    }

    /**
     * Removes the local copy of the info about a download.
     */
    private void deleteDownloadLocked(long id) {
        DownloadInfo info = mDownloads.get(id);
        if (info.mStatus == Downloads.Impl.STATUS_RUNNING) {
            info.mStatus = Downloads.Impl.STATUS_CANCELED;
        }
        if (info.mDestination != Downloads.Impl.DESTINATION_EXTERNAL && info.mFileName != null) {
            if (true || Constants.LOGVV) {
                Log.d(TAG, "deleteDownloadLocked() deleting " + info.mFileName);
            }
            deleteFileIfExists(info.mFileName);
        }

        /// M: add to fix 452723. @{
        Intent contentIntent = (Intent) mNotifier.getPendingIntentsMap().get(info.mId);
        if (contentIntent != null) {
            if (PendingIntent.getBroadcast(mNotifier.getNotificationContext(), 0,
                    contentIntent, PendingIntent.FLAG_NO_CREATE) != null) {
                PendingIntent.getBroadcast(mNotifier.getNotificationContext(), 0,
                        contentIntent, PendingIntent.FLAG_UPDATE_CURRENT).cancel();
            }

            Intent deleteIntent = new Intent(Constants.ACTION_HIDE);
            deleteIntent.setClassName("com.android.providers.downloads",
                    DownloadReceiver.class.getName());
            deleteIntent.setData(contentIntent.getData());

            if (PendingIntent.getBroadcast(mNotifier.getNotificationContext(), 0,
                    deleteIntent, PendingIntent.FLAG_NO_CREATE) != null) {
                PendingIntent.getBroadcast(mNotifier.getNotificationContext(), 0,
                        deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT).cancel();
            }

            mNotifier.getPendingIntentsMap().remove(info.mId);
        }
        /// @}
        /// M: add to fix 1918727. @{
        mNotifier.getUpdateDoneItems().remove(info.mId);
        /// @}
        mDownloads.remove(info.mId);

        /// M: Add this to support OMA DL
        /// When user click "Cancel" on dd file AlertDialog, and dd file contain install url
        /// The download item is deleted and will notify to web server
        /// If user cancel download the media object, will notify to web server. @{
        if (!Downloads.Impl.OMA_DOWNLOAD_SUPPORT) {
            return;
        }
        Log.i(Constants.LOG_OMA_DL,
                "DownloadService: deleteDownload before notifyUrl: info.mStatus is :"
                        + info.mStatus + "info.mFileName is: " + info.mFileName
                        + " info.mOmaDownload is:" + info.mOmaDownload
                        + " info.mOmaDownloadInsNotifyUrl is:" + info.mOmaDownloadInsNotifyUrl
                        + " info.mOmaDownloadStatus is: " + info.mOmaDownloadStatus);

        if ((((info.mStatus == Downloads.Impl.STATUS_CANCELED || info.mStatus == Downloads.Impl.STATUS_PAUSED_BY_APP)
                && (info.mOmaDownload == 1 && info.mOmaDownloadInsNotifyUrl != null)
                && (info.mOmaDownloadStatus < Downloads.Impl.OMADL_STATUS_DOWNLOAD_COMPLETELY
                || info.mOmaDownloadStatus == Downloads.Impl.OMADL_STATUS_PARSE_DDFILE_SUCCESS)))
                || (info.mOmaDownloadInsNotifyUrl != null
                && (info.mOmaDownloadStatus == Downloads.Impl.OMADL_STATUS_ERROR_USER_CANCELLED
                || info.mOmaDownloadStatus == Downloads.Impl.OMADL_STATUS_ERROR_NON_ACCEPTABLE_CONTENT))) {
            final int omaDownloadStatus = info.mOmaDownloadStatus;
            try {
                final URL notifyUrl = new URL(info.mOmaDownloadInsNotifyUrl);
                Thread nofiyUrlThread = new Thread() {
                    public void run() {
                        OmaDescription omaDescription = new OmaDescription();
                        omaDescription.setInstallNotifyUrl(notifyUrl);
                        // installNotify need return value and Handler may not used
                        if (omaDownloadStatus == Downloads.Impl.OMADL_STATUS_ERROR_NON_ACCEPTABLE_CONTENT) {
                            omaDescription.setStatusCode(OmaStatusHandler.NON_ACCEPTABLE_CONTENT);
                            Log.i(Constants.LOG_OMA_DL, "DownloadService: deleteDownload(): " +
                                    "nofiyUrlThread: before install notify:NON_ACCEPTABLE_CONTENT");
                        } else {
                            omaDescription.setStatusCode(OmaStatusHandler.USER_CANCELLED);
                            Log.i(Constants.LOG_OMA_DL, "DownloadService: deleteDownload(): " +
                                    "nofiyUrlThread: before install notify:USER_CANCELLED");
                        }
                        // handler set to null will occur exception
                        OmaDownload.installNotify(omaDescription, null);
                    }
                };
                nofiyUrlThread.start();
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        /// @}
    }

    private void deleteFileIfExists(String path) {
        if (!TextUtils.isEmpty(path)) {
            if (true || Constants.LOGVV) {
                Log.d(TAG, "deleteFileIfExists() deleting " + path);
            }
            final File file = new File(path);
            if (file.exists() && !file.delete()) {
                Log.w(TAG, "file: '" + path + "' couldn't be deleted");
            }
        }
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        final IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        synchronized (mDownloads) {
            final List<Long> ids = Lists.newArrayList(mDownloads.keySet());
            Collections.sort(ids);
            for (Long id : ids) {
                final DownloadInfo info = mDownloads.get(id);
                info.dump(pw);
            }
        }
    }
}
