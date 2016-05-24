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
package com.mediatek.galleryfeature.stereo.fancycolor;

import android.os.Environment;
import android.util.Log;

import com.mediatek.xmp.JsonParser;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Helper class for fancy color effect feature.
 */
public class FancyColorHelper {
    private static final String TAG = "MtkGallery2/FancyColor/FancyColorHelper";
    public static final int MSG_UPDATE_VIEW = 1;
    public static final int MSG_LOADING_FINISH = 2;
    public static final int MSG_SAVING_FINISH = 3;
    public static final int MSG_RELOAD_THUMB_VIEW = 4;
    public static final int MSG_STATE_ERROR = 5;
    public static final int MSG_HIDE_LOADING_PROGRESS = 6;

    public static final String EFFECT_NAME_NORMAL = "imageFilterNormal";
    public static final String EFFECT_NAME_MONO_CHROME = "imageFilterBwFilter";
    public static final String EFFECT_NAME_POSTERIZE = "imageFilterKMeans";
    public static final String EFFECT_NAME_RADIAL_BLUR = "imageFilterRadialBlur";
    public static final String EFFECT_NAME_STROKE = "imageFilterStroke";
    public static final String EFFECT_NAME_SIHOUETTE = "imageFilterSihouette";
    public static final String EFFECT_NAME_WHITE_BOARD = "imageFilterWhiteBoard";
    public static final String EFFECT_NAME_BLACK_BOARD = "imageFilterBlackBoard";
    public static final String EFFECT_NAME_NEGATIVE = "imageFilterNegative";

    public static final int TYPE_PREVIEW_THUMBNAIL = 1;
    public static final int TYPE_THUMBNAIL = 2;
    public static final int TYPE_HIGH_RES_THUMBNAIL = 3;

    private static final int PREVIEW_THUMBNAIL_SIZE = 640;
    private static final int DEFAULT_VIEW_WIDTH = 720;
    private static final int DEFAULT_VIEW_HEIGHT = 1080;
    private static final int DEFAULT_ROW_COUNT = 3;
    private static final int DEFAULT_COLUM_COUNT = 3;

    private static int sViewWidth = DEFAULT_VIEW_WIDTH;
    private static int sViewHeight = DEFAULT_VIEW_HEIGHT;
    private static final String KEY_GRID_SPEC = "GridSpec";
    private static final String KEY_ROW_COUNT = "RowCount";
    private static final String KEY_COLUM_COUNT = "ColumCount";
    private static final String KEY_EFFECTS = "Effects";
    private static JsonParser sJsonParser;
    private static final String CONFIG_PATH = Environment.getExternalStorageDirectory().getPath()
            + "/fancy_color_config.txt";

    /**
     * Get decoding target size.
     *
     * @param type
     *            thumbnail type
     * @return target size for current type
     */
    public static int getTargetSize(int type) {
        if (TYPE_PREVIEW_THUMBNAIL == type) {
            return PREVIEW_THUMBNAIL_SIZE;
        } else if (TYPE_THUMBNAIL == type) {
            return Math.min(sViewWidth, sViewHeight);
        } else if (TYPE_HIGH_RES_THUMBNAIL == type) {
            return Math.max(sViewWidth, sViewHeight);
        }
        return Math.min(sViewWidth, sViewHeight);
    }

    /**
     * Set view size.
     *
     * @param viewWidth
     *            view width
     * @param viewHeight
     *            view height
     */
    public static void setViewSize(int viewWidth, int viewHeight) {
        sViewWidth = viewWidth;
        sViewHeight = viewHeight;
    }

    /**
     * Get grid view row count.
     *
     * @return row count
     */
    public static int getRowCount() {
        if (sJsonParser == null) {
            byte[] in = readFileToBuffer(CONFIG_PATH);
            if (in == null) {
                return DEFAULT_ROW_COUNT;
            }
            sJsonParser = new JsonParser(in);
        }
        int rowCount = sJsonParser.getValueIntFromObject(KEY_GRID_SPEC, null, KEY_ROW_COUNT);
        if (rowCount <= 0) {
            return DEFAULT_ROW_COUNT;
        }
        return rowCount;
    }

    /**
     * Get grid view colum count.
     *
     * @return colum count
     */
    public static int getColumCount() {
        if (sJsonParser == null) {
            byte[] in = readFileToBuffer(CONFIG_PATH);
            if (in == null) {
                return DEFAULT_COLUM_COUNT;
            }
            sJsonParser = new JsonParser(in);
        }
        int columCount = sJsonParser.getValueIntFromObject(KEY_GRID_SPEC, null, KEY_COLUM_COUNT);
        if (columCount <= 0) {
            return DEFAULT_COLUM_COUNT;
        }
        return columCount;
    }

    /**
     * Get config from file.
     *
     * @return int array containing effect loading flag.
     */
    public static int[] getEffectsFromConfig() {
        if (sJsonParser == null) {
            byte[] in = readFileToBuffer(CONFIG_PATH);
            if (in == null) {
                return null;
            }
            sJsonParser = new JsonParser(in);
        }
        return sJsonParser.getIntArrayFromObject(null, KEY_EFFECTS);
    }

    private static byte[] readFileToBuffer(String filePath) {
        File inFile = new File(filePath);
        if (!inFile.exists()) {
            Log.d(TAG, "<readFileToBuffer> " + filePath + " not exists!!!");
            return null;
        }

        RandomAccessFile rafIn = null;
        try {
            rafIn = new RandomAccessFile(inFile, "r");
            int len = (int) inFile.length();
            byte[] buffer = new byte[len];
            rafIn.read(buffer);
            return buffer;
        } catch (IOException e) {
            Log.e(TAG, "<readFileToBuffer> Exception ", e);
            return null;
        } finally {
            try {
                if (rafIn != null) {
                    rafIn.close();
                    rafIn = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "<readFileToBuffer> close IOException ", e);
            }
        }
    }
}
