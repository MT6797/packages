/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.dreams.phototable;

import android.service.dreams.DreamService;

/**
 * Example interactive screen saver: single photo with flipping.
 */
public class FlipperDream extends DreamService {
    public static final String TAG = "FlipperDream";

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        setInteractive(false);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        AlbumSettings settings = AlbumSettings.getAlbumSettings(
                getSharedPreferences(FlipperDreamSettings.PREFS_NAME, 0));
        /** M: ALPS01189548, check source config is empty */
        if (settings.isConfigured() && !settings.isEmpty(this.getApplicationContext())) {
            setContentView(R.layout.carousel);
        } else {
            setContentView(R.layout.bummer);
        }

        setFullscreen(true);
    }

    /// M: ALPS00439129, after daydream exits, remove delay tasks @{
    @Override
    public void onDetachedFromWindow() {
        PhotoCarousel carousel = (PhotoCarousel) findViewById(R.id.carousel);
        if (null != carousel) {
            carousel.removeTasks();
        }
        super.onDetachedFromWindow();
    }
    /// @}
}
