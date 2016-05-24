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

package com.mediatek.galleryfeature.stereo.segment.background;

import android.content.Intent;
import android.provider.MediaStore.Images.ImageColumns;

import com.mediatek.galleryframework.base.MediaFilter;
import com.mediatek.galleryframework.base.MediaFilter.IFilter;

/**
 * Filter images that can be background in background substitution feature.
 */
public class SegmentBackgroundFilter implements IFilter {
    public static final String FILTER_TAG = "sbg";

    @Override
    public void setFlagFromIntent(Intent intent, MediaFilter filter) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (Intent.ACTION_GET_CONTENT.equalsIgnoreCase(action)) {
            String tag = intent.getStringExtra(FILTER_TAG);
            if (tag != null && tag.equalsIgnoreCase(FILTER_TAG)) {
                filter.setFlagEnable(MediaFilter.INCLUDE_SEGMENT_BACKGROUND);
            }
        }
    }

    @Override
    public void setDefaultFlag(MediaFilter filter) {
        filter.setFlagDisable(MediaFilter.INCLUDE_SEGMENT_BACKGROUND);
    }

    @Override
    public String getWhereClauseForImage(int flag, int bucketID) {
        // TODO condition is not complete.
        if ((flag & MediaFilter.INCLUDE_SEGMENT_BACKGROUND) != 0) {
            String str = MediaFilter.AND(ImageColumns.WIDTH + ">=900", ImageColumns.HEIGHT
                    + ">=900");
            return str;
        }
        return null;
    }

    @Override
    public String getWhereClauseForVideo(int flag, int bucketID) {
        return null;
    }

    @Override
    public String getWhereClause(int flag, int bucketID) {
        return getWhereClauseForImage(flag, bucketID);
    }

    @Override
    public String getDeleteWhereClauseForImage(int flag, int bucketID) {
        return getWhereClauseForImage(flag, bucketID);
    }

    @Override
    public String getDeleteWhereClauseForVideo(int flag, int bucketID) {
        return null;
    }

    @Override
    public String convertFlagToString(int flag) {
        if ((flag & MediaFilter.INCLUDE_SEGMENT_BACKGROUND) != 0) {
            return "INCLUDE_SEGMENT_BACKGROUND, ";
        }
        return "";
    }
}
