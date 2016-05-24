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

#include "ImageRender.h"

ImageRender::ImageRender() {
    if (access(FANCYCOLOR_DUMP_PATH, 0) != -1) {
        m_DebugUtils = new DebugUtils(FANCYCOLOR_DUMP_PATH);
    }
}

ImageRender::~ImageRender() {
    if (NULL != m_DebugUtils) {
        delete m_DebugUtils;
        m_DebugUtils = NULL;
    }
}

void ImageRender::imageFilterNegative(char* bitmap, int width, int height) {
    int tot_len = height * width * 4;
    int i;
    char * dst = bitmap;
    for (i = 0; i < tot_len; i += 4) {
        dst[RED] = 255 - dst[RED];
        dst[GREEN] = 255 - dst[GREEN];
        dst[BLUE] = 255 - dst[BLUE];
    }

    if (NULL != m_DebugUtils) {
        m_DebugUtils->dumpBufferToPPM("imageFilterNegative.ppm", (unsigned char*)bitmap, width, height, 6, 4);
    }
}

void ImageRender::ImageFilterWBalance(char* bitmap, int width, int height, int locX, int locY) {
    int i;
    int len = width * height * 4;
    unsigned char * rgb = (unsigned char *) bitmap;
    int wr;
    int wg;
    int wb;

    if (locX == -1)
        estmateWhite(rgb, len, &wr, &wg, &wb);
    else
        estmateWhiteBox(rgb, width, height, locX, locY, &wr, &wg, &wb);

    int min = MIN(wr, MIN(wg, wb));
    int max = MAX(wr, MAX(wg, wb));
    float avg = (min + max) / 2.f;
    float scaleR = avg / wr;
    float scaleG = avg / wg;
    float scaleB = avg / wb;

    for (i = 0; i < len; i += 4) {
        int r = rgb[RED];
        int g = rgb[GREEN];
        int b = rgb[BLUE];

        float Rc = r * scaleR;
        float Gc = g * scaleG;
        float Bc = b * scaleB;

        rgb[RED] = clamp(Rc);
        rgb[GREEN] = clamp(Gc);
        rgb[BLUE] = clamp(Bc);
    }
}

void ImageRender::ImageFilterBwFilter(char * bitmap, int width, int height, int rw, int gw, int bw) {
    unsigned char * rgb = (unsigned char *) bitmap;
    float sr = rw;
    float sg = gw;
    float sb = bw;

    float min = MIN(sg, sb);
    min = MIN(sr, min);
    float max = MAX(sg, sb);
    max = MAX(sr, max);
    float avg = (min + max) / 2;
    sb /= avg;
    sg /= avg;
    sr /= avg;
    int i;
    int len = width * height * 4;

    for (i = 0; i < len; i += 4) {
        float r = sr * rgb[RED];
        float g = sg * rgb[GREEN];
        float b = sb * rgb[BLUE];
        min = MIN(g, b);
        min = MIN(r, min);
        max = MAX(g, b);
        max = MAX(r, max);
        avg = (min + max) / 2;
        rgb[RED] = CLAMP(avg);
        rgb[GREEN] = rgb[RED];
        rgb[BLUE] = rgb[RED];
    }
}

void ImageRender::ImageFilterBlackBoard(char * bitmap, int width, int height, float p) {
    // using contrast function:
    // f(v) = exp(-alpha * v^beta)
    // use beta ~ 1

    float const alpha = 5.0f;
    float const beta = p;
    float const c_min = 100.0f;
    float const c_max = 500.0f;

    // pixels must be 4 bytes
    char * dst = bitmap;

    int j, k;
    char * ptr = bitmap;
    int row_stride = 4 * width;

    // set 2 row buffer (avoids bitmap copy)
    int buf_len = 2 * row_stride;
    char buf[buf_len];
    int buf_row_ring = 0;

    // set initial buffer to black
    memset(buf, 0, buf_len * sizeof(char));
    // set initial alphas
    for (j = 3; j < buf_len; j += 4) {
        *(buf + j) = 255;
    }

    // apply sobel filter
    for (j = 1; j < height - 1; j++) {
        for (k = 1; k < width - 1; k++) {
            int loc = j * row_stride + k * 4;
            float bestx = 0.0f;
            int l;
            for (l = 0; l < 3; l++) {
                float tmp = 0.0f;
                tmp += *(ptr + (loc - row_stride + 4 + l));
                tmp += *(ptr + (loc + 4 + l)) * 2.0f;
                tmp += *(ptr + (loc + row_stride + 4 + l));
                tmp -= *(ptr + (loc - row_stride - 4 + l));
                tmp -= *(ptr + (loc - 4 + l)) * 2.0f;
                tmp -= *(ptr + (loc + row_stride - 4 + l));
                if (fabs(tmp) > fabs(bestx)) {
                    bestx = tmp;
                }
            }

            float besty = 0.0f;
            for (l = 0; l < 3; l++) {
                float tmp = 0.0f;
                tmp -= *(ptr + (loc - row_stride - 4 + l));
                tmp -= *(ptr + (loc - row_stride + l)) * 2.0f;
                tmp -= *(ptr + (loc - row_stride + 4 + l));
                tmp += *(ptr + (loc + row_stride - 4 + l));
                tmp += *(ptr + (loc + row_stride + l)) * 2.0f;
                tmp += *(ptr + (loc + row_stride + 4 + l));
                if (fabs(tmp) > fabs(besty)) {
                    besty = tmp;
                }
            }

            // compute gradient magnitude
            float mag = sqrt(bestx * bestx + besty * besty);

            // clamp
            mag = MIN(MAX(c_min, mag), c_max);

            // scale to [0, 1]
            mag = (mag - c_min) / (c_max - c_min);

            float ret = 1.0f - exp(-alpha * pow(mag, beta));
            ret = 255 * ret;

            int off = k * 4;
            *(buf + buf_row_ring + off) = ret;
            *(buf + buf_row_ring + off + 1) = ret;
            *(buf + buf_row_ring + off + 2) = ret;
            *(buf + buf_row_ring + off + 3) = *(ptr + loc + 3);
        }

        buf_row_ring += row_stride;
        buf_row_ring %= buf_len;
        if (j - 1 >= 0) {
            memcpy((dst + row_stride * (j - 1)), (buf + buf_row_ring), row_stride * sizeof(char));
        }
    }
    buf_row_ring += row_stride;
    buf_row_ring %= buf_len;
    int second_last_row = row_stride * (height - 2);
    memcpy((dst + second_last_row), (buf + buf_row_ring), row_stride * sizeof(char));

    // set last row to black
    int last_row = row_stride * (height - 1);
    memset((dst + last_row), 0, row_stride * sizeof(char));
    // set alphas
    for (j = 3; j < row_stride; j += 4) {
        *(dst + last_row + j) = 255;
    }

    if (NULL != m_DebugUtils) {
        m_DebugUtils->dumpBufferToPPM("ImageFilterBlackBoard.ppm", (unsigned char*)bitmap, width, height, 6, 4);
    }
}

void ImageRender::ImageFilterWhiteBoard(char * bitmap, int width, int height, float p) {
    ImageFilterBlackBoard(bitmap, width, height, p);
    imageFilterNegative(bitmap, width, height);
}

void ImageRender::ImageFilterKMeans(char * bitmap, int width, int height, char* large_ds_bitmap, int lwidth,
        int lheight, char* small_ds_bitmap, int swidth, int sheight, int p, int seed) {
    unsigned char * dst = (unsigned char *) bitmap;
    unsigned char * small_ds = (unsigned char *) small_ds_bitmap;
    unsigned char * large_ds = (unsigned char *) large_ds_bitmap;

    // setting for small bitmap
    int len = swidth * sheight * 4;
    int dimension = 3;
    int stride = 4;
    int iterations = 20;
    int k = p;
    unsigned int s = seed;
    unsigned char finalCentroids[k * stride];

    // get initial picks from small downsampled image
    runKMeans<unsigned char, int>(k, finalCentroids, small_ds, len, dimension, stride, iterations, s);

    len = lwidth * lheight * 4;
    iterations = 8;
    unsigned char nextCentroids[k * stride];

    // run kmeans on large downsampled image
    runKMeansWithPicks<unsigned char, int>(k, nextCentroids, large_ds, len, dimension, stride, iterations,
            finalCentroids);

    len = width * height * 4;

    // apply to final image
    applyCentroids<unsigned char, int>(k, nextCentroids, dst, len, dimension, stride);
}

void ImageRender::ImageFilterSihouette(char* bitmap, unsigned char*mask, int width, int height) {
    char * dst = bitmap;
    for (int i = 0; i < width * height; i++) {
        if (mask[i] > 0) {
            dst[i * 4 + 0] = 0;
            dst[i * 4 + 1] = 0;
            dst[i * 4 + 2] = 0;
        }
    }
}

void ImageRender::merge2Bitmap(char* dst, char* ori, unsigned char*mask, int width, int height) {
    int tot_len = height * width * 4;
    int i, j;
    for (i = 0, j = 0; i < tot_len; i += 4, j++) {
        dst[RED] = dst[RED] * ((float) mask[j] / 255) + ori[RED] * (1 - (float) mask[j] / 255);
        dst[GREEN] = dst[GREEN] * ((float) mask[j] / 255) + ori[GREEN] * (1 - (float) mask[j] / 255);
        dst[BLUE] = dst[BLUE] * ((float) mask[j] / 255) + ori[BLUE] * (1 - (float) mask[j] / 255);
    }
}

void ImageRender::estmateWhite(unsigned char *src, int len, int *wr, int *wb, int *wg) {
    int STEP = 4;
    int RANGE = 256;
    int *histR = (int *) malloc(256 * sizeof(int));
    int *histG = (int *) malloc(256 * sizeof(int));
    int *histB = (int *) malloc(256 * sizeof(int));
    int i;
    for (i = 0; i < 255; i++) {
        histR[i] = histG[i] = histB[i] = 0;
    }

    for (i = 0; i < len; i += STEP) {
        histR[(src[RED])]++;
        histG[(src[GREEN])]++;
        histB[(src[BLUE])]++;
    }
    int min_r = -1, min_g = -1, min_b = -1;
    int max_r = 0, max_g = 0, max_b = 0;
    int sum_r = 0, sum_g = 0, sum_b = 0;

    for (i = 1; i < RANGE - 1; i++) {
        int r = histR[i];
        int g = histG[i];
        int b = histB[i];
        sum_r += r;
        sum_g += g;
        sum_b += b;

        if (r > 0) {
            if (min_r < 0)
                min_r = i;
            max_r = i;
        }
        if (g > 0) {
            if (min_g < 0)
                min_g = i;
            max_g = i;
        }
        if (b > 0) {
            if (min_b < 0)
                min_b = i;
            max_b = i;
        }
    }

    int sum15r = 0, sum15g = 0, sum15b = 0;
    int count15r = 0, count15g = 0, count15b = 0;
    int tmp_r = 0, tmp_g = 0, tmp_b = 0;

    for (i = RANGE - 2; i > 0; i--) {
        int r = histR[i];
        int g = histG[i];
        int b = histB[i];
        tmp_r += r;
        tmp_g += g;
        tmp_b += b;

        if ((tmp_r > sum_r / 20) && (tmp_r < sum_r / 5)) {
            sum15r += r * i;
            count15r += r;
        }
        if ((tmp_g > sum_g / 20) && (tmp_g < sum_g / 5)) {
            sum15g += g * i;
            count15g += g;
        }
        if ((tmp_b > sum_b / 20) && (tmp_b < sum_b / 5)) {
            sum15b += b * i;
            count15b += b;
        }
    }
    free(histR);
    free(histG);
    free(histB);

    if ((count15r > 0) && (count15g > 0) && (count15b > 0)) {
        *wr = sum15r / count15r;
        *wb = sum15g / count15g;
        *wg = sum15b / count15b;
    } else {
        *wg = *wb = *wr = 255;
    }
}

void ImageRender::estmateWhiteBox(unsigned char *src, int iw, int ih, int x, int y, int *wr, int *wb, int *wg) {
    int r = 0;
    int g = 0;
    int b = 0;
    int sum = 0;
    int xp = 0;
    int yp = 0;
    int bounds = 5;
    if (x < 0)
        x = bounds;
    if (y < 0)
        y = bounds;
    if (x >= (iw - bounds))
        x = (iw - bounds - 1);
    if (y >= (ih - bounds))
        y = (ih - bounds - 1);
    int startx = x - bounds;
    int starty = y - bounds;
    int endx = x + bounds;
    int endy = y + bounds;

    for (yp = starty; yp < endy; yp++) {
        for (xp = startx; xp < endx; xp++) {
            int i = 4 * (xp + yp * iw);
            r += src[RED];
            g += src[GREEN];
            b += src[BLUE];
            sum++;
        }
    }
    if (0 == sum) {
        return;
    }
    *wr = r / sum;
    *wg = g / sum;
    *wb = b / sum;
}

template<typename T>
void ImageRender::initialPickHeuristicRandom(int k, T values[], int len, int dimension, int stride, T dst[],
        unsigned int seed) {
    int x = 0;
    int z = 0;
    int cntr = 0;
    int num_vals = len / stride;
    srand(seed);
    unsigned int r_vals[k];
    unsigned int r;

    for (x = 0; x < k; x++) {
        /// M: if num_vals <=  k ,Randomly chosen value should go into infinite loops.@{
        if (num_vals <= k) {
            r = (unsigned int) x % num_vals;
        } else {
            /// @}
            // ensure randomly chosen value is unique
            int r_check = 0;
            while (r_check == 0) {
                r = (unsigned int) rand() % num_vals;
                r_check = 1;
                for (z = 0; z < x; z++) {
                    if (r == r_vals[z]) {
                        r_check = 0;
                    }
                }
            }
            r_vals[x] = r;
            r *= stride;
        }
        // set dst to be randomly chosen value
        set<T, T>(dst + cntr, values + r, dimension);
        cntr += stride;
    }
}

template<typename T, typename N>
int ImageRender::calculateNewCentroids(int k, T values[], int len, int dimension, int stride, T oldCenters[], T dst[]) {
    int x, pop_size;
    pop_size = k * stride;
    int popularities[k];
    N tmp[pop_size];

    // zero popularities
    memset(popularities, 0, sizeof(int) * k);
    // zero dst, and tmp
    for (x = 0; x < pop_size; x++) {
        tmp[x] = 0;
    }

    // put summation for each k in tmp
    for (x = 0; x < len; x += stride) {
        int best = findClosest<T, N>(values + x, oldCenters, dimension, stride, pop_size);
        add<T, N>(values + x, tmp + best, dimension);
        popularities[best / stride]++;
    }

    int ret = 0;
    int y;
    // divide to get centroid and set dst to result
    for (x = 0; x < pop_size; x += stride) {
        divide<N, int>(tmp + x, popularities[x / stride], dimension);
        for (y = 0; y < dimension; y++) {
            if ((dst + x)[y] != (T) ((tmp + x)[y])) {
                ret = 1;
            }
        }
        set(dst + x, tmp + x, dimension);
    }
    return ret;
}

template<typename T, typename N>
void ImageRender::runKMeansWithPicks(int k, T finalCentroids[], T values[], int len, int dimension, int stride,
        int iterations, T initialPicks[]) {
    int k_len = k * stride;
    int x;

    // zero newCenters
    for (x = 0; x < k_len; x++) {
        finalCentroids[x] = 0;
    }

    T * c1 = initialPicks;
    T * c2 = finalCentroids;
    T * temp;
    int ret = 1;
    for (x = 0; x < iterations; x++) {
        ret = calculateNewCentroids<T, N>(k, values, len, dimension, stride, c1, c2);
        temp = c1;
        c1 = c2;
        c2 = temp;
        if (ret == 0) {
            x = iterations;
        }
    }
    set<T, T>(finalCentroids, c1, dimension);
}

template<typename T, typename N>
void ImageRender::runKMeans(int k, T finalCentroids[], T values[], int len, int dimension, int stride, int iterations,
        unsigned int seed) {
    int k_len = k * stride;
    T initialPicks[k_len];
    initialPickHeuristicRandom<T>(k, values, len, dimension, stride, initialPicks, seed);

    runKMeansWithPicks<T, N>(k, finalCentroids, values, len, dimension, stride, iterations, initialPicks);
}

template<typename T, typename N>
void ImageRender::applyCentroids(int k, T centroids[], T values[], int len, int dimension, int stride) {
    int x, pop_size;
    pop_size = k * stride;
    for (x = 0; x < len; x += stride) {
        int best = findClosest<T, N>(values + x, centroids, dimension, stride, pop_size);
        set<T, T>(values + x, centroids + best, dimension);
    }
}
