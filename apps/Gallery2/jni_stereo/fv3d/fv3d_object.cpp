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

#include "fv3d_object.h"

#define LOG_TAG "Gallery2_fv3d_object"

FV3DObject::FV3DObject() {
    memset(&mProcInfo, 0, sizeof(mProcInfo));
    if (access(FREEVIEW_DUMP_PATH, 0) != -1) {
        m_DebugUtils = new DebugUtils(FREEVIEW_DUMP_PATH);
    }
}

FV3DObject::~FV3DObject() {
    if (NULL != m_DebugUtils) {
        delete m_DebugUtils;
        m_DebugUtils = NULL;
    }
}

bool FV3DObject::initFreeView(unsigned char* bitmap, int inputWidth, int inputHeight, unsigned char* depthData,
        int depthWidth, int depthHeight, int outputWidth, int outputHeight, int orientation) {
    mOutputWidth = outputWidth;
    mOutputHeight = outputHeight;
    MRESULT apiResult;
    FV3DInitInfo initInfo;
    initInfo.outputWidth = (MUINT32) outputWidth;
    initInfo.outputHeight = (MUINT32) outputHeight;
    initInfo.inputWidth = (MUINT32) inputWidth;
    initInfo.inputHeight = (MUINT32) inputHeight;
    initInfo.depthWidth = depthWidth;
    initInfo.depthHeight = depthHeight;
    initInfo.orientation = orientation;

    ALOGI("<init> initInfo {inputWidth=%d,inputHeight=%d,outputWidth=%d, outputHeight=%d}", initInfo.inputWidth,
            initInfo.inputHeight, initInfo.outputWidth, initInfo.outputHeight);

    mFV3D = MTKFV3D::createInstance(DRV_FV3D_OBJ_SW);
    if (NULL == mFV3D) {
        ALOGI("<init> MTKFV3D::createInstance() fail!!!");
        return false;
    }

    apiResult = mFV3D->FV3DInit((void*) &initInfo, NULL);
    if (S_FV3D_OK != apiResult) {
        ALOGI("<init> FV3DInit() fail!!!");
        return false;
    }

    MUINT32 bufferSize = 0;
    mFV3D->FV3DFeatureCtrl(FV3D_FEATURE_GET_WORKBUF_SIZE, NULL, (void*) &bufferSize);
    ALOGI("<init> requires working buffer size %d", bufferSize);
    if (bufferSize != 0) {
        mWorkingBuffer = (MUINT8*) malloc(bufferSize);
        ALOGI("<init> malloc working buffer at %p", mWorkingBuffer);
        initInfo.workingBufferAddr = mWorkingBuffer;
        initInfo.workingBufferSize = bufferSize;
        apiResult = mFV3D->FV3DFeatureCtrl(FV3D_FEATURE_SET_WORKBUF_ADDR, (void*) &initInfo, NULL);
        ALOGI("<init> set working buffer with result: %d", apiResult);
    } else {
        return false;
    }

    ALOGI("begin RGBA->RGB bitmap:%p", bitmap);
    MUINT32 targetSize = inputWidth * inputHeight * 3;
    m_pImgBuf = (MUINT8 *) malloc(targetSize);
    for (int i = 0; i < inputWidth * inputHeight; i++) {
        m_pImgBuf[i * 3 + 0] = bitmap[i * 4 + 0];
        m_pImgBuf[i * 3 + 1] = bitmap[i * 4 + 1];
        m_pImgBuf[i * 3 + 2] = bitmap[i * 4 + 2];
    }
    ALOGI("end RGBA->RGB");

    return setInputImageInfo(m_pImgBuf, depthData);
}

bool FV3DObject::step(int x, int y, int outputTexId) {
    MRESULT apiResult;
    mProcInfo.x_coord = (MINT32) x;
    mProcInfo.y_coord = (MINT32) y;
    mProcInfo.outputTexID = (GLuint) outputTexId;

    ALOGI("<step> mProcInfo {x_coord=%d, y_coord=%d, outputTexID=%d}", mProcInfo.x_coord, mProcInfo.y_coord,
            mProcInfo.outputTexID);

    apiResult = mFV3D->FV3DFeatureCtrl(FV3D_FEATURE_SET_PROC_INFO, (void*) &mProcInfo, NULL);

    ALOGI("<step> FV3D_FEATURE_SET_PROC_INFO with result: %d", apiResult);
    if (S_FV3D_OK != apiResult) {
        return false;
    }

    apiResult = mFV3D->FV3DMain();

    if (NULL != m_DebugUtils) {
        MUINT8* dumpBuffer = new MUINT8[mOutputWidth * mOutputHeight * 4];
        char outputFileName[256];
        // glReadPixels(0, 0, mOutputWidth, mOutputHeight, GL_RGBA, GL_UNSIGNED_BYTE, dumpBuffer);
        sprintf(outputFileName, "%s_id%d_x%d_y%d.ppm", "outText", outputTexId, x, y);
        m_DebugUtils->dumpBufferToPPM(outputFileName, dumpBuffer, mOutputWidth, mOutputHeight, 6, 4);
        delete dumpBuffer;
        dumpBuffer = NULL;
    }
    return (S_FV3D_OK == apiResult ? true : false);
}

void FV3DObject::release() {
    if (mFV3D != NULL) {
        mFV3D->FV3DReset();
        mFV3D->destroyInstance(mFV3D);
        mFV3D = NULL;
    }
    if (mWorkingBuffer != NULL) {
        free(mWorkingBuffer);
        mWorkingBuffer = NULL;
    }
    if (NULL != m_pImgBuf) {
        free(m_pImgBuf);
        m_pImgBuf = NULL;
    }
    ALOGI("<release> successfully");
}

bool FV3DObject::setInputImageInfo(unsigned char*imageData, unsigned char* depthData) {
    MRESULT apiResult;
    FV3DImageInfo imageInfo;

    ALOGI("<setInputImageInfo> imageData:%p,depthData:%p", imageData, depthData);
    if (NULL == imageData || NULL == depthData) {
        return false;
    }
    imageInfo.inputBufAddr = (MUINT8*) imageData;
    imageInfo.depthBufAddr = (MUINT8*) depthData;
    apiResult = mFV3D->FV3DFeatureCtrl(FV3D_FEATURE_SET_INPUT_IMG, (void*) &imageInfo, NULL);
    ALOGI("<setInputTextureAndDepthBuffer> FV3D_FEATURE_SET_INPUT_IMG with result: %d", apiResult);

    return (S_FV3D_OK == apiResult ? true : false);
}
