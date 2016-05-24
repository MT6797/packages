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

import android.view.Surface;

import com.mediatek.galleryframework.util.MtkLog;

import java.util.Arrays;

/**
 * Calculator angle for gyroscope sensor.
 */
public class GyroPositionCalculator {
    private static final String TAG = "MtkGallery2/CalculateGyroPosition";
    private static final int ANGLE_PI = 180;
    private static final float BASE_ANGLE = 15f;
    private static final float NS2S = 0.000000001f;
    private static final float TH = 0.001f;
    private static final float OFFSET = 0.0f;
    private static final float UNUSABLE_ANGLE_VALUE = -1;
    private int mOrientation = -1;
    private float mValueOfXAxis = 0;
    private float mValueOfYAxis = 0;
    private long mTimestamp = 0;
    private float mAnglesOfXAxis[] = { 0, 0, 0 };
    private float mAnglesOfYAxis[] = { 0, 0, 0 };
    private boolean mFirstTime = true;
    private float mAngle[] = { UNUSABLE_ANGLE_VALUE, UNUSABLE_ANGLE_VALUE,
            UNUSABLE_ANGLE_VALUE };

    public float[] getAngle() {
        return mAngle;
    }

    /**
     * Calculate angle by current Gyro sensor Event.
     * @param newTimestamp the time in nanosecond at which the event happened.
     * @param eventValuesX angular speed (w/o drift compensation) around the X axis in rad/s.
     * @param eventValuesY angular speed (w/o drift compensation) around the Y axis in rad/s.
     * @param newRotation the display rotation.
     * @return the new angle.
     */
    public float[] calculateAngle(long newTimestamp, float eventValuesX,
            float eventValuesY, int newRotation) {
        // workaround for Gyro sensor HW limitation.
        // As sensor continues to report small movement, wrongly
        // indicating that the phone is slowly moving, we should
        // filter the small movement.
        float valueOfXAxis = 0;
        float valueOfYAxis = 0;
        if (mOrientation != newRotation) {
            // orientation has changed, reset calculations
            mOrientation = newRotation;
            mValueOfXAxis = 0;
            mValueOfYAxis = 0;
            Arrays.fill(mAnglesOfXAxis, 0);
            Arrays.fill(mAnglesOfYAxis, 0);
            mFirstTime = true;
        }
        MtkLog.d(TAG, "newRotation = " + newRotation + " eventValuesY = "
                + eventValuesY + " " + "||||" + " " + eventValuesX);
        switch (mOrientation) {
        case Surface.ROTATION_0:
            valueOfXAxis = eventValuesY;
            valueOfYAxis = -eventValuesX;
            break;
        case Surface.ROTATION_90:
            // no need to re-map
            valueOfXAxis = eventValuesX;
            valueOfYAxis = eventValuesY;
            break;
        case Surface.ROTATION_180:
            // we do not have this rotation on our device
            valueOfXAxis = -eventValuesY;
            valueOfYAxis = -eventValuesX;
            break;
        case Surface.ROTATION_270:
            valueOfXAxis = -eventValuesX;
            valueOfYAxis = -eventValuesY;
            break;
        default:
            valueOfXAxis = eventValuesX;
        }
        mValueOfXAxis = valueOfXAxis + OFFSET;
        mValueOfYAxis = valueOfYAxis + OFFSET;
        mAngle[0] = calculateAngle(mAnglesOfXAxis, mValueOfXAxis, newTimestamp);
        mAngle[1] = calculateAngle(mAnglesOfYAxis, mValueOfYAxis, newTimestamp);
        mTimestamp = newTimestamp;
        if (mAngle[0] != UNUSABLE_ANGLE_VALUE) {
            return mAngle;
        } else {
            return null;
        }
    }

    private float calculateAngle(float mAngles[], float mValue,
            long newTimestamp) {
        if (mTimestamp != 0 && Math.abs(mValue) > TH) {
            final float dT = (newTimestamp - mTimestamp) * NS2S;
            mAngles[1] += mValue * dT * ANGLE_PI / Math.PI;
            if (mFirstTime) {
                mAngles[0] = mAngles[1] - BASE_ANGLE;
                mAngles[2] = mAngles[1] + BASE_ANGLE;
                mFirstTime = false;
            } else if (mAngles[1] <= mAngles[0]) {
                mAngles[0] = mAngles[1];
                mAngles[2] = mAngles[0] + 2 * BASE_ANGLE;
            } else if (mAngles[1] >= mAngles[2]) {
                mAngles[2] = mAngles[1];
                mAngles[0] = mAngles[2] - 2 * BASE_ANGLE;
            }
        }
        float angle;
        if (mTimestamp != 0) {
            angle = (mAngles[1] - mAngles[0]) / (2 * BASE_ANGLE);
        } else {
            angle = UNUSABLE_ANGLE_VALUE;
        }
        return angle;
    }

}
