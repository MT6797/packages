/*
 * Copyright (C) 2006 The Android Open Source Project
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

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneNumberUtils.EccEntry;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.DialerKeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.phone.PhoneGlobals;
import com.android.phone.R;
import com.android.phone.PhoneGlobals.SubInfoUpdateListener;
import com.mediatek.telephony.TelephonyManagerEx;

/**
 * Activity to let the user add or edit an ECC contact.
 */
public class CdgEditEccContactScreen extends Activity implements SubInfoUpdateListener{
    private static final String LOG_TAG = "CdgEditEccContactScreen";

    // Menu item codes
    private static final int MENU_DELETE = 1;


    private String mName;
    private String mNumber;
    private boolean mAddContact;
    private int mPosition;

    private EditText mNameField;
    private EditText mNumberField;
    private Button mButton;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        resolveIntent();

        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.mtk_cdg_edit_ecc_contact_screen);
        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
        setupView();
        setTitle(mAddContact ? R.string.add_ecc_number : R.string.edit_ecc_number);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        Resources r = getResources();
        menu.add(0, MENU_DELETE, 0, r.getString(R.string.delete_ecc_number))
                .setIcon(android.R.drawable.ic_menu_delete);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Log.d(LOG_TAG, "[onOptionsItemSelected]item text = " + item.getTitle());

        switch (item.getItemId()) {
            case MENU_DELETE:
                deleteSelected();

                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);

        super.onDestroy();
    }

    private void resolveIntent() {
        Intent intent = getIntent();

        mName = intent.getStringExtra(CdgEccList.INTENT_EXTRA_NAME);
        mNumber = intent.getStringExtra(CdgEccList.INTENT_EXTRA_NUMBER);
        mAddContact = intent.getBooleanExtra(CdgEccList.INTENT_EXTRA_ADD, false);
        mPosition = intent.getIntExtra(CdgEccList.INTENT_EXTRA_POSITION,
                CdgEccList.INVALID_POSITION);

        Log.d(LOG_TAG, "mName: " + mName + " ,mNumber: "
                +mNumber + " ,mAddContact: " + mAddContact);
    }

    private void setupView() {
        mNameField = (EditText) findViewById(R.id.ecc_name);
        if (mNameField != null) {
            mNameField.setOnFocusChangeListener(mOnFocusChangeHandler);
            mNameField.setOnClickListener(mClicked);
        }

        mNumberField = (EditText) findViewById(R.id.ecc_number);
        if (mNumberField != null) {
            mNumberField.setKeyListener(DialerKeyListener.getInstance());
            mNumberField.setOnFocusChangeListener(mOnFocusChangeHandler);
            mNumberField.setOnClickListener(mClicked);
        }

        if (!mAddContact) {
            if (mNameField != null) {
                mNameField.setText(mName);
            }
            if (mNumberField != null) {
                mNumberField.setText(mNumber);
            }
        }

        mButton = (Button) findViewById(R.id.button);
        if (mButton != null) {
            mButton.setOnClickListener(mClicked);
        }
    }

    private String getNameFromTextField() {
        return mNameField.getText().toString();
    }

    private String getNumberFromTextField() {
        return mNumberField.getText().toString();
    }

    private boolean isValidNumber(String number) {
        boolean isValid = true;
        if (number.getBytes().length > 40 || TextUtils.isEmpty(number)) {
            isValid = false;
        }

        Log.d(LOG_TAG, "isValidNumber: " + isValid);
        return isValid;
    }

    private boolean isValidName(String name) {
        boolean isValid = true;
        if (name.getBytes().length > 14) {
            isValid = false;
        }

        Log.d(LOG_TAG, "isValidName: " + isValid);
        return isValid;
    }

    private void updateEccList() {
        Log.d(LOG_TAG, "--- updateEccList ---");

        final String name = getNameFromTextField();
        final String number = PhoneNumberUtils.stripSeparators(getNumberFromTextField());

        Log.d(LOG_TAG, "name = " + name + " , number = " + number);

        ArrayList<EccEntry> eccList = TelephonyManagerEx.getDefault().getUserCustomizedEccList();

        boolean isValidEntry = true;
        if (!isValidNumber(number) || !isValidName(name)) {
            isValidEntry = false;
        }

        if (!isValidNumber(number)) {
            showStatus(getResources().getText(R.string.ecc_invalid_number));
        }

        if (!isValidName(name)) {
            showStatus(getResources().getText(R.string.ecc_invalid_name));
        }

        if (!isValidEntry) {
            return;
        }

        if (!mAddContact && mPosition != CdgEccList.INVALID_POSITION) {
            eccList.remove(mPosition);
        }

        EccEntry newEntry = new EccEntry(name, number);
        eccList.add(newEntry);
        if (eccList.size() > 10) {
            showStatus(getResources().getText(R.string.ecc_storage_full));
            Log.d(LOG_TAG, "storage is full");

            finish();
            return;
        }

        TelephonyManagerEx.getDefault().updateUserCustomizedEccList(eccList);

        showStatus(getResources().getText(mAddContact ?
                R.string.ecc_contact_added : R.string.ecc_contact_updated));
        finish();
    }

    /**
     * Handle the delete command, based upon the state of the Activity.
     */
    private void deleteSelected() {

        Log.d(LOG_TAG, "--- deleteSelected ---" + mAddContact + mPosition);

        ArrayList<EccEntry> eccList = TelephonyManagerEx.getDefault().getUserCustomizedEccList();

        if (mAddContact) {
            finish();
            return;
        } else {
            if (mPosition != CdgEccList.INVALID_POSITION) {
                eccList.remove(mPosition);
            }
        }

        TelephonyManagerEx.getDefault().updateUserCustomizedEccList(eccList);
        finish();
    }

    /**
     * Removed the status field, with preference to displaying a toast
     * to match the rest of settings UI.
     */
    private void showStatus(CharSequence statusMsg) {
        if (statusMsg != null) {
            Toast.makeText(this, statusMsg, Toast.LENGTH_LONG)
                    .show();
        }
    }

    private final View.OnClickListener mClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mNameField) {
                mNumberField.requestFocus();
            } else if (v == mNumberField) {
                mButton.requestFocus();
            } else if (v == mButton) {
                updateEccList();
            }
        }
    };

    private final View.OnFocusChangeListener mOnFocusChangeHandler =
            new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                TextView textView = (TextView) v;
                Selection.selectAll((Spannable) textView.getText());
            }
        }
    };

    @Override
    public void handleSubInfoUpdate() {
        finish();
    }
}
