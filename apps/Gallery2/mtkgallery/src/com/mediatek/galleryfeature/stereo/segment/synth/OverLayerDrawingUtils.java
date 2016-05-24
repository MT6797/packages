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

package com.mediatek.galleryfeature.stereo.segment.synth;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Over layer drawing utils.
 */
public abstract class OverLayerDrawingUtils {
private final static int STROKE_WIDTH = 3;
    /**
     * Set draw rect.
     * @param canvas the canvas for draw.
     * @param bounds the bounds for draw.
     */
    public static void drawCropRect(Canvas canvas, RectF bounds) {
        Paint p = new Paint();
        p.setStyle(Paint.Style.STROKE);
        p.setColor(Color.WHITE);
        p.setStrokeWidth(STROKE_WIDTH);
        canvas.drawRect(bounds, p);
    }

    private static void drawIndicator(Canvas canvas, Drawable indicator,
            int indicatorSize, float centerX, float centerY) {
        int left = (int) centerX - indicatorSize / 2;
        int top = (int) centerY - indicatorSize / 2;
        indicator.setBounds(left, top, left + indicatorSize, top
                + indicatorSize);
        indicator.draw(canvas);
    }

    /**
     * Draw indicators.
     * @param canvas the canvas for draw.
     * @param cropIndicator the crop indicator to draw.
     * @param indicatorSize the size of indicator.
     * @param bounds the draw bounds.
     * @param fixedAspect Whether fix aspect.
     * @param selection the draw corner.
     */
    public static void drawIndicators(Canvas canvas, Drawable cropIndicator,
            int indicatorSize, RectF bounds, boolean fixedAspect, int selection) {
        boolean notMoving = (selection == OverLayerObject.MOVE_NONE);
        if (fixedAspect) {
            if ((selection == OverLayerObject.TOP_LEFT) || notMoving) {
                drawIndicator(canvas, cropIndicator, indicatorSize,
                        bounds.left, bounds.top);
            }
            if ((selection == OverLayerObject.TOP_RIGHT) || notMoving) {
                drawIndicator(canvas, cropIndicator, indicatorSize,
                        bounds.right, bounds.top);
            }
            if ((selection == OverLayerObject.BOTTOM_LEFT) || notMoving) {
                drawIndicator(canvas, cropIndicator, indicatorSize,
                        bounds.left, bounds.bottom);
            }
            if ((selection == OverLayerObject.BOTTOM_RIGHT) || notMoving) {
                drawIndicator(canvas, cropIndicator, indicatorSize,
                        bounds.right, bounds.bottom);
            }
        } else {
            if (((selection & OverLayerObject.MOVE_TOP) != 0) || notMoving) {
                drawIndicator(canvas, cropIndicator, indicatorSize,
                        bounds.centerX(), bounds.top);
            }
            if (((selection & OverLayerObject.MOVE_BOTTOM) != 0) || notMoving) {
                drawIndicator(canvas, cropIndicator, indicatorSize,
                        bounds.centerX(), bounds.bottom);
            }
            if (((selection & OverLayerObject.MOVE_LEFT) != 0) || notMoving) {
                drawIndicator(canvas, cropIndicator, indicatorSize,
                        bounds.left, bounds.centerY());
            }
            if (((selection & OverLayerObject.MOVE_RIGHT) != 0) || notMoving) {
                drawIndicator(canvas, cropIndicator, indicatorSize,
                        bounds.right, bounds.centerY());
            }
        }
    }

    /**
     * Set image to screenMatrix.
     * @param dst the display matrix.
     * @param image the image rect.
     * @param screen the screen rect.
     * @param rotation the rotation background.
     * @return whether successful set.
     */
    public static boolean setImageToScreenMatrix(Matrix dst, RectF image,
            RectF screen, int rotation) {
        RectF rotatedImage = new RectF();
        if (!dst.mapRect(rotatedImage, image)) {
            return false; // fails for rotations that are not multiples of 90
            // degrees
        }
        boolean rToR = dst.setRectToRect(rotatedImage, screen,
                Matrix.ScaleToFit.CENTER);
        boolean rot = dst.preRotate(rotation, image.centerX(), image.centerY());
        return rToR && rot;
    }

}
