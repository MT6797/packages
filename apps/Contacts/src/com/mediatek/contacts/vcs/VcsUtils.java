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
package com.mediatek.contacts.vcs;

import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Directory;
//import android.util.Log;

import com.android.contacts.list.DefaultContactBrowseListFragment;

import com.mediatek.contacts.util.Log;

import java.util.ArrayList;

public class VcsUtils {
    private static final String TAG = "VcsUtils";

    private static boolean IS_ANIMATOR_ENABLE = false;
    private static final int MAX_NAME_COUNTS_TODISPLAY = 6;
    private static final int COLUMN_COUNTS_OF_CURSOR = 8;
    private static boolean MTK_VOICE_CONTACT_SEARCH_SUPPORT = SystemProperties.getBoolean(
            "ro.mtk_voice_contact_support", false);
    private static final String KEY_ENABLE_VCS_BY_USER = "enable_vcs_by_user";
    private static final String PREFERENCE_NAME = "vcs_preference";

    private static final String[] VCS_CONTACT_PROJECTION = new String[] { Contacts._ID, // 0
            Contacts.DISPLAY_NAME_PRIMARY, // 1
            Contacts.CONTACT_PRESENCE, // 2
            Contacts.CONTACT_STATUS, // 3
            Contacts.PHOTO_ID, // 4
            Contacts.PHOTO_URI, // 5
            Contacts.PHOTO_THUMBNAIL_URI, // 6
            Contacts.LOOKUP_KEY // 7
    };

    /**
     * [VCS] whether VCS feature enabled on this device
     *
     * @return ture if allowed to enable
     */
    public static boolean isVcsFeatureEnable() {
        return MTK_VOICE_CONTACT_SEARCH_SUPPORT;
    }

    /**
     *
     * @param context
     * @return true if vcs if enable by user,false else.default will return
     *         false.
     */
    public static boolean isVcsEnableByUser(Context context) {
        SharedPreferences sp = context.getSharedPreferences(VcsUtils.PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_ENABLE_VCS_BY_USER, false);
    }

    /**
     *
     * @param enable
     *            true to enable the vcs,false to disable.
     * @param context
     */
    public static void setVcsEnableByUser(boolean enable, Context context) {
        SharedPreferences sp = context.getSharedPreferences(VcsUtils.PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_ENABLE_VCS_BY_USER, enable).commit();
    }

    public static boolean isAnimatorEnable() {
        return IS_ANIMATOR_ENABLE;
    }

    public static Cursor getCursorByAudioName(DefaultContactBrowseListFragment allFragment,
            ArrayList<String> audioNameList, CursorLoader loader, Context context) {
        if (audioNameList.size() <= 0) {
            Log.w(TAG, "[getCursorByAudioName]audioNameList is empty:" + audioNameList);
            return null;
        }
        int nameListSize = audioNameList.size();
        StringBuffer sbToLog = new StringBuffer();

        // 1.make name filter selection
        StringBuffer selection = new StringBuffer();
        ArrayList<String> selectionArgs = new ArrayList<String>();
        selection.append("(");
        for (int i = 0; i < nameListSize; i++) {
            selection.append("display_name like ? or ");
            selectionArgs.add("%" + audioNameList.get(i) + "%");
            sbToLog.append(audioNameList.get(i) + ",");
        }
        // 1==1 to handle nameListSize is null or empty
        selection.append("1=1) ");

        // 2.make account filter selection
        String accountFilter = "1=1";
        if (allFragment.getAdapter() == null) {
            Log.w(TAG, "[getCursorByAudioName]adapter is null");
            return null;
        }
        if (allFragment.getAdapter() instanceof
                com.android.contacts.common.list.DefaultContactListAdapter) {
            allFragment.getAdapter().configureLoader(loader, Directory.DEFAULT);
            accountFilter = loader.getSelection();
        }
        selection.append("and (" + accountFilter + ")");

        // 3.make selection args
        // fix CR: ALPS02009019,allFragment.getActivity is null pointer problem.
        final ContentResolver resolver = context.getContentResolver();
        Uri uri = loader.getUri();
        String[] args = loader.getSelectionArgs();
        if (args != null) {
            for (String s : args) {
                selectionArgs.add(s);
                sbToLog.append(s + ",");
            }
        }
        Log.d(TAG, "[getCursorByAudioName] uri:" + uri + ",selects:" + selection + ":args:"
                + sbToLog.toString());

        // 4.query contacts DB
        Log.i(
                TAG,
                "[vcs][performance],start query ContactsProvider,time:"
                        + System.currentTimeMillis());
        Cursor originalCursor = resolver.query(uri, VCS_CONTACT_PROJECTION, selection.toString(),
                selectionArgs.toArray(new String[0]), "sort_key");
        Log.i(TAG,
                "[vcs][performance],end query ContactsProvider,time:" + System.currentTimeMillis());

        Log.i(TAG,
                "[getCursorByAudioName] [vcs] originalCursor counts:" + originalCursor.getCount());
        Cursor cursor = orderVcsCursor(audioNameList, originalCursor);
        return cursor;
    }

    private static Cursor orderVcsCursor(ArrayList<String> audioNameList, Cursor originalCursor) {
        if (originalCursor == null) {
            Log.w(TAG, "[orderVcsCursor] cusur is null.");
            return null;
        }
        String preAudioItemName = new String();
        String currAudioItemName = new String();
        String cursorItemName = new String();
        int itemCounts = 0;
        MatrixCursor audioOrderedCursor = new MatrixCursor(originalCursor.getColumnNames());
        for (int i = 0; i < audioNameList.size(); i++) {
            currAudioItemName = audioNameList.get(i);
            if (currAudioItemName.equals(preAudioItemName)) {
                Log.i(TAG, "[orderVcsCursor] skip preAudioItemName:" + preAudioItemName);
                continue;
            }

            while (originalCursor.moveToNext()) {
                cursorItemName = originalCursor.getString(originalCursor
                        .getColumnIndex(Contacts.DISPLAY_NAME_PRIMARY));
                if (currAudioItemName.equals(cursorItemName)) {
                    String[] columnValArray = new String[COLUMN_COUNTS_OF_CURSOR];
                    columnValArray[0] = String.valueOf(originalCursor.getLong(originalCursor
                            .getColumnIndex(Contacts._ID)));
                    columnValArray[1] = originalCursor.getString(originalCursor
                            .getColumnIndex(Contacts.DISPLAY_NAME_PRIMARY));
                    columnValArray[2] = originalCursor.getString(originalCursor
                            .getColumnIndex(Contacts.CONTACT_PRESENCE));
                    columnValArray[3] = originalCursor.getString(originalCursor
                            .getColumnIndex(Contacts.CONTACT_STATUS));
                    columnValArray[4] = originalCursor.getString(originalCursor
                            .getColumnIndex(Contacts.PHOTO_ID));
                    columnValArray[5] = originalCursor.getString(originalCursor
                            .getColumnIndex(Contacts.PHOTO_URI));
                    columnValArray[6] = originalCursor.getString(originalCursor
                            .getColumnIndex(Contacts.PHOTO_THUMBNAIL_URI));
                    columnValArray[7] = originalCursor.getString(originalCursor
                            .getColumnIndex(Contacts.LOOKUP_KEY));
                    try {
                        itemCounts++;
                        if (itemCounts > MAX_NAME_COUNTS_TODISPLAY) {
                            Log.i(TAG, "[orderVcsCursor] [vcs] mounts to max list counts!");
                            break;
                        }
                        audioOrderedCursor.addRow(columnValArray);
                    } catch (Exception e) {
                        // TODO: handle exception
                        Log.e(TAG, "[orderVcsCursor] [vcs]e:", e);
                    }
                }
            }
            // back to original position for ordering next time
            originalCursor.moveToPosition(-1);
            // set previous audio name item
            preAudioItemName = currAudioItemName;
        }

        // close the cursor
        if (originalCursor != null) {
            originalCursor.close();
        }
        Log.i(TAG,
                "[orderVcsCursor] [vcs] orderedCursor counts:" + audioOrderedCursor.getCount());
        audioOrderedCursor.moveToPosition(-1);
        return audioOrderedCursor;
    }
}
