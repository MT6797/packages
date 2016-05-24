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

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;

import java.util.Arrays;

/**
 * Utils class for Overlayer.
 */
public class OverlayerMath {
    private final static int OFFSET_POINTS = 2;

    /**
     * Gets a float array of the 2D coordinates representing a rectangles
     * corners. The order of the corners in the float array is: 0------->1 ^ | |
     * v 3<-------2
     * @param r
     *            the rectangle to get the corners of
     * @return the float array of corners (8 floats)
     */

    public static float[] getCornersFromRect(RectF r) {
        float[] corners = { r.left, r.top, r.right, r.top, r.right, r.bottom,
                r.left, r.bottom };
        return corners;
    }

    /**
     * Returns true iff point (x, y) is within or on the rectangle's bounds.
     * RectF's "contains" function treats points on the bottom and right bound
     * as not being contained.
     * @param r
     *            the rectangle
     * @param x
     *            the x value of the point
     * @param y
     *            the y value of the point
     * @return return state.
     */
    public static boolean inclusiveContains(RectF r, float x, float y) {
        return !(x > r.right || x < r.left || y > r.bottom || y < r.top);
    }

    /**
     * Takes an array of 2D coordinates representing corners and returns the
     * smallest rectangle containing those coordinates.
     * @param array
     *            array of 2D coordinates
     * @return smallest rectangle containing coordinates
     */
    public static RectF trapToRect(float[] array) {
        RectF r = new RectF(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        for (int i = 1; i < array.length; i += 2) {
            float x = array[i - 1];
            float y = array[i];
            r.left = (x < r.left) ? x : r.left;
            r.top = (y < r.top) ? y : r.top;
            r.right = (x > r.right) ? x : r.right;
            r.bottom = (y > r.bottom) ? y : r.bottom;
        }
        r.sort();
        return r;
    }

    /**
     * If edge point [x, y] in array [x0, y0, x1, y1, ...] is outside of the
     * image bound rectangle, clamps it to the edge of the rectangle.
     * @param imageBound
     *            the rectangle to clamp edge points to.
     * @param array
     *            an array of points to clamp to the rectangle, gets set to the
     *            clamped values.
     */
    public static void getEdgePoints(RectF imageBound, float[] array) {
        if (array.length < 2) {
            return;
        }
        for (int x = 0; x < array.length; x += 2) {
            array[x] = clamp(array[x], imageBound.left, imageBound.right);
            array[x + 1] = clamp(array[x + 1], imageBound.top,
                    imageBound.bottom);
        }
    }

    private static float clamp(float i, float low, float high) {
        return Math.max(Math.min(i, high), low);
    }

    /**
     * Takes a point and the corners of a rectangle and returns the two corners
     * representing the side of the rectangle closest to the point.
     * @param point
     *            the point which is being checked
     * @param corners
     *            the corners of the rectangle
     * @return two corners representing the side of the rectangle
     */
    public static float[] closestSide(float[] point, float[] corners) {
        int len = corners.length;
        float oldMag = Float.POSITIVE_INFINITY;
        float[] bestLine = null;
        for (int i = 0; i < len; i += 2) {
            float[] line = { corners[i], corners[(i + 1) % len],
                    corners[(i + OFFSET_POINTS) % len],
                    corners[(i + OFFSET_POINTS + 1) % len] };
            float mag = vectorLength(shortestVectorFromPointToLine(point, line));
            if (mag < oldMag) {
                oldMag = mag;
                bestLine = line;
            }
        }
        return bestLine;
    }

    private static float vectorLength(float[] a) {
        if (a != null) {
            return (float) Math.sqrt(a[0] * a[0] + a[1] * a[1]);
        } else {
            return Float.POSITIVE_INFINITY;
        }
    }

    private static float[] shortestVectorFromPointToLine(float[] point,
            float[] line) {
        float x1 = line[0];
        float x2 = line[OFFSET_POINTS];
        float y1 = line[1];
        float y2 = line[OFFSET_POINTS + 1];
        float xdelt = x2 - x1;
        float ydelt = y2 - y1;
        if (xdelt == 0 && ydelt == 0) {
            return null;
        }
        float u = ((point[0] - x1) * xdelt + (point[1] - y1) * ydelt)
                / (xdelt * xdelt + ydelt * ydelt);
        float[] ret = { (x1 + u * (x2 - x1)), (y1 + u * (y2 - y1)) };
        float[] vec = { ret[0] - point[0], ret[1] - point[1] };
        return vec;
    }

    /**
     * Checks if a given point is within a rotated rectangle.
     * @param point
     *            2D point to check
     * @param bound
     *            rectangle to rotate
     * @param rot
     *            angle of rotation about rectangle center
     * @return true if point is within rotated rectangle
     */
    public static boolean pointInRotatedRect(float[] point, RectF bound,
            float rot) {
        Matrix m = new Matrix();
        float[] p = Arrays.copyOf(point, OFFSET_POINTS);
        m.setRotate(rot, bound.centerX(), bound.centerY());
        Matrix m0 = new Matrix();
        if (!m.invert(m0)) {
            return false;
        }
        m0.mapPoints(p);
        return inclusiveContains(bound, p[0], p[1]);
    }

    /**
     * Checks if a given point is within a rotated rectangle.
     * @param point
     *            2D point to check
     * @param rotatedRect
     *            corners of a rotated rectangle
     * @param center
     *            center of the rotated rectangle
     * @return true if point is within rotated rectangle
     */
    public static boolean pointInRotatedRect(float[] point,
            float[] rotatedRect, float[] center) {
        RectF unrotated = new RectF();
        float angle = getUnrotated(rotatedRect, center, unrotated);
        return pointInRotatedRect(point, unrotated, angle);
    }

    /**
     * Resizes rectangle to have a certain aspect ratio (center remains
     * stationary).
     * @param r
     *            rectangle to resize
     * @param w
     *            new width aspect
     * @param h
     *            new height aspect
     */
    public static void fixAspectRatio(RectF r, float w, float h) {
        float scale = Math.min(r.width() / w, r.height() / h);
        float centX = r.centerX();
        float centY = r.centerY();
        float hw = scale * w / 2;
        float hh = scale * h / 2;
        r.set(centX - hw, centY - hh, centX + hw, centY + hh);
    }

    /**
     * Resizes rectangle to have a certain aspect ratio (center remains
     * stationary) while constraining it to remain within the original rect.
     * @param r
     *            rectangle to resize
     * @param w
     *            new width aspect
     * @param h
     *            new height aspect
     */
    public static void fixAspectRatioContained(RectF r, float w, float h) {
        float origW = r.width();
        float origH = r.height();
        float origA = origW / origH;
        float a = w / h;
        float finalW = origW;
        float finalH = origH;
        if (origA < a) {
            finalH = origW / a;
            r.top = r.centerY() - finalH / 2;
            r.bottom = r.top + finalH;
        } else {
            finalW = origH * a;
            r.left = r.centerX() - finalW / 2;
            r.right = r.left + finalW;
        }
    }

    /**
     * Stretches/Scales/Translates photoBounds to match displayBounds, and and
     * returns an equivalent stretched/scaled/translated cropBounds or null if
     * the mapping is invalid.
     * @param cropBounds
     *            cropBounds to transform
     * @param photoBounds
     *            original bounds containing crop bounds
     * @param displayBounds
     *            final bounds for crop
     * @return the stretched/scaled/translated crop bounds that fit within
     *         displayBounds
     */
    public static RectF getScaledCropBounds(RectF cropBounds,
            RectF photoBounds, RectF displayBounds) {
        Matrix m = new Matrix();
        m.setRectToRect(photoBounds, displayBounds, Matrix.ScaleToFit.FILL);
        RectF trueCrop = new RectF(cropBounds);
        if (!m.mapRect(trueCrop)) {
            return null;
        }
        return trueCrop;
    }

    /**
     * Returns the size of a bitmap in bytes.
     * @param bmap
     *            bitmap whose size to check
     * @return bitmap size in bytes
     */
    public static int getBitmapSize(Bitmap bmap) {
        return bmap.getRowBytes() * bmap.getHeight();
    }

    /**
     * Constrains rotation to be in [0, 90, 180, 270] rounding down.
     * @param rotation
     *            any rotation value, in degrees
     * @return integer rotation in [0, 90, 180, 270]
     */
    public static int constrainedRotation(float rotation) {
        int r = (int) ((rotation % OverLayController.ORITATION_360)
                / OverLayController.ORITATION_90);
        r = (r < 0) ? (r + OFFSET_POINTS * 2) : r;
        return r * OverLayController.ORITATION_90;
    }

    private static float getUnrotated(float[] rotatedRect, float[] center,
            RectF unrotated) {
        float dy = rotatedRect[1] - rotatedRect[OFFSET_POINTS + 1];
        float dx = rotatedRect[0] - rotatedRect[OFFSET_POINTS];
        float angle = (float) (Math.atan(dy / dx)
                * OverLayController.ORITATION_180 / Math.PI);
        Matrix m = new Matrix();
        m.setRotate(-angle, center[0], center[1]);
        float[] unrotatedRect = new float[rotatedRect.length];
        m.mapPoints(unrotatedRect, rotatedRect);
        unrotated.set(trapToRect(unrotatedRect));
        return angle;
    }

}
