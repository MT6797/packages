/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.os.Bundle;
import android.os.storage.StorageEventListener;
import android.os.storage.VolumeInfo;
import android.util.Log;

import com.android.settings.R;

import java.util.Objects;

public class StorageWizardReady extends StorageWizardBase {
    private static final String TAG = "StorageWizardReady";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mDisk == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_generic);

        setHeaderText(R.string.storage_wizard_ready_title, mDisk.getDescription());

        // TODO: handle mixed partition cases instead of just guessing based on
        // first volume type we encounter
        final VolumeInfo publicVol = findFirstVolume(VolumeInfo.TYPE_PUBLIC);
        final VolumeInfo privateVol = findFirstVolume(VolumeInfo.TYPE_PRIVATE);
        Log.d(TAG, "onCreate(), publicVol : " + publicVol + " privateVol : " + privateVol);

        if (publicVol != null) {
            setIllustrationInternal(false);
            setBodyText(R.string.storage_wizard_ready_external_body,
                    mDisk.getDescription());
        } else if (privateVol != null) {
            setIllustrationInternal(true);
            setBodyText(R.string.storage_wizard_ready_internal_body,
                    mDisk.getDescription());
        }

        getNextButton().setText(R.string.done);
        /// M:
        mStorage.registerListener(mStorageMountListener);
    }

    @Override
    public void onNavigateNext() {
        finishAffinity();
    }
    /// M: ALPS02300202, add StorageEventListenner to update UI @{
    private final StorageEventListener mStorageMountListener = new StorageEventListener() {
        @Override
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            Log.d(TAG, "onVolumeStateChanged, disk : " + vol.getDiskId()
                    + ", type : " + vol.getType() + ", state : " + vol.getState());
            if (Objects.equals(mDisk.getId(), vol.getDiskId())
                    && (vol.getType() == VolumeInfo.TYPE_PUBLIC)
                    && (newState == VolumeInfo.STATE_MOUNTED)) {
                setIllustrationInternal(false);
                setBodyText(R.string.storage_wizard_ready_external_body,
                        mDisk.getDescription());
            }
        }
    };

    @Override
    protected void onDestroy() {
        mStorage.unregisterListener(mStorageMountListener);
        super.onDestroy();
    }
    /// }@
}
