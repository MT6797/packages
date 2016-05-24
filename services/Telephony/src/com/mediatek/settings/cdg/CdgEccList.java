/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.mediatek.settings.cdg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils.EccEntry;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.android.phone.PhoneGlobals.SubInfoUpdateListener;
import com.android.phone.PhoneGlobals;
import com.android.phone.R;

import com.mediatek.settings.TelephonyUtils;
import com.mediatek.telephony.TelephonyManagerEx;


/**
 * Emergency Number (ECC) List UI for the Phone app.
 */
public class CdgEccList extends ListActivity implements SubInfoUpdateListener {
    private static final String LOG_TAG = "CdgEccList";
    private static final int MENU_ADD = 1;

    public static final String INTENT_EXTRA_NAME = "name";
    public static final String INTENT_EXTRA_NUMBER = "number";
    public static final String INTENT_EXTRA_ADD = "addContact";
    public static final String INTENT_EXTRA_POSITION = "position";
    public static final int INVALID_POSITION = -1;

    private List<Map<String, Object>> mData = new ArrayList<Map<String, Object>>();
    private SimpleAdapter mSimpleAdapter;

    private static final String[] COLUMN_NAMES = new String[] {
        "name",
        "number",
    };

    private static final int[] VIEW_NAMES = new int[] {
        android.R.id.text1,
        android.R.id.text2
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.mtk_cdg_ecc_list);
        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.ecc_title);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mData.clear();
        queryUserCustomizedEccList();
        setListAdapter(mSimpleAdapter);
    }

    @Override
    protected void onDestroy() {
        PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        Resources r = getResources();

        // Added the icons to the context menu
        menu.add(0, MENU_ADD, 0, r.getString(R.string.add_ecc_number))
                .setIcon(android.R.drawable.ic_menu_add);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case MENU_ADD:
                addContact();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        editSelected(position);
    }

    private void queryUserCustomizedEccList() {
        ArrayList<EccEntry> eccList = TelephonyManagerEx.getDefault().getUserCustomizedEccList();
        mSimpleAdapter = newAdapter(eccList);
    }

    private SimpleAdapter newAdapter(ArrayList<EccEntry> data) {
        for(EccEntry entry : data) {
            Map<String, Object> map = new HashMap<String, Object>();
            String name = entry.getName();
            String number = entry.getEcc();

            map.put("name", name);
            map.put("number", number);

            mData.add(map);
        }

        Log.d(LOG_TAG, "data size: " + mData.size());

        return new SimpleAdapter(this, mData,
                    android.R.layout.simple_list_item_2,
                    COLUMN_NAMES, VIEW_NAMES);
    }

    private void addContact() {
        //If there is no INTENT_EXTRA_NAME provided, EditFdnContactScreen treats it as an "add".
        Log.d(LOG_TAG, "--- addContact ---");

        Intent intent = new Intent(this, CdgEditEccContactScreen.class);
        intent.putExtra(INTENT_EXTRA_ADD, true);
        startActivity(intent);
    }

    /**
     * Edit the item at the selected position in the list.
     */
    private void editSelected(int position) {
        if (mData.get(position) != null) {
            Map<String, Object> map = mData.get(position);
            String name = (String) map.get("name");
            String number = (String) map.get("number");

            Log.d(LOG_TAG, "position: " + position + " ,name: "
                  + name + " ,number: " + number);

            Intent intent = new Intent(this, CdgEditEccContactScreen.class);
            intent.putExtra(INTENT_EXTRA_NAME, name);
            intent.putExtra(INTENT_EXTRA_NUMBER, number);
            intent.putExtra(INTENT_EXTRA_ADD, false);
            intent.putExtra(INTENT_EXTRA_POSITION, position);

            startActivity(intent);
        }
    }

    @Override
    public void handleSubInfoUpdate() {
        finish();
    }
}
