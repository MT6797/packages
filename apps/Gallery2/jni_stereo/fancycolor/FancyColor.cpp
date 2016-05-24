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

#include "FancyColor.h"

#define LOG_TAG "Gallery2_FancyColor"

FancyColor::FancyColor() {
    memset(&mFancyEnvInfo, 0, sizeof(mFancyEnvInfo));
    memset(&mFancyWorkBufferInfo, 0, sizeof(mFancyWorkBufferInfo));
    memset(&mFancyProcInfo, 0, sizeof(mFancyProcInfo));
    memset(&mFancyResult, 0, sizeof(mFancyResult));
    // add for debug
    if (access(FANCYCOLOR_DUMP_PATH, 0) != -1) {
        m_DebugUtils = new DebugUtils(FANCYCOLOR_DUMP_PATH);
    }
}

FancyColor::~FancyColor() {
    if (NULL != m_DebugUtils) {
        delete m_DebugUtils;
        m_DebugUtils = NULL;
    }
}

bool FancyColor::init(unsigned char* color_img, unsigned char* alpha_mask, Rect rect, int img_width, int img_height,
        int center_x, int center_y) {
    ALOGI("<init> img_width:%d, img_height:%d,center_x:%d,center_y:%d", img_width, img_height, center_x, center_y);

    mFancyColor = mFancyColor->createInstance(DRV_FANCY_COLOR_OBJ_SW);

    mOriImgData = (unsigned char*) malloc(img_width * img_height * 4);
    memcpy(mOriImgData, color_img, img_width * img_height * 4);

    mAlphaMask = (unsigned char*) malloc(img_width * img_height);
    memcpy(mAlphaMask, alpha_mask, img_width * img_height);

    mMaskRect.left = rect.left;
    mMaskRect.top = rect.top;
    mMaskRect.right = rect.right;
    mMaskRect.bottom = rect.bottom;

    mImgWidth = img_width;
    mImgHeight = img_height;

    m_ImageData = (unsigned char*) malloc(img_width * img_height * 3);
    for (int i = 0; i < img_width * img_height; i++) {
        m_ImageData[i * 3 + 0] = color_img[i * 4 + 0];
        m_ImageData[i * 3 + 1] = color_img[i * 4 + 1];
        m_ImageData[i * 3 + 2] = color_img[i * 4 + 2];
    }

    mFancyEnvInfo.input_color_img_addr = m_ImageData;
    mFancyEnvInfo.input_color_img_height = img_height;
    mFancyEnvInfo.input_color_img_width = img_width;
    mFancyEnvInfo.input_color_img_stride = img_width * 3;
    mFancyEnvInfo.input_alpha_mask_addr = alpha_mask;
    mFancyEnvInfo.input_alpha_mask_height = img_height;
    mFancyEnvInfo.input_alpha_mask_width = img_width;
    mFancyEnvInfo.input_alpha_mask_stride = img_width;
    mFancyEnvInfo.center_x = center_x;
    mFancyEnvInfo.center_y = center_y;

    mFancyColor->Init((void*) &mFancyEnvInfo, NULL);

    MUINT32 buffer_size;
    mFancyColor->FeatureCtrl(FANCY_COLOR_FEATURE_GET_WORKBUF_SIZE, NULL, (void *) &buffer_size);
    mFancyWorkBufferInfo.ext_mem_start_addr = (unsigned char*) malloc(buffer_size);
    if (mFancyWorkBufferInfo.ext_mem_start_addr == 0) {
        ALOGI("[ERROR] Fail to allocate fancy color working buffer");
        return false;
    }
    mFancyWorkBufferInfo.ext_mem_size = buffer_size;

    ALOGI("allocate fancy color working buffer size %d, address:%p", buffer_size,
            mFancyWorkBufferInfo.ext_mem_start_addr);

    mFancyColor->FeatureCtrl(FANCY_COLOR_FEATURE_SET_WORKBUF_INFO, (void*) &mFancyWorkBufferInfo, NULL);

    return true;
}

bool FancyColor::doStrokeEffect() {
    ALOGI("doStrokeEffect");

    mFancyProcInfo.color_effect = FANCY_COLOR_EFFECT_STROKE;
    mFancyProcInfo.rect_in.left = mMaskRect.left;
    mFancyProcInfo.rect_in.top = mMaskRect.top;
    mFancyProcInfo.rect_in.right = mMaskRect.right;
    mFancyProcInfo.rect_in.bottom = mMaskRect.bottom;
    ALOGI("<doStrokeEffect> rect_in left-top-right-bottom: %d-%d-%d-%d", mMaskRect.left, mMaskRect.top,
          mMaskRect.right, mMaskRect.bottom);

    mFancyColor->FeatureCtrl(FANCY_COLOR_FEATURE_SET_PROC_INFO, (void*) &mFancyProcInfo, NULL);
    mFancyColor->Main();
    mFancyColor->FeatureCtrl(FANCY_COLOR_FEATURE_GET_RESULT, NULL, (void*) &mFancyResult);
    ALOGI("<doStrokeEffect> rect_out left-top-right-bottom: %d-%d-%d-%d", mFancyResult.rect_out.left,
            mFancyResult.rect_out.top, mFancyResult.rect_out.right, mFancyResult.rect_out.bottom);

    mStrokeOutputMask = (unsigned char*) malloc(mImgWidth * mImgHeight);
    memcpy(mStrokeOutputMask, mFancyResult.output_mask_addr, mImgWidth * mImgHeight);

    mStrokeRect.left = mFancyResult.rect_out.left;
    mStrokeRect.top = mFancyResult.rect_out.top;
    mStrokeRect.right = mFancyResult.rect_out.right;
    mStrokeRect.bottom = mFancyResult.rect_out.bottom;

    if (NULL != m_DebugUtils) {
        m_DebugUtils->dumpBufferToPPM("StrokeIutputImage.ppm", m_ImageData, mImgWidth, mImgHeight, 6, 3);
        m_DebugUtils->dumpBufferToPPM("StrokeOutputMask.ppm", mStrokeOutputMask, mImgWidth, mImgHeight, 5, 3);
    }

    return true;
}

bool FancyColor::doRadialBlurEffect() {
    ALOGI("<doRadialBlurEffect>");
    mFancyProcInfo.color_effect = FANCY_COLOR_EFFECT_RADIAL_BLUR;
    mFancyProcInfo.rect_in.left = mMaskRect.left;
    mFancyProcInfo.rect_in.top = mMaskRect.top;
    mFancyProcInfo.rect_in.right = mMaskRect.right;
    mFancyProcInfo.rect_in.bottom = mMaskRect.bottom;
    ALOGI("<doRadialBlurEffect> rect_in left-top-right-bottom: %d-%d-%d-%d", mMaskRect.left, mMaskRect.top,
          mMaskRect.right, mMaskRect.bottom);

    mFancyColor->FeatureCtrl(FANCY_COLOR_FEATURE_SET_PROC_INFO, (void*) &mFancyProcInfo, NULL);
    mFancyColor->Main();
    mFancyColor->FeatureCtrl(FANCY_COLOR_FEATURE_GET_RESULT, NULL, (void*) &mFancyResult);
    ALOGI("<doRadialBlurEffect> rect_out left-top-right-bottom: %d-%d-%d-%d", mFancyResult.rect_out.left,
            mFancyResult.rect_out.top, mFancyResult.rect_out.right, mFancyResult.rect_out.bottom);

    mRadialOutputMask = (unsigned char*) malloc(mImgWidth * mImgHeight);
    memcpy(mRadialOutputMask, mFancyResult.output_mask_addr, mImgWidth * mImgHeight);

    mRadialOutputImg = (unsigned char*) malloc(mImgWidth * mImgHeight * 3);
    memcpy(mRadialOutputImg, mFancyResult.output_img_addr, mImgWidth * mImgHeight * 3);

    mRadialMask.left = mFancyResult.rect_out.left;
    mRadialMask.top = mFancyResult.rect_out.top;
    mRadialMask.right = mFancyResult.rect_out.right;
    mRadialMask.bottom = mFancyResult.rect_out.bottom;

    if (NULL != m_DebugUtils) {
        m_DebugUtils->dumpBufferToPPM("RadialIutputImg.ppm", m_ImageData, mImgWidth, mImgHeight, 6, 3);
        m_DebugUtils->dumpBufferToPPM("RadialOutputImg.ppm", mRadialOutputImg, mImgWidth, mImgHeight, 6, 3);
        m_DebugUtils->dumpBufferToPPM("RadialOutputMask.ppm", mRadialOutputMask, mImgWidth, mImgHeight, 5, 3);
    }

    return true;
}

void FancyColor::getStrokeImg(char* bitmap, int width, int height) {
    ALOGI("getStrokeImg,width:%d, height:%d", width, height);

    if (width != mImgWidth || height != mImgHeight) {
        ALOGI("<getStrokeImg> error size, width:%d, height:%d", width, height);
        return;
    }
    memcpy(bitmap, mOriImgData, width * height * 4);

    ALOGI("<getStrokeImg> left:%d,top:%d,right:%d,bottom:%d", mStrokeRect.left, mStrokeRect.top,
            mStrokeRect.right, mStrokeRect.bottom);

    for (int i = mStrokeRect.top; i < mStrokeRect.bottom; i++) {
        for (int j = mStrokeRect.left; j < mStrokeRect.right; j++) {
            int inputMaskData = mAlphaMask[i * width + j];
            int outputMaskData = mStrokeOutputMask[i * width + j];

            if (inputMaskData == 0 && outputMaskData != 0) {
                memset(&bitmap[(i * width + j) * 4], STROKE_DATA, 3);
            } else if (inputMaskData != 0 && inputMaskData != 255) {
                bitmap[(i * width + j) * 4] = bitmap[(i * width + j) * 4] * (inputMaskData / 255.0)
                        + STROKE_DATA * (1 - inputMaskData / 255.0);
                bitmap[(i * width + j) * 4 + 1] = bitmap[(i * width + j) * 4 + 1] * (inputMaskData / 255.0)
                        + STROKE_DATA * (1 - inputMaskData / 255.0);
                bitmap[(i * width + j) * 4 + 2] = bitmap[(i * width + j) * 4 + 2] * (inputMaskData / 255.0)
                        + STROKE_DATA * (1 - inputMaskData / 255.0);
            }
        }
    }
}

void FancyColor::getRadialBlurImg(char* bitmap, int width, int height) {
    ALOGI("<getRadialBlurImg> width:%d,height:%d", width, height);

    if (width != mImgWidth || height != mImgHeight) {
        ALOGI("<getRadialBlurImg> error size, width:%d, height:%d", width, height);
        return;
    }

    for (int i = 0; i < height; i++) {
        for (int j = 0; j < width; j++) {
            memcpy(&bitmap[(i * width + j) * 4], &mRadialOutputImg[(i * width + j) * 3], 3);
        }
    }

    for (int i = mRadialMask.top; i < mRadialMask.bottom; i++) {
        for (int j = mRadialMask.left; j < mRadialMask.right; j++) {
            if (mRadialOutputMask[i * width + j] != 0) {
                bitmap[(i * width + j) * 4] = mOriImgData[(i * width + j) * 4]
                        * (mRadialOutputMask[i * width + j] / 255.0)
                        + bitmap[(i * width + j) * 4] * (1 - mRadialOutputMask[i * width + j] / 255.0);
                bitmap[(i * width + j) * 4 + 1] = mOriImgData[(i * width + j) * 4 + 1]
                        * (mRadialOutputMask[i * width + j] / 255.0)
                        + bitmap[(i * width + j) * 4 + 1] * (1 - mRadialOutputMask[i * width + j] / 255.0);
                bitmap[(i * width + j) * 4 + 2] = mOriImgData[(i * width + j) * 4 + 2]
                        * (mRadialOutputMask[i * width + j] / 255.0)
                        + bitmap[(i * width + j) * 4 + 2] * (1 - mRadialOutputMask[i * width + j] / 255.0);
            }
        }
    }
}

void FancyColor::release() {
    ALOGI("<release>");

    if (mFancyColor != NULL) {
        mFancyColor->Reset();
        mFancyColor->destroyInstance(mFancyColor);
        mFancyColor = NULL;
    }
    freeMem(mOriImgData);
    freeMem(m_ImageData);
    freeMem(mAlphaMask);
    freeMem(mStrokeOutputMask);
    freeMem(mRadialOutputMask);
    freeMem(mRadialOutputImg);
    freeMem(mFancyWorkBufferInfo.ext_mem_start_addr);
}

void FancyColor::freeMem(unsigned char* addr) {
    if (addr != NULL) {
        free(addr);
        addr = NULL;
    }
}
