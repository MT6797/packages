/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.cellbroadcastreceiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.telephony.CellBroadcastMessage;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.media.AudioManager;
import static com.android.cellbroadcastreceiver.CellBroadcastReceiver.DBG;
import com.android.internal.telephony.PhoneConstants;

/**
 * Manages alert reminder notification.
 */
public class CellBroadcastAlertReminder extends Service {
    private static final String TAG = "[ETWS]CellBroadcastAlertReminder";

    /** Action to wake up and play alert reminder sound. */
    static final String ACTION_PLAY_ALERT_REMINDER = "ACTION_PLAY_ALERT_REMINDER";

    /**
     * Pending intent for alert reminder. This is static so that we don't have to start the
     * service in order to cancel any pending reminders when user dismisses the alert dialog.
     */
    private static PendingIntent sPlayReminderIntent;

    /**
     * Alert reminder for current ringtone being played.
     */
    private static Ringtone sPlayReminderRingtone;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // No intent or unrecognized action; tell the system not to restart us.
        if (intent == null || !ACTION_PLAY_ALERT_REMINDER.equals(intent.getAction())) {
            stopSelf();
            log("onStartCommand stopSelf ");
            return START_NOT_STICKY;
        }

        int subId = intent.getExtras().getInt(PhoneConstants.SUBSCRIPTION_KEY);
        if(DBG) Log.d(TAG, "subscription id = " + subId);

        log("playing alert reminder");
        playAlertReminderSound();

        if (queueAlertReminder(this, false, subId)) {
            return START_STICKY;
        } else {
            log("no reminders queued");
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    /**
     * Use the RingtoneManager to play the alert reminder sound.
     */
    private void playAlertReminderSound() {
        log("playAlertReminderSound");
        Uri notificationUri = RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_NOTIFICATION);
        if (notificationUri == null) {
            loge("Can't get URI for alert reminder sound");
            return;
        }
        Ringtone r = RingtoneManager.getRingtone(this, notificationUri);
        if (r != null) {
            log("playing alert reminder sound");
            r.setStreamType(AudioManager.STREAM_NOTIFICATION);
            r.play();
        } else {
            loge("can't get Ringtone for alert reminder sound");
        }
    }

    /**
     * Helper method to start the alert reminder service to queue the alert reminder.
     * @return true if a pending reminder was set; false if there are no more reminders
     */
    static boolean queueAlertReminder(Context context, boolean firstTime, int subId) {
        // Stop any alert reminder sound and cancel any previously queued reminders.
        cancelAlertReminder();

        int interval = SubscriptionManager.getIntegerSubscriptionProperty(subId,
                SubscriptionManager.CB_ALERT_REMINDER_INTERVAL, Integer.parseInt(
                        CellBroadcastSettings.ALERT_REMINDER_INTERVAL), context);

        if (interval == 0 || (interval == 1 && !firstTime)) {
            log("queueAlertReminder interval invalid");
            return false;
        }
        if (interval == 1) {
            interval = 2;   // "1" = one reminder after 2 minutes
        }

        log("queueAlertReminder() in " + interval + " minutes" + " with sub " + subId);

        Intent playIntent = new Intent(context, CellBroadcastAlertReminder.class);
        playIntent.setAction(ACTION_PLAY_ALERT_REMINDER);
        playIntent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, subId);
        sPlayReminderIntent = PendingIntent.getService(context, 0, playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            loge("can't get Alarm Service");
            return false;
        }

        // remind user after 2 minutes or 15 minutes
        long triggerTime = SystemClock.elapsedRealtime() + (interval * 60000);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, sPlayReminderIntent);
        return true;
    }

    /**
     * Stops alert reminder and cancels any queued reminders.
     */
    static void cancelAlertReminder() {
        if (DBG) log("cancelAlertReminder()");
        if (sPlayReminderRingtone != null) {
            if (DBG) log("stopping play reminder ringtone");
            sPlayReminderRingtone.stop();
            sPlayReminderRingtone = null;
        }
        if (sPlayReminderIntent != null) {
            if (DBG) log("canceling pending play reminder intent");
            sPlayReminderIntent.cancel();
            sPlayReminderIntent = null;
        }
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    private static void loge(String msg) {
        Log.e(TAG, msg);
    }
}
