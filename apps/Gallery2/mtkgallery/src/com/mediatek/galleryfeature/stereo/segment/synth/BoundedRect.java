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

import android.graphics.Matrix;
import android.graphics.RectF;

import java.util.Arrays;

/**
 * Maintains invariant that inner rectangle is constrained to be within the
 * outer, rotated rectangle.
 */
public class BoundedRect {
    public final static int CORNER_INDEX_0 = 0;
    public final static int CORNER_INDEX_1 = 1;
    public final static int CORNER_INDEX_2 = 2;
    public final static int CORNER_INDEX_3 = 3;
    public final static int CORNER_INDEX_4 = 4;
    public final static int CORNER_INDEX_5 = 5;
    public final static int CORNER_INDEX_6 = 6;
    public final static int CORNER_INDEX_7 = 7;
    public final static int CORNER_INDEX_8 = 8;
    private final static int TOTAL_COUNT = 8;
    private float mRot;
    private RectF mOuter;
    private RectF mInner;
    private float[] mInnerRotated;

    /**
     * Constructor.
     * @param rotation the rotation of the rect.
     * @param outerRect the outer rect.
     * @param innerRect the inner rect.
     */
    public BoundedRect(float rotation, RectF outerRect, RectF innerRect) {
        mRot = rotation;
        mOuter = new RectF(outerRect);
        mInner = new RectF(innerRect);
        mInnerRotated = OverlayerMath.getCornersFromRect(mInner);
        rotateInner();
        if (!isConstrained()) {
            reconstrain();
        }
    }

    /**
     * Reset rect.
     * @param rotation the rotation of the rect.
     * @param outerRect the outer rect.
     * @param innerRect the inner rect.
     */
    public void resetTo(float rotation, RectF outerRect, RectF innerRect) {
        mRot = rotation;
        mOuter.set(outerRect);
        mInner.set(innerRect);
        mInnerRotated = OverlayerMath.getCornersFromRect(mInner);
        rotateInner();
        if (!isConstrained()) {
            reconstrain();
        }
    }

    /**
     * Sets inner and re-constrains it to fit within the rotated bounding rect.
     * @param newInner the new inner rect.
     */
    public void setInner(RectF newInner) {
        if (mInner.equals(newInner)) {
            return;
        }
        mInner = newInner;
        mInnerRotated = OverlayerMath.getCornersFromRect(mInner);
        rotateInner();
        if (!isConstrained()) {
            reconstrain();
        }
    }

    /**
     * Sets rotation, and re-constrains inner to fit within the rotated bounding
     * rect.
     * @param rotation the rotation.
     */
    public void setRotation(float rotation) {
        if (rotation == mRot) {
            return;
        }
        mRot = rotation;
        mInnerRotated = OverlayerMath.getCornersFromRect(mInner);
        rotateInner();
        if (!isConstrained()) {
            reconstrain();
        }
    }

    /**
     * Set rect.
     * @param r the new inner Rect.
     */
    public void setToInner(RectF r) {
        r.set(mInner);
    }

    /**
     * Set rect.
     * @param r the new outer Rect.
     */
    public void setToOuter(RectF r) {
        r.set(mOuter);
    }

    public RectF getInner() {
        return new RectF(mInner);
    }

    public RectF getOuter() {
        return new RectF(mOuter);
    }

    /**
     * Tries to move the inner rectangle by (dx, dy). If this would cause it to
     * leave the bounding rectangle, snaps the inner rectangle to the edge of
     * the bounding rectangle.
     * @param dx the move distance in x axis.
     * @param dy the move distance in y axis.
     */
    public void moveInner(float dx, float dy) {
        Matrix m0 = getInverseRotMatrix();
        RectF translatedInner = new RectF(mInner);
        translatedInner.offset(dx, dy);

        float[] translatedInnerCorners = OverlayerMath
                .getCornersFromRect(translatedInner);
        float[] outerCorners = OverlayerMath.getCornersFromRect(mOuter);

        m0.mapPoints(translatedInnerCorners);
        float[] correction = { 0, 0 };

        // find correction vectors for corners that have moved out of bounds
        for (int i = 0; i < translatedInnerCorners.length; i += 2) {
            float correctedInnerX = translatedInnerCorners[i] + correction[0];
            float correctedInnerY = translatedInnerCorners[i + 1]
                    + correction[1];
            if (!OverlayerMath.inclusiveContains(mOuter, correctedInnerX,
                    correctedInnerY)) {
                float[] badCorner = { correctedInnerX, correctedInnerY };
                float[] nearestSide = OverlayerMath.closestSide(badCorner,
                        outerCorners);
                float[] correctionVec = shortestVectorFromPointToLine(
                        badCorner, nearestSide);
                if (correctionVec != null) {
                    correction[0] += correctionVec[0];
                    correction[1] += correctionVec[1];
                }
            }
        }

        for (int i = 0; i < translatedInnerCorners.length; i += 2) {
            float correctedInnerX = translatedInnerCorners[i] + correction[0];
            float correctedInnerY = translatedInnerCorners[i + 1]
                    + correction[1];
            if (!OverlayerMath.inclusiveContains(mOuter, correctedInnerX,
                    correctedInnerY)) {
                float[] correctionVec = { correctedInnerX, correctedInnerY };
                OverlayerMath.getEdgePoints(mOuter, correctionVec);
                correctionVec[0] -= correctedInnerX;
                correctionVec[1] -= correctedInnerY;
                correction[0] += correctionVec[0];
                correction[1] += correctionVec[1];
            }
        }

        // Set correction
        for (int i = 0; i < translatedInnerCorners.length; i += 2) {
            float correctedInnerX = translatedInnerCorners[i] + correction[0];
            float correctedInnerY = translatedInnerCorners[i + 1]
                    + correction[1];
            // update translated corners with correction vectors
            translatedInnerCorners[i] = correctedInnerX;
            translatedInnerCorners[i + 1] = correctedInnerY;
        }

        mInnerRotated = translatedInnerCorners;
        // reconstrain to update inner
        reconstrain();
    }

    /**
     * Attempts to resize the inner rectangle. If this would cause it to leave
     * the bounding rect, clips the inner rectangle to fit.
     * @param newInner the new inner.
     */
    public void resizeInner(RectF newInner) {
        Matrix m = getRotMatrix();
        Matrix m0 = getInverseRotMatrix();
        float[] outerCorners = OverlayerMath.getCornersFromRect(mOuter);
        m.mapPoints(outerCorners);
        float[] oldInnerCorners = OverlayerMath.getCornersFromRect(mInner);
        float[] newInnerCorners = OverlayerMath.getCornersFromRect(newInner);
        RectF ret = new RectF(newInner);
        for (int i = 0; i < newInnerCorners.length; i += 2) {
            float[] c = { newInnerCorners[i], newInnerCorners[i + 1] };
            float[] c0 = Arrays.copyOf(c, 2);
            m0.mapPoints(c0);
            if (!OverlayerMath.inclusiveContains(mOuter, c0[0], c0[1])) {
                float[] outerSide = OverlayerMath.closestSide(c, outerCorners);
                if (outerSide == null) {
                    break;
                }
                float[] pathOfCorner = { newInnerCorners[i],
                        newInnerCorners[i + 1], oldInnerCorners[i],
                        oldInnerCorners[i + 1] };
                float[] p = lineIntersect(pathOfCorner, outerSide);
                if (p == null) {
                    // lines are parallel or not well defined, so don't resize
                    p = new float[2];
                    p[0] = oldInnerCorners[i];
                    p[1] = oldInnerCorners[i + 1];
                }
                // relies on corners being in same order as method
                // getCornersFromRect
                switch (i) {
                case CORNER_INDEX_0:
                case CORNER_INDEX_1:
                    ret.left = (p[0] > ret.left) ? p[0] : ret.left;
                    ret.top = (p[1] > ret.top) ? p[1] : ret.top;
                    break;
                case CORNER_INDEX_2:
                case CORNER_INDEX_3:
                    ret.right = (p[0] < ret.right) ? p[0] : ret.right;
                    ret.top = (p[1] > ret.top) ? p[1] : ret.top;
                    break;
                case CORNER_INDEX_4:
                case CORNER_INDEX_5:
                    ret.right = (p[0] < ret.right) ? p[0] : ret.right;
                    ret.bottom = (p[1] < ret.bottom) ? p[1] : ret.bottom;
                    break;
                case CORNER_INDEX_6:
                case CORNER_INDEX_7:
                    ret.left = (p[0] > ret.left) ? p[0] : ret.left;
                    ret.bottom = (p[1] < ret.bottom) ? p[1] : ret.bottom;
                    break;
                default:
                    break;
                }
            }
        }
        float[] retCorners = OverlayerMath.getCornersFromRect(ret);
        m0.mapPoints(retCorners);
        mInnerRotated = retCorners;
        reconstrain();
    }

    /**
     * Attempts to resize the inner rectangle. If this would cause it to leave
     * the bounding rect, clips the inner rectangle to fit while maintaining
     * aspect ratio.
     * @param newInner the new inner.
     */
    public void fixedAspectResizeInner(RectF newInner) {
        if (newInner == null) {
            return;
        }
        Matrix m = getRotMatrix();
        Matrix m0 = getInverseRotMatrix();
        float aspectW = mInner.width();
        float aspectH = mInner.height();
        float aspRatio = aspectW / aspectH;
        float[] corners = OverlayerMath.getCornersFromRect(mOuter);
        m.mapPoints(corners);
        float[] newInnerCorners = OverlayerMath.getCornersFromRect(newInner);
        // find fixed corner
        int fixed = -1;
        if (mInner.top == newInner.top) {
            if (mInner.left == newInner.left) {
                // top left
                fixed = CORNER_INDEX_0;
            } else if (mInner.right == newInner.right) {
                fixed = CORNER_INDEX_2; // top right
            }
        } else if (mInner.bottom == newInner.bottom) {
            if (mInner.right == newInner.right) {
                fixed = CORNER_INDEX_4; // bottom right
            } else if (mInner.left == newInner.left) {
                fixed = CORNER_INDEX_6; // bottom left
            }
        }
        // no fixed corner, return without update
        /*
         * if (fixed == -1) return;
         */
        float widthSoFar = newInner.width();
        boolean needupdate = false;
        for (int i = 0; i < newInnerCorners.length; i += 2) {
            float[] c = { newInnerCorners[i], newInnerCorners[i + 1] };
            float[] c0 = Arrays.copyOf(c, 2);
            m0.mapPoints(c0);
            if (!OverlayerMath.inclusiveContains(mOuter, c0[0], c0[1])) {
                needupdate = true;
                break;
            }
        }
        if (needupdate) {
            return;
        }
        float heightSoFar = widthSoFar / aspRatio;
        RectF ret = new RectF(mInner);
        ret.right = ret.right + (widthSoFar - mInner.width()) / 2;
        ret.left = ret.left - (widthSoFar - mInner.width()) / 2;
        ret.top = ret.top - (heightSoFar - mInner.height()) / 2;
        ret.bottom = ret.bottom + (heightSoFar - mInner.height()) / 2;
        float[] retCorners = OverlayerMath.getCornersFromRect(ret);
        m0.mapPoints(retCorners);
        mInnerRotated = retCorners;
        // reconstrain to update inner
        reconstrain();
    }

    /**
     * Reset inner.
     * @param origin the new rect.
     */
    public void resetInner(RectF origin) {
        mInner.set(origin);
    }

    /**
     * LineInter sect.
     * @param line1 the first line.
     * @param line2 the second line.
     * @return the intersect points.
     */
    public static float[] lineIntersect(float[] line1, float[] line2) {
        float a0 = line1[CORNER_INDEX_0];
        float a1 = line1[CORNER_INDEX_1];
        float b0 = line1[CORNER_INDEX_2];
        float b1 = line1[CORNER_INDEX_3];
        float c0 = line2[CORNER_INDEX_0];
        float c1 = line2[CORNER_INDEX_1];
        float d0 = line2[CORNER_INDEX_2];
        float d1 = line2[CORNER_INDEX_3];
        float t0 = a0 - b0;
        float t1 = a1 - b1;
        float t2 = b0 - d0;
        float t3 = d1 - b1;
        float t4 = c0 - d0;
        float t5 = c1 - d1;

        float denom = t1 * t4 - t0 * t5;
        if (denom == 0) {
            return null;
        }
        float u = (t3 * t4 + t5 * t2) / denom;
        float[] intersect = { b0 + u * t0, b1 + u * t1 };
        return intersect;
    }

    /**
     * Shortest vector from point to line.
     * @param point the point.
     * @param line the line.
     * @return the point.
     */
    public static float[] shortestVectorFromPointToLine(float[] point,
            float[] line) {
        if (point == null || line == null) {
            return null;
        }
        float x1 = line[CORNER_INDEX_0];
        float x2 = line[CORNER_INDEX_2];
        float y1 = line[CORNER_INDEX_1];
        float y2 = line[CORNER_INDEX_3];
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

    // internal methods

    private boolean isConstrained() {
        for (int i = 0; i < TOTAL_COUNT; i += 2) {
            if (!OverlayerMath.inclusiveContains(mOuter, mInnerRotated[i],
                    mInnerRotated[i + 1])) {
                return false;
            }
        }
        return true;
    }

    private void reconstrain() {
        // mInnerRotated has been changed to have incorrect values
        OverlayerMath.getEdgePoints(mOuter, mInnerRotated);
        Matrix m = getRotMatrix();
        float[] unrotated = Arrays.copyOf(mInnerRotated, TOTAL_COUNT);
        m.mapPoints(unrotated);
        mInner = OverlayerMath.trapToRect(unrotated);
    }

    private void rotateInner() {
        Matrix m = getInverseRotMatrix();
        m.mapPoints(mInnerRotated);
    }

    private Matrix getRotMatrix() {
        Matrix m = new Matrix();
        m.setRotate(mRot, mOuter.centerX(), mOuter.centerY());
        return m;
    }

    private Matrix getInverseRotMatrix() {
        Matrix m = new Matrix();
        m.setRotate(-mRot, mOuter.centerX(), mOuter.centerY());
        return m;
    }

}
