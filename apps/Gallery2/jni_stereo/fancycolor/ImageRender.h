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

#ifndef IMAGERENDER_H_
#define IMAGERENDER_H_

#include <stdlib.h>
#include <math.h>
#include <string.h>
#include <utils/Log.h>
#include "../DebugUtils.h"

class ImageRender {
#define RED i
#define GREEN i+1
#define BLUE i+2
#define ALPHA i+3
#define CLAMP(c) (MAX(0, MIN(255, c)))

#define MIN(a, b) (a < b ? a : b)
#define MAX(a, b) (a > b ? a : b)

#define FANCYCOLOR_DUMP_PATH "/storage/sdcard0/fancycolor/"

public:
    ImageRender();
    virtual ~ImageRender();

    void imageFilterNegative(char* bitmap, int width, int height);

    void ImageFilterBwFilter(char * bitmap, int width, int height, int rw, int gw, int bw);

    void ImageFilterWBalance(char* bitmap, int width, int height, int locX, int locY);

    void ImageFilterBlackBoard(char * bitmap, int width, int height, float p);

    void ImageFilterWhiteBoard(char * bitmap, int width, int height, float p);

    void ImageFilterKMeans(char* bitmap, int widht, int height, char* large_ds_bitmap, int lwidth, int lheight,
            char* small_ds_bitmap, int swidth, int sheight, int p, int seed);

    void ImageFilterSihouette(char* bitmap, unsigned char*mask, int width, int height);

    void merge2Bitmap(char* filterBitmap, char* oriBitmap, unsigned char*mask, int width, int height);

private:
    void estmateWhite(unsigned char *src, int len, int *wr, int *wb, int *wg);

    void estmateWhiteBox(unsigned char *src, int iw, int ih, int x, int y, int *wr, int *wb, int *wg);

    template<typename T>
    void initialPickHeuristicRandom(int k, T values[], int len, int dimension, int stride, T dst[], unsigned int seed);

    template<typename T, typename N>
    int calculateNewCentroids(int k, T values[], int len, int dimension, int stride, T oldCenters[], T dst[]);

    template<typename T, typename N>
    void runKMeansWithPicks(int k, T finalCentroids[], T values[], int len, int dimension, int stride, int iterations,
            T initialPicks[]);

    template<typename T, typename N>
    void runKMeans(int k, T finalCentroids[], T values[], int len, int dimension, int stride, int iterations,
            unsigned int seed);

    template<typename T, typename N>
    void applyCentroids(int k, T centroids[], T values[], int len, int dimension, int stride);

    unsigned char clamp(int c) {
        int N = 255;
        c &= ~(c >> 31);
        c -= N;
        c &= (c >> 31);
        c += N;
        return (unsigned char) c;
    }

    template<typename T, typename N>
    void sum(T values[], int len, int dimension, int stride, N dst[]) {
        int x, y;
        // zero out dst vector
        for (x = 0; x < dimension; x++) {
            dst[x] = 0;
        }
        for (x = 0; x < len; x += stride) {
            for (y = 0; y < dimension; y++) {
                dst[y] += values[x + y];
            }
        }
    }

    template<typename T, typename N>
    void set(T val1[], N val2[], int dimension) {
        int x;
        for (x = 0; x < dimension; x++) {
            val1[x] = val2[x];
        }
    }

    template<typename T, typename N>
    void add(T val[], N dst[], int dimension) {
        int x;
        for (x = 0; x < dimension; x++) {
            dst[x] += val[x];
        }
    }

    template<typename T, typename N>
    void divide(T dst[], N divisor, int dimension) {
        int x;
        if (divisor == 0) {
            return;
        }
        for (x = 0; x < dimension; x++) {
            dst[x] /= divisor;
        }
    }

    /**
     * Calculates euclidean distance.
     */
    template<typename T, typename N>
    N euclideanDist(T val1[], T val2[], int dimension) {
        int x;
        N sum = 0;
        for (x = 0; x < dimension; x++) {
            N diff = (N) val1[x] - (N) val2[x];
            sum += diff * diff;
        }
        return sqrt(sum);
    }

    /**
     * Finds index of closet centroid to a value
     */
    template<typename T, typename N>
    int findClosest(T values[], T oldCenters[], int dimension, int stride, int pop_size) {
        int best_ind = 0;
        N best_len = euclideanDist<T, N>(values, oldCenters, dimension);
        int y;
        for (y = stride; y < pop_size; y += stride) {
            N l = euclideanDist<T, N>(values, oldCenters + y, dimension);
            if (l < best_len) {
                best_len = l;
                best_ind = y;
            }
        }
        return best_ind;
    }

    DebugUtils* m_DebugUtils = NULL;
};

#endif /* IMAGERENDER_H_ */
