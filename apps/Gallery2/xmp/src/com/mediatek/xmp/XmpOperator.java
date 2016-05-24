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
package com.mediatek.xmp;

import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;

import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;

import com.mediatek.xmp.SegmentMaskOperator.SegmentMaskInfo;
import com.mediatek.xmp.XmpInterface.ByteArrayInputStreamExt;
import com.mediatek.xmp.XmpInterface.ByteArrayOutputStreamExt;
import com.mediatek.xmp.XmpInterface.Section;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

/**
 * XmpOperator.
 *
 */
public class XmpOperator {
    private final static String TAG = "MtkGallery2/Xmp/XmpOperator";
    private static final boolean ENABLE_BUFFER_DUMP;
    private static final String DUMP_PATH =
            Environment.getExternalStorageDirectory().getPath() + "/";
    private static final String DUMP_FOLDER_NAME = "dump_jps_buffer" + "/";
    private static final String CFG_FILE_NAME = "dumpjps";

    private static final String NS_GFOCUS = "http://ns.google.com/photos/1.0/focus/";
    private static final String NS_GIMAGE = "http://ns.google.com/photos/1.0/image/";
    private static final String NS_GDEPTH = "http://ns.google.com/photos/1.0/depthmap/";
    private static final String NS_XMPNOTE = "http://ns.adobe.com/xmp/note/";

    private static final String PRIFIX_GFOCUS = "GFocus";
    private static final String PRIFIX_GIMAGE = "GImage";
    private static final String PRIFIX_GDEPTH = "GDepth";
    private static final String PRIFIX_XMPNOTE = "xmpNote";

    private static final String ATTRIBUTE_GFOCUS_BLUR_INFINITY = "BlurAtInfinity";
    private static final String ATTRIBUTE_GFOCUS_FOCALDISTANCE = "FocalDistance";
    private static final String ATTRIBUTE_GFOCUS_FOCALPOINTX = "FocalPointX";
    private static final String ATTRIBUTE_GFOCUS_FOCALPOINTY = "FocalPointY";
    private static final String ATTRIBUTE_GIMAGE_DATA = "Data";
    private static final String ATTRIBUTE_GDEPTH_DATA = "Data";

    private static final String ATTRIBUTE_GIMAGE_MIME = "Mime";
    private static final String ATTRIBUTE_GDEPTH_FORMAT = "Format";
    private static final String ATTRIBUTE_GDEPTH_NEAR = "Near";
    private static final String ATTRIBUTE_GDEPTH_FAR = "Far";
    private static final String ATTRIBUTE_GDEPTH_MIME = "Mime";

    private static final String ATTRIBUTE_XMPNOTE = "HasExtendedXMP";
    private static final String MIME_JPEG = "image/jpeg";
    private static final String DEPTH_FORMAT = "RangeInverse";
    private static final String MIME_PNG = "image/png";

    private static final String MEDIATEK_IMAGE_REFOCUS_NAMESPACE =
            "http://ns.mediatek.com/refocus/";
    private static final String MTK_REFOCUS_PREFIX = "MRefocus";
    private static final String JPS_WIDTH = "JpsWidth";
    private static final String JPS_HEIGHT = "JpsHeight";
    private static final String MASK_WIDTH = "MaskWidth";
    private static final String MASK_HEIGHT = "MaskHeight";
    private static final String POS_X = "PosX";
    private static final String POS_Y = "PosY";
    private static final String VIEW_WIDTH = "ViewWidth";
    private static final String VIEW_HEIGHT = "ViewHeight";
    private static final String ORIENTATION = "Orientation";
    private static final String DEPTH_ROTATION = "DepthRotation";
    private static final String MAIN_CAM_POS = "MainCamPos";
    private static final String TOUCH_COORDX_1ST = "TouchCoordX1st";
    private static final String TOUCH_COORDY_1ST = "TouchCoordY1st";
    private static final String DOF_LEVEL = "DOF";
    private static final String LDC = "LDC";
    private static final String LDC_WIDTH = "LdcWidth";
    private static final String LDC_HEIGHT = "LdcHeight";

    private static final String DEPTH_BUFFER_WIDTH = "DepthBufferWidth";
    private static final String DEPTH_BUFFER_HEIGHT = "DepthBufferHeight";
    private static final String XMP_DEPTH_WIDTH = "XmpDepthWidth";
    private static final String XMP_DEPTH_HEIGHT = "XmpDepthHeight";
    private static final String META_BUFFER_WIDTH = "MetaBufferWidth";
    private static final String META_BUFFER_HEIGHT = "MetaBufferHeight";
    private static final String TOUCH_COORDX_LAST = "TouchCoordXLast";
    private static final String TOUCH_COORDY_LAST = "TouchCoordYLast";
    private static final String DEPTH_OF_FIELD_LAST = "DepthOfFieldLast";
    private static final String DEPTH_BUFFER_FLAG = "DepthBufferFlag";
    private static final String XMP_DEPTH_FLAG = "XmpDepthFlag";

    private static final double DEFAULT_VALUE = 1.0;
    private static final int JPG_SUFIX_LEN = 3;
    private static final int INDEX_0 = 0;
    private static final int INDEX_1 = 1;
    private static final int INDEX_2 = 2;
    private static final int INDEX_3 = 3;

    private ArrayList<Section> mParsedSectionsForCamera;
    private ArrayList<Section> mParsedSectionsForGallery;

    private XmpInterface mXmpInterface;
    private SegmentMaskOperator mSegmentMaskOperator;
    private StereoDebugInfoParser mStereoDebugInfoParser;
    private String mFileName;
    private String mValueOfMd5;
    private byte[] mExtendXmpData;
    private boolean mNeedExtXmp;

    static {
        File xmpCfg = new File(DUMP_PATH + CFG_FILE_NAME);
        if (xmpCfg.exists()) {
            ENABLE_BUFFER_DUMP = true;
        } else {
            ENABLE_BUFFER_DUMP = false;
        }
        if (ENABLE_BUFFER_DUMP) {
            makeDir(DUMP_PATH + DUMP_FOLDER_NAME);
        }
        Log.d(TAG, "ENABLE_BUFFER_DUMP: " + ENABLE_BUFFER_DUMP + ", DUMP_PATH: " + DUMP_PATH);
    }

    /**
     * StereoDebugInfo.
     *
     */
    public static class StereoDebugInfo {
        public int jpsWidth;
        public int jpsHeight;
        public int maskWidth;
        public int maskHeight;
        public int posX;
        public int posY;
        public int viewWidth;
        public int viewHeight;
        public int orientation;
        public int depthRotation;
        public int mainCamPos;
        public int touchCoordX1st;
        public int touchCoordY1st;
        public int faceCount;
        public ArrayList<FaceDetectionInfo> fdInfoArray;
        public int dofLevel;
        public int ldcWidth;
        public int ldcHeight;

        // google params
        public double gFocusBlurAtInfinity;
        public double gFocusFocalDistance;
        public double gFocusFocalPointX;
        public double gFocusFocalPointY;
        public String gImageMime;
        public String gDepthFormat;
        public double gDepthNear;
        public double gDepthFar;
        public String gDepthMime;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ConfigInfo:");
            sb.append("\n    jpsWidth = 0x" + Integer.toHexString(jpsWidth) + "(" + jpsWidth + ")");
            sb.append("\n    jpsHeight = 0x" + Integer.toHexString(jpsHeight) + "(" + jpsHeight
                    + ")");
            sb.append("\n    maskWidth = 0x" + Integer.toHexString(maskWidth) + "(" + maskWidth
                    + ")");
            sb.append("\n    maskHeight = 0x" + Integer.toHexString(maskHeight) + "(" + maskHeight
                    + ")");
            sb.append("\n    posX = 0x" + Integer.toHexString(posX) + "(" + posX + ")");
            sb.append("\n    posY = 0x" + Integer.toHexString(posY) + "(" + posY + ")");
            sb.append("\n    viewWidth = 0x" + Integer.toHexString(viewWidth) + "(" + viewWidth
                    + ")");
            sb.append("\n    viewHeight = 0x" + Integer.toHexString(viewHeight) + "(" + viewHeight
                    + ")");
            sb.append("\n    orientation = 0x" + Integer.toHexString(orientation) + "("
                    + orientation + ")");
            sb.append("\n    depthRotation = 0x" + Integer.toHexString(depthRotation) + "("
                    + depthRotation + ")");
            sb.append("\n    mainCamPos = 0x" + Integer.toHexString(mainCamPos) + "(" + mainCamPos
                    + ")");
            sb.append("\n    touchCoordX1st = 0x" + Integer.toHexString(touchCoordX1st) + "("
                    + touchCoordX1st + ")");
            sb.append("\n    touchCoordY1st = 0x" + Integer.toHexString(touchCoordY1st) + "("
                    + touchCoordY1st + ")");
            sb.append("\n    faceCount = 0x" + Integer.toHexString(faceCount) + "(" + faceCount
                    + ")");
            for (int i = 0; i < fdInfoArray.size(); i++) {
                sb.append("\n    face " + i + ": " + fdInfoArray.get(i));
            }
            sb.append("\n    dofLevel = 0x" + Integer.toHexString(dofLevel) + "(" + dofLevel
                    + ")");
            sb.append("\n    ldcWidth = 0x" + Integer.toHexString(ldcWidth) + "(" + ldcWidth
                    + ")");
            sb.append("\n    ldcHeight = 0x" + Integer.toHexString(ldcHeight) + "(" + ldcHeight
                    + ")");
            if (gFocusBlurAtInfinity != -1) {
                sb.append("\n    ------------------------------------------");
                sb.append("\n    gFocusBlurAtInfinity = " + gFocusBlurAtInfinity);
                sb.append("\n    gFocusFocalDistance = " + gFocusFocalDistance);
                sb.append("\n    gFocusFocalPointX = " + gFocusFocalPointX);
                sb.append("\n    gFocusFocalPointY = " + gFocusFocalPointY);
                sb.append("\n    gImageMime = " + gImageMime);
                sb.append("\n    gDepthFormat = " + gDepthFormat);
                sb.append("\n    gDepthNear = " + gDepthNear);
                sb.append("\n    gDepthFar = " + gDepthFar);
                sb.append("\n    gDepthMime = " + gDepthMime);
            }
            return sb.toString();
        }
    }

    /**
     * FaceDetectionInfo.
     *
     */
    public static class FaceDetectionInfo {
        public int mFaceLeft;
        public int mFaceTop;
        public int mFaceRight;
        public int mFaceBottom;
        public int mFaceRip;

        /**
         * FaceDetectionInfo.
         *
         * @param faceLeft
         *            faceLeft
         * @param faceTop
         *            faceTop
         * @param faceRight
         *            faceRight
         * @param faceBottom
         *            faceBottom
         * @param faceRip
         *            faceRip
         */
        public FaceDetectionInfo(int faceLeft, int faceTop, int faceRight, int faceBottom,
                int faceRip) {
            mFaceLeft = faceLeft;
            mFaceTop = faceTop;
            mFaceRight = faceRight;
            mFaceBottom = faceBottom;
            mFaceRip = faceRip;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("FaceDetectionInfo:");
            sb.append("\n    mFaceLeft = " + mFaceLeft);
            sb.append("\n    mFaceTop = " + mFaceTop);
            sb.append("\n    mFaceRight = " + mFaceRight);
            sb.append("\n    mFaceBottom = " + mFaceBottom);
            sb.append("\n    mFaceRip = " + mFaceRip);
            return sb.toString();
        }
    }

    /**
     * DepthBufferInfo.
     *
     */
    public static class DepthBufferInfo {
        // write below params to xmp main
        public boolean depthBufferFlag;
        public int depthBufferWidth;
        public int depthBufferHeight;
        public boolean xmpDepthFlag;
        public int xmpDepthWidth;
        public int xmpDepthHeight;
        public int metaBufferWidth;
        public int metaBufferHeight;
        public int touchCoordXLast;
        public int touchCoordYLast;
        public int depthOfFieldLast;
        // write below buffer to app1
        public byte[] depthData;
        public byte[] xmpDepthData;

        /**
         * DepthBufferInfo.
         */
        public DepthBufferInfo() {
            depthBufferFlag = false;
            depthBufferWidth = -1;
            depthBufferHeight = -1;
            xmpDepthFlag = false;
            xmpDepthWidth = -1;
            xmpDepthHeight = -1;
            metaBufferWidth = -1;
            metaBufferHeight = -1;
            touchCoordXLast = -1;
            touchCoordYLast = -1;
            depthOfFieldLast = -1;
            depthData = null;
            xmpDepthData = null;
        }

        /**
         * DepthBufferInfo.
         *
         * @param depthBufferFlag
         *            depthBufferFlag
         * @param depthData
         *            depthData
         * @param depthBufferWidth
         *            depthBufferWidth
         * @param depthBufferHeight
         *            depthBufferHeight
         * @param xmpDepthFlag
         *            xmpDepthFlag
         * @param xmpDepthData
         *            xmpDepthData
         * @param xmpDepthWidth
         *            xmpDepthWidth
         * @param xmpDepthHeight
         *            xmpDepthHeight
         * @param touchCoordXLast
         *            touchCoordXLast
         * @param touchCoordYLast
         *            touchCoordYLast
         * @param depthOfFieldLast
         *            depthOfFieldLast
         */
        public DepthBufferInfo(boolean depthBufferFlag, byte[] depthData, int depthBufferWidth,
                int depthBufferHeight, boolean xmpDepthFlag, byte[] xmpDepthData,
                int xmpDepthWidth, int xmpDepthHeight, int touchCoordXLast, int touchCoordYLast,
                int depthOfFieldLast) {
            this.depthBufferFlag = depthBufferFlag;
            this.depthBufferWidth = depthBufferWidth;
            this.depthBufferHeight = depthBufferHeight;
            this.xmpDepthFlag = xmpDepthFlag;
            this.xmpDepthWidth = xmpDepthWidth;
            this.xmpDepthHeight = xmpDepthHeight;
            this.touchCoordXLast = touchCoordXLast;
            this.touchCoordYLast = touchCoordYLast;
            this.depthOfFieldLast = depthOfFieldLast;
            this.depthData = depthData;
            this.xmpDepthData = xmpDepthData;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("DepthBufferInfo:");
            sb.append("\n    depthBufferFlag = " + depthBufferFlag);
            sb.append("\n    xmpDepthFlag = " + xmpDepthFlag);
            sb.append("\n    depthBufferWidth = 0x" + Integer.toHexString(depthBufferWidth) + "("
                    + depthBufferWidth + ")");
            sb.append("\n    depthBufferHeight = 0x" + Integer.toHexString(depthBufferHeight) + "("
                    + depthBufferHeight + ")");
            sb.append("\n    xmpDepthWidth = 0x" + Integer.toHexString(xmpDepthWidth) + "("
                    + xmpDepthWidth + ")");
            sb.append("\n    xmpDepthHeight = 0x" + Integer.toHexString(xmpDepthHeight) + "("
                    + xmpDepthHeight + ")");
            sb.append("\n    metaBufferWidth = 0x" + Integer.toHexString(metaBufferWidth) + "("
                    + metaBufferWidth + ")");
            sb.append("\n    metaBufferHeight = 0x" + Integer.toHexString(metaBufferHeight) + "("
                    + metaBufferHeight + ")");
            sb.append("\n    touchCoordXLast = 0x" + Integer.toHexString(touchCoordXLast) + "("
                    + touchCoordXLast + ")");
            sb.append("\n    touchCoordYLast = 0x" + Integer.toHexString(touchCoordYLast) + "("
                    + touchCoordYLast + ")");
            sb.append("\n    depthOfFieldLast = 0x" + Integer.toHexString(depthOfFieldLast) + "("
                    + depthOfFieldLast + ")");
            if (depthData != null) {
                sb.append("\n    depthData length = 0x" + Integer.toHexString(depthData.length)
                        + "(" + depthData.length + ")");
            } else {
                sb.append("\n    depthData = null");
            }
            if (xmpDepthData != null) {
                sb.append("\n    xmpDepthData length = 0x"
                        + Integer.toHexString(xmpDepthData.length) + "(" + xmpDepthData.length
                        + ")");
            } else {
                sb.append("\n    xmpDepthData = null");
            }
            return sb.toString();
        }
    }

    /**
     * XmpOperator.
     */
    public XmpOperator() {
        mXmpInterface = new XmpInterface();
        mSegmentMaskOperator = new SegmentMaskOperator(mXmpInterface);
    }

    /**
     * initialize.
     *
     * @param srcFilePath
     *            file path
     * @return success->true, fail->false
     */
    public boolean initialize(String srcFilePath) {
        File srcFile = new File(srcFilePath);
        if (!srcFile.exists()) {
            Log.d(TAG, "<initialize> " + srcFilePath + " not exists!!!");
            return false;
        }
        String fileFormat = srcFilePath.substring(srcFilePath.length() - JPG_SUFIX_LEN, srcFilePath
                .length());
        Log.d(TAG, "<initialize> srcFilePath " + srcFilePath);
        if (!"JPG".equalsIgnoreCase(fileFormat)) {
            Log.d(TAG, "<initialize> " + srcFilePath + " is not JPG!!!");
            return false;
        }
        mFileName = getFileNameFromPath(srcFilePath) + "/";
        makeDir(DUMP_PATH + DUMP_FOLDER_NAME + mFileName);
        if (ENABLE_BUFFER_DUMP) {
            mSegmentMaskOperator.enableDump(ENABLE_BUFFER_DUMP, DUMP_PATH + DUMP_FOLDER_NAME
                    + mFileName);
        }
        mParsedSectionsForGallery = mXmpInterface.parseAppInfo(srcFilePath);
        if (mParsedSectionsForGallery == null) {
            return false;
        }
        mXmpInterface.setSectionInfo(mParsedSectionsForGallery);
        mSegmentMaskOperator.setSectionInfo(mParsedSectionsForGallery);
        return true;
    }

    /**
     * deInitialize.
     */
    public void deInitialize() {
        mParsedSectionsForGallery = null;
    }

    /**
     * getJpsDataFromJpgFile.
     *
     * @param filePath
     *            file path
     * @return jps data
     */
    public byte[] getJpsDataFromJpgFile(String filePath) {
        RandomAccessFile rafIn = null;
        try {
            rafIn = new RandomAccessFile(filePath, "r");
            Log.d(TAG, "<getJpsDataFromJpgFile> begin!!! ");
            byte[] out = getJpsInfoFromSections(rafIn, true);
            if (ENABLE_BUFFER_DUMP) {
                XmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                        + "Jps_Read.jpg", out);
            }
            Log.d(TAG, "<getJpsDataFromJpgFile> end!!! ");
            return out;
        } catch (IOException e) {
            Log.e(TAG, "<getJpsDataFromJpgFile> IOException ", e);
            return null;
        } finally {
            closeRandomAccessFile(rafIn);
        }
    }

    /**
     * getJpsMaskFromJpgFile.
     *
     * @param filePath
     *            file path
     * @return mask
     */
    public byte[] getJpsMaskFromJpgFile(String filePath) {
        RandomAccessFile rafIn = null;
        try {
            rafIn = new RandomAccessFile(filePath, "r");
            Log.d(TAG, "<getJpsMaskFromJpgFile> begin!!! ");
            byte[] out = getJpsInfoFromSections(rafIn, false);
            if (ENABLE_BUFFER_DUMP) {
                XmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                        + "Mask_Read.raw", out);
            }
            Log.d(TAG, "<getJpsMaskFromJpgFile> end!!! ");
            return out;
        } catch (IOException e) {
            Log.e(TAG, "<getJpsMaskFromJpgFile> IOException ", e);
            return null;
        } finally {
            closeRandomAccessFile(rafIn);
        }
    }

    /**
     * writeDepthBufferToJpg.
     *
     * @param srcFilePath
     *            file path
     * @param depthBufferInfo
     *            depth buffer
     * @param deleteJps
     *            jps
     * @return success->true,fail->false
     */
    public boolean writeDepthBufferToJpg(String srcFilePath, DepthBufferInfo depthBufferInfo,
            boolean deleteJps) {
        String tempPath = srcFilePath + ".tmp";
        boolean result = writeDepthBufferToJpg(srcFilePath, tempPath, depthBufferInfo,
                /* !ENABLE_BUFFER_DUMP */false);
        // delete source file and rename new file to source file
        Log.d(TAG, "<writeDepthBufferToJpg> delete src file and rename back!!!");
        File srcFile = new File(srcFilePath);
        File outFile = new File(tempPath);
        srcFile.delete();
        outFile.renameTo(srcFile);
        Log.d(TAG, "<writeDepthBufferToJpg> refresh app sections!!!");
        mParsedSectionsForGallery = mXmpInterface.parseAppInfo(srcFilePath);
        mXmpInterface.setSectionInfo(mParsedSectionsForGallery);
        if (ENABLE_BUFFER_DUMP && depthBufferInfo.xmpDepthData != null) {
            byte[] gDepthMapRead = getGoogleDepthMap(srcFilePath);
            XmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                    + "Google_DepthMap_Large_Read.raw", gDepthMapRead);
        }
        return result;
    }

    /**
     * writeSegmentMaskInfoToJpg.
     *
     * @param srcFilePath
     *            file path
     * @param maskInfo
     *            mask
     * @return success->true,fail->false
     */
    public boolean writeSegmentMaskInfoToJpg(String srcFilePath, SegmentMaskInfo maskInfo) {
        return mSegmentMaskOperator.writeSegmentMaskInfoToJpg(srcFilePath, maskInfo);
    }

    /**
     * getSegmentMaskInfoFromFile.
     *
     * @param filePath
     *            file path
     * @return segment mask
     */
    public SegmentMaskInfo getSegmentMaskInfoFromFile(String filePath) {
        return mSegmentMaskOperator.getSegmentMaskInfoFromFile(filePath);
    }

    /**
     * Camera needs parse debug info, debug tool's using.
     *
     * @param jsonBuffer
     *            json buffer
     */
    public void setJsonBuffer(byte[] jsonBuffer) {
        if (jsonBuffer == null) {
            Log.d(TAG, "<setJsonBuffer> jsonBuffer is null!!");
            return;
        }
        mStereoDebugInfoParser = new StereoDebugInfoParser(jsonBuffer);
    }

    /**
     * Get Geo verify level, debug tool's using.
     *
     * @return Geo verify level
     */
    public int getGeoVerifyLevel() {
        if (mStereoDebugInfoParser != null) {
            return mStereoDebugInfoParser.getGeoVerifyLevel();
        }
        return -1;
    }

    /**
     * Get Pho verify level, debug tool's using.
     *
     * @return Pho verify level.
     */
    public int getPhoVerifyLevel() {
        if (mStereoDebugInfoParser != null) {
            return mStereoDebugInfoParser.getPhoVerifyLevel();
        }
        return -1;
    }

    /**
     * Get Cha verify level, debug tool's using.
     *
     * @return Cha verify level.
     */
    public int getMtkChaVerifyLevel() {
        if (mStereoDebugInfoParser != null) {
            return mStereoDebugInfoParser.getMtkChaVerifyLevel();
        }
        return -1;
    }

    /**
     * Write stereo capture info into jpg.
     * if clearImage & gDepthMapSmall == null, for jade
     * @param fileName
     *            file name
     * @param jpgBuffer
     *            jpg
     * @param jpsData
     *            jps
     * @param jsonBuffer
     *            debug info && mask
     * @param clearImage
     *            clear image
     * @param gDepthMapSmall
     *            small depth buffer
     * @return success->true,fail->false
     */
    public byte[] writeStereoCaptureInfoToJpg(String fileName, byte[] jpgBuffer, byte[] jpsData,
            byte[] jsonBuffer, byte[] clearImage, byte[] gDepthMapSmall) {
        return writeStereoCaptureInfoToJpg(fileName, jpgBuffer, jpsData, jsonBuffer, clearImage,
                gDepthMapSmall, null);
    }

    /**
     * Write stereo capture info into jpg.
     * if clearImage & gDepthMapSmall == null, for jade
     * @param fileName
     *            file name
     * @param jpgBuffer
     *            jpg
     * @param jpsData
     *            jps
     * @param jsonBuffer
     *            debug info && mask
     * @param clearImage
     *            clear image
     * @param gDepthMapSmall
     *            small depth buffer
     * @param ldc
     *            ldc
     * @return success->true,fail->false
     */
    public byte[] writeStereoCaptureInfoToJpg(String fileName, byte[] jpgBuffer, byte[] jpsData,
            byte[] jsonBuffer, byte[] clearImage, byte[] gDepthMapSmall, byte[] ldc) {
        if (jpgBuffer == null || jpsData == null || jsonBuffer == null) {
            Log.d(TAG, "<writeStereoCaptureInfoToJpg> params error!!");
            return null;
        }
        Log.d(TAG, "<writeStereoCaptureInfoToJpg> write begin!!!");
        if (clearImage != null || gDepthMapSmall != null || ldc != null) {
            mNeedExtXmp = true;
        } else {
            Log.d(TAG, "<writeStereoCaptureInfoToJpg> jade, clearImage & gDepthMapSmall is null");
            mNeedExtXmp = false;
        }
        String jsonString = new String(jsonBuffer);
        if (ENABLE_BUFFER_DUMP && fileName != null) {
            mFileName = fileName + "/";
            makeDir(DUMP_PATH + DUMP_FOLDER_NAME + mFileName);
            XmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName + "In.jpg",
                    jpgBuffer);
            XmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                    + "Jps_Written.jpg", jpsData);
            XmpInterface.writeStringToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                    + "Json_Written.txt", jsonString);
            XmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                    + "ClearImage_Written.jpg", clearImage);
            XmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                    + "Google_DepthMap_Small_Written.raw", gDepthMapSmall);
            XmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                    + "ldc_Written.raw", ldc);
        }
        StereoDebugInfoParser parser = new StereoDebugInfoParser(jsonBuffer);
        byte[] jpsMask = parser.getMaskBuffer();
        if (jpsMask == null) {
            Log.d(TAG, "<writeStereoCaptureInfoToJpg> parsed jpsMask is null!!!");
            return null;
        }
        if (ENABLE_BUFFER_DUMP && fileName != null) {
            XmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                    + "Mask_Written.raw", jpsMask);
        }
        byte[] encodedDepth = encodingDepth(gDepthMapSmall, parser);
        ByteArrayInputStreamExt is = new ByteArrayInputStreamExt(jpgBuffer);
        ByteArrayOutputStreamExt os = new ByteArrayOutputStreamExt();
        mParsedSectionsForCamera = mXmpInterface.parseAppInfoFromStream(is);
        mXmpInterface.setSectionInfo(mParsedSectionsForCamera);
        writeStereoCaptureInfo(is, os, jpsData, jsonBuffer, jpsMask, clearImage, encodedDepth,
                ldc);
        byte[] out = os.toByteArray();
        if (ENABLE_BUFFER_DUMP && fileName != null) {
            XmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName + "Out.jpg",
                    out);
            byte[] gDepthMapRead = getGoogleDepthMap(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                    + "Out.jpg");
            XmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                    + "Google_DepthMap_Small_Read.raw", gDepthMapRead);
            byte[] ldcRead = getLdcInfoFrom(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                    + "Out.jpg");
        }
        closeStream(is, os);
        Log.d(TAG, "<writeStereoCaptureInfoToJpg> write end!!!");
        return out;
    }

    /**
     * getDepthBufferInfoFromFile.
     *
     * @param filePath
     *            file path
     * @return depth buffer info
     */
    public DepthBufferInfo getDepthBufferInfoFromFile(String filePath) {
        Log.d(TAG, "<getDepthBufferInfoFromFile> begin!!!");
        XMPMeta meta = mXmpInterface.getXmpMetaFromFile(filePath);
        DepthBufferInfo depthBufferInfo = parseDepthBufferInfo(meta);
        if (depthBufferInfo == null) {
            Log.d(TAG, "<getDepthBufferInfoFromFile> depthBufferInfo is null!!!");
            return null;
        }
        depthBufferInfo.depthData = getDepthDataFromJpgFile(filePath);
        // depthBufferInfo.xmpDepthData = getXmpDepthDataFromJpgFile(filePath);
        Log.d(TAG, "<getDepthBufferInfoFromFile> " + depthBufferInfo);
        if (ENABLE_BUFFER_DUMP && depthBufferInfo.depthData != null) {
            XmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                    + "DepthBuffer_Read.raw", depthBufferInfo.depthData);
        }
        if (ENABLE_BUFFER_DUMP) {
            XmpInterface.writeStringToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                    + "DepthBufferInfo_Read.txt", depthBufferInfo.toString());
        }
        Log.d(TAG, "<getDepthBufferInfoFromFile> end!!!");
        return depthBufferInfo;
    }

    /**
     * Get stereo debug info.
     *
     * @param filePath
     *            file path
     * @return stereo debug info
     */
    public StereoDebugInfo getStereoDebugInfoFromFile(String filePath) {
        XMPMeta meta = mXmpInterface.getXmpMetaFromFile(filePath);
        StereoDebugInfo stereoDebugInfo = parseStereoDebugInfo(meta);
        Log.d(TAG, "<getStereoDebugInfoFromFile> " + stereoDebugInfo);
        if (ENABLE_BUFFER_DUMP) {
            XmpInterface.writeStringToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                    + "Config_Read.txt", stereoDebugInfo.toString());
        }
        return stereoDebugInfo;
    }

    /**
     * Get clear image from extend xmp.
     *
     * Extend xmp section layout: 0xffe1 + len tag + header + 0x0 + md5(32bytes)
     * + ext xmp total len(2 bytes) + offset(2 bytes)
     *
     * @param filePath
     *            file path
     * @return clear image buffer
     */
    public byte[] getClearImage(String filePath) {
        if (filePath == null) {
            Log.d(TAG, "<getClearImage> filePath is null, error!!");
            return null;
        }
        Log.d(TAG, "<getClearImage> begin!!!");
        mExtendXmpData = extractExtendXmpData(filePath);
        if (mExtendXmpData == null) {
            Log.d(TAG, "<getClearImage> mExtendXmpData is null, return null!!");
            return null;
        }
        XmpInterface extXmpInterface = new XmpInterface();
        XMPMeta extXmpMeta = extXmpInterface.getExtXmpMetaFromBuffer(mExtendXmpData);
        if (extXmpMeta == null) {
            Log.d(TAG, "<getClearImage> extXmpMeta is null, return null!!");
            return null;
        }
        byte[] clearImageBuffer = extXmpInterface.getPropertyBase64(extXmpMeta, NS_GIMAGE,
                ATTRIBUTE_GIMAGE_DATA);
        if (ENABLE_BUFFER_DUMP) {
            XmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                    + "ClearImage_Read.jpg", clearImageBuffer);
        }
        Log.d(TAG, "<getClearImage> end!!!");
        return clearImageBuffer;
    }

    /**
     * Get google depth map buffer.
     *
     * @param filePath
     *            file path
     * @return google depth map buffer
     */
    public byte[] getGoogleDepthMap(String filePath) {
        if (filePath == null) {
            Log.d(TAG, "<getGoogleDepthMap> filePath is null, error!!");
            return null;
        }
        Log.d(TAG, "<getGoogleDepthMap> begin!!!");
        mExtendXmpData = extractExtendXmpData(filePath);
        if (mExtendXmpData == null) {
            Log.d(TAG, "<getGoogleDepthMap> mExtendXmpData is null, error!!");
            return null;
        }
        XmpInterface extXmpInterface = new XmpInterface();
        XMPMeta extXmpMeta = extXmpInterface.getExtXmpMetaFromBuffer(mExtendXmpData);
        if (extXmpMeta == null) {
            Log.d(TAG, "<getGoogleDepthMap> extXmpMeta is null, return null!!");
            return null;
        }
        byte[] googleDepthMap = Utils.decodePng(extXmpInterface.getPropertyBase64(extXmpMeta,
                NS_GDEPTH, ATTRIBUTE_GDEPTH_DATA));
        Log.d(TAG, "<getGoogleDepthMap> end!!!");
        return googleDepthMap;
    }

    /**
     * Get LDC from file.
     *
     * @param filePath
     *            input file path
     * @return ldc buffer
     */
    public byte[] getLdcInfoFrom(String filePath) {
        if (filePath == null) {
            Log.d(TAG, "<getLdcInfoFrom> filePath is null, error!!");
            return null;
        }
        Log.d(TAG, "<getLdcInfoFrom> begin!!!");
        mExtendXmpData = extractExtendXmpData(filePath);
        if (mExtendXmpData == null) {
            Log.d(TAG, "<getLdcInfoFrom> mExtendXmpData is null, error!!");
            return null;
        }
        XmpInterface extXmpInterface = new XmpInterface();
        XMPMeta extXmpMeta = extXmpInterface.getExtXmpMetaFromBuffer(mExtendXmpData);
        if (extXmpMeta == null) {
            Log.d(TAG, "<getLdcInfoFrom> extXmpMeta is null, return null!!");
            return null;
        }
        byte[] ldcBuffer = extXmpInterface.getPropertyBase64(extXmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, LDC);
        if (ENABLE_BUFFER_DUMP) {
            XmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                    + "ldc_Read.raw", ldcBuffer);
        }
        Log.d(TAG, "<getLdcInfoFrom> end!!!");
        return ldcBuffer;
    }

    /**
     * Replace clear image and update config in src file with blurImage.
     *
     * @param srcFilePath
     *            src file
     * @param blurImageFilePath
     *            blur image file
     * @return true if success
     */
    public boolean updateConfigAndReplaceBlurImage(String srcFilePath, String blurImageFilePath,
            StereoDebugInfo config) {
        Log.d(TAG, "<updateConfigAndReplaceBlurImage> write begin!!!");
        String tempPath = srcFilePath + ".tmp";
        // delete source file and rename new file to source file
        boolean status = updateConfigAndReplaceBlurImage(srcFilePath, blurImageFilePath, tempPath,
                config);
        Log.d(TAG, "<updateConfigAndReplaceBlurImage> delete src file and rename back!!!");
        File srcFile = new File(srcFilePath);
        File outFile = new File(tempPath);
        srcFile.delete();
        outFile.renameTo(srcFile);
        Log.d(TAG, "<updateConfigAndReplaceBlurImage> refresh app sections!!!");
        mParsedSectionsForGallery = mXmpInterface.parseAppInfo(srcFilePath);
        Log.d(TAG, "<updateConfigAndReplaceBlurImage> write end!!!");
        return status;
    }

    private byte[] updateMainXmpSection(RandomAccessFile rafIn, StereoDebugInfo config,
            Section mainXmpSection) {
        XmpInterface xmpInterface = new XmpInterface();
        XMPMeta meta = xmpInterface.getXmpMetaFromSection(rafIn, mainXmpSection);
        if (meta == null) {
            Log.d(TAG, "<updateMainXmpSection> meta is null, error!!");
            return null;
        }
        // just update "TOUCH_COORDX_1ST / TOUCH_COORDY_1ST / DOF_LEVEL"
        xmpInterface.registerNamespace(MEDIATEK_IMAGE_REFOCUS_NAMESPACE, MTK_REFOCUS_PREFIX);
        xmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, TOUCH_COORDX_1ST,
                config.touchCoordX1st);
        xmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, TOUCH_COORDY_1ST,
                config.touchCoordY1st);
        xmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, DOF_LEVEL,
                config.dofLevel);
        byte[] serializedData = xmpInterface.serialize(meta);
        if (serializedData == null) {
            Log.d(TAG, "<updateMainXmpSection> serializedData is null, error!!");
            return null;
        }
        // mainXmpBuffer contains: main xmp header + real main xmp data
        byte[] mainXmpBuffer = new byte[XmpInterface.XMP_HEADER_START.length()
                + serializedData.length];
        System.arraycopy(XmpInterface.XMP_HEADER_START.getBytes(), 0, mainXmpBuffer, 0,
                XmpInterface.XMP_HEADER_START.length());
        System.arraycopy(serializedData, 0, mainXmpBuffer, XmpInterface.XMP_HEADER_START.length(),
                serializedData.length);
        return mainXmpBuffer;
    }

    private boolean replaceMainXmpData(RandomAccessFile rafIn, RandomAccessFile rafOut,
            StereoDebugInfo config, Section mainXmpSection) {
        byte[] mainXmpBuffer = updateMainXmpSection(rafIn, config, mainXmpSection);
        if (mainXmpBuffer == null) {
            Log.d(TAG, "<replaceMainXmpData> mainXmpBuffer is null, error!!");
            return false;
        }
        try {
            rafOut.writeShort(XmpInterface.APP1);
            rafOut.writeShort(2 + mainXmpBuffer.length);
            rafOut.write(mainXmpBuffer);
        } catch (IOException e) {
            Log.d(TAG, "<replaceMainXmpData> IOException", e);
            return false;
        }
        return true;
    }

    private boolean updateConfigAndReplaceBlurImage(String srcFilePath, String blurImageFilePath,
            String outputPath, StereoDebugInfo config) {
        Log.d(TAG, "<updateConfigAndReplaceBlurImage> parse app section [src file]...");
        ArrayList<Section> srcFileSections = mXmpInterface.parseAppInfo(srcFilePath);
        Log.d(TAG, "<updateConfigAndReplaceBlurImage> parse app section [blur image]");
        ArrayList<Section> blurImageSections = mXmpInterface.parseAppInfo(blurImageFilePath);
        if (srcFileSections.isEmpty() || blurImageSections.isEmpty()) {
            Log.d(TAG, "<updateConfigAndReplaceBlurImage> app sections is empty!!");
            return false;
        }

        RandomAccessFile srcFileRafIn = null;
        RandomAccessFile blurImageRafIn = null;
        RandomAccessFile rafOut = null;
        try {
            File outFile = new File(outputPath);
            if (outFile.exists()) {
                outFile.delete();
            }
            outFile.createNewFile();
            srcFileRafIn = new RandomAccessFile(srcFilePath, "r");
            blurImageRafIn = new RandomAccessFile(blurImageFilePath, "r");
            rafOut = new RandomAccessFile(outFile, "rw");

            // 1. copy src file from 0 ~ xmp main to output file
            Section mainXmpSection = getMainXmpSection(srcFileSections);
            if (mainXmpSection == null) {
                Log.d(TAG, "<updateConfigAndReplaceBlurImage> parse src mainXmpSection error!!");
                return false;
            }
            mXmpInterface.copyFileWithFixBuffer(srcFileRafIn, rafOut, 0, mainXmpSection.mOffset);

            // 2. replace xmp main
            if (!replaceMainXmpData(srcFileRafIn, rafOut, config, mainXmpSection)) {
                Log.d(TAG, "<updateConfigAndReplaceBlurImage> replaceMainXmpData, error!!");
                return false;
            }

            // 3. copy src file from [extend xmp ~ DQT] to output file
            Section firstExtXmpSection = getFirstExtXmpSection(srcFileSections);
            Section sectionDQT = getDQTSection(srcFileSections);
            if (firstExtXmpSection == null || sectionDQT == null) {
                Log.d(TAG, "<updateConfigAndReplaceBlurImage> parse src firstExtXmpSection "
                        + " or sectionDQT error!!");
                return false;
            }
            mXmpInterface.copyFileWithFixBuffer(srcFileRafIn, rafOut, firstExtXmpSection.mOffset,
                    sectionDQT.mOffset - firstExtXmpSection.mOffset);

            // 4. copy blur image file from DQT ~ end to output file
            sectionDQT = getDQTSection(blurImageSections);
            if (sectionDQT == null) {
                Log.d(TAG, "<updateConfigAndReplaceBlurImage> parse blurImage DQT section error!!");
                return false;
            }
            mXmpInterface.copyFileWithFixBuffer(blurImageRafIn, rafOut, sectionDQT.mOffset,
                    XmpInterface.COPY_ALL_REMAINING_DATA);
            return true;
        } catch (IOException e) {
            Log.d(TAG, "<updateConfigAndReplaceBlurImage> IOException", e);
            return false;
        } finally {
            closeRandomAccessFile(srcFileRafIn);
            closeRandomAccessFile(blurImageRafIn);
            closeRandomAccessFile(rafOut);
        }
    }

    private Section getDQTSection(ArrayList<Section> sections) {
        Section section = null;
        for (int i = 0; i < sections.size(); i++) {
            section = sections.get(i);
            if (section.mMarker == XmpInterface.DQT) {
                return section;
            }
        }
        return null;
    }

    private Section getFirstExtXmpSection(ArrayList<Section> sections) {
        Section section = null;
        for (int i = 0; i < sections.size(); i++) {
            section = sections.get(i);
            if (section.mIsXmpExt) {
                return section;
            }
        }
        return null;
    }

    private Section getMainXmpSection(ArrayList<Section> sections) {
        Section section = null;
        for (int i = 0; i < sections.size(); i++) {
            section = sections.get(i);
            if (section.mIsXmpMain) {
                return section;
            }
        }
        return null;
    }

    private byte[] extractExtendXmpData(String filePath) {
        File srcFile = new File(filePath);
        if (!srcFile.exists()) {
            Log.d(TAG, "<extractExtendXmpData> " + filePath + " not exists!!!");
            return null;
        }
        Log.d(TAG, "<extractExtendXmpData> begin, filePath " + filePath);
        mParsedSectionsForGallery = mXmpInterface.parseAppInfo(filePath);
        RandomAccessFile rafIn = null;
        try {
            rafIn = new RandomAccessFile(filePath, "r");
            Section sec = null;
            // step1: parse extend xmp total length
            int extXmpTotalLen = 0;
            for (int i = 0; i < mParsedSectionsForGallery.size(); i++) {
                sec = mParsedSectionsForGallery.get(i);
                if (sec.mIsXmpExt) {
                    rafIn.seek(sec.mOffset + 2 + 2
                            + XmpInterface.EXT_XMP_COMMON_HEADER_TOTAL_LEN_OFFSET);
                    extXmpTotalLen = rafIn.readInt();
                    Log.d(TAG, "<extractExtendXmpData> extXmpTotalLen " + extXmpTotalLen);
                    break;
                }
            }
            // step2: copy all ext xmp data to buffer
            byte[] extXmpData = new byte[extXmpTotalLen];
            int app1Len = 0;
            int extXmpPartitionLen = 0;
            int offset = 0;
            for (int i = 0; i < mParsedSectionsForGallery.size(); i++) {
                sec = mParsedSectionsForGallery.get(i);
                if (sec.mIsXmpExt) {
                    // seek to app1 length tag
                    rafIn.seek(sec.mOffset + 2);
                    app1Len = rafIn.readUnsignedShort() - 2;
                    extXmpPartitionLen = app1Len
                            - XmpInterface.EXT_XMP_COMMON_HEADER_REAL_DATA_OFFSET;
                    // seek to real extend xmp data
                    rafIn.skipBytes(XmpInterface.EXT_XMP_COMMON_HEADER_REAL_DATA_OFFSET);
                    rafIn.read(extXmpData, offset, extXmpPartitionLen);
                    offset += extXmpPartitionLen;
                }
            }
            Log.d(TAG, "<extractExtendXmpData> end!!!");
            return extXmpData;
        } catch (IOException e) {
            Log.e(TAG, "<extractExtendXmpData> IOException ", e);
            return null;
        } finally {
            try {
                if (rafIn != null) {
                    rafIn.close();
                    rafIn = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "<extractExtendXmpData> IOException when close ", e);
            }
        }
    }

    private void writeStereoCaptureInfo(ByteArrayInputStreamExt is, ByteArrayOutputStreamExt os,
            byte[] jpsData, byte[] jsonBuffer, byte[] jpsMask, byte[] clearImage,
            byte[] gDepthMapSmall, byte[] ldc) {
        os.writeShort(XmpInterface.SOI);
        boolean hasWritenConfigInfo = false;
        boolean hasWritenJpsAndMask = false;
        int writenLocation = mXmpInterface.findProperLocationForXmp(mParsedSectionsForCamera);
        if (writenLocation == XmpInterface.WRITE_XMP_AFTER_SOI) {
            // means no APP1
            writeConfigToStream(os, jsonBuffer, clearImage, gDepthMapSmall, ldc);
            hasWritenConfigInfo = true;
        }
        for (int i = 0; i < mParsedSectionsForCamera.size(); i++) {
            Section sec = mParsedSectionsForCamera.get(i);
            if (sec.mIsExif) {
                mXmpInterface.writeSectionToStream(is, os, sec);
                if (!hasWritenConfigInfo) {
                    writeConfigToStream(os, jsonBuffer, clearImage, gDepthMapSmall, ldc);
                    hasWritenConfigInfo = true;
                }
            } else {
                if (!hasWritenConfigInfo) {
                    writeConfigToStream(os, jsonBuffer, clearImage, gDepthMapSmall, ldc);
                    hasWritenConfigInfo = true;
                }
                // APPx must be before DQT/DHT
                if (!hasWritenJpsAndMask && (sec.mMarker == XmpInterface.DQT
                        || sec.mMarker == XmpInterface.DHT)) {
                    writeJpsAndMaskToStream(os, jpsData, jpsMask);
                    hasWritenJpsAndMask = true;
                }
                if (sec.mIsXmpMain || sec.mIsXmpExt || sec.mIsJpsData || sec.mIsJpsMask) {
                    // skip old jpsData and jpsMask
                    is.skip(sec.mLength + 2);
                } else {
                    mXmpInterface.writeSectionToStream(is, os, sec);
                }
            }
        }
        // write jps and mask to app15, before sos
        if (!hasWritenJpsAndMask) {
            writeJpsAndMaskToStream(os, jpsData, jpsMask);
            hasWritenJpsAndMask = true;
        }
        // write remain whole file (from SOS)
        mXmpInterface.copyToStreamWithFixBuffer(is, os);
        Log.d(TAG, "<writeJpsAndMaskAndConfigToJpgBuffer> write end!!!");
    }

    private byte[] makeXmpMainData(StereoDebugInfo configInfo) {
        if (configInfo == null) {
            Log.d(TAG, "<makeXmpMainData> configInfo is null, so return null!!!");
            return null;
        }
        XMPMeta meta = XMPMetaFactory.create();
        meta = makeXmpMainDataInternal(meta, configInfo);
        byte[] bufferOutTmp = mXmpInterface.serialize(meta);
        if (bufferOutTmp == null) {
            Log.d(TAG, "<makeXmpMainData> serialized meta is null, so return null!!!");
            return null;
        }
        byte[] bufferOut = new byte[bufferOutTmp.length +
                XmpInterface.XMP_HEADER_START.length()];
        System.arraycopy(XmpInterface.XMP_HEADER_START.getBytes(), 0, bufferOut, 0,
                XmpInterface.XMP_HEADER_START.length());
        System.arraycopy(bufferOutTmp, 0, bufferOut, XmpInterface.XMP_HEADER_START.length(),
                bufferOutTmp.length);
        return bufferOut;
    }

    // return buffer not include app1 tag and length
    private byte[] updateXmpMainDataWithDepthBuffer(XMPMeta meta, DepthBufferInfo depthBufferInfo) {
        if (depthBufferInfo == null || meta == null) {
            return null;
        }
        mXmpInterface.registerNamespace(MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                MTK_REFOCUS_PREFIX);
        if (depthBufferInfo.depthBufferWidth != -1) {
            mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                    DEPTH_BUFFER_WIDTH, depthBufferInfo.depthBufferWidth);
        }
        if (depthBufferInfo.depthBufferHeight != -1) {
            mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                    DEPTH_BUFFER_HEIGHT, depthBufferInfo.depthBufferHeight);
        }
        if (depthBufferInfo.xmpDepthWidth != -1) {
            mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                    XMP_DEPTH_WIDTH, depthBufferInfo.xmpDepthWidth);
        }
        if (depthBufferInfo.xmpDepthHeight != -1) {
            mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                    XMP_DEPTH_HEIGHT, depthBufferInfo.xmpDepthHeight);
        }
        if (depthBufferInfo.metaBufferWidth != -1) {
            mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                    META_BUFFER_WIDTH, depthBufferInfo.metaBufferWidth);
        }
        if (depthBufferInfo.metaBufferHeight != -1) {
            mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                    META_BUFFER_HEIGHT, depthBufferInfo.metaBufferHeight);
        }
        if (depthBufferInfo.touchCoordXLast != -1) {
            mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                    TOUCH_COORDX_LAST, depthBufferInfo.touchCoordXLast);
        }
        if (depthBufferInfo.touchCoordYLast != -1) {
            mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                    TOUCH_COORDY_LAST, depthBufferInfo.touchCoordYLast);
        }
        if (depthBufferInfo.depthOfFieldLast != -1) {
            mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                    DEPTH_OF_FIELD_LAST, depthBufferInfo.depthOfFieldLast);
        }
        if (depthBufferInfo.depthBufferFlag != mXmpInterface.getPropertyBoolean(meta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, DEPTH_BUFFER_FLAG)) {
            mXmpInterface.setPropertyBoolean(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                    DEPTH_BUFFER_FLAG, depthBufferInfo.depthBufferFlag);
        }
        if (depthBufferInfo.xmpDepthFlag != mXmpInterface.getPropertyBoolean(meta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, XMP_DEPTH_FLAG)) {
            mXmpInterface.setPropertyBoolean(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                    XMP_DEPTH_FLAG, depthBufferInfo.xmpDepthFlag);
        }
        updateMd5ValueInXmpMain(meta);
        return serializeXMP(meta);
    }

    private byte[] serializeXMP(XMPMeta meta) {
        byte[] bufferOutTmp = mXmpInterface.serialize(meta);
        if (bufferOutTmp == null) {
            Log.d(TAG, "<serializeXMP> serialized meta is null, return!!!");
            return null;
        }
        byte[] bufferOut = new byte[bufferOutTmp.length +
                XmpInterface.XMP_HEADER_START.length()];
        System.arraycopy(XmpInterface.XMP_HEADER_START.getBytes(), 0, bufferOut, 0,
                XmpInterface.XMP_HEADER_START.length());
        System.arraycopy(bufferOutTmp, 0, bufferOut, XmpInterface.XMP_HEADER_START.length(),
                bufferOutTmp.length);
        return bufferOut;
    }

    private void setGoogleStereoParams(XMPMeta meta, StereoDebugInfo stereoParams) {
        mXmpInterface.registerNamespace(NS_GFOCUS, PRIFIX_GFOCUS);
        mXmpInterface.registerNamespace(NS_GIMAGE, PRIFIX_GIMAGE);
        mXmpInterface.registerNamespace(NS_GDEPTH, PRIFIX_GDEPTH);

        mXmpInterface.setPropertyDouble(meta, NS_GFOCUS, ATTRIBUTE_GFOCUS_BLUR_INFINITY,
                stereoParams.gFocusBlurAtInfinity);
        mXmpInterface.setPropertyDouble(meta, NS_GFOCUS, ATTRIBUTE_GFOCUS_FOCALDISTANCE,
                stereoParams.gFocusFocalDistance);
        mXmpInterface.setPropertyDouble(meta, NS_GFOCUS, ATTRIBUTE_GFOCUS_FOCALPOINTX,
                stereoParams.gFocusFocalPointX);
        mXmpInterface.setPropertyDouble(meta, NS_GFOCUS, ATTRIBUTE_GFOCUS_FOCALPOINTY,
                stereoParams.gFocusFocalPointY);

        mXmpInterface.setPropertyString(meta, NS_GIMAGE, ATTRIBUTE_GIMAGE_MIME,
                stereoParams.gImageMime);

        mXmpInterface.setPropertyString(meta, NS_GDEPTH, ATTRIBUTE_GDEPTH_FORMAT,
                stereoParams.gDepthFormat);
        mXmpInterface.setPropertyDouble(meta, NS_GDEPTH, ATTRIBUTE_GDEPTH_NEAR,
                stereoParams.gDepthNear);
        mXmpInterface.setPropertyDouble(meta, NS_GDEPTH, ATTRIBUTE_GDEPTH_FAR,
                stereoParams.gDepthFar);
        mXmpInterface.setPropertyString(meta, NS_GDEPTH, ATTRIBUTE_GDEPTH_MIME,
                stereoParams.gDepthMime);
    }

    private StereoDebugInfo parseGoogleStereoParams(XMPMeta meta, StereoDebugInfo stereoParams) {
        if (stereoParams == null || meta == null) {
            Log.d(TAG, "<parseGoogleStereoParams> params error!!");
            return null;
        }
        stereoParams.gFocusBlurAtInfinity = mXmpInterface.getPropertyDouble(meta, NS_GFOCUS,
                ATTRIBUTE_GFOCUS_BLUR_INFINITY);
        stereoParams.gFocusFocalDistance = mXmpInterface.getPropertyDouble(meta, NS_GFOCUS,
                ATTRIBUTE_GFOCUS_FOCALDISTANCE);
        stereoParams.gFocusFocalPointX = mXmpInterface.getPropertyDouble(meta, NS_GFOCUS,
                ATTRIBUTE_GFOCUS_FOCALPOINTX);
        stereoParams.gFocusFocalPointY = mXmpInterface.getPropertyDouble(meta, NS_GFOCUS,
                ATTRIBUTE_GFOCUS_FOCALPOINTY);

        stereoParams.gImageMime = mXmpInterface.getPropertyString(meta, NS_GIMAGE,
                ATTRIBUTE_GIMAGE_MIME);

        stereoParams.gDepthFormat = mXmpInterface.getPropertyString(meta, NS_GDEPTH,
                ATTRIBUTE_GDEPTH_FORMAT);
        stereoParams.gDepthNear = mXmpInterface.getPropertyDouble(meta, NS_GDEPTH,
                ATTRIBUTE_GDEPTH_NEAR);
        stereoParams.gDepthFar = mXmpInterface.getPropertyDouble(meta, NS_GDEPTH,
                ATTRIBUTE_GDEPTH_FAR);
        stereoParams.gDepthMime = mXmpInterface.getPropertyString(meta, NS_GDEPTH,
                ATTRIBUTE_GDEPTH_MIME);
        return stereoParams;
    }

    private XMPMeta makeXmpMainDataInternal(XMPMeta meta, StereoDebugInfo configInfo) {
        if (configInfo == null || meta == null) {
            Log.d(TAG, "<makeXmpMainDataInternal> params error!!");
            return null;
        }
        setGoogleStereoParams(meta, configInfo);
        setMtkStereoInfo(meta, configInfo);
        setMtkSegmentInfo(meta, configInfo);
        if (mNeedExtXmp) {
            updateMd5ValueInXmpMain(meta);
        }
        return meta;
    }

    private void setMtkStereoInfo(XMPMeta meta, StereoDebugInfo configInfo) {
        mXmpInterface.registerNamespace(MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                MTK_REFOCUS_PREFIX);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, JPS_WIDTH,
                configInfo.jpsWidth);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, JPS_HEIGHT,
                configInfo.jpsHeight);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, MASK_WIDTH,
                configInfo.maskWidth);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, MASK_HEIGHT,
                configInfo.maskHeight);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, POS_X,
                configInfo.posX);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, POS_Y,
                configInfo.posY);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, VIEW_WIDTH,
                configInfo.viewWidth);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, VIEW_HEIGHT,
                configInfo.viewHeight);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, ORIENTATION,
                configInfo.orientation);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, DEPTH_ROTATION,
                configInfo.depthRotation);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, MAIN_CAM_POS,
                configInfo.mainCamPos);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, TOUCH_COORDX_1ST,
                configInfo.touchCoordX1st);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, TOUCH_COORDY_1ST,
                configInfo.touchCoordY1st);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, DOF_LEVEL,
                configInfo.dofLevel);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, LDC_WIDTH,
                configInfo.ldcWidth);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, LDC_HEIGHT,
                configInfo.ldcHeight);
        // add default value for DepthBufferInfo
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                DEPTH_BUFFER_WIDTH, -1);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                DEPTH_BUFFER_HEIGHT, -1);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, XMP_DEPTH_WIDTH,
                -1);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, XMP_DEPTH_HEIGHT,
                -1);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, META_BUFFER_WIDTH,
                -1);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                META_BUFFER_HEIGHT, -1);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, TOUCH_COORDX_LAST,
                -1);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, TOUCH_COORDY_LAST,
                -1);
        mXmpInterface.setPropertyInteger(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                DEPTH_OF_FIELD_LAST, -1);
        mXmpInterface.setPropertyBoolean(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, DEPTH_BUFFER_FLAG,
                false);
        mXmpInterface.setPropertyBoolean(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, XMP_DEPTH_FLAG,
                false);
    }

    private void updateMd5ValueInXmpMain(XMPMeta meta) {
        mXmpInterface.registerNamespace(NS_XMPNOTE, PRIFIX_XMPNOTE);
        if (meta == null) {
            Log.d(TAG, "<updateMd5ValueInXmpMain> meta is null, error!!");
            return;
        }
        Log.d(TAG, "<updateMd5ValueInXmpMain> md5 " + mValueOfMd5);
        mXmpInterface.setPropertyString(meta, NS_XMPNOTE, ATTRIBUTE_XMPNOTE, mValueOfMd5);
    }

    private void setMtkSegmentInfo(XMPMeta meta, StereoDebugInfo configInfo) {
        mXmpInterface.registerNamespace(XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                XmpInterface.MTK_SEGMENT_PREFIX);
        mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                XmpInterface.SEGMENT_MASK_WIDTH, -1);
        mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                XmpInterface.SEGMENT_MASK_HEIGHT, -1);
        mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                XmpInterface.SEGMENT_X, -1);
        mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                XmpInterface.SEGMENT_Y, -1);
        mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                XmpInterface.SEGMENT_LEFT, -1);
        mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                XmpInterface.SEGMENT_RIGHT, -1);
        mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                XmpInterface.SEGMENT_TOP, -1);
        mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                XmpInterface.SEGMENT_BOTTOM, -1);
        setMtkFdInfo(meta, configInfo);
    }

    private void setMtkFdInfo(XMPMeta meta, StereoDebugInfo configInfo) {
        mXmpInterface.setPropertyInteger(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                XmpInterface.SEGMENT_FACE_COUNT, configInfo.faceCount);
        FaceDetectionInfo fdInfo = null;
        mXmpInterface.registerNamespace(XmpInterface.SEGMENT_FACE_FIELD_NS,
                XmpInterface.SEGMENT_FACE_PREFIX);
        for (int i = 0; i < configInfo.fdInfoArray.size(); i++) {
            fdInfo = configInfo.fdInfoArray.get(i);
            mXmpInterface.setStructField(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                    XmpInterface.SEGMENT_FACE_STRUCT_NAME + i, XmpInterface.SEGMENT_FACE_FIELD_NS,
                    XmpInterface.SEGMENT_FACE_LEFT, Integer.toString(fdInfo.mFaceLeft));
            mXmpInterface.setStructField(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                    XmpInterface.SEGMENT_FACE_STRUCT_NAME + i, XmpInterface.SEGMENT_FACE_FIELD_NS,
                    XmpInterface.SEGMENT_FACE_TOP, Integer.toString(fdInfo.mFaceTop));
            mXmpInterface.setStructField(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                    XmpInterface.SEGMENT_FACE_STRUCT_NAME + i, XmpInterface.SEGMENT_FACE_FIELD_NS,
                    XmpInterface.SEGMENT_FACE_RIGHT, Integer.toString(fdInfo.mFaceRight));
            mXmpInterface.setStructField(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                    XmpInterface.SEGMENT_FACE_STRUCT_NAME + i, XmpInterface.SEGMENT_FACE_FIELD_NS,
                    XmpInterface.SEGMENT_FACE_BOTTOM, Integer.toString(fdInfo.mFaceBottom));
            mXmpInterface.setStructField(meta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                    XmpInterface.SEGMENT_FACE_STRUCT_NAME + i, XmpInterface.SEGMENT_FACE_FIELD_NS,
                    XmpInterface.SEGMENT_FACE_RIP, Integer.toString(fdInfo.mFaceRip));
        }
    }

    private void writeJpsAndMaskToStream(ByteArrayOutputStreamExt os, byte[] jpsData,
            byte[] jpsMask) {
        try {
            Log.d(TAG, "<writeJpsAndMaskToStream> write begin!!!");
            int totalCount = 0;
            ArrayList<byte[]> jpsAndMaskArray = makeJpsAndMaskData(jpsData, jpsMask);
            for (int i = 0; i < jpsAndMaskArray.size(); i++) {
                byte[] section = jpsAndMaskArray.get(i);
                if (section[INDEX_0] == 'J' && section[INDEX_1] == 'P' && section[INDEX_2] == 'S'
                        && section[INDEX_3] == 'D') {
                    // current section is jps data
                    totalCount = jpsData.length;
                } else if (section[INDEX_0] == 'J' && section[INDEX_1] == 'P'
                        && section[INDEX_2] == 'S'
                        && section[INDEX_3] == 'M') {
                    // current section is jps mark
                    totalCount = jpsMask.length;
                }
                // os.writeShort(APP1);
                os.writeShort(XmpInterface.APP15);
                os.writeShort(section.length + 2 + XmpInterface.TOTAL_LENGTH_TAG_BYTE);
                // 4 bytes
                os.writeInt(totalCount);
                os.write(section);
            }
            Log.d(TAG, "<writeJpsAndMaskToStream> write end!!!");
        } catch (IOException e) {
            Log.e(TAG, "<writeJpsAndMaskToStream> Exception", e);
        }
    }

    private void writeConfigToStream(ByteArrayOutputStreamExt os, byte[] jpsConfig,
            byte[] clearImage, byte[] gDepthMapSmall, byte[] ldc) {
        try {
            Log.d(TAG, "<writeConfigToStream> write begin!!!");
            StereoDebugInfo configInfo = parseStereoDebugInfoBuffer(jpsConfig);
            Log.d(TAG, "<writeConfigToStream> stereoDebugInfo " + configInfo);
            if (ENABLE_BUFFER_DUMP) {
                XmpInterface.writeStringToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                        + "Config_Written.txt", configInfo.toString());
            }
            // 1. make extend xmp data first, in order to calc md5 value
            ArrayList<byte[]> extXmp = null;
            if (mNeedExtXmp) {
                extXmp = makeXmpExtData(clearImage, gDepthMapSmall, ldc);
            }

            // 2. write xmp main data
            byte[] xmpMain = makeXmpMainData(configInfo);
            if (xmpMain == null) {
                Log.d(TAG, "<writeConfigToStream> xmpMain is null, error!!");
                return;
            }
            os.writeShort(XmpInterface.APP1);
            os.writeShort(xmpMain.length + 2);
            os.write(xmpMain);

            // 3. write extend xmp data
            if (mNeedExtXmp && extXmp != null && extXmp.size() > 0) {
                for (int i = 0; i < extXmp.size(); i++) {
                    byte[] section = extXmp.get(i);
                    os.writeShort(XmpInterface.APP1);
                    os.writeShort(section.length + 2);
                    os.write(section);
                }
            }
            Log.d(TAG, "<writeConfigToStream> write end!!!");
        } catch (IOException e) {
            Log.e(TAG, "<writeConfigToStream> Exception", e);
        }
    }

    private ArrayList<byte[]> makeXmpExtData(byte[] clearImage, byte[] gDepthMapSmall,
            byte[] ldc) {
        if (clearImage == null && gDepthMapSmall == null && ldc == null) {
            Log.d(TAG, "<makeXmpExtData> jade implement!!");
            return new ArrayList<byte[]>();
        }
        XMPMeta meta = XMPMetaFactory.create();
        XmpInterface extXmpInterface = new XmpInterface();
        // write google stereo info into extend xmp
        extXmpInterface.registerExtXmpNamespace(NS_GIMAGE, PRIFIX_GIMAGE);
        extXmpInterface.registerExtXmpNamespace(NS_GDEPTH, PRIFIX_GDEPTH);
        if (clearImage != null) {
            extXmpInterface.setPropertyBase64(meta, NS_GIMAGE, ATTRIBUTE_GIMAGE_DATA, clearImage);
        }
        if (gDepthMapSmall != null) {
            extXmpInterface.setPropertyBase64(meta, NS_GDEPTH, ATTRIBUTE_GDEPTH_DATA,
                    gDepthMapSmall);
        }
        // write ldc info into extend xmp
        if (ldc != null) {
            extXmpInterface.registerExtXmpNamespace(MEDIATEK_IMAGE_REFOCUS_NAMESPACE,
                    MTK_REFOCUS_PREFIX);
            extXmpInterface.setPropertyBase64(meta, MEDIATEK_IMAGE_REFOCUS_NAMESPACE, LDC, ldc);
        }

        byte[] serializedData = extXmpInterface.serialize(meta);
        if (serializedData == null) {
            Log.d(TAG, "<makeXmpExtData> serialized meta is null, so return null!!!");
            return null;
        }
        return makeExtXmpDataInternal(serializedData);
    }

    private ArrayList<byte[]> makeExtXmpDataInternal(byte[] extXmpData) {
        ArrayList<byte[]> extXmpDataArray = new ArrayList<byte[]>();
        if (extXmpData == null) {
            Log.d(TAG, "<makeExtXmpDataInternal> extXmpData is null, error!!");
            return extXmpDataArray;
        }
        mValueOfMd5 = XmpInterface.getMd5(extXmpData);
        int sectionCount = extXmpData.length / XmpInterface.MAX_LEN_FOR_REAL_XMP_DATA_PER_APP1 + 1;
        byte[] commonHeader = null;
        byte[] section = null;
        int currentPos = 0;
        for (int i = 0; i < sectionCount; i++) {
            commonHeader = XmpInterface.getXmpCommonHeader(mValueOfMd5, extXmpData.length, i);
            if (i == sectionCount - 1) {
                int sectionLen = extXmpData.length
                        % XmpInterface.MAX_LEN_FOR_REAL_XMP_DATA_PER_APP1 + commonHeader.length;
                section = new byte[sectionLen];
                // 1. copy header
                System.arraycopy(commonHeader, 0, section, 0, commonHeader.length);
                // 2. copy data
                System.arraycopy(extXmpData, currentPos, section, commonHeader.length, sectionLen
                        - commonHeader.length);
                currentPos += sectionLen - commonHeader.length;
            } else {
                section = new byte[XmpInterface.MAX_BYTE_PER_APP1];
                // 1. copy header
                System.arraycopy(commonHeader, 0, section, 0, commonHeader.length);
                // 2. copy data
                System.arraycopy(extXmpData, currentPos, section, commonHeader.length,
                        XmpInterface.MAX_BYTE_PER_APP1 - commonHeader.length);
                currentPos += XmpInterface.MAX_BYTE_PER_APP1 - commonHeader.length;
            }
            extXmpDataArray.add(i, section);
        }
        return extXmpDataArray;
    }

    private ArrayList<byte[]> makeJpsAndMaskData(byte[] jpsData, byte[] jpsMask) {
        if (jpsData == null || jpsMask == null) {
            Log.d(TAG, "<makeJpsAndMaskData> jpsData or jpsMask buffer is null!!!");
            return null;
        }

        int arrayIndex = 0;
        ArrayList<byte[]> jpsAndMaskArray = new ArrayList<byte[]>();

        for (int i = 0; i < 2; i++) {
            byte[] data = (i == 0 ? jpsData : jpsMask);
            String header = (i == 0 ? XmpInterface.TYPE_JPS_DATA : XmpInterface.TYPE_JPS_MASK);
            int dataRemain = data.length;
            int dataOffset = 0;
            int sectionCount = 0;

            while (header.length() + 1 + dataRemain >= XmpInterface.JPS_PACKET_SIZE) {
                byte[] section = new byte[XmpInterface.JPS_PACKET_SIZE];
                // copy type
                System.arraycopy(header.getBytes(), 0, section, 0, header.length());
                // write section number
                section[header.length()] = (byte) sectionCount;
                // copy data
                System.arraycopy(data, dataOffset, section, header.length() + 1, section.length
                        - header.length() - 1);
                jpsAndMaskArray.add(arrayIndex, section);

                dataOffset += section.length - header.length() - 1;
                dataRemain = data.length - dataOffset;
                sectionCount++;
                arrayIndex++;
            }
            if (header.length() + 1 + dataRemain < XmpInterface.JPS_PACKET_SIZE) {
                byte[] section = new byte[header.length() + 1 + dataRemain];
                // copy type
                System.arraycopy(header.getBytes(), 0, section, 0, header.length());
                // write section number
                section[header.length()] = (byte) sectionCount;
                // write data
                System.arraycopy(data, dataOffset, section, header.length() + 1, dataRemain);
                jpsAndMaskArray.add(arrayIndex, section);
                arrayIndex++;
            }
        }
        return jpsAndMaskArray;
    }

    private byte[] getJpsInfoFromSections(RandomAccessFile rafIn, boolean isJpsDataOrMask) {
        try {
            Section sec = null;
            int dataLen = 0;
            // parse JPS Data or Mask length
            int i = 0;
            for (; i < mParsedSectionsForGallery.size(); i++) {
                sec = mParsedSectionsForGallery.get(i);
                if (isJpsDataOrMask && sec.mIsJpsData) {
                    rafIn.seek(sec.mOffset + 2 + 2);
                    dataLen = rafIn.readInt();
                    break;
                }
                if (!isJpsDataOrMask && sec.mIsJpsMask) {
                    rafIn.seek(sec.mOffset + 2 + 2);
                    dataLen = rafIn.readInt();
                    break;
                }
            }
            if (i == mParsedSectionsForGallery.size()) {
                Log.d(TAG, "<getJpsInfoFromSections> can not find JPS INFO, return null");
                return null;
            }
            int app1Len = 0;
            int copyLen = 0;
            int byteOffset = 0;
            byte[] data = new byte[dataLen];
            for (i = i - 1; i < mParsedSectionsForGallery.size(); i++) {
                sec = mParsedSectionsForGallery.get(i);
                if (isJpsDataOrMask && sec.mIsJpsData) {
                    rafIn.seek(sec.mOffset + 2);
                    app1Len = rafIn.readUnsignedShort();
                    copyLen = app1Len - 2 - XmpInterface.TOTAL_LENGTH_TAG_BYTE
                            - XmpInterface.TYPE_JPS_DATA.length()
                            - XmpInterface.JPS_SERIAL_NUM_TAG_BYTE;
                    rafIn.skipBytes(XmpInterface.TOTAL_LENGTH_TAG_BYTE
                            + XmpInterface.TYPE_JPS_DATA.length()
                            + XmpInterface.JPS_SERIAL_NUM_TAG_BYTE);
                    rafIn.read(data, byteOffset, copyLen);
                    byteOffset += copyLen;
                }
                if (!isJpsDataOrMask && sec.mIsJpsMask) {
                    rafIn.seek(sec.mOffset + 2);
                    app1Len = rafIn.readUnsignedShort();
                    copyLen = app1Len - 2 - XmpInterface.TOTAL_LENGTH_TAG_BYTE
                            - XmpInterface.TYPE_JPS_MASK.length()
                            - XmpInterface.JPS_SERIAL_NUM_TAG_BYTE;
                    rafIn.skipBytes(XmpInterface.TOTAL_LENGTH_TAG_BYTE
                            + XmpInterface.TYPE_JPS_MASK.length()
                            + XmpInterface.JPS_SERIAL_NUM_TAG_BYTE);
                    rafIn.read(data, byteOffset, copyLen);
                    byteOffset += copyLen;
                }
            }
            return data;
        } catch (IOException e) {
            Log.e(TAG, "<getJpsInfoFromSections> IOException ", e);
            return null;
        }
    }

    private StereoDebugInfo parseStereoDebugInfo(XMPMeta xmpMeta) {
        if (xmpMeta == null) {
            Log.d(TAG, "<parseStereoDebugInfo> xmpMeta is null, return!!!");
            return null;
        }
        StereoDebugInfo configInfo = new StereoDebugInfo();
        configInfo = parseMtkStereoInfo(xmpMeta, configInfo);
        configInfo = parseFaceInfo(xmpMeta, configInfo);
        configInfo = parseGoogleStereoParams(xmpMeta, configInfo);
        return configInfo;
    }

    private StereoDebugInfo parseMtkStereoInfo(XMPMeta xmpMeta, StereoDebugInfo configInfo) {
        configInfo.jpsWidth = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, JPS_WIDTH);
        configInfo.jpsHeight = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, JPS_HEIGHT);
        configInfo.maskWidth = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, MASK_WIDTH);
        configInfo.maskHeight = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, MASK_HEIGHT);
        configInfo.posX = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, POS_X);
        configInfo.posY = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, POS_Y);
        configInfo.viewWidth = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, VIEW_WIDTH);
        configInfo.viewHeight = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, VIEW_HEIGHT);
        configInfo.orientation = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, ORIENTATION);
        configInfo.depthRotation = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, DEPTH_ROTATION);
        configInfo.mainCamPos = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, MAIN_CAM_POS);
        configInfo.touchCoordX1st = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, TOUCH_COORDX_1ST);
        configInfo.touchCoordY1st = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, TOUCH_COORDY_1ST);
        configInfo.dofLevel = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, DOF_LEVEL);
        configInfo.ldcWidth = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, LDC_WIDTH);
        configInfo.ldcHeight = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, LDC_HEIGHT);
        return configInfo;
    }

    private StereoDebugInfo parseFaceInfo(XMPMeta xmpMeta, StereoDebugInfo stereoParams) {
        stereoParams.faceCount = mXmpInterface.getPropertyInteger(xmpMeta,
                XmpInterface.MEDIATEK_SEGMENT_NAMESPACE, XmpInterface.SEGMENT_FACE_COUNT);
        stereoParams.fdInfoArray = new ArrayList<FaceDetectionInfo>();
        for (int i = 0; i < stereoParams.faceCount; i++) {
            int faceLeft = mXmpInterface
                    .getStructFieldInt(xmpMeta, XmpInterface.MEDIATEK_SEGMENT_NAMESPACE,
                            XmpInterface.SEGMENT_FACE_STRUCT_NAME + i,
                            XmpInterface.SEGMENT_FACE_FIELD_NS, XmpInterface.SEGMENT_FACE_LEFT);
            int faceTop = mXmpInterface.getStructFieldInt(xmpMeta,
                    XmpInterface.MEDIATEK_SEGMENT_NAMESPACE, XmpInterface.SEGMENT_FACE_STRUCT_NAME
                            + i, XmpInterface.SEGMENT_FACE_FIELD_NS, XmpInterface.SEGMENT_FACE_TOP);
            int faceRight = mXmpInterface.getStructFieldInt(xmpMeta,
                    XmpInterface.MEDIATEK_SEGMENT_NAMESPACE, XmpInterface.SEGMENT_FACE_STRUCT_NAME
                            + i, XmpInterface.SEGMENT_FACE_FIELD_NS,
                    XmpInterface.SEGMENT_FACE_RIGHT);
            int faceBottom = mXmpInterface.getStructFieldInt(xmpMeta,
                    XmpInterface.MEDIATEK_SEGMENT_NAMESPACE, XmpInterface.SEGMENT_FACE_STRUCT_NAME
                            + i, XmpInterface.SEGMENT_FACE_FIELD_NS,
                    XmpInterface.SEGMENT_FACE_BOTTOM);
            int faceRip = mXmpInterface.getStructFieldInt(xmpMeta,
                    XmpInterface.MEDIATEK_SEGMENT_NAMESPACE, XmpInterface.SEGMENT_FACE_STRUCT_NAME
                            + i, XmpInterface.SEGMENT_FACE_FIELD_NS, XmpInterface.SEGMENT_FACE_RIP);
            stereoParams.fdInfoArray.add(i, new FaceDetectionInfo(faceLeft, faceTop, faceRight,
                    faceBottom, faceRip));
        }
        return stereoParams;
    }

    private StereoDebugInfo parseStereoDebugInfoBuffer(byte[] configBuffer) {
        if (configBuffer == null) {
            Log.d(TAG, "<parseStereoDebugInfoBuffer> configBuffer is null!!!");
            return null;
        }
        StereoDebugInfoParser parser = new StereoDebugInfoParser(configBuffer);
        StereoDebugInfo configInfo = new StereoDebugInfo();
        configInfo.jpsWidth = parser.getJpsWidth();
        configInfo.jpsHeight = parser.getJpsHeight();
        configInfo.maskWidth = parser.getMaskWidth();
        configInfo.maskHeight = parser.getMaskHeight();
        configInfo.posX = parser.getPosX();
        configInfo.posY = parser.getPosY();
        configInfo.viewWidth = parser.getViewWidth();
        configInfo.viewHeight = parser.getViewHeight();
        configInfo.orientation = parser.getOrientation();
        configInfo.depthRotation = parser.getDepthRotation();
        configInfo.mainCamPos = parser.getMainCamPos();
        configInfo.touchCoordX1st = parser.getTouchCoordX1st();
        configInfo.touchCoordY1st = parser.getTouchCoordY1st();
        // read face detection info
        configInfo.faceCount = parser.getFaceRectCount();
        configInfo.fdInfoArray = new ArrayList<FaceDetectionInfo>();
        for (int i = 0; i < configInfo.faceCount; i++) {
            Rect faceRect = parser.getFaceRect(i);
            if (faceRect != null) {
                configInfo.fdInfoArray.add(i, new FaceDetectionInfo(faceRect.left, faceRect.top,
                        faceRect.right, faceRect.bottom, parser.getFaceRip(i)));
            }
        }
        configInfo.dofLevel = parser.getDof();
        configInfo.ldcWidth = parser.getLdcWidth();
        configInfo.ldcHeight = parser.getLdcHeight();

        // parse google params
        configInfo.gFocusBlurAtInfinity = parser.getGFocusBlurAtInfinity();
        configInfo.gFocusFocalDistance = parser.getGFocusFocalDistance();
        configInfo.gFocusFocalPointX = parser.getGFocusFocalPointX();
        configInfo.gFocusFocalPointY = parser.getGFocusFocalPointY();
        configInfo.gImageMime = parser.getGImageMime();
        configInfo.gDepthFormat = parser.getGDepthFormat();
        configInfo.gDepthNear = parser.getGDepthNear();
        configInfo.gDepthFar = parser.getGDepthFar();
        configInfo.gDepthMime = parser.getGDepthMime();
        return configInfo;
    }

    private boolean writeDepthBufferToJpg(String srcFilePath, String dstFilePath,
            DepthBufferInfo depthBufferInfo, boolean deleteJps) {
        if (depthBufferInfo == null) {
            Log.d(TAG, "<writeDepthBufferToJpg> depthBufferInfo is null!!!");
            return false;
        }
        if (ENABLE_BUFFER_DUMP && depthBufferInfo.depthData != null) {
            XmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                    + "DepthBuffer_Written.raw", depthBufferInfo.depthData);
        }
        if (ENABLE_BUFFER_DUMP && depthBufferInfo.xmpDepthData != null) {
            XmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                    + "Google_DepthMap_Large_Written.raw", depthBufferInfo.xmpDepthData);
        }
        if (ENABLE_BUFFER_DUMP) {
            XmpInterface.writeStringToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                    + "DepthBufferInfo_Written.txt", depthBufferInfo.toString());
        }
        Log.d(TAG, "<writeDepthBufferToJpg> write begin!!!");
        mExtendXmpData = extractExtendXmpData(srcFilePath);
        if (ENABLE_BUFFER_DUMP) {
            XmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                    + "ExtendXMP_RealData_Read.raw", mExtendXmpData);
        }
        // begin to copy or replace
        RandomAccessFile rafIn = null;
        RandomAccessFile rafOut = null;
        try {
            File outFile = new File(dstFilePath);
            if (outFile.exists()) {
                outFile.delete();
            }
            outFile.createNewFile();
            rafIn = new RandomAccessFile(srcFilePath, "r");
            rafOut = new RandomAccessFile(outFile, "rw");

            if (rafIn.readUnsignedShort() != XmpInterface.SOI) {
                Log.d(TAG, "<writeDepthBufferToJpg> image is not begin with 0xffd8!!!");
                return false;
            }
            writeDepthBuffer(rafIn, rafOut, srcFilePath, depthBufferInfo, deleteJps);
            Log.d(TAG, "<writeDepthBufferToJpg> write end!!!");
            return true;
        } catch (IOException e) {
            Log.d(TAG, "<writeDepthBufferToJpg> Exception", e);
            return false;
        } finally {
            closeRandomAccessFile(rafIn);
            closeRandomAccessFile(rafOut);
        }
    }

    private void writeDepthBuffer(RandomAccessFile rafIn, RandomAccessFile rafOut,
            String srcFilePath,
            DepthBufferInfo depthBufferInfo, boolean deleteJps) throws IOException {
        rafOut.writeShort(XmpInterface.SOI);
        boolean hasUpdateXmpMain = false;
        boolean hasWritenDepthData = false;
        XMPMeta meta = mXmpInterface.getXmpMetaFromFile(srcFilePath);
        int writenLocation = mXmpInterface.findProperLocationForXmp(mParsedSectionsForGallery);
        if (writenLocation == XmpInterface.WRITE_XMP_AFTER_SOI) {
            updateOnlyDepthInfo(rafOut, meta, depthBufferInfo);
            hasUpdateXmpMain = true;
        }
        boolean needUpdateDepthBuffer = depthBufferInfo.depthData != null ? true : false;
        boolean needUpdateXmpDepth = depthBufferInfo.xmpDepthData != null ? true : false;
        for (int i = 0; i < mParsedSectionsForGallery.size(); i++) {
            Section sec = mParsedSectionsForGallery.get(i);
            if (sec.mIsExif) {
                mXmpInterface.writeSectionToFile(rafIn, rafOut, sec);
                if (!hasUpdateXmpMain) {
                    updateOnlyDepthInfo(rafOut, meta, depthBufferInfo);
                    hasUpdateXmpMain = true;
                }
            } else {
                if (!hasUpdateXmpMain) {
                    updateOnlyDepthInfo(rafOut, meta, depthBufferInfo);
                    hasUpdateXmpMain = true;
                }
                // APPx must be before DQT/DHT
                if (!hasWritenDepthData/* && (sec.mMarker == XmpInterface.DQT ||
                        sec.mMarker == XmpInterface.DHT)*/) {
                    writeOnlyDepthBuffer(rafOut, depthBufferInfo);
                    hasWritenDepthData = true;
                }
                if (sec.mIsXmpMain || sec.mIsXmpExt) {
                    // delete old xmp main and ext
                    rafIn.skipBytes(sec.mLength + 2);
                } else if (deleteJps && (sec.mIsJpsData || sec.mIsJpsMask)) {
                    rafIn.skipBytes(sec.mLength + 2);
                } else if (needUpdateDepthBuffer && sec.mIsDepthData) {
                    // delete depth data
                    rafIn.skipBytes(sec.mLength + 2);
                } else if (needUpdateXmpDepth && sec.mIsXmpDepth) {
                    // delete xmp depth
                    rafIn.skipBytes(sec.mLength + 2);
                } else {
                    mXmpInterface.writeSectionToFile(rafIn, rafOut, sec);
                }
            }
        }
        // write buffer to app15
        if (!hasWritenDepthData) {
            writeOnlyDepthBuffer(rafOut, depthBufferInfo);
            hasWritenDepthData = true;
        }
        // write remain whole file (from SOS)
        mXmpInterface.copyFileWithFixBuffer(rafIn, rafOut);
        Log.d(TAG, "<writeDepthBufferToJpg> write end!!!");
    }

    /**
     * Update main xmp and extend xmp(xmp depthmap) data.
     *
     * @param rafOut
     *            out file
     * @param meta
     *            xmp meta
     * @param depthBufferInfo
     *            depth buffer info
     * @return true if success
     */
    private boolean updateOnlyDepthInfo(RandomAccessFile rafOut, XMPMeta meta,
            DepthBufferInfo depthBufferInfo) {
        if (rafOut == null || meta == null || depthBufferInfo == null) {
            Log.d(TAG, "<updateOnlyDepthInfo> input params are null, return false!!!");
            return false;
        }
        Log.d(TAG, "<updateOnlyDepthInfo> write begin!!!");

        // step 1: generate extend xmp data in order to calc md5 value
        ArrayList<byte[]> extXmpDataArray = updateGoogleDepthMap(mExtendXmpData,
                depthBufferInfo);

        // step 2: generate main xmp data
        byte[] newXmpMainBuffer = updateXmpMainDataWithDepthBuffer(meta, depthBufferInfo);
        if (newXmpMainBuffer == null) {
            Log.d(TAG,
                    "<updateOnlyDepthInfo> updated xmp main data is null, return false!!!");
            return false;
        }

        try {
            // step3: write main xmp data
            rafOut.writeShort(XmpInterface.APP1);
            rafOut.writeShort(newXmpMainBuffer.length + 2);
            rafOut.write(newXmpMainBuffer);

            // step4: write extend xmp data: google depthmap
            byte[] section = null;
            if (extXmpDataArray != null && extXmpDataArray.size() > 0) {
                for (int i = 0; i < extXmpDataArray.size(); i++) {
                    section = extXmpDataArray.get(i);
                    rafOut.writeShort(XmpInterface.APP1);
                    rafOut.writeShort(section.length + 2);
                    rafOut.write(section);
                }
                Log.d(TAG, "<updateOnlyDepthInfo> update xmpDepthData");
            }
            Log.d(TAG, "<updateOnlyDepthInfo> write end!!!");
            return true;
        } catch (IOException e) {
            Log.e(TAG, "<updateOnlyDepthInfo> IOException ", e);
            return false;
        }
    }

    private ArrayList<byte[]> updateGoogleDepthMap(byte[] srcXmpBuffer,
            DepthBufferInfo depthBufferInfo) {
        if (srcXmpBuffer == null || depthBufferInfo == null
                || depthBufferInfo.xmpDepthData == null) {
            Log.d(TAG, "<updateGoogleDepthMap> params error!!");
            return new ArrayList<byte[]>();
        }
        Log.d(TAG, "<updateGoogleDepthMap> begin!!");

        XmpInterface extXmpInterface = new XmpInterface();
        XMPMeta extXmpMeta = extXmpInterface.getExtXmpMetaFromBuffer(srcXmpBuffer);
        if (extXmpMeta == null) {
            Log.d(TAG, "<updateGoogleDepthMap> extXmpMeta is null, return null!!");
            return null;
        }

        byte[] encodedDepth = Utils.encodePng(depthBufferInfo.xmpDepthData,
                depthBufferInfo.xmpDepthWidth, depthBufferInfo.xmpDepthHeight);
        if (encodedDepth == null) {
            Log.d(TAG, "<updateGoogleDepthMap> encodedDepth is null, return!!");
            return null;
        }
        extXmpInterface.setPropertyBase64(extXmpMeta, NS_GDEPTH, ATTRIBUTE_GDEPTH_DATA,
                encodedDepth);
        byte[] serializedData = extXmpInterface.serialize(extXmpMeta);
        if (ENABLE_BUFFER_DUMP) {
            XmpInterface.writeBufferToFile(DUMP_PATH + DUMP_FOLDER_NAME + mFileName
                    + "ExtendXMP_Updating_RealData_Written.raw", serializedData);
        }
        Log.d(TAG, "<updateGoogleDepthMap> end!!");
        return makeExtXmpDataInternal(serializedData);
    }

    // write depthBufferInfo.depthData to app15
    // depthBufferInfo.xmpDepthData will be saved into extend xmp in app1
    private boolean writeOnlyDepthBuffer(RandomAccessFile rafOut, DepthBufferInfo depthBufferInfo) {
        if (rafOut == null || depthBufferInfo == null) {
            Log.d(TAG, "<writeOnlyDepthBuffer> input params error, return false!!!");
            return false;
        }
        Log.d(TAG, "<writeOnlyDepthBuffer> write begin!!!");
        if (depthBufferInfo.depthData == null) {
            Log.d(TAG, "<writeOnlyDepthBuffer> skip write depth buffer!!!");
            return true;
        }

        try {
            // write depth data
            int totalCount = 0;
            byte[] section = null;
            ArrayList<byte[]> depthDataArray = makeDepthData(depthBufferInfo.depthData, null);
            if (depthDataArray == null) {
                Log.d(TAG, "<writeOnlyDepthBuffer> depthDataArray is null," +
                        "skip write depth buffer!!!");
                return true;
            }
            for (int i = 0; i < depthDataArray.size(); i++) {
                section = depthDataArray.get(i);
                totalCount = depthBufferInfo.depthData.length;
                Log.d(TAG,
                        "<writeOnlyDepthBuffer> write depthData total count: 0x"
                                + Integer.toHexString(totalCount));
                rafOut.writeShort(XmpInterface.APP15);
                rafOut.writeShort(section.length + 2 + XmpInterface.TOTAL_LENGTH_TAG_BYTE);
                rafOut.writeInt(totalCount);
                rafOut.write(section);
            }
            Log.d(TAG, "<writeOnlyDepthBuffer> write end!!!");
            return true;
        } catch (IOException e) {
            Log.e(TAG, "<writeOnlyDepthBuffer> IOException ", e);
            return false;
        }
    }

    private ArrayList<byte[]> makeDepthData(byte[] depthData, byte[] xmpDepthData) {
        if (depthData == null && xmpDepthData == null) {
            Log.d(TAG, "<makeDepthData> depthData and xmpDepthData are null, skip!!!");
            return null;
        }

        int arrayIndex = 0;
        ArrayList<byte[]> depthDataArray = new ArrayList<byte[]>();

        for (int i = 0; i < 2; i++) {
            if (i == 0 && depthData == null) {
                Log.d(TAG, "<makeDepthData> depthData is null, skip!!!");
                continue;
            }
            if (i == 1 && xmpDepthData == null) {
                Log.d(TAG, "<makeDepthData> xmpDepthData is null, skip!!!");
                continue;
            }
            byte[] data = (i == 0 ? depthData : xmpDepthData);
            String header = (i == 0 ? XmpInterface.TYPE_DEPTH_DATA : XmpInterface.TYPE_XMP_DEPTH);
            int dataRemain = data.length;
            int dataOffset = 0;
            int sectionCount = 0;

            while (header.length() + 1 + dataRemain >= XmpInterface.DEPTH_PACKET_SIZE) {
                byte[] section = new byte[XmpInterface.DEPTH_PACKET_SIZE];
                // copy type
                System.arraycopy(header.getBytes(), 0, section, 0, header.length());
                // write section number
                section[header.length()] = (byte) sectionCount;
                // copy data
                System.arraycopy(data, dataOffset, section, header.length() + 1, section.length
                        - header.length() - 1);
                depthDataArray.add(arrayIndex, section);
                dataOffset += section.length - header.length() - 1;
                dataRemain = data.length - dataOffset;
                sectionCount++;
                arrayIndex++;
            }
            if (header.length() + 1 + dataRemain < XmpInterface.DEPTH_PACKET_SIZE) {
                byte[] section = new byte[header.length() + 1 + dataRemain];
                // copy type
                System.arraycopy(header.getBytes(), 0, section, 0, header.length());
                // write section number
                section[header.length()] = (byte) sectionCount;
                // write data
                System.arraycopy(data, dataOffset, section, header.length() + 1, dataRemain);
                depthDataArray.add(arrayIndex, section);
                arrayIndex++;
                // sectionCount++;
            }
        }
        return depthDataArray;
    }

    private byte[] getDepthDataFromJpgFile(String filePath) {
        RandomAccessFile rafIn = null;
        try {
            rafIn = new RandomAccessFile(filePath, "r");
            return getDepthDataFromSections(rafIn, true);
        } catch (IOException e) {
            Log.e(TAG, "<getDepthDataFromJpgFile> IOException ", e);
            return null;
        } finally {
            closeRandomAccessFile(rafIn);
        }
    }

    private byte[] getXmpDepthDataFromJpgFile(String filePath) {
        RandomAccessFile rafIn = null;
        try {
            Log.d(TAG, "<getXmpDepthDataFromJpgFile> run...");
            rafIn = new RandomAccessFile(filePath, "r");
            return getDepthDataFromSections(rafIn, false);
        } catch (IOException e) {
            Log.e(TAG, "<getXmpDepthDataFromJpgFile> IOException ", e);
            return null;
        } finally {
            closeRandomAccessFile(rafIn);
        }
    }

    private byte[] getDepthDataFromSections(RandomAccessFile rafIn, boolean isDepthOrXmpDepth) {
        try {
            Section sec = null;
            int dataLen = 0;
            int i = 0;
            for (; i < mParsedSectionsForGallery.size(); i++) {
                sec = mParsedSectionsForGallery.get(i);
                if (isDepthOrXmpDepth && sec.mIsDepthData) {
                    rafIn.seek(sec.mOffset + 2 + 2);
                    dataLen = rafIn.readInt();
                    Log.d(TAG, "<getDepthDataFromSections> type DEPTH DATA, dataLen: 0x"
                            + Integer.toHexString(dataLen));
                    break;
                }
                if (!isDepthOrXmpDepth && sec.mIsXmpDepth) {
                    rafIn.seek(sec.mOffset + 2 + 2);
                    dataLen = rafIn.readInt();
                    Log.d(TAG, "<getDepthDataFromSections> type XMP DEPTH, dataLen: 0x"
                            + Integer.toHexString(dataLen));
                    break;
                }
            }
            if (i == mParsedSectionsForGallery.size()) {
                Log.d(TAG, "<getDepthDataFromSections> can not find DEPTH INFO, return null");
                return null;
            }

            return getDepthData(rafIn, isDepthOrXmpDepth, dataLen);
        } catch (IOException e) {
            Log.e(TAG, "<getDepthDataFromSections> IOException ", e);
            return null;
        }
    }

    private byte[] getDepthData(RandomAccessFile rafIn, boolean isDepthOrXmpDepth, int dataLen)
            throws IOException {
        int app1Len = 0;
        int copyLen = 0;
        int byteOffset = 0;
        Section sec = null;
        byte[] data = new byte[dataLen];

        for (int i = 0; i < mParsedSectionsForGallery.size(); i++) {
            sec = mParsedSectionsForGallery.get(i);
            if (isDepthOrXmpDepth && sec.mIsDepthData) {
                rafIn.seek(sec.mOffset + 2);
                app1Len = rafIn.readUnsignedShort();
                copyLen = app1Len - 2 - XmpInterface.TOTAL_LENGTH_TAG_BYTE
                        - XmpInterface.TYPE_DEPTH_DATA.length()
                        - XmpInterface.DEPTH_SERIAL_NUM_TAG_BYTE;
                Log.d(TAG, "<getDepthData> app1Len: 0x" + Integer.toHexString(app1Len)
                        + ", copyLen 0x" + Integer.toHexString(copyLen));
                rafIn.skipBytes(XmpInterface.TOTAL_LENGTH_TAG_BYTE
                        + XmpInterface.TYPE_DEPTH_DATA.length()
                        + XmpInterface.DEPTH_SERIAL_NUM_TAG_BYTE);
                rafIn.read(data, byteOffset, copyLen);
                byteOffset += copyLen;
            }
            if (!isDepthOrXmpDepth && sec.mIsXmpDepth) {
                rafIn.seek(sec.mOffset + 2);
                app1Len = rafIn.readUnsignedShort();
                copyLen = app1Len - 2 - XmpInterface.TOTAL_LENGTH_TAG_BYTE
                        - XmpInterface.TYPE_XMP_DEPTH.length()
                        - XmpInterface.DEPTH_SERIAL_NUM_TAG_BYTE;
                Log.d(TAG, "<getDepthData> app1Len: 0x" + Integer.toHexString(app1Len)
                        + ", copyLen 0x" + Integer.toHexString(copyLen));
                rafIn.skipBytes(XmpInterface.TOTAL_LENGTH_TAG_BYTE
                        + XmpInterface.TYPE_XMP_DEPTH.length()
                        + XmpInterface.DEPTH_SERIAL_NUM_TAG_BYTE);
                rafIn.read(data, byteOffset, copyLen);
                byteOffset += copyLen;
            }
        }
        return data;
    }

    private DepthBufferInfo parseDepthBufferInfo(XMPMeta xmpMeta) {
        if (xmpMeta == null) {
            return null;
        }
        DepthBufferInfo depthBufferInfo = new DepthBufferInfo();
        depthBufferInfo.depthBufferWidth = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, DEPTH_BUFFER_WIDTH);
        depthBufferInfo.depthBufferHeight = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, DEPTH_BUFFER_HEIGHT);
        depthBufferInfo.xmpDepthWidth = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, XMP_DEPTH_WIDTH);
        depthBufferInfo.xmpDepthHeight = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, XMP_DEPTH_HEIGHT);
        depthBufferInfo.metaBufferWidth = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, META_BUFFER_WIDTH);
        depthBufferInfo.metaBufferHeight = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, META_BUFFER_HEIGHT);
        depthBufferInfo.touchCoordXLast = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, TOUCH_COORDX_LAST);
        depthBufferInfo.touchCoordYLast = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, TOUCH_COORDY_LAST);
        depthBufferInfo.depthOfFieldLast = mXmpInterface.getPropertyInteger(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, DEPTH_OF_FIELD_LAST);
        depthBufferInfo.depthBufferFlag = mXmpInterface.getPropertyBoolean(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, DEPTH_BUFFER_FLAG);
        depthBufferInfo.xmpDepthFlag = mXmpInterface.getPropertyBoolean(xmpMeta,
                MEDIATEK_IMAGE_REFOCUS_NAMESPACE, XMP_DEPTH_FLAG);
        return depthBufferInfo;
    }

    private byte[] encodingDepth(byte[] gDepthMapSmall, StereoDebugInfoParser parser) {
        if (gDepthMapSmall != null) {
            return Utils.encodePng(gDepthMapSmall, parser.getGoogleDepthWidth(),
                    parser.getGoogleDepthHeight());
        }
        return null;
    }

    private String getFileNameFromPath(String filePath) {
        if (filePath == null) {
            return null;
        }
        int start = filePath.lastIndexOf("/");
        if (start < 0 || start > filePath.length()) {
            return filePath;
        }
        return filePath.substring(start);
    }

    private static void makeDir(String filePath) {
        if (filePath == null) {
            return;
        }
        File dir = new File(filePath);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    private void closeStream(ByteArrayInputStreamExt is, ByteArrayOutputStreamExt os) {
        try {
            if (is != null) {
                is.close();
                is = null;
            }
            if (os != null) {
                os.close();
                os = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "<closeStream> close IOException ", e);
        }
    }

    private void closeRandomAccessFile(RandomAccessFile raf) {
        try {
            if (raf != null) {
                raf.close();
                raf = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "<closeRandomAccessFile> IOException when close ", e);
        }
    }
}
