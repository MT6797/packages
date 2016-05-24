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

#ifndef SEGMENT_H_
#define SEGMENT_H_

#include "MTKImageSegment.h"
#include "Image.h"
#include "../DebugUtils.h"

#include <utils/Log.h>
#include <stdio.h>
#include <assert.h>
#include <string.h>

namespace android {

#define ALIGNMENT_BYTE 4

#define ALIGN128(x)  ((x + 127)&(~(128-1)))
#define ALIGN16(x)  ((x + 15)&(~(16-1)))
#define ALIGN4(x)  ((x+3)&(~3))

#define min(x, y) (x > y ? y : x)
#define max(x, y) (x > y ? x : y)

#define SCENARIO_AUTO 0
#define SCENARIO_SELECTION 1
#define SCENARIO_SCRIBBLE_FG 2
#define SCENARIO_SCRIBBLE_BG 3

#define ALPHA 0xFF
#define COVER 0xBB

#define SEGMENT_DUMP_PATH "/storage/sdcard0/segment/"

class ImageSegment {
public:
    ImageSegment();
    ~ImageSegment();

    bool init(unsigned char* bitmap, int imgWidth, int imgHeight, unsigned char* depthImagBuf, int depthWidth,
            int depthHeight, unsigned char* occImgBuf, int occImgWidth, int occImgHeight, int scribbleWidth,
            int scribbleHeight, int faceNum, Rect* faceRect, int* pFaceRip, int orientation);

    bool doSegment(int scenario, int mode, unsigned char* scribbleBuf, Rect roiRect);

    bool undoSegment();

    bool initSegmentMask(unsigned char* mask, Rect rect, Point point);

    unsigned char* getSegmentMask(bool isNew);

    Point getSegmentPoint(bool isNew);

    Rect getSegmentRect(bool isNew);

    bool getSegmentImg(char* oriImg, int oriWidth, int oriHeight, char* newImg, int width,
            int height, bool isNew);

    bool fillMaskToImg(char* bitmap, int width, int height);

    bool setNewBitmap(char* bitmap, int width, int height);

    bool setNewBitmap(char* bitmap, int bitmapWidth, int bitmapHeight, unsigned char* mask,
            int maskWidth, int maskHeight);

    void release();

private:
    const static int MAX_UNDO_NUM = 5;
    SEGMENT_CORE_SET_ENV_INFO_STRUCT gEnvInfo;
    SEGMENT_CORE_SET_PROC_INFO_STRUCT gProcInfo;
    SEGMENT_CORE_SET_WORK_BUF_INFO_STRUCT gWorkBufferInfo;
    SEGMENT_CORE_SET_SAVE_INFO_STRUCT gSaveInfo;
    SEGMENT_CORE_RESULT_STRUCT gResult;

    MTKImageSegment* mImageSegment = NULL;
    MUINT8* m_ImageData = NULL;
    MUINT8* m_pOccBuf = NULL;
    MUINT8* m_pDepthBuf = NULL;

    Image<unsigned char> user_scribbles[MAX_UNDO_NUM];
    Image<unsigned char> alpha_mask[MAX_UNDO_NUM];
    Image<unsigned char> m_newMask;
    Point m_objPoint[MAX_UNDO_NUM];
    Point m_newMaskPoint;
    Rect m_objRect[MAX_UNDO_NUM];
    Rect m_newMaskRect;

    int m_EditCurrIdx = 0;
    int m_ImageWidth = 0;
    int m_ImageHeight = 0;

    bool m_isUndo = false;
    // add for debug
    DebugUtils *m_DebugUtils = NULL;
};
}  // namespace android
#endif /* SEGMENT_H_ */
