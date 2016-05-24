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

import android.app.Activity;
import android.app.ListActivity;

import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.app.AlertDialog;
import android.widget.Toast;
import android.util.Log;
import android.os.Bundle;
import java.util.LinkedList;
import android.content.Intent;

/**
 * Permission handling activity for dream service.
 */
public class PermissionActivity extends Activity {
    @SuppressWarnings("unused")
    private static final String TAG = "PermissionActivity";
    private final static int REQ_CODE = 3;
    private final static int RESULT_OK = 4;
    private final static int RESULT_CANCELED = 5;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
  //protected SharedPreferences mSettings;
    public static final String FIRST_TIME_ENTER="first_enter";
    public Intent mIntent = new Intent();


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if (getApplicationContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
          != PackageManager.PERMISSION_GRANTED)
         {
               Log.d(TAG, "We are asking for permissions now!! ");
               this.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
               REQUEST_EXTERNAL_STORAGE);
        }else{
            // permission has been granted, continue as usual
              Log.d(TAG, "onResume() Permission granted ");
              setResult(RESULT_OK,mIntent);
              finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                          int[] grantResults)
    {

      switch (requestCode)
      {
       case REQUEST_EXTERNAL_STORAGE:
          if (permissions.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
          {
            Log.d(TAG, "Yaiyee!! Permission granted");
            setResult(RESULT_OK, mIntent);
          }else{
              Log.d(TAG, "Oops!! Permission denied");
              Toast.makeText(this,
              getApplicationContext().getResources()
              .getString(com.mediatek.R.string.denied_required_permission),
              Toast.LENGTH_SHORT)
              .show();
              setResult(RESULT_CANCELED,mIntent);
         }
    break;
       default:
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    break;
    }
     finish();
  }
  }