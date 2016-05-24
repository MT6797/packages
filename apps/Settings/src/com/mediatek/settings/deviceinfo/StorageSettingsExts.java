package com.mediatek.settings.deviceinfo;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Environment;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.android.settings.R;
import com.mediatek.settings.FeatureOption;
import com.mediatek.storage.StorageManagerEx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StorageSettingsExts {
    private static final String TAG = "StorageSettings";

    public static final String KEY_DEFAULT_WRITE_DISK = "default_write_disk";
    public static final String KEY_WRITE_DISK_ITEM = "/storage/";
    private static final String USB_STORAGE_PATH = "/mnt/usbotg";

    private static final int ORDER_PHONE_STORAGE = -3;
    private static final int ORDER_SD_CARD = -2;
    private static final int ORDER_USB_OTG = -1;

    private Activity mActivity;
    private PreferenceScreen mRoot;
    private PreferenceCategory mDiskCategory;
    private RadioButtonPreference mDeafultWritePathPref;
    private RadioButtonPreference[] mStorageWritePathList;

    private boolean mIsCategoryAdded = true;
    private boolean[] mWritePathAdded;

    private String mExternalSDPath;
    private String mDefaultWritePath;
    private StorageVolume[] mStorageVolumes;
    private StorageManager mStorageManager;

    public StorageSettingsExts(Activity activity, PreferenceScreen preferenceScreen,
            StorageManager storageManager) {
        mActivity = activity;
        mRoot = preferenceScreen;
        mStorageManager = storageManager;
    }

    private void initDefaultWriteDiskCategory() {
        mDiskCategory = (PreferenceCategory) mRoot
                .findPreference(KEY_DEFAULT_WRITE_DISK);

        if (FeatureOption.MTK_A1_FEATURE) {
            mRoot.removePreference(mDiskCategory);
        }
    }

    private void updateDefaultWriteDiskCategory() {
        if (FeatureOption.MTK_A1_FEATURE) {
            return;
        }

        mDiskCategory.removeAll();

        mExternalSDPath = StorageManagerEx.getExternalStoragePath();
        mDefaultWritePath = StorageManagerEx.getDefaultPath();
        Log.d(TAG, "Get default Path : " + mDefaultWritePath);

        StorageVolume[] availableVolumes = getDefaultWriteDiskList();
        for (StorageVolume volume : availableVolumes) {
            RadioButtonPreference preference = new RadioButtonPreference(mActivity);
            String path = volume.getPath();
            preference.setKey(path);
            preference.setTitle(volume.getDescription(mActivity));
            preference.setPath(path);
            preference.setOnPreferenceChangeListener(defaultWriteDiskListener);
            mDiskCategory.addPreference(preference);

            if (path.equals(mExternalSDPath)) {
                preference.setOrder(ORDER_SD_CARD);
                // TODO: Replace with Environment.DIRECTORY_USBOTG
            } else if (path.startsWith(USB_STORAGE_PATH)) {
                preference.setOrder(ORDER_USB_OTG);
            } else {
                preference.setOrder(ORDER_PHONE_STORAGE);
            }

            if (mDefaultWritePath.equals(path)) {
                preference.setChecked(true);
                mDeafultWritePathPref = preference;
            } else {
                preference.setChecked(false);
            }
        }

        if (mDiskCategory.getPreferenceCount() > 0) {
            mRoot.addPreference(mDiskCategory);
        }
    }

    private OnPreferenceChangeListener defaultWriteDiskListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference != null && preference instanceof RadioButtonPreference) {
                if (mDeafultWritePathPref != null) {
                    mDeafultWritePathPref.setChecked(false);
                }
                StorageManagerEx.setDefaultPath(preference.getKey());
                Log.d(TAG, "Set default path : " + preference.getKey());
                mDeafultWritePathPref = (RadioButtonPreference) preference;
                return true;
            }
            return false;
        }
    };

    private StorageVolume[] getDefaultWriteDiskList() {
        List<StorageVolume> storageVolumes = new ArrayList<StorageVolume>();
        StorageVolume[] volumes =
            mStorageManager.getVolumeList(UserHandle.myUserId(), StorageManager.FLAG_FOR_WRITE);
        for (StorageVolume volume : volumes) {
            Log.d(TAG, "Volume : " + volume.getDescription(mActivity)
                    + " , path : " + volume.getPath()
                    + " , state : " + mStorageManager.getVolumeState(volume.getPath())
                    + " , emulated : " + volume.isEmulated());
            if (Environment.MEDIA_MOUNTED.equals(
                    mStorageManager.getVolumeState(volume.getPath()))) {
                storageVolumes.add(volume);
            }
        }
        return storageVolumes.toArray(new StorageVolume[storageVolumes.size()]);
    }

    public void initCustomizationCategory() {
        initDefaultWriteDiskCategory();
    }

    public void updateCustomizationCategory() {
        updateDefaultWriteDiskCategory();
    }
}
