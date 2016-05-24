package com.mediatek.xmp;

import android.graphics.Rect;
import android.util.Log;

/**
 * StereoDebugInfoParser.
 *
 */
public class StereoDebugInfoParser {
    private final static String TAG = "mtkGallery2/StereoDebugInfoParser";
    private final static int VALID_MASK = 0xff;
    private final static String MASKINFO_TAG = "mask_info";
    private final static String MASKINFO_WIDTH = "width";
    private final static String MASKINFO_HEIGHT = "height";
    private final static String MASKINFO_MASK = "mask";
    private final static String JPSINFO_TAG = "JPS_size";
    private final static String JPSINFO_WIDTH = "width";
    private final static String JPSINFO_HEIGHT = "height";
    private final static String DEPTHINFO_TAG = "depth_buffer_size";
    private final static String DEPTHINFO_WIDTH = "width";
    private final static String DEPTHINFO_HEIGHT = "height";
    private final static String POSINFO_TAG = "main_cam_align_shift";
    private final static String POSINFO_X = "x";
    private final static String POSINFO_Y = "y";
    private final static String TOUCH_COORD_INFO_TAG = "focus_roi";
    private final static String TOUCH_COORD_INFO_LEFT = "left";
    private final static String TOUCH_COORD_INFO_TOP = "top";
    private final static String TOUCH_COORD_INFO_RIGHT = "right";
    private final static String TOUCH_COORD_INFO_BOTTOM = "bottom";
    private final static String VIEWINFO_TAG = "input_image_size";
    private final static String VIEWINFO_WIDTH = "width";
    private final static String VIEWINFO_HEIGHT = "height";
    private final static String ORIENTATIONINFO_TAG = "capture_orientation";
    private final static String ORIENTATIONINFO_ORIENTATION = "orientation";
    private final static String DEPTH_ROTATION_INFO_TAG = "depthmap_orientation";
    private final static String DEPTH_ROTATION_INFO_ORIENTATION = "orientation";
    private final static String MAIN_CAM_POSITION_INFO_TAG = "sensor_relative_position";
    private final static String MAIN_CAM_POSITION_INFO_POSITION = "relative_position";
    private final static String VERIFY_GEO_INFO_TAG = "verify_geo_data";
    private final static String VERIFY_GEO_INFO_LEVEL = "quality_level";
    private final static String VERIFY_GEO_INFO_STATISTICS = "statistics";
    private final static String VERIFY_PHO_INFO_TAG = "verify_pho_data";
    private final static String VERIFY_PHO_INFO_LEVEL = "quality_level";
    private final static String VERIFY_PHO_INFO_STATISTICS = "statistics";
    private final static String VERIFY_MTK_CHA_INFO_TAG = "verify_mtk_cha";
    private final static String VERIFY_MTK_CHA_INFO_LEVEL = "quality_level";
    private final static String VERIFY_MTK_CHA_INFO_STATISTICS = "statistics";
    private final static String FACE_DETECTION_INFO_TAG = "face_detections";
    private final static String FACE_DETECTION_INFO_LEFT = "left";
    private final static String FACE_DETECTION_INFO_TOP = "top";
    private final static String FACE_DETECTION_INFO_RIGHT = "right";
    private final static String FACE_DETECTION_INFO_BOTTOM = "bottom";
    private final static String FACE_DETECTION_INFO_RIP = "rotation-in-plane";
    private final static String DOF_LEVEL_TAG = "dof_level";
    private final static String LDCINFO_TAG = "ldc_size";
    private final static String LDCINFO_WIDTH = "width";
    private final static String LDCINFO_HEIGHT = "height";

    // google stereo params
    private final static String GFOCUSINFO_TAG = "GFocus";
    private final static String GFOCUSINFO_BLUR_AT_INFINITY = "BlurAtInfinity";
    private final static String GFOCUSINFO_FOCAL_DISTANCE = "FocalDistance";
    private final static String GFOCUSINFO_FOCAL_POINT_X = "FocalPointX";
    private final static String GFOCUSINFO_FOCAL_POINT_Y = "FocalPointY";
    private final static String GIMAGEINFO_TAG = "GImage";
    private final static String GIMAGEINFO_MIME = "Mime";
    private final static String GDEPTHINFO_TAG = "GDepth";
    private final static String GDEPTHINFO_FORMAT = "Format";
    private final static String GDEPTHINFO_NEAR = "Near";
    private final static String GDEPTHINFO_FAR = "Far";
    private final static String GDEPTHINFO_MIME = "Mime";

    private int mFaceRectCount = -1;
    private int mMainCamPostion = -1;
    private int mOrientation = -1;
    private int mDepthRotation = -1;
    private int mViewWidth = -1;
    private int mViewHeight = -1;
    private int mTouchCoordX1st = -1;
    private int mTouchCoordY1st = -1;
    private int mPosX = -1;
    private int mPosY = -1;
    private int mMaskWidth = -1;
    private int mMaskHeight = -1;
    private int mMaskSize = -1;
    private int mJpsWidth = -1;
    private int mJpsHeight = -1;
    private int mGDepthWidth = -1;
    private int mGDepthHeight = -1;

    private JsonParser mParser;

    /**
     * constructor,create JsonParser.
     *
     * @param jsonString
     *            jsonString
     */
    public StereoDebugInfoParser(String jsonString) {
        mParser = new JsonParser(jsonString);
    }

    /**
     * constructor,create JsonParser.
     *
     * @param jsonBuffer
     *            jsonBuffer
     */
    public StereoDebugInfoParser(byte[] jsonBuffer) {
        mParser = new JsonParser(jsonBuffer);
    }

    /**
     * get jps width.
     *
     * @return jps width
     */
    public int getJpsWidth() {
        if (mJpsWidth != -1) {
            return mJpsWidth;
        }
        mJpsWidth = mParser.getValueIntFromObject(JPSINFO_TAG, null, JPSINFO_WIDTH);
        Log.d(TAG, "<getJpsWidth> mJpsWidth: " + mJpsWidth);
        return mJpsWidth;
    }

    /**
     * get jps height.
     *
     * @return jps height
     */
    public int getJpsHeight() {
        if (mJpsHeight != -1) {
            return mJpsHeight;
        }
        mJpsHeight = mParser.getValueIntFromObject(JPSINFO_TAG, null, JPSINFO_HEIGHT);
        Log.d(TAG, "<getJpsHeight> mJpsHeight: " + mJpsHeight);
        return mJpsHeight;
    }

    /**
     * Get google depth buffer width returned by camera.
     *
     * @return google depth buffer width
     */
    public int getGoogleDepthWidth() {
        if (mGDepthWidth != -1) {
            return mGDepthWidth;
        }
        mGDepthWidth = mParser.getValueIntFromObject(DEPTHINFO_TAG, null, DEPTHINFO_WIDTH);
        Log.d(TAG, "<getGoogleDepthWidth> mGDepthWidth: " + mGDepthWidth);
        return mGDepthWidth;
    }

    /**
     * Get google depth buffer height returned by camera.
     *
     * @return google depth buffer height
     */
    public int getGoogleDepthHeight() {
        if (mGDepthHeight != -1) {
            return mGDepthHeight;
        }
        mGDepthHeight = mParser.getValueIntFromObject(DEPTHINFO_TAG, null, DEPTHINFO_HEIGHT);
        Log.d(TAG, "<getGoogleDepthHeight> mGDepthHeight: " + mGDepthHeight);
        return mGDepthHeight;
    }

    /**
     * get mask width.
     *
     * @return mask width
     */
    public int getMaskWidth() {
        if (mMaskWidth != -1) {
            return mMaskWidth;
        }
        mMaskWidth = mParser.getValueIntFromObject(MASKINFO_TAG, null, MASKINFO_WIDTH);
        Log.d(TAG, "<getMaskWidth> mMaskWidth: " + mMaskWidth);
        return mMaskWidth;
    }

    /**
     * get mask height.
     *
     * @return mask height
     */
    public int getMaskHeight() {
        if (mMaskHeight != -1) {
            return mMaskHeight;
        }
        mMaskHeight = mParser.getValueIntFromObject(MASKINFO_TAG, null, MASKINFO_HEIGHT);
        Log.d(TAG, "<getMaskHeight> mMaskHeight: " + mMaskHeight);
        return mMaskHeight;
    }

    /**
     * get mask size.
     *
     * @return mask size
     */
    public int getMaskSize() {
        if (mMaskSize != -1) {
            return mMaskSize;
        }
        mMaskSize = getMaskWidth() * getMaskHeight();
        Log.d(TAG, "<getMaskSize> mMaskSize: " + mMaskSize);
        return mMaskSize;
    }

    /**
     * get mask buffer.
     *
     * @return mask buffer
     */
    public byte[] getMaskBuffer() {
        mMaskSize = getMaskSize();
        int[][] encodedMaskArray = mParser.getInt2DArrayFromObject(MASKINFO_TAG, MASKINFO_MASK);
        if (encodedMaskArray == null) {
            Log.d(TAG, "<getMaskBuffer> Json mask array is null, return null!!");
            return null;
        }
        return decodeMaskBuffer(encodedMaskArray, mMaskSize);
    }

    /**
     * get posX.
     *
     * @return posX
     */
    public int getPosX() {
        if (mPosX != -1) {
            return mPosX;
        }
        mPosX = mParser.getValueIntFromObject(POSINFO_TAG, null, POSINFO_X);
        Log.d(TAG, "<getPosX> mPosX: " + mPosX);
        return mPosX;
    }

    /**
     * get posY.
     *
     * @return posY
     */
    public int getPosY() {
        if (mPosY != -1) {
            return mPosY;
        }
        mPosY = mParser.getValueIntFromObject(POSINFO_TAG, null, POSINFO_Y);
        Log.d(TAG, "<getPosY> mPosY: " + mPosY);
        return mPosY;
    }

    /**
     * get view width.
     *
     * @return view width
     */
    public int getViewWidth() {
        if (mViewWidth != -1) {
            return mViewWidth;
        }
        mViewWidth = mParser.getValueIntFromObject(VIEWINFO_TAG, null, VIEWINFO_WIDTH);
        Log.d(TAG, "<getViewWidth> mViewWidth: " + mViewWidth);
        return mViewWidth;
    }

    /**
     * get view height.
     *
     * @return view height
     */
    public int getViewHeight() {
        if (mViewHeight != -1) {
            return mViewHeight;
        }
        mViewHeight = mParser.getValueIntFromObject(VIEWINFO_TAG, null, VIEWINFO_HEIGHT);
        Log.d(TAG, "<getViewHeight> mViewHeight: " + mViewHeight);
        return mViewHeight;
    }

    /**
     * get orientation.
     *
     * @return orientation
     */
    public int getOrientation() {
        if (mOrientation != -1) {
            return mOrientation;
        }
        mOrientation = mParser.getValueIntFromObject(ORIENTATIONINFO_TAG, null,
                ORIENTATIONINFO_ORIENTATION);
        Log.d(TAG, "<getOrientation> mOrientation: " + mOrientation);
        return mOrientation;
    }

    /**
     * get depth rotation.
     *
     * @return depth rotation
     */
    public int getDepthRotation() {
        if (mDepthRotation != -1) {
            return mDepthRotation;
        }
        mDepthRotation = mParser.getValueIntFromObject(DEPTH_ROTATION_INFO_TAG, null,
                DEPTH_ROTATION_INFO_ORIENTATION);
        Log.d(TAG, "<getDepthRotation> mDepthRotation: " + mDepthRotation);
        return mDepthRotation;
    }

    /**
     * get main camera position.
     *
     * @return main camera position
     */
    public int getMainCamPos() {
        if (mMainCamPostion != -1) {
            return mMainCamPostion;
        }
        mMainCamPostion = mParser.getValueIntFromObject(MAIN_CAM_POSITION_INFO_TAG, null,
                MAIN_CAM_POSITION_INFO_POSITION);
        Log.d(TAG, "<getMainCamPos> mMainCamPostion: " + mMainCamPostion);
        return mMainCamPostion;
    }

    /**
     * get first touch x coordinates.
     *
     * @return first touch x coordinates
     */
    public int getTouchCoordX1st() {
        if (mTouchCoordX1st != -1) {
            return mTouchCoordX1st;
        }
        int left = mParser.getValueIntFromObject(TOUCH_COORD_INFO_TAG, null, TOUCH_COORD_INFO_LEFT);
        int right = mParser.getValueIntFromObject(TOUCH_COORD_INFO_TAG, null,
                TOUCH_COORD_INFO_RIGHT);
        mTouchCoordX1st = (left + right) / 2;
        Log.d(TAG, "<getTouchCoordX1st> mTouchCoordX1st: " + mTouchCoordX1st);
        return mTouchCoordX1st;
    }

    /**
     * get first touch y coordinates.
     *
     * @return first touch y coordinates
     */
    public int getTouchCoordY1st() {
        if (mTouchCoordY1st != -1) {
            return mTouchCoordY1st;
        }
        int top = mParser.getValueIntFromObject(TOUCH_COORD_INFO_TAG, null, TOUCH_COORD_INFO_TOP);
        int bottom = mParser.getValueIntFromObject(TOUCH_COORD_INFO_TAG, null,
                TOUCH_COORD_INFO_BOTTOM);
        mTouchCoordY1st = (top + bottom) / 2;
        Log.d(TAG, "<getTouchCoordY1st> mTouchCoordY1st: " + mTouchCoordY1st);
        return mTouchCoordY1st;
    }

    /**
     * get GEO verify level.
     *
     * @return GEO verify level
     */
    public int getGeoVerifyLevel() {
        return mParser.getValueIntFromObject(VERIFY_GEO_INFO_TAG, null, VERIFY_GEO_INFO_LEVEL);
    }

    /**
     * get GEO verify data.
     *
     * @return GEO verify data
     */
    public int[] getGeoVerifyData() {
        return mParser.getIntArrayFromObject(VERIFY_GEO_INFO_TAG, VERIFY_GEO_INFO_STATISTICS);
    }

    /**
     * get photo verify level.
     *
     * @return photo verify level
     */
    public int getPhoVerifyLevel() {
        return mParser.getValueIntFromObject(VERIFY_PHO_INFO_TAG, null, VERIFY_PHO_INFO_LEVEL);
    }

    /**
     * get photo verify data.
     *
     * @return photo verify data
     */
    public int[] getPhoVerifyData() {
        return mParser.getIntArrayFromObject(VERIFY_PHO_INFO_TAG, VERIFY_PHO_INFO_STATISTICS);
    }

    /**
     * get MTKCHA verify level.
     *
     * @return MTKCHA verify level
     */
    public int getMtkChaVerifyLevel() {
        return mParser.getValueIntFromObject(VERIFY_MTK_CHA_INFO_TAG, null,
                VERIFY_MTK_CHA_INFO_LEVEL);
    }

    /**
     * get MTKCHA verify data.
     *
     * @return MTKCHA verify data
     */
    public int[] getMtkChaVerifyData() {
        return mParser.getIntArrayFromObject(VERIFY_MTK_CHA_INFO_TAG,
                VERIFY_MTK_CHA_INFO_STATISTICS);
    }

    /**
     * get face count.
     *
     * @return face count
     */
    public int getFaceRectCount() {
        if (mFaceRectCount != -1) {
            return mFaceRectCount;
        }
        mFaceRectCount = mParser.getArrayLength(FACE_DETECTION_INFO_TAG);
        Log.d(TAG, "<getFaceRectCount> mFaceRectCount: " + mFaceRectCount);
        return mFaceRectCount;
    }

    /**
     * get face region.
     *
     * @param index
     *            index
     * @return face region
     */
    public Rect getFaceRect(int index) {
        int left = mParser.getObjectPropertyValueFromArray(FACE_DETECTION_INFO_TAG, index,
                FACE_DETECTION_INFO_LEFT);
        int top = mParser.getObjectPropertyValueFromArray(FACE_DETECTION_INFO_TAG, index,
                FACE_DETECTION_INFO_TOP);
        int right = mParser.getObjectPropertyValueFromArray(FACE_DETECTION_INFO_TAG, index,
                FACE_DETECTION_INFO_RIGHT);
        int bottom = mParser.getObjectPropertyValueFromArray(FACE_DETECTION_INFO_TAG, index,
                FACE_DETECTION_INFO_BOTTOM);
        if (left == -1 || top == -1 || right == -1 || bottom == -1) {
            Log.d(TAG,
                    "<getFaceRect> error: left == -1 || top == -1 || right == -1 || bottom == -1");
            return null;
        }
        return new Rect(left, top, right, bottom);
    }

    /**
     * get face rip.
     *
     * @param index
     *            face rip
     * @return face rip
     */
    public int getFaceRip(int index) {
        return mParser.getObjectPropertyValueFromArray(FACE_DETECTION_INFO_TAG, index,
                FACE_DETECTION_INFO_RIP);
    }

    /**
     * Get BlurAtInfinity.
     *
     * @return BlurAtInfinity
     */
    public double getGFocusBlurAtInfinity() {
        double blurAtInfinity = mParser.getValueDoubleFromObject(GFOCUSINFO_TAG, null,
                GFOCUSINFO_BLUR_AT_INFINITY);
        Log.d(TAG, "<getGFocusBlurAtInfinity>  " + blurAtInfinity);
        return blurAtInfinity;
    }

    /**
     * Get FocalDistance.
     *
     * @return FocalDistance
     */
    public double getGFocusFocalDistance() {
        double focalDistance = mParser.getValueDoubleFromObject(GFOCUSINFO_TAG, null,
                GFOCUSINFO_FOCAL_DISTANCE);
        Log.d(TAG, "<getGFocusFocalDistance>  " + focalDistance);
        return focalDistance;
    }

    /**
     * Get FocalPointX.
     *
     * @return FocalPointX
     */
    public double getGFocusFocalPointX() {
        double focalPointX = mParser.getValueDoubleFromObject(GFOCUSINFO_TAG, null,
                GFOCUSINFO_FOCAL_POINT_X);
        Log.d(TAG, "<getGFocusFocalPointX>  " + focalPointX);
        return focalPointX;
    }

    /**
     * Get FocalPointY.
     *
     * @return FocalPointY
     */
    public double getGFocusFocalPointY() {
        double focalPointY = mParser.getValueDoubleFromObject(GFOCUSINFO_TAG, null,
                GFOCUSINFO_FOCAL_POINT_Y);
        Log.d(TAG, "<getGFocusFocalPointY>  " + focalPointY);
        return focalPointY;
    }

    /**
     * Get ImageMime.
     *
     * @return ImageMime
     */
    public String getGImageMime() {
        String gImageMime = mParser.getValueStringFromObject(GIMAGEINFO_TAG, null, GIMAGEINFO_MIME);
        Log.d(TAG, "<getGImageMime>  " + gImageMime);
        return gImageMime;
    }

    /**
     * Get DepthFormat.
     *
     * @return DepthFormat
     */
    public String getGDepthFormat() {
        String gDepthFormat = mParser.getValueStringFromObject(GDEPTHINFO_TAG, null,
                GDEPTHINFO_FORMAT);
        Log.d(TAG, "<getGDepthFormat>  " + gDepthFormat);
        return gDepthFormat;
    }

    /**
     * Get DepthNear.
     *
     * @return DepthNear
     */
    public double getGDepthNear() {
        double gDepthNear = mParser.getValueIntFromObject(GDEPTHINFO_TAG, null,
                GDEPTHINFO_NEAR);
        Log.d(TAG, "<getGDepthNear>  " + gDepthNear);
        return gDepthNear;
    }

    /**
     * Get DepthFar.
     *
     * @return DepthFar
     */
    public double getGDepthFar() {
        double gDepthFar = mParser.getValueIntFromObject(GDEPTHINFO_TAG, null,
                GDEPTHINFO_FAR);
        Log.d(TAG, "<getGDepthFar>  " + gDepthFar);
        return gDepthFar;
    }

    /**
     * Get DepthMime.
     *
     * @return DepthMime
     */
    public String getGDepthMime() {
        String gDepthMime = mParser.getValueStringFromObject(GDEPTHINFO_TAG, null,
                GDEPTHINFO_MIME);
        Log.d(TAG, "<getGDepthMime>  " + gDepthMime);
        return gDepthMime;
    }

    /**
     * Get DOF value.
     *
     * @return dof value
     */
    public int getDof() {
        int dof = mParser.getValueIntFromObject(null, null,
                DOF_LEVEL_TAG);
        Log.d(TAG, "<getDof>  " + dof);
        return dof;
    }

    /**
     * Get ldc width.
     *
     * @return ldc width
     */
    public int getLdcWidth() {
        int ldcWidth = mParser.getValueIntFromObject(LDCINFO_TAG, null, LDCINFO_WIDTH);
        Log.d(TAG, "<getLdcWidth> ldcWidth: " + ldcWidth);
        return ldcWidth;
    }

    /**
     * get ldc height.
     *
     * @return ldc height
     */
    public int getLdcHeight() {
        int ldcHeight = mParser.getValueIntFromObject(LDCINFO_TAG, null, LDCINFO_HEIGHT);
        Log.d(TAG, "<getLdcHeight> ldcHeight: " + ldcHeight);
        return ldcHeight;
    }

    private byte[] decodeMaskBuffer(int[][] encodedMaskArray, int maskSize) {
        int startIndex = 0;
        int endIndex = 0;
        byte[] maskBuffer = new byte[maskSize];
        long begin = System.currentTimeMillis();
        // clear buffer
        for (int i = 0; i < maskSize; i++) {
            maskBuffer[i] = 0;
        }
        // just set valid mask to 0xff
        for (int i = 0; i < encodedMaskArray.length; i++) {
            startIndex = encodedMaskArray[i][0];
            endIndex = startIndex + encodedMaskArray[i][1];
            if (startIndex > maskSize || startIndex < 0 || endIndex < 0 || endIndex > maskSize) {
                Log.d(TAG, "<decodeMaskBuffer> error, startIndex: " + startIndex + ", endIndex: "
                        + endIndex + ", maskSize: " + maskSize);
                return null;
            }
            for (int j = startIndex; j < endIndex; j++) {
                maskBuffer[j] = (byte) VALID_MASK;
            }
        }
        long end = System.currentTimeMillis();
        Log.d(TAG, "<decodeMaskBuffer> performance, decode mask costs: " + (end - begin));
        return maskBuffer;
    }
}
