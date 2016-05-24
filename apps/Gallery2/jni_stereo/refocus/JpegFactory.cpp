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
#include "JpegFactory.h"

#define LOG_TAG "Gallery2_JpegFactory"

JpegFactory::JpegFactory() {
}

JpegFactory::~JpegFactory() {
}

bool JpegFactory::jpgDecode(char const *fileName, ImageInfo *pImageInfo, int sampleSize) {
    FILE *fp = NULL;
    unsigned char *file_buffer = NULL;
    unsigned char *align128_file_buffer = NULL;
    uint32_t ret = 0;
    uint32_t file_size = 0;

    ALOGI("<jpgDecode>begin");
    // open a image file
    fp = fopen(fileName, "rb");
    if (fp == NULL) {
        ALOGI("ERROR: Open file %s failed.", fileName);
        return false;
    }
    // get file size
    fseek(fp, SEEK_SET, SEEK_END);
    file_size = ftell(fp);
    if (file_size == 0) {
        ALOGI("ERROR: [readImageFile]file size is 0");
        fclose(fp);
        return false;
    }
    ALOGI("<jpgDecode>open file %s success,file size:%d", fileName, file_size);

    // allocate buffer for the file
    // should free this memory when not use it !!!
    file_buffer = (unsigned char *) malloc(ALIGN128(file_size) + 512 + 127);
    align128_file_buffer = (unsigned char *)((((size_t)file_buffer + 127) >> 7) << 7);
    ALOGI("<jpgDecode>align128_file_buffer :%p", (size_t)align128_file_buffer);
    if (align128_file_buffer == NULL) {
        ALOGI("ERROR: Can not allocate memory");
        fclose(fp);
        return false;
    }

    // read image file
    fseek(fp, SEEK_SET, SEEK_SET);
    ret = fread(align128_file_buffer, 1, file_size, fp);
    if (ret != file_size) {
        ALOGI("ERROR: File read error ret[%d]", ret);
        free(file_buffer);
        fclose(fp);
        return false;
    }

    mStereoUtils.starMeasureTime();
    if (!jpgToYV12(align128_file_buffer, file_size, pImageInfo, sampleSize)) {
        ALOGI("ERROR: decode failed!!");
    }
    mStereoUtils.endMeasureTime("<jpgDecode> jpg to yv12");
    free(file_buffer);
    fclose(fp);
    return true;
}

bool JpegFactory::jpgToYV12(uint8_t* srcBuffer, uint32_t srcSize, ImageInfo *pImageInfo, int sampleSize) {
    MHAL_JPEG_DEC_INFO_OUT outInfo;
    MHAL_JPEG_DEC_START_IN inParams;
    MHAL_JPEG_DEC_SRC_IN    srcInfo;

    ALOGI("<jpgToYV12>onDecode start %d ", srcSize);

    // 2 step4: init srcInfo
    srcInfo.jpgDecHandle = NULL;
    srcInfo.srcBuffer = srcBuffer;
    srcInfo.srcLength = srcSize;
    // 2 step5 jpeg dec parser
    ALOGI("<jpgToYV12>onDecode MHAL_IOCTL_JPEG_DEC_PARSER");
    if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_JPEG_DEC_PARSER, (void *)&srcInfo, sizeof(srcInfo),
            NULL, 0, NULL)) {
        ALOGI("ERROR: parser file error!!!");
        return false;
    }
    // 2 step6 set jpgDecHandle value
    outInfo.jpgDecHandle = srcInfo.jpgDecHandle;
    ALOGI("<jpgToYV12>outInfo.jpgDecHandle --> %p", outInfo.jpgDecHandle);
    // 2 step7: get jpeg info
    if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_JPEG_DEC_GET_INFO, NULL, 0, (void *)&outInfo,
            sizeof(outInfo), NULL)) {
        ALOGI("ERROR:get info error!!!");
        return false;
    }
    ALOGI("<jpgToYV12>outInfo.srcWidth:%d,outInfo.srcHeight:%d", outInfo.srcWidth, outInfo.srcHeight);

    pImageInfo->width = ALIGN16(outInfo.srcWidth / sampleSize);
    pImageInfo->height = ALIGN16(outInfo.srcHeight / sampleSize);
    pImageInfo->bufferSize = pImageInfo->width * pImageInfo->height * 3 / 2;
    pImageInfo->destBuffer = (uint8_t*)malloc(pImageInfo->bufferSize);

    // 2 step8: set inParams
    inParams.dstFormat = JPEG_OUT_FORMAT_I420;
    inParams.srcBuffer = srcBuffer;
    inParams.srcBufSize = ALIGN128(srcSize) + 512;
    inParams.srcLength = srcSize;
    inParams.dstPhysAddr = NULL;
    inParams.doDithering = 0;
    inParams.doRangeDecode = 0;
    inParams.doPostProcessing = 0;
    inParams.postProcessingParam = NULL;
    inParams.PreferQualityOverSpeed = 0;
    inParams.jpgDecHandle = srcInfo.jpgDecHandle;
    inParams.dstWidth = pImageInfo->width;
    inParams.dstHeight = pImageInfo->height;
    inParams.dstVirAddr = pImageInfo->destBuffer;

    ALOGI("<jpgToYV12>dstFormat:%d,dstWidth:%d,dstHeight:%d,srcLength:%d", inParams.dstFormat,
            inParams.dstWidth, inParams.dstHeight, inParams.srcLength);
    // 2 step9: start decode
    if (MHAL_NO_ERROR != mHalJpeg(MHAL_IOCTL_JPEG_DEC_START, (void *)&inParams, sizeof(inParams),
            NULL, 0, NULL)) {
        ALOGI("ERROR: JPEG HW not support this image");
        return false;
    }
    return true;
}

bool JpegFactory::yv12ToJpg(unsigned char *srcBuffer, int srcSize, int srcWidth, int srcHeight,
        unsigned char *dstBuffer, int dstSize, unsigned int* u4EncSize) {
    bool ret = false;
    int fIsAddSOI = true;  // if set true, not need add exif
    int quality = 90;
    size_t yuvAddr[3], yuvSize[3];

    yuvSize[0] = getBufSize(srcWidth, srcHeight);
    yuvSize[1] = getBufSize(srcWidth/2, srcHeight/2);
    yuvSize[2] = getBufSize(srcWidth/2, srcHeight/2);
    //
    yuvAddr[0] = (size_t)srcBuffer;
    yuvAddr[1] = yuvAddr[0]+yuvSize[0];
    yuvAddr[2] = yuvAddr[1]+yuvSize[1];

    ALOGI("<yv12ToJpg>begin");
    ALOGI("<yv12ToJpg>srcBuffer:%p,dstBuffer:%p,srcWidth:%d,srcHeight:%d", (size_t)srcBuffer, (size_t)dstBuffer,
            srcWidth, srcHeight);
    ALOGI("yuvSize[0]=%p, yuvSize[1]=%p, yuvSize[2]=%p", yuvSize[0], yuvSize[1], yuvSize[2]);
    ALOGI("yuvAddr[0]=%p, yuvAddr[1]=%p, yuvAddr[2]=%p", yuvAddr[0], yuvAddr[1], yuvAddr[2]);

    JpgEncHal* pJpgEncoder = new JpgEncHal();
    // (1). Lock
    pJpgEncoder->unlock();
    if (!pJpgEncoder->lock()) {
        ALOGI("ERROR:can't lock jpeg resource!!!");
        delete pJpgEncoder;
        return false;
    }
    // (2). size, format, addr
    pJpgEncoder->setEncSize(srcWidth, srcHeight, JpgEncHal::kENC_YV12_Format);
    pJpgEncoder->setSrcAddr((void*)ALIGN16(yuvAddr[0]), (void*)ALIGN16(yuvAddr[1]), (void*)ALIGN16(yuvAddr[2]));
    pJpgEncoder->setSrcBufSize(srcWidth, yuvSize[0], yuvSize[1], yuvSize[2]);
    pJpgEncoder->setQuality(quality);
    pJpgEncoder->setDstAddr((void *)dstBuffer);
    pJpgEncoder->setDstSize(dstSize);
    pJpgEncoder->enableSOI((fIsAddSOI > 0) ? 1 : 0);
    // (7). ION mode
    ALOGI("<yv12ToJpg>start");
    if (pJpgEncoder->start(u4EncSize)) {
        ALOGI("<yv12ToJpg>Jpeg encode done, size = %d", *u4EncSize);
        ret = true;
    } else {
        ALOGI("ERROR:encode fail");
    }
    pJpgEncoder->unlock();
    delete pJpgEncoder;
    ALOGI("<yv12ToJpg>end ret:%d", ret);
    return ret;
}

int JpegFactory::getBufSize(int width, int height) {
    int bufSize = 0;
    int w;
    w = ALIGN16(width);
    bufSize = w*height;
    ALOGI("<getBufSize>W(%d)xH(%d),BS(%d)", w, height, bufSize);
    return bufSize;
}

