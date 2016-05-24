/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.stk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.TextView.BufferType;
import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.cat.FontSize;
import com.android.internal.telephony.cat.Input;

/**
 * Display a request for a text input a long with a text edit form.
 */
public class StkInputActivity extends Activity implements View.OnClickListener,
        TextWatcher {

    // Members
    private int mState;
    private Context mContext;
    private EditText mTextIn = null;
    private TextView mPromptView = null;
    private View mYesNoLayout = null;
    private View mNormalLayout = null;

    // Constants
    private static final String className = new Object(){}.getClass().getEnclosingClass().getName();
    private static final String LOG_TAG = className.substring(className.lastIndexOf('.') + 1);

    private Input mStkInput = null;
    private boolean mAcceptUsersInput = true;
    // Constants
    private static final int STATE_TEXT = 1;
    private static final int STATE_YES_NO = 2;

    static final String YES_STR_RESPONSE = "YES";
    static final String NO_STR_RESPONSE = "NO";

    // Font size factor values.
    static final float NORMAL_FONT_FACTOR = 1;
    static final float LARGE_FONT_FACTOR = 2;
    static final float SMALL_FONT_FACTOR = (1 / 2);

    // message id for time out
    private static final int MSG_ID_TIMEOUT = 1;
    private StkAppService appService = StkAppService.getInstance();

    private boolean mIsResponseSent = false;
    private int mSlotId = -1;
    Activity mInstance = null;

    Handler mTimeoutHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case MSG_ID_TIMEOUT:
                CatLog.d(LOG_TAG, "Msg timeout.");
                mAcceptUsersInput = false;
                appService.getStkContext(mSlotId).setPendingActivityInstance(mInstance);
                sendResponse(StkAppService.RES_ID_TIMEOUT);
                break;
            }
        }
    };

    // Click listener to handle buttons press..
    public void onClick(View v) {
        String input = null;
        if (!mAcceptUsersInput) {
            CatLog.d(LOG_TAG, "mAcceptUsersInput:false");
            return;
        }

        switch (v.getId()) {
        case R.id.button_ok:
            // Check that text entered is valid .
            if (!verfiyTypedText()) {
                CatLog.d(LOG_TAG, "handleClick, invalid text");
                return;
            }
            mAcceptUsersInput = false;
            input = mTextIn.getText().toString();
            break;
        // Yes/No layout buttons.
        case R.id.button_yes:
            mAcceptUsersInput = false;
            input = YES_STR_RESPONSE;
            break;
        case R.id.button_no:
            mAcceptUsersInput = false;
            input = NO_STR_RESPONSE;
            break;
        }
        CatLog.d(LOG_TAG, "handleClick, ready to response");
        cancelTimeOut();
        appService.getStkContext(mSlotId).setPendingActivityInstance(this);
        sendResponse(StkAppService.RES_ID_INPUT, input, false);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        CatLog.d(LOG_TAG, "onCreate - mIsResponseSent[" + mIsResponseSent + "]");

        // Set the layout for this activity.
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.stk_input);

        // Initialize members
        mTextIn = (EditText) this.findViewById(R.id.in_text);
        mPromptView = (TextView) this.findViewById(R.id.prompt);
        mInstance = this;
        // Set buttons listeners.
        Button okButton = (Button) findViewById(R.id.button_ok);
        Button yesButton = (Button) findViewById(R.id.button_yes);
        Button noButton = (Button) findViewById(R.id.button_no);

        okButton.setOnClickListener(this);
        yesButton.setOnClickListener(this);
        noButton.setOnClickListener(this);

        mYesNoLayout = findViewById(R.id.yes_no_layout);
        mNormalLayout = findViewById(R.id.normal_layout);
        initFromIntent(getIntent());
        mContext = getBaseContext();
        mAcceptUsersInput = true;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        mTextIn.addTextChangedListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        CatLog.d(LOG_TAG, "onResume - mIsResponseSent[" + mIsResponseSent +
                "], slot id: " + mSlotId);
        startTimeOut();
        appService.getStkContext(mSlotId).setPendingActivityInstance(null);
        if (mIsResponseSent) {
            cancelTimeOut();
            finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        CatLog.d(LOG_TAG, "onPause - mIsResponseSent[" + mIsResponseSent + "]");
    }

    @Override
    public void onStop() {
        super.onStop();
        CatLog.d(LOG_TAG, "onStop - mIsResponseSent[" + mIsResponseSent + "]");
        if (mIsResponseSent) {
            cancelTimeOut();
            finish();
        } else {
            appService.getStkContext(mSlotId).setPendingActivityInstance(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CatLog.d(LOG_TAG, "onDestroy - before Send End Session mIsResponseSent[" +
                mIsResponseSent + " , " + mSlotId + "]");
        //If the input activity is finished by stkappservice
        //when receiving OP_LAUNCH_APP from the other SIM, we can not send TR here
        //, since the input cmd is waiting user to process.
        if (!mIsResponseSent && !appService.isInputPending(mSlotId)) {
            CatLog.d(LOG_TAG, "handleDestroy - Send End Session");
            sendResponse(StkAppService.RES_ID_END_SESSION);
        }
        cancelTimeOut();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!mAcceptUsersInput) {
            CatLog.d(LOG_TAG, "mAcceptUsersInput:false");
            return true;
        }

        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            CatLog.d(LOG_TAG, "onKeyDown - KEYCODE_BACK");
            mAcceptUsersInput = false;
            cancelTimeOut();
            appService.getStkContext(mSlotId).setPendingActivityInstance(this);
            sendResponse(StkAppService.RES_ID_BACKWARD, null, false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    void sendResponse(int resId) {
        sendResponse(resId, null, false);
    }

    void sendResponse(int resId, String input, boolean help) {
        if (mSlotId == -1) {
            CatLog.d(LOG_TAG, "slot id is invalid");
            return;
        }

        if (StkAppService.getInstance() == null) {
            CatLog.d(LOG_TAG, "StkAppService is null, Ignore response: id is " + resId);
            return;
        }

        CatLog.d(LOG_TAG, "sendResponse resID[" + resId + "] input[*****] help[" 
                + help + "]");
        mIsResponseSent = true;
        Bundle args = new Bundle();
        args.putInt(StkAppService.OPCODE, StkAppService.OP_RESPONSE);
        args.putInt(StkAppService.SLOT_ID, mSlotId);
        args.putInt(StkAppService.RES_ID, resId);
        if (input != null) {
            args.putString(StkAppService.INPUT, input);
        }
        args.putBoolean(StkAppService.HELP, help);
        mContext.startService(new Intent(mContext, StkAppService.class)
                .putExtras(args));
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(android.view.Menu.NONE, StkApp.MENU_ID_END_SESSION, 1,
                R.string.menu_end_session);
        menu.add(0, StkApp.MENU_ID_HELP, 2, R.string.help);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(android.view.Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(StkApp.MENU_ID_END_SESSION).setVisible(true);
        menu.findItem(StkApp.MENU_ID_HELP).setVisible(mStkInput.helpAvailable);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!mAcceptUsersInput) {
            CatLog.d(LOG_TAG, "mAcceptUsersInput:false");
            return true;
        }
        switch (item.getItemId()) {
        case StkApp.MENU_ID_END_SESSION:
            mAcceptUsersInput = false;
            cancelTimeOut();
            sendResponse(StkAppService.RES_ID_END_SESSION);
            finish();
            return true;
        case StkApp.MENU_ID_HELP:
            mAcceptUsersInput = false;
            cancelTimeOut();
            sendResponse(StkAppService.RES_ID_INPUT, "", true);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        CatLog.d(LOG_TAG, "onSaveInstanceState: " + mSlotId);
        outState.putBoolean("ACCEPT_USERS_INPUT", mAcceptUsersInput);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        CatLog.d(LOG_TAG, "onRestoreInstanceState: " + mSlotId);
        mAcceptUsersInput = savedInstanceState.getBoolean("ACCEPT_USERS_INPUT");
    }

    public void beforeTextChanged(CharSequence s, int start, int count,
            int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Reset timeout.
        startTimeOut();
    }

    public void afterTextChanged(Editable s) {
    }

    private boolean verfiyTypedText() {
        // If not enough input was typed in stay on the edit screen.
        if (mTextIn.getText().length() < mStkInput.minLen) {
            return false;
        }

        return true;
    }

    private void cancelTimeOut() {
        mTimeoutHandler.removeMessages(MSG_ID_TIMEOUT);
    }

    private void startTimeOut() {
        int duration = StkApp.calculateDurationInMilis(mStkInput.duration);

        if (duration <= 0) {
            duration = StkApp.UI_TIMEOUT;
        }
        cancelTimeOut();
        mTimeoutHandler.sendMessageDelayed(mTimeoutHandler
                .obtainMessage(MSG_ID_TIMEOUT), duration);
    }

    private void configInputDisplay() {
        TextView numOfCharsView = (TextView) findViewById(R.id.num_of_chars);
        TextView inTypeView = (TextView) findViewById(R.id.input_type);

        int inTypeId = R.string.alphabet;

        // set the prompt.
        mPromptView.setText(mStkInput.text);

        // Set input type (alphabet/digit) info close to the InText form.
        if (mStkInput.digitOnly) {
            mTextIn.setKeyListener(StkDigitsKeyListener.getInstance());
            inTypeId = R.string.digits;
        }
        inTypeView.setText(inTypeId);

        if (mStkInput.icon != null) {
            setFeatureDrawable(Window.FEATURE_LEFT_ICON, new BitmapDrawable(
                    mStkInput.icon));
        }

        // Handle specific global and text attributes.
        switch (mState) {
        case STATE_TEXT:
            int maxLen = mStkInput.maxLen;
            int minLen = mStkInput.minLen;
            mTextIn.setFilters(new InputFilter[] {new InputFilter.LengthFilter(
                    maxLen)});

            // Set number of chars info.
            String lengthLimit = String.valueOf(minLen);
            if (maxLen != minLen) {
                lengthLimit = minLen + " - " + maxLen;
            }
            numOfCharsView.setText(lengthLimit);

            if (!mStkInput.echo) {
                mTextIn.setInputType(InputType.TYPE_CLASS_NUMBER
                                     | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            }
            // Set default text if present.
            if (mStkInput.defaultText != null) {
                mTextIn.setText(mStkInput.defaultText);
            } else {
                // make sure the text is cleared
                mTextIn.setText("", BufferType.EDITABLE);
            }

            break;
        case STATE_YES_NO:
            // Set display mode - normal / yes-no layout
            mYesNoLayout.setVisibility(View.VISIBLE);
            mNormalLayout.setVisibility(View.GONE);
            break;
        }
    }

    private float getFontSizeFactor(FontSize size) {
        final float[] fontSizes =
            {NORMAL_FONT_FACTOR, LARGE_FONT_FACTOR, SMALL_FONT_FACTOR};

        return fontSizes[size.ordinal()];
    }

    private void initFromIntent(Intent intent) {
        // Get the calling intent type: text/key, and setup the
        // display parameters.
        CatLog.d(LOG_TAG, "initFromIntent - slot id: " + mSlotId);
        if (intent != null) {
            mStkInput = intent.getParcelableExtra("INPUT");
            mSlotId = intent.getIntExtra(StkAppService.SLOT_ID, -1);
            CatLog.d(LOG_TAG, "onCreate - slot id: " + mSlotId);
            if (mStkInput == null) {
                finish();
            } else {
                mState = mStkInput.yesNo ? STATE_YES_NO :
                        STATE_TEXT;
                configInputDisplay();
            }
        } else {
            finish();
        }
    }
}
