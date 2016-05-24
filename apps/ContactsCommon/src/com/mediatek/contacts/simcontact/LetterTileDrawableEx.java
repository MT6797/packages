/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
package com.mediatek.contacts.simcontact;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.contacts.common.ContactPhotoManager.DefaultImageRequest;
import com.android.contacts.common.R;
import com.android.contacts.common.lettertiles.LetterTileDrawable;

import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.util.SimContactPhotoUtils;

import java.util.HashMap;

public class LetterTileDrawableEx extends LetterTileDrawable {
    private static final String TAG = LetterTileDrawableEx.class.getSimpleName();

    /**
     * This height ratio is just a experience value. Avatar icon will take up
     * the ratio height of View.
     */
    private static float SIM_AVATAR_HEIGHT_RATIO = 0.32f;
    private static float SIM_AVATAR_WIDTH_RATIO = 0.32f;
    private static float SDN_LOCKED_RATIO = 0.3f;
    /**
     * This width ratio is just a experience value. Avatar icon will take up the
     * ratio width of View.
     */
    private static int SIM_ALPHA = 240;

    private static Bitmap DEFAULT_SIM_AVATAR;
    private static Bitmap DEFAULT_SIM_YELLOW_AVATAR;
    private static Bitmap DEFAULT_SIM_ORANGE_AVATAR;
    private static Bitmap DEFAULT_SIM_GREEN_AVATAR;
    private static Bitmap DEFAULT_SIM_PURPLE_AVATAR;
    private static Bitmap DEFAULT_SIM_SDN_AVATAR;
    private static Bitmap DEFAULT_SIM_YELLOW_SDN_AVATAR;
    private static Bitmap DEFAULT_SIM_ORANGE_SDN_AVATAR;
    private static Bitmap DEFAULT_SIM_GREEN_SDN_AVATAR;
    private static Bitmap DEFAULT_SIM_PURPLE_SDN_AVATAR;
    private static Bitmap DEFAULT_SIM_SDN_AVATAR_LOCKED;

    private long mSdnPhotoId = 0;
    private Paint mSimPaint;
    private static final Paint sPaint = new Paint();
    private int mBackgroundColor;
    private int mSubId = 0;

    public LetterTileDrawableEx(Resources res) {
        super(res);
        mSimPaint = new Paint();
        mSimPaint.setAntiAlias(true);
        // mSimPaint.setAlpha(SIM_ALPHA);
        mSimPaint.setDither(true);
        mBackgroundColor = res.getColor(R.color.background_primary);
        if (DEFAULT_SIM_AVATAR == null) {
            DEFAULT_SIM_AVATAR = BitmapFactory.decodeResource(res,
                    R.drawable.sim_indicator_default_small);
            DEFAULT_SIM_YELLOW_AVATAR = BitmapFactory.decodeResource(res,
                    R.drawable.sim_indicator_yellow_small);
            DEFAULT_SIM_ORANGE_AVATAR = BitmapFactory.decodeResource(res,
                    R.drawable.sim_indicator_orange_small);
            DEFAULT_SIM_GREEN_AVATAR = BitmapFactory.decodeResource(res,
                    R.drawable.sim_indicator_green_small);
            DEFAULT_SIM_PURPLE_AVATAR = BitmapFactory.decodeResource(res,
                    R.drawable.sim_indicator_purple_small);
            // SDN avatar
            DEFAULT_SIM_SDN_AVATAR = BitmapFactory.decodeResource(res,
                    R.drawable.sim_indicator_default_small_locked);
            DEFAULT_SIM_YELLOW_SDN_AVATAR = BitmapFactory.decodeResource(res,
                    R.drawable.sim_indicator_yellow_small_locked);
            DEFAULT_SIM_ORANGE_SDN_AVATAR = BitmapFactory.decodeResource(res,
                    R.drawable.sim_indicator_orange_small_locked);
            DEFAULT_SIM_GREEN_SDN_AVATAR = BitmapFactory.decodeResource(res,
                    R.drawable.sim_indicator_green_small_locked);
            DEFAULT_SIM_PURPLE_SDN_AVATAR = BitmapFactory.decodeResource(res,
                    R.drawable.sim_indicator_purple_small_locked);
            DEFAULT_SIM_SDN_AVATAR_LOCKED = BitmapFactory.decodeResource(res,
                    R.drawable.sim_indicator_sim_locked);
            // / M: add show sim card icon feature @{
            ExtensionManager.getInstance().getCtExtension().loadSimCardIconBitmap(res);
            // / @}
        }
    }

    public void setSIMProperty(DefaultImageRequest request) {
        if (request.subId > 0) {
            mSubId = request.subId;
            mSdnPhotoId = request.photoId;
        }
        Log.d(TAG, "[setSIMProperty]request subId = " + request.subId + ",request photoId: "
                + request.photoId);
    }

    class IconEntry {
        public int iconTint;
        public Bitmap iconBitmap;

    }

    private static HashMap<Integer, IconEntry> BITMAP_ICONS = new HashMap<Integer, IconEntry>();

    public void initSimIconBitmaps() {
        BITMAP_ICONS.clear();
        int[] subIds = SubInfoUtils.getActiveSubscriptionIdList();
        int size = subIds.length;
        for (int i = 0; i < size; i++) {
            IconEntry icon = new IconEntry();
            icon.iconBitmap = SubInfoUtils.getIconBitmap(subIds[i]);
            icon.iconTint = SubInfoUtils.getColorUsingSubId(subIds[i]);
            int soltId = SubscriptionManager.getSlotId(subIds[i]);
            BITMAP_ICONS.put(soltId, icon);
        }
    }

    private Bitmap getIconBitmapUsingSubId(int subId) {
        int soltId = SubscriptionManager.getSlotId(subId);
        IconEntry iconEntry = BITMAP_ICONS.get(soltId);
        Bitmap bitmap = null;
        if (iconEntry != null) {
            bitmap = iconEntry.iconBitmap;
        }
        return bitmap;
    }

    public Bitmap getIconBitmapCache(int subId) {
        // Icon color change by setting, we refresh bitmaps icon cache.
        Bitmap bitmap = null;
        int soltId = SubscriptionManager.getSlotId(subId);
        IconEntry iconEntry = BITMAP_ICONS.get(soltId);
        if (iconEntry == null || SubInfoUtils.iconTintChange(iconEntry.iconTint, subId)) {
            Log.d(TAG, "icon tint changed need to re-get sim icons bitmap");
            initSimIconBitmaps();
        }
        bitmap = getIconBitmapUsingSubId(subId);
        return bitmap;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (SubInfoUtils.checkSubscriber(mSubId)) {
            Bitmap bitmap = getIconBitmapCache(mSubId);
            Log.d(TAG, "bitmap: " + bitmap);
            if (bitmap != null) {
                drawSimAvatar(bitmap, bitmap.getWidth(), bitmap.getHeight(), canvas);
            }
            // For SDN icon.
            if (mSdnPhotoId == SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_ID_SDN_LOCKED) {
                bitmap = DEFAULT_SIM_SDN_AVATAR_LOCKED;
                drawSdnAvatar(bitmap, bitmap.getWidth(), bitmap.getHeight(), canvas);
            }
        }
    }

    private void drawSimAvatar(final Bitmap bitmap, final int width, final int height,
            final Canvas canvas) {
        // rect for sim avatar
        final Rect destRect = copyBounds();
        destRect.set((int) (destRect.right - mScale * SIM_AVATAR_WIDTH_RATIO * destRect.width()),
                (int) (destRect.bottom - mScale * SIM_AVATAR_HEIGHT_RATIO * destRect.height()),
                destRect.right, destRect.bottom);
        sPaint.setColor(mBackgroundColor);
        sPaint.setAntiAlias(true);
        float radius = destRect.width() / 2 * 1.2f;
        Log.d(TAG, "width: " + width);
        Log.d(TAG, "radius: " + radius);
        canvas.drawCircle(destRect.centerX(), destRect.centerY(), radius, sPaint);
        canvas.drawBitmap(bitmap, null, destRect, mSimPaint);
    }

    private void drawSdnAvatar(final Bitmap bitmap, final int width, final int height,
            final Canvas canvas) {
        // rect for sim avatar
        final Rect destRect = copyBounds();

        destRect.set((int) (destRect.left), (int) (destRect.top + mScale * SDN_LOCKED_RATIO
                * destRect.height()),
                (int) (destRect.left + mScale * SDN_LOCKED_RATIO * destRect.width()),
                (int) (destRect.top + 2.0f * mScale * SDN_LOCKED_RATIO * destRect.height()));

        canvas.drawBitmap(bitmap, null, destRect, mSimPaint);
    }
}
