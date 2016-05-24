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

import android.graphics.RectF;
/**
 * Over layer object calculate display rect.
 *
 */
public class OverLayerObject {
    public static final int MOVE_NONE = 0;
    // Sides
    public static final int MOVE_LEFT = 1;
    public static final int MOVE_TOP = 2;
    public static final int MOVE_RIGHT = 4;
    public static final int MOVE_BOTTOM = 8;
    public static final int MOVE_BLOCK = 16;
    // Corners
    public static final int TOP_LEFT = MOVE_TOP | MOVE_LEFT;
    public static final int TOP_RIGHT = MOVE_TOP | MOVE_RIGHT;
    public static final int BOTTOM_RIGHT = MOVE_BOTTOM | MOVE_RIGHT;
    public static final int BOTTOM_LEFT = MOVE_BOTTOM | MOVE_LEFT;

    public float mCurrentPointX = 0.0f;
    public float mCurrentPointY = 0.0f;

    private int mMovingEdges = MOVE_NONE;
    private BoundedRect mBoundedRect;
    private boolean mFixAspectRatio = false;
    private float mRotation = 0;
    private float mTouchTolerance = OverLayController.TOLERANCE;
    private float mMinSideSize = OverLayController.MIN_SILE_SIZE;
    // save original inner as mOriginalInnerRect.
    private RectF mOriginalInnerRect = null;

    /**
     * Constructor.
     * @param outerBound outer bound.
     * @param innerBound inner bound.
     * @param outerAngle outer bound angle.
     */
    public OverLayerObject(RectF outerBound, RectF innerBound, int outerAngle) {
        mBoundedRect = new BoundedRect(outerAngle % OverLayController.ORITATION_360,
                outerBound, innerBound);
    }

    /**
     * reset inner bound and outer bound.
     * @param inner inner bound.
     * @param outer outer bound.
     */
    public void resetBoundsTo(RectF inner, RectF outer) {
        mBoundedRect.resetTo(0, outer, inner);
    }

    /**
     * get inner bounds.
     * @param r new inner bound.
     */
    public void getInnerBounds(RectF r) {
        mBoundedRect.setToInner(r);
    }

    /**
     * get outer bounds.
     * @param r new outer bound.
     */
    public void getOuterBounds(RectF r) {
        mBoundedRect.setToOuter(r);
    }

    public RectF getInnerBounds() {
        return mBoundedRect.getInner();
    }

    public RectF getOuterBounds() {
        return mBoundedRect.getOuter();
    }

    public int getSelectState() {
        return mMovingEdges;
    }

    public boolean isFixedAspect() {
        return mFixAspectRatio;
    }

    /**
     * Rotate outer angle.
     * @param angle new angle.
     */
    public void rotateOuter(int angle) {
        mRotation = angle % OverLayController.ORITATION_360;
        mBoundedRect.setRotation(mRotation);
        clearSelectState();
    }

    /**
     * @param width Width of bitmap.
     * @param height Height of bitmap.
     * @return whether success to set aspect ratio.
     */
    public boolean setInnerAspectRatio(float width, float height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and Height must be greater than zero");
        }
        RectF inner = mOriginalInnerRect == null ? mBoundedRect.getInner() : new RectF(
                mOriginalInnerRect);

        OverlayerMath.fixAspectRatioContained(inner, width, height);
        if (inner.width() < mMinSideSize || inner.height() < mMinSideSize) {
            return false;
        }
        mFixAspectRatio = true;
        mBoundedRect.setInner(inner);
        clearSelectState();
        return true;
    }

    /**
     * Set touch position tolerance.
     * @param tolerance the tolerance of touch position.
     */
    public void setTouchTolerance(float tolerance) {
        if (tolerance <= 0) {
            throw new IllegalArgumentException("Tolerance must be greater than zero");
        }
        mTouchTolerance = tolerance;
    }

    /**
     * set min inner side size.
     * @param minSide the min side size.
     */
    public void setMinInnerSideSize(float minSide) {
        if (minSide <= 0) {
            throw new IllegalArgumentException("Min dide must be greater than zero");
        }
        mMinSideSize = minSide;
    }

    /**
     * clear select state.
     */
    public void unsetAspectRatio() {
        mFixAspectRatio = false;
        clearSelectState();
    }

    /**
     * clear select state and set mFixAspectRatio as true.
     */
    public void setAspectRatio() {
        mFixAspectRatio = true;
        clearSelectState();
    }

    /**
     * get current station.
     * @return whether in MOVE state.
     */
    public boolean hasSelectedEdge() {
        return mMovingEdges != MOVE_NONE;
    }

    /**
     * Check corner.
     * @param selected the current selected state.
     * @return whether select 4 corners.
     */
    public static boolean checkCorner(int selected) {
        return selected == TOP_LEFT || selected == TOP_RIGHT || selected == BOTTOM_RIGHT
                || selected == BOTTOM_LEFT;
    }

    /**
     * Check edge.
     * @param selected the current selected state.
     * @return whether select 4 edges.
     */
    public static boolean checkEdge(int selected) {
        return selected == MOVE_LEFT || selected == MOVE_TOP || selected == MOVE_RIGHT
                || selected == MOVE_BOTTOM;
    }

    /**
     * Check block.
     * @param selected the current selected state.
     * @return  whether in MOVE_BLOCK state.
     */
    public static boolean checkBlock(int selected) {
        return selected == MOVE_BLOCK;
    }

    /**
     * Check Valid.
     * @param selected  the current selected state.
     * @return whether in Valid state.
     */
    public static boolean checkValid(int selected) {
        return selected == MOVE_NONE || checkBlock(selected) || checkEdge(selected)
                || checkCorner(selected);
    }

    /**
     * Clear select state.
     */
    public void clearSelectState() {
        mMovingEdges = MOVE_NONE;
    }

    /**
     * Would select edge.
     * @param x X position.
     * @param y Y position.
     * @return Current state.
     */
    public int wouldSelectEdge(float x, float y) {
        int edgeSelected = calculateSelectedEdge(x, y);
        if (edgeSelected != MOVE_NONE && edgeSelected != MOVE_BLOCK) {
            return edgeSelected;
        }
        return MOVE_NONE;
    }

    /**
     * Check and set current edge.
     * @param edge Current edge.
     * @return whether in valid state.
     */
    public boolean selectEdge(int edge) {
        if (!checkValid(edge)) {
            // temporary
            throw new IllegalArgumentException("bad edge selected");
            // return false;
        }
        if ((mFixAspectRatio && !checkCorner(edge)) && !checkBlock(edge) && edge != MOVE_NONE) {
            // temporary
            //throw new IllegalArgumentException("bad corner selected");
            // return false;
        }
        mMovingEdges = edge;
        return true;
    }

    /**
     * set select edge.
     * @param x Current X position.
     * @param y Current Y position.
     * @return whether in valid state.
     */
    public boolean selectEdge(float x, float y) {
        int edgeSelected = calculateSelectedEdge(x, y);
        if (mFixAspectRatio) {
            //edgeSelected = fixEdgeToCorner(edgeSelected);
        }
        if (edgeSelected == MOVE_NONE) {
            return false;
        }
        return selectEdge(edgeSelected);
    }

    /**
     * Check whether the points is in inner bound.
     * @param x the x points.
     * @param y the y points.
     * @return whether in bounds.
     */
    public boolean inBounds(float x, float y) {
        RectF cropped = mBoundedRect.getInner();
        if (x >= cropped.left && x < cropped.right && y >= cropped.top && y <= cropped.bottom) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param dX the slide distance of bound X coordinate.
     * @param dY the slide distance of bound Y coordinate.
     * @param screenDX the slide distance of screen X coordinate
     * @param screenDY the slide distance of screen Y coordinate.
     * @return whether validate state.
     */
    public boolean moveCurrentSelection(float dX, float dY, float screenDX, float screenDY) {
        if (mMovingEdges == MOVE_NONE) {
            return false;
        }
        RectF crop = mBoundedRect.getInner();
        float minWidthHeight = mMinSideSize;
        int movingEdges = mMovingEdges;
        if (movingEdges == MOVE_BLOCK) {
            mBoundedRect.moveInner(screenDX, screenDY);
            return true;
        } else {
            float dx = 0;
            float dy = 0;

            if ((movingEdges & MOVE_LEFT) != 0) {
                dx = Math.min(crop.left + dX, crop.right - minWidthHeight) - crop.left;
            }
            if ((movingEdges & MOVE_TOP) != 0) {
                dy = Math.min(crop.top + dY, crop.bottom - minWidthHeight) - crop.top;
            }
            if ((movingEdges & MOVE_RIGHT) != 0) {
                dx = Math.max(crop.right + dX, crop.left + minWidthHeight) - crop.right;
            }
            if ((movingEdges & MOVE_BOTTOM) != 0) {
                dy = Math.max(crop.bottom + dY, crop.top + minWidthHeight) - crop.bottom;
            }

            if (mFixAspectRatio) {
                float[] l1 = { crop.left, crop.bottom };
                float[] l2 = { crop.right, crop.top };
                if (movingEdges == TOP_LEFT || movingEdges == BOTTOM_RIGHT) {
                    l1[1] = crop.top;
                    l2[1] = crop.bottom;
                }
                float[] b = { l1[0] - l2[0], l1[1] - l2[1] };
                float[] disp = { dx, dy };
                float[] bUnit = normalize(b);
                float sp = scalarProjection(disp, bUnit);
                dx = sp * bUnit[0];
                dy = sp * bUnit[1];
                RectF newCrop = fixedCornerResize(crop, movingEdges, dx, dy);
                mBoundedRect.fixedAspectResizeInner(newCrop);
            } else {
                if ((movingEdges & MOVE_LEFT) != 0) {
                    crop.left += dx;
                }
                if ((movingEdges & MOVE_TOP) != 0) {
                    crop.top += dy;
                }
                if ((movingEdges & MOVE_RIGHT) != 0) {
                    crop.right += dx;
                }
                if ((movingEdges & MOVE_BOTTOM) != 0) {
                    crop.bottom += dy;
                }
                mBoundedRect.resizeInner(crop);
            }
        }
        return true;
    }

    private float scalarProjection(float[] a, float[] b) {
        float length = (float) Math.sqrt(b[0] * b[0] + b[1] * b[1]);
        return dotProduct(a, b) / length;
    }

    private float dotProduct(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1];
    }

    private float[] normalize(float[] a) {
        float length = (float) Math.sqrt(a[0] * a[0] + a[1] * a[1]);
        float[] b = { a[0] / length, a[1] / length };
        return b;
    }

    public void setOriginalInnerRect(RectF inner) {
        mOriginalInnerRect = new RectF(inner);
    }

    public RectF getOriginalInnerRect() {
        return mOriginalInnerRect;
    }

    public float getMinSideSize() {
        return mMinSideSize;
    }

/*    private int fixEdgeToCorner(int moving_edges) {
        if (moving_edges == MOVE_LEFT) {
            moving_edges |= MOVE_TOP;
        }
        if (moving_edges == MOVE_TOP) {
            moving_edges |= MOVE_LEFT;
        }
        if (moving_edges == MOVE_RIGHT) {
            moving_edges |= MOVE_BOTTOM;
        }
        if (moving_edges == MOVE_BOTTOM) {
            moving_edges |= MOVE_RIGHT;
        }
        return moving_edges;
    }*/

    // Helper methods
    private int calculateSelectedEdge(float x, float y) {
        RectF cropped = mBoundedRect.getInner();

        float left = Math.abs(x - cropped.left);
        float right = Math.abs(x - cropped.right);
        float top = Math.abs(y - cropped.top);
        float bottom = Math.abs(y - cropped.bottom);

        int edgeSelected = MOVE_NONE;
        if (left <= mTouchTolerance) {
            edgeSelected |= MOVE_LEFT;
            if (top <= mTouchTolerance) {
                edgeSelected |= MOVE_TOP;
            } else if (bottom <= mTouchTolerance) {
                edgeSelected |= MOVE_BOTTOM;
            } else {
                edgeSelected = MOVE_NONE;
            }
        } else if (right <= mTouchTolerance) {
            edgeSelected |= MOVE_RIGHT;
            if (top <= mTouchTolerance) {
                edgeSelected |= MOVE_TOP;
            } else if (bottom <= mTouchTolerance) {
                edgeSelected |= MOVE_BOTTOM;
            } else {
                edgeSelected = MOVE_NONE;
            }
        }
        return edgeSelected;
    }

    private static RectF fixedCornerResize(RectF r, int movingCorner, float dx, float dy) {
        RectF newCrop = null;
        // Fix opposite corner in place and move sides
        if (movingCorner == BOTTOM_RIGHT) {
            newCrop = new RectF(r.left - dx, r.top - dy, r.left + r.width() + dx, r.top
                    + r.height() + dy);
        } else if (movingCorner == BOTTOM_LEFT) {
            newCrop = new RectF(r.right - r.width() + dx, r.top - dy, r.right - dx, r.top
                    + r.height() + dy);
        } else if (movingCorner == TOP_LEFT) {
            newCrop = new RectF(r.right - r.width() + dx, r.bottom - r.height() + dy, r.right - dx,
                    r.bottom - dy);
        } else if (movingCorner == TOP_RIGHT) {
            newCrop = new RectF(r.left - dx, r.bottom - r.height() + dy, r.left + r.width() + dx,
                    r.bottom - dy);
        }
        return newCrop;
    }
}
