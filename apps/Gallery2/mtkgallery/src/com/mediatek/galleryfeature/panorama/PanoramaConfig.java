package com.mediatek.galleryfeature.panorama;


import com.mediatek.galleryframework.util.MtkLog;
import com.mediatek.galleryframework.util.Utils;

public class PanoramaConfig {
    private static final String TAG = "MtkGallery2/PanoramaConfig";
    private static final float FOVY_ADJUST_PARAM = 3.0f;
    private static final float DEGREE_180 = 180.f;
    private static final float DEGREE_360 = 360.f;
    private static final float RADIAN_TO_DEGREE = (float) (DEGREE_180 / Math.PI);
    private static final float DEGREE_TO_RADIAN = (float) (Math.PI / DEGREE_180);
    private static final int MAX_CANVAS_LIMIT = 2048;
    private static final int MAX_FRAME_COUNT = 45;

    public int mOriginWidth;
    public int mOriginHeight;
    public int mNewWidth;
    public int mNewHeight;
    public int mCanvasWidth;
    public int mCanvasHeight;
    public float mCanvasScale;
    public float mWidthDegree;

    public float mFovy;
    public float mFovx;
    public float mHalfHeightDegree;
    public float mRotateDegree;
    public int mDegreeRange;
    public float mCameraDistance;
    public int mFrameTotalCount;
    public int mFrameTimeGap;
    public float mFrameDegreeGap;

    public PanoramaConfig(int imageWidth, int imageHeight, int canvasWidth, int canvasHeight) {
        this(imageWidth, imageHeight, canvasWidth, canvasHeight, 1.0f);
    }

    public PanoramaConfig(int imageWidth, int imageHeight, int canvasWidth, int canvasHeight,
            float canvasScale) {
        mOriginWidth = imageWidth;
        mOriginHeight = imageHeight;
        mCanvasScale = canvasScale;
        mCanvasWidth = (int) (canvasWidth * mCanvasScale);
        mCanvasHeight = (int) (canvasHeight * mCanvasScale);
        // for texture memory issue, we set canvasWidth and canvasHeight below 2048
        if (Utils.nextPowerOf2(mCanvasWidth) > MAX_CANVAS_LIMIT) {
            float ratio = (float) mCanvasWidth / (float) mCanvasHeight;
            mCanvasWidth = MAX_CANVAS_LIMIT;
            mCanvasHeight = (int) (mCanvasWidth / ratio);
            mCanvasScale = (float) mCanvasWidth / (float) canvasWidth;
        }

        // In order to keep all ratio panorama images have almost the same height when playback,
        // we adjust widthPercent according to the ratio of image,
        // the bigger the ratio is, the smaller the widthPercent is.
        // and the widthPercent is between PANORAMA_P80_WIDTHPERCENT and PANORAMA_MIN_WIDTHPERCENT
        float widthPercent = PanoramaHelper.getWidthPercent(mOriginWidth, mOriginHeight);
        mWidthDegree = DEGREE_180 / widthPercent;

        float heightDegreeTemp = mOriginHeight * mWidthDegree / mOriginWidth;
        if (heightDegreeTemp > PanoramaHelper.MAX_HEIGHT_DEGREE) {
            mNewWidth = (int) (mOriginHeight * DEGREE_360 / PanoramaHelper.MAX_HEIGHT_DEGREE);
        } else {
            mNewWidth = (int) (mOriginWidth * DEGREE_360 / mWidthDegree);
        }
        mNewHeight = mOriginHeight;
        mHalfHeightDegree = mNewHeight * DEGREE_180 / mNewWidth;
        // mFovy = mHalfHeightDegree * 2.f;
        mFovy = mHalfHeightDegree * mOriginWidth / mOriginHeight * 2.f / FOVY_ADJUST_PARAM
                * widthPercent;
        mFovx = (float) (2 * Math.atan((float) mCanvasWidth / mCanvasHeight
                * Math.tan(mFovy / 2 * DEGREE_TO_RADIAN)) * RADIAN_TO_DEGREE);

        mCameraDistance = (float) (PanoramaHelper.MESH_RADIUS
                * Math.sin(mHalfHeightDegree * DEGREE_TO_RADIAN) / Math.tan(mHalfHeightDegree
                * DEGREE_TO_RADIAN));
        mRotateDegree = (float) Math.atan(mCanvasWidth / mCameraDistance) * RADIAN_TO_DEGREE;
        mDegreeRange = (int) (mOriginWidth * DEGREE_360 / mNewWidth - mRotateDegree * 2);

        mFrameTotalCount = mDegreeRange / PanoramaHelper.FRAME_DEGREE_GAP;
        mFrameTotalCount = mFrameTotalCount < MAX_FRAME_COUNT ? MAX_FRAME_COUNT : mFrameTotalCount;
        mFrameDegreeGap = (float) mDegreeRange / (float) mFrameTotalCount;
        mFrameTimeGap = PanoramaHelper.FRAME_TIME_GAP;
        MtkLog.i(TAG, "<new> this = " + this.toString());
    }

    public String toString() {
        return "originW = " + mOriginWidth +
                ", originH = " + mOriginHeight +
                ", newW = " + mNewWidth +
                ", newH = " + mNewHeight +
                ", canvasW = " + mCanvasWidth +
                ", canvasH = " + mCanvasHeight +
                ", heightD = " + mHalfHeightDegree +
                ", widthD = " + mWidthDegree +
                ", fovy = " + mFovy +
                ", fovx = " + mFovx +
                ", rotateD = " + mRotateDegree +
                ", rangeD = " + mDegreeRange +
                ", cameraDis = " + mCameraDistance +
                ", frameCount = " + mFrameTotalCount +
                ", timeGap = " + mFrameTimeGap +
                ", degreeGap = " + mFrameDegreeGap;
    }

}