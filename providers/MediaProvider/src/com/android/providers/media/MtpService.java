/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.providers.media;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.mtp.MtpDatabase;
import android.mtp.MtpServer;
import android.mtp.MtpStorage;
import android.os.Environment;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;
import com.mediatek.storage.StorageManagerEx;

import java.io.File;
import java.util.HashMap;

public class MtpService extends Service {
    private static final String TAG = "MtpService";
    private static final boolean LOGD = true;

    // We restrict PTP to these subdirectories
    private static final String[] PTP_DIRECTORIES = new String[] {
        Environment.DIRECTORY_DCIM,
        Environment.DIRECTORY_PICTURES,
    };

    private void addStorageDevicesLocked() {
        if (mPtpMode) {
            // In PTP mode we support only primary storage
            final StorageVolume primary = StorageManager.getPrimaryVolume(mVolumes);
            final String path = primary.getPath();
            if (path != null) {
                String state = mStorageManager.getVolumeState(path);
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    addStorageLocked(mVolumeMap.get(path));
                }
            }
        } else {
            for (StorageVolume volume : mVolumeMap.values()) {
                addStorageLocked(volume);
            }
        }
    }

    private final StorageEventListener mStorageEventListener = new StorageEventListener() {
        @Override
        public void onStorageStateChanged(String path, String oldState, String newState) {
            synchronized (mBinder) {
                Log.d(TAG, "onStorageStateChanged " + path + " " + oldState + " -> " + newState);
                if (Environment.MEDIA_MOUNTED.equals(newState)) {
                        volumeMountedLocked(path);
                } else if (Environment.MEDIA_MOUNTED.equals(oldState)) {
                    StorageVolume volume = mVolumeMap.remove(path);
                    if (volume != null) {
                        removeStorageLocked(volume);
                    }
                }
            }
        }
    };

    private MtpDatabase mDatabase;
    private MtpServer mServer;
    private StorageManager mStorageManager;
    /** Flag indicating if MTP is disabled due to keyguard */
    private boolean mMtpDisabled;
    private boolean mUnlocked;
    private boolean mPtpMode;
    private final HashMap<String, StorageVolume> mVolumeMap = new HashMap<String, StorageVolume>();
    private final HashMap<String, MtpStorage> mStorageMap = new HashMap<String, MtpStorage>();
    private StorageVolume[] mVolumes;
    private boolean mIsUsbConfigured;

    @Override
    public void onCreate() {
        // for storage update
        registerReceiver(mLocaleChangedReceiver, new IntentFilter(Intent.ACTION_LOCALE_CHANGED));
        mStorageManager = StorageManager.from(this);
        synchronized (mBinder) {
            updateDisabledStateLocked();
            mStorageManager.registerListener(mStorageEventListener);
            StorageVolume[] volumes = mStorageManager.getVolumeList();
            mVolumes = volumes;
            for (int i = 0; i < volumes.length; i++) {
                String path = volumes[i].getPath();
                String state = mStorageManager.getVolumeState(path);
                Log.d(TAG, "onCreate: path of volumes[" + i + "]=" + path);
                Log.d(TAG, "onCreate: state of volumes[" + i + "]=" + state);
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    volumeMountedLocked(path);
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mUnlocked = intent.getBooleanExtra(UsbManager.USB_DATA_UNLOCKED, false);
        if (LOGD) { Log.d(TAG, "onStartCommand intent=" + intent + " mUnlocked=" + mUnlocked); }
        synchronized (mBinder) {
            updateDisabledStateLocked();
            mPtpMode = (intent == null ? false
                    : intent.getBooleanExtra(UsbManager.USB_FUNCTION_PTP, false));
            String[] subdirs = null;
            if (mPtpMode) {
                int count = PTP_DIRECTORIES.length;
                subdirs = new String[count];
                for (int i = 0; i < count; i++) {
                    File file =
                            Environment.getExternalStoragePublicDirectory(PTP_DIRECTORIES[i]);
                    // make sure this directory exists
                    file.mkdirs();
                    subdirs[i] = file.getPath();
                }
            }
                final StorageVolume primary = StorageManager.getPrimaryVolume(mVolumes);
                if (mDatabase != null) {
                    mDatabase.setServer(null);
                }
                mDatabase = new MtpDatabase(this, MediaProvider.EXTERNAL_VOLUME,
                        primary.getPath(), subdirs);
                manageServiceLocked();
            }

        return START_REDELIVER_INTENT;
    }

    private void updateDisabledStateLocked() {
        final boolean isCurrentUser = UserHandle.myUserId() == ActivityManager.getCurrentUser();
        mMtpDisabled = !mUnlocked || !isCurrentUser;
        if (LOGD) {
            Log.d(TAG, "updating state; isCurrentUser=" + isCurrentUser + ", mMtpLocked="
                    + mMtpDisabled);
        }
    }

    /**
     * Manage {@link #mServer}, creating only when running as the current user.
     */
    private void manageServiceLocked() {
        final boolean isCurrentUser = UserHandle.myUserId() == ActivityManager.getCurrentUser();
        if (mServer == null && isCurrentUser) {
            Log.d(TAG, "starting MTP server in " + (mPtpMode ? "PTP mode" : "MTP mode"));
            mServer = new MtpServer(mDatabase, mPtpMode);
            mDatabase.setServer(mServer);
            if (!mMtpDisabled) {
                addStorageDevicesLocked();
            }
            mServer.start();
        } else if (mServer != null && !isCurrentUser) {
            Log.d(TAG, "no longer current user; shutting down MTP server");
            // Internally, kernel will close our FD, and server thread will
            // handle cleanup.
            mServer = null;
            mDatabase.setServer(null);
        }
    }

    @Override
    public void onDestroy() {
        mStorageManager.unregisterListener(mStorageEventListener);
        if (mDatabase != null) {
            mDatabase.setServer(null);
        }
    }

    private final IMtpService.Stub mBinder =
            new IMtpService.Stub() {
        public void sendObjectAdded(int objectHandle) {
            synchronized (mBinder) {
                if (mServer != null) {
                    mServer.sendObjectAdded(objectHandle);
                }
            }
        }

        public void sendObjectRemoved(int objectHandle) {
            synchronized (mBinder) {
                if (mServer != null) {
                    mServer.sendObjectRemoved(objectHandle);
                }
            }
        }
        // for storage update
        public void sendObjectInfoChanged(int objectHandle) {
            synchronized (mBinder) {
                Log.d(TAG, "mBinder: sendObjectInfoChanged, objectHandle = 0x" + Integer.toHexString(objectHandle));
                if (mServer != null) {
                    mServer.sendObjectInfoChanged(objectHandle);
                }
            }
        }

        public void sendStorageInfoChanged(MtpStorage storage) {
            synchronized (mBinder) {
                Log.d(TAG, "mBinder: sendObjectInfoChanged, storage.getStorageId = 0x"
                        + Integer.toHexString(storage.getStorageId()));
                if (mServer != null) {
                    mServer.sendStorageInfoChanged(storage);
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void volumeMountedLocked(String path) {
        // for update storage
        StorageVolume[] volumes = mStorageManager.getVolumeList();
        mVolumes = volumes;
        for (int i = 0; i < mVolumes.length; i++) {
            StorageVolume volume = mVolumes[i];
            if (volume.getPath().equals(path)) {
                mVolumeMap.put(path, volume);
                if (!mMtpDisabled) {
                    // In PTP mode we support only primary storage
                    if (volume.isPrimary() || !mPtpMode) {
                        addStorageLocked(volume);
                    }
                }
                break;
            }
        }
    }

    private void addStorageLocked(StorageVolume volume) {
        if (volume == null) {
            Log.e(TAG, "addStorageLocked: No storage was mounted!");
            return;
        }

        MtpStorage storage = new MtpStorage(volume, getApplicationContext());
        mStorageMap.put(storage.getPath(), storage);

        if (storage.getStorageId() == StorageVolume.STORAGE_ID_INVALID) {
            Log.w(TAG, "Ignoring volume with invalid MTP storage ID: " + storage);
            return;
        } else {
            Log.d(TAG, "Adding MTP storage 0x" + Integer.toHexString(storage.getStorageId())
                    + " at " + storage.getPath());
        }

        Log.d(TAG, "addStorageLocked " + storage.getStorageId() + " " + storage.getPath());

        //ICUSB , we do not share the ICUSB storage 1 to PC
        String ICUSB_STORAGE_1_MNT_POINT = "/mnt/udisk/folder1" ;
        if (volume.getPath().equals(ICUSB_STORAGE_1_MNT_POINT)) {
            Log.e(TAG, "addStorageLocked: meet icusb storage " + storage.getPath() + " , and make it unshared");
            return;
        }

        if (mDatabase != null) {
            mDatabase.addStorage(storage);
        }
        if (mServer != null) {
            mServer.addStorage(storage);
        }
    }

    private void removeStorageLocked(StorageVolume volume) {
        MtpStorage storage = mStorageMap.remove(volume.getPath());
        if (storage == null) {
            Log.e(TAG, "Missing MtpStorage for " + volume.getPath());
            return;
        }

        Log.d(TAG, "Removing MTP storage " + Integer.toHexString(storage.getStorageId()) + " at "
                + storage.getPath());
        if (mDatabase != null) {
            mDatabase.removeStorage(storage);
        }
        if (mServer != null) {
            mServer.removeStorage(storage);
        }
    }
    private final BroadcastReceiver mLocaleChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "ACTION_LOCALE_CHANGED: BroadcastReceiver: onReceive: synchronized");

            final String action = intent.getAction();
            if (Intent.ACTION_LOCALE_CHANGED.equals(action) && !mMtpDisabled) {
                synchronized (mBinder) {
                    Log.d(TAG, "ACTION_LOCALE_CHANGED : BroadcastReceiver: onReceive: synchronized");

                    StorageVolume[] volumes = mStorageManager.getVolumeList();
                    mVolumes = volumes;

                    for (int i = 0; i < mVolumes.length; i++) {
                        StorageVolume volume = mVolumes[i];
                        updateStorageLocked(volume);
                    }
                }
            }
        }
    };

    private void updateStorageLocked(StorageVolume volume) {
        MtpStorage storage = new MtpStorage(volume, getApplicationContext());
        Log.d(TAG, "updateStorageLocked " + storage.getStorageId() + " = " + storage.getStorageId());

        if (mServer != null) {
            Log.d(TAG, "updateStorageLocked: updateStorageLocked storage " + storage.getPath() + " into MtpServer");
            mServer.updateStorage(storage);
        }
    }
}
