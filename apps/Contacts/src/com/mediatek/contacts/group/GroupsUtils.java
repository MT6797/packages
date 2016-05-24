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
package com.mediatek.contacts.group;

import android.database.Cursor;

import com.android.contacts.GroupListLoader;
import com.google.common.base.Objects;

import com.mediatek.contacts.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class GroupsUtils {
    private static final String TAG = "GroupsUtils";
    /// M: We need get group numbers for Move to other group feature.@{
    /**
     * Get group numbers for every account.
     */
    public static void initGroupsByAllAccount(Cursor mCursor, ArrayList<String> mAccountNameList,
            HashMap<String, Integer> mAccountGroupMembers) {
        if (mCursor == null || mCursor.getCount() == 0) {
            Log.d(TAG, "[initGroupsByAllAccount] mCursor = " + mCursor);
            return;
        }

        mCursor.moveToPosition(-1);
        int accountNum = getAllAccoutNums(mCursor, mAccountNameList);
        Log.d(TAG, "[initGroupsByAllAccount]group count:" + accountNum);
        int groups = 0;
        for (int index = 0; index < accountNum; index++) {
            groups = getGroupNumsByAccountName(mCursor, mAccountNameList.get(index));
            mAccountGroupMembers.put(mAccountNameList.get(index), groups);
        }
    }
    /**
     * Get groups by specified account name.
     * @param name, account name.
     * @return
     */
    public static int getGroupNumsByAccountName(Cursor mCursor, String name) {
        int count = 0;
        int index = 0;
        while (mCursor.moveToPosition(index)) {
            String accountName = mCursor.getString(GroupListLoader.ACCOUNT_NAME);
            if (accountName.equals(name)) {
                count++;
            }
            index++;
        }
        return count;
    }
    /**
     * Get all account numbers.
     * @return
     */
    public static int getAllAccoutNums(Cursor mCursor, ArrayList<String> mAccountNameList) {
        int pos = 0;
        int count = 0;
        mAccountNameList.clear();
        while (mCursor.moveToPosition(pos)) {
            String accountName = mCursor.getString(GroupListLoader.ACCOUNT_NAME);
            String accountType = mCursor.getString(GroupListLoader.ACCOUNT_TYPE);
            String dataSet = mCursor.getString(GroupListLoader.DATA_SET);
            int previousIndex = pos - 1;
            if (previousIndex >= 0 && mCursor.moveToPosition(previousIndex)) {
                String previousGroupAccountName = mCursor.getString(GroupListLoader.ACCOUNT_NAME);
                String previousGroupAccountType = mCursor.getString(GroupListLoader.ACCOUNT_TYPE);
                String previousGroupDataSet = mCursor.getString(GroupListLoader.DATA_SET);

                if (!(accountName.equals(previousGroupAccountName) &&
                        accountType.equals(previousGroupAccountType) && Objects
                        .equal(dataSet, previousGroupDataSet))) {
                    count++;
                    mAccountNameList.add(accountName);
                }
            }
            else {
                mAccountNameList.add(accountName);
                count++;
            }
            pos++;
        }

        return count;
    }
  /// @}
}
