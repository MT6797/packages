/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.alarmclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.android.deskclock.DeskClock;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.worldclock.Cities;
import com.android.deskclock.worldclock.CitiesActivity;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class DigitalAppWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "DigitalAppWidgetProvider";

    /**
     * Intent to be used for checking if a world clock's date has changed. Must be every fifteen
     * minutes because not all time zones are hour-locked.
     **/
    public static final String ACTION_ON_QUARTER_HOUR = "com.android.deskclock.ON_QUARTER_HOUR";

    // Lazily creating this intent to use with the AlarmManager
    private PendingIntent mPendingIntent;
    // Lazily creating this name to use with the AppWidgetManager
    private ComponentName mComponentName;

    public DigitalAppWidgetProvider() {
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        startAlarmOnQuarterHour(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        cancelAlarmOnQuarterHour(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (DigitalAppWidgetService.LOGGING) {
            Log.i(TAG, "onReceive: " + action);
        }
        super.onReceive(context, intent);

        if (ACTION_ON_QUARTER_HOUR.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            if (appWidgetManager != null) {
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(getComponentName(context));
                for (int appWidgetId : appWidgetIds) {
                    appWidgetManager.
                            notifyAppWidgetViewDataChanged(appWidgetId,
                                    R.id.digital_appwidget_listview);
                    RemoteViews widget = new RemoteViews(context.getPackageName(),
                            R.layout.digital_appwidget);
                    float ratio = WidgetUtils.getScaleRatio(context, null, appWidgetId);
                    ///M: change Android default design and show the AM/PM string to widget. @{
                    int amPmFontSize = (int) context.getResources().getDimension(R.dimen.main_ampm_font_size);
                    ///@}
                    WidgetUtils.setTimeFormat(context, widget, amPmFontSize, R.id.the_clock);
                    WidgetUtils.setClockSize(context, widget, ratio);
                    refreshAlarm(context, widget);
                    appWidgetManager.partiallyUpdateAppWidget(appWidgetId, widget);
                }
            }
            if(!ACTION_ON_QUARTER_HOUR.equals(action)) {
                cancelAlarmOnQuarterHour(context);
            }
            startAlarmOnQuarterHour(context);
        } else if (isNextAlarmChangedAction(action) || Intent.ACTION_SCREEN_ON.equals(action)) {
            // Refresh the next alarm
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            if (appWidgetManager != null) {
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(getComponentName(context));
                for (int appWidgetId : appWidgetIds) {
                    RemoteViews widget = new RemoteViews(context.getPackageName(),
                            R.layout.digital_appwidget);
                    refreshAlarm(context, widget);
                    appWidgetManager.partiallyUpdateAppWidget(appWidgetId, widget);
                }
            }
        } else if (Cities.WORLDCLOCK_UPDATE_INTENT.equals(action)) {
            // Refresh the world cities list
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            if (appWidgetManager != null) {
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(getComponentName(context));
                for (int appWidgetId : appWidgetIds) {
                    appWidgetManager.
                            notifyAppWidgetViewDataChanged(appWidgetId,
                                    R.id.digital_appwidget_listview);
                }
            }
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if (DigitalAppWidgetService.LOGGING) {
            Log.i(TAG, "onUpdate");
        }
        for (int appWidgetId : appWidgetIds) {
            float ratio = WidgetUtils.getScaleRatio(context, null, appWidgetId);
            updateClock(context, appWidgetManager, appWidgetId, ratio);
        }
        startAlarmOnQuarterHour(context);
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions) {
        // scale the fonts of the clock to fit inside the new size
        float ratio = WidgetUtils.getScaleRatio(context, newOptions, appWidgetId);
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        updateClock(context, widgetManager, appWidgetId, ratio);
    }

    /**
     * Determine whether action received corresponds to a "next alarm" changed action depending
     * on the SDK version.
     */
    private boolean isNextAlarmChangedAction(String action) {
        final String nextAlarmIntentAction;
        if (Utils.isLOrLater()) {
            nextAlarmIntentAction = AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED;
        } else {
            nextAlarmIntentAction = AlarmStateManager.SYSTEM_ALARM_CHANGE_ACTION;
        }
        return nextAlarmIntentAction.equals(action);
    }

    private void updateClock(
            Context context, AppWidgetManager appWidgetManager, int appWidgetId, float ratio) {
        RemoteViews widget = new RemoteViews(context.getPackageName(), R.layout.digital_appwidget);

        // Launch clock when clicking on the time in the widget only if not a lock screen widget
        Bundle newOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
        if (newOptions != null &&
                newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1)
                != AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD) {
            widget.setOnClickPendingIntent(R.id.digital_appwidget,
                    PendingIntent.getActivity(context, 0, new Intent(context, DeskClock.class), 0));
        }

        // Setup alarm text clock's format and font sizes
        refreshAlarm(context, widget);
        ///M: change Android default design and show the AM/PM string to widget. @{
        int amPmFontSize = (int) context.getResources().getDimension(R.dimen.main_ampm_font_size);
        ///@}
        WidgetUtils.setTimeFormat(context, widget, amPmFontSize/*0 no am/pm*/, R.id.the_clock);
        WidgetUtils.setClockSize(context, widget, ratio);

        // Set today's date format
        final CharSequence dateFormat = Utils.isJBMR2OrLater()
                ? DateFormat.getBestDateTimePattern(Locale.getDefault(),
                        context.getString(R.string.abbrev_wday_month_day_no_year))
                : ((SimpleDateFormat) SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT))
                        .toPattern();
        widget.setCharSequence(R.id.date, "setFormat12Hour", dateFormat);
        widget.setCharSequence(R.id.date, "setFormat24Hour", dateFormat);

        // Set up R.id.digital_appwidget_listview to use a remote views adapter
        // That remote views adapter connects to a RemoteViewsService through intent.
        final Intent intent = new Intent(context, DigitalAppWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        widget.setRemoteAdapter(R.id.digital_appwidget_listview, intent);

        // Set up the click on any world clock to start the Cities Activity
        //TODO: Should this be in the options guard above?
        widget.setPendingIntentTemplate(R.id.digital_appwidget_listview,
                PendingIntent.
                        getActivity(context, 0, new Intent(context, CitiesActivity.class), 0));

        // Refresh the widget
        appWidgetManager.notifyAppWidgetViewDataChanged(
                appWidgetId, R.id.digital_appwidget_listview);
        appWidgetManager.updateAppWidget(appWidgetId, widget);
    }

    protected void refreshAlarm(Context context, RemoteViews widget) {
        final String nextAlarm = Utils.getNextAlarm(context);
        if (!TextUtils.isEmpty(nextAlarm)) {
            widget.setTextViewText(R.id.nextAlarm,
                    context.getString(R.string.control_set_alarm_with_existing, nextAlarm));
            widget.setViewVisibility(R.id.nextAlarm, View.VISIBLE);
            if (DigitalAppWidgetService.LOGGING) {
                Log.v(TAG, "DigitalWidget sets next alarm string to " + nextAlarm);
            }
        } else  {
            widget.setViewVisibility(R.id.nextAlarm, View.GONE);
            if (DigitalAppWidgetService.LOGGING) {
                Log.v(TAG, "DigitalWidget sets next alarm string to null");
            }
        }
    }

    /**
     * Start an alarm that fires on the next quarter hour to update the world clock city
     * day when the local time or the world city crosses midnight.
     *
     * @param context The context in which the PendingIntent should perform the broadcast.
     */
    private void startAlarmOnQuarterHour(Context context) {
        if (context != null) {
            long onQuarterHour = Utils.getAlarmOnQuarterHour();
            PendingIntent quarterlyIntent = getOnQuarterHourPendingIntent(context);
            AlarmManager alarmManager = ((AlarmManager) context
                    .getSystemService(Context.ALARM_SERVICE));
            if (Utils.isKitKatOrLater()) {
                alarmManager.setExact(AlarmManager.RTC, onQuarterHour, quarterlyIntent);
            } else {
                alarmManager.set(AlarmManager.RTC, onQuarterHour, quarterlyIntent);
            }
            if (DigitalAppWidgetService.LOGGING) {
                Log.v(TAG, "startAlarmOnQuarterHour " + context.toString());
            }
        }
    }


    /**
     * Remove the alarm for the quarter hour update.
     *
     * @param context The context in which the PendingIntent was started to perform the broadcast.
     */
    public void cancelAlarmOnQuarterHour(Context context) {
        if (context != null) {
            PendingIntent quarterlyIntent = getOnQuarterHourPendingIntent(context);
            if (DigitalAppWidgetService.LOGGING) {
                Log.v(TAG, "cancelAlarmOnQuarterHour " + context.toString());
            }
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(
                    quarterlyIntent);
        }
    }

    /**
     * Create the pending intent that is broadcast on the quarter hour.
     *
     * @param context The Context in which this PendingIntent should perform the broadcast.
     * @return a pending intent with an intent unique to DigitalAppWidgetProvider
     */
    private PendingIntent getOnQuarterHourPendingIntent(Context context) {
        if (mPendingIntent == null) {
            mPendingIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(ACTION_ON_QUARTER_HOUR), PendingIntent.FLAG_CANCEL_CURRENT);
        }
        return mPendingIntent;
    }

    /**
     * Create the component name for this class
     *
     * @param context The Context in which the widgets for this component are created
     * @return the ComponentName unique to DigitalAppWidgetProvider
     */
    private ComponentName getComponentName(Context context) {
        if (mComponentName == null) {
            mComponentName = new ComponentName(context, getClass());
        }
        return mComponentName;
    }
}
