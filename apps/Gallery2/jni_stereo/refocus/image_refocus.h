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

#include <utils/Log.h>

#include "MTKRefocus.h"
#include "JpegFactory.h"
#include "../DebugUtils.h"
#include "../StereoUtils.h"

namespace android {

/*********************************************************************/
#define ALIGN128(x)  ((x + 127)&(~(128-1)))
#define DFT_RCFY_ERROR       0
#define DFT_RCFY_ITER_NO    10
#define DFT_THETA            0
#define DFT_DISPARITY_RANGE 10

/*********************************************************************/

#define FILE_NAME_LENGTH 100
#define PATH_NAME_LENGTH 300
#define SUCCESS true
#define FAIL false
#define REFOCUS_DUMP_PATH  "/storage/sdcard0/refocus/"

#define testDepthOfField 16

#define  ORIENTATION_0 0
#define  ORIENTATION_90 90
#define  ORIENTATION_180 180
#define  ORIENTATION_270 270

#define  DEPTH_BUFFER_SECTION_SIZE 9
#define  WMI_DEPTH_MAP_INDEX 0
#define  VAR_BUFFER_INDEX 2  // no need to rotate
#define  DVEC_MAP_INDEX 5  // no need to rotate
#define  DS4_BUFFER_Y_INDEX 8

struct ImageBuffer {
    int width;
    int height;
    unsigned char * buffer;
};

class ImageRefocus {
private:
    // Refocus global
    uint8_t *pWorkingBuffer = NULL;
    uint8_t *mPLdcBuffer = NULL;
    uint8_t *mPMaskBuffer = NULL;
    MTKRefocus *mRefocus = NULL;
    JpegFactory *m_pJpegFactory = NULL;
    RefocusInitInfo mRefocusInitInfo;
    RefocusTuningInfo mRefocusTuningInfo;
    RefocusImageInfo mRefocusImageInfo;
    RefocusResultInfo mRefocusResultInfo;

    int mOrientation = 0;
    int mDepthRotation = 0;  // rotate depth buffer with this rotation
    int mDepthBufferWidth = 0;
    int mDepthBufferHeight = 0;
    int mDepthBufferSize = 0;
    int mMetaBufferWidth = 0;
    int mMetaBufferHeight = 0;
    int mXMPDepthWidth = 0;
    int mXMPDepthHeight = 0;
    int mXMPDepthSize = 0;

    char mSourceFileName[FILE_NAME_LENGTH];  // prefix name

    DebugUtils* m_DebugUtils = NULL;

    void dumpBufferToFile(MUINT8* buffer, int bufferSize, char* filename);
    void initRefocusIMGSource(const char *sourceFilePath, int outImgWidth, int outImgHeight, int imgOrientation);
    bool createRefocusInstance();
    bool setBufferAddr();
    void copyRefocusResultInfo(RefocusResultInfo* refocusResultInfo);
    void rotateBuffer(MUINT8* bufferIn, MUINT8* bufferOut, int bufferWidth, int bufferHeight, int orientation);
    void swap(unsigned int* x, unsigned int* y);
    void swapConfigInfo();
    bool initRefocusIMGSource(uint8_t* jpegBuf, int bufSize, int outImgWidth, int outImgHeight);
    bool initJPSBuffer(MUINT8* jpsBuffer, int jpsBufferSize, int jpsWidth, int jpsHeight);

public:
    ImageRefocus(int jpsWidth, int jpsHeight, int maskWidth, int maskHeight, int posX, int posY, int viewWidth,
            int viewHeight, int orientation, int mainCamPos, int touchCoordX1st, int touchCoordY1st, int refocusMode,
            int depthRotation);
    bool initRefocusNoDepthMap(uint8_t* jpegBuf, int jpegbufferSize, int outImgWidth,
            int outImgHeight, int orientation, uint8_t* jpsBuffer, int jpsBufferSize, int jpsWidth, int jpsHeight,
            uint8_t* maskBuffer, int maskBufferSize, int maskWidth, int maskHeight,
            uint8_t* ldcBuffer, int ldcBufferSize, int ldcWidth, int ldcHeight);
    bool initRefocusNoDepthMap(const char *sourceFilePath, int outImgWidth, int outImgHeight, int imgOrientation,
            MUINT8* jpsBuffer, int jpsBufferSize, int inStereoImgWidth, int inStereoImgHeight, MUINT8* maskBuffer,
            int maskBufferSize, int maskWidth, int maskHeight, uint8_t* ldcBuffer, int ldcBufferSize,
            int ldcWidth, int ldcHeight);
    bool initRefocusWithDepthMap(uint8_t* jpegBuf, int jpegbufferSize, int outImgWidth, int outImgHeight,
            int orientation, MUINT8* depthBuffer, int depthBufferSize, int jpsWidth, int jpsHeight);

    bool initRefocusWithDepthMap(const char *sourceFilePath, int outImgWidth, int outImgHeight, int imgOrientation,
            MUINT8* depthMapBuffer, int depthBufferSize, int inStereoImgWidth, int inStereoImgHeight);
    ImageBuffer generateRefocusImage(int touchCoordX, int touchCoordY, int depthOfField);
    bool generate();
    void deinit();
    void saveDepthMapInfo(MUINT8* depthBufferArray, MUINT8* xmpDepthBufferArray);
    void saveRefocusImage(const char *saveFileName, int inSampleSize);
    int getDepthBufferSize();
    int getDepthBufferWidth();
    int getDepthBufferHeight();
    int getXMPDepthBufferSize();
    int getXMPDepthBufferWidth();
    int getXMPDepthBufferHeight();
    int getMetaBufferWidth();
    int getMetaBufferHeight();
    ~ImageRefocus();
    StereoUtils mStereoUtils;
};
}  // namespace android
