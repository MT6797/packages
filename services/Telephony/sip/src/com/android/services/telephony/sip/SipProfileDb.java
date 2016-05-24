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

package com.android.services.telephony.sip;

import com.android.internal.os.AtomicFile;

import android.content.Context;
import android.net.sip.SipProfile;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class that helps perform operations on the SipProfile database.
 */
class SipProfileDb {
    private static final String PREFIX = "[SipProfileDb] ";
    private static final boolean VERBOSE = false; /* STOP SHIP if true */

    private static final String PROFILES_DIR = "/profiles/";
    private static final String PROFILE_OBJ_FILE = ".pobj";

    private static final String SCHEME_PREFIX = "sip:";

    private String mProfilesDirectory;
    private SipSharedPreferences mSipSharedPreferences;
    private int mProfilesCount = -1;

    public SipProfileDb(Context context) {
        mProfilesDirectory = context.getFilesDir().getAbsolutePath() + PROFILES_DIR;
        mSipSharedPreferences = new SipSharedPreferences(context);
    }

    public void deleteProfile(SipProfile p) {
        synchronized(SipProfileDb.class) {
            deleteProfile(new File(mProfilesDirectory + p.getProfileName()));
            if (mProfilesCount < 0) retrieveSipProfileListInternal();
            mSipSharedPreferences.setProfilesCount(--mProfilesCount);
        }
    }

    private void deleteProfile(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) deleteProfile(child);
        }
        file.delete();
    }

    public void saveProfile(SipProfile p) throws IOException {
        synchronized(SipProfileDb.class) {
            if (mProfilesCount < 0) retrieveSipProfileListInternal();
            File f = new File(mProfilesDirectory + p.getProfileName());
            if (!f.exists()) f.mkdirs();
            AtomicFile atomicFile = new AtomicFile(new File(f, PROFILE_OBJ_FILE));
            FileOutputStream fos = null;
            ObjectOutputStream oos = null;
            try {
                fos = atomicFile.startWrite();
                oos = new ObjectOutputStream(fos);
                oos.writeObject(p);
                oos.flush();
                mSipSharedPreferences.setProfilesCount(++mProfilesCount);
                atomicFile.finishWrite(fos);
            } catch (IOException e) {
                atomicFile.failWrite(fos);
                throw e;
            } finally {
                if (oos != null) oos.close();
            }
        }
    }

    public int getProfilesCount() {
        return (mProfilesCount < 0) ?  mSipSharedPreferences.getProfilesCount() : mProfilesCount;
    }

    public List<SipProfile> retrieveSipProfileList() {
        synchronized(SipProfileDb.class) {
            return retrieveSipProfileListInternal();
        }
    }

    private List<SipProfile> retrieveSipProfileListInternal() {
        List<SipProfile> sipProfileList = Collections.synchronizedList(
                new ArrayList<SipProfile>());

        File root = new File(mProfilesDirectory);
        String[] dirs = root.list();
        if (dirs == null) return sipProfileList;
        for (String dir : dirs) {
            SipProfile p = retrieveSipProfileFromName(dir);
            if (p == null) continue;
            sipProfileList.add(p);
        }
        mProfilesCount = sipProfileList.size();
        mSipSharedPreferences.setProfilesCount(mProfilesCount);
        return sipProfileList;
    }

    public SipProfile retrieveSipProfileFromName(String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }

        File root = new File(mProfilesDirectory);
        File f = new File(new File(root, name), PROFILE_OBJ_FILE);
        if (f.exists()) {
            try {
                SipProfile p = deserialize(f);
                if (p != null && name.equals(p.getProfileName())) {
                    return p;
                }
            } catch (IOException e) {
                log("retrieveSipProfileListInternal, exception: " + e);
            }
        }
        return null;
    }

    private SipProfile deserialize(File profileObjectFile) throws IOException {
        AtomicFile atomicFile = new AtomicFile(profileObjectFile);
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(atomicFile.openRead());
            SipProfile p = (SipProfile) ois.readObject();
            return p;
        } catch (ClassNotFoundException e) {
            log("deserialize, exception: " + e);
        } finally {
            if (ois!= null) ois.close();
        }
        return null;
    }

    private static void log(String msg) {
        Log.d(SipUtil.LOG_TAG, PREFIX + msg);
    }
}
