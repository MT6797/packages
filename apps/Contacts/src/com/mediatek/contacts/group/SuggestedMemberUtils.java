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

import com.android.contacts.group.SuggestedMemberListAdapter.SuggestedMember;

import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.util.Log;

import java.util.HashMap;
import java.util.List;

public class SuggestedMemberUtils {
    private static final String TAG = "SuggestedMemberUtils";

    public static void setSimInfo(Cursor cursor, SuggestedMember member,
            int simIdColumnIndex, int sdnColumnIndex) {
        int simId = cursor.getInt(simIdColumnIndex);
        member.setSimId(simId);
        if (simId > 0) {
            member.setSimType(SimCardUtils.getSimTypeBySubId(simId));
        }
        member.setIsSdnContact(cursor.getInt(sdnColumnIndex));
    }

    /**
     * Bug fix ALPS00280807, process the joint contacts.
     */
    public static void processJointContacts(SuggestedMember member, long rawContactId,
            HashMap<Long, SuggestedMember> jointContactsMap,
            List<SuggestedMember> suggestionsList) {
        Log.i(TAG, "[processJointContacts] rawContactId = " + rawContactId);
        if (member.getRawContactId() < 0) {
            member.setRawContactId(rawContactId);
        } else {
            if (member.getRawContactId() != rawContactId) {
                SuggestedMember tempMember = jointContactsMap.get(rawContactId);
                if (tempMember == null) {
                    tempMember = new SuggestedMember(member);
                    tempMember.setRawContactId(rawContactId);
                    int index = suggestionsList.indexOf(member);
                    if (index >= 0 && index <= suggestionsList.size()) {
                        suggestionsList.add(index, tempMember);
                    } else {
                        suggestionsList.add(tempMember);
                    }
                    jointContactsMap.put(rawContactId, tempMember);
                }
                member = tempMember;
            }
        }
    }

    /**
     * Bug fix ALPS00280807, set suggested member's fix extras info.
     */
    public static void setFixExtrasInfo(SuggestedMember member, int dataColumnIndex,
            Cursor memberDataCursor, String searchFilter) {
        Log.i(TAG, "[setFixExtrasInfo] dataColumnIndex = " + dataColumnIndex +
                ",searchFilter = " + searchFilter);
        String info = memberDataCursor.getString(dataColumnIndex);
        if (!member.hasExtraInfo()) {
            if (info.indexOf(searchFilter) > -1) {
                member.setFixExtrasInfo(true);
            }
        } else {
            if (!member.hasFixedExtrasInfo()) {
                if (info.indexOf(searchFilter) > -1) {
                    member.setExtraInfo(info);
                    member.setFixExtrasInfo(true);
                }
            }
        }
    }
}
