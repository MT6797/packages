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

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.deskclock.AlarmAlertWakeLock;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.AlarmInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * This service is in charge of starting/stopping the alarm. It will bring up and manage the
 * {@link AlarmActivity} as well as {@link AlarmKlaxon}.
 *
 * Registers a broadcast receiver to listen for snooze/dismiss intents. The broadcast receiver
 * exits early if AlarmActivity is bound to prevent double-processing of the snooze/dismiss intents.
 */
public class AlarmService extends Service {
    /**
     * AlarmActivity and AlarmService (when unbound) listen for this broadcast intent
     * so that other applications can snooze the alarm (after ALARM_ALERT_ACTION and before
     * ALARM_DONE_ACTION).
     */
    public static final String ALARM_SNOOZE_ACTION = "com.android.deskclock.ALARM_SNOOZE";

    /**
     * AlarmActivity and AlarmService listen for this broadcast intent so that other
     * applications can dismiss the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
     */
    public static final String ALARM_DISMISS_ACTION = "com.android.deskclock.ALARM_DISMISS";

    /** A public action sent by AlarmService when the alarm has started. */
    public static final String ALARM_ALERT_ACTION = "com.android.deskclock.ALARM_ALERT";

    /** A public action sent by AlarmService when the alarm has stopped for any reason. */
    public static final String ALARM_DONE_ACTION = "com.android.deskclock.ALARM_DONE";

    /** Private action used to start an alarm with this service. */
    public static final String START_ALARM_ACTION = "START_ALARM";

    /** Private action used to stop an alarm with this service. */
    public static final String STOP_ALARM_ACTION = "STOP_ALARM";

    /** Binder given to AlarmActivity */
    private final IBinder mBinder = new Binder();

    /** Whether the service is currently bound to AlarmActivity */
    private boolean mIsBound = false;

    /** Whether the receiver is currently registered */
    private boolean mIsRegistered = false;

    private TelephonyStateListener mPhoneStateListener = new TelephonyStateListener();

    @Override
    public IBinder onBind(Intent intent) {
        mIsBound = true;
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mIsBound = false;
        return super.onUnbind(intent);
    }


    /// M: Stop the alarm alert when the device shut down.
    public static final String PRE_SHUTDOWN_ACTION = "android.intent.action.ACTION_PRE_SHUTDOWN";

    /// M: Stop the alarm alert when privacy protection lock enable.
    public static final String PRIVACY_PROTECTION_CLOCK = "com.mediatek.ppl.NOTIFY_LOCK";

    /// M: Power off alarm start and stop deskclock play ringtone. @{
    private static final String NORMAL_SHUTDOWN_ACTION = "android.intent.action.normal.shutdown";
    private static final String ALARM_REQUEST_SHUTDOWN_ACTION = "android.intent.action.ACTION_ALARM_REQUEST_SHUTDOWN";

    private static final String POWER_OFF_ALARM_START_ACITION = "com.android.deskclock.START_ALARM";
    private static final String POWER_OFF_ALARM_POWER_ON_ACITION = "com.android.deskclock.POWER_ON_ALARM";
    private static final String POWER_OFF_ALARM_DISMISS_ACITION = "com.android.deskclock.DISMISS_ALARM";
    public static final String POWER_OFF_ALARM_SNOOZE_ACITION = "com.android.deskclock.SNOOZE_ALARM";
    private static boolean  mStopPlayReceiverRegistered = false;
    /// @}

    private final BroadcastReceiver mStopPlayReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtils.v("AlarmService mStopPlayReceiver: " + intent.getAction());
            if (mCurrentAlarm == null) {
                LogUtils.v("mStopPlayReceiver mCurrentAlarm is null, just return");
                return;
            }
            /// M: Send by the PowerOffAlarm AlarmAlertFullScreen, user drag the icon or time out
            if (intent.getAction().equals(POWER_OFF_ALARM_SNOOZE_ACITION)) {
                AlarmStateManager.setSnoozeState(context, mCurrentAlarm, false);
                /// M: Now it is time to delete the unused backup ringtone
                PowerOffAlarm.deleteRingtone(context, mCurrentAlarm);
                shutDown(context);
            } else {
                /// M: Power on action or pre_shutdown, so set dismiss state and don't shut down
                AlarmStateManager.setDismissState(context, mCurrentAlarm);
                /// M: Now it is time to delete the unused backup ringtone
                PowerOffAlarm.deleteRingtone(context, mCurrentAlarm);
                /// M: Send by the PowerOffAlarm AlarmAlertFullScreen, set dismiss state and shut down
                if (intent.getAction().equals(POWER_OFF_ALARM_DISMISS_ACITION)) {
                    shutDown(context);
                }
            }
        }
    };

    /**
     * Utility method to help start alarm properly. If alarm is already firing, it
     * will mark it as missed and start the new one.
     *
     * @param context application context
     * @param instance to trigger alarm
     */
    public static void startAlarm(Context context, AlarmInstance instance) {
        final Intent intent = AlarmInstance.createIntent(context, AlarmService.class, instance.mId)
                .setAction(START_ALARM_ACTION);

        // Maintain a cpu wake lock until the service can get it
        AlarmAlertWakeLock.acquireCpuWakeLock(context);
        context.startService(intent);
    }

    /**
     * Utility method to help stop an alarm properly. Nothing will happen, if alarm is not firing
     * or using a different instance.
     *
     * @param context application context
     * @param instance you are trying to stop
     */
    public static void stopAlarm(Context context, AlarmInstance instance) {
        final Intent intent = AlarmInstance.createIntent(context, AlarmService.class, instance.mId)
                .setAction(STOP_ALARM_ACTION);

        // We don't need a wake lock here, since we are trying to kill an alarm
        context.startService(intent);
    }

    private TelephonyManager mTelephonyManager;
    /// M: Define the notification key for start the service foreground
    private static final int NOTIFICATION_KEY_FOREGROUND = -1;
    /// M: init the parameter
    private int mInitialCallState = TelephonyManager.CALL_STATE_IDLE;
    private AlarmInstance mCurrentAlarm = null;

    /// M: Support multi sim card @{
    private Context mContext = null;
    private AlarmInstance mInstance = null;
    private AlarmInstance mInstanceAlarm = null;
    /*private final List<TelephonyStateListener> mStateListeners =
            new ArrayList<TelephonyStateListener>();
            */
    /// @}

    /// M: Define the params
    private SubscriptionManager mSubscriptionManager;

    /// M: in order to register for separate listener, use extends @{
    private class TelephonyStateListener extends PhoneStateListener {

        TelephonyStateListener() {
            super();
        }


        TelephonyStateListener(int subscription) {
            super(subscription);
        }
    /// @}

        @Override
        public void onCallStateChanged(int state, String ignored) {
            if (mCurrentAlarm == null) {
                LogUtils.v("onStateChange mCurrentAlarm is null, just return");
                return;
            }
            // The user might already be in a call when the alarm fires. When
            // we register onCallStateChanged, we get the initial in-call state
            // which kills the alarm. Check against the initial call state so
            // we don't kill the alarm during a call.
            if (state != TelephonyManager.CALL_STATE_IDLE
                    && mInitialCallState == TelephonyManager.CALL_STATE_IDLE) {
                LogUtils.v("AlarmService onCallStateChanged sendBroadcast to Missed alarm");
                sendBroadcast(AlarmStateManager.createStateChangeIntent(AlarmService.this,
                        "AlarmService", mCurrentAlarm, AlarmInstance.MISSED_STATE));
            }

            /// M: If the state change to CALL_STATE_IDLE, it means the user havn't in the call @{
            int newPhoneState = mTelephonyManager.getCallState();
            LogUtils.v("AlarmService onCallStateChanged state = " + state
                    + " ,newState: " + newPhoneState + " ,initState = " + mInitialCallState);
            if (newPhoneState == TelephonyManager.CALL_STATE_IDLE
                    && state == TelephonyManager.CALL_STATE_IDLE && state != mInitialCallState) {
                /// M: If the alarm has been dismissed by user, shouldn't restart the alarm
                if (null != mInstanceAlarm
                        && mInstanceAlarm.mAlarmState == AlarmInstance.FIRED_STATE) {
                    LogUtils.v("AlarmService AlarmFiredState startAlarm");
                    mCurrentAlarm = null;
                    startAlarm(mContext, mInstanceAlarm);
                    mInitialCallState = TelephonyManager.CALL_STATE_IDLE;
                }
            }
            /// @}
        }
    }

    private void startAlarmKlaxon(AlarmInstance instance) {
        LogUtils.v("AlarmService.start with instance: " + instance.mId);
        if (mCurrentAlarm != null) {
            AlarmStateManager.setMissedState(this, mCurrentAlarm);
            stopCurrentAlarm();
        }

        AlarmAlertWakeLock.acquireCpuWakeLock(this);

        Events.sendEvent(R.string.category_alarm, R.string.action_fire, 0);

        mCurrentAlarm = instance;

        /// M: init the telephony service and register the listener
        //initTelephonyService();
        mInitialCallState = mTelephonyManager.getCallState();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        //mTelephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);

        boolean inCall = mInitialCallState != TelephonyManager.CALL_STATE_IDLE;

        /// M: If boot from power off alarm, don't show the notification and alarmActivity @{
        if (!PowerOffAlarm.bootFromPoweroffAlarm()) {
            /* M:If user is in call, just show alarm notification without AlarmActivity,
             * otherwise show Alarm Notification with AlarmActivity
             */
            if (inCall) {
                mInstanceAlarm = mCurrentAlarm;
                AlarmNotifications.updateAlarmNotification(this, mCurrentAlarm);
            } else {
                AlarmNotifications.showAlarmNotification(this, mCurrentAlarm);
            }
        } /// @}
        /**
         * M: Set alarmService foreground for the case that alarm's ringtone
         * stop because low memory to kill deskclock process @{
         */
        Notification notification = new Notification();
        notification.flags |= Notification.FLAG_HIDE_NOTIFICATION;
        this.startForeground(NOTIFICATION_KEY_FOREGROUND, notification);
        LogUtils.v("Start set the alarmService foreground");
        /** @} */
        AlarmKlaxon.start(this, mCurrentAlarm,inCall);
        sendBroadcast(new Intent(ALARM_ALERT_ACTION));
    }

    private void stopCurrentAlarm() {
        if (mCurrentAlarm == null) {
            LogUtils.v("There is no current alarm to stop");
            return;
        }

        LogUtils.v("AlarmService.stop with instance: %s", (Object) mCurrentAlarm.mId);
        AlarmKlaxon.stop(this);

        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);

        /// M: Stop listening for incoming calls and clear the listener
        /*
        for (TelephonyStateListener listener : mStateListeners) {
            mTelephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE);
        }
        mStateListeners.clear();
        */

        sendBroadcast(new Intent(ALARM_DONE_ACTION));

        mCurrentAlarm = null;
        AlarmAlertWakeLock.releaseCpuLock();
    }

    private final BroadcastReceiver mActionsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            LogUtils.i("AlarmService received intent %s", action);
            if (mCurrentAlarm == null || mCurrentAlarm.mAlarmState != AlarmInstance.FIRED_STATE) {
                LogUtils.i("No valid firing alarm");
                return;
            }

            if (mIsBound) {
                LogUtils.i("AlarmActivity bound; AlarmService no-op");
                return;
            }

            switch (action) {
                case ALARM_SNOOZE_ACTION:
                    // Set the alarm state to snoozed.
                    // If this broadcast receiver is handling the snooze intent then AlarmActivity
                    // must not be showing, so always show snooze toast.
                    AlarmStateManager.setSnoozeState(context, mCurrentAlarm, true /* showToast */);
                    Events.sendAlarmEvent(R.string.action_snooze, R.string.label_intent);
                    break;
                case ALARM_DISMISS_ACTION:
                    // Set the alarm state to dismissed.
                    AlarmStateManager.setDismissState(context, mCurrentAlarm);
                    Events.sendAlarmEvent(R.string.action_dismiss, R.string.label_intent);
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        // Register the broadcast receiver
        final IntentFilter filter = new IntentFilter(ALARM_SNOOZE_ACTION);
        filter.addAction(ALARM_DISMISS_ACTION);
        registerReceiver(mActionsReceiver, filter);
        mIsRegistered = true;

        mContext = this;
        /// M: Instance the mSubscriptionManager
        mSubscriptionManager = new SubscriptionManager(mContext);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long instanceId = -1;
        /// M: check if it's boot from power off alarm or not
        boolean isAlarmBoot = intent.getBooleanExtra("isAlarmBoot", false);
        IntentFilter filter = new IntentFilter();
        if (PowerOffAlarm.bootFromPoweroffAlarm()) {
            /// M: add the power off alarm snooze\dismiss\power_on action @{
            filter.addAction(POWER_OFF_ALARM_POWER_ON_ACITION);
            filter.addAction(POWER_OFF_ALARM_SNOOZE_ACITION);
            filter.addAction(POWER_OFF_ALARM_DISMISS_ACITION);
        } else {
            /// M: add for DeskClock to dismiss the alarm when preShutDown
            filter.addAction(PRE_SHUTDOWN_ACTION);
            /// M: add for privacy protection lock
            filter.addAction(PRIVACY_PROTECTION_CLOCK);
        }
        registerReceiver(mStopPlayReceiver, filter);
        mStopPlayReceiverRegistered = true;
        /// @}
        if (!isAlarmBoot) {
            instanceId = AlarmInstance.getId(intent.getData());
        }
    switch (intent.getAction()) {
            case START_ALARM_ACTION:
            case POWER_OFF_ALARM_START_ACITION:
             /// M: check if it's boot from power off alarm or not @{
            if (isAlarmBoot) {
                LogUtils.v("AlarmService isAlarmBoot = " + isAlarmBoot);
                mInstance = AlarmStateManager.getNextFiringAlarm(mContext);
                if (mInstance != null) {
                    AlarmStateManager.setFiredState(mContext, mInstance);
                }
            /// @}
            } else {
                ContentResolver cr = this.getContentResolver();
                mInstance = AlarmInstance.getInstance(cr, instanceId);
            }
            LogUtils.v("AlarmService instance[%s]", mInstance);

            if (mInstance == null) {
                LogUtils.e("No instance found to start alarm: " + instanceId);
                if (mCurrentAlarm != null) {
                    // Only release lock if we are not firing alarm
                    AlarmAlertWakeLock.releaseCpuLock();
                }
                return Service.START_NOT_STICKY;
            } else if (mCurrentAlarm != null) {
                if (mCurrentAlarm.mId == mInstance.mId) {
                    LogUtils.e("Alarm already started for instance: " + instanceId);
                    return Service.START_NOT_STICKY;
                } else if (mCurrentAlarm.getAlarmTime().getTimeInMillis()
                        == mInstance.getAlarmTime().getTimeInMillis()) {
                    LogUtils.v("The same time alarm playing, so missed this instance");
                    AlarmStateManager.setMissedState(mContext, mInstance);
                    return Service.START_NOT_STICKY;
                }
            }
            /// M: PowerOffAlarm start and change the label @{
            if (PowerOffAlarm.bootFromPoweroffAlarm()) {
                updatePoweroffAlarmLabel(this, mInstance.mLabel);
            }
            /// @}
            startAlarmKlaxon(mInstance);
                 break;
            case STOP_ALARM_ACTION:
                if (mCurrentAlarm != null && mCurrentAlarm.mId != instanceId) {
                    LogUtils.e("Can't stop alarm for instance: %d because current alarm is: %d",
                            instanceId, mCurrentAlarm.mId);
                    break;
                }
                stopCurrentAlarm();
                stopSelf();
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        LogUtils.v("AlarmService.onDestroy() called");
        /// M: Ordering corrected, onDestroy should be the last method being called
        //super.onDestroy();
        if (mCurrentAlarm != null) {
            stopCurrentAlarm();
        }
        /// M: unregister the power off alarm snooze\dismiss\power_on receiver @{
        if(mStopPlayReceiverRegistered == true){
            unregisterReceiver(mStopPlayReceiver);
            mStopPlayReceiverRegistered = false;
         }
        /// @}

        if (mIsRegistered) {
            unregisterReceiver(mActionsReceiver);
            mIsRegistered = false;
        }
        super.onDestroy();
    }

    /**M: @{
     * update power off alarm label
     */
    private void updatePoweroffAlarmLabel(Context context, String label) {
        Intent intent = new Intent("update.power.off.alarm.label");
        intent.putExtra("label", (label == null ? "" : label));
        context.sendBroadcast(intent);
    }

    /**M: @{
     * shut down the device
     */
    private void shutDown(Context context) {
        // send normal shutdown broadcast
        Intent shutdownIntent = new Intent(NORMAL_SHUTDOWN_ACTION);
        context.sendBroadcast(shutdownIntent);

        // shutdown the device
        Intent intent = new Intent(ALARM_REQUEST_SHUTDOWN_ACTION);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /// M: Get telephony call state
    /*
    private int getCallState() {
        int state = TelephonyManager.CALL_STATE_IDLE;
        List<SubscriptionInfo> subInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (null != subInfoList && !subInfoList.isEmpty()) {
            for (SubscriptionInfo record : subInfoList) {
                state += mTelephonyManager.getCallState(record.getSubscriptionId());
            }
        }
        return state;
    }
    */

    /// M: init the telephony service and register the listen
    /*
    private void initTelephonyService() {
        mInitialCallState = TelephonyManager.CALL_STATE_IDLE;
        List<SubscriptionInfo> subInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (null != subInfoList && !subInfoList.isEmpty()) {
            TelephonyStateListener listener = null;
            for (SubscriptionInfo record : subInfoList) {
                int subId = record.getSubscriptionId();
                mInitialCallState += mTelephonyManager.getCallState(subId);
                listener = new TelephonyStateListener(subId);
                mTelephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
                mStateListeners.add(listener);
            }
        }
    }
    */
}

