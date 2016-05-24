/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2015. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */
package com.mediatek.galleryfeature.stereo.freeview3d;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.WindowManager;

import com.mediatek.galleryframework.util.MtkLog;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Register and unregister gyroscope. dispatch event.
 */
public class GyroSensorEx {
    private static final String TAG = "MtkGallery2/GyroSensorEx";
    public static final float UNUSABLE_ANGLE_VALUE = -1;
    protected Sensor mGyroSensor;
    protected boolean mHasGyroSensor;
    protected Object mSyncObj = new Object();
    private Context mContext;
    private HashSet<GyroPositionListener> mListener = new HashSet<GyroPositionListener>();
    private PositionListener mPositionListener = new PositionListener();
    private SensorManager mSensorManager;

    /**
     * Calculate angle interface.
     */
    public interface GyroPositionListener {
        /**
         * Calculate angle by current Gyro sensor Event.
         * @param newTimestamp the time in nanosecond at which the event happened.
         * @param eventValues0 angular speed (w/o drift compensation) around the X axis in rad/s.
         * @param eventValues1 angular speed (w/o drift compensation) around the Y axis in rad/s.
         * @param newRotation the display rotation.
         * @return the new angle.
         */
        public float[] onCalculateAngle(long newTimestamp, float eventValues0,
                float eventValues1, int newRotation);
    }

    /**
     * Constructor.
     * @param context get system service by the context.
     */
    public GyroSensorEx(Context context) {
        mContext = context;
        mSensorManager = (SensorManager) mContext
                .getSystemService(Context.SENSOR_SERVICE);
        mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mHasGyroSensor = (mGyroSensor != null);
        if (!mHasGyroSensor) {
            MtkLog.d(TAG, "<GyroSensorEx>not has gyro sensor");
        }
    }

    /**
     * Check if is gyroscope devices.
     * @return whether has Gyrosensor or not.
     */
    public boolean hasGyroSensor() {
        return mHasGyroSensor;
    }

    /**
     *Sensor event listener for dispatch event.
     */
    public class PositionListener implements SensorEventListener {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            onGyroSensorChanged(event);
        }
    }

    /**
     * Add gyroscope listener.
     * @param gyroPositionListener the add gyroscope listener.
     */
    public void setGyroPositionListener(
            GyroPositionListener gyroPositionListener) {
        synchronized (mSyncObj) {
            registerGyroSensorListener();
            mListener.add(gyroPositionListener);
        }
    }

    /**
     * Remove gyroscope listener.
     * @param gyroPositionListener the remove gyroscope listener.
     */
    public void removeGyroPositionListener(
            GyroPositionListener gyroPositionListener) {
        synchronized (mSyncObj) {
            mListener.remove(gyroPositionListener);
            if (mListener.size() == 0) {
                unregisterGyroSensorListener();
            }
        }
    }

    /**
     * Get sensor change event.
     * @param event the SensorEvent.
     */
    public void onGyroSensorChanged(SensorEvent event) {
        synchronized (mSyncObj) {
            if (mListener != null) {
                WindowManager w = (WindowManager) mContext
                        .getSystemService(Context.WINDOW_SERVICE);
                int newRotation = w.getDefaultDisplay().getRotation();
                for (Iterator<GyroPositionListener> it = mListener.iterator(); it
                        .hasNext(); ) {
                    it.next().onCalculateAngle(event.timestamp,
                            event.values[0], event.values[1], newRotation);
                }
            }
        }
    }

    private void registerGyroSensorListener() {
        if (mHasGyroSensor) {
            MtkLog.d(TAG, "<registerGyroSensorListener> gyro sensor listener");
            mSensorManager.registerListener(mPositionListener, mGyroSensor,
                    SensorManager.SENSOR_DELAY_GAME);
        }
    }

    private void unregisterGyroSensorListener() {
        if (mHasGyroSensor) {
            MtkLog.d(TAG,
                    "<unregisterGyroSensorListener>unregister gyro listener");
            mSensorManager.unregisterListener(mPositionListener);
        }
    }
}
