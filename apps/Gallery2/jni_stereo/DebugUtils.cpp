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

#include "DebugUtils.h"

#define LOG_TAG "Gallery2_DebugUtils"

DebugUtils::DebugUtils(const char * path) {
    m_DebugPath = path;
}

DebugUtils::~DebugUtils() {
}

void DebugUtils::dumpBufferToPPM(const char * filename, unsigned char * img, int width, int height,  int format,
        int step) {
    char filePath[256];
    sprintf(filePath, "%s%s", m_DebugPath, filename);

    ALOGI("<_dump_ppm> filePath:%s,img:%p,width:%d,height:%d, format:%d", filePath, img, width, height,
            format, img);

    if(NULL == img)  {
        ALOGI("<_dump_ppm> ERROR,NULL image buffer!!!");
        return;
    }

    int i = 0, j = 0;
    FILE* pFile;
    pFile = fopen(filePath, "wb");

    if (format == 6) {
        fprintf(pFile, "P6\n");
    } else if (format == 5) {
        fprintf(pFile, "P5\n");
    } else {
        fclose(pFile);
        return;
    }

    fprintf(pFile, "%d %d\n255 ", width, height);

    /// P6 format
    int w, h;
    unsigned char *bufferPtr;
    unsigned char * buffer;
    if (format == 6) {
        buffer = new unsigned char[width * 3];

        for (h = 0; h < height; h++) {
            bufferPtr = buffer;
            for (w = 0; w < width; w++) {
                j = step * width * h + step * w;
                i = 3 * w;
                bufferPtr[i] = img[j];
                bufferPtr[i + 1] = img[j + 1];
                bufferPtr[i + 2] = img[j + 2];
            }
            fwrite(buffer, 1, width * 3, pFile);
        }
    } else if (format == 5) {
        buffer = new unsigned char[width];

        for (h = 0; h < height; h++) {
            bufferPtr = buffer;
            for (w = 0; w < width; w++) {
                j = width * h + w;
                i = w;
                bufferPtr[i] = img[j];
            }
            fwrite(buffer, 1, width, pFile);
        }
    }
    fflush(pFile);
    fclose(pFile);
    delete[] buffer;
}

int DebugUtils::dumpBufferToFile(const char* filename, unsigned char* buffer, int size) {
    FILE *fp;
    unsigned char *pData;

    char filePath[256];
    sprintf(filePath, "%s%s", m_DebugPath, filename);

    ALOGI("<dumpBufferToFile>filePath:%s,buffer:%p,size:%d", filePath, buffer, size);

    if (strlen(filePath) == 0) {
        ALOGI("[ERROR] Invalid file name\n");
        return -1;
    }

    if (buffer == 0 || size == 0) {
        ALOGI("[ERROR] Invalid image\n");
        return -1;
    }

    if ((fp = fopen(filePath, "wb")) == NULL) {
        ALOGI("[ERROR] Cannot open file %s\n", filePath);
        return -1;
    }

    pData = buffer;
    fwrite(pData, 1, size, fp);
    fflush(fp);
    fclose(fp);

    return 0;
}
