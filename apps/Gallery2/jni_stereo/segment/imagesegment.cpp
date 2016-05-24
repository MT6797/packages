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
#include "imagesegment.h"

#define LOG_TAG "Gallery2_ImageSegment"
namespace android {

ImageSegment::ImageSegment() {
    memset(&gEnvInfo, 0, sizeof(gEnvInfo));
    memset(&gProcInfo, 0, sizeof(gProcInfo));
    memset(&gWorkBufferInfo, 0, sizeof(gWorkBufferInfo));
    memset(&gResult, 0, sizeof(gResult));
    memset(&gSaveInfo, 0, sizeof(gSaveInfo));

    memset(user_scribbles, 0, sizeof(user_scribbles));
    memset(alpha_mask, 0, sizeof(alpha_mask));

    memset(m_objPoint, 0, sizeof(m_objPoint));
    memset(m_objRect, 0, sizeof(m_objRect));

    memset(&m_newMaskPoint, 0, sizeof(m_newMaskPoint));
    memset(&m_newMaskRect, 0, sizeof(m_newMaskRect));
    memset(&m_newMask, 0, sizeof(m_newMask));

    // add for debug
    if (access(SEGMENT_DUMP_PATH, 0) != -1) {
        m_DebugUtils = new DebugUtils(SEGMENT_DUMP_PATH);
    }
}

ImageSegment::~ImageSegment() {
    ALOGI("<~ImageSegment>");
    release();
}

bool ImageSegment::init(unsigned char* bitmap, int imgWidth, int imgHeight, unsigned char* depthImagBuf, int depthWidth,
        int depthHeight, unsigned char* occImgBuf, int occImgWidth, int occImgHeight, int scribbleWidth,
        int scribbleHeight, int faceNum, Rect* faceRect, int* pFaceRip, int orientation) {
    ALOGI("[initSegment] bitmap:%p,imgWidth:%d,imgHeight:%d,depthImagBuf:%p,depthWidth:%d,depthHeight:%d,occImgBuf:%p,"
            "occImgWidth:%d,occImgHeight:%d", bitmap, imgWidth, imgHeight, depthImagBuf, depthWidth, depthHeight,
            occImgBuf, occImgWidth, occImgHeight);
    for (int i = 0; i < faceNum; i++) {
        ALOGI("[initSegment]%dfaceRect:left:%d,top:%d,right:%d,bottom:%d,facerip:%d", i, faceRect[i].left,
                faceRect[i].top, faceRect[i].right, faceRect[i].bottom, pFaceRip[i]);
    }

    m_ImageWidth = imgWidth;
    m_ImageHeight = imgHeight;
    int target_size = imgWidth * imgHeight * 3;
    m_ImageData = (MUINT8*) malloc(target_size);
    for (int i = 0; i < imgWidth * imgHeight; i++) {
        m_ImageData[i * 3 + 0] = bitmap[i * 4 + 0];
        m_ImageData[i * 3 + 1] = bitmap[i * 4 + 1];
        m_ImageData[i * 3 + 2] = bitmap[i * 4 + 2];
    }

    m_pDepthBuf = (MUINT8*) malloc(depthWidth * depthHeight);
    memcpy(m_pDepthBuf, depthImagBuf, depthWidth * depthHeight);
    m_pOccBuf = (MUINT8*) malloc(occImgWidth * occImgHeight);
    memcpy(m_pOccBuf, occImgBuf, occImgWidth * occImgHeight);

    // add for debug
    if (m_DebugUtils != NULL) {
        // m_DebugUtils->_dump_rect_ppm(imgWidth, imgHeight, "oriimage.ppm", m_ImageData, 6, 3, faceRect, faceNum);
        m_DebugUtils->dumpBufferToPPM("depth.ppm", m_pDepthBuf, depthWidth, depthHeight, 5, 3);
        m_DebugUtils->dumpBufferToPPM("occ.ppm", m_pOccBuf, occImgWidth, occImgHeight, 5, 3);

        m_DebugUtils->dumpBufferToFile("oriimage.raw", m_ImageData, imgWidth * imgHeight * 3);
        m_DebugUtils->dumpBufferToFile("depth.raw", m_pDepthBuf, depthWidth * depthHeight);
        m_DebugUtils->dumpBufferToFile("occ.raw", m_pOccBuf, occImgWidth * occImgHeight);
    }

    gEnvInfo.debug_level = 7;
    gEnvInfo.img_orientation = orientation;

    gEnvInfo.input_color_img_width = imgWidth;
    gEnvInfo.input_color_img_height = imgHeight;
    gEnvInfo.input_color_img_stride = imgWidth * 3;
    gEnvInfo.input_color_img_addr = m_ImageData;

    gEnvInfo.input_depth_img_width = depthWidth;
    gEnvInfo.input_depth_img_height = depthHeight;
    gEnvInfo.input_depth_img_stride = depthWidth;
    gEnvInfo.input_depth_img_addr = m_pDepthBuf;

    gEnvInfo.input_occ_img_width = occImgWidth;
    gEnvInfo.input_occ_img_height = occImgHeight;
    gEnvInfo.input_occ_img_stride = occImgWidth;
    gEnvInfo.input_occ_img_addr = m_pOccBuf;

    for (int i = 0; i < MAX_UNDO_NUM; i++) {
        user_scribbles[i].alloc(m_ImageWidth, m_ImageHeight, 1);
        alpha_mask[i].alloc(m_ImageWidth, m_ImageHeight, 1);
    }
    gEnvInfo.input_scribble_width = user_scribbles[0].width;
    gEnvInfo.input_scribble_height = user_scribbles[0].height;
    gEnvInfo.input_scribble_stride = user_scribbles[0].stride;

    gEnvInfo.output_mask_width = alpha_mask[0].width;
    gEnvInfo.output_mask_height = alpha_mask[0].height;
    gEnvInfo.output_mask_stride = alpha_mask[0].stride;
    if (faceNum > 0) {
        gEnvInfo.face_num = faceNum;
        gEnvInfo.face_pos = faceRect;
        gEnvInfo.face_rip = pFaceRip;
    } else {
        gEnvInfo.face_num = 0;
    }
    gEnvInfo.mem_alignment = ALIGNMENT_BYTE;

    gEnvInfo.tuning_para.alpha_mode = 4;
    gEnvInfo.tuning_para.seg_ratio = 0.25f;

    mImageSegment = mImageSegment->createInstance(DRV_IMAGE_SEGMENT_OBJ_SW);
    mImageSegment->Init((void*) &gEnvInfo, NULL);

    MUINT32 buffer_size;
    mImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_GET_WORKBUF_SIZE, NULL, (void*) &buffer_size);
    gWorkBufferInfo.ext_mem_start_addr = (unsigned char*) malloc(buffer_size);
    if (gWorkBufferInfo.ext_mem_start_addr == NULL) {
        ALOGI("<initSegment> Fail to allocate working buffer\n");
        return false;
    }
    ALOGI("[initSegment] mem_addr:%p,buffer_size:%d", gWorkBufferInfo.ext_mem_start_addr, buffer_size);
    gWorkBufferInfo.ext_mem_size = buffer_size;
    mImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_SET_WORKBUF_INFO, (void*) &gWorkBufferInfo, NULL);
    return true;
}

bool ImageSegment::doSegment(int scenario, int mode, unsigned char* scribbleBuf, Rect roiRect) {
    ALOGI("[doSegment] scenario:%d,mode:%d,scribbleBuf:%p,roiRect.left:%d,top:%d,right:%d,bottom:%d", scenario,
            mode, scribbleBuf, roiRect.left, roiRect.top, roiRect.right, roiRect.bottom);

    if (NULL == mImageSegment) {
        ALOGI("[doSegment]mImageSegment is null, fail!!!");
        return false;
    }

    switch (scenario) {
    case SCENARIO_AUTO:
        gProcInfo.scenario = SEGMENT_SCENARIO_AUTO;
        break;
    case SCENARIO_SELECTION:
        gProcInfo.scenario = SEGMENT_SCENARIO_SELECTION;
        break;
    case SCENARIO_SCRIBBLE_FG:
        gProcInfo.scenario = SEGMENT_SCENARIO_SCRIBBLE_FG;
        break;
    case SCENARIO_SCRIBBLE_BG:
        gProcInfo.scenario = SEGMENT_SCENARIO_SCRIBBLE_BG;
        break;
    default:
        break;
    }

    memset(user_scribbles[m_EditCurrIdx].ptr, 128, user_scribbles[m_EditCurrIdx].size);
    if ((SCENARIO_SCRIBBLE_FG == scenario || SCENARIO_SCRIBBLE_BG == scenario) && scribbleBuf != NULL) {
        unsigned char *ptr = user_scribbles[m_EditCurrIdx].ptr;
        for (int i = roiRect.top; i < roiRect.bottom; i++) {
            for (int j = roiRect.left; j < roiRect.right; j++) {
                ptr[i * m_ImageWidth + j] = scribbleBuf[(i * m_ImageWidth + j) * 4];
            }
        }
    }

    if (mode == 0) {
        gProcInfo.mode = SEGMENT_OBJECT;
    } else {
        gProcInfo.mode = SEGMENT_FOREGROUND;
    }

    if (NULL != m_DebugUtils) {
        m_DebugUtils->dumpBufferToPPM("scribble.ppm", user_scribbles[m_EditCurrIdx].ptr, m_ImageWidth,
                m_ImageHeight, 5, 3);
    }

    gProcInfo.undo = m_isUndo ? 1 : 0;
    m_isUndo = false;

    gProcInfo.input_user_roi.left = roiRect.left;
    gProcInfo.input_user_roi.top = roiRect.top;
    gProcInfo.input_user_roi.right = roiRect.right;
    gProcInfo.input_user_roi.bottom = roiRect.bottom;

    gProcInfo.input_scribble_addr = user_scribbles[m_EditCurrIdx].ptr;
    gProcInfo.prev_output_mask_addr = alpha_mask[m_EditCurrIdx].ptr;

    ALOGI("<doSegment> algorithm start");
    mImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_SET_PROC_INFO, (void*) &gProcInfo, NULL);
    MRESULT result = mImageSegment->Main();
    if (S_IMAGE_SEGMENT_OK != result) {
        if (scenario == SCENARIO_SCRIBBLE_BG && E_IMAGE_SEGMENT_NULL_OBJECT == result) {
            ALOGI("<doSegment> scribble null object!!");
        } else {
            ALOGI("[doSegment]ERROR: main fail,result:%d", result);
            return false;
        }
    }
    mImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_GET_RESULT, NULL, (void*) &gResult);
    ALOGI("<doSegment> algorithm end");

    m_EditCurrIdx = (m_EditCurrIdx + 1) % MAX_UNDO_NUM;
    memcpy(alpha_mask[m_EditCurrIdx].ptr, gResult.output_mask_addr, alpha_mask[m_EditCurrIdx].size);

    m_objPoint[m_EditCurrIdx].x = gResult.center.x;
    m_objPoint[m_EditCurrIdx].y = gResult.center.y;

    m_objRect[m_EditCurrIdx].left = max(gResult.bbox.left, 0);
    m_objRect[m_EditCurrIdx].top = max(gResult.bbox.top, 0);
    m_objRect[m_EditCurrIdx].right = min(gResult.bbox.right, m_ImageWidth);
    m_objRect[m_EditCurrIdx].bottom = min(gResult.bbox.bottom, m_ImageHeight);

    ALOGI("[doSegment] rect.left:%d,top:%d,right:%d,bottom:%d", gResult.bbox.left, gResult.bbox.top, gResult.bbox.right,
            gResult.bbox.bottom);
    ALOGI("[doSegment] center.x:%d,center.y:%d", gResult.center.x, gResult.center.y);

    if (NULL != m_DebugUtils) {
        m_DebugUtils->dumpBufferToPPM("mask.ppm", alpha_mask[m_EditCurrIdx].ptr, m_ImageWidth, m_ImageHeight, 5, 3);
    }
    return true;
}

bool ImageSegment::undoSegment() {
    ALOGI("<undoSegment>");

    m_EditCurrIdx = (m_EditCurrIdx - 1 + MAX_UNDO_NUM) % MAX_UNDO_NUM;
    m_isUndo = true;

    return true;
}

bool ImageSegment::initSegmentMask(unsigned char* mask, Rect rect, Point point) {
    ALOGI("<initSegmentMask> rect.left:%d,top:%d,right:%d,bottom:%d,point.x:%d,y:%d", rect.left, rect.top, rect.right,
            rect.bottom, point.x, point.y);

    memcpy(alpha_mask[m_EditCurrIdx].ptr, mask, alpha_mask[m_EditCurrIdx].size);
    m_objPoint[m_EditCurrIdx].x = point.x;
    m_objPoint[m_EditCurrIdx].y = point.y;

    m_objRect[m_EditCurrIdx].left = rect.left;
    m_objRect[m_EditCurrIdx].top = rect.top;
    m_objRect[m_EditCurrIdx].right = rect.right;
    m_objRect[m_EditCurrIdx].bottom = rect.bottom;

    if (NULL != m_DebugUtils) {
        m_DebugUtils->dumpBufferToPPM("mask.ppm", alpha_mask[m_EditCurrIdx].ptr, m_ImageWidth, m_ImageHeight, 5, 3);
    }
    return true;
}

unsigned char* ImageSegment::getSegmentMask(bool isNew) {
    if (isNew) {
        return m_newMask.ptr;
    } else {
        return alpha_mask[m_EditCurrIdx].ptr;
    }
}

Point ImageSegment::getSegmentPoint(bool isNew) {
    ALOGI("<getSegmentPoint> isNew:%d", isNew);
    if (isNew) {
        return m_newMaskPoint;
    } else {
        return m_objPoint[m_EditCurrIdx];
    }
}

Rect ImageSegment::getSegmentRect(bool isNew) {
    if (isNew) {
        return m_newMaskRect;
    } else {
        return m_objRect[m_EditCurrIdx];
    }
}

bool ImageSegment::getSegmentImg(char* oriImg, int oriWidth, int oriHeight, char* newImg, int width,
        int height, bool isNew) {
    Rect rect = getSegmentRect(isNew);
    ALOGI("<getSegmentBitmap> width:%d,height:%d,rect.left:%d,top:%d,right:%d,bottom:%d", width, height, rect.left,
            rect.top, rect.right, rect.bottom);

    if (width != rect.right - rect.left || height != rect.bottom - rect.top) {
        ALOGI("ERROR: error size, fail!!!!");
        return false;
    }

    for (int i = 0; i < height; i++) {
        memcpy((newImg + i * width * 4), (oriImg + ((i + rect.top) * oriWidth + rect.left) * 4), width * 4);
    }

    unsigned char* maskPtr = getSegmentMask(isNew);
    int mask = 0;
    for (int i = 0; i < height; i++) {
        for (int j = 0; j < width; j++) {
            mask = maskPtr[(i + rect.top) * oriWidth + (j + rect.left)];
            newImg[(i * width + j) * 4 + 0] = newImg[(i * width + j) * 4 + 0] * mask / ALPHA;
            newImg[(i * width + j) * 4 + 1] = newImg[(i * width + j) * 4 + 1] * mask / ALPHA;
            newImg[(i * width + j) * 4 + 2] = newImg[(i * width + j) * 4 + 2] * mask / ALPHA;
            newImg[(i * width + j) * 4 + 3] = mask;
        }
    }
    return true;
}

bool ImageSegment::fillMaskToImg(char* img, int width, int height) {
    ALOGI("<fillMaskToImg> width:%d,height:%d", width, height);

    if (width != m_ImageWidth || height != m_ImageHeight) {
        ALOGI("ERROR: error size, fail!!!!");
        return false;
    }
    Rect rect = getSegmentRect(false);
    ALOGI("<fillMaskToImg> left:%d,top:%d,right:%d,bottom:%d", rect.left, rect.top, rect.right, rect.bottom);
    for (int i = rect.top; i <= rect.bottom; i++) {
        for (int j = rect.left; j <= rect.right; j++) {
            img[i * width + j] = (float) (ALPHA - alpha_mask[m_EditCurrIdx].ptr[i * width + j]) / ALPHA
                    * COVER;
        }
    }
    return true;
}

bool ImageSegment::setNewBitmap(char* img, int width, int height) {
    ALOGI("<setNewBitmap> bitmap:%p,width:%d,height:%d", img, width, height);
    if (width <= 0 || height <= 0) {
        ALOGI("ERROR: illegal image width and height!!!");
        return false;
    }

    if (NULL == mImageSegment) {
        ALOGI("ERROR: NULL mImageSegment object!!!!");
        return false;
    }

    memset(&gSaveInfo, 0, sizeof(gSaveInfo));
    gSaveInfo.save_width = width;
    gSaveInfo.save_height = height;
    gSaveInfo.save_color_img_stride = width * 3;
    gSaveInfo.save_mask_stride = width;
    mImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_SET_SAVE_INFO, &gSaveInfo, NULL);

    MUINT32 buffer_size;
    mImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_GET_SAVE_WORKBUF_SIZE, NULL, (void*)&buffer_size);
    unsigned char* pSaveBuffer = (unsigned char*)malloc(buffer_size);
    if (NULL == pSaveBuffer) {
        ALOGI("ERROR: allocate pSaveBuffer memory fail!!!");
        return false;
    }
    MUINT8* pImageBuffer = (MUINT8*) malloc(width * height * 3);
    if (NULL == pImageBuffer) {
        ALOGI("ERROR: allocate pImageData memory fail!!!");
        free(pSaveBuffer);
        pSaveBuffer = NULL;
        return false;
    }
    for (int i = 0; i < width * height; i++) {
        pImageBuffer[i * 3 + 0] = img[i * 4 + 0];
        pImageBuffer[i * 3 + 1] = img[i * 4 + 1];
        pImageBuffer[i * 3 + 2] = img[i * 4 + 2];
    }
    m_newMask.release();
    m_newMask.alloc(width, height, 1);
    memset(&gProcInfo, 0, sizeof(gProcInfo));
    gProcInfo.scenario = SEGMENT_SCENARIO_SAVE;
    gProcInfo.input_color_img_addr = pImageBuffer;
    gProcInfo.prev_output_mask_addr = alpha_mask[m_EditCurrIdx].ptr;
    gProcInfo.working_buffer_addr = pSaveBuffer;
    mImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_SET_PROC_INFO, (void*)&gProcInfo, NULL);
    mImageSegment->Main();
    mImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_GET_RESULT, NULL, (void*)&gResult);
    memcpy(m_newMask.ptr, gResult.output_mask_addr, m_newMask.size);
    m_newMaskPoint.x = gResult.center.x;
    m_newMaskPoint.y = gResult.center.y;

    m_newMaskRect.left = gResult.bbox.left;
    m_newMaskRect.top = gResult.bbox.top;
    m_newMaskRect.right = gResult.bbox.right;
    m_newMaskRect.bottom = gResult.bbox.bottom;
    free(pSaveBuffer);
    free(pImageBuffer);
    pSaveBuffer = NULL;
    pImageBuffer = NULL;

    if (NULL != m_DebugUtils) {
        m_DebugUtils->dumpBufferToPPM("newMask.ppm", m_newMask.ptr, width, height, 5, 3);
    }
    return true;
}

bool ImageSegment::setNewBitmap(char* img, int imgWidth, int imgHeight, unsigned char* mask,
        int maskWidth, int maskHeight) {
    ALOGI("<setNewBitmap> img:%p,imgWidth:%d,imgHeight:%d,mask:%p,maskWidth:%d,maskHeight:%d",
            img, imgWidth, imgHeight, mask, maskWidth, maskHeight);
    if (mImageSegment != NULL) {
        assert(false);
        return false;
    }
    if (NULL != m_DebugUtils) {
        m_DebugUtils->dumpBufferToPPM("oldMask.ppm", mask, maskWidth, maskHeight, 5, 3);
    }
    mImageSegment = mImageSegment->createInstance(DRV_IMAGE_SEGMENT_OBJ_SW);
    gEnvInfo.mem_alignment = ALIGNMENT_BYTE;
    gEnvInfo.input_color_img_width = maskWidth;
    gEnvInfo.input_color_img_height = maskHeight;
    gEnvInfo.output_mask_width = maskWidth;
    gEnvInfo.output_mask_height = maskHeight;
    gEnvInfo.output_mask_stride = (maskWidth + ALIGNMENT_BYTE - 1) / ALIGNMENT_BYTE * ALIGNMENT_BYTE;

    gEnvInfo.save_width = imgWidth;
    gEnvInfo.save_height = imgHeight;
    gEnvInfo.save_color_img_stride = imgWidth * 3;
    gEnvInfo.save_mask_stride = (imgWidth + ALIGNMENT_BYTE - 1) / ALIGNMENT_BYTE * ALIGNMENT_BYTE;

    gEnvInfo.tuning_para.alpha_mode = 4;
    gEnvInfo.tuning_para.seg_ratio = 0.25f;

    mImageSegment->Init((void*)&gEnvInfo, NULL);

    // get buffer size
    MUINT32 buffer_size;
    mImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_GET_WORKBUF_SIZE, NULL, (void*)&buffer_size);
    if (gWorkBufferInfo.ext_mem_start_addr != NULL) {
        free(gWorkBufferInfo.ext_mem_start_addr);
        gWorkBufferInfo.ext_mem_start_addr = NULL;
    }
    gWorkBufferInfo.ext_mem_start_addr = (unsigned char*)malloc(buffer_size);
    if (gWorkBufferInfo.ext_mem_start_addr == NULL) {
        ALOGI("ERROR: allocate gWorkBufferInfo.ext_mem_start_addr memory fail!!!");
        return false;
    }
    gWorkBufferInfo.ext_mem_size = buffer_size;
    mImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_SET_WORKBUF_INFO, (void*)&gWorkBufferInfo, NULL);

    mImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_GET_SAVE_WORKBUF_SIZE, NULL, (void*)&buffer_size);
    unsigned char* pSaveBuffer = (unsigned char*)malloc(buffer_size);
    if (NULL == pSaveBuffer) {
        ALOGI("ERROR: allocate pSaveBuffer memory fail!!!");
        return false;
    }
    MUINT8* pImageBuffer = (MUINT8*) malloc(imgWidth * imgHeight * 3);
    if (NULL == pImageBuffer) {
        ALOGI("ERROR: allocate pImageData memory fail!!!");
        free(pSaveBuffer);
        pSaveBuffer = NULL;
        return false;
    }
    for (int i = 0; i < imgWidth * imgHeight; i++) {
        pImageBuffer[i * 3 + 0] = img[i * 4 + 0];
        pImageBuffer[i * 3 + 1] = img[i * 4 + 1];
        pImageBuffer[i * 3 + 2] = img[i * 4 + 2];
    }
    memset(&gProcInfo, 0, sizeof(gProcInfo));
    gProcInfo.scenario = SEGMENT_SCENARIO_SAVE;
    gProcInfo.input_color_img_addr = pImageBuffer;
    gProcInfo.prev_output_mask_addr = mask;
    gProcInfo.working_buffer_addr = pSaveBuffer;

    mImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_SET_PROC_INFO, (void*)&gProcInfo, NULL);
    mImageSegment->Main();
    mImageSegment->FeatureCtrl(IMAGE_SEGMENT_FEATURE_GET_RESULT, NULL, (void*)&gResult);

    m_newMask.release();
    m_newMask.alloc(imgWidth, imgHeight, 1);
    memcpy(m_newMask.ptr, gResult.output_mask_addr, m_newMask.size);
    m_newMaskPoint.x = gResult.center.x;
    m_newMaskPoint.y = gResult.center.y;

    m_newMaskRect.left = gResult.bbox.left;
    m_newMaskRect.top = gResult.bbox.top;
    m_newMaskRect.right = gResult.bbox.right;
    m_newMaskRect.bottom = gResult.bbox.bottom;
    free(pSaveBuffer);
    free(pImageBuffer);
    pSaveBuffer = NULL;
    pImageBuffer = NULL;

    ALOGI("<setNewBitmap> output_mask_addr=%p,center.x=%d,center.y=%d,bbox.left=%d,bbox.top=%d,bbox.right=%d,bbox.bottom=%d",
            gResult.output_mask_addr, gResult.center.x, gResult.center.y, gResult.bbox.left, gResult.bbox.top,
            gResult.bbox.right, gResult.bbox.bottom);

    if (NULL != m_DebugUtils) {
        m_DebugUtils->dumpBufferToPPM("newMask.ppm", m_newMask.ptr, imgWidth, imgHeight, 5, 3);
    }
    return true;
}

void ImageSegment::release() {
    ALOGI("<release>");

    if (mImageSegment != NULL) {
        mImageSegment->Reset();
        mImageSegment->destroyInstance(mImageSegment);
        mImageSegment = NULL;
    }
    if (gWorkBufferInfo.ext_mem_start_addr != NULL) {
        free(gWorkBufferInfo.ext_mem_start_addr);
        gWorkBufferInfo.ext_mem_start_addr = NULL;
    }
    if (NULL != m_ImageData) {
        free(m_ImageData);
        m_ImageData = NULL;
    }
    if (NULL != m_pDepthBuf) {
        free(m_pDepthBuf);
        m_pDepthBuf = NULL;
    }
    if (NULL != m_pOccBuf) {
        free(m_pOccBuf);
        m_pOccBuf = NULL;
    }
    for (int i = 0; i < MAX_UNDO_NUM; i++) {
        user_scribbles[i].release();
        alpha_mask[i].release();
    }
    if (m_DebugUtils != NULL) {
        delete m_DebugUtils;
        m_DebugUtils = NULL;
    }
    m_newMask.release();
}
}  // namespace android
