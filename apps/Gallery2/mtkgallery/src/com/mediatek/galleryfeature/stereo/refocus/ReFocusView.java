package com.mediatek.galleryfeature.stereo.refocus;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;

import com.android.gallery3d.R;

import com.mediatek.galleryframework.util.MtkLog;
import com.mediatek.ngin3d.Dimension;
import com.mediatek.ngin3d.Image;
import com.mediatek.ngin3d.Point;
import com.mediatek.ngin3d.Scale;
import com.mediatek.ngin3d.Stage;
import com.mediatek.ngin3d.android.StageView;

import java.io.File;

import javax.microedition.khronos.opengles.GL10;

/**
 * Stereo refocus view.
 */
@SuppressLint("SdCardPath")
public class ReFocusView extends StageView {
    private static final String TAG = "mtkGallery2/Refocus/ReFocusView";
    private static final float DEFAULT_LOCATION_X = 0.5f;
    private static final float DEFAULT_LOCATION_Y = 0.5f;
    private static final float DEFAULT_LOCATION_DEPTH_X = 0.1f;
    private static final float DEFAULT_LOCATION_DEPTH_Y = 0.2f;
    private static final float TIME_TOTAL_DURATION = 600.0f;
    private static final float TIME_FIRST_DURATION = 300.0f;
    private static final float TIME_FADEIN_DURATION = 300.0f;
    private static final float TIME_BACK_TOTAL_DURATION = 250.0f;
    private static final float BACK_TARGET_X = 0.5f;
    private static final float BACK_TARGET_Y = 0.5f;
    private static final float DOWNR_X = 0.5f;
    private static final float DOWNR_Y = 0.5f;
    private static final float THRESHOLD_MAX_SCALE = 4.0f;
    private static final int TOUCH_MODE_NOTHING = 0;
    private static final int TOUCH_MODE_ONE_FINGER_SCROLL = 0x00000001;
    private static final int TOUCH_MODE_TWO_FINGER_SCROLL = 0x00000002;
    private static final int TOUCH_MODE_TRANSITION_EFFECT = 0x00000004;
    private static final int TOUCH_MODE_BACK_EFFECT = 0x00000008;

    private int mTouchMode = TOUCH_MODE_NOTHING;
    private static int sTotalNewCount = 0;
    private int mViewID;

    private final Context mContext;
    private int mWidth = 1;
    private int mHeight = 1;
    private float mDownX;
    private float mDownY;

    private float mLocationX = DEFAULT_LOCATION_X;
    private float mLocationY = DEFAULT_LOCATION_Y;
    private GestureDetectorCompat mGestureDetector;

    private MyScaleGestureListener mZoomGestureListener;
    private ScaleGestureDetector mZoomGestureDetector;
    private float mScaleFit;
    private float mScaleCurrent;

    private float mXOffset;
    private float mYOffset;
    private Bitmap mBitmapNew;
    private Bitmap mBitmap;
    private Image mImageActor;
    private Image mFocusActor;

    private Image mDepthActor;
    private float mDepthLocationX = DEFAULT_LOCATION_DEPTH_X;
    private float mDepthLocationY = DEFAULT_LOCATION_DEPTH_Y;
    private RefocusListener mRefocusListener;

    private boolean mIsShowImageIsTransitionPlaying;
    private boolean mIsShowImageIsGetFirstFrameTime;
    private long mShowImageFirstFrameTime;
    private float mShowImageTotalDurationTime = TIME_TOTAL_DURATION;
    private float mShowImageFirstDurationTime = TIME_FIRST_DURATION;

    private boolean mStageFadeInIsGetFirstFrameTime;
    private long mStageFadeInFirstFrameTime;
    private boolean mStageFadeInIsFadeInDone;
    private final float mStageFadeInDurationTime = TIME_FADEIN_DURATION;

    private boolean mBackIsTransitionPlaying = false;
    private boolean mBackIsGetFirstFrameTime = false;
    private long mBackFirstFrameTime;
    private float mBackTotalDurationTime = TIME_BACK_TOTAL_DURATION;
    private float mBackLocationX;
    private float mBackLocationY;
    private float mBackScaleCurrent;
    private boolean mBackIsScaleBack = false;
    private float mBackTargetX = BACK_TARGET_X;
    private float mBackTargetY = BACK_TARGET_Y;

    /**
     *
     * callback interface when onTouch.
     *
     */
    public interface RefocusListener {
        /**
         * callback location.
         *
         * @param x
         *            x coordinate
         * @param y
         *            y coordinate
         */
        public void setRefocusImage(float x, float y);
    }

    /**
     * constructor.
     * @param context activity context
     */
    public ReFocusView(Context context) {
        this(context, null);
    }

    /**
     * constructor.
     *
     * @param context
     *            context
     * @param attrs
     *            attrs
     */
    public ReFocusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mViewID = sTotalNewCount;
        sTotalNewCount++;
        logD("new " + this.getClass().getSimpleName() + "()");
        setup();

        mZoomGestureListener = new MyScaleGestureListener();
        mZoomGestureDetector = new ScaleGestureDetector(mContext,
                mZoomGestureListener);
        mGestureDetector = new GestureDetectorCompat(mContext,
                mZoomGestureListener);
    }

    @Override
    protected synchronized void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        logW("onSizeChanged() " + w + "x" + h + " <= " + oldw + "x" + oldh);
        mWidth = w;
        mHeight = h;
        setImageActorViewSize(mImageActor, mBitmap, w, h);
    }

    @Override
    protected synchronized void onAttachedToWindow() {
        logI("onAttachedToWindow()");
        super.onAttachedToWindow();

        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
        if (mBitmapNew != null) {
            mBitmapNew.recycle();
            mBitmapNew = null;
        }
    }

    @Override
    protected synchronized void onDetachedFromWindow() {
        logI("onDetachedFromWIndow()");
        onPause();
        mStage.unrealize();
        mStage.removeAll();
        super.onDetachedFromWindow();
    }

    @Override
    public String toString() {
        return "[" + mViewID + "] " + super.toString();
    }

    /*@Override
    protected void finalize() throws Throwable {
        logD("~ " + this.getClass().getSimpleName() + "()");
        this.onDetachedFromWindow();
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
        if (mBitmapNew != null) {
            mBitmapNew.recycle();
            mBitmapNew = null;
        }
        super.finalize();
    }*/


    /**
      * Stereo refocus Gesture Listener.
      */
    public class MyScaleGestureListener extends
    GestureDetector.SimpleOnGestureListener implements
    OnScaleGestureListener {
        private float mScaleFactor;
        private float mStartScale;
        private float mEndScale;
        private boolean mIsScaling = false;

        private ReFocusView mTheView;
        private MotionEvent mEvent;

        private float mPreFocusX = 0;
        private float mPreFocusY = 0;
        private float mLocationXtmp = 0;
        private float mLocationYtmp = 0;
        // double tap is the first event than down, so we shall ignore the
        // following down
        private boolean mIsDoubleTap = false;

        /**
         * set the touch event.
         *
         * @param v
         *            refocus view
         * @param event
         *            motion event
         */
        public void setMotionEvent(ReFocusView v, MotionEvent event) {
            mEvent = event;
            mTheView = v;
        }

        /**
         * check is it scaling.
         *
         * @return scaling
         */
        public boolean isScaling() {
            return mIsScaling;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mEndScale = detector.getScaleFactor();

            // this condition does not pass. not > or < succeeds
            if (mStartScale > mEndScale) {
                logI("onScaleEnd() Pinch Dection");
            } else if (mStartScale < mEndScale) {
                logI("onScaleEnd() Zoom Dection");
            }
            mIsScaling = false;
            setTouchMode(getTouchMode()
                    & (~ReFocusView.TOUCH_MODE_TWO_FINGER_SCROLL));

            moveBackCheck();
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mStartScale = mScaleCurrent;
            logW("onScaleBegin() startScale:" + mStartScale);
            mIsScaling = true;
            mPreFocusX = detector.getFocusX();
            mPreFocusY = detector.getFocusY();
            mLocationXtmp = mLocationX;
            mLocationYtmp = mLocationY;
            setTouchMode(getTouchMode()
                    | ReFocusView.TOUCH_MODE_TWO_FINGER_SCROLL);
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if ((getTouchMode() & ReFocusView.TOUCH_MODE_TWO_FINGER_SCROLL) > 0) {
                mScaleFactor = detector.getScaleFactor();
                if ((mStartScale * mScaleFactor > mScaleFit)
                        && ((mStartScale * mScaleFactor) / mScaleFit <= THRESHOLD_MAX_SCALE)) {
                    mTheView.setImageActorViewSizeScale(mStartScale
                            * mScaleFactor);
                }

                float k = mScaleCurrent / mStartScale;
                final float x = mLocationXtmp * (float) mWidth - mPreFocusX;
                final float y = mLocationYtmp * (float) mHeight - mPreFocusY;

                mLocationX = ((detector.getFocusX() + (k * x)) / ((float) mWidth));
                mLocationY = ((detector.getFocusY() + (k * y)) / ((float) mHeight));

                if (mImageActor != null) {
                    mImageActor.setPosition(new Point(mLocationX, mLocationY, true));
                }
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent event) {
            logI("onDown() " + event.getX() + ", " + event.getY());
            if (event.getAction() == MotionEvent.ACTION_DOWN
                    && mIsDoubleTap == false) {
                moveBackStop();
            }
            mIsDoubleTap = false;
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                float distanceX, float distanceY) {
            if (mScaleCurrent <= mScaleFit) {
                return false;
            }
            if (getTouchMode() == TOUCH_MODE_NOTHING
                    || getTouchMode() == TOUCH_MODE_ONE_FINGER_SCROLL) {
                mLocationX = mLocationX - (distanceX / (float) mWidth);
                mLocationY = mLocationY - (distanceY / (float) mHeight);
                if (mImageActor != null) {
                    mImageActor.setPosition(new Point(mLocationX, mLocationY, true));
                }
                if (getTouchMode() == TOUCH_MODE_NOTHING) {
                    setTouchMode(getTouchMode() | TOUCH_MODE_ONE_FINGER_SCROLL);
                }
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            logW("onDoubleTap() " + e.getX() + ", " + e.getY() + ", c:"
                    + e.getPointerCount());
            mIsDoubleTap = true;

            mBackIsTransitionPlaying = true;
            mBackIsScaleBack = true;
            mBackTargetX = BACK_TARGET_X;
            mBackTargetY = BACK_TARGET_Y;
            setTouchMode(getTouchMode() | TOUCH_MODE_BACK_EFFECT);
            return true;
        }
    };

    private static final int MASK_R_8bit = 0x000000ff;
    private static final int MASK_L_8bit = 0xff000000;
    private static final int SHIFT_8 = 8;
    private static final int SHIFT_16 = 16;

    /**
     * set the current displayed depth picture immediately.
     *
     * @param data
     *            data
     * @param offset
     *            offset
     * @param pixelsize
     *            pixel size
     * @param width
     *            width
     * @param height
     *            height
     */
    public void setDepthActor(byte[] data, int offset, int pixelsize,
            int width, int height) {
        if (data != null) {
            Log.v(TAG, "setDepthActor(" + width + "x" + height + ") ("
                    + pixelsize + ") data.len:" + data.length + " offset:"
                    + offset);
            if (data.length >= width * height * pixelsize + offset) {
                Bitmap img = Bitmap.createBitmap(width, height,
                        Bitmap.Config.ARGB_8888);
                int x = 0;
                int y = 0;
                for (y = 0; y < height; y++) {
                    for (x = 0; x < width; x++) {
                        int rgb = MASK_R_8bit & ((int) data[(y * width + x)
                                                            * pixelsize + offset]);
                        img.setPixel(x, y, (MASK_L_8bit | (rgb << SHIFT_16)
                                | (rgb << SHIFT_8) | rgb));
                    }
                }
                setDepthActor(img);
            }
        }
    }

    /**
     * set the current displayed picture immediately.
     *
     * @param pic
     *            bitmap
     */
    public void setDepthActor(Bitmap pic) {
        Log.v(TAG, "setDepthActor() " + pic);
        if (pic != null) {
            mDepthActor.setImageFromBitmap(pic);
            mDepthActor.setSize(new Dimension(pic.getWidth(), pic.getHeight()));
            setImageActorViewSize(mImageActor, mBitmap, mWidth, mHeight);
            mDepthActor.setVisible(true);
        } else {
            mDepthActor.setVisible(false);
        }
    }

    /**
     * set the picture displayed scale factor.
     *
     * @param scale
     *            scale
     */
    public void setImageActorViewSizeScale(float scale) {
        if (mImageActor != null) {
            setImageActorViewSizeScale(mImageActor, scale);
        }
    }

    /**
     * set the current displayed picture immediately.
     *
     * @param pic
     *            bitmap
     * @param viewWidht
     *            view Widht
     * @param viewHeight
     *            view Height
     */
    public synchronized void setImageActor(Bitmap pic, final float viewWidht,
            final float viewHeight) {
        setImageActor(pic, true);
        if (viewWidht > 0 && viewHeight > 0) {
            setImageActorViewSize(mImageActor, mBitmap, mWidth, mHeight);
        }
    }

    /**
     * set the picture that is going to be show after transition effect done.
     * @param pic bitmap
     * @param recycle need to do bitmap recycle if previous bitmap existed
     */
    public synchronized void setImageActorNew(Bitmap pic, boolean recycle) {
        logD("setImageActorNew(" + recycle + ") pic:" + pic);
        if (pic != null) {
            if (mBitmapNew != null && recycle) {
                mBitmapNew.recycle();
            }
            mBitmapNew = pic;
            mImageActor.setMaterialProperty("", "M_NEW_TEXTURE", mBitmapNew);
        }
    }

    /**
     * set the picture that is going to be show after transition effect done.
     * @param pic bitmap
     */
    public synchronized void setImageActorNew(Bitmap pic) {
        setImageActorNew(pic, true);
    }

    /**
     * set the effect animation transition time.
     * @param total total animation time
     * @param firstpart the previous part
     */
    public synchronized void setTransitionTime(float total, float firstpart) {
        logD("setTransitionTime() total:" + total + ", firstpart:" + firstpart);
        if (mIsShowImageIsTransitionPlaying == false) {
            if (total > firstpart) {
                mShowImageTotalDurationTime = total;
                mShowImageFirstDurationTime = firstpart;
            }
        }
    }

    @Override
    public synchronized void onDrawFrame(GL10 gl) {
        // onDrawFrameStageFadeIn();

        if (mIsShowImageIsTransitionPlaying) {
            onDrawFrameImage();
        }

        if (mBackIsTransitionPlaying) {
            onDrawFrameBack();
        }

        super.onDrawFrame(gl);
    }

    @Override
    public synchronized boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        boolean bProc = false;
        if (mZoomGestureDetector != null) {
            mZoomGestureListener.setMotionEvent(this, event);
            bProc |= mZoomGestureDetector.onTouchEvent(event);
        }
        if (mGestureDetector != null) {
            bProc |= mGestureDetector.onTouchEvent(event);
        }
        if (getTouchMode() == TOUCH_MODE_NOTHING) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    mDownX = event.getX();
                    mDownY = event.getY();
                    logI("onTouchEvent() @View(" + event.getX() + ","
                            + event.getY() + ")");

                    if (mImageActor.hitTest(new Point(mDownX, mDownY)) != null) {
                        MtkLog.i(TAG, "mDownX = " + mDownX + "|| mDownY = "
                                + mDownY);

                        // setImageActorNew(refocusBitmap);
                        float k = mScaleCurrent / mScaleFit;
                        float mDownRX = DOWNR_X;
                        float mDownRY = DOWNR_Y;

                        mDownRX += (mDownX - mLocationX * (float) mWidth)
                                / (((float) mWidth - mXOffset * 2.0f) * k);
                        mDownRY += (mDownY - mLocationY * (float) mHeight)
                                / (((float) mHeight - mYOffset * 2.0f) * k);
                        logI("onTouchEvent() @Image(" + mDownRX + "," + mDownRY
                                + ") k:" + k);
                        mRefocusListener.setRefocusImage(mDownRX, mDownRY);
                        onDrawFrameImageReset(false);
                        mImageActor.setMaterialProperty("", "M_X", mDownRX);
                        mImageActor.setMaterialProperty("", "M_Y", mDownRY);
                        mIsShowImageIsTransitionPlaying = true;

                        mFocusActor.setVisible(true);
                        mFocusActor.setPosition(new Point(mDownX, mDownY, -1.0f,
                                false));
                        setTouchMode(getTouchMode() | TOUCH_MODE_TRANSITION_EFFECT);
                        requestRender();
                    } else {
                        moveBackCheck();
                    }
                    bProc |= true;
                    break;
                default:
                    break;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP
                && event.getPointerCount() <= 1) {
            setTouchMode(TOUCH_MODE_NOTHING);
            moveBackCheck();
        }
        return bProc;
    }

    /**
     * Get image action position.
     *
     * @param point The touch points.
     */
    public void getImageActorPosition(int point[]) {
        if (point == null || point.length < ACTOR_XXYY_ELEMENT) {
            return;
        }
        float k = mScaleCurrent / mScaleFit;
        float imgW = ((float) mWidth - mXOffset * 2.0f);
        float imgH = ((float) mHeight - mYOffset * 2.0f);
        float w = imgW / 2.0f;
        float h = imgH / 2.0f;
        float centerX = mLocationX * mWidth;
        float centerY = mLocationY * mHeight;
        point[0] = (int) (centerX - w * k);
        point[1] = (int) (centerX + w * k);
        point[2] = (int) (centerY - h * k);
        point[INDEX_4] = (int) (centerY + h * k);
    }

    /**
     * check the Boundary if it need to be changed.
     * @param isY 1 for Y axis
     * @return true if it is out of range
     */
    public boolean checkBoundary(int isY) {
        boolean result = false;
        int pX1X2Y1Y2[] = new int[ACTOR_XXYY_ELEMENT];
        getImageActorPosition(pX1X2Y1Y2);

        switch (isY) {
            case 0:
                if ((0 < pX1X2Y1Y2[0] && pX1X2Y1Y2[0] < mWidth)
                        || (0 < pX1X2Y1Y2[1] && pX1X2Y1Y2[1] < mWidth)) {
                    result = false;
                } else {
                    result = true;
                }
                break;

            case 1:
                if ((0 < pX1X2Y1Y2[2] && pX1X2Y1Y2[2] < mHeight)
                        || (0 < pX1X2Y1Y2[INDEX_4] && pX1X2Y1Y2[INDEX_4] < mHeight)) {
                    result = false;
                } else {
                    result = true;
                }
                break;
            default:
                break;
        }
        logE(result
                + " isY:"
                + isY
                + " Point:"
                + String.format("(%d, %d) (%d, %d)", pX1X2Y1Y2[0],
                        pX1X2Y1Y2[1], pX1X2Y1Y2[2], pX1X2Y1Y2[INDEX_4]));
        return result;
    }

    public void setRefocusListener(RefocusListener refocusListener) {
        mRefocusListener = refocusListener;
    }

    private synchronized void setTouchMode(int mode) {
        logI("setTouchMode() " + mTouchMode + " => " + mode);
        mTouchMode = mode;
        requestRender();
    }

    private synchronized int getTouchMode() {
        return mTouchMode;
    }

    private void onDrawFrameImageReset(boolean reloadBitmap) {
        mIsShowImageIsTransitionPlaying = false;
        mIsShowImageIsGetFirstFrameTime = false;
        if (reloadBitmap) {
            mImageActor.setImageFromBitmap(mBitmap);
            mImageActor.setSize(new Dimension(mBitmap.getWidth(), mBitmap.getHeight()));
            mImageActor.setMaterialProperty("", "M_NEW_TEXTURE", mBitmapNew);
        }
        mImageActor.setMaterialProperty("", "M_STEP", 0.0f);
        mImageActor.setMaterialProperty("", "M_FADEOUTSTEP", 1.0f);
        mImageActor.setMaterialProperty("", "M_LEVEL", 1.0f);
        mFocusActor.setVisible(false);
    }

    private static final float ACTOR_MAX_ANIMATION_LEVEL = 25f;
    private void onDrawFrameImage() {
        Log.i(TAG, "onDrawFrameImage");
        boolean isEnd = false;
        if (mIsShowImageIsGetFirstFrameTime == false) {
            mShowImageFirstFrameTime = SystemClock.elapsedRealtime();
            mIsShowImageIsGetFirstFrameTime = true;
        }

        long localTick = SystemClock.elapsedRealtime()
                - mShowImageFirstFrameTime;
        float fadeoutstep = 1.0f;
        if (localTick > mShowImageTotalDurationTime) {
            isEnd = true;
        }

        float step = 0.0f;
        if (localTick <= mShowImageFirstDurationTime) {
            step = localTick / mShowImageFirstDurationTime;
        } else if (localTick > mShowImageFirstDurationTime) {
            fadeoutstep = (mShowImageTotalDurationTime - localTick)
                    / (mShowImageTotalDurationTime - mShowImageFirstDurationTime);
            fadeoutstep = (fadeoutstep > 0.0f) ? fadeoutstep : 0.0f;
            mImageActor.setMaterialProperty("", "M_FADEOUTSTEP", fadeoutstep);

            localTick = (long) (mShowImageTotalDurationTime - localTick);
            step = (float) localTick
                    / (mShowImageTotalDurationTime - mShowImageFirstDurationTime);
        }
        step = (step > 0.0f) ? step : 0.0f;
        mImageActor.setMaterialProperty("", "M_STEP", step);
        mImageActor.setMaterialProperty("", "M_LEVEL", ACTOR_MAX_ANIMATION_LEVEL);

        if (isEnd) {
            onDrawFrameImageReset(true);
            setImageActorSwap();
            mImageActor.setMaterialProperty("", "M_LEVEL", 1.0f);
            setTouchMode(getTouchMode() & (~TOUCH_MODE_TRANSITION_EFFECT));
            logD("onDrawFrameImage() done");
            requestRender();
        }
    }

    private void onDrawFrameBack() {
        boolean isEnd = false;
        if (mBackIsGetFirstFrameTime == false) {
            mBackFirstFrameTime = SystemClock.elapsedRealtime();
            mBackIsGetFirstFrameTime = true;
            mBackLocationX = mLocationX;
            mBackLocationY = mLocationY;
            mBackScaleCurrent = mScaleCurrent;
        }

        long localTick = SystemClock.elapsedRealtime() - mBackFirstFrameTime;
        if (localTick > mBackTotalDurationTime) {
            isEnd = true;
        }

        float step = 0.0f;
        if (localTick <= mBackTotalDurationTime) {
            step = localTick / mBackTotalDurationTime;
            mLocationX = mBackLocationX * (1 - step) + mBackTargetX * step;
            mLocationY = mBackLocationY * (1 - step) + mBackTargetY * step;
            if (mImageActor != null) {
                mImageActor.setPosition(new Point(mLocationX, mLocationY, true));
            }
            if (mBackIsScaleBack) {
                float scale = mBackScaleCurrent * (1 - step) + mScaleFit * step;
                setImageActorViewSizeScale(scale);
            }
        }

        if (isEnd) {
            mBackIsTransitionPlaying = false;
            mBackIsGetFirstFrameTime = false;
            mLocationX = mBackTargetX;
            mLocationY = mBackTargetY;
            if (mImageActor != null) {
                mImageActor.setPosition(new Point(mLocationX, mLocationY, true));
            }
            if (mBackIsScaleBack) {
                setImageActorViewSizeScale(mScaleFit);
            }
            mBackIsScaleBack = false;
            setTouchMode(getTouchMode() & (~TOUCH_MODE_BACK_EFFECT));
            logD("onDrawFrameBack() done");
            requestRender();
        }
    }

    private synchronized void moveBackStop() {
        logW("onDrawFrameBackStop() " + mBackIsTransitionPlaying + ", "
                + mBackIsScaleBack);
        mBackIsTransitionPlaying = false;
        mBackIsGetFirstFrameTime = false;
        mBackIsScaleBack = false;
        setTouchMode(getTouchMode() & (~TOUCH_MODE_BACK_EFFECT));
        requestRender();
    }

    private static final int ACTOR_XXYY_ELEMENT = 4;
    private static final int INDEX_4 = 3;

    private void moveBackCheck() {
        int pXXYY[] = new int[ACTOR_XXYY_ELEMENT];
        getImageActorPosition(pXXYY);
        boolean needMove = false;

        if (mBackIsTransitionPlaying == false
                && (getTouchMode() & TOUCH_MODE_BACK_EFFECT) != TOUCH_MODE_BACK_EFFECT) {

            mBackTargetX = mLocationX;
            mBackTargetY = mLocationY;
            if (checkXCoordinate(pXXYY)) {
                needMove = true;
            }
            if (checkYCoordinate(pXXYY)) {
                needMove = true;
            }
            if (needMove) {
                mBackIsTransitionPlaying = true;
                mBackIsScaleBack = false;
                setTouchMode(getTouchMode() | TOUCH_MODE_BACK_EFFECT);
            }
        }
    }

    private boolean checkXCoordinate(int[] pXXYY) {
        boolean needMove = false;
        // check X
        if ((0 < pXXYY[0] && pXXYY[0] < mWidth) && (mWidth < pXXYY[1])) {
            needMove = true;
            mBackTargetX = mLocationX
                    - (float) (((float) pXXYY[0]) / (float) mWidth);
            if ((pXXYY[1] - pXXYY[0]) < mWidth) {
                mBackTargetX += ((float) mWidth - (float) (pXXYY[1] - pXXYY[0]))
                        / (2.0f * (float) mWidth);
            }
        } else if ((0 < pXXYY[1] && pXXYY[1] < mWidth) && (0 > pXXYY[0])) {
            needMove = true;
            mBackTargetX = mLocationX
                    + (float) ((float) (mWidth - pXXYY[1]) / (float) mWidth);
            if ((pXXYY[1] - pXXYY[0]) < mWidth) {
                mBackTargetX -= ((float) mWidth - (float) (pXXYY[1] - pXXYY[0]))
                        / (2.0f * (float) mWidth);
            }
        } else if (pXXYY[0] < 0 && pXXYY[1] < 0) {
            needMove = true;
            mBackTargetX = mLocationX
                    + (1.0f - ((float) pXXYY[1] / (float) mWidth));
            if ((pXXYY[1] - pXXYY[0]) < mWidth) {
                mBackTargetX -= ((float) mWidth - (float) (pXXYY[1] - pXXYY[0]))
                        / (2.0f * (float) mWidth);
            }
        } else if (pXXYY[0] > mWidth && pXXYY[1] > mWidth) {
            needMove = true;
            mBackTargetX = mLocationX
                    - ((float) pXXYY[0] / (float) mWidth);
            if ((pXXYY[INDEX_4] - pXXYY[2]) < mWidth) {
                mBackTargetX += ((float) mWidth - (float) (pXXYY[1] - pXXYY[0]))
                        / (2.0f * (float) mWidth);
            }
        }
        return needMove;
    }

    private boolean checkYCoordinate(int[] pXXYY) {
        boolean needMove = false;
        // check Y
        if ((0 < pXXYY[2] && pXXYY[2] < mHeight) && (mHeight < pXXYY[INDEX_4])) {
            needMove = true;
            mBackTargetY = mLocationY
                    - (float) (((float) pXXYY[2]) / (float) mHeight);
            if ((pXXYY[INDEX_4] - pXXYY[INDEX_4]) < mHeight) {
                mBackTargetY += ((float) mHeight - (float) (pXXYY[INDEX_4] - pXXYY[2]))
                        / (2.0f * (float) mHeight);
            }
        } else if ((0 < pXXYY[INDEX_4] && pXXYY[INDEX_4] < mHeight) && (0 > pXXYY[2])) {
            needMove = true;
            mBackTargetY = mLocationY
                    + (float) ((float) (mHeight - pXXYY[INDEX_4]) / (float) mHeight);
            if ((pXXYY[INDEX_4] - pXXYY[2]) < mHeight) {
                mBackTargetY -= ((float) mHeight - (float) (pXXYY[INDEX_4] - pXXYY[2]))
                        / (2.0f * (float) mHeight);
            }
        } else if (pXXYY[2] < 0 && pXXYY[INDEX_4] < 0) {
            needMove = true;
            mBackTargetY = mLocationY
                    + (1.0f - ((float) pXXYY[INDEX_4] / (float) mHeight));
            if ((pXXYY[INDEX_4] - pXXYY[2]) < mHeight) {
                mBackTargetY -= ((float) mHeight - (float) (pXXYY[INDEX_4] - pXXYY[2]))
                        / (2.0f * (float) mHeight);
            }
        } else if (pXXYY[2] > mHeight && pXXYY[INDEX_4] > mHeight) {
            needMove = true;
            mBackTargetY = mLocationY
                    - ((float) pXXYY[2] / (float) mHeight);
            if ((pXXYY[INDEX_4] - pXXYY[2]) < mHeight) {
                mBackTargetY += ((float) mHeight - (float) (pXXYY[INDEX_4] - pXXYY[2]))
                        / (2.0f * (float) mHeight);
            }
        }
        return needMove;
    }

    private synchronized void setImageActorSwap() {
        logD("setImageActorSwap() to " + mBitmapNew);
        Bitmap tmp = mBitmapNew;
        mBitmapNew = mBitmap;
        setImageActor(tmp, false);
        setImageActorNew(mBitmapNew, false);
        float fit = mScaleCurrent;
        setImageActorViewSize(mImageActor, mBitmap, mWidth, mHeight);
        setImageActorViewSizeScale(fit);
    }

    private static final int ACTOR_MAX_OPACITY = 255;
    private void onDrawFrameStageFadeIn() {
        if (mStageFadeInIsGetFirstFrameTime == false) {
            logD("onDrawFrame() Stage Fade-In Start !");
            mStageFadeInFirstFrameTime = SystemClock.elapsedRealtime();
            mStageFadeInIsGetFirstFrameTime = true;
        }
        if (mStageFadeInIsFadeInDone == false) {
            long currentTime = SystemClock.elapsedRealtime();
            int value = 1 + (int) (((float) ACTOR_MAX_OPACITY - 1f) * Math.min(
                    (float) (currentTime - mStageFadeInFirstFrameTime) / mStageFadeInDurationTime,
                    1.0f));
            mStage.setOpacity(value);
            if (value >= ACTOR_MAX_OPACITY) {
                mStageFadeInIsFadeInDone = true;
            }
        }
    }

    private void setImageActorViewSizeScale(Image img, float scale) {
        mScaleCurrent = scale;
        img.setScale(new Scale(scale, scale));
    }

    private synchronized void setImageActor(Bitmap pic, boolean recycle) {
        long begin = System.currentTimeMillis();
        long spentTime = 0;
        if (pic != null) {
            if (mBitmap != null && recycle) {
                mBitmap.recycle();
                MtkLog.i(TAG, "********* mBitmap.recycle()************");
            }
            spentTime = System.currentTimeMillis() - begin;
            MtkLog.i(TAG, "setImageActor mBitmap.recycle SpentTime = "
                    + spentTime);
            mBitmap = pic;
            begin = System.currentTimeMillis();
            mImageActor.setImageFromBitmap(mBitmap);
            spentTime = System.currentTimeMillis() - begin;
            MtkLog.i(TAG, "mImageActor.setImageFromBitmap spentTime = "
                    + spentTime);
            mImageActor.setSize(new Dimension(mBitmap.getWidth(), mBitmap
                    .getHeight()));
            MtkLog.i(TAG, " mImageActor.setImageFromBitmap(mBitmap)");
            mImageActor.setMaterialProperty("", "M_NEW_TEXTURE", mBitmapNew);
            mImageActor.setMaterialProperty("", "M_STEP", 0.0f);
            mImageActor.setMaterialProperty("", "M_FADEOUTSTEP", 1.0f);
        }
    }

    private static final float DEPTH_ACTOR_SIZE_FACTOR = 5f;

    private void setImageActorViewSize(Image img, Bitmap bmp,
            final float viewWidth, final float viewHeight) {

        if (bmp == null) {
            return;
        }
        final float bitmapWidth = bmp.getWidth();
        final float bitmapHeight = bmp.getHeight();

        final float viewAspectRatio = viewWidth / viewHeight;
        final float bitmapAspectRatio = bitmapWidth / bitmapHeight;

        float scale = 0.0f;
        if (viewAspectRatio <= bitmapAspectRatio) {
            scale = viewWidth / bitmapWidth;
            mXOffset = 0.0f;
            mYOffset = (viewHeight - bitmapHeight * scale) / 2.0f;
        } else {
            scale = viewHeight / bitmapHeight;
            mXOffset = (viewWidth - bitmapWidth * scale) / 2.0f;
            mYOffset = 0.0f;
        }

        logD("setImage(View__) " + viewWidth + " x " + viewHeight + " ["
                + viewAspectRatio + "]");
        logD("setImage(Bitmap) " + bitmapWidth + " x " + bitmapHeight + " ["
                + bitmapAspectRatio + "]");
        logD("setImage() scale: " + scale + ", Offset: (" + mXOffset + ", "
                + mYOffset + ")");

        mScaleFit = scale;
        setImageActorViewSizeScale(img, mScaleFit);

        // Depth Actor
        float depthW = viewWidth / DEPTH_ACTOR_SIZE_FACTOR;
        Dimension dim = mDepthActor.getSize();
        float ratio = depthW / dim.width;
        mDepthActor.setScale(new Scale(ratio, ratio));
    }

    private static final float DEPTH_ACTOR_SIZE = 10f;
    private static final float DEPTH_ACTOR_Z = -3f;
    private final float mProjectionZNear = 500f;
    private final float mProjectionZFar = 5000f;
    private final float mProjectionZStage = -1111.0f;

    private void setup() {
        logI("setup() is called : Configuration : "
                + getResources().getConfiguration());
        mStage.setProjection(Stage.UI_PERSPECTIVE, mProjectionZNear, mProjectionZFar,
                mProjectionZStage);

        mImageActor = Image.createEmptyImage();
        mImageActor.setPosition(new Point(mLocationX, mLocationY, true));
        mImageActor.setMaterial("speed.mat");
        mImageActor.enableMipmap(true);
        mImageActor.setMaterialProperty("", "M_LEVEL", 1.0f);
        mStage.add(mImageActor);

        mFocusActor = Image.createFromResource(getResources(), R.drawable.m_refocus_refocus);
        mFocusActor.setVisible(false);
        mStage.add(mFocusActor);

        mDepthActor = Image.createEmptyImage();
        mDepthActor.setPosition(new Point(mDepthLocationX, mDepthLocationY,
                DEPTH_ACTOR_Z, true));
        mDepthActor.setSize(new Dimension(DEPTH_ACTOR_SIZE, DEPTH_ACTOR_SIZE));
        mDepthActor.setVisible(false);
        mStage.add(mDepthActor);
    }

    private void logD(String msg) {
        Log.d(TAG, "[" + mViewID + "] " + msg);
    }

    private void logI(String msg) {
        Log.i(TAG, "[" + mViewID + "] " + msg);
    }

    private void logW(String msg) {
        Log.w(TAG, "[" + mViewID + "] " + msg);
    }

    private void logE(String msg) {
        Log.e(TAG, "[" + mViewID + "] " + msg);
    }

    private void removeFolder(String path) {
        removeFolder(path, 0);
    }

    private void removeFolder(String path, int level) {
        File folder = new File(path);
        if (!folder.exists()) {
            logI("[" + level + "] folder doesn't exist! : " + path);
            return;
        }

        File[] files = folder.listFiles();
        for (File thefile : files) {
            logW("[" + level + "] Remove: " + thefile.getPath());
            if (thefile.isDirectory()) {
                removeFolder(thefile.getPath(), level + 1);
            } else {
                if (!thefile.delete()) {
                    logW("[" + level + "] file deletion failed! : " + thefile);
                }
            }
        }

        if (!folder.delete()) {
            logW("[" + level + "] folder deletion failed! : " + folder);
        } else {
            logI("[" + level + "] folder deletion ok : " + folder);
        }
    }
}
