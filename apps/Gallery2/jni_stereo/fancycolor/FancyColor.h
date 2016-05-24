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
#ifndef FANCYCOLOR_H_
#define FANCYCOLOR_H_

#include <utils/Log.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "MTKFancyColor.h"
#include "../DebugUtils.h"

#define MIN(a, b) (a < b ? a : b)
#define MAX(a, b) (a > b ? a : b)
#define STROKE_DATA 255

#define FANCYCOLOR_DUMP_PATH "/storage/sdcard0/fancycolor/"

typedef struct {
    int left;
    int top;
    int right;
    int bottom;
} Rect;

class FancyColor {
public:
    FancyColor();
    virtual ~FancyColor();

    bool init(unsigned char* color_img, unsigned char* alpha_mask, Rect rect, int img_width, int img_height,
            int center_x, int center_y);

    bool doStrokeEffect();
    bool doRadialBlurEffect();
    void getStrokeImg(char* bitmap, int width, int height);
    void getRadialBlurImg(char* bitmap, int width, int height);

    void release();

private:
    void freeMem(unsigned char* addr);

    FANCY_CORE_SET_ENV_INFO_STRUCT mFancyEnvInfo;
    FANCY_CORE_SET_WORK_BUF_INFO_STRUCT mFancyWorkBufferInfo;
    FANCY_CORE_SET_PROC_INFO_STRUCT mFancyProcInfo;
    FANCY_CORE_RESULT_STRUCT mFancyResult;

    MTKFancyColor* mFancyColor = NULL;

    Rect mMaskRect;
    Rect mStrokeRect;
    Rect mRadialMask;

    unsigned char* mOriImgData = NULL;
    unsigned char* m_ImageData = NULL;
    unsigned char* mAlphaMask = NULL;

    unsigned char* mStrokeOutputMask = NULL;

    unsigned char* mRadialOutputMask = NULL;
    unsigned char* mRadialOutputImg = NULL;

    int mImgWidth = 0;
    int mImgHeight = 0;

    // debug
    DebugUtils* m_DebugUtils = NULL;
};

#endif /* FANCYCOLOR_H_ */
