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

#include "image_refocus.h"

namespace android {

#define LOG_TAG "Gallery2_ImageRefocus"

ImageRefocus::ImageRefocus(int jpsWidth, int jpsHeight, int maskWidth, int maskHeight, int posX, int posY,
        int viewWidth, int viewHeight, int orientation, int mainCamPos, int touchCoordX1st, int touchCoordY1st,
        int refocusMode, int depthRotation) {
    memset(&mRefocusInitInfo, 0, sizeof(mRefocusInitInfo));
    memset(&mRefocusTuningInfo, 0, sizeof(mRefocusTuningInfo));
    memset(&mRefocusImageInfo, 0, sizeof(mRefocusImageInfo));
    memset(&mRefocusResultInfo, 0, sizeof(mRefocusResultInfo));
    memset(mSourceFileName, 0, sizeof(mSourceFileName));

    m_pJpegFactory = new JpegFactory();
    mOrientation = orientation;
    mDepthRotation = depthRotation;
    mRefocusTuningInfo = {8, 16, 4, 0, 4, 1, 3.4};
    mRefocusImageInfo.ImgNum = 1;
    mRefocusImageInfo.ImgFmt = REFOCUS_IMAGE_YUV420;
    switch (refocusMode) {
    case 0:
        mRefocusImageInfo.Mode = REFOCUS_MODE_FULL;
        break;
    case 1:
        mRefocusImageInfo.Mode = REFOCUS_MODE_DEPTH_ONLY;
        break;
    case 2:
        mRefocusImageInfo.Mode = REFOCUS_MODE_DEPTH_AND_XMP;
        break;
    case 3:
        mRefocusImageInfo.Mode = REFOCUS_MODE_DEPTH_AND_REFOCUS;
        break;
    case 4:
        mRefocusImageInfo.Mode = REFOCUS_MODE_MAX;
        break;
    default:
        break;
    }

    // test number for generate depth map buffer, maybe need optimization in future
    mRefocusImageInfo.TouchCoordX = touchCoordX1st;
    mRefocusImageInfo.TouchCoordY = touchCoordY1st;
    mRefocusImageInfo.DepthOfField = testDepthOfField;

    mRefocusImageInfo.Width = jpsWidth;
    mRefocusImageInfo.Height = jpsHeight;
    mRefocusImageInfo.MaskWidth = maskWidth;
    mRefocusImageInfo.MaskHeight = maskHeight;
    mRefocusImageInfo.PosX = posX;
    mRefocusImageInfo.PosY = posY;
    mRefocusImageInfo.ViewWidth = viewWidth;
    mRefocusImageInfo.ViewHeight = viewHeight;
    mRefocusImageInfo.JPSOrientation = REFOCUS_ORIENTATION_0;
    mRefocusImageInfo.JPGOrientation = REFOCUS_ORIENTATION_0;
    mRefocusImageInfo.MainCamPos = (REFOCUS_MAINCAM_POS_ENUM)mainCamPos;

    // default rectify info in dual cam refocus
    mRefocusImageInfo.RcfyError = DFT_RCFY_ERROR;
    mRefocusImageInfo.RcfyIterNo = DFT_RCFY_ITER_NO;
    mRefocusImageInfo.DisparityRange = DFT_DISPARITY_RANGE;
    mRefocusImageInfo.Theta[0] = DFT_THETA;
    mRefocusImageInfo.Theta[1] = DFT_THETA;
    mRefocusImageInfo.Theta[2] = DFT_THETA;
    mRefocusImageInfo.DepthBufferAddr = NULL;
    ALOGI("<ImageRefocus>mRefocusImageInfo.RcfyError %d, ", mRefocusImageInfo.RcfyError);

    mRefocusTuningInfo.IterationTimes = 3;
    mRefocusTuningInfo.HorzDownSampleRatio = 4;
    mRefocusTuningInfo.VertDownSampleRatio = 4;
    mRefocusImageInfo.DRZ_WD = 960;
    mRefocusImageInfo.DRZ_HT = 540;

    mRefocusTuningInfo.Baseline = 2.0f;
    mRefocusTuningInfo.CoreNumber = 4;

    // for debug
    if (access(REFOCUS_DUMP_PATH, 0) != -1) {
        m_DebugUtils = new DebugUtils(REFOCUS_DUMP_PATH);
    }
}

bool ImageRefocus::initRefocusNoDepthMap(const char *sourceFilePath, int outImgWidth, int outImgHeight,
        int imgOrientation, MUINT8* jpsBuffer, int jpsBufferSize, int jpsWidth, int jpsHeight,
        MUINT8* maskBuffer, int maskBufferSize, int maskWidth, int maskHeight,
        uint8_t* ldcBuffer, int ldcBufferSize, int ldcWidth, int ldcHeight) {
    ALOGI("<initRefocusNoDepthMap>inStereoImgWidth %d, inStereoImgHeight %d, outImgWidth %d, outImgHeight %d,"
            "instance ID %p", jpsWidth, jpsHeight, outImgWidth, outImgHeight, this);

    // decode jpeg file
    initRefocusIMGSource(sourceFilePath, outImgWidth, outImgHeight, imgOrientation);

    if (!initJPSBuffer(jpsBuffer, jpsBufferSize, jpsWidth, jpsHeight)) {
        ALOGI("<initRefocusNoDepthMap> initJPSBuffer fail!!");
        return false;
    }
    mPMaskBuffer = (unsigned char *) malloc(maskBufferSize);
    memcpy(mPMaskBuffer, maskBuffer, maskBufferSize);
    mRefocusImageInfo.MaskWidth = maskWidth;
    mRefocusImageInfo.MaskHeight = maskHeight;
    mRefocusImageInfo.MaskImageAddr = mPMaskBuffer;

    mRefocusImageInfo.NumOfMetadata = 1;
    mRefocusImageInfo.metaInfo[0].Type = REFOCUS_METADATA_TYPE_LDC_MAP;
    mPLdcBuffer = (unsigned char *) malloc(ldcBufferSize);
    memcpy(mPLdcBuffer, ldcBuffer, ldcBufferSize);
    mRefocusImageInfo.metaInfo[0].Width = ldcWidth;
    mRefocusImageInfo.metaInfo[0].Height = ldcHeight;
    mRefocusImageInfo.metaInfo[0].Size = ldcBufferSize;
    mRefocusImageInfo.metaInfo[0].Addr = mPLdcBuffer;

    if (NULL != m_DebugUtils) {
        m_DebugUtils->dumpBufferToFile("ldc.raw", ldcBuffer, ldcBufferSize);
    }

    if (createRefocusInstance() && setBufferAddr()) {
        ALOGI("<initRefocusNoDepthMap>image refocus success, instance ID %p", this);
        return SUCCESS;
    }
    ALOGI("ERROR: image refocus fail!!!, instance ID %p", this);
    return FAIL;
}

bool ImageRefocus::initRefocusNoDepthMap(uint8_t* jpegBuf, int jpegbufferSize, int outImgWidth,
        int outImgHeight, int orientation, uint8_t* jpsBuffer, int jpsBufferSize, int jpsWidth, int jpsHeight,
        uint8_t* maskBuffer, int maskBufferSize, int maskWidth, int maskHeight,
        uint8_t* ldcBuffer, int ldcBufferSize, int ldcWidth, int ldcHeight) {
    ALOGI("<initRefocusNoDepthMap> jpsWidth %d, jpsHeight %d,"
            "outImgWidth %d, outImgHeight %d, maskWidth %d, maskHeight %d instance ID %p",
            jpsWidth, jpsHeight, outImgWidth, outImgHeight, maskWidth, maskHeight, this);

    // decode jpeg buffer
    if (!initRefocusIMGSource(jpegBuf, jpegbufferSize, outImgWidth, outImgHeight)) {
        ALOGI("ERROR: initRefocusIMGSource fail!!");
        return false;
    }

    if (!initJPSBuffer(jpsBuffer, jpsBufferSize, jpsWidth, jpsHeight)) {
        ALOGI("ERROR: initJPSBuffer fail!!");
        return false;
    }
    // 3. mask parse start
    mPMaskBuffer = (unsigned char *) malloc(maskBufferSize);
    memcpy(mPMaskBuffer, maskBuffer, maskBufferSize);
    mRefocusImageInfo.MaskWidth = maskWidth;
    mRefocusImageInfo.MaskHeight = maskHeight;
    mRefocusImageInfo.MaskImageAddr = mPMaskBuffer;

    mRefocusImageInfo.NumOfMetadata = 1;
    mRefocusImageInfo.metaInfo[0].Type = REFOCUS_METADATA_TYPE_LDC_MAP;
    mPLdcBuffer = (unsigned char *) malloc(ldcBufferSize);
    memcpy(mPLdcBuffer, ldcBuffer, ldcBufferSize);
    mRefocusImageInfo.metaInfo[0].Width = ldcWidth;
    mRefocusImageInfo.metaInfo[0].Height = ldcHeight;
    mRefocusImageInfo.metaInfo[0].Size = ldcBufferSize;
    mRefocusImageInfo.metaInfo[0].Addr = mPLdcBuffer;

    if (NULL != m_DebugUtils) {
        m_DebugUtils->dumpBufferToFile("ldc.raw", ldcBuffer, ldcBufferSize);
    }

    if (createRefocusInstance() && setBufferAddr()) {
        ALOGI("<initRefocusNoDepthMap>image refocus success, instance ID %p", this);
        return SUCCESS;
    }
    ALOGI("ERROR: image refocus fail!!!, instance ID %p", this);
    return FAIL;
}

bool ImageRefocus::initRefocusWithDepthMap(const char *sourceFilePath, int outImgWidth, int outImgHeight,
        int orientation, MUINT8* depthBuffer, int depthBufferSize, int jpsWidth, int jpsHeight) {
    ALOGI("<initRefocusWithDepthMap> outImgWidth %d, outImgHeight %d, depthBuffer %, depthBufferSize %d",
             outImgWidth, outImgHeight, depthBuffer, depthBufferSize);

    initRefocusIMGSource(sourceFilePath, outImgWidth, outImgHeight, orientation);

    mRefocusImageInfo.Width = jpsWidth;
    mRefocusImageInfo.Height = jpsHeight;
    mRefocusImageInfo.DepthBufferAddr = depthBuffer;
    mRefocusImageInfo.DepthBufferSize = depthBufferSize;

    swapConfigInfo();

    if (createRefocusInstance() && setBufferAddr()) {
        ALOGI("<initRefocusWithDepthMap>image refocus init end, success");
        return SUCCESS;
    }
    ALOGI("ERROR: image refocus init end, fail");
    return FAIL;
}

bool ImageRefocus::initRefocusWithDepthMap(uint8_t* jpegBuf, int jpegbufferSize, int outImgWidth, int outImgHeight,
        int orientation, MUINT8* depthBuffer, int depthBufferSize, int jpsWidth, int jpsHeight) {
    ALOGI("<initRefocusWithDepthMap> outImgWidth %d, outImgHeight %d, depthBuffer %p, depthBufferSize %d",
            outImgWidth, outImgHeight, depthBuffer, depthBufferSize);

    // decode jpeg buffer
    if (!initRefocusIMGSource(jpegBuf, jpegbufferSize, outImgWidth, outImgHeight)) {
        ALOGI("ERROR: initRefocusIMGSource fail!!");
        return false;
    }

    mRefocusImageInfo.Width = jpsWidth;
    mRefocusImageInfo.Height = jpsHeight;
    mRefocusImageInfo.DepthBufferAddr = depthBuffer;
    mRefocusImageInfo.DepthBufferSize = depthBufferSize;

    swapConfigInfo();

    if (createRefocusInstance() && setBufferAddr()) {
        ALOGI("<initRefocusWithDepthMap>image refocus init end, success");
        return SUCCESS;
    }
    ALOGI("ERROR:image refocus init end, fail");
    return FAIL;
}

void ImageRefocus::swapConfigInfo() {
    // when photo is captured at landscape mode, and jps is portrait, jpg is land
    // and depthbuffer can be generated when jps is portrait. so we need generate
    // depthbuffer at portrait mode ,then rotate depthbuffer to land.
    // so we do NOT swap maskWidth/maskHeight, PosX/PosY, ViewWidth/ViewHeight at initRefocusNoDepthMap
    // because we need portrait depthbuffer.
    // after depthbuffer's rotation, we can swap those params to generate land image, so when swap here
    if (mDepthRotation == ORIENTATION_90 || mDepthRotation == ORIENTATION_270) {
        swap(&mRefocusImageInfo.MaskWidth, &mRefocusImageInfo.MaskHeight);
        swap(&mRefocusImageInfo.PosX, &mRefocusImageInfo.PosY);
        swap(&mRefocusImageInfo.ViewWidth, &mRefocusImageInfo.ViewHeight);
        ALOGI("<swapConfigInfo>after swapping, maskWidth %d, maskHeight %d, ViewWidth %d, ViewHeight %d",
                mRefocusImageInfo.MaskWidth, mRefocusImageInfo.MaskHeight, mRefocusImageInfo.ViewWidth,
                mRefocusImageInfo.ViewHeight);
    }
}

void ImageRefocus::initRefocusIMGSource(const char *sourceFilePath, int outImgWidth, int outImgHeight,
        int imgOrientation) {

    ImageInfo imageInfo;
    sprintf(mSourceFileName, "%s", sourceFilePath);
    ALOGI("<initRefocusIMGSource>start");
    m_pJpegFactory->jpgDecode(sourceFilePath, &imageInfo, 1);
    ALOGI("<initRefocusIMGSource> decode image resource end, destBuffer:%p,width:%d,height:%d",
            imageInfo.destBuffer, imageInfo.width, imageInfo.height);

    // for target image
    mRefocusImageInfo.TargetWidth = imageInfo.width;
    mRefocusImageInfo.TargetHeight = imageInfo.height;
    mRefocusImageInfo.TargetImgAddr = (MUINT8*)imageInfo.destBuffer;

    if (NULL != m_DebugUtils) {
        m_DebugUtils->dumpBufferToFile("jpg.yuv", imageInfo.destBuffer, imageInfo.bufferSize);
    }
}

bool ImageRefocus::initRefocusIMGSource(uint8_t* jpegBuf, int bufSize, int outImgWidth,
        int outImgHeight) {

    if (jpegBuf == NULL) {
        ALOGI("<initRefocusIMGSource> null jpeg buffer!!!");
        return false;
    }

    ImageInfo imageInfo;
    unsigned char* file_buffer = (unsigned char *) malloc(ALIGN128(bufSize) + 512 + 127);
    unsigned char* align128_file_buffer = (unsigned char *)((((size_t)file_buffer + 127) >> 7) << 7);

    if (file_buffer == NULL) {
        ALOGI("ERROR: malloc memory fail!!!");
        return false;
    }
    memcpy(align128_file_buffer, jpegBuf, bufSize);
    if (!m_pJpegFactory->jpgToYV12(align128_file_buffer, bufSize, &imageInfo, 1)) {
        ALOGI("ERROR: decode failed!!");
        free(file_buffer);
        file_buffer = NULL;
        return false;
    }

    ALOGI("<initRefocusIMGSource>decode image end,destBuffer:%p,width:%d,height:%d",
            imageInfo.destBuffer, imageInfo.width, imageInfo.height);

    free(file_buffer);
    file_buffer = NULL;
    // for target image
    mRefocusImageInfo.TargetWidth = imageInfo.width;
    mRefocusImageInfo.TargetHeight = imageInfo.height;
    mRefocusImageInfo.TargetImgAddr = (MUINT8*)imageInfo.destBuffer;
    if (NULL != m_DebugUtils) {
        m_DebugUtils->dumpBufferToFile("jpg.yuv", imageInfo.destBuffer, imageInfo.bufferSize);
    }
    return true;
}

bool ImageRefocus::initJPSBuffer(MUINT8* jpsBuffer, int jpsBufferSize, int jpsWidth, int jpsHeight) {
    ALOGI("<initJPSBuffer> jpsBufferSize %d, jpsWidth %d, jpsHeight %d", jpsBufferSize, jpsWidth, jpsHeight);
    ImageInfo imageInfo;

    unsigned char* pJpsBuffer = (unsigned char *) malloc(ALIGN128(jpsBufferSize) + 512 + 127);
    unsigned char * align128_file_buffer = (unsigned char *) ((((size_t)pJpsBuffer + 127) >> 7) << 7);

    if (pJpsBuffer == NULL) {
        ALOGI("ERROR: pJpsBuffer malloc fail!!!");
        return false;
    }
    memcpy(align128_file_buffer, jpsBuffer, jpsBufferSize);
    if (!m_pJpegFactory->jpgToYV12(align128_file_buffer, jpsBufferSize, &imageInfo, 1)) {
        ALOGI("ERROR: decode failed!!");
        free(pJpsBuffer);
        pJpsBuffer = NULL;
        return false;
    }

    free(pJpsBuffer);
    pJpsBuffer = NULL;

    // jps parse start
    mRefocusImageInfo.Width = imageInfo.width;
    mRefocusImageInfo.Height = imageInfo.height;
    mRefocusImageInfo.ImgAddr[0] = (MUINT8*)imageInfo.destBuffer;

    if (NULL != m_DebugUtils) {
        m_DebugUtils->dumpBufferToFile("jps.yuv", imageInfo.destBuffer, imageInfo.bufferSize);
    }
    return true;
}

bool ImageRefocus::createRefocusInstance() {
    ALOGI("<createRefocusInstance>createRefocusInstance start");
    MUINT32 initResult;
    // init
    mRefocusInitInfo.pTuningInfo = &mRefocusTuningInfo;

    mStereoUtils.starMeasureTime();
    mRefocus = mRefocus->createInstance(DRV_REFOCUS_OBJ_SW);
    mStereoUtils.endMeasureTime("<createRefocusInstance>performance createInstance");

    mStereoUtils.starMeasureTime();
    initResult = mRefocus->RefocusInit((MUINT32 *)&mRefocusInitInfo, 0);
    mStereoUtils.endMeasureTime("<createRefocusInstance>performance RefocusInit");

    if (initResult != S_REFOCUS_OK) {
        ALOGI("ERROR: image refocus createRefocusInstance fail ");
        return FAIL;
    }
    ALOGI("<createRefocusInstance>createRefocusInstance success ");
    return SUCCESS;
}

bool ImageRefocus::setBufferAddr() {
    ALOGI("<setBufferAddr> start");
    // get buffer size
    MUINT32 result;
    MUINT32 buffer_size;

    ALOGI("<setBufferAddr> TargetWidth %d, TargetHieght %d, TargetImgAddr %p  ImgNum %d, Width %d Height %d ImgAddr %p "
            "DepthBufferAddr %p  DepthBufferSize %d Orientation %d  MainCamPos %d", mRefocusImageInfo.TargetWidth,
            mRefocusImageInfo.TargetHeight, mRefocusImageInfo.TargetImgAddr, mRefocusImageInfo.ImgNum,
            mRefocusImageInfo.Width, mRefocusImageInfo.Height, mRefocusImageInfo.ImgAddr,
            mRefocusImageInfo.DepthBufferAddr, mRefocusImageInfo.DepthBufferSize ,
            mRefocusImageInfo.JPSOrientation , mRefocusImageInfo.MainCamPos);

    mStereoUtils.starMeasureTime();
    result = mRefocus->RefocusFeatureCtrl(REFOCUS_FEATURE_GET_WORKBUF_SIZE, (void *)&mRefocusImageInfo,
            (void *)&buffer_size);
    mStereoUtils.endMeasureTime("<setBufferAddr>performance get workbuff size");

    ALOGI("<setBufferAddr>REFOCUS_FEATURE_GET_WORKBUF_SIZE buffer size  %d, result %d ", buffer_size, result);
    if (result != S_REFOCUS_OK) {
        ALOGI("ERROR: image refocus GET_WORKBUF_SIZE fail ");
        return FAIL;
    }

    // set buffer address
    // unsigned char *pWorkingBuffer = new unsigned char[buffer_size];
    pWorkingBuffer = (unsigned char *) malloc(buffer_size);
    mRefocusInitInfo.WorkingBuffAddr = (MUINT8*)pWorkingBuffer;

    ALOGI("<setBufferAddr> SET_WORKBUF_ADDR start");
    mStereoUtils.starMeasureTime();
    result = mRefocus->RefocusFeatureCtrl(REFOCUS_FEATURE_SET_WORKBUF_ADDR,
            (void *)&mRefocusInitInfo.WorkingBuffAddr, NULL);
    mStereoUtils.endMeasureTime("<setBufferAddr>performance set work buffer size");

    if (result != S_REFOCUS_OK) {
        ALOGI("ERROR: image refocus SET_WORKBUF_ADDR fail ");
        return FAIL;
    }
    ALOGI("<setBufferAddr> SET_WORKBUF_ADDR success");
    return SUCCESS;
}

bool ImageRefocus::generate() {
    MUINT32 result;
    // algorithm - gen depth map
    ALOGI("<generate>generate start,RcfyError:%d,JPSOrientation:%d",
            mRefocusImageInfo.RcfyError, mRefocusImageInfo.JPSOrientation);
    ALOGI("<generate> xCoord:%d,yCoord:%d,dof:%d", mRefocusImageInfo.TouchCoordX,
            mRefocusImageInfo.TouchCoordY, mRefocusImageInfo.DepthOfField);

    mStereoUtils.starMeasureTime();
    result = mRefocus->RefocusFeatureCtrl(REFOCUS_FEATURE_ADD_IMG, (void *)&mRefocusImageInfo, NULL);
    mStereoUtils.endMeasureTime("<generate> feature add img");

    if (result != S_REFOCUS_OK) {
        ALOGI("ERROR: image refocus ADD_IMG fail ");
        return FAIL;
    }

    mStereoUtils.starMeasureTime();
    result = mRefocus->RefocusMain();
    mStereoUtils.endMeasureTime("<generate> RefocusMain");

    if (result != S_REFOCUS_OK) {
        ALOGI("ERROR: image refocus RefocusMain fail,result:%d", result);
        return FAIL;
    }
    mStereoUtils.starMeasureTime();
    result = mRefocus->RefocusFeatureCtrl(REFOCUS_FEATURE_GET_RESULT, NULL, (void *)&mRefocusResultInfo);
    mStereoUtils.endMeasureTime("<generate> feature get result");

    ALOGI("<generate>RefocusImageWidth:%d,RefocusImageHeight:%d,DepthBufferWidth:%d,DepthBufferHeight:%d",
            mRefocusResultInfo.RefocusImageWidth, mRefocusResultInfo.RefocusImageHeight,
            mRefocusResultInfo.DepthBufferWidth, mRefocusResultInfo.DepthBufferHeight);
    ALOGI("<generate>DepthBufferSize:%d,DepthBufferAddr:%p", mRefocusResultInfo.DepthBufferSize,
            mRefocusImageInfo.DepthBufferAddr);
    if (result != S_REFOCUS_OK) {
        ALOGI("ERROR: image refocus GET_RESULT fail,result:%d", result);
        return FAIL;
    }
    if (mRefocusImageInfo.DepthBufferAddr == NULL) {
        MUINT8* depthBuffer = new MUINT8[mRefocusResultInfo.DepthBufferSize];
        memcpy(depthBuffer, mRefocusResultInfo.DepthBufferAddr, mRefocusResultInfo.DepthBufferSize);
        mRefocusImageInfo.DepthBufferAddr = depthBuffer;
        mRefocusImageInfo.DepthBufferSize = mRefocusResultInfo.DepthBufferSize;
        copyRefocusResultInfo(&mRefocusResultInfo);
        ALOGI("<generate>copy depthBuffer from %p to %p", mRefocusResultInfo.DepthBufferAddr, depthBuffer);
    }
    return SUCCESS;
}

ImageBuffer ImageRefocus::generateRefocusImage(int touchCoordX, int touchCoordY, int depthOfField) {
    ImageBuffer image;
    memset(&image, 0, sizeof(image));

    mRefocusImageInfo.TouchCoordX = touchCoordX;
    mRefocusImageInfo.TouchCoordY = touchCoordY;
    mRefocusImageInfo.DepthOfField = depthOfField;
    if (generate()) {
        ALOGI("generate success");
        image.width = mRefocusResultInfo.RefocusImageWidth;
        image.height = mRefocusResultInfo.RefocusImageHeight;
        image.buffer = mRefocusResultInfo.RefocusedRGBAImageAddr;
    }

    return image;
}

void ImageRefocus::deinit() {
    ALOGI("<deinit>start release memory!");
    if (NULL != pWorkingBuffer) {
        free(pWorkingBuffer);
        pWorkingBuffer = NULL;
    }
    if (NULL != mRefocusImageInfo.TargetImgAddr) {
        free(mRefocusImageInfo.TargetImgAddr);
        mRefocusImageInfo.TargetImgAddr = NULL;
    }
    if (NULL != mRefocusImageInfo.ImgAddr[0]) {
        free(mRefocusImageInfo.ImgAddr[0]);
        mRefocusImageInfo.ImgAddr[0] = NULL;
    }
    if (NULL != mRefocus) {
        mRefocus->RefocusReset();
        ALOGI("<deinit>RefocusReset");
        mRefocus->destroyInstance(mRefocus);
        mRefocus = NULL;
    }
    if (NULL != mPLdcBuffer) {
        free(mPLdcBuffer);
        mPLdcBuffer = NULL;
    }
    if (NULL != mPMaskBuffer) {
        free(mPMaskBuffer);
        mPMaskBuffer = NULL;
    }
    ALOGI("<deinit>end");
}

int ImageRefocus::getDepthBufferSize() {
    ALOGI("<getDepthBufferSize>DepthBufferSize %d,instance ID:%p ", mDepthBufferSize, this);
    return mDepthBufferSize;
}

int ImageRefocus::getDepthBufferWidth() {
    if (mDepthRotation == ORIENTATION_90 || mDepthRotation == ORIENTATION_270) {
        ALOGI("<getDepthBufferWidth>%d, instance ID %p ", mDepthBufferHeight, this);
        return mDepthBufferHeight;
    }
    ALOGI("<getDepthBufferWidth>%d, instance ID %p", mDepthBufferWidth, this);
    return mDepthBufferWidth;
}

int ImageRefocus::getDepthBufferHeight() {
    if (mDepthRotation == ORIENTATION_90 || mDepthRotation == ORIENTATION_270) {
        ALOGI("<getDepthBufferHeight>%d, instance ID %p", mDepthBufferWidth, this);
        return mDepthBufferWidth;
    }
    ALOGI("<getDepthBufferHeight>%d, instance ID %p", mDepthBufferHeight, this);
    return mDepthBufferHeight;
}

int ImageRefocus::getXMPDepthBufferSize() {
    ALOGI("<getXMPDepthBufferSize>mXMPDepthSize %d, instance ID %p", mXMPDepthSize, this);
    return mXMPDepthSize;
}

int ImageRefocus::getXMPDepthBufferWidth() {
    ALOGI("<getXMPDepthBufferWidth>mXMPDepthWidth %d, instance ID %p", mXMPDepthWidth, this);
    return mXMPDepthWidth;
}

int ImageRefocus::getXMPDepthBufferHeight() {
    ALOGI("<getXMPDepthBufferHeight>mXMPDepthHeight %d, instance ID %p", mXMPDepthHeight, this);
    return mXMPDepthHeight;
}

int ImageRefocus::getMetaBufferWidth() {
    if (mDepthRotation == ORIENTATION_90 || mDepthRotation == ORIENTATION_270) {
        ALOGI("<getMetaBufferWidth>%d, instance ID %p", mMetaBufferHeight, this);
        return mMetaBufferHeight;
    }
    ALOGI("<getMetaBufferWidth>%d, instance ID %p", mMetaBufferWidth, this);
    return mMetaBufferWidth;
}
int ImageRefocus::getMetaBufferHeight() {
    if (mDepthRotation == ORIENTATION_90 || mDepthRotation == ORIENTATION_270) {
        ALOGI("<getMetaBufferHeight>%d, instance ID %p", mMetaBufferWidth, this);
        return mMetaBufferWidth;
    }
    ALOGI("<getMetaBufferHeight>%d, instance ID %p", mMetaBufferHeight, this);
    return mMetaBufferHeight;
}

void ImageRefocus::saveDepthMapInfo(MUINT8* depthBufferArray, MUINT8* xmpDepthBufferArray) {
    ALOGI("<saveDepthMapInfo>DepthBufferSize %d ", mRefocusResultInfo.DepthBufferSize);
    memcpy(depthBufferArray, (MUINT8*)mRefocusResultInfo.DepthBufferAddr, mRefocusResultInfo.DepthBufferSize);
    memcpy(xmpDepthBufferArray, (MUINT8*)mRefocusResultInfo.XMPDepthMapAddr,
           mRefocusResultInfo.XMPDepthWidth * mRefocusResultInfo.XMPDepthHeight);

    if (mDepthRotation == ORIENTATION_90 || mDepthRotation == ORIENTATION_180
            || mDepthRotation == ORIENTATION_270) {
        mStereoUtils.starMeasureTime();

        int offset = 0;
        int bufferWidth = 0;
        int bufferHeight = 0;
        int depthBufferWidth = mRefocusResultInfo.DepthBufferWidth;
        int depthBufferHeight = mRefocusResultInfo.DepthBufferHeight;
        int metaBufferWidth = mRefocusResultInfo.MetaBufferWidth;
        int metaBufferHeight = mRefocusResultInfo.MetaBufferHeight;
        char name[FILE_NAME_LENGTH];
        for (int i = 0; i < DEPTH_BUFFER_SECTION_SIZE; i++) {
            if (i == WMI_DEPTH_MAP_INDEX) {
                offset = 0;
                bufferWidth = depthBufferWidth;
                bufferHeight = depthBufferHeight;
            } else if (i == VAR_BUFFER_INDEX || i == DVEC_MAP_INDEX) {
                continue;  // no need to rotate
            } else if (i == DS4_BUFFER_Y_INDEX) {
                offset = depthBufferWidth * depthBufferHeight + (i - 1) * metaBufferWidth * metaBufferHeight;
                bufferWidth = depthBufferWidth;
                bufferHeight = depthBufferHeight;
            } else {
                offset = depthBufferWidth * depthBufferHeight + (i - 1)* metaBufferWidth * metaBufferHeight;
                bufferWidth = metaBufferWidth;
                bufferHeight = metaBufferHeight;
            }
            ALOGI("<saveDepthMapInfo>rotate section %d: offset %d, bufferWidth %d, bufferHeight %d", i, offset,
                    bufferWidth, bufferHeight);
            if (NULL != m_DebugUtils) {
                sprintf(name, "section_%d_before_rotate.ppm", i);
                m_DebugUtils->dumpBufferToPPM(name, depthBufferArray + offset, bufferWidth, bufferHeight, 5, 3);
            }
            rotateBuffer((MUINT8*)mRefocusResultInfo.DepthBufferAddr + offset, depthBufferArray + offset,
                    bufferWidth, bufferHeight,  mDepthRotation);
            if (NULL != m_DebugUtils) {
                sprintf(name, "section_%d_after_rotate.ppm", i);
                m_DebugUtils->dumpBufferToPPM(name, depthBufferArray + offset, bufferHeight, bufferWidth, 5, 3);
            }
        }

        mStereoUtils.endMeasureTime("<saveDepthMapInfo>mRefocus->saveDepthMapInfo,rotating depthBuffer");
    }
}

void ImageRefocus::saveRefocusImage(const char *saveFileName, int inSampleSize) {
    char file[FILE_NAME_LENGTH];
    FILE *fp;
    MUINT32 result;
    unsigned char *jpgBuf = NULL;
    unsigned int jpegSize = 0;

    mRefocusImageInfo.TouchCoordX = mRefocusImageInfo.TouchCoordX;
    mRefocusImageInfo.TouchCoordY = mRefocusImageInfo.TouchCoordY;
    mRefocusImageInfo.DepthOfField = mRefocusImageInfo.DepthOfField;
    mRefocusImageInfo.Mode = REFOCUS_MODE_DEPTH_AND_REFOCUS_SAVEAS;
    mStereoUtils.starMeasureTime();
    result = mRefocus->RefocusFeatureCtrl(REFOCUS_FEATURE_ADD_IMG, (void *)&mRefocusImageInfo, NULL);
    if (result != S_REFOCUS_OK) {
        ALOGI("ERROR: image refocus ADD_IMG fail ");
        return;
    }
    result = mRefocus->RefocusMain();
    if (result != S_REFOCUS_OK) {
        ALOGI("ERROR: image refocus RefocusMain fail ");
        return;
    }
    result = mRefocus->RefocusFeatureCtrl(REFOCUS_FEATURE_GET_RESULT, NULL, (void *)&mRefocusResultInfo);
    if (result != S_REFOCUS_OK) {
        ALOGI("ERROR: image refocus GET_RESULT fail ");
        return;
    }
    mStereoUtils.endMeasureTime("<saveRefocusImage>performance generate");

    sprintf(file, "%s", saveFileName);
    ALOGI("<saveRefocusImage>test file:%s", file);
    fp = fopen(file, "w");
    if (fp == NULL) {
        ALOGI("ERROR: Open file %s failed!!!", file);
        return;
    }

    // should free this memory when not use it !!!
    int imageWidth = mRefocusResultInfo.RefocusImageWidth;
    int imageHeight = mRefocusResultInfo.RefocusImageHeight;
    jpgBuf = (unsigned char *) malloc(imageWidth * imageHeight);
    if (jpgBuf == NULL) {
        ALOGI("ERROR: Can not allocate memory!!!");
        fclose(fp);
        return;
    }
    if (NULL != m_DebugUtils) {
        m_DebugUtils->dumpBufferToFile("save.yuv", mRefocusResultInfo.RefocusedYUVImageAddr,
                imageWidth*imageHeight*3/2);
    }

    ALOGI("<saveRefocusImage>RefocusedYUVImageAddr:%p", (size_t)mRefocusResultInfo.RefocusedYUVImageAddr);
    mStereoUtils.starMeasureTime();
    m_pJpegFactory->yv12ToJpg((unsigned char *)mRefocusResultInfo.RefocusedYUVImageAddr, imageWidth * imageHeight,
            imageWidth, imageHeight, jpgBuf, imageWidth * imageHeight, &jpegSize);
    mStereoUtils.endMeasureTime("<saveRefocusImage>performance yv12ToJpg");

    dumpBufferToFile(jpgBuf, jpegSize, file);
    free(jpgBuf);
    fclose(fp);
    jpgBuf = NULL;
}

ImageRefocus::~ImageRefocus() {
    if (mRefocusImageInfo.DepthBufferAddr != NULL) {
        delete mRefocusImageInfo.DepthBufferAddr;
    }
    if (m_pJpegFactory != NULL) {
        delete m_pJpegFactory;
        m_pJpegFactory = NULL;
    }
    if (NULL != m_DebugUtils) {
        delete m_DebugUtils;
        m_DebugUtils = NULL;
    }
    ALOGI("~ImageRefocus");
}

void ImageRefocus::dumpBufferToFile(unsigned char* buffer, int bufferSize, char* fileName) {
    FILE* fp;
    int index;

    ALOGI("<dumpBufferToFile>buffer address:%p, bufferSize %d, fileName:%s", buffer,
            bufferSize, fileName);

    if (buffer == NULL) {
        ALOGI("ERROR: null buffer address, dump fail!!!");
        return;
    }

    fp = fopen(fileName, "w");
    if (fp == NULL) {
        ALOGI("ERROR: Open file %s failed.", fileName);
        return;
    }

    for (index = 0 ; index < bufferSize; index++) {
        fprintf(fp, "%c", buffer[index]);
    }
    fclose(fp);
    ALOGI("<dumpBufferToFile>dump buffer to file success!");
}

void ImageRefocus::copyRefocusResultInfo(RefocusResultInfo* refocusResultInfo) {
    if (NULL == refocusResultInfo) {
        return;
    }
    mDepthBufferWidth = refocusResultInfo->DepthBufferWidth;
    mDepthBufferHeight = refocusResultInfo->DepthBufferHeight;
    mDepthBufferSize = refocusResultInfo->DepthBufferSize;
    mMetaBufferWidth = refocusResultInfo->MetaBufferWidth;
    mMetaBufferHeight = refocusResultInfo->MetaBufferHeight;
    mXMPDepthWidth = refocusResultInfo->XMPDepthWidth;
    mXMPDepthHeight = refocusResultInfo->XMPDepthHeight;
    mXMPDepthSize = mXMPDepthWidth * mXMPDepthHeight;
    ALOGI("<copyRefocusResultInfo>instance ID %p, mDepthBufferWidth %d, mDepthBufferHeight %d,"
            "mDepthBufferSize %d, mMetaBufferWidth %d, mMetaBufferHeight %d,"
            "mXMPDepthWidth %d, mXMPDepthHeight %d, mXMPDepthSize %d", this,
            mDepthBufferWidth, mDepthBufferHeight, mDepthBufferSize, mMetaBufferWidth,
            mMetaBufferHeight, mXMPDepthWidth, mXMPDepthHeight, mXMPDepthSize);
}

void ImageRefocus::rotateBuffer(MUINT8*  bufferIn, MUINT8*  bufferOut, int bufferWidth,
        int bufferHeight, int orientation) {
    ALOGI("<rotateBuffer>bufferWidth %d, bufferHeight %d, orientation %d",
            bufferWidth, bufferHeight, orientation);

    int index = 0;
    switch (orientation) {
    case ORIENTATION_90:
        // rotate 90 degree, clockwise
        index = 0;
        for (int i = bufferHeight - 1; i >= 0; i--) {
            for (int j = 0; j < bufferWidth; j++) {
                bufferOut[i + j * bufferHeight] = bufferIn[index];
                index++;
            }
        }
        break;

    case ORIENTATION_270:
        // rotate 270 degree, clockwise
        index = 0;
        for (int i = 0; i < bufferHeight; i++) {
            for (int j = bufferWidth - 1; j >= 0; j--) {
                bufferOut[i + j * bufferHeight] = bufferIn[index];
                index++;
            }
        }
        break;

    case ORIENTATION_180:
        // rotate 180 degree, clockwise
        index = 0;
        for (int j = bufferHeight - 1; j >= 0; j--) {
            for (int i = bufferWidth - 1; i >= 0; i--) {
                bufferOut[i + j * bufferWidth] = bufferIn[index];
                index++;
            }
        }
        break;

    case ORIENTATION_0:
    default:
        // no need rotation
        bufferOut = bufferIn;
        break;
    }
}

void ImageRefocus::swap(unsigned int* x, unsigned int* y) {
    unsigned int temp = *x;
    *x = *y;
    *y = temp;
}
}  // namespace android
