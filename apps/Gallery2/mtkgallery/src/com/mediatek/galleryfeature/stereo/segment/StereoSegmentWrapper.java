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

package com.mediatek.galleryfeature.stereo.segment;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;

import com.mediatek.gallery3d.util.TraceHelper;
import com.mediatek.galleryfeature.stereo.SegmentJni;
import com.mediatek.galleryframework.util.MtkLog;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This wrapper class utilizes SegmentJNI to do segment work for segment app.<br/>
 * It would process data coming from app side before pass it to SegmentJNI and
 * also do necessary post-process to data that is from SegmentJNI. In addition,
 * StereoSegmentWrapper maintains an internal structure to handle frequent
 * segment requests.<br/>
 * We must follow the steps below to use this class:<br/>
 * new StereoSegmentWrapper() -> initSegment() -> segment operations such as
 * setMaskCenter(), addSelection(), delSelection(), etc. -> release().<br/>
 * Now multiple segment instances usage is not permitted. We should release one
 * StereoSegmentWrapper before we create a new one.
 */
public class StereoSegmentWrapper {
    private static final String TAG = "MtkGallery2/SegmentApp/StereoSegmentWrapper";

    private static final int MAX_SEGMENT_STEP = 5;
    private static final int REFINE_BACKGROUND_COLOR = 0xff808080;
    private static final int REFINE_ADD_PATH_COLOR = 0xffffffff;
    private static final int REFINE_DELETE_PATH_COLOR = 0xff000000;
    private static final int MIN_SCRIBBLE_LEN = 10;
    private static final int REFINE_STROKE_WIDTH = 10;
    private static final int MSG_SELECTION = 1;

    private final SegmentJni mSegment;
    private final MySecretary mSecretary;
    private Bitmap mCachedMask;
    private final int mOriginalWidth;
    private final int mOriginalHeight;
    private Bitmap mInitBitmap;
    private String mInitFilePath;
    private int mInBitmapWidth;
    private int mInBitmapHeight;
    private Bitmap mRefineBitmap;
    private RectF mRefineAABBBox;
    private Path mRefinePath;
    private boolean mIsRefinePathConstructing;
    private int mRefineSteps;
    private DoSegmentInfo mLatestDoSegmentInfo;
    private final ISegmentOperationListener mSegmentOperationListener;
    private final Set<IRedrawListener> mOutterRedrawListeners;
    private final Handler mMainHandler;

    /**
     * A listener which will be activated when a segment operation impacting the
     * image show happens.
     */
    public interface IRedrawListener {
        /**
         * Callback when a segment operation done and image show should be
         * refreshed.
         *
         * @param result
         *            has error occured
         */
        void onRedraw(boolean result);
    }

    /**
     * Segment operation info.
     */
    private class DoSegmentInfo {
        int mScenario;
        int mMode;
        Bitmap mScribble;
        Rect mRoiRect;
        Point mPoint;

        public DoSegmentInfo() {
        }

        public DoSegmentInfo(DoSegmentInfo info) {
            mScenario = info.mScenario;
            mMode = info.mMode;
            mScribble = info.mScribble;
            mRoiRect = info.mRoiRect;
            mPoint = info.mPoint;
        }
    }

    /**
     * Segment operation Listener.
     */
    private interface ISegmentOperationListener {
        void onSegmentOperationDone(int scenario, boolean result);
    }

    /**
     * Worker thread to handle segment request.
     */
    private class MySecretary {
        private static final int STATUS_UNSTARTED = 0;
        private static final int STATUS_RUNNING = 1;
        private static final int STATUS_SHUTDOWN = 2;

        private int mStatus = STATUS_UNSTARTED;
        private ExecutorService mThreadPool = Executors.newSingleThreadExecutor();

        private volatile Bitmap mMaskBitmap;

        /**
         * Segment operation encapsulation.
         */
        private class DoSegment implements Callable<Boolean> {
            private final DoSegmentInfo mDoSegmentInfo;
            private final ISegmentOperationListener mDoSegmentListener;

            public DoSegment(DoSegmentInfo info, ISegmentOperationListener listener) {
                mDoSegmentInfo = info;
                mDoSegmentListener = listener;
            }

            @Override
            public Boolean call() throws Exception {
                MtkLog.d(TAG, "<DoSegment_call> segment.doSegment() start");
                TraceHelper.traceBegin(">>>>SegmentJni-doSegment");
                boolean result = mSegment.doSegment(mDoSegmentInfo.mScenario, mDoSegmentInfo.mMode,
                        mDoSegmentInfo.mScribble, mDoSegmentInfo.mRoiRect, mDoSegmentInfo.mPoint);
                TraceHelper.traceEnd();
                MtkLog.d(TAG, "<DoSegment_call> segment.doSegment() end");
                if (result) {
                    invalidateMaskBitmap();
                }
                if (mDoSegmentListener != null) {
                    mDoSegmentListener.onSegmentOperationDone(mDoSegmentInfo.mScenario, result);
                }
                return result;
            }
        }

        public boolean initSegment(final String sourceFilePath, final Bitmap bitmap,
                final int orientation) {
            if (mStatus != STATUS_UNSTARTED) {
                return true;
            }
            mStatus = STATUS_RUNNING;

            Callable<Boolean> initOp = new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    MtkLog.d(TAG, "<initSegment> initSegment() start");
                    TraceHelper.traceBegin(">>>>SegmentJni-initSegment");
                    boolean isSegmentValid = mSegment.initSegment(sourceFilePath, bitmap, 0);
                    TraceHelper.traceEnd();
                    MtkLog.d(TAG, "<initSegment> initSegment() end");

                    if (isSegmentValid) {
                        MtkLog.d(TAG, "<initSegment> generateSegmentMask() start");
                        TraceHelper.traceBegin(">>>>SegmentJni-doSegment-autoPick");
                        isSegmentValid = mSegment.doSegment(SegmentJni.SCENARIO_AUTO,
                                SegmentJni.MODE_OBJECT, null, null, null);
                        TraceHelper.traceEnd();
                        MtkLog.d(TAG, "<initSegment> generateSegmentMask() end");
                        if (isSegmentValid) {
                            invalidateMaskBitmap();
                        }
                    }

                    return isSegmentValid;
                }
            };

            Future<Boolean> future = mThreadPool.submit(initOp);
            try {
                return future.get();
            } catch (InterruptedException e) {
                return false;
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        public boolean doSegmentAndWait(DoSegmentInfo doSegmentInfo) {
            if (mStatus != STATUS_RUNNING) {
                return true;
            }

            DoSegment segmentOp = new DoSegment(doSegmentInfo, null);
            Future<Boolean> future = mThreadPool.submit(segmentOp);
            try {
                return future.get();
            } catch (InterruptedException e) {
                return false;
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        public void doSegmentAsync(DoSegmentInfo doSegmentInfo,
                ISegmentOperationListener listener) {
            if (mStatus != STATUS_RUNNING) {
                return;
            }

            DoSegment segmentOp = new DoSegment(doSegmentInfo, listener);
            mThreadPool.submit(segmentOp);
        }

        public void releaseSegment() {
            if (mStatus != STATUS_RUNNING) {
                return;
            }
            mStatus = STATUS_SHUTDOWN;

            Callable<Void> releaseOp = new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    MtkLog.d(TAG, "<releaseSegment> segment.release() start");
                    TraceHelper.traceBegin(">>>>SegmentJni-release");
                    mSegment.release();
                    TraceHelper.traceEnd();
                    MtkLog.d(TAG, "<releaseSegment> segment.release() end");
                    return null;
                }
            };
            Future<Void> future = mThreadPool.submit(releaseOp);
            mThreadPool.shutdown();
            try {
                future.get();
            } catch (InterruptedException e) {
                return;
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        public Bitmap getMaskBitmap() {
            if (mStatus != STATUS_RUNNING) {
                return null;
            }

            return mMaskBitmap;
        }

        public boolean undoSegment() {
            if (mStatus != STATUS_RUNNING) {
                return true;
            }

            Callable<Boolean> op = new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    TraceHelper.traceBegin(">>>>SegmentJni-undoSegment");
                    boolean result = mSegment.undoSegment();
                    TraceHelper.traceEnd();
                    if (result) {
                        invalidateMaskBitmap();
                    }
                    return result;
                }
            };

            Future<Boolean> future = mThreadPool.submit(op);
            try {
                return future.get();
            } catch (InterruptedException e) {
                return false;
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        private void invalidateMaskBitmap() {
            MtkLog.d(TAG, "<invalidateMaskBitmap> mSegment.getCoverBitmap() start");
            TraceHelper.traceBegin(">>>>SegmentJni-getMaskBitmap");
            mMaskBitmap = mSegment.getCoverBitmap();
            TraceHelper.traceEnd();
            MtkLog.d(TAG, "<invalidateMaskBitmap> mSegment.getCoverBitmap() end");
        }
    }

    /**
     * Constructor.
     *
     * @param width
     *            original width of the image to segment.
     * @param height
     *            original height of the image to segment.
     * @param widthSmall
     *            never used. TODO remove.
     * @param heightSmall
     *            never used. TODO remove.
     */
    public StereoSegmentWrapper(int width, int height, int widthSmall, int heightSmall) {
        MtkLog.d(TAG, "<StereoSegmentWrapper> Segment() start");
        TraceHelper.traceBegin(">>>>SegmentJni-constructor");
        mSegment = new SegmentJni();
        TraceHelper.traceEnd();
        MtkLog.d(TAG, "<StereoSegmentWrapper> Segment() end");
        mSecretary = new MySecretary();
        mMainHandler = new Handler() {
            public void handleMessage(Message message) {
                switch (message.what) {
                case MSG_SELECTION:
                    return;
                default:
                    throw new AssertionError();
                }
            }
        };
        mSegmentOperationListener = new ISegmentOperationListener() {
            @Override
            public void onSegmentOperationDone(final int scenario, final boolean result) {
                // in UI thread
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!result) {
                            mLatestDoSegmentInfo = null;
                            notifyOutterListeners(false);
                            return;
                        }

                        if (scenario == SegmentJni.SCENARIO_SELECTION
                                || scenario == SegmentJni.SCENARIO_AUTO) {
                            mRefineSteps = 1;
                            if (mLatestDoSegmentInfo != null) {
                                if (mLatestDoSegmentInfo.mPoint != null) {
                                    DoSegmentInfo info = new DoSegmentInfo(mLatestDoSegmentInfo);
                                    mLatestDoSegmentInfo.mPoint = null;
                                    mSecretary.doSegmentAsync(info, mSegmentOperationListener);
                                } else {
                                    mLatestDoSegmentInfo = null;
                                }
                            }
                        } else if (mRefineSteps < MAX_SEGMENT_STEP) {
                            mRefineSteps++;
                        }

                        notifyOutterListeners(true);
                    }
                });
            }
        };
        mOutterRedrawListeners = new HashSet<IRedrawListener>();
        mOriginalWidth = width;
        mOriginalHeight = height;
    }

    /**
     * Add ReDrawListener.
     *
     * @param listener
     *            the listener to add.
     * @return true if sccessfully added.
     */
    public boolean addReDrawListener(IRedrawListener listener) {
        return mOutterRedrawListeners.add(listener);
    }

    /**
     * Remove ReDrawListener.
     *
     * @param listener
     *            the listener to remove.
     * @return true if sccessfully removed.
     */
    public boolean removeRedrawListener(IRedrawListener listener) {
        return mOutterRedrawListeners.remove(listener);
    }

    /**
     * Do init work.
     *
     * @param filePath
     *            file path of image to segment.
     * @param inBitmap
     *            a downsampled bitmap decoded from the file.
     * @return true if no error occured.
     */
    public boolean initSegment(String filePath, Bitmap inBitmap) {
        MtkLog.d(TAG, "<initSegment> filePath = " + filePath + ",width:" + inBitmap.getWidth()
                + ",height:" + inBitmap.getHeight());
        boolean isSegmentValid = true;

        mInBitmapWidth = inBitmap.getWidth();
        mInBitmapHeight = inBitmap.getHeight();
        isSegmentValid = mSecretary.initSegment(filePath, inBitmap, 0);
        mRefineSteps = 1;

        mInitBitmap = inBitmap;
        mInitFilePath = filePath;
        mRefineBitmap = Bitmap.createBitmap(mInBitmapWidth, mInBitmapHeight,
                Bitmap.Config.ARGB_8888);
        return isSegmentValid;
    }

    /**
     * Get segment point coordinate (coordinate of the point to select
     * foreground object).<br/>
     * The point coordinate is given by auto segment or setMaskCenter().
     *
     * @return segment point coordinate.
     */
    public float[] getSegmentPoint() {
        float result[] = new float[2];
        TraceHelper.traceBegin(">>>>SegmentJni-getSegmentPoint");
        Point point = mSegment.getSegmentPoint();
        TraceHelper.traceEnd();

        result[0] = (float) point.x * mOriginalWidth / mInBitmapWidth;
        result[1] = (float) point.y * mOriginalHeight / mInBitmapHeight;
        return result;
    }

    /**
     * Begin to construct a new refine path from a given point on the image.
     *
     * @param x
     *            x coordinate of the point.
     * @param y
     *            y coordinate of the point.
     */
    public void startNewRefine(float x, float y) {
        x = x * mInBitmapWidth / mOriginalWidth;
        y = y * mInBitmapHeight / mOriginalHeight;
        mRefineAABBBox = new RectF(x, y, x, y);
        mRefinePath = new Path();
        mRefinePath.moveTo(x, y);

        mIsRefinePathConstructing = true;
    }

    /**
     * Add a point to the current refine path.
     *
     * @param x
     *            x coordinate of the point.
     * @param y
     *            y coordinate of the point.
     */
    public void addRefinePoint(float x, float y) {
        x = x * mInBitmapWidth / mOriginalWidth;
        y = y * mInBitmapHeight / mOriginalHeight;
        mRefineAABBBox.union(x, y);
        mRefinePath.lineTo(x, y);
    }

    /**
     * End constructing a refine path with a given point the path end.
     *
     * @param x
     *            x coordinate of the point.
     * @param y
     *            y coordinate of the point.
     */
    public void endRefine(float x, float y) {
        addRefinePoint(x, y);
        mIsRefinePathConstructing = false;
    }

    /**
     * Commit the current refine path to add selection to the foreground object.<br/>
     * The information of the selection is implied in the refine path and the
     * final selection area is decided by algorithm.
     */
    public void addSelection() {
        Rect roiRect = getRoiRect();
        if (!isScribbleValid(roiRect)) {
            return;
        }
        regenerateRefinePath(REFINE_BACKGROUND_COLOR, REFINE_ADD_PATH_COLOR);
        DoSegmentInfo info = new DoSegmentInfo();
        info.mScenario = SegmentJni.SCENARIO_SCRIBBLE_FG;
        info.mMode = 0;
        info.mScribble = mRefineBitmap;
        info.mRoiRect = roiRect;
        info.mPoint = null;
        mSecretary.doSegmentAsync(info, mSegmentOperationListener);
    }

    /**
     * Commit the current refine path to delete selection to the foreground object.<br/>
     * The information of the selection is implied in the refine path and the
     * final selection area is decided by algorithm.
     */
    public void delSelection() {
        Rect roiRect = getRoiRect();
        if (!isScribbleValid(roiRect)) {
            return;
        }
        regenerateRefinePath(REFINE_BACKGROUND_COLOR, REFINE_DELETE_PATH_COLOR);
        DoSegmentInfo info = new DoSegmentInfo();
        info.mScenario = SegmentJni.SCENARIO_SCRIBBLE_BG;
        info.mMode = 0;
        info.mScribble = mRefineBitmap;
        info.mRoiRect = roiRect;
        info.mPoint = null;
        mSecretary.doSegmentAsync(info, mSegmentOperationListener);
    }

    /**
     * Select foreground object by a given coordinate on the image.<br/>
     * The final object is decided by algorithm.
     *
     * @param x
     *            x coordinate of the point.
     * @param y
     *            y coordinate of the point.
     */
    public void setMaskCenter(int x, int y) {
        x = x * mInBitmapWidth / mOriginalWidth;
        y = y * mInBitmapHeight / mOriginalHeight;
        DoSegmentInfo info = new DoSegmentInfo();
        info.mScenario = SegmentJni.SCENARIO_SELECTION;
        info.mMode = 0;
        info.mScribble = null;
        info.mRoiRect = null;
        info.mPoint = new Point(x, y);
        if (mLatestDoSegmentInfo == null) {
            mLatestDoSegmentInfo = new DoSegmentInfo(info);
            mLatestDoSegmentInfo.mPoint = null;
            mSecretary.doSegmentAsync(info, mSegmentOperationListener);
        } else {
            mLatestDoSegmentInfo = info;
        }
    }

    /**
     * Auto select foreground object.<br/>
     * The final object is decided by algorithm (and may be read from xmp).
     */
    public void autoSegment() {
        DoSegmentInfo info = new DoSegmentInfo();
        info.mScenario = SegmentJni.SCENARIO_AUTO;
        info.mMode = 0;
        info.mScribble = null;
        info.mRoiRect = null;
        info.mPoint = null;
        if (mLatestDoSegmentInfo == null) {
            mLatestDoSegmentInfo = new DoSegmentInfo(info);
            mLatestDoSegmentInfo.mPoint = null;
            mSecretary.doSegmentAsync(info, mSegmentOperationListener);
        } else {
            mLatestDoSegmentInfo = info;
        }
    }

    /**
     * Get mask bitmap. A mask bitmap represents the background area on the
     * bitmap.
     *
     * @return mask bitmap.
     */
    public Bitmap getMaskBitmap() {
        Bitmap bitmap = mSecretary.getMaskBitmap();
        if (bitmap != mCachedMask) {
            if (mCachedMask != null) {
                mCachedMask.recycle();
            }
            mCachedMask = bitmap;
        }
        return mCachedMask;
    }

    /**
     * Get Axis-Aligned Bounding Box of the foreground object.
     *
     * @return the AABB box of the foreground object on the bitmap.
     */
    public Rect getClippingBox() {
        TraceHelper.traceBegin(">>>>SegmentJni-getSegmentRect");
        Rect rect = mSegment.getSegmentRect();
        TraceHelper.traceEnd();
        return rect;
    }

    /**
     * Return the bitmap of the foreground object.
     *
     * @param oriBitmap
     *            no use. TODO remove this.
     * @return bitmap of the foreground object.
     */
    public Bitmap getForground(Bitmap oriBitmap) {
        Bitmap bitmap;
        if ((oriBitmap == null) || (oriBitmap == mInitBitmap)) {
            TraceHelper.traceBegin(">>>>SegmentJni-getSegmentBitmap");
            bitmap = mSegment.getSegmentBitmap(mInitBitmap);
            TraceHelper.traceEnd();
        } else {
            TraceHelper.traceBegin(">>>>SegmentJni-setNewBitmap");
            mSegment.setNewBitmap(oriBitmap, null, 0, 0);
            TraceHelper.traceEnd();
            TraceHelper.traceBegin(">>>>SegmentJni-getNewSegmentBitmap");
            bitmap = mSegment.getNewSegmentBitmap(oriBitmap);
            TraceHelper.traceEnd();
        }
        return bitmap;
    }

    /**
     * Undo the last refine operation.<br/>
     * Now we only keep record for 5 latest segment operations. So we can at
     * most undo for 4 coutinuous times (cause 1 record is for the selection
     * operation).
     *
     * @return whether further undo can be done (whether we have more refine
     *         operation records).
     */
    public boolean undo() {
        MtkLog.d(TAG, "<undo> mRefineSteps = " + mRefineSteps);
        if (mRefineSteps > 1) {
            mRefineSteps--;
            mSecretary.undoSegment();
            if (mRefineSteps == 1) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Return whether further undo can be done (whether we have more refine
     * operation records).
     *
     * @return true if further undo can be done.
     */
    public boolean isUndoEnabled() {
        return (mRefineSteps > 1);
    }

    /**
     * Serialize the refine result into the original stereo image file. We can
     * reuse this information the next time we begin to segment the same photo
     * by aoto segment.
     */
    public void saveMaskToXmp() {
        TraceHelper.traceBegin(">>>>SegmentJni-saveMaskToXmp");
        mSegment.saveMaskToXmp();
        TraceHelper.traceEnd();
    }

    /**
     * Do de-init work.
     */
    public void release() {
        mSecretary.releaseSegment();
        mRefineBitmap.recycle();
    }

    /**
     * Get the current refine path. The current refine path is valid only if it
     * is in constructing (never commited to algorithm).
     *
     * @return the current refine path if it is in constructing, otherwise null.
     */
    public Path getRefinePath() {
        return mIsRefinePathConstructing ? mRefinePath : null;
    }

    private Rect getRoiRect() {
        RectF pathAABBBox = mRefineAABBBox;
        int left = Math.max((int) (pathAABBBox.left) - 1, 0);
        int right = Math.min((int) (pathAABBBox.right) + 1, mInBitmapWidth - 1);
        int top = Math.max((int) (pathAABBBox.top) - 1, 0);
        int bottom = Math.min((int) (pathAABBBox.bottom) + 1, mInBitmapHeight - 1);
        Rect roiRect = new Rect(left, top, right, bottom);
        MtkLog.d(TAG, "<getRoiRect> rotRect(" + left + ", " + top + " - " + right + ", " + bottom
                + ")");
        return roiRect;
    }

    private void regenerateRefinePath(int bgColor, int pathColor) {
        TraceHelper.traceBegin(">>>>StereoSegmentWrapper-regenerateRefinePath");
        Path path = mRefinePath;
        Canvas canvas = new Canvas(mRefineBitmap);
        canvas.drawColor(bgColor);
        Paint paint = new Paint();
        paint.setColor(pathColor);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(REFINE_STROKE_WIDTH);
        canvas.drawPath(path, paint);
        TraceHelper.traceEnd();
    }

    private void notifyOutterListeners(boolean result) {
        for (IRedrawListener listener : mOutterRedrawListeners) {
            listener.onRedraw(result);
        }
    }

    private boolean isScribbleValid(Rect roiRect) {
        if (roiRect.width() < MIN_SCRIBBLE_LEN && roiRect.height() < MIN_SCRIBBLE_LEN) {
            MtkLog
                    .d(TAG, "<addSelection> width:" + roiRect.width() + ",height:"
                            + roiRect.height());
            notifyOutterListeners(true);
            return false;
        }
        if (!RectF.intersects(mRefineAABBBox, new RectF(0, 0, mRefineBitmap.getWidth(),
                mRefineBitmap.getHeight()))) {
            return false;
        }

        return true;
    }
}
