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
package com.android.deskclock.alarms;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.android.deskclock.AlarmClockFragment;
import com.android.deskclock.AlarmUtils;
import com.android.deskclock.DeskClock;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;

public final class AlarmNotifications {
    public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";

    public static void registerNextAlarmWithAlarmManager(Context context, AlarmInstance instance)  {
        // Sets a surrogate alarm with alarm manager that provides the AlarmClockInfo for the
        // alarm that is going to fire next. The operation is constructed such that it is ignored
        // by AlarmStateManager.

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        int flags = instance == null ? PendingIntent.FLAG_NO_CREATE : 0;
        PendingIntent operation = PendingIntent.getBroadcast(context, 0 /* requestCode */,
                AlarmStateManager.createIndicatorIntent(context), flags);

        if (instance != null) {
            long alarmTime = instance.getAlarmTime().getTimeInMillis();

            // Create an intent that can be used to show or edit details of the next alarm.
            PendingIntent viewIntent = PendingIntent.getActivity(context, instance.hashCode(),
                    createViewAlarmIntent(context, instance), PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager.AlarmClockInfo info =
                    new AlarmManager.AlarmClockInfo(alarmTime, viewIntent);
            alarmManager.setAlarmClock(info, operation);
        } else if (operation != null) {
            alarmManager.cancel(operation);
        }
    }

    public static void showLowPriorityNotification(Context context, AlarmInstance instance) {
        LogUtils.v("Displaying low priority notification for alarm instance: " + instance.mId);
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context)
                .setContentTitle(context.getString(
                        R.string.alarm_alert_predismiss_title))
                .setContentText(AlarmUtils.getAlarmText(context, instance))
                .setSmallIcon(R.drawable.stat_notify_alarm)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true);

        // Setup up hide notification
        Intent hideIntent = AlarmStateManager.createStateChangeIntent(context,
                AlarmStateManager.ALARM_DELETE_TAG, instance,
                AlarmInstance.HIDE_NOTIFICATION_STATE);
        notification.setDeleteIntent(PendingIntent.getBroadcast(context, instance.hashCode(),
                hideIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        // Setup up dismiss action
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context,
                AlarmStateManager.ALARM_DISMISS_TAG, instance, AlarmInstance.PREDISMISSED_STATE);
        notification.addAction(R.drawable.ic_alarm_off_24dp,
                context.getString(R.string.alarm_alert_dismiss_now_text),
                PendingIntent.getBroadcast(context, instance.hashCode(),
                        dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        // Setup content action if instance is owned by alarm
        Intent viewAlarmIntent = createViewAlarmIntent(context, instance);
        notification.setContentIntent(PendingIntent.getActivity(context, instance.hashCode(),
                viewAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        nm.cancel(instance.hashCode());
        nm.notify(instance.hashCode(), notification.build());
    }

    public static void showHighPriorityNotification(Context context, AlarmInstance instance) {
        LogUtils.v("Displaying high priority notification for alarm instance: " + instance.mId);
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context)
                .setContentTitle(context.getString(R.string.alarm_alert_predismiss_title))
                .setContentText(AlarmUtils.getAlarmText(context, instance))
                .setSmallIcon(R.drawable.stat_notify_alarm)
                .setAutoCancel(false)
                .setOngoing(true)
                .setGroup(Integer.toString(instance.hashCode()))
                .setGroupSummary(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true);

        // Setup up dismiss action
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context,
                AlarmStateManager.ALARM_DISMISS_TAG, instance, AlarmInstance.PREDISMISSED_STATE);
        notification.addAction(R.drawable.ic_alarm_off_24dp,
                context.getString(R.string.alarm_alert_dismiss_now_text),
                PendingIntent.getBroadcast(context, instance.hashCode(),
                        dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        // Setup content action if instance is owned by alarm
        Intent viewAlarmIntent = createViewAlarmIntent(context, instance);
        notification.setContentIntent(PendingIntent.getActivity(context, instance.hashCode(),
                viewAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        nm.cancel(instance.hashCode());
        nm.notify(instance.hashCode(), notification.build());
    }

    public static void showSnoozeNotification(Context context, AlarmInstance instance) {
        LogUtils.v("Displaying snoozed notification for alarm instance: " + instance.mId);
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context)
                .setContentTitle(instance.getLabelOrDefault(context))
                .setContentText(context.getString(R.string.alarm_alert_snooze_until,
                        AlarmUtils.getFormattedTime(context, instance.getAlarmTime())))
                .setSmallIcon(R.drawable.stat_notify_alarm)
                .setAutoCancel(false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true);

        // Setup up dismiss action
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context,
                AlarmStateManager.ALARM_DISMISS_TAG, instance, AlarmInstance.DISMISSED_STATE);
        notification.addAction(R.drawable.ic_alarm_off_24dp,
                context.getString(R.string.alarm_alert_dismiss_text),
                PendingIntent.getBroadcast(context, instance.hashCode(),
                        dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        // Setup content action if instance is owned by alarm
        Intent viewAlarmIntent = createViewAlarmIntent(context, instance);
        notification.setContentIntent(PendingIntent.getActivity(context, instance.hashCode(),
                viewAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        nm.cancel(instance.hashCode());
        nm.notify(instance.hashCode(), notification.build());
    }

    public static void showMissedNotification(Context context, AlarmInstance instance) {
        LogUtils.v("Displaying missed notification for alarm instance: " + instance.mId);
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);

        String label = instance.mLabel;
        String alarmTime = AlarmUtils.getFormattedTime(context, instance.getAlarmTime());
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context)
                .setContentTitle(context.getString(R.string.alarm_missed_title))
                .setContentText(instance.mLabel.isEmpty() ? alarmTime :
                        context.getString(R.string.alarm_missed_text, alarmTime, label))
                .setSmallIcon(R.drawable.stat_notify_alarm)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true);

        final int hashCode = instance.hashCode();

        // Setup dismiss intent
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context,
                AlarmStateManager.ALARM_DISMISS_TAG, instance, AlarmInstance.DISMISSED_STATE);
        notification.setDeleteIntent(PendingIntent.getBroadcast(context, hashCode,
                dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        // Setup content intent
        Intent showAndDismiss = AlarmInstance.createIntent(context, AlarmStateManager.class,
                instance.mId);
        showAndDismiss.putExtra(EXTRA_NOTIFICATION_ID, hashCode);
        showAndDismiss.setAction(AlarmStateManager.SHOW_AND_DISMISS_ALARM_ACTION);
        notification.setContentIntent(PendingIntent.getBroadcast(context, hashCode,
                showAndDismiss, PendingIntent.FLAG_UPDATE_CURRENT));

        nm.cancel(hashCode);
        nm.notify(hashCode, notification.build());
    }

    public static void showAlarmNotification(Context context, AlarmInstance instance) {
        LogUtils.v("Displaying alarm notification for alarm instance: " + instance.mId);
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);

        Resources resources = context.getResources();
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context)
                .setContentTitle(instance.getLabelOrDefault(context))
                .setContentText(AlarmUtils.getFormattedTime(context, instance.getAlarmTime()))
                .setSmallIcon(R.drawable.stat_notify_alarm)
                .setOngoing(true)
                .setAutoCancel(false)
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                .setWhen(0)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true);

        // Setup Snooze Action
        Intent snoozeIntent = AlarmStateManager.createStateChangeIntent(context,
                AlarmStateManager.ALARM_SNOOZE_TAG, instance, AlarmInstance.SNOOZE_STATE);
        snoozeIntent.putExtra(AlarmStateManager.FROM_NOTIFICATION_EXTRA, true);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, instance.hashCode(),
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notification.addAction(R.drawable.ic_snooze_24dp,
                resources.getString(R.string.alarm_alert_snooze_text), snoozePendingIntent);

        // Setup Dismiss Action
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context,
                AlarmStateManager.ALARM_DISMISS_TAG, instance, AlarmInstance.DISMISSED_STATE);
        dismissIntent.putExtra(AlarmStateManager.FROM_NOTIFICATION_EXTRA, true);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context,
                instance.hashCode(), dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.addAction(R.drawable.ic_alarm_off_24dp,
                resources.getString(R.string.alarm_alert_dismiss_text),
                dismissPendingIntent);

        // Setup Content Action
        /** M: Using AlarmStateManager to launch AlarmActivity but not to launch it directly,
         * so that we can update the notification before the AlarmActivity is launched
         * @{ */
        Intent contentIntent = AlarmInstance.createIntent(context, AlarmStateManager.class,
                instance.mId);
        contentIntent.setAction("launch_activity");
        notification.setContentIntent(PendingIntent.getBroadcast(context,
                instance.hashCode(), contentIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        /** @} */

        // Setup fullscreen intent
        Intent fullScreenIntent = AlarmInstance.createIntent(context, AlarmActivity.class,
                instance.mId);
        // set action, so we can be different then content pending intent
        fullScreenIntent.setAction("fullscreen_activity");
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        notification.setFullScreenIntent(PendingIntent.getActivity(context,
                instance.hashCode(), fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT), true);
        notification.setPriority(NotificationCompat.PRIORITY_MAX);

        nm.cancel(instance.hashCode());
        nm.notify(instance.hashCode(), notification.build());
    }

    public static void clearNotification(Context context, AlarmInstance instance) {
        LogUtils.v("Clearing notifications for alarm instance: " + instance.mId);
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        nm.cancel(instance.hashCode());
    }

    public static Intent createViewAlarmIntent(Context context, AlarmInstance instance) {
        long alarmId = instance.mAlarmId == null ? Alarm.INVALID_ID : instance.mAlarmId;
        Intent viewAlarmIntent = Alarm.createIntent(context, DeskClock.class, alarmId);
        viewAlarmIntent.putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.ALARM_TAB_INDEX);
        viewAlarmIntent.putExtra(AlarmClockFragment.SCROLL_TO_ALARM_INTENT_EXTRA, alarmId);
        viewAlarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return viewAlarmIntent;
    }

    /// M: Update the alarm's notification when alarm be set fired state directly @{
    public static void updateAlarmNotification(Context context, AlarmInstance instance) {
        LogUtils.v("Updating alarm notification for alarm instance: " + instance.mId);
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        Resources resources = context.getResources();
        Notification.Builder notification = new Notification.Builder(context)
                .setContentTitle(instance.getLabelOrDefault(context))
                .setContentText(AlarmUtils.getFormattedTime(context, instance.getAlarmTime()))
                .setSmallIcon(R.drawable.stat_notify_alarm)
                .setOngoing(true)
                .setAutoCancel(false)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setWhen(0);

        // Setup Snooze Action
        Intent snoozeIntent = AlarmStateManager.createStateChangeIntent(context, "SNOOZE_TAG",
                instance, AlarmInstance.SNOOZE_STATE);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, instance.hashCode(),
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notification.addAction(R.drawable.ic_snooze_24dp,
                resources.getString(R.string.alarm_alert_snooze_text), snoozePendingIntent);

        // Setup Dismiss Action
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context, "DISMISS_TAG",
                instance, AlarmInstance.DISMISSED_STATE);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context,
                instance.hashCode(), dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.addAction(R.drawable.ic_alarm_off_24dp,
                resources.getString(R.string.alarm_alert_dismiss_text),
                dismissPendingIntent);

        // Setup Content Action
        Intent contentIntent = AlarmInstance.createIntent(context, AlarmActivity.class,
                instance.mId);
        notification.setContentIntent(PendingIntent.getActivity(context,
                instance.hashCode(), contentIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        notification.setPriority(Notification.PRIORITY_MAX);

        nm.cancel(instance.hashCode());
        nm.notify(instance.hashCode(), notification.build());
    }
    /// @}
}
