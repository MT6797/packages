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

import android.app.ListActivity;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.widget.Toast;
import android.util.Log;
import java.util.LinkedList;
import android.content.Intent;

/**
 * Settings panel for photo flipping dream.
 */
public class FlipperDreamSettings extends ListActivity {
    @SuppressWarnings("unused")
    private static final String TAG = "FlipperDreamSettings";
    public static final String PREFS_NAME = FlipperDream.TAG;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    protected SharedPreferences mSettings;
    public static final String FIRST_TIME_ENTER="first_enter";
    private final static int REQ_CODE = 3;
    private final static int RESULT_OK = 4;
    private final static int RESULT_CANCELED = 5;

    private PhotoSourcePlexor mPhotoSource;
    private SectionedAlbumDataAdapter mAdapter;
    private MenuItem mSelectAll;
    private AsyncTask<Void, Void, Void> mLoadingTask;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        mSettings = getSharedPreferences(PREFS_NAME, 0);
    }

    @Override
    protected void onResume(){
         super.onResume();
         if (getApplicationContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
          != PackageManager.PERMISSION_GRANTED)
          {
           Log.d(TAG, "onResume() Inside Permission check ");
         Intent intent_per = new Intent(this, PermissionActivity.class);
         startActivityForResult(intent_per,REQ_CODE);
         }else
         {
           init();
         }
         //return false;
    }

    @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {

     if (requestCode == REQ_CODE) {
       Log.d(TAG, " requestCode == REQ_CODE" + requestCode);
       if(resultCode == RESULT_OK){
            Log.d(TAG, "resultCode == RESULT_OK" + resultCode);
            Log.d(TAG, "Yaiyee!! Permission granted");
            init();
       }
       else if (resultCode == RESULT_CANCELED) {
              Log.d(TAG, "resultCode == RESULT_CANCELED" + resultCode);
              Log.d(TAG, "Oops!! We are denied");
              finish();
        }
      }
     }
    protected void init() {
        mPhotoSource = new PhotoSourcePlexor(this, mSettings);
        setContentView(R.layout.settingslist);
        if (mLoadingTask != null && mLoadingTask.getStatus() != Status.FINISHED) {
            mLoadingTask.cancel(true);
        }
        showApology(false);
        mLoadingTask = new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... unused) {
                mAdapter = new SectionedAlbumDataAdapter(FlipperDreamSettings.this,
                        mSettings,
                        R.layout.header,
                        R.layout.album,
                        new LinkedList<PhotoSource.AlbumData>(mPhotoSource.findAlbums()));
                return null;
            }

           @Override
           public void onPostExecute(Void unused) {
               mAdapter.registerDataSetObserver(new DataSetObserver () {
                       @Override
                       public void onChanged() {
                           updateActionItem();
                       }
                       @Override
                       public void onInvalidated() {
                           updateActionItem();
                       }
                   });
               setListAdapter(mAdapter);
               getListView().setItemsCanFocus(true);
               updateActionItem();
               showApology(mAdapter.getCount() == 0);
           }
        };
        mLoadingTask.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.photodream_settings_menu, menu);
        mSelectAll = menu.findItem(R.id.photodream_menu_all);
        updateActionItem();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.photodream_menu_all:
            if (mAdapter != null) {
                mAdapter.selectAll(!mAdapter.areAllSelected());
            }
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void showApology(boolean apologize) {
        View empty = findViewById(R.id.spinner);
        View sorry = findViewById(R.id.sorry);
        if (empty != null && sorry != null) {
            empty.setVisibility(apologize ? View.GONE : View.VISIBLE);
            sorry.setVisibility(apologize ? View.VISIBLE : View.GONE);
        }
    }

    private void updateActionItem() {
        if (mAdapter != null && mSelectAll != null) {
            if (mAdapter.areAllSelected()) {
                mSelectAll.setTitle(R.string.photodream_select_none);
            } else {
                mSelectAll.setTitle(R.string.photodream_select_all);
            }
        }
    }
}
