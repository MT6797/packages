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
#ifndef __IMAGE_H__
#define __IMAGE_H__

#include <stdlib.h>
#include <string.h>

#define ALIGNMENT_BYTE  4

template<class T>
class Image {
public:
    Image() {
        this->init();
    }
    Image(int w, int h, int d) {
        this->init();
        alloc(w, h, d);
    }
    ~Image() {
        this->release();
    }

    void init() {
        ptr = NULL;
        width = 0;
        height = 0;
        channel = 0;
        stride = 0;
        size = 0;
    }

    void release() {
        if (ptr != NULL)
            free(ptr);
        this->init();
    }

    void alloc(int w, int h, int d) {
        int w_stride = ((w * d * sizeof(T) + ALIGNMENT_BYTE - 1) / ALIGNMENT_BYTE) * ALIGNMENT_BYTE;
        int new_size = w_stride * h;
        if (new_size <= 0)
            // Invalid allocate
            return;
        if (new_size == size) {
            // same as previous
        } else {
            // re-allocate
            this->release();
            size = new_size;
            ptr = (T*) malloc(size * sizeof(T));
        }
        memset(ptr, 0, size * sizeof(T));

        // update size
        width = w;
        height = h;
        channel = d;
        stride = w_stride;
    }

    void clear() {
        if (ptr) {
            memset(ptr, 0, size * sizeof(T));
        }
    }

    void set(T value) {
        if (ptr) {
            memset(ptr, value, size * sizeof(T));
        }
    }

    void copy(Image<T> *img) {
        if (img && img->width && img->height) {
            alloc(img->width, img->height, img->channel);
            memcpy(ptr, img->ptr, size);
        }
    }

    T* ptr;
    int width;
    int height;
    int channel;
    int stride;
    int size;
};

#endif
