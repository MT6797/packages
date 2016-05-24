/*
* This software/firmware and related documentation ("MediaTek Software") are
* protected under relevant copyright laws. The information contained herein
* is confidential and proprietary to MediaTek Inc. and/or its licensors.
* Without the prior written permission of MediaTek inc. and/or its licensors,
* any reproduction, modification, use or disclosure of MediaTek Software,
* and information contained herein, in whole or in part, shall be strictly prohibited.
*/
/* MediaTek Inc. (C) 2011. All rights reserved.
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

package com.mediatek.contacts.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.contacts.R;
import com.android.contacts.activities.GroupDetailActivity;
import com.android.contacts.activities.GroupEditorActivity;
import com.android.contacts.activities.PeopleActivity;
import com.android.contacts.group.GroupBrowseListFragment;
import com.android.contacts.group.GroupBrowseListFragment.OnGroupBrowserActionListener;

import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.simcontact.SubInfoUtils;

import java.util.List;

public class GroupBrowseActivity extends Activity {
    private static final String TAG = "GroupBrowseActivity";

    private static final int SUBACTIVITY_NEW_GROUP = 4;

    private GroupBrowseListFragment mGroupsFragment;

    private final class GroupBrowserActionListener implements OnGroupBrowserActionListener {

        GroupBrowserActionListener() {}

        @Override
        public void onViewGroupAction(Uri groupUri) {
                int simId = -1;
                int subId = SubInfoUtils.getInvalidSubId();
        ///M: For move to other group feature.
                int count = mGroupsFragment.getAccountGroupMemberCount();
                String accountType = "";
                String accountName = "";
                Log.i(TAG, "groupUri" + groupUri.toString());
                List uriList = groupUri.getPathSegments();
                Uri newGroupUri = ContactsContract.AUTHORITY_URI.buildUpon()
                        .appendPath(uriList.get(0).toString())
                        .appendPath(uriList.get(1).toString()).build();
                if (uriList.size() > 2) {
                    subId = Integer.parseInt(uriList.get(2).toString());
                    Log.i(TAG, "people subId-----------" + subId);
                }
                if (uriList.size() > 3) {
                    accountType = uriList.get(3).toString();
                }
                if (uriList.size() > 4) {
                    accountName = uriList.get(4).toString();
                }
                Log.i(TAG, "newUri-----------" + newGroupUri);
                Log.i(TAG, "accountType-----------" + accountType);
                Log.i(TAG, "accountName-----------" + accountName);
                Intent intent = new Intent(GroupBrowseActivity.this, GroupDetailActivity.class);
                intent.setData(newGroupUri);
                intent.putExtra("AccountCategory", new AccountCategoryInfo(accountType, subId,
                        accountName, count));
                startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.mtk_group_browse_activity);

        mGroupsFragment = (GroupBrowseListFragment) getFragmentManager().
                findFragmentById(R.id.groups_fragment);
        mGroupsFragment.setListener(new GroupBrowserActionListener());

        // We want the UP affordance but no app icon.
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE,
                    ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE
                    | ActionBar.DISPLAY_SHOW_HOME);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mtk_group_browse_options, menu);
        //M:OP01 RCS will add group menu item @{
        ExtensionManager.getInstance().getRcsExtension().addGroupMenuOptions(menu, this);
        /** @} */

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_add_group:
            final Intent intent = new Intent(this, GroupEditorActivity.class);
            intent.setAction(Intent.ACTION_INSERT);
            startActivityForResult(intent, SUBACTIVITY_NEW_GROUP);
            return true;
        case android.R.id.home:
            Intent homeIntent = new Intent(this, PeopleActivity.class);
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(homeIntent);
            finish();
            return true;
        }
        return false;
    }

    public static class AccountCategoryInfo implements Parcelable {

        public String mAccountCategory;
        public int mSubId;
        public String mSimName;
        ///M: For move to other group feature.
        public int mAccountGroupMemberCount;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeString(mAccountCategory);
            out.writeInt(mSubId);
            out.writeString(mSimName);
            ///M: For move to other group feature.
            out.writeInt(mAccountGroupMemberCount);
        }

        public static final Parcelable.Creator<AccountCategoryInfo> CREATOR =
                new Parcelable.Creator<AccountCategoryInfo>() {
            public AccountCategoryInfo createFromParcel(Parcel in) {
                return new AccountCategoryInfo(in);
            }

            public AccountCategoryInfo[] newArray(int size) {
                return new AccountCategoryInfo[size];
            }
        };

        private AccountCategoryInfo(Parcel in) {
            mAccountCategory = in.readString();
            mSubId = in.readInt();
            mSimName = in.readString();
            ///M: For move to other group feature.
            mAccountGroupMemberCount = in.readInt();
        }

        public AccountCategoryInfo(String accountCategory, int subId, String simName, int count) {
            mAccountCategory = accountCategory;
            mSubId = subId;
            mSimName = simName;
            ///M: For move to other group feature.
            mAccountGroupMemberCount = count;
        }
    }
}
