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

import android.content.res.Resources;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.content.pm.PackageManager;
import android.Manifest;
import android.app.Service;
import android.app.Activity;
import android.content.Context;
import android.service.dreams.DreamService;

import android.util.Log;

/**
 * Example interactive screen saver: flick photos onto a table.
 */
public class PhotoTableDream extends DreamService {
    public static final String TAG = "PhotoTableDream";
    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        setInteractive(true);

        BummerView bummer = (BummerView) findViewById(R.id.bummer);
        if (bummer != null) {
            Resources resources = getResources();
            bummer.setAnimationParams(true,
                    resources.getInteger(R.integer.table_drop_period),
                    resources.getInteger(R.integer.fast_drop));
        }

        PhotoTable table = (PhotoTable) findViewById(R.id.table);
        if (table != null) {
            table.setDream(this);
        }
    }
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        AlbumSettings settings = AlbumSettings.getAlbumSettings(
                getSharedPreferences(PhotoTableDreamSettings.PREFS_NAME, 0));
        /** M: ALPS01189548, check source config is empty */
        if (settings.isConfigured() && !settings.isEmpty(this.getApplicationContext())) {
            setContentView(R.layout.table);
        } else {
            setContentView(R.layout.bummer);
        }
        setFullscreen(true);
    }

    /// M: ALPS00439129, after daydream exits, remove delay tasks @{
    public void onDetachedFromWindow() {
        PhotoTable table = (PhotoTable) findViewById(R.id.table);
        if (null != table) {
            table.removeTasks();
        }
        super.onDetachedFromWindow();
    }
    /// @}
}
